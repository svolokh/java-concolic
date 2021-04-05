package csci699cav;

public class PathConstraint {
    public int branchId;
    public String condition;
    public boolean conditionConcrete;

    public PathConstraint(int branchId, String condition, boolean conditionConcrete) {
        this.branchId = branchId;
        this.condition = condition;
        this.conditionConcrete = conditionConcrete;
    }
}