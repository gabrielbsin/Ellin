package server.minirooms;

import server.minirooms.components.SoldItem;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;
import client.player.Player;
import client.Client;
import client.player.inventory.Inventory;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemFactory;
import com.mysql.jdbc.Statement;
import constants.GameConstants;
import constants.ItemConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import packet.creators.MerchantPackets;
import packet.creators.PacketCreator;
import packet.transfer.write.OutPacket;
import server.itens.InventoryManipulator;
import server.itens.ItemInformationProvider;
import server.maps.Field;
import server.maps.object.AbstractMapleFieldObject;
import server.maps.object.FieldObjectType;
import tools.Pair;

/**
 *
 * @author XoticStory
 * @author Ronan - concurrency protection
 */

public final class Merchant extends AbstractMapleFieldObject {

    private final int itemId;
    private final int channel;
    private final int ownerId;
    private final int mapId;
    private int footHold = 0;
    private int storeid;
    private final long start;
    private AtomicBoolean open = new AtomicBoolean();
    private Field map;
    private String ownerName = "", description = "";
    public ScheduledFuture<?> schedule = null;
    private Player[] visitors = new Player[3];
    public static List<PlayerShopItem> items = new LinkedList<>();
    private final List<Pair<String, Byte>> messages = new LinkedList<>();
    private final List<SoldItem> sold = new LinkedList<>();
    private final Lock visitorLock = new ReentrantLock(true);

    public Merchant(Player owner, int itemId, String desc) {
        this.setPosition(owner.getPosition());
	this.start = System.currentTimeMillis();
        this.ownerId = owner.getId();
        this.itemId = itemId;
        this.ownerName = owner.getName();
        this.description = desc;
        this.map = owner.getMap();
        this.mapId = owner.getMapId();
        this.channel = owner.getClient().getChannel();
        this.footHold = owner.getFoothold().getId();
    }

    public void broadcastToVisitorsThreadsafe(final OutPacket packet) {
        visitorLock.lock();
        try {
            broadcastToVisitors(packet);
        } finally {
            visitorLock.unlock();
        }
    }
    
    public void broadcastToVisitors(OutPacket packet) {
        for (Player visitor : visitors) {
            if (visitor != null) {
                visitor.getClient().getSession().write(packet);
            }
        }
    }
    
    public boolean addVisitor(Player visitor) {
        visitorLock.lock();
        try {
            int i = this.getFreeSlot();
            if (i > -1) {
                visitors[i] = visitor;
                broadcastToVisitors(MerchantPackets.MerchantVisitorAdd(visitor, i + 1));
                if (getFreeSlot() == -1) {
                    this.updateMerchantBallom();
                }
                return true;
            }
            return false;
        } finally {
            visitorLock.unlock();
        }
    }
    
    public void removeVisitor(Player visitor) {
        visitorLock.lock();
        try {
            int slot = getVisitorSlot(visitor);
            boolean shouldUpdate = getFreeSlot() == -1;
            if (slot < 0) { 
                return;
            }
            
            if (visitors[slot] != null && visitors[slot].getId() == visitor.getId()) {
                visitors[slot] = null;
                broadcastToVisitors(MerchantPackets.MerchantVisitorLeave(slot + 1));
                if (shouldUpdate) {
                    this.updateMerchantBallom();
                }
            }
            
            
        } finally {
            visitorLock.unlock();
        }
    }
    
    public int getVisitorSlotThreadsafe(Player visitor) {
        visitorLock.lock();
        try {
            return getVisitorSlot(visitor);
        } finally {
            visitorLock.unlock();
        }
    }
    
