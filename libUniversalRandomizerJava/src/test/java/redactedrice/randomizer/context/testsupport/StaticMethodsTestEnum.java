package redactedrice.randomizer.context.testsupport;

public enum StaticMethodsTestEnum {
    A, B;

    public static String join(StaticMethodsTestEnum left, StaticMethodsTestEnum right) {
        return left.name() + "+" + right.name();
    }

    public static String tag(StaticMethodsTestEnum value) {
        if (value == null) {
            return "nil";
        }
        return "@" + value.name();
    }
}
