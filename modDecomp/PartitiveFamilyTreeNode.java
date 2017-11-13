package dicograph.modDecomp;

import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

import dicograph.graphIO.DirectedInducedIntSubgraph;
import dicograph.utils.SortAndCompare;

/**
 * Created by Fynn Leitow on 29.10.17.
 */
public class PartitiveFamilyTreeNode extends RootedTreeNode {

    int le_X;
    int re_X;
    int lc_X;
    int rc_X;
    private BitSet vertices;
    private boolean isModuleInG;
    private MDNodeType type; // null by default???
    private DirectedInducedIntSubgraph<DefaultEdge> inducedPartialSubgraph;



    // only needed for leaf
    PartitiveFamilyTreeNode(){
        super();
        isModuleInG = false;
        vertices = null;
        type = null;
    }

    PartitiveFamilyTreeNode(BitSet vertexModule){
        super();
        isModuleInG = false;
        vertices = vertexModule;
        type = null;
    }

    void reorderAllInnerNodes(Logger log,
            BitSet[] outNeighbors, BitSet[] inNeighbors, List<PartitiveFamilyLeafNode> orderedLeaves, int[] positionInPermutation){
        if(type == MDNodeType.ORDER){
            log.fine(() -> type + ": computing fact perm of tournament " + inducedPartialSubgraph);
            List<Integer> perfectFactPerm = perfFactPermFromTournament.apply(inducedPartialSubgraph);
            log.fine(() -> type + ": reordering according to permutation: " + perfectFactPerm);
            reorderAccordingToPerfFactPerm(perfectFactPerm, log);
        } else if (type.isDegenerate() && isModuleInG){ // todo: really only with the flag?
            log.fine(() -> type + " ");
            computeEquivalenceClassesAndReorderChildren(log, outNeighbors, inNeighbors, orderedLeaves, positionInPermutation);
        }

        PartitiveFamilyTreeNode currentChild = (PartitiveFamilyTreeNode) getFirstChild();
        if(currentChild != null) {
            while (currentChild != null) {
                if(!currentChild.isALeaf()){
                    currentChild.reorderAllInnerNodes(log,outNeighbors, inNeighbors, orderedLeaves, positionInPermutation);
                }

                currentChild = (PartitiveFamilyTreeNode) currentChild.getRightSibling();
            }
        }
    }

