package csci699cav;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

// tag indicating that a method has been instrumented
public class InstrumentedTag implements Tag {
    public static final String NAME = "InstrumentedTag";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] getValue() throws AttributeValueException {
        return null;
    }
}
