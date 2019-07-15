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
import community.MapleGuild;
import community.MapleParty;
import community.MaplePartyCharacter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Invocable;
import javax.script.ScriptException;

import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.service.GuildService;
import server.life.MapleMonster;
import server.life.MapleLifeFactory;
import server.quest.MapleQuest;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import server.maps.Field;
import tools.ConvertTool;
import tools.TimerTools.EventTimer;
import tools.locks.MonitoredLockType;
import tools.locks.MonitoredReentrantLock;

/**
 *
 * @author Matze
 * @author Ronan
 */
public class EventManager {
    
    private Invocable iv;
    private String name;
    private ChannelServer cserv;
    private Integer readyId = 0;
    private List<Boolean> openedLobbys;
    private Properties props = new Properties();
    private Map<String, Integer> instanceLocks = new HashMap<>();
    private final Queue<Integer> queuedGuilds = new LinkedList<>();
    private Map<String, EventInstanceManager> instances = new HashMap<>();
    private final Map<Integer, Integer> queuedGuildLeaders = new HashMap<>();
    private List<EventInstanceManager> readyInstances = new LinkedList<>();
    private Lock lobbyLock = new MonitoredReentrantLock(MonitoredLockType.EM_LOBBY);
    private Lock queueLock = new MonitoredReentrantLock(MonitoredLockType.EM_QUEUE);

    private static final int maxLobbys = 8;     
    
    public EventManager(ChannelServer cserv, Invocable iv, String name) {
        this.iv = iv;
        this.cserv = cserv;
        this.name = name;
        
        this.openedLobbys = new ArrayList<>();
        for (int i = 0; i < maxLobbys; i++) this.openedLobbys.add(false);
    }

