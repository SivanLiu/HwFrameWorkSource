package com.android.server.wm;

import android.content.pm.ApplicationInfo;
import android.os.IBinder;
import android.view.IApplicationToken;
import com.android.server.AttributeCache.Entry;

public interface IHwAppWindowContainerController {
    int continueHwStartWindow(String str, Entry entry, ApplicationInfo applicationInfo, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7, IBinder iBinder, IApplicationToken iApplicationToken, RootWindowContainer rootWindowContainer);

    IBinder getTransferFrom(String str);

    boolean isHwStartWindowEnabled();
}
