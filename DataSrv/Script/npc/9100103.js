/*
 * Criado por JavaScriptz
 * LeaderMS 2014
 * Gachapon - Kerning
 * www.leaderms.com.br
EDITADO
 */


/*            Variaveis         */
var comum = Array(2030007, 2044700, 2044701, 2044702, 2043300, 2043301, 2043302, 2040515, 2040516, 2040517, 2040005, 2040008, 2040009, 2040318, 2040010, 2040010, 2040010, 2040010, 2040010, 2040011, 2040014, 2040015, 2041021, 2041022, 2041023, 2040016, 2040017, 2040301, 1002110, 1002185, 1002248, 1002330, 1060033, 1060044, 1060072, 1060095, 1040044, 1040094, 1040100, 1040107, 1092020, 1102012, 1102002, 1102023, 1002270, 1102024, 1082053, 1082064, 1002086, 1002628, 1002619, 1072054, 1472030, 1472019, 1332015, 1032012, 1332055);
var normal = Array(1012111, 1102054, 1332020, 1332018,2040300, 2040406, 2040407, 2040410, 2040411, 2044404, 2044405, 2040302, 1002550, 1060104, 1040110, 1041107, 1102082, 1072238,1092022, 1002081, 1002393, 1002585, 1002026, 1472064, 1002089, 1002857, 1050018, 1051017, 1082146, 1082148, 1082150, 1082176, 1082178, 1082222, 1050025, 1050039,1051058, 1060070,1072122, 1072182, 1472033, 1472053, 1472029, 1332054, 1332029, 2030007, 2030007, 2030007, 2030007);
var raro = Array(1472052, 1332050, 1492012, 2040304, 2040305, 2044404, 2044405, 2040206, 2040207, 1332053, 1050099, 1052072, 1051093, 1032030, 1022060, 1492013, 1332027, 1482013, 1332051, 1332052, 1332050, 1082138, 1102042, 1102145, 1072239, 2022179, 1472053, 2030007, 2030007, 2030007, 2030007);

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
