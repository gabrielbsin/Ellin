/**
 * Ellin é um servidor privado de MapleStory
 * Baseado em um servidor GMS-Like na v.62
 */

package tools;

import client.player.inventory.Equip;
import client.player.inventory.Item;
import java.util.ArrayList;
import server.itens.ItemInformationProvider;

/**
 * @brief SortInventoryTool
 * @author GabrielSin <gabrielsin@playellin.net>
 * @date   21/06/2018
 */
public class PairedQuicksort {
    private int i = 0;
    private int j = 0;
    private final ArrayList<Integer> intersect;
    ItemInformationProvider ii = ItemInformationProvider.getInstance();
    
    private void PartitionByItemId(int Esq, int Dir, ArrayList<Item> A) {
        Item x, w;

        i = Esq;
        j = Dir;
        
        x = A.get((i + j) / 2);
        do {
            while (x.getItemId() > A.get(i).getItemId()) i++;
            while (x.getItemId() < A.get(j).getItemId()) j--;
            
            if (i <= j) {
                w = A.get(i);
                A.set(i, A.get(j));
                A.set(j, w);

                i++;
                j--;
            }
        } while (i <= j);
    }
    
    private void PartitionByName(int Esq, int Dir, ArrayList<Item> A) {
        Item x, w;

        i = Esq;
        j = Dir;
        
        x = A.get((i + j) / 2);
        do {
            while (ii.getName(x.getItemId()).compareTo(ii.getName(A.get(i).getItemId())) > 0) i++;
            while (ii.getName(x.getItemId()).compareTo(ii.getName(A.get(j).getItemId())) < 0) j--;
            
            if (i <= j) {
                w = A.get(i);
                A.set(i, A.get(j));
                A.set(j, w);

                i++;
                j--;
            }
        } while (i <= j);
    }
    
    private void PartitionByQuantity(int Esq, int Dir, ArrayList<Item> A) {
        Item x, w;

        i = Esq;
        j = Dir;
        
        x = A.get((i + j) / 2);
        do {
            while (x.getQuantity() > A.get(i).getQuantity()) i++;
            while (x.getQuantity() < A.get(j).getQuantity()) j--;
            
            if (i <= j) {
                w = A.get(i);
                A.set(i, A.get(j));
                A.set(j, w);

                i++;
                j--;
            }
        } while (i <= j);
    }
    
    private void PartitionByLevel(int Esq, int Dir, ArrayList<Item> A) {
        Equip x, w, eqpI, eqpJ;

        i = Esq;
        j = Dir;
        
        x = (Equip)(A.get((i + j) / 2));
        
        do {
            eqpI = (Equip)A.get(i);
            eqpJ = (Equip)A.get(j);
            
            while (x.getLevel() > eqpI.getLevel()) i++;
            while (x.getLevel() < eqpJ.getLevel()) j--;
            
            if (i <= j) {
                w = (Equip)A.get(i);
                A.set(i, A.get(j));
                A.set(j, (Item)w);

                i++;
                j--;
            }
        } while (i <= j);
    }

    void MapleQuicksort(int Esq, int Dir, ArrayList<Item> A, int sort) {
        switch(sort) {
            case 3:
                PartitionByLevel(Esq, Dir, A);
                break;
            
            case 2:
                PartitionByName(Esq, Dir, A);
                break;
                
            case 1:
                PartitionByQuantity(Esq, Dir, A);
                break;
                    
            default:
                PartitionByItemId(Esq, Dir, A);
        }
        
        
        if (Esq < j) MapleQuicksort(Esq, j, A, sort);
        if (i < Dir) MapleQuicksort(i, Dir, A, sort);
    }
    
    public PairedQuicksort(ArrayList<Item> A, int primarySort, int secondarySort) {
        intersect = new ArrayList<>();
        
        if(A.size() > 0) MapleQuicksort(0, A.size() - 1, A, primarySort);
        
        intersect.add(0);
        for(int ind = 1; ind < A.size(); ind++) {
            if(A.get(ind - 1).getItemId() != A.get(ind).getItemId()) {
                intersect.add(ind);
            }
        }
        intersect.add(A.size());
        
        for (int ind = 0; ind < intersect.size() - 1; ind++) {
            if(intersect.get(ind + 1) > intersect.get(ind)) MapleQuicksort(intersect.get(ind), intersect.get(ind + 1) - 1, A, secondarySort);
        }
    }
}
