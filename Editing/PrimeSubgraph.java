package dicograph.Editing;

import com.google.common.collect.Sets;

import org.jgrapht.Graphs;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.io.ImportException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.MDTree;
import dicograph.modDecomp.MDTreeNode;
import dicograph.utils.Edge;
import dicograph.utils.Parameters;
import dicograph.utils.WeightedPair;
import ilog.concert.IloException;

/**
 * Created by Fynn Leitow on 21.12.17.
 */
public class PrimeSubgraph extends SimpleDirectedGraph<Integer,DefaultWeightedEdge> {

    private final Parameters p;
    // parameters
    private static final int maxCost = 20; // from parameter k

    // prev: Force stop if forbiddenSub-Score <= this value during firstRun run and use brute-force/branching/ILP in second run to complete.

    // For brute force:
    private static boolean useMD = false; // takes too long for small n

    private final SimpleDirectedGraph<Integer, DefaultWeightedEdge> base;
    private final int nVertices;

    private final HashMap<Integer,Integer> baseNoTosubNo;
    private final double[] subVertexToWeight;
    private final int[] subNoToBaseNo;
    private final MDTreeNode primeNode;


    public PrimeSubgraph(SimpleDirectedGraph<Integer,DefaultWeightedEdge> baseGraph, MDTreeNode node, Parameters params){
        super(new ClassBasedEdgeFactory<>(DefaultWeightedEdge.class),true);
        base = baseGraph;
        p = params;
        baseNoTosubNo = new HashMap<>();
        primeNode = node;
        subVertexToWeight = node.initWeightedSubgraph(this,base);
        nVertices = subVertexToWeight.length;
        subNoToBaseNo = new int[nVertices];
        for(Map.Entry<Integer, Integer> base2Sub : baseNoTosubNo.entrySet()){
            subNoToBaseNo[base2Sub.getValue()] =  base2Sub.getKey();
        }
    }


