package csci699cav;

import soot.*;
import soot.jimple.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SwitchToIfStmtTransformer extends SceneTransformer {
    private void processMethod(Body b) {
        UnitPatchingChain units = b.getUnits();
        Iterator<Unit> it = units.snapshotIterator();
        while (it.hasNext())
        {
            Unit u = it.next();
            if (u instanceof TableSwitchStmt) {
                List<Unit> insertUnits = new LinkedList<>();
                TableSwitchStmt stmt = (TableSwitchStmt)u;
                int index = stmt.getLowIndex();
                int highIndex = stmt.getHighIndex();
                Value key = stmt.getKey();
                for (int i = 0, n = highIndex - index + 1; i != n; ++i, ++index) {
                    insertUnits.add(Jimple.v().newIfStmt(
                            Jimple.v().newEqExpr(key, IntConstant.v(index)),
                            stmt.getTarget(i)));
                }
                insertUnits.add(Jimple.v().newGotoStmt(stmt.getDefaultTarget()));
                units.insertAfter(insertUnits, u);
                units.remove(u);
            } else if (u instanceof LookupSwitchStmt) {
                List<Unit> insertUnits = new LinkedList<>();
                LookupSwitchStmt stmt = (LookupSwitchStmt)u;
                Value key = stmt.getKey();
                int i = 0;
                for (IntConstant ic : stmt.getLookupValues())
                {
                    insertUnits.add(Jimple.v().newIfStmt(
                            Jimple.v().newEqExpr(key, ic), stmt.getTarget(i)));
                    ++i;
                }
                insertUnits.add(Jimple.v().newGotoStmt(stmt.getDefaultTarget()));
                units.insertAfter(insertUnits, u);
                units.remove(u);
            }
        }
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> map) {
        for (SootClass sc : Scene.v().getApplicationClasses())
        {
            for (SootMethod m : sc.getMethods())
            {
                Body b = m.retrieveActiveBody();
                processMethod(b);
            }
        }
    }
}
