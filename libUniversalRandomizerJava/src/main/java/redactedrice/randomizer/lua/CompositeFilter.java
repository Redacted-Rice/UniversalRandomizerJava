package redactedrice.randomizer.lua;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Combines multiple filters - module must pass all filters to be accepted
public class CompositeFilter implements ModuleFilter {
    private final List<ModuleFilter> filters;

    public CompositeFilter(ModuleFilter... filters) {
        this.filters = new ArrayList<>(Arrays.asList(filters));
    }

    public CompositeFilter(List<ModuleFilter> filters) {
        this.filters = new ArrayList<>(filters);
    }

    @Override
    public boolean accepts(Module module) {
        for (ModuleFilter filter : filters) {
            if (!filter.accepts(module)) {
                return false;
            }
        }
        return true;
    }
}
