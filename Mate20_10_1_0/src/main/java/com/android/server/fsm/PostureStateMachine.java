package com.android.server.fsm;

import android.content.pm.ActivityInfo;
import android.hardware.display.DisplayManagerInternal;
import android.os.Binder;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.LocalServices;
import com.android.server.gesture.GestureNavPolicy;
import com.huawei.android.app.ActivityManagerEx;
import huawei.android.hardware.tp.HwTpManager;
import java.util.HashMap;
import java.util.Map;

public class PostureStateMachine extends StateMachine {
    private static final int HANDHELD_MAIN = 1;
    private static final int HANDHELD_START_FLIP = 100;
    private static final int HANDHELD_STOP_FLIP = 101;
    private static final int HANDHELD_SUB = 2;
    private static final boolean IS_PROP_FOLD_SWITCH_DEBUG = SystemProperties.getBoolean("persist.debug.fold_switch", true);
    public static final int SCREEN_OFF_WHEN_CALL_COMING = 2;
    public static final int SCREEN_ON_WHEN_CALL_COMING = 1;
    private static final int SET_DISPLAY_MODE_DELAY_DEFAULT = 150;
    private static final int SM_CHANGE_DISPLAYMODE_CMD = 1;
    private static final int SM_CHANGE_POSTURE_CMD = 0;
    private static final int SM_MILLIS_TO_SECOND = 1000;
    private static final int SM_SCREEN_OFF = 1;
    private static final int SM_SCREEN_ON = 0;
    private static final int SM_SCREEN_STAY_ON = 2;
    private static final int SM_USE_LASTMODE_CMD = 2;
    private static final int SM_USE_LASTMODE_TIMEOUT = 2000;
    private static final String TAG = "Fsm_PostureStateMachine";
    public static final int UNKNOW_WHEN_CALL_COMING = 0;
    private static PostureStateMachine sInstance = null;
    /* access modifiers changed from: private */
    public int mDisplayMode = 0;
    private int mDisplayModeSleep = 0;
    /* access modifiers changed from: private */
    public int mDisplayRectForDoubleClick = 0;
    private final DisplayManagerInternal mDm = ((DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class));
    private int mFoldState = 0;
    private int mFoldStateChangeCount = 0;
    private State mFoldedState = new FoldedState();
    private State mFullState = new FullState();
    private State mHalfFoldedState = new HalfFoldedState();
    private State mHandheldFoldedMainState = new HandheldFoldedMainState();
    private State mHandheldFoldedSubState = new HandheldFoldedSubState();
    private boolean mIsPickUp = false;
    private volatile int mIsScreenOnWhenCallComing = 0;
    /* access modifiers changed from: private */
    public int mIsTest = SystemProperties.getInt("persist.sys.testDisplayMode", 0);
    /* access modifiers changed from: private */
    public int mLastDisplayMode = 0;
    private long mLastFoldStateTime = 0;
    private long mLastdisplayModeTime = 0;
    private State mLayFlatMainUpState = new LayFlatMainUpState();
    private State mLayFlatSubUpState = new LayFlatSubUpState();
    private final Object mLock = new Object();
    /* access modifiers changed from: private */
    public int mPosture = 100;
    private final Map<Integer, State> mPostureMap = new HashMap(7);
    /* access modifiers changed from: private */
    public HwFoldScreenManagerService mService;
    private HwTpManager mTpManager;

    private PostureStateMachine(String name) {
        super(name);
        initStateMap();
    }

    public static synchronized PostureStateMachine getInstance() {
        PostureStateMachine postureStateMachine;
        synchronized (PostureStateMachine.class) {
            if (sInstance == null) {
                sInstance = new PostureStateMachine("Fsm_PostureStateMachine");
            }
            postureStateMachine = sInstance;
        }
        return postureStateMachine;
    }

    private void initStateMap() {
        Map<Integer, State> map = this.mPostureMap;
        if (map != null) {
            map.put(101, this.mLayFlatMainUpState);
            this.mPostureMap.put(102, this.mLayFlatSubUpState);
            this.mPostureMap.put(103, this.mFoldedState);
            this.mPostureMap.put(104, this.mHandheldFoldedMainState);
            this.mPostureMap.put(105, this.mHandheldFoldedSubState);
            this.mPostureMap.put(106, this.mHalfFoldedState);
            this.mPostureMap.put(109, this.mFullState);
        }
    }

