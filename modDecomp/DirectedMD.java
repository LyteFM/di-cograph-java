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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.swing.tree.TreeNode;

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

    /**
     * Computes the lowest common ancestor for a set of RootedTreeNodes.
     *
     * @param lowerNodesCollection the treenodes
     * @param log                  the logger
     * @return the LCA. null indicates an error.
     */
    public static RootedTreeNode computeLCA(Collection<RootedTreeNode> lowerNodesCollection, Logger log) {

        boolean alreadyReachedRoot = false;
        // init
        ArrayList<RootedTreeNode> lowerNodes = new ArrayList<>(lowerNodesCollection);

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
        log.warning("Error: Unexpected Exit! current Distance: " + currDistance);
        return null;
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

    public void computeModularDecomposition() throws InterruptedException, IOException {

        log.info("Starting md of graph: " + inputGraph.toString());


        // Step 1: Find G_s, G_d and H

        // G_s: undirected graph s.t. {u,v} in E_s iff (u,v) in E or (v,u) in E
        // todo: make sure this graph is not edited!!!
        G_s = new AsUndirectedGraph<>(inputGraph);

        // G_d: undirected graph s.t. {u,v} in E_d iff both (u,v) and (v,u) in E
        G_d = new SimpleGraph<>(DefaultEdge.class);
        for (String vertex : inputGraph.vertexSet()) {
            G_d.addVertex(vertex);
        }
        for (DefaultEdge edge : inputGraph.edgeSet()) {
            String source = inputGraph.getEdgeSource(edge);
            String target = inputGraph.getEdgeTarget(edge);
            if (inputGraph.containsEdge(target, source)) {
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

        RootedTree TreeForH = intersectPartitiveFamiliesOf(TreeForG_s, TreeForG_d);

        String baum = "tree";


    }

    /**
     * Implementation of the Inclusion tree according to Lemma 11
     *
     * @param inputSet a Collection of BitSets representing a partitive set family
     * @return The rooted tree of the input set
     */
    protected RootedTree getInclusionTreeFromBitsets(Collection<BitSet> inputSet, Map<BitSet, RootedTreeNode> bitsetToInclusionTreenNode) {


        // Step 0: eliminate doubles from input
        HashSet<BitSet> inputSetNoDoubles = new HashSet<>(inputSet);
        if (debugMode && inputSetNoDoubles.size() != inputSet.size()) {
            log.warning("Double entry in input for inclusion tree - possible merge in overlap components.");
        }
        ArrayList<BitSet> inputNodeList = new ArrayList<>(inputSetNoDoubles);

        // Sort the array by size, using counting sort
        // todo: Das brauche ich häufiger! Auch hier mit BucketSort...
        inputNodeList.sort( // descending size, with lamdas.
                (b1, b2) -> Integer.compare(b2.cardinality(), b1.cardinality()));
        log.fine("Input sorted by size: " + inputNodeList);


        // Create a List for each v ∈ V of the members of F (i.e. the elements of σ) containing v in ascending order of their size:


        // init empty
        ArrayList<LinkedList<BitSet>> xLists = new ArrayList<>(nVertices);
        for (int i = 0; i < nVertices; i++) {
            xLists.add(i, new LinkedList<>());
        }

        // add root
        BitSet rootSet = new BitSet(nVertices);
        rootSet.set(0, nVertices); // toIndex must be n
        inputNodeList.add(0, rootSet);

        // - visit each Y ∈ F in descending order of size. For each x ∈ Y, insert pointer to Y to front of x's list. [O(sz(F)]
        inputNodeList.forEach(ySet ->
                ySet.stream().forEach(i ->
                        xLists.get(i).addFirst(ySet)
                )
        );
        // root is now the last element in every xList


        RootedTree ret = new RootedTree();
        RootedTreeNode root = new RootedTreeNode();
        ret.setRoot(root);
        //HashMap<BitSet, RootedTreeNode> bitsetToOverlapTreenNode = new HashMap<>(nontrivOverlapComponents.size() * 4 / 3);
        bitsetToInclusionTreenNode.put(rootSet, root);

        // This creates the inclusion tree from the x's lists
        // - visit each x ∈ V, put a parent pointer from each member of x's list to its successor in x's list (if not already done)
        //      -> these are chains of ancestors of {x}

        PartitiveFamilyLeafNode[] allLeafs = new PartitiveFamilyLeafNode[nVertices];
        int relationCount = 0;
        for (int vertexNr = 0; vertexNr < nVertices; vertexNr++) {

            // retrieve List and create leafNode
            LinkedList<BitSet> currVertexList = xLists.get(vertexNr);
            PartitiveFamilyLeafNode leafNode = new PartitiveFamilyLeafNode(vertexNr, vertexForIndex[vertexNr]);

            boolean firstEntry = true;
            ListIterator<BitSet> vListIter = currVertexList.listIterator(0);
            while (vListIter.hasNext()) {

                BitSet currModule = vListIter.next();
                // create Trenode if not yet present
                RootedTreeNode currTreenode = bitsetToInclusionTreenNode.getOrDefault(currModule, null);
                if (currTreenode == null) {
                    currTreenode = new RootedTreeNode();
                    bitsetToInclusionTreenNode.put(currModule, currTreenode);
                }

                // the first entry of the list always gets the leaf attached.
                if (firstEntry) {
                    currTreenode.addChild(leafNode);
                    allLeafs[vertexNr] = leafNode;
                    firstEntry = false;
                }

                // add as child to the next element of xList
                // Note: currTreenode.equals(root) occurs when we're at the end of the list
                // Don't check for parent of root, of course.
                if (vListIter.hasNext()) {
                    BitSet parentModule = vListIter.next();
                    RootedTreeNode parentTreeNode = bitsetToInclusionTreenNode.get(parentModule);

                    // create parent if not yet present
                    if (parentTreeNode == null) {
                        parentTreeNode = new RootedTreeNode();
                        bitsetToInclusionTreenNode.put(parentModule, parentTreeNode);
                        log.fine("Vertex " + vertexNr + ": Created Parent " + parentModule);
                    }

                    // add parent if current treenode has no parent
                    if (currTreenode.isRoot()) {
                        parentTreeNode.addChild(currTreenode);
                        relationCount++;
                        log.fine("Vertex " + vertexNr + ": Rel " + relationCount + ": child " + currModule + " to parent " + parentModule);
                    }
                    // go one element back in list
                    vListIter.previous();
                }

            }

        }

        log.info("Inclusion Tree: " + ret.toString());

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

    RootedTree intersectPartitiveFamiliesOf(MDTree T_a, MDTree T_b) throws InterruptedException,IOException{

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

        // get an arbitrary Index for each of the Treenodes in order to process them later
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
        HashMap<BitSet, RootedTreeNode> bitsetToOverlapTreenNode = new HashMap<>(overlapComponents.size() * 4 / 3);
        RootedTree overlapInclusionTree = getInclusionTreeFromBitsets(overlapComponents.values(), bitsetToOverlapTreenNode);

        /*

        // Step 0: eleminate doubles
        HashSet<BitSet> overlapComponentsNoDoubles = new HashSet<>(overlapComponents.values());
        if (debugMode && overlapComponentsNoDoubles.size() != overlapComponents.size()) {
            log.warning("Overlap compenents were merged into an already existing compontent!");
        }
        ArrayList<BitSet> nontrivOverlapComponents = new ArrayList<>(overlapComponentsNoDoubles);

        // Step 1: Sort the array by size, using bucket sort
        // todo: Das brauche ich häufiger! Auch hier mit BucketSort...
        nontrivOverlapComponents.sort( // descending size, with lamdas.
                (b1, b2) -> Integer.compare(b2.cardinality(), b1.cardinality()));
        log.fine("Overlap components: " + nontrivOverlapComponents);


        // Step 2: Create a List for each v ∈ V of the members of F (i.e. the elements of σ) containing v in ascending order of their size:


        // init empty
        ArrayList<LinkedList<BitSet>> xLists = new ArrayList<>(nVertices);
        for (int i = 0; i < nVertices; i++) {
            xLists.add(i, new LinkedList<>());
        }

        // add root
        BitSet rootSet = new BitSet(nVertices);
        rootSet.set(0, nVertices); // toIndex must be n
        nontrivOverlapComponents.add(0, rootSet);

        // - visit each Y ∈ F in descending order of size. For each x ∈ Y, insert pointer to Y to front of x's list. [O(sz(F)]
        nontrivOverlapComponents.forEach(ySet ->
                ySet.stream().forEach(i ->
                        xLists.get(i).addFirst(ySet)
                )
        );
        // root is now the last element in every xList



        RootedTree rootedTree = new RootedTree();
        RootedTreeNode root = new RootedTreeNode();
        rootedTree.setRoot(root);
        HashMap<BitSet, RootedTreeNode> bitsetToOverlapTreenNode = new HashMap<>(nontrivOverlapComponents.size() * 4 / 3);
        bitsetToOverlapTreenNode.put(rootSet, root);

        // This creates the inclusion tree from the x's lists
        // - visit each x ∈ V, put a parent pointer from each member of x's list to its successor in x's list (if not already done)
        //      -> these are chains of ancestors of {x}

        PartitiveFamilyLeafNode[] allLeafs = new PartitiveFamilyLeafNode[nVertices];
        int relationCount = 0;
        for (int vertexNr = 0; vertexNr < nVertices; vertexNr++) {

            // retrieve List and create leafNode
            LinkedList<BitSet> currVertexList = xLists.get(vertexNr);
            PartitiveFamilyLeafNode leafNode = new PartitiveFamilyLeafNode(vertexNr, vertexForIndex[vertexNr]);

            boolean firstEntry = true;
            ListIterator<BitSet> vListIter = currVertexList.listIterator(0);
            while (vListIter.hasNext()) {

                BitSet currModule = vListIter.next();
                // create Trenode if not yet present
                RootedTreeNode currTreenode = bitsetToOverlapTreenNode.getOrDefault(currModule, null);
                if (currTreenode == null) {
                    currTreenode = new RootedTreeNode();
                    bitsetToOverlapTreenNode.put(currModule, currTreenode);
                }

                // the first entry of the list always gets the leaf attached.
                if (firstEntry) {
                    currTreenode.addChild(leafNode);
                    allLeafs[vertexNr] = leafNode;
                    firstEntry = false;
                }

                // add as child to the next element of xList
                // Note: currTreenode.equals(root) occurs when we're at the end of the list
                // Don't check for parent of root, of course.
                if (vListIter.hasNext()) {
                    BitSet parentModule = vListIter.next();
                    RootedTreeNode parentTreeNode = bitsetToOverlapTreenNode.get(parentModule);

                    // create parent if not yet present
                    if (parentTreeNode == null) {
                        parentTreeNode = new RootedTreeNode();
                        bitsetToOverlapTreenNode.put(parentModule, parentTreeNode);
                        log.fine("Vertex " + vertexNr + ": Created Parent " + parentModule);
                    }

                    // add parent if current treenode has no parent
                    if (currTreenode.isRoot()) {
                        parentTreeNode.addChild(currTreenode);
                        relationCount++;
                        log.fine("Vertex " + vertexNr + ": Rel " + relationCount + ": child " + currModule + " to parent " + parentModule);
                    }
                    // go one element back in list
                    vListIter.previous();
                }

            }

        }

        log.info("Inclusion Tree of overlap components: " + rootedTree.toString());
        */


        // 4.) Algorithm 1: compute Ü(T_a,T_b) = A* \cap B*;
        //     A* = {X | X ∈ σ(T_a, T_b) AND X node in T_a OR P_a not prime in T_a}, B analog
        //     and number its members according to the number pairs from P_a, P_b


        // or is that easier with BitSets??? A ⊂ B means e.g.
        // A = 0001 0010 1011
        // B = 1011 0011 1011
        // -> A  OR B == B. If A had elements not contained in B, the result wouldn't be B.
        //    note: this would yield true if A was empty.


        LinkedList<Integer> test2 = new LinkedList<>();
        Object test3;

        // Necessary for the equivalence classes
        HashMap<RootedTreeNode, RootedTreeNode> elementOfAToP_a = new HashMap<>();
        HashMap<RootedTreeNode, RootedTreeNode> elementOfBToP_b = new HashMap<>();

        HashMap<RootedTreeNode, BitSet> elementsOfA = computeNodesWithCompleteParent(
                bitsetToOverlapTreenNode, modulesAToBitset, true, elementOfAToP_a);
        HashMap<RootedTreeNode, BitSet> elementsOfB = computeNodesWithCompleteParent(
                bitsetToOverlapTreenNode, modulesBToBitset, false, elementOfBToP_b);


        // todo: start here

        /*
        // nodes of T_a: (same with T_b)
        HashSet<RootedTreeNode> innerNodes = new HashSet<>(bitsetToOverlapTreenNode.size() * 4 / 3); // internal
        HashMap<RootedTreeNode, BitSet> nodesOfT_aWithLeaves = new HashMap<>(); // this is internal only
        HashSet<RootedTreeNode> maximumMembers = new HashSet<>(); // internal



        // outer loop: iterate over all inner nodes of σ(T_a,T_b)
        // need to get the P_a for every node S in σ that is not directly in T_a -> lca and check if not prime.
        for (Map.Entry<BitSet, RootedTreeNode> setEntryOfSigma : bitsetToOverlapTreenNode.entrySet()) {

            // Init: Mark the leaf entries of T_a corresponding to the current set of σ(T_a,T_b)
            BitSet bits = setEntryOfSigma.getKey();
            for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
                leavesOfT_a[i].addMark(); // todo: for T_b, and can I re-use?
                maximumMembers.add(leavesOfT_a[i]);
                RootedTreeNode parent = leavesOfT_a[i].getParent();
                BitSet bitSet = modulesAToBitset.get(parent);
                // NodesWithLeaves setzen.
                nodesOfT_aWithLeaves.put(parent, bitSet); // todo: ok this way?
            }

            // Iterate bottom-up through T_a
            ///
            for (RootedTreeNode node : nodesOfT_aWithLeaves.keySet()) {
                if (node.getNumChildren() == node.getNumMarkedChildren()) { // todo: hier mal mit Lamdas filtern?
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
            /// todo: reuseable code!

            // Loop, until no parents are fully marked anymore
            boolean completed = false;
            while (!completed) {
                // holds the parents - to be processed in the next iteration
                HashSet<RootedTreeNode> tmpSet = new HashSet<>();
                ///
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
                ///

                completed = tmpSet.isEmpty();
                innerNodes = tmpSet;
            }
            // now, we have the maximal members of T_a marked that are subsets of an S ⊂ σ.
            // to determine if S \in A*, check if maximumMembers has only one entry OR their first shared parent is complete.


            // Step 2: Take the initialized inclusion tree of σ(T_a, T_b) and test its nodes for membership in A* and B*,
            // using Algorithm 1. This yields Ü(T_a,T_b).

            // todo: HashMap from max/LCa to BitSet of σ (union of them). This directly gives the R_U-Unions!
            if (maximumMembers.size() == 1) {
                elementsOfA.put(setEntryOfSigma.getKey(), setEntryOfSigma.getValue());
                log.fine("Added: " + setEntryOfSigma.toString() + " directly");
            } else if (maximumMembers.size() == 0) {
                log.warning("Strange: no max member for " + setEntryOfSigma.toString());
            } else {
                // compute the LCA of all maximum members and check if it is root. Note: one entry itself could be that.
                // LCA can be found in O(h), h height of the tree, at least.
                ArrayList<RootedTreeNode> maxMembersList = new ArrayList<>(maximumMembers);
                // todo: does that really work or do i need true MDTreeNodes -> getStrongModules there?
                MDTreeNode lca = (MDTreeNode) computeLCA(maxMembersList, log);
                if (lca.getType().isDegenerate()) {
                    elementsOfA.put(setEntryOfSigma.getKey(), setEntryOfSigma.getValue());
                }
                log.fine("Added: " + setEntryOfSigma.toString() + " after lca computation.");
            }

            // Cleaup. I need to unmark all nodes. Marked nodes are now only among children of the maximum Members!
            for (RootedTreeNode node : maximumMembers) {
                node.unmarkAllChildren();
            }
            nodesOfT_aWithLeaves.clear();
            maximumMembers.clear();
            innerNodes.clear();

        }

        */
        // todo: Gefährlich mit dem ganzen Klassen-casten. ggf noch eine Modules-List erstellen für T_a, T_b, um indizes zu haben.
        // end here


        // Reinitialize and Compute P_b with alg 1

        // IMPORTANT: before step to, I need to compute P_a and P_b to compute A* and B*!!!
        // unfortunately, no algorithm was given to compute P_a and P_b...
        // No Problem: I can directly compute A* and B*!!!

        // Ü(T_a,T_b) is now simply the intersection of A* and B*

        HashMap<RootedTreeNode, BitSet> intersectionOfAandB = new HashMap<>();
        //HashSet<RootedTreeNode> onlyVals = new HashSet<>(elementsOfB.values());
        for (Map.Entry<RootedTreeNode, BitSet> entry : elementsOfA.entrySet()) {
            RootedTreeNode node = entry.getKey();
            if (elementsOfB.containsKey(node)) {
                intersectionOfAandB.put(node, entry.getValue());
            }
        }

        // Now, the paper suggests computing P_a and P_b for each element X \in Ü.
        // However, this has already been done in the previous step. It is either:
        // - if there is only one maximum member: it's this node
        // - if there are several maximum members: it's the computed (necessarily nonprime) lca
        // so I simply need to keep a HashMap from X to the corresponding P_a and P_b :)


        // todo: don't need number pairs. Already have the eqiv classes.
        // no idea what these number pairs are. So let's just try to find out to get
        // the equivalence classes, which are also needed in step4!


        // then, the set \mathcal S(T_a, T_b) - which is the set Family of H's MD-Tree - is computed. Simply join the BitSets!
        // From that set Family, The Inclusion Tree can be constructed by Lem 11 - if needed. Maybe BitSet-Family is enough...
        // Assuming, BitSet-Family is enough for now
        // But: Cor 19 requires a permutation of the leaves -> which will yield a fact perm.


        //
        //
        // 5.) Bucket sort members of Ü(T_a,T_b) according to number pairs given by P_a, P_b
        //     to get equivalence classes of R_U (Lem 9)


        // 6.) union of each eq. class via boolean array (to get result from Th. 10)

        // now we have the Tree T(H) = T_a Λ T_b


        // Th. 15: T(F) =T_a Λ T_b in O(sz(S(F_a)) + sz(S(F_b))

        RootedTree ret = null;
        return ret;
    }

    /**
     * Let A* := {X | X ∈ σ(T_a, T_b) AND X node in T_a OR P_a not prime in T_a}, B* analog. // todo: wo ist hier die Klammer???
     * So these are the entries of σ(T_a,T_b) that also appear in T_a [T_b] or share a complete parent in T_a [T_b].
     *
     * This method use Alg 1 in order to get the desired result of Cor. 13: Testing each node
     * of σ(T_a,T_b) for membership in A* [and B*, respectively]. Then, their intersection is returned:
     *
     * Ü(T_a,T_b) = A* \cap B*;
     *
     * @param bitsetToOverlapTreenNode the entries of σ(T_a,T_b)
     * @param modulesAToBitset         the modules of the MDTree T_a [T_b, respectively]
     */
    private HashMap<RootedTreeNode, BitSet> computeNodesWithCompleteParent(Map<BitSet, RootedTreeNode> bitsetToOverlapTreenNode,
                                                                           Map<RootedTreeNode, BitSet> modulesAToBitset, boolean isA,
                                                                           Map<RootedTreeNode, RootedTreeNode> elementOfAToP_a) {

        // todo: note - usually only use the nodes not Bitsets: Except for the leavesOf...
        //

        // Compute P_a(S) for every S \in σ with alg 1.
        // "P_a(S) is the smallest node of T_a that contains S as proper subset"
        //  -> I'll compute A* and B* directly and save the LCAs for later.
        HashMap<RootedTreeNode, BitSet> elementsOfA = new HashMap<>();

        // nodes of T_a:
        HashSet<RootedTreeNode> innerNodes = new HashSet<>(bitsetToOverlapTreenNode.size() * 4 / 3);
        HashMap<RootedTreeNode, BitSet> nodesOfT_aWithLeaves = new HashMap<>();
        HashSet<RootedTreeNode> maximumMembers = new HashSet<>();
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

            // Init: Mark the leaf entries of T_a corresponding to the current set of σ(T_a,T_b)
            BitSet bits = setEntryOfSigma.getKey();
            for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
                leaves[i].addMark();
                maximumMembers.add(leaves[i]);
                RootedTreeNode parent = leaves[i].getParent();
                BitSet bitSet = modulesAToBitset.get(parent);
                // NodesWithLeaves setzen.
                nodesOfT_aWithLeaves.put(parent, bitSet);
            }

            // Iterate bottom-up through T_a
            ///
            for (RootedTreeNode node : nodesOfT_aWithLeaves.keySet()) {
                if (node.getNumChildren() == node.getNumMarkedChildren()) { // todo: hier mal mit Lamdas filtern?
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
            /// todo: reuseable code!

            // Loop, until no parents are fully marked anymore
            boolean completed = false;
            RootedTreeNode currLCA = null;
            while (!completed) {
                // holds the parents - to be processed in the next iteration
                HashSet<RootedTreeNode> tmpSet = new HashSet<>();
                ///
                for (RootedTreeNode node : innerNodes) {
                    if (node.getNumMarkedChildren() == node.getNumChildren()) {
                        node.unmarkAllChildren();
                        node.mark();
                        tmpSet.add(node.getParent());
                        // update maximum members:
                        currLCA = node.getParent();
                        maximumMembers.add(currLCA);
                        RootedTreeNode child = node.getFirstChild();
                        while (child != null) {
                            maximumMembers.remove(child); // todo: does this work?
                            child = child.getRightSibling();
                        }
                    }
                }
                ///

                completed = tmpSet.isEmpty();
                innerNodes = tmpSet;
            }
            // now, we have the maximal members of T_a marked that are subsets of an S ⊂ σ.
            // to determine if S \in A*, check if maximumMembers has only one entry OR their first shared parent is complete.


            // Step 2: Take the initialized inclusion tree of σ(T_a, T_b) and test its nodes for membership in A* and B*
            // Saving a HashMap from element of σ to P_a allows us to compute the R_U-equivalence classes easily.

            if (maximumMembers.size() == 1) {
                elementsOfA.put(setEntryOfSigma.getValue(), setEntryOfSigma.getKey());
                elementOfAToP_a.put(setEntryOfSigma.getValue(), currLCA);
                log.fine(logPrefix + "Added: " + setEntryOfSigma.toString() + " directly");
            } else if (maximumMembers.size() == 0) {
                log.warning(logPrefix + "Strange: no max member for " + setEntryOfSigma.toString());
            } else {
                // compute the LCA of all maximum members and check if it is root. Note: one entry itself could be that.
                // LCA can be found in O(h), h height of the tree, at least.
                MDTreeNode lca = (MDTreeNode) computeLCA(maximumMembers, log);
                if (lca.getType().isDegenerate()) {
                    elementsOfA.put(setEntryOfSigma.getValue(), setEntryOfSigma.getKey());
                    elementOfAToP_a.put(setEntryOfSigma.getValue(), lca);
                    log.fine(logPrefix + "Added: " + setEntryOfSigma.toString() + " with complete LCA: " + lca.toString());
                } else {
                    log.fine(logPrefix + "Discarded: " + setEntryOfSigma.toString() + " with prime LCA: " + lca.toString());
                }
            }

            // Cleaup. I need to unmark all nodes. Marked nodes are now only among children of the maximum Members! todo: verify!
            for (RootedTreeNode node : maximumMembers) {
                node.unmarkAllChildren();
            }
            nodesOfT_aWithLeaves.clear();
            maximumMembers.clear();
            innerNodes.clear();

        }

        return elementsOfA;
    }

}
