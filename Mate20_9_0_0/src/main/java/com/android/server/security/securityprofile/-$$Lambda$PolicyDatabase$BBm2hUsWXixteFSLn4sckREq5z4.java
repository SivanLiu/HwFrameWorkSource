package com.android.server.security.securityprofile;

import huawei.android.security.securityprofile.ApkDigest;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PolicyDatabase$BBm2hUsWXixteFSLn4sckREq5z4 implements DigestHelper {
    private final /* synthetic */ ApkDigest f$0;

    public /* synthetic */ -$$Lambda$PolicyDatabase$BBm2hUsWXixteFSLn4sckREq5z4(ApkDigest apkDigest) {
        this.f$0 = apkDigest;
    }

    public final boolean matches(ApkDigest apkDigest) {
        return apkDigest.base64Digest.equals(this.f$0.base64Digest);
    }
}
