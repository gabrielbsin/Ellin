/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.player.Player;
import client.player.skills.PlayerSkill;
import client.player.skills.PlayerSkillFactory;
import java.util.List;
import java.util.Map;
import server.MapleStatEffect;

/**
 *
 * @author GabrielSin
 */
 public class AttackInfo {

    public int numAttacked;
    public int skill;
    public int stance;
    public int direction;
    public int charge;
    public int numDamage;
    public int numAttackedAndDamage;
    public int display;
    public int speed = 4;
    public long attackTime;
    public boolean ranged;
    public boolean magic;
    public boolean isHH = false;
    public boolean isTempest = false;
    public Map<Integer, List<Integer>> allDamage;


    public final MapleStatEffect getAttackEffect(Player p, PlayerSkill theSkill) {
        PlayerSkill mySkill = theSkill;
        if (mySkill == null) {
            mySkill = PlayerSkillFactory.getSkill(skill);
        }
        int skillLevel = p.getSkillLevel(mySkill);
        if (skillLevel == 0) {
            return null;
        }
        return mySkill.getEffect(skillLevel);
    }

    public MapleStatEffect getAttackEffect(Player chr) {
        return getAttackEffect(chr, null);
    }
}
