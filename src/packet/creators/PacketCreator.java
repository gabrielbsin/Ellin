/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packet.creators;

import client.Client;
import client.player.PlayerKeyBinding;
import client.player.Player;
import client.player.PlayerStat;
import client.player.buffs.BuffStat;
import client.player.buffs.Disease;
import client.player.inventory.Equip;
import client.player.inventory.EquipScrollResult;
import static client.player.inventory.EquipScrollResult.FAIL;
import static client.player.inventory.EquipScrollResult.SUCCESS;
import client.player.inventory.Inventory;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemPet;
import client.player.inventory.ItemRing;
import client.player.inventory.types.ItemRingType;
import client.player.inventory.types.ItemType;
import client.player.inventory.TamingMob;
import client.player.skills.PlayerSkill;
import client.player.skills.PlayerSkillEntry;
import client.player.skills.PlayerSkillMacro;
import community.MapleGuildAlliance;
import community.MapleBuddyListEntry;
import community.MapleGuild;
import constants.GameConstants;
import constants.MapConstants;
import constants.SkillConstants.ChiefBandit;
import database.DatabaseConnection;
import handling.channel.handler.ChannelHeaders.*;
import handling.channel.handler.SummonHandler;
import handling.world.PlayerCoolDownValueHolder;
import handling.world.service.AllianceService;
import handling.world.service.GuildService;
import java.awt.Point;
import java.awt.Rectangle;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import packet.opcode.SendPacketOpcode;
import packet.transfer.write.OutPacket;
import packet.transfer.write.WritingPacket;
import server.itens.DueyPackages;
import server.itens.ItemInformationProvider;
import server.shops.ShopItem;
import server.life.npc.MapleNPC;
import server.life.MobSkill;
import server.life.status.MonsterStatus;
import server.maps.Field;
import server.maps.FieldItem;
import server.maps.MapleMist;
import server.maps.reactors.Reactor;
import server.maps.MapleSummon;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuestStatus;
import tools.BitTools;
import tools.HexTool;
import tools.KoreanDateUtil;
import tools.Pair;
import tools.Randomizer;
import tools.StringUtil;

/**
 *
 * @author GabrielSin
 */
public class PacketCreator {
    
    public static final List<Pair<PlayerStat, Integer>> EMPTY_STATUPDATE = Collections.emptyList();
    public final static byte[] CHAR_INFO_MAGIC = new byte[] { (byte) 0xff, (byte) 0xc9, (byte) 0x9a, 0x3b };
    public final static byte[] ITEM_MAGIC = new byte[] { (byte) 0x80, 5 };
    public final static long FT_UT_OFFSET = 116444592000000000L;
    
    public static long GetKoreanTimestamp(long realTimestamp) {
        long time = (realTimestamp / 1000 / 60); 
        return ((time * 600000000) + FT_UT_OFFSET);
    }

    public static long GetTime(long realTimestamp) {
        long time = (realTimestamp / 1000); 
        return ((time * 10000000) + FT_UT_OFFSET);
    }
     
    private static void AddExpirationTime(WritingPacket wp, long time) {
        AddExpirationTime(wp, time, true);
    }

    private static void AddExpirationTime(WritingPacket wp, long time, boolean addZero) {
        if (addZero)
            wp.write(0);
            wp.writeShort(1408); 
        if (time > 0) {
            wp.writeInt(KoreanDateUtil.getItemTimestamp(time));
            wp.write(1);
        } else {
            wp.writeInt(400967355);
            wp.write(2);
        }
    }
    
    public static OutPacket EnableActions() {
        return UpdatePlayerStats(EMPTY_STATUPDATE, true);
    }
    
    public static OutPacket UpdatePlayerStats(List<Pair<PlayerStat, Integer>> stats) {
            return UpdatePlayerStats(stats, false);
    }
    
