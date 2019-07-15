/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server.minirooms.components;

 public class SoldItem {

    int itemid, mesos;
    short quantity;
    String buyer;

    public SoldItem(String buyer, int itemid, short quantity, int mesos) {
        this.buyer = buyer;
        this.itemid = itemid;
        this.quantity = quantity;
        this.mesos = mesos;
    }

    public String getBuyer() {
        return buyer;
    }

    public int getItemId() {
        return itemid;
    }

    public short getQuantity() {
        return quantity;
    }

    public int getMesos() {
        return mesos;
    }
}