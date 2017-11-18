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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;


/**
 * Created by Fynn Leitow on 11.10.17.
 */
public class DirectedMD {

    final UnmodifiableDirectedGraph<Integer, DefaultEdge> inputGraph;
    final Logger log;
    final int nVertices;
    SimpleGraph<Integer, DefaultEdge> G_s;
    SimpleGraph<Integer, DefaultEdge> G_d;

    final boolean debugMode; // false for max speed, true for nicely sorted vertices etc.

    final MDTreeLeafNode[] leavesOfT_a;
    final MDTreeLeafNode[] leavesOfT_b;
    List<PartitiveFamilyTreeNode> orderNodes; // they can be merged modules and in fact contain primes.



    public DirectedMD(SimpleDirectedGraph<Integer, DefaultEdge> input, Logger logger, boolean debugMode){

        inputGraph = new UnmodifiableDirectedGraph<>(input);
        log = logger;
        nVertices = input.vertexSet().size();
        leavesOfT_a = new MDTreeLeafNode[nVertices];
        leavesOfT_b = new MDTreeLeafNode[nVertices];
        this.debugMode = debugMode;

        // Simply: vertices have the numbers from 0 to n-1. Verify that in debug mode.
        if(debugMode){
            TreeSet<Integer> sortedVertices = new TreeSet<>(input.vertexSet());
            int count = 0;
            for(int vertex : sortedVertices){
                if(count != vertex){
                    throw new IllegalArgumentException("Vertex number " + count + " is " + vertex +". Vertices strictly must numbered from 0 to n-1!");
                }
                count++;
            }
        }

    }

