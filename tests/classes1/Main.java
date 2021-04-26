import csci699cav.Concolic;

public class Main {

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
        Concolic.assertFalse(p.x + p.y == 10);
    }

    public static void main(String[] args) {
        run();
    }
}
