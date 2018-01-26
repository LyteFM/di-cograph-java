package dicograph.Editing;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.List;

import dicograph.utils.Edge;

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

public class Solution {

    private final EditType type;
    private final SimpleDirectedGraph<Integer,DefaultEdge> graph;
    private final List<Edge> edits;
    private double treeDistance;

    Solution(SimpleDirectedGraph<Integer,DefaultEdge> _graph, List<Edge> _edges, EditType _type){
        graph = _graph;
        edits = _edges;
        type = _type;
        treeDistance = Double.MAX_VALUE;
    }

    public EditType getType() {
        return type;
    }

    public SimpleDirectedGraph<Integer, DefaultEdge> getGraph() {
        return graph;
    }

    public List<Edge> getEdits() {
        return edits;
    }

    public int getCost() {
        return edits.size();
    }

    public double getTreeDistance() {
        return treeDistance;
    }

    public void setTreeDistance(double treeDistance) {
        this.treeDistance = treeDistance;
    }

    @Override
    public String toString(){
        return "Type: " + type + ", Cost: " + getCost() + ", Edits: " + edits;
    }
}