    // Note: Parameters are:
    // printgraph - 0
    // printcc    - 1
    // check      - 0
    public static ArrayList<Integer> dahlhausProcessDelegator(String inputFile, Logger log)
            throws InterruptedException, IOException {
        List<String> command = new ArrayList<>();
        command.add("./OverlapComponentProg/main"); // ./OverlapComponentProg/main
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

    public void computeModularDecomposition() throws InterruptedException, IOException {

        log.info("init md of graph: " + inputGraph.toString());


        // Step 1: Find G_s, G_d and H

        // G_d: undirected graph s.t. {u,v} in E_d iff both (u,v) and (v,u) in E
        G_d = new SimpleGraph<>(DefaultEdge.class);
        inputGraph.vertexSet().forEach( G_d::addVertex );

        for (DefaultEdge edge : inputGraph.edgeSet()) {
            int source = inputGraph.getEdgeSource(edge);
            int target = inputGraph.getEdgeTarget(edge);
            if (inputGraph.containsEdge(target, source)) {
                G_d.addEdge(source, target);
            }
        }
        log.info("  G_d of digraph: " + G_d);

        // G_s: undirected graph s.t. {u,v} in E_s iff (u,v) in E or (v,u) in E todo: ändert nix :(
        G_s = new SimpleGraph<>(DefaultEdge.class);
        inputGraph.vertexSet().forEach( G_s::addVertex );
        for (DefaultEdge edge : inputGraph.edgeSet()) {
            int source = inputGraph.getEdgeSource(edge);
            int target = inputGraph.getEdgeTarget(edge);
            if (!G_s.containsEdge(source, target)) {
                G_s.addEdge(source, target);
            }
        }

        log.info("  G_s of digraph: " + G_s);



        // H: symmetric 2-structure with
        //    E_H(u,v) = 0 if {u,v} non-edge (i.e. non-edge in both G_s and G_d)
        //    E_H(u,v) = 1 if {u,v} edge (i.e. edge in both G_s and G_d)
        //    E_H(u,v) = 2 if (u,v) or (v,u) simple arc (i.e. edge in G_s but not G_d)

        log.info("computing md for G_d and G_s");

        // Step 2: T(G_d) and T(G_s) with algorithm for undirected graphs

        MDTree treeForG_d = new MDTree(G_d);
        MDTree treeForG_s = new MDTree(G_s);
        log.info("md for G_d:\n" + MDTree.beautify(treeForG_d.toString()));
        log.fine("DOT for G_d:\n" + treeForG_s.exportAsDot());
        log.info("md for G_s:\n" + MDTree.beautify(treeForG_s.toString()));
        log.fine("DOT for G_s:\n" + treeForG_s.exportAsDot());

        // Step 3: Find T(H) = T(G_s) Λ T(G_d)

        PartitiveFamilyTree treeForH = intersectPartitiveFamiliesOf(treeForG_s, treeForG_d);

        // I guess I should determine which node-type there is:
        treeForH.computeAllNodeTypes(this);
        log.info("Inclusion Tree with computed types: " + MDTree.beautify(treeForH.toString()));


        // Step 4: At each O-complete and 1-complete node X of T(H), order the children s.t.
        //         each equivalence class of R_X is consecutive.
        // AND
        // Step 5: At each  2-complete node Y, select an arbitrary set S of representatives from the children.
        //         order the children of Y according to a perfect factorizing permutation of G[S].
        treeForH.computeFactorizingPermutationAndReorderAccordingly(log, inputGraph, nVertices );


        // Step 6: Resulting leaf order of T(H) is a factorizing permutation of G by Lem 20,21. Use algorithm
        //         [2] to find the modular decomposition of G.
        // todo: wat? the left-to-right-order?
        // todo: I already have more than just the permutation (as Tedder says!). Use that Tree!
        ArrayList<PartitiveFamilyLeafNode> trueLeafOrder =  new ArrayList<>(nVertices);
        treeForH.getLeavesInLeftToRightOrder( trueLeafOrder );

        ArrayList<Integer> leafNumbers = new ArrayList<>(trueLeafOrder.size());
        trueLeafOrder.forEach( l ->  leafNumbers.add(l.getVertex()));
        log.info(() ->"Leaves ordered as factorizing permutation: " + leafNumbers);
        log.info("Reordered Tree: " + MDTree.beautify(treeForH.toString()));
        log.info("As .dot:\n" + treeForH.exportAsDot());





        String baum = "tree";

    }


    // todo: possible to use the same edge Objects???
    int getEdgeValueForH(int u, int v) {

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


    private PartitiveFamilyTree intersectPartitiveFamiliesOf(MDTree of_Gs_T_a, MDTree of_Gd_T_b) throws InterruptedException,IOException{

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

        // todo: Brauche ich die corresp. TreeNode irgendwann? Muss hier auch die leaves mit Index abfragen. Könnte das BitSet auch an die Node schreiben.
        HashMap<BitSet, RootedTreeNode> nontrivModulesBoolA = of_Gs_T_a.getStrongModulesBool(leavesOfT_a);
        HashMap<BitSet, RootedTreeNode> nontrivModulesBoolB = of_Gd_T_b.getStrongModulesBool(leavesOfT_b);


        // debug option: verify if the modules are correct (kills linearity).
        if(debugMode){
            String debug_T_a = of_Gs_T_a.verifyNodeTypes(G_s);
            String debug_T_b = of_Gd_T_b.verifyNodeTypes(G_d);
            String msg = "";
            if(!debug_T_a.isEmpty()) {
                msg = "Error in modules of G_s:\n" + debug_T_a;
            }

            if(!debug_T_b.isEmpty()) {
                msg += "\n\nError in modules of G_d:\n" + debug_T_b;
            }
            if(!msg.isEmpty()){
                log.severe(msg);
                throw new IllegalStateException(msg);
            }
        }


        // other way round for later

//        HashMap<RootedTreeNode, BitSet> modulesAToBitset = new HashMap<>(nontrivModulesBoolA.size() * 4 / 3);
//        for (Map.Entry<BitSet, RootedTreeNode> entry : nontrivModulesBoolA.entrySet()) {
//            modulesAToBitset.put(entry.getValue(), entry.getKey());
//        }
//        HashMap<RootedTreeNode, BitSet> modulesBToBitset = new HashMap<>(nontrivModulesBoolB.size() * 4 / 3);
//        for (Map.Entry<BitSet, RootedTreeNode> entry : nontrivModulesBoolB.entrySet()) {
//            modulesBToBitset.put(entry.getValue(), entry.getKey());
//        }


        // todo: Statt dieser ganzen Maps das alles sauber direkt and die (MD)TreeNodes oder 'ne erweiterte Klasse hängen.


        HashSet<BitSet> nontrivModulesTemp = new HashSet<>(nontrivModulesBoolA.keySet());
        nontrivModulesTemp.addAll(nontrivModulesBoolB.keySet());

        // todo: hier BucketSortBySize
        ArrayList<BitSet> allNontrivModules= new ArrayList<>(nontrivModulesTemp); // need a well-defined order
        // allNontrivModules.sort(new BitSetComparatorDesc()); // descending size
        allNontrivModules.sort( // descending size. let's try with lamdas!
                (b1, b2) -> Integer.compare(b2.cardinality(), b1.cardinality()));

        StringBuilder overlapInput = new StringBuilder();


        // Since the singletons and V itself will never overlap another module, I can exclude them here and only consider the nontrivial modules
        allNontrivModules.forEach(module -> {
            module.stream().forEach(vertexNo ->
                    overlapInput.append(vertexNo).append(" "));
            overlapInput.append("-1\n");
        });


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
                // the UNION is computed here already
                overlapComponents.get(componentNr).or(allNontrivModules.get(i));
            } else {
                overlapComponents.put(componentNr, allNontrivModules.get(i));
            }
        }
        // What exacty _are_ the overlap Components now? number -> module
        // what do I want: number -> set with all vertices. therefore:
        //   I. Put them all in one ArrayList
        //  II. unionSort the ArrayList
        // currently with BitSets

        // 2.) use booleanArray to compute σ(T_a,T_b) = the union of the overlap components in O(V).
        // not even necessary - σ is obtained by simply joining the components, no need to check for equality

        // According to paper, V and the singleton subsets must be in σ(T_a, T_b) = { U ς | ς is overlap components of S(T_a) \cup S(T_b)
        // Excluded them as they Don't contribute to computing the overlap components. Let's add them now.


        // Assuming now: ArrayList of ArrayList<Int> with the singletons, V and the overlapComponents
        // currently: BitSets

        // 3.) @Lemma 11: compute the inclusion tree of σ(T_a, T_b)
        // The previously ommitted V and the singleton sets are also added to the tree.
        RootedTree overlapInclusionTree = new PartitiveFamilyTree(overlapComponents.values(), log, nVertices);
        log.fine("Inclusion tree of overlap components: " + MDTree.beautify(overlapInclusionTree.toString()));
        HashMap<BitSet, RootedTreeNode> bitsetToOverlapTreenNode = overlapInclusionTree.getModuleToTreenode();


        // 4.) Algorithm 1: compute Ü(T_a,T_b) = A* \cap B*;
        //     A* = {X | X ∈ σ(T_a, T_b) AND X node in T_a OR P_a not prime in T_a}, B analog
        //     and number its members according to the number pairs from P_a, P_b


        LinkedList<Integer> test2 = new LinkedList<>();
        Object test3;

        // Necessary for the equivalence classes
        HashMap<RootedTreeNode, RootedTreeNode> elementOfAToP_a = new HashMap<>();
        HashMap<RootedTreeNode, RootedTreeNode> elementOfBToP_b = new HashMap<>();

        log.fine("Computing nodes with complete Parent for Tree T_a of G_s");
        HashMap<RootedTreeNode, BitSet> elementsOfA = computeNodesWithCompleteParent(
                bitsetToOverlapTreenNode, true, elementOfAToP_a);

        // Reinitialize and Compute P_b
        log.fine("Computing nodes with complete Parent for Tree T_B of G_d");
        HashMap<RootedTreeNode, BitSet> elementsOfB = computeNodesWithCompleteParent(
                bitsetToOverlapTreenNode,false, elementOfBToP_b);



        // Ü(T_a,T_b) is now simply the intersection of A* and B*
        HashMap<RootedTreeNode, BitSet> intersectionOfAandB = new HashMap<>();
        //HashSet<RootedTreeNode> onlyVals = new HashSet<>(elementsOfB.values());
        for (Map.Entry<RootedTreeNode, BitSet> entry : elementsOfA.entrySet()) {
            RootedTreeNode node = entry.getKey();
            if (elementsOfB.containsKey(node)) {
                intersectionOfAandB.put(node, entry.getValue());
            }
        }
        log.fine("Intersection of A* and B*: " + intersectionOfAandB);

        // Now, the paper suggests computing P_a and P_b for each element X \in Ü.
        // However, this has already been done in the previous step. It is either:
        // - if there is only one maximum member: it's this node
        // - if there are several maximum members: it's the computed (necessarily nonprime) lca

        // 5.) Bucket sort members of Ü(T_a,T_b) according to number pairs given by P_a, P_b
        //     to get equivalence classes of R_U (Lem 9)
        //     -> I use a HashMap instead of Bucketsorting by the sortKey.
        //     union of each eq. class via boolean array (BitSet) to get result from Th. 10

        // get an arbitrary Index for each of the Treenodes // todo: if needed, init at start
        ArrayList<RootedTreeNode> treeNodesOfA = new ArrayList<>(nontrivModulesBoolA.values());
        ArrayList<RootedTreeNode> treeNodesOfB = new ArrayList<>(nontrivModulesBoolB.values());
        // Also Map the TreeNode to its index
        HashMap<RootedTreeNode, Integer> modulesAToTreeIndex = new HashMap<>(treeNodesOfA.size() * 4 / 3);
        HashMap<RootedTreeNode, Integer> modulesBToTreeIndex = new HashMap<>(treeNodesOfB.size() * 4 / 3);
        for (int i = 0; i < treeNodesOfA.size(); i++) {
            modulesAToTreeIndex.put(treeNodesOfA.get(i), i);
        }
        for (int i = 0; i < treeNodesOfB.size(); i++) {
            modulesBToTreeIndex.put(treeNodesOfB.get(i), i);
        }

        // Compute the equivalence classes.
        HashMap<String,BitSet > equivalenceClassesR_U = new HashMap<>((elementOfAToP_a.size() + elementOfBToP_b.size()) * 2/3);

        for( Map.Entry<RootedTreeNode, BitSet> entry : intersectionOfAandB.entrySet()){

            RootedTreeNode aElement = elementOfAToP_a.get(entry.getKey());
            RootedTreeNode bElement = elementOfBToP_b.get(entry.getKey());

            // must be contained in both maps! Generate the key:
            String sortKey = modulesAToTreeIndex.get(aElement) + "-" + modulesBToTreeIndex.get(bElement);
            if(equivalenceClassesR_U.containsKey(sortKey)){
                equivalenceClassesR_U.get(sortKey).or(entry.getValue());
            } else {
                equivalenceClassesR_U.put(sortKey, entry.getValue());
            }

        }
        log.fine("Equivalence Classes: " + equivalenceClassesR_U.values());

        // 6. ) the set \mathcal S(T_a, T_b) - which is the set Family of H's MD-Tree:
        ArrayList<BitSet> strongModulesOfH = new ArrayList<>(
                equivalenceClassesR_U.size() + intersectionOfAandB.size());
        strongModulesOfH.addAll(equivalenceClassesR_U.values());
        strongModulesOfH.addAll(intersectionOfAandB.values());

        // 7. ) From that set Family, The Inclusion Tree can be constructed by Lem 11.
        // (Cor 19 requires a permutation of the leaves, which will yield the fact perm. So I need the Tree)

        PartitiveFamilyTree ret = new PartitiveFamilyTree(strongModulesOfH, log, nVertices);
        // now we have the Tree T(H) = T_a Λ T_b

        return ret;
    }

