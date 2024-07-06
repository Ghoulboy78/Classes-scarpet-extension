package scarpetclasses.scarpet;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A place to store declared classes, and a place to retrieve them afterwards
 */
public class Classes {
    //todo possibly store these by app/ ScriptHost object
    //todo possibly add default classes here?
    private static final Map<String, ScarpetClass> declaredClasses = new HashMap<>();

    public static void addNewClassDef(String className, Map<Value, Value> members) {
        if (declaredClasses.containsKey(className)) //todo possible add overwrite option?
            throw new InternalExpressionException("Already defined class '" + className + "'");

        declaredClasses.put(className, new ScarpetClass(className, members));
    }

    public static ScarpetClass getClass(String name) {
        if (!declaredClasses.containsKey(name))
            throw new InternalExpressionException("Unknown class '" + name + "'");

        return declaredClasses.get(name);
    }

    /**
     * For getting rid of declared classes between loads
     * todo test if how it works with multiple apps etc.
     * todo test wth commandline declared classes
     */
    public static void clearDeclaredClasses() {
        declaredClasses.clear();
    }

    public static Set<String> getDeclaredClassNames() {
        return declaredClasses.keySet();
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
