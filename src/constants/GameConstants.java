/*
 * GabrielSin (http://forum.ragezone.com/members/822844.html)
 * LeaderMS v.62
 * GameConstants
 */

package constants;

import client.player.PlayerJob;

public class GameConstants {
    
    //Thread Tracker Configuration
    public static final boolean USE_THREAD_TRACKER = true;      //[SEVERE] This deadlock auditing thing will bloat the memory as fast as the time frame one takes to lose track of a raindrop on a tempesting day. Only for debugging purposes.
    
    public static final int MAX_STATS = 999;
    public static final int PARTY_EXPERIENCE_MOD = 0; 
    public static final int MIN_LEVEL_CASHSHOP = 10;
    public static final boolean GMS = true; 
    public static final boolean GENDER_RESTRICT_RINGS = false;
    public static final int MAX_USER_COUNT_IN_MERCHANT = 4;
    public static final boolean USE_MULTIPLE_SAME_EQUIP_DROP = false;//Enables multiple drops by mobs of the same equipment, number of possible drops based on the quantities provided at the drop data.    
  
    //Event End Timestamp
    public static final long EVENT_END_TIMESTAMP = 1428897600000L;
    
    // PartyQuest
    public static final double PARTY_BONUS_EXP_RATE = 1.0;          //Rate for the party exp reward.
    public static final double PQ_BONUS_EXP_MOD = 0.5;              //Rate for the party exp reward.
    public static final double PQ_BONUS_EXP_RATE = 0.5;             //Rate for the PQ exp reward.
    
    // Announce
    public static final boolean SHOW_JOB_UPDATE = true;             //Shows job/level update
    public static final boolean USE_ANNOUNCE_SHOPITEMSOLD = false;  //Automatic message sent to owner when an item from the Player Shop or Hired Merchant is sold.
    public static final String REACHED_MAX_LEVEL = "[Congrats] %s has reached Level 200! Let us congratulate %s on such an amazing achievement!";

    // Debugs purposes
    public static final boolean USE_DEBUG = false;                  //Will enable some text prints on the client, oriented for debugging purposes.

    // NPCs
    public static final long BLOCK_NPC_RACE_CONDT = (long)(0.5 * 1000); //Time the player client must wait before reopening a conversation with an NPC.
    public static final boolean USE_DUEY = true;
    
    // Skills
    public static final int MAX_ENERGY = 10000;
    
    // RockPaperScissors
    public static final int MIN_MESO_RPS = 1000;
    
    // Guilds 
    public static final int GUILD_CRETECOST = 500000;
    public static final int GUILD_CHANGEEMBLEM_COST = 15000000;
    
    // Defaults values new persons
    public static int DEFAULT_SLOTLIMIT = 24;
    public static byte DEFAULT_BUDDY = 20;
    
    public static int DEFAULT_HP = 50;
    public static int DEFAULT_MP = 5;
    public static int DEFAULT_MAXMP = 5;
    public static int DEFAULT_MAXHP = 50;
    
    public static int DEFAULT_LEVEL = 1;
    public static PlayerJob DEFAULT_JOB = PlayerJob.BEGINNER;
    
    //Miscellaneous Configuration
    public static String TIMEZONE = "-GMT3";
    public static final boolean USE_ITEM_SORT = true;               //Enables inventory "Item Sort/Merge" feature.
     public static final boolean TRACK_WIZET_ITENS = true; 
    
    // Logs     
    public final static boolean LOG_PACKETS = true;
    public final static boolean LOG_COMMANDS = true;
    
    //Server Flags
    public static final boolean USE_OLD_GMS_STYLED_PQ_NPCS = true;  //Enables PQ NPCs with similar behaviour to old GMS style, that skips info about the PQs and immediately tries to register the party in.
    public static final int ITEM_LIMIT_ON_MAP = 200;            //Max number of items allowed on a map.
    public static final boolean USE_ENFORCE_OWL_SUGGESTIONS = true;//Forces the Owl of Minerva to always display the defined item array on GameConstants.OWL_DATA instead of those featured by the players.

    //Dangling Locks Configuration
    public static final int LOCK_MONITOR_TIME = 30 * 1000;      //Waiting time for a lock to be released. If it reaches timeout, a critical server deadlock has made present.
 
    // Anti-cheat
    public static final boolean AUTO_BAN = true; //Enables autoban for hackers
    public static final boolean TRACK_MISSGODMODE = true;
    
    //Beginner Skills Configuration
    public static final boolean USE_ULTRA_RECOVERY = true;      //Massive recovery amounts overtime.

    //Keymap configs
    public static final int[] DEFAULT_KEY = {18, 65, 2, 23, 3, 4, 5, 6, 16, 17, 19, 25, 26, 27, 31, 34, 35, 37, 38, 40, 43, 44, 45, 46, 50, 56, 59, 60, 61, 62, 63, 64, 57, 48, 29, 7, 24, 33, 41, 39};
    public static final int[] DEFAULT_TYPE = {4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 5, 6, 6, 6, 6, 6, 6, 5, 4, 5, 4, 4, 4, 4, 4};
    public static final int[] DEFAULT_ACTION = {0, 106, 10, 1, 12, 13, 18, 24, 8, 5, 4, 19, 14, 15, 2, 17, 11, 3, 20, 16, 9, 50, 51, 6, 7, 53, 100, 101, 102, 103, 104, 105, 54, 22, 52, 21, 25, 26, 23, 27};
    
