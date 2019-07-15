//9201014
var status;
	
	function start() {
	    status = -1;
	    action(1, 0, 0);
	}
	
	function action(mode, type, selection) {
	    if (mode == -1 || mode == 0) {
	        cm.sendOk("Bye");
	        cm.dispose();
	        return;
	    } else if (mode == 1) {
	        status++;
	    } else {
	        status--;
	    }
	   if (cm.getPlayer().getMapId() == 680000000){            
	    if (status == 0) {
	        var msg = "Hello I can exchange your #bOnyx Chest for Bride and Groom#k for a random reward for getting married!";
	        var choice1 = new Array("I have an Onyx Chest for Bride and Groom");
	        for (var i = 0; i < choice1.length; i++) {
	            msg += "\r\n#L" + i + "#" + choice1[i] + "#l";
	        }
	        cm.sendSimple(msg);
	    } else if (status == 1) {
	        if (selection == 0) {
	            if (cm.haveItem(4031424)) {
	                var rand = Math.floor(Math.random() * 4);
	                if (rand == 0)
	                    cm.gainItem(2022179,2);
	                else if (rand == 1)
	                    cm.gainItem(2022282,2);
	                else if (rand == 2)
	                    cm.gainItem(2022273,2);
	                cm.gainItem(4031424,-1);
	            } else {
	                cm.sendOk("You don't have an Onyx Chest for Bride and Groom.");
	                cm.dispose();
	            }
	        } 
			}
	} else if (cm.getPlayer().getMapId() == 680000200) {
			cm.sendOk("Hey! So you two are getting married huh? That's good news! Well don't worry, the alter is getting cleared now and were waiting for your friends to enter if you invited anyone. Just wait here and enjoy your last minutes as singles!");
			cm.dispose();
			}
			}