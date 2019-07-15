/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package server.transitions;

import handling.channel.ChannelServer;
import server.PropertiesTable;
import tools.TimerTools.MapTimer;
import server.maps.Field;

/**
 * @author GabrielSin
*/

public class AirPlane {

public static PropertiesTable prop = new PropertiesTable();
public long closeTime = 24 * 1000;
public long beginTime = 30 * 1000;
public long rideTime = 6 * 1000; 
public Field kcBfd, planeToCbd, cbdDocked, cbdBfd, planeToKc, kcDocked;
    
    public void Start(ChannelServer channel) {
        kcBfd = channel.getMapFactory().getMap(540010100);
        cbdBfd = channel.getMapFactory().getMap(540010001);
        planeToCbd = channel.getMapFactory().getMap(540010101);
        planeToKc = channel.getMapFactory().getMap(540010002);
        cbdDocked = channel.getMapFactory().getMap(540010000);
        kcDocked = channel.getMapFactory().getMap(103000000);
        scheduleNew();
    }
        
    public final void scheduleNew() {
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
        prop.setProperty("entry", Boolean.TRUE);
        kcBfd.warpEveryone(planeToCbd.getId());
        cbdBfd.warpEveryone(planeToKc.getId());
        MapTimer.getInstance().schedule(() -> {
           arrived();
        }, rideTime);
        scheduleNew();
    }
        
    public void arrived() {
        planeToCbd.warpEveryone(cbdDocked.getId());
        planeToKc.warpEveryone(kcDocked.getId());
    }

    public static PropertiesTable getProperties() {
        return prop;
    }

    public static boolean airPlaneOpen () {
        return getProperties().getProperty("entry").equals(Boolean.TRUE);
    }
}  