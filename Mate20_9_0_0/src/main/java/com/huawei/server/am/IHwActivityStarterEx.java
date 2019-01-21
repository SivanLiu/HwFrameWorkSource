package com.huawei.server.am;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;

public interface IHwActivityStarterEx {
    void effectiveIawareToLaunchApp(Intent intent, ActivityInfo activityInfo, String str);

    boolean isAbleToLaunchInPCCastMode(String str, int i);

    boolean isAbleToLaunchInVR(Context context, String str);

    boolean isAbleToLaunchVideoActivity(Context context, Intent intent);
}
