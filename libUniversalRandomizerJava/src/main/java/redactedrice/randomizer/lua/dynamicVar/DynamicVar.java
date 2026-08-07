package redactedrice.randomizer.lua.dynamicVar;

import java.util.Objects;

/** Named dynamic variable declared by module provides or needs metadata. */
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

    public boolean satisfiesNeed(DynamicVar need) {
        return need != null && name.equals(need.name) && type.equals(need.type);
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
        return name.equals(that.name) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
