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
var status = -1;

function start() {
    if (cm.haveItem(4031508, 5) && cm.haveItem(4031507,5)) {
        cm.sendNext("Wow ~ Você conseguiu coletar 5 de cada #b#t4031508##k e #b#t4031507##k. Ok então, eu vou mandar voce para o Zoo. Por favor, fale comigo de novo quando você chegar lá.");
    } else {
        cm.sendYesNo("Vocr nao tenham completou os requisitos. Tem certeza de que deseja sair ?");
    }
}

function action(mode, type, selection){
    status++;
    var eim = cm.getPlayer().getEventInstance();
    var party = cm.getPlayer().getEventInstance().getPlayers();
    if (mode != 1) {
        if (status > 0)
           for (var outt = 0; outt <party.size(); outt++){//Kick everyone out =D
		 var exitMapz = eim.getMapInstance(230000003);
		 party.get(outt).changeMap(exitMapz, exitMapz.getPortal(0));
		 eim.unregisterPlayer(party.get(outt));
	    }
        eim.liberaEntrada();
        cm.dispose();
        return;
    }
    if (status == 0)
        if (cm.haveItem(4031508, 5) && cm.haveItem(4031507, 5)) {
            for (var outt = 0; outt <party.size(); outt++){//Kick everyone out =D
		 var exitMapz = eim.getMapInstance(230000003);
		 party.get(outt).changeMap(exitMapz, exitMapz.getPortal(0));
		 eim.unregisterPlayer(party.get(outt));
	    }
            eim.liberaEntrada();
            cm.dispose();
        } else {
            for (var outt = 0; outt <party.size(); outt++){//Kick everyone out =D
		 var exitMapz = eim.getMapInstance(230000003);
		 party.get(outt).changeMap(exitMapz, exitMapz.getPortal(0));
		 eim.unregisterPlayer(party.get(outt));
	    } 
            eim.liberaEntrada();
            cm.dispose();
        }
    else {
        for (var outt = 0; outt <party.size(); outt++){//Kick everyone out =D
		 var exitMapz = eim.getMapInstance(230000003);
		 party.get(outt).changeMap(exitMapz, exitMapz.getPortal(0));
		 eim.unregisterPlayer(party.get(outt));
	}
        eim.liberaEntrada();
        cm.dispose();
    }
}