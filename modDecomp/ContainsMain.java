package dicograph.modDecomp;

/* 
 * A wrapper for main demonstrating the construction of the MD 
 * tree for a graph encoded in a file supplied as the first command
 * line argument.  See 'Graph.java' for the input file format for graphs.
 */
public class ContainsMain {

	public static void main(String[] args) {

	    //String filePath = args[0];
        String filePath = "importFiles/tedder_test0.txt";

        // old läuft korrekt.
		GraphHandle g = new GraphHandle(filePath);
		System.out.println("Old Code:\n" + g.getMDTreeOld());

		GraphHandle g2 = new GraphHandle(filePath);
		System.out.println("\nNew Code:\n" + g2.getMDTree());
	}
}
