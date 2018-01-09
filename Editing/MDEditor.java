package dicograph.Editing;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.io.ImportException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import dicograph.graphIO.GraphGenerator;
import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.MDTree;
import dicograph.modDecomp.MDTreeNode;
import dicograph.utils.IntEdge;
import dicograph.utils.WeightedPair;
import ilog.concert.IloException;

/**
 * Created by Fynn Leitow on 20.12.17.
 */
public class MDEditor {

    final SimpleDirectedGraph<Integer,DefaultWeightedEdge> inputGraph;
    SimpleDirectedGraph<Integer, DefaultWeightedEdge> editedGraph;
    final  int nVertices;
    final Logger log;
    final MDTree inputTree;
    final List<IntEdge> allEdits;
    List<IntEdge> prevEdits;
    boolean first;
    EditType eType;

    public MDEditor(SimpleDirectedGraph<Integer,DefaultWeightedEdge> input, MDTree tree, Logger logger, List<IntEdge> oldEdits, EditType ed, boolean _1st){
        inputGraph = input;
        nVertices = inputGraph.vertexSet().size();
        editedGraph = GraphGenerator.deepClone(inputGraph);
        log = logger;
        eType = ed;
        inputTree = tree;
        allEdits = new LinkedList<>();
        prevEdits = oldEdits;
        first = _1st;
    }

    public MDEditor(SimpleDirectedGraph<Integer,DefaultWeightedEdge> input, MDTree tree, Logger logger,EditType ed){
        this(input, tree, logger, null, ed, true);
    }



    // want:
    // List<Pair<SimpleDirectedGraph<Integer,DefaultWeightedEdge>,
    //            List<IntEdge>>>
    public SimpleDirectedGraph<Integer,DefaultWeightedEdge> editIntoCograph()
            throws ImportException, IOException, InterruptedException, IloException{


        log.info(()->"Original Tree: " + MDTree.beautify(inputTree.toString()));
        TreeMap<Integer,LinkedList<MDTreeNode>> depthToPrimes = inputTree.getPrimeModulesBottomUp();
        if(!depthToPrimes.isEmpty()){
            boolean finished = false;
            for(Map.Entry<Integer,LinkedList<MDTreeNode>> entry : depthToPrimes.descendingMap().entrySet()){
                log.info(()->"Editing primes on lvl " + entry.getKey());
                for(MDTreeNode primeNode : entry.getValue()){
                    log.info(()->"Editing prime: " + primeNode);
                    
                    List<IntEdge> editEdges = new LinkedList<>();
                    finished = editSubgraph(primeNode, editEdges);
                    
                    if(!editEdges.isEmpty()) {
                        log.info(() -> "Found edit with " + editEdges.size() + " Edges for this prime: " + editEdges);
                        allEdits.addAll(editEdges);
                        editGraph(editedGraph,editEdges);
                    } else {
                        log.warning(() -> "No edit found for this prime. Aborting.");
                        break;
                    }
                }
            }
            // if we're in a successive run and already did some editing:
            if(prevEdits != null){
                log.info("Initially all edits of this run: " + allEdits);
                log.info("adding previous and removing double edits.");
                ArrayList<IntEdge> checkRemoved = new ArrayList<>(allEdits);
                if(checkRemoved.size() != removeDoublesCheckLoops(allEdits)){
                    checkRemoved.removeAll(allEdits); // now only the loops and doubles
                    editGraph(editedGraph,checkRemoved);// away with them.
                }


                /*
                List<IntEdge> tmp = new ArrayList<>(allEdits);
                allEdits.removeAll(prevEdits);
                prevEdits.removeAll(tmp);
                allEdits.addAll(prevEdits);


                // check for loops:
                List<IntEdge> loops = new LinkedList<>();
                for(int i = 0; i< allEdits.size(); i++){
                    for (int j = i+1; j < allEdits.size(); j++) {
                        Pair<Integer,Integer> _1st = allEdits.get(i);
                        Pair<Integer,Integer> _2nd = allEdits.get(j);
                        if(_1st != _2nd){
                            if((int) _1st.getFirst() == _2nd.getSecond() && (int) _1st.getSecond() == _2nd.getFirst()){
                                loops.add(_1st);
                                loops.add(_2nd);
                            }
                        }
                    }
                }
                if(!loops.isEmpty()){
                    editedGraph.editedGraph(loops);
                    DirectedMD looplessMD = new DirectedMD(editedGraph, log, false);
                    MDTree looplessRes = looplessMD.computeModularDecomposition();
                    if(looplessRes.getPrimeModulesBottomUp().isEmpty()){
                        allEdits.removeAll(loops);
                    } else {
                        editedGraph.editedGraph(loops);
                    }
                }
                */
            }
            if(finished)
                log.info("Total Cost: " + allEdits.size() + ", edges: " + allEdits);
            else
                log.info("Current Cost: " + allEdits.size() + ", edges: " + allEdits);
        } else {
            log.warning("Input was already a di-cograph!");
        }

        return editedGraph;
    }

    public int getCost(){
        return allEdits.size();
    }

    public List<IntEdge> getAllEdits() {
        return allEdits;
    }

