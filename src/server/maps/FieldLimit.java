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
package server.maps;

public enum FieldLimit {
    
    JUMP(0x01),
    MOVEMENTSKILLS(0x02),
    SUMMON(0x04),
    DOOR(0x08),
    CHANGECHANNEL(0x10),
    REGULAREXPLOSS(0x20),
    CANNOTVIPROCK(0x40),
    CANNOTMINIGAME(0x80),
    SPECIFIED_PORTAL_SCROLL(0x100),
    CANNOTUSEMOUNTS(0x200),
    STAT_CHANGING_POTIONS(0x400), 
    CANTSWITCHPARTYLEADER(0x800),
    CANNOTUSEPOTION(0x1000),
    CANTWEDDINGINVITE(0x2000),
    CASHWEATHER(0x4000),
    CANNOTUSEPET(0x8000), 
    CANTUSEMACRO(0x10000), 
    CANNOTJUMPDOWN(0x20000),
    NOEXPDECREASE(0x80000),
    NOFALLDAMAGE(0x100000),
    SHOPS(0x200000),
    CANTDROP(0x400000);
    
    private final long i;

    private FieldLimit(long i) {
        this.i = i;
    }

    public long getValue() {
        return i;
    }

    public boolean check(int fieldlimit) {
        return (fieldlimit & i) == i;
    }
}
