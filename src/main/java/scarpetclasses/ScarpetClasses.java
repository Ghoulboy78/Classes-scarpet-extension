package scarpetclasses;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.script.CarpetExpression;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scarpetclasses.scarpet.ClassExpression;

public class ScarpetClasses implements CarpetExtension, ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("Scarpet Classes");

    public static final String defaultClassScriptName = "default_classes";
    public static final String defaultClassConfigInclude = "base_classes";

    @Override
    public void scarpetApi(CarpetExpression carpetExpression){
        ClassExpression.apply(carpetExpression);
        //todo add carpet rule to control overwrite, default being no overwrite
        ClassExpression.applyOverwrite(carpetExpression);
    }

    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(this);
    }
}
