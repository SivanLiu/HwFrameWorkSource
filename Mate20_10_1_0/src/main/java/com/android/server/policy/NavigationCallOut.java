package com.android.server.policy;

import android.app.ActivityManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import android.widget.Toast;
import com.android.server.gesture.GestureNavConst;
import com.android.server.gesture.GestureUtils;
import com.android.server.gesture.HwGestureNavWhiteConfig;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.policy.WindowManagerPolicy;
import com.huawei.android.fsm.HwFoldScreenManager;
import com.huawei.controlcenter.ui.service.IControlCenterGesture;

public class NavigationCallOut {
    private static final String AUTHORITY = "com.huawei.controlcenter.SwitchProvider";
    private static final Uri AUTHORITY_URI = Uri.parse("content://com.huawei.controlcenter.SwitchProvider");
    private static final String CTRLCENTER_ACTION = "com.huawei.controlcenter.action.CONTROL_CENTER_GESTURE";
    private static final String CTRLCENTER_PKG = "com.huawei.controlcenter";
    private static final boolean DEBUG = false;
    private static final int DEGREE_RAGE = 1;
    private static final String ISCONTROLCENTERENABLE = "isControlCenterEnable";
    private static final String ISSWITCHON = "isSwitchOn";
    private static final boolean IS_FOLD_SCREEN_DEVICE = HwFoldScreenManager.isFoldable();
    private static final String KEY_ENABLE_NAVBAR_DB = "enable_navbar";
    private static final double LEFT_RANGE = 0.2d;
    private static final double LEFT_RANGE_LAND = 0.125d;
    private static final String METHOD_CHECK_CONTROL_CENTER_ENABLE = "getControlCenterState";
    private static final String METHOD_CHECK_SWITCH = "checkControlSwith";
    private static final double NAV_START_PART = 0.28d;
    private static final int NOTCH_HEIGHT = 1;
    private static final int NOTCH_NUMS = 4;
    private static final double RIGHT_RANGE = 0.8d;
    private static final double RIGHT_RANGE_LAND = 0.875d;
    private static final String TAG = "NavigationCallOut";
    private static final String URI_CONTROLCENTER_SWITCH = "content://com.huawei.controlcenter.SwitchProvider/controlSwitch";
    private int mAreaHeight;
    /* access modifiers changed from: private */
    public Context mContext = null;
    private final ServiceConnection mCtrlCenterConn;
    /* access modifiers changed from: private */
    public IControlCenterGesture mCtrlCenterIntf;
    private WindowManagerPolicy.WindowState mFocusWin;
    private Handler mHandler;
    private boolean mHwHasNaviBar;
    private boolean mIsControlCenterOn;
    private boolean mIsCtrlCenterInstalled;
    private boolean mIsFirstGetControlCenter;
    private boolean mIsFirstStarted;
    /* access modifiers changed from: private */
    public boolean mIsFromLeft;
    private boolean mIsGestureOn;
    private boolean mIsInSpecialMode;
    private boolean mIsInTaskLockMode;
    /* access modifiers changed from: private */
    public boolean mIsNeedStart;
    private boolean mIsNeedUpdate;
    private boolean mIsRegistered;
    private boolean mIsStartCtrlCenter;
    /* access modifiers changed from: private */
    public boolean mIsValidGesture;
    private int mLandAreaHeight;
    private long mLastAftGestureTime;
    private Rect mLeftRect;
    private final Object mLock = new Object();
    private WindowManagerPolicyConstants.PointerEventListener mPointListener;
    private PhoneWindowManager mPolicy = null;
    private Rect mRightRect;
    private SettingsObserver mSettingsObserver;
    private float mShortDistance;
    private int mSlideStartThreshold;
    private StatusBarManager mStatusBar;
    private final BroadcastReceiver mUserSwitchedReceiver;
    private WindowManagerPolicy.WindowManagerFuncs mWindowFuncs;
    private Point realSize;
    private MotionEvent startEvent;

