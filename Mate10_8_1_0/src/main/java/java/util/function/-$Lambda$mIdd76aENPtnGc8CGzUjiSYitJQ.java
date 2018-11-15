package java.util.function;

import java.util.Comparator;

final /* synthetic */ class -$Lambda$mIdd76aENPtnGc8CGzUjiSYitJQ implements BinaryOperator {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ Object $m$0(Object arg0, Object arg1) {
        return BinaryOperator.lambda$-java_util_function_BinaryOperator_3246((Comparator) this.-$f0, arg0, arg1);
    }

    private final /* synthetic */ Object $m$1(Object arg0, Object arg1) {
        return BinaryOperator.lambda$-java_util_function_BinaryOperator_2544((Comparator) this.-$f0, arg0, arg1);
    }

    public /* synthetic */ -$Lambda$mIdd76aENPtnGc8CGzUjiSYitJQ(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final Object apply(Object obj, Object obj2) {
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
