package scripting.npc;

import java.util.List;
import java.util.Map;
import javax.script.Invocable;
import client.Client;
import client.player.Player;
import client.player.violation.CheatingOffense;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.WeakHashMap;
import community.MaplePartyCharacter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import packet.creators.PacketCreator;
import scripting.AbstractScriptManager;
import server.partyquest.mcpq.MCParty;
import tools.FileLogger;

/**
 * @author Matze
 */

public class NPCScriptManager extends AbstractScriptManager {

    private final transient Lock npcLock = new ReentrantLock();
    private final Map<Client, NPCScript> scripts = new WeakHashMap<>();
    private final Map<Client, NPCConversationManager> cms = new WeakHashMap<>();
    private static final NPCScriptManager instance = new NPCScriptManager();

    public static final NPCScriptManager getInstance() {
	return instance;
    }
    
    public final void start(final Client c, final int npc) {
        start(c, npc, null, null);
    }

    public void start(String filename, Client c, int npc, List<MaplePartyCharacter> chrs, MCParty pty) {
        npcLock.lock();
        try {
            NPCConversationManager cm = new NPCConversationManager(c, npc, chrs, pty);
            cm.dispose();
            if (cms.containsKey(c)) {
                return;
            }
            if (c.canClickNPC()) {
                cms.put(c, cm);
                Invocable iv = null;   
                if (filename != null) {
                    iv = getInvocable("npc/" + filename + ".js", c);
                }    
                NPCScriptManager npcsm = NPCScriptManager.getInstance();
                if (iv == null || NPCScriptManager.getInstance() == null) {
                    cm.dispose();
                    return;
                }
                if (npcsm == null) {
                    cm.dispose();
                    return;
                }
                engine.put("cm", cm);
                NPCScript ns = iv.getInterface(NPCScript.class);
                scripts.put(c, ns);
                ns.start(chrs, pty);
                c.setClickedNPC();
            } else {
                c.announce(PacketCreator.EnableActions());
            }
        } catch (Exception e) {
            FileLogger.printError(FileLogger.NPC + npc + ".txt", e);
            dispose(c);
            cms.remove(c);
        } finally {
            npcLock.unlock();
        }		
    }
  
    public void start(Client c, int npc, String filename, Player chr) {
        npcLock.lock();
        try {
            NPCConversationManager cm = new NPCConversationManager(c, npc);
            if (cms.containsKey(c)) {
                return;
            }
            if (c.canClickNPC()) {
                cms.put(c, cm);
                Invocable iv = null;
                String path = "";
                if (filename != null) {
                    path = "npc/" + filename + ".js";
                    iv = getInvocable("npc/" + filename + ".js", c);
                }
                if (iv == null) {
                    path = "npc/" + npc + ".js";
                    iv = getInvocable("npc/" + npc + ".js", c);
                }
                if (iv == null || NPCScriptManager.getInstance() == null) {
                    dispose(c);
                    return;
                }
                engine.put("cm", cm);
                NPCScript ns = iv.getInterface(NPCScript.class);
                scripts.put(c, ns);
                c.setClickedNPC();
                if (chr == null) {
                    ns.start();
                } else {
                    ns.start(chr);
                }
            } else {
                c.announce(PacketCreator.EnableActions());
            }
        } catch (UndeclaredThrowableException ute) {
            FileLogger.printError(FileLogger.NPC + npc + ".txt", ute);
            dispose(c);
            cms.remove(c);
            notice(c, npc);
        } catch (Exception e) {
            FileLogger.printError(FileLogger.NPC + npc + ".txt", e);
            dispose(c);
            cms.remove(c);
            notice(c, npc);
        } finally {
            npcLock.unlock();
        }
    }

    public void action(Client c, byte mode, byte type, int selection) {
        NPCScript ns = scripts.get(c);
        if (ns != null) {
            npcLock.lock();
            try {
                if (selection < -1) {
                    CheatingOffense.PACKET_EDIT.cheatingSuspicious(c.getPlayer(), "The player is trying to send a negative selectiom hack to an npc. Please monitor this person, and let a developer know.");
                    return;
                }
                c.setClickedNPC();
                ns.action(mode, type, selection);
            } catch (Exception e) {
                FileLogger.printError(FileLogger.NPC + getCM(c).getNpc() + ".txt", e);
                notice(c, getCM(c).getNpc());
                dispose(c);
            } finally {
                npcLock.unlock();
            } 
        }
    }

    public void dispose(NPCConversationManager cm) {
        Client c = cm.getClient();
        cms.remove(c);
        scripts.remove(c);
        c.getPlayer().setNpcCooldown(System.currentTimeMillis());
        resetContext("npc/" + cm.getNpc() + ".js", c);
    }

    public void dispose(Client c) {
        if (cms.get(c) != null) {
            dispose(cms.get(c));
        }
    }
     
    private void notice(Client c, int id) {
        if (c != null) {
            c.getPlayer().dropMessage(1, "An error occurred while running this NPC, reporting to administrators. (ID: " + id + ")");
        }
    }

    public NPCConversationManager getCM(Client c) {
        return cms.get(c);
    }
}
