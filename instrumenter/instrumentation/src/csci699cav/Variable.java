package csci699cav;

public class Variable {
    public VariableType type;
    public String id;
    public boolean instanceArray;

    public Variable(VariableType type, String id) {
        this.type = type;
        this.id = id;
        this.instanceArray = false;
    }

    public Variable(VariableType type, String id, boolean instanceArray) {
        this.type = type;
        this.id = id;
        this.instanceArray = instanceArray;
    }
}