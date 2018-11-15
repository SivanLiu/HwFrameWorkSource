package com.android.server.mtm.utils;

import android.os.SystemClock;
import android.os.UserHandle;
import android.rms.iaware.AppTypeRecoManager;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.am.HwActivityManagerService;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.mtm.iaware.appmng.AwareProcessBaseInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfoCollector;
import com.android.server.rms.algorithm.AwareUserHabit;
import com.android.server.rms.iaware.appmng.AppMngConfig;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup;
import com.android.server.rms.iaware.appmng.AwareAppLruBase;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.security.tsmagent.logic.spi.tsm.laser.LaserTSMServiceImpl;
import huawei.com.android.server.fingerprint.FingerViewController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

public final class AppStatusUtils {
    private static final String TAG = "AppStatusUtils";
    private static volatile AppStatusUtils instance;
    private final String ADJTYPE_SERVICE = AwareAppMngSort.ADJTYPE_SERVICE;
    private final long EXPIRE_TIME_NANOS = 10000000;
    private final String FG_SERVICE = AwareAppMngSort.FG_SERVICE;
    private final String RECENT_TASK_ADJ_TYPE = "pers-top-activity";
    private final String SYSTEMUI_PACKAGE_NAME = FingerViewController.PKGNAME_OF_KEYGUARD;
    private final String TOP_ACTIVITY_ADJ_TYPE = "top-activity";
    private ArrayMap<Integer, AwareProcessInfo> mAllProcNeedSort = null;
    private ArrayMap<Integer, ProcessInfo> mAudioIn = null;
    private ArrayMap<Integer, ProcessInfo> mAudioOut = null;
    private Set<String> mBlindPkg = new ArraySet();
    private int mCurrentUserId = 0;
    private ArraySet<Integer> mForeGroundAssocPid = null;
    private ArrayMap<Integer, ArrayList<String>> mForeGroundUid = null;
    private List<String> mGcmAppList = null;
    private final HwActivityManagerService mHwAMS = HwActivityManagerService.self();
    private final AwareAppKeyBackgroup mKeyBackgroupInstance = AwareAppKeyBackgroup.getInstance();
    private Set<Integer> mKeyPercepServicePid = null;
    private AwareAppLruBase mPrevAmsBase = null;
    private AwareAppLruBase mPrevAwareBase = null;
    private Set<String> mRestrainedVisWinList = null;
    private ProcessInfo mSystemuiProcInfo = null;
    private volatile long mUpdateTime = -1;
    private Set<String> mVisibleWindowList = null;
    private Set<String> mWidgetList = null;
    private volatile Map<Status, Predicate<AwareProcessInfo>> predicates = new HashMap();

    public enum Status {
        NOT_IMPORTANT("not_important"),
        PERSIST("Persist"),
        TOP_ACTIVITY("TopActivity"),
        VISIBLE_APP("VisApp"),
        FOREGROUND("Fground"),
        VISIBLEWIN("VisWin"),
        WIDGET("Widget"),
        ASSOC_WITH_FG("FgAssoc"),
        KEYBACKGROUND("KeyBground"),
        BACKUP_APP("BackApp"),
        HEAVY_WEIGHT_APP("HeaWeiApp"),
        PREV_ONECLEAN("RecTask"),
        MUSIC_PLAY("Music"),
        SOUND_RECORD("Record"),
        GUIDE("Guide"),
        DOWN_UP_LOAD("Download"),
        KEY_SYS_SERVICE("KeySys"),
        HEALTH("Health"),
        PREVIOUS("LastUse"),
        SYSTEM_APP("SysApp"),
        NON_CURRENT_USER("non_cur_user"),
        LAUNCHER("Launcher"),
        INPUT_METHOD("InputMethed"),
        WALLPAPER("WallPaper"),
        FROZEN("Frozen"),
        BLUETOOTH("Btooth"),
        TOAST_WINDOW("ToastWin"),
        FOREGROUND_APP("FgApp"),
        BLIND("Blind"),
        MUSIC_INSTANT("MusicINS"),
        NONSYSTEMUSER("NonSystemUser"),
        GCM("gcm"),
        INSMALLSAMPLELIST("InSmallSampleList"),
        ACHSCRCHANGEDNUM("AchScrChangedNum"),
        SCREENRECORD("ScreenRecord"),
        CAMERARECORD("CameraRecord"),
        RESTRAINED_VIS_WIN("RestrainedVisWin"),
        OVERSEA_APP("OverseaApp"),
        DOWNLOAD_FREEZE("DownloadFreeze"),
        PROC_STATE_CACHED("PSCached"),
        REG_KEEP_ALIVER_APP("RegKeepAlive");
        
        private String mDescription;

        private Status(String description) {
            this.mDescription = description;
        }

        public String description() {
            return this.mDescription;
        }
    }

