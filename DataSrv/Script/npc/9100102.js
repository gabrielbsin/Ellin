/*
 * LeaderMS Revision
 * @author GabrielSin
 * Gachapon - Perion
 */

/*            Variaveis         */
var comum = Array(1082105,1082130,1082116,1002003,1002059,1002023,1002532,1002045,1312013,1302030,1402001,1432015,1302013,1402013,1412001,1412002,1412019,1302017,2040000,2041012,2040621,2040623,2040001,2040002,2040003,2040004,1082175,1422030,1422031,1412027,1412020,1432022,1472054,2030007,2030007,2030007,2030007,2030007,2030007,2030007,2030007);
var normal = Array(1012111, 2040316,2040624,2041013,2040627,2041014,2048013,2040532,2040417,2040418,2040419,2040530,2040534,2044000,2044001,2044002,2043106,2043107,2043011,2043012,2043000,2043001,2043002,2044400,2044401,2044402,2044300,2044301,2044302,2044200,2044201,2044202,2044100,2044101,2044102,2030007,2030007,2030007,2030007,2030007,2040532,2040532,2040532,2044002,2044002,2040004,1082175,1422030,1422031,1412027,1412020,1432022,1472054,2030007,2030007,2030007);
var raro = Array(1082105,1082130,1082116,1002003,1002059,1002023,1002532,2043206,2043207,2043100,2043101,2043102,2043200,2043201,2043202,1302062,1302098,1302063,1302099,1302026,1072264,1072261,1072263,2040512,2040513,2040514,2040317,2040614,1432030,1082149,2040625,2040626,2040202,2040203,2040204,2040518,2040519,2030007,1432038,1422028,1312031,1402036,2030007,2030007,2030007,2030007,2030007,2030007);
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
