package com.android.server.policy;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import com.android.server.gesture.GestureNavPolicy;
import com.android.server.hidata.appqoe.HwAPPQoEUserAction;
import com.android.server.policy.WindowManagerPolicy;
import com.huawei.hms.jos.dock.IFingerService;

public class HwGameDockGesture {
    private static final int BUOYGESTURE_MOVE_TIME_THRESHOLD = 120;
    private static final float BUOY_MIN_DIS_MULTIPLE = 2.0f;
    private static final int BUOY_POSITION_LEFT = 1;
    private static final int BUOY_POSITION_RIGHT = 2;
    private static final int BUOY_REGION_DIR_LEFT = 1;
    private static final int BUOY_REGION_DIR_NONE = 0;
    private static final int BUOY_REGION_DIR_RIGHT = 2;
    private static final float BUOY_START_CHECK_DIS_MULTIPLE = 1.2f;
    private static final int DEFAULT_BUOY_POSITION = 1;
    private static final int FINGERSERVICE_EVENTTYPE_CANCEL = 3;
    private static final int FINGERSERVICE_EVENTTYPE_MOVE = 1;
    private static final int FINGERSERVICE_EVENTTYPE_NONE = 0;
    private static final int FINGERSERVICE_EVENTTYPE_UP = 2;
    private static final float HEIGHT_RATIO = 0.25f;
    private static final String HUAWEI_FINGERSER_ACTION = "com.huawei.hwid.gameservice.fingerservice";
    private static final String HUAWEI_FINGERSER_PKG = "com.huawei.hwid";
    private static final boolean IS_DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final boolean IS_NOTCH_CFG = (!SystemProperties.get("ro.config.hw_notch_size", "").equals(""));
    private static final boolean IS_SUPPORT_GAMEASS_FULL;
    private static final String KEY_GAME_BUOY = "game_buoy";
    private static final String KEY_GAME_BUOY_ISCHECKTIMEOUT = "game_buoy_check_timeout";
    private static final String KEY_GAME_BUOY_POSITION = "game_buoy_position";
    private static final int ROTATION_RADIX = 90;
    private static final int SLIDE_OUT_MAX_ANGLE = 70;
    private static final int STRAIGHT_ANGLE = 180;
    private static final String TAG = "HwGameDockGesture";
    private static final float VERTICAL_ANGLE = 90.0f;
    private int mBuoyConfigPosition = 1;
    /* access modifiers changed from: private */
    public float mBuoyCurrRawX;
    /* access modifiers changed from: private */
    public float mBuoyCurrRawY;
    private float mBuoyDownRawX;
    private float mBuoyDownRawY;
    private Context mContext;
    private int mCurRotation = (SystemProperties.getInt("ro.panel.hw_orientation", 0) / ROTATION_RADIX);
    private int mCurUser;
    private Point mDisplaySize = new Point();
    private long mDownTime;
    /* access modifiers changed from: private */
    public int mEventType;
    /* access modifiers changed from: private */
    public IFingerService mFingerIntf = null;
    /* access modifiers changed from: private */
    public IBinder.DeathRecipient mFingleSerBinderDieListener = new IBinder.DeathRecipient() {
        /* class com.android.server.policy.HwGameDockGesture.AnonymousClass1 */

        public void binderDied() {
            IFingerService unused = HwGameDockGesture.this.mFingerIntf = null;
        }
    };
    private final ServiceConnection mFingleSerConn = new ServiceConnection() {
        /* class com.android.server.policy.HwGameDockGesture.AnonymousClass2 */

        public void onServiceConnected(ComponentName name, IBinder service) {
            IFingerService unused = HwGameDockGesture.this.mFingerIntf = IFingerService.Stub.asInterface(service);
            if (HwGameDockGesture.this.mFingerIntf == null) {
                Log.e(HwGameDockGesture.TAG, "Connect IFingerIntf null object");
                return;
            }
            try {
                service.linkToDeath(HwGameDockGesture.this.mFingleSerBinderDieListener, 0);
            } catch (RemoteException ex) {
                Log.e(HwGameDockGesture.TAG, "Lost connection to FingleSer", ex);
            }
            HwGameDockGesture hwGameDockGesture = HwGameDockGesture.this;
            hwGameDockGesture.notifyFingerServicewhenMoveOrUp(hwGameDockGesture.mEventType, HwGameDockGesture.this.mIsPointInRect, HwGameDockGesture.this.mBuoyCurrRawX, HwGameDockGesture.this.mBuoyCurrRawY);
            Log.i(HwGameDockGesture.TAG, "IFingerIntf not null, success " + HwGameDockGesture.this.mEventType);
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.i(HwGameDockGesture.TAG, "Finger Service disconnected");
            IFingerService unused = HwGameDockGesture.this.mFingerIntf = null;
        }
    };
    private GameDockMotionListener mGameDockListener = null;
    private WindowManager mGameDockWindowManager;
    private ContentObserver mGameDoclObserver;
    private GestureNavPolicy mGestureNavPolicy;
    private int mHoleHeight;
    private boolean mIsBuoyCheckTimeout = false;
    private boolean mIsBuoyEnable = true;
    private boolean mIsBuoyGestureFail = false;
    private boolean mIsFocusWindowUseNotch = true;
    private boolean mIsNotchSwitchOn = false;
    private boolean mIsNotifiedFingerSer = false;
    /* access modifiers changed from: private */
    public int mIsPointInRect;
    private int mLastNotifyRawX;
    private int mLastNotifyRawY;
    private Rect mLeftRect = new Rect();
    private final Object mLock = new Object();
    private int mRegionWidth;
    private ContentResolver mResolver;
    private Rect mRightRect = new Rect();
    private int mSlideOutThreshold;
    private int mSlideUpThreshold;
    private WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;

