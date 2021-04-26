import csci699cav.Concolic;

public class Main {
    @Concolic.Entrypoint
    public static void run() {
        int x = Concolic.inputInt() + 2;
        switch(x) {
            case 0:
            case 1000:
                break;
            case 2301:
                int y = Concolic.inputInt();
                switch(y) {
                    case 100:
                        System.exit(1);
                        break;
                    case 200:
                        System.exit(0);
                        break;
                    default:
                        break;
                }
                break;
            default:
                System.exit(0);
                break;
        }
    }

    public static void main(String[] args) {
        run();
    }
}
