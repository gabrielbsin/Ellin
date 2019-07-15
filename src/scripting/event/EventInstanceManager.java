/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting.event;

import client.player.Player;
import client.player.skills.PlayerSkill;
import client.player.skills.PlayerSkillFactory;
import community.MapleParty;
import community.MaplePartyCharacter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import tools.Pair;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.script.ScriptException;

import server.MapleStatEffect;
import server.life.MapleMonster;
import constants.ItemConstants;
import database.DatabaseConnection;
import java.awt.Point;
import java.io.File;
import java.sql.Connection;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import packet.creators.EffectPackets;
import packet.creators.PacketCreator;
import provider.MapleDataProviderFactory;
import scripting.AbstractPlayerInteraction;
import server.itens.ItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.npc.MapleNPC;
import server.maps.Field;
import server.maps.FieldManager;
import server.maps.portal.Portal;
import server.maps.reactors.Reactor;
import tools.ConvertTool;
import tools.TimerTools.EventTimer;
import tools.locks.MonitoredLockType;
import tools.locks.MonitoredReentrantLock;
import tools.locks.MonitoredReentrantReadWriteLock;

/**
 *
 * @author Matze
 * @author Ronan
 */
public class EventInstanceManager {
    
    private Map<Integer, Player> chars = new HashMap<>();
    private int leaderId = -1;
    private List<MapleMonster> mobs = new LinkedList<>();
    private Map<Player, Integer> killCount = new HashMap<>();
    private EventManager em;
    private FieldManager mapFactory;
    private String name;
    private Properties props = new Properties();
    private long timeStarted = 0;
    private long eventTime = 0;
    private List<Integer> mapIds = new LinkedList<>();

    private final ReentrantReadWriteLock lock = new MonitoredReentrantReadWriteLock(MonitoredLockType.EIM, true);
    private final ReadLock rL = lock.readLock();
    private final WriteLock wL = lock.writeLock();

    private final Lock pL = new MonitoredReentrantLock(MonitoredLockType.EIM_PARTY, true);
    private final Lock sL = new MonitoredReentrantLock(MonitoredLockType.EIM_SCRIPT, true);

    private ScheduledFuture<?> eventSchedule = null;
    private boolean disposed = false;
    private boolean eventCleared = false;
    private boolean eventStarted = false;

    private Map<Integer, List<Integer>> collectionSet = new HashMap<>(8);
    private Map<Integer, List<Integer>> collectionQty = new HashMap<>(8);
    private Map<Integer, Integer> collectionExp = new HashMap<>(8);

    private List<Integer> onMapClearExp = new ArrayList<>();
    private List<Integer> onMapClearMeso = new ArrayList<>();

    private Map<Integer, Integer> playerGrid = new HashMap<>();

    private Map<Integer, Pair<String, Integer>> openedGates = new HashMap<>();

    private Set<Integer> exclusiveItems = new HashSet<>();
        
