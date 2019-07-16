package client.player.inventory;

import cashshop.CashItem;
import cashshop.CashItemFactory;
import client.player.Player;
import client.player.PlayerQuery;
import client.player.inventory.types.InventoryType;
import client.player.inventory.types.ItemRingType;
import constants.ItemConstants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import database.DatabaseConnection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import packet.creators.CashShopPackets;
import tools.FileLogger;

/**
 * @author Danny
 * Special thanks GabrielSin (http://forum.ragezone.com/members/822844.html)
 */

public class ItemRing implements Comparable<ItemRing> {

    private final int ringId;
    private final int partnerDatabaseId;
    private final int partnerId;
    private final int itemId;
    private final String partnerName;
    
    private List<ItemRing> crushRings = new LinkedList<>();
    private List<ItemRing> friendshipRings = new LinkedList<>();
    private List<ItemRing> weddingRings = new LinkedList<>();

    private ItemRing(int id, int id2, int partnerId, int itemid, String partnername) {
        this.ringId = id;
        this.partnerDatabaseId = id2;
        this.partnerId = partnerId;
        this.itemId = itemid;
        this.partnerName = partnername;
    }
    
    public int getRingDatabaseId() {
        return ringId;
    }

    public int getPartnerRingDatabaseId() {
        return partnerDatabaseId;
    }

    public int getPartnerCharacterId() {
        return partnerId;
    }

    public int getItemId() {
        return itemId;
    }

    public String getPartnerName() {
        return partnerName;
    }

   public static ItemRing loadingRing(int ringId) {
        try {
            ItemRing ret;
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM rings WHERE id = ?")) {
                ps.setInt(1, ringId);
                try (ResultSet rs = ps.executeQuery()) {
                    ret = null;
                    if (rs.next()) {
                        ret = new ItemRing(ringId, rs.getInt("partnerRingId"), 
                        rs.getInt("partnerChrId"),
                        rs.getInt("itemid"),
                        rs.getString("partnerName"));
                    }
                }
            }
            return ret;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static boolean createRing(int itemid, final Player sender, final int receiverId, String message, int serialNumber) {
        if (verifyExistRing(sender.getId()) || verifyExistRing(receiverId)) {
            return false;
        } else {
            addRingDatabase(itemid, sender, receiverId, message, serialNumber);
            return true;
        }
    }
    
    public static void addRingDatabase(int itemId, final Player sender, final int receiverId, String message, int serialNumber) {
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
    
    public List<ItemRing> getCrushRings() {
        Collections.sort(crushRings);
        return crushRings;
    }

    public List<ItemRing> getFriendshipRings() {
        Collections.sort(friendshipRings);
        return friendshipRings;
    }

    public List<ItemRing> getWeddingRings() {
        Collections.sort(weddingRings);
        return weddingRings;
    }
    
    public void addRingToCache(int ringId) {
        ItemRing ring = loadingRing(ringId);
        if (ring != null) {
            if (ItemConstants.isCrushRing(ring.getItemId())) {
                crushRings.add(ring);
            } else if (ItemConstants.isFriendshipRing(ring.getItemId())) {
                friendshipRings.add(ring);
            } else if (ItemConstants.isWeddingRing(ring.getItemId())) {
                weddingRings.add(ring);
            }
        }
    }
    
    public int getEquippedRing(Player p, int type) {
        for (Item item : p.getInventory(InventoryType.EQUIPPED)) {
            Equip equip = (Equip) item;
            if (equip.getRing() != null) {
                int itemId = equip.getItemId();
                if (ItemConstants.isCrushRing(itemId) && type == ItemRingType.CRUSH_RING.getType()) {
                    return equip.getRing().getRingDatabaseId();
                }
                if (ItemConstants.isFriendshipRing(itemId) && type == ItemRingType.FRIENDSHIP_RING.getType()) {
                    return equip.getRing().getRingDatabaseId();
                }
                if (ItemConstants.isWeddingRing(itemId) && type == ItemRingType.WEDDING_RING.getType()) {
                    return equip.getRing().getRingDatabaseId();
                }
            }
        }
        return 0;
    }
    
    public boolean isRingEquipped(Player p, int ringId) {
        for (Item item : p.getInventory(InventoryType.EQUIPPED)) {
            Equip equip = (Equip) item;
            if (equip.getRing().getRingDatabaseId() == ringId) {
                return equip.getPosition() <= (byte) -1;
            }
        }
        return false;
    }
   
    @Override
    public boolean equals(Object o) {
        if (o instanceof ItemRing) {
            return ((ItemRing) o).getRingDatabaseId() == getRingDatabaseId();
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
        if (ringId < other.getRingDatabaseId()) {
            return -1;
        } else if (ringId == other.getRingDatabaseId()) {
            return 0;
        }
        return 1;
    }
}
