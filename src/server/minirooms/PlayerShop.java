package server.minirooms;

import server.minirooms.components.SoldItem;
import java.util.ArrayList;
import java.util.List;
import client.player.Player;
import client.Client;
import client.player.inventory.Inventory;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import packet.creators.PacketCreator;
import packet.creators.PersonalShopPackets;
import packet.transfer.write.OutPacket;
import server.itens.InventoryManipulator;
import server.maps.object.AbstractMapleFieldObject;
import server.maps.object.FieldObjectType;
import tools.Pair;

public class PlayerShop extends AbstractMapleFieldObject {

    private final Player owner;
    private int boughtnumber = 0;
    private String description;
    private Player[] visitors = new Player[3];
    private List<SoldItem> sold = new LinkedList<>();
    private AtomicBoolean open = new AtomicBoolean(false);
    private List<PlayerShopItem> items = new ArrayList<>();
    private final List<String> bannedList = new ArrayList<>();
    private Map<Integer, Byte> chatSlot = new LinkedHashMap<>();
    private List<Pair<Player, String>> chatLog = new LinkedList<>();
    private Lock visitorLock = new ReentrantLock(true);

    public PlayerShop(Player owner, String description) {
        this.setPosition(owner.getPosition());
        this.owner = owner;
        this.description = description;
    }
    
    public void addVisitor(Player visitor) {
        visitorLock.lock();
        try {
            for (int i = 0; i < 3; i++) {
                if (visitors[i] == null) {
                    visitors[i] = visitor;
                    visitor.setSlot(i);
                    this.broadcast(PersonalShopPackets.GetPlayerShopNewVisitor(visitor, i + 1));
                    
                    if (i == 2) {
                        visitor.getMap().broadcastMessage(PersonalShopPackets.AddCharBox(this.getOwner(), 1));
                    }
                    break;
                }
            }
        } finally {
            visitorLock.unlock();
        }
    }
    
    public void forceRemoveVisitor(Player visitor) {
        if (visitor == owner) {
            owner.getMap().removeMapObject(this);
            owner.setPlayerShop(null);
        }
        
        visitorLock.lock();
        try {
            for (int i = 0; i < 3; i++) {
                if (visitors[i] != null && visitors[i].getId() == visitor.getId()) {
                    visitors[i] = null;
                    visitor.setSlot(-1);
                    this.broadcast(PersonalShopPackets.GetPlayerShopRemoveVisitor(i + 1));
                    return;
                }
            }
        } finally {
            visitorLock.unlock();
        }
    }
    
    public void removeVisitor(Player visitor) {
        if (visitor == owner) {
            owner.getMap().removeMapObject(this);
            owner.setPlayerShop(null);
        } else {
            visitorLock.lock();
            try {
                 for (int i = 0; i < 3; i++) {
                    if (visitors[i] != null && visitors[i].getId() == visitor.getId()) {
                        visitor.setSlot(-1);  
                        
                        for(int j = i; j < 2; j++) {
                            if(visitors[j] != null) owner.announce(PersonalShopPackets.GetPlayerShopRemoveVisitor(j + 1));
                            visitors[j] = visitors[j + 1];
                            if(visitors[j] != null) visitors[j].setSlot(j);
                        }
                        visitors[2] = null;
                        for(int j = i; j < 2; j++) {
                            if(visitors[j] != null) owner.announce(PersonalShopPackets.GetPlayerShopNewVisitor(visitors[j], j + 1));
                        }
                        
                        this.broadcastRestoreToVisitors();
                        return;
                    }
                }
            } finally {
                visitorLock.unlock();
            }
            
            if (owner.getPlayerShop() != null) visitor.getMap().broadcastMessage(PersonalShopPackets.AddCharBox(owner, 4));
        }
    }
    
    public void broadcastRestoreToVisitors() {
        visitorLock.lock();
        try {
            for (int i = 0; i < 3; i++) {
                if (visitors[i] != null) {
                    visitors[i].getClient().announce(PersonalShopPackets.GetPlayerShopRemoveVisitor(i + 1));
                }
            }
            
            for (int i = 0; i < 3; i++) {
                if (visitors[i] != null) {
                    visitors[i].getClient().announce(PersonalShopPackets.GetPlayerShop(this, false));
                }
            }
            
            recoverChatLog();
        } finally {
            visitorLock.unlock();
        }
    }

    public void broadcast(OutPacket packet) {
        if (owner.getClient() != null && owner.getClient().getSession() != null) {
            owner.getClient().getSession().write(packet);
        }
        broadcastToVisitors(packet);
    }
    
    public void broadcastToVisitors(OutPacket packet) {
        visitorLock.lock();
        try {
            for (int i = 0; i < 3; i++) {
                if (visitors[i] != null) {
                    visitors[i].getClient().announce(packet);
                }
            }
        } finally {
            visitorLock.unlock();
        }
    }

