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
	Mue - Leafre Ticketing Booth(240000100)
-- By ---------------------------------------------------------------------------------------------
	Information
-- Version Info -----------------------------------------------------------------------------------
	1.1 - Price like GMS [sadiq]
	1.0 - First Version by Information
---------------------------------------------------------------------------------------------------
**/

var cost = 30000;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if(mode == -1) {
		cm.dispose();
	} else {
		if(mode == 1) {
			status++;
		}
		if(mode == 0) {
			cm.sendNext("Voce deve ter alguns negocios para cuidar aqui, certo?");
			cm.dispose();
			return;
		}
		if(status == 0) {
			cm.sendYesNo("Ola, sou o responsavel pela venda de bilhetes para o passeio de barco para a Estacao de Orbis Ossyria.  A viagem ate vai te custar #b"+cost+" mesos#k. Tem certeza de que quer comprar #b#t4031045##k?");
		} else if(status == 1) {
			if(cm.getPlayer().getMeso() >= cost && cm.canHold(4031045)) {
				cm.gainItem(4031045,1);
				cm.gainMeso(-cost);
			} else {
				cm.sendOk("Tem certeza que voce tem #b"+cost+" mesos#k? Se assim for, entao peco que voce verifique o seu inventario, etc, e ver se ele esta cheio ou nao.");
			}
			cm.dispose();
		}
	}
}
