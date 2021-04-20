import csci699cav.Concolic;

public class MyClass {
    @Concolic.Entrypoint
    public static void run() {
        boolean[] arr = new boolean[8];
        for (int i = 0; i != arr.length; ++i) {
            arr[i] = Concolic.inputBoolean();
        }
        int sum = 0;
        int x = 1;
        for (int i = arr.length - 1; i >= 0; --i) {
            if (arr[i]) {
                sum += x;
            }
            x *= 2;
        }
        if (sum == 124) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
