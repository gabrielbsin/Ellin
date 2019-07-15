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
	Shalon - Ticketing Usher
-- By ---------------------------------------------------------------------------------------------
	Information
-- Version Info -----------------------------------------------------------------------------------
	1.1 - some fixes
	1.0 - First Version by Information
---------------------------------------------------------------------------------------------------
**/

var cost = 20000;
var ap;

function start() {
	ap = cm.getEventManager("AirPlane");
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if(mode == -1 || (mode == 0 && status == 0)) {
		cm.dispose();
		return;
	}
	if(mode == 1) {
		status++;
	}
	if(mode == 0 && menu == 0) {
		cm.sendNext("I am here for a long time. Please talk to me again when you change your mind.");
		cm.dispose();
	}
	if(mode == 0 && menu == 1) {
		cm.sendOk("Please confirm the departure time you wish to leave. Thank you.");
		cm.dispose();
	}
	if(status == 0) {
		cm.sendSimple("Hello there~ I am #p"+cm.getNpc()+"# from Singapore Airport. I will assist you in getting back to #m103000000# in no time. How can i help you?\r\n#L0##bI would like to buy a plane ticket to #m103000000##k#l\r\n#L1##bLet me go in to the departure point.#k#l");
	} else if(status == 1) {
		menu = selection;
		if(menu == 0) {
			cm.sendYesNo("The ticket will cost you 20,000 mesos. Will you purchase the ticket?");
		} else if(menu == 1) {
			cm.sendYesNo("Would you like to go in now? You will lose your ticket once you go in~ Thank you for choosing Wizet Airline.");
		}
	} else if(status == 2) {
		if(menu == 0) {
			if(!cm.canHold(4031732) || cm.getPlayer().getMeso() < cost) {
				cm.sendOk("I don't think you have enough meso or empty slot in your ETC inventory. Please check and talk to me again.");
			} else {
				cm.gainMeso(-cost);
				cm.gainItem(4031732, 1);
			}
			cm.dispose();
		} else if(menu == 1) {
			if(!cm.haveItem(4031732)) {
				cm.sendNext("Please do purchase the ticket first. Thank you~");
			} else if(ap.getProperty("entry").equals("true")) {
				cm.sendNext("We are sorry but the gate is closed 1 minute before the departure.");
			} else {
				cm.gainItem(4031732,-1);
				cm.warp(540010001, 0);
			}
			cm.dispose();
		}
	}
}
