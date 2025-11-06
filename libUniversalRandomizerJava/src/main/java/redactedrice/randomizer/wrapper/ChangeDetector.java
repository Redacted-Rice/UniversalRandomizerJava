package redactedrice.randomizer.wrapper;

import java.lang.reflect.Method;
import java.util.*;

// detects changes in java objects by comparing before and after snapshots
public class ChangeDetector {
    static class ObjectSnapshot {
        Object object;
        Map<String, Object> fieldValues;

        public ObjectSnapshot(Object object) {
            this.object = object;
            this.fieldValues = captureState(object);
        }

        private Map<String, Object> captureState(Object obj) {
            if (obj == null) {
                return new LinkedHashMap<>();
            }

            // use trackable interface if object implements it
            if (obj instanceof Trackable) {
                return new LinkedHashMap<>(((Trackable) obj).getTrackableState());
            }

            // otherwise use reflection to find getters
            return captureStateViaReflection(obj);
        }

        private Map<String, Object> captureStateViaReflection(Object obj) {
            Map<String, Object> state = new LinkedHashMap<>();
            Class<?> clazz = obj.getClass();

            // look for getter methods
            for (Method method : clazz.getMethods()) {
                String methodName = method.getName();

                // check if its a getter method
                if ((methodName.startsWith("get") || methodName.startsWith("is"))
                        && method.getParameterCount() == 0
                        && method.getReturnType() != void.class) {
                    try {
                        Object value = method.invoke(obj);

                        // extract field name from getter name
                        String fieldName;
                        if (methodName.startsWith("is")) {
                            fieldName = methodName.substring(2);
                        } else {
                            fieldName = methodName.substring(3);
                        }

                        // lowercase first char of field name
                        if (fieldName.length() > 0) {
                            fieldName = Character.toLowerCase(fieldName.charAt(0))
                                    + fieldName.substring(1);
                        }

                        state.put(fieldName, value);
                    } catch (Exception e) {
                        // skip if getter fails
                    }
                }
            }

            return state;
        }

        public Map<String, String> detectChanges() {
            Map<String, String> changes = new LinkedHashMap<>();
            Map<String, Object> currentState = captureState(object);

            // compare old values to new values
            for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
                String fieldName = entry.getKey();
                Object oldValue = entry.getValue();
                Object newValue = currentState.get(fieldName);

                if (!Objects.equals(oldValue, newValue)) {
                    changes.put(fieldName, formatChange(oldValue, newValue));
                }
            }

            return changes;
        }

        private String formatChange(Object oldValue, Object newValue) {
            return String.format("%s -> %s", oldValue != null ? oldValue.toString() : "null",
                    newValue != null ? newValue.toString() : "null");
        }

        public Object getObject() {
            return object;
        }
    }

    List<ObjectSnapshot> snapshots;
    boolean enabled;

    public ChangeDetector() {
        this.snapshots = new ArrayList<>();
        this.enabled = true;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void takeSnapshots(List<Object> objects) {
        snapshots.clear();

        if (!enabled || objects == null) {
            return;
        }

        for (Object obj : objects) {
            if (obj != null) {
                snapshots.add(new ObjectSnapshot(obj));
            }
        }
    }

    public Map<String, Map<String, String>> detectChanges() {
        Map<String, Map<String, String>> allChanges = new LinkedHashMap<>();

        if (!enabled) {
            return allChanges;
        }

        // check each snapshot for changes
        for (ObjectSnapshot snapshot : snapshots) {
            Map<String, String> changes = snapshot.detectChanges();

            if (!changes.isEmpty()) {
                // use simple class name for the key
                String objectKey = snapshot.getObject().getClass().getSimpleName();

                // make key unique if needed by adding a number
                int counter = 1;
                String uniqueKey = objectKey;
                while (allChanges.containsKey(uniqueKey)) {
                    uniqueKey = objectKey + counter;
                    counter++;
                }

                allChanges.put(uniqueKey, changes);
            }
        }

        return allChanges;
    }

    public String formatAllChanges(Map<String, Map<String, String>> changes) {
        if (changes.isEmpty()) {
            return "No changes detected";
        }

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Map<String, String>> entry : changes.entrySet()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }

            sb.append(entry.getKey()).append(": {");

            boolean first = true;
            for (Map.Entry<String, String> fieldChange : entry.getValue().entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(fieldChange.getKey()).append(": ").append(fieldChange.getValue());
                first = false;
            }

            sb.append("}");
        }

        return sb.toString();
    }

    public void clear() {
        snapshots.clear();
    }
}
