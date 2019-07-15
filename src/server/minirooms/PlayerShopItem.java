package server.minirooms;

import client.player.inventory.Item;
import java.lang.ref.WeakReference;

public class PlayerShopItem {

    public final Item item;
    public short bundles;
    public short perBundle;
    public final int price;
    public boolean doesExist; 
    private WeakReference<Merchant> merchant;
    
    public PlayerShopItem(Item item, short bundles, int price) {
        this.item = item;
        this.bundles = bundles;
        this.price = price;
        this.doesExist = true; 
    }

    public Item getItem() {
        return item;
    }

    public short getBundles() {
        return bundles;
    }

    public int getPrice() {
        return price;
    }
    
    public boolean isExist() { 
        return doesExist; 
    } 

    public void setBundles(short bundles) {
        this.bundles = bundles;
    }

    public void setDoesExist(boolean tf) {
        this.doesExist = tf;
    }

    public int getPerBundles() {
        return perBundle;
    }

    public void setMerchant(Merchant merchant) {
    	this.merchant = new WeakReference<>(merchant);
    }
    
    public Merchant getMerchant() {
    	return merchant.get();
    }
}