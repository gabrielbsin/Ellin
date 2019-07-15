package client.player.commands.object;

import client.Client;
import constants.CommandConstants.CommandType;

/**
 * Interface for the executable part of a {@link CommandObject}.
 *
 */
public abstract class CommandExecute {

    /**
     * The method executed when this command is used.
     *
     * @param c the client executing this command
     * @param splitted the command and any arguments attached
     *
     * @return 1 if you want to log the command, 0 if not. TODO: USE {@link #ReturnValue}
     */
    public abstract boolean execute(Client c, String[] splitted);

    enum ReturnValue {
        DONT_LOG,
        LOG;
    }

    public CommandType getType() {
        return CommandType.NORMAL;
    }

    public static abstract class TradeExecute extends CommandExecute {

        @Override
        public CommandType getType() {
            return CommandType.TRADE;
        }
    }
}
