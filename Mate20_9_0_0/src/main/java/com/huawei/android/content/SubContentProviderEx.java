package com.huawei.android.content;

import android.content.ContentProvider;
import android.net.Uri;
import android.os.IBinder;

public abstract class SubContentProviderEx extends ContentProvider {
    protected int enforceReadPermissionInner(Uri uri, String callingPkg, IBinder callerToken) {
        return super.enforceReadPermissionInner(uri, callingPkg, callerToken);
    }

    protected int enforceWritePermissionInner(Uri uri, String callingPkg, IBinder callerToken) {
        return super.enforceWritePermissionInner(uri, callingPkg, callerToken);
    }
}
