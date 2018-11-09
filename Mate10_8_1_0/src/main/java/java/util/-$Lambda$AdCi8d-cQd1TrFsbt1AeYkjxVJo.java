package java.util;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

final /* synthetic */ class -$Lambda$AdCi8d-cQd1TrFsbt1AeYkjxVJo implements DoubleConsumer {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    /* renamed from: java.util.-$Lambda$AdCi8d-cQd1TrFsbt1AeYkjxVJo$1 */
    final /* synthetic */ class AnonymousClass1 implements IntConsumer {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(int arg0) {
            ((Consumer) this.-$f0).accept(Integer.valueOf(arg0));
        }

        private final /* synthetic */ void $m$1(int arg0) {
            ((Consumer) this.-$f0).accept(Integer.valueOf(arg0));
        }

        private final /* synthetic */ void $m$2(int arg0) {
            ((Consumer) this.-$f0).accept(Integer.valueOf(arg0));
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final void accept(int i) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(i);
                    return;
                case (byte) 1:
                    $m$1(i);
                    return;
                case (byte) 2:
                    $m$2(i);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: java.util.-$Lambda$AdCi8d-cQd1TrFsbt1AeYkjxVJo$2 */
    final /* synthetic */ class AnonymousClass2 implements LongConsumer {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(long arg0) {
            ((Consumer) this.-$f0).accept(Long.valueOf(arg0));
        }

        private final /* synthetic */ void $m$1(long arg0) {
            ((Consumer) this.-$f0).accept(Long.valueOf(arg0));
        }

        private final /* synthetic */ void $m$2(long arg0) {
            ((Consumer) this.-$f0).accept(Long.valueOf(arg0));
        }

        public /* synthetic */ AnonymousClass2(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final void accept(long j) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(j);
                    return;
                case (byte) 1:
                    $m$1(j);
                    return;
                case (byte) 2:
                    $m$2(j);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ void $m$0(double arg0) {
        ((Consumer) this.-$f0).accept(Double.valueOf(arg0));
    }

    private final /* synthetic */ void $m$1(double arg0) {
        ((Consumer) this.-$f0).accept(Double.valueOf(arg0));
    }

    private final /* synthetic */ void $m$2(double arg0) {
        ((Consumer) this.-$f0).accept(Double.valueOf(arg0));
    }

    public /* synthetic */ -$Lambda$AdCi8d-cQd1TrFsbt1AeYkjxVJo(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final void accept(double d) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(d);
                return;
            case (byte) 1:
                $m$1(d);
                return;
            case (byte) 2:
                $m$2(d);
                return;
            default:
                throw new AssertionError();
        }
    }
}
