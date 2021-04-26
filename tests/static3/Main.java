import csci699cav.Concolic;

public class Main {
    private static int[] nums = new int[] {1, 10, 20, 300};

    @Concolic.Entrypoint
    public static void run() {
        int[] inputNums = new int[nums.length];
        for (int i = 0; i != inputNums.length; ++i) {
            inputNums[i] = Concolic.inputInt();
        }
        Concolic.assertFalse(inputNums[0] == nums[0] && inputNums[2] == nums[2]);
    }

    public static void main(String[] args) {
        run();
    }
}
