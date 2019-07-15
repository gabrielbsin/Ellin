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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scripting;

import java.io.File;
import java.io.FileReader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import client.Client;
import constants.ServerProperties;
import java.io.IOException;
import javax.script.ScriptException;
import tools.FileLogger;

/**
 *
 * @author Matze
 */
public abstract class AbstractScriptManager {

	protected ScriptEngine engine;
	private ScriptEngineManager sem;
        
	protected AbstractScriptManager() {
		sem = new ScriptEngineManager();
	}
	
	protected Invocable getInvocable(String path, Client c) {
            path = ServerProperties.Misc.DATA_ROOT + "/Script/" + path;
            engine = null;
            if (c != null) {
                engine = c.getScriptEngine(path);
            }
            if (engine == null) {
                File scriptFile = new File(path);
                if (!scriptFile.exists()) {
                    return null;
                }
                engine = sem.getEngineByName("javascript");
                if (c != null) {
                    c.setScriptEngine(path, engine);
                }
                try (FileReader fr = new FileReader(scriptFile)) {
                    if (ServerProperties.Misc.USE_JAVA8){
            		engine.eval("load('nashorn:mozilla_compat.js');");
                    }
                    engine.eval(fr);
                } catch (final ScriptException | IOException t) {
                    FileLogger.printError(FileLogger.INVOCABLE + path.substring(12, path.length()), t, path);
                    return null;
                }
            }
          return (Invocable) engine;
        }
	 
        protected void resetContext(String path, Client c) {
            c.removeScriptEngine(ServerProperties.Misc.DATA_ROOT + "/Script/" + path);
        }
}
