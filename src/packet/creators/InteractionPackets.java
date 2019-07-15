/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packet.creators;

import client.Client;
import client.player.Player;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemFactory;
import constants.ItemConstants;
import handling.channel.handler.ChannelHeaders;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import packet.opcode.SendPacketOpcode;
import packet.transfer.write.OutPacket;
import packet.transfer.write.WritingPacket;
import server.itens.Trade;
import server.maps.object.AbstractMapleFieldObject;
import server.minirooms.Minigame;
import server.minirooms.Merchant;
import server.minirooms.PlayerShop;
import server.minirooms.PlayerShopItem;
import tools.HexTool;
import tools.Pair;

public class InteractionPackets {
    
  
    public static OutPacket GetHiredMerchant(Client c, Minigame miniGame, String description) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(HexTool.getByteArrayFromHexString("05 05 04 00 00 71 C0 4C 00"));
        wp.writeMapleAsciiString(description);
        wp.write(0xFF);
        wp.write(0);
        wp.write(0);
        wp.writeMapleAsciiString(c.getPlayer().getName());
        wp.write(HexTool.getByteArrayFromHexString("1F 7E 00 00 00 00 00 00 00 00 03 00 31 32 33 10 00 00 00 00 01 01 00 01 00 7B 00 00 00 02 52 8C 1E 00 00 00 80 05 BB 46 E6 17 02 01 00 00 00 00 00"));
        return wp.getPacket();
    }
    
    
    
    public static OutPacket DestroyHiredMerchant(int id) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.DESTROY_HIRED_MERCHANT.getValue());
        wp.writeInt(id);
        return wp.getPacket();
    }
    
    public static OutPacket ShopVisitorAdd(Player p, int slot) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(0x04);
        wp.write(slot);
        PacketCreator.AddCharLook(wp, p, false);
        wp.writeMapleAsciiString(p.getName());
        return wp.getPacket();
    }

    public static OutPacket ShopVisitorLeave(int slot) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(0x0A);
        wp.write(slot);
        return wp.getPacket();
    }
    
    public static OutPacket RetrieveFirstMessage() {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOW_HIRED_MERCHANT_AGREEMENT.getValue());
        wp.write(0x09);
        return wp.getPacket();
    }
    
    public static OutPacket HiredMerchantBox() {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(0x2F);
        wp.write(0x07);
        return wp.getPacket();
    }
    
    public static OutPacket FredrickMessageSend(byte operation) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FREDRICK_MESSAGE.getValue());
        wp.write(operation);
        return wp.getPacket();
    }
    
    public static OutPacket GetFredrick(Player p, int op) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FREDRICK.getValue());
        wp.write(op);
        switch (op) {
            case 0x23:
                try {
                    List<Pair<Item, InventoryType>> items = ItemFactory.MERCHANT.loadItems(p.getId(), false);
                    long mask = 0;
                    wp.writeInt(9030000);//m_dwNpcTemplateID        
                    wp.write(items.size()); //m_nSlotCount, going to make it the item array size for now
                    int mesos = p.getMerchantMeso();
                    if (mesos > 0) {
                        mask |= 2;
                    }
                    if (!items.isEmpty()) {
                        mask |= 4;
                    }
                    wp.writeLong(mask);
                    if ((mask & 2) != 0) {
                        wp.writeInt(p.getMerchantMeso());
                    }
                    if ((mask & 4) != 0) {
                        wp.write(items.size());
                        items.stream().forEach((f) -> {
                            PacketCreator.AddItemInfo(wp, f.getLeft(), true, true);
                        });
                    }
                } catch (SQLException e) {
                }    
                break;
            case 0x24:
                break;
            case 0x26:
            	wp.writeInt(0); //dwStoreBankNpcTemplateID
            	wp.writeInt(0); //mapId
            	wp.write(0); //nIdx  for channel
            	break;
        }