    public static OutPacket UpdatePlayerStats(List<Pair<PlayerStat, Integer>> stats, boolean itemReaction) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_STATS.getValue());
        if (itemReaction) {
            wp.writeBool(true);
        } else {
            wp.writeBool(false);
        }
        int updateMask = 0;
        updateMask = stats.stream().map((statupdate) -> statupdate.getLeft().getValue()).reduce(updateMask, (accumulator, _item) -> accumulator | _item);
        List<Pair<PlayerStat, Integer>> mystats = stats;
        if (mystats.size() > 1) {
            Collections.sort(mystats, (Pair<PlayerStat, Integer> o1, Pair<PlayerStat, Integer> o2) -> {
                int val1 = o1.getLeft().getValue();
                int val2 = o2.getLeft().getValue();
                return (val1 < val2 ? -1 : (val1 == val2 ? 0 : 1));
            });
        }
        wp.writeInt(updateMask);
        mystats.stream().filter((statupdate) -> (statupdate.getLeft().getValue() >= 1)).forEachOrdered((statupdate) -> {
            if (statupdate.getLeft().getValue() == 0x1) {
                wp.writeShort(statupdate.getRight().shortValue());
            } else if (statupdate.getLeft().getValue() <= 0x4) {
                wp.writeInt(statupdate.getRight());
            } else if (statupdate.getLeft().getValue() < 0x20) {
                wp.write(statupdate.getRight().shortValue());
            } else if (statupdate.getLeft().getValue() < 0xFFFF) {
                wp.writeShort(statupdate.getRight().shortValue());
            } else {
                wp.writeInt(statupdate.getRight().intValue());
            }
        });
        return wp.getPacket();
    }
    
    /**
    * Gets a server message packet.
    * 
    * @param message The message to convey.
    * @return The server message packet.
    */
    public static OutPacket ServerMessage(String message) {
        return ServerMessage(4, 0, message, true, false);
    }

    /**
     * Gets a server notice packet.
     * 
     * Possible values for <code>type</code>:<br>
     * 0: [Notice]<br>
     * 1: Popup<br>
     * 2: Megaphone<br>
     * 3: Super Megaphone<br>
     * 4: Scrolling message at top<br>
     * 5: Pink Text<br>
     * 6: Lightblue Text
     * 
     * @param type The type of the notice.
     * @param message The message to convey.
     * @return The server notice packet.
     */
    public static OutPacket ServerNotice(int type, String message) {
        return ServerMessage(type, 0, message, false, false);
    }

    /**
     * Gets a server notice packet.
     * 
     * Possible values for <code>type</code>:<br>
     * 0: [Notice]<br>
     * 1: Popup<br>
     * 2: Megaphone<br>
     * 3: Super Megaphone<br>
     * 4: Scrolling message at top<br>
     * 5: Pink Text<br>
     * 6: Lightblue Text
     * 
     * @param type The type of the notice.
     * @param channel The channel this notice was sent on.
     * @param message The message to convey.
     * @return The server notice packet.
     */
    public static OutPacket ServerNotice(int type, int channel, String message) {
        return ServerMessage(type, channel, message, false, false);
    }

    public static OutPacket ServerNotice(int type, int channel, String message, boolean smegaEar) {
        return ServerMessage(type, channel, message, false, smegaEar);
    }

    /**
    * Gets a server message packet.
    * 
    * Possible values for <code>type</code>:<br>
    * 0: [Notice]<br>
    * 1: Popup<br>
    * 2: Megaphone<br>
    * 3: Super Megaphone<br>
    * 4: Scrolling message at top<br>
    * 5: Pink Text<br>
    * 6: Lightblue Text
    * 
    * @param type The type of the notice.
    * @param channel The channel this notice was sent on.
    * @param message The message to convey.
    * @param servermessage Is this a scrolling ticker?
    * @return The server notice packet.
    */
    private static OutPacket ServerMessage(int type, int channel, String message, boolean servermessage, boolean megaEar) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        wp.write(type);
        if (servermessage) {
            wp.writeBool(true);
        }
        wp.writeMapleAsciiString(message);
        if (type == 3) {
            wp.write(channel - 1); 
            wp.write(megaEar ? 1 : 0);
        }
        return wp.getPacket();
    }

    /**
    * Gets an avatar megaphone packet.
    * @param chr The character using the avatar megaphone.
    * @param channel The channel the character is on.
    * @param itemId The ID of the avatar-mega.
    * @param message The message that is sent.
    * @param ear 
    * @return The avatar mega packet.
    */
    public static OutPacket GetAvatarMega(Player p, int channel, int itemID, List<String> message, boolean ear) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.AVATAR_MEGA.getValue());
        wp.writeInt(itemID);
        wp.writeMapleAsciiString(p.getName());
        for (String s : message) {
            wp.writeMapleAsciiString(s);
        }
        wp.writeInt(channel - 1);
        wp.write(ear ? 1 : 0);
        AddCharLook(wp, p, true);
        return wp.getPacket();
    }

    public static OutPacket SendHint(String message, short width, short height) {
        if (width < 1) {
            width = (short) Math.max(message.length() * 10, 40);
        }
        if (height < 5) {
            height = 5;
        }
        WritingPacket wp = new WritingPacket(9 + message.length());
        wp.writeShort(SendPacketOpcode.PLAYER_HINT.getValue());
        wp.writeMapleAsciiString(message);
        wp.writeShort(width);
        wp.writeShort(height);
        wp.writeBool(true);
        return wp.getPacket();
    }
    
    public static OutPacket SendYellowTip(String tip) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.TIP_MESSAGE.getValue());
        wp.write(0xFF);
        wp.writeMapleAsciiString(tip);
        wp.writeShort(0);
        return wp.getPacket();
    }
    
    public static OutPacket CompleteQuest(short quest, long time) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        wp.write(PacketHeaders.STATUS_INFO_QUEST);
        wp.writeShort(quest);
        wp.write(MapleQuestStatus.Status.COMPLETED.getId());
        wp.writeLong(time);
        return wp.getPacket();
    }
    
    public static OutPacket UpdateQuestInfo(short quest, int npc, byte progress) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        wp.write(progress);
        wp.writeShort(quest);
        wp.writeInt(npc);
        wp.writeInt(0);
        return wp.getPacket();
    }
    
    public static OutPacket GetShowQuestCompletion(int id) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOW_QUEST_COMPLETION.getValue());
        wp.writeShort(id);
        return wp.getPacket();
    }
    
    public static OutPacket RemoveQuestTimeLimit(final short quest) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        wp.write(7);
        wp.writeShort(1);
        wp.writeShort(quest);
        return wp.getPacket();
    }
    
    public static OutPacket AddQuestTimeLimit(final short quest, final int time) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        wp.write(6);
        wp.writeShort(1);
        wp.writeShort(quest);
        wp.writeInt(time);
        return wp.getPacket();
    }
    
    public static OutPacket UpdateQuestFinish(short quest, int npc, short nextQuest) { 
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        wp.write(8);
        wp.writeShort(quest);
        wp.writeInt(npc);
        wp.writeShort(nextQuest);
        return wp.getPacket();
    }
    
    public static OutPacket QuestError(short quest) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        wp.write(0x0A);
        wp.writeShort(quest);
        return wp.getPacket();
    }

   public static OutPacket QuestFailure(byte type) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        wp.write(type);
        return wp.getPacket();
    }

    public static OutPacket QuestExpire(short quest) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        wp.write(0x0F);
        wp.writeShort(quest);
        return wp.getPacket();
    }
    
    public static OutPacket UpdateQuest(MapleQuestStatus quest, boolean infoUpdate) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        wp.write(PacketHeaders.STATUS_INFO_QUEST);
        wp.writeShort(infoUpdate ? quest.getQuest().getInfoNumber() : quest.getQuest().getId());
        if (infoUpdate) {
            wp.write(MapleQuestStatus.Status.STARTED.getId());
        } else {
            wp.write(quest.getStatus().getId());
        }
        wp.writeMapleAsciiString(quest.getQuestData());
        return wp.getPacket();
    }
    
    public static OutPacket UpdateQuestInfo(short quest, int npc) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        wp.write(8); 
        wp.writeShort(quest);
        wp.writeInt(npc);
        wp.writeInt(0);
        return wp.getPacket();
   }

    public static OutPacket ItemExpired(int itemID) {
	WritingPacket wp = new WritingPacket();
	wp.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
	wp.write(PacketHeaders.STATUS_INFO_EXPIRE);
	wp.writeInt(itemID);
	return wp.getPacket();
    }

    public static OutPacket RemovePlayerFromMap(int cID) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
        wp.writeInt(cID);
        return wp.getPacket();
    }
    
    public static OutPacket RemoveSpecialMapObject(MapleSummon summon, boolean animated) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.REMOVE_SPECIAL_MAPOBJECT.getValue());
        wp.writeInt(summon.getOwnerId());
        wp.writeInt(summon.getObjectId());
        wp.write(animated ? 4 : 1); 
        return wp.getPacket();
    }
    
     public static OutPacket SpawnPlayerMapObject(Player p) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPAWN_PLAYER.getValue());
        
        wp.writeInt(p.getId());
        wp.writeMapleAsciiString(p.getName());
        MapleGuild guild = p.getGuild();
        if (guild != null) {
            wp.writeMapleAsciiString(guild.getName());
            wp.writeShort(guild.getEmblemBackground());
            wp.write(guild.getEmblemBackgroundColor());
            wp.writeShort(guild.getEmblemDesign());
            wp.write(guild.getEmblemDesignColor());
        } else {
            wp.writeMapleAsciiString("");
            wp.writeShort(0);
            wp.write(0);
            wp.writeShort(0);
            wp.write(0);
        }
        
        wp.writeInt(0);
        wp.writeInt(1);
        if (p.getBuffedValue(BuffStat.MORPH) != null) {
            wp.write(2);
        } else {
            wp.write(0);
        }
        wp.writeShort(0);
        wp.write(0xF8);
        long buffmask = 0;
        Integer buffvalue = null;
        if (p.getBuffedValue(BuffStat.DARKSIGHT) != null && !p.isHidden()) {
            buffmask |= BuffStat.DARKSIGHT.getValue();
        }
        if (p.getBuffedValue(BuffStat.COMBO) != null) {
            buffmask |= BuffStat.COMBO.getValue();
            buffvalue = Integer.valueOf(p.getBuffedValue(BuffStat.COMBO).intValue());
        }
        if (p.getBuffedValue(BuffStat.SHADOWPARTNER) != null) {
            buffmask |= BuffStat.SHADOWPARTNER.getValue();
        }
        if (p.getBuffedValue(BuffStat.SOULARROW) != null) {
            buffmask |= BuffStat.SOULARROW.getValue();
        }
        if (p.getBuffedValue(BuffStat.MORPH) != null) {
            buffvalue = Integer.valueOf(p.getBuffedValue(BuffStat.MORPH).intValue());
        }
        if (p.getBuffedValue(BuffStat.STUN) != null) {
            buffvalue = Integer.valueOf(p.getBuffedValue(BuffStat.STUN).intValue());
        }
        if (p.getBuffedValue(BuffStat.DARKNESS) != null) {
            buffvalue = Integer.valueOf(p.getBuffedValue(BuffStat.DARKNESS).intValue());
        }
        if (p.getBuffedValue(BuffStat.SEAL) != null) {
            buffvalue = Integer.valueOf(p.getBuffedValue(BuffStat.SEAL).intValue());
        }
        if (p.getBuffedValue(BuffStat.WEAKEN) != null) {
            buffvalue = Integer.valueOf(p.getBuffedValue(BuffStat.WEAKEN).intValue());
        }
        if (p.getBuffedValue(BuffStat.CURSE) != null) {
            buffvalue = Integer.valueOf(p.getBuffedValue(BuffStat.CURSE).intValue());
        }
        if (p.getBuffedValue(BuffStat.POISON) != null) {
            buffvalue = Integer.valueOf(p.getBuffedValue(BuffStat.POISON).intValue());
        }
        if (p.getBuffedValue(BuffStat.SHIELD) != null) {
            buffvalue = Integer.valueOf(p.getBuffedValue(BuffStat.SHIELD).intValue());
        }
        
        wp.writeInt((int) ((buffmask >> 32) & 0xffffffffL));
        if (buffvalue != null) {
            if (p.getBuffedValue(BuffStat.MORPH) != null) {
                wp.writeShort(buffvalue);
            } else {
                wp.write(buffvalue.byteValue());
            }
        }
        wp.writeInt((int) (buffmask & 0xffffffffL));
        int CHAR_MAGIC_SPAWN = new Random().nextInt();
        wp.writeInt(0);
        wp.writeShort(0);
        wp.writeInt(CHAR_MAGIC_SPAWN);
        wp.writeLong(0);
        wp.writeShort(0);
        wp.writeInt(CHAR_MAGIC_SPAWN);
        wp.writeLong(0);
        wp.writeShort(0);
        wp.writeInt(CHAR_MAGIC_SPAWN);
        wp.writeShort(0);
        if (p.getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
            TamingMob mount = p.getMount();
            if (mount != null) {
                if (p.getInventory(InventoryType.EQUIPPED).getItem((byte) -19) != null) {
                    wp.writeInt(mount.getItemId());
                    wp.writeInt(mount.getSkillId());
                    wp.writeInt(CHAR_MAGIC_SPAWN);
                }
            } else {
                wp.writeInt(1932000);
                wp.writeInt(5221006);
                wp.writeInt(CHAR_MAGIC_SPAWN);
            }
        } else {
            wp.writeInt(0);
            wp.writeInt(0);
            wp.writeInt(CHAR_MAGIC_SPAWN);
        }
        wp.writeLong(0);
        wp.writeInt(CHAR_MAGIC_SPAWN);
        wp.writeLong(0);
        wp.writeInt(0);
        wp.writeShort(0);
        wp.writeInt(CHAR_MAGIC_SPAWN);
        wp.writeInt(0);
        wp.writeShort(p.getJob().getId());
        AddCharLook(wp, p, false);
        wp.writeInt(p.getInventory(InventoryType.CASH).countById(5110000));
        wp.writeInt(p.getItemEffect());
        wp.writeInt(p.getChair());
        wp.writePos(p.getPosition());
        wp.write(p.getStance());
        wp.writeShort(0); 
        wp.writeBool(p.isGameMaster());
        if (!p.isHidden() && p.getActivePets() > 0) {
            for (final ItemPet pet : p.getPets()) {
                if (pet.getSummoned()) {
                    PetPackets.AddPetInfo(wp, pet, false);
                }
            }
        }
        wp.write(0);
        wp.writeInt(1);
        wp.writeLong(0);
        if (p.getPlayerShop() != null && p.getPlayerShop().isOwner(p)) {
            if (p.getPlayerShop().hasFreeSlot()) {
                PersonalShopPackets.AddAnnounceBox(wp, p.getPlayerShop(), p.getPlayerShop().getVisitors().length);
            } else {
                PersonalShopPackets.AddAnnounceBox(wp, p.getPlayerShop(), 1);
            }
        } else if (p.getMiniGame() != null && p.getMiniGame().isOwner(p)) {
            if (p.getMiniGame().hasFreeSlot()) {
                MinigamePackets.AddAnnounceBox(wp, p.getMiniGame(), 1, 0, 1, 0);
            } else {
                MinigamePackets.AddAnnounceBox(wp, p.getMiniGame(), 1, 0, 2, 1);
            }
        } else { 
            wp.writeBool(false);
        }
        if (p.getChalkboard() != null) {
            wp.writeBool(true);
            wp.writeMapleAsciiString(p.getChalkboard());
        } else {
            wp.writeBool(false);
        }
        AddRingLooks(wp, p);
        wp.write(0);
        wp.write(p.getTeam());
        return wp.getPacket();
    }
     
    private static void AddRingLooks(WritingPacket wp, Player p) {
        wp.write(p.getEquippedRing(ItemRingType.CRUSH_RING.getType()) != 0 ? 1 : 0);
        for (ItemRing ring : p.getCrushRings()) {
            if (ring.getRingId() == p.getEquippedRing(ItemRingType.CRUSH_RING.getType())) {
                wp.writeInt(ring.getRingId());
                wp.writeInt(0);
                wp.writeInt(ring.getPartnerRingId());
                wp.writeInt(0);
                wp.writeInt(ring.getItemId());
            }
        }
        wp.write(p.getEquippedRing(ItemRingType.FRIENDSHIP_RING.getType()) != 0 ? 1 : 0);
        for (ItemRing ring : p.getFriendshipRings()) {
            if (ring.getRingId() == p.getEquippedRing(ItemRingType.FRIENDSHIP_RING.getType())) {
                wp.writeInt(ring.getRingId());
                wp.writeInt(0);
                wp.writeInt(ring.getPartnerRingId());
                wp.writeInt(0);
                wp.writeInt(ring.getItemId());
            }
        }
        wp.write(p.getEquippedRing(ItemRingType.WEDDING_RING.getType()) != 0 ? 1 : 0);
        for (ItemRing ring : p.getWeddingRings()) {
            if (ring.getRingId() == p.getEquippedRing(ItemRingType.WEDDING_RING.getType())) {
                wp.writeInt(ring.getPartnerChrId());
                wp.writeInt(p.getId());
                wp.writeInt(ring.getItemId());
            }
        }
    }
    
    public static OutPacket UseChalkBoard(Player p, boolean close) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.CHANGE_BINDING.getValue());
        wp.writeInt(p.getId());
        if (close) {
            wp.writeBool(false);
        } else {
            wp.writeBool(true);
            wp.writeMapleAsciiString(p.getChalkboard());
        }
        return wp.getPacket();
    }
    
    private static long GetLongMask( List<Pair<BuffStat, Integer>> statups) {
        long mask = 0;
        for (Pair<BuffStat, Integer> statup : statups) {
             mask |= statup.getLeft().getValue();
        }
        return mask;
    }

    private static long GetLongMaskFromList(List<BuffStat> statups) {
        long mask = 0;
        for (BuffStat statup : statups) {
             mask |= statup.getValue();
        }
        return mask;
    }
    
    private static long GetLongMaskFromListD(List<Disease> statups) {
        long mask = 0;
        for (Disease statup : statups)
            mask |= statup.getValue();
        return mask;
    }
    
    private static long GetLongMaskD(List<Pair<Disease, Integer>> statups) {
        long mask = 0;
        for (Pair<Disease, Integer> statup : statups) {
            mask |= statup.getLeft().getValue();
        }
        return mask;
    }
    
    public static OutPacket CancelBuff(List<BuffStat> statups) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_CANCEL_STATUS_EFFECT.getValue());
        long mask = GetLongMaskFromList(statups);
        wp.writeLong(0);
        wp.writeLong(mask); 
        wp.write(3);  
        return wp.getPacket();
    }

    public static OutPacket CancelDebuff(long mask) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_CANCEL_STATUS_EFFECT.getValue());
        wp.writeLong(0);
        wp.writeLong(mask);
        wp.write(0);
        return wp.getPacket();
    }
    
     public static OutPacket CancelDebuff(List<Disease> statups) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_CANCEL_STATUS_EFFECT.getValue());
        long mask = GetLongMaskFromListD(statups);
        wp.writeLong(0);
        wp.writeLong(mask);
        wp.write(0);
        return wp.getPacket();
    }
     
    public static OutPacket CancelDebuff(long mask, boolean first) {
        
        WritingPacket mplew = new WritingPacket();

        mplew.writeShort(SendPacketOpcode.FIRST_PERSON_CANCEL_STATUS_EFFECT.getValue());
        mplew.writeLong(first ? mask : 0);
        mplew.writeLong(first ? 0 : mask);
        mplew.write(1);

        return mplew.getPacket();
    }
    
    public static OutPacket GiveBuffTest(int buffID, int buffLength, long mask) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_APPLY_STATUS_EFFECT.getValue());
        wp.writeLong(0);
        wp.writeLong(mask);
        wp.writeShort(1);
        wp.writeInt(buffID);
        wp.writeInt(buffLength);
        wp.writeShort(0); 
        wp.write(0); 
        wp.write(0);
        wp.write(0);
        return wp.getPacket();
    }
    
    public static OutPacket GiveBuff(int buffID, int buffLength, List<Pair<BuffStat, Integer>> statups) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_APPLY_STATUS_EFFECT.getValue());
        long mask = GetLongMask(statups);
        wp.writeLong(0);
        wp.writeLong(mask);
        for (Pair<BuffStat, Integer> statup : statups) {
                wp.writeShort(statup.getRight().shortValue());
                wp.writeInt(buffID);
                wp.writeInt(buffLength);
        }
        if (buffID == 1004 || buffID == 5221006) {
                wp.writeInt(0x2F258DB9);
        } else {
                wp.writeShort(0); 
        }
        wp.write(0); 
        wp.write(0); 
        wp.write(0);
        return wp.getPacket();
    }
     
//     public static OutPacket GiveBuff(int buffid, int bufflength, List<Pair<BuffStat, Integer>> statups) {
//        WritingPacket mplew = new WritingPacket();
//        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
//        long mask = GetLongMask(statups);
//        if (bufflength % 10000000 != 1004 && bufflength != 5221006) {
//            mplew.writeLong(0);
//        } else {
//            mplew.writeInt(0);
//        }
//        mplew.writeLong(mask);
//        if (bufflength % 10000000 == 1004 || bufflength == 5221006) {
//            mplew.writeInt(0);
//        }
//        for (Pair<BuffStat, Integer> statup : statups) {
//            mplew.writeShort(statup.getRight().shortValue());
//            mplew.writeInt(buffid);
//            mplew.writeInt(bufflength);
//        }
//        if (bufflength % 10000000 == 1004 || bufflength == 5221006) {
//            mplew.writeInt(0);
//        } else {
//            mplew.writeShort(0);
//        }
//        mplew.write(0);  
//        mplew.write(0);  
//        mplew.write(0);
//        if (bufflength % 10000000 == 1004 || bufflength == 5221006) {
//            mplew.write(0);
//        }
//        return mplew.getPacket();
//    }

    public static OutPacket GiveBuff(int buffID, int buffLength, List<Pair<BuffStat, Integer>> statups, boolean morph, boolean ismount, TamingMob mount) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_APPLY_STATUS_EFFECT.getValue());
        long mask = GetLongMask(statups);
        wp.writeLong(0);
        wp.writeLong(mask);
        if (!ismount) {
            for (Pair<BuffStat, Integer> statup : statups) {
                wp.writeShort(statup.getRight().shortValue());
                wp.writeInt(buffID);
                wp.writeInt(buffLength);
            }
            wp.writeShort(0);
            wp.write(0);
            wp.write(0);
            wp.write(0);
        } else {
            if (ismount) {
                wp.writeShort(0);
                wp.writeInt(mount.getItemId());
                wp.writeInt(mount.getSkillId());
                wp.writeInt(0);
                wp.writeShort(0);
                wp.write(0);
            } else {
                return null;
            }
        }
        return wp.getPacket();
    }

