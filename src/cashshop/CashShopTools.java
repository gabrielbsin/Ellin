/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cashshop;

import client.Client;
import client.player.PlayerJob;
import static client.player.PlayerJob.CLASS_BOWMAN;
import static client.player.PlayerJob.CLASS_MAGICIAN;
import static client.player.PlayerJob.CLASS_PIRATE;
import static client.player.PlayerJob.CLASS_THIEF;
import static client.player.PlayerJob.CLASS_WARRIOR;
import client.player.inventory.types.InventoryType;
import java.util.Calendar;

/**
 * 
 * @author GabrielSin
 */
public class CashShopTools {

    public static int getMaxInventorySlots(int job, InventoryType type) {
        int max = 48;
        switch (PlayerJob.getJobPath(job)) {
            case CLASS_WARRIOR:
                switch (type) {
                    case EQUIP:
                    case SETUP:
                        max += 4;
                        break;
                    case USE:
                    case ETC:
                        max += 4;
                        if (PlayerJob.getAdvancement(job) > 1) {
                            max += 4;
                        }
                    break;
                }
            break;
            case CLASS_MAGICIAN:
                switch (type) {
                    case ETC:
                        if (PlayerJob.getAdvancement(job) > 1) {
                             max += 4;
                        }
                        break;
                    }
            break;
            case CLASS_BOWMAN:
                switch (type) {
                    case EQUIP: 
                    case USE:
                        max += 4;
                        break;
                    case ETC:
                        if (PlayerJob.getAdvancement(job) > 1) {
                            max += 4;
                        }
                    break;
                }
            break;
            case CLASS_THIEF:
            case CLASS_PIRATE:
                switch (type) {
                    case EQUIP: 
                    case ETC:
                        max += 4;
                        break;
                    case USE:
                        if (PlayerJob.getAdvancement(job) > 1) {
                            max += 4;
                        }
                    break;
                }
            break;
        }
      return max;
    }
    
    public static boolean checkBirthday(Client c, int idate) {
        int year = idate / 10000;
        int month = (idate - year * 10000) / 100;
        int day = idate - year * 10000 - month * 100;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(year, month - 1, day);
        return c.checkBirthDate(cal);
    }
}
