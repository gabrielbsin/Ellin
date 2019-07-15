/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

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
package handling.channel;

import handling.world.CheaterData;
import handling.world.service.FindService;
import client.player.Player;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import client.player.PlayerStringUtil;

public class PlayerStorage {

    private final Lock pStorageLock = new ReentrantLock();
    private final Map<String, Player> nameToChar = new HashMap<>();
    private final Map<Integer, Player> idToChar = new HashMap<>();
    private final int channel;

    public PlayerStorage(int channel) {
	this.channel = channel;
    }

    public final ArrayList<Player> getAllCharacters() {
        pStorageLock.lock();
        try {
            return new ArrayList<>(idToChar.values());
        } finally {
            pStorageLock.unlock();
        }
    }

    public final void registerPlayer(final Player chr) {
        pStorageLock.lock();
        try {
            nameToChar.put(chr.getName().toLowerCase(), chr);
            idToChar.put(chr.getId(), chr);
        } finally {
            pStorageLock.unlock();
        }
        FindService.register(chr.getId(), chr.getName(), channel);
    }

   
    public final void deregisterPlayer(final Player chr) {
        pStorageLock.lock();
        try {
            nameToChar.remove(chr.getName().toLowerCase());
            idToChar.remove(chr.getId());
        } finally {
            pStorageLock.unlock();
        }
        FindService.forceDeregister(chr.getId(), chr.getName());
    }

    public final void deregisterPlayer(final int idz, final String namez) {
        pStorageLock.lock();
        try {
            nameToChar.remove(namez.toLowerCase());
            idToChar.remove(idz);
        } finally {
            pStorageLock.unlock();
        }
        FindService.forceDeregister(idz, namez);
    }

    public final Player getCharacterByName(final String name) {
        pStorageLock.lock();
        try {
            return nameToChar.get(name.toLowerCase());
        } finally {
            pStorageLock.unlock();
        }
    }

    public final Player getCharacterById(final int id) {
        pStorageLock.lock();
        try {
            return idToChar.get(id);
        } finally {
            pStorageLock.unlock();
        }
    }

    public final int getConnectedClients() {
        return idToChar.size();
    }
    
    public final int getCheatersSize() {
        int size = 0;
        pStorageLock.lock();
        try {
            final Iterator<Player> itr = nameToChar.values().iterator();
            Player chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (chr.getCheatTracker().getPoints() > 0) {
                    size++;
                }
            }
        } finally {
            pStorageLock.unlock();
        }
        return size;
    }

    
    public final List<CheaterData> getCheaters() {
        final List<CheaterData> cheaters = new ArrayList<>();

        pStorageLock.lock();
        try {
            final Iterator<Player> itr = nameToChar.values().iterator();
            Player chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (chr.getCheatTracker().getPoints() > 0) {
                    cheaters.add(new CheaterData(chr.getCheatTracker().getPoints(), PlayerStringUtil.makeMapleReadable(chr.getName()) + " (" + chr.getCheatTracker().getPoints() + ") " + chr.getCheatTracker().getSummary()));
                }
            }
        } finally {
            pStorageLock.unlock();
        }
        return cheaters;
    }


    public final void disconnectAll() {
        disconnectAll(false);
    }

    public final void disconnectAll(final boolean checkGM) {
        pStorageLock.lock();
        try {
            final Iterator<Player> itr = nameToChar.values().iterator();
            Player chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (!chr.isGameMaster() || !checkGM) {
                    chr.getClient().disconnect(false, false);
                    chr.getClient().getSession().close();
                    FindService.forceDeregister(chr.getId(), chr.getName());
                    itr.remove();
                }
            }
        } finally {
            pStorageLock.unlock();
        }
    }

    public final String getOnlinePlayers(final boolean byGM) {
        final StringBuilder sb = new StringBuilder();

        if (byGM) {
            pStorageLock.lock();
            try {
                final Iterator<Player> itr = nameToChar.values().iterator();
                while (itr.hasNext()) {
                    sb.append(PlayerStringUtil.makeMapleReadable(itr.next().getName()));
                    sb.append(", ");
                }
            } finally {
                pStorageLock.unlock();
            }
        } else {
            pStorageLock.lock();
            try {
                final Iterator<Player> itr = nameToChar.values().iterator();
                Player chr;
                while (itr.hasNext()) {
                    chr = itr.next();

                    if (!chr.isGameMaster()) {
                        sb.append(PlayerStringUtil.makeMapleReadable(chr.getName()));
                        sb.append(", ");
                    }
                }
            } finally {
                pStorageLock.unlock();
            }
        }
        return sb.toString();
    }

    public final void broadcastPacket(final byte[] data) {
        pStorageLock.lock();
        try {
            final Iterator<Player> itr = nameToChar.values().iterator();
            while (itr.hasNext()) {
                itr.next().getClient().getSession().write(data);
            }
        } finally {
            pStorageLock.unlock();
        }
    }

    public final void broadcastSmegaPacket(final byte[] data) {
        pStorageLock.lock();
        try {
            final Iterator<Player> itr = nameToChar.values().iterator();
            Player chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (chr.getClient().isLoggedIn() && chr.getSmegaEnabled()) {
                    chr.getClient().getSession().write(data);
                }
            }
        } finally {
            pStorageLock.unlock();
        }
    }

    public final void broadcastGMPacket(final byte[] data) {
        pStorageLock.lock();
        try {
            final Iterator<Player> itr = nameToChar.values().iterator();
            Player chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (chr.getClient().isLoggedIn()) {
                    chr.getClient().getSession().write(data);
                }
            }
        } finally {
            pStorageLock.unlock();
        }
    }
}