    /**
     * Let A* := {X | X ∈ σ(T_a, T_b) AND X node in T_a OR P_a not prime in T_a}, B* analog.
     * So these are the entries of σ(T_a,T_b) that also appear in T_a [T_b] or share a complete parent in T_a [T_b].
     *
     * This method use Alg 1 in order to get the desired result of Cor. 13: Testing each node
     * of σ(T_a,T_b) for membership in A* [and B*, respectively]. Then, their intersection is returned:
     *
     * Ü(T_a,T_b) = A* \cap B*;
     *
     * @param bitsetToOverlapTreenNode the entries of σ(T_a,T_b)
     */
    private HashMap<RootedTreeNode, BitSet> computeNodesWithCompleteParent(Map<BitSet, RootedTreeNode> bitsetToOverlapTreenNode, boolean isA,
                                                                           Map<RootedTreeNode, RootedTreeNode> elementOfAToP_a) {

        // todo: note - usually only use the nodes not Bitsets: Except for the leavesOf...
        //

        // Compute P_a(S) for every S \in σ with alg 1.
        // "P_a(S) is the smallest node of T_a that contains S as proper subset"
        //  -> I'll compute A* and B* directly and save the LCAs for later.
        HashMap<RootedTreeNode, BitSet> elementsOfA = new HashMap<>();

        // nodes of T_a:
        MDTreeLeafNode[] leaves;
        String logPrefix;
        if (isA) {
            leaves = leavesOfT_a;
            logPrefix = "A: ";
        } else {
            leaves = leavesOfT_b;
            logPrefix = "B: ";
        }

        // outer loop: iterate over all inner nodes of σ(T_a,T_b)
        // need to get the P_a for every node S in σ that is not directly in T_a -> lca and check if not prime.
        for (Map.Entry<BitSet, RootedTreeNode> setEntryOfSigma : bitsetToOverlapTreenNode.entrySet()) {

            // Step 1: Initialize the inclusion tree, i.e. each node must have:
            //         - a parent pointer (ok)
            //         - a list of pointers to its children (firstChild, then iterate rightSibling)
            //         - record of how many children it has (numChildren)
            //         - an initialized field for marking (marked)
            //         - how many children are marked (numChildrenMarked)

            BitSet bits = setEntryOfSigma.getKey();
            HashSet<RootedTreeNode> innerNodes = new HashSet<>(bits.cardinality()*4/3);
            HashSet<RootedTreeNode> maximumMembers = new HashSet<>();

            // Init: Mark the leaf entries of T_a corresponding to the current set of σ(T_a,T_b)
            for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
                leaves[i].addMark();
                maximumMembers.add(leaves[i]);
                RootedTreeNode parent = leaves[i].getParent();
                innerNodes.add(parent);
            }

            // Iterate bottom-up through T_a and loop, until no parents are fully marked anymore
            boolean completed = false;
            while (!completed) {
                // holds the parents - to be processed in the next iteration
                HashSet<RootedTreeNode> tmpSet = new HashSet<>(innerNodes.size());

                innerNodes.stream().filter( tnode -> tnode.getNumChildren() == tnode.getNumMarkedChildren() ).forEach(node ->{
                    node.unmarkAllChildren();
                    node.mark();
                    // nodes might have same parent.
                    tmpSet.add(node.getParent());
                    // update maximum members:
                    maximumMembers.add(node.getParent());
                    RootedTreeNode child = node.getFirstChild();
                    while (child != null) {
                        maximumMembers.remove(child);
                        child = child.getRightSibling();
                    }
                });

                completed = tmpSet.isEmpty();
                innerNodes = tmpSet;
            }
            // now, we have the maximal members of T_a marked that are subsets of an S ⊂ σ.
            // to determine if S \in A*, check if maximumMembers has only one entry OR their first shared parent is complete.


            // Step 2: Take the initialized inclusion tree of σ(T_a, T_b) and test its nodes for membership in A* and B*
            // Saving a HashMap from element of σ to P_a allows us to compute the R_U-equivalence classes.

            if (maximumMembers.size() == 1) {
                elementsOfA.put(setEntryOfSigma.getValue(), setEntryOfSigma.getKey());
                elementOfAToP_a.put(setEntryOfSigma.getValue(), maximumMembers.stream().findFirst().get() );
                log.fine(logPrefix + "Added: " + setEntryOfSigma.toString() + " directly");

            } else if (maximumMembers.size() == 0) {
                log.warning(logPrefix + "Strange: no max member for " + setEntryOfSigma.toString());

            } else {
                // compute the LCA of all maximum members and check if it is root. Note: one entry itself could be that.
                // LCA can be found in O(h), h height of the tree, at least.
                MDTreeNode lca = (MDTreeNode) RootedTree.computeLCA(maximumMembers, log);
                if(lca == null || lca.hasNoChildren()){
                    throw new IllegalStateException("LCA computation failed for: " + setEntryOfSigma);
                }
                else if (lca.getType().isDegenerate()) {

                    elementsOfA.put(setEntryOfSigma.getValue(), setEntryOfSigma.getKey());
                    elementOfAToP_a.put(setEntryOfSigma.getValue(), lca);
                    log.fine(() -> logPrefix + "Added: " + setEntryOfSigma);
                    log.fine(() -> "   with complete LCA: " + lca);
                }
                else {
                    log.fine(() -> logPrefix + "Discarded: " + setEntryOfSigma);
                    log.fine(() -> "   with prime LCA: " + lca);
                }
            }

            // Cleaup. I need to unmark all nodes. Marked nodes are now only among children of the maximum Members!
            // todo: verify!
            for (RootedTreeNode node : maximumMembers) {
                node.unmarkAllChildren();
            }
        }

        return elementsOfA;
    }

}