//        wp.writeInt(9030000); 
//        wp.write(16); 
//        wp.writeLong(126L);
//        wp.writeLong(p.getMerchantMeso());
//        try {
//            List<Pair<Item, InventoryType>> items = ItemFactory.MERCHANT.loadItems(p.getId(), false);
//            wp.write(items.size());
//            for (int i = 0; i < items.size(); i++) {
//                PacketCreator.AddItemInfo(wp, items.get(i).getLeft(), true, true);
//            }
//        } catch (SQLException e) {
//        }
//        wp.write(new byte[3]);
        return wp.getPacket();
    }
    
    public static OutPacket GetFredrick(byte op) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FREDRICK.getValue());
        wp.write(op);
        switch (op) {
            case 0x24:
                wp.write0(8);
                break;
            default:
                wp.write(0);
                break;
        }
        return wp.getPacket();
    }
    
    public static OutPacket HiredMerchantForceLeave2() {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(0x0A);
        wp.write(0);
        wp.write(0x10);
        return wp.getPacket();
    }

    public static OutPacket HiredMerchantForceLeave1() {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(0x0A);
        wp.write(0x01);
        wp.write(0x0D);
        return wp.getPacket();
    }
    
    public static OutPacket ShopErrorMessage(int error, int type) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(0x0A);
        wp.write(type);
        wp.write(error);
        return wp.getPacket();
    }
    
    public static OutPacket ShopChat(String message, int slot) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(0x06);
        wp.write(8);
        wp.write(slot);
        wp.writeMapleAsciiString(message);
        return wp.getPacket();
    }
 
    public static OutPacket GetMiniBoxFull() {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.writeShort(5);
        wp.write(2);
        return wp.getPacket();
    }
    
    public static OutPacket GetTradeInvite(Player p) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(HexTool.getByteArrayFromHexString("02 03"));
        wp.writeMapleAsciiString(p.getName());
        wp.write(HexTool.getByteArrayFromHexString("B7 50 00 00"));
        return wp.getPacket();
    }

    public static OutPacket GetTradeMesoSet(byte number, int meso) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(0xF);
        wp.write(number);
        wp.writeInt(meso);
        return wp.getPacket();
    }

    public static OutPacket GetTradeItemAdd(byte number, Item item) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(0xE);
        wp.write(number);
        PacketCreator.AddItemInfo(wp, item);
        return wp.getPacket();
    }

    public static OutPacket GetTradeStart(Client c, Trade trade, byte number) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(HexTool.getByteArrayFromHexString("05 03 02"));
        wp.write(number);
        if (number == 1) {
            wp.write(0);
            PacketCreator.AddCharLook(wp, trade.getPartner().getChr(), false);
            wp.writeMapleAsciiString(trade.getPartner().getChr().getName());
        }
        wp.write(number);
        PacketCreator.AddCharLook(wp, c.getPlayer(), false);
        wp.writeMapleAsciiString(c.getPlayer().getName());
        wp.write(0xFF);
        return wp.getPacket();
    }

    public static OutPacket GetTradeConfirmation() {
        WritingPacket mplew = new WritingPacket();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x10);
        return mplew.getPacket();
    }

    public static OutPacket GetTradeCompletion(byte number) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(0xA);
        wp.write(number);
        wp.write(6);
        return wp.getPacket();
    }

    public static OutPacket GetTradeCancel(byte number) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(0xA);
        wp.write(number);
        wp.write(2);
        return wp.getPacket();
    }
    
    public static OutPacket GetPlayerShopChat(Player p, String chat, boolean owner) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(HexTool.getByteArrayFromHexString("06 08"));
        wp.write(owner ? 0 : 1);
        wp.writeMapleAsciiString(p.getName() + " : " + chat);
        return wp.getPacket();
    }
        
    public static OutPacket GetPlayerShopChat(Player p, String chat, byte slot) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(HexTool.getByteArrayFromHexString("06 08"));
        wp.write(slot);
        wp.writeMapleAsciiString(p.getName() + " : " + chat);
        return wp.getPacket();
    }
    
    public static OutPacket GetTradePartnerAdd(Player p) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(HexTool.getByteArrayFromHexString("04 01"));
        PacketCreator.AddCharLook(wp, p, false);
        wp.writeMapleAsciiString(p.getName());
        return wp.getPacket();
    }
    
    public static OutPacket SendShopLinkResult(int msg) {
        WritingPacket mplew = new WritingPacket(3);
        mplew.writeShort(SendPacketOpcode.SHOP_LINK_RESULT.getValue());
        mplew.write(msg);
        return mplew.getPacket();
    }
    
    public static OutPacket ShopScannerResult(Client c, boolean displayTopTen, int itemid, List<Pair<PlayerShopItem, AbstractMapleFieldObject>> hmsAvailable, ConcurrentLinkedQueue<Integer> mostSearched) {
        byte itemType = ItemConstants.getInventoryType(itemid).getType();
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOP_SCANNER_RESULT.getValue()); 
        wp.write(displayTopTen ? 7 : 6);
        if (!displayTopTen) {
            wp.writeInt(itemid);
            wp.writeInt(hmsAvailable.size());
            for (Pair<PlayerShopItem, AbstractMapleFieldObject> hme : hmsAvailable) {
                PlayerShopItem item = hme.getLeft();
                AbstractMapleFieldObject mo = hme.getRight();

                if (mo instanceof PlayerShop) {
                    PlayerShop ps = (PlayerShop) mo;
                    Player owner = ps.getOwner();

                    wp.writeMapleAsciiString(owner.getName());
                    wp.writeInt(owner.getMapId());
                    wp.writeMapleAsciiString(ps.getDescription());
                    wp.writeInt(item.getBundles());
                    wp.writeInt(item.getItem().getQuantity());
                    wp.writeInt(item.getPrice());
                    wp.writeInt(owner.getId());
                    wp.write(owner.getClient().getChannel() - 1);
                } else {
                    Merchant hm = (Merchant) mo;

                    wp.writeMapleAsciiString(hm.getOwner());
                    wp.writeInt(hm.getMapId());
                    wp.writeMapleAsciiString(hm.getDescription());
                    wp.writeInt(item.getBundles());
                    wp.writeInt(item.getItem().getQuantity());
                    wp.writeInt(item.getPrice());
                    wp.writeInt(hm.getOwnerId());

                    wp.write(hm.getChannel() - 1);
                }
                wp.write(itemType);
                if (itemType == InventoryType.EQUIP.getType()) {
                    PacketCreator.AddItemInfo(wp, item.getItem(), true, false);
                }
            }
        } else {
            wp.write(mostSearched.size());
            for (Integer i : mostSearched) {
                wp.writeInt(i);
            }
        }
        return wp.getPacket();
    }
    
    /**
     * 1 = Room already closed  * 
     * 2 = Can't enter due full cappacity
     * 3 = Other requests at this minute
     * 4 = Can't do while dead 
     * 5 = Can't do while middle event
     * 6 = This character unable to do it
     * 7, 20 = Not allowed to trade anymore 
     * 9 = Can only trade on same map 
     * 10 = May not open store near portal
     * 11, 14 = Can't start game here 
     * 12 = Can't open store at this channel 
     * 13 = Can't estabilish miniroom
     * 15 = Stores only an the free market 
     * 16 = Lists the rooms at FM (?) 
     * 17 = You may not enter this store
     * 18 = Owner undergoing store maintenance
     * 19 = Unable to enter tournament room 
     * 21 = Not enough mesos to enter
     * 22 = Incorrect password
     *
     * @param status
     * @return
     */
    public static OutPacket GetMiniroomMessage(int status) {
        final WritingPacket mplew = new WritingPacket(5);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(ChannelHeaders.PlayerInteractionHeaders.ACT_JOIN);
        mplew.write(0);
        mplew.write(status);
        return mplew.getPacket();
    }
}
