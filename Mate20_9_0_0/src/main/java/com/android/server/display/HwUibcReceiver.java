package com.android.server.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.input.IInputManager;
import android.hardware.input.IInputManager.Stub;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.util.HwPCUtils;
import android.util.Log;
import android.view.Display;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import com.android.server.gesture.GestureNavConst;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class HwUibcReceiver extends HandlerThread implements IHwUibcReceiver {
    private static final int ACEEPT_TIMEOUT = 14000;
    private static final String ACTION_UIBC_USE_INFO = "com.huawei.hardware.display.action.WIFI_DISPLAY_UIBC_USE_INFO";
    public static final Runnable AcceptCheck = new Runnable() {
        public void run() {
            if (HwUibcReceiver.instance != null && HwUibcReceiver.instance.getReceiverState() == ReceiverState.STATE_INITIAL) {
                Log.w(HwUibcReceiver.TAG, "No one connect UIBC, Destroy it");
                HwUibcReceiver.instance.destroyReceiver();
            }
        }
    };
    private static final int BUFFER_MAX = 100;
    public static final int DESTORY_TIMEOUT = 16000;
    private static final String KEY_UIBC_USE_INFO = "WIFI_DISPLAY_UIBC_USE_INFO";
    private static final int RECEIVER_DELAY = 3;
    private static final String TAG = "UIBCReceiver";
    private static final String THREAD_NAME = "UIBCReceiver_Thread";
    private static final int UIBC_CONNECTION_FAIL_INNER = 4;
    private static final int UIBC_CONNECTION_FAIL_NOIN = 3;
    private static final int UIBC_GENERIC_ID_KEY_DOWN = 3;
    private static final int UIBC_GENERIC_ID_KEY_UP = 4;
    private static final int UIBC_GENERIC_INPUT_CATEGORY_ID = 0;
    private static final int UIBC_HIDC_INPUT_CATEGORY_ID = 1;
    private static final int UIBC_PACKET_HEADER_LEN = 4;
    private static final int UIBC_RECEIVE = 2;
    private static final int UIBC_RESUME = 5;
    private static final int UIBC_START = 1;
    private static final int UIBC_STOP = 3;
    private static final int UIBC_SUSPEND = 4;
    private static final int UIBC_USE_GEN = 1;
    private static final int UIBC_USE_HID = 2;
    private static final String WIFI_DISPLAY_UIBC_PERMISSION = "com.huawei.wfd.permission.ACCESS_WIFI_DISPLAY_UIBC";
    private static InputEvent[] inputEvents = new InputEvent[10];
    private static volatile HwUibcReceiver instance = null;
    private final IInputManager inputManager;
    private boolean isUploadGenEvent;
    private boolean isUploadHidEvent;
    private Context mContext;
    private CurGenericEvent mCurGenericEvent;
    private CurrentPacket mCurrentPacket;
    private boolean mDestroyed;
    private Display mDisplay;
    private Handler mDisplayHandler;
    private Handler mHandler;
    private BufferedInputStream mInput;
    private int mParseBytes;
    private int mPort;
    private int mReceiveBytes;
    private BroadcastReceiver mReceiver;
    private ServerSocket mServer;
    private Socket mSocket;
    private ReceiverState mState;
    private byte[] mUibcBuffer;
    private String macAddress;
    private volatile float remoteAspectRatio;
    private volatile int remoteHeight;
    private volatile int remoteWidth;
    private volatile float screenAspectRatio;
    private volatile int screenHeight;
    private volatile int screenWidth;
    boolean skiponce;

    public class CurGenericEvent {
        public static final int GENERIC_DESCRIBE_INDEX = 3;
        public static final int GENERIC_EVENT_LIST_COUNT = 10;
        public static final int GENERIC_FIRST_KEY_CODE = 5;
        public static final int GENERIC_LEFT_KEY_DOWN = 3;
        public static final int GENERIC_LEFT_KEY_UP = 4;
        public static final int GENERIC_LEFT_MOVE = 2;
        public static final int GENERIC_LEFT_POINT_DOWN = 0;
        public static final int GENERIC_LEFT_POINT_UP = 1;
        public static final int GENERIC_LENGTH_INDEX = 1;
        public static final int GENERIC_NUM_POINT_INDEX = 3;
        public static final int GENERIC_POINT_ID = 4;
        public static final int GENERIC_POINT_LEN = 5;
        public static final int GENERIC_POINT_X = 5;
        public static final int GENERIC_POINT_Y = 7;
        public static final int GENERIC_SECOND_KEY_CODE = 7;
        public static final int GENERIC_TYPE_ID_INDEX = 0;
        private static final float TOUCH_PRESSURE = 0.8f;
        public int bodyLength;
        private int curPointerIndex = 0;
        public int inputTypeID;
        private boolean isMultiTouch = false;
        private PointerCoords[] pointerCoords = new PointerCoords[10];
        private PointerProperties[] pointerProperties = new PointerProperties[10];
        private MotionEvent prevEvent = null;

        public CurGenericEvent() {
            int i = 0;
            while (i < 10) {
                this.pointerProperties[i] = new PointerProperties();
                this.pointerCoords[i] = new PointerCoords();
                i++;
            }
        }

        public int resolveInputBody(byte[] payload, int index) {
            if (index < 0 || 2 + index >= payload.length) {
                Log.w(HwUibcReceiver.TAG, "resolveInputBody overflow, not expect go in");
                return 0;
            }
            this.inputTypeID = payload[0 + index];
            this.bodyLength = (payload[1 + index] << 8) | payload[2 + index];
            return this.bodyLength + 3;
        }

        public int resolveEvent(byte[] payload, int index) {
            switch (this.inputTypeID) {
                case 0:
                    return createTouchEvents(payload, index, 0);
                case 1:
                    return createTouchEvents(payload, index, 1);
                case 2:
                    return createTouchEvents(payload, index, 2);
                case 3:
                    return createKeyEvent(payload, index, true);
                case 4:
                    return createKeyEvent(payload, index, false);
                default:
                    Log.d(HwUibcReceiver.TAG, "unsupport event");
                    return 0;
            }
        }

        private int createKeyEvent(byte[] payload, int index, boolean isdown) {
            if (index < 0 || index + 5 >= payload.length) {
                Log.w(HwUibcReceiver.TAG, "createKeyEvent overflow, not expect go in");
                return 0;
            }
            int keySatus = isdown ^ 1;
            HwUibcReceiver.inputEvents[0] = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), keySatus, payload[index + 5], 0);
            return 1;
        }

        /* JADX WARNING: Missing block: B:26:0x00a8, code:
            r0 = -1;
     */
        /* JADX WARNING: Missing block: B:27:0x00ab, code:
            if (r1.isMultiTouch != false) goto L_0x00b2;
     */
        /* JADX WARNING: Missing block: B:29:0x00ae, code:
            if (r5 <= 1) goto L_0x00b2;
     */
        /* JADX WARNING: Missing block: B:30:0x00b0, code:
            r1.isMultiTouch = true;
     */
        /* JADX WARNING: Missing block: B:32:0x00b4, code:
            if (r1.isMultiTouch == false) goto L_0x0199;
     */
        /* JADX WARNING: Missing block: B:34:0x00b8, code:
            if (r1.prevEvent == null) goto L_0x016a;
     */
        /* JADX WARNING: Missing block: B:36:0x00c0, code:
            if (r1.prevEvent.getPointerCount() >= r5) goto L_0x00c4;
     */
        /* JADX WARNING: Missing block: B:38:0x00ca, code:
            if (r1.prevEvent.getPointerCount() <= r5) goto L_0x0167;
     */
        /* JADX WARNING: Missing block: B:39:0x00cc, code:
            android.util.Log.d(com.android.server.display.HwUibcReceiver.TAG, "UIBC  pointer up");
            r4 = new java.util.ArrayList();
            r6 = r1.prevEvent.getPointerCount();
            r7 = 0;
     */
        /* JADX WARNING: Missing block: B:40:0x00e0, code:
            if (r7 >= r6) goto L_0x00f2;
     */
        /* JADX WARNING: Missing block: B:41:0x00e2, code:
            r4.add(java.lang.Integer.valueOf(r1.prevEvent.getPointerId(r7)));
            r7 = r7 + 1;
     */
        /* JADX WARNING: Missing block: B:42:0x00f2, code:
            r7 = 0;
     */
        /* JADX WARNING: Missing block: B:43:0x00f3, code:
            if (r7 >= r5) goto L_0x0105;
     */
        /* JADX WARNING: Missing block: B:44:0x00f5, code:
            r4.remove(java.lang.Integer.valueOf(r1.prevEvent.getPointerId(r7)));
            r7 = r7 + 1;
     */
        /* JADX WARNING: Missing block: B:46:0x010a, code:
            if (r4.size() == 1) goto L_0x0110;
     */
        /* JADX WARNING: Missing block: B:47:0x010c, code:
            r7 = 0;
            r1.isMultiTouch = false;
     */
        /* JADX WARNING: Missing block: B:48:0x0110, code:
            r7 = 0;
     */
        /* JADX WARNING: Missing block: B:49:0x0111, code:
            r2 = (6 & 255) | (r1.prevEvent.findPointerIndex(((java.lang.Integer) r4.get(r7)).intValue()) << 8);
            com.android.server.display.HwUibcReceiver.access$1800()[0] = android.view.MotionEvent.obtain(r1.prevEvent.getDownTime(), android.os.SystemClock.uptimeMillis(), r2, r1.prevEvent.getPointerCount(), r1.pointerProperties, r1.pointerCoords, 0, 1, 2.0f, 2.1f, 0, 0, 4098, 0);
            r14 = 1;
     */
        /* JADX WARNING: Missing block: B:50:0x015d, code:
            if (r5 != 1) goto L_0x0164;
     */
        /* JADX WARNING: Missing block: B:51:0x015f, code:
            r1.isMultiTouch = false;
            r0 = r43;
     */
        /* JADX WARNING: Missing block: B:52:0x0164, code:
            r3 = 0 + 1;
     */
        /* JADX WARNING: Missing block: B:53:0x0167, code:
            r14 = 1;
            r0 = 2;
     */
        /* JADX WARNING: Missing block: B:54:0x016a, code:
            r14 = 1;
            android.util.Log.d(com.android.server.display.HwUibcReceiver.TAG, "UIBC  pointer down");
            r0 = 5 & 255;
            r2 = 0;
     */
        /* JADX WARNING: Missing block: B:55:0x0176, code:
            if (r2 >= r5) goto L_0x019c;
     */
        /* JADX WARNING: Missing block: B:57:0x017a, code:
            if (r1.prevEvent == null) goto L_0x018f;
     */
        /* JADX WARNING: Missing block: B:59:0x0189, code:
            if (r1.prevEvent.findPointerIndex(r1.pointerProperties[r2].id) != -1) goto L_0x018c;
     */
        /* JADX WARNING: Missing block: B:60:0x018c, code:
            r2 = r2 + 1;
     */
        /* JADX WARNING: Missing block: B:61:0x018f, code:
            r0 = r0 | (r1.pointerProperties[r2].id << 8);
     */
        /* JADX WARNING: Missing block: B:62:0x0199, code:
            r14 = 1;
            r0 = r43;
     */
        /* JADX WARNING: Missing block: B:63:0x019c, code:
            if (r5 != 0) goto L_0x01a7;
     */
        /* JADX WARNING: Missing block: B:64:0x019e, code:
            android.util.Log.w(com.android.server.display.HwUibcReceiver.TAG, "numPointers is zero");
     */
        /* JADX WARNING: Missing block: B:65:0x01a6, code:
            return 0;
     */
        /* JADX WARNING: Missing block: B:66:0x01a7, code:
            r23 = r14;
            com.android.server.display.HwUibcReceiver.access$1800()[r3] = android.view.MotionEvent.obtain(android.os.SystemClock.uptimeMillis(), android.os.SystemClock.uptimeMillis(), r0, r5, r1.pointerProperties, r1.pointerCoords, 0, 1, 2.0f, 2.1f, 0, 0, 4098, 0);
            r1.prevEvent = (android.view.MotionEvent) com.android.server.display.HwUibcReceiver.access$1800()[r3];
            r3 = r3 + 1;
     */
        /* JADX WARNING: Missing block: B:67:0x01dd, code:
            if (r22 != false) goto L_0x01e1;
     */
        /* JADX WARNING: Missing block: B:69:0x01e0, code:
            return 0;
     */
        /* JADX WARNING: Missing block: B:70:0x01e1, code:
            return r3;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private int createTouchEvents(byte[] payload, int index, int action) {
            Throwable th;
            byte[] bArr = payload;
            int ret = 0;
            if (index < 0 || index + 3 >= bArr.length) {
                Log.w(HwUibcReceiver.TAG, "createTouchEvents overflow, not expect go in");
                return 0;
            }
            int numPointers = bArr[index + 3];
            this.curPointerIndex = 0;
            synchronized (HwUibcReceiver.class) {
                try {
                    if (HwUibcReceiver.this.screenHeight == 0 || HwUibcReceiver.this.screenWidth == 0 || HwUibcReceiver.this.remoteHeight == 0 || HwUibcReceiver.this.remoteWidth == 0) {
                        Log.w(HwUibcReceiver.TAG, "screen size is wrong");
                        return 0;
                    }
                    if (numPointers >= 0) {
                        int i = 1;
                        if (((index + 7) + 1) + ((numPointers - 1) * 5) < bArr.length) {
                            boolean injectFlag = true;
                            int i2 = 0;
                            while (i2 < numPointers) {
                                try {
                                    int lowX = bArr[((index + 5) + i) + (i2 * 5)] & 255;
                                    injectFlag = doTranslateCoord(((float) (((bArr[(index + 5) + (i2 * 5)] & 255) << 8) | lowX)) / ((float) HwUibcReceiver.this.remoteWidth), ((float) (((bArr[(index + 7) + (i2 * 5)] & 255) << 8) | (bArr[((index + 7) + i) + (i2 * 5)] & 255))) / ((float) HwUibcReceiver.this.remoteHeight), bArr[(index + 4) + (i2 * 5)]);
                                    i2++;
                                    bArr = payload;
                                    i = 1;
                                } catch (Throwable th2) {
                                    th = th2;
                                    boolean z = injectFlag;
                                    throw th;
                                }
                            }
                        }
                    }
                    Log.w(HwUibcReceiver.TAG, "createTouchEvents  numPointers  overflow, not expect go in");
                    return 0;
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
        }

        private boolean doTranslateCoord(float x, float y, int id) {
            boolean isValid = true;
            if (this.curPointerIndex >= this.pointerProperties.length || this.curPointerIndex >= this.pointerCoords.length || this.curPointerIndex < 0) {
                return false;
            }
            if (x >= GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && x <= 1.0f && y >= GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && y <= 1.0f) {
                this.pointerProperties[this.curPointerIndex].clear();
                this.pointerProperties[this.curPointerIndex].id = id;
                this.pointerCoords[this.curPointerIndex].clear();
                String str;
                StringBuilder stringBuilder;
                if (HwUibcReceiver.this.screenAspectRatio < HwUibcReceiver.this.remoteAspectRatio) {
                    if (HwUibcReceiver.this.remoteAspectRatio != GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                        x = (1.0f + ((x - 0.5f) / (HwUibcReceiver.this.screenAspectRatio / (HwUibcReceiver.this.remoteAspectRatio * 2.0f)))) * 0.5f;
                        if (x < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO || x > 1.0f) {
                            str = HwUibcReceiver.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("x: ");
                            stringBuilder.append(x);
                            stringBuilder.append(" y: ");
                            stringBuilder.append(y);
                            Log.i(str, stringBuilder.toString());
                            y = -1.0f;
                            x = -1.0f;
                            isValid = false;
                        }
                    } else {
                        Log.e(HwUibcReceiver.TAG, "UIBC  remoteAspectRatio is zero");
                    }
                } else if (HwUibcReceiver.this.screenAspectRatio > HwUibcReceiver.this.remoteAspectRatio) {
                    if (HwUibcReceiver.this.screenAspectRatio != GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                        y = (1.0f + ((y - 0.5f) / (HwUibcReceiver.this.remoteAspectRatio / (HwUibcReceiver.this.screenAspectRatio * 2.0f)))) * 0.5f;
                        if (y < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO || y > 1.0f) {
                            str = HwUibcReceiver.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("x: ");
                            stringBuilder.append(x);
                            stringBuilder.append(" y: ");
                            stringBuilder.append(y);
                            Log.i(str, stringBuilder.toString());
                            y = -1.0f;
                            x = -1.0f;
                            isValid = false;
                        }
                    } else {
                        Log.e(HwUibcReceiver.TAG, "UIBC  screenAspectRatio is zero");
                    }
                }
                if (x < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO || x > 1.0f || y < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO || y > 1.0f) {
                    return false;
                }
                this.pointerCoords[this.curPointerIndex].setAxisValue(0, ((float) HwUibcReceiver.this.screenWidth) * x);
                this.pointerCoords[this.curPointerIndex].setAxisValue(1, ((float) HwUibcReceiver.this.screenHeight) * y);
                this.pointerCoords[this.curPointerIndex].setAxisValue(2, 0.8f);
                this.curPointerIndex++;
            }
            return isValid;
        }
    }

    public static class CurrentPacket {
        public static final int BODY_INDEX_WITHOUT_TIME = 4;
        public static final int INPUT_CATEGORY_INDEX = 1;
        public static final byte INPUT_MASK = (byte) 15;
        public static final int LENGTH_INDEX = 2;
        public static final int TIME_INDEX = 0;
        public static final byte TIME_MASK = (byte) 1;
        public static final int TIME_OFFSET = 4;
        public static final int VERSION_INDEX = 0;
        public static final byte VERSION_MASK = (byte) 7;
        public static final int VERSION_OFFSET = 5;
        public static final int VERSION_VALID = 0;
        public int bodyIndex;
        public int curVer;
        public boolean hasTime;
        public int inputCategory;
        public int payloadLength;

        public int resolveNaked(byte[] payload, int index) {
            boolean z = false;
            if (index < 0 || (index + 2) + 1 >= payload.length) {
                Log.w(HwUibcReceiver.TAG, "resolveNaked overflow, not expect go in");
                return 0;
            }
            this.curVer = (payload[0 + index] >> 5) & 7;
            int i = 4;
            if (((payload[0 + index] >> 4) & 1) == 1) {
                z = true;
            }
            this.hasTime = z;
            this.inputCategory = payload[1 + index] & 15;
            this.payloadLength = (payload[2 + index] << 8) | payload[3 + index];
            if (this.hasTime) {
                i = 6;
            }
            this.bodyIndex = i;
            return this.bodyIndex;
        }

        public boolean isVerValid() {
            if (this.curVer != 0) {
                return false;
            }
            return true;
        }
    }

    public enum ReceiverState {
        STATE_NONE,
        STATE_INITIAL,
        STATE_WAITING,
        STATE_WORKING,
        STATE_SUSPENDING
    }

    class UibcHandler extends Handler {
        public UibcHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Log.i(HwUibcReceiver.TAG, "UIBC_START -- Start Recevier Message Loop");
                    if (HwUibcReceiver.this.mState != ReceiverState.STATE_INITIAL) {
                        String str = HwUibcReceiver.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Wrong state start! state : ");
                        stringBuilder.append(HwUibcReceiver.this.mState);
                        Log.w(str, stringBuilder.toString());
                    }
                    try {
                        HwUibcReceiver.this.mState = ReceiverState.STATE_WAITING;
                        Log.d(HwUibcReceiver.TAG, "Begin accept SeverSocket");
                        HwUibcReceiver.this.mSocket = HwUibcReceiver.this.mServer.accept();
                        if (HwUibcReceiver.this.doIdCheck()) {
                            Log.d(HwUibcReceiver.TAG, "Someone coming ! Accept");
                            HwUibcReceiver.this.mInput = new BufferedInputStream(HwUibcReceiver.this.mSocket.getInputStream());
                            HwUibcReceiver.this.receiveEvent();
                            HwUibcReceiver.this.mState = ReceiverState.STATE_WORKING;
                            break;
                        }
                        Log.w(HwUibcReceiver.TAG, "the Evil Within!");
                        HwUibcReceiver.this.mSocket.close();
                        HwUibcReceiver.this.mState = ReceiverState.STATE_INITIAL;
                        return;
                    } catch (SocketTimeoutException e) {
                        Log.i(HwUibcReceiver.TAG, "Nobody come in");
                        HwUibcReceiver.this.sendUEBroadcast(HwUibcReceiver.KEY_UIBC_USE_INFO, 3);
                        HwUibcReceiver.this.mState = ReceiverState.STATE_INITIAL;
                        break;
                    } catch (IOException e2) {
                        Log.e(HwUibcReceiver.TAG, "buffer error occur");
                        HwUibcReceiver.this.sendUEBroadcast(HwUibcReceiver.KEY_UIBC_USE_INFO, 4);
                        HwUibcReceiver.this.mState = ReceiverState.STATE_INITIAL;
                        break;
                    }
                case 2:
                    try {
                        if (HwUibcReceiver.this.mReceiveBytes == HwUibcReceiver.this.mParseBytes) {
                            HwUibcReceiver.this.mReceiveBytes = 0;
                            HwUibcReceiver.this.mParseBytes = 0;
                        } else if (HwUibcReceiver.this.mReceiveBytes > HwUibcReceiver.this.mParseBytes) {
                            moveBytesLeft(HwUibcReceiver.this.mParseBytes);
                            HwUibcReceiver.access$920(HwUibcReceiver.this, HwUibcReceiver.this.mParseBytes);
                            HwUibcReceiver.this.mParseBytes = 0;
                        }
                        int readCount = HwUibcReceiver.this.mInput.read(HwUibcReceiver.this.mUibcBuffer, HwUibcReceiver.this.mReceiveBytes, 100 - HwUibcReceiver.this.mReceiveBytes);
                        if (!HwUibcReceiver.this.skiponce) {
                            if (HwUibcReceiver.this.mState != ReceiverState.STATE_SUSPENDING && !HwPCUtils.isPcCastModeInServer()) {
                                if (readCount > 0) {
                                    HwUibcReceiver.access$912(HwUibcReceiver.this, readCount);
                                    HwUibcReceiver.this.mParseBytes = 0;
                                }
                                if (readCount > 0 || (readCount == 0 && HwUibcReceiver.this.mReceiveBytes == 100)) {
                                    consumePacket();
                                }
                                if (HwUibcReceiver.this.mState == ReceiverState.STATE_WORKING) {
                                    HwUibcReceiver.this.receiveEvent();
                                    break;
                                }
                            }
                            HwUibcReceiver.this.mReceiveBytes = HwUibcReceiver.this.mParseBytes;
                            HwUibcReceiver.this.receiveEvent();
                            break;
                        }
                        HwUibcReceiver.this.mReceiveBytes = 0;
                        HwUibcReceiver.this.mParseBytes = 0;
                        HwUibcReceiver.this.skiponce = false;
                        HwUibcReceiver.this.receiveEvent();
                        break;
                    } catch (IOException ex) {
                        Log.e(HwUibcReceiver.TAG, "Read data error");
                        String str2 = HwUibcReceiver.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Exception : ");
                        stringBuilder2.append(ex.toString());
                        Log.e(str2, stringBuilder2.toString());
                        HwUibcReceiver.this.mReceiveBytes = 0;
                        HwUibcReceiver.this.mParseBytes = 0;
                        HwUibcReceiver.this.skiponce = true;
                    }
                    break;
                case 3:
                    Log.i(HwUibcReceiver.TAG, "UIBC_STOP -- Stop Recevier Message Loop");
                    HwUibcReceiver.this.closeSocket();
                    break;
                case 4:
                    Log.i(HwUibcReceiver.TAG, "UIBC_SUSPEND");
                    break;
                case 5:
                    Log.i(HwUibcReceiver.TAG, "UIBC_RESUME");
                    break;
                default:
                    Log.i(HwUibcReceiver.TAG, "do not support the message");
                    break;
            }
        }

        private void consumePacket() {
            while (HwUibcReceiver.this.mReceiveBytes > HwUibcReceiver.this.mParseBytes && HwUibcReceiver.this.mParseBytes + 4 <= 100) {
                int parsedCount = HwUibcReceiver.this.mCurrentPacket.resolveNaked(HwUibcReceiver.this.mUibcBuffer, HwUibcReceiver.this.mParseBytes);
                if (!HwUibcReceiver.this.mCurrentPacket.isVerValid() || parsedCount == 0) {
                    Log.w(HwUibcReceiver.TAG, "Unkonwn Version");
                    HwUibcReceiver.this.mParseBytes = HwUibcReceiver.this.mReceiveBytes;
                    return;
                } else if (HwUibcReceiver.this.mCurrentPacket.payloadLength > HwUibcReceiver.this.mReceiveBytes) {
                    String str = HwUibcReceiver.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("UIBCReceiver need read more payloadlengh : ");
                    stringBuilder.append(HwUibcReceiver.this.mCurrentPacket.payloadLength);
                    stringBuilder.append(" Receive : ");
                    stringBuilder.append(HwUibcReceiver.this.mReceiveBytes);
                    Log.w(str, stringBuilder.toString());
                    return;
                } else if (HwUibcReceiver.this.mParseBytes + HwUibcReceiver.this.mCurrentPacket.payloadLength <= 100) {
                    int headerLen = parsedCount;
                    HwUibcReceiver.access$1012(HwUibcReceiver.this, parsedCount);
                    switch (HwUibcReceiver.this.mCurrentPacket.inputCategory) {
                        case 0:
                            if (!HwUibcReceiver.this.isUploadGenEvent) {
                                HwUibcReceiver.this.isUploadGenEvent = true;
                                HwUibcReceiver.this.sendUEBroadcast(HwUibcReceiver.KEY_UIBC_USE_INFO, 1);
                            }
                            parsedCount = WorkGenericInput();
                            break;
                        case 1:
                            if (!HwUibcReceiver.this.isUploadHidEvent) {
                                HwUibcReceiver.this.isUploadHidEvent = true;
                                HwUibcReceiver.this.sendUEBroadcast(HwUibcReceiver.KEY_UIBC_USE_INFO, 2);
                            }
                            parsedCount = HwUibcReceiver.this.nativeParseHid(HwUibcReceiver.this.mUibcBuffer, HwUibcReceiver.this.mParseBytes);
                            if (headerLen + parsedCount != HwUibcReceiver.this.mCurrentPacket.payloadLength) {
                                parsedCount = HwUibcReceiver.this.mCurrentPacket.payloadLength - headerLen;
                                break;
                            }
                            break;
                        default:
                            return;
                    }
                    if (parsedCount == 0) {
                        HwUibcReceiver.this.skiponce = true;
                    }
                    HwUibcReceiver.access$1012(HwUibcReceiver.this, parsedCount);
                } else {
                    return;
                }
            }
        }

        private int WorkGenericInput() {
            int parsedCount = HwUibcReceiver.this.mCurGenericEvent.resolveInputBody(HwUibcReceiver.this.mUibcBuffer, HwUibcReceiver.this.mParseBytes);
            int eventCount = HwUibcReceiver.this.mCurGenericEvent.resolveEvent(HwUibcReceiver.this.mUibcBuffer, HwUibcReceiver.this.mParseBytes);
            int i = 0;
            if (eventCount > HwUibcReceiver.inputEvents.length) {
                return 0;
            }
            while (true) {
                int i2 = i;
                if (i2 >= eventCount) {
                    break;
                }
                try {
                    InputManager.getInstance().injectInputEvent(HwUibcReceiver.inputEvents[i2], 2);
                    i = i2 + 1;
                } catch (Exception e) {
                    Log.i(HwUibcReceiver.TAG, "Inject fail!");
                }
            }
            return parsedCount;
        }

        private void moveBytesLeft(int offset) {
            byte[] tmp = new byte[100];
            if (offset < 0 || offset >= 100) {
                HwUibcReceiver.this.skiponce = true;
                return;
            }
            System.arraycopy(HwUibcReceiver.this.mUibcBuffer, 0, tmp, 0, 100);
            System.arraycopy(tmp, offset, HwUibcReceiver.this.mUibcBuffer, 0, 100 - offset);
        }
    }

    private native void nativeCloseHid();

    private native int nativeParseHid(byte[] bArr, int i);

    static /* synthetic */ int access$1012(HwUibcReceiver x0, int x1) {
        int i = x0.mParseBytes + x1;
        x0.mParseBytes = i;
        return i;
    }

    static /* synthetic */ int access$912(HwUibcReceiver x0, int x1) {
        int i = x0.mReceiveBytes + x1;
        x0.mReceiveBytes = i;
        return i;
    }

    static /* synthetic */ int access$920(HwUibcReceiver x0, int x1) {
        int i = x0.mReceiveBytes - x1;
        x0.mReceiveBytes = i;
        return i;
    }

    public static HwUibcReceiver getInstance() {
        if (instance == null) {
            synchronized (HwUibcReceiver.class) {
                if (instance == null) {
                    instance = new HwUibcReceiver(THREAD_NAME);
                }
            }
        }
        return instance;
    }

    public static void clearInstance() {
        synchronized (HwUibcReceiver.class) {
            instance = null;
        }
    }

    public synchronized int createReceiver(Context context, Handler handler) {
        if (this.mDestroyed) {
            setInContextAndHandler(context, handler);
            start();
            CreateHandler();
            if (getReceiverState() == ReceiverState.STATE_NONE) {
                destroyReceiver();
                return -1;
            }
            this.mDestroyed = false;
            return getPort();
        }
        Log.w(TAG, "Duplicate create");
        return -1;
    }

    public synchronized void startReceiver() {
        Log.i(TAG, "Start Recevier");
        sendMessage(1);
    }

    public synchronized void suspendReceiver() {
        Log.i(TAG, "Suspend Recevier");
        setReceiverState(ReceiverState.STATE_SUSPENDING);
    }

    public synchronized void resumeReceiver() {
        Log.i(TAG, "Resume Recevier");
        setReceiverState(ReceiverState.STATE_WORKING);
    }

    public synchronized void stopReceiver() {
        Log.i(TAG, "Stop Recevier");
        shutDownInput();
        sendMessage(3);
    }

    public synchronized void destroyReceiver() {
        if (this.mDestroyed) {
            Log.w(TAG, "Duplicate Destroy, return");
            return;
        }
        Log.i(TAG, "Destroy Recevier");
        if (getReceiverState() != ReceiverState.STATE_INITIAL) {
            closeSocket();
        }
        this.mState = ReceiverState.STATE_INITIAL;
        closeHid();
        closeServer();
        deInit();
        quitSafely();
        this.mDestroyed = true;
    }

    public synchronized Runnable getAcceptCheck() {
        return AcceptCheck;
    }

    public synchronized void setRemoteScreenSize(int width, int height) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("remoteHeight : ");
        stringBuilder.append(height);
        stringBuilder.append(" remoteWidth : ");
        stringBuilder.append(width);
        Log.i(str, stringBuilder.toString());
        this.remoteHeight = height;
        this.remoteWidth = width;
        if (height != 0) {
            this.remoteAspectRatio = ((float) width) / ((float) height);
        }
    }

    public synchronized void setRemoteMacAddress(String mac) {
        this.macAddress = mac;
    }

    private HwUibcReceiver(String name) {
        super(name);
        this.mHandler = null;
        this.mPort = -1;
        this.mInput = null;
        this.mServer = null;
        this.mSocket = null;
        this.mState = ReceiverState.STATE_NONE;
        this.mContext = null;
        this.mDisplayHandler = null;
        this.mCurrentPacket = new CurrentPacket();
        this.mCurGenericEvent = new CurGenericEvent();
        this.inputManager = Stub.asInterface(ServiceManager.getService("input"));
        this.screenWidth = 0;
        this.screenHeight = 0;
        this.screenAspectRatio = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        this.remoteWidth = 0;
        this.remoteHeight = 0;
        this.remoteAspectRatio = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        this.isUploadGenEvent = false;
        this.isUploadHidEvent = false;
        this.mDestroyed = true;
        this.skiponce = false;
        this.isUploadGenEvent = false;
        this.isUploadHidEvent = false;
        this.skiponce = false;
        this.mUibcBuffer = new byte[100];
        try {
            this.mServer = new ServerSocket(0);
            this.mServer.setSoTimeout(ACEEPT_TIMEOUT);
            this.mServer.setReuseAddress(true);
            this.mState = ReceiverState.STATE_INITIAL;
        } catch (IOException e) {
            Log.e(TAG, "Server Socket Create Fail");
            this.mState = ReceiverState.STATE_NONE;
        }
    }

    private void shutDownInput() {
        if (this.mSocket != null) {
            try {
                this.mSocket.shutdownInput();
            } catch (IOException e) {
                Log.e(TAG, "Socket Shutdown Fail");
            }
        }
        this.mState = ReceiverState.STATE_INITIAL;
    }

    private void closeServer() {
        if (this.mServer != null) {
            try {
                this.mServer.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Close Fail");
            }
        }
    }

    private void closeSocket() {
        if (this.mSocket != null) {
            try {
                this.mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Close Fail");
            }
        }
    }

    private int getPort() {
        if (this.mServer == null) {
            return -1;
        }
        return this.mServer.getLocalPort();
    }

    private void closeHid() {
        nativeCloseHid();
    }

    private void CreateHandler() {
        try {
            this.mHandler = new UibcHandler(getLooper());
        } catch (Exception e) {
            Log.e(TAG, "Create Handler Fail");
            this.mState = ReceiverState.STATE_NONE;
        }
    }

    private ReceiverState getReceiverState() {
        return this.mState;
    }

    private void setReceiverState(ReceiverState state) {
        this.mState = state;
    }

    private void setInContextAndHandler(Context context, Handler handler) {
        this.mContext = context;
        this.mDisplayHandler = handler;
        refreshScreenSize();
        this.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                    if (HwUibcReceiver.instance != null) {
                        HwUibcReceiver.this.refreshScreenSize();
                    }
                } else if (action.equals("android.intent.action.SCREEN_ON")) {
                    if (HwUibcReceiver.instance != null) {
                        HwUibcReceiver.this.resumeReceiver();
                    }
                } else if (action.equals("android.intent.action.SCREEN_OFF") && HwUibcReceiver.instance != null) {
                    HwUibcReceiver.this.suspendReceiver();
                }
            }
        };
        IntentFilter filter = new IntentFilter("android.intent.action.CONFIGURATION_CHANGED");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    private void deInit() {
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    private synchronized void refreshScreenSize() {
        Context context = this.mContext;
        Context context2 = this.mContext;
        this.mDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        DisplayMetrics sRealMetrics = new DisplayMetrics();
        this.mDisplay.getRealMetrics(sRealMetrics);
        this.screenHeight = sRealMetrics.heightPixels;
        this.screenWidth = sRealMetrics.widthPixels;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("screenHeight : ");
        stringBuilder.append(this.screenHeight);
        stringBuilder.append(" screenWidth : ");
        stringBuilder.append(this.screenWidth);
        Log.i(str, stringBuilder.toString());
        if (this.screenHeight != 0) {
            this.screenAspectRatio = ((float) this.screenWidth) / ((float) this.screenHeight);
        }
    }

    private synchronized void sendUEBroadcast(String key, int info) {
        Intent intent = new Intent(ACTION_UIBC_USE_INFO);
        intent.addFlags(1073741824);
        intent.putExtra(KEY_UIBC_USE_INFO, info);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, WIFI_DISPLAY_UIBC_PERMISSION);
    }

    private void receiveEvent() {
        this.mHandler.sendEmptyMessageDelayed(2, 3);
    }

    public void sendMessage(int message) {
        this.mHandler.sendEmptyMessage(message);
    }

    /* JADX WARNING: Missing block: B:10:0x0017, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void makeAllUserToastAndShow(final String text) {
        if (this.mContext != null && this.mDisplayHandler != null) {
            this.mDisplayHandler.post(new Runnable() {
                public void run() {
                    Toast toast = Toast.makeText(HwUibcReceiver.this.mContext, text, 1);
                    LayoutParams windowParams = toast.getWindowParams();
                    windowParams.privateFlags |= 16;
                    toast.show();
                }
            });
        }
    }

    private String getMACAddress(InetAddress ia) {
        String arpPath = "/proc/net/arp";
        String ipAddress = ia.toString().substring(1);
        String macAddress = null;
        BufferedReader reader = null;
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(new FileInputStream("/proc/net/arp"), "UTF-8");
            reader = new BufferedReader(isr);
            String line = reader.readLine();
            while (true) {
                String readLine = reader.readLine();
                line = readLine;
                if (readLine == null) {
                    break;
                }
                String[] tokens = line.split("[ ]+");
                if (tokens.length >= 6) {
                    String ip = tokens[null];
                    String mac = tokens[3];
                    if (ipAddress.equals(ip)) {
                        macAddress = mac;
                        break;
                    }
                }
            }
            if (macAddress == null) {
                Log.e(TAG, "Did not find remoteAddress");
            }
            try {
                reader.close();
                isr.close();
            } catch (IOException e) {
                Log.e(TAG, "reader close wrong occur");
            }
        } catch (FileNotFoundException e2) {
            Log.e(TAG, "Could not open arp file to lookup mac address");
            if (reader != null) {
                reader.close();
            }
            if (isr != null) {
                isr.close();
            }
        } catch (IOException e3) {
            Log.e(TAG, "Could not read arp file to lookup mac address");
            if (reader != null) {
                reader.close();
            }
            if (isr != null) {
                isr.close();
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e4) {
                    Log.e(TAG, "reader close wrong occur");
                }
            }
            if (isr != null) {
                isr.close();
            }
        }
        return macAddress;
    }

    private boolean doIdCheck() {
        String curAddr = "";
        curAddr = getMACAddress(this.mSocket.getInetAddress());
        if (this.macAddress != null && curAddr != null && this.macAddress.substring(3, this.macAddress.length() - 3).equalsIgnoreCase(curAddr.substring(3, curAddr.length() - 3))) {
            return true;
        }
        String str;
        StringBuilder stringBuilder;
        if (this.macAddress != null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("check macAdress from wfd");
            stringBuilder.append(this.macAddress.substring(3, this.macAddress.length() - 3));
            Log.w(str, stringBuilder.toString());
        }
        if (curAddr != null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("check curAddr from uibc");
            stringBuilder.append(curAddr.substring(3, curAddr.length() - 3));
            Log.w(str, stringBuilder.toString());
        }
        return false;
    }
}
