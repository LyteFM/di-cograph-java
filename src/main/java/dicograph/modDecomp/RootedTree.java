package dicograph.modDecomp;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


import dicograph.graphIO.DirectedInducedIntSubgraph;
import dicograph.graphIO.UndirectedInducedIntSubgraph;

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

    /** F.L. 16.10.17:
     * Returns a String representation of the strong members of the tree's set family, i.e. the inner nodes.
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


            Graph<Integer, DefaultEdge> subgraph;
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

    // F.L. 27.01.18: KISS! Incoming list will be changed.
    public RootedTreeNode getLCA(List<RootedTreeNode> nodes){

        if(nodes.size() == 1)
            return nodes.get(0);
        else if(nodes.size() == 2)
            return getLCA(nodes.get(0),nodes.get(1));
        else{
            RootedTreeNode lca = getLCA(nodes.get(0),nodes.get(1));
            if(lca.equals(root))
                return root;
            nodes.remove(0);
            nodes.remove(0);
            nodes.add(lca);
            return getLCA( nodes );
        }
    }

    // F.L. 27.01.18: KISS!
    private RootedTreeNode getLCA(RootedTreeNode x, RootedTreeNode y){
        // trivial cases
        if(x.equals(root) || y.equals(root))
            return root;
        if(x.equals(y))
            return x;

        LinkedList<RootedTreeNode> xList = new LinkedList<>();
        RootedTreeNode current = x;
        while (current != null){
            xList.add(0,current);
            current = current.getParent(); // until root
            if(y.equals(current))
                return y; // y is ancestor of x
        }

        LinkedList<RootedTreeNode> yList = new LinkedList<>();
        current = y;
        while (current != null){
            yList.add(0,current);
            current = current.getParent();
            if(x.equals(current))
                return x; // x is ancestor of y
        }

        // run list top-down to find lca
        RootedTreeNode previous = root;
        while (true){
            if(xList.isEmpty() || yList.isEmpty())
                throw new IllegalStateException("Empty lists in LCA search!");
            RootedTreeNode nextX = xList.pop();
            RootedTreeNode nextY = yList.pop();
            if(!nextX.equals(nextY))
                return previous;
            else
                previous = nextX;
        }
    }

}

