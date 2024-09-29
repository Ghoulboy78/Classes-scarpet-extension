package scarpetclasses;

import carpet.api.settings.Rule;

import static carpet.api.settings.RuleCategory.EXPERIMENTAL;

public class ClassesSettings {
    @Rule(categories = {EXPERIMENTAL})
    public static boolean overwriteNativeFunctions = false;
}
