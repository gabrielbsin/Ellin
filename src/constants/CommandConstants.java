/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package constants;

import client.player.Player;

/*
 * GabrielSin (http://forum.ragezone.com/members/822844.html)
 * Ellin v.62
 * GameConstants
 */
public class CommandConstants {
    
    public final static String[] HELP_TYPES = {"bug", "quest", "party", "party", "npcs", "mob"};
    
    public static String HelpCommandGM (String type, Player p) {
        switch (type) {
            case "bug":
                p.dropMessage("[Auto-Help] Desc.");
                break;
            case "quest":
                p.dropMessage("[Auto-Help] Desc.");
                break;
            case "party":
                p.dropMessage("[Auto-Help] Desc.");
                break;
            case "npcs":
                p.dropMessage("[Auto-Help] Desc.");
                break;
            case "mob":
                p.dropMessage("[Auto-Help] Desc.");
                break;
        }
        return "";
    }
    
    public static enum CommandType {

        NORMAL(0),
        TRADE(1);
        private final int level;

        CommandType(int level) {
            this.level = level;
        }

        public int getType() {
            return level;
        }
    }
    
    public static enum CoomandRank {

        NORMAL('@', 0),
        DONOR('#', 1),
        GM('!', 2),
        ADMIN('!', 3);
        
        private final char prefix;
        private final int level;

        CoomandRank(char ch, int level) {
            prefix = ch;
            this.level = level;
        }

        public char getCommandPrefix() {
            return prefix;
        }

        public int getLevel() {
            return level;
        }
    }
}
