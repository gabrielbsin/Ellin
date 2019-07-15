package server.maps;

import server.maps.object.FieldObjectType;
import server.maps.object.FieldObject;
import server.maps.object.AbstractMapleFieldObject;
import client.Client;
import client.player.Player;
import client.player.inventory.Item;
import java.awt.Point;
import java.util.concurrent.locks.ReentrantLock;
import packet.creators.PacketCreator;

public class FieldItem extends AbstractMapleFieldObject {
    
    protected Item item;
    protected byte type;
    protected int charOwnerId, meso = 0, questid = -1;
    protected boolean pickedUp = false, playerDrop;
    protected long nextExpiry = 0, nextFFA = 0;
    protected Client ownerClient;
    protected FieldObject dropper;
    private ReentrantLock itemLock = new ReentrantLock();

    public FieldItem(Item item, Point position, FieldObject dropper, Player owner, byte type, boolean playerDrop) {
        setPosition(position);
        this.item = item;
        this.dropper = dropper;
        this.charOwnerId = owner.getId();
        this.ownerClient = owner.getClient();
        this.type = type;
        this.playerDrop = playerDrop;
    }

    public FieldItem(Item item, Point position, FieldObject dropper, Player owner, byte type, boolean playerDrop, int questid) {
        setPosition(position);
        this.item = item;
        this.dropper = dropper;
        this.charOwnerId = owner.getId();
        this.type = type;
        this.playerDrop = playerDrop;
        this.questid = questid;
    }

    public FieldItem(int meso, Point position, FieldObject dropper, Player owner, byte type, boolean playerDrop) {
        setPosition(position);
        this.item = null;
        this.dropper = dropper;
        this.charOwnerId = owner.getId();
        this.meso = meso;
        this.type = type;
        this.playerDrop = playerDrop;
    }

    public FieldItem(Point position, Item item) {
        setPosition(position);
        this.item = item;
        this.charOwnerId = 0;
        this.type = 2;
        this.playerDrop = false;
    }

    public Item getItem() {
        return item;
    }
    
    public Client getOwnerClient() {
        return ownerClient;
    }

    public FieldObject getDropper() {
        return dropper;
    }
    
    public final int getItemId() {
	if (getMeso() > 0) {
	    return meso;
	}
	return item.getItemId();
    }

    public final int getOwnerId() {
        return charOwnerId;
    }

    public int getMeso() {
        return meso;
    }
    
    public final boolean isPlayerDrop() {
        return playerDrop;
    }

    public boolean isPickedUp() {
        return pickedUp;
    }

    public void setPickedUp(boolean pickedUp) {
        this.pickedUp = pickedUp;
    }
    
    public byte getDropType() {
        return type;
    }

    public void setDropType(byte z) {
        this.type = z;
    }

    public void lockItem() {
        itemLock.lock();
    }
    
    public void unlockItem() {
        itemLock.unlock();
    }

    @Override
    public FieldObjectType getType() {
        return FieldObjectType.ITEM;
    }
    
    @Override
    public void sendSpawnData(final Client client) {
        if (questid <= 0 || (client.getPlayer().getQuestStatus(questid) == 1 && client.getPlayer().needQuestItem(questid, item.getItemId()))) {
            client.getSession().write(PacketCreator.DropItemFromMapObject(this, null, getPosition(), (byte) 2));
        }
    }
    
    @Override
    public void sendDestroyData(Client client) {
        client.getSession().write(PacketCreator.RemoveItemFromMap(getObjectId(), 1, 0));
    }

    public void registerExpire(final long time) {
	nextExpiry = System.currentTimeMillis() + time;
    }

    public void registerFFA(final long time) {
	nextFFA = System.currentTimeMillis() + time;
    }

    public boolean shouldExpire() {
	return !pickedUp && nextExpiry > 0 && nextExpiry < System.currentTimeMillis();
    }

    public boolean shouldFFA() {
	return !pickedUp && type < 2 && nextFFA > 0 && nextFFA < System.currentTimeMillis();
    }

    public void expire(final Field map) {
	pickedUp = true;
	map.broadcastMessage(PacketCreator.RemoveItemFromMap(getObjectId(), 0, 0));
	map.removeMapObject(this);
    }
}