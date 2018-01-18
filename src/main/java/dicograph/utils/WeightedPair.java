package dicograph.utils;

import org.jgrapht.alg.util.Pair;

/**
 * Created by Fynn Leitow on 22.12.17.
 */
public class WeightedPair<A,B> extends Pair<A,B> {
    private double weight;

    public WeightedPair(A a, B b){
        super(a,b);
    }

    public WeightedPair(A a, B b, double w){
        this(a,b);
        weight = w;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