    public static final boolean THIRD_KILL_EVENT = true; 
    
    public static int getTaxAmount(final int meso) {
        if (meso >= 100000000) {
            return (int) Math.round(0.06 * meso);
        } else if (meso >= 25000000) {
            return (int) Math.round(0.05 * meso);
        } else if (meso >= 10000000) {
            return (int) Math.round(0.04 * meso);
        } else if (meso >= 5000000) {
            return (int) Math.round(0.03 * meso);
        } else if (meso >= 1000000) {
            return (int) Math.round(0.018 * meso);
        } else if (meso >= 100000) {
            return (int) Math.round(0.008 * meso);
        }
        return 0;
    }
    
    /**
     * This handles a range of boss monsters that should not drop mesos
     * 
     * @param mobId
     * @return boolean
     */
    public static boolean blackListedBosses(int mobId) {
        return mobId >= 8500000 && mobId <= 9400537 || mobId >= 9400594 && mobId <= 9410004 || mobId == 6300004 || mobId == 9300379 || mobId == 9300380 || mobId == 9300381 || mobId == 7130602;
    }

    public static boolean bossCanDrop(int mobId) {
        return whiteListedBosses(mobId) || !blackListedBosses(mobId);
    }
        
    /**
     * This is a list of boss monsters that can drop mesos from the range that is
     * inside the blacklistedBosses method
     * 
     * @param mobId
     * @return boolean
     */
    public static boolean whiteListedBosses(int mobId) {
        switch (mobId) {
            case 8800002:
            case 8510000:
            case 8520000:
            case 8500002:
            case 8220000:
            case 8220001:
            case 8220002:
            case 8220003:
            case 8220004:
            case 8220005:
            case 8220006:
            case 8220007:
            case 8220009:
            case 8810018:
            case 8820014:
            case 9400408:
            case 9400409:
            case 9400609:
            case 9400610:
            case 9400611:
            case 9400612:
            case 9400613:
            case 9400623:
            case 9400633:
                return true;
        }
        return false;
    }

    public static boolean ignoreBossMesoDrop(int mobId) {
        switch (mobId) {
            case 100002:
            case 100003:
            case 100004:
            case 100005:
                return true;
        }
        return false;
    }

    public static String ordinal(int i) {
        String[] sufixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
                
            default:
                return i + sufixes[i % 10];
        }
    }
    
    public static int getHallOfFameMapid(PlayerJob job) {
        if (job.isA(PlayerJob.WARRIOR)) {
            return 102000004;
        } else if(job.isA(PlayerJob.MAGICIAN)) {
            return 101000004;
        } else if(job.isA(PlayerJob.BOWMAN)) {
            return 100000204;
        } else if(job.isA(PlayerJob.THIEF)) {
            return 103000008;
        } else if(job.isA(PlayerJob.PIRATE)) {
            return 120000105;
        } else {
            return 130000110;  
        }
    }
    
    private static final int[] mobHpVal = {0, 15, 20, 25, 35, 50, 65, 80, 95, 110, 125, 150, 175, 200, 225, 250, 275, 300, 325, 350,
        375, 405, 435, 465, 495, 525, 580, 650, 720, 790, 900, 990, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800,
        1900, 2000, 2100, 2200, 2300, 2400, 2520, 2640, 2760, 2880, 3000, 3200, 3400, 3600, 3800, 4000, 4300, 4600, 4900, 5200,
        5500, 5900, 6300, 6700, 7100, 7500, 8000, 8500, 9000, 9500, 10000, 11000, 12000, 13000, 14000, 15000, 17000, 19000, 21000, 23000,
        25000, 27000, 29000, 31000, 33000, 35000, 37000, 39000, 41000, 43000, 45000, 47000, 49000, 51000, 53000, 55000, 57000, 59000, 61000, 63000,
        65000, 67000, 69000, 71000, 73000, 75000, 77000, 79000, 81000, 83000, 85000, 89000, 91000, 93000, 95000, 97000, 99000, 101000, 103000,
        105000, 107000, 109000, 111000, 113000, 115000, 118000, 120000, 125000, 130000, 135000, 140000, 145000, 150000, 155000, 160000, 165000, 170000, 175000, 180000,
        185000, 190000, 195000, 200000, 205000, 210000, 215000, 220000, 225000, 230000, 235000, 240000, 250000, 260000, 270000, 280000, 290000, 300000, 310000, 320000,
        330000, 340000, 350000, 360000, 370000, 380000, 390000, 400000, 410000, 420000, 430000, 440000, 450000, 460000, 470000, 480000, 490000, 500000, 510000, 520000,
        530000, 550000, 570000, 590000, 610000, 630000, 650000, 670000, 690000, 710000, 730000, 750000, 770000, 790000, 810000, 830000, 850000, 870000, 890000, 910000};
    
    public static int getMonsterHP(final int level) {
        if (level < 0 || level >= mobHpVal.length) {
            return Integer.MAX_VALUE;
        }
        return mobHpVal[level];
    }
}
