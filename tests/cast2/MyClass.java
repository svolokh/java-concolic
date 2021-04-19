import csci699cav.Concolic;

public class MyClass {
    public static long f(long x) {
        return 2*x + 2L;
    }

    @Concolic.Entrypoint
    public static void run() {
        int i = (int)f((long)Concolic.inputInt());
        short s = (short)Concolic.inputInt();
        if (i + s == 10) {
            long x = (long)s;
            long y = (long)Concolic.inputInt();
            if (y > 0) {
                long z = x + y;
                if (z >= 1 && z < 4) {
                    System.exit(1);
                }
            }
        }
    }

    public static void main(String[] args) {
        run();
    }
}
