package redactedrice.support.test;

import redactedrice.randomizer.context.testsupport.TestEnergyType;

public class EnumFieldTestCard {
    private final String name;
    private TestEnergyType type;

    public EnumFieldTestCard(String name, TestEnergyType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public TestEnergyType getType() {
        return type;
    }

    public void setType(TestEnergyType type) {
        this.type = type;
    }
}
