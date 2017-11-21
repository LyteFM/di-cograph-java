package dicograph.modDecomp;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.io.DOTImporter;
import org.jgrapht.io.ImportException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import dicograph.graphIO.UndirectedInducedIntSubgraph;

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

    // F.L. 16.11.17: Debug option (via moduleToTreenode)
    public String verifyNodeTypes(SimpleGraph<Integer,DefaultEdge> graph){

        StringBuilder builder = new StringBuilder();
        LinkedList<RootedTreeNode> allNodes = new LinkedList<>(moduleToTreenode.values());
        allNodes.add(root);

        for(RootedTreeNode node : allNodes ){

            MDTreeNode currNode = (MDTreeNode) node; // note: of course, I may only use one represantative.
            LinkedList<Integer> childRepresentatives = new LinkedList<>();
            RootedTreeNode currChild = currNode.getFirstChild();
            while (currChild != null){
                int anyVertex;
                if(currChild.isALeaf()) {
                    anyVertex = ((MDTreeLeafNode) currChild).getVertexNo();
                } else {
                    anyVertex = currChild.vertices.nextSetBit(0);
                }
                if(anyVertex >= 0)
                    childRepresentatives.add(anyVertex);
                else
                    throw new IllegalStateException("No valid child vertex found for "+ currChild);

                currChild = currChild.getRightSibling();
            }


            UndirectedInducedIntSubgraph<DefaultEdge> subgraph = new UndirectedInducedIntSubgraph<>(graph, childRepresentatives);
            String verificationResult = MDNodeType.verifyNodeType(false,subgraph, graph,currNode.getType(), childRepresentatives, currNode);
            builder.append(verificationResult);

        }

        return builder.toString();
    }


    /**
     * Makes the String representation of MDTree human-readable.
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



	public static MDTree readFromDot(File dotFile) throws IOException, ImportException{

        SimpleDirectedGraph< Integer ,DefaultEdge> treeGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
        HashMap<Integer,String> noToLabel = new HashMap<>();

        DOTImporter<Integer,DefaultEdge> importer = new DOTImporter<>(
                (label, attributes) -> { int no = Integer.valueOf(label); noToLabel.put(no,attributes.get("label").getValue()); return no;
                }
                ,
                (from, to, label, attributes) -> treeGraph.addEdge(from, to) );
        importer.importGraph(treeGraph, dotFile );
        System.out.println(treeGraph);


        MDTree ret = new MDTree();

        return ret;
    }


}
