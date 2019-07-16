/*
This file was written by "StellarAshes" <stellar_dust@hotmail.com> 
as a part of the Guild package for
the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package community;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import client.player.Player;
import client.Client;
import client.player.PlayerNote;
import community.MapleGuildBBS.MapleBBSReply;
import database.DatabaseConnection;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import packet.transfer.write.OutPacket;
import handling.channel.ChannelServer;
import handling.channel.PlayerStorage;
import handling.world.service.AllianceService;
import handling.world.service.BroadcastService;
import handling.world.service.GuildService;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import packet.creators.GuildPackets;
import packet.creators.PacketCreator;
import packet.transfer.write.WritingPacket;

public final class MapleGuild implements java.io.Serializable {
    
    private static enum BCOp {
        NONE,
        DISBAND,
        EMBELMCHANGE,
        NAMECHANGE
    }
       
    public static final long serialVersionUID = 6322150443228168192L;
    private final List<MapleGuildCharacter> members = new CopyOnWriteArrayList<>();
    private final String rankTitles[] = new String[5];
    private String name, notice;
    private int id, channel, gp, emblemFg, emblemFgC, leader, capacity, emblemBg, emblemBgC, signature;
    private boolean bDirty = true, proper = true;
    private int allianceid = 0, invitedid = 0;
    private final Map<Integer, MapleGuildBBS> bbs = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock rL = lock.readLock(), wL = lock.writeLock();
    private boolean init = false;


   public MapleGuild(final int guildid, int channel) {
        super();
        this.channel = channel;
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM guilds WHERE guildid = ?");
            ps.setInt(1, guildid);
            ResultSet rs = ps.executeQuery();

            if (!rs.first()) {
                rs.close();
                ps.close();
                id = -1;
                return;
            }
            id = guildid;
            name = rs.getString("name");
            gp = rs.getInt("GP");
            emblemFg = rs.getInt("logo");
            emblemFgC = rs.getInt("logoColor");
            emblemBg = rs.getInt("logoBG");
            emblemBgC = rs.getInt("logoBGColor");
            capacity = rs.getInt("capacity");
            rankTitles[0] = rs.getString("rank1title");
            rankTitles[1] = rs.getString("rank2title");
            rankTitles[2] = rs.getString("rank3title");
            rankTitles[3] = rs.getString("rank4title");
            rankTitles[4] = rs.getString("rank5title");
            leader = rs.getInt("leader");
            notice = rs.getString("notice");
            signature = rs.getInt("signature");
            allianceid = rs.getInt("alliance");
            rs.close();
            ps.close();

            ps = DatabaseConnection.getConnection().prepareStatement("SELECT id, name, level, job, guildrank, alliancerank FROM characters WHERE guildid = ? ORDER BY guildrank ASC, name ASC");
            ps.setInt(1, guildid);
            rs = ps.executeQuery();

            if (!rs.first()) {
                System.err.println("No members in guild " + id + ".  Impossible... guild is disbanding");
                rs.close();
                ps.close();
                writeToDB(true);
                proper = false;
                return;
            }
            boolean leaderCheck = false;
            do {
                if (rs.getInt("id") == leader) {
                    leaderCheck = true;
                }
                members.add(new MapleGuildCharacter(rs.getInt("id"), rs.getInt("level"), rs.getString("name"), (byte) -1, rs.getInt("job"), rs.getByte("guildrank"), guildid, rs.getByte("alliancerank"), false));
            } while (rs.next());
            rs.close();
            ps.close();

            if (!leaderCheck) {
                System.err.println("Leader " + leader + " isn't in guild " + id + ".  Impossible... guild is disbanding.");
                writeToDB(true);
                proper = false;
                return;
            }


            ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM bbs_threads WHERE guildid = ? ORDER BY localthreadid DESC");
            ps.setInt(1, guildid);
            rs = ps.executeQuery();
            while (rs.next()) {
                final MapleGuildBBS thread = new MapleGuildBBS(rs.getInt("localthreadid"), rs.getString("name"), rs.getString("startpost"), rs.getLong("timestamp"),
                        guildid, rs.getInt("postercid"), rs.getInt("icon"));
                try (PreparedStatement pse = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM bbs_replies WHERE threadid = ?")) {
                    pse.setInt(1, rs.getInt("threadid"));
                    try (ResultSet rse = pse.executeQuery()) {
                        while (rse.next()) {
                            thread.replies.put(thread.replies.size(), new MapleBBSReply(thread.replies.size(), rse.getInt("postercid"), rse.getString("content"), rse.getLong("timestamp")));
                        }
                    }
                }
                bbs.put(rs.getInt("localthreadid"), thread);
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
            System.err.println("unable to read guild information from sql :" + se);
        }
    }
   
    private void buildNotifications() {
        if (!bDirty) {
            return;
        }
        final List<Integer> mem = new LinkedList<>();
        final Iterator<MapleGuildCharacter> toRemove = members.iterator();
        while (toRemove.hasNext()) {
            MapleGuildCharacter mgc = toRemove.next();
            if (!mgc.isOnline()) {
                continue;
            }
            if (mem.contains(mgc.getId()) || mgc.getGuildId() != id) {
                members.remove(mgc);
                continue;
            }
            mem.add(mgc.getId());

        }
        bDirty = false;
    }
    
    public boolean isProper() {
        return proper;
    }
    
    public final boolean isInit() {
        return init;
    }

    public void writeToDB() {
        writeToDB(false);
    }

    public void writeToDB(boolean bDisband) {
        try {
            Connection con = DatabaseConnection.getConnection();
            if (!bDisband) {
                StringBuilder builder = new StringBuilder();
                builder.append("UPDATE guilds SET GP = ?, logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ?, ");
                for (int i = 0; i < 5; i++) {
                    builder.append("rank").append(i).append(1).append("title = ?, ");
                }
                builder.append("capacity = ?, notice = ? WHERE guildid = ?");
                try (PreparedStatement ps = con.prepareStatement(builder.toString())) {
                    ps.setInt(1, gp);
                    ps.setInt(2, emblemFg);
                    ps.setInt(3, emblemFgC);
                    ps.setInt(4, emblemBg);
                    ps.setInt(5, emblemBgC);
                    for (int i = 6; i < 11; i++) {
                        ps.setString(i, rankTitles[i - 6]);
                    }
                    ps.setInt(11, capacity);
                    ps.setString(12, notice);
                    ps.setInt(13, this.id);
                    ps.execute();
                }
            } else {
                PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?");
                ps.setInt(1, this.id);
                ps.execute();
                ps.close();
                ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
                ps.setInt(1, this.id);
                ps.execute();
                ps.close();
                this.broadcast(GuildPackets.GuildDisband(this.id));
            }
        } catch (SQLException se) {
        }
    }

    public int getId() {
        return id;
    }

    public int getLeaderId() {
        return leader;
    }
    
     public final Player getLeader(final Client c) {
        return c.getChannelServer().getPlayerStorage().getCharacterById(leader);
    }

    public int getGP() {
        return gp;
    }

    public int getEmblemDesign() {
        return emblemFg;
    }

    public void setLogo(int l) {
        emblemFg = l;
    }

    public int getEmblemDesignColor() {
        return emblemFgC;
    }

    public void setEmblemDesignColor(int c) {
        emblemFgC = c;
    }

    public int getEmblemBackground() {
        return emblemBg;
    }

    public void setEmblemBackground(int bg) {
        emblemBg = bg;
    }

    public int getEmblemBackgroundColor() {
        return emblemBgC;
    }

    public void setLogoBGColor(int c) {
        emblemBgC = c;
    }

    public String getNotice() {
        if (notice == null) {
            return "";
        }
        return notice;
    }

    public String getName() {
        return name;
    }
    
    public int getIncreaseGuildCost(int size) {
        return 500000 * (size - 6) / 6;
    }
    
    public final void addMemberData(final WritingPacket wp) {
        wp.write(members.size());

        members.forEach((mgc) -> {
            wp.writeInt(mgc.getId());
        });
        for (final MapleGuildCharacter mgc : members) {
            wp.writeAsciiString(mgc.getName(), 13);
            wp.writeInt(mgc.getJobId());
            wp.writeInt(mgc.getLevel());
            wp.writeInt(mgc.getGuildRank());
            wp.writeInt(mgc.isOnline() ? 1 : 0);
            wp.writeInt(signature);
            wp.writeInt(mgc.getAllianceRank());
        }
    }

    public java.util.Collection<MapleGuildCharacter> getMembers() {
        return java.util.Collections.unmodifiableCollection(members);
    }

    public int getCapacity() {
        return capacity;
    }

    public int getSignature() {
        return signature;
    }

    public final void broadcast(final OutPacket packet) {
        broadcast(packet, -1, BCOp.NONE);
    }

    public final void broadcast(final OutPacket packet, final int exception) {
        broadcast(packet, exception, BCOp.NONE);
    }

    /**
     *
     * @param packet
     * @param exceptionId
     * @param bcop
     */
    public final void broadcast(final OutPacket packet, final int exceptionId, final BCOp bcop) {
        wL.lock();
        try {
            buildNotifications();
        } finally {
            wL.unlock();
        }

        rL.lock();
        try {
            members.forEach((mgc) -> {
                if (bcop == BCOp.DISBAND) {
                    if (mgc.isOnline()) {
                        GuildService.setGuildAndRank(mgc.getId(), 0, 5, 5);
                    } else {
                        setOfflineGuildStatus(0, (byte) 5, (byte) 5, mgc.getId());
                    }
                } else if (mgc.isOnline() && mgc.getId() != exceptionId) {
                    if (null == bcop) {
                        BroadcastService.sendGuildPacket(mgc.getId(), packet, exceptionId, id);
                    } else switch (bcop) {
                        case EMBELMCHANGE:
                            GuildService.changeEmblem(id, mgc.getId(), new MapleGuildSummary(this));
                            break;
                        case NAMECHANGE:
                            GuildService.changeName(id, mgc.getId(), new MapleGuildSummary(this));
                            break;
                        default:
                            BroadcastService.sendGuildPacket(mgc.getId(), packet, exceptionId, id);
                            break;
                    }
                }
            });
        } finally {
            rL.unlock();
        }

    }

    public void guildMessage(OutPacket serverNotice) {
        for (MapleGuildCharacter mgc : members) {
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                if (cs.getPlayerStorage().getCharacterById(mgc.getId()) != null) {
                    Player chr = cs.getPlayerStorage().getCharacterById(mgc.getId());
                    chr.getClient().getSession().write(serverNotice);
                    break;
                }
            }
        }
    }
    
    public void dropMessage(int type, String message) {
        for (MapleGuildCharacter mgc : members) {
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                if (cs.getPlayerStorage().getCharacterById(mgc.getId()) != null) {
                    Player chr = cs.getPlayerStorage().getCharacterById(mgc.getId());
                    chr.dropMessage(type, message);
                    break;
                }
            }
        }
    }

    public final void setOnline(final int cid, final boolean online, final int channel) {
        boolean bBroadcast = true;
        for (MapleGuildCharacter mgc : members) {
            if (mgc.getGuildId() == id && mgc.getId() == cid) {
                if (mgc.isOnline() == online) {
                    bBroadcast = false;
                }
                mgc.setOnline(online);
                mgc.setChannel((byte) channel);
                break;
            }
        }
        if (bBroadcast) {
            broadcast(GuildPackets.GuildMemberOnline(id, cid, online), cid);
            if (allianceid > 0) {
                AllianceService.sendGuild(GuildPackets.AllianceMemberOnline(allianceid, id, cid, online), id, allianceid);
            }
        }
        bDirty = true;
        init = true;
    }

    public void guildChat(String name, int cid, String msg) {
        this.broadcast(PacketCreator.PrivateChatMessage(name, msg, 2), cid);
    }
    
    public final void allianceChat(final String name, final int cid, final String msg) {
        broadcast(PacketCreator.PrivateChatMessage(name, msg, 3), cid);
    }

    public String getRankTitle(int rank) {
        return rankTitles[rank - 1];
    }

    public static final int createGuild(final int leaderId, final String name) {
        if (name.length() > 12) {
            return 0;
        }
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if (rs.first()) {
                rs.close();
                ps.close();
                return 0;
            }
            ps.close();
            rs.close();

            ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO guilds (`leader`, `name`, `signature`, `alliance`) VALUES (?, ?, ?, 0)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, leaderId);
            ps.setString(2, name);
            ps.setInt(3, (int) (System.currentTimeMillis() / 1000));
            ps.execute();
            rs = ps.getGeneratedKeys();
            int ret = 0;
            if (rs.next()) {
                ret = rs.getInt(1);
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException se) {
            System.err.println("SQL THROW");
            return 0;
        }
    }

    public final int addGuildMember(final MapleGuildCharacter mgc) {
        wL.lock();
        try {
            if (members.size() >= capacity) {
                return 0;
            }
            for (int i = members.size() - 1; i >= 0; i--) {
                if (members.get(i).getGuildRank() < 5 || members.get(i).getName().compareTo(mgc.getName()) < 0) {
                    members.add(i + 1, mgc);
                    bDirty = true;
                    break;
                }
            }
        } finally {
            wL.unlock();
        }
        broadcast(GuildPackets.NewGuildMember(mgc));
        if (allianceid > 0) {
            AllianceService.sendGuild(allianceid);
        }
        return 1;
    }

    public final void leaveGuild(final MapleGuildCharacter mgc) {
        broadcast(GuildPackets.MemberLeft(mgc, false));
        wL.lock();
        try {
            bDirty = true;
            members.remove(mgc);
            if (mgc.isOnline()) {
                GuildService.setGuildAndRank(mgc.getId(), 0, 5, 5);
            } else {
                setOfflineGuildStatus((short) 0, (byte) 5, (byte) 5, mgc.getId());
            }
            if (allianceid > 0) {
                AllianceService.sendGuild(allianceid);
            }
        } finally {
            wL.unlock();
        }
    }
    
    public static void setOfflineGuildStatus(int guildid, byte guildrank, byte alliancerank, int cid) {
        try {
            java.sql.Connection con = DatabaseConnection.getConnection();
            try (java.sql.PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ?, alliancerank = ? WHERE id = ?")) {
                ps.setInt(1, guildid);
                ps.setInt(2, guildrank);
                ps.setInt(3, alliancerank);
                ps.setInt(4, cid);
                ps.execute();
            }
        } catch (SQLException se) {
            System.out.println("SQLException: " + se.getLocalizedMessage());
        }
    }
  
    public final void expelMember(final MapleGuildCharacter initiator, final String name, final int cid) {
        wL.lock();
        try {
            final Iterator<MapleGuildCharacter> itr = members.iterator();
            while (itr.hasNext()) {
                final MapleGuildCharacter mgc = itr.next();

                if (mgc.getId() == cid && initiator.getGuildRank() < mgc.getGuildRank()) {
                    broadcast(GuildPackets.MemberLeft(mgc, true));

                    bDirty = true;

                    if (allianceid > 0) {
                        AllianceService.sendGuild(allianceid);
                    }
                    if (mgc.isOnline()) {
                        GuildService.setGuildAndRank(cid, 0, 5, 5);
                    } else {
                        PlayerNote.sendNote(mgc.getName(), initiator.getName(), "You have been expelled from the guild.", 0);
                        setOfflineGuildStatus((short) 0, (byte) 5, (byte) 5, cid);
                    }
                    members.remove(mgc);
                    break;
                }
            }
        } finally {
            wL.unlock();
        }
    }

   public final void changeARank() {
        changeARank(false);
    }

    public final void changeARank(final boolean leader) {
        members.forEach((mgc) -> {
            if (this.leader == mgc.getId()) {
                changeARank(mgc.getId(), leader ? 1 : 2);
            } else {
                changeARank(mgc.getId(), 3);
            }
        });
    }

    public final void changeARank(final int newRank) {
        members.forEach((mgc) -> {
            changeARank(mgc.getId(), newRank);
        });
    }

    public final void changeARank(final int cid, final int newRank) {
        if (allianceid <= 0) {
            return;
        }
        for (final MapleGuildCharacter mgc : members) {
            if (cid == mgc.getId()) {
                if (mgc.isOnline()) {
                    GuildService.setGuildAndRank(cid, this.id, mgc.getGuildRank(), newRank);
                } else {
                    setOfflineGuildStatus((short) this.id, (byte) mgc.getGuildRank(), (byte) newRank, cid);
                }
                mgc.setAllianceRank((byte) newRank);
                AllianceService.sendGuild(allianceid);
                return;
            }
        }
        System.err.println("INFO: unable to find the correct id for changeRank({" + cid + "}, {" + newRank + "})");
    }

    public final void changeRank(final int cid, final int newRank) {
        for (final MapleGuildCharacter mgc : members) {
            if (cid == mgc.getId()) {
                if (mgc.isOnline()) {
                    GuildService.setGuildAndRank(cid, this.id, newRank, mgc.getAllianceRank());
                } else {
                    setOfflineGuildStatus((short) this.id, (byte) newRank, (byte) mgc.getAllianceRank(), cid);
                }
                mgc.setGuildRank((byte) newRank);
                broadcast(GuildPackets.ChangeRank(mgc));
                return;
            }
        }
        System.err.println("INFO: unable to find the correct id for changeRank({" + cid + "}, {" + newRank + "})");
    }
    
    public final void setGuildName(final String name) {
        this.name = name;
        broadcast(null, -1, BCOp.NAMECHANGE);
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE guilds SET `name` = ? WHERE guildid = ?")) {
                ps.setString(1, name);
                ps.setInt(2, id);
                ps.execute();
            }
        } catch (SQLException e) {
            System.err.println("Saving guild name ERROR. ");
        }
    }
    
    public final List<MapleGuildBBS> getBBS() {
        final List<MapleGuildBBS> ret = new ArrayList<>(bbs.values());
        Collections.sort(ret, new MapleGuildBBS.ThreadComparator());
        return ret;
    }
    
     public final int addBBSThread(final String title, final String text, final int icon, final boolean bNotice, final int posterID) {
        final int add = bbs.get(0) == null ? 1 : 0;  
        final int ret = bNotice ? 0 : Math.max(1, bbs.size() + add);
        bbs.put(ret, new MapleGuildBBS(ret, title, text, System.currentTimeMillis(), this.id, posterID, icon));
        return ret;
    }

    public final void editBBSThread(final int localthreadid, final String title, final String text, final int icon, final int posterID, final int guildRank) {
        final MapleGuildBBS thread = bbs.get(localthreadid);
        if (thread != null && (thread.ownerID == posterID || guildRank <= 2)) {
            bbs.put(localthreadid, new MapleGuildBBS(localthreadid, title, text, System.currentTimeMillis(), this.id, thread.ownerID, icon));
        }
    }

    public final void deleteBBSThread(final int localthreadid, final int posterID, final int guildRank) {
        final MapleGuildBBS thread = bbs.get(localthreadid);
        if (thread != null && (thread.ownerID == posterID || guildRank <= 2)) {
            bbs.remove(localthreadid);
        }
    }

    public final void addBBSReply(final int localthreadid, final String text, final int posterID) {
        final MapleGuildBBS thread = bbs.get(localthreadid);
        if (thread != null) {
            thread.replies.put(thread.replies.size(), new MapleBBSReply(thread.replies.size(), posterID, text, System.currentTimeMillis()));
        }
    }

    public final void deleteBBSReply(final int localthreadid, final int replyid, final int posterID, final int guildRank) {
        final MapleGuildBBS thread = bbs.get(localthreadid);
        if (thread != null) {
            final MapleBBSReply reply = thread.replies.get(replyid);
            if (reply != null && (reply.ownerID == posterID || guildRank <= 2)) {
                thread.replies.remove(replyid);
            }
        }
    }

    public final void setGuildNotice(final String notice) {
        this.notice = notice;
        broadcast(GuildPackets.GuildNotice(id, notice));
    }

    public final void memberLevelJobUpdate(final MapleGuildCharacter mgc) {
        for (final MapleGuildCharacter member : members) {
            if (member.getId() == mgc.getId()) {
                int oldLevel = member.getLevel();
                member.setJobId(mgc.getJobId());
                member.setLevel((short) mgc.getLevel());
                if (mgc.getLevel() > oldLevel) {
                    gainGP((mgc.getLevel() - oldLevel) * mgc.getLevel() / 10); 
                }
                broadcast(GuildPackets.GuildMemberLevelJobUpdate(mgc));
                if (allianceid > 0) {
                    AllianceService.sendGuild(GuildPackets.UpdateAlliance(mgc, allianceid), id, allianceid);
                }
                break;
            }
        }
    }

    public final void changeRankTitle(final String[] ranks) {
        for (int i = 0; i < 5; i++) {
            rankTitles[i] = ranks[i];
        }
        broadcast(GuildPackets.RankTitleChange(id, ranks));
    }

    public final void disbandGuild() {
        writeToDB(true);
        broadcast(null, -1, BCOp.DISBAND);
    }

    public final void setGuildEmblem(final short bg, final byte bgcolor, final short logo, final byte logocolor) {
        this.emblemBg = bg;
        this.emblemBgC = bgcolor;
        this.emblemFg = logo;
        this.emblemFgC = logocolor;
        broadcast(null, -1, BCOp.EMBELMCHANGE);

        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE guilds SET logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ? WHERE guildid = ?")) {
                ps.setInt(1, logo);
                ps.setInt(2, emblemFgC);
                ps.setInt(3, emblemBg);
                ps.setInt(4, emblemBgC);
                ps.setInt(5, id);
                ps.execute();
            }
        } catch (SQLException e) {
            System.err.println("Saving guild logo / BG colo ERROR");
        }
    }

    public final MapleGuildCharacter getMGC(final int cid) {
        for (final MapleGuildCharacter mgc : members) {
            if (mgc.getId() == cid) {
                return mgc;
            }
        }
        return null;
    }

     public final boolean increaseCapacity() {
        if (capacity >= 100 || ((capacity + 5) > 100)) {
            return false;
        }
        capacity += 5;
        broadcast(GuildPackets.GuildCapacityChange(this.id, this.capacity));

        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE guilds SET capacity = ? WHERE guildid = ?")) {
                ps.setInt(1, this.capacity);
                ps.setInt(2, this.id);
                ps.execute();
            }
        } catch (SQLException e) {
            System.err.println("Saving guild capacity ERROR");
        }
        return true;
    }

    public final void gainGP(int amount) {
        if (amount == 0) {
            return;
        }
        if (amount + gp < 0) {
            amount = -gp;
        } 
        gp += amount;
        broadcast(GuildPackets.UpdateGP(id, gp));
    }

    public static final MapleGuildResponse sendInvite(final Client c, final String targetName) {
        final Player mc = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);
        if (mc == null) {
            return MapleGuildResponse.NOT_IN_CHANNEL;
        }
        if (mc.getGuildId() > 0) {
            return MapleGuildResponse.ALREADY_IN_GUILD;
        }
        mc.getClient().getSession().write(GuildPackets.GuildInvite(c.getPlayer().getGuildId(), c.getPlayer().getName()));
        return null;
    }
    
    public void broadcastNameChanged() {
        PlayerStorage ps = ChannelServer.getInstance(channel).getPlayerStorage();
        
        for (MapleGuildCharacter mgc : getMembers()) {
            Player p = ps.getCharacterById(mgc.getId());
            if (p == null /*|| !p.isLoggedinWorld()*/) continue;

            OutPacket packet = GuildPackets.LoadGuildName(p);
            p.getMap().broadcastMessage(p, packet);
        }
    }
    
    public void broadcastEmblemChanged() {
        PlayerStorage ps = ChannelServer.getInstance(channel).getPlayerStorage();
        
        for (MapleGuildCharacter mgc : getMembers()) {
            Player p = ps.getCharacterById(mgc.getId());
            if (p == null /*|| !p.isLoggedinWorld()*/) continue;
            
            OutPacket packet = GuildPackets.LoadGuildIcon(p);
            p.getMap().broadcastMessage(p, packet);
        }
    }

   public static void displayGuildRanks(Client c, int npcid) {
        try {
            ResultSet rs;
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `name`, `GP`, `logoBG`, `logoBGColor`, " + "`logo`, `logoColor` FROM guilds ORDER BY `GP` DESC LIMIT 50")) {
                rs = ps.executeQuery();
                c.getSession().write(GuildPackets.ShowGuildRanks(npcid, rs));
            }
            rs.close();
        } catch (SQLException e) {
        }
    }

    public int getAllianceId() {
        return this.allianceid;
    }

    public int getInvitedId() {
        return this.invitedid;
    }

    public void setInvitedId(int iid) {
        this.invitedid = iid;
    }

    public void setAllianceId(int a) {
        this.allianceid = a;
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE guilds SET alliance = ? WHERE guildid = ?")) {
                ps.setInt(1, a);
                ps.setInt(2, id);
                ps.execute();
            }
        } catch (SQLException e) {
            System.err.println("Saving allianceid ERROR" + e);
        }
    }
}
