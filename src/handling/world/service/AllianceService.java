/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package handling.world.service;

import community.MapleGuild;
import community.MapleGuildAlliance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import packet.creators.GuildPackets;
import packet.creators.PacketCreator;
import packet.transfer.write.OutPacket;

public class AllianceService {
        
    private static final Map<Integer, MapleGuildAlliance> alliances = new LinkedHashMap<>();
    public static final ReentrantReadWriteLock allianceLocks = new ReentrantReadWriteLock();

    static {
        Collection<MapleGuildAlliance> allGuilds = MapleGuildAlliance.loadAll();
        allGuilds.forEach((g) -> {
            alliances.put(g.getId(), g);
        });
    }
    
    public static MapleGuildAlliance getAlliance(int id) {
        synchronized (alliances) {
            if (alliances.get(id) != null) {
                return alliances.get(id);
            }
            return null;
        }
    }

    public static MapleGuildAlliance getAlliance(final int allianceid, int channel) {
        MapleGuildAlliance ret = null;
        allianceLocks.readLock().lock();
        try {
            ret = alliances.get(allianceid);
        } finally {
            allianceLocks.readLock().unlock();
        }
        if (ret == null) {
            allianceLocks.writeLock().lock();
            try {
                ret = new MapleGuildAlliance(allianceid, channel);
                if (ret == null || ret.getId() <= 0) {
                    return null;
                }
                alliances.put(allianceid, ret);
            } finally {
                allianceLocks.writeLock().unlock();
            }
        }
        return ret;
    }

    public static int getAllianceLeader(final int allianceid) {
        final MapleGuildAlliance mga = alliances.get(allianceid);
        if (mga != null) {
            return mga.getLeaderId();
        }
        return 0;
    }

    public static void updateAllianceRanks(final int allianceid, final String[] ranks) {
        final MapleGuildAlliance mga = alliances.get(allianceid);
        if (mga != null) {
            mga.setRank(ranks);
        }
    }

    public static void updateAllianceNotice(final int allianceid, final String notice) {
        final MapleGuildAlliance mga = alliances.get(allianceid);
        if (mga != null) {
            mga.setNotice(notice);
        }
    }

    public static boolean canInvite(final int allianceid) {
        final MapleGuildAlliance mga = alliances.get(allianceid);
        if (mga != null) {
            return mga.getCapacity() > mga.getNoGuilds();
        }
        return false;
    }

    public static boolean changeAllianceLeader(final int allianceid, final int cid) {
        final MapleGuildAlliance mga = alliances.get(allianceid);
        if (mga != null) {
            return mga.setLeaderId(cid);
        }
        return false;
    }

    public static boolean changeAllianceRank(final int allianceid, final int cid, final int change) {
        final MapleGuildAlliance mga = alliances.get(allianceid);
        if (mga != null) {
            return mga.changeAllianceRank(cid, change);
        }
        return false;
    }

    public static boolean changeAllianceCapacity(final int allianceid) {
        final MapleGuildAlliance mga = alliances.get(allianceid);
        if (mga != null) {
            return mga.setCapacity();
        }
        return false;
    }

    public static boolean disbandAlliance(final int allianceid) {
        final MapleGuildAlliance mga = alliances.get(allianceid);
        if (mga != null) {
            return mga.disband();
        }
        return false;
    }

    public static boolean addGuildToAlliance(final int allianceid, final int gid) {
        final MapleGuildAlliance mga = alliances.get(allianceid);
        if (mga != null) {
            return mga.addGuild(gid);
        }
        return false;
    }

    public static boolean removeGuildFromAlliance(final int allianceid, final int gid, final boolean expelled) {
        final MapleGuildAlliance mga = alliances.get(allianceid);
        if (mga != null) {
            return mga.removeGuild(gid, expelled);
        }
        return false;
    }

    public static void sendGuild(final int allianceid) {
        final MapleGuildAlliance alliance = alliances.get(allianceid);
        if (alliance != null) {
            sendGuild(GuildPackets.GetAllianceUpdate(alliance), -1, allianceid);
            sendGuild(GuildPackets.GetGuildAlliance(alliance), -1, allianceid);
        }
    }

    public static void sendGuild(final OutPacket packet, final int exceptionId, final int allianceid) {
        final MapleGuildAlliance alliance = alliances.get(allianceid);
        if (alliance != null) {
            for (int i = 0; i < alliance.getNoGuilds(); i++) {
                int gid = alliance.getGuildId(i);
                if (gid > 0 && gid != exceptionId) {
                    GuildService.guildPacket(gid, packet);
                }
            }
        }
    }

