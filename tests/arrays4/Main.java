import csci699cav.Concolic;

public class Main {
    private static char[] concat(char[] s1, char[] s2) {
        int l1 = s1.length;
        int l2 = s2.length;
        char[] s3 = new char[l1 + l2];
        for (int i = 0; i != l1; ++i) {
            s3[i] = s1[i];
        }
        for (int i = 0; i != l2; ++i) {
            s3[i + l1] = s2[i];
        }
        return s3;
    }

    private static boolean isLetter(char c) {
        return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z';
    }

    private static void println(char[] s) {
        for (char c : s) {
            System.out.print(c);
        }
        System.out.print('\n');
    }
    
    @Concolic.Entrypoint
    public static void run() {
        char[] s1 = new char[] {'H', 'e', 'l', 'l', 'o', ' '};

        char c1 = Concolic.inputChar();
        char c2 = Concolic.inputChar();
        char c3 = Concolic.inputChar();
        char c4 = Concolic.inputChar();

        Concolic.assume(isLetter(c1) && isLetter(c2) && isLetter(c3) && isLetter(c4));

        char[] s2 = new char[] {c1, c2, c3, c4};
        char[] s3 = concat(s1, s2);

        println(s3);
        System.exit(1);
    }

    public static void main(String[] args) {
        run();
    }
}