    private State getInitState() {
        State initState = this.mFullState;
        int mode = SystemProperties.getInt("persist.sys.foldDispMode", 0);
        if (mode == 1) {
            return this.mFullState;
        }
        if (mode == 2) {
            return this.mLayFlatMainUpState;
        }
        if (mode == 3) {
            return this.mLayFlatSubUpState;
        }
        Slog.w("Fsm_PostureStateMachine", "getInitState SysFoldDisplayMode = " + mode);
        return initState;
    }

    /* access modifiers changed from: package-private */
    public void init(HwFoldScreenManagerService service) {
        this.mService = service;
        addStates();
        setInitialState(getInitState());
    }

    private void addStates() {
        for (int i = 101; i <= 109; i++) {
            State state = this.mPostureMap.get(Integer.valueOf(i));
            if (state != null) {
                addState(state);
            }
        }
    }

    public void setScreeStateWhenCallComing(int state) {
        this.mIsScreenOnWhenCallComing = state;
    }

    private String displayModeToString(int displayMode) {
        if (displayMode == 0) {
            return "DISPLAY_MODE_UNKNOWN";
        }
        if (displayMode == 1) {
            return "DISPLAY_MODE_FULL";
        }
        if (displayMode == 2) {
            return "DISPLAY_MODE_MAIN";
        }
        if (displayMode == 3) {
            return "DISPLAY_MODE_SUB";
        }
        if (displayMode == 4) {
            return "DISPLAY_MODE_COORDINATION";
        }
        return "displayModeToString error displayMode:" + displayMode;
    }

    private int transformDisplayMode(int displayMode) {
        boolean isIntelligentOn = this.mService.isIntelligentOn();
        boolean isCameraOn = this.mService.isCameraOn();
        boolean isIncall = this.mService.isIncall();
        int ret = displayMode;
        if (Log.HWINFO) {
            Slog.i("Fsm_PostureStateMachine", "transformDisplayMode isIntelligentOn=" + isIntelligentOn + " isCameraOn=" + isCameraOn + " isIncall=" + isIncall + " ScreeStateWhenCallComing=" + this.mIsScreenOnWhenCallComing);
        }
        if (isIntelligentOn && !isCameraOn) {
            if (this.mIsScreenOnWhenCallComing == 2 && isIncall) {
                ret = transformDisplayModeForCall(displayMode);
            } else if (this.mIsScreenOnWhenCallComing == 1 && displayMode == 2) {
                Slog.i("Fsm_PostureStateMachine", "change WhenCallComing frome SCREEN_ON to SCREEN_OFF");
                setScreeStateWhenCallComing(2);
                ret = 2;
            } else {
                Slog.e("Fsm_PostureStateMachine", "transformDisplayMode error displayMode:" + displayMode + "change WhenCallComing: " + this.mIsScreenOnWhenCallComing);
            }
        }
        if (!isIntelligentOn) {
            boolean isNeedCheck = false;
            if (this.mDisplayMode == 1 && displayMode == 2) {
                isNeedCheck = true;
            }
            if (isNeedCheck && this.mService.isFrontCameraOn()) {
                ret = 3;
            }
        }
        if (Log.HWINFO) {
            Slog.i("Fsm_PostureStateMachine", "transformDisplayMode from " + displayModeToString(displayMode) + " to " + displayModeToString(ret));
        }
        return ret;
    }

    private int transformDisplayModeForCall(int displayMode) {
        int ret = displayMode;
        if (!(displayMode == 0 || displayMode == 1)) {
            if (displayMode == 2 || displayMode == 3 || displayMode == 4) {
                ret = 2;
            } else {
                Slog.e("Fsm_PostureStateMachine", "transformDisplayModeForCall error displayMode:" + displayMode);
            }
        }
        if (Log.HWINFO) {
            Slog.i("Fsm_PostureStateMachine", "transformDisplayModeForCall from " + displayModeToString(displayMode) + " to " + displayModeToString(ret));
        }
        return ret;
    }

    private boolean displayModinnerDebug(int displayMode) {
        if (this.mDisplayMode == 0) {
            this.mService.resetDispModeChange();
        }
        if (this.mService.isPausedDispModeChange()) {
            Slog.w("Fsm_PostureStateMachine", "setting display mode to be deferred,  setDeferredDispMode=" + displayMode);
            this.mService.setDeferredDispMode(displayMode);
            return false;
        }
        handlePauseDispModeChange();
        return true;
    }

