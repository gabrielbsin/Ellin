package server.minirooms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import client.player.Player;
import client.Client;
import packet.creators.InteractionPackets;
import packet.creators.MinigamePackets;
import packet.transfer.write.OutPacket;
import server.maps.object.AbstractMapleFieldObject;
import server.maps.object.FieldObjectType;

/**
 *
 * @author Matze
 */
public class Minigame extends AbstractMapleFieldObject {
    
    private final Player owner;
    private Player visitor;
    private String password;
    private String GameType = null;
    private final int[] piece = new int[250];
    private final List<Integer> list4x3 = new ArrayList<>();
    private final List<Integer> list5x4 = new ArrayList<>();
    private final List<Integer> list6x5 = new ArrayList<>();
    private final String description;
    private int loser = 1;
    private int piecetype;
    private int firstslot = 0;
    private int visitorpoints = 0;
    private int ownerpoints = 0;
    private int matchestowin = 0;

    public Minigame(Player owner, String description, String password) {
        this.owner = owner;
        this.description = description;
        this.password = password;
    }

    public boolean hasFreeSlot() {
        return visitor == null;
    }

    public boolean isOwner(Player c) {
        return owner.equals(c);
    }

     public void addVisitor(Player challenger) {
        visitor = challenger;
        if (GameType.equals("omok")) {
            this.getOwner().getClient().announce(MinigamePackets.GetMiniGameNewVisitor(challenger, 1));
            this.getOwner().getMap().broadcastMessage(MinigamePackets.AddBoxGame(owner, 2, 0, true));
        }
        if (GameType.equals("matchcard")) {
            this.getOwner().getClient().announce(MinigamePackets.GetMatchCardNewVisitor(challenger, 1));
            this.getOwner().getMap().broadcastMessage(MinigamePackets.AddBoxGame(owner, 2, 0, false));
        }
    }

     public void removeVisitor(Player challenger) {
        if (visitor == challenger) {
            visitor = null;
            this.getOwner().getClient().announce(MinigamePackets.GetMiniGameRemoveVisitor());
            if (GameType.equals("omok")) {
                this.getOwner().getMap().broadcastMessage(MinigamePackets.AddBoxGame(owner, 1, 0, true));
            }
            if (GameType.equals("matchcard")) {
                this.getOwner().getMap().broadcastMessage(MinigamePackets.AddBoxGame(owner, 1, 0, false));
            }
        }
    }

    public boolean isVisitor(Player challenger) {
        return visitor == challenger;
    }

    public void broadcastToVisitor(OutPacket packet) {
        if (visitor != null) {
            visitor.getClient().announce(packet);
        }
    }

    public void setFirstSlot(int type) {
        firstslot = type;
    }

    public int getFirstSlot() {
        return firstslot;
    }

    public void setOwnerPoints() {
        ownerpoints++;
        if (ownerpoints + visitorpoints == matchestowin) {
            if (ownerpoints == visitorpoints) {
                this.broadcast(MinigamePackets.GetMatchCardTie(this));
            } else if (ownerpoints > visitorpoints) {
                this.broadcast(MinigamePackets.GetMatchCardOwnerWin(this));
            } else {
                this.broadcast(MinigamePackets.GetMatchCardVisitorWin(this));
            }
            ownerpoints = 0;
            visitorpoints = 0;
        }
    }

    public void setVisitorPoints() {
        visitorpoints++;
        if (ownerpoints + visitorpoints == matchestowin) {
            if (ownerpoints > visitorpoints) {
                this.broadcast(MinigamePackets.GetMiniGameOwnerWin(this));
            } else if (visitorpoints > ownerpoints) {
                this.broadcast(MinigamePackets.GetMiniGameVisitorWin(this));
            } else {
                this.broadcast(MinigamePackets.GetMiniGameTie(this));
            }
            ownerpoints = 0;
            visitorpoints = 0;
        }
    }

    public void setMatchesToWin(int type) {
        matchestowin = type;
    }

    public void setPieceType(int type) {
        piecetype = type;
    }

    public int getPieceType() {
        return piecetype;
    }

    public void setGameType(String game) {
        GameType = game;
        if (game.equals("matchcard")) {
            switch (matchestowin) {
                case 6:
                    for (int i = 0; i < 6; i++) {
                        list4x3.add(i);
                        list4x3.add(i);
                    }   break;
                case 10:
                    for (int i = 0; i < 10; i++) {
                        list5x4.add(i);
                        list5x4.add(i);
                    }   break;
                default:
                    for (int i = 0; i < 15; i++) {
                        list6x5.add(i);
                        list6x5.add(i);
                    }   break;
            }
        }
    }

    public String getGameType() {
        return GameType;
    }

   public void shuffleList() {
        switch (matchestowin) {
            case 6:
                Collections.shuffle(list4x3);
                break;
            case 10:
                Collections.shuffle(list5x4);
                break;
            default:
                Collections.shuffle(list6x5);
                break;
        }
    }

