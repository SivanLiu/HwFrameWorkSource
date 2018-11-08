package java.util.function;

final /* synthetic */ class -$Lambda$Y5kXEBZc5fDOtRicvFfczerD_ZI implements DoublePredicate {
    private final /* synthetic */ Object -$f0;

    /* renamed from: java.util.function.-$Lambda$Y5kXEBZc5fDOtRicvFfczerD_ZI$1 */
    final /* synthetic */ class AnonymousClass1 implements DoublePredicate {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ boolean $m$0(double arg0) {
            return ((DoublePredicate) this.-$f0).lambda$-java_util_function_DoublePredicate_2852((DoublePredicate) this.-$f1, arg0);
        }

        private final /* synthetic */ boolean $m$1(double arg0) {
            return ((DoublePredicate) this.-$f0).lambda$-java_util_function_DoublePredicate_4102((DoublePredicate) this.-$f1, arg0);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final boolean test(double d) {
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

    private final /* synthetic */ boolean $m$0(double arg0) {
        return ((DoublePredicate) this.-$f0).lambda$-java_util_function_DoublePredicate_3160(arg0);
    }

    public /* synthetic */ -$Lambda$Y5kXEBZc5fDOtRicvFfczerD_ZI(Object obj) {
        this.-$f0 = obj;
    }

    public final boolean test(double d) {
        return $m$0(d);
    }
}
