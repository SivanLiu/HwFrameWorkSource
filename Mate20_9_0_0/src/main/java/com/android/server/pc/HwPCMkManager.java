package com.android.server.pc;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.HwPCUtils;
import android.view.Display;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.ViewConfiguration;
import com.android.server.LocalServices;
import com.android.server.gesture.GestureNavConst;
import com.android.server.input.HwInputManagerService.HwInputManagerLocalService;

public class HwPCMkManager {
    public static final boolean DEBUG = SystemProperties.getBoolean("hw_pc_mkmanager_debug", false);
    public static final int MOTION_DOUBLE_CLICK_DELAY_TIME = 300;
    public static final String TAG = "HwPCMkManager";
    private static HwPCMkManager mStatic;
    private final int SHOW_DEFAULT = 0;
    private final int SHOW_LASERPOINTER = 1;
    private final int SHOW_UNUSE = -1;
    final float default_height = 50.0f;
    final float default_width = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private int mClickCount = 0;
    private float mCoefficientX = 1.7f;
    private float mCoefficientY = 0.8f;
    private Context mContext;
    private float mCurrentEventX = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private float mCurrentEventY = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private float mCurrentPointerX = 100.0f;
    private float mCurrentPointerY = 100.0f;
    private float mDensity = 3.0f;
    private DisplayManager mDisplayManager;
    private MotionEvent mDownEvent = null;
    private HwInputManagerLocalService mInputManager = null;
    private boolean mIsMove = false;
    private boolean mIsSequent = false;
    private MotionEvent mMultiPointerDownEvent = null;
    private long mOldClickTime = 0;
    private HwPCManagerService mPCManager;
    private PoninterMode mPoninterMode = PoninterMode.NORMAL_POINTER_MODE;
    private int mScreentHeight;
    private int mScreentWidth;
    private SendEventThread mSendEvent = null;
    private int mShowMode = 0;
    private int mTouchSlopSquare = 0;
    final float showlaserpointer_height = 90.0f;
    final float showlaserpointer_width = 46.0f;

    private enum PoninterMode {
        NORMAL_POINTER_MODE,
        MULTI_POINTER_MODE,
        SINGLE_POINTER_MODE
    }

    class SendEventThread extends HandlerThread implements Callback {
        public static final int MSG_MOTIONEVENT = 0;
        public static final int MSG_MOTIONEVENT_MOVE = 1;
        private Handler mEventHandler;

        public SendEventThread(String name) {
            super(name);
        }

        public SendEventThread(String name, int priority) {
            super(name, priority);
        }

