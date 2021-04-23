import csci699cav.Concolic;

public class MyClass {
    @Concolic.Entrypoint
    public static void run() {
        Boolean b = Concolic.inputBoolean();
        if (b) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
