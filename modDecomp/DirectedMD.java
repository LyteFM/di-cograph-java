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
import java.util.Iterator;
import java.util.LinkedList;
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
    final MDTreeLeafNode[] leavesOfT_a;
    final MDTreeLeafNode[] leavesOfT_b;



    public DirectedMD(SimpleDirectedGraph<String, DefaultEdge> input, Logger logger, boolean debugMode){

        inputGraph = new UnmodifiableDirectedGraph<>(input);
        log = logger;
        nVertices = input.vertexSet().size();
        vertexForIndex = new String[nVertices];
        vertexToIndex = new HashMap<>(nVertices*4/3);
        leavesOfT_a = new MDTreeLeafNode[nVertices];
        leavesOfT_b = new MDTreeLeafNode[nVertices];
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

    // Note: Parameters are:
    // printgraph - 0
    // printcc    - 1
    // check      - 0
    public static ArrayList<Integer> dahlhausProcessDelegator(String inputFile, Logger log)
            throws InterruptedException, IOException {
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
            count++;
            if (count == 7) {
                for (String res : nextLine.split(" ")) {
                    ret.add(Integer.valueOf(res));
                }
            }
        }
        log.info("Dahlhaus algorithm finished");
        return ret;
    }

    int getEdgeValueForH(String u, String v) {

        boolean inG_s = G_s.containsEdge(u, v);
        boolean inG_d = G_d.containsEdge(u, v);

        if (!inG_d && !inG_s) {
            return 0;
        } else if (inG_d && inG_s) {
            return 1;
        } else if (!inG_d && inG_s) {
            return 2;
        } else {
            throw new IllegalStateException("Error: illegal state in H for edge (" + u + ":" + v + ")");
        }
    }

    RootedTreeNode computeLCA(List<RootedTreeNode> lowerNodes, Logger log) {

        boolean alreadyReachedRoot = false;
        // init
        HashMap<Integer, LinkedList<RootedTreeNode>> inputNodeToAncestor = new HashMap<>(lowerNodes.size() * 4 / 3);
        HashMap<RootedTreeNode, Integer> allTraversedNodes = new HashMap<>();
        HashSet<Integer> lastNodeRemaining = new HashSet<>();

        for (int i = 0; i < lowerNodes.size(); i++) {
            LinkedList<RootedTreeNode> list = new LinkedList<>();
            list.add(lowerNodes.get(i));
            allTraversedNodes.put(lowerNodes.get(i), i);
            inputNodeToAncestor.put(i, list);
            lastNodeRemaining.add(i);
        }
        int currDistance = 0;
        // Traverse the Tree bottom-up

        while (lastNodeRemaining.size() > 1) {
            Iterator<Map.Entry<Integer, LinkedList<RootedTreeNode>>> nodesIter = inputNodeToAncestor.entrySet().iterator();
            while (nodesIter.hasNext()) {
                Map.Entry<Integer, LinkedList<RootedTreeNode>> currEntry = nodesIter.next();
                RootedTreeNode parent = currEntry.getValue().peekLast().getParent();
                if (allTraversedNodes.containsKey(parent)) {

                    // already present: close this list.
                    lastNodeRemaining.remove(currEntry.getKey());
                    if (parent.isRoot()) {
                        log.fine("Reached root: " + currEntry.toString());
                        // No problem if just one went up to root.
                        if (alreadyReachedRoot) {
                            log.fine("Root is LCA.");
                            return parent;
                        }
                        alreadyReachedRoot = true;
                    } else {
                        log.fine("Closing: " + currEntry.toString());
                    }
                    nodesIter.remove();
                    // We're done if this was the last.
                    if (lastNodeRemaining.size() == 1) {
                        return parent;
                    }

                } else {
                    // add the parent
                    currEntry.getValue().addLast(parent);
                    allTraversedNodes.put(parent, currEntry.getKey());
                }
            }
            currDistance++;

        }
        log.warning("Error: Unexpected Exit!");
        return null;
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
        // Brauche ich die corresp. TreeNode irgendwann? Muss hier auch die leaves mit Index abfragen.
        HashMap<BitSet, RootedTreeNode> nontrivModulesBoolA = T_a.getStrongModulesBool(vertexToIndex, leavesOfT_a);
        HashMap<BitSet, RootedTreeNode> nontrivModulesBoolB = T_b.getStrongModulesBool(vertexToIndex, leavesOfT_b);
        // other way round for later
        HashMap<RootedTreeNode, BitSet> modulesAToBitset = new HashMap<>(nontrivModulesBoolA.size() * 4 / 3);
        for (Map.Entry<BitSet, RootedTreeNode> entry : nontrivModulesBoolA.entrySet()) {
            modulesAToBitset.put(entry.getValue(), entry.getKey());
        }
        HashMap<RootedTreeNode, BitSet> modulesBToBitset = new HashMap<>(nontrivModulesBoolB.size() * 4 / 3);
        for (Map.Entry<BitSet, RootedTreeNode> entry : nontrivModulesBoolB.entrySet()) {
            modulesBToBitset.put(entry.getValue(), entry.getKey());
        }



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
                overlapComponents.get(componentNr).or(allNontrivModules.get(i)); // todo: hier passiert was dämilches. Lieber erstmal mit Listen arbeiten?
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
        // Might be able to re-use the RootedTreeNode s



        // Step 1: Sort the array by size, using bucket sort todo: add V (last) and singletons (front) :)
        // Sorting.bucketSortBySize() // todo: Das brauche ich häufiger! Abstrakt mit Generics machen!
        HashSet<BitSet> overlapComponentsNoDoubles = new HashSet<>(overlapComponents.values());
        // todo: hier ärgerlicher Fehler für gespeicherten Dahlhaus-input
        if (debugMode && overlapComponentsNoDoubles.size() != overlapComponents.size()) {
            log.warning("Overlap compenents were merged into an already existing compontent!");
        }
        ArrayList<BitSet> nontrivOverlapComponents = new ArrayList<>(overlapComponentsNoDoubles);

        nontrivOverlapComponents.sort(new BitSetComparatorDesc());
        log.fine("Overlap components: " + nontrivOverlapComponents);


        // Step 2: Create a List for each v ∈ V of the members of F (i.e. the elements of σ) containing v in ascending order of their size:


        // init empty
        ArrayList<ArrayList<BitSet>> xLists = new ArrayList<>(nVertices);
        for (int i = 0; i < nVertices; i++) {
            xLists.add(i, new ArrayList<>());
        }
        // add root
        BitSet rootSet = new BitSet(nVertices);
        rootSet.set(0, nVertices); // toIndex must be n
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
        HashMap<BitSet, RootedTreeNode> bitsetToOverlapTreenNode = new HashMap<>(nontrivOverlapComponents.size() * 4 / 3);
        HashMap<RootedTreeNode, BitSet> nodesWithLeavesOnly = new HashMap<>();
        bitsetToOverlapTreenNode.put(rootSet, root);
        // Brauche noch ein Set, das die unteren Treenodes verwaltet...
        int nodeCount = 1; // root ist bereits drin.
        // - visit each x ∈ V, put a parent pointer from each member of x's list to its successor in x's list (if not already done)
        //      -> these are chains of ancestors of {x}
        while (nodeCount < nontrivOverlapComponents.size()) {
            for (int vertexNr = 0; vertexNr < nVertices; vertexNr++) {
                // I can stop once all are in the tree. todo: why even use vertexNr?

                ArrayList<BitSet> currVertexList = xLists.get(vertexNr);
                for (int bIndex = 0; bIndex < currVertexList.size() - 1; bIndex++) {

                    BitSet currModule = currVertexList.get(bIndex);
                    RootedTreeNode currTreenode = bitsetToOverlapTreenNode.getOrDefault(currModule, null);
                    // current node already has a parent -> nothing to do. No child gets added twice.

                    if (currTreenode == null) {
                        currTreenode = new RootedTreeNode();
                        bitsetToOverlapTreenNode.put(currModule, currTreenode);
                        nodesWithLeavesOnly.put(currTreenode, currModule); // assuming this at first
                        nodeCount++;

                        BitSet parentModule = currVertexList.get(bIndex + 1);
                        RootedTreeNode parentTreeNode = bitsetToOverlapTreenNode.get(parentModule);
                        if (parentTreeNode == null) {
                            // todo: does that ever happen? yes, we go bottom-up. Are the 1st always...?
                            parentTreeNode = new RootedTreeNode();
                            bitsetToOverlapTreenNode.put(parentModule, parentTreeNode);
                            nodeCount++; // todo: Baum stimmt noch nicht ganz :/ muss die parent-pointer richtiger machen.
                        }
                        nodesWithLeavesOnly.remove(parentTreeNode);
                        parentTreeNode.addChild(currTreenode);
                    }

                }

            }
        }
        log.fine("Inclusion Tree of overlap components: " + rootedTree.toString());


        // 4.) Algorithm 1: compute Ü(T_a,T_b) = A* \cap B*;
        //     A = {X | X ∈ σ(T_a, T_b) AND X node in T_a OR P_a not prime in T_a}, B analog
        //     and number its members according to the number pairs from P_a, P_b

        // Step 1: Initialize the inclusion tree, i.e. each node must have:
        //         - a parent pointer (ok)
        //         - a list of pointers to its children (atm: firstChild, then iterate rightSibling)
        //         - record of how many children it has (numChildren, ok)
        //         - an initialized field for marking
        //         - how many children are marked (hmm...)
        // -> ok, RootedTreeNode

        // use alg. 1 to compute P_a(X) and P_b(X) for all X in the tree of σ(T_a,T_b)
        // todo: P_a(X): smallest (size) node of T_a containing X as proper subset.
        // If X is union of siblings in T_a -> P_a(X) is their parent!
        // todo: I'll do that for every (inner?) node of the inclusion tree of σ(T_a,T_b)


        // or is that easier with BitSets??? A ⊂ B means e.g.
        // A = 0001 0010 1011
        // B = 1011 0011 1011
        // -> A  OR B == B. If A had elements not contained in B, the result wouldn't be B.
        //    note: this would yield true if A was empty.

        // adds the leaf-entries to the trees and saves them in a List
        // todo: brauch das eigentlich nicht.
        PartitiveFamilyLeafNode[] allLeafs = new PartitiveFamilyLeafNode[nVertices];
        for (Map.Entry<RootedTreeNode, BitSet> nodeEntry : nodesWithLeavesOnly.entrySet()) {
            BitSet bits = nodeEntry.getValue();
            for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
                PartitiveFamilyLeafNode child = new PartitiveFamilyLeafNode(i, vertexForIndex[i]);
                nodeEntry.getKey().addChild(child);
                allLeafs[i] = child;
            }
        }

        LinkedList<Integer> test2 = new LinkedList<>();

        // Compute P_a(S) for every S \in σ with alg 1. -> nope, I'll compute A* and B* directly
        // "P_a(S) is the smallest node of T_a that contains S as proper subset"
        // HashMap<RootedTreeNode , MDTreeNode> P_a = new HashMap<>();
        // HashMap<RootedTreeNode , MDTreeNode> P_b = new HashMap<>();

        HashSet<Map.Entry<BitSet, RootedTreeNode>> elementsOfA = new HashSet<>();
        // nodes of T_a:
        HashSet<RootedTreeNode> innerNodes = new HashSet<>(bitsetToOverlapTreenNode.size() * 4 / 3);
        HashMap<RootedTreeNode, BitSet> nodesOfT_aWithLeaves = new HashMap<>();
        HashSet<RootedTreeNode> maximumMembers = new HashSet<>();

        // outer loop: iterate over all inner nodes of σ(T_a,T_b)
        // want to get the P_a for every node S in σ!!!
        for (Map.Entry<BitSet, RootedTreeNode> setEntryOfSigma : bitsetToOverlapTreenNode.entrySet()) {

            nodesOfT_aWithLeaves.clear();
            maximumMembers.clear();
            innerNodes.clear();
            BitSet bits = setEntryOfSigma.getKey();
            // Init: Mark the leaf entries of T_a corresponding to the current set of σ(T_a,T_b)
            for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
                leavesOfT_a[i].addMark(); // todo: for T_b, and can I re-use?
                maximumMembers.add(leavesOfT_a[i]);
                RootedTreeNode parent = leavesOfT_a[i].getParent(); //MDTreeNode. brauche ich nicht typ?
                BitSet bitSet = modulesAToBitset.get(parent);
                // NodesWithLeaves setzen.
                nodesOfT_aWithLeaves.put(parent, bitSet); // todo: ok this way?
            }
            // Now, iterate bottom-up through T_a, not through σ!!!
            for (RootedTreeNode node : nodesOfT_aWithLeaves.keySet()) {
                if (node.getNumChildren() == node.getNumMarkedChildren()) {
                    node.unmarkAllChildren();
                    node.mark();
                    // nodes can have same parent, HashSet keeps us safe
                    innerNodes.add(node.getParent());
                    // update maximum members:
                    maximumMembers.add(node.getParent());
                    RootedTreeNode child = node.getFirstChild();
                    while (child != null) {
                        maximumMembers.remove(child);
                        child = child.getRightSibling();
                    }
                }
                // else: not completely marked - ignore.
            }
            // I might need to start at root top-down, then go bottom-up
            // Now simply loop, until no parents are fully marked anymore
            // todo: do the map-operations
            boolean completed = false;
            while (!completed) {
                // holds the parents - to be processed in the next iteration
                HashSet<RootedTreeNode> tmpSet = new HashSet<>();
                for (RootedTreeNode node : innerNodes) {
                    if (node.getNumMarkedChildren() == node.getNumChildren()) {
                        node.unmarkAllChildren();
                        node.mark();
                        tmpSet.add(node.getParent());
                        // update maximum members:
                        maximumMembers.add(node.getParent());
                        RootedTreeNode child = node.getFirstChild();
                        while (child != null) {
                            maximumMembers.remove(child); // todo: does this work?
                            child = child.getRightSibling();
                        }
                    }
                }
                completed = tmpSet.isEmpty();
                innerNodes = tmpSet;
            }
            // now, we have the maximal members of T_a marked that are subsets of an S ⊂ σ.
            // to determine if S \in A*, check if maximumMembers has only one entry OR their first shared parent is complete.
            if (maximumMembers.size() == 1) {
                elementsOfA.add(setEntryOfSigma);
                log.fine("Added: " + setEntryOfSigma.toString());
            } else if (maximumMembers.size() == 0) {
                log.warning("Strange: no max member for " + setEntryOfSigma.toString());
            } else {
                // compute the LCA of all maximum members and check if it is root. Note: one entry itself could be that.
                // LCA can be found in O(h), h height of the tree, at least.
                ArrayList<RootedTreeNode> maxMembersList = new ArrayList<>(maximumMembers);
                RootedTreeNode lca = computeLCA(maxMembersList, log); // indices are useful


            }

        }

        // todo: Gefährlich mit dem ganzen Klassen-casten. ggf noch eine Modules-List erstellen für T_a, T_b, um indizes zu haben.


        // Reinitialize and Compute P_b with alg 1

        // IMPORTANT: before step to, I need to compute P_a and P_b to compute A* and B*!!!
        // unfortunately, no algorithm was given to compute P_a and P_b...
        // No Problem: I can directly compute A* and B*!!!



        // Step 2: Take the initialized inclusion tree of σ(T_a, T_b) and test its nodes for membership in A* and B*,
        // using Algorithm 1. This yields Ü(T_a,T_b).


        // 5.) Bucket sort members of Ü(T_a,T_b) according to number pairs given by P_a, P_b
        //     to get equivalence classes of R_U (Lem 9)

        // 6.) union of each eq. class via boolean array (to get result from Th. 10)

        // now we have the Tree T(H) = T_a Λ T_b


        // Th. 15: T(F) =T_a Λ T_b in O(sz(S(F_a)) + sz(S(F_b))

        return ret;
    }

}
