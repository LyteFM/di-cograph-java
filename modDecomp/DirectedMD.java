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

        log.info("computing md for G_d and G_s");

        // Step 2: T(G_d) and T(G_s) with algorithm for undirected graphs
        MDTree TreeForG_d = new MDTree(G_d);
        MDTree TreeForG_s = new MDTree(G_s);
        log.info("md for G_d:\n" + TreeForG_d.toString());
        log.info("md for G_s:\n" + TreeForG_s.toString());

        // Step 3: Find T(H) = T(G_s) Λ T(G_d)

        MDTree TreeForH = intersectPartitiveFamiliesOf(TreeForG_s, TreeForG_d);


    }

    MDTree intersectPartitiveFamiliesOf(MDTree T_a, MDTree T_b){
        MDTree ret = new MDTree();

        // Notes from section 2:

        //  Th.5: modules of an undirected graph form a strongly partitive family
        //  Th.3: internal node X of str.part.fam. F is either
        //       complete: Any union of children is in F
        //       prime:     no         -||-
        //  T(F) is the inclusion tree (Hasse-Diagram) of F.

        // processing section 3:

        // F = F_a \cap F_b is family of sets which are members in both F_a and F_b.

        // 0.) get the sets from the tree and compute their union. (initialized bool array?)

        // 1.) use Dahlhaus algorithm to compute the overlap components

        // 2.) use booleanArray to compute σ(T_a,T_b) = the union of the overlap components in O(V).

        // 3.) Lemma 11: compute the inclusion tree of σ(T_a, T_b)

        // 4.) Algorithm 1: compute Ü(T_a,T_b) and number its members

        // 5.) Algorithm 1: compute P_a(X) and P_b(X) ∀ X ⊂ Ü(T_a,T_b)

        // 6.) Bucket sort members of Ü(T_a,T_b) according to number pairs given by P_a, P_b
        //     to get equivalence classes of R_U (Lem 9)

        // 7.) union of each eq. class via boolean array (to get result from Th. 10)

        // now we have the Tree T = T_a Λ T_b



        // Th. 15: T(F) =T_a Λ T_b in O(sz(S(F_a)) + sz(S(F_b))

        return ret;
    }

    int getEdgeValueForH(String u, String v){

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
