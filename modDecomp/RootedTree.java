package dicograph.modDecomp;

import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
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

    // F.L. 13.11.17: moved here
    /**
     * Computes the lowest common ancestor for a set of RootedTreeNodes.
     *
     * @param lowerNodesCollection the treenodes
     * @param log                  the logger
     * @return the LCA. null indicates an error.
     */
    public static RootedTreeNode computeLCA(Collection<RootedTreeNode> lowerNodesCollection, Logger log) {

        boolean alreadyReachedRoot = false;
        HashMap<RootedTreeNode, LinkedList<RootedTreeNode>> inputNodeToAncestors = new HashMap<>(lowerNodesCollection.size() * 4 / 3);
        HashMap<RootedTreeNode, RootedTreeNode> allTraversedNodes = new HashMap<>();

        for (RootedTreeNode lowerNode : lowerNodesCollection) {
            LinkedList<RootedTreeNode> ancestryList = new LinkedList<>();
            ancestryList.add(lowerNode);
            inputNodeToAncestors.put(lowerNode, ancestryList);
        }
        int remainingNodes = lowerNodesCollection.size();

        // Traverse the Tree bottom-up

        while (remainingNodes > 1) {
            Iterator<Map.Entry<RootedTreeNode, LinkedList<RootedTreeNode>>> nodesIter = inputNodeToAncestors.entrySet().iterator();
            while (nodesIter.hasNext()) {
                Map.Entry<RootedTreeNode, LinkedList<RootedTreeNode>> currEntry = nodesIter.next();
                RootedTreeNode parent = currEntry.getValue().peekLast().getParent();


                if(parent != null) {
                    if (parent.isRoot()) {
                        log.finest(() -> "Reached root: " + currEntry.toString());
                        // No problem if just one went up to root.
                        if (alreadyReachedRoot) {
                            log.finest("Root is LCA.");
                            return parent;
                        }
                        alreadyReachedRoot = true;
                    }

                    if (allTraversedNodes.containsKey(parent)) {

                        // already present: close this list.
                        remainingNodes--;
                        log.finest(() -> "Closing: " + currEntry.toString());

                        nodesIter.remove();
                        // We're done if this was the last.
                        if (remainingNodes == 1) {
                            if (inputNodeToAncestors.size() != 1) {
                                throw new IllegalStateException("Multiple ancestry lists: " + inputNodeToAncestors);
                            }
                            LinkedList<RootedTreeNode> lastList = inputNodeToAncestors.values().stream().findFirst().get();
                            if (lastList.contains(parent)) {
                                return parent;
                            } else {
                                log.finest(() -> "Not found: return first inner node entry of last list:");
                                log.finest(lastList::toString);
                                for (RootedTreeNode lastListNode : lastList) {
                                    if (!lastListNode.isALeaf()) {
                                        return lastListNode;
                                    }
                                }
                            }
                        }

                    } else {
                        // add the parent
                        log.finest(() -> "Adding: " + currEntry + ", parent: " + parent);
                        currEntry.getValue().addLast(parent);
                        allTraversedNodes.put(parent, currEntry.getKey());
                    }
                }
                else {
                    log.finest(() -> "Ignoring root.");
                }
            }
            log.finest(() -> "All nodes: " + allTraversedNodes);
            log.finest(() -> "Next Iteration: " + inputNodeToAncestors);

        }
        // debug
        log.severe(() -> "Error: Unexpected Exit! All nodes: " + allTraversedNodes);
        log.severe(() -> "next Iteration: " + inputNodeToAncestors);
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
    public String verifyNodeTypes(Graph<Integer, DefaultWeightedEdge> graph, boolean directed) {

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
                    //if (directed) {
                    //    anyVertex = ((PartitiveFamilyLeafNode) currChild).getVertex();
                    //} else {
                        anyVertex = ((MDTreeLeafNode) currChild).getVertexNo();
                    //}
                } else {
                    anyVertex = currChild.vertices.nextSetBit(0);
                }
                if (anyVertex >= 0)
                    childRepresentatives.add(anyVertex);
                else
                    throw new IllegalStateException("No valid child vertex found for " + currChild);

                currChild = currChild.getRightSibling();
            }


            Graph<Integer, DefaultWeightedEdge> subgraph;
            MDNodeType currNodeType = ((MDTreeNode) node).getType();
            if (directed) {
                //currNodeType = ((PartitiveFamilyTreeNode) node).getType();
                subgraph = new DirectedInducedIntSubgraph<>(graph, childRepresentatives);
            } else {
                //currNodeType = ((MDTreeNode) node).getType();
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

