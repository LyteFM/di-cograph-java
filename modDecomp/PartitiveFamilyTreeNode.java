package dicograph.modDecomp;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dicograph.graphIO.DirectedInducedIntSubgraph;
import dicograph.utils.SortAndCompare;

/**
 * Created by Fynn Leitow on 29.10.17.
 */
public class PartitiveFamilyTreeNode extends RootedTreeNode {

    protected int le_X;
    protected int re_X;
    protected int lc_X;
    protected int rc_X;
    private BitSet vertices;
    private boolean isModuleInG;
    private MDNodeType type; // null by default???



    // only needed for leaf
    protected PartitiveFamilyTreeNode(){
        super();
        isModuleInG = false;
        vertices = null;
    }

    protected PartitiveFamilyTreeNode(BitSet vertexModule){
        super();
        isModuleInG = false;
        vertices = vertexModule;
    }

    protected int determineNodeType(DirectedMD data){

        // node type can be efficiently computed bottom-up: only take one vertex of each child to construct the module
        PartitiveFamilyTreeNode currentChild = (PartitiveFamilyTreeNode) getFirstChild();
        LinkedList<Integer> subgraphVertices = new LinkedList<>();
        if(currentChild != null) {
            while (currentChild != null) {

                if(currentChild.isALeaf()){
                    subgraphVertices.add( ((PartitiveFamilyLeafNode) currentChild).getVertex() );
                } else  {
                    subgraphVertices.add( currentChild.determineNodeType(data) ); // recursion
                }

                currentChild = (PartitiveFamilyTreeNode) currentChild.getRightSibling();
            }
        }
        int returnVal = subgraphVertices.getFirst();
        // compute the induced subgraph and determine the node type
        DirectedInducedIntSubgraph<DefaultEdge> moduleSubgraph = new DirectedInducedIntSubgraph<>(data.inputGraph, subgraphVertices);
        if(moduleSubgraph.edgeSet().isEmpty()){
            // no edges means 0-complete
            type = MDNodeType.PARALLEL;
        } else {
            int typeVal = -1;
            int currVal;
            boolean firstRun = true;
            for(DefaultEdge edge : moduleSubgraph.edgeSet()){
                int source = moduleSubgraph.getEdgeSource(edge);
                int target = moduleSubgraph.getEdgeTarget(edge);
                currVal = data.getEdgeValueForH(source, target);
                if(firstRun) {
                    typeVal = currVal;
                    firstRun = false;
                } else {
                    if(currVal != typeVal){
                        type = MDNodeType.PRIME;
                        return returnVal;
                    }
                }
            }
            switch (typeVal){
                case 0:
                    type = MDNodeType.PARALLEL;
                    data.log.warning("Unexpected parallel node " + this);
                    break;
                case 1:
                    type = MDNodeType.SERIES;
                    break;
                case 2:
                    type = MDNodeType.ORDER;
                    break;
            }

        }
        assert type != null : "No MDtype found! for " + this;

        return returnVal;
    }

    /**
     * reorders the children of this vertex accound to the perfect factorizing permutation
     * @param perfFactPerm the computed perfect factorizing permutation of the corresponding tournament
     */
    protected void reorderAccordingToPerfFactPerm(List<Integer> perfFactPerm){

        assert type == MDNodeType.ORDER : "Wrong type in step 5: " + type;

        int sz = perfFactPerm.size();
        int[] positionInPermutation = new int[sz];
        for(int i = 0; i<sz; i++){
            positionInPermutation[perfFactPerm.get(i)] = i;
        }

        PartitiveFamilyTreeNode[] orderedNodes = new PartitiveFamilyTreeNode[sz];

        PartitiveFamilyTreeNode currentChild = (PartitiveFamilyTreeNode) getFirstChild();
        if(currentChild != null) {
            while (currentChild != null) {

                int firstPosition = sz;
                int realV;
                // computes the first position of any vertex in the child module
                if(currentChild.isALeaf()){
                    realV = ((PartitiveFamilyLeafNode) currentChild).getVertex();
                    firstPosition = positionInPermutation[realV];
                } else {
                    for(realV = vertices.nextSetBit(0); realV >= 0; realV = vertices.nextSetBit(realV+1)){
                        int position = positionInPermutation[realV];
                        if(position < firstPosition)
                            firstPosition = position;
                    }
                }

                assert orderedNodes[firstPosition] == null : "Vertex for position " + firstPosition + " already present!";
                orderedNodes[firstPosition] = currentChild;

                currentChild = (PartitiveFamilyTreeNode) currentChild.getRightSibling();
            }
        }
        // retrieve the new ordering
        ArrayList<PartitiveFamilyTreeNode> orderedChildren = new ArrayList<>(getNumChildren());
        for(PartitiveFamilyTreeNode node : orderedNodes){
            if(node != null)
                orderedChildren.add(node);
        }
        // sort:
        reorderChildren(orderedChildren);
    }

