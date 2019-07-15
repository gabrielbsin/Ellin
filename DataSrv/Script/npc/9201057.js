///*
//    This file is part of the OdinMS Maple Story Server
//    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
//                       Matthias Butz <matze@odinms.de>
//                       Jan Christian Meyer <vimes@odinms.de>
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU Affero General Public License version 3
//    as published by the Free Software Foundation. You may not use, modify
//    or distribute this program under any other version of the
//    GNU Affero General Public License.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU Affero General Public License for more details.
//
//    You should have received a copy of the GNU Affero General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
//*/
//
///**
//-- Odin JavaScript --------------------------------------------------------------------------------
//	Bell - KC/NLC Subway Station(103000100/600010001), Waiting Room(600010002/600010004)
//-- By ---------------------------------------------------------------------------------------------
//	Information
//-- Version Info -----------------------------------------------------------------------------------
//	1.0 - First Version by Information
//	    - Thanks for Davidkun and Shogi for the information
//---------------------------------------------------------------------------------------------------
//**/
//
//var section;
//var msg = new Array("New Leaf City of Masteria","Kerning City of Victoria Island","Kerning City","New Leaf City");
//var ticket = new Array(4031711,4031713);
//var cost = 5000;
//var returnMap = new Array(103000100,600010001);
//
//function start() {
//	status = -1;
//	sw = cm.getEventManager("Subway");
//    action(1, 0, 0);
//}
//
//function action(mode, type, selection) {
//	if(mode == -1 || (mode == 0 && status ==0)) {
//		cm.dispose();
//	} else {
//		status++;
//		if(mode == 0) {
//			if(section == 2) {
//				cm.sendNext("Ok, aguarde !");
//			} else {
//				cm.sendOk("Voce deve ter alguns negocios para cuidar aqui, certo?");
//			}
//			cm.dispose();
//			return;
//		}
//	    if (status == 0) {
//			switch(cm.getPlayer().getMapId()) {
//				case 103000100:
//					section = 0;
//					break;
//				case 600010001:
//					section = 1;
//					break;
//				case 600010004:
//					section = 2;
//					break;
//				case 600010002:
//					section = 3;
//					break;
//				default:
//					cm.sendNext("Erro!");
//					cm.dispose();
//					break;
//			}
//			if(section < 2) {
//				cm.sendSimple("Ola, gostaria de comprar um bilhete para o metro?\r\n#L0##b"+msg[section]+"#l");
//			} else {
//				cm.sendYesNo("Voce quer voltar para a "+msg[section]+" estacao de metro agora?");
//			}
//		}
//		else if(status == 1) {
//			if(section < 2) {
//				cm.sendYesNo("A viagem ata "+msg[section]+" decola a cada 10 minutos, comecando na hora, e ele vai te custar #b"+cost+" mesos#k. Tem certeza de que quer comprar #b#t"+ticket[section]+"##k?");
//			} else {
//				section -= 2;
//				cm.warp(returnMap[section]);
//				cm.dispose();
//			}
//		}
//		else if(status == 2) {
//			if(cm.getMeso() < cost || !cm.canHold(ticket[section])) {
//				cm.sendNext("Tem certeza que voce tem #b"+cost+" mesos#k? Se assim for, entao peco que voce verifique o seu inventario, etc, e ver se ele esta cheio ou nao.");
//			}
//			else {
//				cm.gainItem(ticket[section],1);
//				cm.gainMeso(-cost);
//			}
//			cm.dispose();
//		}
//	}
//}

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

/*
        Author :        BubblesDev
        NPC Name:       Bell
        Description:    Subway ticket seller and taker off.
*/

function start() {
    if (cm.c.getPlayer().getMapId() == 103000100 || cm.c.getPlayer().getMapId() == 600010001)
        cm.sendYesNo("A viagem e para " + (cm.c.getPlayer().getMapId() == 103000100 ? "New Leaf City of Masteria" : "Kerning City of Victoria Island") + " vai lhe custar #b5000 mesos#k. Tem certeza de que deseja comprar um #b#t" + (4031711 + parseInt(cm.c.getPlayer().getMapId() / 300000000)) + "##k?");
    else if (cm.c.getPlayer().getMapId() == 600010002 || cm.c.getPlayer().getMapId() == 600010004)
        cm.sendYesNo("Voce quer sair antes que o trem comecar? Não havera reembolso.");
}

function action(mode, type, selection) {
    if(mode != 1){
        cm.dispose();
        return;
    }
    if (cm.c.getPlayer().getMapId() == 103000100 || cm.c.getPlayer().getMapId() == 600010001){
        if(cm.getMeso() >= 5000){
            cm.gainMeso(-5000);
            cm.gainItem(4031711 + parseInt(cm.c.getPlayer().getMapId() / 300000000), 1);
            cm.sendNext("Voce agora possui o ticket de viagem.");
        }else
            cm.sendNext("Voce nao possui mesos suficiente!");
    }else{
        cm.sendNext("Tudo bem, te vejo na proxima vez.");
        cm.warp(cm.c.getPlayer().getMapId() == 600010002 ? 600010001 : 103000100);
    }
    cm.dispose();
}