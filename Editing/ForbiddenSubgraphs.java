package dicograph.Editing;

/**
 * Created by Fynn Leitow on 23.12.17.
 */
public class ForbiddenSubgraphs {
    // forbidden subgraphs of length 4:
    static int [] _p4    = {    1,-1,-1,
            1,    1,-1,
            -1, 1,    1,
            -1,-1, 1    };

    static int [] _n     = {   -1,-1,-1,
            -1,   -1,-1,
            -1, 1,   -1,
            1, 1,-1    };

    static int [] _n_bar = {    1, 1,-1,
            1,   -1,-1,
            1, 1,    1,
            1, 1, 1    };

    static int [][] len_4 = {_p4, _n, _n_bar};
    static String [] len_4_names = {"p4", "n", "n_bar"};
    // threshold = number of ones - 1
    static int [] threshold_len_4 = {5, 2, 8};

    // forbidden subgraphs of length 3:
    static int [] _p3 = {    1,-1,
            -1,    1,
            -1,-1    };

    static int [] _a  = {    1,-1,
            -1,    1,
            -1, 1    };

    static int [] _b  = {   -1,-1,
            1,    1,
            -1, 1    };

    static int [] _c3 = {    1,-1,
            -1,    1,
            1,-1    };

    static int [] _d3 = {    1, 1,
            -1, 1,
            1,-1    };

    static int [][] len_3 = {_p3, _a, _b, _c3, _d3};
    static String [] len_3_names = {"p3", "a", "b", "c3", "d3"};
    static int [] threshold_len_3 = {1, 2, 2, 2, 3};
}
