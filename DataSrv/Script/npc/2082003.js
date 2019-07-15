/* Author: Xterminator
	NPC Name: 		Rain
	Map(s): 		Maple Road : Amherst (1010000)
	Description: 		Talks about Amherst
*/
var status = 0;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
		if (mode == 1)
			status++;
		else
			status--;
		if (status == 0) {
			if(cm.getPlayer().getMapId()== 240000110){
                            cm.warp(270000100, 0);
                            cm.dispose();
                        } else {
                            cm.warp(240000110, 0);
                            cm.dispose();
             }
	}
   }
}