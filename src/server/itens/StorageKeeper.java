/*
	This file is part of the OdinMS Maple Story Server
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

package server.itens;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import client.Client;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemFactory;
import constants.ItemConstants;
import database.DatabaseConnection;
import database.DatabaseException;
import java.sql.Statement;
import packet.creators.PacketCreator;
import tools.Pair;

public class StorageKeeper {
    
    private final int id;
    private int meso;   
    private final int accountId;
    private byte slots;
    private boolean changed = false;
    private final List<Item> items;
    private final Map<InventoryType, List<Item>> typeItems = new HashMap<>();

    private StorageKeeper(int id, byte slots, int meso, int accountId) {
        this.id = id;
        this.slots = slots;
        this.items = new LinkedList<>();
        this.meso = meso;
        this.accountId = accountId;
    }

    public static int create(int id) throws SQLException {
        ResultSet rs;
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO storages (accountid, slots, meso) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, id);
            ps.setInt(2, 4);
            ps.setInt(3, 0);
            ps.executeUpdate();
            int storageid;
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                storageid = rs.getInt(1);
                ps.close();
                rs.close();
                return storageid;
            }
        }
        rs.close();
        throw new DatabaseException("Inserting char failed.");
    }
    
    public static StorageKeeper loadStorage(int id) {
        StorageKeeper ret = null;
        int storeId;
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM storages WHERE accountid = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                storeId = rs.getInt("storageid");
                ret = new StorageKeeper(storeId, rs.getByte("slots"), rs.getInt("meso"), id);
                rs.close();
                ps.close();

                for (Pair<Item, InventoryType> mit : ItemFactory.STORAGE.loadItems(id, false)) {
                    ret.items.add(mit.getLeft());
                }
            } else {
                storeId = create(id);
                ret = new StorageKeeper(storeId, (byte) 4, 0, id);
                rs.close();
                ps.close();
            }
        } catch (SQLException ex) {
            System.err.println("Error loading storage");
        }
        return ret;
    }
    
     public void saveToDB() {
        if (!changed) {
            return;
        }
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE storages SET slots = ?, meso = ? WHERE storageid = ?")) {
                ps.setInt(1, slots);
                ps.setInt(2, meso);
                ps.setInt(3, id);
                ps.executeUpdate();
            }

            List<Pair<Item, InventoryType>> listing = new ArrayList<>();
            items.forEach((item) -> {
                listing.add(new Pair<>(item, ItemConstants.getInventoryType(item.getItemId())));
            });
            ItemFactory.STORAGE.saveItems(listing, accountId);
        } catch (SQLException ex) {
            System.err.println("Error saving storage" + ex);
        }
    }

    public Item takeOut(byte slot) {
        if (slot >= items.size() || slot < 0) {
            return null;
        }
        changed = true;
        Item ret = items.remove(slot);
        InventoryType type = ItemConstants.getInventoryType(ret.getItemId());
        typeItems.put(type, new ArrayList<>(filterItems(type)));
        return ret;
    }
    
    public void arrange() { 
        Collections.sort(items, (Item o1, Item o2) -> {
            if (o1.getItemId() < o2.getItemId()) {
                return -1;
            } else if (o1.getItemId() == o2.getItemId()) {
                return 0;
            } else {
                return 1;
            }
        });
        for (InventoryType type : InventoryType.values()) {
            typeItems.put(type, items);
        }
    }

    public void store(Item item) {
        changed = true;
        items.add(item);
        InventoryType type = ItemConstants.getInventoryType(item.getItemId());
        typeItems.put(type, new ArrayList<>(filterItems(type)));
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    private List<Item> filterItems(InventoryType type) {
        List<Item> ret = new LinkedList<>();

        items.stream().filter((item) -> (ItemConstants.getInventoryType(item.getItemId()) == type)).forEachOrdered((item) -> {
            ret.add(item);
        });
        return ret;
    }

    public byte getSlot(InventoryType type, byte slot) {
        byte ret = 0;
        final List<Item> it = typeItems.get(type);
        if (slot >= it.size() || slot < 0) {
            return -1;
        }
        for (Item item : items) {
            if (item == it.get(slot)) {
                return ret;
            }
            ret++;
        }
        return -1;
    }

     public void sendStorage(Client c, int npcId) {
        Collections.sort(items, (Item o1, Item o2) -> {
            if (ItemConstants.getInventoryType(o1.getItemId()).getType() < ItemConstants.getInventoryType(o2.getItemId()).getType()) {
                return -1;
            } else if (ItemConstants.getInventoryType(o1.getItemId()) == ItemConstants.getInventoryType(o2.getItemId())) {
                return 0;
            } else {
                return 1;
            }
        });
        for (InventoryType type : InventoryType.values()) {
            typeItems.put(type, new ArrayList<>(items));
        }
        c.getSession().write(PacketCreator.GetStorage(npcId, slots, items, meso));
    }

    public void sendStored(Client c, InventoryType type) {
        c.getSession().write(PacketCreator.StoreStorage(slots, type, typeItems.get(type)));
    }

    public void sendTakenOut(Client c, InventoryType type) {
        c.getSession().write(PacketCreator.TakeOutStorage(slots, type, typeItems.get(type)));
    }
    
    public Item findById(int itemId) {
        for (Item item : items) {
            if (item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public int getMeso() {
        return meso;
    }

    public void setMeso(int meso) {
        if (meso < 0) {
            return;
        }
        changed = true;
        this.meso = meso;
    }

    public void sendMeso(Client c) {
        c.getSession().write(PacketCreator.MesoStorage(slots, meso));
    }

    public boolean isFull() {
        return items.size() >= slots;
    }

    public int getSlots() {
        return slots;
    }

    public void increaseSlots(byte gain) {
        changed = true;
        this.slots += gain;
    }

    public void setSlots(byte set) {
        changed = true;
        this.slots = set;
    }
    
    public void update(Client c) {
        c.getSession().write(PacketCreator.ArrangeStorage(slots, items, true));
    }
    
    public void close() {
        typeItems.clear();
    }
}
