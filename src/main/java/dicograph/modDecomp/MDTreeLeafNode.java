package dicograph.modDecomp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

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
 * A leaf node in a modular decomposition tree.
 */
class MDTreeLeafNode extends MDTreeNode {

	// F.L. 2017
	int vertexNo;


	// The vertex to which this leaf node is in one-to-one correspondance.
	private Vertex vertex;

    // The leaf node representation of the neighbours of the vertex
	// associated with this leaf node.
	private LinkedList<MDTreeLeafNode> neighbours;

	// The neighbours of this leaf node in different subproblems than the one
	// this node is contained in.
	private LinkedList<MDTreeLeafNode> alpha;
	
	// Has this leaf node been used as a pivot.
	private boolean visited;


	

	/* The default constructor. */
	protected MDTreeLeafNode() {
		super();
		alpha = new LinkedList<>();
		neighbours = new LinkedList<>();
		visited = false;
		vertex = null;
	}
	
	
	/* 
	 * Creates a leaf node in one-to-one correpsondance with the given
	 * vertex.  Does not assume any neighbours of the vertex.
	 * @param vertex The vertex to be associated with this leaf node.
	 */
	protected MDTreeLeafNode(Vertex vertex) {
		this();
		this.vertex = vertex;		
	}

	// F.L. 2017: use int instead of Vertex
	protected MDTreeLeafNode(int vertexNo){
		this();
		this.vertexNo = vertexNo;
	}
	
	
	/* Returns true iff this node has been used as a pivot. */
	protected boolean isVisited() {
		return visited;
	}

	
	/*
	 * Adds the supplied leaf to the alpha-list of this node.
	 * @param leaf The leaf to be added to the alpha list.
	 */
	protected void addToAlpha(MDTreeLeafNode leaf) {
		alpha.add(leaf);		
	}
	
	
	/*
	 * Adds the given set of leaf nodes to this node's alpha-list.
	 * @param leaves The leaves to be added to this node's alpha-list.
	 */
	protected void addToAlpha(Collection<MDTreeLeafNode> leaves) {
		alpha.addAll(leaves);
	}

	
	/* Sets this node as having been used as a pivot. */
	protected void setVisited() {
		visited = true;		
	}

	
	/* Returns this node's alpha list. */
	protected Collection<MDTreeLeafNode> getAlpha() {
		return alpha;
	}
	
	
	/* Resets this node's alpha list to the empty-list. */ 
	protected void clearAlpha() {
		alpha = new LinkedList<MDTreeLeafNode>();
	}
	
	
	/*
	 * Adds the supplied leaf node as a neighbour of this node.
	 * @param neighbour The neighbour to be added.
	 */
	protected void addNeighbour(MDTreeLeafNode neighbour) {
		neighbours.add(neighbour);
	}		
	
	
	/* Returns this node's neighbours. */
	protected Collection<MDTreeLeafNode> getNeighbours() {
		return neighbours;
	}


	/*
	 * This node is a leaf of an MD tree and so this method always returns true.
	 */
	protected boolean isALeaf() { return true; }

	
	/*
	 * Replaces this node's alpha list with the one supplied.
	 * @param newAlpha What this node's alpha list is to become.
	 */
	protected void replaceAlpha(LinkedList<MDTreeLeafNode> newAlpha) {
		alpha = newAlpha;		
	}
	
	
	/* Returns the vertex associated with this leaf. */
	protected Vertex getVertex() {
		return vertex;
	}

	
	/* 
	 * Resets to their defaults all properties of this node, except its
	 * 'visited' field, which remains the same.
	 */	
	protected void clearAll() {
		super.clearAll();
		alpha = new LinkedList<MDTreeLeafNode>();
	}
	
	
	/* Resets this node to not having been used as a pivot. */
	protected void clearVisited() {
		visited = false;
	}
	
	/*
	 * Returns a string representation of this node.  Merely uses the string
	 * representation of the vertex with which it is associated.
	 */
	public String toString() {
	    if(vertex != null)
		    return String.format("(no= %s)", vertex.getLabel());
	    else{
	    	// F.L. 2017
//            StringBuilder result = new StringBuilder("(no= " + vertexNo);

//            // unnecessary overhead.
//            result.append(", neighbours: ");
//            boolean firstRun = true;
//
//            for(MDTreeLeafNode name : neighbourNames){
//                if(!firstRun) {
//                    result.append(",");
//                }
//                result.append(name);
//                firstRun = false;
//            }

//            result.append(")");

//            return result.toString();
            return (String.format("(no= %s)", vertexNo));
        }
	}
	// F.L. 2017
    public int getVertexNo() {
        return vertexNo;
    }

    // F.L. 2017
    @Override
	int exportAsDot(StringBuilder output, int[] counter){
	    counter[0]++;
		int myCounter = counter[0];
		output.append(myCounter).append("[label=").append(vertexNo).append("];\n");
		return myCounter;
	}

	// F.L. 2017
	@Override
    public String getSetRepresentation(ArrayList<String> bla) {
        return Integer.toString(vertexNo);
    }
}
