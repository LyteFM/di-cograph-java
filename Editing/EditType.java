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
    ILPGlobal;

    boolean oneIsEnogh(){
        return (this == Lazy || this == ILP || this == ILPGlobal);
    }
}
