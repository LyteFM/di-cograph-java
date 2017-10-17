package dicograph.modDecomp;

import java.util.BitSet;
import java.util.Comparator;

/**
 * Created by Fynn Leitow on 17.10.17.
 */
public class BitSetComparator implements Comparator<BitSet> {

    // want the arraylist in descending size.
    public int compare(BitSet bs1, BitSet bs2) {
        return Integer.compare(bs1.cardinality(), bs2.cardinality()) * -1;
    }
}
