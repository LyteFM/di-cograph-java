package dicograph.modDecomp;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import dicograph.utils.SortAndCompare;

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
 * A node in a rooted tree.
 */
public class RootedTreeNode {


	// F.L. 16.11.17: moved here
	protected BitSet vertices;


	// F.L. 30.10. Flags for directedMD, should go there.
    boolean marked;
    int numMarkedChildren;
    int nodeNumber;
    // end

	// The parent of this node.
	private RootedTreeNode parent;
	
	// The first child of this node.
	private RootedTreeNode firstChild;
	
	// This node's sibling to its left.
	private RootedTreeNode leftSibling;
	
	// This node's sibling to its right.
	private RootedTreeNode rightSibling;
	
	// The number of this node's children.
	private int numChildren;

	
	/* The default constructor. */
	protected RootedTreeNode() {
		parent = null;
		firstChild = null;
		leftSibling = null;
		rightSibling = null;
		numChildren = 0;

        // F.L. 30.10. Flags for Algorithm 1:
        // actually used for MDTree/Leafnodes.
        marked = false;
        numMarkedChildren = 0;
        nodeNumber = 0;
        // end
        vertices = null;

    }

    // F.L. 30.10. Flags for Algorithm 1:
    public int getNumMarkedChildren() {
        return numMarkedChildren;
    }

    protected void mark() {
        marked = true;
        // shouldn't be root...
		RootedTreeNode parent = getParent();
		parent.numMarkedChildren++;
    }

    protected void unmark() {
		// only unmark marked nodes...
		if (marked) {
			marked = false;
			RootedTreeNode parent = getParent();
			parent.numMarkedChildren--;
		}
	}

    void unmarkAllChildren() {
		RootedTreeNode currentChild = this.getFirstChild();

        while (currentChild != null) {
            currentChild.unmark();
			currentChild = currentChild.getRightSibling();
		}
        if (numMarkedChildren != 0)
            System.err.println("Illegal number of marked children after unmarking!!!");
    }
    // end F.L.

    /*
     * Creates a node with a single child.
     * @param child The child of this node.
     */
	protected void addChild(RootedTreeNode child) {
		child.removeSubtree();
		if (firstChild != null) {
			firstChild.leftSibling = child;
			child.rightSibling = firstChild;
		}
		
		firstChild = child;
		child.parent = this;
		numChildren++;
	}
	
	/* Returns the number of this node's children. */
	public int getNumChildren() {
		return numChildren;
	}
	
	
	/* Returns true iff this node has no children. */
	protected boolean hasNoChildren() {
		return (numChildren == 0);
	}


	/* Returns true iff this node has a single child. */
	protected boolean hasOnlyOneChild() {
		return (numChildren == 1);
	}
	
	/* 
	 * Replaces this node in its tree with the supplied node.  This node's
	 * takes its children with it when it is removed.  That is, the supplied
	 * node does not assume its children.  Thus, this method might be better
	 * referred to as 'replaceSubtreeWith'.
	 * @param replacement The node to replace this one in the tree.
	 */
	protected void replaceWith(RootedTreeNode replacement) {
		replacement.removeSubtree();
		replacement.leftSibling = leftSibling;
		replacement.rightSibling = rightSibling;
		if (leftSibling != null) {leftSibling.rightSibling = replacement; }
		if (rightSibling != null) {rightSibling.leftSibling = replacement; }
		replacement.parent = parent;
		if (parent != null && parent.firstChild == this) { 
			parent.firstChild = replacement; 
		}
		parent = null;
		leftSibling = null;
		rightSibling = null;		
	}
	
