/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.player.Player;
import client.Client;
import client.player.commands.object.CommandProcessor;
import client.player.violation.CheatingOffense;
import constants.CommandConstants.CommandType;
import handling.channel.ChannelServer;
import static handling.channel.handler.ChannelHeaders.ChatHeaders.*;
import handling.mina.PacketReader;
import handling.world.messenger.MapleMessenger;
import handling.world.messenger.MapleMessengerCharacter;
import handling.world.World;
import handling.world.service.AllianceService;
import handling.world.service.BuddyService;
import handling.world.service.FindService;
import handling.world.service.GuildService;
import handling.world.service.MessengerService;
import handling.world.service.PartyService;
import packet.creators.PacketCreator;

/**
 *
 * @author GabrielSin
 */
public class ChatHandler {

    public static void GeneralChat(PacketReader packet, Client c) {
        String s = packet.readMapleAsciiString();
        Player p = c.getPlayer();
        byte show = packet.readByte();
        if (p != null && !CommandProcessor.processCommand(c, s, CommandType.NORMAL)) {
            if (s.length() > Byte.MAX_VALUE && !p.isGameMaster()) {
                CheatingOffense.PACKET_EDIT.cheatingSuspicious(p, p.getName() + " text unlimited.");
                return;
            }
            if (p.getMap().getProperties().getProperty("mute").equals(Boolean.TRUE) && !p.isGameMaster()) {
                p.dropMessage("GM has now silenced this map. Wait until it is activated again.");
            } else { 
                if (!p.isHidden()) {
                    p.getMap().broadcastMessage(PacketCreator.GetChatText(p.getId(), s, p.isGameMaster(), show));
                } else {
                    p.getMap().broadcastGMMessage(PacketCreator.GetChatText(p.getId(), s, p.isGameMaster(), show));
                }
            }
        }
    } 

    public static void Whisper_Find(PacketReader packet, Client c) {
        switch (packet.readByte()) {
            case COMMAND_FIND: { 
                String toFind = packet.readMapleAsciiString();
                Player victim = c.getChannelServer().getPlayerStorage().getCharacterByName(toFind);
                if(victim != null) {
                    if (!victim.isGameMaster() || (c.getPlayer().isGameMaster() && victim.isGameMaster())) {
                        if (victim.getCashShop().isOpened()) {
                            c.getSession().write(PacketCreator.GetFindReplyWithCS(victim.getName()));
                        } else if (c.getChannel() == victim.getClient().getChannel()) {
                            c.getSession().write(PacketCreator.GetFindReplyWithMap(victim.getName(), victim.getMapId()));
                        } else {
                            c.getSession().write(PacketCreator.GetFindReply(victim.getName(), (byte) victim.getClient().getChannel()));
                        }
                    } else {
                        c.getSession().write(PacketCreator.GetWhisperReply(toFind, (byte) 0));
                    }
                } else {
                    c.getSession().write(PacketCreator.GetWhisperReply(toFind, (byte) 0)); 
                }    
              break;
            }
            case COMMAND_WHISPER: { 
                String recipient = packet.readMapleAsciiString();
		String message = packet.readMapleAsciiString();
                final int ch = FindService.findChannel(recipient);
                if (message.length() > 100) {
                    break;
                }
                 if (ch > 0) {
                    Player p = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(recipient);
		    if (p == null) {
			break;
		    }
                    p.getClient().getSession().write(PacketCreator.GetWhisper(c.getPlayer().getName(), c.getChannel(), message));
                    if (!c.getPlayer().isGameMaster() && p.isGameMaster()) {
                        c.getSession().write(PacketCreator.GetWhisperReply(recipient, (byte) 0));
                    } else {
                        c.getSession().write(PacketCreator.GetWhisperReply(recipient, (byte) 1));
                    }
                } else {
                    c.getSession().write(PacketCreator.GetWhisperReply(recipient, (byte) 0));
                }
                break;
            }
        }
    }

    public static void PrivateChat(PacketReader packet, Client c) { 
        int type = packet.readByte(); 
        int numRecipients = packet.readByte();
        int recipients[] = new int[numRecipients];
        
        for (int i = 0; i < numRecipients; i++) {
            recipients[i] = packet.readInt();
        }
        
        String chatText = packet.readMapleAsciiString();
        Player p = c.getPlayer();
        if (chatText.length() > 100 || p == null) {
            return;
        }
        switch (type) {
            case PRIVATE_CHAT_TYPE_BUDDY: 
                BuddyService.buddyChat(recipients, p.getId(), p.getName(), chatText);
                break;
            case PRIVATE_CHAT_TYPE_PARTY: 
                PartyService.partyChat(p.getParty().getId(), chatText, p.getName());
                break;
           case PRIVATE_CHAT_TYPE_GUILD: 
                GuildService.guildChat(p.getGuildId(), p.getName(), p.getId(), chatText);
                break;
           case PRIVATE_CHAT_TYPE_ALLIANCE: 
                AllianceService.allianceChat(p.getGuildId(), p.getName(), p.getId(), chatText);
                break;
        } 
    }

