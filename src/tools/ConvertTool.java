/**
 * Ellin é um servidor privado de MapleStory
 * Baseado em um servidor GMS-Like na v.62
 */

package tools;

import community.MaplePartyCharacter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.script.Bindings;

/**
 * @brief ConvertArray
 * @author GabrielSin <gabrielsin@playellin.net>
 * @date   02/06/2018
 * This class helps in Java 8 support
 * Thanks Stackoverflow (https://stackoverflow.com/questions/22492641/)
 */
public class ConvertTool {
    
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConvertTool.class);
    
    public static List<MaplePartyCharacter> ConvertFromScriptArray(final Object obj){
        List<MaplePartyCharacter> returnList = new ArrayList<>();

        if (obj == null){
            returnList.add(null);
            return returnList;
        }

        if (obj instanceof Bindings) {
            FlatArray(returnList, obj);
            return returnList;
        }

        if (obj instanceof List<?>) {
            final List<?> list = (List<?>) obj;
            returnList.addAll(list.stream().map(new Function<Object, MaplePartyCharacter>() {

                @Override
                public MaplePartyCharacter apply(Object t) {
                    return (MaplePartyCharacter) t;
                }
            }).collect(Collectors.toList()));
            return returnList;
        }

        if (obj.getClass().isArray()){
            Object[] array = (Object[])obj;
            for (Object anArray : array) {
                returnList.add((MaplePartyCharacter) anArray);
            }
            return returnList;
        }

        returnList.add((MaplePartyCharacter) obj);
        return returnList;
    }

    private static void FlatArray(List<MaplePartyCharacter> returnList, Object partialArray){
        try {
            final Class<?> cls = Class.forName("jdk.nashorn.api.scripting.ScriptObjectMirror");
            if (cls.isAssignableFrom(partialArray.getClass())) {
                final Method isArray = cls.getMethod("isArray");
                final Object result = isArray.invoke(partialArray);
                if (result != null && result.equals(true)) {
                    final Method values = cls.getMethod("values");
                    final Object vals = values.invoke(partialArray);
                    if (vals instanceof Collection<?>) {
                        final Collection<?> coll = (Collection<?>) vals;
                        for(Object el : coll) {
                            if (cls.isAssignableFrom(el.getClass())) {
                                FlatArray(returnList, el);
                            }
                            else{
                                returnList.add((MaplePartyCharacter) el);
                            }
                        }
                    }
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ignored) {}
    }
    
    public static List<Integer> ConvertFromScriptInt(final Object obj){
        List<Integer> returnList = new ArrayList<>();

        if (obj == null){
            returnList.add(null);
            return returnList;
        }

        if (obj instanceof Bindings) {
            FloatArrayInt(returnList, obj);
            return returnList;
        }

        if (obj instanceof List<?>) {
            final List<?> list = (List<?>) obj;
            returnList.addAll(list.stream().map(new Function<Object, Integer>() {

                @Override
                public Integer apply(Object t) {
                  return (Integer) t;
                }
            }).collect(Collectors.toList()));
            return returnList;
        }

        if (obj.getClass().isArray()){
            Object[] array = (Object[])obj;
            for (Object anArray : array) {
                returnList.add((Integer) anArray);
            }
            return returnList;
        }

        returnList.add((Integer) obj);
        return returnList;
    }
    
     private static void FloatArrayInt(List<Integer> returnList, Object partialArray){
        try {
            final Class<?> cls = Class.forName("jdk.nashorn.api.scripting.ScriptObjectMirror");
            if (cls.isAssignableFrom(partialArray.getClass())) {
                final Method isArray = cls.getMethod("isArray");
                final Object result = isArray.invoke(partialArray);
                if (result != null && result.equals(true)) {
                    final Method values = cls.getMethod("values");
                    final Object vals = values.invoke(partialArray);
                    if (vals instanceof Collection<?>) {
                        final Collection<?> coll = (Collection<?>) vals;
                        for(Object el : coll) {
                            if (cls.isAssignableFrom(el.getClass())) {
                                FloatArrayInt(returnList, el);
                            }
                            else{
                                returnList.add((Integer) el);
                            }
                        }
                    }
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ignored) {}
    }
}