    /**
     * Step 4: reorders the Tree according to its equivalence classes
     * @param outNeighbors
     * @param inNeighbors
     * @param orderedLeaves
     * @param positionInPermutation
     */
    protected void computeEquivalenceClassesAndReorderChildren(
            BitSet[] outNeighbors, BitSet[] inNeighbors, List<PartitiveFamilyLeafNode> orderedLeaves, int[] positionInPermutation){
        // BitSets are according to the position, not the true vertex!
        // for the sake of simplicity, I use strings and compare if they are equal.
        // running time is same as worst case when comparing the lists element by element.

        assert type.isDegenerate() : "Wrong type in step 4: " + type;
        HashMap<String, List<PartitiveFamilyTreeNode>> equivStringToEquivClass = new HashMap<>(getNumChildren()*4/3);

        PartitiveFamilyTreeNode currentChild = (PartitiveFamilyTreeNode) getFirstChild();
        if(currentChild != null) {
            while (currentChild != null) {
                // take any vertex of child Y and prune vertices of this node X from its adjacency list
                // note: this is the real vertex number, not permutation element.
                int anyVertex;
                if(currentChild.isALeaf()) {
                    anyVertex = ((PartitiveFamilyLeafNode) currentChild).getVertex();
                } else {
                    anyVertex = currentChild.getVertices().nextSetBit(0);
                }
                int vertexPosition = positionInPermutation[anyVertex];

                // compute S_{+}
                StringBuilder verticesWithoutX = new StringBuilder();
                BitSet outNeighborPositions = outNeighbors[vertexPosition];
                // adds the vertex to the list, if it's not a vertex of this treenode
                outNeighborPositions.stream().forEach( vPos -> {
                        int realVertex = orderedLeaves.get(vPos).getVertex();
                        if(!vertices.get(realVertex)){
                            verticesWithoutX.append(realVertex).append('-');
                        }
                });
                // compute S_{-}
                // StringBuilder incomingWithoutX = new StringBuilder();
                verticesWithoutX.append('*'); // want a delimiter

                BitSet incNeighborPositions = inNeighbors[vertexPosition];
                // adds the vertex to the list, if it's not a vertex of this treenode
                incNeighborPositions.stream().forEach( vPos -> {
                    int realVertex = orderedLeaves.get(vPos).getVertex();
                    if(!vertices.get(realVertex)){
                        verticesWithoutX.append(realVertex).append('-');
                    }
                });

                // save  directly into equivalence classes, don't bother manual partition refinement
                String key = verticesWithoutX.toString();
                if(equivStringToEquivClass.containsKey(key)){
                    equivStringToEquivClass.get(key).add(currentChild);
                } else {
                    LinkedList<PartitiveFamilyTreeNode> eqClass = new LinkedList<>();
                    eqClass.addLast(currentChild);
                    equivStringToEquivClass.put(key, eqClass);
                }

                currentChild = (PartitiveFamilyTreeNode) currentChild.getRightSibling();
            }
        }

        // reorder the children accordingly:
        ArrayList<PartitiveFamilyTreeNode> orderedChildren = new ArrayList<>(getNumChildren());
        for(List<PartitiveFamilyTreeNode> children : equivStringToEquivClass.values()){
            orderedChildren.addAll(children);
        }
        reorderChildren(orderedChildren);


        // return equivStringToEquivClass
    }

    private void reorderChildren(List<PartitiveFamilyTreeNode> orderedChildren){
        assert orderedChildren.size() == getNumChildren() : "Error: different number of children in List!";

        for(int index = orderedChildren.size() -1; index >= 0; index--){
            PartitiveFamilyTreeNode child = orderedChildren.get(index);
            child.makeFirstChild();
        }

    }

    /**
     * computes the cutters and identfies the node as a module in G
     * @param outNeighbors
     * @param inNeighbors
     * @return
     */
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

    public BitSet getVertices() {
        return vertices;
    }
}
