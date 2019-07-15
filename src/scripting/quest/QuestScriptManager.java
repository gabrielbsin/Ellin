/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting.quest;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;

import scripting.AbstractScriptManager;
import server.quest.MapleQuest;
import tools.FileLogger;
import client.Client;
import javax.script.ScriptException;
import server.quest.MapleQuestStatus;

public class QuestScriptManager extends AbstractScriptManager {
    
    private final Map<Client, QuestActionManager> qms = new HashMap<>();
    private final Map<Client, Invocable> scripts = new HashMap<>();
    private static final QuestScriptManager instance = new QuestScriptManager();

    public static final QuestScriptManager getInstance() {
        return instance;
    }

    public void start(Client c, short questid, int npc) {
        MapleQuest quest = MapleQuest.getInstance(questid);
        if (!c.getPlayer().getQuest(quest).getStatus().equals(MapleQuestStatus.Status.NOT_STARTED)) {
            dispose(c);
            return;
        }
        try {
            QuestActionManager qm = new QuestActionManager(c, questid, npc, true);
            if (qms.containsKey(c)) {
                return;
            }
            qms.put(c, qm);
            Invocable iv = getInvocable("quest/" + questid + ".js", c);
            if (iv == null) {
                FileLogger.printError(FileLogger.QUEST_UNCODED, "Quest " + questid + " is uncoded.\r\n");
            }
            if (iv == null || QuestScriptManager.getInstance() == null) {
                qm.dispose();
                return;
            }
            engine.put("qm", qm);
            scripts.put(c, iv);
            iv.invokeFunction("start", (byte) 1, (byte) 0, 0);
        } catch (final UndeclaredThrowableException ute) {
            FileLogger.printError(FileLogger.QUEST + questid + ".txt", ute);
            dispose(c);
        } catch (final ScriptException | NoSuchMethodException t) {
            FileLogger.printError(FileLogger.QUEST + getQM(c).getQuest() + ".txt", t);
            dispose(c);
        }
    }

    public void start(Client c, byte mode, byte type, int selection) {
        Invocable iv = scripts.get(c);
        if (iv != null) {
            try {
                iv.invokeFunction("start", mode, type, selection);
            } catch (final UndeclaredThrowableException | ScriptException | NoSuchMethodException ute) {
                FileLogger.printError(FileLogger.QUEST + getQM(c).getQuest() + ".txt", ute);
                dispose(c);
            }
        }
    }

    public void end(Client c, short questid, int npc) {
        MapleQuest quest = MapleQuest.getInstance(questid);
        if (!c.getPlayer().getQuest(quest).getStatus().equals(MapleQuestStatus.Status.STARTED) || !c.getPlayer().getMap().containsNPC(npc)) {
            dispose(c);
            return;
        }
        try {
            QuestActionManager qm = new QuestActionManager(c, questid, npc, false);
            if (qms.containsKey(c)) {
                return;
            }
            qms.put(c, qm);
            Invocable iv = getInvocable("quest/" + questid + ".js", c);
            if (iv == null) {
                qm.dispose();
                return;
            }
            engine.put("qm", qm);
            scripts.put(c, iv);
            iv.invokeFunction("end", (byte) 1, (byte) 0, 0);                    
        } catch (final UndeclaredThrowableException ute) {
            FileLogger.printError(FileLogger.QUEST + questid + ".txt", ute);
            dispose(c);
        } catch (final ScriptException | NoSuchMethodException t) {
            FileLogger.printError(FileLogger.QUEST + getQM(c).getQuest() + ".txt", t);
            dispose(c);
	}
    }

    public void end(Client c, byte mode, byte type, int selection) {
        Invocable iv = scripts.get(c);
        if (iv != null) {
            try {
                iv.invokeFunction("end", mode, type, selection);
            } catch (final UndeclaredThrowableException | ScriptException | NoSuchMethodException ute) {
                FileLogger.printError(FileLogger.QUEST + getQM(c).getQuest() + ".txt", ute);
                dispose(c);
            }
        }
    }

    public void dispose(QuestActionManager qm, Client c) {
        qms.remove(c);
        scripts.remove(c);
        resetContext("quest/" + qm.getQuest() + ".js", c);
    }

    public void dispose(Client c) {
        QuestActionManager qm = qms.get(c);
        if (qm != null) {
            dispose(qm, c);
        }
    }

    public QuestActionManager getQM(Client c) {
        return qms.get(c);
    }
}
