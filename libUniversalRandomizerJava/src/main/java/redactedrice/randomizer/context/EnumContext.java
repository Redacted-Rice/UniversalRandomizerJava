package redactedrice.randomizer.context;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// manages enum registrations and provides them to lua context
public class EnumContext {
    Map<String, EnumDefinition> enums;

    public EnumContext() {
        this.enums = new ConcurrentHashMap<>();
    }

    public <E extends Enum<E>> void registerEnum(Class<E> enumClass) {
        registerEnum(enumClass.getSimpleName(), enumClass);
    }

    public <E extends Enum<E>> void registerEnum(String name, Class<E> enumClass) {
        if (enumClass == null) {
            throw new IllegalArgumentException("Enum class cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum name cannot be null or empty");
        }

        E[] constants = enumClass.getEnumConstants();

        List<String> values = new ArrayList<>();
        Map<String, Integer> valueMap = new HashMap<>();

        // check if enum implements EnumValueProvider (avoids reflection)
        boolean implementsValueProvider = EnumValueProvider.class.isAssignableFrom(enumClass);

        // try to find a method to get integer values (only if not implementing EnumValueProvider)
        Method valueMethod = null;
        if (!implementsValueProvider) {
            // look for getValue first
            try {
                valueMethod = enumClass.getMethod("getValue");
            } catch (NoSuchMethodException e) {
                // try value if getValue doesnt exist
                try {
                    valueMethod = enumClass.getMethod("value");
                } catch (NoSuchMethodException e2) {
                    // will use ordinal as fallback
                }
            }
        }

        // extract all enum values and their integer mappings
        for (E constant : constants) {
            String enumName = constant.name();
            values.add(enumName);

            int intValue;
            if (implementsValueProvider) {
                // use interface directly (no reflection needed)
                intValue = ((EnumValueProvider) constant).getIntValue();
            } else if (valueMethod != null) {
                // use reflection as fallback
                try {
                    Object result = valueMethod.invoke(constant);
                    if (result instanceof Number) {
                        intValue = ((Number) result).intValue();
                    } else {
                        intValue = constant.ordinal();
                    }
                } catch (Exception e) {
                    intValue = constant.ordinal();
                }
            } else {
                // Use ordinal as final fallback
                intValue = constant.ordinal();
            }
            valueMap.put(enumName, intValue);
        }

        enums.put(name, new EnumDefinition(name, values, valueMap, enumClass));
    }

    public void registerEnum(String name, List<String> values) {
        registerEnum(name, values, null);
    }

    public void registerEnum(String name, List<String> values, Map<String, Integer> valueMap) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum name cannot be null or empty");
        }
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Enum values cannot be null or empty");
        }

        enums.put(name, new EnumDefinition(name, new ArrayList<>(values), valueMap, null));
    }

    public void registerEnum(String name, Map<String, Integer> valueMap) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Enum name cannot be null or empty");
        }
        if (valueMap == null || valueMap.isEmpty()) {
            throw new IllegalArgumentException("Enum value map cannot be null or empty");
        }

        // Preserve insertion order if possible (LinkedHashMap), otherwise use sorted keys
        List<String> values;
        if (valueMap instanceof LinkedHashMap) {
            values = new ArrayList<>(valueMap.keySet());
        } else {
            // For non-ordered maps, sort keys for deterministic order
            values = new ArrayList<>(valueMap.keySet());
            Collections.sort(values);
        }
        // Preserve the valueMap with its order (use LinkedHashMap if input was LinkedHashMap)
        Map<String, Integer> orderedValueMap =
                (valueMap instanceof LinkedHashMap) ? new LinkedHashMap<>(valueMap)
                        : new HashMap<>(valueMap);
        enums.put(name, new EnumDefinition(name, values, orderedValueMap, null));
    }

    public void mergeFrom(EnumContext source) {
        if (source != null) {
            for (String enumName : source.getEnumNames()) {
                EnumDefinition enumDef = source.getEnum(enumName);
                if (enumDef != null) {
                    enums.put(enumName, enumDef);
                }
            }
        }
    }

    public Object stringToEnum(String enumName, String valueName) {
        EnumDefinition enumDef = enums.get(enumName);
        if (enumDef == null || enumDef.getEnumClass() == null) {
            return null;
        }

        Class<? extends Enum<?>> enumClass = enumDef.getEnumClass();
        try {
            // Use raw type for Enum.valueOf which requires Class<T extends Enum<T>>
            @SuppressWarnings({"unchecked", "rawtypes"})
            Enum enumValue = Enum.valueOf((Class) enumClass, valueName);
            return enumValue;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean hasEnum(String name) {
        return enums.containsKey(name);
    }

    public EnumDefinition getEnum(String name) {
        return enums.get(name);
    }

    public Set<String> getEnumNames() {
        return Collections.unmodifiableSet(enums.keySet());
    }

    public boolean isValidEnumValue(String enumName, String value) {
        EnumDefinition enumDef = enums.get(enumName);
        if (enumDef == null) {
            return false;
        }
        return enumDef.hasValue(value);
    }

    public Map<String, LuaTable> toLuaTables() {
        Map<String, LuaTable> luaEnums = new HashMap<>();

        for (Map.Entry<String, EnumDefinition> entry : enums.entrySet()) {
            LuaTable enumTable = new LuaTable();
            EnumDefinition enumDef = entry.getValue();
            List<String> values = enumDef.getValues();
            Map<String, Integer> valueMap = enumDef.getValueMap();

            // Create sequential array of strings
            for (int i = 0; i < values.size(); i++) {
                String value = values.get(i);
                enumTable.set(i + 1, LuaValue.valueOf(value));
            }

            // Create values subtable mapping name -> integer value
            if (!valueMap.isEmpty()) {
                LuaTable valuesTable = new LuaTable();
                for (Map.Entry<String, Integer> valueEntry : valueMap.entrySet()) {
                    valuesTable.set(valueEntry.getKey(), LuaValue.valueOf(valueEntry.getValue()));
                }
                enumTable.set("values", valuesTable);
            }

            // Add metadata
            enumTable.set("_name", LuaValue.valueOf(entry.getKey()));

            // Make the table read-only (best effort in LuaJ)
            enumTable.setmetatable(createReadOnlyMetatable());

            luaEnums.put(entry.getKey(), enumTable);
        }

        return luaEnums;
    }

    private LuaTable createReadOnlyMetatable() {
        LuaTable mt = new LuaTable();
        // Prevent modifications
        mt.set("__newindex", LuaValue.valueOf("Enums are read-only"));
        return mt;
    }

}
