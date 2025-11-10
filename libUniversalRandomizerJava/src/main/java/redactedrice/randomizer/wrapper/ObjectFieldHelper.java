package redactedrice.randomizer.wrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Used to expose field names to lua so it knows what to track.
// otherwise it might try to track functions as well or other things
// we don't want it to
public class ObjectFieldHelper {
    public static List<String> getFieldNames(Object obj) {
        if (obj == null) {
            return new ArrayList<>();
        }

        return getFieldNames(obj.getClass());
    }

    public static List<String> getFieldNames(Class<?> clazz) {
        if (clazz == null) {
            return new ArrayList<>();
        }

        Set<String> fieldNames = new LinkedHashSet<>();
        Class<?> currentClass = clazz;
        // Start at the base class and walk up the hierarchy
        // TODO: Should we have a start and stop class or can we just walk all the way up?
        while (currentClass != null && currentClass != Object.class) {
            Field[] declaredFields = currentClass.getDeclaredFields();

            for (Field field : declaredFields) {
                int modifiers = field.getModifiers();

                // skip static transient and synthetic fields
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)
                        || field.isSynthetic()) {
                    continue;
                }

                fieldNames.add(field.getName());
            }

            // Continue up the hierarchy
            currentClass = currentClass.getSuperclass();
        }

        return new ArrayList<>(fieldNames);
    }
}

