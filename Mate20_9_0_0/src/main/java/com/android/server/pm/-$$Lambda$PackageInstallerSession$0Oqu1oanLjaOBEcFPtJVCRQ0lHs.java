package com.android.server.pm;

import android.os.FileUtils.ProgressListener;
import android.system.Int64Ref;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PackageInstallerSession$0Oqu1oanLjaOBEcFPtJVCRQ0lHs implements ProgressListener {
    private final /* synthetic */ PackageInstallerSession f$0;
    private final /* synthetic */ Int64Ref f$1;

    public /* synthetic */ -$$Lambda$PackageInstallerSession$0Oqu1oanLjaOBEcFPtJVCRQ0lHs(PackageInstallerSession packageInstallerSession, Int64Ref int64Ref) {
        this.f$0 = packageInstallerSession;
        this.f$1 = int64Ref;
    }

    public final void onProgress(long j) {
        PackageInstallerSession.lambda$doWriteInternal$0(this.f$0, this.f$1, j);
    }
}
