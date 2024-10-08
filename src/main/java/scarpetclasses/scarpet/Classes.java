package scarpetclasses.scarpet;

import carpet.script.CarpetContext;
import carpet.script.CarpetExpression;
import carpet.script.CarpetScriptHost;
import carpet.script.Context;
import carpet.script.ScriptHost;
import carpet.script.Module;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import scarpetclasses.mixins.ScriptHostMixin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static scarpetclasses.ScarpetClasses.LOGGER;
import static scarpetclasses.ScarpetClasses.defaultClassScriptName;
import static scarpetclasses.ScarpetClasses.defaultClassConfigInclude;

/**
 * A place to store declared classes, and a place to retrieve them afterwards
 */
public class Classes {

    //todo add option to make these visible universally (instead of by app / ScriptHost object), so all declared classes are visible in all apps
    //todo make it so (after implementing inheritance) that if the user imports the default classes library, all created classes will inherit from Objects (and this will also enable interfaces, etc.)
    private static final Map<ScriptHost, Map<String, ScarpetClass>> declaredClasses = new HashMap<>();

    public static void addNewClassDef(Context c, String className, Map<Value, Value> members) {
        ScriptHost host = c.host;

        Map<Value, Value> config = ((CarpetScriptHost) host).appConfig;
        boolean hasDefaultClasses = config.containsKey(StringValue.of(defaultClassConfigInclude)) && config.get(StringValue.of(defaultClassConfigInclude)).getBoolean();

        if (!declaredClasses.containsKey(host)) {//Initialising class-based stuff for this app
            declaredClasses.put(host, new HashMap<>());

            //If user wants default classes in config
            if(hasDefaultClasses) { //todo see if running this every time a class declared instead of on startup causes problems down the line
                Module defaultClasses = Module.fromJarPath("assets/scarpetclasses/scripts/", defaultClassScriptName, true);

                //Repurposed code from ScriptHost#importModule(Context c, String moduleName)
                ((ScriptHostMixin) host).getModules().put(defaultClasses.name(), defaultClasses);
                ScriptHost.ModuleData data = new ScriptHost.ModuleData(defaultClasses);
                //host.initializeModuleGlobals(data); This is unnecesary, but keeping it here in case it causes problems and needs to be done
                ((ScriptHostMixin) host).getModuleData().put(defaultClasses, data);

                //Repurposed code from CarpetScriptHost#runModuleCode(Context c, Module module)
                CarpetContext cc = (CarpetContext) c;
                CarpetExpression ex = new CarpetExpression(defaultClasses, defaultClasses.code(), cc.source(), cc.origin());
                ex.getExpr().asATextSource();
                ex.scriptRunCommand(host, cc.origin());
            }
        }

        if (declaredClasses.get(host).containsKey(className)) //todo possible add overwrite option?
            throw new InternalExpressionException("Already defined class '" + className + "'");

        declaredClasses.get(host).put(className, new ScarpetClass(className, members));
    }

    public static ScarpetClass getClass(ScriptHost host, String name) {
        if (!declaredClasses.containsKey(host) || !declaredClasses.get(host).containsKey(name))
            throw new InternalExpressionException("Unknown class '" + name + "'");

        return declaredClasses.get(host).get(name);
    }

    public static boolean hasClass(ScriptHost host, String name) {
        return declaredClasses.containsKey(host) && declaredClasses.get(host).containsKey(name);
    }

    /**
     * For getting rid of declared classes between loads and reloads (if host contains classes)
     */
    public static void clearDeclaredClasses(ScriptHost host) {
        if (declaredClasses.containsKey(host)) {
            Set<String> classes = declaredClasses.get(host).keySet();

            if(!classes.isEmpty()) {
                LOGGER.info("Removing %d classes from module %s: %s".formatted(classes.size(), host.getName(), classes));
            }

            declaredClasses.get(host).clear();
        }
    }

    public static Set<String> getDeclaredClassNames(ScriptHost host) {
        if (!declaredClasses.containsKey(host))
            return Collections.emptySet();
        return declaredClasses.get(host).keySet();
    }

    /**
     * Calling it this in order to avoid conflict with {@link Class}
     */
    public static class ScarpetClass {
        public final Map<String, Value> fields = new HashMap<>();
        public final Map<String, FunctionValue> methods = new HashMap<>();
        public final String className;

        public ScarpetClass(String name, Map<Value, Value> members) {
            this.className = name;
            for (Map.Entry<Value, Value> entry : members.entrySet()) {
                if (entry.getValue() instanceof FunctionValue f) {
                    methods.put(entry.getKey().getString(), f);
                } else {
                    fields.put(entry.getKey().getString(), entry.getValue());
                }
            }
        }
    }
}
