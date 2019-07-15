/* 
*
*	NimaKIN - v.61 Female Support
*
*/
var status = 0;
var beauty = 0;
var haircolor = Array();
var skin = Array(0, 1, 2, 3, 4, 9);
var hair = Array(31000, 31010, 31020, 31030, 31040, 31050, 31060, 31070, 31080, 31090, 31100, 31110, 31120, 31130, 31140, 31150, 31160, 31170, 31180, 31190, 31200, 31210, 31220, 31230, 31240, 31250, 31260, 31270, 31280, 31290, 31300, 31310, 31320, 31330, 31340, 31350, 31360, 31400, 31410, 31420, 31430, 31440, 31450, 31460, 31470, 31480, 31490, 31510, 31520, 31530, 31540, 31550, 31560, 31570, 31580, 31590, 31600, 31610, 31620, 31630, 31640, 31650, 31660, 31670, 31680, 31690, 31700, 31710, 31720, 31730, 31740, 31750, 31760, 31770, 31780, 31790, 31800, 31810, 31820, 31830 ,31840, 31850, 31860, 31870, 31880, 31890, 31910, 31920, 31930, 31940, 31950, 31990, 32160, 34000, 34010, 34020, 34030, 34040, 34050, 34060, 34070, 34080, 34090, 34100, 34110, 34120, 34130, 34140, 34150, 34160, 34180, 34190, 34210, 34220, 34250, 34260, 34270, 34310, 34320, 34330, 34340, 34360, 34400, 34450, 34470, 34480, 34490, 34540);
var hairnew = Array();
var face = Array(21000, 21001, 21002, 21003, 21004, 21005, 21006, 21007, 21008, 21009, 21010, 21011, 21012, 21013, 21014, 21016, 21017, 21018, 21019, 21020, 21021, 21022, 21023, 21024, 21025, 21026, 21027, 21028, 21029, 21030, 21031, 21034, 21035, 21038, 21041, 21042, 21044, 21049, 21052, 21053, 21054);
var facenew = Array();
var colors = Array();

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1)
            status++;
        else
            status--;
        if (status == 0) {
            if (cm.getPlayer().getGender() == 0) {
		cm.sendOk("Você deve falar com o NPC do seu genero.");
		cm.dispose();
            } else {
                cm.sendSimple("Neste NPC e possivel trocar seu visual usando JS-Points, está interessado?\r\n\Lembrando que cada modficação e (#e10#n) JS-Points.\r\n#L0#Cor#l\r\n#L1#Cabelo#l\r\n#L2#Cor do Cabelo#l\r\n#L3#Olhos#l\r\n#L4#Cor dos Olhos#l");
            }
        } else if (status == 1) {
            if (selection == 0) {
                beauty = 1;
                cm.sendStyle("Escolha alguma?", skin);
            } else if (selection == 1) {
                beauty = 2;
                hairnew = Array();
                for(var i = 0; i < hair.length; i++) {
                    hairnew.push(hair[i] + parseInt(cm.getPlayer().getHair() % 10));
                }
                cm.sendStyle("Escolha alguma?", hairnew);
            } else if (selection == 2) {
                beauty = 3;
                haircolor = Array();
                var current = parseInt(cm.getPlayer().getHair()/10)*10;
                for(var i = 0; i < 8; i++) {
                    haircolor.push(current + i);
                }
                cm.sendStyle("Escolha alguma?", haircolor);
            } else if (selection == 3) {
                beauty = 4;
                facenew = Array();
                for(var i = 0; i < face.length; i++) {
                    facenew.push(face[i] + cm.getPlayer().getFace() % 1000 - (cm.getPlayer().getFace() % 100));
                }
                cm.sendStyle("Escolha alguma?", facenew);
            } else if (selection == 4) {
                beauty = 5;
                var current = cm.getPlayer().getFace() % 100 + 21000;
                colors = Array();
                colors = Array(current , current + 100, current + 200, current + 300, current +400, current + 500, current + 600, current + 700);
                cm.sendStyle("Escolha alguma?", colors);
            }
        }
        else if (status == 2){
            if(cm.getPlayer().getCSPoints(2) >= 10) {
            if (beauty == 1){
                cm.setSkin(skin[selection]);
            }
            if (beauty == 2){
                cm.setHair(hairnew[selection]);
            }
            if (beauty == 3){
                cm.setHair(haircolor[selection]);
            }
            if (beauty == 4){
                cm.setFace(facenew[selection]);
            }
            if (beauty == 5){
                cm.setFace(colors[selection]);
            }
            cm.getPlayer().modifyCSPoints(2, -10);
            cm.dispose();
        } else {
          cm.sendOk("Você não tem JS-Points, me desculpe.");
          cm.dispose();
        }
      }
    }
}