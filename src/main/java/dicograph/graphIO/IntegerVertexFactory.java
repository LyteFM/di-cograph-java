package dicograph.graphIO;

import org.jgrapht.VertexFactory;

/**
 * Default implementation of a vertex factory which creates integers. The vertices start by default
 * from zero. (Copied from JGraphT test)
 *
 * @author Assaf Lehr
 */
class IntegerVertexFactory
        implements VertexFactory<Integer>
{
    private int counter = 0;

    public IntegerVertexFactory()
    {
        this(0);
    }

    private IntegerVertexFactory(int counter)
    {
        this.counter = counter;
    }

    @Override
    public Integer createVertex()
    {
        return counter++;
    }

}