package scarpetclasses.scarpet;

import carpet.script.value.FunctionValue;
import carpet.script.value.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Calling it ScarpetClass in order to avoid conflict with {@link Class}.
 * This class stores all information about classes, and handles inheritance.
 */
public class ScarpetClass {
    public final Map<String, Value> fields = new HashMap<>();
    public final Map<String, FunctionValue> methods = new HashMap<>();
    public final String className;
    public final Set<ScarpetClass> parents = new HashSet<>();

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

    public ScarpetClass(String name, Map<Value, Value> members, Set<ScarpetClass> parents) {
        this.className = name;
        for (Map.Entry<Value, Value> entry : members.entrySet()) {
            if (entry.getValue() instanceof FunctionValue f) {
                methods.put(entry.getKey().getString(), f);
            } else {
                fields.put(entry.getKey().getString(), entry.getValue());
            }
        }
        this.parents.addAll(parents);
    }
}