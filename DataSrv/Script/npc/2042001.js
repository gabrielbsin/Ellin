/*
  [Monster Carnival PQ] [By Haiku01]
*/
importPackage(Packages.server.maps);
 
var status = 0;
var rnk = 0;
 
function start() {
  status = -1;
        action(1, 0, 0);
}
 
function action(mode, type, selection) {
        if (mode == -1) {
                cm.dispose();
        } else {
                if (status >= 0 && mode == 0) {
                        cm.sendOk("Tudo bem então, eu espero que nós possamos conversar mais na próxima vez.");
                        cm.dispose();
                        return;
                }
                if (mode == 1)
                        status++;
                else
                        status--;
    if (cm.getPlayer().getMap().getId() == 220000000) {
        if (status == 0) {
            cm.sendSimple("Olá, meu nome é Spieglemann, e eu sou responsável pela Fólia de Monstros. Eu estou tentando encontrar jogadores que estão dispostos a lutar, por lá vive uns contra os outros em uma competição justa.\r\n\r\n#L0#Gostaria de participar#l\r\n#L1#O que é a Fólia de Monstros?#l\r\n#L2#Trocar MapleCoins#l");
        } else if (status == 1) {
            if (selection == 0) {
                if (cm.getLevel() > 29 && cm.getLevel() < 51 || cm.getPlayer().isGM()) {
                    cm.getPlayer().saveLocation(SavedLocationType.MONSTER_CARNIVAL);
                    cm.warp(980000000, 0);
                    cm.dispose();
                    return;
                } else {
                    cm.sendOk("Sinto muito, mas apenas os jogadores de nível 30~50 podem participar.");
                    cm.dispose();
                    return;
                }
            } else if (selection == 1) {
                cm.sendOk("O que é Fólia de Monstros? Ha Ha Ha! Vamos apenas dizer que é uma experiência que nunca vou esquecer! É uma batalha contra outros viajantes como você, e eu sei que é muito perigoso para você lutar entre si usando armas reais, nem eu gostaria de sugerir um ato tão bárbaro. Não meu amigo, o que eu ofereço é a competição. A emoção de batalha e emoção contra pessoas tão fortes e motivados como a ti mesmo . Essa é aessência da Fólia de Monstros. Além disso, você pode usar suas MapleCoins ganhas durante a Fólia de Monstros e obter novos itens e armas! Claro, não é tão simples assim. Existem diferentes maneiras para impedir a outra parte de caçar monstros, e é até você para descobrir como. O que você acha? Interessado em um pouco de competição amigável (ou não tão amigável) ?");
                cm.dispose();
                status = 0;
        } else if (selection == 2) {
	     cm.sendSimple("Aqui você pode adquirir um colar, você precisa de 50 MapleCoins.\r\n#L43#Clique para ganhar!#l");
	 }
    }  else if (selection == 43) {
         if(cm.haveItem(4001129, 50)) {
           cm.gainItem(1122007, 1)
           cm.gainItem(4001129, -50)
           cm.dispose();
           } else {
               cm.sendOk("Você nao tem moedas suficiente!");
               cm.dispose();
           }
        } else if (cm.getPlayer().getMap().isCPQWinnerMap()) {
        if (status == 0) {
            if (cm.getPlayer().getParty() != null) {
                var shi = "Parabéns pela sua vitória!!! Foi realmente perfeita! O grupo oposto mal pode reagir! De agora em diante é isso que eu espero de você sempre que participar!\r\n\r\n#bRank do Festival dos Monstros: ";
                switch (cm.calculateCPQRanking()) {
                    case 1:
                        shi += "A";
                        rnk = 1;
                        cm.sendOk(shi);
                        break;
                    case 2:
                        shi += "B";
                        rnk = 2;
                        cm.sendOk(shi);
                        break;
                    case 3:
                        shi += "C";
                        rnk = 3;
                        cm.sendOk(shi);
                        break;
                    case 4:
                        shi += "D";
                        rnk = 4;
                        cm.sendOk(shi);
                        break;
                    default:
                        cm.sendOk("Houve um erro com a Fólia de Monstros.");
                }
            } else {
                cm.warp(980000000, 0);
                cm.dispose();
            }
        } else if (status == 1) {
            switch (rnk) {
                case 1:
                    cm.warp(980000000, 0);
                    cm.gainExp(30000);
                    cm.dispose();
                    break;
                case 2:
                    cm.warp(980000000, 0);
                    cm.gainExp(25500);
                    cm.dispose();
                    break;
                case 3:
                    cm.warp(980000000, 0);
                    cm.gainExp(21000);
                    cm.dispose();
                    break;
                case 4:
                    cm.warp(980000000, 0);
                    cm.gainExp(19550);
                    cm.dispose();
                    break;
                default:
                  cm.warp(980000000, 0);  
                  cm.gainExp(19550);
            }
        }
      } else if (cm.getPlayer().getMap().isCPQLoserMap()) {
        if (status == 0) {
          if (cm.getPlayer().getParty() != null) {
           var shiu = "Desculpe pela perda... No entanto, você foi muito bem e deu um show! De agora em diante é isso que eu espero de você sempre que participar, desempenho!\r\n\r\nRank do Festival dos Monstros: ";
            switch (cm.calculateCPQRanking()) {
              case 10:
                shiu += "A";
		rnk = 10;
                cm.sendOk(shiu);
                break;
              case 20:
                shiu += "B";
		rnk = 20;
                cm.sendOk(shiu);
                break;
              case 30:
                shiu += "C";
		rnk = 30;
                cm.sendOk(shiu);
                break;
              case 40:
                shiu += "D";
		rnk = 40;
                cm.sendOk(shiu);
                break;
              default:
                cm.sendOk("Houve um erro com a Fólia de Monstros.");
            }
          } else {
            cm.warp(980000000, 0);
            cm.dispose();
          }
        }
    } else if (status == 1) {
        switch (rnk) {
            case 10:
                cm.warp(980000000, 0);
                cm.gainExp(10000);
                    cm.dispose();
                    break;
                case 20:
                    cm.warp(980000000, 0);
                    cm.gainExp(8500);
                    cm.dispose();
                    break;
                case 30:
                    cm.warp(980000000, 0);
                    cm.gainExp(7000);
                    cm.dispose();
                    break;
                case 40:
                    cm.warp(980000000, 0);
                    cm.gainExp(4550);
                    cm.dispose();
                    break;
            }
        }
      }
  }
}