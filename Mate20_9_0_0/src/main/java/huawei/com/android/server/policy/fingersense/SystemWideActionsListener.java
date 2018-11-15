package huawei.com.android.server.policy.fingersense;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManagerNative;
import android.app.KeyguardManager;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.gesture.Gesture;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Flog;
import android.util.IMonitor;
import android.util.IMonitor.EventStream;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import android.view.WindowManagerPolicyConstants.PointerEventListener;
import android.widget.Toast;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.ScreenshotHelper;
import com.android.server.LocalServices;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy.WindowState;
import com.android.server.wm.WindowManagerInternal;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.IGameObserver.Stub;
import com.huawei.android.statistical.StatisticalUtils;
import huawei.android.provider.FingerSenseSettings;
import huawei.com.android.server.policy.fingersense.CustomGestureDetector.OnGesturePerformedListener;
import huawei.com.android.server.policy.fingersense.CustomGestureDetector.OrientationFix;
import huawei.com.android.server.policy.fingersense.KnockGestureDetector.OnKnockGestureListener;
import huawei.com.android.server.policy.fingersense.pixiedust.PointerLocationView;
import huawei.com.android.server.policy.stylus.StylusGestureSettings;
import java.util.List;

public class SystemWideActionsListener implements PointerEventListener, OnGesturePerformedListener, OnKnockGestureListener {
    private static final String ACTION_KNOCK_DOWN = "com.qeexo.syswideactions.KNOCK_DOWN";
    private static final String CAMERA_PACKAGE_NAME = "com.huawei.camera";
    private static final boolean DEBUG = false;
    private static final boolean DISABLE_MULTIWIN = SystemProperties.getBoolean("ro.huawei.disable_multiwindow", false);
    private static final int EVENT_MARK_AS_GAME = 4;
    private static final int EVENT_MOVE_BACKGROUND = 2;
    private static final int EVENT_MOVE_FRONT = 1;
    private static final int EVENT_REPLACE_FRONT = 3;
    public static final String EXTRA_EVENT1 = "com.qeexo.syswideactions.event1";
    public static final String EXTRA_GESTURE = "com.qeexo.syswideactions.gesture";
    public static final String EXTRA_GESTURE_NAME = "com.qeexo.syswideactions.gesture.name";
    public static final String EXTRA_GESTURE_PREDICTION_SCORE = "com.qeexo.syswideactions.gesture.score";
    public static final String EXTRA_SCREENSHOT_BITMAP = "com.qeexo.syswideactions.screenshot.bitmap";
    private static final String EXTRA_SCREENSHOT_PACKAGENAME = "com.qeexo.syswideactions.screenshot.packagename";
    private static final String FINGERSENSE_DISABLE = "0";
    private static final String FINGERSENSE_ENABLE = "1";
    private static final String GAME_GESTURE_MODE = "game_gesture_disabled_mode";
    private static final int GAME_GESTURE_MODE_CLOSE = 2;
    private static final int GAME_GESTURE_MODE_DEFAULT = 2;
    private static final int GAME_GESTURE_MODE_OPEN = 1;
    private static boolean HWFLOW = false;
    private static final String KNUCKLE_GESTURES_PATH = "/system/bin/knuckle_gestures.bin";
    private static final String LEFT = "left";
    private static final int MAX_PATH_LENGTH = 6144;
    private static final int MSG_LETTER_GETURE = 1;
    private static final int MSG_LINE_GETURE = 2;
    private static final int MSG_REGION_GETURE = 0;
    private static final int MSG_SCREEN_CAPTURED_GETURE = 4;
    private static final int MSG_SCREEN_RECORDER_GETURE = 3;
    private static final OrientationFix[] ORIENTATION_FIXES = new OrientationFix[]{new OrientationFix(StylusGestureSettings.STYLUS_GESTURE_C_SUFFIX, StylusGestureSettings.STYLUS_GESTURE_W_SUFFIX, null), new OrientationFix(StylusGestureSettings.STYLUS_GESTURE_C_SUFFIX, StylusGestureSettings.STYLUS_GESTURE_M_SUFFIX, null)};
    private static final String PERSIST_SYS_FINGERSENSE = "persist.sys.fingersense";
    private static final String RIGHT = "right";
    private static final float SCALE = 0.75f;
    private static final int SPLIT_SCREEN_MIN_SCREEN_HEIGHT_PERCENTAGE = 10;
    private static final int STATE_LEFT = 1;
    private static final int STATE_MIDDLE = 0;
    private static final int STATE_RIGHT = 2;
    private static final String TAG = "SystemWideActionsListener";
    private static final String mNotchProp = SystemProperties.get("ro.config.hw_notch_size", "");
    private boolean FLOG_FLAG = true;
    private final ActivityManager activityManager;
    private final Context context;
    private final CustomGestureDetector customGestureDetector;
    private Handler handler;
    private boolean hasKnuckleDownOccured;
    private boolean isActionOnKeyboard = false;
    private final KnockGestureDetector knockGestureDetector;
    private KnuckGestureSetting knuckGestureSetting = KnuckGestureSetting.getInstance();
    private LayoutParams layoutParams;
    private int mDisplayHeight;
    private int mDisplayWidth;
    private boolean mHasNotchInScreen = false;
    private HwGameObserver mHwGameObserver = null;
    private KeyguardManager mKeyguardManager;
    private int mNavBarLandscapeHeight;
    private int mNavBarPortraitHeight;
    private Bitmap mScreenshotBitmap;
    private ScreenshotHelper mScreenshotHelper;
    final Object mServiceAquireLock = new Object();
    IStatusBarService mStatusBarService;
    private WindowManagerInternal mWindowManagerInternal;
    private final MotionEventRunnable onKnockDownRunnable = new MotionEventRunnable() {
        public void run() {
            SystemWideActionsListener.this.notifyKnockDown(this.event);
        }
    };
    private final HwPhoneWindowManager phoneWindowManager;
    private PointerLocationView pointerLocationView;
    private Intent startServiceIntent;
    private Vibrator vibrator;
    private boolean viewAdd = false;
    private Object viewLock = new Object();
    private final WindowManager windowManager;

