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

package server.shops;

import client.Client;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.violation.AutobanManager;
import constants.ItemConstants;
import database.DatabaseConnection;
import packet.creators.PacketCreator;
import server.itens.InventoryManipulator;
import server.itens.ItemInformationProvider;

public class Shop {

    private static final Set<Integer> rechargeableItems = new LinkedHashSet<>();
    private final int id;
    private final int npcId;
    private final List<ShopItem> items;

    static {
        for (int itemId = 2070000; itemId <= 2070018; itemId++) {
            if (itemId != 2070014 && itemId != 2070017) {
                rechargeableItems.add(itemId);  
            }
        }
        for (int itemId = 2330000; itemId <= 2330006; itemId++) {
            rechargeableItems.add(itemId);
        }
        rechargeableItems.add(2331000);
        rechargeableItems.add(2332000);
    }

    private Shop(int id, int npcId) {
        this.id = id;
        this.npcId = npcId;
        items = new LinkedList<>();
    }

    public void addItem(ShopItem item) {
        items.add(item);
    }

    public void sendShop(Client c) {
        c.getPlayer().setShop(this);
        c.getSession().write(PacketCreator.GetNPCShop(c, getNpcId(), items));
    }

    public void buy(Client c, int itemId, short quantity, int price) {
        if (quantity < 0) {
            AutobanManager.getInstance().autoban(c, "Tried to buy negative quantity from NPC shop");
            return;
        }
        ShopItem item = findById(itemId);
        if (item == null || item.getItemId() != itemId || item.getPrice() != price) {
            AutobanManager.getInstance().autoban(c, "Tried to buy nonexistent item from NPC shop");
            return;
        }

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        if (item.getPrice() > 0 && c.getPlayer().getMeso() >= item.getPrice() * quantity) {
            if (InventoryManipulator.checkSpace(c, itemId, quantity, "")) {
                if (ii.isRechargable(itemId)) {
                    short rechquantity = ii.getSlotMax(c, item.getItemId());
                    InventoryManipulator.addById(c, itemId, rechquantity, "Rechargable item purchased.", null, null);
                } else {
                    InventoryManipulator.addById(c, itemId, quantity, c.getPlayer().getName() + " bought " + quantity + " for " + item.getPrice() * quantity + " from shop " + id, null, null);
                }
                c.getPlayer().gainMeso(-(item.getPrice() * quantity), false);
                c.getSession().write(PacketCreator.ConfirmShopTransaction((byte) 0));
            } else {
                c.getSession().write(PacketCreator.ConfirmShopTransaction((byte) 3));
            }
        } else {
            c.getSession().write(PacketCreator.ConfirmShopTransaction((byte) 2));
        } 
    }

    public void sell(Client c, InventoryType type, short slot, short quantity) {
        if (quantity == 0xFFFF || quantity == 0) {
            quantity = 1;
        }
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item == null || type == InventoryType.CASH) {
            AutobanManager.getInstance().autoban(c, "Tried to sell nonexistent items to NPC shop");
            return;
        }
        if (ii.isThrowingStar(item.getItemId()) || ii.isBullet(item.getItemId())) {
            quantity = item.getQuantity();
        }
        if (quantity < 0) {
            return;
        }
        short iQuant = item.getQuantity();
        if (iQuant == 0xFFFF) {
            iQuant = 1;
        }
        if (quantity <= iQuant && iQuant > 0) {
            InventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
            double price;
            if (ii.isThrowingStar(item.getItemId()) || ii.isBullet(item.getItemId())) {
                price = ii.getWholePrice(item.getItemId()) / (double) ii.getSlotMax(c, item.getItemId());
            } else {
                price = ii.getPrice(item.getItemId());
            }
            int recvMesos = (int) Math.max(Math.ceil(price * quantity), 0);
            if (price != -1 && recvMesos > 0) {
                c.getPlayer().gainMeso(recvMesos, false);
            }
            c.getSession().write(PacketCreator.ConfirmShopTransaction((byte) 0x8));
        }
    }

    public void recharge(Client c, byte slot) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        Item item = c.getPlayer().getInventory(InventoryType.USE).getItem(slot);
        if (item == null || (!ItemConstants.isThrowingStar(item.getItemId()) && !ItemConstants.isBullet(item.getItemId()))) {
            AutobanManager.getInstance().autoban(c, "Tried to buy nonexistent item from NPC shop");
            return;
        }
        short slotMax = ii.getSlotMax(c, item.getItemId());

        if (item.getQuantity() < 0) {
            return;
        }
        if (item.getQuantity() < slotMax) {
            int price = (int) Math.round(ii.getPrice(item.getItemId()) * (slotMax - item.getQuantity()));
            if (c.getPlayer().getMeso() >= price) {
                item.setQuantity(slotMax);
                c.getSession().write(PacketCreator.UpdateInventorySlot(InventoryType.USE, (Item) item));
                c.getPlayer().gainMeso(-price, false, true, false);
                c.getSession().write(PacketCreator.ConfirmShopTransaction((byte) 0x8));
            }
        }
    }

    protected ShopItem findById(int itemId) {
        for (ShopItem item : items) {
            if (item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public static Shop createFromDB(int id, boolean isShopId) {
        Shop ret = null;
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        int shopId;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(isShopId ? "SELECT * FROM shops WHERE shopid = ?" : "SELECT * FROM shops WHERE npcid = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                shopId = rs.getInt("shopid");
                ret = new Shop(shopId, rs.getInt("npcid"));
                rs.close();
                ps.close();
            } else {
                rs.close();
                ps.close();
                return null;
            }
            ps = con.prepareStatement("SELECT * FROM shopitems WHERE shopid = ? ORDER BY position ASC");
            ps.setInt(1, shopId);
            rs = ps.executeQuery();
            List<Integer> recharges = new ArrayList<>(rechargeableItems);
            while (rs.next()) {
                if (!ii.itemExists(rs.getInt("itemid"))) {
                    continue;
                }
                if (ii.isThrowingStar(rs.getInt("itemid")) || ii.isBullet(rs.getInt("itemid"))) {
                    ShopItem starItem = new ShopItem((short) 1, rs.getInt("itemid"), rs.getInt("price"));
                    ret.addItem(starItem);
                    if (rechargeableItems.contains(starItem.getItemId())) {
                        recharges.remove(Integer.valueOf(starItem.getItemId()));
                    }
                } else {
                    ret.addItem(new ShopItem((short) 1000, rs.getInt("itemid"), rs.getInt("price")));
                }
            }
            for (Integer recharge : recharges) {
                ret.addItem(new ShopItem((short) 1000, recharge.intValue(), 0));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public int getNpcId() {
        return npcId;
    }

    public int getId() {
        return id;
    }
}