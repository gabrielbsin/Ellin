/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.Client;
import client.player.Player;
import client.player.inventory.Inventory;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemFactory;
import database.DatabaseConnection;
import static handling.channel.handler.ChannelHeaders.HiredMerchantHeaders.*;
import handling.mina.PacketReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import packet.creators.InteractionPackets;
import server.itens.InventoryManipulator;
import server.itens.ItemInformationProvider;
import server.maps.object.FieldObjectType;
import server.minirooms.Merchant;
import tools.FileLogger;
import tools.Pair;

/**
 *
 * @author GabrielSin
 */
public class HiredMerchantHandler {

    public static void HiredMerchantRequest(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        if (p.getMap().getMapObjectsInRange(p.getPosition(), 23000, Arrays.asList(FieldObjectType.HIRED_MERCHANT)).isEmpty() && p.getMapId() > 910000000 && p.getMapId() < 910000023) {
            if (!p.hasMerchant()) {
                try {
                    if (ItemFactory.MERCHANT.loadItems(p.getId(), false).isEmpty() && p.getMerchantMeso() == 0) {
                        c.getSession().write(InteractionPackets.HiredMerchantBox());
                    } else {
                        c.getSession().write(InteractionPackets.RetrieveFirstMessage());
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } else {
                p.dropMessage(1, "Please close the existing store and try again.");
            }
        } else {
            p.dropMessage(1, "You can not open a store here.");
        }
    }
    
    public static void FredrickRequest(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        byte operation = packet.readByte();
        switch (operation) {
            case UNKNOW: 
                break;
            case RETRIEVE_ITENS:
                c.lockClient();
                try {
                    if (p.hasMerchant()) {
                        return;
                    }
                    List<Pair<Item, InventoryType>> items;
                    try {
                        items = ItemFactory.MERCHANT.loadItems(p.getId(), false);
                        if (!canRetrieveFromFredrick(p, items)) {
                            c.getSession().write(InteractionPackets.FredrickMessageSend((byte) 0x21));
                            return;
                        }

                        p.withdrawMerchantMesos();


                        if (deleteItemsFredrick(p)) {

                            Merchant merchant = p.getHiredMerchant();

                            if (merchant != null) {
                                merchant.clearItems();
                            }

                             for (Pair<Item, InventoryType> it : items) {
                                Item item = it.getLeft();
                                InventoryManipulator.addFromDrop(p.getClient(), item, "Return item from Fredrick", false);
                                String itemName = ItemInformationProvider.getInstance().getName(item.getItemId());
                                FileLogger.print(FileLogger.FREDRICK + p.getName() + ".txt", p.getName() + " gained " + item.getQuantity() + " " + itemName + " (" + item.getItemId() + ")\r\n");
                            }

                            c.getSession().write(InteractionPackets.FredrickMessageSend((byte) 0x1E));
                        } else {
                            p.message("An unknown error has occured.");
                        }
                        break;
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                } finally {
                    c.unlockClient();
                }
                break;
            case CLOSE_FREDRICK: 
                break;
            default:
                System.out.println("Operation not found: " + operation);
                break;
        }
    }
    
    private static boolean canRetrieveFromFredrick(Player p, List<Pair<Item, InventoryType>> items) {
        if (p.getMeso() + p.getMerchantMeso() < 0 || p.getMeso() + p.getMerchantMeso() > Integer.MAX_VALUE) {
            return false;
        }
        return Inventory.checkSpotsAndOwnership(p, items);
    }
    
    private static boolean deleteItemsFredrick(Player p) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM `inventoryitems` WHERE `type` = ? AND `characterid` = ?")) {
                ps.setInt(1, ItemFactory.MERCHANT.getValue());
                ps.setInt(2, p.getId());
                ps.execute();
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
