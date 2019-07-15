/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client.player;

import client.Client;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author GabrielSin
 */
public class PlayerQuery {

    public static String getNameById(int id) {
        try {
            String name;
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name FROM characters WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return null;
                    }   name = rs.getString("name");
                }
            }
            return name;
        } catch (SQLException sqle) {}
      return null;
    }
    
    public static int getIdByName(String name) {
        try {
            int id;
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT id FROM characters WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return -1;
                    }  
                    id = rs.getInt("id");
                }
            }
            return id;
        } catch (SQLException sqle) {}
        return -1;
    }
    
    public static int getGenderByName(String name) {
        try {
            int gender;
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT gender FROM characters WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return -1;
                    }  
                    gender = rs.getInt("gender");
                }
            }
            return gender;
        } catch (SQLException sqle) {}
        return -1;
    }
    
    public static int getIdByName(String name, int world) {
        try {
            int id;
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT id FROM characters WHERE name = ? AND world = ?")) {
                ps.setString(1, name);
                ps.setInt(2, world);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return -1;
                    }
                    id = rs.getInt("id");
                }
            }
            return id;
        } catch (SQLException sqle) {}
      return -1;
    }
    
    public static int getAccIdFromCNAME(String name) {
        try {
            int id_;
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT accountid FROM characters WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();                        
                        return -1;
                    }
                    id_ = rs.getInt("accountid");
                }
            }
            return id_;
        } catch (SQLException sqle) {}
      return -1;
    }
    
    public static int getGenderById(int id) {
        try {
            int gender;
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT gender FROM characters WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return 2;
                    }  
                    gender = rs.getInt("gender");
                }
            }
            return gender;
        } catch (SQLException sqle) {}
        return 2;
    }
    
    public static Map<String, String> getCharacterFromDatabase(String name) {
        Map<String, String> character = new LinkedHashMap<>();
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `id`, `accountid`, `name` FROM `characters` WHERE `name` = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return null;
                    }
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        character.put(rs.getMetaData().getColumnLabel(i), rs.getString(i));
                    }
                }
            }
        } catch (SQLException sqle) {}
      return character;
    }

    public static void autoBan(Client c, String reason, int greason) {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        Timestamp TS = new Timestamp(cal.getTimeInMillis());
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banreason = ?, tempban = ?, greason = ? WHERE id = ?")) {
                ps.setString(1, reason);
                ps.setTimestamp(2, TS);
                ps.setInt(3, greason);
                ps.setInt(4, c.getAccountID());
                ps.executeUpdate();
                ps.close();
            }
        } catch (SQLException e) {
        }
    }
    
    public static List<Integer> getVIPRockMaps(Player p, int type) {
        List<Integer> rockmaps = new LinkedList<>();
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT mapid FROM VIPRockMaps WHERE cid = ? AND type = ?")) {
                ps.setInt(1, p.getId());
                ps.setInt(2, type);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rockmaps.add(rs.getInt("mapid"));
                    }
                }
            }
        } catch (SQLException e) {
            return null;
        }
        return rockmaps;
    } 
    
    public static void sendCallGM(Client c, String msg) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO callgm (`accountid`, `charid`, `nome`, `bug`, `date`) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP())")) {
                ps.setInt(1, c.getAccountID());
                ps.setInt(2, c.getPlayer().getId());
                ps.setString(3, c.getPlayer().getName());
                ps.setString(4, msg);
                ps.executeUpdate();
            }
        } catch(SQLException e) {
            System.out.println(e);
        }
    }
    
    public static void setPlayerVariable(String name, String value, Player p) {
        try {
            ResultSet rs;
            PreparedStatement ps2;
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM player_variables WHERE name = ? AND characterid = ?")) {
                ps.setString(1, name);
                ps.setInt(2, p.getId());
                rs = ps.executeQuery();
                if (rs.next()) {
                    ps2 = DatabaseConnection.getConnection().prepareStatement("UPDATE player_variables SET value = ? WHERE characterid = ? AND name = ?");
                    ps2.setString(1, value);
                    ps2.setInt(2, p.getId());
                    ps2.setString(3, name);
                } else {
                    ps2 = DatabaseConnection.getConnection().prepareStatement("INSERT INTO player_variables (characterid, name, value) VALUES (?, ?, ?)");
                    ps2.setInt(1, p.getId());
                    ps2.setString(2, name);
                    ps2.setString(3, value);
                }
            }
            rs.close();
            ps2.execute();
            ps2.close();
        } catch (SQLException ex) {
            System.out.println("Error setting player variable: " + ex);
        }
    }
     
    public static String getPlayerVariable(String name, Player p) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM player_variables WHERE name = ? AND characterid = ?");
            ps.setString(1, name);
            ps.setInt(2, p.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String value = rs.getString("value");
                ps.close();
                rs.close();
                return value;
            } else {
                ps.close();
                rs.close();
                return null;
            }
        } catch (SQLException ex) {
            System.out.println("Error getting player variable: " + ex);
            return null;
        }
    }
    
    public static void deletePlayerVariable(String name, Player p) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM player_variables WHERE name = ? AND characterid = ?");
            ps.setString(1, name);
            ps.setInt(2, p.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ps.close();
                rs.close();
                ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM player_variables WHERE name = ? AND characterid = ?");
                ps.setString(1, name);
                ps.setInt(2, p.getId());
                ps.execute();
            }
            ps.close();
            rs.close();
        } catch (SQLException ex) {
            System.out.println("Error deleting player variable: " + ex);
        }
    }
}
