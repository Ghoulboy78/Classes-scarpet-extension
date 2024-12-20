package scarpetclasses.scarpet.value;

import carpet.script.CarpetScriptHost;
import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.external.Carpet;
import carpet.script.value.BooleanValue;
import carpet.script.value.ContainerValueInterface;
import carpet.script.value.FunctionValue;
import carpet.script.value.MapValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.google.gson.JsonElement;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.DynamicRegistryManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import scarpetclasses.mixins.FunctionValueMixin;
import scarpetclasses.scarpet.Classes;
import scarpetclasses.scarpet.ScarpetClass;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static scarpetclasses.ScarpetClasses.LOGGER;

public class ClassValue extends Value implements ContainerValueInterface {

    /**
     * The name of the user-defined class that this object is instantiated from
     */
    public final String className;
    /**
     * Context from when this was declared
     */
    private final Context context;
    /**
     * Object fields. Todo implement public and private fields and methods maybe
     */
    private final Map<String, Value> fields;
    /**
     * Object methods
     */
    private final Map<String, FunctionValue> methods;

    /**
     * Class parent names, if applicable. Not gonna store the {@link ScarpetClass} objects unless required
     */
    public final Set<String> parents;

    /**
     * Instantiating an object
     */
    public ClassValue(String className, Context c, List<Value> params) {
        ScarpetClass declarer = Classes.getClass(c.host, className);

        this.context = c;
        this.className = className;
        this.fields = declarer.fields.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().deepcopy()));
        this.methods = declarer.methods.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (FunctionValue) ((FunctionValueMixin) e.getValue()).cloneFunction()));
        this.parents = declarer.parents.stream().map(sc->sc.className).collect(Collectors.toUnmodifiableSet());

        initializeCall(params);
    }

    /**
     * Creating an object that's already initialised, though it may have unexpected values for fields.
     */
    public ClassValue(String className, Context c, Map<String, Value> fields) {
        this.context = c;
        this.className = className;
        Set<String> fieldKeys = Classes.getClass(c.host, className).fields.keySet();
        if (!fieldKeys.equals(fields.keySet()))
            throw new InternalExpressionException("Mismatched fields, class '" + className + "' requires fields like (" + StringUtils.join(fieldKeys, ", ") + ")");
        
        this.fields = fields;
        this.methods = Classes.getClass(c.host, className).methods;
        this.parents = new HashSet<>(); //todo see abt this, if required
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

    public boolean isInstanceOf(String otherClass){
        return className.equals(otherClass) || parents.contains(otherClass);
    }

    /**
     * Special private call to initialise, to ensure no one tries to initialise without using proper initialisation function
     */
    private void initializeCall(List<Value> params) {
        if (methods.containsKey(KeywordNames.initMethodName)) {
            FunctionValue initFunc = methods.get(KeywordNames.initMethodName);
            Map<String, LazyValue> outer = ((FunctionValueMixin) initFunc).getOuterState();

            if (outer == null) {
                outer = new HashMap<>();
            } else if (outer.containsKey(KeywordNames.selfReference)) { //This is a mistake that can be corrected, but that should not go unnoticed
                String msg = "Tried to override '%s' variable in class '%s' initialisation with an outer scope variable of the same name. This is bad practice and will be overwritten.".formatted(KeywordNames.selfReference, className);
                LOGGER.warn(msg);
                Carpet.Messenger_message(((CarpetScriptHost) context.host).responsibleSource, "r " + msg);
            } //A similar thing for 'super'

            outer.put(KeywordNames.selfReference, (_c, _t) -> this);
            //todo This is where 'super' will go once inheritance is implemented, will have to test overriding as well as using super's methods then
            ((FunctionValueMixin) initFunc).setOuterState(outer);
            initFunc.callInContext(context, Context.NONE, params);

            //No need to let the programmer re-initialise an initialised object. Redundant since we don't allow it anyway, but better be safe than sorry
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
        Map<String, LazyValue> outer = ((FunctionValueMixin) func).getOuterState();
        //If it's empty, then it gets set to null, which I totally missed out on
        //thx replaceitem
        if (outer == null)
            outer = new HashMap<>();

        outer.put(KeywordNames.selfReference, (_c, _t) -> this);
        //todo This is where 'super' will go once inheritance is implemented
        ((FunctionValueMixin) func).setOuterState(outer);
        return func.callInContext(c, Context.NONE, params);
    }

    /**
     * Simple access to {@link ClassValue#callMethod(Context, String, List)}
     * but evaluated with local {@link ClassValue#context}
     */
    private Value callMethod(String name, Value... params) {
        return callMethod(context, name, List.of(params)).evalValue(context);
    }

    private InternalExpressionException undefinedMethod(String name) {
        return new InternalExpressionException("Did not define " + name + " for class '" + className + "'");
    }

    /**
     * This will be accessed via {@code type()} function,
     * whereas {@link ClassValue#className} will be accessed via {@code class_name()} function
     */
    @Override
    @NotNull
    public String getTypeString() {
        return KeywordNames.typeString;
    }

    @Override
    @NotNull
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

        throw undefinedMethod("'" + KeywordNames.booleanMethodName + "'");
    }

    @Override
    public int length() {
        if (hasMethod(KeywordNames.lengthMethodName)) {
            return (int) callMethod(KeywordNames.lengthMethodName).readInteger();
        }

        return fields.size() + methods.size();
    }


    @Override
    @NotNull
    public Value deepcopy() {
        if (hasMethod(KeywordNames.deepCopyMethodName)) {
            return callMethod(KeywordNames.deepCopyMethodName);//Not using .deepcopy(), but keeping a comment here in case problems arise
        }

        return new ClassValue(className, context, fields);
    }

    @Override
    public int hashCode() {
        if (hasMethod(KeywordNames.hashMethodName)) {
            return (int) callMethod(KeywordNames.hashMethodName).readInteger();
        }

        return fields.hashCode() + methods.hashCode() + className.hashCode();
    }

    @Override
    @NotNull
    public Value split(Value delimiter) {
        if (hasMethod(KeywordNames.splitMethodName)) {
            return callMethod(KeywordNames.splitMethodName, delimiter);
        }

        throw undefinedMethod("'" + KeywordNames.splitMethodName + "'");
    }

    @Override
    @NotNull
    public Value slice(long from, Long to) {
        if (hasMethod(KeywordNames.sliceMethodName)) {
            return callMethod(KeywordNames.sliceMethodName, NumericValue.of(from), NumericValue.of(to));
        }

        throw undefinedMethod("'" + KeywordNames.sliceMethodName + "'");
    }


    @Override
    @NotNull
    public NbtElement toTag(boolean force, DynamicRegistryManager regs) {
        if (hasMethod(KeywordNames.makeNBTMethodName)) {
            return ((NBTSerializableValue) callMethod(KeywordNames.makeNBTMethodName, BooleanValue.of(force))).getTag();
        }
        if (!force) { //Cos this shouldn't normally be done cos it conflicts with nbt maps
            throw new NBTSerializableValue.IncompatibleTypeException(this);
        }
        NbtCompound tag = new NbtCompound();
        fields.forEach((s, v) -> tag.put(s, v.toTag(true, regs)));
        tag.putString(KeywordNames.typeString, className);

        return tag;
    }

    @Override
    @NotNull
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
    @NotNull
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
    @NotNull
    public Value in(Value where) {
        return NULL;//todo containers
    }

    @Override
    public int compareTo(Value other) {
        if (this.hasMethod(KeywordNames.comparisonOperationMask)) {
            return (int) callMethod(KeywordNames.comparisonOperationMask, other).readInteger();
        }

        throw undefinedMethod("comparison behaviour");
    }


    @Override
    @NotNull
    public Value add(Value other) {
        if (this.hasMethod(KeywordNames.addOperationMask)) {
            return callMethod(KeywordNames.addOperationMask, other);
        }

        throw undefinedMethod("addition behaviour");
    }

    @Override
    @NotNull
    public Value subtract(Value other) {
        if (this.hasMethod(KeywordNames.minusOperationMask)) {
            return callMethod(KeywordNames.minusOperationMask, other);
        }

        throw undefinedMethod("subtraction behaviour");
    }

    @Override
    @NotNull
    public Value multiply(Value other) {
        if (this.hasMethod(KeywordNames.timesOperationMask)) {
            return callMethod(KeywordNames.timesOperationMask, other);
        }

        throw undefinedMethod("multiplication behaviour");
    }

    @Override
    @NotNull
    public Value divide(Value other) {
        if (this.hasMethod(KeywordNames.divideOperationMask)) {
            return callMethod(KeywordNames.divideOperationMask, other);
        }

        throw undefinedMethod("division behaviour");
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Value ov && this.hasMethod(KeywordNames.equalsOperationMask)) {
            return callMethod(context, KeywordNames.equalsOperationMask, List.of(ov)).evalValue(context, Context.BOOLEAN).getBoolean();
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
         * Return value of {@link ClassValue#getTypeString()}, as well as used in nbt and json conversions
         */
        public static final String typeString = "class";
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
        public static final String addOperationMask = "add";
        /**
         * Name of the method which overwrites subtraction behaviour
         */
        public static final String minusOperationMask = "subtract";
        /**
         * Name of the method which overwrites multiplication behaviour
         */
        public static final String timesOperationMask = "multiply";
        /**
         * Name of the method which overwrites division behaviour
         */
        public static final String divideOperationMask = "divide";
        /**
         * Name of the method which overwrites equality '{@code ==}' operator
         */
        public static final String equalsOperationMask = "equals";
        /**
         * Name of the method which defines comparison for inequality operators
         */
        public static final String comparisonOperationMask = "compare";
    }
}
