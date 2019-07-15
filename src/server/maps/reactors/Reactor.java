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
package server.maps.reactors;


import client.Client;
import constants.GameConstants;
import java.awt.Rectangle;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;import packet.creators.PacketCreator;
import packet.transfer.write.OutPacket;

import scripting.reactor.ReactorScriptManager;
import server.maps.Field;
import server.maps.object.AbstractMapleFieldObject;
import server.maps.object.FieldObjectType;
import tools.Pair;
import tools.TimerTools.MapTimer;


/**
 * @author Lerk, Ronan
 */

public class Reactor extends AbstractMapleFieldObject {
    private int rid;
    private ReactorStats stats;
    private byte state;
    private byte evstate;
    private int delay;
    private Field map;
    private String name;
    private boolean alive;
    private boolean shouldCollect;
    private boolean attackHit;
    private ScheduledFuture<?> timeoutTask = null;
    private Lock reactorLock = new ReentrantLock(true);
    private Lock hitLock = new ReentrantLock(true);

    public Reactor(ReactorStats stats, int rid) {
        this.evstate = (byte)0;
        this.stats = stats;
        this.rid = rid;
        this.alive = true;
    }
    
    public void setShouldCollect(boolean collect) {
        this.shouldCollect = collect;
    }
    
    public boolean getShouldCollect() {
        return shouldCollect;
    }
    
    public void lockReactor() {
        reactorLock.lock();
    }
    
    public void unlockReactor() {
        reactorLock.unlock();
    }

    public void setState(byte state) {
        this.state = state;
    }
    
    public byte getState() {
        return state;
    }
    
    public void setEventState(byte substate) {
        this.evstate = substate;
    }
    
    public byte getEventState() {
        return evstate;
    }
    
    public ReactorStats getStats() {
        return stats;
    }

    public int getId() {
        return rid;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getDelay() {
        return delay;
    }

    @Override
    public FieldObjectType getType() {
        return FieldObjectType.REACTOR;
    }

    public int getReactorType() {
        return stats.getType(state);
    }
    
    public boolean isRecentHitFromAttack() {
        return attackHit;
    }

    public void setMap(Field map) {
        this.map = map;
    }

    public Field getMap() {
        return map;
    }

    public Pair<Integer, Integer> getReactItem(byte index) {
        return stats.getReactItem(state, index);
    }

    public boolean isAlive() {
        return alive;
    }
    
    public boolean isActive() {
        return alive && stats.getType(state) != -1;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Override
    public void sendDestroyData(Client client) {
        client.announce(makeDestroyData());
    }

    public final OutPacket makeDestroyData() {
        return PacketCreator.DestroyReactor(this);
    }

    @Override
    public void sendSpawnData(Client client) {
        client.announce(makeSpawnData());
    }

    public final OutPacket makeSpawnData() {
        return PacketCreator.SpawnReactor(this);
    }

    public void resetReactorActions(int newState) {
        setState((byte) newState);
        cancelReactorTimeout();
        setShouldCollect(true);
        refreshReactorTimeout();
        
        if (map != null) map.searchItemReactors(this);
    }
    
    public void forceHitReactor(final byte newState) {
        this.lockReactor();
        try {
            this.resetReactorActions(newState);
            map.broadcastMessage(PacketCreator.TriggerReactor(this, (short) 0));
        } finally {
            this.unlockReactor();
        }
    }
    
    private void tryForceHitReactor(final byte newState) {
        if (!this.reactorLock.tryLock()) return;
        
        try {
            this.resetReactorActions(newState);
            map.broadcastMessage(PacketCreator.TriggerReactor(this, (short) 0));
        } finally {
            this.unlockReactor();
        }
    }
    
    public void cancelReactorTimeout() {
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
            timeoutTask = null;
        }
    }
    
    private void refreshReactorTimeout() {
        int timeOut = stats.getTimeout(state);
        if (timeOut > -1) {
            final byte nextState = stats.getTimeoutState(state);
            
            timeoutTask = MapTimer.getInstance().schedule(() -> {
                timeoutTask = null;
                tryForceHitReactor(nextState);
            }, timeOut);
        }
    }
    
    public void delayedHitReactor(final Client c, long delay) {
        MapTimer.getInstance().schedule(() -> {
            hitReactor(c);
        }, delay);
    }

    public void hitReactor(Client c) {
        hitReactor(false, 0, (short) 0, c);
    }
    
    public void hitReactor(boolean wHit, int charPos, short stance, Client c) {
        try {
            if(!this.isActive()) {
                return;
            }
            
            if (c.getPlayer().getMCPQField() != null) {
                c.getPlayer().getMCPQField().onGuardianHit(c.getPlayer(), this);
                return;
            }
            
            if (hitLock.tryLock()) {
                this.lockReactor();
                try {
                    cancelReactorTimeout();
                    attackHit = wHit;

                    if (GameConstants.USE_DEBUG == true) c.getPlayer().dropMessage(5, "Hitted REACTOR " + this.getId() + " with POS " + charPos + " , STANCE " + stance + " , STATE " + stats.getType(state) + " STATESIZE " + stats.getStateSize(state));
                    ReactorScriptManager.getInstance().onHit(c, this);

                    int reactorType = stats.getType(state);
                    if (reactorType < 999 && reactorType != -1) {
                        if (!(reactorType == 2 && (stance == 0 || stance == 2))) { 
                            for (byte b = 0; b < stats.getStateSize(state); b++) {
                                state = stats.getNextState(state, b);
                                if (stats.getNextState(state, b) == -1) {
                                    if (reactorType < 100) {
                                        if (delay > 0) {
                                            map.destroyReactor(getObjectId());
                                        } else {
                                            map.broadcastMessage(PacketCreator.TriggerReactor(this, stance));
                                        }
                                    } else {
                                        map.broadcastMessage(PacketCreator.TriggerReactor(this, stance));
                                    }

                                    ReactorScriptManager.getInstance().act(c, this);
                                } else { 
                                    map.broadcastMessage(PacketCreator.TriggerReactor(this, stance));
                                    if (state == stats.getNextState(state, b)) {
                                        ReactorScriptManager.getInstance().act(c, this);
                                    }

                                    setShouldCollect(true);    
                                    refreshReactorTimeout();
                                    if(stats.getType(state) == 100) {
                                        map.searchItemReactors(this);
                                    }
                                }
                                break;
                            }
                        }
                    } else {
                        state++;
                        map.broadcastMessage(PacketCreator.TriggerReactor(this, stance));
                        ReactorScriptManager.getInstance().act(c, this);

                        setShouldCollect(true);
                        refreshReactorTimeout();
                        if(stats.getType(state) == 100) {
                            map.searchItemReactors(this);
                        }
                    }
                } finally {
                    this.unlockReactor();
                }
                
                hitLock.unlock();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public Rectangle getArea() {
        return new Rectangle(getPosition().x + stats.getTL().x, getPosition().y + stats.getTL().y, stats.getBR().x - stats.getTL().x, stats.getBR().y - stats.getTL().y);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
