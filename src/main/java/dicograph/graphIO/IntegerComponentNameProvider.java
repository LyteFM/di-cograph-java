package dicograph.graphIO;


import org.jgrapht.io.ComponentNameProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Assigns a unique integer to represent each component. Each instance of provider maintains an
 * internal map between every component it has ever seen and the unique integer representing that
 * edge. As a result it is probably desirable to have a separate instance for each distinct graph.
 *
 * @param <T> the component type
 *
 * @author Trevor Harmon
 *
 *
 */

// F.L. 18.01.18: Copied from JGraphT, Start counting at 0 s.t. the release version of JGraphT can be used

public class IntegerComponentNameProvider<T>
        implements ComponentNameProvider<T>
{
    private int nextID = 0;
    private final Map<T, Integer> idMap = new HashMap<>();

    /**
     * Clears all cached identifiers, and resets the unique identifier counter.
     */
    public void clear()
    {
        nextID = 0;
        idMap.clear();
    }

    /**
     * Returns the string representation of a component.
     *
     * @param component the component to be named
     */
    @Override
    public String getName(T component)
    {
        Integer id = idMap.get(component);
        if (id == null) {
            id = nextID++;
            idMap.put(component, id);
        }
        return id.toString();
    }
}