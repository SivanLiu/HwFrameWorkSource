package com.android.server.display;

import android.hardware.display.IDisplayManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog;
import android.view.IHwRotateObserver;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import com.android.server.display.HwBrightnessXmlLoader;
import com.android.server.wm.WindowManagerService;

class TouchProximityDetector {
    private static final int CONTINUE_INVALID_PRINT_PERIOD_MS = 30000;
    /* access modifiers changed from: private */
    public static final boolean HWDEBUG = (Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3)));
    /* access modifiers changed from: private */
    public static final boolean HWFLOW;
    private static final String TAG = "TouchProximityDetector";
    private int mContinueInvalidCnt;
    private final int mContinueInvalidPrintPeriod;
    private volatile boolean mCurrentLuxValid = true;
    /* access modifiers changed from: private */
    public boolean mEnable;
    private WindowManagerService.HwInnerWindowManagerService mHwInnerWindowManagerService;
    /* access modifiers changed from: private */
    public MyRotateObserver mIHwRotateObserver;
    private boolean mInited;
    private MyPointerEventListener mPointerEventListener;
    private WindowManagerService mWindowManagerService;
    /* access modifiers changed from: private */
    public int mXThreshold270Max;
    /* access modifiers changed from: private */
    public int mXThreshold270Min;
    /* access modifiers changed from: private */
    public int mXThreshold90Max;
    /* access modifiers changed from: private */
    public int mXThreshold90Min;
    /* access modifiers changed from: private */
    public int mYThreshold0Max;
    /* access modifiers changed from: private */
    public int mYThreshold0Min;
    /* access modifiers changed from: private */
    public int mYThreshold180Max;
    /* access modifiers changed from: private */
    public int mYThreshold180Min;

    static {
        boolean z = false;
        if (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4))) {
            z = true;
        }
        HWFLOW = z;
    }

    public TouchProximityDetector(HwBrightnessXmlLoader.Data data) {
        this.mContinueInvalidPrintPeriod = 30000 / (data.lightSensorRateMills > 0 ? data.lightSensorRateMills : 300);
        IDisplayManager iDisplayManager = ServiceManager.getService("display");
        if (iDisplayManager == null) {
            Slog.e(TAG, "init failed, display manager is null");
            return;
        }
        this.mWindowManagerService = ServiceManager.getService("window");
        WindowManagerService windowManagerService = this.mWindowManagerService;
        if (windowManagerService == null) {
            Slog.e(TAG, "init failed, window manager is null");
            return;
        }
        this.mHwInnerWindowManagerService = windowManagerService.getHwInnerService();
        if (this.mHwInnerWindowManagerService == null) {
            Slog.e(TAG, "init failed, inner window manager is null");
            return;
        }
        this.mPointerEventListener = new MyPointerEventListener();
        this.mIHwRotateObserver = new MyRotateObserver();
        try {
            int yMaxPixels = iDisplayManager.getStableDisplaySize().y;
            this.mYThreshold0Min = (int) (((float) yMaxPixels) * data.touchProximityYNearbyRatioMin);
            this.mYThreshold0Max = (int) (((float) yMaxPixels) * data.touchProximityYNearbyRatioMax);
            this.mXThreshold90Min = this.mYThreshold0Min;
            this.mXThreshold90Max = this.mYThreshold0Max;
            this.mYThreshold180Min = yMaxPixels - this.mYThreshold0Max;
            this.mYThreshold180Max = yMaxPixels - this.mYThreshold0Min;
            this.mXThreshold270Min = this.mYThreshold180Min;
            this.mXThreshold270Max = this.mYThreshold180Max;
            if (yMaxPixels <= 0) {
                Slog.e(TAG, "init the threeshold failed, invalid parameter value");
                this.mInited = false;
                return;
            }
            this.mInited = true;
        } catch (RemoteException e) {
            Slog.e(TAG, "getStableDisplaySize failed ");
        }
    }

    public void enable() {
        if (this.mInited && !this.mEnable) {
            this.mCurrentLuxValid = true;
            this.mEnable = true;
            this.mPointerEventListener.clear();
            this.mWindowManagerService.registerPointerEventListener(this.mPointerEventListener, 0);
            this.mHwInnerWindowManagerService.registerRotateObserver(this.mIHwRotateObserver);
            if (HWFLOW) {
                Slog.i(TAG, "enable()");
            }
        }
    }

    public void disable() {
        if (this.mInited && this.mEnable) {
            this.mWindowManagerService.unregisterPointerEventListener(this.mPointerEventListener, 0);
            this.mHwInnerWindowManagerService.unregisterRotateObserver(this.mIHwRotateObserver);
            this.mPointerEventListener.clear();
            this.mEnable = false;
            if (HWFLOW) {
                Slog.i(TAG, "disable()");
            }
        }
    }

    public boolean isCurrentLuxValid() {
        if (!this.mInited) {
            return true;
        }
        continueInvalidPrint(!this.mCurrentLuxValid);
        return this.mCurrentLuxValid;
    }

    public void startNextLux() {
        if (this.mInited) {
            this.mCurrentLuxValid = !this.mPointerEventListener.isCovered();
        }
    }

    /* access modifiers changed from: private */
    public void setCurrentLuxInvalid() {
        this.mCurrentLuxValid = false;
    }

    private void continueInvalidPrint(boolean isInvalid) {
        if (isInvalid) {
            int i = this.mContinueInvalidCnt + 1;
            this.mContinueInvalidCnt = i;
            if (i % this.mContinueInvalidPrintPeriod == 0 && HWFLOW) {
                Slog.i(TAG, "lux continue invalid for " + this.mContinueInvalidCnt + " times");
                return;
            }
            return;
        }
        this.mContinueInvalidCnt = 0;
    }

    private class MyPointerEventListener implements WindowManagerPolicyConstants.PointerEventListener {
        private boolean mIsCovered;
        private int mNearbyPointBits;
        private int mUsedOrientation;

        private MyPointerEventListener() {
            this.mIsCovered = false;
            this.mNearbyPointBits = 0;
            this.mUsedOrientation = 0;
        }

        public void onPointerEvent(MotionEvent motionEvent) {
            updatePointState(motionEvent);
        }

        /* JADX WARNING: Code restructure failed: missing block: B:12:0x0023, code lost:
            if (r6 != 6) goto L_0x00c7;
         */
        /* JADX WARNING: Removed duplicated region for block: B:40:0x00cd  */
        /* JADX WARNING: Removed duplicated region for block: B:41:0x00cf  */
        /* JADX WARNING: Removed duplicated region for block: B:43:0x00d2  */
        /* JADX WARNING: Removed duplicated region for block: B:46:0x00d6  */
        /* JADX WARNING: Removed duplicated region for block: B:60:? A[RETURN, SYNTHETIC] */
        private void updatePointState(MotionEvent motionEvent) {
            if (TouchProximityDetector.this.mEnable) {
                int action = motionEvent.getActionMasked();
                boolean stateChanged = false;
                if (action != 0) {
                    if (action != 1) {
                        if (action == 2) {
                            int pointCount = motionEvent.getPointerCount();
                            for (int i = 0; i < pointCount; i++) {
                                if (motionEvent.getToolType(i) == 1) {
                                    float x = motionEvent.getX(i);
                                    float y = motionEvent.getY(i);
                                    int id = motionEvent.getPointerId(i);
                                    if (nearbyProximity(motionEvent.getXPrecision() * x, motionEvent.getYPrecision() * y)) {
                                        this.mNearbyPointBits |= 1 << id;
                                    } else {
                                        this.mNearbyPointBits &= ~(1 << id);
                                    }
                                }
                            }
                        } else if (action != 3) {
                            if (action != 5) {
                            }
                        }
                        if (this.mIsCovered != (this.mNearbyPointBits != 0)) {
                            stateChanged = true;
                        }
                        if (stateChanged) {
                            this.mIsCovered = !this.mIsCovered;
                            if (this.mIsCovered) {
                                TouchProximityDetector.this.setCurrentLuxInvalid();
                            }
                            if (TouchProximityDetector.HWDEBUG) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(this.mIsCovered ? "one more" : "no");
                                sb.append(" finger(s) nearby the proximity sensor");
                                Slog.d(TouchProximityDetector.TAG, sb.toString());
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    if (motionEvent.getToolType(motionEvent.getActionIndex()) == 1) {
                        this.mNearbyPointBits = 0;
                    }
                    if (this.mIsCovered != (this.mNearbyPointBits != 0)) {
                    }
                    if (stateChanged) {
                    }
                }
                int index = motionEvent.getActionIndex();
                if (motionEvent.getToolType(index) == 1) {
                    if (action == 0) {
                        this.mNearbyPointBits = 0;
                        if (TouchProximityDetector.this.mIHwRotateObserver != null) {
                            this.mUsedOrientation = TouchProximityDetector.this.mIHwRotateObserver.getOrientation();
                        }
                    }
                    float x2 = motionEvent.getX(index);
                    float y2 = motionEvent.getY(index);
                    int id2 = motionEvent.getPointerId(index);
                    float realX = x2 * motionEvent.getXPrecision();
                    float realY = y2 * motionEvent.getYPrecision();
                    if (action == 6) {
                        this.mNearbyPointBits &= ~(1 << id2);
                    } else if (nearbyProximity(realX, realY)) {
                        this.mNearbyPointBits |= 1 << id2;
                    }
                }
                if (this.mIsCovered != (this.mNearbyPointBits != 0)) {
                }
                if (stateChanged) {
                }
            }
        }

        private boolean nearbyProximity(float x, float y) {
            int i = this.mUsedOrientation;
            if (i == 0) {
                return y >= ((float) TouchProximityDetector.this.mYThreshold0Min) && y <= ((float) TouchProximityDetector.this.mYThreshold0Max);
            }
            if (i == 1) {
                return x >= ((float) TouchProximityDetector.this.mXThreshold90Min) && x <= ((float) TouchProximityDetector.this.mXThreshold90Max);
            }
            if (i == 2) {
                return y >= ((float) TouchProximityDetector.this.mYThreshold180Min) && y <= ((float) TouchProximityDetector.this.mYThreshold180Max);
            }
            if (i == 3) {
                return x >= ((float) TouchProximityDetector.this.mXThreshold270Min) && x <= ((float) TouchProximityDetector.this.mXThreshold270Max);
            }
            if (TouchProximityDetector.HWFLOW) {
                Slog.i(TouchProximityDetector.TAG, "unknow mOrientation=" + this.mUsedOrientation);
            }
            return false;
        }

        public boolean isCovered() {
            return this.mIsCovered;
        }

        public void clear() {
            this.mIsCovered = false;
        }
    }

    /* access modifiers changed from: private */
    public class MyRotateObserver extends IHwRotateObserver.Stub {
        private int mOrientation;

        private MyRotateObserver() {
            this.mOrientation = 0;
        }

        public int getOrientation() {
            return this.mOrientation;
        }

        public void onRotate(int oldRotation, int newRotation) {
            if (TouchProximityDetector.HWFLOW) {
                Slog.i(TouchProximityDetector.TAG, "RotateObserver onRotate " + oldRotation + "->" + newRotation);
            }
            this.mOrientation = newRotation;
        }
    }
}
