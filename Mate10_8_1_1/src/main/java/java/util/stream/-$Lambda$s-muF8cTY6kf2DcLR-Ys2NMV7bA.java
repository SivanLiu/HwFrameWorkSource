package java.util.stream;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;

final /* synthetic */ class -$Lambda$s-muF8cTY6kf2DcLR-Ys2NMV7bA implements BinaryOperator {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ Object $m$0(Object arg0, Object arg1) {
        return Collectors.lambda$-java_util_stream_Collectors_13304((BinaryOperator) this.-$f0, (Map) arg0, (Map) arg1);
    }

    private final /* synthetic */ Object $m$1(Object arg0, Object arg1) {
        return new Partition(((BinaryOperator) this.-$f0).apply(((Partition) arg0).forTrue, ((Partition) arg1).forTrue), ((BinaryOperator) this.-$f0).apply(((Partition) arg0).forFalse, ((Partition) arg1).forFalse));
    }

    private final /* synthetic */ Object $m$2(Object arg0, Object arg1) {
        return ((Object[]) arg0)[0] = ((BinaryOperator) this.-$f0).apply(((Object[]) arg0)[0], ((Object[]) arg1)[0]);
    }

    private final /* synthetic */ Object $m$3(Object arg0, Object arg1) {
        return ((Object[]) arg0)[0] = ((BinaryOperator) this.-$f0).apply(((Object[]) arg0)[0], ((Object[]) arg1)[0]);
    }

    private final /* synthetic */ Object $m$4(Object arg0, Object arg1) {
        return ((BiConsumer) this.-$f0).accept(arg0, arg1);
    }

    private final /* synthetic */ Object $m$5(Object arg0, Object arg1) {
        return ((BiConsumer) this.-$f0).accept(arg0, arg1);
    }

    private final /* synthetic */ Object $m$6(Object arg0, Object arg1) {
        return ((BiConsumer) this.-$f0).accept(arg0, arg1);
    }

    public /* synthetic */ -$Lambda$s-muF8cTY6kf2DcLR-Ys2NMV7bA(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final Object apply(Object obj, Object obj2) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj, obj2);
            case (byte) 1:
                return $m$1(obj, obj2);
            case (byte) 2:
                return $m$2(obj, obj2);
            case (byte) 3:
                return $m$3(obj, obj2);
            case (byte) 4:
                return $m$4(obj, obj2);
            case (byte) 5:
                return $m$5(obj, obj2);
            case (byte) 6:
                return $m$6(obj, obj2);
            default:
                throw new AssertionError();
        }
    }
}
