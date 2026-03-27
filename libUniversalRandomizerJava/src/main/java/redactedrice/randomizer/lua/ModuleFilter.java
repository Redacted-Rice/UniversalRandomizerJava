package redactedrice.randomizer.lua;

// Filter interface for accepting/rejecting modules
public interface ModuleFilter {
    boolean accepts(Module module);
}
