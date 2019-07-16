/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.player.Player;
import client.Client;
import client.ClientLoginState;
import community.MapleBuddyListEntry;
import community.MapleGuild;
import community.MapleParty;
import community.MaplePartyCharacter;
import community.MaplePartyOperation;
import constants.ServerProperties;
import handling.channel.ChannelServer;
import handling.channel.handler.ChannelHeaders.BuddyListHeaders;
import handling.mina.PacketReader;
import handling.world.CharacterIdChannelPair;
import handling.world.messenger.MapleMessenger;
import handling.world.messenger.MapleMessengerCharacter;
import handling.world.PlayerBuffStorage;
import handling.world.World;
import handling.world.service.AllianceService;
import handling.world.service.BuddyService;
import handling.world.service.FindService;
import handling.world.service.GuildService;
import handling.world.service.MessengerService;
import handling.world.service.PartyService;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.List;
import packet.creators.CashShopPackets;
import packet.creators.GuildPackets;
import packet.creators.PacketCreator;
import packet.creators.PetPackets;
import packet.transfer.write.OutPacket;
import client.player.PlayerNote;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import server.maps.FieldLimit;
import tools.FileLogger;

/**
 *
 * @author GabrielSin
 */
public class InterServerHandler {
    
    public static void ChangeChannel(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        int channel = packet.readByte() + 1;
        if (p.getEventInstance() != null || c.getChannel() == channel || FieldLimit.CHANGECHANNEL.check(p.getMap().getFieldLimit())) {
            c.getSession().write(PacketCreator.ServerMigrateFailed((byte) 1));
            return;
        }
        p.changeChannel(channel);
    }

    public static final void Loggedin(PacketReader packet, final Client c) throws SQLException {
        final int cid = packet.readInt();
        Player p = c.getChannelServer().getPlayerStorage().getCharacterById(cid);
	try {
            p = Player.loadinCharacterDatabase(cid, c, true);
        } catch (SQLException e) {
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, "Error player logged-in: " + e);
            System.out.println("[-] Loggedin SQLException");
        }
        
        if (p == null) return;
        
        c.setPlayer(p);
        c.setAccountID(p.getAccountID());
        c.announce(PacketCreator.UpdateGender(p));
        
        final ClientLoginState state = c.getLoginState();
        boolean allowLogin = false;
        
        
        ChannelServer channelServer = c.getChannelServer();
        if (state == ClientLoginState.LOGIN_SERVER_TRANSITION || state == ClientLoginState.CHANGE_CHANNEL || state == ClientLoginState.LOGIN_NOTLOGGEDIN) {
            allowLogin = !World.isCharacterListConnected(c.loadCharacterNames(c.getWorld()));
        }
        
        if (!allowLogin) {
            c.setPlayer(null);
            c.getSession().close();
            return;
        }
        
        c.updateLoginState(ClientLoginState.LOGIN_LOGGEDIN, c.getSessionIPAddress());  
        channelServer.addPlayer(p);
        
        c.getSession().write(PacketCreator.GetCharInfo(p));
        p.getMap().addPlayer(p);
        
        if (!p.isHidden()) {
            p.toggleVisibility(true);
        } 
        
