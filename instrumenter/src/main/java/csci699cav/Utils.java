package csci699cav;

import soot.*;

public class Utils {
    public static int bitVectorSize(PrimType t) {
        if (t instanceof ByteType) {
            return 8;
        } else if (t instanceof CharType) {
            return 16;
        } else if (t instanceof ShortType) {
            return 16;
        } else if (t instanceof IntType) {
            return 32;
        } else if (t instanceof LongType) {
            return 64;
        } else {
            throw new IllegalArgumentException("unsupported type " + t);
        }
    }

    public static boolean isBitVectorType(PrimType t) {
        return t instanceof IntegerType || t instanceof LongType;
    }
}
