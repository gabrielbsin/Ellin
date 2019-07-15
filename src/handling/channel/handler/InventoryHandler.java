/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.player.Player;
import client.Client;
import client.player.buffs.BuffStat;
import community.MaplePartyCharacter;
import constants.ExperienceConstants;
import constants.ItemConstants;
import constants.ServerProperties;
import handling.channel.ChannelServer;
import handling.mina.PacketReader;
import handling.world.service.BroadcastService;
import java.util.List;
import java.util.Map;
import java.util.Random;
import packet.creators.PacketCreator;
import client.player.buffs.Disease;
import client.player.inventory.Equip;
import client.player.inventory.EquipScrollResult;
import client.player.inventory.Inventory;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemPet;
import client.player.inventory.TamingMob;
import client.player.skills.PlayerSkill;
import client.player.skills.PlayerSkillFactory;
import client.player.violation.CheatingOffense;
import constants.GameConstants;
import constants.MapConstants;
import handling.channel.handler.operation.UseCashItemOperation;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import packet.creators.InteractionPackets;
import packet.creators.MerchantPackets;
import server.MapleStatEffect;
import server.itens.InventoryManipulator;
import server.itens.ItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.Field;
import server.maps.FieldItem;
import server.maps.FieldLimit;
import server.maps.object.FieldObject;
import server.minirooms.Merchant;
import server.minirooms.PlayerShop;
import tools.FileLogger;
import tools.Randomizer;

/**
 *
 * @author GabrielSin
 */
public class InventoryHandler {

    public static void UseCashItem(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        if (System.currentTimeMillis() - p.getLastUsedCashItem() < 3000) {
            return;
        }
        p.setLastUsedCashItem(System.currentTimeMillis());
        
        short slot = (short) packet.readShort();
        int itemId = packet.readInt();
        int itemType = itemId / 10000;
        Item item = p.getInventory(InventoryType.CASH).getItem(slot);
        if (item == null || item.getItemId() != itemId || item.getQuantity() < 1 || !c.getPlayer().haveItem(itemId)) {
            p.announce(PacketCreator.EnableActions());
            return;
        }
        switch (itemType) {
            case 504:
                UseCashItemOperation.UseVipTeleport(packet, p, itemId, slot);
                break;
            case 505:
                UseCashItemOperation.UseAPandSP(packet, p, itemId, slot);
                break;
            case 506:
                UseCashItemOperation.UseUndefined(packet, p, itemId, slot);
                break;
            case 507:
                UseCashItemOperation.UseMegaphoneItem(packet, p, itemId, slot, item);
		break;
            case 509:
                UseCashItemOperation.UseSendNote(packet, p, slot);
                break;
            case 510:
                UseCashItemOperation.UseJukeBox(p, slot);
                break;
            case 512:
                UseCashItemOperation.UseMapEffectItem(packet, p, itemId, slot);
                break;
            case 517:
                UseCashItemOperation.UsePetNameChange(packet, p, slot);
                break;
            case 520:
                UseCashItemOperation.UseBagMeso(p, itemId, slot);
                break;
            case 524:
                UseCashItemOperation.UsePetFood(p, itemId, slot);
                break;
            case 523:
                UseCashItemOperation.UseShopScanner(packet, p, slot);
                break;
            case 528:
                UseCashItemOperation.UsePassedGas(p, itemId, slot);
                break;
            case 530:
                UseCashItemOperation.UseItemEffect(p, itemId, slot);
                break;     
            case 537:
                UseCashItemOperation.UseChalkBoard(packet, p);
                break;
            case 539:
                UseCashItemOperation.UseMegaAvatar(packet, p, itemId, slot);
                break;
            case 545:
                UseCashItemOperation.UseMyoMyo(p);
                break;
            default:
                System.out.println("Packet unhandled (type: " + itemType + ") : " + packet.toString());
                FileLogger.print("useCashHandler.txt", packet.toString());
                break;
        }
    }
    
    public static void ItemGather(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        packet.readInt();
        InventoryType inventoryType = InventoryType.getByType(packet.readByte());
                
	if (!GameConstants.USE_ITEM_SORT) {
            c.announce(PacketCreator.EnableActions());
            return;
	}
		
	Inventory inventory = c.getPlayer().getInventory(inventoryType);
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        Item srcItem, dstItem;

        for (short dst = 1; dst <= inventory.getSlotLimit(); dst++) {
            dstItem = inventory.getItem(dst);
            if (dstItem == null) continue;

            for (short src = (short)(dst + 1); src <= inventory.getSlotLimit(); src++) {
                srcItem = inventory.getItem(src);
                if (srcItem == null) continue;

                if (dstItem.getItemId() != srcItem.getItemId()) continue;
                if (dstItem.getQuantity() == ii.getSlotMax(c, inventory.getItem(dst).getItemId())) break;

                InventoryManipulator.move(c, inventoryType, src, dst);
            }
        }
                
        inventory = p.getInventory(inventoryType);
        boolean sorted = false;

        while (!sorted) {
            short freeSlot = inventory.getNextFreeSlot();

            if (freeSlot != -1) {
                short itemSlot = -1;
                for (short i = (short) (freeSlot + 1); i <= inventory.getSlotLimit(); i = (short) (i + 1)) {
                    if (inventory.getItem(i) != null) {
                        itemSlot = i;
                        break;
                    }
                }
                if (itemSlot > 0) {
                   InventoryManipulator.move(c, inventoryType, itemSlot, freeSlot);
                } else {
                    sorted = true;
                }
            } else {
                sorted = true;
            }
        }
        c.announce(PacketCreator.FinishedGather(inventoryType.getType()));
        c.announce(PacketCreator.EnableActions());
    }

    public static void ItemSort(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        packet.readInt();
        byte inv = packet.readByte();
        if (inv < 0 || inv > 5) {
            return;
        }
        Inventory Inv = p.getInventory(InventoryType.getByType(inv));
        ArrayList<Item> itemarray = new ArrayList<>();
        for (Item i : Inv) {
            itemarray.add(i.copy());
        }
        Collections.sort(itemarray);
        for (Item item : itemarray) {
            InventoryManipulator.removeById(c, InventoryType.getByType(inv), item.getItemId(), item.getQuantity(), false, false);
        }
        for (Item i : itemarray) {
            InventoryManipulator.addFromDrop(c, i, "", false);
        }
        c.announce(PacketCreator.FinishedSort(inv));
    }
    
