package com.android.server.wm;

import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.os.IBinder;
import android.view.IApplicationToken;
import com.android.server.AttributeCache;

public interface IHwAppWindowTokenEx {
    int continueHwStartWindow(String str, AttributeCache.Entry entry, ApplicationInfo applicationInfo, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7, IBinder iBinder, IApplicationToken iApplicationToken, RootWindowContainer rootWindowContainer, boolean z8);

    IBinder getTransferFrom(ApplicationInfo applicationInfo);

    boolean isHwMwAnimationBelowStack(AppWindowToken appWindowToken);

    boolean isHwStartWindowEnabled(String str, CompatibilityInfo compatibilityInfo);
}
