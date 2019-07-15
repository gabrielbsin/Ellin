/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package handling.world.service;

import handling.channel.ChannelServer;
import handling.world.CharacterIdChannelPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FindService {

    private static final HashMap<Integer, Integer> idToChannel = new HashMap<>();
    private static final HashMap<String, Integer> nameToChannel = new HashMap<>();
    private static final ReentrantReadWriteLock findLock = new ReentrantReadWriteLock();

    public static void register(int id, String name, int channel) {
        findLock.writeLock().lock();
        try {
            idToChannel.put(id, channel);
            nameToChannel.put(name.toLowerCase(), channel);
        } finally {
            findLock.writeLock().unlock();
        }
    }

    public static void forceDeregister(int id) {
        findLock.writeLock().lock();
        try {
            idToChannel.remove(id);
        } finally {
            findLock.writeLock().unlock();
        }
    }

    public static void forceDeregister(String id) {
        findLock.writeLock().lock();
        try {
            nameToChannel.remove(id.toLowerCase());
        } finally {
            findLock.writeLock().unlock();
        }
    }

    public static void forceDeregister(int id, String name) {
        findLock.writeLock().lock();
        try {
            idToChannel.remove(id);
            nameToChannel.remove(name.toLowerCase());
        } finally {
            findLock.writeLock().unlock();
        }
    }

    public static int findChannel(int id) {
        Integer ret;
        findLock.readLock().lock();
        try {
            ret = idToChannel.get(id);
        } finally {
            findLock.readLock().unlock();
        }
        if (ret != null) {
            if (ret != -10 && ret != -20 && ChannelServer.getInstance(ret) == null) { 
                forceDeregister(id);
                return -1;
            }
            return ret;
        }
        return -1;
    }

    public static int findChannel(String st) {
        Integer ret;
        findLock.readLock().lock();
        try {
            ret = nameToChannel.get(st.toLowerCase());
        } finally {
            findLock.readLock().unlock();
        }
        if (ret != null) {
            if (ret != -10 && ret != -20 && ChannelServer.getInstance(ret) == null) { 
                forceDeregister(st);
                return -1;
            }
            return ret;
        }
        return -1;
    }

    public static CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) {
        List<CharacterIdChannelPair> foundsChars = new ArrayList<>(characterIds.length);
        for (int i : characterIds) {
            int channel = findChannel(i);
            if (channel > 0) {
                foundsChars.add(new CharacterIdChannelPair(i, channel));
            }
        }
        Collections.sort(foundsChars);
        return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
    }  
}
