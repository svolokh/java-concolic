import csci699cav.Concolic;

public class Main {
    private static int x = 0;

    private static int f(int i) {
        x += i;
        return x;
    }
    
    private static float h(float v) {
        int i = (int)v;
        switch(i % 4) {
            case 0:
                return x;
            case 1:
                return 1.5f*x;
            case 2:
                return 2.0f*x;
            default:
                return x - 1.0f;
        }
    }

    private static float k(float x, float y, float z) {
        return (h(x) + h(y) + h(z))/3.0f;
    }

    private static float g(int d) {
        int i = f(d);
        int j = f(d);
        int k = f(d);
        return k((float)i, (float)j, (float)k);
    }

    @Concolic.Entrypoint
    public static void run() {
        float sum = 0.0f;
        for (int i = 0; i != 3; ++i) {
            int c = Concolic.inputInt();
            sum += g(c);
        }
        Concolic.assertFalse(sum > 20.0f && sum < 30.0f);
    }

    public static void main(String[] args) {
        run();
    }
}