    public void removeVisitors() {
        List<Player> visitorList = new ArrayList<>(3);
        
        visitorLock.lock();
        try {
            try {
                for (int i = 0; i < 3; i++) {
                    if (visitors[i] != null) {
                        visitors[i].getClient().announce(PersonalShopPackets.ShopErrorMessage(10, 1));
                        visitorList.add(visitors[i]);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            visitorLock.unlock();
        }
        
        for(Player mc : visitorList) {
            forceRemoveVisitor(mc);
        }
        if (owner != null) {
            forceRemoveVisitor(getOwner());
        }
    }
    
    public void chat(Client c, String chat) {
        byte s = getVisitorSlot(c.getPlayer());
        
        synchronized(chatLog) {
            chatLog.add(new Pair<>(c.getPlayer(), chat));
            if (chatLog.size() > 25) {
                chatLog.remove(0);
            }
            chatSlot.put(c.getPlayer().getId(), s);
        }
        broadcast(PersonalShopPackets.ShopChat(c.getPlayer().getName() + " : " + chat, s));
    }
    
    private byte getVisitorSlot(Player p) {
        byte s = 0;
        for (Player mc : getVisitors()) {
            s++;
            if (mc != null) {
                if (mc.getName().equalsIgnoreCase(p.getName())) {
                    break;
                }
            } else if (s == 3) {
                s = 0;
            }
        }
        
        return s;
    }
    
     public void banPlayer(String name) {
        if (!bannedList.contains(name)) {
            bannedList.add(name);
        }
        
        Player target = null;
        visitorLock.lock();
        try {
            for (int i = 0; i < 3; i++) {
                if (visitors[i] != null && visitors[i].getName().equals(name)) {
                    target = visitors[i];
                    break;
                }
            }
        } finally {
            visitorLock.unlock();
        }
        
        if (target != null) {
            target.getClient().announce(PersonalShopPackets.ShopErrorMessage(5, 1));
            removeVisitor(target);
        }
    }
     
    public synchronized boolean visitShop(Player p) {
         if (this.isBanned(p.getName())) {
            p.dropMessage(1, "You have been banned from this store.");
            return false;
        }
        
        visitorLock.lock();
        try {
            if(!open.get()) {
                p.dropMessage(1, "This store is not yet open.");
                return false;
            }
            
            if (this.hasFreeSlot() && !this.isVisitor(p)) {
                this.addVisitor(p);
                p.setPlayerShop(this);
                this.sendShop(p.getClient());

                return true;
            }

            return false;
        } finally {
            visitorLock.unlock();
        }
    }
    
    public List<PlayerShopItem> sendAvailableBundles(int itemid) {
        List<PlayerShopItem> list = new LinkedList<>();
        List<PlayerShopItem> all = new ArrayList<>();
        
        synchronized (items) {
            for (PlayerShopItem mpsi : items) {
                all.add(mpsi);
            }
        }
        
        for (PlayerShopItem mpsi : all) {
            if(mpsi.getItem().getItemId() == itemid && mpsi.getBundles() > 0 && mpsi.isExist()) {
                list.add(mpsi);
            }
        }
        return list;
    }
    
    public int getMapId() {
        return owner.getMapId();
    }
    
    public int getChannel() {
        return owner.getClient().getChannel();
    }
    
    public boolean isOpen() {
        return open.get();
    }
    
    public void setOpen(boolean openShop) {
        open.set(openShop);
    }
    
    public boolean isBanned(String name) {
        return bannedList.contains(name);
    }
    
    public boolean hasFreeSlot() {
        visitorLock.lock();
        try {
            return visitors[0] == null || visitors[1] == null || visitors[2] == null;
        } finally {
            visitorLock.unlock();
        }
    }

    public boolean isOwner(Player c) {
        return owner.equals(c);
    }
    
     public boolean isVisitor(Player visitor) {
        visitorLock.lock();
        try {
            return visitors[0] == visitor || visitors[1] == visitor || visitors[2] == visitor;
        } finally {
            visitorLock.unlock();
        }
    }

    public void addItem(PlayerShopItem item) {
        synchronized (items) {
            items.add(item);
        }
    }

    public void removeItem(int item) {
        items.remove(item);
    }
    
    public void sendShop(Client c) {
        visitorLock.lock();
        try {
            c.getSession().write(PersonalShopPackets.GetPlayerShop(this, isOwner(c.getPlayer())));
        } finally {
            visitorLock.unlock();
        }
    }

    public Player getOwner() {
        return owner;
    }

    public Player[] getVisitors() {
        visitorLock.lock();
        try {
            Player[] copy = new Player[3];
            for(int i = 0; i < visitors.length; i++) copy[i] = visitors[i];
                    
            return copy;
        } finally {
            visitorLock.unlock();
        }
    }

    public List<PlayerShopItem> getItems() {
        synchronized (items) {
            return Collections.unmodifiableList(items);
        }
    }
    
    public boolean hasItem(int itemid) {
        for(PlayerShopItem mpsi : getItems()) {
            if(mpsi.getItem().getItemId() == itemid && mpsi.isExist() && mpsi.getBundles() > 0) {
                return true;
            }
        }
        
        return false;
    }
    
    public List<SoldItem> getSold() {
        synchronized (sold) {
            return Collections.unmodifiableList(sold);
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public void sendDestroyData(Client client) {
        client.getSession().write(PersonalShopPackets.RemoveCharBox(this.getOwner()));
    }

    @Override
    public void sendSpawnData(Client client) {
        client.getSession().write(PersonalShopPackets.AddCharBox(this.getOwner(), 4));
    }

    @Override
    public FieldObjectType getType() {
        return FieldObjectType.SHOP;
    }
    
    private void recoverChatLog() {
        synchronized (chatLog) {
            for (Pair<Player, String> it : chatLog) {
                Player chr = it.getLeft();
                Byte pos = chatSlot.get(chr.getId());
                broadcastToVisitors(PersonalShopPackets.ShopChat(it.getRight(), pos));
            }
        }
    }
    
    private void clearChatLog() {
        synchronized(chatLog) {
            chatLog.clear();
        }
    }
    
    public void closeShop() {
        owner.getMap().broadcastMessage(PersonalShopPackets.RemoveCharBox(owner));
        clearChatLog();
        removeVisitors();
    }
    
    private void removeFromSlot(int slot) {
        items.remove(slot);
    }
    
    private static boolean canBuy(Client c, Item newItem) {
        return InventoryManipulator.checkSpace(c, newItem.getItemId(), newItem.getQuantity(), newItem.getOwner()) && InventoryManipulator.addFromDrop(c, newItem, "", false);
    }

    /**
     * no warnings for now o.op
     * @param c
     * @param item
     * @param quantity
     */
    public void buy(Client c, int item, short quantity) {
        synchronized (items) {
            if (isVisitor(c.getPlayer())) {
                PlayerShopItem pItem = items.get(item);
                Item newItem = pItem.getItem().copy();
                
                newItem.setQuantity((short) ((pItem.getItem().getQuantity() * quantity)));
                if (quantity < 1 || !pItem.isExist() || pItem.getBundles() < quantity) {
                    c.announce(PacketCreator.EnableActions());
                    return;
                } else if (newItem.getInventoryType().equals(InventoryType.EQUIP) && newItem.getQuantity() > 1) {
                    c.announce(PacketCreator.EnableActions());
                    return;
                }
                visitorLock.lock();
                try {
                    
                    int price = (int) Math.min((float)pItem.getPrice() * quantity, Integer.MAX_VALUE);
                    
                    if (c.getPlayer().getMeso() >= price) {
                        if (canBuy(c, newItem)) {
                            c.getPlayer().gainMeso(-price, false);
                            
                            owner.gainMeso(price, true);
                            
                            SoldItem soldItem = new SoldItem(c.getPlayer().getName(), pItem.getItem().getItemId(), quantity, price);
                          
                            owner.announce(PersonalShopPackets.GetPlayerShopOwnerUpdate(soldItem, item));
                            
                            synchronized (sold) {
                                sold.add(soldItem);
                            }
                            
                            pItem.setBundles((short) (pItem.getBundles() - quantity));
                            if (pItem.getBundles() < 1) {
                                pItem.setDoesExist(false);
                                if (++boughtnumber == items.size()) {
                                    owner.setPlayerShop(null);
                                    this.setOpen(false);
                                    this.closeShop();
                                    owner.dropMessage(1, "Your items are sold out, and therefore your shop is closed.");
                                }
                            }
                        } else { 
                            c.getPlayer().dropMessage(1, "Your inventory is full. Please clean a slot before buying this item.");
                        }
                    } else {
                        c.getPlayer().dropMessage(1, "You don't have enough mesos to purchase this item.");
                    }
                } finally {
                    visitorLock.unlock();
                }
            }
        }
    }
    
    public void takeItemBack(int slot, Player p) {
        synchronized (items) {
            PlayerShopItem shopItem = items.get(slot);
            if (shopItem.isExist()) {
                if (shopItem.getBundles() > 0) {
                    Item iitem = shopItem.getItem().copy();
                    iitem.setQuantity((short) (shopItem.getItem().getQuantity() * shopItem.getBundles()));
                    
                    if (!Inventory.checkSpot(p, iitem)) {
                        p.announce(PacketCreator.ServerNotice(1, "Have a slot available on your inventory to claim back the item."));
                        p.announce(PacketCreator.EnableActions());
                        return;
                    }
                    
                    InventoryManipulator.addFromDrop(p.getClient(), iitem, "", true);
                }
                
                removeFromSlot(slot);
                p.announce(PersonalShopPackets.ShopItemUpdate(this));
            }
        }
    }
}