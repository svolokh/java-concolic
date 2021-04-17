package csci699cav;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class BranchIdTag implements Tag {
    public static final String NAME = "BranchIdTag";

    public final int id;

    public BranchIdTag(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] getValue() throws AttributeValueException {
        return null;
    }
}
