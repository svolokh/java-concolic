package csci699cav;

import java.util.Random;

public class Concolic {

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