package dicograph.Editing;

/**
 * Created by Fynn Leitow on 23.12.17.
 */
enum ForbiddenSubgraphs {

    // forbidden subgraphs of length 3:

    _p3(new int[]{
            1,-1,
            -1,    1,
            -1,-1    },
            1),

    _a(new int[]{
            1,-1,
            -1,    1,
            -1, 1    },
            2),

    _b(new int[]{
            -1,-1,
            1,    1,
            -1, 1    },
            2),

    _c3(new int[]{
            1,-1,
            -1,    1,
            1,-1    },
            2),

    _d3(new int[]{
            1, 1,
            -1,    1,
            1,-1    },
            3),


    // forbidden subgraphs of length 4:

    _p4(new int[]{
                1,-1,-1,
             1,    1,-1,
            -1, 1,    1,
            -1,-1, 1},
            5),

    _n(new int[]{
               -1,-1,-1,
            -1,   -1,-1,
            -1, 1,   -1,
             1, 1,-1},
             2),

    _n_bar(new int[]{
               1, 1,-1,
            1,   -1,-1,
            1, 1,    1,
            1, 1, 1    },
            8);


    private final int[] args;

    // threshold = number of ones - 1 (i.e. must be less or equal to not contain this forbidden subgraph)
    private final int threshold;

    ForbiddenSubgraphs(int[] args, int threshold){
        this.args = args;
        this.threshold = threshold;
    }

    int[] get(){
        return args;
    }

    public int getThreshold() {
        return threshold;
    }

    static ForbiddenSubgraphs[] len_4 = {_p4, _n, _n_bar};
    static ForbiddenSubgraphs[] len_3 = {_p3, _a, _b, _c3, _d3};



    //static int [] threshold_len_4 = {5, 2, 8};
    //static int [] threshold_len_3 = {1, 2, 2, 2, 3};
    //static String [] len_4_names = {"p4", "n", "n_bar"};
    //static String [] len_3_names = {"p3", "a", "b", "c3", "d3"};
}
