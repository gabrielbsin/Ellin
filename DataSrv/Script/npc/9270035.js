/*
* @NPC : Shanks(22000)
* @function : checkdrops (advanced)
* @Author : Squiggles
*/
status = -1;
var sel;

function start() {
cm.sendSimple("Olá, eu posso te ajudar a encontrar diversos drops. Escolha abaixo algumas das opções!\r\n#L0#Monstro no Mapa#l\r\n#L1#Procurar drop por nome do Monstro#l\r\n#L2#Procurar drop por ID do Item!#l\r\n\r\n#eOutros#n\r\n#L3#Procurar nome de um Monstro!#l\r\n#L4#Procurar ID de um Item!#l");
}
function action (mode, type, selection) {
    status++;
    if (mode != 1) {
        if (mode == 0 && type != 3) {
            status -= 2;
        } else {
            cm.dispose();
            return;
        }
    }

    if (status == 0) {
        sel = selection;
        if (selection == 0) 
            cm.listMonsters();
     else if (selection == 1) 
            cm.sendGetText("Digite o nome do monstro que você deseja encontrar os drops.");
     else if (selection == 2) 
            cm.sendGetText("Coloque o ID do item abaixo:");
     else if (selection == 3) 
            cm.sendGetText("Coloque o nome do monstro abaixo:");
     else if (selection == 4) 
            cm.sendGetText("Coloque o nome do item abaixo:");   
    } else if (status == 1) {
        if (sel == 0) {
            cm.displayDrops(selection);
            cm.dispose();
        } if (sel == 1) {
            cm.displayDrops(cm.getMobId(cm.getText()));
            cm.dispose();
        } if (sel == 2) {
            if (isNaN(cm.getText())) {
                cm.sendOk("Somente numeros.");
                cm.dispose();
            } else {
                cm.searchToItemId(parseInt(cm.getText()));
                cm.dispose();
            }
        } if (sel == 3) {
            cm.searchToNameMob(cm.getText());
            cm.dispose();
        } if (sel == 4) {
            cm.searchToNameItem(cm.getText());
            cm.dispose();
        }
    }
}  

function isNumber(n) {
    return !isNaN(parseFloat(n)) && isFinite(n);
}
