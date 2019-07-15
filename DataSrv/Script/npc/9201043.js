/*Amos the Strong - Entrance
**9201043
**@author Jvlaple
*/

var status = 0;
var MySelection = -1;

importPackage(Packages.client);



function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
		if (status >= 0 && mode == 0) {
			cm.sendOk("Tudo bem, volte quando estiver pronto!");
			cm.dispose();
			return;
		}
		if (mode == 1) {
			status++;
		}
		else {
			status--;
		}
		if (status == 0) {
			cm.sendSimple("Meu nome é Amos the Strong. O que você gostaria de fazer?\r\n#b#L0#Entrar no desafio de Amoria#l\r\n#L1#Trocar 10 chaves para um bilhete!#l\r\n#k");
		} else if (status == 1 && selection == 0) {
			if (cm.haveItem(4031592, 1) && cm.getPlayer().isMarried()== 1) {
				cm.sendYesNo("Então você gostaria de ir para #bEntrada#k?");
				MySelection = selection;
			} else {
				cm.sendOk("Você deve ter um bilhete de entrada para entrar, e precisa estar casado.");
				cm.dispose();
			}
		} else if (status == 1 && selection == 1) {
			if (cm.haveItem(4031593, 10)) {
				cm.sendYesNo("Então, você gostaria de um Ticket ?");
				MySelection = selection;
			} else {
				cm.sendOk("Por favor, me 10 chaves primeiro!");
				cm.dispose();
			}
		} else if (status == 2 && MySelection == 0) {
			cm.warp(670010100, 0);
			cm.gainItem(4031592, -1)
			cm.dispose();
		} else if (status == 2 && MySelection == 1) {
			cm.gainItem(4031593, -10);
			cm.gainItem(4031592, 1);
			cm.dispose();
		}
	}
}