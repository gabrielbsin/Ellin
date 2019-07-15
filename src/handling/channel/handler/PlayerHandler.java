/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.Client;
import client.player.PlayerKeyBinding;
import client.player.Player;
import client.player.PlayerJob;
import client.player.PlayerStat;
import client.player.buffs.BuffStat;
import client.player.PlayerEffects;
import client.player.PlayerStringUtil;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.types.WeaponType;
import client.player.skills.PlayerSkill;
import client.player.skills.PlayerSkillFactory;
import client.player.skills.PlayerSkillMacro;
import client.player.violation.AutobanManager;
import client.player.violation.CheatingOffense;
import constants.GameConstants;
import constants.ItemConstants;
import constants.SkillConstants;
import constants.SkillConstants.Archer;
import constants.SkillConstants.Assassin;
import constants.SkillConstants.Bishop;
import constants.SkillConstants.Bowmaster;
import constants.SkillConstants.Brawler;
import constants.SkillConstants.ChiefBandit;
import constants.SkillConstants.Corsair;
import constants.SkillConstants.Crusader;
import constants.SkillConstants.DarkKnight;
import constants.SkillConstants.DragonKnight;
import constants.SkillConstants.FPArchMage;
import constants.SkillConstants.FPMage;
import constants.SkillConstants.Gunslinger;
import constants.SkillConstants.Hermit;
import constants.SkillConstants.Hero;
import constants.SkillConstants.Hunter;
import constants.SkillConstants.ILArchMage;
import constants.SkillConstants.Marauder;
import constants.SkillConstants.Marksman;
import constants.SkillConstants.Paladin;
import constants.SkillConstants.Priest;
import constants.SkillConstants.Rogue;
import constants.SkillConstants.WhiteKnight;
import handling.channel.ChannelServer;
import static handling.channel.handler.ChannelHeaders.PlayerHeaders.*;
import static handling.channel.handler.DamageParse.*;
import handling.mina.PacketReader;
import handling.world.service.BroadcastService;
import java.awt.Point;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import packet.creators.EffectPackets;
import packet.creators.MonsterPackets;
import packet.creators.PacketCreator;
import packet.transfer.write.OutPacket;
import server.MapleStatEffect;
import server.itens.InventoryManipulator;
import server.itens.ItemInformationProvider;
import server.life.MapleMonster;
import server.life.MobAttackInfo;
import server.life.MobAttackInfoFactory;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.life.status.MonsterStatus;
import server.life.status.MonsterStatusEffect;
import server.maps.Field;
import server.maps.FieldLimit;
import server.movement.LifeMovementFragment;
import server.maps.portal.Portal;

/**
 *
 * @author GabrielSin
 */
public class PlayerHandler {

