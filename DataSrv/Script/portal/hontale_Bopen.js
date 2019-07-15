/* The five caves
 * @author Jvlaple
 */
importPackage(Packages.server.maps);
importPackage(Packages.handling.channel);
importPackage(Packages.packet.creators);

function enter(pi) {
    if (pi.getPlayer().getMapId() == 240050101) {
        var nextMap = 240050102;
        var eim = pi.getPlayer().getEventInstance()
        var target = eim.getMapInstance(nextMap);
        var targetPortal = target.getPortal("sp");
        var avail = eim.getProperty("2stageclear");
        if (avail == null) {
            pi.getPlayer().getClient().getSession().write(PacketCreator.ServerNotice(6, "The portal is blocked."));
            return false;
        } else {
            pi.getPlayer().changeMap(target, targetPortal);
            return true;
        }
    } else if (pi.getPlayer().getMapId() == 240050102) {
        var nextMap = 240050103;
        var eim = pi.getPlayer().getEventInstance()
        var target = eim.getMapInstance(nextMap);
        var targetPortal = target.getPortal("sp");
        var avail = eim.getProperty("3stageclear");
        if (avail == null) {
            pi.getPlayer().getClient().getSession().write(PacketCreator.ServerNotice(6, "The portal is blocked."));
            return false;
        } else {
            pi.getPlayer().changeMap(target, targetPortal);
            return true;
        }
    } else if (pi.getPlayer().getMapId() == 240050103) {
        var nextMap = 240050104;
        var eim = pi.getPlayer().getEventInstance()
        var target = eim.getMapInstance(nextMap);
        var targetPortal = target.getPortal("sp");
        var avail = eim.getProperty("4stageclear");
        if (avail == null) {
            pi.getPlayer().getClient().getSession().write(PacketCreator.ServerNotice(6, "The portal is blocked."));
            return false;
        } else {
            pi.getPlayer().changeMap(target, targetPortal);
            return true;
        }
    } else if (pi.getPlayer().getMapId() == 240050104) {
        var nextMap = 240050105;
        var eim = pi.getPlayer().getEventInstance()
        var target = eim.getMapInstance(nextMap);
        var targetPortal = target.getPortal("sp");
        var avail = eim.getProperty("5stageclear");
        if (avail == null) {
            pi.getPlayer().getClient().getSession().write(PacketCreator.ServerNotice(6, "The portal is blocked."));
            return false;
        } else {
            pi.getPlayer().changeMap(target, targetPortal);
            return true;
        }
    } else if (pi.getPlayer().getMapId() == 240050105) {
        if (pi.haveItem(4001091, 1) && pi.isLeader()) {
            pi.gainItem(4001091, -1);
            pi.getPlayer().getClient().getSession().write(PacketCreator.ServerNotice(6, "The six keys break the seal for a flash..."));
            pi.warp(240050100, "st00");
            return true;
        } else {
            pi.getPlayer().getClient().getSession().write(PacketCreator.ServerNotice(6, "The portal is blocked."));
            return false;
        }
    }
    return true;
}
