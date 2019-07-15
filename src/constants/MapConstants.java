/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package constants;

import client.player.Player;
import java.util.Arrays;
import java.util.List;
import server.maps.object.FieldObjectType;

/*
 * GabrielSin (http://forum.ragezone.com/members/822844.html)
 * Ellin v.62
 * MapConstants
 */

public class MapConstants {
    
    public static final int MAX_ITEMS = 250;
    public static final int NULL_MAP = 999999999;
    public static final int GUILD_ROOM = 200000301;
    public static final boolean USE_MAXRANGE = true; 
    public static final boolean USE_ENFORCE_MDOOR_POSITION = true;
    
    public static final int MaxViewRangeSq() {
	return 722500;
    }
    
    public static int MaxViewRangeSq_Half() {
        return 500000; 
    }
    
   public static boolean isBegginerMap(int mapId) {
       switch (mapId) {
           case 0:
           case 1:
           case 2:
           case 3:
               return true;
           default:
               return false;
       }
   }
    
    public static boolean isForceRespawn(int mapid) {
        switch (mapid) {
            case 103000800:
            case 925100100: 
                return true;
            default:
                return mapid / 100000 == 9800 && (mapid % 10 == 1 || mapid % 1000 == 100);
        }
    }
    
    public static final List<FieldObjectType> RANGE_FIELD_OBJ = Arrays.asList(
        FieldObjectType.SHOP,
        FieldObjectType.ITEM,
        FieldObjectType.NPC, 
        FieldObjectType.MONSTER,
        FieldObjectType.DOOR, 
        FieldObjectType.SUMMON,
        FieldObjectType.REACTOR);
    
    public static boolean isNonRangedType(FieldObjectType type) {
        switch (type) {
            case NPC:
            case PLAYER:
            case MIST:
            case PLAYER_NPC_MERCHANT:
            case HIRED_MERCHANT:
                return true;
        }
        return false;
    }
    
    public static boolean isFreeMarketRoom(int mapid) {
        return mapid > 910000000 && mapid < 910000023;
    }
     
    public static boolean inMoonRabbitPQ(Player p) {
        int[] HPQ_FIELD = {
            910010000, 910010200, 910010300, 910010400};
        for (int i = 0; i < HPQ_FIELD.length; i++) {
            if (p.getMapId() == HPQ_FIELD[i]) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isMiniDungeonMap(int mapId) {
        switch (mapId) {
            case 100020000:
            case 105040304:
            case 105050100:
            case 221023400:
                return true;
            default:
                return false;
        }
    }
    
    public static int isStorageKeeperMap(int mapId) {
        switch (mapId) {
            case 104000000:
                return 1002005;
            case 100000200:
                return 1012009;
            case 102000000:
                return 1022005;
            case 101000000:
                return 1032006;
            case 103000000:
                return 1052017;
            case 105040300:
                return 1061008;
            case 120000200:
                return 1091004;
            case 200000000:
                return 2010006;
            case 211000100:
                return 2020004;
            case 220000000:
                return 2041008;
            case 221000200:
                return 2050004;
            case 230000002:
                return 2060008;
            case 222000000:
                return 2070000;
            case 240000000:
                return 2080005;
            case 250000000:
                return 2090000;
            case 251000000:
                return 2093003;
            case 260000000:
                return 2100000;
            case 261000000:
                return 2110000;
            case 910000000:
                return 9030100;
            case 801000000:
                return 9120009;
            case 600000000:
                return 9201081;
            case 540000000:
                return 9270042;
            default:
                System.out.println("StorageKeeper map not found: " + mapId);
                return 0;
        }
    }
   
    public static boolean inPartyQuest(Player p) {
        int[] partyQuestFields = {
            922010100, 922010200, 922010300, 
            922010400, 922010500, 922010600,
            922010700, 922010800, 922010900, 
            922011000, 910010000, 910010200, 
            910010300, 910010400, 920010100,
            920010200, 920010300, 920010400, 
            920010500, 920010600, 920010700,
            920010800, 920010900, 920011000,
            920011100, 920011300, 980010010,
            980010020, 980010101, 980010201,
            980010301, 103000800, 103000801,
            103000802, 103000803, 103000804,
            103000890, 809050000, 809050001,
            809050002, 809050003, 809050004, 
            809050005, 809050006, 809050007,
            809050008, 809050009, 809050010, 
            809050011, 809050012, 809050013,
            809050014, 809050015, 809050016,
            809050017, 980000100, 980000101,
            980000102, 980000103, 980000104, 
            980000200, 980000201, 980000202,
            980000203, 980000204, 980000300, 
            980000301, 980000302, 980000303,
            980000304, 980000400, 980000401, 
            980000402, 980000403, 980000404, 
            980000500, 980000501, 980000502,
            980000503, 980000504, 980000600, 
            980000601, 980000602, 980000603, 
            980000604, 889100001, 889100011, 
            889100021, 390000000, 390000100, 
            390000200, 390000300, 390000400,
            390000500, 390000600, 390000700,
            390000800, 390000900};
        for (int i = 0; i < partyQuestFields.length; i++) {
            if (p.getMapId() == partyQuestFields[i]) {
                return  true;
            }
        }
        return false;
    }
    
    public static boolean isAriantPartyQuestField(Player p) {
        switch (p.getMap().getId()) {
            case 980010101:
            case 980010201:
            case 980010301:
                return true;
        }
        return false;
    }
    
    public static boolean isPartyQuestOwnerMap(Player p) {
        switch (p.getMap().getId()) {
            case 922010100:  
            case 103000800:  
            case 920010000:  
            case 910010000:  
            case 980010101: 
            case 980010201:  
            case 980010301:   
            case 809050000:  
            case 925100000:    
            case 990000000:   
            return true;
        }
        return false;
    }
    
    public static boolean isChristmasPQ(Player p) {
        switch (p.getMap().getId()) {
            case 889100001:
            case 889100011:
            case 889100021:
            return true;
        }
      return false;
    }

    public static boolean isFMRoom(Player p) {
        switch (p.getMap().getId()) {
            case 910000001:
            case 910000002:
            case 910000003:
            case 910000004:
            case 910000005:
            case 910000006:   
            case 910000007:
            case 910000008:
            case 910000009:
            case 910000010:
            case 910000011:
            case 910000012:
            case 910000013:
            case 910000014:
            case 910000015:
            case 910000016:
            case 910000017:
            case 910000018:
            case 910000019:
            case 910000020:
            case 910000021:
            case 910000022:  
            return true;
        }
        return false;
    }
}
