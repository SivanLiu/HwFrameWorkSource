package java.util;

import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

final /* synthetic */ class -$Lambda$i2v6-5RQNI3YGcqf1AP8d7D8zis implements Consumer {
    private final /* synthetic */ Object -$f0;

    /* renamed from: java.util.-$Lambda$i2v6-5RQNI3YGcqf1AP8d7D8zis$1 */
    final /* synthetic */ class AnonymousClass1 implements IntFunction {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ Object $m$0(int arg0) {
            return ((CopiesList) this.-$f0).lambda$-java_util_Collections$CopiesList_199260(arg0);
        }

        private final /* synthetic */ Object $m$1(int arg0) {
            return ((CopiesList) this.-$f0).lambda$-java_util_Collections$CopiesList_199111(arg0);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final Object apply(int i) {
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

    /* renamed from: java.util.-$Lambda$i2v6-5RQNI3YGcqf1AP8d7D8zis$2 */
    final /* synthetic */ class AnonymousClass2 implements BiFunction {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ Object $m$0(Object arg0, Object arg1) {
            return ((CheckedMap) this.-$f0).lambda$-java_util_Collections$CheckedMap_151435((BiFunction) this.-$f1, arg0, arg1);
        }

        private final /* synthetic */ Object $m$1(Object arg0, Object arg1) {
            return ((CheckedMap) this.-$f0).lambda$-java_util_Collections$CheckedMap_146492((BiFunction) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass2(byte b, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
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

    /* renamed from: java.util.-$Lambda$i2v6-5RQNI3YGcqf1AP8d7D8zis$3 */
    final /* synthetic */ class AnonymousClass3 implements Function {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ Object $m$0(Object arg0) {
            return ((CheckedMap) this.-$f0).lambda$-java_util_Collections$CheckedMap_150612((Function) this.-$f1, arg0);
        }

        public /* synthetic */ AnonymousClass3(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final Object apply(Object obj) {
            return $m$0(obj);
        }
    }

    /* renamed from: java.util.-$Lambda$i2v6-5RQNI3YGcqf1AP8d7D8zis$4 */
    final /* synthetic */ class AnonymousClass4 implements UnaryOperator {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ Object $m$0(Object arg0) {
            return ((CheckedList) this.-$f0).lambda$-java_util_Collections$CheckedList_142882((UnaryOperator) this.-$f1, arg0);
        }

        public /* synthetic */ AnonymousClass4(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final Object apply(Object obj) {
            return $m$0(obj);
        }
    }

    private final /* synthetic */ void $m$0(Object arg0) {
        ((Consumer) this.-$f0).accept(new UnmodifiableEntry((Entry) arg0));
    }

    public /* synthetic */ -$Lambda$i2v6-5RQNI3YGcqf1AP8d7D8zis(Object obj) {
        this.-$f0 = obj;
    }

    public final void accept(Object obj) {
        $m$0(obj);
    }
}
