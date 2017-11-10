package dicograph.modDecomp;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by Fynn Leitow on 09.11.17.
 */
public class PartitiveFamilyTree extends RootedTree {

    protected PartitiveFamilyTree(){
        super();
    }

    public void getLeavesInLeftToRightOrder(List<PartitiveFamilyLeafNode> orderedLeaves){
        PartitiveFamilyTreeNode realRoot = (PartitiveFamilyTreeNode) root;
        realRoot.getLeavesInLeftToRightOrder(orderedLeaves);
    }

    public void computeReAndLeBottomUp(List<PartitiveFamilyLeafNode> leftOrderedLeaves){

        // init
        for(int i = 0; i<leftOrderedLeaves.size(); i++){
            PartitiveFamilyLeafNode leaf = leftOrderedLeaves.get(i);
            leaf.le_X = i;
            leaf.re_X = i;
            // can also be done here
            leaf.lc_X = i;
            leaf.rc_X = i;
        }

        PartitiveFamilyTreeNode rootNode = (PartitiveFamilyTreeNode) root;
        rootNode.computeReAndLe();
    }

    public void computeLeftRightCutters(BitSet[] outgoing, BitSet[] incoming){
        PartitiveFamilyTreeNode rootNode = (PartitiveFamilyTreeNode) root;
        rootNode.computeLeftRightCutterForThis(outgoing,incoming);
    }

    public void computeAllNodeTypes(DirectedMD data){
        PartitiveFamilyTreeNode rootNode = (PartitiveFamilyTreeNode) root;
        rootNode.determineNodeType(data);
    }

    public void reorderAllNodes(Logger log, BitSet[] outNeighbors, BitSet[] inNeighbors,
                                List<PartitiveFamilyLeafNode> orderedLeaves, int[] positionInPermutation){
        PartitiveFamilyTreeNode rootNode = (PartitiveFamilyTreeNode) root;
        rootNode.reorderAllInnerNodes( log, outNeighbors, inNeighbors,  orderedLeaves, positionInPermutation);
    }


}