    private class HwGameObserver extends Stub {
        private HwGameObserver() {
        }

        /* synthetic */ HwGameObserver(SystemWideActionsListener x0, AnonymousClass1 x1) {
            this();
        }

        public void onGameListChanged() {
        }

        public void onGameStatusChanged(String packageName, int event) {
            String str = SystemWideActionsListener.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("observe gameStatusChange event : ");
            stringBuilder.append(event);
            Log.d(str, stringBuilder.toString());
            if (!FingerSenseSettings.isFingerSenseEnabled(SystemWideActionsListener.this.context.getContentResolver())) {
                Log.d(SystemWideActionsListener.TAG, "fingersense not enabled");
            } else if (SystemProperties.getBoolean("runtime.mmitest.isrunning", false)) {
                Log.d(SystemWideActionsListener.TAG, "in MMI test");
            } else {
                if (Secure.getIntForUser(SystemWideActionsListener.this.context.getContentResolver(), "game_gesture_disabled_mode", 2, 0) == 1 && SystemWideActionsListener.this.isGameForeground(event)) {
                    SystemProperties.set(SystemWideActionsListener.PERSIST_SYS_FINGERSENSE, "0");
                } else {
                    SystemProperties.set(SystemWideActionsListener.PERSIST_SYS_FINGERSENSE, "1");
                }
            }
        }
    }

    private static abstract class MotionEventRunnable implements Runnable {
        MotionEvent event;

        private MotionEventRunnable() {
            this.event = null;
        }