    private AppStatusUtils() {
        this.predicates.put(Status.PERSIST, -$$Lambda$AppStatusUtils$pzX_iOvm521ZtH93faodknz7Hwg.INSTANCE);
        this.predicates.put(Status.TOP_ACTIVITY, -$$Lambda$AppStatusUtils$gYXvacvzzxawxrbmBjc_KJ5lztE.INSTANCE);
        this.predicates.put(Status.VISIBLE_APP, -$$Lambda$AppStatusUtils$7E2bYrJ-nbiYmh8toQkk1ZP4THU.INSTANCE);
        this.predicates.put(Status.FOREGROUND, -$$Lambda$AppStatusUtils$6urJlXXNXnVV2wx0LEBFOLgcMzM.INSTANCE);
        this.predicates.put(Status.VISIBLEWIN, new -$$Lambda$AppStatusUtils$BBdDVstlycCdsjB1Z_n37eOuPtQ(this));
        this.predicates.put(Status.WIDGET, new -$$Lambda$AppStatusUtils$-wSMzBKXVrqfFy3yPgdKTMxl76I(this));
        this.predicates.put(Status.ASSOC_WITH_FG, new -$$Lambda$AppStatusUtils$LXoeYUH3Tnmn3-9sdWH8-ypiQc8(this));
        this.predicates.put(Status.KEYBACKGROUND, new -$$Lambda$AppStatusUtils$9m0_t0g7j2ozX3QSPiUIXp1uFPw(this));
        this.predicates.put(Status.BACKUP_APP, -$$Lambda$AppStatusUtils$evEKtntBZDMPoH3rjBl76lj0yc4.INSTANCE);
        this.predicates.put(Status.HEAVY_WEIGHT_APP, -$$Lambda$AppStatusUtils$iUWENTOSCiKEf90R5ewWLgDRytY.INSTANCE);
        this.predicates.put(Status.PREV_ONECLEAN, new -$$Lambda$AppStatusUtils$I64CBBsc_em4fX-qTHLTCMvH9lk(this));
        this.predicates.put(Status.MUSIC_PLAY, new -$$Lambda$AppStatusUtils$IDrZ1ChK8_0DKIr3ux1Rs4ykNYw(this));
        this.predicates.put(Status.SOUND_RECORD, new -$$Lambda$AppStatusUtils$r6ymesnzOOuAjDdMCL0UBLamZjI(this));
        this.predicates.put(Status.GUIDE, new -$$Lambda$AppStatusUtils$5Sv82n-q6SOV4zOWz-Ms5jVh-xE(this));
        this.predicates.put(Status.DOWN_UP_LOAD, new -$$Lambda$AppStatusUtils$1OSXMZtkUQfJg_ieCG7p7IWxEBw(this));
        this.predicates.put(Status.KEY_SYS_SERVICE, new -$$Lambda$AppStatusUtils$e5_Etqe5ZmGUkYQCLJdHewv4Vdw(this));
        this.predicates.put(Status.HEALTH, new -$$Lambda$AppStatusUtils$w7JYeLL-m6PZn1Cbb77BFG7yIqI(this));
        this.predicates.put(Status.PREVIOUS, new -$$Lambda$AppStatusUtils$M5AZQ16dAqNZO-0We02bFPDUwY8(this));
        this.predicates.put(Status.SYSTEM_APP, new -$$Lambda$AppStatusUtils$VL_giPGf17yhPujz4nKOAFngil0(this));
        this.predicates.put(Status.NON_CURRENT_USER, new -$$Lambda$AppStatusUtils$hgZani-4SDiLI40CvUoeevoHtQM(this));
        this.predicates.put(Status.LAUNCHER, new -$$Lambda$AppStatusUtils$oEgbwoM2xNwwH--OcAJGPDhhdVk(this));
        this.predicates.put(Status.INPUT_METHOD, new -$$Lambda$AppStatusUtils$mx3ag2ar_Lu7P1AFajNAyghXTfs(this));
        this.predicates.put(Status.WALLPAPER, new -$$Lambda$AppStatusUtils$2jvPXACn924X7xPIa0HAz9UCqH4(this));
        this.predicates.put(Status.FROZEN, new -$$Lambda$AppStatusUtils$hS4gtSjisBAzW0qB3LZZAc3oap4(this));
        this.predicates.put(Status.BLUETOOTH, new -$$Lambda$AppStatusUtils$9Ux1hC1YzTJQz6-r0sBsBWqUpek(this));
        this.predicates.put(Status.TOAST_WINDOW, new -$$Lambda$AppStatusUtils$fTaa-6uSdQMuZ-M5Gg7ICekdMUo(this));
        this.predicates.put(Status.FOREGROUND_APP, new -$$Lambda$AppStatusUtils$6NjWgXZ6NTaoRAmdPki8abYLj6o(this));
        this.predicates.put(Status.BLIND, new -$$Lambda$AppStatusUtils$lQ00MtwluoSi7S3gkjgMQp5i3dc(this));
        this.predicates.put(Status.MUSIC_INSTANT, new -$$Lambda$AppStatusUtils$TlMtbV7aPPd-nnyuRLDck7Uywz4(this));
        this.predicates.put(Status.NONSYSTEMUSER, new -$$Lambda$AppStatusUtils$xuQ6_nhCyJ0OCThhVRzWWw-V70k(this));
        this.predicates.put(Status.GCM, new -$$Lambda$AppStatusUtils$9AhkQd7i_a2cxpyZ-OnFPZC9o18(this));
        this.predicates.put(Status.INSMALLSAMPLELIST, new -$$Lambda$AppStatusUtils$jvMnBNmTGKB4E9xxLKXtvbnff60(this));
        this.predicates.put(Status.ACHSCRCHANGEDNUM, new -$$Lambda$AppStatusUtils$l6TyHq-xAZS1jSUVNCgiuHMAWqg(this));
        this.predicates.put(Status.SCREENRECORD, new -$$Lambda$AppStatusUtils$_izxbGuTXo6Xb6qC_wXWOICm56c(this));
        this.predicates.put(Status.CAMERARECORD, new -$$Lambda$AppStatusUtils$F6QyQpW2rfN0QVP-5MrVZR5eAeY(this));
        this.predicates.put(Status.RESTRAINED_VIS_WIN, new -$$Lambda$AppStatusUtils$Ug8kiYuBo8Lj0MRyuhHxqORp0To(this));
        this.predicates.put(Status.OVERSEA_APP, new -$$Lambda$AppStatusUtils$trmeLsS860uRpLFTVj211qaRIjQ(this));
        this.predicates.put(Status.DOWNLOAD_FREEZE, new -$$Lambda$AppStatusUtils$Ci9XXk4cIZ5GHuom6L0c-_P6fUE(this));
        this.predicates.put(Status.PROC_STATE_CACHED, -$$Lambda$AppStatusUtils$zeTic1YZ3MP4rMTOde-s0tYmSXc.INSTANCE);
        this.predicates.put(Status.REG_KEEP_ALIVER_APP, new -$$Lambda$AppStatusUtils$WUNj4fhdruTKJ248w50w6P87wNE(this));
    }

