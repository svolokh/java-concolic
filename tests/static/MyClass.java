import csci699cav.Concolic;

public class MyClass {
    private static int sum = 0;

    public static void addNum(int x) {
        if (x % 3 == 0) {
            return;
        }
        sum += x;
    }

    @Concolic.Entrypoint
    public static void run() {
        for (int i = 0; i != 5; ++i) {
            int x = Concolic.inputInt();
            addNum(x);
        }
        if (sum == 100) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
