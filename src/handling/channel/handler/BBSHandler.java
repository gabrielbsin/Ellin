/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.player.Player;
import client.Client;
import database.DatabaseConnection;
import static handling.channel.handler.ChannelHeaders.BBSHeaders.*;
import handling.mina.PacketReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import packet.creators.GuildPackets;
import tools.FileLogger;

/**
 *
 * @author GabrielSin
 */
public class BBSHandler {
    
    private static String correctLength(String in, int maxSize) {
        if (in.length() > maxSize) {
            return in.substring(0, maxSize);
        }
        return in;
    }

    public static void BBSOperation(PacketReader packet, Client c) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        } 
        byte mode = packet.readByte();
        int localthreadid = 0;
        switch (mode) {
            case START_TOPIC:
                boolean bEdit = packet.readByte() == 1;
                if (bEdit) {
                    localthreadid = packet.readInt();
                }
                boolean bNotice = packet.readByte() == 1;
                String title = correctLength(packet.readMapleAsciiString(), 25);
                String text = correctLength(packet.readMapleAsciiString(), 600);
                int icon = packet.readInt();
                if (icon >= 0x64 && icon <= 0x6a) {
                    if (c.getPlayer().getItemQuantity(5290000 + icon - 0x64, false) > 0) {
                        return;
                    }
                } else if (icon < 0 || icon > 3) {
                    return;
                }
                if (!bEdit) {
                    newBBSThread(c, title, text, icon, bNotice);
                } else {
                    editBBSThread(c, title, text, icon, localthreadid);
                }
                break;
            case DELETE_TOPIC:
                localthreadid = packet.readInt();
                deleteBBSThread(c, localthreadid);
                break;
            case LIST_TOPIC:
                int start = packet.readInt();
                listBBSThreads(c, start * 10);
                break;
            case LIST_TOPIC_REPLY: 
                localthreadid = packet.readInt();
                displayThread(c, localthreadid);
                break;
            case TOPIC_REPLY: 
                localthreadid = packet.readInt();
                text = correctLength(packet.readMapleAsciiString(), 25);
                newBBSReply(c, localthreadid, text);
                break;
            case DELETE_REPLY:
                /* localthreadid  = */ packet.readInt();  
                int replyid = packet.readInt();
                deleteBBSReply(c, replyid);
                break;
            default:
                System.out.println("Unknown mode BBS: " + packet.toString());
        }
    }
    
    private static void listBBSThreads(Client c, int start) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM bbs_threads WHERE guildid = ? ORDER BY localthreadid DESC")) {
                ps.setInt(1, c.getPlayer().getGuildId());
                try (ResultSet rs = ps.executeQuery()) {
                    c.announce(GuildPackets.BBSThreadList(rs, start));
                }
            }
        } catch (SQLException se) {
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, se);
        }
    }

    private static void newBBSReply(Client c, int localthreadid, String text) {
        if (c.getPlayer().getGuildId() < 1) {
            return;
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT threadid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?");
            ps.setInt(1, c.getPlayer().getGuildId());
            ps.setInt(2, localthreadid);
            ResultSet threadRS = ps.executeQuery();
            if (!threadRS.next()) {
                threadRS.close();
                ps.close();
                return;
            }
            int threadid = threadRS.getInt("threadid");
            threadRS.close();
            ps.close();
            ps = con.prepareStatement("INSERT INTO bbs_replies " + "(`threadid`, `postercid`, `timestamp`, `content`) VALUES " + "(?, ?, ?, ?)");
            ps.setInt(1, threadid);
            ps.setInt(2, c.getPlayer().getId());
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, text);
            ps.execute();
            ps.close();
            ps = con.prepareStatement("UPDATE bbs_threads SET replycount = replycount + 1 WHERE threadid = ?");
            ps.setInt(1, threadid);
            ps.execute();
            ps.close();
            displayThread(c, localthreadid);
        } catch (SQLException se) {
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, se);
        }
    }

    private static void editBBSThread(Client client, String title, String text, int icon, int localthreadid) {
        Player c = client.getPlayer();
        if (c.getGuildId() < 1) {
            return;
        }
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE bbs_threads SET `name` = ?, `timestamp` = ?, " + "`icon` = ?, " + "`startpost` = ? WHERE guildid = ? AND localthreadid = ? AND (postercid = ? OR ?)")) {
                ps.setString(1, title);
                ps.setLong(2, System.currentTimeMillis());
                ps.setInt(3, icon);
                ps.setString(4, text);
                ps.setInt(5, c.getGuildId());
                ps.setInt(6, localthreadid);
                ps.setInt(7, c.getId());
                ps.setBoolean(8, c.getGuildRank() < 3);
                ps.execute();
            }
            displayThread(client, localthreadid);
        } catch (SQLException se) {
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, se);
        }
    }

    private static void newBBSThread(Client client, String title, String text, int icon, boolean bNotice) {
        Player c = client.getPlayer();
        if (c.getGuildId() <= 0) {
            return;
        }
        int nextId = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (!bNotice) {
                ps = con.prepareStatement("SELECT MAX(localthreadid) AS lastLocalId FROM bbs_threads WHERE guildid = ?");
                ps.setInt(1, c.getGuildId());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    nextId = rs.getInt("lastLocalId") + 1;
                }
                ps.close();
            }
            ps = con.prepareStatement("INSERT INTO bbs_threads " + "(`postercid`, `name`, `timestamp`, `icon`, `startpost`, " + "`guildid`, `localthreadid`) " + "VALUES(?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, c.getId());
            ps.setString(2, title);
            ps.setLong(3, System.currentTimeMillis());
            ps.setInt(4, icon);
            ps.setString(5, text);
            ps.setInt(6, c.getGuildId());
            ps.setInt(7, nextId);
            ps.execute();
            ps.close();
            displayThread(client, nextId);
        } catch (SQLException se) {
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, se);
        }
    }

    public static void deleteBBSThread(Client client, int localthreadid) {
        Player mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT threadid, postercid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?");
            ps.setInt(1, mc.getGuildId());
            ps.setInt(2, localthreadid);
            ResultSet threadRS = ps.executeQuery();
            if (!threadRS.next()) {
                threadRS.close();
                ps.close();
                return;
            }
            if (mc.getId() != threadRS.getInt("postercid") && mc.getGuildRank() > 2) {
                threadRS.close();
                ps.close();
                return;
            }
            int threadid = threadRS.getInt("threadid");
            ps.close();
            ps = con.prepareStatement("DELETE FROM bbs_replies WHERE threadid = ?");
            ps.setInt(1, threadid);
            ps.execute();
            ps.close();
            ps = con.prepareStatement("DELETE FROM bbs_threads WHERE threadid = ?");
            ps.setInt(1, threadid);
            ps.execute();
            threadRS.close();
            ps.close();
        } catch (SQLException se) {
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, se);
        }
    }

    public static void deleteBBSReply(Client client, int replyid) {
        Player mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }
        int threadid;
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT postercid, threadid FROM bbs_replies WHERE replyid = ?");
            ps.setInt(1, replyid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return;
            }
            if (mc.getId() != rs.getInt("postercid") && mc.getGuildRank() > 2) {
                rs.close();
                ps.close();
                return;
            }
            threadid = rs.getInt("threadid");
            rs.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM bbs_replies WHERE replyid = ?");
            ps.setInt(1, replyid);
            ps.execute();
            ps.close();
            ps = con.prepareStatement("UPDATE bbs_threads SET replycount = replycount - 1 WHERE threadid = ?");
            ps.setInt(1, threadid);
            ps.execute();
            ps.close();
            displayThread(client, threadid, false);
        } catch (SQLException se) {
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, se);
        }
    }

    public static void displayThread(Client client, int threadid) {
        displayThread(client, threadid, true);
    }

     public static void displayThread(Client client, int threadid, boolean bIsThreadIdLocal) {
        Player mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps2;
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM bbs_threads WHERE guildid = ? AND " + (bIsThreadIdLocal ? "local" : "") + "threadid = ?")) {
                ps.setInt(1, mc.getGuildId());
                ps.setInt(2, threadid);
                ResultSet threadRS = ps.executeQuery();
                if (!threadRS.next()) {
                    threadRS.close();
                    ps.close();
                    return;
                }   
                ResultSet repliesRS = null;
                ps2 = null;
                if (threadRS.getInt("replycount") >= 0) {
                    ps2 = con.prepareStatement("SELECT * FROM bbs_replies WHERE threadid = ?");
                    ps2.setInt(1, !bIsThreadIdLocal ? threadid : threadRS.getInt("threadid"));
                    repliesRS = ps2.executeQuery();
                }  
                client.announce(GuildPackets.ShowThread(bIsThreadIdLocal ? threadid : threadRS.getInt("localthreadid"), threadRS, repliesRS));
                if (repliesRS != null) {
                    repliesRS.close();
                }
            }
            if (ps2 != null) {
                ps2.close();
            }
        } catch (SQLException se) {
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, se);
        } catch (RuntimeException re) {
            System.out.println("The number of reply rows does not match the replycount in thread.");
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, re);
        }
    }
}
