import csci699cav.Concolic;
import java.util.Arrays;

public class MyClass {

    @Concolic.Entrypoint
    public static void run() {
        int[] i = new int[] {Concolic.inputInt(), Concolic.inputInt(), Concolic.inputInt()};
        if (Arrays.binarySearch(i, 22) >= 0) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
