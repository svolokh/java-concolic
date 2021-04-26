import csci699cav.Concolic;

public class Main {
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
    }

    @Concolic.Entrypoint
    public static void run() {
        for (int i = 0; i != 2; ++i) {
            int message = Concolic.inputInt();
            Concolic.assume(message >= 0 && message <= 3);
            acController(message);
            Concolic.assertFalse(isRoomHot && isDoorClosed && !ac);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
