
/*  Cliff - Happy Ville NPC
 */
importPackage(Packages.client);
importPackage(Packages.server);

var status = -1;
var nomeServer = "Ellin";

function start() {
    action(1, 0, 0);
 }

function action(mode, type, selection) {
    if (mode == 1) {
        status++;
    } else {
        if (status > 0) {
            status--;
        } else {
            cm.dispose();
            return;
        }
    } 
    if (status == 0) {
        cm.sendNext("                                  <#e" + nomeServer + " Halloween#n>             \r\n\r\nOla #e#h ##n, eu sou a Malady o auxiliar do LeaderMS.\r\nEstou precisando de sua ajuda para #bcoletar#k um item de outro mundo, se voce me recolher este item eu te ajudarei a crescer, pode me ajudar?\r\nTudo bem, os item que preciso e:\r\n\r\n#i4000524# #t4000524# - Qntd. 500\r\n\r\nVoce tem a quantia de (#e" + cm.getPlayer().countItem(4000524) + "#n) #t4000524#.\r\n\r\nSe voce ja #epossui#n estes items, clique em continuar, caso nao tenha, volte novamente mais tarde.");
    } else if (status == 1) {
        cm.sendSimple("Qual item voce gostaria de trocar? \r\n\r\n#L0#Chapeu Falante - Atributos Aleatorios#l");
        
    } else if (selection == 1000){
                 cm.sendSimple("#eConseguindo LeaderPoints - Mini Tuto#n\r\nOs LeaderPoints sao adquiridos atraves de monstros, cada monstro derrotado voce tem uma porcetagem de ganhar uma quantia de LeaderPoints, eles variam de 1 entre 3.\r\n\r\n#eConseguindo Ocupacao - Mini Tuto#n\r\nVoce devera participar de grande maioria das missoes espalhadas pelo Leader, apos o termino delas voce acumula pontos para poder obter uma ocupacao! ");
    } else if(selection == 0) { /* HERO */
      if(cm.haveItem(4000524, 1)) {
            var ii = MapleItemInformationProvider.getInstance();
            var newItem = ii.randomizeStatsMalady(ii.getEquipById(1000027));
            MapleInventoryManipulator.addFromDrop(cm.getClient(), newItem, "");
            cm.gainItem(4000524, -1);
            cm.sendOk("Obrigado, voce ja pode aproveitar seu novo item!");
            cm.dispose();
        } else {
        cm.sendOk("Que pena, voce ainda nao tem os items suficientes.");
        cm.dispose();
   } 
 }
} 
