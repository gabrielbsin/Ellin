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

import server.life.npc.MapleNPC;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import provider.wzxml.MapleDataType;
import server.life.components.BanishInfo;
import server.life.components.LoseItem;
import server.life.components.SelfDestruction;
import server.life.npc.MapleNPCStats;
import tools.Pair;
import tools.StringUtil;

public class MapleLifeFactory {

    private static final MapleDataProvider data = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Mob"));
    private static final MapleDataProvider dataNPC = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Npc"));
    private static final MapleDataProvider stringDataWZ =  MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String"));
    private static final MapleDataProvider etcDataWZ = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Etc"));
    private static final MapleData mobStringData = stringDataWZ.getData("Mob.img");
    private static final MapleData npcStringData = stringDataWZ.getData("Npc.img");
    private static final MapleData npcLocationData = etcDataWZ.getData("NpcLocation.img");
    private static final Map<Integer, MapleMonsterStats> monsterStats = new HashMap<>();
    private static final Map<Integer, String> npcNames = new HashMap<>();
    
    public static AbstractLoadedMapleLife getLife(int id, String type) {
	if (type.equalsIgnoreCase("n")) {
	    return getNPC(id);
	} else if (type.equalsIgnoreCase("m")) {
	    return getMonster(id);
	} else {
	    System.err.println("Unknown Life type: " + type + "");
	    return null;
	}
    }
    
    public static MapleMonster getMonster(int mid) {
        MapleMonsterStats stats = getMonsterStats(mid);
        if (stats == null) {
            return null;
        }
        return new MapleMonster(mid, stats);
    }

    public static MapleMonsterStats getMonsterStats(int mid) {
        try {
            MapleMonsterStats stats = monsterStats.get(Integer.valueOf(mid));

            if (stats == null) {
                MapleData monsterData = data.getData(StringUtil.getLeftPaddedStr(Integer.toString(mid) + ".img", '0', 11));
                if (monsterData == null) {
                    return null;
                }
                MapleData monsterInfoData = monsterData.getChildByPath("info");
                stats = new MapleMonsterStats();
                
                stats.setHp(MapleDataTool.getIntConvert("maxHP", monsterInfoData));
                stats.setMp(MapleDataTool.getIntConvert("maxMP", monsterInfoData, 0));
                stats.setFriendly(MapleDataTool.getIntConvert("damagedByMob", monsterInfoData, 0) == 1);
                stats.setPADamage(MapleDataTool.getIntConvert("PADamage", monsterInfoData));
                stats.setPDDamage(MapleDataTool.getIntConvert("PDDamage", monsterInfoData));
                stats.setMADamage(MapleDataTool.getIntConvert("MADamage", monsterInfoData));
                stats.setMDDamage(MapleDataTool.getIntConvert("MDDamage", monsterInfoData));
                stats.setExp(MapleDataTool.getIntConvert("exp", monsterInfoData, 0));
                stats.setLevel(MapleDataTool.getIntConvert("level", monsterInfoData));
                stats.setRemoveAfter(MapleDataTool.getIntConvert("removeAfter", monsterInfoData, 0));
                stats.setBoss(MapleDataTool.getIntConvert("boss", monsterInfoData, 0) > 0);
                stats.setPublicReward(MapleDataTool.getIntConvert("publicReward", monsterInfoData, 0) > 0);
                stats.setUndead(MapleDataTool.getIntConvert("undead", monsterInfoData, 0) > 0);
                stats.setName(MapleDataTool.getString(mid + "/name", mobStringData, "MISSINGNO"));
                stats.setBuffToGive(MapleDataTool.getIntConvert("buff", monsterInfoData, -1));
                stats.setCp(MapleDataTool.getIntConvert("getCP", monsterInfoData, 0));
                stats.setExplosive(MapleDataTool.getIntConvert("explosiveReward", monsterInfoData, 0) > 0);
                stats.setAccuracy(MapleDataTool.getIntConvert("acc", monsterInfoData, 0));
                MapleData firstAttackData = monsterInfoData.getChildByPath("firstAttack");
                int firstAttack = 0;
                if (firstAttackData != null) {
                    if (firstAttackData.getType() == MapleDataType.FLOAT) {
                        firstAttack = Math.round(MapleDataTool.getFloat(firstAttackData));
                    } else {
                        firstAttack = MapleDataTool.getInt(firstAttackData);
                    }
                }
                stats.setFirstAttack(firstAttack > 0);
                stats.setDropPeriod(MapleDataTool.getIntConvert("dropItemPeriod", monsterInfoData, 0) * 10000);
                if (stats.isBoss() || mid == 8810018) {
                    if (monsterInfoData.getChildByPath("hpTagColor") == null || monsterInfoData.getChildByPath("hpTagBgcolor") == null) {
                        stats.setTagColor(0);
                        stats.setTagBgColor(0);
                    } else {
                        stats.setTagColor(MapleDataTool.getIntConvert("hpTagColor", monsterInfoData));
                        stats.setTagBgColor(MapleDataTool.getIntConvert("hpTagBgcolor", monsterInfoData));
                    }
                }

                for (MapleData idata : monsterData) {
                    if (!idata.getName().equals("info")) {
                        int delay = 0;
                        delay = idata.getChildren().stream().map((pic) -> MapleDataTool.getIntConvert("delay", pic, 0)).reduce(delay, Integer::sum);
                        stats.setAnimationTime(idata.getName(), delay);
                    }
                }

                MapleData special = monsterInfoData.getChildByPath("loseItem");
                if (special != null) {
                    for (MapleData liData : special.getChildren()) {
                        System.out.println("set loseItem");
                        stats.addLoseItem(new LoseItem(MapleDataTool.getInt(liData.getChildByPath("id")),
                        (byte) MapleDataTool.getInt(liData.getChildByPath("prop")),
                        (byte) MapleDataTool.getInt(liData.getChildByPath("x"))));
                    }
                }

                special = monsterInfoData.getChildByPath("selfDestruction");
                if (special != null) {
                    stats.setSelfDestruction(new SelfDestruction((byte) MapleDataTool.getInt(special.getChildByPath("action")),
                    MapleDataTool.getIntConvert("removeAfter", special, -1),
                    MapleDataTool.getIntConvert("hp", special, -1)));
                }

                MapleData banishData = monsterInfoData.getChildByPath("ban");
                if (banishData != null) {
                    stats.setBanishInfo(new BanishInfo(MapleDataTool.getString("banMsg", banishData), MapleDataTool.getInt("banMap/0/field", banishData, -1), MapleDataTool.getString("banMap/0/portal", banishData, "sp")));
                }

                final MapleData reviveInfo = monsterInfoData.getChildByPath("revive");
                if (reviveInfo != null) {
                    List<Integer> revives = new LinkedList<>();
                    for (MapleData data_ : reviveInfo) {
                        revives.add(MapleDataTool.getInt(data_));
                    }
                    stats.setRevives(revives);
                }

                decodeElementalString(stats, MapleDataTool.getString("elemAttr", monsterInfoData, ""));

                final int link = MapleDataTool.getIntConvert("link", monsterInfoData, 0);
                if (link != 0) { 
                    monsterData = data.getData(StringUtil.getLeftPaddedStr(link + ".img", '0', 11));
                    monsterInfoData = monsterData.getChildByPath("info");
                }


                final MapleData monsterSkillData = monsterInfoData.getChildByPath("skill");
                if (monsterSkillData != null) {
                    int i = 0;
                    List<Pair<Integer, Integer>> skills = new ArrayList<>();
                    while (monsterSkillData.getChildByPath(Integer.toString(i)) != null) {
                        skills.add(new Pair<>(Integer.valueOf(MapleDataTool.getInt(i + "/skill", monsterSkillData, 0)), Integer.valueOf(MapleDataTool.getInt(i + "/level", monsterSkillData, 0))));
                        i++;
                    }
                    stats.setSkills(skills);
                }

                monsterStats.put(Integer.valueOf(mid), stats);
            }

            return stats;
        } catch(NullPointerException npe) {
            System.out.println("[SEVERE] MOB " + mid + " failed to load. Issue: " + npe.getMessage() + "\n\n");
            npe.printStackTrace();
            
            return null;
        }
    }

    

