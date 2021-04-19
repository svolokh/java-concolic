import csci699cav.Concolic;

public class MyClass {
    @Concolic.Entrypoint
    public static void h() {
        int x = Concolic.inputInt();
        long time = System.currentTimeMillis();
        int y = (int)((time/10000) % 10);
        if (x == y + 2) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        h();
    }
}
