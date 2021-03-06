package dicograph.modDecomp;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dicograph.Editing.PrimeSubgraph;
import dicograph.utils.Edge;

/**
 *   This source file is part of the program for computing the modular
 *   decomposition of undirected graphs. Adapted for DCEdit.
 *   Copyright (C) 2010 Marc Tedder
 *   Copyright (C) 2017 Fynn Leitow
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * An internal node in a modular decomposition tree.
 */
public class MDTreeNode extends RootedTreeNode {
	
	// The type of this node, either PRIME, PARALLEL, or SERIES.
	// The default is PRIME.
	private MDNodeType type;
	
	// The number of this node's (co-)component in the recursively
	// computed modular decomposition trees for the subproblem in 
	// which this node is a member.
	private int compNumber;
	// The default value assigned to a node for its compNumber.
	protected static final int DEF_COMP_NUM = -1;
	
	// The number of this node's tree in the recursively computed
	// modular decomposition trees for the subproblem in which this
	// node is a member.
	private int treeNumber;
	// The default value assigned to a node for its treeNumber.
	protected static final int DEF_TREE_NUM = -1;
	
	// The number of marks this node has accumulated in the construction
	// of the MD tree for the subproblem in which this node is a member.
	private int numMarks;
	
	// The type of split this node has received during the refinement stage
	// of constructing the MD tree for the subproblem in which this node
	// is a member.
	private SplitDirection splitType;
	
	
	/* The default constructor. */
	protected MDTreeNode() {
		super();
		type = MDNodeType.PRIME;
		compNumber = DEF_COMP_NUM;
		treeNumber = DEF_TREE_NUM;
		numMarks = 0;
		splitType = SplitDirection.NONE;
	}
	
	
	/* 
	 * The copy-constructor.  Receives the value of the supplied node's
	 * fields but none of its children.
	 * @param copy The node from which the field values are to be taken.
	 */
	protected MDTreeNode(MDTreeNode copy) {
		super();
		type = copy.type;
		compNumber = copy.compNumber;
		treeNumber = copy.treeNumber;
		numMarks = copy.numMarks;
		splitType = copy.splitType;
	}
	
	
	/* 
	 * Creates a new node with the supplied type and default values for the
	 * rest of its fields.
	 * @param type The type the node is assigned.
	 */
	protected MDTreeNode(MDNodeType type) {
		this();
		this.type = type;
	}


	// F.L. 09.11.: moved here
    /**
     * Using a BitSet for easy UNION-Computation lateron. Running Time: O(n (1 + Anzahl module))
     * @param leaves the empty but size-initialized array for storing all leafs
     * @param modules
     * @return
     */
    public BitSet getStrongModulesBool(MDTreeLeafNode[] leaves, HashMap<BitSet, RootedTreeNode> modules) {

        BitSet ret = new BitSet(leaves.length);
        MDTreeNode currentChild = (MDTreeNode) getFirstChild();
        if(currentChild != null){
            while (currentChild != null){
                if(currentChild.isALeaf()){
                    MDTreeLeafNode leafNode = (MDTreeLeafNode) currentChild;
                    int index = leafNode.getVertexNo(); // 0 <= index  <= n-1
                    ret.set(index);
                    leaves[index] = leafNode;
                } else {
                    BitSet childSet = currentChild.getStrongModulesBool(leaves, modules);
                    ret.or(childSet);
                }
                currentChild = (MDTreeNode) currentChild.getRightSibling();
            }
            if(!isRoot()){
                modules.put(ret, this);
            }
        }

        vertices = ret;
        return ret;
    }

	
	/* Adds one to the number of marks this node has received. */
	protected void addMark() {
		numMarks++;
	}
	
	
	/* 
	 * Returns true iff the number of marks this node has received equals
	 * the number of its children.
	 */
	protected boolean isFullyMarked() {
		return (numMarks == getNumChildren());
	}
	
	
	/* Resets the number of marks this node has received to zero. */
	protected void clearMarks() {
		numMarks = 0;
	}
	
