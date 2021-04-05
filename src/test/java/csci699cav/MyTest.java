package csci699cav;

import org.junit.Test;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class MyTest {

    @Test
    public void test() {
        TestUtils.setUpScene();

        SootClass sc = Scene.v().loadClassAndSupport("MyClass");
        for (SootMethod m : sc.getMethods())
        {
            System.out.println(m.retrieveActiveBody());
        }

    }

}
