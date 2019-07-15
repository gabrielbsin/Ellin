
/**
  Questions.
-- Original Author --------------------------------------------------------------------------------
	Jvlaple
-- Modified by -----------------------------------------------------------------------------------
	XoticMS.
---------------------------------------------------------------------------------------------------
**/
importPackage(Packages.client);
importPackage(Packages.server);

var status;
var otherChar;
var Char;
 
function start(character3) {
    status = -1;
    action(1, 0, 0);
    otherChar = character3;
}

function action(mode, type, selection) {
    if (mode == 1)
        status++;
    else {
        if (type == 1 && mode == 0)
            otherChar.dropMessage(1, "Your partner has declined your request.");
        else
            otherChar.dropMessage(1, "Your partner closed the npc chat.");
        cm.dispose();
        return;
    }
	
    if (status == 0) {
        cm.sendNext("Someone in JS-Maple wants to send you a message.");
    } else if (status == 1) {
        var t = otherChar.getName();
        cm.sendYesNo("Do you wish to be engaged to " + t + "?") ;
    } else if (status == 2) {
        var t = otherChar.getName();
        if (cm.createEngagement(t)) {
            otherChar.dropMessage(1, "Your partner has accepted your request.");
            otherChar.setMarriageQuestLevel(50);
            cm.getPlayer().setMarriageQuestLevel(50);
            if (otherChar.getItemQuantity(2240000, false) > 0) {
                MapleInventoryManipulator.removeById(otherChar.getClient(), MapleInventoryType.USE, 2240000, 1, false, false);
                MapleInventoryManipulator.addById(otherChar.getClient(), 4031358, 1, "slut!");
                MapleInventoryManipulator.addById(otherChar.getClient(), 4031357, 1, "cunt!");
                cm.gainItem(4031358, 1);
            } else if (otherChar.getItemQuantity(2240001, false) > 0) {
                MapleInventoryManipulator.removeById(otherChar.getClient(), MapleInventoryType.USE, 2240001, 1, false, false);
                MapleInventoryManipulator.addById(otherChar.getClient(), 4031360, 1, "shit!");
                MapleInventoryManipulator.addById(otherChar.getClient(), 4031359, 1, "shit!");
                cm.gainItem(4031360, 1);
            } else if (otherChar.getItemQuantity(2240002, false) > 0) {
                MapleInventoryManipulator.removeById(otherChar.getClient(), MapleInventoryType.USE, 2240002, 1, false, false);
                MapleInventoryManipulator.addById(otherChar.getClient(), 4031362, 1, "shit!");
                MapleInventoryManipulator.addById(otherChar.getClient(), 4031361, 1, "shit!");
                cm.gainItem(4031362, 1);
            } else if (otherChar.getItemQuantity(2240003, false) > 0) {
                MapleInventoryManipulator.removeById(otherChar.getClient(), MapleInventoryType.USE, 2240003, 1, false, false);
                MapleInventoryManipulator.addById(otherChar.getClient(), 4031364, 1, "shit!");
                MapleInventoryManipulator.addById(otherChar.getClient(), 4031363, 1, "shit!");
                cm.gainItem(4031364, 1);
            }
        } else {
            cm.sendOk("There seems to be an error with the system. Try again ?");
            otherChar.dropMessage(1, "There seems to be an error with the system. Try again ?.");
        }
        cm.dispose();
    }
}