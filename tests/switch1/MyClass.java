import csci699cav.Concolic;

public class MyClass {
    public static void h() {
        int x = Concolic.inputInt() + 2;
        switch(x) {
            case 0:
            case 4:
                break;
            case 1:
                System.exit(1);
                break;
            default:
                System.exit(0);
                break;
        }
    }

    public static void main(String[] args) {
        h();
    }
}
