package scarpetclasses.scarpet.value;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.value.BooleanValue;
import carpet.script.value.ContainerValueInterface;
import carpet.script.value.FunctionValue;
import carpet.script.value.MapValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.google.gson.JsonElement;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.DynamicRegistryManager;
import scarpetclasses.mixins.FunctionValueAccessorMixin;
import scarpetclasses.scarpet.Classes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @noinspection ReferenceToMixin
 */
public class ClassValue extends Value implements ContainerValueInterface {

    /**
     * The name of this user-defined class, or the name of the user-defined class that this object belongs to.
     */
    public final String className;
    /**
     * Context from when this was declared
     */
    private final Context context;
    private Map<String, Value> fields = new HashMap<>();
    private Map<String, FunctionValue> methods = new HashMap<>();

    /**
     * Instantiating an object
     */
    public ClassValue(String className, Context c, List<Value> params) {
        Classes.ScarpetClass declarer = Classes.getClass(className);

        this.context = c;
        this.className = className;
        this.fields = declarer.fields; //todo check if I need to copy this
        this.methods = declarer.methods;

        initializeCall(params);
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
     * Special private call to initialise, to ensure no one tries to initialise without using proper initialisation function
     */
    private void initializeCall(List<Value> params) {
        if (methods.containsKey(KeywordNames.initMethodName)) {
            FunctionValue initFunc = methods.get(KeywordNames.initMethodName);
            Map<String, LazyValue> outer = ((FunctionValueAccessorMixin) initFunc).getOuterState();

            if (outer != null)
                throw new InternalExpressionException("How did we get here? Had non-null outer scope at initialisation, make an issue at https://github.com/Ghoulboy78/Classes-scarpet-extension/issues");

            outer = new HashMap<>();
            outer.put(KeywordNames.selfReference, (_c, _t) -> this);
            //todo This is where 'super' will go once inheritance is implemented
            ((FunctionValueAccessorMixin) initFunc).setOuterState(outer);
            initFunc.callInContext(context, Context.NONE, params);

            //No need to let the programmer re-initialise an initialised object
            methods.remove(KeywordNames.initMethodName);
        }
    }

    /**
     * Method used to call a method in the class
     */
    public LazyValue callMethod(Context c, String name, List<Value> params) {
        if (!methods.containsKey(name))
            throw new InternalExpressionException("Unknown method '" + name + "' in class '" + className + "'");

        if (name.equals(KeywordNames.initMethodName))
            throw new InternalExpressionException("Tried to initialise a class improperly, note that this must be done with 'new_object()' function to avoid unwanted side-effects (and ensure wanted ones)");

        FunctionValue func = methods.get(name);
        Map<String, LazyValue> outer = ((FunctionValueAccessorMixin) func).getOuterState();
        //If it's empty, then it gets set to null, which I totally missed out on
        //thx replaceitem
        if (outer == null)
            outer = new HashMap<>();

        outer.put(KeywordNames.selfReference, (_c, _t) -> this);
        //todo This is where 'super' will go once inheritance is implemented
        ((FunctionValueAccessorMixin) func).setOuterState(outer);
        return func.callInContext(c, Context.NONE, params);
    }

    /**
     * Simple access to {@link ClassValue#callMethod(Context, String, List)}
     * but evaluated with local {@link ClassValue#context}
     */
    private Value callMethod(String name, Value... params) {
        return callMethod(context, name, List.of(params)).evalValue(context);
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
        if (hasMethod(KeywordNames.stringMethodName)) {
            return callMethod(KeywordNames.stringMethodName).getString();
        }

        return "Object@" + className + "@" + this.hashCode();
    }

    @Override
    public boolean getBoolean() {
        if (hasMethod(KeywordNames.booleanMethodName)) {
            return callMethod(context, KeywordNames.booleanMethodName, List.of()).evalValue(context, Context.BOOLEAN).getBoolean();
        }

        //todo possibly replace with null check
        throw new InternalExpressionException("Did not define '" + KeywordNames.booleanMethodName + "' for class value");
    }

    @Override
    public int length() {
        if (hasMethod(KeywordNames.lengthMethodName)) {
            return (int) callMethod(KeywordNames.lengthMethodName).readInteger();
        }

        return fields.size() + methods.size();
    }

    /* todo uncomment when I redo this
    @Override
    public Value deepcopy() {
        if (hasMethod(KeywordNames.deepCopyMethodName)) {
            return callMethod(KeywordNames.deepCopyMethodName);
        }

        Map<Value, Value> members = new HashMap<>();
        fields.forEach((s, v) -> members.put(StringValue.of(s), v.deepcopy()));
        methods.forEach((s, fv) -> members.put(StringValue.of(s), fv.deepcopy()));
        ClassValue copyClass = new ClassValue(className, members, context.recreate());
        return copyClass;
    }
     */

    @Override
    public int hashCode() {
        if (hasMethod(KeywordNames.hashMethodName)) {
            return (int) callMethod(KeywordNames.hashMethodName).readInteger();
        }

        return fields.hashCode() + methods.hashCode() + className.hashCode();
    }

    @Override
    public Value split(Value delimiter) {
        if (hasMethod(KeywordNames.splitMethodName)) {
            return callMethod(KeywordNames.splitMethodName, delimiter);
        }

        throw new InternalExpressionException("Did not define 'split' for class value");
    }

    @Override
    public Value slice(long from, Long to) {
        if (hasMethod(KeywordNames.sliceMethodName)) {
            return callMethod(KeywordNames.sliceMethodName, NumericValue.of(from), NumericValue.of(to));
        }

        throw new InternalExpressionException("Did not define 'slice' for class value");
    }

    //Todo change this majorly when I switch to newer class system, with storing and retrieving field data, since the rest will already be stored and memorised
    @Override
    public NbtElement toTag(boolean force, DynamicRegistryManager regs) {
        if (hasMethod(KeywordNames.makeNBTMethodName)) {
            return ((NBTSerializableValue) callMethod(KeywordNames.makeNBTMethodName, BooleanValue.of(force))).getTag();
        }
        if (!force) {
            throw new NBTSerializableValue.IncompatibleTypeException(this);
        }
        return NbtString.of(getString()); //this may not work, but I'll cross that bridge when we get there
    }

    @Override
    public JsonElement toJson() {
        if (hasMethod(KeywordNames.makeJSONMethodName)) {
            //Make sure to use MapValue's toJson() instead of Value's toJson()
            return ((MapValue) callMethod(KeywordNames.makeJSONMethodName)).toJson();
        }
        throw new ThrowStatement(this, Throwables.JSON_ERROR);
    }

    public Value toBase64() {
        if (hasMethod(KeywordNames.makeB64MethodName)) {
            return callMethod(KeywordNames.makeB64MethodName);
        }
        return StringValue.of(Base64.getEncoder().encodeToString(getString().getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public boolean put(Value where, Value value) {//todo containers
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
    public Value in(Value where) {
        return NULL;//todo containers
    }

    @Override
    public int compareTo(Value other) {
        if (this.hasMethod(KeywordNames.comparisonOperationmask)) {
            return (int) callMethod(KeywordNames.comparisonOperationmask, other).readInteger();
        }

        throw new InternalExpressionException("Did not define comparison for class value");
    }


    @Override
    public Value add(Value other) {
        if (this.hasMethod(KeywordNames.addOperationmask)) {
            return callMethod(KeywordNames.addOperationmask, other);
        }

        throw new InternalExpressionException("Did not define addition behaviour for class value");
    }

    @Override
    public Value subtract(Value other) {
        if (this.hasMethod(KeywordNames.minusOperationmask)) {
            return callMethod(KeywordNames.minusOperationmask, other);
        }

        throw new InternalExpressionException("Did not define subtraction behaviour for class value");
    }

    @Override
    public Value multiply(Value other) {
        if (this.hasMethod(KeywordNames.timesOperationmask)) {
            return callMethod(KeywordNames.timesOperationmask, other);
        }

        throw new InternalExpressionException("Did not define multiplication behaviour for class value");
    }

    @Override
    public Value divide(Value other) {
        if (this.hasMethod(KeywordNames.divideOperationmask)) {
            return callMethod(KeywordNames.divideOperationmask, other);
        }

        throw new InternalExpressionException("Did not define division behaviour for class value");
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Value ov && this.hasMethod(KeywordNames.equalsOperationmask)) {
            return callMethod(context, KeywordNames.equalsOperationmask, List.of(ov)).evalValue(context, Context.BOOLEAN).getBoolean();
        }
        return other instanceof ClassValue c && c.className.equals(className) && c.fields.equals(fields);
    }

    /**
     * Static class which contains all the keywords for classes, such as the name of the initialisation method
     * ({@link KeywordNames#initMethodName}), or the variable used for self-reference within methods
     * ({@link KeywordNames#selfReference}).
     */
    public static class KeywordNames {
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
         * Currently unused.
         */
        public static final String prettyStringMethodName = "pretty_str";
        /**
         * Name of the method which overwrites behaviour with scarpet {@code length()} function
         */
        public static final String lengthMethodName = "length";
        /**
         * Name of the method which overwrites behaviour with scarpet {@code slice()} function
         */
        public static final String sliceMethodName = "slice";
        /**
         * Name of the method which overwrites behaviour with scarpet {@code split()} function
         */
        public static final String splitMethodName = "split";
        /**
         * Name of the method which overwrites behaviour with scarpet {@code hash_code()} function
         */
        public static final String hashMethodName = "hash_code";
        /**
         * Name of the method which overwrites behaviour with scarpet {@code copy()} function
         */
        public static final String deepCopyMethodName = "copy";
        /**
         * Name of the method which overwrites behaviour with scarpet {@code encode_nbt()} function
         * todo make a note that reverse operation is prolly not possible
         */
        public static final String makeNBTMethodName = "to_nbt";
        /**
         * Name of the method which overwrites behaviour with scarpet {@code encode_json()} function
         * todo make a note that reverse operation is prolly not possible
         */
        public static final String makeJSONMethodName = "to_json";
        /**
         * Name of the method which overwrites behaviour with scarpet {@code encode_b64()} function
         */
        public static final String makeB64MethodName = "to_b64";
        /**
         * Name of the method which overwrites addition behaviour
         */
        public static final String addOperationmask = "add";
        /**
         * Name of the method which overwrites subtraction behaviour
         */
        public static final String minusOperationmask = "subtract";
        /**
         * Name of the method which overwrites multiplication behaviour
         */
        public static final String timesOperationmask = "multiply";
        /**
         * Name of the method which overwrites division behaviour
         */
        public static final String divideOperationmask = "divide";
        /**
         * Name of the method which overwrites equality '{@code ==}' operator
         */
        public static final String equalsOperationmask = "equals";
        /**
         * Name of the method which defines comparison for inequality operators
         */
        public static final String comparisonOperationmask = "compare";
    }
}