    public NavigationCallOut(Context context, PhoneWindowManager policy, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        boolean z = false;
        this.mAreaHeight = 0;
        this.mLandAreaHeight = 0;
        this.mHwHasNaviBar = false;
        this.mIsValidGesture = false;
        this.realSize = new Point();
        this.mCtrlCenterIntf = null;
        this.mIsControlCenterOn = false;
        this.mIsFirstGetControlCenter = false;
        this.mLeftRect = new Rect();
        this.mRightRect = new Rect();
        this.mIsRegistered = false;
        this.mIsCtrlCenterInstalled = false;
        this.mIsNeedUpdate = false;
        this.mIsNeedStart = false;
        this.mIsStartCtrlCenter = false;
        this.startEvent = null;
        this.mIsFromLeft = false;
        this.mIsInTaskLockMode = false;
        this.mIsInSpecialMode = false;
        this.mIsFirstStarted = false;
        this.mShortDistance = 0.0f;
        this.mUserSwitchedReceiver = new BroadcastReceiver() {
            /* class com.android.server.policy.NavigationCallOut.AnonymousClass1 */

            public void onReceive(Context context, Intent intent) {
                if (context != null && intent != null) {
                    NavigationCallOut.this.updateSettings();
                    NavigationCallOut.this.updateHotArea();
                }
            }
        };
        this.mCtrlCenterConn = new ServiceConnection() {
            /* class com.android.server.policy.NavigationCallOut.AnonymousClass3 */

            public void onServiceConnected(ComponentName name, IBinder service) {
                IControlCenterGesture unused = NavigationCallOut.this.mCtrlCenterIntf = IControlCenterGesture.Stub.asInterface(service);
                if (NavigationCallOut.this.mCtrlCenterIntf == null) {
                    Log.e(NavigationCallOut.TAG, "mCtrlCenterIntf null object");
                } else if (NavigationCallOut.this.mIsNeedStart) {
                    NavigationCallOut navigationCallOut = NavigationCallOut.this;
                    navigationCallOut.startCtrlCenter(navigationCallOut.mIsFromLeft);
                    boolean unused2 = NavigationCallOut.this.mIsNeedStart = false;
                } else if (NavigationCallOut.this.mIsValidGesture) {
                    NavigationCallOut navigationCallOut2 = NavigationCallOut.this;
                    navigationCallOut2.preloadCtrlCenter(navigationCallOut2.mIsFromLeft);
                }
            }

            public void onServiceDisconnected(ComponentName name) {
                IControlCenterGesture unused = NavigationCallOut.this.mCtrlCenterIntf = null;
            }
        };
        this.mContext = context;
        this.mHandler = new Handler();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(Settings.Secure.getUriFor(GestureNavConst.KEY_SECURE_GESTURE_NAVIGATION), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Uri.parse(URI_CONTROLCENTER_SWITCH), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Secure.getUriFor(KEY_ENABLE_NAVBAR_DB), false, this.mSettingsObserver, -1);
        this.mPolicy = policy;
        this.mWindowFuncs = windowManagerFuncs;
        updateRealSize();
        changeArea();
        this.mPointListener = new WindowManagerPolicyConstants.PointerEventListener() {
            /* class com.android.server.policy.NavigationCallOut.AnonymousClass2 */

            public void onPointerEvent(MotionEvent motionEvent) {
                NavigationCallOut.this.addPointerEvent(motionEvent);
            }
        };
        this.mIsGestureOn = Settings.Secure.getInt(this.mContext.getContentResolver(), GestureNavConst.KEY_SECURE_GESTURE_NAVIGATION, 0) > 0;
        this.mHwHasNaviBar = Settings.System.getInt(this.mContext.getContentResolver(), KEY_ENABLE_NAVBAR_DB, 0) > 0 ? true : z;
        this.mStatusBar = (StatusBarManager) context.getSystemService("statusbar");
        IntentFilter filter = new IntentFilter();
        filter.addAction(SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED);
        this.mContext.registerReceiverAsUser(this.mUserSwitchedReceiver, UserHandle.ALL, filter, null, this.mHandler);
        if (!this.mIsGestureOn && this.mHwHasNaviBar) {
            registerPointer();
        }
    }

    private void registerPointer() {
        if (!this.mIsRegistered) {
            this.mWindowFuncs.registerPointerEventListener(this.mPointListener, 0);
            this.mIsRegistered = true;
        }
    }

    private void unregisterPointer() {
        if (this.mIsRegistered) {
            this.mWindowFuncs.unregisterPointerEventListener(this.mPointListener, 0);
            this.mIsRegistered = false;
        }
    }