    public EventInstanceManager(EventManager em, String name) {
        this.em = em;
        this.name = name;
        mapFactory = new FieldManager(this, MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Map")), MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String")), (byte) 1);//Fk this
        mapFactory.setChannel(em.getChannelServer().getChannel());
    }

    public void setName(String name) {
        this.name = name;
    }

    public EventManager getEm() {
        sL.lock();
        try {
            return em;
        } finally {
            sL.unlock();
        }
    }
        
    public int getEventPlayersJobs() {
        int mask = 0;
        for (Player chr: getPlayers()) {
            mask |= (1 << chr.getJob().getJobNiche());
        }
        return mask;
    }
        
    public void applyEventPlayersItemBuff(int itemId) {
        List<Player> players = getPlayerList();
        MapleStatEffect mse = ItemInformationProvider.getInstance().getItemEffect(itemId);
        if (mse != null) {
            for (Player player: players) {
                mse.applyTo(player);
            }
        }
    }
        
    public void applyEventPlayersSkillBuff(int skillId) {
        applyEventPlayersSkillBuff(skillId, Integer.MAX_VALUE);
    }
        
    public void applyEventPlayersSkillBuff(int skillId, int skillLv) {
        List<Player> players = getPlayerList();
        PlayerSkill skill = PlayerSkillFactory.getSkill(skillId);

        if (skill != null) {
            MapleStatEffect mse = skill.getEffect(Math.min(skillLv, skill.getMaxLevel()));
            if (mse != null) {
                for (Player player: players) {
                    mse.applyTo(player);
                }
            }
        }
    }
        
    public void giveEventPlayersExp(int gain) {
        giveEventPlayersExp(gain, -1);
    }
        
    public void giveEventPlayersExp(int gain, int mapId) {
        if (gain == 0) return;

        List<Player> players = getPlayerList();

        if (mapId == -1) {
            for(Player mc: players) {
                mc.gainExp(gain, true, true);
            }
        } else {
            for(Player mc: players) {
                if(mc.getMapId() == mapId) mc.gainExp(gain, true, true);
            }
        }
    }
        
    public void giveEventPlayersMeso(int gain) {
        giveEventPlayersMeso(gain, -1);
    }
        
    public void giveEventPlayersMeso(int gain, int mapId) {
        if (gain == 0) return;

        List<Player> players = getPlayerList();

        if (mapId == -1) {
            for(Player mc: players) {
                mc.gainMeso(gain, true);
            }
        } else {
            for (Player mc: players) {
                if (mc.getMapId() == mapId) mc.gainMeso(gain, true);
            }
        }
    }

    public void registerPlayer(Player P) {
        if (P == null){
            return;
        }

        try {
            wL.lock();
            try {
                chars.put(P.getId(), P);
            }
            finally {
                wL.unlock();
            }

            P.setEventInstance(this);

            sL.lock();
            try {
                em.getIv().invokeFunction("playerEntry", this, P);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }  
        
    public void exitPlayer(Player p) { 
        if (p == null){
            return;
        }
        try {
            unregisterPlayer(p);

            sL.lock();
            try {
                em.getIv().invokeFunction("playerExit", this, p);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }
        
    public void dropMessage(int type, String message) {
        for (Player p : getPlayers()) {
            p.dropMessage(type, message);
        }
    }

    public void restartEventTimer(long time) {
        stopEventTimer();
        startEventTimer(time);
    }
        
    public void startEventTimer(long time) {
        timeStarted = System.currentTimeMillis();
        eventTime = time;

        for (Player p: getPlayers()) {
            p.announce(PacketCreator.GetClockTimer((int) (time / 1000)));
        }

        eventSchedule = EventTimer.getInstance().schedule(() -> {
            try {
                dismissEventTimer();
                sL.lock();
                try {
                    em.getIv().invokeFunction("scheduledTimeout", EventInstanceManager.this);
                } finally {
                    sL.unlock();
                }
            } catch (ScriptException | NoSuchMethodException ex) {
                Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }, time);
    }
        
    public void addEventTimer(long time) {
        if (eventSchedule != null) {
            if (eventSchedule.cancel(false)) {
                long nextTime = getTimeLeft() + time;
                eventTime += time;

                eventSchedule = EventTimer.getInstance().schedule(() -> {
                    try {
                        dismissEventTimer();
                        
                        sL.lock();
                        try {
                            em.getIv().invokeFunction("scheduledTimeout", EventInstanceManager.this);
                        } finally {
                            sL.unlock();
                        }
                    } catch (ScriptException | NoSuchMethodException ex) {
                        Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }, nextTime);
            }
        } else {
            startEventTimer(time);
        }
    }
        
    private void dismissEventTimer() {
        for (Player p: getPlayers()) {
            p.getClient().getSession().write(PacketCreator.DestroyClock());
        }

        eventSchedule = null;
        eventTime = 0;
        timeStarted = 0;
    }
        
    public void stopEventTimer() {
        if (eventSchedule != null) {
            eventSchedule.cancel(false);
            eventSchedule = null;
        }
        dismissEventTimer();
    }
        
    public boolean isTimerStarted() {
        return eventTime > 0 && timeStarted > 0;
    }

    public long getTimeLeft() {
        return eventTime - (System.currentTimeMillis() - timeStarted);
    }

    public void registerParty(Player p) {
        if (p.isPartyLeader()) {
            registerParty(p.getParty(), p.getMap());
        }
    }
        
    public void registerParty(MapleParty party, Field map) {
        for (MaplePartyCharacter pc : party.getEligibleMembers()) {
            Player c = map.getCharacterById(pc.getId());
            registerPlayer(c);
        }
    }

    public void unregisterPlayer(Player p) {
        try {
            sL.lock();
            try {
                em.getIv().invokeFunction("playerUnregistered", EventInstanceManager.this, p);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        wL.lock();
        try {
            chars.remove(p.getId());
        } finally {
            wL.unlock();
        }

        gridRemove(p);
        dropExclusiveItems(p);

        p.setEventInstance(null);
    }
	
    public int getPlayerCount() {
        rL.lock();
        try {
            return chars.size();
        } finally {
            rL.unlock();
        }
    }
        
    public Player getPlayerById(int id) {
        rL.lock();
        try {
            return chars.get(id);
        } finally {
            rL.unlock();
        }
    }

    public List<Player> getPlayers() {
        rL.lock();
        try {
            return new ArrayList<>(chars.values());
        } finally {
            rL.unlock();
        }
    }

    private List<Player> getPlayerList() {
        rL.lock();
        try {
            return new LinkedList<>(chars.values());
        } finally {
            rL.unlock();
        }
    }
        
    public void registerMonster(MapleMonster mob) {
        if (!mob.getStats().isFriendly()) { 
            mobs.add(mob);
        }
    }

    public void movePlayer(Player chr) {
        try {
            sL.lock();
            try {
                em.getIv().invokeFunction("moveMap", this, chr);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }
        
    public void changedMap(Player chr, int mapId) {  
        try {
            sL.lock();
            try {
                em.getIv().invokeFunction("changedMap", this, chr, mapId);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {}
    }
        
    public void afterChangedMap(Player chr, int mapId) {    
        try {
            sL.lock();
            try {
                em.getIv().invokeFunction("afterChangedMap", this, chr, mapId);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {}
    }
        
    public void changedLeader(Player ldr) {
        try {
            sL.lock();
            try {
                em.getIv().invokeFunction("changedLeader", this, ldr);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
        leaderId = ldr.getId();
    }
	
    public void monsterKilled(MapleMonster mob, boolean hasKiller) {
        sL.lock();
        try {
            mobs.remove(mob);

            if (eventStarted) {
                try {
                    em.getIv().invokeFunction("monsterKilled", mob, this, hasKiller);
                } catch (ScriptException | NoSuchMethodException ex) {
                    ex.printStackTrace();
                }

                if (mobs.isEmpty()) {
                    try {
                        em.getIv().invokeFunction("allMonstersDead", this, hasKiller);
                    } catch (ScriptException | NoSuchMethodException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } finally {
            sL.unlock();
        }
    }
        
    public void friendlyKilled(MapleMonster mob, boolean hasKiller) {
        try {
            sL.lock();
            try {
                em.getIv().invokeFunction("friendlyKilled", mob, this, hasKiller);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) { }
    }

    public void playerKilled(Player p) {
        try {
            sL.lock();
            try {
                em.getIv().invokeFunction("playerDead", this, p);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public boolean revivePlayer(Player p) {
        try {
            Object b;

            sL.lock();
            try {
                b = em.getIv().invokeFunction("playerRevive", this, p);
            } finally {
                sL.unlock();
            }

            if (b instanceof Boolean) {
                return (Boolean) b;
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
        return true;
    }

    public void playerDisconnected(Player p) {
        try {
            sL.lock();
            try {
                em.getIv().invokeFunction("playerDisconnected", this, p);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    /**
     *
     * @param chr
     * @param mob
     */
    public void monsterKilled(Player chr, MapleMonster mob) {
        try {
            Integer kc = killCount.get(chr);
            int inc;

            sL.lock();
            try {
                inc = (int) em.getIv().invokeFunction("monsterValue", this, mob.getId());
            } finally {
                sL.unlock();
            }

            if (kc == null) {
                kc = inc;
            } else {
                kc += inc;
            }
            killCount.put(chr, kc);
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public int getKillCount(Player chr) {
        Integer kc = killCount.get(chr);
        return (kc == null) ? 0 : kc;
    }

    public void cancelSchedule() {
        if (eventSchedule != null) {
            eventSchedule.cancel(false);
            eventSchedule = null;
        }
    }

    public synchronized void dispose() {
        if (disposed) return;

        disposed = true;
        try {
            sL.lock();
            try {
                em.getIv().invokeFunction("dispose", this);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }

        wL.lock();
        try {
            for (Player p: chars.values()) p.setEventInstance(null);
            chars.clear();

            mobs.clear();

            mapFactory.dispose();
            mapFactory = null;
        } finally {
            wL.unlock();
        }

        cancelSchedule();
        killCount.clear();
        mapIds.clear();

        sL.lock();
        try {
            if (!eventCleared) em.disposeInstance(name);
            em = null;
        } finally {
            sL.unlock();
        }
    }

    public FieldManager getMapFactory() {
        return mapFactory;
    }

    public void schedule(final String methodName, long delay) {
        EventTimer.getInstance().schedule(() -> {
            try {
                sL.lock();
                try {
                    if (em == null) return;
                    em.getIv().invokeFunction(methodName, EventInstanceManager.this);
                } finally {
                    sL.unlock();
                }
            } catch (ScriptException | NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }, delay);
    }

    public String getName() {
        return name;
    }

    public Field getMapInstance(int mapId) {
        Field map = mapFactory.getMap(mapId);
        map.setEventInstance(this);

        if (!mapFactory.isMapLoaded(mapId)) {
            sL.lock();
            try {
                if (em.getProperty("shuffleReactors") != null && em.getProperty("shuffleReactors").equals("true")) {
                    map.shuffleReactors();
                }
            } finally {
                sL.unlock();
            }
        }
        return map;
    }

    public void setIntProperty(String key, Integer value) {
        setProperty(key, value);
    }

    public void setProperty(String key, Integer value) {
        setProperty(key, "" + value);
    }
        
    public void setProperty(String key, String value) {
        pL.lock();
        try {
            props.setProperty(key, value);
        } finally {
            pL.unlock();
        }
    }

    public Object setProperty(String key, String value, boolean prev) {
        pL.lock();
        try {
            return props.setProperty(key, value);
        } finally {
            pL.unlock();
        }
    }

    public String getProperty(String key) {
        pL.lock();
        try {
            return props.getProperty(key);
        } finally {
            pL.unlock();
        }
    }

    public int getIntProperty(String key) {
        pL.lock();
        try {
            return Integer.parseInt(props.getProperty(key));
        } finally {
            pL.unlock();
        }
    }
	
    public void leftParty(Player p) {
        try {
            sL.lock();
            try {
                em.getIv().invokeFunction("leftParty", this, p);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public void disbandParty() {
        try {
            sL.lock();
            try {
                em.getIv().invokeFunction("disbandParty", this);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public void clearPQ() {
        try {
            sL.lock();
            try {
                em.getIv().invokeFunction("clearPQ", this);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public void removePlayer(Player p) {
        try {
            sL.lock();
            try {
                em.getIv().invokeFunction("playerExit", this, p);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isLeader(Player p) {
        return (p.getParty().getLeaderId() == p.getId());
    }

    public boolean isEventLeader(Player p) {
        return (p.getId() == getLeaderId());
    }

    public final Field getInstanceMap(final int mapid) { 
        if (disposed) {
            return getMapFactory().getMap(mapid);
        }
        mapIds.add(mapid);
        return getMapFactory().getMap(mapid);
    }
        
    public final boolean disposeIfPlayerBelow(final byte size, final int towarp) {
        if (disposed) {
            return true;
        }
        if (chars == null) {
            return false;
        }

        Field map = null;
        if (towarp > 0) {
            map = this.getMapFactory().getMap(towarp);
        }

        List<Player> players = getPlayerList();

        try {
            if (players.size() < size) {
                for (Player chr : players) {
                    if (chr == null) {
                        continue;
                    }
                    unregisterPlayer(chr);
                    if (towarp > 0) {
                        chr.changeMap(map, map.getPortal(0));
                    }
                }

                dispose();
                return true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
        
    public void spawnNpc(int npcId, Point pos, Field map) {
        MapleNPC npc = MapleLifeFactory.getNPC(npcId);
        if (npc != null) {
            npc.setPosition(pos);
            npc.setCy(pos.y);
            npc.setRx0(pos.x + 50);
            npc.setRx1(pos.x - 50);
            npc.setFh(map.getFootholds().findBelow(pos).getId());
            map.addMapObject(npc);
            map.broadcastMessage(PacketCreator.SpawnNPC(npc));
        }
    }
        
    public void dispatchUpdateQuestMobCount(int mobid, int mapid) {
        Map<Integer, Player> mapChars = getInstanceMap(mapid).getMapPlayers();
        if(!mapChars.isEmpty()) {
            List<Player> eventMembers = getPlayers();

            for (Player evChr : eventMembers) {
                Player p = mapChars.get(evChr.getId());

                if (p != null) {
                    p.updateQuestMobCount(mobid);
                }
            }
        }
    }
        
    public MapleMonster getMonster(int mid) {
        return(MapleLifeFactory.getMonster(mid));
    }
        
    public void setEventClearStageExp(List<Integer> gain) {
        onMapClearExp.clear();
        onMapClearExp.addAll(ConvertTool.ConvertFromScriptInt(gain));
    }

    public void setEventClearStageMeso(List<Integer> gain) {
        onMapClearMeso.clear();
        onMapClearMeso.addAll(ConvertTool.ConvertFromScriptInt(gain));
    }

    public Integer getClearStageExp(int stage) {  
        if (stage > onMapClearExp.size()) return 0;
        return onMapClearExp.get(stage - 1);
    }

    public Integer getClearStageMeso(int stage) {   
        if (stage > onMapClearMeso.size()) return 0;
        return onMapClearMeso.get(stage - 1);
    }

    public List<Integer> getClearStageBonus(int stage) {
        List<Integer> list = new ArrayList<>();
        list.add(getClearStageExp(stage));
        list.add(getClearStageMeso(stage));

        return list;
    }
        
    private void dropExclusiveItems(Player chr) {
        AbstractPlayerInteraction api = chr.getClient().getAbstractPlayerInteraction();

        for (Integer item: exclusiveItems) {
            api.removeAll(item);
        }
    }
        
    public final void setExclusiveItems(List<Integer> items) {
        List<Integer> exclusive = ConvertTool.ConvertFromScriptInt(items);

        wL.lock();
        try {
            for(Integer item: exclusive) {
                exclusiveItems.add(item);
            }
        } finally {
            wL.unlock();
        }
    }
        
    public final void setEventRewards(List<Integer> rwds, List<Integer> qtys, int expGiven) {
        setEventRewards(1, rwds, qtys, expGiven);
    }

    public final void setEventRewards(List<Integer> rwds, List<Integer> qtys) {
        setEventRewards(1, rwds, qtys);
    }

    public final void setEventRewards(int eventLevel, List<Integer> rwds, List<Integer> qtys) {
        setEventRewards(eventLevel, rwds, qtys, 0);
    }
        
    public final void setEventRewards(int eventLevel, List<Integer> rwds, List<Integer> qtys, int expGiven) {
        if (eventLevel <= 0 || eventLevel > 8) return;
        eventLevel--;    

        List<Integer> rewardIds = ConvertTool.ConvertFromScriptInt(rwds);
        List<Integer> rewardQtys = ConvertTool.ConvertFromScriptInt(qtys);

        wL.lock();
        try {
            collectionSet.put(eventLevel, rewardIds);
            collectionQty.put(eventLevel, rewardQtys);
            collectionExp.put(eventLevel, expGiven);
        } finally {
            wL.unlock();
        }
    }
        
    private byte getRewardListRequirements(int level) {
        if (level >= collectionSet.size()) return 0;

        byte rewardTypes = 0;
        List<Integer> list = collectionSet.get(level);

        for (Integer itemId : list) {
            rewardTypes |= (1 << ItemConstants.getInventoryType(itemId).getType());
        }

        return rewardTypes;
    }
        
    private boolean hasRewardSlot(Player player, int eventLevel) {
        byte listReq = getRewardListRequirements(eventLevel);   

        for (byte type = 1; type <= 5; type++) {
            if ((listReq >> type) % 2 == 1 && !player.hasEmptySlot(type))
                return false;
        }

        return true;
    }

    public final boolean giveEventReward(Player player) {
        return giveEventReward(player, 1);
    }

    public final boolean giveEventReward(Player player, int eventLevel) {
        List<Integer> rewardsSet, rewardsQty;
        Integer rewardExp;

        rL.lock();
        try {
            eventLevel--;      
            if (eventLevel >= collectionSet.size()) return true;

            rewardsSet = collectionSet.get(eventLevel);
            rewardsQty = collectionQty.get(eventLevel);

            rewardExp = collectionExp.get(eventLevel);
        } finally {
            rL.unlock();
        }

        if (rewardExp == null) rewardExp = 0;

        if (rewardsSet == null || rewardsSet.isEmpty()) {
            if (rewardExp > 0) player.gainExp(rewardExp);
                return true;
        }

        if (!hasRewardSlot(player, eventLevel)) return false;

        AbstractPlayerInteraction api = player.getClient().getAbstractPlayerInteraction();
        int rnd = (int) Math.floor(Math.random() * rewardsSet.size());

        api.gainItem(rewardsSet.get(rnd), rewardsQty.get(rnd).shortValue());
        if (rewardExp > 0) player.gainExp(rewardExp);
        return true;
    }
        
    public final void startEvent() {
        try {
            sL.lock();
            try {
                eventStarted = true;
                em.getIv().invokeFunction("afterSetup", this);
            } finally {
                sL.unlock();
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public final void setEventCleared() {
        eventCleared = true;

        sL.lock();
        try {
            em.disposeInstance(name);
        } finally {
            sL.unlock();
        }
    }
        
    public final boolean isEventCleared() {
        return eventCleared;
    }

    private boolean isEventTeamLeaderOn() {
        for (Player p: getPlayers()) {
            if(p.getId() == getLeaderId()) return true;
        }

        return false;
    }

    public final boolean checkEventTeamLacking(boolean leavingEventMap, int minPlayers) {
        if (eventCleared && getPlayerCount() > 1) return false;

        if (!eventCleared && leavingEventMap && !isEventTeamLeaderOn()) return true;
        if (getPlayerCount() < minPlayers) return true;

        return false;
    }
        
    public final boolean isEventTeamLackingNow(boolean leavingEventMap, int minPlayers, Player quitter) {
        if (eventCleared) {
                if (leavingEventMap && getPlayerCount() <= 1) return true;
        } else {
            if (leavingEventMap && getLeaderId() == quitter.getId()) return true;
            if (getPlayerCount() <= minPlayers) return true;
        }

        return false;
    }

    public final boolean isEventTeamTogether() {
        rL.lock();
        try {
            if (chars.size() <= 1) return true;

            Iterator<Player> iterator = chars.values().iterator();
            Player mc = iterator.next();
            int mapId = mc.getMapId();

            for (; iterator.hasNext();) {
                mc = iterator.next();
                if(mc.getMapId() != mapId) return false;
            }

            return true;
        } finally {
            rL.unlock();
        }
    }
        
    public final void warpEventTeam(int warpFrom, int warpTo) {
        List<Player> players = getPlayerList();

        for (Player p : players) {
            if (p.getMapId() == warpFrom)
                p.changeMap(warpTo);
        }
    }

    public final void warpEventTeam(int warpTo) {
        List<Player> players = getPlayerList();

        for (Player p : players) {
            p.changeMap(warpTo);
        }
    }
        
    public final void warpEventTeamToMapSpawnPoint(int warpFrom, int warpTo, int toSp) {
        List<Player> players = getPlayerList();

        for (Player p : players) {
            if (p.getMapId() == warpFrom)
                p.changeMap(warpTo, toSp);
        }
    }

    public final void warpEventTeamToMapSpawnPoint(int warpTo, int toSp) {
        List<Player> players = getPlayerList();

        for (Player p : players) {
            p.changeMap(warpTo, toSp);
        }
    }
        
    public final int getLeaderId() {
        rL.lock();
        try {
            return leaderId;
        } finally {
            rL.unlock();
        }
    }

    public Player getLeader() {
        rL.lock();
        try {
            return chars.get(leaderId);
        } finally {
            rL.unlock();
        }
    }

    public final void setLeader(Player chr) {
        wL.lock();
        try {
            leaderId = chr.getId();
        } finally {
            wL.unlock();
        }
    }
        
    public final void showWrongEffect() {
        showWrongEffect(getLeader().getMapId());
    }

    public final void showWrongEffect(int mapId) {
        Field map = getMapInstance(mapId);
        map.broadcastMessage(EffectPackets.ShowEffect("quest/party/wrong_kor"));
        map.broadcastMessage(EffectPackets.PlaySound("Party1/Failed"));
    }

    public final void showClearEffect() {
        showClearEffect(false);
    }

    public final void showClearEffect(boolean hasGate) {
        Player leader = getLeader();
        if (leader != null) showClearEffect(hasGate, leader.getMapId());
    }

    public final void showClearEffect(int mapId) {
        showClearEffect(false, mapId);
    }

    public final void showClearEffect(boolean hasGate, int mapId) {
        showClearEffect(hasGate, mapId, "gate", 2);
    }

    public final void showClearEffect(int mapId, String mapObj, int newState) {
        showClearEffect(true, mapId, mapObj, newState);
    }

    public final void showClearEffect(boolean hasGate, int mapId, String mapObj, int newState) {
        Field map = getMapInstance(mapId);
        map.broadcastMessage(EffectPackets.ShowEffect("quest/party/clear"));
        map.broadcastMessage(EffectPackets.PlaySound("Party1/Clear"));
        if (hasGate) {
            map.broadcastMessage(EffectPackets.EnvironmentChange(mapObj, newState));
            wL.lock();
            try {
                openedGates.put(map.getId(), new Pair<>(mapObj, newState));
            } finally {
                wL.unlock();
            }
        }
    }
        
    public final void recoverOpenedGate(Player p, int thisMapId) {
        rL.lock();
        try {
            if (openedGates.containsKey(thisMapId)) {
                Pair<String, Integer> gateData = openedGates.get(thisMapId);
                p.announce(EffectPackets.EnvironmentChange(gateData.getLeft(), gateData.getRight()));
            }
        } finally {
            rL.unlock();
        }
    }
        
    public final void giveEventPlayersStageReward(int thisStage) {
        List<Integer> list = getClearStageBonus(thisStage);
        giveEventPlayersExp(list.get(0));
        giveEventPlayersMeso(list.get(1));
    }

    public final void linkToNextStage(int thisStage, String eventFamily, int thisMapId) {
        giveEventPlayersStageReward(thisStage);
        thisStage--;    

        Field nextStage = getMapInstance(thisMapId);
        Portal portal = nextStage.getPortal("next00");
        if (portal != null) {
            portal.setScriptName(eventFamily + thisStage);
        }
    }
        
    public final void linkPortalToScript(int thisStage, String portalName, String scriptName, int thisMapId) {
        giveEventPlayersStageReward(thisStage);
        thisStage--;   
        
        Field nextStage = getMapInstance(thisMapId);
        Portal portal = nextStage.getPortal(portalName);
        if (portal != null) {
            portal.setScriptName(scriptName);
        }
    }
        
    public final void gridInsert(Player chr, int newStatus) {
        wL.lock();
        try {
            playerGrid.put(chr.getId(), newStatus);
        } finally {
            wL.unlock();
        }
    }
       
    public final void gridRemove(Player chr) {
        wL.lock();
        try {
            playerGrid.remove(chr.getId());
        } finally {
            wL.unlock();
        }
    }

    public final int gridCheck(Player chr) {
        rL.lock();
        try {
            Integer i = playerGrid.get(chr.getId());
            return (i != null) ? i : -1;
        } finally {
            rL.unlock();
        }
    }
        
    public final int gridSize() {
        rL.lock();
        try {
            return playerGrid.size();
        } finally {
            rL.unlock();
        }
    }
        
    public final void gridClear() {
        wL.lock();
        try {
            playerGrid.clear();
        } finally {
            wL.unlock();
        }
    }
        
    public boolean activatedAllReactorsOnMap(int mapId, int minReactorId, int maxReactorId) {
        return activatedAllReactorsOnMap(this.getMapInstance(mapId), minReactorId, maxReactorId);
    }
 
    public boolean activatedAllReactorsOnMap(Field map, int minReactorId, int maxReactorId) {
        if (map == null) return true;
        for (Reactor r : map.getReactorsByIdRange(minReactorId, maxReactorId)) {
            if (r.getReactorType() != -1) {
                return false;
            }
        }
        return true;     
    }
}