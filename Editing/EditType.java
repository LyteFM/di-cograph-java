package dicograph.Editing;

/**
 * Created by Fynn Leitow on 09.01.18.
 */
public enum  EditType {
    Lazy,
    BruteForce,
    GreedyILP,
    ILP,
    ILPGlobal,
    None;


//    boolean oneIsEnough(){
//        return (this == Lazy || this == ILP || this == ILPGlobal);
//    }

    boolean checkPrimesSize(){
        return (this == BruteForce || this == GreedyILP);
    }


    boolean doLazyOnFirst(){
        return (this == Lazy || this == BruteForce || this == GreedyILP );
    }

    boolean doLazyOnSecond(boolean useGlobalScore){
        return (this == Lazy || useGlobalScore && this == BruteForce);
    }
}
