package scarpetclasses.mixins;

import carpet.script.ScriptHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import scarpetclasses.scarpet.Classes;

@Mixin(ScriptHost.class)
public class ScriptHostInjectorMixin {
    @Inject(method="onClose", at=@At("HEAD"), remap=false)
    private void onClose(CallbackInfo ci){
        Classes.clearDeclaredClasses();//todo see if this is as stupid of a solution as I think it is
    }
}
