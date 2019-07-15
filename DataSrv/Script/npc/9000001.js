///* Cadeiras
//Made By bahadirtje Ragezone- YarakMS
//Pf, best nomes de cadeiras by Ank 
//*/
//
//
//var nomeServer = "Leader-MapleStory";
//
//
//
//function start() {
//    status = -1;
//    action(1, 0, 0);
//}
//
//function action(mode, type, selection) {
//    if (mode == 1)
//        status++;
//    else {
//        cm.sendOk("Tudo bem, até a próxima!");
//        cm.dispose();
//        return;
//    }
//    if (status == 0) {
//            cm.sendSimple("                          #e<" + nomeServer + " - Cadeiras>#n            \r\n\r\nOlá #e#h ##n,\r\nAqui você poderá trocar seus LeaderPoints por cadeiras.\r\n\Você tem algum LeaderPoints para mim?#b\r\n\#L1#Trocar LeaderPoints#l");
//        } else if (selection == 1) {
//            cm.sendSimple("Quantos LeaderPoints você tem?#b\r\n\#L4#Trocar (5) LeaderPoints por cadeiras\r\n\#L5#Trocar (10) LeaderPoints por cadeiras\r\n\#L6#Trocar (20) LeaderPoints por cadeiras");
//        }  else if (selection == 4) {
//        cm.sendSimple("Escolha a sua cadeira, são  5 LeaderPoints#b cada!\r\n\#L7##v3010004# Yellow Relaxer\r\n\#L8##v3010005# Red Relaxer\r\n\#L9##v3010011# Amorian Relaxer \r\n\#L10# #v3010006# Yellow Chair \r\n\#L11##v3010002# Green Chair \r\n\#L12##v3010003# Red Chair \r\n\#L13##v3010012# Warrior Throne\r\n");
//        }  else if (selection == 5) {
//        cm.sendSimple("Escolha a sua cadeira, são  10 LeaderPoints#b cada!\r\n\#L14##v3010008# Cadeira de Foca Azul \r\n\#L15##v3010007# Cadeira de Foca Rosa\r\n\#L16##v3010017# Cadeira de Foca Douorada\r\n\#L17##v3010016# Cadeira de Foca Cinza \r\n\#L18##v3010010# Cadeira de Foca Branca \r\n\#L19##v3010013# Cadeira de Praia com Palmeiras  \r\n\#L20##v3010018# Cadeira de Praia Rosa \r\n\#L21# #v3011000# Cadeira de Pesca\r\n");
//        }  else if (selection == 6) {
//        cm.sendSimple("Escolha a sua cadeira, são  20 LeaderPoints#b cada!\r\n\#L22# #v 3010022 # Urso Polar Branco (Coca Cola) \r\n\#L23# #v 3010023 # Urso Polar Marrom (Coca Cola)\r\n\#L24# #v 3010025 # Cadeira Arvore Maple\r\n\#L25##v3010009# Cadeira Rosa Redonda \r\n\#L26##v3010040# Cadeira de Morcego \r\n\#L27##v3010026# Cadeira de Urso Zumbi \r\n\#L28##v3010028# Cadeira do Lorde Pirata \r\n\#L29##v3010041# Trono de Cranio\r\n");
//        } else if (selection == 7) {
//      if (cm.getPlayer().getCSPoints(2) >= 5) {
//                      cm.getPlayer().modifyCSPoints(2, -5);
//                      cm.gainItem(3010004, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 8) {
//      if (cm.getPlayer().getCSPoints(2) >= 5) {
//                      cm.getPlayer().modifyCSPoints(2, -5);
//                      cm.gainItem(3010005, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 9) {
//      if (cm.getPlayer().getCSPoints(2) >= 5) {
//                      cm.getPlayer().modifyCSPoints(2, -5);
//                      cm.gainItem(3010011, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 10) {
//      if (cm.getPlayer().getCSPoints(2) >= 5) {
//                      cm.getPlayer().modifyCSPoints(2, -5);
//                      cm.gainItem(3010006, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 11) {
//      if (cm.getPlayer().getCSPoints(2) >= 5) {
//                      cm.getPlayer().modifyCSPoints(2, -5);
//                      cm.gainItem(3010002, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 12) {
//      if (cm.getPlayer().getCSPoints(2) >= 5) {
//                      cm.getPlayer().modifyCSPoints(2, -5);
//                      cm.gainItem(3010003, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 13) {
//      if (cm.getPlayer().getCSPoints(2) >= 5) {
//                      cm.getPlayer().modifyCSPoints(2, -5);
//                      cm.gainItem(3010012, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 14) {
//      if (cm.getPlayer().getCSPoints(2) >= 10) {
//                      cm.getPlayer().modifyCSPoints(2, -10);
//                      cm.gainItem(3010008, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 15) {
//      if (cm.getPlayer().getCSPoints(2) >= 10) {
//                      cm.getPlayer().modifyCSPoints(2, -10);
//                      cm.gainItem(3010007, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 16) {
//      if (cm.getPlayer().getCSPoints(2) >= 10) {
//                      cm.getPlayer().modifyCSPoints(2, -10);
//                      cm.gainItem(3010017, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 17) {
//      if (cm.getPlayer().getCSPoints(2) >= 10) {
//                      cm.getPlayer().modifyCSPoints(2, -10);
//                      cm.gainItem(3010016, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 18) {
//      if (cm.getPlayer().getCSPoints(2) >= 10) {
//                      cm.getPlayer().modifyCSPoints(2, -10);
//                      cm.gainItem(3010010, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 19) {
//      if (cm.getPlayer().getCSPoints(2) >= 10) {
//                      cm.getPlayer().modifyCSPoints(2, -10);
//                      cm.gainItem(3010013, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 20) {
//      if (cm.getPlayer().getCSPoints(2) >= 10) {
//                      cm.getPlayer().modifyCSPoints(2, -10);
//                      cm.gainItem(3010018, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 21) {
//      if (cm.getPlayer().getCSPoints(2) >= 10) {
//                      cm.getPlayer().modifyCSPoints(2, -10);
//                      cm.gainItem(3011000, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 22) {
//      if (cm.getPlayer().getCSPoints(2) >= 20) {
//                      cm.getPlayer().modifyCSPoints(2, -20);
//                      cm.gainItem(3010022, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 23) {
//      if (cm.getPlayer().getCSPoints(2) >= 20) {
//                      cm.getPlayer().modifyCSPoints(2, -20);
//                      cm.gainItem(3010023, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 24) {
//      if (cm.getPlayer().getCSPoints(2) >= 20) {
//                      cm.getPlayer().modifyCSPoints(2, -20);
//                      cm.gainItem(3010025, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 25) {
//      if (cm.getPlayer().getCSPoints(2) >= 20) {
//                      cm.getPlayer().modifyCSPoints(2, -20);
//                      cm.gainItem(3010009, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 26) {
//      if (cm.getPlayer().getCSPoints(2) >= 20) {
//                      cm.getPlayer().modifyCSPoints(2, -20);
//                      cm.gainItem(3010040, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 27) {
//      if (cm.getPlayer().getCSPoints(2) >= 20) {
//                      cm.getPlayer().modifyCSPoints(2, -20);
//                      cm.gainItem(3010026, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 28) {
//      if (cm.getPlayer().getCSPoints(2) >= 20) {
//                      cm.getPlayer().modifyCSPoints(2, -20);
//                      cm.gainItem(3010028, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        } else if (selection == 29) {
//      if (cm.getPlayer().getCSPoints(2) >= 20) {
//                      cm.getPlayer().modifyCSPoints(2, -20);
//                      cm.gainItem(3010041, 1);
//                      cm.sendOk("Obrigado, aproveite sua nova cadeira!");
//                      cm.dispose();
//            } else {
//                cm.sendOk("Desculpe, mais você não tem LeaderPoints suficiente!");
//                cm.dispose();
//        }
//        
//
//    } 
//}

function start() {
    if(cm.getPlayer().getClient().getChannel() == 1){
        if (cm.getEventNotScriptOpen("RoletaRussa"))
            cm.sendYesNo("Oba oba... me parece que o evento #eRoleta Russa#n está acontecendo, deseja participar? Isso vai lhe custar 100k de Mesos.");
        else{
            cm.sendOk("Que pena, parece que o evento ja fechou!");
            cm.dispose();
        }
    } else {
        cm.sendOk("Ah, não... Parece que você não está no canal correto, verifique.");
        cm.dispose();
    }
}
function action(mode, type, selection) {
    if (mode <= 0) {
	cm.sendOk("Tudo bem, fale comigo quando quiser novamente!");
        cm.dispose();
	return;
    } 
    cm.gainMeso(-100000);
    cm.warp(670010400, 0);
    cm.dispose();
}	