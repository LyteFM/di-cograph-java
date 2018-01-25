package dicograph.utils;

import org.jgrapht.alg.util.Pair;

/**
 * Created by Fynn Leitow on 24.01.18.
 */
public class Triple extends Pair<Pair<Integer,Integer>, Integer> {

    public Triple(Integer x, Integer y, Integer z){
        super(new Pair<>(x,y), z);
    }

}
