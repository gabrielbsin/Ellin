
package constants;

import client.Client;
import client.player.Player;
import client.player.inventory.types.InventoryType;
import static client.player.inventory.types.InventoryType.*;
import client.player.inventory.Item;
import client.player.inventory.types.WeaponType;
import static client.player.inventory.types.WeaponType.*;
import client.player.skills.PlayerSkill;

import java.util.List;

import tools.Pair;

/*
 * GabrielSin (http://forum.ragezone.com/members/822844.html)
 * Ellin v.62
 * ItemConstants
 */ 

public class ItemConstants {
    
    public static final int HP_ITEM = 1812002;
    public static final int MP_ITEM = 1812003;
    public static final int SILVER_BOX_KEY = 5490000;
    public static final int MAPLELEAF_ITEM = 4001126;
    public static final int GRUMPY_ITEM = 4032176;
    public static final int CHRISTMAS_LEAF_ITEM = 4000314;
    public static final int PET_COME = 0x80;
    public static final int UNKNOWN_SKILL = 0x100;
    public static final int PASSED_GAS = 5281000;
    public static final int MESO_MAGNET = 1812000;
    public static final int ITEM_POUCH = 1812001;
    public static final int ITEM_IGNORE = 1812007;
    public static final int ARIANT_JEWEL = 4031868;
    public static final int PROTECTIVE_SHIELD = 2022269;
    
    public static final int[] OWL_DATA = new int[]{1082002, 2070005, 2070006, 1022047, 1102041, 2044705, 2340000, 2040017, 1092030, 2040804};

    
    public static final Pair<?, ?>[] NECKSON_CARDS = {
        new Pair<>(4031530, 100),
        new Pair<>(4031531, 250)
    };
    
    public static final int [] CARDS_4HRS = {
        5211048, 5360042
    };
    
    public static final int[] WIZET_ITEMS = {
        1002140, 1042003, 1062007, 1322013
    };
    
    public static final int[] CARNIVAL_DROPS = {
        2022157, 2022158, 2022159, 2022160, 
        2022161, 2022162, 2022163, 2022164,
        2022165, 2022166, 2022167, 2022168,
        2022169, 2022170, 2022171, 2022172,
        2022173, 2022174, 2022175, 2022176,
        2022177, 2022178, 4001129
    };
    
    public static final int[] WEDDING_RINGS = {
        1112803, 1112806, 1112807, 1112809
    };

    public static final byte 
        TOP = -5,
        BOTTOM = -6,
        SHOES = -7,
        SHIELD = -10,
        WEAPON = -11,
        MOUNT = -18;
    
    public static int [] CHARM_ITEM = {
        5130000, 4031283, 4140903
    };
    
    public static int[] BLOCKED_CS = {
        1812006, 1812007, 5230000, 5400000, 
        5211048, 5360042, 5401000, 5430000,
        5140000, 5140001, 5140002, 5140003,
        5140004, 1912004, 1902009, 1912003, 
        1902008, 5140006, 5370000,5370001,
        5281000};  
    
    public static final int[] EXP_CARDS = { 
        5210000, 5210001, 5210002, 5210003, 
        5210004, 5210005, 5211000, 5211003, 
        5211004, 5211005, 5211006, 5211007, 
        5211008, 5211009, 5211010, 5211011, 
        5211012, 5211013, 5211014, 5211015, 
        5211016, 5211017, 5211018, 5211037, 
        5211038, 5211039, 5211040, 5211041, 
        5211042, 5211043, 5211044, 5211045, 
        5211046, 5211047, 5211048, 5211049,
        5211001, 5211002};
    
    public static final int[] DROP_CARDS = {
        5360000, 5360001, 5360002, 5360003,
        5360004, 5360005, 5360006, 5360007,
        5360008, 5360009, 5360010, 5360011,
        5360012, 5360013, 5360014, 5360042};
    
    public static int [] GM_SCROLLS = {
        2040603, 2044503, 2041024, 2041025, 
        2044703, 2044603, 2043303, 2040303, 
        2040807,2040806, 2040006, 2040007, 
        2043103, 2043203, 2043003, 2040507,
        2040506, 2044403, 2040903, 2043703,
        2040709, 2040710, 2040711, 2044303,
        2040403, 2044103, 2044203, 2044003,
        2044003};
    
    public static int [] QUEST_ITEMS = {
        4001022, 4001023, 4001007, 4001008,
        4001101, 4001095, 4001096, 4001097,
        4001098, 4001099, 4001100, 2022266, 
        2022267, 2022269, 2270002, 2100067,
        4031868, 4001045, 4001046, 4001047,
        4001048, 4001049, 4001050, 4001051,
        4001052, 4001053, 4001054, 4001055,
        4001056, 4001057, 4001058, 4001059,
        4001060, 4001061, 4001062, 4001063, 
        4001106, 4001120, 4001121, 4001122, 
        1032033, 4001024, 4001025, 4001026, 
        4001027, 4001028, 4001031, 4001032,
        4001033, 4001034, 4001035, 4001037};