    static {
        boolean z = false;
        if (SystemProperties.getInt("ro.config.gameassist.full-finger", 0) == 1) {
            z = true;
        }
        IS_SUPPORT_GAMEASS_FULL = z;
    }

    public class GameDockMotionListener implements WindowManagerPolicyConstants.PointerEventListener {
        public GameDockMotionListener() {
        }

        public void onPointerEvent(MotionEvent motionEvent) {
            HwGameDockGesture.this.handleBuoyMotion(motionEvent);
        }
    }

    public HwGameDockGesture(Context context) {
        this.mResolver = context.getContentResolver();
        this.mContext = context;
        this.mGameDockWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mCurUser = ActivityManager.getCurrentUser();
        this.mGameDoclObserver = new ContentObserver(new Handler()) {
            /* class com.android.server.policy.HwGameDockGesture.AnonymousClass3 */

            public void onChange(boolean isChange) {
                HwGameDockGesture.this.updateGameDockStat();
            }
        };
        registerGameDockStat(this.mCurUser);
        synchronized (this.mLock) {
            updateBuoyRectLocked();
        }
    }

    public void systemReadyAndInit(WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs, GestureNavPolicy gestureNavPolicy) {
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mGestureNavPolicy = gestureNavPolicy;
    }

    public void enableGameDockGesture(boolean enable) {
        if (IS_DEBUG) {
            Log.i(TAG, "enableGameDockGesture enable:" + enable);
        }
        if (this.mWindowManagerFuncs == null) {
            Log.i(TAG, "system not ready, mWindowManagerFuncs is null");
        } else if (!enable || !this.mIsBuoyEnable) {
            GameDockMotionListener gameDockMotionListener = this.mGameDockListener;
            if (gameDockMotionListener != null) {
                this.mWindowManagerFuncs.unregisterPointerEventListener(gameDockMotionListener, 0);
                updateGestureNavBackRegion(false);
                this.mGameDockListener = null;
            }
        } else if (this.mGameDockListener == null) {
            this.mGameDockListener = new GameDockMotionListener();
            this.mWindowManagerFuncs.registerPointerEventListener(this.mGameDockListener, 0);
            updateGestureNavBackRegion(true);
        }
    }

