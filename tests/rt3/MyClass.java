import csci699cav.Concolic;

public class MyClass {
    @Concolic.Entrypoint
    public static void run() {
        int i = Concolic.inputInt();
        if (Integer.bitCount(i) == 12) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
