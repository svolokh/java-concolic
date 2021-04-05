package csci699cav;

import soot.Scene;
import soot.options.Options;

public class TestUtils {

    public static void setUpScene()
    {
        Options.v().set_drop_bodies_after_load(false);
        Scene.v().loadBasicClasses();
        Scene.v().setSootClassPath(Scene.v().defaultClassPath());
        Scene.v().extendSootClassPath("test-classes");
    }

}