	/* Returns true iff this node has been marked. */
	protected boolean isMarked() {
		return (numMarks > 0);
	}
	
	
	/* Returns the number of marks this node has accumulated. */
	protected int getNumMarks() {
		return numMarks;
	}

	
	/* 
	 * Assigns this node's compNumber to be the one supplied.
	 * @param number The (co-)component number to be assigned.
	 */	
	protected void setCompNumber(int number) {
		compNumber = number;
	}
	
	
	/* 
	 * Assigns this node's tree number to be the one supplied.
	 * @param number The tree number to be assigned.
	 */
	protected void setTreeNumber (int number) {
		treeNumber = number;
	}
	
	
	/* Returns the type of this node. */
	protected MDNodeType getType() {
		return type;
	}
	
	
	/* Returns the compNumber of this node. */
	protected int getCompNumber() {
		return compNumber;
	}
	
	
	/* Returns the treeNumber of this node. */
	protected int getTreeNumber() {
		return treeNumber;
	}
	
	
	/* 
	 * All nodes in the subtree rooted at this node have their
	 * 'compNumber' field assigned the given number.
	 * @param The number to be assigned.
	 */
	protected void setCompNumForSubtree(int compNumber) {
		this.compNumber = compNumber;
		MDTreeNode currentChild = (MDTreeNode) getFirstChild();
		while (currentChild != null) {
			currentChild.setCompNumForSubtree(compNumber);
			currentChild = (MDTreeNode) currentChild.getRightSibling();
		}
	}

	
	/* 
	 * All nodes in the subtree rooted at this node have their
	 * 'treeNumber' field assigned the given number.
	 * @param The number to be assigned.
	 */
	protected void setTreeNumForSubtree(int treeNumber) {
		this.treeNumber = treeNumber;
		MDTreeNode currentChild = (MDTreeNode) getFirstChild();
		while (currentChild != null) {
			currentChild.setTreeNumForSubtree(treeNumber);
			currentChild = (MDTreeNode) currentChild.getRightSibling();
		}
	}
	
	
	/* 
	 * All nodes in the subtree rooted at this node have their
	 * 'treeNumber' field reset to the default (i.e. DEF_TREE_NUM).
	 */
	protected void clearTreeNumForSubtree() {
		setTreeNumForSubtree(DEF_TREE_NUM);
	}

	
	/* 
	 * All nodes in the subtree rooted at this node have their
	 * 'compNumber' field reset to the default (i.e. DEF_COMP_NUM).
	 */	
	protected void clearCompNumForSubtree() {
		setCompNumForSubtree(DEF_COMP_NUM);
	}

	
	/* 
	 * Starting with the supplied number, incrementally numbers
	 * the nodes in the subtree rooted at this node according
	 * to the component they reside in, storing the number in the node's
	 * 'compNumber' field.  That is, if this node is
	 * labelled parallel, then the subtrees defined by its children
	 * are numbered incrementally starting with the supplied number,
	 * with all nodes in each subtree having their 'compNumber' set
	 * to the number assigned to the tree.  If this node is not labelled
	 * parallel, then all nodes in its subtree have their
	 * 'compNumber' field assigned the supplied number.
	 * @param compNumber the number at which the counting of the 
	 * components should begin.
	 * @return The number of components counted. 
	 */
	protected int numberByComp(int compNumber) {
		return numberComps(compNumber,MDNodeType.PARALLEL);
	}
	
	
	/* 
	 * The same as 'numberByComp' but instead for co-components; that is,
	 * we look instead to see if this node is labelled as series.
	 * @param compNumber the number at which the counting of the 
	 * co-components should begin.
	 * @return The number of co-components counted.  
	 */
	protected int numberByCoComp(int compNumber) {
		return numberComps(compNumber,MDNodeType.SERIES);
	}

	
	/* 
	 * Starting with the supplied number, incrementally numbers
	 * the nodes in the subtree rooted at this node according
	 * to either the component or co-component they reside in, 
	 * storing the number in the node's 'compNumber' field.  That is, 
	 * if the supplied type is parallel, and this node is labelled 
	 * parallel, then the subtrees defined by its children
	 * are numbered incrementally starting with the supplied number,
	 * with all nodes in each subtree having their 'compNumber' set
	 * to the number assigned to the tree.  If this node is not labelled
	 * parallel, then all nodes in the subtree it roots have their
	 * 'compNumber' field assigned the supplied number.
	 * Symmetrically for when the supplied type is series and this node
	 * is labelled as series.
	 * @param compNumber the number at which the counting of the 
	 * components should begin.
	 * @param byType the type according to which the number should proceed.
	 * Must be either parallel or series.
	 * @return The number of components counted. 
	 */	
	private int numberComps(int compNumber,MDNodeType byType) {
		int origCompNumber = compNumber;
		
		if (this.type == byType) {
			MDTreeNode currentChild = (MDTreeNode) getFirstChild();
			while (currentChild != null) {
				currentChild.setCompNumForSubtree(compNumber);
				currentChild = (MDTreeNode) currentChild.getRightSibling();
				compNumber++;
			}
		}
		else {
			setCompNumForSubtree(compNumber);
			compNumber++;
		}
		
		return (compNumber - origCompNumber);
	}

	
	/* Change the type of this node to the one given. */
	protected void setType(MDNodeType type) {
		this.type = type;
	}

	
	/* 
	 * Adds the given mark to all of this node's ancestors.  
	 * Precondition: if an ancestor, say 'n', of this node is marked 
	 * by 'x', then all of n's ancestors are also marked by 'x'; 
	 * similarly for the children of all prime ancestors.
	 * @param splitType the mark to be added.
	 */
	protected void markAncestorsBySplit(SplitDirection splitType) {
		
		if (this.isRoot()) { return; }
		
		MDTreeNode parent = (MDTreeNode) getParent();		
		parent.addSplitMark(splitType);		
		parent.markAncestorsBySplit(splitType);	
	}

	
	/* 
	 * Adds the given mark to all of this node's children.  
	 * @param splitType the mark to be added.
	 */
	private void markChildrenBySplit(SplitDirection splitType) {
		MDTreeNode currentChild = (MDTreeNode) getFirstChild();
		while (currentChild != null) {
			currentChild.addSplitMark(splitType);
			currentChild = (MDTreeNode) currentChild.getRightSibling();
		}		
	}

	
	/* 
	 * Adds the given mark to this node.  If the node has already
	 * been marked by this type, then nothing happens.  If the node 
	 * already has a different mark it is marked as 'mixed'.  
	 * If the node is prime, then the node's children are also 
	 * marked by the supplied type.  
	 * @param splitType the marke to be added.
	 */
	protected void addSplitMark(SplitDirection splitType) {
		
		if (this.splitType == splitType) {
			return;
		} else if (this.splitType == SplitDirection.NONE) {
			this.splitType = splitType;
		}
		else {
			this.splitType = SplitDirection.MIXED;
		}
		
		if (type == MDNodeType.PRIME) {
			markChildrenBySplit(splitType);
		}
	}

	
	/* 
	 * Returns true if and only if this node has been split 
	 * marked by the supplied type of split.
	 */
	private boolean isSplitMarked(SplitDirection splitType) {
		return (this.splitType == SplitDirection.MIXED ||
				this.splitType == splitType);
	}
	
	
	/* Returns the type of this node's split mark. */
	protected SplitDirection getSplitType() {
		return splitType;
	}
	
	
	/* 
	 * Removes the split marks assigned to all nodes in the subtree
	 * rooted at this node.
	 */
	protected void clearSplitMarksForSubtree() {
		
		splitType = SplitDirection.NONE;
		
		MDTreeNode currentChild = (MDTreeNode) getFirstChild();
		while (currentChild != null) {
			currentChild.clearSplitMarksForSubtree();
			currentChild = (MDTreeNode) currentChild.getRightSibling();
		}
	}	

	
	/* 
	 * Promotes to depth-0 all nodes in the subtree rooted at this node labelled
	 * by the supplied type.  If the split type is LEFT, then nodes are promoted to
	 * the left of their parents, and if the split type is RIGHT, nodes are promoted
	 * to the right of their parent.  If, after promoting these nodes, some are found to
	 * have no children or only a single child, then these nodes are deleted, and
	 * in the latter case, replaced by their only child.
	 * @param splitType nodes of this type are the ones to be promoted.
	 * Precondition: if node x is marked by splitType t, then all of x's ancestors
	 * are also marked by t.
	 */
	protected void promote(SplitDirection splitType) {
								
		MDTreeNode toPromote = (MDTreeNode) getFirstChild();		
		
		// Promote each child marked by the given type.
		while (toPromote != null) {			
			
			MDTreeNode nextToPromote = (MDTreeNode) toPromote.getRightSibling();
			
			if (toPromote.isSplitMarked(splitType)) {
				
				if (splitType == SplitDirection.LEFT) {
					toPromote.insertBefore(toPromote.getParent());
				}
				else {
					toPromote.insertAfter(toPromote.getParent());
				}
				
				// Recursively promote in the subtree rooted at the node just 
				// promoted.
				toPromote.promote(splitType);
			}
		
			toPromote = nextToPromote;
		}
		
		if (hasNoChildren() && !isALeaf()) { removeSubtree(); }
		else if (hasOnlyOneChild()) { replaceWith(getFirstChild()); }
	}
	
