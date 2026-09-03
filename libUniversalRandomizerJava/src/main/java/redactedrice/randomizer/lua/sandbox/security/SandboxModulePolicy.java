package redactedrice.randomizer.lua.sandbox.security;


import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SandboxModulePolicy {
    public static final Set<String> REMOVED_MODULES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("io", "os", "luajava")));
    public static final Set<String> MODIFIED_MODULES =
            Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("debug")));

    private SandboxModulePolicy() {}

    public static Set<String> blockedModulesForRequire() {
        Set<String> blocked = new HashSet<>(REMOVED_MODULES);
        blocked.addAll(MODIFIED_MODULES);
        return blocked;
    }
}
