package scarpetclasses.scarpet;

import carpet.script.CarpetExpression;
import carpet.script.Expression;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import scarpetclasses.scarpet.value.ClassValue;

import java.util.function.Function;

//Temporary imports for testing purposes
import static carpet.CarpetSettings.superSecretSetting;
import static carpet.utils.Messenger.m;
import static carpet.CarpetServer.minecraft_server;

public class ClassExpression {

    public static void apply(CarpetExpression cexpr) {

        Expression expr = cexpr.getExpr();

        expr.addContextFunction("new_class", 2, (c, t, lv) -> {
            ClassValue newClass;

            if (superSecretSetting) {
                m(minecraft_server.getCommandSource(), "gi new class name: " + lv.get(0).getString());
                m(minecraft_server.getCommandSource(), "gi new class params: " + lv.get(1).getString());
            }

            if (lv.get(1) instanceof MapValue map)
                newClass = new ClassValue(lv.get(0).getString(), map.getMap());
            else
                throw new InternalExpressionException("Must declare a class with a map of fields and methods.");

            return newClass;
        });

        addUnaryClassFunction(expr, "class_name", c -> StringValue.of(c.className));
        addUnaryClassFunction(expr, "class_fields", c -> ListValue.wrap(c.fields.keySet().stream().map(StringValue::of)));
        addUnaryClassFunction(expr, "class_methods", c -> ListValue.wrap(c.methods.keySet().stream().map(StringValue::of)));


        expr.addLazyFunction("call_function", -1, (c, t, lv) -> {
            if (lv.size() < 2)
                throw new InternalExpressionException("'call_function' requires at least an object and a method name to call");

            ClassValue object = (ClassValue) lv.get(0).evalValue(c, t);
            String methodName = lv.get(1).evalValue(c, t).getString();
            return object.callMethod(c.host, cexpr.getSource(), cexpr.getOrigin(), methodName, FunctionValue.unpackArgs(lv.subList(2, lv.size()), c));
        });
    }

    /**
     * Simple way of adding classvalue based functions, since there are plenty of these
     */
    public static void addUnaryClassFunction(Expression expr, String name, Function<ClassValue, Value> fun) {
        expr.addUnaryFunction(name, v -> {
            if (v instanceof ClassValue c) {
                return fun.apply(c);
            }
            throw new InternalExpressionException(name + " requires a class value as argument, not " + v.getTypeString());
        });
    }
}
