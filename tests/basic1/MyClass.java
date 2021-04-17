import csci699cav.Concolic;

public class MyClass {
    public static int f(int x) {
        return 2*x;
    }

    public static void h(int x, int y) {
        if (x != y) {
            if (f(x) == x + 10) {
                System.exit(1);
            }
        }
    }

    public static void main(String[] args) {
        int x = Concolic.inputInt();
        int y = Concolic.inputInt();
        h(x, y);
    }
}
