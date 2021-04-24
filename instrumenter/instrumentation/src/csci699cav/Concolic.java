package csci699cav;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Concolic {
    @Retention(RetentionPolicy.CLASS)
    public static @interface Entrypoint {}

    public static void assume(boolean condition) {
        if (!condition) {
            System.exit(0);
        }
    }

    public static void assertTrue(boolean condition) {
        if (!condition) {
            System.exit(1);
        }
    }

    public static void assertFalse(boolean condition) {
        if (condition) {
            System.exit(1);
        }
    }

    public static byte inputByte() {
        String givenVal = System.getenv("JAVA_CONCOLIC_INPUT" + ConcolicState.inputCounter);
        byte result;
        if (givenVal == null) {
            result = (byte)(ConcolicState.rng.nextInt(256) - 128);
        } else {
            result = (byte)Integer.parseInt(givenVal);
        }
        ConcolicState.addInput(VariableType.BYTE, Integer.toString((int)result));
        return result;
    }

    public static short inputShort() {
        String givenVal = System.getenv("JAVA_CONCOLIC_INPUT" + ConcolicState.inputCounter);
        short result;
        if (givenVal == null) {
            result = (short)(ConcolicState.rng.nextInt(65536) - 32768);
        } else {
            result = (short)Integer.parseInt(givenVal);
        }
        ConcolicState.addInput(VariableType.SHORT, Short.toString(result));
        return result;
    }

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

    public static long inputLong() {
        String givenVal = System.getenv("JAVA_CONCOLIC_INPUT" + ConcolicState.inputCounter);
        long result;
        if (givenVal == null) {
            result = ConcolicState.rng.nextLong();
        } else {
            result = Long.parseLong(givenVal);
        }
        ConcolicState.addInput(VariableType.LONG, Long.toString(result));
        return result;
    }

    public static float inputFloat() {
        String givenVal = System.getenv("JAVA_CONCOLIC_INPUT" + ConcolicState.inputCounter);
        float result;
        if (givenVal == null) {
            result = Float.intBitsToFloat(ConcolicState.rng.nextInt());
        } else {
            result = Float.intBitsToFloat(Integer.parseInt(givenVal));
        }
        ConcolicState.addInput(VariableType.FLOAT, Float.toString(result));
        return result;
    }

    public static double inputDouble() {
        String givenVal = System.getenv("JAVA_CONCOLIC_INPUT" + ConcolicState.inputCounter);
        double result;
        if (givenVal == null) {
            result = Double.longBitsToDouble(ConcolicState.rng.nextLong());
        } else {
            result = Double.longBitsToDouble(Long.parseLong(givenVal));
        }
        ConcolicState.addInput(VariableType.DOUBLE, Double.toString(result));
        return result;
    }

    public static boolean inputBoolean() {
        String givenVal = System.getenv("JAVA_CONCOLIC_INPUT" + ConcolicState.inputCounter);
        boolean result;
        if (givenVal == null) {
            result = ConcolicState.rng.nextBoolean();
        } else {
            result = !givenVal.equals("0");
        }
        ConcolicState.addInput(VariableType.BYTE, result ? "true" : "false");
        return result;
    }

    public static char inputChar() {
        String givenVal = System.getenv("JAVA_CONCOLIC_INPUT" + ConcolicState.inputCounter);
        char result;
        if (givenVal == null) {
            result = (char)((short)(ConcolicState.rng.nextInt(65536) - 32768));
        } else {
            result = (char)((short)Integer.parseInt(givenVal));
        }
        ConcolicState.addInput(VariableType.CHAR, Short.toString((short)result));
        return result;
    }
}