    /* access modifiers changed from: private */
    public void setDisplayModeInner(int sourceDisplayMode, boolean isNeedClear) {
        int displayMode = transformDisplayMode(sourceDisplayMode);
        if (displayMode == this.mDisplayMode) {
            Slog.i("Fsm_PostureStateMachine", "displayMode not change");
            if (IS_PROP_FOLD_SWITCH_DEBUG) {
                handleResumeDispModeChange();
            }
        } else if (!IS_PROP_FOLD_SWITCH_DEBUG || displayModinnerDebug(displayMode)) {
            int lastDisplayMode = this.mDisplayMode;
            this.mDisplayMode = displayMode;
            Slog.i("Fsm_PostureStateMachine", "setDisplayModeInner mDisplayMode:" + this.mDisplayMode);
            this.mService.notifyDispalyModeChangePrepare(displayMode);
            if (lastDisplayMode == 0) {
                int wakeupType = 1;
                if (this.mDisplayRectForDoubleClick != 0) {
                    wakeupType = 0;
                    this.mDisplayRectForDoubleClick = 0;
                }
                this.mService.bdReport(10104, "{wakeupType:" + wakeupType + ", posture:" + getPosture() + ", displayMode:" + displayMode + "}");
                setDisplayModeInnerDelay(displayMode, isNeedClear, true);
            } else {
                Message msg = Message.obtain(getHandler(), 1);
                msg.arg1 = displayMode;
                msg.arg2 = isNeedClear ? 1 : 0;
                getHandler().removeMessages(1);
                sendMessageDelayed(msg, 150);
            }
            this.mLastdisplayModeTime = SystemClock.uptimeMillis();
        }
    }

