package csci699cav;

import java.util.*;

public class ConcolicState {
    protected static int inputCounter = 0;
    protected static Random rng = new Random();

    protected static List<Variable> inputVariables = new LinkedList<>();
    protected static List<Variable> variables = new LinkedList<>();
    protected static List<Assignment> inputAssignments = new LinkedList<>();
    protected static List<Assignment> fieldInitAssignments = new LinkedList<>();
    protected static List<Assignment> assignments = new LinkedList<>();
    protected static List<PathConstraint> pathConstraints = new LinkedList<>();

    private static Set<Integer> declaredVariables = new HashSet<Integer>();

    private static Stack<String> frameStack = new Stack<String>();
    private static String lastFrame = null;
    private static int frameCounter = 0;
    private static int objectCounter = 0;
    private static int arrayCounter = 0;

    private static String defaultValue(VariableType type) {
        switch(type) {
            case BYTE:
                return "BitVecVal(0, 8)";
            case SHORT:
                return "BitVecVal(0, 16)";
            case INT:
                return "BitVecVal(0, 32)";
            case LONG:
                return "BitVecVal(0, 64)";
            case FLOAT:
                return "FPVal(0, Float)";
            case DOUBLE:
                return "FPVal(0, Float)";
            case CHAR:
                return "BitVecVal(0, 16)";
            default:
                return null;
        }
    }

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

