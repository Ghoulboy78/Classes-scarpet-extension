package scarpetclasses.scarpet.value;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.Tokenizer;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In theory this will be able to be called more easily than the regular FunctionValue, but I may end up tearing it out when I get to implementing methods
 */
public class MethodValue extends FunctionValue {
    public MethodValue(Expression expression, Tokenizer.Token token, String name, LazyValue body, List<String> args, String varArgs, Map<String, LazyValue> outerState) {
        super(expression, token, name, body, args, varArgs, outerState);
    }

    public MethodValue(FunctionValue fv){
        super(fv.getExpression(), fv.getToken(), fv.getString(), LazyValue.NULL, fv.getArguments(), fv.getVarArgs(), new HashMap<>());
    }

    @Override
    public LazyValue callInContext(Context c, Context.Type type, List<Value> params){
        return super.callInContext(c, type, params);
    }
}
