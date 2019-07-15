/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login.handler;

import client.player.Player;
import client.player.PlayerStringUtil;
import client.Client;
import client.ClientLoginState;
import constants.ItemConstants;
import handling.channel.ChannelServer;
import handling.login.LoginTools;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import handling.login.LoginServer;
import handling.login.LoginWorker;
import static handling.login.handler.CharLoginHeaders.*;
import handling.mina.PacketReader;
import java.util.HashMap;
import java.util.Map;
import packet.creators.LoginPackets;
import client.player.PlayerSkin;
import client.player.inventory.Inventory;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import server.itens.ItemInformationProvider;
import tools.KoreanDateUtil;
import tools.TimerTools;

public class CharLoginHandler {
    
    public static final void ServerListRequest(final Client c) {
        c.announce(LoginPackets.getServerList(0, LoginServer.getServerName(), LoginServer.getLoad()));
        c.announce(LoginPackets.getEndOfServerList());
    }

    public static final void AfterLogin(final PacketReader packet, final Client c) {
        byte opOne = packet.readByte();
        byte opTwo = 5;
        if (packet.available() > 0) {
            opTwo = packet.readByte();
        }
        if (opOne == 1 && opTwo == 1) {
            if (c.getPin() == null) {
                c.announce(LoginPackets.RequestPinStatus(CharLoginHeaders.PIN_REGISTER));
            } else {
                c.announce(LoginPackets.RequestPinStatus(CharLoginHeaders.PIN_REQUEST));
            }
        } else if (opOne == 1 && opTwo == 0) {
            packet.seek(8);
            String pin = packet.readMapleAsciiString();
            if (c.checkPin(pin)) {
                c.announce(LoginPackets.RequestPinStatus(CharLoginHeaders.PIN_ACCEPTED));
            } else {
                c.announce(LoginPackets.RequestPinStatus(CharLoginHeaders.PIN_REJECTED));
            }
        } else if (opOne == 2 && opTwo == 0) {
            packet.seek(8);
            String pin = packet.readMapleAsciiString();
            if (c.checkPin(pin)) {
                c.announce(LoginPackets.RequestPinStatus(CharLoginHeaders.PIN_REGISTER));
            } else {
                c.announce(LoginPackets.RequestPinStatus(CharLoginHeaders.PIN_REJECTED));
            }
        } else if (opOne == 0 && opTwo == 5) {
            c.updateLoginState(ClientLoginState.LOGIN_NOTLOGGEDIN, c.getSessionIPAddress());
        }
    }

    public static final void SetGender(final PacketReader packet, final Client c) {
        final byte type = packet.readByte(); 
        if ((type == 0x01) && (c.getGender() == 10)) { 
            c.setGender(packet.readByte());
            c.getSession().write(LoginPackets.GetAuthSuccess(c));
            final Client client = c;
            c.setIdleTask(TimerTools.ItemTimer.getInstance().schedule(() -> {
                client.getSession().close();
            }, 600000));
        }
    }

    public static final void RegisterPin(final PacketReader packet, final Client c) {
        byte operation = packet.readByte();
        if (operation == 0) {
            c.updateLoginState(ClientLoginState.LOGIN_NOTLOGGEDIN, c.getSessionIPAddress());
        } else {
            String pin = packet.readMapleAsciiString();
            if (pin != null) {
                c.setPin(pin);
                c.announce(LoginPackets.PinRegistered());
                c.updateLoginState(ClientLoginState.LOGIN_NOTLOGGEDIN, c.getSessionIPAddress());
            }
        }
    }

    public static final void CharlistRequest(final PacketReader packet, final Client c) {
        if (!c.isLoggedIn()) {
            c.getSession().close();
            return;
        }
        final int server = packet.readByte();
        final int channel = packet.readByte() + 1;

        final List<Player> chars = c.loadCharacters(server);
        if (chars != null && ChannelServer.getInstance(channel) != null) {
            c.setWorld(server);
            c.setChannel(channel);
            c.sendCharList(server);
        } else {
            c.getSession().close();
        }
    }

