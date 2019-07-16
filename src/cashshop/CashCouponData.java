package cashshop;


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
