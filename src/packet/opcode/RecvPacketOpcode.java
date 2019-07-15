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

public enum RecvPacketOpcode {
    PONG((short) 0x18),
    // LOGIN
    AFTER_LOGIN((short) 0x09),
    SERVERLIST_REQUEST((short) 0x0B),
    SERVERLIST_REREQUEST((short) 0x04),
    ACCEPT_TOS((short) 0x07),
    CHARLIST_REQUEST((short) 0x05),
    CHAR_SELECT((short) 0x13),
    SET_GENDER((short) 0x08),
    CHECK_CHAR_NAME((short) 0x15),
    CREATE_CHAR((short) 0x16),
    DELETE_CHAR((short) 0x17),
    LOGIN_PASSWORD((short) 0x01),
    RELOG((short) 0x1C),
    SERVERSTATUS_REQUEST((short) 0x06),
    VIEW_ALL_CHAR((short) 0x0D),
    PICK_ALL_CHAR((short) 0x0E),
    CLIENT_ERROR((short) 0x19),
    // CHANNEL
    CHANGE_CHANNEL((short) 0x24),
    CHAR_INFO_REQUEST((short) 0x59),
    MELEE_ATTACK((short) 0x29),
    RANGED_ATTACK((short) 0x2A),
    MAGIC_ATTACK((short) 0x2B),
    FACE_EXPRESSION((short) 0x30),
    HEAL_OVER_TIME((short) 0x51),
    ITEM_GATHER((short) 0x40),
    ITEM_SORT((short) 0x41),
    ITEM_MOVE((short) 0x42),
    ITEM_PICKUP((short) 0xAB),
    CHANGE_MAP((short) 0x23),
    MESO_DROP((short) 0x56),
    MOVE_LIFE((short) 0x9D),
    MOVE_PLAYER((short) 0x26),
    NPC_SHOP((short) 0x39),
    NPC_TALK((short) 0x36),
    NPC_TALK_MORE((short) 0x38),
    PLAYER_LOGGEDIN((short) 0x14),
    QUEST_ACTION((short) 0x62),
    TAKE_DAMAGE((short) 0x2D),
    USE_CASH_ITEM((short) 0x49),
    USE_SCRIPTED_ITEM((short) 0x4D),
    USE_ITEM((short) 0x43),
    USE_RETURN_SCROLL((short) 0x4E),
    USE_UPGRADE_SCROLL((short) 0x4F),
    USE_SUMMON_BAG((short) 0x46),
    GENERAL_CHAT((short) 0x2E),
    WHISPER((short) 0x6C),
    SPECIAL_MOVE((short) 0x53),
    USE_INNER_PORTAL((short) 0x5D),
    TROCK_ADD_MAP((short) 0x5E),
    CANCEL_BUFF((short) 0x54),
    PLAYER_INTERACTION((short) 0x6F),
    CANCEL_ITEM_EFFECT((short) 0x44),
    DISTRIBUTE_AP((short) 0x50),
    DISTRIBUTE_SP((short) 0x52),
    CHANGE_KEYMAP((short) 0x7B),
    CHANGE_MAP_SPECIAL((short) 0x5C),
    STORAGE((short) 0x3A),
    STRANGE_DATA((short) 0x1A),
    GIVE_FAME((short) 0x57),
    PARTY_OPERATION((short) 0x70),
    DENY_PARTY_REQUEST((short) 0x71),
    PARTYCHAT((short) 0x6B),
    USE_DOOR((short) 0x79),
    ENTER_MTS((short) 0x87),
    ENTER_CASH_SHOP((short) 0x25),
    DAMAGE_SUMMON((short) 0x96),
    MOVE_SUMMON((short) 0x94),
    SUMMON_ATTACK((short) 0x95),
    BUDDYLIST_MODIFY((short) 0x76),
    ENTERED_SHIP_MAP((short) 0xBB),
    USE_ITEMEFFECT((short) 0x31),
    CHAIR((short) 0x27),
    USE_CHAIR_ITEM((short) 0x28),
    SKILL_EFFECT((short) 0x55),
    DAMAGE_REACTOR((short) 0xAE),
    GUILD_OPERATION((short) 0x72),
    DENY_GUILD_REQUEST((short) 0x73),
    BBS_OPERATION((short) 0x86),
    MESSENGER((short) 0x6E),
    NPC_ACTION((short) 0xA6),
    TOUCHING_CS((short) 0xC5),
    BUY_CS_ITEM((short) 0xC6),
    COUPON_CODE((short) 0xC7),
    SPAWN_PET((short) 0x5A),
    MOVE_PET((short) 0x8C),
    PET_CHAT((short) 0x8D),
    PET_COMMAND((short) 0x8E),
    PET_FOOD((short) 0x47),
    PET_LOOT((short) 0x8F),
    REGISTER_PIN((short) 0x0A),
    AUTO_AGGRO((short) 0x9E),
    MOB_DAMAGE_MOB((short) 0xA1),
    MONSTER_BOMB((short) 0xA2),
    CANCEL_DEBUFF((short) 0x5B),
    USE_SKILL_BOOK((short) 0x4B),
    SKILL_MACRO((short) 0x65),
    NOTE_ACTION((short) 0x77),
    MAPLETV((short) 0xD2),
    ENABLE_ACTION((short) 0xC0),
    USE_CATCH_ITEM((short) 0x4A),
    USE_MOUNT_FOOD((short) 0x48),
    CLOSE_CHALKBOARD((short) 0x2F),
    DUEY_ACTION((short) 0x3D),
    MONSTER_CARNIVAL((short) 0xB9),
    RPS_ACTION((short) 0x7C),
    RING_ACTION((short) 0x7D),
    PASSIVE_ENERGY((short) 0x2C),
    SPOUSE_CHAT((short) 0x6D),
    REPORT_PLAYER((short) 0x68),
    GRENADE((short) 0x64),
    UNSTUCK((short) 0x63),
    MTS_OP((short) 0xD9),
    UNKNOWN((short) 0x81),
    ALLIANCE_OPERATION((short) 0x83),
    PET_AUTO_POT((short) 0x90),
    PET_ITEM_IGNORE ((short) 0x91),
    SILVER_BOX((short) 0x69), 
    HIRED_MERCHANT_REQUEST((short) 0x3B),
    FREDRICK_REQUEST((short) 0x3C),
    OWL_ACTION((short) 0x3E),
    OWL_WARP((short) 0x3F),
    LOGGED_OUT((short) 0xC0);
 
    private short code = -2;

    private RecvPacketOpcode(short code) {
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
