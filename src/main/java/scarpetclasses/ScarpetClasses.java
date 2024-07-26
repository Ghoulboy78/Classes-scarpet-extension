package scarpetclasses;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.script.CarpetExpression;
import carpet.script.CarpetScriptServer;
import carpet.script.Module;
import net.fabricmc.api.ModInitializer;
import scarpetclasses.scarpet.ClassExpression;

public class ScarpetClasses implements CarpetExtension, ModInitializer {

    @Override
    public void scarpetApi(CarpetExpression carpetExpression){
        ClassExpression.apply(carpetExpression);
        ClassExpression.applyOverwrite(carpetExpression);
        //todo see difference between this being a library or not
        CarpetScriptServer.registerBuiltInApp(Module.fromJarPath("assets/scarpetclasses/scripts/", "default_classes", true));
    }

    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(this);
    }
}
