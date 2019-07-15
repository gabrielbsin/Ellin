/*
 * Criado por JavaScriptz
 * LeaderMS 2014
 * Gachapon - Ellinia
 * www.leaderms.com.br
 EDITADO
 */

/*            Variaveis         */
var comum = Array(1372003,1372007,1372011,1382003,1382055,1072224,1072225,1072226,1052125,1052128,1052131,1052134,1072312,1002643,2040327,2043800,2040004,2040000,2040001,2040002,2040003,2040423,2040424,2043700,2040328,2022000,2022000,2022000,2022000);
var normal = Array(2040004,2040000,2040001,2040002,2040003,2040423,2040424,2043700,2040323.2040324,2040427,2043701,2043702.2043801,2043802,2040105,1382034,1372008,1051094,1051095,1002646,1002649,2022000,2022000,2022000,2022000,2022000,2022000);
var raro = Array(1002646,1002649,1372010,1372032,1382035,1382036,1051094,1051095,1051101,1051102,1050103,1050104,1072315,1072318,2040106,2040425,2040426,2043704,2043705,2043804,2043805,2044704,2044705,2048010,2040322,2022000,2022000,2022000,2022000,2022000);
/*             Fim              */

/*            Funcao            */
function getRandom(min, max) {
	if (min > max) {
		return(-1);
	}

	if (min == max) {
		return(min);
	}

	return(min + parseInt(Math.random() * (max - min + 1)));
}
/*             Fim              */

/*            Variaveis         */
var icomum = comum[getRandom(0, comum.length - 1)];
var inormal = normal[getRandom(0, normal.length - 1)];
var iraro = raro[getRandom(0, raro.length - 1)];

var chance = getRandom(0, 100);
var status;
/*             fim              */


function start() {
    if (cm.haveItem(5451000)) {
        cm.dispose();
    } else if (cm.haveItem(5220000))
        cm.sendYesNo("Percebo que voce possui um bilhete do Gachapon, deseja usalo?");
    else {
        cm.sendSimple("Bem-vindo ao " + cm.getPlayer().getMap().getMapName() + " Gachapon. Como posso ajuda-lo?\r\n\r\n#L0#O que e Gachapon?#l\r\n#L1#Onde voce pode comprar bilhetes Gachapon?#l");
    }
}

function action(mode, type, selection){
    if (mode == 1 && cm.haveItem(5220000)) {
       if (chance > 0 && chance <= 60) {
        if(!cm.canHold(icomum)) return;   
	cm.gainItem(icomum, 1, true);
        cm.GachaMessage(icomum);
	} else if (chance >= 61 && chance <= 85) {
        if(!cm.canHold(inormal)) return;
	cm.gainItem(inormal, 1, true);
        cm.GachaMessage(inormal);
	} else {
        if(!cm.canHold(iraro)) return;    
	cm.gainItem(iraro, 1, true);
        cm.GachaMessage(iraro, true);
	}
        cm.gainItem(5220000, -1);
        cm.dispose();
    } else {
        if (mode > 0) {
            status++;
            if (selection == 0) {
                cm.sendNext("Jogando no Gachapon voce pode ganhar scrolls raros, equipamentos, cadeiras, livros de maestria, e outros artigos legais! Tudo que voce precisa e de um #bGachapon Ticket#k para poder obter algum desses items raros.");
            } else if (selection == 1) {
                cm.sendNext("Bilhete Gachapon estao disponiveis no #rCash Shop#k e podem ser adquiridos atraves do NX ou MaplePoints. Clique no SHOP vermelho no canto inferior direito da tela para visitar o #rCash Shop #konde voce podera comprar bilhetes.");
                cm.dispose();
            } else if (status == 2) {
                cm.sendNext("Voce vai encontrar uma variedade de itens da " + cm.getPlayer().getMap().getMapName() + " Gachapon, mas voce provavelmente vai encontrar varios itens e pergaminhos relacionados a cidade de " + cm.getPlayer().getMap().getMapName() + ".");
                cm.dispose();
            }
        }
    }
}
