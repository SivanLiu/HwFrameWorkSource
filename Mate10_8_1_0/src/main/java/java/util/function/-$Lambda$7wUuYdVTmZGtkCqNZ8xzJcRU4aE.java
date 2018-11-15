package java.util.function;

final /* synthetic */ class -$Lambda$7wUuYdVTmZGtkCqNZ8xzJcRU4aE implements IntPredicate {
    private final /* synthetic */ Object -$f0;

    /* renamed from: java.util.function.-$Lambda$7wUuYdVTmZGtkCqNZ8xzJcRU4aE$1 */
    final /* synthetic */ class AnonymousClass1 implements IntPredicate {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ boolean $m$0(int arg0) {
            return ((IntPredicate) this.-$f0).lambda$-java_util_function_IntPredicate_2831((IntPredicate) this.-$f1, arg0);
        }

        private final /* synthetic */ boolean $m$1(int arg0) {
            return ((IntPredicate) this.-$f0).lambda$-java_util_function_IntPredicate_4072((IntPredicate) this.-$f1, arg0);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final boolean test(int i) {
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

    private final /* synthetic */ boolean $m$0(int arg0) {
        return ((IntPredicate) this.-$f0).lambda$-java_util_function_IntPredicate_3136(arg0);
    }

    public /* synthetic */ -$Lambda$7wUuYdVTmZGtkCqNZ8xzJcRU4aE(Object obj) {
        this.-$f0 = obj;
    }

    public final boolean test(int i) {
        return $m$0(i);
    }
}
