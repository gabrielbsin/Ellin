package constants;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/*
 * GabrielSin (http://forum.ragezone.com/members/822844.html)
 * Ellin v.62
 * ServerProperties
 */

public class ServerProperties {

    public final static class Login {
        
        public static int INTERVAL; 
        public static int USER_LIMIT;
        public static int FLAG;
        public static long RANKING_INTERVAL; 
        public static short PORT;  
        public static Boolean ENABLE_PIN; 
        public static Boolean ENABLE_BALLONS; 
        public static String SERVER_NAME;
        public static String EVENT_MESSAGE;
        
        static {
            Properties p = new Properties();
            try {
                p.load(new FileInputStream(Misc.DATA_ROOT + "/Login.ini"));
              
                PORT = Short.parseShort(p.getProperty("LoginPort"));
                
                INTERVAL = Integer.parseInt(p.getProperty("LoginInterval"));
                USER_LIMIT = Integer.parseInt(p.getProperty("LoginUserLimit"));
                FLAG = Integer.parseInt(p.getProperty("LoginFlag"));
                RANKING_INTERVAL = Integer.parseInt(p.getProperty("LoginRankingInterval"));
                
                ENABLE_PIN = Boolean.parseBoolean(p.getProperty("LoginEnablePin"));
                ENABLE_BALLONS = Boolean.parseBoolean(p.getProperty("LoginEnableBallons"));
                
                SERVER_NAME = p.getProperty("LoginServerName");
                EVENT_MESSAGE = p.getProperty("LoginEventMessage");
                
            } catch (IOException e) { 
                System.out.println("Failed loading Login.ini - " + e);
            }
        }
    }
    
    public final static class Channel {
        
        public static int PORT;
        public static int COUNT;
        public static String EVENTS;
        
        static {
            Properties p = new Properties();
            try {
                p.load(new FileInputStream(Misc.DATA_ROOT + "/Channel.ini"));
              
                PORT = Integer.parseInt(p.getProperty("ChannelPort"));
                COUNT = Integer.parseInt(p.getProperty("ChannelCount"));
                EVENTS = p.getProperty("ChannelEvents");
                
            } catch (IOException e) { 
                System.out.println("Failed loading Channel.ini - " + e);
            }
        }
    }
    
    public final static class World {
        
        public static int EXP;
        public static int QUEST_EXP;
        public static int MESO;
        public static int DROP;
        public static int BOSS_DROP;
        public static int PET_EXP;
        public static int MOUNT_EXP;
        public static String SERVER_MESSAGE;
        public static String HOST;
        public static short MAPLE_VERSION;
        public static byte[] HOST_BYTE = {(byte) 127, (byte) 0, (byte) 0, (byte) 1};
        public static String REVISION;
        
        static {
            Properties p = new Properties();
            try {
                p.load(new FileInputStream(ServerProperties.Misc.DATA_ROOT + "/World.ini"));

                EXP = Integer.parseInt(p.getProperty("GameExpRate"));
                QUEST_EXP = Integer.parseInt(p.getProperty("GameQuestExpRate"));
                MESO = Integer.parseInt(p.getProperty("GameMesoRate"));
                DROP = Integer.parseInt(p.getProperty("GameDropRate"));
                BOSS_DROP = Integer.parseInt(p.getProperty("GameBossDropRate"));
                PET_EXP = Integer.parseInt(p.getProperty("GamePetExpRate"));
                MOUNT_EXP = Integer.parseInt(p.getProperty("GameTamingMobRate"));
                SERVER_MESSAGE = p.getProperty("GameServerMessage");
                
                HOST = p.getProperty("GameServerIP");
                
                REVISION = p.getProperty("GameRevision");
                
                MAPLE_VERSION = 62;

            } catch (IOException e) { 
                System.out.println("Failed loading World.ini - " + e);
            }
        }
    }
    
    public final static class Database { 
        
        public static String DB_URL = "";
        public static String DB_USER = "";
        public static String DB_PASS = "";
    
        static {
            Properties p = new Properties();
            try {
                p.load(new FileInputStream(Misc.DATA_ROOT + "/Database.ini"));

                DB_URL = p.getProperty("DatabaseUrl");
                DB_USER = p.getProperty("DatabaseUser");
                DB_PASS = p.getProperty("DatabasePass");

            } catch (IOException e) {
                System.out.println("Failed loading Database.ini - " + e);
            }
        }
    }
    
    public final static class Misc { 
        
        public static boolean CASHSHOP_AVAILABLE;
        public static boolean USE_JAVA8;
        public static boolean VPS;
        public static boolean HAMACHI;
        public static boolean RELEASE;
        public static boolean VOTE_MESSAGE;
        public static boolean WELCOME_MESSAGE;
        public static String WEB_SITE;
        public static String DATA_ROOT = "DataSrv";
        
        static {
            Properties p = new Properties();
            try {
                p.load(new FileInputStream(DATA_ROOT + "/Misc.ini"));

                CASHSHOP_AVAILABLE = p.getProperty("CashShopAvailable").equalsIgnoreCase("True");
                USE_JAVA8 = p.getProperty("UseJava8").equalsIgnoreCase("True");
                VPS = p.getProperty("UseVPS").equalsIgnoreCase("True");
                HAMACHI = p.getProperty("UseHamachi").equalsIgnoreCase("True");
                RELEASE = p.getProperty("UseRelease").equalsIgnoreCase("True");
                VOTE_MESSAGE = p.getProperty("ShowVoteMessage").equalsIgnoreCase("True");
                WELCOME_MESSAGE = p.getProperty("ShowWelcomeMessage").equalsIgnoreCase("True");
                
                WEB_SITE = p.getProperty("WebSiteLink");

            } catch (IOException e) {
                System.out.println("Failed loading Misc.ini - " + e);
            }
        }
    }  
}