//    public static OutPacket GiveForeignBuff(int cID, List<Pair<BuffStat, Integer>> statups, boolean morph) {
//        WritingPacket wp = new WritingPacket();
//        wp.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
//        wp.writeInt(cID);
//        long mask = GetLongMask(statups);
//        wp.writeLong(0);
//        wp.writeLong(mask);
//        for (Pair<BuffStat, Integer> statup : statups) {
//            if (morph) {
//                wp.write(statup.getRight().byteValue());
//            } else {
//                wp.writeShort(statup.getRight().shortValue());
//            }
//        }
//        wp.writeShort(0);
//        if (morph) {
//            wp.writeShort(0);
//        }
//        wp.write(0);
//        return wp.getPacket();
//    }
    
     public static OutPacket BuffMapEffect(int cid, List<Pair<BuffStat, Integer>> statups, boolean morph) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_APPLY_STATUS_EFFECT.getValue());
        wp.writeInt(cid);
        long mask = GetLongMask(statups);
        wp.writeLong(0);
        wp.writeLong(mask);
        for (Pair<BuffStat, Integer> statup : statups) {
            wp.writeShort(statup.getRight().shortValue());
            if (morph) {
                wp.write(statup.getRight().byteValue());
            }
        }
        wp.write(0);
        wp.writeShort(0);
        return wp.getPacket();
    }
    
    public static OutPacket GiveForeignDebuff(int cID, List<Pair<Disease, Integer>> statups, MobSkill skill) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_APPLY_STATUS_EFFECT.getValue());
        wp.writeInt(cID);
        long mask = GetLongMaskD(statups);
        wp.writeLong(0);
        wp.writeLong(mask);
        for (@SuppressWarnings("unused") Pair<Disease, Integer> statup : statups) {
            wp.writeShort(skill.getSkillId());
            wp.writeShort(skill.getSkillLevel());
        }
        wp.writeShort(0); 
        wp.writeShort(900);
        return wp.getPacket();
    }
    
     public static OutPacket GiveForeignDebuff(int cid, final List<Pair<Disease, Integer>> statups, int skillid, int level) {
        
        WritingPacket wp = new WritingPacket();

        wp.writeShort(SendPacketOpcode.THIRD_PERSON_APPLY_STATUS_EFFECT.getValue());
        wp.writeInt(cid);
        long mask = GetLongMaskD(statups);
        wp.writeLong(0);
        wp.writeLong(mask);

        if (skillid == 125) {
            wp.writeShort(0);
        }
        wp.writeShort(skillid);
        wp.writeShort(level);
        wp.writeShort(0);
        wp.writeShort(900); 
        return wp.getPacket();
    }

    public static OutPacket GiveForeignDebuff(int cID, long mask, MobSkill skill) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_APPLY_STATUS_EFFECT.getValue());
        wp.writeInt(cID);
        wp.writeLong(0);
        wp.writeLong(mask);
        wp.writeShort(skill.getSkillId());
        wp.writeShort(skill.getSkillLevel());
        wp.writeShort(0);
        wp.writeShort(900);
        return wp.getPacket();
    }
	
    public static OutPacket GiveDebuff(long mask, List<Pair<Disease, Integer>> statups, MobSkill skill) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_APPLY_STATUS_EFFECT.getValue());
        wp.writeLong(0);
        wp.writeLong(mask);
        for (Pair<Disease, Integer> statup : statups) {
            wp.writeShort(statup.getRight().shortValue());
            wp.writeShort(skill.getSkillId());
            wp.writeShort(skill.getSkillLevel());
            wp.writeInt((int) skill.getDuration());
        }
        wp.writeShort(0);  
        wp.writeShort(900); 
        wp.write(1);

        return wp.getPacket();
    }
    
     public static OutPacket GiveDebuff(List<Pair<Disease, Integer>> statups, MobSkill skill) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_APPLY_STATUS_EFFECT.getValue());
        long mask = GetLongMaskD(statups);
        wp.writeLong(0);
        wp.writeLong(mask);
        for (Pair<Disease, Integer> statup : statups) {
            wp.writeShort(statup.getRight().shortValue());
            wp.writeShort(skill.getSkillId());
            wp.writeShort(skill.getSkillLevel());
            wp.writeInt((int) skill.getDuration());
        }
        wp.writeShort(0);
        wp.writeShort(900);
        wp.write(1);
        return wp.getPacket();
    }
     
    public static OutPacket GiveDebuff(final List<Pair<Disease, Integer>> statups, int skillid, int level, int duration) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_APPLY_STATUS_EFFECT.getValue());
        long mask = GetLongMaskD(statups);
        wp.writeLong(0);
        wp.writeLong(mask);

        for (Pair<Disease, Integer> statup : statups) {
            wp.writeShort(statup.getRight().shortValue());
            wp.writeShort(skillid);
            wp.writeShort(level);
            wp.writeInt((int) duration);
        }
        wp.writeShort(0); 
        wp.writeShort(900); 
        wp.write(1);
        return wp.getPacket();
    }
    
    public static OutPacket CancelForeignBuff(int cID, List<BuffStat> statups) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_CANCEL_STATUS_EFFECT.getValue());
        wp.writeInt(cID);
        long mask = GetLongMaskFromList(statups);
        wp.writeLong(0);
        wp.writeLong(mask); 
        return wp.getPacket();
    }
    
    public static OutPacket CancelForeignDebuff(int cID, long mask) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_CANCEL_STATUS_EFFECT.getValue());
        wp.writeInt(cID);
        wp.writeLong(0);
        wp.writeLong(mask);
        return wp.getPacket();
    }
    
    public static OutPacket CancelForeignDebuff(int cID, List<Disease> statups) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_CANCEL_STATUS_EFFECT.getValue());
        wp.writeInt(cID);
        long mask = GetLongMaskFromListD(statups);
        wp.writeLong(0);
        wp.writeLong(mask);
        return wp.getPacket();
    }
    
    public static OutPacket CancelForeignDebuff(int cid, long mask, boolean first) {
        
        WritingPacket mplew = new WritingPacket();

        mplew.writeShort(SendPacketOpcode.THIRD_PERSON_CANCEL_STATUS_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.writeLong(first ? mask : 0);
        mplew.writeLong(first ? 0 : mask);

        return mplew.getPacket();
    }
    
    public static OutPacket UpdateAriantPQRanking(String name, int score, boolean empty) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ARIANT_PQ_START.getValue());
        wp.write(empty ? 0 : 1);
        if (!empty) {
            wp.writeMapleAsciiString(name);
            wp.writeInt(score);
        }
        return wp.getPacket();
    }
    
    public static OutPacket ShowAriantScoreBoard() {
	WritingPacket wp = new WritingPacket();
	wp.writeShort(SendPacketOpcode.ARIANT_SCOREBOARD.getValue());
	return wp.getPacket();
    }
    
    public static OutPacket GetShowItemGain(int itemID, short quantity) {
        return GetShowItemGain(itemID, quantity, false);
    }

    public static OutPacket GetShowItemGain(int itemID, short quantity, boolean inChat) {
        WritingPacket wp = new WritingPacket();
        if (inChat) {
            wp.writeShort(SendPacketOpcode.FIRST_PERSON_VISUAL_EFFECT.getValue());
            wp.write(3);
            wp.write(1);
            wp.writeInt(itemID);
            wp.writeInt(quantity);
        } else {
            wp.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            wp.writeShort(0);
            wp.writeInt(itemID);
            wp.writeInt(quantity);
            wp.writeInt(0);
            wp.writeInt(0);
        }
        return wp.getPacket();
    }
    
    public static OutPacket GetWarpToMap(Field to, int spawnPoint, Player p) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.WARP_TO_MAP.getValue()); 
        wp.writeInt(p.getClient().getChannel() - 1);
        wp.writeShort(0x2);
        wp.writeShort(0);
        wp.writeInt(to.getId());
        wp.write(spawnPoint);
        wp.writeShort(p.getStat().getHp()); 
        wp.write(0);
        long questMask = 0x1ffffffffffffffL;
        wp.writeLong(questMask);
        return wp.getPacket();
    }
    
    public static OutPacket ShowThirdPersonEffect(int cID, int effect) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_VISUAL_EFFECT.getValue());
        wp.writeInt(cID);
        wp.write(effect);
        return wp.getPacket();
    }
    
    public static OutPacket UpdateSkill(int skillID, int level, int masterLevel) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_SKILLS.getValue());
        wp.write(1);
        wp.writeShort(1);
        wp.writeInt(skillID);
        wp.writeInt(level);
        wp.writeInt(masterLevel);
        wp.write(1);
        return wp.getPacket();
    }
   
    public static OutPacket GetShowExpGain(int base, int party,/* int thirdBonus, int hours,*/ boolean quest, boolean killer) {
        WritingPacket pw = new WritingPacket(21);
        int hours = 0, thirdBonus = 0;
        pw.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        pw.write(3); // EXP stat
        pw.writeBool(killer); // white text = killer
        pw.writeInt(base); // base experience
        pw.writeBool(quest); // "onQuest"
        pw.writeInt(0); // non-stackable event EXP
        pw.write(thirdBonus); // 0 = not third mob or no event
        pw.write(party); // add party bonus to base, then this splits it off to display
        pw.writeInt(0); // wedding bonus exp
        if (thirdBonus > 0) {
            pw.write(hours); // shows how many hours for third bonus
        }
        if (quest) { // "onQuest"
            pw.write(0);
        }
        int mod = GameConstants.PARTY_EXPERIENCE_MOD != 1 ? GameConstants.PARTY_EXPERIENCE_MOD * 100 : 0;
        pw.write(mod); // party exp modifier (bonus exp)

        return pw.getPacket();
    }
   
    public static OutPacket GetShowMesoGain(int gain) {
        return GetShowMesoGain(gain, false);
    }

    public static OutPacket GetShowMesoGain(int gain, boolean inChat) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        if (!inChat) {
            wp.write(PacketHeaders.STATUS_INFO_INVENTORY);
            wp.write(1);
        } else {
            wp.write(5);
        }
        wp.writeInt(gain);
        wp.writeShort(0); 
        return wp.getPacket();
    }
    
    public static OutPacket GetShowFameGain(int gain) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        wp.write(4);
        wp.writeInt(gain);
        return wp.getPacket();
    }

    public static OutPacket BuffMapPirateEffect(Player p, List<Pair<BuffStat, Integer>> stats, int skillId, int duration) {
        WritingPacket wp = new WritingPacket();

        wp.writeShort(SendPacketOpcode.THIRD_PERSON_APPLY_STATUS_EFFECT.getValue());
        wp.writeInt(p.getId());
        long updateMask = GetLongMask(stats);
        wp.writeLong(0);
        wp.writeLong(updateMask);
        wp.writeShort((short) 0);
        for (Pair<BuffStat, Integer> statupdate : stats) {
            wp.writeShort(statupdate.getRight().shortValue());
            wp.writeShort(0);
            wp.writeInt(skillId);
            wp.writeInt(0);
            wp.write(0);
            wp.writeShort((short) duration);
        }
        wp.writeShort((short) 0);
        return wp.getPacket();
    }

    public static OutPacket UsePirateSkill(List<Pair<BuffStat, Integer>> stats, int skillId, int duration, short delay) {
        WritingPacket wp = new WritingPacket();

        wp.writeShort(SendPacketOpcode.FIRST_PERSON_APPLY_STATUS_EFFECT.getValue());
        long updateMask = GetLongMask(stats);
        wp.writeLong(0);
        wp.writeLong(updateMask);
        wp.writeShort((short) 0);
        for (Pair<BuffStat, Integer> statupdate : stats) {
                wp.writeShort(statupdate.getRight().shortValue());
                wp.writeShort((short) 0);
                wp.writeInt(skillId);
                wp.writeInt(0);
                wp.write(0);
                wp.writeShort(duration);
        }
        wp.writeShort(delay);
        wp.write(0); 
        return wp.getPacket();
    }

    public static OutPacket GetNPCTalk(int npc, byte msgType, String talk, String endBytes) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        wp.write(4); 
        wp.writeInt(npc);
        wp.write(msgType);
        wp.writeMapleAsciiString(talk);
        wp.write(HexTool.getByteArrayFromHexString(endBytes));
        return wp.getPacket();
    }

    public static OutPacket GetNPCTalkStyle(int npc, String talk, int styles[]) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        wp.write(4); 
        wp.writeInt(npc);
        wp.write(7);
        wp.writeMapleAsciiString(talk);
        wp.write(styles.length);
        for (int i = 0; i < styles.length; i++) {
            wp.writeInt(styles[i]);
        }
        return wp.getPacket();
    }

    public static OutPacket GetNPCTalkNum(int npc, String talk, int def, int min, int max) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        wp.write(4); 
        wp.writeInt(npc);
        wp.write(3);
        wp.writeMapleAsciiString(talk);
        wp.writeInt(def);
        wp.writeInt(min);
        wp.writeInt(max);
        wp.writeInt(0);
        return wp.getPacket();
    }

    public static OutPacket GetNPCTalkText(int npc, String talk) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        wp.write(4); 
        wp.writeInt(npc);
        wp.write(2);
        wp.writeMapleAsciiString(talk);
        wp.writeInt(0);
        wp.writeInt(0);
        return wp.getPacket();
    }
    
    public static OutPacket SkillCooldown(int sID, int time) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.COOLDOWN.getValue());
        wp.writeInt(sID);
        wp.writeShort(time);
        return wp.getPacket();
    }
    
    public static OutPacket GetKeyMap(Map<Integer, PlayerKeyBinding> keyBindings) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.KEYMAP.getValue());
        wp.write(0);
        for (int x = 0; x < 90; x++) {
            PlayerKeyBinding binding = keyBindings.get(Integer.valueOf(x));
            if (binding != null) {
                wp.write(binding.getType());
                wp.writeInt(binding.getAction());
            } else {
                wp.write(0);
                wp.writeInt(0);
            }
        }
        return wp.getPacket();
    }
    
    public static OutPacket GetMacros(PlayerSkillMacro[] macros) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SKILL_MACRO.getValue());
        wp.write(macros.length); 
        for (byte i = 0; i < macros.length; i++) {
            PlayerSkillMacro macro = macros[i];
            wp.writeMapleAsciiString(macro.getName());
            wp.writeBool(macro.isSilent());
            wp.writeInt(macro.getFirstSkill());
            wp.writeInt(macro.getSecondSkill());
            wp.writeInt(macro.getThirdSkill());
        }
        return wp.getPacket();
    }
    
    public static OutPacket GetClock() { 
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.CLOCK.getValue());
        wp.write(1);
        Calendar now = Calendar.getInstance();
        wp.write(now.get(Calendar.HOUR_OF_DAY));
        wp.write(now.get(Calendar.MINUTE));
        wp.write(now.get(Calendar.SECOND));
        return wp.getPacket();
    }
    
    public static OutPacket GetClockTimer(int seconds) { 
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.CLOCK.getValue());
        wp.write(2); 
        wp.writeInt(seconds);
        return wp.getPacket();
    }

    public static OutPacket DestroyClock() { 
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.DESTROY_CLOCK.getValue());
        return wp.getPacket();
    }
    
    public static OutPacket UpdateCharLook(Player p) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_CHAR_LOOK.getValue());
        wp.writeInt(p.getId());
        wp.writeBool(true);
        AddCharLook(wp, p, false);
        AddRingLooks(wp, p);
        wp.write(0);
        wp.writeShort(0);
        return wp.getPacket();
    }
    
     

