var status = 0;

function start() {
        cm.sendYesNo("Deseja participar da #bNPQ#k?");
}

function action(mode, type, selection) {
    if(mode != 1)
        cm.dispose();
    else {
        status++;
        if(status == 1) {
            if(!cm.getNatal()) {
                cm.dispose();
                return;
            }
            if(cm.getPlayer().getLevel() > 9) {
                cm.warp(889100100, 0);
                cm.dispose();
                } else {
                  cm.sendOk("E necesário ter no minino lv. 10!");
                  cm.dispose();
              }
        }
    }
}