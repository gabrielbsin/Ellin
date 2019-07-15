function enter(pi) {
    if (pi.haveItem(4001094)) {
	pi.getMap().getReactorByName("dragonBaby").forceHitReactor(1);
	pi.getMap().getReactorByName("dragonBaby2").forceHitReactor(1);
	pi.playerMessage(5, "The Egg of Nine Spirit, which was comfortably nested, has emitted a mysterious light and has returned to its nest.");
	pi.gainItem(4001094, -1);
    }
}