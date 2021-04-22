package csci699cav;

import java.io.*;

public class ShutdownHook implements Runnable {
    @Override
    public void run() {
        String outputFile = System.getenv("JAVA_CONCOLIC_OUTPUT");
        if (outputFile == null) {
            return;
        }
        File f = new File(outputFile);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f)))
        {
            for (Variable var : ConcolicState.inputVariables)
            {
                if (var.instanceArray) {
                    bw.write("INSTANCE:");
                }
                bw.write(var.type.name());
                bw.write(" ");
                bw.write(var.id);
                bw.write("\n");
            }

            bw.write("\n");

            for (Variable var : ConcolicState.variables)
            {
                if (var.instanceArray) {
                    bw.write("INSTANCE:");
                }
                bw.write(var.type.name());
                bw.write(" ");
                bw.write(var.id);
                bw.write("\n");
            }

            bw.write("\n");

            for (Assignment assign : ConcolicState.inputAssignments)
            {
                bw.write(assign.leftOp);
                bw.write(" = ");
                bw.write(assign.rightOp);
                bw.write("\n");
            }

            bw.write("\n");

            for (Assignment assign : ConcolicState.fieldInitAssignments)
            {
                bw.write(assign.leftOp);
                bw.write(" = ");
                bw.write(assign.rightOp);
                bw.write("\n");
            }
            for (Assignment assign : ConcolicState.assignments)
            {
                bw.write(assign.leftOp);
                bw.write(" = ");
                bw.write(assign.rightOp);
                bw.write("\n");
            }

            bw.write("\n");

            int assignmentIndexOffset = ConcolicState.fieldInitAssignments.size();
            for (PathConstraint pc : ConcolicState.pathConstraints)
            {
                bw.write(Integer.toString(pc.branchId));
                bw.write("; ");
                bw.write(pc.condition);
                bw.write("; ");
                bw.write(pc.conditionConcrete ? "true" : "false");
                bw.write("; ");
                bw.write(Integer.toString(pc.assignmentIndex + assignmentIndexOffset));
                bw.write("\n");
            }
        } catch (IOException e)
        {
            System.err.println("java-concolic: exception when writing instrumentation");
            e.printStackTrace();
        }
    }
}