	/* This node is an internal MD tree node and so not a leaf.
	 * Meaning this method always returns false.
	 */
	protected boolean isALeaf() { return false; }

	
	/*
	 * Returns a collection representing the union of the alpha-lists
	 * of all the leaves in the subtree rooted at this node.
	 * @return The union of the alpha lists of the leaves of this node's subtree.
	 */
	protected Collection<MDTreeLeafNode> getAlpha() {		
		
		LinkedList<MDTreeLeafNode> alpha = new LinkedList<MDTreeLeafNode>();
		
		Iterator<RootedTreeNode> leafIt = getLeaves().iterator();
		while (leafIt.hasNext()) {
			alpha.addAll(((MDTreeLeafNode) leafIt.next()).getAlpha());			
		}
		
		return alpha;
	}

	
	/* 
	 * Removes consecutive degenerate nodes of the same type from
	 * the subtree rooted at this node.
	 */
	protected void removeDegenerateDuplicatesFromSubtree() {
		
		MDTreeNode currentChild = (MDTreeNode) getFirstChild();
		
		while(currentChild != null) {
		
			MDTreeNode nextChild = (MDTreeNode) currentChild.getRightSibling();
			
			currentChild.removeDegenerateDuplicatesFromSubtree();
						
			if (currentChild.getType() == type && type.isDegenerate()) {					
					addChildrenFrom(currentChild);
					currentChild.removeSubtree();
			}
			
			currentChild = nextChild;
		}				
	}

	
	/* 
	 * Resets to their defaults all properties of the nodes in the subtree
	 * rooted at this node, except its type, which remains the same.
	 */
	protected void clearAll() {
		
		compNumber = DEF_COMP_NUM;
		treeNumber = DEF_TREE_NUM;
		numMarks = 0;
		splitType = SplitDirection.NONE;
		
		MDTreeNode currentChild = (MDTreeNode) getFirstChild();
		while (currentChild != null) {
			currentChild.clearAll();
			currentChild = (MDTreeNode) currentChild.getRightSibling();
		}		
	}

	
	/*
	 * Resets to false the 'visited' field of all nodes in the subtree
	 * rooted at this node.
	 */
	protected void clearVisited() {
		// Only the leaves of this tree have a 'visited' field.
		Iterator<RootedTreeNode> vertexIt = getLeaves().iterator();		
		while (vertexIt.hasNext()) {
			((MDTreeLeafNode)vertexIt.next()).clearVisited();
		}		
	}
	
	
	/* Returns true iff this node the root of an MD tree. */	
	protected boolean isRoot() {				
		// This is a hack, I realize, but tries to cast its parent
		// to an MDTreeNode, and on failure, realizes it is a root.
		// Could just have easily used the run-time class checking
		// facility to verify the class of its parent.
		try {
			MDTreeNode parent = (MDTreeNode) getParent();
			if (parent == null) { return true; }
			else { return false; }
		}
		catch(ClassCastException e) {
			return true;
		}		
	}
	
	
	/*
	 * Reutrns a string representation of the subtree rooted at this node.
	 * The representation is enclosed in brackets inside of which is listed the
	 * root's type, the number of its children, and then the representation for
	 * each of its children in order.
	 * @return The string representation of the subtree rooted at this node.
	 */	
	public String toString() {
		
		String result ="(" ;
		if(isRoot())
			result += "ROOT ";
		result += type + ", numChildren=" + getNumChildren();
		
		RootedTreeNode current = getFirstChild();
		if (current != null) { 
			result += current; 
			current  = current.getRightSibling(); 
		}
		while (current != null) {
			result += ", " + current;
			current = current.getRightSibling();
		}
		return result + ")";
	}

