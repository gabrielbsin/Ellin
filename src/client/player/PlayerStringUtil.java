/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package client.player;

import java.util.regex.Pattern;

public class PlayerStringUtil {
    
    private static final Pattern petPattern = Pattern.compile("[a-zA-Z0-9_-]{4,12}");
    private static final Pattern namePattern = Pattern.compile("[a-zA-Z0-9_-]{3,12}");
    
    public static String[] RESERVED = {"Rental", "Donor","MapleNews"};
       
    public static boolean canCreateChar(String name, int world) {
        if (name.length() < 3 || name.length() > 12 || !namePattern.matcher(name).matches() || PlayerQuery.getIdByName(name) != -1) {
            return false;
        }
        for (String z : RESERVED) {
            if (name.indexOf(z) != -1) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasSymbols(String name) {
        String[] symbols = {"`","~","!","@","#","$","%","^","&","*","(",")","_","-","=","+","{","[","]","}","|",";",":","'",",","<",">",".","?","/"};
        for (byte s = 0; s < symbols.length; s++) {
            if (name.contains(symbols[s])) {
                return true;
            }
        }
        return false;
    } 
      
    public static final boolean canChangePetName(final String name) {
        if (petPattern.matcher(name).matches()) {
            for (String z : RESERVED) {
                if (name.indexOf(z) != -1) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
  
    public static String makeMapleReadable(String in) {
        String i = in.replace('I', 'i');
        i = i.replace('l', 'L');
        i = i.replace("rn", "Rn");
        i = i.replace("vv", "Vv");
        i = i.replace("VV", "Vv");
        return i;
    }
    
    public static String getTime(long time){
        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;
        long monthsInMilli = daysInMilli * 30;
        long months = time / monthsInMilli;
        time = time % monthsInMilli;
        long days = time / daysInMilli;
        time = time % daysInMilli;
        long hours = time / hoursInMilli;
        time = time % hoursInMilli;
        long minutes = time / minutesInMilli;
        time = time % minutesInMilli;
        long seconds = time / secondsInMilli;
        time = time % secondsInMilli;
        StringBuilder sb = new StringBuilder();
        if (months > 0){
            sb.append(months).append(months == 1 ? " month" : " months").append((days > 0 || hours > 0 || minutes > 0) ? ", " : "");
        }
        if (days > 0){
            sb.append(days).append(days == 1 ? " day" : " days").append((hours > 0 || minutes > 0) ? ", " : "");
        }
        if (hours > 0){
            sb.append(hours).append(hours == 1 ? " hour" : " hours").append(minutes > 0 ? ", " : "");
        }
        if (minutes > 0){
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        if (sb.length() < 5){
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }
        return sb.toString();
    }
}