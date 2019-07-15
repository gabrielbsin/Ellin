var status = 0;

function start() {
    cm.sendNext("Que dia lindo, que tal receber um presente? (...)");
}

function action(mode, type, selection) {
     if (cm.haveItem(3010045) == true) {
                cm.sendOk("Sinto muito, voce ja recebeu seu presente!");
                cm.dispose();
            } else {
            cm.gainItem(3010045, 1);
            cm.sendOk("Parabens, voce recebeu uma cadeira!");
            cm.getPlayer().dropMessage("[Equipe LeaderMS] Feliz Natal e boas festas! <3");
            cm.dispose();
        }
}