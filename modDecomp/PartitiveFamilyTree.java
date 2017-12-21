package dicograph.modDecomp;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import dicograph.utils.SortAndCompare;

/**
 * Created by Fynn Leitow on 09.11.17.
 */
public class PartitiveFamilyTree extends RootedTree {

    public PartitiveFamilyTree(){
        super();
    }

    public void getLeavesInLeftToRightOrder(List<PartitiveFamilyLeafNode> orderedLeaves){
        PartitiveFamilyTreeNode realRoot = (PartitiveFamilyTreeNode) root;
        realRoot.getLeavesInLeftToRightOrder(orderedLeaves);
    }


    public void computeAllNodeTypes(final DirectedMD data){
        PartitiveFamilyTreeNode rootNode = (PartitiveFamilyTreeNode) root;
        rootNode.determineNodeTypeForH(data);
    }

    public void computeFactorizingPermutationAndReorderAccordingly(DirectedMD data, boolean reorder){

        // First step: order the leaves in accordance of their left-right appearance in the Tree
        List<PartitiveFamilyLeafNode> orderedLeaves = new ArrayList<>(data.nVertices);
        this.getLeavesInLeftToRightOrder(orderedLeaves);
        ArrayList<Integer> permutationAsIntegers = new ArrayList<>(data.nVertices);
        for(PartitiveFamilyLeafNode leaf : orderedLeaves){
            permutationAsIntegers.add(leaf.getVertex());
        }
        data.log.fine(() -> "Initial leaf order: " + permutationAsIntegers);
        // the position of every element in the permutation
        int[] positionInPermutation = new int[data.nVertices];
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
        }
        // compute
        rootNode.computeReAndLe(data.log);


        //   - lc(X), rc(X): the leftmost/rightmost of its cutters
        // Therefore: "BucketSort" edges of G according to σ. BitSets guarantee easy symdiff operation.:

        // This is for N_{+}: 1st key is outVertex, 2nd key is destVertex
        BitSet[] sortedOutEgdes = SortAndCompare.edgesSortedByPerm(permutationAsIntegers, positionInPermutation, data.inputGraph, true);
        // This is for N_{-}: 1st key is destVertex, 2nd outVertex
        BitSet[] sortedInEdges = SortAndCompare.edgesSortedByPerm(permutationAsIntegers, positionInPermutation,  data.inputGraph, false);



        // now, compute the cutters:
        rootNode.computeLeftRightCutter(sortedOutEgdes, sortedInEdges, positionInPermutation, data.log);

        // remember: X is a module iff le(X) == lc(X) and re(X) == rc(X)
        // next step: use N_{+} and N_{-} to separate the children of a 0/1-complete node that is a module into R_X-classes
        // According to step 5, for the tournament I can select any representative of its children.
        // todo: Problem - they have uniform relationship to other nodes ONLY IF the child is a strong node!!! Must make them Strong, if they aren't!!!
        if(reorder) {
            // reorder only, don't delete/recover anything
            rootNode.reorderAllInnerNodes(data, sortedOutEgdes, sortedInEdges, orderedLeaves, positionInPermutation);
        } else {
            // delete and recover.
        }

