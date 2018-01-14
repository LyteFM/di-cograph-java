package dicograph.Editing;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.List;

import dicograph.utils.Edge;

/**
 * Created by Fynn Leitow on 10.01.18.
 */
public class Solution {

    private EditType type;
    private SimpleDirectedGraph<Integer,DefaultWeightedEdge> graph;
    private List<Edge> edits;

    Solution(SimpleDirectedGraph<Integer,DefaultWeightedEdge> _graph, List<Edge> _edges, EditType _type){
        graph = _graph;
        edits = _edges;
        type = _type;
    }

    public EditType getType() {
        return type;
    }

    public SimpleDirectedGraph<Integer, DefaultWeightedEdge> getGraph() {
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