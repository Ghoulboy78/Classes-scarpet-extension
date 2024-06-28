package scarpetclasses.scarpet;

import carpet.CarpetServer;
import carpet.script.CarpetExpression;
import carpet.script.Expression;
import carpet.script.ScriptHost;
import carpet.script.Tokenizer;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.MapValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.utils.Messenger;
import scarpetclasses.scarpet.value.ClassValue;

public class ClassExpression {
    public static void carpetApply(CarpetExpression cexpr){

    }

    public static void apply(Expression expr){
        expr.addContextFunction("new_class", 2, (c, t, lv)->{
            ClassValue newClass;
            Messenger.m(CarpetServer.minecraft_server.getCommandSource(), "gi Ran new_class function with name "+ lv.get(0).getString());
            Messenger.m(CarpetServer.minecraft_server.getCommandSource(), "gi Param map "+ lv.get(1).getString());

            if (lv.get(1) instanceof MapValue map)
                 newClass = new ClassValue(lv.get(0).getString(), map.getMap());
            else
                throw new InternalExpressionException("Must declare a class with a map of fields and methods.");

            Messenger.m(CarpetServer.minecraft_server.getCommandSource(), "gi Created new class");


            return Value.NULL;
        });


        expr.addUnaryFunction("class_name", v-> StringValue.of(((ClassValue) v).className));

    }
}
