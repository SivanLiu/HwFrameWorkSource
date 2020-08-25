package com.android.server.wm;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Binder;
import android.os.UserHandle;
import android.util.HwMwUtils;
import android.util.HwPCUtils;
import android.util.Slog;
import com.huawei.forcerotation.HwForceRotationManager;
import com.huawei.server.HwPCFactory;

public class HwActivityRecord extends ActivityRecord {
    public long mCreateTime;
    int mCustomRequestedOrientation;
    public boolean mIsAniRunningBelow;
    public boolean mIsFinishAllRightBottom;
    public boolean mIsFullScreenVideoInLandscape;
    private boolean mIsStartFromLauncher;
    private int mLastActivityHash;
    public Rect mLastBound;
    public int mMagicWindowPageType;
    private boolean mSplitMode;

    public HwActivityRecord(ActivityTaskManagerService _service, WindowProcessController _caller, int _launchedFromPid, int _launchedFromUid, String _launchedFromPackage, Intent _intent, String _resolvedType, ActivityInfo aInfo, Configuration _configuration, ActivityRecord _resultTo, String _resultWho, int _reqCode, boolean _componentSpecified, boolean _rootVoiceInteraction, ActivityStackSupervisor supervisor, ActivityOptions options, ActivityRecord sourceRecord) {
        super(_service, _caller, _launchedFromPid, _launchedFromUid, _launchedFromPackage, _intent, _resolvedType, aInfo, _configuration, _resultTo, _resultWho, _reqCode, _componentSpecified, _rootVoiceInteraction, supervisor, options, sourceRecord);
        this.mMagicWindowPageType = 0;
        this.mIsFullScreenVideoInLandscape = false;
        this.mIsFinishAllRightBottom = false;
        this.mIsAniRunningBelow = false;
        this.mCreateTime = System.currentTimeMillis();
        this.mLastActivityHash = -1;
        this.mIsStartFromLauncher = false;
        this.mCustomRequestedOrientation = 0;
        this.mIsFullScreenVideoInLandscape = ActivityInfo.isFixedOrientationLandscape(this.info.screenOrientation);
    }

    /* access modifiers changed from: package-private */
    public void scheduleMultiWindowModeChanged(Configuration overrideConfig) {
        if (!inHwMagicWindowingMode()) {
            HwActivityRecord.super.scheduleMultiWindowModeChanged(overrideConfig);
            if (!HwMultiWindowManager.IS_HW_MULTIWINDOW_SUPPORTED) {
                this.mAtmService.onMultiWindowModeChanged(inMultiWindowMode());
            }
        }
    }

    /* access modifiers changed from: protected */
    public void initSplitMode(Intent intent) {
        if (intent != null) {
            this.mSplitMode = (intent.getHwFlags() & 4) != 0 && (intent.getHwFlags() & 8) == 0;
        }
    }

    /* access modifiers changed from: protected */
    public boolean isSplitMode() {
        return this.mSplitMode;
    }

    public void schedulePCWindowStateChanged() {
        if (this.task != null && this.task.getStack() != null && this.app != null && this.app.mThread != null) {
            try {
                this.app.mThread.schedulePCWindowStateChanged(this.appToken, this.task.getWindowState());
            } catch (Exception e) {
                Slog.d("HwActivityRecord", "on schedulePCWindowStateChanged error");
            }
        }
    }

    /* access modifiers changed from: protected */
    public void computeBounds(Rect outBounds, Rect containingAppBounds) {
        if (this.task == null || !HwPCUtils.isExtDynamicStack(this.task.getStackId())) {
            HwActivityRecord.super.computeBounds(outBounds, containingAppBounds);
        } else {
            outBounds.setEmpty();
        }
    }

