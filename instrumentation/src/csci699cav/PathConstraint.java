package csci699cav;

public class PathConstraint {
    public int branchId;
    public String condition;
    public boolean conditionConcrete;
    public int assignmentIndex; // index after the last assignment when this condition was checked

    public PathConstraint(int branchId, String condition, boolean conditionConcrete, int assignmentIndex) {
        this.branchId = branchId;
        this.condition = condition;
        this.conditionConcrete = conditionConcrete;
        this.assignmentIndex = assignmentIndex;
    }
}