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
package handling.channel.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import constants.SkillConstants.Assassin;
import constants.SkillConstants.Bowmaster;
import client.player.Player;
import client.player.PlayerJob;
import client.player.buffs.BuffStat;
import client.player.skills.PlayerSkill;
import client.player.skills.PlayerSkillFactory;
import client.player.violation.AutobanManager;
import client.player.violation.CheatingOffense;
import constants.SkillConstants.Bandit;
import constants.SkillConstants.Bishop;
import constants.SkillConstants.Brawler;
import server.life.status.MonsterStatus;
import server.life.status.MonsterStatusEffect;
import constants.SkillConstants.Buccaneer;
import constants.SkillConstants.ChiefBandit;
import constants.SkillConstants.Cleric;
import constants.SkillConstants.Corsair;
import constants.SkillConstants.DragonKnight;
import constants.SkillConstants.FPArchMage;
import constants.SkillConstants.Gunslinger;
import constants.SkillConstants.Hermit;
import constants.SkillConstants.Hero;
import constants.SkillConstants.ILArchMage;
import constants.SkillConstants.Marauder;
import constants.SkillConstants.Marksman;
import constants.SkillConstants.NightLord;
import constants.SkillConstants.Paladin;
import constants.SkillConstants.Rogue;
import constants.SkillConstants.Shadower;
import constants.SkillConstants.Sniper;
import constants.SkillConstants.SuperGm;
import constants.SkillConstants.WhiteKnight;
import handling.mina.PacketReader;
import java.util.HashMap;
import packet.creators.PacketCreator;
import server.MapleStatEffect;
import tools.TimerTools.ItemTimer;
import server.life.Element;
import server.life.ElementalEffectiveness;
import server.life.MapleMonster;
import server.maps.Field;
import server.maps.FieldItem;
import server.maps.object.FieldObject;
import server.maps.object.FieldObjectType;
import tools.Randomizer;

public abstract class DamageParse {

    public static void applyAttack(AttackInfo attack, Player p, int maxDamagePerMonster, int attackCount) {
      
        p.getCheatTracker().resetHPRegen();
        p.getCheatTracker().checkAttack(attack.skill);

        if (!p.isAlive()) {
            p.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        
        PlayerSkill skill = null;
        MapleStatEffect effect = null;
        if (attack.skill != 0) {
            skill = PlayerSkillFactory.getSkill(attack.skill);
            effect = attack.getAttackEffect(p, skill);
            if (effect == null) {
                p.announce(PacketCreator.EnableActions());
                return;
            }
            int mobCount = effect.getMobCount();
            if (attack.numAttacked > mobCount) {
                AutobanManager.getInstance().autoban(p.getClient(), "Skill: " + attack.skill + "; Count: " + attack.numAttacked + " Max: " + effect.getMobCount());
                return;
            } 
            if (PlayerSkillFactory.getSkill(attack.skill).isGMSkill() && !p.isGameMaster()) {
                AutobanManager.getInstance().autoban(p.getClient(), "Used GMSkills");
                return;
            }
        }
        
        if (attackCount > attack.numDamage) {
	    if (attack.skill != ChiefBandit.MesoExplosion) {
                p.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT);
		return;
	    }
	}
            
        if (attackCount != attack.numDamage && attack.skill != ChiefBandit.MesoExplosion && attack.numDamage != attackCount * 2) {
            p.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT, attack.numDamage + "/" + attackCount);
        }
        
        int totDamage = 0;
        final Field map = p.getMap();
        
