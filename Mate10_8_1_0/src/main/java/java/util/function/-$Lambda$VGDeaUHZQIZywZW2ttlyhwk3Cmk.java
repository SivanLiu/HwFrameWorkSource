package java.util.function;

final /* synthetic */ class -$Lambda$VGDeaUHZQIZywZW2ttlyhwk3Cmk implements DoubleUnaryOperator {
    public static final /* synthetic */ -$Lambda$VGDeaUHZQIZywZW2ttlyhwk3Cmk $INST$0 = new -$Lambda$VGDeaUHZQIZywZW2ttlyhwk3Cmk();

    /* renamed from: java.util.function.-$Lambda$VGDeaUHZQIZywZW2ttlyhwk3Cmk$1 */
    final /* synthetic */ class AnonymousClass1 implements DoubleUnaryOperator {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ double $m$0(double arg0) {
            return ((DoubleUnaryOperator) this.-$f0).lambda$-java_util_function_DoubleUnaryOperator_3397((DoubleUnaryOperator) this.-$f1, arg0);
        }

        private final /* synthetic */ double $m$1(double arg0) {
            return ((DoubleUnaryOperator) this.-$f0).lambda$-java_util_function_DoubleUnaryOperator_2626((DoubleUnaryOperator) this.-$f1, arg0);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final double applyAsDouble(double d) {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0(d);
                case (byte) 1:
                    return $m$1(d);
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ double $m$0(double arg0) {
        return DoubleUnaryOperator.lambda$-java_util_function_DoubleUnaryOperator_3682(arg0);
    }

    private /* synthetic */ -$Lambda$VGDeaUHZQIZywZW2ttlyhwk3Cmk() {
    }

    public final double applyAsDouble(double d) {
        return $m$0(d);
    }
}
