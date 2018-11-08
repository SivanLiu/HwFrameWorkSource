package java.util.function;

import java.util.Objects;

final /* synthetic */ class -$Lambda$1rDGbc8p7Mv-tQJZzJy5uAxVFbE implements Predicate {
    public static final /* synthetic */ -$Lambda$1rDGbc8p7Mv-tQJZzJy5uAxVFbE $INST$0 = new -$Lambda$1rDGbc8p7Mv-tQJZzJy5uAxVFbE();

    /* renamed from: java.util.function.-$Lambda$1rDGbc8p7Mv-tQJZzJy5uAxVFbE$1 */
    final /* synthetic */ class AnonymousClass1 implements Predicate {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ boolean $m$0(Object arg0) {
            return this.-$f0.equals(arg0);
        }

        private final /* synthetic */ boolean $m$1(Object arg0) {
            return ((Predicate) this.-$f0).lambda$-java_util_function_Predicate_3052(arg0);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final boolean test(Object obj) {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0(obj);
                case (byte) 1:
                    return $m$1(obj);
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: java.util.function.-$Lambda$1rDGbc8p7Mv-tQJZzJy5uAxVFbE$2 */
    final /* synthetic */ class AnonymousClass2 implements Predicate {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ boolean $m$0(Object arg0) {
            return ((Predicate) this.-$f0).lambda$-java_util_function_Predicate_2759((Predicate) this.-$f1, arg0);
        }

        private final /* synthetic */ boolean $m$1(Object arg0) {
            return ((Predicate) this.-$f0).lambda$-java_util_function_Predicate_3988((Predicate) this.-$f1, arg0);
        }

        public /* synthetic */ AnonymousClass2(byte b, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final boolean test(Object obj) {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0(obj);
                case (byte) 1:
                    return $m$1(obj);
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ boolean $m$0(Object arg0) {
        return Objects.isNull(arg0);
    }

    private /* synthetic */ -$Lambda$1rDGbc8p7Mv-tQJZzJy5uAxVFbE() {
    }

    public final boolean test(Object obj) {
        return $m$0(obj);
    }
}
