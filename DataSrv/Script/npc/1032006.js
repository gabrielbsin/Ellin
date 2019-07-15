/*
 * LeaderMS Revision
 * @autor Gabriel A. <javascriptz@leaderms.com.br>
 * Deposito - 1052017
*/
var status;
 
function start() {
    status = -1;
    action(1, 0, 0);
}
 
function action(mode, type, selection) {
    if (mode == 1)
        status++;
    else {
        cm.dispose();
        return;
    }
    if (status == 0) {
            cm.getPlayer().getStorage().sendStorage(cm.getClient(), 1032006);
            cm.dispose();
    }
}