/**
     * Adds the aesthetic aspects of a character to an existing
     * MaplePacketLittleEndianWriter.
     *
     * @param wp The MaplePacketLittleEndianWrite instance to write the stats to.
     * @param p The character to add the looks of.
     * @param mega Unknown
     */
    public static void AddCharLook(WritingPacket wp, Player p, boolean mega) {
        wp.write(p.getGender());
        wp.write(p.getSkinColor().getId());
        wp.writeInt(p.getFace());
        wp.writeBool(mega);
        wp.writeInt(p.getHair()); 
        Inventory equip = p.getInventory(InventoryType.EQUIPPED);
        Map<Short, Integer> myEquip = new LinkedHashMap<>();
        Map<Short, Integer> maskedEquip = new LinkedHashMap<>();
        
        for (Item item : equip.list()) {
            short pos = (short) (item.getPosition() * -1);
            if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, item.getItemId());
            } else if (pos > 100 && pos != 111) {
                pos -= 100;
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, item.getItemId());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, item.getItemId());
            }
        }

        for (Entry<Short, Integer> entry : myEquip.entrySet()) {
            wp.write(entry.getKey());
            wp.writeInt(entry.getValue());
        }
        wp.write(0xFF);
        
        for (Entry<Short, Integer> entry : maskedEquip.entrySet()) {
            wp.write(entry.getKey());
            wp.writeInt(entry.getValue());
        }
        wp.write(0xFF);
        Item cWeapon = equip.getItem((short) -111);
        wp.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);
        
         for (int i = 0; i < 3; i++) {
            if (p.getPet(i) != null) {
                wp.writeInt(p.getPet(i).getPetItemId());
            } else {
                wp.writeInt(0);
            }
        }
    }
    
    /**
     * Gets character info for a character.
     * 
     * @param p The character to get info about.
     * @return The character info packet.
     */
    public static OutPacket GetCharInfo(Player p) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.WARP_TO_MAP.getValue());
        wp.writeInt(p.getClient().getChannel() - 1);
        wp.write(1);
        wp.write(1);
        wp.writeShort(0);
        
        wp.writeInt(Randomizer.nextInt());
        wp.writeInt(Randomizer.nextInt());
        wp.writeInt(Randomizer.nextInt());
        
        AddCharacterData(wp, p);
        
        wp.writeLong(GetTime(System.currentTimeMillis()));
        return wp.getPacket();
    }
    
    public static OutPacket PersonalInfo(Player p, boolean self) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.CHAR_INFO.getValue());
        wp.writeInt(p.getId());
        wp.write(p.getLevel());
        wp.writeShort(p.getJob().getId());
        wp.writeShort(p.getFame());
        wp.writeBool(p.getPartnerId() != 0);
        if (p.getGuildId() <= 0) {
            wp.writeMapleAsciiString("-");
            wp.writeMapleAsciiString("");
        } else {
            final MapleGuild gs = GuildService.getGuild(p.getGuildId());
            if (gs != null) {
                wp.writeMapleAsciiString(gs.getName());
                if (gs.getAllianceId() > 0) {
                    final MapleGuildAlliance allianceName = AllianceService.getAlliance(gs.getAllianceId());
                    if (allianceName != null) {
                        wp.writeMapleAsciiString(allianceName.getName());
                    } else {
                        wp.writeMapleAsciiString("");
                    }
                } else {
                    wp.writeMapleAsciiString("");
                }
            } else {
                wp.writeMapleAsciiString("-");
                wp.writeMapleAsciiString("");
            }
        }
        wp.writeBool(self);
        for (final ItemPet pet : p.getPets()) {
            if (pet.getSummoned()) {
                wp.writeBool(true);
                wp.writeInt(pet.getPetItemId()); 
                wp.writeMapleAsciiString(pet.getName());
                wp.write(pet.getLevel());
                wp.writeShort(pet.getCloseness());
                wp.write(pet.getFullness());
                wp.writeShort(0);
                final Item inv = p.getInventory(InventoryType.EQUIPPED).getItem((byte)  -114);
                wp.writeInt(inv == null ? 0 : inv.getItemId());   
            }
        }
        wp.writeBool(false); 
        if (p.getMount() != null && p.getInventory(InventoryType.EQUIPPED).getItem((byte) -18) != null) {
            if (p.getInventory(InventoryType.EQUIPPED).getItem((byte) -18).getItemId() == p.getMount().getItemId()) {
                if (p.getInventory(InventoryType.EQUIPPED).getItem((byte) -19) != null) { 
                    wp.write(p.getMount().getId()); 
                    wp.writeInt(p.getMount().getLevel());
                    wp.writeInt(p.getMount().getExp()); 
                    wp.writeInt(p.getMount().getTiredness()); 
                }
            }
        }
        wp.writeBool(false);  
        wp.write(p.getCashShop().getWishList().size());
        for (int sn : p.getCashShop().getWishList()) {
            wp.writeInt(sn);
        }
        wp.writeInt(1); //monster book level
        wp.writeInt(0); //monster book normals
        wp.writeInt(0); //monster book specials
        wp.writeInt(0); //monster book size
        wp.writeInt(0); //monster book cover
        return wp.getPacket();
    }

    public static void addCharStats(WritingPacket wp, Player p) {
        wp.writeInt(p.getId());
        wp.writeAsciiString(p.getName());
        for (int x = p.getName().length(); x < 13; x++) {
            wp.write(0);
        }
        wp.write(p.getGender()); 
        wp.write(p.getSkinColor().getId()); 
        wp.writeInt(p.getFace()); 
        wp.writeInt(p.getHair()); 
        for (int i = 0; i < 3; i++) {
            if (p.getPet(i) != null) {
                wp.writeLong(p.getPet(i).getUniqueId());
            } else {
                wp.writeLong(0);
            }
        }
        wp.write(p.getLevel()); 
        wp.writeShort(p.getJob().getId()); 
        wp.writeShort(p.getStat().getStr()); 
        wp.writeShort(p.getStat().getDex()); 
        wp.writeShort(p.getStat().getInt());
        wp.writeShort(p.getStat().getLuk()); 
        wp.writeShort(p.getStat().getHp()); 
        wp.writeShort(p.getStat().getMaxHp()); 
        wp.writeShort(p.getStat().getMp()); 
        wp.writeShort(p.getStat().getMaxMp()); 
        wp.writeShort(p.getStat().getRemainingAp()); 
        wp.writeShort(p.getStat().getRemainingSp()); 
        wp.writeInt(p.getCurrentExp()); 
        wp.writeShort(p.getFame()); 
        wp.writeInt(p.getPartnerId()); 
        wp.writeInt(p.getMapId()); 
        wp.write(p.getInitialSpawnpoint()); 
        wp.writeInt(0);
    }
    
    public static void AddInventoryInfo(WritingPacket wp, Player p) {
        wp.writeInt(p.getMeso()); 
        for (byte i = 1; i <= 5; i++) {
            wp.write(p.getInventory(InventoryType.getByType(i)).getSlotLimit());
        }
        Inventory iv = p.getInventory(InventoryType.EQUIPPED);
        Collection<Item> equippedList = iv.list();
        Item[] equipped = new Item[17];
        Item[] equippedCash = new Item[17];
        for (Item item : equippedList) {
            short pos = item.getPosition();
            if (pos < 0) {
                pos = (byte) Math.abs(pos);
                if (pos > 100) {
                    equippedCash[(byte)(pos - 100)] = (Item)item;
                } else {
                    equipped[(byte) pos] = (Item)item;
                }
            }
            if (pos < 0) {
                if (pos < -100) {
                    pos += 100;
                    pos = (byte) Math.abs(pos);
                    equippedCash[(byte) (pos - 100)] = (Item)item;
                } else {
                    pos = (byte) Math.abs(pos);
                    equipped[(byte) pos] = (Item)item;
                }
            }
        }
        for (Item item : equipped) {
            if (item != null) {
                AddItemInfo(wp, item);
            }
        }
        wp.write(0);
        for (Item item : equippedCash) {
            if (item != null) {
                AddItemInfo(wp, item);
            }
        }
        wp.write(0);
        for (byte i = 1; i < 6; i++) {
            iv = p.getInventory(InventoryType.getByType((byte)i));
            for (Item item : iv.list()) {
                if (item != null && item.getPosition() > 0) {
                    AddItemInfo(wp, item);
                }
            }
            wp.write(0);
        }
    }
    
    protected static void AddItemInfo(WritingPacket wp, Item item) {
        AddItemInfo(wp, item, false, false);
    }
    
    public static void AddItemInfo(WritingPacket wp, Item item, boolean zeroPosition, boolean leaveOut) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        boolean hasUniqueId = item.getUniqueId() > 0;
        short pos = item.getPosition();
        if (zeroPosition) {
            if (!leaveOut) {
                wp.write(0);
            }
        } else {
            if (pos <= -1) {
                pos *= -1;
                if (pos > 100 && pos < 1000) {
                    pos -= 100;
                }
            }
            wp.write(pos);
        }
        wp.write(item.getPet() != null ? 3 : item.getType());
        wp.writeInt(item.getItemId());
        wp.writeBool(hasUniqueId);
        if (hasUniqueId) {
            wp.writeLong(item.getUniqueId());
        } if (item.getPet() != null) { 
            AddPetItemInfo(wp, item, item.getPet());
        } else {
            AddExpirationTime(wp, item.getExpiration());
            if (item.getType() == ItemType.EQUIP) {
                Equip equip = (Equip) item;
                wp.write(equip.getUpgradeSlots());
                wp.write(equip.getLevel());
                wp.writeShort(equip.getStr()); 
                wp.writeShort(equip.getDex()); 
                wp.writeShort(equip.getInt());
                wp.writeShort(equip.getLuk());
                wp.writeShort(equip.getHp());
                wp.writeShort(equip.getMp());
                wp.writeShort(equip.getWatk()); 
                wp.writeShort(equip.getMatk());
                wp.writeShort(equip.getWdef());
                wp.writeShort(equip.getMdef());
                wp.writeShort(equip.getAcc());
                wp.writeShort(equip.getAvoid());
                wp.writeShort(equip.getHands());
                wp.writeShort(equip.getSpeed());
                wp.writeShort(equip.getJump());
                wp.writeMapleAsciiString(equip.getOwner());
                wp.writeShort(equip.getLocked());
                if (!hasUniqueId) {
                    wp.writeLong(0);
                }
            } else {
                wp.writeShort(item.getQuantity());
                wp.writeMapleAsciiString(item.getOwner());
                wp.writeShort(0); 
                if (ii.isThrowingStar(item.getItemId()) || ii.isBullet(item.getItemId())) {
                    wp.writeLong(0);
                }
            }
        }
    }
    
    public static final void AddPetItemInfo(final WritingPacket wp, final Item item, final ItemPet pet) {
        if (item == null) {
            wp.writeLong(GetKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
        } else {
            AddExpirationTime(wp, item.getExpiration() <= System.currentTimeMillis() ? -1 : item.getExpiration());
        }
        wp.writeAsciiString(pet.getName(), 13);
        wp.write(pet.getLevel());
        wp.writeShort(pet.getCloseness());
        wp.write(pet.getFullness());
        if (item == null) {
            wp.writeLong(GetKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
        } else {
            AddExpirationTime(wp, item.getExpiration() <= System.currentTimeMillis() ? -1 : item.getExpiration());
        }
        wp.writeInt(0);
    }
    
    public static OutPacket ShowOwnBerserk(int skillLevel, boolean Berserk) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_VISUAL_EFFECT.getValue());
        wp.write(1);
        wp.writeInt(1320006);
        wp.write(skillLevel);
        wp.write(Berserk ? 1 : 0);
        return wp.getPacket();
    }

    public static OutPacket ShowBerserk(int cID, int skillLevel, boolean Berserk) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_VISUAL_EFFECT.getValue());
        wp.writeInt(cID);
        wp.write(1);
        wp.writeInt(1320006);
        wp.write(skillLevel);
        wp.write(Berserk ? 1 : 0);
        return wp.getPacket();
    }
    
    public static OutPacket UpdateBuddyCapacity(int capacity) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        wp.write(0x15);
        wp.write(capacity);
        return wp.getPacket();
    }
    
    public static OutPacket ShowNotes(ResultSet notes, int count) throws SQLException {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOW_NOTES.getValue());
        wp.write(2);
        wp.write(count);
        for (int i = 0; i < count; i++) {
            wp.writeInt(notes.getInt("id"));
            wp.writeMapleAsciiString(notes.getString("from"));
            wp.writeMapleAsciiString(notes.getString("message"));
            wp.writeLong(GetKoreanTimestamp(notes.getLong("timestamp")));
            wp.write(notes.getInt("fame"));
            notes.next();
        }
        return wp.getPacket();
    }
  
    public static OutPacket SummonAttack(int cID, int objectId, int newStance, List<SummonHandler.SummonAttackEntry> allDamage, int level) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SUMMON_ATTACK.getValue());
        
        wp.writeInt(cID);
        wp.writeInt(objectId);
        
        wp.write(newStance);
        wp.write(allDamage.size());
        
        for (SummonHandler.SummonAttackEntry attackEntry : allDamage) {
            wp.writeInt(attackEntry.getMonster().getObjectId()); 
            if (attackEntry.getMonster().getObjectId() > 0) {
                wp.write(6); 
                wp.writeInt(attackEntry.getDamage()); 
            }
            wp.writeInt(attackEntry.getDamage()); 
        }
        return wp.getPacket();
    }
       
    public static OutPacket SummonSkill(int cID, int summonSkillID, int newStance) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SUMMON_SKILL.getValue());
        wp.writeInt(cID);
        wp.writeInt(summonSkillID);
        wp.write(newStance);
        return wp.getPacket();
    }

    public static OutPacket UpdateMount(Player owner, boolean levelup) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_MOUNT.getValue());
        wp.writeInt(owner.getId());
        wp.writeInt(owner.getMount().getLevel());
        wp.writeInt(owner.getMount().getExp());
        wp.writeInt(owner.getMount().getTiredness());
        wp.write(levelup ? (byte) 1 : (byte) 0);
        return wp.getPacket();
    }
    
    public static OutPacket GetWhisper(String sender, int channel, String text) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.WHISPER.getValue());
        wp.write(0x12);
        wp.writeMapleAsciiString(sender);
        wp.writeShort(channel - 1); 
        wp.writeMapleAsciiString(text);
        return wp.getPacket();
    }
    
    public static OutPacket PrivateChatMessage(String name, String chatText, int mode) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PRIVATE_CHAT.getValue());
        wp.write(mode);
        wp.writeMapleAsciiString(name);
        wp.writeMapleAsciiString(chatText);
        return wp.getPacket();
    }
    
    public static OutPacket ToSpouse(String sender, String text, int type) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPOUSE_CHAT.getValue());
        wp.write(type);
        if (type == 4) {
            wp.write(1);
        } else {
            wp.writeMapleAsciiString(sender);
            wp.write(5);
        }
        wp.writeMapleAsciiString(text);
        return wp.getPacket();
    }
     
    public static OutPacket SendSpouseChat(Player wife, String msg) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPOUSE_CHAT.getValue());
        wp.writeMapleAsciiString(wife.getName());
        wp.writeMapleAsciiString(msg);
        return wp.getPacket();
    }
    
    public static OutPacket UpdateBuddyChannel(int characterID, int channel) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        wp.write(0x14);
        wp.writeInt(characterID);
        wp.write(0);
        wp.writeInt(channel);
        return wp.getPacket();
    }
    
    public static OutPacket MessengerInvite(String from, int messengerID) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MESSENGER.getValue());
        wp.write(0x03);
        wp.writeMapleAsciiString(from);
        wp.write(0x00);
        wp.writeInt(messengerID);
        wp.write(0x00);
        return wp.getPacket();
    }
	 
    public static OutPacket AddMessengerPlayer(String from, Player p, int position, int channel) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MESSENGER.getValue());
        wp.write(0x00);
        wp.write(position);
        AddCharLook(wp, p, true);
        wp.writeMapleAsciiString(from);
        wp.write(channel);
        wp.write(0x00);
        return wp.getPacket();
    }
	 
    public static OutPacket RemoveMessengerPlayer(int position) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MESSENGER.getValue());
        wp.write(0x02);
        wp.write(position);
        return wp.getPacket();
    }
	 
    public static OutPacket UpdateMessengerPlayer(String from, Player p, int position, int channel) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MESSENGER.getValue());
        wp.write(0x07);
        wp.write(position);
        AddCharLook(wp, p, true);
        wp.writeMapleAsciiString(from);
        wp.write(channel);
        wp.write(0x00);
        return wp.getPacket();
    }

    public static OutPacket JoinMessenger(int position) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MESSENGER.getValue());
        wp.write(0x01);
        wp.write(position);
        return wp.getPacket();
    }
	 
    public static OutPacket MessengerChat(String text) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MESSENGER.getValue());
        wp.write(0x06);
        wp.writeMapleAsciiString(text);
        return wp.getPacket();
    }
	 
    public static OutPacket MessengerNote(String text, int mode, int mode2) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MESSENGER.getValue());
        wp.write(mode);
        wp.writeMapleAsciiString(text);
        wp.write(mode2);
        return wp.getPacket();
    }
    
    public static OutPacket UpdateBuddylist(byte action, Collection<MapleBuddyListEntry> buddylist) {
        
        WritingPacket mplew = new WritingPacket();

        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(action);
        mplew.write(buddylist.size());

        for (MapleBuddyListEntry buddy : buddylist) {
            mplew.writeInt(buddy.getCharacterId());
            mplew.writeAsciiString(StringUtil.getRightPaddedStr(buddy.getName(), '\0', 13));
            mplew.write(0);
            mplew.writeInt(buddy.getChannel() == -1 ? -1 : buddy.getChannel() - 1);
        }
        for (int x = 0; x < buddylist.size(); x++) {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static OutPacket BuddylistMessage(byte message) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        wp.write(message);
        return wp.getPacket();
    }

    public static OutPacket RequestBuddylistAdd(int cIDFrom, String nameFrom) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        wp.write(9);
        wp.writeInt(cIDFrom);
        wp.writeMapleAsciiString(nameFrom);
        wp.writeInt(cIDFrom);
        wp.writeAsciiString(StringUtil.getRightPaddedStr(nameFrom, '\0', 13));
        wp.write(1);
        wp.write(31);
        wp.writeInt(0);
        return wp.getPacket();
    }
    
    public static OutPacket GetChatText(int cid, String text, boolean whiteBG, byte show) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MAP_CHAT.getValue());
        wp.writeInt(cid);
        wp.writeBool(whiteBG);
        wp.writeMapleAsciiString(text);
        wp.write(show);
        return wp.getPacket();
    }
    
    public static OutPacket GetWhisperReply(String target, byte reply) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.WHISPER.getValue());
        wp.write(0x0A); 
        wp.writeMapleAsciiString(target);
        wp.write(reply);
        return wp.getPacket();
    }

    public static OutPacket GetFindReplyWithMap(String target, int mapID) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.WHISPER.getValue());
        wp.write(9);
        wp.writeMapleAsciiString(target);
        wp.write(ChatHeaders.FIND_RESPONSE_MAP);
        wp.writeInt(mapID);
        wp.writeLong(0);
        return wp.getPacket();
    }

    public static OutPacket GetFindReply(String target, int channel) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.WHISPER.getValue());
        wp.write(9);
        wp.writeMapleAsciiString(target);
        wp.write(ChatHeaders.FIND_RESPONSE_CHANNEL);
        wp.writeInt(channel - 1);
        return wp.getPacket();
    }
    
    public static OutPacket GetFindReplyWithCS(String target) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.WHISPER.getValue());
        wp.write(9);
        wp.writeMapleAsciiString(target);
        wp.write(ChatHeaders.FIND_RESPONSE_CASH_SHOP);
        wp.writeInt(-1);
        return wp.getPacket();
    }
     
     public static OutPacket OnCoupleMessage(String fiance, String text, boolean spouse) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPOUSE_CHAT.getValue());
        wp.write(spouse ? 5 : 4); 
        if (spouse) { 
            wp.writeMapleAsciiString(fiance);
        }
        wp.write(spouse ? 5 : 1);
        wp.writeMapleAsciiString(text);
        return wp.getPacket();
    } 
     
    public static OutPacket showEventInstructions() {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GMEVENT_INSTRUCTIONS.getValue());
        wp.write(0);
        return wp.getPacket();
    }
     
    public static OutPacket RemoveItemFromMap(int objectID, int animation, int cID) {
        return RemoveItemFromMap(objectID, animation, cID, false, 0);
    }
    
    public static OutPacket RemoveItemFromMap(int objectID, int animation, int cID, boolean pet, int slot) {
        WritingPacket warp = new WritingPacket();
        warp.writeShort(SendPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        warp.write(animation);
        warp.writeInt(objectID);
        if (animation >= 2) {
            warp.writeInt(cID);
            if (pet) {
                warp.write(slot);
            }
        }
        return warp.getPacket();
    }
    
    public static OutPacket GetShowInventoryFull() {
	return GetShowInventoryStatus(0xff);
    }
    
    public static OutPacket ShowItemUnavailable() {
        return GetShowInventoryStatus(0xfe);
    }
    
    public static OutPacket GetInventoryFull() {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        wp.write(1);
        wp.write(0);
        return wp.getPacket();
    }
    
    public static OutPacket GetShowInventoryStatus(int mode) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        wp.write(0);
        wp.write(mode);
        wp.writeInt(0);
        wp.writeInt(0);
        return wp.getPacket();
    }
    
    public static OutPacket GetStorage(int npcID, byte slots, Collection<Item> items, int meso) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.NPC_STORAGE.getValue());
        wp.write(0x15);
        wp.writeInt(npcID);
        wp.write(slots);
        wp.writeShort(0x7E);
        wp.writeShort(0);
        wp.writeInt(0);
        wp.writeInt(meso);
        wp.writeShort(0);
        wp.write((byte) items.size());
        for (Item item : items) {
            AddItemInfo(wp, item, true, true);
        }
        wp.writeShort(0);
        wp.write(0);
        return wp.getPacket();
    }
    
    public static OutPacket GetStorageInsufficientFunds() {
        WritingPacket wp = new WritingPacket(3);
        wp.writeShort(SendPacketOpcode.NPC_STORAGE.getValue());
        wp.write(0x0F);
        return wp.getPacket();
    }
    
    public static OutPacket StorageWithdrawInventoryFull() {
        WritingPacket wp = new WritingPacket(3);
        wp.writeShort(SendPacketOpcode.NPC_STORAGE.getValue());
        wp.write(0x0A);
        return wp.getPacket();
    }

    public static OutPacket GetStorageFull() {
        WritingPacket wp = new WritingPacket(3);
        wp.writeShort(SendPacketOpcode.NPC_STORAGE.getValue());
        wp.write(0x10);
        return wp.getPacket();
    }

    public static OutPacket MesoStorage(byte slots, int meso) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.NPC_STORAGE.getValue());
        wp.write(0x12);
        wp.write(slots);
        wp.writeShort(2); // TODO: int value?
        wp.writeShort(0);
        wp.writeInt(0);
        wp.writeInt(meso);
        return wp.getPacket();
    }

    public static OutPacket StoreStorage(byte slots, InventoryType type, Collection<Item> items) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.NPC_STORAGE.getValue());
        wp.write(0xC);
        wp.write(slots);
        wp.writeInt(type.getBitfieldEncoding());
        wp.writeInt(0);
        wp.write(items.size());
        for (Item item : items) {
            AddItemInfo(wp, item, true, true);
        }
        return wp.getPacket();
    }

    public static OutPacket TakeOutStorage(byte slots, InventoryType type, Collection<Item> items) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.NPC_STORAGE.getValue());
        wp.write(0x9);
        wp.write(slots);
        wp.writeInt(type.getBitfieldEncoding());
        wp.writeInt(0);
        wp.write(items.size());
        for (Item item : items) {
            AddItemInfo(wp, item, true, true);
        }
        return wp.getPacket();
    }
    
    public static OutPacket GetChannelChange(InetAddress inetAddr, int port) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GAME_HOST_ADDRESS.getValue());
        wp.writeBool(true);
        wp.write(inetAddr.getAddress());
        wp.writeShort(port);
        return wp.getPacket();
    }
    
    public static OutPacket UpdateGender(Player p) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.GENDER.getValue());
        wp.write(p.getGender());
        return wp.getPacket();
    }
    
    public static void AddCharacterData(WritingPacket wp, Player p) {
        wp.writeLong(-1);
        addCharStats(wp, p);
        wp.write(p.getBuddylist().getCapacity());
        
        wp.writeInt(p.getMeso()); 
        InventoryType types[] = {InventoryType.EQUIP, InventoryType.USE, InventoryType.SETUP, InventoryType.ETC, InventoryType.CASH};
        for (InventoryType tp : types) {
             wp.write(p.getInventory(tp).getSlotLimit()); 
        }
        Inventory iv = p.getInventory(InventoryType.EQUIPPED);
        Collection<Item> equippedC = iv.list();
        List<Item> equipped = new ArrayList<>(equippedC.size());
        List<Item> equippedCash = new ArrayList<>(equippedC.size());
        for (Item item : equippedC) {
            if (item.getPosition() <= -100) {
                equippedCash.add((Item) item);
            } else {
                equipped.add((Item) item);
            }
        }
        Collections.sort(equipped);
        for (Item item : equipped) {
            AddItemInfo(wp, item);
        }
        wp.write(0);
        for (Item item : equippedCash) {
            AddItemInfo(wp, item);
        }
        wp.write(0);
        for (Item item : p.getInventory(InventoryType.EQUIP).list()) {
            AddItemInfo(wp, item);
        }
        wp.write(0);
        for (Item item : p.getInventory(InventoryType.USE).list()) {
            AddItemInfo(wp, item);
        }
        wp.write(0);
        for (Item item : p.getInventory(InventoryType.SETUP).list()) {
            AddItemInfo(wp, item);
        }
        wp.write(0);
        for (Item item : p.getInventory(InventoryType.ETC).list()) {
            AddItemInfo(wp, item);
        }
        wp.write(0);
        for (Item item : p.getInventory(InventoryType.CASH).list()) {
            AddItemInfo(wp, item);
        }
        wp.write(0); 
        Map<PlayerSkill, PlayerSkillEntry> skills = p.getSkills();
        wp.writeShort(skills.size());
        for (Map.Entry<PlayerSkill, PlayerSkillEntry> skill : skills.entrySet()) {
            wp.writeInt(skill.getKey().getId());
            wp.writeInt(skill.getValue().skillevel);
            if (skill.getKey().isFourthJob()) {
                wp.writeInt(skill.getValue().masterlevel);
            }
        }
        final List<PlayerCoolDownValueHolder> cd = p.getAllCooldowns();
        wp.writeShort(cd.size());
        for (final PlayerCoolDownValueHolder cooling : cd) {
            wp.writeInt(cooling.skillId);
            wp.writeShort((int) (cooling.length + cooling.startTime - System.currentTimeMillis()) / 1000);
        }
        
        wp.writeShort(p.getStartedQuestsSize());
        for (MapleQuestStatus q : p.getStartedQuests()) {
                wp.writeShort(q.getQuest().getId());
                wp.writeMapleAsciiString(q.getQuestData());
                if (q.getQuest().getInfoNumber() > 0) {
                    wp.writeShort(q.getQuest().getInfoNumber());
                    wp.writeMapleAsciiString(q.getQuestData());
                }
        }
        List<MapleQuestStatus> completed = p.getCompletedQuests();
        wp.writeShort(completed.size());
        for (MapleQuestStatus q : completed) {
            wp.writeShort(q.getQuest().getId());
            wp.writeLong(GetTime(q.getCompletionTime()));
        }
        AddRingInfo(wp, p);
        AddRocksInfo(wp, p);
        wp.writeInt(0);
    }
    
    public static void AddRingInfo(WritingPacket wp, Player p) {
        wp.writeShort(0);
        wp.writeShort(p.getCrushRings().size());
        for (ItemRing ring : p.getCrushRings()) {
            wp.writeInt(ring.getPartnerChrId());
            wp.writeAsciiString(StringUtil.getRightPaddedStr(ring.getPartnerName(), '\0', 13));
            wp.writeInt(ring.getRingId());
            wp.writeInt(0);
            wp.writeInt(ring.getPartnerRingId());
            wp.writeInt(0);
        }
        wp.writeShort(p.getFriendshipRings().size());
        for (ItemRing ring : p.getFriendshipRings()) {
            wp.writeInt(ring.getPartnerChrId());
            wp.writeAsciiString(StringUtil.getRightPaddedStr(ring.getPartnerName(), '\0', 13));
            wp.writeInt(ring.getRingId());
            wp.writeInt(0);
            wp.writeInt(ring.getPartnerRingId());
            wp.writeInt(0);
            wp.writeInt(ring.getItemId());
        }
        wp.writeShort(p.getWeddingRings().size());
        for (ItemRing ring : p.getWeddingRings()) {
            wp.writeInt(p.getPartnerId());
            wp.writeInt(p.getGender() == 0 ? p.getId() : ring.getPartnerChrId());
            wp.writeInt(p.getGender() == 0 ? ring.getPartnerChrId() : p.getId());
            wp.writeShort(3);
            wp.writeInt(ring.getItemId());
            wp.writeInt(ring.getItemId());
            wp.writeAsciiString(StringUtil.getRightPaddedStr(p.getGender() == 0 ? p.getName() : ring.getPartnerName(), '\0', 13));
            wp.writeAsciiString(StringUtil.getRightPaddedStr(p.getGender() == 0 ? ring.getPartnerName() : p.getName(), '\0', 13));
        }
    }
    
    public static final void AddRocksInfo(final WritingPacket wp, final Player p) {
        final int[] mapz = p.getRegRocks();
        for (int i = 0; i < 5; i++) { 
            wp.writeInt(mapz[i]);
        }
        final int[] map = p.getRocks();
        for (int i = 0; i < 10; i++) {  
            wp.writeInt(map[i]);
        }
    }
    
    public static OutPacket ItemMegaphone(String msg, boolean whisper, int channel, Item item) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        wp.write(8);
        wp.writeMapleAsciiString(msg);
        wp.write(channel - 1);
        wp.writeBool(whisper);
        if (item == null) {
            wp.writeBool(false);
        } else {
            AddItemInfo(wp, item);
        }
        return wp.getPacket();
    }
    
    public static OutPacket UpdateInventorySlot(InventoryType type, Item item) {
        return UpdateInventorySlot(type, item, false);
    }

    public static OutPacket UpdateInventorySlot(InventoryType type, Item item, boolean fromDrop) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        if (fromDrop) {
            wp.writeBool(true);
        } else {
            wp.writeBool(false);
        }
        wp.write(1);
        wp.write(PacketHeaders.INVENTORY_QUANTITY_UPDATE);
        wp.write(type.getType());
        wp.writeShort(item.getPosition()); 
        wp.writeShort(item.getQuantity());
        return wp.getPacket();
    }

    public static OutPacket MoveInventoryItem(InventoryType type, short src, short dst) {
        return MoveInventoryItem(type, src, dst, (byte) -1);
    }

    public static OutPacket MoveInventoryItem(InventoryType type, short src, short dst, short equipIndicator) {
        WritingPacket wp = new WritingPacket(equipIndicator == -1 ? 10 : 11);
        wp.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        wp.writeBool(true);
        wp.write(1);
        wp.write(PacketHeaders.INVENTORY_CHANGE_POSITION);
        wp.write(type.getType());
        wp.writeShort(src);
        wp.writeShort(dst);
        if (equipIndicator != -1) {
            wp.write(equipIndicator);
        }
        return wp.getPacket();
    }

    public static OutPacket MoveAndMergeInventoryItem(InventoryType type, short src, short dst, short total) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        wp.writeBool(true);
        wp.write(2);
        wp.write(PacketHeaders.INVENTORY_CLEAR_SLOT);
        wp.write(type.getType());
        wp.writeShort(src);
        wp.write(1); 
        wp.write(type.getType());
        wp.writeShort(dst);
        wp.writeShort(total);
        return wp.getPacket();
    }

    public static OutPacket MoveAndMergeWithRestInventoryItem(InventoryType type, short src, short dst, short srcQ, short dstQ) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        wp.writeBool(true);
        wp.write(2);
        wp.write(PacketHeaders.INVENTORY_QUANTITY_UPDATE);
        wp.write(type.getType());
        wp.writeShort(src);
        wp.writeShort(srcQ);
        wp.write(PacketHeaders.INVENTORY_QUANTITY_UPDATE);
        wp.write(type.getType());
        wp.writeShort(dst);
        wp.writeShort(dstQ);
        return wp.getPacket();
    }
    
    public static OutPacket ClearInventoryItem(InventoryType type, short slot, boolean fromDrop) {
        WritingPacket wp = new WritingPacket(slot >= 0 ? 8 : 9);
        wp.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        wp.write(fromDrop ? 1 : 0);
        wp.write(1);
        wp.write(PacketHeaders.INVENTORY_CLEAR_SLOT);
        wp.write(type.getType());
        wp.writeShort(slot);
        if (slot < 0) {
            wp.writeBool(true);
        }
        return wp.getPacket();
    }
    
    public static OutPacket ScrolledItem(Item scroll, Item item, boolean destroyed) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        wp.writeBool(true); 
        wp.write(destroyed ? 2 : 3);
        wp.write(scroll.getQuantity() > 0 ? 1 : 3);
        wp.write(InventoryType.USE.getType());
        wp.writeShort(scroll.getPosition());
        if (scroll.getQuantity() > 0) {
            wp.writeShort(scroll.getQuantity());
        }
        wp.write(3);
        if (!destroyed) {
            wp.write(InventoryType.EQUIP.getType());
            wp.writeShort(item.getPosition());
            wp.write(0);
        }
        wp.write(InventoryType.EQUIP.getType());
        wp.writeShort(item.getPosition());
        if (!destroyed) {
            AddItemInfo(wp, item, true, true);
        }
        wp.write(1);
        return wp.getPacket();
    }

    public static OutPacket GetScrollEffect(int charID, EquipScrollResult scrollSuccess, boolean legendarySpirit) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOW_SCROLL_EFFECT.getValue());
        wp.writeInt(charID);
        switch (scrollSuccess) {
            case SUCCESS:
                wp.writeShort(1);
                wp.writeShort(legendarySpirit ? 1 : 0);
                break;
            case FAIL:
                wp.writeShort(0);
                wp.writeShort(legendarySpirit ? 1 : 0);
                break;
            case CURSE:
                wp.write(0);
                wp.write(1);
                wp.writeShort(legendarySpirit ? 1 : 0);
                break;
            default:
                throw new IllegalArgumentException("effect in illegal range");
        }
        return wp.getPacket();
    }
    
    public static OutPacket useSkillBook(Player p, int skillid, int maxlevel, boolean canuse, boolean success) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.USE_SKILL_BOOK.getValue());
        wp.writeInt(p.getId()); 
        wp.writeBool(true);
        wp.writeInt(skillid);
        wp.writeInt(maxlevel);
        wp.writeBool(canuse);
        wp.writeBool(success);
        return wp.getPacket();
    }
    
    public static OutPacket CatchMonster(int mobID, int itemID, byte success) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.CATCH_MONSTER.getValue());
        wp.writeInt(mobID);
        wp.writeInt(itemID);
        wp.write(success);
        return wp.getPacket();
    }
    
    public static OutPacket SilverBoxOpened(int itemID) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SILVER_BOX_OPEN.getValue());
        wp.writeInt(itemID);
        return wp.getPacket();
    }
     /* 00 = /
     * 01 = You don't have enough in stock
     * 02 = You do not have enough mesos
     * 03 = Please check if your inventory is full or not
     * 05 = You don't have enough in stock
     * 06 = Due to an error, the trade did not happen
     * 07 = Due to an error, the trade did not happen
     * 08 = /
     * 0D = You need more items
     * 0E = CRASH; LENGTH NEEDS TO BE LONGER :O
     */
    public static OutPacket ConfirmShopTransaction(byte code) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
        wp.write(code);
        return wp.getPacket();
    }
    
    public static OutPacket ReportReply(byte type) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.REPORTREPLY.getValue());
        wp.write(type);
        return wp.getPacket();
    }
    
    private static void AddAttackBody(WritingPacket wp, int cid, int skill, int stance, int numAttackedAndDamage, int projecTile, Map<Integer, List<Integer>> damage, int speed) {
        wp.writeInt(cid);
        wp.write(numAttackedAndDamage);
        if (skill > 0) {
            wp.write(0xFF); 
            wp.writeInt(skill);
        } else {
            wp.write(0);
        }
        wp.write(0);
        wp.write(stance);
        wp.write(speed);
        wp.write(0x0A);
        wp.writeInt(projecTile);
        for (Integer oned : damage.keySet()) {
            List<Integer> onedList = damage.get(oned);
            if (onedList != null) {
                wp.writeInt(oned.intValue());
                wp.write(0xFF);
                if (skill == ChiefBandit.MesoExplosion) {
                    wp.write(onedList.size());
                }
                for (Integer eachd : onedList) {
                    wp.writeInt(eachd.intValue());
                }
            }
        }
    }
    
    public static OutPacket EnergyChargeAttack(int cID, int skill, int stance, int numAttackedAndDamage, Map<Integer, List<Integer>> damage, int speed) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ENERGY_CHARGE_ATTACK.getValue());
        AddAttackBody(wp, cID, skill, stance, numAttackedAndDamage, 0, damage, speed);
        return wp.getPacket();
    }
    
    public static OutPacket CloseRangeAttack(int cID, int skill, int stance, int numAttackedAndDamage, Map<Integer, List<Integer>> damage, int speed) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.CLOSE_RANGE_ATTACK.getValue());
        AddAttackBody(wp, cID, skill, stance, numAttackedAndDamage, 0, damage, speed);
        return wp.getPacket();
    }

    public static OutPacket RangedAttack(int cID, int skill, int stance, int numAttackedAndDamage, int projectile, Map<Integer, List<Integer>> damage, int speed) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.RANGED_ATTACK.getValue());
        AddAttackBody(wp, cID, skill, stance, numAttackedAndDamage, projectile, damage, speed);
        return wp.getPacket();
    }

    public static OutPacket MagicAttack(int cID, int skill, int stance, int numAttackedAndDamage, Map<Integer, List<Integer>> damage, int charge, int speed) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MAGIC_ATTACK.getValue());
        AddAttackBody(wp, cID, skill, stance, numAttackedAndDamage, 0, damage, speed);
        if (charge != -1) {
            wp.writeInt(charge);
        }
        return wp.getPacket();
    }
    
    public static OutPacket DamagePlayer(int skill, int monsterIDFrom, int cID, int damage) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.DAMAGE_PLAYER.getValue());
        wp.writeInt(cID);
        wp.write(skill);
        wp.writeInt(0);
        wp.writeInt(monsterIDFrom);
        wp.write(1);
        wp.write(0);
        wp.write(0); 
        wp.writeInt(damage);
        return wp.getPacket();
    }

    public static OutPacket DamagePlayer(int mobAttack, int monsterIDFrom, int cID, int damage, int fake, int direction, boolean pgmr, int pgmr_1, boolean is_pg, int oid, int pos_x, int pos_y) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.DAMAGE_PLAYER.getValue());
        wp.writeInt(cID);
        wp.write(mobAttack);
        if (mobAttack != -2) {
            wp.writeInt(damage);
            wp.writeInt(monsterIDFrom);
            wp.write(direction);
            wp.write(pgmr_1);
            if (pgmr) {
                wp.writeBool(is_pg);
                wp.writeInt(oid);
                wp.write(6);
                wp.writeShort(pos_x);
                wp.writeShort(pos_y);
                wp.write(0);
            }
            wp.write(0);
            wp.writeInt(damage);
            if (fake > 0) {
                wp.writeInt(fake);
            }
        } else {
            wp.writeInt(damage);
            wp.writeInt(damage);
        }
        return wp.getPacket();
    }
    
    public static OutPacket MovePlayer(int cID, List<LifeMovementFragment> moves) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MOVE_PLAYER.getValue());
        wp.writeInt(cID);
        wp.writeInt(0);
        HelpPackets.SerializeMovementList(wp, moves);
        return wp.getPacket();
    }

    public static OutPacket MoveSummon(int cID, int objectID, Point startPos, List<LifeMovementFragment> moves) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MOVE_SUMMON.getValue());
        wp.writeInt(cID);
        wp.writeInt(objectID);
        wp.writePos(startPos);
        HelpPackets.SerializeMovementList(wp, moves);
        return wp.getPacket();
    }
    
    public static OutPacket SkillCancel(Player from, int skillID) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.CANCEL_SKILL_EFFECT.getValue());
        wp.writeInt(from.getId());
        wp.writeInt(skillID);
        return wp.getPacket();
    }
    
    public static OutPacket GiveFameResponse(int mode, String charName, int newFame) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FAME_RESPONSE.getValue());
        wp.write(PlayersHeaders.FAME_OPERATION_RESPONSE_SUCCESS);
        wp.writeMapleAsciiString(charName);
        wp.write(mode);
        wp.writeShort(newFame);
        wp.writeShort(0);
        return wp.getPacket();
    }
    
    public static OutPacket GiveFameErrorResponse(int status) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FAME_RESPONSE.getValue());
        wp.write(status);
        return wp.getPacket();
    }

    public static OutPacket ReceiveFame(int mode, String charNameFrom) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FAME_RESPONSE.getValue());
        wp.write(PlayersHeaders.FAME_OPERATION_FAME_CHANGED);
        wp.writeMapleAsciiString(charNameFrom);
        wp.write(mode);
        return wp.getPacket();
    }
    
    public static OutPacket DamageSummon(int dwCharacterID, int objectId, int nDamage, int nAttackIdx, int dwMobTemplateID, boolean bLeft) {
	WritingPacket wp = new WritingPacket();
	wp.writeShort(SendPacketOpcode.DAMAGE_SUMMON.getValue());
	wp.writeInt(dwCharacterID);
	wp.writeInt(objectId);
	wp.write(nAttackIdx);
	wp.writeInt(dwMobTemplateID);
        wp.writeInt(nDamage);
        wp.writeBool(bLeft);
	return wp.getPacket();
    }
    
    public static OutPacket DamageSummon(int dwCharacterID, int dwSummonedID, int nAttackIdx, int nDamage, int dwMobTemplateID, int bLeft) { 
        WritingPacket wp = new WritingPacket(); 
        wp.writeShort(SendPacketOpcode.DAMAGE_SUMMON.getValue());
        wp.writeInt(dwCharacterID); 
        wp.writeInt(dwSummonedID); 
        wp.write(nAttackIdx); 
        wp.writeInt(nDamage); 
        if (nAttackIdx > -2) { 
            wp.writeInt(dwMobTemplateID); 
            wp.write(bLeft); 
        } 
        return wp.getPacket();
    } 
    
    public static OutPacket DamageSummon(Player p, int summon, int damage, byte action, int monsterIdFrom) {
	WritingPacket wp = new WritingPacket();
	wp.writeShort(SendPacketOpcode.DAMAGE_SUMMON.getValue());
	wp.writeInt(p.getId());
	wp.writeInt(summon);
	wp.write(action);
	wp.writeInt(damage);
	wp.writeInt(monsterIdFrom);
	wp.write(0);
	return wp.getPacket();
    }
    
    public static OutPacket AddInventorySlot(InventoryType type, Item item) {
	return AddInventorySlot(type, item, false);
    }
    
    public static OutPacket AddInventorySlot(InventoryType type, Item item, boolean fromDrop) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        wp.write(fromDrop? 1 : 0);
        wp.write(HexTool.getByteArrayFromHexString("01 00")); 
        wp.write(type.getType());
        wp.write(item.getPosition()); 
        AddItemInfo(wp, item, true, false);
        return wp.getPacket();
    }
    
    public static OutPacket SpawnNPC(MapleNPC npc) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPAWN_NPC.getValue());
        wp.writeInt(npc.getObjectId());
        wp.writeInt(npc.getId());
        wp.writeShort(npc.getPosition().x);
        wp.writeShort(npc.getCy());
        if (npc.getF() == 1) {
            wp.write(0);
        } else {
            wp.write(1);
        }
        wp.writeShort(npc.getFh());
        wp.writeShort(npc.getRx0());
        wp.writeShort(npc.getRx1());
        wp.write(1);
        return wp.getPacket();
    }
        
    public static OutPacket SpawnNPC(MapleNPC npc, boolean show) {
	WritingPacket wp = new WritingPacket();
	wp.writeShort(SendPacketOpcode.SPAWN_NPC.getValue());
	wp.writeInt(npc.getObjectId());
	wp.writeInt(npc.getId());
	wp.writeShort(npc.getPosition().x);
	wp.writeShort(npc.getCy());
	wp.write(npc.getF() == 1 ? 0 : 1);
	wp.writeShort(npc.getFh());
	wp.writeShort(npc.getRx0());
	wp.writeShort(npc.getRx1());
	wp.write(show ? 1 : 0);
	return wp.getPacket();
    }
    
    public static OutPacket SpawnNPCRequestController(MapleNPC npc, boolean MiniMap) {
	WritingPacket wp = new WritingPacket();
	wp.writeShort(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
	wp.write(1);
	wp.writeInt(npc.getObjectId());
	wp.writeInt(npc.getId());
	wp.writeShort(npc.getPosition().x);
	wp.writeShort(npc.getCy());
	wp.write(npc.getF() == 1 ? 0 : 1);
	wp.writeShort(npc.getFh());
	wp.writeShort(npc.getRx0());
	wp.writeShort(npc.getRx1());
	wp.write(MiniMap ? 1 : 0);
	return wp.getPacket();
    }
    
    public static OutPacket RemoveNPC(int oid) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.REMOVE_NPC.getValue());
        wp.writeInt(oid);
        return wp.getPacket();
    }
        
    public static OutPacket RemoveNPCController(int objectid) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
        wp.write(0);
        wp.writeInt(objectid);
        return wp.getPacket();
    }
    
    public static OutPacket ShowDashP(List<Pair<BuffStat, Integer>> statups, int x, int y, int duration) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_APPLY_STATUS_EFFECT.getValue());
        wp.writeLong(0);
        long mask = GetLongMask(statups);
        wp.writeLong(mask);
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
        wp.write(2);
        return wp.getPacket();
    }
    
    public static OutPacket GiveInfusion(int buffLength, int speed) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_APPLY_STATUS_EFFECT.getValue());
        wp.writeLong(0);
        wp.writeLong(BuffStat.MORPH.getValue());
        wp.writeShort(speed);
        wp.writeInt(0000000);
        wp.writeLong(0);
        wp.writeShort(buffLength);
        wp.writeShort(0);
        return wp.getPacket();
    }

    public static OutPacket GiveForeignInfusion(int cID, int speed, int duration) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_APPLY_STATUS_EFFECT.getValue());
        wp.writeInt(cID);
        wp.writeLong(0);
        wp.writeLong(BuffStat.MORPH.getValue());
        wp.writeShort(0);
        wp.writeInt(speed);
        wp.writeInt(0000000);
        wp.writeLong(0);
        wp.writeInt(duration);
        wp.writeShort(0);
        return wp.getPacket();
    }
    
    public static OutPacket ShowMonsterRiding(int cid, List<Pair<BuffStat, Integer>> statups, TamingMob mount) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_APPLY_STATUS_EFFECT.getValue());
        wp.writeInt(cid);
        long mask = GetLongMask(statups);
        wp.writeLong(0);
        wp.writeLong(mask);
        wp.writeShort(0);
        wp.writeInt(mount.getItemId());
        wp.writeInt(mount.getSkillId());
        wp.writeInt(0x2D4DFC2A);
        wp.writeShort(0); 
        return wp.getPacket();
    }
    
    public static OutPacket DropInventoryItem(InventoryType type, short src) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        wp.write(HexTool.getByteArrayFromHexString("01 01 03"));
        wp.write(type.getType());
        wp.writeShort(src);
        if (src < 0) {
            wp.write(1);
        }
        return wp.getPacket();
    }
    
    public static OutPacket DropInventoryItemUpdate(InventoryType type, Item item) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        wp.write(HexTool.getByteArrayFromHexString("01 01 01"));
        wp.write(type.getType());
        wp.writeShort(item.getPosition());
        wp.writeShort(item.getQuantity());
        return wp.getPacket();
    }
    
    public static OutPacket GetNPCShop(Client c, int sID, List<ShopItem> items) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.OPEN_NPC_SHOP.getValue());
        wp.writeInt(sID);
        wp.writeShort(items.size()); 
        for (ShopItem item : items) {
            wp.writeInt(item.getItemId());
            wp.writeInt(item.getPrice());
            if (!ii.isThrowingStar(item.getItemId()) && !ii.isBullet(item.getItemId())) {
                wp.writeShort(1); 
                wp.writeShort(item.getBuyable());
            } else {
                wp.writeShort(0);
                wp.writeInt(0);
                wp.writeShort(BitTools.doubleToShortBits(ii.getPrice(item.getItemId())));
                wp.writeShort(ii.getSlotMax(c, item.getItemId()));
            }
        }
       return wp.getPacket();
    }
    
    public static OutPacket DropMesoFromFieldObject(int amount, int itemOID, int dropperOID, int ownerID, Point dropFrom, Point dropTo, byte mod) {
        return DropItemFromFieldObjectInternal(amount, itemOID, dropperOID, ownerID, dropFrom, dropTo, mod, true);
    }

    public static OutPacket DropItemFromFieldObject(int itemID, int itemOID, int dropperOID, int ownerID, Point dropFrom, Point dropTo, byte mod) {
        return DropItemFromFieldObjectInternal(itemID, itemOID, dropperOID, ownerID, dropFrom, dropTo, mod, false);
    }
    
    public static OutPacket DropItemFromMapObject(FieldItem drop, Point dropfrom, Point dropto, byte mod) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
        wp.write(mod);
        wp.writeInt(drop.getObjectId()); 
        wp.write(drop.getMeso() > 0 ? 1 : 0); 
        wp.writeInt(drop.getItemId()); 
        wp.writeInt(drop.getOwnerId()); 
        wp.write(drop.getDropType());
        wp.writePos(dropto);

        //wp.writeInt(0); // mob