    public static void OwlWarp(PacketReader packet, Client c) {
        int ownerid = packet.readInt();
        int mapid = packet.readInt();
        
        Player p = c.getPlayer();
        Merchant hm = c.getChannelServer().getHiredMerchant(ownerid);   
        PlayerShop ps;
        if(hm == null || hm.getMapId() != mapid || !hm.hasItem(p.getOwlSearch())) {
            ps = c.getChannelServer().getPlayerShop(ownerid);
            if(ps == null || ps.getMapId() != mapid || !ps.hasItem(p.getOwlSearch())) {
                if (hm == null && ps == null) {
                    c.announce(InteractionPackets.SendShopLinkResult(1));
                } else {
                    c.announce(InteractionPackets.SendShopLinkResult(3));
                }
                return;
            }
            
            if (ps.isOpen()) {
                if (MapConstants.isFreeMarketRoom(mapid)) {
                    if (ps.getChannel() == c.getChannel()) {
                        p.changeMap(mapid);

                        if (ps.isOpen()) {
                            if(!ps.visitShop(p)) {
                                if(!ps.isBanned(p.getName())) c.announce(InteractionPackets.SendShopLinkResult(2));
                                else c.announce(InteractionPackets.SendShopLinkResult(17));
                            }
                        } else {
                            c.announce(InteractionPackets.SendShopLinkResult(18));
                        }
                    } else {
                        c.announce(PacketCreator.ServerNotice(1, "That shop is currently located in another channel. Current location: Channel " + hm.getChannel() + ", '" + hm.getMap().getMapName() + "'."));
                    }
                } else {
                    c.announce(PacketCreator.ServerNotice(1, "That shop is currently located outside of the FM area. Current location: Channel " + hm.getChannel() + ", '" + hm.getMap().getMapName() + "'."));
                }
            } else {
                c.announce(InteractionPackets.SendShopLinkResult(18));
            }
        } else {
            if (hm.isOpen()) {
                if (MapConstants.isFreeMarketRoom(mapid)) {
                    if (hm.getChannel() == c.getChannel()) {
                        p.changeMap(mapid);

                        if (hm.isOpen()) {
                            if (hm.addVisitor(p)) {
                                c.announce(InteractionPackets.SendShopLinkResult(0));
                                c.announce(MerchantPackets.GetMerchant(c.getPlayer(), hm, false));
                                p.setHiredMerchant(hm);
                            } else {
                                c.announce(InteractionPackets.SendShopLinkResult(2));
                            }
                        } else {
                            c.announce(InteractionPackets.SendShopLinkResult(18));
                        }
                    } else {
                        c.announce(PacketCreator.ServerNotice(1, "That merchant is currently located in another channel. Current location: Channel " + hm.getChannel() + ", '" + hm.getMap().getMapName() + "'."));
                    }
                } else {
                    c.announce(PacketCreator.ServerNotice(1, "That merchant is currently located outside of the FM area. Current location: Channel " + hm.getChannel() + ", '" + hm.getMap().getMapName() + "'."));
                }
            } else {
                c.announce(InteractionPackets.SendShopLinkResult(18));
            }
        }
    }
    
    public static void UseOwlOfMinerva(PacketReader packet, Client c) {
        int owlCase = packet.readByte();
        if (owlCase == 5) {
            ConcurrentLinkedQueue<Integer> mostSearched = c.getChannelServer().retrieveTopResults();
            c.announce(InteractionPackets.ShopScannerResult(c, true, 0, null, mostSearched));
        } else {
            System.out.println("UserShopScannerRequestHandler new owl case.");
        }
    }

