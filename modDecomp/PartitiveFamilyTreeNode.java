package dicograph.modDecomp;

import org.jgrapht.alg.util.Pair;

import java.util.BitSet;
import java.util.List;

import dicograph.utils.SortAndCompare;

/**
 * Created by Fynn Leitow on 29.10.17.
 */
public class PartitiveFamilyTreeNode extends RootedTreeNode {

    protected int le_X;
    protected int re_X;
    protected int lc_X;
    protected int rc_X;
    private boolean isModuleInG;


    protected PartitiveFamilyTreeNode(){
        super();
        isModuleInG = false;
    }


    protected Pair<Integer,Integer> computeLeftRightCutterForThis(BitSet[] outNeighbors, BitSet[] inNeighbors){

        lc_X = outNeighbors.length;
        rc_X = 0;
        int first;
        int second;
        // iterate over children, compute this and the cutter for their re/le
        PartitiveFamilyTreeNode currentChild = (PartitiveFamilyTreeNode) getFirstChild();
        PartitiveFamilyTreeNode nextChild = null;
        if(currentChild != null) {
            while (currentChild != null) {

                // compute and compare with its real cutters
                Pair<Integer,Integer> childsCutters = currentChild.computeLeftRightCutterForThis(outNeighbors,inNeighbors);
                first = childsCutters.getFirst();
                second = childsCutters.getSecond();

                if(lc_X > first)
                    lc_X = first;
                if(rc_X < second)
                    rc_X = second;

                // compute and compare with cutters of le/re
                nextChild = (PartitiveFamilyTreeNode) currentChild.getRightSibling();
                if(nextChild != null){
                    Pair<Integer,Integer> leftRightCutters = computeLeftRightCutterForVertices(
                            currentChild.getRe_X(), nextChild.getLe_X(), outNeighbors, inNeighbors);
                    first = leftRightCutters.getFirst();
                    second = leftRightCutters.getSecond();

                    if(lc_X > first)
                        lc_X = first;
                    if(rc_X < second)
                        rc_X = second;
                }

                currentChild = nextChild;
            }
        }

        if(lc_X == le_X && rc_X == re_X){
            isModuleInG = true;
        }

        return new Pair<>(lc_X,rc_X);
    }

    /**
     * Helper method to compute left and right cutter of two given vertices
     */
    protected static Pair<Integer, Integer> computeLeftRightCutterForVertices(int x, int y, BitSet[] outNeighbors, BitSet[] inNeighbors){
        // compute the symmetrical difference. Need to copy!
        BitSet inSymDiff = SortAndCompare.symDiff(outNeighbors[x], outNeighbors[y]);
        BitSet outSymDiff = SortAndCompare.symDiff(inNeighbors[x], inNeighbors[y]);

        inSymDiff.or(outSymDiff); // ∪

        boolean first = true;
        int leftCutter = -1; // todo: was, wenn's keine gibt?
        int rightCutter = -1;
        for (int i = inSymDiff.nextSetBit(0); i >= 0; i = inSymDiff.nextSetBit(i+1)) {
            if(first) {
                leftCutter = i;
            }
            rightCutter = i;
            first = false;
        }

        return new Pair<>(leftCutter, rightCutter);
    }

    /**
     * Fills the provided empty, but size-initialized list with the leaves of the tree
     * in left-to-right order. Initial action in step 4.
     * @param orderedLeaves the list to be filled
     */
    protected void getLeavesInLeftToRightOrder(List<PartitiveFamilyLeafNode> orderedLeaves){

        PartitiveFamilyTreeNode currentChild = (PartitiveFamilyTreeNode) getFirstChild();
        if(currentChild != null){
            while (currentChild != null){
                if(currentChild.isALeaf()){
                    PartitiveFamilyLeafNode leafNode = (PartitiveFamilyLeafNode) currentChild;
                    // appends the leaf to the list, it is the leftmost discovered one
                    orderedLeaves.add(leafNode);
                } else {
                    // follow the leftmost branch before checking the right sibling.
                    currentChild.getLeavesInLeftToRightOrder(orderedLeaves);
                }
                currentChild = (PartitiveFamilyTreeNode) currentChild.getRightSibling();
            }
        }
    }

    /**
     * Computes the first (le_X) and last (re_X) occurence of any vertex below the node X,
     * according to Lemma 24.
     * @return first Element: le_X, second Element: re_X
     */
    protected Pair<Integer,Integer> computeReAndLe(){

        RootedTreeNode currentChild = getFirstChild();
        // I assume that the leaves are already set. If this is a leaf, currentChild is null.

        if(currentChild != null){
            boolean firstRun = true;
            while (currentChild != null){

                // follow the leftmost branch before checking the right sibling.
                PartitiveFamilyTreeNode realNode = (PartitiveFamilyTreeNode) currentChild;
                Pair<Integer,Integer> vals = realNode.computeReAndLe();
                if(firstRun) {
                    // only the first entry counts here
                    le_X = vals.getFirst();
                }
                // updated until rightmost neighbour completed
                re_X = vals.getSecond();

                currentChild = currentChild.getRightSibling();
                firstRun = false;
            }
        }

        return new Pair<>(le_X, re_X);
    }

    public int getLe_X() {
        return le_X;
    }

    public int getRe_X() {
        return re_X;
    }

    public boolean isModuleInG() {
        return isModuleInG;
    }
}
