/* Author: Xterminator
	NPC Name: 		Joel
	Map(s): 		Victoria Road : Ellinia Station (101000300)
	Description: 		Ellinia Ticketing Usher
*/
var status = 0;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
	if (status >= 0 && mode == 0) {
		cm.sendNext("Voce deve ter alguns negocios para cuidar aqui, certo?");
		cm.dispose();
		return;
	}
	if (mode == 1)
		status++;
	else
		status--;
	if (status == 0) {
		cm.sendYesNo("Ola, sou o responsavel pela venda de bilhetes para o passeio de barco para a Estacao de Orbis Ossyria. A viagem ate Orbis vai te custar #b5000 mesos#k. Tem certeza de que quer comprar um #bBilhete para Orbis (Normal)#k?");
	} else if (status == 1) {
		if (cm.getPlayer().getMeso() < 5000) {
			cm.sendNext("Tem certeza que voce tem #b5000 mesos#k? Se assim for, entao peço que voce verifique o seu inventario, etc, e ver se ele esta cheio ou nao.");
			cm.dispose();
		} else {
			cm.gainMeso(-5000);
			cm.gainItem(4031045, 1);
			cm.dispose();
			}		
		}
	}
}