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

public class Cabin {

public long closeTime = 60 * 1000;
public long beginTime = 60 * 1000;
public long rideTime = 60 * 1000;
public static PropertiesTable prop = new PropertiesTable();
public Field orbisBtf, leafreBtf, cabinToOrbis, cabinToLeafre, orbisDocked ,leafreDocked, orbisStation, leafreStation;

    public void Start(ChannelServer channel) {
        orbisBtf = channel.getMapFactory().getMap(200000132);
        leafreBtf = channel.getMapFactory().getMap(240000111);
        cabinToOrbis = channel.getMapFactory().getMap(200090210);
        cabinToLeafre = channel.getMapFactory().getMap(200090200);
        orbisDocked = channel.getMapFactory().getMap(200000131);
        leafreDocked = channel.getMapFactory().getMap(240000110);
        orbisStation = channel.getMapFactory().getMap(200000100);
        leafreStation = channel.getMapFactory().getMap(240000100);
        scheduleNew();
    }
        
    public final void scheduleNew() {
        leafreDocked.setDocked(true);
        orbisDocked.setDocked(true);
        
        leafreDocked.broadcastMessage(PacketCreator.ShipEffect(true));
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
        leafreDocked.setDocked(false);
        orbisDocked.setDocked(false);
        
        leafreDocked.broadcastMessage(PacketCreator.ShipEffect(false));
        orbisDocked.broadcastMessage(PacketCreator.ShipEffect(false));
        
        prop.setProperty("docked", Boolean.FALSE);
        orbisBtf.warpEveryone(cabinToLeafre.getId());
        leafreBtf.warpEveryone(cabinToOrbis.getId());
        MapTimer.getInstance().schedule(() -> {
            arrived();
        }, rideTime);
    }

    public void arrived() {
        cabinToOrbis.warpEveryone(orbisStation.getId());
        cabinToLeafre.warpEveryone(leafreStation.getId());
        scheduleNew();
    }
        
    public static PropertiesTable getProperties() {
        return Cabin.prop;
    }

    public static boolean cabinOpen () {
        return getProperties().getProperty("entry").equals(Boolean.TRUE);
    }      
}  