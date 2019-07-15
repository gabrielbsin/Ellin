/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package packet.creators;

import client.player.Player;
import handling.channel.handler.ChannelHeaders.PlayerInteractionHeaders;
import java.util.List;
import packet.opcode.SendPacketOpcode;
import packet.transfer.write.OutPacket;
import packet.transfer.write.WritingPacket;
import server.minirooms.Merchant;
import server.minirooms.PlayerShopItem;
import server.minirooms.components.SoldItem;
import tools.HexTool;
import tools.Pair;

public class MerchantPackets {
    
    public static OutPacket MerchantVisitorAdd(Player p, int slot) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_VISIT);
        wp.write(slot);
        PacketCreator.AddCharLook(wp, p, false);
        wp.writeMapleAsciiString(p.getName());
        return wp.getPacket();
    }
    
    public static OutPacket MerchantMaintenanceMessage() {
        WritingPacket wp = new WritingPacket(5);
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_JOIN);
        wp.write(0x00);
        wp.write(0x10);
        return wp.getPacket();
    }
    
    public static OutPacket MerchantVisitorLeave(int slot) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_EXIT);
        if (slot != 0) {
            wp.write(slot);
        }
        return wp.getPacket();
    }
    
    public static OutPacket MerchantOwnerLeave() {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_REAL_CLOSE_MERCHANT);
        wp.write(0);
        return wp.getPacket();
    }
    
    public static OutPacket MerchantLeave(int error, int type) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_EXIT);
        wp.write(error);
        wp.write(type);
        return wp.getPacket();
    }
    
    public static OutPacket HiredMerchantForceLeaveOne() {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_EXIT);
        wp.write(0);
        wp.write(0x10);
        return wp.getPacket();
    }

    public static OutPacket HiredMerchantForceLeaveTwo() {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_EXIT);
        wp.write(0x01);
        wp.write(0x0D);
        return wp.getPacket();
    }
    
    public static OutPacket MerchantSpawn(Merchant miniRoom, int miniRoomType) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPAWN_HIRED_MERCHANT.getValue());
        wp.writeInt(miniRoom.getOwnerId());
        wp.writeInt(miniRoom.getItemId());
        wp.writePos(miniRoom.getPosition());
        wp.writeShort(miniRoom.getFootHold());
        wp.writeMapleAsciiString(miniRoom.getOwner());
        wp.write(miniRoomType);
        if (miniRoomType == 5) {
            updateMerchatBalloon(wp, miniRoom);
        }
        return wp.getPacket();
    }
    
    public static OutPacket UpdateHiredMerchantBalloon(Merchant miniRoom, int miniRoomType) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_HIRED_MERCHANT.getValue());
        wp.writeInt(miniRoom.getOwnerId());
        wp.write(miniRoomType);
        if (miniRoomType == 5) {
            updateMerchatBalloon(wp, miniRoom);
        }   
        return wp.getPacket();
    }
    
    public static void updateMerchatBalloon(WritingPacket wp, Merchant miniRoom) {
        wp.writeInt(miniRoom.getObjectId()); 
        wp.writeMapleAsciiString(miniRoom.getDescription()); 
        wp.write(miniRoom.getItemId() % 10);
        wp.write(miniRoom.getSize()); 
        wp.write(miniRoom.getMaxSize());
    }
    
    public static OutPacket GetMerchant(Player p, Merchant hm, boolean firstTime) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue()); 
        wp.write(HexTool.getByteArrayFromHexString("05 05 04"));
        wp.write(hm.isOwner(p) ? 0 : 1);
        wp.write(0);
        wp.writeInt(hm.getItemId());
        wp.writeMapleAsciiString("Hired Merchant");
        for (int i = 0; i < 3; i++) {
            if (hm.getVisitors()[i] != null) {
                wp.write(i + 1);
                PacketCreator.AddCharLook(wp, hm.getVisitors()[i], false);
                wp.writeMapleAsciiString(hm.getVisitors()[i].getName());
            }
        }
        wp.write(-1);
        if (hm.isOwner(p)) {
            List<Pair<String, Byte>> msgList = hm.getMessages();
            wp.writeShort(hm.getMessages().size());
            for (int i = 0; i < hm.getMessages().size(); i++) {
                wp.writeMapleAsciiString(msgList.get(i).getLeft());
                wp.write(msgList.get(i).getRight());
            }
        } else {
                wp.writeShort(0);
        }
        wp.writeMapleAsciiString(hm.getOwner());
        if (hm.isOwner(p)) {
            wp.writeInt(hm.getTimeLeft());
            wp.write(firstTime ? 1 : 0);
            List<SoldItem> sold = hm.getSold();
            wp.write(sold.size());
            for (SoldItem s : sold) {
                wp.writeInt(s.getItemId());
                wp.writeShort(s.getQuantity()); 
                wp.writeInt(s.getMesos());
                wp.writeMapleAsciiString(s.getBuyer()); 
            }
            wp.writeInt(p.getMerchantMeso());
        }
        wp.writeMapleAsciiString(hm.getDescription());
        wp.write(0x10);
        wp.writeInt(p.getMeso());
        wp.write(hm.getItems().size());
        if (hm.getItems().isEmpty()) {
            wp.write(0);
        } else {
            for (PlayerShopItem item : hm.getItems()) {
                wp.writeShort(item.getBundles());
                wp.writeShort(item.getItem().getQuantity());
                wp.writeInt(item.getPrice());
                PacketCreator.AddItemInfo(wp, item.getItem(), true, true);
            }
        }
        return wp.getPacket();
    }
    
    public static OutPacket MerchantChat(String message, int slot) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_CHAT);
        wp.write(PlayerInteractionHeaders.ACT_CHAT_THING);
        wp.write(slot);
        wp.writeMapleAsciiString(message);
        return wp.getPacket();
    }
    
    public static OutPacket MerchantDestroy(int id) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.DESTROY_HIRED_MERCHANT.getValue());
        wp.writeInt(id);
        return wp.getPacket();
    }
  
    public static OutPacket UpdateMerchant(Merchant hm, Player chr) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_MERCHANT_ITEM_UPDATE);
        wp.writeInt(chr.getMeso());
        wp.write(hm.getItems().size());
        for (PlayerShopItem item : hm.getItems()) {
            wp.writeShort(item.getBundles());
            wp.writeShort(item.getItem().getQuantity());
            wp.writeInt(item.getPrice());
            PacketCreator.AddItemInfo(wp, item.getItem(), true, true);
        }
        return wp.getPacket();
    }
}
