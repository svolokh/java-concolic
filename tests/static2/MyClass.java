import csci699cav.Concolic;

public class MyClass {
    private static int sum;

    static {
        sum = 100;
    }


    @Concolic.Entrypoint
    public static void run() {
        if (Concolic.inputInt() == sum) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
