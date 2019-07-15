/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.Client;
import static handling.channel.handler.MovementParse.parseMovement;
import static handling.channel.handler.MovementParse.updatePosition;
import handling.mina.PacketReader;
import handling.world.service.BroadcastService;
import java.awt.Point;
import java.util.List;
import java.util.Random;
import packet.creators.MonsterPackets;
import packet.creators.PacketCreator;
import client.player.Player;
import client.player.PlayerJob;
import client.player.skills.PlayerSkillFactory;
import constants.SkillConstants.Gunslinger;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.Field;
import server.maps.object.FieldObjectType;
import server.movement.LifeMovementFragment;
import tools.Pair;

/**
 *
 * @author GabrielSin
 */
public class MobHandler {

    public static void MoveMonster(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        if (p == null || p.getMap() == null) {
            return;  
        }
        
        int objectid = packet.readInt();
        short moveid = packet.readShort();
        
        final MapleMonster mmo = p.getMap().getMonsterByOid(objectid);
        if (mmo == null || mmo.getType() != FieldObjectType.MONSTER) {
           return;
	}
        
        final MapleMonster monster = (MapleMonster) mmo;
        
        int skillByte = packet.readByte();
        int skill = packet.readByte();
        int skillOne = packet.readByte() & 0xFF;
        int skillTwo=  packet.readByte();
        int skillThree = packet.readByte();
        packet.readByte();

        MobSkill toUse = null;
        Random rand = new Random();

        if (skillByte == 1 && monster.getNoSkills() > 0) {
            int random = rand.nextInt(monster.getNoSkills());
            Pair<Integer, Integer> skillToUse = monster.getSkills().get(random);
            toUse = MobSkillFactory.getMobSkill(skillToUse.getLeft(), skillToUse.getRight());
            int percHpLeft = (int) ((monster.getHp() / monster.getMaxHp()) * 100);
            if (toUse.getHP() < percHpLeft || !monster.canUseSkill(toUse)) {
                toUse = null;
            }
        }

        if (skillOne >= 100 && skillOne <= 200 && monster.hasSkill(skillOne, skillTwo)) {
            MobSkill skillData = MobSkillFactory.getMobSkill(skillOne, skillTwo);
            if (skillData != null && monster.canUseSkill(skillData)) {
                skillData.applyEffect(c.getPlayer(), monster, true);
            }
        }

        packet.readByte();
        packet.readInt(); 
        Point startPos = new Point(packet.readShort(), packet.readShort());

        List<LifeMovementFragment> mov = null;
        try {
            mov = parseMovement(packet);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("AIOBE Type2:\n" + packet.toString());
            return;
        }

        if (monster.getController() != p) {
            if (monster.isAttackedBy(p)) { 
                monster.switchController(p, true);
            } else {
                return;
            }
        } else {
            if (skill == -1 && monster.isControllerKnowsAboutAggro() && !monster.isMobile() && !monster.isFirstAttack()) {
                monster.setControllerHasAggro(false);
                monster.setControllerKnowsAboutAggro(false);
            }
        }
        
        boolean aggro = monster.controllerHasAggro();

        if (toUse != null) {
            c.getSession().write(MonsterPackets.MoveMonsterResponse(objectid, moveid, monster.getMp(), aggro, toUse.getSkillId(), toUse.getSkillLevel()));
        } else {
            c.getSession().write(MonsterPackets.MoveMonsterResponse(objectid, moveid, monster.getMp(), aggro));
        }

        if (aggro) {
            monster.setControllerKnowsAboutAggro(true);
        }

        if (mov != null) {
            if (packet.available() != 9) {
                BroadcastService.broadcastGMMessage(PacketCreator.ServerNotice(5, c.getPlayer().getName() + " is using dupex monster vaccum."));
                c.getSession().close();
                return;
            }

            p.getMap().broadcastMessage(c.getPlayer(), MonsterPackets.MoveMonster(skillByte, skill, skillOne, skillTwo, skillThree, objectid, startPos, mov), monster.getPosition());
            updatePosition(mov, monster, -1);
            p.getMap().moveMonster(monster, monster.getPosition());
            
            p.getCheatTracker().checkMoveMonster(monster.getPosition());
        }
    }  

    public static void AutoAggro(PacketReader slea, Client c) {
        Player p = c.getPlayer();
        if (p == null || p.getMap() == null || p.isHidden()) { 
            return;
        }
        
        final MapleMonster monster = p.getMap().getMonsterByOid(slea.readInt());
        
        if (monster != null && monster.getController() != null) {
            if (!monster.controllerHasAggro()) {
                if (p.getMap().getCharacterById(monster.getController().getId()) == null) {
                    monster.switchController(c.getPlayer(), true);
                } else {
                    monster.switchController(monster.getController(), true);
                }
            } else {
                if (p.getMap().getCharacterById(monster.getController().getId()) == null) {
                    monster.switchController(c.getPlayer(), true);
                }
            }
        } else if (monster != null && monster.getController() == null) {
            monster.switchController(c.getPlayer(), true);
        }
    }

    public static void MonsterBomb(PacketReader slea, Client c) {
        Player p = c.getPlayer(); 
        final MapleMonster monster = p.getMap().getMonsterByOid(slea.readInt());
        if (monster == null || p.getMap() == null || !p.isAlive() || p.isHidden()) {
            return;
        }
        if (monster.getStats().selfDestruction().getAction() == 2) {
            monster.getMap().broadcastMessage(MonsterPackets.KillMonster(monster.getObjectId(), 4));
            p.getMap().removeMapObject(monster);
        }
    }
    
    public static void FriendlyDamage(PacketReader packet, Client c) {
        MapleMonster attacker = c.getPlayer().getMap().getMonsterByOid(packet.readInt());
        packet.readInt(); 
        MapleMonster attacked = c.getPlayer().getMap().getMonsterByOid(packet.readInt());

        if ((attacker != null) && (attacked != null) && (attacked.getStats().isFriendly())) {
    
            int damage = attacker.getStats().getPADamage() + attacker.getStats().getPDDamage() - 1;

            if (attacked.getHp() - damage < 1) { 
                if (attacked.getId() == 9300102) {
                    attacked.getMap().broadcastMessage(PacketCreator.ServerNotice(6, "The Watch Hog has been injured by the aliens. Better luck next time..."));
                } else if (attacked.getId() == 9300061) {  //moon bunny
                    attacked.getMap().broadcastMessage(PacketCreator.ServerNotice(6, "The Moon Bunny went home because he was sick."));
                }
                c.getPlayer().getMap().killFriendlies(attacked);
            } else {
                if (attacked.getId() == 9300061) {
                    Field map = c.getPlayer().getEventInstance().getMapInstance(attacked.getMap().getId());
                    map.addBunnyHit();
                }
            }
            
            c.getPlayer().getMap().broadcastMessage(MonsterPackets.MobDamageMob(attacked, damage, true));
            c.announce(PacketCreator.EnableActions());
        }
    }
    
    /*
    * @author GabrielSin
    * Ellin MapleStory
    * @skillid = Gunslinger, grenade
    */
    public static void GrenadeEffect(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        Point position = new Point(packet.readInt(), packet.readInt());
        int keyDown = packet.readInt();
        
        int skillId = Gunslinger.Grenade;
        if (PlayerSkillFactory.getSkill(skillId) != null || c.getPlayer().getSkillLevel(skillId) > 0) {
            p.getMap().broadcastMessage(p, PacketCreator.ThrowGrenade(p.getId(), position, keyDown, Gunslinger.Grenade, p.getSkillLevel(Gunslinger.Grenade)), position);
        }
    }
}