    /* access modifiers changed from: private */
    public void addPointerEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == 0) {
            handleDown(event);
        } else if (action == 1) {
            handActionUp(this.startEvent, event);
        } else if (action == 2) {
            handleMove(this.startEvent, event);
        } else if (action == 3) {
            handleCancel();
        }
    }

    private void reset() {
        this.mIsValidGesture = false;
        this.startEvent = null;
    }

    private boolean touchDownIsValid(float pointX, float pointY) {
        if (getDisplay() == null || ((this.mPolicy.mKeyguardDelegate.isShowing() && !this.mPolicy.mKeyguardDelegate.isOccluded()) || this.mIsInTaskLockMode)) {
            return false;
        }
        if (!this.mIsFirstGetControlCenter) {
            this.mIsCtrlCenterInstalled = isCtrlCenterInstalled();
            this.mIsControlCenterOn = isControlCenterSwitchOn() && this.mIsCtrlCenterInstalled;
            this.mIsFirstGetControlCenter = true;
            changeArea();
        }
        if (!this.mIsControlCenterOn) {
            unregisterPointer();
            return false;
        }
        if (this.mIsNeedUpdate) {
            changeArea();
        }
        StatusBarManager statusBarManager = this.mStatusBar;
        if (statusBarManager != null && statusBarManager.isNotificationsPanelExpand()) {
            return false;
        }
        if (this.mLeftRect.contains((int) pointX, (int) pointY)) {
            if (shouldStartGesture()) {
                this.mIsFromLeft = true;
                bindCtrlCenter();
            }
            return true;
        } else if (!this.mRightRect.contains((int) pointX, (int) pointY)) {
            return false;
        } else {
            if (shouldStartGesture()) {
                this.mIsFromLeft = false;
                bindCtrlCenter();
            }
            return true;
        }
    }

    private boolean shouldStartGesture() {
        this.mIsInSpecialMode = shouldCheckAftForThisGesture();
        if (this.mIsInSpecialMode) {
            this.mIsFirstStarted = SystemClock.uptimeMillis() - this.mLastAftGestureTime <= 2500;
        }
        return (this.mIsInSpecialMode && this.mIsFirstStarted) || !this.mIsInSpecialMode;
    }

    private void updateRealSize() {
        if (getDisplay() != null) {
            getDisplay().getRealSize(this.realSize);
        }
    }

    private Display getDisplay() {
        return this.mContext.getDisplay();
    }

    private int getNavigationBarPosition() {
        if (this.mPolicy.mDefaultDisplayPolicy != null) {
            return this.mPolicy.mDefaultDisplayPolicy.getNavBarPosition();
        }
        PhoneWindowManager phoneWindowManager = this.mPolicy;
        return 4;
    }

    private void handleDown(MotionEvent event) {
        this.mIsValidGesture = touchDownIsValid(event.getRawX(), event.getRawY());
        if (this.mIsValidGesture) {
            this.startEvent = event.copy();
        }
    }

    private void handleMove(MotionEvent e1, MotionEvent e2) {
        if (!this.mIsValidGesture || this.startEvent == null) {
            dismissCtrlCenter();
            return;
        }
        float startX = e1.getX();
        float startY = e1.getY();
        float endX = e2.getX();
        float endY = e2.getY();
        float deltX = Math.max(Math.abs(endX - startX), 1.0f);
        float deltY = Math.max(Math.abs(endY - startY), 1.0f);
        if (deltX / deltY > 1.0f || endY > startY) {
            reset();
        } else if (((this.mIsInSpecialMode && this.mIsFirstStarted) || !this.mIsInSpecialMode) && deltY > this.mShortDistance) {
            if (!this.mIsStartCtrlCenter) {
                preloadCtrlCenter(this.mIsFromLeft);
            } else {
                moveCtrlCenter(((float) this.realSize.y) - endY);
            }
        }
    }

    private void handActionUp(MotionEvent e1, MotionEvent e2) {
        if (!this.mIsValidGesture || this.startEvent == null) {
            dismissCtrlCenter();
            return;
        }
        float startX = e1.getX();
        float startY = e1.getY();
        float endX = e2.getX();
        float endY = e2.getY();
        if (Math.max(Math.abs(endX - startX), 1.0f) / Math.max(Math.abs(endY - startY), 1.0f) <= 1.0f && Math.abs(startY - endY) > ((float) this.mSlideStartThreshold)) {
            if ((!this.mIsInSpecialMode || !this.mIsFirstStarted) && this.mIsInSpecialMode) {
                showReTryToast();
                this.mIsFirstStarted = true;
                this.mLastAftGestureTime = SystemClock.uptimeMillis();
            } else {
                locateCtrlCenter();
            }
            Log.i(TAG, "start activity");
        }
        dismissCtrlCenter();
        reset();
    }

    private void handleCancel() {
        reset();
        dismissCtrlCenter();
    }

    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean isSelfChange) {
            NavigationCallOut.this.updateSettings();
        }
    }

    /* access modifiers changed from: private */
    public void updateSettings() {
        synchronized (this.mLock) {
            boolean z = true;
            this.mIsGestureOn = Settings.Secure.getInt(this.mContext.getContentResolver(), GestureNavConst.KEY_SECURE_GESTURE_NAVIGATION, 0) > 0;
            this.mIsCtrlCenterInstalled = isCtrlCenterInstalled();
            this.mIsControlCenterOn = isControlCenterSwitchOn() && this.mIsCtrlCenterInstalled;
            if (Settings.System.getInt(this.mContext.getContentResolver(), KEY_ENABLE_NAVBAR_DB, 0) <= 0) {
                z = false;
            }
            this.mHwHasNaviBar = z;
            if (!this.mHwHasNaviBar || this.mIsGestureOn || !this.mIsControlCenterOn) {
                unregisterPointer();
            } else {
                registerPointer();
            }
            reset();
            Log.e(TAG, "mIsControlCenterOn" + this.mIsControlCenterOn + this.mHwHasNaviBar + this.mIsGestureOn);
        }
    }

    private void bindCtrlCenter() {
        if (this.mCtrlCenterIntf == null) {
            Intent intent = new Intent(CTRLCENTER_ACTION);
            intent.setPackage(CTRLCENTER_PKG);
            try {
                Context context = this.mContext;
                ServiceConnection serviceConnection = this.mCtrlCenterConn;
                Context context2 = this.mContext;
                context.bindServiceAsUser(intent, serviceConnection, 1, UserHandle.of(ActivityManager.getCurrentUser()));
            } catch (SecurityException e) {
                Log.i(TAG, "bind ControlCenterGesture Service failed");
            }
        }
    }

    /* access modifiers changed from: private */
    public void startCtrlCenter(boolean isLeft) {
        if (this.mCtrlCenterIntf != null) {
            Log.i(TAG, "start control center");
            try {
                this.mCtrlCenterIntf.startControlCenterSide(isLeft);
            } catch (RemoteException | IllegalStateException e) {
                Log.e(TAG, "start ctrlCenter fail");
            } catch (Exception e2) {
                Log.e(TAG, "unknow error");
            }
        }
    }

    private boolean isCtrlCenterInstalled() {
        try {
            boolean isCtrlCenterExist = checkPackageExist(CTRLCENTER_PKG, ActivityManager.getCurrentUser());
            if (!isCtrlCenterExist) {
                return isCtrlCenterExist;
            }
            Bundle res = this.mContext.getContentResolver().call(AUTHORITY_URI, METHOD_CHECK_CONTROL_CENTER_ENABLE, (String) null, (Bundle) null);
            if (res == null || !res.getBoolean(ISCONTROLCENTERENABLE, false)) {
                return false;
            }
            return isCtrlCenterExist;
        } catch (IllegalArgumentException | IllegalStateException e) {
            Log.w(TAG, "not ready.");
            return false;
        } catch (Exception e2) {
            Log.w(TAG, "Illegal.");
            return false;
        }
    }

    private boolean checkPackageExist(String packageName, int userId) {
        try {
            this.mContext.getPackageManager().getPackageInfoAsUser(packageName, 128, userId);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, packageName + " not found for userId:" + userId);
            return false;
        } catch (Exception e2) {
            Log.w(TAG, packageName + " not available for userId:" + userId);
            return false;
        }
    }

    private boolean isControlCenterSwitchOn() {
        Bundle res;
        try {
            if (AUTHORITY_URI == null || (res = this.mContext.getContentResolver().call(AUTHORITY_URI, METHOD_CHECK_SWITCH, (String) null, (Bundle) null)) == null || !res.getBoolean(ISSWITCHON, false)) {
                return false;
            }
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            Log.w(TAG, "not ready.");
            return false;
        } catch (Exception e2) {
            Log.w(TAG, "Illegal.");
            return false;
        }
    }

    public void updateHotArea() {
        dismissCtrlCenter();
        this.mIsNeedUpdate = true;
    }

    private void changeArea() {
        if (this.mIsCtrlCenterInstalled) {
            updateRealSize();
            this.mSlideStartThreshold = this.mContext.getResources().getDimensionPixelSize(34472763);
            this.mLandAreaHeight = this.mContext.getResources().getDimensionPixelSize(34472510);
            this.mAreaHeight = (int) (((double) this.mContext.getResources().getDimensionPixelSize(17105305)) * NAV_START_PART);
            int rotation = this.mPolicy.getDefaultDisplayPolicy().getDisplayRotation();
            if (!IS_FOLD_SCREEN_DEVICE || HwFoldScreenManager.getDisplayMode() != 3) {
                if (rotation == 3 || rotation == 1) {
                    this.mLeftRect.set(0, this.realSize.y - this.mLandAreaHeight, (int) (((double) this.realSize.x) * LEFT_RANGE_LAND), this.realSize.y);
                    this.mRightRect.set((int) (((double) this.realSize.x) * RIGHT_RANGE_LAND), this.realSize.y - this.mLandAreaHeight, this.realSize.x, this.realSize.y);
                } else {
                    this.mLeftRect.set(0, this.realSize.y - this.mAreaHeight, (int) (((double) this.realSize.x) * LEFT_RANGE), this.realSize.y);
                    this.mRightRect.set((int) (((double) this.realSize.x) * RIGHT_RANGE), this.realSize.y - this.mAreaHeight, this.realSize.x, this.realSize.y);
                }
                this.mShortDistance = (float) ((this.mLeftRect.bottom - this.mLeftRect.top) / 2);
                this.mIsNeedUpdate = false;
                return;
            }
            this.mLeftRect.set(0, 0, 0, 0);
            this.mRightRect.set(0, 0, 0, 0);
            this.mIsNeedUpdate = false;
        }
    }

    /* access modifiers changed from: private */
    public void preloadCtrlCenter(boolean isLeft) {
        IControlCenterGesture iControlCenterGesture = this.mCtrlCenterIntf;
        if (iControlCenterGesture != null) {
            try {
                iControlCenterGesture.preloadControlCenterSide(isLeft);
                this.mIsStartCtrlCenter = true;
            } catch (RemoteException | IllegalStateException e) {
                Log.e(TAG, "start ctrlCenter fail");
            } catch (Exception e2) {
                Log.e(TAG, "unknow error");
            }
        }
    }

    private void moveCtrlCenter(float currentTouch) {
        IControlCenterGesture iControlCenterGesture = this.mCtrlCenterIntf;
        if (iControlCenterGesture != null) {
            try {
                iControlCenterGesture.moveControlCenter(currentTouch);
            } catch (RemoteException | IllegalStateException e) {
            } catch (Exception e2) {
                Log.e(TAG, "unknow error");
            }
        }
    }

    private void locateCtrlCenter() {
        IControlCenterGesture iControlCenterGesture = this.mCtrlCenterIntf;
        if (iControlCenterGesture != null) {
            try {
                iControlCenterGesture.locateControlCenter();
                this.mIsStartCtrlCenter = false;
            } catch (RemoteException | IllegalStateException e) {
                Log.e(TAG, "reset ctrlCenter fail");
            } catch (Exception e2) {
                Log.e(TAG, "unknow error");
            }
        } else {
            this.mIsNeedStart = true;
        }
    }

    private void dismissCtrlCenter() {
        IControlCenterGesture iControlCenterGesture;
        if (this.mIsStartCtrlCenter && (iControlCenterGesture = this.mCtrlCenterIntf) != null) {
            try {
                iControlCenterGesture.dismissControlCenter();
                this.mIsStartCtrlCenter = false;
            } catch (RemoteException | IllegalStateException e) {
                Log.e(TAG, "dismiss ctrlCenter fail");
            } catch (Exception e2) {
                Log.e(TAG, "unknow error");
            }
        }
    }

    private boolean shouldCheckAftForThisGesture() {
        return HwGestureNavWhiteConfig.getInstance().isEnable(this.mFocusWin, this.mPolicy.getDefaultDisplayPolicy().getDisplayRotation(), this.mPolicy);
    }

    private void showReTryToast() {
        this.mHandler.post(new Runnable() {
            /* class com.android.server.policy.NavigationCallOut.AnonymousClass4 */

            public void run() {
                Toast toast = Toast.makeText(new ContextThemeWrapper(NavigationCallOut.this.mContext, 33947656), 33686228, 0);
                toast.getWindowParams().type = HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL;
                toast.getWindowParams().privateFlags |= 16;
                toast.show();
            }
        });
    }

    public void updateLockTaskState(int lockTaskState) {
        this.mIsInTaskLockMode = GestureUtils.isInLockTaskMode(lockTaskState);
    }

    public void notifyFocusChange(WindowManagerPolicy.WindowState newFocus) {
        this.mFocusWin = newFocus;
    }
}
