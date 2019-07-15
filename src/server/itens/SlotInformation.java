/**
 * Ellin é um servidor privado de MapleStory
 * Baseado em um servidor GMS-Like na v.62
 */

package server.itens;

/**
 * @brief SlotInformation
 * @author BlackRabbit (http://forum.ragezone.com/members/2000112471.html)
 */
public enum SlotInformation {
    
    CAP(100, -1),
    FACE_ACCESSORY(101, -2),
    EYE_ACCESSORY(102, -3),
    EARRINGS(103, -4),
    TOP(104, -5),
    OVERCOAT(105, -5),
    PANTS(106, -6),
    SHOES(107, -7),
    GLOVES(108, -8),
    SHIELD(109, -10),
    CAPE(110, -9),
    RING(111, -12, -13, -15, -16),
    PENDANT(112, -17),
    
    ONE_HANDED_SWORD(130, -11),
    ONE_HANDED_AXE(131, -11),
    ONE_HANDED_BLUNT_WEAPON(132, -11),
    DAGGER(133, -11),
    WAND(137, -11),
    STAFF(138, -11),
    
    FISTS(139, -11),
    TWO_HANDED_SWORD(140, -11),
    TWO_HANDED_AXE(141, -11),
    TWO_HANDED_BLUNT_WEAPON(142, -11),
    SPEAR(143, -11),
    POLEARM(144, -11),
    BOW(145, -11),
    CROSSBOW(146, -11),
    CLAW(147, -11),
    KNUCKLER(148, -11),
    GUN(149, -11),
    
    TAMING_MOB(190, -18),
    SADDLE(191, -19),
    SPECIAL_TAMING_MOB(193, -18),
    
    CASH_ITEM;


    private int prefix;
    private int[] allowed;


    private SlotInformation() {
        prefix = 0;
    }

    private SlotInformation(int pre, int... in) {
        prefix = pre;
        allowed = in;
    }

    public int getPrefix() {
        return prefix;
    }
    
    public boolean isTwoHanded() {
        return prefix >= 139 && prefix <= 149;
    }

    public boolean isAllowed(int slot, boolean cash) {
        if (allowed != null) {
            for (Integer allow : allowed) {
                int condition = cash ? allow - 100 : allow;
                if (slot == condition) {
                    return true;
                }
            }
        }
        return cash;
    }

    public static SlotInformation getFromItemId(int id) {
        int prefix = id / 10000;
        if (prefix != 0) {
            for (SlotInformation c : values()) {
                if (c.getPrefix() == prefix) {
                    return c;
                }
            }
        }
        return CASH_ITEM;
    }
}
