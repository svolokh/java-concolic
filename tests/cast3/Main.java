import csci699cav.Concolic;

public class Main {
    @Concolic.Entrypoint
    public static void run() {
        char c = Concolic.inputChar();
        short s = Concolic.inputShort();
        int i = Concolic.inputInt();
        long l = Concolic.inputLong();

        Concolic.assume(c > 0);
        Concolic.assume(s > 0);
        Concolic.assume(i > 0);
        Concolic.assume(l > 0);

        int x = c + s;
        int y = s + i;
        long z = i + l;
        long w = x + y + z;
        short r = (short)w;

        Concolic.assertFalse(r == 1234);
    }

    public static void main(String[] args) {
        run();
    }
}
