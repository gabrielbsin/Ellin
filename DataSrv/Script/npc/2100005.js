/*
 * LeaderMS Revision
 * @author Ank 
 * 2100005 (Mini) - Ariant HairStyle
*/

var status = 0;
var beauty = 0;
var mhair = Array(30320, 30330, 30150, 30800, 30680, 30900, 30170, 30180, 30820, 30410, 30460);
var fhair = Array(31400, 31090, 31190, 31520, 31650, 31620, 31420, 31780, 34000, 31330, 31340, 31660);
var hairnew = Array();

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
		if (mode == 0 && status == 0) {
			cm.dispose();
			return;
		}
		if (mode == 1)
			status++;
		else
			status--;
		if (status == 0) {
			cm.sendSimple("Hi, I'm the assistant here. Don't worry, I'm plenty good enough for this. If you have #b#t5150026##k or #b#t5151021##k by any chance, then allow me to take care of the rest, alright?\r\n#L0##i5150026##t5150026##l\r\n#L1##i5151021##t5151021##l");						
		} else if (status == 1) {
			if (selection == 0) {
				beauty = 1;
				hairnew = Array();
				if (cm.getPlayer().getGender() == 0) {
					for(var i = 0; i < mhair.length; i++) {
						hairnew.push(mhair[i] + parseInt(cm.getPlayer().getHair() % 10));
					}
				} 
				if (cm.getPlayer().getGender() == 1) {
					for(var i = 0; i < fhair.length; i++) {
						hairnew.push(fhair[i] + parseInt(cm.getPlayer().getHair() % 10));
					}
				}
				cm.sendYesNo("If you use the EXP coupon your hair will change RANDOMLY with a chance to obtain a new experimental style that I came up with. Are you going to use #b#t5150026##k and really change your hairstyle?");
			} else if (selection == 1) {
				beauty = 2;
				haircolor = Array();
				var current = parseInt(cm.getPlayer().getHair() / 10) * 10;
				for(var i = 0; i < 8; i++) {
					haircolor.push(current + i);
				}
				cm.sendYesNo("If you use a regular coupon your hair will change RANDOMLY. Do you still want to use #b#t5150026##k and change it up?");
			}
		} else if (status == 2){
			if (beauty == 1){
				if (cm.haveItem(5150026)){
					cm.gainItem(5150026, -1);
					cm.setHair(hairnew[Math.floor(Math.random() * hairnew.length)]);
					cm.sendOk("Enjoy your new and improved hairstyle!");
                                        cm.dispose();
				} else {
					cm.sendOk("Hmmm...it looks like you don't have our designated coupon...I'm afraid I can't give you a haircut without it. I'm sorry...");
                                        cm.dispose();
				}
			}
			if (beauty == 2){
				if (cm.haveItem(5151021)){
					cm.gainItem(5151021, -1);
					cm.setHair(haircolor[Math.floor(Math.random() * haircolor.length)]);
					cm.sendOk("Enjoy your new and improved haircolor!");
                                        cm.dispose();
				} else {
					cm.sendOk("Hmmm...it looks like you don't have our designated coupon...I'm afraid I can't dye your hair without it. I'm sorry...");
                                        cm.dispose();
				}
			}
		}
	}
}
