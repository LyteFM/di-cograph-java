package dicograph.modDecomp;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dicograph.utils.SortAndCompare;

/*
 * The different types of nodes in a modular decomposition tree. 
 */
public enum MDNodeType {
	PRIME,
	SERIES,  // "1"
	PARALLEL,// "0"
	ORDER;   // "2"

	/* Returns true iff this type is degenerate. */
	public boolean isDegenerate() {
		return (this == PARALLEL || this == SERIES);
	}


	// F.L. 17.11.17: added verification methods
	// independent of MDTree/Partivive Tree

	public static String verifyNodeType(boolean isDirected, Graph<Integer,DefaultEdge> subgraph, Graph<Integer,DefaultEdge> mainGraph,
										MDNodeType expected, List<Integer> childRepresentatives, RootedTreeNode node){
		StringBuilder builder = new StringBuilder();

		MDNodeType verified = determineNodeType(subgraph,isDirected);
		if(verified != expected){
			builder.append("Wrong verified type ").append(verified).append(" for node: ").append(node).append("\n");
		} else {
			// Prime: We have an error if any vertex subset has the same relation to all other vertices of G.
			if(expected == PRIME) {
				List<List<Integer>> validSubsets = SortAndCompare.computeAllSubsets(childRepresentatives).stream().filter(
						l -> l.size() > 1 && l.size() < childRepresentatives.size()
				).collect(Collectors.toList());
				String result;
				System.out.println("For node " + node);
				//System.out.println("All subsets: " + validSubsets);
				for(int i = 0; i< validSubsets.size(); i++) {

					List<Integer> subSet = validSubsets.get(i);
					result = SortAndCompare.checkModuleBruteForce(mainGraph, subSet,false);
					if(!result.isEmpty()){
						builder.append("For vertices: ").append(subSet).append("\n").append(result);
						//break; // todo: not if I want all
					}
					if( i % 100000 == 0) {
						System.out.println("verified subset " + (i+1) + " of " + validSubsets.size());
					}
				}
			}
			// Internal relations are determined, now check verify relations to all other nodes.
			node.verifyModuleStatus(builder, mainGraph);
		}

		String ret = builder.toString();
		if(!ret.isEmpty()){
			return "For node: " + node +"\n" + ret;
		}

		return ret;
	}

	// if it's none of these two cases, it should be prime. However, in order to detect "false primes",
	// it's necessary to combine any two vertices and check again.

	public static MDNodeType determineNodeType(Graph<Integer,DefaultEdge> subgraph, boolean directed){

		Set<DefaultEdge> subEdgeSet = subgraph.edgeSet();

		if(subEdgeSet.isEmpty()){
			// no edges means 0-complete
			return MDNodeType.PARALLEL;
		} else {
			// series means degree = n-1 (or double for directed)
			boolean valid = true;
			Set<Integer> vertices = subgraph.vertexSet();
			BitSet allOutDegs = new BitSet(vertices.size());
			int expCountSeries = vertices.size() - 1;
			int expCountOrder = expCountSeries;
			if (directed)
				expCountSeries *= 2;
			for(int vertex : vertices){
				Set<DefaultEdge> touchingEdges = subgraph.edgesOf(vertex);
				int count = touchingEdges.size();
				if (count != expCountSeries) {
					// order needs n-1 touching vertices, but Series required twice as many in directed case
					if (!(directed && count == expCountOrder)) {
						valid = false;
						break;
					}
				}
				if(directed){
					// for order: need all outdegs from 0 to n-1
					int outDeg = 0;
					for(DefaultEdge edge : touchingEdges){
						if(subgraph.getEdgeTarget(edge) == vertex)
							outDeg++;
					}
					allOutDegs.set(outDeg);
				}
			}
			if(valid){
				if(!directed) {
					return MDNodeType.SERIES;
				} else{
					if(allOutDegs.cardinality() == vertices.size()){
					    // we have all out-degrees from 0 to n-1 -> Order.
						return MDNodeType.ORDER;
					} else if(allOutDegs.nextSetBit(0) == vertices.size()-1){
						// every node has n-1 outgoing edges -> Series.
						return MDNodeType.SERIES;
					} else {
						// the node appeared to be ORDER, but doesn't have the correct outdegs.
						return MDNodeType.PRIME;
					}
				}
			}
		}
		return MDNodeType.PRIME;

	}

}