    public static void ItemMove(PacketReader packet, Client c) {
        long time = packet.readInt();
        if (c.getPlayer().getLastRequestTime() > time || c.getPlayer().getLastRequestTime() == time) { 
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        c.getPlayer().setLastRequestTime(time);
        
        InventoryType type = InventoryType.getByType(packet.readByte());
        short src = packet.readShort();
        short dst = packet.readShort();
        short quantity = packet.readShort();
        if (src < 0 && dst > 0) {
            InventoryManipulator.unequip(c, src, dst);
        } else if (dst < 0) {
            InventoryManipulator.equip(c, src, dst);
        } else if (dst == 0) {
            InventoryManipulator.drop(c, type, src, quantity);
        } else {
            InventoryManipulator.move(c, type, src, dst);
        }
    }  

    public static void UseItem(PacketReader packet, Client c) {
        if (c.checkCondition()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        Player p = c.getPlayer();
        
        packet.readInt(); 
        byte slot = (byte) packet.readShort();
        int itemId = packet.readInt(); 
        final Item toUse = p.getInventory(InventoryType.USE).getItem(slot); 

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        switch (itemId) {
            case 2050000:
                p.dispelDebuff(Disease.POISON);
                break;
            case 2050001:
                p.dispelDebuff(Disease.DARKNESS);
                break;
            case 2050002:
                p.dispelDebuff(Disease.WEAKEN);
                break;
            case 2050003:
                p.dispelDebuff(Disease.SEAL);
                p.dispelDebuff(Disease.CURSE);
                break;
            case 2050004:
                p.dispelDebuffs();
                break;
            default:
                break;
        }
        if (!FieldLimit.CANNOTUSEPOTION.check(p.getMap().getFieldLimit())) {
            if (ItemConstants.isTownScroll(itemId)) {
                if (ii.getItemEffect(toUse.getItemId()).applyTo(p)) {
                    InventoryManipulator.removeFromSlot(c, InventoryType.USE, slot, (short) 1, false);
                } else {
                    c.getSession().write(PacketCreator.EnableActions());
                    return;
                }
            }
            InventoryManipulator.removeFromSlot(c, InventoryType.USE, slot, (short) 1, false);
            ii.getItemEffect(toUse.getItemId()).applyTo(p);
            p.checkBerserk(p.isHidden());
        } else {
            c.getSession().write(PacketCreator.EnableActions());
        }
    }

    public static void UseUpgradeScroll(PacketReader packet, Client c) {
        if (c.checkCondition()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        Player p = c.getPlayer();
        long time =  packet.readInt();
        if (p.getLastRequestTime() > time || p.getLastRequestTime() == time) { 
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        p.setLastRequestTime(time);
        
        byte slot = (byte) packet.readShort();
        byte dst = (byte) packet.readShort();
        byte ws = (byte) packet.readShort();
        boolean whiteScroll = false; 
        boolean legendarySpirit = false; 
        final ItemInformationProvider ii = ItemInformationProvider.getInstance();

        if ((ws & 2) == 2) {
            whiteScroll = true;
        }
        
        Equip toScroll;
        if (dst < 0) {
            toScroll = (Equip) p.getInventory(InventoryType.EQUIPPED).getItem(dst);
        } else {
            legendarySpirit = true;
            toScroll = (Equip) p.getInventory(InventoryType.EQUIP).getItem(dst);
        }
        if (toScroll == null) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to scroll equip with nonexistent equip");
            return;
        }
        
        byte oldLevel = toScroll.getLevel();
        byte oldSlots = toScroll.getUpgradeSlots();
        
        if (((Equip) toScroll).getUpgradeSlots() < 1) {
            c.getSession().write(PacketCreator.GetInventoryFull());
            return;
        }
        Inventory useInventory = p.getInventory(InventoryType.USE);
        Item scroll = useInventory.getItem(slot);
        Item wscroll = null;
        
        if (scroll == null || scroll.getQuantity() < 1) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to scroll equip with nonexistent scroll");
            return;
        }

        List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
        if(scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId())) {
            c.getSession().write(PacketCreator.GetInventoryFull()); 
            return; 
        } 

        if (whiteScroll) {
            wscroll = p.getInventory(InventoryType.USE).findById(2340000);
            if (wscroll == null || wscroll.getItemId() != 2340000) {
                whiteScroll = false;
            }
        }
        if (scroll.getItemId() != 2049100 && !ii.isCleanSlate(scroll.getItemId())) {
            if (!ii.canScroll(scroll.getItemId(), toScroll.getItemId())) {
                return;
            }
        }
        Equip scrolled = (Equip) ii.scrollEquipWithId(toScroll, scroll.getItemId(), whiteScroll, p.isGameMaster());
        EquipScrollResult scrollSuccess = EquipScrollResult.FAIL;  
        if (scrolled == null) {
            scrollSuccess = EquipScrollResult.CURSE;
        } else if (scrolled.getLevel() > oldLevel || (ii.isCleanSlate(scroll.getItemId()) && scrolled.getUpgradeSlots() == oldSlots + 1)) {
            scrollSuccess = EquipScrollResult.SUCCESS;
        }
        useInventory.removeItem(scroll.getPosition(), (short) 1, false);
        if (whiteScroll) {
            if (wscroll != null) {
                useInventory.removeItem(wscroll.getPosition(), (short) 1, false);
                if (wscroll.getQuantity() < 1) {
                    c.getSession().write(PacketCreator.ClearInventoryItem(InventoryType.USE, wscroll.getPosition(), false));
                } else {
                    c.getSession().write(PacketCreator.UpdateInventorySlot(InventoryType.USE, (Item) wscroll));
                }
            }
        }
        if (scrollSuccess == EquipScrollResult.CURSE) {
            c.getSession().write(PacketCreator.ScrolledItem(scroll, toScroll, true));
            if (dst < 0) {
                p.getInventory(InventoryType.EQUIPPED).removeItem(toScroll.getPosition());
            } else {
                p.getInventory(InventoryType.EQUIP).removeItem(toScroll.getPosition());
            }
        } else {
            c.getSession().write(PacketCreator.ScrolledItem(scroll, scrolled, false));
        }
        
        p.getMap().broadcastMessage(PacketCreator.GetScrollEffect(p.getId(), scrollSuccess, legendarySpirit));
        
        PlayerSkill LS = PlayerSkillFactory.getSkill(1003);
        int LSLevel = p.getSkillLevel(LS);

        if (legendarySpirit && LSLevel <= 0) {
            return;
        }

        if (dst < 0 && (scrollSuccess == EquipScrollResult.SUCCESS || scrollSuccess == EquipScrollResult.CURSE)) {
            p.equipChanged();
        }

        switch (toScroll.getItemId()) {
            case 1122000:
                Field map = c.getChannelServer().getMapFactory().getMap(240000000);
                map.broadcastMessage(PacketCreator.ServerNotice(5, "A mysterious power arose as I heard the powerful cry of Nine Spirit Baby Dragon."));
                map.buffField(2022109);
                break;
        }
        if (ItemConstants.isGmScroll(scroll.getItemId()) && !(toScroll.getItemId() == 1122000)) {
            ItemInformationProvider mii = ItemInformationProvider.getInstance();
            MapleStatEffect statEffect = mii.getItemEffect(2022118);
            c.getChannelServer().getPlayerStorage().getAllCharacters().forEach((mc) -> {
                statEffect.applyTo(mc);
            });
            c.getChannelServer().broadcastPacket(PacketCreator.ServerNotice(5, "A Mysterious power arose as I heard the power of the super scroll."));
            BroadcastService.broadcastGMMessage(PacketCreator.ServerNotice(5, p.getName() + " is using a GM scroll, itemID: " + scroll.getItemId()));
            FileLogger.print("useGMScroll.txt", p.getName() + " is using a GM scroll, itemID: " + scroll.getItemId());
        }
    }
	
    public static void UseSummonBag(PacketReader packet, Client c) {
        if (c.checkCondition()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        packet.readInt(); 
        Player p = c.getPlayer();
        final byte slot = (byte) packet.readShort();
        final int itemId = packet.readInt();
        final Item toUse = p.getInventory(InventoryType.USE).getItem(slot);
        
        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId) {
            
            InventoryManipulator.removeFromSlot(c, InventoryType.USE, slot, (short) 1, false);
            if (p.isGameMaster() || !FieldLimit.SUMMON.check(p.getMap().getFieldLimit())) {
                final int[][] toSpawn = ItemInformationProvider.getInstance().getSummonMobs(itemId);

                for (int[] toSpawnChild : toSpawn) {
                    if (Randomizer.nextInt(101) <= toSpawnChild[1]) {
                        p.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(toSpawnChild[0]), p.getPosition());
                    }
                }
            }
        } 
       c.getSession().write(PacketCreator.EnableActions());
    }

