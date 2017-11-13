package dicograph.modDecomp;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

import dicograph.utils.SortAndCompare;

/**
 * Created by Fynn Leitow on 09.11.17.
 */
public class PartitiveFamilyTree extends RootedTree {

    private PartitiveFamilyTree(){
        super();
    }

    /**
     * Creates a new Tree reprenstation from the given vertices-Bitset (Lemma 13)
     * @param inputSet the BitSet of input vertices representing a partitive family
     * @param nVertices the number of vertices
     */
    public PartitiveFamilyTree(Collection<BitSet> inputSet, Logger log, int nVertices){
        this();
        createInclusionTreeFromBitsets(inputSet, log, nVertices);
    }

    public void getLeavesInLeftToRightOrder(List<PartitiveFamilyLeafNode> orderedLeaves){
        PartitiveFamilyTreeNode realRoot = (PartitiveFamilyTreeNode) root;
        realRoot.getLeavesInLeftToRightOrder(orderedLeaves);
    }


    public void computeAllNodeTypes(final DirectedMD data){
        PartitiveFamilyTreeNode rootNode = (PartitiveFamilyTreeNode) root;
        rootNode.determineNodeType(data);
    }

    public void computeFactorizingPermutationAndReorderAccordingly(Logger log, DirectedGraph<Integer,DefaultEdge> inputGraph, int nVertices){

        // First step: order the leaves in accordance of their left-right appearance in the Tree
        List<PartitiveFamilyLeafNode> orderedLeaves = new ArrayList<>(nVertices);
        this.getLeavesInLeftToRightOrder(orderedLeaves);
        ArrayList<Integer> permutationAsIntegers = new ArrayList<>(nVertices);
        for(PartitiveFamilyLeafNode leaf : orderedLeaves){
            permutationAsIntegers.add(leaf.getVertex());
        }
        // the position of every element in the permutation
        int[] positionInPermutation = new int[nVertices];
        for(int i = 0; i< permutationAsIntegers.size(); i++){
            positionInPermutation[permutationAsIntegers.get(i)] = i;
        }

        PartitiveFamilyTreeNode rootNode = (PartitiveFamilyTreeNode) root;


        // now, iterate through the tree and compute for every inner node X of T_H:
        //   - le(X), re(X): the first occurence of any vertex of X in σ.
        //     this can be done bottom-up

        // init
        for(int i = 0; i<orderedLeaves.size(); i++){
            PartitiveFamilyLeafNode leaf = orderedLeaves.get(i);
            leaf.le_X = i;
            leaf.re_X = i;
            // can also be done here
            leaf.lc_X = i;
            leaf.rc_X = i;
        }
        // compute
        rootNode.computeReAndLe(log);


        //   - lc(X), rc(X): the leftmost/rightmost of its cutters
        // Therefore: "BucketSort" edges of G according to σ. BitSets guarantee easy symdiff operation.:

        // This is for N_{+}: 1st key is outVertex, 2nd key is destVertex
        BitSet[] sortedOutEgdes = SortAndCompare.edgesSortedByPerm(permutationAsIntegers, positionInPermutation, inputGraph, true);
        // This is for N_{-}: 1st key is destVertex, 2nd outVertex
        BitSet[] sortedInEdges = SortAndCompare.edgesSortedByPerm(permutationAsIntegers, positionInPermutation,  inputGraph, false);



        // now, compute the cutters:
        rootNode.computeLeftRightCutterForThis(sortedOutEgdes, sortedInEdges, log);

        // remember: X is a module iff le(X) == lc(X) and re(X) == rc(X)
        // next step: use N_{+} and N_{-} to separate the children of a 0/1-complete node that is a module into R_X-classes
        // todo: according to step 5, I can select any representative of its children. Is my subgraph okay???
        rootNode.reorderAllInnerNodes( log, sortedOutEgdes, sortedInEdges,  orderedLeaves, positionInPermutation);

    }

