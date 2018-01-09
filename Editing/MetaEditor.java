package dicograph.Editing;

import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

import dicograph.utils.Parameters;

/**
 * Created by Fynn Leitow on 09.01.18.
 */
public class MetaEditor {

    final Parameters p;
    final SimpleDirectedGraph<Integer,DefaultWeightedEdge> inputGraph;
    final Logger log;




    // Original Graph and all the parameters
    // Calls MDEditor twice for each mode (first & prevEdits as flag)
    public MetaEditor(SimpleDirectedGraph<Integer,DefaultWeightedEdge> g, Parameters params, Logger logger){
        inputGraph = g;
        p = params;
        log = logger;
    }

    public void edit(){
        // List of Maps: Cost -> Edit-Graph and Edit-Edges
        List<TreeMap<Integer,
                Pair<SimpleDirectedGraph<Integer,DefaultWeightedEdge>,
                        List<Pair<Integer,Integer>>>>> allMethodsSolutions = new ArrayList<>(6);
        if(p.isLazy()){

        }
        if(p.isPrime()){

        }
    }
}
