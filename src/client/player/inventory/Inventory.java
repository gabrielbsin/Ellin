package client.player.inventory;

import client.player.inventory.types.InventoryType;
import client.Client;
import client.player.Player;
import constants.ItemConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import server.itens.InventoryManipulator;
import tools.Pair;
import tools.locks.MonitoredLockType;
import tools.locks.MonitoredReentrantLock;


public class Inventory implements Iterable<Item> {

    private byte slotLimit;
    private boolean checked = false;
    private final InventoryType type;
    private Map<Short, Item> inventory = new LinkedHashMap<>();
    private Lock lock = new MonitoredReentrantLock(MonitoredLockType.INVENTORY, true);

    public Inventory(InventoryType type, byte slotLimit) {
        this.inventory = new LinkedHashMap<>();
        this.slotLimit = slotLimit;
        this.type = type;
    }
    
    public boolean contains(short i) {
    	return inventory.containsKey(i);
    }

    public Item findById(int itemId) {
        lock.lock();
           try {
            for (Item item : inventory.values()) {
                if (item.getItemId() == itemId) {
                    return item;
                }
            }
          return null;
        } finally {
           lock.unlock();
        }
    }

    public Item findByUniqueId(long uniqueid) {
        lock.lock();
        try {
            for (Item item : inventory.values()) {
                if (item.getUniqueId() == uniqueid) {
                    return item;
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    } 

    public int countById(int itemId) {
        lock.lock();
        try {
            int possesed = 0;
            for (Item item : inventory.values()) {
                if (item.getItemId() == itemId) {
                    possesed += item.getQuantity();
                }
            }
           return possesed;
        } finally {
           lock.unlock();
        }
    }

    public List<Item> listById(int itemId) {
        lock.lock();
        try {
            List<Item> ret = new ArrayList<>();
            inventory.values().stream().filter((item) -> (item.getItemId() == itemId)).forEach((item) -> {
                ret.add(item);
            });
            if (ret.size() > 1) {
                Collections.sort(ret);
            }
            return ret;
        } finally {
          lock.unlock();   
        }
    }
        
    public Collection<Item> list() {
        lock.lock();
        try {
            return Collections.unmodifiableCollection(inventory.values());
        } finally {
            lock.unlock();
        }
    }
        
    public short addItem(Item item) {
        lock.lock();
        try {
            short slotId = getNextFreeSlot();
            if (slotId < 0) {
                return -1;
            }
            inventory.put(slotId, item);
            item.setPosition(slotId);
            return slotId;
        } finally {
            lock.unlock();
        }
    }

    public void addFromDB(Item item) {
        lock.lock();
        try {
            if (item.getPosition() < 0 && !type.equals(InventoryType.EQUIPPED)) {
                return;
            }
            if (item.getPosition() > 0 && type.equals(InventoryType.EQUIPPED)) {
                return;
            }
            inventory.put(item.getPosition(), item);
        } finally {
            lock.unlock();    
        }
    }

    public void move(short sSlot, short dSlot, short slotMax) {
        lock.lock();
        try {
            Item source = (Item) inventory.get(sSlot);
            Item target = (Item) inventory.get(dSlot);
            if (source == null) {
                throw new InventoryException("Trying to move empty slot");
            }
            if (target == null) {
                source.setPosition(dSlot);
                inventory.put(dSlot, source);
                inventory.remove(sSlot);
            } else if (target.getItemId() == source.getItemId() && !ItemConstants.isThrowingStar(source.getItemId()) && !ItemConstants.isBullet(source.getItemId())) {
                if (type.getType() == InventoryType.EQUIP.getType()) {
                    swap(target, source);
                }
                if (source.getQuantity() + target.getQuantity() > slotMax) {
                    short rest = (short) ((source.getQuantity() + target.getQuantity()) - slotMax);
                    source.setQuantity(rest);
                    target.setQuantity(slotMax);
                } else {
                    target.setQuantity((short) (source.getQuantity() + target.getQuantity()));
                    inventory.remove(sSlot);
                }
            } else  {
                swap(target, source);
            }    
	} finally {
            lock.unlock();
        }
    }

    private void swap(Item source, Item target) {
        inventory.remove(source.getPosition());
        inventory.remove(target.getPosition());
        short swapPos = source.getPosition();
        source.setPosition(target.getPosition());
        target.setPosition(swapPos);
        inventory.put(source.getPosition(), source);
        inventory.put(target.getPosition(), target);
    }

    public Item getItem(short slot) {
        lock.lock();
        try {
            return inventory.get(slot);
        } finally {
            lock.unlock();
        }
    }

    public void removeItem(short slot) {
        removeItem(slot, (short) 1, false);
    }

    public void removeItem(short slot, short quantity, boolean allowZero) {
        lock.lock();
        try {
            Item item = inventory.get(slot);
            if (item == null) {
                return;
            }
            item.setQuantity((short) (item.getQuantity() - quantity));
            if (item.getQuantity() < 0) {
                item.setQuantity((short) 0);
            }
            if (item.getQuantity() == 0 && !allowZero) {
                removeSlot(slot);
            }
        } finally {
           lock.unlock();
        }
    }
       
    public void removeSlot(short slot) {
        Item item;
        lock.lock();
        try {
            item = inventory.remove(slot);
        } finally {
            lock.unlock();
        }
    }

    public boolean isFull() {
        lock.lock();
        try {
            return inventory.size() >= slotLimit;
        } finally {
            lock.unlock();
        }
    }

    public boolean isFull(int margin) {
        lock.lock();
        try {
            return inventory.size() + margin >= slotLimit;
        } finally {
            lock.unlock();
        }
    }

    public short getNextFreeSlot() {
        if (isFull()) {
            return -1;
        }
        for (short i = 1; i <= slotLimit; i++) {
            if (!inventory.keySet().contains(i)) {
                return i;
            }
        }
        return -1;
    }
    
    public short getNumFreeSlot() {
        if (isFull()) {
            return 0;
        }
        byte free = 0;
        for (short i = 1; i <= slotLimit; i++) {
            if (!inventory.keySet().contains(i)) {
                free++;
            }
        }
        return free;
    }

    public InventoryType getType() {
        return type;
    }

     @Override
    public Iterator<Item> iterator() {
        return Collections.unmodifiableCollection(list()).iterator();
    }

    public boolean checked() {
        return checked;
    }

    public void checked(boolean yes) {
        checked = yes;
    }
    
    public void addSlots(int add) {
        slotLimit += (byte) add;
    }

    public byte getSlotLimit() {
        lock.lock();
        try {
            return slotLimit;
        } finally {
            lock.unlock();
        }
    }

    public void setSlotLimit(int newLimit) {
        lock.lock();
        try {
            slotLimit = (byte) newLimit;
        } finally {
            lock.unlock();
        }
    }
    
    public static boolean checkSpot(Player chr, Item item) {
    	return !chr.getInventory(InventoryType.getByType(item.getType())).isFull();
    }
    
    public static boolean checkSpots(Player p, List<Pair<Item, InventoryType>> items) {
        List<Integer> zeroedList = new ArrayList<>(5);
        for (byte i = 0; i < 5; i++) zeroedList.add(0);
        
        return checkSpots(p, items, zeroedList);
    }
    
    public static boolean checkSpots(Player p, List<Pair<Item, InventoryType>> items, List<Integer> typesSlotsUsed) {
        Map<Integer, Short> rcvItems = new LinkedHashMap<>();
        Map<Integer, Byte> rcvTypes = new LinkedHashMap<>();
        
        items.stream().forEach((item) -> {
            Integer itemId = item.left.getItemId();
            Short quantity = rcvItems.get(itemId);

            if (quantity == null) {
                rcvItems.put(itemId, item.left.getQuantity());
                rcvTypes.put(itemId, item.right.getType());
            } else {
                rcvItems.put(itemId, (short)(quantity + item.left.getQuantity()));
            }
        });
        
        Client c = p.getClient();
        for (Map.Entry<Integer, Short> it: rcvItems.entrySet()) {
            int itemType = rcvTypes.get(it.getKey()) - 1;
            int usedSlots = typesSlotsUsed.get(itemType);

            int result = InventoryManipulator.checkSpaceProgressively(c, it.getKey(), it.getValue(), "", usedSlots);
            boolean hasSpace = ((result % 2) != 0);

            if (!hasSpace) return false;
            typesSlotsUsed.set(itemType, (result >> 1));
        }
        
    	return true;
    }
    
    private static long fnvHash32(final String k) {
        final int FNV_32_INIT = 0x811c9dc5;
        final int FNV_32_PRIME = 0x01000193;

        int rv = FNV_32_INIT;
        final int len = k.length();
        for(int i = 0; i < len; i++) {
            rv ^= k.charAt(i);
            rv *= FNV_32_PRIME;
        }
        
        return rv >= 0 ? rv : (2L * Integer.MAX_VALUE) + rv;
    }
    
    private static Long hashKey(Integer itemId, String owner) {
        return (itemId.longValue() << 32L) + fnvHash32(owner);
    }
    
    public static boolean checkSpotsAndOwnership(Player p, List<Pair<Item, InventoryType>> items) {
        List<Integer> zeroedList = new ArrayList<>(5);
        for(byte i = 0; i < 5; i++) zeroedList.add(0);
        
        return checkSpotsAndOwnership(p, items, zeroedList);
    }
    
    public static boolean checkSpotsAndOwnership(Player p, List<Pair<Item, InventoryType>> items, List<Integer> typesSlotsUsed) {
        Map<Long, Short> rcvItems = new LinkedHashMap<>();
        Map<Long, Byte> rcvTypes = new LinkedHashMap<>();
        Map<Long, String> rcvOwners = new LinkedHashMap<>();
        
        items.stream().forEach((item) -> {
            Long itemHash = hashKey(item.left.getItemId(), item.left.getOwner());
            Short quantity = rcvItems.get(itemHash);
            
            if (quantity == null) {
                rcvItems.put(itemHash, item.left.getQuantity());
                rcvTypes.put(itemHash, item.right.getType());
                rcvOwners.put(itemHash, item.left.getOwner());
            } else {
                rcvItems.put(itemHash, (short)(quantity + item.left.getQuantity()));
            }
        });
        
        Client c = p.getClient();
        for (Map.Entry<Long, Short> it: rcvItems.entrySet()) {
            int itemType = rcvTypes.get(it.getKey()) - 1;
            int usedSlots = typesSlotsUsed.get(itemType);

            Long itemId = it.getKey() >> 32L;

            int result = InventoryManipulator.checkSpaceProgressively(c, itemId.intValue(), it.getValue(), rcvOwners.get(it.getKey()), usedSlots);
            boolean hasSpace = ((result % 2) != 0);

            if (!hasSpace) return false;
            typesSlotsUsed.set(itemType, (result >> 1));
        }
        
    	return true;
    }
    
    public int freeSlotCountById(int itemId, int required) {
        List<Item> itemList = listById(itemId);
        int openSlot = 0;
        
        if (!ItemConstants.isRechargeable(itemId)) {
            for (Item item : itemList) {
                required -= item.getQuantity();

                if(required >= 0) {
                    openSlot++;
                    if(required == 0) return openSlot;
                } else {
                    return openSlot;
                }
            }
        } else {
            for (Item item : itemList) {
                required -= 1;

                if (required >= 0) {
                    openSlot++;
                    if(required == 0) return openSlot;
                } else {
                    return openSlot;
                }
            }
        }
        
        return -1;
    }
    
//    public Collection<Item> getItems() {
//        return inventory.values();
//    }
//   
    public boolean isFullAfterSomeItems(int margin, int used) {
        lock.lock();
        try {
            return inventory.size() + margin >= slotLimit - used;
        } finally {
            lock.unlock();
        }
    }
    
    public void lockInventory() {
        lock.lock();
    }
    
    public void unlockInventory() {
        lock.unlock();
    }
}