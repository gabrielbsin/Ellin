package client.player.commands.object;

import client.Client;
import constants.CommandConstants.CommandType;

public class CommandObject {


    private final String command;
    private final int administrativeLevelReq;
    private final CommandExecute exe;

    public CommandObject(String com, CommandExecute c, int gmLevel) {
        command = com;
        exe = c;
        administrativeLevelReq = gmLevel;
    }

    /**
     * Call this to apply this command to the specified {@link Client}
     * with the specified arguments.
     *
     * @param c the MapleClient to apply this to
     * @param splitted the arguments
     * @return See {@link CommandExecute#execute}
     */
    public boolean execute(Client c, String[] splitted) {
        return exe.execute(c, splitted);
    }

    public CommandType getType() {
        return exe.getType();
    }

    /**
     * Returns the GMLevel needed to use this command.
     *
     * @return the required GM Level
     */
    public int getAdministrativeLevel() {
        return administrativeLevelReq;
    }
}
