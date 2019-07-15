/* Salon Seamus
	Amoria Random Hair/Hair Color Change.
*/
var status = 0;
var beauty = 0;
var mhair = Array(30570, 30690, 30250, 30230, 30050, 30280, 30410, 30290, 30300, 30580, 30590, 30200, 30450);
var fhair = Array(31490, 31570, 31150, 31590, 31310, 31220, 31260, 31020, 31160, 31110, 31230, 31580, 31480);
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
			cm.sendSimple("I'm Salon Seamus. If you have #b#t5150019##k or #b#t5151016##k by any chance, then how about letting me change your hairdo?\r\n#L0##i5150019##t5150019##l\r\n#L1##i5151016##t5151016##l");						
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
				cm.sendYesNo("If you use the EXP coupon your hair will change RANDOMLY with a chance to obtain a new experimental style that even you didn't think was possible. Are you going to use #b#t5150019##k and really change your hairstyle?");
			} else if (selection == 1) {
				beauty = 2;
				haircolor = Array();
				var current = parseInt(cm.getPlayer().getHair() / 10) * 10;
				for(var i = 0; i < 8; i++) {
					haircolor.push(current + i);
				}
				cm.sendYesNo("If you use a regular coupon your hair will change RANDOMLY. Do you still want to use #b#t5150019##k and change it up?");
			}
		} else if (status == 2){
			if (beauty == 1){
				if (cm.haveItem(5150019)){
					cm.gainItem(5150019, -1);
					cm.setHair(hairnew[Math.floor(Math.random() * hairnew.length)]);
					cm.sendOk("Enjoy your new and improved hairstyle!");
                                        cm.dispose();
				} else {
					cm.sendOk("Hmmm...it looks like you don't have our designated coupon...I'm afraid I can't give you a haircut without it. I'm sorry...");
                                        cm.dispose();
				}
			}
			if (beauty == 2){
				if (cm.haveItem(5151016)){
					cm.gainItem(5151016, -1);
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