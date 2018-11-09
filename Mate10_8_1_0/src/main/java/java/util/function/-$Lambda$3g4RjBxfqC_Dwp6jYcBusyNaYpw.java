package java.util.function;

final /* synthetic */ class -$Lambda$3g4RjBxfqC_Dwp6jYcBusyNaYpw implements LongUnaryOperator {
    public static final /* synthetic */ -$Lambda$3g4RjBxfqC_Dwp6jYcBusyNaYpw $INST$0 = new -$Lambda$3g4RjBxfqC_Dwp6jYcBusyNaYpw();

    /* renamed from: java.util.function.-$Lambda$3g4RjBxfqC_Dwp6jYcBusyNaYpw$1 */
    final /* synthetic */ class AnonymousClass1 implements LongUnaryOperator {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ long $m$0(long arg0) {
            return ((LongUnaryOperator) this.-$f0).lambda$-java_util_function_LongUnaryOperator_3361((LongUnaryOperator) this.-$f1, arg0);
        }

        private final /* synthetic */ long $m$1(long arg0) {
            return ((LongUnaryOperator) this.-$f0).lambda$-java_util_function_LongUnaryOperator_2602((LongUnaryOperator) this.-$f1, arg0);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final long applyAsLong(long j) {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0(j);
                case (byte) 1:
                    return $m$1(j);
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ long $m$0(long arg0) {
        return LongUnaryOperator.lambda$-java_util_function_LongUnaryOperator_3638(arg0);
    }

    private /* synthetic */ -$Lambda$3g4RjBxfqC_Dwp6jYcBusyNaYpw() {
    }

    public final long applyAsLong(long j) {
        return $m$0(j);
    }
}
