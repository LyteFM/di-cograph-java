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
    private boolean isModuleInG;
    private MDNodeType type;
    private DirectedInducedIntSubgraph<DefaultEdge> inducedPartialSubgraph;
    final PartitiveFamilyTree treeContext; // reference to Tree Object



    // only needed for leaf
    protected PartitiveFamilyTreeNode(PartitiveFamilyTree tree){
        super();
        treeContext = tree;
        isModuleInG = false;
        type = null;
    }

    PartitiveFamilyTreeNode(BitSet vertexModule, PartitiveFamilyTree tree){
        this(tree);
        vertices = vertexModule;
    }

    @Override
    void removeThis() {
        super.removeThis();
        treeContext.moduleToTreenode.remove(vertices);
    }

    void reorderAllInnerNodes(Logger log,
                              BitSet[] outNeighbors, BitSet[] inNeighbors, List<PartitiveFamilyLeafNode> orderedLeaves, int[] positionInPermutation){
        if(type == MDNodeType.ORDER){
            if (!isModuleInG) {
                // delete module weak modules: ->  Replace with their children.
                log.warning(() -> "Weak module found: " + this);
                //throw new IllegalStateException("Weak module found: " + this);
                removeThis();
            } else {
                log.fine(() -> type + ": computing fact perm of tournament " + inducedPartialSubgraph);
                List<Pair<Integer, Integer>> perfectFactPerm = perfFactPermFromTournament.apply(inducedPartialSubgraph);
                // results are real vertices in a new order (first) and their outdegree (second).
                log.fine(() -> type + ": reordering and splitting merged modules according to permutation: " + perfectFactPerm);
                // ok: if a merged module has been split, it's children are processed later
                reorderAccordingToPerfFactPerm(perfectFactPerm, log);
            }
        } else if (type.isDegenerate()){
            if(isModuleInG) {
                log.fine(() -> type + " ");
                computeEquivalenceClassesAndReorderChildren(log, outNeighbors, inNeighbors, orderedLeaves, positionInPermutation);
                // todo: How about merged modules here?

            } else {
                // how to delete weak modules? -> I think I can simply add their vertices at the correct position.
                log.warning(() -> "Weak module found: " + this);
                throw new IllegalStateException("Weak module found: " + this);
            }
        } else {
            // todo: weak prime modules -> add their children!
            if (!isModuleInG) {
                log.warning("Weak Prime Module: " + this);
                throw new IllegalStateException("Weak Prime Module: " + this);
                //replaceThisByItsChildren();
            }
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
     * Based only on the Edge type of the 2-Structure H
     * @param data the data from Modular decomposition
     * @return a vertex of this node
     */
    int determineNodeTypeForH(final DirectedMD data) {

        // node type can be efficiently computed bottom-up: only take one vertex of each child to construct the module
        PartitiveFamilyTreeNode currentChild = (PartitiveFamilyTreeNode) getFirstChild();
        LinkedList<Integer> subgraphVertices = new LinkedList<>();
        if(currentChild != null) {
            while (currentChild != null) {

                if(currentChild.isALeaf()){
                    subgraphVertices.add( ((PartitiveFamilyLeafNode) currentChild).getVertex() );
                } else  {
                    subgraphVertices.add(currentChild.determineNodeTypeForH(data)); // recursion
                }

                currentChild = (PartitiveFamilyTreeNode) currentChild.getRightSibling();
            }
        }
        int returnVal = subgraphVertices.getFirst();
        // compute the induced subgraph and determine the node type
        inducedPartialSubgraph = new DirectedInducedIntSubgraph<>(data.inputGraph, subgraphVertices);
        Set<DefaultEdge> edgeSet = inducedPartialSubgraph.edgeSet();
        int n = inducedPartialSubgraph.vertexSet().size();
        if (edgeSet.isEmpty()) {
            // no edges means 0-complete
            type = MDNodeType.PARALLEL;
        } else if (edgeSet.size() == n * (n - 1)) {
            // I don't have multi-edges, so this means it's 1-complete.
            // two edges between every vertex pair.
            type = MDNodeType.SERIES;
        } else {
            boolean isOrder = edgeSet.size() == n * (n - 1) / 2;
            // 2-complete means: one edge between every vertex pair
            if (isOrder) {
                // need to brute-force check every edge
                for (DefaultEdge edge : edgeSet) {
                    int source = inducedPartialSubgraph.getEdgeSource(edge);
                    int target = inducedPartialSubgraph.getEdgeTarget(edge);
                    if (data.getEdgeValueForH(source, target) != 2) {
                        type = MDNodeType.PRIME;
                        return returnVal;
                    }
                }
                // not prime -> success!
                type = MDNodeType.ORDER;
            } else {
                type = MDNodeType.PRIME;
            }
        }

        return returnVal;
    }

    /**
     * reorders the children of this vertex accound to the perfect factorizing permutation
     * @param perfFactPerm the computed perfect factorizing permutation of the corresponding tournament
     */
    private void reorderAccordingToPerfFactPerm(List<Pair<Integer,Integer>> perfFactPerm, Logger log){

        if( type != MDNodeType.ORDER ){
            throw new IllegalStateException("Wrong type in step 5: " + type + " for node:\n" + toString());
        }

        int sz = perfFactPerm.size();
        HashMap<Integer,Integer> positionInPermutation = new HashMap<>(sz*4/3);

        // detect merged modules here. According to Th3 of the fact.perm. paper, the "true" module appears consecutively
        // therefore, there is only room for mergers at start end end of the fact.perm.
        int outScore = sz - 1;
        HashMap<Integer,Integer> primeMemberIndexInPerm = new HashMap<>();

        for(int i = 0; i<sz; i++){
            Pair<Integer,Integer> element = perfFactPerm.get(i);
            int vertex = element.getFirst();
            positionInPermutation.put(vertex, i);

            int outDeg = element.getSecond();
            if(outScore !=outDeg){
                // if it's not the expected score, you're out. need to verify if that's enough, though.
                log.fine(() -> "Detected merged vertex: " + vertex + ", outDegree " + outDeg );
                primeMemberIndexInPerm.put(i,vertex);
            }

            outScore --;
        }



        PartitiveFamilyTreeNode[] orderedNodes = new PartitiveFamilyTreeNode[sz];

        PartitiveFamilyTreeNode currentChild = (PartitiveFamilyTreeNode) getFirstChild();
        if(currentChild != null) {
            while (currentChild != null) {

                int position = -1; // want an error if not found
                int realV;
                // computes the first position of any vertex in the child module
                if(currentChild.isALeaf()){
                    realV = ((PartitiveFamilyLeafNode) currentChild).getVertex();
                    position = positionInPermutation.get(realV);
                } else {
                    int skipCount = 0;
                    for(realV = currentChild.vertices.nextSetBit(0); realV >= 0; realV = currentChild.vertices.nextSetBit(realV+1)){
                        if(positionInPermutation.containsKey(realV)){
                            position = positionInPermutation.get(realV);
                            break;
                        } else {
                            skipCount++;
                        }
                    }
                    log.fine("Checked " + skipCount + " of " + currentChild.getNumChildren() + " vertices to find position " + position);
                }

                if(orderedNodes[position] != null){
                    throw new IllegalStateException("Vertex for position " + position + " already present for node\n" + toString());
                }
                orderedNodes[position] = currentChild;

                currentChild = (PartitiveFamilyTreeNode) currentChild.getRightSibling();
            }
        }





        boolean first = true;
        boolean recoverMerged = !primeMemberIndexInPerm.isEmpty();
        BitSet trueModule;
        PartitiveFamilyTreeNode newNode;
        trueModule = new BitSet();
        newNode = new PartitiveFamilyTreeNode(trueModule, treeContext);

        // ordering is still valid, but might need to split merged module.
        ArrayList<PartitiveFamilyTreeNode> orderedChildren = new ArrayList<>(getNumChildren());
        for(int i = 0; i< orderedNodes.length; i++){
            PartitiveFamilyTreeNode node = orderedNodes[i];

            if(recoverMerged){

                if(primeMemberIndexInPerm.containsKey(i)){
                    orderedChildren.add(node);
                } else {
                    newNode.addChild(node); // also removes it from the current node.
                    if(node.isALeaf()){
                        newNode.vertices.set(perfFactPerm.get(i).getFirst());
                    } else {
                        newNode.vertices.or( node.vertices );
                    }

                    if(first){
                        orderedChildren.add(newNode);
                        first = false;
                    }
                }

            } else {
                //if (node != null) todo: warum darf das?
                orderedChildren.add(node);
            }
        }

        if (recoverMerged) {
            type = MDNodeType.PRIME;
            newNode.type = MDNodeType.ORDER;
            newNode.isModuleInG = true;
            newNode.inducedPartialSubgraph = new DirectedInducedIntSubgraph<>(inducedPartialSubgraph.getBase(), newNode.vertices);
            // todo: does it need any other properties? like LC/RC?
            addChild(newNode);
            log.fine(() -> "new child created: " + newNode.toString());
        }

        log.fine( () -> type + " Reordering children of " + toString());
        log.fine( () -> type + " according to: " + orderedChildren.toString() );
        reorderChildren(orderedChildren);
    }

    /**
     * Computes a perfect factorizing permutation of the given tournament.
     * Assertion if it is a tournament - already verified. todo: may be a merged module!!! will it be in order here???
     * "The ordering of the vertices in σ is exactly the ordering induced by the order node, otherwise there would exist some cutter."
     */
    private Function<SimpleDirectedGraph<Integer, DefaultEdge>, List<Pair<Integer,Integer>>> perfFactPermFromTournament = tournament -> {

        int n = tournament.vertexSet().size();
        HashMap<Integer, Collection<Integer>> vertexToPartition = new HashMap<>(n*4/3);
        ArrayList<Collection<Integer>> partitions = new ArrayList<>(n);
        // init: P_0 = V. This also defines the vertex Indices.
        List<Integer> VList = new ArrayList<>(tournament.vertexSet());
        partitions.add(VList);
        for(int vertex : VList) {
            vertexToPartition.put(vertex, VList);
        }

        //neuer Ansatz für merged modules:
        HashMap<Integer,Integer> vertexToOutdegree = new HashMap<>(n*4/3);

        for(int i = 0; i<n; i++){

            int realVertexNo = VList.get(i); // unnecessary, if int-vertices from 0 to n-1. necessary, if arbitrary.
            Collection<Integer> cPartition = vertexToPartition.get(realVertexNo);
            int partitionsIndex = partitions.indexOf(cPartition); // todo: ouch, linearity :/

            Set <DefaultEdge> outgoing = tournament.outgoingEdgesOf(realVertexNo);
            vertexToOutdegree.put(realVertexNo,outgoing.size());


            // skip singletons
            if (cPartition.size() > 1 && partitions.size() < n) {
                // neighborhood N_- and N_+:
                Set <DefaultEdge> incoming = tournament.incomingEdgesOf(realVertexNo);

                assert incoming.size() + outgoing.size() == n-1 : "Not a tournament: " + tournament; // may be still true for merger. Happened in small tError.

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

                // add C ∩ N_{-}(v_i), if not empty
                if(!inNeighbors.isEmpty()) {
                    partitions.add(partitionsIndex, inNeighbors);
                    for (int vNo : inNeighbors) {
                        vertexToPartition.put(vNo, inNeighbors); // todo: or still put?
                    }
                    partitionsIndex++;
                }

                // add singleton {v_i}
                ArrayList<Integer> singleton = new ArrayList<>(1);
                singleton.add(realVertexNo);
                partitions.add(partitionsIndex, singleton);
                vertexToPartition.put(realVertexNo, singleton);
                partitionsIndex++;

                // add C ∩ N_{+}(v_i), if not empty
                if(!outNeigbors.isEmpty()) {
                    partitions.add(partitionsIndex, outNeigbors);
                    for (int vNo : outNeigbors) {
                        vertexToPartition.put(vNo, outNeigbors); // todo: or still update???
                    }
                }
            }
        }

        ArrayList<Pair<Integer,Integer>> ret = new ArrayList<>(n);

        for(Collection<Integer> singleton : partitions){
            if(singleton.size() != 1) {
                throw new IllegalStateException("Error: invalid element " + singleton + " of partitions: " + partitions + "\nvertexToPartition: " + vertexToPartition);
            } else {
                int realVertexNo = singleton.stream().findFirst().get();
                ret.add(new Pair<>(realVertexNo, vertexToOutdegree.get(realVertexNo))); // todo: habe ich hier bereits die ungültigen???
            }
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

        HashMap<Pair<BitSet,BitSet>, List<PartitiveFamilyTreeNode>> equivClassByBits = new HashMap<>(getNumChildren()*4/3);

        PartitiveFamilyTreeNode currentChild = (PartitiveFamilyTreeNode) getFirstChild();
        if(currentChild != null) {
            while (currentChild != null) {
                // take any vertex of child Y and prune vertices of this node X from its adjacency list
                // note: this is the real vertex number, not permutation element.
                int anyVertex;
                if(currentChild.isALeaf()) {
                    anyVertex = ((PartitiveFamilyLeafNode) currentChild).getVertex();
                } else {
                    anyVertex = currentChild.vertices.nextSetBit(0);
                }
                int vertexPosition = positionInPermutation[anyVertex];

                // compute S_{+}
                BitSet outgoingWithoutX = new BitSet(orderedLeaves.size());
                BitSet outNeighborPositions = outNeighbors[vertexPosition];
                // adds the vertex to the list, if it's not a vertex of this treenode
                outNeighborPositions.stream().forEach( vPos -> {
                        int realVertex = orderedLeaves.get(vPos).getVertex();
                        if(!vertices.get(realVertex)){
                            outgoingWithoutX.set(realVertex);
                        }
                });

                // compute S_{-}
                BitSet incomingWithoutX = new BitSet(orderedLeaves.size());
                BitSet incNeighborPositions = inNeighbors[vertexPosition];
                // adds the vertex to the list, if it's not a vertex of this treenode
                incNeighborPositions.stream().forEach( vPos -> {
                    int realVertex = orderedLeaves.get(vPos).getVertex();
                    if(!vertices.get(realVertex)){
                        incomingWithoutX.set(realVertex);
                    }
                });

                // save  directly into equivalence classes, don't bother manual partition refinement
                Pair<BitSet,BitSet> key = new Pair<>(outgoingWithoutX,incomingWithoutX);
                if(equivClassByBits.containsKey(key)){
                    equivClassByBits.get(key).add(currentChild);
                    log.fine(() -> type + " adding child with vertex " + anyVertex + " to eqClass " + key);
                } else {
                    LinkedList<PartitiveFamilyTreeNode> eqClass = new LinkedList<>();
                    eqClass.addLast(currentChild);
                    equivClassByBits.put(key, eqClass);
                    log.fine(() -> type + " creating new eqClass " + key + " for child with vertex " + anyVertex );
                }

                currentChild = (PartitiveFamilyTreeNode) currentChild.getRightSibling();
            }
        }

        // todo: need to spot and handle merged modules here!
        if (equivClassByBits.size() > 1) {
            throw new IllegalStateException("Found several equiv classes in step 5 :)");
        }

        // reorder the children accordingly:
        ArrayList<PartitiveFamilyTreeNode> orderedChildren = new ArrayList<>(getNumChildren());
        for(List<PartitiveFamilyTreeNode> children : equivClassByBits.values()){
            orderedChildren.addAll(children);
        }
        log.fine( () -> type + " Reordering children of " + toString());
        log.fine( () -> type + " according to: " + orderedChildren.toString() );
        reorderChildren(orderedChildren);
    }

    private void reorderChildren(List<PartitiveFamilyTreeNode> orderedChildren){
        if( orderedChildren.size() != getNumChildren()){
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
    protected Pair<Integer,Integer> computeLeftRightCutter(BitSet[] outNeighbors, BitSet[] inNeighbors, int[] positionInPermutation, Logger log){

        lc_X = outNeighbors.length;
        rc_X = 0;
        int first;
        int second;
        // iterate over children, compute this and the cutter for their re/le
        PartitiveFamilyTreeNode currentChild = (PartitiveFamilyTreeNode) getFirstChild();
        PartitiveFamilyTreeNode nextChild;
        if(currentChild != null) {
            while (currentChild != null) {

                // compute and compare with its real cutters. just the vertexNo, if child is leaf
                Pair<Integer,Integer> childsCutters = currentChild.computeLeftRightCutter(outNeighbors,inNeighbors, positionInPermutation, log);
                first = childsCutters.getFirst();
                second = childsCutters.getSecond();

                if(lc_X > first)
                    lc_X = first;
                if(rc_X < second)
                    rc_X = second;

                // compute and compare with cutters of le/re
                nextChild = (PartitiveFamilyTreeNode) currentChild.getRightSibling();
                if(nextChild != null){
                    int re_leftNode = currentChild.re_X;
                    int le_rightNode = nextChild.le_X;
                    Pair<Integer,Integer> leftRightCutters = computeLeftRightCutterForVertices(
                            re_leftNode, le_rightNode, outNeighbors, inNeighbors);
                    first = leftRightCutters.getFirst();
                    second = leftRightCutters.getSecond();

                    if(first == -1 || second == -1){
                        throw new IllegalStateException("Error in computing left and right cutter from Bitsets:\nout: " + outNeighbors[re_leftNode] +
                                "\nin: " +inNeighbors[le_rightNode] + " \nwith cutter for left: " + first + ", right: " + second +
                                " for x = " + re_leftNode + ", y = " + le_rightNode + "\nfor node " + toString());
                    }

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
            log.fine( () -> "Module found - LC: " + lc_X + ", RC: " + rc_X + " for node: " + toString());
        } else {
            log.fine( () -> "Discard node - LC: " + lc_X + ", RC: " + rc_X + " for node: " + toString());
        }

        return new Pair<>(lc_X,rc_X);
    }

    /**
     * Helper method to compute left and right cutter of two given vertices (end of proof for Lem. 24).
     * A return value == -1 should be caught as an error.
     * @param re_left  re(X) of the left  node X - i.e. a position in σ
     * @param le_right le(Y) of the right node Y - i.e. a position in σ
     * @param outNeighbors for each index σ(v), the indices σ(w) of each outgoing edge (v,w)
     * @param inNeighbors  for each index σ(v), the indices σ(u) of each incoming edge (u,v)
     * @return the left and right cutters as positions in σ
     */
    private static Pair<Integer, Integer> computeLeftRightCutterForVertices(int re_left, int le_right, BitSet[] outNeighbors, BitSet[] inNeighbors){

        BitSet inSymDiff  = SortAndCompare.symDiff(outNeighbors[re_left], outNeighbors[le_right]);
        BitSet outSymDiff = SortAndCompare.symDiff(inNeighbors[re_left], inNeighbors[le_right]);

        inSymDiff.or(outSymDiff);

        boolean first = true;
        int leftCutter = re_left;
        int rightCutter = le_right; // todo: wie mit leerer Symdiff umgehen?
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
    void getLeavesInLeftToRightOrder(List<PartitiveFamilyLeafNode> orderedLeaves){

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
     * according to Lemma 24. This works, because no reordering has occured yet.
     * @return first Element: le_X, second Element: re_X
     */
     Pair<Integer,Integer> computeReAndLe(Logger log){

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

    public boolean isModuleInG() {
        return isModuleInG;
    }


    public String toString() {

        StringBuilder result = new StringBuilder("(");
        if(isRoot())
            result.append("ROOT ");
        if(type != null)
            result.append(type.toString()).append(", ");
        result.append("numChildren=").append(getNumChildren());

        RootedTreeNode current = getFirstChild();
        if (current != null) {
            result.append(current);
            current  = current.getRightSibling();
        }
        while (current != null) {
            result.append(", ").append(current);
            current = current.getRightSibling();
        }
        return result.append(')').toString();
    }

    public MDNodeType getType() {
        return type;
    }
}
