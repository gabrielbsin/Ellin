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

package server.life.status;

import java.io.Serializable;

public enum MonsterStatus implements Serializable {

    WATK(0x00000001),
    WDEF(0x00000002),
    MATK(0x00000004),
    MDEF(0x00000008),
    ACC(0x00000010),
    AVOID(0x00000020),
    SPEED(0x00000040),
    STUN(0x00000080), 
    FREEZE(0x00000100),
    POISON(0x00000200),
    SEAL(0x00000400),
    TAUNT_1(0x00004000),
    TAUNT_2(0x00008000),
    WEAPON_ATTACK_UP(0x1000),
    WEAPON_DEFENSE_UP(0x2000),
    MAGIC_ATTACK_UP(0x4000),
    MAGIC_DEFENSE_UP(0x8000),
    DOOM(0x10000),
    SHADOW_WEB(0x20000),
    WEAPON_IMMUNITY(0x40000),
    MAGIC_IMMUNITY(0x80000),
    NINJA_AMBUSH(0x00400000),
    
    INERTMOB(0x10000000);
        
    static final long serialVersionUID = 0L;
    private final int i;
    private final boolean first;

    private MonsterStatus(int i) {
        this.i = i;
        this.first = false;
    }

    private MonsterStatus(int i, boolean first) {
        this.i = i;
        this.first = first;
    }

    public boolean isFirst() {
        return first;
    }

    public int getValue() {
        return i;
    }
}
