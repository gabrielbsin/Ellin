package client.player.violation;

import client.Client;
import constants.GameConstants;
import handling.world.service.BroadcastService;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import packet.creators.PacketCreator;

public class AutobanManager implements Runnable {

    private static class ExpirationEntry implements Comparable<ExpirationEntry> {

        public long time;
        public int acc;
        public int points;

        public ExpirationEntry(long time, int acc, int points) {
            this.time = time;
            this.acc = acc;
            this.points = points;
        }

        @Override
        public int compareTo(AutobanManager.ExpirationEntry o) {
            return (int) (time - o.time);
        }
    }

    private static final int autobanPoints = 1000;
    private static AutobanManager instance = null;
    private Map<Integer, Integer> points = new HashMap<>();
    private Map<Integer, List<String>> reasons = new HashMap<>();
    private Set<ExpirationEntry> expirations = new TreeSet<>();

    public static AutobanManager getInstance() {
        if (instance == null) {
            instance = new AutobanManager();
        }
        return instance;
    }

    public void autoban(Client c, String reason) {
        if (c.getPlayer().isGameMaster()) return;
        addPoints(c, autobanPoints, 0, reason);
    }

    public synchronized void addPoints(Client c, int points, long expiration, String reason) {
        if (c.getPlayer().isGameMaster()) return;
        
        int acc = c.getPlayer().getAccountID();
        List<String> reasonList;
        
        if (this.points.containsKey(acc)) {
            if (this.points.get(acc) >= autobanPoints) {
                return;
            }
            this.points.put(acc, this.points.get(acc) + points);
            reasonList = this.reasons.get(acc);
            reasonList.add(reason);
        } else {
            this.points.put(acc, points);
            reasonList = new LinkedList<>();
            reasonList.add(reason);
            this.reasons.put(acc, reasonList);
        }
        if (this.points.get(acc) >= autobanPoints) {
            String name = c.getPlayer().getName();
            StringBuilder banReason = new StringBuilder();
            for (String s : reasons.get(acc)) {
                banReason.append(s);
            }
            if (GameConstants.AUTO_BAN) {
                c.getPlayer().ban(banReason.toString(), true);
                BroadcastService.broadcastGMMessage(PacketCreator.ServerNotice(6, name + " has been banned by the system. (Reason: " + reason + ")"));
            }
            return;
        }
        if (expiration > 0) {
            expirations.add(new ExpirationEntry(System.currentTimeMillis() + expiration, acc, points));
        }
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (ExpirationEntry e : expirations) {
            if (e.time <= now) {
                this.points.put(e.acc, this.points.get(e.acc) - e.points);
            } else {
                return;
            }
        }
    }
}