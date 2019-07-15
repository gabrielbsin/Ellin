/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.player.Player;
import client.Client;
import community.MapleBuddyInvitedEntry;
import community.MapleBuddyList;
import community.MapleBuddyList.BuddyAddResult;
import community.MapleBuddyListEntry;
import handling.channel.ChannelServer;
import static handling.channel.handler.ChannelHeaders.BuddyListHeaders.*;
import handling.mina.PacketReader;
import handling.world.service.BuddyService;
import handling.world.service.FindService;
import packet.creators.PacketCreator;
import tools.Pair;

/**
 *
 * @author GabrielSin
 */
public class BuddyListHandler {

    public static void BuddyOperation(final PacketReader packet, final Client c) {
        final MapleBuddyList buddylist = c.getPlayer().getBuddylist();
        switch (packet.readByte()) {
             case BUDDY_INVITE_MODIFY: 
                final String addName = packet.readMapleAsciiString();
                if (addName.length() > 13) {
                    return;
                }
                boolean isOnPending = BuddyService.isBuddyPending(new MapleBuddyInvitedEntry(addName, c.getPlayer().getId()));
                if (isOnPending) {  
                    c.getSession().write(PacketCreator.BuddylistMessage(ALREADY_FRIEND_REQUEST));
                    return;
                }
                if (buddylist.isFull()) {
                    c.getSession().write(PacketCreator.BuddylistMessage(YOUR_LIST_FULL));
                    return;
                }
                final int channel = FindService.findChannel(addName);
                if (channel <= 0) {  
                    c.getPlayer().dropMessage(5, "The character is not registered or not in game.");
                    c.getSession().write(PacketCreator.EnableActions());
                    return;
                }
                final Player otherChar = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(addName);
                if (!otherChar.isGameMaster() || c.getPlayer().isGameMaster()) {
                    if (otherChar.getBuddylist().isFull()) {
                        c.getSession().write(PacketCreator.BuddylistMessage(THEIR_LIST_FULL));
                        return;
                    }
                    final BuddyAddResult buddyAddResult = BuddyService.requestBuddyAdd(addName, c.getPlayer());
                    if (buddyAddResult == BuddyAddResult.BUDDYLIST_FULL) {
                        c.getSession().write(PacketCreator.BuddylistMessage(THEIR_LIST_FULL));
                        return;
                    }
                    if (buddyAddResult == BuddyAddResult.ALREADY_ON_LIST) {
                        c.getPlayer().dropMessage(5, "An error has occured. Please ask the player to re-add you as a buddy again.");
                        c.getSession().write(PacketCreator.EnableActions());
                        return;
                    }
                    if (buddyAddResult == BuddyAddResult.NOT_FOUND) {
                        c.getPlayer().dropMessage(5, "The character is not registered or not in game.");
                        c.getSession().write(PacketCreator.EnableActions());
                        return;
                    }
                    c.getPlayer().dropMessage(1, "You have invited '" + addName + "' into your Buddy List.");
                    c.getSession().write(PacketCreator.EnableActions());
                } else {
                    c.getSession().write(PacketCreator.BuddylistMessage(NO_GM_INVITES));
                }
                break;
            case BUDDY_ACCEPT: 
                final int otherCid = packet.readInt();
                boolean isOnPending_ = BuddyService.isBuddyPending(new MapleBuddyInvitedEntry(c.getPlayer().getName(), otherCid));
                if (!isOnPending_) {
                    c.getSession().write(PacketCreator.BuddylistMessage(DENY_ERROR));
                    return;
                }
                final Pair<BuddyAddResult, String> bal = BuddyService.acceptToInvite(c.getPlayer(), otherCid);
                if (bal.getLeft() == BuddyAddResult.NOT_FOUND) {
                    c.getSession().write(PacketCreator.BuddylistMessage(DENY_ERROR));
                    return;
                }
                if (bal.getLeft() == BuddyAddResult.BUDDYLIST_FULL) {
                    c.getSession().write(PacketCreator.BuddylistMessage(YOUR_LIST_FULL));
                    return;
                }
                c.getPlayer().dropMessage(5, "Congratulations, you are now friends with '" + bal.getRight() + "'.");
                c.getSession().write(PacketCreator.EnableActions());
                break;
            case BUDDY_DELETE_DENY: 
                final int otherCID = packet.readInt();
                boolean isInvited = BuddyService.isBuddyPending(new MapleBuddyInvitedEntry(c.getPlayer().getName(), otherCID));
                if (isInvited) {
                    c.getPlayer().dropMessage(5, BuddyService.denyToInvite(c.getPlayer(), otherCID));
                    c.getSession().write(PacketCreator.UpdateBuddylist(REMOVE, buddylist.getBuddies()));
                    c.getSession().write(PacketCreator.EnableActions());
                    return;
                }
                final MapleBuddyListEntry blz = buddylist.get(otherCID);
                if (blz == null) { 
                    c.getPlayer().dropMessage(5, "An error has occured. The character is not on your buddy list.");
                    c.getSession().write(PacketCreator.EnableActions());
                    return;
                }
                final MapleBuddyList.BuddyDelResult bdr = BuddyService.DeleteBuddy(c.getPlayer(), otherCID);
                if (bdr == MapleBuddyList.BuddyDelResult.NOT_ON_LIST) {
                    c.getPlayer().dropMessage(5, "An error has occured. The character is not on your buddy list.");
                    c.getSession().write(PacketCreator.EnableActions());
                    return;
                }
                if (bdr == MapleBuddyList.BuddyDelResult.IN_CASH_SHOP) {
                    c.getPlayer().dropMessage(5, "The character is currently in the Cash Shop.");
                    c.getSession().write(PacketCreator.EnableActions());
                    return;
                }
                if (bdr == MapleBuddyList.BuddyDelResult.ERROR) { 
                    c.getPlayer().dropMessage(5, "An error has occured. Please contact one of the GameMasters.");
                    c.getSession().write(PacketCreator.EnableActions());
                    return;
                }
                c.getPlayer().dropMessage(5, "Your buddy relationship with the deleted person has ended.");
                c.getSession().write(PacketCreator.EnableActions());
                break;
            default:
                System.out.println("Unknown buddylist action: " + packet.toString());
                break;
        }
    }
}
