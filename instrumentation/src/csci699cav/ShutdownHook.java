package csci699cav;

import java.io.*;

public class ShutdownHook implements Runnable {
    @Override
    public void run() {
        File f = new File("/home/sasha-usc/Documents/test.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f)))
        {
            for (Variable var : ConcolicState.variables)
            {
                bw.write(var.type.name());
                bw.write(" ");
                bw.write(var.id);
                bw.write("\n");
            }

            bw.write("\n");

            for (Assignment assign : ConcolicState.assignments)
            {
                bw.write(assign.leftOp);
                bw.write(" = ");
                bw.write(assign.rightOp);
                bw.write("\n");
            }

            bw.write("\n");

            for (PathConstraint pc : ConcolicState.pathConstraints)
            {
                bw.write(Integer.toString(pc.branchId));
                bw.write("; ");
                bw.write(pc.condition);
                bw.write("; ");
                bw.write(pc.conditionConcrete ? "true" : "false");
                bw.write("\n");
            }
        } catch (IOException e)
        {
            System.err.println("java-concolic: exception when writing instrumentation");
            e.printStackTrace();
        }
    }
}