package cashshop;

import static cashshop.CashShopTools.getMaxInventorySlots;
import client.Client;
import handling.mina.PacketReader;
import java.util.List;
import packet.creators.CashShopPackets;
import packet.creators.PacketCreator;
import client.player.Player;
import client.player.PlayerNote;
import client.player.PlayerQuery;
import client.player.inventory.Inventory;
import client.player.inventory.InventoryIdentifier;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemRing;
import client.player.violation.CheatingOffense;
import constants.GameConstants;
import server.itens.InventoryManipulator;
import server.itens.ItemInformationProvider;

/**
 * @author GabrielSin (http://forum.ragezone.com/members/822844.html)
 */

public class CashShopOperation {
    
    public static void CashShopAction(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        switch (packet.readByte()) {
            case 3: 
                BuyCashItem(p, packet);
                break;
            case 4:
                GiftItem(p, packet);
                break;
            case 5:
                UpdateWishList(p, packet);
                break;
            case 6:
                BuyInventorySlots(p, packet);
                break;
            case 7:
                CashInventorySlots(p, packet);
                break;
            case 8:
                BuyCharacterSlots(p, packet);
                break;
            case 12:
                TakeFromCashInventory(p, packet);
                break;
            case 13:
                PutIntoCashInventory(p, packet);
                break;
            case 28:
                BuyPackage(p, packet);
                break;
            case 29:
                GiftPackage(p, packet);
                break;
            case 30:
                BuyQuestItem(p, packet);
                break;
            case 27:    
                BuyFriendshipAndCoupleRing(p, packet, true);
                break;
            case 33:
                BuyFriendshipAndCoupleRing(p, packet, false);
                break;
            default:
                break;
	 }
    }
    
    private static void updateInformation(Client c, Item item, boolean showBuy) {
        if (showBuy) {
            c.getSession().write(CashShopPackets.ShowBoughtCashItem(item, c.getAccountID()));
        }
        c.getSession().write(CashShopPackets.ShowCash(c.getPlayer()));
        c.getSession().write(PacketCreator.EnableActions());
    }

