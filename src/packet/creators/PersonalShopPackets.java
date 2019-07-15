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
import server.minirooms.PlayerShop;
import server.minirooms.PlayerShopItem;
import server.minirooms.components.SoldItem;

public class PersonalShopPackets {
    
    public static void AddAnnounceBox(WritingPacket wp, PlayerShop shop, int availability) {
        wp.write(4);
        wp.writeInt(shop.getObjectId());
        wp.writeMapleAsciiString(shop.getDescription());
        wp.write(0);
        wp.write(0);
        wp.write(1);
        wp.write(availability);
        wp.writeBool(false);
    }
    
    public static OutPacket AddCharBox(Player c, int type) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        wp.writeInt(c.getId());
        AddAnnounceBox(wp, c.getPlayerShop(), type);
        return wp.getPacket();
    }
    
    public static OutPacket RemoveCharBox(Player p) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        wp.writeInt(p.getId());
        wp.write(0);
        return wp.getPacket();
    }
    
    public static OutPacket GetPlayerShopRemoveVisitor(int slot) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_EXIT);
        if (slot != 0) {
            wp.writeShort(slot);
        }
        return wp.getPacket();
    }
    
    public static OutPacket ShopErrorMessage(int error, int type) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_EXIT);
        wp.write(type);
        wp.write(error);
        return wp.getPacket();
    }
    
    public static OutPacket ShopChat(String message, int slot) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_CHAT);
        wp.write(PlayerInteractionHeaders.ACT_CHAT_THING);
        wp.write(slot);
        wp.writeMapleAsciiString(message);
        return wp.getPacket();
    }
    
    public static OutPacket GetPlayerShopNewVisitor(Player c, int slot) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_VISIT);
        wp.write(slot);
        PacketCreator.AddCharLook(wp, c, false);
        wp.writeMapleAsciiString(c.getName());
        return wp.getPacket();
    }
    
    public static OutPacket GetPlayerShop(PlayerShop shop, boolean owner) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_JOIN);
        wp.write(4);
        wp.write(4);
        wp.write(owner ? 0 : 1);
        
         if (owner) {
            List<SoldItem> sold = shop.getSold();
            wp.write(sold.size());
            for (SoldItem s : sold) {
                wp.writeInt(s.getItemId());
                wp.writeShort(s.getQuantity());
                wp.writeInt(s.getMesos());
                wp.writeMapleAsciiString(s.getBuyer());
            }
        } else {
            wp.write(0);
        }

        PacketCreator.AddCharLook(wp, shop.getOwner(), false);
        wp.writeMapleAsciiString(shop.getOwner().getName());
        
        Player visitors[] = shop.getVisitors();
        for (int i = 0; i < 3; i++) {
            if(visitors[i] != null) {
                wp.write(i + 1);
                PacketCreator.AddCharLook(wp, visitors[i], false);
                wp.writeMapleAsciiString(visitors[i].getName());
            }
        }
        
        wp.write(0xFF);
        wp.writeMapleAsciiString(shop.getDescription());
        List<PlayerShopItem> items = shop.getItems();
        wp.write(0x10);
        wp.write(items.size());
        for (PlayerShopItem item : items) {
            wp.writeShort(item.getBundles());
            wp.writeShort(item.getItem().getQuantity());
            wp.writeInt(item.getPrice());
            PacketCreator.AddItemInfo(wp, item.getItem(), true, true);
        }
        return wp.getPacket();
    }
     
    public static OutPacket ShopItemUpdate(PlayerShop shop) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(PlayerInteractionHeaders.ACT_MERCHANT_ITEM_UPDATE);
        wp.write(shop.getItems().size());
        for (PlayerShopItem item : shop.getItems()) {
            wp.writeShort(item.getBundles());
            wp.writeShort(item.getItem().getQuantity());
            wp.writeInt(item.getPrice());
            PacketCreator.AddItemInfo(wp, item.getItem(), true, true);
        }
        return wp.getPacket();
    }

    public static OutPacket GetPlayerShopOwnerUpdate(SoldItem item, int position) {
        WritingPacket mplew = new WritingPacket();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHeaders.ACT_SHOP_ITEM_UPDATE);
        mplew.write(position);
        mplew.writeShort(item.getQuantity());
        mplew.writeMapleAsciiString(item.getBuyer());
        return mplew.getPacket();
    }
}
