package dicograph.modDecomp;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.io.Attribute;
import org.jgrapht.io.DOTImporter;
import org.jgrapht.io.ImportException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/*
 * A modular decomposition tree of a simple, undirected graph.
 */
public class MDTree extends RootedTree {
		
	/*
	 * Creates the modular decomposition tree for the supplied graph.
	 */
	@Deprecated
	protected MDTree(GraphHandle graphHandle) {
		super();
		setRoot(buildMDTree(graphHandle));
	}

	public MDTree(SimpleGraph<Integer,DefaultEdge> jGraph) {
		super();
		setRoot( buildMDTree(jGraph) );
	}

	protected MDTree(){
	    super();
    }
    /*
     * Builds the modular decomposition tree for the supplied graph.
     * @return The root of the constructed modular decomposition tree.
     */
    @Deprecated
    private MDTreeNode buildMDTree(GraphHandle g) {

        if (g.getNumVertices() == 0) { return null; }

        RecSubProblem entireProblem = new RecSubProblem(g);

        MDTreeNode root = entireProblem.solve();
        root.clearVisited();
        return root;
    }

    // F.L. 27.09.17:

    public HashMap<BitSet, RootedTreeNode> getStrongModulesBool(MDTreeLeafNode[] leaves) {
        HashMap<BitSet, RootedTreeNode> ret = new HashMap<>();
        MDTreeNode rootAsMd = (MDTreeNode) root;
        rootAsMd.getStrongModulesBool(leaves, ret);
        moduleToTreenode = ret;

        return ret;
    }




    /**
     * F.L. Makes the String representation of MDTree human-readable.
     * @param mdTree the MDTRee
     * @return the String, formatted according to the tree structure
     */
    public static String beautify(String mdTree){
        StringBuilder ret = new StringBuilder();
        int offsetMultiplier = 0;
        char previous = '(';
        for(char c : mdTree.toCharArray()){
            if(c == '('){
                ret.append("\n");
                addOffset(ret,offsetMultiplier);
                ret.append(c);
                offsetMultiplier++;
            } else if (c == ')'){
                offsetMultiplier--;
                // new line only for modules, not for vertices.
                if(previous == ')') {
                    ret.append("\n");
                    addOffset(ret,offsetMultiplier);
                }
                ret.append(c);
            } else {
                ret.append(c);
            }
            previous = c;
        }


        return ret.toString();

    }

    // F.L.
    private static StringBuilder addOffset(StringBuilder builder, int times){
        String offset = "  ";
        for(int i = 0; i< times; i++){
            builder.append(offset);
        }
        return builder;
    }



	// F.L. new: use JGraph
	private MDTreeNode buildMDTree(SimpleGraph<Integer, DefaultEdge> g) {

		if (g.vertexSet().isEmpty()){
			return null;
		}

		RecSubProblem entireProblem = new RecSubProblem(g);

		MDTreeNode root = entireProblem.solve();
		root.clearVisited();
		return root;
	}

