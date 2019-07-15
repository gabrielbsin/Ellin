function start() {
    if(cm.haveItem(4031576)){
        if (cm.getEventNotScriptOpen("Genio")) {
            cm.sendYesNo("Parece que está cheio de lugares vazios para esta viagem. Por favor, tenha seu bilhete em mãos para embarcar. O trajeto será longo, mas chegará bem a seu destino. O que você acha? Quer embarcar neste?");
        } else {
            cm.sendOk("Sinto muito, mas o Gênio já está CHEIO. Não podemos aceitar mais nenhum passageiro. Por favor, embarque no próximo.");
            cm.dispose();
        }
    } else {
        cm.sendOk("Ah, não... Não creio que tenha seu bilhete consigo. Não pode embarcar sem ele. Por favor, compre o bilhete no balcão de bilhetes.");
        cm.dispose();
    }
}
function action(mode, type, selection) {
    if (mode <= 0) {
	cm.sendOk("Tudo bem, fale comigo quando quiser novamente!");
        cm.dispose();
	return;
    } 
    cm.gainItem(4031576, -1);
    cm.warp(200000152, 0);
    cm.dispose();
}