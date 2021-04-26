import csci699cav.Concolic;

public class Main {
    @Concolic.Entrypoint
    public static void h() {
        int x = Concolic.inputInt();
        long time = System.currentTimeMillis();
        int y = (int)((time/10000) % 10);
        Concolic.assertFalse(x == y + 2);
    }

    public static void main(String[] args) {
        h();
    }
}
