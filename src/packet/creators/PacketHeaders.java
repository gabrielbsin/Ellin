/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packet.creators;

/**
 *
 * @author GabrielSin
 */
public class PacketHeaders {
    
    public static final byte
        STATUS_INFO_INVENTORY = 0,
        STATUS_INFO_QUEST = 1,
        STATUS_INFO_EXPIRE = 2,
        STATUS_INFO_EXP = 3,
        STATUS_INFO_FAME = 4,
        STATUS_INFO_MESOS = 5,
        STATUS_INFO_GUILD_POINTS = 6,
        INVENTORY_STAT_UPDATE = 0,
        INVENTORY_QUANTITY_UPDATE = 1,
        INVENTORY_CHANGE_POSITION = 2,
        INVENTORY_CLEAR_SLOT = 3,
        //got these from Vana, I'm really interested in how they work! ^.^
        GM_BLOCK = 4,
        GM_INVALID_CHAR_NAME = 6,
        GM_SET_GET_VAR_RESULT = 9,
        GM_HIDE = 16,
        GM_HIRED_MERCHANT_PLACE = 19,
        GM_WARNING = 29;
}
