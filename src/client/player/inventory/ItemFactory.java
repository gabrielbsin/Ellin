package client.player.inventory;

import client.player.inventory.types.InventoryType;
import constants.ItemConstants;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import server.itens.ItemInformationProvider;
import tools.Pair;

public enum ItemFactory {
    
    INVENTORY(1, false),
    STORAGE(2, true),
    CASHSHOP(3, true),
    MERCHANT(4, false);

    private final int value;
    private final boolean account;

    private ItemFactory(int value, boolean account) {
        this.value = value;
        this.account = account;
    }

    public int getValue() {
        return value;
    }

    public List<Pair<Item, InventoryType>> loadItems(int id, boolean login) throws SQLException {
        List<Pair<Item, InventoryType>> items = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM `");
        query.append("inventoryitems");
        query.append("` LEFT JOIN `");
        query.append("inventoryequipment");
        query.append("` USING(`");
        query.append("inventoryitemid");
        query.append("`) WHERE `type` = ? AND `");
        query.append(account ? "accountid" : "characterid");
        query.append("` = ?");

        if (login) {
            query.append(" AND `inventorytype` = ");
            query.append(InventoryType.EQUIPPED.getType());
        }

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(query.toString())) {
        ps.setInt(1, value);
        ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final ItemInformationProvider ii = ItemInformationProvider.getInstance();
                    if (!ii.itemExists(rs.getInt("itemid"))) {
                        continue;
                    }
                    InventoryType mit = InventoryType.getByType(rs.getByte("inventorytype"));

                    if (mit.equals(InventoryType.EQUIP) || mit.equals(InventoryType.EQUIPPED)) {
                        Equip equip = new Equip(rs.getInt("itemid"), (short) rs.getInt("position"), rs.getInt("uniqueid"));
                        equip.setOwner(rs.getString("owner"));
                        equip.setQuantity((short) rs.getInt("quantity"));
                        equip.setAcc((short) rs.getInt("acc"));
                        equip.setAvoid((short) rs.getInt("avoid"));
                        equip.setDex((short) rs.getInt("dex"));
                        equip.setHands((short) rs.getInt("hands"));
                        equip.setHp((short) rs.getInt("hp"));
                        equip.setInt((short) rs.getInt("int"));
                        equip.setJump((short) rs.getInt("jump"));
                        equip.setLuk((short) rs.getInt("luk"));
                        equip.setMatk((short) rs.getInt("matk"));
                        equip.setMdef((short) rs.getInt("mdef"));
                        equip.setMp((short) rs.getInt("mp"));
                        equip.setSpeed((short) rs.getInt("speed"));
                        equip.setStr((short) rs.getInt("str"));
                        equip.setWatk((short) rs.getInt("watk"));
                        equip.setWdef((short) rs.getInt("wdef"));
                        equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                        equip.setLocked((byte) rs.getInt("locked"));
                        equip.setLevel((byte) rs.getInt("level"));
                        equip.setGiftFrom(rs.getString("giftFrom"));
                        equip.setExpiration(rs.getLong("expiration"));
                        if (equip.getUniqueId() > -1) {
                            if (ItemConstants.isEffectRing(rs.getInt("itemid"))) {
                                ItemRing ring = ItemRing.loadingRing(equip.getUniqueId());
                                if (ring != null) {
                                    equip.setRing(ring);
                                }
                            }
                        }
                        items.add(new Pair<>(equip.copy(), mit));
                    } else {
                        Item item = new Item(rs.getInt("itemid"), (short) rs.getInt("position"), (short) rs.getInt("quantity"), rs.getInt("uniqueid"));
                        item.setOwner(rs.getString("owner"));
                        item.setGiftFrom(rs.getString("giftFrom"));
                        item.setExpiration(rs.getLong("expiration"));
                        if (ItemConstants.isPet(item.getItemId())) {
                            if (item.getUniqueId() > -1) {
                                ItemPet pet = ItemPet.loadDatabase(item.getItemId(), item.getUniqueId(), item.getPosition());
                                if (pet != null) {
                                    item.setPet(pet);
                                }
                            } else {
                                final int new_unique = InventoryIdentifier.getInstance();
                                item.setUniqueId(new_unique);
                                item.setPet(ItemPet.createPet(item.getItemId(), new_unique));
                            }
                        }
                        items.add(new Pair<>(item.copy(), mit));
                    }
                }   
            }
        } catch(Exception ex) {
            System.err.println(ex);
        }
        return items;
    }

    public synchronized void saveItems(List<Pair<Item, InventoryType>> items, int id) throws SQLException {
        StringBuilder queryOne = new StringBuilder();
        queryOne.append("DELETE FROM `inventoryitems` WHERE `type` = ? AND `");
        queryOne.append(account ? "accountid" : "characterid");
        queryOne.append("` = ?");
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(queryOne.toString());
            ps.setInt(1, value);
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();

            StringBuilder queryTwo = new StringBuilder("INSERT INTO `");
            queryTwo.append("inventoryitems");
            queryTwo.append("` (");
            queryTwo.append("inventoryitemid, type, characterid, accountid, itemid, inventorytype, position, quantity, owner, uniqueid, giftfrom, expiration) VALUES (DEFAULT, ?, ?, ?, ?,?, ?, ?, ?, ?, ?, ?)"); 
            ps = con.prepareStatement(queryTwo.toString(), Statement.RETURN_GENERATED_KEYS);
            PreparedStatement pse = con.prepareStatement("INSERT INTO `inventoryequipment` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            for (Pair<Item, InventoryType> pair : items) {
                Item item = pair.getLeft();
                if (item.disappearsAtLogout())
                    continue;
                InventoryType mit = pair.getRight();
                ps.setInt(1, value);
                ps.setString(2, account ? null : String.valueOf(id));
                ps.setString(3, account ? String.valueOf(id) : null);
                ps.setInt(4, item.getItemId());
                ps.setInt(5, mit.getType());
                ps.setInt(6, item.getPosition());
                ps.setInt(7, item.getQuantity());
                ps.setString(8, item.getOwner());
                ps.setInt(9, item.getUniqueId());
                ps.setString(10, item.getGiftFrom());
                ps.setLong(11, item.getExpiration());
                ps.executeUpdate();

                if (mit.equals(InventoryType.EQUIP) || mit.equals(InventoryType.EQUIPPED)) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next())
                            throw new RuntimeException("Inserting item failed.");
                            pse.setInt(1, rs.getInt(1));
                        }
                        Equip equip = (Equip) item;
                        pse.setInt(2, equip.getUpgradeSlots());
                        pse.setInt(3, equip.getLevel());
                        pse.setInt(4, equip.getStr());
                        pse.setInt(5, equip.getDex());
                        pse.setInt(6, equip.getInt());
                        pse.setInt(7, equip.getLuk());
                        pse.setInt(8, equip.getHp());
                        pse.setInt(9, equip.getMp());
                        pse.setInt(10, equip.getWatk());
                        pse.setInt(11, equip.getMatk());
                        pse.setInt(12, equip.getWdef());
                        pse.setInt(13, equip.getMdef());
                        pse.setInt(14, equip.getAcc());
                        pse.setInt(15, equip.getAvoid());
                        pse.setInt(16, equip.getHands());
                        pse.setInt(17, equip.getSpeed());
                        pse.setInt(18, equip.getJump());
                        pse.setInt(19, equip.getLocked());
                        pse.executeUpdate();
                }
            }
            pse.close();
            ps.close();
        } catch (SQLException | RuntimeException ex) {
            System.err.println(ex);
        }
    }
}
