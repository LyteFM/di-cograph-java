package dicograph.graphIO;

import org.jgrapht.VertexFactory;

/**
 * Created by Fynn Leitow on 08.10.17.
 * Default implementation for a String-VertexFactory.
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
        String vertex = parseInt(counter);
        counter++;
        return vertex;
    }

    public static String parseInt(int number){

        String vertex = Integer.toString(number);
        if(number < 10){
            return "0" + vertex;
        } else {
            return vertex;
        }
    }
}
