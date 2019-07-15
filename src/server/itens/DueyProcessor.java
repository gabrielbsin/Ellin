/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    Copyleft (L) 2016 - 2018 RonanLana (HeavenMS)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package server.itens;

import client.Client;
import client.player.Player;
import client.player.inventory.Equip;
import client.player.inventory.Item;
import client.player.inventory.types.InventoryType;
import client.player.violation.AutobanManager;
import constants.ItemConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.service.FindService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import packet.creators.PacketCreator;
import tools.FileLogger;

/**
 * @author RonanLana (synchronization of Duey modules)
 */

public class DueyProcessor {
    
    public enum Actions {
        
        TOSERVER_SEND_ITEM(0x02),
        TOSERVER_CLAIM_PACKAGE(0x04),
        TOSERVER_REMOVE_PACKAGE(0x05),
        TOSERVER_CLOSE_DUEY(0x07),
        TOCLIENT_OPEN_DUEY(0x08),
        TOCLIENT_SEND_ENABLE_ACTIONS(0x09),
        TOCLIENT_SEND_NOT_ENOUGH_MESOS(0x0A),
        TOCLIENT_SEND_INCORRECT_REQUEST(0x0B),
        TOCLIENT_SEND_NAME_DOES_NOT_EXIST(0x0C),
        TOCLIENT_SEND_SAMEACC_ERROR(0x0D),
        TOCLIENT_SEND_RECEIVER_STORAGE_FULL(0x0E),
        TOCLIENT_SEND_RECEIVER_UNABLE_TO_RECV(0x0F),
        TOCLIENT_SEND_RECEIVER_STORAGE_WITH_UNIQUE(0x10),
        TOCLIENT_SEND_MESO_LIMIT(0x11),
        TOCLIENT_SEND_SUCCESSFULLY_SENT(0x12),
        TOCLIENT_RECV_UNKNOWN_ERROR(0x13),
        TOCLIENT_RECV_ENABLE_ACTIONS(0x14),
        TOCLIENT_RECV_NO_FREE_SLOTS(0x15),
        TOCLIENT_RECV_RECEIVER_WITH_UNIQUE(0x16),
        TOCLIENT_RECV_SUCCESSFUL_MSG(0x17),
        TOCLIENT_RECV_PACKAGE_MSG(0x1B);
        
        final byte code;

        private Actions(int code) {
            this.code = (byte) code;
        }

        public byte getCode() {
            return code;
        }
    }
    
    private static int getAccIdFromCNAME(String name, boolean accountid) {
        try {
            PreparedStatement ps;
            String text = "SELECT id,accountid FROM characters WHERE name = ?";
            int id_;
            try (Connection con = DatabaseConnection.getConnection()) {
                ps = con.prepareStatement(text);
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return -1;
                    }
                    id_ = accountid ? rs.getInt("accountid") : rs.getInt("id");
                }
                ps.close();
            }
            return id_;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    private static String getCurrentDate() {
        String date = "";
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DATE) - 1; // instant duey ?
        int month = cal.get(Calendar.MONTH) + 1; // its an array of months.
        int year = cal.get(Calendar.YEAR);
        date += day < 9 ? "0" + day + "-" : "" + day + "-";
        date += month < 9 ? "0" + month + "-" : "" + month + "-";
        date += year;
        
