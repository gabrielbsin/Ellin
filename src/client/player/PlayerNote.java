/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client.player;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import packet.creators.PacketCreator;

/**
 * 
 * @author GabrielSin
 */
public class PlayerNote {
    
    public static void sendNote(Player p, String to, String msg, int fame) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`, `fame`) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, to);
                ps.setString(2, p.getName());
                ps.setString(3, msg);
                ps.setLong(4, System.currentTimeMillis());
                ps.setInt(5, fame);
                ps.executeUpdate();
                ps.close();
            }
        } catch (SQLException e) {
            System.err.println("Unable to send note" + e);
        }
    }
    
    public static void sendNote(String sender, String to, String msg, int fame) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`, `fame`) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, to);
                ps.setString(2, sender);
                ps.setString(3, msg);
                ps.setLong(4, System.currentTimeMillis());
                ps.setInt(5, fame);
                ps.executeUpdate();
                ps.close();
            }
        } catch (SQLException e) {
            System.err.println("Unable to send note" + e);
        }
    }
    
    public static void sendNote(Player p, String to, String msg) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, to);
                ps.setString(2, p.getName());
                ps.setString(3, msg);
                ps.setLong(4, System.currentTimeMillis());
                ps.setInt(5, 0);
                ps.executeUpdate();
                ps.close();
            }
        } catch (SQLException e) {
            System.err.println("Unable to send note" + e);
        }
    }

    public static void showNote(Player p) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM notes WHERE `to`=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
                ps.setString(1, p.getName());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.last();
                    rs.first();
                    p.announce(PacketCreator.ShowNotes(rs, rs.getRow()));
                }
            }
        } catch (SQLException e) {
            System.err.println("Unable to show note" + e);
        }
    }
    
    public static void deleteNote(Player p, int id, int fame) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT fame FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("fame") == fame && fame > 0 && p != null) { 
                    p.addFame(fame);
                    p.getStat().updateSingleStat(PlayerStat.FAME, p.getFame());
                    p.announce(PacketCreator.GetShowFameGain(fame));
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Unable to delete note" + e);
        }
    }
    
    public static void deleteNote(int id) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM notes WHERE `id`=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
