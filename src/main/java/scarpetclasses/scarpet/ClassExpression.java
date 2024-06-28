package scarpetclasses.scarpet;

import carpet.CarpetServer;
import carpet.script.CarpetExpression;
import carpet.script.Expression;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.MapValue;
import carpet.script.value.StringValue;
import scarpetclasses.scarpet.value.ClassValue;

import static carpet.CarpetSettings.superSecretSetting;
import static carpet.utils.Messenger.m;

public class ClassExpression {
    public static void carpetApply(CarpetExpression cexpr){

    }

    public static void apply(Expression expr){
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
