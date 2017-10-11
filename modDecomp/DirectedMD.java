package dicograph.modDecomp;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.UnmodifiableDirectedGraph;

import java.util.logging.Logger;

/**
 * Created by Fynn Leitow on 11.10.17.
 */
public class DirectedMD {

    final UnmodifiableDirectedGraph<String, DefaultEdge> inputGraph;
    final Logger log;
    AsUndirectedGraph<String, DefaultEdge> G_s;
    SimpleGraph<String, DefaultEdge> G_d;

    public DirectedMD(SimpleDirectedGraph<String, DefaultEdge> input, Logger logger){
        inputGraph = new UnmodifiableDirectedGraph<>(input);
        log = logger;

    }

    void computeModularDecomposition(){

        log.info("Starting md of graph: " + inputGraph.toString());

        // Step 1: Find G_s, G_d and H

        // G_s: undirected graph s.t. {u,v} in E_s iff (u,v) in E or (v,u) in E
        // todo: make sure this graph is not edited!!!
        G_s = new AsUndirectedGraph<>(inputGraph);

        // G_d: undirected graph s.t. {u,v} in E_d iff both (u,v) and (v,u) in E
        G_d = new SimpleGraph<>(DefaultEdge.class);
        for(String vertex : inputGraph.vertexSet()){
            G_d.addVertex(vertex);
        }
        for(DefaultEdge edge : inputGraph.edgeSet()){
            String source = inputGraph.getEdgeSource(edge);
            String target = inputGraph.getEdgeTarget(edge);
            if(inputGraph.containsEdge(target, source)){
                G_d.addEdge(source, target);
            }
        }

        // H: symmetric 2-structure with
        //    E_H(u,v) = 0 if {u,v} non-edge (i.e. non-edge in both G_s and G_d)
        //    E_H(u,v) = 1 if {u,v} edge (i.e. edge in both G_s and G_d)
        //    E_H(u,v) = 2 if (u,v) or (v,u) simple arc (i.e. edge in G_s but not G_d)

    }

    int getH(String u, String v){

        boolean inG_s = G_s.containsEdge(u,v);
        boolean inG_d = G_d.containsEdge(u,v);

        if(!inG_d && !inG_s){
            return 0;
        } else if (inG_d && inG_s){
            return 1;
        } else if (!inG_d && inG_s){
            return 2;
        } else {
            throw new IllegalStateException("Error: illegal state in H for edge (" + u + ":" + v + ")");
        }
    }


}
