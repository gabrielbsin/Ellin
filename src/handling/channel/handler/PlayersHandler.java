/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.player.Player;
import client.Client;
import static handling.channel.handler.ChannelHeaders.PlayersHeaders.*;
import client.player.PlayerStat;
import handling.mina.PacketReader;
import packet.creators.CashShopPackets;
import packet.creators.PacketCreator;
import client.player.PlayerNote;
import client.player.inventory.Item;
import client.player.violation.AutobanManager;
import client.player.violation.CheatingOffense;
import community.MapleParty;
import scripting.npc.NPCScriptManager;
import server.maps.object.FieldObject;
import server.maps.object.FieldDoorObject;
import server.maps.reactors.Reactor;
import tools.FileLogger;

/**
 *
 * @author GabrielSin
 */
public class PlayersHandler {
    
    public static void GiveFame(PacketReader packet, Client c) {
        final Player self = c.getPlayer();
        final Player receiver = (Player) c.getPlayer().getMap().getMapObject(packet.readInt());
        int add = packet.readByte();
        int fameChange = 2 * add - 1;
        
        if (receiver == self) {
            self.getCheatTracker().registerOffense(CheatingOffense.FAMING_SELF, "Tried to give fame to self");
            return;
        }
        if (receiver == null) {
            c.getSession().write(PacketCreator.GiveFameErrorResponse(FAME_OPERATION_RESPONSE_NOT_IN_MAP));
            return;
        }
        if (self.getLevel() < 15) {
            self.getCheatTracker().registerOffense(CheatingOffense.FAMING_UNDER_15);
            c.getSession().write(PacketCreator.GiveFameErrorResponse(FAME_OPERATION_RESPONSE_UNDER_LEVEL));
            return;
        }
        if (fameChange != 1 && fameChange != -1) {
            AutobanManager.getInstance().autoban(self.getClient(), self.getName() + " problaby edit packet.");
            return;
        }
        switch (c.getPlayer().canGiveFame(receiver)) {
            case OK:
                if (Math.abs(receiver.getFame() + fameChange) < 30001) {
                    receiver.addFame(fameChange);
                    receiver.getStat().updateSingleStat(PlayerStat.FAME, receiver.getFame());
                }
                if (!c.getPlayer().isGameMaster()) {
                    c.getPlayer().hasGivenFame(receiver);
                }
                c.getSession().write(PacketCreator.GiveFameResponse(add, receiver.getName(), receiver.getFame()));
                receiver.getClient().getSession().write(PacketCreator.ReceiveFame(add, c.getPlayer().getName()));
                break;
            case NOT_TODAY:
                c.getSession().write(PacketCreator.GiveFameErrorResponse(FAME_OPEARTION_RESPONSE_NOT_TODAY));
                break;
            case NOT_THIS_MONTH:
                c.getSession().write(PacketCreator.GiveFameErrorResponse(FAME_OPERATION_RESPONSE_NOT_THIS_MONTH));
                break;
        }
    }  

    public static void UseDoor(PacketReader packet, Client c) {
        int doorId  = packet.readInt();
        boolean inTown = packet.readBool();
        
        for (FieldObject obj : c.getPlayer().getMap().getAllDoorsThreadsafe()) {
            Player p = c.getPlayer();
            Player owner = c.getChannelServer().getPlayerStorage().getCharacterById(doorId);
            MapleParty party = c.getPlayer().getParty();
            FieldDoorObject door = (FieldDoorObject) obj;
            if (door == null) {
                p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to use nonexistent mystic door");
                return;
            }
            if (owner != p && (party == null || party != p.getParty())) {
                p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to use nonexistent mystic door");
                return;
            }
            if (door.getOwnerId() == doorId ) {
                door.warp(c.getPlayer(), inTown);
                return;
            }
        }
    }

    public static void HitReactor(PacketReader packet, Client c) {
        final int oid = packet.readInt();
	final int charPos = packet.readInt();
	final short stance = packet.readShort();
	final Reactor reactor = c.getPlayer().getMap().getReactorByOid(oid);

        if (reactor == null || !reactor.isAlive()) {
            return;
        }
        reactor.hitReactor(true, charPos, stance, c);
    }

    public static void Note(PacketReader packet, Client c) {
        int type = packet.readByte();
        switch (type) {
            case NOTE_RECEIVE:
                String name = packet.readMapleAsciiString();
                String message = packet.readMapleAsciiString();
                boolean fame = packet.readBool();
                packet.readInt();
                
                int uniqueid = (int) packet.readLong();
                boolean isPackage = c.getPlayer().getCashShop().isPackage(uniqueid);
                if (!isPackage) {
                    Item item = c.getPlayer().getCashShop().findByUniqueId(uniqueid);
                    if (item == null || !item.getGiftFrom().equalsIgnoreCase(name) || !c.getPlayer().getCashShop().canSendNote(item.getUniqueId())) {
                        return;
                    }
                    c.getPlayer().getCashShop().sendedNote(item.getUniqueId());
                } else {
                    c.getPlayer().getCashShop().removePackage(uniqueid);
                }
                PlayerNote.sendNote(c.getPlayer(), name, message, fame ? 1 : 0);
                c.getSession().write(CashShopPackets.ShowCashInventory(c));
                break;
            case NOTE_DELETE:
                int num = packet.readByte();
                packet.skip(2);
                for (int i = 0; i < num; i++) {
                    final int id = packet.readInt();
                    PlayerNote.deleteNote(c.getPlayer(), id, packet.readBool() ? 1 : 0);
                }
                break;
        }
    }
    
    public static void EnableActions(PacketReader packet, Client c) {
        try {
            if (c.getPlayer() != null && c.getPlayer().getMap() != null) {
                c.getPlayer().saveDatabase();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            FileLogger.printError("Erro_updatep.txt", ex);
        }
    }   

    public static void RingAction(PacketReader packet, Client c) {
        byte mode = packet.readByte();
        Player player = c.getPlayer();
        switch (mode) {
            case SEND_RING: 
                String partnerName = packet.readMapleAsciiString();
                Player partner = c.getChannelServer().getPlayerStorage().getCharacterByName(partnerName);
                if (partnerName.equalsIgnoreCase(player.getName())) {
                    player.dropMessage(1, "You cannot put your own name in it.");
                } else if (partner == null) {
                    player.dropMessage(1, partnerName + " was not found on this channel. If you are both logged in, please make sure you are in the same channel.");
                } else if (partner.getGender() == player.getGender()) {
                    player.dropMessage(1, "Your partner is the same gender as you.");
                } else if (player.getPartnerId() > 0 && partner.getPartnerId() > 0 ) {
                    NPCScriptManager.getInstance().start(partner.getClient(), 9201002, "marriagequestion", player);
                }
                break;
            case CANCEL_SEND_RING: 
                player.dropMessage(1, "You've cancelled the request.");
                break;
            case DROP_RING: 
            //    Marriage.divorceEngagement(player);
                player.dropMessage(1, "Your engagement has been broken up.");
                break;
            default:
                System.out.println("Unhandled Ring Packet : " + packet.toString());
                break;
        }
    }
}