        try {
            p.silentGiveBuffs(PlayerBuffStorage.getBuffsFromStorage(p.getId()));
            p.giveCoolDowns(PlayerBuffStorage.getCooldownsFromStorage(p.getId()));
            p.giveSilentDebuff(PlayerBuffStorage.getDiseaseFromStorage(p.getId()));

            int buddyIds[] = p.getBuddylist().getBuddyIds();
            BuddyService.loggedOn(p.getName(), p.getId(), c.getChannel(), buddyIds, p.getAdministrativeLevel(), p.isHidden());
            final CharacterIdChannelPair[] onlineBuddies = FindService.multiBuddyFind(p.getId(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
                final MapleBuddyListEntry ble = p.getBuddylist().get(onlineBuddy.getCharacterId());
                ble.setChannel(onlineBuddy.getChannel());
                p.getBuddylist().put(ble);
            }

            c.getSession().write(PacketCreator.UpdateBuddylist(BuddyListHeaders.FIRST, p.getBuddylist().getBuddies()));    

            if (p.getParty() != null) {
                MaplePartyCharacter partyChar = p.getMPC();
                partyChar.setChannel(c.getChannel());
                partyChar.setMapId(p.getMapId());
                partyChar.setOnline(true);
                p.receivePartyMemberHP();
                p.updatePartyMemberHP();
                final MapleParty party = p.getParty();
                if (party != null) {
                    PartyService.updateParty(party.getId(), MaplePartyOperation.LOG_ONOFF, new MaplePartyCharacter(p));
                }
            }

            showDueyNotification(c, p);
            
            p.sendKeymap();
            p.checkBerserk(p.isHidden());
            p.expirationTask();
            p.spawnSavedPets(false, true);
            c.announce(PetPackets.AutoHpPot(p.getAutoHpPot()));
            c.announce(PetPackets.AutoMpPot(p.getAutoMpPot()));
            c.announce(PacketCreator.GetMacros(p.getMacros()));
            PlayerNote.showNote(p); 


            final MapleMessenger messenger = p.getMessenger();
            if (messenger != null) {
                MessengerService.silentJoinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()));
                MessengerService.updateMessenger(messenger.getId(), c.getPlayer().getName(), c.getChannel());
            }
            if (p.getGuildId() > 0) {
                GuildService.setGuildMemberOnline(p.getMGC(), true, c.getChannel());
                c.announce(GuildPackets.ShowGuildInfo(p));
                final MapleGuild gs = GuildService.getGuild(p.getGuildId(), p.getClient().getChannel());
                if (gs != null) {
                    final List<OutPacket> packetList = AllianceService.getAllianceInfo(gs.getAllianceId(), true);
                    if (packetList != null) {
                        for (OutPacket pack : packetList) {
                            if (pack != null) {
                                c.getSession().write(pack);
                            }
                        }
                    }
                } else { 
                    p.setGuildId(0);
                    p.setGuildRank((byte) 5);
                    p.setAllianceRank((byte) 5);
                    p.saveGuildStatus();
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }
        
//        for (Integer npcid : NPCConstants.SCRIPTABLE_NPCS)
//            c.announce(PacketCreator.SetNPCScriptable(npcid));
//        
        if (c.getPlayer().getMapId() == 0 && c.getPlayer().getLevel() == 1 && ServerProperties.Misc.WELCOME_MESSAGE) {
            c.getChannelServer().broadcastYellowMessage("[Welcome] The player <" + c.getPlayer().getName() + "> acabou de se juntar ao nosso servidor!");
            c.announce(PacketCreator.GetWhisper("[" +  ServerProperties.Login.SERVER_NAME +"]", 1, "Hello mapler, use the @help command and have a good game."));
        }
        if (ServerProperties.Misc.VOTE_MESSAGE){
            if (!c.hasVotedAlready()) {
                p.dropMessage("Hello, have not you \"voted\" today? How about voting and receiving rewards?");
                p.dropMessage("Go to " + ServerProperties.Misc.WEB_SITE + " and click on \"Vote\"!");
            }
        }   
    }
    
    private static void showDueyNotification(Client c, Player p) {
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement pss = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT Mesos FROM dueypackages WHERE ReceiverId = ? and Checked = 1");
            ps.setInt(1, p.getId());
            rs = ps.executeQuery();
            if (rs.next()) {
                try {
                    Connection con2 = DatabaseConnection.getConnection();
                    pss = con2.prepareStatement("UPDATE dueypackages SET Checked = 0 where ReceiverId = ?");
                    pss.setInt(1, p.getId());
                    pss.executeUpdate();
                    pss.close();
                    con2.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                c.announce(PacketCreator.SendDueyNotification(false));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pss != null) {
                    pss.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void EnterCS(PacketReader packet, Client c) {
        try {
            Player p = c.getPlayer();
            if (!ServerProperties.Misc.CASHSHOP_AVAILABLE || p.getCashShop().isOpened()) {
                c.announce(PacketCreator.ServerMigrateFailed((byte) 2));
                return;
            }
            
            p.closePlayerInteractions();

            p.cancelAllBuffs(false);
            p.cancelAllDebuffs();
            p.cancelExpirationTask();
            
            c.announce(CashShopPackets.TransferToCashShop(c));
            c.announce(CashShopPackets.ShowCashInventory(c));
            c.announce(CashShopPackets.SendWishList(p, false));
            c.announce(CashShopPackets.GiftedCashItems(p.getCashShop().loadGifts(c)));
            c.announce(CashShopPackets.ShowCash(p));
            
            c.getChannelServer().removePlayer(c.getPlayer());
            p.getMap().removePlayer(p);
            p.getCashShop().openedCashShop(true);
            p.saveDatabase();
        } catch (Exception ex) {
            FileLogger.printError("EnterCS.txt", ex);
            System.out.println("[-] EnterCS Exception");
        }
    }

    public static void LeaveCS(PacketReader packet, Client c) {
        String[] socket = ChannelServer.getInstance(c.getChannel()).getIP().split(":");
        if (c.getPlayer().getCashShop().isOpened()) {
            c.getPlayer().getCashShop().openedCashShop(false);
            c.getPlayer().saveDatabase();
        } else {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        ChannelServer.getInstance(c.getChannel()).removePlayer(c.getPlayer());
        c.updateLoginState(ClientLoginState.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
        try {
            c.announce(PacketCreator.GetChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
        } catch (UnknownHostException ex) {
            FileLogger.printError("LeaveCS.txt", ex);
            System.out.println("[-] LeaveCS Exception");
        } 
    }

    public static void TouchingCS(PacketReader packet, Client c) {
        c.getSession().write(CashShopPackets.ShowCash(c.getPlayer()));
    }
}
