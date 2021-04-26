import csci699cav.Concolic;
import java.util.Arrays;

public class Main {

    @Concolic.Entrypoint
    public static void run() {
        int[] i = new int[] {Concolic.inputInt(), Concolic.inputInt(), Concolic.inputInt()};
        Concolic.assertFalse(Arrays.binarySearch(i, 22) >= 0);
    }

    public static void main(String[] args) {
        run();
    }
}
