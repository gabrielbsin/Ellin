/* Author: Xterminator
	NPC Name: 		Rain
	Map(s): 		Maple Road : Amherst (1010000)
	Description: 		Talks about Amherst
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
		if (mode == 1)
			status++;
		else
			status--;
		if (status == 0) {
			cm.sendNext("Esta e a cidade chamada #bAmherst#k, localizada na parte norte da Ilha Maple. Voce sabe que ha Ilha Maple e para iniciantes, certo? Estou feliz so ha monstros fracos ao redor deste lugar.");
		} else if (status == 1) {
			cm.sendNextPrev("Se voce quer ficar mais forte, entao va para #bSouthperry#k onde ha um porto. Passeio no navio gigantesco e cabeca para o lugar chamado #bIlha Victoria#k. E incomparavel em tamanho em relacao a esta pequena ilha.");
		} else if (status == 2) {
			cm.sendPrev("Na Ilha de Victoria, voce pode escolher o seu trabalho. E chamado #bPerion#k...? Ouvi dizer que e uma cidade, nua desolada onde vive guerreiros. Um planalto ... que tipo de lugar seria esse?");
		} else if (status == 3) {
			cm.dispose();
		}
	}
}