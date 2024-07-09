package scarpetclasses.mixins;

import carpet.script.CarpetScriptHost;
import carpet.script.CarpetScriptServer;
import carpet.utils.Messenger;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import scarpetclasses.scarpet.Classes;

import java.util.Map;

@Mixin(CarpetScriptServer.class)
public class CarpetScriptServerInjectionMixin {
    @Shadow
    public Map<String, CarpetScriptHost> modules;

    @Inject(method = "removeScriptHost", at = @At("HEAD"), remap = false)
    private void removeModuleClasses(ServerCommandSource source, String name, boolean notifySource, boolean isRuleApp, CallbackInfoReturnable<Boolean> cir) {
        Messenger.m(source, "gi Removing classes from host "+name);
        Classes.clearDeclaredClasses(modules.get(name));
    }
}
