///*
//	This file is part of the OdinMS Maple Story Server
//    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
//					   Matthias Butz <matze@odinms.de>
//					   Jan Christian Meyer <vimes@odinms.de>
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU Affero General Public License as
//    published by the Free Software Foundation version 3 as published by
//    the Free Software Foundation. You may not use, modify or distribute
//    this program under any other version of the GNU Affero General Public
//    License.
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
//	 Kerning Ticket Gate - Subway Ticketing Booth(103000100)
//-- By ---------------------------------------------------------------------------------------------
//	Information
//-- Version Info -----------------------------------------------------------------------------------
//	1.1 - Added function to NLC [Information]
//	1.0 - First Version by Information
//---------------------------------------------------------------------------------------------------
//**/
//
//var itemid = new Array(9031711, 9031711, 9031711, 4031711);
//var mapid = new Array(103000900,103000903,103000906,600010004);
//var mapname = new Array("Construcao Site B1", "Construcao Site B2", "Construcao Site B3","New Leafe city (Normal)");
//var menu;
//var sw;
//
//function start() {
//	status = -1;
//	sw = cm.getEventManager("Subway");
//	action(1, 0, 0);
//}
//
//function action(mode, type, selection) {
//	if (mode == -1 || (mode == 0 && status ==1)) {
//		cm.dispose();
//	} else {
//		if (mode == 0) {
//			cm.sendNext("You must have some business to take care of here, right?");
//			cm.dispose();
//			return;
//		}
//		if (mode == 1)
//			status++;
//		if (status == 0) {
//			if (cm.haveItem(itemid[0]) || cm.haveItem(itemid[1]) || cm.haveItem(itemid[2]) || cm.haveItem(itemid[3])) {
//				status = 1;
//			} else {
//				cm.sendNext("Voce deve ter alguns negocios para cuidar aqui, certo?");
//				cm.dispose();
//			}
//		} if (status == 1) {
//			menu = "Aqui esta o leitor de bilhetes. Voce sera levado imediatamente. Qual bilhete gostaria de usar?\r\n";
//			for(i=0; i < itemid.length; i++) {
//				if(cm.haveItem(itemid[i])) {
//					menu += "#L"+i+"##b"+mapname[i]+"#k#l\r\n";
//				}
//			}
//			cm.sendSimple(menu);
//		} if (status == 2) {
//			section = selection;
//			if(section < 3) {
//				cm.gainItem(itemid[selection],-1);
//				cm.warp(mapid[selection]);
//				cm.dispose();
//			}
//			else {
//				if(sw == null) {
//					cm.sendNext("Evento de erro, reinicie o servidor para solucao!");
//					cm.dispose();
//				} else if(sw.getProperty("entry").equals("true")) {
//					cm.sendYesNo("Parece que nao ha muito espaco para esse passeio. Por favor, tenham o seu bilhete pronto para que eu possa deixa-lo ir. A viagem vai ser longa, mas voce vai chegar ao seu destino bem. O que voce acha? Voce quer ficar com esse passeio?");
//				} else if(sw.getProperty("entry").equals("false") && sw.getProperty("docked").equals("true")) {
//					cm.sendNext("O metro esta se preparando para a decolagem. Sinto muito, mas voce vai ter que pegar o proximo passeio. O cronograma de passeio esta disponível atraves do lanterninha no estande da emissao do bilhete.");
//					cm.dispose();
//				} else {
//					cm.sendNext("Comecaremos embarque em um minuto antes da decolagem. Por favor, seja paciente e espere alguns minutos. Esteja ciente de que o metro vai decolar na hora certa, e parar de receber bilhetes 1 minuto antes de que, por isso, certifique-se de estar aqui na hora certa.");
//					cm.dispose();
//				}
//			}
//		} if (status == 3) {
//			cm.gainItem(itemid[section],-1);
//			cm.warp(mapid[section]);
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
status = -1;
close = false;
oldSelection = -1;

function start() {
    var text = "Ola, eu sou o leitor de ticket's.";
    if (cm.haveItem(4031711) || cm.haveItem(4031036) || cm.haveItem(4031037) || cm.haveItem(4031038))
        text += " Voce sera levado imediatamente, que ticket quer usar?#b";
    else
        close = true;
    if (cm.haveItem(4031711))
        text += "\r\n#L3##t4031711#";
    for (var i = 0; i < 3; i++)
        if (cm.haveItem(4031036 + i))
            text += "\r\n#L" + i + "##t" + (4031036 + i) +"#";
    if (close) {
        cm.sendOk(text);
        cm.dispose();
    } else
        cm.sendSimple(text);
}

function action(mode, type, selection) {
    status++;
    if (mode != 1) {
        if(mode == 0)
            cm.sendNext("Voce deve ter algum negocio para cuidar aqui, certo?");
        cm.dispose();
        return;
    }
    if (status == 0) {
        if (selection == 3) {
                cm.sendYesNo("Me parece que ha muito espaco para esse passeio. Por favor, tenha o seu bilhete pronto para que eu possa deixa-lo entrar. A viagem pode ser longa, mas voce vai chegar ao seu destino muito bem. O que voce acha? Voce quer entrar nesta viagem?");
        }
        oldSelection = selection;
    } else if (status == 1) {
        if (oldSelection == 3) {
            cm.gainItem(4031711, -1);
            cm.warp(600010001, 0);
        } else {
            cm.gainItem(4031036 + oldSelection, -1);
            cm.warp(103000900 + (oldSelection * 3), 0);
        }
        cm.dispose();
    }
}