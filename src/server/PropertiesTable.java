/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package server;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import tools.Pair;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class PropertiesTable {
    private LinkedHashMap<String, Object> PropertiesHashmap = new LinkedHashMap<>();
    private ReentrantReadWriteLock propsLock = new ReentrantReadWriteLock();
 
    public PropertiesTable(){}

    public PropertiesTable(Pair<String,Object>[] initialProps) {
        for(Pair<String, Object> p : initialProps) {
            this.setProperty(p.getLeft(), p.getRight());
        }
    }

    public ArrayList<String> getPropertyNames() {
        ArrayList<String> res = new ArrayList<>();
        propsLock.readLock().lock();
        try {
            this.PropertiesHashmap.keySet().forEach((s) -> {
                res.add(s);
            });
        } finally {
            propsLock.readLock().unlock();
        }
        return res;
    }

    public void setProperty(String propertyName, Object value) {
        propsLock.writeLock().lock();
        try {
            if( this.PropertiesHashmap.containsKey(propertyName)) {
                this.PropertiesHashmap.remove(propertyName);
            }
            this.PropertiesHashmap.put(propertyName, value);
        } finally {
            propsLock.writeLock().unlock();
        }
    }

    public Object getProperty(String propertyName) {
        propsLock.readLock().lock();
        try {
            if(this.PropertiesHashmap.containsKey(propertyName)) {
                return this.PropertiesHashmap.get(propertyName);
            }
            return null;
        } finally {
            propsLock.readLock().unlock();
        }
    }
}
