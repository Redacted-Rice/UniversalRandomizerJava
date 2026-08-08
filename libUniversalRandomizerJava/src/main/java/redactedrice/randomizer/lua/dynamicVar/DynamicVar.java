package redactedrice.randomizer.lua.dynamicVar;

import java.util.Locale;
import java.util.Objects;

/**
 * Named dynamic variable declared in module provides or needs metadata. URJ
 * does not read or write
 * the runtime value. modules set and read values in Lua. this type only drives
 * dependency
 * validation so names and types line up across providers and consumers. Type
 * matching is
 * case insensitive. The declared spelling is preserved for display.
 */
public final class DynamicVar {
    private final String name;
    private final String type;

    public DynamicVar(String name, String type) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Dynamic var name cannot be null or empty");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Dynamic var type cannot be null or empty");
        }
        this.name = name.trim();
        this.type = type.trim();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean typesMatch(String otherType) {
        return otherType != null && type.equalsIgnoreCase(otherType.trim());
    }

    public boolean satisfiesNeed(DynamicVar need) {
        return need != null && name.equals(need.name) && typesMatch(need.type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DynamicVar that = (DynamicVar) o;
        return name.equals(that.name) && typesMatch(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type.toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
