package scarpetclasses;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import carpet.script.CarpetExpression;
import carpet.utils.Translations;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scarpetclasses.scarpet.ClassExpression;

import java.util.Map;

public class ScarpetClasses implements CarpetExtension, ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("Scarpet Classes");

    public static final String defaultClassScriptName = "default_classes";
    public static final String defaultClassConfigInclude = "base_classes";

    private static final SettingsManager settingsManager;

    //Taken from https://github.com/FxMorin/carpet-fixes/blob/dev/src/main/java/carpetfixes/CarpetFixesServer.java
    private static final String MOD_ID = "scarpetclasses";

    static {
        ModMetadata metadata = FabricLoader.getInstance().getModContainer(MOD_ID)
                .orElseThrow(RuntimeException::new).getMetadata();
        settingsManager = new SettingsManager(metadata.getVersion().getFriendlyString(), MOD_ID, metadata.getName());
    }

    @Override
    public void scarpetApi(CarpetExpression carpetExpression) {
        ClassExpression.apply(carpetExpression);
        if (ClassesSettings.overwriteNativeFunctions)
            ClassExpression.applyOverwrite(carpetExpression);
    }

    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(this);
    }

    @Override
    public void onGameStarted() {
        settingsManager.parseSettingsClass(ClassesSettings.class);
    }

    @Override
    public SettingsManager extensionSettingsManager() {
        return settingsManager;
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return Translations.getTranslationFromResourcePath("assets/scarpetclasses/lang/%s.json".formatted(lang));
    }
}
