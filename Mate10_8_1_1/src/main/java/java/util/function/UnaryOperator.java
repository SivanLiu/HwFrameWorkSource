package java.util.function;

@FunctionalInterface
public interface UnaryOperator<T> extends Function<T, T> {
    static <T> UnaryOperator<T> identity() {
        return -$Lambda$fV5r4SHSx_8Jib5fuc5m9G2MmTk.$INST$0;
    }

    static /* synthetic */ Object lambda$-java_util_function_UnaryOperator_2074(Object t) {
        return t;
    }
}
