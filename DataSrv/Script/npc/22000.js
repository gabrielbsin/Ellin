/* Author: Xterminator
	NPC Name: 		Shanks
	Map(s): 		Maple Road : Southperry (60000)
	Description: 		Brings you to Victoria Island
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
			cm.sendOk("Hmm ... Eu acho que voce ainda tem coisas a fazer aqui?");
			cm.dispose();
			return;
		}
		if (mode == 1)
			status++;
		else
			status--;
		if (status == 0) {
			cm.sendYesNo("Com este navio voce pode sair do continente. Por #e150 mesos#n, Eu vou leva-lo para #bIlha Victoria#k. A coisa e, uma vez que voce deixar este lugar, voce nao pode nunca mais voltar. O que voce acha? Voce quer ir para a Ilha Victoria?");
		} else if (status == 1) {
			if (cm.haveItem(4031801)) {
				cm.sendNext("Ok, agora me de 150 mesos ... Ei, o que e isso? E que a carta da recomendacao de Lucas, o chefe de Amherst? Ei, voce deve ter me dito que tinha isso. Eu, Shanks, reconhecer a grandeza quando vejo um, e uma vez que voce tenha sido recomendado por Lucas, eu vejo que voce tem um potencial grande, grande como um aventureiro. De jeito nenhum eu iria cobrar por esta viagem!");
			} else {
				cm.sendNext("Entediado deste lugar? Aqui ... De-me #e150 mesos#n meses primeiro...");
			}
		} else if (status == 2) {
			if (cm.haveItem(4031801)) {
				cm.sendNextPrev("Desde que voce tem a carta de recomendacao, nao vou cobrar por isso. Tudo bem, apertem os cintos, porque nos estamos indo para a cabeca de Ilha Victoria, agora, e ele pode ficar um pouco turbulento!");
			} else {
				if (cm.getLevel() >= 7) {
					if (cm.getPlayer().getMeso() < 150) {
						cm.sendOk("O que? Voce esta me dizendo que queria ir sem dinheiro? Voce e uma pessoa estranha...");
						cm.dispose();
					} else {
						cm.sendNext("Awesome! #e150#n mesos aceito! Tudo bem, fora de Ilha Victoria!");
					}
				} else {
					cm.sendOk("Vamos ver ... Eu nao acho que voce e forte o suficiente. Voce vai ter que ter pelo menos nivel 7 para ir a Ilha Victoria.");
					cm.dispose();
				}
			}
		} else if (status == 3) {
			if (cm.haveItem(4031801)) {
				cm.gainItem(4031801, -1);
				cm.warp(104000000, 0);
				cm.dispose();
			} else {
				cm.gainMeso(-150);
				cm.warp(104000000, 0);
				cm.dispose();
			}
		}
	}
}