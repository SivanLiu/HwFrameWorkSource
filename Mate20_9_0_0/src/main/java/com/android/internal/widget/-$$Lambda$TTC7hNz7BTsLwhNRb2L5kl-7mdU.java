package com.android.internal.widget;

import com.android.internal.widget.LockPatternChecker.OnCheckCallback;
import com.android.internal.widget.LockPatternUtils.CheckCredentialProgressCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TTC7hNz7BTsLwhNRb2L5kl-7mdU implements CheckCredentialProgressCallback {
    private final /* synthetic */ OnCheckCallback f$0;

    public /* synthetic */ -$$Lambda$TTC7hNz7BTsLwhNRb2L5kl-7mdU(OnCheckCallback onCheckCallback) {
        this.f$0 = onCheckCallback;
    }

    public final void onEarlyMatched() {
        this.f$0.onEarlyMatched();
    }
}