    public static void PickupPlayer(PacketReader packet, Client c) {
        if (c.checkCondition()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        packet.readByte();	
        packet.readInt(); 
        packet.readInt(); 
        
        Player p = c.getPlayer();
        final FieldObject ob = p.getMap().getMapObject(packet.readInt());
        if (ob == null) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        if (p.getBuffedValue(BuffStat.DARKSIGHT) != null && !p.isGameMaster()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        final FieldItem fieldItem = (FieldItem) ob;
        final ItemInformationProvider ii = ItemInformationProvider.getInstance();
        fieldItem.lockItem();

        try {
            synchronized (fieldItem) {
                if (fieldItem.isPickedUp()) {
                    c.getSession().write(PacketCreator.EnableActions());
                    return;
                }
                if (fieldItem.getOwnerId() != p.getId() && ((!fieldItem.isPlayerDrop() && fieldItem.getDropType() == 0) || (fieldItem.isPlayerDrop() && p.getMap().getEverlast()))) {
                    c.getSession().write(PacketCreator.EnableActions());
                    return;
                }
                if (!fieldItem.isPlayerDrop() && fieldItem.getDropType() == 1 && fieldItem.getOwnerId() != p.getId() && (p.getParty() == null || p.getParty().getMemberById(fieldItem.getOwnerId()) == null)) {     
                    c.getSession().write(PacketCreator.EnableActions());
                    return;
                }
                
                double Distance = p.getPosition().distanceSq(fieldItem.getPosition());
                if (Distance > 2500) {
                    p.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC);
                    if (p.getCheatTracker().getPoints() > 50) {
                        p.getClient().getSession().close();
                    }
                } else if (p.getPosition().distanceSq(fieldItem.getPosition()) > 640000.0) {
                    p.getCheatTracker().registerOffense(CheatingOffense.SHORT_ITEMVAC);
                    if (p.getCheatTracker().getPoints() > 50) {
                        p.getClient().getSession().close();
                    }
                }

                if (!p.isAlive()) {
                    p.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
                    if (p.getCheatTracker().getPoints() > 10) {
                        BroadcastService.broadcastGMMessage(PacketCreator.ServerNotice(5, p.getName() + " is looting while dead."));
                        p.getClient().getSession().close();
                    }
                    return;
                } 

                if (p.getMCPQField() != null) {     
                    boolean consumed = p.getMCPQField().onItemPickup(c.getPlayer(), fieldItem);
                    if (consumed) {
                        p.getMap().broadcastMessage(PacketCreator.RemoveItemFromMap(fieldItem.getObjectId(), 2, p.getId()), fieldItem.getPosition());
                        p.getCheatTracker().pickupComplete();
                        p.getMap().removeMapObject(ob);
                        return;
                    } else {
                        if (InventoryManipulator.addFromDrop(c, fieldItem.getItem(), "Add item from Carnival", true)) {
                            p.getMap().broadcastMessage(PacketCreator.RemoveItemFromMap(fieldItem.getObjectId(), 2, p.getId()), fieldItem.getPosition());
                            p.getCheatTracker().pickupComplete();
                            p.getMap().removeMapObject(ob);
                        } else {
                            p.getCheatTracker().pickupComplete();
                            return;
                        }
                    }
                }

                if (fieldItem.getMeso() > 0) {
                    if (p.getParty() != null) {
                        ChannelServer cserv = c.getChannelServer();
                        int mesosAmm = fieldItem.getMeso();
                        int partyNum = 0;
                        for (MaplePartyCharacter partymem : p.getParty().getMembers()) {
                            if (partymem.isOnline() && partymem.getMapId() == p.getMap().getId() && partymem.getChannel() == c.getChannel() && partymem.getPlayer() != null && !partymem.getPlayer().getCashShop().isOpened()) {
                                partyNum++;
                            }
                        }
                        int mesosGain = mesosAmm / partyNum;
                        for (MaplePartyCharacter partyMem : p.getParty().getMembers()) {
                            if (partyMem.isOnline() && partyMem.getMapId() == p.getMap().getId() && partyMem.getChannel() == c.getChannel() && partyMem.getPlayer() != null && !partyMem.getPlayer().getCashShop().isOpened()) {
                                Player somecharacter = cserv.getPlayerStorage().getCharacterById(partyMem.getId());
                                if (somecharacter != null) {
                                    somecharacter.gainMeso(mesosGain, true, true);
                                }
                            }
                        }
                    } else {
                        p.gainMeso(fieldItem.getMeso(), true, true);
                    }
                    p.getMap().broadcastMessage(PacketCreator.RemoveItemFromMap(fieldItem.getObjectId(), 2, p.getId()), fieldItem.getPosition());
                    p.getCheatTracker().pickupComplete();
                    p.getMap().removeMapObject(ob);

                } else if (fieldItem.getItemId() == 4031865 || fieldItem.getItemId() == 4031866) {
                    int nxGain = fieldItem.getItemId() == 4031865 ? 100 : 250;
                    p.getCashShop().gainCash(1, nxGain);

                    p.showHint("You have earned #e#b" + nxGain + " NX#k#n. (" + p.getCashShop().getCash(1) + " NX)", (short) 300);

                    p.getMap().broadcastMessage(PacketCreator.RemoveItemFromMap(fieldItem.getObjectId(), 2, p.getId()), fieldItem.getPosition());
                    p.getCheatTracker().pickupComplete();
                    p.getMap().removeMapObject(ob);
                } else if (fieldItem.getItem().getItemId() == 2022268) {
                        p.getMap().broadcastMessage(PacketCreator.RemoveItemFromMap(fieldItem.getObjectId(), 2, p.getId()),
                        fieldItem .getPosition());
                        p.getCheatTracker().pickupComplete();
                        p.getMap().removeMapObject(ob);
                        c.getSession().write(PacketCreator.GetShowItemGain(fieldItem.getItem().getItemId(), (short) 1));
                        MobSkill skill = MobSkillFactory.getMobSkill(132, 2);
                        for (Player pl : p.getMap().getCharacters()) {
                            if (pl.getId() != p.getId()) {
                                pl.giveDebuff(Disease.CONFUSE, skill);
                            }
                        }
                        c.getSession().write(PacketCreator.EnableActions());
                } else {
                    final int itemId = fieldItem.getItem().getItemId();
                    if (ii.isConsumeOnPickup(itemId)) {
                        ii.getItemEffect(itemId).applyTo(c.getPlayer());
                        c.announce(PacketCreator.GetShowItemGain(itemId, fieldItem.getItem().getQuantity()));
                        p.getMap().broadcastMessage(PacketCreator.RemoveItemFromMap(fieldItem.getObjectId(), 2, p.getId()), fieldItem.getPosition());
                        p.getCheatTracker().pickupComplete();
                        p.getMap().removeMapObject(ob);
                        fieldItem.setPickedUp(true);
                    } else if (InventoryManipulator.addFromDrop(c, fieldItem.getItem(), "Picked up by " + p.getId(), true)) {
                        p.getMap().broadcastMessage(PacketCreator.RemoveItemFromMap(fieldItem.getObjectId(), 2, p.getId()),
                        fieldItem.getPosition());
                        p.getCheatTracker().pickupComplete();
                        p.getMap().removeMapObject(ob);
                        if (fieldItem.getItem().getItemId() == ItemConstants.ARIANT_JEWEL) {
                            p.updateAriantScore();
                        }
                    } else {
                        p.getCheatTracker().pickupComplete();
                    }
                }
                fieldItem.setPickedUp(true);
                c.getSession().write(PacketCreator.EnableActions());
            }
        } finally {
           fieldItem.unlockItem();        
        }
    }

    public static void PetMapItemPickUp(PacketReader packet, Client c) {
        if (c.checkCondition()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        Player p = c.getPlayer();
        if (p.getInventory(InventoryType.EQUIPPED).findById(ItemConstants.MESO_MAGNET) == null && p.getInventory(InventoryType.EQUIPPED).findById(ItemConstants.ITEM_POUCH) == null) {
            p.announce(PacketCreator.EnableActions());
            return;
        }
        
        int petz = p.getPetIndex((int) packet.readLong());
        final ItemPet pet = p.getPet(petz);
        packet.readByte();
        packet.readInt(); 
        packet.readPos();
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        final FieldObject ob = p.getMap().getMapObject(packet.readInt());
        if (ob == null || pet == null) {
            return;
        }

        final FieldItem fieldItem = (FieldItem) ob;
        fieldItem.lockItem();
        try {
            synchronized (fieldItem) {
                if (fieldItem.isPickedUp()) {
                    p.announce(PacketCreator.GetInventoryFull());
                    return;
                }

                final double Distance = pet.getPosition().distanceSq(fieldItem.getPosition());
                p.getCheatTracker().checkPickupAgain();

                if (Distance > 2500) {
                    p.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC);
                } else if (Distance > 640000.0) {
                    p.getCheatTracker().registerOffense(CheatingOffense.SHORT_ITEMVAC);
                }
                
                if (p.getInventory(InventoryType.EQUIPPED).findById(ItemConstants.ITEM_IGNORE) != null){
                    for (int list : pet.getExceptionList()){
                        if ((fieldItem.getItem() != null && fieldItem.getItem().getItemId() == list) || (fieldItem.getMeso() > 0 && list == Integer.MAX_VALUE)) {
                            p.announce(PacketCreator.EnableActions());
                            return;
                        }
                    }
                }

                int mesos = fieldItem.getMeso();
                if (mesos > 0 && p.getInventory(InventoryType.EQUIPPED).findById(ItemConstants.MESO_MAGNET) != null) {
                    if (p.getParty() != null) {
                        final ChannelServer cserv = p.getClient().getChannelServer();
                        int partynum = 0;
                        partynum = p.getParty().getMembers().stream().filter((partymem) -> (partymem.isOnline() && partymem.getChannel() == p.getClient().getChannel() && partymem.getMapId() == p.getMap().getId() && partymem.getPlayer() != null && !partymem.getPlayer().getCashShop().isOpened())).map((_item) -> 1).reduce(partynum, Integer::sum);
                        if (partynum == 0) {
                            partynum = 1;
                        }
                        for (MaplePartyCharacter partymem : p.getParty().getMembers()) {
                            if (partymem.isOnline() && partymem.getChannel() == p.getClient().getChannel() && partymem.getMapId() == p.getMap().getId() && partymem.getPlayer() != null && !partymem.getPlayer().getCashShop().isOpened()) {
                                Player somecharacter = cserv.getPlayerStorage().getCharacterById(partymem.getId());
                                if (somecharacter != null) {
                                    somecharacter.gainMeso(mesos / partynum, true, false, false);
                                    p.getMap().broadcastMessage(PacketCreator.RemoveItemFromMap(fieldItem.getObjectId(), 5, p.getId(), true, petz), fieldItem.getPosition());
                                    p.getCheatTracker().pickupComplete();
                                    p.getMap().removeMapObject(ob);
                                    fieldItem.setPickedUp(true);
                                }
                            }
                        }
                    } else {
                        p.gainMeso(mesos, true, false, false);
                        p.getMap().broadcastMessage(PacketCreator.RemoveItemFromMap(fieldItem.getObjectId(), 5, p.getId(), true, petz), fieldItem.getPosition());
                        p.getCheatTracker().pickupComplete();
                        p.getMap().removeMapObject(ob);
                        fieldItem.setPickedUp(true);
                    }
                } else if (fieldItem.getItem() != null && p.getInventory(InventoryType.EQUIPPED).findById(ItemConstants.ITEM_POUCH) != null) {
                    int itemId = fieldItem.getItem().getItemId();
                    if (ii.isConsumeOnPickup(itemId)) {
                        ItemInformationProvider.getInstance().getItemEffect(itemId).applyTo(p);
                        p.announce(PacketCreator.GetShowItemGain(itemId, fieldItem.getItem().getQuantity()));
                        p.getMap().broadcastMessage(PacketCreator.RemoveItemFromMap(fieldItem.getObjectId(), 5, p.getId(), true, petz), fieldItem.getPosition());
                        p.getCheatTracker().pickupComplete();
                        p.getMap().removeMapObject(ob);
                        fieldItem.setPickedUp(true);
                    } else if(fieldItem.getItemId() == 4031865 || fieldItem.getItemId() == 4031866) {
                        int nxGain = fieldItem.getItemId() == 4031865 ? 100 : 250;
                        p.getCashShop().gainCash(1, nxGain);

                        p.showHint("You have earned #e#b" + nxGain + " NX#k#n. (" + p.getCashShop().getCash(1) + " NX)", (short) 300);

                        p.getMap().broadcastMessage(PacketCreator.RemoveItemFromMap(fieldItem.getObjectId(), 2, p.getId()), fieldItem.getPosition());
                        p.getCheatTracker().pickupComplete();
                        p.getMap().removeMapObject(ob);
                    } else if (fieldItem.getItem().getItemId() == 2022268) {
                        p.getMap().broadcastMessage(PacketCreator.RemoveItemFromMap(fieldItem.getObjectId(), 2, p.getId()),
                        fieldItem .getPosition());
                        p.getCheatTracker().pickupComplete();
                        p.getMap().removeMapObject(ob);
                        p.announce(PacketCreator.GetShowItemGain(fieldItem.getItem().getItemId(), (short) 1));
                        MobSkill skill = MobSkillFactory.getMobSkill(132, 2);
                        for (Player pl : p.getMap().getCharacters()) {
                            if (pl.getId() != p.getId()) {
                                pl.giveDebuff(Disease.CONFUSE, skill);
                            }
                        }
                        p.announce(PacketCreator.EnableActions());
                    } else {
                        if (InventoryManipulator.addFromDrop(p.getClient(), fieldItem.getItem(), "Picked up by " + p.getName(), true)) {
                            p.getMap().broadcastMessage(PacketCreator.RemoveItemFromMap(fieldItem.getObjectId(), 5, p.getId(), true, petz), fieldItem.getPosition());
                            p.getCheatTracker().pickupComplete();
                            p.getMap().removeMapObject(ob);
                            fieldItem.setPickedUp(true);
                        } else {
                            p.getCheatTracker().pickupComplete();
                        }
                    }
                } 
            }
        } finally {
            fieldItem.unlockItem();
        }    
    }

    public static void UseSkillBook(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        packet.readInt();
        short slot = (short) packet.readShort();
        int itemId = packet.readInt();
        final Item toUse = c.getPlayer().getInventory(ItemConstants.getInventoryType(itemId)).getItem(slot);
        
        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            return;
        }

        final Map<String, Integer> skilldata = ItemInformationProvider.getInstance().getSkillStats(toUse.getItemId(), c.getPlayer().getJob().getId());
        if (skilldata == null) { 
            return;
        }
        
        boolean canuse = false;
        boolean success = false;
        int skill = 0;
        int maxlevel = 0;
        
        final int SuccessRate = skilldata.get("success");
        final int ReqSkillLevel = skilldata.get("reqSkillLevel");
        final int MasterLevel = skilldata.get("masterLevel");
        byte i = 0;
        Integer CurrentLoopedSkillId;
        while (true) {
            CurrentLoopedSkillId = skilldata.get("skillid" + i);
            i++;
            if (CurrentLoopedSkillId == null) {
                break; 
            }
            final PlayerSkill CurrSkillData = PlayerSkillFactory.getSkill(CurrentLoopedSkillId);
            if (CurrSkillData != null && CurrSkillData.canBeLearnedBy(p.getJob()) && p.getSkillLevel(CurrSkillData) >= ReqSkillLevel && p.getMasterLevel(CurrSkillData) < MasterLevel) {
                canuse = true;
                if (Randomizer.nextInt(100) <= SuccessRate && SuccessRate != 0) {
                    success = true;
                    p.changeSkillLevel(CurrSkillData, p.getSkillLevel(CurrSkillData), (byte) MasterLevel);
                } else {
                    success = false;
                }
                InventoryManipulator.removeFromSlot(c, ItemConstants.getInventoryType(itemId), slot, (short) 1, false);
                break;
            }
        }
        c.getPlayer().getMap().broadcastMessage(PacketCreator.useSkillBook(p, skill, maxlevel, canuse, success));
        c.getSession().write(PacketCreator.EnableActions());
    }

