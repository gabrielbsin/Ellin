function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == 1) {
	status++;
    } else {
	if (status == 0) {
	    cm.dispose();
		return;
	}
	status--;
    }
    switch(cm.getPlayer().getMapId()) { 
	case 221000200:
		if (cm.getPlayer().getEventInstance() == null || !cm.isLeader()) {
			cm.sendOk("Por favor, eu preciso do seu líder.");
		} else {
                        if (!cm.getPlayer().getEventInstance().getProperty("pqFinished").equals("true")) {
				cm.sendOk("Por favor, termine completamente com os monstros!");
			} else {
                            	var eim = cm.getPlayer().getEventInstance();
                                var mf = eim.getMapFactory();
				map = mf.getMap(cm.getMapId() + 1);
				partyss = eim.getPlayers();
                                for (var i = 0; i < partyss.size(); i++) {
					partyss.get(i).changeMap(map, map.getPortal(0));
					eim.unregisterPlayer(partyss.get(i));
				 }
				eim.dispose();
				cm.dispose();
			}
		}
		cm.dispose();
		break;
    }
}