/* 	
Ralph (Old Guy in Boat Quay) 
Function: Useless.
Cody/AAron
*/


var status = 0;
var job;

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
			cm.sendOk("I miss the old days man.");
			cm.dispose();
			return;
		}
	}
}	