package dicograph.modDecomp;

import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import dicograph.graphIO.DirectedInducedIntSubgraph;
import dicograph.graphIO.UndirectedInducedIntSubgraph;

/*
 * A rooted tree.
 */
class RootedTree {

    // F.L. 02.11.17: added
    protected HashMap<BitSet, RootedTreeNode> moduleToTreenode;
    //

	// The root of the tree.
	protected RootedTreeNode root;
	
	
	/* The default constructor. */
	protected RootedTree() {
		root = null;
	}
	
	
	/* 
	 * Creates a rooted tree with the given root.
	 * @param r The root of the newly created tree.
	 */ 
	protected RootedTree(RootedTreeNode r) {
		root = r; 
	}	
	
	
	/*
	 * Resets the root of this tree to be the node supplied.
	 * Effectively changes this tree to the one rooted at the supplied node. 
	 * @param r The new root of this tree.
	 */
	protected void setRoot(RootedTreeNode r) {
		root = r;
	}
	
	
	/* 
	 * Returns a string representation of this tree.
	 * @return The string representation of this tree.
	 */
	public String toString() {
		return root.toString();
	}

    // F.L. 02.11.17
    public HashMap<BitSet, RootedTreeNode> getModuleToTreenode() {
        return moduleToTreenode;
    }

    public void setModuleToTreenode(HashMap<BitSet, RootedTreeNode> moduleToTreenode) {
        this.moduleToTreenode = moduleToTreenode;
    }

    public static RootedTreeNode computeLCA(Collection<RootedTreeNode> lowerNodesCollection, Logger log) {

        boolean alreadyReachedRoot = false;
        // init
        ArrayList<RootedTreeNode> lowerNodes = new ArrayList<>(lowerNodesCollection);

        HashMap<Integer, LinkedList<RootedTreeNode>> inputNodeToAncestor = new HashMap<>(lowerNodes.size() * 4 / 3);
        HashMap<RootedTreeNode, Integer> allTraversedNodes = new HashMap<>();
        HashSet<Integer> lastNodeRemaining = new HashSet<>();

        for (int i = 0; i < lowerNodes.size(); i++) {
            LinkedList<RootedTreeNode> list = new LinkedList<>();
            list.add(lowerNodes.get(i));
            allTraversedNodes.put(lowerNodes.get(i), i);
            inputNodeToAncestor.put(i, list);
            lastNodeRemaining.add(i);
        }
        log.fine("List: " + lowerNodes);
        int currDistance = 0;
        // Traverse the Tree bottom-up

        while (lastNodeRemaining.size() > 1) {
            Iterator<Map.Entry<Integer, LinkedList<RootedTreeNode>>> nodesIter = inputNodeToAncestor.entrySet().iterator();
            while (nodesIter.hasNext()) {
                Map.Entry<Integer, LinkedList<RootedTreeNode>> currEntry = nodesIter.next();
                RootedTreeNode parent = currEntry.getValue().peekLast().getParent();
                if (allTraversedNodes.containsKey(parent)) { // ignore root

                    // already present: close this list.
                    lastNodeRemaining.remove(currEntry.getKey());
                    if (parent.isRoot()) {
                        log.fine("Reached root: " + currEntry.toString());
                        // No problem if just one went up to root.
                        if (alreadyReachedRoot) {
                            log.fine("Root is LCA.");
                            return parent;
                        }
                        alreadyReachedRoot = true;
                    } else {
                        log.fine("Closing: " + currEntry.toString());
                    }
                    nodesIter.remove();
                    // We're done if this was the last.
                    if (lastNodeRemaining.size() == 1) {
                        int lastIndex = lastNodeRemaining.stream().findFirst().get();
                        LinkedList<RootedTreeNode> lastList = inputNodeToAncestor.get(lastIndex);
                        // now, the question is: which is the correct element??? Additional problem: length could have changed or not...
                        if(lastList.contains(parent)) {
                            log.fine("Found: return parent.");
                            return parent;
                        } else {
                            log.fine("Not found: return first inner node entry of last list.");
                            log.fine(lastList.toString());
                            for(RootedTreeNode lastListNode : lastList){
                                if(!lastListNode.isALeaf()){
                                    return lastListNode;
                                }
                            }
                        }
                    }

                } else if (parent != null){
                    // add the parent
                    log.fine("Adding: " + currEntry + ", parent: " + parent);
                    currEntry.getValue().addLast(parent);
                    allTraversedNodes.put(parent, currEntry.getKey());
                } else {
                    log.fine("Ignoring root.");
                }
            }
            currDistance++;
            log.fine("dist " + currDistance + " all: " + allTraversedNodes);
            log.fine("next Iteration: " + inputNodeToAncestor);

        }
        log.warning("Error: Unexpected Exit! current Distance: " + currDistance);
        return null;
    }

