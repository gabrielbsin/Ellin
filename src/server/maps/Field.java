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
package server.maps;
import server.maps.object.FieldObjectType;
import server.maps.object.FieldObject;
import server.maps.reactors.Reactor;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import client.Client;
import community.MaplePartyOperation;
import server.life.status.MonsterStatus;
import server.life.status.MonsterStatusEffect;
import constants.GameConstants;
import constants.MapConstants;
import packet.transfer.write.OutPacket;
import handling.channel.ChannelServer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import packet.creators.EffectPackets;
import packet.creators.MonsterPackets;
import packet.creators.PacketCreator;
import packet.creators.PartyPackets;
import packet.creators.PetPackets;
import client.player.Player;
import client.player.buffs.BuffStat;
import client.player.inventory.Equip;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemPet;
import client.player.violation.CheatingOffense;
import constants.ItemConstants;
import database.DatabaseConnection;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import scripting.event.EventInstanceManager;
import server.MapleStatEffect;
import tools.TimerTools.MapTimer;
import server.PropertiesTable;
import server.itens.ItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MonsterDropEntry;
import server.life.MonsterGlobalDropEntry;
import server.life.npc.MapleNPC;
import server.life.SpawnPoint;
import server.life.components.SelfDestruction;
//import server.life.SpawnPointAreaBoss;
import server.maps.object.FieldDoorObject;
import tools.Pair;
import server.maps.portal.Portal;
import server.partyquest.mcpq.MCWZData;
import tools.FileLogger;
import tools.Randomizer;

public class Field {

    private Map<Integer, FieldObject> mapObjects = new LinkedHashMap<>();
    private final Collection<Player> characters = new LinkedHashSet<>();
    private Collection<SpawnPoint> monsterSpawn = Collections.synchronizedList(new LinkedList<SpawnPoint>());
    private Collection<SpawnPoint> allMonsterSpawn = Collections.synchronizedList(new LinkedList<SpawnPoint>());
    private Map<Integer, Set<Integer>> mapParty = new LinkedHashMap<>();
    private final AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
    private final Map<Integer, Portal> portals = new HashMap<>();
    private final List<Rectangle> areas = new ArrayList<>();
    private MapleFootholdTree footholds = null;
    private AtomicInteger runningOid = new AtomicInteger(100);
    private final int mapId;
    private int fieldLimit = 0;
    private int lastDoorOwner = -1;
    private int forcedReturnMap =  MapConstants.NULL_MAP;
    private final int dropLife = 180000; 
    private int decHPInterval = 10000;
    private int timeLimit;
    private int decHP = 0;
    private int protectItem = 0;
    private int returnFieldId;
    private final int channel;
    private float monsterRate;
    private boolean clock;
    private boolean boat;
    private boolean town;
    protected boolean swim;
    private boolean docked = false;
    private boolean timer = false;
    private boolean respawning = true;
    private boolean disablePortal = false;
    private boolean disableInvincibilitySkills = false;
    private boolean disableDamage = false;
    private boolean disableChat = false;
    private boolean dropsDisabled = false, isSpawns = true, everlast = false;
    public boolean respawnMonsters = true;
    public boolean gDropsDisabled = false;
    private long lastHurtTime = 0;
    private short mobInterval = 5000;
    private String mapName, streetName;
    private FieldEffect mapEffect = null;
    private FieldTimer mapTimer = null;
    private ScheduledFuture<?> sfme = null;
    private Pair<Integer, String> timeMob = null;
    private ScheduledFuture<?> mapMonitor = null;
    private EventInstanceManager event = null;
    private MCWZData mcpqData;
    private final PropertiesTable properties;
    private boolean allowSummons = true; 
    private Map<FieldItem, Long> droppedItems = new LinkedHashMap<>();
    private LinkedList<WeakReference<FieldObject>> registeredDrops = new LinkedList<>();
    // [HPQ]
    private int riceCakes = 0;
    private int bunnyDamage = 0;
    public Map<Integer, Integer> reactorLink = new HashMap<>();
    // [Locks]
    private final ReadLock chrRLock;
    private final WriteLock chrWLock;
    private final ReadLock objectRLock;
    private final WriteLock objectWLock;
  
    public Field(final int mapid, final int channel, final int returnMapId, final float monsterRate) {
        this.mapId = mapid;
        this.channel = channel;
        this.returnFieldId = returnMapId;
        if (this.returnFieldId == MapConstants.NULL_MAP) {
            this.returnFieldId = mapid;
        }
        this.monsterRate = (byte) Math.round(monsterRate);
        if (this.monsterRate == 0) {
            this.monsterRate = 1;
        }
        this.properties = new PropertiesTable();
        
        properties.setProperty("mute", Boolean.FALSE);
        
        final ReentrantReadWriteLock chrLock = new ReentrantReadWriteLock(true);
        chrRLock = chrLock.readLock();
        chrWLock = chrLock.writeLock();

        final ReentrantReadWriteLock objectLock = new ReentrantReadWriteLock(true);
        objectRLock = objectLock.readLock();
        objectWLock = objectLock.writeLock();
    }
    
    public ReadLock getCharacterReadLock() {
        return chrRLock;
    }
    
    public WriteLock getCharacterWriteLock() {
        return chrWLock;
    }
    
    public final void setSpawns(final boolean fm) {
        this.isSpawns = fm;
    }
    
    public final boolean getSpawns() {
        return isSpawns;
    }

    public final boolean canSpawn() {
        return isSpawns;
    }
 
    public PropertiesTable getProperties() {
        return this.properties;
    }

    public void setMonsterRate(float monsterRate) {
       this.monsterRate = monsterRate;
    }
    
    public float getMonsterRate() {
	return monsterRate;
    }
    
    public int getChannel() {
        return channel;
    }
    
    public boolean canDelete() {
        return this.characters.isEmpty() && ChannelServer.getInstance(getChannel()).getMapFactory().isMapLoaded(mapId);
    }
    
    public void setFieldLimit(int fieldLimit) {
        this.fieldLimit = fieldLimit;
    }

    public int getFieldLimit() {
        return fieldLimit;
    }
    
    public final void toggleGDrops() {
        this.gDropsDisabled = !gDropsDisabled;
    }

