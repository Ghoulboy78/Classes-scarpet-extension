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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static scarpetclasses.ScarpetClasses.LOGGER;
import static scarpetclasses.ScarpetClasses.defaultClassScriptName;
import static scarpetclasses.ScarpetClasses.defaultClassConfigInclude;

/**
 * A place to store declared classes, and a place to retrieve them afterwards
 * TODO Currently implementing inheritance
 */
public class Classes {

    //todo add option to make these visible universally (instead of by app / ScriptHost object), so all declared classes are visible in all apps
    //todo make it so (after implementing inheritance) that if the user imports the default classes library, all created classes will inherit from Objects (and this will also enable interfaces, etc.)
    private static final Map<ScriptHost, Map<String, ScarpetClass>> declaredClasses = new HashMap<>();

    public static void addNewClassDef(Context c, String className, Map<Value, Value> members, List<String> parents) {
        ScriptHost host = c.host;

        Map<Value, Value> config = ((CarpetScriptHost) host).appConfig;
        boolean hasDefaultClasses = config.getOrDefault(StringValue.of(defaultClassConfigInclude), Value.FALSE).getBoolean();

        if (!declaredClasses.containsKey(host)) {//Initialising class-based stuff for this app
            declaredClasses.put(host, new HashMap<>());

            //If user wants default classes in config
            if (hasDefaultClasses) {
                Module defaultClasses = Module.fromJarPath("assets/scarpetclasses/scripts/", defaultClassScriptName, true);

                //Repurposed code from ScriptHost#importModule(Context c, String moduleName)
                ((ScriptHostMixin) host).getModules().put(defaultClasses.name(), defaultClasses);
                ScriptHost.ModuleData data = new ScriptHost.ModuleData(defaultClasses);
                //host.initializeModuleGlobals(data); This is unnecessary, but keeping it here in case it causes problems and needs to be done
                ((ScriptHostMixin) host).getModuleData().put(defaultClasses, data);

                //Repurposed code from CarpetScriptHost#runModuleCode(Context c, Module module)
                CarpetContext cc = (CarpetContext) c;
                CarpetExpression ex = new CarpetExpression(defaultClasses, defaultClasses.code(), cc.source(), cc.origin());
                ex.getExpr().asATextSource();
                ex.scriptRunCommand(host, cc.origin());
            }
        }
        Map<String, ScarpetClass> hostClasses = declaredClasses.get(host);

        if (hostClasses.containsKey(className)) //todo possible add overwrite option?
            throw new InternalExpressionException("Already defined class '" + className + "'");

        // Having default classes means having inheritance
        if (!hasDefaultClasses) {
            if (!parents.isEmpty())
                throw new InternalExpressionException("Cannot have inheritance without including " + defaultClassConfigInclude + " as 'true' in config");

            hostClasses.put(className, new ScarpetClass(className, members));

        } else {
            Set<ScarpetClass> parentClasses = new HashSet<>();
            for (String parent : parents) {
                if (!hostClasses.containsKey(parent))
                    throw new InternalExpressionException("Cannot inherit from unknown class '" + parent + "'");
                parentClasses.add(hostClasses.get(parent));
            }
            hostClasses.put(className, new ScarpetClass(className, members, parentClasses));
        }
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

            if (!classes.isEmpty()) {
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
}
