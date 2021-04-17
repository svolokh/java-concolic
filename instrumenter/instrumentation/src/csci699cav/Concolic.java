package csci699cav;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Concolic {

    @Retention(RetentionPolicy.CLASS)
    public static @interface Entrypoint {}

    public static int inputInt() {
        String givenVal = System.getenv("JAVA_CONCOLIC_INPUT" + ConcolicState.inputCounter);
        int result;
        if (givenVal == null) {
            result = ConcolicState.rng.nextInt();
        } else {
            result = Integer.parseInt(givenVal);
        }
        ConcolicState.addInput(VariableType.INT, Integer.toString(result));
        return result;
    }

}