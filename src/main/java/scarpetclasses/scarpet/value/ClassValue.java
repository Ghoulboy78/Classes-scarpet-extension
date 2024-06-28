package scarpetclasses.scarpet.value;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.ContainerValueInterface;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.DynamicRegistryManager;
import scarpetclasses.mixins.FunctionValueAccessorMixin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassValue extends Value implements ContainerValueInterface {

    //Todo think of a better place to store these strings
    /**
     * The name of the variable which refers to the class itself in methods
     */
    public static final String selfReference = "this";
    /**
     * Name of the initialisation method
     */
    public static final String initMethodName = "init";
    /**
     * Name of the method which overwrites behaviour with scarpet {@code str()} function
     */
    public static final String stringMethodName = "str";
    /**
     * Name of the method which overwrites behaviour with scarpet {@code bool()} function
     */
    public static final String booleanMethodName = "bool";
    /**
     * The name of this user-defined class, or the name of the user-defined class that this object belongs to.
     */
    public final String className;
    /**
     * Whether this is the declaration of the class or an object which is a member of that class
     */
    public final boolean isObject; //todo shift this to make classes untouchable by programmer
    private Map<String, Value> fields = new HashMap<>();
    private Map<String, FunctionValue> methods = new HashMap<>();

    /**
     * Context from when this was declared
     */
    private final Context context;

    /**
     * Defining a class
     *
     * @param name    Name of the user-defined class
     * @param members Map of the members (so fields and methods) of the user-defined class
     */
    public ClassValue(String name, Map<Value, Value> members, Context context) {
        this.className = name;
        this.context = context;
        this.isObject = false;
        for (Map.Entry<Value, Value> entry : members.entrySet()) {
            if (entry.getValue() instanceof FunctionValue f) {
                methods.put(entry.getKey().getString(), f);
            } else {
                fields.put(entry.getKey().getString(), entry.getValue());
            }
        }
    }

    /**
     * Instantiating an object
     *
     * @param declarer The class that this is an object of
     */
    public ClassValue(ClassValue declarer, Context c, List<Value> params) {
        isObject = true;
        this.context = c;
        className = declarer.className;
        this.fields = declarer.fields; //todo check if I need to copy this
        this.methods = declarer.methods;

        if(methods.containsKey(initMethodName)){
            callMethod(c, initMethodName, params);
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

    public Map<String, Value> getFields() {
        return fields;
    }

    public Map<String, FunctionValue> getMethods() {
        return methods;
    }

    /**
     * Method used to call a method in the class
     */
    public LazyValue callMethod(Context c, String name, List<Value> params) {

        if (!isObject)
            throw new InternalExpressionException("Must instantiate a class with 'new_object()' before calling a method");

        if (!methods.containsKey(name))
            throw new InternalExpressionException("Unknown method '" + name + "' in class '" + className + "'");

        FunctionValue func = methods.get(name);
        Map<String, LazyValue> outer = ((FunctionValueAccessorMixin) func).getOuterState();
        //If it's empty, then it gets set to null, which I totally missed out on
        //thx replaceitem
        if (outer == null)
            outer = new HashMap<>();

        outer.put(selfReference, (_c, _t) -> this);
        //todo This is where 'super' will go once inheritance is implemented
        ((FunctionValueAccessorMixin) func).setOuterState(outer);
        return func.callInContext(c, Context.NONE, params);
    }


    /**
     * This will be accessed via {@code type()} function,
     * whereas {@link ClassValue#className} will be accessed via {@code class_name()} function
     */
    @Override
    public String getTypeString() {
        return "class";
    }

    @Override
    public String getString() {
        if (isObject && hasMethod(stringMethodName)) {
            return callMethod(context, stringMethodName, List.of()).evalValue(context).getString();
        }

        //Making distinction between a class declaration and an object belonging to a class
        return (isObject ? "Object@" : "Class@") + className;// + this.hashCode();
    }

    @Override
    public boolean getBoolean() {
        if (isObject && hasMethod(booleanMethodName)) {
            return callMethod(context, booleanMethodName, List.of()).evalValue(context, Context.BOOLEAN).getBoolean();
        }

        return isObject;
    }

    @Override
    public NbtElement toTag(boolean force, DynamicRegistryManager regs) {
        return null;//todo
    }

    @Override
    public boolean put(Value where, Value value) {//todo containers
        if (!isObject)
            throw new InternalExpressionException("Must instantiate a class before modifying its fields");

        if (hasMethod(where.getString()))
            throw new InternalExpressionException("Cannot set value of method");

        if (!hasField(where.getString()))
            throw new InternalExpressionException("Tried to set value of nonexistent field: '" + where.getString() + "' in class of type '" + className + "'");
        boolean res = !value.equals(fields.get(where.getString()));
        fields.put(where.getString(), value);
        return res;
    }

    @Override
    public Value get(Value where) {//todo containers
        if (!isObject)
            throw new InternalExpressionException("Must instantiate a class before accessing its fields");
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

    @Override
    public Value add(Value other) {
        return other;//todo add this stuff later
    }
}