	/* 
	 * Removes this node from its tree.  The node takes its children with it
	 * as it is removed.
	 */
	protected void removeSubtree() {
		if (parent != null) { parent.numChildren--; }
		if (leftSibling != null) { leftSibling.rightSibling = rightSibling; }
		if (rightSibling != null) { rightSibling.leftSibling = leftSibling; }
		if (parent != null && parent.firstChild == this) { 
			parent.firstChild = rightSibling; 
		}
		parent = null;
		leftSibling = null;
		rightSibling = null;
	}
	
	
	/*
	 * Insert supplied node as the left sibling of this node.
	 * @param justBefore The node to be made this node's left sibling.
	 */
	protected void insertBefore(RootedTreeNode justBefore) {
		removeSubtree();
		leftSibling = justBefore.leftSibling;
		if (justBefore.leftSibling != null) { 
			justBefore.leftSibling.rightSibling = this; 
		}
		
		rightSibling = justBefore;
		justBefore.leftSibling = this;
		
		parent = justBefore.parent;
		
		if (justBefore.parent != null) {
			justBefore.parent.numChildren++;
		}
		
		if (justBefore.parent != null && 
				justBefore.parent.firstChild == justBefore) { 
			parent.firstChild = this; 
		}
		
				
	}
	
	
	/*
	 * Insert supplied node as the right sibling of this node.
	 * @param justAfter The node to be made this node's right sibling.
	 */
	protected void insertAfter(RootedTreeNode justAfter) {
		removeSubtree();
		
		rightSibling = justAfter.rightSibling;
		if (justAfter.rightSibling != null) {
			justAfter.rightSibling.leftSibling = this;
		}
		
		leftSibling = justAfter;
		justAfter.rightSibling = this;
		
		parent = justAfter.parent;
		
		if (justAfter.parent != null) {
			justAfter.parent.numChildren++;
		}
	}
	
	
	/*
	 * Moves this node to be the first amongst all its siblings, and thus
	 * its parent's first child.
	 */
	protected void makeFirstChild() {
		
		if (parent.firstChild == this) { return; }
		
		RootedTreeNode newRightSibling = parent.firstChild;
		removeSubtree();
		insertBefore(newRightSibling);		
	}

	
	/* Returns the parent of this node. */
	protected RootedTreeNode getParent() {
		return parent;
	}


	/* Returns the first child of this node. */
	protected RootedTreeNode getFirstChild() {
		return firstChild;
	}
	

	/* Returns the left sibling of this node. */
	protected RootedTreeNode getLeftSibling() {
		return leftSibling;
	}


	/* Returns the right sibling of this node. */
	protected RootedTreeNode getRightSibling() {
		return rightSibling;
	}

	
	/* Returns a collection of the leaves of the subtree rooted at this node. */
	protected Collection<RootedTreeNode> getLeaves() {
		
		LinkedList<RootedTreeNode> leaves = new LinkedList<RootedTreeNode>();
		
		if (isALeaf()) { leaves.add(this); }
		else {
			RootedTreeNode currentChild = firstChild;
			while(currentChild != null) {
				leaves.addAll(currentChild.getLeaves());
				currentChild = currentChild.rightSibling;
			}
		}
		return leaves;
	}

	
	/* Returns true iff this node is a leaf, i.e. it has no children. */
	protected boolean isALeaf() {
		return hasNoChildren();
	}
	
	
	/*
	 * Adds as children the children from the supplied node.
	 * @param from The node whose children are to be added to this node's children.
	 */
	protected void addChildrenFrom(RootedTreeNode from) {
		RootedTreeNode currentChild = from.firstChild;
		while (currentChild != null) {
			RootedTreeNode nextChild = currentChild.rightSibling;
			addChild(currentChild);
			currentChild = nextChild;
		}
	}
	
	
	/* 
	 * Replaces this node in its subtree by its children.  That is, the children
	 * of this node are made to become children of this node's parent, and this
	 * node is removed from its tree.
	 */
	protected void replaceThisByItsChildren() {

		// todo: is this the error source???
		// System.out.println("Warning: replacing " + this + " by its children!");
		RootedTreeNode currentChild = getFirstChild();
		while (currentChild != null) {
			RootedTreeNode nextChild = currentChild.getRightSibling();
			currentChild.insertBefore(this);
			currentChild = nextChild;
		}
		this.removeSubtree();
	}


	/*
	 * Replaces the children of this node with the node supplied.  The
	 * children are removed from this node's tree.
	 * @param replacement This node's new child.
	 */
	protected void replaceChildrenWith(RootedTreeNode replacement) {
		RootedTreeNode currentChild = getFirstChild();
		while (currentChild != null) {
			RootedTreeNode nextChild = currentChild.getRightSibling();
			currentChild.removeSubtree();
			currentChild = nextChild;
		}
		addChild(replacement);
	}

	
	/* Returns true iff this node is the root of a tree. */ 
	protected boolean isRoot() {
		return (parent == null);
	}
	
	
	/*
	 * Returns a string representation of the subtree rooted at this node.
	 * The subtree is enclosed in brackets, the number of this node's children
	 * is supplied, followed by the representations of its children's subtrees
	 * in order.
	 * @return The string representation of the subtree rooted at this node.
	 */
	public String toString() {
		String result = "(numChildren=" + numChildren + " ";
		RootedTreeNode currentChild = firstChild;
		if (currentChild != null) { 
			result += currentChild;
			currentChild = currentChild.rightSibling;
		}
		while (currentChild != null) {
			result += ", " + currentChild;
			currentChild = currentChild.rightSibling;
		}
		return result + ")";
	}

