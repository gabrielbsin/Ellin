/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
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
package client.player.commands.object;

import client.Client;
import java.util.ArrayList;
import constants.CommandConstants.CommandType;
import static constants.CommandConstants.CommandType.TRADE;
import constants.CommandConstants.CoomandRank;
import database.DatabaseConnection;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import client.player.Player;
import client.player.commands.AdminCommand;
import client.player.commands.DonorCommand;
import client.player.commands.GMCommand;
import client.player.commands.PlayerCommand;
import tools.FileLogger;

public class CommandProcessor {

    private final static HashMap<String, CommandObject> commands = new HashMap<>();
    private final static HashMap<Integer, ArrayList<String>> commandList = new HashMap<>();

    static {

        Class<?>[] CommandFiles = {
            PlayerCommand.class, DonorCommand.class, GMCommand.class, AdminCommand.class
        };

        for (Class<?> clasz : CommandFiles) {
            try {
                CoomandRank rankNeeded = (CoomandRank) clasz.getMethod("getPlayerLevelRequired", new Class<?>[]{}).invoke(null, (Object[]) null);
                Class<?>[] a = clasz.getDeclaredClasses();
                ArrayList<String> cL = new ArrayList<>();
                for (Class<?> c : a) {
                    try {
                        if (!Modifier.isAbstract(c.getModifiers()) && !c.isSynthetic()) {
                            Object o = c.newInstance();
                            boolean enabled;
                            try {
                                enabled = c.getDeclaredField("enabled").getBoolean(c.getDeclaredField("enabled"));
                            } catch (NoSuchFieldException ex) {
                                enabled = true; 
                            }
                            if (o instanceof CommandExecute && enabled) {
                                cL.add(rankNeeded.getCommandPrefix() + c.getSimpleName().toLowerCase());
                                commands.put(rankNeeded.getCommandPrefix() + c.getSimpleName().toLowerCase(), new CommandObject(rankNeeded.getCommandPrefix() + c.getSimpleName().toLowerCase(), (CommandExecute) o, rankNeeded.getLevel()));;
                            }
                        }
                    } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | SecurityException ex) {
                        FileLogger.printError(FileLogger.COMMANDS_EXCEPTION, ex);
                    }
                }
                Collections.sort(cL);
                commandList.put(rankNeeded.getLevel(), cL);
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
                FileLogger.printError(FileLogger.COMMANDS_EXCEPTION, ex);
            }
        }
    }

    private static void showMessage(Client c, String msg, CommandType type) {
        switch (type) {
            case NORMAL:
                c.getPlayer().dropMessage(6, msg);
                break;
            case TRADE:
                c.getPlayer().dropMessage(-2, "Error : " + msg);
                break;
        }
    }

    public static boolean processCommand(Client c, String line, CommandType type) {
        Player p = c.getPlayer(); 
        if (p != null) {
            if (line.charAt(0) == CoomandRank.NORMAL.getCommandPrefix()) {
                String[] splitted = line.split(" ");
                splitted[0] = splitted[0].toLowerCase();

                CommandObject commandOb = commands.get(splitted[0]);
                if (commandOb == null || commandOb.getType() != type) {
                    showMessage(c, "That player command does not exist.", type);
                    return true;
                }
                try {
                    boolean ret = commandOb.execute(c, splitted);
                } catch (Exception e) {
                        showMessage(c, "There was an error.", type);
                    if (p.isGameMaster()) {
                        showMessage(c, "Error: " + e, type);
                    }
                }
                return true;
            }

            if (p.getAdministrativeLevel() > CoomandRank.NORMAL.getLevel()) {
                if (line.charAt(0) == CoomandRank.DONOR.getCommandPrefix() || line.charAt(0) == CoomandRank.GM.getCommandPrefix() || line.charAt(0) == CoomandRank.ADMIN.getCommandPrefix()) {

                    String[] splitted = line.split(" ");
                    splitted[0] = splitted[0].toLowerCase();

                    if (line.charAt(0) == '!') { 
                        CommandObject commandOb = commands.get(splitted[0]);
                        if (commandOb == null || commandOb.getType() != type) {
                            showMessage(c, "Esse comando não existe.", type);
                            return true;
                        }
                        if (p.getAdministrativeLevel() >= commandOb.getAdministrativeLevel()) {
                            boolean ret = commandOb.execute(c, splitted);
                            if (ret) { 
                                saveCommandsDatabase(c.getPlayer(), line);
                            }
                        } else {
                            showMessage(c, "Você não tem privilégios para usar esse comando.", type);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void saveCommandsDatabase(Player p, String command) {
        PreparedStatement ps = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO gmlog (cid, command, mapid) VALUES (?, ?, ?)");
            ps.setInt(1, p.getId());
            ps.setString(2, command);
            ps.setInt(3, p.getMap().getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, ex);
        } finally {
            try {
                if (ps != null) {
                   ps.close();
                }
            } catch (SQLException e) {

            }
        }
    }
}