    public int getShrinkIdByDockPosition() {
        if (!this.mIsBuoyEnable || this.mGameDockListener == null) {
            return 0;
        }
        int i = this.mBuoyConfigPosition;
        if (i == 1) {
            return 1;
        }
        if (i == 2) {
            return 2;
        }
        return 0;
    }

    public static boolean isGameDockGestureFeatureOn() {
        return IS_SUPPORT_GAMEASS_FULL;
    }

    /* access modifiers changed from: private */
    public void updateGameDockStat() {
        boolean z = false;
        this.mIsBuoyEnable = Settings.Secure.getIntForUser(this.mResolver, KEY_GAME_BUOY, 1, ActivityManager.getCurrentUser()) == 1;
        this.mBuoyConfigPosition = Settings.Secure.getIntForUser(this.mResolver, KEY_GAME_BUOY_POSITION, 1, ActivityManager.getCurrentUser());
        this.mIsBuoyCheckTimeout = Settings.Secure.getIntForUser(this.mResolver, KEY_GAME_BUOY_ISCHECKTIMEOUT, 0, ActivityManager.getCurrentUser()) == 1;
        if (Settings.Secure.getIntForUser(this.mResolver, "display_notch_status", 0, this.mCurUser) == 1) {
            z = true;
        }
        this.mIsNotchSwitchOn = z;
        Slog.i(TAG, "mIsBuoyEnable:" + this.mIsBuoyEnable + " mIsBuoyCheckTimeout " + this.mIsBuoyCheckTimeout + " mIsNotchSwitchOn " + this.mIsNotchSwitchOn + ", mBuoyConfigPosition:" + this.mBuoyConfigPosition);
    }

