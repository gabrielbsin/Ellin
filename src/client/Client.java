/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
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

package client;

import client.player.PlayerStringUtil;
import client.player.Player;
import client.player.PlayerSaveFactory.DeleteType;
import community.MapleBuddyList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import javax.script.ScriptEngine;
import database.DatabaseConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import packet.transfer.write.OutPacket;
import handling.channel.ChannelServer;
import handling.world.messenger.MapleMessengerCharacter;
import community.MapleParty;
import community.MaplePartyCharacter;
import community.MaplePartyOperation;
import community.MapleGuildCharacter;
import handling.login.handler.CharLoginHeaders;
import handling.world.service.BuddyService;
import handling.world.service.FindService;
import handling.world.service.GuildService;
import handling.world.service.MessengerService;
import handling.world.service.PartyService;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import scripting.npc.NPCScriptManager;
import scripting.npc.NPCConversationManager;
import scripting.quest.QuestScriptManager;
import scripting.quest.QuestActionManager;
import tools.Pair;
import org.apache.mina.core.session.IoSession;
import packet.creators.LoginPackets;
import packet.creators.PacketCreator;
import packet.crypto.MapleCrypto;
import scripting.AbstractPlayerInteraction;
import scripting.event.EventInstanceManager;
import scripting.event.EventManager;
import tools.TimerTools.ClientTimer;
import server.maps.Field;
import server.partyquest.mcpq.MCField;
import tools.FileLogger;
import tools.HexTool;
import tools.locks.MonitoredLockType;
import tools.locks.MonitoredReentrantLock;

public class Client {

    public static final String CLIENT_KEY = "CLIENT";
    private MapleCrypto send;
    private MapleCrypto receive;
    private final IoSession session;
    private Player player;
    private int channel = 1;
    private int accId = 1;
    private int world;
    private int gmlevel; 
    private int voteTime = -1;
    private int pinattempt = 0;
    private byte gender = -1, greason = 1, characterSlots;
    private boolean guest, loggedIn = false, serverTransition = false;
    private Calendar birthday = null, tempban = null;
    private String accountName, hwid = null;
    private long lastPong;
    private long sessionId, attemptedLogins = 0;
    private long lastNPCTalk;
    private long lastNpcClick;
    private boolean gm;
    public transient short loginAttempt = 0;
    private String pin = null;
    private boolean disconnecting = false;
    private final Map<Pair<Player, Integer>, Integer> timesTalked = new HashMap<>();
    private final transient Set<String> macs = new HashSet<>();
    private final transient Map<String, ScriptEngine> engines = new HashMap<>();
    private transient ScheduledFuture<?> idleTask = null;
    /* =============================================== LOCKS =============================================== */
    private final Lock lock = new MonitoredReentrantLock(MonitoredLockType.CLIENT, true);
    private final Lock encoderLock = new MonitoredReentrantLock(MonitoredLockType.CLIENT, true);
    private static final Lock loginLock = new MonitoredReentrantLock(MonitoredLockType.CLIENT, true);
 
    public Client(MapleCrypto send, MapleCrypto receive, IoSession session) {
        this.send = send;
        this.receive = receive;
        this.session = session;
    }      

