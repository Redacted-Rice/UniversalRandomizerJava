package redactedrice.randomizer.wrapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

// Tests for change detector
// verifies change detection via reflection and trackable interface
public class ChangeDetectorTest {

    private ChangeDetector detector;

    @BeforeEach
    public void setUp() {
        detector = new ChangeDetector();
    }

    static class TestEntity {
        private String name;
        private int health;

        public TestEntity(String name, int health) {
            this.name = name;
            this.health = health;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getHealth() {
            return health;
        }

        public void setHealth(int health) {
            this.health = health;
        }
    }

    static class TrackableEntity implements Trackable {
        private String name;
        private int value;

        public TrackableEntity(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> getTrackableState() {
            Map<String, Object> state = new HashMap<>();
            state.put("name", name);
            state.put("value", value);
            return state;
        }
    }

    @Test
    public void testChangeDetectionEnabled() {
        assertTrue(detector.isEnabled());
        detector.setEnabled(false);
        assertFalse(detector.isEnabled());
    }

    @Test
    public void testDetectChangesViaReflection() {
        detector.setEnabled(true);
        TestEntity entity = new TestEntity("Test", 100);

        detector.takeSnapshots(Arrays.asList(entity));
        entity.setName("Changed");
        entity.setHealth(200);

        Map<String, Map<String, String>> changes = detector.detectChanges();
        assertFalse(changes.isEmpty());
    }

    @Test
    public void testTrackableInterface() {
        detector.setEnabled(true);
        TrackableEntity entity = new TrackableEntity("Test", 100);

        detector.takeSnapshots(Arrays.asList(entity));
        entity.setName("Changed");
        entity.setValue(200);

        Map<String, Map<String, String>> changes = detector.detectChanges();
        assertFalse(changes.isEmpty());
    }

    @Test
    public void testNoChangesWhenDisabled() {
        detector.setEnabled(false);
        TestEntity entity = new TestEntity("Test", 100);

        detector.takeSnapshots(Arrays.asList(entity));
        entity.setName("Changed");

        Map<String, Map<String, String>> changes = detector.detectChanges();
        assertTrue(changes.isEmpty());
    }

    @Test
    public void testNoChangesWhenUnmodified() {
        detector.setEnabled(true);
        TestEntity entity = new TestEntity("Test", 100);

        detector.takeSnapshots(Arrays.asList(entity));
        Map<String, Map<String, String>> changes = detector.detectChanges();
        assertTrue(changes.isEmpty());
    }

    @Test
    public void testFormatAllChanges() {
        detector.setEnabled(true);
        TestEntity entity = new TestEntity("Test", 100);

        detector.takeSnapshots(Arrays.asList(entity));
        entity.setName("Changed");
        entity.setHealth(200);

        Map<String, Map<String, String>> changes = detector.detectChanges();
        String formatted = detector.formatAllChanges(changes);
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
    }

    @Test
    public void testClear() {
        detector.setEnabled(true);
        TestEntity entity = new TestEntity("Test", 100);

        detector.takeSnapshots(Arrays.asList(entity));
        detector.clear();

        Map<String, Map<String, String>> changes = detector.detectChanges();
        assertTrue(changes.isEmpty());
    }

    @Test
    public void testMultipleObjects() {
        detector.setEnabled(true);
        TestEntity entity1 = new TestEntity("Entity1", 100);
        TestEntity entity2 = new TestEntity("Entity2", 200);

        detector.takeSnapshots(Arrays.asList(entity1, entity2));
        entity1.setName("Changed1");
        entity2.setHealth(300);

        Map<String, Map<String, String>> changes = detector.detectChanges();
        assertFalse(changes.isEmpty());
    }

    @Test
    public void testNullHandling() {
        detector.setEnabled(true);
        detector.takeSnapshots(Arrays.asList((Object) null));
        // should not crash
        Map<String, Map<String, String>> changes = detector.detectChanges();
        assertTrue(changes.isEmpty());
    }
}