        return date;
    }

    private static void removeItemFromDB(int packageid) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            
            PreparedStatement ps = con.prepareStatement("DELETE FROM dueypackages WHERE PackageId = ?");
            ps.setInt(1, packageid);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("DELETE FROM dueyitems WHERE PackageId = ?");
            ps.setInt(1, packageid);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static DueyPackages getItemByPID(ResultSet rs) {
        try {
            DueyPackages dueypack;
            if (rs.getInt("type") == 1) {
                Equip eq = new Equip(rs.getInt("itemid"), (byte) 0, -1);
                eq.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                eq.setLevel((byte) rs.getInt("level"));
                eq.setStr((short) rs.getInt("str"));
                eq.setDex((short) rs.getInt("dex"));
                eq.setInt((short) rs.getInt("int"));
                eq.setLuk((short) rs.getInt("luk"));
                eq.setHp((short) rs.getInt("hp"));
                eq.setMp((short) rs.getInt("mp"));
                eq.setWatk((short) rs.getInt("watk"));
                eq.setMatk((short) rs.getInt("matk"));
                eq.setWdef((short) rs.getInt("wdef"));
                eq.setMdef((short) rs.getInt("mdef"));
                eq.setAcc((short) rs.getInt("acc"));
                eq.setAvoid((short) rs.getInt("avoid"));
                eq.setHands((short) rs.getInt("hands"));
                eq.setSpeed((short) rs.getInt("speed"));
                eq.setJump((short) rs.getInt("jump"));
                eq.setOwner(rs.getString("owner"));
                dueypack = new DueyPackages(rs.getInt("PackageId"), eq);
            } else if (rs.getInt("type") == 2) {
                Item newItem = new Item(rs.getInt("itemid"), (short) 0, (short) rs.getInt("quantity"));
                newItem.setOwner(rs.getString("owner"));
                dueypack = new DueyPackages(rs.getInt("PackageId"), newItem);
            } else {
                dueypack = new DueyPackages(rs.getInt("PackageId"));
            }
            return dueypack;
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        }
    }
    
    private static void showDueyNotification(Client c, Player p) {
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement pss = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT Mesos FROM dueypackages WHERE ReceiverId = ? and Checked = 1");
            ps.setInt(1, p.getId());
            rs = ps.executeQuery();
            if (rs.next()) {
                try {
                    try (Connection con2 = DatabaseConnection.getConnection()) {
                        pss = con2.prepareStatement("UPDATE dueypackages SET Checked = 0 where ReceiverId = ?");
                        pss.setInt(1, p.getId());
                        pss.executeUpdate();
                        pss.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                c.announce(PacketCreator.SendDueyNotification(false));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pss != null) {
                    pss.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private static int getFee(long meso) {
        long fee = 0;
        if (meso >= 100000000) {
            fee = (meso * 6) / 100;
        } else if (meso >= 25000000) {
            fee = (meso * 5) / 100;
        } else if (meso >= 10000000) {
            fee = (meso * 4) / 100;
        } else if (meso >= 5000000) {
            fee = (meso * 3) / 100;
        } else if (meso >= 1000000) {
            fee = (meso * 18) / 1000;
        } else if (meso >= 100000) {
            fee = (meso * 8) / 1000;
        }
        return (int) fee;
    }
    
    private static void addMesoToDB(int mesos, String sName, int recipientID) {
        addItemToDB(null, 1, mesos, sName, recipientID);
    }

    public static void addItemToDB(Item item, int quantity, int mesos, String sName, int recipientID) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO dueypackages (ReceiverId, SenderName, Mesos, TimeStamp, Checked, Type) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, recipientID);
                ps.setString(2, sName);
                ps.setInt(3, mesos);
                ps.setString(4, getCurrentDate());
                ps.setInt(5, 1);
                if (item == null) {
                    ps.setInt(6, 3);
                    ps.executeUpdate();
                } else {
                    ps.setInt(6, item.getType());
                    
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        PreparedStatement ps2;
                        if (item.getInventoryType().equals(InventoryType.EQUIP)) {
                            ps2 = con.prepareStatement("INSERT INTO dueyitems (PackageId, itemid, quantity, upgradeslots, level, str, dex, `int`, luk, hp, mp, watk, matk, wdef, mdef, acc, avoid, hands, speed, jump, owner) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                            Equip eq = (Equip) item;
                            ps2.setInt(2, eq.getItemId());
                            ps2.setInt(3, 1);
                            ps2.setInt(4, eq.getUpgradeSlots());
                            ps2.setInt(5, eq.getLevel());
                            ps2.setInt(6, eq.getStr());
                            ps2.setInt(7, eq.getDex());
                            ps2.setInt(8, eq.getInt());
                            ps2.setInt(9, eq.getLuk());
                            ps2.setInt(10, eq.getHp());
                            ps2.setInt(11, eq.getMp());
                            ps2.setInt(12, eq.getWatk());
                            ps2.setInt(13, eq.getMatk());
                            ps2.setInt(14, eq.getWdef());
                            ps2.setInt(15, eq.getMdef());
                            ps2.setInt(16, eq.getAcc());
                            ps2.setInt(17, eq.getAvoid());
                            ps2.setInt(18, eq.getHands());
                            ps2.setInt(19, eq.getSpeed());
                            ps2.setInt(20, eq.getJump());
                            ps2.setString(21, eq.getOwner());
                        } else {
                            ps2 = con.prepareStatement("INSERT INTO dueyitems (PackageId, itemid, quantity, owner) VALUES (?, ?, ?, ?)");
                            ps2.setInt(2, item.getItemId());
                            ps2.setInt(3, quantity);
                            ps2.setString(4, item.getOwner());
                        }
                        ps2.setInt(1, rs.getInt(1));
                        ps2.executeUpdate();
                        ps2.close();
                    }
                }
            }
            
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static List<DueyPackages> loadItems(Player p) {
        List<DueyPackages> packages = new LinkedList<>();
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages dp LEFT JOIN dueyitems di ON dp.PackageId=di.PackageId WHERE ReceiverId = ?")) {
                ps.setInt(1, p.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        DueyPackages dueypack = getItemByPID(rs);
                        dueypack.setSender(rs.getString("SenderName"));
                        dueypack.setMesos(rs.getInt("Mesos"));
                        dueypack.setSentTime(rs.getString("TimeStamp"));
                        packages.add(dueypack);
                    }
                }
            }
            
            con.close();
            return packages;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void dueySendItem(Client c, byte inventId, short itemPos, short amount, int mesos, String recipient) {
        c.lockClient();
        try {
            final int fee = 5000;
            final long sendMesos = (long) mesos + fee;
            if (mesos < 0 || sendMesos > Integer.MAX_VALUE || (amount < 1 && mesos == 0)) {
                AutobanManager.getInstance().autoban(c, c.getPlayer().getName() + " tried to packet edit with duey.");
                FileLogger.printError(FileLogger.EXPLOITS + c.getPlayer().getName() + ".txt", c.getPlayer().getName() + " tried to use duey with mesos " + mesos + " and amount " + amount + "\r\n");           	
                return;
            }
            int finalcost = mesos + fee;
            boolean send = false;
            if (c.getPlayer().getMeso() >= finalcost) {
                int accid = getAccIdFromCNAME(recipient, true);
                if (accid != -1) {
                    if (accid != c.getAccountID()) {
                        send = true;
                    } else {
                        c.announce(PacketCreator.SendDueyMSG(DueyProcessor.Actions.TOCLIENT_SEND_SAMEACC_ERROR.getCode()));
                    }
                } else {
                    c.announce(PacketCreator.SendDueyMSG(DueyProcessor.Actions.TOCLIENT_SEND_NAME_DOES_NOT_EXIST.getCode()));
                }
            } else {
                c.announce(PacketCreator.SendDueyMSG(DueyProcessor.Actions.TOCLIENT_SEND_NOT_ENOUGH_MESOS.getCode()));
            }

            Client rClient = null;
            
            int channel = FindService.findChannel(recipient);
            if (channel > -1) {
                ChannelServer rcserv = c.getChannelServer();
                rClient = rcserv.getPlayerStorage().getCharacterByName(recipient).getClient();
            }
            
            if (send) {
                if (inventId > 0) {
                    InventoryType inv = InventoryType.getByType(inventId);
                    Item item = c.getPlayer().getInventory(inv).getItem(itemPos);
                    if (item != null && c.getPlayer().getItemQuantity(item.getItemId(), false) >= amount) {
                        c.getPlayer().gainMeso(-finalcost, false);
                        c.announce(PacketCreator.SendDueyMSG(DueyProcessor.Actions.TOCLIENT_SEND_SUCCESSFULLY_SENT.getCode()));

                        if (ItemConstants.isRechargeable(item.getItemId())) {
                            InventoryManipulator.removeFromSlot(c, inv, itemPos, item.getQuantity(), true);
                        } else {
                            InventoryManipulator.removeFromSlot(c, inv, itemPos, amount, true, false);
                        }

                       // MapleKarmaManipulator.toggleKarmaFlagToUntradeable(item);
                        addItemToDB(item, amount, mesos - getFee(mesos), c.getPlayer().getName(), getAccIdFromCNAME(recipient, false));
                    } else {
                        if (item != null) {
                            c.announce(PacketCreator.SendDueyMSG(DueyProcessor.Actions.TOCLIENT_SEND_INCORRECT_REQUEST.getCode()));
                        }
                        return;
                    }
                } else {
                    c.getPlayer().gainMeso(-finalcost, false);
                    c.announce(PacketCreator.SendDueyMSG(DueyProcessor.Actions.TOCLIENT_SEND_SUCCESSFULLY_SENT.getCode()));    

                    addMesoToDB(mesos - getFee(mesos), c.getPlayer().getName(), getAccIdFromCNAME(recipient, false));
                }

                if (rClient != null && rClient.isLoggedIn()) {
                    showDueyNotification(rClient, rClient.getPlayer());
                }
            }
        } finally {
            c.unlockClient();
        }
    }
    
    public static void dueyRemovePackage(Client c, int packageid) {
        c.lockClient();
        try {
            removeItemFromDB(packageid);
            c.announce(PacketCreator.RemoveItemFromDuey(true, packageid));
        } finally {
            c.unlockClient();
        }
    }
    
    public static void dueyClaimPackage(Client c, int packageid) {
        c.lockClient();
        try {
            List<DueyPackages> packages = new LinkedList<>();
            DueyPackages dp = null;
            Connection con = null;
            try {
                con = DatabaseConnection.getConnection();
                DueyPackages dueypack;
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages LEFT JOIN dueyitems USING (PackageId) WHERE PackageId = ?")) {
                    ps.setInt(1, packageid);
                    try (ResultSet rs = ps.executeQuery()) {
                        dueypack = null;
                        if (rs.next()) {
                            dueypack = getItemByPID(rs);
                            dueypack.setSender(rs.getString("SenderName"));
                            dueypack.setMesos(rs.getInt("Mesos"));
                            dueypack.setSentTime(rs.getString("TimeStamp"));

                            packages.add(dueypack);
                        }
                    }
                }
                dp = dueypack;
                if(dp == null) {
                    c.announce(PacketCreator.SendDueyMSG(Actions.TOCLIENT_RECV_UNKNOWN_ERROR.getCode()));
                    FileLogger.printError(FileLogger.EXPLOITS + c.getPlayer().getName() + ".txt", c.getPlayer().getName() + " tried to receive package from duey with id " + packageid + "\r\n");
                    return;
                }

                if (dp.getItem() != null) {
                    if (!InventoryManipulator.checkSpace(c, dp.getItem().getItemId(), dp.getItem().getQuantity(), dp.getItem().getOwner())) {
                        int itemid = dp.getItem().getItemId();
                        if(ItemInformationProvider.getInstance().isPickupRestricted(itemid) && c.getPlayer().getInventory(ItemConstants.getInventoryType(itemid)).findById(itemid) != null) {
                            c.announce(PacketCreator.SendDueyMSG(Actions.TOCLIENT_RECV_RECEIVER_WITH_UNIQUE.getCode()));
                        } else {
                            c.announce(PacketCreator.SendDueyMSG(Actions.TOCLIENT_RECV_NO_FREE_SLOTS.getCode()));
                        }

                        return;
                    } else {
                        InventoryManipulator.addFromDrop(c, dp.getItem(), "", false);
                    }
                }

                long gainmesos;
                long totalmesos = (long) dp.getMesos() + c.getPlayer().getMeso();

                if (totalmesos < 0 || dp.getMesos() < 0) {
                    gainmesos = 0;
                } else {
                    totalmesos = Math.min(totalmesos, Integer.MAX_VALUE);
                    gainmesos = totalmesos - c.getPlayer().getMeso();
                }
                
                c.getPlayer().gainMeso((int)gainmesos, false);

                removeItemFromDB(packageid);
                c.announce(PacketCreator.RemoveItemFromDuey(false, packageid));

                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } finally {
            c.unlockClient();
        }
    }
    
    public static void dueySendTalk(Client c) {
        c.lockClient();
        try {
            c.announce(PacketCreator.SendDuey((byte) 8, loadItems(c.getPlayer())));
        } finally {
            c.unlockClient();
        }
    }
}
