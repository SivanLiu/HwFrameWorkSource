package huawei.com.android.server.policy.stylus;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.KeyguardManager;
import android.common.HwFrameworkFactory;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.gesture.Gesture;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import android.widget.Toast;
import com.android.internal.util.ScreenshotHelper;
import com.android.server.LocalServices;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.WindowManagerInternal;
import com.huawei.android.app.HwActivityTaskManager;
import com.huawei.android.inputmethod.HwInputMethodManager;
import com.huawei.android.statistical.StatisticalUtils;
import com.huawei.hiai.awareness.AwarenessConstants;
import huawei.android.view.HwWindowManager;
import huawei.com.android.server.policy.stylus.HwGestureDetector;
import huawei.com.android.server.policy.stylus.StylusGestureDetector;
import huawei.com.android.server.policy.stylus.glow.HwPointPositionView;
import java.util.List;
import java.util.ServiceConfigurationError;

public class StylusGestureListener implements WindowManagerPolicyConstants.PointerEventListener, HwGestureDetector.GestureListener, StylusGestureDetector.StylusGestureRecognizeListener {
    private static final String ACTION_KNOCK_DOWN = "com.qeexo.syswideactions.KNOCK_DOWN";
    private static final float ANGLE_DEVIATION = 10.0f;
    private static final int DEFAULT_TOOL_TYPE = -1;
    private static final String EXTRA_SCREENSHOT_BITMAP = "com.qeexo.syswideactions.screenshot.bitmap";
    private static final String EXTRA_SCREENSHOT_PACKAGENAME = "com.qeexo.syswideactions.screenshot.packagename";
    private static final float LANDSCAPE_ANGLE = 90.0f;
    private static final int LINE_GESTURE_SCALE = 2;
    private static final float MIN_SCORE = 2.0f;
    private static final String NOTCH_PROP = SystemProperties.get("ro.config.hw_notch_size", "");
    private static final float REGION_DIAGONAL_THRESHOLD = 0.5f;
    private static final float STRAIGHTNESS = 4.0f;
    private static final String STYLUS_GESTURES_PATH = "/hw_product/bin/stylus_gestures.bin";
    private static final int STYLUS_USAGED_DURATION_TIMEOUT = 60000;
    private static final int STYLUS_USAGE_TIME_THRESHOLD = 300000;
    private static final String TAG = "StylusGestureListener";
    private static final String WRITE_IME_ID = "com.visionobjects.stylusmobile.v3_2_huawei/com.visionobjects.stylusmobile.v3_2.StylusIMService";
    private ClassLoader mClassLoader;
    private final Context mContext;
    private Class mCustomGestureDetectorCls = null;
    private Object mCustomGestureDetectorObj = null;
    private int mDisplayHeight;
    private int mDisplayWidth;
    private long mFingerTime = 0;
    private final HwGestureDetector mGestureDetector;
    private Handler mHandler;
    private boolean mHasKnockDownOccured;
    private boolean mHasNotchInScreen = false;
    private boolean mIsActionOnKeyboard = false;
    private boolean mIsCanceledTiming = false;
    private boolean mIsFirstTime = true;
    private boolean mIsForPcModeOnly = false;
    private boolean mIsStylusViewAdded = false;
    private KeyguardManager mKeyguardManager;
    private long mLastFingerUsageTime = 0;
    private long mLastStylusUsageTime = 0;
    private int mLastToolType = -1;
    private WindowManager.LayoutParams mLayoutParams;
    private final HwPhoneWindowManager mPhoneWindowManager;
    private String mPkg;
    private Class mPointerLocationViewCls = null;
    private Object mPointerLocationViewObj = null;
    private Bitmap mScreenshotBitmap;
    private ScreenshotHelper mScreenshotHelper;
    private final StylusGestureDetector mStylusGestureDetector;
    private StylusGestureManager mStylusGestureManager;
    private HwPointPositionView mStylusGestureView;
    private long mStylusTime = 0;
    private Object mViewLock = new Object();
    private WindowManager mWindowManager;
    private WindowManagerInternal mWindowManagerInternal;
    private final MotionEventRunnable onKnockDownRunnable = new MotionEventRunnable() {
        /* class huawei.com.android.server.policy.stylus.StylusGestureListener.AnonymousClass1 */

        public void run() {
            StylusGestureListener.this.notifyKnockDown(this.event);
        }
    };
    private final MotionEventRunnable usageDurationTimeout = new MotionEventRunnable() {
        /* class huawei.com.android.server.policy.stylus.StylusGestureListener.AnonymousClass2 */

        public void run() {
            Log.i(StylusGestureListener.TAG, "handle Uasage Duration Timeout");
            StylusGestureListener.this.reportStylusUasageDurationTimeout();
        }
    };

