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
package server.life;

import client.player.Player;
import java.awt.Point;
import java.util.concurrent.atomic.AtomicInteger;

public class SpawnPoint {
    
    private final int mobTime, fh, f, team, monsterId;
    private long nextPossibleSpawn;
    private final Point pos;
    private final AtomicInteger spawnedMonsters = new AtomicInteger(0);
    private boolean denySpawn = false;
    private int mobInterval = 5000;

    private final boolean immobile;
    private boolean temporary = false;

    public SpawnPoint(final MapleMonster monster, final Point pos, boolean immobile, final int mobTime, int mobInterval, final int team) {
        super();
        this.monsterId = monster.getId();
        this.pos = new Point(pos);
        this.fh = monster.getFh();
	this.f = monster.getF();
        this.mobTime = mobTime;
        this.immobile = immobile;
        this.mobInterval = mobInterval;
        this.nextPossibleSpawn = System.currentTimeMillis();
        this.team = team;
    }
    
    public int getSpawned() {
        return spawnedMonsters.intValue();
    }

    public final boolean shouldSpawn() {
        return shouldSpawn(System.currentTimeMillis());
    }
    
    public void setDenySpawn(boolean val) {
        denySpawn = val;
    }
    
    public boolean getDenySpawn() {
        return denySpawn;
    }

    public boolean shouldSpawn(long now) {
        if (mobTime < 0 || denySpawn) {
            return false;
        }
        if (((mobTime != 0 || immobile) && spawnedMonsters.get() > 0) || spawnedMonsters.get() > 1) {
            return false;
        }
        
        return nextPossibleSpawn <= now;
    }
    
    public boolean shouldForceSpawn() {
        return !(mobTime < 0 || spawnedMonsters.get() > 0);
    }

    public final MapleMonster getMonster() {
        final MapleMonster mob = new MapleMonster(MapleLifeFactory.getMonster(monsterId));
        mob.setPosition(new Point(pos));
        mob.setTeam(team);
        mob.setFh(fh);
        mob.setF(f);
        spawnedMonsters.incrementAndGet();
        mob.addListener(new MonsterListener() {
            @Override
            public void monsterKilled(int aniTime) {
                nextPossibleSpawn = System.currentTimeMillis();
                if (mobTime > 0) {
                    nextPossibleSpawn += mobTime * 1000;
                } else {
                    nextPossibleSpawn += aniTime;
                }
                spawnedMonsters.decrementAndGet();
            }
            
            @Override
            public void monsterDamaged(Player from, int trueDmg) {}
            
            @Override
            public void monsterHealed(int trueHeal) {}
        });

        if (mobTime == 0) {
            nextPossibleSpawn = System.currentTimeMillis() + mobInterval;
        }
        return mob;
    }
    
    public int getMonsterId() {
        return monsterId;
    }

    public Point getPosition() {
        return pos;
    }

    public int getTeam() {
        return team;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }
    
    public final int getMobTime() {
        return mobTime;
    }

    public final int getF() {
	return f;
    }
    
    public final int getFh() {
	return fh;
    }
}