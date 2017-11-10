package dicograph.modDecomp;

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
}
