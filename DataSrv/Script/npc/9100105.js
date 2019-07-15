/*
 * LeaderMS Revision
 * @author GabrielSin
 * Gachapon - Mushroom Shine
 */

/*            Variaveis         */
var comum = Array(2022184, 2022184, 2022184, 2022068, 2022068, 2022068, 1382015, 2022068, 1382020, 1382028, 1402006, 1442037, 1382012, 2022068, 1432046, 1402007, 2022068, 2043111, 2043112, 2043113, 2043114, 2043211, 2043212, 2043213, 2043214, 2044111, 2044112, 2044113, 2044114, 2044806, 2044807, 2044808, 2044809, 2044411, 2044412, 2044413, 2044414, 2044311,2044312, 2044313, 2044314, 2022184, 2040508, 2040520, 2040521, 2040531, 2040533, 2040703, 2040704, 2040705, 2040712, 2040713, 2040714, 2040716, 2040717, 2040810, 2041034, 2041035, 2041038, 2041039, 2041040, 2041041, 2044800, 2044801, 2044802, 2044803, 2044900, 2044901, 2044902, 2044903, 2044604, 2043304, 2044004, 2044104, 2044204); 
var normal = Array(1012111, 1082145, 1332053, 2040411, 2044405, 2043005, 2044804, 2044904, 2044505, 2040611, 1332032, 2022184, 2022068, 2022184, 2022068, 2022184, 2022068, 2022184, 2022068, 2022184, 2022068, 2022184, 2022068, 2049000, 2049000, 2049000, 2044113, 2044114, 1472053, 1472048, 1482011, 1452033, 1472054, 1442012, 1442013, 1442014, 1442015, 1442016, 1442017, 1442018, 1332039,1332040, 1332041, 2043305, 2040811, 2040509);
var raro = Array(1082145, 1332053, 2040411, 2044405, 2043005, 2044804, 2044904, 2044505, 2040611, 1332032, 1372038, 1382045, 1382046, 1382047, 1382048, 1382049, 1382050, 1382051, 1382052, 2044205, 2044105, 2044005, 2043305, 2044605, 2044904, 2040811, 2040715, 2040610, 2040509, 2040917, 2040914, 2044804, 2040611, 2040005, 2040008, 2040009, 2040010, 2040319, 1402037, 2040320, 2040321, 2040011, 2040014, 2040015, 2040016, 2040017, 2040018, 2040019, 2040020, 2040012, 2040013, 2040024, 2040025, 2040026);

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
