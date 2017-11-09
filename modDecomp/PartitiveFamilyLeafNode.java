package dicograph.modDecomp;

/**
 * Created by Fynn Leitow on 29.10.17.
 */
public class PartitiveFamilyLeafNode extends PartitiveFamilyTreeNode {

    int vertex;
    // Debug: Corresponding String in JGraphT

    protected PartitiveFamilyLeafNode(int vertexNo) {
        super();
        vertex = vertexNo;
    }

    @Override
    public String toString() {

        return "(Leaf: " + vertex + ")";
    }
}
