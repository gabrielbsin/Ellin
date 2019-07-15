package client.player.inventory;

import cashshop.CashItem;
import cashshop.CashItemFactory;
import client.player.Player;
import client.player.PlayerQuery;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import database.DatabaseConnection;
import java.sql.SQLException;
import java.sql.Statement;
import packet.creators.CashShopPackets;
import tools.FileLogger;

/**
 *
 * @author GabrielSin
 */
public class ItemRing implements Comparable<ItemRing> {

    private final int ringId;
    private final int ringId2;
    private final int partnerId;
    private final int itemId;
    private final String partnerName;

    private ItemRing(int id, int id2, int partnerId, int itemid, String partnername) {
        this.ringId = id;
        this.ringId2 = id2;
        this.partnerId = partnerId;
        this.itemId = itemid;
        this.partnerName = partnername;
    }
    
    public int getRingId() {
        return ringId;
    }

    public int getPartnerRingId() {
        return ringId2;
    }

    public int getPartnerChrId() {
        return partnerId;
    }

    public int getItemId() {
        return itemId;
    }

    public String getPartnerName() {
        return partnerName;
    }

   public static ItemRing loadFromDb(int ringId) {
        try {
            ItemRing ret;
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM rings WHERE id = ?")) {
                ps.setInt(1, ringId);
                try (ResultSet rs = ps.executeQuery()) {
                    ret = null;
                    if (rs.next()) {
                        ret = new ItemRing(ringId, rs.getInt("partnerRingId"), rs.getInt("partnerChrId"), rs.getInt("itemid"), rs.getString("partnerName"));
                    }
                }
            }
            return ret;
        } catch (SQLException ex) {
            System.out.println("[-] loadFromDb error");
            FileLogger.printError("MapleRing_loadFromDb.txt", ex);
            return null;
        }
    }

    public static boolean createRing(int itemid, final Player sender, final int receiverId, String message, int serialNumber) {
        if (verifyExistRing(sender.getId()) || verifyExistRing(receiverId)) {
            return false;
        } else {
            addRingDB(itemid, sender, receiverId, message, serialNumber);
            return true;
        }
    }
    
    public static void addRingDB(int itemId, final Player sender, final int receiverId, String message, int serialNumber) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int[] uniqueId = {InventoryIdentifier.getInstance(), InventoryIdentifier.getInstance()};
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO `rings` (`itemid`, `partnerChrId`, `partnername`) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, itemId);
            ps.setInt(2, receiverId);
            ps.setString(3, PlayerQuery.getNameById(receiverId));
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            rs.next();
            uniqueId[0] = rs.getInt(1);
            rs.close();
            ps.close();

            ps = con.prepareStatement("INSERT INTO `rings` (`itemid`, `partnerRingId`, `partnerChrId`, `partnername`) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, itemId);
            ps.setInt(2, uniqueId[0]);
            ps.setInt(3, sender.getId());
            ps.setString(4, sender.getName());
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            rs.next();
            uniqueId[1] = rs.getInt(1);
            rs.close();
            ps.close();

            ps = con.prepareStatement("UPDATE `rings` SET `partnerRingId` = ? WHERE id = ?");
            ps.setInt(1, uniqueId[1]);
            ps.setInt(2, uniqueId[0]);
            ps.executeUpdate();
            ps.close();
            sendRingCashInventory(sender, receiverId, message, serialNumber, uniqueId[0], uniqueId[1]);
        } catch (SQLException ex) {
            System.out.println("[-] addRingDB error");
            FileLogger.printError("addRingDB.txt", ex);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                System.out.println("[-] addRingDB: " + e);
            }
        }
    }
    
    private static void sendRingCashInventory(final Player sender, final int receiverId, String message, int serialNumber, int uniqueIdSender, int uniqueIdReceiver) {
        try {
            CashItem itemRing = CashItemFactory.getItem(serialNumber); 
            Item item = itemRing.toItem(itemRing, uniqueIdSender, 0, "");
            sender.getCashShop().addToInventory(item );
            sender.announce(CashShopPackets.ShowCashInventory(sender.getClient()));
            sender.getCashShop().gift(receiverId, sender.getName(), message, serialNumber, uniqueIdReceiver);
        } catch (Exception ex) {
            System.out.println("[-] sendRingCashInventory error");
            FileLogger.printError("sendRingCashInventory.txt", ex);
        }
    }
    
    public static boolean verifyExistRing(int id) {
        try {
            Connection con = DatabaseConnection.getConnection();
            boolean has;
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM rings WHERE partnerChrId = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    has = rs.next();
                }
            }
            return has;
        } catch (SQLException ex) {
            return true;
        }
    }
   
    @Override
    public boolean equals(Object o) {
        if (o instanceof ItemRing) {
            return ((ItemRing) o).getRingId() == getRingId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + this.ringId;
        return hash;
    }

    @Override
    public int compareTo(ItemRing other) {
        if (ringId < other.getRingId()) {
            return -1;
        } else if (ringId == other.getRingId()) {
            return 0;
        }
        return 1;
    }
}
