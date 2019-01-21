package android.content.pm;

import android.content.Intent;
import android.content.pm.InstantAppResolveInfo.InstantAppDigest;
import android.os.Bundle;
import android.text.TextUtils;

public final class InstantAppRequest {
    public final String callingPackage;
    public final InstantAppDigest digest;
    public final Intent origIntent;
    public final boolean resolveForStart;
    public final String resolvedType;
    public final AuxiliaryResolveInfo responseObj;
    public final int userId;
    public final Bundle verificationBundle;

    public InstantAppRequest(AuxiliaryResolveInfo responseObj, Intent origIntent, String resolvedType, String callingPackage, int userId, Bundle verificationBundle, boolean resolveForStart) {
        this.responseObj = responseObj;
        this.origIntent = origIntent;
        this.resolvedType = resolvedType;
        this.callingPackage = callingPackage;
        this.userId = userId;
        this.verificationBundle = verificationBundle;
        this.resolveForStart = resolveForStart;
        if (origIntent.getData() == null || TextUtils.isEmpty(origIntent.getData().getHost())) {
            this.digest = InstantAppDigest.UNDEFINED;
        } else {
            this.digest = new InstantAppDigest(origIntent.getData().getHost(), 5);
        }
    }
}
