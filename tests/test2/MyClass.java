import csci699cav.Concolic;

public class MyClass {
    public static void h() {
        int x = Concolic.inputInt();
        if (x < 0 || x > 4) {
            return;
        }

        int sum = 0;
        int last = 0;
        for (int i = 0; i != x; ++i) {
            int y = Concolic.inputInt();
            if (y == last) {
                return;
            }
            sum += y;
            last = y;
        }

        if (sum == 20) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        h();
    }
}
