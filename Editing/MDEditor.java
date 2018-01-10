package dicograph.Editing;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.io.ImportException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import dicograph.graphIO.GraphGenerator;
import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.MDTree;
import dicograph.modDecomp.MDTreeNode;
import dicograph.utils.Edge;
import dicograph.utils.WeightedPair;
import ilog.concert.IloException;

/**
 * Created by Fynn Leitow on 20.12.17.
 */
public class MDEditor {

    final SimpleDirectedGraph<Integer,DefaultWeightedEdge> inputGraph;
    final  int nVertices;
    final Logger log;
    final MDTree inputTree;
    final List<Edge> oldInputEdits;
    final EditType type;

    boolean first;

    final List<Edge> newEdits; // All Edits -> local. But need all NEW edits (i.e. from prev/lower primes)
    SimpleDirectedGraph<Integer, DefaultWeightedEdge> workGraph; // in theory, I can have several! One best enough, though.


    // 1st run: just one. Easy.
    // 2nd run:
    //      first prime: Simply add all the edits that worked on this prime, with count as key.
    //
    //      next primes: combine with ALL previous solutions, updating the count. Assert: no same!
    //
    //      last prime: again, combine with ALL before.
    //                  - in the end, combine every of them with _prevEdits_
    //                  - check the Tree / forbiddenSubs. If successful: try removing loops.
    List< List<Edge>> currentSolutions;


    public MDEditor(SimpleDirectedGraph<Integer,DefaultWeightedEdge> input, MDTree tree, Logger logger, List<Edge> oldEdits, EditType ed){
        inputGraph = input;
        nVertices = inputGraph.vertexSet().size();
        workGraph = GraphGenerator.deepClone(inputGraph);
        log = logger;
        type = ed;
        inputTree = tree;
        newEdits = new LinkedList<>();
        oldInputEdits = oldEdits;
        currentSolutions = new LinkedList<>();
        if(oldInputEdits != null){
            currentSolutions.add(new ArrayList<>(oldInputEdits));
        }
    }

    public MDEditor(SimpleDirectedGraph<Integer,DefaultWeightedEdge> input, MDTree tree, Logger logger,EditType ed){
        this(input, tree, logger, null, ed);
        first = true;
    }



