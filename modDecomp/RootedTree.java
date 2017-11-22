package dicograph.modDecomp;

import org.jgrapht.Graph;
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
        int currDistance = 0;
        // Traverse the Tree bottom-up

        while (lastNodeRemaining.size() > 1) {
            Iterator<Map.Entry<Integer, LinkedList<RootedTreeNode>>> nodesIter = inputNodeToAncestor.entrySet().iterator();
            while (nodesIter.hasNext()) {
                Map.Entry<Integer, LinkedList<RootedTreeNode>> currEntry = nodesIter.next();
                RootedTreeNode parent = currEntry.getValue().peekLast().getParent();
                if (allTraversedNodes.containsKey(parent)) {

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
                        return parent;
                    }

                } else {
                    // add the parent
                    currEntry.getValue().addLast(parent);
                    allTraversedNodes.put(parent, currEntry.getKey());
                }
            }
            currDistance++;

        }
        log.warning("Error: Unexpected Exit! current Distance: " + currDistance);
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
                subgraph = new DirectedInducedIntSubgraph<DefaultEdge>(graph, childRepresentatives);
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