    private void setDisplayModeInnerDelay(int displayMode, boolean isNeedClear, boolean isWakeup) {
        int foldedState = getFoldableState();
        if (isNeedClear) {
            long origId = Binder.clearCallingIdentity();
            try {
                this.mDm.setDisplayMode(displayMode, foldedState, isWakeup);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        } else {
            this.mDm.setDisplayMode(displayMode, foldedState, isWakeup);
        }
        this.mService.notifyDisplayModeChange(displayMode);
    }

    /* access modifiers changed from: protected */
    public void onPostHandleMessage(Message msg) {
        int i = msg.what;
        int i2 = 0;
        boolean z = true;
        if (i == 0) {
            Slog.i("Fsm_PostureStateMachine", "posture changed, need notify");
            this.mService.notifyPostureChange(getPosture());
            int currentFoldState = getFoldableState();
            if (currentFoldState != this.mFoldState) {
                GestureNavPolicy mGestureNavPolicy = (GestureNavPolicy) LocalServices.getService(GestureNavPolicy.class);
                if (!(mGestureNavPolicy == null || this.mFoldState == 0)) {
                    mGestureNavPolicy.onMultiWindowChanged(2);
                }
                long timeDiff = 0;
                long currentTime = SystemClock.uptimeMillis();
                String eventMsg = "{lastFoldState:" + this.mFoldState + ", currentFoldState:" + currentFoldState;
                if (this.mFoldState != 0) {
                    timeDiff = currentTime - this.mLastFoldStateTime;
                }
                StringBuilder sb = new StringBuilder();
                sb.append(eventMsg);
                sb.append(", state:");
                if (this.mFoldState != 0) {
                    i2 = 2;
                }
                sb.append(i2);
                String eventMsg2 = sb.toString() + ", time:" + ((((double) timeDiff) * 1.0d) / 1000.0d);
                ActivityInfo ai = ActivityManagerEx.getLastResumedActivity();
                if (ai != null) {
                    eventMsg2 = eventMsg2 + ", package:" + ai.packageName;
                }
                this.mService.bdReport(10105, eventMsg2 + "}");
                this.mLastFoldStateTime = currentTime;
                this.mFoldState = currentFoldState;
                this.mService.notifyFoldStateChange(this.mFoldState);
                this.mFoldStateChangeCount++;
            }
        } else if (i == 1) {
            Slog.i("Fsm_PostureStateMachine", "setDisplayModeInnerDelay 150ms");
            int i3 = msg.arg1;
            if (1 != msg.arg2) {
                z = false;
            }
            setDisplayModeInnerDelay(i3, z, false);
        } else if (i == 2) {
            this.mLastDisplayMode = 0;
            if (this.mDisplayModeSleep == 3) {
                Slog.i("Fsm_PostureStateMachine", "sub display mode sleep to draw window.");
                this.mDisplayModeSleep = 2;
                this.mService.setDrawWindowFlag(true);
            }
        }
    }

    private String postureToString(int posture) {
        if (posture == 109) {
            return "POSTURE_FULL";
        }
        switch (posture) {
            case 100:
                return "POSTURE_UNKNOWN";
            case 101:
                return "POSTURE_LAY_FLAT_MAIN_UP";
            case 102:
                return "POSTURE_LAY_FLAT_SUB_UP";
            case 103:
                return "POSTURE_FOLDED";
            case 104:
                return "POSTURE_HANDHELD_FOLDED_MAIN";
            case 105:
                return "POSTURE_HANDHELD_FOLDED_SUB";
            case 106:
                return "POSTURE_HALF_FOLDED";
            default:
                return "postureToString, error posture:" + posture;
        }
    }

    private int transformPostureForCall(int posture) {
        int ret = posture;
        if (posture != 109) {
            switch (posture) {
                case 100:
                case 106:
                    break;
                case 101:
                case 102:
                case 103:
                case 104:
                case 105:
                    ret = 101;
                    break;
                default:
                    Slog.e("Fsm_PostureStateMachine", "transformPostureForCall error posture:" + posture);
                    break;
            }
        }
        if (Log.HWINFO) {
            Slog.i("Fsm_PostureStateMachine", "transformPostureForCall from " + postureToString(posture) + " to " + postureToString(ret));
        }
        return ret;
    }

    private int transformPosture(int posture) {
        boolean isIntelligentOn = this.mService.isIntelligentOn();
        boolean isCameraOn = this.mService.isCameraOn();
        boolean isIncall = this.mService.isIncall();
        int ret = posture;
        if (Log.HWINFO) {
            Slog.i("Fsm_PostureStateMachine", "transformPosture isIntelligentOn=" + isIntelligentOn + " isCameraOn=" + isCameraOn + " isIncall=" + isIncall + " ScreeStateWhenCallComing=" + this.mIsScreenOnWhenCallComing);
        }
        if (isIntelligentOn && !isCameraOn) {
            int i = this.mIsScreenOnWhenCallComing;
            if (i != 1) {
                if (i != 2) {
                    Slog.e("Fsm_PostureStateMachine", "transformPosture error ScreeStateWhenCallComing:" + this.mIsScreenOnWhenCallComing);
                } else if (isIncall) {
                    ret = transformPostureForCall(posture);
                }
            } else if (posture == 101 || posture == 103 || posture == 104) {
                if (Log.HWINFO) {
                    Slog.i("Fsm_PostureStateMachine", "transform ScreeStateWhenCallComing, frome SCREEN_ON to SCREEN_OFF");
                }
                setScreeStateWhenCallComing(2);
                ret = 101;
            } else {
                Slog.e("Fsm_PostureStateMachine", "transformPosture error posture:" + posture);
            }
        }
        if (Log.HWINFO) {
            Slog.i("Fsm_PostureStateMachine", "transformPosture from " + postureToString(posture) + " to " + postureToString(ret));
        }
        return ret;
    }

    public int getFoldStateChangeCount() {
        return this.mFoldStateChangeCount;
    }

    /* access modifiers changed from: package-private */
    public void setPosture(int posture) {
        setPosture(posture, false);
    }

    /* access modifiers changed from: package-private */
    public void setPosture(int sourcePosture, boolean isUseLast) {
        this.mIsTest = SystemProperties.getInt("persist.sys.testDisplayMode", 0);
        if (!isUseLast) {
            this.mLastDisplayMode = 0;
        }
        int posture = transformPosture(sourcePosture);
        getHandler().removeMessages(2);
        this.mService.removeForceWakeUp();
        transitionTo((IState) this.mPostureMap.get(Integer.valueOf(preparePostureForPickWakeUp(posture))));
        sendMessage(0);
    }

    /* access modifiers changed from: package-private */
    public void setPickupWakeUp(boolean isPickUp) {
        synchronized (this.mLock) {
            Slog.i("Fsm_PostureStateMachine", "setPickupWakeUp old mIsPickUp = " + this.mIsPickUp + " new isPickUp = " + isPickUp);
            this.mIsPickUp = isPickUp;
        }
    }

    private int preparePostureForPickWakeUp(int posture) {
        synchronized (this.mLock) {
            if (this.mIsPickUp) {
                Slog.i("Fsm_PostureStateMachine", "preparePostureForPickWakeUp posture : " + posture);
                this.mLastDisplayMode = 0;
                this.mIsPickUp = false;
                if (posture == 102 || posture == 101 || posture == 103) {
                    return 103;
                }
            }
            return posture;
        }
    }

    public int getPosture() {
        IState posture = getCurrentState();
        if (posture == null || !(posture instanceof PostureState)) {
            return 100;
        }
        return ((PostureState) posture).getPosture();
    }

    public int getFoldableState() {
        IState posture = getCurrentState();
        if (posture == null || !(posture instanceof PostureState)) {
            return 0;
        }
        return ((PostureState) posture).getFoldableState();
    }

    public int setDisplayMode(int mode) {
        IState posture = getCurrentState();
        Slog.d("Fsm_PostureStateMachine", "setDisplayMode mode=" + mode + " CurrentPosture=" + posture);
        if (posture == null || !(posture instanceof PostureState)) {
            return 0;
        }
        int result = ((PostureState) posture).setDisplayMode(mode);
        if (this.mService.isScreenOn() || this.mDisplayModeSleep != 4) {
            return result;
        }
        notifySleep();
        return result;
    }

    public int getDisplayMode() {
        int i = this.mDisplayMode;
        if (i == 0) {
            return this.mDisplayModeSleep;
        }
        return i;
    }

    /* access modifiers changed from: protected */
    public void setDisplayRectForDoubleClick(int displayRect) {
        this.mDisplayRectForDoubleClick = displayRect;
    }

    /* access modifiers changed from: protected */
    public int doubleClickToSetDisplayMode(int displayRect) {
        int displayMode;
        int posture = getPosture();
        Slog.i("Fsm_PostureStateMachine", "doubleClickToSetDisplayMode displayRect:" + displayRect + ", posture:" + posture + "displayMode: " + this.mDisplayMode);
        int i = this.mDisplayMode;
        if (i == 4 || displayRect == 2 || displayRect == 0) {
            return this.mDisplayMode;
        }
        if (posture == 109) {
            return i;
        }
        if (displayRect == 1) {
            displayMode = 2;
        } else {
            displayMode = 3;
        }
        long currentTime = SystemClock.uptimeMillis();
        this.mService.bdReport(10103, "{posture:" + posture + ", lastDisplayMode:" + this.mDisplayMode + ", displayMode:" + displayMode + ", time:" + ((((double) (currentTime - this.mLastdisplayModeTime)) * 1.0d) / 1000.0d) + "}");
        return setDisplayMode(displayMode);
    }

    /* access modifiers changed from: protected */
    public void handleFlipPosture() {
        int posture = getPosture();
        Slog.i("Fsm_PostureStateMachine", "handleFlipPosture posture:" + posture + ",DisplayMode: " + this.mDisplayMode);
        if (this.mDisplayMode != 4 && posture != 109 && posture != 106) {
            int handHeld = getHandheldPosture();
            Slog.i("Fsm_PostureStateMachine", "handleFlipPosture handHeld:" + handHeld);
            if (handHeld == 1 && this.mDisplayMode == 2) {
                setDisplayMode(3);
            } else if (handHeld == 2 && this.mDisplayMode == 3) {
                setDisplayMode(2);
            } else if (handHeld == 100) {
                this.mService.freezeFoldRotation();
                return;
            } else if (handHeld == 101) {
                this.mService.unFreezeFoldRotation();
                return;
            } else {
                return;
            }
            setPosture(103);
        }
    }

    private int getHandheldPosture() {
        if (this.mTpManager == null) {
            this.mTpManager = HwTpManager.getInstance();
        }
        return this.mTpManager.hwTsSetAftConfig("version:3+grab_gesture");
    }

    /* access modifiers changed from: protected */
    public void notifySleep() {
        int lastDisplayMode = this.mDisplayMode;
        if (lastDisplayMode != 0) {
            this.mDisplayModeSleep = lastDisplayMode;
        }
        this.mDisplayMode = 0;
        this.mPosture = 100;
        Slog.i("Fsm_PostureStateMachine", "notifySleep mDisplayMode: 0, SleepMode = " + this.mDisplayModeSleep);
        getHandler().removeMessages(1);
        if (lastDisplayMode == 2 || lastDisplayMode == 3) {
            getHandler().removeMessages(2);
            getHandler().sendEmptyMessageDelayed(2, 2000);
            this.mLastDisplayMode = lastDisplayMode;
            Slog.i("Fsm_PostureStateMachine", "LastDisplayMode:" + this.mLastDisplayMode);
        } else {
            this.mLastDisplayMode = 0;
        }
        long currentTime = SystemClock.uptimeMillis();
        String eventMsg = "{lastFoldState:" + this.mFoldState + ", currentFoldState:" + 0 + ", state:" + 1 + ", time:" + ((((double) (currentTime - this.mLastFoldStateTime)) * 1.0d) / 1000.0d);
        ActivityInfo ai = ActivityManagerEx.getLastResumedActivity();
        if (ai != null) {
            eventMsg = eventMsg + ", package:" + ai.packageName;
        }
        this.mService.bdReport(10105, eventMsg + "}");
        this.mFoldState = 0;
        this.mService.unFreezeFoldRotation();
    }

    private void handleResumeDispModeChange() {
        Slog.d("Fsm_PostureStateMachine", "handleResumeDispModeChange in PSM");
        this.mService.resumeDispModeChange();
        this.mService.unFreezeFoldRotation();
    }

    private void handlePauseDispModeChange() {
        if (this.mDisplayMode != 0) {
            Slog.d("Fsm_PostureStateMachine", "handlePauseDispModeChange in PSM");
            this.mService.pauseDispModeChange();
            this.mService.freezeFoldRotation();
        }
    }

    private abstract class PostureState extends State {
        /* access modifiers changed from: protected */
        public abstract int getFoldableState();

        /* access modifiers changed from: protected */
        public abstract int getPosture();

        private PostureState() {
        }

        /* access modifiers changed from: protected */
        public boolean isNeedEnter() {
            Slog.d("Fsm_PostureStateMachine", getName() + " enter. mDisplayRectForDoubleClick = " + PostureStateMachine.this.mDisplayRectForDoubleClick + "DisplayMode: " + PostureStateMachine.this.mDisplayMode);
            if (PostureStateMachine.this.mDisplayMode == 4) {
                return false;
            }
            if (PostureStateMachine.this.mDisplayRectForDoubleClick == 2) {
                int unused = PostureStateMachine.this.mDisplayRectForDoubleClick = 0;
                return false;
            } else if (PostureStateMachine.this.mDisplayRectForDoubleClick == 4) {
                PostureStateMachine.this.setDisplayModeInner(3, false);
                return false;
            } else if (PostureStateMachine.this.mDisplayRectForDoubleClick != 1) {
                return true;
            } else {
                PostureStateMachine.this.setDisplayModeInner(2, false);
                return false;
            }
        }

        /* access modifiers changed from: protected */
        public int setDisplayMode(int mode) {
            Slog.d("Fsm_PostureStateMachine", getName() + ": setDisplayMode mode = " + mode + " old DisplayMode = " + PostureStateMachine.this.mDisplayMode);
            if (PostureStateMachine.this.mIsTest == 0 && mode == 1) {
                return PostureStateMachine.this.mDisplayMode;
            }
            if (PostureStateMachine.this.mDisplayMode != mode) {
                int tempDisplayMode = PostureStateMachine.this.mDisplayMode;
                PostureStateMachine.this.setDisplayModeInner(mode, true);
                if (tempDisplayMode == 4) {
                    PostureStateMachine.this.mService.exitCoordinationDisplayMode();
                }
            }
            return PostureStateMachine.this.mDisplayMode;
        }
    }

    private class FoldedState extends PostureState {
        private FoldedState() {
            super();
        }

        public void enter() {
            if (super.isNeedEnter()) {
                if (PostureStateMachine.this.mLastDisplayMode != 0) {
                    PostureStateMachine postureStateMachine = PostureStateMachine.this;
                    postureStateMachine.setDisplayModeInner(postureStateMachine.mLastDisplayMode, false);
                    int unused = PostureStateMachine.this.mLastDisplayMode = 0;
                } else if (PostureStateMachine.this.mPosture == 109 || PostureStateMachine.this.mPosture == 103 || PostureStateMachine.this.mPosture == 100) {
                    PostureStateMachine.this.setDisplayModeInner(2, false);
                }
            }
            int unused2 = PostureStateMachine.this.mPosture = 103;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int getPosture() {
            return 103;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int getFoldableState() {
            return 2;
        }
    }

    private class LayFlatMainUpState extends PostureState {
        private LayFlatMainUpState() {
            super();
        }

        public void enter() {
            if (super.isNeedEnter()) {
                PostureStateMachine.this.setDisplayModeInner(2, false);
            }
            int unused = PostureStateMachine.this.mPosture = 101;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int getPosture() {
            return 101;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int getFoldableState() {
            return 2;
        }
    }

    private class LayFlatSubUpState extends PostureState {
        private LayFlatSubUpState() {
            super();
        }

        public void enter() {
            if (super.isNeedEnter()) {
                PostureStateMachine.this.setDisplayModeInner(3, false);
            }
            int unused = PostureStateMachine.this.mPosture = 102;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int getPosture() {
            return 102;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int getFoldableState() {
            return 2;
        }
    }

    private class HandheldFoldedMainState extends PostureState {
        private HandheldFoldedMainState() {
            super();
        }

        public void enter() {
            if (super.isNeedEnter()) {
                PostureStateMachine.this.setDisplayModeInner(3, false);
            }
            int unused = PostureStateMachine.this.mPosture = 104;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int getPosture() {
            return 104;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int getFoldableState() {
            return 2;
        }
    }

    private class HandheldFoldedSubState extends PostureState {
        private HandheldFoldedSubState() {
            super();
        }

        public void enter() {
            if (super.isNeedEnter()) {
                PostureStateMachine.this.setDisplayModeInner(2, false);
            }
            int unused = PostureStateMachine.this.mPosture = 105;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int getPosture() {
            return 105;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int getFoldableState() {
            return 2;
        }
    }

    private class HalfFoldedState extends PostureState {
        private HalfFoldedState() {
            super();
        }

        public void enter() {
            if (super.isNeedEnter() && (PostureStateMachine.this.mPosture == 109 || PostureStateMachine.this.mPosture == 100)) {
                PostureStateMachine.this.setDisplayModeInner(2, false);
            }
            int unused = PostureStateMachine.this.mPosture = 106;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int getPosture() {
            return 106;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int getFoldableState() {
            return 3;
        }
    }

    private class FullState extends PostureState {
        private FullState() {
            super();
        }

        public void enter() {
            Slog.d("Fsm_PostureStateMachine", "FullState enter. mDisplayRectForDoubleClick = " + PostureStateMachine.this.mDisplayRectForDoubleClick + " DisplayMode: " + PostureStateMachine.this.mDisplayMode);
            int tempDisplayMode = PostureStateMachine.this.mDisplayMode;
            PostureStateMachine.this.setDisplayModeInner(1, false);
            if (tempDisplayMode == 4) {
                PostureStateMachine.this.mService.exitCoordinationDisplayMode();
            }
            int unused = PostureStateMachine.this.mPosture = 109;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int getPosture() {
            return 109;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int getFoldableState() {
            return 1;
        }

        @Override // com.android.server.fsm.PostureStateMachine.PostureState
        public int setDisplayMode(int mode) {
            Slog.d("Fsm_PostureStateMachine", "FullState: setDisplayMode mode = " + mode + " old DisplayMode = " + PostureStateMachine.this.mDisplayMode);
            if (PostureStateMachine.this.mIsTest == 0) {
                if (mode != 1) {
                    return PostureStateMachine.this.mDisplayMode;
                }
            } else if (mode == 4) {
                return PostureStateMachine.this.mDisplayMode;
            }
            if (PostureStateMachine.this.mDisplayMode != mode) {
                int tmpDisplayMode = PostureStateMachine.this.mDisplayMode;
                PostureStateMachine.this.setDisplayModeInner(mode, true);
                if (tmpDisplayMode == 4) {
                    PostureStateMachine.this.mService.exitCoordinationDisplayMode();
                }
            }
            return PostureStateMachine.this.mDisplayMode;
        }
    }
}
