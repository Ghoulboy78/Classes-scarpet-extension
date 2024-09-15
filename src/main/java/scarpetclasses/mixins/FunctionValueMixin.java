package scarpetclasses.mixins;

import carpet.script.LazyValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(FunctionValue.class)
public interface FunctionValueMixin {
    @Accessor(value = "outerState", remap = false)
    Map<String, LazyValue> getOuterState();

    @Accessor(value = "outerState", remap = false)
    void setOuterState(Map<String, LazyValue> newState);

    @Invoker(value= "clone", remap = false) //Cos clone is a special method, so it causes problems to just call the method clone
    Value cloneFunction();
}
