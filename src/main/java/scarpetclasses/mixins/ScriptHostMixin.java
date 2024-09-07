package scarpetclasses.mixins;

import carpet.script.Module;
import carpet.script.ScriptHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ScriptHost.class)
public interface ScriptHostMixin {
    @Accessor(value = "modules", remap = false)
    Map<String, Module> getModules();

    @Accessor(value = "moduleData", remap = false)
    Map<Module, ScriptHost.ModuleData> getModuleData();
}
