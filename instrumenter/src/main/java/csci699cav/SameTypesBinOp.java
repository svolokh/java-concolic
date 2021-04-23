package csci699cav;

import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.Jimple;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// transforms all binary operations on primitives so that the types of both operands in the expression are the same
public class SameTypesBinOp {
    public static void processMethod(Body b) {
        Map<PrimType, Local> tempLocals = new HashMap<>();
        UnitPatchingChain units = b.getUnits();
        Iterator<Unit> it = units.snapshotIterator();
        while (it.hasNext()) {
            Unit u = it.next();
            if (u instanceof AssignStmt) {
                AssignStmt stmt = (AssignStmt)u;
                if (stmt.getRightOp() instanceof BinopExpr) {
                    BinopExpr expr = (BinopExpr)stmt.getRightOp();
                    Value op1 = expr.getOp1();
                    Value op2 = expr.getOp2();
                    if (!(op1.getType() instanceof PrimType) || !(op2.getType() instanceof PrimType)) {
                        continue;
                    }
                    PrimType t1 = (PrimType)op1.getType();
                    PrimType t2 = (PrimType)op2.getType();
                    if (!Utils.isBitVectorType(t1) || !Utils.isBitVectorType(t2)) {
                        continue;
                    }
                    int s1 = Utils.bitVectorSize(t1);
                    int s2 = Utils.bitVectorSize(t2);
                    if (s1 < s2) {
                        Local l = tempLocals.get(t2);
                        if (l == null) {
                            l = Jimple.v().newLocal("cast" + t2.getClass().getSimpleName(), t2);
                            b.getLocals().add(l);
                            tempLocals.put(t2, l);
                        }
                        units.insertBefore(Jimple.v().newAssignStmt(l, Jimple.v().newCastExpr(op1, t2)), u);
                        expr.setOp1(l);
                    } else if (s1 > s2) {
                        Local l = tempLocals.get(t1);
                        if (l == null) {
                            l = Jimple.v().newLocal("cast" + t1.getClass().getSimpleName(), t1);
                            b.getLocals().add(l);
                            tempLocals.put(t1, l);
                        }
                        units.insertBefore(Jimple.v().newAssignStmt(l, Jimple.v().newCastExpr(op2, t1)), u);
                        expr.setOp2(l);
                    }
                }
            }
        }
    }
}
