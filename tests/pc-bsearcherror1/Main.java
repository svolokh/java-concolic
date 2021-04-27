import csci699cav.Concolic;


/**
  From pathcrawler-online.com "BsearchError1"
 */
public class Main {

    public static int Bsearch( int[] A, int elem)
    {
        int low, high, mid, found ;

        low = 0 ;
        high = 9 ;
        found = 0 ;
        while( ( high > low ) )                     /* line 18 */
        { 
            mid = (low + high) / 2 ; /* error, next line should be : if( elem == A[mid] ) */
            if( elem != A[mid] )                    /* line 21 */ 
                found = 1;
            if( elem > A[mid] )                     /* line 23 */
                low = mid + 1 ;
            else
                high = mid - 1;
        }  
        mid = (low + high) / 2 ;

        if( ( found != 1)  && ( elem == A[mid]) )   /* line 30 */
            found = 1; 

        return found ;
    }

    public static void oracle_Bsearch(
            int Pre_A[], int A[],
            int Pre_elem, int elem,
            int result_implementation)
    {
        int i;
        int present = 0;
        int modif = 10;

        for(i=0;i<10;i++)
        {
            if(A[i] != Pre_A[i])
                modif = i;
            if(A[i] == elem)
                present = 1;
        }

        if(present==0 && present != result_implementation) {
            System.exit(1); } /* implementation wrongly found elem in A */
        else {
            if(present==1 && present != result_implementation) {
                System.exit(1); } /* implementation wrongly said elem was not in A */
            else {
                if(modif<10) {
                    System.exit(1); } /* implementation modified A */
                else { 
                    System.exit(0);
                }
            }
        }
        return;
    }

    @Concolic.Entrypoint
    public static void run() {
        int elem = Concolic.inputInt();
        Concolic.assume(elem >= 0 && elem <= 100);

        int[] A = new int[10];
        for (int i = 0; i != A.length; ++i) {
            int x = Concolic.inputInt();
            Concolic.assume(x >= 0 && x <= 100);
            A[i] = x;
        }
        for (int i = 1; i != A.length; ++i) {
            Concolic.assume(A[i] >= A[i-1]);
        }

        int result = Bsearch(A, elem);
        oracle_Bsearch(A, A, elem, elem, result);
    }

    public static void main(String[] args) {
        run();
    }
}

