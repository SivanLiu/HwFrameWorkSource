package com.android.server.display;

import android.hardware.display.IDisplayManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog;
import android.view.IHwRotateObserver.Stub;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants.PointerEventListener;
import com.android.server.display.HwBrightnessXmlLoader.Data;
import com.android.server.gesture.GestureNavConst;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowManagerService.HwInnerWindowManagerService;

class TouchProximityDetector {
    private static final int CONTINUE_INVALID_PRINT_PERIOD_MS = 30000;
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final String TAG = "TouchProximityDetector";
    private int mContinueInvalidCnt;
    private final int mContinueInvalidPrintPeriod;
    private volatile boolean mCurrentLuxValid = true;
    private boolean mEnable;
    private HwInnerWindowManagerService mHwInnerWindowManagerService;
    private MyRotateObserver mIHwRotateObserver;
    private boolean mInited;
    private MyPointerEventListener mPointerEventListener;
    private WindowManagerService mWindowManagerService;
    private int mXThreshold270;
    private int mXThreshold90;
    private int mYMaxPixels;
    private int mYThreshold0;
    private int mYThreshold180;

    private class MyPointerEventListener implements PointerEventListener {
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

        private void updatePointState(MotionEvent motionEvent) {
            if (TouchProximityDetector.this.mEnable) {
                int action = motionEvent.getActionMasked();
                boolean stateChanged = false;
                float y;
                int id;
                float realX;
                float realY;
                switch (action) {
                    case 0:
                    case 5:
                    case 6:
                        int index = motionEvent.getActionIndex();
                        if (motionEvent.getToolType(index) == 1) {
                            if (action == 0) {
                                this.mNearbyPointBits = 0;
                                if (TouchProximityDetector.this.mIHwRotateObserver != null) {
                                    this.mUsedOrientation = TouchProximityDetector.this.mIHwRotateObserver.getOrientation();
                                }
                            }
                            float x = motionEvent.getX(index);
                            y = motionEvent.getY(index);
                            id = motionEvent.getPointerId(index);
                            realX = x * motionEvent.getXPrecision();
                            realY = y * motionEvent.getYPrecision();
                            if (action != 6) {
                                if (nearbyProximity(realX, realY)) {
                                    this.mNearbyPointBits |= 1 << id;
                                    break;
                                }
                            }
                            this.mNearbyPointBits &= ~(1 << id);
                            break;
                        }
                        break;
                    case 1:
                    case 3:
                        if (motionEvent.getToolType(motionEvent.getActionIndex()) == 1) {
                            this.mNearbyPointBits = 0;
                            break;
                        }
                        break;
                    case 2:
                        int pointCount = motionEvent.getPointerCount();
                        float realY2 = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                        realY = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                        realX = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                        y = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                        for (int i = 0; i < pointCount; i++) {
                            if (motionEvent.getToolType(i) == 1) {
                                y = motionEvent.getX(i);
                                realX = motionEvent.getY(i);
                                id = motionEvent.getPointerId(i);
                                float realX2 = motionEvent.getXPrecision() * y;
                                realY = motionEvent.getYPrecision() * realX;
                                if (nearbyProximity(realX2, realY)) {
                                    this.mNearbyPointBits |= 1 << id;
                                } else {
                                    this.mNearbyPointBits &= ~(1 << id);
                                }
                                realY2 = realY;
                                realY = realX2;
                            }
                        }
                        break;
                }
                if (this.mIsCovered != (this.mNearbyPointBits != 0)) {
                    stateChanged = true;
                }
                if (stateChanged) {
                    this.mIsCovered = 1 ^ this.mIsCovered;
                    if (this.mIsCovered) {
                        TouchProximityDetector.this.setCurrentLuxInvalid();
                    }
                    if (TouchProximityDetector.HWDEBUG) {
                        String str = TouchProximityDetector.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(this.mIsCovered ? "one more" : "no");
                        stringBuilder.append(" finger(s) nearby the proximity sensor");
                        Slog.d(str, stringBuilder.toString());
                    }
                }
            }
        }

        private boolean nearbyProximity(float x, float y) {
            boolean z = true;
            switch (this.mUsedOrientation) {
                case 0:
                    if (y <= ((float) TouchProximityDetector.this.mYThreshold0)) {
                        z = false;
                    }
                    return z;
                case 1:
                    if (x <= ((float) TouchProximityDetector.this.mXThreshold90)) {
                        z = false;
                    }
                    return z;
                case 2:
                    if (y >= ((float) TouchProximityDetector.this.mYThreshold180)) {
                        z = false;
                    }
                    return z;
                case 3:
                    if (x >= ((float) TouchProximityDetector.this.mXThreshold270)) {
                        z = false;
                    }
                    return z;
                default:
                    if (TouchProximityDetector.HWFLOW) {
                        String str = TouchProximityDetector.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unknow mOrientation=");
                        stringBuilder.append(this.mUsedOrientation);
                        Slog.i(str, stringBuilder.toString());
                    }
                    return false;
            }
        }

