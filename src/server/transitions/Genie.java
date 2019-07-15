/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package server.transitions;

import handling.channel.ChannelServer;
import packet.creators.PacketCreator;
import tools.TimerTools;
import server.PropertiesTable;
import server.maps.Field;

/**
* @author GabrielSin
*/

public class Genie {
 
public long closeTime = 60 * 1000;
public long beginTime = closeTime;
public long rideTime = closeTime; 
public static PropertiesTable prop = new PropertiesTable(); 
public Field orbisBrf, genieToOrbis, orbisDocked, arianteBtf, genieToAriant ,ariantDocked, orbisStation;    
    
    public void Start(ChannelServer channel) {
        orbisBrf = channel.getMapFactory().getMap(200000152);
        arianteBtf = channel.getMapFactory().getMap(260000110);
        genieToOrbis = channel.getMapFactory().getMap(200090410);
        genieToAriant = channel.getMapFactory().getMap(200090400);
        orbisDocked = channel.getMapFactory().getMap(200000151);
        ariantDocked = channel.getMapFactory().getMap(260000100);
        orbisStation = channel.getMapFactory().getMap(200000100);
        scheduleNew();
    }

    public final void scheduleNew() {
        ariantDocked.setDocked(true);
        orbisDocked.setDocked(true);
        
        ariantDocked.broadcastMessage(PacketCreator.ShipEffect(true));
        orbisDocked.broadcastMessage(PacketCreator.ShipEffect(true));    
        
        prop.setProperty("docked", Boolean.TRUE);
        prop.setProperty("entry", Boolean.TRUE);
        TimerTools.MapTimer.getInstance().schedule(() -> {
            stopEntry();
        }, closeTime);
        TimerTools.MapTimer.getInstance().schedule(() -> {
            takeoff();
        }, beginTime);
    }

    public void stopEntry() {
        prop.setProperty("entry", Boolean.FALSE);
    }

    public void takeoff() {
        prop.setProperty("docked", Boolean.FALSE);
        ariantDocked.setDocked(false);
        orbisDocked.setDocked(false);
        
        ariantDocked.broadcastMessage(PacketCreator.ShipEffect(false));
        orbisDocked.broadcastMessage(PacketCreator.ShipEffect(false));
        
        orbisBrf.warpEveryone(genieToAriant.getId());
        arianteBtf.warpEveryone(genieToOrbis.getId());
        TimerTools.MapTimer.getInstance().schedule(() -> {
            arrived();
        }, rideTime);
    }
    
    public void arrived() {
        genieToOrbis.warpEveryone(orbisStation.getId());
        genieToAriant.warpEveryone(ariantDocked.getId());
        scheduleNew();
    }
        
    public static PropertiesTable getProperties() {
        return Genie.prop;
    }

    public static boolean genioOpen() {
        return getProperties().getProperty("entry").equals(Boolean.TRUE);
    }
}  