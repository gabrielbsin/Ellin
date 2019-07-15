/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package handling.world.service;

import handling.channel.ChannelServer;
import static handling.world.World.isConnected;
import handling.world.messenger.MapleMessenger;
import handling.world.messenger.MapleMessengerCharacter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import packet.creators.PacketCreator;
import client.player.Player;

public class MessengerService {

    private static final Map<Integer, MapleMessenger> messengers = new HashMap<>();
    private static final AtomicInteger runMessengerId = new AtomicInteger();

    static {
        runMessengerId.set(1);
    }

    public static MapleMessenger createMessenger(MapleMessengerCharacter chrfor) {
        int messengerid = runMessengerId.getAndIncrement();
        MapleMessenger messenger = new MapleMessenger(messengerid, chrfor);
        messengers.put(messenger.getId(), messenger);
        return messenger;
    }

    public static void declineChat(String target, String namefrom) {
        int ch = FindService.findChannel(target);
        if (ch > 0) {
            ChannelServer cs = ChannelServer.getInstance(ch);
            Player p = cs.getPlayerStorage().getCharacterByName(target);
            if (p != null) {
                MapleMessenger messenger = p.getMessenger();
                if (messenger != null) {
                    p.getClient().getSession().write(PacketCreator.MessengerNote(namefrom, 5, 0));
                }
            }
        }
    }

    public static MapleMessenger getMessenger(int messengerid) {
        return messengers.get(messengerid);
    }

    public static void leaveMessenger(int messengerid, MapleMessengerCharacter target) {
        MapleMessenger messenger = getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        int position = messenger.getPositionByName(target.getName());
        messenger.removeMember(target);

        messenger.getMembers().stream().filter((mmc) -> (mmc != null)).forEachOrdered((mmc) -> {
            int ch = FindService.findChannel(mmc.getId());
            System.out.println("leaveMessenger + " + ch);
            if (ch > 0) {
                Player chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(mmc.getName());
                if (chr != null) {
                    chr.getClient().getSession().write(PacketCreator.RemoveMessengerPlayer(position));
                }
            }
        });
    }

    public static void silentLeaveMessenger(int messengerid, MapleMessengerCharacter target) {
        MapleMessenger messenger = getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.silentRemoveMember(target);
    }

    public static void silentJoinMessenger(int messengerid, MapleMessengerCharacter target) {
        MapleMessenger messenger = getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.silentAddMember(target);
    }

    public static void updateMessenger(int messengerid, String namefrom, int fromchannel) {
        MapleMessenger messenger = getMessenger(messengerid);
        int position = messenger.getPositionByName(namefrom);

        messenger.getMembers().stream().filter((messengerchar) -> (messengerchar != null && !messengerchar.getName().equals(namefrom))).forEachOrdered((messengerchar) -> {
            int ch = FindService.findChannel(messengerchar.getName());
            if (ch > 0) {
                Player chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    Player from = ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(namefrom);
                    chr.getClient().getSession().write(PacketCreator.UpdateMessengerPlayer(namefrom, from, position, fromchannel - 1));
                }
            }
        });
    }

    public static void joinMessenger(int messengerid, MapleMessengerCharacter target, String from, int fromchannel) {
        MapleMessenger messenger = getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.addMember(target);
        int position = messenger.getPositionByName(target.getName());
        messenger.getMembers().stream().filter((messengerchar) -> (messengerchar != null)).forEachOrdered((messengerchar) -> {
            int mposition = messenger.getPositionByName(messengerchar.getName());
            int ch = FindService.findChannel(messengerchar.getName());
            if (ch > 0) {
                Player p = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (p != null) {
                    if (!messengerchar.getName().equals(from)) {
                        Player fromCh = ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(from);
                        p.getClient().getSession().write(PacketCreator.AddMessengerPlayer(from, fromCh, position, fromchannel - 1));
                        fromCh.getClient().getSession().write(PacketCreator.AddMessengerPlayer(p.getName(), p, mposition, messengerchar.getChannel() - 1));
                    } else {
                        p.getClient().getSession().write(PacketCreator.JoinMessenger(mposition));
                    }
                }
            }
        });
    }

    public static void messengerChat(int messengerId, String chatText, String nameFrom) {
        MapleMessenger messenger = getMessenger(messengerId);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }

        messenger.getMembers().forEach((messengerchar) -> {
            if (messengerchar != null && !messengerchar.getName().equals(nameFrom)) {
                int ch = FindService.findChannel(messengerchar.getName());
                if (ch > 0) {
                    Player p = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                    if (p != null) {
                        p.getClient().getSession().write(PacketCreator.MessengerChat(chatText));
                    }
                }
            } else if (messengerchar != null) {
                int ch = FindService.findChannel(messengerchar.getName());
                if (ch > 0) {
                    Player chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                }
            }
        });
    }

    public static void messengerInvite(String sender, int messengerId, String target, int fromChannel, boolean gm) {
        if (isConnected(target)) {
            int ch = FindService.findChannel(target);
            if (ch > 0) {
                Player from = ChannelServer.getInstance(fromChannel).getPlayerStorage().getCharacterByName(sender);
                Player targeter = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(target);
                if (targeter != null && targeter.getMessenger() == null) {
                    if (!targeter.isGameMaster() || gm) {
                        targeter.getClient().getSession().write(PacketCreator.MessengerInvite(sender, messengerId));
                        from.getClient().getSession().write(PacketCreator.MessengerNote(target, 4, 1));
                    } else {
                        from.getClient().getSession().write(PacketCreator.MessengerNote(target, 4, 0));
                    }
                } else {
                    from.getClient().getSession().write(PacketCreator.MessengerChat(sender + " : " + target + " is already using Maple Messenger"));
                }
            }
        }
    }        
}