    public void cancel() {
        try {
            iv.invokeFunction("cancelSchedule", (Object) null);
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }
    
    public long getLobbyDelay() {
        return 10;
    }
    
    private List<Integer> getLobbyRange() {
        try {
            return ConvertTool.ConvertFromScriptInt(iv.invokeFunction("setLobbyRange", (Object) null));
        } catch (ScriptException | NoSuchMethodException ex) { 
            List<Integer> defaultRange = new ArrayList<>();
            defaultRange.add(0);
            defaultRange.add(maxLobbys);
            
            return defaultRange;
        }
    }

    public ScheduledFuture<?> schedule(String methodName, long delay) {
        return schedule(methodName, null, delay);
    }

    public ScheduledFuture<?> schedule(final String methodName, final EventInstanceManager eim, long delay) {
        return EventTimer.getInstance().schedule(() -> {
            try {
                iv.invokeFunction(methodName, eim);
            } catch (ScriptException | NoSuchMethodException ex) {
                Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }, delay);
    }

    public ScheduledFuture<?> scheduleAtTimestamp(final String methodName, long timestamp) {
        return EventTimer.getInstance().scheduleAtTimestamp(() -> {
            try {
                iv.invokeFunction(methodName, (Object) null);
            } catch (ScriptException | NoSuchMethodException ex) {
                Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }, timestamp);
    }

    
    public ChannelServer getChannelServer() {
        return cserv;
    }
    
    public Invocable getIv() {
        return iv;
    }

    public EventInstanceManager getInstance(String name) {
        return instances.get(name);
    }

    public Collection<EventInstanceManager> getInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public EventInstanceManager newInstance(String name) {
        EventInstanceManager ret = getReadyInstance();
        
        if (ret == null) {
            ret = new EventInstanceManager(this, name);
        } else {
            ret.setName(name);
        }
        
        instances.put(name, ret);
        return ret;
    }

    public void disposeInstance(final String name) {
        EventTimer.getInstance().schedule(() -> {
            freeLobbyInstance(name);
            instances.remove(name);
        }, 10 * 1000);
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }
    
    public void setIntProperty(String key, int value) {
        setProperty(key, value);
    }
    
    public void setProperty(String key, int value) {
        props.setProperty(key, value + "");
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }
    
    public int getIntProperty(String key) {
        return Integer.parseInt(props.getProperty(key));
    }
    
    private void setLockLobby(int lobbyId, boolean lock) {
        lobbyLock.lock();
        try {
            openedLobbys.set(lobbyId, lock);
        } finally {
            lobbyLock.unlock();
        }
    }
    
    private boolean startLobbyInstance(int lobbyId) {
        lobbyLock.lock();
        try {
            if(!openedLobbys.get(lobbyId)) {
                openedLobbys.set(lobbyId, true);
                return true;
            }
            
            return false;
        } finally {
            lobbyLock.unlock();
        }
    }
    
    private void freeLobbyInstance(String lobbyName) {
        Integer i = instanceLocks.get(lobbyName);
        if(i == null) return;
        
        instanceLocks.remove(lobbyName);
        if(i > -1) setLockLobby(i, false);
    }

    public String getName() {
        return name;
    }
    
    public int availableLobbyInstance() {
        List<Integer> lr = getLobbyRange();
        int lb = 0, hb = 0;

        if (lr.size() >= 2) {
            lb = Math.max(lr.get(0), 0);
            hb = Math.min(lr.get(1), maxLobbys - 1);
        }

        for (int i = lb; i <= hb; i++) {
            if (startLobbyInstance(i)) {
                return i;
            }
        }

        return -1;
    }
    
    public boolean startInstance(Player chr) {
        return startInstance(-1, chr);
    }
    
    public boolean startInstance(int lobbyId, Player leader) {
        return startInstance(lobbyId, leader, leader, 1);
    }
    
    public boolean startInstance(int lobbyId, Player p, Player leader, int difficulty) {
        try {
            if (lobbyId == -1) {
                lobbyId = availableLobbyInstance();
                if (lobbyId == -1) return false;
            }
            else {
                if (!startLobbyInstance(lobbyId)) return false;
            }
            
            EventInstanceManager eim = (EventInstanceManager) (iv.invokeFunction("setup", difficulty, (lobbyId > -1) ? lobbyId : leader.getId()));
            if (eim == null) {
                if (lobbyId > -1) setLockLobby(lobbyId, false);
                return false;
            }
            instanceLocks.put(eim.getName(), lobbyId);
            eim.setLeader(leader);
            
            if (p != null) eim.registerPlayer(p);
            
            eim.startEvent();
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }    

    public boolean startInstance(MapleParty party, Field map) {
        return startInstance(-1, party, map);
    }
    
    public boolean startInstance(int lobbyId, MapleParty party, Field map) {
        return startInstance(lobbyId, party, map, party.getLeader().getPlayer());
    }
    
    public boolean startInstance(int lobbyId, MapleParty party, Field map, Player leader) {
        try {
            if (lobbyId == -1) {
                lobbyId = availableLobbyInstance();
                if (lobbyId == -1) return false;
            }
            else {
                if (!startLobbyInstance(lobbyId)) return false;
            }
            
            EventInstanceManager eim = (EventInstanceManager) (iv.invokeFunction("setup", (Object) null));
            if (eim == null) {
                if(lobbyId > -1) setLockLobby(lobbyId, false);
                return false;
            }
            instanceLocks.put(eim.getName(), lobbyId);
            eim.setLeader(leader);
            
            eim.registerParty(party, map);
            party.setEligibleMembers(null);
            
            eim.startEvent();
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    public boolean startInstance(MapleParty party, Field map, int difficulty) {
        return startInstance(-1, party, map, difficulty);
    }
    
    public boolean startInstance(int lobbyId, MapleParty party, Field map, int difficulty) {
        return startInstance(lobbyId, party, map, difficulty, party.getLeader().getPlayer());
    }
    
    public boolean startInstance(int lobbyId, MapleParty party, Field map, int difficulty, Player leader) {
        try {
            if (lobbyId == -1) {
                lobbyId = availableLobbyInstance();
                if (lobbyId == -1) return false;
            }
            else {
                if (!startLobbyInstance(lobbyId)) return false;
            }
            
            EventInstanceManager eim = (EventInstanceManager) (iv.invokeFunction("setup", difficulty, (lobbyId > -1) ? lobbyId : party.getLeaderId()));
            if (eim == null) {
                if (lobbyId > -1) setLockLobby(lobbyId, false);
                return false;
            }
            instanceLocks.put(eim.getName(), lobbyId);
            eim.setLeader(leader);
            
            eim.registerParty(party, map);
            party.setEligibleMembers(null);
            
            eim.startEvent();
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }

    public boolean startInstance(EventInstanceManager eim, String ldr) {
        return startInstance(-1, eim, ldr);
    }
    
    public boolean startInstance(EventInstanceManager eim, Player ldr) {
        return startInstance(-1, eim, ldr.getName(), ldr);
    }
    
    public boolean startInstance(int lobbyId, EventInstanceManager eim, String ldr) {
        return startInstance(-1, eim, ldr, eim.getEm().getChannelServer().getPlayerStorage().getCharacterByName(ldr)); 
    }
    
    public boolean startInstance(int lobbyId, EventInstanceManager eim, String ldr, Player leader) {
        try {
            if (lobbyId == -1) {
                lobbyId = availableLobbyInstance();
                if (lobbyId == -1) return false;
            }
            else {
                if (!startLobbyInstance(lobbyId)) return false;
            }
            
            if (eim == null) {
                if (lobbyId > -1) setLockLobby(lobbyId, false);
                return false;
            }
            instanceLocks.put(eim.getName(), lobbyId);
            eim.setLeader(leader);
            
            iv.invokeFunction("setup", eim);
            eim.setProperty("leader", ldr);
            
            eim.startEvent();
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    public List<MaplePartyCharacter> getEligibleParty(MapleParty party) {
        if (party == null) {
            return(new ArrayList<>());
        }
        try {
            Object p = iv.invokeFunction("getEligibleParty", party.getPartyMembers());
            
            if (p != null) {
                List<MaplePartyCharacter> lmpc = ConvertTool.ConvertFromScriptArray(p);
                party.setEligibleMembers(lmpc);
                return lmpc;
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }

        return(new ArrayList<>());
    }
    
    public void clearPQ(EventInstanceManager eim) {
        try {
            iv.invokeFunction("clearPQ", eim);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void clearPQ(EventInstanceManager eim, Field toMap) {
        try {
            iv.invokeFunction("clearPQ", eim, toMap);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public MapleMonster getMonster(int mid) {
        return(MapleLifeFactory.getMonster(mid));
    }
    
    private void exportReadyGuild(Integer guildId) {
        MapleGuild mg = GuildService.getGuild(guildId);
        String callout = "[Guild Quest] Your guild has been registered to attend to the Sharenian Guild Quest at channel " + this.getChannelServer().getChannel()
                       + " and HAS JUST STARTED THE STRATEGY PHASE. After 3 minutes, no more guild members will be allowed to join the effort."
                       + " Check out Shuang at the excavation site in Perion for more info.";
        
        mg.dropMessage(6, callout);
    }
    
    private void exportMovedQueueToGuild(Integer guildId, int place) {
        MapleGuild mg = GuildService.getGuild(guildId);
        String callout = "[Guild Quest] Your guild has been registered to attend to the Sharenian Guild Quest at channel " + this.getChannelServer().getChannel()
                       + " and is currently on the " + GameConstants.ordinal(place) + " place on the waiting queue.";
        
        mg.dropMessage(6, callout);
    }
    
    private List<Integer> getNextGuildQueue() {
        synchronized (queuedGuilds) {
            Integer guildId = queuedGuilds.poll();
            if (guildId == null) return null;
            
            GuildService.removeGuildQueued(guildId);
            Integer leaderId = queuedGuildLeaders.remove(guildId);
            
            exportReadyGuild(guildId);
            
            int place = 1;
            for(Integer i: queuedGuilds) {
                exportMovedQueueToGuild(i, place);
                place++;
            }
            
            List<Integer> list = new ArrayList<>(2);
            list.add(guildId); list.add(leaderId);
            return list;
        }
    }
    
    public boolean isQueueFull() {
        synchronized(queuedGuilds) {
            return queuedGuilds.size() >= 10;
        }
    }
    
    public int getQueueSize() {
        synchronized(queuedGuilds) {
            return queuedGuilds.size();
        }
    }
    
    public byte addGuildToQueue(Integer guildId, Integer leaderId) {
        if (GuildService.isGuildQueued(guildId)) return -1;
        
        if (!isQueueFull()) {
            boolean canStartAhead;
            synchronized(queuedGuilds) {
                canStartAhead = queuedGuilds.isEmpty();

                queuedGuilds.add(guildId);
                GuildService.putGuildQueued(guildId);
                queuedGuildLeaders.put(guildId, leaderId);

                int place = queuedGuilds.size();
                exportMovedQueueToGuild(guildId, place);
            }
            
            if (canStartAhead) {
                if (!attemptStartGuildInstance()) {
                    synchronized(queuedGuilds) {
                        queuedGuilds.add(guildId);
                        GuildService.putGuildQueued(guildId);
                        queuedGuildLeaders.put(guildId, leaderId);
                    }
                } else {
                    return 2;
                }
            }
            
            return 1;
        } else {
            return 0;
        }
    }
    
    public boolean attemptStartGuildInstance() {
        Player p = null;
        while (p == null) {
            List<Integer> guildInstance = getNextGuildQueue();
            if (guildInstance == null) {
                return false;
            }

            p = cserv.getPlayerStorage().getCharacterById(guildInstance.get(1));
        }
        
        return startInstance(p);
    }
    
    public void startQuest(Player p, int id, int npcid) {
        try {
            MapleQuest.getInstance(id).forceStart(p, npcid);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    public void completeQuest(Player p, int id, int npcid) {
        try {
            MapleQuest.getInstance(id).forceComplete(p, npcid);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }
    
    private void fillEimQueue() {
        Thread t = new Thread(new EventManagerWorker()); 
        t.start();
    }
    
    private EventInstanceManager getReadyInstance() {
        queueLock.lock();
        try {
            if(readyInstances.isEmpty()) {
                fillEimQueue();
                return null;
            }
            
            EventInstanceManager eim = readyInstances.remove(0);
            fillEimQueue();
                    
            return eim;
        } finally {
            queueLock.unlock();
        }
    }
    
    private void instantiateQueuedInstance() {
        queueLock.lock();
        try {
            if(readyInstances.size() >= Math.ceil((double)maxLobbys / 3.0)) return;
            
            readyInstances.add(new EventInstanceManager(this, "sampleName" + readyId));
            readyId++;
        } finally {
            queueLock.unlock();
        }
        
        instantiateQueuedInstance();   
    }
    
    private class EventManagerWorker implements Runnable {
    
        @Override
        public void run() {
            instantiateQueuedInstance();
        }
    }
}