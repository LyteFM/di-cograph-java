package dicograph.modDecomp;

import org.jgrapht.alg.util.Pair;

import java.util.BitSet;
import java.util.logging.Logger;

/**
 * Created by Fynn Leitow on 29.10.17.
 */
public class PartitiveFamilyLeafNode extends PartitiveFamilyTreeNode {

    private final int vertex;

    PartitiveFamilyLeafNode(int vertexNo, PartitiveFamilyTree tree) {
        super(tree);
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

    @Override
    int exportAsDot(StringBuilder output, int[] counter){
        counter[0]++;
        int myCounter = counter[0];
        output.append(myCounter).append("[label=").append(vertex).append("];\n");
        return myCounter;
    }
}
