/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.player.Player;
import client.Client;
import client.player.violation.CheatingOffense;
import community.MapleGuild;
import community.MapleGuildContract;
import community.MapleGuildResponse;
import constants.GameConstants;
import constants.MapConstants;
import handling.mina.PacketReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import packet.creators.GuildPackets;
import static handling.channel.handler.ChannelHeaders.GuildHeaders.*;
import handling.world.service.AllianceService;
import handling.world.service.GuildService;

/**
 *
 * @author GabrielSin
 */
public class GuildHandler {
    
    private static final List<Invited> invited = new LinkedList<>();
    private static long nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000;
    
    public static void Guild(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        if (p == null) {
            return;
        }
        if (System.currentTimeMillis() >= nextPruneTime) {
            Iterator<Invited> itr = invited.iterator();
            Invited inv;
            while (itr.hasNext()) {
                inv = itr.next();
                if (System.currentTimeMillis() >= inv.expiration) {
                    itr.remove();
                }
            }
            nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000;
        }
        byte t = packet.readByte();
        switch (t) {
            case GUILD_INFO:
                break;
            case GUILD_CREATE:
                SendGuildContract(packet, p);
                break;
            case GUILD_INVITE:
                GuildInvite(packet, p);
                break;
            case GUILD_JOIN:
                GuildJoin(packet, p);
                break;
            case GUILD_LEAVE:
                GuildLeave(packet, p);
                break;
            case GUILD_EXPEL:
                GuildExpel(packet, p);
                break;
            case GUILD_CHANGE_RANK_STRING:
                GuildChangeRankString(packet, p);
                break;
            case GUILD_CHANGE_PLAYER_RANK:
                GuildChangePlayerRank(packet, p);
                break;
            case GUILD_CHANGE_EMBLEM:
               GuildChangeEmblem(packet, p);
                break;
            case GUILD_CHANGE_NOTICE:
                GuildChangeNotice(packet, p);
                break;
            case GUILD_CONTRACT_RESPONSE:
                GuildContractResponse(packet, p);
                break;
            default:
                System.out.println("Unhandled GUILD_OPERATION packet: \n" + t);
                break;
        }
    }
    
    public static void SendGuildContract(PacketReader packet, Player p) {
        String guildName = packet.readMapleAsciiString();
        if (p.getParty() != null) {
            MapleGuildContract.sendContractMembers(p, guildName, p.getParty());
        }
    }
    
    public static void GuildInvite(PacketReader packet, Player p) {
        if (p.getGuildId() <= 0 || p.getGuildRank() > 2) {  
            return;
        }
        String name = packet.readMapleAsciiString();
        final MapleGuildResponse mgr = MapleGuild.sendInvite(p.getClient(), name);

        if (mgr != null) {
            p.getClient().getSession().write(mgr.getPacket());
        } else {
            Invited inv = new Invited(name, p.getGuildId());
            if (!invited.contains(inv)) {
                invited.add(inv);
            }
        }
    }
    
    public static void GuildJoin(PacketReader packet, Player p) {
        if (p.getGuildId() > 0) {
            return;
        }
        int guildId = packet.readInt();
        int cid = packet.readInt();

        if (cid != p.getId()) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to join guild without being invited");
            return;
        }
        String name = p.getName().toLowerCase();
        Iterator<Invited> itr = invited.iterator();

