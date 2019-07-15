var status;
var x;

	
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
		if (cm.getPlayer().getMapId() == 680000210){
	    if (status == 0) {
	                var text = "";
	                var choice = new Array("Quando e o inicio do casamento?", "Eu quero sair?");
	                for (x = 0; x < choice.length; x++) {
	                        text += "\r\n#L" + x + "##b" + choice[x] + "#l";
	                }
	                cm.sendSimple(text);
	        } else if (status == 1) {
	                switch(selection) {
	                        case 0:
	                                cm.sendOk("E so esperar ate que a noiva eo noivo estejam prontos para se casar. Aguarde uns minutinhos!");
	                                cm.dispose();
	                                break;
	                        case 1:
				       cm.removeAll(5251100);
	                               cm.warp(680000000, 0);
				       cm.dispose();
	                                break;
	                        }                
					}		
			} else if (cm.getPlayer().getMapId() == 680000200){
			cm.sendOk("Uhh, desculpe pelo atraso, Padre Joao correu para fazer algo bem rapido. Ele nao deve demorar, aguarde para que possamos comecar.");
			cm.dispose();
			}
	}
			