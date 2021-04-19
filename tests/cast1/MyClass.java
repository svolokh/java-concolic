import csci699cav.Concolic;

public class MyClass {
    @Concolic.Entrypoint
    public static void run() {
        int i = Concolic.inputInt();
        double f = ((double)i) + 0.5f;
        if (f == 11.5f) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
