import csci699cav.Concolic;

public class MyClass {
    @Concolic.Entrypoint
    public static void run() {
        double sum = 0.0f;
        for (int i = 0; i != 3; ++i) {
            double d = Concolic.inputDouble();
            sum += (float)d;
        }
        if (sum == 112.3) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        run();
    }
}