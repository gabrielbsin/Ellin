/*
 * Criado por JavaScriptz
 * LeaderMS 2014
 * Gachapon - Sleep
 * www.leaderms.com.br
EDITADO - ANK
 */

/*            Variaveis         */
var comum = Array(2000005, 1032032, 1442018, 2044701, 2044702, 2044602, 2040101, 2043002, 2040517, 2040902, 2040705, 2040707, 2040708, 2044001, 2044002, 1442039, 2101000, 2041006, 2041007, 2041008, 2041009, 2041010, 2041011, 2040803, 2040804, 2040805, 2040532, 2040534, 2040024, 2040026, 2040027, 2040028, 2040030, 2040031, 2040000, 2040001, 2040002, 2000001, 2000002, 2000003, 2000004, 2044302, 2044404, 2044405, 2044401, 2044402, 4010001, 4010002, 4010003, 4010004, 4010005, 4030012, 2030007, 2040420, 2040421, 2040803, 2043104, 2043105, 2043204, 2043205, 2022069, 2022069, 2022069, 1040014, 1002060, 1002723, 1002788, 1082148, 1072008, 1072056, 2040422, 2040325, 2040326);
var normal = Array(2000005, 1032032, 1442018, 2044701, 1072056, 2040422, 2040325, 2040326, 2040815, 2040816, 2040306, 2040804, 2040805, 2040612, 2040613, 2040817, 2040818, 2040200, 2040201, 2044504, 2043004, 2043006, 2043007, 2000005, 1032032, 1442018);
var raro = Array(2000005, 1032032, 1442018, 2000005, 1032032, 1442018, 2040513, 2100000, 2040025, 2040029, 2040804, 2040805, 2040612, 2040613, 2040817, 2040915, 2040208, 2040209, 2040814,2043005,2040918, 2040307, 2044505, 2040916, 2048012, 1022060, 1022058, 1012084, 1002393, 2022069, 1072238, 1102040, 1382047,1372036);

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

