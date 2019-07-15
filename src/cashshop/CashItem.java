/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cashshop;

import client.player.inventory.Equip;
import client.player.inventory.InventoryIdentifier;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemPet;
import client.player.inventory.ItemRing;
import constants.ItemConstants;
import server.itens.InventoryManipulator;
import server.itens.ItemInformationProvider;

/**
 * 
 * @author GabrielSin
 */
public class CashItem {
    
    private final int serialNumber;
    private final int itemId;
    private final int price;
    private final int period;
    private final int gender;
    private final short quantity;
    private final boolean onSale;

    /**
     *
     * @param sn
     * @param itemId
     * @param price
     * @param count
     * @param onSale
     * @param period
     */
    public CashItem (int sn, int itemId, int price, short count, boolean onSale, int period, int gender) {
        this.serialNumber = sn;
        this.itemId = itemId;
        this.price = price;
        this.quantity = count;
        this.onSale = onSale;
        this.period = period;
        this.gender = gender;
    }

    public int getSN() {
        return serialNumber;
    }

    public int getItemId() {
        return itemId;
    }

    public int getPrice() {
        return price;
    }

    public short getCount() {
        return quantity;
    }

    public boolean isOnSale() {
        return onSale;
    }
    
    public int getPeriod() {
        return period;
    }
    
    public int getGender() {
        return gender;
    }
    
    public boolean genderEquals(int g) {
        return g == this.gender || this.gender == 2;
    }

    public Item toItem(CashItem cItem) {
       return toItem(cItem, InventoryManipulator.getUniqueId(cItem.getItemId(), null), 0, "");
    }

    public Item toItem(CashItem cItem, int quantity) {
        return toItem(cItem, InventoryManipulator.getUniqueId(cItem.getItemId(), null), quantity, "");
    }

    public Item toItem(CashItem cItem, int uniqueId, int quantity, String gift) {
        if (uniqueId < 1) {
            uniqueId = InventoryIdentifier.getInstance();
        }
        long nPeriod = cItem.getPeriod();
        if (nPeriod  < 1 || ItemConstants.isPet(itemId)) {
            nPeriod = 45;
        }
        Item ret;
        if (ItemConstants.getInventoryType(cItem.getItemId()) == InventoryType.EQUIP) {
            Equip eq = (Equip) ItemInformationProvider.getInstance().getEquipById(cItem.getItemId());
            eq.setUniqueId(uniqueId);
            eq.setGiftFrom(gift);
            eq.setExpiration((long) (System.currentTimeMillis() + (long) (nPeriod * 24 * 60 * 60 * 1000)));
            if (ItemConstants.isEffectRing(cItem.getItemId()) && uniqueId > 0) {
                ItemRing ring = ItemRing.loadFromDb(uniqueId);
                if (ring != null) {
                    eq.setRing(ring);
                }
            }
            ret = eq.copy();
        } else {
            Item item = new Item(itemId, (byte) 0, (short) (quantity > 0 ? quantity : cItem.getCount()), uniqueId);
            item.setExpiration((long) (System.currentTimeMillis() + (long) (nPeriod * 24 * 60 * 60 * 1000)));
            item.setGiftFrom(gift);
            if (ItemConstants.isPet(cItem.getItemId())) {
                final ItemPet pet = ItemPet.createPet(cItem.getItemId(), uniqueId);
                if (pet != null) {
                    item.setPet(pet);
                }
            }
            ret = item.copy();
        }
        ret.setSN(serialNumber);
        return ret;
    }
}
