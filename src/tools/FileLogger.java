package tools;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FileLogger {
    
    private static final SimpleDateFormat SIMPLE_DATE = new SimpleDateFormat("dd-MM-yyyy");
    private static final SimpleDateFormat SIMPLE_DATE_ = new SimpleDateFormat("HH:mm");

    public static final String 
            
        NPC = "Npcs/",
        ITEM = "Items/",
        QUEST = "Quests/",
        PORTAL = "Portals/",
        REACTOR = "Reactors/",
        EXPLOITS = "Exploits/",
        FREDRICK = "fredrick/",
        FIELD = "Field/map.txt",
        INVOCABLE = "Invocable/",
        PACKET_LOG = "Packets/log.txt",
        DEADLOCK_ERROR = "deadlocks.txt",
        DEADLOCK_STACK = "deadlocks/path.txt",
        DEADLOCK_LOCKS = "deadlocks/locks.txt",
        DEADLOCK_STATE = "deadlocks/state.txt",
        EXCEPTION = "Exception/exceptions.txt",
        ACCOUNT_STUCK = "Stuck/AccountStuck.txt",
        PLAYER_STUCK = "Stuck/PlayerStuck.txt",
        DATABASE_EXCEPTION = "Exception/Database/",
        COMMANDS_EXCEPTION = "Exception/Commands/",
        QUEST_UNCODED = "Quests/uncodedQuests.txt",
        EXCEPTION_CAUGHT = "Exception/ExceptionCaught.txt";
    
    private static final String FILE_PATH = "MSLog/" + SIMPLE_DATE.format(Calendar.getInstance().getTime()) + "/";
    
    public static void logPacket(final String file, final String msg) {
        logETCs(file, msg, true);
    }

    public static void log(final String file, final String msg) {
	FileOutputStream out = null;
	try {
	    out = new FileOutputStream(file, true);
	    out.write(msg.getBytes());
	    out.write("\n------------------------\n".getBytes());
	} catch (IOException ess) {
	} finally {
	    try {
		if (out != null) {
		    out.close();
		}
	    } catch (IOException ignore) {
	    }
	}
    }
    
    public static void logETCs(final String file, final String msg, final boolean packet) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream((packet ? "../MSLog/packet\\" : "../MSLog/Usuarios\\")+ file + ".rtf", true);
            out.write(("\n------------------------ " + CurrentReadable_Time() + " ------------------------\n").getBytes());
            out.write(msg.getBytes());
        } catch (IOException ess) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
        }
    }
    
    public static final String CurrentReadable_Time() {
	return SIMPLE_DATE.format(Calendar.getInstance().getTime());
    }
    
    public static void printError(final String name, final Throwable t) {
        FileOutputStream out = null;
        final String file = FILE_PATH + "error/" + name;
        try {
            File outputFile = new File(file);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(file, true);
            out.write(getString(t).getBytes());
            out.write("\n---------------------------------\r\n".getBytes());
        } catch (IOException ess) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    public static void printError(final String name, final Throwable t, final String info) {
        FileOutputStream out = null;
        final String file = FILE_PATH + "error/" + name;
        try {
            File outputFile = new File(file);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(file, true);
            out.write((info + "\r\n").getBytes());
            out.write(getString(t).getBytes());
            out.write("\n---------------------------------\r\n".getBytes());
        } catch (IOException ess) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    public static void printError(final String name, final String s) {
        FileOutputStream out = null;
        final String file = FILE_PATH + "error/" + name;
        try {
            File outputFile = new File(file);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(file, true);
            out.write(s.getBytes());
        } catch (IOException ess) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    public static void print(final String name, final String s) {
        print(name, s, true);
    }
    

    public static void print(final String name, final String s, boolean line) {
        FileOutputStream out = null;
        String file = FILE_PATH + name;
        try {
            File outputFile = new File(file);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(file, true);
            out.write(s.getBytes());
            out.write("\r\n".getBytes());
            if (line) {
                out.write("---------------------------------\r\n".getBytes());
            }
        } catch (IOException ess) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
        }
    }
    
    private static String getString(final Throwable e) {
        String retValue = null;
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            retValue = sw.toString();
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
                if (sw != null) {
                    sw.close();
                }
            } catch (IOException ignore) {
            }
        }
        return retValue;
    }
}