        /*
        if(data.debugMode){
            // vielleicht nützlich:

            // First step: order the leaves in accordance of their left-right appearance in the Tree
            List<PartitiveFamilyLeafNode> ordered = new ArrayList<>(data.nVertices);
            this.getLeavesInLeftToRightOrder(ordered);
            ArrayList<Integer> integers = new ArrayList<>(data.nVertices);
            for(PartitiveFamilyLeafNode leaf : ordered){
                integers.add(leaf.getVertex());
            }
            data.log.fine(() -> "Final leaf order: " + integers);
            // the position of every element in the permutation
            int[] posiperm = new int[data.nVertices];
            for(int i = 0; i< integers.size(); i++){
                posiperm[integers.get(i)] = i;
            }

            // This is for N_{+}: 1st key is outVertex, 2nd key is destVertex
            BitSet[] outEgdes = SortAndCompare.edgesSortedByPerm(integers, posiperm, data.inputGraph, true);

            // This is for N_{-}: 1st key is destVertex, 2nd outVertex
            BitSet[] inEdges = SortAndCompare.edgesSortedByPerm(integers, posiperm,  data.inputGraph, false);

            // Now, let's test the equivalence classes of leaves 9,10 and 12:
            // how should I know who is forming a prime and who not?
            // Remember the usally OK default: directly adding the prime vertices

            List<Integer> leavesAndStrongVertices = Arrays.asList(0,1,12);
//            for( int el : leavesAndStrongVertices){
//                BitSet outs = outEgdes[el];
//                BitSet ins = inEdges[el];
//                for( int j : leavesAndStrongVertices) {
//                    outs.set(j, false);
//                    ins.set(j, false);
//                }
//            }

            data.log.fine("Out-Edges:");
            for(int i = 0; i< outEgdes.length; i++){
                BitSet edge = outEgdes[i];
                data.log.fine(integers.get(i).toString() + ": " + edge);
            }

            data.log.fine("In-Edges:");
            for(int i = 0; i< inEdges.length; i++){
                BitSet edge = inEdges[i];
                data.log.fine(integers.get(i).toString() + ": " + edge);
            }


        }
        */

    }
    /**
     * Creates a new Tree reprenstation from the given vertices-Bitset (Lemma 13)
     * @param inputSet the BitSet of input vertices representing a partitive family
     * @param nVertices the number of vertices
     * @return the leaves of the Tree
     */

    public PartitiveFamilyLeafNode[] createInclusionTreeFromBitsets(Collection<BitSet> inputSet, Logger log, int nVertices) {

        HashMap<BitSet, RootedTreeNode> bitsetToInclusionTreenNode = new HashMap<>(inputSet.size()*4/3);

        // Step 0: eliminate doubles from input and add root, if not yet present.
        HashSet<BitSet> inputSetNoDoubles = new HashSet<>(inputSet);
        if (inputSetNoDoubles.size() != inputSet.size()) {
            log.fine(() -> "Double entry in input for inclusion tree");
        }
        // add root, if not yet included
        BitSet rootSet = new BitSet(nVertices);
        rootSet.set(0, nVertices);
        if(!inputSetNoDoubles.contains(rootSet)) {
            inputSetNoDoubles.add(rootSet);
        }
        ArrayList<BitSet> inputNodeList = new ArrayList<>(inputSetNoDoubles);

        // Sort the array by size, using bucket sort (w/o singletons)
        SortAndCompare.bucketSortBySize(inputNodeList,true);
        log.fine(() -> "Input sorted by size: " + inputNodeList);


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


        PartitiveFamilyTreeNode root = new PartitiveFamilyTreeNode(rootSet, this);
        this.setRoot(root);
        //HashMap<BitSet, RootedTreeNode> bitsetToOverlapTreenNode = new HashMap<>(nontrivOverlapComponents.size() * 4 / 3);
        bitsetToInclusionTreenNode.put(rootSet, root);

        // This creates the inclusion tree from the x's lists
        // - visit each x ∈ V, put a parent pointer from each member of x's list to its successor in x's list (if not already done)
        //      -> these are chains of ancestors of {x}

        PartitiveFamilyLeafNode[] allLeafs = new PartitiveFamilyLeafNode[nVertices];
        int relationCount = 0;
        for (int vertexNr = 0; vertexNr < nVertices; vertexNr++) {

            // retrieve List and create leafNode
            LinkedList<BitSet> currVertexList = xLists.get(vertexNr);
            PartitiveFamilyLeafNode leafNode = new PartitiveFamilyLeafNode(vertexNr,this);

            boolean firstEntry = true;
            ListIterator<BitSet> vListIter = currVertexList.listIterator(0);
            while (vListIter.hasNext()) {

                BitSet currModule = vListIter.next();
                // create Trenode if not yet present
                PartitiveFamilyTreeNode currTreenode = (PartitiveFamilyTreeNode) bitsetToInclusionTreenNode.getOrDefault(currModule, null);
                if (currTreenode == null) {
                    currTreenode =  new PartitiveFamilyTreeNode(currModule,this);
                    bitsetToInclusionTreenNode.put(currModule, currTreenode);
                }

                // the first entry of the list always gets the leaf attached.
                if (firstEntry) {
                    currTreenode.addChild(leafNode); // no need to change bitset
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
                        parentTreeNode = new PartitiveFamilyTreeNode(parentModule,this);
                        bitsetToInclusionTreenNode.put(parentModule, parentTreeNode);
                        log.fine("Vertex " + vertexNr + ": Created Parent " + parentModule);
                    }

                    // add parent if current treenode has no parent
                    if (currTreenode.isRoot()) {
                        parentTreeNode.addChild(currTreenode); // no need to update bitset here, they are already set.
                        relationCount++;
                        log.fine("Vertex " + vertexNr + ": Rel " + relationCount + ": child " + currModule + " to parent " + parentModule);
                    }
                    // go one element back in list
                    vListIter.previous();
                }

            }

        }
        moduleToTreenode = bitsetToInclusionTreenNode;
        return allLeafs;
    }


}
