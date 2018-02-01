package dicograph.modDecomp;

import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.io.ImportException;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import dicograph.utils.SortAndCompare;
import dicograph.utils.TimerLog;
/*
 *   This source file is part of the program for editing directed graphs
 *   into cographs using modular decomposition.
 *   Copyright (C) 2018 Fynn Leitow
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Created by Fynn Leitow on 11.10.17.
 */
public class DirectedMD {

    private final static int overlapCodeBufferLimit = 3500; // max n for size of text buffer in the C code (16384)
    private final static String overlapTransferFile = "OverlapComponentProg/doNotDelete.txt";

    final SimpleDirectedGraph<Integer, DefaultEdge> inputGraph;
    final Logger log;
    private final TimerLog timeLog;
    final int nVertices;
    private SimpleGraph<Integer, DefaultEdge> G_s;
    private SimpleGraph<Integer, DefaultEdge> G_d;

    private final boolean debugMode; // false for max speed, true for nicely sorted vertices etc.


    public DirectedMD(SimpleDirectedGraph<Integer, DefaultEdge> input, Logger logger, boolean debugMode){

        inputGraph = input;
        log = logger;
        timeLog = new TimerLog(log, Level.FINER);
        nVertices = input.vertexSet().size();
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
    private static ArrayList<Integer> dahlhausProcessDelegator(Logger log)
            throws IOException {
        List<String> command = new ArrayList<>();
        command.add("./OverlapComponentProg/main"); // ./OverlapComponentProg/main
        command.add(overlapTransferFile);
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
            log.finer(nextLine);
            count++;
            if (count == 7) {
                for (String res : nextLine.split(" ")) {
                    ret.add(Integer.valueOf(res));
                }
            }
        }
        log.finer("Dahlhaus algorithm finished");
        return ret;
    }

