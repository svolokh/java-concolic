import csci699cav.Concolic;

public class MyClass {
    @Concolic.Entrypoint
    public static void run() {
        float x = (float)Concolic.inputInt();
        float y = (float)Concolic.inputInt();
        float z = (float)Concolic.inputInt();
        if (x > 0.f && y > 0.f && Math.max(x + y, z) == Math.max(x, y + z)) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
