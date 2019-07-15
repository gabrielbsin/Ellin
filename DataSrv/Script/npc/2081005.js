var status = 0;
var mval;

importPackage(Packages.client);

function start() {
	mval = cm.getMorphValue();
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
		if (status >= 0 && mode == 0) {
			cm.sendOk("Train more then... Support the Cornian Species...");
			cm.dispose();
			return;
		}
		if (mode == 1)
			status++;
		else
			status--;
		if(cm.isMorphed() && mval == 4){
			if (status == 0 ) {
				cm.sendYesNo("Hey Fellow Cornian,\r\nYou want to go in to fight for us and the #rWyverns#k and \r\n#bHorned Tail#k and everything else in Leafre?");
			} else if (status == 1) {
				cm.warp(240050000, 0);
				cm.playerMessage(1, "For help, click the Horntail's Schedule.");
				cm.playerMessage(6, "For help, click the Horntail's Schedule.");
				cm.dispose();
			}
		}else if(cm.isMorphed() && mval != 4){
			if (status==0) {
				cm.playerMessage(6, "Keroben hates Pigs and Mushrooms and Aliens...");
				cm.setHp(0);
				cm.warp(240040600, 0);
				cm.dispose();
			}
		}else{
			if (status==0) {
				cm.playerMessage(6, "Keroben hates Humans...");
				cm.setHp(0);
				cm.warp(240040600, 0);
				cm.dispose();
			}
		}
	}
}