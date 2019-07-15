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

//Spindle

var status = 0;
var minLevel = 100;
var maxLevel = 200;
var minPlayers = 6;
var maxPlayers = 6;

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
		if (mode == 1) {
			status++;
		} else if (mode == 0 && status == 2) {
			cm.sendOk("Você está assustado? Eu também estou.");
			cm.dispose();
			return;
		} else {
			status--;
		}
		if (cm.getPlayer().getEventInstance() == null) {
			if (status == 0) {
				cm.sendNext("Recentemente, houve um ataque ao Setor #bOmega#k. Todos os chefes do mundo Ellin entraram nesta dimensão, tornaram-se muito mais fortes, tem mais HP e estão invadindo Setor #bOmega#k. Precisamos de uma equipe corajosa para detê-los.");
			} else if (status == 1) {
				cm.sendNextPrev("Esses monstros vêm com o poder da #bOmegaPQ#k, um lendário poder que vem com a armadura que vestem.");
			}else if (status == 2) {
				cm.sendYesNo("Você deseja salvar o #bSetor Omega#k?");
			}else if (status == 3) {
				// Slate has no preamble, directly checks if you're in a party
				if (cm.getParty() == null) { // no party
					cm.sendOk("Por favor, fale comigo de novo depois de ter formado um grupo.");
					cm.dispose();
	                                return;
				}
				if (!cm.isLeader()) { // not party leader
					cm.sendOk("Por favor, fale para seu líder do grupo para falar comigo.");
					cm.dispose();
				} else {
					// Check teh partyy
					var party = cm.getParty().getMembers();
					var mapId = cm.getPlayer().getMapId();
					var next = true;
					var levelValid = 0;
					var inMap = 0;
					// Temp removal for testing
					if (party.size() < minPlayers || party.size() > maxPlayers) 
						next = false;
					else {
						for (var i = 0; i < party.size() && next; i++) {
							if ((party.get(i).getLevel() >= minLevel) && (party.get(i).getLevel() <= maxLevel))
								levelValid += 1;
							if (party.get(i).getMapid() == mapId)
								inMap += 1;
						}
						if (levelValid < minPlayers || inMap < minPlayers)
							next = false;
					}
					if (next) {
						var em = cm.getEventManager("OmegaPQ");
						if (em == null) {
							cm.sendOk("Indisponível.");
							cm.dispose();
						}
						else {
						  var prop = em.getProperty("state");
                                                  if (prop.equals("0") || prop == null) {
                                                    em.startInstance(cm.getParty(),cm.getPlayer().getMap());
                                                    party = cm.getPlayer().getEventInstance().getPlayers();
                                                    cm.dispose();
                                                    } else {
                                                      cm.sendOk("Existe outro grupo dentro da PQ.");
                                                      cm.dispose();
                                                 }
						}
						cm.dispose();
					} else {
						cm.sendOk("Seu grupo não é um grupo de 6/6 membros. Certifique-se de todos os seus membros estão presentes e qualificados para participar desta missão. Ou podem estar em outro mapa, verifique!");
						cm.dispose();
					}
				}
			}
			else {
				cm.sendOk("RAWR!?!?!?");
				cm.dispose();
			}
		} else {
			if (status == 0) {
				cm.sendYesNo("Você quer sair? Isso é demais para você?");
			} else if (status == 1) {
				var eim = cm.getPlayer().getEventInstance();
				eim.finishPQ();
				cm.dispose();
			}
		}
	}
}
					
					
