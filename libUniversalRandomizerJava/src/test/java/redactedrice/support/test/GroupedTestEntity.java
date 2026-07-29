package redactedrice.support.test;

// Simple entity with a category field for table of lists group randomization tests
public class GroupedTestEntity {
    private final String category;
    private int health;

    public GroupedTestEntity(String category, int health) {
        this.category = category;
        this.health = health;
    }

    public String getCategory() {
        return category;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }
}
