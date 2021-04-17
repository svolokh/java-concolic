import csci699cav.Concolic;

public class MyClass {
    private static boolean isRoomHot = false;
    private static boolean isDoorClosed = false;
    private static boolean ac = false;

    private static void acController(int message) {
        if (message == 0) {
            isRoomHot = true;
        } 
        if (message == 1) {
            isRoomHot = false;
        }
        if (message == 2) {
            isDoorClosed = false;
            ac = false;
        }
        if (message == 3) {
            isDoorClosed = true;
            if (isRoomHot) {
                ac = true;
            }
        }
        if (isRoomHot && isDoorClosed && !ac) {
            System.exit(1);
        }
    }

    @Concolic.Entrypoint
    public static void run() {
        for (int i = 0; i != 2; ++i) {
            int message = Concolic.inputInt();
            acController(message);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
