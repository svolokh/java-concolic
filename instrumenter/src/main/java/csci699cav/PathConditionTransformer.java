package csci699cav;

import CallGraph.NewNode;
import CallGraph.StringCallGraph;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;

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
    private SootMethod newFrame;
    private SootMethod exitFrame;
    private SootMethod addVariable;
    private SootMethod addVariableIfNotPresent;
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

    private int branchIdCounter = 0;

    private SootField sootTypeToSymbolicType(Type t) {
        if (t instanceof BooleanType) {
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
        } else {
            throw new IllegalArgumentException("unsupported type " + t);
        }
    }

    private String varNameForStaticField(SootField sf) {
        return "ST_" + sf.getDeclaringClass().getName().replace(".", "_") + "_" + sf.getName();
    }

    private String constantToSymbolicExpr(Constant c) {
        if (c instanceof IntConstant || c instanceof StringConstant) {
            return c.toString();
        } else if (c instanceof LongConstant) {
            return Long.toString(((LongConstant)c).value);
        } else if (c instanceof FloatConstant) {
            return Float.toString(((FloatConstant)c).value);
        } else if (c instanceof DoubleConstant) {
            return Double.toString(((DoubleConstant)c).value);
        } else {
            throw new IllegalArgumentException("unsupported constant " + c + " (" + c.getClass() + ")");
        }
    }

    // generates statement to produce a symbolic value for the given value
    private Unit obtainSymbolicValue(Value v, Local outVar) {
        if (v instanceof Constant) {
            return Jimple.v().newAssignStmt(outVar, StringConstant.v(constantToSymbolicExpr((Constant)v)));
        } else if (v instanceof Local) {
            Local l = (Local)v;
            return Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(local.makeRef(), StringConstant.v(l.getName())));
        } else if (v instanceof StaticFieldRef) {
            StaticFieldRef sfr = (StaticFieldRef)v;
            return Jimple.v().newAssignStmt(outVar, StringConstant.v(varNameForStaticField(sfr.getField())));
        } else if (v instanceof CastExpr) {
            CastExpr expr = (CastExpr)v;
            Type from = expr.getOp().getType();
            Type to = expr.getCastType();

            boolean opConstant;
            String op;
            if (expr.getOp() instanceof Constant) {
                op = constantToSymbolicExpr((Constant)expr.getOp());
                opConstant = true;
            } else {
                op = ((Local)expr.getOp()).getName();
                opConstant = false;
            }

            if (from instanceof PrimType) {
                PrimType fromPrim = (PrimType)from;
                PrimType toPrim = (PrimType)to;

                if (fromPrim instanceof BooleanType || toPrim instanceof BooleanType) {
                    if (!(fromPrim instanceof BooleanType) || !(toPrim instanceof BooleanType)) {
                        throw new IllegalArgumentException("unexpected cast from " + fromPrim + " to " + toPrim);
                    }
                }

                boolean fromBv = Utils.isBitVectorType(fromPrim);
                boolean toBv = Utils.isBitVectorType(toPrim);

                if (fromBv && toBv) { // BV => BV
                    int fromSize = Utils.bitVectorSize(fromPrim);
                    int toSize = Utils.bitVectorSize(toPrim);
                    if (toSize < fromSize) {
                        return Jimple.v().newAssignStmt(outVar,
                                Jimple.v().newStaticInvokeExpr(bvToBvNarrow.makeRef(), StringConstant.v(op),
                                        IntConstant.v(opConstant ? 1 : 0), IntConstant.v(toSize)));
                    } else if (toSize > fromSize) {
                        return Jimple.v().newAssignStmt(outVar,
                                Jimple.v().newStaticInvokeExpr(bvToBvWiden.makeRef(), StringConstant.v(op),
                                        IntConstant.v(opConstant ? 1 : 0), IntConstant.v(toSize - fromSize)));
                    } else {
                        return Jimple.v().newAssignStmt(outVar,
                                Jimple.v().newStaticInvokeExpr(identity.makeRef(), StringConstant.v(op), IntConstant.v(opConstant ? 1 : 0)));
                    }
                } else if (fromBv) { // BV => FP
                    boolean isDouble = toPrim instanceof DoubleType;
                    return Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(bvToFp.makeRef(), StringConstant.v(op), IntConstant.v(opConstant ? 1 : 0),
                            IntConstant.v(isDouble ? 1 : 0)));
                } else if (toBv) { // FP => BV
                    int bitVecSize = Utils.bitVectorSize(toPrim);
                    return Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(fpToBv.makeRef(), StringConstant.v(op), IntConstant.v(opConstant ? 1 : 0),
                            IntConstant.v(bitVecSize)));
                } else { // FP => FP
                    boolean toDouble = toPrim instanceof DoubleType;
                    return Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(fpToFp.makeRef(), StringConstant.v(op), IntConstant.v(opConstant ? 1 : 0),
                            IntConstant.v(toDouble ? 1 : 0)));
                }
            } else {
                throw new IllegalArgumentException("unsupported cast " + expr);
            }
        } else if (v instanceof UnopExpr)
        {
            UnopExpr e = (UnopExpr)v;

            boolean opConstant;
            String op;
            if (e.getOp() instanceof Constant) {
                op = constantToSymbolicExpr((Constant)e.getOp());
                opConstant = true;
            } else {
                op = ((Local)e.getOp()).getName();
                opConstant = false;
            }

            Local l = (Local)e.getOp();

            String symbol;
            if (e instanceof NegExpr)
            {
                symbol = "-";
            } else if (e instanceof LengthExpr)
            {
                // TODO
                throw new UnsupportedOperationException("arrays not yet supported");
            } else
            {
                throw new IllegalArgumentException("unexpected UnopExpr: " + e + " (" + e.getClass().getName() + ")");
            }

            return Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(unaryOp.makeRef(),
                    StringConstant.v(symbol), StringConstant.v(op), IntConstant.v(opConstant ? 1 : 0)));
        } else if (v instanceof BinopExpr) {
            BinopExpr e = (BinopExpr)v;

            boolean op1Constant, op2Constant;
            String op1, op2;
            if (e.getOp1() instanceof Constant) {
                op1 = constantToSymbolicExpr((Constant)e.getOp1());
                op1Constant = true;
            } else {
                op1 = ((Local)e.getOp1()).getName();
                op1Constant = false;
            }
            if (e.getOp2() instanceof Constant) {
                op2 = constantToSymbolicExpr((Constant)e.getOp2());
                op2Constant = true;
            } else {
                op2 = ((Local)e.getOp2()).getName();
                op2Constant = false;
            }

            if (e instanceof CmpExpr) {
                return Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(cmp.makeRef(),
                        StringConstant.v(op1), IntConstant.v(op1Constant ? 1 : 0), StringConstant.v(op2), IntConstant.v(op2Constant ? 1 : 0)));
            } else if (e instanceof CmplExpr) {
                return Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(cmpl.makeRef(),
                        StringConstant.v(op1), IntConstant.v(op1Constant ? 1 : 0), StringConstant.v(op2), IntConstant.v(op2Constant ? 1 : 0)));
            } else if (e instanceof CmpgExpr) {
                return Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(cmpg.makeRef(),
                        StringConstant.v(op1), IntConstant.v(op1Constant ? 1 : 0), StringConstant.v(op2), IntConstant.v(op2Constant ? 1 : 0)));
            } else {
                return Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(binaryOp.makeRef(),
                        StringConstant.v(e.getSymbol()),
                        StringConstant.v(op1), IntConstant.v(op1Constant ? 1 : 0), StringConstant.v(op2), IntConstant.v(op2Constant ? 1 : 0)));
            }
        } else {
            throw new IllegalArgumentException("unsupported value: " + v + " (" + v.getClass().getName() + ")");
        }
    }

    // inputs: invoke expression, and a local string where a temporary value can be stored
    // return value: list of statements to insert before the invoke expression
    private void symbolicInvocation(InvokeExpr expr, Local opTmp, List<Unit> output) {
        SootMethod m = expr.getMethod();
        if (m.hasTag(InstrumentedTag.NAME)) {
            Body b = m.retrieveActiveBody();
            int i = 0;
            for (Local paramLocal : b.getParameterLocals()) {
                output.add(obtainSymbolicValue(expr.getArg(i), opTmp));
                output.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignmentToParameter.makeRef(),
                        StringConstant.v(paramLocal.getName()),
                        StringConstant.v(b.getMethod().getName()),
                        opTmp)));
                ++i;
            }
        }
    }

    private void processMethod(Body body)
    {
        SootMethod m = body.getMethod();

        if (m.getName().equals("<init>"))
        {
            // TODO remove this when we have support for classes
            return;
        }

        if (m.isStatic() && m.getReturnType().equals(VoidType.v()) && m.getParameterCount() == 1 &&
                m.getParameterType(0).equals(ArrayType.v(RefType.v("java.lang.String"), 1)) && m.getName().equals("main"))
        {
            // skip Java main method
            return;
        }

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

            // variable for each static field access
            Set<Integer> staticFieldsDeclared = new HashSet<>();
            while (it2.hasNext())
            {
                Unit u = it2.next();
                for (ValueBox vb : u.getUseAndDefBoxes()) {
                    Value v = vb.getValue();
                    if (v instanceof StaticFieldRef) {
                        StaticFieldRef s = (StaticFieldRef)v;
                        SootField sf = s.getField();
                        int key = sf.equivHashCode();
                        if (staticFieldsDeclared.add(key))
                        {
                            String varName = varNameForStaticField(sf);
                            units.insertBefore(
                                    Arrays.asList(
                                            Jimple.v().newAssignStmt(varTypeTmp, Jimple.v().newStaticFieldRef(sootTypeToSymbolicType(sf.getType()).makeRef())),
                                            Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addVariableIfNotPresent.makeRef(), varTypeTmp,
                                                    StringConstant.v(varName), IntConstant.v(key)))), first);
                        }
                    }
                }
            }
        }

        // if entry-point, add entry-point header
        boolean isEntryPoint = false;
        VisibilityAnnotationTag vat = (VisibilityAnnotationTag) body.getMethod().getTag("VisibilityAnnotationTag");
        if (vat != null) {
            for (AnnotationTag tag : vat.getAnnotations()) {
                if (tag.getType().equals("Lcsci699cav/Concolic$Entrypoint;"))
                {
                    isEntryPoint = true;
                    break;
                }
            }
        }

        if (isEntryPoint)
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
                units.insertBefore(Arrays.asList(
                        obtainSymbolicValue(retVal, opTmp1),
                        Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignmentToReturnValue.makeRef(), opTmp1))
                ), exitFrameStmt);
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
                    if (expr.getMethod().getDeclaringClass().getName().equals("csci699cav.Concolic"))
                    {
                        if (expr.getMethod().getName().startsWith("input"))
                        {
                            units.insertAfter(Arrays.asList(
                                    obtainSymbolicValue(leftOp, opTmp1),
                                    Jimple.v().newAssignStmt(opTmp2, Jimple.v().newStaticInvokeExpr(lastInput.makeRef())),
                                    Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignment.makeRef(), opTmp1, opTmp2))
                            ), s);
                        } else {
                            throw new IllegalArgumentException("encountered unexpected method call from Concolic class: " + expr);
                        }
                    } else
                    {
                        List<Unit> toInsert = new ArrayList<>();
                        symbolicInvocation(expr, opTmp1, toInsert);
                        toInsert.add(obtainSymbolicValue(leftOp, opTmp1));
                        units.insertBefore(toInsert, s);
                        toInsert.clear();
                        if (!expr.getMethod().hasTag(InstrumentedTag.NAME)) {
                            // encountered method for which we cannot do symbolic execution; use its concrete return value
                            if (leftOp.getType() instanceof PrimType) {
                                toInsert.add(Jimple.v().newInvokeStmt(
                                        Jimple.v().newStaticInvokeExpr(addConcreteAssignment.get(leftOp.getType()).makeRef(), opTmp1, leftOp)));
                            } else {
                                toInsert.add(Jimple.v().newInvokeStmt(
                                        Jimple.v().newStaticInvokeExpr(addConcreteAssignment.get(RefType.v("java.lang.Object")).makeRef(), opTmp1, leftOp)));
                            }
                            System.out.println("Warning: Encountered method call to " + expr.getMethod().getSignature() + " for which symbolic execution is unavailable, will use concrete return value");
                            isComplete = false;
                        } else {
                            toInsert.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignmentFromReturnValue.makeRef(), opTmp1)));
                        }
                        units.insertAfter(toInsert, s);
                    }
                } else {
                    units.insertBefore(Arrays.asList(
                            obtainSymbolicValue(leftOp, opTmp1),
                            obtainSymbolicValue(rightOp, opTmp2),
                            Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignment.makeRef(), opTmp1, opTmp2))
                    ), s);
                }
            } else if (s instanceof InvokeStmt) {
                InvokeStmt stmt = (InvokeStmt)s;
                List<Unit> toInsert = new ArrayList<>();
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
                Unit obtainSym = obtainSymbolicValue(stmt.getCondition(), opTmp1);
                units.insertBefore(Arrays.asList(
                        Jimple.v().newIfStmt(stmt.getCondition(), yes),
                        Jimple.v().newAssignStmt(condTmp, IntConstant.v(0)),
                        Jimple.v().newGotoStmt(obtainSym),
                        yes,
                        obtainSym,
                        Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addPathConstraint.makeRef(), IntConstant.v(branchId), opTmp1, condTmp))
                ), s);
            }
        }

        m.addTag(new InstrumentedTag());
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        new SwitchToIfStmtTransformer().internalTransform(phaseName, options);
        new SameTypesBinOpTransformer().internalTransform(phaseName, options);

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
        newFrame = sc.getMethodByName("newFrame");
        exitFrame = sc.getMethodByName("exitFrame");
        addVariable = sc.getMethodByName("addVariable");
        addVariableIfNotPresent = sc.getMethodByName("addVariableIfNotPresent");
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

        Set<SootMethod> allMethods = new HashSet<>();
        for(SootClass c : Scene.v().getApplicationClasses())
        {
            allMethods.addAll(c.getMethods());
        }
        CHATransformer.v().transform("", new HashMap<String, String>()
        {
            {
                put("enabled", "true");
                put("apponly", "true");
            }
        });

        // instrument methods in reverse topological order
        StringCallGraph scg = new StringCallGraph(Scene.v().getCallGraph(), allMethods);
        for (NewNode node : scg.getRTOdering())
        {
            SootMethod m = node.getMethod();
            if (m != null && m.isConcrete() && m.getSource() != null) {
                Body b = m.retrieveActiveBody();
                processMethod(b);

                // System.out.println(b); // DEBUG
            }
        }

        if (!isComplete) {
            System.out.println("Warning: Complete exploration of program paths will not be possible due to issues listed above");
        }
    }
}
