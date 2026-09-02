package redactedrice.randomizer;

import java.util.LinkedHashMap;
import java.util.Map;

import redactedrice.randomizer.example.ExampleEntityType;

// example entity with private fields and public getters setters
public class ExampleEntity {

    public static class Tag {
        private String label = "";

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Tag copy() {
            Tag copy = new Tag();
            copy.label = label;
            return copy;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Tag tag && label.equals(tag.label);
        }

        @Override
        public int hashCode() {
            return label.hashCode();
        }
    }

    String name;
    ExampleEntityType type;
    String startingItem;

    int health;
    double damage;
    int speed;
    int defense;

    private int numTags;
    private final Tag[] tags = { new Tag(), new Tag(), new Tag() };
    private int rankCounts;
    private final Tag[] ranks = { new Tag(), new Tag(), new Tag() };
    private final Map<String, Integer> perkRanks = new LinkedHashMap<>();

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

    public int getNumTags() {
        return numTags;
    }

    public void setNumTags(int numTags) {
        this.numTags = numTags;
    }

    public Tag getTag(int index) {
        return tags[index].copy();
    }

    public boolean setTag(Tag tag, int index, boolean force) {
        if (!force && tags[index].equals(tag)) {
            return false;
        }
        tags[index] = tag.copy();
        return true;
    }

    public int getRankCounts() {
        return rankCounts;
    }

    public void setRankCounts(int rankCounts) {
        this.rankCounts = rankCounts;
    }

    public Tag getAtRank(int index) {
        return ranks[index].copy();
    }

    public boolean setAtRank(Tag rank, int index, boolean force) {
        if (!force && ranks[index].equals(rank)) {
            return false;
        }
        ranks[index] = rank.copy();
        return true;
    }

    public void clearPerkRanks() {
        perkRanks.clear();
    }

    public void setPerkRank(String perk, int rank) {
        perkRanks.put(perk, rank);
    }

    public int getPerkRank(String perk) {
        return perkRanks.getOrDefault(perk, 0);
    }

    public ExampleEntity copy() {
        ExampleEntity copy = new ExampleEntity(name, type, health, damage, speed, defense);
        copy.startingItem = this.startingItem;
        copy.numTags = this.numTags;
        for (int i = 0; i < tags.length; i++) {
            copy.tags[i] = this.tags[i].copy();
        }
        copy.rankCounts = this.rankCounts;
        for (int i = 0; i < ranks.length; i++) {
            copy.ranks[i] = this.ranks[i].copy();
        }
        copy.perkRanks.putAll(this.perkRanks);
        return copy;
    }

    @Override
    public String toString() {
        return String.format(
                "ExampleEntity{name='%s', health=%d, damage=%.2f, speed=%d, defense=%d, type=%s, startingItem='%s'}",
                name, health, damage, speed, defense, type, startingItem);
    }
}
