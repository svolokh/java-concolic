import csci699cav.Concolic;

public class Main {
    @Concolic.Entrypoint
    public static void run() {
        int x = Concolic.inputInt();
        int y = Concolic.inputInt();
        Concolic.assume(x > 0 && x <= 10);
        Concolic.assume(y > 0 && y <= 10);
        Concolic.assertFalse(Math.max(x, y) > 2);
    }

    public static void main(String[] args) {
        run();
    }
}
