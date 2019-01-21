package android.app;

import android.accessibilityservice.AccessibilityService.Callbacks;
import android.accessibilityservice.AccessibilityService.IAccessibilityServiceClientWrapper;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.accessibilityservice.IAccessibilityServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.view.Display;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.WindowAnimationFrameStats;
import android.view.WindowContentFrameStats;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityInteractionClient;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import com.android.internal.util.function.pooled.PooledLambda;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import libcore.io.IoUtils;

public final class UiAutomation {
    private static final int CONNECTION_ID_UNDEFINED = -1;
    private static final long CONNECT_TIMEOUT_MILLIS = 10000;
    private static final boolean DEBUG = false;
    public static final int FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES = 1;
    private static final String LOG_TAG = UiAutomation.class.getSimpleName();
    public static final int ROTATION_FREEZE_0 = 0;
    public static final int ROTATION_FREEZE_180 = 2;
    public static final int ROTATION_FREEZE_270 = 3;
    public static final int ROTATION_FREEZE_90 = 1;
    public static final int ROTATION_FREEZE_CURRENT = -1;
    public static final int ROTATION_UNFREEZE = -2;
    private IAccessibilityServiceClient mClient;
    private int mConnectionId = -1;
    private final ArrayList<AccessibilityEvent> mEventQueue = new ArrayList();
    private int mFlags;
    private boolean mIsConnecting;
    private boolean mIsDestroyed;
    private long mLastEventTimeMillis;
    private final Handler mLocalCallbackHandler;
    private final Object mLock = new Object();
    private OnAccessibilityEventListener mOnAccessibilityEventListener;
    private HandlerThread mRemoteCallbackThread;
    private final IUiAutomationConnection mUiAutomationConnection;
    private boolean mWaitingForEventDelivery;

    public interface AccessibilityEventFilter {
        boolean accept(AccessibilityEvent accessibilityEvent);
    }

    public interface OnAccessibilityEventListener {
        void onAccessibilityEvent(AccessibilityEvent accessibilityEvent);
    }

