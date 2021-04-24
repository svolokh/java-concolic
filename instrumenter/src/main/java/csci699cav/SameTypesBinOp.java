package csci699cav;

import soot.*;
import soot.jimple.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// transforms all binary operations on primitives so that the types of both operands in the expression are the same
public class SameTypesBinOp {
    public static void processMethod(Body b) {
        Map<PrimType, Local> tempLocalsX = new HashMap<>();
        Map<PrimType, Local> tempLocalsY = new HashMap<>();
        UnitPatchingChain units = b.getUnits();
        Iterator<Unit> it = units.snapshotIterator();
        while (it.hasNext()) {
            Unit u = it.next();
            if (u instanceof AssignStmt) {
                AssignStmt stmt = (AssignStmt)u;
                if (stmt.getRightOp() instanceof BinopExpr) {
                    BinopExpr expr = (BinopExpr)stmt.getRightOp();
                    if (expr instanceof ConditionExpr) {
                        continue;
                    }

                    // z = x <op> y
                    Value z = stmt.getLeftOp();
                    Value x = expr.getOp1();
                    Value y = expr.getOp2();

                    if (!(z.getType() instanceof PrimType) || !(x.getType() instanceof PrimType) || !(y.getType() instanceof PrimType)) {
                        continue;
                    }

                    PrimType tx = (PrimType)x.getType();
                    PrimType ty = (PrimType)y.getType();
                    PrimType tz = (PrimType)z.getType();

                    if (!Utils.isBitVectorType(tz) || !Utils.isBitVectorType(tx) || !Utils.isBitVectorType(ty)) {
                        continue;
                    }

                    int sx = Utils.bitVectorSize(tx);
                    int sy = Utils.bitVectorSize(ty);
                    int sz = Utils.bitVectorSize(tz);

                    if (sz > sx || sz > sy) {
                        Local lx = tempLocalsX.computeIfAbsent(tz, t -> {
                            Local l = Jimple.v().newLocal("cast" + t.getClass().getSimpleName() + "X", t);
                            b.getLocals().add(l);
                            return l;
                        });
                        Local ly = tempLocalsY.computeIfAbsent(tz, t -> {
                            Local l = Jimple.v().newLocal("cast" + t.getClass().getSimpleName() + "Y", t);
                            b.getLocals().add(l);
                            return l;
                        });
                        units.insertBefore(Arrays.asList(
                                Jimple.v().newAssignStmt(lx, Jimple.v().newCastExpr(x, tz)),
                                Jimple.v().newAssignStmt(ly, Jimple.v().newCastExpr(y, tz))
                        ), u);
                        expr.setOp1(lx);
                        expr.setOp2(ly);
                    }
                }
            }
        }
    }
}
