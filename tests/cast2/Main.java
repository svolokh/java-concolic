import csci699cav.Concolic;

public class Main {
    public static long f(long x) {
        return 2*x + 2L;
    }

    @Concolic.Entrypoint
    public static void run() {
        int i = (int)f((long)Concolic.inputInt());
        short s = (short)Concolic.inputInt();
        Concolic.assume(i + s == 10);

        long x = (long)s;
        long y = (long)Concolic.inputInt();
        Concolic.assume(y > 0);

        long z = x + y;
        Concolic.assertFalse(z >= 1 && z < 4);
    }

    public static void main(String[] args) {
        run();
    }
}