//
//        if (mod != 2) {
//            wp.writePos(dropfrom);
//            wp.writeShort(0);
//        }
//        if (drop.getMeso() == 0) {
//            HelpPackets.AddExpirationTime(wp, drop.getItem().getExpiration(), false);
//        }
//        wp.writeShort(drop.isPlayerDrop() ? 0 : 1); 
         if (mod != 2) {
            wp.writeInt(drop.getOwnerId());
            wp.writeShort(dropfrom.x);
            wp.writeShort(dropfrom.y);
        } else
            wp.writeInt(0);
        wp.write(0);
        if (mod != 2) {
            wp.write(0); 
            wp.write(1); 
        }
        boolean mesos = drop.getMeso() > 0;
        if (!mesos) {
            wp.write(ITEM_MAGIC);
            HelpPackets.AddExpirationTime(wp, System.currentTimeMillis(), false);
            wp.write(1); 
        }
        return wp.getPacket();
    }
    
    public static OutPacket DropItemFromFieldObjectInternal(int itemID, int itemOID, int dropperOID, int ownerID, Point dropFrom, Point dropTo, byte mod, boolean mesos) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
        wp.write(mod);
        wp.writeInt(itemOID);
        wp.write(mesos ? 1 : 0);
        wp.writeInt(itemID);
        wp.writeInt(ownerID); 
        wp.write(0);
        wp.writeShort(dropTo.x);
        wp.writeShort(dropTo.y);
        if (mod != 2) {
            wp.writeInt(ownerID);
            wp.writeShort(dropFrom.x);
            wp.writeShort(dropFrom.y);
        } else
            wp.writeInt(dropperOID);
        wp.write(0);
        if (mod != 2) {
            wp.write(0); 
            wp.write(1); 
        }
        if (!mesos) {
            wp.write(ITEM_MAGIC);
            HelpPackets.AddExpirationTime(wp, System.currentTimeMillis(), false);
            wp.write(1); 
        }
        return wp.getPacket();
    }
    
     public static OutPacket TriggerReactor(Reactor reactor, int stance) {
	WritingPacket wp = new WritingPacket();   
	wp.writeShort(SendPacketOpcode.REACTOR_HIT.getValue());
	wp.writeInt(reactor.getObjectId());
	wp.write(reactor.getState());
	wp.writePos(reactor.getPosition());
	wp.writeShort(stance);
	wp.writeBool(false);
	wp.write(5); 
	return wp.getPacket();
    }
	
    public static OutPacket DestroyReactor(Reactor reactor) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.REACTOR_DESTROY.getValue());
        wp.writeInt(reactor.getObjectId());
        wp.write(reactor.getState());
        wp.writePos(reactor.getPosition());
        return wp.getPacket();
    }
    
    public static OutPacket SpawnDoor(int objectID, Point pos, boolean town) {
        WritingPacket wp = new WritingPacket(11);
        wp.writeShort(SendPacketOpcode.SPAWN_DOOR.getValue());
        wp.writeBool(town);
        wp.writeInt(objectID);
        wp.writePos(pos);
        return wp.getPacket();
    }
    
    public static OutPacket RemoveDoor(int objectID, boolean town) {
        WritingPacket wp = new WritingPacket(10);
        if (town) {
            wp.writeShort(SendPacketOpcode.SPAWN_PORTAL.getValue());
            wp.writeInt(MapConstants.NULL_MAP);
            wp.writeInt(MapConstants.NULL_MAP);
        } else {
            wp.writeShort(SendPacketOpcode.REMOVE_DOOR.getValue());
            wp.write(0);
            wp.writeInt(objectID);
        }
        return wp.getPacket();
    }
    
    public static OutPacket SpawnPortal(int townId, int targetId, Point pos) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPAWN_PORTAL.getValue());
        wp.writeInt(townId);
        wp.writeInt(targetId);
        if (pos != null) {
            wp.writePos(pos);
        }
        return wp.getPacket();
    }
    
    public static OutPacket SpawnSpecialFieldObject(MapleSummon summon, boolean animated) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPAWN_SPECIAL_MAPOBJECT.getValue());
        wp.writeInt(summon.getOwner().getId());
        wp.writeInt(summon.getObjectId()); 
        wp.writeInt(summon.getSkill());
        wp.write(summon.getSkillLevel());
        wp.writePos(summon.getPosition());
        wp.write(summon.getStance());
        wp.writeShort(summon.getFh()); 
        wp.write(summon.getMovementType().getValue());
        wp.write(summon.isPuppet() ? 0 : 1);
        wp.writeBool(animated);
        return wp.getPacket();
    }
    
    public static OutPacket ShipEffect(boolean type) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHIP_EFFECT.getValue());
        wp.write(type ? (byte) 12 : (byte) 8);
        wp.write(type ? (byte) 6 : (byte) 2);
        //todo: short
        return wp.getPacket();
    }
    
    public static OutPacket SpawnReactor(Reactor reactor) {
	WritingPacket wp = new WritingPacket();
	wp.writeShort(SendPacketOpcode.REACTOR_SPAWN.getValue());
	wp.writeInt(reactor.getObjectId());
	wp.writeInt(reactor.getId());
	wp.write(reactor.getState());
	wp.writePos(reactor.getPosition());
        wp.writeBool(false);
	return wp.getPacket();
    }
    
    public static OutPacket SpawnMist(int objectID, int ownerCid, int skillId, Rectangle mistPosition, int level) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPAWN_MIST.getValue());
        wp.writeInt(objectID);
        wp.writeInt(objectID); 
        wp.writeInt(ownerCid); 
        wp.writeInt(skillId);
        wp.write(level); 
        wp.writeShort(8); 
        wp.writeInt(mistPosition.x);
        wp.writeInt(mistPosition.y); 
        wp.writeInt(mistPosition.x + mistPosition.width); 
        wp.writeInt(mistPosition.y + mistPosition.height);
        wp.writeInt(0);
        return wp.getPacket();
    }
        
    public static OutPacket SpawnMist(int objectID, int ownerCid, int skill, int level, MapleMist mist) {
       WritingPacket wp = new WritingPacket();
       wp.writeShort(SendPacketOpcode.SPAWN_MIST.getValue());
       wp.writeInt(objectID);
       wp.writeInt(mist.isMobMist() ? 0 : mist.isPoisonMist() ? 1 : mist.isRecoveryMist() ? 4 : 2); 
       wp.writeInt(ownerCid);
       wp.writeInt(skill);
       wp.write(level);
       wp.writeShort(mist.getSkillDelay()); 
       wp.writeInt(mist.getBox().x);
       wp.writeInt(mist.getBox().y);
       wp.writeInt(mist.getBox().x + mist.getBox().width);
       wp.writeInt(mist.getBox().y + mist.getBox().height);
       wp.writeInt(0);
       return wp.getPacket();
    }
	
    public static OutPacket SpawnMobMist(int objectID, int ownerCid, int skillId, Rectangle mistPosition, int level) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPAWN_MIST.getValue());
        wp.writeInt(objectID);
        wp.writeInt(objectID);
        wp.writeInt(ownerCid); 
        wp.writeInt(skillId);
        wp.write(level); 
        wp.writeShort(8); 
        wp.writeInt(mistPosition.x); 
        wp.writeInt(mistPosition.y);
        wp.writeInt(mistPosition.x + mistPosition.width);
        wp.writeInt(mistPosition.y + mistPosition.height); 
        wp.writeInt(1);
        return wp.getPacket();
    }

    public static OutPacket RemoveMist(int objectID) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.REMOVE_MIST.getValue());
        wp.writeInt(objectID);
        return wp.getPacket();
    }
    
    public static OutPacket SendTV(Player chr, List<String> messages, int type, Player partner) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SEND_TV.getValue()); 
        wp.write(partner != null ? 2 : 0);
        wp.write(type);
        AddCharLook(wp, chr, false);
        wp.writeMapleAsciiString(chr.getName());
        if (partner != null) {
            wp.writeMapleAsciiString(partner.getName());
        } else {
            wp.writeShort(0); 
        }
        for (int i = 0; i < messages.size(); i++) { 
            if (i == 4 && messages.get(4).length() > 15) {
                wp.writeMapleAsciiString(messages.get(4).substring(0, 15)); 
            } else {
                wp.writeMapleAsciiString(messages.get(i));
            }
        }
        wp.writeInt(1337); 
        if (partner != null) {
            AddCharLook(wp, partner, false);
        }
        return wp.getPacket();
    }  
     
    public static OutPacket EnableTV() {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ENABLE_TV.getValue()); 
        wp.writeInt(0);
        wp.write(0);
        return wp.getPacket();
    }

    public static OutPacket RemoveTV() {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.REMOVE_TV.getValue()); 
        return wp.getPacket();
    }
    
    public static OutPacket YellowChat(String msg) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.TIP_MESSAGE.getValue());
        wp.write(5);
        wp.writeMapleAsciiString(msg);
        return wp.getPacket();
    }  
    
    public static void SendUnkwnNote(String to, String msg, String from) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, to);
            ps.setString(2, from);
            ps.setString(3, msg);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }
    
    public static OutPacket ShowHide() {
        WritingPacket wp = new WritingPacket(4);
        wp.writeShort(SendPacketOpcode.GM.getValue());
        wp.write(16);
        wp.writeBool(true);
        return wp.getPacket();
    }

    public static OutPacket StopHide() {
        WritingPacket wp = new WritingPacket(4);
        wp.writeShort(SendPacketOpcode.GM.getValue());
        wp.write(16);
        wp.writeBool(false);
        return wp.getPacket();
    }

    public static OutPacket ArrangeStorage(byte slots, Collection<Item> items, boolean changed) {
        WritingPacket mplew = new WritingPacket();
        mplew.writeShort(SendPacketOpcode.NPC_STORAGE.getValue());
        mplew.write(0x0E);
        mplew.write(slots);
        mplew.write(0x7C);  
        mplew.writeZeroBytes(10);
        mplew.write(items.size());
        for (Item item : items) {
            AddItemInfo(mplew, item, true, true);
        }
        mplew.write(0);
        return mplew.getPacket();
    }
    
    /**
     * Error message to the client if a user cannot warp to another area.
     * @param type Message to be sent. Possible values :<br>
     *             0x01 (1) - You cannot move that channel. Please try again later.
     *             0x02 (2) - You cannot go into the cash shop. Please try again later.
     *             0x03 (3) - The Item-Trading shop is currently unavailable, please try again later.
     *             0x04 (4) - You cannot go into the trade shop, due to the limitation of user count.
     *             0x05 (5) - You do not meet the minimum level requirement to access the Trade Shop.
     * @return 
     */

    public static OutPacket ServerMigrateFailed(byte msg) {
        WritingPacket mplew = new WritingPacket();
        mplew.writeShort(SendPacketOpcode.BLOCK_MIGRATE.getValue());
        mplew.write(msg);
        return mplew.getPacket();
    }

    public static OutPacket GiveDash(List<Pair<BuffStat, Integer>> statups, int x, int y, int duration) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_APPLY_STATUS_EFFECT.getValue());
        wp.writeLong(0);
        long mask = GetLongMask(statups);
        wp.writeLong(mask);
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
        wp.write(2);
        return wp.getPacket();
    }

   public static OutPacket ShowSpeedInfusion(int cid, int skillid, int time, List<Pair<BuffStat, Integer>> statups) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_APPLY_STATUS_EFFECT.getValue());
        wp.writeInt(cid);
        long mask = GetLongMask(statups);
        wp.writeLong(mask);
        wp.writeLong(0);
        wp.writeShort(0);
        wp.writeInt(statups.get(0).getRight());
        wp.writeInt(skillid);
        wp.writeInt(0);
        wp.writeInt(0);
        wp.writeShort(0);
        wp.writeShort(time);
        wp.writeShort(0);
        return wp.getPacket();
    }

    public static OutPacket UpdateBattleShipHP(int chr, int hp) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOW_MONSTER_HP.getValue());
        wp.writeInt(chr);
        wp.write(hp);
        return wp.getPacket();
    }

    public static OutPacket PortalBlocked(int message) {
        WritingPacket wp = new WritingPacket(3);
        wp.writeShort(SendPacketOpcode.BLOCK_PORTAL.getValue());
        wp.write(message);
        return wp.getPacket();
    }
    
    public static OutPacket GetTrockRefresh(Player p, boolean vip, boolean delete) {
        WritingPacket wp = new WritingPacket();

        wp.writeShort(SendPacketOpcode.TROCK_LOCATIONS.getValue());
        wp.write(delete ? 2 : 3);
        wp.write(vip ? 1 : 0);
        if (vip) {
            int[] map = p.getRocks();
            for (int i = 0; i < 10; i++) {
                wp.writeInt(map[i]);
            }
        } else {
            int[] map = p.getRegRocks();
            for (int i = 0; i < 5; i++) {
                wp.writeInt(map[i]);
            }
        }
        return wp.getPacket();
    }

    public static OutPacket CancelMonsterStatus(int oid, Map<MonsterStatus, Integer> stats) {
        WritingPacket mplew = new WritingPacket();
        mplew.writeShort(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        int mask = 0;
        for (MonsterStatus stat : stats.keySet()) {
            mask |= stat.getValue();
        }
        mplew.writeInt(mask);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static OutPacket SendPolice(String text) {
        final WritingPacket mplew = new WritingPacket();
        mplew.writeShort(SendPacketOpcode.GM_POLICE.getValue());
        mplew.writeMapleAsciiString(text);
        return mplew.getPacket();
    }

    public static OutPacket FinishedGather(int type) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FINISH_SORT.getValue());
        wp.write(0);
        wp.write(type);
        return wp.getPacket();
    }

    public static OutPacket FinishedSort(int type) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FINISH_SORT2.getValue());
        wp.write(0);
        wp.write(type);
        return wp.getPacket();
    }
    
    public static OutPacket RemoveItemFromDuey(boolean remove, int Package) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.DUEY.getValue());
        wp.write(0x17);
        wp.writeInt(Package);
        wp.write(remove ? 3 : 4);
        return wp.getPacket();
    }

    public static OutPacket SendDueyNotification(boolean quickDelivery) {
        final WritingPacket mplew = new WritingPacket();
        mplew.writeShort(SendPacketOpcode.DUEY.getValue());
        mplew.write(0x1B);
       // mplew.writeBool(quickDelivery); 

        return mplew.getPacket();
    }
    
    public static OutPacket SendDueyMSG(byte operation) {
        return SendDuey(operation, null);
    }
    
    public static OutPacket SendDuey(byte operation, List<DueyPackages> packages) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.DUEY.getValue());
        wp.write(operation);
        if (operation == 8) {
            wp.write(0);
            wp.write(packages.size());
            for (DueyPackages dp : packages) {
                wp.writeInt(dp.getPackageId());
                wp.writeAsciiString(dp.getSender());
                for (int i = dp.getSender().length(); i < 13; i++) {
                    wp.write(0);
                }
                wp.writeInt(dp.getMesos());
                wp.writeLong(GetTime(dp.sentTimeInMilliseconds()));
                wp.writeLong(0); 
                for (int i = 0; i < 48; i++) {
                    wp.writeInt(Randomizer.nextInt(Integer.MAX_VALUE));
                }
                wp.writeInt(0);
                wp.write(0);
                if (dp.getItem() != null) {
                    wp.write(1);
                    AddItemInfo(wp, dp.getItem(), true, true);
                } else {
                    wp.write(0);
                }
            }
            wp.write(0);
        }
        return wp.getPacket();
    }

    public static OutPacket UpdateInventorySlotLimit(int type, int newLimit) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_INVENTORY_CAPACITY.getValue());
        wp.write(type);
        wp.write(newLimit);
        return wp.getPacket();
    }

    public static OutPacket ShowOwnRecovery(byte heal) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_VISUAL_EFFECT.getValue());
        wp.write(0x0A);
        wp.write(heal);
        return wp.getPacket();
    }

    public static OutPacket ShowRecovery(int cid, byte amount) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_VISUAL_EFFECT.getValue());
        wp.writeInt(cid);
        wp.write(0x0A);
        wp.write(amount);
        return wp.getPacket();
    }

    public static OutPacket GetRockPaperScissorsMode(byte mode, int mesos, int selection, int answer) {
        WritingPacket wp = new WritingPacket();

        wp.writeShort(SendPacketOpcode.RPS_GAME.getValue());
        wp.write(mode);
        switch (mode) {
            case 6: {
                if (mesos != -1) {
                    wp.writeInt(mesos);
                }
                break;
            }
            case 8: {
                wp.writeInt(9000019);
                break;
            }
            case 11: { 
                wp.write(selection);
                wp.write(answer); 
                break;
            }
        }
        return wp.getPacket();
    }  

    public static OutPacket ThrowGrenade(int cid, Point p, int keyDown, int skillId, int skillLevel) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THROW_GRENADE.getValue());
        wp.writeInt(cid);
        wp.writeInt(p.x);
        wp.writeInt(p.y);
        wp.writeInt(keyDown);
        wp.writeInt(skillId);
        wp.writeInt(skillLevel);
        return wp.getPacket();
    }

    public static final OutPacket GetSpeedQuiz(int npc, byte type, int oid, int points, int questionNo, int time) {
        WritingPacket wp = new WritingPacket();

        wp.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        wp.write(4);
        wp.writeInt(npc);
        wp.writeShort(6);
        wp.write(0); 
        wp.writeInt(type); 
        wp.writeInt(oid); 
        wp.writeInt(points); 
        wp.writeInt(questionNo); 
        wp.writeInt(time);

        return wp.getPacket();
    }  
    
    public static OutPacket SetNPCScriptable(int npcId) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SET_NPC_SCRIPTABLE.getValue());
        wp.write(1); // following structure is repeated n times
        wp.writeInt(npcId);
        wp.writeMapleAsciiString("Talk to npcid" + npcId);
        wp.writeInt(0); // start time
        wp.writeInt(Integer.MAX_VALUE); // end time
        return wp.getPacket();
    }
}
