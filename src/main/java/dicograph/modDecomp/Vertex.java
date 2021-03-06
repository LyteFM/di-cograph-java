package dicograph.modDecomp;

import java.util.Collection;

import java.util.LinkedList;
import java.util.ListIterator;
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
 * A vertex in a simple, undirected graph.
 */
public class Vertex {
	
	// The label of the vertex.
	private String label;
	// The default label assigned to vertices.
	private static final String DEFAULT_LABEL = "|";
	
	// The vertex's neighbours.
	private LinkedList<Vertex> neighbours;

	
	/* The default constructor. */
	public Vertex() {
		label = DEFAULT_LABEL;
		neighbours = new LinkedList<Vertex>();
	}
	
	
	/* Creates a string with the given string label, but with no neighbours. */
	public Vertex(String vertexLabel) {
		this();
		label = vertexLabel;
	}
	
	
	/* Creates a string with the given integer label, but with no neighbours. */
	public Vertex(int vertexLabel) {
		this();
		Integer wrapper = new Integer(vertexLabel);
		label = wrapper.toString();
	}

	
	/* Returns this vertex's label. */
	public String getLabel() {
		return label;
	}

	
	/* 
	 * Adds the given vertex as a neighbour of the given vertex.
	 * @param vertex The neighbour to be added.
	 */
	public void addNeighbour(Vertex vertex) {
		neighbours.add(vertex);		
	}
	
	
	/* Returns this vertex's collection of neighbours. */
	public Collection<Vertex> getNeighbours() {
		return neighbours;
	}
	
	
	/* 
	 * Returns a string representation of this vertex.  Enclosed in brackets 
	 * is the vertex's label, followed by a list of its neighbours.
	 */
	public String toString() {		
		
		String result = "(label= " + label;
		
		result += ", neighbours: ";
		
		ListIterator<Vertex> neighboursIt = neighbours.listIterator();
		
		if (neighboursIt.hasNext()) { result += neighboursIt.next().label; }		
		while(neighboursIt.hasNext()) {
			result += "," + neighboursIt.next().getLabel();		
		}
		
		return result + ")";
	}	
}
