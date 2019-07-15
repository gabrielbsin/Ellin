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

package client.player.violation;

import client.player.Player;
import client.player.PlayerStringUtil;
import handling.world.service.BroadcastService;
import packet.creators.PacketCreator;
import tools.FileLogger;

public enum CheatingOffense {
    
    FASTATTACK(1, 60000, 300),
    MOVE_MONSTERS,
    POSSIBLE_VAC(1, 60000, 500),
    TUBI,
    PASSING_DAMAGE_CAP(10, 300000),
    INSANE_DAMAGE(10, 300000),
    LOWSTAT_DAMAGE(10, 300000),
    FAST_HP_REGEN,
    FAST_MP_REGEN(1, 60000, 500),
    SAME_DAMAGE(10, 300000, 20),
    ATTACK_WITHOUT_GETTING_HIT,
    HIGH_DAMAGE(10, 300000l),
    ATTACK_FARAWAY_MONSTER(5),
    HIGH_AVOID(5, 180000, 100),
    REGEN_HIGH_HP(50),
    REGEN_HIGH_MP(50),
    ITEMVAC(5),
    SHORT_ITEMVAC(2),
    USING_FARAWAY_PORTAL(30, 300000),
    SUMMON_HACK_MOBS((byte) 1, 300000),
    FAST_TAKE_DAMAGE(1),
    FAST_MOVE(1, 60000),
    HIGH_JUMP(1, 60000),
    MISMATCHING_BULLETCOUNT(50),
    ETC_EXPLOSION(50, 300000),
    FAST_SUMMON_ATTACK,
    ATTACKING_WHILE_DEAD(10, 300000),
    USING_UNAVAILABLE_ITEM(10, 300000),
    FAMING_SELF(10, 300000),  
    FAMING_UNDER_15(10, 300000),
    EXPLODING_NONEXISTANT,
    SUMMON_HACK,
    PACKET_EDIT,
    DISTANCE_HACK(10, 120 * 1000),
    WZ_EDIT,
    DAMAGE_HACK(15, 60 * 1000),
    HEAL_ATTACKING_UNDEAD(1, 60000, 5),
    COOLDOWN_HACK(10, 300000, 10),
    MOB_INSTANT_DEATH_HACK(10, 300000, 5);

    private final int points;
    private final long validityDuration;
    private final int autobancount;
    private boolean enabled = true;

    public int getPoints() {
        return points;
    }

    public long getValidityDuration() {
        return validityDuration;
    }

    public boolean shouldAutoban(int count) {
        if (autobancount == -1) {
            return false;
        }
        return count > autobancount;
    }

    public void setEnabled (boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled () {
        return enabled;
    }

    private CheatingOffense() {
        this(1);
    }

    private CheatingOffense(int points) {
        this(points, 60000);
    }

    private CheatingOffense(int points, long validityDuration) {
        this(points, validityDuration, -1);
    }

    private CheatingOffense(int points, long validityDuration, int autobancount) {
        this(points, validityDuration, autobancount, true);
    }

    private CheatingOffense(int points, long validityDuration, int autobancount, boolean enabled) {
        this.points = points;
        this.validityDuration = validityDuration;
        this.autobancount = autobancount;
        this.enabled = enabled;
    }
    
    public void cheatingSuspicious(Player p, String reason) {
        FileLogger.printError("cheatingWarn.txt", (p != null ? PlayerStringUtil.makeMapleReadable(p.getName()) : "") + " caused " + this.name() + " " + reason + "\r\n");
        if (p != null){
            return;
        }
        BroadcastService.broadcastGMMessage(PacketCreator.SendYellowTip((p != null ? PlayerStringUtil.makeMapleReadable(p.getName()) : "") + " caused " + this.name() + " " + reason));
        if (p != null) {
            p.announce(PacketCreator.EnableActions());
        }
    }
}