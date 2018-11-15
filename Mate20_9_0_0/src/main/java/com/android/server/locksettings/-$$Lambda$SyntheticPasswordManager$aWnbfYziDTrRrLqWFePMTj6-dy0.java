package com.android.server.locksettings;

import android.hardware.weaver.V1_0.IWeaver.readCallback;
import android.hardware.weaver.V1_0.WeaverReadResponse;
import com.android.internal.widget.VerifyCredentialResponse;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SyntheticPasswordManager$aWnbfYziDTrRrLqWFePMTj6-dy0 implements readCallback {
    private final /* synthetic */ VerifyCredentialResponse[] f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$SyntheticPasswordManager$aWnbfYziDTrRrLqWFePMTj6-dy0(VerifyCredentialResponse[] verifyCredentialResponseArr, int i) {
        this.f$0 = verifyCredentialResponseArr;
        this.f$1 = i;
    }

    public final void onValues(int i, WeaverReadResponse weaverReadResponse) {
        SyntheticPasswordManager.lambda$weaverVerify$1(this.f$0, this.f$1, i, weaverReadResponse);
    }
}