    private static void BuyCashItem(Player p, PacketReader packet) {
        packet.readByte();
        int currencyType = packet.readInt();
        int serialNumber = packet.readInt();
        CashItem cashItem = CashItemFactory.getItem(serialNumber);   
        if (cashItem == null || !cashItem.isOnSale() ||p.getCashShop().getCash(currencyType) < cashItem.getPrice()) {
            p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_INSUFFICIENT_CASH));    
            return;
        } 
        if (p.getCashShop().isFull())  {
            p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_INVENTORY_FULL));
            return;
        }
        if (!cashItem.genderEquals(p.getGender())) {
            p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_GENDER_RESTRICTIONS));
            return;
        }
        Item item = cashItem.toItem(cashItem);
        p.getCashShop().addToInventory(item);
        p.getCashShop().gainCash(currencyType, -cashItem.getPrice());
        updateInformation(p.getClient(), item, true);
    }
   
    private static void GiftItem(Player p, PacketReader packet) {
        int birthday = packet.readInt();
        int serialNumber = packet.readInt();
        String recipient = packet.readMapleAsciiString();
        String message = packet.readMapleAsciiString();

        if (!CashShopTools.checkBirthday(p.getClient(), birthday)) {
            p.getClient().getSession().write(CashShopPackets.GiftError(CashShopPackets.ERROR_BIRTHDAY));
            return;
        }
        CashItem item = CashItemFactory.getItem(serialNumber);   
        if (item == null || !item.isOnSale()) {
            p.getClient().getSession().write(CashShopPackets.GiftError(CashShopPackets.ERROR_OUT_OF_STOCK));
            return;
        }
        if (p.getCashShop().getCash(1) < item.getPrice() || message.length() > 78 || message.length() < 1 || item.getPrice() <= 0) {
           p.getClient().getSession().write(CashShopPackets.GiftError(CashShopPackets.ERROR_INSUFFICIENT_CASH));
           return; 
        }
       
        int recipientAcct = PlayerQuery.getIdByName(recipient);
        if (recipientAcct == -1) {
            p.getClient().getSession().write(CashShopPackets.GiftError(CashShopPackets.ERROR_RECIPIENT_NAME));
            return;
        }
        if (!item.genderEquals(PlayerQuery.getGenderById(recipientAcct))) {
            p.getClient().getSession().write(CashShopPackets.GiftError(CashShopPackets.ERROR_GIFT_ITEM_RECEIVER_GENDER));
            return; 
        }
        p.getCashShop().gift(recipientAcct, p.getName(), message, item.getSN());
        p.getCashShop().gainCash(4, -item.getPrice());
        p.getClient().getSession().write(CashShopPackets.GiftSent(recipient, item.getItemId(), item.getPrice(), item.getCount(), false));
        updateInformation(p.getClient(), null, false);
    } 
    
    private static void BuyInventorySlots(Player p, PacketReader packet) {
        packet.readByte();
        int currencyType = packet.readInt();
	    boolean hasSerial = packet.readBool();
        InventoryType invType;
        int cost;
        if (hasSerial) {
            int sn = packet.readInt();
            System.out.printf("Item não disponivel: %s", sn);
            return;
        }  else {
            invType = InventoryType.getByType(packet.readByte());
            cost = 4000;
        }
        
        Inventory inv = p.getInventory(invType);
        byte currentSlots = inv.getSlotLimit();
        
        if (currentSlots + 4 > getMaxInventorySlots(p.getJob().getId(), invType)) {
            p.getClient().getSession().write(CashShopPackets.BuyInventorySlotsError(CashShopPackets.ERROR_TOO_MANY_CASH_ITEMS));
	    return;
        }
        
        if (p.getCashShop().getCash(currencyType) < cost) {
            p.getClient().getSession().write(CashShopPackets.BuyInventorySlotsError(CashShopPackets.ERROR_INSUFFICIENT_CASH));
            return;
        }
        
        if (p.gainSlots(invType.getType(), 4, false)) {
            p.getClient().getSession().write(CashShopPackets.UpdateInventorySlots(invType.getType(), p.getSlots(invType.getType())));
            updateInformation(p.getClient(), null, false);
        }
    }
    
    private static void CashInventorySlots(Player p, PacketReader packet) {
        packet.readByte();
	int currencyType = packet.readInt();
        Inventory inv = p.getInventory(InventoryType.CASH);       
        byte currentSlots = inv.getSlotLimit();
        if (currentSlots + 4 > CashShopTools.getMaxInventorySlots(p.getJob().getId(), InventoryType.CASH)) {
            p.getClient().getSession().write(CashShopPackets.BuyStorageSlotsError(CashShopPackets.ERROR_TOO_MANY_CASH_ITEMS));
	    return;
        }        
        if (p.getCashShop().getCash(currencyType) < 4000) {
            p.getClient().getSession().write(CashShopPackets.BuyStorageSlotsError(CashShopPackets.ERROR_INSUFFICIENT_CASH));
	    return;
        }
        p.getStorage().increaseSlots((byte) 4);
        p.getCashShop().gainCash(currencyType, -4000);
        p.getClient().getSession().write(CashShopPackets.UpdateStorageSlots((short) p.getStorage().getSlots()));    
        updateInformation(p.getClient(), null, false);
    }
    
    private static void BuyCharacterSlots(Player p, PacketReader packet) {
        packet.readByte();
        int currencyType = packet.readInt();
        int currentSlots = p.getClient().getCharacterSlots();
        if (currentSlots + 1 > 6) {
            p.getClient().getSession().write(CashShopPackets.BuyCharacterSlotsError(CashShopPackets.ERROR_TOO_MANY_CASH_ITEMS));
	    return;
        }        
        if (p.getCashShop().getCash(currencyType) < 6900) {
            p.getClient().getSession().write(CashShopPackets.BuyCharacterSlotsError(CashShopPackets.ERROR_INSUFFICIENT_CASH));
	    return;
        }    
        if (p.getClient().gainCharacterSlot()) {
            p.getCashShop().gainCash(currencyType, -6900);
            p.getClient().getSession().write(CashShopPackets.UpdateCharacterSlots(p.getClient().getCharacterSlots()));
            updateInformation(p.getClient(), null, false);
        } else {
           p.getClient().getSession().write(CashShopPackets.BuyCharacterSlotsError(CashShopPackets.ERROR_TOO_MANY_CASH_ITEMS));
        }
    }
    
    private static void TakeFromCashInventory(Player p, PacketReader packet) {
        long uniqueId = packet.readLong();
        packet.readByte();
        packet.readByte();
        packet.readByte();
        final Item cashItem = p.getCashShop().findByUniqueId((int) uniqueId);
        
        if (cashItem == null) {
            CheatingOffense.PACKET_EDIT.cheatingSuspicious(p, "Tried to transfer non-existent cashshop item from staging");
            p.getClient().getSession().write(CashShopPackets.TakeError(CashShopPackets.ERROR_UNKNOWN));
            return;
        }
        if (cashItem.getQuantity() > 0 && !InventoryManipulator.checkSpace(p.getClient(), cashItem.getItemId(), cashItem.getQuantity(), cashItem.getOwner())) {
            p.getClient().getSession().write(CashShopPackets.TakeError(CashShopPackets.ERROR_UNKNOWN));
            return;
        }
        
        final Item item = cashItem.copy();
        short position = InventoryManipulator.addbyItem(p.getClient(), item, true);
        if (position < 0) {
            p.getClient().getSession().write(CashShopPackets.TakeError(CashShopPackets.ERROR_INVENTORY_FULL));
            return;
        }
        if (item.getPet() != null) {
            item.getPet().setInventoryPosition(position);
            p.addPet(item.getPet());
        }
        p.getCashShop().removeFromInventory(cashItem);
        p.getClient().getSession().write(CashShopPackets.TakeFromCashInventory(cashItem, position));
    }
     
    private static void PutIntoCashInventory(Player p, PacketReader packet) {
        long uniqueId = packet.readLong();
        byte type = packet.readByte();
        Inventory mi = p.getInventory(InventoryType.getByType(type));
        Item item = mi.findByUniqueId((int) uniqueId);
        if (item == null) {
            p.getClient().getSession().write(CashShopPackets.PlaceError(CashShopPackets.ERROR_UNKNOWN));
            return;
        }
        if (p.getCashShop().getItemsSize() >= 100) {
            p.getClient().getSession().write(CashShopPackets.TakeError(CashShopPackets.ERROR_INVENTORY_FULL));
            return;
        }
        if (item.getUniqueId() < 0 ) {
            p.getClient().getSession().write(CashShopPackets.TakeError(CashShopPackets.ERROR_UNKNOWN));
            return;
        }
        if (item.getPet() != null) {
            p.removePetCS(item.getPet());
        }
        p.getCashShop().addToInventory(item);
        mi.removeSlot(item.getPosition());
        p.getClient().announce(CashShopPackets.PutIntoCashInventory(item, p.getClient().getAccountID()));
        p.getClient().announce(CashShopPackets.ShowCashInventory(p.getClient()));
    }
    
    private static void UpdateWishList(Player p, PacketReader packet) {
        p.getCashShop().clearWishList();
        CashItem item;
        for (int i = 0; i < 10; i++) {
            int serialNumber = packet.readInt();
            item = CashItemFactory.getItem(serialNumber);
            if (serialNumber != 0) {
                if (item == null) {
                    p.getClient().getSession().write(CashShopPackets.WishListError(CashShopPackets.ERROR_OUT_OF_STOCK));
                    return;
                }
                p.getCashShop().addToWishList(serialNumber);
            }
        }
        p.getClient().getSession().write(CashShopPackets.SendWishList(p, true));
        updateInformation(p.getClient(), null, false);
    }
    
    private static void BuyQuestItem(Player p, PacketReader packet) {
        int serialNumber = packet.readInt();
        CashItem item = CashItemFactory.getItem(serialNumber);
        if (serialNumber / 10000000 != 8) {
            p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_OUT_OF_STOCK));
            return;
        }
        if (item == null || !item.isOnSale()) {
            p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_OUT_OF_STOCK));
            return;
        }
        if (p.getMeso() >= item.getPrice()) {
            if (ItemInformationProvider.getInstance().isQuestItem(item.getItemId())) {
                p.gainMeso(-item.getPrice(), false);
                InventoryManipulator.addById(p.getClient(), item.getItemId(), (short) item.getCount(), "Obtained through CashShop!");
            } else {
                p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_UNKNOWN));
            }
        } else {
            p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_INSUFFICIENT_MESOS));
        }
    }
    
    private static void BuyPackage(Player p, PacketReader packet) {
        packet.readByte();
        int currencyType = packet.readInt();
        int serialNumber = packet.readInt();
        CashItem cashItem = CashItemFactory.getItem(serialNumber);
        if (cashItem == null || !cashItem.isOnSale()) {
            p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_OUT_OF_STOCK));
            return;
        }

        List<Item> serialNumbers = CashItemFactory.getPackage(cashItem.getItemId());
        if (serialNumbers == null) {
            p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_OUT_OF_STOCK));
            return;
        }

        if (!p.getCashShop().canFit(serialNumbers.size())) {
            p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_INVENTORY_FULL));
            return;
        }

        if (p.getCashShop().getCash(currencyType) < cashItem.getPrice()) {
            p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_INSUFFICIENT_CASH));
            return;
        }
        Item item = cashItem.toItem(cashItem);
        List<Item> cashPackage = CashItemFactory.getPackage(cashItem.getItemId());
        for (Item c: cashPackage) {
            if (c == null) {
                continue;
            }
            p.getCashShop().addToInventory(c);
        }
        p.getCashShop().gainCash(currencyType, -cashItem.getPrice());
        updateInformation(p.getClient(), item, true);
        p.getClient().getSession().write(CashShopPackets.ShowCashInventory(p.getClient()));
    }
    
    private static void GiftPackage(Player p, PacketReader packet) {
        int enteredBirthday = packet.readInt();
        int serialNumber = packet.readInt();
        String recipient = packet.readMapleAsciiString();
        String message = packet.readMapleAsciiString();
        
        CashItem cashItem = CashItemFactory.getItem(serialNumber);
        if (!CashShopTools.checkBirthday(p.getClient(), enteredBirthday)) {
            p.getClient().getSession().write(CashShopPackets.GiftError(CashShopPackets.ERROR_BIRTHDAY));
            return;
        } 
        
        if (cashItem == null || !cashItem.isOnSale()) {
            CheatingOffense.PACKET_EDIT.cheatingSuspicious(p, "Tried to buy nonexistent package from cash shop");
            p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_OUT_OF_STOCK));
            return;
        }

        List<Item> serialNumbers = CashItemFactory.getPackage(cashItem.getItemId());
        if (serialNumbers == null) {
            p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_OUT_OF_STOCK));
            return;
        }

        if (!p.getCashShop().canFit(serialNumbers.size())) {
            p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_INVENTORY_FULL));
            return;
        }

        if (p.getCashShop().getCash(4) < cashItem.getPrice()) {
            CheatingOffense.PACKET_EDIT.cheatingSuspicious(p, "Tried to gift item from cash shop with nonexistent cash");
            p.getClient().getSession().write(CashShopPackets.BuyError(CashShopPackets.ERROR_INSUFFICIENT_CASH));
            return;
        }
        
        int recipientAcct = PlayerQuery.getIdByName(recipient);
        if (recipientAcct == -1) {
            p.getClient().getSession().write(CashShopPackets.GiftError(CashShopPackets.ERROR_RECIPIENT_NAME));
            return;
        }
        
        p.getCashShop().gift(recipientAcct, p.getName(), message, cashItem.getSN(), InventoryIdentifier.getInstance());
        p.getCashShop().gainCash(4, -cashItem.getPrice());
        p.getClient().getSession().write(CashShopPackets.GiftSent(recipient, cashItem.getItemId(), cashItem.getPrice(), cashItem.getCount(), false));
        updateInformation(p.getClient(), null, false);
    }
    
    private static void BuyFriendshipAndCoupleRing(Player p, PacketReader r, boolean couple) {
        int enteredBirthday = r.readInt();
        int currencyType = r.readInt();
        int serialNumber = r.readInt();
        String recipient = r.readMapleAsciiString();
        String message = r.readMapleAsciiString();
        CashItem ring = CashItemFactory.getItem(serialNumber);
        if (!CashShopTools.checkBirthday(p.getClient(), enteredBirthday)) {
            p.getClient().getSession().write(CashShopPackets.GiftError(CashShopPackets.ERROR_BIRTHDAY));
            return;
        } 
        if (ring == null || !ring.isOnSale()) {
            p.getClient().getSession().write(CashShopPackets.GiftError(CashShopPackets.ERROR_OUT_OF_STOCK));
            return;
        }
        if (p.getCashShop().isFull()) {
            p.getClient().getSession().write(CashShopPackets.GiftError(CashShopPackets.ERROR_INVENTORY_FULL));
            return;
        }
        if (p.getCashShop().getCash(currencyType) < ring.getPrice()) {
            CheatingOffense.PACKET_EDIT.cheatingSuspicious(p, "Tried to buy friendship ring from cash shop with nonexistent cash");
            p.getClient().getSession().write(CashShopPackets.GiftError(CashShopPackets.ERROR_INSUFFICIENT_CASH));
            return;
        }
        int recipientAcct = PlayerQuery.getIdByName(recipient);
        if (recipientAcct == -1) {
            p.getClient().getSession().write(CashShopPackets.GiftError(CashShopPackets.ERROR_RECIPIENT_NAME));
            return;
        }
        int gender = PlayerQuery.getGenderByName(recipient);
        if (p.getGender() == gender && GameConstants.GENDER_RESTRICT_RINGS && couple) {
            p.getClient().getSession().write(CashShopPackets.GiftError(CashShopPackets.ERROR_GENDER_RESTRICTIONS));
            return;
        }
        
        boolean creationRing = ItemRing.createRing(ring.getItemId(), p, recipientAcct, message, ring.getSN());
        if (!creationRing) {
            p.dropMessage(1, "You already have a ring with this person."); // TODO: GMS-LIKE ?
            updateInformation(p.getClient(), null, false);
            return;
        }
        p.getCashShop().gainCash(currencyType, -ring.getPrice());
        PlayerNote.sendNote(p, PlayerQuery.getNameById(recipientAcct), message, 0);
        Player partnerChar = p.getClient().getChannelServer().getPlayerStorage().getCharacterByName(recipient);
        if (partnerChar != null) {
            PlayerNote.showNote(partnerChar);
        }
        updateInformation(p.getClient(), null, false);
    }
}
