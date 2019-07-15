/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package server.transitions;

import handling.channel.ChannelServer;
import packet.creators.PacketCreator;
import server.PropertiesTable;
import server.maps.Field;
import tools.TimerTools.MapTimer;

/**
 * @author GabrielSin
 */
public class Trains {

public long closeTime = 60 * 1000;
public long beginTime = 60 * 1000;
public long rideTime = 60 * 1000;
private static PropertiesTable prop = new PropertiesTable();
public Field orbisBtf, trainToOrbis, orbisDocked, ludibriumBtf, trainToLudibrium, ludibriumDocked, orbisStation, ludibriumStation;    

   public void Start(ChannelServer channel) {
        orbisBtf = channel.getMapFactory().getMap(200000122);
        ludibriumBtf = channel.getMapFactory().getMap(220000111);
        trainToOrbis = channel.getMapFactory().getMap(200090110);
        trainToLudibrium = channel.getMapFactory().getMap(200090100);
        orbisDocked = channel.getMapFactory().getMap(200000121);
        ludibriumDocked = channel.getMapFactory().getMap(220000110);
        orbisStation = channel.getMapFactory().getMap(200000100);
        ludibriumStation = channel.getMapFactory().getMap(220000100);
        scheduleNew(); 
   }

    public final void scheduleNew() {
        ludibriumDocked.setDocked(true);
        orbisDocked.setDocked(true);
        
        ludibriumDocked.broadcastMessage(PacketCreator.ShipEffect(true));
        orbisDocked.broadcastMessage(PacketCreator.ShipEffect(true));
        
        prop.setProperty("docked", Boolean.TRUE);
        prop.setProperty("entry", Boolean.TRUE);
        MapTimer.getInstance().schedule(() -> {
            stopEntry();
        }, closeTime);
        MapTimer.getInstance().schedule(() -> {
            takeoff();
        }, beginTime);
    }

    public void stopEntry() {
        prop.setProperty("entry", Boolean.FALSE);
    }

    public void takeoff() {
        ludibriumDocked.setDocked(false);
        orbisDocked.setDocked(false);
        
        ludibriumDocked.broadcastMessage(PacketCreator.ShipEffect(false));
        orbisDocked.broadcastMessage(PacketCreator.ShipEffect(false));
        
        prop.setProperty("docked", Boolean.FALSE);
        orbisBtf.warpEveryone(trainToLudibrium.getId());
        ludibriumBtf.warpEveryone(trainToOrbis.getId());
        MapTimer.getInstance().schedule(() -> {
            arrived();
        }, rideTime);
    }

    public void arrived() {
        trainToOrbis.warpEveryone(orbisStation.getId());
        trainToLudibrium.warpEveryone(ludibriumStation.getId());
        scheduleNew();
    }

    public static PropertiesTable getProperties() {
        return Trains.prop;
    }

    public static boolean trainsDocked() {
        return getProperties().getProperty("docked").equals(Boolean.TRUE);
    }

    public static boolean trainsOpen() {
        return getProperties().getProperty("entry").equals(Boolean.TRUE);
    }   
}  