    public int getCardId(int slot) {
        int cardid;
        switch (matchestowin) {
            case 6:
                cardid = list4x3.get(slot - 1);
                break;
            case 10:
                cardid = list5x4.get(slot - 1);
                break;
            default:
                cardid = list6x5.get(slot - 1);
                break;
        }
        return cardid;
    }

    public int getMatchesToWin() {
        return matchestowin;
    }

    public void setLoser(int type) {
        loser = type;
    }

    public int getLoser() {
        return loser;
    }
    
    public void broadcast(OutPacket packet) {
        if (owner.getClient() != null && owner.getClient().getSession() != null) {
            owner.getClient().announce(packet);
        }
        broadcastToVisitor(packet);
    }

    public void chat(Client c, String chat) {
        broadcast(InteractionPackets.GetPlayerShopChat(c.getPlayer(), chat, isOwner(c.getPlayer())));
    }

    public void sendOmok(Client c, int type) {
        c.announce(MinigamePackets.GetMiniGame(c, this, isOwner(c.getPlayer()), type));
    }

    public void sendMatchCard(Client c, int type) {
        c.announce(MinigamePackets.GetMatchCard(c, this, isOwner(c.getPlayer()), type));
    }

    public Player getOwner() {
        return owner;
    }

    public Player getVisitor() {
        return visitor;
    }
    
    public void setPiece(int move1, int move2, int type, Player chr) {
        int slot = ((move2 * 15) + (move1 + 1));
        if (piece[slot] == 0) {
            piece[slot] = type;
            broadcast(MinigamePackets.GetMiniGameMoveOmok(this, move1, move2, type));
            for (int y = 0; y < 15; y++) {
                for (int x = 0; x < 11; x++) {
                    if (searchCombo(x, y, type)) {
                        if (isOwner(chr)) {
                            broadcast(MinigamePackets.GetMiniGameOwnerWin(this));
                            setLoser(0);
                        } else {
                            broadcast(MinigamePackets.GetMiniGameVisitorWin(this));
                            this.setLoser(1);
                        }
                        for (int y2 = 0; y2 < 15; y2++) {
                            for (int x2 = 0; x2 < 15; x2++) {
                                int slot2 = ((y2 * 15) + (x2 + 1));
                                piece[slot2] = 0;

                            }
                        }
                    }
                }
            }
            for (int y = 0; y < 15; y++) {
                for (int x = 4; x < 15; x++) {
                    if (searchCombo2(x, y, type)) {
                        if (isOwner(chr)) {
                            broadcast(MinigamePackets.GetMiniGameOwnerWin(this));
                            setLoser(0);
                        } else {
                            broadcast(MinigamePackets.GetMiniGameVisitorWin(this));
                            setLoser(1);
                        }
                        for (int y2 = 0; y2 < 15; y2++) {
                            for (int x2 = 0; x2 < 15; x2++) {
                                int slot2 = ((y2 * 15) + (x2 + 1));
                                piece[slot2] = 0;

                            }
                        }
                    }
                }
            }
        }

    }

   public boolean searchCombo(int x, int y, int type) {
        boolean winner = false;
        int slot = ((y * 15) + (x + 1));
        if (piece[slot] == type) {
            if (piece[slot + 1] == type) {
                if (piece[slot + 2] == type) {
                    if (piece[slot + 3] == type) {
                        if (piece[slot + 4] == type) {
                            winner = true;
                        }
                    }
                }
            }
        }
        if (piece[slot] == type) {
            if (piece[slot + 16] == type) {
                if (piece[slot + 32] == type) {
                    if (piece[slot + 48] == type) {
                        if (piece[slot + 64] == type) {
                            winner = true;
                        }
                    }
                }
            }
        }
        if (piece[slot] == type) {
            if (piece[slot + 15] == type) {
                if (piece[slot + 30] == type) {
                    if (piece[slot + 45] == type) {
                        if (piece[slot + 60] == type) {
                            winner = true;
                        }
                    }
                }
            }
        }
        return winner;
    }

    public boolean searchCombo2(int x, int y, int type) {
        boolean winner = false;
        int slot = ((y * 15) + (x + 1));
        if (piece[slot] == type) {
            if (piece[slot + 15] == type) {
                if (piece[slot + 30] == type) {
                    if (piece[slot + 45] == type) {
                        if (piece[slot + 60] == type) {
                            winner = true;
                        }
                    }
                }
            }
        }
        if (piece[slot] == type) {
            if (piece[slot + 14] == type) {
                if (piece[slot + 28] == type) {
                    if (piece[slot + 42] == type) {
                        if (piece[slot + 56] == type) {
                            winner = true;
                        }
                    }
                }
            }
        }
        return winner;
    }
    
    public String getPassword() {
        return this.password;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public void sendDestroyData(Client client) {}

    @Override
    public void sendSpawnData(Client client) {}

    @Override
    public FieldObjectType getType() {
        return FieldObjectType.MINI_GAME;
    }
}