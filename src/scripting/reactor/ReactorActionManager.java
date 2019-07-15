/*
 * This file is part of the OdinMS Maple Story Server
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

package scripting.reactor;

import java.awt.Point;
import java.util.List;

import client.Client;
import client.player.Player;
import client.player.inventory.Equip;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import constants.ItemConstants;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import packet.creators.PacketCreator;
import scripting.AbstractPlayerInteraction;
import scripting.event.EventManager;
import server.itens.ItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.npc.MapleNPC;
import server.maps.Field;
import server.maps.FieldMonitor;
import server.maps.reactors.Reactor;
import server.maps.reactors.ReactorDropEntry;
import tools.TimerTools.MiscTimer;

/*
 * Drops functions  - ronancpl (HeavenMS)
*/

public class ReactorActionManager extends AbstractPlayerInteraction {
    
    private Reactor reactor;
    private ScheduledFuture<?> sprayTask = null;

    public ReactorActionManager(Client c, Reactor reactor) {
        super(c);
        this.reactor = reactor;
    }
    
    public void destroyNpc(int npcId) {
        reactor.getMap().destroyNPC(npcId);
    }
    
    public void sprayItems() {
        sprayItems(false, 0, 0, 0, 0);
    }

    public void sprayItems(boolean meso, int mesoChance, int minMeso, int maxMeso) {
        sprayItems(meso, mesoChance, minMeso, maxMeso, 0);
    }
    
    public void sprayItems(boolean meso, int mesoChance, int minMeso, int maxMeso, int minItems) {
        sprayItems((int)reactor.getPosition().getX(), (int)reactor.getPosition().getY(), meso, mesoChance, minMeso, maxMeso, minItems);
    }
    
    public void sprayItems(int posX, int posY, boolean meso, int mesoChance, int minMeso, int maxMeso, int minItems) {
        dropItems(true, posX, posY, meso, mesoChance, minMeso, maxMeso, minItems);
    }

    public void dropItems() {
        dropItems(false, 0, 0, 0, 0);
    }

