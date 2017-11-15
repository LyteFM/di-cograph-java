package dicograph.graphIO;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.BitSet;
import java.util.Set;

import dicograph.modDecomp.MDNodeType;

/**
 * Created by Fynn Leitow on 15.11.17.
 */
public class NodeTypeTester {

    public static MDNodeType determineNodeType(Graph<Integer,DefaultEdge> subgraph, boolean directed){

        Set<DefaultEdge> subEdgeSet = subgraph.edgeSet();

        if(subEdgeSet.isEmpty()){
            // no edges means 0-complete
            return MDNodeType.PARALLEL;
        } else {
            // series means degree = n-1 (or double for directed)
            boolean firstRun = true;
            boolean valid = true;
            Set<Integer> vertices = subgraph.vertexSet();
            BitSet allOutDegs = new BitSet(vertices.size());
            int expectedCount = vertices.size();
            for(int vertex : vertices){
                Set<DefaultEdge> touchingEdges = subgraph.edgesOf(vertex);
                int count = touchingEdges.size();
                if (count != expectedCount) {
                    // also order needs n-1 touching vertices
                    valid = false;
                }
                if(directed && valid){
                    // for order: need all outdegs from 0 to n-1
                    int outDeg = 0;
                    for(DefaultEdge edge : touchingEdges){
                        if(subgraph.getEdgeTarget(edge) == vertex)
                            outDeg++;
                    }
                    allOutDegs.set(outDeg);
                }
            }
            if(valid){
                if(!directed) {
                    return MDNodeType.SERIES;
                } else{
                    if(allOutDegs.cardinality() == vertices.size()){
                        return MDNodeType.SERIES;
                    } else if(allOutDegs.nextSetBit(0) == vertices.size()-1){
                        return MDNodeType.ORDER;
                    }
                }
                return null; // shouldn't happen, at that point it's either series or order, right?
            }

        }
        return MDNodeType.PRIME;
    }
}
