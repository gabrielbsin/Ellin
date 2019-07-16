package client.player.inventory;

/** 
 * @author GabrielSin (http://forum.ragezone.com/members/822844.html)
 */
public enum EquipScrollResult {
    
    FAIL(0),
    SUCCESS(1),
    CURSE(2);
    
    private int value = -1;

    private EquipScrollResult(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
