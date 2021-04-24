package csci699cav;

import soot.*;
import soot.asm.AsmClassSource;
import soot.jimple.StringConstant;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;

import java.lang.reflect.Field;

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

    public static boolean hasEntryPointAnnotation(SootMethod m) {
        boolean isEntryPoint = false;
        VisibilityAnnotationTag vat = (VisibilityAnnotationTag) m.getTag("VisibilityAnnotationTag");
        if (vat != null) {
            for (AnnotationTag tag : vat.getAnnotations()) {
                if (tag.getType().equals("Lcsci699cav/Concolic$Entrypoint;"))
                {
                    isEntryPoint = true;
                    break;
                }
            }
        }
        return isEntryPoint;
    }

    public static String getFilePathForClass(SootClass sc) {
        AsmClassSource src = ((AsmClassSource)SourceLocator.v().getClassSource(sc.getName()));
        try {
            Field f = AsmClassSource.class.getDeclaredField("foundFile");
            f.setAccessible(true);
            FoundFile ff = (FoundFile)f.get(src);
            return ff.getFilePath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // decision for which methods to avoid instrumenting
    public static boolean doNotInstrument(SootMethod m) {
        SootClass sc = m.getDeclaringClass();
        String className = sc.getName();
        String methodName = m.getName();

        // ignore any methods that contain string constants (we currently do not support them)
        if (m.getSource() != null) {
            Body b = m.retrieveActiveBody();
            for (Unit u : b.getUnits()) {
                for (ValueBox vb : u.getUseAndDefBoxes()) {
                    if (vb.getValue() instanceof StringConstant) {
                        return true;
                    }
                }
            }
        }

        {
            // do not instrument any exception classes
            SootClass c = sc;
            while (c.hasSuperclass()) {
                SootClass supClass = c.getSuperclass();
                if (supClass.getName().equals("java.lang.Throwable")) {
                    return true;
                }
                c = supClass;
            }
        }

        return (className.equals("csci699cav.Concolic") && !methodName.equals("assertTrue")
                    && !methodName.equals("assertFalse")
                    && !methodName.equals("assume"))
                || className.equals("java.lang.System")
                || className.equals("java.lang.Thread")
                || className.equals("java.io.PrintStream")
                || className.equals("java.io.Writer")
                || className.equals("java.io.OutputStreamWriter")
                || className.equals("java.lang.Object")
                || className.equals("java.lang.Class")
                || className.equals("java.lang.String")
                || (className.startsWith("java.") && methodName.equals("<clinit>"));
    }
}
