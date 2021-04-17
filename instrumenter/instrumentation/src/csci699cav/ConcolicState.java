package csci699cav;

import java.util.*;

public class ConcolicState {
    protected static int inputCounter = 0;
    protected static Random rng = new Random();

    protected static List<Variable> inputVariables = new ArrayList<>();
    protected static List<Variable> variables = new ArrayList<>();
    protected static List<Assignment> inputAssignments = new ArrayList<>();
    protected static List<Assignment> assignments = new ArrayList<>();
    protected static List<PathConstraint> pathConstraints = new ArrayList<>();

    private static Set<Integer> declaredVariables = new HashSet<Integer>();

    private static Stack<String> frameStack = new Stack<String>();
    private static String lastFrame = null;
    private static int frameCounter = 0;

    public static void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));
    }

    public static String lastInput()
    {
        return "INPUT" + (inputCounter-1);
    }

    public static void addInput(VariableType type, String value) {
        String varName = "INPUT" + inputCounter;
        inputVariables.add(new Variable(type, varName));
        inputAssignments.add(new Assignment(varName, value));
        ++inputCounter;
    }

    public static String local(String name) {
        String currentFrame = frameStack.peek();
        return name + "_" + currentFrame;
    }

    // variable used to store return value for current frame
    public static String retVar() {
        String currentFrame = frameStack.peek();
        return "RET_" + currentFrame;
    }

    public static String unaryOp(String symbol, String localOrConstantOp, boolean opConstant) {
        String op = opConstant ? localOrConstantOp : local(localOrConstantOp);
        return symbol + op;
    }

    public static String binaryOp(String symbol, String localOrConstantOp1, boolean op1Constant, String localOrConstantOp2, boolean op2Constant) {
        String leftOp = op1Constant ? localOrConstantOp1 : local(localOrConstantOp1);
        String rightOp = op2Constant ? localOrConstantOp2 : local(localOrConstantOp2);
        return leftOp + symbol + rightOp;
    }

    public static String peekNextFrame(String fn) {
        return fn + (frameCounter + 1);
    }

    public static void newFrame(String fn) {
        ++frameCounter;
        frameStack.push(fn + frameCounter);
    }

    public static void exitFrame() {
        lastFrame = frameStack.pop();
    }

    public static void addVariable(VariableType type, String id) {
        variables.add(new Variable(type, id));
    }

    // key is used to quickly check if the variable has already been declared
    public static void addVariableIfNotPresent(VariableType type, String id, int key) {
        if (declaredVariables.add(key)) {
            variables.add(new Variable(type, id));
        }
    }

    public static void addAssignment(String leftOp, String rightOp) {
        assignments.add(new Assignment(leftOp, rightOp));
    }

    // call this before entering the callee
    public static void addAssignmentToParameter(String paramName, String fn, String rightOp) {
        addAssignment(paramName + "_" + peekNextFrame(fn), rightOp);
    }

    // call this in the caller right after the invocation
    public static void addAssignmentFromReturnValue(String localVarLeftOp) {
        addAssignment(localVarLeftOp, "RET_" + lastFrame);
    }

    // call this in the callee before exiting
    public static void addAssignmentToReturnValue(String rightOp) {
        String currentFrame = frameStack.peek();
        assignments.add(new Assignment("RET_" + currentFrame, rightOp));
    }

    public static void addPathConstraint(int branchId, String condition, boolean conditionConcrete) {
        pathConstraints.add(new PathConstraint(branchId, condition, conditionConcrete, assignments.size()));
    }
}