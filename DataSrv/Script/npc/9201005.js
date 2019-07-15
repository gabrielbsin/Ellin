var status;
	var x;
	var hasEngageRing = false;
	
	function start() { 
	    status = -1; 
	    action(1, 0, 0); 
	} 
	
	function action(mode, type, selection) { 
	     if (mode == -1 || mode == 0) {
	            cm.dispose();
	                        return;
	    } else if (mode == 1) {
	            status++;
	        } else {
	            status--;
	        }
		if (cm.getPlayer().getMapId() == 680000000){
	    if (status == 0) {
	                var text = "Rings are currently unavailable.";
	                var choice = new Array("They will be available in the future.#k");
	                for (x = 0; x < choice.length; x++) {
	                        text += "\r\n#L" + x + "##b" + choice[x] + "#l";
	                }
	                cm.sendSimple(text);
	        } else if (status == 1) {
	                switch(selection) {
	                        case 0:
	                                cm.sendOk("Come back later!");
	                                cm.dispose();
	                                break;
	                        case 1:    
	                             cm.sendOk("");	                                      
	                                break;
	                        }                
					}		
			} else if (cm.getPlayer().getMapId() == 680000200){
			cm.sendOk("I'm here to assist you if you need anything! I'll be right next to you until you get married.");
			cm.dispose();
			}
		}
			