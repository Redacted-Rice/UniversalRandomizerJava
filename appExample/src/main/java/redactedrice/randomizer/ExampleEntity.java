package redactedrice.randomizer;

import redactedrice.randomizer.example.ExampleEntityType;

// example entity with private fields and public getters setters
public class ExampleEntity {

    String name;
    ExampleEntityType type;
    String startingItem;

    int health;
    double damage;
    int speed;
    int defense;

    public ExampleEntity(String name) {
        this("Unnamed", ExampleEntityType.WARRIOR, 100, 10.0, 10, 10);
    }

    public ExampleEntity(String name, ExampleEntityType type, int health, double damage, int speed,
            int defense) {
        this.name = name;
        this.type = type;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.defense = defense;
        this.startingItem = "";
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

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getDefense() {
        return defense;
    }

    public void setDefense(int defense) {
        this.defense = defense;
    }

    public ExampleEntityType getType() {
        return type;
    }

    public void setType(ExampleEntityType type) {
        this.type = type;
    }

    public String getStartingItem() {
        return startingItem;
    }

    public void setStartingItem(String startingItem) {
        this.startingItem = startingItem;
    }

    public ExampleEntity copy() {
        ExampleEntity copy = new ExampleEntity(name, type, health, damage, speed, defense);
        copy.startingItem = this.startingItem;
        return copy;
    }

    @Override
    public String toString() {
        return String.format(
                "ExampleEntity{name='%s', health=%d, damage=%.2f, speed=%d, defense=%d, type=%s, startingItem='%s'}",
                name, health, damage, speed, defense, type, startingItem);
    }
}
