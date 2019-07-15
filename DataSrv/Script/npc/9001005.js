// KIN - v.62 Male Support
var status = 0;
var beauty = 0;
var haircolor = Array();
var skin = Array(0, 1, 2, 3, 4);
var hair = Array(30000, 30020, 30030, 30040, 30050, 30060, 30100, 30110, 30120, 30130, 30140, 30150, 30160, 30170, 30180, 30190, 30200, 30210, 30220, 30230, 30240, 30250, 30260, 30270, 30280, 30290, 30300, 30310, 30320, 30330, 30340, 30350, 30360, 30370, 30400, 30410, 30420, 30430, 30400, 30450, 30460, 30470, 30480, 30490, 30510, 30520, 30530, 30540, 30550, 30560, 30570, 30580, 30590, 30600, 30610, 30620, 30630, 30640, 30650, 30660, 30670, 30680, 30690, 30700, 30710, 30720, 30730, 30740, 30750, 30760, 30770, 30780, 30790, 30800, 30810, 30820, 30830, 30840, 30850, 30860, 30870, 30880, 30890, 30900, 30910, 30920, 30930, 30940, 30950, 30990, 33000, 33030, 33040, 33050, 33060, 33100, 33110, 33120, 33130, 33150, 33160, 33170, 33180, 33190, 33210, 33220, 33240, 33250, 33270, 33280, 33290, 33350, 33360, 33370, 33380, 33390, 33400, 33440, 33510, 33520, 33530, 33580, 33590, 33800);
var hairnew = Array();
var face = Array(20000, 20001, 20002, 20003, 20004, 20005, 20006, 20007, 20008, 20009, 20010, 20011, 20012, 20013, 20014, 20015, 20016, 20017, 20018, 20019, 20020, 20021, 20022, 20023, 20024, 20025, 20026, 20027, 20028, 20029, 20030, 20031, 20032, 20036, 20037, 20040, 20044, 20045, 20050, 20052, 20053, 20055, 20056);
//var face = Array(20000, 20001, 20002, 20003, 20004, 20005, 20006, 20007, 20008, 20009, 20010, 20011, 20012, 20013, 20014, 20015, 20016, 20017, 20018, 20019, 20020, 20021, 20022, 20023, 20024, 20025, 20026, 20027, 20028, 20029, 20030, 20031, 20032, 20036, 20037, 20040, 20044, 20045, 20050, 20052, 20053, 20055, 20056);
var facenew = Array();
var colors = Array();

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1)
        cm.dispose();
    else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1)
            status++;
        else
            status--;
        if (status == 0) {
             if (cm.getPlayer().getGender() == 1) {
		cm.sendOk("Você deve falar com o NPC do seu genero.");
		cm.dispose();
            } else {
                cm.sendSimple("Neste NPC e possivel trocar seu visual usando JS-Points, está interessado?\r\n\Lembrando que cada modficação e (#e10#n) JS-Points.\r\n#L0#Cor#l\r\n#L1#Cabelo#l\r\n#L2#Cor do Cabelo#l\r\n#L3#Olhos#l\r\n#L4#Cor dos Olhos#l");
            }
        }  else if (status == 1) {
            if (selection == 0) {
                beauty = 1;
                cm.sendStyle("Escolha alguma?", skin);
            } else if (selection == 1) {
                beauty = 2;
                hairnew = Array();
                for(var i = 0; i < hair.length; i++) {
                    if (hair[i] == 30100 || hair[i] == 30010)
                        hairnew.push(hair[i]);
                    else
                        hairnew.push(hair[i] + parseInt(cm.getPlayer().getHair() % 10));
                }
                cm.sendStyle("Escolha alguma?", hairnew);
            } else if (selection == 2) {
                beauty = 3;
                haircolor = Array();
                var current = parseInt(cm.getPlayer().getHair()/10)*10;
                if(current == 30100)
                    haircolor = Array(current , current + 1, current + 2, current + 3, current +4);
                else if (current == 30010)
                    haircolor = Array(current);
                else {
                    for(var i = 0; i < 8; i++)
                        haircolor.push(current + i);
                }
                cm.sendStyle("Escolha alguma?", haircolor);
            } else if (selection == 3) {
                beauty = 4;
                facenew = Array();
                for(var i = 0; i < face.length; i++) {
                    if (face[i] == 20022 || face[i] == 20021)
                        facenew.push(face[i]);
                    else
                        facenew.push(face[i] + cm.getPlayer().getFace() % 1000 - (cm.getPlayer().getFace() % 100));
                }
                cm.sendStyle("Escolha alguma?", facenew);
            } else if (selection == 4) {
                beauty = 5;
                var current = cm.getPlayer().getFace() % 100 + 20000;
                colors = Array();
                if(current == 20022 || current == 20021)
                    colors = Array(current , current + 100, current + 200, current + 300, current +400, current + 600, current + 700);
                else
                    colors = Array(current , current + 100, current + 200, current + 300, current +400, current + 500, current + 600, current + 700, current + 800);
                cm.sendStyle("Escolha alguma?", colors);
            }
        }
        else if (status == 2){
            if(cm.getPlayer().getCSPoints(2) >= 10) {
            if (beauty == 1) cm.setSkin(skin[selection]);
            if (beauty == 2) cm.setHair(hairnew[selection]);
            if (beauty == 3) cm.setHair(haircolor[selection]);
            if (beauty == 4) cm.setFace(facenew[selection]);
            if (beauty == 5) cm.setFace(colors[selection]);
            cm.getPlayer().modifyCSPoints(2, -10);
            cm.dispose();
        } else {
          cm.sendOk("Você não tem JS-Points, me desculpe.");
          cm.dispose();
        } 
      } 
    }
}