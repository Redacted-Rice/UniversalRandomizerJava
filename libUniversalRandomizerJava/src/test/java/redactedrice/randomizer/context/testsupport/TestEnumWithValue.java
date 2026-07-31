package redactedrice.randomizer.context.testsupport;

import redactedrice.randomizer.context.EnumValueProvider;


public enum TestEnumWithValue implements EnumValueProvider {
    LOW(1), MEDIUM(10), HIGH(100);

    private final int value;

    TestEnumWithValue(int value) {
        this.value = value;
    }

    @Override
    public int getIntValue() {
        return value;
    }
}
