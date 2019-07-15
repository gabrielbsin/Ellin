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

import cashshop.CashItem;
import cashshop.CashItemFactory;
import java.awt.Point;
import java.util.Iterator;
import java.util.List;

import client.player.Player;
import client.Client;
import client.player.buffs.BuffStat;
import client.player.inventory.Equip;
import client.player.inventory.Inventory;
import client.player.inventory.InventoryException;
import client.player.inventory.InventoryIdentifier;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemPet;
import client.player.violation.AutobanManager;
import constants.GameConstants;
import constants.ItemConstants;
import packet.creators.EffectPackets;
import packet.creators.PacketCreator;
import tools.FileLogger;

/**
 *
 * @author Matze
 */
public class InventoryManipulator {

    public static boolean addbyItem(final Client c, final Item item) {
        return addbyItem(c, item, false) >= 0;
    }

    public static short addbyItem(final Client c, final Item item, final boolean fromcs) {
        final InventoryType type = ItemConstants.getInventoryType(item.getItemId());
        final short newSlot = c.getPlayer().getInventory(type).addItem(item);
        if (newSlot == -1) {
            if (!fromcs) {
                c.getSession().write(PacketCreator.GetInventoryFull());
                c.getSession().write(PacketCreator.GetShowInventoryFull());
            }
            return newSlot;
        }
        c.getSession().write(PacketCreator.AddInventorySlot(type, item));
        return newSlot;
    }

    public static int getUniqueId(int itemId, ItemPet pet) {
        int uniqueid = -1;
        if (ItemConstants.isPet(itemId)) {
            if (pet != null) {
                uniqueid = pet.getUniqueId();
            } else {
                uniqueid = InventoryIdentifier.getInstance();
            }
        } else if (ItemConstants.getInventoryType(itemId) == InventoryType.CASH || ItemInformationProvider.getInstance().isCash(itemId)) { //less work to do
            uniqueid = InventoryIdentifier.getInstance();  
        }
        return uniqueid;
    }

    public static void addRing(Player p, int itemId, int ringId, int sn, String partner) {
        CashItem ringItem = CashItemFactory.getItem(sn);
        if (ringItem == null) {
            return;
        }
        Item ring = ringItem.toItem(ringItem, ringId, 0, "");
        if (ring == null || ring.getUniqueId() != ringId || ring.getUniqueId() <= 0 || ring.getItemId() != itemId) {
            return;
        }
        p.getCashShop().addToInventory(ring);
    }

