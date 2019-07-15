/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
					   Matthias Butz <matze@odinms.de>
					   Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/**
-- Odin JavaScript --------------------------------------------------------------------------------
	Rosey - Before the Departure Orbis & Ludibrium(220000111 & 220000122)
-- By ---------------------------------------------------------------------------------------------
	Information
-- Version Info -----------------------------------------------------------------------------------
	1.3 - Add missing return statement [Thanks sadiq for the bug, fix by Information]
	1.2 - Replace function to support latest [Information]
	1.1 - Fix wrong placed statement [Information]
	1.0 - First Version by Information
---------------------------------------------------------------------------------------------------
**/

importPackage(Packages.client);

function start() {
	status = -1;
	tm = cm.getEventManager("Trains");
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if(mode == -1) {
		cm.dispose();
		return;
	} else {
		status++;
		if(mode == 0) {
			cm.sendOk("Voce vai chegar ao seu destino bem. Va em frente e falar com outras pessoas, e antes que voce perceba, voce vai estar la!");
			cm.dispose();
			return;
		}
		if(status == 0) {
			cm.sendYesNo("Voce quer deixar a sala de espera? Voce pode, mas o bilhete nao e reembolsavel. Tem certeza de que ainda quer sair desta sala?");
		} else if(status == 1) {
			if(cm.getPlayer().getMapId() == 220000111)
				cm.warp(220000110, 0);
			else
				cm.warp(220000121, 0);
			cm.dispose();
		}
	}
}
