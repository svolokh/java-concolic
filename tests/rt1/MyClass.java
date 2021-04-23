import csci699cav.Concolic;

public class MyClass {
    @Concolic.Entrypoint
    public static void run() {
        int x = Concolic.inputInt();
        int y = Concolic.inputInt();
        if (Math.max(x, y) > 2) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
