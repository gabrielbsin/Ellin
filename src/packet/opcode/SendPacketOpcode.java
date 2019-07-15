/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package packet.opcode;

public enum SendPacketOpcode {

	PING((short) 0x11), 
	// LOGIN
	LOGIN_STATUS((short) 0x00), 
        SERVERSTATUS((short) 0x03),
        GENDER_DONE((short) 0x04),
        CONFIRM_EULA_RESULT((short) 0x05),
	PIN_OPERATION((short) 0x06), 
	SERVERLIST((short) 0x0A),
	SERVER_IP((short) 0x0C),
	CHARLIST((short) 0x0B),
	CHAR_NAME_RESPONSE((short) 0x0D),
	RELOG_RESPONSE((short) 0x16),
	ADD_NEW_CHAR_ENTRY((short) 0x0E),
	DELETE_CHAR_RESPONSE((short) 0x0F),
	CHANNEL_SELECTED((short) 0x14),
	ALL_CHARLIST((short) 0x08),
        PIN_ASSIGNED((short) 0x07), 
	// CHANNEL
	GAME_HOST_ADDRESS((short) 0x10),
	UPDATE_STATS((short) 0x1C),
	FAME_RESPONSE((short) 0x23),
	UPDATE_SKILLS((short) 0x21),
	WARP_TO_MAP((short) 0x5C),
	SERVERMESSAGE((short) 0x41),
        SHOP_SCANNER_RESULT((short) 0x43),
        SHOP_LINK_RESULT((short) 0x44),
	AVATAR_MEGA((short) 0x54),
	SPAWN_NPC((short) 0xC2),
        REMOVE_NPC((short) 0xC3),
        SPAWN_NPC_REQUEST_CONTROLLER((short) 0xC4),
        NPC_ACTION((short) 0xC5),
        NPC_SUMMON_EFFECT((short) 0xC6),
        NPC_SPECIAL_ACTION((short) 0xC7),
        SET_NPC_SCRIPTABLE((short) 0xC8),
        END_NPCPOOL((short) 0xC9),
        NPC_TALK((short) 0xED),
        FINISH_SORT((short) 0x31),
        FINISH_SORT2((short) 0x32),
	SPAWN_MONSTER((short) 0xAF),
	SPAWN_MONSTER_CONTROL((short) 0xB1),
	MOVE_MONSTER_RESPONSE((short) 0xB3),
	MAP_CHAT((short) 0x7A),
	SHOW_STATUS_INFO((short) 0x24),
	SHOW_MESO_GAIN((short) 0x22),
	SHOW_QUEST_COMPLETION((short) 0x2E),
        SHOW_HIRED_MERCHANT_AGREEMENT((short) 0x2F),
        GMEVENT_INSTRUCTIONS((short) 0x6D),
        BLOCK_PORTAL ((short) 0x61),
        BLOCK_MIGRATE ((short) 0x62),
	WHISPER((short) 0x65),
        GM_POLICE((short) 0x59),
	SPAWN_PLAYER((short) 0x78),
	ANNOUNCE_PLAYER_SHOP((short) 0x67),
	SHOW_SCROLL_EFFECT((short) 0x7E),
	FIRST_PERSON_VISUAL_EFFECT((short) 0xA1),
	KILL_MONSTER((short) 0xB0),
	FACIAL_EXPRESSION((short) 0x95),
	MOVE_PLAYER((short) 0x8D),
	MOVE_MONSTER((short) 0xB2),
	CLOSE_RANGE_ATTACK((short) 0x8E),
	RANGED_ATTACK((short) 0x8F),
	MAGIC_ATTACK((short) 0x90),
	OPEN_NPC_SHOP((short) 0xEE),
        TIP_MESSAGE((short) 0x4A),
        UPDATE_INVENTORY_CAPACITY((short) 0x1B),
	CONFIRM_SHOP_TRANSACTION((short) 0xEF),
	NPC_STORAGE((short) 0xF0),
	MODIFY_INVENTORY_ITEM((short) 0x1A),
	REMOVE_PLAYER_FROM_MAP((short) 0x79),
        
        DROP_ITEM_FROM_MAPOBJECT((short) 0xCD),
	REMOVE_ITEM_FROM_MAP((short) 0xCE),
        
