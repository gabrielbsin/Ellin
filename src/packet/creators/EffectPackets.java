/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packet.creators;

import client.player.Player;
import client.player.PlayerEffects;
import packet.opcode.SendPacketOpcode;
import packet.transfer.write.OutPacket;
import packet.transfer.write.WritingPacket;
import tools.HexTool;

public class EffectPackets {
    
    public static OutPacket PlayPortalSound() {
        return ShowSpecialEffect(PlayerEffects.PLAY_PORTAL_SE.getEffect());
    }

    public static OutPacket ShowMonsterBookPickup() {
        return ShowSpecialEffect(PlayerEffects.MONSTERBOOK_CARD_GET.getEffect());
    }

    public static OutPacket ShowEquipmentLevelUp() {
        return ShowSpecialEffect(PlayerEffects.ITEM_LEVEL_UP.getEffect());
    }

    public static OutPacket ShowItemLevelup() {
        return ShowSpecialEffect(PlayerEffects.ITEM_LEVEL_UP.getEffect());
    }
    
    public static OutPacket ShowSelfQuestComplete() {
        return ShowSpecialEffect(PlayerEffects.QUEST_COMPLETE.getEffect());
    }
    
    public static OutPacket MusicChange(String song) {
	return EnvironmentChange(song, 6);
    }
    public static OutPacket ShowEffect(String effect) {
        return EnvironmentChange(effect, 3);
    }

    public static OutPacket PlaySound(String sound) {
        return EnvironmentChange(sound, 4);
    }
    
    public static OutPacket EnvironmentChange(String env, int mode) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIELD_EFFECT.getValue());
        wp.write(mode);
        wp.writeMapleAsciiString(env);
        return wp.getPacket();
    }
    
    public static OutPacket StartMapEffect(String msg, int itemid, boolean active) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.BLOW_WEATHER.getValue());
        wp.writeBool(active);
        wp.writeInt(itemid);
        if (active) {
            wp.writeMapleAsciiString(msg);
        }
        return wp.getPacket();
    }

    public static OutPacket RemoveMapEffect() {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.BLOW_WEATHER.getValue());
        wp.write(0);
        wp.writeInt(0);
        return wp.getPacket();
    }
    
    public static OutPacket ShowEquipEffect() {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOW_EQUIP_EFFECT.getValue());
        return wp.getPacket();
    }
    
    public static OutPacket ShowOwnBuffEffect(int skillID, int effectID) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_VISUAL_EFFECT.getValue());
        wp.write(effectID);
        wp.writeInt(skillID);
        wp.writeBool(true);
        return wp.getPacket();
    }
    
    public static OutPacket BuffMapVisualEffect(int cID, int skillID, int effectID) {
        return BuffMapVisualEffect(cID, skillID, effectID, (byte) 3);
    }

    public static OutPacket BuffMapVisualEffect(int cID, int skillID, int effectID, byte direction) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_VISUAL_EFFECT.getValue());
        wp.writeInt(cID); 
        wp.write(effectID);
        wp.writeInt(skillID);
        wp.writeBool(true);
        if (direction != (byte) -3) {
            wp.write(direction);
        }
        return wp.getPacket();
    }
    
    public static OutPacket ExpressionChange(Player from, int expression) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FACIAL_EXPRESSION.getValue());
        wp.writeInt(from.getId());
        wp.writeInt(expression);
        return wp.getPacket();
    }
    
    public static OutPacket ItemEffect(int charID ,int itemID ){
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOW_ITEM_EFFECT.getValue());
        wp.writeInt(charID);
        wp.writeInt(itemID);
        return wp.getPacket();
    }
    
    public static OutPacket ShowChair(int charID, int itemID) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ITEM_CHAIR.getValue());
        wp.writeInt(charID);
        wp.writeInt(itemID);
        return wp.getPacket();
    }

    public static OutPacket SitOnChair(short chairId) {
        WritingPacket wp = new WritingPacket(5);
        wp.writeShort(SendPacketOpcode.CHAIR.getValue());
        wp.writeBool(true);
        wp.writeShort(chairId);
        return wp.getPacket();
    }
    
    public static OutPacket RiseFromChair() {
        WritingPacket wp = new WritingPacket(3);
        wp.writeShort(SendPacketOpcode.CHAIR.getValue());
        wp.writeBool(false);
        return wp.getPacket();
    }
    
    public static OutPacket SelfCharmEffect(short charmsLeft, short daysLeft) {
        WritingPacket wp = new WritingPacket(8);
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_VISUAL_EFFECT.getValue());
        wp.write(PlayerEffects.SAFETY_CHARM.getEffect());
        wp.writeBool(true);
        wp.write(charmsLeft);
        wp.write(daysLeft);
        return wp.getPacket();
    }
    
    public static OutPacket SkillEffect(Player from, int skillID, int level, byte flags, int speed) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SKILL_EFFECT.getValue());
        wp.writeInt(from.getId());
        wp.writeInt(skillID);
        wp.write(level);
        wp.write(flags);
        wp.write(speed);
        return wp.getPacket();
    }
    
    /**
     * 6 = Exp did not drop (Safety Charms)
     * 7 = Enter portal sound
     * 8 = Job change
     * 9 = Quest complete
     * 10 = damage O.O
     * 14 = Monster book pickup
     * 15 = Equipment levelup
     * 16 = Maker Skill Success
     * 19 = Exp card [500, 200, 50]
     * @param effect
     * @return 
     */
    public static OutPacket ShowSpecialEffect(int effect) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_VISUAL_EFFECT.getValue());
        wp.write(effect);
        return wp.getPacket();
    }
    
    public static OutPacket ShowThirdPersonEffect(int cID, int effect) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_VISUAL_EFFECT.getValue());
        wp.writeInt(cID);
        wp.write(effect);
        return wp.getPacket();
    }
    
    public static OutPacket ShowDashEffecttoOthers(int Cid, int x, int y, int duration) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_APPLY_STATUS_EFFECT.getValue());
        wp.writeInt(Cid);
        wp.writeLong(0);
        wp.write(HexTool.getByteArrayFromHexString("00 00 00 30 00 00 00 00"));
        wp.writeShort(0);
        wp.writeInt(x);
        wp.writeInt(5001005);
        wp.write(HexTool.getByteArrayFromHexString("1A 7C 8D 35"));
        wp.writeShort(duration);
        wp.writeInt(y);
        wp.writeInt(5001005);
        wp.write(HexTool.getByteArrayFromHexString("1A 7C 8D 35"));
        wp.writeShort(duration);
        wp.writeShort(0);
        return wp.getPacket();
    }	
}
