package redactedrice.randomizer;

// example item with public fields
public class ExampleItem {
    public enum ItemRarity {
        COMMON, UNCOMMON, RARE, LEGENDARY
    }

    public String name;
    public ItemRarity rarity;
    public int attackBonus;
    public int defenseBonus;
    public int healthBonus;
    public int speedBonus;

    public ExampleItem(String name, ItemRarity rarity, int attackBonus, int defenseBonus,
            int healthBonus, int speedBonus) {
        this.name = name;
        this.rarity = rarity;
        this.attackBonus = attackBonus;
        this.defenseBonus = defenseBonus;
        this.healthBonus = healthBonus;
        this.speedBonus = speedBonus;
    }

    public ExampleItem copy() {
        return new ExampleItem(name, rarity, attackBonus, defenseBonus, healthBonus, speedBonus);
    }

    @Override
    public String toString() {
        return String.format(
                "ExampleItem{name='%s', rarity=%s, attack=%+d, defense=%+d, health=%+d, speed=%+d}",
                name, rarity, attackBonus, defenseBonus, healthBonus, speedBonus);
    }
}
