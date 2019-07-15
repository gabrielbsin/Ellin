function enter(pi) {
    if (pi.getPlayer().getMapId() == 222020100) {
        if (pi.getEventNotScriptOpen("ElevadorDescendo")) {
            pi.warp(222020110, "sp");
            return true;
        } else {
            pi.playerMessage("The elevator is not available at the moment. Please try again later.");
            return false;
        }
    } else if (pi.getPlayer().getMapId() == 222020200) {
        if (pi.getEventNotScriptOpen("ElevadorSubindo")) {
            pi.warp(222020210, "sp");
            return true;
        } else {
            pi.playerMessage("The elevator is not available at the moment. Please try again later.");
            return false;
        }
    } else {
        return false;
    }
}