    /* access modifiers changed from: package-private */
    public void setRequestedOrientation(int requestedOrientation) {
        DefaultHwPCMultiWindowManager multiWindowMgr;
        if (HwMwUtils.ENABLED) {
            if (HwMwUtils.performPolicy(41, new Object[]{this.appToken, Integer.valueOf(requestedOrientation)}).getBoolean("RESULT_REJECT_ORIENTATION", false)) {
                return;
            }
        }
        HwActivityRecord.super.setRequestedOrientation(requestedOrientation);
        HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, "requestedOrientation: " + requestedOrientation);
        if (HwPCUtils.enabledInPad() && HwPCUtils.isExtDynamicStack(this.task.getStackId()) && this.task.getTopActivity() == this && (multiWindowMgr = getHwPCMultiWindowManager(buildAtmsEx())) != null) {
            if (multiWindowMgr.isFixedOrientationPortrait(requestedOrientation)) {
                this.mCustomRequestedOrientation = 1;
            } else if (multiWindowMgr.isFixedOrientationLandscape(requestedOrientation)) {
                this.mCustomRequestedOrientation = 2;
            }
            HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, "doCustomRequestedOrientation: " + this.mCustomRequestedOrientation + " (" + toString() + ")");
            multiWindowMgr.updateTaskByRequestedOrientation(buildTaskRecordEx(this.task), this.mCustomRequestedOrientation);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isForceRotationMode(String packageName, Intent _intent) {
        HwForceRotationManager forceRotationManager = HwForceRotationManager.getDefault();
        if (!forceRotationManager.isForceRotationSupported() || !forceRotationManager.isForceRotationSwitchOpen() || UserHandle.isIsolated(Binder.getCallingUid())) {
            return false;
        }
        boolean isAppInForceRotationWhiteList = false;
        if (packageName != null) {
            isAppInForceRotationWhiteList = forceRotationManager.isAppInForceRotationWhiteList(packageName);
        }
        boolean isFirstActivity = (_intent.getFlags() & 67108864) != 0;
        if (!isFirstActivity && _intent.getCategories() != null) {
            isFirstActivity = _intent.getCategories().contains("android.intent.category.LAUNCHER");
        }
        if (!isAppInForceRotationWhiteList || !isFirstActivity) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public int overrideRealConfigChanged(ActivityInfo info) {
        int realConfigChange = info.getRealConfigChanged();
        HwForceRotationManager forceRotationManager = HwForceRotationManager.getDefault();
        if (forceRotationManager.isForceRotationSupported() && forceRotationManager.isForceRotationSwitchOpen() && !inMultiWindowMode() && forceRotationManager.isAppInForceRotationWhiteList(info.packageName)) {
            return realConfigChange | 3232;
        }
        return realConfigChange;
    }

    /* access modifiers changed from: protected */
    public int getConfigurationChanges(Configuration lastReportedConfig) {
        if (HwPCUtils.isExtDynamicStack(getStackId())) {
            int changes = lastReportedConfig.diff(getConfiguration());
            if (((changes & 4) | (1073741824 & changes)) == 0) {
                return 0;
            }
        }
        int changes2 = HwActivityRecord.super.getConfigurationChanges(lastReportedConfig);
        if (!HwMwUtils.ENABLED || !HwMwUtils.IS_FOLD_SCREEN_DEVICE || (this.info.getRealConfigChanged() & 2048) != 0) {
            return changes2;
        }
        Configuration currentConfig = getConfiguration();
        if (lastReportedConfig.windowConfiguration.getWindowingMode() == 103 && currentConfig.windowConfiguration.getWindowingMode() == 1 && currentConfig.orientation == 2) {
            changes2 &= -2049;
        }
        if (lastReportedConfig.windowConfiguration.getWindowingMode() == 1 && lastReportedConfig.orientation == 2 && currentConfig.windowConfiguration.getWindowingMode() == 103) {
            return changes2 & -2049;
        }
        return changes2;
    }

    public String getPackageName() {
        return this.packageName;
    }

    /* access modifiers changed from: protected */
    public boolean isSplitBaseActivity() {
        ActivityRecord lastResumed = this.mAtmService.getLastResumedActivityRecord();
        return lastResumed != null && lastResumed.isSplitMode() && lastResumed.getTaskRecord() != null && lastResumed.getTaskRecord() == getTaskRecord() && this == getTaskRecord().getRootActivity();
    }

    public int getWindowState() {
        if (this.task == null) {
            return -1;
        }
        return this.task.getWindowState();
    }

    public void resize() {
        HwActivityRecord.super.updateOverrideConfiguration();
        if (this.mAppWindowToken != null) {
            this.mAppWindowToken.resize();
        }
    }

    public void setLastActivityHash(int hashValue) {
        this.mLastActivityHash = hashValue;
    }

    public int getLastActivityHash() {
        return this.mLastActivityHash;
    }

    public boolean isStartFromLauncher() {
        return this.mIsStartFromLauncher;
    }

    public void setIsStartFromLauncher(boolean isStartFromLauncher) {
        this.mIsStartFromLauncher = isStartFromLauncher;
    }

    private TaskRecordEx buildTaskRecordEx(TaskRecord taskRecord) {
        TaskRecordEx taskRecordEx = new TaskRecordEx();
        taskRecordEx.setTaskRecord(taskRecord);
        return taskRecordEx;
    }

    private ActivityTaskManagerServiceEx buildAtmsEx() {
        ActivityTaskManagerServiceEx atmsEx = new ActivityTaskManagerServiceEx();
        atmsEx.setActivityTaskManagerService(this.mAtmService);
        return atmsEx;
    }

    private DefaultHwPCMultiWindowManager getHwPCMultiWindowManager(ActivityTaskManagerServiceEx atmsEx) {
        return HwPCFactory.getHwPCFactory().getHwPCFactoryImpl().getHwPCMultiWindowManager(atmsEx);
    }
}
