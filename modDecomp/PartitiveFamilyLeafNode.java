package dicograph.modDecomp;

/**
 * Created by Fynn Leitow on 29.10.17.
 */
public class PartitiveFamilyLeafNode extends PartitiveFamilyTreeNode {

    private int vertex;

    protected PartitiveFamilyLeafNode(int vertexNo) {
        super();
        vertex = vertexNo;
    }

    public int getVertex() {
        return vertex;
    }

    @Override
    public String toString() {

        return "(Leaf: " + vertex + ")";
    }
}
