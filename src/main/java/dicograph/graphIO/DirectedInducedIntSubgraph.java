package dicograph.graphIO;

import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.BitSet;
import java.util.List;
import java.util.TreeSet;

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

/**
 * Created by Fynn Leitow on 09.11.17. Only Integers as vertices allowed s.t. BitSet works.
 */
public class DirectedInducedIntSubgraph<E> extends SimpleDirectedGraph<Integer, E>
{

    private final SimpleDirectedGraph<Integer, E> base;

    private DirectedInducedIntSubgraph(Graph<Integer, E> baseGraph) {
        super(baseGraph.getEdgeFactory());
        base = (SimpleDirectedGraph<Integer, E>) baseGraph;
    }

    public DirectedInducedIntSubgraph(Graph<Integer, E> baseGraph, BitSet vertices) {
        this(baseGraph);

        vertices.stream().forEach( this::addVertex );
        addEdges();
    }

    public DirectedInducedIntSubgraph(Graph<Integer, E> baseGraph, List<Integer> vertices) {
        this(baseGraph);

        vertices.forEach( this::addVertex );
        addEdges();
    }

    private void addEdges() {
        // adds edges to vertices that are contained in this subgraph
        for (int vertex : vertexSet()) {
            for (E outEdge : base.outgoingEdgesOf(vertex)) {
                int target = base.getEdgeTarget(outEdge);

                if (containsVertex(target)) {
                    addEdge(vertex, target);
                }
            }
        }
    }

    /**
     * Naive, nonlinear method to verify whether the graph is a tournament.
     * one equivalence condition for being a tournament is:
     * "The score sequence of T is {0,1,2,...,n-1}".
     * For debugging purposes only.
     * @return true, if this graph is a Tournament
     */
    public boolean isTournament(){
        TreeSet<Integer> outdegrees = new TreeSet<>();
        for(Integer vertex : vertexSet()){
            outdegrees.add(outDegreeOf(vertex));
        }
        int score = 0;
        for(int outDeg :outdegrees){
            if(outDeg != score)
                return false;
            score++;
        }
        return true;
    }

    public SimpleDirectedGraph<Integer, E> getBase() {
        return base;
    }
}
