import csci699cav.Concolic;

public class Main {
    @Concolic.Entrypoint
    public static void run() {
        int i = Concolic.inputInt();
        double f = ((double)i) + 0.5f;
        Concolic.assertFalse(f == 11.5f);
    }

    public static void main(String[] args) {
        run();
    }
}
