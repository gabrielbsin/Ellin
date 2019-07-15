/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packet.creators;

import client.player.Player;
import community.MapleGuildAlliance;
import community.MapleGuild;
import community.MapleGuildCharacter;
import handling.channel.handler.ChannelHeaders.GuildHeaders;
import handling.world.service.GuildService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import packet.opcode.SendPacketOpcode;
import packet.transfer.write.OutPacket;
import packet.transfer.write.WritingPacket;
import tools.StringUtil;

public class GuildPackets {
    
    public static OutPacket ShowGuildInfo(Player p) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(0x1A);
        if (p == null) { 
            wp.write(0);
            return wp.getPacket();
        }
        MapleGuild g = GuildService.getGuild(p.getGuildId());
        if (g == null) { 
            wp.write(0);
            return wp.getPacket();
        } else {
            MapleGuildCharacter mgc = g.getMGC(p.getId());
            p.setGuildRank(mgc.getGuildRank());
        }
        wp.write(1); 
        GetGuildInfo(wp, g);
        return wp.getPacket();
    }
    
    public static OutPacket GuildMemberOnline(int gID, int cID, boolean bOnline) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(GuildHeaders.CHANNEL_CHANGE);
        wp.writeInt(gID);
        wp.writeInt(cID);
        wp.write(bOnline ? 1 : 0);
        return wp.getPacket();
    }

    public static OutPacket GuildInvite(int gID, String charName) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(GuildHeaders.INVITE_SENT);
        wp.writeInt(gID);
        wp.writeMapleAsciiString(charName);
        return wp.getPacket();
    }
    
    public static OutPacket DenyGuildInvitation(String charName) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(GuildHeaders.INVITE_DENIED);
        wp.writeMapleAsciiString(charName);
        return wp.getPacket();
    }
	
    public static OutPacket GenericGuildMessage(byte code) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(code);
        return wp.getPacket();
    }

    public static OutPacket NewGuildMember(MapleGuildCharacter mgc) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(GuildHeaders.JOINED_GUILD);
        wp.writeInt(mgc.getGuildId());
        wp.writeInt(mgc.getId());
        wp.writeAsciiString(StringUtil.getRightPaddedStr(mgc.getName(), '\0', 13));
        wp.writeInt(mgc.getJobId());
        wp.writeInt(mgc.getLevel());
        wp.writeInt(mgc.getGuildRank()); 
        wp.writeInt(mgc.isOnline() ? 1 : 0);
        wp.writeInt(1); 
        wp.writeInt(3);
        return wp.getPacket();
    }

    public static OutPacket MemberLeft(MapleGuildCharacter mgc, boolean bExpelled) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(bExpelled ? GuildHeaders.EXPELLED_FROM_GUILD : GuildHeaders.LEFT_GUILD);
        wp.writeInt(mgc.getGuildId());
        wp.writeInt(mgc.getId());
        wp.writeMapleAsciiString(mgc.getName());
        return wp.getPacket();
    }

    public static OutPacket ChangeRank(MapleGuildCharacter mgc) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(GuildHeaders.RANK_CHANGED);
        wp.writeInt(mgc.getGuildId());
        wp.writeInt(mgc.getId());
        wp.write(mgc.getGuildRank());
        return wp.getPacket();
    }

    public static OutPacket GuildNotice(int gID, String notice) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(GuildHeaders.NOTICE_CHANGED);
        wp.writeInt(gID);
        wp.writeMapleAsciiString(notice);
        return wp.getPacket();
    }

    public static OutPacket GuildMemberLevelJobUpdate(MapleGuildCharacter mgc) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(GuildHeaders.LEVEL_JOB_CHANGED);
        wp.writeInt(mgc.getGuildId());
        wp.writeInt(mgc.getId());
        wp.writeInt(mgc.getLevel());
        wp.writeInt(mgc.getJobId());
        return wp.getPacket();
    }

    public static OutPacket RankTitleChange(int gID, String[] ranks) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(GuildHeaders.RANK_TITLES_CHANGED);
        wp.writeInt(gID);
        for (int i = 0; i < 5; i++) {
                wp.writeMapleAsciiString(ranks[i]);
        }
        return wp.getPacket();
    }

    public static OutPacket GuildDisband(int gID) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(GuildHeaders.DISBANDED_GUILD);
        wp.writeInt(gID);
        wp.write(1);
        return wp.getPacket();
    }

    public static OutPacket GuildEmblemChange(int gID, short bg, byte bgColor, short logo, byte logoColor) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(GuildHeaders.EMBLEM_CHANGED);
        wp.writeInt(gID);
        wp.writeShort(bg);
        wp.write(bgColor);
        wp.writeShort(logo);
        wp.write(logoColor);
        return wp.getPacket();
    }

    public static OutPacket GuildCapacityChange(int gID, int capacity) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(GuildHeaders.CAPACITY_CHANGED);
        wp.writeInt(gID);
        wp.write(capacity);
        return wp.getPacket();
    }

    public static void AddThread(WritingPacket mplew, ResultSet rs) throws SQLException {
        mplew.writeInt(rs.getInt("localthreadid"));
        mplew.writeInt(rs.getInt("postercid"));
        mplew.writeMapleAsciiString(rs.getString("name"));
        mplew.writeLong(PacketCreator.GetKoreanTimestamp(rs.getLong("timestamp")));
        mplew.writeInt(rs.getInt("icon"));
        mplew.writeInt(rs.getInt("replycount"));
    }

    public static OutPacket BBSThreadList(ResultSet rs, int start) throws SQLException {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.BBS_OPERATION.getValue());
        wp.write(0x06);
        if (!rs.last()) {
            wp.write(0);
            wp.writeInt(0);
            wp.writeInt(0);
            return wp.getPacket();
        }
        int threadCount = rs.getRow();
        if (rs.getInt("localthreadid") == 0) {
            wp.write(1);
            AddThread(wp, rs);
            threadCount--;
        } else {
            wp.write(0);
        }
        if (!rs.absolute(start + 1)) {
            rs.first();
            start = 0;
        }
        wp.writeInt(threadCount);
        wp.writeInt(Math.min(10, threadCount - start));
        for (int i = 0; i < Math.min(10, threadCount - start); i++) {
            AddThread(wp, rs);
            rs.next();
        }
        return wp.getPacket();
    }

    public static OutPacket ShowThread(int localThreadID, ResultSet threadRS, ResultSet repliesRS) throws SQLException, RuntimeException {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.BBS_OPERATION.getValue());
        wp.write(0x07);
        wp.writeInt(localThreadID);
        wp.writeInt(threadRS.getInt("postercid"));
        wp.writeLong(PacketCreator.GetKoreanTimestamp(threadRS.getLong("timestamp")));
        wp.writeMapleAsciiString(threadRS.getString("name"));
        wp.writeMapleAsciiString(threadRS.getString("startpost"));
        wp.writeInt(threadRS.getInt("icon"));
        if (repliesRS != null) {
            int replyCount = threadRS.getInt("replycount");
            wp.writeInt(replyCount);
            int i;
            for (i = 0; i < replyCount && repliesRS.next(); i++) {
                wp.writeInt(repliesRS.getInt("replyid"));
                wp.writeInt(repliesRS.getInt("postercid"));
                wp.writeLong(PacketCreator.GetKoreanTimestamp(repliesRS.getLong("timestamp")));
                wp.writeMapleAsciiString(repliesRS.getString("content"));
            }
            if (i != replyCount || repliesRS.next()) {
                throw new RuntimeException(String.valueOf(threadRS.getInt("threadid")));
            }
        } else {
            wp.writeInt(0); 
        }
        return wp.getPacket();
    }
    
    public static OutPacket ShowGuildRanks(int npcID, ResultSet rs) throws SQLException {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(GuildHeaders.SHOW_GUILD_RANK_BOARD);
        wp.writeInt(npcID);
        if (!rs.last())	{  
            wp.writeInt(0);
            return wp.getPacket();
        }
        wp.writeInt(rs.getRow());  
        rs.beforeFirst();
        while (rs.next()) {
            wp.writeMapleAsciiString(rs.getString("name"));
            wp.writeInt(rs.getInt("GP"));
            wp.writeInt(rs.getInt("logo"));
            wp.writeInt(rs.getInt("logoColor"));
            wp.writeInt(rs.getInt("logoBG"));
            wp.writeInt(rs.getInt("logoBGColor"));
        }
        return wp.getPacket();
    }
	
    public static OutPacket UpdateGP(int gID, int GP) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(GuildHeaders.GUILD_GP_CHANGED);
        wp.writeInt(gID);
        wp.writeInt(GP);
        return wp.getPacket();
    }
    
    public static OutPacket LoadGuildName(Player p) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_GUILD_MEMBERSHIP.getValue());
        wp.writeInt(p.getId());
        if (p.getGuildId() <= 0) {
            wp.writeShort(0);
        } else {
            final MapleGuild gs = GuildService.getGuild(p.getGuildId());
            wp.writeMapleAsciiString(gs != null ? gs.getName() : "");
        }
        return wp.getPacket();
    }
   
    public static OutPacket LoadGuildIcon(Player p) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_GUILD_EMBLEM.getValue());
        wp.writeInt(p.getId());
        if (p.getGuildId() <= 0) {
            wp.writeZeroBytes(6);
        } else {
            final MapleGuild gs = GuildService.getGuild(p.getGuildId());
            if (gs != null) {
                wp.writeShort(gs.getEmblemBackground());
                wp.write(gs.getEmblemBackgroundColor());
                wp.writeShort(gs.getEmblemDesign());
                wp.write(gs.getEmblemDesignColor());
            } else {
                wp.writeZeroBytes(6);
            }
        }
        return wp.getPacket();
    }
    
    public static OutPacket ContractGuildMember(int nPartyID, String sGuildName, String sMasterName) {
        WritingPacket wp = new WritingPacket();    
        wp.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        wp.write(GuildHeaders.GUILD_CONTRACT);
        wp.writeInt(nPartyID);
        wp.writeMapleAsciiString(sMasterName);
        wp.writeMapleAsciiString(sGuildName);
        return wp.getPacket();
    }
    
    public static OutPacket GetAllianceInfo(MapleGuildAlliance alliance) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        wp.write(0x0C);
        wp.write(alliance == null ? 0 : 1); 
        if (alliance != null) {
            AddAllianceInfo(wp, alliance);
        }
        return wp.getPacket();
    }
    
     private static void AddAllianceInfo(WritingPacket mplew, MapleGuildAlliance alliance) {
        
        mplew.writeInt(alliance.getId());
        mplew.writeMapleAsciiString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(alliance.getRank(i));
        }
        mplew.write(alliance.getNoGuilds());
        for (int i = 0; i < alliance.getNoGuilds(); i++) {
            mplew.writeInt(alliance.getGuildId(i));
        }
        mplew.writeInt(alliance.getCapacity());  
        mplew.writeMapleAsciiString(alliance.getNotice());
    }

    public static OutPacket UpdateAlliance(MapleGuildCharacter mgc, int allianceid) {
        WritingPacket mplew = new WritingPacket();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x18);
        mplew.writeInt(allianceid);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getJobId());

        return mplew.getPacket();
    }

    public static OutPacket GetGuildAlliance(MapleGuildAlliance alliance) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        wp.write(0x0D);
        if (alliance == null) {
            wp.writeInt(0);
            return wp.getPacket();
        }
        final int noGuilds = alliance.getNoGuilds();
        MapleGuild[] g = new MapleGuild[noGuilds];
        for (int i = 0; i < alliance.getNoGuilds(); i++) {
            g[i] = GuildService.getGuild(alliance.getGuildId(i));
            if (g[i] == null) {
                return PacketCreator.EnableActions();
            }
        }
        wp.writeInt(noGuilds);
        for (MapleGuild gg : g) {
            GetGuildInfo(wp, gg);
        }
        return wp.getPacket();
    }

    public static OutPacket AddGuildToAlliance(MapleGuildAlliance alliance, MapleGuild newGuild) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        wp.write(0x12);
        AddAllianceInfo(wp, alliance);
        wp.writeInt(newGuild.getId());
        GetGuildInfo(wp, newGuild);
        wp.write(0);  
        return wp.getPacket();
    }
    
     private static void GetGuildInfo(WritingPacket wp, MapleGuild guild) {
        
        wp.writeInt(guild.getId());
        wp.writeMapleAsciiString(guild.getName());
        for (int i = 1; i <= 5; i++) {
            wp.writeMapleAsciiString(guild.getRankTitle(i));
        }
        guild.addMemberData(wp);
        wp.writeInt(guild.getCapacity());
        wp.writeShort(guild.getEmblemBackground());
        wp.write(guild.getEmblemBackgroundColor());
        wp.writeShort(guild.getEmblemDesign());
        wp.write(guild.getEmblemDesignColor());
        wp.writeMapleAsciiString(guild.getNotice());
        wp.writeInt(guild.getGP());
        wp.writeInt(guild.getAllianceId() > 0 ? guild.getAllianceId() : 0);
    }
    
    
    public static OutPacket AllianceMemberOnline(int alliance, int gid, int id, boolean online) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        wp.write(0x0E);
        wp.writeInt(alliance);
        wp.writeInt(gid);
        wp.writeInt(id);
        wp.writeBool(online);
        return wp.getPacket();
    }

    public static OutPacket AllianceNotice(int id, String notice) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        wp.write(0x1C);
        wp.writeInt(id);
        wp.writeMapleAsciiString(notice);
        return wp.getPacket();
    }

    public static OutPacket ChangeAllianceRankTitle(int alliance, String[] ranks) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        wp.write(0x1A);
        wp.writeInt(alliance);
        for (int i = 0; i < 5; i++) {
            wp.writeMapleAsciiString(ranks[i]);
        }
        return wp.getPacket();
    }

    public static OutPacket UpdateAllianceJobLevel(Player p) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        wp.write(0x18);
        wp.writeInt(p.getGuild().getAllianceId());
        wp.writeInt(p.getGuildId());
        wp.writeInt(p.getId());
        wp.writeInt(p.getLevel());
        wp.writeInt(p.getJob().getId());
        return wp.getPacket();
    }
    
    public static OutPacket RemoveGuildFromAlliance(MapleGuildAlliance alliance, MapleGuild expelledGuild, boolean expelled) {   
        WritingPacket mplew = new WritingPacket();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x10);
        AddAllianceInfo(mplew, alliance);
        GetGuildInfo(mplew, expelledGuild);
        mplew.write(expelled ? 1 : 0); 
        return mplew.getPacket();
    }

    public static OutPacket DisbandAlliance(int alliance) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        wp.write(0x1D);
        wp.writeInt(alliance);
        return wp.getPacket();
    }
    
    public static OutPacket CreateGuildAlliance(MapleGuildAlliance alliance) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        wp.write(0x0F);
        AddAllianceInfo(wp, alliance);
        final int noGuilds = alliance.getNoGuilds();
        MapleGuild[] g = new MapleGuild[noGuilds];
        for (int i = 0; i < alliance.getNoGuilds(); i++) {
            g[i] = GuildService.getGuild(alliance.getGuildId(i));
            if (g[i] == null) {
                return PacketCreator.EnableActions();
            }
        }
        for (MapleGuild gg : g) {
            GetGuildInfo(wp, gg);
        }
        return wp.getPacket();
    }

    public static OutPacket ChangeAlliance(MapleGuildAlliance alliance, final boolean in) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        wp.write(0x01);
        wp.write(in ? 1 : 0);
        wp.writeInt(in ? alliance.getId() : 0);
        final int noGuilds = alliance.getNoGuilds();
        MapleGuild[] g = new MapleGuild[noGuilds];
        for (int i = 0; i < noGuilds; i++) {
            g[i] = GuildService.getGuild(alliance.getGuildId(i));
            if (g[i] == null) {
                return PacketCreator.EnableActions();
            }
        }
        wp.write(noGuilds);
        for (int i = 0; i < noGuilds; i++) {
            wp.writeInt(g[i].getId());
            Collection<MapleGuildCharacter> members = g[i].getMembers();
            wp.writeInt(members.size());
            for (MapleGuildCharacter mgc : members) {
                wp.writeInt(mgc.getId());
                wp.write(in ? mgc.getAllianceRank() : 0);
            }
        }
        return wp.getPacket();
    }
    
    public static OutPacket GetAllianceUpdate(MapleGuildAlliance alliance) {
        WritingPacket mplew = new WritingPacket();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x17);
        AddAllianceInfo(mplew, alliance);
        return mplew.getPacket();
    }

    public static OutPacket ChangeGuildInAlliance(MapleGuildAlliance alliance, MapleGuild guild, final boolean add) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        wp.write(0x04);
        wp.writeInt(add ? alliance.getId() : 0);
        wp.writeInt(guild.getId());
        Collection<MapleGuildCharacter> members = guild.getMembers();
        wp.writeInt(members.size());
        for (MapleGuildCharacter mgc : members) {
            wp.writeInt(mgc.getId());
            wp.write(add ? mgc.getAllianceRank() : 0);
        }
        return wp.getPacket();
    }
    
    public static OutPacket ChangeAllianceLeader(int allianceid, int newLeader, int oldLeader) {
        WritingPacket mplew = new WritingPacket();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x02);
        mplew.writeInt(allianceid);
        mplew.writeInt(oldLeader);
        mplew.writeInt(newLeader);
        return mplew.getPacket();
    }
    
    public static OutPacket UpdateAllianceLeader(int allianceid, int newLeader, int oldLeader) {
        WritingPacket mplew = new WritingPacket();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x19);
        mplew.writeInt(allianceid);
        mplew.writeInt(oldLeader);
        mplew.writeInt(newLeader);
        return mplew.getPacket();
    }

     public static OutPacket SendAllianceInvite(String allianceName, Player inviter) {
        WritingPacket mplew = new WritingPacket();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x03);
        mplew.writeInt(inviter.getGuildId());
        mplew.writeMapleAsciiString(inviter.getName());
        mplew.writeMapleAsciiString(allianceName);
        return mplew.getPacket();
    }
}
