package redactedrice.randomizer.example;

public enum ItemRarity {
    COMMON, UNCOMMON, RARE, LEGENDARY;

    public static String displayLabel(ItemRarity rarity) {
        if (rarity == null) {
            return "Unknown";
        }
        return rarity.name().substring(0, 1) + rarity.name().substring(1).toLowerCase();
    }
}
