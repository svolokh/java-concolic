import csci699cav.Concolic;

public class MyClass {
    @Concolic.Entrypoint
    public static void run() {
        byte b = Concolic.inputByte();
        boolean bl = Concolic.inputBoolean();
        int x = Concolic.inputInt();
        long l = Concolic.inputLong();
        float f = Concolic.inputFloat();
        double d = Concolic.inputDouble();
        char c = Concolic.inputChar();
        if (b == (byte)0x33 && !bl && x == 22 && l == 0x12345678L && f == 2.5f && d == 120.5 && c == 'X') {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
