package client.player.commands;

import client.Client;
import constants.CommandConstants;
import constants.CommandConstants.CoomandRank;
import handling.world.service.BroadcastService;
import java.util.Arrays;
import launch.Start;
import packet.creators.PacketCreator;
import client.player.Player;
import client.player.PlayerStringUtil;
import client.player.commands.object.CommandExecute;
import constants.ServerProperties;
import scripting.npc.NPCScriptManager;
import server.maps.FieldLimit;
import server.maps.SavedLocationType;
import tools.StringUtil;

/**
 * @author Emilyx3
 * @author GabrielSin (http://forum.ragezone.com/members/822844.html)
 */

public class PlayerCommand {
        
    public static CoomandRank getPlayerLevelRequired() {
        return CoomandRank.NORMAL;
    }

    public static class JoinEvent extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            Player p = c.getPlayer();
            if (p.getSavedLocation(SavedLocationType.EVENT) > 1){
                p.dropMessage( "You can not use the command 2 times, use @leaveevent and enter again!");
                return false;
            } else if (FieldLimit.CHANGECHANNEL.check(p.getMap().getFieldLimit()) || p.getEventInstance() != null || p.getMap().getId() == p.getClient().getChannelServer().getEventMap()) {
                p.dropMessage("You can not use this command here!");
                return false;
            } else if (p.getClient().getChannelServer().getEventStarted()) {
                p.changeMap(p.getClient().getChannelServer().getEventMap(), 0);
                p.saveLocation(SavedLocationType.EVENT);   
                return true;
            } else {
                 p.dropMessage("There are no events at the moment.");
                return false;
            }
        }
    }
    
    public static class LeaveEvent extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            Player p = c.getPlayer();
            if (FieldLimit.CHANGECHANNEL.check(p.getMap().getFieldLimit()) || p.getEventInstance() != null) {
                p.dropMessage("You can not use this command here!");
                return false;
            } else if (p.getSavedLocation(SavedLocationType.EVENT) > 0) {
                p.changeMap(p.getSavedLocation(SavedLocationType.EVENT), 0);
                p.clearSavedLocation(SavedLocationType.EVENT);
                return true;
            } else {
                p.dropMessage("There are no active events at the moment or there is no saved map. Please try again later!");
                return false;
            }
        }
    }
    
    public static class Drops extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9270035);
            return true;
        }
    }
    
    public static class Playtime extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            c.getPlayer().dropMessage("Your playing time is currently " + PlayerStringUtil.getTime(c.getPlayer().getPlaytime()) + ".");
            return false; 
        }
    }
    
    public static class Gm extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            Player p = c.getPlayer();
            if (splitted.length == 1) {
                p.dropMessage("Type: @gm <message>");
                return false;
            }
            String subString = Arrays.toString(splitted).toLowerCase();
            for (int i = 0; i < CommandConstants.HELP_TYPES.length; i++){
                if (subString.contains(CommandConstants.HELP_TYPES[i].toLowerCase())){
                    if (null != CommandConstants.HELP_TYPES[i]) {
                        CommandConstants.HelpCommandGM(CommandConstants.HELP_TYPES[i], p);
                    } 
                }
            }
            if (!p.getCheatTracker().Spam(300000, 0)) { 
                BroadcastService.broadcastGMMessage(PacketCreator.ServerNotice(5, "[GM Message] Channel: " + c.getChannel() + "  " + p.getName()  + StringUtil.joinStringFrom(splitted, 1)));
                p.dropMessage("Message sent.");
                return true;
            } else {
                p.dropMessage("You can only send messages to GM every 5 minutes.");
                return false;
            }
        }
    }
    
    public static class Uptime extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            c.getPlayer().showHint(ServerProperties.Login.SERVER_NAME + " has been online for  " + StringUtil.getReadableMillis(Start.startTime, System.currentTimeMillis()));
            c.getPlayer().dropMessage(6, ServerProperties.Login.SERVER_NAME + " has been online for  " + StringUtil.getReadableMillis(Start.startTime, System.currentTimeMillis()));
            return true;
        }
    }
    
    public static class Dispose extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            NPCScriptManager.getInstance().dispose(c);
            c.getSession().write(PacketCreator.EnableActions());
            c.getPlayer().dropMessage("Done!");
            return true;
        }
    }
    
    public static class help extends Commands {}
    
    public static class Commands extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            final Player p = c.getPlayer();
                p.dropMessage(5, "@joinevent - If an event is in progress, use this to warp to the event map.");
                p.dropMessage(5, "@leaveevent - If an event has ended, use this to warp to your original map.");
                p.dropMessage(5, "@gm <message>: Sends a message to all online GMs in the case of an emergency.");
                p.dropMessage(5, "@dispose - Fixes your character if it is stuck.");
                p.dropMessage(5, "@playtime - Shows the playing time within our server.");
                p.dropMessage(5, "@uptime - Shows how long " + ServerProperties.Login.SERVER_NAME + " has been online.");
                return true;
        }
    }
}