        if (attack.skill == ChiefBandit.MesoExplosion) { 
            for (Integer oned : attack.allDamage.keySet()) {
                final FieldObject mapObject = map.getMapObject(oned.intValue());
                
                if (mapObject != null && mapObject.getType() == FieldObjectType.ITEM) {
                    final FieldItem mapItem = (FieldItem) mapObject;
                    synchronized (mapItem) {
                        if (mapItem.getMeso() > 0) {
                            if (mapItem.isPickedUp()) {
                                return;
                            }
                            map.removeMapObject(mapItem);
                            map.broadcastMessage(PacketCreator.RemoveItemFromMap(mapItem.getObjectId(), 4, 0), mapItem.getPosition());
                            mapItem.setPickedUp(true);
                        } else {
                            p.getCheatTracker().registerOffense(CheatingOffense.ETC_EXPLOSION);
                            return;
                        }
                    }
                } else if (mapObject != null && mapObject.getType() != FieldObjectType.MONSTER) {
                    p.getCheatTracker().registerOffense(CheatingOffense.EXPLODING_NONEXISTANT);
                    return;
                }
            }
        }
       
        for (Integer oned : attack.allDamage.keySet()) {
            final MapleMonster monster = map.getMonsterByOid(oned);

            if (monster != null) {

                double distance = p.getPosition().distanceSq(monster.getPosition());
                double distanceToDetect = 200000.0;
                
                if (attack.ranged) {
                    distanceToDetect += 400000;
                }
                
                if (attack.magic) {
                    distanceToDetect += 200000;
                }

                if (attack.skill == Bishop.Genesis || attack.skill == ILArchMage.Blizzard || attack.skill == FPArchMage.Metoer_Shower) {
                    distanceToDetect += 275000;
                }

                if (attack.skill == Hero.Brandish || attack.skill == DragonKnight.Spear_Crusher || attack.skill == DragonKnight.Pole_Arm_Crusher){
                    distanceToDetect += 40000;
                }
                
                if (attack.skill == DragonKnight.DragonRoar || attack.skill == SuperGm.SuperDragonRoar) {
                    distanceToDetect += 250000;
                }

                if (attack.skill == Shadower.BoomerangStep) {
                    distanceToDetect += 60000;
                }

                if (distance > distanceToDetect) {
                    p.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER, Double.toString(Math.sqrt(distance)));
                }
                
                int totDamageToOneMonster = 0;
                List<Integer> onedList = attack.allDamage.get(oned);
                
                for (Integer eachd : onedList) {
                    if (eachd < 0) {
                        eachd += Integer.MAX_VALUE;
                    }
                    totDamageToOneMonster += eachd;
                }
                totDamage += totDamageToOneMonster;

                p.checkMonsterAggro(monster);
                
                if (attack.skill == Cleric.Heal && !monster.getUndead()) {
                    p.getCheatTracker().registerOffense(CheatingOffense.HEAL_ATTACKING_UNDEAD);
                    return;
                }
                
                int dmgCheck = p.getCheatTracker().checkDamage(totDamageToOneMonster);
                
                if (totDamageToOneMonster > attack.numDamage + 1) {
                    if (dmgCheck > 5 && totDamageToOneMonster < 99999 && monster.getId() < 9500317 && monster.getId() > 9600066) {
                       p.getCheatTracker().registerOffense(CheatingOffense.SAME_DAMAGE, dmgCheck + " times: " + totDamageToOneMonster);
                    }
                }
                if (totDamageToOneMonster >= 2499999 && attack.skill != Marksman.PiercingArrow && attack.skill != ChiefBandit.MesoExplosion) {
                    AutobanManager.getInstance().autoban(p.getClient(), p.getName() + " dealt " + totDamageToOneMonster + " to monster " + monster.getId() + ".");
                }
                
                if (totDamageToOneMonster > 69999 && attack.skill == 0 && p.getLevel() < 200) {
                    AutobanManager.getInstance().autoban(p.getClient(), p.getName() + " dealt " + totDamageToOneMonster + " to monster " + monster.getId() + " at level " + p.getLevel() + " with only a basic attack...");
                }
                 
                checkHighDamage(p, monster, attack, skill, effect, totDamageToOneMonster, maxDamagePerMonster);
                
                if (p.getBuffedValue(BuffStat.PICKPOCKET) != null) {
                    switch (attack.skill) {
                        case 0:
                        case Rogue.DoubleStab:
                        case Bandit.SavageBlow:
                        case ChiefBandit.Assaulter:
                        case ChiefBandit.BandOfThieves:
                        case ChiefBandit.Chakra:
                        case Shadower.Taunt:
                        case Shadower.BoomerangStep:
                            final int maxMeso = p.getBuffedValue(BuffStat.PICKPOCKET);
                            final PlayerSkill skillPocket = PlayerSkillFactory.getSkill(ChiefBandit.Pickpocket);
                            final MapleStatEffect s = skillPocket.getEffect(p.getSkillLevel(skillPocket));

                            for (Integer eachd : onedList) {
                                    eachd += Integer.MAX_VALUE;
                                    
                                if (skillPocket.getEffect(p.getSkillLevel(skillPocket)).makeChanceResult()) {
                                    final Integer eachdf;
                                    if (eachd < 0) {
                                        eachdf = eachd + Integer.MAX_VALUE;
                                    } else {
                                        eachdf = eachd;
                                    }
                                    ItemTimer.getInstance().schedule(() -> {
                                        p.getMap().spawnMesoDrop(Math.min((int) Math.max(((double) eachdf / (double) 20000) * (double) maxMeso, (double) 1), maxMeso), new Point((int) (monster.getPosition().getX() + Randomizer.nextInt(100) - 50), (int) (monster.getPosition().getY())), monster, p, true, (byte) 0);
                                    }, 100);
                                }
                            }
                        break;
                    }
                }

            switch (attack.skill) {
                case Paladin.HeavensHammer:
                    if (attack.isHH) {
                        int HHDmg = (p.getStat().calculateMaxBaseDamage(p.getStat().getTotalWatk())  * (PlayerSkillFactory.getSkill(Paladin.HeavensHammer).getEffect(p.getSkillLevel(PlayerSkillFactory.getSkill(Paladin.HeavensHammer))).getDamage() / 100));
                        map.damageMonster(p, monster, (int) (Math.floor(Math.random() * (HHDmg / 5) + HHDmg * .8)));
                    } else {
                        map.damageMonster(p, monster, totDamageToOneMonster);
                    }
                    break;
                case Marksman.Snipe: 
                    totDamageToOneMonster = (int) (195000 + Math.random() * 4999);
                    break;    
                case NightLord.Taunt:
                    monster.setTaunted(true);
                    monster.setTaunter(p);
                    break;
                case Assassin.Drain: 
                case Marauder.EnergyDrain: 
                    int gainHp = (int) ((double) totDamageToOneMonster * (double) PlayerSkillFactory.getSkill(attack.skill).getEffect(p.getSkillLevel(PlayerSkillFactory.getSkill(attack.skill))).getX() / 100.0);
                    gainHp = Math.min(monster.getMaxHp(), Math.min(gainHp, p.getStat().getMaxHp() / 2));
                    p.getStat().addHP(gainHp);
                    break;
                default:
                    if (attack.skill != 0) {
                        if (effect != null) { 
                            if (effect.getFixDamage() != -1) {
                                if (totDamageToOneMonster != effect.getFixDamage() && totDamageToOneMonster != 0) {
                                    AutobanManager.getInstance().autoban(p.getClient(), String.valueOf(totDamageToOneMonster) + " damage");
                                }
                            }
                        }
                    }
                    if (totDamageToOneMonster > 0 && monster.isAlive()) {
                        if (p.getBuffedValue(BuffStat.BLIND) != null) {
                            if (PlayerSkillFactory.getSkill(Marksman.Blind).getEffect(p.getSkillLevel(PlayerSkillFactory.getSkill(Marksman.Blind))).makeChanceResult()) {
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.ACC, PlayerSkillFactory.getSkill(Marksman.Blind).getEffect(p.getSkillLevel(PlayerSkillFactory.getSkill(Marksman.Blind))).getX()), PlayerSkillFactory.getSkill(Marksman.Blind), false);
                                monster.applyStatus(p, monsterStatusEffect, false, PlayerSkillFactory.getSkill(Marksman.Blind).getEffect(p.getSkillLevel(PlayerSkillFactory.getSkill(Marksman.Blind))).getY() * 1000);

                            }
                        }
                        if (p.getBuffedValue(BuffStat.HAMSTRING) != null) {
                            if (PlayerSkillFactory.getSkill(Bowmaster.Hamstring).getEffect(p.getSkillLevel(PlayerSkillFactory.getSkill(Bowmaster.Hamstring))).makeChanceResult()) {
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, PlayerSkillFactory.getSkill(Bowmaster.Hamstring).getEffect(p.getSkillLevel(PlayerSkillFactory.getSkill(Bowmaster.Hamstring))).getX()), PlayerSkillFactory.getSkill(Bowmaster.Hamstring), false);
                                monster.applyStatus(p, monsterStatusEffect, false, PlayerSkillFactory.getSkill(Bowmaster.Hamstring).getEffect(p.getSkillLevel(PlayerSkillFactory.getSkill(Bowmaster.Hamstring))).getY() * 1000);
                            }
                        }
                        if (p.getJob().isA(PlayerJob.WHITEKNIGHT)) {
                            int[] charges = {WhiteKnight.SwordIceCharge, WhiteKnight.BwIceCharge};
                            for (int charge : charges) {
                                if (p.isBuffFrom(BuffStat.WK_CHARGE, PlayerSkillFactory.getSkill(charge))) {
                                    final ElementalEffectiveness iceEffectiveness = monster.getEffectiveness(Element.ICE);
                                    if (iceEffectiveness == ElementalEffectiveness.NORMAL || iceEffectiveness == ElementalEffectiveness.WEAK) {
                                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.FREEZE, 1), PlayerSkillFactory.getSkill(charge), false);
                                        monster.applyStatus(p, monsterStatusEffect, false, PlayerSkillFactory.getSkill(charge).getEffect(p.getSkillLevel(PlayerSkillFactory.getSkill(charge))).getY() * 2000);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    break;
                }
                if (p.getSkillLevel(PlayerSkillFactory.getSkill(NightLord.VenomousStar)) > 0) {
                    MapleStatEffect venomEffect = PlayerSkillFactory.getSkill(NightLord.VenomousStar).getEffect(p.getSkillLevel(PlayerSkillFactory.getSkill(NightLord.VenomousStar)));
                    for (int i = 0; i < attackCount; i++) {
                        if (venomEffect.makeChanceResult()) {
                            if (monster.getVenomMulti() < 3) {
                                monster.setVenomMulti((monster.getVenomMulti() + 1));
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), PlayerSkillFactory.getSkill(NightLord.VenomousStar), false);
                                monster.applyStatus(p, monsterStatusEffect, false, venomEffect.getDuration(), true);
                            }
                        }
                    }
                } else if (p.getSkillLevel(PlayerSkillFactory.getSkill(Shadower.VenomousStab)) > 0) {
                    MapleStatEffect venomEffect = PlayerSkillFactory.getSkill(Shadower.VenomousStab).getEffect(p.getSkillLevel(PlayerSkillFactory.getSkill(Shadower.VenomousStab)));
                    for (int i = 0; i < attackCount; i++) {
                        if (venomEffect.makeChanceResult()) {
                            if (monster.getVenomMulti() < 3) {
                                monster.setVenomMulti((monster.getVenomMulti() + 1));
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), PlayerSkillFactory.getSkill(Shadower.VenomousStab), false);
                                monster.applyStatus(p, monsterStatusEffect, false, venomEffect.getDuration(), true);
                            }
                        }
                    }
                }
                if (totDamageToOneMonster > 0 && effect != null && effect.getMonsterStati().size() > 0) {
                    if (effect.makeChanceResult()) {
                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(effect.getMonsterStati(), skill, false);
                        monster.applyStatus(p, monsterStatusEffect, effect.isPoison(), effect.getDuration());
                    }
                }
                if (!attack.isHH) {
                    map.damageMonster(p, monster, totDamageToOneMonster);
                }
            }
        }
        if (totDamage > 1) {
            p.getCheatTracker().setAttacksWithoutHit(p.getCheatTracker().getAttacksWithoutHit() + 1);
            final int offenseLimit;
            switch (attack.skill) {
                case Bowmaster.Hurricane:
                case Corsair.RapidFire:
                    offenseLimit = 100;
                    break;
                default:
                    offenseLimit = 500;
                    break;
            }
            if (p.getCheatTracker().getAttacksWithoutHit() > offenseLimit) {
                p.getCheatTracker().registerOffense(CheatingOffense.ATTACK_WITHOUT_GETTING_HIT, Integer.toString(p.getCheatTracker().getAttacksWithoutHit()));
            }
        }
    }

    public static void checkHighDamage(Player p, MapleMonster monster, AttackInfo attack, PlayerSkill theSkill, MapleStatEffect attackEffect, int damageToMonster, int maximumDamageToMonster) {
        if (!p.isGameMaster()) {
            int elementalMaxDamagePerMonster;
            int multiplyer = p.getJob().isA(PlayerJob.PIRATE) ? 40 : 4;
            Element element = Element.NEUTRAL;
            if (theSkill != null) {
                element = theSkill.getElement();
                int skillId = theSkill.getId();
                switch (skillId) {
                    case 3221007:  
                        maximumDamageToMonster = 99999;
                        break;
                    case 4221001:
                        maximumDamageToMonster = 400000;
                        break;
                    default:
                        break;
                }
            }
            if (p.getBuffedValue(BuffStat.WK_CHARGE) != null) {
                int chargeSkillId = p.getBuffSource(BuffStat.WK_CHARGE);
                switch (chargeSkillId) {
                    case WhiteKnight.SwordFireCharge:
                    case WhiteKnight.BwFireCharge:
                        element = Element.FIRE;
                        break;
                    case WhiteKnight.SwordIceCharge:
                    case WhiteKnight.BwIceCharge:
                        element = Element.ICE;
                        break;
                    case WhiteKnight.SwordLitCharge:
                    case WhiteKnight.BwLitCharge:
                        element = Element.LIGHTING;
                        break;
                    case Paladin.SwordHolyCharge:
                    case Paladin.BwHolyCharge:
                        element = Element.HOLY;
                        break;
                }
                PlayerSkill chargeSkill = PlayerSkillFactory.getSkill(chargeSkillId);
                maximumDamageToMonster *= chargeSkill.getEffect(p.getSkillLevel(chargeSkill)).getDamage() / 100.0;
            }
            if (element != Element.NEUTRAL) {
                double elementalEffect;
                if (attack.skill == Sniper.Blizzard || attack.skill == 3111003) {  
                    elementalEffect = attackEffect.getX() / 200.0;
                } else {
                    elementalEffect = 0.5;
                }
                switch (monster.getEffectiveness(element)) {
                    case IMMUNE:
                        elementalMaxDamagePerMonster = 1;
                        break;
                    case NORMAL:
                        elementalMaxDamagePerMonster = maximumDamageToMonster;
                        break;
                    case WEAK:
                        elementalMaxDamagePerMonster = (int) (maximumDamageToMonster * (1.0 + elementalEffect));
                        break;
                    case STRONG:
                        elementalMaxDamagePerMonster = (int) (maximumDamageToMonster * (1.0 - elementalEffect));
                        break;
                    default:
                        throw new RuntimeException("Unknown enum constant");
                }
            }  else {
                elementalMaxDamagePerMonster = maximumDamageToMonster;
            }
            if (damageToMonster > elementalMaxDamagePerMonster * multiplyer) {
                p.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE);
                if (damageToMonster > elementalMaxDamagePerMonster * multiplyer * 1.5) { 
                   CheatingOffense.DAMAGE_HACK.cheatingSuspicious(p, "DMG: " + damageToMonster + " MobID: " + (monster != null ? monster.getId() : "null") + " MobLevel: " + (monster != null ? monster.getLevel() : "null") + "  Map: " + p.getMap().getMapName() + " (" + p.getMapId() + ")");              
                }
            }
        }
    }

    public static final AttackInfo parseDamage(PacketReader packet, Player p, boolean ranged, boolean magic) {   
        final AttackInfo ret = new AttackInfo();

        packet.readByte();
        ret.numAttackedAndDamage = packet.readByte();
        ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF;
        ret.numDamage = ret.numAttackedAndDamage & 0xF;
        ret.allDamage = new HashMap<>();
        ret.skill = packet.readInt();
        ret.ranged = ranged;
        ret.magic = magic;
        
        switch (ret.skill) {
            case FPArchMage.BigBang:
            case ILArchMage.BigBang:
            case Bishop.BigBang:
            case Brawler.CorkscrewBlow:
            case Gunslinger.Grenade:
                ret.charge = packet.readInt();
                break;
            default:
                ret.charge = 0;
                break;
        }
       
        if (ret.skill == Paladin.HeavensHammer) {
            ret.isHH = true;
        }
        packet.readByte(); 
        ret.stance = packet.readByte();

        if (ret.skill == ChiefBandit.MesoExplosion) {
            return parseMesoExplosion(packet, ret);
        }

        if (ranged) {
            packet.readByte();
            ret.speed = packet.readByte();
            packet.readByte();
            ret.direction = packet.readByte(); 
            packet.skip(7);
            switch (ret.skill) {
                case Bowmaster.Hurricane:
                case Marksman.PiercingArrow:
                case Corsair.RapidFire:
                    packet.skip(4);
                    break;
                default:
                    break;
            }
        } else {
            packet.readByte();
            ret.speed = packet.readByte();
            ret.attackTime = packet.readInt();
            long lastAttackTime = System.currentTimeMillis();
            p.setLastHitTime(lastAttackTime);
            if (ret.skill == 0 && p.getJob().getId() != PlayerJob.CORSAIR.getId() && p.getJob().getId() != PlayerJob.BOWMASTER.getId()) {
                if ((ret.attackTime - p.getLastAttackTime() < 110) && (ret.attackTime - p.getLastAttackTime() >= 0)) {
                    long math = ret.attackTime - p.getLastAttackTime();
                    CheatingOffense.FASTATTACK.cheatingSuspicious(p, "Attacked with a speed of " + math + " and it doesn't seem right.\nMapID of character was " + p.getMapId() + ".\n");
                } else if (ret.attackTime-p.getLastAttackTime() < 0 ) {
                    p.dropMessage("You seem to have a negative attack time... that's not normal! You'll need to post on forums about this and possibly check your Antivirus.");
                }
                p.setLastAttackTime(ret.attackTime);
            }
        } 
        
        int calcDmgMax = 0;
        if (magic && ret.skill != 0) {
            calcDmgMax = (p.getStat().getTotalMagic() * p.getStat().getTotalMagic() / 1000 + p.getStat().getTotalMagic()) / 30 + p.getStat().getTotalInt() / 200;
        } else if(ret.skill == 4001344) {
            calcDmgMax = (p.getStat().getTotalLuk() * 5) * p.getStat().getTotalWatk() / 100;
        } else if(ret.skill == DragonKnight.DragonRoar) {
            calcDmgMax = (p.getStat().getTotalStr() * 4 + p.getStat().getTotalDex()) * p.getStat().getTotalWatk() / 100;
        } else if(ret.skill == Shadower.VenomousStab) {
            calcDmgMax = (int) (18.5 * (p.getStat().getTotalStr() + p.getStat().getTotalLuk()) + p.getStat().getTotalDex() * 2) / 100 * p.getStat().calculateMaxBaseDamage(p.getStat().getTotalWatk());
        } else {
            calcDmgMax = p.getStat().calculateMaxBaseDamage(p.getStat().getTotalWatk());
        }
        
        boolean canCrit = false;
        if (p.getJob().isA((PlayerJob.BOWMAN)) || p.getJob().isA(PlayerJob.THIEF) || p.getJob() == PlayerJob.MARAUDER || p.getJob() == PlayerJob.BUCCANEER) {
            canCrit = true;
        } 
        
	if (p.getBuffEffect(BuffStat.SHARP_EYES) != null) {
            canCrit = true;
            calcDmgMax *= 1.4;
        }
        
        boolean shadowPartner = false;
	if (p.getBuffEffect(BuffStat.SHADOWPARTNER) != null) {
            shadowPartner = true;
        }
        
        if (ret.skill != 0) {
            int fixed = ret.getAttackEffect(p, PlayerSkillFactory.getSkill(ret.skill)).getFixDamage();
            if (fixed > 0) {
               calcDmgMax = fixed;
            }
        }
        
        for (int i = 0; i < ret.numAttacked; i++) {
            int objectId=  packet.readInt();
            packet.skip(14);
            List<Integer> allDamageNumbers = new ArrayList<>();
            
            MapleMonster monster = p.getMap().getMonsterByOid(objectId);
            
            if (p.getBuffEffect(BuffStat.WK_CHARGE) != null) {
                int sourceID = p.getBuffSource(BuffStat.WK_CHARGE);
                int level = p.getBuffedValue(BuffStat.WK_CHARGE);
                if (monster != null) {
                    switch (sourceID) {
                        case WhiteKnight.BwFireCharge:
                        case WhiteKnight.SwordFireCharge:
                            if(monster.getStats().getEffectiveness(Element.FIRE) == ElementalEffectiveness.WEAK) {
                                calcDmgMax *= 1.05 + level * 0.015;
                            }   break;
                        case WhiteKnight.BwIceCharge:
                        case WhiteKnight.SwordIceCharge:
                            if(monster.getStats().getEffectiveness(Element.ICE) == ElementalEffectiveness.WEAK) {
                                calcDmgMax *= 1.05 + level * 0.015;
                            }   break;
                        case WhiteKnight.BwLitCharge:
                        case WhiteKnight.SwordLitCharge:
                            if(monster.getStats().getEffectiveness(Element.LIGHTING) == ElementalEffectiveness.WEAK) {
                                calcDmgMax *= 1.05 + level * 0.015;
                            }   break;
                        case Paladin.BwHolyCharge:
                        case Paladin.SwordHolyCharge:
                            if(monster.getStats().getEffectiveness(Element.HOLY) == ElementalEffectiveness.WEAK) {
                                calcDmgMax *= 1.2 + level * 0.015;
                            }   break;
                        default:
                            break;
                    }
                } else {
                    calcDmgMax *= 1.5;
                }
            }
            
            if (ret.skill != 0) {
                PlayerSkill skill = PlayerSkillFactory.getSkill(ret.skill);
                if(skill.getElement() != Element.NEUTRAL && p.getBuffedValue(BuffStat.ELEMENTAL_RESET) == null) {
                    if (monster != null) {
                        ElementalEffectiveness eff = monster.getEffectiveness(skill.getElement());
                        if(eff == ElementalEffectiveness.WEAK) {
                            calcDmgMax *= 1.5;
                        } else if(eff == ElementalEffectiveness.STRONG) {}
                    } else {
                        calcDmgMax *= 1.5;
                    }
                }
                if (ret.skill == Hermit.ShadowWeb) {
                    if (monster != null) {
                       calcDmgMax = monster.getHp() / (50 - p.getSkillLevel(skill));
                    }
                }
            }
            for (int j = 0; j < ret.numDamage; j++) {
                int damage = packet.readInt();
                int hitDmgMax = calcDmgMax;
                if (ret.skill == Buccaneer.Barrage) {
                    if (j > 3) {
                        hitDmgMax *= Math.pow(2, (j - 3));
                    }
                }
		if (shadowPartner) {
	            if (j >= ret.numDamage / 2) {
			hitDmgMax *= 0.5;
                    }
		}
                if (ret.skill == Marksman.Snipe) {
                    damage = 195000 + Randomizer.nextInt(5000);
                    hitDmgMax = 200000;
		}
                
                int maxWithCrit = hitDmgMax;
		if (canCrit)  {
                    maxWithCrit *= 2;
                }
                if (damage > maxWithCrit * 1.5 && !p.isGameMaster()) {
                    CheatingOffense.DAMAGE_HACK.cheatingSuspicious(p, "DMG: " + damage + " MaxDMG: " + maxWithCrit + " SID: " + ret.skill + " MobID: " + (monster != null ? monster.getId() : "null") + " Map: " + p.getMap().getMapName() + " (" + p.getMapId() + ")");
                }
                if (damage > maxWithCrit  * 5 && !p.isGameMaster()) {
                    AutobanManager.getInstance().autoban(p.getClient(), "DMG: " + damage + " MaxDMG: " + maxWithCrit + " SID: " + ret.skill + " MobID: " + (monster != null ? monster.getId() : "null") + " Map: " + p.getMap().getMapName() + " (" + p.getMapId() + ")");
                }
                if (ret.skill == Marksman.Snipe || (canCrit && damage > hitDmgMax)) {
                    damage = -Integer.MAX_VALUE + damage - 1;
                }
                
                checkDamage(p, damage, ret);
                allDamageNumbers.add(damage);
            }
            if (ret.skill != Corsair.RapidFire) {
                packet.skip(4);
            }
            ret.allDamage.put(Integer.valueOf(objectId), allDamageNumbers);
        }
        return ret;
    }
    
    public static void checkDamage(Player p, int damage, AttackInfo ret) {
        if ((damage > p.getStat().calculateWorkingDamageTotal(p.getStat().getTotalWatk()) * 1.3) && ret.skill == 0) {
            switch (p.getJob().getId()) {
                case 311:
                case 312:
                case 321:
                case 322:
                case 410:
                case 411:
                case 412:
                case 500:
                case 512:
                case 520:
                case 521:
                    CheatingOffense.DAMAGE_HACK.cheatingSuspicious(p, "Player with damage " + damage + " with rangeattack " 
                    + p.getStat().calculateWorkingDamageTotal(p.getStat().getTotalWatk()) + " using a basic attack. Player was level " + p.getLevel() 
                    + ". With the JobID " + p.getJob().getId() + "\n");
                break;
            }
        } else if ((damage > p.getStat().calculateWorkingDamageTotal(p.getStat().getTotalWatk())) && ret.skill == 0 && p.getJob().getId() < 300) {
            CheatingOffense.DAMAGE_HACK.cheatingSuspicious(p, "Player with damage  " + damage + " with rangeattack " 
            + p.getStat().calculateWorkingDamageTotal(p.getStat().getTotalWatk()) + " using a basic attack. Player was level " + p.getLevel() 
            + ". CWith the JobID " + p.getJob().getId() + "\n");
        }
    }
    
    public static AttackInfo parseMesoExplosion(PacketReader r, AttackInfo ret) {
        if (ret.numAttackedAndDamage == 0) {
            r.skip(10);
            int bullets = r.readByte();
            for (int j = 0; j < bullets; j++) {
                int mesoid = r.readInt();
                r.skip(1);
                ret.allDamage.put(Integer.valueOf(mesoid), null);
            }
            return ret;
        } else {
            r.skip(6);
        }
        for (int i = 0; i < ret.numAttacked + 1; i++) {
            int oid = r.readInt();
            if (i < ret.numAttacked) {
                r.skip(12);
                int bullets = r.readByte();
                List<Integer> allDamageNumbers = new ArrayList<>();
                for (int j = 0; j < bullets; j++) {
                    int damage = r.readInt();
                    allDamageNumbers.add(Integer.valueOf(damage));
                }
                ret.allDamage.put(Integer.valueOf(oid), allDamageNumbers);
                r.skip(4);
            } else {
                int bullets = r.readByte();
                for (int j = 0; j < bullets; j++) {
                    int mesoid = r.readInt();
                    r.skip(1);
                    ret.allDamage.put(Integer.valueOf(mesoid), null);
                }
            }
        }
        return ret;
    }
}