    // an entry in the map means: if present -> remove, if not -> add.
    // Key > 0 => success! Key < 0 => not yet done, but ok after round 1; empty => fail after step 2.
    public TreeMap<Integer, List<List<WeightedPair<Integer, Integer>>>> computeEdits(Logger log, EditType method, boolean first)
    throws InterruptedException, IOException, ImportException, IloException{
        TreeMap<Integer, List<List<WeightedPair<Integer, Integer>>>> costToEdges = new TreeMap<>(); // results
        boolean thresholdReached = false;

        // Recognize all forbidden subgraphs and compute subgraph-scores for edits.
        HashMap<Edge,Integer> edgeToCount = new HashMap<>();
        Pair<Map<BitSet,ForbiddenSubgraph>,Map<BitSet,ForbiddenSubgraph>> badSubs = ForbiddenSubgraph.verticesToForbidden(this, edgeToCount,false);
        log.info("Length 3:\n" + badSubs.getFirst());
        log.info("Length 4:\n" + badSubs.getSecond());
        // sort descending
        ArrayList<Map.Entry<Edge,Integer>> edgesToScore = new ArrayList<>(edgeToCount.entrySet());
        edgesToScore.sort( Comparator.comparingInt(e ->  -Math.abs( e.getValue() )));
        log.info("Edges by subgraph-score: " + edgesToScore.size() + "\n" + edgesToScore);



        int cnt = 0;
        Map<Integer, WeightedPair<Integer,Integer>> intToEdge = new HashMap<>();

        // Computes all weights for an edit between subVertices i,j.
        double[][] weightMatrix = new double[nVertices][nVertices];
        for (int i : vertexSet()) {
            for (int j : vertexSet()) {
                if(j!=i){
                    double weight = subVertexToWeight[i] * subVertexToWeight[j];
                    weightMatrix[i][j] = weight;
                    // this only for brute force:
                    if(method == EditType.Primes) {
                        WeightedPair<Integer, Integer> e = new WeightedPair<>(i, j, weight);
                        intToEdge.put(cnt, e);
                        cnt++;
                    }
                }
            }
        }

        if(method == EditType.Lazy || first) {
            SimpleDirectedGraph<Integer,DefaultEdge> graphOfEditEdges = new SimpleDirectedGraph<>(DefaultEdge.class);
            ConnectivityInspector<Integer,DefaultEdge> pathFinder = new ConnectivityInspector<>(graphOfEditEdges);
            List<WeightedPair<Integer, Integer>> currEdgeList = new LinkedList<>(); // just one edge
            List<WeightedPair<Integer,Integer >> allEdgesList = new LinkedList<>();
            List<WeightedPair<Integer, Integer>> edgesToRemove = new LinkedList<>(); // remove edits below threshold, if no successful edit found.
            WeightedPair<Integer,Integer > lastEdge = null;
            int i, u,v, u_v_score, global_u_v,global_v_u, both;
            int v_u_score = 0;
            int prevScore = badSubs.getFirst().size() + badSubs.getSecond().size();
            int subCount = prevScore;
            log.info("Total no of forbidden subs: " + subCount);
            int cost = 0;
            double weight;
            for (i = 0; i < edgesToScore.size(); i++) {
                Map.Entry<Edge,Integer> edge = edgesToScore.get(i);
                // don't do that for very small modules...
                if(first && allEdgesList.size() > 3 && Math.abs(edge.getValue()) <= p.getHardThreshold()){
                    thresholdReached = true;
                    log.warning("Reached hard threshold " + p.getHardThreshold() + " for edge " + edge);
                    break;
                }
                u = edge.getKey().getFirst();
                v = edge.getKey().getSecond();
                // compute the graph of edited edits to exclude existing paths (any direction)
                boolean addedU = graphOfEditEdges.addVertex(u);
                boolean addedV = graphOfEditEdges.addVertex(v);
                if( !addedU && !addedV ){
                    log.info("Both vertices for path " + u + ", " + v + " already exist!");
                    if(p.isSkipExistingVertices()){
                        continue;
                    } else if(p.isSkipPaths() && pathFinder.pathExists(u,v)) {
                        log.warning("Skipped: path already exists from " + u + " to " + v);
                        continue;
                    }
                }
                weight = subVertexToWeight[u] * subVertexToWeight[v];
                log.info("Subgraph-Score: " + edge.getValue() + ", weight: " + weight);
                // determine the edit direction (Problem: what if both?) todo: Häh???

                // global score might be useful, too.
                currEdgeList.clear();
                global_u_v = computeEditScoreForEgde(u,v,weight,currEdgeList);
                log.info("("+ u + ","  +v + ") global edit-score: " + global_u_v);
                currEdgeList.clear();
                global_v_u = computeEditScoreForEgde(v,u,weight,currEdgeList);
                log.info("("+ v + ","  +u + ") global edit-score: "+ global_v_u);
                //currEdgeList.add(new WeightedPair<>(u,v,weight)); v,u already there
                both = computeEditScoreForEgde(u,v,weight,currEdgeList);
                log.info("both global: "+ both);
                currEdgeList.clear();

                boolean globallyFeasable = global_u_v < subCount ||
                        global_v_u < subCount || both < subCount;

                currEdgeList.addAll(allEdgesList);

                u_v_score = computeEditScoreForEgde(u,v,weight,currEdgeList);
                log.info("("+ u + ","  +v + ") local edit-score: " + u_v_score);

                if(u_v_score != 0) {
                    currEdgeList.clear();
                    currEdgeList.addAll(allEdgesList);
                    v_u_score = computeEditScoreForEgde(v, u, weight, currEdgeList);
                    log.info("("+ v + ","  +u + ") local edit-score: " + v_u_score);

                    if(v_u_score == 0)
                        lastEdge = new WeightedPair<>(v,u,weight);
                } else {
                    lastEdge = new WeightedPair<>(u,v,weight);
                }

                if(lastEdge != null){
                    // we're done.
                    log.info("Subgraph edited into Cograph with edge " + lastEdge);
                    allEdgesList.add(lastEdge);
                    List<List<WeightedPair<Integer, Integer>>> res = new LinkedList<>();
                    res.add(allEdgesList);
                    cost += Math.round(weight);
                    costToEdges.put(cost,res);
                    break;
                }
                // todo: gut möglich, dass BEIDE Kantenrichtungen editiert werden...

                if( (u_v_score < prevScore || v_u_score < prevScore) && (!p.isRequireGlobal() || globallyFeasable) ) {

                    if(!globallyFeasable){
                        log.warning("Adding edge with unexpected global score!");
                    }
                    cost += Math.round(weight);
                    if (u_v_score < v_u_score) {
                        prevScore = addEditEdge(u,v,weight,u_v_score, v_u_score, log,graphOfEditEdges, allEdgesList);
                    } else if (v_u_score < u_v_score) {
                        prevScore = addEditEdge(v,u,weight, v_u_score, u_v_score, log,graphOfEditEdges, allEdgesList);
                    } else {
                        log.warning("Same score for u "+ u + ", v " + v);
                        prevScore = addEditEdge(u,v,weight,u_v_score, v_u_score, log,graphOfEditEdges, allEdgesList);
                    }
                    // This is only for the Primes-Brute-Force method atm.
                    // todo: introduce the entry points for all other methods!
                    if(method == EditType.Primes && first) {
                        edit(allEdgesList);
                        DirectedMD checkSizeMD = new DirectedMD(this, log, false);
                        MDTree checkSizeTree = checkSizeMD.computeModularDecomposition();
                        if (checkSizeTree.getMaxPrimeSize() <= p.getBruteForceThreshold()) {
                            log.info("Size of prime modules now below " + p.getBruteForceThreshold() + ". Ready for second run.");
                            List<List<WeightedPair<Integer, Integer>>> res = new LinkedList<>();
                            res.add(allEdgesList);
                            costToEdges.put(-res.size(), res); // not yet a solution, but ok.
                            return costToEdges;
                        } else {
                            edit(allEdgesList); // edit back
                        }
                    }

                    if(edge.getValue() <= p.getSoftThreshold())
                        edgesToRemove.add(allEdgesList.get(allEdgesList.size() - 1));
                } else {
                    log.info("Discarded: edge (" + u + "," + v + ") which doesn't improve the module" );
                    if(addedU)
                        graphOfEditEdges.removeVertex(u);
                    if(addedV)
                        graphOfEditEdges.removeVertex(v);
                }

                currEdgeList.clear();
            }
            if(i == edgesToScore.size() || thresholdReached){
                log.info(()->"Processed all edits or reached hard threshold - no cograph-edit found!!!");
                log.info(()->"To remove (below soft threshold): " + edgesToRemove);

                // ok if we're in step 1:
                if(first) {
                    List<List<WeightedPair<Integer, Integer>>> res = new LinkedList<>();
                    // remove those below soft threshold:
                    allEdgesList.removeAll(edgesToRemove);
                    res.add(allEdgesList);
                    costToEdges.put(-res.size(), res);
                    return costToEdges;
                } else {
                    log.warning(()->"All Chosen Subgraph edges so far: " + allEdgesList);
                    log.warning(()->"As real edges: " + getRealEdges( allEdgesList ));
                    return costToEdges; // empty.
                }
            }
        }
        // ILP todo: Timeout! -> ret empty!
        else if(method == EditType.ILP){
            // todo: later, I only need one best solution!
            CplexDiCographEditingSolver primeSolver = new CplexDiCographEditingSolver(
                    this, new int[]{0,1}, weightMatrix, log);
            primeSolver.solve();
            for (int i = 0; i < primeSolver.getEditingDistances().size(); i++) {
                int val = (int) Math.round(primeSolver.getEditingDistances().get(i));
                costToEdges.putIfAbsent(val,new LinkedList<>());
                costToEdges.get(val).add(primeSolver.getSolutionEdgeEdits().get(i));
            }

            log.info("MD Tree for CPlex subgraph solution:");
            edit(primeSolver.getSolutionEdgeEdits().get(0));
            DirectedMD subMD = new DirectedMD(this, log, false);
            MDTree subTree = subMD.computeModularDecomposition();
            log.info(MDTree.beautify(subTree.toString()));

        }
        // Cost-aware Brute Force
        // todo: add other options!
        else if(method == EditType.Primes){
            int smallestCost = maxCost + 1;
            for (int i = 1; i < maxCost; i++) {
                // e.g. maxCost=5, smallest cost was 2 but wanted all solutions up to cost 4
                if (smallestCost + p.getSolutionGap() < i) {
                    return costToEdges;
                }
                log.info("Computing all permutations of size " + i + " for n = " + cnt);
                Set<Set<Integer>> combinations = Sets.combinations(intToEdge.keySet(), i);
                log.info(combinations.size() + " permutations!");
                boolean costStillValid = false;

                int count = 0;
                boolean success = false;


                for (Set<Integer> edgeSubset : combinations) {
                    int cost = 0;
                    List<WeightedPair<Integer, Integer>> currEdgeList = new ArrayList<>(edgeSubset.size());
                    for (int edgeNo : edgeSubset) {
                        WeightedPair<Integer, Integer> e = intToEdge.get(edgeNo);
                        currEdgeList.add(e);
                        cost += Math.round(e.getWeight()); // better use rounding, just in case...
                    }
                    if (cost < maxCost) {

                        costStillValid = true;

                        edit(currEdgeList);
                        // Brute force checking for forbidden subgraphs is way more efficient...
                        if(useMD) {
                            DirectedMD subMD = new DirectedMD(this, log, false);
                            MDTree subTree = subMD.computeModularDecomposition();
                            if(subTree.getPrimeModulesBottomUp().isEmpty()){
                                success = true;
                                log.info("Tree: " + MDTree.beautify(subTree.toString()));
                            }
                        } else {
                            Pair<Map<BitSet,ForbiddenSubgraph>,Map<BitSet,ForbiddenSubgraph>> check = ForbiddenSubgraph.verticesToForbidden(this, new HashMap<>(),true);
                            success = check.getFirst().isEmpty() && check.getSecond().isEmpty();
                        }
                        if(success){
                            log.info(() -> " Successful subgraph edit found: " + currEdgeList);
                            costToEdges.putIfAbsent(cost, new LinkedList<>());
                            costToEdges.get(cost).add(currEdgeList);
                            // need this to verify if an early, but expensive edit is best.
                            if (cost < smallestCost) {
                                if (p.getSolutionGap() < 0) {
                                    edit(currEdgeList);
                                    return costToEdges; // take the firstRun one below maxCost
                                }
                                smallestCost = cost;
                            }
                        }

                        // edit back.
                        edit(currEdgeList);

                    }


                    count++;
                    if (count % 1000000 == 0)
                        log.info("i: " + i + ", Count: " + count + ", Edges: " + currEdgeList);

                    // todo: timer instead!
                    //if(count == 50000000) {
                    //    log.info("Aborted after 500");
                    //    break;
                    //}
                }
                // all of size i are done now.


                if (!costStillValid) {
                    break; // no need to continue
                }

            }
        }

        // Subgraph Edges!
        return costToEdges;
    }

