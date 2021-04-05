package csci699cav;

import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.options.Options;

public class Main {

    public static void main(String[] args) {
        Options.v().set_whole_program(true);
        Options.v().set_drop_bodies_after_load(false);
        Scene.v().addBasicClass("csci699cav.Assignment", SootClass.SIGNATURES);
        Scene.v().addBasicClass("csci699cav.ConcolicState", SootClass.SIGNATURES);
        Scene.v().addBasicClass("csci699cav.PathConstraint", SootClass.SIGNATURES);
        Scene.v().addBasicClass("csci699cav.ShutdownHook", SootClass.SIGNATURES);
        Scene.v().addBasicClass("csci699cav.Variable", SootClass.SIGNATURES);
        Scene.v().addBasicClass("csci699cav.VariableType", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.Runtime", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.util.ArrayList", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.ThreadGroup", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.util.Enumeration", SootClass.SIGNATURES);
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.pct", new PathConditionTransformer()));
        soot.Main.main(args);
    }

}
