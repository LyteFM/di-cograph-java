package dicograph.modDecomp;

import java.util.Iterator;
import java.util.LinkedList;

/**
 *   This source file is part of the program for computing the modular
 *   decomposition of undirected graphs.
 *   Copyright (C) 2010 Marc Tedder
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
 * An element in a factorizing permutation.
 */
class FactPermElement extends RootedTreeNode {

	// The index of this element within the factorizing permutation.
	private int index;
	// The default index if none applies.
	protected static final int DEFAULT_INDEX = -1;
	
	// The list of neighbours of this element.
	private LinkedList<FactPermElement> neighbours;
	
	// The mu value for this element, based on its neighbours.  See the
	// paper for details on its definition.
	private FactPermElement mu;
	
	// Is this element part of a component, another part of which appears to its right.
	private boolean hasRightCompFragment;
	
	// Is this element part of a co-component, another part of which appears to its left. 
	private boolean hasLeftCoCompFragment;
	
	// Does this element have an edge to a layer/subproblem to its right.
	private boolean hasRightLayerNeighbour;
	
	// The number of marks this element has accumulated in the construction
	// of the MD tree it is facilitating.
	int numMarks;
	
	
	/* The default constructor. */
	protected FactPermElement() {
		super();
		index = DEFAULT_INDEX;
		mu = null;
		hasRightCompFragment = false;
		hasLeftCoCompFragment = false;
		hasRightLayerNeighbour = false;
		numMarks = 0;
		neighbours = new LinkedList<FactPermElement>();
	}
	
	
	/* 
	 * Creates a factorizing permutation element with the given index.
	 * @param index The index to be assigned to this element.
	 */
	protected FactPermElement(int index) {
		this();
		this.index = index; 
	}
	
	
	/* Adds another mark to this element. */
	protected void addMark() {
		numMarks++;	
	}
	
	
	/* Returns true iff this element has been marked. */
	protected boolean isMarked() {
		return numMarks > 0;
	}
	
	
	/* Returns the number of marks accumulated by this element. */
	protected int getNumMarks() {
		return numMarks;		
	}
	
	
	/* Resets the number of marks of this element to zero. */
	protected void clearMarks() {
		numMarks = 0;
	}
	
	
	/* 
	 * Adds the given element as a neighbour of this element.
	 * @param neighbour The neighbour to be added.
	 */
	protected void addNeighbour(FactPermElement neighbour) {
		neighbours.add(neighbour);
	}
	
	
	/*
	 * Replaces this element's neighbours with the supplied list.
	 * @param newNeighbours The neighbours replacing this node's current neighbours.
	 */
	protected void replaceNeighbours(LinkedList<FactPermElement> newNeighbours) {
		neighbours = newNeighbours;
	}
	
	
	/* Returns this element's neighbours. */
	protected LinkedList<FactPermElement> getNeighbours() {
		return neighbours;
	}
	
	
	/* Sets this element's mu value. */
	protected void setMu(FactPermElement mu) {
		this.mu = mu; 		
	}

	
	/* Returns this element's mu value. */
	protected FactPermElement getMu() {
		return mu;
	}

	
	/* Sets this element's 'hasRightCompFragment' flag to 'true'. */
	protected void setRightCompFragment() {
		hasRightCompFragment = true;		
	}


	/* Returns this element's 'hasRightCompFragment' flag. */
	protected boolean hasRightCompFragment() {
		return hasRightCompFragment;
	}

	
	/* Sets this element's 'hasLeftCoCompFragment' flag to 'true'. */	
	protected void setLeftCoCompFragment() {
		hasLeftCoCompFragment = true;
	}

	
	/* Returns this element's 'hasLeftCoCompFragment' flag. */
	protected boolean hasLeftCoCompFragment() {
		return hasLeftCoCompFragment;
	}

	
	/* Returns this element's index. */
	protected int getIndex() {
		return index;
	}

	
	/* 
	 * Returns true iff this element has an edge to an element in a
	 * layer/subproblem to its right.
	 */
	protected boolean hasRightLayerNeighbour() {
		return hasRightLayerNeighbour;
	}
	
	
	/* Sets this elements 'setHasRightLayerNeighbour' flag to 'true'. */
	protected void setHasRightLayerNeighbour() {
		hasRightLayerNeighbour = true;		
	}
	
	
	/* 
	 * Returns a string representation of the given element consisting of the element's
	 * index, followed by its neighbours, and then the subtree it represents.
	 * @return A string representation of this object.
	 */
	public String toString() {
		String result = "|";
		if (index != DEFAULT_INDEX) {
			result = "{Index=" + index + " neighbours=";
			
			Iterator<FactPermElement> neighIt = neighbours.iterator();
			if (neighIt.hasNext()) { result += (neighIt.next()).getIndex(); }	
			while (neighIt.hasNext()) {
				result += ", " + (neighIt.next()).getIndex();
			}			
		}
		return result;
	}	
}