    static /* synthetic */ boolean lambda$new$0(AwareProcessInfo param) {
        return param.mProcInfo.mCurAdj < 0;
    }

    static /* synthetic */ boolean lambda$new$1(AwareProcessInfo param) {
        return param.mProcInfo.mCurAdj == 0;
    }

    static /* synthetic */ boolean lambda$new$2(AwareProcessInfo param) {
        return param.mProcInfo.mCurAdj >= 100 && param.mProcInfo.mCurAdj < 200;
    }

    static /* synthetic */ boolean lambda$new$3(AwareProcessInfo param) {
        return param.mProcInfo.mCurAdj < 200;
    }

    static /* synthetic */ boolean lambda$new$8(AwareProcessInfo param) {
        return param.mProcInfo.mCurAdj == 300;
    }

    static /* synthetic */ boolean lambda$new$9(AwareProcessInfo param) {
        return param.mProcInfo.mCurAdj == 400;
    }

    static /* synthetic */ boolean lambda$new$38(AwareProcessInfo param) {
        return param.mProcInfo.mSetProcState > 13;
    }

    public static AppStatusUtils getInstance() {
        if (instance == null) {
            synchronized (AppStatusUtils.class) {
                if (instance == null) {
                    instance = new AppStatusUtils();
                }
            }
        }
        return instance;
    }

    public boolean checkAppStatus(Status statusType, AwareProcessInfo awareProcInfo) {
        if (awareProcInfo == null) {
            AwareLog.e(TAG, "null AwareProcessInfo input!");
            return false;
        } else if (awareProcInfo.mProcInfo == null) {
            AwareLog.e(TAG, "null ProcessInfo input!");
            return false;
        } else if (-1 == awareProcInfo.mProcInfo.mCurAdj) {
            AwareLog.e(TAG, "mCurAdj of ProcessInfo not set!");
            return false;
        } else {
            Predicate<AwareProcessInfo> func = (Predicate) this.predicates.get(statusType);
            if (func == null) {
                AwareLog.e(TAG, "error status type input!");
                return false;
            } else if (getNewProcesses()) {
                ArrayMap<Integer, AwareProcessInfo> tmpAllProcNeedSort = this.mAllProcNeedSort;
                if (tmpAllProcNeedSort == null || tmpAllProcNeedSort.isEmpty()) {
                    AwareLog.e(TAG, "mAllProcNeedSort is null");
                    return false;
                }
                AwareProcessInfo newAwareProcInfo = (AwareProcessInfo) tmpAllProcNeedSort.get(Integer.valueOf(awareProcInfo.mPid));
                if (newAwareProcInfo != null) {
                    return func.test(newAwareProcInfo);
                }
                return func.test(awareProcInfo);
            } else {
                AwareLog.e(TAG, "update processes status failed!");
                return false;
            }
        }
    }

    private boolean checkSystemApp(AwareProcessInfo awareProcInfo) {
        int uid = awareProcInfo.mProcInfo.mUid;
        int tmpCurrentUserId = this.mCurrentUserId;
        if (uid <= 10000) {
            return true;
        }
        if (uid <= tmpCurrentUserId * LaserTSMServiceImpl.EXCUTE_OTA_RESULT_SUCCESS || uid > (LaserTSMServiceImpl.EXCUTE_OTA_RESULT_SUCCESS * tmpCurrentUserId) + 10000) {
            return false;
        }
        return true;
    }

    private boolean checkNonCurrentUser(AwareProcessInfo awareProcInfo) {
        return AwareIntelligentRecg.getInstance().isCurrentUser(awareProcInfo.mProcInfo.mUid, this.mCurrentUserId) ^ 1;
    }

    private boolean checkPrevOneClean(AwareProcessInfo awareProcInfo) {
        ProcessInfo tmpSystemuiProcInfo = this.mSystemuiProcInfo;
        int tmpCurrentUserId = this.mCurrentUserId;
        if (tmpSystemuiProcInfo == null) {
            return false;
        }
        if (tmpCurrentUserId == 0) {
            if (!"pers-top-activity".equals(tmpSystemuiProcInfo.mAdjType)) {
                return false;
            }
        } else if (!"top-activity".equals(tmpSystemuiProcInfo.mAdjType)) {
            return false;
        }
        AwareAppLruBase tmpAppLruBase = this.mPrevAmsBase;
        if (tmpAppLruBase == null) {
            return false;
        }
        ArrayList<String> packageNames = awareProcInfo.mProcInfo.mPackageName;
        if (packageNames == null || packageNames.isEmpty()) {
            return false;
        }
        ArrayMap<Integer, AwareProcessInfo> allProcNeedSort = this.mAllProcNeedSort;
        if (allProcNeedSort == null || allProcNeedSort.isEmpty()) {
            AwareLog.e(TAG, "mAllProcNeedSort is null");
            return false;
        } else if (awareProcInfo.mProcInfo.mUid != tmpAppLruBase.mUid) {
            return false;
        } else {
            if (!AwareAppAssociate.isDealAsPkgUid(awareProcInfo.mProcInfo.mUid)) {
                return true;
            }
            AwareProcessInfo prevProcInfo = (AwareProcessInfo) allProcNeedSort.get(Integer.valueOf(tmpAppLruBase.mPid));
            if (prevProcInfo == null) {
                return false;
            }
            return isPkgIncludeForTgt(packageNames, prevProcInfo.mProcInfo.mPackageName);
        }
    }

