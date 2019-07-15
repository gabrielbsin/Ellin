/*
 * @autor JS-Maple
 */
var status = 0;

function start() {
    cm.sendYesNo("Você deseja sair do Mapa?");
}

function action(mode, type, selection) {
	if (mode == 1) {
             switch(cm.getPlayer().getMapId()) {
                 case 889100000:
                 case 889100010:
                 case 889100020:
                     cm.warp(209000000);
                     break;
                case 889100001:
	        case 889100011:
	        case 889100021:
                if (cm.isLeader()) {
                    var eim = cm.getPlayer().getEventInstance();
		    var party = cm.getPlayer().getEventInstance().getPlayers();
                    var mf = eim.getMapFactory();
                    map = mf.getMap(cm.getMapId() + 1);
                    for (var i = 0; i < party.size(); i++) {
			party.get(i).changeMap(map, map.getPortal(0));
			eim.unregisterPlayer(party.get(i));
		   }
               } else {
                     if(eim != null) {
                     eim.unregisterPlayer(cm.getPlayer());
                     }
		     cm.warp(cm.getMapId() + 1);
		     cm.dispose();  
                }
                break;
             }
	}
}