    public void clearDrops() {
        for (FieldObject i : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.ITEM))) {
            removeMapObject(i);
        }
    }

    public void killFriendlies(MapleMonster mob) {
        this.killMonster(mob, (Player) getAllPlayer().get(0), false);
    }
       
    public void killAllMonstersNotFriendly() {
        for (FieldObject monstermo : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.MONSTER))) {
            MapleMonster monster = (MapleMonster) monstermo;
            if (monster.getStats().isFriendly()) {
                continue;
            }
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            broadcastMessage(MonsterPackets.KillMonster(monster.getObjectId(), true), monster.getPosition());
            removeMapObject(monster);
        }
    }
    
    public boolean toggleDrops() {
        dropsDisabled = !dropsDisabled;
        return dropsDisabled;
    }

    public int getId() {
        return mapId;
    }
        
    public Field getReturnField() {
        if (returnFieldId == MapConstants.NULL_MAP) return null;
            try {
                return ChannelServer.getInstance(channel).getMapFactory().getMap(returnFieldId);
            } catch (Exception ex) {
            return null;
        }
    }

    public int getReturnMapId() {
        return returnFieldId;
    }

    public int getForcedReturnId() {
        return forcedReturnMap;
    }

    public Field getForcedReturnField() {
        if (forcedReturnMap == MapConstants.NULL_MAP) return null;
        return ChannelServer.getInstance(channel).getMapFactory().getMap(forcedReturnMap);
    }

    public void setForcedReturnField(int map) {
        this.forcedReturnMap = map;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public final int getCurrentPartyId() {
        chrRLock.lock();
        try {
            final Iterator<Player> ltr = characters.iterator();
            Player p;
            while (ltr.hasNext()) {
                p = ltr.next();
                if (p.getPartyId() != -1) {
                    return p.getPartyId();
                }
            }
        } finally {
           chrRLock.unlock();
        }
        return -1;
    }
    
    public void addMapObject(FieldObject mapobject) {
        objectWLock.lock();
        try {
            int curOID = getUsableOID();
            mapobject.setObjectId(curOID);
            this.mapObjects.put(curOID, mapobject);
        } finally {
            objectWLock.unlock();
        }
    }
    
    private int getUsableOID() {
        objectRLock.lock();
        try {
            Integer curOid;
            
            do {
                if ((curOid = runningOid.incrementAndGet()) < 0) {
                    runningOid.set(curOid = 1000000001);
                }
            } while (mapObjects.containsKey(curOid));
            
            return curOid;
        } finally {
            objectRLock.unlock();
        }
    }
    
    private void spawnAndAddRangedMapObject(FieldObject mapobject, DelayedPacketCreation packetbakery) {
        spawnAndAddRangedMapObject(mapobject, packetbakery, null);
    }
    
    private void spawnAndAddRangedMapObject(FieldObject mapobject, DelayedPacketCreation packetbakery, SpawnCondition condition) {
        chrRLock.lock();
        objectWLock.lock();
        try {
            int curOID = getUsableOID();
            mapobject.setObjectId(curOID);
            this.mapObjects.put(curOID, mapobject);
            for (Player p : characters) {
                if (condition == null || condition.canSpawn(p)) {
                    if (p.getPosition().distanceSq(mapobject.getPosition()) <= getRangedDistance()) {
                        packetbakery.sendPackets(p.getClient());
                        p.addVisibleMapObject(mapobject);
                    }
                }
            }
        } finally {
            objectWLock.unlock();
            chrRLock.unlock();
        }
    }

    public void removeMapObject(int num) {
        objectWLock.lock();
        try {
            this.mapObjects.remove(Integer.valueOf(num));
        } finally {
            objectWLock.unlock();
        }
    }

    public void removeMapObject(final FieldObject obj) {
        removeMapObject(obj.getObjectId());
    }

    private static double getRangedDistance() {
        return(MapConstants.USE_MAXRANGE ? Double.POSITIVE_INFINITY : 722500);
    }
    
    private Point calcPointBelow(Point initial) {
        MapleFoothold fh = footholds.findBelow(initial);
        if (fh == null) {
            return null;
        }
        int dropY = fh.getY1();
        if (!fh.isWall() && fh.getY1() != fh.getY2()) {
            double s1 = Math.abs(fh.getY2() - fh.getY1());
            double s2 = Math.abs(fh.getX2() - fh.getX1());
            double s5 = Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2)));
            if (fh.getY2() < fh.getY1()) {
                dropY = fh.getY1() - (int) s5;
            } else {
                dropY = fh.getY1() + (int) s5;
            }
        }
        return new Point(initial.x, dropY);
    }

    public final Point calcDropPos(final Point initial, final Point fallback) {
        final Point ret = calcPointBelow(new Point(initial.x, initial.y - 50));
        if (ret == null) {
            return fallback;
        }
        return ret;
    }

    public void setReactorState() {
        chrRLock.lock();
        objectRLock.lock();
        try {
            for (FieldObject o : mapObjects.values()) {
                if (o.getType() == FieldObjectType.REACTOR) {
                    if (((Reactor) o).getState() < 1) {
                        Reactor mr = (Reactor) o;
                        mr.lockReactor();
                        try {
                            mr.setState((byte) 1);
                            broadcastMessage(PacketCreator.TriggerReactor((Reactor) o, 1));
                        } finally {
                            mr.unlockReactor();
                        }
                    }
                }
            }
        } finally {
            objectRLock.unlock();
            chrRLock.unlock();
        }
    }
    
    public void setReactorState(final byte state) {
        chrRLock.lock();
        objectRLock.lock();
        try {
            for (FieldObject o : mapObjects.values()) {
                if (o.getType() == FieldObjectType.REACTOR) {
                    if (((Reactor) o).getState() < 1) {
                        Reactor mr = (Reactor) o;
                        mr.lockReactor();
                        try {
                            mr.forceHitReactor((byte) state);
                            broadcastMessage(PacketCreator.TriggerReactor((Reactor) o, 1));
                        } finally {
                            mr.unlockReactor();
                        }
                    }
                }
            }
        } finally {
            objectRLock.unlock();
            chrRLock.unlock();
        }
    }
    
    public void setReactorState(Reactor reactor, byte state) {
        chrRLock.lock();
        objectRLock.lock();
        try {
            for (FieldObject o : mapObjects.values()) {
                if (o.getType() == FieldObjectType.REACTOR) {
                    if (reactor.getState() < 1) {
                        reactor.lockReactor();
                        try {
                            reactor.forceHitReactor((byte) state);
                            broadcastMessage(PacketCreator.TriggerReactor((Reactor) reactor, 1));
                        } finally {
                            reactor.unlockReactor();
                        }
                    }
                }
            }
        } finally {
            objectRLock.unlock();
            chrRLock.unlock();
        }
    }
    
    private void dropFromMonster(final Player p, final MapleMonster mob) {
        if (mob == null || p == null || ChannelServer.getInstance(channel) == null || dropsDisabled || mob.dropsDisabled()) { 
            return;
        }
        
        if (itemCount() >= MapConstants.MAX_ITEMS) {
            removeDrops();
        }
        
        byte d = 1;
        final byte droptype = (byte) (mob.getStats().isExplosive() ? 3 : mob.getStats().isPublicReward() ? 2 : p.getParty() != null ? 1 : 0);
      
        int mobPos = mob.getPosition().x;
        int dropRate = ChannelServer.getInstance(channel).getDropRate();
        
        Point pos = new Point(0, mob.getPosition().y);
        Map<MonsterStatus, MonsterStatusEffect> stati = mob.getStati();
        
        if (stati.containsKey(MonsterStatus.TAUNT_2)) {
            dropRate *= (stati.get(MonsterStatus.TAUNT_2).getStati().get(MonsterStatus.TAUNT_2).doubleValue() / 100.0 + 1.0);
        }

        final MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
        
        final List<MonsterDropEntry>  dropEntry = new ArrayList<>();
        final List<MonsterDropEntry> visibleQuestEntry = new ArrayList<>();
        final List<MonsterDropEntry> otherQuestEntry = new ArrayList<>();
        
        sortDropEntries(mi.retrieveEffectiveDrop(mob.getId()), dropEntry, visibleQuestEntry, otherQuestEntry, p);
        
        // Normal Drops
        d = dropItemsFromMonsterOnMap(dropEntry, pos, d, dropRate, droptype, mobPos, p, mob);
        
        //  Global Drops
        final List<MonsterGlobalDropEntry> globalEntry = mi.getGlobalDrop();
        d = dropGlobalItemsFromMonsterOnMap(p.getEventInstance() != null, globalEntry, pos, d, droptype, mobPos, p, mob);
        
        // Quest Drops
        d = dropItemsFromMonsterOnMap(visibleQuestEntry, pos, d, dropRate, droptype, mobPos, p, mob);
        dropItemsFromMonsterOnMap(otherQuestEntry, pos, d, dropRate, droptype, mobPos, p, mob);
    }
    
    private byte dropItemsFromMonsterOnMap(List<MonsterDropEntry> dropEntry, Point pos, byte d, int chRate, byte droptype, int mobpos, Player chr, MapleMonster mob) {
        if(dropEntry.isEmpty()) {
            return d;
        }
        
        Collections.shuffle(dropEntry);
        
        Item idrop;
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        
        for (final MonsterDropEntry de : dropEntry) {
            int dropChance = (int) Math.min((float) de.chance * chRate, Integer.MAX_VALUE);
            
            if (Randomizer.nextInt(999999) < dropChance) {
                if (droptype == 3) {
                    pos.x = (int) (mobpos + ((d % 2 == 0) ? (40 * ((d + 1) / 2)) : -(40 * (d / 2))));
                } else {
                    pos.x = (int) (mobpos + ((d % 2 == 0) ? (25 * ((d + 1) / 2)) : -(25 * (d / 2))));
                }
                if (de.itemId == 0) { // meso
                    int mesos = Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum;

                    if (mesos > 0) {
                        if (chr.getBuffedValue(BuffStat.MESOUP) != null) {
                            mesos = (int) (mesos * chr.getBuffedValue(BuffStat.MESOUP).doubleValue() / 100.0);
                        }
                        if(mesos <= 0) mesos = Integer.MAX_VALUE;
                        
                        spawnMesoDrop(mesos, calcDropPos(pos, mob.getPosition()), mob, chr, false, droptype);
                    }
                } else {
                    if (!ii.isItemValid(de.itemId)) {
                        int invalidItem = de.itemId;
                        String sql = "DELETE FROM drop_data WHERE dropperid = ? AND itemid = " + invalidItem;
                        Connection con = DatabaseConnection.getConnection();
                        try (PreparedStatement ps = con.prepareStatement(sql)) {
                            ps.setInt(1, de.MonsterId);
                            ps.executeUpdate();
                        } catch (SQLException ex) {
                            Logger.getLogger(Field.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        System.out.println("[DROP] Item removido da DB {mob: " + de.MonsterId + "} | {item: " + invalidItem + "}");
                    }
                    if (ItemConstants.getInventoryType(de.itemId) == InventoryType.EQUIP) {
                        idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                    } else {
                        idrop = new Item(de.itemId, (short) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1));
                    }
                    spawnDrop(idrop, calcDropPos(pos, mob.getPosition()), mob, chr, droptype, de.questid);
                }
                d++;
            }
        }
        return d;
    }
    
    private byte dropGlobalItemsFromMonsterOnMap(boolean event, List<MonsterGlobalDropEntry> globalEntry, Point pos, byte d, byte droptype, int mobpos, Player p, MapleMonster mob) {
        Collections.shuffle(globalEntry);
        
        Item idrop;
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        
        for (final MonsterGlobalDropEntry de : globalEntry) {
            if (Randomizer.nextInt(999999) < de.chance) {
                if (droptype == 3) {
                    pos.x = (int) (mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2))));
                } else {
                    pos.x = (int) (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))));
                }
                if (de.itemId != 0 && !event) {
                    if (ItemConstants.getInventoryType(de.itemId) == InventoryType.EQUIP) {
                        idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                    } else {
                        idrop = new Item(de.itemId, (short) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1));
                    }
                    spawnDrop(idrop, calcDropPos(pos, mob.getPosition()), mob, p, droptype, de.questid);
                    d++;
                }
            }
        }
        return d;
    }
    
    private static void sortDropEntries(List<MonsterDropEntry> from, List<MonsterDropEntry> item, List<MonsterDropEntry> visibleQuest, List<MonsterDropEntry> otherQuest, Player p) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        
        for (MonsterDropEntry mde : from) {
            if (!ii.isQuestItem(mde.itemId)) {
                item.add(mde);
            } else {
                if (p.needQuestItem(mde.questid, mde.itemId)) {
                    visibleQuest.add(mde);
                } else {
                    otherQuest.add(mde);
                }
            }
        }
    }
    
    public final void removeDrops() {
        List<FieldItem> items = this.getAllItemsThreadsafe();
        for (FieldItem i : items) {
            i.expire(this);
        }
    }

    private int countDrops(List<Integer> theDrop, int dropID) {
        int count = 0;
        for (int i = 0; i < theDrop.size(); i++) {
            if (theDrop.get(i) == dropID) {
                count++;
            }
        }
        return count;
    }
    
    public int countReactorsOnField() {
        int count = 0;
        for (FieldObject m : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.REACTOR))) {
            Reactor reactor = (Reactor) m;
            if (reactor instanceof Reactor) {
                count++;
            }
        }
        return count;
    }

    public int countMobOnField() {
        int count = 0;
        for (FieldObject m : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.MONSTER))) {
            MapleMonster mob = (MapleMonster) m;
            if (mob instanceof MapleMonster) {
                count++;
            }
        }
        return count;
    }
    
    public int countMobOnField(int id) {
        int count = 0;
        for (FieldObject m : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.MONSTER))) {
            MapleMonster mob = (MapleMonster) m;
            if (mob.getId() == id) {
                count++;
            }
        }
        return count;
    }

    public int countMonster(Player p) {
        Field field = p.getClient().getPlayer().getMap();
        double range = Double.POSITIVE_INFINITY;
        List<FieldObject> monsters = field.getMapObjectsInRange(p.getClient().getPlayer().getPosition(), range, Arrays.asList(FieldObjectType.MONSTER));
        return monsters.size();
    }
    
    public void warpField(Field field) {
        synchronized (characters) {
            for (Player p : this.characters) {
                if (p.isAlive()) {
                    p.changeMap(field, field.getPortal(0));
                } else {
                    p.changeMap(p.getMap().getReturnField(), field.getPortal(0));
                }
            }
        }
    }
    
    public final int getNumPlayersInArea(final int index) {
        return getNumPlayersInRect(getArea(index));
    }

    public final int getNumPlayersInRect(final Rectangle rect) {
        int ret = 0;

        chrRLock.lock();
        try {
            final Iterator<Player> ltr = characters.iterator();
            while (ltr.hasNext()) {
                if (rect.contains(ltr.next().getPosition())) {
                    ret++;
                }
            }
        } finally {
            chrRLock.unlock();
        }
        return ret;
    }
    
    public final int getNumPlayersItemsInArea(final int index) {
        return getNumPlayersItemsInRect(getArea(index));
    }

    public final int getNumPlayersItemsInRect(final Rectangle rect) {
        int retP = getNumPlayersInRect(rect);
        int retI = getMapObjectsInBox(rect, Arrays.asList(FieldObjectType.ITEM)).size();

        return retP + retI;
    }
       
    public void spawnMonsterOnGroudBelow(MapleMonster mob, Point pos) {
        spawnMonsterOnGroundBelow(mob, pos);
    }

    public void spawnMonsterOnGroundBelow(MapleMonster mob, Point pos) {
        Point spos = getGroundBelow(pos);
        mob.setPosition(spos);
        spawnMonster(mob);
    }
    
    public void spawnMonsterOnGroundBelow(int mobid, int x, int y) {
        MapleMonster mob = MapleLifeFactory.getMonster(mobid);
        if (mob != null) {
            Point point = new Point(x, y);
            spawnMonsterOnGroundBelow(mob, point);
        }
    }
    
    public void spawnMonsterOnGroudBelowXY(int x, int y, int mobid) {
        MapleMonster mob = MapleLifeFactory.getMonster(mobid);
        if (mob != null) {
            Point point = new Point(x, y);
            spawnMonsterOnGroundBelow(mob, point);
        }
    }
    
    public void spawnMonsterOnGroundBelow(int mobid, int x, int y, String msg) {
        MapleMonster mob = MapleLifeFactory.getMonster(mobid);
        if (mob != null) {
            Point point = new Point(x, y);
            spawnMonsterOnGroundBelow(mob, point);
            this.broadcastMessage(PacketCreator.ServerNotice(6, msg));
        }
    }
                
    public Point getGroundBelow(Point pos) {
        Point spos = new Point(pos.x, pos.y - 7);
        spos = calcPointBelow(spos);
        spos.y--;
        return spos;
    }
       
    public void spawnZakum(MapleMonster mob, Point pos) {
    	spawnFakeMonsterOnGroundBelow(new MapleMonster(mob), pos);
        ArrayList<Integer> theList = new ArrayList<>(8);
        theList.addAll(Arrays.asList(8800003, 8800004, 8800005, 8800006, 8800007, 8800008, 8800009, 8800010));
        for (int mid : theList) {
             MapleMonster monsterid = MapleLifeFactory.getMonster(mid);
             spawnMonsterOnGroundBelow(monsterid, pos);
        }
    }
    
    public List<FieldObject> getMapObjects() {
        objectRLock.lock();
        try {
            return new LinkedList(mapObjects.values());
        }
        finally {
            objectRLock.unlock();
        }
    }

    public boolean damageMonster(Player p, MapleMonster monster, int damage) {
        if (monster != null) {
            if (monster.getId() == 8800000) {
                for (FieldObject object : p.getMap().getMapObjects()) {
                    MapleMonster mons = p.getMap().getMonsterByOid(object.getObjectId());
                    if (mons != null && mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                        return true;
                    }
                }
            }
            if (monster.isAlive()) {
                boolean killMonster = false;
                monster.lockMonster();
                try {
                    if (!monster.isAlive()) {
                        return false;
                    }
                    if (damage > 0) {
                        int monsterhp = monster.getHp();
                        monster.damage(p, damage);
                        if (!monster.isAlive()) { 
                            killMonster(monster, p, true);
                            if (monster.getId() >= 8810002 && monster.getId() <= 8810009) {
                                for (FieldObject mmo : p.getMap().getMapObjects()) {
                                    MapleMonster mons = p.getMap().getMonsterByOid(mmo.getObjectId());
                                    if (mons != null) {
                                        if (mons.getId() == 8810018) {
                                            damageMonster(p, mons, monsterhp);
                                        }
                                    }
                                }
                            }
                        } else {
                            if (monster.getId() >= 8810002 && monster.getId() <= 8810009) {
                                for (FieldObject mmo : p.getMap().getMapObjects()) {
                                     MapleMonster mons = p.getMap().getMonsterByOid(mmo.getObjectId());
                                    if (mons != null) {
                                        if (mons.getId() == 8810018) {
                                            damageMonster(p, mons, damage);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    monster.unlockMonster();
                } 
                if (monster.getStats().selfDestruction() != null && monster.getStats().selfDestruction().getHp() > -1) {// should work ;p
                    if (monster.getHp() <= monster.getStats().selfDestruction().getHp()) {
                        killMonster(monster, p, true, monster.getStats().selfDestruction().getAction());
                        return true;
                    }
                }
                if (killMonster) {
                    killMonster(monster, p, true);
                }
                return true;
            }
        }
        return false;
    }

    public void killMonster(MapleMonster monster, Player p, boolean withDrops) {
        killMonster(monster, p, withDrops, 1);
    }

        
    public void killAllBoogies() {
        List<FieldObject> monsters = getMapObjectsInRange(new Point(0,0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.MONSTER));
        for (final FieldObject monstermo : monsters) {
            final MapleMonster monster = (MapleMonster) monstermo;
            if (monster.getId() == 3230300 || monster.getId() == 3230301 || monster.getName().toLowerCase().contains("boogie")) {
                spawnedMonstersOnMap.decrementAndGet();
                monster.setHp(0);
                broadcastMessage(MonsterPackets.KillMonster(monster.getObjectId(), true), monster.getPosition());
                removeMapObject(monster);
            }
        }
        this.broadcastMessage(PacketCreator.ServerNotice(6, "As the rock crumbled, Jr. Boogie fell in great pain and disappeared."));
    }
        
    public void buffField(int buffID) {
        ItemInformationProvider mii = ItemInformationProvider.getInstance();
        MapleStatEffect statEffect = mii.getItemEffect(buffID);
        synchronized (this.characters) {
            for (Player character : this.characters) {
                if (character.isAlive()) {
                    statEffect.applyTo(character);
                }
            }
        }		
    }
	
    public void addClock(int seconds) {
        broadcastMessage(PacketCreator.GetClockTimer(seconds));
    }

    @SuppressWarnings("static-access")
    public void killMonster(final MapleMonster monster, final Player p, final boolean withDrops, int animation) {
        ItemInformationProvider mii = ItemInformationProvider.getInstance();

        if (monster == null) return;

        if (p == null) {
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            removeMapObject(monster);

            monster.dispatchMonsterKilled(false);
            broadcastMessage(MonsterPackets.KillMonster(monster.getObjectId(), animation), monster.getPosition());
            return;
        }
    

        if (monster.getStats().getLevel() >= p.getLevel() + 30 && !p.isGameMaster()) {
            CheatingOffense.DAMAGE_HACK.cheatingSuspicious(p," for killing a " + monster.getName() + " which is over 30 levels higher.");
        }
        
        int buff = monster.getBuffToGive();
        if (buff > -1) {
            for (FieldObject mmo : this.getAllPlayer()) {
                Player character = (Player) mmo;
                if (character.isAlive()) {
                    MapleStatEffect statEffect = mii.getItemEffect(monster.getBuffToGive());
                    statEffect.applyTo(character);
                }
            }
        }
           
        spawnedMonstersOnMap.decrementAndGet();
        monster.setHpZero();
        removeMapObject(monster);
            
        if (monster.getId() >= 8800003 && monster.getId() <= 8800010) {
            boolean makeZakReal = true;
            Collection<FieldObject> objects = getMapObjects();
            for (FieldObject object : objects) {
                MapleMonster mons = getMonsterByOid(object.getObjectId());
                if (mons != null) {
                    if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                        makeZakReal = false;
                        break;
                    }
                }
            }
            if (makeZakReal) {
                Field map = p.getMap();
                
                for (FieldObject object : objects) {
                    MapleMonster mons = map.getMonsterByOid(object.getObjectId());
                    if (mons != null) {
                        if (mons.getId() == 8800000) {
                            makeMonsterReal(mons);
                            updateMonsterController(mons);
                            break;
                        }
                    }
                }
            }
        }
            
        Player dropOwner = monster.killBy(p);
        if (withDrops && !monster.dropsDisabled()) {
            if (dropOwner == null) {
                dropOwner = p;
            }
            dropFromMonster(dropOwner, monster);
        }
            
        monster.dispatchMonsterKilled(true);
        broadcastMessage(MonsterPackets.KillMonster(monster.getObjectId(), animation), monster.getPosition());
    }

    public void scheduleWarp(Field toGoto, Field frm, long time) {
        MapTimer tMan = MapTimer.getInstance();
        tMan.schedule(new warpAll(toGoto, frm), time);
    }
       
    public void closeMapSpawnPoints() {
        for (SpawnPoint spawnPoint : monsterSpawn) {
            spawnPoint.setDenySpawn(true);
        }
    }
    
    public void killAllMonsters() {
        closeMapSpawnPoints();
        
        for (FieldObject monstermo : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.MONSTER))) {
            MapleMonster monster = (MapleMonster) monstermo;
            
            killMonster(monster, null, false, 1);
        }
    }

    public void killMonster(int mobId) {
        Player chr = (Player) getPlayers().get(0);
        List<MapleMonster> mobList = getMonsters();
        
        for (MapleMonster mob : mobList) {    
            if (mob.getId() == mobId) {
                this.killMonster(mob, chr, false);
            }
        }
    }
    
    public final void destroyReactors(final int first, final int last) {
        List<Reactor> toDestroy = new ArrayList<>();
        List<FieldObject> reactors = getReactors();
        
        for (FieldObject obj : reactors) {
            Reactor mr = (Reactor) obj;
            if (mr.getId() >= first && mr.getId() <= last) {
                toDestroy.add(mr);
            }
        }
        
        for (Reactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public void destroyReactor(int oid) {
        final Reactor reactor = getReactorByOid(oid);
        broadcastMessage(PacketCreator.DestroyReactor(reactor));
        reactor.cancelReactorTimeout();
        reactor.setAlive(false);
        removeMapObject(reactor);
        
        if (reactor.getDelay() > 0) {
            MapTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    respawnReactor(reactor);
                }
            }, reactor.getDelay());
        }
    }

    public void resetReactors() {
        List<Reactor> list = new ArrayList<>();
        
        objectRLock.lock();
        try {
            for (FieldObject o : mapObjects.values()) {
                if (o.getType() == FieldObjectType.REACTOR) {
                    final Reactor r = ((Reactor) o);
                    list.add(r);
                }
            }
        } finally {
            objectRLock.unlock();
        }
        
        resetReactors(list);
    }
    
    public final void resetReactors(List<Reactor> list) {
        for (Reactor r : list) {
            r.lockReactor();
            try {
                r.resetReactorActions(0);
                broadcastMessage(PacketCreator.TriggerReactor(r, 0));
            } finally {
                r.unlockReactor();
            }
        }
    }
    
    public void shuffleReactors() {
        List<Point> points = new ArrayList<>();
        objectRLock.lock();
        try {
            for (FieldObject o : mapObjects.values()) {
                if (o.getType() == FieldObjectType.REACTOR) {
                    points.add(((Reactor) o).getPosition());
                }
            }
            Collections.shuffle(points);
            for (FieldObject o : mapObjects.values()) {
                if (o.getType() == FieldObjectType.REACTOR) {
                    ((Reactor) o).setPosition(points.remove(points.size() - 1));
                }
            }
        } finally {
            objectRLock.unlock();
        }
    }
    
    public void updateMonsterController(MapleMonster monster) {
        monster.lockMonster();
        try {
            if (!monster.isAlive()) {
                return;
            }
            if (monster.getController() != null) {
                if (monster.getController().getMap() != this) {
                    monster.getController().uncontrolMonster(monster);
                } else {
                    return;
                }
            }
            int mincontrolled = -1;
            Player newController = null;

            chrRLock.lock();
            try {
                final Iterator<Player> ltr = characters.iterator();
                Player chr;
                while (ltr.hasNext()) {
                    chr = ltr.next();
                    if (!chr.isHidden() && (chr.getControlledMonsters().size() < mincontrolled || mincontrolled == -1)) {
                        mincontrolled = chr.getControlledMonsters().size();
                        newController = chr;
                    }
                }
            } finally {
                chrRLock.unlock();
            }
            if (newController != null) {
                if (monster.isFirstAttack()) {
                    newController.controlMonster(monster, true);
                    monster.setControllerHasAggro(true);
                    monster.setControllerKnowsAboutAggro(true);
                } else {
                    newController.controlMonster(monster, false);
                }
            }
        } finally {
            monster.unlockMonster();
        }
    }
    
    public FieldObject getMapObject(int oid) {
        objectRLock.lock();
        try {
            return mapObjects.get(oid);
        } finally {
            objectRLock.unlock();
        }
    }

    public boolean containsNPC(int npcid) {
        objectRLock.lock();
        try {
            for (FieldObject obj : mapObjects.values()) {
                if (obj.getType() == FieldObjectType.NPC) {
                    if (((MapleNPC) obj).getId() == npcid) {
                        return true;
                    }
                }
            }
        } finally {
            objectRLock.unlock();
        }
        return false;
    }
      
    public Portal getRandomSpawnpoint() {        
        List<Portal> spawnPoints = new ArrayList<>();
        for (Portal portal : portals.values()) {
            if (portal.getType() >= 0 && portal.getType() <= 2) {
                spawnPoints.add(portal);
            }
        }
        Portal portal = spawnPoints.get(new Random().nextInt(spawnPoints.size()));
        return portal != null ? portal : getPortal(0);
    }

    public MapleMonster getMonsterByOid(int oid) {
        FieldObject mmo = getMapObject(oid);
        return (mmo != null && mmo.getType() == FieldObjectType.MONSTER) ? (MapleMonster) mmo : null;
    }

    public Reactor getReactorByOid(int oid) {
        FieldObject mmo = getMapObject(oid);
        return (mmo != null && mmo.getType() == FieldObjectType.REACTOR) ? (Reactor) mmo : null;
    }
    
    public MapleNPC getNPCByOid(int oid) {
        FieldObject mmo = getMapObject(oid);
        return (mmo != null && mmo.getType() == FieldObjectType.NPC) ? (MapleNPC) mmo : null;
    }

    public Reactor getReactorByName(String name) {
        objectRLock.lock();
        try {
            for (FieldObject obj : mapObjects.values()) {
                if (obj.getType() == FieldObjectType.REACTOR) {
                    if (((Reactor) obj).getName().equals(name)) {
                        return (Reactor) obj;
                    }
                }
            }
        } finally {
            objectRLock.unlock();
        }
        return null;
    }
    
    public Reactor getReactorById(int Id) {
        objectRLock.lock();
        try {
            for (FieldObject obj : mapObjects.values()) {
                if (obj.getType() == FieldObjectType.REACTOR) {
                    if (((Reactor) obj).getId() == Id) {
                        return (Reactor) obj;
                    }
                }
            }
            return null;
        } finally {
            objectRLock.unlock();
        }
    }
    
    public MapleMonster getMonsterById(int id) {
        objectRLock.lock();
        try {
            for (FieldObject obj : mapObjects.values()) {
                if (obj.getType() == FieldObjectType.MONSTER) {
                    if (((MapleMonster) obj).getId() == id) {
                        return (MapleMonster) obj;
                    }
                }
            }
        } finally {
            objectRLock.unlock();
        }
        return null;
    }

    public void spawnFakeMonsterOnGroundBelow(MapleMonster mob, Point pos) {
        Point spos = getGroundBelow(pos);
        mob.setPosition(spos);
        spawnFakeMonster(mob);
    }

    public void spawnRevives(final MapleMonster monster) {
        monster.setMap(this);

        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
            @Override
            public void sendPackets(Client c) {
                c.announce(MonsterPackets.SpawnMonster(monster, false));
            }
        });
        updateMonsterController(monster);
        spawnedMonstersOnMap.incrementAndGet();
        
        final SelfDestruction selfDestruction = monster.getStats().selfDestruction();
        if (monster.getStats().getRemoveAfter() > 0 || selfDestruction != null && selfDestruction.getHp() < 0) {
            if (selfDestruction == null) {
                MapTimer.getInstance().schedule(() -> {
                    killMonster(monster, null, false);
                }, monster.getStats().getRemoveAfter() * 1000);
            } else {
                MapTimer.getInstance().schedule(() -> {
                    killMonster(monster, null, false, selfDestruction.getAction());
                }, selfDestruction.removeAfter() * 1000);
            }
        }
    }
    
    public void resetRiceCakes() {
        this.riceCakes = 0;
    }

    public void addBunnyHit() {
        this.bunnyDamage++;
        if (bunnyDamage > 5) {
            broadcastMessage(PacketCreator.ServerNotice(6, "The Moon Bunny is feeling sick. Please protect it so it can make delicious rice cakes."));
            bunnyDamage = 0;
        }
    }
    
    private void monsterItemDrop(final MapleMonster m, long delay) {
        final ScheduledFuture<?> monsterItemDrop = MapTimer.getInstance().register(() -> {
          
            List<FieldObject> chrList = Field.this.getPlayers();
            
            if (m.isAlive() && !Field.this.getAllPlayer().isEmpty()) {
                Player p = (Player) chrList.get(0);
                
                switch (m.getId()) {
                    case 9300061:
                        Field.this.riceCakes++;
                        Field.this.broadcastMessage(PacketCreator.ServerNotice(6, "The Moon Bunny made rice cake number " + (Field.this.riceCakes) + "."));
                        break;
                }
                
                dropFromMonster(p, m);
            }
        }, delay, delay);
        if (!m.isAlive()) {
            monsterItemDrop.cancel(true);
        }
    }
    
    public void spawnMonster(final MapleMonster monster) {
        spawnMonster(monster, 1, false);
    }
   
    public void spawnMonster(final MapleMonster monster, int difficulty, boolean isPq) {
        monster.changeDifficulty(difficulty, isPq);
        
        monster.setMap(this);
        
        if (getEventInstance() != null) getEventInstance().registerMonster(monster);
        
        spawnAndAddRangedMapObject(monster, (Client c) -> {
            c.getSession().write(MonsterPackets.SpawnMonster(monster, true));
        }, null);
        
        updateMonsterController(monster);
        
        if (monster.getDropPeriodTime() > 0) {
            switch (monster.getId()) {
                case 9300061: // Moon Bunny (HPQ)
                    monsterItemDrop(monster, monster.getDropPeriodTime() / 3);
                    break;
                case 9300102: //Watchhog
                    monsterItemDrop(monster, monster.getDropPeriodTime());
                    break;
                default:
                    FileLogger.print("spawnMonster_getDropPeriodTime", "UNCODED TIMED MOB DETECTED: " + monster.getId());
                    System.out.println("UNCODED TIMED MOB DETECTED: " + monster.getId());
                    break;
            }
        }   
        
        spawnedMonstersOnMap.incrementAndGet();
        
        final SelfDestruction selfDestruction = monster.getStats().selfDestruction();
        if (monster.getStats().getRemoveAfter() > 0 || selfDestruction != null && selfDestruction.getHp() < 0) {
            if (selfDestruction == null) {
                MapTimer.getInstance().schedule(() -> {
                    killMonster(monster, null, false);
                }, monster.getStats().getRemoveAfter() * 1000);
            } else {
                MapTimer.getInstance().schedule(() -> {
                    killMonster(monster, null, false, selfDestruction.getAction());
                }, selfDestruction.removeAfter() * 1000);
            }
        }
    }

    public int spawnMonsterWithCoords(MapleMonster mob, int x, int y) {
        Point spos = new Point(x, y - 1);
        spos = calcPointBelow(spos);
        spos.y -= 1;
        mob.setPosition(spos);    
        spawnMonster(mob);
        return mob.getObjectId();
    }

    public void spawnMonsterWithEffect(final MapleMonster monster, final int effect, Point pos) {
        monster.setMap(this);
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        if (spos == null) return;
        
        spos.y--;
        monster.setPosition(spos);
        monster.disableDrops();
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
            @Override
            public void sendPackets(Client c) {
                c.getSession().write(MonsterPackets.SpawnMonster(monster, true, effect));
            }
        }, null);
        
        updateMonsterController(monster);
        spawnedMonstersOnMap.incrementAndGet();
    }

    public boolean hasTimer() {
        return timer;
    }

    public void setTimer(boolean timer) {
        this.timer = timer;
    }

    public void spawnFakeMonster(final MapleMonster monster) {
        monster.setMap(this);
        monster.setFake(true);
        spawnAndAddRangedMapObject(monster, (Client c) -> {
            c.getSession().write(MonsterPackets.SpawnFakeMonster(monster, 0));
        }, null);
        spawnedMonstersOnMap.incrementAndGet();
    }

    public void makeMonsterReal(final MapleMonster monster) {
        monster.setFake(false);
        broadcastMessage(MonsterPackets.MakeMonsterReal(monster));
        updateMonsterController(monster);
    }

    public void spawnReactor(final Reactor reactor) {
        reactor.setMap(this);
        spawnAndAddRangedMapObject(reactor, new DelayedPacketCreation() {
            @Override
            public void sendPackets(Client c) {
                c.announce(reactor.makeSpawnData());
            }
        });
    }
         
    private void respawnReactor(final Reactor reactor) {
        reactor.lockReactor();
        try {
            reactor.resetReactorActions(0);
            reactor.setAlive(true);
        } finally {
            reactor.unlockReactor();
        }
        
        spawnReactor(reactor);
    }
    
    public void spawnDoor(final FieldDoorObject door) {
        spawnAndAddRangedMapObject(door, (Client c) -> {
            if (door.getFrom().getId() == c.getPlayer().getMapId()) {
                if (c.getPlayer().getParty() != null && (door.getOwnerId() == c.getPlayer().getId() || c.getPlayer().getParty().getMemberById(door.getOwnerId()) != null)) {
                    c.announce(PartyPackets.PartyPortal(door.getFrom().getId(), door.getTo().getId(), door.toPosition()));
                }
                c.announce(PacketCreator.SpawnPortal(door.getFrom().getId(), door.getTo().getId(), door.toPosition()));
                if (!door.inTown()) {
                    c.announce(PacketCreator.SpawnDoor(door.getOwnerId(), door.getPosition(), false));
                }
            }
            c.announce(PacketCreator.EnableActions());
        }, (Player chr) -> chr.getMapId() == door.getFrom().getId());
        
        if (!door.inTown()) {
            setLastDoorOwner(door.getOwnerId());
        }
    }
    
    public boolean canDeployDoor(Point pos) {
        Point toStep = calcPointBelow(pos);
        return toStep != null && toStep.distance(pos) <= 42;
    }
    
    /**
     * Fetches angle relative between spawn and door points
     * where 3 O'Clock is 0 and 12 O'Clock is 270 degrees
     * 
     * @param spawnPoint
     * @param doorPoint
     * @return angle in degress from 0-360.
     */
    private static double getAngle(Point doorPoint, Point spawnPoint) {
        double dx = doorPoint.getX() - spawnPoint.getX();
        double dy = -(doorPoint.getY() - spawnPoint.getY());
        double inRads = Math.atan2(dy, dx);
        if (inRads < 0) {
            inRads = Math.abs(inRads);
        } else {
            inRads = 2 * Math.PI - inRads;
        }
        return Math.toDegrees(inRads);
    }
    
    public Pair<String, Integer> getDoorPositionStatus(Point pos) {
        Portal portal = findClosestPlayerSpawnpoint(pos);
        
        double angle = getAngle(portal.getPosition(), pos);
        double distn = pos.distanceSq(portal.getPosition());
        
        if (distn <= 777777.7) {
            return null;
        }
        distn = Math.sqrt(distn);
        return new Pair(getRoundedCoordinate(angle), Integer.valueOf((int)distn));
    }
    
    /**
     * Converts angle in degrees to rounded cardinal coordinate.
     * 
     * @param angle
     * @return correspondent coordinate.
     */
    public static String getRoundedCoordinate(double angle) {
        String directions[] = {"E", "SE", "S", "SW", "W", "NW", "N", "NE", "E"};
        return directions[ (int)Math.round((  ((double)angle % 360) / 45)) ];
    }
     
    public void spawnSummon(final MapleSummon summon) {
        if (summon != null) {
            summon.updateMap(this);
            spawnAndAddRangedMapObject(summon, (Client c) -> {
                c.getSession().write(PacketCreator.SpawnSpecialFieldObject(summon, true));
            }, null);
        }
    }

    public void spawnMist(final MapleMist mist, final int duration, boolean poison, boolean fake) {
        addMapObject(mist);
        broadcastMessage(fake ? mist.makeFakeSpawnData(30) : mist.makeSpawnData());
        MapTimer tMan = MapTimer.getInstance();
        final ScheduledFuture<?> poisonSchedule;
        if (poison) {
            Runnable poisonTask = () -> {
                List<FieldObject> affectedMonsters = getMapObjectsInBox(mist.getBox(), Collections.singletonList(FieldObjectType.MONSTER));
                affectedMonsters.stream().filter((mo) -> (mist.makeChanceResult())).forEachOrdered((mo) -> {
                    MonsterStatusEffect poisonEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), mist.getSourceSkill(), false);
                    ((MapleMonster) mo).applyStatus(mist.getOwner(), poisonEffect, true, duration);
                });
            };
         poisonSchedule = tMan.register(poisonTask, 2000, 2500);
        } else {
            poisonSchedule = null;
        }
        tMan.schedule(() -> {
            removeMapObject(mist);
            if (poisonSchedule != null) {
                poisonSchedule.cancel(false);
            }
            broadcastMessage(mist.makeDestroyData());
        }, duration);
    }
        
    public void spawnMist(final MapleMist mist, final int duration, boolean poison, boolean fake, boolean recovery) {
        addMapObject(mist);
        broadcastMessage(fake ? mist.makeFakeSpawnData(30) : mist.makeSpawnData());
        MapTimer tMan = MapTimer.getInstance();
        final ScheduledFuture<?> poisonSchedule;
        if (poison) {
            Runnable poisonTask = () -> {
                List<FieldObject> affectedMonsters = getMapObjectsInBox(mist.getBox(), Collections.singletonList(FieldObjectType.MONSTER));
                    for (FieldObject mo : affectedMonsters) {
                    if (mist.makeChanceResult()) {
                        MonsterStatusEffect poisonEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), mist.getSourceSkill(), null, false);
                        ((MapleMonster) mo).applyStatus(mist.getOwner(), poisonEffect, true, duration);
                    }
                }
            };
            poisonSchedule = tMan.register(poisonTask, 2000, 2500);
        } else if (recovery) {
            Runnable poisonTask = () -> {
                List<FieldObject> affectedMonsters = getMapObjectsInBox(mist.getBox(), Collections.singletonList(FieldObjectType.MONSTER));
                    for (FieldObject mo : affectedMonsters) {
                    if (mist.makeChanceResult()) {
                        Player chr = (Player) mo;
                        if (mist.getOwner().getId() == chr.getId() || mist.getOwner().getParty() != null && mist.getOwner().getParty().containsMembers(chr.getMPC())) {
                            chr.getStat().addMP((int) mist.getSourceSkill().getEffect(chr.getSkillLevel(mist.getSourceSkill().getId())).getX() * chr.getStat().getMp() / 100);
                        }
                    }
                }
            };
            poisonSchedule = tMan.register(poisonTask, 2000, 2500);
        } else {
            poisonSchedule = null;
        }      
        tMan.schedule(() -> {
            removeMapObject(mist);
            if (poisonSchedule != null) {
                poisonSchedule.cancel(false);
            }
            broadcastMessage(mist.makeDestroyData());
        }, duration);
    }

    public void timeMob(int id, String msg) {
        timeMob = new Pair<>(id, msg);
    }

    public Pair<Integer, String> getTimeMob() {
        return timeMob;
    }
    
    public void toggleHiddenNPC(int id) {
        objectRLock.lock();
        try {
            for (FieldObject obj : mapObjects.values()) {
                if (obj.getType() == FieldObjectType.NPC) {
                    MapleNPC npc = (MapleNPC) obj;
                    if (npc.getId() == id) {
                        npc.setHide(!npc.isHidden());
                        if (!npc.isHidden())  {
                            broadcastMessage(PacketCreator.SpawnNPC(npc));
                        }
                    }
                }
            }
        } finally {
            objectRLock.unlock();
        }
    }
    
    public final List<FieldObject> getReactors() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.REACTOR));
    }
    
    public List<FieldObject> getPlayers() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.PLAYER));
    }
    
    private void activateItemReactors(final FieldItem drop, final Client c) {
        final Item item = drop.getItem();
        objectRLock.lock();
        try {
            for (final FieldObject o : getReactors()) {
                final Reactor react = (Reactor) o;

                if (react.getReactorType() == 100) {
                    if (react.getReactItem(react.getEventState()).getLeft() == item.getItemId() && react.getReactItem(react.getEventState()).getRight() == item.getQuantity()) {

                        if (react.getArea().contains(drop.getPosition())) {
                            MapTimer.getInstance().schedule(new ActivateItemReactor(drop, react, c), 5000);
                            break;
                        }
                    }
                }
            }
        } finally {
            objectRLock.unlock();
        }
    }
    
    public List<Reactor> getAllReactor() {
        return getAllReactorsThreadsafe();
    }
    
    public Collection<Player> getCharacters() {
        return Collections.unmodifiableCollection(this.characters);
    }
    
    public final List<FieldObject> getAllMonsters() {
	return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.MONSTER));
    }	

    public List<FieldObject> getAllPlayer() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.PLAYER));
    }
    
    public int playerCount() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.PLAYER)).size();
    }
    
    public int monsterCount() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.MONSTER)).size();
    }
    
    public int itemCount() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.ITEM)).size();
    }
	
    public int monsterCountById(int id) {
        int mobQuantity = 0;
        for (FieldObject m : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.MONSTER))) {
            MapleMonster monster = (MapleMonster) m;
            if (monster.getId() == id) {
                mobQuantity++;
            }
        }
        return mobQuantity;
    }
    
    public int countMonster(int id) {
        return countMonster(id, id);
    }
    
    public int countMonster(int minid, int maxid) {
        int count = 0;
        for (FieldObject m : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.MONSTER))) {
            MapleMonster mob = (MapleMonster) m;
            if (mob.getId() >= minid && mob.getId() <= maxid) {
                count++;
            }
        }
        return count;
    }
    
    public int countMonsters() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.MONSTER)).size();
    }
    
    public ArrayList<Player> getAllPlayers() {
        ArrayList<Player> arr = new ArrayList<>();
        for (FieldObject mmo : getAllPlayer()) {
            if (mmo.getType().equals(FieldObjectType.PLAYER)) {
                arr.add((Player) mmo);
            }
        }
        return arr;
    }
    
    public List<MapleMonster> getMonsters() {
        List<MapleMonster> mobs = new ArrayList<>();
        for (FieldObject object : this.getMapObjects()) {
            if(object instanceof MapleMonster) mobs.add((MapleMonster)object);
        }
        return mobs;
    }

    public boolean withinObjectRange(Player who, int objectid) {
        List<FieldObject> npc = getMapObjectsInRange(who.getPosition(), 850 * 850, Arrays.asList(FieldObjectType.NPC));
        for (FieldObject pen : npc) {
            MapleNPC npcla = (MapleNPC) pen;
            if (npcla.getObjectId() == objectid) {
                return true;
            }
        }
        return false;
    }
    
    public final List<Player> getCharactersThreadsafe() {
        return getCharactersThreadsafe(new ArrayList<>());
    }

    public final ArrayList<Player> getCharactersThreadsafe(ArrayList<Player> chars) {
        chars.clear();
        chrRLock.lock();
        try {
            for (Player mc : characters) {
                chars.add(mc);
            }
        } finally {
            chrRLock.unlock();
        }
        return chars;
    }

    public List<FieldObject> getAllDoorsThreadsafe() {
        ArrayList<FieldObject> ret = new ArrayList<>();
        objectRLock.lock();
        try {
            for (FieldObject mmo : mapObjects.values()) {
                if (mmo.getType() == FieldObjectType.DOOR) {
                    ret.add(mmo);
                }
            }
        } finally {
            objectRLock.unlock();
        }
        return ret;
    }
    
    public void spawnAllMonsterIdFromMapSpawnList(int id) {
        spawnAllMonsterIdFromMapSpawnList(id, 1, false);
    }
    
    public void spawnAllMonsterIdFromMapSpawnList(int id, int difficulty, boolean isPq) {
        for(SpawnPoint sp: allMonsterSpawn) {
            if(sp.getMonsterId() == id) {
                spawnMonster(sp.getMonster(), difficulty, isPq);
            }
        }
    }
    
    public void spawnAllMonstersFromMapSpawnList() {
        spawnAllMonstersFromMapSpawnList(1, false);
    }
    
    public void spawnAllMonstersFromMapSpawnList(int difficulty, boolean isPq) {
        for(SpawnPoint sp: allMonsterSpawn) {
            spawnMonster(sp.getMonster(), difficulty, isPq);
        }
    }

    public final void addAreaMonsterSpawn(final MapleMonster monster, Point pos1, Point pos2, Point pos3, final int mobTime, final String msg) {
//        pos1 = calcPointBelow(pos1);
//        pos2 = calcPointBelow(pos2);
//        pos3 = calcPointBelow(pos3);
//        if (pos1 != null) {
//            pos1.y -= 1;
//        }
//        if (pos2 != null) {
//            pos2.y -= 1;
//        }
//        if (pos3 != null) {
//            pos3.y -= 1;
//        }
//        if (pos1 == null && pos2 == null && pos3 == null) {
//            System.out.println("WARNING: mapid " + mapId + ", monster " + monster.getId() + " could not be spawned.");
//            return;
//        } else if (pos1 != null) {
//            if (pos2 == null) {
//                pos2 = new Point(pos1);
//            }
//            if (pos3 == null) {
//                pos3 = new Point(pos1);
//            }
//        } else if (pos2 != null) {
//            if (pos1 == null) {
//                pos1 = new Point(pos2);
//            }
//            if (pos3 == null) {
//                pos3 = new Point(pos2);
//            }
//        } else if (pos3 != null) {
//            if (pos1 == null) {
//                pos1 = new Point(pos3);
//            }
//            if (pos2 == null) {
//                pos2 = new Point(pos3);
//            }
//        }
//        if (monster != null) {
//            monsterSpawn.add(new SpawnPointAreaBoss(monster, pos1, pos2, pos3, mobTime, msg, true));
//        }
    }

    public MapleNPC getNPCById(int id) {
        for (FieldObject obj : mapObjects.values())
            if (obj.getType() == FieldObjectType.NPC) 
                if (((MapleNPC) obj).getId() == id)
                    return (MapleNPC) obj;
        return null;
    }

     public final void disappearingItemDrop(final FieldObject dropper, final Player owner, final Item item, final Point pos) {
        final Point droppos = calcDropPos(pos, pos);
        final FieldItem drop = new FieldItem(item, droppos, dropper, owner, (byte) 1, false);
        broadcastMessage(PacketCreator.DropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 3), drop.getPosition());
    }

    public final void spawnItemDrop(final FieldObject dropper, final Player owner, final Item item, Point pos, final boolean ffaDrop, final boolean playerDrop) {
        final Point dropPos = calcDropPos(pos, pos);
        final FieldItem drop = new FieldItem(item, dropPos, dropper, owner, (byte) 2, playerDrop);

        spawnAndAddRangedMapObject(drop, new DelayedPacketCreation() {

            @Override
            public void sendPackets(Client c) {
                drop.lockItem();
                try {
                    c.getSession().write(PacketCreator.DropItemFromMapObject(drop, dropper.getPosition(), dropPos, (byte) 1));
                } finally {
                    drop.unlockItem();
                }    
            }
        }, null);
        
        drop.lockItem();
        try {
            broadcastMessage(PacketCreator.DropItemFromMapObject(drop, dropper.getPosition(), dropPos, (byte) 0));
        } finally {
            drop.unlockItem();
        }

        if (!everlast) {
            drop.registerExpire(120000);
            activateItemReactors(drop, owner.getClient());
        }
    }

     public final void spawnMesoDrop(final int meso, final Point position, final FieldObject dropper, final Player owner, final boolean playerDrop, final byte droptype) {
        final Point droppos = calcDropPos(position, position);
        final FieldItem mdrop = new FieldItem(meso, droppos, dropper, owner, droptype, playerDrop);

        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

            @Override
            public void sendPackets(Client c) {
                mdrop.lockItem();
                try {
                    c.getSession().write(PacketCreator.DropItemFromMapObject(mdrop, dropper.getPosition(), droppos, (byte) 1));
                } finally {
                    mdrop.unlockItem();
                }
            }
        }, null);
        
        if (!everlast) {
            mdrop.registerExpire(120000);
            if (droptype == 0 || droptype == 1) {
                mdrop.registerFFA(30000);
            }
        }
    }

    public void dropFromReactor(final Player p, final Reactor reactor, Item drop, Point dropPos, short questid) {
        spawnDrop(drop, this.calcDropPos(dropPos, reactor.getPosition()), reactor, p, (byte)(p.getParty() != null ? 1 : 0), questid);
    }

     public void displayClock(final Player p, int time) {
        broadcastMessage(PacketCreator.GetClockTimer(time));
        MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                broadcastMessage(PacketCreator.DestroyClock());
            }
        }, time * 1000);
    } 

    public void setEventInstance(EventInstanceManager eim) {
        event = eim;
    }
    
    public EventInstanceManager getEventInstance() {
        return event;
    }

    public Map<Integer, Player> getMapPlayers() {
        chrRLock.lock();
        try {
            Map<Integer, Player> mapChars = new HashMap<>(characters.size());
            
            for(Player chr : characters) {
                mapChars.put(chr.getId(), chr);
            }
            
            return mapChars;
        }
        finally {
            chrRLock.unlock();
        }
    }

   public List<Reactor> getReactorsByIdRange(final int first, final int last) {
        List<Reactor> list = new LinkedList<>();
        
        objectRLock.lock();
        try {
            for (FieldObject obj : mapObjects.values()) {
                if (obj.getType() == FieldObjectType.REACTOR) {
                    Reactor mr = (Reactor) obj;
                    
                    if (mr.getId() >= first && mr.getId() <= last) {
                        list.add(mr);
                    }
                }
            }
            
            return list;
        } finally {
            objectRLock.unlock();
        }
    }

     public Portal getRandomPlayerSpawnpoint() {
        List<Portal> spawnPoints = new ArrayList<>();
        for (Portal portal : portals.values()) {
            if (portal.getType() >= 0 && portal.getType() <= 1 && portal.getTargetMapId() == MapConstants.NULL_MAP) {
                spawnPoints.add(portal);
            }
        }
        Portal portal = spawnPoints.get(new Random().nextInt(spawnPoints.size()));
        return portal != null ? portal : getPortal(0);
    }

    public void searchItemReactors(final Reactor react) {
        if (react.getReactorType() == 100) {
            Pair<Integer, Integer> reactProp = react.getReactItem(react.getEventState());
            int reactItem = reactProp.getLeft(), reactQty = reactProp.getRight();
            Rectangle reactArea = react.getArea();
            
            List<FieldItem> list = new ArrayList<>();
            objectRLock.lock();
            try {
                for(FieldItem mmi : droppedItems.keySet()) {
                    if(!mmi.isPickedUp()) {
                        list.add(mmi);
                    }
                }
            } finally {
                objectRLock.unlock();
            }
            
            for(final FieldItem drop : list) {
                final Item item = drop.getItem();
            
                if (item != null && reactItem == item.getItemId() && reactQty == item.getQuantity()) {
                    if (reactArea.contains(drop.getPosition())) {
                        Client owner = drop.getOwnerClient();
                        if (owner != null) {
                            MapTimer.getInstance().schedule(new ActivateItemReactor(drop, react, owner), 5000);
                        }
                    }
                }
            }
        }
    }

    public void destroyNPC(int npcid) {
        List<FieldObject> npcs = getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.NPC));

        chrRLock.lock();
        objectWLock.lock();
        try {
            for (FieldObject obj : npcs) {
                if (((MapleNPC) obj).getId() == npcid) {
                    broadcastMessage(PacketCreator.RemoveNPCController(obj.getObjectId()));
                    broadcastMessage(PacketCreator.RemoveNPC(obj.getObjectId()));
                    
                    this.mapObjects.remove(Integer.valueOf(obj.getObjectId()));
                }
            }
        } finally {
            objectWLock.unlock();
            chrRLock.unlock();
        }
    }

    public void broadcastBossHpMessage(MapleMonster mm, int bossHash, OutPacket packet) {
        broadcastBossHpMessage(mm, bossHash, null, packet, Double.POSITIVE_INFINITY, null);
    }
    
    public void broadcastBossHpMessage(MapleMonster mm, int bossHash, final OutPacket packet, Point rangedFrom) {
        broadcastBossHpMessage(mm, bossHash, null, packet, getRangedDistance(), rangedFrom);
    }
    
    private void broadcastBossHpMessage(MapleMonster mm, int bossHash, Player source, final OutPacket packet, double rangeSq, Point rangedFrom) {
        chrRLock.lock();
        try {
            for (Player p : characters) {
                if (p != source) {
                    if (rangeSq < Double.POSITIVE_INFINITY) {
                        if (rangedFrom.distanceSq(p.getPosition()) <= rangeSq) {
                            p.getClient().announceBossHpBar(mm, bossHash, packet);
                        }
                    } else {
                        p.getClient().announceBossHpBar(mm, bossHash, packet);
                    }
                }
            }
        } finally {
            chrRLock.unlock();
        }
    }
    
    private class TimerDestroyWorker implements Runnable {
        @Override
        public void run() {
            if (mapTimer != null) {
                int warpMap = mapTimer.warpToField();
                int minWarp = mapTimer.minLevelToWarp();
                int maxWarp = mapTimer.maxLevelToWarp();
                mapTimer = null;
                if (warpMap != -1) {
                    Field map2wa2 = ChannelServer.getInstance(channel).getMapFactory().getMap(warpMap);
                    String warpmsg = "You will now be warped to " + map2wa2.getStreetName() + " : " + map2wa2.getMapName();
                    broadcastMessage(PacketCreator.ServerNotice(6, warpmsg));
                    getCharacters().forEach((chr) -> {
                        try {
                            if (chr.getLevel() >= minWarp && chr.getLevel() <= maxWarp) {
                                chr.changeMap(map2wa2, map2wa2.getPortal(0));
                            } else {
                                chr.getClient().getSession().write(PacketCreator.ServerNotice(5, "You are not at least level " + minWarp + " or you are higher than level " + maxWarp + "."));
                            }
                        } catch (Exception ex) {
                            chr.getClient().getSession().write(PacketCreator.ServerNotice(5, "There was a problem warping you. Please contact a GM"));
                        }
                    });
                }
            }
        }
    }

    public void addFieldTimer(int duration) {
        ScheduledFuture<?> sheduled = MapTimer.getInstance().schedule(new TimerDestroyWorker(), duration * 1000);
        mapTimer = new FieldTimer(sheduled, duration, -1, -1, -1);
        broadcastMessage(mapTimer.makeSpawnData());
    }

    public void addFieldTimer(int duration, int fieldToWarpTo) {
        ScheduledFuture<?> sheduled = MapTimer.getInstance().schedule(new TimerDestroyWorker(), duration * 1000);
        mapTimer = new FieldTimer(sheduled, duration, fieldToWarpTo, 0, 256);
        broadcastMessage(mapTimer.makeSpawnData());
    }

    public void addFieldTimer(int duration, int fieldToWarpTo, int minLevelToWarp) {
        ScheduledFuture<?> sheduled = MapTimer.getInstance().schedule(new TimerDestroyWorker(), duration * 1000);
        mapTimer = new FieldTimer(sheduled, duration, fieldToWarpTo, minLevelToWarp, 256);
        broadcastMessage(mapTimer.makeSpawnData());
    }

    public void addFieldTimer(int duration, int fieldToWarpTo, int minLevelToWarp, int maxLevelToWarp) {
        ScheduledFuture<?> sheduled = MapTimer.getInstance().schedule(new TimerDestroyWorker(), duration * 1000);
        mapTimer = new FieldTimer(sheduled, duration, fieldToWarpTo, minLevelToWarp, maxLevelToWarp);
        broadcastMessage(mapTimer.makeSpawnData());
    }

    public void clearFieldTimer() {
        if (mapTimer != null) {
            mapTimer.getSchedule().cancel(true);
        }
        mapTimer = null;
    }
    
    public boolean isLastDoorOwner(int cid) {
        return lastDoorOwner == cid;
    }
    
    public void setLastDoorOwner(int cid) {
        lastDoorOwner = cid;
    }
    
    public void dropMessage(int type, String message) {
        broadcastStringMessage(type, message);
    }
    
    public void broadcastStringMessage(int type, String message) {
        broadcastMessage(PacketCreator.ServerNotice(type, message));
    }
        	
    public final void spawnNpc(final int id, final Point pos) {
	final MapleNPC npc = MapleLifeFactory.getNPC(id);
	npc.setPosition(pos);
	npc.setCy(pos.y);
	npc.setRx0(pos.x + 50);
	npc.setRx1(pos.x - 50);
	npc.setFh(getFootholds().findBelow(pos).getId());
	npc.getStats().setCustom(true);
	addMapObject(npc);
	broadcastMessage(PacketCreator.SpawnNPC(npc, true));
    }

    public final void spawnMobMesoDrop(final int meso, final Point position, final FieldObject dropper, final Player owner, final boolean playerDrop, final byte droptype) {
        final FieldItem mdrop = new FieldItem(meso, position, dropper, owner, droptype, playerDrop);

        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

            @Override
            public void sendPackets(Client c) {
                c.getSession().write(PacketCreator.DropItemFromMapObject(mdrop, dropper.getPosition(), position, (byte) 1));
            }
        }, null);

        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
    }
    
    private static boolean shouldShowQuestItem(Player p, int questid, int itemid) {
        return questid <= 0 || (p.getQuestStatus(questid) == 1 && p.needQuestItem(questid, itemid));
    }
    
    public final void spawnDrop(final Item idrop, final Point dropPos, final FieldObject dropper, final Player p, final byte droptype, final short questid) {
        final FieldItem mdrop = new FieldItem(idrop, dropPos, dropper, p, droptype, false, questid);

        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

            @Override
            public void sendPackets(Client c) {
                if (shouldShowQuestItem(p, questid, idrop.getItemId())) {
                    mdrop.lockItem();
                    try {
                        c.announce(PacketCreator.DropItemFromMapObject(mdrop, dropper.getPosition(), dropPos, (byte) 1));
                    } finally {
                        mdrop.unlockItem();
                    }
                }
            }
        }, null);

        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
        
        activateItemReactors(mdrop, p.getClient());
    }
      
    public void startMapEffect(String msg, int itemId) {
        if (mapEffect != null) {
            return;
        }
        mapEffect = new FieldEffect(msg, itemId);
        broadcastMessage(mapEffect.makeStartData());
        MapTimer tMan = MapTimer.getInstance();
        sfme = tMan.schedule(() -> {
            broadcastMessage(mapEffect.makeDestroyData());
            mapEffect = null;
        }, 30000);
    }
    
    public void stopMapEffect() {
        if (sfme != null) {
            sfme.cancel(false);
        }
        if(mapEffect != null) {
            broadcastMessage(mapEffect.makeDestroyData());
            mapEffect = null;
        }
    }
    
    public Player getAnyCharacterFromParty(int partyid) {
        for (Player p : this.getAllPlayers()) {
            if (p.getPartyId() == partyid) {
                return p;
            }
        }
        
        return null;
    }
    
    private void addPartyMemberInternal(Player p) {
        int partyid = p.getPartyId();
        if (partyid == -1) {
            return;
        }
        
        Set<Integer> partyEntry = mapParty.get(partyid);
        if(partyEntry == null) {
            partyEntry = new LinkedHashSet<>();
            partyEntry.add(p.getId());
            
            mapParty.put(partyid, partyEntry);
        } else {
            partyEntry.add(p.getId());
        }
    }
    
    private void removePartyMemberInternal(Player p) {
        int partyid = p.getPartyId();
        if (partyid == -1) {
            return;
        }
        
        Set<Integer> partyEntry = mapParty.get(partyid);
        if(partyEntry != null) {
            if (partyEntry.size() > 1) {
                partyEntry.remove(p.getId());
            } else {
                mapParty.remove(partyid);
            }
        }
    }
    
    public void addPartyMember(Player p) {
        chrWLock.lock();
        try {
            addPartyMemberInternal(p);
        } finally {
            chrWLock.unlock();
        }
    }
            
    public void removePartyMember(Player p) {
        chrWLock.lock();
        try {
            removePartyMemberInternal(p);
        } finally {
            chrWLock.unlock();
        }
    }
    
    public void removeParty(int partyid) {
        chrWLock.lock();
        try {
            mapParty.remove(partyid);
        } finally {
            chrWLock.unlock();
        }
    }
         	
    /**
     * Adds a player to this map and sends nescessary data
     *
     * @param p
     */
    public void addPlayer(final Player p) {
        objectWLock.lock();
        try {
            this.mapObjects.put(Integer.valueOf(p.getObjectId()), p);
        } finally {
            objectWLock.unlock();
        }
        chrWLock.lock();
        try {
           this.characters.add(p);
        } finally {
            chrWLock.unlock();
        }
        p.setMapId(mapId);
        
        if (GameConstants.USE_DEBUG) System.out.println("[FIELD] Mapid: " + mapId);
        
        if (FieldLimit.CANNOTUSEMOUNTS.check(fieldLimit) && p.getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
            p.cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING);
            p.cancelBuffStats(BuffStat.MONSTER_RIDING);
        }
        final OutPacket packet = PacketCreator.SpawnPlayerMapObject(p);
        
        if (!p.isHidden()) {
            for (final ItemPet pet : p.getPets()) {
                if (pet.getSummoned()) {
                    pet.setPosition(getGroundBelow(p.getPosition()));
                    broadcastMessage(p, PetPackets.ShowPet(p, pet, false, false), false);
                }
            }
            broadcastMessage(p, packet, false);
        } else {
            for (final ItemPet pet : p.getPets()) {
                if (pet.getSummoned()) {
                    pet.setPosition(getGroundBelow(p.getPosition()));
                    broadcastGMMessage(p, PetPackets.ShowPet(p, pet, false, false), false);
                }
            }
            broadcastGMMessage(p, packet, false);
            p.getClient().announce(PacketCreator.ShowHide());
            
            List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<BuffStat, Integer>(BuffStat.DARKSIGHT, 0));
            broadcastGMMessage(p, PacketCreator.BuffMapEffect(p.getId(), stat, false), false);
        }
        
