import csci699cav.Concolic;

public class MyClass {
    private static int sum;

    static {
        sum = 100;
    }


    @Concolic.Entrypoint
    public static void run() {
        Concolic.assertFalse(Concolic.inputInt() == sum);
    }

    public static void main(String[] args) {
        run();
    }
}
