package dicograph.modDecomp;

/*
 * The different types of nodes in a modular decomposition tree. 
 */
public enum MDNodeType {
	PRIME,SERIES,PARALLEL, ORDER;

	/* Returns true iff this type is degenerate. */
	public boolean isDegenerate() {
		return (this == PARALLEL || this == SERIES);
	}
}