    // want:
    // TreeMap<Integer, List<Solution>>
    // key < -1 means unsuccessful.
    public TreeMap<Integer, List<Solution>> editIntoCograph()
            throws ImportException, IOException, InterruptedException, IloException{

        // ret todo nur die ist am Ende wichtig!!!
        TreeMap<Integer, List<Solution>> finalSolutions = new TreeMap<>();

        TreeMap<Integer, List<List<Edge>>> currentEditResults = new TreeMap<>();

        log.info(()->"Original Tree: " + MDTree.beautify(inputTree.toString()));
        TreeMap<Integer,LinkedList<MDTreeNode>> depthToPrimes = inputTree.getPrimeModulesBottomUp();
        if(!depthToPrimes.isEmpty()){
            boolean finished = false;
            for(Map.Entry<Integer,LinkedList<MDTreeNode>> entry : depthToPrimes.descendingMap().entrySet()){
                log.info(()->"Editing primes on lvl " + entry.getKey());
                for(MDTreeNode primeNode : entry.getValue()){
                    log.info(()->"Editing prime: " + primeNode);
                    
                    List<Edge> editEdges = new LinkedList<>();
                    currentEditResults = computeRealEditsForNode(primeNode);
                    if(currentEditResults.isEmpty()){
                        log.warning(() -> "Aborting, no edit found for this prime: " + primeNode);
                        return finalSolutions;
                    }

                    List<List<Edge>> allNewSolutions = new LinkedList<>();
                    // Combine every new edit with every previous edit
                    for(Map.Entry<Integer, List<List<Edge>>> editsForCost : currentEditResults.entrySet() ){
                        for(List<Edge> newEdit : editsForCost.getValue()) {
                            for (List<Edge> previousEdit : currentSolutions) {

                                assert editIsValid(previousEdit,newEdit) : "Illegal situation - edit of one prime included in edit of other!!!";
                                newEdit.addAll(previousEdit);
                                allNewSolutions.add(newEdit);
                                // todo: is List enough or do I need Map for current solution??? -> should be ok as I check all of them for the best one...
                            }
                        }
                    }
                    log.info("Total number of current solutions: " + allNewSolutions.size());
                    currentSolutions = allNewSolutions;
                }
            }
            // if we're in a successive run and already did some editing:
            if(oldInputEdits != null){
                log.info("Initially all edits of this run: " + newEdits);
                log.info("adding previous and removing double edits.");
                ArrayList<Edge> checkRemoved = new ArrayList<>(newEdits);
                if(checkRemoved.size() != removeDoublesCheckLoops(newEdits)){
                    checkRemoved.removeAll(newEdits); // now only the loops and doubles
                    editGraph(workGraph,checkRemoved);// away with them.
                }

                // todo: After the last edit...
                /*

                 // second round with brute force, want to choose from ALL possible solutions if gap >= 0 todo!!!
                int bestScore = nVertices * nVertices;
                int score = 0;
                for (Map.Entry<Integer, List<List<WeightedPair<Integer, Integer>>>> listOfEdits : allPossibleEdits.entrySet()) {
                    for (List<WeightedPair<Integer, Integer>> oneEdit : listOfEdits.getValue()) {

                        ArrayList<Edge> currentList = new ArrayList<>(oneEdit.size());
                        // edits between modules
                        if (listOfEdits.getKey() != oneEdit.size()) {
                            currentList.addAll(subGraph.addModuleEdges(oneEdit, log));
                        }
                        // convert subgraph-edge to real edge
                        for (WeightedPair<Integer, Integer> subEdge : oneEdit) {
                            int src = subGraph.getSubNoToBaseNo()[subEdge.getFirst()];
                            int dst = subGraph.getSubNoToBaseNo()[subEdge.getSecond()];
                            currentList.add(new Edge(src, dst));
                        }
                        // remove doubles, check loops
                        score = removeDoublesCheckLoops(currentList);
                        if (score <= bestScore) {
                            log.info("Found edit with cost " + score + ": " + currentList);
                            bestScore = score;
                            costToEdits.clear();
                            costToEdits.addAll(currentList);
                        }


                    }
                }

                 */


                /*
                List<IntEdge> tmp = new ArrayList<>(newEdits);
                newEdits.removeAll(oldInputEdits);
                oldInputEdits.removeAll(tmp);
                newEdits.addAll(oldInputEdits);


                // check for loops:
                List<IntEdge> loops = new LinkedList<>();
                for(int i = 0; i< newEdits.size(); i++){
                    for (int j = i+1; j < newEdits.size(); j++) {
                        Pair<Integer,Integer> _1st = newEdits.get(i);
                        Pair<Integer,Integer> _2nd = newEdits.get(j);
                        if(_1st != _2nd){
                            if((int) _1st.getFirst() == _2nd.getSecond() && (int) _1st.getSecond() == _2nd.getFirst()){
                                loops.add(_1st);
                                loops.add(_2nd);
                            }
                        }
                    }
                }
                if(!loops.isEmpty()){
                    workGraph.workGraph(loops);
                    DirectedMD looplessMD = new DirectedMD(workGraph, log, false);
                    MDTree looplessRes = looplessMD.computeModularDecomposition();
                    if(looplessRes.getPrimeModulesBottomUp().isEmpty()){
                        newEdits.removeAll(loops);
                    } else {
                        workGraph.workGraph(loops);
                    }
                }
                */
            } // else: just need to verify one solution. Error ok if 1st.

            if(finished)
                log.info("Total Cost: " + newEdits.size() + ", edits: " + newEdits);
            else
                log.info("Current Cost: " + newEdits.size() + ", edits: " + newEdits);
        } else {
            log.info("Input was already a di-cograph!");
        }

        //return workGraph;
        return finalSolutions;
    }

    public int getCost(){
        return newEdits.size();
    }

    public List<Edge> getNewEdits() {
        return newEdits;
    }

