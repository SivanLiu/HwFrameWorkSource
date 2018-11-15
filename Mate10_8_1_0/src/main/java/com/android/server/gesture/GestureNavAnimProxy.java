package com.android.server.gesture;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.hdm.HwDeviceManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings.Secure;
import android.util.Flog;
import android.util.Log;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.IStatusBarService.Stub;
import com.android.server.devicepolicy.StorageUtils;
import com.android.server.gesture.GestureNavView.IGestureNavBackAnim;
import com.android.server.gesture.GestureNavView.IGestureNavBottomAnim;
import com.android.server.gesture.GestureNavView.WindowConfig;
import com.android.server.gesture.anim.GLGestureBackView;
import com.android.server.gesture.anim.GLGestureBackView.GestureBackAnimListener;
import com.android.server.gesture.anim.ScaleImageView;
import com.android.server.gesture.anim.ScaleImageView.TranslateAnimationListener;
import com.huawei.android.app.HwActivityManager;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public class GestureNavAnimProxy implements IGestureNavBackAnim, IGestureNavBottomAnim {
    private static final String ACTION_STATUSBAR_CHANGE = "com.android.systemui.statusbar.visible.change";
    private static final int DEFAULT_ANIM_MAX_TIME = 2000;
    private static final int GO_RECENT_DELAY = 3000;
    private static final int HW_RECENT_TRANSACTION_CODE = 122;
    private static final int MSG_ANIM_TIMEOUT = 1;
    private static final int MSG_CHECK_LONG_PRESS = 2;
    private static final int MSG_GESTURE_UP = 3;
    private static final int MSG_HIDE_ACTIVITY_TASK = 4;
    private static final int MSG_HIDE_BOTTOM_VIEW = 7;
    private static final int MSG_PLAY_GO_RECENT = 5;
    private static final int MSG_PLAY_GO_RECENT_TIMEOUT = 6;
    private static final int OVER_MODE_CANCEL = 3;
    private static final int OVER_MODE_GO_HOME = 2;
    private static final int OVER_MODE_GO_RECENT = 1;
    private static final int OVER_MODE_NONE = 0;
    private static final int SPLIT_MODE_OFF = 0;
    private static final int SPLIT_MODE_ON = 1;
    private static final String SPLIT_SCREEN_MODE = "split_screen_mode";
    private static final String TAG = "GestureNavAnim";
    private final int WINDOW_APPEND_FLAG;
    private GLGestureBackView mBackAnimView;
    private GestureAnimContainer mBackContainer;
    private ScaleImageView mBottomAnimIconViewLegacy;
    private boolean mBottomAnimStarted;
    private ScaleImageView mBottomAnimViewLegacy;
    private GestureAnimContainer mBottomContainer;
    private Context mContext;
    private DeviceStateController mDeviceStateController;
    private boolean mDisableBottomAnim;
    private boolean mDisableBottomFunction;
    private boolean mDisableHomeAnim;
    private boolean mDisableHomeFunc;
    private boolean mDisableRecentAnim;
    private boolean mDisableRecentFunc;
    private String mFocusWindowTitle;
    private boolean mGestureNavReady;
    private Handler mHandler;
    private float mHomeStartMinDistance;
    private String mHomeWindow;
    private boolean mIsStatusBarExplaned = false;
    private final Object mLock = new Object();
    private boolean mLongPressChecking;
    private int mOverMode;
    private boolean mPlayUpAnimReady;
    private boolean mPlayUpAnimTimeout;
    private float mRecentAppearDistance;
    private float mRecentDisappearDistance;
    private Rect mRecentRect = new Rect();
    private boolean mRecentStarted;
    private boolean mRecentStartedOnce;
    private boolean mRecentTriggled;
    private final Object mServiceAquireLock = new Object();
    private StatusBarStatesChangedReceiver mStatusBarReceiver;
    private IStatusBarService mStatusBarService;
    private boolean mTargetStartAllRight;
    private boolean mTaskVisibleSet;
    private boolean mTaskVisibleState = true;
    private String mTopActivity;
    private int mTopTaskId = -1;
    private boolean mUseAppendFlag = false;
    private WindowConfig mWindowConfig = new WindowConfig();
    private WindowManager mWindowManager;
    private boolean mWindowViewSetuped;

    interface AnimContainerListener {
        void onAttachedToWindow();

        void onDetachedFromWindow();
    }

    private final class AnimContainerListenerImpl implements AnimContainerListener {
        private AnimContainerListenerImpl() {
        }

        public void onAttachedToWindow() {
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavAnimProxy.TAG, "container attached to window");
            }
            if (GestureNavAnimProxy.this.mBackAnimView != null) {
                GestureNavAnimProxy.this.mBackAnimView.onResume();
            }
        }

        public void onDetachedFromWindow() {
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavAnimProxy.TAG, "container detached from window");
            }
            if (GestureNavAnimProxy.this.mBackAnimView != null) {
                GestureNavAnimProxy.this.mBackAnimView.onPause();
            }
        }
    }

    private final class AnimHandler extends Handler {
        public AnimHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Object -get5;
            switch (msg.what) {
                case 1:
                    Log.i(GestureNavAnimProxy.TAG, "animation timeout, force hide views");
                    GestureNavAnimProxy.this.setTaskVisibleState(true);
                    GestureNavAnimProxy.this.updateAppendFlag(false);
                    GestureNavAnimProxy.this.showBackContainer(false);
                    GestureNavAnimProxy.this.showBottomContainer(false);
                    return;
                case 2:
                    GestureNavAnimProxy.this.notifyRecentStart();
                    if (!GestureNavAnimProxy.this.mRecentStartedOnce) {
                        GestureNavAnimProxy.this.mRecentStartedOnce = true;
                        return;
                    }
                    return;
                case 3:
                    GestureNavAnimProxy.this.notifyRecentEnd();
                    return;
                case 4:
                    GestureNavAnimProxy.this.setTaskVisibleState(false);
                    return;
                case 5:
                    -get5 = GestureNavAnimProxy.this.mLock;
                    synchronized (-get5) {
                        Log.d(GestureNavAnimProxy.TAG, "MSG_PLAY_GO_RECENT, mPlayUpAnimReady " + GestureNavAnimProxy.this.mPlayUpAnimReady);
                        if (GestureNavAnimProxy.this.mPlayUpAnimReady) {
                            GestureNavAnimProxy.this.mPlayUpAnimReady = false;
                            GestureNavAnimProxy.this.playGoRecentAnim();
                            break;
                        }
                    }
                    break;
                case 6:
                    -get5 = GestureNavAnimProxy.this.mLock;
                    synchronized (-get5) {
                        Log.d(GestureNavAnimProxy.TAG, "MSG_PLAY_GO_RECENT_TIMEOUT, mPlayUpAnimReady " + GestureNavAnimProxy.this.mPlayUpAnimReady);
                        GestureNavAnimProxy.this.mPlayUpAnimTimeout = true;
                        if (GestureNavAnimProxy.this.mPlayUpAnimReady) {
                            GestureNavAnimProxy.this.mPlayUpAnimReady = false;
                            GestureNavAnimProxy.this.playGoRecentAnim();
                            break;
                        }
                    }
                    break;
                case 7:
                    GestureNavAnimProxy.this.hideBottomViewsAtEnd();
                    return;
                default:
                    return;
            }
        }
    }

    static class GestureAnimContainer extends FrameLayout {
        private AnimContainerListener mListener;

        public GestureAnimContainer(Context context) {
            super(context);
        }

        public void setListener(AnimContainerListener listener) {
            this.mListener = listener;
        }

        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (this.mListener != null) {
                this.mListener.onAttachedToWindow();
            }
        }

        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (this.mListener != null) {
                this.mListener.onDetachedFromWindow();
            }
        }
    }

    private final class GestureBackAnimListenerImpl implements GestureBackAnimListener {
        private GestureBackAnimListenerImpl() {
        }

        public void onAnimationEnd(int animType) {
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavAnimProxy.TAG, "back anim end, animType=" + animType);
            }
            GestureNavAnimProxy.this.mHandler.removeMessages(1);
            if (GestureNavAnimProxy.this.mBackAnimView != null) {
                GestureNavAnimProxy.this.mBackAnimView.setDraw(false);
            }
            GestureNavAnimProxy.this.showBackContainer(false);
        }
    }

    private final class HomeAnimationListenerImpl implements TranslateAnimationListener {
        private HomeAnimationListenerImpl() {
        }

        public void onAnimationStart(int type, boolean hasAnim) {
            if (GestureNavAnimProxy.this.mOverMode == 2) {
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavAnimProxy.TAG, "Home onAnimationStart type=" + type + ", hasAnim=" + hasAnim);
                }
                if (type == 2 && hasAnim) {
                    GestureNavAnimProxy.this.setTaskVisibleState(false);
                }
            }
        }

        public void onAnimationEnd(int type, boolean hasAnim) {
            if (GestureNavAnimProxy.this.mOverMode == 2) {
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavAnimProxy.TAG, "Home onAnimationEnd type=" + type + ", hasAnim=" + hasAnim);
                }
                GestureNavAnimProxy.this.mHandler.removeMessages(1);
                if (type == 2) {
                    GestureNavAnimProxy.this.hideBottomViewsAtEnd();
                }
            }
        }
    }

    private final class StatusBarStatesChangedReceiver extends BroadcastReceiver {
        private StatusBarStatesChangedReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null && GestureNavAnimProxy.ACTION_STATUSBAR_CHANGE.equals(intent.getAction())) {
                String visible = StorageUtils.SDCARD_RWMOUNTED_STATE;
                if (intent.getExtras() != null) {
                    visible = intent.getExtras().getString("visible");
                }
                GestureNavAnimProxy.this.mIsStatusBarExplaned = Boolean.valueOf(visible).booleanValue();
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavAnimProxy.TAG, "mIsStatusBarExplaned:" + GestureNavAnimProxy.this.mIsStatusBarExplaned);
                }
            }
        }
    }

    private final class TranslateAnimationListenerImpl implements TranslateAnimationListener {
        private TranslateAnimationListenerImpl() {
        }

        public void onAnimationStart(int type, boolean hasAnim) {
        }

        public void onAnimationEnd(int type, boolean hasAnim) {
            if (GestureNavAnimProxy.this.mOverMode == 1 || GestureNavAnimProxy.this.mOverMode == 3) {
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavAnimProxy.TAG, "Translate onAnimationEnd type=" + type + ", hasAnim=" + hasAnim);
                }
                GestureNavAnimProxy.this.mHandler.removeMessages(1);
                if (type == 1) {
                    GestureNavAnimProxy.this.recoverAppAfterCancel();
                } else {
                    GestureNavAnimProxy.this.hideBottomViewsAtEnd();
                }
            }
        }
    }

    private android.view.WindowManager.LayoutParams createLayoutParams(java.lang.String r1, com.android.server.gesture.GestureNavView.WindowConfig r2, boolean r3) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.server.gesture.GestureNavAnimProxy.createLayoutParams(java.lang.String, com.android.server.gesture.GestureNavView$WindowConfig, boolean):android.view.WindowManager$LayoutParams
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.gesture.GestureNavAnimProxy.createLayoutParams(java.lang.String, com.android.server.gesture.GestureNavView$WindowConfig, boolean):android.view.WindowManager$LayoutParams");
    }

    public GestureNavAnimProxy(Context context, Looper looper) {
        this.mContext = context;
        this.mHandler = new AnimHandler(looper);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.WINDOW_APPEND_FLAG = HighBitsCompModeID.MODE_COLOR_ENHANCE;
        this.mDeviceStateController = DeviceStateController.getInstance(context);
    }

    private void updateConfig() {
        int resId;
        boolean isLand = 2 == this.mContext.getResources().getConfiguration().orientation;
        if (isLand) {
            resId = 34472225;
        } else {
            resId = 34472224;
        }
        this.mRecentAppearDistance = (float) this.mContext.getResources().getDimensionPixelSize(resId);
        this.mRecentDisappearDistance = (float) ((int) (this.mRecentAppearDistance * 0.9f));
        this.mHomeStartMinDistance = (float) ((int) (this.mRecentAppearDistance * 0.5f));
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "isLand=" + isLand + ", RecentAppearDistance=" + this.mRecentAppearDistance + ", RecentDisappearDistance=" + this.mRecentDisappearDistance + ", HomeStartDistance=" + this.mHomeStartMinDistance);
        }
    }

    private void showBottomContainer(boolean show) {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "showBottomContainer show=" + show);
        }
        if (show) {
            this.mHandler.removeMessages(7);
        }
        if (this.mBottomContainer != null) {
            this.mBottomContainer.setVisibility(show ? 0 : 8);
        }
    }

    private void showBackContainer(boolean show) {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "showBackContainer show=" + show);
        }
        if (this.mBackContainer != null) {
            this.mBackContainer.setVisibility(show ? 0 : 8);
        }
    }

    private void updateAppendFlag(boolean useAppendFlag) {
        synchronized (this.mLock) {
            this.mUseAppendFlag = useAppendFlag;
            updateAnimWindowLocked();
        }
    }

    private void updateGestureNavAnimWindow() {
        synchronized (this.mLock) {
            updateAnimWindowLocked();
        }
    }

    private void updateAnimWindowLocked() {
        if (this.mGestureNavReady) {
            if (this.mWindowViewSetuped) {
                updateAnimWindows();
            } else {
                createAnimWindows();
            }
        } else if (this.mWindowViewSetuped) {
            destroyNavWindows();
        }
    }

    private void createAnimWindows() {
        Log.i(TAG, "createAnimWindows");
        if (GestureNavConst.USE_ANIM_LEGACY) {
            this.mBottomContainer = new GestureAnimContainer(this.mContext);
            this.mBottomAnimIconViewLegacy = new ScaleImageView(this.mContext);
            this.mBottomAnimIconViewLegacy.setAnimationListener(new HomeAnimationListenerImpl());
            this.mBottomContainer.addView(this.mBottomAnimIconViewLegacy, -1, -1);
            this.mBottomAnimViewLegacy = new ScaleImageView(this.mContext);
            this.mBottomAnimViewLegacy.setAnimationListener(new TranslateAnimationListenerImpl());
            this.mBottomAnimViewLegacy.setControlVisibleByProxy(true);
            this.mBottomContainer.addView(this.mBottomAnimViewLegacy, -1, -1);
            this.mBottomAnimViewLegacy.bringToFront();
            GestureUtils.addWindowView(this.mWindowManager, this.mBottomContainer, createLayoutParams("GestureNavAnimBottom", this.mWindowConfig, true));
            showBottomContainer(false);
        }
        this.mBackContainer = new GestureAnimContainer(this.mContext);
        this.mBackContainer.setListener(new AnimContainerListenerImpl());
        this.mBackAnimView = new GLGestureBackView(this.mContext);
        this.mBackAnimView.addAnimationListener(new GestureBackAnimListenerImpl());
        this.mBackContainer.addView(this.mBackAnimView, -1, -1);
        GestureUtils.addWindowView(this.mWindowManager, this.mBackContainer, createLayoutParams("GestureNavAnimBack", this.mWindowConfig, false));
        showBackContainer(false);
        this.mWindowViewSetuped = true;
    }

    private void updateAnimWindows() {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "updateAnimWindows, useAppendFlag:" + this.mUseAppendFlag);
        }
        if (GestureNavConst.USE_ANIM_LEGACY) {
            GestureUtils.updateViewLayout(this.mWindowManager, this.mBottomContainer, createLayoutParams("GestureNavAnimBottom", this.mWindowConfig, true));
        }
        GestureUtils.updateViewLayout(this.mWindowManager, this.mBackContainer, createLayoutParams("GestureNavAnimBack", this.mWindowConfig, false));
    }

    private void destroyNavWindows() {
        Log.i(TAG, "destroyNavWindows");
        this.mWindowViewSetuped = false;
        this.mUseAppendFlag = false;
        if (GestureNavConst.USE_ANIM_LEGACY) {
            showBottomContainer(false);
            GestureUtils.removeWindowView(this.mWindowManager, this.mBottomContainer, true);
            this.mBottomContainer = null;
        }
        showBackContainer(false);
        GestureUtils.removeWindowView(this.mWindowManager, this.mBackContainer, true);
        this.mBackContainer = null;
    }

    public void updateViewConfig(int _displayWidth, int _displayHeight, int _startX, int _startY, int _width, int _height, int _locationOnScreenX, int _locationOnScreenY) {
        synchronized (this.mLock) {
            this.mWindowConfig.update(_displayWidth, _displayHeight, _startX, _startY, _width, _height, _locationOnScreenX, _locationOnScreenY);
            updateConfig();
        }
    }

    public void updateViewNotchState(boolean usingNotch) {
        synchronized (this.mLock) {
            this.mWindowConfig.udpateNotch(usingNotch);
        }
    }

    public void onNavCreate() {
        this.mGestureNavReady = true;
        this.mStatusBarReceiver = new StatusBarStatesChangedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STATUSBAR_CHANGE);
        this.mContext.registerReceiver(this.mStatusBarReceiver, filter, "huawei.android.permission.HW_SIGNATURE_OR_SYSTEM", this.mHandler);
        updateGestureNavAnimWindow();
    }

    public void onNavUpdate() {
        updateGestureNavAnimWindow();
    }

    public void onNavDestroy() {
        this.mGestureNavReady = false;
        if (this.mStatusBarReceiver != null) {
            this.mContext.unregisterReceiver(this.mStatusBarReceiver);
            this.mStatusBarReceiver = null;
        }
        updateGestureNavAnimWindow();
    }

    public void updateHomeWindow(String homeWindow) {
        synchronized (this.mLock) {
            this.mHomeWindow = homeWindow;
        }
    }

    public void updateFocusWindow(String focusWindow) {
        synchronized (this.mLock) {
            this.mFocusWindowTitle = focusWindow;
        }
    }

    private boolean isSpecialWindow() {
        synchronized (this.mLock) {
            if ((this.mHomeWindow == null || !this.mHomeWindow.equals(this.mFocusWindowTitle)) && !GestureNavConst.RECENT_WINDOW.equals(this.mFocusWindowTitle)) {
                return false;
            }
            return true;
        }
    }

    private boolean isSpecialActivity(String activity) {
        synchronized (this.mLock) {
            if (this.mHomeWindow == null || !this.mHomeWindow.equals(activity)) {
                return false;
            }
            return true;
        }
    }

    private void updateBottomNavDisableState() {
        this.mDisableRecentFunc = false;
        this.mDisableHomeFunc = false;
        this.mDisableRecentAnim = false;
        this.mDisableHomeAnim = false;
        if (GestureUtils.isSuperPowerSaveMode() || isSplitMode() || GestureUtils.isInLockTaskMode()) {
            this.mDisableRecentFunc = true;
            this.mDisableRecentAnim = true;
            this.mDisableHomeAnim = true;
        }
        if (this.mDeviceStateController.isWindowRecentDisabled() || HwDeviceManager.disallowOp(15)) {
            this.mDisableRecentFunc = true;
            this.mDisableRecentAnim = true;
        }
        if (isSpecialWindow() || this.mIsStatusBarExplaned) {
            this.mDisableRecentAnim = true;
            this.mDisableHomeAnim = true;
        }
        if (this.mDeviceStateController.isWindowHomeDisabled() || HwDeviceManager.disallowOp(14)) {
            this.mDisableHomeFunc = true;
            this.mDisableHomeAnim = true;
        }
        Log.i(TAG, "disableRecent=" + this.mDisableRecentFunc + ", disableHome=" + this.mDisableHomeFunc + ", disableRecentAnim=" + this.mDisableRecentAnim + ", disableHomeAnim=" + this.mDisableHomeAnim);
    }

    private boolean checkDisableAnimAtEnd(int overMode) {
        switch (overMode) {
            case 1:
                return this.mDisableRecentAnim;
            case 2:
                return this.mDisableHomeAnim;
            default:
                return this.mDisableBottomAnim;
        }
    }

    private boolean isOverModeBad(int overMode) {
        boolean z = false;
        switch (overMode) {
            case 1:
                if (this.mBottomAnimStarted) {
                    z = this.mDisableRecentAnim;
                }
                return z;
            case 2:
                if (this.mBottomAnimStarted) {
                    z = this.mDisableHomeAnim;
                }
                return z;
            default:
                return false;
        }
    }

    private boolean isSplitMode() {
        return Secure.getIntForUser(this.mContext.getContentResolver(), SPLIT_SCREEN_MODE, 0, ActivityManager.getCurrentUser()) == 1;
    }

    public void onGestureAction(boolean down) {
    }

    public void setSide(boolean isLeft) {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "setSide isLeft=" + isLeft);
        }
        if (this.mBackAnimView != null) {
            this.mBackAnimView.setSide(isLeft);
        }
    }

    public void setAnimPosition(float y) {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "setAnimPosition y=" + y);
        }
        if (this.mBackAnimView != null) {
            showBackContainer(true);
            this.mBackAnimView.setDraw(true);
            this.mHandler.removeMessages(1);
            this.mBackAnimView.setAnimPosition(y);
        }
    }

    public boolean setAnimProcess(float process) {
        if (this.mBackAnimView == null) {
            return false;
        }
        this.mBackAnimView.setAnimProcess(process);
        return true;
    }

    public void playDisappearAnim() {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "playDisappearAnim");
        }
        if (this.mBackAnimView != null) {
            if (!this.mHandler.hasMessages(1)) {
                this.mHandler.sendEmptyMessageDelayed(1, 2000);
            }
            this.mBackAnimView.playDisappearAnim();
        }
    }

    public void playFastSlidingAnim() {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "playFastSlidingAnim");
        }
        if (this.mBackAnimView != null) {
            if (!this.mHandler.hasMessages(1)) {
                this.mHandler.sendEmptyMessageDelayed(1, 2000);
            }
            this.mBackAnimView.playFastSlidingAnim();
        }
    }

    public void onGestureStarted() {
        if (GestureNavConst.USE_ANIM_LEGACY) {
            boolean z;
            updateBottomNavDisableState();
            this.mDisableBottomFunction = this.mDisableRecentFunc ? this.mDisableHomeFunc : false;
            if (this.mDisableRecentAnim) {
                z = this.mDisableHomeAnim;
            } else {
                z = false;
            }
            this.mDisableBottomAnim = z;
            this.mTaskVisibleState = true;
            this.mTaskVisibleSet = false;
            this.mRecentStarted = false;
            this.mRecentStartedOnce = false;
            this.mRecentTriggled = false;
            this.mTargetStartAllRight = false;
            this.mBottomAnimStarted = false;
            this.mTopTaskId = -1;
        }
    }

    public void onGestureReallyStarted() {
        if (GestureNavConst.USE_ANIM_LEGACY && !this.mDisableBottomFunction) {
            resetLongPressState();
            this.mHandler.removeMessages(1);
            updateTopTaskInfo();
            this.mDisableBottomAnim |= isSpecialActivity(this.mTopActivity);
            if (!this.mDisableBottomAnim) {
                updateAppendFlag(true);
                showBottomContainer(true);
                HwActivityManager.gestureToHome();
            }
        }
    }

    public void onGestureSlowProcessStarted() {
        if (GestureNavConst.USE_ANIM_LEGACY && !this.mDisableBottomAnim) {
            notifyGestureStartForAnim();
        }
    }

    public void onGestureSlowProcess(float distance, float offsetX, float offsetY) {
        if (GestureNavConst.USE_ANIM_LEGACY && !this.mDisableBottomFunction) {
            if (!this.mDisableBottomAnim) {
                boolean ready = notifyGestureProcessForAnim(distance, offsetX, offsetY);
                if (!this.mTaskVisibleSet && ready) {
                    this.mTaskVisibleSet = true;
                    hideActivityTask(true, true);
                }
            }
            if (!this.mDisableRecentFunc) {
                if (this.mRecentStarted) {
                    if (distance < this.mRecentDisappearDistance) {
                        Log.i(TAG, "distance slide back, cancel recent");
                        resetLongPressState();
                        notifyRecentCancel();
                    }
                } else if (distance > this.mRecentAppearDistance) {
                    if (!this.mLongPressChecking && (this.mRecentStartedOnce ^ 1) != 0) {
                        Log.i(TAG, "start check long press");
                        this.mLongPressChecking = true;
                        this.mHandler.sendEmptyMessageDelayed(2, 200);
                    } else if (!this.mLongPressChecking && this.mRecentStartedOnce) {
                        Log.i(TAG, "distance slide up again, check restart");
                        this.mLongPressChecking = true;
                        this.mHandler.sendEmptyMessageDelayed(2, 150);
                    }
                } else if (this.mLongPressChecking) {
                    Log.i(TAG, "abort check long press as slide back");
                    resetLongPressState();
                }
            }
        }
    }

    public void onGestureFailed() {
        if (GestureNavConst.USE_ANIM_LEGACY && !this.mDisableBottomFunction) {
            checkHideMessageAtEnd();
            resetLongPressState();
            notifyRecentCancel();
            if (checkDisableAnimAtEnd(3)) {
                recoverAppAfterCancel();
            } else if (!notifyGestureFinishForAnim(3)) {
                hideBottomViewsAtEnd();
            }
            Flog.bdReport(this.mContext, 853, GestureNavConst.REPORT_FAILURE);
        }
    }

    public void onGestureSuccessFinished(float distance, long durationTime, float velocity, boolean isFastSlideGesture, Runnable goHomeRunnable) {
        if (GestureNavConst.USE_ANIM_LEGACY && !this.mDisableBottomFunction) {
            int overMode;
            checkHideMessageAtEnd();
            resetLongPressState();
            if (this.mRecentStarted) {
                overMode = 1;
            } else if ((durationTime >= 500 || distance <= this.mHomeStartMinDistance) && (!this.mTaskVisibleSet || distance <= this.mRecentAppearDistance)) {
                overMode = 3;
            } else {
                overMode = 2;
            }
            switch (overMode) {
                case 1:
                    if (!this.mDisableRecentFunc) {
                        Log.i(TAG, "Recent started, notify success up");
                        this.mRecentTriggled = true;
                        this.mHandler.sendEmptyMessage(3);
                        this.mTargetStartAllRight = true;
                        Flog.bdReport(this.mContext, 853, GestureNavConst.REPORT_SUCCESS);
                        break;
                    }
                    break;
                case 2:
                    if (!this.mDisableHomeFunc) {
                        Log.i(TAG, "slide all right, start goHome");
                        if (!((isFastSlideGesture && startHome()) || goHomeRunnable == null)) {
                            goHomeRunnable.run();
                        }
                        this.mTargetStartAllRight = true;
                        Flog.bdReport(this.mContext, 852, GestureNavConst.REPORT_SUCCESS);
                        break;
                    }
                    break;
                case 3:
                    Log.i(TAG, "slide cancel, duration:" + durationTime + ", distance:" + distance);
                    notifyRecentCancel();
                    Flog.bdReport(this.mContext, 852, GestureNavConst.REPORT_FAILURE);
                    break;
            }
            if (isOverModeBad(overMode)) {
                Log.i(TAG, "over mode is bad as animation conflict, set cancel");
                overMode = 3;
            }
            if (overMode == 2 && this.mTargetStartAllRight && isFastSlideGesture) {
                if (GestureNavConst.DEBUG) {
                    Log.d(TAG, "fast go home, skip scale view animation");
                }
                hideBottomViewsAtEnd();
                return;
            }
            if (checkDisableAnimAtEnd(overMode)) {
                if (overMode == 3) {
                    recoverAppAfterCancel();
                } else {
                    hideBottomViewsAtEnd();
                }
            } else if (!notifyGestureFinishForAnim(overMode)) {
                hideBottomViewsAtEnd();
            }
        }
    }

    public void onGestureEnd(int action) {
        if (GestureNavConst.USE_ANIM_LEGACY && !this.mDisableBottomFunction) {
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "onGestureEnd action=" + action);
            }
            if (!(this.mDisableRecentFunc || (this.mRecentTriggled ^ 1) == 0)) {
                notifyRecentEnd();
            }
        }
    }

    private void notifyRecentStart() {
        if (!this.mRecentStarted) {
            transactGestureRecent(122, "notifyRecentStart", 1);
            this.mRecentStarted = true;
        }
    }

    private void notifyRecentEnd() {
        transactGestureRecent(122, "notifyRecentEnd", 2);
    }

    private void notifyRecentCancel() {
        if (this.mRecentStarted) {
            transactGestureRecent(122, "notifyRecentCancel", 3);
            this.mRecentStarted = false;
        }
    }

    private IStatusBarService getHWStatusBarService() {
        IStatusBarService iStatusBarService;
        synchronized (this.mServiceAquireLock) {
            if (this.mStatusBarService == null) {
                this.mStatusBarService = Stub.asInterface(ServiceManager.getService("statusbar"));
            }
            iStatusBarService = this.mStatusBarService;
        }
        return iStatusBarService;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void transactGestureRecent(int code, String transactName, int paramValue) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            IBinder statusBarServiceBinder = getHWStatusBarService().asBinder();
            if (statusBarServiceBinder != null) {
                Log.i(TAG, "transact code:" + code + " , name:" + transactName + ", param:" + paramValue);
                data.writeInterfaceToken("com.android.internal.statusbar.IStatusBarService");
                data.writeInt(paramValue);
                statusBarServiceBinder.transact(code, data, reply, 0);
            }
            reply.recycle();
            data.recycle();
        } catch (RemoteException e) {
            Log.e(TAG, "exception occur in " + transactName + ", param:" + paramValue, e);
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
        }
    }

    private void hideActivityTask(boolean forceSend, boolean delay) {
        if (forceSend) {
            this.mHandler.removeMessages(4);
        }
        if (!this.mHandler.hasMessages(4)) {
            this.mHandler.sendEmptyMessageDelayed(4, (long) (delay ? 20 : 0));
        }
    }

    private void checkHideMessageAtEnd() {
        if (this.mHandler.hasMessages(4)) {
            this.mHandler.removeMessages(4);
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "up arrive and remove stranding hide message");
            }
        }
    }

    private void setTaskVisibleState(boolean show) {
        if (this.mTaskVisibleState != show) {
            this.mTaskVisibleState = show;
            Log.i(TAG, "setActivityVisibleState:" + show);
            HwActivityManager.setActivityVisibleState(show);
        }
    }

    private void notifyGestureStartForAnim() {
        if (this.mBottomAnimViewLegacy != null) {
            this.mBottomAnimViewLegacy.refreshContent();
        }
        if (this.mBottomAnimIconViewLegacy != null) {
            this.mBottomAnimIconViewLegacy.refreshContent();
        }
        this.mBottomAnimStarted = true;
    }

    private boolean notifyGestureProcessForAnim(float distance, float offsetX, float offsetY) {
        if (!this.mBottomAnimStarted || this.mBottomAnimViewLegacy == null) {
            return false;
        }
        this.mBottomAnimViewLegacy.setFollowPosition(offsetX, offsetY);
        if (this.mBottomAnimIconViewLegacy != null) {
            this.mBottomAnimIconViewLegacy.setFollowPosition(offsetX, offsetY);
        }
        return true;
    }

    private boolean notifyGestureFinishForAnim(int overMode) {
        if (!this.mBottomAnimStarted || this.mBottomAnimViewLegacy == null) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "overMode=" + overMode + ", mRecentRect.top=" + this.mRecentRect.top);
        }
        this.mOverMode = overMode;
        if (!this.mHandler.hasMessages(1)) {
            this.mHandler.sendEmptyMessageDelayed(1, 2000);
        }
        switch (overMode) {
            case 1:
                synchronized (this.mLock) {
                    Log.d(TAG, "setGestureEnd try to go recent, mPlayUpAnimReady " + this.mPlayUpAnimReady);
                    this.mPlayUpAnimReady = true;
                    this.mPlayUpAnimTimeout = false;
                    this.mHandler.sendEmptyMessageDelayed(6, 3000);
                }
                break;
            case 2:
                this.mHandler.post(new Runnable() {
                    public void run() {
                        GestureNavAnimProxy.this.mBottomAnimViewLegacy.playGestureToLauncherIconAnimation(false);
                        if (GestureNavAnimProxy.this.mBottomAnimIconViewLegacy != null) {
                            GestureNavAnimProxy.this.mBottomAnimIconViewLegacy.playGestureToLauncherIconAnimation(true);
                        }
                    }
                });
                break;
            case 3:
                this.mBottomAnimViewLegacy.playRecoverAnimation();
                if (this.mBottomAnimIconViewLegacy != null) {
                    this.mBottomAnimIconViewLegacy.playRecoverAnimation();
                    break;
                }
                break;
        }
        return true;
    }

    private void resetLongPressState() {
        this.mLongPressChecking = false;
        this.mHandler.removeMessages(2);
    }

    private void playGoRecentAnim() {
        this.mBottomAnimViewLegacy.playTranslateAnimation(this.mRecentRect.left, this.mRecentRect.top, this.mRecentRect.width(), this.mRecentRect.height());
        if (this.mBottomAnimIconViewLegacy != null) {
            this.mBottomAnimIconViewLegacy.playTranslateAnimation(this.mRecentRect.left, this.mRecentRect.top, this.mRecentRect.width(), this.mRecentRect.height());
        }
    }

    public void setRecentPosition(int x, int y, int width, int height) {
        Log.i(TAG, "setRecentPosition x=" + x + ", y=" + y + ", width=" + width + ", height=" + height);
        this.mRecentRect.set(x, y, x + width, y + height);
        synchronized (this.mLock) {
            if (this.mPlayUpAnimReady) {
                this.mHandler.removeMessages(6);
                this.mHandler.sendEmptyMessage(5);
            } else if (this.mPlayUpAnimTimeout) {
                Log.w(TAG, "setRecentPosition already timeout.");
            } else {
                Log.w(TAG, "setRecentPosition warning, play up anim not ready");
            }
        }
    }

    private void updateTopTaskInfo() {
        String str = null;
        RunningTaskInfo topTask = GestureUtils.getRunningTask(this.mContext);
        if (topTask != null) {
            this.mTopTaskId = topTask.id;
            if (topTask.topActivity != null) {
                str = topTask.topActivity.flattenToString();
            }
            this.mTopActivity = str;
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "updateTopTaskInfo taskId=" + this.mTopTaskId + ", topActivity=" + this.mTopActivity);
                return;
            }
            return;
        }
        this.mTopTaskId = -1;
        this.mTopActivity = null;
    }

    private void moveTaskToFront(int taskId) {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "moveTaskToFront taskId=" + taskId);
        }
        if (taskId != -1) {
            try {
                ActivityManager.getService().moveTaskToFront(taskId, 0, null);
            } catch (RemoteException e) {
                Log.e(TAG, "moveTaskToFront fail", e);
            }
        }
    }

    private void recoverAppAfterCancel() {
        setTaskVisibleState(true);
        moveTaskToFront(this.mTopTaskId);
        hideBottomViewsDelay(800);
    }

    private void hideBottomViewsDelay(int delayTime) {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "hideBottomViewsDelay delayTime=" + delayTime);
        }
        if (delayTime > 0) {
            this.mHandler.sendEmptyMessageDelayed(7, (long) delayTime);
        } else {
            hideBottomViewsAtEnd();
        }
    }

    private void hideBottomViewsAtEnd() {
        updateAppendFlag(false);
        showBottomContainer(false);
    }

    private boolean startHome() {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "start home activity");
        }
        return this.mDeviceStateController.startHome();
    }
}
