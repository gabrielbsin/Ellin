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

/**
-- Odin JavaScript --------------------------------------------------------------------------------
	Jake - Victoria Road : Subway Ticketing Booth (103000100)
-- By ---------------------------------------------------------------------------------------------
	Xterminator
-- Version Info -----------------------------------------------------------------------------------
	1.1 - Optimize statement [Information]
	1.0 - First Version by Xterminator
---------------------------------------------------------------------------------------------------
**/

var meso = new Array(500, 1200, 2000);
var item = new Array(4031036, 4031037, 4031038);
var selector;
var menu = "";

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
	if (status == 0 && mode == 0) {
		cm.dispose();
		return;
	} else if (status == 1 && mode == 0) {
		cm.sendNext("You can enter the premise once you have bought the ticket. I heard there are strange devices in there everywhere but in the end, rare precious items await you. So let me know if you ever decide to change your mind.");
		cm.dispose();
		return;
	}
	if (mode == 1)
		status++;
	else
		status--;
	if (status == 0) {
		if (cm.getLevel() <= 19) {
			cm.sendNext("You can enter the premise once you have bought the ticket; however it doesn't seem like you can enter here. There are foreign devices underground that may be too much for you to handle, so please train yourself, be prepared, and then come back.");
			cm.dispose();
		} else {
			for(var x=0; x < 3; x++) {
				if (cm.getLevel() >= 20 && cm.getLevel() <= 29) {
					menu += "\r\n#L" + x + "##bConstruction Site B" + x + "#k#l";
					break;
				} else if (cm.getLevel() >= 30 && cm.getLevel() <= 39 && x < 2) {
					menu += "\r\n#L" + x + "##bConstruction Site B" + x + "#k#l";
				} else {
					menu += "\r\n#L" + x + "##bConstruction Site B" + x + "#k#l";
				}
			}
			cm.sendSimple("You must purchase the ticket to enter. Once you have made the purchase, you can enter through The Ticket Gate on the right. What would you like to buy?" + menu);
		}
	} else if (status == 1) {
		selector = selection;
		selection += 1;
		cm.sendYesNo("Will you purchase the ticket to #bConstruction Site B" + selection + "#k? It'll cost you " + meso[selector] + " mesos. Before making the purchase, please make sure you have an empty slot on your etc. inventory.");
	} else if (status == 2) {
		if (cm.getMeso() < meso[selector]) {
			cm.sendNext("Are you lacking mesos? Check and see if you have an empty slot on your etc. inventory or not.");
			cm.dispose();
		} else {
			if (selector == 0) {
				cm.sendNext("You can insert the ticket in The Ticket Gate. I heard Area 1 has some precious items available but with so many traps all over the place most come back out early. Wishing you the best of luck.");
			} else if (selector == 1) {
				cm.sendNext("You can insert the ticket in The Ticket Gate. I heard Area 2 has rare, precious items available but with so many traps all over the place most come back out early. Please be safe.");
			} else {
				cm.sendNext("You can insert the ticket in The Ticket Gate. I heard Area 3 has very rare, very precious items available but with so many traps all over the place most come back out early. Wishing you the best of luck.");
			}
				cm.gainMeso(-meso[selector]);
				cm.gainItem(item[selector], 1);
				cm.dispose();
			}
		}
	}
}