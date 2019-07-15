importPackage(Packages.server.maps); 
importPackage(Packages.net.channel); 

function enter(pi) {
    var returnMap = pi.getPlayer().getSavedLocation(SavedLocationType.RICHIE); 
    if (returnMap < 0) { 
        returnMap = 100000000;
        return true;
    } 
    pi.getPlayer().changeMap(returnMap, 0); 
    pi.getPlayer().clearSavedLocation(SavedLocationType.RICHIE); 
    return true;
}