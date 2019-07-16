package client.player.inventory.types;

/**
 * @author GabrielSin (http://forum.ragezone.com/members/822844.html)
 */
public enum ItemRingType {
    
    CRUSH_RING(0),
    FRIENDSHIP_RING(1),
    WEDDING_RING(2);

    int type = -1;

    private ItemRingType (int type) {
        this.type = type;
    } 

    public int getType() {
        return this.type;
    }
}
