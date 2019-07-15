function enter(pi) {
    if (pi.getPlayer().getMap().countMonsters() == 0) {
        pi.playPortalSound(); pi.warp(910500200, "out00");
        return true;
    }
    pi.getPlayer().dropMessage(5, "You must defeat all the monsters first.");
    return true;
}