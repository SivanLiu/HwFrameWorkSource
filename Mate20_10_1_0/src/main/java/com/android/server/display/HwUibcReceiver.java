package com.android.server.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.input.IInputManager;
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
import android.view.WindowManager;
import android.widget.Toast;
import com.android.server.intellicom.common.SmartDualCardConsts;
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
        /* class com.android.server.display.HwUibcReceiver.AnonymousClass3 */

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
    /* access modifiers changed from: private */
    public static InputEvent[] inputEvents = new InputEvent[10];
    /* access modifiers changed from: private */
    public static volatile HwUibcReceiver instance = null;
    private final IInputManager inputManager;
    /* access modifiers changed from: private */
    public boolean isUploadGenEvent;
    /* access modifiers changed from: private */
    public boolean isUploadHidEvent;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public CurGenericEvent mCurGenericEvent;
    /* access modifiers changed from: private */
    public CurrentPacket mCurrentPacket;
    private boolean mDestroyed;
    private Display mDisplay;
    private Handler mDisplayHandler;
    private Handler mHandler;
    /* access modifiers changed from: private */
    public BufferedInputStream mInput;
    /* access modifiers changed from: private */
    public int mParseBytes;
    private int mPort;
    /* access modifiers changed from: private */
    public int mReceiveBytes;
    private BroadcastReceiver mReceiver;
    /* access modifiers changed from: private */
    public ServerSocket mServer;
    /* access modifiers changed from: private */
    public Socket mSocket;
    /* access modifiers changed from: private */
    public ReceiverState mState;
    /* access modifiers changed from: private */
    public byte[] mUibcBuffer;
    private String macAddress;
    /* access modifiers changed from: private */
    public volatile float remoteAspectRatio;
    /* access modifiers changed from: private */
    public volatile int remoteHeight;
    /* access modifiers changed from: private */
    public volatile int remoteWidth;
    /* access modifiers changed from: private */
    public volatile float screenAspectRatio;
    /* access modifiers changed from: private */
    public volatile int screenHeight;
    /* access modifiers changed from: private */
    public volatile int screenWidth;
    boolean skiponce;

    public enum ReceiverState {
        STATE_NONE,
        STATE_INITIAL,
        STATE_WAITING,
        STATE_WORKING,
        STATE_SUSPENDING
    }

    private native void nativeCloseHid();

    /* access modifiers changed from: private */
    public native int nativeParseHid(byte[] bArr, int i);

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

    static {
        System.loadLibrary("uibcjni");
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
        if (!this.mDestroyed) {
            Log.w(TAG, "Duplicate create");
            return -1;
        }
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
        Log.i(TAG, "remoteHeight : " + height + " remoteWidth : " + width);
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
        this.inputManager = IInputManager.Stub.asInterface(ServiceManager.getService("input"));
        this.screenWidth = 0;
        this.screenHeight = 0;
        this.screenAspectRatio = 0.0f;
        this.remoteWidth = 0;
        this.remoteHeight = 0;
        this.remoteAspectRatio = 0.0f;
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
        Socket socket = this.mSocket;
        if (socket != null) {
            try {
                socket.shutdownInput();
            } catch (IOException e) {
                Log.e(TAG, "Socket Shutdown Fail");
            }
        }
        this.mState = ReceiverState.STATE_INITIAL;
    }

    private void closeServer() {
        ServerSocket serverSocket = this.mServer;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Close Fail");
            }
        }
    }

    /* access modifiers changed from: private */
    public void closeSocket() {
        Socket socket = this.mSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Close Fail");
            }
        }
    }

    private int getPort() {
        ServerSocket serverSocket = this.mServer;
        if (serverSocket == null) {
            return -1;
        }
        return serverSocket.getLocalPort();
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

    /* access modifiers changed from: private */
    public ReceiverState getReceiverState() {
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
            /* class com.android.server.display.HwUibcReceiver.AnonymousClass1 */

            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                        if (HwUibcReceiver.instance != null) {
                            HwUibcReceiver.this.refreshScreenSize();
                        }
                    } else if (action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON)) {
                        if (HwUibcReceiver.instance != null) {
                            HwUibcReceiver.this.resumeReceiver();
                        }
                    } else if (action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF) && HwUibcReceiver.instance != null) {
                        HwUibcReceiver.this.suspendReceiver();
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter("android.intent.action.CONFIGURATION_CHANGED");
        filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON);
        filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF);
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    private void deInit() {
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    /* access modifiers changed from: private */
    public synchronized void refreshScreenSize() {
        Context context = this.mContext;
        Context context2 = this.mContext;
        this.mDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        DisplayMetrics sRealMetrics = new DisplayMetrics();
        this.mDisplay.getRealMetrics(sRealMetrics);
        this.screenHeight = sRealMetrics.heightPixels;
        this.screenWidth = sRealMetrics.widthPixels;
        Log.i(TAG, "screenHeight : " + this.screenHeight + " screenWidth : " + this.screenWidth);
        if (this.screenHeight != 0) {
            this.screenAspectRatio = ((float) this.screenWidth) / ((float) this.screenHeight);
        }
    }

    /* access modifiers changed from: private */
    public synchronized void sendUEBroadcast(String key, int info) {
        Intent intent = new Intent(ACTION_UIBC_USE_INFO);
        intent.addFlags(1073741824);
        intent.putExtra(KEY_UIBC_USE_INFO, info);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, WIFI_DISPLAY_UIBC_PERMISSION);
    }

    class UibcHandler extends Handler {
        public UibcHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                Log.i(HwUibcReceiver.TAG, "UIBC_START -- Start Recevier Message Loop");
                if (HwUibcReceiver.this.mState != ReceiverState.STATE_INITIAL) {
                    Log.w(HwUibcReceiver.TAG, "Wrong state start! state : " + HwUibcReceiver.this.mState);
                }
                try {
                    ReceiverState unused = HwUibcReceiver.this.mState = ReceiverState.STATE_WAITING;
                    Log.d(HwUibcReceiver.TAG, "Begin accept SeverSocket");
                    Socket unused2 = HwUibcReceiver.this.mSocket = HwUibcReceiver.this.mServer.accept();
                    if (!HwUibcReceiver.this.doIdCheck()) {
                        Log.w(HwUibcReceiver.TAG, "the Evil Within!");
                        HwUibcReceiver.this.mSocket.close();
                        ReceiverState unused3 = HwUibcReceiver.this.mState = ReceiverState.STATE_INITIAL;
                        return;
                    }
                    Log.d(HwUibcReceiver.TAG, "Someone coming ! Accept");
                    BufferedInputStream unused4 = HwUibcReceiver.this.mInput = new BufferedInputStream(HwUibcReceiver.this.mSocket.getInputStream());
                    HwUibcReceiver.this.receiveEvent();
                    ReceiverState unused5 = HwUibcReceiver.this.mState = ReceiverState.STATE_WORKING;
                } catch (SocketTimeoutException e) {
                    Log.i(HwUibcReceiver.TAG, "Nobody come in");
                    HwUibcReceiver.this.sendUEBroadcast(HwUibcReceiver.KEY_UIBC_USE_INFO, 3);
                    ReceiverState unused6 = HwUibcReceiver.this.mState = ReceiverState.STATE_INITIAL;
                } catch (IOException e2) {
                    Log.e(HwUibcReceiver.TAG, "buffer error occur");
                    HwUibcReceiver.this.sendUEBroadcast(HwUibcReceiver.KEY_UIBC_USE_INFO, 4);
                    ReceiverState unused7 = HwUibcReceiver.this.mState = ReceiverState.STATE_INITIAL;
                }
            } else if (i == 2) {
                try {
                    if (HwUibcReceiver.this.mReceiveBytes == HwUibcReceiver.this.mParseBytes) {
                        int unused8 = HwUibcReceiver.this.mReceiveBytes = 0;
                        int unused9 = HwUibcReceiver.this.mParseBytes = 0;
                    } else if (HwUibcReceiver.this.mReceiveBytes > HwUibcReceiver.this.mParseBytes) {
                        moveBytesLeft(HwUibcReceiver.this.mParseBytes);
                        HwUibcReceiver.access$920(HwUibcReceiver.this, HwUibcReceiver.this.mParseBytes);
                        int unused10 = HwUibcReceiver.this.mParseBytes = 0;
                    }
                    int readCount = HwUibcReceiver.this.mInput.read(HwUibcReceiver.this.mUibcBuffer, HwUibcReceiver.this.mReceiveBytes, 100 - HwUibcReceiver.this.mReceiveBytes);
                    if (HwUibcReceiver.this.skiponce) {
                        int unused11 = HwUibcReceiver.this.mReceiveBytes = 0;
                        int unused12 = HwUibcReceiver.this.mParseBytes = 0;
                        HwUibcReceiver.this.skiponce = false;
                        HwUibcReceiver.this.receiveEvent();
                        return;
                    }
                    if (HwUibcReceiver.this.mState != ReceiverState.STATE_SUSPENDING) {
                        if (!HwPCUtils.isPcCastModeInServer()) {
                            if (readCount > 0) {
                                HwUibcReceiver.access$912(HwUibcReceiver.this, readCount);
                                int unused13 = HwUibcReceiver.this.mParseBytes = 0;
                            }
                            if (readCount > 0 || (readCount == 0 && HwUibcReceiver.this.mReceiveBytes == 100)) {
                                consumePacket();
                            }
                            if (HwUibcReceiver.this.mState == ReceiverState.STATE_WORKING) {
                                HwUibcReceiver.this.receiveEvent();
                                return;
                            }
                            return;
                        }
                    }
                    int unused14 = HwUibcReceiver.this.mReceiveBytes = HwUibcReceiver.this.mParseBytes;
                    HwUibcReceiver.this.receiveEvent();
                } catch (IOException ex) {
                    Log.e(HwUibcReceiver.TAG, "Read data error");
                    Log.e(HwUibcReceiver.TAG, "Exception : " + ex.toString());
                    int unused15 = HwUibcReceiver.this.mReceiveBytes = 0;
                    int unused16 = HwUibcReceiver.this.mParseBytes = 0;
                    HwUibcReceiver.this.skiponce = true;
                }
            } else if (i == 3) {
                Log.i(HwUibcReceiver.TAG, "UIBC_STOP -- Stop Recevier Message Loop");
                HwUibcReceiver.this.closeSocket();
            } else if (i == 4) {
                Log.i(HwUibcReceiver.TAG, "UIBC_SUSPEND");
            } else if (i != 5) {
                Log.i(HwUibcReceiver.TAG, "do not support the message");
            } else {
                Log.i(HwUibcReceiver.TAG, "UIBC_RESUME");
            }
        }

        private void consumePacket() {
            int parsedCount;
            while (HwUibcReceiver.this.mReceiveBytes > HwUibcReceiver.this.mParseBytes && HwUibcReceiver.this.mParseBytes + 4 <= 100) {
                int parsedCount2 = HwUibcReceiver.this.mCurrentPacket.resolveNaked(HwUibcReceiver.this.mUibcBuffer, HwUibcReceiver.this.mParseBytes);
                if (!HwUibcReceiver.this.mCurrentPacket.isVerValid() || parsedCount2 == 0) {
                    Log.w(HwUibcReceiver.TAG, "Unkonwn Version");
                    HwUibcReceiver hwUibcReceiver = HwUibcReceiver.this;
                    int unused = hwUibcReceiver.mParseBytes = hwUibcReceiver.mReceiveBytes;
                    return;
                } else if (HwUibcReceiver.this.mCurrentPacket.payloadLength > HwUibcReceiver.this.mReceiveBytes) {
                    Log.w(HwUibcReceiver.TAG, "UIBCReceiver need read more payloadlengh : " + HwUibcReceiver.this.mCurrentPacket.payloadLength + " Receive : " + HwUibcReceiver.this.mReceiveBytes);
                    return;
                } else if (HwUibcReceiver.this.mParseBytes + HwUibcReceiver.this.mCurrentPacket.payloadLength <= 100) {
                    HwUibcReceiver.access$1012(HwUibcReceiver.this, parsedCount2);
                    int i = HwUibcReceiver.this.mCurrentPacket.inputCategory;
                    if (i == 0) {
                        if (!HwUibcReceiver.this.isUploadGenEvent) {
                            boolean unused2 = HwUibcReceiver.this.isUploadGenEvent = true;
                            HwUibcReceiver.this.sendUEBroadcast(HwUibcReceiver.KEY_UIBC_USE_INFO, 1);
                        }
                        parsedCount = WorkGenericInput();
                    } else if (i == 1) {
                        if (!HwUibcReceiver.this.isUploadHidEvent) {
                            boolean unused3 = HwUibcReceiver.this.isUploadHidEvent = true;
                            HwUibcReceiver.this.sendUEBroadcast(HwUibcReceiver.KEY_UIBC_USE_INFO, 2);
                        }
                        HwUibcReceiver hwUibcReceiver2 = HwUibcReceiver.this;
                        parsedCount = hwUibcReceiver2.nativeParseHid(hwUibcReceiver2.mUibcBuffer, HwUibcReceiver.this.mParseBytes);
                        if (parsedCount2 + parsedCount != HwUibcReceiver.this.mCurrentPacket.payloadLength) {
                            parsedCount = HwUibcReceiver.this.mCurrentPacket.payloadLength - parsedCount2;
                        }
                    } else {
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
            if (eventCount > HwUibcReceiver.inputEvents.length) {
                return 0;
            }
            int i = 0;
            while (i < eventCount) {
                try {
                    InputManager.getInstance().injectInputEvent(HwUibcReceiver.inputEvents[i], 2);
                    i++;
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

    /* access modifiers changed from: private */
    public void receiveEvent() {
        this.mHandler.sendEmptyMessageDelayed(2, 3);
    }

    public void sendMessage(int message) {
        this.mHandler.sendEmptyMessage(message);
    }

    private synchronized void makeAllUserToastAndShow(final String text) {
        if (this.mContext != null) {
            if (this.mDisplayHandler != null) {
                this.mDisplayHandler.post(new Runnable() {
                    /* class com.android.server.display.HwUibcReceiver.AnonymousClass2 */

                    public void run() {
                        Toast toast = Toast.makeText(HwUibcReceiver.this.mContext, text, 1);
                        toast.getWindowParams().privateFlags |= 16;
                        toast.show();
                    }
                });
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:39:0x00a5 A[SYNTHETIC, Splitter:B:39:0x00a5] */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00b3 A[SYNTHETIC, Splitter:B:44:0x00b3] */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x00c1  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00cf A[SYNTHETIC, Splitter:B:54:0x00cf] */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x00dd A[SYNTHETIC, Splitter:B:59:0x00dd] */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x00eb  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x00f4 A[SYNTHETIC, Splitter:B:69:0x00f4] */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0102 A[SYNTHETIC, Splitter:B:74:0x0102] */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x0110 A[SYNTHETIC, Splitter:B:79:0x0110] */
    private String getMACAddress(InetAddress ia) {
        Throwable th;
        int IP_FIRST = 1;
        String ipAddress = ia.toString().substring(1);
        String macAddress2 = null;
        BufferedReader reader = null;
        InputStreamReader isr = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream("/proc/net/arp");
            isr = new InputStreamReader(fileInputStream, "UTF-8");
            reader = new BufferedReader(isr);
            reader.readLine();
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                String[] tokens = line.split("[ ]+");
                if (tokens.length >= 6) {
                    try {
                        String ip = tokens[0];
                        String mac = tokens[3];
                        if (ipAddress.equals(ip)) {
                            macAddress2 = mac;
                            break;
                        }
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "Could not open arp file to lookup mac address");
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e2) {
                                Log.e(TAG, "reader close wrong occur");
                            }
                        }
                        if (isr != null) {
                            try {
                                isr.close();
                            } catch (IOException e3) {
                                Log.e(TAG, "isr close wrong occur");
                            }
                        }
                        if (fileInputStream != null) {
                            fileInputStream.close();
                        }
                        return macAddress2;
                    } catch (IOException e4) {
                        try {
                            Log.e(TAG, "Could not read arp file to lookup mac address");
                            if (reader != null) {
                                try {
                                    reader.close();
                                } catch (IOException e5) {
                                    Log.e(TAG, "reader close wrong occur");
                                }
                            }
                            if (isr != null) {
                                try {
                                    isr.close();
                                } catch (IOException e6) {
                                    Log.e(TAG, "isr close wrong occur");
                                }
                            }
                            if (fileInputStream != null) {
                                fileInputStream.close();
                            }
                            return macAddress2;
                        } catch (Throwable th2) {
                            th = th2;
                            if (reader != null) {
                            }
                            if (isr != null) {
                            }
                            if (fileInputStream != null) {
                            }
                            throw th;
                        }
                    }
                }
                IP_FIRST = IP_FIRST;
            }
            if (macAddress2 == null) {
                Log.e(TAG, "Did not find remoteAddress");
            }
            try {
                reader.close();
            } catch (IOException e7) {
                Log.e(TAG, "reader close wrong occur");
            }
            try {
                isr.close();
            } catch (IOException e8) {
                Log.e(TAG, "isr close wrong occur");
            }
            try {
                fileInputStream.close();
            } catch (IOException e9) {
                Log.e(TAG, "fileInputStream close wrong occur");
            }
        } catch (FileNotFoundException e10) {
            Log.e(TAG, "Could not open arp file to lookup mac address");
            if (reader != null) {
            }
            if (isr != null) {
            }
            if (fileInputStream != null) {
            }
            return macAddress2;
        } catch (IOException e11) {
            Log.e(TAG, "Could not read arp file to lookup mac address");
            if (reader != null) {
            }
            if (isr != null) {
            }
            if (fileInputStream != null) {
            }
            return macAddress2;
        } catch (Throwable th3) {
            th = th3;
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e12) {
                    Log.e(TAG, "reader close wrong occur");
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e13) {
                    Log.e(TAG, "isr close wrong occur");
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e14) {
                    Log.e(TAG, "fileInputStream close wrong occur");
                }
            }
            throw th;
        }
        return macAddress2;
    }

    /* access modifiers changed from: private */
    public boolean doIdCheck() {
        String curAddr = getMACAddress(this.mSocket.getInetAddress());
        String str = this.macAddress;
        if (str != null && curAddr != null && str.substring(3, str.length() - 3).equalsIgnoreCase(curAddr.substring(3, curAddr.length() - 3))) {
            return true;
        }
        if (this.macAddress != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("check macAdress from wfd");
            String str2 = this.macAddress;
            sb.append(str2.substring(3, str2.length() - 3));
            Log.w(TAG, sb.toString());
        }
        if (curAddr == null) {
            return false;
        }
        Log.w(TAG, "check curAddr from uibc" + curAddr.substring(3, curAddr.length() - 3));
        return false;
    }

    public static class CurrentPacket {
        public static final int BODY_INDEX_WITHOUT_TIME = 4;
        public static final int INPUT_CATEGORY_INDEX = 1;
        public static final byte INPUT_MASK = 15;
        public static final int LENGTH_INDEX = 2;
        public static final int TIME_INDEX = 0;
        public static final byte TIME_MASK = 1;
        public static final int TIME_OFFSET = 4;
        public static final int VERSION_INDEX = 0;
        public static final byte VERSION_MASK = 7;
        public static final int VERSION_OFFSET = 5;
        public static final int VERSION_VALID = 0;
        public int bodyIndex;
        public int curVer;
        public boolean hasTime;
        public int inputCategory;
        public int payloadLength;

        public int resolveNaked(byte[] payload, int index) {
            boolean z = false;
            if (index < 0 || index + 2 + 1 >= payload.length) {
                Log.w(HwUibcReceiver.TAG, "resolveNaked overflow, not expect go in");
                return 0;
            }
            this.curVer = (payload[index + 0] >> 5) & 7;
            int i = 4;
            if (((payload[index + 0] >> 4) & 1) == 1) {
                z = true;
            }
            this.hasTime = z;
            this.inputCategory = payload[index + 1] & INPUT_MASK;
            this.payloadLength = (payload[index + 2] << 8) | payload[index + 3];
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
        private MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[10];
        private MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[10];
        private MotionEvent prevEvent = null;

        public CurGenericEvent() {
            for (int i = 0; i < 10; i++) {
                this.pointerProperties[i] = new MotionEvent.PointerProperties();
                this.pointerCoords[i] = new MotionEvent.PointerCoords();
            }
        }

        public int resolveInputBody(byte[] payload, int index) {
            if (index < 0 || index + 2 >= payload.length) {
                Log.w(HwUibcReceiver.TAG, "resolveInputBody overflow, not expect go in");
                return 0;
            }
            this.inputTypeID = payload[index + 0];
            this.bodyLength = (payload[index + 1] << 8) | payload[index + 2];
            return this.bodyLength + 3;
        }

        public int resolveEvent(byte[] payload, int index) {
            int i = this.inputTypeID;
            if (i == 0) {
                return createTouchEvents(payload, index, 0);
            }
            if (i == 1) {
                return createTouchEvents(payload, index, 1);
            }
            if (i == 2) {
                return createTouchEvents(payload, index, 2);
            }
            if (i == 3) {
                return createKeyEvent(payload, index, true);
            }
            if (i == 4) {
                return createKeyEvent(payload, index, false);
            }
            Log.d(HwUibcReceiver.TAG, "unsupport event");
            return 0;
        }

        private int createKeyEvent(byte[] payload, int index, boolean isdown) {
            if (index < 0 || index + 5 >= payload.length) {
                Log.w(HwUibcReceiver.TAG, "createKeyEvent overflow, not expect go in");
                return 0;
            }
            byte b = payload[index + 5];
            HwUibcReceiver.inputEvents[0] = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), !isdown, b, 0);
            return 1;
        }

        /* JADX WARNING: Code restructure failed: missing block: B:24:0x00a0, code lost:
            r0 = -1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:25:0x00a3, code lost:
            if (r38.isMultiTouch != false) goto L_0x00aa;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:27:0x00a6, code lost:
            if (r5 <= 1) goto L_0x00aa;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:28:0x00a8, code lost:
            r38.isMultiTouch = true;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:30:0x00ac, code lost:
            if (r38.isMultiTouch == false) goto L_0x018f;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:31:0x00ae, code lost:
            r2 = r38.prevEvent;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:32:0x00b0, code lost:
            if (r2 == null) goto L_0x0162;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:34:0x00b6, code lost:
            if (r2.getPointerCount() >= r5) goto L_0x00bb;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:35:0x00b8, code lost:
            r15 = true;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:37:0x00c1, code lost:
            if (r38.prevEvent.getPointerCount() <= r5) goto L_0x015f;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:38:0x00c3, code lost:
            android.util.Log.d(com.android.server.display.HwUibcReceiver.TAG, "UIBC  pointer up");
            r6 = new java.util.ArrayList<>();
            r7 = r38.prevEvent.getPointerCount();
            r8 = 0;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:39:0x00d7, code lost:
            if (r8 >= r7) goto L_0x00e9;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:40:0x00d9, code lost:
            r6.add(java.lang.Integer.valueOf(r38.prevEvent.getPointerId(r8)));
            r8 = r8 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:41:0x00e9, code lost:
            r8 = 0;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:42:0x00ea, code lost:
            if (r8 >= r5) goto L_0x00fc;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:43:0x00ec, code lost:
            r6.remove(java.lang.Integer.valueOf(r38.prevEvent.getPointerId(r8)));
            r8 = r8 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:45:0x0101, code lost:
            if (r6.size() == 1) goto L_0x0107;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:46:0x0103, code lost:
            r8 = 0;
            r38.isMultiTouch = false;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:47:0x0107, code lost:
            r8 = 0;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:48:0x0108, code lost:
            com.android.server.display.HwUibcReceiver.access$1800()[0] = android.view.MotionEvent.obtain(r38.prevEvent.getDownTime(), android.os.SystemClock.uptimeMillis(), (6 & 255) | (r38.prevEvent.findPointerIndex(r6.get(r8).intValue()) << 8), r38.prevEvent.getPointerCount(), r38.pointerProperties, r38.pointerCoords, 0, 1, 2.0f, 2.1f, 0, 0, 4098, 0);
            r15 = true;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:49:0x0154, code lost:
            if (r5 != 1) goto L_0x015b;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:50:0x0156, code lost:
            r38.isMultiTouch = false;
            r0 = r41;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:51:0x015b, code lost:
            r3 = 0 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:52:0x015f, code lost:
            r15 = true;
            r0 = 2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:53:0x0162, code lost:
            r15 = true;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:54:0x0163, code lost:
            android.util.Log.d(com.android.server.display.HwUibcReceiver.TAG, "UIBC  pointer down");
            r0 = 5 & 255;
            r2 = 0;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:55:0x016e, code lost:
            if (r2 >= r5) goto L_0x0192;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:56:0x0170, code lost:
            r6 = r38.prevEvent;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:57:0x0172, code lost:
            if (r6 == null) goto L_0x0185;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:59:0x017f, code lost:
            if (r6.findPointerIndex(r38.pointerProperties[r2].id) != -1) goto L_0x0182;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:60:0x0182, code lost:
            r2 = r2 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:61:0x0185, code lost:
            r0 = r0 | (r38.pointerProperties[r2].id << 8);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:62:0x018f, code lost:
            r15 = true;
            r0 = r41;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:63:0x0192, code lost:
            if (r5 != 0) goto L_0x019e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:64:0x0194, code lost:
            android.util.Log.w(com.android.server.display.HwUibcReceiver.TAG, "numPointers is zero");
         */
        /* JADX WARNING: Code restructure failed: missing block: B:65:0x019d, code lost:
            return 0;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:66:0x019e, code lost:
            com.android.server.display.HwUibcReceiver.access$1800()[r3] = android.view.MotionEvent.obtain(android.os.SystemClock.uptimeMillis(), android.os.SystemClock.uptimeMillis(), r0, r5, r38.pointerProperties, r38.pointerCoords, 0, 1, 2.0f, 2.1f, 0, 0, 4098, 0);
            r38.prevEvent = (android.view.MotionEvent) com.android.server.display.HwUibcReceiver.access$1800()[r3];
            r3 = r3 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:67:0x01e2, code lost:
            if (r4 != false) goto L_0x01e6;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:68:0x01e4, code lost:
            return 0;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:69:0x01e6, code lost:
            return r3;
         */
        private int createTouchEvents(byte[] payload, int index, int action) {
            byte[] bArr = payload;
            int ret = 0;
            boolean injectFlag = true;
            if (index < 0 || index + 3 >= bArr.length) {
                Log.w(HwUibcReceiver.TAG, "createTouchEvents overflow, not expect go in");
                return 0;
            }
            byte b = bArr[index + 3];
            this.curPointerIndex = 0;
            synchronized (HwUibcReceiver.class) {
                if (!(HwUibcReceiver.this.screenHeight == 0 || HwUibcReceiver.this.screenWidth == 0 || HwUibcReceiver.this.remoteHeight == 0)) {
                    if (HwUibcReceiver.this.remoteWidth != 0) {
                        if (b >= 0) {
                            int i = 1;
                            if (index + 7 + 1 + ((b - 1) * 5) < bArr.length) {
                                int i2 = 0;
                                while (i2 < b) {
                                    injectFlag = doTranslateCoord(((float) (((bArr[(index + 5) + (i2 * 5)] & 255) << 8) | (bArr[((index + 5) + i) + (i2 * 5)] & 255))) / ((float) HwUibcReceiver.this.remoteWidth), ((float) (((bArr[(index + 7) + (i2 * 5)] & 255) << 8) | (bArr[((index + 7) + i) + (i2 * 5)] & 255))) / ((float) HwUibcReceiver.this.remoteHeight), bArr[index + 4 + (i2 * 5)]);
                                    i2++;
                                    bArr = payload;
                                    i = 1;
                                }
                            }
                        }
                        Log.w(HwUibcReceiver.TAG, "createTouchEvents  numPointers  overflow, not expect go in");
                        return 0;
                    }
                }
                Log.w(HwUibcReceiver.TAG, "screen size is wrong");
                return 0;
            }
        }

        private boolean doTranslateCoord(float x, float y, int id) {
            float x2 = x;
            float y2 = y;
            boolean isValid = true;
            int i = this.curPointerIndex;
            MotionEvent.PointerProperties[] pointerPropertiesArr = this.pointerProperties;
            if (i >= pointerPropertiesArr.length || i >= this.pointerCoords.length) {
                return false;
            }
            if (i < 0) {
                return false;
            }
            if (x2 >= 0.0f && x2 <= 1.0f && y2 >= 0.0f && y2 <= 1.0f) {
                pointerPropertiesArr[i].clear();
                MotionEvent.PointerProperties[] pointerPropertiesArr2 = this.pointerProperties;
                int i2 = this.curPointerIndex;
                pointerPropertiesArr2[i2].id = id;
                this.pointerCoords[i2].clear();
                if (HwUibcReceiver.this.screenAspectRatio < HwUibcReceiver.this.remoteAspectRatio) {
                    if (HwUibcReceiver.this.remoteAspectRatio != 0.0f) {
                        x2 = (((x2 - 0.5f) / (HwUibcReceiver.this.screenAspectRatio / (HwUibcReceiver.this.remoteAspectRatio * 2.0f))) + 1.0f) * 0.5f;
                        if (x2 < 0.0f || x2 > 1.0f) {
                            Log.i(HwUibcReceiver.TAG, "x: " + x2 + " y: " + y2);
                            y2 = -1.0f;
                            x2 = -1.0f;
                            isValid = false;
                        }
                    } else {
                        Log.e(HwUibcReceiver.TAG, "UIBC  remoteAspectRatio is zero");
                    }
                } else if (HwUibcReceiver.this.screenAspectRatio > HwUibcReceiver.this.remoteAspectRatio) {
                    if (HwUibcReceiver.this.screenAspectRatio != 0.0f) {
                        y2 = (((y2 - 0.5f) / (HwUibcReceiver.this.remoteAspectRatio / (HwUibcReceiver.this.screenAspectRatio * 2.0f))) + 1.0f) * 0.5f;
                        if (y2 < 0.0f || y2 > 1.0f) {
                            Log.i(HwUibcReceiver.TAG, "x: " + x2 + " y: " + y2);
                            y2 = -1.0f;
                            x2 = -1.0f;
                            isValid = false;
                        }
                    } else {
                        Log.e(HwUibcReceiver.TAG, "UIBC  screenAspectRatio is zero");
                    }
                }
                if (x2 < 0.0f || x2 > 1.0f || y2 < 0.0f || y2 > 1.0f) {
                    return false;
                }
                this.pointerCoords[this.curPointerIndex].setAxisValue(0, ((float) HwUibcReceiver.this.screenWidth) * x2);
                this.pointerCoords[this.curPointerIndex].setAxisValue(1, ((float) HwUibcReceiver.this.screenHeight) * y2);
                this.pointerCoords[this.curPointerIndex].setAxisValue(2, 0.8f);
                this.curPointerIndex++;
            }
            return isValid;
        }
    }
}
