/**
 * Ellin é um servidor privado de MapleStory
 * Baseado em um servidor GMS-Like na v.62
 */

package packet.creators;

import client.Client;
import client.player.Player;
import handling.channel.handler.ChannelHeaders;
import packet.opcode.SendPacketOpcode;
import packet.transfer.write.OutPacket;
import packet.transfer.write.WritingPacket;
import server.minirooms.Minigame;

public class MinigamePackets {
    
   public static void AddAnnounceBox(WritingPacket wp, Minigame game, int gameType, int type, int ammount, int joinAble) {
        wp.write(gameType);
        wp.writeInt(game.getObjectId()); 
        wp.writeMapleAsciiString(game.getDescription()); 
        wp.writeBool(game.getPassword() != null);
        wp.write(type);
        wp.write(ammount);
        wp.write(2);
        wp.write(joinAble);
    }
    
    public static OutPacket GetMiniGame(Client c, Minigame miniGame, boolean owner, int piece) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_JOIN);
        wp.write(1);
        wp.write(0);
        wp.write(owner ? 0 : 1);
        wp.write(0);
        PacketCreator.AddCharLook(wp, miniGame.getOwner(), false);
        wp.writeMapleAsciiString(miniGame.getOwner().getName());
        if (miniGame.getVisitor() != null) {
            Player visitor = miniGame.getVisitor();
            wp.write(1); 
            PacketCreator.AddCharLook(wp, visitor, false);
            wp.writeMapleAsciiString(visitor.getName());
        }
        wp.write(0xFF);
        wp.write(0);
        wp.writeInt(1);
        wp.writeInt(miniGame.getOwner().getMiniGamePoints("wins", true));
        wp.writeInt(miniGame.getOwner().getMiniGamePoints("ties", true));
        wp.writeInt(miniGame.getOwner().getMiniGamePoints("losses", true));
        wp.writeInt(2000);
        if (miniGame.getVisitor() != null) {
            Player visitor = miniGame.getVisitor();
            wp.write(1);
            wp.writeInt(1);
            wp.writeInt(visitor.getMiniGamePoints("wins", true));
            wp.writeInt(visitor.getMiniGamePoints("ties", true));
            wp.writeInt(visitor.getMiniGamePoints("losses", true));
            wp.writeInt(2000);
        }
        wp.write(0xFF);
        wp.writeMapleAsciiString(miniGame.getDescription());
        wp.writeShort(piece);
        return wp.getPacket();
    }
    
    public static OutPacket GetMiniGameReady(Minigame game) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_READY);
        return wp.getPacket();
    }

    public static OutPacket GetMiniGameUnReady(Minigame game) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_UN_READY);
        return wp.getPacket();
    }

    public static OutPacket GetMiniGameStart(Minigame game, int loser) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_START);
        return wp.getPacket();
    }

    public static OutPacket GetMiniGameSkipOwner(Minigame game) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_SKIP);
        wp.write(0x01);
        return wp.getPacket();
    }

    public static OutPacket GetMiniGameRequestTie(Minigame game) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_REQUEST_TIE);
        return wp.getPacket();
    }

    public static OutPacket GetMiniGameDenyTie(Minigame game) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_ANSWER_TIE);
        return wp.getPacket();
    }

    public static OutPacket GetMiniGameFull() {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_JOIN);
        wp.write(0);
        wp.write(2);
        return wp.getPacket();
    }
    
    public static OutPacket GetMiniGamePassIncorrect(){
        final WritingPacket mplew = new WritingPacket(5);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(ChannelHeaders.PlayerInteractionHeaders.ACT_JOIN);
        mplew.write(0);
        mplew.write(28);
        return mplew.getPacket();
    }

    public static OutPacket GetMiniGameSkipVisitor(Minigame game) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_SKIP);
        wp.write(0x00);
        return wp.getPacket();
    }

    public static OutPacket GetMiniGameMoveOmok(Minigame game, int moveOne, int moveTwo, int moveThree) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_MOVE_OMOK);
        wp.writeInt(moveOne);
        wp.writeInt(moveTwo);
        wp.write(moveThree);
        return wp.getPacket();
    }

    public static OutPacket GetMiniGameNewVisitor(Player p, int slot) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_VISIT);
        wp.write(slot);
        PacketCreator.AddCharLook(wp, p, false);
        wp.writeMapleAsciiString(p.getName());
        wp.writeInt(1);
        wp.writeInt(p.getMiniGamePoints("wins", true));
        wp.writeInt(p.getMiniGamePoints("ties", true));
        wp.writeInt(p.getMiniGamePoints("losses", true));
        wp.writeInt(2000);
        return wp.getPacket();
    }

    public static OutPacket GetMiniGameRemoveVisitor() {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_EXIT);
        wp.writeBool(true);
        return wp.getPacket();
    }

    private static OutPacket GetMiniGameResult(Minigame game, int win, int lose, int tie, int result, int forfeit, boolean omok) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_FINISH_GAME);
        if (tie == 0 && forfeit != 1) {
            wp.write(0);
        } else if (tie == 1) {
            wp.write(1);
        } else if (forfeit == 1) {
            wp.write(2);
        }
        wp.write(result - 1);
        wp.writeInt(1);
        wp.writeInt(game.getOwner().getMiniGamePoints("wins", omok) + win); 
        wp.writeInt(game.getOwner().getMiniGamePoints("ties", omok) + tie); 
        wp.writeInt(game.getOwner().getMiniGamePoints("losses", omok) + lose); 
        wp.writeInt(2000);
        wp.writeInt(1); 
        wp.writeInt(game.getVisitor().getMiniGamePoints("wins", omok) + lose); 
        wp.writeInt(game.getVisitor().getMiniGamePoints("ties", omok) + tie); 
        wp.writeInt(game.getVisitor().getMiniGamePoints("losses", omok) + win); 
        wp.writeInt(2000);
        game.getOwner().setMiniGamePoints(game.getVisitor(), result, omok);
        return wp.getPacket();
    }

    public static OutPacket GetMiniGameOwnerWin(Minigame game) {
        return GetMiniGameResult(game, 1, 0, 0, 1, 0, true);
    }

    public static OutPacket GetMiniGameVisitorWin(Minigame game) {
        return GetMiniGameResult(game, 0, 1, 0, 2, 0, true);
    }

    public static OutPacket GetMiniGameTie(Minigame game) {
        return GetMiniGameResult(game, 0, 0, 1, 3, 0, true);
    }

    public static OutPacket GetMiniGameOwnerForfeit(Minigame game) {
        return GetMiniGameResult(game, 0, 1, 0, 2, 1, true);
    }

    public static OutPacket GetMiniGameVisitorForfeit(Minigame game) {
        return GetMiniGameResult(game, 1, 0, 0, 1, 1, true);
    }

    public static OutPacket GetMiniGameClose() {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_EXIT);
        wp.write(1);
        wp.write(3);
        return wp.getPacket();
    }

    public static OutPacket GetMatchCard(Client c, Minigame miniGame, boolean owner, int piece) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_JOIN);
        wp.write(2);
        wp.write(2);
        wp.write(owner ? 0 : 1);
        wp.write(0);
        PacketCreator.AddCharLook(wp, miniGame.getOwner(), false);
        wp.writeMapleAsciiString(miniGame.getOwner().getName());
        if (miniGame.getVisitor() != null) {
            Player visitor = miniGame.getVisitor();
            wp.writeBool(true);
            PacketCreator.AddCharLook(wp, visitor, false);
            wp.writeMapleAsciiString(visitor.getName());
        }
        wp.write(0xFF);
        wp.write(0);
        wp.writeInt(2);
        wp.writeInt(miniGame.getOwner().getMiniGamePoints("wins", false));
        wp.writeInt(miniGame.getOwner().getMiniGamePoints("ties", false));
        wp.writeInt(miniGame.getOwner().getMiniGamePoints("losses", false));
        wp.writeInt(2000);
        if (miniGame.getVisitor() != null) {
            Player visitor = miniGame.getVisitor();
            wp.write(1);
            wp.writeInt(2);
            wp.writeInt(visitor.getMiniGamePoints("wins", false));
            wp.writeInt(visitor.getMiniGamePoints("ties", false));
            wp.writeInt(visitor.getMiniGamePoints("losses", false));
            wp.writeInt(2000);
        }
        wp.write(0xFF);
        wp.writeMapleAsciiString(miniGame.getDescription());
        wp.write(piece);
        wp.write(0);
        return wp.getPacket();
    }

    public static OutPacket GetMatchCardStart(Minigame game, int loser) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_START);
        wp.write(loser);
        int last = 13;
        if (game.getMatchesToWin() > 10){
            last = 31;
        }else if(game.getMatchesToWin() > 6){
            last = 21;
        }
        wp.write(last - 1);
        for (int i = 1; i < last; i++){
            wp.writeInt(game.getCardId(i));
        }
        return wp.getPacket();
    }

    public static OutPacket GetMatchCardNewVisitor(Player p, int slot) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_VISIT);
        wp.write(slot);
        PacketCreator.AddCharLook(wp, p, false);
        wp.writeMapleAsciiString(p.getName());
        wp.writeInt(1);
        wp.writeInt(p.getMiniGamePoints("wins", false));
        wp.writeInt(p.getMiniGamePoints("ties", false));
        wp.writeInt(p.getMiniGamePoints("losses", false));
        wp.writeInt(2000);
        return wp.getPacket();
    }

    public static OutPacket GetMatchCardSelect(Minigame game, int turn, int slot, int firstslot, int type) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        wp.write(ChannelHeaders.PlayerInteractionHeaders.ACT_SELECT_CARD);
        wp.write(turn);
        if (turn == 1)
            wp.write(slot);
        if (turn == 0) {
            wp.write(slot);
            wp.write(firstslot);
            wp.write(type);
        }
        return wp.getPacket();
    }
    
    public static OutPacket GetMatchCardOwnerWin(Minigame game){
        return GetMiniGameResult(game, 1, 0, 0, 1, 0, false);
    }

    public static OutPacket GetMatchCardVisitorWin(Minigame game){
        return GetMiniGameResult(game, 0, 1, 0, 2, 0, false);
    }

    public static OutPacket GetMatchCardTie(Minigame game){
        return GetMiniGameResult(game, 0, 0, 1, 3, 0, false);
    }

    public static OutPacket AddBoxGame(Player p, int ammount, int type, boolean omok) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        wp.writeInt(p.getId());
        AddAnnounceBox(wp, p.getMiniGame(), omok ? 1 : 2, 0, ammount, type);
        return wp.getPacket();
    }

    public static OutPacket RemoveCharBox(Player p) {
        final WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        wp.writeInt(p.getId());
        wp.writeBool(false);
        return wp.getPacket();
    }
}
