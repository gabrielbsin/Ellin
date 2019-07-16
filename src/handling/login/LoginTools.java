package handling.login;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;

/**
 * @author GabrielSin (http://forum.ragezone.com/members/822844.html)
 */
public class LoginTools {
    
    private static List<String> forbiddenNames;
    private static List<NewEquip> newEquip;
    
    public static void setUp() {
        forbiddenNames = new ArrayList<>();
        newEquip = new ArrayList<>();
        
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Etc"));
        dataProvider.getData("ForbiddenName.img").getChildren().forEach((forbiddenNameData) -> {
            forbiddenNames.add(MapleDataTool.getString(forbiddenNameData));
        });

        MapleData newEquipData = dataProvider.getData("MakeCharInfo.img");

        for (int i = 0; i < 2; i++) {
            for (MapleData data : newEquipData.getChildByPath("Info").getChildByPath(i == 1 ? "CharFemale" : "CharMale").getChildren()) {
                for (MapleData eq : data.getChildren()) {
                    NewEquip newEq = new NewEquip();
                    newEq.gender = i;
                    newEq.itemId = MapleDataTool.getInt(eq);
                    newEq.type = Integer.valueOf(data.getName());
                    newEquip.add(newEq);
                }
            }
        }
    }
    
    public static final boolean checkCharEquip(int gender, int type, int itemId) {
        return newEquip.stream().anyMatch((newEq) -> (newEq.gender == gender && newEq.type == type && newEq.itemId == itemId));
    }
    
    public static final boolean isForbiddenName(final String in) {
        return forbiddenNames.stream().anyMatch((name) -> (in.contains(name)));
    }
    
    static class NewEquip {

        int gender;
        int type;
        int itemId;
    }
}
