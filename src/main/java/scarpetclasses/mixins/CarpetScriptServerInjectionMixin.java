package scarpetclasses.mixins;

import carpet.script.CarpetScriptHost;
import carpet.script.CarpetScriptServer;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import scarpetclasses.scarpet.Classes;

import java.util.Map;
import java.util.Set;

import static scarpetclasses.ScarpetClasses.LOGGER;

@Mixin(CarpetScriptServer.class)
public class CarpetScriptServerInjectionMixin {
    @Shadow
    public Map<String, CarpetScriptHost> modules;

    @Inject(method = "removeScriptHost", at = @At("HEAD"), remap = false)
    private void removeModuleClasses(ServerCommandSource source, String name, boolean notifySource, boolean isRuleApp, CallbackInfoReturnable<Boolean> cir) {
        Set<String> classes = Classes.getDeclaredClassNames(modules.get(name));
        if (!classes.isEmpty()) {//todo maybe make info instead of warn
            LOGGER.warn("Removing %d classes from module %s: %s".formatted(classes.size(), name, classes));
            Classes.clearDeclaredClasses(modules.get(name));
        }
    }
}