    public MDTree computeModularDecomposition() throws InterruptedException, IOException, ImportException {

        log.finer("init md of graph: " + inputGraph.toString());


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
        log.finer("  G_d of digraph: " + G_d);

        // G_s: undirected graph s.t. {u,v} in E_s iff (u,v) in E or (v,u) in E
        G_s = new SimpleGraph<>(DefaultEdge.class);
        inputGraph.vertexSet().forEach( G_s::addVertex );
        for (DefaultEdge edge : inputGraph.edgeSet()) {
            int source = inputGraph.getEdgeSource(edge);
            int target = inputGraph.getEdgeTarget(edge);
            if (!G_s.containsEdge(source, target)) {
                G_s.addEdge(source, target);
            }
        }

        log.finer("  G_s of digraph: " + G_s);


        // H: symmetric 2-structure with
        //    E_H(u,v) = 0 if {u,v} non-edge (i.e. non-edge in both G_s and G_d)
        //    E_H(u,v) = 1 if {u,v} edge (i.e. edge in both G_s and G_d)
        //    E_H(u,v) = 2 if (u,v) or (v,u) simple arc (i.e. edge in G_s but not G_d)

        timeLog.logTime("Init of G_d and G_s");
        log.finer("computing md for G_d:");

        // Step 2: T(G_d) and T(G_s) with algorithm for undirected graphs

        // without null used Tedder's MD
        MDTree treeForG_d = new MDTree(G_d, null, debugMode, log);
//        if(treeForG_d.removeDummies()){
//            log.warning("Removed dummy primes for G_d");
//        }
        log.finer("computing md for G_s:");

        MDTree treeForG_s = new MDTree(G_s, null, debugMode, log);
//        if(treeForG_s.removeDummies()){
//            log.warning("Removed dummy primes for G_s");
//        }
        timeLog.logTime("MD for G_d and G_s");
        log.finer("md for G_d:\n" + MDTree.beautify(treeForG_d.toString()));
        //log.finer("DOT for G_d:\n" + treeForG_d.exportAsDot());
        log.finer("md for G_s:\n" + MDTree.beautify(treeForG_s.toString()));
        //log.finer("DOT for G_s:\n" + treeForG_s.exportAsDot());

        // Step 3: Find T(H) = T(G_s) Λ T(G_d)

        PartitiveFamilyTree treeForH = intersectPartitiveFamiliesOf(treeForG_s, treeForG_d);

        // I guess I should determine the node-type. Note: Due to possibly merged modules, this is
        // not yet the "true" node-type, just a reference to 0/1/2-completeness.
        treeForH.computeAllNodeTypes(this);
        timeLog.logTime("End of step 3 - Inclusion Tree");
        log.finer("Inclusion Tree with computed types: " + MDTree.beautify(treeForH.toString()));


        // Step 4: At each O-complete and 1-complete node X of T(H), order the children s.t.
        //         each equivalence class of R_X is consecutive.
        // AND
        // Step 5: At each  2-complete node Y, select an arbitrary set S of representatives from the children.
        //         order the children of Y according to a perfect factorizing permutation of G[S].
        treeForH.computeFactorizingPermutationAndReorderAccordingly(this, true );
        timeLog.logTime("Fact. Permutation");


        // Resulting leaf order of T(H) is a factorizing permutation of G by Lem 20,21. Use algorithm
        //         [2] to find the modular decomposition of G.
        ArrayList<PartitiveFamilyLeafNode> trueLeafOrder =  new ArrayList<>(nVertices);
        treeForH.getLeavesInLeftToRightOrder( trueLeafOrder );

        StringBuilder leafNumbers = new StringBuilder();
        for(int i = 0; i < trueLeafOrder.size(); i++){
            PartitiveFamilyLeafNode l = trueLeafOrder.get(i);
            leafNumbers.append(l.getVertex());
            if(i != nVertices-1)
                leafNumbers.append(", ");
        }
        log.finer(() ->"Leaves ordered as factorizing permutation: " + leafNumbers);
        log.finer("Reordered Tree: " + MDTree.beautify(treeForH.toString()));


        // get the MD Tree from C++
        MDTree finalTree = new MDTree(inputGraph, leafNumbers.toString(), true, log);
        finalTree.getStrongModulesBool(nVertices);


        // Step 6 b): Deletion of weak modules and recovering of merged modules - should happen in C++
//        if(finalTree.removeDummies()){
//            log.warning("Removed dummy primes/ weak orders!");
//        }
        timeLog.logTime("MD Tree from FP");
        log.finer("Final Tree: " + MDTree.beautify(finalTree.toString()));


        if (debugMode) {
            String msg = finalTree.verifyNodeTypes(inputGraph, true);

            if (!msg.isEmpty()) {
                msg = "Error in modules of G:\n" + msg;
                throw new IllegalStateException(msg);
            }
        }

        return finalTree;

    }


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


