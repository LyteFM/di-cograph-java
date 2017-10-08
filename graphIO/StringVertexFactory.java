package dicograph.graphIO;

import org.jgrapht.VertexFactory;

import java.util.HashMap;

/**
 * Created by Fynn Leitow on 08.10.17.
 * Default implementation for a String-VertexFactory.
 * todo: add possibility to create from a Set of Strings
 */
public class StringVertexFactory implements VertexFactory<String> {

    private int counter = 0;


    public StringVertexFactory()
    {
        this(0);
    }

    public StringVertexFactory(int counter)
    {
        this.counter = counter;
    }

    @Override
    public String createVertex()
    {
        counter++;
        return Integer.toString(counter);
    }
}
