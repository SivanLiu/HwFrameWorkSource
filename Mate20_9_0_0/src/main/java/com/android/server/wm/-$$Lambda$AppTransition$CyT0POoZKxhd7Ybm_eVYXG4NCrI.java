package com.android.server.wm;

import android.view.IAppTransitionAnimationSpecsFuture;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AppTransition$CyT0POoZKxhd7Ybm_eVYXG4NCrI implements Runnable {
    private final /* synthetic */ AppTransition f$0;
    private final /* synthetic */ IAppTransitionAnimationSpecsFuture f$1;

    public /* synthetic */ -$$Lambda$AppTransition$CyT0POoZKxhd7Ybm_eVYXG4NCrI(AppTransition appTransition, IAppTransitionAnimationSpecsFuture iAppTransitionAnimationSpecsFuture) {
        this.f$0 = appTransition;
        this.f$1 = iAppTransitionAnimationSpecsFuture;
    }

    public final void run() {
        AppTransition.lambda$fetchAppTransitionSpecsFromFuture$0(this.f$0, this.f$1);
    }
}
