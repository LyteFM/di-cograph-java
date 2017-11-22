package dicograph.modDecomp;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dicograph.graphIO.DirectedInducedIntSubgraph;
import dicograph.graphIO.UndirectedInducedIntSubgraph;

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
		} else if (verified == PRIME) {
			// Prime: We have an error if any node has the same relation to all others.
			boolean error = false;
			for (int v1 : childRepresentatives) {
				boolean first = true;
				boolean isPrime = false;
				MDNodeType firstNodeType = verified;

				for (int v2: childRepresentatives) {
					if(v1 != v2) {
						LinkedList<Integer> subSet = new LinkedList<>();
						subSet.add(v1);
						subSet.add(v2);
						Graph<Integer, DefaultEdge> subsetSubgraph;
						if (isDirected) {
							subsetSubgraph = new DirectedInducedIntSubgraph<>(mainGraph, subSet);
						} else {
							subsetSubgraph = new UndirectedInducedIntSubgraph<>(mainGraph, subSet);
						}
						MDNodeType innerType = determineNodeType(subsetSubgraph, isDirected);
						if (first) {
							firstNodeType = innerType;
							first = false;
						} else {
							if (firstNodeType != innerType) {
								isPrime = true;
								break;
							}
						}
					}
				}
				if (!isPrime && firstNodeType == PRIME) {
					isPrime = true; // no changes, always stayed prime
				}
				if (!isPrime) { // no error, if
					error = true;
					builder.append("Found only ").append(firstNodeType).append(" for adjacencies of ").append(v1).append("\n");
				}
			}

			if(error){
				builder.append("For node: ").append(node).append("\n");
			}
		}

		return builder.toString();
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
					}
				}
				return null; // shouldn't happen, at that point it's either series or order.
			}
		}
		return MDNodeType.PRIME;

	}
}
