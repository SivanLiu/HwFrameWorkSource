package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.HwRecentTaskInfo;
import android.app.IActivityController;
import android.app.IHwActivityNotifier;
import android.app.ITaskStackListener;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IMWThirdpartyCallback;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import com.huawei.android.app.IGameObserver;
import com.huawei.android.app.IGameObserverEx;
import java.util.HashMap;
import java.util.List;

public interface IHwActivityTaskManagerServiceEx {
    boolean addGameSpacePackageList(List<String> list);

    void addStackReferenceIfNeeded(ActivityStack activityStack);

    void addSurfaceInNotchIfNeed();

    void adjustHwFreeformPosIfNeed(DisplayContent displayContent, boolean z);

    boolean blockSwipeFromTop(MotionEvent motionEvent, DisplayContent displayContent);

    void calcHwMultiWindowStackBoundsForConfigChange(ActivityStack activityStack, Rect rect, Rect rect2, int i, int i2, int i3, int i4, boolean z);

    void call(Bundle bundle);

    int canAppBoost(ActivityInfo activityInfo, boolean z);

    boolean canCleanTaskRecord(String str, int i, String str2);

    Intent changeStartActivityIfNeed(Intent intent);

    boolean checkTaskId(int i);

    boolean customActivityResuming(String str);

    boolean delGameSpacePackageList(List<String> list);

    void dismissSplitScreenModeWithFinish(ActivityRecord activityRecord);

    void dismissSplitScreenToFocusedStack();

    void dispatchActivityLifeState(ActivityRecord activityRecord, String str);

    void enterCoordinationMode();

    boolean enterCoordinationMode(Intent intent);

    void exitCoordinationMode();

    boolean exitCoordinationMode(boolean z, boolean z2);

    void exitSingleHandMode();

    List<ActivityStack> findCombinedSplitScreenStacks(ActivityStack activityStack);

    void focusStackChange(int i, int i2, ActivityStack activityStack, ActivityStack activityStack2);

    int getActivityWindowMode(IBinder iBinder);

    float getAspectRatioWithUserSet(String str, String str2, ActivityInfo activityInfo);

    int getCaptionState(IBinder iBinder);

    int[] getCombinedSplitScreenTaskIds(ActivityStack activityStack);

    Point getDragBarCenterPoint(Rect rect, ActivityStack activityStack);

    List<String> getGameList();

    float getHwMultiWinCornerRadius(int i);

    Bundle getHwMultiWindowAppControlLists();

    Bundle getHwMultiWindowState();

    HwRecentTaskInfo getHwRecentTaskInfo(int i);

    TaskChangeNotificationController getHwTaskChangeController();

    boolean getMultiWindowDisabled();

    Rect getPCTopTaskBounds(int i);

    HashMap<String, Integer> getPkgDisplayMaps();

    int getPreferedDisplayId(ActivityRecord activityRecord, ActivityOptions activityOptions, int i);

    Bundle getSplitStacksPos(int i, int i2);

    ActivityManager.TaskSnapshot getTaskSnapshot(int i, boolean z);

    Bitmap getTaskThumbnailOnPCMode(int i);

    Bundle getTopActivity();

    int getTopTaskIdInDisplay(int i, String str, boolean z);

    List<String> getVisiblePackages();

    List<ActivityManager.RunningTaskInfo> getVisibleTasks();

    int getWindowState(IBinder iBinder);

    void handleMultiWindowSwitch(IBinder iBinder, Bundle bundle);

    void hwResizeTask(int i, Rect rect);

    void hwRestoreTask(int i, float f, float f2);

    boolean isActivityVisiableInFingerBoost(ActivityRecord activityRecord);

    boolean isAllowToStartActivity(Context context, String str, ActivityInfo activityInfo, boolean z, ActivityInfo activityInfo2);

    boolean isExSplashEnable(Bundle bundle);

    boolean isFreeFormVisible();

    boolean isGameDndOn();

    boolean isGameDndOnEx();

    boolean isGameGestureDisabled();

    boolean isGameKeyControlOn();

    boolean isHwResizableApp(String str, int i);

    boolean isInGameSpace(String str);

    boolean isInMultiWindowMode();

    boolean isMagicWinExcludeTaskFromRecents(TaskRecord taskRecord);

    boolean isMagicWinSkipRemoveFromRecentTasks(TaskRecord taskRecord, TaskRecord taskRecord2);

    boolean isMaximizedPortraitAppOnPCMode(ActivityRecord activityRecord);

    boolean isNerverUseSizeCompateMode(String str);

    boolean isOverrideConfigByMagicWin(Configuration configuration);

    boolean isPhoneLandscape(DisplayContent displayContent);

    boolean isResizableApp(String str, int i);

    boolean isSpecialVideoForPCMode(ActivityRecord activityRecord);

