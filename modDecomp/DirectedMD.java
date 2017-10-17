package dicograph.modDecomp;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.UnmodifiableDirectedGraph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Created by Fynn Leitow on 11.10.17.
 */
public class DirectedMD {

    final UnmodifiableDirectedGraph<String, DefaultEdge> inputGraph;
    final Logger log;
    final int nVertices;
    AsUndirectedGraph<String, DefaultEdge> G_s;
    SimpleGraph<String, DefaultEdge> G_d;

    final boolean debugMode; // false for max speed, true for nicely sorted vertices etc.
    final String[] vertexForIndex;
    Map<String, Integer> vertexToIndex;



    public DirectedMD(SimpleDirectedGraph<String, DefaultEdge> input, Logger logger, boolean debugMode){

        inputGraph = new UnmodifiableDirectedGraph<>(input);
        log = logger;
        nVertices = input.vertexSet().size();
        vertexForIndex = new String[nVertices];
        vertexToIndex = new HashMap<>(nVertices*4/3);
        this.debugMode = debugMode;

        // Initialize Index-Vertex-BiMap
        if(debugMode){
            TreeSet<String> sortedVertices = new TreeSet<>(input.vertexSet());
            int count = 0;
            for(String vertex : sortedVertices){
                vertexForIndex[count] = vertex;
                vertexToIndex.put(vertex, count);
                count++;
            }
        } else {
            int count = 0;
            for(String vertex : input.vertexSet()){
                vertexForIndex[count] = vertex;
                vertexToIndex.put(vertex, count);
                count++;
            }
        }


    }

    public void computeModularDecomposition() throws InterruptedException,IOException{

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

        String baum = "tree";


    }

    MDTree intersectPartitiveFamiliesOf(MDTree T_a, MDTree T_b) throws InterruptedException,IOException{
        MDTree ret = new MDTree();

        // Notes from section 2:

        //  Th.5: modules of an undirected graph form a strongly partitive family
        //  Th.3: internal node X of str.part.fam. F is either
        //       complete: Any union of children is in F
        //       prime:     no         -||-
        //  T(F) is the inclusion tree (Hasse-Diagram) of F.

        // processing section 3:

        // F = F_a \cap F_b is family of sets which are members in both F_a and F_b.

        // 0.) get the sets from the tree and compute their union. (initialized bool array???)
        // -> Iteration über die Bäume liefert die Eingabe-Daten für DH
        // todo: Das alles irgendwie in Linearzeit hinbekommen x)

        HashMap<BitSet,RootedTreeNode> nontrivModulesBoolA = T_a.getStrongModulesBool(vertexToIndex);
        HashMap<BitSet, RootedTreeNode> nontrivModulesBoolB = T_b.getStrongModulesBool(vertexToIndex);
        HashSet<BitSet> nontrivModulesTemp = new HashSet<>(nontrivModulesBoolA.keySet());
        nontrivModulesTemp.addAll(nontrivModulesBoolB.keySet());

        ArrayList<BitSet> allNontrivModules= new ArrayList<>(nontrivModulesTemp); // need a well-defined order
        allNontrivModules.sort(new BitSetComparator());

        StringBuilder overlapInput = new StringBuilder();

        /*
        // todo: Kann ich die trivialen weglassen? Ja!.
        StringBuilder allVertices = new StringBuilder();
        for(int i=0; i< nVertices;i++){
            overlapInput.append(i).append(" -1\n");
            allVertices.append(i).append(" ");
        }
        allVertices.append("-1\n");
        overlapInput.append(allVertices);
        // todo: merken - Index 0 bis nVertices-1: die Singletons. nVertices: V.
        */

        // todo: Indices n+1 bis n+allNontrivModules.size() -> ebendiese.
        for(BitSet module : allNontrivModules){
            for(int i =0; i<nVertices; i++){
                if(module.get(i)){
                    overlapInput.append(i).append(" ");
                }
            }
            overlapInput.append("-1\n");
        }


        // 1.) use Dahlhaus algorithm to compute the overlap components
        log.fine("Input for Dahlhaus algorith:\n" + overlapInput);
        File dahlhausFile = new File("dahlhaus.txt");

        // Try with ressources
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dahlhausFile))){
            System.out.println(dahlhausFile.getCanonicalPath());
            writer.write(overlapInput.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList<Integer> overlapComponentNumbers = dahlhausProcessDelegator("dahlhaus.txt", log);

        // Moooment. Die Knoten im Overlap-Graph sind:
        // 1. Die Singleton-Subsets
        // 2. V
        // 3. Die oben berechneten Module

        HashMap<Integer, ArrayList<BitSet>> overlapComponents =new HashMap<>();
        for(int i = 0; i< overlapComponentNumbers.size(); i++){

            int componentNr = overlapComponentNumbers.get(i);
            overlapComponents.putIfAbsent(componentNr, new ArrayList<>());

            // add contents
            /*
            if(i < nVertices) {
                BitSet singleVertex = new BitSet(nVertices);
                singleVertex.set(i);
                overlapComponents.get(componentNr).add(singleVertex);
            } else if (i == nVertices){
                BitSet allVset = new BitSet(nVertices);
                allVset.set(0,nVertices);
                overlapComponents.get(componentNr).add(allVset);
            } else {
                overlapComponents.get(componentNr).add(allNontrivModules.get(i-nVertices-1));
            }
            */
            overlapComponents.get(componentNr).add(allNontrivModules.get(i));
        }

        String test = "baum";
        // 2.) todo: use booleanArray to compute σ(T_a,T_b) = the union of the overlap components in O(V).


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

    public static ArrayList<Integer> dahlhausProcessDelegator(String inputFile, Logger log)
            throws InterruptedException,IOException
    {
        List<String> command = new ArrayList<>();
        command.add("./dahlhaus"); // ./OverlapComponentProg/main
        command.add(inputFile);
        ArrayList<Integer> ret = new ArrayList<>();

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment();

        Process process = processBuilder.start();
        InputStream inputStream = process.getInputStream();
        InputStreamReader inputReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputReader);
        String nextLine;
        int count = 0;
        while ((nextLine = bufferedReader.readLine()) != null) {
            log.fine(nextLine);
            count ++;
            if(count == 7){
                for(String res : nextLine.split(" ")){
                    ret.add(Integer.valueOf(res));
                }
            }
        }
        log.info("Dahlhaus algorithm finished");
        return ret;
    }

}
