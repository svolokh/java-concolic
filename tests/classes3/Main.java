import csci699cav.Concolic;

public class Main {
    public static class Node {
        public int value;
        public Node[] children;

        public Node(int value, Node[] children) { 
            if (value > 10) {
                this.value = value;
            }
            this.children = children;
        }
        
        public int sum() {
            int result = value;
            if (children != null) {
                for (int i = 0; i != children.length; ++i) {
                    result += children[i].sum();
                }
            }
            return result;
        }
    }

    @Concolic.Entrypoint
    public static void run() {
        Node n = new Node(10, 
                new Node[] {
                    new Node(Concolic.inputInt(), new Node[] { new Node(Concolic.inputInt(), null), new Node(20, null), new Node(30, null) }), 
                    new Node(Concolic.inputInt(), new Node[] { new Node(20, null), new Node(Concolic.inputInt(), null), new Node(40, null) }), 
                    new Node(Concolic.inputInt(), new Node[] { new Node(30, null), new Node(11, null), new Node(Concolic.inputInt(), null) })
                });
        Concolic.assertFalse(n.sum() == 300);
    }

    public static void main(String[] args) {
        run();
    }
}