    static int addEditEdge(int u, int v, double weight, int u_v_score, int v_u_score, Logger log,
                           SimpleDirectedGraph<Integer,DefaultEdge> graphOfEditEdges, List<WeightedPair<Integer,Integer>> allEdgesList){
        allEdgesList.add(new WeightedPair<>(u, v, weight));
        graphOfEditEdges.addEdge(u,v);
        log.info("Chose edge (" + u + "," + v + ") with score: " + u_v_score +" < " + v_u_score);
        return u_v_score;
    }

    // returns the additional edits (With real vertexNos)
    List<Edge> addModuleEdges(List<WeightedPair<Integer,Integer>> edgeList, Logger log){
        List<Edge> addEdges = new LinkedList<>();
        for (WeightedPair<Integer, Integer> singleEdge : edgeList) {
            if (Math.round(singleEdge.getWeight()) > 1) {
                log.info(() -> "Adding edits for " + singleEdge);
                int realFirstVertex = subNoToBaseNo[singleEdge.getFirst()];
                int realSecondVertex = subNoToBaseNo[singleEdge.getSecond()];
                List<Edge> added = primeNode.addModuleEdges(realFirstVertex,realSecondVertex);
                log.info(() -> "Added: " + added);
                addEdges.addAll(added);
            }
        }
        return addEdges;
    }

