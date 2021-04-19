import csci699cav.Concolic;

public class MyClass {
    @Concolic.Entrypoint
    public static void run() {
        boolean b = Concolic.inputBoolean();
        boolean b2 = Concolic.inputBoolean();
        boolean b3 = Concolic.inputBoolean();
        b = !b;
        b2 = !b2;
        b3 = !b3;
        if (b && b2 && b3) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
