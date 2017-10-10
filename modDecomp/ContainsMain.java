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
		String g_res = g.getMDTreeOld().toString();
		System.out.println("Old Code:\n" + beautify(g_res));

		GraphHandle g2 = new GraphHandle(filePath);
		String g2_res = g2.getMDTree().toString();
		System.out.println("\nNew Code:\n" + beautify(g2_res));
	}

	static String beautify(String mdTree){
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

    static StringBuilder addOffset(StringBuilder builder, int times){
        String offset = "  ";
        for(int i = 0; i< times; i++){
	        builder.append(offset);
        }
        return builder;
    }
}
