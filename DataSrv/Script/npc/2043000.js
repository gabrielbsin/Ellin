var partyss;
var  map;

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
                var eim = cm.getPlayer().getEventInstance();
                var mf = eim.getMapFactory();
                map = mf.getMap(cm.getMapId() - 200);
                partyss = eim.getPlayers();
                if (status == 0) {
	            cm.sendSimple("Você deseja sair?\r\n #L0#Sim, eu quero!#l");
    	        } else if (status == 1) {
                    if (selection == 0) {
                    if(!cm.isLeader()) {
		        eim.unregisterPlayer(cm.getPlayer());
		        cm.dispose();
                    } else {
                        for (var i = 0; i < partyss.size(); i++) {
					partyss.get(i).changeMap(map, map.getPortal(0));
					eim.unregisterPlayer(partyss.get(i));
				 }
			eim.dispose();
			cm.dispose();
                    }
                }
          }
     }
}