    private PartitiveFamilyTree intersectPartitiveFamiliesOf(MDTree of_Gs_T_s, MDTree of_Gd_T_g) throws InterruptedException,IOException{

        // Notes from section 2:

        //  Th.5: modules of an undirected graph form a strongly partitive family
        //  Th.3: internal node X of str.part.fam. F is either
        //       complete: Any union of children is in F
        //       prime:     no         -||-
        //  T(F) is the inclusion tree (Hasse-Diagram) of F.

        // processing section 3:

        // F = F_a \cap F_b is family of sets which are members in both F_a and F_b.

        // 0.) get the sets from the tree and compute their union.

        // Not a problem according to Th 22. Just retrieve the vertices unsorted as arraylist -> |M| < 2m + 3n
        // 1. bucket sort the array lists by size (bound met)
        // 2. bucket sort the array lists of same size;
        //    2.a) sort them at the same time and keep an "equals" boolean-flag OR
        //    2.b) iterate again, setting checking for equality
        // -> Create the String at the same time.
        // might be better than my current approach with BitSets of size n :)

        HashMap<BitSet, RootedTreeNode> strongModulesBoolT_s = of_Gs_T_s.getStrongModulesBool(nVertices);
        HashMap<BitSet, RootedTreeNode> strongModulesBoolT_d = of_Gd_T_g.getStrongModulesBool(nVertices);


        // debug option: verify if the modules are correct (kills linearity).
        if(debugMode){
            String debug_T_s = of_Gs_T_s.verifyNodeTypes(G_s, false);
            String debug_T_g = of_Gd_T_g.verifyNodeTypes(G_d, false);
            String msg = "";
            if(!debug_T_s.isEmpty()) {
                msg = "Error in modules of G_s:\n" + debug_T_s;
            }

            if(!debug_T_g.isEmpty()) {
                msg += "\n\nError in modules of G_d:\n" + debug_T_g;
            }
            if(!msg.isEmpty()){
                log.severe(msg);
                throw new IllegalStateException(msg);
            }
        }

        // union, no doubles.
        HashSet<BitSet> nontrivModulesTemp = new HashSet<>(strongModulesBoolT_s.keySet());
        nontrivModulesTemp.addAll(strongModulesBoolT_d.keySet());

        // Sets need to be ordered for the algorithm
        ArrayList<BitSet> allNontrivModules= new ArrayList<>(nontrivModulesTemp);
        SortAndCompare.bucketSortBySize(allNontrivModules,false);

        StringBuilder overlapInput = new StringBuilder();

        // Since the singletons and V itself will never overlap another module, I can exclude them here and only consider the nontrivial modules
        // The Program takes vertices separated by " " in one line, ended by a "-1".
        allNontrivModules.forEach(module -> {
            module.stream().forEach(vertexNo ->
                    overlapInput.append(vertexNo).append(" "));
            overlapInput.append("-1\n");
        });
        timeLog.logTime("Init step 3");


        // 1.) use M.Rao's Dahlhaus algorithm to compute the overlap components (Bound: |M| <= 4m + 6n)
        log.finer("Input for Dahlhaus algorith:\n" + overlapInput);
        File dahlhausFile = new File(overlapTransferFile);

        // Try with ressources
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dahlhausFile))){
            //System.out.println(dahlhausFile.getCanonicalPath());
            writer.write(overlapInput.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // I need to make sure that the program breaks if the char-Buffer would overflow // todo: edit C code to get rid of this.
        if (nVertices > overlapCodeBufferLimit)
            throw new IndexOutOfBoundsException("Error: adapt the size of the char buff[" + 16384 +"] in OverlapComponentProg/main.c and recompile.");

        ArrayList<Integer> overlapComponentNumbers = dahlhausProcessDelegator(log);

        // Moooment. Die Knoten im Overlap-Graph sind:
        // 1. Die Singleton-Subsets
        // 2. V
        // 3. Die oben berechneten Module

        // Retrieves the overlap components and computes UNION for same numbers
        HashMap<Integer, BitSet> overlapComponents = new HashMap<>();
        for(int i = 0; i< overlapComponentNumbers.size(); i++){

            int componentNr = overlapComponentNumbers.get(i);
            if (overlapComponents.containsKey(componentNr)) {
                overlapComponents.get(componentNr).or(allNontrivModules.get(i));
            } else {
                overlapComponents.put(componentNr, allNontrivModules.get(i));
            }
        }
        timeLog.logTime("Overlap components");

        // What exacty _are_ the overlap Components now? number -> module
        // what do I want: number -> set with all vertices. therefore:
        //   I. Put them all in one ArrayList
        //  II. unionSort the ArrayList
        // currently with BitSets

        // 2.) use booleanArray to compute σ(T_s,T_g) = the union of the overlap components in O(V).
        // not even necessary - σ is obtained by simply joining the components, no need to check for equality

        // According to paper, V and the singleton subsets must be in σ(T_s, T_g) = { U ς | ς is overlap components of S(T_s) \cup S(T_g)
        // Excluded them as they Don't contribute to computing the overlap components. Added in the next step.


        // Assuming now: ArrayList of ArrayList<Int> with the singletons, V and the overlapComponents
        // currently: BitSets

        // 3.) @Lemma 11: compute the inclusion tree of σ(T_s, T_g)
        // The previously ommitted V and the singleton sets are also added to the tree.
        PartitiveFamilyTree overlapInclusionTree = new PartitiveFamilyTree();
        PartitiveFamilyLeafNode[] leafNodes = overlapInclusionTree.createInclusionTreeFromBitsets(overlapComponents.values(),log,nVertices);
        timeLog.logTime("Overlap inclusion tree");
        log.finer(() -> MDTree.beautify(overlapInclusionTree.toString()));
        HashMap<BitSet, RootedTreeNode> bitsetToOverlapTreenNode = overlapInclusionTree.getModuleToTreenode();

        // 4.) Algorithm 1: compute Ü(T_s,T_g) = A* \cap B*;
        //     A* = {X | X ∈ σ(T_s, T_g) AND X node in T_s OR P_a not prime in T_s}, B analog
        //     and number its members according to the number pairs from P_a, P_b

        // Necessary for the equivalence classes
        HashMap<RootedTreeNode, RootedTreeNode> elementOfAToP_a = new HashMap<>();
        HashMap<RootedTreeNode, RootedTreeNode> elementOfBToP_b = new HashMap<>();


        log.finer("Computing nodes with complete Parent for Tree T_s of G_s");
        HashMap<RootedTreeNode, BitSet> elementsOfA = computeNodesWithCompleteParent(bitsetToOverlapTreenNode,
                true, leafNodes, elementOfAToP_a, strongModulesBoolT_s, of_Gs_T_s);

        // Reinitialize and Compute P_b
        log.finer("Computing nodes with complete Parent for Tree T_g of G_d");
        HashMap<RootedTreeNode, BitSet> elementsOfB = computeNodesWithCompleteParent(bitsetToOverlapTreenNode,
                false, leafNodes,elementOfBToP_b, strongModulesBoolT_d, of_Gd_T_g);


        // Ü(T_s,T_g) is now simply the intersection of A* and B*
        HashMap<RootedTreeNode, BitSet> intersectionOfAandB = new HashMap<>();
        //HashSet<RootedTreeNode> onlyVals = new HashSet<>(elementsOfB.values());
        for (Map.Entry<RootedTreeNode, BitSet> entry : elementsOfA.entrySet()) {
            RootedTreeNode node = entry.getKey();
            if (elementsOfB.containsKey(node)) {
                intersectionOfAandB.put(node, entry.getValue());
            }
        }
        log.finer("Intersection of A* and B*: " + intersectionOfAandB.values());

        // Now, the paper suggests computing P_a and P_b for each element X \in Ü.
        // However, this has already been done in the previous step. It is either:
        // - if there is only one maximum member: it's this node
        // - if there are several maximum members: it's the computed (necessarily nonprime) lca

        // 5.) Bucket sort members of Ü(T_s,T_g) according to number pairs given by P_a, P_b
        //     to get equivalence classes of R_U (Lem 9)
        //     -> I use a HashMap instead of Bucketsorting by the sortKey.
        //     union of each eq. class via boolean array (BitSet) to get result from Th. 10


        // Compute the equivalence classes.
        HashMap<Pair<RootedTreeNode, RootedTreeNode>, BitSet> equivalenceClassesR_U = new HashMap<>((elementOfAToP_a.size() + elementOfBToP_b.size()) * 2 / 3);

        for( Map.Entry<RootedTreeNode, BitSet> entry : intersectionOfAandB.entrySet()){

            RootedTreeNode aElement = elementOfAToP_a.get(entry.getKey());
            RootedTreeNode bElement = elementOfBToP_b.get(entry.getKey());

            // must be contained in both maps!
            if (aElement != null && bElement != null) {
                // Pair is fine, hashCode isn't overridden.
                Pair<RootedTreeNode, RootedTreeNode> sortKey = new Pair<>(aElement, bElement);
                if (equivalenceClassesR_U.containsKey(sortKey)) {
                    log.finer(() -> "Adding " + entry.getValue() + " to equivalence class " + sortKey);
                    equivalenceClassesR_U.get(sortKey).or(entry.getValue());
                } else {
                    log.finer(() -> "For " + entry.getValue() + ", create new equivalence class " + sortKey);
                    equivalenceClassesR_U.put(sortKey, (BitSet) entry.getValue().clone()); // need clone, else chaos
                }
            }

        }
        timeLog.logTime("Equivalence Classes");
        log.finer("Equivalence Classes: " + equivalenceClassesR_U.values());


        // 6. ) the set \mathcal S(T_s, T_g) - which is the set Family of H's MD-Tree:
        // (not quite, might also contain singletons. Filtered in 7.)
        ArrayList<BitSet> strongModulesOfH = new ArrayList<>(
                equivalenceClassesR_U.size() + intersectionOfAandB.size());
        strongModulesOfH.addAll(equivalenceClassesR_U.values());
        strongModulesOfH.addAll(intersectionOfAandB.values());

        // 7. ) From that set Family, The Inclusion Tree can be constructed by Lem 11.

        PartitiveFamilyTree ret = new PartitiveFamilyTree();
        ret.createInclusionTreeFromBitsets(strongModulesOfH, log, nVertices);
        // now we have the Tree T(H) = T_s Λ T_g which is equal to the MD Tree, exept for weak and merged modules.

        return ret;
    }

    /**
     * Let A* := {X | X ∈ σ(T_s, T_g) AND X node in T_s OR P_a not prime in T_s}, B* analog.
     * So these are the entries of σ(T_s,T_g) that also appear in T_s [T_g] or share a complete parent in T_s [T_g].
     *
     * This method use Alg 1 in order to get the desired result of Cor. 13: Testing each node
     * of σ(T_s,T_g) for membership in A* [and B*, respectively]. Then, their intersection is returned:
     *
     * Ü(T_s,T_g) = A* \cap B*;
     *
     * @param bitsetToOverlapTreenNode the entries of σ(T_s,T_g)
     */
    private HashMap<RootedTreeNode, BitSet> computeNodesWithCompleteParent(Map<BitSet, RootedTreeNode> bitsetToOverlapTreenNode, boolean isT_s, PartitiveFamilyLeafNode[] leavesOfOverlapTree,
                                                                           Map<RootedTreeNode, RootedTreeNode> elementOfAToP_a, Map<BitSet, RootedTreeNode> strongModules, MDTree mdTree) {

        // note: usually only using the nodes not Bitsets: Except for the leavesOf...

        // Compute P_a(S) for every S \in σ with alg 1.
        // "P_a(S) is the smallest node of T_s that contains S as proper subset"
        //  -> I'll compute A* and B* directly and save the LCAs for later.
        HashMap<RootedTreeNode, BitSet> elementsOfA = new HashMap<>();

        // nodes of T_s:
        MDTreeLeafNode[] mdTreeLeaves = mdTree.getLeaves();
        String logPrefix;
        if (isT_s) {
            logPrefix = "G_s: ";
        } else {
            logPrefix = "G_d: ";
        }

        // outer loop: iterate over all inner nodes of σ(T_s,T_g)
        // need to get the P_a for every node S in σ that is not directly in T_s -> lca and check if not prime.
        for (Map.Entry<BitSet, RootedTreeNode> setEntryOfSigma : bitsetToOverlapTreenNode.entrySet()) {

            BitSet bits = setEntryOfSigma.getKey();

            // check if already a node of T_s or T_g -> done.
            MDTreeNode easyNode = (MDTreeNode) strongModules.get(bits);
            if (easyNode != null) {
                log.finer(() -> logPrefix + "Added: " + setEntryOfSigma);
                log.finer(() -> "   as it is node in MD Tree.");
                elementsOfA.put(setEntryOfSigma.getValue(), bits);
                // if not a prime, possibly also in equiv class:
                if (easyNode.getType().isDegenerate()) {
                    elementOfAToP_a.put(setEntryOfSigma.getValue(), easyNode);
                }
            } else {

                // Step 1: Initialize the inclusion tree, i.e. each node must have:
                //         - a parent pointer (ok)
                //         - a list of pointers to its children (firstChild, then iterate rightSibling)
                //         - record of how many children it has (numChildren)
                //         - an initialized field for marking (marked)
                //         - how many children are marked (numChildrenMarked)

                HashSet<RootedTreeNode> innerNodes = new HashSet<>(bits.cardinality() * 4 / 3);
                HashSet<RootedTreeNode> maximumMembers = new HashSet<>();

                // Init: Mark the leaf entries of T_s corresponding to the current set of σ(T_s,T_g)
                for (int v = bits.nextSetBit(0); v >= 0; v = bits.nextSetBit(v + 1)) {
                    mdTreeLeaves[v].addMark();
                    maximumMembers.add(mdTreeLeaves[v]);
                    RootedTreeNode parent = mdTreeLeaves[v].getParent();
                    innerNodes.add(parent);
                }

                // Iterate bottom-up through T_s and loop, until no parents are fully marked anymore
                boolean completed = false;
                while (!completed) {
                    // holds the parents - to be processed in the next iteration
                    HashSet<RootedTreeNode> tmpSet = new HashSet<>(innerNodes.size());

                    innerNodes.stream().filter(tnode -> tnode.getNumChildren() == tnode.getNumMarkedChildren()).forEach(node -> {
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
                // now, we have the maximal members of T_s marked that are subsets of an S ⊂ σ.
                // to determine if S \in A*, check if maximumMembers has only one entry OR their first shared parent is complete.


                // Step 2: Take the initialized inclusion tree of σ(T_s, T_g) and test its nodes for membership in A* and B*
                // Saving a HashMap from element of σ to P_a allows us to compute the R_U-equivalence classes.

                if(maximumMembers.size() < 2){ // doesn't happen.
                    throw new IllegalStateException("Number of max. members: " + maximumMembers.size() + ", entries: " + maximumMembers);
                } else {
                    // compute the LCA of all maximum members and check if it is complete.
                    // LCA can be found in O(h), h height of the tree, at least.
                    MDTreeNode lca;
                    if(maximumMembers.size() == nVertices){
                        lca = (MDTreeNode) mdTree.root;
                    } else {
                        lca = (MDTreeNode) mdTree.getLCA(new ArrayList<>(maximumMembers));
                    }
                    if (lca == null || lca.hasNoChildren()) {
                        throw new IllegalStateException("LCA: " + lca + " invalid!\nFor: " + setEntryOfSigma);
                    } else if (lca.getType().isDegenerate()) {

                        elementsOfA.put(setEntryOfSigma.getValue(), setEntryOfSigma.getKey());
                        elementOfAToP_a.put(setEntryOfSigma.getValue(), lca);
                        log.finer(() -> logPrefix + "Added: " + setEntryOfSigma);
                        log.finer(() -> "   with complete LCA: " + lca);
                    } else {
                        log.finer(() -> logPrefix + "Discarded: " + setEntryOfSigma);
                        log.finer(() -> "   with prime LCA: " + lca);
                        // neither in P_a: "...and neither of these nodes is prime".
                    }
                }

                // Cleaup. I need to unmark all nodes. Marked nodes are now only among children of the maximum Members!
                for (RootedTreeNode node : maximumMembers) {
                    node.unmarkAllChildren();
                }
            }
            // Handling of the single vertices I skipped during overlap computation
            for(PartitiveFamilyLeafNode overlapLeaf : leavesOfOverlapTree){
                int vertexNo = overlapLeaf.getVertex();
                MDTreeLeafNode leafOfMDTree = mdTreeLeaves[vertexNo];
                MDTreeNode mdParent = (MDTreeNode) leafOfMDTree.getParent();
                //For the following equivalence class computation, add those complete parent node in the MD Tree
                if(mdParent.getType().isDegenerate()){
                    BitSet leafBit = new BitSet();
                    leafBit.set(vertexNo);
                    elementOfAToP_a.put(overlapLeaf,mdParent);
                    elementsOfA.put(overlapLeaf,leafBit);
                }
            }

        }

        return elementsOfA;
    }

    /**
     * Attempt to fix small missing modules that are directly attached to the root of the undirected MD trees.
     * Adds every subset of direct leaves of size >= 2.
     * @param moduleToTreeNode
     * @param t
     */
    private static void addCompleteRootSubsets(HashMap<BitSet,RootedTreeNode> moduleToTreeNode, MDTree t){
        MDTreeNode rootNode = (MDTreeNode) t.root;
        List<Integer> directLeafNumbers = rootNode.getDirectLeaves().stream()
                .map( l -> (  (MDTreeLeafNode) l).getVertexNo() )
                .collect(Collectors.toList());
        List<List<Integer>> allSubsets = SortAndCompare.computeAllSubsets(directLeafNumbers).stream()
                .filter( l -> l.size() > 1 ).collect( Collectors.toList() );
        for( List<Integer> myList : allSubsets){
            BitSet subSet = new BitSet();
            for( int v : myList){
                subSet.set(v);
            }
            moduleToTreeNode.putIfAbsent(subSet, rootNode);
        }
    }

}
