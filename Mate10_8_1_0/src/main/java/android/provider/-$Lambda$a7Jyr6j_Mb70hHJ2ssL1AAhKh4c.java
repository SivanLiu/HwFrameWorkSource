package android.provider;

import android.content.Context;
import android.graphics.Typeface;
import android.os.CancellationSignal;
import android.os.Handler;
import android.provider.FontsContract.FontRequestCallback;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

final /* synthetic */ class -$Lambda$a7Jyr6j_Mb70hHJ2ssL1AAhKh4c implements Comparator {
    public static final /* synthetic */ -$Lambda$a7Jyr6j_Mb70hHJ2ssL1AAhKh4c $INST$0 = new -$Lambda$a7Jyr6j_Mb70hHJ2ssL1AAhKh4c();

    /* renamed from: android.provider.-$Lambda$a7Jyr6j_Mb70hHJ2ssL1AAhKh4c$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((FontRequestCallback) this.-$f0).onTypefaceRetrieved((Typeface) this.-$f1);
        }

        private final /* synthetic */ void $m$1() {
            ((FontRequestCallback) this.-$f0).onTypefaceRetrieved((Typeface) this.-$f1);
        }

        private final /* synthetic */ void $m$2() {
            ((FontRequestCallback) this.-$f0).onTypefaceRetrieved((Typeface) this.-$f1);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void run() {
            switch (this.$id) {
                case (byte) 0:
                    $m$0();
                    return;
                case (byte) 1:
                    $m$1();
                    return;
                case (byte) 2:
                    $m$2();
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: android.provider.-$Lambda$a7Jyr6j_Mb70hHJ2ssL1AAhKh4c$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;
        private final /* synthetic */ Object -$f4;

        private final /* synthetic */ void $m$0() {
            FontsContract.lambda$-android_provider_FontsContract_20998((Context) this.-$f0, (CancellationSignal) this.-$f1, (FontRequest) this.-$f2, (Handler) this.-$f3, (FontRequestCallback) this.-$f4);
        }

        public /* synthetic */ AnonymousClass2(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
            this.-$f2 = obj3;
            this.-$f3 = obj4;
            this.-$f4 = obj5;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: android.provider.-$Lambda$a7Jyr6j_Mb70hHJ2ssL1AAhKh4c$3 */
    final /* synthetic */ class AnonymousClass3 implements Runnable {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;
        private final /* synthetic */ Object -$f4;
        private final /* synthetic */ Object -$f5;
        private final /* synthetic */ Object -$f6;

        private final /* synthetic */ void $m$0() {
            FontsContract.lambda$-android_provider_FontsContract_13824((FontRequest) this.-$f0, (String) this.-$f1, (AtomicReference) this.-$f2, (Lock) this.-$f3, (AtomicBoolean) this.-$f4, (AtomicBoolean) this.-$f5, (Condition) this.-$f6);
        }

        public /* synthetic */ AnonymousClass3(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
            this.-$f2 = obj3;
            this.-$f3 = obj4;
            this.-$f4 = obj5;
            this.-$f5 = obj6;
            this.-$f6 = obj7;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: android.provider.-$Lambda$a7Jyr6j_Mb70hHJ2ssL1AAhKh4c$4 */
    final /* synthetic */ class AnonymousClass4 implements Runnable {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((FontRequestCallback) this.-$f1).onTypefaceRequestFailed(this.-$f0);
        }

        public /* synthetic */ AnonymousClass4(int i, Object obj) {
            this.-$f0 = i;
            this.-$f1 = obj;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return FontsContract.lambda$-android_provider_FontsContract_31286((byte[]) arg0, (byte[]) arg1);
    }

    private /* synthetic */ -$Lambda$a7Jyr6j_Mb70hHJ2ssL1AAhKh4c() {
    }

    public final int compare(Object obj, Object obj2) {
        return $m$0(obj, obj2);
    }
}
