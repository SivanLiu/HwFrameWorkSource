package java.util;

import java.io.Serializable;
import java.util.function.Function;

final /* synthetic */ class -$Lambda$4EqhxufgNKat19m0CB0-toH_lzo implements Comparator, Serializable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return ((Comparator) this.-$f0).compare(((Function) this.-$f1).apply(arg0), ((Function) this.-$f1).apply(arg1));
    }

    private final /* synthetic */ int $m$1(Object arg0, Object arg1) {
        return ((Comparator) this.-$f0).lambda$-java_util_Comparator_10127((Comparator) this.-$f1, arg0, arg1);
    }

    public /* synthetic */ -$Lambda$4EqhxufgNKat19m0CB0-toH_lzo(byte b, Object obj, Object obj2) {
        this.$id = b;
        this.-$f0 = obj;
        this.-$f1 = obj2;
    }

    public final int compare(Object obj, Object obj2) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj, obj2);
            case (byte) 1:
                return $m$1(obj, obj2);
            default:
                throw new AssertionError();
        }
    }
}
