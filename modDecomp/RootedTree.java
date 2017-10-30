package dicograph.modDecomp;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/*
 * A rooted tree.
 */
class RootedTree {

	// The root of the tree.
	private RootedTreeNode root;
	
	
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

    /** F.L. 16.10.17:
     * Returns a String representation of the strong members of the tree's set family, i.e. the inner nodes.
     */
    public ArrayList<String> getSetRepresentationAsStrings(){
        ArrayList<String> ret = new ArrayList<String>();
        root.getSetRepresentation(ret);
        return ret;
    }

    public HashMap<BitSet, RootedTreeNode> getStrongModulesBool(Map<String, Integer> vertexToIndex, MDTreeLeafNode[] leaves) {
        HashMap<BitSet, RootedTreeNode> ret = new HashMap<>();
        root.getStrongModulesBool(vertexToIndex, leaves, ret);

        return ret;
    }

    protected ArrayList<ArrayList<Integer>> getStrongModulesIntList(Map<String, Integer> vertexToIndex) {
        ArrayList<ArrayList<Integer>> ret = new ArrayList<>();
        root.getStrongModulesIntList(vertexToIndex, ret);

        return ret;
    }
}