	UPDATE_CHAR_LOOK((short) 0x98),
	THIRD_PERSON_VISUAL_EFFECT((short) 0x99),
	THIRD_PERSON_APPLY_STATUS_EFFECT((short) 0x9A),
	THIRD_PERSON_CANCEL_STATUS_EFFECT((short) 0x9B),
	DAMAGE_PLAYER((short) 0x94),
	CHAR_INFO((short) 0x3A),
	UPDATE_QUEST_INFO((short) 0xA6),
	FIRST_PERSON_APPLY_STATUS_EFFECT((short) 0x1D),
	FIRST_PERSON_CANCEL_STATUS_EFFECT((short) 0x1E),
	PLAYER_INTERACTION((short) 0xF5),
	UPDATE_CHAR_BOX((short) 0x7c),
	KEYMAP((short) 0x107),
	SHOW_MONSTER_HP((short) 0xBD),
	PARTY_OPERATION((short) 0x3B),
	UPDATE_PARTYMEMBER_HP((short) 0x9C),
        UPDATE_GUILD_MEMBERSHIP((short) 0x9D),
	UPDATE_GUILD_EMBLEM((short) 0x9E),
        THROW_GRENADE((short) 0x9F),
	PRIVATE_CHAT((short) 0x64),
	APPLY_MONSTER_STATUS((short) 0xB5),
	CANCEL_MONSTER_STATUS((short) 0xB6),
	CLOCK((short) 0x6E),
        DESTROY_CLOCK((short) 0x75),
        GM((short) 0x6B),
	SPAWN_PORTAL((short) 0x40),
	SPAWN_DOOR((short) 0xD4),
	REMOVE_DOOR((short) 0xD5),
	SPAWN_SPECIAL_MAPOBJECT((short) 0x86),
	REMOVE_SPECIAL_MAPOBJECT((short) 0x87),
	SUMMON_ATTACK((short) 0x89),
	MOVE_SUMMON((short) 0x88),
	SPAWN_MIST((short) 0xD2),
	REMOVE_MIST((short) 0xD3),
	DAMAGE_SUMMON((short) 0x8A),
	DAMAGE_MONSTER((short) 0xB9),
	BUDDYLIST((short) 0x3C),
	SHOW_ITEM_EFFECT((short) 0x96),
	ITEM_CHAIR((short) 0x97),
	CHAIR((short) 0xA0),
	SKILL_EFFECT((short) 0x92),
	CANCEL_SKILL_EFFECT((short) 0x93),
	FIELD_EFFECT((short) 0x68),
	REACTOR_SPAWN((short) 0xD8),
	REACTOR_HIT((short) 0xD6),
	REACTOR_DESTROY((short) 0xD9),
	BLOW_WEATHER((short) 0x69),
	GUILD_OPERATION((short) 0x3E),
	BBS_OPERATION((short) 0x38),
	SHOW_MAGNET((short) 0xBE),
        FREDRICK_MESSAGE((short) 0xF1),
        FREDRICK((short) 0xF2),
        RPS_GAME((short) 0xF3),
	MESSENGER((short) 0xF4),
	SPAWN_PET((short) 0x7F),
	MOVE_PET((short) 0x81),
	PET_CHAT((short) 0x82),
        PET_NAMECHANGE((short) 0x83),
        PET_ITEM_IGNORE((short) 0x84),
        PET_RESPONSE((short)0x85),
	COOLDOWN((short) 0xAD),
	PLAYER_HINT((short) 0xA9),
	USE_SKILL_BOOK((short) 0x30),
	SHOW_EQUIP_EFFECT((short) 0x63),
	SKILL_MACRO((short) 0x5B),
	CS_OPEN((short) 0x5E),
	CS_UPDATE((short) 0xFF),
	CASH_SHOP((short) 0x100),
	PLAYER_NPC((short) 0x4E),
	SHOW_NOTES((short) 0x26),
        TROCK_LOCATIONS((short) 0x27),
	SUMMON_SKILL((short) 0x8B),
	ARIANT_PQ_START((short) 0xEA),
	CATCH_MONSTER((short) 0xBF),
	ARIANT_SCOREBOARD((short) 0x76),
	ZAKUM_SHRINE((short) 0xEC),
	SHIP_EFFECT((short) 0x6F),
        TRAIN_EFFECT((short) 0x70),
	CHANGE_BINDING((short) 0x7B),
        UPDATE_MOUNT((short) 0x2D),
	DUEY((short) 0xFD),
        
        MONSTER_CARNIVAL_START((short) 0xE2),
        MONSTER_CARNIVAL_OBTAINED_CP((short) 0xE3),
        MONSTER_CARNIVAL_PARTY_CP((short) 0xE4),
        MONSTER_CARNIVAL_SUMMON((short) 0xE5),
        MONSTER_CARNIVAL_MESSAGE((short) 0xE6),
        MONSTER_CARNIVAL_DIED((short) 0xE7),
        MONSTER_CARNIVAL_LEAVE((short) 0xE8),  
        
        SEND_TV((short) 0x10D),
        REMOVE_TV((short) 0x10E),
        ENABLE_TV((short) 0x10F),
        
        SPAWN_HIRED_MERCHANT((short) 0xCA),
        DESTROY_HIRED_MERCHANT((short) 0xCB),
        UPDATE_HIRED_MERCHANT((short) 0xCC),
        
        SPOUSE_CHAT((short) 0x66),
        REPORTREPLY((short) 0x34),
	MTS_OPEN((short) 0x5D),
	MTS_OPERATION((short) 0x114),
	MTS_OPERATION2((short) 0x113),
	ALLIANCE_OPERATION((short) 0x3F),
        SILVER_BOX_OPEN((short) 0x5A),
        GENDER((short) 0x37),
        PET_AUTO_HP_POT((short) 0x108),
	PET_AUTO_MP_POT((short) 0x109),
        TRADE_MONEY_LIMIT((short) 0x36),
        ENERGY_CHARGE_ATTACK((short) 0x91);
        
        
    private short code = -2;

    private SendPacketOpcode(short code) {
        this.code = code;
    }
    
    public static String getOpcodeName(int value) {
        for (SendPacketOpcode opcode : SendPacketOpcode.values()) {
            if (opcode.getValue() == value) {
                return opcode.name();
            }
        }
        return "UNKNOWN";
    }

    public short getValue() {
        return code;
    }
}
