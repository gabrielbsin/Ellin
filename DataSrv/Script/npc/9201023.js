////script by Alcandon
//
//function start() {
//    status = -1;
//    action(1, 0, 0);
//}
//
//function action(mode, type, selection) {
//	if (mode == -1) {
//		cm.dispose();
//	} else {
//		if (mode == 0 && status == 0) {
//			cm.dispose();
//			return;
//		}
//		if (mode == 1)
//			status++;
//		else
//			status--;
//		if (status == 0) {
//			cm.sendSimple ("                        #e<LeaderMS Ocupacoes>#n\r\n\r\nOla #e#h ##n,\r\nEu sou o #eSistema de Ocupacao#n do LeaderMS.\r\nSua ocupacao atual e (#b#e" + cm.getPlayer().getOccupation() + "#k#n).\r\n#r#eAlem disso, por favor leia o grafico nivel de ocupacao.#n #k\r\n#L0#Eu gostaria de subir de nivel um nivel de ocupacao!\r\n#L1#Eu ainda nao tenho certeza.\r\n#L3#Veja a tabela de nivel de ocupacao!");
//				 } else if (selection == 0) {
//                 cm.sendSimple ("O seu nivel de ocupacao atual e (#b#e" + cm.getPlayer().getOccupation() + "#k#n).\r\nPara mais informacoes, leia o grafico nivel de ocupacao. Gostaria de avancar para?" +
//                 "#k\r\n#L45#Nivel (1) - #b#eAlpha#k#n #r#e[Lvl. - 30]#k#n" +
//                 "#k\r\n#L46#Nivel (2) #b#ePlatinium#k#n #r#e[Lvl. - 50]#k#n" +
//                 "#k\r\n#L47#Nivel (3) #b#eSenior#k#n #r#e[Lvl. - 70]#k#n"); 
//                                 } else if (selection == 1) {
//                                     cm.dispose();
//				 } else if (selection == 45) {
//				  if (cm.getLevel() >= 29 && cm.HasOccupation0() && cm.getPQPoints() >= 200 && cm.getLeaderPoints() >= 100)  {
//				  cm.changeOccupationById(100);
//                                  cm.gainLeaderPoints(-100);
//                                  cm.gainPQPoints(-200);
//				  cm.sendOk("Parabens, voce agora e um Alpha.");
//				  cm.dispose();
//				  } else {
//				  cm.sendOk("Voce nao e um nivel suficientemente ou elevado para ser um #bAlpha#k, ou voce ja e um #bAlpha#k ou superior a #bAlpha#k. Por favor, leia o grafico de nivel de ocupacao.")
//				  cm.dispose();
//				  }
//				} else if (selection == 46) {
//				if (cm.getLevel() >= 50 && cm.HasOccupation1() && cm.getPQPoints() >= 400 && cm.getLeaderPoints() >= 250)  {
//				  cm.changeOccupationById(110);
//                                  cm.gainLeaderPoints(-250);
//                                  cm.gainPQPoints(-400);
//				  cm.sendOk("Parabens, voce agora e um Platinium.");
//				  cm.dispose();
//				  } else {
//				  cm.sendOk("Voce nao e um nivel suficientemente ou elevado para ser um #bPlatinium#k, ou voce ja e um #bPlatinium#k ou superior a #bPlatinium#k, ou o seu nivel de ocupacao e maneira atras. Por favor, leia o grafico de nivel de ocupacao.");
//				  cm.dispose();
//				  }
//				} else if (selection == 47) {
//				if (cm.getLevel() >= 70 && cm.HasOccupation2() && cm.getPQPoints() >= 600 && cm.getLeaderPoints() >= 800)  {
//				  cm.changeOccupationById(120);
//                                  cm.gainLeaderPoints(-800);
//                                  cm.gainPQPoints(-600);
//				  cm.sendOk("Parabens, voce agora e um Senior.");
//				  cm.dispose();
//				  } else {
//				  cm.sendOk("Voce nao e um nivel suficientemente ou elevado para ser um #bSenior#k, ou voce ja e um #bSenior#k. Por favor, leia o grafico de nivel de ocupacao.");
//				  cm.dispose();
//				  }
//				} else if (selection == 3) {
//				  cm.sendNext("Com o aumento de nivel de #eocupacao#n, voce tera diversas vantagens, podera participar de quest's dentre outros.\r\nDe uma olhada na tabela #eNivel de Ocupacao#n, e se voce tem os seguintes requisitos para a proxima ocupacao, por favor, fale comigo de novo.\r\n \r\nNivel (0) - #r#eIniciante#k#n\r\n#eRequisitos#n - Nenhum\r\n\r\nNivel (1) - #r#eAlpha#n#k\r\n#eRequisitos#n - #b[Lvl. - 30/Q.Points - 200/LeaderPoints - 100]#k\r\n\r\nNivel (2) - #r#ePlatinium#k#n\r\n#eRequisitos#n - #b[Lvl. - 50/Q.Points - 400/LeaderPoints - 250]#k\r\n\r\nNivel (3) - #r#eSenior#k#n\r\n#eRequisitos#n - #b[Lvl. - 70/Q.Points - 600/LeaderPoints - 800]#k\r\n");
//				  cm.dispose();
//				}
//		}
//	}


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
 *9201023 - Nana(K)
 *@author Jvlaple
 */
 
function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1)
            status++;
        else
            status--;
        if (cm.getPlayer().getMarriageQuestLevel() == 1 || cm.getPlayer().getMarriageQuestLevel() == 52) {
            if (!cm.haveItem(4000015, 40)) {
                if (status == 0) {
                    cm.sendNext("Hey, voce parece que precisade provas de amor? Eu posso leva-las para voce.");
                } else if (status == 1) {
                    cm.sendNext("Tudo que voce tem a fazer e trazer me 40 #bHorned Mushroom Caps#k.");
                    cm.dispose();
                }
            } else {
                if (status == 0) {
                    cm.sendNext("Uau, voce foi rapido ! Aqui a prova de amor...");
                    cm.gainItem(4000015, -40)
                    cm.gainItem(4031367, 1);
                    cm.dispose();
                }
            }
        } else {
            cm.sendOk("Oi, eu sou Nana a fada amor ... e voce quem e?");
            cm.dispose();
        }
    }
}
	