    private void registerGameDockStat(int userId) {
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor(KEY_GAME_BUOY), false, this.mGameDoclObserver, userId);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor(KEY_GAME_BUOY_POSITION), false, this.mGameDoclObserver, userId);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor(KEY_GAME_BUOY_ISCHECKTIMEOUT), false, this.mGameDoclObserver, userId);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("display_notch_status"), false, this.mGameDoclObserver, userId);
        updateGameDockStat();
    }

    public void setCurrentUser(int userId, int[] currentProfileIds) {
        this.mCurUser = userId;
        registerGameDockStat(this.mCurUser);
    }

    public void updateOnRotationChange(int rotation) {
        if (IS_DEBUG) {
            Log.i(TAG, "rotation " + rotation + " mCurRotation " + this.mCurRotation);
        }
        if (rotation != this.mCurRotation) {
            this.mCurRotation = rotation;
            synchronized (this.mLock) {
                updateBuoyRectLocked();
            }
        }
    }

    public void updateOnNotchSwitchChange() {
        synchronized (this.mLock) {
            updateBuoyRectLocked();
        }
    }

    public void updateOnConfigurationChange() {
        synchronized (this.mLock) {
            updateBuoyRectLocked();
        }
    }

    private static int getTpWindowWidth(Context context) {
        return context.getResources().getDimensionPixelSize(34472512);
    }

    public void updateOnFocusChange(WindowManagerPolicy.WindowState newFocus) {
        synchronized (this.mLock) {
            if (newFocus != null) {
                if (newFocus.getAttrs() != null) {
                    String packageName = newFocus.getOwningPackage();
                    int uid = newFocus.getOwningUid();
                    String focusWindowTitle = newFocus.getAttrs().getTitle().toString();
                    this.mIsFocusWindowUseNotch = IS_NOTCH_CFG ? newFocus.isWindowUsingNotch() : true;
                    if (IS_DEBUG) {
                        Log.i(TAG, "Focus:" + focusWindowTitle + ", Uid=" + uid + ", UN=" + newFocus.isWindowUsingNotch() + ", LUN=" + this.mIsFocusWindowUseNotch + ", pkg:" + packageName);
                    }
                    updateBuoyRectLocked();
                }
            }
        }
    }

    private void updateBuoyRectLocked() {
        if (this.mGameDockWindowManager != null) {
            this.mRegionWidth = getTpWindowWidth(this.mContext);
            int i = this.mRegionWidth;
            this.mSlideOutThreshold = (int) (((float) i) * BUOY_START_CHECK_DIS_MULTIPLE);
            this.mSlideUpThreshold = (int) (((float) i) * 2.0f);
            this.mGameDockWindowManager.getDefaultDisplay().getRealSize(this.mDisplaySize);
            int displayWidth = this.mDisplaySize.x;
            int displayHeight = this.mDisplaySize.y;
            boolean isUsingNotch = !this.mIsNotchSwitchOn && this.mIsFocusWindowUseNotch;
            this.mHoleHeight = this.mContext.getResources().getDimensionPixelSize(17105443);
            if (IS_DEBUG) {
                Log.i(TAG, "updateBuoyRectLocked IS_NOTCH_CFG: " + IS_NOTCH_CFG + " UsingNotch " + isUsingNotch + " mCurRotation " + this.mCurRotation + " mHoleHeight " + this.mHoleHeight + " displayWidth " + displayWidth + " displayHeight " + displayHeight + " SlideOut " + this.mSlideOutThreshold + " SlideUp " + this.mSlideUpThreshold);
            }
            int leftOffset = 0;
            int rightOffset = 0;
            if (IS_NOTCH_CFG && !isUsingNotch) {
                int i2 = this.mCurRotation;
                if (i2 == 1) {
                    leftOffset = this.mHoleHeight;
                    rightOffset = 0;
                } else if (i2 == 3) {
                    leftOffset = 0;
                    rightOffset = this.mHoleHeight;
                }
            }
            updateBuoyRect(leftOffset, rightOffset, displayWidth, displayHeight);
        } else if (IS_DEBUG) {
            Log.i(TAG, "BuoyRect mBuoyWindowManager is null ");
        }
    }

    private void updateBuoyRect(int leftOffset, int rightOffset, int width, int height) {
        if (IS_DEBUG) {
            Log.i(TAG, "BuoyRect width: " + width + " height " + height + " lOffset " + leftOffset + " rOffset " + rightOffset + " regionWidth " + this.mRegionWidth);
        }
        this.mLeftRect.set(leftOffset, 0, this.mRegionWidth + leftOffset, (int) (((float) height) * HEIGHT_RATIO));
        this.mRightRect.set((width - rightOffset) - this.mRegionWidth, 0, width - rightOffset, (int) (((float) height) * HEIGHT_RATIO));
    }

    private void isDownPointInRect(MotionEvent event) {
        this.mBuoyDownRawX = event.getRawX();
        this.mBuoyDownRawY = event.getRawY();
        if (this.mLeftRect.contains((int) this.mBuoyDownRawX, (int) this.mBuoyDownRawY)) {
            this.mIsPointInRect = 1;
        } else if (this.mRightRect.contains((int) this.mBuoyDownRawX, (int) this.mBuoyDownRawY)) {
            this.mIsPointInRect = 2;
        } else {
            this.mIsPointInRect = 0;
            this.mIsBuoyGestureFail = true;
        }
        if (IS_DEBUG) {
            Log.i(TAG, "BuoyRect mBuoyDownRawX: " + this.mBuoyDownRawX + " mBuoyDownRawY " + this.mBuoyDownRawY + " Left " + this.mLeftRect.toString() + ", Right " + this.mRightRect.toString() + " mIsBuoyGestureFail " + this.mIsBuoyGestureFail);
        }
    }

    private void clearBuoy() {
        this.mIsPointInRect = 0;
        this.mIsBuoyGestureFail = false;
        this.mEventType = 0;
        this.mIsNotifiedFingerSer = false;
    }

    private void handleBuoyDown(MotionEvent event) {
        clearBuoy();
        this.mDownTime = event.getEventTime();
        isDownPointInRect(event);
    }

    private boolean isPointInRegion(int x, int y) {
        if ((this.mIsPointInRect != 1 || !this.mLeftRect.contains(x, y)) && (this.mIsPointInRect != 2 || !this.mRightRect.contains(x, y))) {
            return false;
        }
        return true;
    }

    private void handleBuoyMove(MotionEvent event) {
        this.mEventType = 1;
        this.mBuoyCurrRawX = event.getRawX();
        this.mBuoyCurrRawY = event.getRawY();
        if (this.mIsNotifiedFingerSer) {
            notifyFingerServicewhenMoveOrUp(this.mEventType, this.mIsPointInRect, this.mBuoyCurrRawX, this.mBuoyCurrRawY);
        }
        if (!this.mIsBuoyGestureFail) {
            if (Math.abs(this.mBuoyCurrRawX - this.mBuoyDownRawX) <= ((float) this.mSlideOutThreshold) || isAngleLegal()) {
                long durationTime = event.getEventTime() - this.mDownTime;
                if (this.mIsBuoyCheckTimeout && durationTime > 120 && isPointInRegion((int) this.mBuoyCurrRawX, (int) this.mBuoyCurrRawY)) {
                    if (IS_DEBUG) {
                        Log.i(TAG, "BuoyMove Gesture Fail timeout " + durationTime + " curTime " + event.getEventTime() + " downTime " + this.mDownTime + " mBuoyCurrRawX " + this.mBuoyCurrRawX + " mBuoyCurrRawY " + this.mBuoyCurrRawY);
                    }
                    this.mIsBuoyGestureFail = true;
                } else if (!this.mIsBuoyGestureFail && !this.mIsNotifiedFingerSer && isBuoyGestureEffect()) {
                    this.mIsBuoyGestureFail = false;
                    this.mIsNotifiedFingerSer = true;
                    notifyFingerServicewhenMoveOrUp(this.mEventType, this.mIsPointInRect, this.mBuoyCurrRawX, this.mBuoyCurrRawY);
                }
            } else {
                if (IS_DEBUG) {
                    Log.i(TAG, "BuoyMove Gesture Fail angle too large" + this.mSlideOutThreshold);
                }
                this.mIsBuoyGestureFail = true;
            }
        }
    }

    private void handleBuoyCancel(MotionEvent event) {
        if (this.mIsNotifiedFingerSer) {
            this.mEventType = 3;
            this.mBuoyCurrRawX = event.getRawX();
            this.mBuoyCurrRawY = event.getRawY();
            notifyFingerServicewhenMoveOrUp(this.mEventType, this.mIsPointInRect, this.mBuoyCurrRawX, this.mBuoyCurrRawY);
        }
        clearBuoy();
    }

    private void handleBuoyUp(MotionEvent event) {
        if (this.mIsNotifiedFingerSer) {
            this.mBuoyCurrRawX = event.getRawX();
            this.mBuoyCurrRawY = event.getRawY();
            this.mEventType = 2;
            notifyFingerServicewhenMoveOrUp(this.mEventType, this.mIsPointInRect, this.mBuoyCurrRawX, this.mBuoyCurrRawY);
        }
        clearBuoy();
    }

    private void handleBuoyPointDown(MotionEvent event) {
        if (this.mIsNotifiedFingerSer) {
            this.mBuoyCurrRawX = event.getRawX();
            this.mBuoyCurrRawY = event.getRawY();
            this.mEventType = 3;
            notifyFingerServicewhenMoveOrUp(this.mEventType, this.mIsPointInRect, this.mBuoyCurrRawX, this.mBuoyCurrRawY);
        }
        clearBuoy();
    }

    private void handleBuoyPointUp(MotionEvent event) {
        clearBuoy();
    }

    private double angleCaculate(float eventX, float eventY) {
        if (eventX == 0.0f) {
            return 90.0d;
        }
        return (Math.atan((double) (eventY / eventX)) / 3.141592653589793d) * 180.0d;
    }

    private boolean isAngleLegal() {
        if (angleCaculate(Math.abs(this.mBuoyCurrRawX - this.mBuoyDownRawX), Math.abs(this.mBuoyCurrRawY - this.mBuoyDownRawY)) > 70.0d) {
            return false;
        }
        return true;
    }

    private boolean isLeftBuoyEffect() {
        if (this.mBuoyCurrRawX - this.mBuoyDownRawX < ((float) this.mSlideOutThreshold)) {
            return false;
        }
        return true;
    }

    private boolean isRightBuoyEffect() {
        if (this.mBuoyDownRawX - this.mBuoyCurrRawX < ((float) this.mSlideOutThreshold)) {
            return false;
        }
        return true;
    }

    private boolean isBuoyGestureEffect() {
        if ((this.mIsPointInRect != 1 || !isLeftBuoyEffect()) && (this.mIsPointInRect != 2 || !isRightBuoyEffect())) {
            return false;
        }
        if (IS_DEBUG) {
            Log.i(TAG, "BuoyMove effect, dir:" + this.mIsPointInRect);
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void handleBuoyMotion(MotionEvent event) {
        if (this.mIsBuoyEnable) {
            int action = event.getActionMasked();
            if (action == 0) {
                handleBuoyDown(event);
            } else if (action == 1) {
                handleBuoyUp(event);
            } else if (action == 2) {
                handleBuoyMove(event);
            } else if (action == 3) {
                handleBuoyCancel(event);
            } else if (action == 5) {
                handleBuoyPointDown(event);
            } else if (action == 6) {
                handleBuoyPointUp(event);
            }
        }
    }

    private void bindFingerService() {
        if (this.mFingerIntf == null) {
            Intent intent = new Intent(HUAWEI_FINGERSER_ACTION);
            intent.setPackage(HUAWEI_FINGERSER_PKG);
            try {
                Context context = this.mContext;
                ServiceConnection serviceConnection = this.mFingleSerConn;
                Context context2 = this.mContext;
                context.bindServiceAsUser(intent, serviceConnection, 1, UserHandle.CURRENT);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Notify Finger service bind IllegalArgumentException");
            } catch (SecurityException e2) {
                Log.e(TAG, "Notify Finger service bind SecurityException");
            }
        }
    }

    private String fingerServiceEventTypeTrans(int eventType) {
        if (eventType == 0) {
            return HwAPPQoEUserAction.DEFAULT_CHIP_TYPE;
        }
        if (eventType == 1) {
            return "MOVE";
        }
        if (eventType == 2) {
            return "UP";
        }
        if (eventType != 3) {
            return HwAPPQoEUserAction.DEFAULT_CHIP_TYPE;
        }
        return "CANCEL";
    }

    /* access modifiers changed from: private */
    public void notifyFingerServicewhenMoveOrUp(int eventType, int dir, float rawX, float rawY) {
        if (eventType != 1 || this.mLastNotifyRawX != ((int) rawX) || this.mLastNotifyRawY != ((int) rawY)) {
            bindFingerService();
            if (this.mFingerIntf != null) {
                String eventAct = fingerServiceEventTypeTrans(eventType);
                if (eventType == 1) {
                    this.mLastNotifyRawX = (int) rawX;
                    this.mLastNotifyRawY = (int) rawY;
                }
                try {
                    this.mFingerIntf.fingerAction(dir, eventAct, (int) rawX, (int) rawY);
                } catch (RemoteException e) {
                    Log.e(TAG, "IFingerIntf notify err: " + eventAct);
                }
                if (IS_DEBUG) {
                    Log.i(TAG, "send fingerAction " + eventAct + " dir " + dir + " rawX " + rawX + " rawY " + rawY);
                }
            }
        }
    }

    private void updateGestureNavBackRegion(boolean shrink) {
        GestureNavPolicy gestureNavPolicy = this.mGestureNavPolicy;
        if (gestureNavPolicy != null) {
            gestureNavPolicy.updateGestureNavRegion(shrink, getShrinkIdByDockPosition());
        }
    }
}
