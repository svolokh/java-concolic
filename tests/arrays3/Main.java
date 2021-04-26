import csci699cav.Concolic;

public class Main {
    static float[] a;

    @Concolic.Entrypoint
    public static void run() {
        a = new float[5];
        for (int i = 0; i != a.length; ++i) {
            int x = Concolic.inputInt();
            a[i] = (float)x;
        }
        long sum = 0;
        for (int i = 0; i != a.length; ++i) {
            sum += a[i];
        }
        Concolic.assertFalse(sum == 200);
    }

    public static void main(String[] args) {
        run();
    }
}
