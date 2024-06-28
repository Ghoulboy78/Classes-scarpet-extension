package scarpetclasses.mixins;

import carpet.script.LazyValue;
import carpet.script.value.FunctionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(FunctionValue.class)
public interface FunctionValueAccessorMixin {
    @Accessor(value = "outerState", remap = false)
    Map<String, LazyValue> getOuterState();

    @Accessor(value = "outerState", remap = false)
    void setOuterState(Map<String, LazyValue> newState);
}
