/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package handling.channel.handler;

/**
 * 
 * @author GabrielSin
 */
public class ChannelHeaders {
    
    public static class BBSHeaders {
        public final static byte     
            START_TOPIC = 0,
            DELETE_TOPIC = 1,
            LIST_TOPIC = 2,
            LIST_TOPIC_REPLY = 3,
            TOPIC_REPLY = 4,
            DELETE_REPLY = 5;
    }
    
    public static class BuddyListHeaders {
        
        public final static byte 
            BUDDY_INVITE_MODIFY = 1,
            BUDDY_ACCEPT = 2,
            BUDDY_DELETE_DENY = 3;
                                
        public final static byte  
            FIRST = 0x07,
            INVITE_RECEIVED = 0x09,
            ADD = 0x0A,
            YOUR_LIST_FULL = 0x0B,
            THEIR_LIST_FULL = 0x0C,
            ALREADY_ON_LIST = 0x0D,
            NO_GM_INVITES = 0x0E,
            NONEXISTENT = 0x0F,
            DENY_ERROR = 0x10,
            REMOVE = 0x12,
            BUDDY_LOGGED_IN = 0x14,
            CAPACITY_CHANGE = 0x15,
            ALREADY_FRIEND_REQUEST = 0x17;      
    }
    
    public static class ChatHeaders {
        
        public static final byte 
            COMMAND_FIND = 5,
            COMMAND_WHISPER = 6;
    
        public static final byte
            FIND_RESPONSE_MAP = 1,
            FIND_RESPONSE_CASH_SHOP = 2,
            FIND_RESPONSE_CHANNEL = 3;

        public static final byte
            PRIVATE_CHAT_TYPE_BUDDY = 0,
            PRIVATE_CHAT_TYPE_PARTY = 1,
            PRIVATE_CHAT_TYPE_GUILD = 2,
            PRIVATE_CHAT_TYPE_ALLIANCE = 3;
    
        public static final byte
            MESSENGER_OPEN = 0,
            MESSENGER_JOIN = 1,
            MESSENGER_EXIT = 2,
            MESSENGER_INVITE = 3,
            MESSENGER_INVITE_RESPONSE = 4,
            MESSENGER_DECLINE = 5,
            MESSENGER_CHAT = 6,
            MESSENGER_REFRESH_AVATAR = 7;
    }
    
    public static class GuildHeaders {
        
        public static final byte 
            GUILD_INFO = 0x00,
            GUILD_CREATE = 0x02,
            GUILD_INVITE = 0x05,
            GUILD_JOIN = 0x06,
            GUILD_LEAVE = 0x07,
            GUILD_EXPEL = 0x08,
            GUILD_CHANGE_RANK_STRING = 0x0D,
            GUILD_CHANGE_PLAYER_RANK = 0x0E,
            GUILD_CHANGE_EMBLEM = 0x0F,
            GUILD_CHANGE_NOTICE = 0x10,
            GUILD_CONTRACT_RESPONSE = 0x1E
	;
        
        public static final byte 
            ASK_NAME = 0x01,
            GENERAL_ERROR = 0x02,
            GUILD_CONTRACT = 0x03, 
            INVITE_SENT = 0x05,
            ASK_EMBLEM = 0x11,
            LIST = 0x1A,
            NAME_TAKEN = 0x1C,
            LEVEL_TOO_LOW = 0x23,
            JOINED_GUILD = 0x27,
            ALREADY_IN_GUILD = 0x28,
            CANNOT_FIND = 0x2A,
            LEFT_GUILD = 0x2C,
            EXPELLED_FROM_GUILD = 0x2F,
            DISBANDED_GUILD = 0x32, 
            INVITE_DENIED = 0x37, 
            CAPACITY_CHANGED = 0x3A,
            LEVEL_JOB_CHANGED = 0x3C,
            CHANNEL_CHANGE = 0x3D,
            RANK_TITLES_CHANGED = 0x3E,
            RANK_CHANGED = 0x40,
            EMBLEM_CHANGED = 0x42, 
            NOTICE_CHANGED = 0x44,
            GUILD_GP_CHANGED = 0x48,
            SHOW_GUILD_RANK_BOARD = 0x49;
    }
    
    public static class AllianceHeaders {
        
