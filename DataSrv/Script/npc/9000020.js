/* Author: Xterminator
	NPC Name: 		Spinel
	Map(s): 		Victoria Road : Henesys (100000000), Victoria Road : Ellinia (101000000), Victoria Road : Perion (102000000), Victoria Road : Kerning City (103000000), Victoria Road : Lith Harbor (104000000), Orbis : Orbis (200000000), Ludibrium : Ludibrium (220000000), Leafre : Leafre (240000000), Zipangu : Mushroom Shrine (800000000)
	Description: 		World Tour Guide - Takes you to Mushroom Shrine and back
*/
importPackage(Packages.server.maps);

var status = 0;
var cost;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
	if ((status <= 2 && mode == 0) || (status == 4 && mode == 1)) {
		cm.dispose();
		return;
	}
	if (mode == 1)
		status++;
	else
		status--;
	if (cm.getPlayer().getMapId() != 800000000) {
		if (status == 0) {
			if (cm.getJob().equals(Packages.client.MapleJob.BEGINNER)) {
				cm.sendNext("Se voce esta cansado do cotidiano monotono, que tal sair para uma mudanca? Nao ha nada como a absorver uma nova cultura, aprender algo novo a cada minuto! E hora de voce sair e viajar. Recomendamos uma #bWorld Tour#k, voce esta preocupado com a despesa de viagem? Nao precisa se preocupar! O #bMaple Travel Agency#k oferece o alojamento em viagens de primeira classe para o baixo preco de #b300 mesos#k.");
				cost = 300;
			} else {
				cm.sendNext("Se voce esta cansado do cotidiano monotono, que tal sair para uma mudanca? Nao ha nada como a absorver uma nova cultura, aprender algo novo a cada minuto! E hora de voce sair e viajar. Recomendamos uma #bWorld Tour#k, voce esta preocupado com a despesa de viagem? Nao precisa se preocupar! O #bMaple Travel Agency#k oferece o alojamento em viagens de primeira classe para o baixo preco de \r\n#b3,000 mesos#k!");
				cost = 3000;
			}
		} else if (status == 1) {
			cm.sendSimple("Atualmente oferecem este lugar para o seu prazer de viajar: #bMushroom Shrine of Japan#k.\r\nEu estarei la para servir-lhe como guia de viagem. Fique tranquilo, o numero de destinos ira aumentar ao longo do tempo. Agora, voce gostaria de dirigir-se ao Santuario de Cogumelo?\r\n#L0##b Sim, leve-me para Mushroom Shrine (Japao)#k#l");
		} else if (status == 2) {
			cm.sendYesNo("Gostaria de viajar para o #bMushroom Shrine of Japan#k?\r\nSe tiver vontade de sentir a essencia do Japao, nao ha nada como visitar o Santuario, um caldeirao cultural japones. Cogumelo Santuario e um lugar mitico que serve o incomparavel Mushroom desde os tempos antigos.");
		} else if (status == 3) {
			cm.sendNext("Confira o xama a servir a Mushroom God, e eu recomendo fortemente tentando Takoyaki, Yakisoba, e outros alimentos deliciosos vendido nas ruas de Japao. Agora, vamos cabeca para #bMushroom Shrine#k, um lugar mitico se alguma vez houve um.");
		} else if (status == 4) {
			if (cm.getPlayer().getMeso() < cost) {
				cm.sendPrev("Por favor, verifique e veja se voce tem mesos suficientes para ir.");
			} else {
				cm.gainMeso(-cost);
				cm.getPlayer().saveLocation(SavedLocationType.WORLDTOUR);
				cm.warp(800000000, 0);
				cm.dispose();
			}	
		}
	} else if (cm.getPlayer().getMapId() == 800000000) {
		if (status == 0) {
			cm.sendSimple ("How's the traveling? Are you enjoying it?\r\n#L0##bYes, I'm done with travelling. Can I go back to #m" + cm.getPlayer().getSavedLocation(SavedLocationType.WORLDTOUR) + "#?#k#l\r\n#L1##bNo, I'd like to continue exploring this place.#k#l");
		} else if (status == 1) {
			if (selection == 0) {
				cm.sendNext("Alright, I'll now take you back to where you were before the visit to Japan. If you ever feel like traveling again down the road, please let me know!");
			} else if (selection == 1) {
				cm.sendOk("OK. If you ever change your mind, please let me know.");
				cm.dispose();
			} 
		} else if (status == 2) {
			var map = cm.getPlayer().getSavedLocation(SavedLocationType.WORLDTOUR);
			if (map == -1) {
				map = 100000000;
			}
			cm.warp(map, 0);
			cm.dispose();
			}
		}
	}
}