        while (itr.hasNext()) {
            Invited inv = itr.next();
            if (guildId == inv.gid && name.equals(inv.name)) {
                p.setGuildId(guildId);
                p.setGuildRank((byte) 5);
                itr.remove();

                int s = GuildService.addGuildMember(p.getMGC());
                if (s == 0) {
                    p.dropMessage(1, "The Guild you are trying to join is already full.");
                    p.setGuildId(0);
                    return;
                }
                p.getClient().getSession().write(GuildPackets.ShowGuildInfo(p));
                final MapleGuild gs = GuildService.getGuild(guildId);
                AllianceService.getAllianceInfo(gs.getAllianceId(), true).stream().filter((pack) -> (pack != null)).forEachOrdered((pack) -> {
                    p.getClient().getSession().write(pack);
                });
                p.saveGuildStatus();
                respawnPlayer(p);
                break;
            }
        }
    }
    
    public static void GuildLeave(PacketReader packet, Player p) {
        int cid = packet.readInt();
        String name = packet.readMapleAsciiString();

        if (cid != p.getId() || !name.equals(p.getName()) || p.getGuildId() <= 0) {
            return;
        }
        GuildService.leaveGuild(p.getMGC());
        p.getClient().getSession().write(GuildPackets.ShowGuildInfo(null));
    }
    
    public static void GuildExpel(PacketReader packet, Player p) {
        int cid = packet.readInt();
        String name = packet.readMapleAsciiString();

        if (p.getGuildRank() > 2 || p.getGuildId() <= 0) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to leave guild without being in one");
            return;
        }
        GuildService.expelMember(p.getMGC(), name, cid);
    }
    
    public static void GuildChangeRankString(PacketReader packet, Player p) {
        if (p.getGuild() != null || p.getGuildRank() > 2) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to edit guild titles without having privileges");
            return;
        }
        String ranks[] = new String[5];
        for (int i = 0; i < 5; i++) {
            ranks[i] = packet.readMapleAsciiString();
            if (ranks[i].length() > 12 || (i <= 2 || i > 2 && !ranks[i].isEmpty()) && ranks[i].length() < 4) {
                p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to set invalid guild title");
                return;
            }
        }

        GuildService.changeRankTitle(p.getGuildId(), ranks);
    }
    
    public static void GuildChangePlayerRank(PacketReader packet, Player p) {
        int cid = packet.readInt();
        byte newRank = packet.readByte();

        if ((newRank <= 1 || newRank > 5) || p.getGuildRank() > 2 || (newRank <= 2 && p.getGuildRank() != 1) || p.getGuildId() <= 0) {
            return;
        }

        GuildService.changeRank(p.getGuildId(), cid, newRank); 
    }
    
    public static void GuildChangeEmblem(PacketReader packet, Player p) {
        if (p.getGuildId() <= 0 || p.getGuildRank() != 1 || p.getMapId() != MapConstants.GUILD_ROOM) {
            return;
        }

        if (p.getMeso() < GameConstants.GUILD_CHANGEEMBLEM_COST) {
            p.dropMessage(1, "You do not have enough mesos to change emblem.");
            return;
        }
        
        GuildService.setGuildEmblem(p.getGuildId(), packet.readShort(), packet.readByte(), packet.readShort(), packet.readByte());

        p.gainMeso(-GameConstants.GUILD_CHANGEEMBLEM_COST, true, false, true);
        respawnPlayer(p);
    }
    
    public static void GuildChangeNotice(PacketReader packet, Player p) {
        final String notice = packet.readMapleAsciiString();
        
        if (notice.length() > 100 || p.getGuildId() <= 0 || p.getGuildRank() > 2) {
            return;
        }
        
        GuildService.setGuildNotice(p.getGuildId(), notice);
    }
    
    public static void GuildContractResponse(PacketReader packet, Player p) {
        int characterId = packet.readInt();
        boolean accept = packet.readBool();
        if (characterId != p.getId()) {
            return;
        }
        MapleGuildContract.receivedVote(p.getClient(), p.getParty(), accept, characterId);
    }
    
    public static void DenyGuildRequest(PacketReader packet, Client c) {
        packet.readByte();
        String from = packet.readMapleAsciiString();
        Player cfrom = c.getChannelServer().getPlayerStorage().getCharacterByName(from);
        if (cfrom != null) {
            cfrom.getClient().getSession().write(GuildPackets.DenyGuildInvitation(c.getPlayer().getName()));
        }
    }  
    
    public static final void respawnPlayer(final Player p) {
        if (p.getMap() == null) {
            return;
        }
        p.getMap().broadcastMessage(GuildPackets.LoadGuildName(p));
        p.getMap().broadcastMessage(GuildPackets.LoadGuildIcon(p));
    }
    
    private static class Invited {
        public String name;
        public int gid;
        public long expiration;

        public Invited(String n, int id) {
            name = n.toLowerCase();
            gid = id;
            expiration = System.currentTimeMillis() + 60 * 60 * 1000; 
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Invited)) {
                return false;
            }
            Invited oth = (Invited) other;
            return (gid == oth.gid && name.equals(oth));
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 73 * hash + Objects.hashCode(this.name);
            hash = 73 * hash + this.gid;
            return hash;
        }
    } 
}
