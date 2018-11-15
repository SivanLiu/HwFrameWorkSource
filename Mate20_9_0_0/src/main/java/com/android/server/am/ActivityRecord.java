package com.android.server.am;

import android.app.ActivityManager.TaskDescription;
import android.app.ActivityOptions;
import android.app.IApplicationThread;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.PictureInPictureParams.Builder;
import android.app.ResultInfo;
import android.app.WindowConfiguration;
import android.app.servertransaction.ActivityConfigurationChangeItem;
import android.app.servertransaction.ActivityLifecycleItem;
import android.app.servertransaction.ActivityRelaunchItem;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.ClientTransactionItem;
import android.app.servertransaction.MoveToDisplayItem;
import android.app.servertransaction.MultiWindowModeChangeItem;
import android.app.servertransaction.NewIntentItem;
import android.app.servertransaction.PauseActivityItem;
import android.app.servertransaction.PipModeChangeItem;
import android.app.servertransaction.ResumeActivityItem;
import android.app.servertransaction.WindowVisibilityItem;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.service.voice.IVoiceInteractionSession;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.HwVRUtils;
import android.util.Jlog;
import android.util.Log;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.AppTransitionAnimationSpec;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.IApplicationToken.Stub;
import android.view.RemoteAnimationDefinition;
import com.android.internal.R;
import com.android.internal.app.ResolverActivity;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.util.XmlUtils;
import com.android.server.AttributeCache;
import com.android.server.AttributeCache.Entry;
import com.android.server.HwServiceFactory;
import com.android.server.os.HwBootFail;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.AppWindowContainerController;
import com.android.server.wm.AppWindowContainerListener;
import com.android.server.wm.ConfigurationContainer;
import com.android.server.wm.TaskWindowContainerController;
import com.android.server.wm.WindowManagerService;
import com.huawei.android.audio.HwAudioServiceManager;
import huawei.android.hwutil.HwFullScreenDisplay;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ActivityRecord extends AbsActivityRecord implements AppWindowContainerListener {
    static final String ACTIVITY_ICON_SUFFIX = "_activity_icon_";
    private static final String ATTR_COMPONENTSPECIFIED = "component_specified";
    private static final String ATTR_ID = "id";
    private static final String ATTR_LAUNCHEDFROMPACKAGE = "launched_from_package";
    private static final String ATTR_LAUNCHEDFROMUID = "launched_from_uid";
    private static final String ATTR_RESOLVEDTYPE = "resolved_type";
    private static final String ATTR_USERID = "user_id";
    private static final String LEGACY_RECENTS_PACKAGE_NAME = "com.android.systemui.recents";
    private static final String LEGACY_RECENTS_PACKAGE_NAME_LAUNCHER = "com.huawei.android.launcher.quickstep";
    private static final boolean SHOW_ACTIVITY_START_TIME = true;
    static final int STARTING_WINDOW_NOT_SHOWN = 0;
    static final int STARTING_WINDOW_REMOVED = 2;
    static final int STARTING_WINDOW_SHOWN = 1;
    private static final String TAG = "ActivityManager";
    private static final String TAG_CONFIGURATION;
    private static final String TAG_INTENT = "intent";
    private static final String TAG_PERSISTABLEBUNDLE = "persistable_bundle";
    private static final String TAG_SAVED_STATE = "ActivityManager";
    private static final String TAG_STATES = "ActivityManager";
    private static final String TAG_SWITCH = "ActivityManager";
    private static final String TAG_VISIBILITY;
    private static final int UNIPERF_BOOST_OFF = 4;
    public static float mDeviceMaxRatio = -1.0f;
    ProcessRecord app;
    ApplicationInfo appInfo;
    AppTimeTracker appTimeTracker;
    final Stub appToken;
    CompatibilityInfo compat;
    private final boolean componentSpecified;
    int configChangeFlags;
    HashSet<ConnectionRecord> connections;
    long cpuTimeAtResume;
    private long createTime = System.currentTimeMillis();
    boolean deferRelaunchUntilPaused;
    boolean delayedResume;
    long displayStartTime;
    boolean finishing;
    boolean forceNewConfig;
    boolean frontOfTask;
    boolean frozenBeforeDestroy;
    boolean fullscreen;
    long fullyDrawnStartTime;
    boolean hasBeenLaunched;
    final boolean hasWallpaper;
    boolean haveState;
    Bundle icicle;
    private int icon;
    boolean idle;
    boolean immersive;
    private boolean inHistory;
    public final ActivityInfo info;
    final Intent intent;
    private boolean keysPaused;
    private int labelRes;
    long lastLaunchTime;
    long lastVisibleTime;
    int launchCount;
    boolean launchFailed;
    int launchMode;
    long launchTickTime;
    final String launchedFromPackage;
    final int launchedFromPid;
    final int launchedFromUid;
    int lockTaskLaunchMode;
    private int logo;
    boolean mClientVisibilityDeferred;
    private boolean mDeferHidingClient;
    private int[] mHorizontalSizeConfigurations;
    private boolean mIsFloating;
    private boolean mIsTransluent;
    private MergedConfiguration mLastReportedConfiguration;
    private int mLastReportedDisplayId;
    private boolean mLastReportedMultiWindowMode;
    private boolean mLastReportedPictureInPictureMode;
    boolean mLaunchTaskBehind;
    int mRotationAnimationHint = -1;
    private boolean mShowWhenLocked;
    private int[] mSmallestSizeConfigurations;
    final ActivityStackSupervisor mStackSupervisor;
    int mStartingWindowState = 0;
    private ActivityState mState;
    boolean mTaskOverlay = false;
    private final Rect mTmpBounds = new Rect();
    private final Configuration mTmpConfig = new Configuration();
    private boolean mTurnScreenOn;
    private int[] mVerticalSizeConfigurations;
    AppWindowContainerController mWindowContainerController;
    public float maxAspectRatio = 0.0f;
    ArrayList<ReferrerIntent> newIntents;
    final boolean noDisplay;
    private CharSequence nonLocalizedLabel;
    boolean nowVisible;
    final String packageName;
    long pauseTime;
    ActivityOptions pendingOptions;
    HashSet<WeakReference<PendingIntentRecord>> pendingResults;
    boolean pendingVoiceInteractionStart;
    PersistableBundle persistentState;
    PictureInPictureParams pictureInPictureArgs = new Builder().build();
    boolean preserveWindowOnDeferredRelaunch;
    public final String processName;
    public final ComponentName realActivity;
    private int realTheme;
    final int requestCode;
    ComponentName requestedVrComponent;
    final String resolvedType;
    ActivityRecord resultTo;
    final String resultWho;
    ArrayList<ResultInfo> results;
    ActivityOptions returningOptions;
    final boolean rootVoiceInteraction;
    final ActivityManagerService service;
    final String shortComponentName;
    boolean sleeping;
    private long startTime;
    final boolean stateNotNeeded;
    boolean stopped;
    String stringName;
    boolean supportsEnterPipOnTaskSwitch;
    TaskRecord task;
    final String taskAffinity;
    TaskDescription taskDescription;
    private int theme;
    UriPermissionOwner uriPermissions;
    final int userId;
    boolean visible;
    boolean visibleIgnoringKeyguard;
    IVoiceInteractionSession voiceSession;
    private int windowFlags;

    static class Token extends Stub {
        private final String name;
        private final WeakReference<ActivityRecord> weakActivity;

        Token(ActivityRecord activity, Intent intent) {
            this.weakActivity = new WeakReference(activity);
            this.name = intent.getComponent().flattenToShortString();
        }

        private static ActivityRecord tokenToActivityRecordLocked(Token token) {
            if (token == null) {
                return null;
            }
            ActivityRecord r = (ActivityRecord) token.weakActivity.get();
            if (r == null || r.getStack() == null) {
                return null;
            }
            return r;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Token{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            sb.append(this.weakActivity.get());
            sb.append('}');
            return sb.toString();
        }

        public String getName() {
            return this.name;
        }
    }

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ActivityManagerService.TAG);
        stringBuilder.append(ActivityManagerDebugConfig.POSTFIX_CONFIGURATION);
        TAG_CONFIGURATION = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(ActivityManagerService.TAG);
        stringBuilder.append(ActivityManagerDebugConfig.POSTFIX_VISIBILITY);
        TAG_VISIBILITY = stringBuilder.toString();
    }

    private static String startingWindowStateToString(int state) {
        switch (state) {
            case 0:
                return "STARTING_WINDOW_NOT_SHOWN";
            case 1:
                return "STARTING_WINDOW_SHOWN";
            case 2:
                return "STARTING_WINDOW_REMOVED";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown state=");
                stringBuilder.append(state);
                return stringBuilder.toString();
        }
    }

    void dump(PrintWriter pw, String prefix) {
        long now = SystemClock.uptimeMillis();
        pw.print(prefix);
        pw.print("packageName=");
        pw.print(this.packageName);
        pw.print(" processName=");
        pw.println(this.processName);
        pw.print(prefix);
        pw.print("launchedFromUid=");
        pw.print(this.launchedFromUid);
        pw.print(" launchedFromPackage=");
        pw.print(this.launchedFromPackage);
        pw.print(" userId=");
        pw.println(this.userId);
        pw.print(prefix);
        pw.print("app=");
        pw.println(this.app);
        pw.print(prefix);
        pw.println(this.intent.toInsecureStringWithClip());
        pw.print(prefix);
        pw.print("frontOfTask=");
        pw.print(this.frontOfTask);
        pw.print(" task=");
        pw.println(this.task);
        pw.print(prefix);
        pw.print("taskAffinity=");
        pw.println(this.taskAffinity);
        pw.print(prefix);
        pw.print("realActivity=");
        pw.println(this.realActivity.flattenToShortString());
        if (this.appInfo != null) {
            pw.print(prefix);
            pw.print("baseDir=");
            pw.println(this.appInfo.sourceDir);
            if (!Objects.equals(this.appInfo.sourceDir, this.appInfo.publicSourceDir)) {
                pw.print(prefix);
                pw.print("resDir=");
                pw.println(this.appInfo.publicSourceDir);
            }
            pw.print(prefix);
            pw.print("dataDir=");
            pw.println(this.appInfo.dataDir);
            if (this.appInfo.splitSourceDirs != null) {
                pw.print(prefix);
                pw.print("splitDir=");
                pw.println(Arrays.toString(this.appInfo.splitSourceDirs));
            }
        }
        pw.print(prefix);
        pw.print("stateNotNeeded=");
        pw.print(this.stateNotNeeded);
        pw.print(" componentSpecified=");
        pw.print(this.componentSpecified);
        pw.print(" mActivityType=");
        pw.println(WindowConfiguration.activityTypeToString(getActivityType()));
        if (this.rootVoiceInteraction) {
            pw.print(prefix);
            pw.print("rootVoiceInteraction=");
            pw.println(this.rootVoiceInteraction);
        }
        pw.print(prefix);
        pw.print("compat=");
        pw.print(this.compat);
        pw.print(" labelRes=0x");
        pw.print(Integer.toHexString(this.labelRes));
        pw.print(" icon=0x");
        pw.print(Integer.toHexString(this.icon));
        pw.print(" theme=0x");
        pw.println(Integer.toHexString(this.theme));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mLastReportedConfigurations:");
        pw.println(stringBuilder.toString());
        MergedConfiguration mergedConfiguration = this.mLastReportedConfiguration;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(prefix);
        stringBuilder2.append(" ");
        mergedConfiguration.dump(pw, stringBuilder2.toString());
        pw.print(prefix);
        pw.print("CurrentConfiguration=");
        pw.println(getConfiguration());
        if (!getOverrideConfiguration().equals(Configuration.EMPTY)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("OverrideConfiguration=");
            stringBuilder.append(getOverrideConfiguration());
            pw.println(stringBuilder.toString());
        }
        if (!matchParentBounds()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("bounds=");
            stringBuilder.append(getBounds());
            pw.println(stringBuilder.toString());
        }
        if (!(this.resultTo == null && this.resultWho == null)) {
            pw.print(prefix);
            pw.print("resultTo=");
            pw.print(this.resultTo);
            pw.print(" resultWho=");
            pw.print(this.resultWho);
            pw.print(" resultCode=");
            pw.println(this.requestCode);
        }
        if (!(this.taskDescription == null || (this.taskDescription.getIconFilename() == null && this.taskDescription.getLabel() == null && this.taskDescription.getPrimaryColor() == 0))) {
            String stringBuilder3;
            pw.print(prefix);
            pw.print("taskDescription:");
            pw.print(" label=\"");
            pw.print(this.taskDescription.getLabel());
            pw.print("\"");
            pw.print(" icon=");
            if (this.taskDescription.getInMemoryIcon() != null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.taskDescription.getInMemoryIcon().getByteCount());
                stringBuilder2.append(" bytes");
                stringBuilder3 = stringBuilder2.toString();
            } else {
                stringBuilder3 = "null";
            }
            pw.print(stringBuilder3);
            pw.print(" iconResource=");
            pw.print(this.taskDescription.getIconResource());
            pw.print(" iconFilename=");
            pw.print(this.taskDescription.getIconFilename());
            pw.print(" primaryColor=");
            pw.println(Integer.toHexString(this.taskDescription.getPrimaryColor()));
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append(" backgroundColor=");
            pw.print(stringBuilder2.toString());
            pw.println(Integer.toHexString(this.taskDescription.getBackgroundColor()));
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append(" statusBarColor=");
            pw.print(stringBuilder2.toString());
            pw.println(Integer.toHexString(this.taskDescription.getStatusBarColor()));
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append(" navigationBarColor=");
            pw.print(stringBuilder2.toString());
            pw.println(Integer.toHexString(this.taskDescription.getNavigationBarColor()));
        }
        if (this.results != null) {
            pw.print(prefix);
            pw.print("results=");
            pw.println(this.results);
        }
        if (this.pendingResults != null && this.pendingResults.size() > 0) {
            pw.print(prefix);
            pw.println("Pending Results:");
            Iterator it = this.pendingResults.iterator();
            while (it.hasNext()) {
                WeakReference<PendingIntentRecord> wpir = (WeakReference) it.next();
                PendingIntentRecord pir = wpir != null ? (PendingIntentRecord) wpir.get() : null;
                pw.print(prefix);
                pw.print("  - ");
                if (pir == null) {
                    pw.println("null");
                } else {
                    pw.println(pir);
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append(prefix);
                    stringBuilder4.append("    ");
                    pir.dump(pw, stringBuilder4.toString());
                }
            }
        }
        if (this.newIntents != null && this.newIntents.size() > 0) {
            pw.print(prefix);
            pw.println("Pending New Intents:");
            for (int i = 0; i < this.newIntents.size(); i++) {
                Intent intent = (Intent) this.newIntents.get(i);
                pw.print(prefix);
                pw.print("  - ");
                if (intent == null) {
                    pw.println("null");
                } else {
                    pw.println(intent.toShortString(true, true, false, true));
                }
            }
        }
        if (this.pendingOptions != null) {
            pw.print(prefix);
            pw.print("pendingOptions=");
            pw.println(this.pendingOptions);
        }
        if (this.appTimeTracker != null) {
            this.appTimeTracker.dumpWithHeader(pw, prefix, false);
        }
        if (this.uriPermissions != null) {
            this.uriPermissions.dump(pw, prefix);
        }
        pw.print(prefix);
        pw.print("launchFailed=");
        pw.print(this.launchFailed);
        pw.print(" launchCount=");
        pw.print(this.launchCount);
        pw.print(" lastLaunchTime=");
        if (this.lastLaunchTime == 0) {
            pw.print("0");
        } else {
            TimeUtils.formatDuration(this.lastLaunchTime, now, pw);
        }
        pw.println();
        pw.print(prefix);
        pw.print("haveState=");
        pw.print(this.haveState);
        pw.print(" icicle=");
        pw.println(this.icicle);
        pw.print(prefix);
        pw.print("state=");
        pw.print(this.mState);
        pw.print(" stopped=");
        pw.print(this.stopped);
        pw.print(" delayedResume=");
        pw.print(this.delayedResume);
        pw.print(" finishing=");
        pw.println(this.finishing);
        pw.print(prefix);
        pw.print("keysPaused=");
        pw.print(this.keysPaused);
        pw.print(" inHistory=");
        pw.print(this.inHistory);
        pw.print(" visible=");
        pw.print(this.visible);
        pw.print(" sleeping=");
        pw.print(this.sleeping);
        pw.print(" idle=");
        pw.print(this.idle);
        pw.print(" mStartingWindowState=");
        pw.println(startingWindowStateToString(this.mStartingWindowState));
        pw.print(prefix);
        pw.print("fullscreen=");
        pw.print(this.fullscreen);
        pw.print(" noDisplay=");
        pw.print(this.noDisplay);
        pw.print(" immersive=");
        pw.print(this.immersive);
        pw.print(" launchMode=");
        pw.println(this.launchMode);
        pw.print(prefix);
        pw.print("frozenBeforeDestroy=");
        pw.print(this.frozenBeforeDestroy);
        pw.print(" forceNewConfig=");
        pw.println(this.forceNewConfig);
        pw.print(prefix);
        pw.print("mActivityType=");
        pw.println(WindowConfiguration.activityTypeToString(getActivityType()));
        if (this.requestedVrComponent != null) {
            pw.print(prefix);
            pw.print("requestedVrComponent=");
            pw.println(this.requestedVrComponent);
        }
        if (!(this.displayStartTime == 0 && this.startTime == 0)) {
            pw.print(prefix);
            pw.print("displayStartTime=");
            if (this.displayStartTime == 0) {
                pw.print("0");
            } else {
                TimeUtils.formatDuration(this.displayStartTime, now, pw);
            }
            pw.print(" startTime=");
            if (this.startTime == 0) {
                pw.print("0");
            } else {
                TimeUtils.formatDuration(this.startTime, now, pw);
            }
            pw.println();
        }
        boolean waitingVisible = this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.contains(this);
        if (this.lastVisibleTime != 0 || waitingVisible || this.nowVisible) {
            pw.print(prefix);
            pw.print("waitingVisible=");
            pw.print(waitingVisible);
            pw.print(" nowVisible=");
            pw.print(this.nowVisible);
            pw.print(" lastVisibleTime=");
            if (this.lastVisibleTime == 0) {
                pw.print("0");
            } else {
                TimeUtils.formatDuration(this.lastVisibleTime, now, pw);
            }
            pw.println();
        }
        if (this.mDeferHidingClient) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("mDeferHidingClient=");
            stringBuilder2.append(this.mDeferHidingClient);
            pw.println(stringBuilder2.toString());
        }
        if (this.deferRelaunchUntilPaused || this.configChangeFlags != 0) {
            pw.print(prefix);
            pw.print("deferRelaunchUntilPaused=");
            pw.print(this.deferRelaunchUntilPaused);
            pw.print(" configChangeFlags=");
            pw.println(Integer.toHexString(this.configChangeFlags));
        }
        if (this.connections != null) {
            pw.print(prefix);
            pw.print("connections=");
            pw.println(this.connections);
        }
        if (this.info != null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("resizeMode=");
            stringBuilder2.append(ActivityInfo.resizeModeToString(this.info.resizeMode));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("mLastReportedMultiWindowMode=");
            stringBuilder2.append(this.mLastReportedMultiWindowMode);
            stringBuilder2.append(" mLastReportedPictureInPictureMode=");
            stringBuilder2.append(this.mLastReportedPictureInPictureMode);
            pw.println(stringBuilder2.toString());
            if (this.info.supportsPictureInPicture()) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(prefix);
                stringBuilder2.append("supportsPictureInPicture=");
                stringBuilder2.append(this.info.supportsPictureInPicture());
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(prefix);
                stringBuilder2.append("supportsEnterPipOnTaskSwitch: ");
                stringBuilder2.append(this.supportsEnterPipOnTaskSwitch);
                pw.println(stringBuilder2.toString());
            }
            if (this.info.maxAspectRatio != 0.0f) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(prefix);
                stringBuilder2.append("maxAspectRatio=");
                stringBuilder2.append(this.info.maxAspectRatio);
                pw.println(stringBuilder2.toString());
            }
        }
    }

    void updateApplicationInfo(ApplicationInfo aInfo) {
        this.appInfo = aInfo;
        this.info.applicationInfo = aInfo;
    }

    private boolean crossesHorizontalSizeThreshold(int firstDp, int secondDp) {
        return crossesSizeThreshold(this.mHorizontalSizeConfigurations, firstDp, secondDp);
    }

    private boolean crossesVerticalSizeThreshold(int firstDp, int secondDp) {
        return crossesSizeThreshold(this.mVerticalSizeConfigurations, firstDp, secondDp);
    }

    private boolean crossesSmallestSizeThreshold(int firstDp, int secondDp) {
        return crossesSizeThreshold(this.mSmallestSizeConfigurations, firstDp, secondDp);
    }

    private static boolean crossesSizeThreshold(int[] thresholds, int firstDp, int secondDp) {
        if (thresholds == null) {
            return false;
        }
        for (int i = thresholds.length - 1; i >= 0; i--) {
            int threshold = thresholds[i];
            if ((firstDp < threshold && secondDp >= threshold) || (firstDp >= threshold && secondDp < threshold)) {
                return true;
            }
        }
        return false;
    }

    void setSizeConfigurations(int[] horizontalSizeConfiguration, int[] verticalSizeConfigurations, int[] smallestSizeConfigurations) {
        this.mHorizontalSizeConfigurations = horizontalSizeConfiguration;
        this.mVerticalSizeConfigurations = verticalSizeConfigurations;
        this.mSmallestSizeConfigurations = smallestSizeConfigurations;
    }

    private void scheduleActivityMovedToDisplay(int displayId, Configuration config) {
        String str;
        StringBuilder stringBuilder;
        if (this.app == null || this.app.thread == null) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Can't report activity moved to display - client not running, activityRecord=");
                stringBuilder.append(this);
                stringBuilder.append(", displayId=");
                stringBuilder.append(displayId);
                Slog.w(str, stringBuilder.toString());
            }
            return;
        }
        try {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Reporting activity moved to display, activityRecord=");
                stringBuilder.append(this);
                stringBuilder.append(", displayId=");
                stringBuilder.append(displayId);
                stringBuilder.append(", config=");
                stringBuilder.append(config);
                Slog.v(str, stringBuilder.toString());
            }
            this.service.getLifecycleManager().scheduleTransaction(this.app.thread, this.appToken, MoveToDisplayItem.obtain(displayId, config));
        } catch (RemoteException e) {
        }
    }

    private void scheduleConfigurationChanged(Configuration config) {
        String str;
        StringBuilder stringBuilder;
        if (this.app == null || this.app.thread == null) {
            if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Can't report activity configuration update - client not running, activityRecord=");
                stringBuilder.append(this);
                Slog.w(str, stringBuilder.toString());
            }
            return;
        }
        try {
            if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Sending new config to ");
                stringBuilder.append(this);
                stringBuilder.append(", config: ");
                stringBuilder.append(config);
                Slog.v(str, stringBuilder.toString());
            }
            this.service.getLifecycleManager().scheduleTransaction(this.app.thread, this.appToken, ActivityConfigurationChangeItem.obtain(config));
        } catch (RemoteException e) {
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0036, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void updateMultiWindowMode() {
        if (this.task != null && this.task.getStack() != null && this.app != null && this.app.thread != null && !this.task.getStack().deferScheduleMultiWindowModeChanged()) {
            boolean inMultiWindowMode = inMultiWindowMode();
            if (inMultiWindowMode != this.mLastReportedMultiWindowMode) {
                this.mLastReportedMultiWindowMode = inMultiWindowMode;
                scheduleMultiWindowModeChanged(getConfiguration());
            }
        }
    }

    void scheduleMultiWindowModeChanged(Configuration overrideConfig) {
        try {
            this.service.getLifecycleManager().scheduleTransaction(this.app.thread, this.appToken, MultiWindowModeChangeItem.obtain(this.mLastReportedMultiWindowMode, overrideConfig));
        } catch (Exception e) {
        }
    }

    void updatePictureInPictureMode(Rect targetStackBounds, boolean forceUpdate) {
        if (this.task != null && this.task.getStack() != null && this.app != null && this.app.thread != null) {
            boolean inPictureInPictureMode = inPinnedWindowingMode() && targetStackBounds != null;
            if (inPictureInPictureMode != this.mLastReportedPictureInPictureMode || forceUpdate) {
                this.mLastReportedPictureInPictureMode = inPictureInPictureMode;
                this.mLastReportedMultiWindowMode = inMultiWindowMode();
                Configuration newConfig = this.task.computeNewOverrideConfigurationForBounds(targetStackBounds, null);
                schedulePictureInPictureModeChanged(newConfig);
                scheduleMultiWindowModeChanged(newConfig);
            }
        }
    }

    private void schedulePictureInPictureModeChanged(Configuration overrideConfig) {
        try {
            this.service.getLifecycleManager().scheduleTransaction(this.app.thread, this.appToken, PipModeChangeItem.obtain(this.mLastReportedPictureInPictureMode, overrideConfig));
        } catch (Exception e) {
        }
    }

    protected int getChildCount() {
        return 0;
    }

    protected ConfigurationContainer getChildAt(int index) {
        return null;
    }

    protected ConfigurationContainer getParent() {
        return getTask();
    }

    TaskRecord getTask() {
        return this.task;
    }

    void setTask(TaskRecord task) {
        setTask(task, false);
    }

    void setTask(TaskRecord task, boolean reparenting) {
        if (task == null || task != getTask()) {
            ActivityStack oldStack = getStack();
            ActivityStack newStack = task != null ? task.getStack() : null;
            if (oldStack != newStack) {
                if (!(reparenting || oldStack == null)) {
                    oldStack.onActivityRemovedFromStack(this);
                }
                if (newStack != null) {
                    newStack.onActivityAddedToStack(this);
                }
            }
            this.task = task;
            if (!reparenting) {
                onParentChanged();
            }
        }
    }

    void setWillCloseOrEnterPip(boolean willCloseOrEnterPip) {
        getWindowContainerController().setWillCloseOrEnterPip(willCloseOrEnterPip);
    }

    public static ActivityRecord forToken(IBinder token) {
        return forTokenLocked(token);
    }

    static ActivityRecord forTokenLocked(IBinder token) {
        try {
            return Token.tokenToActivityRecordLocked((Token) token);
        } catch (ClassCastException e) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad activity token: ");
            stringBuilder.append(token);
            Slog.w(str, stringBuilder.toString(), e);
            return null;
        }
    }

    boolean isResolverActivity() {
        return ResolverActivity.class.getName().equals(this.realActivity.getClassName());
    }

    boolean isResolverOrChildActivity() {
        if (!PackageManagerService.PLATFORM_PACKAGE_NAME.equals(this.packageName)) {
            return false;
        }
        try {
            return ResolverActivity.class.isAssignableFrom(Object.class.getClassLoader().loadClass(this.realActivity.getClassName()));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public ActivityRecord(ActivityManagerService _service, ProcessRecord _caller, int _launchedFromPid, int _launchedFromUid, String _launchedFromPackage, Intent _intent, String _resolvedType, ActivityInfo aInfo, Configuration _configuration, ActivityRecord _resultTo, String _resultWho, int _reqCode, boolean _componentSpecified, boolean _rootVoiceInteraction, ActivityStackSupervisor supervisor, ActivityOptions options, ActivityRecord sourceRecord) {
        ProcessRecord processRecord = _caller;
        Intent intent = _intent;
        ActivityInfo activityInfo = aInfo;
        ActivityOptions activityOptions = options;
        this.service = _service;
        this.appToken = new Token(this, intent);
        this.info = activityInfo;
        this.launchedFromPid = _launchedFromPid;
        int i = _launchedFromUid;
        this.launchedFromUid = i;
        this.launchedFromPackage = _launchedFromPackage;
        this.userId = UserHandle.getUserId(activityInfo.applicationInfo.uid);
        this.intent = intent;
        this.shortComponentName = _intent.getComponent().flattenToShortString();
        this.resolvedType = _resolvedType;
        boolean z = _componentSpecified;
        this.componentSpecified = z;
        this.rootVoiceInteraction = _rootVoiceInteraction;
        this.mLastReportedConfiguration = new MergedConfiguration(_configuration);
        this.resultTo = _resultTo;
        this.resultWho = _resultWho;
        this.requestCode = _reqCode;
        setState(ActivityState.INITIALIZING, "ActivityRecord ctor");
        this.frontOfTask = false;
        this.launchFailed = false;
        this.stopped = false;
        this.delayedResume = false;
        this.finishing = false;
        this.deferRelaunchUntilPaused = false;
        this.keysPaused = false;
        this.inHistory = false;
        this.visible = false;
        this.nowVisible = false;
        this.idle = false;
        this.hasBeenLaunched = false;
        this.mStackSupervisor = supervisor;
        this.haveState = true;
        initSplitMode(intent);
        if (activityInfo.targetActivity == null || (activityInfo.targetActivity.equals(_intent.getComponent().getClassName()) && (activityInfo.launchMode == 0 || activityInfo.launchMode == 1))) {
            this.realActivity = _intent.getComponent();
        } else {
            this.realActivity = new ComponentName(activityInfo.packageName, activityInfo.targetActivity);
        }
        if (isSplitMode()) {
            if (activityInfo.taskAffinity == null || activityInfo.taskAffinity.equals(activityInfo.processName)) {
                this.taskAffinity = processRecord.processName;
            } else {
                this.taskAffinity = activityInfo.taskAffinity;
            }
            if (activityInfo.launchMode == 2) {
                activityInfo.launchMode = 0;
            }
        } else {
            this.taskAffinity = activityInfo.taskAffinity;
        }
        this.stateNotNeeded = (activityInfo.flags & 16) != 0;
        this.appInfo = activityInfo.applicationInfo;
        this.nonLocalizedLabel = activityInfo.nonLocalizedLabel;
        this.labelRes = activityInfo.labelRes;
        if (this.nonLocalizedLabel == null && this.labelRes == 0) {
            ApplicationInfo app = activityInfo.applicationInfo;
            this.nonLocalizedLabel = app.nonLocalizedLabel;
            this.labelRes = app.labelRes;
        }
        this.icon = aInfo.getIconResource();
        this.logo = aInfo.getLogoResource();
        this.theme = aInfo.getThemeResource();
        this.realTheme = this.theme;
        if (this.realTheme == 0) {
            this.realTheme = activityInfo.applicationInfo.targetSdkVersion < 11 ? 16973829 : 16973931;
        }
        if ((activityInfo.flags & 512) != 0) {
            this.windowFlags |= DumpState.DUMP_SERVICE_PERMISSIONS;
        }
        if ((activityInfo.flags & 1) == 0 || processRecord == null || !(activityInfo.applicationInfo.uid == 1000 || activityInfo.applicationInfo.uid == processRecord.info.uid)) {
            this.processName = activityInfo.processName;
        } else {
            this.processName = processRecord.processName;
        }
        if ((activityInfo.flags & 32) != 0) {
            this.intent.addFlags(DumpState.DUMP_VOLUMES);
        }
        this.packageName = activityInfo.applicationInfo.packageName;
        this.launchMode = activityInfo.launchMode;
        Entry ent = AttributeCache.instance().get(this.packageName, this.realTheme, R.styleable.Window, this.userId);
        if (ent != null) {
            boolean z2 = !ActivityInfo.isTranslucentOrFloating(ent.array) || isForceRotationMode(this.packageName, intent);
            this.fullscreen = z2;
            this.hasWallpaper = ent.array.getBoolean(14, false);
            this.noDisplay = ent.array.getBoolean(10, false);
            this.mIsFloating = ent.array.getBoolean(4, false);
            this.mIsTransluent = ent.array.getBoolean(5, false);
        } else {
            this.hasWallpaper = false;
            this.noDisplay = false;
        }
        if (isSplitMode()) {
            if (this.fullscreen) {
                this.fullscreen = false;
            } else {
                intent.addHwFlags(8);
            }
        }
        setActivityType(z, i, intent, activityOptions, sourceRecord);
        this.immersive = (activityInfo.flags & 2048) != 0;
        this.requestedVrComponent = activityInfo.requestedVrComponent == null ? null : ComponentName.unflattenFromString(activityInfo.requestedVrComponent);
        this.mShowWhenLocked = (activityInfo.flags & DumpState.DUMP_VOLUMES) != 0;
        this.mTurnScreenOn = (activityInfo.flags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0;
        this.mRotationAnimationHint = activityInfo.rotationAnimation;
        this.lockTaskLaunchMode = activityInfo.lockTaskLaunchMode;
        if (this.appInfo.isPrivilegedApp() && (this.lockTaskLaunchMode == 2 || this.lockTaskLaunchMode == 1)) {
            this.lockTaskLaunchMode = 0;
        }
        if (activityOptions != null) {
            this.pendingOptions = activityOptions;
            this.mLaunchTaskBehind = options.getLaunchTaskBehind();
            int rotationAnimation = this.pendingOptions.getRotationAnimationHint();
            if (rotationAnimation >= 0) {
                this.mRotationAnimationHint = rotationAnimation;
            }
            PendingIntent usageReport = this.pendingOptions.getUsageTimeReport();
            if (usageReport != null) {
                this.appTimeTracker = new AppTimeTracker(usageReport);
            }
            if (this.pendingOptions.getLockTaskMode() && this.lockTaskLaunchMode == 0) {
                this.lockTaskLaunchMode = 3;
            }
        }
    }

    void setProcess(ProcessRecord proc) {
        this.app = proc;
        if ((this.task != null ? this.task.getRootActivity() : null) == this) {
            this.task.setRootProcess(proc);
        }
    }

    AppWindowContainerController getWindowContainerController() {
        return this.mWindowContainerController;
    }

    void createWindowContainer(boolean naviBarHide) {
        if (this.mWindowContainerController == null) {
            this.inHistory = true;
            TaskWindowContainerController taskController = this.task.getWindowContainerController();
            this.task.updateOverrideConfigurationFromLaunchBounds();
            updateOverrideConfiguration();
            AppWindowContainerController appWindowContainerController = r0;
            AppWindowContainerController appWindowContainerController2 = new AppWindowContainerController(taskController, this.appToken, this, HwBootFail.STAGE_BOOT_SUCCESS, this.info.screenOrientation, this.fullscreen, (this.info.flags & 1024) != 0, this.info.configChanges, this.task.voiceSession != null, this.mLaunchTaskBehind, isAlwaysFocusable(), this.appInfo.targetSdkVersion, this.mRotationAnimationHint, 1000000 * ActivityManagerService.getInputDispatchingTimeoutLocked(this), naviBarHide, this.info);
            this.mWindowContainerController = appWindowContainerController;
            this.task.addActivityToTop(this);
            this.mLastReportedMultiWindowMode = inMultiWindowMode();
            this.mLastReportedPictureInPictureMode = inPinnedWindowingMode();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Window container=");
        stringBuilder.append(this.mWindowContainerController);
        stringBuilder.append(" already created for r=");
        stringBuilder.append(this);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    void removeWindowContainer() {
        if (this.mWindowContainerController != null) {
            resumeKeyDispatchingLocked();
            this.mWindowContainerController.removeContainer(getDisplayId());
            this.mWindowContainerController = null;
        }
    }

    void reparent(TaskRecord newTask, int position, String reason) {
        TaskRecord prevTask = this.task;
        StringBuilder stringBuilder;
        if (prevTask == newTask) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(reason);
            stringBuilder.append(": task=");
            stringBuilder.append(newTask);
            stringBuilder.append(" is already the parent of r=");
            stringBuilder.append(this);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (prevTask == null || newTask == null || prevTask.getStack() == newTask.getStack()) {
            if (this.mWindowContainerController != null) {
                this.mWindowContainerController.reparent(newTask.getWindowContainerController(), position);
            }
            ActivityStack prevStack = prevTask.getStack();
            if (prevStack != newTask.getStack()) {
                prevStack.onActivityRemovedFromStack(this);
            }
            prevTask.removeActivity(this, true);
            newTask.addActivityAtIndex(position, this);
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(reason);
            stringBuilder.append(": task=");
            stringBuilder.append(newTask);
            stringBuilder.append(" is in a different stack (");
            stringBuilder.append(newTask.getStackId());
            stringBuilder.append(") than the parent of r=");
            stringBuilder.append(this);
            stringBuilder.append(" (");
            stringBuilder.append(prevTask.getStackId());
            stringBuilder.append(")");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private boolean isHomeIntent(Intent intent) {
        if ("android.intent.action.MAIN".equals(intent.getAction()) && intent.hasCategory("android.intent.category.HOME") && intent.getCategories().size() == 1 && intent.getData() == null && intent.getType() == null) {
            return true;
        }
        return false;
    }

    static boolean isMainIntent(Intent intent) {
        if ("android.intent.action.MAIN".equals(intent.getAction()) && intent.hasCategory("android.intent.category.LAUNCHER") && intent.getCategories().size() == 1 && intent.getData() == null && intent.getType() == null) {
            return true;
        }
        return false;
    }

    private boolean canLaunchHomeActivity(int uid, ActivityRecord sourceRecord) {
        boolean z = true;
        if (uid == Process.myUid() || uid == 0) {
            return true;
        }
        RecentTasks recentTasks = this.mStackSupervisor.mService.getRecentTasks();
        if (recentTasks != null && recentTasks.isCallerRecents(uid)) {
            return true;
        }
        if (sourceRecord == null || !sourceRecord.isResolverActivity()) {
            z = false;
        }
        return z;
    }

    private boolean canLaunchAssistActivity(String packageName) {
        ComponentName assistComponent = this.service.mActiveVoiceInteractionServiceComponent;
        if (assistComponent != null) {
            return assistComponent.getPackageName().equals(packageName);
        }
        return false;
    }

    private void setActivityType(boolean componentSpecified, int launchedFromUid, Intent intent, ActivityOptions options, ActivityRecord sourceRecord) {
        int activityType = 0;
        if ((!componentSpecified || canLaunchHomeActivity(launchedFromUid, sourceRecord)) && isHomeIntent(intent) && !isResolverActivity()) {
            activityType = 2;
            if (this.info.resizeMode == 4 || this.info.resizeMode == 1) {
                this.info.resizeMode = 0;
            }
        } else if (this.realActivity.getClassName().contains(LEGACY_RECENTS_PACKAGE_NAME) || this.service.getRecentTasks().isRecentsComponent(this.realActivity, this.appInfo.uid) || this.realActivity.getClassName().contains(LEGACY_RECENTS_PACKAGE_NAME_LAUNCHER)) {
            activityType = 3;
        } else if (options != null && options.getLaunchActivityType() == 4 && canLaunchAssistActivity(this.launchedFromPackage)) {
            activityType = 4;
        }
        setActivityType(activityType);
    }

    void setTaskToAffiliateWith(TaskRecord taskToAffiliateWith) {
        if (this.launchMode != 3 && this.launchMode != 2) {
            this.task.setTaskToAffiliateWith(taskToAffiliateWith);
        }
    }

    <T extends ActivityStack> T getStack() {
        return this.task != null ? this.task.getStack() : null;
    }

    int getStackId() {
        return getStack() != null ? getStack().mStackId : -1;
    }

    ActivityDisplay getDisplay() {
        ActivityStack stack = getStack();
        return stack != null ? stack.getDisplay() : null;
    }

    boolean changeWindowTranslucency(boolean toOpaque) {
        if (this.fullscreen == toOpaque) {
            return false;
        }
        TaskRecord taskRecord = this.task;
        taskRecord.numFullscreen += toOpaque ? 1 : -1;
        this.fullscreen = toOpaque;
        return true;
    }

    void takeFromHistory() {
        if (this.inHistory) {
            this.inHistory = false;
            if (!(this.task == null || this.finishing)) {
                this.task = null;
            }
            clearOptionsLocked();
        }
    }

    boolean isInHistory() {
        return this.inHistory;
    }

    boolean isInStackLocked() {
        ActivityStack stack = getStack();
        return (stack == null || stack.isInStackLocked(this) == null) ? false : true;
    }

    boolean isPersistable() {
        return (this.info.persistableMode == 0 || this.info.persistableMode == 2) && (this.intent == null || (this.intent.getFlags() & DumpState.DUMP_VOLUMES) == 0);
    }

    boolean isFocusable() {
        return this.mStackSupervisor.isFocusable(this, isAlwaysFocusable());
    }

    boolean isResizeable() {
        return ActivityInfo.isResizeableMode(this.info.resizeMode) || this.info.supportsPictureInPicture();
    }

    boolean isNonResizableOrForcedResizable() {
        return (this.info.resizeMode == 2 || this.info.resizeMode == 1) ? false : true;
    }

    boolean supportsPictureInPicture() {
        return this.service.mSupportsPictureInPicture && isActivityTypeStandardOrUndefined() && this.info.supportsPictureInPicture();
    }

    public boolean supportsSplitScreenWindowingMode() {
        return super.supportsSplitScreenWindowingMode() && this.service.mSupportsSplitScreenMultiWindow && supportsResizeableMultiWindow();
    }

    boolean supportsFreeform() {
        return this.service.mSupportsFreeformWindowManagement && supportsResizeableMultiWindow();
    }

    private boolean supportsResizeableMultiWindow() {
        return this.service.mSupportsMultiWindow && !isActivityTypeHome() && (ActivityInfo.isResizeableMode(this.info.resizeMode) || this.service.mForceResizableActivities);
    }

    boolean canBeLaunchedOnDisplay(int displayId) {
        TaskRecord task = getTask();
        boolean resizeable = task != null ? task.isResizeable() : supportsResizeableMultiWindow();
        if (!resizeable) {
            boolean z = (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayId)) || (HwVRUtils.isVRMode() && HwVRUtils.isValidVRDisplayId(displayId));
            resizeable = z;
        }
        return this.service.mStackSupervisor.canPlaceEntityOnDisplay(displayId, resizeable, this.launchedFromPid, this.launchedFromUid, this.info);
    }

    boolean checkEnterPictureInPictureState(String caller, boolean beforeStopping) {
        boolean z = false;
        if (!supportsPictureInPicture() || !checkEnterPictureInPictureAppOpsState() || this.service.shouldDisableNonVrUiLocked()) {
            return false;
        }
        boolean isKeyguardLocked = this.service.isKeyguardLocked();
        boolean isCurrentAppLocked = this.service.getLockTaskModeState() != 0;
        ActivityDisplay display = getDisplay();
        boolean hasPinnedStack = display != null && display.hasPinnedStack();
        boolean isNotLockedOrOnKeyguard = (isKeyguardLocked || isCurrentAppLocked) ? false : true;
        if (beforeStopping && hasPinnedStack) {
            return false;
        }
        switch (this.mState) {
            case RESUMED:
                if (!isCurrentAppLocked && (this.supportsEnterPipOnTaskSwitch || !beforeStopping)) {
                    z = true;
                }
                return z;
            case PAUSING:
            case PAUSED:
                if (isNotLockedOrOnKeyguard && !hasPinnedStack && this.supportsEnterPipOnTaskSwitch) {
                    z = true;
                }
                return z;
            case STOPPING:
                if (this.supportsEnterPipOnTaskSwitch) {
                    if (isNotLockedOrOnKeyguard && !hasPinnedStack) {
                        z = true;
                    }
                    return z;
                }
                break;
        }
        return false;
    }

    private boolean checkEnterPictureInPictureAppOpsState() {
        boolean z = false;
        try {
            if (this.service.getAppOpsService().checkOperation(67, this.appInfo.uid, this.packageName) == 0) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            return false;
        }
    }

    boolean isAlwaysFocusable() {
        return (this.info.flags & 262144) != 0;
    }

    boolean hasDismissKeyguardWindows() {
        return this.service.mWindowManager.containsDismissKeyguardWindow(this.appToken);
    }

    void makeFinishingLocked() {
        if (!this.finishing) {
            this.finishing = true;
            if (this.stopped) {
                clearOptionsLocked();
            }
            if (this.service != null) {
                this.service.mTaskChangeNotificationController.notifyTaskStackChanged();
            }
        }
    }

    UriPermissionOwner getUriPermissionsLocked() {
        if (this.uriPermissions == null) {
            this.uriPermissions = new UriPermissionOwner(this.service, this);
        }
        return this.uriPermissions;
    }

    void addResultLocked(ActivityRecord from, String resultWho, int requestCode, int resultCode, Intent resultData) {
        ActivityResult r = new ActivityResult(from, resultWho, requestCode, resultCode, resultData);
        if (this.results == null) {
            this.results = new ArrayList();
        }
        this.results.add(r);
    }

    void removeResultsLocked(ActivityRecord from, String resultWho, int requestCode) {
        if (this.results != null) {
            for (int i = this.results.size() - 1; i >= 0; i--) {
                ActivityResult r = (ActivityResult) this.results.get(i);
                if (r.mFrom == from) {
                    if (r.mResultWho == null) {
                        if (resultWho != null) {
                        }
                    } else if (!r.mResultWho.equals(resultWho)) {
                    }
                    if (r.mRequestCode == requestCode) {
                        this.results.remove(i);
                    }
                }
            }
        }
    }

    private void addNewIntentLocked(ReferrerIntent intent) {
        if (this.newIntents == null) {
            this.newIntents = new ArrayList();
        }
        this.newIntents.add(intent);
    }

    final boolean isSleeping() {
        ActivityStack stack = getStack();
        return stack != null ? stack.shouldSleepActivities() : this.service.isSleepingLocked();
    }

    final void deliverNewIntentLocked(int callingUid, Intent intent, String referrer) {
        String str;
        StringBuilder stringBuilder;
        this.service.grantUriPermissionFromIntentLocked(callingUid, this.packageName, intent, getUriPermissionsLocked(), this.userId);
        ReferrerIntent rintent = new ReferrerIntent(intent, referrer);
        boolean unsent = true;
        boolean z = false;
        boolean isTopActivityWhileSleeping = isTopRunningActivity() && isSleeping();
        if (!((this.mState != ActivityState.RESUMED && this.mState != ActivityState.PAUSED && !isTopActivityWhileSleeping) || this.app == null || this.app.thread == null)) {
            try {
                ArrayList<ReferrerIntent> ar = new ArrayList(1);
                ar.add(rintent);
                ClientLifecycleManager lifecycleManager = this.service.getLifecycleManager();
                IApplicationThread iApplicationThread = this.app.thread;
                IBinder iBinder = this.appToken;
                if (this.mState == ActivityState.PAUSED) {
                    z = true;
                }
                lifecycleManager.scheduleTransaction(iApplicationThread, iBinder, NewIntentItem.obtain(ar, z));
                unsent = false;
            } catch (RemoteException e) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception thrown sending new intent to ");
                stringBuilder.append(this);
                Slog.w(str, stringBuilder.toString(), e);
            } catch (NullPointerException e2) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception thrown sending new intent to ");
                stringBuilder.append(this);
                Slog.w(str, stringBuilder.toString(), e2);
            }
        }
        if (unsent) {
            addNewIntentLocked(rintent);
        }
    }

    void updateOptionsLocked(ActivityOptions options) {
        if (options != null) {
            if (this.pendingOptions != null) {
                this.pendingOptions.abort();
            }
            this.pendingOptions = options;
        }
    }

    void applyOptionsLocked() {
        if (this.pendingOptions != null && this.pendingOptions.getAnimationType() != 5) {
            int animationType = this.pendingOptions.getAnimationType();
            boolean z = true;
            switch (animationType) {
                case 1:
                    this.service.mWindowManager.overridePendingAppTransition(this.pendingOptions.getPackageName(), this.pendingOptions.getCustomEnterResId(), this.pendingOptions.getCustomExitResId(), this.pendingOptions.getOnAnimationStartListener());
                    break;
                case 2:
                    this.service.mWindowManager.overridePendingAppTransitionScaleUp(this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getWidth(), this.pendingOptions.getHeight());
                    if (this.intent.getSourceBounds() == null) {
                        this.intent.setSourceBounds(new Rect(this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getStartX() + this.pendingOptions.getWidth(), this.pendingOptions.getStartY() + this.pendingOptions.getHeight()));
                        break;
                    }
                    break;
                case 3:
                case 4:
                    boolean scaleUp = animationType == 3;
                    GraphicBuffer buffer = this.pendingOptions.getThumbnail();
                    this.service.mWindowManager.overridePendingAppTransitionThumb(buffer, this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getOnAnimationStartListener(), scaleUp);
                    if (this.intent.getSourceBounds() == null && buffer != null) {
                        this.intent.setSourceBounds(new Rect(this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getStartX() + buffer.getWidth(), this.pendingOptions.getStartY() + buffer.getHeight()));
                        break;
                    }
                case 8:
                case 9:
                    AppTransitionAnimationSpec[] specs = this.pendingOptions.getAnimSpecs();
                    IAppTransitionAnimationSpecsFuture specsFuture = this.pendingOptions.getSpecsFuture();
                    if (specsFuture == null) {
                        if (animationType == 9 && specs != null) {
                            this.service.mWindowManager.overridePendingAppTransitionMultiThumb(specs, this.pendingOptions.getOnAnimationStartListener(), this.pendingOptions.getAnimationFinishedListener(), false);
                            break;
                        }
                        this.service.mWindowManager.overridePendingAppTransitionAspectScaledThumb(this.pendingOptions.getThumbnail(), this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getWidth(), this.pendingOptions.getHeight(), this.pendingOptions.getOnAnimationStartListener(), animationType == 8);
                        if (this.intent.getSourceBounds() == null) {
                            this.intent.setSourceBounds(new Rect(this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getStartX() + this.pendingOptions.getWidth(), this.pendingOptions.getStartY() + this.pendingOptions.getHeight()));
                            break;
                        }
                    }
                    WindowManagerService windowManagerService = this.service.mWindowManager;
                    IRemoteCallback onAnimationStartListener = this.pendingOptions.getOnAnimationStartListener();
                    if (animationType != 8) {
                        z = false;
                    }
                    windowManagerService.overridePendingAppTransitionMultiThumbFuture(specsFuture, onAnimationStartListener, z);
                    break;
                    break;
                case 11:
                    this.service.mWindowManager.overridePendingAppTransitionClipReveal(this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getWidth(), this.pendingOptions.getHeight());
                    if (this.intent.getSourceBounds() == null) {
                        this.intent.setSourceBounds(new Rect(this.pendingOptions.getStartX(), this.pendingOptions.getStartY(), this.pendingOptions.getStartX() + this.pendingOptions.getWidth(), this.pendingOptions.getStartY() + this.pendingOptions.getHeight()));
                        break;
                    }
                    break;
                case 12:
                    this.service.mWindowManager.overridePendingAppTransitionStartCrossProfileApps();
                    break;
                case 13:
                    this.service.mWindowManager.overridePendingAppTransitionRemote(this.pendingOptions.getRemoteAnimationAdapter());
                    break;
                default:
                    String str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("applyOptionsLocked: Unknown animationType=");
                    stringBuilder.append(animationType);
                    Slog.e(str, stringBuilder.toString());
                    break;
            }
            if (this.task == null) {
                clearOptionsLocked(false);
            } else {
                this.task.clearAllPendingOptions();
            }
        }
    }

    ActivityOptions getOptionsForTargetActivityLocked() {
        return this.pendingOptions != null ? this.pendingOptions.forTargetActivity() : null;
    }

    void clearOptionsLocked() {
        clearOptionsLocked(true);
    }

    void clearOptionsLocked(boolean withAbort) {
        if (withAbort && this.pendingOptions != null) {
            this.pendingOptions.abort();
        }
        this.pendingOptions = null;
    }

    ActivityOptions takeOptionsLocked() {
        ActivityOptions opts = this.pendingOptions;
        this.pendingOptions = null;
        return opts;
    }

    void removeUriPermissionsLocked() {
        if (this.uriPermissions != null) {
            this.uriPermissions.removeUriPermissionsLocked();
            this.uriPermissions = null;
        }
    }

    void pauseKeyDispatchingLocked() {
        if (!this.keysPaused) {
            this.keysPaused = true;
            if (this.mWindowContainerController != null) {
                this.mWindowContainerController.pauseKeyDispatching();
            }
        }
    }

    void resumeKeyDispatchingLocked() {
        if (this.keysPaused) {
            this.keysPaused = false;
            if (this.mWindowContainerController != null) {
                this.mWindowContainerController.resumeKeyDispatching();
            }
        }
    }

    private void updateTaskDescription(CharSequence description) {
        this.task.lastDescription = description;
    }

    void setDeferHidingClient(boolean deferHidingClient) {
        if (this.mDeferHidingClient != deferHidingClient) {
            this.mDeferHidingClient = deferHidingClient;
            if (!(this.mDeferHidingClient || this.visible)) {
                setVisibility(false);
            }
        }
    }

    void setVisibility(boolean visible) {
        if (this.mWindowContainerController != null) {
            this.mWindowContainerController.setVisibility(visible, this.mDeferHidingClient);
            this.mStackSupervisor.getActivityMetricsLogger().notifyVisibilityChanged(this);
        }
    }

    void setVisible(boolean newVisible) {
        this.visible = newVisible;
        boolean z = !this.visible && this.mDeferHidingClient;
        this.mDeferHidingClient = z;
        setVisibility(this.visible);
        this.mStackSupervisor.mAppVisibilitiesChangedSinceLastPause = true;
    }

    void setState(ActivityState state, String reason) {
        String str;
        StringBuilder stringBuilder;
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("State movement: ");
            stringBuilder.append(this);
            stringBuilder.append(" from:");
            stringBuilder.append(getState());
            stringBuilder.append(" to:");
            stringBuilder.append(state);
            stringBuilder.append(" reason:");
            stringBuilder.append(reason);
            Slog.v(str, stringBuilder.toString());
        }
        if (state == this.mState) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("State unchanged from:");
                stringBuilder.append(state);
                Slog.v(str, stringBuilder.toString());
            }
            return;
        }
        this.mState = state;
        TaskRecord parent = getTask();
        if (parent != null) {
            parent.onActivityStateChanged(this, state, reason);
        }
        if (state == ActivityState.STOPPING && !isSleeping()) {
            this.mWindowContainerController.notifyAppStopping();
        }
    }

    ActivityState getState() {
        return this.mState;
    }

    boolean isState(ActivityState state) {
        return state == this.mState;
    }

    boolean isState(ActivityState state1, ActivityState state2) {
        return state1 == this.mState || state2 == this.mState;
    }

    boolean isState(ActivityState state1, ActivityState state2, ActivityState state3) {
        return state1 == this.mState || state2 == this.mState || state3 == this.mState;
    }

    boolean isState(ActivityState state1, ActivityState state2, ActivityState state3, ActivityState state4) {
        return state1 == this.mState || state2 == this.mState || state3 == this.mState || state4 == this.mState;
    }

    void notifyAppResumed(boolean wasStopped) {
        this.mWindowContainerController.notifyAppResumed(wasStopped);
    }

    void notifyUnknownVisibilityLaunched() {
        if (!this.noDisplay) {
            this.mWindowContainerController.notifyUnknownVisibilityLaunched();
        }
    }

    boolean shouldBeVisibleIgnoringKeyguard(boolean behindFullscreenActivity) {
        boolean z = false;
        if (!okToShowLocked()) {
            return false;
        }
        if (!behindFullscreenActivity || this.mLaunchTaskBehind) {
            z = true;
        }
        return z;
    }

    void makeVisibleIfNeeded(ActivityRecord starting, boolean reportToClient) {
        String str;
        StringBuilder stringBuilder;
        if (this.mState == ActivityState.RESUMED || this == starting) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                str = TAG_VISIBILITY;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Not making visible, r=");
                stringBuilder.append(this);
                stringBuilder.append(" state=");
                stringBuilder.append(this.mState);
                stringBuilder.append(" starting=");
                stringBuilder.append(starting);
                Slog.d(str, stringBuilder.toString());
            }
            return;
        }
        str = TAG_VISIBILITY;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Making visible and scheduling visibility: ");
        stringBuilder.append(this);
        Slog.v(str, stringBuilder.toString());
        ActivityStack stack = getStack();
        try {
            if (stack.mTranslucentActivityWaiting != null) {
                updateOptionsLocked(this.returningOptions);
                stack.mUndrawnActivitiesBelowTopTranslucent.add(this);
            }
            setVisible(true);
            this.sleeping = false;
            this.app.pendingUiClean = true;
            if (reportToClient) {
                makeClientVisible();
            } else {
                this.mClientVisibilityDeferred = true;
            }
            this.mStackSupervisor.mStoppingActivities.remove(this);
            this.mStackSupervisor.mGoingToSleepActivities.remove(this);
        } catch (Exception e) {
            String str2 = ActivityManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Exception thrown making visible: ");
            stringBuilder2.append(this.intent.getComponent());
            Slog.w(str2, stringBuilder2.toString(), e);
        }
        handleAlreadyVisible();
    }

    void makeClientVisible() {
        this.mClientVisibilityDeferred = false;
        try {
            this.service.getLifecycleManager().scheduleTransaction(this.app.thread, this.appToken, WindowVisibilityItem.obtain(true));
            if (shouldPauseWhenBecomingVisible()) {
                setState(ActivityState.PAUSING, "makeVisibleIfNeeded");
                this.service.getLifecycleManager().scheduleTransaction(this.app.thread, this.appToken, PauseActivityItem.obtain(this.finishing, false, this.configChangeFlags, false));
            }
        } catch (Exception e) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception thrown sending visibility update: ");
            stringBuilder.append(this.intent.getComponent());
            Slog.w(str, stringBuilder.toString(), e);
        }
    }

    private boolean shouldPauseWhenBecomingVisible() {
        if (!isState(ActivityState.STOPPED, ActivityState.STOPPING) || getStack().mTranslucentActivityWaiting != null || this.mStackSupervisor.getResumedActivityLocked() == this) {
            return false;
        }
        int positionInTask = this.task.mActivities.indexOf(this);
        if (positionInTask == -1) {
            throw new IllegalStateException("Activity not found in its task");
        } else if (positionInTask == this.task.mActivities.size() - 1) {
            return true;
        } else {
            if (((ActivityRecord) this.task.mActivities.get(positionInTask + 1)).finishing && this.results == null) {
                return true;
            }
            return false;
        }
    }

    boolean handleAlreadyVisible() {
        stopFreezingScreenLocked(false);
        try {
            if (this.returningOptions != null) {
                this.app.thread.scheduleOnNewActivityOptions(this.appToken, this.returningOptions.toBundle());
            }
        } catch (RemoteException e) {
        }
        if (this.mState == ActivityState.RESUMED) {
            return true;
        }
        return false;
    }

    static void activityResumedLocked(IBinder token) {
        ActivityRecord r = forTokenLocked(token);
        if (ActivityManagerDebugConfig.DEBUG_SAVED_STATE) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Resumed activity; dropping state of: ");
            stringBuilder.append(r);
            Slog.i(str, stringBuilder.toString());
        }
        if (r != null) {
            r.icicle = null;
            r.haveState = false;
        }
    }

    void completeResumeLocked() {
        boolean wasVisible = this.visible;
        setVisible(true);
        if (!wasVisible) {
            this.mStackSupervisor.mAppVisibilitiesChangedSinceLastPause = true;
        }
        this.idle = false;
        this.results = null;
        this.newIntents = null;
        this.stopped = false;
        if (isActivityTypeHome()) {
            ProcessRecord app = ((ActivityRecord) this.task.mActivities.get(0)).app;
            if (!(app == null || app == this.service.mHomeProcess)) {
                this.service.mHomeProcess = app;
                this.service.mHwAMSEx.reportHomeProcess(this.service.mHomeProcess);
            }
        }
        if (this.nowVisible) {
            this.mStackSupervisor.reportActivityVisibleLocked(this);
        }
        this.mStackSupervisor.scheduleIdleTimeoutLocked(this);
        this.mStackSupervisor.reportResumedActivityLocked(this);
        this.service.setFocusedActivityLockedForNavi(this);
        resumeKeyDispatchingLocked();
        ActivityStack stack = getStack();
        this.mStackSupervisor.mNoAnimActivities.clear();
        HwAudioServiceManager.setSoundEffectState(false, this.packageName, true, null);
        if (this.app != null) {
            this.cpuTimeAtResume = this.service.mProcessCpuTracker.getCpuTimeForPid(this.app.pid);
        } else {
            this.cpuTimeAtResume = 0;
        }
        this.returningOptions = null;
        if (canTurnScreenOn()) {
            this.mStackSupervisor.wakeUp("turnScreenOnFlag");
        } else {
            stack.checkReadyForSleep();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Complete resume: ");
        stringBuilder.append(this);
        stringBuilder.append(", launchTrack: ");
        stringBuilder.append(this.mStackSupervisor.mActivityLaunchTrack);
        Flog.i(101, stringBuilder.toString());
        this.mStackSupervisor.mActivityLaunchTrack = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    }

    final void activityStoppedLocked(Bundle newIcicle, PersistableBundle newPersistentState, CharSequence description) {
        ActivityStack stack = getStack();
        String str;
        if (this.mState != ActivityState.STOPPING) {
            str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Activity reported stop, but no longer stopping: ");
            stringBuilder.append(this);
            Slog.i(str, stringBuilder.toString());
            stack.mHandler.removeMessages(104, this);
            return;
        }
        StringBuilder stringBuilder2;
        if (newPersistentState != null) {
            this.persistentState = newPersistentState;
            this.service.notifyTaskPersisterLocked(this.task, false);
        }
        if (ActivityManagerDebugConfig.DEBUG_SAVED_STATE) {
            String str2 = ActivityManagerService.TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Saving icicle of ");
            stringBuilder2.append(this);
            stringBuilder2.append(": ");
            stringBuilder2.append(this.icicle);
            Slog.i(str2, stringBuilder2.toString());
        }
        if (newIcicle != null) {
            this.icicle = newIcicle;
            this.haveState = true;
            this.launchCount = 0;
            updateTaskDescription(description);
        }
        if (!this.stopped) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                str = ActivityManagerService.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Moving to STOPPED: ");
                stringBuilder2.append(this);
                stringBuilder2.append(" (stop complete)");
                Slog.v(str, stringBuilder2.toString());
            }
            stack.mHandler.removeMessages(104, this);
            this.stopped = true;
            setState(ActivityState.STOPPED, "activityStoppedLocked");
            this.mWindowContainerController.notifyAppStopped();
            if (this.finishing) {
                clearOptionsLocked();
            } else if (this.deferRelaunchUntilPaused) {
                stack.destroyActivityLocked(this, true, "stop-config");
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            } else {
                this.mStackSupervisor.updatePreviousProcessLocked(this);
            }
        }
    }

    void startLaunchTickingLocked() {
        if (!Build.IS_USER && this.launchTickTime == 0) {
            this.launchTickTime = SystemClock.uptimeMillis();
            continueLaunchTickingLocked();
        }
    }

    boolean continueLaunchTickingLocked() {
        if (this.launchTickTime == 0) {
            return false;
        }
        ActivityStack stack = getStack();
        if (stack == null) {
            return false;
        }
        Message msg = stack.mHandler.obtainMessage(103, this);
        stack.mHandler.removeMessages(103);
        stack.mHandler.sendMessageDelayed(msg, 500);
        return true;
    }

    void finishLaunchTickingLocked() {
        this.launchTickTime = 0;
        ActivityStack stack = getStack();
        if (stack != null) {
            stack.mHandler.removeMessages(103);
        }
    }

    public boolean mayFreezeScreenLocked(ProcessRecord app) {
        return (app == null || app.crashing || app.notResponding) ? false : true;
    }

    public void startFreezingScreenLocked(ProcessRecord app, int configChanges) {
        if (mayFreezeScreenLocked(app) && this.mWindowContainerController != null) {
            this.mWindowContainerController.startFreezingScreen(configChanges);
        }
    }

    public void stopFreezingScreenLocked(boolean force) {
        if (force || this.frozenBeforeDestroy) {
            this.frozenBeforeDestroy = false;
            if (this.mWindowContainerController != null) {
                this.mWindowContainerController.stopFreezingScreen(force);
            }
        }
    }

    public void reportFullyDrawnLocked(boolean restoredFromBundle) {
        long curTime = SystemClock.uptimeMillis();
        if (this.displayStartTime != 0) {
            reportLaunchTimeLocked(curTime);
        } else {
            Jlog.warmLaunchingAppEnd(this.shortComponentName);
        }
        Entry entry = this.mStackSupervisor.getLaunchTimeTracker().getEntry(getWindowingMode());
        if (!(this.fullyDrawnStartTime == 0 || entry == null)) {
            long thisTime = curTime - this.fullyDrawnStartTime;
            long totalTime = entry.mFullyDrawnStartTime != 0 ? curTime - entry.mFullyDrawnStartTime : thisTime;
            Trace.asyncTraceEnd(64, "drawing", 0);
            EventLog.writeEvent(EventLogTags.AM_ACTIVITY_FULLY_DRAWN_TIME, new Object[]{Integer.valueOf(this.userId), Integer.valueOf(System.identityHashCode(this)), this.shortComponentName, Long.valueOf(thisTime), Long.valueOf(totalTime)});
            StringBuilder sb = this.service.mStringBuilder;
            sb.setLength(0);
            sb.append("Fully drawn ");
            sb.append(this.shortComponentName);
            sb.append(": ");
            TimeUtils.formatDuration(thisTime, sb);
            if (thisTime != totalTime) {
                sb.append(" (total ");
                TimeUtils.formatDuration(totalTime, sb);
                sb.append(")");
            }
            Log.i(ActivityManagerService.TAG, sb.toString());
            entry.mFullyDrawnStartTime = 0;
        }
        this.mStackSupervisor.getActivityMetricsLogger().logAppTransitionReportedDrawn(this, restoredFromBundle);
        this.fullyDrawnStartTime = 0;
    }

    public boolean isFloating() {
        return this.mIsFloating;
    }

    public boolean isTransluent() {
        return this.mIsTransluent;
    }

    private void reportLaunchTimeLocked(long curTime) {
        Entry entry = this.mStackSupervisor.getLaunchTimeTracker().getEntry(getWindowingMode());
        if (entry != null) {
            long thisTime = curTime - this.displayStartTime;
            long totalTime = entry.mLaunchStartTime != 0 ? curTime - entry.mLaunchStartTime : thisTime;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("launching: ");
            stringBuilder.append(this.packageName);
            Trace.asyncTraceEnd(64, stringBuilder.toString(), 0);
            EventLog.writeEvent(EventLogTags.AM_ACTIVITY_LAUNCH_TIME, new Object[]{Integer.valueOf(this.userId), Integer.valueOf(System.identityHashCode(this)), this.shortComponentName, Long.valueOf(thisTime), Long.valueOf(totalTime)});
            if (getStack() != null && getStack().mshortComponentName.equals(this.shortComponentName)) {
                if (this.app != null) {
                    String str = this.shortComponentName;
                    int i = (int) thisTime;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("#PID:<");
                    stringBuilder2.append(String.valueOf(this.app.pid));
                    stringBuilder2.append(">");
                    Jlog.d(44, str, i, stringBuilder2.toString());
                } else {
                    Jlog.d(44, this.shortComponentName, (int) thisTime, "#PID:<0>");
                }
            }
            StringBuilder sb = this.service.mStringBuilder;
            sb.setLength(0);
            sb.append("Displayed ");
            sb.append(this.shortComponentName);
            sb.append(": ");
            TimeUtils.formatDuration(thisTime, sb);
            if (thisTime != totalTime) {
                sb.append(" (total ");
                TimeUtils.formatDuration(totalTime, sb);
                sb.append(")");
            }
            Log.i(ActivityManagerService.TAG, sb.toString());
            this.mStackSupervisor.reportActivityLaunchedLocked(false, this, thisTime, totalTime);
            this.displayStartTime = 0;
            this.service.mDAProxy.notifyAppEventToIaware(4, this.shortComponentName);
            entry.mLaunchStartTime = 0;
        }
    }

    public void onStartingWindowDrawn(long timestamp) {
        synchronized (this.service) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mStackSupervisor.getActivityMetricsLogger().notifyStartingWindowDrawn(getWindowingMode(), timestamp);
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void onWindowsDrawn(long timestamp) {
        synchronized (this.service) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mStackSupervisor.getActivityMetricsLogger().notifyWindowsDrawn(getWindowingMode(), timestamp);
                if (this.displayStartTime != 0) {
                    reportLaunchTimeLocked(timestamp);
                } else {
                    Jlog.warmLaunchingAppEnd(this.shortComponentName);
                }
                this.mStackSupervisor.sendWaitingVisibleReportLocked(this);
                this.startTime = 0;
                finishLaunchTickingLocked();
                if (this.task != null) {
                    this.task.hasBeenVisible = true;
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void onWindowsVisible() {
        synchronized (this.service) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mStackSupervisor.reportActivityVisibleLocked(this);
                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                    String str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("windowsVisibleLocked(): ");
                    stringBuilder.append(this);
                    Log.v(str, stringBuilder.toString());
                }
                if (!this.nowVisible) {
                    this.nowVisible = true;
                    this.lastVisibleTime = SystemClock.uptimeMillis();
                    int i = 0;
                    if (this.idle || this.mStackSupervisor.isStoppingNoHistoryActivity()) {
                        int size = this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.size();
                        if (size > 0) {
                            while (true) {
                                int i2 = i;
                                if (i2 >= size) {
                                    break;
                                }
                                ActivityRecord r = (ActivityRecord) this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.get(i2);
                                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                                    String str2 = ActivityManagerService.TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Was waiting for visible: ");
                                    stringBuilder2.append(r);
                                    Log.v(str2, stringBuilder2.toString());
                                }
                                i = i2 + 1;
                            }
                            this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.clear();
                            this.mStackSupervisor.scheduleIdleLocked();
                        }
                    } else {
                        this.mStackSupervisor.processStoppingActivitiesLocked(null, false, true);
                    }
                    this.service.scheduleAppGcsLocked();
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void onWindowsGone() {
        synchronized (this.service) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (ActivityManagerService.isInCallActivity(this)) {
                    Flog.i(101, "Incall is gone");
                    Jlog.d(131, "JLID_PHONE_INCALLUI_CLOSE_END");
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("windowsGone(): ");
                stringBuilder.append(this);
                Flog.i(101, stringBuilder.toString());
                this.nowVisible = false;
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean keyDispatchingTimedOut(String reason, int windowPid) {
        ActivityRecord anrActivity;
        ProcessRecord anrApp;
        boolean z;
        boolean windowFromSameProcessAsActivity;
        synchronized (this.service) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                anrActivity = getWaitingHistoryRecordLocked();
                anrApp = this.app;
                z = true;
                windowFromSameProcessAsActivity = this.app == null || this.app.pid == windowPid || windowPid == -1;
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        if (windowFromSameProcessAsActivity) {
            return this.service.inputDispatchingTimedOut(anrApp, anrActivity, this, false, reason);
        }
        if (this.service.inputDispatchingTimedOut(windowPid, false, reason) >= 0) {
            z = false;
        }
        return z;
    }

    private ActivityRecord getWaitingHistoryRecordLocked() {
        if (this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.contains(this) || this.stopped) {
            ActivityStack stack = this.mStackSupervisor.getFocusedStack();
            ActivityRecord r = stack.getResumedActivity();
            if (r == null) {
                r = stack.mPausingActivity;
            }
            if (r != null) {
                return r;
            }
        }
        return this;
    }

    public boolean okToShowLocked() {
        boolean z = false;
        if (!StorageManager.isUserKeyUnlocked(this.userId) && !this.info.applicationInfo.isEncryptionAware()) {
            return false;
        }
        if ((this.info.flags & 1024) != 0 || (this.mStackSupervisor.isCurrentProfileLocked(this.userId) && this.service.mUserController.isUserRunning(this.userId, 0))) {
            z = true;
        }
        return z;
    }

    public boolean isInterestingToUserLocked() {
        return this.visible || this.nowVisible || this.mState == ActivityState.PAUSING || this.mState == ActivityState.RESUMED;
    }

    void setSleeping(boolean _sleeping) {
        setSleeping(_sleeping, false);
    }

    void setSleeping(boolean _sleeping, boolean force) {
        if (!((!force && this.sleeping == _sleeping) || this.app == null || this.app.thread == null)) {
            try {
                this.app.thread.scheduleSleeping(this.appToken, _sleeping);
                if (_sleeping && !this.mStackSupervisor.mGoingToSleepActivities.contains(this)) {
                    this.mStackSupervisor.mGoingToSleepActivities.add(this);
                }
                this.sleeping = _sleeping;
            } catch (RemoteException e) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception thrown when sleeping: ");
                stringBuilder.append(this.intent.getComponent());
                Slog.w(str, stringBuilder.toString(), e);
            }
        }
    }

    static int getTaskForActivityLocked(IBinder token, boolean onlyRoot) {
        ActivityRecord r = forTokenLocked(token);
        if (r == null) {
            return -1;
        }
        TaskRecord task = r.task;
        int activityNdx = task.mActivities.indexOf(r);
        if (activityNdx < 0 || (onlyRoot && activityNdx > task.findEffectiveRootIndex())) {
            return -1;
        }
        return task.taskId;
    }

    static ActivityRecord isInStackLocked(IBinder token) {
        ActivityRecord r = forTokenLocked(token);
        return r != null ? r.getStack().isInStackLocked(r) : null;
    }

    static ActivityStack getStackLocked(IBinder token) {
        ActivityRecord r = isInStackLocked(token);
        if (r != null) {
            return r.getStack();
        }
        return null;
    }

    protected int getDisplayId() {
        ActivityStack stack = getStack();
        if (stack == null) {
            return -1;
        }
        return stack.mDisplayId;
    }

    /* JADX WARNING: Missing block: B:19:0x002a, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final boolean isDestroyable() {
        if (this.finishing || this.app == null) {
            return false;
        }
        ActivityStack stack = getStack();
        if (stack == null || this == stack.getResumedActivity() || this == stack.mPausingActivity || !this.haveState || !this.stopped || this.visible) {
            return false;
        }
        return true;
    }

    private static String createImageFilename(long createTime, int taskId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.valueOf(taskId));
        stringBuilder.append(ACTIVITY_ICON_SUFFIX);
        stringBuilder.append(createTime);
        stringBuilder.append(".png");
        return stringBuilder.toString();
    }

    void setTaskDescription(TaskDescription _taskDescription) {
        if (_taskDescription.getIconFilename() == null) {
            Bitmap icon = _taskDescription.getIcon();
            Bitmap icon2 = icon;
            if (icon != null) {
                String iconFilePath = new File(TaskPersister.getUserImagesDir(this.task.userId), createImageFilename(this.createTime, this.task.taskId)).getAbsolutePath();
                this.service.getRecentTasks().saveImage(icon2, iconFilePath);
                _taskDescription.setIconFilename(iconFilePath);
            }
        }
        this.taskDescription = _taskDescription;
    }

    void setVoiceSessionLocked(IVoiceInteractionSession session) {
        this.voiceSession = session;
        this.pendingVoiceInteractionStart = false;
    }

    void clearVoiceSessionLocked() {
        this.voiceSession = null;
        this.pendingVoiceInteractionStart = false;
    }

    void showStartingWindow(ActivityRecord prev, boolean newTask, boolean taskSwitch) {
        showStartingWindow(prev, newTask, taskSwitch, false);
    }

    void showStartingWindow(ActivityRecord prev, boolean newTask, boolean taskSwitch, boolean fromRecents) {
        ActivityRecord activityRecord = prev;
        if (this.mWindowContainerController != null && !this.mTaskOverlay) {
            CompatibilityInfo compatInfo = this.service.compatibilityInfoForPackageLocked(this.info.applicationInfo);
            boolean shown = this.mWindowContainerController;
            String str = this.packageName;
            int i = this.theme;
            CharSequence charSequence = this.nonLocalizedLabel;
            int i2 = this.labelRes;
            int i3 = this.icon;
            int i4 = this.logo;
            int i5 = this.windowFlags;
            Stub stub = activityRecord != null ? activityRecord.appToken : null;
            boolean isProcessRunning = isProcessRunning();
            boolean allowTaskSnapshot = allowTaskSnapshot();
            boolean z = this.mState.ordinal() >= ActivityState.RESUMED.ordinal() && this.mState.ordinal() <= ActivityState.STOPPED.ordinal();
            int i6 = 1;
            if (shown.addStartingWindow(str, i, compatInfo, charSequence, i2, i3, i4, i5, stub, newTask, taskSwitch, isProcessRunning, allowTaskSnapshot, z, fromRecents)) {
                this.mStartingWindowState = i6;
                this.service.mHwAMSEx.dispatchActivityLifeState(this, "showStartingWindow");
            }
        }
    }

    void removeOrphanedStartingWindow(boolean behindFullscreenActivity) {
        if (this.mStartingWindowState == 1 && behindFullscreenActivity) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                String str = TAG_VISIBILITY;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Found orphaned starting window ");
                stringBuilder.append(this);
                Slog.w(str, stringBuilder.toString());
            }
            this.mStartingWindowState = 2;
            this.mWindowContainerController.removeStartingWindow();
        }
    }

    int getRequestedOrientation() {
        return this.mWindowContainerController.getOrientation();
    }

    void setRequestedOrientation(int requestedOrientation) {
        if (this.mWindowContainerController != null) {
            int displayId = getDisplayId();
            Configuration config = this.mWindowContainerController.setOrientation(requestedOrientation, displayId, this.mStackSupervisor.getDisplayOverrideConfiguration(displayId), mayFreezeScreenLocked(this.app));
            if (config != null) {
                this.frozenBeforeDestroy = true;
                if (!this.service.updateDisplayOverrideConfigurationLocked(config, this, false, displayId)) {
                    this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                }
            }
            this.service.mTaskChangeNotificationController.notifyActivityRequestedOrientationChanged(this.task.taskId, requestedOrientation);
        }
    }

    void setDisablePreviewScreenshots(boolean disable) {
        if (this.mWindowContainerController != null) {
            this.mWindowContainerController.setDisablePreviewScreenshots(disable);
        }
    }

    void setLastReportedGlobalConfiguration(Configuration config) {
        this.mLastReportedConfiguration.setGlobalConfiguration(config);
    }

    void setLastReportedConfiguration(MergedConfiguration config) {
        setLastReportedConfiguration(config.getGlobalConfiguration(), config.getOverrideConfiguration());
    }

    private void setLastReportedConfiguration(Configuration global, Configuration override) {
        this.mLastReportedConfiguration.setConfiguration(global, override);
    }

    private void updateOverrideConfiguration() {
        this.mTmpConfig.unset();
        computeBounds(this.mTmpBounds);
        boolean isInMWPortraitWhiteList = false;
        if (!(this.task == null || !this.task.inMultiWindowMode() || (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(getDisplayId())))) {
            ActivityRecord topActivity = this.task.getTopActivity();
            if (topActivity != null) {
                isInMWPortraitWhiteList = this.service.getPackageManagerInternalLocked().isInMWPortraitWhiteList(topActivity.packageName);
            }
        }
        if (!this.mTmpBounds.equals(getOverrideBounds()) || isInMWPortraitWhiteList) {
            setBounds(this.mTmpBounds);
            Rect updatedBounds = getOverrideBounds();
            if (!matchParentBounds() || isInMWPortraitWhiteList) {
                this.task.computeOverrideConfiguration(this.mTmpConfig, updatedBounds, null, false, false);
            }
            if (this.mTmpBounds.isEmpty()) {
                this.mTmpConfig.nonFullScreen = 0;
            } else {
                this.mTmpConfig.nonFullScreen = 1;
            }
            onOverrideConfigurationChanged(this.mTmpConfig);
        }
    }

    boolean isConfigurationCompatible(Configuration config) {
        int orientation = this.mWindowContainerController != null ? this.mWindowContainerController.getOrientation() : this.info.screenOrientation;
        if (!ActivityInfo.isFixedOrientationPortrait(orientation) || config.orientation == 1) {
            return !ActivityInfo.isFixedOrientationLandscape(orientation) || config.orientation == 2;
        } else {
            return false;
        }
    }

    protected void computeBounds(Rect outBounds) {
        outBounds.setEmpty();
        this.maxAspectRatio = this.info.maxAspectRatio;
        if (mDeviceMaxRatio < 0.0f) {
            mDeviceMaxRatio = this.service.mWindowManager.getDeviceMaxRatio();
        }
        float userMaxAspectRatio = 0.0f;
        if (!(mDeviceMaxRatio <= 0.0f || this.service == null || TextUtils.isEmpty(this.packageName))) {
            userMaxAspectRatio = this.service.getPackageManagerInternalLocked().getUserMaxAspectRatio(this.packageName);
        }
        if (userMaxAspectRatio != 0.0f) {
            if (userMaxAspectRatio >= mDeviceMaxRatio || ((double) this.info.originMaxAspectRatio) <= 1.0d) {
                this.maxAspectRatio = userMaxAspectRatio;
            } else {
                this.maxAspectRatio = this.info.originMaxAspectRatio;
            }
        }
        ActivityStack stack = getStack();
        if (this.task != null && stack != null && !this.task.inMultiWindowMode() && this.maxAspectRatio != 0.0f && !isInVrUiMode(getConfiguration())) {
            Rect appBounds = getParent().getWindowConfiguration().getAppBounds();
            int containingAppWidth = appBounds.width();
            int containingAppHeight = appBounds.height();
            int maxActivityWidth = containingAppWidth;
            int maxActivityHeight = containingAppHeight;
            if (containingAppWidth < containingAppHeight) {
                maxActivityHeight = (int) ((((float) maxActivityWidth) * this.maxAspectRatio) + 0.5f);
            } else {
                maxActivityWidth = (int) ((((float) maxActivityHeight) * this.maxAspectRatio) + 0.5f);
            }
            if (HwFullScreenDisplay.getDeviceMaxRatio() > this.maxAspectRatio || containingAppWidth > maxActivityWidth || containingAppHeight > maxActivityHeight) {
                outBounds.set(0, 0, appBounds.left + maxActivityWidth, appBounds.top + maxActivityHeight);
                if (this.service.mWindowManager.getNavBarPosition() == 1) {
                    outBounds.left = appBounds.right - maxActivityWidth;
                    outBounds.right = appBounds.right;
                }
                this.service.mWindowManager.getAppDisplayRect(this.maxAspectRatio, outBounds, appBounds.left);
                return;
            }
            outBounds.set(getOverrideBounds());
        }
    }

    boolean ensureActivityConfiguration(int globalChanges, boolean preserveWindow) {
        return ensureActivityConfiguration(globalChanges, preserveWindow, false);
    }

    boolean ensureActivityConfiguration(int globalChanges, boolean preserveWindow, boolean ignoreStopState) {
        ActivityStack stack = getStack();
        String str;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        if (stack.mConfigWillChange) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                str = TAG_CONFIGURATION;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Skipping config check (will change): ");
                stringBuilder.append(this);
                Slog.v(str, stringBuilder.toString());
            }
            return true;
        } else if (this.finishing) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                str = TAG_CONFIGURATION;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Configuration doesn't matter in finishing ");
                stringBuilder2.append(this);
                Slog.v(str, stringBuilder2.toString());
            }
            stopFreezingScreenLocked(false);
            return true;
        } else if (!ignoreStopState && !isSplitBaseActivity() && (this.mState == ActivityState.STOPPING || this.mState == ActivityState.STOPPED)) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                str = TAG_CONFIGURATION;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Skipping config check stopped or stopping: ");
                stringBuilder.append(this);
                Slog.v(str, stringBuilder.toString());
            }
            return true;
        } else if (stack.shouldBeVisible(null)) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                str = TAG_CONFIGURATION;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Ensuring correct configuration: ");
                stringBuilder2.append(this);
                Slog.v(str, stringBuilder2.toString());
            }
            int newDisplayId = getDisplayId();
            boolean displayChanged = this.mLastReportedDisplayId != newDisplayId;
            if (displayChanged) {
                this.mLastReportedDisplayId = newDisplayId;
            }
            updateOverrideConfiguration();
            this.mTmpConfig.setTo(this.mLastReportedConfiguration.getMergedConfiguration());
            String str2;
            if (!getConfiguration().equals(this.mTmpConfig) || this.forceNewConfig || displayChanged) {
                int changes = getConfigurationChanges(this.mTmpConfig);
                Configuration newMergedOverrideConfig = getMergedOverrideConfiguration();
                setLastReportedConfiguration(this.service.getGlobalConfiguration(), newMergedOverrideConfig);
                StringBuilder stringBuilder3;
                String str3;
                StringBuilder stringBuilder4;
                if (this.mState == ActivityState.INITIALIZING) {
                    if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                        str2 = TAG_CONFIGURATION;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Skipping config check for initializing activity: ");
                        stringBuilder3.append(this);
                        Slog.v(str2, stringBuilder3.toString());
                    }
                    return true;
                } else if (changes == 0 && !this.forceNewConfig) {
                    if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                        str2 = TAG_CONFIGURATION;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Configuration no differences in ");
                        stringBuilder3.append(this);
                        Slog.v(str2, stringBuilder3.toString());
                    }
                    if (displayChanged) {
                        scheduleActivityMovedToDisplay(newDisplayId, newMergedOverrideConfig);
                    } else {
                        scheduleConfigurationChanged(newMergedOverrideConfig);
                    }
                    return true;
                } else if (this.app == null || this.app.thread == null) {
                    if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                        str3 = TAG_CONFIGURATION;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Configuration doesn't matter not running ");
                        stringBuilder4.append(this);
                        Slog.v(str3, stringBuilder4.toString());
                    }
                    stopFreezingScreenLocked(false);
                    this.forceNewConfig = false;
                    return true;
                } else {
                    if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                        str3 = TAG_CONFIGURATION;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Checking to restart ");
                        stringBuilder4.append(this.info.name);
                        stringBuilder4.append(": changed=0x");
                        stringBuilder4.append(Integer.toHexString(changes));
                        stringBuilder4.append(", handles=0x");
                        stringBuilder4.append(Integer.toHexString(this.info.getRealConfigChanged()));
                        stringBuilder4.append(", mLastReportedConfiguration=");
                        stringBuilder4.append(this.mLastReportedConfiguration);
                        stringBuilder4.append(", forceNewConfig:");
                        stringBuilder4.append(this.forceNewConfig);
                        Slog.v(str3, stringBuilder4.toString());
                    }
                    if (shouldRelaunchLocked(changes, this.mTmpConfig) || this.forceNewConfig) {
                        this.configChangeFlags |= changes;
                        startFreezingScreenLocked(this.app, globalChanges);
                        this.forceNewConfig = false;
                        preserveWindow &= isResizeOnlyChange(changes);
                        if (this.app == null || this.app.thread == null) {
                            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                                str3 = TAG_CONFIGURATION;
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Config is destroying non-running ");
                                stringBuilder4.append(this);
                                Slog.v(str3, stringBuilder4.toString());
                            }
                            stack.destroyActivityLocked(this, true, "config");
                        } else if (this.mState == ActivityState.PAUSING) {
                            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                                str2 = TAG_CONFIGURATION;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Config is skipping already pausing ");
                                stringBuilder3.append(this);
                                Slog.v(str2, stringBuilder3.toString());
                            }
                            this.deferRelaunchUntilPaused = true;
                            this.preserveWindowOnDeferredRelaunch = preserveWindow;
                            return true;
                        } else if (this.mState == ActivityState.RESUMED) {
                            if (!ActivityManagerDebugConfig.DEBUG_STATES || this.visible) {
                                str3 = TAG_CONFIGURATION;
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Config is relaunching resumed ");
                                stringBuilder4.append(this);
                                stringBuilder4.append(", changes=0x");
                                stringBuilder4.append(Integer.toHexString(changes));
                                Slog.v(str3, stringBuilder4.toString());
                            } else {
                                str3 = ActivityManagerService.TAG;
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Config is relaunching resumed invisible activity ");
                                stringBuilder4.append(this);
                                stringBuilder4.append(" called by ");
                                stringBuilder4.append(Debug.getCallers(4));
                                Slog.v(str3, stringBuilder4.toString());
                            }
                            relaunchActivityLocked(true, preserveWindow);
                        } else {
                            String str4 = TAG_CONFIGURATION;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Config is relaunching non-resumed ");
                            stringBuilder3.append(this);
                            stringBuilder3.append(", changes=0x");
                            stringBuilder3.append(Integer.toHexString(changes));
                            Slog.v(str4, stringBuilder3.toString());
                            relaunchActivityLocked(false, preserveWindow);
                        }
                        return false;
                    }
                    if (displayChanged) {
                        scheduleActivityMovedToDisplay(newDisplayId, newMergedOverrideConfig);
                    } else {
                        scheduleConfigurationChanged(newMergedOverrideConfig);
                    }
                    stopFreezingScreenLocked(false);
                    return true;
                }
            }
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                str2 = TAG_CONFIGURATION;
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("Configuration & display unchanged in ");
                stringBuilder5.append(this);
                Slog.v(str2, stringBuilder5.toString());
            }
            return true;
        } else {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                str = TAG_CONFIGURATION;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Skipping config check invisible stack: ");
                stringBuilder.append(this);
                Slog.v(str, stringBuilder.toString());
            }
            return true;
        }
    }

    private boolean shouldRelaunchLocked(int changes, Configuration changesConfig) {
        int configChanged = overrideRealConfigChanged(this.info);
        boolean onlyVrUiModeChanged = onlyVrUiModeChanged(changes, changesConfig);
        if (this.appInfo.targetSdkVersion < 26 && this.requestedVrComponent != null && onlyVrUiModeChanged) {
            configChanged |= 512;
        }
        if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
            String str = TAG_CONFIGURATION;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("shouldRelaunchLocked configChanged=0x");
            stringBuilder.append(Integer.toHexString(configChanged));
            Slog.v(str, stringBuilder.toString());
        }
        return ((~configChanged) & changes) != 0;
    }

    private boolean onlyVrUiModeChanged(int changes, Configuration lastReportedConfig) {
        return changes == 512 && isInVrUiMode(getConfiguration()) != isInVrUiMode(lastReportedConfig);
    }

    protected int getConfigurationChanges(Configuration lastReportedConfig) {
        Configuration currentConfig = getConfiguration();
        int changes = lastReportedConfig.diff(currentConfig);
        if ((changes & 1024) != 0) {
            boolean crosses = crossesHorizontalSizeThreshold(lastReportedConfig.screenWidthDp, currentConfig.screenWidthDp) || crossesVerticalSizeThreshold(lastReportedConfig.screenHeightDp, currentConfig.screenHeightDp);
            if (!crosses) {
                changes &= -1025;
            }
        }
        if (!((changes & 2048) == 0 || crossesSmallestSizeThreshold(lastReportedConfig.smallestScreenWidthDp, currentConfig.smallestScreenWidthDp))) {
            changes &= -2049;
        }
        if ((536870912 & changes) != 0) {
            return changes & -536870913;
        }
        return changes;
    }

    private static boolean isResizeOnlyChange(int change) {
        return (change & -3457) == 0;
    }

    void relaunchActivityLocked(boolean andResume, boolean preserveWindow) {
        if (this.service.mSuppressResizeConfigChanges && preserveWindow) {
            this.configChangeFlags = 0;
            return;
        }
        String str;
        StringBuilder stringBuilder;
        int i;
        List<ResultInfo> pendingResults = null;
        List<ReferrerIntent> pendingNewIntents = null;
        if (andResume) {
            pendingResults = this.results;
            pendingNewIntents = this.newIntents;
        }
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Relaunching: ");
            stringBuilder.append(this);
            stringBuilder.append(" with results=");
            stringBuilder.append(pendingResults);
            stringBuilder.append(" newIntents=");
            stringBuilder.append(pendingNewIntents);
            stringBuilder.append(" andResume=");
            stringBuilder.append(andResume);
            stringBuilder.append(" preserveWindow=");
            stringBuilder.append(preserveWindow);
            Slog.v(str, stringBuilder.toString());
        }
        if (andResume) {
            i = EventLogTags.AM_RELAUNCH_RESUME_ACTIVITY;
        } else {
            i = EventLogTags.AM_RELAUNCH_ACTIVITY;
        }
        EventLog.writeEvent(i, new Object[]{Integer.valueOf(this.userId), Integer.valueOf(System.identityHashCode(this)), Integer.valueOf(this.task.taskId), this.shortComponentName});
        startFreezingScreenLocked(this.app, 0);
        try {
            ActivityLifecycleItem lifecycleItem;
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_STATES) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Moving to ");
                stringBuilder.append(andResume ? "RESUMED" : "PAUSED");
                stringBuilder.append(" Relaunching ");
                stringBuilder.append(this);
                stringBuilder.append(" callers=");
                stringBuilder.append(Debug.getCallers(6));
                Slog.i(str, stringBuilder.toString());
            }
            this.forceNewConfig = false;
            this.mStackSupervisor.activityRelaunchingLocked(this);
            ClientTransactionItem callbackItem = ActivityRelaunchItem.obtain(pendingResults, pendingNewIntents, this.configChangeFlags, new MergedConfiguration(this.service.getGlobalConfiguration(), getMergedOverrideConfiguration()), preserveWindow);
            if (andResume) {
                lifecycleItem = ResumeActivityItem.obtain(this.service.isNextTransitionForward());
            } else {
                lifecycleItem = PauseActivityItem.obtain();
            }
            ClientTransaction transaction = ClientTransaction.obtain(this.app.thread, this.appToken);
            transaction.addCallback(callbackItem);
            transaction.setLifecycleStateRequest(lifecycleItem);
            this.service.getLifecycleManager().scheduleTransaction(transaction);
        } catch (RemoteException e) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.i(ActivityManagerService.TAG, "Relaunch failed", e);
            }
        }
        if (andResume) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Resumed after relaunch ");
                stringBuilder.append(this);
                Slog.d(str, stringBuilder.toString());
            }
            this.results = null;
            this.newIntents = null;
            this.service.getAppWarningsLocked().onResumeActivity(this);
            this.service.showAskCompatModeDialogLocked(this);
        } else {
            this.service.mHandler.removeMessages(101, this);
            setState(ActivityState.PAUSED, "relaunchActivityLocked");
        }
        this.configChangeFlags = 0;
        this.deferRelaunchUntilPaused = false;
        this.preserveWindowOnDeferredRelaunch = false;
    }

    private boolean isProcessRunning() {
        ProcessRecord proc = this.app;
        if (proc == null) {
            proc = (ProcessRecord) this.service.mProcessNames.get(this.processName, this.info.applicationInfo.uid);
        }
        return (proc == null || proc.thread == null) ? false : true;
    }

    private boolean allowTaskSnapshot() {
        if (this.newIntents == null) {
            return true;
        }
        for (int i = this.newIntents.size() - 1; i >= 0; i--) {
            Intent intent = (Intent) this.newIntents.get(i);
            if (intent != null && !isMainIntent(intent)) {
                return false;
            }
        }
        return true;
    }

    boolean isNoHistory() {
        return ((this.intent.getFlags() & 1073741824) == 0 && (this.info.flags & 128) == 0) ? false : true;
    }

    void saveToXml(XmlSerializer out) throws IOException, XmlPullParserException {
        out.attribute(null, ATTR_ID, String.valueOf(this.createTime));
        out.attribute(null, ATTR_LAUNCHEDFROMUID, String.valueOf(this.launchedFromUid));
        if (this.launchedFromPackage != null) {
            out.attribute(null, ATTR_LAUNCHEDFROMPACKAGE, this.launchedFromPackage);
        }
        if (this.resolvedType != null) {
            out.attribute(null, ATTR_RESOLVEDTYPE, this.resolvedType);
        }
        out.attribute(null, ATTR_COMPONENTSPECIFIED, String.valueOf(this.componentSpecified));
        out.attribute(null, ATTR_USERID, String.valueOf(this.userId));
        if (this.taskDescription != null) {
            this.taskDescription.saveToXml(out);
        }
        out.startTag(null, "intent");
        this.intent.saveToXml(out);
        out.endTag(null, "intent");
        if (isPersistable() && this.persistentState != null) {
            out.startTag(null, TAG_PERSISTABLEBUNDLE);
            this.persistentState.saveToXml(out);
            out.endTag(null, TAG_PERSISTABLEBUNDLE);
        }
    }

    static ActivityRecord restoreFromXml(XmlPullParser in, ActivityStackSupervisor stackSupervisor) throws IOException, XmlPullParserException {
        String attrValue;
        PersistableBundle persistentState;
        XmlPullParser xmlPullParser = in;
        Intent intent = null;
        PersistableBundle persistentState2 = null;
        int launchedFromUid = 0;
        String launchedFromPackage = null;
        String resolvedType = null;
        boolean componentSpecified = false;
        int userId = 0;
        long createTime = -1;
        int outerDepth = in.getDepth();
        TaskDescription taskDescription = new TaskDescription();
        int attrNdx = in.getAttributeCount() - 1;
        while (attrNdx >= 0) {
            Intent intent2;
            String attrName = xmlPullParser.getAttributeName(attrNdx);
            attrValue = xmlPullParser.getAttributeValue(attrNdx);
            if (ATTR_ID.equals(attrName)) {
                createTime = Long.parseLong(attrValue);
            } else if (ATTR_LAUNCHEDFROMUID.equals(attrName)) {
                launchedFromUid = Integer.parseInt(attrValue);
            } else if (ATTR_LAUNCHEDFROMPACKAGE.equals(attrName)) {
                launchedFromPackage = attrValue;
            } else if (ATTR_RESOLVEDTYPE.equals(attrName)) {
                resolvedType = attrValue;
            } else if (ATTR_COMPONENTSPECIFIED.equals(attrName)) {
                componentSpecified = Boolean.parseBoolean(attrValue);
            } else if (ATTR_USERID.equals(attrName)) {
                userId = Integer.parseInt(attrValue);
            } else if (attrName.startsWith("task_description_")) {
                taskDescription.restoreFromXml(attrName, attrValue);
            } else {
                String str = ActivityManagerService.TAG;
                intent2 = intent;
                StringBuilder stringBuilder = new StringBuilder();
                persistentState = persistentState2;
                stringBuilder.append("Unknown ActivityRecord attribute=");
                stringBuilder.append(attrName);
                Log.d(str, stringBuilder.toString());
                attrNdx--;
                intent = intent2;
                persistentState2 = persistentState;
            }
            intent2 = intent;
            persistentState = persistentState2;
            attrNdx--;
            intent = intent2;
            persistentState2 = persistentState;
        }
        persistentState = persistentState2;
        while (true) {
            attrNdx = in.next();
            int event = attrNdx;
            if (attrNdx != 1) {
                int i;
                if (event == 3 && in.getDepth() < outerDepth) {
                    i = outerDepth;
                    break;
                } else if (event == 2) {
                    String name = in.getName();
                    if ("intent".equals(name)) {
                        intent = Intent.restoreFromXml(in);
                    } else if (TAG_PERSISTABLEBUNDLE.equals(name)) {
                        persistentState2 = PersistableBundle.restoreFromXml(in);
                    } else {
                        attrValue = ActivityManagerService.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        i = outerDepth;
                        stringBuilder2.append("restoreActivity: unexpected name=");
                        stringBuilder2.append(name);
                        Slog.w(attrValue, stringBuilder2.toString());
                        XmlUtils.skipCurrentTag(in);
                        outerDepth = i;
                    }
                    i = outerDepth;
                    outerDepth = i;
                }
            } else {
                break;
            }
        }
        ActivityStackSupervisor activityStackSupervisor;
        if (intent != null) {
            activityStackSupervisor = stackSupervisor;
            ActivityManagerService service = activityStackSupervisor.mService;
            ActivityInfo aInfo = activityStackSupervisor.resolveActivity(intent, resolvedType, 0, null, userId, Binder.getCallingUid());
            if (aInfo != null) {
                ActivityRecord r = HwServiceFactory.createActivityRecord(service, null, 0, launchedFromUid, launchedFromPackage, intent, resolvedType, aInfo, service.getConfiguration(), null, null, 0, componentSpecified, false, activityStackSupervisor, null, null);
                r.persistentState = persistentState2;
                r.taskDescription = taskDescription;
                r.createTime = createTime;
                return r;
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("restoreActivity resolver error. Intent=");
            stringBuilder3.append(intent);
            stringBuilder3.append(" resolvedType=");
            stringBuilder3.append(resolvedType);
            throw new XmlPullParserException(stringBuilder3.toString());
        }
        activityStackSupervisor = stackSupervisor;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("restoreActivity error intent=");
        stringBuilder4.append(intent);
        throw new XmlPullParserException(stringBuilder4.toString());
    }

    private static boolean isInVrUiMode(Configuration config) {
        return (config.uiMode & 15) == 7;
    }

    int getUid() {
        return this.info.applicationInfo.uid;
    }

    void setShowWhenLocked(boolean showWhenLocked) {
        this.mShowWhenLocked = showWhenLocked;
        this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
    }

    boolean canShowWhenLocked() {
        return !inPinnedWindowingMode() && (this.mShowWhenLocked || this.service.mWindowManager.containsShowWhenLockedWindow(this.appToken));
    }

    void setTurnScreenOn(boolean turnScreenOn) {
        this.mTurnScreenOn = turnScreenOn;
    }

    boolean canTurnScreenOn() {
        ActivityStack stack = getStack();
        if (this.mTurnScreenOn && stack != null && stack.checkKeyguardVisibility(this, true, true)) {
            return true;
        }
        return false;
    }

    boolean getTurnScreenOnFlag() {
        return this.mTurnScreenOn;
    }

    boolean isTopRunningActivity() {
        return this.mStackSupervisor.topRunningActivityLocked() == this;
    }

    void registerRemoteAnimations(RemoteAnimationDefinition definition) {
        this.mWindowContainerController.registerRemoteAnimations(definition);
    }

    public String toString() {
        StringBuilder stringBuilder;
        if (this.stringName != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.stringName);
            stringBuilder.append(" t");
            stringBuilder.append(this.task == null ? -1 : this.task.taskId);
            stringBuilder.append(this.finishing ? " f}" : "}");
            return stringBuilder.toString();
        }
        stringBuilder = new StringBuilder(128);
        stringBuilder.append("ActivityRecord{");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append(" u");
        stringBuilder.append(this.userId);
        stringBuilder.append(' ');
        stringBuilder.append(this.intent.getComponent().flattenToShortString());
        this.stringName = stringBuilder.toString();
        return toString();
    }

    void writeIdentifierToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1120986464257L, System.identityHashCode(this));
        proto.write(1120986464258L, this.userId);
        proto.write(1138166333443L, this.intent.getComponent().flattenToShortString());
        proto.end(token);
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        super.writeToProto(proto, 1146756268033L, false);
        writeIdentifierToProto(proto, 1146756268034L);
        proto.write(1138166333443L, this.mState.toString());
        proto.write(1133871366148L, this.visible);
        proto.write(1133871366149L, this.frontOfTask);
        if (this.app != null) {
            proto.write(1120986464262L, this.app.pid);
        }
        proto.write(1133871366151L, this.fullscreen ^ 1);
        proto.end(token);
    }
}
