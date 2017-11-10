package dicograph.modDecomp;

import org.jgrapht.alg.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Fynn Leitow on 29.10.17.
 */
public class PartitiveFamilyTreeNode extends RootedTreeNode {

    protected int le_X;
    protected int re_X;
    protected int lc_X;
    protected int rc_X;

    protected PartitiveFamilyTreeNode(){
        super();
    }


    /**
     * Fills the provided empty, but size-initialized list with the leaves of the tree in left-to-right order
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
}
