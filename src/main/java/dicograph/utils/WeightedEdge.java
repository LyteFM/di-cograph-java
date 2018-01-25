package dicograph.utils;

import org.jgrapht.alg.util.Pair;

/**
 * Created by Fynn Leitow on 22.12.17.
 */
public class WeightedEdge extends Pair<Integer,Integer> {
    private double weight;

    public WeightedEdge(int a, int b){
        super(a,b);
    }

    public WeightedEdge(int a, int b, double w){
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
