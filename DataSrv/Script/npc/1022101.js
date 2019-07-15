/*
        This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
/*
        Author: Biscuit
*/
var status = 0;

function start() {
        if(cm.getMapId() == 100000000) {
        cm.sendYesNo("Você está curioso(a) sobre as aventuras que Vila Feliz reservou para você? Vamos!");
       } else if (cm.getMapId() == 200000000) {
        cm.sendYesNo("Você está curioso(a) sobre as aventuras que Vila Feliz reservou para você? Vamos!");   
       } else {
           cm.sendYesNo("Aguarde!");
       }
}

function action(mode, type, selection) {
    if(mode != 1)
        cm.dispose();
    else {
        status++;
        if(status == 1) {
            cm.setPlayerVariable("HV_map", cm.getMapId()+"");
            cm.warp(209000000, 0);
            cm.dispose();
        }
    }
}