    // Positive, empty or negative.
    private TreeMap<Integer,List<List<Edge>>> computeRealEditsForNode(MDTreeNode primeNode)throws ImportException, IOException, InterruptedException,
            IloException{

        TreeMap<Integer,List<List<Edge>>> allRealEdits = new TreeMap<>();
        // 1. create weighted subgraph todo: but only from this node, right???
        PrimeSubgraph subGraph = new PrimeSubgraph(workGraph,primeNode);
        log.info("Subgraph: " + subGraph.toString());
        log.info("Base-Vertex to Sub-Vertex " + subGraph.getBaseNoTosubNo());

        log.info(() -> "Computing all possible edit Sets for node " + primeNode);
        // negative cost - not yet successful (aborted in step 1 due to threshold/ processed all)
        // empty: nothing found in step 2
        TreeMap<Integer, List<List<WeightedPair<Integer, Integer>>>> allPossibleEdits = subGraph.computeEdits(log, type, first);

        int fst = allPossibleEdits.firstKey();
        log.info( "Edits computed from cost: " + fst + " to cost: " + allPossibleEdits.lastKey());


        // Todo: With the second-round-brute-force-approach, I should:
        // - compute ALL feasable edits up to size k
        // - choose the BEST edit when taking the original graph into account
        //      - Problem: difficult if there are several new prime modules. Would need to combine them all...
        if(!allPossibleEdits.isEmpty()){
            log.info(()->"Computed Edits: " + allPossibleEdits);

            // retrieve the original vertex-Nos and corresponding edits from the main graph
            for (Map.Entry<Integer, List<List<WeightedPair<Integer, Integer>>>> listOfEdits : allPossibleEdits.entrySet()) {
                for (List<WeightedPair<Integer, Integer>> oneEdit : listOfEdits.getValue()) {

                    ArrayList<Edge> currentList = new ArrayList<>(oneEdit.size());
                    // edits between modules (as real edges)
                    if (listOfEdits.getKey() != oneEdit.size()) {
                        currentList.addAll(subGraph.addModuleEdges(oneEdit, log));
                    }
                    // convert subgraph-edges to real edges
                    currentList.addAll( subGraph.getRealEdges(oneEdit) );
                    // remove doubles, check loops -> later. Here, just add all to map.
                    int cost = listOfEdits.getKey(); // negative means not successful!
                    log.info(()->"Adding: cost " + cost + ", Real Edges: " + currentList);
                    allRealEdits.putIfAbsent(cost, new LinkedList<>());
                    allRealEdits.get( cost ).add(currentList);
                }
            }

        }
        // else: no solution for this prime.
        return allRealEdits;
    }

    private int removeDoublesCheckLoops(List<Edge> currentList) throws ImportException, InterruptedException, IOException{

        // remove doubles
        List<Edge> tmp = new ArrayList<>(currentList);
        log.info("Previous: " + oldInputEdits);
        List<Edge> prev = new ArrayList<>(oldInputEdits);
        currentList.removeAll(prev);
        prev.removeAll(tmp);
        currentList.addAll(prev);

        // check for loops:
        List<Edge> loops = new LinkedList<>();
        for(int i = 0; i< currentList.size(); i++){
            for (int j = i+1; j < currentList.size(); j++) {
                Edge _1st = currentList.get(i);
                Edge _2nd = currentList.get(j);
                if(_1st != _2nd){
                    if((int) _1st.getFirst() == _2nd.getSecond() && (int) _1st.getSecond() == _2nd.getFirst()){
                        loops.add(_1st);
                        loops.add(_2nd);
                    }
                }
            }
        }
        if(!loops.isEmpty()){
            editGraph(workGraph,loops);
            DirectedMD looplessMD = new DirectedMD(workGraph, log, false);
            MDTree looplessRes = looplessMD.computeModularDecomposition();
            if(looplessRes.getPrimeModulesBottomUp().isEmpty()){
                currentList.removeAll(loops);
            }
            editGraph(workGraph,loops); // edit back.
        }

        return currentList.size();
    }

    // testing purposes only
    boolean editIsValid(List<Edge> _prev, List<Edge> _new){
        HashSet<Edge> validator = new HashSet<>(_prev);
        validator.removeAll(oldInputEdits);
        return !_prev.removeAll(_new);
    }


    static void editGraph(Graph<Integer,DefaultWeightedEdge> g, List<Edge> edgeList){
        for(Edge e : edgeList){
            if(g.containsEdge(e.getFirst(),e.getSecond())){
                g.removeEdge(e.getFirst(),e.getSecond());
            } else {
                g.addEdge(e.getFirst(), e.getSecond());
            }
        }
    }
}
