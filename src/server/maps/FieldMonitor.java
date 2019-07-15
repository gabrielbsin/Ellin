package server.maps;

import java.util.concurrent.ScheduledFuture;
import tools.TimerTools.MapTimer;
import server.maps.portal.Portal;

public class FieldMonitor {

    private final Field map;
    private final Portal portal;
    private final ScheduledFuture<?> monitorSchedule;

    public FieldMonitor(final Field map, String portal) {
        this.map = map;
        this.portal = map.getPortal(portal);
        monitorSchedule = MapTimer.getInstance().register(() -> {
            if (map.getCharacters().size() < 1) {
                FieldMonitor.this.run();
            }
        }, 10000);
    }

    private void run() {
        monitorSchedule.cancel(false);
        map.killAllMonsters();
        map.resetReactors();
        map.clearDrops();
        if (portal != null) {
            portal.setPortalState(Portal.OPEN);
        }
        map.resetReactors();
    }
}
