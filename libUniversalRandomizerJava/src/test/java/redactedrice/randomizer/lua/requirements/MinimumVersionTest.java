package redactedrice.randomizer.lua.requirements;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MinimumVersionTest {
    @Test
    void currentVersionSatisfiesMinimumWhenEqual() {
        assertTrue(RequirementValidator.satisfiesMinimumVersion("1.0.0", "1.0.0"));
    }

    @Test
    void currentVersionSatisfiesMinimumWhenGreater() {
        assertTrue(RequirementValidator.satisfiesMinimumVersion("0.2.0", "0.1.0"));
        assertTrue(RequirementValidator.satisfiesMinimumVersion("0.5.0", "0.4.0"));
        assertTrue(RequirementValidator.satisfiesMinimumVersion("1.0.0", "0.9.0"));
    }

    @Test
    void currentVersionFailsWhenBelowMinimum() {
        assertFalse(RequirementValidator.satisfiesMinimumVersion("0.1.0", "0.2.0"));
        assertFalse(RequirementValidator.satisfiesMinimumVersion("0.9.9", "1.0.0"));
    }

    @Test
    void shorterVersionsAreComparedWithImplicitZeroSegments() {
        assertTrue(RequirementValidator.satisfiesMinimumVersion("1.0.0", "1.0"));
        assertTrue(RequirementValidator.satisfiesMinimumVersion("1.0", "1.0.0"));
        assertTrue(RequirementValidator.satisfiesMinimumVersion("1.0.1", "1"));
    }

    @Test
    void invalidVersionsFailComparison() {
        assertFalse(RequirementValidator.satisfiesMinimumVersion("1.0.0", ""));
        assertFalse(RequirementValidator.satisfiesMinimumVersion("1.a.0", "1.0.0"));
        assertThrows(IllegalArgumentException.class,
                () -> RequirementValidator.parseVersionParts(""));
    }
}
