package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.WindowConfiguration;
import android.app.servertransaction.PauseActivityItem;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Flog;
import android.util.HwMwUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.view.WindowManager;
import com.android.server.am.ActivityManagerService;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.magicwin.HwMagicWinStatistics;
import com.android.server.magicwin.HwMagicWindowConfig;
import com.android.server.magicwin.HwMagicWindowService;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.wm.ActivityStack;
import com.huawei.android.fsm.HwFoldScreenManagerEx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HwMagicWinAmsPolicy extends HwMwUtils.ModulePolicy {
    private static final int ADJ_WIN_DELAY_TIME = 100;
    private static final String BAIDU_HOMEWORK_INITACTIVITY = "com.baidu.homework.activity.init.InitActivity";
    private static final int DEAFAULT_PROCESS_UID = -1;
    private static final int DEFAULT_CAMERA_CROP = -1;
    public static final String DEVICE_ADMIN_ACTIVITY = "com.android.settings.DeviceAdminAdd";
    public static final String FINISH_REASON_CRASH = "force-crash";
    private static final int FRAME_POINT_DIVISOR = 4;
    private static final int FRAME_SIZE_DIVISOR = 2;
    private static final int FULLSCREEN_VISIBILITY_DELAY = 50;
    private static final boolean IS_DEFAULT_LAND_DEVICE;
    public static final String MAGIC_WINDOW_FINISH_EVENT = "activity finish for magicwindow";
    private static final boolean MAIN_RELATED_ENABLE = true;
    private static final int NUM_ACTIVITY_SIZE = 2;
    private static final int NUM_MAX_TASKS = 100;
    private static final int PARAM_INDEX_FOUR = 4;
    private static final int PARAM_INDEX_ONE = 1;
    private static final int PARAM_INDEX_THREE = 3;
    private static final int PARAM_INDEX_TWO = 2;
    private static final int PARAM_INDEX_ZERO = 0;
    private static final int PARAM_NUM_PROCESS_ARGS = 4;
    public static final String PERMISSION_ACTIVITY = "com.android.packageinstaller.permission.ui.GrantPermissionsActivity";
    private static final int SIZE_SHOULD_CALL_IDLE = 2;
    private static final String TAG = "HwMagicWinAmsPolicy";
    private static final String TAMLL_TMEMPTYACTIVITY = "com.tmall.wireless.common.navigator.TMEmptyActivity";
    private static final String TAOBAO_SHOPURLROUTERACTIVITY = "com.taobao.android.shop.activity.ShopUrlRouterActivity";
    private static final HashSet<String> TRANSITION_ACTIVITIES = new HashSet<>();
    private static final String WECHAT_APPBRANDPROXYACTIVITY = "com.tencent.mm.plugin.appbrand.launching.AppBrandLaunchProxyUI";
    private static final String WELINK_W3SPLASHACTIVITY = "huawei.w3.ui.welcome.W3SplashScreenActivity";
    private static final String XINLANG_NEWS_PERMISSIONACTIVITY = "com.sina.news.module.base.permission.PermissionActivity";
    private HwMwUtils.IPolicyOperation canPauseInHwMultiwin = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$irnWPawS_sXFuIutkOcaHEjFsm4 */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$28$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation changeOrientationForMultiWin = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$hRqHOPB6dMxron10MjE2gxG0rSI */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$31$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation checkMagicOrientation = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$iByL1qcIxNUvKsgj53qFoWI4prs */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$13$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation clearTask = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$KwJYH_xBbBpvXmUYtFuWVfJOlM */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$26$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation finishActivityForHwMagicWin = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$tFdN1fZwkw6qFzfQyNASbsfdAM */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$22$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation getDetectedParam = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$HiY5tkzuokmAq04cRAECcGWvEY */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$3$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation isActivityFullscreen = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$NCRwUGb0Cg8KrchdmyTQclyY */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$1$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation isInAppSplite = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$j4JjUkEDv5YmAJSyIUj4iXLQ5Pc */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$14$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation isRotation_180 = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$Y7dhbKBEogCu2g4jshcmLijzd4 */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$10$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private ActivityTaskManagerService mActivityTaskManager;
    private ActivityManagerService mAms;
    private Context mContext = null;
    public final Rect mDefaultFullScreenBounds = new Rect();
    private DisplayMetrics mDm = null;
    public final Rect mFullScreenBounds = new Rect();
    private boolean mIsLaunchFromHomeOrRecent = true;
    private int mLastDensityDpi;
    private boolean mLastRotateCamera = false;
    public HwMagicWinSplitManager mMagicWinSplitMng;
    public HwMagicModeSwitcher mModeSwitcher;
    private HwMagicWindowService mService = null;
    private HwMwUtils.IPolicyOperation moveLatestActivityToTop = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$A1uUfs6KDbtP8pJfz6jtRfI8s */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$12$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation moveMwToHwMultiStack = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$cOLM8eyYrk4B1VEBV1AYTsqHg */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$9$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation onBackPressed = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$TzH1xyQqEttMdO2eNZnQMQlw4kY */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$16$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation onProcessDied = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$7AIiICfHvgMKSwDcO5MbUGqZ7wc */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$0$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation overrideArgsForHwMagicWin = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$z9wc8S7FD2_FXxVovahaPu9jOqo */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$20$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation overrideConfigInMagicWin = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$g8W8QjckEFPq298p5Yb4XXX86fc */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$5$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation overrideIntentFlagForHwMagicWin = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$YQpPPgU3ItRO8sm54moH2fLqc */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$24$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation overrideIntentForHwMagicWin = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$iYEPLboOfs8Hp0eHvvaG1peIjtc */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$25$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation processHwMultiStack = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$_xe1VHkDICbO6O3g075SyMP0UhQ */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$8$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation processSpliteScreenForMutilWin = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$21Wcz_SQJYwRP0BMHyplBLhgDu8 */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$7$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation putDetectedResult = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$K65dyZs87zm18QhmHOY7GCbmalw */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$4$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation resetTaskWindowMode = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$gVp5yrsF138QbzBKz2YMDLshNBw */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$2$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation resizeForDrag = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$elxGhWfMmMMd1wo6ROQedIxEmNg */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$29$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation resizeSpecialVideoInMagicWindowMode = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$ROBYglcdXSFTy0P0pfBlNvJ3aNc */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$27$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation resizeSplitStackBeforeResume = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$CU9dzw7quqVuNia9p7vityI7skk */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$18$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation resizeWhenMovebackIfNeed = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$Il_NxVJjWXLq9K6dkj1fSVEu8 */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$17$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation resumeActivityForHwMagicWin = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$Rsu5T9lBZ5g8mKj3mkpUyyknnEs */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$21$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation switchFocusIfNeeded = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$O8kWfHCZbAJseQjkgGNkGL4bnQ4 */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$15$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation updateFocusActivity = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$MEcN7rRsQSZw9iXFlZpKAR0QaM */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$6$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation updateLaunchAnimation = $$Lambda$HwMagicWinAmsPolicy$68VSx05ZgzVgDyZbp4GccEzqLU.INSTANCE;
    private HwMwUtils.IPolicyOperation updateMagicWindowConfiguration = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$UykHj_jLCYV5vwlHqK_zIolIseI */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$19$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation updateStackVisibilitySplitMode = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$5TDhZaZfTsogmNawvbuDMLqTpjo */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$11$HwMagicWinAmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation updateVisibilityForHwMagicWin = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinAmsPolicy$emiUXCBMJS4E770n1RtTdNclsMs */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinAmsPolicy.this.lambda$new$23$HwMagicWinAmsPolicy(list, bundle);
        }
    };

    static {
        boolean z = false;
        if (SystemProperties.getInt("ro.panel.hw_orientation", 0) == 90) {
            z = true;
        }
        IS_DEFAULT_LAND_DEVICE = z;
        TRANSITION_ACTIVITIES.add(TAOBAO_SHOPURLROUTERACTIVITY);
        TRANSITION_ACTIVITIES.add(TAMLL_TMEMPTYACTIVITY);
        TRANSITION_ACTIVITIES.add(XINLANG_NEWS_PERMISSIONACTIVITY);
        TRANSITION_ACTIVITIES.add(BAIDU_HOMEWORK_INITACTIVITY);
        TRANSITION_ACTIVITIES.add(WELINK_W3SPLASHACTIVITY);
        TRANSITION_ACTIVITIES.add(WECHAT_APPBRANDPROXYACTIVITY);
    }

    public HwMagicWinAmsPolicy(HwMagicWindowService service, Context context, ActivityManagerService ams) {
        this.mService = service;
        this.mContext = context;
        this.mAms = ams;
        this.mActivityTaskManager = ams.mActivityTaskManager;
        this.mMagicWinSplitMng = new HwMagicWinSplitManager(ams, service, this);
        this.mModeSwitcher = new HwMagicModeSwitcher(this, service, ams);
        this.mDm = new DisplayMetrics();
        ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay().getRealMetrics(this.mDm);
        this.mLastDensityDpi = this.mDm.densityDpi;
        this.mFullScreenBounds.set(new Rect(0, 0, this.mDm.widthPixels > this.mDm.heightPixels ? this.mDm.widthPixels : this.mDm.heightPixels, this.mDm.widthPixels < this.mDm.heightPixels ? this.mDm.widthPixels : this.mDm.heightPixels));
        initPolicy();
    }

    private void initPolicy() {
        addPolicy(0, this.overrideIntentForHwMagicWin, new Class[]{IBinder.class, HwActivityRecord.class, IBinder.class, Boolean.class, ActivityOptions.class});
        addPolicy(1, this.overrideIntentFlagForHwMagicWin, new Class[]{HwActivityRecord.class, ActivityOptions.class});
        addPolicy(2, this.overrideArgsForHwMagicWin, new Class[]{ApplicationInfo.class, Boolean.class, String[].class});
        addPolicy(51, this.resumeActivityForHwMagicWin, new Class[]{IBinder.class, String.class});
        addPolicy(52, this.finishActivityForHwMagicWin, new Class[]{IBinder.class, Boolean.class, Rect.class, Rect.class, String.class});
        addPolicy(3, this.updateVisibilityForHwMagicWin, new Class[]{IBinder.class, IBinder.class, Boolean.class, int[].class});
        addPolicy(10, this.processSpliteScreenForMutilWin, new Class[]{Integer.class, Boolean.class, Integer.class});
        addPolicy(41, this.resizeSpecialVideoInMagicWindowMode, new Class[]{IBinder.class, Integer.class});
        addPolicy(9, this.checkMagicOrientation, new Class[]{IBinder.class});
        addPolicy(4, this.updateMagicWindowConfiguration, new Class[]{Integer.class, Integer.class, IBinder.class});
        addPolicy(5, this.updateFocusActivity, new Class[]{IBinder.class});
        addPolicy(6, this.overrideConfigInMagicWin, new Class[]{Configuration.class});
        addPolicy(7, this.clearTask, new Class[]{IBinder.class, Integer.class, IBinder.class});
        addPolicy(31, this.getDetectedParam, new Class[]{String.class});
        addPolicy(32, this.putDetectedResult, new Class[]{IBinder.class});
        addPolicy(61, this.isRotation_180, new Class[]{Integer.class});
        addPolicy(13, this.moveLatestActivityToTop, new Class[]{Boolean.class, Boolean.class, Integer.class, Integer.class, IBinder.class});
        addPolicy(14, this.canPauseInHwMultiwin, new Class[]{IBinder.class, IBinder.class});
        addPolicy(15, this.resetTaskWindowMode, new Class[]{TaskRecord.class});
        addPolicy(16, this.processHwMultiStack, new Class[]{Integer.class});
        addPolicy(17, this.moveMwToHwMultiStack, new Class[]{Integer.class, Rect.class});
        addPolicy(62, this.resizeForDrag, new Class[]{TaskRecord.class, Rect.class, Rect.class, Integer.class});
        addPolicy(72, this.updateLaunchAnimation, new Class[]{ActivityRecord.class, Intent.class});
        addPolicy(80, this.changeOrientationForMultiWin, new Class[]{Configuration.class, Float.class});
        addPolicy(28, this.onBackPressed, new Class[]{IBinder.class});
        addPolicy(CPUFeature.MSG_SET_BOOST_CPUS, this.updateStackVisibilitySplitMode, new Class[]{Integer.class});
        addPolicy(BigMemoryConstant.ACTIVITY_NAME_MAX_LEN, this.isInAppSplite, new Class[]{Integer.class, Boolean.class});
        addPolicy(18, this.onProcessDied, new Class[]{Integer.class, Integer.class, String.class});
        addPolicy(133, this.resizeWhenMovebackIfNeed, new Class[]{Integer.class});
        addPolicy(CPUFeature.MSG_RESET_BOOST_CPUS, this.resizeSplitStackBeforeResume, new Class[]{ActivityRecord.class});
        addPolicy(CPUFeature.MSG_SET_LIMIT_CGROUP, this.switchFocusIfNeeded, new Class[]{Integer.class, Integer.class});
        addPolicy(HwAPPQoEUtils.MSG_APP_STATE_UNKNOW, this.isActivityFullscreen, new Class[]{ActivityRecord.class});
    }

    public boolean isStackInHwMagicWindowMode() {
        ActivityStack focusedStack = getFocusedTopStack();
        if (focusedStack == null) {
            return false;
        }
        return focusedStack.inHwMagicWindowingMode();
    }

    public boolean isSupportMainRelatedMode(String pkgName) {
        String relateAct = this.mService.getConfig().getRelateActivity(pkgName);
        List<String> mainActs = this.mService.getConfig().getMainActivity(pkgName);
        return mainActs != null && mainActs.size() > 0 && !TextUtils.isEmpty(relateAct);
    }

    public boolean isMainActivity(ActivityRecord activity) {
        List<String> mainActName = this.mService.getConfig().getMainActivity(getPackageName(activity));
        return mainActName != null && mainActName.contains(getClassName(activity)) && !TextUtils.isEmpty(this.mService.getConfig().getRelateActivity(getPackageName(activity)));
    }

    public boolean isRelatedActivity(ActivityRecord activity) {
        String realteActName = this.mService.getConfig().getRelateActivity(getPackageName(activity));
        return !realteActName.isEmpty() && realteActName.equals(getClassName(activity));
    }

    public String getFocusedStackPackageName() {
        String realPkgName;
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            ActivityStack focusedStack = getFocusedTopStack();
            realPkgName = focusedStack == null ? null : getRealPkgName(focusedStack.getTopActivity());
        }
        return realPkgName;
    }

    public /* synthetic */ void lambda$new$0$HwMagicWinAmsPolicy(List params, Bundle result) {
        ArrayList<ActivityRecord> allActivities;
        int userId;
        String pkg = (String) params.get(2);
        if (this.mService.getConfig().isSupportAppTaskSplitScreen(pkg)) {
            boolean z = false;
            int uid = ((Integer) params.get(0)).intValue();
            int pid = ((Integer) params.get(1)).intValue();
            synchronized (this.mActivityTaskManager.mGlobalLock) {
                WindowProcessController processController = this.mActivityTaskManager.getProcessController(pid, uid);
                StringBuilder sb = new StringBuilder();
                sb.append("ProcessDied pid ");
                sb.append(pid);
                sb.append(" uid ");
                sb.append(uid);
                sb.append(" process exsit ");
                if (processController != null) {
                    z = true;
                }
                sb.append(z);
                Slog.i(TAG, sb.toString());
                if (processController != null) {
                    userId = processController.mUserId;
                    allActivities = processController.mActivities;
                } else {
                    userId = UserHandle.getUserId(uid);
                    allActivities = getAllActivities(this.mMagicWinSplitMng.getMainActivityStack(pkg, userId));
                }
                this.mService.getConfig().removeReportLoginStatus(getJoinStr(pkg, userId));
                Iterator<ActivityRecord> it = allActivities.iterator();
                while (it.hasNext()) {
                    ActivityRecord ar = it.next();
                    if (isRelatedActivity(ar)) {
                        ar.makeFinishingLocked();
                    } else if (isMainActivity(ar)) {
                        ar.haveState = true;
                    }
                }
            }
        }
    }

    public /* synthetic */ void lambda$new$1$HwMagicWinAmsPolicy(List params, Bundle result) {
        result.putBoolean("ACTIVITY_FULLSCREEN", isFullScreenActivity((ActivityRecord) params.get(0)));
    }

    public /* synthetic */ void lambda$new$2$HwMagicWinAmsPolicy(List params, Bundle result) {
        Slog.i(TAG, "### Execute -> reset Task Window Mode To UNDEFINED");
        TaskRecord task = (TaskRecord) params.get(0);
        if (task != null && task.realActivity != null) {
            String packageName = task.realActivity.getPackageName();
            if (this.mService.getHwMagicWinEnabled(packageName)) {
                task.setWindowingMode(0);
                if (this.mMagicWinSplitMng.isMainStack(packageName, task.getStack())) {
                    this.mService.getConfig().removeReportLoginStatus(getJoinStr(packageName, task.userId));
                }
            }
        }
    }

    public /* synthetic */ void lambda$new$3$HwMagicWinAmsPolicy(List params, Bundle result) {
        boolean isNeedDetect = this.mService.isNeedDect((String) params.get(0));
        result.putBoolean("NEED_HOST_DETECT", isNeedDetect);
        if (isNeedDetect) {
            result.putInt("VIEW_COUNT", this.mService.getConfig().getHostViewThreshold());
        }
    }

    public /* synthetic */ void lambda$new$4$HwMagicWinAmsPolicy(List params, Bundle result) {
        ActivityRecord detectedActivity = ActivityRecord.forToken((IBinder) params.get(0));
        if (detectedActivity == null) {
            result.putBoolean("IS_RESULT_DETECT", false);
            return;
        }
        String pkgName = getPackageName(detectedActivity);
        if (!this.mService.isNeedDect(pkgName)) {
            result.putBoolean("IS_RESULT_DETECT", false);
        } else if (!pkgName.equals(detectedActivity.processName)) {
            Slog.i(TAG, " processName=" + detectedActivity.processName + " Activity =" + detectedActivity);
            result.putBoolean("IS_RESULT_DETECT", true);
        } else {
            Slog.i(TAG, "putDetectedResult is host homeActivity =" + detectedActivity);
            String homeName = getClassName(detectedActivity);
            this.mService.setHost(pkgName, homeName);
            Context context = this.mContext;
            Flog.bdReport(context, 10101, "{\"app\":\"" + pkgName + "\", \"home_recognized\":\"" + homeName + "\"}");
            result.putBoolean("IS_RESULT_DETECT", true);
        }
    }

    private void startRelatedAndSetMainMode(ActivityRecord mainAr, String pkgName) {
        ArrayList<ActivityRecord> arList = getAllActivities(mainAr.getActivityStack());
        ArrayList<ActivityRecord> otherArList = new ArrayList<>();
        Iterator<ActivityRecord> it = arList.iterator();
        while (it.hasNext()) {
            ActivityRecord ar = it.next();
            if (ar != null && !isMainActivity(ar)) {
                otherArList.add(ar);
            }
        }
        if (!this.mService.isMaster(mainAr)) {
            this.mMagicWinSplitMng.addOrUpdateMainActivityStat(mainAr);
            updateActivityModeAndBounds(mainAr, this.mService.getBounds(1, pkgName), 103);
        }
        Slog.i(TAG, "start other activity size " + otherArList.size());
        if (otherArList.size() == 0) {
            String relateActName = this.mService.getConfig().getRelateActivity(pkgName);
            if (!relateActName.isEmpty()) {
                startRelateActivity(pkgName, relateActName, mainAr);
                return;
            }
            return;
        }
        Iterator<ActivityRecord> it2 = otherArList.iterator();
        while (it2.hasNext()) {
            ActivityRecord otherAr = it2.next();
            Slog.i(TAG, "setLoginStatus startRelatedAndSetMode start otherAr =" + otherAr + " pkgName " + pkgName);
            if (otherAr != null && !this.mService.isSlave(otherAr) && !this.mService.isMaster(otherAr) && !PERMISSION_ACTIVITY.equals(getClassName(otherAr))) {
                updateActivityModeAndBounds(otherAr, this.mService.getBounds(2, pkgName), 103);
            }
        }
    }

    private void updateActivityModeAndBounds(ActivityRecord activityRecord, Rect bounds, int windowMode) {
        if (activityRecord != null) {
            ActivityStack activityStack = activityRecord.getTaskRecord().getStack();
            if (!(activityStack == null || activityStack.getWindowingMode() == windowMode)) {
                activityStack.setWindowingMode(windowMode);
            }
            Slog.i(TAG, "updateActivityModeAndBounds activityRecord =" + activityRecord + " bounds " + bounds);
            activityRecord.setWindowingMode(windowMode);
            activityRecord.setBounds(bounds);
        }
    }

    private void overrideConfigInMagicWinInner(Configuration config, int width, int height) {
        float density = ((float) config.densityDpi) / 160.0f;
        config.orientation = 1;
        int i = (int) (((float) width) / density);
        config.screenWidthDp = i;
        config.compatScreenWidthDp = i;
        int i2 = (int) (((float) height) / density);
        config.screenHeightDp = i2;
        config.compatScreenHeightDp = i2;
        int i3 = config.screenWidthDp;
        config.smallestScreenWidthDp = i3;
        config.compatSmallestScreenWidthDp = i3;
        config.screenLayout = Configuration.reduceScreenLayout(Configuration.resetScreenLayout(config.screenLayout), config.screenHeightDp, config.screenWidthDp);
        config.windowConfiguration.setWindowingMode(103);
        config.windowConfiguration.setBounds(new Rect(0, 0, width, height));
        config.windowConfiguration.setAppBounds(new Rect(0, 0, width, height));
    }

    public /* synthetic */ void lambda$new$5$HwMagicWinAmsPolicy(List params, Bundle result) {
        Configuration config = (Configuration) params.get(0);
        if (config != null) {
            if (HwMwUtils.IS_FOLD_SCREEN_DEVICE && config.densityDpi != this.mLastDensityDpi) {
                Slog.i(TAG, "overrideConfigInMagicWin update system bound size when resolution change!!!");
                this.mLastDensityDpi = config.densityDpi;
                Message msg = this.mService.mHandler.obtainMessage(16);
                this.mService.mHandler.removeMessages(16);
                this.mService.mHandler.sendMessage(msg);
            }
            Configuration configSmall = new Configuration(config);
            Rect smallLeftBound = this.mService.getBounds(1, getFocusedStackPackageName());
            overrideConfigInMagicWinInner(configSmall, smallLeftBound.width(), smallLeftBound.height());
            Configuration configLarger = new Configuration(config);
            Rect largerLeftBound = this.mService.getBounds(1, true);
            overrideConfigInMagicWinInner(configLarger, largerLeftBound.width(), largerLeftBound.height());
            SparseArray<WindowProcessController> pidMap = this.mActivityTaskManager.mProcessMap.getPidMap();
            for (int i = pidMap.size() - 1; i >= 0; i--) {
                WindowProcessController app = pidMap.get(pidMap.keyAt(i));
                if (app.mActivities.size() <= 0 || !((ActivityRecord) app.mActivities.get(0)).inHwMagicWindowingMode()) {
                    app.onConfigurationChanged(config);
                } else {
                    app.onConfigurationChanged(this.mService.isScaled(app.mInfo.packageName) ? configLarger : configSmall);
                }
            }
        }
    }

    public /* synthetic */ void lambda$new$6$HwMagicWinAmsPolicy(List params, Bundle result) {
        ActivityRecord touchActivity = ActivityRecord.forToken((IBinder) params.get(0));
        Slog.i(TAG, "### Execute -> updateFocusActivity touchActivity " + touchActivity);
        if (touchActivity == null || touchActivity.getTaskRecord() == null) {
            Slog.i(TAG, "### Execute -> updateFocusActivity no need update and return");
        } else if (this.mService.isSlave(touchActivity)) {
            moveRightActvitiesToFront(touchActivity);
        } else {
            moveToFrontInner(touchActivity);
        }
    }

    private void moveRightActvitiesToFront(ActivityRecord focus) {
        this.mModeSwitcher.adjustActivitiesOrder(focus.getActivityStack().mResumedActivity, getAllActivities(focus.getTaskRecord().getStack()));
        if (!focus.isState(ActivityStack.ActivityState.RESUMED)) {
            this.mActivityTaskManager.mRootActivityContainer.resumeFocusedStacksTopActivities();
        }
        checkResumeStateForMagicWindow(focus);
    }

    public void checkResumeStateForMagicWindow(ActivityRecord focus) {
        if (this.mService.isSupportMultiResume(getPackageName(focus)) && focus.visible && focus.isTopRunningActivity() && focus.getActivityStack() != null && focus.getActivityStack().mResumedActivity != focus && focus.isState(ActivityStack.ActivityState.RESUMED)) {
            focus.getActivityStack().onActivityStateChanged(focus, ActivityStack.ActivityState.RESUMED, "checkResumeStateForMagicWindow");
        }
    }

    public /* synthetic */ void lambda$new$7$HwMagicWinAmsPolicy(List params, Bundle result) {
        Slog.i(TAG, "### Execute -> processSpliteScreenForMutilWin");
        this.mModeSwitcher.processSpliteScreenForMutilWin(((Integer) params.get(0)).intValue(), ((Boolean) params.get(1)).booleanValue(), ((Integer) params.get(2)).intValue(), result);
    }

    public /* synthetic */ void lambda$new$8$HwMagicWinAmsPolicy(List params, Bundle result) {
        Slog.i(TAG, "### Execute -> processHwMultiStack");
        this.mModeSwitcher.processHwMultiStack(((Integer) params.get(0)).intValue(), this.mContext.getResources().getConfiguration().orientation, result);
    }

    public /* synthetic */ void lambda$new$9$HwMagicWinAmsPolicy(List params, Bundle result) {
        Slog.i(TAG, "### Execute -> moveMwToHwMultiStack");
        this.mModeSwitcher.moveMwToHwMultiStack(((Integer) params.get(0)).intValue(), (Rect) params.get(1), result);
    }

    public /* synthetic */ void lambda$new$10$HwMagicWinAmsPolicy(List params, Bundle result) {
        ActivityStack focusedStack;
        boolean isRightWindowingMode = false;
        int rotation = ((Integer) params.get(0)).intValue();
        if (!HwMwUtils.isInSuitableScene(true)) {
            return;
        }
        if ((HwMwUtils.IS_FOLD_SCREEN_DEVICE || rotation == 2) && getCurrentRotation() != rotation && (focusedStack = getFocusedTopStack()) != null) {
            String pkgName = getRealPkgName(focusedStack.getTopActivity());
            HwActivityRecord topActivity = focusedStack.getTopActivity();
            if (focusedStack.inHwMagicWindowingMode() || focusedStack.getWindowingMode() == 1) {
                isRightWindowingMode = true;
            }
            if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE || topActivity == null || !isRightWindowingMode) {
                if (isRightWindowingMode && this.mService.getHwMagicWinEnabled(pkgName)) {
                    result.putBoolean("BUNDLE_IS_ROTATION_180", true);
                } else if (isRightWindowingMode && isLimitReversePortrait(focusedStack)) {
                    result.putBoolean("BUNDLE_IS_ROTATION_180", true);
                }
            } else if (this.mService.getHwMagicWinEnabled(pkgName) && !topActivity.mIsFullScreenVideoInLandscape && rotation != 0) {
                result.putBoolean("BUNDLE_IS_ROTATION_180", true);
            }
        }
    }

    public /* synthetic */ void lambda$new$11$HwMagicWinAmsPolicy(List params, Bundle result) {
        this.mMagicWinSplitMng.updateStackVisibility(params, result);
    }

    public /* synthetic */ void lambda$new$12$HwMagicWinAmsPolicy(List params, Bundle result) {
        Slog.d(TAG, "### Execute -> Move Right Activity To Left");
        boolean isNormalPort = false;
        boolean quitMagicWindow = ((Boolean) params.get(0)).booleanValue();
        boolean clearBounds = ((Boolean) params.get(1)).booleanValue();
        int windowingMode = ((Integer) params.get(2)).intValue();
        int taskId = ((Integer) params.get(3)).intValue();
        ActivityRecord activityRecord = ActivityRecord.forToken((IBinder) params.get(4));
        if (windowingMode == 3) {
            this.mModeSwitcher.moveLatestActivityToTop(quitMagicWindow, clearBounds);
        } else if (WindowConfiguration.isHwFreeFormWindowingMode(windowingMode)) {
            int orientation = this.mContext.getResources().getConfiguration().orientation;
            if (HwMwUtils.IS_TABLET && orientation != 2) {
                isNormalPort = true;
            }
            if (isNormalPort) {
                Slog.w(TAG, "moveLatestActivityToTop orientation is in unsupport orientation");
            } else if (clearBounds) {
                this.mModeSwitcher.clearOverrideBounds(taskId);
            }
        } else if (windowingMode == 2 && !HwMwUtils.IS_FOLD_SCREEN_DEVICE && this.mContext.getResources().getConfiguration().orientation == 2 && clearBounds) {
            this.mModeSwitcher.clearOverrideBounds(activityRecord);
        }
    }

    public /* synthetic */ void lambda$new$13$HwMagicWinAmsPolicy(List params, Bundle result) {
        if (!HwMwUtils.isInSuitableScene(true)) {
            Slog.w(TAG, "it is in PC mode or mmi test !");
            return;
        }
        HwActivityRecord ar = ActivityRecord.forToken((IBinder) params.get(0));
        if (ar == null || ar.mAppWindowToken == null) {
            Slog.e(TAG, "checkMagicOrientation activityrecord is null!");
        } else if (shouldOverrideOrientation(ar.mAppWindowToken)) {
            int orientation = ar.mAppWindowToken.mOrientation;
            boolean magicMode = ar.inHwMagicWindowingMode();
            String pkgName = getRealPkgName(ar);
            int magicRotation = checkMagicOrientationInner(pkgName, orientation, magicMode, ar);
            if (HwMwUtils.MAGICWIN_LOG_SWITCH) {
                Slog.i(TAG, "checkMagicRotation, pkgName = " + pkgName + ", orientation = " + orientation + ", magicMode = " + magicMode + ", mIsFullScreenVideoInLandscape = " + ar.mIsFullScreenVideoInLandscape + ", return :" + magicRotation);
            }
            result.putInt("BUNDLE_RESULT_ORIENTATION", magicRotation);
        }
    }

    private boolean shouldOverrideOrientation(AppWindowToken token) {
        return !(token.sendingToBottom || token.getDisplayContent().mClosingApps.contains(token)) && (token.isVisible() || token.getDisplayContent().mOpeningApps.contains(token));
    }

    private boolean isDefaultLandOrientation(int orientation) {
        return ActivityInfo.isFixedOrientationLandscape(orientation);
    }

    private int checkMagicOrientationInner(String pkgName, int orientation, boolean magicMode, HwActivityRecord ar) {
        if (!this.mService.getHwMagicWinEnabled(pkgName) || (isDefaultLandOrientation(orientation) && (!HwMwUtils.IS_FOLD_SCREEN_DEVICE || orientation != -1))) {
            return -3;
        }
        boolean isLandScape = this.mContext.getResources().getConfiguration().orientation == 2;
        if (HwMwUtils.IS_TABLET && isLandScape && !magicMode && ar.mIsFullScreenVideoInLandscape) {
            return -3;
        }
        ar.mIsFullScreenVideoInLandscape = false;
        return getOrientation();
    }

    private int getOrientation() {
        if (HwMwUtils.IS_FOLD_SCREEN_DEVICE || 2 == getCurrentRotation()) {
            return 1;
        }
        return this.mService.getWmsPolicy().getOrientation();
    }

    public /* synthetic */ void lambda$new$14$HwMagicWinAmsPolicy(List params, Bundle result) {
        result.putBoolean("RESULT_IN_APP_SPLIT", this.mMagicWinSplitMng.isInAppSplite(((Integer) params.get(0)).intValue(), ((Boolean) params.get(1)).booleanValue()));
    }

    public /* synthetic */ void lambda$new$15$HwMagicWinAmsPolicy(List params, Bundle result) {
        int touchDownX = ((Integer) params.get(0)).intValue();
        int touchDownY = ((Integer) params.get(1)).intValue();
        Slog.i(TAG, "switchFocusIfNeeded touchDownX=" + touchDownX + " touchDownY=" + touchDownY);
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            if (!this.mService.getWmsPolicy().isInputMethodWindowVisible()) {
                ActivityRecord topActivity = getTopActivity();
                if (topActivity != null && topActivity.inHwMagicWindowingMode()) {
                    if (!this.mMagicWinSplitMng.isPkgSpliteScreenMode(topActivity, true)) {
                        if (this.mService.isSlave(topActivity)) {
                            moveToFrontIfNeeded(topActivity, 1, touchDownX, touchDownY);
                        } else if (this.mService.isMaster(topActivity)) {
                            moveToFrontIfNeeded(topActivity, 2, touchDownX, touchDownY);
                        } else {
                            Slog.i(TAG, "switchFocusIfNeeded is not double windows");
                        }
                    }
                }
            }
        }
    }

    private void moveToFrontIfNeeded(ActivityRecord top, int windowPosition, int touchDownX, int touchDownY) {
        ActivityRecord activity = getActvityByPosition(top, windowPosition, 0);
        if (activity != null && new Region(activity.getRequestedOverrideBounds()).contains(touchDownX, touchDownY)) {
            if (activity instanceof HwActivityRecord) {
                ((HwActivityRecord) activity).mMagicWindowPageType = 1;
            }
            moveToFrontInner(activity);
            activity.resumeKeyDispatchingLocked();
        }
    }

    public void moveToFrontInner(ActivityRecord activity) {
        activity.getTaskRecord().moveActivityToFrontLocked(activity);
        if (!activity.isState(ActivityStack.ActivityState.RESUMED)) {
            this.mActivityTaskManager.mRootActivityContainer.resumeFocusedStacksTopActivities();
        }
        checkResumeStateForMagicWindow(activity);
    }

    public /* synthetic */ void lambda$new$16$HwMagicWinAmsPolicy(List params, Bundle result) {
        ActivityRecord masterTop;
        ActivityRecord rightTop;
        IBinder token = (IBinder) params.get(0);
        ActivityRecord ar = ActivityRecord.isInStackLocked(token);
        if (ar != null && isRelatedActivity(ar) && isSupportMainRelatedMode(getPackageName(ar)) && (masterTop = getActvityByPosition(ar, 1, 0)) != null && isMainActivity(masterTop) && (rightTop = getActvityByPosition(ar, 2, 0)) != null && isRelatedActivity(rightTop)) {
            Slog.i(TAG, "onBackPressed: move activity task to back for magicwindow, currentActivity = " + ar);
            this.mActivityTaskManager.moveActivityTaskToBack(token, true);
            result.putBoolean("BUNDLE_RESULT_ONBACKPRESSED", true);
        }
    }

    public /* synthetic */ void lambda$new$17$HwMagicWinAmsPolicy(List params, Bundle result) {
        this.mMagicWinSplitMng.resizeWhenMoveBackIfNeed(((Integer) params.get(0)).intValue());
    }

    public /* synthetic */ void lambda$new$18$HwMagicWinAmsPolicy(List params, Bundle result) {
        ActivityRecord resumeActivity = (ActivityRecord) params.get(0);
        this.mMagicWinSplitMng.resizeSplitStackBeforeResume(resumeActivity, getRealPkgName(resumeActivity));
    }

    public /* synthetic */ void lambda$new$19$HwMagicWinAmsPolicy(List params, Bundle result) {
        Slog.i(TAG, "### Execute -> updateMagicWindowConfiguration");
        if (!HwMwUtils.isInSuitableScene(true)) {
            Slog.w(TAG, "it is in PC mode or mmi test !");
            return;
        }
        this.mModeSwitcher.updateMagicWindowConfiguration(((Integer) params.get(0)).intValue(), ((Integer) params.get(1)).intValue(), (IBinder) params.get(2));
    }

    private boolean isLimitReversePortrait(ActivityStack focus) {
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            ActivityRecord topActivity = focus.getTopActivity();
            boolean isTopTrans = false;
            boolean z = true;
            if (topActivity != null) {
                AppWindowToken token = topActivity.mAppWindowToken;
                isTopTrans = token != null && !token.fillsParent() && token.mOrientation == -1;
            }
            if (!isTopTrans) {
                return false;
            }
            ActivityDisplay activityDisplay = this.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
            if (activityDisplay == null) {
                return false;
            }
            ActivityStack nextStack = activityDisplay.getNextFocusableStack(focus, true);
            boolean isNextMagicAppVisible = false;
            if (nextStack != null) {
                if (!this.mService.getHwMagicWinEnabled(getRealPkgName(nextStack.getTopActivity()))) {
                    return false;
                }
                if (nextStack.mTaskStack == null || !nextStack.mTaskStack.isVisible()) {
                    z = false;
                }
                isNextMagicAppVisible = z;
            }
            return isNextMagicAppVisible;
        }
    }

    public ActivityRecord getTopActivity() {
        ActivityStack topFocus = getFocusedTopStack();
        if (topFocus != null) {
            return topFocus.getTopActivity();
        }
        return null;
    }

    public ActivityStack getFocusedTopStack() {
        return getFilteredTopStack(this.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay(), Arrays.asList(5, 2, 102));
    }

    private ActivityStack getFilteredTopStack(ActivityDisplay activityDisplay, List<Integer> ignoreWindowModes) {
        ActivityStack stack = null;
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            if (activityDisplay == null) {
                Slog.i(TAG, "getFilteredTopStack activityDisplay null, no TopStack");
                return null;
            }
            for (int stackNdx = activityDisplay.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                stack = activityDisplay.getChildAt(stackNdx);
                if (ignoreWindowModes == null || !ignoreWindowModes.contains(Integer.valueOf(stack.getWindowingMode()))) {
                    return stack;
                }
            }
            return stack;
        }
    }

    public ArrayList<ActivityRecord> getAllActivities(ActivityStack stack) {
        ArrayList<ActivityRecord> outActivities = new ArrayList<>();
        if (stack == null) {
            return outActivities;
        }
        for (int taskNdx = stack.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = (TaskRecord) stack.mTaskHistory.get(taskNdx);
            for (int activityNdx = task.mActivities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord activity = (ActivityRecord) task.mActivities.get(activityNdx);
                if (!activity.finishing) {
                    outActivities.add(activity);
                }
            }
        }
        return outActivities;
    }

    public /* synthetic */ void lambda$new$20$HwMagicWinAmsPolicy(List params, Bundle result) {
        ApplicationInfo info = (ApplicationInfo) params.get(0);
        ActivityStack focusStack = getFocusedTopStack();
        boolean isMagicMode = focusStack != null ? focusStack.inHwMagicWindowingMode() : false;
        String[] args = (String[]) params.get(2);
        if (info != null && HwMwUtils.isInSuitableScene(true)) {
            if (args == null || args.length < 4) {
                Slog.w(TAG, "overrideArgsForHwMagicWin args is not valid");
            } else if (this.mService.getHwMagicWinEnabled(info.packageName)) {
                Slog.w(TAG, "overrideArgsForHwMagicWin packageName " + info.packageName);
                args[0] = String.valueOf(true);
                Rect bound = this.mService.getBounds(3, info.packageName);
                args[1] = String.valueOf(String.valueOf(bound.width()));
                args[2] = String.valueOf(String.valueOf(bound.height()));
                if (isMagicMode) {
                    args[3] = String.valueOf(103);
                } else {
                    args[3] = String.valueOf(0);
                }
                args[4] = String.valueOf(this.mService.getConfig().isDragable(info.packageName));
            }
        }
    }

    private void adjustWindowForResume(ActivityRecord activity) {
        if (!TRANSITION_ACTIVITIES.contains(getClassName(activity)) && !this.mMagicWinSplitMng.isPkgSpliteScreenMode(activity, true)) {
            if (this.mService.isMaster(activity) && getActvityByPosition(activity, 2, 0) == null) {
                if (getActvityByPosition(activity, 1, 1) == null || isMainActivity(activity)) {
                    moveWindow(activity, 3);
                } else {
                    moveWindow(activity, 2);
                    return;
                }
            }
            if (this.mService.isSlave(activity) && getActvityByPosition(activity, 1, 0) == null) {
                ActivityRecord activityRecord = getActvityByPosition(activity, 3, 0);
                if (activityRecord != null) {
                    moveWindow(activityRecord, 1);
                    return;
                }
                moveWindow(activity, 3);
            }
            if ((this.mService.isMaster(activity) || this.mService.isSlave(activity)) && this.mService.getConfig().isDragable(getRealPkgName(activity))) {
                this.mService.getUIController().updateSplitBarVisibility(true);
            } else {
                this.mService.getUIController().updateSplitBarVisibility(false);
            }
        }
    }

    private void adjustWindowForFinish(HwActivityRecord activity, String finishReason) {
        if (!TRANSITION_ACTIVITIES.contains(getClassName((ActivityRecord) activity)) && !activity.mIsFullScreenVideoInLandscape && !this.mMagicWinSplitMng.isPkgSpliteScreenMode(activity, true) && !this.mService.isMiddle(activity) && !this.mService.isFull(activity)) {
            this.mService.getMode(getRealPkgName(activity)).adjustWindowForFinish(activity, finishReason);
        }
    }

    private void adjustWindowForMiddle(ActivityRecord activity) {
        if (this.mService.isMiddle(activity) && !this.mService.getConfig().isDefaultFullscreenActivity(getRealPkgName(activity), getClassName(activity))) {
            if (getActvityByPosition(activity, 1, 0) != null) {
                moveWindow(activity, 2);
            } else if (getActvityByPosition(activity, 2, 0) != null) {
                moveWindow(activity, 1);
            }
        }
    }

    public /* synthetic */ void lambda$new$21$HwMagicWinAmsPolicy(List params, Bundle result) {
        ActivityRecord topActivity;
        Slog.i(TAG, "### Execute -> resumeActivityForHwMagicWin");
        HwActivityRecord resumeActivity = ActivityRecord.forToken((IBinder) params.get(0));
        Slog.i(TAG, "resumeActivityForHwMagicWin ActivityRecord resumeActivity=" + resumeActivity);
        String pkg = getRealPkgName(resumeActivity);
        HwMagicWinStatistics.getInstance().startTick(this.mService.getConfig(), pkg, resumeActivity.inHwMagicWindowingMode() ? -1 : -2);
        if (isNeedRotateCamera() != this.mLastRotateCamera) {
            sendMessageForSetMultiWinCameraProp(isNeedRotateCamera());
        }
        requestRotation(resumeActivity);
        if (this.mService.getConfig().isSupportAppTaskSplitScreen(pkg)) {
            this.mMagicWinSplitMng.addOrUpdateMainActivityStat(resumeActivity);
        }
        startRightOnResume(resumeActivity, pkg);
        if (resumeActivity.inHwMagicWindowingMode()) {
            if (resumeActivity.isTopRunningActivity()) {
                if (this.mFullScreenBounds.equals(resumeActivity.getRequestedOverrideBounds())) {
                    this.mService.getUIController().updateMagicWindowWallpaperVisibility(false);
                } else if (this.mService.isMiddle(resumeActivity)) {
                    this.mService.getUIController().updateMagicWindowWallpaperVisibility(true);
                }
            }
            this.mMagicWinSplitMng.resizeStackWhileResumeSplitAppIfNeed(pkg, resumeActivity);
            if (this.mMagicWinSplitMng.isPkgSpliteScreenMode(resumeActivity, true)) {
                this.mMagicWinSplitMng.resizeStackIfNeedOnresume(resumeActivity);
            }
            if (isMainActivity(resumeActivity) && this.mService.isMaster(resumeActivity) && !this.mService.getConfig().isSupportAppTaskSplitScreen(pkg)) {
                startRelateActivityIfNeed(resumeActivity, false);
            }
            TaskRecord taskRecord = resumeActivity.getTaskRecord();
            if (taskRecord != null && taskRecord.getChildCount() == 2 && (topActivity = taskRecord.getChildAt(taskRecord.getChildCount() - 1)) != null && topActivity.finishing && this.mService.isSlave(topActivity)) {
                Slog.d(TAG, "resumeActivityForHwMagicWin call scheduleIdleLocked");
                this.mActivityTaskManager.getStackSupervisor().scheduleIdleLocked();
            }
            adjustWindowForResume(resumeActivity);
            checkResumeStateForMagicWindow(resumeActivity);
            checkBackgroundForMagicWindow(resumeActivity);
            boolean canShowWhileOccluded = this.mAms.mWindowManager.getPolicy().isKeyguardOccluded();
            boolean isKeyguardLocked = this.mActivityTaskManager.getStackSupervisor().getKeyguardController().isKeyguardLocked();
            if (canShowWhileOccluded && resumeActivity.isTopRunningActivity() && resumeActivity.nowVisible) {
                if (isKeyguardLocked) {
                    resumeActivity.setBounds(this.mService.getBounds(3, getPackageName((ActivityRecord) resumeActivity)));
                } else {
                    adjustWindowForMiddle(resumeActivity);
                }
            }
            if (resumeActivity.mIsFinishAllRightBottom) {
                finishMagicWindow(resumeActivity, false);
            }
            resumeActivity.mIsFinishAllRightBottom = false;
            if (this.mIsLaunchFromHomeOrRecent) {
                moveToTopWhenMultiResume(resumeActivity);
            }
            if (this.mService.isMaster(resumeActivity)) {
                resumeUnusualActivity(resumeActivity, 2);
            }
            if (this.mService.isSlave(resumeActivity)) {
                resumeUnusualActivity(resumeActivity, 1);
            }
            this.mIsLaunchFromHomeOrRecent = true;
            this.mService.getUIController().updateBgColor();
        }
    }

    private void startRightOnResume(ActivityRecord resumeAr, String pkg) {
        if (isPkgInLoginStatus(pkg, resumeAr.mUserId) && isNeedStartOrMoveRight(resumeAr, pkg)) {
            if (isMainActivity(resumeAr) && resumeAr.getTaskRecord().getStack() == getFocusedTopStack()) {
                Slog.w(TAG, "resumeAr on the top");
                startRelatedAndSetMainMode(resumeAr, pkg);
            } else if (isRelatedActivity(resumeAr)) {
                ActivityRecord preAr = getActvityByPosition(resumeAr, 0, 1);
                if (preAr != null && preAr.getTaskRecord() != null && this.mService.isMiddle(preAr) && this.mService.isSlave(resumeAr)) {
                    resumeAr.getTaskRecord().moveActivityToFrontLocked(preAr);
                    Slog.i(TAG, "start right resume move the middle to top");
                }
            } else {
                Slog.d(TAG, "start right other activity");
            }
        }
    }

    private void moveToTopWhenMultiResume(ActivityRecord resumedActivity) {
        ActivityRecord activityRecord;
        if (this.mService.isSupportMultiResume(getPackageName(resumedActivity)) && this.mService.isMaster(resumedActivity) && (activityRecord = getActvityByPosition(resumedActivity, 2, 0)) != null) {
            resumedActivity.getTaskRecord().moveActivityToFrontLocked(activityRecord);
            this.mActivityTaskManager.mRootActivityContainer.resumeFocusedStacksTopActivities();
            checkResumeStateForMagicWindow(activityRecord);
        }
    }

    private void resumeUnusualActivity(ActivityRecord resumeActivity, int windowPosition) {
        boolean isNoMainWindow = false;
        ActivityRecord activity = getActvityByPosition(resumeActivity, windowPosition, 0);
        if (activity != null) {
            boolean isNormalState = activity.isState(ActivityStack.ActivityState.PAUSED, ActivityStack.ActivityState.PAUSING, ActivityStack.ActivityState.RESUMED);
            if (activity.mAppWindowToken != null && activity.mAppWindowToken.findMainWindow() == null) {
                isNoMainWindow = true;
            }
            if (!isNormalState || isNoMainWindow) {
                Slog.i(TAG, "resumeActivityForHwMagicWin unusualActivity = " + activity);
                resumeActivity.getTaskRecord().moveActivityToFrontLocked(activity);
                this.mActivityTaskManager.mRootActivityContainer.resumeFocusedStacksTopActivities();
            }
        }
    }

    public boolean isKeyguardLockedAndOccluded() {
        return this.mActivityTaskManager.getStackSupervisor().getKeyguardController().isKeyguardLocked() && this.mAms.mWindowManager.getPolicy().isKeyguardOccluded();
    }

    public boolean isRelatedInSlave(ActivityRecord ar) {
        return isRelatedActivity(ar) && this.mService.isSlave(ar);
    }

    public void checkBackgroundForMagicWindow(ActivityRecord resumeActivity) {
        HwMagicWindowService hwMagicWindowService = this.mService;
        boolean z = true;
        if (!(getActvityByPosition(2) == null || getActvityByPosition(1) == null)) {
            z = false;
        }
        hwMagicWindowService.changeWallpaper(z);
    }

    private boolean isNormalPage(HwActivityRecord finishActivity) {
        if (isFullScreenActivity(finishActivity.getActivityStack().getTopActivity()) || !(finishActivity == getActvityByPosition(finishActivity, 1, 0) || finishActivity == getActvityByPosition(finishActivity, 2, 0))) {
            Slog.w(TAG, "isNormalPage ActivityRecord is not top Activity");
            return false;
        } else if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE ? !isSpecTransActivity(finishActivity) : !isSpecTransActivityPreDefined(finishActivity)) {
            return true;
        } else {
            Slog.w(TAG, "isNormalPage ActivityRecord is not normal Activity");
            return false;
        }
    }

    public /* synthetic */ void lambda$new$22$HwMagicWinAmsPolicy(List params, Bundle result) {
        HwActivityRecord finishActivity = ActivityRecord.forToken((IBinder) params.get(0));
        String finishReason = (String) params.get(4);
        if (!FINISH_REASON_CRASH.equals(finishReason)) {
            String pkgName = getPackageName((ActivityRecord) finishActivity);
            if (finishActivity.inHwMagicWindowingMode()) {
                boolean isFullScreen = this.mFullScreenBounds.equals(finishActivity.getRequestedOverrideBounds());
                if (PERMISSION_ACTIVITY.equals(getClassName((ActivityRecord) finishActivity)) || isFullScreen) {
                    this.mService.getUIController().updateSplitBarVisibility(true);
                }
                boolean isRootActivity = false;
                TaskRecord task = finishActivity.getTaskRecord();
                if (!(task == null || task.getRootActivity() == null)) {
                    isRootActivity = finishActivity == task.getRootActivity();
                }
                if (isFullScreen && !isRootActivity && finishActivity == getTopActivity()) {
                    this.mService.getUIController().updateMagicWindowWallpaperVisibility(true);
                }
                if (isMainActivity(finishActivity) && finishActivity.getActivityStack() != null) {
                    removeRelatedActivity(finishActivity.getActivityStack());
                }
                Slog.i(TAG, "finishActivityForHwMagicWin ActivityRecord " + finishActivity + " finishReason=" + finishReason);
                overrideFinishForJudgeHost(finishActivity);
                ActivityRecord slave2AR = getActvityByPosition(finishActivity, 2, 1);
                boolean isEmptyInSlave = slave2AR == null;
                boolean isMoveToMiddleOrSlave = isEmptyInSlave | (!finishActivity.isTopRunningActivity()) | ((getActvityByPosition(finishActivity, 1, 1) != null) && isRelatedActivity(slave2AR));
                if (isEmptyInSlave) {
                    finishActivity.mIsAniRunningBelow = true;
                }
                this.mService.getWmsPolicy().overrideFinishActivityAnimation(finishActivity.getRequestedOverrideBounds(), isMoveToMiddleOrSlave, isSpecTransActivity(finishActivity), isEmptyInSlave);
                if (isNormalPage(finishActivity)) {
                    if (this.mService.isMaster(finishActivity) && finishActivity.isTopRunningActivity()) {
                        this.mService.getMode(pkgName).finishRightAfterFinishingLeft(finishActivity);
                    }
                    adjustWindowForFinish(finishActivity, finishReason);
                    if (pkgName != null && pkgName.equals(getRealPkgName(getTopActivity())) && this.mService.getConfig().isNeedStartByNewTaskActivity(pkgName, getClassName((ActivityRecord) finishActivity)) && finishActivity.getTaskRecord().getChildCount() == 1) {
                        this.mMagicWinSplitMng.showMoveAnimation(finishActivity, 1);
                    }
                    this.mMagicWinSplitMng.moveTaskToFullscreenIfNeed(finishActivity, false);
                }
            } else if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE && finishActivity.getWindowingMode() == 1 && finishActivity.mIsFullScreenVideoInLandscape && this.mService.getHwMagicWinEnabled(pkgName) && this.mContext.getResources().getConfiguration().orientation == 2 && finishActivity.isTopRunningActivity()) {
                this.mModeSwitcher.moveAppToMagicWinWhenFinishingFullscreen(finishActivity);
            }
        }
    }

    public boolean isHomeActivity(ActivityRecord ar) {
        return ar != null && this.mService.isHomePage(getPackageName(ar), getClassName(ar));
    }

    public void removeRelatedActivity(ActivityStack stack) {
        Iterator<ActivityRecord> it = getAllActivities(stack).iterator();
        while (it.hasNext()) {
            ActivityRecord currentActivity = it.next();
            if (isRelatedActivity(currentActivity)) {
                stack.finishActivityLocked(currentActivity, 0, (Intent) null, MAGIC_WINDOW_FINISH_EVENT, true, false);
            }
        }
    }

    private int getSameActvityNums(ActivityRecord focus, String name) {
        List<TaskRecord> taskHistory = focus.getActivityStack().getAllTasks();
        int offsetIndex = 0;
        for (int taskIndex = taskHistory.size() - 1; taskIndex >= 0; taskIndex--) {
            List<ActivityRecord> activityRecords = taskHistory.get(taskIndex).mActivities;
            for (int activityIndex = activityRecords.size() - 1; activityIndex >= 0; activityIndex--) {
                if (getClassName(activityRecords.get(activityIndex)).contains(name)) {
                    offsetIndex++;
                }
            }
        }
        return offsetIndex;
    }

    private void setVisibilityForHwMagicWin(ActivityRecord targetActivity, int position, Bundle result) {
        int windowBounds = this.mService.getBoundsPosition(targetActivity.getRequestedOverrideBounds());
        if (HwMwUtils.MAGICWIN_LOG_SWITCH) {
            Slog.i(TAG, "updateVisibility windowBounds=" + windowBounds + " position =" + position);
        }
        if (position == 0) {
            result.putBoolean("BUNDLE_RESULT_UPDATE_VISIBILITY", true);
        }
        if (position > 0) {
            String pkgName = getPackageName(targetActivity);
            if ((this.mService.isSupportAnAnMode(pkgName) || this.mService.isSupportOpenMode(pkgName)) && position == 1 && windowBounds == 1) {
                result.putBoolean("BUNDLE_RESULT_UPDATE_VISIBILITY", true ^ isHomeActivity(getActvityByPosition(targetActivity, 1, 0)));
                return;
            }
            ActivityRecord lastActivity = getActvityByPosition(targetActivity, windowBounds, position - 1);
            if (lastActivity == null || lastActivity.fullscreen) {
                result.putBoolean("BUNDLE_RESULT_UPDATE_VISIBILITY", false);
            } else {
                result.putBoolean("BUNDLE_RESULT_UPDATE_VISIBILITY", true);
            }
        }
    }

    public boolean isNeedRotateCamera() {
        ActivityRecord topActivity = getTopActivity();
        if (topActivity == null) {
            return false;
        }
        boolean isFullScreen = this.mFullScreenBounds.equals(topActivity.getRequestedOverrideBounds());
        if (!topActivity.inHwMagicWindowingMode() || isFullScreen) {
            return false;
        }
        return true;
    }

    private int getCurrentRotation() {
        return this.mAms.mWindowManager.getDefaultDisplayRotation();
    }

    public void sendMessageForSetMultiWinCameraProp(boolean isNeedRotateCamera) {
        if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
            this.mLastRotateCamera = isNeedRotateCamera;
            Message msg = this.mService.mHandler.obtainMessage(3);
            msg.obj = Integer.valueOf(isNeedRotateCamera ? getCurrentRotation() : -1);
            this.mService.mHandler.removeMessages(3);
            this.mService.mHandler.sendMessage(msg);
        }
    }

    public boolean isFullScreenActivity(ActivityRecord ar) {
        if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
            return this.mFullScreenBounds.equals(ar.getRequestedOverrideBounds()) || this.mDefaultFullScreenBounds.equals(ar.getRequestedOverrideBounds());
        }
        int pos = this.mService.getBoundsPosition(ar.getRequestedOverrideBounds());
        return pos == 3 || pos == 5;
    }

    private boolean isNeedUpdateVisibilityForTopFullscreen(ActivityRecord top, ActivityRecord current, boolean originalVisible, Bundle result) {
        ActivityRecord ar;
        if (isFullScreenActivity(top) && top != current) {
            if (top.fullscreen || !isFullScreenActivity(current)) {
                Iterator<ActivityRecord> it = getAllActivities(current.getActivityStack()).iterator();
                while (it.hasNext() && (ar = it.next()) != current) {
                    if (isFullScreenActivity(ar) && ar.fullscreen) {
                        result.putBoolean("BUNDLE_RESULT_UPDATE_VISIBILITY", false);
                        return true;
                    }
                }
            } else {
                result.putBoolean("BUNDLE_RESULT_UPDATE_VISIBILITY", originalVisible);
                return true;
            }
        }
        return false;
    }

    public /* synthetic */ void lambda$new$23$HwMagicWinAmsPolicy(List params, Bundle result) {
        boolean originalVisible = ((Boolean) params.get(2)).booleanValue();
        int[] positions = (int[]) params.get(3);
        ActivityRecord current = ActivityRecord.forToken((IBinder) params.get(1));
        ActivityRecord top = ActivityRecord.forToken((IBinder) params.get(0));
        if (HwMwUtils.MAGICWIN_LOG_SWITCH) {
            Slog.i(TAG, "updateVisibilityForHwMagicWin r=" + current + " top=" + top);
        }
        if (current == null || top == null) {
            result.putBoolean("BUNDLE_RESULT_UPDATE_VISIBILITY", originalVisible);
            Slog.w(TAG, "updateVisibilityForHwMagicWin r or top is null");
            return;
        }
        String topPackageName = getPackageName(top);
        String rPackageName = getPackageName(current);
        if (rPackageName == null || topPackageName == null) {
            result.putBoolean("BUNDLE_RESULT_UPDATE_VISIBILITY", originalVisible);
            Slog.w(TAG, "updateVisibilityForHwMagicWin rPackageName or topPackageName is null");
        } else if (this.mService.getAppSupportMode(rPackageName) <= 0) {
            result.putBoolean("BUNDLE_RESULT_UPDATE_VISIBILITY", originalVisible);
        } else if (this.mMagicWinSplitMng.isPkgSpliteScreenMode(current, false)) {
            result.putBoolean("BUNDLE_RESULT_UPDATE_VISIBILITY", originalVisible);
        } else {
            ActivityStack activityStack = getFocusedTopStack();
            if (activityStack != null && activityStack != current.getActivityStack() && activityStack.inHwMagicWindowingMode()) {
                result.putBoolean("BUNDLE_RESULT_UPDATE_VISIBILITY", false);
            } else if (isKeyguardLockedAndOccluded() && current != top) {
                result.putBoolean("BUNDLE_RESULT_UPDATE_VISIBILITY", false);
            } else if (this.mService.isMiddle(top) && !isFullScreenActivity(top) && (this.mService.isMaster(current) || this.mService.isSlave(current))) {
                result.putBoolean("BUNDLE_RESULT_UPDATE_VISIBILITY", false);
            } else if (!isNeedUpdateVisibilityForTopFullscreen(top, current, originalVisible, result)) {
                if (this.mService.isMaster(current)) {
                    setVisibilityForHwMagicWin(current, positions[0], result);
                    positions[0] = positions[0] + 1;
                } else if (this.mService.isSlave(current)) {
                    setVisibilityForHwMagicWin(current, positions[1], result);
                    positions[1] = positions[1] + 1;
                } else {
                    result.putBoolean("BUNDLE_RESULT_UPDATE_VISIBILITY", originalVisible);
                }
            }
        }
    }

    public /* synthetic */ void lambda$new$24$HwMagicWinAmsPolicy(List params, Bundle result) {
        ActivityOptions options = (ActivityOptions) params.get(1);
        Slog.i(TAG, "### Execute -> overrideIntentFlagForHwMagicWin");
        HwActivityRecord focus = getTopActivity();
        boolean z = false;
        HwActivityRecord next = (HwActivityRecord) params.get(0);
        HwMagicModeBase appMode = this.mService.getMode(getPackageName((ActivityRecord) focus));
        if (focus == null || next == null) {
            Slog.e(TAG, "overrideIntentFlagForHwMagicWin focus or next is null");
        } else if (!isOtherMultiWinOptions(options, next, focus)) {
            if (focus.isActivityTypeHome() && next.intent != null) {
                Set<String> categories = next.intent.getCategories();
                if (categories != null && categories.contains("android.intent.category.LAUNCHER")) {
                    z = true;
                }
                next.setIsStartFromLauncher(z);
            }
            if (focus.inHwMagicWindowingMode()) {
                appMode.addNewTaskFlag(focus, next);
            }
            if (focus.isActivityTypeHome() && this.mService.getHwMagicWinEnabled(getPackageName((ActivityRecord) next)) && !HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
                this.mService.getWmsPolicy().setOpenAppAnimation();
            }
            if (isDefaultLandOrientation(next.info.screenOrientation)) {
                next.mIsFullScreenVideoInLandscape = true;
            }
        } else if (options != null) {
            this.mMagicWinSplitMng.multWindowModeProcess(focus, options.getLaunchWindowingMode());
        }
    }

    public /* synthetic */ void lambda$new$25$HwMagicWinAmsPolicy(List params, Bundle result) {
        TaskRecord task;
        Slog.i(TAG, "### Execute -> overrideIntentForHwMagicWin");
        HwActivityRecord next = (HwActivityRecord) params.get(1);
        boolean isNewTask = ((Boolean) params.get(3)).booleanValue();
        ActivityOptions options = (ActivityOptions) params.get(4);
        HwActivityRecord focus = ActivityRecord.forToken((IBinder) params.get(0));
        HwMagicModeBase appMode = this.mService.getMode(getPackageName((ActivityRecord) focus));
        if (appMode.checkStatus(focus, next) && !isOtherMultiWinOptions(options, next, focus)) {
            String nextPkg = getPackageName((ActivityRecord) next);
            String focusPkg = getPackageName((ActivityRecord) focus);
            if (focus == null || ((!focus.inHwMultiStackWindowingMode() && !focus.inFreeformWindowingMode()) || focusPkg == 0 || !focusPkg.equals(nextPkg))) {
                if (HwMwUtils.IS_FOLD_SCREEN_DEVICE && next.isStartFromLauncher()) {
                    next.setIsStartFromLauncher(false);
                    if (!isMainActivity(next)) {
                        return;
                    }
                }
                ActivityRecord reusedActivity = ActivityRecord.forToken((IBinder) params.get(2));
                if (!(isNewTask || reusedActivity == null || (task = reusedActivity.getTaskRecord()) == null || task.getTopActivity() != null || task == focus.getTaskRecord())) {
                    isNewTask = true;
                }
                if (isMainActivity(next)) {
                    focus.mStartingWindowState = 0;
                }
                if (!focus.inHwMagicWindowingMode()) {
                    if (isMainActivity(next)) {
                        this.mModeSwitcher.moveToMagicWinFromFullscreenForMain(focus, next);
                    } else if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE && (!HwMwUtils.IS_TABLET || !this.mService.getConfig().isSupportAppTaskSplitScreen(next.packageName))) {
                        appMode.overrideIntent(focus, next, isNewTask);
                    } else if (!isNewTask) {
                        this.mModeSwitcher.moveToMagicWinFromFullscreenForTah(focus, next);
                    } else {
                        return;
                    }
                } else if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE || !isNewTask || focusPkg == null || focusPkg.equals(nextPkg)) {
                    appMode.setOrigActivityToken(focus);
                    appMode.overrideIntent(focus, next, isNewTask);
                } else {
                    Slog.d(TAG, "overrideIntentForHwMagicWin start another app");
                    return;
                }
                if (next.isActivityTypeHome() || isNewTask) {
                    this.mService.getUIController().updateSplitBarVisibility(false);
                }
                if (next.inHwMagicWindowingMode() && isMainActivity(next)) {
                    startRelateActivityIfNeed(next, false);
                }
                if (next.inHwMagicWindowingMode() && this.mService.isMiddle(next) && isPkgInLogoffStatus(nextPkg, next.mUserId)) {
                    Slog.d(TAG, "overrideIntentForHwMagicWin not login set to full");
                    next.setBounds(null);
                    next.setWindowingMode(1);
                }
                if (next.intent != null) {
                    next.intent.removeFlags(65536);
                }
            }
        }
    }

    public boolean isHomeStackHotStart(HwActivityRecord focus, HwActivityRecord next) {
        ActivityRecord activityRecord;
        if (!(focus == null || next == null || !next.inHwMagicWindowingMode())) {
            if (!(next.launchedFromUid != next.appInfo.uid && focus.appInfo.uid == next.appInfo.uid)) {
                return false;
            }
            int homeUid = -1;
            if (!(next.getDisplay() == null || next.getDisplay().getHomeStack() == null || (activityRecord = next.getDisplay().getHomeStack().getTopActivity()) == null)) {
                homeUid = activityRecord.appInfo.uid;
            }
            if (homeUid == -1 || homeUid != next.launchedFromUid) {
                return false;
            }
            return true;
        }
        return false;
    }

    public /* synthetic */ void lambda$new$26$HwMagicWinAmsPolicy(List params, Bundle result) {
        Slog.i(TAG, "### Execute -> clearTask");
        int flag = ((Integer) params.get(1)).intValue();
        ActivityRecord next = ActivityRecord.forToken((IBinder) params.get(0));
        ActivityRecord current = ActivityRecord.forToken((IBinder) params.get(2));
        if (next == null || current == null) {
            result.putBoolean("RESULT_CLEAR_TASK", false);
            return;
        }
        if (!((67108864 & flag) != 0 || 2 == next.launchMode) || !this.mService.isHomePage(getPackageName(current), getClassName(current)) || isMainActivity(next)) {
            result.putBoolean("RESULT_CLEAR_TASK", false);
        } else {
            result.putBoolean("RESULT_CLEAR_TASK", true);
        }
    }

    private boolean isOtherMultiWinOptions(ActivityOptions options, HwActivityRecord next, HwActivityRecord focus) {
        int windowMode;
        if (options == null || ((windowMode = options.getLaunchWindowingMode()) == 102 && getPackageName((ActivityRecord) next) != null && getPackageName((ActivityRecord) next).equals(getRealPkgName(focus)) && focus.inHwMagicWindowingMode() && focus.mUserId == next.mUserId)) {
            return false;
        }
        if (WindowConfiguration.isHwMultiStackWindowingMode(windowMode) || windowMode == 5) {
            return true;
        }
        return false;
    }

    public ActivityRecord getActvityByPosition(int position) {
        if (getTopActivity() == null) {
            return null;
        }
        return getActvityByPosition(getTopActivity(), position, 0);
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x0069  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x0067 A[SYNTHETIC] */
    public ActivityRecord getActvityByPosition(ActivityRecord focus, int windowPosition, int windowIndex) {
        boolean realPosition;
        if (focus == null || focus.getActivityStack() == null) {
            Slog.w(TAG, "overrideIntentForHwMagicWin getActvityByPosition the focus or stack is null");
            return null;
        }
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            ArrayList<TaskRecord> taskHistory = focus.getActivityStack().getAllTasks();
            int offsetIndex = 0;
            for (int taskIndex = taskHistory.size() - 1; taskIndex >= 0; taskIndex--) {
                ArrayList<ActivityRecord> activityRecords = taskHistory.get(taskIndex).mActivities;
                for (int activityIndex = activityRecords.size() - 1; activityIndex >= 0; activityIndex--) {
                    HwActivityRecord activity = (HwActivityRecord) activityRecords.get(activityIndex);
                    if (!activity.finishing) {
                        if (windowPosition != this.mService.getBoundsPosition(activity.getRequestedOverrideBounds())) {
                            if (windowPosition != this.mService.getBoundsPosition(activity.mLastBound) || !this.mFullScreenBounds.equals(activity.getRequestedOverrideBounds())) {
                                realPosition = false;
                                if (!realPosition || windowPosition == 0) {
                                    if (offsetIndex != windowIndex) {
                                        return activity;
                                    }
                                    offsetIndex++;
                                }
                            }
                        }
                        realPosition = true;
                        if (!realPosition) {
                        }
                        if (offsetIndex != windowIndex) {
                        }
                    }
                }
            }
            return null;
        }
    }

    private boolean isLeftWindowHomePage(ActivityRecord focus) {
        ActivityRecord masterTop = getActvityByPosition(focus, 1, 0);
        if (masterTop != null) {
            return this.mService.isHomePage(getPackageName(masterTop), getClassName(masterTop));
        }
        Slog.e(TAG, "isLeftWindowHomePage masterTop is null ");
        return false;
    }

    public void finishMagicWindow(ActivityRecord currentActivityRecord, boolean isFinishAll) {
        if (currentActivityRecord != null) {
            boolean isFinishActivity = isFinishAll;
            boolean isAnyActivityFinished = false;
            for (ActivityRecord ar : getAllActivities(currentActivityRecord.getActivityStack())) {
                if (!isFinishActivity || isRelatedInSlave(ar)) {
                    if (currentActivityRecord == ar) {
                        isFinishActivity = true;
                    }
                } else if (isFinishActivity && (this.mService.isSlave(ar) || (this.mService.getBoundsPosition(((HwActivityRecord) ar).mLastBound) == 2 && this.mFullScreenBounds.equals(ar.getRequestedOverrideBounds())))) {
                    ar.getActivityStack().finishActivityLocked(ar, 0, (Intent) null, MAGIC_WINDOW_FINISH_EVENT, true, false);
                    isAnyActivityFinished = true;
                }
            }
            if (isAnyActivityFinished) {
                this.mActivityTaskManager.getStackSupervisor().scheduleIdleLocked();
            }
        }
    }

    public boolean moveWindow(ActivityRecord targetActivity, int position) {
        if (this.mMagicWinSplitMng.isPkgSpliteScreenMode(targetActivity, false) || (isMainActivity(targetActivity) && position == 3)) {
            return false;
        }
        if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE || position != 3) {
            Slog.i(TAG, "moveWindow, targetActivity=" + targetActivity + ",position = " + position);
            setWindowBoundsLocked(targetActivity, this.mService.getBounds(position, getPackageName(targetActivity)));
            return true;
        }
        Message msg = this.mService.mHandler.obtainMessage(14);
        msg.obj = targetActivity;
        this.mService.mHandler.removeMessages(14);
        this.mService.mHandler.sendMessageDelayed(msg, 100);
        return true;
    }

    public String getRealPkgName(ActivityRecord ar) {
        if (ar == null || ar.getTaskRecord() == null || ar.getTaskRecord().realActivity == null) {
            return null;
        }
        return ar.getTaskRecord().realActivity.getPackageName();
    }

    public String getClassName(ActivityRecord r) {
        if (r == null || r.intent == null || r.intent.getComponent() == null) {
            return null;
        }
        return r.intent.getComponent().getClassName();
    }

    private String getClassName(Intent intent) {
        if (intent == null || intent.getComponent() == null) {
            return null;
        }
        return intent.getComponent().getClassName();
    }

    public String getPackageName(ActivityRecord r) {
        if (r == null || r.intent == null || r.intent.getComponent() == null) {
            return null;
        }
        return r.intent.getComponent().getPackageName();
    }

    public String getPackageName(Intent intent) {
        if (intent == null || intent.getComponent() == null) {
            return null;
        }
        return intent.getComponent().getPackageName();
    }

    public String getPackageName(ActivityDisplay activityDisplay) {
        if (activityDisplay.getTopStack() == null || activityDisplay.getTopStack().topTask() == null) {
            return null;
        }
        TaskRecord taskRecord = activityDisplay.getTopStack().topTask();
        if (taskRecord.realActivity == null || taskRecord.realActivity.getPackageName() == null) {
            return null;
        }
        return taskRecord.realActivity.getPackageName();
    }

    public void setWindowBoundsLocked(ActivityRecord activityRecord, Rect bounds) {
        Slog.i(TAG, "setWindowBoundsLocked: activityRecord = " + activityRecord + ",bounds = " + bounds);
        HwActivityRecord resizeActivity = (HwActivityRecord) activityRecord;
        if (!this.mFullScreenBounds.equals(activityRecord.getRequestedOverrideBounds())) {
            resizeActivity.mLastBound = new Rect(resizeActivity.getRequestedOverrideBounds());
        }
        resizeActivity.setBounds(bounds);
        resize(resizeActivity);
    }

    public /* synthetic */ void lambda$new$27$HwMagicWinAmsPolicy(List params, Bundle result) {
        int requestedOrientation = ((Integer) params.get(1)).intValue();
        HwActivityRecord activityRecord = ActivityRecord.forToken((IBinder) params.get(0));
        if (!this.mService.getHwMagicWinEnabled(getPackageName((ActivityRecord) activityRecord))) {
            Slog.i(TAG, "resizeSpecialVideoInMagicWindowMode, package not supported");
        } else if (this.mMagicWinSplitMng.isPkgSpliteScreenMode(activityRecord, true)) {
            Slog.i(TAG, "resizeSpecialVideoInMagicWindowMode in PkgSpliteScreenMode, do nothing");
        } else if (activityRecord != getTopActivity()) {
            Slog.i(TAG, "resizeSpecialVideoInMagicWindowMode, activity not top running");
        } else {
            Slog.i(TAG, "resizeSpecialVideoInMagicWindowMode,requested = " + requestedOrientation);
            if (ActivityInfo.isFixedOrientationLandscape(requestedOrientation)) {
                activityRecord.mIsFullScreenVideoInLandscape = true;
                Slog.i(TAG, "set mIsFullScreenVideoInLandscape to true, activity = " + activityRecord);
            }
            if (HwMwUtils.IS_FOLD_SCREEN_DEVICE && activityRecord.inHwMagicWindowingMode() && !activityRecord.isTopRunningActivity()) {
                result.putBoolean("RESULT_REJECT_ORIENTATION", true);
            }
            if (this.mService.isSupportFullScreenVideo(activityRecord.packageName) && activityRecord.inHwMagicWindowingMode()) {
                resizeActivityInMagicWindowMode(activityRecord, requestedOrientation);
            }
        }
    }

    private void resizeActivityInMagicWindowMode(HwActivityRecord activityRecord, int requestedOrientation) {
        boolean isInFullScreen = this.mFullScreenBounds.equals(activityRecord.getRequestedOverrideBounds());
        boolean isEnterMwOrientation = ActivityInfo.isFixedOrientationLandscape(requestedOrientation);
        boolean isExitMwOrientation = ActivityInfo.isFixedOrientationPortrait(requestedOrientation) || -1 == requestedOrientation;
        if (!isInFullScreen && isEnterMwOrientation) {
            sendMessageForSetMultiWinCameraProp(false);
            setWindowBoundsLocked(activityRecord, this.mFullScreenBounds);
            activityRecord.getMergedOverrideConfiguration().orientation = 2;
            this.mService.getUIController().updateSplitBarVisibility(false);
            Message msg = this.mService.mHandler.obtainMessage(12);
            msg.obj = activityRecord;
            this.mService.mHandler.removeMessages(12);
            this.mService.mHandler.sendMessageDelayed(msg, 50);
        } else if (!isInFullScreen || !isExitMwOrientation || activityRecord.mLastBound == null) {
            Slog.i(TAG, "do nothing, bounds = " + activityRecord.getRequestedOverrideBounds());
            return;
        } else {
            sendMessageForSetMultiWinCameraProp(true);
            setWindowBoundsLocked(activityRecord, activityRecord.mLastBound);
            activityRecord.getMergedOverrideConfiguration().orientation = 1;
            this.mService.getUIController().updateSplitBarVisibility(true);
            updateStackVisibility(activityRecord, true);
        }
        activityRecord.ensureActivityConfiguration(0, true);
    }

    public void updateStackVisibility(ActivityRecord activity, boolean isWallpaerVisible) {
        this.mService.getUIController().updateMagicWindowWallpaperVisibility(Boolean.valueOf(isWallpaerVisible));
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            if (activity != null) {
                if (activity.getActivityStack() != null) {
                    activity.getActivityStack().ensureActivitiesVisibleLocked((ActivityRecord) null, 0, false);
                }
            }
            ActivityRecord master = getActvityByPosition(activity, 1, 0);
            if (master != null && isWallpaerVisible) {
                setMagicWindowToPauseInner(master);
            }
        }
    }

    private void resize(HwActivityRecord r) {
        this.mAms.mWindowManager.deferSurfaceLayout();
        try {
            r.resize();
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "resize error");
        } catch (Throwable th) {
            this.mAms.mWindowManager.continueSurfaceLayout();
            throw th;
        }
        this.mAms.mWindowManager.continueSurfaceLayout();
    }

    private boolean overrideRotationForHwMagicWin(String packageName) {
        return packageName != null && this.mService.getHwMagicWinEnabled(packageName);
    }

    public void removeCachedMagicWindowApps(Set<String> apps) {
        List<ActivityManager.RunningAppProcessInfo> appProcessList = this.mAms.getRunningAppProcesses();
        if (appProcessList != null) {
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcessList) {
                String[] pkNameList = appProcess.pkgList;
                for (String pkName : pkNameList) {
                    if (apps.contains(pkName)) {
                        removeRecentMagicWindowApp(pkName);
                    }
                }
            }
        }
    }

    public void removeRecentMagicWindowApp(String pkgName) {
        ActivityManagerService activityManagerService = this.mAms;
        activityManagerService.forceStopPackage(pkgName, activityManagerService.getCurrentUser().id);
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            List<ActivityManager.RecentTaskInfo> recentTasks = this.mAms.getRecentTasks(100, 1, this.mAms.getCurrentUser().id).getList();
            if (recentTasks != null) {
                for (ActivityManager.RecentTaskInfo recentTaskInfo : recentTasks) {
                    if (recentTaskInfo.realActivity != null && pkgName.equals(recentTaskInfo.realActivity.getPackageName())) {
                        this.mAms.removeTask(recentTaskInfo.persistentId);
                        this.mService.getConfig().removeReportLoginStatus(getJoinStr(pkgName, recentTaskInfo.userId));
                    }
                }
            }
        }
    }

    private void overrideFinishForJudgeHost(ActivityRecord ar) {
        String pkgName = getPackageName(ar);
        if (!this.mService.isJudgeHost(pkgName, getClassName(ar)) || this.mService.getAppSupportMode(pkgName) == 0) {
            return;
        }
        if (isAutoFinish(ar)) {
            judgeHostAutoFinish(ar);
        } else {
            judgeHost(ar);
        }
    }

    private boolean isAutoFinish(ActivityRecord ar) {
        ActivityStack stack = ar.getActivityStack();
        if (stack == null) {
            return false;
        }
        ActivityRecord top = stack.getTopActivity();
        if (top == null) {
            return true;
        }
        if (getClassName(ar).equals(getClassName(top))) {
            return false;
        }
        return true;
    }

    private void judgeHost(ActivityRecord ar) {
        ActivityRecord realHost;
        Slog.i(TAG, "back key judgeHost " + ar);
        ActivityStack stack = ar.getActivityStack();
        if (stack != null && stack.getTaskHistory().size() == 1) {
            TaskRecord task = ar.getTaskRecord();
            if (task.getChildCount() > 1 && (realHost = task.getChildAt(0)) != null) {
                int position = this.mService.getBoundsPosition(realHost.getRequestedOverrideBounds());
                if (this.mService.isSlave(ar)) {
                    if (position == 3) {
                        return;
                    }
                    if (HwMwUtils.IS_FOLD_SCREEN_DEVICE && position == 5) {
                        return;
                    }
                }
                ActivityRecord masterNext = getActvityByPosition(ar, 1, 1);
                if (this.mService.isMaster(ar) && masterNext != null) {
                    realHost = masterNext;
                }
                String pkgName = getPackageName(realHost);
                String clsName = getClassName(realHost);
                Slog.i(TAG, "JudgeHost set new host " + realHost);
                this.mService.setHost(pkgName, clsName);
            }
        }
    }

    private void judgeHostAutoFinish(ActivityRecord ar) {
        Slog.i(TAG, "AutoFinish judgeHost " + ar);
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            Iterator<ActivityRecord> it = getAllActivities(ar.getActivityStack()).iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ActivityRecord top = it.next();
                if (!isRelatedActivity(top)) {
                    Slog.i(TAG, "JudgeHostAutoFinish set new host " + top);
                    this.mService.setHost(getPackageName(top), getClassName(top));
                    break;
                }
            }
        }
    }

    private void adjustBounds(Rect bounds, float scale) {
        bounds.set(bounds.left, bounds.top, ((int) ((((float) bounds.width()) * scale) + 0.5f)) + bounds.left, ((int) ((((float) bounds.height()) * scale) + 0.5f)) + bounds.top);
    }

    public void setWindowType(IBinder token, int type) {
        HwActivityRecord activity = ActivityRecord.forToken(token);
        if (activity != null) {
            activity.mMagicWindowPageType = type;
        }
    }

    public boolean isSpecTransActivityPreDefined(HwActivityRecord activity) {
        String clasName = getClassName((ActivityRecord) activity);
        if (activity == null || TRANSITION_ACTIVITIES.contains(clasName)) {
            return true;
        }
        HwMagicWindowConfig config = this.mService.getConfig();
        String pkg = getPackageName((ActivityRecord) activity);
        if (!this.mService.isSupportOpenCapability() || !config.getOpenCapAppConfigs().containsKey(pkg)) {
            return false;
        }
        return config.isSpecTransActivity(pkg, clasName);
    }

    public boolean isSpecTransActivity(HwActivityRecord activity) {
        return isSpecTransActivityPreDefined(activity) || activity.mMagicWindowPageType != 1;
    }

    private void requestRotation(ActivityRecord activity) {
        if (HwMwUtils.IS_FOLD_SCREEN_DEVICE && activity.getActivityStack().getWindowingMode() == 1 && !((HwActivityRecord) activity).mIsFullScreenVideoInLandscape) {
            if ((this.mContext.getResources().getConfiguration().orientation == 2) && this.mService.getHwMagicWinEnabled(getRealPkgName(activity))) {
                activity.setRequestedOrientation(1);
            }
        }
    }

    public boolean isFoldedState() {
        if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE || HwFoldScreenManagerEx.getDisplayMode() == 1) {
            return false;
        }
        return true;
    }

    public /* synthetic */ void lambda$new$28$HwMagicWinAmsPolicy(List params, Bundle result) {
        Slog.i(TAG, "### Execute -> canPauseInHwMultiwin");
        IBinder resumedBinder = (IBinder) params.get(0);
        ActivityRecord next = ActivityRecord.forToken((IBinder) params.get(1));
        this.mIsLaunchFromHomeOrRecent = false;
        if (!this.mService.isSupportMultiResume(getPackageName(next))) {
            Slog.i(TAG, "canPauseInHwMagicWin : app is not support multi-resume");
            result.putBoolean("CAN_PAUSE", true);
            return;
        }
        ActivityRecord resumedActivity = ActivityRecord.forToken(resumedBinder);
        if (resumedActivity.finishing) {
            Slog.i(TAG, "canPauseInHwMagicWin : app is finishing");
            return;
        }
        boolean isFocusSlaveChangeToMaster = this.mService.isSlave(resumedActivity) && this.mService.isMaster(next);
        boolean isFocusMasterChangeToSlave = this.mService.isMaster(resumedActivity) && this.mService.isSlave(next) && this.mService.getConfig().isPkgSupport(getPackageName(resumedActivity), 1);
        if (isFocusSlaveChangeToMaster || isFocusMasterChangeToSlave) {
            result.putBoolean("CAN_PAUSE", false);
        } else {
            result.putBoolean("CAN_PAUSE", true);
        }
    }

    public void setMagicWindowToPause(ActivityRecord activity) {
        if (activity.isState(ActivityStack.ActivityState.RESUMED) && this.mService.isSupportMultiResume(getPackageName(activity))) {
            setMagicWindowToPauseInner(activity);
        }
    }

    private void setMagicWindowToPauseInner(ActivityRecord activity) {
        if (activity != null && activity.getActivityStack() != null) {
            ActivityStack stack = activity.getActivityStack();
            if (stack.mPausingActivity != null) {
                Slog.w(TAG, "activity has pausing in magic window");
                return;
            }
            stack.mPausingActivity = activity;
            activity.setState(ActivityStack.ActivityState.PAUSING, "pause activity in magic window");
            if (activity.attachedToProcess()) {
                try {
                    this.mActivityTaskManager.getLifecycleManager().scheduleTransaction(activity.app.getThread(), activity.appToken, PauseActivityItem.obtain(activity.finishing, false, activity.configChangeFlags, false));
                } catch (RemoteException e) {
                    Slog.e(TAG, "RemoteException pause activity in magic window", e);
                    stack.mPausingActivity = null;
                }
            }
        }
    }

    public boolean isInHwDoubleWindow() {
        ActivityStack focusedStack = getFocusedTopStack();
        if (focusedStack == null) {
            return false;
        }
        HwActivityRecord top = focusedStack.getTopActivity();
        if (this.mService.isMaster(top) || this.mService.isSlave(top)) {
            return true;
        }
        return false;
    }

    private ActivityRecord getActvityByNameOnTask(ActivityRecord focus, String actName) {
        if (actName.isEmpty()) {
            return null;
        }
        ArrayList<ActivityRecord> actHistory = focus.getTaskRecord().mActivities;
        for (int actIndex = actHistory.size() - 1; actIndex >= 0; actIndex--) {
            ActivityRecord activity = actHistory.get(actIndex);
            if (actName.equals(getClassName(activity)) && activity.mUserId == focus.mUserId) {
                return activity;
            }
        }
        return null;
    }

    private boolean isNeedStartOrMoveRight(ActivityRecord resumeActivity, String pkgName) {
        boolean functionEnabled = isSupportMainRelatedMode(pkgName) && this.mService.getHwMagicWinEnabled(pkgName);
        if (pkgName == null || !functionEnabled || isPkgInLogoffStatus(pkgName, resumeActivity.mUserId)) {
            Slog.i(TAG, "isNeedStartOrMoveRight return for package");
            return false;
        }
        ActivityStack resumeStack = resumeActivity.getTaskRecord().getStack();
        if (resumeStack != null && resumeStack.getWindowingMode() != 1 && resumeStack.getWindowingMode() != 103) {
            return false;
        }
        return ((HwMwUtils.IS_FOLD_SCREEN_DEVICE && !isFoldedState()) || (HwMwUtils.IS_TABLET && (this.mContext.getResources().getConfiguration().orientation == 2 || resumeActivity.getWindowingMode() == 103))) && !this.mMagicWinSplitMng.isPkgSpliteScreenMode(resumeActivity, true);
    }

    public boolean startRelateActivityIfNeed(ActivityRecord resumeActivity, boolean forceStart) {
        String pkgName = getPackageName(resumeActivity);
        if (isNeedStartOrMoveRight(resumeActivity, pkgName)) {
            String relateActName = this.mService.getConfig().getRelateActivity(pkgName);
            ActivityRecord relatedAr = getActvityByNameOnTask(resumeActivity, relateActName);
            ActivityRecord slaveAr = getActvityByPosition(resumeActivity, 2, 0);
            Slog.i(TAG, "startRelateActivityIfNeed resumeActivity=" + resumeActivity + " slaveAr=" + slaveAr);
            boolean isRelatedActivityFinishing = relatedAr != null && relatedAr.finishing;
            if (((relatedAr == null || isRelatedActivityFinishing) && slaveAr == null && isMainActivity(resumeActivity)) || forceStart) {
                startRelateActivity(pkgName, relateActName, resumeActivity);
                return true;
            } else if (relatedAr != null) {
                Slog.d(TAG, "startRelateActivityIfNeed no existing right activity, move empty activity to right");
                Rect slaveBound = this.mService.getBounds(2, pkgName);
                if (this.mService.getBoundsPosition(relatedAr.getRequestedOverrideBounds()) != 2) {
                    relatedAr.setBounds(slaveBound);
                }
            }
        }
        return false;
    }

    private void startRelateActivity(String pkgName, String relateActName, ActivityRecord mainAr) {
        if (mainAr.getActivityStack() != getFocusedTopStack()) {
            Slog.w(TAG, "startRelateActivity is not focused top stack");
            return;
        }
        Intent intent = new Intent();
        intent.setClassName(pkgName, relateActName);
        intent.setFlags(268435456);
        Message msg = this.mService.mHandler.obtainMessage(13);
        msg.obj = intent;
        msg.arg1 = mainAr.mUserId;
        Slog.i(TAG, "start relate activity " + mainAr.mUserId);
        this.mService.mHandler.removeMessages(13);
        this.mService.mHandler.sendMessage(msg);
    }

    public boolean isPkgInLoginStatus(String pkgName, int userId) {
        return this.mService.getConfig().isInLoginStatus(getJoinStr(pkgName, userId)) && this.mService.getConfig().isSupportAppTaskSplitScreen(pkgName);
    }

    public boolean isPkgInLogoffStatus(String pkgName, int userId) {
        return !this.mService.getConfig().isInLoginStatus(getJoinStr(pkgName, userId)) && this.mService.getConfig().isSupportAppTaskSplitScreen(pkgName);
    }

    public /* synthetic */ void lambda$new$29$HwMagicWinAmsPolicy(List params, Bundle result) {
        TaskRecord task = (TaskRecord) params.get(0);
        int dragMode = ((Integer) params.get(3)).intValue();
        Rect[] newBounds = this.mService.getConfig().adjustBoundsForResize((Rect) params.get(1), (Rect) params.get(2));
        if (newBounds != null && newBounds.length > 1) {
            executeDragWindow(task, newBounds[0], newBounds[1], dragMode);
        }
    }

    private void executeDragWindow(TaskRecord task, Rect leftBounds, Rect rightBounds, int dragMode) {
        String dragPkgName = null;
        Rect tmpLeftBounds = null;
        Rect tmpRightBounds = null;
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            for (int activityNdx = task.mActivities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) task.mActivities.get(activityNdx);
                if (!r.finishing && r.app != null) {
                    if (this.mService.isMaster(r)) {
                        setWindowBoundsLocked(r, this.mService.getConfig().isRtl() ? rightBounds : leftBounds);
                        dragPkgName = r.packageName;
                        tmpLeftBounds = leftBounds;
                    } else if (this.mService.isSlave(r)) {
                        setWindowBoundsLocked(r, this.mService.getConfig().isRtl() ? leftBounds : rightBounds);
                        tmpRightBounds = rightBounds;
                    }
                }
            }
        }
        if (tmpLeftBounds != null && tmpRightBounds != null && !tmpLeftBounds.equals(this.mFullScreenBounds) && !tmpRightBounds.equals(this.mFullScreenBounds) && tmpLeftBounds.top == tmpRightBounds.top) {
            Slog.i(TAG, "execute drag for magic. left:" + tmpLeftBounds + ", right:" + tmpRightBounds);
            HwMagicWinStatistics.getInstance().startTick(this.mService.getConfig(), dragPkgName, dragMode);
            this.mService.getConfig().updateAppDragBounds(dragPkgName, tmpLeftBounds, tmpRightBounds, dragMode);
            sendMsgToWriteSettingsXml();
        }
    }

    public void sendMsgToWriteSettingsXml() {
        Message msg = this.mService.mHandler.obtainMessage(15);
        this.mService.mHandler.removeMessages(15);
        this.mService.mHandler.sendMessage(msg);
    }

    public int getTaskPosition(String pkg, int taskId) {
        ActivityDisplay display = this.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        boolean isPkgSplitModeSupport = this.mService.getConfig().isSupportAppTaskSplitScreen(pkg) && ((HwMwUtils.IS_TABLET && this.mContext.getResources().getConfiguration().orientation == 2) || (HwMwUtils.IS_FOLD_SCREEN_DEVICE && !isFoldedState()));
        if (isPkgSplitModeSupport && !this.mMagicWinSplitMng.isMainStackInMwMode(getTopActivity())) {
            return -1;
        }
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                String packageName = getRealPkgName(stack.getTopActivity());
                TaskRecord taskRecord = stack.taskForIdLocked(taskId);
                if (packageName != null && pkg.equals(packageName)) {
                    if (taskRecord != null) {
                        if (isPkgSplitModeSupport && stack.getWindowingMode() == 1) {
                            stack.setWindowingMode(103);
                        }
                        if (!stack.inHwMagicWindowingMode()) {
                            return -1;
                        }
                        int position = this.mService.getBoundsPosition(stack.getRequestedOverrideBounds());
                        if (!this.mService.getConfig().isSupportAppTaskSplitScreen(pkg) || position != 3) {
                            return position;
                        }
                        return 0;
                    }
                }
            }
            return -1;
        }
    }

    public void setTaskPosition(String pkg, int taskId, int targetPosition) {
        ActivityStack stack = getFocusedTopStack();
        Slog.i(TAG, "setTaskPosition : stack = " + stack + ", pkg = " + pkg);
        if (stack != null && pkg != null) {
            if (targetPosition == 1) {
                Slog.i(TAG, "setTaskPosition : return for move left on stack");
            } else if (pkg.equals(getRealPkgName(stack.getTopActivity())) && stack.isInStackLocked(stack.taskForIdLocked(taskId))) {
                Slog.i(TAG, "setTaskPosition function: call resizeStack");
                this.mMagicWinSplitMng.setTaskPosition(pkg, taskId, targetPosition);
            }
        }
    }

    public void setLoginStatus(String pkg, int status, int uid) {
        if (this.mService.getConfig().isSupportAppTaskSplitScreen(pkg)) {
            this.mService.getConfig().setLoginStatus(getJoinStr(pkg, UserHandle.getUserId(uid)), status);
        }
    }

    public int getStackUserId(ActivityStack stack) {
        int currentUserId = this.mAms.getCurrentUser().id;
        if (stack == null) {
            return currentUserId;
        }
        ActivityRecord topAr = stack.getTopActivity();
        return topAr == null ? currentUserId : topAr.mUserId;
    }

    public String getJoinStr(String pkg, int userId) {
        if (pkg == null) {
            return "" + userId;
        }
        return pkg + userId;
    }

    public boolean isInMagicWindowMode(int taskId) {
        ActivityDisplay display = this.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                if (stack.taskForIdLocked(taskId) != null) {
                    return stack.inHwMagicWindowingMode();
                }
            }
            return false;
        }
    }

    static /* synthetic */ void lambda$new$30(List params, Bundle result) {
        int frameHeight = 0;
        ActivityRecord activityRecord = (ActivityRecord) params.get(0);
        Intent intent = (Intent) params.get(1);
        TaskRecord taskRecord = activityRecord.getTaskRecord();
        int frameWidth = taskRecord != null ? taskRecord.getBounds().width() : 0;
        if (taskRecord != null) {
            frameHeight = taskRecord.getBounds().height();
        }
        int startX = frameWidth / 4;
        int startY = frameHeight / 4;
        int width = frameWidth / 2;
        int height = frameHeight / 2;
        activityRecord.mAppWindowToken.getDisplayContent().mAppTransition.overridePendingAppTransitionScaleUp(startX, startY, width, height);
        if (intent.getSourceBounds() == null) {
            intent.setSourceBounds(new Rect(startX, startY, startX + width, startY + height));
        }
    }

    public void finishActivitiesAfterTopActivity() {
        ActivityStack stack;
        TaskRecord task;
        if (HwMwUtils.IS_FOLD_SCREEN_DEVICE && this.mContext.getResources().getConfiguration().orientation == 2 && (stack = getFocusedTopStack()) != null) {
            ActivityRecord topActivity = stack.getTopActivity();
            if (this.mService.getHwMagicWinEnabled(getRealPkgName(topActivity)) && (task = topActivity.getTaskRecord()) != null) {
                Iterator it = task.mActivities.iterator();
                while (it.hasNext()) {
                    ActivityRecord activity = (ActivityRecord) it.next();
                    if (((HwActivityRecord) activity).mCreateTime > ((HwActivityRecord) topActivity).mCreateTime) {
                        stack.finishActivityLocked(activity, 0, (Intent) null, MAGIC_WINDOW_FINISH_EVENT, true, false);
                    }
                }
            }
        }
    }

    public /* synthetic */ void lambda$new$31$HwMagicWinAmsPolicy(List params, Bundle result) {
        Configuration configuration = (Configuration) params.get(0);
        int i = 1;
        float density = ((Float) params.get(1)).floatValue();
        Rect bounds = configuration.windowConfiguration.getBounds();
        configuration.screenHeightDp = (int) (((float) bounds.height()) / density);
        configuration.windowConfiguration.setAppBounds(bounds);
        if (!isInHwDoubleWindow() || this.mService.getConfig().isSupportAppTaskSplitScreen(getPackageName(getTopActivity()))) {
            if (configuration.screenWidthDp > configuration.screenHeightDp) {
                i = 2;
            }
            configuration.orientation = i;
            return;
        }
        configuration.orientation = 1;
    }

    public void calcHwSplitStackBounds() {
        Map<Integer, List<Rect>> modeBounds = new HashMap<>();
        ActivityDisplay display = this.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        modeBounds.put(0, calcHwSplitStackBounds(display, 0));
        modeBounds.put(1, calcHwSplitStackBounds(display, 1));
        modeBounds.put(2, calcHwSplitStackBounds(display, 2));
        Rect leftRect = modeBounds.get(0).get(0);
        Rect rightRect = modeBounds.get(0).get(1);
        if (leftRect == null || rightRect == null || leftRect.top != rightRect.top) {
            Slog.e(TAG, "calc split bounds error.");
        } else {
            this.mService.getConfig().updateAppBoundsFromMode(modeBounds);
        }
    }

    public void pauseTopWhenScreenOff() {
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            ActivityRecord topActivity = getTopActivity();
            if (topActivity != null && topActivity.inHwMagicWindowingMode()) {
                if (!topActivity.isState(ActivityStack.ActivityState.RESUMED)) {
                    ActivityRecord slaveTop = getActvityByPosition(topActivity, 2, 0);
                    ActivityRecord masterTop = getActvityByPosition(topActivity, 1, 0);
                    if (slaveTop != null) {
                        if (masterTop != null) {
                            if (topActivity == slaveTop) {
                                setMagicWindowToPauseInner(masterTop);
                            } else {
                                setMagicWindowToPauseInner(slaveTop);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isDefaultFullscreenActivity(ActivityRecord ar) {
        return this.mService.getConfig().isDefaultFullscreenActivity(getPackageName(ar), getClassName(ar));
    }

    public void forceStopPackage(String packageName) {
        Message msg = this.mService.mHandler.obtainMessage(6);
        msg.obj = packageName;
        this.mService.mHandler.sendMessage(msg);
    }

    private List<Rect> calcHwSplitStackBounds(ActivityDisplay display, int splitRatio) {
        List<Rect> modeBounds = new ArrayList<>();
        Rect leftBound = new Rect();
        Rect rightBound = new Rect();
        HwMultiWindowManager.calcHwSplitStackBounds(display, splitRatio, leftBound, rightBound);
        Rect[] newBounds = this.mService.getConfig().adjustBoundsForResize(leftBound, rightBound);
        if (newBounds == null || newBounds.length <= 1) {
            modeBounds.add(leftBound);
            modeBounds.add(rightBound);
        } else {
            modeBounds.add(newBounds[0]);
            modeBounds.add(newBounds[1]);
        }
        return modeBounds;
    }
}
