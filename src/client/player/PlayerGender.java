package client.player;

/**
 * @author GabrielSin (http://forum.ragezone.com/members/822844.html)
 */

public enum PlayerGender {

    MALE(0),
    FEMALE(1),
    UNSPECIFIED(2);

    private byte type;

    private PlayerGender(final int type) {
        this.type = (byte) type;
    }

    public byte asNumber() {
        return this.type;
    }

    public static PlayerGender fromNumber(final int type) {
        switch (type) {
            case 0: 
                return MALE;
            case 1: 
                return FEMALE;
            case 2:
                return UNSPECIFIED;
            default:
                return null;
        }
    }
}
