/**
 * Ellin é um servidor privado de MapleStory
 * Baseado em um servidor GMS-Like na v.62
 */

package client.player;

/**
 * @brief PlayerGender
 * @author GabrielSin <gabrielsin@playellin.net>
 * @date   02/08/2018
 */
public enum PlayerGender {
    /**
     * Indicates the male gender.
     */
    MALE(0),
    /**
     * Indicates the female gender.
     */
    FEMALE(1),
    /**
     * Indicates an unspecified gender. Note: Used only for gender restriction
     * in item data.
     */
    UNSPECIFIED(2);

    private byte type;

    private PlayerGender(final int type) {
        this.type = (byte) type;
    }

    /**
     * Returns the underlying number for this Gender.
     * 
     * @return the value for this Gender.
     */
    public byte asNumber() {
        return this.type;
    }

    /**
     * Returns the Gender for the specified type value, or <code>null</code> if
     * there is no corresponding one.
     * 
     * @param type
     *            the value to query
     * @return the Gender corresponding to the specified value, or
     *         <code>null</code> if there is no such Gender.
     */
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
