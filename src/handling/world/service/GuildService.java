/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package handling.world.service;

import community.MapleGuild;
import community.MapleGuildBBS;
import community.MapleGuildCharacter;
import community.MapleGuildSummary;
import static handling.world.World.getStorage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import packet.creators.GuildPackets;
import packet.transfer.write.OutPacket;
import client.player.Player;
import java.util.HashSet;
import java.util.Set;

public class GuildService {
    
    private static final Set<Integer> queuedGuilds = new HashSet<>();
    private static final Map<Integer, MapleGuild> guilds = new LinkedHashMap<>();
    private static final ReentrantReadWriteLock guildLock = new ReentrantReadWriteLock();

    public static void addLoadedGuild(MapleGuild g) {
        if (g.isProper()) {
            guilds.put(g.getId(), g);
        }
    }

    public static int createGuild(int leaderId, String name) {
        return MapleGuild.createGuild(leaderId, name);
    }
    
    public static MapleGuild getGuild(int id) {
        synchronized (guilds) {
            if (guilds.get(id) != null) {
                return guilds.get(id);
            }
            return null;
        }
    }
    
    public static MapleGuild getGuild(Player mc) {
        return getGuild(mc.getGuildId(), mc.getClient().getChannel());
    }

    public static MapleGuild getGuild(int id, int channel) {
        MapleGuild ret = null;
        guildLock.readLock().lock();
        try {
            ret = guilds.get(id);
        } finally {
            guildLock.readLock().unlock();
        }
        if (ret == null) {
            guildLock.writeLock().lock();
            try {
                ret = new MapleGuild(id, channel);
                if (ret == null || ret.getId() <= 0 || !ret.isProper()) { 
                    return null;
                }
                guilds.put(id, ret);
            } finally {
                guildLock.writeLock().unlock();
            }
        }
        return ret; 
    }

    public static MapleGuild getGuildByName(String guildName) {
        guildLock.readLock().lock();
        try {
            for (MapleGuild g : guilds.values()) {
                if (g.getName().equalsIgnoreCase(guildName)) {
                    return g;
                }
            }
            return null;
        } finally {
            guildLock.readLock().unlock();
        }
    }

    public static boolean isGuildQueued(int guildId) {
        return queuedGuilds.contains(guildId);
    }

    public static void putGuildQueued(int guildId) {
        queuedGuilds.add(guildId);
    }

    public static void removeGuildQueued(int guildId) {
        queuedGuilds.remove(guildId);
    }

    public static void setGuildMemberOnline(MapleGuildCharacter mc, boolean bOnline, int channel) {
        MapleGuild g = getGuild(mc.getGuildId());
        if (g != null) {
            g.setOnline(mc.getId(), bOnline, channel);
        }
    }

    public static void guildPacket(int gid, OutPacket message) {
        MapleGuild g = getGuild(gid);
        if (g != null) {
            g.broadcast(message);
        }
    }

    public static int addGuildMember(MapleGuildCharacter mc) {
        MapleGuild g = getGuild(mc.getGuildId());
        if (g != null) {
            return g.addGuildMember(mc);
        }
        return 0;
    }

    public static void leaveGuild(MapleGuildCharacter mc) {
        MapleGuild g = getGuild(mc.getGuildId());
        if (g != null) {
            g.leaveGuild(mc);
        }
    }

    public static void guildChat(int gid, String name, int cid, String msg) {
        MapleGuild g = getGuild(gid);
        if (g != null) {
            g.guildChat(name, cid, msg);
        }
    }

    public static void changeRank(int gid, int cid, int newRank) {
        MapleGuild g = getGuild(gid);
        if (g != null) {
            g.changeRank(cid, newRank);
        }
    }

    public static void expelMember(MapleGuildCharacter initiator, String name, int cid) {
        MapleGuild g = getGuild(initiator.getGuildId());
        if (g != null) {
            g.expelMember(initiator, name, cid);
        }
    }

    public static void setGuildNotice(int gid, String notice) {
        MapleGuild g = getGuild(gid);
        if (g != null) {
            g.setGuildNotice(notice);
        }
    }

    public static void memberLevelJobUpdate(MapleGuildCharacter mc) {
        MapleGuild g = getGuild(mc.getGuildId());
        if (g != null) {
            g.memberLevelJobUpdate(mc);
        }
    }

    public static void changeRankTitle(int gid, String[] ranks) {
        MapleGuild g = getGuild(gid);
        if (g != null) {
            g.changeRankTitle(ranks);
        }
    }

