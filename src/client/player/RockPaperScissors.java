package client.player;

import client.Client;
import packet.creators.PacketCreator;
import server.itens.InventoryManipulator;
import tools.Randomizer;

/**
 * @author AuraSEA/kevintjuh93 
 */

public class RockPaperScissors {

    private int round = 0;
    private boolean ableAnswer = true;
    private boolean win = false;

    public RockPaperScissors(final Client c, final byte mode) {
        c.getSession().write(PacketCreator.GetRockPaperScissorsMode((byte) (0x09 + mode), -1, -1, -1));
        if (mode == 0) {
            c.getPlayer().gainMeso(-1000, true, true, true);
        }
    }

    public final boolean answer(final Client c, final int answer) {
        if (ableAnswer && !win && answer >= 0 && answer <= 2) {
            final int response = Randomizer.nextInt(3);
            if (response == answer) {
                c.getSession().write(PacketCreator.GetRockPaperScissorsMode((byte) 0x0B, -1, (byte) response, (byte) round));
            } else if ((answer == 0 && response == 2) || (answer == 1 && response == 0) || (answer == 2 && response == 1)) {
                c.getSession().write(PacketCreator.GetRockPaperScissorsMode((byte) 0x0B, -1, (byte) response, (byte) (round + 1)));
                ableAnswer = false;
                win = true;
            } else { 
                c.getSession().write(PacketCreator.GetRockPaperScissorsMode((byte) 0x0B, -1, (byte) response, (byte) -1));
                ableAnswer = false;
            }
            return true;
        }
        reward(c);
        return false;
    }

    public final boolean timeOut(final Client c) {
        if (ableAnswer && !win) {
            ableAnswer = false;
            c.getSession().write(PacketCreator.GetRockPaperScissorsMode((byte) 0x0A, -1, -1, -1));
            return true;
        }
        reward(c);
        return false;
    }

    public final boolean nextRound(final Client c) {
        if (win) {
            round++;
            if (round < 10) {
                win = false;
                ableAnswer = true;
                c.getSession().write(PacketCreator.GetRockPaperScissorsMode((byte) 0x0C, -1, -1, -1));
                return true;
            }
        }
        reward(c);
        return false;
    }

    public final void reward(final Client c) {
        if (win) {
           InventoryManipulator.addById(c, 4031332 + round, (short) 1, "");
        } else if (round == 0) {
            c.getPlayer().gainMeso(500, true, true, true);
        }
        c.getPlayer().setRPS(null);
    }

    public final void dispose(final Client c) {
        reward(c);
        c.getSession().write(PacketCreator.GetRockPaperScissorsMode((byte) 0x0D, -1, -1, -1));
    }
}
