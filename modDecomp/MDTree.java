package dicograph.modDecomp;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.BitSet;
import java.util.HashMap;

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

	public MDTree(UndirectedGraph jGraph) {
		super();
		setRoot( buildMDTree(jGraph) );
	}

	protected MDTree(){
	    super();
    }

    public HashMap<BitSet, RootedTreeNode> getStrongModulesBool(MDTreeLeafNode[] leaves) {
        HashMap<BitSet, RootedTreeNode> ret = new HashMap<>();
        MDTreeNode rootAsMd = (MDTreeNode) root;
        rootAsMd.getStrongModulesBool(leaves, ret);
        moduleToTreenode = ret;

        return ret;
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