    public static String identity(String localOrConstantOp, boolean opConstant) {
        String op = opConstant ? localOrConstantOp : local(localOrConstantOp);
        return op;
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

    public static String cmp(String localOrConstantOp1, boolean op1Constant, String localOrConstantOp2, boolean op2Constant) {
        String leftOp = op1Constant ? localOrConstantOp1 : local(localOrConstantOp1);
        String rightOp = op2Constant ? localOrConstantOp2 : local(localOrConstantOp2);
        return "If(" + leftOp + " > " + rightOp + ", BitVecVal(1, 8), If(" + leftOp + " == " + rightOp + ", BitVecVal(0, 8), BitVecVal(-1, 8)))";
    }

    public static String cmpl(String localOrConstantOp1, boolean op1Constant, String localOrConstantOp2, boolean op2Constant) {
        String leftOp = op1Constant ? localOrConstantOp1 : local(localOrConstantOp1);
        String rightOp = op2Constant ? localOrConstantOp2 : local(localOrConstantOp2);
        return "If(Or(fpIsNaN(" + leftOp + "), fpIsNaN(" + rightOp + ")), BitVecVal(-1, 8), If(" + leftOp + " > " + rightOp + ", BitVecVal(1, 8), If(" + leftOp + " == " + rightOp + ", BitVecVal(0, 8), BitVecVal(-1, 8))))";
    }

    public static String cmpg(String localOrConstantOp1, boolean op1Constant, String localOrConstantOp2, boolean op2Constant) {
        String leftOp = op1Constant ? localOrConstantOp1 : local(localOrConstantOp1);
        String rightOp = op2Constant ? localOrConstantOp2 : local(localOrConstantOp2);
        return "If(Or(fpIsNaN(" + leftOp + "), fpIsNaN(" + rightOp + ")), BitVecVal(1, 8), If(" + leftOp + " > " + rightOp + ", BitVecVal(1, 8), If(" + leftOp + " == " + rightOp + ", BitVecVal(0, 8), BitVecVal(-1, 8))))";
    }

    // cast from larger to smaller bit-vector
    public static String bvToBvNarrow(String localOrConstantOp, boolean opConstant, int newSize) {
        String op = opConstant ? localOrConstantOp : local(localOrConstantOp);
        return "Extract(" + (newSize - 1) + ", 0, " + op + ")";
    }

    // cast from smaller to larger bit-vector
    public static String bvToBvWiden(String localOrConstantOp, boolean opConstant, int deltaSize) {
        String op = opConstant ? localOrConstantOp : local(localOrConstantOp);
        return "Concat(BitVecVal(0, " + deltaSize + "), " + op + ")";
    }

    // cast from bit-vector to floating-point
    public static String bvToFp(String localOrConstantOp, boolean opConstant, boolean isDouble) {
        String s = isDouble ? "Double" : "Float";
        String op = opConstant ? localOrConstantOp : local(localOrConstantOp);
        return "fpSignedToFP(RNE(), " + op + ", " + s + ")";
    }

    // cast from floating-point to bit-vector
    public static String fpToBv(String localOrConstantOp, boolean opConstant, int bitVecSize) {
        String op = opConstant ? localOrConstantOp : local(localOrConstantOp);
        return "fpToSBV(RTZ(), " + op + ", BitVecSort(" + bitVecSize + "))";
    }

    // cast from floating-point to floating-point
    public static String fpToFp(String localOrConstantOp, boolean opConstant, boolean toDouble) {
        String s = toDouble ? "Double" : "Float";
        String op = opConstant ? localOrConstantOp : local(localOrConstantOp);
        return "fpFPToFP(RNE(), " + op + ", " + s + ")";
    }

    public static String newObject() {
        ++objectCounter;
        return "BitVecVal(" + Integer.toString(objectCounter) + ", 32)";
    }

    public static String instanceFieldAccess(String varName, String localOrConstantId, boolean idConstant) {
        String id = idConstant ? localOrConstantId : local(localOrConstantId);
        return "Select(" + varName + ", " + id + ")";
    }

    public static void addInstanceFieldStore(String varName, String localOrConstantId, boolean idConstant, String localOrConstantValue, boolean valueConstant) {
        String id = idConstant ? localOrConstantId : local(localOrConstantId);
        String value = valueConstant ? localOrConstantValue : local(localOrConstantValue);
        addAssignment(varName, "Store(" + varName + ", " + id + ", " + value + ")");
    }

    public static String newArray() {
        ++arrayCounter;
        return "BitVecVal(" + Integer.toString(arrayCounter) + ", 32)";
    }

    public static void initArray(String id, VariableType baseType, String localOrConstantSize, boolean sizeConstant) {
        String arrVar = baseType.name() + "_Arrays";
        String arrLenVar = baseType.name() + "_ArrayLengths";
        String size = sizeConstant ? localOrConstantSize : local(localOrConstantSize);
        addAssignment(arrLenVar, "Store(" + arrLenVar + ", " + id + ", " + size + ")");
        addAssignment(arrVar, "Store(" + arrVar + ", " + id + ", K(BitVecSort(32), " + defaultValue(baseType) + "))");
    }

    public static String arrayAccess(VariableType baseType, String localOrConstantId, boolean idConstant, String localOrConstantIndex, boolean indexConstant) {
        String arrVar = baseType.name() + "_Arrays";
        String id = idConstant ? localOrConstantId : local(localOrConstantId);
        String index = indexConstant ? localOrConstantIndex : local(localOrConstantIndex);
        return "Select(Select(" + arrVar + ", " + id + "), " + index + ")";
    }

    public static void addArrayStore(VariableType baseType,
                                     String localOrConstantId, boolean idConstant,
                                     String localOrConstantIndex, boolean indexConstant,
                                     String localOrConstantValue, boolean valueConstant)
    {
        String arrVar = baseType.name() + "_Arrays";
        String id = idConstant ? localOrConstantId : local(localOrConstantId);
        String index = indexConstant ? localOrConstantIndex : local(localOrConstantIndex);
        String value = valueConstant ? localOrConstantValue : local(localOrConstantValue);
        addAssignment(arrVar, "Store(" + arrVar + ", " + id + ", Store(Select(" + arrVar + ", " + id + "), " + index + ", " + value + "))");
    }

    public static String lengthof(VariableType baseType, String localOrConstantId, boolean idConstant) {
        String id = idConstant ? localOrConstantId : local(localOrConstantId);
        return "Select(" + baseType.name() + "_ArrayLengths, " + id + ")";
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
    public static void addStaticFieldVariableIfNotPresent(VariableType type, String id, int key) {
        if (declaredVariables.add(key)) {
            variables.add(new Variable(type, id));
            fieldInitAssignments.add(new Assignment(id, defaultValue(type)));
        }
    }
    public static void addInstanceFieldVariableIfNotPresent(VariableType type, String id, int key) {
        if (declaredVariables.add(key)) {
            variables.add(new Variable(type, id, true));
            fieldInitAssignments.add(new Assignment(id, "K(BitVecSort(32), " + defaultValue(type) + ")"));
        }
    }

    public static void addAssignment(String leftOp, String rightOp) {
        assignments.add(new Assignment(leftOp, rightOp));
    }

    // call this before entering the callee
    public static void addAssignmentToParameter(String paramName, String fn, String rightOp) {
        addAssignment(paramName + "_" + peekNextFrame(fn), rightOp);
    }

    // call this in the caller after the invocation
    public static void addAssignmentFromReturnValue(String leftOp) {
        addAssignment(leftOp, "RET_" + lastFrame);
    }

    // call this in the callee before exiting
    public static void addAssignmentToReturnValue(String rightOp) {
        String currentFrame = frameStack.peek();
        assignments.add(new Assignment("RET_" + currentFrame, rightOp));
    }

    // call this in the caller after the invocation when symbolic execution not available for a method
    public static void addConcreteAssignment(String leftOp, Object value) {
        addAssignment(leftOp, value.toString());
    }

    public static void addConcreteAssignment(String leftOp, boolean value) {
        addAssignment(leftOp, value ? "True" : "False");
    }

    public static void addConcreteAssignment(String leftOp, byte value) {
        addAssignment(leftOp, "BitVecVal(" + value + ", 8)");
    }

    public static void addConcreteAssignment(String leftOp, char value) {
        addAssignment(leftOp, "BitVecVal(" + (int)value + ", 16)");
    }

    public static void addConcreteAssignment(String leftOp, double value) {
        addAssignment(leftOp, "FPVal(" + value + ", Double)");
    }

    public static void addConcreteAssignment(String leftOp, float value) {
        addAssignment(leftOp, "FPVal(" + value + ", Float)");
    }

    public static void addConcreteAssignment(String leftOp, int value) {
        addAssignment(leftOp, "BitVecVal(" + value + ", 32)");
    }

    public static void addConcreteAssignment(String leftOp, long value) {
        addAssignment(leftOp, "BitVecVal(" + value + ", 64)");
    }

    public static void addConcreteAssignment(String leftOp, short value) {
        addAssignment(leftOp, "BitVecVal(" + value + ", 16)");
    }

    public static void addPathConstraint(int branchId, String condition, boolean conditionConcrete) {
        pathConstraints.add(new PathConstraint(branchId, condition, conditionConcrete, assignments.size()));
    }
}