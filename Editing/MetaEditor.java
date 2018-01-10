package dicograph.Editing;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.ImportException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.MDTree;
import dicograph.utils.Parameters;
import ilog.concert.IloException;

/**
 * Created by Fynn Leitow on 09.01.18.
 */
public class MetaEditor {

    final Parameters p;
    final SimpleDirectedGraph<Integer,DefaultWeightedEdge> inputGraph;
    final Logger log;
    final MDTree origTree;
    final int nVertices;




    // Original Graph and all the parameters
    // Calls MDEditor twice for each mode (first & oldInputEdits as flag)
    public MetaEditor(SimpleDirectedGraph<Integer,DefaultWeightedEdge> g, Parameters params, Logger logger)
            throws IOException, ImportException, InterruptedException{
        inputGraph = g;
        nVertices = g.vertexSet().size();
        p = params;
        log = logger;
        DirectedMD modDecomp = new DirectedMD(inputGraph, log, false);
        origTree = modDecomp.computeModularDecomposition();
    }

    public void computeAllMethods() throws IOException, ImportException, IloException, InterruptedException, ExportException{

        // already cograph?
        log.info("Input graph:\n" + inputGraph);
        log.info("MD of input graph:\n" + MDTree.beautify(origTree.toString()));
        if(origTree.getPrimeModulesBottomUp().isEmpty()){
            log.info("Input graph is already a dicograph. Aborting.");
            return;
        }


        // List of Maps: Cost -> One best Edit-Graph with Edit-Edges. No zeros here!
        List<TreeMap<Integer,List<Solution>>> allMethodsSolutions = new ArrayList<>(6);
        if(p.isLazy()){
            allMethodsSolutions.add( computeEditFor(EditType.Lazy) );
        }
        if(p.isPrime()){

        }
        if(p.isSoft()){

        }
        if(p.isHard()){

        }
        if(p.isIlpMD()){

        }
        if(p.isIlpOnly()){

        }

        // best solution(s) - with gap
        int best = nVertices * nVertices;
        List<Solution> bestSolutions = new LinkedList<>();
        for(TreeMap<Integer, List<Solution>> solutionMap : allMethodsSolutions){
            int cost = solutionMap.firstKey();
            if( cost <= best){
                if( cost < best && best - cost > p.getSolutionGap()){
                    bestSolutions.clear();
                }
                bestSolutions.addAll(solutionMap.firstEntry().getValue());
            }
        }
        // output one best solution as .dot:
        for(Solution solution : bestSolutions){
            int cost = solution.getCost();
            log.info(() -> "Found solution with " + cost + " edits: " + solution.getEdits());
            log.info(() ->"Edit-Graph: " + solution.getGraph());
            if(solution.getCost() == best){
                System.out.println("//Edits: " + solution.getEdits());
                DOTExporter<Integer,DefaultWeightedEdge> exporter = new DOTExporter<>();
                exporter.exportGraph(solution.getGraph(), System.out);
            }
        }

    }

    private TreeMap<Integer, List<Solution>> computeEditFor(EditType method) throws
    IOException, ImportException, InterruptedException, IloException{
        // first call
        MDEditor firstEditor = new MDEditor(inputGraph, origTree, log, method);
        TreeMap<Integer, List<Solution>> firstSolns = firstEditor.editIntoCograph();
        Solution firstSol = firstSolns.firstEntry().getValue().get(0);
        DirectedMD firstMD = new DirectedMD(firstSol.getGraph(), log, false);
        MDTree firstTree = firstMD.computeModularDecomposition();

        if(method.oneIsEnogh()){
            if(firstTree.getPrimeModulesBottomUp().isEmpty()){
                if(method == EditType.Lazy){
                    log.info(()->"Lazy method successful after first run.");
                }
                return firstSolns;
            }
        }

        // second call
        MDEditor secondEdit = new MDEditor(firstSol.getGraph(), firstTree, log, firstSol.getEdits(), method);
        TreeMap<Integer, List<Solution>> secondSolns = secondEdit.editIntoCograph();
        if(secondSolns.isEmpty()){
            log.warning(()-> method + " method was unsuccessful.");
            secondSolns.clear();
        }

        return secondSolns;
    }
}
