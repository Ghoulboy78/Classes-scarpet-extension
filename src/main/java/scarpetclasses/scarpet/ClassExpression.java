package scarpetclasses.scarpet;

import carpet.script.CarpetExpression;
import carpet.script.Expression;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.MapValue;
import carpet.script.value.StringValue;
import scarpetclasses.scarpet.value.ClassValue;

//Temporary imports for testing purposes
import static carpet.CarpetSettings.superSecretSetting;
import static carpet.utils.Messenger.m;
import static carpet.CarpetServer.minecraft_server;

public class ClassExpression {

    public static void apply(CarpetExpression cexpr){

        Expression expr = cexpr.getExpr();

        expr.addContextFunction("new_class", 2, (c, t, lv)->{
            ClassValue newClass;

            if (lv.get(1) instanceof MapValue map)
                 newClass = new ClassValue(lv.get(0).getString(), map.getMap());
            else
                throw new InternalExpressionException("Must declare a class with a map of fields and methods.");

            return newClass;
        });

        expr.addUnaryFunction("class_name", v-> StringValue.of(((ClassValue) v).className));

    }
}