        /* synthetic */ MotionEventRunnable(AnonymousClass1 x0) {
            this();
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    IStatusBarService getHWStatusBarService() {
        IStatusBarService iStatusBarService;
        synchronized (this.mServiceAquireLock) {
            if (this.mStatusBarService == null) {
                this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
            }
            iStatusBarService = this.mStatusBarService;
        }
        return iStatusBarService;
    }

    public SystemWideActionsListener(Context context, HwPhoneWindowManager phoneWindowManager) {
        this.context = context;
        this.phoneWindowManager = phoneWindowManager;
        this.activityManager = (ActivityManager) context.getSystemService("activity");
        this.windowManager = (WindowManager) context.getSystemService("window");
        this.mNavBarPortraitHeight = context.getResources().getDimensionPixelSize(17105186);
        this.mNavBarLandscapeHeight = context.getResources().getDimensionPixelSize(17105188);
        Display display = this.windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        this.mDisplayWidth = size.x > size.y ? size.y : size.x;
        this.mDisplayHeight = size.x > size.y ? size.x : size.y;
        this.customGestureDetector = new CustomGestureDetector(context, KNUCKLE_GESTURES_PATH, (OnGesturePerformedListener) this);
        this.customGestureDetector.setMinPredictionScore(2.0f);
        this.customGestureDetector.setOrientationFixes(ORIENTATION_FIXES);
        this.customGestureDetector.setMinLineGestureStrokeLength(this.mDisplayWidth / 2);
        this.customGestureDetector.setLineGestureStrokePortraitAngle(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
        this.customGestureDetector.setLineGestureStrokeLandscapeAngle(90.0f);
        this.customGestureDetector.setMaxLineGestureStrokeAngleDeviation(10.0f);
        this.customGestureDetector.setLineGestureStrokeStraightness(4.0f);
        this.knockGestureDetector = new KnockGestureDetector(context, this);
        this.hasKnuckleDownOccured = false;
        this.handler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String str = SystemWideActionsListener.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleMessage msg ");
                stringBuilder.append(msg.what);
                Log.d(str, stringBuilder.toString());
                EventStream eStream = IMonitor.openEventStream(KnuckGestureSetting.KNOCK_GESTURE_DATA_RECORD);
                if (eStream != null) {
                    switch (msg.what) {
                        case 0:
                            eStream.setParam((short) 2, 1);
                            eStream.setParam((short) 23, SystemWideActionsListener.this.knuckGestureSetting.getTpVendorName());
                            eStream.setParam((short) 24, SystemWideActionsListener.this.knuckGestureSetting.getAccVendorName());
                            eStream.setParam((short) 25, SystemWideActionsListener.this.knuckGestureSetting.getLcdInfo());
                            eStream.setParam((short) 26, SystemWideActionsListener.this.knuckGestureSetting.getOrientation());
                            IMonitor.sendEvent(eStream);
                            SystemWideActionsListener.this.knuckGestureSetting.setLastReportFSTime(SystemClock.uptimeMillis());
                            break;
                        case 1:
                            String lowerGestureName = msg.obj;
                            if (StylusGestureSettings.STYLUS_GESTURE_S_SUFFIX.equals(lowerGestureName)) {
                                eStream.setParam((short) 10, 1);
                            } else if (StylusGestureSettings.STYLUS_GESTURE_C_SUFFIX.equals(lowerGestureName)) {
                                eStream.setParam((short) 7, 1);
                            } else if ("e".equals(lowerGestureName)) {
                                eStream.setParam((short) 8, 1);
                            } else if (StylusGestureSettings.STYLUS_GESTURE_M_SUFFIX.equals(lowerGestureName)) {
                                eStream.setParam((short) 9, 1);
                            } else if (StylusGestureSettings.STYLUS_GESTURE_W_SUFFIX.equals(lowerGestureName)) {
                                eStream.setParam((short) 11, 1);
                            }
                            eStream.setParam((short) 23, SystemWideActionsListener.this.knuckGestureSetting.getTpVendorName());
                            eStream.setParam((short) 24, SystemWideActionsListener.this.knuckGestureSetting.getAccVendorName());
                            eStream.setParam((short) 25, SystemWideActionsListener.this.knuckGestureSetting.getLcdInfo());
                            eStream.setParam((short) 26, SystemWideActionsListener.this.knuckGestureSetting.getOrientation());
                            IMonitor.sendEvent(eStream);
                            SystemWideActionsListener.this.knuckGestureSetting.setLastReportFSTime(SystemClock.uptimeMillis());
                            break;
                        case 2:
                            eStream.setParam((short) 12, 1);
                            eStream.setParam((short) 23, SystemWideActionsListener.this.knuckGestureSetting.getTpVendorName());
                            eStream.setParam((short) 24, SystemWideActionsListener.this.knuckGestureSetting.getAccVendorName());
                            eStream.setParam((short) 25, SystemWideActionsListener.this.knuckGestureSetting.getLcdInfo());
                            eStream.setParam((short) 26, SystemWideActionsListener.this.knuckGestureSetting.getOrientation());
                            IMonitor.sendEvent(eStream);
                            SystemWideActionsListener.this.knuckGestureSetting.setLastReportFSTime(SystemClock.uptimeMillis());
                            break;
                        case 3:
                            eStream.setParam((short) 1, 1);
                            eStream.setParam((short) 5, SystemWideActionsListener.this.knockGestureDetector.getDoubleKnockTimeInterval());
                            eStream.setParam((short) 23, SystemWideActionsListener.this.knuckGestureSetting.getTpVendorName());
                            eStream.setParam((short) 24, SystemWideActionsListener.this.knuckGestureSetting.getAccVendorName());
                            eStream.setParam((short) 25, SystemWideActionsListener.this.knuckGestureSetting.getLcdInfo());
                            eStream.setParam((short) 26, SystemWideActionsListener.this.knuckGestureSetting.getOrientation());
                            IMonitor.sendEvent(eStream);
                            SystemWideActionsListener.this.knuckGestureSetting.setLastReportFSTime(SystemClock.uptimeMillis());
                            break;
                        case 4:
                            eStream.setParam((short) 0, 1);
                            eStream.setParam((short) 4, SystemWideActionsListener.this.knockGestureDetector.getDoubleKnockTimeInterval());
                            eStream.setParam((short) 6, SystemWideActionsListener.this.knockGestureDetector.getDoubleKnockDisInterval());
                            eStream.setParam((short) 23, SystemWideActionsListener.this.knuckGestureSetting.getTpVendorName());
                            eStream.setParam((short) 24, SystemWideActionsListener.this.knuckGestureSetting.getAccVendorName());
                            eStream.setParam((short) 25, SystemWideActionsListener.this.knuckGestureSetting.getLcdInfo());
                            eStream.setParam((short) 26, SystemWideActionsListener.this.knuckGestureSetting.getOrientation());
                            IMonitor.sendEvent(eStream);
                            SystemWideActionsListener.this.knuckGestureSetting.setLastReportFSTime(SystemClock.uptimeMillis());
                            break;
                    }
                    IMonitor.closeEventStream(eStream);
                }
            }
        };
        this.vibrator = (Vibrator) context.getSystemService("vibrator");
        this.mScreenshotHelper = new ScreenshotHelper(context);
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mHasNotchInScreen = true ^ TextUtils.isEmpty(mNotchProp);
        this.mHwGameObserver = new HwGameObserver(this, null);
        ActivityManagerEx.registerGameObserver(this.mHwGameObserver);
    }