    // TreeMap<Integer,List<List<IntEdge>>>
    // resulting edges kommt leer.
    private boolean editSubgraph(MDTreeNode primeNode, List<IntEdge> costToEdits)throws ImportException, IOException, InterruptedException,
            IloException{

        boolean finished = false;
        // 1. create weighted subgraph
        PrimeSubgraph subGraph = new PrimeSubgraph(editedGraph,primeNode);
        log.info("Subgraph: " + subGraph.toString());
        log.info("Base-Vertex to Sub-Vertex " + subGraph.getBaseNoTosubNo());



        // brute-Force approach: try out all possible edge-edits, costs from low to high, until the subgraph is non-prime.
        log.info(() -> "Computing all possible edit Sets for node " + primeNode);
        Map<Integer, List<List<WeightedPair<Integer, Integer>>>> allPossibleEdits = subGraph.computeBestEdgeEdit(log,eType, first);



        TreeSet<Integer> allSizesSorted = new TreeSet<>(allPossibleEdits.keySet());
        int fst = allSizesSorted.first();
        log.info( "Valid Edits computed from cost: " + fst + " to cost: " + allSizesSorted.last());


        // Todo: With the second-round-brute-force-approach, I should:
        // - compute ALL feasable edits up to size k
        // - choose the BEST edit when taking the original graph into account
        //      - Problem: difficult if there are several new prime modules. Would need to combine them all...
        if(!allSizesSorted.isEmpty()){
            // retrieve the original vertex-Nos and corresponding edges from the main graph
            log.info("Computed Edits: " + allPossibleEdits);

            List<List<WeightedPair<Integer, Integer>>> edits = allPossibleEdits.get(fst);
            // let's try continue on error:
            if (fst < 0) {
                fst = edits.size();
            } else {
                finished = true;
            }

            // first run or lazy: only one result.
            if(first || eType == EditType.Lazy) {
                List<IntEdge> firstEditEdges = new LinkedList<>();
                // add edges between modules
                if (fst != edits.get(0).size()) {
                    firstEditEdges.addAll(subGraph.addModuleEdges(edits.get(0), log));
                }

                // convert subgraph-edge to real edge
                for (WeightedPair<Integer, Integer> subEdge : edits.get(0)) {
                    int src = subGraph.getSubNoToBaseNo()[subEdge.getFirst()];
                    int dst = subGraph.getSubNoToBaseNo()[subEdge.getSecond()];
                    firstEditEdges.add(new IntEdge(src, dst));
                }
                costToEdits.addAll(firstEditEdges);

            } else {
                // second round with brute force, want to choose from ALL possible solutions if gap >= 0 todo!!!
                int bestScore = nVertices * nVertices;
                int score = 0;
                for (Map.Entry<Integer, List<List<WeightedPair<Integer, Integer>>>> listOfEdits : allPossibleEdits.entrySet()) {
                    for (List<WeightedPair<Integer, Integer>> oneEdit : listOfEdits.getValue()) {

                        ArrayList<IntEdge> currentList = new ArrayList<>(oneEdit.size());
                        // edges between modules
                        if (listOfEdits.getKey() != oneEdit.size()) {
                            currentList.addAll(subGraph.addModuleEdges(oneEdit, log));
                        }
                        // convert subgraph-edge to real edge
                        for (WeightedPair<Integer, Integer> subEdge : oneEdit) {
                            int src = subGraph.getSubNoToBaseNo()[subEdge.getFirst()];
                            int dst = subGraph.getSubNoToBaseNo()[subEdge.getSecond()];
                            currentList.add(new IntEdge(src, dst));
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

            }
        }
        return finished;
    }

    private int removeDoublesCheckLoops(List<IntEdge> currentList) throws ImportException, InterruptedException, IOException{

        // remove doubles
        List<IntEdge> tmp = new ArrayList<>(currentList);
        log.info("Previous: " + prevEdits);
        List<IntEdge> prev = new ArrayList<>(prevEdits);
        currentList.removeAll(prev);
        prev.removeAll(tmp);
        currentList.addAll(prev);

        // check for loops:
        List<IntEdge> loops = new LinkedList<>();
        for(int i = 0; i< currentList.size(); i++){
            for (int j = i+1; j < currentList.size(); j++) {
                IntEdge _1st = currentList.get(i);
                IntEdge _2nd = currentList.get(j);
                if(_1st != _2nd){
                    if((int) _1st.getFirst() == _2nd.getSecond() && (int) _1st.getSecond() == _2nd.getFirst()){
                        loops.add(_1st);
                        loops.add(_2nd);
                    }
                }
            }
        }
        if(!loops.isEmpty()){
            editGraph(editedGraph,loops);
            DirectedMD looplessMD = new DirectedMD(editedGraph, log, false);
            MDTree looplessRes = looplessMD.computeModularDecomposition();
            if(looplessRes.getPrimeModulesBottomUp().isEmpty()){
                currentList.removeAll(loops);
            }
            editGraph(editedGraph,loops); // edit back.
        }

        return currentList.size();
    }


    static void editGraph(Graph<Integer,DefaultWeightedEdge> g, List<IntEdge> edgeList){
        for(IntEdge e : edgeList){
            if(g.containsEdge(e.getFirst(),e.getSecond())){
                g.removeEdge(e.getFirst(),e.getSecond());
            } else {
                g.addEdge(e.getFirst(), e.getSecond());
            }
        }
    }
}
