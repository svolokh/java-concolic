package csci699cav;

import fj.P;
import soot.*;
import soot.jimple.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class PathConditionTransformer extends SceneTransformer {
    private boolean isComplete;

    private SootMethod init;
    private SootMethod lastInput;
    private SootMethod local;
    private SootMethod retVar;
    private SootMethod identity;
    private SootMethod unaryOp;
    private SootMethod binaryOp;
    private SootMethod cmp;
    private SootMethod cmpl;
    private SootMethod cmpg;
    private SootMethod bvToBvNarrow;
    private SootMethod bvToBvWiden;
    private SootMethod bvToFp;
    private SootMethod fpToBv;
    private SootMethod fpToFp;
    private SootMethod newObject;
    private SootMethod instanceFieldAccess;
    private SootMethod addInstanceFieldStore;
    private SootMethod newArray;
    private SootMethod initArray;
    private SootMethod arrayAccess;
    private SootMethod addArrayStore;
    private SootMethod lengthof;
    private SootMethod newFrame;
    private SootMethod exitFrame;
    private SootMethod addVariable;
    private SootMethod addStaticFieldVariableIfNotPresent;
    private SootMethod addInstanceFieldVariableIfNotPresent;
    private SootMethod addAssignment;
    private SootMethod addAssignmentToParameter;
    private SootMethod addAssignmentFromReturnValue;
    private SootMethod addAssignmentToReturnValue;
    private Map<Type, SootMethod> addConcreteAssignment;
    private SootMethod addPathConstraint;

    private SootField varTypeByte;
    private SootField varTypeShort;
    private SootField varTypeInt;
    private SootField varTypeLong;
    private SootField varTypeFloat;
    private SootField varTypeDouble;
    private SootField varTypeChar;

    private Set<SootMethod> methodsToInstrument;
    private Set<SootMethod> libraryMethodsToInstrument;
    private int branchIdCounter = 0;

    private SootField sootTypeToSymbolicType(Type t) {
        if (t instanceof RefType) {
            return varTypeInt;
        } if (t instanceof BooleanType) {
            return varTypeByte;
        } else if (t instanceof ByteType) {
            return varTypeByte;
        } else if (t instanceof CharType) {
            return varTypeChar;
        } else if (t instanceof DoubleType) {
            return varTypeDouble;
        } else if (t instanceof FloatType) {
            return varTypeFloat;
        } else if (t instanceof IntType) {
            return varTypeInt;
        } else if (t instanceof LongType) {
            return varTypeLong;
        } else if (t instanceof ShortType) {
            return varTypeShort;
        } else if (t instanceof ArrayType) {
            return varTypeInt;
        } else {
            throw new IllegalArgumentException("unsupported type " + t);
        }
    }

    private String varNameForInstanceField(SootField sf) {
        return "INST_" + sf.getDeclaringClass().getName().replace(".", "_") + "_" + sf.getName();
    }

    private String varNameForStaticField(SootField sf) {
        return "ST_" + sf.getDeclaringClass().getName().replace(".", "_") + "_" + sf.getName();
    }

    private String constantToSymbolicExpr(Constant c, Type type) {
        if (c instanceof NullConstant) {
            return "BitVecVal(0, 32)";
        } else if (c instanceof IntConstant) {
            if (type instanceof BooleanType) {
                return "BitVecVal(" + c.toString() + ", 8)";
            } else if (type instanceof ByteType) {
                return "BitVecVal(" + c.toString() + ", 8)";
            } else if (type instanceof CharType) {
                return "BitVecVal(" + c.toString() + ", 16)";
            } else if (type instanceof IntType) {
                return "BitVecVal(" + c.toString() + ", 32)";
            } else if (type instanceof ShortType) {
                return "BitVecVal(" + c.toString() + ", 16)";
            } else {
                throw new IllegalArgumentException("unsupported int constant assigned to type " + type);
            }
        } else if (c instanceof LongConstant) {
            return "BitVecVal(" + ((LongConstant)c).value + ", 64)";
        } else if (c instanceof FloatConstant) {
            return "FPVal(" + ((FloatConstant)c).value + ", Float)";
        } else if (c instanceof DoubleConstant) {
            return "FPVal(" + ((DoubleConstant)c).value + ", Double)";
        } else {
            throw new IllegalArgumentException("unsupported constant " + c + " (" + c.getClass() + ")");
        }
    }

    // generates statements to produce a symbolic value for the given value
    private List<Unit> obtainSymbolicValue(Value v, Type type, Local varTypeTmp, Local outVar) {
        if (v instanceof Constant) {
            return Collections.singletonList(
                    Jimple.v().newAssignStmt(outVar, StringConstant.v(constantToSymbolicExpr((Constant)v, type))));
        } else if (v instanceof Local) {
            Local l = (Local)v;
            return Collections.singletonList(
                    Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(local.makeRef(), StringConstant.v(l.getName()))));
        } else if (v instanceof InstanceFieldRef) {
            InstanceFieldRef ifr = (InstanceFieldRef)v;
            String varName = varNameForInstanceField(ifr.getField());

            boolean idConstant;
            String id;
            if (ifr.getBase() instanceof Constant) {
                idConstant = true;
                id = constantToSymbolicExpr((Constant)ifr.getBase(), type);
            } else {
                idConstant = false;
                id = ((Local)ifr.getBase()).getName();
            }

            return Collections.singletonList(
                    Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(instanceFieldAccess.makeRef(),
                            StringConstant.v(varName), StringConstant.v(id), IntConstant.v(idConstant ? 1 : 0))));
        } else if (v instanceof StaticFieldRef) {
            StaticFieldRef sfr = (StaticFieldRef)v;
            return Collections.singletonList(
                    Jimple.v().newAssignStmt(outVar, StringConstant.v(varNameForStaticField(sfr.getField()))));
        } else if (v instanceof ArrayRef) {
            ArrayRef ar = (ArrayRef)v;
            Type baseType = ((ArrayType)ar.getBase().getType()).baseType;

            boolean idConstant;
            String id;
            if (ar.getBase() instanceof Constant) {
                idConstant = true;
                id = constantToSymbolicExpr((Constant)ar.getBase(), type);
            } else {
                idConstant = false;
                id = ((Local)ar.getBase()).getName();
            }

            boolean indexConstant;
            String index;
            if (ar.getIndex() instanceof Constant) {
                indexConstant = true;
                index = constantToSymbolicExpr((Constant)ar.getIndex(), type);
            } else {
                indexConstant = false;
                index = ((Local)ar.getIndex()).getName();
            }

            return Arrays.asList(
                    Jimple.v().newAssignStmt(varTypeTmp, Jimple.v().newStaticFieldRef(sootTypeToSymbolicType(baseType).makeRef())),
                    Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(arrayAccess.makeRef(),
                            varTypeTmp,
                            StringConstant.v(id), IntConstant.v(idConstant ? 1 : 0),
                            StringConstant.v(index), IntConstant.v(indexConstant ? 1 : 0))));
        } else if (v instanceof CastExpr) {
            CastExpr expr = (CastExpr)v;
            Type from = expr.getOp().getType();
            Type to = expr.getCastType();

            boolean opConstant;
            String op;
            if (expr.getOp() instanceof Constant) {
                op = constantToSymbolicExpr((Constant)expr.getOp(), type);
                opConstant = true;
            } else {
                op = ((Local)expr.getOp()).getName();
                opConstant = false;
            }

            if (from instanceof PrimType) {
                PrimType fromPrim = (PrimType)from;
                PrimType toPrim = (PrimType)to;

                if (toPrim instanceof BooleanType) {
                    toPrim = ByteType.v();
                }
                if (fromPrim instanceof BooleanType) {
                    fromPrim = ByteType.v();
                }

                boolean fromBv = Utils.isBitVectorType(fromPrim);
                boolean toBv = Utils.isBitVectorType(toPrim);

                if (fromBv && toBv) { // BV => BV
                    int fromSize = Utils.bitVectorSize(fromPrim);
                    int toSize = Utils.bitVectorSize(toPrim);
                    if (toSize < fromSize) {
                        return Collections.singletonList(
                                Jimple.v().newAssignStmt(outVar,
                                        Jimple.v().newStaticInvokeExpr(bvToBvNarrow.makeRef(), StringConstant.v(op),
                                                IntConstant.v(opConstant ? 1 : 0), IntConstant.v(toSize))));
                    } else if (toSize > fromSize) {
                        return Collections.singletonList(
                                Jimple.v().newAssignStmt(outVar,
                                        Jimple.v().newStaticInvokeExpr(bvToBvWiden.makeRef(), StringConstant.v(op),
                                                IntConstant.v(opConstant ? 1 : 0), IntConstant.v(toSize - fromSize))));
                    } else {
                        return Collections.singletonList(
                                Jimple.v().newAssignStmt(outVar,
                                        Jimple.v().newStaticInvokeExpr(identity.makeRef(), StringConstant.v(op), IntConstant.v(opConstant ? 1 : 0))));
                    }
                } else if (fromBv) { // BV => FP
                    boolean isDouble = toPrim instanceof DoubleType;
                    return Collections.singletonList(
                            Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(bvToFp.makeRef(), StringConstant.v(op), IntConstant.v(opConstant ? 1 : 0),
                                    IntConstant.v(isDouble ? 1 : 0))));
                } else if (toBv) { // FP => BV
                    int bitVecSize = Utils.bitVectorSize(toPrim);
                    return Collections.singletonList(
                            Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(fpToBv.makeRef(), StringConstant.v(op), IntConstant.v(opConstant ? 1 : 0),
                                    IntConstant.v(bitVecSize))));
                } else { // FP => FP
                    boolean toDouble = toPrim instanceof DoubleType;
                    return Collections.singletonList(
                            Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(fpToFp.makeRef(), StringConstant.v(op), IntConstant.v(opConstant ? 1 : 0),
                                    IntConstant.v(toDouble ? 1 : 0))));
                }
            } else if ((from instanceof RefType && to instanceof RefType) || (from instanceof ArrayType && to instanceof ArrayType)) {
                // nothing needs to be done for references
                return Collections.singletonList(
                        Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(identity.makeRef(), StringConstant.v(op),  IntConstant.v(opConstant ? 1 : 0))));
            } else {
                throw new IllegalArgumentException("unsupported cast " + expr);
            }
        } else if (v instanceof UnopExpr)
        {
            UnopExpr e = (UnopExpr)v;

            boolean opConstant;
            String op;
            if (e.getOp() instanceof Constant) {
                op = constantToSymbolicExpr((Constant)e.getOp(), type);
                opConstant = true;
            } else {
                op = ((Local)e.getOp()).getName();
                opConstant = false;
            }

            if (e instanceof NegExpr)
            {
                return Collections.singletonList(
                        Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(unaryOp.makeRef(),
                                StringConstant.v("-"), StringConstant.v(op), IntConstant.v(opConstant ? 1 : 0))));
            } else if (e instanceof LengthExpr)
            {
                LengthExpr le = (LengthExpr)e;
                ArrayType arrType = (ArrayType)le.getOp().getType();
                return Arrays.asList(
                        Jimple.v().newAssignStmt(varTypeTmp, Jimple.v().newStaticFieldRef(sootTypeToSymbolicType(arrType.baseType).makeRef())),
                        Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(lengthof.makeRef(), varTypeTmp, StringConstant.v(op), IntConstant.v(opConstant ? 1 : 0))));
            } else
            {
                throw new IllegalArgumentException("unexpected UnopExpr: " + e + " (" + e.getClass().getName() + ")");
            }
        } else if (v instanceof BinopExpr) {
            BinopExpr e = (BinopExpr)v;

            Type constantType = null;
            if (!(e.getOp1() instanceof Constant)) {
                constantType = ((Local)e.getOp1()).getType();
            }
            if (!(e.getOp2() instanceof Constant)) {
                constantType = ((Local)e.getOp2()).getType();
            }

            boolean op1Constant, op2Constant;
            String op1, op2;
            if (e.getOp1() instanceof Constant) {
                op1 = constantToSymbolicExpr((Constant)e.getOp1(), constantType);
                op1Constant = true;
            } else {
                op1 = ((Local)e.getOp1()).getName();
                op1Constant = false;
            }
            if (e.getOp2() instanceof Constant) {
                op2 = constantToSymbolicExpr((Constant)e.getOp2(), constantType);
                op2Constant = true;
            } else {
                op2 = ((Local)e.getOp2()).getName();
                op2Constant = false;
            }

            if (e instanceof CmpExpr) {
                return Collections.singletonList(
                        Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(cmp.makeRef(),
                                StringConstant.v(op1), IntConstant.v(op1Constant ? 1 : 0), StringConstant.v(op2), IntConstant.v(op2Constant ? 1 : 0))));
            } else if (e instanceof CmplExpr) {
                return Collections.singletonList(
                        Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(cmpl.makeRef(),
                                StringConstant.v(op1), IntConstant.v(op1Constant ? 1 : 0), StringConstant.v(op2), IntConstant.v(op2Constant ? 1 : 0))));
            } else if (e instanceof CmpgExpr) {
                return Collections.singletonList(
                        Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(cmpg.makeRef(),
                                StringConstant.v(op1), IntConstant.v(op1Constant ? 1 : 0), StringConstant.v(op2), IntConstant.v(op2Constant ? 1 : 0))));
            } else {
                String symbol = e.getSymbol();
                if (symbol.equals(" >>> ")) {
                    symbol = " >> ";
                } else if (symbol.equals(" <<< ")) {
                    symbol = " << ";
                }
                return Collections.singletonList(
                        Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(binaryOp.makeRef(),
                                StringConstant.v(symbol),
                                StringConstant.v(op1), IntConstant.v(op1Constant ? 1 : 0), StringConstant.v(op2), IntConstant.v(op2Constant ? 1 : 0))));
            }
        } else {
            throw new IllegalArgumentException("unsupported value: " + v + " (" + v.getClass().getName() + ")");
        }
    }

    private boolean methodHasSymbolicExecution(SootMethod m) {
        return (methodsToInstrument.contains(m) || libraryMethodsToInstrument.contains(m)) && m.getSource() != null;
    }

    // inputs: invoke expression, and a local string where a temporary value can be stored
    // return value: list of statements to insert before the invoke expression
    private void symbolicInvocation(InvokeExpr expr, Local opTmp, List<Unit> output) {
        SootMethod m = expr.getMethod();
        if (methodHasSymbolicExecution(m)) {
            Body b = m.retrieveActiveBody();
            int i = 0;
            if (!m.isStatic()) {
                InstanceInvokeExpr iie = (InstanceInvokeExpr)expr;
                Local thisLocal = b.getThisLocal();
                output.addAll(obtainSymbolicValue(iie.getBase(), m.getDeclaringClass().getType(), null, opTmp));
                output.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignmentToParameter.makeRef(),
                        StringConstant.v(thisLocal.getName()), StringConstant.v(b.getMethod().getName()), opTmp)));
            }
            for (Local paramLocal : b.getParameterLocals()) {
                output.addAll(obtainSymbolicValue(expr.getArg(i), m.getParameterType(i), null, opTmp));
                output.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignmentToParameter.makeRef(),
                        StringConstant.v(paramLocal.getName()),
                        StringConstant.v(b.getMethod().getName()),
                        opTmp)));
                ++i;
            }
        } else {
            System.out.println("Warning: Encountered method call to " + expr.getMethod().getSignature() + " for which symbolic execution is unavailable, will use concrete return value");
            isComplete = false;
        }
    }

    private void processMethod(Body body)
    {
        SootMethod m = body.getMethod();

        if (m.isStatic() && m.getReturnType().equals(VoidType.v()) && m.getParameterCount() == 1 &&
                m.getParameterType(0).equals(ArrayType.v(RefType.v("java.lang.String"), 1)) && m.getName().equals("main"))
        {
            // skip Java main method
            return;
        }

        SwitchToIfStmt.processMethod(body);
        SameTypesBinOp.processMethod(body);
        SameTypeReturnValue.processMethod(body);

        List<Local> origLocals = new ArrayList<>(body.getLocals());

        Local opTmp1 = Jimple.v().newLocal("opTmp1", RefType.v("java.lang.String"));
        Local opTmp2 = Jimple.v().newLocal("opTmp2", RefType.v("java.lang.String"));
        Local varTypeTmp = Jimple.v().newLocal("varTypeTmp", RefType.v("csci699cav.VariableType"));
        Local condTmp = Jimple.v().newLocal("condTmp", BooleanType.v());
        body.getLocals().addLast(opTmp1);
        body.getLocals().addLast(opTmp2);
        body.getLocals().addLast(varTypeTmp);
        body.getLocals().addLast(condTmp);

        UnitPatchingChain units = body.getUnits();
        Iterator<Unit> it = units.snapshotIterator(); // for use when instrumenting the method body
        Iterator<Unit> it2  = units.snapshotIterator(); // for use when declaring variables

        // assign a unique id to each branch
        for (Unit u : body.getUnits())
        {
            if (u instanceof IfStmt)
            {
                ++branchIdCounter;
                u.addTag(new BranchIdTag(branchIdCounter));
            }
        }

        Unit first = units.getFirst();
        while (first instanceof IdentityStmt)
        {
            first = units.getSuccOf(first);
        }

        // add header
        units.insertBefore(
                Jimple.v().newInvokeStmt(
                        Jimple.v().newStaticInvokeExpr(newFrame.makeRef(), StringConstant.v(body.getMethod().getName()))), first);

        // add variables
        {
            // variable for return value
            if (!m.getReturnType().equals(VoidType.v()))
            {
                units.insertBefore(
                        Arrays.asList(
                                Jimple.v().newAssignStmt(varTypeTmp, Jimple.v().newStaticFieldRef(sootTypeToSymbolicType(m.getReturnType()).makeRef())),
                                Jimple.v().newAssignStmt(opTmp1, Jimple.v().newStaticInvokeExpr(retVar.makeRef())),
                                Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addVariable.makeRef(), varTypeTmp, opTmp1))), first);
            }

            // variable for each local
            for (Local l : origLocals)
            {
                units.insertBefore(
                        Arrays.asList(
                                Jimple.v().newAssignStmt(varTypeTmp, Jimple.v().newStaticFieldRef(sootTypeToSymbolicType(l.getType()).makeRef())),
                                Jimple.v().newAssignStmt(opTmp1,
                                        Jimple.v().newStaticInvokeExpr(local.makeRef(), StringConstant.v(l.getName()))),
                                Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addVariable.makeRef(), varTypeTmp, opTmp1))
                        ), first);
            }

            // declare variables for instance and static fields
            Set<Integer> fieldsDeclared = new HashSet<>();
            while (it2.hasNext())
            {
                Unit u = it2.next();
                for (ValueBox vb : u.getUseAndDefBoxes()) {
                    Value v = vb.getValue();
                    if (v instanceof FieldRef) {
                        SootField sf = ((FieldRef)v).getField();
                        int key = sf.equivHashCode();
                        if (fieldsDeclared.add(key))
                        {
                            if (v instanceof StaticFieldRef) {
                                String varName = varNameForStaticField(sf);
                                units.insertBefore(
                                        Arrays.asList(
                                                Jimple.v().newAssignStmt(varTypeTmp, Jimple.v().newStaticFieldRef(sootTypeToSymbolicType(sf.getType()).makeRef())),
                                                Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addStaticFieldVariableIfNotPresent.makeRef(), varTypeTmp,
                                                        StringConstant.v(varName), IntConstant.v(key)))), first);
                            } else {
                                assert v instanceof InstanceFieldRef;
                                String varName = varNameForInstanceField(sf);
                                units.insertBefore(
                                        Arrays.asList(
                                                Jimple.v().newAssignStmt(varTypeTmp, Jimple.v().newStaticFieldRef(sootTypeToSymbolicType(sf.getType()).makeRef())),
                                                Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addInstanceFieldVariableIfNotPresent.makeRef(), varTypeTmp,
                                                        StringConstant.v(varName), IntConstant.v(key)))), first);
                            }
                        }
                    }
                }
            }
        }

        // if entry-point, add entry-point header
        if (Utils.hasEntryPointAnnotation(body.getMethod()))
        {
            // add call to ConcolicState.init()
            {
                Unit u = units.getFirst();
                while (u instanceof IdentityStmt)
                {
                    u = units.getSuccOf(u);
                }
                units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(init.makeRef())), u);
            }

            // add assignment to input for each parameter to the entry-point method
            List<Local> paramLocals = body.getParameterLocals();
            for (int i = 0; i != paramLocals.size(); ++i)
            {
                Local paramLocal = paramLocals.get(i);
                units.insertBefore(
                        Arrays.asList(
                                Jimple.v().newAssignStmt(opTmp1,
                                        Jimple.v().newStaticInvokeExpr(local.makeRef(), StringConstant.v(paramLocal.getName()))),
                                Jimple.v().newInvokeStmt(
                                        Jimple.v().newStaticInvokeExpr(
                                                addAssignment.makeRef(),
                                                opTmp1,
                                                StringConstant.v("INPUT" + i)))
                        ),
                        first);
            }
        }

        Type returnType = body.getMethod().getReturnType();
        List<Unit> returnStmts = units.stream().filter(u -> u instanceof ReturnStmt || u instanceof ReturnVoidStmt).collect(Collectors.toList());
        for (Unit u : returnStmts)
        {
            Unit exitFrameStmt = Jimple.v().newInvokeStmt(
                    Jimple.v().newStaticInvokeExpr(exitFrame.makeRef()));
            units.insertBefore(exitFrameStmt, u);

            if (u instanceof ReturnStmt)
            {
                ReturnStmt retStmt = (ReturnStmt)u;
                Value retVal = retStmt.getOp();
                List<Unit> toInsert = new LinkedList<>();
                toInsert.addAll(obtainSymbolicValue(retVal, returnType, varTypeTmp, opTmp1));
                toInsert.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignmentToReturnValue.makeRef(), opTmp1)));
                units.insertBefore(toInsert, exitFrameStmt);
            }
        }

        while (it.hasNext())
        {
            Stmt s = (Stmt)it.next();
            if (s instanceof AssignStmt)
            {
                AssignStmt stmt = (AssignStmt)s;
                Value leftOp = stmt.getLeftOp();
                Value rightOp = stmt.getRightOp();

                if (rightOp instanceof InvokeExpr) {
                    InvokeExpr expr = (InvokeExpr)rightOp;
                    SootMethod exprMethod = expr.getMethod();
                    String methodName = exprMethod.getName();
                    if (exprMethod.getDeclaringClass().getName().equals("csci699cav.Concolic")
                            && !methodName.equals("assertTrue")
                            && !methodName.equals("assertFalse")
                            && !methodName.equals("assume"))
                    {
                        if (expr.getMethod().getName().startsWith("input"))
                        {
                            List<Unit> toInsert = new LinkedList<>();
                            toInsert.addAll(obtainSymbolicValue(leftOp, leftOp.getType(), varTypeTmp, opTmp1));
                            toInsert.add(Jimple.v().newAssignStmt(opTmp2, Jimple.v().newStaticInvokeExpr(lastInput.makeRef())));
                            toInsert.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignment.makeRef(), opTmp1, opTmp2)));
                            units.insertAfter(toInsert, s);
                        } else {
                            throw new IllegalArgumentException("encountered unexpected method call from Concolic class: " + expr);
                        }
                    } else
                    {
                        List<Unit> toInsert = new LinkedList<>();
                        symbolicInvocation(expr, opTmp1, toInsert);
                        toInsert.addAll(obtainSymbolicValue(leftOp, leftOp.getType(), varTypeTmp, opTmp1));
                        units.insertBefore(toInsert, s);
                        toInsert.clear();
                        if (!methodHasSymbolicExecution(expr.getMethod())) {
                            // encountered method for which we cannot do symbolic execution; use its concrete return value
                            if (leftOp.getType() instanceof PrimType) {
                                toInsert.add(Jimple.v().newInvokeStmt(
                                        Jimple.v().newStaticInvokeExpr(addConcreteAssignment.get(leftOp.getType()).makeRef(), opTmp1, leftOp)));
                            } else {
                                toInsert.add(Jimple.v().newInvokeStmt(
                                        Jimple.v().newStaticInvokeExpr(addConcreteAssignment.get(RefType.v("java.lang.Object")).makeRef(), opTmp1, leftOp)));
                            }
                        } else {
                            toInsert.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignmentFromReturnValue.makeRef(), opTmp1)));
                        }
                        units.insertAfter(toInsert, s);
                    }
                } else if (rightOp instanceof NewExpr) {
                    List<Unit> toInsert = new LinkedList<>();
                    toInsert.addAll(obtainSymbolicValue(leftOp, leftOp.getType(), varTypeTmp, opTmp1));
                    toInsert.addAll(Arrays.asList(
                            Jimple.v().newAssignStmt(opTmp2, Jimple.v().newStaticInvokeExpr(newObject.makeRef())),
                            Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignment.makeRef(), opTmp1,  opTmp2))
                    ));
                    units.insertBefore(toInsert, s);
                } else if (rightOp instanceof NewArrayExpr) {
                    NewArrayExpr nae = (NewArrayExpr)rightOp;

                    boolean sizeConstant;
                    String size;
                    if (nae.getSize() instanceof Constant) {
                        size = constantToSymbolicExpr((Constant)nae.getSize(), IntType.v());
                        sizeConstant = true;
                    } else {
                        size = ((Local)nae.getSize()).getName();
                        sizeConstant = false;
                    }

                    List<Unit> toInsert = new LinkedList<>();
                    toInsert.addAll(obtainSymbolicValue(leftOp, leftOp.getType(), varTypeTmp, opTmp1));
                    toInsert.addAll(Arrays.asList(
                            Jimple.v().newAssignStmt(opTmp2, Jimple.v().newStaticInvokeExpr(newArray.makeRef())),
                            Jimple.v().newAssignStmt(varTypeTmp, Jimple.v().newStaticFieldRef(sootTypeToSymbolicType(nae.getBaseType()).makeRef())),
                            Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignment.makeRef(), opTmp1, opTmp2)),
                            Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(initArray.makeRef(), opTmp2, varTypeTmp, StringConstant.v(size), IntConstant.v(sizeConstant ? 1 : 0)))
                    ));
                    units.insertBefore(toInsert, s);
                } else if (leftOp instanceof InstanceFieldRef) {
                    InstanceFieldRef ifr = (InstanceFieldRef)leftOp;
                    SootField f = ifr.getField();
                    String varName = varNameForInstanceField(f);

                    boolean idConstant;
                    String id;
                    if (ifr.getBase() instanceof Constant) {
                        idConstant = true;
                        id = constantToSymbolicExpr((Constant)ifr.getBase(), IntType.v());
                    } else {
                        idConstant = false;
                        id = ((Local)ifr.getBase()).getName();
                    }

                    boolean valueConstant;
                    String value;
                    if (rightOp instanceof Constant) {
                        valueConstant = true;
                        value = constantToSymbolicExpr((Constant)rightOp, f.getType());
                    } else {
                        valueConstant = false;
                        value = ((Local)rightOp).getName();
                    }

                    units.insertBefore(
                            Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addInstanceFieldStore.makeRef(), StringConstant.v(varName),
                                    StringConstant.v(id), IntConstant.v(idConstant ? 1 : 0),
                                    StringConstant.v(value), IntConstant.v(valueConstant ? 1 : 0)))
                            , s);
                } else if (leftOp instanceof ArrayRef) {
                    ArrayRef ar = (ArrayRef)leftOp;
                    Type baseType = ((ArrayType)ar.getBase().getType()).baseType;

                    boolean idConstant;
                    String id;
                    if (ar.getBase() instanceof Constant) {
                        idConstant = true;
                        id = constantToSymbolicExpr((Constant)ar.getBase(), IntType.v());
                    } else {
                        idConstant = false;
                        id = ((Local)ar.getBase()).getName();
                    }

                    boolean indexConstant;
                    String index;
                    if (ar.getIndex() instanceof Constant) {
                        indexConstant = true;
                        index = constantToSymbolicExpr((Constant)ar.getIndex(), IntType.v());
                    } else {
                        indexConstant = false;
                        index = ((Local)ar.getIndex()).getName();
                    }

                    boolean valueConstant;
                    String value;
                    if (rightOp instanceof Constant) {
                        valueConstant = true;
                        value = constantToSymbolicExpr((Constant)rightOp, baseType);
                    } else {
                        valueConstant = false;
                        value = ((Local)rightOp).getName();
                    }

                    units.insertBefore(Arrays.asList(
                            Jimple.v().newAssignStmt(varTypeTmp, Jimple.v().newStaticFieldRef(sootTypeToSymbolicType(baseType).makeRef())),
                            Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addArrayStore.makeRef(),
                                    varTypeTmp,
                                    StringConstant.v(id), IntConstant.v(idConstant ? 1 : 0),
                                    StringConstant.v(index), IntConstant.v(indexConstant ? 1 : 0),
                                    StringConstant.v(value), IntConstant.v(valueConstant ? 1 : 0)))
                    ), s);
                } else {
                    List<Unit> toInsert = new LinkedList<>();
                    toInsert.addAll(obtainSymbolicValue(leftOp, leftOp.getType(), varTypeTmp, opTmp1));
                    toInsert.addAll(obtainSymbolicValue(rightOp, leftOp.getType() /* this is intentional */, varTypeTmp, opTmp2));
                    toInsert.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignment.makeRef(), opTmp1, opTmp2)));
                    units.insertBefore(toInsert, s);
                }
            } else if (s instanceof InvokeStmt) {
                InvokeStmt stmt = (InvokeStmt)s;
                List<Unit> toInsert = new LinkedList<>();
                symbolicInvocation(stmt.getInvokeExpr(), opTmp1, toInsert);
                if (!toInsert.isEmpty()) {
                    units.insertBefore(toInsert, s);
                }
            } else if (s instanceof IfStmt)
            {
                /**
                 * if (condition): goto yes
                 * condTmp = 0
                 * goto done
                 * yes: condTmp = 1
                 * done: ...
                 */
                IfStmt stmt = (IfStmt)s;
                int branchId = ((BranchIdTag)stmt.getTag(BranchIdTag.NAME)).id;
                Unit yes = Jimple.v().newAssignStmt(condTmp, IntConstant.v(1));
                List<Unit> obtainSym = obtainSymbolicValue(stmt.getCondition(), BooleanType.v(), varTypeTmp, opTmp1);
                List<Unit> toInsert = new LinkedList<>();
                toInsert.addAll(Arrays.asList(
                        Jimple.v().newIfStmt(stmt.getCondition(), yes),
                        Jimple.v().newAssignStmt(condTmp, IntConstant.v(0)),
                        Jimple.v().newGotoStmt(obtainSym.get(0)),
                        yes));
                toInsert.addAll(obtainSym);
                toInsert.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addPathConstraint.makeRef(), IntConstant.v(branchId), opTmp1, condTmp)));
                units.insertBefore(toInsert, s);
            }
        }
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        String entryPointsFile = System.getenv("ENTRYPOINTS");
        String libraryDepsOutput = System.getenv("LIBRARY_DEPS_OUT");

        SootClass sc = Scene.v().loadClassAndSupport("csci699cav.ConcolicState");
        SootClass variableTypeClass = Scene.v().loadClassAndSupport("csci699cav.VariableType");

        isComplete = true;

        init = sc.getMethodByName("init");
        lastInput = sc.getMethodByName("lastInput");
        local = sc.getMethodByName("local");
        retVar = sc.getMethodByName("retVar");
        identity = sc.getMethodByName("identity");
        unaryOp = sc.getMethodByName("unaryOp");
        binaryOp = sc.getMethodByName("binaryOp");
        cmp = sc.getMethodByName("cmp");
        cmpl = sc.getMethodByName("cmpl");
        cmpg = sc.getMethodByName("cmpg");
        bvToBvNarrow = sc.getMethodByName("bvToBvNarrow");
        bvToBvWiden = sc.getMethodByName("bvToBvWiden");
        bvToFp = sc.getMethodByName("bvToFp");
        fpToBv = sc.getMethodByName("fpToBv");
        fpToFp = sc.getMethodByName("fpToFp");
        newObject = sc.getMethodByName("newObject");
        instanceFieldAccess = sc.getMethodByName("instanceFieldAccess");
        addInstanceFieldStore = sc.getMethodByName("addInstanceFieldStore");
        newArray = sc.getMethodByName("newArray");
        initArray = sc.getMethodByName("initArray");
        arrayAccess = sc.getMethodByName("arrayAccess");
        addArrayStore = sc.getMethodByName("addArrayStore");
        lengthof = sc.getMethodByName("lengthof");
        newFrame = sc.getMethodByName("newFrame");
        exitFrame = sc.getMethodByName("exitFrame");
        addVariable = sc.getMethodByName("addVariable");
        addStaticFieldVariableIfNotPresent = sc.getMethodByName("addStaticFieldVariableIfNotPresent");
        addInstanceFieldVariableIfNotPresent = sc.getMethodByName("addInstanceFieldVariableIfNotPresent");
        addAssignment = sc.getMethodByName("addAssignment");
        addAssignmentToParameter = sc.getMethodByName("addAssignmentToParameter");
        addAssignmentFromReturnValue = sc.getMethodByName("addAssignmentFromReturnValue");
        addAssignmentToReturnValue = sc.getMethodByName("addAssignmentToReturnValue");

        addConcreteAssignment = new HashMap<>();
        for (SootMethod m : sc.getMethods())
        {
            if (m.getName().equals("addConcreteAssignment"))
            {
                addConcreteAssignment.put(m.getParameterType(1), m);
            }
        }

        addPathConstraint = sc.getMethodByName("addPathConstraint");

        varTypeByte = variableTypeClass.getFieldByName("BYTE");
        varTypeShort = variableTypeClass.getFieldByName("SHORT");
        varTypeInt = variableTypeClass.getFieldByName("INT");
        varTypeLong = variableTypeClass.getFieldByName("LONG");
        varTypeFloat = variableTypeClass.getFieldByName("FLOAT");
        varTypeDouble = variableTypeClass.getFieldByName("DOUBLE");
        varTypeChar = variableTypeClass.getFieldByName("CHAR");

        methodsToInstrument = new HashSet<>();
        List<SootMethod> entryPoints = new ArrayList<>();
        if (entryPointsFile != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(entryPointsFile)))
            {
                while (br.ready()) {
                    String sig = br.readLine();
                    entryPoints.add(Scene.v().getMethod(sig));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            for (SootClass c : Scene.v().getApplicationClasses())
            {
                List<SootMethod> methods = c.getMethods();
                for (SootMethod m : methods) {
                    if (Utils.hasEntryPointAnnotation(m)) {
                        entryPoints.add(m);
                    }
                }
            }
        }

        Set<SootClass> newAppClasses = new HashSet<>();

        libraryMethodsToInstrument = new HashSet<>();
        Set<SootClass> classesLoaded = new HashSet<>();
        Set<SootMethod> workList = new HashSet<>(entryPoints);
        while (!workList.isEmpty()) {
            while (!workList.isEmpty()) {
                Iterator<SootMethod> it = workList.iterator();
                SootMethod m = it.next();
                it.remove();

                classesLoaded.add(m.getDeclaringClass());

                if (m.getSource() == null) {
                    // if method source is unavailable we cannot instrument this method (it will be evaluated concretely)
                    continue;
                }

                if (m.getDeclaringClass().isLibraryClass()) {
                    libraryMethodsToInstrument.add(m);
                } else {
                    methodsToInstrument.add(m);
                    newAppClasses.add(m.getDeclaringClass());

                    if (m.getSource() != null) {
                        Body b = m.retrieveActiveBody();
                        for (Unit u : b.getUnits()) {
                            for (ValueBox vb : u.getUseAndDefBoxes()) {
                                Value v = vb.getValue();
                                if (v instanceof InvokeExpr) {
                                    InvokeExpr expr = (InvokeExpr)v;
                                    SootMethod md = expr.getMethod();
                                    if (!Utils.doNotInstrument(md) && !methodsToInstrument.contains(md) && !libraryMethodsToInstrument.contains(md)) {
                                        workList.add(md);
                                    }
                                } else if (v instanceof FieldRef) {
                                    FieldRef fr = (FieldRef)v;
                                    classesLoaded.add(fr.getField().getDeclaringClass());
                                }
                            }
                        }
                    }
                }
            }
            for (SootClass c : classesLoaded) {
                // add <clinit> for each loaded class
                if (c.declaresMethodByName("<clinit>"))
                {
                    SootMethod m = c.getMethodByName("<clinit>");
                    if (!Utils.doNotInstrument(m) && !methodsToInstrument.contains(m) && !libraryMethodsToInstrument.contains(m)) {
                        workList.add(m);
                    }
                }
            }
        }

        if (libraryDepsOutput != null) {
            Map<String, Map<SootClass, Set<SootMethod>>> deps = new HashMap<>();
            for (SootMethod m : libraryMethodsToInstrument) {
                SootClass c = m.getDeclaringClass();
                if (c.getName().equals("csci699cav.Concolic")) {
                    // we handle this implicitly
                    continue;
                }
                String filePath = Utils.getFilePathForClass(c);
                Map<SootClass, Set<SootMethod>> classToMethods = deps.computeIfAbsent(filePath, k -> new HashMap<>());
                Set<SootMethod> methods = classToMethods.computeIfAbsent(c, k -> new HashSet<>());
                methods.add(m);
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(libraryDepsOutput)))
            {
                for (Map.Entry<String, Map<SootClass, Set<SootMethod>>> dep : deps.entrySet()) {
                    bw.write("@ " + dep.getKey());
                    bw.write("\n");
                    for (Map.Entry<SootClass, Set<SootMethod>> e : dep.getValue().entrySet()) {
                        for (SootMethod m : e.getValue()) {
                            bw.write(m.getSignature());
                            bw.write("\n");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (!libraryMethodsToInstrument.isEmpty()) {
            throw new RuntimeException("No output for library method dependencies was specified but there are dependencies on library methods");
        }

        for (SootMethod m : methodsToInstrument)
        {
            Body b = m.retrieveActiveBody();
            try
            {
                processMethod(b);
            } catch (Exception e) {
                System.err.println(b);
                throw new RuntimeException("failed to instrument method " + m.getSignature(), e);
            }
        }

        // now set the application classes to only be the ones we are adding instrumentation code to, so Soot only writes class files for the ones we modified
        for (SootClass c : Scene.v().getClasses()) {
            SootClass outerMost = c;
            while (outerMost.hasOuterClass()) {
                outerMost = outerMost.getOuterClass();
            }
            if (newAppClasses.contains(outerMost)) {
                newAppClasses.add(c);
                c.setApplicationClass();
            } else {
                c.setLibraryClass();
            }
        }

        if (entryPointsFile != null) {
            // post-processing to do when processing a library
            // remove some methods that are known to be problematic when instrumented
            {
                SootClass c = Scene.v().getSootClass("java.lang.Integer");
                c.removeMethod(c.getMethodByName("<clinit>"));
            }
            // rename all the instrumented classes to have the instrumented prefix
            for (SootClass c : newAppClasses) {
                if (c.getName().startsWith("csci699cav.")) {
                    // special case that we do not rename
                    continue;
                }
                c.rename("concolic." + c.getName());
            }
        }

        {
            // rename all library dependencies to point to instrumented version
            Set<SootClass> libraryDepClasses = new HashSet<>();
            for (SootMethod m : libraryMethodsToInstrument) {
                libraryDepClasses.add(m.getDeclaringClass());
            }
            for (SootClass c : libraryDepClasses) {
                if (c.getName().startsWith("csci699cav.")) {
                    // special case that we do not rename
                    continue;
                }
                c.rename("concolic." + c.getName());
            }
        }

        // DEBUG
       /* for (SootMethod m : methodsToInstrument)
        {
            System.out.println(m.retrieveActiveBody());
        }*/

        if (!isComplete) {
            System.out.println("Warning: Complete exploration of program paths will not be possible due to issues listed above");
        }
    }
}