/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client.player.skills;

/**
 * 
 * @author GabrielSin
 */
public class PlayerSkillEntry {

    public int skillevel;
    public int masterlevel;
    public long expiration;

    public PlayerSkillEntry(int skillevel, int masterlevel) {
        this.skillevel = skillevel;
        this.masterlevel = masterlevel;
    }

    public PlayerSkillEntry(byte skillevel, int masterlevel, long expiration) {
        this.skillevel = skillevel;
        this.masterlevel = masterlevel;
        this.expiration = expiration;
    }

    @Override
    public String toString() {
        return skillevel + ":" + masterlevel;
    }
}