    // high score means bad edit.
    private int computeEditScoreForEgde(int u, int v, double weight, List<WeightedPair<Integer,Integer>> currEdgeList){
        HashMap<Edge,Integer> edgeToCount = new HashMap<>();
        currEdgeList.add(new WeightedPair<>(u,v,weight));
        edit(currEdgeList);
        Pair<Map<BitSet,ForbiddenSubgraph>,Map<BitSet,ForbiddenSubgraph>> badSubs = ForbiddenSubgraph.verticesToForbidden(this, edgeToCount,false);
        edit(currEdgeList);
        int badScore = badSubs.getSecond().size() + badSubs.getFirst().size();
        // take any solution here, nevermind.
        if(badScore != 0 && weight > 1.5){
            // I'm officially an idiot, score must INCREASE of course...
            badScore =  (int) Math.round( badScore + (p.getWeightMultiplier() * weight));
        }
        return badScore;
    }

    public void edit(List<WeightedPair<Integer,Integer>> edgeList){

        for(WeightedPair<Integer,Integer> e : edgeList){
            if(containsEdge(e.getFirst(),e.getSecond())){
                removeEdge(e.getFirst(),e.getSecond());
            } else {
                // todo: due to weighted Pair, I might not need weightedEdge at all.
                Graphs.addEdge(this, e.getFirst(), e.getSecond(), e.getWeight());
            }
        }

    }

