package dicograph.modDecomp;

import org.jgrapht.alg.util.Pair;

import java.util.BitSet;
import java.util.logging.Logger;

/**
 * Created by Fynn Leitow on 29.10.17.
 */
public class PartitiveFamilyLeafNode extends PartitiveFamilyTreeNode {

    private int vertex;

    protected PartitiveFamilyLeafNode(int vertexNo) {
        super();
        vertex = vertexNo;
    }

    public int getVertex() {
        return vertex;
    }

    @Override
    public String toString() {
        return "(Leaf: " + vertex + ")";
    }

    /**
     * See proof of Lem 24: "If X is a leaf, lc(X) = rc(X) = σ(x)"
     * @return the position of the assiciated vertex in σ
     */
    @Override
    protected Pair<Integer,Integer> computeLeftRightCutter(BitSet[] outNeighbors, BitSet[] inNeighbors, int[] positionInPermutation, Logger log){
        lc_X = positionInPermutation[vertex];
        rc_X = positionInPermutation[vertex];
        return new Pair<>(lc_X,rc_X);
    }
}
