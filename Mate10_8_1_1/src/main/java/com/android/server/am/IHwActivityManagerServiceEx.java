package com.android.server.am;

import android.app.IHwActivityNotifier;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.IMWThirdpartyCallback;
import android.util.TimingsTraceLog;
import java.util.List;
import java.util.Map;

public interface IHwActivityManagerServiceEx {
    void call(Bundle bundle);

    int canAppBoost(ApplicationInfo applicationInfo);

    boolean canCleanTaskRecord(String str);

    int changeGidIfRepairMode(int i, String str);

    boolean cleanPackageRes(List<String> list, Map<String, List<String>> map, int i, boolean z, boolean z2, boolean z3);

    void dispatchActivityLifeState(ActivityRecord activityRecord, String str);

    void gestureToHome();

    TaskChangeNotificationController getHwTaskChangeController();

    boolean isApplyPersistAppPatch(String str, int i, int i2, boolean z, boolean z2, String str2, String str3);

    boolean isSpecialVideoForPCMode(ActivityRecord activityRecord);

    void killApplication(String str, int i, int i2, String str2);

    void notifyActivityState(ActivityRecord activityRecord, String str);

    void notifyAppSwitch(ActivityRecord activityRecord, ActivityRecord activityRecord2);

    void onAppGroupChanged(int i, int i2, String str, int i3, int i4);

    void onMultiWindowModeChanged(boolean z);

    void registerHwActivityNotifier(IHwActivityNotifier iHwActivityNotifier, String str);

    boolean registerThirdPartyCallBack(IMWThirdpartyCallback iMWThirdpartyCallback);

    void setActivityVisibleState(boolean z);

    void showUninstallLauncherDialog(String str);

    Boolean switchUser(int i);

    void systemReady(Runnable runnable, TimingsTraceLog timingsTraceLog);

    void unregisterHwActivityNotifier(IHwActivityNotifier iHwActivityNotifier);

    boolean unregisterThirdPartyCallBack(IMWThirdpartyCallback iMWThirdpartyCallback);
}