    private boolean isGameForeground(int event) {
        return event == 1 || event == 3 || event == 4;
    }

    private KeyguardManager getKeyguardService() {
        if (this.mKeyguardManager == null) {
            this.mKeyguardManager = (KeyguardManager) this.context.getSystemService("keyguard");
        }
        return this.mKeyguardManager;
    }

    public void cancelSystemWideAction() {
        MotionEvent cancelEvent = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 3, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0);
        this.customGestureDetector.onTouchEvent(cancelEvent);
        if (this.pointerLocationView != null) {
            this.pointerLocationView.onTouchEvent(cancelEvent);
        }
        removePointerLocationView();
        this.hasKnuckleDownOccured = false;
        cancelEvent.recycle();
    }

    public void onPointerEvent(MotionEvent motionEvent) {
        if (!isInValidAction(motionEvent.getAction())) {
            this.knockGestureDetector.onAnyTouchEvent(motionEvent);
            if (shouldProcessMotionEvent(motionEvent)) {
                if (ActivityManagerEx.isGameGestureDisabled()) {
                    Log.i(TAG, "GameGestureDisabled is true,return");
                    return;
                }
                createPointerLocationView();
                if (motionEvent.getAction() == 0) {
                    StatisticalUtils.reportc(this.context, 101);
                    this.onKnockDownRunnable.event = motionEvent;
                    this.handler.post(this.onKnockDownRunnable);
                    if (HWFLOW) {
                        Log.i(TAG, "FingerSense Down Event.");
                    }
                    this.hasKnuckleDownOccured = true;
                    saveScreenBitmap();
                }
                this.knockGestureDetector.onKnuckleTouchEvent(motionEvent);
                if (this.hasKnuckleDownOccured && this.knockGestureDetector.getKnucklePointerCount() < 2) {
                    addPointerLocationView();
                    this.customGestureDetector.onTouchEvent(motionEvent);
                    if (this.pointerLocationView != null) {
                        this.pointerLocationView.onTouchEvent(motionEvent);
                    }
                }
                if (motionEvent.getAction() == 1 || motionEvent.getAction() == 3) {
                    cancelSystemWideAction();
                }
            } else if (this.hasKnuckleDownOccured) {
                cancelSystemWideAction();
            }
        }
    }

    private boolean isInValidAction(int action) {
        return (action == 0 || (action & 255) == 5 || action == 1 || (action & 255) == 6 || action == 3 || action == 2) ? false : true;
    }

    private static int getLazyState(Context context) {
        String str = Global.getString(context.getContentResolver(), "single_hand_mode");
        if (str == null || "".equals(str)) {
            return 0;
        }
        if (str.contains(LEFT)) {
            return 1;
        }
        if (str.contains(RIGHT)) {
            return 2;
        }
        return 0;
    }

    private static boolean isLazyMode(Context context) {
        return getLazyState(context) != 0;
    }

    private static Rect getScreenshotRect(Context context) {
        Display display = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        int state = getLazyState(context);
        if (1 == state) {
            return new Rect(0, (int) (((float) displayMetrics.heightPixels) * 0.25f), (int) (((float) displayMetrics.widthPixels) * 0.75f), displayMetrics.heightPixels);
        }
        if (2 == state) {
            return new Rect((int) (((float) displayMetrics.widthPixels) * 0.25f), (int) (((float) displayMetrics.heightPixels) * 0.25f), displayMetrics.widthPixels, displayMetrics.heightPixels);
        }
        return null;
    }

    private void notifyKnockDown(MotionEvent event) {
        Intent intent = new Intent(ACTION_KNOCK_DOWN);
        intent.addFlags(536870912);
        this.context.sendBroadcast(intent);
    }

