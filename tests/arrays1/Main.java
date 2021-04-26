import csci699cav.Concolic;

public class Main {
    @Concolic.Entrypoint
    public static void run() {
        boolean b = true;
        int[] arr = new int[3];
        for (int i = 0; i != arr.length; ++i) {
            arr[i] = Concolic.inputInt();
        }
        int j = Concolic.inputInt();
        Concolic.assertFalse(j >= 1 && j < arr.length && arr[j] + arr[j-1] == 30);
    }

    public static void main(String[] args) {
        run();
    }
}
