/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.player.Player;
import client.Client;
import client.player.buffs.BuffStat;
import client.player.skills.PlayerSkill;
import client.player.skills.PlayerSkillFactory;
import client.player.skills.PlayerSummonSkillEntry;
import client.player.violation.CheatingOffense;
import constants.GameConstants;
import constants.SkillConstants.Outlaw;
import server.life.status.MonsterStatusEffect;
import static handling.channel.handler.MovementParse.parseMovement;
import static handling.channel.handler.MovementParse.updatePosition;
import handling.mina.PacketReader;
import java.awt.Point;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import packet.creators.PacketCreator;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.maps.Field;
import server.maps.MapleSummon;
import server.maps.SummonMovementType;
import server.movement.LifeMovementFragment;

/**
 *
 * @author GabrielSin
 */
public class SummonHandler {

    public static class SummonAttackEntry {

        private final int damage;
        private final WeakReference<MapleMonster> mob;

        public SummonAttackEntry(MapleMonster mob, int damage) {
            super();
            this.mob = new WeakReference<>(mob);
            this.damage = damage;
        }

        public MapleMonster getMonster() {
            return mob.get();
        }

        public int getDamage() {
            return damage;
        }
    }
    
    public static void DamageSummon(PacketReader packet, Client c) {
        int summonEntId = packet.readInt();

        Player p = c.getPlayer();
        MapleSummon puppet = (MapleSummon) p.getMap().getMapObject(summonEntId);

        if (puppet != null) {
            int misc = packet.readByte();
            int damage = packet.readInt();
            
            int mobEid = 0, isLeft = 0;
            MapleMonster monster = null;
            if (misc > -2) {
                mobEid = packet.readInt();
                isLeft = packet.readByte();  
                
                monster = p.getMap().getMonsterById(mobEid);
            }
            
            if (puppet.hurt(damage)) {
                p.cancelEffectFromBuffStat(BuffStat.PUPPET);
            }

            if (GameConstants.USE_DEBUG) System.out.println("isPuppet: " + puppet.isPuppet() + " | damage: " + damage + " | action: " + misc + " | mobEid: " + mobEid + "  | skill: " + puppet.getSkill() + " | objecId: " + summonEntId);

            p.getMap().broadcastMessage(p, PacketCreator.DamageSummon(p.getId(), puppet.getObjectId(), (byte) 12, damage, monster.getId(), isLeft), puppet.getPosition());

        } 	

    }

    public static void MoveSummon(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        int objectId = packet.readInt();
        Point startPos = packet.readPos();
        
        List<LifeMovementFragment> mov = parseMovement(packet);
        Collection<MapleSummon> summons = p.getSummons().values();
        MapleSummon summon = null;
        for (MapleSummon sum : summons) {
            if (sum.getObjectId() == objectId) {
                summon = sum;
                break;
            }
        } 
        if (summon != null) {
            updatePosition(mov, summon, 0);
            if (summon.getObjectId() == objectId && summon.getMovementType() != SummonMovementType.STATIONARY) {
                p.getMap().broadcastMessage(p, PacketCreator.MoveSummon(p.getId(), objectId, startPos, mov), summon.getPosition());
            }
        } else {
            p.getSummons().remove(objectId);
        }
    }

    public static void SummonAttack(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        
        final int objectId = packet.readInt();
        final Field map = p.getMap();
        
        if (!p.isAlive() || p.getMap() == null) {
            return;
        }
        
        MapleSummon summon = null;
        for (MapleSummon sum : p.getSummons().values()) {
            if (sum.getObjectId() == objectId) {
                summon = sum;
            }
        }
        if (summon == null) {
            return;
        }
        
        PlayerSkill summonSkill = PlayerSkillFactory.getSkill(summon.getSkill());
        MapleStatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());
        final PlayerSummonSkillEntry sse = PlayerSkillFactory.getSummonData(summon.getSkill());
        if (summon.getSkill() / 1000000 != 35 && summon.getSkill() != 33101008 && sse == null) {
            return;
        }
        
        packet.skip(4);
        final byte direction = packet.readByte();
        List<SummonAttackEntry> allDamage = new ArrayList<>();
        int numAttacked = packet.readByte();
        if (sse != null && numAttacked > sse.mobCount) {
            p.getCheatTracker().registerOffense(CheatingOffense.SUMMON_HACK_MOBS);
        }
        for (int x = 0; x < numAttacked; x++) {
            final MapleMonster mob = map.getMonsterByOid(packet.readInt());
            if (mob == null) {
                continue;
            }
            packet.skip(14);
            int damage = packet.readInt();
            allDamage.add(new SummonAttackEntry(mob, damage));
        }
        if (!p.isAlive()) {
            p.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        
        p.getMap().broadcastMessage(p, PacketCreator.SummonAttack(p.getId(), summon.getObjectId(), direction, allDamage, p.getLevel()), summon.getPosition());
      
        allDamage.forEach((attackEntry) -> {
            int damage = attackEntry.getDamage();
            MapleMonster target = p.getMap().getMonsterByOid(attackEntry.getMonster().getObjectId());
            if (target != null) {
                if (damage > 0 && summonEffect.getMonsterStati().size() > 0) {
                    if (summonEffect.makeChanceResult()) {
                        target.applyStatus(p, new MonsterStatusEffect(summonEffect.getMonsterStati(), summonSkill, false), summonEffect.isPoison(), 4000);
                    }
                }
                p.getMap().damageMonster(p, target, damage);
            }
        });
        if (summon.getSkill() == Outlaw.Gaviota) {
            c.getPlayer().cancelEffect(summonEffect, false, -1L);
        }
    }
}
