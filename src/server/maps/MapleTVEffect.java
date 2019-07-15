/**
 * Ellin é um servidor privado de MapleStory
 * Baseado em um servidor GMS-Like na v.62
 */

package server.maps;

import client.player.Player;
import handling.world.service.BroadcastService;
import java.util.ArrayList;
import java.util.List;
import packet.creators.PacketCreator;
import tools.TimerTools.MiscTimer;

/**
 * @brief MapleTVEffect
 * @author GabrielSin <gabrielsin@playellin.net>
 * @date   04/06/2018
 */
public class MapleTVEffect {
    
    private static boolean active;
	
    private List<String> message = new ArrayList<>(5);
    private Player user;
    private int type;
    private Player partner;

    public MapleTVEffect(Player u, Player p, List<String> msg, int t) {
        this.message = msg;
        this.user = u;
        this.type = t;
        this.partner = p;
        broadcastTV(true);
    }

    public static boolean isActive(){
        return active;
    }

    private void broadcastTV(boolean activity) {
        active = activity;
        if (active) {
            BroadcastService.broadcastMessage(PacketCreator.EnableTV());
            BroadcastService.broadcastMessage(PacketCreator.SendTV(user, message, type <= 2 ? type : type - 3, partner));
            int delay = 15000;
            if (type == 4) {
                delay = 30000;
            } else if (type == 5) {
                delay = 60000;
            }
            MiscTimer.getInstance().schedule(() -> {
                broadcastTV(false);
            }, delay);
        } else {
            BroadcastService.broadcastMessage(PacketCreator.RemoveTV());
        }
    }
}