    // F.L. 13.11.17: moved here
    /**
     * Computes the lowest common ancestor for a set of RootedTreeNodes.
     *
     * @param lowerNodesCollection the treenodes
     * @param log                  the logger
     * @return the LCA. null indicates an error.
     */
    public static RootedTreeNode computeLCAfals(Collection<RootedTreeNode> lowerNodesCollection, Logger log) {

        boolean alreadyReachedRoot = false;

        // active initial nodes and possible parent nodes might be LCAs
        HashMap<RootedTreeNode,RootedTreeNode> inputNodeToLCA = new HashMap<>(lowerNodesCollection.size() * 4 / 3);
        HashMap<RootedTreeNode, RootedTreeNode> parentNodeToInputNode = new HashMap<>(lowerNodesCollection.size() * 4 / 3); // value ist immer inputNode!
        for(RootedTreeNode lowerNode : lowerNodesCollection){
            parentNodeToInputNode.put(lowerNode,lowerNode);
            inputNodeToLCA.put(lowerNode,lowerNode);
        }

        // not necessary with new approach
        /*
        HashMap<RootedTreeNode, Integer> allTraversedNodes = new HashMap<>();
        HashSet<Integer> lastNodeRemaining = new HashSet<>();

        for (int i = 0; i < lowerNodesToRemember.size(); i++) {
            LinkedList<RootedTreeNode> list = new LinkedList<>();
            list.add(lowerNodesToRemember.get(i));
            allTraversedNodes.put(lowerNodesToRemember.get(i), i);
            parentNodeToLowerNode.put(i, list);
            lastNodeRemaining.add(i);
        }
        */
        int currDistance = 0;
        int count = lowerNodesCollection.size();
        // Traverse the Tree bottom-up
        // todo: can parent ever be null?

        while (count > 1) {
            Iterator<Map.Entry<RootedTreeNode, RootedTreeNode>> nodesIter = parentNodeToInputNode.entrySet().iterator();
            LinkedList<Pair<RootedTreeNode,RootedTreeNode>> possibleLCAsToAdd = new LinkedList<>();
            // 1st iteration: all initial nodes
            while (nodesIter.hasNext()) {
                Map.Entry<RootedTreeNode, RootedTreeNode> currEntry = nodesIter.next();
                RootedTreeNode currLCAcandidate = currEntry.getKey();
                RootedTreeNode inputNode = currEntry.getValue();
                RootedTreeNode currParent = currLCAcandidate.getParent();
                final int remCount = count;
                if(currParent == null){
                    log.fine(() -> remCount + " - reached parent of root: " + currLCAcandidate);

                } else {

                    if (parentNodeToInputNode.containsKey(currParent)) {
                        if (currParent.isRoot()) {
                            log.fine(() -> remCount + " - reached root: " + currLCAcandidate);
                            // If two paths reach root, root is LCA.
                            if (alreadyReachedRoot) {
                                log.fine("Root is LCA.");
                                return currParent;
                            }
                            alreadyReachedRoot = true;
                        } else {
                            log.fine(() -> remCount + " - closing: " + currLCAcandidate);
                        }
                        // already present: remove this entry and its corresponding input node
                        count--;
                        // replace returns the old val
                        RootedTreeNode prevLCA = inputNodeToLCA.replace(inputNode, currParent); // update the LCA for the inputNode
                        nodesIter.remove();
                        // We're done if this was the last.
                        if (count == 0) {

                            //if(currParent == prevLCA) {
                                return currParent;
                            //} else {
                                // problem: parent of last element might not necessarily be the LCA, but simply the last found (lower) element

                            //}
                        }

                    } else {
                        log.fine(() -> remCount + " - adding: " + currLCAcandidate);
                        possibleLCAsToAdd.add(new Pair<>(currParent, inputNode ));
                        // inputNodeToLCA.put() // todo: or also update here??? but maybe inputNode itself is LCA.
                    }
                }

            }

            // add the parent and the corresp. lower node
            for(Pair<RootedTreeNode,RootedTreeNode> toAdd : possibleLCAsToAdd){
                parentNodeToInputNode.put(toAdd.getFirst(),toAdd.getSecond());
                if(count == 1){
                    return toAdd.getFirst();
                }
            }
            currDistance++;
        }

        log.warning(() -> "Error: Unexpected Exit!");
        return null;
    }