    public static boolean createAlliance(final String alliancename, final int cid, final int cid2, final int gid, final int gid2) {
        final int allianceid = MapleGuildAlliance.createToDb(cid, alliancename, gid, gid2);
        if (allianceid <= 0) {
            return false;
        }
        final MapleGuild g = GuildService.getGuild(gid), g_ = GuildService.getGuild(gid2);
        g.setAllianceId(allianceid);
        g_.setAllianceId(allianceid);
        g.changeARank(true);
        g_.changeARank(false);

        final MapleGuildAlliance alliance = alliances.get(allianceid);

        sendGuild(GuildPackets.CreateGuildAlliance(alliance), -1, allianceid);
        sendGuild(GuildPackets.GetAllianceInfo(alliance), -1, allianceid);
        sendGuild(GuildPackets.GetGuildAlliance(alliance), -1, allianceid);
        sendGuild(GuildPackets.ChangeAlliance(alliance, true), -1, allianceid);
        return true;
    }

    public static void allianceChat(final int gid, final String name, final int cid, final String msg) {
        final MapleGuild g = GuildService.getGuild(gid);
        if (g != null) {
            final MapleGuildAlliance ga = alliances.get(g.getAllianceId());
            if (ga != null) {
                for (int i = 0; i < ga.getNoGuilds(); i++) {
                    final MapleGuild g_ = GuildService.getGuild(ga.getGuildId(i));
                    if (g_ != null) {
                        g_.allianceChat(name, cid, msg);
                    }
                }
            }
        }
    }

    public static void setNewAlliance(final int gid, final int allianceid) {
        final MapleGuildAlliance alliance = alliances.get(allianceid);
        final MapleGuild guild = GuildService.getGuild(gid);
        if (alliance != null && guild != null) {
            for (int i = 0; i < alliance.getNoGuilds(); i++) {
                if (gid == alliance.getGuildId(i)) {
                    guild.setAllianceId(allianceid);
                    guild.broadcast(GuildPackets.GetAllianceInfo(alliance));
                    guild.broadcast(GuildPackets.GetGuildAlliance(alliance));
                    guild.broadcast(GuildPackets.ChangeAlliance(alliance, true));
                    guild.changeARank();
                    guild.writeToDB(false);
                } else {
                    final MapleGuild g_ = GuildService.getGuild(alliance.getGuildId(i));
                    if (g_ != null) {
                        g_.broadcast(GuildPackets.AddGuildToAlliance(alliance, guild));
                        g_.broadcast(GuildPackets.ChangeGuildInAlliance(alliance, guild, true));
                    }
                }
            }
        }
    }

    public static void setOldAlliance(final int gid, final boolean expelled, final int allianceid) {
        final MapleGuildAlliance alliance = alliances.get(allianceid);
        final MapleGuild g_ = GuildService.getGuild(gid);
        if (alliance != null) {
            for (int i = 0; i < alliance.getNoGuilds(); i++) {
                final MapleGuild guild = GuildService.getGuild(alliance.getGuildId(i));
                if (guild == null) {
                    if (gid != alliance.getGuildId(i)) {
                        alliance.removeGuild(gid, false);
                    }
                    continue; 
                }
                if (g_ == null || gid == alliance.getGuildId(i)) {
                    guild.changeARank(5);
                    guild.setAllianceId(0);
                    guild.broadcast(GuildPackets.DisbandAlliance(allianceid));
                } else if (g_ != null) {
                    guild.broadcast(PacketCreator.ServerNotice(5, "[" + g_.getName() + "] Guild has left the alliance."));
                    guild.broadcast(GuildPackets.ChangeGuildInAlliance(alliance, g_, false));
                    guild.broadcast(GuildPackets.RemoveGuildFromAlliance(alliance, g_, expelled));
                }

            }
        }

        if (gid == -1) {
            allianceLocks.writeLock().lock();
            try {
                alliances.remove(allianceid);
            } finally {
                allianceLocks.writeLock().unlock();
            }
        }
    }

    public static List<OutPacket> getAllianceInfo(final int allianceid, final boolean start) {
        List<OutPacket> ret = new ArrayList<>();
        final MapleGuildAlliance alliance = alliances.get(allianceid);
        if (alliance != null) {
            if (start) {
                ret.add(GuildPackets.GetAllianceInfo(alliance));
                ret.add(GuildPackets.GetGuildAlliance(alliance));
            }
            ret.add(GuildPackets.GetAllianceUpdate(alliance));
        }
        return ret;
    }

    public static void save() {
        System.out.println("Saving alliances...");
        allianceLocks.writeLock().lock();
        try {
            alliances.values().forEach((a) -> {
                a.saveToDb();
            });
        } finally {
            allianceLocks.writeLock().unlock();
        }
    }
    
}
