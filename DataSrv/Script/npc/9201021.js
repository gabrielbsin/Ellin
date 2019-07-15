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
	    if (cm.getPlayer().getMapId() == 680000300){		
				cm.sendOk("Congratulations on your wedding! Now lets take a lot of pictures to remember this great day.");
				cm.dispose();
			} 	 
		}