package com.android.internal.widget;

import com.android.internal.widget.LockPatternUtils.CheckCredentialProgressCallback;

final /* synthetic */ class -$Lambda$Z_D20fNZ3pzkot0ZIQi5t8fFLYw implements Runnable {
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0() {
        ((CheckCredentialProgressCallback) this.-$f0).onEarlyMatched();
    }

    public /* synthetic */ -$Lambda$Z_D20fNZ3pzkot0ZIQi5t8fFLYw(Object obj) {
        this.-$f0 = obj;
    }

    public final void run() {
        $m$0();
    }
}
