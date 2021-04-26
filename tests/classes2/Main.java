import csci699cav.Concolic;

public class Main {

    public static class Node {
        public int value;
        public Node next;

        public Node(int value, Node next) {
            this.value = value;
            this.next = next;
        }
    }

    @Concolic.Entrypoint
    public static void run() {
        Node curr = null;
        for (int i = 0; i != 5; ++i) {
            int x = Concolic.inputInt();
            if (x >= 10 && x <= 30) {
                curr = new Node(x, curr);
            } else {
                break;
            }
        }

        int sum = 0;
        while (curr != null) {
            sum += curr.value;
            curr = curr.next;
        }
        
        Concolic.assertFalse(sum == 100);
    }

    public static void main(String[] args) {
        run();
    }
}
