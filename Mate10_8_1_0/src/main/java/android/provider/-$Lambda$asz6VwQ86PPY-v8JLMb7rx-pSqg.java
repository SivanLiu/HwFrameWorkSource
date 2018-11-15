package android.provider;

import android.provider.FontsContract.FontRequestCallback;

final /* synthetic */ class -$Lambda$asz6VwQ86PPY-v8JLMb7rx-pSqg implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0() {
        ((FontRequestCallback) this.-$f0).onTypefaceRequestFailed(-1);
    }

    private final /* synthetic */ void $m$1() {
        ((FontRequestCallback) this.-$f0).onTypefaceRequestFailed(-2);
    }

    private final /* synthetic */ void $m$2() {
        ((FontRequestCallback) this.-$f0).onTypefaceRequestFailed(-3);
    }

    private final /* synthetic */ void $m$3() {
        ((FontRequestCallback) this.-$f0).onTypefaceRequestFailed(-3);
    }

    private final /* synthetic */ void $m$4() {
        ((FontRequestCallback) this.-$f0).onTypefaceRequestFailed(1);
    }

    private final /* synthetic */ void $m$5() {
        ((FontRequestCallback) this.-$f0).onTypefaceRequestFailed(-3);
    }

    private final /* synthetic */ void $m$6() {
        ((FontRequestCallback) this.-$f0).onTypefaceRequestFailed(-3);
    }

    private final /* synthetic */ void $m$7() {
        ((NameValueCache) this.-$f0).lambda$-android_provider_Settings$NameValueCache_76966();
    }

    public /* synthetic */ -$Lambda$asz6VwQ86PPY-v8JLMb7rx-pSqg(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
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
            case (byte) 3:
                $m$3();
                return;
            case (byte) 4:
                $m$4();
                return;
            case (byte) 5:
                $m$5();
                return;
            case (byte) 6:
                $m$6();
                return;
            case (byte) 7:
                $m$7();
                return;
            default:
                throw new AssertionError();
        }
    }
}