    /**
     * F.L. 2017 just for display
	 * This is the desired format. Separating members my " " and using -1 to end a module.
     * 2 3 4 5 6 7 -1
     * 1 8 9 10 11 -1
     * 4 5 6 7 -1
     * 1 2 3 -1
     * 0 1 -1
     * 3 4 -1
     * 5 6 -1
     * 1 7 -1
     * 9 10 -1
     * 10 11 -1
     * 0 8 -1
     * @return
     */
	public String getSetRepresentation(ArrayList<String> strongModules){

	    String thisNodesMembers = "";
        RootedTreeNode currentChild = firstChild;
        if(currentChild != null){
            thisNodesMembers = currentChild.getSetRepresentation(strongModules);
            currentChild = currentChild.rightSibling;

            while (currentChild != null){
                    thisNodesMembers += " "+ currentChild.getSetRepresentation(strongModules);
                    currentChild = currentChild.rightSibling;
            }
            //ret += "-1\n";
            if(!isRoot()) {
				strongModules.add(thisNodesMembers + " -1");
			}
        }

	    return thisNodesMembers;
    }

    // F.L. 2017
     int exportAsDot(StringBuilder output, int[] counter){

         // first action: increment the counter, used for this
         counter[0] ++;
         // assign the number to this node
         int myNumber = counter[0];

         String label;
         if(this instanceof MDTreeNode){
             label = ((MDTreeNode) this).getType().toString();
         } else if(this instanceof PartitiveFamilyTreeNode){
             label = ((PartitiveFamilyTreeNode)this).getType().toString();
         } else {
             label = vertices.toString();
         }

         output.append(myNumber).append("[label=").append(label).append("];\n");

         RootedTreeNode currentChild = firstChild;
         while (currentChild != null){
             // add edge from this to no of child
             // increment happens as first step

             int childNo = currentChild.exportAsDot(output, counter);
             output.append(myNumber).append("->").append(childNo).append( ";\n");
             currentChild = currentChild.rightSibling;
         }

         return myNumber;

	}

    void setParent(RootedTreeNode parent) {
        this.parent = parent;
    }



    // F.L. 24.11.17:
	RootedTreeNode removeThis(){

     	// default: insert before right sibling
     	RootedTreeNode stillRightSibling = rightSibling;
     	// if node is rightmost node: need to insert after left sibling.
		RootedTreeNode fallBackLeftSibling = leftSibling;

		RootedTreeNode myParent = parent;
     	RootedTreeNode currentChild = firstChild;
     	RootedTreeNode nextChild;
     	while (currentChild != null){
     		nextChild = currentChild.rightSibling;
     		if(stillRightSibling != null) {
				currentChild.insertBefore(stillRightSibling);
			} else {
     			currentChild.insertAfter(fallBackLeftSibling);
			}
     		currentChild = nextChild;
		}
		removeSubtree();
     	return myParent;
	}

	// F.L. 2017
	void verifyModuleStatus(StringBuilder res, Graph<Integer, DefaultEdge> graph){
		ArrayList<Integer> moduleVertices = new ArrayList<>(vertices.cardinality());
		vertices.stream().forEach(moduleVertices::add);
		res.append(SortAndCompare.checkModuleBruteForce(graph,moduleVertices, true));
	}



	// F.L. 27.11.17: trying to fix the directed MD
	/* Returns a List of leaves directly attached to this node. */
	protected List<RootedTreeNode> getDirectLeaves() {

		LinkedList<RootedTreeNode> leaves = new LinkedList<>();

		if (isALeaf()) {
			leaves.add(this);
		}
		else {
			RootedTreeNode currentChild = firstChild;
			while (currentChild != null) {
				if (currentChild.isALeaf()) {
					leaves.add(currentChild);
				}
				currentChild = currentChild.rightSibling;
			}
		}

		return leaves;
	}

	public BitSet getVertices() {
		return vertices;
	}
}