    public static boolean addFromDrop(Client c, Item item, String logInfo, boolean show) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        InventoryType type = ii.getInventoryType(item.getItemId());
        if (ii.isPickupRestricted(item.getItemId()) && c.getPlayer().haveItem(item.getItemId(), 1, true, false)) {
            c.getSession().write(PacketCreator.GetInventoryFull());
            c.getSession().write(PacketCreator.ShowItemUnavailable());
            return false;
        }
        short quantity = item.getQuantity();
        if (!type.equals(InventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(c, item.getItemId());
            List<Item> existing = c.getPlayer().getInventory(type).listById(item.getItemId());
            if (!ii.isThrowingStar(item.getItemId()) && !ii.isBullet(item.getItemId())) {
                if (existing.size() > 0) { 
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = (Item) i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && (eItem.getOwner().equals(eItem.getOwner()) || eItem.getOwner() == null)) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                eItem.setExpiration(item.getExpiration());
                                c.getSession().write(PacketCreator.UpdateInventorySlot(type, eItem, true));
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
            while (quantity > 0 || ItemConstants.isThrowingStar(item.getItemId()) || ItemConstants.isBullet(item.getItemId())) {
                short newQ = (short) Math.min(quantity, slotMax);
                if (newQ != 0) {
                    quantity -= newQ;
                    Item nItem = new Item(item.getItemId(), (byte) 0, newQ);
                    nItem.setOwner(item.getOwner());
                    nItem.setExpiration(item.getExpiration());
                    short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                    if (newSlot == -1) {
                        c.getSession().write(PacketCreator.GetInventoryFull());
                        c.getSession().write(PacketCreator.GetShowInventoryFull());
                        item.setQuantity((short) (quantity + newQ));
                        return false;
                    }
                    c.getSession().write(PacketCreator.AddInventorySlot(type, nItem, true));
                    if ((ItemConstants.isThrowingStar(item.getItemId()) || ItemConstants.isBullet(item.getItemId())) && quantity == 0) {
                        break;
                    }
                } else {
                    c.announce(PacketCreator.EnableActions());
                    return false;
                }
            }
        } else {
            if (quantity == 1) {
                short newSlot = c.getPlayer().getInventory(type).addItem(item);
                if (newSlot == -1) {
                    c.getSession().write(PacketCreator.GetInventoryFull());
                    c.getSession().write(PacketCreator.GetShowInventoryFull());
                    return false;
                }
                c.getSession().write(PacketCreator.AddInventorySlot(type, item, true));
            } else {
                FileLogger.printError(FileLogger.ITEM, "Tried to pickup Equip id " + item.getItemId() + " containing more than 1 quantity --> " + quantity);
                c.announce(PacketCreator.GetInventoryFull());
            }
        }
        if (show) {
            c.getSession().write(PacketCreator.GetShowItemGain(item.getItemId(), item.getQuantity()));
        }
        return true;
    }

    public static boolean addById(Client c, int itemId, short quantity, String logInfo) {
        return addById(c, itemId, quantity, logInfo, null);
    }

    public static boolean addById(Client c, int itemId, short quantity, String logInfo, String owner) {
        return addById(c, itemId, quantity, logInfo, owner, null);
    }
    
    public static boolean addById(Client c, int itemId, short quantity, String logInfo, String owner, ItemPet pet) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        InventoryType type = ItemConstants.getInventoryType(itemId);
        int uniqueid = getUniqueId(itemId, pet);
        if (!type.equals(InventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(c, itemId);
            List<Item> existing = c.getPlayer().getInventory(type).listById(itemId);
            if (!ItemConstants.isThrowingStar(itemId) && !ItemConstants.isBullet(itemId)) {
                if (existing.size() > 0) {
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = (Item) i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && (eItem.getOwner().equals(owner) || owner == null)) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.getSession().write(PacketCreator.UpdateInventorySlot(type, eItem));
                            }
                        } else {
                            break;
                        }
                    }
                }
                Item nItem;
                while (quantity > 0 || ItemConstants.isThrowingStar(itemId) || ItemConstants.isBullet(itemId)) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, uniqueid);
                        short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                        if (pet != null) {
                            nItem.setPet(pet);
                            pet.setInventoryPosition(newSlot);
                            c.getPlayer().addPet(pet);
                        }
                        if (newSlot == -1) {
                            c.getSession().write(PacketCreator.GetInventoryFull());
                            c.getSession().write(PacketCreator.GetShowInventoryFull());
                            return false;
                        }
                        if (owner != null) {
                            nItem.setOwner(owner);
                        }
                        c.getSession().write(PacketCreator.AddInventorySlot(type, nItem));
                        if ((ItemConstants.isThrowingStar(itemId) || ItemConstants.isBullet(itemId)) && quantity == 0) {
                            break;
                        }
                    } else {
                        c.getSession().write(PacketCreator.EnableActions());
                        return false;
                    }
                }
            } else {
                Item nItem = new Item(itemId, (byte) 0, quantity);
                short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    c.getSession().write(PacketCreator.GetInventoryFull());
                    c.getSession().write(PacketCreator.GetShowInventoryFull());
                    return false;
                }
                c.getSession().write(PacketCreator.AddInventorySlot(type, nItem));
                c.getSession().write(PacketCreator.EnableActions());
            }
        } else {
            if (quantity == 1) {
                Item nEquip = ii.getEquipById(itemId);
                StringBuilder logMsg = new StringBuilder("Created while adding by id. (");
                logMsg.append(logInfo);
                logMsg.append(" )");
                nEquip.log(logMsg.toString(), false);
                nEquip.setUniqueId(uniqueid);
                if (owner != null) {
                    nEquip.setOwner(owner);
                }

                short newSlot = c.getPlayer().getInventory(type).addItem(nEquip);
                if (newSlot == -1) {
                    c.getSession().write(PacketCreator.GetInventoryFull());
                    c.getSession().write(PacketCreator.GetShowInventoryFull());
                    return false;
                }
                c.getSession().write(PacketCreator.AddInventorySlot(type, nEquip));
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        return true;
    }

    public static boolean checkSpace(Client c, int itemid, int quantity, String owner) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        if (quantity <= 0 && !ItemConstants.isRechargeable(itemid)) {
            return false;
        }
        InventoryType type = ItemConstants.getInventoryType(itemid);
        if (c == null || c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        if (!type.equals(InventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(c, itemid);
            List<Item> existing = c.getPlayer().getInventory(type).listById(itemid);
            if (!ItemConstants.isThrowingStar(itemid) && !ItemConstants.isBullet(itemid)) {
                if (existing.size() > 0) { 
                    for (Item eItem : existing) {
                        short oldQ = eItem.getQuantity();
                        if (oldQ < slotMax && owner != null && owner.equals(eItem.getOwner())) {
                            final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                            quantity -= (newQ - oldQ);
                        }
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }
            }
            final int numSlotsNeeded;
            if (slotMax > 0) {
                numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
            } else {
                numSlotsNeeded = 1;
            }
            return !c.getPlayer().getInventory(type).isFull(numSlotsNeeded - 1);
        } else {
            return !c.getPlayer().getInventory(type).isFull();
        }
    }
    
    public static void removeFromSlot(Client c, InventoryType type, short slot, short quantity, boolean fromDrop) {
        removeFromSlot(c, type, slot, quantity, fromDrop, false);
    }

    public static boolean removeFromSlot(Client c, InventoryType type, short slot, short quantity, boolean fromDrop, boolean consume) {
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item != null) {
            boolean allowZero = consume && (ItemConstants.isThrowingStar(item.getItemId()) || ItemConstants.isBullet(item.getItemId()));
            c.getPlayer().getInventory(type).removeItem(slot, quantity, allowZero);
            if (item.getQuantity() == 0 && !allowZero) {
                c.getSession().write(PacketCreator.ClearInventoryItem(type, item.getPosition(), fromDrop));
            } else {
                c.getSession().write(PacketCreator.UpdateInventorySlot(type, (Item) item, fromDrop));
            }
            return true;
        }
        return false;
    }
    
    public static boolean removeById(Client c, InventoryType type, int itemId, int quantity, boolean fromDrop, boolean consume) {
        int remremove = quantity;
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        for (Item item : c.getPlayer().getInventory(type).listById(itemId)) {
            int theQ = item.getQuantity();
            if (remremove <= theQ && removeFromSlot(c, type, item.getPosition(), (short) remremove, fromDrop, consume)) {
                remremove = 0;
                break;
            } else if (remremove > theQ && removeFromSlot(c, type, item.getPosition(), item.getQuantity(), fromDrop, consume)) {
                remremove -= theQ;
            }
        }
        return remremove <= 0;
    }

    public static void move(Client c, InventoryType type, short src, short dst) {
        if (src < 0 || dst < 0 || dst > c.getPlayer().getInventory(type).getSlotLimit() || src == dst) {
            return;
        }
        final ItemInformationProvider ii = ItemInformationProvider.getInstance();
        final Item source = c.getPlayer().getInventory(type).getItem(src);
        final Item initialTarget = c.getPlayer().getInventory(type).getItem(dst);
        if (source == null) {
            return;
        }
        short olddstQ = -1;
        if (initialTarget != null) {
            olddstQ = initialTarget.getQuantity();
        }
        short oldsrcQ = source.getQuantity();
        short slotMax = ii.getSlotMax(c, source.getItemId());
        c.getPlayer().getInventory(type).move(src, dst, slotMax);
        if (!type.equals(InventoryType.EQUIP) && initialTarget != null && initialTarget.getItemId() == source.getItemId() && !ItemConstants.isThrowingStar(source.getItemId()) && !ItemConstants.isBullet(source.getItemId())) {
            if ((olddstQ + oldsrcQ) > slotMax) {
                c.getSession().write(PacketCreator.MoveAndMergeWithRestInventoryItem(type, src, dst, (short) ((olddstQ + oldsrcQ) - slotMax), slotMax));
            } else {
                c.getSession().write(PacketCreator.MoveAndMergeInventoryItem(type, src, dst, ((Item) c.getPlayer().getInventory(type).getItem(dst)).getQuantity()));
            }
        } else {
            c.getSession().write(PacketCreator.MoveInventoryItem(type, src, dst));
        }
    }

    public static void equip(Client c, short src, short dst) {
        final ItemInformationProvider ii = ItemInformationProvider.getInstance();
        
        final Player p = c.getPlayer();
        Inventory eqpInv = p.getInventory(InventoryType.EQUIP);
        Inventory eqpdInv = p.getInventory(InventoryType.EQUIPPED);
        
        Equip source = (Equip) eqpInv.getItem(src);
        
        if (source == null) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        boolean isCash = ItemInformationProvider.getInstance().isCash(source.getItemId());
        SlotInformation slotCheck = SlotInformation.getFromItemId(source.getItemId());
        if (!slotCheck.isAllowed(dst, isCash)) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        if (!c.getPlayer().isGameMaster() && GameConstants.TRACK_WIZET_ITENS) {
            if (ItemConstants.isWizetItem(source.getItemId())) {
                removeAllById(c, source.getItemId(), false);
                return;
            }
        } 

        if (ItemConstants.isWeapon(source.getItemId()) && dst != ItemConstants.SHIELD && dst != ItemConstants.WEAPON) {
            AutobanManager.getInstance().autoban(c, "Equipment hack, itemid " + source.getItemId() + " to slot " + dst);
            return;
        }
        
        switch (dst) {
            case -6: {
                    Item top = eqpdInv.getItem((short) ItemConstants.TOP);
                    if (top != null && ItemConstants.isOverall(top.getItemId())) {
                        if (eqpdInv.isFull()) {
                            c.getSession().write(PacketCreator.GetInventoryFull());
                            c.getSession().write(PacketCreator.GetShowInventoryFull());
                            return;
                        }
                        unequip(c, (byte) -5, eqpInv.getNextFreeSlot());
                    }      
                    break;
                }
            case -5: {
                    Item top = eqpdInv.getItem((short) ItemConstants.TOP);
                    Item bottom = eqpdInv.getItem((short) ItemConstants.BOTTOM);
                    if (top != null && ItemConstants.isOverall(source.getItemId())) {
                        if (eqpInv.isFull(bottom != null && ItemConstants.isOverall(source.getItemId()) ? 1 : 0)) {
                            c.getSession().write(PacketCreator.GetInventoryFull());
                            c.getSession().write(PacketCreator.GetShowInventoryFull());
                            return;
                        }
                        unequip(c, (byte) -5, eqpInv.getNextFreeSlot());
                    }       
                    if (bottom != null && ItemConstants.isOverall(source.getItemId())) {
                        if (eqpInv.isFull()) {
                            c.getSession().write(PacketCreator.GetInventoryFull());
                            c.getSession().write(PacketCreator.GetShowInventoryFull());
                            return;
                        }
                        unequip(c, (byte) -6, eqpInv.getNextFreeSlot());
                    }     
                    break;
                }
            case -10:
                Item weapon = eqpdInv.getItem((short) ItemConstants.WEAPON);
                if (weapon != null && ItemConstants.isTwoHanded(weapon.getItemId())) {
                    if (eqpInv.isFull()) {
                        c.getSession().write(PacketCreator.GetInventoryFull());
                        c.getSession().write(PacketCreator.GetShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -11, eqpInv.getNextFreeSlot());
                }  
                break;
            case -11:
                Item shield = eqpdInv.getItem((short) ItemConstants.SHIELD);
                if (shield != null && ItemConstants.isTwoHanded(source.getItemId())) {
                    if (eqpInv.isFull()) {
                        c.getSession().write(PacketCreator.GetInventoryFull());
                        c.getSession().write(PacketCreator.GetShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -10, eqpInv.getNextFreeSlot());
                }  
                break;
            case -18:
                if (p.getMount() != null) {
                    p.getMount().setItemId(source.getItemId());
                }
                break;
            default:
                break;
        }
        
        source = (Equip) eqpInv.getItem(src);
        Equip target = (Equip) eqpdInv.getItem(dst);
        
        eqpInv.removeSlot(src);
        if (target != null) {
            eqpdInv.lockInventory();
            try {
                eqpdInv.removeSlot(dst);
            } finally {
                eqpdInv.unlockInventory();
            }
        }
        
        source.setPosition(dst);
        
        eqpdInv.lockInventory();
        try {
            eqpdInv.addFromDB(source);
        } finally {
            eqpdInv.unlockInventory();
        }
        
        if (target != null) {
            target.setPosition(src);
            eqpInv.addFromDB(target);
        }
        if (p.getBuffedValue(BuffStat.BOOSTER) != null && ii.isWeapon(source.getItemId())) {
            p.cancelBuffStats(BuffStat.BOOSTER);
        }
        
        c.getSession().write(PacketCreator.MoveInventoryItem(InventoryType.EQUIP, src, dst, (byte) 2));
        p.equipChanged();
    }

    public static void unequip(Client c, short src, short dst) {
        Equip source = (Equip) c.getPlayer().getInventory(InventoryType.EQUIPPED).getItem(src);
        Equip target = (Equip) c.getPlayer().getInventory(InventoryType.EQUIP).getItem(dst);
        if (dst < 0 || source == null) {
            return;
        }
        if (target != null && src <= 0) {
            c.getSession().write(PacketCreator.GetInventoryFull());
            return;
        }
        c.getPlayer().getInventory(InventoryType.EQUIPPED).removeSlot(src);
        if (target != null) {
            c.getPlayer().getInventory(InventoryType.EQUIP).removeSlot(dst);
        }
        source.setPosition(dst);
        c.getPlayer().getInventory(InventoryType.EQUIP).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            c.getPlayer().getInventory(InventoryType.EQUIPPED).addFromDB(target);
        }
        c.getSession().write(PacketCreator.MoveInventoryItem(InventoryType.EQUIP, src, dst, (byte) 1));
        c.getPlayer().equipChanged();
    }

    public static void drop(Client c, InventoryType type, short src, short quantity) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        if (src < 0) {
            type = InventoryType.EQUIPPED;
        }
        Item source = c.getPlayer().getInventory(type).getItem(src);
        if (quantity < 0 || c.getPlayer().getTrade() != null || c.getPlayer().getMiniGame() != null || source == null || (quantity == 0 && !ItemConstants.isRechargeable(source.getItemId()))) { 
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        int itemId = source.getItemId();
        if (quantity > source.getQuantity()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
       
        if (c.getPlayer().getItemEffect() == itemId && source.getQuantity() == 1) {
            c.getPlayer().setItemEffect(0);
            c.getPlayer().getMap().broadcastMessage(EffectPackets.ItemEffect(c.getPlayer().getId(), 0));
        } 

        Point dropPos = new Point(c.getPlayer().getPosition());
        if (quantity < source.getQuantity() && !ItemConstants.isThrowingStar(source.getItemId()) && !ItemConstants.isBullet(itemId)) {
            Item target = source.copy();
            target.setQuantity(quantity);
            source.setQuantity((short) (source.getQuantity() - quantity));
            c.getSession().write(PacketCreator.DropInventoryItemUpdate(type, source));
            if (ItemConstants.isWeddingRing(source.getItemId())) {
                c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
            }
            if (ii.isDropRestricted(target.getItemId())) {
                c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
            } else {
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, !c.getPlayer().getMap().getEverlast());
            }
        } else {
            c.getPlayer().getInventory(type).removeSlot(src);
            c.getSession().write(PacketCreator.DropInventoryItem((src < 0 ? InventoryType.EQUIP : type), src));
            if (src < 0) {
                c.getPlayer().equipChanged();
            }
            if (ii.isDropRestricted(source.getItemId())) {
                c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
            } else {
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
            }
        }
    }

    public static void removeAllById(Client c, int itemId, boolean checkEquipped) {
        InventoryType type = ItemInformationProvider.getInstance().getInventoryType(itemId);
        for (Item item : c.getPlayer().getInventory(type).listById(itemId)) {
            if (item != null) {
                removeFromSlot(c, type, item.getPosition(), item.getQuantity(), true, false);
            }
        }
        if (checkEquipped) {
            Item ii = c.getPlayer().getInventory(type).findById(itemId);
            if (ii != null) {
                c.getPlayer().getInventory(InventoryType.EQUIPPED).removeItem(ii.getPosition());
                c.getPlayer().equipChanged();
            }
        }
    }  
    
    private static boolean haveItemWithId(Inventory inv, int itemid) {
        return inv.findById(itemid) != null;
    }

    public static int checkSpaceProgressively(Client c, int itemid, int quantity, String owner, int usedSlots) {
        int returnValue;
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        InventoryType type = ItemConstants.getInventoryType(itemid);
        
        if(ii.isPickupRestricted(itemid) && haveItemWithId(c.getPlayer().getInventory(type), itemid)) {
            return 0;
        }
        
        if (!type.equals(InventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(c, itemid);
            if (!ItemConstants.isRechargeable(itemid)) {
                List<Item> existing = c.getPlayer().getInventory(type).listById(itemid);
                
                if (existing.size() > 0) {
                    for (Item eItem : existing) {
                        short oldQ = eItem.getQuantity();
                        if (oldQ < slotMax && owner.equals(eItem.getOwner())) {
                            short newQ = (short) Math.min(oldQ + quantity, slotMax);
                            quantity -= (newQ - oldQ);
                        }
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }
            }
            final int numSlotsNeeded;
            if (slotMax > 0) {
                numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
            } else if (ItemConstants.isRechargeable(itemid)) {
                numSlotsNeeded = 1;
            } else {
                numSlotsNeeded = 1;
            }
            
            returnValue = ((numSlotsNeeded + usedSlots) << 1);
            returnValue += (numSlotsNeeded == 0 || !c.getPlayer().getInventory(type).isFullAfterSomeItems(numSlotsNeeded - 1, usedSlots)) ? 1 : 0;
        } else {
            returnValue = ((quantity + usedSlots) << 1);
            returnValue += (!c.getPlayer().getInventory(type).isFullAfterSomeItems(0, usedSlots)) ? 1 : 0;
        }
        return returnValue;
    }
}