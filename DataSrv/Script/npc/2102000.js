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

function start() {
    if(cm.haveItem(4031045)){
         if (cm.getEventNotScriptOpen("Genio")) {   
            cm.sendYesNo("Parece que está cheio de lugares vazios para esta viagem. Por favor, tenha seu bilhete em mãos para embarcar. O trajeto será longo, mas chegará bem a seu destino. O que você acha? Quer embarcar neste?");
        } else {
            cm.sendOk("Sinto muito, mas o Gênio já está CHEIO. Não podemos aceitar mais nenhum passageiro. Por favor, embarque no próximo.");
            cm.dispose();
        }
    } else {
        cm.sendOk("Ah, não... Não creio que tenha seu bilhete consigo. Não pode embarcar sem ele. Por favor, compre o bilhete no balcão de bilhetes.");
        cm.dispose();
    }
}
function action(mode, type, selection) {
    if (mode <= 0) {
	cm.sendOk("Tudo bem, fale comigo quando quiser novamente!");
        cm.dispose();
	return;
    } 
    cm.gainItem(4031045, -1);
    cm.warp(260000110);
    cm.dispose();
}