package redactedrice.support.test;

// Simple test entity class for testing change detection and Lua interaction. Has private fields
// with getters/setters to test reflection-based change tracking
public class TestEntity {
    private String name;
    private int health;
    private double damage;
    private boolean isActive;

    public TestEntity() {
        this("Unnamed", 100, 10.0, true);
    }

    public TestEntity(String name, int health, double damage, boolean isActive) {
        this.name = name;
        this.health = health;
        this.damage = damage;
        this.isActive = isActive;
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

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return String.format("TestEntity{name='%s', health=%d, damage=%.2f, isActive=%s}", name,
                health, damage, isActive);
    }

    public TestEntity copy() {
        return new TestEntity(name, health, damage, isActive);
    }
}
