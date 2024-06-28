package scarpetclasses.scarpet.value;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.ContainerValueInterface;
import carpet.script.value.FunctionValue;
import carpet.script.value.MapValue;
import carpet.script.value.Value;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.DynamicRegistryManager;

import java.util.HashMap;
import java.util.Map;

public class ClassValue extends Value implements ContainerValueInterface {

    /**
     * The name of this user-defined class, or the name of the user-defined class that this object belongs to.
     */
    public final String className;
    /**
     * Whether this is the declaration of the class or an object which is a member of that class
     */
    private final boolean isObject;
    public final Map<String, Value> fields = new HashMap<>();
    public final Map<String, FunctionValue> methods = new HashMap<>();

    /**
     * Defining a class
     * @param name Name of the user-defined class
     * @param members Map of the members (so fields and methods) of the user-defined class
     */
    public ClassValue(String name, Map<Value, Value> members) {
        this.className = name;
        this.isObject = false;
        for (Map.Entry<Value, Value> entry : members.entrySet()) {
            if (entry.getValue() instanceof FunctionValue f) {
                methods.put(entry.getKey().getString(), f);
            } else {
                fields.put(entry.getKey().getString(), entry.getValue());
            }
        }
    }

    public boolean hasMember(String member) {
        return hasField(member) || hasMethod(member);
    }

    public boolean hasMethod(String method) {
        return methods.containsKey(method);
    }

    public boolean hasField(String field) {
        return fields.containsKey(field);
    }

    @Override
    public String getString() {
        if (hasMethod("str")) {
            methods.get("str"); //Idk how to do this
        }

        //Making distinction between a class declaration and an object belonging to that class
        return (isObject? "Object" : "Class-")+ className + "@" + this.hashCode();
    }

    @Override
    public boolean getBoolean() {
        return false;//todo
    }

    @Override
    public NbtElement toTag(boolean force, DynamicRegistryManager regs) {
        return null;//todo
    }

    @Override
    public boolean put(Value where, Value value) {//todo containers
        if (!hasField(where.getString())) throw new InternalExpressionException("Tried to set value of nonexistant field: "+where.getString());
        boolean res = !value.equals(fields.get(where.getString()));
        fields.put(where.getString(), value);
        return res;
    }

    @Override
    public Value get(Value where) {//todo containers
        return fields.getOrDefault(where.getString(), NULL);
    }

    @Override
    public boolean has(Value where) {//todo containers
        return hasMember(where.getString());
    }

    @Override
    public boolean delete(Value where) {
        return false;//todo possibly make this illegal (with exception for containers ofc)
    }

    public Value add(Value other){
        return other;//todo add this stuff later
    }
}