        protected void onLooperPrepared() {
            this.mEventHandler = new Handler(getLooper(), this);
        }

        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                case 1:
                    if (msg.obj instanceof InputEvent) {
                        HwPCMkManager.this.sendPointerSync(msg.obj);
                        break;
                    }
                    break;
            }
            return true;
        }

        public void send(InputEvent event) {
            if (HwPCMkManager.DEBUG) {
                String str = HwPCMkManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SendEventThread send event:");
                stringBuilder.append(event);
                HwPCUtils.log(str, stringBuilder.toString());
            }
            Message msg = Message.obtain();
            msg.what = 0;
            msg.obj = event;
            this.mEventHandler.sendMessage(msg);
        }

        public void sendMoveEvent(InputEvent event) {
            if (HwPCMkManager.DEBUG) {
                String str = HwPCMkManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SendEventThread send move event:");
                stringBuilder.append(event);
                HwPCUtils.log(str, stringBuilder.toString());
            }
            this.mEventHandler.removeMessages(1);
            Message msg = Message.obtain();
            msg.what = 1;
            msg.obj = event;
            this.mEventHandler.sendMessage(msg);
        }

        public void clear() {
            this.mEventHandler.removeMessages(1);
            this.mEventHandler.removeMessages(0);
        }
    }

    public static synchronized HwPCMkManager getInstance(Context context) {
        HwPCMkManager hwPCMkManager;
        synchronized (HwPCMkManager.class) {
            if (mStatic == null) {
                mStatic = new HwPCMkManager(context);
            }
            hwPCMkManager = mStatic;
        }
        return hwPCMkManager;
    }

    private HwPCMkManager(Context context) {
        this.mContext = context;
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        DisplayMetrics dm = new DisplayMetrics();
        Display mDefaultDisplay = this.mDisplayManager.getDisplay(0);
        if (mDefaultDisplay != null) {
            mDefaultDisplay.getMetrics(dm);
            this.mDensity = dm.density;
        }
        int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mTouchSlopSquare = touchSlop * touchSlop;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("MkManager mTouchSlopSquare:");
        stringBuilder.append(this.mTouchSlopSquare);
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public void initCrop(Context context, HwPCManagerService pcManager) {
        this.mPCManager = pcManager;
        if (this.mDisplayManager != null) {
            Display mDisplay = this.mDisplayManager.getDisplay(HwPCUtils.getPCDisplayID());
            if (mDisplay != null) {
                Point size = new Point();
                mDisplay.getRealSize(size);
                this.mScreentWidth = size.x;
                this.mScreentHeight = size.y;
            } else {
                HwPCUtils.log(TAG, "initCrop display is null");
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwPCMkManager mScreentWidth:");
        stringBuilder.append(this.mScreentWidth);
        stringBuilder.append(" mScreentHeight:");
        stringBuilder.append(this.mScreentHeight);
        stringBuilder.append(" mDensity = ");
        stringBuilder.append(this.mDensity);
        stringBuilder.append(" mTouchSlopSquare = ");
        stringBuilder.append(this.mTouchSlopSquare);
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public void startSendEventThread() {
        synchronized (this) {
            if (this.mSendEvent == null) {
                this.mSendEvent = new SendEventThread("PCSendEvent", -4);
                this.mSendEvent.start();
            }
        }
    }

    public void stopSendEventThreadAndRelease() {
        synchronized (this) {
            this.mScreentWidth = 0;
            this.mScreentHeight = 0;
            if (this.mSendEvent != null) {
                this.mSendEvent.clear();
            }
        }
    }

    final float dipsToPixels(float dips) {
        return (this.mDensity * dips) + 0.5f;
    }

    final float pixelsToDips(float pixels) {
        return (pixels / this.mDensity) + 0.5f;
    }

    public boolean sendEvent(MotionEvent event, Rect visibleRect, Rect displayRect, int mode) {
        if (event == null || this.mSendEvent == null) {
            return false;
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendEvent event = ");
            stringBuilder.append(event);
            stringBuilder.append("  visibleRect = ");
            stringBuilder.append(visibleRect);
            stringBuilder.append(" displayRect = ");
            stringBuilder.append(displayRect);
            HwPCUtils.log(str, stringBuilder.toString());
        }
        if (!isInTouchRect(event, visibleRect, displayRect, mode)) {
            return false;
        }
        int pointerCount = event.getPointerCount();
        if (DEBUG) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("sendEvent pointerCount:");
            stringBuilder2.append(pointerCount);
            stringBuilder2.append(" mPoninterMode:");
            stringBuilder2.append(this.mPoninterMode);
            stringBuilder2.append(" event:");
            stringBuilder2.append(event);
            HwPCUtils.log(str2, stringBuilder2.toString());
        }
        OnDownEventProcess(event);
        if (this.mPoninterMode == PoninterMode.NORMAL_POINTER_MODE) {
            if (pointerCount > 1) {
                this.mPoninterMode = PoninterMode.MULTI_POINTER_MODE;
            } else {
                this.mPoninterMode = PoninterMode.SINGLE_POINTER_MODE;
            }
        }
        if (this.mPoninterMode == PoninterMode.SINGLE_POINTER_MODE) {
            onSinglePointerMode(event);
        } else if (this.mPoninterMode == PoninterMode.MULTI_POINTER_MODE) {
            this.mDownEvent = null;
            this.mClickCount = 0;
            onMultiPointerMode(event);
        }
        return true;
    }

    private boolean isInTouchRect(MotionEvent event, Rect visibleRect, Rect displayRect, int mode) {
        if (mode != -1) {
            this.mShowMode = mode;
        }
        float height = 50.0f;
        float width = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        if (displayRect.right < displayRect.bottom) {
            if (this.mShowMode == 1) {
                height = 90.0f;
            }
            if (event.getX() < ((float) visibleRect.left) || event.getX() > ((float) visibleRect.right) || event.getY() < ((float) visibleRect.top) + dipsToPixels(height) || event.getY() > ((float) visibleRect.bottom)) {
                HwPCUtils.log(TAG, "sendEvent donot in visibleRect, right < bottom");
                return false;
            }
        }
        if (this.mShowMode == 1) {
            width = 46.0f;
        }
        if (event.getX() < ((float) visibleRect.left) || event.getX() > ((float) visibleRect.right) - dipsToPixels(width) || event.getY() < ((float) visibleRect.top) + dipsToPixels(50.0f) || event.getY() > ((float) visibleRect.bottom)) {
            HwPCUtils.log(TAG, "sendEvent donot in visibleRect right >= bottom");
            return false;
        }
        return true;
    }

    private void OnDownEventProcess(MotionEvent event) {
        int action = event.getAction();
        String str;
        StringBuilder stringBuilder;
        if (action == 0) {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("OnDownEventProcess ACTION_DOWN mPoninterMode:");
                stringBuilder.append(this.mPoninterMode);
                HwPCUtils.log(str, stringBuilder.toString());
            }
            this.mPoninterMode = PoninterMode.SINGLE_POINTER_MODE;
            this.mIsSequent = true;
        } else if ((action & 255) == 5) {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("OnDownEventProcess ACTION_POINTER_DOWN mPoninterMode:");
                stringBuilder.append(this.mPoninterMode);
                HwPCUtils.log(str, stringBuilder.toString());
            }
            if (this.mPoninterMode == PoninterMode.NORMAL_POINTER_MODE) {
                this.mPoninterMode = PoninterMode.MULTI_POINTER_MODE;
            }
            if (this.mIsSequent) {
                this.mPoninterMode = PoninterMode.MULTI_POINTER_MODE;
            }
        } else if (this.mPoninterMode != PoninterMode.SINGLE_POINTER_MODE || action != 2 || !this.mIsSequent) {
            this.mIsSequent = false;
        } else if (!isShakeMove(event.getX(), event.getY(), this.mCurrentEventX, this.mCurrentEventY)) {
            this.mIsSequent = false;
        }
    }

    private void onSinglePointerMode(MotionEvent event) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSinglePointerMode event:");
            stringBuilder.append(event);
            HwPCUtils.log(str, stringBuilder.toString());
        }
        int action = event.getAction();
        if (this.mDownEvent != null) {
            int pointerId = this.mDownEvent.getPointerId(0);
            int curPointerId = event.getPointerId(0);
            String str2;
            StringBuilder stringBuilder2;
            if (pointerId == curPointerId) {
                String str3;
                StringBuilder stringBuilder3;
                switch (action) {
                    case 1:
                        if (DEBUG) {
                            str3 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("sendEvent ACTION_UP mClickCount:");
                            stringBuilder3.append(this.mClickCount);
                            stringBuilder3.append(" mIsMove:");
                            stringBuilder3.append(this.mIsMove);
                            HwPCUtils.log(str3, stringBuilder3.toString());
                        }
                        if (this.mIsMove) {
                            if (this.mClickCount >= 1) {
                                onClickUpMouseEvent(event);
                            } else {
                                onUpMouseEvent(event);
                            }
                            this.mOldClickTime = 0;
                            this.mClickCount = 0;
                        } else {
                            if (this.mClickCount >= 1) {
                                onClickUpMouseEvent(event);
                                this.mOldClickTime = 0;
                                this.mClickCount = 0;
                            } else {
                                onClickMouseEvent(this.mDownEvent, event);
                            }
                            this.mClickCount++;
                            this.mOldClickTime = SystemClock.uptimeMillis();
                        }
                        this.mIsMove = false;
                        this.mDownEvent = null;
                        this.mPoninterMode = PoninterMode.NORMAL_POINTER_MODE;
                        break;
                    case 2:
                        if (this.mIsMove || !isShakeMove(event.getX(), event.getY(), this.mDownEvent.getX(), this.mDownEvent.getY())) {
                            this.mIsMove = true;
                        }
                        if (DEBUG) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("sendEvent ACTION_MOVE mClickCount:");
                            stringBuilder2.append(this.mClickCount);
                            HwPCUtils.log(str2, stringBuilder2.toString());
                        }
                        if (this.mClickCount < 1) {
                            onHoverEvent(event);
                            break;
                        } else {
                            onMouseEvent(event);
                            break;
                        }
                    default:
                        if (((65280 & action) >> 8) <= 0) {
                            if (DEBUG) {
                                str3 = TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("sendEvent nukown action:");
                                stringBuilder3.append(action);
                                stringBuilder3.append(" mClickCount:");
                                stringBuilder3.append(this.mClickCount);
                                stringBuilder3.append(" mIsMove:");
                                stringBuilder3.append(this.mIsMove);
                                stringBuilder3.append(" mOldClickTime:");
                                stringBuilder3.append(this.mOldClickTime);
                                HwPCUtils.log(str3, stringBuilder3.toString());
                            }
                            onUpMouseEvent(event);
                            this.mOldClickTime = 0;
                            this.mIsMove = false;
                            this.mDownEvent = null;
                            this.mClickCount = 0;
                            this.mPoninterMode = PoninterMode.NORMAL_POINTER_MODE;
                            break;
                        }
                        break;
                }
            }
            if (DEBUG) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("sendEvent pointerId:");
                stringBuilder2.append(pointerId);
                stringBuilder2.append(" curPointerId:");
                stringBuilder2.append(curPointerId);
                HwPCUtils.log(str2, stringBuilder2.toString());
            }
            this.mIsMove = false;
            this.mDownEvent = null;
            this.mClickCount = 0;
            this.mPoninterMode = PoninterMode.NORMAL_POINTER_MODE;
        } else if (action == 0) {
            this.mIsMove = false;
            this.mCurrentEventX = event.getX();
            this.mCurrentEventY = event.getY();
            this.mDownEvent = MotionEvent.obtain(event);
            long clickTime = SystemClock.uptimeMillis() - this.mOldClickTime;
            if (DEBUG) {
                String str4 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("sendEvent ACTION_DOWN clickTime:");
                stringBuilder4.append(clickTime);
                stringBuilder4.append(" mOldClickTime:");
                stringBuilder4.append(this.mOldClickTime);
                stringBuilder4.append(" mClickCount:");
                stringBuilder4.append(this.mClickCount);
                HwPCUtils.log(str4, stringBuilder4.toString());
            }
            if (clickTime > 300) {
                this.mOldClickTime = 0;
                this.mClickCount = 0;
            }
            if (this.mClickCount >= 1) {
                onClickMouseEvent(event);
            }
        }
    }

    private void onMultiPointerMode(MotionEvent event) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onMultiPointerMode event:");
            stringBuilder.append(event);
            HwPCUtils.log(str, stringBuilder.toString());
        }
        int action = event.getAction();
        String str2;
        StringBuilder stringBuilder2;
        switch (action & 255) {
            case 5:
                this.mCurrentEventX = event.getX();
                this.mCurrentEventY = event.getY();
                if (DEBUG) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("sendEvent ACTION_POINTER_DOWN mCurrentEventX:");
                    stringBuilder2.append(this.mCurrentEventX);
                    stringBuilder2.append(" mCurrentEventY:");
                    stringBuilder2.append(this.mCurrentEventY);
                    HwPCUtils.log(str2, stringBuilder2.toString());
                }
                this.mMultiPointerDownEvent = MotionEvent.obtain(event);
                this.mIsMove = false;
                onHoverExitForRightClick(event);
                break;
            case 6:
                if (DEBUG) {
                    str2 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("sendEvent ACTION_POINTER_UP mCurrentPointerX:");
                    stringBuilder3.append(this.mCurrentPointerX);
                    stringBuilder3.append(" mCurrentPointerY:");
                    stringBuilder3.append(this.mCurrentPointerY);
                    HwPCUtils.log(str2, stringBuilder3.toString());
                }
                if (!(this.mIsMove || this.mMultiPointerDownEvent == null)) {
                    onMouseSecondaryClick(this.mMultiPointerDownEvent, event, this.mCurrentPointerX, this.mCurrentPointerY);
                }
                this.mIsMove = false;
                onHoverEnterForRightClick(event);
                this.mMultiPointerDownEvent = null;
                this.mPoninterMode = PoninterMode.NORMAL_POINTER_MODE;
                break;
            default:
                if (action != 2) {
                    if (action == 3) {
                        this.mIsMove = false;
                        this.mMultiPointerDownEvent = null;
                        this.mPoninterMode = PoninterMode.NORMAL_POINTER_MODE;
                        break;
                    }
                }
                if (DEBUG) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("sendEvent multi pointer ACTION_MOVE mCurrentPointerX:");
                    stringBuilder2.append(this.mCurrentPointerX);
                    stringBuilder2.append(" mCurrentPointerY:");
                    stringBuilder2.append(this.mCurrentPointerY);
                    stringBuilder2.append(" eventX:");
                    stringBuilder2.append(event.getX());
                    stringBuilder2.append(" eventY:");
                    stringBuilder2.append(event.getY());
                    HwPCUtils.log(str2, stringBuilder2.toString());
                }
                if (this.mIsMove || !isShakeMove(event.getX(), event.getY(), this.mCurrentEventX, this.mCurrentEventY)) {
                    this.mIsMove = true;
                    if (onScrollEvent(event, event.getX() - this.mCurrentEventX, event.getY() - this.mCurrentEventY)) {
                        this.mCurrentEventX = event.getX();
                        this.mCurrentEventY = event.getY();
                        break;
                    }
                }
                if (DEBUG) {
                    HwPCUtils.log(TAG, "onMultiPointerMode isShakeMove");
                }
                return;
                break;
        }
    }

    private boolean isShakeMove(float moveX, float moveY, float downX, float downY) {
        int deltaX = (int) (moveX - downX);
        int deltaY = (int) (moveY - downY);
        int distance = (deltaX * deltaX) + (deltaY * deltaY);
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("distance:");
            stringBuilder.append(distance);
            stringBuilder.append(" mTouchSlopSquare:");
            stringBuilder.append(this.mTouchSlopSquare);
            HwPCUtils.log(str, stringBuilder.toString());
        }
        return distance < this.mTouchSlopSquare;
    }

    private void onMouseSecondaryClick(MotionEvent downEvent, MotionEvent upEvent, float x, float y) {
        if (this.mSendEvent != null) {
            this.mSendEvent.send(obtainMotionEvent(downEvent, x, y, 0, 2, 2));
            float f = x;
            float f2 = y;
            this.mSendEvent.send(obtainMotionEvent(downEvent, f, f2, 11, 2, 2));
            MotionEvent motionEvent = upEvent;
            this.mSendEvent.send(obtainMotionEvent(motionEvent, f, f2, 12, 2, 2));
            this.mSendEvent.send(obtainMotionEvent(motionEvent, f, f2, 1, upEvent.getButtonState(), 0));
        }
    }

    private MotionEvent obtainMotionEvent(MotionEvent event, float x, float y, int action, int buttonState) {
        float f;
        float f2;
        MotionEvent motionEvent = event;
        PointerProperties[] pointerProperties = new PointerProperties[1];
        PointerCoords[] pointerCoords = new PointerCoords[1];
        for (int index = 0; index < 1; index++) {
            PointerProperties outPointerProperties = new PointerProperties();
            motionEvent.getPointerProperties(index, outPointerProperties);
            outPointerProperties.toolType = 3;
            pointerProperties[index] = outPointerProperties;
            PointerCoords outPointerCoords = new PointerCoords();
            motionEvent.getPointerCoords(index, outPointerCoords);
            pointerCoords[index] = outPointerCoords;
            if (index == 0) {
                outPointerCoords.x = x;
                outPointerCoords.y = y;
            } else {
                f = x;
                f2 = y;
            }
        }
        f = x;
        f2 = y;
        long downTime = event.getDownTime();
        long eventTime = event.getEventTime();
        int metaState = event.getMetaState();
        float xPrecision = event.getXPrecision();
        f = xPrecision;
        return MotionEvent.obtain(downTime, eventTime, action, 1, pointerProperties, pointerCoords, metaState, buttonState, f, event.getYPrecision(), event.getDeviceId(), event.getEdgeFlags(), 8194, 0);
    }

    public static MotionEvent obtainMotionEvent(MotionEvent event, float x, float y, int action, int buttonState, float vScroll, float hScroll) {
        float f;
        float f2;
        MotionEvent motionEvent = event;
        PointerProperties[] pointerProperties = new PointerProperties[1];
        PointerCoords[] pointerCoords = new PointerCoords[1];
        for (int index = 0; index < 1; index++) {
            PointerProperties outPointerProperties = new PointerProperties();
            motionEvent.getPointerProperties(index, outPointerProperties);
            outPointerProperties.toolType = 3;
            pointerProperties[index] = outPointerProperties;
            PointerCoords outPointerCoords = new PointerCoords();
            motionEvent.getPointerCoords(index, outPointerCoords);
            outPointerCoords.setAxisValue(9, vScroll);
            outPointerCoords.setAxisValue(10, hScroll);
            pointerCoords[index] = outPointerCoords;
            if (index == 0) {
                outPointerCoords.x = x;
                outPointerCoords.y = y;
            } else {
                f = x;
                f2 = y;
            }
        }
        f = x;
        f2 = y;
        float f3 = vScroll;
        float f4 = hScroll;
        return MotionEvent.obtain(event.getDownTime(), event.getEventTime(), action, 1, pointerProperties, pointerCoords, event.getMetaState(), buttonState, event.getXPrecision(), event.getYPrecision(), event.getDeviceId(), event.getEdgeFlags(), 8194, 0);
    }

    private MotionEvent obtainMotionEvent(MotionEvent event, float x, float y, int action, int buttonState, int actionButton) {
        MotionEvent motionEvent = obtainMotionEvent(event, x, y, action, buttonState);
        motionEvent.setActionButton(actionButton);
        return motionEvent;
    }

    private void updatePointer(MotionEvent motionEvent) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updatePointer mCurrentEventX:");
            stringBuilder.append(this.mCurrentEventX);
            stringBuilder.append(" mCurrentEventY:");
            stringBuilder.append(this.mCurrentEventY);
            stringBuilder.append(" mCurrentPointerX:");
            stringBuilder.append(this.mCurrentPointerX);
            stringBuilder.append(" mCurrentEventY:");
            stringBuilder.append(this.mCurrentPointerY);
            stringBuilder.append(" motionX:");
            stringBuilder.append(motionEvent.getX());
            stringBuilder.append(" motionY:");
            stringBuilder.append(motionEvent.getY());
            HwPCUtils.log(str, stringBuilder.toString());
        }
        float x = (motionEvent.getX() - this.mCurrentEventX) + this.mCurrentPointerX;
        float y = (motionEvent.getY() - this.mCurrentEventY) + this.mCurrentPointerY;
        this.mCurrentEventX = motionEvent.getX();
        this.mCurrentEventY = motionEvent.getY();
        if (x < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            x = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
        if (x >= ((float) this.mScreentWidth)) {
            x = (float) (this.mScreentWidth - 1);
        }
        if (y < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            y = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
        if (y >= ((float) this.mScreentHeight)) {
            y = (float) (this.mScreentHeight - 1);
        }
        this.mCurrentPointerX = x;
        this.mCurrentPointerY = y;
        if (DEBUG) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updatePointer end mCurrentPointerX:");
            stringBuilder2.append(this.mCurrentPointerX);
            stringBuilder2.append(" mCurrentPointerY:");
            stringBuilder2.append(this.mCurrentPointerY);
            HwPCUtils.log(str2, stringBuilder2.toString());
        }
    }

    private boolean onScrollEvent(MotionEvent motionEvent, float distanceX, float distanceY) {
        float f = distanceX;
        float f2 = distanceY;
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onScrollEvent distanceX:");
            stringBuilder.append(f);
            stringBuilder.append(" distanceY:");
            stringBuilder.append(f2);
            stringBuilder.append(" mDensity");
            stringBuilder.append(this.mDensity);
            HwPCUtils.log(str, stringBuilder.toString());
        }
        if (this.mSendEvent == null) {
            return false;
        }
        float distance = 16.0f * this.mDensity;
        if (Math.abs(distanceX) < distance && Math.abs(distanceY) < distance) {
            return false;
        }
        float vScroll = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        float hScroll = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        float f3 = -1.0f;
        if (Math.abs(distanceY) >= distance) {
            vScroll = f2 > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO ? 1.0f : -1.0f;
        }
        if (Math.abs(distanceX) >= distance) {
            if (f > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                f3 = 1.0f;
            }
            hScroll = f3;
        }
        this.mSendEvent.sendMoveEvent(obtainMotionEvent(motionEvent, this.mCurrentPointerX, this.mCurrentPointerY, 8, motionEvent.getButtonState(), vScroll, hScroll));
        return true;
    }

    private void onHoverEvent(MotionEvent motionEvent) {
        if (this.mSendEvent != null) {
            updatePointer(motionEvent);
            this.mSendEvent.sendMoveEvent(obtainMotionEvent(motionEvent, this.mCurrentPointerX, this.mCurrentPointerY, 7, motionEvent.getButtonState()));
        }
    }

    private void onHoverEnterForRightClick(MotionEvent motionEvent) {
        if (this.mSendEvent != null) {
            this.mSendEvent.send(obtainMotionEvent(motionEvent, this.mCurrentPointerX, this.mCurrentPointerY, 9, 2, 0));
        }
    }

    private void onHoverExitForRightClick(MotionEvent motionEvent) {
        if (this.mSendEvent != null) {
            this.mSendEvent.send(obtainMotionEvent(motionEvent, this.mCurrentPointerX, this.mCurrentPointerY, 10, 2, 0));
        }
    }

    private void onMouseEvent(MotionEvent motionEvent) {
        if (this.mSendEvent != null) {
            updatePointer(motionEvent);
            this.mSendEvent.sendMoveEvent(obtainMotionEvent(motionEvent, this.mCurrentPointerX, this.mCurrentPointerY, motionEvent.getAction(), 1));
        }
    }

    private void onClickMouseEvent(MotionEvent motionEvent) {
        if (this.mSendEvent != null) {
            modifyToMouseEvent(motionEvent, this.mCurrentPointerX, this.mCurrentPointerY, 1);
            this.mSendEvent.send(obtainMotionEvent(motionEvent, this.mCurrentPointerX, this.mCurrentPointerY, 11, 1, 1));
        }
    }

    private void onUpMouseEvent(MotionEvent upEvent) {
        modifyToMouseEvent(upEvent, this.mCurrentPointerX, this.mCurrentPointerY, upEvent.getButtonState());
    }

    private void onClickUpMouseEvent(MotionEvent upEvent) {
        if (this.mSendEvent != null) {
            this.mSendEvent.send(obtainMotionEvent(upEvent, this.mCurrentPointerX, this.mCurrentPointerY, 12, 1, 1));
            modifyToMouseEvent(upEvent, this.mCurrentPointerX, this.mCurrentPointerY, upEvent.getButtonState());
        }
    }

    private void onClickMouseEvent(MotionEvent downEvent, MotionEvent upEvent) {
        if (this.mSendEvent != null) {
            sendMouseEvent(0, 1, this.mCurrentPointerX, this.mCurrentPointerY, 0, 0);
            this.mSendEvent.send(obtainMotionEvent(downEvent, this.mCurrentPointerX, this.mCurrentPointerY, 11, 1, 1));
            this.mSendEvent.send(obtainMotionEvent(upEvent, this.mCurrentPointerX, this.mCurrentPointerY, 12, 1, 1));
            sendMouseEvent(1, 0, this.mCurrentPointerX, this.mCurrentPointerY, 0, 0);
        }
    }

    private void modifyToMouseEvent(MotionEvent event, float x, float y, int buttonState) {
        MotionEvent motionEvent = event;
        if (this.mSendEvent != null) {
            float f;
            float f2;
            int pointerCount = event.getPointerCount();
            PointerProperties[] pointerProperties = new PointerProperties[pointerCount];
            PointerCoords[] pointerCoords = new PointerCoords[pointerCount];
            for (int index = 0; index < pointerCount; index++) {
                PointerProperties outPointerProperties = new PointerProperties();
                motionEvent.getPointerProperties(index, outPointerProperties);
                outPointerProperties.toolType = 3;
                pointerProperties[index] = outPointerProperties;
                PointerCoords outPointerCoords = new PointerCoords();
                motionEvent.getPointerCoords(index, outPointerCoords);
                pointerCoords[index] = outPointerCoords;
                if (index == 0) {
                    outPointerCoords.x = x;
                    outPointerCoords.y = y;
                } else {
                    f = x;
                    f2 = y;
                }
            }
            f = x;
            f2 = y;
            this.mSendEvent.send(MotionEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(), event.getPointerCount(), pointerProperties, pointerCoords, event.getMetaState(), buttonState, event.getXPrecision(), event.getYPrecision(), event.getDeviceId(), event.getEdgeFlags(), 8194, 0));
        }
    }

    private void sendMouseEvent(int action, int buttonState, float xx, float yy, int offsetX, int offsetY) {
        if (this.mSendEvent != null) {
            this.mSendEvent.send(obtainMouseEvent(action, buttonState, xx, yy, offsetX, offsetY));
        }
    }

    public void sendFakedMouseMoveEvent() {
        if (this.mSendEvent != null) {
            this.mSendEvent.sendMoveEvent(obtainMouseEvent(2, 0, this.mCurrentPointerX, this.mCurrentPointerY, 0, 0));
        }
    }

    public static MotionEvent obtainMouseEvent(int action, int buttonState, float xx, float yy, int offsetX, int offsetY) {
        long eventTime = SystemClock.uptimeMillis();
        PointerProperties[] props = new PointerProperties[]{new PointerProperties()};
        props[0].id = 0;
        props[0].toolType = 3;
        PointerCoords[] coords = new PointerCoords[]{new PointerCoords()};
        coords[0].x = xx + ((float) offsetX);
        coords[0].y = yy + ((float) offsetY);
        return MotionEvent.obtain(eventTime, eventTime, action, 1, props, coords, 0, buttonState, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, -1, 0, 8194, 0);
    }

    private boolean sendPointerSync(InputEvent event) {
        if (this.mInputManager == null) {
            this.mInputManager = (HwInputManagerLocalService) LocalServices.getService(HwInputManagerLocalService.class);
        }
        if (this.mInputManager == null) {
            return false;
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendPointerSync event = ");
            stringBuilder.append(event);
            HwPCUtils.log(str, stringBuilder.toString());
        }
        return this.mInputManager.injectInputEvent(event, 0);
    }

    public void updatePointerAxis(float[] axis) {
        this.mCurrentPointerX = axis[0];
        this.mCurrentPointerY = axis[1];
    }
}
