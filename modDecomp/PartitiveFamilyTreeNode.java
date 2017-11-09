package dicograph.modDecomp;

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


    // todo: use general lepth-first-visitor and the apply functions. Migrate to PartitiveFamilyTreeNode.
    public void getLeavesInLeftToRightOrder(List<PartitiveFamilyLeafNode> orderedLeaves){

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

    protected ArrayList<Integer> computeReAndLe(){

        ArrayList<Integer> ret = new ArrayList<>(2);
        RootedTreeNode currentChild = getFirstChild();
        // I assume that the leaves are already set. If this is a leaf, currentChild is null.

        if(currentChild != null){
            boolean firstRun = true;
            while (currentChild != null){

                // follow the leftmost branch before checking the right sibling.
                PartitiveFamilyTreeNode realNode = (PartitiveFamilyTreeNode) currentChild;
                ArrayList<Integer> vals = realNode.computeReAndLe();
                if(firstRun) {
                    // only the first entry counts here
                    le_X = vals.get(0);
                }
                // updated until rightmost neighbour completed
                re_X = vals.get(1);

                currentChild = currentChild.getRightSibling();
                firstRun = false;
            }
        }

        ret.add(le_X);
        ret.add(re_X);
        return ret;
    }

    public int getLe_X() {
        return le_X;
    }

    public int getRe_X() {
        return re_X;
    }
}
