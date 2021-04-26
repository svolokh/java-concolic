import csci699cav.Concolic;

import java.util.ArrayList;

public class Main {
    @Concolic.Entrypoint
    public static void run() {
        int i = Concolic.inputInt();
        int j = Concolic.inputInt();
        if (i + j > 2) {
            throw new RuntimeException("error!");
        }
    }

    public static void main(String[] args) {
        run();
    }
}
