/*
 * LeaderMS Revision
 * @author GabrielSin
 * Gachapon - Henesys
 */

/*            Variaveis         */
var comum = Array(2043205, 2044205, 1462003, 1432009, 1302022, 1002060, 1002159, 1061051, 1002214, 1412006, 1002167, 1002162, 1040070, 1040073, 1002042, 1002138, 1002169, 1002164, 1452006, 1452022, 1462014, 1462048, 1462019, 2040008, 2040009, 2040415, 2040416, 2040005, 2041018, 2041019, 2041020, 2040413, 2040500, 2040010, 2040011, 2030007, 1002586, 1302017, 1002013, 1102029, 1102032, 1102033, 1452006, 1060095, 1061100, 1452008, 2040016, 1002161, 1050052, 1051037, 1462008, 1050059, 1051065, 1050090, 1002406, 1051084, 1050064, 1050018, 1462003, 1462004, 1072193, 1002330, 1050064);
var normal = Array(1462008, 1462010, 1462011, 1462022, 1452010,1452015, 1002405, 1002739, 1002381, 2041015, 2044304, 2044305, 2040414, 2040501, 2040502, 2040014, 2040015, 2040017, 2044500, 2044501, 2044502, 2044600, 2044601, 2044602, 1452023, 1051062, 1050078, 1050089, 1051082, 1082110, 1050091);
var raro = Array(1082147, 2040611, 2044605, 2040307, 2040916, 1051105, 1051107, 1052071, 1082158, 1002550, 2040107, 2040108, 2041016, 2041017, 2048011, 2040109, 2040921, 2040922, 2041036, 2041037, 1052148, 1052071, 1452044, 1462015, 1452018, 1452011, 1072216, 1072215, 1462018, 1452042, 1462013, 1462018, 1452017, 1452019, 1452021);

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
        cm.sendYesNo("Percebo que você possui um bilhete do Gachapon, deseja usá-lo?");
    else {
        cm.sendSimple("Bem-vindo ao " + cm.getPlayer().getMap().getMapName() + " Gachapon.\r\nComo posso ajudá-lo?\r\n\r\n#L0#O que é Gachapon?#l\r\n#L1#Onde você pode adiquirir Gachapon?#l");
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
                cm.sendNext("Jogando no Gachapon você pode ganhar scrolls raros, equipamentos, cadeiras, livros de maestria, e outros artigos legais! Tudo que você precisa é de um #bGachapon Ticket#k para poder obter algum desses items raros.");
            } else if (selection == 1) {
                cm.sendNext("Bilhete Gachapon estao disponiveis através dos monstros do jogo. O Ellin colocou uma (%) de drop do Ticket em cada mob.");
                cm.dispose();
            } else if (status == 2) {
                cm.sendNext("Você vai encontrar uma variedade de itens da " + cm.getPlayer().getMap().getMapName() + " Gachapon, mas você provavelmente vai encontrar varios itens e pergaminhos relacionados a cidade de " + cm.getPlayer().getMap().getMapName() + ".");
                cm.dispose();
            }
        }
    }
}