    /**
     * Implementation of the Inclusion tree according to Lemma 11
     *
     * @param inputSet a Collection of BitSets representing a partitive set family
     * @return The rooted tree of the input set
     */
    private void createInclusionTreeFromBitsets(Collection<BitSet> inputSet, Logger log, int nVertices) {

        HashMap<BitSet, RootedTreeNode> bitsetToInclusionTreenNode = new HashMap<>(inputSet.size()*4/3);

        // Step 0: eliminate doubles from input and add root, if not yet present.
        HashSet<BitSet> inputSetNoDoubles = new HashSet<>(inputSet);
        if (inputSetNoDoubles.size() != inputSet.size()) {
            log.info("Double entry in input for inclusion tree - possible merge in overlap components.");
        }
        ArrayList<BitSet> inputNodeList = new ArrayList<>(inputSetNoDoubles);

        // Sort the array by size, using counting sort
        // todo: Das brauche ich häufiger! Auch hier mit BucketSort...
        inputNodeList.sort( // descending size, with lamdas.
                (b1, b2) -> Integer.compare(b2.cardinality(), b1.cardinality()));
        log.fine("Input sorted by size: " + inputNodeList);

        // add root, if not yet included
        BitSet rootSet = new BitSet(nVertices);
        rootSet.set(0, nVertices); // toIndex must be n
        if(!inputSetNoDoubles.contains(rootSet)) {
            inputNodeList.add(0, rootSet);
        }


        // Create a List for each v ∈ V of the members of F (i.e. the elements of σ) containing v in ascending order of their size:

        // init empty
        ArrayList<LinkedList<BitSet>> xLists = new ArrayList<>(nVertices);
        for (int i = 0; i < nVertices; i++) {
            xLists.add(i, new LinkedList<>());
        }



        // - visit each Y ∈ F in descending order of size. For each x ∈ Y, insert pointer to Y to front of x's list. [O(sz(F)]
        inputNodeList.forEach(ySet ->
                ySet.stream().forEach(i ->
                        xLists.get(i).addFirst(ySet)
                )
        );
        // root is now the last element in every xList


        // PartitiveFamilyTree ret = new PartitiveFamilyTree(); this
        PartitiveFamilyTreeNode root = new PartitiveFamilyTreeNode(rootSet);
        this.setRoot(root);
        //HashMap<BitSet, RootedTreeNode> bitsetToOverlapTreenNode = new HashMap<>(nontrivOverlapComponents.size() * 4 / 3);
        bitsetToInclusionTreenNode.put(rootSet, root);

        // This creates the inclusion tree from the x's lists
        // - visit each x ∈ V, put a parent pointer from each member of x's list to its successor in x's list (if not already done)
        //      -> these are chains of ancestors of {x}

        PartitiveFamilyLeafNode[] allLeafs = new PartitiveFamilyLeafNode[nVertices]; // todo: was damit? Muss ja mal bottom-up?
        int relationCount = 0;
        for (int vertexNr = 0; vertexNr < nVertices; vertexNr++) {

            // retrieve List and create leafNode
            LinkedList<BitSet> currVertexList = xLists.get(vertexNr);
            PartitiveFamilyLeafNode leafNode = new PartitiveFamilyLeafNode(vertexNr);

            boolean firstEntry = true;
            ListIterator<BitSet> vListIter = currVertexList.listIterator(0);
            while (vListIter.hasNext()) {

                BitSet currModule = vListIter.next();
                // create Trenode if not yet present
                PartitiveFamilyTreeNode currTreenode = (PartitiveFamilyTreeNode) bitsetToInclusionTreenNode.getOrDefault(currModule, null);
                if (currTreenode == null) {
                    currTreenode =  new PartitiveFamilyTreeNode(currModule);
                    bitsetToInclusionTreenNode.put(currModule, currTreenode);
                }

                // the first entry of the list always gets the leaf attached.
                if (firstEntry) {
                    currTreenode.addChild(leafNode);
                    allLeafs[vertexNr] = leafNode;
                    firstEntry = false;
                }

                // add as child to the next element of xList
                // Note: currTreenode.equals(root) occurs when we're at the end of the list
                // Don't check for parent of root, of course.
                if (vListIter.hasNext()) {
                    BitSet parentModule = vListIter.next();
                    PartitiveFamilyTreeNode parentTreeNode = (PartitiveFamilyTreeNode) bitsetToInclusionTreenNode.get(parentModule);

                    // create parent if not yet present
                    if (parentTreeNode == null) {
                        parentTreeNode = new PartitiveFamilyTreeNode(parentModule);
                        bitsetToInclusionTreenNode.put(parentModule, parentTreeNode);
                        log.fine("Vertex " + vertexNr + ": Created Parent " + parentModule);
                    }

                    // add parent if current treenode has no parent
                    if (currTreenode.isRoot()) {
                        parentTreeNode.addChild(currTreenode);
                        relationCount++;
                        log.fine("Vertex " + vertexNr + ": Rel " + relationCount + ": child " + currModule + " to parent " + parentModule);
                    }
                    // go one element back in list
                    vListIter.previous();
                }

            }

        }
        setModuleToTreenode(bitsetToInclusionTreenNode);
    }


}
