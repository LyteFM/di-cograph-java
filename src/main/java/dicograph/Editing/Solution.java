package dicograph.Editing;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.List;

import dicograph.utils.Edge;

/**
 * Created by Fynn Leitow on 10.01.18.
 */
public class Solution {

    private final EditType type;
    private final SimpleDirectedGraph<Integer,DefaultEdge> graph;
    private final List<Edge> edits;

    Solution(SimpleDirectedGraph<Integer,DefaultEdge> _graph, List<Edge> _edges, EditType _type){
        graph = _graph;
        edits = _edges;
        type = _type;
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

    @Override
    public String toString(){
        return "Cost: " + getCost() + ", Edits: " + edits;
    }
}