    private boolean shouldProcessMotionEvent(MotionEvent motionEvent) {
        int i;
        int my_count = motionEvent.getPointerCount();
        boolean areSystemWideActionsEnabled = false;
        for (i = 0; i < my_count; i++) {
            if (motionEvent.getToolType(i) != 7) {
                return false;
            }
        }
        if (this.FLOG_FLAG) {
            this.FLOG_FLAG = false;
            Flog.i(1503, "Get event type TOOL_TYPE_FINGER_KNUCKLE!");
        }
        i = this.mWindowManagerInternal.getInputMethodWindowVisibleHeight();
        if (i > 0 && motionEvent.getY() > ((float) (this.phoneWindowManager.getRestrictedScreenHeight() - i)) && motionEvent.getAction() == 0) {
            this.isActionOnKeyboard = true;
        }
        if (this.isActionOnKeyboard) {
            if (motionEvent.getAction() == 1) {
                this.isActionOnKeyboard = false;
            }
            return false;
        }
        WindowState windowState = this.phoneWindowManager.getFocusedWindow();
        if (windowState == null || (windowState.getAttrs().flags & 4096) == 0) {
            areSystemWideActionsEnabled = true;
        }
        return areSystemWideActionsEnabled;
    }

    private void saveScreenBitmap() {
        recycleScreenshot();
        Rect sourceCrop = getScreenshotRect(this.context);
        if (isLazyMode(this.context)) {
            this.mScreenshotBitmap = SurfaceControl.screenshot_ext_hw(sourceCrop, this.mDisplayWidth, this.mDisplayHeight, 0, Integer.MAX_VALUE, false, 0);
            return;
        }
        this.mScreenshotBitmap = SurfaceControl.screenshot_ext_hw(new Rect(0, 0, this.mDisplayWidth, this.mDisplayHeight), this.mDisplayWidth, this.mDisplayHeight, 0);
    }

    private void recycleScreenshot() {
        if (this.mScreenshotBitmap != null) {
            this.mScreenshotBitmap.recycle();
            this.mScreenshotBitmap = null;
        }
    }

