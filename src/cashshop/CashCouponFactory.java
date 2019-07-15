/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cashshop;

import client.player.inventory.Item;
import database.DatabaseConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import tools.Pair;

/**
 * 
 * @author GabrielSin
 */
public class CashCouponFactory {

    public static boolean getCouponCodeValid(String code) {
        boolean validcode = false;
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `used` FROM `cashshopcouponitems` WHERE `code` = ?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        validcode = (rs.getByte("used") == 0);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting NX Code type " + e);
        }
        return validcode;
    }

    public static List<CashCouponData> getCcData(final String code) {
        List<CashCouponData> all = new ArrayList<>();
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `type`, `itemData`, `quantity` FROM `cashshopcouponitems` WHERE `code` = ?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final byte type = rs.getByte("type");
                        final int itemdata = rs.getInt("itemData");
                        final int quantity = rs.getInt("quantity");
                        all.add(new CashCouponData(type, itemdata, quantity));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting Coupon Data " + e);
        }
        return all;
    }

    public static void setCouponCodeUsed(String name, String code) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE `cashshopcouponitems` SET `character` = ?, `used` = 1 WHERE code = ?")) {
                ps.setString(1, name);
                ps.setString(2, code);
                ps.execute();
            }
        } catch (SQLException e) {
            System.out.println("Error getting Coupon Data " + e);
        }
    }

    public static void deleteCouponData(String name, String code) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("DELETE from `cashshopcouponitems` WHERE `code` = ?")) {
                ps.setString(1, code);
                ps.execute();
            }
        } catch (SQLException e) {
            System.out.println("Error deleting Coupon Data " + e);
        }
    }

    public static Pair<Pair<Integer, Integer>, Pair<List<Item>, Integer>> getSize(List<CashCouponData> ccd) {
        int MaplePoints = 0, mesos = 0, Cashsize = 0;
        final List<Item> togiveII = new ArrayList<>();
        for (CashCouponData hmm : ccd) {
            switch (hmm.getType()) {
                case 0:  
                    if (hmm.getData() > 0) {
                        MaplePoints += hmm.getData();
                    }
                    break;
                case 1: 
                    Cashsize++;
                    break;
                case 2: 
                    if (hmm.getQuantity() <= Short.MAX_VALUE && hmm.getQuantity() > 0) {
                        togiveII.add(new Item(hmm.getData(), (byte) 0, (short) hmm.getQuantity()));
                    }
                    break;
                case 3: 
                    if (hmm.getData() > 0) {
                        mesos += hmm.getData();
                    }
                    break;
            }
        }
        return new Pair<>(new Pair<>(MaplePoints, Cashsize), new Pair<>(togiveII, mesos));
    }
}
