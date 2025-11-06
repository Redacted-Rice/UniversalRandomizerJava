package redactedrice.support.test;

/**
 * Test enum with integer flag values for testing enum value extraction.
 */
public enum FlagEnum {
    FLAG_NONE(0), FLAG_ONE(1), FLAG_TWO(2), FLAG_FOUR(4), FLAG_EIGHT(8);

    private final int value;

    FlagEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