    public StylusGestureListener(Context context, HwPhoneWindowManager phoneWindowManager) {
        this.mContext = context;
        this.mPhoneWindowManager = phoneWindowManager;
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mStylusGestureDetector = new StylusGestureDetector(this.mContext, this);
        this.mHasKnockDownOccured = false;
        this.mHandler = new Handler();
        updateConfiguration();
        this.mGestureDetector = new HwGestureDetector(this.mContext, STYLUS_GESTURES_PATH);
        this.mGestureDetector.setGestureListener(this);
        this.mGestureDetector.setMinLineGestureStrokeLength(this.mDisplayWidth / 2);
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mScreenshotHelper = new ScreenshotHelper(context);
        this.mStylusGestureManager = new StylusGestureManager(context);
        this.mHasNotchInScreen = !TextUtils.isEmpty(NOTCH_PROP);
    }

    public void setForPCModeOnly(boolean isOnly4PcMode) {
        this.mIsForPcModeOnly = isOnly4PcMode;
    }

    public void cancelStylusGesture() {
        MotionEvent cancelEvent = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 3, 0.0f, 0.0f, 0);
        HwPointPositionView hwPointPositionView = this.mStylusGestureView;
        if (hwPointPositionView != null) {
            hwPointPositionView.onTouchEvent(cancelEvent);
        }
        this.mGestureDetector.onTouchEvent(cancelEvent);
        removeStylusGestureView();
        this.mHasKnockDownOccured = false;
        cancelEvent.recycle();
    }

    private void recycleScreenshot() {
        Bitmap bitmap = this.mScreenshotBitmap;
        if (bitmap != null) {
            bitmap.recycle();
            this.mScreenshotBitmap = null;
        }
    }

    private void saveScreenBitmap() {
        recycleScreenshot();
        this.mScreenshotBitmap = SurfaceControl.screenshot_ext_hw(new Rect(0, 0, this.mDisplayWidth, this.mDisplayHeight), this.mDisplayWidth, this.mDisplayHeight, 0);
    }

    private KeyguardManager getKeyguardManager() {
        if (this.mKeyguardManager == null) {
            this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        }
        return this.mKeyguardManager;
    }

    private void dismissKeyguardIfCurrentlyShown() {
        KeyguardManager keyguardManager = getKeyguardManager();
        if (keyguardManager == null) {
            Log.e(TAG, "keyguardManager is null");
        } else if (keyguardManager.inKeyguardRestrictedInputMode()) {
            Log.d(TAG, "phoneWindowManager.dismissKeyguardLw()");
            this.mPhoneWindowManager.dismissKeyguardLw(null, null);
        }
    }

    public void updateConfiguration() {
        Display display = this.mWindowManager.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        this.mDisplayWidth = size.x > size.y ? size.y : size.x;
        this.mDisplayHeight = size.x > size.y ? size.x : size.y;
    }

    public void setToolType() {
        StylusGestureDetector stylusGestureDetector = this.mStylusGestureDetector;
        if (stylusGestureDetector != null) {
            stylusGestureDetector.setToolType();
        }
    }

    public void onPointerEvent(MotionEvent motionEvent) {
        if (motionEvent == null) {
            Log.e(TAG, "onPointerEvent MotionEvent is null");
            return;
        }
        showStylusIntroducePage(motionEvent);
        if (StylusGestureDetector.IS_TABLET || this.mStylusGestureManager.isStylusEnabled()) {
            this.mStylusGestureDetector.shouldSwtichInputMethod(motionEvent);
            reportStylusUsageDuration(motionEvent.getAction(), motionEvent.getToolType(0));
            reportStylusUsageCount(motionEvent);
            if (!this.mIsForPcModeOnly) {
                if (motionEvent.getAction() == 0) {
                    Log.i(TAG, "stylusMotionEvent ACTION_DOWN " + motionEvent);
                }
                if (isValidStylusMotionEvent(motionEvent)) {
                    if (motionEvent.getAction() == 0) {
                        MotionEventRunnable motionEventRunnable = this.onKnockDownRunnable;
                        motionEventRunnable.event = motionEvent;
                        this.mHandler.post(motionEventRunnable);
                        notifyKnockDown(motionEvent);
                        this.mHasKnockDownOccured = true;
                        saveScreenBitmap();
                        addStylusGestureView();
                        Log.d(TAG, "stylus actionDown occured");
                    }
                    this.mStylusGestureDetector.onTouchEvent(motionEvent);
                    this.mGestureDetector.onTouchEvent(motionEvent);
                    HwPointPositionView hwPointPositionView = this.mStylusGestureView;
                    if (hwPointPositionView != null) {
                        hwPointPositionView.onTouchEvent(motionEvent);
                    }
                    if (motionEvent.getAction() == 1 || motionEvent.getAction() == 3) {
                        Log.d(TAG, "stylus actionUp occured");
                        cancelStylusGesture();
                    }
                } else if (this.mHasKnockDownOccured) {
                    cancelStylusGesture();
                }
            }
        }
    }

    private static abstract class MotionEventRunnable implements Runnable {
        MotionEvent event;

        private MotionEventRunnable() {
            this.event = null;
        }
    }

    private void cancelUasageDurationTimeout() {
        this.mHandler.removeCallbacks(this.usageDurationTimeout);
        this.mIsCanceledTiming = true;
    }

    /* access modifiers changed from: private */
    public void reportStylusUasageDurationTimeout() {
        reportStylusUsageDuration(0, 1);
    }

    private void reportStylusUsageDuration(int action, int currentToolType) {
        if (action == 1 || (action == 3 && currentToolType == 2)) {
            cancelUasageDurationTimeout();
            this.mIsCanceledTiming = false;
            this.mHandler.postDelayed(this.usageDurationTimeout, AppHibernateCst.DELAY_ONE_MINS);
        }
        if (!this.mIsCanceledTiming && action == 2 && currentToolType == 2) {
            cancelUasageDurationTimeout();
        }
        if (action != 0) {
            return;
        }
        if (currentToolType == 2) {
            this.mPkg = getTopActivity();
            if (this.mLastToolType != currentToolType) {
                this.mStylusTime = SystemClock.uptimeMillis();
                this.mLastToolType = currentToolType;
                return;
            }
            return;
        }
        int i = this.mLastToolType;
        if (currentToolType != i && i > 0) {
            cancelUasageDurationTimeout();
            this.mLastToolType = currentToolType;
            this.mFingerTime = SystemClock.uptimeMillis();
            String currentPkgName = getTopActivity();
            if (currentPkgName != null) {
                try {
                    String[] result = currentPkgName.split("\\/");
                    if (result != null && result.length > 0 && this.mPkg != null) {
                        if (this.mPkg.contains(result[0])) {
                            int duration = Integer.parseInt(String.valueOf((this.mFingerTime - this.mStylusTime) / 1000));
                            Context context = this.mContext;
                            Flog.bdReport(context, 950, "{package:" + result[0] + ",duration:" + duration + "}");
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "stylus reportStylusUsageDuration NumberFormatException");
                } catch (Exception e2) {
                    Log.e(TAG, "stylus reportStylusUsageDuration");
                }
            }
        }
    }

    private String getTopActivity() {
        ActivityInfo activityInfo = HwActivityTaskManager.getLastResumedActivity();
        if (activityInfo == null) {
            return null;
        }
        ComponentName componentName = activityInfo.getComponentName();
        if (componentName != null) {
            return componentName.flattenToShortString();
        }
        Log.e(TAG, "componentName is null.");
        return null;
    }

    private void reportStylusUsageCount(MotionEvent motionEvent) {
        if (motionEvent.getAction() != 0) {
            return;
        }
        if (motionEvent.getToolType(0) != 2) {
            this.mLastFingerUsageTime = SystemClock.uptimeMillis();
        } else if (this.mIsFirstTime) {
            Flog.bdReport(this.mContext, 951);
            this.mIsFirstTime = false;
        } else {
            long stylusUsageTime = SystemClock.uptimeMillis();
            if (this.mLastStylusUsageTime == 0) {
                this.mLastStylusUsageTime = stylusUsageTime;
            }
            long j = this.mLastFingerUsageTime;
            if (j > 0 && j < stylusUsageTime && stylusUsageTime - this.mLastStylusUsageTime >= 300000) {
                this.mLastStylusUsageTime = 0;
                this.mLastFingerUsageTime = 0;
                Flog.bdReport(this.mContext, 951);
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyKnockDown(MotionEvent event) {
        Intent intent = new Intent(ACTION_KNOCK_DOWN);
        intent.addFlags(536870912);
        this.mContext.sendBroadcast(intent);
    }

    private boolean isValidStylusMotionEvent(MotionEvent motionEvent) {
        boolean isStylusScreenshotMode = motionEvent.getToolType(0) == 2 && motionEvent.getButtonState() == 32;
        WindowManagerPolicy.WindowState windowState = this.mPhoneWindowManager.getFocusedWindow();
        return isStylusScreenshotMode && (windowState == null || (windowState.getAttrs().flags & HwWindowManager.LayoutParams.FLAG_DISABLE_KNUCKLE_TO_LAUNCH_APP) == 0);
    }

    @Override // huawei.com.android.server.policy.stylus.HwGestureDetector.GestureListener
    public void onRegionGesture(String gestureName, Gesture gesture, double predictionScore) {
        if (gestureName == null || gesture == null) {
            Log.e(TAG, "onRegionGesture param error");
            return;
        }
        Log.i(TAG, "onRegionGesture, gestureName = " + gestureName + " predictionScore = " + predictionScore);
        Intent intent = StylusGestureSettings.getIntentForStylusGesture(gestureName, gesture, predictionScore, this.mContext);
        if (intent == null) {
            Log.d(TAG, "Ignoring " + gestureName + " gesture.");
        } else if (this.mScreenshotBitmap == null) {
            Log.e(TAG, "ScreenshotBitmap is null, failed to take Screenshot.");
        } else {
            String packageName = getCurrentPackageName(this.mContext);
            intent.addFlags(AwarenessConstants.TRAVEL_HELPER_DATA_CHANGE_ACTION);
            intent.putExtra(EXTRA_SCREENSHOT_BITMAP, this.mScreenshotBitmap.copy(Bitmap.Config.ARGB_8888, false));
            intent.putExtra(EXTRA_SCREENSHOT_PACKAGENAME, packageName);
            recycleScreenshot();
            dismissKeyguardIfCurrentlyShown();
            if (StylusGestureSettings.checkPackageInstalled(this.mContext, StylusGestureSettings.PKG_QEEXO_SMARTSHOT)) {
                intent.setPackage(StylusGestureSettings.PKG_QEEXO_SMARTSHOT);
            } else if (StylusGestureSettings.checkPackageInstalled(this.mContext, StylusGestureSettings.PKG_HUAWEI_SMARTSHOT)) {
                intent.setPackage(StylusGestureSettings.PKG_HUAWEI_SMARTSHOT);
            } else {
                Log.w(TAG, "both qeexo and huawei smartshot not exists");
                return;
            }
            try {
                this.mContext.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
                Log.i(TAG, "Start screenshot regionCrop activity");
                StatisticalUtils.reportc(this.mContext, 162);
                Flog.bdReport(this.mContext, 952, "true");
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Failed to start activity: ");
            }
        }
    }

    @Override // huawei.com.android.server.policy.stylus.HwGestureDetector.GestureListener
    public void onLetterGesture(String gestureName, Gesture gesture, double predictionScore) {
        List<ActivityManager.RecentTaskInfo> recentTask;
        if (gestureName == null || gesture == null) {
            Log.e(TAG, "onLetterGesture param error");
            return;
        }
        Log.i(TAG, "onLetterGesture, gestureName = " + gestureName);
        Intent intent = StylusGestureSettings.getIntentForStylusGesture(gestureName, gesture, predictionScore, this.mContext);
        if (intent == null) {
            Log.d(TAG, "Ignoring " + gestureName + " gesture.");
            return;
        }
        try {
            ParceledListSlice<ActivityManager.RecentTaskInfo> slice = ActivityManagerNative.getDefault().getRecentTasks(1, 1, ActivityManagerNative.getDefault().getCurrentUser().id);
            if (!(slice == null || (recentTask = slice.getList()) == null || recentTask.size() <= 0)) {
                UserInfo user = ((UserManager) this.mContext.getSystemService("user")).getUserInfo(recentTask.get(0).userId);
                if (user == null || !user.isManagedProfile()) {
                    this.mContext.startServiceAsUser(intent, ActivityManagerNative.getDefault().getCurrentUser().getUserHandle());
                } else {
                    this.mContext.startServiceAsUser(intent, user.getUserHandle());
                }
                Flog.bdReport(this.mContext, 952, "true");
            }
            Log.i(TAG, "Start MultiScreenShotService.");
            StatisticalUtils.reportc(this.mContext, 163);
        } catch (SecurityException e) {
            Log.e(TAG, "Can not find the service with SecurityException");
        } catch (IllegalStateException e2) {
            Log.e(TAG, "Can not find the service with IllegalStateException");
        } catch (Exception e3) {
            Log.e(TAG, "Can not find the service");
        }
    }

    @Override // huawei.com.android.server.policy.stylus.HwGestureDetector.GestureListener
    public void onLineGesture(String gestureName, Gesture gesture) {
        Log.i(TAG, "onLineGesture, gestureName = " + gestureName);
        if (isTopTaskRecentOrHome()) {
            Log.i(TAG, "Top task is home or recent, return.");
            Toast toast = Toast.makeText(this.mContext, 33685924, 0);
            toast.getWindowParams().privateFlags |= 16;
            toast.show();
            return;
        }
        ((StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class)).toggleSplitScreen();
        Flog.bdReport(this.mContext, 952, "true");
    }

    @Override // huawei.com.android.server.policy.stylus.StylusGestureDetector.StylusGestureRecognizeListener
    public void onStylusSingleTapPerformed(MotionEvent event, boolean isShowMenuView) {
        if (event != null) {
            if (StylusGestureDetector.IS_TABLET) {
                int positionX = (int) event.getRawX();
                int positionY = (int) event.getRawY();
                Log.i(TAG, "onStylusSingleTapPerformed positionX = " + positionX + " positionY = " + positionY);
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(StylusGestureSettings.FLOAT_ENTRANCE_PACKAGE_NAME, StylusGestureSettings.FLOAT_ENTRANCE_CLASSNAME));
                intent.putExtra("positionX", positionX);
                intent.putExtra("positionY", positionY);
                if (isShowMenuView) {
                    try {
                        StatisticalUtils.reportc(this.mContext, 160);
                        intent.putExtra("prepareStatus", 1);
                        this.mContext.startServiceAsUser(intent, UserHandle.CURRENT_OR_SELF);
                    } catch (ServiceConfigurationError e) {
                        Log.e(TAG, "can not start service: " + e.getMessage());
                    }
                } else {
                    this.mContext.stopServiceAsUser(intent, UserHandle.CURRENT_OR_SELF);
                }
            } else if (isShowMenuView) {
                Log.i(TAG, "onStylusSingleTapPerformed and wakeupNoteEditor.");
                wakeupNoteEditor();
            }
        }
    }

    @Override // huawei.com.android.server.policy.stylus.StylusGestureDetector.StylusGestureRecognizeListener
    public void onStylusDoubleTapPerformed() {
        if (this.mPhoneWindowManager != null) {
            HwPointPositionView hwPointPositionView = this.mStylusGestureView;
            if (hwPointPositionView != null && hwPointPositionView.isShown()) {
                removeStylusGestureView();
                Log.d(TAG, "mStylusGestureView isShown(), removing...");
            }
            Log.i(TAG, "onStylusDoubleTapPerformed and takeScreenshot.");
            StatisticalUtils.reportc(this.mContext, (int) CPUFeature.MSG_RM_AUX_THREAD);
            this.mScreenshotHelper.takeScreenshot(1, true, true, this.mHandler);
            Flog.bdReport(this.mContext, 952, "true");
        }
    }

    @Override // huawei.com.android.server.policy.stylus.StylusGestureDetector.StylusGestureRecognizeListener
    public void notifySwtichInputMethod(boolean isStylus) {
        String ime = isStylus ? "handwrite IME" : "general IME";
        Log.i(TAG, "notifySwtichInputMethod and swtich current IME to " + ime);
        StatisticalUtils.reportc(this.mContext, 164);
        if (isStylus) {
            try {
                HwInputMethodManager.setDefaultIme(WRITE_IME_ID);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "SwtichInputMethod IllegalArgumentException:" + e.getMessage());
            } catch (Exception e2) {
                Log.e(TAG, "SwtichInputMethod error");
            }
        } else {
            HwInputMethodManager.setDefaultIme("");
        }
    }

    private String getCurrentPackageName(Context context) {
        try {
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = ((ActivityManager) context.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG)).getRunningTasks(1);
            if (runningTaskInfos != null) {
                if (!runningTaskInfos.isEmpty()) {
                    ActivityManager.RunningTaskInfo runningTaskInfo = runningTaskInfos.get(0);
                    if (runningTaskInfo == null) {
                        Log.e(TAG, "failed to get runningTaskInfo");
                        return "";
                    }
                    String packageName = runningTaskInfo.topActivity.getPackageName();
                    if (!TextUtils.isEmpty(packageName)) {
                        return packageName;
                    }
                    return "";
                }
            }
            Log.e(TAG, "running task is null");
            return "";
        } catch (SecurityException e) {
            Log.e(TAG, "get current package name error with SecurityException");
            return "";
        } catch (Exception e2) {
            Log.e(TAG, "get current package name error");
            return "";
        }
    }

    private void showStylusIntroducePage(MotionEvent motionEvent) {
        if (!StylusGestureDetector.IS_TABLET && motionEvent.getToolType(0) == 2 && motionEvent.getAction() == 0 && !this.mStylusGestureManager.isStylusIntroduced()) {
            Log.d(TAG, "show stylus introduce page!");
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(StylusGestureSettings.FLOAT_ENTRANCE_PACKAGE_NAME, "com.huawei.stylus.floatmenu.IntroduceDialogActivity"));
            intent.addFlags(268435456);
            try {
                this.mContext.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
            } catch (ActivityNotFoundException exp) {
                Log.e(TAG, "start introduce Activity failed! message : " + exp.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "startActivity failed!");
            }
        }
    }

    private void wakeupNoteEditor() {
        try {
            this.mContext.startActivityAsUser(new Intent("android.huawei.intent.action.note.handwriting"), UserHandle.CURRENT_OR_SELF);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "startActivity notepad activity failed! message : " + ex.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "startActivityAsUser(): Exception");
        }
    }

    private boolean isTopTaskRecentOrHome() {
        try {
            List<ActivityManager.RunningTaskInfo> tasks = ActivityManager.getService().getFilteredTasks(1, 0, 2);
            if (tasks.isEmpty()) {
                return false;
            }
            int activityType = tasks.get(0).configuration.windowConfiguration.getActivityType();
            return activityType == 2 || activityType == 3;
        } catch (RemoteException e) {
            return false;
        }
    }

    private void addStylusGestureView() {
        boolean isVrMode = false;
        if (HwFrameworkFactory.getVRSystemServiceManager() != null) {
            isVrMode = HwFrameworkFactory.getVRSystemServiceManager().isVRMode();
        }
        if (isVrMode) {
            Log.d(TAG, "current is in VR Mode,view cannot added!");
            return;
        }
        if (this.mStylusGestureView == null) {
            this.mStylusGestureView = new HwPointPositionView(this.mContext, this);
            this.mLayoutParams = new WindowManager.LayoutParams(-1, -1);
            WindowManager.LayoutParams layoutParams = this.mLayoutParams;
            layoutParams.type = HwArbitrationDEFS.MSG_MPLINK_STOP_COEX_SUCC;
            layoutParams.flags = 1304;
            layoutParams.privateFlags |= 16;
            if (ActivityManager.isHighEndGfx()) {
                this.mLayoutParams.flags |= 16777216;
                this.mLayoutParams.privateFlags |= 2;
            }
            WindowManager.LayoutParams layoutParams2 = this.mLayoutParams;
            layoutParams2.format = -3;
            layoutParams2.setTitle("StylusGestureView");
            this.mLayoutParams.inputFeatures |= 2;
            if (this.mHasNotchInScreen) {
                this.mLayoutParams.layoutInDisplayCutoutMode = 1;
            }
        }
        synchronized (this.mViewLock) {
            if (!this.mIsStylusViewAdded) {
                Log.d(TAG, "addStylusGestureView");
                this.mWindowManager.addView(this.mStylusGestureView, this.mLayoutParams);
                this.mIsStylusViewAdded = true;
            }
        }
    }

    private void removeStylusGestureView() {
        synchronized (this.mViewLock) {
            if (this.mStylusGestureView != null && this.mIsStylusViewAdded) {
                Log.d(TAG, "removeStylusGestureView");
                this.mWindowManager.removeView(this.mStylusGestureView);
                this.mIsStylusViewAdded = false;
            }
        }
    }
}
