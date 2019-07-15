package scripting.portal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import client.Client;
import constants.ServerProperties;
import java.lang.reflect.UndeclaredThrowableException;
import tools.FileLogger;
import server.maps.portal.Portal;

public class PortalScriptManager {

    private static final PortalScriptManager instance = new PortalScriptManager();
    private final Map<String, PortalScript> scripts = new HashMap<>();
    private static ScriptEngineFactory sef;

    private PortalScriptManager() {
        ScriptEngineManager sem = new ScriptEngineManager();
        sef = sem.getEngineByName("javascript").getFactory();
    }

    public static PortalScriptManager getInstance() {
        return instance;
    }

    private PortalScript getPortalScript(String scriptName) {
        if (scripts.containsKey(scriptName)) {
            return scripts.get(scriptName);
        }
        File scriptFile = new File(ServerProperties.Misc.DATA_ROOT + "/Script/portal/" + scriptName + ".js");
        if (!scriptFile.exists()) {
            scripts.put(scriptName, null);
            return null;
        }
        FileReader fr = null;
        ScriptEngine portal = sef.getScriptEngine();
        try {
            fr = new FileReader(scriptFile);
            ((Compilable) portal).compile(fr).eval();
        } catch (ScriptException | IOException | UndeclaredThrowableException e) {
            FileLogger.printError(FileLogger.PORTAL + scriptName + ".txt", e);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    System.out.println("ERROR CLOSING " + e);
                }
            }
        }
        PortalScript script = ((Invocable) portal).getInterface(PortalScript.class);
        scripts.put(scriptName, script);
        return script;
    }

    public boolean executePortalScript(Portal portal, Client c) {
        try {
            PortalScript script = getPortalScript(portal.getScriptName());
            if (script != null) {
                return script.enter(new PortalPlayerInteraction(c, portal));
            }
        } catch (UndeclaredThrowableException ute) {
            FileLogger.printError(FileLogger.PORTAL + portal.getScriptName() + ".txt", ute);
        } catch (final Exception e) {
            FileLogger.printError(FileLogger.PORTAL + portal.getScriptName() + ".txt", e);
        }
        return false;
    }

    public void clearScripts() {
        scripts.clear();
    }
}

