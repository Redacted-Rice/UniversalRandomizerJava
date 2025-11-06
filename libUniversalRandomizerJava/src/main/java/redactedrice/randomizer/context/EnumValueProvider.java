package redactedrice.randomizer.context;

// interface for enums that provide integer values via a method instead of ordinal
public interface EnumValueProvider
{
	// get the integer value for this enum constant
	int getIntValue();
}
