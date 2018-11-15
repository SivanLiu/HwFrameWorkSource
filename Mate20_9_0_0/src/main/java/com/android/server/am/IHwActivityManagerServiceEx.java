package com.android.server.am;

import android.app.ActivityOptions;
import android.app.IHwActivityNotifier;
import android.app.usage.UsageStatsManagerInternal;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.IMWThirdpartyCallback;
import android.util.TimingsTraceLog;
import android.view.WindowManagerPolicyConstants.PointerEventListener;
import com.android.server.mtm.taskstatus.ProcessInfo;
import java.util.List;
import java.util.Map;

public interface IHwActivityManagerServiceEx {
    void call(Bundle bundle);

    int canAppBoost(ActivityInfo activityInfo, boolean z);

    boolean canCleanTaskRecord(String str);

    boolean canSleepForPCMode();

    boolean canUpdateSleepForPCMode();

    int changeGidIfRepairMode(int i, String str);

    int[] changeGidsIfNeeded(ProcessRecord processRecord, int[] iArr);

    boolean checkActivityStartForPCMode(ActivityStarter activityStarter, ActivityOptions activityOptions, ActivityRecord activityRecord, ActivityStack activityStack);

    boolean cleanPackageRes(List<String> list, Map<String, List<String>> map, int i, boolean z, boolean z2, boolean z3);

    void dismissSplitScreenModeWithFinish(ActivityRecord activityRecord);

    void dispatchActivityLifeState(ActivityRecord activityRecord, String str);

    int getEffectiveUid(int i, int i2);

    TaskChangeNotificationController getHwTaskChangeController();

    Rect getPCTopTaskBounds(int i);

    List<String> getPidWithUiFromUid(int i);

    PointerEventListener getPointerEventListener();

    boolean getProcessRecordFromMTM(ProcessInfo processInfo);

    int getTopTaskIdInDisplay(int i, String str, boolean z);

    boolean handleANRFilterFIFO(int i, int i2);

    boolean isAllowToStartActivity(Context context, String str, ActivityInfo activityInfo, boolean z, ActivityInfo activityInfo2);

    boolean isApplyPersistAppPatch(String str, int i, int i2, boolean z, boolean z2, String str2, String str3);

    boolean isExemptedAuthority(Uri uri);

    boolean isFreeFormVisible();

    boolean isPcMutiResumeStack(ActivityStack activityStack);

    boolean isProcessExistPidsSelfLocked(String str, int i);

    boolean isSpecialVideoForPCMode(ActivityRecord activityRecord);

    boolean isTaskSupportResize(int i, boolean z, boolean z2);

    boolean isTaskVisible(int i);

    void killApplication(String str, int i, int i2, String str2);

    boolean killProcessRecordFromIAwareInternal(ProcessInfo processInfo, boolean z, boolean z2, String str, boolean z3);

    boolean killProcessRecordFromMTM(ProcessInfo processInfo, boolean z, String str);

    void noteActivityStart(String str, String str2, String str3, int i, int i2, boolean z);

    void notifyActivityState(ActivityRecord activityRecord, String str);

    void notifyAppSwitch(ActivityRecord activityRecord, ActivityRecord activityRecord2);

    void onAppGroupChanged(int i, int i2, String str, int i3, int i4);

    void onMultiWindowModeChanged(boolean z);

    int preloadApplication(String str, int i);

    void registerBroadcastReceiver();

    void registerHwActivityNotifier(IHwActivityNotifier iHwActivityNotifier, String str);

    boolean registerThirdPartyCallBack(IMWThirdpartyCallback iMWThirdpartyCallback);

    void removePackageAlarm(String str, List<String> list, int i);

    void removePackageStopFlag(String str, int i, String str2, int i2, String str3, Bundle bundle, int i3);

    void reportAppDiedMsg(int i, String str, int i2, String str2);

    void reportAssocDisable();

    void reportHomeProcess(ProcessRecord processRecord);

    void reportPreviousInfo(int i, ProcessRecord processRecord);

    void reportProcessDied(int i);

    void reportServiceRelationIAware(int i, ContentProviderRecord contentProviderRecord, ProcessRecord processRecord);

    void reportServiceRelationIAware(int i, ServiceRecord serviceRecord, ProcessRecord processRecord);

    void setAndRestoreMaxAdjIfNeed(List<String> list);

    void setHbsMiniAppUid(ApplicationInfo applicationInfo, Intent intent);

    void setThreadSchedPolicy(int i, ProcessRecord processRecord);

    boolean shouldPreventStartProcess(String str, int i);

    void showUninstallLauncherDialog(String str);

    Boolean switchUser(int i);

    void systemReady(Runnable runnable, TimingsTraceLog timingsTraceLog);

    void unregisterHwActivityNotifier(IHwActivityNotifier iHwActivityNotifier);

    boolean unregisterThirdPartyCallBack(IMWThirdpartyCallback iMWThirdpartyCallback);

    String[] updateEntryPointArgsForPCMode(ProcessRecord processRecord, String[] strArr);

    void updateUsageStatsForPCMode(ActivityRecord activityRecord, boolean z, UsageStatsManagerInternal usageStatsManagerInternal);

    boolean zrHungSendEvent(String str, int i, int i2, String str2, String str3, String str4);
}