    private class IAccessibilityServiceClientImpl extends IAccessibilityServiceClientWrapper {
        public IAccessibilityServiceClientImpl(Looper looper) {
            super(null, looper, new Callbacks(UiAutomation.this) {
                public void init(int connectionId, IBinder windowToken) {
                    synchronized (r2.mLock) {
                        r2.mConnectionId = connectionId;
                        r2.mLock.notifyAll();
                    }
                }

                public void onServiceConnected() {
                }

                public void onInterrupt() {
                }

                public boolean onGesture(int gestureId) {
                    return false;
                }

                public void onAccessibilityEvent(AccessibilityEvent event) {
                    OnAccessibilityEventListener listener;
                    synchronized (r2.mLock) {
                        r2.mLastEventTimeMillis = event.getEventTime();
                        if (r2.mWaitingForEventDelivery) {
                            r2.mEventQueue.add(AccessibilityEvent.obtain(event));
                        }
                        r2.mLock.notifyAll();
                        listener = r2.mOnAccessibilityEventListener;
                    }
                    if (listener != null) {
                        r2.mLocalCallbackHandler.post(PooledLambda.obtainRunnable(-$$Lambda$GnVtsLTLDH5bZdtLeTd6cfwpgcs.INSTANCE, listener, AccessibilityEvent.obtain(event)).recycleOnUse());
                    }
                }

                public boolean onKeyEvent(KeyEvent event) {
                    return false;
                }

                public void onMagnificationChanged(Region region, float scale, float centerX, float centerY) {
                }

                public void onSoftKeyboardShowModeChanged(int showMode) {
                }

                public void onPerformGestureResult(int sequence, boolean completedSuccessfully) {
                }

                public void onFingerprintCapturingGesturesChanged(boolean active) {
                }

                public void onFingerprintGesture(int gesture) {
                }

                public void onAccessibilityButtonClicked() {
                }

                public void onAccessibilityButtonAvailabilityChanged(boolean available) {
                }
            });
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: SSATransform
        jadx.core.utils.exceptions.JadxRuntimeException: Not initialized variable reg: 7, insn: 0x007f: INVOKE  (r7 java.util.List), (r11 java.lang.Object) java.util.List.add(java.lang.Object):boolean type: INTERFACE, block:B:37:?, method: android.app.UiAutomation.executeAndWaitForEvent(java.lang.Runnable, android.app.UiAutomation$AccessibilityEventFilter, long):android.view.accessibility.AccessibilityEvent
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:162)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:133)
        	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
        	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public android.view.accessibility.AccessibilityEvent executeAndWaitForEvent(java.lang.Runnable r20, android.app.UiAutomation.AccessibilityEventFilter r21, long r22) throws java.util.concurrent.TimeoutException {
        /*
        r19 = this;
        r1 = r19;
        r2 = r22;
        r4 = r1.mLock;
        monitor-enter(r4);
        r19.throwIfNotConnectedLocked();	 Catch:{ all -> 0x010c }
        r0 = r1.mEventQueue;	 Catch:{ all -> 0x010c }
        r0.clear();	 Catch:{ all -> 0x010c }
        r0 = 1;	 Catch:{ all -> 0x010c }
        r1.mWaitingForEventDelivery = r0;	 Catch:{ all -> 0x010c }
        monitor-exit(r4);	 Catch:{ all -> 0x010c }
        r5 = android.os.SystemClock.uptimeMillis();
        r20.run();
        r0 = new java.util.ArrayList;
        r0.<init>();
        r7 = r0;
        r4 = 0;
        r8 = android.os.SystemClock.uptimeMillis();	 Catch:{ all -> 0x00e0 }
        r0 = new java.util.ArrayList;	 Catch:{ all -> 0x00e0 }
        r0.<init>();	 Catch:{ all -> 0x00e0 }
        r10 = r0;	 Catch:{ all -> 0x00e0 }
        r11 = r1.mLock;	 Catch:{ all -> 0x00e0 }
        monitor-enter(r11);	 Catch:{ all -> 0x00e0 }
        r0 = r1.mEventQueue;	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r10.addAll(r0);	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r0 = r1.mEventQueue;	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r0.clear();	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        monitor-exit(r11);	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r0 = r10.isEmpty();	 Catch:{ all -> 0x00e0 }
        if (r0 != 0) goto L_0x008a;
        r0 = r10.remove(r4);	 Catch:{ all -> 0x0085 }
        r0 = (android.view.accessibility.AccessibilityEvent) r0;	 Catch:{ all -> 0x0085 }
        r11 = r0;	 Catch:{ all -> 0x0085 }
        r12 = r11.getEventTime();	 Catch:{ all -> 0x0085 }
        r0 = (r12 > r5 ? 1 : (r12 == r5 ? 0 : -1));
        if (r0 >= 0) goto L_0x004f;
        goto L_0x0039;
        r12 = r21;
        r0 = r12.accept(r11);	 Catch:{ all -> 0x0083 }
        if (r0 == 0) goto L_0x007f;
        r13 = r7.size();
        r0 = r4;
        if (r0 >= r13) goto L_0x006b;
        r14 = r7.get(r0);
        r14 = (android.view.accessibility.AccessibilityEvent) r14;
        r14.recycle();
        r0 = r0 + 1;
        goto L_0x005d;
        r14 = r1.mLock;
        monitor-enter(r14);
        r1.mWaitingForEventDelivery = r4;
        r0 = r1.mEventQueue;
        r0.clear();
        r0 = r1.mLock;
        r0.notifyAll();
        monitor-exit(r14);
        return r11;
        r0 = move-exception;
        monitor-exit(r14);
        throw r0;
        r7.add(r11);	 Catch:{ all -> 0x0083 }
        goto L_0x0039;
        r0 = move-exception;
        goto L_0x0088;
        r0 = move-exception;
        r12 = r21;
        r15 = r5;
        goto L_0x00e4;
        r12 = r21;
        r13 = android.os.SystemClock.uptimeMillis();	 Catch:{ all -> 0x00d4 }
        r13 = r13 - r8;
        r15 = r5;
        r4 = r2 - r13;
        r17 = 0;
        r0 = (r4 > r17 ? 1 : (r4 == r17 ? 0 : -1));
        if (r0 <= 0) goto L_0x00b5;
        r6 = r1.mLock;	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        monitor-enter(r6);	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r0 = r1.mEventQueue;	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r0 = r0.isEmpty();	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        if (r0 == 0) goto L_0x00ac;
        r0 = r1.mLock;	 Catch:{ InterruptedException -> 0x00ab }
        r0.wait(r4);	 Catch:{ InterruptedException -> 0x00ab }
        goto L_0x00ac;
        r0 = move-exception;
        monitor-exit(r6);	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r5 = r15;	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r4 = 0;	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        goto L_0x0025;	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r0 = move-exception;	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        monitor-exit(r6);	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        throw r0;	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r0 = new java.util.concurrent.TimeoutException;	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r6.<init>();	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r11 = "Expected event not received within: ";	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r6.append(r11);	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r6.append(r2);	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r11 = " ms among: ";	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r6.append(r11);	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r6.append(r7);	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r6 = r6.toString();	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r0.<init>(r6);	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        throw r0;	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r0 = move-exception;
        goto L_0x00e3;
        r0 = move-exception;
        r12 = r21;
        r15 = r5;
        monitor-exit(r11);	 Catch:{ all -> 0x00de }
        throw r0;	 Catch:{ all -> 0x00d6, all -> 0x00dc }
        r0 = move-exception;
        goto L_0x00e4;
        r0 = move-exception;
        goto L_0x00da;
        r0 = move-exception;
        r12 = r21;
        r15 = r5;
        r5 = r7.size();
        r4 = 0;
        if (r4 >= r5) goto L_0x00f7;
        r6 = r7.get(r4);
        r6 = (android.view.accessibility.AccessibilityEvent) r6;
        r6.recycle();
        r4 = r4 + 1;
        goto L_0x00e9;
        r6 = r1.mLock;
        monitor-enter(r6);
        r4 = 0;
        r1.mWaitingForEventDelivery = r4;
        r4 = r1.mEventQueue;
        r4.clear();
        r4 = r1.mLock;
        r4.notifyAll();
        monitor-exit(r6);
        throw r0;
        r0 = move-exception;
        monitor-exit(r6);
        throw r0;
        r0 = move-exception;
        r12 = r21;
        monitor-exit(r4);	 Catch:{ all -> 0x0111 }
        throw r0;
        r0 = move-exception;
        goto L_0x010f;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.app.UiAutomation.executeAndWaitForEvent(java.lang.Runnable, android.app.UiAutomation$AccessibilityEventFilter, long):android.view.accessibility.AccessibilityEvent");
    }

    public UiAutomation(Looper looper, IUiAutomationConnection connection) {
        if (looper == null) {
            throw new IllegalArgumentException("Looper cannot be null!");
        } else if (connection != null) {
            this.mLocalCallbackHandler = new Handler(looper);
            this.mUiAutomationConnection = connection;
        } else {
            throw new IllegalArgumentException("Connection cannot be null!");
        }
    }

    public void connect() {
        connect(0);
    }

    /* JADX WARNING: Missing block: B:10:?, code skipped:
            r10.mUiAutomationConnection.connect(r10.mClient, r11);
            r10.mFlags = r11;
     */
    /* JADX WARNING: Missing block: B:11:0x0034, code skipped:
            r0 = r10.mLock;
     */
    /* JADX WARNING: Missing block: B:12:0x0037, code skipped:
            monitor-enter(r0);
     */
    /* JADX WARNING: Missing block: B:14:?, code skipped:
            r1 = android.os.SystemClock.uptimeMillis();
     */
    /* JADX WARNING: Missing block: B:18:0x0041, code skipped:
            if (isConnectedLocked() == false) goto L_0x0049;
     */
    /* JADX WARNING: Missing block: B:20:0x0047, code skipped:
            monitor-exit(r0);
     */
    /* JADX WARNING: Missing block: B:21:0x0048, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:24:0x004d, code skipped:
            r6 = 10000 - (android.os.SystemClock.uptimeMillis() - r1);
     */
    /* JADX WARNING: Missing block: B:25:0x0055, code skipped:
            if (r6 <= 0) goto L_0x005f;
     */
    /* JADX WARNING: Missing block: B:27:?, code skipped:
            r10.mLock.wait(r6);
     */
    /* JADX WARNING: Missing block: B:32:0x0066, code skipped:
            throw new java.lang.RuntimeException("Error while connecting UiAutomation");
     */
    /* JADX WARNING: Missing block: B:35:?, code skipped:
            r10.mIsConnecting = false;
     */
    /* JADX WARNING: Missing block: B:37:0x006e, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:39:0x0076, code skipped:
            throw new java.lang.RuntimeException("Error while connecting UiAutomation", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void connect(int flags) {
        synchronized (this.mLock) {
            throwIfConnectedLocked();
            if (this.mIsConnecting) {
                return;
            }
            this.mIsConnecting = true;
            this.mRemoteCallbackThread = new HandlerThread("UiAutomation");
            this.mRemoteCallbackThread.start();
            this.mClient = new IAccessibilityServiceClientImpl(this.mRemoteCallbackThread.getLooper());
        }
    }

    public int getFlags() {
        return this.mFlags;
    }

    public void disconnect() {
        synchronized (this.mLock) {
            if (this.mIsConnecting) {
                throw new IllegalStateException("Cannot call disconnect() while connecting!");
            }
            throwIfNotConnectedLocked();
            this.mConnectionId = -1;
        }
        try {
            this.mUiAutomationConnection.disconnect();
            this.mRemoteCallbackThread.quit();
            this.mRemoteCallbackThread = null;
        } catch (RemoteException re) {
            throw new RuntimeException("Error while disconnecting UiAutomation", re);
        } catch (Throwable th) {
            this.mRemoteCallbackThread.quit();
            this.mRemoteCallbackThread = null;
        }
    }

    public int getConnectionId() {
        int i;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            i = this.mConnectionId;
        }
        return i;
    }

    public boolean isDestroyed() {
        return this.mIsDestroyed;
    }

    public void setOnAccessibilityEventListener(OnAccessibilityEventListener listener) {
        synchronized (this.mLock) {
            this.mOnAccessibilityEventListener = listener;
        }
    }

    public void destroy() {
        disconnect();
        this.mIsDestroyed = true;
    }

    public final boolean performGlobalAction(int action) {
        IAccessibilityServiceConnection connection;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            AccessibilityInteractionClient.getInstance();
            connection = AccessibilityInteractionClient.getConnection(this.mConnectionId);
        }
        if (connection != null) {
            try {
                return connection.performGlobalAction(action);
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while calling performGlobalAction", re);
            }
        }
        return false;
    }

    public AccessibilityNodeInfo findFocus(int focus) {
        return AccessibilityInteractionClient.getInstance().findFocus(this.mConnectionId, -2, AccessibilityNodeInfo.ROOT_NODE_ID, focus);
    }

    public final AccessibilityServiceInfo getServiceInfo() {
        IAccessibilityServiceConnection connection;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            AccessibilityInteractionClient.getInstance();
            connection = AccessibilityInteractionClient.getConnection(this.mConnectionId);
        }
        if (connection != null) {
            try {
                return connection.getServiceInfo();
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while getting AccessibilityServiceInfo", re);
            }
        }
        return null;
    }

    public final void setServiceInfo(AccessibilityServiceInfo info) {
        IAccessibilityServiceConnection connection;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            AccessibilityInteractionClient.getInstance().clearCache();
            AccessibilityInteractionClient.getInstance();
            connection = AccessibilityInteractionClient.getConnection(this.mConnectionId);
        }
        if (connection != null) {
            try {
                connection.setServiceInfo(info);
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while setting AccessibilityServiceInfo", re);
            }
        }
    }

    public List<AccessibilityWindowInfo> getWindows() {
        int connectionId;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            connectionId = this.mConnectionId;
        }
        return AccessibilityInteractionClient.getInstance().getWindows(connectionId);
    }

    public AccessibilityNodeInfo getRootInActiveWindow() {
        int connectionId;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            connectionId = this.mConnectionId;
        }
        return AccessibilityInteractionClient.getInstance().getRootInActiveWindow(connectionId);
    }

    public boolean injectInputEvent(InputEvent event, boolean sync) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            return this.mUiAutomationConnection.injectInputEvent(event, sync);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error while injecting input event!", re);
            return false;
        }
    }

    public boolean setRotation(int rotation) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        switch (rotation) {
            case -2:
            case -1:
            case 0:
            case 1:
            case 2:
            case 3:
                try {
                    this.mUiAutomationConnection.setRotation(rotation);
                    return true;
                } catch (RemoteException re) {
                    Log.e(LOG_TAG, "Error while setting rotation!", re);
                    return false;
                }
            default:
                throw new IllegalArgumentException("Invalid rotation.");
        }
    }

    public void waitForIdle(long idleTimeoutMillis, long globalTimeoutMillis) throws TimeoutException {
        long j = idleTimeoutMillis;
        long j2 = globalTimeoutMillis;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            long startTimeMillis = SystemClock.uptimeMillis();
            long j3 = 0;
            if (this.mLastEventTimeMillis <= 0) {
                this.mLastEventTimeMillis = startTimeMillis;
            }
            while (true) {
                long currentTimeMillis = SystemClock.uptimeMillis();
                if (j2 - (currentTimeMillis - startTimeMillis) > j3) {
                    long startTimeMillis2 = startTimeMillis;
                    startTimeMillis = j - (currentTimeMillis - this.mLastEventTimeMillis);
                    if (startTimeMillis <= 0) {
                    } else {
                        try {
                            this.mLock.wait(startTimeMillis);
                        } catch (InterruptedException e) {
                        }
                        j3 = 0;
                        startTimeMillis = startTimeMillis2;
                    }
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No idle state with idle timeout: ");
                    stringBuilder.append(j);
                    stringBuilder.append(" within global timeout: ");
                    stringBuilder.append(j2);
                    throw new TimeoutException(stringBuilder.toString());
                }
            }
        }
    }

    public Bitmap takeScreenshot() {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        Point displaySize = new Point();
        display.getRealSize(displaySize);
        Bitmap screenShot = null;
        try {
            screenShot = this.mUiAutomationConnection.takeScreenshot(new Rect(0, 0, displaySize.x, displaySize.y), display.getRotation());
            if (screenShot == null) {
                return null;
            }
            screenShot.setHasAlpha(false);
            return screenShot;
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error while taking screnshot!", re);
            return null;
        }
    }

    public void setRunAsMonkey(boolean enable) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            ActivityManager.getService().setUserIsMonkey(enable);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error while setting run as monkey!", re);
        }
    }

    public boolean clearWindowContentFrameStats(int windowId) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            return this.mUiAutomationConnection.clearWindowContentFrameStats(windowId);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error clearing window content frame stats!", re);
            return false;
        }
    }

    public WindowContentFrameStats getWindowContentFrameStats(int windowId) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            return this.mUiAutomationConnection.getWindowContentFrameStats(windowId);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error getting window content frame stats!", re);
            return null;
        }
    }

    public void clearWindowAnimationFrameStats() {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            this.mUiAutomationConnection.clearWindowAnimationFrameStats();
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error clearing window animation frame stats!", re);
        }
    }

    public WindowAnimationFrameStats getWindowAnimationFrameStats() {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            return this.mUiAutomationConnection.getWindowAnimationFrameStats();
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error getting window animation frame stats!", re);
            return null;
        }
    }

    public void grantRuntimePermission(String packageName, String permission) {
        grantRuntimePermissionAsUser(packageName, permission, Process.myUserHandle());
    }

    @Deprecated
    public boolean grantRuntimePermission(String packageName, String permission, UserHandle userHandle) {
        grantRuntimePermissionAsUser(packageName, permission, userHandle);
        return true;
    }

    public void grantRuntimePermissionAsUser(String packageName, String permission, UserHandle userHandle) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            this.mUiAutomationConnection.grantRuntimePermission(packageName, permission, userHandle.getIdentifier());
        } catch (Exception e) {
            throw new SecurityException("Error granting runtime permission", e);
        }
    }

    public void revokeRuntimePermission(String packageName, String permission) {
        revokeRuntimePermissionAsUser(packageName, permission, Process.myUserHandle());
    }

    @Deprecated
    public boolean revokeRuntimePermission(String packageName, String permission, UserHandle userHandle) {
        revokeRuntimePermissionAsUser(packageName, permission, userHandle);
        return true;
    }

    public void revokeRuntimePermissionAsUser(String packageName, String permission, UserHandle userHandle) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            this.mUiAutomationConnection.revokeRuntimePermission(packageName, permission, userHandle.getIdentifier());
        } catch (Exception e) {
            throw new SecurityException("Error granting runtime permission", e);
        }
    }

    public ParcelFileDescriptor executeShellCommand(String command) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        warnIfBetterCommand(command);
        ParcelFileDescriptor source = null;
        ParcelFileDescriptor sink = null;
        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            source = pipe[0];
            sink = pipe[1];
            this.mUiAutomationConnection.executeShellCommand(command, sink, null);
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Error executing shell command!", ioe);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error executing shell command!", re);
        } catch (Throwable th) {
            IoUtils.closeQuietly(sink);
        }
        IoUtils.closeQuietly(sink);
        return source;
    }

    public ParcelFileDescriptor[] executeShellCommandRw(String command) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        warnIfBetterCommand(command);
        ParcelFileDescriptor source_read = null;
        ParcelFileDescriptor sink_read = null;
        ParcelFileDescriptor source_write = null;
        ParcelFileDescriptor sink_write = null;
        try {
            ParcelFileDescriptor[] pipe_read = ParcelFileDescriptor.createPipe();
            source_read = pipe_read[0];
            sink_read = pipe_read[1];
            ParcelFileDescriptor[] pipe_write = ParcelFileDescriptor.createPipe();
            source_write = pipe_write[0];
            sink_write = pipe_write[1];
            this.mUiAutomationConnection.executeShellCommand(command, sink_read, source_write);
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Error executing shell command!", ioe);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error executing shell command!", re);
        } catch (Throwable th) {
            IoUtils.closeQuietly(sink_read);
            IoUtils.closeQuietly(source_write);
        }
        IoUtils.closeQuietly(sink_read);
        IoUtils.closeQuietly(source_write);
        return new ParcelFileDescriptor[]{source_read, sink_write};
    }

    private static float getDegreesForRotation(int value) {
        switch (value) {
            case 1:
                return 270.0f;
            case 2:
                return 180.0f;
            case 3:
                return 90.0f;
            default:
                return 0.0f;
        }
    }

    private boolean isConnectedLocked() {
        return this.mConnectionId != -1;
    }

    private void throwIfConnectedLocked() {
        if (this.mConnectionId != -1) {
            throw new IllegalStateException("UiAutomation not connected!");
        }
    }

    private void throwIfNotConnectedLocked() {
        if (!isConnectedLocked()) {
            throw new IllegalStateException("UiAutomation not connected!");
        }
    }

    private void warnIfBetterCommand(String cmd) {
        if (cmd.startsWith("pm grant ")) {
            Log.w(LOG_TAG, "UiAutomation.grantRuntimePermission() is more robust and should be used instead of 'pm grant'");
        } else if (cmd.startsWith("pm revoke ")) {
            Log.w(LOG_TAG, "UiAutomation.revokeRuntimePermission() is more robust and should be used instead of 'pm revoke'");
        }
    }
}
