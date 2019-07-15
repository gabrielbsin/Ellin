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
status = -1;
close = false;
oldSelection = -1;

function start() {
    var text = "Ola, eu sou o leitor de ticket's.";
    if(cm.haveItem(4031713))
        text += " Voce sera levado imediatamente, que ticket quer usar?#b";
    else
        close = true;
    if(cm.haveItem(4031713))
        text += "\r\n#L0##t4031713#";
    if(close){
        cm.sendOk(text);
        cm.dispose();
    }else
        cm.sendSimple(text);
}

function action(mode, type, selection) {
    status++;
    if (mode != 1) {
        if(mode == 0)
            cm.sendNext("Voce deve ter algum negocio para cuidar aqui, certo?");
        cm.dispose();
        return;
    }
    if (status == 0) {
        if(selection == 0){
            cm.sendYesNo("Me parece que ha muito espaco para esse passeio. Por favor, tenha o seu bilhete pronto para que eu possa deixa-lo entrar. A viagem pode ser longa, mas voce vai chegar ao seu destino muito bem. O que voce acha? Voce quer entrar nesta viagem?");
        }
        oldSelection = selection;
    } else if(status == 1){
        if(oldSelection == 0){
            cm.gainItem(4031713, -1);
            cm.warp(103000100, 0);
        }
        cm.dispose();
    }
}