	// F.L. 22.11.17: deal with error in Adrains Code
	// F.L. 13.12.17: and with weak order modules
	boolean removeDummies() {
		RootedTreeNode currentChild = getFirstChild();
		boolean ret = false;
		while (currentChild != null) {
			MDTreeNode current = (MDTreeNode) currentChild;
			if (current.removeDummies()) {
				ret = true;
			}
			MDNodeType currentType = current.getType();
			currentChild = currentChild.getRightSibling(); // now nextChild

			if (currentType == MDNodeType.PRIME && current.getNumChildren() == 1
					|| currentType == MDNodeType.ORDER && !isRoot() && type == MDNodeType.ORDER) {
				currentChild.insertBefore(current.getFirstChild()); // takes the subtree with it
				ret = true;
				current.removeSubtree(); // shouldn't have anything now, just remove it
			}
		}
		return ret;
	}

	// F.L. 21.12.17: For Editing.
	void getPrimeModules(Map<Integer,LinkedList<MDTreeNode>> depthToNodes, int currDepth){

		if(!isALeaf() && MDNodeType.PRIME == type){
			depthToNodes.putIfAbsent(currDepth, new LinkedList<>());
			depthToNodes.get(currDepth).add(this);
		}

		MDTreeNode rightSibling = (MDTreeNode) getRightSibling();
		if(rightSibling != null) {
			rightSibling.getPrimeModules(depthToNodes, currDepth);
		}

		if(!isALeaf()) {
			currDepth++;
			MDTreeNode firstChild = (MDTreeNode) getFirstChild();
			firstChild.getPrimeModules(depthToNodes, currDepth);
		}
	}