    private Intent getIntentForCustomGesture(String gestureName, Gesture gesture, double predictionScore) {
        if (FingerSenseSettings.isKnuckleGestureEnable(gestureName, this.context.getContentResolver())) {
            Intent intent = FingerSenseSettings.getIntentForGesture(gestureName, this.context);
            if (intent == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Gesture '");
                stringBuilder.append(gestureName);
                stringBuilder.append("' recognized but application intent was null.");
                Log.w(str, stringBuilder.toString());
                return null;
            }
            intent.addFlags(268435456);
            intent.putExtra(EXTRA_GESTURE, gesture);
            intent.putExtra(EXTRA_GESTURE_NAME, gestureName);
            intent.putExtra(EXTRA_GESTURE_PREDICTION_SCORE, predictionScore);
            return intent;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Gesture '");
        stringBuilder2.append(gestureName);
        stringBuilder2.append("' recognized but no application assigned.");
        Log.w(str2, stringBuilder2.toString());
        return null;
    }

    public void onRegionGesture(String gestureName, Gesture gesture, double predictionScore) {
        Message msg = this.handler.obtainMessage(0);
        if (msg != null) {
            msg.setAsynchronous(true);
            this.handler.sendMessage(msg);
        }
        Intent intent = getIntentForCustomGesture(gestureName, gesture, predictionScore);
        if (intent == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring ");
            stringBuilder.append(gestureName);
            stringBuilder.append(" gesture.");
            Log.d(str, stringBuilder.toString());
            return;
        }
        intent.putExtra(EXTRA_SCREENSHOT_PACKAGENAME, getCurrentPackageName(this.context));
        intent.addFlags(32768);
        Log.d(TAG, "Fingersense Region Gesture");
        StatisticalUtils.reportc(this.context, 104);
        if (this.mScreenshotBitmap == null) {
            Log.w(TAG, "SystemWideActionsListener failed to take Screenshot.");
            return;
        }
        intent.putExtra(EXTRA_SCREENSHOT_BITMAP, this.mScreenshotBitmap.copy(Config.ARGB_8888, false));
        recycleScreenshot();
        dismissKeyguardIfCurrentlyShown();
        intent.setPackage("com.qeexo.smartshot");
        this.context.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
    }

    private String getCurrentPackageName(Context context1) {
        try {
            List<RunningTaskInfo> runningTaskInfos = ((ActivityManager) context1.getSystemService("activity")).getRunningTasks(1);
            if (runningTaskInfos == null || runningTaskInfos.isEmpty()) {
                Log.e(TAG, "running task is null");
                return "";
            }
            RunningTaskInfo runningTaskInfo = (RunningTaskInfo) runningTaskInfos.get(0);
            if (runningTaskInfo == null) {
                Log.e(TAG, "failed to get runningTaskInfo");
                return "";
            }
            String packageName = runningTaskInfo.topActivity.getPackageName();
            if (TextUtils.isEmpty(packageName)) {
                return "";
            }
            return packageName;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get current package name error: ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return "";
        }
    }

    public void onLetterGesture(String gestureName, Gesture gesture, double predictionScore) {
        Message msg = this.handler.obtainMessage(1);
        if (msg != null) {
            msg.obj = gestureName.toLowerCase();
            msg.setAsynchronous(true);
            this.handler.sendMessage(msg);
        }
        StringBuilder stringBuilder;
        if (StylusGestureSettings.STYLUS_GESTURE_S_SUFFIX.equals(gestureName.toLowerCase()) && FingerSenseSettings.isFingerSenseSmartshotEnabled(this.context.getContentResolver())) {
            Intent startIntent = FingerSenseSettings.getIntentForMultiScreenShot(this.context);
            if (startIntent != null) {
                try {
                    List<RecentTaskInfo> recentTask = ActivityManagerNative.getDefault().getRecentTasks(1, 1, ActivityManagerNative.getDefault().getCurrentUser().id).getList();
                    if (recentTask != null && recentTask.size() > 0) {
                        UserInfo user = ((UserManager) this.context.getSystemService("user")).getUserInfo(((RecentTaskInfo) recentTask.get(0)).userId);
                        if (user == null || !user.isManagedProfile()) {
                            this.context.startServiceAsUser(startIntent, ActivityManagerNative.getDefault().getCurrentUser().getUserHandle());
                        } else {
                            this.context.startServiceAsUser(startIntent, user.getUserHandle());
                        }
                    }
                    StatisticalUtils.reportc(this.context, 109);
                    if (HWFLOW) {
                        Log.i(TAG, "Fingersense Letter S recognized, start MultiScreenShotService.");
                    }
                } catch (Exception e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Can not find the service for: ");
                    stringBuilder2.append(e.getMessage());
                    Log.w(str, stringBuilder2.toString());
                }
            } else {
                String str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Letter Gesture '");
                stringBuilder.append(gestureName);
                stringBuilder.append("' recognized but application intent was null.");
                Log.w(str2, stringBuilder.toString());
            }
            return;
        }
        Intent intent = getIntentForCustomGesture(gestureName, gesture, predictionScore);
        String str3;
        if (intent == null) {
            str3 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring ");
            stringBuilder.append(gestureName);
            stringBuilder.append(" gesture.");
            Log.d(str3, stringBuilder.toString());
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("onLetterGesture gesture= ");
        stringBuilder.append(gesture);
        Flog.i(1503, stringBuilder.toString());
        intent.addFlags(536870912);
        if (!gestureName.toLowerCase().equals(StylusGestureSettings.STYLUS_GESTURE_S_SUFFIX)) {
            str3 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Fingersense Letter Gesture ");
            stringBuilder.append(gestureName);
            Log.d(str3, stringBuilder.toString());
            StatisticalUtils.reportc(this.context, 105);
        }
        KeyguardManager keyguardManager = getKeyguardService();
        Context context;
        StringBuilder stringBuilder3;
        if (intent.getComponent() != null && "com.huawei.camera".equals(intent.getComponent().getPackageName()) && keyguardManager != null && keyguardManager.isKeyguardLocked() && keyguardManager.isKeyguardSecure()) {
            intent = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE").setPackage("com.huawei.camera").addFlags(8388608);
            Log.d(TAG, "Keyguard is locked, starting secured camera.");
            this.context.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
            context = this.context;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("{letter:");
            stringBuilder3.append(gestureName);
            stringBuilder3.append(",pkg:null}");
            StatisticalUtils.reporte(context, 110, stringBuilder3.toString());
            return;
        }
        this.context.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
        context = this.context;
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("{letter:");
        stringBuilder3.append(gestureName);
        stringBuilder3.append(",pkg:");
        stringBuilder3.append(intent.getComponent().getPackageName());
        stringBuilder3.append("}");
        StatisticalUtils.reporte(context, 110, stringBuilder3.toString());
        this.handler.postDelayed(new Runnable() {
            public void run() {
                SystemWideActionsListener.this.dismissKeyguardIfCurrentlyShown();
            }
        }, 500);
    }

    public void onLineGesture(String gestureName, Gesture gesture, double predictionScore) {
        Message msg = this.handler.obtainMessage(2);
        if (msg != null) {
            msg.setAsynchronous(true);
            this.handler.sendMessage(msg);
        }
        String str;
        if (FingerSenseSettings.isFingerSenseLineGestureEnabled(this.context.getContentResolver())) {
            float orientedDistance;
            int navBarHeight;
            String str2 = gestureName;
            Log.d(TAG, "Fingersense Line Gesture");
            StatisticalUtils.reportc(this.context, 102);
            RectF boundingBox = gesture.getBoundingBox();
            if (this.context.getResources().getConfiguration().orientation == 2) {
                orientedDistance = boundingBox.centerX();
                navBarHeight = this.mNavBarLandscapeHeight;
            } else {
                orientedDistance = boundingBox.centerY();
                navBarHeight = this.mNavBarPortraitHeight;
            }
            float orientedDistance2 = orientedDistance;
            int navBarHeight2 = navBarHeight;
            if (!this.phoneWindowManager.isNavigationBarVisible()) {
                navBarHeight2 = 0;
            }
            int availableScreenSize = this.mDisplayHeight - navBarHeight2;
            int minWindowSize = (availableScreenSize * 10) / 100;
            if (orientedDistance2 <= ((float) minWindowSize) || orientedDistance2 >= ((float) (availableScreenSize - minWindowSize)) || DISABLE_MULTIWIN) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Line Gesture recognized, but too close to screen edge or have disable multiwindow ");
                stringBuilder.append(DISABLE_MULTIWIN);
                Log.w(str, stringBuilder.toString());
            } else {
                String str3;
                StringBuilder stringBuilder2;
                KeyguardManager keyguardManager = getKeyguardService();
                str = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("onLineGesture keyguardManager ");
                if (keyguardManager == null) {
                    str3 = "null";
                } else {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("locked ");
                    stringBuilder2.append(keyguardManager.isKeyguardLocked());
                    str3 = stringBuilder2.toString();
                }
                stringBuilder3.append(str3);
                Log.d(str, stringBuilder3.toString());
                if (!(keyguardManager == null || keyguardManager.isKeyguardLocked())) {
                    String str4;
                    try {
                        navBarHeight2 = WindowManagerGlobal.getWindowManagerService().getDockedStackSide();
                        str4 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("onLineGesture dockSide ");
                        stringBuilder2.append(navBarHeight2);
                        stringBuilder2.append(" DOCKED_INVALID ");
                        stringBuilder2.append(-1);
                        Log.d(str4, stringBuilder2.toString());
                        if (navBarHeight2 == -1) {
                            toggleSplitScreenByLineGesture(107, "toggleSplitScreenByLineGesture", (int) boundingBox.centerX(), (int) boundingBox.centerY());
                        }
                    } catch (RemoteException e) {
                        str4 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Failed to get dock side: ");
                        stringBuilder2.append(e);
                        Log.w(str4, stringBuilder2.toString());
                    }
                }
            }
            return;
        }
        str = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append(gestureName);
        stringBuilder4.append(" gesture disabled");
        Log.w(str, stringBuilder4.toString());
    }

    public void onDoubleKnuckleDoubleKnock(String gestureName, MotionEvent event) {
        Message msg = this.handler.obtainMessage(3);
        if (msg != null) {
            msg.setAsynchronous(true);
            this.handler.sendMessage(msg);
        }
        if (FingerSenseSettings.isFingerSenseSmartshotEnabled(this.context.getContentResolver())) {
            KeyguardManager keyguardManager = getKeyguardService();
            if (keyguardManager == null || !keyguardManager.inKeyguardRestrictedInputMode()) {
                Flog.i(1503, "FingerSense Double Knuckle Double Knock");
                StatisticalUtils.reportc(this.context, 107);
                this.startServiceIntent = new Intent();
                this.startServiceIntent.setAction("com.huawei.screenrecorder.Start");
                this.startServiceIntent.setClassName("com.huawei.screenrecorder", "com.huawei.screenrecorder.ScreenRecordService");
                try {
                    this.context.startServiceAsUser(this.startServiceIntent, UserHandle.CURRENT_OR_SELF);
                } catch (IllegalStateException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to start service: ");
                    stringBuilder.append(e);
                    Log.w(str, stringBuilder.toString());
                }
                return;
            }
            Flog.w(1503, "On lock screen SmartShot Disabled!");
            return;
        }
        Flog.w(1503, "FingerSense SmartShot Disabled!");
    }

    public void onSingleKnuckleDoubleKnock(String gestureName, MotionEvent event) {
        Message msg = this.handler.obtainMessage(4);
        if (msg != null) {
            msg.setAsynchronous(true);
            this.handler.sendMessage(msg);
        }
        if (!FingerSenseSettings.isFingerSenseSmartshotEnabled(this.context.getContentResolver())) {
            Flog.i(1503, "FingerSense SmartShot Disabled!");
        } else if (HwFrameworkFactory.getCoverManager().isCoverOpen()) {
            Flog.i(1503, "FingerSense Single Knuckle Double Knock");
            StatisticalUtils.reportc(this.context, 103);
            while (this.pointerLocationView.isShown()) {
                removePointerLocationView();
                Flog.i(1503, "pointerLocationView isShown(), removing...");
            }
            if (HWFLOW) {
                Flog.i(1503, "double_knock ScreenShot enter.");
            }
            sendScreenshotNotification();
            this.mScreenshotHelper.takeScreenshot(1, true, true, this.handler);
        } else {
            Flog.i(1503, "Cover is closed.");
        }
    }

    private void sendScreenshotNotification() {
        Intent screenshotIntent = new Intent("com.huawei.recsys.action.RECEIVE_EVENT");
        screenshotIntent.putExtra("eventOperator", "sysScreenShot");
        screenshotIntent.putExtra("eventItem", "double_knock");
        screenshotIntent.setPackage("com.huawei.recsys");
        this.context.sendBroadcastAsUser(screenshotIntent, UserHandle.CURRENT, "com.huawei.tips.permission.SHOW_TIPS");
    }

    public void onDoubleKnocksNotYetConfirmed(String gestureName, MotionEvent event) {
        cancelSystemWideAction();
    }

    public void createPointerLocationView() {
        if (this.pointerLocationView == null) {
            this.pointerLocationView = new PointerLocationView(this.context, this);
            this.layoutParams = new LayoutParams(-1, -1);
            this.layoutParams.type = HwArbitrationDEFS.MSG_MPLINK_STOP_COEX_SUCC;
            this.layoutParams.flags = 1304;
            LayoutParams layoutParams = this.layoutParams;
            layoutParams.privateFlags |= 16;
            if (ActivityManager.isHighEndGfx()) {
                layoutParams = this.layoutParams;
                layoutParams.flags |= 16777216;
                layoutParams = this.layoutParams;
                layoutParams.privateFlags |= 2;
            }
            this.layoutParams.format = -3;
            this.layoutParams.setTitle("KnucklePointerLocationView");
            layoutParams = this.layoutParams;
            layoutParams.inputFeatures |= 2;
            if (this.mHasNotchInScreen) {
                this.layoutParams.layoutInDisplayCutoutMode = 1;
            }
        }
    }

    private void addPointerLocationView() {
        boolean isVRMode = false;
        if (HwFrameworkFactory.getVRSystemServiceManager() != null) {
            isVRMode = HwFrameworkFactory.getVRSystemServiceManager().isVRMode();
        }
        if (isVRMode) {
            Flog.i(1503, "current is in VR Mode,view cannot added!");
            return;
        }
        synchronized (this.viewLock) {
            if (!(this.viewAdd || this.pointerLocationView == null)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("addPointerLocationView layoutParams flags= ");
                stringBuilder.append(Integer.toHexString(this.layoutParams.flags));
                stringBuilder.append(" privateFlags= ");
                stringBuilder.append(Integer.toHexString(this.layoutParams.privateFlags));
                stringBuilder.append(" type= ");
                stringBuilder.append(Integer.toHexString(this.layoutParams.type));
                stringBuilder.append(" inputFeatures= ");
                stringBuilder.append(Integer.toHexString(this.layoutParams.inputFeatures));
                Flog.i(1503, stringBuilder.toString());
                this.windowManager.addView(this.pointerLocationView, this.layoutParams);
                this.viewAdd = true;
            }
        }
    }

    public void destroyPointerLocationView() {
        if (this.pointerLocationView != null) {
            removePointerLocationView();
            this.pointerLocationView = null;
        }
    }

    private void removePointerLocationView() {
        synchronized (this.viewLock) {
            if (this.viewAdd && this.pointerLocationView != null) {
                this.windowManager.removeView(this.pointerLocationView);
                this.viewAdd = false;
            }
        }
    }

    private void toast(String message, boolean vibrate) {
        Toast.makeText(this.context, message, 0).show();
        if (vibrate) {
            vibrate(new long[]{0, 20, 100, 20});
        }
    }

    private void vibrate(long[] pattern) {
        this.vibrator.vibrate(pattern, -1);
    }

    private void dismissKeyguardIfCurrentlyShown() {
        KeyguardManager keyguardManager = getKeyguardService();
        if (keyguardManager != null && keyguardManager.inKeyguardRestrictedInputMode()) {
            this.phoneWindowManager.dismissKeyguardLw(null, null);
        }
    }

    private String getAppName(String packageName) {
        PackageManager pm = this.context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = pm.getApplicationInfo(packageName, 0);
        } catch (NameNotFoundException e) {
        }
        return (String) (applicationInfo != null ? pm.getApplicationLabel(applicationInfo) : packageName);
    }

    public void updateConfiguration() {
        Display display = this.windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        this.mDisplayWidth = size.x > size.y ? size.y : size.x;
        this.mDisplayHeight = size.x > size.y ? size.x : size.y;
    }

    private void toggleSplitScreenByLineGesture(int code, String transactName, int param1, int param2) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            if (getHWStatusBarService() != null) {
                IBinder statusBarServiceBinder = getHWStatusBarService().asBinder();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("statusBarServiceBinder ");
                stringBuilder.append(statusBarServiceBinder != null);
                Log.d(str, stringBuilder.toString());
                if (statusBarServiceBinder != null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to status bar service");
                    Log.d(str, stringBuilder.toString());
                    data.writeInterfaceToken("com.android.internal.statusbar.IStatusBarService");
                    data.writeInt(param1);
                    data.writeInt(param2);
                    statusBarServiceBinder.transact(code, data, reply, 0);
                }
            }
        } catch (RemoteException localRemoteException) {
            localRemoteException.printStackTrace();
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
        }
        reply.recycle();
        data.recycle();
    }
}