    public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso) {
        dropItems(meso, mesoChance, minMeso, maxMeso, 0);
    }
    
    public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso, int minItems) {
        dropItems((int) reactor.getPosition().getX(), (int) reactor.getPosition().getY(), meso, mesoChance, minMeso, maxMeso, minItems);
    }

    public void dropItems(int posX, int posY, boolean meso, int mesoChance, int minMeso, int maxMeso, int minItems) {
        dropItems(false, posX, posY, meso, mesoChance, minMeso, maxMeso, minItems);
    }
    
    public void dropItems(boolean delayed, int posX, int posY, boolean meso, int mesoChance, final int minMeso, final int maxMeso, int minItems) {
        if (c.getPlayer() == null) {
            return;
        }
        
        List<ReactorDropEntry> items = generateDropList(getDropChances(), c.getChannelServer().getDropRate(), meso, mesoChance, minItems);
        
        if (items.size() % 2 == 0) {
            posX -= 12;
        }
        final Point dropPos = new Point(posX, posY);
        
        if (!delayed) {
            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            
            byte p = 1;
            for (ReactorDropEntry d : items) {
                dropPos.x = (int) (posX + ((p % 2 == 0) ? (25 * ((p + 1) / 2)) : -(25 * (p / 2))));
                p++;
                
                if (!ii.isItemValid(d.itemId)) {
                    System.out.println("[REMOVER - ReactorDrops] Item invalido: " + d.itemId);
                    continue;
                }
                
                
                
                if (d.itemId == 0) {
                    int range = maxMeso - minMeso;
                    int displayDrop = (int) (Math.random() * range) + minMeso;
                    int mesoDrop = (displayDrop * c.getChannelServer().getMesoRate());
                    reactor.getMap().spawnMesoDrop(mesoDrop, reactor.getMap().calcDropPos(dropPos, reactor.getPosition()), reactor, c.getPlayer(), false, (byte) 2);
                } else {
                    Item drop;
                    
                    if (!ii.isItemValid(d.itemId)) {
                        int invalidItem = d.itemId;
                        String sql = "DELETE FROM reactordrops WHERE reactorid = ? AND itemid = " + invalidItem;
                        Connection con = DatabaseConnection.getConnection();
                        try (PreparedStatement ps = con.prepareStatement(sql)) {
                            ps.setInt(1, reactor.getId());
                            ps.executeUpdate();
                        } catch (SQLException ex) {
                            Logger.getLogger(Field.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        System.out.println("[REACTOR] Item removido da DB {react: " + reactor.getId() + "} | {item: " + invalidItem + "}");
                    }
                    
                    if (ItemConstants.getInventoryType(d.itemId) != InventoryType.EQUIP) {
                        drop = new Item(d.itemId, (short) 0, (short) 1);
                    } else {
                        drop = ii.randomizeStats((Equip) ii.getEquipById(d.itemId));
                    }

                    reactor.getMap().dropFromReactor(getPlayer(), reactor, drop, dropPos, (short) d.questid);
                }
            }
        } else {
            final Player p = c.getPlayer();
            final Reactor r = reactor;
            final List<ReactorDropEntry> dropItems = items;
            final int worldMesoRate = c.getChannelServer().getMesoRate();
            
            dropPos.x -= (12 * items.size());
            
            sprayTask = MiscTimer.getInstance().register(() -> {
                if(dropItems.isEmpty()) {
                    sprayTask.cancel(false);
                    return;
                }
                
                ReactorDropEntry d = dropItems.remove(0);
                if (d.itemId == 0) {
                    int range = maxMeso - minMeso;
                    int displayDrop = (int) (Math.random() * range) + minMeso;
                    int mesoDrop = (displayDrop * worldMesoRate);
                    r.getMap().spawnMesoDrop(mesoDrop, r.getMap().calcDropPos(dropPos, r.getPosition()), r, p, false, (byte) 2);
                } else {
                    Item drop;
                    
                    if (ItemConstants.getInventoryType(d.itemId) != InventoryType.EQUIP) {
                        drop = new Item(d.itemId, (short) 0, (short) 1);
                    } else {
                        ItemInformationProvider ii = ItemInformationProvider.getInstance();
                        drop = ii.randomizeStats((Equip) ii.getEquipById(d.itemId));
                    }
                    
                    r.getMap().dropFromReactor(getPlayer(), r, drop, dropPos, (short) d.questid);
                }
                
                dropPos.x += 25;
            }, 100);
        }
    }
    
    private List<ReactorDropEntry> getDropChances() {
        return ReactorScriptManager.getInstance().getDrops(reactor.getId());
    }
    
    private static List<ReactorDropEntry> generateDropList(List<ReactorDropEntry> drops, int dropRate, boolean meso, int mesoChance, int minItems) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        
        List<ReactorDropEntry> items = new ArrayList<>();
        List<ReactorDropEntry> questItems = new ArrayList<>();
        int numItems = 0;
        
        if (meso && Math.random() < (1 / (double) mesoChance)) {
            items.add(new ReactorDropEntry(0, mesoChance, -1));
        }
        
        for (ReactorDropEntry mde : drops) {
            if (Math.random() < (dropRate / (double) mde.chance)) {
                if(!ii.isQuestItem(mde.itemId)) {
                    items.add(mde);
                } else {
                    questItems.add(mde);
                }
                
                numItems++;
            }
        }
        
        while (numItems < minItems) {
            items.add(new ReactorDropEntry(0, mesoChance, -1));
            numItems++;
        }
        
        java.util.Collections.shuffle(items);
        java.util.Collections.shuffle(questItems);
        
        items.addAll(questItems);
        return items;
    }

    @Override
    public EventManager getEventManager(String event) {
        return getClient().getChannelServer().getEventSM().getEventManager(event);
    }

    public void spawnZakum(int id) {
        reactor.getMap().spawnZakum(MapleLifeFactory.getMonster(id), getPosition());
    }

    public void spawnMonster(int id) {
        spawnMonster(id, 1, getPosition());
    }

    public void spawnMonster(int id, int x, int y) {
        spawnMonster(id, 1, new Point(x, y));
    }

    public void spawnMonster(int id, int qty) {
        spawnMonster(id, qty, getPosition());
    }

    public void spawnMonster(int id, int qty, int x, int y) {
        spawnMonster(id, qty, new Point(x, y));
    }

    private void spawnMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            reactor.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(id), pos);
        }
    }

    public Point getPosition() {
        Point pos = reactor.getPosition();
        pos.y -= 10;
        return pos;
    }

    public void spawnNpc(int npcId) {
        spawnNpc(npcId, getPosition());
    }

    public void spawnNpc(int npcId, int x, int y) {
        spawnNpc(npcId, new Point(x,y));
    }
    
    public void hitMonsterWithReactor(int id, int hitsToKill) { 
        MapleMonster mm = reactor.getMap().getMonsterById(id);
        if(mm != null) {
            int damage = (int)Math.ceil(mm.getMaxHp() / hitsToKill);
            reactor.getMap().damageMonster(this.getPlayer(), mm, damage);
        }
    }
	
    public void spawnNpc(int npcId, Point pos) {
        MapleNPC npc = MapleLifeFactory.getNPC(npcId);
        if (npc != null && !npc.getName().equals("MISSINGNO")) {
            npc.setPosition(pos);
            npc.setCy(pos.y);
            npc.setRx0(pos.x + 50);
            npc.setRx1(pos.x - 50);
            npc.setFh(reactor.getMap().getFootholds().findBelow(pos).getId());
            npc.getStats().setCustom(true);
            reactor.getMap().addMapObject(npc);  
            reactor.getMap().broadcastMessage(PacketCreator.SpawnNPC(npc));
        }
    }
	
    public Reactor getReactor() {
        return reactor;
    }

    public void spawnFakeMonster(int id) {
        spawnFakeMonster(id, 1, getPosition());
    }

    public void spawnFakeMonster(int id, int x, int y) {
        spawnFakeMonster(id, 1, new Point(x, y));
    }

    public void spawnFakeMonster(int id, int qty) {
        spawnFakeMonster(id, qty, getPosition());
    }

    public void spawnFakeMonster(int id, int qty, int x, int y) {
        spawnFakeMonster(id, qty, new Point(x, y));
    }

    private void spawnFakeMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            final MapleMonster mob = MapleLifeFactory.getMonster(id);
            reactor.getMap().spawnFakeMonsterOnGroundBelow(mob, pos);
        }
    }

    public void killAll() {
        reactor.getMap().killAllMonsters();
    }

    public void killMonster(int monsId) {
        reactor.getMap().killMonster(monsId);
    }

    @Override
    public Client getClient() {
        return c;
    }

    public void closeDoor(int mapid) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).setReactorState();
    }

    public void openDoor(int mapid) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).resetReactors();
    }

    public void createMapMonitor(int mapId, String portal) {
        new FieldMonitor(c.getChannelServer().getMapFactory().getMap(mapId), portal);
    }
}
