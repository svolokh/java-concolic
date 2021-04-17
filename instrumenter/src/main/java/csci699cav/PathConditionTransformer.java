package csci699cav;

import soot.*;
import soot.jimple.*;

import java.util.*;
import java.util.stream.Collectors;

public class PathConditionTransformer extends SceneTransformer {
    private SootMethod init;
    private SootMethod lastInput;
    private SootMethod local;
    private SootMethod retVar;
    private SootMethod unaryOp;
    private SootMethod binaryOp;
    private SootMethod peekNextFrame;
    private SootMethod newFrame;
    private SootMethod exitFrame;
    private SootMethod addVariable;
    private SootMethod addAssignment;
    private SootMethod addAssignmentToParameter;
    private SootMethod addAssignmentFromReturnValue;
    private SootMethod addAssignmentToReturnValue;
    private SootMethod addPathConstraint;

    private SootField varTypeByte;
    private SootField varTypeShort;
    private SootField varTypeInt;
    private SootField varTypeLong;
    private SootField varTypeFloat;
    private SootField varTypeDouble;
    private SootField varTypeBoolean;
    private SootField varTypeChar;

    private int branchIdCounter = 0;

    private SootField sootTypeToSymbolicType(Type t) {
        if (t instanceof BooleanType) {
            return varTypeBoolean;
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

    // generates statement to produce a symbolic value for the given value
    private Unit obtainSymbolicValue(Value v, Local outVar) {
        if (v instanceof Constant) {
            return Jimple.v().newAssignStmt(outVar, StringConstant.v(v.toString()));
        } else if (v instanceof Local) {
            Local l = (Local)v;
            return Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(local.makeRef(), StringConstant.v(l.getName())));
        } else if (v instanceof UnopExpr)
        {
            UnopExpr e = (UnopExpr)v;

            assert e.getOp() instanceof Local || e.getOp() instanceof Constant;
            boolean opConstant;
            String op;
            if (e.getOp() instanceof Constant) {
                op = e.getOp().toString();
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
            assert e.getOp1() instanceof Local || e.getOp1() instanceof Constant;
            assert e.getOp2() instanceof Local || e.getOp2() instanceof Constant;

            boolean op1Constant, op2Constant;
            String op1, op2;
            if (e.getOp1() instanceof Constant) {
                op1 = e.getOp1().toString();
                op1Constant = true;
            } else {
                op1 = ((Local)e.getOp1()).getName();
                op1Constant = false;
            }
            if (e.getOp2() instanceof Constant) {
                op2 = e.getOp2().toString();
                op2Constant = true;
            } else {
                op2 = ((Local)e.getOp2()).getName();
                op2Constant = false;
            }

            return Jimple.v().newAssignStmt(outVar, Jimple.v().newStaticInvokeExpr(binaryOp.makeRef(),
                    StringConstant.v(e.getSymbol()),
                    StringConstant.v(op1), IntConstant.v(op1Constant ? 1 : 0), StringConstant.v(op2), IntConstant.v(op2Constant ? 1 : 0)));
        } else {
            throw new IllegalArgumentException("unsupported value: " + v + " (" + v.getClass().getName() + ")");
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
        if (!m.getReturnType().equals(VoidType.v()))
        {
            units.insertBefore(
                    Arrays.asList(
                            Jimple.v().newAssignStmt(varTypeTmp, Jimple.v().newStaticFieldRef(sootTypeToSymbolicType(m.getReturnType()).makeRef())),
                            Jimple.v().newAssignStmt(opTmp1, Jimple.v().newStaticInvokeExpr(retVar.makeRef())),
                            Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addVariable.makeRef(), varTypeTmp, opTmp1))), first);
        }
        for (Local paramLocal : origLocals)
        {
            units.insertBefore(
                    Arrays.asList(
                            Jimple.v().newAssignStmt(varTypeTmp, Jimple.v().newStaticFieldRef(sootTypeToSymbolicType(paramLocal.getType()).makeRef())),
                            Jimple.v().newAssignStmt(opTmp1,
                                    Jimple.v().newStaticInvokeExpr(local.makeRef(), StringConstant.v(paramLocal.getName()))),
                            Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addVariable.makeRef(), varTypeTmp, opTmp1))
                    ), first);
        }

