var status;

function start() {
    status = -1;
    action(1,0,0);
}

function action(mode, type, selection){
    if (mode == 1)
        status++;
    else {
        cm.dispose();
        return;
    }
    var mapId = cm.getPlayer().getMapId();
    if (mapId == 103000890) {
        if (status == 0) {
            cm.sendNext("Entendo. O trabalho de equipe e muito importante aqui. Por favor, esforce-se mais com os membros do seu grupo.");
        } else {
            cm.warp(103000000, 0);
            cm.removeAll(4001007);
            cm.removeAll(4001008);
            cm.dispose();
        }
    } else {
        if (status == 0) {
            var outText = "Se sair do mapa, voce vai precisar refazer toda a missao se quiser tentar novamente. Ainda quer sair deste mapa?";
            if (mapId == 103000805) {
                outText = "Voce esta pronto para deixar este mapa?";
            }
            cm.sendYesNo(outText);
        } else if (mode == 1) {
            var eim = cm.getPlayer().getEventInstance(); 
            if (eim == null)
                cm.warp(103000890, "st00");
            else if (cm.isLeader()) {
                eim.disbandParty();
            } else
            eim.leftParty(cm.getPlayer());
            cm.dispose();
        }
    }
}