import csci699cav.Concolic;

public class MyClass {
    @Concolic.Entrypoint
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

        Concolic.assertFalse(sum == 20);
    }

    public static void main(String[] args) {
        h();
    }
}
