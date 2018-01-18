package dicograph.graphIO;

import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;

import java.util.Collection;

/*
 *   This source file is part of the program for editing directed graphs
 *   into cographs using modular decomposition.
 *   Copyright (C) 2018 Fynn Leitow
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

public class UndirectedInducedIntSubgraph<E> extends SimpleGraph<Integer,E> {

    private final SimpleGraph<Integer, E> base;

    public UndirectedInducedIntSubgraph(Graph<Integer, E> baseGraph, Collection<Integer> vertices){
        super(baseGraph.getEdgeFactory());
        base = (SimpleGraph<Integer, E>) baseGraph;

        // adds the vertices
        vertices.forEach( this::addVertex );

        // adds edges to vertices that are contained in this subgraph
        for( int vertex : vertexSet()){
            for(E outEdge : baseGraph.edgesOf(vertex)) {
                int target = baseGraph.getEdgeTarget(outEdge);
                int source = baseGraph.getEdgeSource(outEdge);

                if(target == vertex){
                    if(containsVertex(source)) {
                        addEdge(source, vertex);
                    }
                } else if (source == vertex){
                    if(containsVertex(target)) {
                        addEdge(vertex, target);
                    }
                } else {
                    throw new IllegalStateException("Error: Edge not connected to vertex " + vertex);
                }


            }
        }
    }


}
