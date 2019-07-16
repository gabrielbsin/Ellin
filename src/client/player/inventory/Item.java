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

package client.player.inventory;

import client.player.inventory.types.InventoryType;
import client.player.inventory.types.ItemType;
import constants.ItemConstants;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class Item implements Comparable<Item> {

    private final int id;
    private int sn;
    private int uniqueid = -1;
    private byte flag;
    private short position;
    private short quantity;
    private long expiration;
    public String owner = "";
    public String giftFrom = "";
    protected List<String> log;
    private ItemPet pet = null;
    protected ItemRing ring = null;
    private boolean disappearsAtLogout = false;

    public Item(int id, short position, short quantity) {
        super();
        this.id = id;
        this.position = position;
        this.quantity = quantity;
        this.log = new LinkedList<>();
        this.flag = 0;
    }

    public Item(int id, short position, short quantity, int uniqueid) {
        super();
        this.id = id;
        this.position = position;
        this.quantity = quantity;
        this.uniqueid = uniqueid;
        this.log = new LinkedList<>();
    }

    public Item copy() {
        final Item ret = new Item(id, position, quantity, uniqueid);
        ret.pet = pet;
        ret.expiration = expiration;
        ret.owner = owner;
        ret.flag = flag;
        ret.log = new LinkedList<>(log);
        return ret;
    }

    public void setPosition(final short position) {
        this.position = position;
         if (pet != null) {
            pet.setInventoryPosition(position);
        }
    }

    public void setQuantity(short quantity) {
        this.quantity = quantity;
    }
    
    public int getItemId() {
        return id;
    }
    
    public short getPosition() {
        return position;
    }

    public short getQuantity() {
        return quantity;
    }

    public byte getType() {
        return ItemType.ITEM;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "Item: " + id + " quantity: " + quantity;
    }

    public List<String> getLog() {
        return Collections.unmodifiableList(log);
    }

    public void log(String msg, boolean fromDB) {
    }

    public long getExpiration() {
        return expiration;
    }
    
    public void setExpiration(long expire) {
        this.expiration = expire;
    }

    public int getSN() {
        return sn;
    }
    
    public int getUniqueId() {
        return uniqueid;
    }
    
    public void setUniqueId(int ui) {
	this.uniqueid = ui;
    }
     
    public String getGiftFrom() {
        return giftFrom;
    }

    public void setGiftFrom(String giftFrom) {
        this.giftFrom = giftFrom;
    }
    
    public final ItemPet getPet() {
        return pet;
    }
    
    public final void setPet(final ItemPet pet) {
        this.pet = pet;
    }
    
    public void setSN(int sn) {
        this.sn = sn;
    }
    
    public boolean disappearsAtLogout() {
    	return this.disappearsAtLogout;
    }
    
    public void setDisappearsAtLogout(boolean toggle) {
    	this.disappearsAtLogout = toggle;
    }

    @Override
    public int compareTo(Item other) {
        if (this.id < other.getItemId()) {
            return -1;
        } else if (this.id > other.getItemId()) {
            return 1;
        }
         return 0;
    }
    
    public void setRing(ItemRing ring) {
        this.ring = ring;
    }

    public ItemRing getRing() {
        if (!ItemConstants.isEffectRing(id) || getUniqueId() < 1) {
            return null;
        }
        if (ring == null) {
            ring = ItemRing.loadingRing(getUniqueId());
        }
        return ring;
    }

    public InventoryType getInventoryType() {
        return ItemConstants.getInventoryType(id);
    }

    public byte getItemType() { 
        if (getPet() != null) {
            return 3;
        }
        return 2;
    }
}
