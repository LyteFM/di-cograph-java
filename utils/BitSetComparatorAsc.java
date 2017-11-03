package dicograph.utils;

import java.util.BitSet;
import java.util.Comparator;

/**
 * Created by Fynn Leitow on 28.10.17. Ascending size
 */
public class BitSetComparatorAsc implements Comparator<BitSet> {
    public int compare(BitSet bs1, BitSet bs2) {
        return Integer.compare(bs1.cardinality(), bs2.cardinality());
    }
}