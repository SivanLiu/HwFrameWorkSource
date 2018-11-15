package com.android.server.power;

import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BatterySaverPolicy$DPeh8xGdH0ye3BQJ8Ozaqeu6Y30 implements AccessibilityStateChangeListener {
    private final /* synthetic */ BatterySaverPolicy f$0;

    public /* synthetic */ -$$Lambda$BatterySaverPolicy$DPeh8xGdH0ye3BQJ8Ozaqeu6Y30(BatterySaverPolicy batterySaverPolicy) {
        this.f$0 = batterySaverPolicy;
    }

    public final void onAccessibilityStateChanged(boolean z) {
        BatterySaverPolicy.lambda$systemReady$0(this.f$0, z);
    }
}
