package dicograph.Editing;

/**
 * Created by Fynn Leitow on 09.01.18.
 */
public enum  EditType {
    Lazy,
    Primes,
    SoftTH,
    HardTH,
    ILP,
    ILPGlobal,
    None;

    boolean oneIsEnough(){
        return (this == Lazy || this == ILP || this == ILPGlobal);
    }

    boolean skipSmallPrimes(){
        return (this == Primes || this == SoftTH || this == HardTH);
    }
}
