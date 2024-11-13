package scarpetclasses.mixins;

import carpet.script.Module;
import carpet.script.ScriptHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = ScriptHost.class, remap = false)
public interface ScriptHostMixin {
    @Accessor(value = "modules")
    Map<String, Module> getModules();

    @Accessor(value = "moduleData")
    Map<Module, ScriptHost.ModuleData> getModuleData();
}