        public static final byte 
            ALLIANCE_UNKNOWN = 1,
            ALLIANCE_INVITE = 3,
            ALLIANCE_ACCEPT_INVITE = 4,
            ALLIANCE_LEAVE = 2,
            ALLIANCE_EXPEL = 6,
            ALLIANCE_CHANGE_LEADER = 7,
            ALLIANCE_TITLE_UPDATE = 8,
            ALLIANCE_CHANGE_RANK = 9,
            ALLIANCE_NOTICE_UPDATE = 10
            ;
    }
    
    public static class HiredMerchantHeaders {
        
        public final static byte 
                
            UNKNOW = 0x19,
            RETRIEVE_ITENS = 0x1A,
            CLOSE_FREDRICK = 0x1C;
    }  
    
    public static class MovementHeaders {
        
        public static final byte
                
            NORMAL_MOVE = 0,
            JUMP_M = 1,
            JUMP_AND_KNOCKBACK = 2,
            UNK_SKILL = 3,
            TELEPORT = 4, 
            NORMAL_MOVE_2 = 5,
            FLASH_JUMP = 6,
            ASSAULTER = 7,
            ASSASSINATE = 8,
            RUSH_M = 9,
            EQUIP_M = 10,
            CHAIR = 11,
            HORNTAIL_KNOCKBACK = 12,
            RECOIL_SHOT = 13, 
            UNK = 14,
            JUMP_DOWN = 15,
            WINGS = 16,
            WINGS_FALL = 17;
    }
    
    public static class NPCHeaders {
        
        public static final byte 
                
            TAKE_OUT_STORAGE = 4,
            SEND_STORAGE = 5,
            ARRANGE_STORAGE = 6,
            SET_MESO_STORAGE = 7,
            CLOSE_STORAGE = 8;
        
        public static final byte
                
            START_QUEST = 1,
            COMPLETE_QUEST = 2,
            FORFEIT_QUEST = 3,
            SCRIPT_START_QUEST = 4,
            SCRIPT_END_QUEST = 5;  
        
        public static final byte
                
            BUY_SHOP = 0,
            SELL_SHOP = 1,
            RECHARGE_SHOP = 2,
            EXIT_SHOP = 3;
    } 
    
    public static class PartyHeaders {
        
        public static final byte 
                
            PARTY_CREATE = 0x01,
            PARTY_LEAVE = 0x02,
            PARTY_ACCEPT_INVITE = 0x03,
            PARTY_INVITE = 0x04,
            PARTY_EXPEL = 0x05,
            PARTY_CHANGE_LEADER = 0x06;
    
        public static final byte
                
            PARTY_INVITE_SENT = 0x04,
            PARTY_SILENT_LIST_UPDATE = 0x07,
            PARTY_CREATED = 0x08,
            PARTY_IS_BEGINNER = 0x0A,
            LEFT_PARTY = 0x0C,
            NOT_IN_PARTY = 0x0D,
            JOINED_PARTY = 0x0F,
            ALREADY_IN_PARTY = 0x10,
            PARTY_FULL = 0x11,
            PARTY_CANNOT_FIND = 0x13,
            PARTY_BUSY = 0x16,
            PARTY_INVITE_DENIED = 0x17,
            PARTY_LEADER_CHANGED = 0x1A,
            PARTY_NOT_IN_VICINITY = 0x1B, 
            PARTY_NO_MEMBERS_IN_VICINITY = 0x1C,
            PARTY_NOT_IN_CHANNEL = 0x1D, 
            PARTY_IS_GM = 0x1F;
    }
    
    public static class PlayerHeaders {
        
        public static final int
                
            BINDING_CHANGE_KEY_MAPPING = 0,
            BINDING_CHANGE_AUTO_HP_POT = 1,
            BINDING_CHANGE_AUTO_MP_POT = 2;
        
        public static final byte
            BUMP_DAMAGE = -1, 
            MAP_DAMAGE = -2;
    }
    
    public static class PlayersHeaders {
            
        public static final byte
                
            FAME_OPERATION_RESPONSE_SUCCESS = 0,
            FAME_OPERATION_RESPONSE_NOT_IN_MAP = 1,
            FAME_OPERATION_RESPONSE_UNDER_LEVEL = 2,
            FAME_OPEARTION_RESPONSE_NOT_TODAY = 3,
            FAME_OPERATION_RESPONSE_NOT_THIS_MONTH = 4,
            FAME_OPERATION_FAME_CHANGED = 5;
        
