package redactedrice.randomizer.wrapper;

/**
 * Functional interface for providing custom string identifiers for objects in change detection.
 * This allows objects to be identified by meaningful names (e.g., "cardname (CARD_ID)").
 */
@FunctionalInterface
public interface ObjectIdentifier {
    /**
     * Generate a string identifier for the given object.
     *
     * @param obj The object to identify
     * @return A string identifier for the object
     */
    String identify(Object obj);
}

