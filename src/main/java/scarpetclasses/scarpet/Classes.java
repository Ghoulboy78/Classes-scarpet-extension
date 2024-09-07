package scarpetclasses.scarpet;

import carpet.script.ScriptHost;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A place to store declared classes, and a place to retrieve them afterwards
 */
public class Classes {

    //todo add option to make these visible universally (instead of by app / ScriptHost object)
    //todo make it so (after implementing inheritance) that if the user imports the default classes library, all created classes will inherit from Objects (and this will also enable interfaces, etc.)
    private static final Map<ScriptHost, Map<String, ScarpetClass>> declaredClasses = new HashMap<>();

    public static void addNewClassDef(ScriptHost host, String className, Map<Value, Value> members) {
        if (!declaredClasses.containsKey(host))
            declaredClasses.put(host, new HashMap<>());

        //host.getName()

        if (declaredClasses.get(host).containsKey(className)) //todo possible add overwrite option?
            throw new InternalExpressionException("Already defined class '" + className + "'");

        declaredClasses.get(host).put(className, new ScarpetClass(className, members));
    }

    public static ScarpetClass getClass(ScriptHost host, String name) {
        if (!declaredClasses.containsKey(host) || !declaredClasses.get(host).containsKey(name))
            throw new InternalExpressionException("Unknown class '" + name + "'");

        return declaredClasses.get(host).get(name);
    }

    public static boolean hasClass(ScriptHost host, String name) {
        return declaredClasses.containsKey(host) && declaredClasses.get(host).containsKey(name);
    }

    /**
     * For getting rid of declared classes between loads and reloads (if host contains classes)
     */
    public static void clearDeclaredClasses(ScriptHost host) {
        if (declaredClasses.containsKey(host))
            declaredClasses.get(host).clear();
    }

    public static Set<String> getDeclaredClassNames(ScriptHost host) {
        if (!declaredClasses.containsKey(host))
            return Collections.emptySet();
        return declaredClasses.get(host).keySet();
    }

    /**
     * Calling it this in order to avoid conflict with {@link Class}
     */
    public static class ScarpetClass {
        public final Map<String, Value> fields = new HashMap<>();
        public final Map<String, FunctionValue> methods = new HashMap<>();
        public final String className;

        public ScarpetClass(String name, Map<Value, Value> members) {
            this.className = name;
            for (Map.Entry<Value, Value> entry : members.entrySet()) {
                if (entry.getValue() instanceof FunctionValue f) {
                    methods.put(entry.getKey().getString(), f);
                } else {
                    fields.put(entry.getKey().getString(), entry.getValue());
                }
            }
        }
    }
}
