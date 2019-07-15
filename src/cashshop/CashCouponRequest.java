/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cashshop;

import client.Client;
import client.player.inventory.Item;
import constants.ItemConstants;
import handling.mina.PacketReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import packet.creators.CashShopPackets;
import server.itens.InventoryManipulator;
import tools.Pair;

/**
 * 
 * @author GabrielSin
 */
public class CashCouponRequest {
    
    public static void CouponCode(PacketReader packet, Client c) {
        packet.readShort();
        String code = packet.readMapleAsciiString();        
        
        if (code == null || code.length() < 16 || code.length() > 32) { 
            c.getSession().write(CashShopPackets.CouponError(CashShopPackets.ERROR_COUPON_NUMBER));
            return;
        }
        
        final boolean validCode = CashCouponFactory.getCouponCodeValid(code.toUpperCase());
        if (!validCode) {
            c.getSession().write(CashShopPackets.CouponError(CashShopPackets.ERROR_COUPON_NUMBER));
            return;
        }
        
        final List<CashCouponData> rewards = CashCouponFactory.getCcData(code.toUpperCase());
        if (rewards == null) {
            CashCouponFactory.setCouponCodeUsed("ERROR", code);
            c.getSession().write(CashShopPackets.CouponError(CashShopPackets.ERROR_COUPON_NUMBER));
            return;
        }
        
        final Pair<Pair<Integer, Integer>, Pair<List<Item>, Integer>> cscsize = CashCouponFactory.getSize(rewards);
        if ((c.getPlayer().getCashShop().getCash(2) + cscsize.getLeft().getLeft()) < 0) {
            return;
        }
        
        if (c.getPlayer().getCashShop().getItemsSize() >= (100 - cscsize.getLeft().getRight())) {
            c.getSession().write(CashShopPackets.CouponError(CashShopPackets.ERROR_INVENTORY_FULL));
            return;
        }
        
        if (!ItemConstants.haveSpace(c.getPlayer(), cscsize.getRight().getLeft())) {
            c.getSession().write(CashShopPackets.CouponError(CashShopPackets.ERROR_INVENTORY_FULL));
            return;
        }
        
        if (c.getPlayer().getMeso() + cscsize.getRight().getRight() < 0) {
            return;
        }
        
        CashCouponFactory.setCouponCodeUsed(c.getPlayer().getName(), code);
        
        int maplePoints = 0;
        int mesos = 0;
        final Map<Integer, Item> togiveCS = new HashMap<>();
        final List<Pair<Integer, Integer>> togiveII = new ArrayList<>();
        for (final CashCouponData reward : rewards) {
            switch (reward.getType()) {
                case 0: {
                    if (reward.getData() > 0) {
                        c.getPlayer().getCashShop().gainCash(2, reward.getData());
                        maplePoints = reward.getData();
                    }
                    break;
                }
                case 1: {
                    final CashItem item = CashItemFactory.getItem(reward.getData());
                    if (item != null) {
                        final Item itemz = item.toItem(item);
                        if (itemz != null && itemz.getUniqueId() > 0) {
                            togiveCS.put(item.getSN(), itemz);
                            c.getPlayer().getCashShop().addToInventory(itemz);
                        }
                    }
                    break;
                }
                case 2: {
                    if (reward.getQuantity() <= Short.MAX_VALUE && reward.getQuantity() > 0) {
                        final boolean pos = InventoryManipulator.addById(c, reward.getData(), (short) reward.getQuantity(), "MapleSystem");
                        if (pos != false) {  
                            togiveII.add(new Pair<>(reward.getData(), (int) reward.getQuantity()));
                        }
                    }
                    break;
                }
                case 3: {  
                    if (reward.getData() > 0) {
                        c.getPlayer().gainMeso(reward.getData(), false);
                        mesos = reward.getData();
                    }
                    break;
                }
            }
        }
        CashCouponFactory.deleteCouponData(c.getPlayer().getName(), code);
        c.getSession().write(CashShopPackets.CouponRewards(c.getAccountID(), maplePoints, togiveCS, togiveII, mesos));
        c.getSession().write(CashShopPackets.ShowCash(c.getPlayer()));
    }
}
