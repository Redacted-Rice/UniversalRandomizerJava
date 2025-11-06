package redactedrice.randomizer.wrapper;

import java.util.Map;

// interface for objects that expose their state for change tracking
// avoids using reflection by letting objects directly provide their state
public interface Trackable {
    // get the current state as a map of field names to values
    Map<String, Object> getTrackableState();
}

