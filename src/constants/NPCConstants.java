/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package constants;

import server.maps.Field;

/*
 * GabrielSin (http://forum.ragezone.com/members/822844.html)
 * LeaderMS v.62
 * NPCConstants
 */

public class NPCConstants {
    
    
    public static final int MYOMYO_SHOP = 912;
    public static final int[] DISABLE_NPCS = {};
    public static final int[] SCRIPTABLE_NPCS = {9000031};
    public static final boolean DISABLE_MAPLETV = false;
    public static String DISABLE_NPCS_MESSAGE = "Hi %s, this NPC is currently disabled!";

    public static final int[] MAPLE_TV =  {
        9201066, 9250023, 9250024, 9250025, 9250026,
        9250042, 9250043, 9250044, 9250045, 9250046, 
        9270000, 9270001, 9270002, 9270003, 9270004, 
        9270005, 9270006, 9270007, 9270008, 9270009,
        9270010, 9270011, 9270012, 9270013, 9270014,
        9270015, 9270016, 9270040, 9270066};
    
    public static boolean hasMapleTV(Field map) { 
        int tvIds[] = {
        9201066, 9250023, 9250024, 9250025, 9250026,
        9250042, 9250043, 9250044, 9250045, 9250046, 
        9270000, 9270001, 9270002, 9270003, 9270004, 
        9270005, 9270006, 9270007, 9270008, 9270009,
        9270010, 9270011, 9270012, 9270013, 9270014,
        9270015, 9270016, 9270040, 9270066}; 
        for (int id : tvIds) { 
            if (map.containsNPC(id)) { 
                return true; 
            } 
        } 
        return false; 
    }  
}