    boolean isSplitStackVisible(ActivityDisplay activityDisplay, int i);

    boolean isStatusBarPermenantlyShowing();

    boolean isSupportDragForMultiWin(IBinder iBinder);

    boolean isSwitchToMagicWin(int i, boolean z, int i2);

    boolean isTaskNotResizeableEx(TaskRecord taskRecord, Rect rect);

    boolean isTaskSupportResize(int i, boolean z, boolean z2);

    boolean isTaskVisible(int i);

    void moveActivityTaskToBackEx(IBinder iBinder);

    void moveStackToFrontEx(ActivityOptions activityOptions, ActivityStack activityStack, ActivityRecord activityRecord);

    void moveTaskBackwards(int i);

    void noteActivityDisplayed(String str, int i, int i2, boolean z);

    boolean noteActivityInitializing(ActivityRecord activityRecord, ActivityRecord activityRecord2);

    void noteActivityStart(String str, String str2, String str3, int i, int i2, boolean z);

    void notifyActivityState(ActivityRecord activityRecord, String str);

    void onCaptionDropAnimationDone(IBinder iBinder);

    void onDisplayConfigurationChanged(int i);

    void onMultiWindowModeChanged(boolean z);

    void onSystemReady();

    void registerBroadcastReceiver();

    void registerGameObserver(IGameObserver iGameObserver);

    void registerGameObserverEx(IGameObserverEx iGameObserverEx);

    void registerHwActivityNotifier(IHwActivityNotifier iHwActivityNotifier, String str);

    void registerHwTaskStackListener(ITaskStackListener iTaskStackListener);

    boolean registerThirdPartyCallBack(IMWThirdpartyCallback iMWThirdpartyCallback);

    Rect relocateOffScreenWindow(Rect rect, ActivityStack activityStack);

    void removeStackReferenceIfNeeded(ActivityStack activityStack);

    void reportHomeProcess(WindowProcessController windowProcessController);

    void reportPreviousInfo(int i, WindowProcessController windowProcessController);

    boolean requestContentNode(ComponentName componentName, Bundle bundle, int i);

    boolean requestContentOther(ComponentName componentName, Bundle bundle, int i);

    void resumeCoordinationPrimaryStack(ActivityRecord activityRecord);

    void saveMultiWindowTipState(String str, int i);

    void setAlwaysOnTopOnly(ActivityDisplay activityDisplay, ActivityStack activityStack, boolean z, boolean z2);

    void setCallingPkg(String str);

    boolean setCustomActivityController(IActivityController iActivityController);

    int[] setFreeformStackVisibility(int i, int[] iArr, boolean z);

    void setHwWinCornerRaduis(WindowState windowState, SurfaceControl surfaceControl);

    boolean setMultiWindowDisabled(boolean z);

    void setRequestedOrientation(int i);

    void setResumedActivityUncheckLocked(ActivityRecord activityRecord, ActivityRecord activityRecord2);

    void setSplitBarVisibility(boolean z);

    boolean shouldPreventSendBroadcast(Intent intent, String str, int i, int i2, String str2, int i3);

    boolean shouldPreventStartActivity(ActivityInfo activityInfo, int i, int i2, String str, int i3, Intent intent, WindowProcessController windowProcessController);

    boolean shouldPreventStartProvider(ProviderInfo providerInfo, int i, int i2, String str, int i3);

    boolean shouldPreventStartService(ServiceInfo serviceInfo, int i, int i2, String str, int i3);

    boolean shouldResumeCoordinationPrimaryStack();

    boolean showIncompatibleAppDialog(ActivityInfo activityInfo, String str);

    void showUninstallLauncherDialog(String str);

    boolean skipOverridePendingTransitionForMagicWindow(ActivityRecord activityRecord);

    boolean skipOverridePendingTransitionForPC(ActivityRecord activityRecord);

    void startExSplash(Bundle bundle, ActivityOptions activityOptions);

    void toggleFreeformWindowingModeEx(ActivityRecord activityRecord);

    void toggleHome();

    void togglePCMode(boolean z, int i);

    void unRegisterHwTaskStackListener(ITaskStackListener iTaskStackListener);

    void unregisterGameObserver(IGameObserver iGameObserver);

    void unregisterGameObserverEx(IGameObserverEx iGameObserverEx);

    void unregisterHwActivityNotifier(IHwActivityNotifier iHwActivityNotifier);

    boolean unregisterThirdPartyCallBack(IMWThirdpartyCallback iMWThirdpartyCallback);

    void updateDragFreeFormPos(Rect rect, ActivityDisplay activityDisplay);

    void updateFreeFormOutLine(int i);

    void updateSplitBarPosForIm(int i);

    void updateUsageStatsForPCMode(ActivityRecord activityRecord, boolean z, UsageStatsManagerInternal usageStatsManagerInternal);
}