        public static final byte
                
            SEND_RING = 0,
            CANCEL_SEND_RING = 1,
            DROP_RING = 3;  
        
        public static final byte
            NOTE_RECEIVE = 0,
            NOTE_DELETE = 1;
    }
    
    public static class StatsHeaders {
        
        public static final int
            STATS_STR = 64,
            STATS_DEX = 128,
            STATS_INT = 256,
            STATS_LUK = 512,
            STATS_MP = 2048,
            STATS_HP = 8192;
    }
    
    public static class InventoryHeaders {
         public static final byte
            MEGAPHONE = 1,
            SUPERMEGAPHONE = 2,
            HEARTMEGAPHONE = 3, 
            SKULLMEGAPHONE = 4, 
            MAPLETV = 5,
            ITEMMEGAPHONE = 6,
            ITEM_TAG = 0,
            ITEM_SEALING_LOCK = 1,
            INCUBATOR = 2;     
        
    }
    
    public static class PlayerInteractionHeaders {
        
        public static final byte
            ACT_CREATE = 0x00, 
            ACT_INVITE = 0x02, 
            ACT_DECLINE = 0x03, 
            ACT_VISIT = 0x04, 
            ACT_JOIN = 0x05, 
            ACT_CHAT = 0x06, 
            ACT_CHAT_THING = 0x08, 
            ACT_EXIT = 0x0A, 
            ACT_OPEN = 0x0B, 
            ACT_SET_ITEMS = 0x0E, 
            ACT_SET_MESO = 0x0F, 
            ACT_CONFIRM = 0x10,
            ACT_ADD_ITEM = 0x14, 
            ACT_BUY = 0x15, 
            ACT_CANNOT_BUY = 0x16, 
            ACT_MERCHANT_ITEM_UPDATE = 0x17, 
            ACT_SHOP_ITEM_UPDATE = 0x18, 
            ACT_REMOVE_ITEM = 0x19,
            ACT_BAN_PLAYER = 0x1A, 
            ACT_SPAWN_SHOP = 0x1C, 
            ACT_PUT_ITEM = 0x1F, 
            ACT_MERCHANT_BUY = 0x20,
            ACT_TAKE_ITEM_BACK = 0x24, 
            ACT_MAINTENANCE_OFF = 0x25, 
            ACT_MERCHANT_ORGANIZE = 0x26,    
            ACT_CLOSE_MERCHANT = 0x27, 
            ACT_REAL_CLOSE_MERCHANT = 0x28, 
            ACT_REQUEST_TIE = 0x2C, 
            ACT_ANSWER_TIE = 0x2D, 
            ACT_GIVE_UP = 0x2E, 
            ACT_REQUEST_REDO = 0x30, 
            ACT_ANSWER_REDO = 0x31,
            ACT_EXIT_AFTER_GAME = 0x32, 
            ACT_CANCEL_EXIT = 0x33, 
            ACT_READY = 0x34, 
            ACT_UN_READY = 0x35, 
            ACT_EXPEL = 0x36, 
            ACT_START = 0x37, 
            ACT_FINISH_GAME = 0x38, 
            ACT_SKIP = 0x39,
            ACT_MOVE_OMOK = 0x3A, 
            ACT_CANNOT_MOVE = 0x3B,
            ACT_SELECT_CARD = 0x3E;
        
        public static final byte 
                
            OBJ_HIRED_MERCHANT = 1,
            OBJ_PLAYER_SHOP = 2,
            OBJ_MATCH_CARD = 3,
            OBJ_OMOK = 4;
        
        public static final byte 
                
            MODE_NONE = 0,
            MODE_OMOK = 1,
            MODE_MATCH_CARDS = 2,
            MODE_TRADE = 3,
            MODE_PLAYER_SHOP = 4,
            MODE_HIRED_MERCHANT = 5;
        
        public static final byte 
                
            START_RPS = 0,
            ANSWER_RPS = 1,
            TIME_OVER_RPS = 2,
            CONTINUE_RPS = 3,
            LEAVE_RPS = 4,
            RETRY_RPS = 5;
                
    }
}
