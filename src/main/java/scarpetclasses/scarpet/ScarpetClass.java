package scarpetclasses.scarpet;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;
import scarpetclasses.ScarpetClasses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Calling it ScarpetClass in order to avoid conflict with {@link Class}.
 * This class stores all information about classes, and handles inheritance.
 * This is where classes are stored before being initialised.
 */
public class ScarpetClass {
    // todo add info about which class this method was inherited from, for super call
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

        try {
            addParents(parents, this);
        } catch (StackOverflowError error) {
            throw new InternalExpressionException("Too many parents for class ");
        }
    }

    /**
     * Handles adding parent classes, as well as their methods etc.
     * todo handle inheritance conflicts
     */
    private void addParents(Set<ScarpetClass> parents, ScarpetClass fromChild) {
        if (parents.isEmpty()) return; //reached class Object, or Interface

        this.parents.addAll(parents);

        for (ScarpetClass parent : parents) {
            // Adding parent's parents
            addParents(parent.parents, parent);

            // Adding parent's fields and methods
            for (Map.Entry<String, Value> fieldEntry : parent.fields.entrySet()) {
                if(this.fields.containsKey(fieldEntry.getKey())){
                    //Overriding existing field from parent
                    //todo add stuff about which parent this field was inherited from
                } else {
                    this.fields.put(fieldEntry.getKey(), fieldEntry.getValue());
                }
            }
            for (Map.Entry<String, FunctionValue> methodEntry : parent.methods.entrySet()) {
                if(this.methods.containsKey(methodEntry.getKey())){
                    //Overriding existing method from parent
                    //todo maybe implement default methods one day
                    //todo add stuff about which parent this method was inherited from, for 'super' parameter
                } else {
                    this.methods.put(methodEntry.getKey(), methodEntry.getValue());
                }
            }
        }
    }
}