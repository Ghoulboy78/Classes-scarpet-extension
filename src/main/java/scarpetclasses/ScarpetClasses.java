package scarpetclasses;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.script.CarpetExpression;
import net.fabricmc.api.ModInitializer;
import scarpetclasses.scarpet.ClassExpression;

public class ScarpetClasses implements CarpetExtension, ModInitializer {

    @Override
    public void scarpetApi(CarpetExpression carpetExpression){
        ClassExpression.apply(carpetExpression);
    }

    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(this);
    }
}
