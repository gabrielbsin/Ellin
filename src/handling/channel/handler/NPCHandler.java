/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.player.Player;
import client.Client;
import client.player.PlayerEffects;
import client.player.inventory.Inventory;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.violation.AutobanManager;
import client.player.violation.CheatingOffense;
import constants.GameConstants;
import constants.ItemConstants;
import constants.MapConstants;
import static handling.channel.handler.ChannelHeaders.NPCHeaders.*;
import handling.mina.PacketReader;
import packet.creators.PacketCreator;
import packet.opcode.SendPacketOpcode;
import packet.transfer.write.WritingPacket;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestScriptManager;
import constants.NPCConstants;
import java.awt.Point;
import packet.creators.EffectPackets;
import scripting.npc.NPCConversationManager;
import server.itens.DueyProcessor;
import server.itens.InventoryManipulator;
import server.itens.ItemInformationProvider;
import server.shops.Shop;
import server.itens.StorageKeeper;
import server.life.MapleLifeFactory;
import server.life.npc.MapleNPC;
import server.quest.MapleQuest;
import tools.FileLogger;

/**
 *
 * @author GabrielSin
 */
public class NPCHandler {

    public static final void NPCTalk(final PacketReader packet, final Client c) {
        int objectNPC = packet.readInt();
        packet.readPos(); 
        final MapleNPC npc = (MapleNPC) c.getPlayer().getMap().getNPCByOid(objectNPC);
        
        if (!c.getPlayer().isAlive()) {
            c.announce(PacketCreator.EnableActions());
            return;
        }

        if (System.currentTimeMillis() - c.getPlayer().getNpcCooldown() < GameConstants.BLOCK_NPC_RACE_CONDT) {
            c.announce(PacketCreator.EnableActions());
            return;
        }
        
        if (GameConstants.USE_DEBUG) c.getPlayer().dropMessage(5, "Talking to NPC " + npc.getId());
        
        for (final int i : NPCConstants.DISABLE_NPCS) {
            if (npc.getId() == i) {
                c.getPlayer().getClient().getSession().write(PacketCreator.GetNPCTalk(i, (byte) 0, String.format(NPCConstants.DISABLE_NPCS_MESSAGE, c.getPlayer().getName()), "00 00"));
                return;
            }
        }
        if (npc.getId() == 9010009) {   //is duey
            c.getPlayer().setNpcCooldown(System.currentTimeMillis());
            DueyProcessor.dueySendTalk(c);
        } else {
            if (c.getCM() != null) {
                NPCScriptManager.getInstance().dispose(c);
            } else if (c.getQM() != null) {
                QuestScriptManager.getInstance().dispose(c);
            } else if (c.getPlayer().getShop() != null) {
                c.getPlayer().setShop(null);
                c.getSession().write(PacketCreator.ConfirmShopTransaction((byte) 20));
            }
            if (npc.hasShop()) {
                npc.sendShop(c);
            } else {
                NPCScriptManager.getInstance().start(c, npc.getId(), null, null);
            }
       }
    }

