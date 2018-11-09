package java.util.function;

final /* synthetic */ class -$Lambda$n4VFJtoJH2VuvmBTzi4u5lJWNJs implements IntUnaryOperator {
    public static final /* synthetic */ -$Lambda$n4VFJtoJH2VuvmBTzi4u5lJWNJs $INST$0 = new -$Lambda$n4VFJtoJH2VuvmBTzi4u5lJWNJs();

    /* renamed from: java.util.function.-$Lambda$n4VFJtoJH2VuvmBTzi4u5lJWNJs$1 */
    final /* synthetic */ class AnonymousClass1 implements IntUnaryOperator {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ int $m$0(int arg0) {
            return ((IntUnaryOperator) this.-$f0).lambda$-java_util_function_IntUnaryOperator_3344((IntUnaryOperator) this.-$f1, arg0);
        }

        private final /* synthetic */ int $m$1(int arg0) {
            return ((IntUnaryOperator) this.-$f0).lambda$-java_util_function_IntUnaryOperator_2591((IntUnaryOperator) this.-$f1, arg0);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final int applyAsInt(int i) {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0(i);
                case (byte) 1:
                    return $m$1(i);
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ int $m$0(int arg0) {
        return IntUnaryOperator.lambda$-java_util_function_IntUnaryOperator_3617(arg0);
    }

    private /* synthetic */ -$Lambda$n4VFJtoJH2VuvmBTzi4u5lJWNJs() {
    }

    public final int applyAsInt(int i) {
        return $m$0(i);
    }
}
