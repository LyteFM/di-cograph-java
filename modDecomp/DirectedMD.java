package dicograph.modDecomp;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

import dicograph.utils.BitSetComparatorAsc;
import dicograph.utils.BitSetComparatorDesc;

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

        // Not a problem according to Th 22. Just retrieve the vertices unsorted as arraylist -> |M| < 2m + 3n
        // 1. bucket sort the array lists by size (bound met)
        // 2. bucket sort the array lists of same size;
        //    2.a) sort them at the same time and keep an "equals" boolean-flag OR
        //    2.b) iterate again, setting checking for equality
        // -> Create the String at the same time.
        // much better than my current approach with BitSets of size n :)

        // todo: hier direkt ArraysList mit Integers bekommen!
        // Brauche ich die corresp. TreeNode irgendwann?
        HashMap<BitSet,RootedTreeNode> nontrivModulesBoolA = T_a.getStrongModulesBool(vertexToIndex);
        HashMap<BitSet, RootedTreeNode> nontrivModulesBoolB = T_b.getStrongModulesBool(vertexToIndex);

        HashSet<BitSet> nontrivModulesTemp = new HashSet<>(nontrivModulesBoolA.keySet());
        nontrivModulesTemp.addAll(nontrivModulesBoolB.keySet());

        // todo: hier BucketSortBySize
        ArrayList<BitSet> allNontrivModules= new ArrayList<>(nontrivModulesTemp); // need a well-defined order
        allNontrivModules.sort(new BitSetComparatorDesc()); // descending size

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
        */

        for(BitSet module : allNontrivModules){
            for (int i = module.nextSetBit(0); i >= 0; i = module.nextSetBit(i + 1)) {
                overlapInput.append(i).append(" ");
            }

//            for(int i =0; i<nVertices; i++){
//                if(module.get(i)){
//                    overlapInput.append(i).append(" ");
//                }
//            }
            overlapInput.append("-1\n");
        }


        // 1.) use Dahlhaus algorithm to compute the overlap components (Bound: |M| <= 4m + 6n)
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

        HashMap<Integer, BitSet> overlapComponents = new HashMap<>();
        for(int i = 0; i< overlapComponentNumbers.size(); i++){

            int componentNr = overlapComponentNumbers.get(i);
            if (overlapComponents.containsKey(componentNr)) {
                overlapComponents.get(componentNr).or(allNontrivModules.get(i));
            } else {
                overlapComponents.put(componentNr, allNontrivModules.get(i));
            }

            //overlapComponents.putIfAbsent(componentNr, new ArrayList<>());

            // add contents - but don't need trivial
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
            //overlapComponents.get(componentNr).add(allNontrivModules.get(i)); // jetzt passt das auch
        }
        // What exacty _are_ the overlap Components now? number -> module
        // what do I want: number -> set with all vertices. therefore:
        //   I. Put them all in one ArrayList
        //  II. unionSort the ArrayList
        // currently with BitSets

        String test = "baum";
        // 2.) todo: use booleanArray to compute σ(T_a,T_b) = the union of the overlap components in O(V).
        // not even necessary - σ is obtained by simply joining the components, no need to check for equality

        // According to paper, V and the singleton subsets must be in σ(T_a, T_b) = { U ς | ς is overlap components of S(T_a) \cup S(T_b)
        // Excluded them as they Don't contribute to computing the overlap components. Let's add them now.


        // Assuming now: ArrayList of ArrayList<Int> with the singletons, V and the overlapComponents
        // currently: BitSets

        // 3.) @Lemma 11: compute the inclusion tree of σ(T_a, T_b)
        // V trivially at root, singletons as leaves? -> singletons are below the components anyways.
        // Might be able to re-use the RootedTreeNodes



        // Step 1: Sort the array by size, using bucket sort todo: add V (last) and singletons (front) :)
        // Sorting.bucketSortBySize() // todo: Das brauche ich häufiger! Abstrakt mit Generics machen!
        ArrayList<BitSet> nontrivOverlapComponents = new ArrayList<>(overlapComponents.values());
        nontrivOverlapComponents.sort(new BitSetComparatorDesc());

        // Step 2: Create a List for each v ∈ V of the members of F (i.e. the elements of σ) containing v in ascending order of their size:


        // init empty
        ArrayList<ArrayList<BitSet>> xLists = new ArrayList<>(nVertices);
        for (int i = 0; i < nVertices; i++) {
            xLists.add(i, new ArrayList<>());
        }
        // add root
        BitSet rootSet = new BitSet(nVertices);
        rootSet.set(0, nVertices - 1);
        nontrivOverlapComponents.add(0, rootSet);

        // - visit each Y ∈ F in descending order of size. For each x ∈ Y, insert pointer to Y to front of x's list. [O(sz(F)]
        for (BitSet ySet : nontrivOverlapComponents) {
            for (int i = ySet.nextSetBit(0); i >= 0; i = ySet.nextSetBit(i + 1)) {
                ArrayList<BitSet> currList = xLists.get(i);
                currList.add(0, ySet);
                // root is now in every xList
            }
        }

        RootedTree rootedTree = new RootedTree();
        RootedTreeNode root = new RootedTreeNode();
        rootedTree.setRoot(root);
        HashMap<BitSet, RootedTreeNode> bitsetToTreenode = new HashMap<>(nontrivOverlapComponents.size() * 4 / 3);
        HashMap<RootedTreeNode, BitSet> nodesWithLeavesOnly = new HashMap<>();
        bitsetToTreenode.put(rootSet, root);
        // Brauche noch ein Set, das die unteren Treenodes verwaltet...
        int nodeCount = 0;
        // - visit each x ∈ V, put a parent pointer from each member of x's list to its successor in x's list (if not already done)
        //      -> these are chains of ancestors of {x}
        for (int vertexNr = 0; vertexNr < nVertices; vertexNr++) {
            // I can stop once all are in the tree. todo: why even use vertexNr?
            while (nodeCount < nontrivOverlapComponents.size()) {
                ArrayList<BitSet> currVertexList = xLists.get(vertexNr);
                for (int bIndex = 0; bIndex < currVertexList.size() - 1; bIndex++) {

                    BitSet currModule = currVertexList.get(bIndex);
                    RootedTreeNode currTreenode = bitsetToTreenode.get(currModule);
                    // current node already has a parent -> nothing to do. No child gets added twice.

                    if (currTreenode == null) {
                        currTreenode = new RootedTreeNode();
                        bitsetToTreenode.put(currModule, currTreenode);
                        nodesWithLeavesOnly.put(currTreenode, currModule); // assuming this at first

                        BitSet parentModule = currVertexList.get(bIndex + 1);
                        RootedTreeNode parentTreeNode = bitsetToTreenode.get(parentModule);
                        if (parentTreeNode == null) {
                            // todo: does that ever happen? yes, we go bottom-up. Are the 1st always...?
                            parentTreeNode = new RootedTreeNode();
                            bitsetToTreenode.put(parentModule, parentTreeNode);
                        }
                        nodesWithLeavesOnly.remove(parentTreeNode);
                        parentTreeNode.addChild(currTreenode);
                    }

                }
            }
        }
        // adds the leaf-entries
        for (Map.Entry<RootedTreeNode, BitSet> nodeEntry : nodesWithLeavesOnly.entrySet()) {
            BitSet bits = nodeEntry.getValue();
            for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
                nodeEntry.getKey().addChild(new MDTreeLeafNode(vertexForIndex[i]));
            }
        }



        // 4.) Algorithm 1: compute Ü(T_a,T_b) = A* \cap B*; A = {X | X ∈ σ(T_a, T_b) AND X node in T_a OR P_a not prime in T_a}, B analog
        //     and number its members according to the number pairs from P_a, P_b

        // Step 1: Initialize the inclusion tree, i.e. each node must have:
        //         - a parent pointer (ok)
        //         - a list of pointers to its children (atm: firstChild, then iterate rightSibling)
        //         - record of how many children it has (numChildren, ok)
        //         - how many children are marked (hmm...)
        // use alg. 1 to mark the maximal members of F that are subsets of any Node X \in V.
        // i.e.: compute P_a(X) and P_b(X) for all X in the tree of σ(T_a,T_b)
        // P_a(X): smallest (size) node of T_a containing X as proper subset.
        // If X is union of siblings in T_a -> P_a(X) is their parent!
        // todo: I'll do that for every (inner?) node of the inclusion tree of σ(T_a,T_b)

        // Step 2: Take the initialized inclusion tree and test its nodes for membership in A* and B*,
        // using Algorithm 1. This yields Ü(T_a,T_b).


        // 5.) Bucket sort members of Ü(T_a,T_b) according to number pairs given by P_a, P_b
        //     to get equivalence classes of R_U (Lem 9)

        // 6.) union of each eq. class via boolean array (to get result from Th. 10)

        // now we have the Tree T(H) = T_a Λ T_b


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
