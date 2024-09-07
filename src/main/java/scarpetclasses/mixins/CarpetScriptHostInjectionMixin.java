package scarpetclasses.mixins;

import carpet.script.CarpetContext;
import carpet.script.CarpetExpression;
import carpet.script.CarpetScriptHost;
import carpet.script.Module;
import carpet.script.ScriptHost;
import carpet.script.value.FunctionValue;
import carpet.script.value.MapValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

import static scarpetclasses.ScarpetClasses.defaultClassConfigInclude;
import static scarpetclasses.ScarpetClasses.defaultClassScriptName;

@Mixin(CarpetScriptHost.class)
public abstract class CarpetScriptHostInjectionMixin {

    //this is code that in theory should run when __config() is read, but in practice I can't get it to work.
    //@Inject(method = "readConfig", at = @At(value = "INVOKE_ASSIGN", target = "Ljava/util/Map;getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), locals = LocalCapture.CAPTURE_FAILSOFT, remap = false)
    private void mixin(CallbackInfoReturnable<Boolean> cir, FunctionValue configFunction, Value ret, MapValue map, Map<Value, Value> config) {

        if (config.getOrDefault(StringValue.of(defaultClassConfigInclude), Value.FALSE).getBoolean()) {

            CarpetScriptHost host = (CarpetScriptHost) (Object) this;

            Module defaultClasses = Module.fromJarPath("assets/scarpetclasses/scripts/", defaultClassScriptName, true);

            //Repurposed code from ScriptHost#importModule(Context c, String moduleName)
            ((ScriptHostMixin) host).getModules().put(defaultClasses.name(), defaultClasses);
            ScriptHost.ModuleData data = new ScriptHost.ModuleData(defaultClasses);
            //host.initializeModuleGlobals(data); This is unnecesary, but keeping it here in case it causes problems and needs to be done
            ((ScriptHostMixin) host).getModuleData().put(defaultClasses, data);

            //Repurposed code from CarpetScriptHost#runModuleCode(Context c, Module module)
            CarpetContext cc = new CarpetContext(host, host.responsibleSource);
            CarpetExpression ex = new CarpetExpression(defaultClasses, defaultClasses.code(), cc.source(), cc.origin());
            ex.getExpr().asATextSource();
            ex.scriptRunCommand(host, cc.origin());
        }
    }
}
