package dicograph.modDecomp;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import dicograph.graphIO.NodeTypeTester;
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

	public MDTree(UndirectedGraph<Integer,DefaultEdge> jGraph) {
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
    public String verifyNodeTypes(UndirectedGraph<Integer,DefaultEdge> graph){

        StringBuilder builder = new StringBuilder();

        for(Map.Entry<BitSet,RootedTreeNode> moduleEntry : moduleToTreenode.entrySet() ){

            MDTreeNode currNode = (MDTreeNode) moduleEntry.getValue(); // note: of course, I may only use one represantative.

            UndirectedInducedIntSubgraph<DefaultEdge> subgraph = new UndirectedInducedIntSubgraph<>(graph, moduleEntry.getKey());
            MDNodeType verified = NodeTypeTester.determineNodeType(subgraph,false);
            if(verified != currNode.getType()){
                builder.append("Wrong type ").append(verified).append(" for node: ").append(currNode).append("\n");
            }

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
	private MDTreeNode buildMDTree(UndirectedGraph<Integer, DefaultEdge> g) {

		if (g.vertexSet().isEmpty()){
			return null;
		}

		RecSubProblem entireProblem = new RecSubProblem(g);

		MDTreeNode root = entireProblem.solve();
		root.clearVisited();
		return root;
	}
}