    // convert subgraph-edge to real edge
    List<Edge> getRealEdges(List<WeightedPair<Integer,Integer>> oneEdit){
        List<Edge> ret = new ArrayList<>(oneEdit.size());
        for (WeightedPair<Integer, Integer> subEdge : oneEdit) {
            int src = subNoToBaseNo[subEdge.getFirst()];
            int dst = subNoToBaseNo[subEdge.getSecond()];
            ret.add(new Edge(src, dst));
        }
        return ret;
    }

    // this doesn't give the desired results.
    @Deprecated
    private int computePrimeScoreForEdge(int u,int v, double weight, List<WeightedPair<Integer,Integer>> currEdgeList, Logger log)
            throws InterruptedException,IOException,ImportException {
        currEdgeList.add(new WeightedPair<>(u,v,weight));
        edit(currEdgeList);
        DirectedMD subMD = new DirectedMD(this, log, false);
        MDTree subTree = subMD.computeModularDecomposition();
        edit(currEdgeList);
        return subTree.getNumPrimeChildren();
    }


    public HashMap<Integer, Integer> getBaseNoTosubNo() {
        return baseNoTosubNo;
    }

    public SimpleDirectedGraph<Integer, DefaultWeightedEdge> getBase() {
        return base;
    }

    public int getnVertices() {
        return nVertices;
    }

    public double[] getSubVertexToWeight() {
        return subVertexToWeight;
    }

    public int[] getSubNoToBaseNo() {
        return subNoToBaseNo;
    }
}
