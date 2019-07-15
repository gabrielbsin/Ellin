/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cashshop;

/**
 * 
 * @author GabrielSin
 */
public class CashCouponData {
    
    private final byte type;
    private final int data;
    private final int quantity;

    public CashCouponData(byte type, int data, int quantity) {
        this.type = type;
        this.data = data;
        this.quantity = quantity;
    }

    public final int getData() {
        return data;
    }

    public final int getQuantity() {
        return quantity;
    }

    public final byte getType() {
        return type;
    }
}
