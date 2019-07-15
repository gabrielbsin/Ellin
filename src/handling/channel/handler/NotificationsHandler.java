/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.Client;
import database.DatabaseConnection;
import handling.mina.PacketReader;
import handling.world.service.BroadcastService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import packet.creators.PacketCreator;
import client.player.PlayerQuery;

/**
 *
 * @author GabrielSin
 */
public class NotificationsHandler {

    public final static String[] REASONS = {
        "Hacking",
        "Botting",
        "Scamming",
        "Fake GM",
        "Harassment",
        "Advertising"
    };
        
    public static void ReportPlayer(PacketReader slea, Client c) {
       	int reportedCharId = slea.readInt();
        byte reason = slea.readByte();
        String chatlog = "No chatlog";
        short clogLen = slea.readShort();
        if (clogLen > 0) {
            chatlog = slea.readAsciiString(clogLen);
        }
        
        int cid = reportedCharId;

        if (addReportEntry(c.getPlayer().getId(), reportedCharId, reason, chatlog)) {
            c.getSession().write(PacketCreator.ReportReply((byte) 0));
        } else {
            c.getSession().write(PacketCreator.ReportReply((byte) 4));
        }
        
        BroadcastService.broadcastGMMessage(PacketCreator.ServerNotice(5, c.getPlayer().getName() + " reportou " + PlayerQuery.getNameById(cid) + " por " + REASONS[reason] + "."));
    }
    
    public static boolean addReportEntry(int reporterId, int victimId, byte reason, String chatlog) {
        try {
            Connection dcon = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = dcon.prepareStatement("INSERT INTO reports VALUES (NULL, CURRENT_TIMESTAMP, ?, ?, ?, ?, 'UNHANDLED')");
            ps.setInt(1, reporterId);
            ps.setInt(2, victimId);
            ps.setInt(3, reason);
            ps.setString(4, chatlog);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
}