        // if entry-point, add entry-point header
        boolean isEntryPoint = body.getMethod().getName().equals("h");
        if (isEntryPoint)
        {
            {
                Unit u = units.getFirst();
                while (u instanceof IdentityStmt)
                {
                    u = units.getSuccOf(u);
                }
                units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(init.makeRef())), u);
            }

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
                        Body b = expr.getMethod().retrieveActiveBody();
                        List<Unit> toInsert = new ArrayList<>();
                        int i = 0;
                        for (Local paramLocal : b.getParameterLocals()) {
                            toInsert.add(obtainSymbolicValue(expr.getArg(i), opTmp1));
                            toInsert.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignmentToParameter.makeRef(),
                                    StringConstant.v(paramLocal.getName()),
                                    StringConstant.v(b.getMethod().getName()),
                                    opTmp1)));
                            ++i;
                        }
                        toInsert.add(obtainSymbolicValue(leftOp, opTmp1));
                        units.insertBefore(toInsert, s);
                        units.insertAfter(
                                Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignmentFromReturnValue.makeRef(), opTmp1)), s);
                    }
                } else {
                    units.insertBefore(Arrays.asList(
                            obtainSymbolicValue(leftOp, opTmp1),
                            obtainSymbolicValue(rightOp, opTmp2),
                            Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(addAssignment.makeRef(), opTmp1, opTmp2))
                    ), s);
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
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        new SwitchToIfStmtTransformer().internalTransform(phaseName, options);

        SootClass sc = Scene.v().loadClassAndSupport("csci699cav.ConcolicState");
        SootClass variableTypeClass = Scene.v().loadClassAndSupport("csci699cav.VariableType");

        init = sc.getMethodByName("init");
        lastInput = sc.getMethodByName("lastInput");
        local = sc.getMethodByName("local");
        retVar = sc.getMethodByName("retVar");
        unaryOp = sc.getMethodByName("unaryOp");
        binaryOp = sc.getMethodByName("binaryOp");
        peekNextFrame = sc.getMethodByName("peekNextFrame");
        newFrame = sc.getMethodByName("newFrame");
        exitFrame = sc.getMethodByName("exitFrame");
        addVariable = sc.getMethodByName("addVariable");
        addAssignment = sc.getMethodByName("addAssignment");
        addAssignmentToParameter = sc.getMethodByName("addAssignmentToParameter");
        addAssignmentFromReturnValue = sc.getMethodByName("addAssignmentFromReturnValue");
        addAssignmentToReturnValue = sc.getMethodByName("addAssignmentToReturnValue");
        addPathConstraint = sc.getMethodByName("addPathConstraint");

        varTypeByte = variableTypeClass.getFieldByName("BYTE");
        varTypeShort = variableTypeClass.getFieldByName("SHORT");
        varTypeInt = variableTypeClass.getFieldByName("INT");
        varTypeLong = variableTypeClass.getFieldByName("LONG");
        varTypeFloat = variableTypeClass.getFieldByName("FLOAT");
        varTypeDouble = variableTypeClass.getFieldByName("DOUBLE");
        varTypeBoolean = variableTypeClass.getFieldByName("BOOLEAN");
        varTypeChar = variableTypeClass.getFieldByName("CHAR");

        for (SootClass c : Scene.v().getApplicationClasses())
        {
            for (SootMethod m : c.getMethods())
            {
                Body b = m.retrieveActiveBody();
                processMethod(b);

                System.out.println(b); // DEBUG
            }
        }
    }
}