    public long getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }  

    public MapleCrypto getReceiveCrypto() {
        return receive;
    }

    public MapleCrypto getSendCrypto() {
        return send;
    }

    public IoSession getSession() {
        return session;
    }

    public void write(OutPacket o) {
        getSession().write(o);
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void sendCharList(int server) {
        this.session.write(LoginPackets.GetCharList(this, server));
    }

    public boolean acceptToS() {
        boolean disconnect = false;
        if (accountName == null) {
            return true;
        }
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `tos` FROM accounts WHERE id = ?");
            ps.setInt(1, accId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                if (rs.getByte("tos") == 1) {
                    disconnect = true;
                }
            }
            ps.close();
            rs.close();
            ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET tos = 1 WHERE id = ?");
            ps.setInt(1, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.err.println(e);
        }
        return disconnect;
    }

    public List<Player> loadCharacters(int serverId) {
        List<Player> chars = new LinkedList<>();
        loadCharactersInternal(serverId).forEach((cni) -> {
            try {
                chars.add(Player.loadinCharacterDatabase(cni.id, this, false));
            } catch (SQLException e) {
                System.err.println("error loading characters internal" + e);
            }
        });
        return chars;
    }

    public List<String> loadCharacterNames(int serverId) {
        List<String> chars = new LinkedList<>();
        loadCharactersInternal(serverId).forEach((cni) -> {
            chars.add(cni.name);
        });
        return chars;
    }

    public void declare(OutPacket packet) {
        this.session.write(packet);
    }

    private List<CharNameAndId> loadCharactersInternal(int serverId) {
        List<CharNameAndId> chars = new ArrayList<>(15);
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT id, name FROM characters WHERE accountid = ? AND world = ?")) {
                ps.setInt(1, this.accId);
                ps.setInt(2, serverId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        chars.add(new CharNameAndId(rs.getString("name"), rs.getInt("id")));
                    }
                    rs.close();
                    ps.close();
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error loadCharactersInternal in Client!");
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
        }
        return chars;
    }

    public boolean isLoggedIn() {
        return (this.loggedIn) && (this.accId >= 0);
    }

    private Calendar getTempBanCalendar(ResultSet rs) throws SQLException {
        Calendar lTempban = Calendar.getInstance();
        long blubb = rs.getLong("tempban");
        if (blubb == 0) { 
            lTempban.setTimeInMillis(0);
            return lTempban;
        }
        Calendar today = Calendar.getInstance();
        lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
        if (today.getTimeInMillis() < lTempban.getTimeInMillis()) {
            return lTempban;
        }

        lTempban.setTimeInMillis(0);
        return lTempban;
    }

    public Calendar getTempBanCalendar() {
        return tempban;
    }

    public byte getBanReason() {
        return greason;
    }

    public boolean hasBannedIP() {
        boolean ret = false;
        if (session.getRemoteAddress() == null) {
            return true;
        }
        try {
           try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')")) {
               ps.setString(1, session.getRemoteAddress().toString());
               try (ResultSet rs = ps.executeQuery()) {
                   rs.next();
                   if (rs.getInt(1) > 0) {
                       ret = true;
                   }
               }
           }
        } catch (SQLException ex) {
            System.out.println("[ERROR] Error hasBannedIP in Client!");
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, ex);
        }
        return ret;
    }

    public boolean hasBannedHWID() {
        if (hwid == null) {
            return false;
        }
        boolean ret = false;
        PreparedStatement ps = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("SELECT COUNT(*) FROM hwidbans WHERE hwid LIKE ?");
            ps.setString(1, hwid);
            ResultSet rs = ps.executeQuery();
            if (rs != null && rs.next()) {
                if (rs.getInt(1) > 0) 
                   ret = true; 
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error hasBannedHWID in Client!");
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
        } finally {
            try {
                if(ps != null && !ps.isClosed()) {
                    ps.close();
                } 
            } catch (SQLException e){
                System.out.println("[ERROR] Error hasBannedHWID in Client!");
                FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
            }
        }
        return ret;
    }

    public boolean hasBannedMac() {
        if (macs.isEmpty()) {
            return false;
        }
        int i = 0;
        boolean ret = false;
        try {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM macbans WHERE mac IN (");
            for (i = 0; i < macs.size(); i++) {
                sql.append("?");
                if (i != macs.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")");
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql.toString())) {
                i = 0;
                for (String mac : macs) {
                    i++;
                    ps.setString(i, mac);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        ret = true;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error hasBannedMac in Client!");
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
        }
        return ret;
    }
        
    private void loadHWIDIfNescessary() throws SQLException {
        if (hwid == null) {
            try(PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT hwid FROM accounts WHERE id = ?")) {
                ps.setInt(1, accId);
                try(ResultSet rs = ps.executeQuery()) {
                    if(rs.next()) {
                        hwid = rs.getString("hwid");
                    }
                   rs.close();
                   ps.close();
                } 
            }   
        }
    }

    private void loadMacsIfNescessary() throws SQLException {
        if (macs.isEmpty()) {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT macs FROM accounts WHERE id = ?")) {
                ps.setInt(1, accId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        for (String mac : rs.getString("macs").split(", ")) {
                            if (!mac.equals("")) {
                                macs.add(mac);
                            }
                        }
                    }
                }
            }
        }
    }
        
    public void banHWID() {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        try {
            loadHWIDIfNescessary();
            ps = con.prepareStatement("INSERT INTO hwidbans (hwid) VALUES (?)");
            ps.setString(1, hwid);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[ERROR] Error banHWID in Client!");
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
        } finally {
            try {
            if(ps != null && !ps.isClosed())
               ps.close();
            } catch (SQLException e) {
                System.err.println(e);
            }
        }
    }

    public void banMacs() {
        Connection con = DatabaseConnection.getConnection();
        try {
            loadMacsIfNescessary();
            List<String> filtered = new LinkedList<>();
            PreparedStatement ps = con.prepareStatement("SELECT filter FROM macfilters");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                filtered.add(rs.getString("filter"));
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("INSERT INTO macbans (mac) VALUES (?)");
            for (String mac : macs) {
                boolean matched = false;
                for (String filter : filtered) {
                    if (mac.matches(filter)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    ps.setString(1, mac);
                    ps.executeUpdate();
                }
            }
            ps.close();
        } catch (SQLException e) {
            System.out.println("[ERROR] Error banMacs in Client!");
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
        }
    }
    
    public int finishLogin() {
        loginLock.lock();
        try {
            final ClientLoginState state = getLoginState();
            if (state.getState() > ClientLoginState.LOGIN_LOGGEDIN.getState()) {
                loggedIn = false;
                return 7;
            }
            updateLoginState(ClientLoginState.LOGIN_LOGGEDIN, getSessionIPAddress());
        } finally {
            loginLock.unlock();
        }

        return 0;
    }
             
    public int clientLogin(String login, String pwd) {
        attemptedLogins++;
        if (attemptedLogins > 4) {
            getSession().close();
        }
        int loginStatus = 5;
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT id, password, salt, banned, gm, pin, gender, greason, tempban, characterslots, tos FROM accounts WHERE name = ?");
            ps.setString(1, login);
            rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getByte("banned") == 1) {
                    return CharLoginHeaders.LOGIN_BLOCKED;
                }
                accId = rs.getInt("id");
                gmlevel = rs.getInt("gm");
                pin = rs.getString("pin");
                gender = rs.getByte("gender");
                greason = rs.getByte("greason");
                tempban = getTempBanCalendar(rs);
                characterSlots = rs.getByte("characterslots");
                
                String passhash = rs.getString("password");
                String salt = rs.getString("salt");
                byte tos = rs.getByte("tos");
                ps.close();
                rs.close();
                ClientLoginState loginstate = getLoginState();
                if (loginstate.getState() > ClientLoginState.LOGIN_NOTLOGGEDIN.getState()) {  
                    loggedIn = false;
                    loginStatus = CharLoginHeaders.LOGIN_ALREADY;
                     if (pwd.equalsIgnoreCase("fixme")) {
                        try {
                            ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE name = ?");
                            ps.setString(1, login);
                            ps.executeUpdate();
                            ps.close();
                        } catch (SQLException se) {
                            System.out.println("[ERROR] Error clientLogin in Client!");
                            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, se);
                        }
                    }
                }
                
                boolean found = false;
                if (loginStatus == CharLoginHeaders.LOGIN_ALREADY){ 
                    for (ChannelServer ch : ChannelServer.getAllInstances()){
                        for (Player c : ch.getPlayerStorage().getAllCharacters()){
                            if(c.getAccountID() == accId){
                                found = true;
                                break;
                            }
                        }
                    }
                }
                if (!found){
                    loginStatus = 0;
                }
                
                if (pwd.equals(passhash) || checkHash(passhash, "SHA-1", pwd) || checkHash(passhash, "SHA-512", pwd + salt)) {
                    if (tos == 0) {
                        loginStatus = CharLoginHeaders.LOGIN_TOS;
                    } else {
                        loginStatus = CharLoginHeaders.LOGIN_OK;
                    }
                } else {
                    loggedIn = false;
                    loginStatus = CharLoginHeaders.LOGIN_WRONG;
                }
                ps = con.prepareStatement("INSERT INTO iplog (accountid, ip) VALUES (?, ?)");
                ps.setInt(1, accId);
                ps.setString(2, session.getRemoteAddress().toString());
                ps.executeUpdate();
                }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error clientLogin in Client!");
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
        } finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
                if (rs != null && !rs.isClosed()) {
                    rs.close();
                }
            } catch (SQLException e) {
                System.out.println("[ERROR] Error clientLogin in Client!");
                FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
            }
        }
        if (loginStatus == 0) {
            attemptedLogins = 0;
        }
        return loginStatus;
    }
        
    private static boolean checkHash(String hash, String type, String password) {
        try {
            MessageDigest digester = MessageDigest.getInstance(type);
            digester.update(password.getBytes("UTF-8"), 0, password.length());
            return HexTool.toString(digester.digest()).replace(" ", "").toLowerCase().equals(hash);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding the string failed", e);
        }
    }
    
    private void unban() {
        int i;
        try {
            Connection con = DatabaseConnection.getConnection();
            loadMacsIfNescessary();
            StringBuilder sql = new StringBuilder("DELETE FROM macbans WHERE mac IN (");
            for (i = 0; i < macs.size(); i++) {
                sql.append("?");
                if (i != macs.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")");
            PreparedStatement ps = con.prepareStatement(sql.toString());
            i = 0;
            for (String mac : macs) {
                i++;
                ps.setString(i, mac);
            }
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("DELETE FROM ipbans WHERE ip LIKE CONCAT(?, '%')");
            ps.setString(1, getSession().getRemoteAddress().toString().split(":")[0]);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("UPDATE accounts SET banned = 0 WHERE id = ?");
            ps.setInt(1, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.out.println("[ERROR] Error unban in Client!");
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
        }
    }

    public void updateHWID(String newHwid) {
        String[] split = newHwid.split("_");
        if(split.length > 1 && split[1].length() == 8) {
            StringBuilder hwid = new StringBuilder();
            String convert = split[1]; 

            int len = convert.length();
            for(int i=len-2; i >= 0; i -= 2) {
                hwid.append(convert.substring(i, i + 2));
            }
            hwid.insert(4, "-");
            this.hwid = hwid.toString();
            PreparedStatement ps = null;
            try {
                ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET hwid = ? WHERE id = ?");
                ps.setString(1, this.hwid);
                ps.setInt(2, accId);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                System.out.println("[ERROR] Error updateHWID in Client!");
                FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
            } finally {
                try {
                    if(ps != null && !ps.isClosed()) {
                        ps.close();
                    }
                } catch (SQLException e) {
                    System.err.println(e);
                }
            }
        } else {
            this.disconnect(false, false); 
        }
    }

    public void updateMacs(String macData) {
        macs.addAll(Arrays.asList(macData.split(", ")));
        StringBuilder newMacData = new StringBuilder();
        Iterator<String> iter = macs.iterator();
        while (iter.hasNext()) {
            String cur = iter.next();
            newMacData.append(cur);
            if (iter.hasNext()) {
                newMacData.append(", ");
            }
        }
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET macs = ? WHERE id = ?")) {
                ps.setString(1, newMacData.toString());
                ps.setInt(2, accId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
   
    public final void updateLoginState(final ClientLoginState newstate, final String SessionID) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET loggedin = ?, lastlogin = CURRENT_TIMESTAMP() WHERE id = ?")) {
                ps.setInt(1, newstate.getState());
                ps.setInt(2, getAccountID());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("error updating login state" + e);
        }
        if (newstate == ClientLoginState.LOGIN_NOTLOGGEDIN) {
            loggedIn = false;
            serverTransition = false;
        } else {
            serverTransition = (newstate == ClientLoginState.LOGIN_SERVER_TRANSITION || newstate == ClientLoginState.CHANGE_CHANNEL);
            loggedIn = !serverTransition;
        }
    }

    public ClientLoginState getLoginState() {  
        try {
            Connection con = DatabaseConnection.getConnection();
            ClientLoginState state;
            try (PreparedStatement ps = con.prepareStatement("SELECT loggedin, lastlogin, UNIX_TIMESTAMP(birthday) as birthday FROM accounts WHERE id = ?")) {
                ps.setInt(1, getAccountID());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        throw new RuntimeException("getLoginState - MapleClient");
                    }
                    birthday = Calendar.getInstance();
                    long blubb = rs.getLong("birthday");
                    if (blubb > 0) {
                        birthday.setTimeInMillis(blubb * 1000);
                    }
                    state = ClientLoginState.getStateByInt(rs.getByte("loggedin"));

                    if (state == ClientLoginState.LOGIN_SERVER_TRANSITION || state == ClientLoginState.CHANGE_CHANNEL) {
                        if (rs.getTimestamp("lastlogin").getTime() + 20000 < System.currentTimeMillis()) {
                            state = ClientLoginState.LOGIN_NOTLOGGEDIN;
                            updateLoginState(state, getSessionIPAddress());
                        }
                    }  else if (state == ClientLoginState.LOGIN_LOGGEDIN && player == null) {
                        state = ClientLoginState.LOGIN_LOGGEDIN;
                        updateLoginState(state, getSessionIPAddress());
                    }
                }
            }
            switch (state) {
                case LOGIN_LOGGEDIN:
                    loggedIn = true;
                    break;
                case LOGIN_SERVER_TRANSITION:
                    PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE id = ?");
                    ps.setInt(1, this.getAccountID());
                    ps.executeUpdate();
                    ps.close();
                    break;
                default:
                    loggedIn = false;
                    break;
            }
            return state;
        } catch (SQLException e) {
            System.err.println(e);
            loggedIn = false;
            throw new RuntimeException("login state");
        }
    }
           
    public int getVoteTime(){
        if (voteTime != -1){
            return voteTime;
        }
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT date FROM bitsite_votingrecords WHERE UPPER(account) = UPPER(?)")) {
                ps.setString(1, accountName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return -1;
                    }
                   voteTime = rs.getInt("date");
                }
            }
        } catch (SQLException e) {
            System.err.println(e);
            FileLogger.printError("hasVotedAlready.txt", e);
            return -1;
        }
        return voteTime;
    }

    public void resetVoteTime() {
        voteTime = -1;
    }

    public boolean hasVotedAlready(){
        Date currentDate = new Date();
        int timeNow = (int) (currentDate.getTime() / 1000);
        int difference = (timeNow - getVoteTime());
        return difference < 86400 && difference > 0;
    }    

    public boolean checkBirthDate(Calendar date) {
        return date.get(Calendar.YEAR) == birthday.get(Calendar.YEAR) && date.get(Calendar.MONTH) == birthday.get(Calendar.MONTH) && date.get(Calendar.DAY_OF_MONTH) == birthday.get(Calendar.DAY_OF_MONTH);
    }

    public final void disconnect(final boolean RemoveInChannelServer, final boolean fromCS) {
        disconnect(RemoveInChannelServer, fromCS, false);
    }

    public final void disconnect(boolean RemoveInChannelServer, boolean cashshop, boolean shutdown) {
        if (player != null && player.getClient() != null) {
            Field map = player.getMap();
            final String namez = player.getName();
            final boolean hidden = player.isHidden();
            final int gmLevel = player.getAdministrativeLevel();
            final int idz = player.getId(), messengerID = player.getMessenger() == null ? 0 : player.getMessenger().getId(),  gid = player.getGuildId();
            final MapleBuddyList bl = player.getBuddylist();
            final MapleParty party = player.getParty();
            final MapleMessengerCharacter chrm = new MapleMessengerCharacter(player);
            final MaplePartyCharacter chrp = new MaplePartyCharacter(player);
            final MapleGuildCharacter chrg = player.getMGC();

            
            player.cancelMagicDoor();

            if (channel == -1 || shutdown) {
                removalTask(player);
                disposePlayer(player);
                player.saveDatabase();
                player = null;
                return;
            }
            
            disposePlayer(player);
            removalTask(player);
            
            if (!cashshop) {
                final ChannelServer ch = ChannelServer.getInstance(map == null ? channel : map.getChannel());
                final int chz = FindService.findChannel(idz);
                if (chz < -1) {
                    disconnect(RemoveInChannelServer, true);
                    return;
                }
                try {
                    if (messengerID > 0) {
                        MessengerService.leaveMessenger(messengerID, chrm);
                    }                            
                    if (gid > 0) {
                        GuildService.setGuildMemberOnline(chrg, false, -1);
                    }
                    if (bl != null) {
                        if (!serverTransition && isLoggedIn()) {
                            BuddyService.loggedOff(namez, idz, channel, bl.getBuddyIds(), gmLevel, hidden);
                        } else {
                            BuddyService.loggedOn(namez, idz, channel, bl.getBuddyIds(), gmLevel, hidden);
                        }
                    }
                    if (party != null) {
                        chrp.setOnline(false);
                        PartyService.updateParty(party.getId(), MaplePartyOperation.LOG_ONOFF, chrp);
                        if (map != null && party.getLeader().getId() == idz) {
                            MaplePartyCharacter lchr = null;
                            for (MaplePartyCharacter pchr : party.getMembers()) {
                                if (pchr != null && map.getCharacterById(pchr.getId()) != null && (lchr == null || lchr.getLevel() < pchr.getLevel())) {
                                    lchr = pchr;
                                }
                            }
                            if (lchr != null) {
                                PartyService.updateParty(party.getId(), MaplePartyOperation.CHANGE_LEADER, lchr);
                            }
                        }
                    }
                } catch (final Exception e) {
                    FileLogger.printError(FileLogger.ACCOUNT_STUCK, e);
                    System.err.println(getLogMessage(this, "ERROR:") + e);
                } finally {
                    if (RemoveInChannelServer && ch != null) {
                        ch.removePlayer(idz, namez);
                    }
                    player = null;
                }   
            } else {
                final int ch = FindService.findChannel(idz);
                if (ch > 0) {
                    disconnect(RemoveInChannelServer, false);
                    return;
                }
                try {
                    if (party != null) {
                        chrp.setOnline(false);
                        PartyService.updateParty(party.getId(), MaplePartyOperation.LOG_ONOFF, chrp);
                    }
                    if (!serverTransition && isLoggedIn()) {
                        BuddyService.loggedOff(namez, idz, channel, bl.getBuddyIds(), gmLevel, hidden);
                    } else { 
                        BuddyService.loggedOn(namez, idz, channel, bl.getBuddyIds(), gmLevel, hidden);
                    }
                    if (gid > 0) {
                        GuildService.setGuildMemberOnline(chrg, false, -1);
                    }
                    if (player != null) {
                        player.setMessenger(null);
                    }
                } catch (Exception e) {
                    System.err.println(getLogMessage(this, "ERROR:") + e);
                } finally {
                    if (player != null) {
                        player.empty();
                    }
                    if (getChannelServer() != null) {
                        getChannelServer().removePlayer(player);
                    } else {
                        System.out.println("No channelserver associated to char (" + player.getName() + ")");
                    }
                    player = null;  
                }
            }
        }
        if (!serverTransition && isLoggedIn()) {
            updateLoginState(ClientLoginState.LOGIN_NOTLOGGEDIN, getSessionIPAddress());
            session.removeAttribute(Client.CLIENT_KEY);
            session.close();
        }
        engines.clear();
    }
        
    public void disposePlayer(Player p) {
        try {
            player.cancelAllBuffs(true);
            player.cancelAllDebuffs();
            
            player.closePlayerInteractions();
            
            player.setMessenger(null);
            player.expireOnLogout();

            MCField monsterCarnival = player.getMCPQField();
            if (monsterCarnival != null) { 
                monsterCarnival.onPlayerDisconnected(player);
            }  
            
            EventInstanceManager eim = player.getEventInstance();
            if (eim != null) {
                eim.playerDisconnected(player);
            }
            
            NPCScriptManager.getInstance().dispose(this);
            QuestScriptManager.getInstance().dispose(this);
            
            if (player.getMap() != null) {
                player.getMap().removePlayer(player);
            }
            
        } catch (final Throwable e) {
            FileLogger.printError(FileLogger.ACCOUNT_STUCK, e);  
        }
    }
 
    public final void removalTask(Player chr) {
        try {
            if (this.idleTask != null) {
                this.idleTask.cancel(true);
                this.idleTask = null;
            }
        } catch (final Throwable e) {
            FileLogger.printError("Account_RemoveTask.txt", e);
        }
    }

    public final String getSessionIPAddress() {
        return session.getRemoteAddress().toString().split(":")[0];
    }

    public int getGMLevel() {
        return this.gmlevel;
    }       

    public void dropDebugMessage(Player mc) {
        StringBuilder builder = new StringBuilder();
        builder.append("Connected: ");
        builder.append(getSession().isConnected());
        builder.append(" Closing: ");
        builder.append(getSession().isClosing());
        builder.append(" ClientKeySet: ");
        builder.append(getSession().getAttribute(Client.CLIENT_KEY) != null);
        builder.append(" loggedin: ");
        builder.append(isLoggedIn());
        builder.append(" has char: ");
        builder.append(getPlayer() != null);
        mc.dropMessage(builder.toString());
    }

    public int getChannel() {
        return channel;
    }

    public final ChannelServer getChannelServer() {
        return ChannelServer.getInstance(channel);
    }

    public int getChannelByWorld() {
        int chnl = channel;
        switch (world) {
            case 1:
                chnl += 2;
        }
        return chnl;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public Calendar getBirthday() {
        return birthday;
    }  

    public int getWorld() {
        return world;
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public void pongReceived() {
        lastPong = System.currentTimeMillis();
    }

    public void sendPing() {
        final long then = System.currentTimeMillis();
        announce(LoginPackets.PingMessage());
        ClientTimer.getInstance().schedule(() -> {
            try {
                if (lastPong < then) {
                    if (getSession().isConnected()) {
                        getSession().close();
                    }
                }
            } catch (NullPointerException e) {
                System.err.println(e);
            }
        }, 15000);
    }

    public static String getLogMessage(Client cfor, String message) {
        return getLogMessage(cfor, message, new Object[0]);
    }

    public static String getLogMessage(Player cfor, String message) {
        return getLogMessage(cfor == null ? null : cfor.getClient(), message);
    }

    public static String getLogMessage(Player cfor, String message, Object... parms) {
        return getLogMessage(cfor == null ? null : cfor.getClient(), message, parms);
    }

    public static String getLogMessage(Client cfor, String message, Object... parms) {
        StringBuilder builder = new StringBuilder();
        if (cfor != null) {
            if (cfor.getPlayer() != null) {
                builder.append("<");
                builder.append(PlayerStringUtil.makeMapleReadable(cfor.getPlayer().getName()));
                builder.append(" (ID: ");
                builder.append(cfor.getPlayer().getId());
                builder.append(")> ");
            }
            if (cfor.getAccountName() != null) {
                builder.append("(Conta: ");
                builder.append(PlayerStringUtil.makeMapleReadable(cfor.getAccountName()));
                builder.append(") ");
            }
        }
        builder.append(message);
        for (Object parm : parms) {
            int start = builder.indexOf("{}");
            builder.replace(start, start + 2, parm.toString());
        }
        return builder.toString();
    }

    public static int findAccIdForCharacterName(String charName) {
        Connection con = DatabaseConnection.getConnection();
        try {
            int ret;
            try (PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?")) {
                ps.setString(1, charName);
                try (ResultSet rs = ps.executeQuery()) {
                    ret = -1;
                    if (rs.next()) {
                        ret = rs.getInt("accountid");
                    }
                }
            }
            return ret;
        } catch (SQLException e) {
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
        }
      return -1;
    }
    
    public void clearInformation() {
        accountName = null;
        accId = -1;
        gm = false;
        loggedIn = false;
        greason = (byte) 1;
        tempban = null;
        gender = (byte) -1;
    }

    public String getHWID() {
        return hwid;
    }

    public Set<String> getMacs() {
        return Collections.unmodifiableSet(macs);
    }

    public boolean isGm() {
        return gm;
    }

    public void setScriptEngine(String name, ScriptEngine e) {
        engines.put(name, e);
    }

    public ScriptEngine getScriptEngine(String name) {
        return engines.get(name);
    }

    public void removeScriptEngine(String name) {
        engines.remove(name);
    }

    public ScheduledFuture<?> getIdleTask() {
        return idleTask;
    }

    public void setIdleTask(ScheduledFuture<?> idleTask) {
        this.idleTask = idleTask;
    }

    public NPCConversationManager getCM() {
        return NPCScriptManager.getInstance().getCM(this);
    }

    public QuestActionManager getQM() {
        return QuestScriptManager.getInstance().getQM(this);
    }

    public void setTimesTalked(int n, int t) {
        timesTalked.remove(new Pair<>(getPlayer(), n));
        timesTalked.put(new Pair<>(getPlayer(), n), t);
    }

    public int getTimesTalked(int n) {
        if (timesTalked.get(new Pair<>(getPlayer(), n)) == null) {
            setTimesTalked(n, 0);
        }
        return timesTalked.get(new Pair<>(getPlayer(), n));
    }

    public synchronized void announce(final OutPacket packet) {
        session.write(packet);
    }
    
    public void announceHint(String msg, short length) {
        announce(PacketCreator.SendHint(msg, length, (short) 10));
        announce(PacketCreator.EnableActions());
    }

    public boolean isGuest() {
        return guest;
    }

    public void setGuest(boolean set) {
        this.guest = set;
    }
    
    public void lockClient() {
        lock.lock();
    }

    public void unlockClient() {
        lock.unlock();
    }

    public void setGender(byte m) {
        this.gender = m;
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET gender = ? WHERE id = ?")) {
                ps.setByte(1, this.gender);
                ps.setInt(2, this.accId);
                ps.executeUpdate();
            }
        } catch (final SQLException e) {
            System.err.println(e);
        }
    }

    public int deleteCharacter(int cid, int idate) { 
        int year = idate / 10000;
        int month = (idate - year * 10000) / 100;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(year, month - 1, idate - year * 10000 - month * 100);
        if (!checkBirthDate(cal)) 
            return 18;
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT guildrank FROM characters WHERE id = ? AND accountid = ?");
            ps.setInt(1, cid);
            ps.setInt(2, accId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("guildrank") == 1) {
                    return 22;
                } 
            }
            rs.close();
            ps.close();
            ps = DatabaseConnection.getConnection().prepareStatement("SELECT married FROM characters WHERE id = ?");
            ps.setInt(1, cid);
            rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("married") == 1) {
                    return 24;
                } 
            }
            rs.close();
            ps.close();

            DeleteType[] types = {DeleteType.CHARACTER, DeleteType.BUDDY, DeleteType.FAME_LOG, DeleteType.INVENTORY_ITEMS, DeleteType.KEYMAP, DeleteType.QUEST, DeleteType.SKILL_MACRO, DeleteType.SKILL, DeleteType.WISH_LIST};
            for (DeleteType values : types) {
                values.removeFromType(DatabaseConnection.getConnection(), cid);
            }
            return 0;  
            } catch (SQLException e) {
               FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
            }
        return 6;
    }

    public void showMessage(String string) {
        getSession().write(PacketCreator.ServerNotice(1, string));
    }

    public short getCharacterSlots() {
        return characterSlots;
    }
    
    public synchronized boolean gainCharacterSlot() {
        if (characterSlots < 6) { 
            Connection con = null;
            try {
                con = DatabaseConnection.getConnection();
                try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET characterslots = ? WHERE id = ?")) {
                    ps.setInt(1, this.characterSlots += 1);
                    ps.setInt(2, accId);
                    ps.executeUpdate();
                }
            } catch (SQLException sqle) {
                FileLogger.printError(FileLogger.DATABASE_EXCEPTION, sqle);
            }
            return true;
        }
        return false;
    }

    public final byte getGReason() {
        final Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT `greason` FROM `accounts` WHERE id = ?");
            ps.setInt(1, accId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getByte("greason");
            }
        } catch (SQLException e) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                System.out.println("ERROR " + e);
            }
        }
        return 0;
    }

    public byte getGender() {
        return gender;
    }

    public void setAccountID(int id) {
        this.accId = id;
    }

    public int getAccountID() {
        return this.accId;
    }

    public void enableActions() {
        session.write(PacketCreator.EnableActions());
    }

    public EventManager getEventManager(String event) {
        return getChannelServer().getEventSM().getEventManager(event);
    }

    public void setPin(String pin) {
        this.pin = pin;
        Connection con = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET pin = ? WHERE id = ?")) {
                ps.setString(1, pin);
                ps.setInt(2, accId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
        }
    }

    public String getPin() {
        return pin;
    }

    public boolean checkPin(String other) {
        pinattempt++;
        if (pinattempt > 5) {
            getSession().close();
        }
        if (pin.equals(other)) {
            pinattempt = 0;
            return true;
        }
        return false;
    }

    public final void DebugMessage(final StringBuilder sb) {
        sb.append(getSession().getRemoteAddress());
        sb.append("Connected: ");
        sb.append(getSession().isConnected());
        sb.append(" Closing: ");
        sb.append(getSession().isClosing());
        sb.append(" ClientKeySet: ");
        sb.append(getSession().getAttribute(Client.CLIENT_KEY) != null);
        sb.append(" loggedin: ");
        sb.append(isLoggedIn());
        sb.append(" has char: ");
        sb.append(getPlayer() != null);
    }
    
    public void lockEncoder() {
        encoderLock.lock();
    }
        
    public void unlockEncoder() {
        encoderLock.unlock();
    }

    public AbstractPlayerInteraction getAbstractPlayerInteraction() {
        return new AbstractPlayerInteraction(this);
    }

    public boolean canClickNPC(){
        return lastNpcClick + 500 < System.currentTimeMillis();
    }

    public void setClickedNPC(){
        lastNpcClick = System.currentTimeMillis();
    }

    public void removeClickedNPC(){
        lastNpcClick = 0;
    }

    public boolean checkCondition() {
        return checkCondition(true, true, true);
    }
    
    public boolean checkCondition(boolean checkAlive, boolean checkMap, boolean checkInteractions) {
       return player == null || !player.isAlive() && checkAlive || player.getMap() == null && checkMap || player.getInteractionsOpen() && checkInteractions;
    }

    protected static final class CharNameAndId {

        public final String name;
        public final int id;

        public CharNameAndId(final String name, final int id) {
            super();
            this.name = name;
            this.id = id;
        }
    }
}