//        if (MapConstants.isAriantPartyQuestField(p)) {
//            broadcastMessage(PacketCreator.UpdateAriantPQRanking(p.getName(), p.getItemQuantity(ItemConstants.ARIANT_JEWEL, false), false));
//        }
        
        sendObjectPlacement(p.getClient());
        p.getClient().getSession().write(packet);
        
        if (!FieldLimit.CANNOTUSEPET.check(fieldLimit)) {
            for (final ItemPet pet : p.getPets()) {
                if (pet.getSummoned()) {
                    pet.setPosition(getGroundBelow(p.getPosition()));
                    p.getClient().getSession().write(PetPackets.ShowPet(p, pet, false, false));
                    if (!pet.getExceptionList().isEmpty()) {
                        p.announce(PetPackets.PetExceptionListResult(p, pet));
                    }
                }
            }
            p.updatePetAuto();
        } else {
            p.unequipAllPets();
        }
        
        switch (this.getId()) {
            case 1:
            case 2:
            case 809000101:
            case 809000201:
                p.getClient().getSession().write(EffectPackets.ShowEquipEffect());
                break;
        }
        final MapleStatEffect stat = p.getStatForBuff(BuffStat.SUMMON);
        if (stat != null) {
            final MapleSummon summon = p.getSummons().get(stat.getSourceId());
            summon.setPosition(p.getPosition());
            try {
                summon.setFh(getFootholds().findBelow(p.getPosition()).getId());
            } catch (NullPointerException e) {
                summon.setFh(0); 
                FileLogger.printError("Position_Summon.txt", e);
            }
            p.addVisibleMapObject(summon);
            this.spawnSummon(summon);
        }
        if (p.getParty() != null) {
            p.silentPartyUpdate();
            p.getClient().announce(PartyPackets.UpdateParty(p.getClient().getChannel(), p.getParty(), MaplePartyOperation.SILENT_UPDATE, null));
            p.updatePartyMemberHP();
            p.receivePartyMemberHP();
        }
        if (mapEffect != null) {
            mapEffect.sendStartData(p.getClient());
        }
        if (mapTimer != null) {
            mapTimer.sendSpawnData(p.getClient());
        }
        if (getTimeLimit() > 0 && getForcedReturnField() != null) {
            p.getClient().getSession().write(PacketCreator.GetClockTimer(getTimeLimit()));
            p.startMapTimeLimitTask(this, this.getForcedReturnField());
        }
        if (p.getEventInstance() != null && p.getEventInstance().isTimerStarted()) {
            p.getClient().getSession().write(PacketCreator.GetClockTimer((int) (p.getEventInstance().getTimeLeft() / 1000)));
        }
        if (hasClock()) {
            p.getClient().announce(PacketCreator.GetClock());
        }
        if (hasBoat() > 0) {
            if (hasBoat() == 1) p.getClient().announce((PacketCreator.ShipEffect(true)));
            else p.getClient().announce(PacketCreator.ShipEffect(false));
        }
    }
    
    public void addMonsterSpawn(MapleMonster monster, int mobTime, int team) {
        Point newpos = calcPointBelow(monster.getPosition());
        newpos.y -= 1;
        SpawnPoint sp = new SpawnPoint(monster, newpos, !monster.isMobile(), mobTime, mobInterval, team);
        monsterSpawn.add(sp);
        
        if (!respawning) {
            return;
        }

        if (sp.shouldSpawn() || mobTime == -1) {
            spawnMonster(sp.getMonster());
        }
    }
    
    public void addAllMonsterSpawn(MapleMonster monster, int mobTime, int team) {
        Point newpos = calcPointBelow(monster.getPosition());
        newpos.y -= 1;
        SpawnPoint sp = new SpawnPoint(monster, newpos, !monster.isMobile(), mobTime, mobInterval, team);
        allMonsterSpawn.add(sp);
    }
    
    public void reportMonsterSpawnPoints(Player p) {
        p.dropMessage(6, "Mob spawnpoints on map " + getId() + ", with available Mob SPs " + monsterSpawn.size() + ", used " + spawnedMonstersOnMap.get() + ":");
        for(SpawnPoint sp: allMonsterSpawn) {
            p.dropMessage(6, "  id: " + sp.getMonsterId() + " canSpawn: " + !sp.getDenySpawn() + " numSpawned: " + sp.getSpawned() + " x: " + sp.getPosition().getX() + " y: " + sp.getPosition().getY() + " time: " + sp.getMobTime() + " team: " + sp.getTeam());
        }
    }
    
    public void beginSpawning() {
        this.respawning = true;
        this.Respawn(true);
    }

    public boolean isRespawning() {
        return respawning;
    }

    public void setRespawning(boolean respawning) {
        this.respawning = respawning;
    }
    
    public void instanceMapRespawn() {
        if(!allowSummons) return;
        
        final int numShouldSpawn = (short) ((monsterSpawn.size() - spawnedMonstersOnMap.get()));
        if (numShouldSpawn > 0) {
            List<SpawnPoint> randomSpawn = new ArrayList<>(monsterSpawn);
            Collections.shuffle(randomSpawn);
            int spawned = 0;
            for (SpawnPoint spawnPoint : randomSpawn) {
                if(spawnPoint.shouldSpawn()) {
                    spawnMonster(spawnPoint.getMonster());
                    spawned++;
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        }
    }

    public void instanceMapForceRespawn() {
        if(!allowSummons) return;
        
        final int numShouldSpawn = (short) ((monsterSpawn.size() - spawnedMonstersOnMap.get()));
        if (numShouldSpawn > 0) {
            List<SpawnPoint> randomSpawn = new ArrayList<>(monsterSpawn);
            Collections.shuffle(randomSpawn);
            int spawned = 0;
            for (SpawnPoint spawnPoint : randomSpawn) {
                if(spawnPoint.shouldForceSpawn()) {
                    spawnMonster(spawnPoint.getMonster());
                    spawned++;
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        }
    }
    
    private int getMaxRegularSpawn() {
        return (int) (monsterSpawn.size() / monsterRate);
    }
    
    public void Respawn(final boolean force) {
        if (!allowSummons) return;
        
	chrRLock.lock();
        try {
            if (characters.isEmpty()) {
                return;
            }
        } finally {
            chrRLock.unlock();
        }
        if (force) { 
            short numShouldSpawn = (short) ((monsterSpawn.size() - spawnedMonstersOnMap.get()));

            if (numShouldSpawn > 0) {
                int spawned = 0;

                for (SpawnPoint spawnPoint : monsterSpawn) {
                    spawnPoint.getMonster();
                    spawned++;
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        } else {
            
            int ispawnedMonstersOnMap = spawnedMonstersOnMap.get();
            double getMaxSpawn = getMaxRegularSpawn() * 1;
            if (mapId == 610020002 || mapId == 610020004) {
                getMaxSpawn *= 2;
            }
            double numShouldSpawn = getMaxSpawn - ispawnedMonstersOnMap;
            if (mapId == 610020002 || mapId == 610020004) {
                numShouldSpawn *= 2;
            }
            if (numShouldSpawn + ispawnedMonstersOnMap >= getMaxSpawn) {
                numShouldSpawn = getMaxSpawn - ispawnedMonstersOnMap;
            } 
            if (numShouldSpawn <= 0) {
                return;
            }
            
            List<SpawnPoint> randomSpawn = new ArrayList<>(monsterSpawn);
            Collections.shuffle(randomSpawn);
            int spawned = 0;
            for (SpawnPoint spawnPoint : randomSpawn) {
                if (!isSpawns && spawnPoint.getMobTime() > 0) {
                    continue;
                }
                if (spawnPoint.shouldSpawn()) {
                    spawnMonster(spawnPoint.getMonster());
                    spawned++;
                }
                if (spawned >= numShouldSpawn) {
                    break;
                }
            }
        }
    }

    public Collection<SpawnPoint> getSpawnPoints() {
        return monsterSpawn;
    }
    
    public void allowSummonState(boolean b) {
        Field.this.allowSummons = b;
    }

    public boolean getSummonState() {
        return Field.this.allowSummons;
    }
    
    public MCWZData getMCPQData() {
        return this.mcpqData;
    }

    public void setMCPQData(MCWZData data) {
        this.mcpqData = data;
    }  
        
    public void startMapEffect(String msg, int itemId, long time) {
        if (mapEffect != null) {
            return;
        }
        mapEffect = new FieldEffect(msg, itemId);
        broadcastMessage(mapEffect.makeStartData());
        MapTimer.getInstance().schedule(() -> {
            broadcastMessage(mapEffect.makeDestroyData());
            mapEffect = null;
        }, time);
    }
         
    public void warpEveryone(int to) {
        List<Player> players;
        chrRLock.lock();
        try {
            players = new ArrayList<>(getCharacters());
        } finally {
            chrRLock.unlock();
        }

        players.forEach((chr) -> {
            chr.changeMap(to);
        });
    }

    public void removePlayer(Player p) {
        chrWLock.lock();
        try {
            characters.remove(p);
        } finally {
            chrWLock.unlock();
        }
        
        removeMapObject(p.getObjectId());
        
        if (!p.isHidden()) {
            broadcastMessage(PacketCreator.RemovePlayerFromMap(p.getId()));
        } else {
            broadcastGMMessage(PacketCreator.RemovePlayerFromMap(p.getId()));
        }


        final List<MapleMonster> update = new ArrayList<>();
        final Iterator<MapleMonster> controlled = p.getControlled().iterator();

        while (controlled.hasNext()) {
            MapleMonster monster = controlled.next();
            if (monster != null) {
                monster.setController(null);
                monster.setControllerHasAggro(false);
                monster.setControllerKnowsAboutAggro(false);
                controlled.remove();
                update.add(monster);
            }
        }
        update.forEach((mons) -> {
            updateMonsterController(mons);
        });
       
        p.cancelMapTimeLimitTask();

        p.getSummons().values().forEach((summon) -> {
            if (summon.isPuppet()) {
                p.cancelBuffStats(BuffStat.PUPPET);
            } else {
                removeMapObject(summon);
            }
        });
        List<MapleSummon> removes = new LinkedList<>();
        p.getPirateSummons().stream().map((summon) -> {
            removeMapObject(summon);
            return summon;
        }).filter((summon) -> (summon.isOctopus())).forEachOrdered((summon) -> {
            removes.add(summon);
        });
        removes.forEach((summon) -> {
            p.removePirateSummon(summon);
        });
    
        p.leaveMap();
    }
    
    public void clearMapObjects() {
        clearDrops();
        killAllMonsters();
        resetReactors();
    }
           
    public void broadcastGMMessage(OutPacket packet) {
        broadcastGMMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }
    
    public void broadcastMessage(OutPacket packet) {
        broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public void broadcastMessage(Player source, OutPacket packet, boolean repeatToSource) {
        broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    public void broadcastMessage(Player source, OutPacket packet, boolean repeatToSource, boolean ranged) {
        broadcastMessage(repeatToSource ? null : source, packet, ranged ? 722500 : Double.POSITIVE_INFINITY, source.getPosition());
    }

    public void broadcastMessage(OutPacket packet, Point rangedFrom) {
        broadcastMessage(null, packet, 722500, rangedFrom);
    }

    public void broadcastMessage(Player source, OutPacket packet, Point rangedFrom) {
        broadcastMessage(source, packet, 722500, rangedFrom);
    }
    
    public void broadcastMessage(Player source, OutPacket packet) {
        chrRLock.lock();
        try {
            characters.stream().filter((chr) -> (chr != source)).forEachOrdered((chr) -> {
                chr.getClient().announce(packet);
            });
        } finally {
            chrRLock.unlock();
        }
    }

    private void broadcastMessage(final Player source, OutPacket packet, final double rangeSq, final Point rangedFrom) {
        chrRLock.lock();
        try {
            characters.stream().filter((chr) -> (chr != source)).forEachOrdered((chr) -> {
                if (rangeSq < Double.POSITIVE_INFINITY) {
                    if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                        chr.getClient().getSession().write(packet);
                    }
                } else {
                    chr.getClient().getSession().write(packet);
                }
            });
        } finally {
            chrRLock.unlock();
        }
    }
    
     public void broadcastGMMessage(Player source, final OutPacket packet, boolean repeatToSource) {
        broadcastGMMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    private void broadcastGMMessage(Player source, OutPacket packet, double rangeSq, Point rangedFrom) {
        chrRLock.lock();
        try {
            if (source == null) {
                for (Player chr : characters) {
                    if (chr.isGameMaster()) {
                        chr.getClient().announce(packet);
                    }
                }
            } else {
                for (Player chr : characters) {
                    if (chr != source && (chr.getAdministrativeLevel() >= source.getAdministrativeLevel())) {
                        chr.getClient().announce(packet);
                    }
                }
            }
        } finally {
            chrRLock.unlock();
        }
    }
    
    public void broadcastNONGMMessage(Player source, OutPacket packet, boolean repeatToSource) {
        broadcastNONGMMessage(repeatToSource ? null : source, packet);
    }

    public void broadcastNONGMMessage(Player source, OutPacket packet) {
        chrRLock.lock();
        try {
            for (Player chr : characters) {
                if (chr != source && !chr.isGameMaster()) {
                   chr.getClient().announce(packet);
                }
            }
        } finally {
            chrRLock.unlock();
        }
    }
    
    public List<MapleMonster> getAllMonstersThreadsafe() {
        return getAllMonstersThreadsafe(new ArrayList<>());
    }

    public ArrayList<MapleMonster> getAllMonstersThreadsafe(ArrayList<MapleMonster> ret) {
        ret.clear();
        objectRLock.lock();
        try {
            getMonsters().forEach((mmo) -> {
                ret.add((MapleMonster) mmo);
            });
        } finally {
            objectRLock.unlock();
        }
        return ret;
    }
    
    public List<FieldItem> getAllItemsThreadsafe() {
        ArrayList<FieldItem> ret = new ArrayList<>();
        objectRLock.lock();
        try {
            for (FieldObject mmo : mapObjects.values()) {
                if (mmo.getType() == FieldObjectType.ITEM) {
                    ret.add((FieldItem) mmo);
                }
            }
        } finally {
            objectRLock.unlock();
        }
        return ret;
    }
    
    public List<Reactor> getAllReactorsThreadsafe() {
        ArrayList<Reactor> ret = new ArrayList<>();
        objectRLock.lock();
        try {
            for (FieldObject mmo : mapObjects.values()) {
                if (mmo.getType() == FieldObjectType.REACTOR) {
                    ret.add((Reactor) mmo);
                }
            }
        } finally {
            objectRLock.unlock();
        }
        return ret;
    }
    
    private void sendObjectPlacement(Client mapleClient) {
        Player p = mapleClient.getPlayer();
        Collection<FieldObject> objects;
        
        objectRLock.lock();
        try {
            objects = Collections.unmodifiableCollection(mapObjects.values());
        } finally {
            objectRLock.unlock();
        }
        
        for (FieldObject o : objects) {
            if (o.getType() == FieldObjectType.SUMMON) {
                MapleSummon summon = (MapleSummon) o;
                if (summon.getOwner() == p) {
                    if (p.getSummons().isEmpty() || !p.getSummons().containsValue(summon)) {
                        objectWLock.lock();
                        try {
                            mapObjects.remove(o.getObjectId());
                        } finally {
                            objectWLock.unlock();
                        }
                        
                        continue;
                    }
                }
            }
            if (MapConstants.isNonRangedType(o.getType())) {
                o.sendSpawnData(mapleClient);
            } else if (o.getType() == FieldObjectType.MONSTER) {
                updateMonsterController((MapleMonster) o);
            }
        }
        
        if (p != null) {
            for (FieldObject o : getMapObjectsInRange(p.getPosition(), getRangedDistance(), MapConstants.RANGE_FIELD_OBJ)) {
                if (o.getType() == FieldObjectType.REACTOR) {
                    if (((Reactor) o).isAlive()) {
                        o.sendSpawnData(p.getClient());
                        p.addVisibleMapObject(o);
                    }
                } else {
                    o.sendSpawnData(p.getClient());
                    p.addVisibleMapObject(o);
                }
            }
        }
    }
     
    public List<FieldObject> getMapObjectsInRange(Point from, double rangeSq, List<FieldObjectType> types) {
        List<FieldObject> ret = new LinkedList<>();
        objectRLock.lock();
        try {
            for (FieldObject l : mapObjects.values()) {
                if (types.contains(l.getType())) {
                    if (from.distanceSq(l.getPosition()) <= rangeSq) {
                        ret.add(l);
                    }
                }
            }
            return ret;
        } finally {
            objectRLock.unlock();
        }
    }
    
    public List<FieldObject> getMapObjectsInRect(Rectangle box, List<FieldObjectType> types) {
        objectRLock.lock();
        final List<FieldObject> ret = new LinkedList<>();
        try {
            for (FieldObject l : mapObjects.values()) {
                if (types.contains(l.getType())) {
                    if (box.contains(l.getPosition())) {
                        ret.add(l);
                    }
                }
            }
        } finally {
            objectRLock.unlock();
        }
        return ret;
    }

    public void addPortal(Portal myPortal) {
        portals.put(myPortal.getId(), myPortal);
    }

    public Portal getPortal(String portalname) {
        for (Portal port : portals.values()) {
            if (port.getName().equals(portalname)) {
                return port;
            }
        }
        return null;
    }
    
    public Collection<Portal> getPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }
	
    public List<Portal> getAvailableDoorPortals() {
        objectRLock.lock();
        try {
            List<Portal> availablePortals = new ArrayList<>();
            
            getPortals().stream().filter((port) -> (port.getType() == Portal.DOOR_PORTAL)).forEachOrdered((port) -> {
                availablePortals.add(port);
            });         
            return availablePortals;
        } finally {
            objectRLock.unlock();
        }
    }
	
    public Portal findClosestPortal(Point from) {
        Portal closest = getPortal(0);
        double distance, shortestDistance = Double.POSITIVE_INFINITY;
        for (Portal portal : portals.values()) {
            distance = portal.getPosition().distanceSq(from);
            if (distance < shortestDistance) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }
	
    public void removePortals() {
        getPortals().forEach((pt) -> {
            pt.setScriptName("blank");
        });
    }

    public Portal getPortal(int portalid) {
        return portals.get(portalid);
    }

    public void addMapleArea(Rectangle rec) {
        areas.add(rec);
    }

    public List<Rectangle> getAreas() {
        return new ArrayList<>(areas);
    }

    public Rectangle getArea(int index) {
        return areas.get(index);
    }

    public void setFootholds(MapleFootholdTree footholds) {
        this.footholds = footholds;
    }

    public MapleFootholdTree getFootholds() {
        return footholds;
    }
    
    public void resetPQ() {
        resetPQ(1);
    }
    
    public void resetPQ(int difficulty) {
        resetMapObjects(difficulty, true);
    }
    
    public void resetMapObjects(int difficulty, boolean isPq) {
        clearMapObjects();
        
        restoreMapSpawnPoints();
        instanceMapFirstSpawn(difficulty, isPq);
    }
    
    public void restoreMapSpawnPoints() {
        for (SpawnPoint spawnPoint : monsterSpawn) {
            spawnPoint.setDenySpawn(false);
        }
    }
    
    public void instanceMapFirstSpawn(int difficulty, boolean isPq) {
        for(SpawnPoint spawnPoint: allMonsterSpawn) {
            if(spawnPoint.getMobTime() == -1) {   //just those allowed to be spawned only once
                spawnMonster(spawnPoint.getMonster());
            }
        }
    }

    public Player getCharacterById(int id) {
        chrRLock.lock();
        try {
            for (Player c : this.characters) {
                if (c.getId() == id) {
                    return c;
                }
            }
        } finally {
            chrRLock.unlock();
        }
        return null;
    }
    
    public void moveMonster(MapleMonster monster, Point reportedPos) {
        monster.setPosition(reportedPos);
        chrRLock.lock();
        try {
            for (Player chr : characters) {
                updateMapObjectVisibility(chr, monster);
            }
        } finally {
            chrRLock.unlock();
        }
    }
    
    public void movePlayer(Player player, Point newPosition) {
        if (player == null) {
            return;
        }
        player.setPosition(newPosition);
        Collection<FieldObject> visibleObjects = player.getVisibleMapObjects();
        
        objectRLock.lock();
        try {
            FieldObject[] visibleObjectsNow = visibleObjects.toArray(new FieldObject[visibleObjects.size()]);
            
            for (FieldObject mo : visibleObjectsNow) {
                if (mo != null) {
                    if (mapObjects.get(mo.getObjectId()) == mo) {
                        updateMapObjectVisibility(player, mo);
                    } else {
                        player.removeVisibleMapObject(mo);
                    }
                }
            }
        } catch (Exception e) {
            FileLogger.printError("MovePlayer_map.txt", e);
        } finally {
            objectRLock.unlock();
        }
        
        for (FieldObject mo : getMapObjectsInRange(player.getPosition(), getRangedDistance(), MapConstants.RANGE_FIELD_OBJ)) {
            if (!player.isMapObjectVisible(mo)) {
                mo.sendSpawnData(player.getClient());
                player.addVisibleMapObject(mo);
            }
        }
    }
    
    private void updateMapObjectVisibility(Player p, FieldObject mo) {
        if (!p.isMapObjectVisible(mo)) { 
            if (mo.getType() == FieldObjectType.SUMMON || mo.getPosition().distanceSq(p.getPosition()) <= getRangedDistance()) {
                p.addVisibleMapObject(mo);
                mo.sendSpawnData(p.getClient());
            }
        } else if (mo.getType() != FieldObjectType.SUMMON && mo.getPosition().distanceSq(p.getPosition()) > getRangedDistance()) {
            p.removeVisibleMapObject(mo);
            mo.sendDestroyData(p.getClient());
        }
    }
    
    public List<Player> getPlayersInRange(Rectangle box, List<Player> chr) {
        List<Player> character = new LinkedList<>();
        chrRLock.lock();
        try {
            for (Player a : characters) {
                if (chr.contains(a.getClient().getPlayer())) {
                    if (box.contains(a.getPosition())) {
                        character.add(a);
                    }
                }
            }
        } finally {
            chrRLock.unlock();
        }
        return character;
    }
    
    public List<FieldObject> getMapObjectsInBox(Rectangle box, List<FieldObjectType> types) {
        List<FieldObject> ret = new LinkedList<>();
        objectRLock.lock();
        try {
            for (FieldObject l : mapObjects.values()) {
                if (types.contains(l.getType())) {
                    if (box.contains(l.getPosition())) {
                        ret.add(l);
                    }
                }
            }
            return ret;
        } finally {
            objectRLock.unlock();
        }
    }
    
    
    public SpawnPoint findClosestSpawnpoint(Point from) {
        SpawnPoint closest = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        for (SpawnPoint sp : monsterSpawn) {
            double distance = sp.getPosition().distanceSq(from);
            if (distance < shortestDistance) {
                closest = sp;
                shortestDistance = distance;
            }
        }
        return closest;
    }
    
    public Portal findClosestPlayerSpawnpoint(Point from) {
        Portal closest = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        for (Portal portal : portals.values()) {
            double distance = portal.getPosition().distanceSq(from);
            if (portal.getType() >= 0 && portal.getType() <= 1 && distance < shortestDistance && portal.getTargetMapId() == MapConstants.NULL_MAP) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }
    
    public final int getMapObjectSize() {
        return mapObjects.size() + getCharactersSize() - characters.size();
    }
    
    public final int getCharactersSize() {
        return characters.size();
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setClock(boolean hasClock) {
        this.clock = hasClock;
    }

    public boolean hasClock() {
        return clock;
    }

    public void setTown(boolean isTown) {
        this.town = isTown;
    }

    public boolean isTown() {
        return town;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public void setEverlast(boolean everlast) {
        this.everlast = everlast;
    }

    public boolean getEverlast() {
        return everlast;
    }

    public int getSpawnedMonstersOnMap() {
        return spawnedMonstersOnMap.get();
    }
    
    public boolean makeDisappearItemFromMap(FieldObject mapobj) {
        if (mapobj instanceof FieldItem) {
            return makeDisappearItemFromMap((FieldItem) mapobj);
        } else {
            return mapobj == null;  
        }
    }

    private class ActivateItemReactor implements Runnable {

        private final FieldItem mapitem;
        private final Reactor reactor;
        private final Client c;

        public ActivateItemReactor(FieldItem mapitem, Reactor reactor, Client c) {
            this.mapitem = mapitem;
            this.reactor = reactor;
            this.c = c;
        }
        
        @Override
        public void run() {
            reactor.lockReactor();
            try {
                if (reactor.getReactorType() == 100) {
                    if (reactor.getShouldCollect() == true && mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
                        mapitem.lockItem();
                        try {
                            if (mapitem.isPickedUp()) {
                                return;
                            }
                            mapitem.setPickedUp(true);

                            reactor.setShouldCollect(false);
                            Field.this.broadcastMessage(PacketCreator.RemoveItemFromMap(mapitem.getObjectId(), 0, 0), mapitem.getPosition());
                            
                            Field.this.removeMapObject(mapitem);
                            
                            reactor.hitReactor(c);

                            if (reactor.getDelay() > 0) {
                                MapTimer tMan = MapTimer.getInstance();
                                tMan.schedule(new Runnable() {
                                    @Override
                                    public void run() {
                                        reactor.lockReactor();
                                        try {
                                            reactor.resetReactorActions(0);
                                            broadcastMessage(PacketCreator.TriggerReactor(reactor, 0));
                                        } finally {
                                            reactor.unlockReactor();
                                        }
                                    }
                                }, reactor.getDelay());
                            }
                        } finally {
                            mapitem.unlockItem();
                        }
                    }
                }
            } finally {
                reactor.unlockReactor();
            }
        }
    }
   
    private static interface DelayedPacketCreation {
        void sendPackets(Client c);
    }

    private static interface SpawnCondition {
        boolean canSpawn(Player chr);
    }

    public int getHPDecProtect() {
        return this.protectItem;
    }

    public void setHPDecProtect(int delta) {
        this.protectItem = delta;
    }
 
    private int hasBoat() {
        return !boat ? 0 : (docked ? 1 : 2);
    }

    public void setBoat(boolean hasBoat) {
        this.boat = hasBoat;
    }

    public void setDocked(boolean isDocked) {
        this.docked = isDocked;
    }

    public void mapMessage(int type, String message) {
        broadcastMessage(PacketCreator.ServerNotice(type, message));
    }

    public void removeItems() {
        Field map = this;
        double range = Double.POSITIVE_INFINITY;
        List<FieldObject> items = map.getMapObjectsInRange(new Point(0, 0), range, Arrays.asList(FieldObjectType.ITEM));
        for (FieldObject itemmo : items) {
            map.removeMapObject(itemmo);
        }
    }
	
    public MapleMonster findClosestMonster(Point from, double range) {
        MapleMonster closest = null;
        double shortestDistance = range;
        List<FieldObject> monstersi = this.getMapObjectsInRange(from, shortestDistance, Arrays.asList(FieldObjectType.MONSTER));
        for (FieldObject monstermo : monstersi) {
            MapleMonster mob = (MapleMonster) monstermo;
            double distance = mob.getPosition().distanceSq(from);
            if (distance < shortestDistance && mob.getId() != 9300061) {
                closest = mob;
                shortestDistance = distance;
            }
        }
        return closest;
    }
	
    private final class warpAll implements Runnable {
        private final Field toGo;
        private final Field from;

        public warpAll(Field toGoto, Field from) {
            this.toGo = toGoto;
            this.from = from;
        }

        @Override
        public void run() {
            synchronized (toGo) {
                for (Player ppp : characters) {
                    if (ppp.getMap().equals(from)){
                        ppp.changeMap(toGo, toGo.getPortal(0));
                        if (ppp.getEventInstance() != null) {
                            ppp.getEventInstance().unregisterPlayer(ppp);
                        }
                    }
                }
            }
	}
    }
    
    public void warpAllToNearestTown(String reason) {
        this.broadcastMessage(PacketCreator.ServerNotice(5, reason));
        int rid = this.forcedReturnMap == MapConstants.NULL_MAP ? this.returnFieldId : this.forcedReturnMap;
        new warpAll(ChannelServer.getInstance(this.channel).getMapFactory().getMap(rid), this).run();
    }
    
    public void dcAllPlayers() {
        int rid = this.forcedReturnMap == MapConstants.NULL_MAP ? this.returnFieldId : this.forcedReturnMap;
        new warpAll(ChannelServer.getInstance(this.channel).getMapFactory().getMap(rid), this).run();
    }

    public boolean setPortalDisable(boolean v) {
        this.disablePortal = v;
        return disablePortal;
    }

    public boolean getPortalDisable() {
        return this.disablePortal;
    }
    
    public boolean setDisableInvincibilitySkills(boolean v) {
        this.disableInvincibilitySkills = v;
        return disableInvincibilitySkills;
    }

    public boolean getDisableInvincibilitySkills() {
        return this.disableInvincibilitySkills;
    }

    public boolean setDisableDamage(boolean v) {
        this.disableDamage = v;
        return disableDamage;
    }

    public boolean getDisableDamage() {
        return this.disableDamage;
    }

    public boolean setDisableChat(boolean v) {
        this.disableChat = v;
        return disableChat;
    }

    public boolean getDisableChat() {
        return this.disableChat;
    }

    public boolean isSwim() {
        return swim;
    }

    public void setSwim(boolean swim) {
        this.swim = swim;
    }
    
    public final boolean canHurt(final long now) {
        if (lastHurtTime > 0 && lastHurtTime + decHPInterval < now) {
            lastHurtTime = now;
            return true;
        }
        return false;
    }
    
    public final int getHPDec() {
        return decHP;
    }

    public final void setHPDec(final int delta) {
        if (delta > 0) { 
            lastHurtTime = System.currentTimeMillis();
        }
        decHP = (short) delta;
    }

    public final int getHPDecInterval() {
        return decHPInterval;
    }

    public final void setHPDecInterval(final int delta) {
        decHPInterval = delta;
    }
}