	// F.L.
    private static Reader readMDAsDot(Graph<Integer, DefaultEdge> inputGraph, String factPerm) throws IOException {

        // I might need two separate classes, anyways...
        boolean undirectedMD = factPerm == null || factPerm.isEmpty();
        if (undirectedMD)
            assert inputGraph instanceof SimpleGraph : "Wrong Graph type provided: " + inputGraph;
        else
            assert inputGraph instanceof SimpleDirectedGraph : "Wrong Graph type provided: " + inputGraph;

        List<String> command = new ArrayList<>();
//        String input = "([0, 1, 2, 3, 4, 5, 6, 7, 8, 9], [{0,4}, {0,5}, {0,6}, {0,7}, {0,8}, {0,9}, {1,0}, {1,4}, {1,5}, {1,6}, {1,7}, {1,8}, {2,3}, {2,4}, {2,5}, {2,6}, {2,7}, {2,8}, {2,9}, {3," +
//                "5}, {3,6}, {3,7}, {3,8}, {3,9}, {4,5}, {4,7}, {4,8}, {4,9}, {5,6}, {5,7}, {5,8}, {5,9}, {6,4}, {7,6}, {7,8}, {7,9}, {8,6}, {8,9}, {9,6}])";
        if(undirectedMD) {
            command.add("./MD/build/mod_dec");
            command.add(inputGraph.toString());
        } else {
            command.add("./FP_to_DMD/factPermToMD");
            command.add(inputGraph.toString());
            command.add(factPerm);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment();

        Process process = processBuilder.start();
        InputStream inputStream = process.getInputStream();
        return new InputStreamReader(inputStream);
    }

    // F.L.
    public MDTree(Graph<Integer, DefaultEdge> inputGraph, String factPerm) throws IOException, ImportException {
        this(inputGraph, factPerm, false, null);
    }

    public MDTree(Graph<Integer, DefaultEdge> inputGraph, String factPerm, boolean debug, Logger log) throws IOException, ImportException {
        super();
        Reader reader = readMDAsDot(inputGraph, factPerm);

        if (debug) {
            BufferedReader br = new BufferedReader(reader);
            StringBuilder builder = new StringBuilder();
            String next;
            boolean start = factPerm == null || factPerm.isEmpty();
            while ((next = br.readLine()) != null) {
                if(start)
                    builder.append(next).append("\n");
                else
                    start = next.startsWith("MD Tree:");
            }
            String res = builder.toString();
            log.fine("Passed .dot-file:\n" + res);
            reader = new StringReader(res);
        }

        readFromDot(reader);
    }


    // F.L. 22.11.17: removing dummy primes from adrians MD
    public boolean removeDummies() {
        MDTreeNode rootNode = (MDTreeNode) root;
        if (rootNode.getType() == MDNodeType.PRIME && rootNode.getNumChildren() == 1) {
            RootedTreeNode newRoot = rootNode.getFirstChild();
            newRoot.setParent(null);
            root = newRoot;
            rootNode = (MDTreeNode) root;
            rootNode.removeDummies();
            return true;
        } else {
            return rootNode.removeDummies();
        }
    }

    // F.L.
    private void readFromDot(Reader dotReader) throws IOException, ImportException {

        SimpleDirectedGraph< Integer ,DefaultEdge> treeGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
        HashMap<Integer, MDTreeNode> noToTreenode = new HashMap<>();

        DOTImporter<Integer,DefaultEdge> importer = new DOTImporter<>(
                (String label, Map<String, Attribute> attributes) -> {
                    int no = Integer.valueOf(label);
                    String content = attributes.get("label").getValue();
                    MDTreeNode node;
                    switch (content) {
                        case "Parallel":
                            node = new MDTreeNode(MDNodeType.PARALLEL);
                            break;
                        case "Prime":
                            node = new MDTreeNode();
                            break;
                        case "Spider":
                            throw new RuntimeException("Error: Didn't expect Spider nodes.");
                            //node = new MDTreeNode();
                            //break;
                        case "Series":
                            node = new MDTreeNode(MDNodeType.SERIES);
                            break;
                        case "Order":
                            node = new MDTreeNode(MDNodeType.ORDER);
                            break;
                        default:
                            int vertexNo = Integer.valueOf(content);
                            node = new MDTreeLeafNode(vertexNo);
                            break;
                    }
                    noToTreenode.put(no, node);
                    return no;
                }
                ,
                (from, to, label, attributes) -> {
                    MDTreeNode parent = noToTreenode.get(from);
                    MDTreeNode child = noToTreenode.get(to);
                    parent.addChild(child);
                    return treeGraph.addEdge(from, to);
                });

        importer.importGraph(treeGraph, dotReader);

        // determine root (is not alwaays the first!)
        root = noToTreenode.get(0);
        while (root.getParent() != null){
            root = root.getParent();
        }
    }

    public TreeMap<Integer,LinkedList<MDTreeNode>> getPrimeModulesBottomUp(){
        TreeMap<Integer,LinkedList<MDTreeNode>>ret = new TreeMap<>();
        ((MDTreeNode) root).getPrimeModules(ret,0);

        return ret;
    }


}
