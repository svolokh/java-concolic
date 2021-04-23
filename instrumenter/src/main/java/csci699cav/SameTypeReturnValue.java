package csci699cav;

import soot.*;
import soot.jimple.Constant;
import soot.jimple.Jimple;
import soot.jimple.ReturnStmt;

import java.util.Arrays;
import java.util.Iterator;

public class SameTypeReturnValue {
    public static void processMethod(Body b) {
        SootMethod m = b.getMethod();
        if (m.getReturnType().equals(VoidType.v())) {
            return;
        }
        Type returnType = m.getReturnType();
        Local tempLocal = null;
        UnitPatchingChain units = b.getUnits();
        Iterator<Unit> it = units.snapshotIterator();
        while (it.hasNext()) {
            Unit u = it.next();
            if (u instanceof ReturnStmt) {
                ReturnStmt stmt = (ReturnStmt)u;
                Value v = stmt.getOp();
                Type opType = v.getType();
                if (!(v instanceof Constant) && !opType.equals(returnType)) {
                    if (tempLocal == null) {
                        tempLocal = Jimple.v().newLocal("tempRetCast", returnType);
                        b.getLocals().add(tempLocal);
                    }
                    units.insertBefore(Arrays.asList(
                            Jimple.v().newAssignStmt(tempLocal, Jimple.v().newCastExpr(stmt.getOp(), returnType)),
                            Jimple.v().newReturnStmt(tempLocal)
                    ), stmt);
                    units.remove(stmt);
                }
            }
        }
    }
}
