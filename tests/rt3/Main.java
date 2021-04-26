import csci699cav.Concolic;

public class Main {
    @Concolic.Entrypoint
    public static void run() {
        int i = Concolic.inputInt();
        Concolic.assertFalse(Integer.bitCount(i) == 12);
    }

    public static void main(String[] args) {
        run();
    }
}
