/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packet.creators;

import server.life.status.MonsterStatus;
import server.life.status.MonsterStatusEffect;
import java.awt.Point;
import java.util.List;
import java.util.Map;
import packet.opcode.SendPacketOpcode;
import packet.transfer.write.OutPacket;
import packet.transfer.write.WritingPacket;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.movement.LifeMovementFragment;

public class MonsterPackets {
    
   public static OutPacket DamageMonster(int objectID, int damage) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        wp.writeInt(objectID);
        wp.write(0);
        wp.writeInt(damage);
        return wp.getPacket();
    }
    
    public static OutPacket KillMonster(int oid, boolean animation) {
        return KillMonster(oid, animation ? 1 : 0);
    }

    /**
    * Gets a packet telling the client that a monster was killed.
    * @param objectID The objectID of the killed monster.
    * @param animation 0 = dissapear, 1 = fade out, 2+ = special
    * @return The kill monster packet.
    */
    public static OutPacket KillMonster(int objectID, int animation) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.KILL_MONSTER.getValue());
        wp.writeInt(objectID);
        wp.write(animation);
        wp.write(animation);
        return wp.getPacket();
    }
    
    public static OutPacket HealMonster(int objectID, int heal) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        wp.writeInt(objectID);
        wp.write(0);
        wp.writeInt(-heal);
        return wp.getPacket();
    }
    
    public static OutPacket ShowMonsterHP(int objectID, int remHPpercentage) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOW_MONSTER_HP.getValue());
        wp.writeInt(objectID);
        wp.write(remHPpercentage);
        return wp.getPacket();
    }
    
    public static OutPacket ShowBossHP(int oid, int currHP, int maxHP, byte tagColor, byte tagBgColor) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.FIELD_EFFECT.getValue());
        wp.write(5);
        wp.writeInt(oid);
        wp.writeInt(currHP);
        wp.writeInt(maxHP);
        wp.write(tagColor);
        wp.write(tagBgColor);
        return wp.getPacket();
    }
    
    public static OutPacket MoveMonster(int usesSkill, int skill, int skill_1, int skill_2, int skill_3, int skill_4, int objectID, Point startPos, List<LifeMovementFragment> moves) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MOVE_MONSTER.getValue());
        wp.writeInt(objectID);
        wp.write(0);
        wp.write(usesSkill);
        wp.write(skill);
        wp.write(skill_1);
        wp.write(skill_2);
        wp.write(skill_3);
        wp.write(skill_4);
        wp.writePos(startPos);
        HelpPackets.SerializeMovementList(wp, moves);
        return wp.getPacket();
    }
      
    public static OutPacket MoveMonster(int usesSkill, int skill, int skill_1, int skill_2, int skill_3, int objectID, Point startPos, List<LifeMovementFragment> moves) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MOVE_MONSTER.getValue());
        wp.writeInt(objectID);
        wp.write(usesSkill);
        wp.write(skill);
        wp.write(skill_1);
        wp.write(skill_2);
        wp.write(skill_3);
        wp.write(0);
        wp.writeShort(startPos.x);
        wp.writeShort(startPos.y);
        HelpPackets.SerializeMovementList(wp, moves);
        return wp.getPacket();
    }
    
    /**
    * Gets a spawn monster packet.
    * @param life The monster to spawn.
    * @param newSpawn Is it a new spawn?
    * @return The spawn monster packet.
    */
    public static OutPacket SpawnMonster(MapleMonster life, boolean newSpawn) {
        return SpawnMonsterInternal(life, false, newSpawn, false, 0, false);
    }

    /**
    * Gets a spawn monster packet.
    * @param life The monster to spawn.
    * @param newSpawn Is it a new spawn?
    * @param effect The spawn effect.
    * @return The spawn monster packet.
    */
    public static OutPacket SpawnMonster(MapleMonster life, boolean newSpawn, int effect) {
        return SpawnMonsterInternal(life, false, newSpawn, false, effect, false);
    }

    /**
    * Gets a control monster packet.
    * @param life The monster to give control to.
    * @param newSpawn Is it a new spawn?
    * @param aggro Aggressive monster?
    * @return The monster control packet.
    */
    public static OutPacket ControlMonster(MapleMonster life, boolean newSpawn, boolean aggro) {
        return SpawnMonsterInternal(life, true, newSpawn, aggro, 0, false);
    }
        
    public static OutPacket MakeMonsterInvisible(MapleMonster life){
        return SpawnMonsterInternal(life, true, false, false, 0, true);
    }
    
    /**
    * Internal function to handler monster spawning and controlling.
    * @param monster The mob to perform operations with.
    * @param requestController Requesting control of mob?
    * @param newSpawn New spawn (fade in?)
    * @param aggro Aggressive mob?
    * @param effect The spawn effect to use.
    * @return The spawn/control packet.
    */
    private static OutPacket SpawnMonsterInternal(MapleMonster monster, boolean requestController, boolean newSpawn, boolean aggro, int effect, boolean makeInvis) {
        WritingPacket wp = new WritingPacket();
        if (makeInvis) {
            wp.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
            wp.write(0);
            wp.writeInt(monster.getObjectId());
            return wp.getPacket();
        }
         if (requestController) {
            wp.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
            wp.write(aggro ? 2 : 1);
        } else {
            wp.writeShort(SendPacketOpcode.SPAWN_MONSTER.getValue());
        }
        wp.writeInt(monster.getObjectId());
        wp.write(5); 
        wp.writeInt(monster.getId());
        wp.write(0);
        wp.writeShort(0);
        wp.write(8);
        wp.writeInt(0);
        wp.writePos(monster.getPosition());
        wp.write(monster.getStance());
        wp.writeShort(0);
        wp.writeShort(monster.getFh());
        if (effect > 0) {
            wp.write(effect);
            wp.write(0);
            wp.writeShort(0);
        }
        wp.write(newSpawn ? -2 : -1);
        wp.write(monster.getTeam());
        wp.writeInt(0);
        return wp.getPacket();
    }

    /**
    * Handles monsters not being targettable, such as Zakum's first body.
    * @param monster The mob to spawn as non-targettable.
    * @param effect The effect to show when spawning.
    * @return The packet to spawn the mob as non-targettable.
    */
    public static OutPacket SpawnFakeMonster(MapleMonster monster, int effect) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        wp.write(1);
        wp.writeInt(monster.getObjectId());
        wp.write(5);
        wp.writeInt(monster.getId());
        wp.writeInt(0);
        wp.writeShort(monster.getPosition().x);
        wp.writeShort(monster.getPosition().y);
        wp.write(monster.getStance());
        wp.writeShort(monster.getStartFh());
        wp.writeShort(monster.getFh());
        if (effect > 0) {
            wp.write(effect);
            wp.write(0);
            wp.writeShort(0);
        }
        wp.writeShort(-2);
        wp.write(monster.getTeam());
        wp.writeInt(0);
        return wp.getPacket();
    }

    /**
     * Makes a monster previously spawned as non-targettable, targettable.
     * @param monster The mob to make targettable.
     * @return The packet to make the mob targettable.
     */
    public static OutPacket MakeMonsterReal(MapleMonster monster) {													
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPAWN_MONSTER.getValue());
        wp.writeInt(monster.getObjectId());
        wp.write(5);
        wp.writeInt(monster.getId());
        wp.writeInt(0);
        wp.writeShort(monster.getPosition().x);
        wp.writeShort(monster.getPosition().y);
        wp.write(monster.getStance());
        wp.writeShort(monster.getStartFh());
        wp.writeShort(monster.getFh());
        wp.writeShort(-1);
        wp.writeInt(0);
        return wp.getPacket();
    }

    public static OutPacket StopControllingMonster(int objectID) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        wp.write(0);
        wp.writeInt(objectID);
        return wp.getPacket();
    }

    /**
    * Gets a response to a move monster packet.
    * @param objectID The ObjectID of the monster being moved.
    * @param moveID The movement ID.
    * @param currentMP The current MP of the monster.
    * @param useSkills Can the monster use skills?
    * @return The move response packet.
    */
    public static OutPacket MoveMonsterResponse(int objectID, short moveID, int currentMP, boolean useSkills) {
        return MoveMonsterResponse(objectID, moveID, currentMP, useSkills, 0, 0);
    }

    /**
    * Gets a response to a move monster packet.
    * @param objectID The ObjectID of the monster being moved.
    * @param moveID The movement ID.
    * @param currentMP The current MP of the monster.
    * @param useSkills Can the monster use skills?
    * @param skillId The skill ID for the monster to use.
    * @param skillLevel The level of the skill to use.
    * @return The move response packet.
    */
    public static OutPacket MoveMonsterResponse(int objectID, short moveID, int currentMP, boolean useSkills, int skillId, int skillLevel) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MOVE_MONSTER_RESPONSE.getValue());
        wp.writeInt(objectID);
        wp.writeShort(moveID);
        wp.write(useSkills ? 1 : 0);
        wp.writeShort(currentMP);
        wp.write(skillId);
        wp.write(skillLevel);
        return wp.getPacket();
    }
    
    public static OutPacket ApplyMonsterStatus(int objectID, Map<MonsterStatus, Integer> stats, int skill, boolean monsterSkill, int delay) {
        return ApplyMonsterStatus(objectID, stats, skill, monsterSkill, delay, null);
    }
    
    public static OutPacket ApplyMonsterStatus(int objectID, Map<MonsterStatus, Integer> stats, int skill, boolean monsterSkill, int delay, MobSkill mobskill) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        wp.writeInt(objectID);
        int mask = 0;
        for (MonsterStatus stat : stats.keySet()) {
                mask |= stat.getValue();
        }
        wp.writeInt(mask);
        for (Integer val : stats.values()) {
            wp.writeShort(val);
            if (monsterSkill) {
                wp.writeShort(mobskill.getSkillId());
                wp.writeShort(mobskill.getSkillLevel());
            } else {
                wp.writeInt(skill);
            }
            wp.writeShort(0); 
        }
        wp.writeShort(delay); 
        wp.write(1);
        return wp.getPacket();
    }
    
    public static OutPacket ApplyMonsterStatus(int objectID, MonsterStatusEffect mse, List<Integer> reflection) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        wp.writeInt(objectID);
        wp.writeLong(0);
        WriteIntMask(wp, mse.getStati());
        for (Map.Entry<MonsterStatus, Integer> stat : mse.getStati().entrySet()) {
           wp.writeShort(stat.getValue());
           if (mse.isMonsterSkill()) {
               wp.writeShort(mse.getMobSkill().getSkillId());
               wp.writeShort(mse.getMobSkill().getSkillLevel());
           } else {
               wp.writeInt(mse.getSkill().getId());
           }
           wp.writeShort(-1);
        }
        int size = mse.getStati().size();
        if (reflection != null) {
           for (Integer ref : reflection) {
               wp.writeInt(ref);
           }
           if (reflection.size() > 0) {
               size /= 2; 
           }
        }
        wp.write(size); 
        wp.writeInt(0);
        return wp.getPacket();
    }
    
    public static OutPacket CancelMonsterStatus(int objectID, Map<MonsterStatus, Integer> stats) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        wp.writeInt(objectID);
        int mask = 0;
        for (MonsterStatus stat : stats.keySet()) {
                mask |= stat.getValue();
        }
        wp.writeInt(mask);
        wp.write(1);
        return wp.getPacket();
    }

    public static OutPacket MobDamageMob(MapleMonster monster, int damage, boolean byMob) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        wp.writeInt(monster.getObjectId());
        wp.writeBool(byMob);
        if (damage > Integer.MAX_VALUE) {
            wp.writeInt(Integer.MAX_VALUE);
        } else {
            wp.writeInt(damage);
        }
        int remainingHp = monster.getHp() - damage;
        if (remainingHp < 0) {
            remainingHp = 0;
        }
        monster.setHp(remainingHp);
        wp.writeInt(remainingHp);
        wp.writeInt(monster.getMaxHp());
        return wp.getPacket();
    }  
    
    public static OutPacket ShowMagnet(int mobID, byte success) { 
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SHOW_MAGNET.getValue());
        wp.writeInt(mobID);
        wp.write(success);
        return wp.getPacket();
    }
    
    private static void WriteIntMask(WritingPacket wp, Map<MonsterStatus, Integer> stats) {
       int firstMask = 0;
       int secondMask = 0;
       for (MonsterStatus stat : stats.keySet()) {
           if (stat.isFirst()) {
               firstMask |= stat.getValue();
           } else {
               secondMask |= stat.getValue();
           }
       }
       wp.writeInt(firstMask);
       wp.writeInt(secondMask);
   }
}
