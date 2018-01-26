package dicograph.utils;

import org.jgrapht.alg.util.Pair;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by Fynn Leitow on 24.01.18.
 */
public class Triple extends Pair<Set<Integer>, Integer> {

    public Triple(Integer x, Integer y, Integer z){
        super( new CopyOnWriteArraySet<>( Arrays.asList(x,y) ), z);
    }
}
