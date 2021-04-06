package csci699cav;

import java.util.Random;

public class Concolic {

    public static int inputInt() {
        String inputVar = "INPUT" + ConcolicState.inputCounter;
        ++ConcolicState.inputCounter;
        String givenVal = System.getenv("JAVA_CONCOLIC_" + inputVar);
        int result;
        if (givenVal == null) {
            result = ConcolicState.rng.nextInt();
        } else {
            result = Integer.parseInt(givenVal);
        }
        ConcolicState.addVariable(VariableType.INT, inputVar);
        return result;
    }

}