        public boolean isCovered() {
            return this.mIsCovered;
        }

        public void clear() {
            this.mIsCovered = false;
        }
    }

    private class MyRotateObserver extends Stub {
        private int mOrientation;

        private MyRotateObserver() {
            this.mOrientation = 0;
        }

        public int getOrientation() {
            return this.mOrientation;
        }

        public void onRotate(int oldRotation, int newRotation) {
            if (TouchProximityDetector.HWFLOW) {
                String str = TouchProximityDetector.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RotateObserver onRotate ");
                stringBuilder.append(oldRotation);
                stringBuilder.append("->");
                stringBuilder.append(newRotation);
                Slog.i(str, stringBuilder.toString());
            }
            this.mOrientation = newRotation;
        }
    }

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        HWFLOW = z;
    }

    public TouchProximityDetector(Data data) {
        this.mContinueInvalidPrintPeriod = 30000 / (data.lightSensorRateMills > 0 ? data.lightSensorRateMills : 300);
        IDisplayManager iDisplayManager = (IDisplayManager) ServiceManager.getService("display");
        if (iDisplayManager == null) {
            Slog.e(TAG, "init failed, display manager is null");
            return;
        }
        this.mWindowManagerService = (WindowManagerService) ServiceManager.getService("window");
        if (this.mWindowManagerService == null) {
            Slog.e(TAG, "init failed, window manager is null");
            return;
        }
        this.mHwInnerWindowManagerService = (HwInnerWindowManagerService) this.mWindowManagerService.getHwInnerService();
        if (this.mHwInnerWindowManagerService == null) {
            Slog.e(TAG, "init failed, inner window manager is null");
            return;
        }
        this.mPointerEventListener = new MyPointerEventListener();
        this.mIHwRotateObserver = new MyRotateObserver();
        try {
            this.mYMaxPixels = iDisplayManager.getStableDisplaySize().y;
            this.mYThreshold0 = this.mYMaxPixels - ((int) (((float) this.mYMaxPixels) * data.touchProximityYNearbyRatio));
            this.mXThreshold90 = this.mYThreshold0;
            this.mXThreshold270 = (int) (((float) this.mYMaxPixels) * data.touchProximityYNearbyRatio);
            this.mYThreshold180 = this.mXThreshold270;
            if (this.mYMaxPixels > 0 && this.mYThreshold0 > 0) {
                if (this.mXThreshold270 < this.mYMaxPixels) {
                    this.mInited = true;
                }
            }
            Slog.e(TAG, "init the threeshold failed, invalid parameter value");
            this.mInited = false;
        } catch (RemoteException e) {
            Slog.e(TAG, "getStableDisplaySize failed ");
        }
    }

    public void enable() {
        if (this.mInited && !this.mEnable) {
            this.mCurrentLuxValid = true;
            this.mEnable = true;
            this.mPointerEventListener.clear();
            this.mWindowManagerService.registerPointerEventListener(this.mPointerEventListener);
            this.mHwInnerWindowManagerService.registerRotateObserver(this.mIHwRotateObserver);
            if (HWFLOW) {
                Slog.i(TAG, "enable()");
            }
        }
    }

    public void disable() {
        if (this.mInited && this.mEnable) {
            this.mWindowManagerService.unregisterPointerEventListener(this.mPointerEventListener);
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
        continueInvalidPrint(this.mCurrentLuxValid ^ 1);
        return this.mCurrentLuxValid;
    }

    public void startNextLux() {
        if (this.mInited) {
            this.mCurrentLuxValid = this.mPointerEventListener.isCovered() ^ 1;
        }
    }

    private void setCurrentLuxInvalid() {
        this.mCurrentLuxValid = false;
    }

    private void continueInvalidPrint(boolean isInvalid) {
        if (isInvalid) {
            int i = this.mContinueInvalidCnt + 1;
            this.mContinueInvalidCnt = i;
            if (i % this.mContinueInvalidPrintPeriod == 0 && HWFLOW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("lux continue invalid for ");
                stringBuilder.append(this.mContinueInvalidCnt);
                stringBuilder.append(" times");
                Slog.i(str, stringBuilder.toString());
                return;
            }
            return;
        }
        this.mContinueInvalidCnt = 0;
    }
}