    public static void Messenger(PacketReader packet, Client c) {
        String input;
        MapleMessenger messenger = c.getPlayer().getMessenger();

        switch (packet.readByte()) {
            case MESSENGER_OPEN:
                if (messenger == null) {
                    int messengerid = packet.readInt();
                    if (messengerid == 0) { 
                        c.getPlayer().setMessenger(MessengerService.createMessenger(new MapleMessengerCharacter(c.getPlayer())));
                    } else {
                        messenger = MessengerService.getMessenger(messengerid);
                        if (messenger != null) {
                            final int position = messenger.getLowestPosition();
                            if (position > -1 && position < 4) {
                                c.getPlayer().setMessenger(messenger);
                                MessengerService.joinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()), c.getPlayer().getName(), c.getChannel());
                            }
                        }
                    }
                }
                break;
            case MESSENGER_EXIT: 
                if (messenger != null) {
                    final MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(c.getPlayer());
                    MessengerService.leaveMessenger(messenger.getId(), messengerplayer);
                    c.getPlayer().setMessenger(null);
                }
                break;
            case MESSENGER_INVITE: 
                 if (messenger != null) {
                    final int position = messenger.getLowestPosition();
                    if (position <= -1 || position >= 4) {
                        return;
                    }
                    input = packet.readMapleAsciiString();
                    final Player target = c.getChannelServer().getPlayerStorage().getCharacterByName(input);

                    if (target != null) {
                        if (target.getMessenger() == null) {
                            if (!target.isGameMaster() || c.getPlayer().isGameMaster()) {
                                c.getSession().write(PacketCreator.MessengerNote(input, 4, 1));
                                target.getClient().getSession().write(PacketCreator.MessengerInvite(c.getPlayer().getName(), messenger.getId()));
                            } else {
                                c.getSession().write(PacketCreator.MessengerNote(input, 4, 0));
                            }
                        } else {
                            c.getSession().write(PacketCreator.MessengerChat(c.getPlayer().getName() + " : " + target.getName() + " is already using Maple Messenger."));
                        }
                    } else {
                        if (World.isConnected(input)) {
                            MessengerService.messengerInvite(c.getPlayer().getName(), messenger.getId(), input, c.getChannel(), c.getPlayer().isGameMaster());
                        } else {
                            c.getSession().write(PacketCreator.MessengerNote(input, 4, 0));
                        }
                    }
                }
                break;	
            case MESSENGER_DECLINE: 
                final String targeted = packet.readMapleAsciiString();
                final Player target = c.getChannelServer().getPlayerStorage().getCharacterByName(targeted);
                if (target != null) { 
                    if (target.getMessenger() != null) {
                        target.getClient().getSession().write(PacketCreator.MessengerNote(c.getPlayer().getName(), 5, 0));
                    }
                } else { 
                    if (!c.getPlayer().isGameMaster()) {
                        MessengerService.declineChat(targeted, c.getPlayer().getName());
                    }
                }
                break;
            case MESSENGER_CHAT: 
                if (messenger != null) {
                    MessengerService.messengerChat(messenger.getId(), packet.readMapleAsciiString(), c.getPlayer().getName());
                }
            break;
        }
    }

    public static void Spouse_Chat(PacketReader packet, Client c) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        String recipient = packet.readMapleAsciiString(); 
        System.out.println("recipient: " + recipient);
        String message = packet.readMapleAsciiString();
        System.out.println("message: " + recipient);
        final int channel = FindService.findChannel(recipient);
        if (c.getPlayer().getPartnerId() == 0 || !c.getPlayer().getPartner().equalsIgnoreCase(recipient)) {
            c.getPlayer().dropMessage(5, "You are not married or your spouse is offline.");
            c.announce(PacketCreator.EnableActions());
            return;
        }
        if (channel > 0) {
            Player spouseChar = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(recipient);
            if (spouseChar == null) {
                c.getPlayer().dropMessage(5, "You are not married or your spouse is offline.");
                c.announce(PacketCreator.EnableActions());
                return;
            }
            // TODO: code spouse-chat watch system: if (c.getPlayer().getWatcher() != null) { return; }
            spouseChar.getClient().getSession().write(PacketCreator.OnCoupleMessage(c.getPlayer().getName(), message, true));
            c.getSession().write(PacketCreator.OnCoupleMessage(c.getPlayer().getName(), message, true));
        } else {
            c.getPlayer().dropMessage(5, "You are not married or your spouse is offline.");
        }
    }
}
