/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package server.events;

import client.player.Player;
import handling.channel.ChannelServer;
import packet.creators.PacketCreator;
import tools.TimerTools;
import server.PropertiesTable;
import server.maps.Field;
import server.maps.object.FieldObject;
import tools.FileLogger;

/**
 * @author GabrielSin
 * Russian Roulette
 * LeaderMS 2016
 * @Amoria
 */
public class RussianRoulette {

    public static PropertiesTable propriedades;
    private int aleatorio = 0;
    private final Field eventMap;
    
    public RussianRoulette() {  
        RussianRoulette.propriedades = new PropertiesTable();
        propriedades.setProperty("eventOpen", Boolean.TRUE);
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            cserv.broadcastPacket(PacketCreator.ServerNotice(6, "[Roleta Russa | Evento] Fale com o Jean (NPC) no canal (1), o evento fecha em 2 minutos."));   
        } 
        ChannelServer cs = ChannelServer.getInstance(1);
        eventMap = cs.getMapFactory().getMap(670010400);
        TimerTools.MapTimer.getInstance().schedule(() -> {
            propriedades.setProperty("eventOpen", Boolean.FALSE);
            PlataformaAleatoria();
            GeraNumeroPlataforma();
        }, 2 * (60 * 1000));  
        eventMap.addFieldTimer(120);   
    }
    
    public final void PlataformaAleatoria() {
        eventMap.broadcastMessage(PacketCreator.GetClockTimer(15));
        eventMap.broadcastMessage(PacketCreator.ServerNotice(6, "[Roleta Russa | Evento] Escolha uma plataforma antes do tempo acabar."));
        TimerTools.MapTimer.getInstance().schedule(() -> {
            eventMap.broadcastMessage(PacketCreator.ServerNotice(6, "[Roleta Russa | Evento] A próxima rodada vai começar em 15 segundos. Prepare-se!"));
            ChecaPlataformaAleatoria();
        }, 15000);  
    } 
    
    public void GeraNumeroPlataforma() {
        aleatorio = (int) (Math.floor(Math.random() * 9));
        if (aleatorio == 0) aleatorio = 1;
        System.out.println("[DEBUG] GeraNumeroPlataforma: " + aleatorio);
    }
    
    public int verifyPlataform(Player p) {
        if (p == null) {
            return -1;
        }
        final double posX = p.getPosition().getX();
        final double posY = p.getPosition().getY();
        if (posX > 1428 && posX < 1501 && posY == 49) {
            System.out.println("[DEBUG] verifyPlataform: 1 || Player > " + p.getName());
            return 1;
        } else if (posX > 1600 && posX < 1680 && posY == 49) {
            System.out.println("[DEBUG] verifyPlataform: 2 || Player > " + p.getName());
            return 2;
        } else if (posX > 1759 && posX < 1830 && posY == 49) {
            System.out.println("[DEBUG] verifyPlataform: 3 || Player > " + p.getName());
            return 3;
        } else if (posX > 1910 && posX < 1990 && posY == 49) {
            System.out.println("[DEBUG] verifyPlataform: 4 || Player > " + p.getName());
            return 4;
        } else if (posX > 1340 && posX < 1415 && posY == 201) {
            System.out.println("[DEBUG] verifyPlataform: 5 || Player > " + p.getName());
            return 5;
        } else if (posX > 1570 && posX < 1581 && posY == 201) {
            System.out.println("[DEBUG] verifyPlataform: 6 || Player > " + p.getName());
            return 6;
        } else if (posX > 1669 && posX < 1743 && posY == 201) {
            System.out.println("[DEBUG] verifyPlataform: 7 || Player > " + p.getName());
            return 7;
        } else if (posX > 1800 && posX < 1910 && posY == 201) {
            System.out.println("[DEBUG] verifyPlataform: 8 || Player > " + p.getName());
            return 8;
        } else if (posX > 1990 && posX < 2075 && posY == 201) {
            System.out.println("[DEBUG] verifyPlataform: 9 || Player > " + p.getName());
            return 9;
        } else {
            System.out.println("[DEBUG] verifyPlataform: -1 || Player > " + p.getName());
            return -1;
        }
    }
    
    public int plataformSize(int plataform) {
        int playerCount = 0;
        for (final FieldObject o : eventMap.getAllPlayer()) {
            if (verifyPlataform(((Player)o)) == plataform) { 
                playerCount++;
            }
        }
      System.out.println("[DEBUG] plataformSize [" + plataform + "]: " + playerCount);
      return playerCount;
    }
    
    public void playerDeadBox(Player p) {
        if (p != null) {
            p.kill(); 
            TimerTools.MapTimer.getInstance().schedule(() -> {
                p.changeMap(p.getMap().getReturnField());
            }, 2000); 
        }  
    }
    
    public final void ChecaPlataformaAleatoria() { 
        for (final FieldObject o : eventMap.getAllPlayer()) {
            if (verifyPlataform(((Player)o)) == aleatorio) {
                playerDeadBox(((Player)o));
            } else if (verifyPlataform(((Player)o)) == -1) {
                playerDeadBox(((Player)o));                
            }
        }

        TimerTools.MapTimer.getInstance().schedule(() -> {
            verifyWinner();
        }, 4000);
   }  
     
    public final void verifyWinner() {
        int winners = 0;
        int[] countPlataforms = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (final FieldObject o : eventMap.getAllPlayer()) {
            for (int i = 1; i < 10; i++) {
                if (plataformSize(i) >= 1) {
                    for (int actual : countPlataforms) {
                         if (actual == i) {
                            return;
                        }
                        if (plataformSize(actual) == 0) {
                            if (verifyPlataform(((Player)o)) == i) {
                                System.out.println("[DEBUG] verifyPlataform: " + verifyPlataform(((Player)o)));
                                FileLogger.print("RoletaRussa.txt", "Ganhador: " + ((Player)o).getName() + " | Level: " + ((Player)o).getLevel() + ".");
                                ((Player)o).dropMessage("[Roleta Russa | Evento] Parabéns, você foi um vencedor.");
                                ((Player)o).changeMap(((Player)o).getMap().getReturnField());
                                winners++;
                            }
                        }
                    }
                }
            } 
        }
        System.out.println("[DEBUG] winners: " + winners);
        if (winners < 1 && eventMap.getCharactersSize() > 0) {
            eventMap.broadcastMessage(PacketCreator.GetClockTimer(15)); 
            eventMap.broadcastMessage(PacketCreator.ServerNotice(6, "[Roleta Russa | Evento] Escolha a próxima plataforma antes do tempo acabar."));
            TimerTools.MapTimer.getInstance().schedule(() -> {
                GeraNumeroPlataforma();
                ChecaPlataformaAleatoria();
            }, 15000);     
        } else if (winners > 0) {
            TimerTools.MapTimer.getInstance().schedule(() -> {
               RussianRoulette roletaRussa = new RussianRoulette();
            }, 1 * 1000 * 60 * 60 * 24);
        }   
    }
    
    public static PropertiesTable getProperties() {
        return RussianRoulette.propriedades;
    }
        
    public static boolean RoletaDisponivel () {
        return getProperties().getProperty("eventOpen").equals(Boolean.TRUE);
    }    
}