    public static class RewardGoldBox {
        
        public static int GOLD_BOX_ITEM = 4280000;
        
        public static int[] COMMON = {
            3010004, 3010015, 3010013, 3011000,
            1382047, 1382048, 1372010, 1382010, 
            1002271, 1402016, 1442020, 1402037, 
            1442008, 1402035, 1422031, 1412019,
            1412010, 1462018, 1462013, 1452009, 
            1332052, 1002283, 1002328, 1002327,
            1082210, 1482010, 2290028, 2290056,
            2290034, 2290044, 2290121, 2290026, 
            2044800, 2044900, 2040800, 5200000};
        
        public static int[] UNCOMMON = {
            3010009, 3010016, 3010017, 3010014,
            3010041, 2040809, 2040811, 2043805, 
            2043305, 2040715, 2044804, 2044405,
            2040713, 2022179, 5200002};
        
        public static int[] RARE = {
            3010046, 3010047, 3010058, 3010057,
            3010043, 3010071, 3010085, 2290007,
            2290049, 2290043, 2290122, 2290039,
            2290055, 2290120, 2290087, 2290063,
            2290103, 2290069, 2290033, 2049000,
            2049001, 2049002, 2049003, 2022282,
            2022283};
    }
    
    public static class RewardSilverBox {
        
        public static int SILVER_BOX_ITEM = 4280001;
        
        public static int[] COMMON = {
            3010000, 3010001, 3010002, 3010003,
            3010006, 1002253, 1002254, 1442019,
            1402028, 1312024, 1002339, 1082116,
            1452014, 1452015, 1452013, 1332029,
            1332027, 1332036, 1472026, 1082210,
            1482010, 2290124, 2290036, 2290088, 
            2290117, 2290123, 2044800, 2044900, 
            2040800, 5200000};
        
        public static int[] UNCOMMON = {
            3010018, 3010019, 3010025, 3010011,
            3010012, 1432018, 2040801, 2044701,
            2043801, 2043301, 2040704, 2044801,
            2044401, 2040701, 2022179, 5200001};
        
        public static int[] RARE = {
            3010007, 3010008, 3010010, 3010040,
            3010060, 3010062, 2290061, 2290085,
            2290089, 2290063, 2290023, 2290003,
            2049000, 2049001, 2049002, 2049003, 
            2022282, 2022283};
    }
    
    public static boolean isGmScroll(int a) {
        for (int k : GM_SCROLLS) {
            if (k == a) return true;
        }
        return false;
    }

    public static boolean isUse(int itemid) {
        return itemid >= 2000000 && itemid < 3000000;
    }

    public static boolean isEquip(int itemid) {
        return itemid < 2000000;
    }

    public static boolean isEtc(int itemid) {
        return itemid >= 4000000 && itemid < 5000000;
    }

    public static boolean isCash(int itemid) {
        return itemid / 1000000 == 5;
    }

    public static boolean isSetup(int itemid) {
        return itemid >= 3000000 && itemid < 4000000;
    }

    public static boolean isCurrency(int itemid){
        return itemid >= 4032015 && itemid < 4032017;
    }
    
    public static final boolean isPet(int itemId) {
        return itemId / 1000 == 5000;
    }
    
    public static boolean isWeapon(final int itemId) {
        return itemId >= 1300000 && itemId < 1533000;
    }
    
    public static final boolean isThrowingStar(int itemId) {
        return itemId / 10000 == 207;
    }

    public static final boolean isBullet(int itemId) {
        return itemId / 10000 == 233;
    }

    public static final boolean isRechargeable(int itemId) {
        return itemId / 10000 == 233 || itemId / 10000 == 207;
    }

    public static final boolean isArrowForCrossBow(int itemId) {
        return itemId / 1000 == 2061;
    }

    public static final boolean isArrowForBow(int itemId) {
        return itemId / 1000 == 2060;
    } 
    
    public static boolean isOverall(final int itemId) {
        return itemId / 10000 == 105;
    }
    
    public static boolean isRing(int itemId) {
        return itemId >= 1112000 && itemId < 1113000;
    }

    public static boolean isEffectRing(int itemid) {
        return isFriendshipRing(itemid) || isCrushRing(itemid) || isWeddingRing(itemid);
    }
    
    public static boolean isTownScroll(final int id) {
        return id >= 2030000 && id < 2040000;
    }
    
