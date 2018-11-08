package java.util.stream;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.Node.OfDouble;
import java.util.stream.Node.OfInt;
import java.util.stream.Node.OfLong;

final /* synthetic */ class -$Lambda$UMgkLPOadvYHXl2uzl_OZxGJCZk implements Consumer {
    public static final /* synthetic */ -$Lambda$UMgkLPOadvYHXl2uzl_OZxGJCZk $INST$0 = new -$Lambda$UMgkLPOadvYHXl2uzl_OZxGJCZk((byte) 0);
    public static final /* synthetic */ -$Lambda$UMgkLPOadvYHXl2uzl_OZxGJCZk $INST$1 = new -$Lambda$UMgkLPOadvYHXl2uzl_OZxGJCZk((byte) 1);
    public static final /* synthetic */ -$Lambda$UMgkLPOadvYHXl2uzl_OZxGJCZk $INST$2 = new -$Lambda$UMgkLPOadvYHXl2uzl_OZxGJCZk((byte) 2);
    private final /* synthetic */ byte $id;

    /* renamed from: java.util.stream.-$Lambda$UMgkLPOadvYHXl2uzl_OZxGJCZk$1 */
    final /* synthetic */ class AnonymousClass1 implements DoubleConsumer {
        public static final /* synthetic */ AnonymousClass1 $INST$0 = new AnonymousClass1((byte) 0);
        public static final /* synthetic */ AnonymousClass1 $INST$1 = new AnonymousClass1((byte) 1);
        private final /* synthetic */ byte $id;

        private final /* synthetic */ void $m$0(double arg0) {
            OfDouble.lambda$-java_util_stream_Node$OfDouble_19427(arg0);
        }

        private final /* synthetic */ void $m$1(double arg0) {
            OfDouble.lambda$-java_util_stream_StreamSpliterators$SliceSpliterator$OfDouble_31710(arg0);
        }

        private /* synthetic */ AnonymousClass1(byte b) {
            this.$id = b;
        }

        public final void accept(double d) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(d);
                    return;
                case (byte) 1:
                    $m$1(d);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: java.util.stream.-$Lambda$UMgkLPOadvYHXl2uzl_OZxGJCZk$2 */
    final /* synthetic */ class AnonymousClass2 implements IntConsumer {
        public static final /* synthetic */ AnonymousClass2 $INST$0 = new AnonymousClass2((byte) 0);
        public static final /* synthetic */ AnonymousClass2 $INST$1 = new AnonymousClass2((byte) 1);
        private final /* synthetic */ byte $id;

        private final /* synthetic */ void $m$0(int arg0) {
            OfInt.lambda$-java_util_stream_Node$OfInt_13922(arg0);
        }

        private final /* synthetic */ void $m$1(int arg0) {
            OfInt.lambda$-java_util_stream_StreamSpliterators$SliceSpliterator$OfInt_29662(arg0);
        }

        private /* synthetic */ AnonymousClass2(byte b) {
            this.$id = b;
        }

        public final void accept(int i) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(i);
                    return;
                case (byte) 1:
                    $m$1(i);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: java.util.stream.-$Lambda$UMgkLPOadvYHXl2uzl_OZxGJCZk$3 */
    final /* synthetic */ class AnonymousClass3 implements LongConsumer {
        public static final /* synthetic */ AnonymousClass3 $INST$0 = new AnonymousClass3((byte) 0);
        public static final /* synthetic */ AnonymousClass3 $INST$1 = new AnonymousClass3((byte) 1);
        private final /* synthetic */ byte $id;

        private final /* synthetic */ void $m$0(long arg0) {
            OfLong.lambda$-java_util_stream_Node$OfLong_16640(arg0);
        }

        private final /* synthetic */ void $m$1(long arg0) {
            OfLong.lambda$-java_util_stream_StreamSpliterators$SliceSpliterator$OfLong_30670(arg0);
        }

        private /* synthetic */ AnonymousClass3(byte b) {
            this.$id = b;
        }

        public final void accept(long j) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(j);
                    return;
                case (byte) 1:
                    $m$1(j);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ void $m$0(Object arg0) {
        Node.lambda$-java_util_stream_Node_5216(arg0);
    }

    private final /* synthetic */ void $m$1(Object arg0) {
        OfRef.lambda$-java_util_stream_StreamSpliterators$SliceSpliterator$OfRef_26203(arg0);
    }

    private final /* synthetic */ void $m$2(Object arg0) {
        OfRef.lambda$-java_util_stream_StreamSpliterators$SliceSpliterator$OfRef_25294(arg0);
    }

    private /* synthetic */ -$Lambda$UMgkLPOadvYHXl2uzl_OZxGJCZk(byte b) {
        this.$id = b;
    }

    public final void accept(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(obj);
                return;
            case (byte) 1:
                $m$1(obj);
                return;
            case (byte) 2:
                $m$2(obj);
                return;
            default:
                throw new AssertionError();
        }
    }
}
