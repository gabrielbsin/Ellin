function enter(pi) {
    if (!pi.haveItem(4031890)) {
	pi.playerMessage(5, "You do not have a warp card");
	return false;
    } else {
	if (pi.haveItem(4031890)) {
	    pi.warp(120000101,"earth01");
		return true;
	}
    }
}