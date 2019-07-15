function enter(pi) {
    if (!pi.haveItem(4031890)) {
	pi.playerMessage(5, "You do not have a warp card");
    } else {
	if (pi.haveItem(4031890)) { 
	    pi.warp(221000300,"earth00");
		pi.gainItem(4031890, -1)
	}
    }
}