    public static void NPCMoreTalk(PacketReader packet, Client c) {
        byte lastMsg = packet.readByte();
        byte action = packet.readByte();
        final NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);
        cm.setLastMsg((byte) -1);
        if (lastMsg == 2) {
            if (action != 0) {
                String returnText = packet.readMapleAsciiString();
                if (c.getQM() != null) {
                    c.getQM().setGetText(returnText);
                    if (c.getQM().isStart()) {
                        QuestScriptManager.getInstance().start(c, action, lastMsg, -1);
                    } else {
                        QuestScriptManager.getInstance().end(c, action, lastMsg, -1);
                    }
                } else {
                    c.getCM().setGetText(returnText);
                    NPCScriptManager.getInstance().action(c, action, lastMsg, -1);
                }
            } else if (c.getQM() != null) {
                c.getQM().dispose();
            } else {
                c.getCM().dispose();
            }
        } else if (lastMsg == 6) { // Speed Quiz 
            if (c.getPlayer().getSpeedQuiz() == null) { 
                cm.dispose(); 
                return; 
            } 
            c.getPlayer().getSpeedQuiz().nextRound(c, packet.readMapleAsciiString()); 
        } else {
            int selection = -1;
            if (packet.available() >= 4) {
                selection = packet.readInt();
            } else if (packet.available() > 0) {
                selection = packet.readByte();
            }
            if (lastMsg == 4 && selection == -1) {
                FileLogger.printError(FileLogger.EXPLOITS + c.getPlayer().getName() + ".txt", "[MoreTalkLog]"+c.getPlayer().getName() +  
                " seems to be trying to exploit NPCs in MAPID " + c.getPlayer().getMapId() + " on NPC " + c.getCM()
                + "\r\nPacket sent was " + lastMsg + " " + action + " " +  selection + " \r\n");
                // cm.dispose();
                //return;
            }
            if (c.getQM() != null) {
                if (c.getQM().isStart()) {
                    QuestScriptManager.getInstance().start(c, action, lastMsg, selection);
                } else {
                    QuestScriptManager.getInstance().end(c, action, lastMsg, selection);
                }
            } else if (c.getCM() != null) {
                NPCScriptManager.getInstance().action(c, action, lastMsg, selection);
            }
        }
    } 
    
    /*
     * Special thanks Darter (YungMoozi) for reporting unchecked player position
    */
    private static boolean isNpcNearby(PacketReader packet, Player p, MapleQuest quest, int npcId) {
        Point playerPosition;
        Point pos = p.getPosition();
        
        if (packet.available() >= 4) {
            playerPosition = new Point(packet.readShort(), packet.readShort());
            if (playerPosition.distance(pos) > 1000) { 
                playerPosition = pos;
            }
        } else {
            playerPosition = pos;
        }
        
        if (!quest.isAutoStart() && !quest.isAutoComplete()) {
            MapleNPC npc = p.getMap().getNPCById(npcId);
            if (npc == null) {
                return false;
            }
            
            Point npcPosition = npc.getPosition();
            if (Math.abs(npcPosition.getX() - playerPosition.getX()) > 1200 || Math.abs(npcPosition.getY() - playerPosition.getY()) > 800) {
                p.dropMessage(5, "Approach the NPC to fulfill this quest operation.");
                return false;
            }
        }
        
        return true;
    }
    
    public static final void QuestAction(final PacketReader packet, final Client c) {
        byte action = packet.readByte();
        short questId = packet.readShort();
        Player p = c.getPlayer();
        MapleQuest quest = MapleQuest.getInstance(questId);
        if (p == null || quest == null) {
            return;
        }
        switch (action) {
            case START_QUEST: {
                int npc = packet.readInt();
                if (!isNpcNearby(packet, p, quest, npc)) {
                    return;
                }
                if (quest.canStart(p, npc)) {
                    quest.start(p, npc); 
                }
                break;
            }
            case COMPLETE_QUEST: {
                int npc = packet.readInt();
                if (!isNpcNearby(packet, p, quest, npc)) {
                    return;
                }
                if (!quest.isAutoComplete()) {
                    quest.complete(p, npc, packet.readInt());
                } else {
                    quest.complete(p, npc);
                }
                p.getClient().announce(EffectPackets.ShowSelfQuestComplete());
                p.getMap().broadcastMessage(p, PacketCreator.ShowThirdPersonEffect(p.getId(), PlayerEffects.QUEST_COMPLETE.getEffect()), false);
                break;
            }
            case FORFEIT_QUEST: {
                quest.forfeit(p);
                break;
            }
            case SCRIPT_START_QUEST: {
                int npc = packet.readInt();
                if (!isNpcNearby(packet, p, quest, npc)) {
                    return;
                }
                if (quest.canStart(p, npc)) {
                    QuestScriptManager.getInstance().start(c, questId, npc);
                }
                break;
            }
            case SCRIPT_END_QUEST: {
                int npc = packet.readInt();
                if (!isNpcNearby(packet, p, quest, npc)) {
                    return;
                }
                if (quest.canComplete(p, npc)) {
                    QuestScriptManager.getInstance().end(c, questId, npc);
                }
                p.getClient().announce(EffectPackets.ShowSelfQuestComplete());
                p.getMap().broadcastMessage(p, PacketCreator.ShowThirdPersonEffect(p.getId(), PlayerEffects.QUEST_COMPLETE.getEffect()), false);
                break;
            }
        }
    }

    public static void NPCShop(PacketReader packet, Client c, Player p) {
        if (p == null || p.getMap() == null) {
            return;
        }
        switch(packet.readByte()) {
            case BUY_SHOP: {
                final Shop shop = p.getShop();
                if (shop == null) {
                    return;
                }
                packet.readShort();
                int itemId = packet.readInt();
                short quantity = packet.readShort();
                int price = packet.readInt();
                p.getShop().buy(c, itemId, quantity, price);
                break;
            }
            case SELL_SHOP: {
                final Shop shop = p.getShop();
                if (shop == null) {
                    return;
                }
                short slot = packet.readShort();
                int itemId = packet.readInt();
                InventoryType type = ItemInformationProvider.getInstance().getInventoryType(itemId);
                short quantity = packet.readShort();
                if (quantity < 0) {
                    p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to sell negative quantity to the NPC store.");
                    return;
                }
                p.getShop().sell(c, type, slot, quantity);
                break;
            }
            case RECHARGE_SHOP: {
                final Shop shop = p.getShop();
                if (shop == null) {
                    return;
                }
                byte slot = (byte) packet.readShort();
                p.getShop().recharge(c, slot);
                break;
            }
            case EXIT_SHOP: {
                p.setShop(null);
            }       
        }
    }

    public static void Storage(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        final StorageKeeper storage = p.getStorage();
        final MapleNPC npc = MapleLifeFactory.getNPC(MapConstants.isStorageKeeperMap(c.getPlayer().getMapId()));
        c.lockClient();
        try {
            switch (packet.readByte()) {
                case TAKE_OUT_STORAGE: {
                    byte type = packet.readByte();
                    byte slot = storage.getSlot(InventoryType.getByType(type), packet.readByte());
                    final Item item = storage.takeOut(slot);
                    if (c.getPlayer().getMeso() < npc.getStats().getWithdrawCost()) {
                        p.announce(PacketCreator.GetStorageInsufficientFunds());
                    }
                    if (slot < 0 || slot > storage.getSlots()) { 
                        c.disconnect(true, false);
                        return;      
                    }
                    if (item != null) {
                        if (ItemInformationProvider.getInstance().isPickupRestricted(item.getItemId()) && p.getItemQuantity(item.getItemId(), true) > 0) {
                            p.announce(PacketCreator.GetStorageFull());
                            return;
                        }
                        if (!InventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                            storage.store(item);
                            p.announce(PacketCreator.StorageWithdrawInventoryFull());
                        } else {
                            InventoryManipulator.addFromDrop(c, item, "Taken out from storage by " + p.getName(), false);
                        }
                        p.gainMeso(-npc.getStats().getWithdrawCost(), npc.getStats().getWithdrawCost() > 0);
                        storage.sendTakenOut(c, ItemConstants.getInventoryType(item.getItemId()));
                    } else {
                        AutobanManager.getInstance().autoban(c, p.getName() + " trying to take item from storage that does not exist.");
                    }
                    break;
                }
                case SEND_STORAGE: {
                    final byte slot = (byte) packet.readShort();
                    final int itemId = packet.readInt();
                    short quantity = packet.readShort();
                    final ItemInformationProvider ii = ItemInformationProvider.getInstance();
                    InventoryType slotType = ItemConstants.getInventoryType(itemId);
                    Inventory Inv = p.getInventory(slotType);
                    if (p.getMeso() < npc.getStats().getDepositCost()) {
                        p.announce(PacketCreator.GetStorageInsufficientFunds());
                        return;
                    }
                    if (slot < 1 || slot > Inv.getSlotLimit()) { 
                        p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to store negative or excess amount for deposit.");
                        return;
                    }
                    if (slotType == InventoryType.CASH) {
                        p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to store cash item in the deposit.");
                        return;
                    }
                    if (storage.isFull()) {
                        p.announce(PacketCreator.GetStorageFull());
                        return;
                    }

                    InventoryType type = ii.getInventoryType(itemId);
                    Item item = c.getPlayer().getInventory(type).getItem(slot).copy();
                    if (item == null) {
                        p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "You tried to store non-existing item in the deposit.");
                        return;
                    }

                    if (item.getItemId() == itemId && (item.getQuantity() >= quantity || ItemConstants.isThrowingStar(itemId) || ItemConstants.isBullet(itemId))) {
                        if (ItemConstants.isThrowingStar(itemId) || ItemConstants.isBullet(itemId)) {
                            quantity = item.getQuantity();
                        }
                        p.gainMeso(-npc.getStats().getDepositCost(), false, true, false);
                        InventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
                        item.setQuantity(quantity);
                        storage.store(item);
                    } 
                    storage.sendStored(c, ItemConstants.getInventoryType(itemId));
                    break;
                }
                case ARRANGE_STORAGE: {
                    storage.arrange();
                    storage.update(c);
                    break;
                }
                case SET_MESO_STORAGE: {
                    int meso = packet.readInt();
                    final int storageMesos = storage.getMeso();
                    final int playerMesos = c.getPlayer().getMeso();

                    if ((meso > 0 && storageMesos >= meso) || (meso < 0 && playerMesos >= -meso)) {
                        if (meso < 0 && (storageMesos - meso) < 0) { 
                            meso = -(Integer.MAX_VALUE - storageMesos);
                            if ((-meso) > playerMesos) {
                                return;
                            }
                        } else if (meso > 0 && (playerMesos + meso) < 0) {
                            meso = (Integer.MAX_VALUE - playerMesos);
                            if ((meso) > storageMesos) {
                                return;
                            }
                        }
                        storage.setMeso(storageMesos - meso);
                        p.gainMeso(meso, false, true, false);
                    } else {
                        return;
                    }
                    storage.sendMeso(c);
                    break;
                }
                case CLOSE_STORAGE: {
                    storage.close();
                    break;
                }  
            }
        } finally {
            c.unlockClient();
        }
    }

    public static void NPCAnimation(PacketReader r, Client c) {
        WritingPacket wp = new WritingPacket();
        int length = (int) r.available();
        if (length == 6) { 
            wp.writeShort(SendPacketOpcode.NPC_ACTION.getValue());
            wp.writeInt(r.readInt());
            wp.writeShort(r.readShort());
            c.getSession().write(wp.getPacket());
        } else if (length > 6) {
            byte[] bytes = r.read(length - 9);
            wp.writeShort(SendPacketOpcode.NPC_ACTION.getValue());
            wp.write(bytes);
            c.getSession().write(wp.getPacket());
        }
    }
    
    public static void Duey(PacketReader packet, Client c) {
        if (!GameConstants.USE_DUEY){
            c.announce(PacketCreator.EnableActions());
            return;
    	}
            
        byte operation = packet.readByte();
        if (operation == DueyProcessor.Actions.TOSERVER_SEND_ITEM.getCode()) {
            byte inventId = packet.readByte();
            short itemPos = packet.readShort();
            short amount = packet.readShort();
            int mesos = packet.readInt();
            String recipient = packet.readMapleAsciiString();
            
            DueyProcessor.dueySendItem(c, inventId, itemPos, amount, mesos, recipient);
        } else if (operation == DueyProcessor.Actions.TOSERVER_REMOVE_PACKAGE.getCode()) {
            int packageid = packet.readInt();
            
            DueyProcessor.dueyRemovePackage(c, packageid);
        } else if (operation == DueyProcessor.Actions.TOSERVER_CLAIM_PACKAGE.getCode()) {
            int packageid = packet.readInt();
            
            DueyProcessor.dueyClaimPackage(c, packageid);
        }
    }
}