    private boolean checkAssocWithFg(AwareProcessInfo awareProcInfo) {
        ArrayMap<Integer, ArrayList<String>> tmpForeGroundUid = this.mForeGroundUid;
        ArraySet<Integer> tmpForeGroundAssocPid = this.mForeGroundAssocPid;
        if (tmpForeGroundUid == null) {
            return false;
        }
        if ((!tmpForeGroundUid.containsKey(Integer.valueOf(awareProcInfo.mProcInfo.mUid)) && !tmpForeGroundAssocPid.contains(Integer.valueOf(awareProcInfo.mProcInfo.mPid))) || awareProcInfo.mProcInfo.mForegroundActivities) {
            return false;
        }
        if (!AwareAppAssociate.isDealAsPkgUid(awareProcInfo.mProcInfo.mUid)) {
            return true;
        }
        return isPkgIncludeForTgt(awareProcInfo.mProcInfo.mPackageName, (ArrayList) tmpForeGroundUid.get(Integer.valueOf(awareProcInfo.mProcInfo.mUid)));
    }

    /* JADX WARNING: Removed duplicated region for block: B:25:0x004a A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean checkKeySysProc(AwareProcessInfo awareProcInfo) {
        ProcessInfo procInfo = awareProcInfo.mProcInfo;
        if (procInfo.mType != 2) {
            return false;
        }
        if (procInfo.mCurAdj == 500) {
            return true;
        }
        boolean condition = (awareProcInfo.mHasShownUi || procInfo.mCreatedTime == -1) ? false : true;
        long timeCost = SystemClock.elapsedRealtime() - procInfo.mCreatedTime;
        if (procInfo.mUid < 10000) {
            if (procInfo.mCurAdj == 800) {
                return true;
            }
            if (!condition || timeCost >= AppMngConfig.getKeySysDecay()) {
                return false;
            }
            return true;
        } else if (condition && timeCost < AppMngConfig.getSysDecay()) {
            return true;
        }
        return false;
    }

    private boolean checkVisibleWindow(AwareProcessInfo awareProcInfo) {
        Set<String> tmpVisibleWindowList = this.mVisibleWindowList;
        if (tmpVisibleWindowList == null || tmpVisibleWindowList.isEmpty()) {
            return false;
        }
        ProcessInfo procInfo = awareProcInfo.mProcInfo;
        if (procInfo.mPackageName == null) {
            return false;
        }
        Iterator it = procInfo.mPackageName.iterator();
        while (it.hasNext()) {
            if (tmpVisibleWindowList.contains((String) it.next())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkRestrainedVisWin(AwareProcessInfo awareProcInfo) {
        Set<String> tmpRestrainedVisWinList = this.mRestrainedVisWinList;
        if (tmpRestrainedVisWinList == null || tmpRestrainedVisWinList.isEmpty()) {
            return false;
        }
        ProcessInfo procInfo = awareProcInfo.mProcInfo;
        if (procInfo.mPackageName == null) {
            return false;
        }
        Iterator it = procInfo.mPackageName.iterator();
        while (it.hasNext()) {
            if (tmpRestrainedVisWinList.contains((String) it.next())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkWidget(AwareProcessInfo awareProcInfo) {
        Set<String> tmpWidgetList = this.mWidgetList;
        if (tmpWidgetList == null || tmpWidgetList.isEmpty()) {
            return false;
        }
        ProcessInfo procInfo = awareProcInfo.mProcInfo;
        if (procInfo.mPackageName == null) {
            return false;
        }
        Iterator it = procInfo.mPackageName.iterator();
        while (it.hasNext()) {
            if (tmpWidgetList.contains((String) it.next())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkSoundRecord(AwareProcessInfo awareProcInfo) {
        ArrayMap<Integer, ProcessInfo> tmpAudioIn = this.mAudioIn;
        if (tmpAudioIn == null) {
            return false;
        }
        for (Entry<Integer, ProcessInfo> m : tmpAudioIn.entrySet()) {
            ProcessInfo info = (ProcessInfo) m.getValue();
            if (awareProcInfo.mProcInfo.mPid == info.mPid) {
                return true;
            }
            if (awareProcInfo.mProcInfo.mUid == info.mUid) {
                if (!AwareAppAssociate.isDealAsPkgUid(awareProcInfo.mProcInfo.mUid) || isPkgIncludeForTgt(awareProcInfo.mProcInfo.mPackageName, info.mPackageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkMusicPlay(AwareProcessInfo awareProcInfo) {
        ArrayMap<Integer, ProcessInfo> tmpAudioOut = this.mAudioOut;
        if (tmpAudioOut == null) {
            return false;
        }
        for (Entry<Integer, ProcessInfo> m : tmpAudioOut.entrySet()) {
            ProcessInfo info = (ProcessInfo) m.getValue();
            if (awareProcInfo.mProcInfo.mUid == info.mUid) {
                if (!AwareAppAssociate.isDealAsPkgUid(awareProcInfo.mProcInfo.mUid) || isPkgIncludeForTgt(awareProcInfo.mProcInfo.mPackageName, info.mPackageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkKeyBackGround(AwareProcessInfo awareProcInfo) {
        ProcessInfo procInfo = awareProcInfo.mProcInfo;
        if (procInfo.mCurAdj != 200) {
            return false;
        }
        if (!AwareAppMngSort.FG_SERVICE.equals(procInfo.mAdjType) && !AwareAppMngSort.ADJTYPE_SERVICE.equals(procInfo.mAdjType)) {
            return true;
        }
        Set<Integer> tmpKeyPercepServicePid = this.mKeyPercepServicePid;
        if (tmpKeyPercepServicePid != null && tmpKeyPercepServicePid.contains(Integer.valueOf(procInfo.mPid))) {
            return true;
        }
        return false;
    }

    private boolean checkPrevious(AwareProcessInfo awareProcInfo) {
        if (awareProcInfo.mProcInfo.mCurAdj == 700) {
            return true;
        }
        if (awareProcInfo.mProcInfo.mCurAdj < 200) {
            return false;
        }
        ArrayList<String> packageNames = awareProcInfo.mProcInfo.mPackageName;
        if (packageNames == null || packageNames.isEmpty()) {
            return false;
        }
        AwareAppLruBase tmpAppLruBase = this.mPrevAwareBase;
        if (tmpAppLruBase == null) {
            return false;
        }
        ArrayMap<Integer, AwareProcessInfo> allProcNeedSort = this.mAllProcNeedSort;
        if (allProcNeedSort == null || allProcNeedSort.isEmpty()) {
            AwareLog.e(TAG, "mAllProcNeedSort is null");
            return false;
        } else if (awareProcInfo.mProcInfo.mUid != tmpAppLruBase.mUid) {
            return false;
        } else {
            if (!AwareAppAssociate.isDealAsPkgUid(awareProcInfo.mProcInfo.mUid)) {
                return true;
            }
            AwareProcessInfo prevProcInfo = (AwareProcessInfo) allProcNeedSort.get(Integer.valueOf(tmpAppLruBase.mPid));
            if (prevProcInfo == null) {
                return false;
            }
            return isPkgIncludeForTgt(packageNames, prevProcInfo.mProcInfo.mPackageName);
        }
    }

    private boolean checkLauncher(AwareProcessInfo awareProcInfo) {
        return awareProcInfo.mProcInfo.mUid == AwareAppAssociate.getInstance().getCurHomeProcessUid();
    }

    private String getMainPkgName(AwareProcessInfo awareProcInfo) {
        ArrayList<String> packageNames = awareProcInfo.mProcInfo.mPackageName;
        if (packageNames == null || packageNames.isEmpty()) {
            return null;
        }
        return (String) packageNames.get(0);
    }

    private boolean checkInputMethod(AwareProcessInfo awareProcInfo) {
        String mainPackageName = getMainPkgName(awareProcInfo);
        if (mainPackageName == null || !mainPackageName.equals(AwareIntelligentRecg.getInstance().getDefaultInputMethod())) {
            return false;
        }
        return true;
    }

    private boolean checkWallPaper(AwareProcessInfo awareProcInfo) {
        String mainPackageName = getMainPkgName(awareProcInfo);
        if (mainPackageName == null || !mainPackageName.equals(AwareIntelligentRecg.getInstance().getDefaultWallPaper())) {
            return false;
        }
        return true;
    }

    private boolean checkFrozen(AwareProcessInfo awareProcInfo) {
        return AwareIntelligentRecg.getInstance().isAppFrozen(awareProcInfo.mProcInfo.mUid);
    }

    private boolean checkBluetooth(AwareProcessInfo awareProcInfo) {
        return AwareIntelligentRecg.getInstance().isAppBluetooth(awareProcInfo.mProcInfo.mUid);
    }

    private boolean checkDownloadFrozen(AwareProcessInfo awareProcInfo) {
        return AwareIntelligentRecg.getInstance().isNotBeClean(getMainPkgName(awareProcInfo));
    }

    private boolean checkToastWindow(AwareProcessInfo awareProcInfo) {
        return AwareIntelligentRecg.getInstance().isToastWindow(awareProcInfo.mProcInfo.mPid);
    }

    private boolean checkKeyBackgroupByState(int state, AwareProcessInfo awareProcInfo) {
        ProcessInfo procInfo = awareProcInfo.mProcInfo;
        if (this.mKeyBackgroupInstance == null) {
            return false;
        }
        return this.mKeyBackgroupInstance.checkKeyBackgroupByState(state, procInfo.mPid, procInfo.mUid, procInfo.mPackageName);
    }

    public boolean getNewProcesses() {
        if (SystemClock.elapsedRealtimeNanos() - this.mUpdateTime <= 10000000) {
            return true;
        }
        if (this.mHwAMS == null) {
            return false;
        }
        return updateNewProcesses();
    }

    private boolean updateNewProcesses() {
        ArrayList<ProcessInfo> tmp_procInfos = ProcessInfoCollector.getInstance().getProcessInfoList();
        Map<Integer, AwareProcessBaseInfo> tmp_baseInfos = this.mHwAMS.getAllProcessBaseInfo();
        boolean isInfosEmpty = tmp_procInfos.isEmpty() || tmp_baseInfos.isEmpty();
        if (isInfosEmpty) {
            return false;
        }
        boolean isInfosEmpty2;
        ArrayMap<Integer, AwareProcessInfo> tmp_allProcNeedSort = new ArrayMap();
        ArrayMap<Integer, ArrayList<String>> tmp_mForeGroundUid = new ArrayMap();
        ArraySet<Integer> tmp_mForeGroundAssocPid = new ArraySet();
        ArrayMap<Integer, ProcessInfo> tmp_mAudioIn = new ArrayMap();
        ArrayMap<Integer, ProcessInfo> tmp_mAudioOut = new ArrayMap();
        Set<Integer> tmp_keyPercepServicePid = new HashSet();
        Set<Integer> tmp_fgServiceUid = new HashSet();
        ArraySet<Integer> tmp_importUid = new ArraySet();
        ArrayMap<Integer, Integer> tmp_percepServicePid = new ArrayMap();
        int tmp_CurrentUserId = AwareAppAssociate.getInstance().getCurUserId();
        ProcessInfo tmp_systemuiProcInfo = null;
        Iterator it = tmp_procInfos.iterator();
        while (true) {
            ArrayList<ProcessInfo> tmp_procInfos2 = tmp_procInfos;
            if (!it.hasNext()) {
                break;
            }
            ProcessInfo tmp_systemuiProcInfo2 = (ProcessInfo) it.next();
            if (tmp_systemuiProcInfo2 == null) {
                isInfosEmpty2 = isInfosEmpty;
            } else {
                isInfosEmpty2 = isInfosEmpty;
                AwareProcessBaseInfo updateInfo = (AwareProcessBaseInfo) tmp_baseInfos.get(Integer.valueOf(tmp_systemuiProcInfo2.mPid));
                if (updateInfo != null) {
                    Map<Integer, AwareProcessBaseInfo> tmp_baseInfos2 = tmp_baseInfos;
                    tmp_systemuiProcInfo2.mCurAdj = updateInfo.mCurAdj;
                    tmp_systemuiProcInfo2.mSetProcState = updateInfo.mSetProcState;
                    tmp_systemuiProcInfo2.mForegroundActivities = updateInfo.mForegroundActivities;
                    tmp_systemuiProcInfo2.mAdjType = updateInfo.mAdjType;
                    tmp_systemuiProcInfo2.mAppUid = updateInfo.mAppUid;
                    updateForeGroundUid(tmp_systemuiProcInfo2, tmp_mForeGroundUid, tmp_mForeGroundAssocPid);
                    Iterator it2 = it;
                    tmp_baseInfos = new AwareProcessInfo(tmp_systemuiProcInfo2.mPid, tmp_systemuiProcInfo2);
                    tmp_baseInfos.mHasShownUi = updateInfo.mHasShownUi;
                    tmp_allProcNeedSort.put(Integer.valueOf(tmp_systemuiProcInfo2.mPid), tmp_baseInfos);
                    updateAudioIn(tmp_baseInfos, tmp_systemuiProcInfo2, tmp_mAudioIn);
                    updateAudioOut(tmp_baseInfos, tmp_systemuiProcInfo2, tmp_mAudioOut);
                    Map<Integer, AwareProcessBaseInfo> awareProcInfo = tmp_baseInfos;
                    if (tmp_systemuiProcInfo2.mCurAdj < 200) {
                        tmp_importUid.add(Integer.valueOf(tmp_systemuiProcInfo2.mUid));
                    } else {
                        updatePerceptibleApp(tmp_systemuiProcInfo2, tmp_fgServiceUid, tmp_percepServicePid, tmp_importUid);
                    }
                    if (isSystemUI(tmp_systemuiProcInfo2, tmp_CurrentUserId) != null) {
                        tmp_systemuiProcInfo = tmp_systemuiProcInfo2;
                    }
                    tmp_procInfos = tmp_procInfos2;
                    isInfosEmpty = isInfosEmpty2;
                    tmp_baseInfos = tmp_baseInfos2;
                    it = it2;
                }
            }
            tmp_procInfos = tmp_procInfos2;
            isInfosEmpty = isInfosEmpty2;
        }
        isInfosEmpty2 = isInfosEmpty;
        int pid = 0;
        int uid = 0;
        Iterator it3 = tmp_percepServicePid.entrySet().iterator();
        while (it3.hasNext()) {
            int uid2;
            Entry<Integer, Integer> m = (Entry) it3.next();
            pid = ((Integer) m.getKey()).intValue();
            uid = ((Integer) m.getValue()).intValue();
            Iterator it4 = it3;
            if (tmp_importUid.contains(Integer.valueOf(uid))) {
                tmp_keyPercepServicePid.add(Integer.valueOf(pid));
            } else if (tmp_fgServiceUid.contains(Integer.valueOf(uid))) {
                Set<Integer> strong = new ArraySet();
                uid2 = uid;
                AwareAppAssociate.getInstance().getAssocClientListForPid(pid, strong);
                Iterator awareProcInfoItem = strong.iterator();
                while (awareProcInfoItem.hasNext()) {
                    Iterator it5 = awareProcInfoItem;
                    Integer clientPid = (Integer) awareProcInfoItem.next();
                    AwareProcessInfo awareProcInfoItem2 = (AwareProcessInfo) tmp_allProcNeedSort.get(clientPid);
                    if (awareProcInfoItem2 == null) {
                        awareProcInfoItem = it5;
                    } else {
                        Set<Integer> strong2 = strong;
                        if (awareProcInfoItem2.mProcInfo.mCurAdj <= 200) {
                            tmp_keyPercepServicePid.add(Integer.valueOf(pid));
                            break;
                        }
                        awareProcInfoItem = it5;
                        strong = strong2;
                    }
                }
                it3 = it4;
                uid = uid2;
            } else {
                tmp_keyPercepServicePid.add(Integer.valueOf(pid));
            }
            uid2 = uid;
            it3 = it4;
            uid = uid2;
        }
        int i = pid;
        int i2 = uid;
        this.mUpdateTime = SystemClock.elapsedRealtimeNanos();
        this.mCurrentUserId = tmp_CurrentUserId;
        this.mForeGroundUid = tmp_mForeGroundUid;
        this.mForeGroundAssocPid = tmp_mForeGroundAssocPid;
        this.mAllProcNeedSort = tmp_allProcNeedSort;
        this.mAudioIn = tmp_mAudioIn;
        this.mAudioOut = tmp_mAudioOut;
        this.mKeyPercepServicePid = tmp_keyPercepServicePid;
        this.mPrevAmsBase = AwareAppAssociate.getInstance().getPreviousByAmsInfo();
        this.mPrevAwareBase = AwareAppAssociate.getInstance().getPreviousAppInfo();
        this.mSystemuiProcInfo = tmp_systemuiProcInfo;
        updateVisibleWindowList();
        updateWidgetList();
        updateGcm();
        return true;
    }

    private void updateGcm() {
        AwareUserHabit habit = AwareUserHabit.getInstance();
        if (habit != null) {
            List<String> tmp = new ArrayList();
            List<String> result = habit.getGCMAppList();
            if (result != null) {
                tmp.addAll(result);
            }
            this.mGcmAppList = tmp;
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0024, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isSystemUI(ProcessInfo procInfo, int currentUserId) {
        ArrayList<String> packageNames = procInfo.mPackageName;
        if (packageNames == null || packageNames.isEmpty() || !FingerViewController.PKGNAME_OF_KEYGUARD.equals(packageNames.get(0)) || currentUserId != UserHandle.getUserId(procInfo.mUid)) {
            return false;
        }
        return true;
    }

    private void updateAudioOut(AwareProcessInfo awareProcInfo, ProcessInfo procInfo, ArrayMap<Integer, ProcessInfo> tmp_mAudioOut) {
        if (checkKeyBackgroupByState(2, awareProcInfo)) {
            tmp_mAudioOut.put(Integer.valueOf(procInfo.mPid), procInfo);
        }
    }

    private void updateAudioIn(AwareProcessInfo awareProcInfo, ProcessInfo procInfo, ArrayMap<Integer, ProcessInfo> tmp_mAudioIn) {
        if (checkKeyBackgroupByState(1, awareProcInfo)) {
            tmp_mAudioIn.put(Integer.valueOf(procInfo.mPid), procInfo);
        }
    }

    private void updatePerceptibleApp(ProcessInfo procInfo, Set<Integer> tmp_fgServiceUid, ArrayMap<Integer, Integer> tmp_percepServicePid, ArraySet<Integer> tmp_importUid) {
        if (procInfo.mCurAdj != 200) {
            return;
        }
        if (AwareAppMngSort.FG_SERVICE.equals(procInfo.mAdjType)) {
            tmp_fgServiceUid.add(Integer.valueOf(procInfo.mUid));
        } else if (AwareAppMngSort.ADJTYPE_SERVICE.equals(procInfo.mAdjType)) {
            tmp_percepServicePid.put(Integer.valueOf(procInfo.mPid), Integer.valueOf(procInfo.mUid));
        } else {
            tmp_importUid.add(Integer.valueOf(procInfo.mUid));
        }
    }

    private void updateForeGroundUid(ProcessInfo procInfo, ArrayMap<Integer, ArrayList<String>> tmp_mForeGroundUid, ArraySet<Integer> tmp_mForeGroundAssocPid) {
        if (procInfo.mForegroundActivities) {
            AwareAppAssociate.getInstance().getAssocProvider(procInfo.mPid, tmp_mForeGroundAssocPid);
            if (AwareAppAssociate.isDealAsPkgUid(procInfo.mUid)) {
                tmp_mForeGroundUid.put(Integer.valueOf(procInfo.mUid), procInfo.mPackageName);
            } else {
                tmp_mForeGroundUid.put(Integer.valueOf(procInfo.mUid), null);
            }
        }
    }

    private void updateWidgetList() {
        this.mWidgetList = AwareAppAssociate.getInstance().getWidgetsPkg();
    }

    private void updateVisibleWindowList() {
        Set<Integer> visibleWindows = new ArraySet();
        Set<Integer> restrainedVisWins = new ArraySet();
        Set<String> tmpVisibleWindowList = new ArraySet();
        Set<String> tmpRestrainedVisWinList = new ArraySet();
        ArrayMap<Integer, AwareProcessInfo> tmpAllProcNeedSort = this.mAllProcNeedSort;
        if (tmpAllProcNeedSort == null || tmpAllProcNeedSort.isEmpty()) {
            AwareLog.e(TAG, "mAllProcNeedSort is null");
            return;
        }
        AwareAppAssociate.getInstance().getVisibleWindows(visibleWindows, null);
        addVisibleWindowToList(visibleWindows, tmpVisibleWindowList, tmpAllProcNeedSort);
        this.mVisibleWindowList = tmpVisibleWindowList;
        AwareAppAssociate.getInstance().getVisibleWindowsInRestriction(restrainedVisWins);
        addVisibleWindowToList(restrainedVisWins, tmpRestrainedVisWinList, tmpAllProcNeedSort);
        this.mRestrainedVisWinList = tmpRestrainedVisWinList;
    }

    private void addVisibleWindowToList(Set<Integer> visibleWindows, Set<String> visibleWindowList, ArrayMap<Integer, AwareProcessInfo> procNeedSort) {
        for (Integer pid : visibleWindows) {
            AwareProcessInfo awareProcInfo = (AwareProcessInfo) procNeedSort.get(pid);
            if (awareProcInfo != null) {
                if (awareProcInfo.mProcInfo != null) {
                    if (awareProcInfo.mProcInfo.mPackageName != null) {
                        visibleWindowList.addAll(awareProcInfo.mProcInfo.mPackageName);
                    }
                }
            }
        }
    }

    private boolean isPkgIncludeForTgt(ArrayList<String> tgtPkg, ArrayList<String> dstPkg) {
        if (tgtPkg == null || tgtPkg.isEmpty() || dstPkg == null) {
            return false;
        }
        Iterator it = dstPkg.iterator();
        while (it.hasNext()) {
            String pkg = (String) it.next();
            if (pkg != null) {
                if (tgtPkg.contains(pkg)) {
                    return true;
                }
            }
        }
        return false;
    }

    public ArrayList<AwareProcessInfo> getAllProcNeedSort() {
        if (getNewProcesses()) {
            ArrayMap<Integer, AwareProcessInfo> tmpAllProcNeedSort = this.mAllProcNeedSort;
            if (tmpAllProcNeedSort != null && !tmpAllProcNeedSort.isEmpty()) {
                return new ArrayList(tmpAllProcNeedSort.values());
            }
            AwareLog.e(TAG, "mAllProcNeedSort is null");
            return null;
        }
        AwareLog.e(TAG, "update processes status failed!");
        return null;
    }

    private boolean checkForeground(AwareProcessInfo awareProcInfo) {
        Set<Integer> forePids = new ArraySet();
        AwareAppAssociate.getInstance().getForeGroundApp(forePids);
        for (Integer pid : forePids) {
            if (pid.equals(Integer.valueOf(awareProcInfo.mPid))) {
                return true;
            }
        }
        return false;
    }

    private boolean checkBlind(AwareProcessInfo awareProcInfo) {
        ProcessInfo procInfo = awareProcInfo.mProcInfo;
        if (procInfo.mPackageName == null) {
            return false;
        }
        Iterator it = procInfo.mPackageName.iterator();
        while (it.hasNext()) {
            if (this.mBlindPkg.contains((String) it.next())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkMusicInstant(AwareProcessInfo awareProcInfo) {
        ProcessInfo procInfo = awareProcInfo.mProcInfo;
        if (this.mKeyBackgroupInstance == null || procInfo == null) {
            return false;
        }
        return this.mKeyBackgroupInstance.checkAudioOutInstant(procInfo.mPid, procInfo.mUid, procInfo.mPackageName);
    }

    private boolean checkNonSystemUser(AwareProcessInfo awareProcInfo) {
        if (awareProcInfo == null) {
            return false;
        }
        return AwareAppMngSort.getInstance().checkNonSystemUser(awareProcInfo);
    }

    private boolean checkInSmallSampleList(AwareProcessInfo awareProcInfo) {
        if (awareProcInfo == null) {
            return false;
        }
        return AwareIntelligentRecg.getInstance().isInSmallSampleList(awareProcInfo);
    }

    private boolean checkScreenChangedAcheive(AwareProcessInfo awareProcInfo) {
        if (awareProcInfo == null) {
            return false;
        }
        return AwareIntelligentRecg.getInstance().isAchScreenChangedNum(awareProcInfo);
    }

    private boolean checkScreenRecord(AwareProcessInfo awareProcInfo) {
        if (awareProcInfo == null) {
            return false;
        }
        return AwareIntelligentRecg.getInstance().isScreenRecord(awareProcInfo);
    }

    private boolean checkCameraRecord(AwareProcessInfo awareProcInfo) {
        if (awareProcInfo == null) {
            return false;
        }
        return AwareIntelligentRecg.getInstance().isCameraRecord(awareProcInfo);
    }

    public void updateBlind(Set<String> blindPkgs) {
        if (blindPkgs == null) {
            this.mBlindPkg = new ArraySet();
        } else {
            this.mBlindPkg = blindPkgs;
        }
    }

    /* JADX WARNING: Missing block: B:19:0x0034, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean checkGcm(AwareProcessInfo awareProcInfo) {
        if (awareProcInfo == null || awareProcInfo.mProcInfo == null || this.mGcmAppList == null || awareProcInfo.mProcInfo.mPackageName == null) {
            return false;
        }
        Iterator it = awareProcInfo.mProcInfo.mPackageName.iterator();
        while (it.hasNext()) {
            if (this.mGcmAppList.contains((String) it.next())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkOverseaApp(AwareProcessInfo awareProcInfo) {
        if (awareProcInfo == null || awareProcInfo.mProcInfo == null) {
            return false;
        }
        boolean result = false;
        Iterator it = awareProcInfo.mProcInfo.mPackageName.iterator();
        while (it.hasNext()) {
            if (AppTypeRecoManager.getInstance().getAppWhereFrom((String) it.next()) != 0) {
                result = true;
                break;
            }
        }
        return result;
    }

    private boolean checkIsRegKeepALive(AwareProcessInfo awareProcInfo) {
        if (awareProcInfo == null || awareProcInfo.mProcInfo == null) {
            return false;
        }
        return AwareIntelligentRecg.getInstance().isCurUserKeepALive(getMainPkgName(awareProcInfo), awareProcInfo.mProcInfo.mUid);
    }
}
