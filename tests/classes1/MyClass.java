import csci699cav.Concolic;

public class MyClass {

    public static class Point {
        public int x;
        public int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    @Concolic.Entrypoint
    public static void run() {
        Point p = new Point(Concolic.inputInt(), Concolic.inputInt());
        if (p.x + p.y == 10) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