	// F.L.: inits Graph, baseVnoToSubVno and returns the weights of each new vertex.
	public double[] initWeigthsAndSubgraph(PrimeSubgraph subGraph, SimpleDirectedGraph<Integer,DefaultEdge> base){

		HashMap<Integer,Integer> baseVNoTosubVNo = subGraph.getBaseNoTosubNo();

		MDTreeNode currChild = (MDTreeNode) getFirstChild();
		double[] subVertexToWeight = new double[getNumChildren()];

		int subIndex = 0;
		// add Vertices
		while (currChild != null) {
			double edgeWeight = 1;
			int vertexNo;
			if (currChild.isALeaf())
				vertexNo = ((MDTreeLeafNode) currChild).vertexNo;
			else {
				vertexNo = currChild.vertices.nextSetBit(0);
				edgeWeight = currChild.vertices.cardinality(); // module preserving: every vertex!!!
			}
			baseVNoTosubVNo.put(vertexNo,subIndex);
			subGraph.addVertex(subIndex);
			subVertexToWeight[subIndex] = edgeWeight;
			currChild = (MDTreeNode) currChild.getRightSibling();
			subIndex++;
		}

		// add edges. Cost of editing is the product, as this many edges would have to be edited.
		// is it smarter to check all Vertices or all edges? Hmm, not if the modules is large...
		for (Map.Entry<Integer,Integer> source : baseVNoTosubVNo.entrySet()) {
			for (DefaultEdge outEdge : base.outgoingEdgesOf(source.getKey())) {
				int target = base.getEdgeTarget(outEdge);

				if (baseVNoTosubVNo.containsKey(target)) {
					int subSourceV = source.getValue();
					int subTargetV = baseVNoTosubVNo.get(target);
					subGraph.addEdge(subSourceV,subTargetV);
				}
			}
		}
		return  subVertexToWeight;
	}

	// F.L. 26.12.17: For Heuristic
	public int getNumPrimeChildren(){
		int count = 0;
		if(type == MDNodeType.PRIME){
			count = getNumChildren();
		}
		RootedTreeNode currChild = getFirstChild();
		while (currChild != null){
			if(!currChild.isALeaf()){
				count += ((MDTreeNode) currChild).getNumPrimeChildren();
			}
			currChild = currChild.getRightSibling();
		}
		return count;
	}

	public List<Edge> addModuleEdges(int u, int v) {
		List<Edge> ret = new LinkedList<>();
		BitSet fromU = null;
		BitSet fromV = null;

		// get the module vertices
		RootedTreeNode currChild = getFirstChild();
		while (currChild != null){
			if(!currChild.isALeaf()){
				MDTreeNode node = (MDTreeNode) currChild;
				if(node.vertices.get(u))
					fromU = (BitSet) node.vertices.clone();
				else if(node.vertices.get(v))
					fromV = (BitSet) node.vertices.clone();
			}
			currChild = currChild.getRightSibling();
		}

		if(fromU == null && fromV == null){
			throw new IllegalStateException("Error: edge (" + u + "," + v + ") has weight, but no module found!");
		}
		// one leaf is fine
		if(fromU == null) {
			fromU = new BitSet();
			fromU.set(u);
		} else if(fromV == null){
			fromV = new BitSet();
			fromV.set(v);
		}

		// all in same direction as (u,v)
		for (int i = fromU.nextSetBit(0); i >= 0; i = fromU.nextSetBit(i+1)) {
			final int sourceVertex = i;
			fromV.stream().forEach( destVertex -> {
				if(!(sourceVertex == u && destVertex == v))
					ret.add( new Edge(sourceVertex,destVertex));
			});
		}

		return ret;
	}

}


