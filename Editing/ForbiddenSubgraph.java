package dicograph.Editing;

import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Created by Fynn Leitow on 23.12.17.
 */
enum ForbiddenSubgraph {

    // forbidden subgraphs of length 3:

    _p3(new int[]{
            1,-1,
            -1,    1,
            -1,-1    },
            1),

    _a(new int[]{
            1,-1,
            -1,    1,
            -1, 1    },
            2),

    _b(new int[]{
            -1,-1,
            1,    1,
            -1, 1    },
            2),

    _c3(new int[]{
            1,-1,
            -1,    1,
            1,-1    },
            2),

    _d3(new int[]{
            1, 1,
            -1,    1,
            1,-1    },
            3),


    // forbidden subgraphs of length 4:

    _p4(new int[]{
                1,-1,-1,
             1,    1,-1,
            -1, 1,    1,
            -1,-1, 1},
            5),

    _n(new int[]{
               -1,-1,-1,
            -1,   -1,-1,
            -1, 1,   -1,
             1, 1,-1},
             2),

    _n_bar(new int[]{
               1, 1,-1,
            1,   -1,-1,
            1, 1,    1,
            1, 1, 1    },
            8);


    private final int[] args;

    // threshold = number of ones - 1 (i.e. must be less or equal to not contain this forbidden subgraph)
    private final int threshold;

    ForbiddenSubgraph(int[] args, int threshold){
        this.args = args;
        this.threshold = threshold;
    }

    int[] get(){
        return args;
    }

    public int getThreshold() {
        return threshold;
    }

    static ForbiddenSubgraph[] len_4 = {_p4, _n, _n_bar};
    static ForbiddenSubgraph[] len_3 = {_p3, _a, _b, _c3, _d3};


    public static Pair<Map<BitSet,ForbiddenSubgraph>,Map<BitSet,ForbiddenSubgraph>> verticesToForbidden(SimpleDirectedGraph<Integer,DefaultWeightedEdge> g){

        HashMap<BitSet,ForbiddenSubgraph> len3 = new HashMap<>();
        HashMap<BitSet,ForbiddenSubgraph> len4 = new HashMap<>();
        HashMap<Pair<Integer,Integer>,Integer> edgeToCount = new HashMap<>();

        int n = g.vertexSet().size();
        boolean[][] E = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if(i != j){
                    E[i][j] = g.containsEdge(i,j);
                }
            }
        }

        // same procedure as in Cplex-Solver
        int w, x,y, z;
        boolean vars_3[], vars_4[];
        for (w = 0; w<n; w++){
            for (x = 0; x<n; x++){
                if (w!=x){
                    for (y = 0; y<n; y++){
                        if (w!=y && x!=y){
                            vars_3 = new boolean[]{
                                    E[w][x], E[w][y],
                                    E[x][w],          E[x][y],
                                    E[y][w], E[y][x]
                            };

                            // subgraphs of length 3:
                            findForbiddenSubgraphs(ForbiddenSubgraph.len_3,len3, edgeToCount, E, vars_3,w,x,y);

                            // subgraphs of length 4:
                            for (z = 0; z<n; z++){
                                if (w != z && x!=z && y!=z){
                                    vars_4 = new boolean[]{
                                            E[w][x], E[w][y], E[w][z],
                                            E[x][w],          E[x][y], E[x][z],
                                            E[y][w], E[y][x],          E[y][z],
                                            E[z][w], E[z][x], E[z][y]
                                    };
                                    findForbiddenSubgraphs(ForbiddenSubgraph.len_4,len4, edgeToCount, E, vars_4,w,x,y,z);
                                }
                            }
                        }
                    }
                }
            }
        }

        return new Pair<>(len3,len4);
    }

    private static void findForbiddenSubgraphs(ForbiddenSubgraph[] subs, Map<BitSet,ForbiddenSubgraph> subsMap, Map<Pair<Integer,Integer>,Integer> edgeCount,
                                               boolean[][] matrix, boolean[] subMatrix, int ... vertices){
        int sum, index;
        // check every subgraph:
        for(int j = 0; j< subs.length; j++){

            sum = 0;
            // matrix must have same lenght as subgraphs.
            for(index=0; index < subMatrix.length; index++){
                if(subMatrix[index]){
                    sum += subs[j].get()[index];
                }
            }
            if(sum > subs[j].getThreshold()) {
                BitSet vertexSet = new BitSet();
                // subs
                for(int v : vertices){
                    vertexSet.set(v);
                }
                subsMap.put(vertexSet, subs[j]);

                // edge scores
                for(int u : vertices){
                    for (int v : vertices){
                        if(u != v && matrix[u][v]){
                            Pair<Integer,Integer> e = new Pair<>(u,v);
                            int cnt = edgeCount.getOrDefault(e,0);
                            edgeCount.put(e,++cnt);
                        }
                    }
                }
            }
        }
    }

    public static void computeScores(Pair<Map<BitSet,ForbiddenSubgraph>,Map<BitSet,ForbiddenSubgraph>> subsMap, Logger log, PrimeSubgraph p, boolean orig){
        int[] vertexScores = new int[p.getnVertices()];

        for(BitSet vertices : subsMap.getFirst().keySet()){
            vertices.stream().forEach( v -> vertexScores[v]++ );
        }
        for(BitSet vertices : subsMap.getSecond().keySet()){
            vertices.stream().forEach( v -> vertexScores[v]++ );
        }


        if(orig){
            for (int i = 0; i < p.getBase().vertexSet().size(); i++) {
                if(p.getBaseNoTosubNo().get(i) != null) {
                    int subNo = p.getBaseNoTosubNo().get(i);
                    log.info("v: " + i + " score: " + vertexScores[subNo]);
                }
            }
        } else {
            for (int i = 0; i < p.getnVertices(); i++) {
                log.info("v: " + i + " score: " + vertexScores[i]);
            }
        }


        // compute the 1-Neighborhood-score. Might be more insightful.
        int[] touchingVerticesScores = new int[p.getnVertices()];
        for(BitSet vertices : subsMap.getFirst().keySet()){
            vertices.stream().forEach( v -> {
                for(DefaultWeightedEdge e : p.edgesOf(v)){
                    if(p.getEdgeSource(e) == v){
                        touchingVerticesScores[p.getEdgeTarget(e)]++;
                    } else {
                        touchingVerticesScores[p.getEdgeSource(e)]++;
                    }
                }
            } );
        }

        if(orig){
            for (int i = 0; i < p.getBase().vertexSet().size(); i++) {
                if(p.getBaseNoTosubNo().get(i) != null) {
                    int subNo = p.getBaseNoTosubNo().get(i);
                    log.info("v: " + i + " T-score: " + touchingVerticesScores[subNo]);
                }
            }
        } else {
            for (int i = 0; i < p.getnVertices(); i++) {
                log.info("v: " + i + " T-score: " + touchingVerticesScores[i]);
            }
        }
    }

}
