/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packet.creators;

import client.player.Player;
import client.player.PlayerStat;
import client.player.inventory.ItemPet;
import java.util.List;
import static packet.creators.PacketCreator.GetKoreanTimestamp;
import packet.opcode.SendPacketOpcode;
import packet.transfer.write.OutPacket;
import packet.transfer.write.WritingPacket;
import server.movement.LifeMovementFragment;
import tools.HexTool;

public class PetPackets {
    
    public static byte 
        
        PET_LVL_UP = 4, 
        INVENTORY_CLEAR_SLOT = 3,
        INVENTORY_STAT_UPDATE = 0;

    public static void AddPetInfo(final WritingPacket wp, ItemPet pet, boolean showpet) {
        wp.write(1);
        if (showpet) {
            wp.write(1);
        }
        wp.writeInt(pet.getPetItemId());
        wp.writeMapleAsciiString(pet.getName());
        wp.writeLong(pet.getUniqueId());
        wp.writePos(pet.getPosition());
        wp.write(pet.getStance());
        wp.writeInt(pet.getFoothold());
    }
    
    public static OutPacket updatePet(ItemPet pet, boolean alive) {
        WritingPacket mplew = new WritingPacket();

        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(0);
        mplew.write(2);
        
        mplew.write(3);
        mplew.write(5);
        mplew.write(pet.getInventoryPosition());
        mplew.writeShort(0);
        mplew.write(5);
        
        mplew.write(pet.getInventoryPosition());
        mplew.write(0);
        mplew.write(3);
        mplew.writeInt(pet.getPetItemId());
        mplew.write(1);
        mplew.writeInt(pet.getUniqueId());
        mplew.writeInt(0);
        mplew.write(HexTool.getByteArrayFromHexString("00 40 6f e5 0f e7 17 02"));
        String petname = pet.getName();
        if (petname.length() > 13) {
            petname = petname.substring(0, 13);
        }
        mplew.writeAsciiString(petname);
        for (int i = petname.length(); i < 13; i++) {
            mplew.write(0);
        }
        mplew.write(pet.getLevel());
        mplew.writeShort(pet.getCloseness());
        mplew.write(pet.getFullness());
        if (alive) {
            mplew.writeLong(GetKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
            mplew.writeInt(0);
        } else {
            mplew.write(0);
            mplew.write(PacketCreator.ITEM_MAGIC);
            mplew.write(HexTool.getByteArrayFromHexString("bb 46 e6 17 02 00 00 00 00"));
        }

        return mplew.getPacket();
    }
    
    public static OutPacket ShowPet(Player chr, ItemPet pet, boolean remove) {
        return ShowPet(chr, pet, remove, false);
    }

    public static OutPacket ShowPet(Player p, ItemPet pet, boolean remove, boolean hunger) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPAWN_PET.getValue());
        wp.writeInt(p.getId());
        wp.write(p.getPetIndex(pet));
        if (remove) {
            wp.write(0);
            wp.writeBool(hunger);
        } else {
           AddPetInfo(wp, pet, true);
        }
        return wp.getPacket();
    }
    
    public static OutPacket RemovePet(int owner, byte slot, byte message) {
        WritingPacket wp = new WritingPacket(9);
        wp.writeShort(SendPacketOpcode.SPAWN_PET.getValue());
        wp.writeInt(owner);
        wp.write(slot);
        wp.writeBool(false);
        wp.write(message);
        return wp.getPacket();
    }
    
    public static OutPacket MovePet(int cid, int pid, int slot, List<LifeMovementFragment> moves) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MOVE_PET.getValue());
        wp.writeInt(cid);
        wp.write(slot);
        wp.writeInt(pid);
        HelpPackets.SerializeMovementList(wp, moves);
        return wp.getPacket();
    }
    
    public static OutPacket PetChat(int cid, byte nType, byte nAction, String text, int slot, boolean hasRing) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PET_CHAT.getValue());
        wp.writeInt(cid);
        wp.write(slot);
        wp.write(nType);
        wp.write(nAction);
        wp.writeMapleAsciiString(text);
        wp.writeBool(hasRing);
        return wp.getPacket();
    }
    
    public static OutPacket CommandPetResponse(int cid, byte command, int slot, boolean success, boolean food) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PET_RESPONSE.getValue());
        wp.writeInt(cid);
        wp.write(slot);
        if (!food) {
            wp.write(0);
        }
        wp.write(command);
        wp.writeBool(success);
        wp.write(0);
        return wp.getPacket();
    }
    
    public static OutPacket ShowOwnPetLevelUp(int petSlot) { 
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIRST_PERSON_VISUAL_EFFECT.getValue());
        wp.write(0x4);
        wp.write(0);
        wp.write(petSlot);
        return wp.getPacket();
    }
    
    public static OutPacket ShowPetLevelUp(Player p, int petSlot) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.THIRD_PERSON_VISUAL_EFFECT.getValue());
        wp.writeInt(p.getId());
        wp.write(0x4);
        wp.write(0);
        wp.write(petSlot);
        return wp.getPacket();
    }
    
    public static OutPacket ChangePetName(Player p, String newname, int slot, boolean hasLabelRing) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PET_NAMECHANGE.getValue());
        wp.writeInt(p.getId());
        wp.write(slot);
        wp.writeMapleAsciiString(newname);
        wp.writeBool(hasLabelRing);
        return wp.getPacket();
    }
    
    public static OutPacket PetStatUpdate(Player p) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_STATS.getValue());
        wp.write(0);
        wp.writeInt(PlayerStat.PET.getValue());
        byte count = 0;
        for (final ItemPet pet : p.getPets()) {
            if (pet.getSummoned()) {
                wp.writeLong(pet.getUniqueId());
                count++;
            }
        }
        while (count < 3) {
            wp.writeZeroBytes(8);
            count++;
        }
        wp.write(0);
        return wp.getPacket();
    }
    
    public static OutPacket AutoHpPot(int itemId) {
        WritingPacket wp = new WritingPacket(6);
        wp.writeShort(SendPacketOpcode.PET_AUTO_HP_POT.getValue());
        wp.writeInt(itemId);
        return wp.getPacket();
    }

    public static OutPacket AutoMpPot(int itemId) {
        WritingPacket wp = new WritingPacket(6);
        wp.writeShort(SendPacketOpcode.PET_AUTO_MP_POT.getValue());
        wp.writeInt(itemId);
        return wp.getPacket();
    }
    
    public static final OutPacket EmptyStatUpdate() {
        return PacketCreator.EnableActions();
    } 

    public static OutPacket PetExceptionListResult(Player p, ItemPet pet) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PET_ITEM_IGNORE.getValue());
        wp.writeInt(p.getId());
        wp.write(p.getPetIndex(pet));
        wp.writeLong(pet.getUniqueId());
        wp.write(pet.getExceptionList().size());
        for (int itemId : pet.getExceptionList()) {
            wp.writeInt(itemId);
        }
        return wp.getPacket();
    }
}
