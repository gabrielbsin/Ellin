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

package client.player.buffs;

import java.io.Serializable;

public enum Disease implements Serializable {

    SLOW(0x1, 2, 126),
    SEDUCE(0x80, 2, 128),
    STUN(0x2000000000000L, 1, 123),
    POISON(0x4000000000000L, 1, 125),
    SEAL(0x8000000000000L, 1, 120),
    DARKNESS(0x10000000000000L, 1, 121),
    WEAKEN(0x4000000000000000L, 1, 122),
    CURSE(0x8000000000000000L, 1, 124),
    CONFUSE(0x80000, 1, 132);

    private static final long serialVersionUID = 0L;
    private final long i;
    private boolean first;
    private int disease;
    private int firsta;
    
   private Disease(long i) {
        this.i = i;
        first = false;
    }

    private Disease(long i, boolean first, int disease) {
        this.i = i;
        this.first = first;
        this.disease = disease;
    }
    
    private Disease(long i, int first, int disease) {
        this.i = i;
        this.firsta = first;
        this.disease = disease;
    }
    
    public boolean isFirst() {
        return first;
    }

    public long getValue() {
        return i;
    }
    
    public int getDisease() {
        return disease;
    }
   
    public static Disease getType(int skill) {
        switch (skill) {
            case 120:
                return SEAL;
            case 123:
                return STUN;
            case 128:
                return SEDUCE;
            case 125:
                return POISON;
            case 121:
                return DARKNESS;
            case 122:
                return WEAKEN;
            case 124:
                return CURSE;
            case 132:
                return CONFUSE;
            default:
                return null;
        }
    }
}