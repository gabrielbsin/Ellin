/*
This file is part of the ZeroFusion MapleStory Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>
ZeroFusion organized by "RMZero213" <RMZero213@hotmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel.handler;


import client.Client;
import client.player.Player;
import handling.mina.PacketReader;
import community.MapleGuild;
import static handling.channel.handler.ChannelHeaders.AllianceHeaders.*;
import handling.world.service.AllianceService;
import handling.world.service.GuildService;
import packet.creators.GuildPackets;
import packet.creators.PacketCreator;
import packet.transfer.write.OutPacket;

public class AllianceHandler {

    public static final void HandleAlliance(final PacketReader packet, final Client c) {
        if (c.getPlayer().getGuildId() <= 0) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        final MapleGuild guild = GuildService.getGuild(c.getPlayer().getGuildId());
        if (guild == null) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }

        byte operation = packet.readByte();
        if (c.getPlayer().getGuildRank() != 1 && operation != 1) {  
            return;
        }
        int leaderid = 0;
        if (guild.getAllianceId() > 0) {
            leaderid = AllianceService.getAllianceLeader(guild.getAllianceId());
        }
        switch (operation) {
            case ALLIANCE_UNKNOWN: 
                AllianceUnknow(c, guild);
                break;
            case ALLIANCE_INVITE:
                AllianceInvite(c, guild, packet, leaderid);
                break;
            case ALLIANCE_ACCEPT_INVITE:
                AllianceAcceptInvite(c);
                break;
            case ALLIANCE_LEAVE:
            case ALLIANCE_EXPEL: 
                AllianceLeaveAndExpel(c, guild, packet, operation);
                break;
            case ALLIANCE_CHANGE_LEADER:
                AllianceChangeLeader(c, guild, packet, leaderid);
                break;
            case ALLIANCE_TITLE_UPDATE:
                AllianceTitleUpdate(c, guild, packet, leaderid);
                break;
            case ALLIANCE_CHANGE_RANK:
                AllianceChangeRank(c, guild, packet);
                break;
            case ALLIANCE_NOTICE_UPDATE: 
                AllianceNoticeUpdate(c, guild, packet);
                break;
            default:
                System.out.println("Unhandled GuildAlliance op: " + operation + ", \n" + packet.toString());
                break;
        }
    }
    
    private static void AllianceUnknow(Client c, MapleGuild gs) {
       for (OutPacket pack : AllianceService.getAllianceInfo(gs.getAllianceId(), false)) {
            if (pack != null) {
                c.getSession().write(pack);
            }
        }  
    }
    
    private static void AllianceInvite(Client c, MapleGuild gs, PacketReader packet, int leaderid) {
        final int newGuild = GuildService.getGuildLeader(packet.readMapleAsciiString());
        if (newGuild > 0 && c.getPlayer().getAllianceRank() == 1 && leaderid == c.getPlayer().getId()) {
            Player p = c.getChannelServer().getPlayerStorage().getCharacterById(newGuild);
            if (p != null && p.getGuildId() > 0 && AllianceService.canInvite(gs.getAllianceId())) {
                p.getClient().getSession().write(GuildPackets.SendAllianceInvite(AllianceService.getAlliance(gs.getAllianceId()).getName(), c.getPlayer()));
                GuildService.setInvitedId(p.getGuildId(), gs.getAllianceId());
            }
        }
    }
    
    private static void AllianceAcceptInvite(Client c) {
        int inviteid = GuildService.getInvitedId(c.getPlayer().getGuildId());
        if (inviteid > 0) {
            if (!AllianceService.addGuildToAlliance(inviteid, c.getPlayer().getGuildId())) {
                c.getPlayer().dropMessage(5, "An error occured when adding guild.");
            }
            GuildService.setInvitedId(c.getPlayer().getGuildId(), 0);
        }
    }
    
    private static void AllianceLeaveAndExpel(Client c, MapleGuild gs, PacketReader packet, byte operation) {
        final int gid;
        if (operation == 6 && packet.available() >= 4) {
            gid = packet.readInt();
            if (packet.available() >= 4 && gs.getAllianceId() != packet.readInt()) {
                return;
            }
        } else {
            gid = c.getPlayer().getGuildId();
        }
        if (c.getPlayer().getAllianceRank() <= 2 && (c.getPlayer().getAllianceRank() == 1 || c.getPlayer().getGuildId() == gid)) {
            if (!AllianceService.removeGuildFromAlliance(gs.getAllianceId(), gid, c.getPlayer().getGuildId() != gid)) {
                c.getPlayer().dropMessage(5, "An error occured when removing guild.");
            }
        }
    }
    
    private static void AllianceChangeLeader(Client c, MapleGuild gs, PacketReader packet, int leaderid) {
        if (c.getPlayer().getAllianceRank() == 1 && leaderid == c.getPlayer().getId()) {
            if (!AllianceService.changeAllianceLeader(gs.getAllianceId(), packet.readInt())) {
                c.getPlayer().dropMessage(5, "An error occured when changing leader.");
            }
        }
    }
    
    private static void AllianceTitleUpdate(Client c, MapleGuild gs, PacketReader packet, int leaderid) {
        if (c.getPlayer().getAllianceRank() == 1 && leaderid == c.getPlayer().getId()) {
            String[] ranks = new String[5];
            for (int i = 0; i < 5; i++) {
                ranks[i] = packet.readMapleAsciiString();
            }
            AllianceService.updateAllianceRanks(gs.getAllianceId(), ranks);
        }
    }
    
    private static void AllianceChangeRank(Client c, MapleGuild gs, PacketReader packet) {
        if (c.getPlayer().getAllianceRank() <= 2) {
            if (!AllianceService.changeAllianceRank(gs.getAllianceId(), packet.readInt(), packet.readByte())) {
                c.getPlayer().dropMessage(5, "An error occured when changing rank.");
            }
        }
    }
    
    private static void AllianceNoticeUpdate(Client c, MapleGuild gs, PacketReader packet) {
        if (c.getPlayer().getAllianceRank() <= 2) {
            final String notice = packet.readMapleAsciiString();
            if (notice.length() > 100) {
                return;
            }
            AllianceService.updateAllianceNotice(gs.getAllianceId(), notice);
        }
    }
}