    public static final void DropMeso(final PacketReader packet, final Client c) {
        if (c.checkCondition()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        Player p = c.getPlayer();
        long time = packet.readInt();
        if (p.getLastRequestTime() > time || p.getLastRequestTime() == time) { 
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        p.setLastRequestTime(time); 
        
        final int meso = packet.readInt();
        if ((meso < 10 || meso > 50000) || (meso > p.getMeso()) || p.getCheatTracker().Spam(500, 2)) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        p.gainMeso(-meso, false, true);
        p.getMap().spawnMesoDrop(meso, p.getPosition(), p, p, true, (byte) 0);
    }

    public static void ChangeMap(PacketReader packet, Client c) {
        if (c.checkCondition(false, true, true)) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        Player p = c.getPlayer();          
        if (packet.available() != 0) {
            if (p.getCashShop().isOpened()) {                 
                c.disconnect(false, false);               
                return;           
            }
            packet.readByte(); 
            int dest = packet.readInt(); 
            String portalName = packet.readMapleAsciiString();
            Portal portal = p.getMap().getPortal(portalName);

            if (dest != -1 && !p.isAlive()) {
                boolean executeStandardPath = true;
                if (p.getEventInstance() != null) {
                    executeStandardPath = p.getEventInstance().revivePlayer(p);
                }
                if (executeStandardPath) {
                    if (p.getMCPQField() != null) {    
                        p.getMCPQField().onPlayerRespawn(p);
                        return;
                    } 
                    p.cancelAllBuffs();
                    p.getStat().setHp(50);
                    p.updatePartyMemberHP();
                    final Field to = p.getMap().getReturnField();
                    p.changeMap(to, to.getRandomPlayerSpawnpoint());
                }
            } else if (dest != -1 && p.isGameMaster()) {
                final Field to = p.getWarpMap(dest);
                p.changeMap(to, to.getPortal(0));
            } else if (dest != -1 && !p.isGameMaster()) {
                p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Player " + p.getName() + " attempted Map jumping without being a GM");
            } else {
                if (portal != null) {
                    portal.enterPortal(c);
                } else {
                    c.announce(PacketCreator.EnableActions());
                }
            }
        }
    }   
    
    public static void PassiveEnergy(PacketReader packet, Client c) {;
        if (c.checkCondition()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        Player p = c.getPlayer();
        if (p.isActiveBuffedValue(Marauder.EnergyCharge) && PlayerJob.getJobPath(p.getJobId()) == PlayerJob.CLASS_PIRATE) {
            AttackInfo attack = parseDamage(packet, p, false, false);
            p.getMap().broadcastMessage(p, PacketCreator.EnergyChargeAttack(p.getId(), attack.skill, attack.stance, attack.numAttackedAndDamage, attack.allDamage, attack.speed), p.getPosition());
            applyAttack(attack, p, 999999, 1);
        }
    }
    
    public static void MeleeAttack(PacketReader packet, Client c) {
        if (c.checkCondition()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        Player p = c.getPlayer();
        if (p.getInventory(InventoryType.EQUIPPED).getItem((byte) ItemConstants.WEAPON) == null) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to attack without a weapon");
            return; 
        }

        AttackInfo attack = DamageParse.parseDamage(packet, p, false, false);
        int skillId = attack.skill;
        PlayerSkill skill = PlayerSkillFactory.getSkill(skillId);


        if (attack.skill != 0) {
            if (skill.getEffect(p.getSkillLevel(skill)).getCoolDown() > 0 && !p.isGameMaster()) {
                if (p.skillisCooling(attack.skill)) {
                    c.getSession().write(PacketCreator.EnableActions());
                    return;
                }
                c.getSession().write(PacketCreator.SkillCooldown(attack.skill, skill.getEffect(p.getSkillLevel(skill)).getCoolDown()));
                p.addCoolDown(attack.skill, System.currentTimeMillis(), skill.getEffect(p.getSkillLevel(skill)).getCoolDown() * 1000);
            }
        }

	p.getMap().broadcastMessage(p, PacketCreator.CloseRangeAttack(p.getId(), skillId, attack.stance, attack.numAttackedAndDamage, attack.allDamage, attack.speed), false, true);

        int numFinisherOrbs = 0;
        Integer comboBuff = p.getBuffedValue(BuffStat.COMBO);
        if (SkillConstants.isFinisherSkill(attack.skill)) {
            if (comboBuff != null) {
                try {
                    numFinisherOrbs = comboBuff.intValue() - 1;
                } catch (ArrayIndexOutOfBoundsException e) {}
            }
            p.handleOrbconsume();
        } else if (attack.numAttacked > 0) {
            if (comboBuff != null) {
                if (attack.skill != Crusader.Shout) { 
                    p.handleOrbgain();
                }
            } else if ((p.getJob().equals(PlayerJob.BUCCANEER) || p.getJob().equals(PlayerJob.MARAUDER)) && p.getSkillLevel(PlayerSkillFactory.getSkill(Marauder.EnergyCharge)) > 0) {
                for (int i = 0; i < attack.numAttacked; i++) {
                    p.energyChargeGain();
                }
            }
        }
        if (attack.numAttacked > 0 && attack.skill == DragonKnight.Sacrifice) {
            int totDamageToOneMonster = 0;  
            final Iterator<List<Integer>> dmgIt = attack.allDamage.values().iterator();
            if (dmgIt.hasNext()) {
                totDamageToOneMonster = dmgIt.next().get(0).intValue();
            }
            int remainingHP = p.getStat().getHp() - totDamageToOneMonster * attack.getAttackEffect(p).getX() / 100;
            if (remainingHP > 1) {
                p.getStat().setHp(remainingHP);
            } else {
                p.getStat().setHp(1);
            }
            p.getStat().updateSingleStat(PlayerStat.HP, p.getStat().getHp());
        }
        if (attack.numAttacked > 0 && attack.skill == WhiteKnight.ChargeBlow) {
            boolean advchargeProb = false;
            int advchargeLevel = p.getSkillLevel(PlayerSkillFactory.getSkill(1220010));
            if (advchargeLevel > 0) {
                advchargeProb = PlayerSkillFactory.getSkill(1220010).getEffect(advchargeLevel).makeChanceResult();
            }
            if (!advchargeProb) {
                p.cancelEffectFromBuffStat(BuffStat.WK_CHARGE);
            }
        }
        int maxDamage = p.getStat().getCurrentMaxBaseDamage();
        int attackCount = 1;
        if (skillId != 0) {
            MapleStatEffect effect = attack.getAttackEffect(p);
            attackCount = effect.getAttackCount();
            maxDamage *= effect.getDamage() / 100.0;
            maxDamage *= attackCount;
        }
        maxDamage = Math.min(maxDamage, 99999);
        if (skillId == ChiefBandit.MesoExplosion) {
            maxDamage = 700000;
        } else if (numFinisherOrbs > 0) {
            maxDamage *= numFinisherOrbs;
        } else if (comboBuff != null) {
            PlayerSkill combo = PlayerSkillFactory.getSkill(Crusader.ComboAttack);
            maxDamage *= (double) 1.0 + (combo.getEffect(p.getSkillLevel(combo)).getDamage() / 100.0 - 1.0) * (comboBuff.intValue() - 1);
        }
        if (numFinisherOrbs == 0 && SkillConstants.isFinisherSkill(skillId)) {
            return;  
        }
        if (SkillConstants.isFinisherSkill(skillId)) {
            maxDamage = 99999;  
        }
        DamageParse.applyAttack(attack, p, maxDamage, attackCount);
    }

    public static void RangedAttack(PacketReader packet, Client c) {
        if (c.checkCondition()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        Player p = c.getPlayer();
        if (p.getInventory(InventoryType.EQUIPPED).getItem((byte) ItemConstants.WEAPON) == null) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to attack without a weapon");
            return; 
        }
        
        AttackInfo attack = parseDamage(packet, p, true, false);
        if (attack == null) {
            if (GameConstants.USE_DEBUG) System.out.println("Player {" + p.getName() + "} null attack {RangedAttack}");
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        int skillId = attack.skill;
        PlayerSkill skill = PlayerSkillFactory.getSkill(skillId);

        if (attack.skill != 0) {
            if (skill.getEffect(p.getSkillLevel(skill)).getCoolDown() > 0 && !p.isGameMaster()) {
                if (p.skillisCooling(attack.skill)) {
                    c.getSession().write(PacketCreator.EnableActions());
                    return;
                }
                c.getSession().write(PacketCreator.SkillCooldown(attack.skill, skill.getEffect(p.getSkillLevel(skill)).getCoolDown()));
                p.addCoolDown(attack.skill, System.currentTimeMillis(), skill.getEffect(p.getSkillLevel(skill)).getCoolDown() * 1000);
            }
        }
         
        Item weapon = p.getInventory(InventoryType.EQUIPPED).getItem((byte) -11);
        ItemInformationProvider mii = ItemInformationProvider.getInstance();
        WeaponType type = mii.getWeaponType(weapon.getItemId());

        int projectile = 0;
        int bulletCount = 1;
        MapleStatEffect effect = null;
        if (skillId != 0) {
            effect = attack.getAttackEffect(p);
            bulletCount = effect.getBulletCount();
        }

        boolean hasShadowPartner = p.getBuffedValue(BuffStat.SHADOWPARTNER) != null;
        int damageBulletCount = bulletCount;
        if (hasShadowPartner) bulletCount *= 2;
        for (int i = 0; i < 255; i++) {  
            Item item = p.getInventory(InventoryType.USE).getItem((byte) i);
            if (item != null) {
                boolean clawCondition = type == WeaponType.CLAW && ItemConstants.isThrowingStar(item.getItemId()) && weapon.getItemId() != 1472063;
                boolean bowCondition = type == WeaponType.BOW && ItemConstants.isArrowForBow(item.getItemId());
                boolean crossbowCondition = type == WeaponType.CROSSBOW && ItemConstants.isArrowForCrossBow(item.getItemId());
                boolean gunCondition = type == WeaponType.GUN && ItemConstants.isBullet(item.getItemId());
                boolean mittenCondition = weapon.getItemId() == 1472063 && (ItemConstants.isArrowForBow(item.getItemId()) || ItemConstants.isArrowForCrossBow(item.getItemId()));
                if ((clawCondition || bowCondition || crossbowCondition || mittenCondition || gunCondition) && item.getQuantity() >= bulletCount) {
                    projectile = item.getItemId(); break;
                }
            }
        }

        boolean soulArrow = p.getBuffedValue(BuffStat.SOULARROW) != null;
        boolean shadowClaw = p.getBuffedValue(BuffStat.SHADOW_CLAW) != null;
        if (!soulArrow && !shadowClaw && !c.getPlayer().isGameMaster()) {
            int bulletConsume = bulletCount;
            if (effect != null && effect.getBulletConsume() != 0) {
                bulletConsume = effect.getBulletConsume() * (hasShadowPartner ? 2 : 1);
            }
            InventoryManipulator.removeById(c, InventoryType.USE, projectile, bulletConsume, false, true);
        }
        if (projectile != 0 || soulArrow || attack.skill == 5121002) {
            int visProjectile = projectile;  
            if (mii.isThrowingStar(projectile)) {
                for (int i = 0; i < 255; i++) { 
                    Item item = p.getInventory(InventoryType.CASH).getItem((byte) i);
                    if (item != null) {
                        if (item.getItemId() / 1000 == 5021) {
                            visProjectile = item.getItemId();
                            break;
                        } 
                    }
                }
            } else if (soulArrow || skillId == 3111004 || skillId == 3211004) {
                visProjectile = 0;
            } 
            int stance = attack.stance;
            switch (skillId) {
                case Bowmaster.Hurricane:
                case Marksman.PiercingArrow:
                case Corsair.RapidFire: 
                    stance = attack.direction; 
                    break;
            }
            p.getMap().broadcastMessage(p, PacketCreator.RangedAttack(p.getId(), skillId, stance, attack.numAttackedAndDamage, visProjectile, attack.allDamage, attack.speed), false, true);

            int baseDamage;
            int projectileWatk = 0;
            int totalWatk = p.getStat().getTotalWatk();
            if (projectile != 0) projectileWatk = mii.getWatkForProjectile(projectile);
            if (skillId != Rogue.LuckySeven) { 
                baseDamage = (projectileWatk != 0) ? p.getStat().calculateMaxBaseDamage(totalWatk + projectileWatk) : p.getStat().getCurrentMaxBaseDamage();
            } else {
                baseDamage = (int) (((p.getStat().getTotalLuk() * 5.0) / 100.0) * (totalWatk + projectileWatk));
            } 
            if (skillId == Hunter.ArrowBomb) {
                if (effect != null) {
                    baseDamage *= effect.getX() / 100.0;
                }
            } 

            // Todo
            double critdamagerate = 0.0;
            if (p.getJob().isA(PlayerJob.ASSASSIN)) {
                PlayerSkill criticalthrow = PlayerSkillFactory.getSkill(Assassin.CriticalThrow);
                if (p.getSkillLevel(criticalthrow) > 0) {
                    critdamagerate = (criticalthrow.getEffect(p.getSkillLevel(criticalthrow)).getDamage() / 100.0);
                }
            } else if (p.getJob().isA(PlayerJob.BOWMAN)) {
                PlayerSkill criticalshot = PlayerSkillFactory.getSkill(Archer.CriticalShot);
                int critlevel = p.getSkillLevel(criticalshot);
                if (critlevel > 0) critdamagerate = (criticalshot.getEffect(critlevel).getDamage() / 100.0) - 1.0;
            }

            int maxDamage = baseDamage;
            if (effect != null) maxDamage *= effect.getDamage() / 100.0;
            maxDamage += (int) (baseDamage * critdamagerate);
            maxDamage *= damageBulletCount;
            if (hasShadowPartner) {
                PlayerSkill shadowPartner = PlayerSkillFactory.getSkill(Hermit.ShadowPartner);
                MapleStatEffect shadowPartnerEffect = shadowPartner.getEffect(p.getSkillLevel(shadowPartner));
                maxDamage *= (skillId != 0) ? (1.0 + shadowPartnerEffect.getY() / 100.0) : (1.0 + shadowPartnerEffect.getX() / 100.0);
            }
            if (skillId == Hermit.ShadowMeso) {
                maxDamage = 35000;
            }

            if (effect != null) {
                int money = effect.getMoneyCon();
                if (money != 0) {
                    money = (int) (money + Math.random() * (money * 0.5));
                    if (money > p.getMeso()) {
                        money = p.getMeso();
                    }
                    p.gainMeso(-money, false);
                }
            }
            applyAttack(attack, p, maxDamage, bulletCount);
        }
    }

    public static void MagicDamage(PacketReader packet, Client c) {
        if (c.checkCondition()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        Player p = c.getPlayer();
        if (p.getInventory(InventoryType.EQUIPPED).getItem((byte) ItemConstants.WEAPON) == null) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to attack without a weapon");
            return; 
        }
        
        AttackInfo attack = parseDamage(packet, p, false, true);
        if (attack == null) {
            if (GameConstants.USE_DEBUG) System.out.println("Player {" + p.getName() + "} null attack {MagicDamage}");
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }

        final PlayerSkill skill = PlayerSkillFactory.getSkill(attack.skill);

        if (attack.skill != 0) {
            if (skill.getEffect(p.getSkillLevel(skill)).getCoolDown() > 0 && !p.isGameMaster()) {
                if (p.skillisCooling(attack.skill)) {
                    c.getSession().write(PacketCreator.EnableActions());
                    return;
                }
                c.getSession().write(PacketCreator.SkillCooldown(attack.skill, skill.getEffect(p.getSkillLevel(skill)).getCoolDown()));
                p.addCoolDown(attack.skill, System.currentTimeMillis(), skill.getEffect(p.getSkillLevel(skill)).getCoolDown() * 1000);
            }
        }

        int charge = -1;
        switch (attack.skill) {
            case FPArchMage.BigBang:
            case ILArchMage.BigBang:
            case Bishop.BigBang: 
                charge = attack.charge;
                break;
        }
        
        p.getMap().broadcastMessage(p, PacketCreator.MagicAttack(p.getId(), attack.skill, attack.stance, attack.numAttackedAndDamage, attack.allDamage, charge, attack.speed), false, true);

        int maxdamage = (int) (((p.getStat().getTotalMagic() * 0.8) + (p.getStat().getLuk() / 4) / 18) * skill.getEffect(p.getSkillLevel(skill)).getDamage() * 0.8 * (p.getMasterLevel(skill) * 10 / 100));
        if (attack.numDamage > maxdamage) maxdamage = 99999;
        applyAttack(attack, p, maxdamage, attack.getAttackEffect(p).getAttackCount());
        
        PlayerSkill eaterSkill = PlayerSkillFactory.getSkill((p.getJob().getId() - (p.getJob().getId() % 10)) * 10000); 
        int eaterLevel = p.getSkillLevel(eaterSkill);
        if (eaterLevel > 0) {
            for (Integer singleDamage : attack.allDamage.keySet()) {
                eaterSkill.getEffect(eaterLevel).applyPassive(p, p.getMap().getMapObject(singleDamage.intValue()), 0);
            }
        }
    }
    
    public static void TakeDamage(PacketReader packet, Client c) {        
        Player p = c.getPlayer();
        packet.readInt();
        int damagefrom = packet.readByte();
        packet.readByte();
        
        int damage = packet.readInt();
        int objectId = 0, monsterIdFrom = 0, pgmr = 0, mpAttack = 0;
        int direction = 0, posX = 0, posY = 0, fake = 0;
        boolean isPgmr = false, isPg = true;
        
        if (GameConstants.USE_DEBUG) System.out.println("Takedamaged from player {" + p.getName() +"}");
        
        MapleMonster attacker = null;
        final Field map = p.getMap();
        
        if (damagefrom != -2) {
            monsterIdFrom = packet.readInt();
            objectId = packet.readInt();
            attacker = (MapleMonster) p.getMap().getMapObject(objectId);
            
            if ((p.getMap().getMonsterById(monsterIdFrom) == null || attacker == null) && monsterIdFrom != 9300166) {
                return;
            } 
            
            if (monsterIdFrom == 9300166) {
                if (p.haveItem(4031868)) {
                    if (p.getItemQuantity(4031868, false) > 1) {
                        int amount = p.getItemQuantity(4031868, false) / 2;
                        Point position = new Point(c.getPlayer().getPosition().x, c.getPlayer().getPosition().y);
                        InventoryManipulator.removeById(c, ItemInformationProvider.getInstance().getInventoryType(4031868), 4031868, amount, false, false);
                        for (int i = 0; i < amount; i++) {
                            position.setLocation(c.getPlayer().getPosition().x + (i % 2 == 0 ? (i * 15) : (-i * 15)), c.getPlayer().getPosition().y);
                            map.spawnItemDrop(p, p, new Item(4031868, (short) 0, (short) 1), map.calcDropPos(position, p.getPosition()), true, true);
                        }
                    } else {
                        InventoryManipulator.removeById(c, ItemInformationProvider.getInstance().getInventoryType(4031868), 4031868, 1, false, false);
                        map.spawnItemDrop(p, p, new Item(4031868, (short) 0, (short) 1), c.getPlayer().getPosition(), true, true);
                    }
                }
            }
            direction = packet.readByte();
        }
        
        if (attacker != null && GameConstants.TRACK_MISSGODMODE) {
            if (damage < 1) {
                final double difference = (double) Math.max(p.getLevel() - attacker.getLevel(), 0);
                final double chanceToBeHit = (double) attacker.getAccuracy() / ((1.84d + 0.07d * difference) * (double) p.getStat().getTotalEva()) - 1.0d;
                if (chanceToBeHit > 0.85d) {
                    p.getCheatTracker().incrementNumGotMissed();
                }
            } else {
                p.getCheatTracker().setNumGotMissed(0);
            }
            if (p.getCheatTracker().getNumGotMissed() > 5 && p.getCheatTracker().getNumGotMissed() < 15) {
                BroadcastService.broadcastGMMessage(PacketCreator.ServerNotice(5, "WARNING: The player with name " + PlayerStringUtil.makeMapleReadable(c.getPlayer().getName()) + " on channel " + c.getChannel() + " MAY be using miss godmode."));
            } else if (p.getCheatTracker().getNumGotMissed() >= 15) {
                AutobanManager.getInstance().autoban(c, "Miss godmode.");
                return;
            }
        }
        
        if (damagefrom != -1 && damagefrom != -2 && attacker != null) {
            final MobAttackInfo attackInfo = MobAttackInfoFactory.getInstance().getMobAttackInfo(attacker, damagefrom);
            if (attackInfo.isDeadlyAttack()) {
                mpAttack = p.getStat().getMp() - 1;
            }
            mpAttack += attackInfo.getMpBurn();
            MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel());
            if (skill != null && damage > 0) {
                skill.applyEffect(p, attacker, false);
            }
            if (attacker != null) {
                attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
                if (p.getBuffedValue(BuffStat.MANA_REFLECTION) != null && damage > 0 && !attacker.isBoss()) {
                    switch (p.getJob()) {
                        case FP_ARCHMAGE:
                        case IL_ARCHMAGE:
                        case BISHOP:
                            int id = p.getJob().getId() * 10000 + 2;
                            PlayerSkill manaReflectSkill = PlayerSkillFactory.getSkill(id);
                            if (p.isBuffFrom(BuffStat.MANA_REFLECTION, manaReflectSkill) && p.getSkillLevel(manaReflectSkill) > 0 && manaReflectSkill.getEffect(p.getSkillLevel(manaReflectSkill)).makeChanceResult()) {
                                int bouncedamage = (damage * manaReflectSkill.getEffect(p.getSkillLevel(manaReflectSkill)).getX() / 100);
                                if (bouncedamage > attacker.getMaxHp() / 5) {
                                    bouncedamage = attacker.getMaxHp() / 5;
                                }
                                p.getMap().damageMonster(p, attacker, bouncedamage);
                                p.getMap().broadcastMessage(p, MonsterPackets.DamageMonster(objectId, bouncedamage), true);
                                p.getClient().getSession().write(EffectPackets.ShowOwnBuffEffect(id, PlayerEffects.SKILL_SPECIAL.getEffect()));
                                p.getMap().broadcastMessage(p, EffectPackets.BuffMapVisualEffect(p.getId(), id, PlayerEffects.SKILL_SPECIAL.getEffect()), false);
                            }
                        break;
                    }
                }
            }
        }
        
        if (damage == -1) {
            int job = (int) (p.getJob().getId() / 10 - 40);
            fake = 4020002 + (job * 100000);
            if (damagefrom == -1 && damagefrom != -2 && p.getInventory(InventoryType.EQUIPPED).getItem((byte) -10) != null) {
                int[] guardianSkillId = {1120005, 1220006};
                for (int guardian : guardianSkillId) {
                    PlayerSkill guardianSkill = PlayerSkillFactory.getSkill(guardian);
                    if (p.getSkillLevel(guardianSkill) > 0 && attacker != null) {
                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.STUN, 1), guardianSkill, false);
                        attacker.applyStatus(p, monsterStatusEffect, false, 2 * 1000);
                    }
                }
            }
        }
        
        if (damage > 0 && !p.isHidden()) {
            if (attacker != null && !attacker.isBoss()) {
                if (damagefrom == BUMP_DAMAGE && p.getBuffedValue(BuffStat.POWERGUARD) != null) {
                    int bouncedamage = (int) (damage * (p.getBuffedValue(BuffStat.POWERGUARD).doubleValue() / 100));
                    bouncedamage = Math.min(bouncedamage, attacker.getMaxHp() / 10);
                    p.getMap().damageMonster(p, attacker, bouncedamage);
                    damage -= bouncedamage;
                    p.getMap().broadcastMessage(p, MonsterPackets.DamageMonster(objectId, bouncedamage), true, true);
                    p.checkMonsterAggro(attacker);
                }
            }
            if (damagefrom != MAP_DAMAGE) {
                int achilles = 0;
                PlayerSkill achilles1 = null;
                int jobid = p.getJob().getId();
                if (jobid < 200 && jobid % 10 == 2) {
                    achilles1 = PlayerSkillFactory.getSkill(jobid * 10000 + jobid == 112 ? 4 : 5);
                    achilles = p.getSkillLevel(achilles);
                }
                if (achilles != 0 && achilles1 != null) {
                    damage *= (int) (achilles1.getEffect(achilles).getX() / 1000.0 * damage);
                }
            }
            Integer mesoguard = p.getBuffedValue(BuffStat.MESOGUARD);
            if (p.getBuffedValue(BuffStat.MAGIC_GUARD) != null && mpAttack == 0) {
                int mploss = (int) (damage * (p.getBuffedValue(BuffStat.MAGIC_GUARD).doubleValue() / 100.0));
                int hploss = damage - mploss;
                if (mploss > p.getStat().getMp()) {
                    hploss += mploss - p.getStat().getMp();
                    mploss = p.getStat().getMp();
                }
                p.getStat().addMPHP(-hploss, -mploss);
            } else if (mesoguard != null) {
                damage = Math.round(damage / 2);
                int mesoLoss = (int) (damage * (mesoguard.doubleValue() / 100.0));
                if (p.getMeso() < mesoLoss) {
                    p.gainMeso(-p.getMeso(), false);
                    p.cancelBuffStats(BuffStat.MESOGUARD);
                } else {
                    p.gainMeso(-mesoLoss, false);
                }
                p.getStat().addMPHP(-damage, -mpAttack);
            } else if (p.getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
                if (p.isActiveBuffedValue(Corsair.Battleship)) {
                    p.sendBattleshipHP(damage);
                    if (p.getCurrentBattleShipHP() > 0) {
                        p.announce(PacketCreator.SkillCooldown(5221999, p.getCurrentBattleShipHP() / 10));
                    }
                } else {
                    p.getStat().addMPHP(-damage, -mpAttack);
                }
            } else {
                p.getStat().addMPHP(-damage, -mpAttack);
            }  
        }
        
        if (!p.isHidden()) {
            p.getMap().broadcastMessage(p, PacketCreator.DamagePlayer(damagefrom, monsterIdFrom, p.getId(), damage, fake, direction, isPgmr, pgmr, isPg, objectId, posX, posY), false);
            p.getStat().updateSingleStat(PlayerStat.HP, p.getStat().getHp());
            p.getStat().updateSingleStat(PlayerStat.MP, p.getStat().getMp());
            p.checkBerserk(true);            
        } else {
            p.getMap().broadcastGMMessage(p, PacketCreator.DamagePlayer(damagefrom, monsterIdFrom, p.getId(), damage, fake, direction, isPgmr, pgmr, isPg, objectId, posX, posY), false);
            p.getStat().updateSingleStat(PlayerStat.HP, p.getStat().getHp());
            p.getStat().updateSingleStat(PlayerStat.MP, p.getStat().getMp());
            p.checkBerserk(false);
        }
    }

    public static void MovePlayer(PacketReader r, Player p) {
        r.readByte();
        r.readInt();
        final List<LifeMovementFragment> mov = MovementParse.parseMovement(r);
        if (mov != null) {
            MovementParse.updatePosition(mov, p, 0);
            p.getMap().movePlayer(p, p.getPosition());
            
            OutPacket packet = PacketCreator.MovePlayer(p.getId(), mov);
            if (p.isHidden()) {
                p.getMap().broadcastGMMessage(p, packet, false);
            } else {
                p.getMap().broadcastMessage(p, packet, false);
            }
        }
    }

    public static void ChangeEmotion(PacketReader packet, Client c) {
        int emote = packet.readInt();
        if (emote > 7) {
            final int emoteID = 5159992 + emote;
            final InventoryType type = ItemConstants.getInventoryType(emoteID);
            if (c.getPlayer().getInventory(type).findById(emoteID) == null) {
                return;
            }
        }
        if (emote > 0 && c.getPlayer() != null && c.getPlayer().getMap() != null) { 
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), EffectPackets.ExpressionChange(c.getPlayer(), emote), false);
        }
    }

    public static void ReplenishHpMp(PacketReader packet, Client c) {
        if (c.checkCondition()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        Player p = c.getPlayer();
        if (p == null) {
            return;
        }
        packet.skip(4);
        int hp = packet.readShort();
        int mp = packet.readShort();
        
        if (p.getHp() == 0 || hp > 400 || mp > 1000 || (hp > 0 && mp > 0)) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to replenish too much HP/MP at once");
            return;
        }
        
        long now = System.currentTimeMillis();
        if ((hp != 0) && (p.canHP(now + 1000L))) {
            if (hp > 1000) {
                AutobanManager.getInstance().autoban(p.getClient(), p.getName() + " healed for " + hp + "/HP in map: " + c.getPlayer().getMapId() + ".");
                return;          
            }
            p.getStat().addHP(hp);
        }
        if (mp != 0 && (p.canMP(now + 1000L))) {
            if (mp > 1000) {
                AutobanManager.getInstance().autoban(p.getClient(), p.getName() + " healed for " + mp + "/MP in map: " + c.getPlayer().getMapId() + ".");
                return;
            }
            p.getStat().addMP(mp);
        }
    }

    public static void OpenInfo(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        if (p == null || p.getMap() == null) {
            return;
        }
        packet.readInt();
        int cid = packet.readInt();
        final Player target = (Player) c.getPlayer().getMap().getMapObject(cid);
        if (target != null) {
           if (target instanceof Player) {
               if (!target.isGameMaster() || (p.isGameMaster() && target.isGameMaster())) {
                   c.getSession().write(PacketCreator.PersonalInfo(target, packet.readBool()));
               } else {
                   c.getSession().write(PacketCreator.EnableActions());
               }
            } 
        }
    }
    
    public static void SpecialMove(PacketReader packet, Client c) {
        if (c.checkCondition()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        
        Player p = c.getPlayer();
        long time = packet.readInt();
        if (p.getLastRequestTime() > time || p.getLastRequestTime() == time) { 
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        c.getPlayer().setLastRequestTime(time);
        
        Point pos = null;
        final int skillId = packet.readInt();
        final int skillLevel = packet.readByte();
        PlayerSkill skill = PlayerSkillFactory.getSkill(skillId);

        MapleStatEffect effect = skill.getEffect(p.getSkillLevel(skill));

        if (skill.isGMSkill() && !p.isGameMaster()) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        if (effect.getCoolDown() > 0 && !p.isGameMaster() && skillId != Corsair.Battleship) {
            if (p.skillisCooling(skillId)) {
                c.getSession().write(PacketCreator.EnableActions());
                return;
            }
            c.getSession().write(PacketCreator.SkillCooldown(skillId, effect.getCoolDown()));
            p.addCoolDown(skillId, System.currentTimeMillis(), effect.getCoolDown() * 1000);
        }
        if (skillLevel != skillLevel) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, p.getName() + " is using a movement skill that he does not have, ID: " + skill.getId());
            return;
        } 
        switch (skillId) {
            case Hero.MonsterMagnet:
            case Paladin.MonsterMagnet:
            case DarkKnight.MonsterMagnet:
                int amount = packet.readInt();
                for (int i = 0; i < amount; i++) {
                    int mobId = packet.readInt();
                    byte success = packet.readByte();
                    p.getMap().broadcastMessage(p, MonsterPackets.ShowMagnet(mobId, success), false);
                    MapleMonster monster = (MapleMonster) p.getMap().getMonsterByOid(mobId);
                    if (monster != null) {
                        if (!monster.isBoss()) {
                            monster.switchController(p, monster.controllerHasAggro());
                        }
                    }
                }
                p.getMap().broadcastMessage(p, EffectPackets.BuffMapVisualEffect(p.getId(), skillId, p.getSkillLevel(skillId), packet.readByte()), false);
                c.announce(PacketCreator.EnableActions());
                break;
            default:
                if (packet.available() == 5) {
                    pos = new Point(packet.readShort(), packet.readShort());
                }
                if (p.isAlive()) {
                    if (skill.getId() != Priest.MysticDoor) {
                        skill.getEffect(skillLevel).applyTo(p, pos);
                    } else if (p.canDoor()) {
                        if (!FieldLimit.DOOR.check(p.getMap().getFieldLimit())) {
                            p.cancelMagicDoor();
                            skill.getEffect(skillLevel).applyTo(p, pos);
                        } else {
                            c.getSession().write(PacketCreator.EnableActions());
                        }
                    } else {
                        p.message("Please wait 5 seconds before casting Mystic Door again.");
                    }
                } else {
                    c.getSession().write(PacketCreator.EnableActions());
                }
            break;
        }
    }

    public static void InnerPortal(PacketReader r, Client c) {
        r.readByte();
        String portalName = r.readMapleAsciiString();
        Point startPos = r.readPos();
        Point endPos = r.readPos();
        
        if (c.getPlayer().getMap().getPortal(portalName) == null){
            c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to enter the nonexistent portal.");
            return;
        }
        boolean foundPortal = false;
        for (Portal portal : c.getPlayer().getMap().getPortals()){
            if (portal.getType() == 1 || portal.getType() == 2 || portal.getType() == 10 || portal.getType() == 20){
                if (portal.getPosition().equals(startPos) || portal.getPosition().equals(endPos)) {
                    foundPortal = true;
                }
            }
        }
        if (!foundPortal){
            c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.WZ_EDIT, "Used inner portal: " + portalName + " in " + c.getPlayer().getMapId() + " targetPos: " + endPos.toString() + " when it doesn't exist.");
        }
    }

    public static final void TrockAddMap(final PacketReader packet, final Client c, final Player p) {
        final byte operation = packet.readByte();
        final boolean isVip = packet.readBool();
              
        switch (operation) {
            case 0:
                if (isVip) {
                    p.deleteFromRocks(packet.readInt());
                } else {
                    p.deleteFromRegRocks(packet.readInt());
                }
                break;
            case 1:
                if (!FieldLimit.CANNOTVIPROCK.check(p.getMap().getFieldLimit())) {
                    if (isVip) {
                        p.addRockMap();
                    } else {
                        p.addRegRockMap();
                    }
                }  else {
                    p.dropMessage(1, "You can not add this map.");
                }
                break;
            default:
                break;
        }
        c.getSession().write(PacketCreator.GetTrockRefresh(p, isVip, operation == 3));
    }

    public static void CancelBuffHandler(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        if (p == null) {
            return;
        }
        int sourceid = packet.readInt();
        switch (sourceid) {
            case FPArchMage.BigBang:
            case ILArchMage.BigBang:
            case Bishop.BigBang:
            case Bowmaster.Hurricane:
            case Marksman.PiercingArrow:
            case Corsair.RapidFire:
            p.getMap().broadcastMessage(p, PacketCreator.SkillCancel(p, sourceid), false);
            break;
        default:
            p.cancelEffect(PlayerSkillFactory.getSkill(sourceid).getEffect(1), false, -1);
            break;
        }
    }

    public static final void CancelItemEffect(final PacketReader packet, final Client c) {
        MapleStatEffect effect = ItemInformationProvider.getInstance().getItemEffect(-packet.readInt());
        c.getPlayer().cancelEffect(effect, false, -1);
    }

    public static final void ChangeKeymap(final PacketReader r, final Client c) {
        Player p = c.getPlayer();
        if (p == null || p.getMap() == null) {
            return;
        }
        int actionType = r.readInt();
        switch (actionType) {
            case BINDING_CHANGE_KEY_MAPPING:
                for (int i = r.readInt(); i > 0; --i) {
                    final int key = r.readInt();
                    final int type = r.readByte();
                    final int action = r.readInt();
                    final PlayerSkill skill = PlayerSkillFactory.getSkill(action);
                    if (skill != null) {
                        if (!p.isGameMaster() && SkillConstants.isGMSkill(skill.getId())) {
                            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, p.getName() + " tried packet keymapping.");
                            return;
                        }
                        if (p.getSkillLevel(skill) < 1) {
                            continue;
                        }
                    }
                    PlayerKeyBinding newbinding = new PlayerKeyBinding(type, action);
                    p.changeKeybinding(key, newbinding);
                }
                break;
            case BINDING_CHANGE_AUTO_HP_POT: {
                int itemId = r.readInt();
                if (itemId == 0) {
                    p.setAutoHpPot(0);
                    return;
                }
                if (!p.haveItem(ItemConstants.HP_ITEM, 1, true, false)) {
                    p.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(itemId));
                    return;
                }

                p.setAutoHpPot(itemId);
                break;
            }
            case BINDING_CHANGE_AUTO_MP_POT: {
                int itemId = r.readInt();
                if (itemId == 0) {
                    p.setAutoMpPot(0);
                    return;
                }
                if (!p.haveItem(ItemConstants.MP_ITEM, 1, true, false)) {
                    p.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(itemId));
                    return;
                }
                p.setAutoMpPot(itemId);
                break;
            }
        }
    }

    public static final void ChangeMapSpecial(PacketReader r, Client c) {
        r.readByte();
        String portalName = r.readMapleAsciiString();
        r.readByte();
        r.readByte();

        Portal portal = c.getPlayer().getMap().getPortal(portalName);
        if (GameConstants.USE_DEBUG) System.out.println("[PORTAL] PortalName: " + portalName);
        if (portal != null) {
           portal.enterPortal(c);
        } else {
           c.getSession().write(PacketCreator.EnableActions());
        }
    }

    public static void UseItemEffect(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        if (p == null) {
            return;
        }
        int itemId = packet.readInt();
        final Item toUse = p.getInventory(InventoryType.CASH).findById(itemId);
        if (toUse == null || toUse.getQuantity() < 1) {
            if (itemId != 0) {
                return;
            }
            p.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(itemId));
            return;
        }
        p.setItemEffect(toUse.getItemId());
        p.getMap().broadcastMessage(p, EffectPackets.ItemEffect(p.getId(), toUse.getItemId()), false);
    }
    
    public static final void UseChair(PacketReader packet, Client c) {
        int id = packet.readShort();
        Player p = c.getPlayer();
        if (p == null) {
            return;
        }
        if (id == -1) {
            if (p.getChair() != 0) {
                p.setChair(0);
                p.getMap().broadcastMessage(p, EffectPackets.ShowChair(p.getId(), 0), false);
            } 
            p.getClient().write(EffectPackets.RiseFromChair());
        } else {
            p.setChair(id);
            p.getClient().write(EffectPackets.SitOnChair((short) id));
        }
    }

    public static final void UseItemChair(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        if (p == null) {
            return;
        }
        final int itemId = packet.readInt();
        Item toUse = p.getInventory(InventoryType.SETUP).findById(itemId);
        if (toUse == null) {
            p.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(itemId));
            return;
        } else {
            p.setChair(itemId);
            p.getMap().broadcastMessage(p, EffectPackets.ShowChair(p.getId(), itemId), false);
        }
        c.getSession().write(PacketCreator.EnableActions());
    }

    public static final void SkillEffect(PacketReader r, Client c) {
        Player p = c.getPlayer();
        if (p == null || p.isHidden()) {
            return;
        }
        final int skillID = r.readInt();
        final int level = r.readByte();
        final byte flags = r.readByte();
        final int speed = r.readByte();
        final PlayerSkill skill = PlayerSkillFactory.getSkill(skillID);
        final int currentLevel = p.getSkillLevel(skill); 
        if (currentLevel > 0 && currentLevel == level && skill.isChargeSkill()) {    
            switch (skillID) {
                case Bowmaster.Hurricane:
                case Corsair.RapidFire: 
                case Hero.MonsterMagnet:
                case Paladin.MonsterMagnet:
                case DarkKnight.MonsterMagnet:
                case FPArchMage.BigBang:
                case ILArchMage.BigBang: 
                case Bishop.BigBang: 
                case Brawler.CorkscrewBlow:
                case Gunslinger.Grenade: 
                case FPMage.Explosion:
                case ChiefBandit.Chakra:
                case Marksman.PiercingArrow:
                    p.getMap().broadcastMessage(p, EffectPackets.SkillEffect(p, skillID, level, flags, speed), false);
                    break;
                default:
                    BroadcastService.broadcastGMMessage(PacketCreator.ServerNotice(5, p.getName() + " is using an unusable skill, skillID: " + skillID));
                    c.getSession().close();
                    break;
            }
        } else {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to charge non-prepared skill");
        }
    }

    public static void SkillMacroAssign(final PacketReader packet, final Client c) {
        final int count = packet.readByte();
        PlayerSkillMacro[] macros = new PlayerSkillMacro[count];
        for (int i = 0; i < count; i++) {
            String name = packet.readMapleAsciiString();
            boolean silent = packet.readBool();
            PlayerSkill skill1 = PlayerSkillFactory.getSkill(packet.readInt());
            PlayerSkill skill2 = PlayerSkillFactory.getSkill(packet.readInt());
            PlayerSkill skill3 = PlayerSkillFactory.getSkill(packet.readInt());
            if (!c.getPlayer().isGameMaster()) {
                if ((skill1 != null && !ItemConstants.canEquip(skill1, c)) || (skill2 != null && !ItemConstants.canEquip(skill2, c)) || (skill3 != null && !ItemConstants.canEquip(skill3, c))) {
                    c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to force a skill he should not have on a macro.");
                    return;
                }
            }
            macros[i] = new PlayerSkillMacro(name, silent, skill1 != null ? skill1.getId() : 0, skill2 != null ? skill2.getId() : 0, skill3 != null ? skill3.getId() : 0);
        }
        c.getPlayer().setMacros(macros);
    }
}