    /** F.L. 16.10.17:
     * Returns a String representation of the strong members of the tree's set family, i.e. the inner nodes.
     * todo: ggf das zu PartitiveFamilyTreeNode extends MDTreeNode...
     */
    public ArrayList<String> getSetRepresentationAsStrings(){
        ArrayList<String> ret = new ArrayList<String>();
        root.getSetRepresentation(ret);
        return ret;
    }


    // F.L. 16.11.17: Debug option (via moduleToTreenode)
    public String verifyNodeTypes(Graph<Integer, DefaultEdge> graph, boolean directed) {

        StringBuilder builder = new StringBuilder();
        LinkedList<RootedTreeNode> allNodes = new LinkedList<>(moduleToTreenode.values());
        if(!allNodes.contains(root))
            allNodes.add(root);

        for (RootedTreeNode node : allNodes) {


            LinkedList<Integer> childRepresentatives = new LinkedList<>();
            RootedTreeNode currChild = node.getFirstChild();
            while (currChild != null) {
                int anyVertex;
                if (currChild.isALeaf()) {
                    if (directed) {
                        anyVertex = ((PartitiveFamilyLeafNode) currChild).getVertex();
                    } else {
                        anyVertex = ((MDTreeLeafNode) currChild).getVertexNo();
                    }
                } else {
                    anyVertex = currChild.vertices.nextSetBit(0);
                }
                if (anyVertex >= 0)
                    childRepresentatives.add(anyVertex);
                else
                    throw new IllegalStateException("No valid child vertex found for " + currChild);

                currChild = currChild.getRightSibling();
            }


            Graph<Integer, DefaultEdge> subgraph;
            MDNodeType currNodeType;
            if (directed) {
                currNodeType = ((PartitiveFamilyTreeNode) node).getType();
                subgraph = new DirectedInducedIntSubgraph<>(graph, childRepresentatives);
            } else {
                currNodeType = ((MDTreeNode) node).getType();
                subgraph = new UndirectedInducedIntSubgraph<>(graph, childRepresentatives);
            }
            String verificationResult = MDNodeType.verifyNodeType(directed, subgraph, graph, currNodeType, childRepresentatives, node);
            builder.append(verificationResult);

        }

        return builder.toString();
    }

    // F.L. 18.11.17: export Tree as .dot
    public String exportAsDot(){
        StringBuilder output = new StringBuilder("digraph G {\n");
        int[] counter = new int[1];
        counter[0] = -1;
        root.exportAsDot(output,counter);
        output.append("}\n");
        return output.toString();
    }


}

