/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cashshop;

import client.player.inventory.Item;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;

/**
 * 
 * @author GabrielSin
 */
public class CashItemFactory {
    private static Map<Integer, CashItem> items = new HashMap<>();
    private static Map<Integer, List<Integer>> packages = new HashMap<>();

    static {
        MapleDataProvider etc = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "Etc"));

        etc.getData("Commodity.img").getChildren().forEach((item) -> {
            int sn = MapleDataTool.getIntConvert("SN", item);
            int itemId = MapleDataTool.getIntConvert("ItemId", item);
            int price = MapleDataTool.getIntConvert("Price", item, 0);
            int period = MapleDataTool.getIntConvert("Period", item, 1);
            int gender = MapleDataTool.getIntConvert("Gender", item, 2);
            short count = (short) MapleDataTool.getIntConvert("Count", item, 1);
            boolean onSale = MapleDataTool.getIntConvert("OnSale", item, 0) == 1;
            items.put(sn, new CashItem(sn, itemId, price, count, onSale, period, gender));
        });

        etc.getData("CashPackage.img").getChildren().forEach((cashPackage) -> {
            List<Integer> cPackage = new ArrayList<>();

            cashPackage.getChildByPath("SN").getChildren().forEach((item) -> {
                cPackage.add(Integer.parseInt(item.getData().toString()));
            });

            packages.put(Integer.parseInt(cashPackage.getName()), cPackage);
        });
    }

    public static CashItem getItem(int sn) {
        return items.get(sn);
    }

    public static List<Item> getPackage(int itemId) {
        List<Item> cashPackage = new ArrayList<>();

        packages.get(itemId).forEach((sn) -> {
            cashPackage.add(getItem(sn).toItem(CashItemFactory.getItem(sn), CashItemFactory.getItem(sn).getCount()));
        });

        return cashPackage;
    }

    public static boolean isPackage(int itemId) {
        return packages.containsKey(itemId);
    }
}