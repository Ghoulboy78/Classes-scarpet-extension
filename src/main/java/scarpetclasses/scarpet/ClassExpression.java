package scarpetclasses.scarpet;

import carpet.script.CarpetExpression;
import carpet.script.Expression;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import scarpetclasses.scarpet.value.ClassValue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static scarpetclasses.ScarpetClasses.LOGGER;
import static scarpetclasses.scarpet.value.ClassValue.KeywordNames.typeString;

public class ClassExpression {

    public static void apply(CarpetExpression cexpr) {
        Expression expr = cexpr.getExpr();

        expr.addContextFunction("declare_class", -1, (c, t, lv) -> { //maybe change name of this func
            if (lv.isEmpty()) { //without arguments, returns all declared classes
                return ListValue.wrap(Classes.getDeclaredClassNames(c.host).stream().map(StringValue::of));
            }

            if (lv.size() == 1)
                throw new InternalExpressionException("Must declare a class with at least a name and map of fields and methods");

            if (lv.get(1) instanceof MapValue map) {
                Classes.addNewClassDef(c, lv.get(0).getString(), map.getMap());
            } else
                throw new InternalExpressionException("Must declare a class with a map of fields and methods");

            return lv.get(0);//returning name of the new class
        });

        addUnaryClassFunction(expr, "class_name", c -> StringValue.of(c.className));
        addUnaryClassFunction(expr, "class_fields", c -> ListValue.wrap(c.getFields().keySet().stream().map(StringValue::of)));
        addUnaryClassFunction(expr, "class_methods", c -> ListValue.wrap(c.getMethods().keySet().stream().map(StringValue::of)));

        expr.addContextFunction("clear_classes", 0, (c, t, lv) -> {
            Classes.clearDeclaredClasses(c.host);
            return Value.TRUE;
        });

        expr.addContextFunction("new", -1, (c, t, lv) -> { //possibly change back to 'new_object' for clarity?
            if (lv.isEmpty()) throw new InternalExpressionException("'new' requires at least a class name");

            return new ClassValue(lv.get(0).getString(), c, lv.subList(1, lv.size()));
        });

        expr.addLazyFunction("call_function", -1, (c, t, lv) -> {
            if (lv.size() < 2)
                throw new InternalExpressionException("'call_function' requires at least an object and a method name to call");

            ClassValue object = (ClassValue) lv.get(0).evalValue(c, t);
            String methodName = lv.get(1).evalValue(c, t).getString();

            return object.callMethod(c, methodName, FunctionValue.unpackArgs(lv.subList(2, lv.size()), c));
        });
    }

    /**
     * Simple way of adding {@link ClassValue} based functions, since there are a few of these
     */
    public static void addUnaryClassFunction(Expression expr, String name, Function<ClassValue, Value> fun) {
        expr.addUnaryFunction(name, v -> {
            if (v instanceof ClassValue c) {
                return fun.apply(c);
            }
            throw new InternalExpressionException(name + " requires a class value as argument, not " + v.getTypeString());
        });
    }

    /**
     * A separate method for the overwrites in case they break stuff
     */
    public static void applyOverwrite(CarpetExpression cexpr) {
        //Keeping this annoying message in logs, because the rule is off by default, and turning it on without knowing the full consequences will cause errors.
        LOGGER.info("Overwriting native scarpet functions with classes-compatible ones");

        Expression expr = cexpr.getExpr();

        expr.addUnaryFunction("encode_b64", v -> {
            if (v instanceof ClassValue c) return c.toBase64();
            return StringValue.of(Base64.getEncoder().encodeToString(v.getString().getBytes(StandardCharsets.UTF_8)));
        });

        expr.addContextFunction("parse_nbt", 1, (c, t, lv) -> {
            Value v = lv.getFirst();
            if (v instanceof final NBTSerializableValue nbtsv) {
                NbtElement tag = nbtsv.getTag();

                if (tag instanceof NbtCompound ctag && Classes.hasClass(c.host, ctag.getString(typeString))) {
                    Map<String, Value> fields = new HashMap<>();
                    String className = ctag.getString(typeString);
                    ctag.remove(typeString);
                    for (String field : ctag.getKeys()) {
                        fields.put(field, ((NBTSerializableValue) NBTSerializableValue.of(ctag.get(field))).toValue());
                    }

                    return new ClassValue(className, c, fields);
                }

                return nbtsv.toValue();
            }
            NBTSerializableValue ret = NBTSerializableValue.parseString(v.getString());
            return ret == null ? Value.NULL : ret.toValue();
        });
    }
}