    public static void UseCatchItem(PacketReader packet, Client c) {
        if (System.currentTimeMillis() - c.getPlayer().getLastCatch() < 2000) {
            c.getSession().write(PacketCreator.ServerNotice(5, "You can not use a rock now!"));
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        packet.readInt();
        final byte slot = (byte) packet.readShort();
        final int itemid = packet.readInt();
        final int oid = packet.readInt();
        final MapleMonster mob = c.getPlayer().getMap().getMonsterByOid(oid);
        final Item toUse = c.getPlayer().getInventory(InventoryType.USE).getItem(slot);
        
        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mob != null) {
            switch (itemid) {
                case 2270002: { // Characteristic Stone
                    final Field map = c.getPlayer().getMap();
                    if (map.getId() != 980010101 && map.getId() != 980010201 && map.getId() != 980010301 && !c.getPlayer().isGameMaster()) {
                        c.getPlayer().dropMessage(1, "This item is not available for use outside of AriantPQ.");
                        c.getSession().write(PacketCreator.EnableActions());
                        return;
                    }
                    if (mob.getHp() <= mob.getMaxHp() / 2) {
                        c.getPlayer().getMap().broadcastMessage(PacketCreator.CatchMonster(oid, itemid, (byte) 1));
                        mob.getMap().killMonster(mob, c.getPlayer(), false, 0);
                        InventoryManipulator.removeById(c, InventoryType.USE, itemid, 1, false, false);
                        InventoryManipulator.addById(c, ItemConstants.ARIANT_JEWEL, (short)1, null, "", null);
                        c.getSession().write(PacketCreator.ServerNotice(5, "You gained a jewel!"));
                        c.getPlayer().setLastCatch(System.currentTimeMillis());
                        c.getPlayer().updateAriantScore();
                    } else {
                        c.getPlayer().getMap().broadcastMessage(PacketCreator.CatchMonster(oid, itemid, (byte) 0));
                        c.getPlayer().dropMessage(5, "The monster has a lot of physical strength, so you can not catch it.");
                    }
                    break;
                }
                case 2270000: { // Pheromone Perfume
                    if (mob.getId() != 9300101) {
                        break;
                    }
                    c.getPlayer().getMap().broadcastMessage(PacketCreator.CatchMonster(oid, itemid, (byte) 1));
                    c.getPlayer().getMap().killMonster(mob, c.getPlayer(), true, (byte) 1);
                    InventoryManipulator.addById(c, 1902000, (short) 1, null, "", null);
                    InventoryManipulator.removeById(c, InventoryType.USE, itemid, 1, false, false);
                    break;
                }
                case 2270003: { // Cliff's Magic Cane
                    if (mob.getId() != 9500320) {
                        break;
                    }
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 4)) {
                        c.getPlayer().getMap().broadcastMessage(PacketCreator.CatchMonster(oid, itemid, (byte) 1));
                        c.getPlayer().getMap().killMonster(mob, c.getPlayer(), true, (byte) 1);
                        InventoryManipulator.removeById(c, InventoryType.USE, itemid, 1, false, false);
                        InventoryManipulator.addById(c, 4031887, (short) 1, null, "", null);
                    } else {
                        c.getPlayer().getMap().broadcastMessage(PacketCreator.CatchMonster(oid, itemid, (byte) 0));
                        c.getPlayer().dropMessage(5, "The monster has a lot of physical strength so you can not catch it.");
                    }
                    break;
                }
                case 2270001: {
                    if (mob.getId() != 9500197) {
                        break;
                    }
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 4)) {
                    c.getPlayer().getMap().broadcastMessage(PacketCreator.CatchMonster(oid, itemid, (byte) 1));
                    c.getPlayer().getMap().killMonster(mob, c.getPlayer(), true, (byte) 1);
                    InventoryManipulator.addById(c, 4031830, (short) 1, null, "", null);
                    InventoryManipulator.removeById(c, InventoryType.USE, itemid, 1, false, false);
                    } else {
                     c.getPlayer().getMap().broadcastMessage(PacketCreator.CatchMonster(oid, itemid, (byte) 0));   
                    }
                    break;
                }

                case 2270005: {
                    if (mob.getId() != 9300187) {
                        break;
                    }
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 3)) {
                        c.getPlayer().getMap().broadcastMessage(PacketCreator.CatchMonster(oid, itemid, (byte) 1));
                        c.getPlayer().getMap().killMonster(mob, c.getPlayer(), true, (byte) 1);
                        InventoryManipulator.removeById(c, InventoryType.USE, itemid, 1, false, false);
                        InventoryManipulator.addById(c, 2109001, (short) 1, null, "", null);
                    } else {
                        c.getPlayer().getMap().broadcastMessage(PacketCreator.CatchMonster(oid, itemid, (byte) 0));
                    }
                }
                c.getSession().write(PacketCreator.EnableActions());
                break;

                case 2270006: {
                    if (mob.getId() == 9300189) {
                        final Field map = c.getPlayer().getMap();
                        if (mob.getHp() < ((mob.getMaxHp() / 10) * 3)) {
                            map.broadcastMessage(PacketCreator.CatchMonster(mob.getId(), itemid, (byte) 1));
                            map.killMonster(mob, c.getPlayer(), true, (byte) 0);
                            InventoryManipulator.removeById(c, InventoryType.USE, itemid, 1, false, false);
                            InventoryManipulator.addById(c, 2109002, (short) 1, "");
                        }

                    }
                    c.getSession().write(PacketCreator.EnableActions());
                    break;
                }
                case 2270007: {
                    if (mob.getId() == 9300191) {
                        final Field map = c.getPlayer().getMap();
                        if (mob.getHp() < ((mob.getMaxHp() / 10) * 3)) {
                            map.broadcastMessage(PacketCreator.CatchMonster(mob.getId(), itemid, (byte) 1));
                            map.killMonster(mob, c.getPlayer(), true, (byte) 0);
                            InventoryManipulator.removeById(c, InventoryType.USE, itemid, 1, false, false);
                            InventoryManipulator.addById(c, 2109003, (short) 1, "");
                        }
                    }
                    c.getSession().write(PacketCreator.EnableActions());
                    break;
                }
                case 2270004: {
                    if (mob.getId() == 9300175) {
                        final Field map = c.getPlayer().getMap();
                        if (mob.getHp() < ((mob.getMaxHp() / 10) * 4)) {
                            map.broadcastMessage(PacketCreator.CatchMonster(mob.getId(), itemid, (byte) 1));
                            map.killMonster(mob, c.getPlayer(), true, (byte) 0);
                            InventoryManipulator.removeById(c, InventoryType.USE, itemid, 1, false, false);
                            InventoryManipulator.addById(c, 4001169, (short) 1, "");
                        }
                    }
                    c.getSession().write(PacketCreator.EnableActions());
                    break;
                }
            }
        }
        c.getSession().write(PacketCreator.EnableActions());
    }

    public static void UseMountFood(PacketReader packet, Client c) {
        packet.readInt();
        final byte slot = (byte) packet.readShort();
        final int itemid = packet.readInt();
        final Item toUse = c.getPlayer().getInventory(InventoryType.USE).getItem(slot);
        final TamingMob mount = c.getPlayer().getMount();
        
        if ((itemid / 10000 == 226) && toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mount != null) {
            boolean levelUp;
            final int fatigue;
            
            c.lockClient();
            try {
                fatigue = mount.getTiredness();
                levelUp = false;
                mount.setTiredness(-30);
            } finally {
                c.unlockClient();
            }
            if (fatigue > 0) {
                mount.increaseExp();
                final int level = mount.getLevel();
                if (mount.getExp() >= ExperienceConstants.getMountExpNeededForLevel(level + 1) && level < 31) {
                    mount.setLevel((level + 1));
                    levelUp = true;
                }
            }
            c.getPlayer().getMap().broadcastMessage(PacketCreator.UpdateMount(c.getPlayer(), levelUp));
            InventoryManipulator.removeById(c, InventoryType.USE, itemid, 1, true, false);
        }
        c.getSession().write(PacketCreator.EnableActions());
    }
    
    

    public static void UseSilverBox(PacketReader packet, Client c) {
        short slot = packet.readShort();
        final int itemId = packet.readInt();
        
        Item item = c.getPlayer().getInventory(InventoryType.CASH).getItem(slot);
        if (item == null || item.getItemId() != itemId || item.getQuantity() < 1 || !c.getPlayer().haveItem(itemId)) {
            c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried open silverbox nonexistent item");
            return;
        }
        if (!c.getPlayer().isAlive()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        if (!c.getPlayer().haveItem(ItemConstants.SILVER_BOX_KEY)) { 
            c.getPlayer().dropMessage(5, "You do not have the box key to open this box.");
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        for (InventoryType type : InventoryType.values()) { 
            if (c.getPlayer().getInventory(type).isFull()) {
                c.getPlayer().dropMessage(5, "You do not have enough inventory space to get the item. Please clear some space from your inventory.");
                c.getSession().write(PacketCreator.EnableActions());
                return;
            }
        }
        int prizeid = 0;
        double chance = Math.random();
        if (itemId == ItemConstants.RewardSilverBox.SILVER_BOX_ITEM) { 
            if (chance < 0.05) {
                prizeid = ItemConstants.RewardSilverBox.RARE[new Random().nextInt(ItemConstants.RewardSilverBox.RARE.length)];
            } else if (chance >= 0.06 && chance < 0.35) { 
                prizeid = ItemConstants.RewardSilverBox.UNCOMMON[new Random().nextInt(ItemConstants.RewardSilverBox.UNCOMMON.length)];
            } else { 
                prizeid = ItemConstants.RewardSilverBox.COMMON[new Random().nextInt(ItemConstants.RewardSilverBox.COMMON.length)];
            }   
        } else if (itemId == ItemConstants.RewardGoldBox.GOLD_BOX_ITEM) { 
            if (chance < 0.1) {
                prizeid = ItemConstants.RewardGoldBox.RARE[new Random().nextInt(ItemConstants.RewardGoldBox.RARE.length)];
            } else if (chance >= 0.11 && chance < 0.35) {
                prizeid = ItemConstants.RewardGoldBox.UNCOMMON[new Random().nextInt(ItemConstants.RewardGoldBox.UNCOMMON.length)];
            } else { 
                prizeid = ItemConstants.RewardGoldBox.COMMON[new Random().nextInt(ItemConstants.RewardGoldBox.COMMON.length)];
            }
        }
        if (prizeid != 0) {
            c.getPlayer().gainItem(ItemConstants.SILVER_BOX_KEY, (short) -1, true); 
            c.getPlayer().gainItem(itemId, (short) -1, true); 
            c.getPlayer().gainItem(prizeid, (short) 1, true); 
            c.getSession().write(PacketCreator.SilverBoxOpened(itemId)); 
        }
    }
    
    public static boolean UseIncubator(Client c, int itemid) {
        String[] types = {"Normal", "Medium", "Rare"};
        HashMap<String, String> IncubatedItem = new HashMap<>();
        try {
            double chance = Math.random();
            BufferedReader br;
                try (FileReader fl = new FileReader(ServerProperties.Misc.DATA_ROOT + "/Reward/Incubator/" + ItemConstants.getNameCityIncubator(itemid) + "/" + types[(int) (chance * types.length)] + ".properties")) {
                    br = new BufferedReader(fl);
                    String[] readSplit = new String[2];
                    String readLine = null;
                    while ((readLine = br.readLine()) != null) {
                        readSplit = readLine.split(" - ");
                        IncubatedItem.put(readSplit[0], readSplit[1]);
                    }  
                }
            br.close(); 
        } catch (IOException e) {
            System.out.println("There was a error with Incubator.");
            FileLogger.printError("Error_Incubator.txt", "Nome - " + ItemConstants.getNameCityIncubator(itemid) + "\r\n" + e);
            return false;
        }
        
        int randomItem = (int) (Math.random() * IncubatedItem.entrySet().size());
        int hmany = 0;
        int itemCode = 0;
        int quantity = 0;
        for (Map.Entry<String, String> entry : IncubatedItem.entrySet()) {
            hmany++;
            if(hmany == randomItem) {
                try {
                    itemCode = Integer.parseInt(entry.getKey());
                    quantity = Integer.parseInt(entry.getValue());
                    break;
                } catch (NumberFormatException e) {
                    System.out.print(e);
                    return false;
                }
            }
        }
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        if (itemCode == 0 || quantity <= 0) {
            return false;
        }
        if (!InventoryManipulator.checkSpace(c, itemCode, quantity, "")) {
            c.getSession().write(PacketCreator.GetInventoryFull());
            c.getSession().write(PacketCreator.GetShowInventoryFull());
            c.getSession().write(PacketCreator.EnableActions());
            return false;
        }
        if (ItemConstants.getInventoryType(itemCode) == InventoryType.EQUIP) {
            InventoryManipulator.addFromDrop(c, ii.randomizeStatsIncuba((Equip) ii.getEquipById(itemCode)), "Obtained through the Incubator", true);
            c.getSession().write(PacketCreator.GetShowItemGain(itemCode, (short) quantity, true));
        } else {
            InventoryManipulator.addById(c, itemCode, (short) quantity, "Obtained through the Incubator", "");
            c.getSession().write(PacketCreator.GetShowItemGain(itemCode, (short) quantity, true));
        }
        return true;
    } 
}
