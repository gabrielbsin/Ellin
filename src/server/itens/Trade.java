/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
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

package server.itens;

import client.Client;
import java.util.LinkedList;
import java.util.List;
import client.player.Player;
import client.player.commands.object.CommandProcessor;
import client.player.inventory.types.InventoryType;
import static client.player.inventory.types.InventoryType.CASH;
import static client.player.inventory.types.InventoryType.ETC;
import static client.player.inventory.types.InventoryType.SETUP;
import static client.player.inventory.types.InventoryType.USE;
import client.player.inventory.Item;
import constants.CommandConstants.CommandType;
import constants.GameConstants;
import constants.ItemConstants;
import java.lang.ref.WeakReference;
import packet.creators.InteractionPackets;
import packet.creators.PacketCreator;

public class Trade {
    private int meso = 0;
    private int exchangeMeso;
    private boolean locked = false, inTrade = false;
    private final byte tradingslot;
    private Trade partner = null;
    private List<Item> exchangeItems;
    private final WeakReference<Player> chr;
    private final List<Item> items = new LinkedList<>();

    public Trade(final byte number, final Player c) {
        this.chr = new WeakReference<>(c);
        this.tradingslot = number;
    }

    public final void CompleteTrade() {
        if (exchangeItems != null) {
            List<Item> itemz = new LinkedList<>(exchangeItems);
            itemz.forEach((item) -> {
                InventoryManipulator.addFromDrop(chr.get().getClient(), item, "Completed trade with " + partner.getChr().getName() + ". " + chr.get().getName() + " received the item.", false);
            });
            exchangeItems.clear();
        }
        if (exchangeMeso > 0) {
            chr.get().gainMeso(exchangeMeso - GameConstants.getTaxAmount(exchangeMeso), false, false);
        }
        exchangeMeso = 0;
        chr.get().getClient().getSession().write(InteractionPackets.GetTradeCompletion(tradingslot));
    }
    
    public final void cancel(final Player chr) {
        cancel(chr, 0);
    }
    
    public final void cancel(final Player chr, final int unsuccessful) {
        if (items != null) { 
            StringBuilder logInfo = new StringBuilder("Canceled trade ");
            if (partner != null) {
                logInfo.append("with ");
                logInfo.append(partner.getChr().getName());
            }
            logInfo.append(". ");
            logInfo.append(chr.getName());
            logInfo.append(" received the item.");
	    List<Item> itemz = new LinkedList<>(items);
            itemz.forEach((item) -> {
                InventoryManipulator.addFromDrop(chr.getClient(), item, logInfo.toString(), false);
            });
            items.clear();
        }
        if (meso > 0) {
            chr.gainMeso(meso, false, false);
        }
        meso = 0;
        chr.getClient().getSession().write(InteractionPackets.GetTradeCancel(tradingslot));
    }
	
    public final boolean isLocked() {
        return locked;
    }
    
    public final void setMeso(final int meso) {
        if (locked || partner == null || meso <= 0 || this.meso + meso <= 0) {
            return;
        }
        if (chr.get().getMeso() >= meso) {
            chr.get().gainMeso(-meso, false, true, false);
            this.meso += meso;
            chr.get().getClient().getSession().write(InteractionPackets.GetTradeMesoSet((byte) 0, this.meso));
            if (partner != null) {
                partner.getChr().getClient().getSession().write(InteractionPackets.GetTradeMesoSet((byte) 1, this.meso));
            }
        }
    }

    public final void addItem(final Item item) {
        if (locked || partner == null) {
            return;
        }
        items.add(item);
        chr.get().getClient().getSession().write(InteractionPackets.GetTradeItemAdd((byte) 0, item));
        if (partner != null) {
            partner.getChr().getClient().getSession().write(InteractionPackets.GetTradeItemAdd((byte) 1, item));
        }
    }
    
    public final void chat(final String message) {
        if (!CommandProcessor.processCommand(chr.get().getClient(), message, CommandType.TRADE)) {
            chr.get().getClient().getSession().write(InteractionPackets.GetPlayerShopChat(chr.get(), message, true));
            if (partner != null) {
                partner.getChr().getClient().getSession().write(InteractionPackets.GetPlayerShopChat(chr.get(), message, false));
            }
        }
    }
    
    public final Trade getPartner() {
        return partner;
    }

    public final void setPartner(final Trade partner) {
        if (locked) {
            return;
        }
        this.partner = partner;
    }

    public final Player getChr() {
        return chr.get();
    }
    
    public boolean inTrade() {
        return inTrade;
    }
	
    public final int getNextTargetSlot() {
        if (items.size() >= 9) {
            return -1;
        }
        int ret = 1;
        for (Item item : items) {
            if (item.getPosition() == ret) {
                ret++;
            }
        }
        return ret;
    }
    