    public static final void CharacterSelect(PacketReader packet, Client c) {
        final int charId = packet.readInt();
        
        String macs = packet.readMapleAsciiString();
        String hwid = packet.readMapleAsciiString();
        
        c.updateMacs(macs);
        c.updateHWID(hwid);

        if (c.hasBannedMac() || c.hasBannedHWID()) {
            c.getSession().close();
            return;
        }
        
        if (c.getIdleTask() != null) {
            c.getIdleTask().cancel(true);
        }

        c.updateLoginState(ClientLoginState.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
        c.getSession().write(LoginPackets.GetServerIP(Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1]), charId));
    }

    public static final void AcceptToS(PacketReader packet, Client c) {
        if (packet.available() == 0 || packet.readByte() != 1 || c.acceptToS()) {
            c.disconnect(false, false);
            return;
        }
        if (c.finishLogin() == 0) {
            c.getSession().write(LoginPackets.GetAuthSuccess(c));
        } else {
            c.announce(LoginPackets.GetLoginStatus(LOGIN_ERROR_));
        }
    }
    
    public static final void Login(PacketReader packet, Client c) {
        final String loginID = packet.readMapleAsciiString();
        final String pwd = packet.readMapleAsciiString();

        c.setAccountName(loginID);

        final boolean ipBan = c.hasBannedIP();
        final boolean macBan = c.hasBannedMac();
        final boolean hwidBan = c.hasBannedHWID();
        
        int loginSucess = c.clientLogin(loginID, pwd);
        
        Calendar tempbannedTill = c.getTempBanCalendar();
        if (loginSucess == CharLoginHeaders.LOGIN_OK && (ipBan || macBan) && !c.isGm()) {
                loginSucess = CharLoginHeaders.LOGIN_BLOCKED;
                if (macBan) {
                    Player.ban(c.getSession().getRemoteAddress().toString().split(":")[0], "Enforcing account ban, account " + loginID, false);
                }
        }
        if (loginSucess == CharLoginHeaders.LOGIN_BLOCKED) {
            c.getSession().write(LoginPackets.GetPermBan((byte) 1));
            return;
        } 
        if (loginSucess != CharLoginHeaders.LOGIN_OK) {
            c.getSession().write(LoginPackets.GetLoginStatus(loginSucess));
        } else if (tempbannedTill.getTimeInMillis() != 0) {
            c.getSession().write(LoginPackets.GetTempBan((long) KoreanDateUtil.getTempBanTimestamp(tempbannedTill.getTimeInMillis()), (byte) c.getBanReason()));
        } else {
            LoginWorker.registerClient(c);
        }
    }

    public static final void CheckCharName(String name, Client c) {
        c.getSession().write(LoginPackets.CharNameResponse(name, !PlayerStringUtil.canCreateChar(name, c.getWorld())));
    }

    public static final void CreateChar(PacketReader packet, Client c) {
        final String name = packet.readMapleAsciiString();
        final int face = packet.readInt();
        final int hair = packet.readInt();
        final int hairColor = packet.readInt();
        final int skinColor = packet.readInt();
        final int top = packet.readInt();
        final int bottom = packet.readInt();
        final int shoes = packet.readInt();
        final int weapon = packet.readInt();
        final int gender = packet.readByte();
        final int str = packet.readByte();
        final int dex = packet.readByte();
        final int _int = packet.readByte();
        final int luk = packet.readByte();

        Player newchar = Player.getDefault(c);
        newchar.setWorld(c.getWorld());
        newchar.setFace(face);
        newchar.setHair(hair + hairColor);
        newchar.setGender(gender);
        newchar.getStat().setStr(str);
        newchar.getStat().setDex(dex);
        newchar.getStat().setInt(_int);
        newchar.getStat().setLuk(luk);
        newchar.getStat().setRemainingAp(9);
        newchar.setName(name, false);
        newchar.setSkinColor(PlayerSkin.getById(skinColor));
        if (c.isGm()) {
            newchar.setGMLevel(c.getGMLevel());
        }

        Inventory equip = newchar.getInventory(InventoryType.EQUIPPED);
        
        Item equipTop = ItemInformationProvider.getInstance().getEquipById(top); 
        equipTop.setPosition((byte) ItemConstants.TOP);
        equip.addFromDB(equipTop);
        
        Item equipBottom = ItemInformationProvider.getInstance().getEquipById(bottom);
        equipBottom.setPosition((byte) ItemConstants.BOTTOM);
        equip.addFromDB(equipBottom);
        
        Item equipShoes = ItemInformationProvider.getInstance().getEquipById(shoes);
        equipShoes.setPosition((byte) ItemConstants.SHOES);
        equip.addFromDB(equipShoes);
        
        Item equipWeapon = ItemInformationProvider.getInstance().getEquipById(weapon);
        equipWeapon.setPosition((byte) ItemConstants.WEAPON);
        equip.addFromDB(equipWeapon);


        newchar.getInventory(InventoryType.ETC).addItem(new Item(4161001, (byte) 0, (short) 1));
        newchar.getInventory(InventoryType.ETC).addItem(new Item(4031180, (byte) 0, (short) 1));

        boolean createChar = true;
        int checking = 0;
        int[] typeName = {face, hair, hairColor, skinColor, top, bottom, shoes, weapon};
        for (int verfifyName : typeName) {
            if (!LoginTools.checkCharEquip(gender, checking, verfifyName)) {
                createChar = false;
            }
            checking++;
        }
        if (PlayerStringUtil.hasSymbols(name) || name.length() < 4 || name.length() > 12) {
            createChar = false;
        }
        if (createChar && PlayerStringUtil.canCreateChar(name, c.getWorld()) && !LoginTools.isForbiddenName(name)) {
            newchar.saveNewCharDB(newchar);
            c.getSession().write(LoginPackets.AddNewCharEntry(newchar, createChar));
        } else {
            System.out.println(Client.getLogMessage(c, "Trying to create a character with a name: " + name));
        }     
    }
    
    public static void DeleteChar(PacketReader packet, Client c) {
        int iDate = packet.readInt();
        int cID = packet.readInt();
        if (!c.isLoggedIn()) {
            return;
        }
        c.announce(LoginPackets.DeleteCharResponse(cID, c.deleteCharacter(cID, iDate)));
    }

    public static void ViewChar(PacketReader packet, Client c) {
        Map<Integer, ArrayList<Player>> worlds = new HashMap<>();
        List<Player> chars = c.loadCharacters(0); 
        c.announce(LoginPackets.ShowAllCharacter(chars.size()));
        chars.stream().filter((chr) -> (chr != null)).forEachOrdered((chr) -> {
            ArrayList<Player> chrr;
            if (!worlds.containsKey(chr.getWorld())) {
                chrr = new ArrayList<>();
                worlds.put(c.getWorld(), chrr);
            } else {
                chrr = worlds.get(chr.getWorld());
            }
            chrr.add(chr);
        });
        worlds.entrySet().forEach((w) -> {
            c.announce(LoginPackets.ShowAllCharacterInfo(w.getKey(), w.getValue()));
        });
    }

    public static void PickCharHandler(PacketReader packet, Client c) {
        int charId = packet.readInt();
        int world = packet.readInt();
        c.setWorld(world);
        try {
            c.setChannel(new Random().nextInt(ChannelServer.getAllInstances().size()));
        } catch (Exception e) {
            c.setChannel(1);
        }
        if (c.getIdleTask() != null) {
            c.getIdleTask().cancel(true);
        }
        c.updateLoginState(ClientLoginState.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
        c.getSession().write(LoginPackets.GetServerIP(Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1]), charId));
    }

    public static final void ServerStatusRequest(final Client c) {
        final int numPlayer = LoginServer.getUsersOn();
        final int userLimit = LoginServer.getUserLimit();
        if (numPlayer >= userLimit) {
            c.getSession().write(LoginPackets.GetServerStatus(2));
        } else if (numPlayer * 2 >= userLimit) {
            c.getSession().write(LoginPackets.GetServerStatus(1));
        } else {
            c.getSession().write(LoginPackets.GetServerStatus(0));
        }
    }
}
