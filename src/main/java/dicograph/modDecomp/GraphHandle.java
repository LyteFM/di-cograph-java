package dicograph.modDecomp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;

import org.jgrapht.graph.*;

/**
 *   This source file is part of the program for computing the modular
 *   decomposition of undirected graphs. Adapted to work with JGraphT.
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
 * A simple, undirected graph.
 */
public class GraphHandle {

	// F.L. new: Use SimpleGraph from JGraphT for the base Graph.
    public SimpleGraph <Integer, DefaultEdge> graph;


	
	// The delimiter separating vertices from their list of neighbours in
	// the input file format for graphs.
	private static final String VERTEX_DELIM = "->";
	
	// The delimiter separating a vertex's neighbours from one another in 
	// the input file format for graphs.
	private static final String NEIGH_DELIM = ",";
	
	// The graph's vertices, keyed by their label.
	private Hashtable<String,Vertex> vertices;
	
	
	/* 
	 * Constructs a graph from the file whose name is that specified.  The 
	 * format of the file must be as follows:
	 * - each line in the file specifies a vertex;
	 * - each vertex is specified by its label, followed by VERTEX_DELIM, 
	 * followed by a list of its neighbours separated by NEIGH_DELIM (without
	 * spaces between these tokens);
	 * - the file must correctly specify a graph in that each vertex appearing
	 * as a neighbour is specified by a line in the file, and the neighbourhood
	 * lists are symmetric in that an entry for x as a neighbour of y implies 
	 * an entry for y as a neighbour of x.
	 * @param file The name of the input file specifying the graph.
	 */
	public GraphHandle(String file) {
        graph = new SimpleGraph<>(DefaultEdge.class);
	    vertices = buildFromFile(file);
	}

	// F.L.
	public GraphHandle(SimpleGraph<Integer,DefaultEdge> graph){
	    this.graph = graph;
    }

	/*
	 * Does the work of reading the file and populating the graph with 
	 * vertices according to the contents of the file.  See 'Graph(String )'
	 * for the required input file format.
	 * @param file The name of the input file specifying the graph. 
	 */
	private Hashtable<String,Vertex> buildFromFile(String file) {
		
		Hashtable<String,Vertex> vertices = new Hashtable<String,Vertex>();
		
		BufferedReader inputStream = null;
		
        try {
            inputStream = 
                new BufferedReader(new FileReader(file));

            String line;
            while ((line = inputStream.readLine()) != null) {
                            	
                // Determine the current vertex's label.
            	String[] vertexAndNeighbours = line.split(VERTEX_DELIM);
                String vertexLabel = vertexAndNeighbours[0];
                int vertexNo = Integer.valueOf(vertexLabel);
                
                // Determine the current vertex's neighbours.
                String[] neighbourLabels = vertexAndNeighbours[1].split(NEIGH_DELIM);
                   
                
                // Create this vertex if it hasn't already been created (from
                // appearing as a neighbour of an earlier vertex).
                Vertex vertex;
                if (vertices.containsKey(vertexLabel)) {
                	vertex = vertices.get(vertexLabel);
                }
                else {
                	vertex = new Vertex(vertexLabel);
                	vertices.put(vertexLabel,vertex);
                }

                // F.L. New: use JGraphT
                if(!graph.containsVertex(vertexNo)){
                    graph.addVertex(vertexNo);
                }
                //
                       
                // Create vertices for each of its neighbours (if they haven't already
                // been created) and add them as neigbhours of this vertex.
				for (String neighbourLabel : neighbourLabels) {

					if (vertices.containsKey(neighbourLabel)) {
						vertex.addNeighbour(vertices.get(neighbourLabel));
					} else {
						Vertex unseenNeighbour = new Vertex(neighbourLabel);
						vertices.put(neighbourLabel, unseenNeighbour);
						vertex.addNeighbour(unseenNeighbour);
					}

					// F.L.: use JGraphT
					int neighbourNo = Integer.valueOf(neighbourLabel);
					if (!graph.containsVertex(neighbourNo)) {
						// if not present, create Vertex for neighbor
						graph.addVertex(Integer.valueOf(neighbourLabel));
					}
					if (!graph.containsEdge(vertexNo, neighbourNo)) {
						// if not present, add Edge.
						graph.addEdge(vertexNo, neighbourNo);
					}
					//

				}
            }
        } 
        catch (IOException e) {
        	System.out.println(e);
        }        		
                        
        if (inputStream != null) {
        	try {
        		inputStream.close();
        	}
        	catch (IOException e) {
        		System.out.println(e);
        	}
        }
        
        return vertices;
	}
	

	/* Returns this graph's vertices. */
	public Collection<Vertex> getVertices() {
		return vertices.values();	
	}

	
	/* Returns the number of this graph's vertices. */
	public int getNumVertices() {		
		return vertices.values().size();
	}

	
	/* Returns the modular decomposition tree for this graph. */
	@Deprecated
	public MDTree getMDTreeOld() {
		return new MDTree(this);
	}

	public MDTree getMDTree(){
		return new MDTree(graph);
	}
	
	
	/* 
	 * Returns a string representation of this graph.  The representation
	 * is a list of the graph's vertices.
	 * @return A string representation of the graph.
	 */
	public String toString() {
		return vertices.values().toString();
	}

	// F.L.
	public SimpleGraph<Integer, DefaultEdge> getGraph() {
		return graph;
	}
}
