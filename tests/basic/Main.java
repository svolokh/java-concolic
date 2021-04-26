import csci699cav.Concolic;

public class Main {
    @Concolic.Entrypoint
    public static void h() {
        StringBuilder sb = new StringBuilder();
        sb.append('a');
        sb.append('b');
        sb.append(Concolic.inputInt());
        String s = sb.toString();
        Concolic.assertFalse(s.charAt(2) == 'x');
    }

    public static void main(String[] args) {
        h();
    }
}
