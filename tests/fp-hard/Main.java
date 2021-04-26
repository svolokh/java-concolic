import csci699cav.Concolic;

public class Main {
    @Concolic.Entrypoint
    public static void run() {
        double sum = 0.0f;
        for (int i = 0; i != 3; ++i) {
            double d = Concolic.inputDouble();
            sum += (float)d;
        }
        Concolic.assertFalse(sum == 112.3);
    }

    public static void main(String[] args) {
        run();
    }
}
