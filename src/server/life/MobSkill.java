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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import client.player.Player;
import client.player.buffs.Disease;
import server.life.components.BanishInfo;
import server.life.status.MonsterStatus;
import server.maps.object.FieldObject;
import server.maps.object.FieldObjectType;
import server.maps.MapleMist;

public class MobSkill {

    private final int skillId;
    private final int skillLevel;
    private int mpCon;
    private int spawnEffect;
    private int hp;
    private int x;
    private int y;
    private int limit;
    private long duration;
    private long cooltime;
    private float prop;
    private Point lt,  rb;
    private final List<Integer> toSummon = new ArrayList<>();

    public MobSkill(int skillId, int level) {
        this.skillId = skillId;
        this.skillLevel = level;
    }

    public void setMpCon(int mpCon) {
        this.mpCon = mpCon;
    }

    public void addSummons(List<Integer> toSummon) {
        toSummon.forEach((summon) -> {
            this.toSummon.add(summon);
        });
    }

    public void setSpawnEffect(int spawnEffect) {
        this.spawnEffect = spawnEffect;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setCoolTime(long cooltime) {
        this.cooltime = cooltime;
    }

    public void setProp(float prop) {
        this.prop = prop;
    }

    public void setLtRb(Point lt, Point rb) {
        this.lt = lt;
        this.rb = rb;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void applyEffect(Player player, MapleMonster monster, boolean skill) {
        MonsterStatus monStat = null;
        Disease disease = null;
        boolean dispel = false;
        boolean seduce = false;
        switch (skillId) {
            case 100:
            case 110:
                monStat = MonsterStatus.WEAPON_ATTACK_UP;
                break;
            case 101:
            case 111:
                monStat = MonsterStatus.MAGIC_ATTACK_UP;
                break;
            case 102:
            case 112:
                monStat = MonsterStatus.WEAPON_DEFENSE_UP;
                break;
            case 103:
            case 113:
                monStat = MonsterStatus.MAGIC_DEFENSE_UP;
                break;
            case 114: 
                if (lt != null && rb != null && skill && monster != null) {
                    List<FieldObject> objects = getObjectsInRange(monster, FieldObjectType.MONSTER);
                    final int hp = (getX() / 1000) * (int) (950 + 1050 * Math.random());
                    for (FieldObject mons : objects) {
                        ((MapleMonster) mons).heal(hp, getY());
                    }
                } else if (monster != null) {
                    monster.heal(getX(), getY());
                }
                break;
            case 120:
            case 121:
            case 122:
            case 123:
            case 124: 
            case 125:
            case 126:
                disease = Disease.getType(skillId);
                break;
            case 128: 
                seduce = true;
                break;
            case 127:
                if (lt != null && rb != null && skill && monster != null && player != null) {
                    for (Player p : getPlayersInRange(monster, player)) {
                        p.dispel();
                    }
                } else if (player != null) {
                    player.dispel();
                }
                break;
            case 129: 
                if (monster != null) {
                    final BanishInfo info = monster.getStats().getBanishInfo();
                    if (info != null) {
                        if (lt != null && rb != null && skill && player != null) {
                            for (Player p : getPlayersInRange(monster, player)) {
                                p.changeMapBanish(info.getMap(), info.getPortal(), info.getMsg());
                            }
                        } else if (player != null) {
                            player.changeMapBanish(info.getMap(), info.getPortal(), info.getMsg());
                        }
                    }
                }
                break;
            case 131: 
                if (monster != null) {
                    monster.getMap().spawnMist(new MapleMist(calculateBoundingBox(monster.getPosition(), true), monster, this), x * 10, false, false, false);
                }
                break;    
            case 140:
                if (makeChanceResult() && !monster.isBuffed(MonsterStatus.MAGIC_IMMUNITY)) {
                    monStat = MonsterStatus.WEAPON_IMMUNITY;
                }
                break;
            case 141:
                if (makeChanceResult() && !monster.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) {
                    monStat = MonsterStatus.MAGIC_IMMUNITY;
                }
                break;
            case 150:    
                monStat = MonsterStatus.WEAPON_ATTACK_UP;
                break;
            case 151:
                monStat = MonsterStatus.WEAPON_DEFENSE_UP;
                break;
            case 152:
                monStat = MonsterStatus.MAGIC_ATTACK_UP;
                break;
            case 153:
                monStat = MonsterStatus.MAGIC_DEFENSE_UP;
                break;
            case 154:
                monStat = MonsterStatus.ACC;
                break;
            case 155:
                monStat = MonsterStatus.AVOID;
                break;
            case 156:
                monStat = MonsterStatus.SPEED;
                break;      
            case 200:
                if (monster == null) {
                    return;
                }
                for (Integer mobId : getSummons()) {
                    MapleMonster toSpawn = null;
                    try {
                        toSpawn = MapleLifeFactory.getMonster(mobId);
                    } catch (RuntimeException e) {
                        continue;
                    }
                    if (toSpawn == null) {
                        continue;
                    }

                    toSpawn.setPosition(monster.getPosition());
                    int yPos = (int) monster.getPosition().getY(), xPos = (int) monster.getPosition().getX();
                    switch (mobId) {
                        case 8500003:  
                            toSpawn.setFh((int) Math.ceil(Math.random() * 19.0));
                            yPos = -590;
                            break;
                        case 8500004:  
                            yPos = (int) (monster.getPosition().getX() + Math.ceil(Math.random() * 1000.0) - 500);
                            yPos = (int) monster.getPosition().getY();
                            break;
                        case 8510100:  
                            if (Math.ceil(Math.random() * 5) == 1) {
                                yPos = 78;
                                xPos = (int) (0 + Math.ceil(Math.random() * 5)) + ((Math.ceil(Math.random() * 2) == 1) ? 180 : 0);
                            } else {
                                xPos = (int) (monster.getPosition().getX() + Math.ceil(Math.random() * 1000.0) - 500);
                            }
                        break;
                    }
                    switch (monster.getMap().getId()) {
                        case 220080001:  
                            if (xPos < -890) {
                                xPos = (int) (-890 + Math.ceil(Math.random() * 150));
                            } else if (xPos > 230) {
                                xPos = (int) (230 - Math.ceil(Math.random() * 150));
                            }
                        break;
                        case 230040420: 
                            if (xPos < -239) {
                                xPos = (int) (-239 + Math.ceil(Math.random() * 150));
                            } else if (xPos > 371) {
                                xPos = (int) (371 - Math.ceil(Math.random() * 150));
                            }
                        break;
                    }
                    toSpawn.setPosition(new Point(xPos, yPos));
                    monster.getMap().spawnMonsterWithEffect(toSpawn, getSpawnEffect(), toSpawn.getPosition());
                }
            break;
        }
        if (monStat != null) {
            if (lt != null && rb != null && skill) {
                List<FieldObject> objects = getObjectsInRange(monster, FieldObjectType.MONSTER);
                for (FieldObject mons : objects) {
                    if (!monster.isBuffed(monStat)) {
                        ((MapleMonster) mons).applyMonsterBuff(monStat, getX(), getSkillId(), getDuration(), this);
                    }
                }
            } else {
                monster.applyMonsterBuff(monStat, getX(), getSkillId(), getDuration(), this);
            }
        } 
        if (disease != null || dispel || seduce) {
            if (skill && lt != null && rb != null) {
                int i = 0;
                for (Player character : getPlayersInRange(monster, player)) {
                    if (dispel) {
                        character.dispel();
                    } else if (seduce && i < 10) {
                        character.giveDebuff(Disease.SEDUCE, this);
                        i++;
                    } else {
                        character.giveDebuff(disease, this);
                    }
                }
            } else {
                if (dispel) {
                    player.dispel();
                } else {
                    player.giveDebuff(disease, this);
                }
            }
        }
        monster.usedSkill(skillId, skillLevel, cooltime);
        monster.setMp(monster.getMp() - getMpCon());
    }

    public int getSkillId() {
        return skillId;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public int getMpCon() {
        return mpCon;
    }

    public List<Integer> getSummons() {
        return Collections.unmodifiableList(toSummon);
    }

    public int getSpawnEffect() {
        return spawnEffect;
    }

    public int getHP() {
        return hp;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public long getDuration() {
        return duration;
    }

    public long getCoolTime() {
        return cooltime;
    }

    public Point getLt() {
        return lt;
    }

    public Point getRb() {
        return rb;
    }

    public int getLimit() {
        return limit;
    }

    public boolean makeChanceResult() {
        return prop == 1.0 || Math.random() < prop;
    }

    private Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft) {
        Point mylt;
        Point myrb;
        if (facingLeft) {
            mylt = new Point(lt.x + posFrom.x, lt.y + posFrom.y);
            myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        } else {
            myrb = new Point(lt.x * -1 + posFrom.x, rb.y + posFrom.y);
            mylt = new Point(rb.x * -1 + posFrom.x, lt.y + posFrom.y);
        }
        Rectangle bounds = new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
        return bounds;
    }

   private List<Player> getPlayersInRange(MapleMonster monster, Player player) {
        List<Player> players = new ArrayList<>();
        players.add(player);
        return monster.getMap().getPlayersInRange(calculateBoundingBox(monster.getPosition(), monster.isFacingLeft()), players);
    }

    private List<FieldObject> getObjectsInRange(MapleMonster monster, FieldObjectType objectType) {
        List<FieldObjectType> objectTypes = new ArrayList<>();
        objectTypes.add(objectType);
        return monster.getMap().getMapObjectsInBox(calculateBoundingBox(monster.getPosition(), monster.isFacingLeft()), objectTypes);
    }
}