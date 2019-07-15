/* Author: Xterminator
	NPC Name: 		Regular Cab
	Map(s): 		Victoria Road : Kerning City (103000000)
	Description: 		Kerning City Cab
*/
importPackage(Packages.player);

var status = 0;
var maps = Array(104000000, 102000000, 101000000, 100000000, 120000000);
var rCost = Array(1000, 800, 1200, 1000, 1000);
var costBeginner = Array(100, 80, 120, 100, 100);
var cost = new Array("1,000", "800", "1,200", "1,000", "1,000");
var show;
var sCost;
var selectedMap = -1;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (status == 1 && mode == 0) {
            cm.dispose();
            return;
        } else if (status >= 2 && mode == 0) {
            cm.sendNext("Ha muito para ver nesta cidade, tambem. Voltar e encontrar-nos quando voce precisa ir para uma cidade diferente.");
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }
        if (status == 0) {
            cm.sendNext("Ola, eu dirijo o taxi normal. Se voce quiser ir de cidade em cidade de maneira segura e rapida, em seguida, entrar em nosso taxi. Teremos muito prazer em leva-lo ao seu destino com um preco acessivel.");
        } else if (status == 1) {
            if (cm.getJob().equals(PlayerJob.BEGINNER)) {
                var selStr = "Temos um desconto especial de 90% para iniciantes. Escolha o seu destino, pois as taxas vao mudar de lugar para lugar.#b";
                for (var i = 0; i < maps.length; i++) {
                    selStr += "\r\n#L" + i + "##m" + maps[i] + "# (" + costBeginner[i] + " mesos)#l";
                }
            } else {
                var selStr = "Escolha o seu destino, pois as taxas vao mudar de lugar para lugar.#b";
                for (var i = 0; i < maps.length; i++) {
                    selStr += "\r\n#L" + i + "##m" + maps[i] + "# (" + cost[i] + " mesos)#l";
                }
            }
            cm.sendSimple(selStr);
        } else if (status == 2) {
            if (cm.getJob().equals(PlayerJob.BEGINNER)) {
                sCost = costBeginner[selection];
                show = costBeginner[selection];
            } else {
                sCost = rCost[selection];
                show = cost[selection];
            }
            cm.sendYesNo("Voce nao tem mais nada para fazer aqui, hein? Voce realmente quer ir para #b#m" + maps[selection] + "##k?  Vai custar-lhe #b" + show + " mesos#k.");
            selectedMap = selection;
        } else if (status == 3) {
            if (cm.getMeso() < sCost) {
                cm.sendNext("Voce nao tem mesos suficiente. Desculpe dizer isso, mas, sem eles, voce nao sera capaz de entrar no taxi.");
            } else {
                cm.gainMeso(-sCost);
                cm.warp(maps[selectedMap], 0);
            }
            cm.dispose();
        }
    }
}