    /**
     * Initialization for Step 4 and 5: computes the induced subgraphs to determine the node types.
     * @param data the data from Modular decomposition
     * @return a vertex of this node
     */
     int determineNodeType(final DirectedMD data){

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
        inducedPartialSubgraph = new DirectedInducedIntSubgraph<>(data.inputGraph, subgraphVertices);
        if(inducedPartialSubgraph.edgeSet().isEmpty()){
            // no edges means 0-complete
            type = MDNodeType.PARALLEL;
        } else {
            int typeVal = -1;
            int currVal;
            boolean firstRun = true;
            for(DefaultEdge edge : inducedPartialSubgraph.edgeSet()){
                int source = inducedPartialSubgraph.getEdgeSource(edge);
                int target = inducedPartialSubgraph.getEdgeTarget(edge);
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
                    assert !inducedPartialSubgraph.isTournament() : type + " but node is a tournament: " + toString();
                    break;
                case 2:
                    type = MDNodeType.ORDER;
                    assert inducedPartialSubgraph.isTournament() : type + " but node not a tournament: " + toString();
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
    private void reorderAccordingToPerfFactPerm(List<Integer> perfFactPerm, Logger log){

        if( type != MDNodeType.ORDER ){
            throw new IllegalStateException("Wrong type in step 5: " + type + " for node:\n" + toString());
        }

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

                if(orderedNodes[firstPosition] != null){
                    throw new IllegalStateException("Vertex for position " + firstPosition + " already present for node\n" + toString());
                }
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
        log.fine( () -> type + " Reordering children of " + toString());
        log.fine( () -> type + " according to: " + orderedChildren.toString() );
        reorderChildren(orderedChildren);
    }

    /**
     * Computes a perfect factorizing permutation of the given tournament.
     * Assertion if it is a tournament - already verified.
     */
    private Function<SimpleDirectedGraph<Integer, DefaultEdge>, List<Integer>> perfFactPermFromTournament = tournament -> {

        int n = tournament.vertexSet().size();
        ArrayList<Integer> ret = new ArrayList<>(n);
        HashMap<Integer, Collection<Integer>> vertexToPartition = new HashMap<>(n*4/3);
        ArrayList<Collection<Integer>> partitions = new ArrayList<>(n);
        // init: P_0 = V. This also defines the vertex Indices.
        List<Integer> VList = new ArrayList<>(tournament.vertexSet());
        partitions.add(VList);
        for(int vertex : VList) {
            vertexToPartition.put(vertex, VList);
        }

        for(int i = 0; i<n; i++){

            int realVertexNo = VList.get(i); // unnecessary, if int-vertices from 0 to n-1. necessary, if arbitrary.
            Collection<Integer> cPartition = vertexToPartition.get(realVertexNo);
            int partitionsIndex = partitions.indexOf(cPartition);

            // skip singletons
            if(cPartition.size() > 1) {
                // neighborhood N_- and N_+:
                Set<DefaultEdge> incoming = tournament.incomingEdgesOf(realVertexNo);
                Set <DefaultEdge> outgoing = tournament.outgoingEdgesOf(realVertexNo);

                assert incoming.size() + outgoing.size() == n-1 : "Not a tournament: " + tournament;

                HashSet<Integer> inNeighbors = new HashSet<>(incoming.size()*4/3);
                for( DefaultEdge edge : incoming){
                    int source = tournament.getEdgeSource(edge);
                    inNeighbors.add( source );
                }
                inNeighbors.retainAll(cPartition); // Compute the intersection

                HashSet<Integer> outNeigbors = new HashSet<>(outgoing.size()*4/3);
                for( DefaultEdge edge : outgoing){
                    int source = tournament.getEdgeTarget(edge);
                    outNeigbors.add( source );
                }
                outNeigbors.retainAll(cPartition);

                // update partitions and update map
                // remove the former C
                vertexToPartition.remove(realVertexNo);
                partitions.remove(partitionsIndex);

                // add C ∩ N_{-}(v_i)
                partitions.add(partitionsIndex, inNeighbors);
                for( int vNo : inNeighbors){
                    vertexToPartition.put(vNo, inNeighbors);
                }

                // add singleton {v_i}
                ArrayList<Integer> singleton = new ArrayList<>(1);
                singleton.add(realVertexNo);
                partitions.add(partitionsIndex+1, singleton);
                vertexToPartition.put(realVertexNo, singleton);

                // add C ∩ N_{+}(v_i)
                partitions.add(partitionsIndex+2, outNeigbors);
                for(int vNo : outNeigbors){
                    vertexToPartition.put(vNo, outNeigbors);
                }

                // done.
                if (partitions.size() == n) {
                    break;
                }
            }
        }

        for(Collection<Integer> singleton : partitions){
            assert singleton.size() != 1 : "Error: invalid element " + singleton.toString();
            ret.add(singleton.stream().findFirst().get());
        }

        return ret;
    };

    /**
     * Step 4: reorders the Tree according to its equivalence classes
     * @param outNeighbors
     * @param inNeighbors
     * @param orderedLeaves
     * @param positionInPermutation
     */
    private void computeEquivalenceClassesAndReorderChildren( Logger log,
            BitSet[] outNeighbors, BitSet[] inNeighbors, List<PartitiveFamilyLeafNode> orderedLeaves, int[] positionInPermutation){
        // BitSets are according to the position, not the true vertex!
        // for the sake of simplicity, I use strings and compare if they are equal.
        // running time is same as worst case when comparing the lists element by element.

        if( !type.isDegenerate() ){
            throw new IllegalStateException("Wrong type in step 4: " + type + " for node\n" + toString());
        }

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
                    log.fine(() -> type + " adding child with vertex " + anyVertex + " to eqClass " + key);
                } else {
                    LinkedList<PartitiveFamilyTreeNode> eqClass = new LinkedList<>();
                    eqClass.addLast(currentChild);
                    equivStringToEquivClass.put(key, eqClass);
                    log.fine(() -> type + " creating new eqClass " + key + " for child with vertex " + anyVertex );
                }

                currentChild = (PartitiveFamilyTreeNode) currentChild.getRightSibling();
            }
        }

        // reorder the children accordingly:
        ArrayList<PartitiveFamilyTreeNode> orderedChildren = new ArrayList<>(getNumChildren());
        for(List<PartitiveFamilyTreeNode> children : equivStringToEquivClass.values()){
            orderedChildren.addAll(children);
        }
        log.fine( () -> type + " Reordering children of " + toString());
        log.fine( () -> type + " according to: " + orderedChildren.toString() );
        reorderChildren(orderedChildren);
    }