    public static final void decodeElementalString(MapleMonsterStats stats, String elemAttr) {
	for (int i = 0; i < elemAttr.length(); i += 2) {
            stats.setEffectiveness(Element.getFromChar(elemAttr.charAt(i)),
            ElementalEffectiveness.getByNumber(Integer.valueOf(String.valueOf(elemAttr.charAt(i + 1)))));
	}
    }
    
    public static MapleNPC getNPC(int nid) {
        MapleNPCStats stats = new MapleNPCStats();

        String name = npcNames.get(nid);
        if (name == null) {
            name = MapleDataTool.getString(nid + "/name", npcStringData, "MISSINGNO");
            npcNames.put(nid, name);
        } 
        
        stats.setName(name);
        
        MapleData npcImgData = dataNPC.getData(StringUtil.getLeftPaddedStr(Integer.toString(nid), '0', 7) + ".img");
        if (npcImgData == null) {
            return null;
        }
        
        MapleData npcInfoData = npcImgData.getChildByPath("info");

        if (npcInfoData != null) {
            int depositCost = MapleDataTool.getIntConvert("trunkPut", npcInfoData, 0);
            if (depositCost != 0) {
                stats.setDepositCost(depositCost);
            }
            int withdrawCost = MapleDataTool.getIntConvert("trunkGet", npcInfoData, 0);
            if (withdrawCost != 0) {
                stats.setWithdrawCost(withdrawCost);
            }
        }
        
        MapleData npcLocationInfoData = npcLocationData.getChildByPath(Integer.toString(nid));
        if (npcLocationInfoData != null) {
            for (MapleData map : npcLocationInfoData.getChildren()) {
                int mapid = MapleDataTool.getInt(map, -1);
                if (mapid != -1) {
                    stats.addMap(mapid);
                }
            }
        }
        return new MapleNPC(nid, stats);
    } 
}
