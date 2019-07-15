package handling.login;

import java.util.Map;
import java.util.Map.Entry;
import client.Client;
import handling.channel.ChannelServer;
import handling.login.handler.CharLoginHeaders;
import packet.creators.LoginPackets;
import tools.TimerTools.PingTimer;


public class LoginWorker {

    private static long lastUpdate = 0;

    public static void registerClient(final Client c) {
        if (System.currentTimeMillis() - lastUpdate > 600000) { 
            lastUpdate = System.currentTimeMillis();
            final Map<Integer, Integer> load = ChannelServer.getChannelLoad();
            int usersOn = 0;
            if (load == null || load.size() <= 0) { 
                lastUpdate = 0;
                c.getSession().write(LoginPackets.GetLoginStatus(CharLoginHeaders.LOGIN_ALREADY));
                return;
            }
            final double loadFactor = 1200 / ((double) LoginServer.getUserLimit() / load.size());
            for (Entry<Integer, Integer> entry : load.entrySet()) {
                usersOn += entry.getValue();
                load.put(entry.getKey(), Math.min(1200, (int) (entry.getValue() * loadFactor)));
            }
            LoginServer.setLoad(load, usersOn);
	    lastUpdate = System.currentTimeMillis();
        }

        if (c.finishLogin() == 0) {
            c.getSession().write(LoginPackets.GetAuthSuccess(c));
            c.setIdleTask(PingTimer.getInstance().schedule(c.getSession()::close, 10 * 60 * 10000));
        } else {
            c.getSession().write(LoginPackets.GetLoginStatus(CharLoginHeaders.LOGIN_ALREADY));
        }
    }
}