    public static boolean isFaceExpression(int itemId) {
        return itemId / 10000 == 516;
    }
    
    public static boolean canEquip(PlayerSkill skill, Client c) {
        return SkillConstants.isBeginnerSkill(skill.getId()) || !SkillConstants.isCorrectJobForSkillRoot(c.getPlayer().getJob().getId(), skill.getId() / 10000) || !skill.isActiveSkill();
    }
    
    public static boolean isWeddingRing(int itemId) {
        for (int i : ItemConstants.WEDDING_RINGS) {
            if (itemId == i) {
                return true;
            }
        }
         return false;
    }
    
    public static boolean isWizetItem(int itemId) {
        for (int i : ItemConstants.WIZET_ITEMS) {
            if (itemId == i) {
                return true;
            }
        }
         return false;
    }
    
    public static boolean isTwoHanded(final int itemId) {
        switch (getWeaponType(itemId)) {
            case AXE2H:
            case GUN:
            case KNUCKLE:
            case BLUNT2H:
            case BOW:
            case CLAW:
            case CROSSBOW:
            case POLE_ARM:
            case SPEAR:
            case SWORD2H:
                return true;
            default:
                return false;
        }
    }
     
    public static WeaponType getWeaponType(final int itemId) {
        int cat = itemId / 10000;
        cat = cat % 100;
        switch (cat) {
            case 30:
                return WeaponType.SWORD1H;
            case 31:
                return WeaponType.AXE1H;
            case 32:
                return WeaponType.BLUNT1H;
            case 33:
                return WeaponType.DAGGER;
            case 37:
                return WeaponType.WAND;
            case 38:
                return WeaponType.STAFF;
            case 40:
                return WeaponType.SWORD2H;
            case 41:
                return WeaponType.AXE2H;
            case 42:
                return WeaponType.BLUNT2H;
            case 43:
                return WeaponType.SPEAR;
            case 44:
                return WeaponType.POLE_ARM;
            case 45:
                return WeaponType.BOW;
            case 46:
                return WeaponType.CROSSBOW;
            case 47:
                return WeaponType.CLAW;
            case 48:
                return WeaponType.KNUCKLE;
            case 49:
                return WeaponType.GUN;
        }
        return WeaponType.NOT_A_WEAPON;
    }
    
    public static String getNameCityIncubator(int itemId) {
        switch (itemId) {
            case 4170000:
                return "Henesys";
            case 4170001:
                return "Ellinia";
            case 4170002:
                return "Kerning";
            case 4170003:
                return "Perion";
            case 4170004:
                return "El_Nath";
            case 4170005:
                return "Ludibrium";
            case 4170006:
                return "Orbis";
            case 4170007:
                return "Aquarium";
            case 4170009:
                return "Notilus";
        }
        return "undefined";
    }

    public static boolean isCrushRing(int itemId) {
        switch (itemId) {
            case 1112001:
            case 1112002:
            case 1112003:
            case 1112005: 
            case 1112006: 
            case 1112007:
            case 1112012:
            case 1112015: 
            case 1048000:
                return true;
        }
        return false;
    }
    
    public static boolean isFriendshipRing(int itemId) {
        switch (itemId) {
            case 1112800:
            case 1112801:
            case 1112802:
            case 1112810: 
            case 1112811: 
            case 1112812: 
            case 1049000:
                return true;
        }
        return false;
    }

    public static int getFlagByInt(int type) {
        if (type == 128) {
            return PET_COME;
        } else if (type == 256) {
            return UNKNOWN_SKILL;
        }
        return 0;
    }

    public static InventoryType getInventoryType(final int itemId) {
	final byte type = (byte) (itemId / 1000000);
	if (type < 1 || type > 5) {
	    return InventoryType.UNKNOWN;
	}
	return InventoryType.getByType(type);
    }
    
    public static boolean haveSpace(final Player p, final List<Item> items) {
        byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;
        for (Item item : items) {
            final InventoryType invtype = getInventoryType(item.getItemId());
            if (null != invtype) switch (invtype) {
                case EQUIP:
                    eq++;
                    break;
                case USE:
                    use++;
                    break;
                case SETUP:
                    setup++;
                    break;
                case ETC:
                    etc++;
                    break;
                case CASH:
                    cash++;
                    break;
                default:
                    break;
            }
        }
        return !(p.getInventory(InventoryType.EQUIP).getNumFreeSlot() < eq || p.getInventory(InventoryType.USE).getNumFreeSlot() < use || p.getInventory(InventoryType.SETUP).getNumFreeSlot() < setup || p.getInventory(InventoryType.ETC).getNumFreeSlot() < etc || p.getInventory(InventoryType.CASH).getNumFreeSlot() < cash);
    }  
}