    private int getVisitorSlot(Player visitor) {
        for (int i = 0; i < 3; i++) {
            if (visitors[i] != null && visitors[i].getId() == visitor.getId()){
                return i;
            }
        }
        return -1; 
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
    
    public int getMaxSize() {
        return visitors.length + 1;
    }

    public int getSize() {
    	int total = 0;
    	for (Player chr : visitors) {
            if (chr != null) {
                    total++;
            }
    	}
    	return total;
    }
    
    public void removeAllVisitors() {
        visitorLock.lock();
        try {
            for (int i = 0; i < 3; i++) {
                if (visitors[i] != null) {
                    visitors[i].setHiredMerchant(null);
                    
                    visitors[i].getClient().getSession().write(MerchantPackets.HiredMerchantForceLeaveOne());
                    visitors[i].getClient().getSession().write(MerchantPackets.HiredMerchantForceLeaveTwo());

                    visitors[i] = null;
                }
            }
           this.updateMerchantBallom();
        } finally {
            visitorLock.unlock();
        }
    }
    
    public void updateMerchantBallom() {
        if (isOpen()) {
            getMap().broadcastMessage(MerchantPackets.UpdateHiredMerchantBalloon(this, 5));
        }
    }
    
    public void withdrawMesos(Player p) {
        if (isOwner(p)) {
            synchronized (items) {
                p.withdrawMerchantMesos();
            }
        }
    }
    
    public void takeItemBack(int slot, Player p) {
        synchronized (items) {
            PlayerShopItem shopItem = items.get(slot);
            if(shopItem.isExist()) {
                if (shopItem.getBundles() > 0) {
                    Item iitem = shopItem.getItem().copy();
                    iitem.setQuantity((short) (shopItem.getItem().getQuantity() * shopItem.getBundles()));
                    
                    if (!Inventory.checkSpot(p, iitem)) {
                        p.announce(PacketCreator.ServerNotice(1, "Have a slot available on your inventory to claim back the item."));
                        p.announce(PacketCreator.EnableActions());
                        return;
                    }
                    
                    InventoryManipulator.addFromDrop(p.getClient(), iitem, "takeItemBack", true);
                }
                
                removeFromSlot(slot);
                p.announce(MerchantPackets.UpdateMerchant(this, p)); //Verify
            }
        }
    }
    
    private static boolean canBuy(Client c, Item newItem) {
        return InventoryManipulator.checkSpace(c, newItem.getItemId(), newItem.getQuantity(), newItem.getOwner()) && InventoryManipulator.addFromDrop(c, newItem, "", false);
    }
    
    private int getFreeSlot() {
        for (int i = 0; i < 3; i++) {
            if (visitors[i] == null) {
                return i;
            }
        }
        return -1;
    }
    
    public void clearInexistentItems() {
        synchronized(items) {
            for (int i = items.size() - 1; i >= 0; i--) {
                if (!items.get(i).isExist()) {
                    items.remove(i);
                }
            }
            
            try {
                this.saveItems(false);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public List<PlayerShopItem> sendAvailableBundles(int itemid) {
        List<PlayerShopItem> list = new LinkedList<>();
        List<PlayerShopItem> all = new ArrayList<>();
        
        if(!open.get()) return list;
        
        synchronized (items) {
            for(PlayerShopItem mpsi : items) all.add(mpsi);
        }
        
        for(PlayerShopItem mpsi : all) {
            if(mpsi.getItem().getItemId() == itemid && mpsi.getBundles() > 0 && mpsi.isExist()) {
                list.add(mpsi);
            }
        }
        return list;
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

    public void buy(Client c, int item, short quantity) {
        synchronized (items) {
            PlayerShopItem pItem = items.get(item);
            Item newItem = pItem.getItem().copy();
            
            newItem.setQuantity((short) ((pItem.getItem().getQuantity() * quantity)));
            if (quantity < 1 || pItem.getBundles() < 1 || newItem.getQuantity() > pItem.getBundles() || !pItem.isExist()) {
                c.announce(PacketCreator.EnableActions());
                return;
            } else if (newItem.getType() == 1 && newItem.getQuantity() > 1) {
                c.announce(PacketCreator.EnableActions());
                return;
            } else if (!pItem.isExist()) {
                c.announce(PacketCreator.EnableActions());
                return;
            }
            
            int price = (int)Math.min((long)pItem.getPrice() * quantity, Integer.MAX_VALUE);
            if (c.getPlayer().getMeso() >= price) {
                if (canBuy(c, newItem)) {
                    c.getPlayer().gainMeso(-price, false);

                    if (GameConstants.USE_ANNOUNCE_SHOPITEMSOLD) announceItemSold(newItem, price);  

                    synchronized (sold) {
                        sold.add(new SoldItem(c.getPlayer().getName(), pItem.getItem().getItemId(), quantity, price));
                    }
                    
                    Player owner = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(ownerName);
                    if (owner != null) {
                        price = tax(price);
                        owner.addMerchantMesos(price);
                    } else {
                        try {
                            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET merchantMesos = merchantMesos + " + price + " WHERE id = ?", Statement.RETURN_GENERATED_KEYS)) {
                                ps.setInt(1, ownerId);
                                ps.executeUpdate();
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    pItem.setBundles((short) (pItem.getBundles() - quantity));
                    if (pItem.getBundles() < 1) {
                        pItem.setDoesExist(false);
                    }
                } else {
                    c.getPlayer().dropMessage(1, "Seu inventário está cheio. Por favor, limpe um slot antes de comprar este item.");
                }
            } else {
                c.getPlayer().dropMessage(1, "Você não tem mesos suficientes.");
            }
            try {
                this.saveItems(false);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void announceItemSold(Item item, int mesos) {
        String qtyStr = (item.getQuantity() > 1) ? " (qty. " + item.getQuantity() + ")" : "";
        
        Player p = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterById(ownerId);
        if (p != null) {
            p.dropMessage(6, "[Hired Merchant] Sold " + qtyStr + " '" + ItemInformationProvider.getInstance().getName(item.getItemId()) + "' for " + mesos + " mesos.");
        }
    }
    
    public void saveItems(boolean shutdown) throws SQLException {
        List<Pair<Item, InventoryType>> itemsWithType = new ArrayList<>();
        
        for (PlayerShopItem pItems : items) {
            Item newItem = pItems.getItem();
            if (shutdown) {
                newItem.setQuantity((short) (pItems.getItem().getQuantity() * pItems.getBundles()));
            } else {
                newItem.setQuantity(pItems.getItem().getQuantity());
            }
            if (pItems.getBundles() > 0) {
                itemsWithType.add(new Pair<>(newItem, ItemConstants.getInventoryType(newItem.getItemId())));
            }
        }
        ItemFactory.MERCHANT.saveItems(itemsWithType, this.ownerId);
    }
    
    public void forceClose() {
        if (schedule != null) {
            schedule.cancel(false);
        }
        
        map.broadcastMessage(MerchantPackets.MerchantDestroy(getOwnerId()));
        map.removeMapObject(this);
        
        
        Player owner = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterById(ownerId);
        visitorLock.lock();
        try {
            setOpen(false);
            removeAllVisitors();
            
            if(owner != null && this == owner.getHiredMerchant()) {
                closeOwnerMerchant(owner);
            }
        } finally {
            visitorLock.unlock();
        }
        
        ChannelServer.getInstance(channel).removeHiredMerchant(ownerId);
        
        
        try {
            saveItems(true);
            synchronized (items) {
                items.clear();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
        Player p = ChannelServer.getInstance(getChannel()).getPlayerStorage().getCharacterById(ownerId);
        if (p != null) {
            p.setHasMerchant(false);
        } else {
            try {
                try (Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("UPDATE characters SET HasMerchant = 0 WHERE id = ?", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, ownerId);
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        map = null;
        schedule = null;
    }
    
    public void closeOwnerMerchant(Player chr) {
        if (this.isOwner(chr)) {
            chr.announce(MerchantPackets.MerchantOwnerLeave());
            chr.announce(MerchantPackets.MerchantLeave(0x00, 0x03));
            this.closeShop(chr.getClient(), false);
            chr.setHasMerchant(false);
        }
    }
    
    public void closeShop(Client c, boolean timeout) {
        map.removeMapObject(this);
        map.broadcastMessage(MerchantPackets.MerchantDestroy(getOwnerId()));
        c.getChannelServer().removeHiredMerchant(ownerId);
        
        
        try {
            Player p = c.getChannelServer().getPlayerStorage().getCharacterById(ownerId);
            if (p != null) {
                p.setHasMerchant(false);
            } else {
                try (
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("UPDATE characters SET HasMerchant = 0 WHERE id = ?", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, ownerId);
                    ps.executeUpdate();
                }                
            }
            
            
            List<PlayerShopItem> copyItems = getItems();
            if (check(c.getPlayer(), copyItems) && !timeout) {
                for (PlayerShopItem mpsi : copyItems) {
                    if(mpsi.isExist()) {
                        if (mpsi.getItem().getInventoryType().equals(InventoryType.EQUIP)) {
                            InventoryManipulator.addFromDrop(c, mpsi.getItem(), "", false);
                        } else {
                            InventoryManipulator.addById(c, mpsi.getItem().getItemId(), (short) (mpsi.getBundles() * mpsi.getItem().getQuantity()), "", "");
                        }
                    }
                }

                synchronized (items) {
                    items.clear();
                }
            }
            
            
            try {
                this.saveItems(false);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            synchronized (items) {
                items.clear();
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
        schedule.cancel(false);
    }
    
     private static boolean check(Player chr, List<PlayerShopItem> items) {
        List<Pair<Item, InventoryType>> li = new ArrayList<>();
        for (PlayerShopItem item : items) {
            Item it = item.getItem().copy();
            it.setQuantity((short)(it.getQuantity() * item.getBundles()));
            
            li.add(new Pair<>(it, it.getInventoryType()));
        }
        
        return Inventory.checkSpotsAndOwnership(chr, li);
    }
    
    
    public List<Pair<String, Byte>> getMessages() {
        synchronized (messages) {
            List<Pair<String, Byte>> msgList = new LinkedList<>();
            for(Pair<String, Byte> m : messages) {
                msgList.add(m);
            }
            
            return msgList;
        }
    }
    
    public void sendMessage(Player p, String msg) {
        String message = p.getName() + " : " + msg;
        byte slot = (byte) (getVisitorSlot(p) + 1);
        
        synchronized (messages) {
            messages.add(new Pair<>(message, slot));
        }
        broadcastToVisitorsThreadsafe(MerchantPackets.MerchantChat(message, slot));
    }
    
    public static ArrayList<PlayerShopItem> searchItems(Client c, int id, boolean sortByPrice) {
        if (!ItemInformationProvider.getInstance().isItemValid(id)) {
            return null;
    	}
        ArrayList<PlayerShopItem> itemz = new ArrayList<>();
        for (ChannelServer ch : ChannelServer.getAllInstances()) {
            for (Merchant merchant : ch.getHiredMerchants().values()) {
                for (PlayerShopItem item : items) {
                    if (item.item.getItemId() == id) {
                        itemz.add(item);
                    }
                }
            }
        }
        return itemz;
    }

    public int tax(int mesos) {
    	if (mesos >= 100000000)
            mesos *= .97;
    	else if (mesos >= 25000000)
            mesos *= .975;
    	else if (mesos >= 10000000)
            mesos *= .98;
    	else if (mesos >= 5000000)
            mesos *= .985;
    	else if (mesos >= 1000000)
            mesos *= .991;
        else if (mesos >= 100000)
            mesos *= .996;
    	return mesos;
    }
    
    public String getOwner() {
        return ownerName;
    }
    
    public int getOwnerId() {
        return ownerId;
    }
    
    public boolean isOpen() {
        return open.get();
    }

    public void setOpen(boolean set) {
        open.getAndSet(set);
    }
    
    public int getTimeLeft() {
        return (int) ((System.currentTimeMillis() - start) / 1000);
    }

    public List<SoldItem> getSold() {
        return sold;
    }

    public Field getMap() {
        return ChannelServer.getInstance(this.channel).getMapFactory().getMap(this.mapId);
    }
    
    public int getMapId() {
        return map.getId();
    }
    
    public int getItemId() {
        return itemId;
    }
    
    public void clearItems() {
        items.clear();
    }
    
    public int getChannel() {
        return this.channel;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getStoreId() {
        return this.storeid;
    }
    
    public void setStoreid(int storeid) {
        this.storeid = storeid;
    }
    
    public boolean isOwner(Player chr) {
        return chr.getId() == ownerId;
    }
    
    /**
     * This returns back the foothold id of the merchant
     * @return int footHold
     */
    public int getFootHold() {
        return footHold;
    }

    /**
     * This sets the foothold of the merchant
     */
    public void setFootHold(int footHold) {
        this.footHold = footHold;
    }
    
    public void clearMessages() {
        synchronized (messages) {
            messages.clear();
        }
    }

    public void addItem(PlayerShopItem item) {
        synchronized (items) {
            items.add(item);
        }
        try {
            this.saveItems(false);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    public void removeFromSlot(int slot) {
        synchronized (items) {
            items.remove(slot);
        }
        try {
            this.saveItems(false);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public void sendDestroyData(Client client) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FieldObjectType getType() {
        return FieldObjectType.HIRED_MERCHANT;
    }

    @Override
    public void sendSpawnData(Client client) {
        client.getSession().write(MerchantPackets.MerchantSpawn(this, 5));
    }

    public synchronized void visitShop(Player p) {
        visitorLock.lock();
        try {
            if (this.isOwner(p)) {
                this.setOpen(false);
                this.removeAllVisitors();

                p.announce(MerchantPackets.GetMerchant(p, this, false));
            } else if (!this.isOpen()) {
                p.announce(MerchantPackets.MerchantMaintenanceMessage());
                return;
            } else if (!this.addVisitor(p)) {
                p.dropMessage(1, "Esta loja atingiu sua capacidade máxima, por favor, tente mais tarde.");
                return;
            } else {
                p.announce(MerchantPackets.GetMerchant(p, this, false));
            }
            p.setHiredMerchant(this);
        } finally {
            visitorLock.unlock();
        }
    }
}