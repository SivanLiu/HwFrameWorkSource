package com.android.internal.widget;

import com.android.internal.widget.LockPatternUtils.CheckCredentialProgressCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$gPQuiuEDuOmrh2MixBcV6a5gu5s implements Runnable {
    private final /* synthetic */ CheckCredentialProgressCallback f$0;

    public /* synthetic */ -$$Lambda$gPQuiuEDuOmrh2MixBcV6a5gu5s(CheckCredentialProgressCallback checkCredentialProgressCallback) {
        this.f$0 = checkCredentialProgressCallback;
    }

    public final void run() {
        this.f$0.onEarlyMatched();
    }
}