    public static void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
        MapleGuild g = getGuild(gid);
        if (g != null) {
            g.setGuildEmblem(bg, bgcolor, logo, logocolor);
        }
    }

    public static void setGuildName(int gid, String name) {
        MapleGuild g = getGuild(gid);
        if (g != null) {
            g.setGuildName(name);
        }
    }

    public static void disbandGuild(int gid) {
        MapleGuild g = getGuild(gid);
        guildLock.writeLock().lock();
        try {
            if (g != null) {
                g.disbandGuild();
                guilds.remove(gid);
            }
        } finally {
            guildLock.writeLock().unlock();
        }
    }

    public static void deleteGuildCharacter(int gid, int charid) {
        MapleGuild g = getGuild(gid);
        if (g != null) {
            MapleGuildCharacter mc = g.getMGC(charid);
            if (mc != null) {
                if (mc.getGuildRank() > 1)  {
                    g.leaveGuild(mc);
                } else {
                    g.disbandGuild();
                }
            }
        }
    }

    public static boolean increaseGuildCapacity(int gid) {
        MapleGuild g = getGuild(gid);
        if (g != null) {
            return g.increaseCapacity();
        }
        return false;
    }

    public static void gainGP(int gid, int amount) {
        MapleGuild g = getGuild(gid);
        if (g != null) {
            g.gainGP(amount);
        }
    }

    public static int getGP(final int gid) {
        final MapleGuild g = getGuild(gid);
        if (g != null) {
            return g.getGP();
        }
        return 0;
    }

    public static int getInvitedId(final int gid) {
        final MapleGuild g = getGuild(gid);
        if (g != null) {
            return g.getInvitedId();
        }
        return 0;
    }

    public static void setInvitedId(final int gid, final int inviteid) {
        final MapleGuild g = getGuild(gid);
        if (g != null) {
            g.setInvitedId(inviteid);
        }
    }

    public static int getGuildLeader(final String guildName) {
        final MapleGuild mga = getGuildByName(guildName);
        if (mga != null) {
            return mga.getLeaderId();
        }
        return 0;
    }

    public static void save() {
        System.out.println("Saving guilds...");
        guildLock.writeLock().lock();
        try {
            guilds.values().forEach((a) -> {
                a.writeToDB(false);
            });
        } finally {
            guildLock.writeLock().unlock();
        }
    }

    public static List<MapleGuildBBS> getBBS(final int gid) {
        final MapleGuild g = getGuild(gid);
        if (g != null) {
            return g.getBBS();
        }
        return null;
    }

    public static int addBBSThread(final int gid, final String title, final String text, final int icon, final boolean bNotice, final int posterID) {
        final MapleGuild g = getGuild(gid);
        if (g != null) {
            return g.addBBSThread(title, text, icon, bNotice, posterID);
        }
        return -1;
    }

    public static void editBBSThread(final int gid, final int localthreadid, final String title, final String text, final int icon, final int posterID, final int guildRank) {
        final MapleGuild g = getGuild(gid);
        if (g != null) {
            g.editBBSThread(localthreadid, title, text, icon, posterID, guildRank);
        }
    }

    public static void deleteBBSThread(final int gid, final int localthreadid, final int posterID, final int guildRank) {
        final MapleGuild g = getGuild(gid);
        if (g != null) {
            g.deleteBBSThread(localthreadid, posterID, guildRank);
        }
    }

    public static void addBBSReply(final int gid, final int localthreadid, final String text, final int posterID) {
        final MapleGuild g = getGuild(gid);
        if (g != null) {
            g.addBBSReply(localthreadid, text, posterID);
        }
    }

    public static void deleteBBSReply(final int gid, final int localthreadid, final int replyid, final int posterID, final int guildRank) {
        final MapleGuild g = getGuild(gid);
        if (g != null) {
            g.deleteBBSReply(localthreadid, replyid, posterID, guildRank);
        }
    }

    public static void changeEmblem(int gid, int affectedPlayers, MapleGuildSummary mgs) {
        BroadcastService.sendGuildPacket(affectedPlayers, GuildPackets.GuildEmblemChange(gid, mgs.getLogoBG(), mgs.getLogoBGColor(), mgs.getLogo(), mgs.getLogoColor()), -1, gid);
        setGuildAndRank(affectedPlayers, -1, -1, -1);	
    }

    public static void changeName(int gid, int affectedPlayers, MapleGuildSummary mgs) {
        setGuildAndRank(affectedPlayers, -1, -1, -1);
    }

    public static void setGuildAndRank(int cid, int guildid, int rank, int alliancerank) {
        int ch = FindService.findChannel(cid);
        if (ch == -1) {
            return;
        }
        Player mc = getStorage(ch).getCharacterById(cid);
        if (mc == null) {
            return;
        }
        boolean bDifferentGuild;
        if (guildid == -1 && rank == -1) {
            bDifferentGuild = true;
        } else {
            bDifferentGuild = guildid != mc.getGuildId();
            mc.setGuildId(guildid);
            mc.setGuildRank((byte) rank);
            mc.setAllianceRank((byte) alliancerank);
            mc.saveGuildStatus();
        }
        if (bDifferentGuild && ch > 0) {
            mc.getMap().broadcastMessage(mc, GuildPackets.LoadGuildName(mc), false);
            mc.getMap().broadcastMessage(mc, GuildPackets.LoadGuildIcon(mc), false);
        }
    }
}
