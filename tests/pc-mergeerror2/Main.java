import csci699cav.Concolic;


/**
  From pathcrawler-online.com "MergeError2"
 */
public class Main {

    public static void Merge (int t1[], int t2[], int t3[], int l1, int l2) {

        int i = 0;
        int j = 0;
        int k = 0;

        while (i < l1 && j < l2) {     /* line 21 */
            if (t1[i] < t2[j]) {     /* line 22 */
                t3[k] = t1[i];
                i++;
            }
            else {
                t3[k] = t2[j];
                j++;
            }
            k++;
        }
        while (i < l1) {     /* line 32 */
            t3[k] = t1[i];
            i++;
            /* error : missing instruction k++; here */
        }
        while (j < l2) {     /* line 37 */
            t3[k] = t2[j];
            j++;
            k++;
        }
    }

    public static void oracle_Merge(
            int Pre_t1[], int t1[],
            int Pre_t2[], int t2[],
            int Pre_t3[], int t3[],
            int Pre_l1, int l1,
            int Pre_l2, int l2)
    {
        int i, j, n1, n2, n3;
        int l3 = l1 + l2;
        int l3moins1 = l3 -1;

        for (i = 0; i < l1; i++) {
            if (Pre_t1[i] != t1[i]) {
                Concolic.assertTrue(false); /* t1 modified */
                return;
            }
        }

        for (i = 0; i < l2; i++) {
            if (Pre_t2[i] != t2[i]) {
                Concolic.assertTrue(false); /* t2 modified */
                return;
            }
        }

        for (i = 0; i < l3moins1; i++) {
            if (t3[i] > t3[i+1]) {
                Concolic.assertTrue(false); /* t3 not ordered */
                return;
            }
        }
        i = 0;
        while (i < l3) {
            /* count occurences of this element in t3 */
            n3 = 1;
            while (i < l3moins1 && t3[i + 1] == t3[i]) {
                i++;
                n3++;
            }
            /* count occurences of this element in  t1 */
            n1 = 0;
            for (j = 0; j < l1; j++) {
                if (t1[j] == t3[i])
                    n1++;
            }
            /* count occurences of this element in  t2 */
            n2 = 0;
            for (j = 0; j < l2; j++) {
                if (t2[j] == t3[i])
                    n2++;
            }
            /* compare */
            if (n3 != (n1 + n2)) {
                Concolic.assertTrue(false); /* t3 does not have the correct number of occurrences of all elements */
                return;
            }
            i++;
        }
    }

    @Concolic.Entrypoint
    public static void run() {
        int l1 = Concolic.inputInt();
        int l2 = Concolic.inputInt();

        Concolic.assume((10 <= l1 && l1 <= 20) || (6 <= l1 && l1 <= 9) ||  (0 <= l1 && l1 <= 5) );
        Concolic.assume((10 <= l2 && l2 <= 20) || (6 <= l2 && l2 <= 9) ||  (0 <= l2 && l2 <= 5) );

        int[] t1 = new int[l1];
        int[] t1_orig = new int[l1];
        int[] t2 = new int[l2];
        int[] t2_orig = new int[l2];
        int[] t3 = new int[l1 + l2];
        int[] t3_orig = new int[l1 + l2];

        for (int i = 0; i < l1; ++i) {
            int x = Concolic.inputInt();
            Concolic.assume(-20 <= x && x <= 20);
            t1[i] = x;
            t1_orig[i] = x;
        }

        for (int i = 1; i < l1; ++i) {
            Concolic.assume(t1[i] >= t1[i-1]);
        }

        for (int i = 0; i < l2; ++i) {
            int x = Concolic.inputInt();
            Concolic.assume(-20 <= x && x <= 20);
            t2[i] = x;
            t2_orig[i] = x;
        }

        for (int i = 1; i < l2; ++i) {
            Concolic.assume(t2[i] >= t2[i-1]);
        }

        Merge(t1, t2, t3, l1, l2);
        oracle_Merge(t1_orig, t1, t2_orig, t2, t3_orig, t3,
                    l1, l1, l2, l2);
    }

    public static void main(String[] args) {
        run();
    }
}