    public final boolean setItems(final Client c, final Item item, byte targetSlot, final int quantity) {
        int target = getNextTargetSlot();
        final ItemInformationProvider ii = ItemInformationProvider.getInstance();
        if (target == -1 || ItemConstants.isPet(item.getItemId()) || isLocked() || (ItemConstants.getInventoryType(item.getItemId()) == InventoryType.CASH && quantity != 1) || (ItemConstants.getInventoryType(item.getItemId()) == InventoryType.EQUIP && quantity != 1)) {
            return false;
        }
       
        if (ii.isDropRestricted(item.getItemId())) {
            c.getSession().write(PacketCreator.EnableActions());
            return false;
        }
        Item tradeItem = item.copy();
        if (ItemConstants.isThrowingStar(item.getItemId()) || ItemConstants.isBullet(item.getItemId())) {
            tradeItem.setQuantity(item.getQuantity());
            InventoryManipulator.removeFromSlot(c, ItemConstants.getInventoryType(item.getItemId()), item.getPosition(), item.getQuantity(), true);
        } else {
            tradeItem.setQuantity((short) quantity);
            InventoryManipulator.removeFromSlot(c, ItemConstants.getInventoryType(item.getItemId()), item.getPosition(), (short) quantity, true);
        }
        if (targetSlot < 0) {
            targetSlot = (byte) target;
        } else {
            for (Item itemz : items) {
                if (itemz.getPosition() == targetSlot) {
                    targetSlot = (byte) target;
                    break;
                }
            }
        }
        tradeItem.setPosition(targetSlot);
        addItem(tradeItem);
        return true;
    }
	
    private int check() {  
        if (chr.get().getMeso() + exchangeMeso < 0) {
            return 1;
        }
        byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;
        for (final Item item : exchangeItems) {
            switch (ItemConstants.getInventoryType(item.getItemId())) {
                case EQUIP:
                    eq++;
                    break;
                case USE:
                    use++;
                    break;
                case SETUP:
                    setup++;
                    break;
                case ETC:
                    etc++;
                    break;
                case CASH:  
                    cash++;
                    break;
            }
        }
        if (chr.get().getInventory(InventoryType.EQUIP).getNumFreeSlot() < eq || chr.get().getInventory(InventoryType.USE).getNumFreeSlot() < use || chr.get().getInventory(InventoryType.SETUP).getNumFreeSlot() < setup || chr.get().getInventory(InventoryType.ETC).getNumFreeSlot() < etc || chr.get().getInventory(InventoryType.CASH).getNumFreeSlot() < cash) {
            return 1;
        }
        return 0;
    }
	
    public final static void completeTrade(final Player p) {
        final Trade local = p.getTrade();
        final Trade partner = local.getPartner();

        if (partner == null || local.locked) {
            return;
        }
        local.locked = true; 
        partner.getChr().getClient().getSession().write(InteractionPackets.GetTradeConfirmation());

        partner.exchangeItems = new LinkedList<>(local.items);  
        partner.exchangeMeso = local.meso;  

        if (partner.isLocked()) { 
            int lz = local.check(), lz2 = partner.check();
            if (lz == 0 && lz2 == 0) {
                local.CompleteTrade();
                partner.CompleteTrade();
            } else {
                partner.cancel(partner.getChr(),lz == 0 ? lz2 : lz);
                local.cancel(p, lz == 0 ? lz2 : lz);
            }
            partner.getChr().setTrade(null);
            p.setTrade(null);
        }
    }

    public static final void cancelTrade(final Trade Localtrade, final Player p) {
        Localtrade.cancel(p);
        final Trade partner = Localtrade.getPartner();
         if (partner != null && partner.getChr() != null) {
            partner.cancel(partner.getChr());
            partner.getChr().setTrade(null);
        }
        p.setTrade(null);
    }
    
    public static final void startTrade(final Player c) {
        if (c.getTrade() == null) {
            c.setTrade(new Trade((byte) 0, c));
            c.getClient().getSession().write(InteractionPackets.GetTradeStart(c.getClient(), c.getTrade(), (byte) 0));
        } else {
            c.getClient().getSession().write(PacketCreator.ServerNotice(5, "You are already in a trade"));
        }
    }
    
    public static final void inviteTrade(final Player c1, final Player c2) {
	if (c1 == null || c1.getTrade() == null) {
	    return;
	}
        if (c2 != null && c2.getTrade() == null) {
            c2.setTrade(new Trade((byte) 1, c2));
            c2.getTrade().setPartner(c1.getTrade());
            c1.getTrade().setPartner(c2.getTrade());
            c2.getClient().getSession().write(InteractionPackets.GetTradeInvite(c1));
        } else {
            c1.getClient().getSession().write(PacketCreator.ServerNotice(5, "The other player is already trading with someone else."));
            cancelTrade(c1.getTrade(), c1);
        }
    }
	
    public static final void visitTrade(final Player c1, final Player c2) {
        if (c1.getTrade() != null && c1.getTrade().getPartner() == c2.getTrade() && c2.getTrade() != null && c2.getTrade().getPartner() == c1.getTrade()) {
            c1.getTrade().inTrade = true;
            c2.getClient().getSession().write(InteractionPackets.GetTradePartnerAdd(c1));
            c1.getClient().getSession().write(InteractionPackets.GetTradeStart(c1.getClient(), c1.getTrade(), (byte) 1));
        } else {
            c1.getClient().getSession().write(PacketCreator.ServerNotice(5, "The other player has already closed the trade"));
        }
    }
   
    public static final void declineTrade(final Player c) {
        final Trade trade = c.getTrade();
        if (trade != null) {
            if (trade.getPartner() != null) {
                Player other = trade.getPartner().getChr();
		if (other != null && other.getTrade() != null) {
                    other.getTrade().cancel(other);
                    other.setTrade(null);
                    other.dropMessage(5, c.getName() + " has declined your trade request.");
		}
            }
            trade.cancel(c);
            c.setTrade(null);
        }
    }
}
