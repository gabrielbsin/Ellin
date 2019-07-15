/**
 *
 * @author Sharky
 */
var status = -1;
var level35 = [[1452016], [1472030], [1462014], [1302020], [1382009], [1492020], [1482020]];
var level43from35 = [[1452022], [1472032], [1462019], [1302030], [1382012], [1492021], [1482021]];
var level43 = [[1452022], [1472032], [1462019], [1332025], [1412011], [1422014], [1302030], [1442024], [1432012], [1382012], [1492021], [1482021]];
var level64 = [[1452045], [1472055], [1462040], [1332055, 1332056], [1412027, 1312032], [1422029, 1322054], [1302064, 1402039], [1442051], [1432040], [1382039, 1372034], [1492022], [1482022]];
var sel;
var sel_2;

function start(){
    cm.sendSimple("Sawp, I can upgrade your Maple Weapons for ya, for a fee. It's 2000 Maple Leaves to upgrade from a lvl 35 weapon to a lvl 43, and 1500 to upgrade from a lvl 43 weapon to a lvl 64. \r\n\t#dWhich level Maple Weapon do you want to upgrade?#b \r\n\t#L0#Level 35 Weapons#l \r\n\t#L1#Level 43 Weapons#l");
}

function action(m,t,s){
    status++;
    if(m != 1){
        cm.dispose();
        return;
    }
    if(status == 0){
        sel = s;
        if(s == 0){
            cm.sendSimple("Which type of level 35 Maple Weapon do you want to upgrade?#r \r\n\t#L0#Bow#l \r\n\t#L1#Claw#l \r\n\t#L2#Crossbow#l \r\n\t#L3#Sword#l \r\n\t#L4#Staff#l \r\n\t#L5#Gun#l \r\n\t#L6#Knuckle#l");
        } else {
            cm.sendSimple("Which type of level 43 Maple Weapon do you want to upgrade?#r \r\n\t#L0#Bow#l \r\n\t#L1#Claw#l \r\n\t#L2#Crossbow#l \r\n\t#L3#Dagger#l \r\n\t#L4#Axe#l \r\n\t#L5#Mace#l \r\n\t#L6#Sword#l \r\n\t#L7#Pole Arm#l \r\n\t#L8#Spear#l \r\n\t#L9#Staff#l \r\n\t#L10#Gun#l \r\n\t#L11#Knuckle#l");
        }
    } else if (status == 1){
        if(cm.haveItem((sel == 0 ? level35[s][0] : level43[s][0]), 1)) {
            if(sel == 0){
                cm.sendYesNo("I see you have a #d#t"+ level35[s][0] +"##k. Would you like to upgrade it to a #d#t"+ level43from35[s][0] +"##k for 2000 Maple Leaves?");
            } else {
                cm.sendSimple("I see you have a #d#t"+ level43[s][0] +"##k. Would you like to upgrade it to one of the following options for 1500 Maple Leaves? \r\n\t"+ (level64[s].length < 2 ? "#L0##b#t" + level64[s][0] +"##k#l" : "#L0##b#t" + level64[s][0] +"##k#l \r\n\t#L1##b#t"+ level64[s][1] +"##k#l"));
            }
            sel_2 = sel;
            sel = s;
        } else {
            cm.sendOk("You don't seem to have a Maple Weapon in that category.");
            cm.dispose();
        }
    } else if (status == 2){
        if(cm.haveItem(4001126, (sel_2 == 0 ? 2000 : 1500))){
            cm.gainItem((sel_2 == 0 ? level35[sel][0] : level43[sel][0]), -1);
            cm.gainItem(4001126, -(sel_2 == 0 ? 2000 : 1500));
            cm.gainItem((sel_2 == 0 ? level43from35[sel][0] : level64[sel][s]));
            cm.sendOk("Thank you for upgrading your Maple Weapon from a #d#t"+ (sel_2 == 0 ? level35[sel][0] : level43[sel][0]) +"##k to a #d#t"+ (sel_2 == 0 ? level43from35[sel][0] : level64[sel][s]) +"##k. Come again!");
        } else {
            cm.sendOk("You only have #d#c4001126# #t4001126## Maple Leaves#k. Come back when you have #d"+ (sel_2 == 0 ? 2000 : 1500) +" Maple Leaves#k.");
        }
        cm.dispose();
    }
} 