    private void reorderChildren(List<PartitiveFamilyTreeNode> orderedChildren){
        if( orderedChildren.size() == getNumChildren()){
            throw new IllegalStateException("Error: " + getNumChildren() + " for node " + toString() +
                    " \ndifferent number of children in List: \n" + orderedChildren);
        }

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
    protected Pair<Integer,Integer> computeLeftRightCutterForThis(BitSet[] outNeighbors, BitSet[] inNeighbors, Logger log){

        lc_X = outNeighbors.length;
        rc_X = 0;
        int first;
        int second;
        // iterate over children, compute this and the cutter for their re/le
        PartitiveFamilyTreeNode currentChild = (PartitiveFamilyTreeNode) getFirstChild();
        PartitiveFamilyTreeNode nextChild;
        if(currentChild != null) {
            while (currentChild != null) {

                // compute and compare with its real cutters
                Pair<Integer,Integer> childsCutters = currentChild.computeLeftRightCutterForThis(outNeighbors,inNeighbors, log);
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
        log.fine( () -> "LC: " + lc_X + ", RC: " + rc_X + " for node: " + toString());

        return new Pair<>(lc_X,rc_X);
    }

    /**
     * Helper method to compute left and right cutter of two given vertices
     */
    private static Pair<Integer, Integer> computeLeftRightCutterForVertices(int x, int y, BitSet[] outNeighbors, BitSet[] inNeighbors){
        // compute the symmetrical difference. Need to copy!
        BitSet inSymDiff  = SortAndCompare.symDiff(outNeighbors[x], outNeighbors[y]);
        BitSet outSymDiff = SortAndCompare.symDiff(inNeighbors[x], inNeighbors[y]);

        inSymDiff.or(outSymDiff); // ∪

        boolean first = true;
        int leftCutter = -1; // todo: was, wenn's keine gibt? i = -1 auch in loop.
        int rightCutter = -1;
        for (int i = inSymDiff.nextSetBit(0); i >= 0; i = inSymDiff.nextSetBit(i+1)) {
            if(first) {
                leftCutter = i;
            }
            rightCutter = i;
            first = false;
        }
        if(leftCutter == -1 || rightCutter == -1){
            throw new IllegalStateException("Error in computing left and right cutter from Bitsets:\n out: " + outNeighbors[x] +
                    ", in: " +inNeighbors[x] + " \nwith cutter for left: " + leftCutter + ", right: " + rightCutter);
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
    protected Pair<Integer,Integer> computeReAndLe(Logger log){

        RootedTreeNode currentChild = getFirstChild();
        // I assume that the leaves are already set. If this is a leaf, currentChild is null.

        if(currentChild != null){
            boolean firstRun = true;
            while (currentChild != null){

                // follow the leftmost branch before checking the right sibling.
                PartitiveFamilyTreeNode realNode = (PartitiveFamilyTreeNode) currentChild;
                Pair<Integer,Integer> vals = realNode.computeReAndLe(log);
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
        if(!isALeaf()){
            log.fine( () -> "1st occ of a v ∈ X: " + le_X + ", last: " + re_X + ", X = " + toString());
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
