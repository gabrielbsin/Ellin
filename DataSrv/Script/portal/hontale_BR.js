importPackage(Packages.server.maps);
importPackage(Packages.handling.channel);
importPackage(Packages.packet.creators);

function enter(pi) {
    if (pi.getPlayer().getMapId() == 240060000) {
        var nextMap = 240060100;
        var eim = pi.getPlayer().getEventInstance()
        var target = eim.getMapInstance(nextMap);
        var targetPortal = target.getPortal("sp");
        var avail = eim.getProperty("head1");
        if (avail != "yes") {
            pi.getPlayer().getClient().getSession().write(PacketCreator.ServerNotice(6, "The portal is blocked."));
            return false;
        } else {
            pi.getPlayer().changeMap(target, targetPortal);
            if (eim.getProperty("head2spawned") != "yes") {
                eim.setProperty("head2spawned", "yes");
                eim.schedule("headTwo", 5000);
            }
            return true;
        }
    } else if (pi.getPlayer().getMapId() == 240060100) {
        var nextMap = 240060200;
        var eim = pi.getPlayer().getEventInstance()
        var target = eim.getMapInstance(nextMap);
        var targetPortal = target.getPortal("sp");
        var avail = eim.getProperty("head2");
        if (avail != "yes") {
            pi.getPlayer().getClient().getSession().write(PacketCreator.ServerNotice(6, "The portal is blocked."));
            return false;
        } else {
            pi.getPlayer().changeMap(target, targetPortal);
            return true;
        }
    }
    return true;	
}