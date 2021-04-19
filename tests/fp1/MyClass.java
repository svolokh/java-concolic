import csci699cav.Concolic;

public class MyClass {
    @Concolic.Entrypoint
    public static void run() {
        float sum = 0.0f;
        for (int i = 0; i != 5; ++i) {
            float x = Concolic.inputInt() + 0.4f;
            sum += x;
        }
        if (sum == 12.0f) {
            double d = (double)Concolic.inputInt() + 2.0;
            if (d == 4.0) {
                System.exit(1);
            }
        }
    }

    public static void main(String[] args) {
        run();
    }
}
