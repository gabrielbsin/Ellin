/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package server.transitions;

import handling.channel.ChannelServer;
import tools.TimerTools.MapTimer;
import server.PropertiesTable;
import server.maps.Field;

/**
 * @author GabrielSin
 */
public class Subway {
 
public long closeTime = 30 * 1000;
public long beginTime = 30 * 1000;
public long rideTime = 30 * 1000; 
public static PropertiesTable prop = new PropertiesTable();  
public Field kcWaiting, subwayToKc, kcDocked, nlcWaiting, subwayToNlc, nlcDocked;   
    
    public void Start(ChannelServer channel) {
        kcWaiting = channel.getMapFactory().getMap(600010004);
        nlcWaiting =channel.getMapFactory().getMap(600010002);
        subwayToKc = channel.getMapFactory().getMap(600010003);
        subwayToNlc = channel.getMapFactory().getMap(600010005);
        kcDocked = channel.getMapFactory().getMap(103000100);
        nlcDocked = channel.getMapFactory().getMap(600010001);
        scheduleNew();
    }

    public final void scheduleNew() {
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
        prop.setProperty("docked", Boolean.FALSE);
        kcWaiting.warpEveryone(subwayToNlc.getId());
        nlcWaiting.warpEveryone(subwayToKc.getId());

        MapTimer.getInstance().schedule(() -> {
            arrived();
        }, rideTime);
    }

    public void arrived() {
       subwayToKc.warpEveryone(kcDocked.getId());
       subwayToNlc.warpEveryone(nlcDocked.getId());
       scheduleNew();
    }

    public static PropertiesTable getProperties() {
        return Genie.prop;
    }

    public static boolean subwayOpen() {
        return getProperties().getProperty("entry").equals(Boolean.TRUE);
    }

    public static boolean subwayDocked() {
        return getProperties().getProperty("docked").equals(Boolean.TRUE);
    }
}  