package com.android.server.security.securityprofile;

import huawei.android.security.securityprofile.ApkDigest;
import huawei.android.security.securityprofile.DigestMatcher;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PolicyDatabase$LwcoExQ65OK6nDlpDPiI3sSHA2E implements DigestHelper {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$PolicyDatabase$LwcoExQ65OK6nDlpDPiI3sSHA2E(String str) {
        this.f$0 = str;
    }

    public final boolean matches(ApkDigest apkDigest) {
        return DigestMatcher.packageMatchesDigest(this.f$0, apkDigest);
    }
}
