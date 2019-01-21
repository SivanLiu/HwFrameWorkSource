package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.accessibilityservice.IAccessibilityServiceConnection.Stub;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ParceledListSlice;
import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Slog;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MagnificationSpec;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.DumpUtils;
import com.android.server.accessibility.AccessibilityManagerService.SecurityPolicy;
import com.android.server.accessibility.FingerprintGestureDispatcher.FingerprintGestureClient;
import com.android.server.accessibility.KeyEventDispatcher.KeyEventFilter;
import com.android.server.os.HwBootFail;
import com.android.server.wm.WindowManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

abstract class AbstractAccessibilityServiceConnection extends Stub implements ServiceConnection, DeathRecipient, KeyEventFilter, FingerprintGestureClient {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "AbstractAccessibilityServiceConnection";
    protected final AccessibilityServiceInfo mAccessibilityServiceInfo;
    boolean mCaptureFingerprintGestures;
    final ComponentName mComponentName;
    protected final Context mContext;
    public Handler mEventDispatchHandler;
    int mEventTypes;
    int mFeedbackType;
    int mFetchFlags;
    private final GlobalActionPerformer mGlobalActionPerformer;
    final int mId;
    public final InvocationHandler mInvocationHandler;
    boolean mIsDefault;
    boolean mLastAccessibilityButtonCallbackState;
    protected final Object mLock;
    long mNotificationTimeout;
    final IBinder mOverlayWindowToken = new Binder();
    Set<String> mPackageNames = new HashSet();
    final SparseArray<AccessibilityEvent> mPendingEvents = new SparseArray();
    boolean mReceivedAccessibilityButtonCallbackSinceBind;
    boolean mRequestAccessibilityButton;
    boolean mRequestFilterKeyEvents;
    boolean mRequestTouchExplorationMode;
    boolean mRetrieveInteractiveWindows;
    protected final SecurityPolicy mSecurityPolicy;
    IBinder mService;
    IAccessibilityServiceClient mServiceInterface;
    protected final SystemSupport mSystemSupport;
    boolean mUsesAccessibilityCache = false;
    protected final WindowManagerInternal mWindowManagerService;

    private final class InvocationHandler extends Handler {
        public static final int MSG_CLEAR_ACCESSIBILITY_CACHE = 2;
        private static final int MSG_ON_ACCESSIBILITY_BUTTON_AVAILABILITY_CHANGED = 8;
        private static final int MSG_ON_ACCESSIBILITY_BUTTON_CLICKED = 7;
        public static final int MSG_ON_GESTURE = 1;
        private static final int MSG_ON_MAGNIFICATION_CHANGED = 5;
        private static final int MSG_ON_SOFT_KEYBOARD_STATE_CHANGED = 6;
        private boolean mIsMagnificationCallbackEnabled = false;
        private boolean mIsSoftKeyboardCallbackEnabled = false;

        public InvocationHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message message) {
            int type = message.what;
            switch (type) {
                case 1:
                    AbstractAccessibilityServiceConnection.this.notifyGestureInternal(message.arg1);
                    return;
                case 2:
                    AbstractAccessibilityServiceConnection.this.notifyClearAccessibilityCacheInternal();
                    return;
                case 5:
                    SomeArgs args = message.obj;
                    AbstractAccessibilityServiceConnection.this.notifyMagnificationChangedInternal(args.arg1, ((Float) args.arg2).floatValue(), ((Float) args.arg3).floatValue(), ((Float) args.arg4).floatValue());
                    args.recycle();
                    return;
                case 6:
                    AbstractAccessibilityServiceConnection.this.notifySoftKeyboardShowModeChangedInternal(message.arg1);
                    return;
                case 7:
                    AbstractAccessibilityServiceConnection.this.notifyAccessibilityButtonClickedInternal();
                    return;
                case 8:
                    AbstractAccessibilityServiceConnection.this.notifyAccessibilityButtonAvailabilityChangedInternal(message.arg1 != 0);
                    return;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown message: ");
                    stringBuilder.append(type);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public void notifyMagnificationChangedLocked(Region region, float scale, float centerX, float centerY) {
            if (this.mIsMagnificationCallbackEnabled) {
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = region;
                args.arg2 = Float.valueOf(scale);
                args.arg3 = Float.valueOf(centerX);
                args.arg4 = Float.valueOf(centerY);
                obtainMessage(5, args).sendToTarget();
            }
        }

        public void setMagnificationCallbackEnabled(boolean enabled) {
            this.mIsMagnificationCallbackEnabled = enabled;
        }

        public void notifySoftKeyboardShowModeChangedLocked(int showState) {
            if (this.mIsSoftKeyboardCallbackEnabled) {
                obtainMessage(6, showState, 0).sendToTarget();
            }
        }

        public void setSoftKeyboardCallbackEnabled(boolean enabled) {
            this.mIsSoftKeyboardCallbackEnabled = enabled;
        }

        public void notifyAccessibilityButtonClickedLocked() {
            obtainMessage(7).sendToTarget();
        }

        public void notifyAccessibilityButtonAvailabilityChangedLocked(boolean available) {
            obtainMessage(8, available, null).sendToTarget();
        }
    }

    public interface SystemSupport {
        void ensureWindowsAvailableTimed();

        MagnificationSpec getCompatibleMagnificationSpecLocked(int i);

        RemoteAccessibilityConnection getConnectionLocked(int i);

        int getCurrentUserIdLocked();

        FingerprintGestureDispatcher getFingerprintGestureDispatcher();

        KeyEventDispatcher getKeyEventDispatcher();

        MagnificationController getMagnificationController();

        MotionEventInjector getMotionEventInjectorLocked();

        PendingIntent getPendingIntentActivity(Context context, int i, Intent intent, int i2);

        boolean isAccessibilityButtonShown();

        void onClientChange(boolean z);

        boolean performAccessibilityAction(int i, long j, int i2, Bundle bundle, int i3, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i4, long j2);

        void persistComponentNamesToSettingLocked(String str, Set<ComponentName> set, int i);

        IAccessibilityInteractionConnectionCallback replaceCallbackIfNeeded(IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i, int i2, int i3, long j);
    }

    protected abstract boolean isCalledForCurrentUserLocked();

    public AbstractAccessibilityServiceConnection(Context context, ComponentName componentName, AccessibilityServiceInfo accessibilityServiceInfo, int id, Handler mainHandler, Object lock, SecurityPolicy securityPolicy, SystemSupport systemSupport, WindowManagerInternal windowManagerInternal, GlobalActionPerformer globalActionPerfomer) {
        this.mContext = context;
        this.mWindowManagerService = windowManagerInternal;
        this.mId = id;
        this.mComponentName = componentName;
        this.mAccessibilityServiceInfo = accessibilityServiceInfo;
        this.mLock = lock;
        this.mSecurityPolicy = securityPolicy;
        this.mGlobalActionPerformer = globalActionPerfomer;
        this.mSystemSupport = systemSupport;
        this.mInvocationHandler = new InvocationHandler(mainHandler.getLooper());
        this.mEventDispatchHandler = new Handler(mainHandler.getLooper()) {
            public void handleMessage(Message message) {
                AbstractAccessibilityServiceConnection.this.notifyAccessibilityEventInternal(message.what, message.obj, message.arg1 != 0);
            }
        };
        setDynamicallyConfigurableProperties(accessibilityServiceInfo);
    }

    /* JADX WARNING: Missing block: B:13:0x001f, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onKeyEvent(KeyEvent keyEvent, int sequenceNumber) {
        if (!this.mRequestFilterKeyEvents || this.mServiceInterface == null || (this.mAccessibilityServiceInfo.getCapabilities() & 8) == 0) {
            return false;
        }
        try {
            this.mServiceInterface.onKeyEvent(keyEvent, sequenceNumber);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setDynamicallyConfigurableProperties(AccessibilityServiceInfo info) {
        this.mEventTypes = info.eventTypes;
        this.mFeedbackType = info.feedbackType;
        String[] packageNames = info.packageNames;
        if (packageNames != null) {
            this.mPackageNames.addAll(Arrays.asList(packageNames));
        }
        this.mNotificationTimeout = info.notificationTimeout;
        boolean z = true;
        this.mIsDefault = (info.flags & 1) != 0;
        if (supportsFlagForNotImportantViews(info)) {
            if ((info.flags & 2) != 0) {
                this.mFetchFlags |= 8;
            } else {
                this.mFetchFlags &= -9;
            }
        }
        if ((info.flags & 16) != 0) {
            this.mFetchFlags |= 16;
        } else {
            this.mFetchFlags &= -17;
        }
        this.mRequestTouchExplorationMode = (info.flags & 4) != 0;
        this.mRequestFilterKeyEvents = (info.flags & 32) != 0;
        this.mRetrieveInteractiveWindows = (info.flags & 64) != 0;
        this.mCaptureFingerprintGestures = (info.flags & 512) != 0;
        if ((info.flags & 256) == 0) {
            z = false;
        }
        this.mRequestAccessibilityButton = z;
    }

    protected boolean supportsFlagForNotImportantViews(AccessibilityServiceInfo info) {
        return info.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion >= 16;
    }

    public boolean canReceiveEventsLocked() {
        return (this.mEventTypes == 0 || this.mFeedbackType == 0 || this.mService == null) ? false : true;
    }

    public void setOnKeyEventResult(boolean handled, int sequence) {
        this.mSystemSupport.getKeyEventDispatcher().setOnKeyEventResult(this, handled, sequence);
    }

    public AccessibilityServiceInfo getServiceInfo() {
        AccessibilityServiceInfo accessibilityServiceInfo;
        synchronized (this.mLock) {
            accessibilityServiceInfo = this.mAccessibilityServiceInfo;
        }
        return accessibilityServiceInfo;
    }

    public int getCapabilities() {
        return this.mAccessibilityServiceInfo.getCapabilities();
    }

    int getRelevantEventTypes() {
        return (this.mUsesAccessibilityCache ? 4307005 : 0) | this.mEventTypes;
    }

    public void setServiceInfo(AccessibilityServiceInfo info) {
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                AccessibilityServiceInfo oldInfo = this.mAccessibilityServiceInfo;
                if (oldInfo != null) {
                    oldInfo.updateDynamicallyConfigurableProperties(info);
                    setDynamicallyConfigurableProperties(oldInfo);
                } else {
                    setDynamicallyConfigurableProperties(info);
                }
                this.mSystemSupport.onClientChange(true);
            }
            Binder.restoreCallingIdentity(identity);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public List<AccessibilityWindowInfo> getWindows() {
        this.mSystemSupport.ensureWindowsAvailableTimed();
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return null;
            } else if (!this.mSecurityPolicy.canRetrieveWindowsLocked(this)) {
                return null;
            } else if (this.mSecurityPolicy.mWindows == null) {
                return null;
            } else {
                List<AccessibilityWindowInfo> windows = new ArrayList();
                int windowCount = this.mSecurityPolicy.mWindows.size();
                for (int i = 0; i < windowCount; i++) {
                    AccessibilityWindowInfo windowClone = AccessibilityWindowInfo.obtain((AccessibilityWindowInfo) this.mSecurityPolicy.mWindows.get(i));
                    windowClone.setConnectionId(this.mId);
                    windows.add(windowClone);
                }
                return windows;
            }
        }
    }

    public AccessibilityWindowInfo getWindow(int windowId) {
        this.mSystemSupport.ensureWindowsAvailableTimed();
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return null;
            } else if (this.mSecurityPolicy.canRetrieveWindowsLocked(this)) {
                AccessibilityWindowInfo window = this.mSecurityPolicy.findA11yWindowInfoById(windowId);
                if (window != null) {
                    AccessibilityWindowInfo windowClone = AccessibilityWindowInfo.obtain(window);
                    windowClone.setConnectionId(this.mId);
                    return windowClone;
                }
                return null;
            } else {
                return null;
            }
        }
    }

    /* JADX WARNING: Missing block: B:21:0x0042, code skipped:
            r3 = android.os.Binder.getCallingPid();
            r5 = r1.mSystemSupport.replaceCallbackIfNeeded(r32, r12, r31, r3, r33);
            r6 = android.os.Binder.getCallingUid();
            r7 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            r13.getRemote().findAccessibilityNodeInfosByViewId(r28, r30, r2, r31, r5, r1.mFetchFlags, r3, r33, r25);
            r0 = r1.mSecurityPolicy.computeValidReportedPackages(r6, r13.getPackageName(), r13.getUid());
     */
    /* JADX WARNING: Missing block: B:24:0x0083, code skipped:
            android.os.Binder.restoreCallingIdentity(r7);
     */
    /* JADX WARNING: Missing block: B:25:0x0086, code skipped:
            if (r2 == null) goto L_0x0095;
     */
    /* JADX WARNING: Missing block: B:27:0x0090, code skipped:
            if (android.os.Binder.isProxy(r13.getRemote()) == false) goto L_0x0095;
     */
    /* JADX WARNING: Missing block: B:28:0x0092, code skipped:
            r2.recycle();
     */
    /* JADX WARNING: Missing block: B:29:0x0095, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:30:0x0096, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:31:0x0097, code skipped:
            android.os.Binder.restoreCallingIdentity(r7);
     */
    /* JADX WARNING: Missing block: B:35:0x00a6, code skipped:
            r2.recycle();
     */
    /* JADX WARNING: Missing block: B:36:0x00a9, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:38:0x00ab, code skipped:
            android.os.Binder.restoreCallingIdentity(r7);
     */
    /* JADX WARNING: Missing block: B:42:0x00ba, code skipped:
            r2.recycle();
     */
    /* JADX WARNING: Missing block: B:43:0x00bd, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String[] findAccessibilityNodeInfosByViewId(int accessibilityWindowId, long accessibilityNodeId, String viewIdResName, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        Region partialInteractiveRegion = Region.obtain();
        synchronized (this.mLock) {
            try {
                this.mUsesAccessibilityCache = true;
                if (isCalledForCurrentUserLocked()) {
                    int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                    if (this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId)) {
                        RemoteAccessibilityConnection connection = this.mSystemSupport.getConnectionLocked(resolvedWindowId);
                        if (connection == null) {
                            return null;
                        }
                        if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                            partialInteractiveRegion.recycle();
                            partialInteractiveRegion = null;
                        }
                        MagnificationSpec spec = this.mSystemSupport.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                    } else {
                        return null;
                    }
                }
                return null;
            } catch (Throwable th) {
                while (true) {
                    throw th;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:21:0x0042, code skipped:
            r3 = android.os.Binder.getCallingPid();
            r5 = r1.mSystemSupport.replaceCallbackIfNeeded(r32, r12, r31, r3, r33);
            r6 = android.os.Binder.getCallingUid();
            r7 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            r13.getRemote().findAccessibilityNodeInfosByText(r28, r30, r2, r31, r5, r1.mFetchFlags, r3, r33, r25);
            r0 = r1.mSecurityPolicy.computeValidReportedPackages(r6, r13.getPackageName(), r13.getUid());
     */
    /* JADX WARNING: Missing block: B:24:0x0083, code skipped:
            android.os.Binder.restoreCallingIdentity(r7);
     */
    /* JADX WARNING: Missing block: B:25:0x0086, code skipped:
            if (r2 == null) goto L_0x0095;
     */
    /* JADX WARNING: Missing block: B:27:0x0090, code skipped:
            if (android.os.Binder.isProxy(r13.getRemote()) == false) goto L_0x0095;
     */
    /* JADX WARNING: Missing block: B:28:0x0092, code skipped:
            r2.recycle();
     */
    /* JADX WARNING: Missing block: B:29:0x0095, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:30:0x0096, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:31:0x0097, code skipped:
            android.os.Binder.restoreCallingIdentity(r7);
     */
    /* JADX WARNING: Missing block: B:35:0x00a6, code skipped:
            r2.recycle();
     */
    /* JADX WARNING: Missing block: B:36:0x00a9, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:38:0x00ab, code skipped:
            android.os.Binder.restoreCallingIdentity(r7);
     */
    /* JADX WARNING: Missing block: B:42:0x00ba, code skipped:
            r2.recycle();
     */
    /* JADX WARNING: Missing block: B:43:0x00bd, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String[] findAccessibilityNodeInfosByText(int accessibilityWindowId, long accessibilityNodeId, String text, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        Region partialInteractiveRegion = Region.obtain();
        synchronized (this.mLock) {
            try {
                this.mUsesAccessibilityCache = true;
                if (isCalledForCurrentUserLocked()) {
                    int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                    if (this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId)) {
                        RemoteAccessibilityConnection connection = this.mSystemSupport.getConnectionLocked(resolvedWindowId);
                        if (connection == null) {
                            return null;
                        }
                        if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                            partialInteractiveRegion.recycle();
                            partialInteractiveRegion = null;
                        }
                        MagnificationSpec spec = this.mSystemSupport.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                    } else {
                        return null;
                    }
                }
                return null;
            } catch (Throwable th) {
                while (true) {
                    throw th;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:21:0x0042, code skipped:
            r3 = android.os.Binder.getCallingPid();
            r5 = r1.mSystemSupport.replaceCallbackIfNeeded(r31, r12, r30, r3, r33);
            r6 = android.os.Binder.getCallingUid();
            r7 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            r13.getRemote().findAccessibilityNodeInfoByAccessibilityId(r28, r2, r30, r5, r1.mFetchFlags | r32, r3, r33, r24, r35);
            r0 = r1.mSecurityPolicy.computeValidReportedPackages(r6, r13.getPackageName(), r13.getUid());
     */
    /* JADX WARNING: Missing block: B:24:0x0083, code skipped:
            android.os.Binder.restoreCallingIdentity(r7);
     */
    /* JADX WARNING: Missing block: B:25:0x0086, code skipped:
            if (r2 == null) goto L_0x0095;
     */
    /* JADX WARNING: Missing block: B:27:0x0090, code skipped:
            if (android.os.Binder.isProxy(r13.getRemote()) == false) goto L_0x0095;
     */
    /* JADX WARNING: Missing block: B:28:0x0092, code skipped:
            r2.recycle();
     */
    /* JADX WARNING: Missing block: B:29:0x0095, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:30:0x0096, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:31:0x0097, code skipped:
            android.os.Binder.restoreCallingIdentity(r7);
     */
    /* JADX WARNING: Missing block: B:35:0x00a6, code skipped:
            r2.recycle();
     */
    /* JADX WARNING: Missing block: B:36:0x00a9, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:38:0x00ab, code skipped:
            android.os.Binder.restoreCallingIdentity(r7);
     */
    /* JADX WARNING: Missing block: B:42:0x00ba, code skipped:
            r2.recycle();
     */
    /* JADX WARNING: Missing block: B:43:0x00bd, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String[] findAccessibilityNodeInfoByAccessibilityId(int accessibilityWindowId, long accessibilityNodeId, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, long interrogatingTid, Bundle arguments) throws RemoteException {
        Region partialInteractiveRegion = Region.obtain();
        synchronized (this.mLock) {
            try {
                this.mUsesAccessibilityCache = true;
                if (isCalledForCurrentUserLocked()) {
                    int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                    if (this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId)) {
                        RemoteAccessibilityConnection connection = this.mSystemSupport.getConnectionLocked(resolvedWindowId);
                        if (connection == null) {
                            return null;
                        }
                        if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                            partialInteractiveRegion.recycle();
                            partialInteractiveRegion = null;
                        }
                        MagnificationSpec spec = this.mSystemSupport.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                    } else {
                        return null;
                    }
                }
                return null;
            } catch (Throwable th) {
                while (true) {
                    throw th;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:22:0x0043, code skipped:
            r3 = android.os.Binder.getCallingPid();
            r18 = r1.mSystemSupport.replaceCallbackIfNeeded(r28, r14, r27, r3, r29);
            r12 = android.os.Binder.getCallingUid();
            r10 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:25:0x0064, code skipped:
            r4 = r10;
            r10 = r2;
            r19 = r2;
            r2 = r12;
            r20 = r4;
            r4 = r13;
            r5 = r14;
     */
    /* JADX WARNING: Missing block: B:27:?, code skipped:
            r13.getRemote().findFocus(r24, r15, r10, r27, r18, r1.mFetchFlags, r3, r29, r17);
            r0 = r1.mSecurityPolicy.computeValidReportedPackages(r2, r4.getPackageName(), r4.getUid());
     */
    /* JADX WARNING: Missing block: B:28:0x0089, code skipped:
            android.os.Binder.restoreCallingIdentity(r20);
     */
    /* JADX WARNING: Missing block: B:29:0x008e, code skipped:
            if (r19 == null) goto L_0x00a0;
     */
    /* JADX WARNING: Missing block: B:31:0x0098, code skipped:
            if (android.os.Binder.isProxy(r4.getRemote()) == false) goto L_0x00a0;
     */
    /* JADX WARNING: Missing block: B:32:0x009a, code skipped:
            r19.recycle();
     */
    /* JADX WARNING: Missing block: B:33:0x00a0, code skipped:
            r8 = r19;
     */
    /* JADX WARNING: Missing block: B:34:0x00a2, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:35:0x00a3, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:36:0x00a4, code skipped:
            r8 = r19;
            r6 = r20;
     */
    /* JADX WARNING: Missing block: B:38:0x00aa, code skipped:
            r8 = r19;
            r6 = r20;
     */
    /* JADX WARNING: Missing block: B:39:0x00af, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:40:0x00b0, code skipped:
            r8 = r2;
            r6 = r10;
            r2 = r12;
            r4 = r13;
            r5 = r14;
     */
    /* JADX WARNING: Missing block: B:41:0x00b5, code skipped:
            android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Missing block: B:45:0x00c4, code skipped:
            r8.recycle();
     */
    /* JADX WARNING: Missing block: B:46:0x00c7, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:48:0x00c9, code skipped:
            r8 = r2;
            r6 = r10;
            r2 = r12;
            r4 = r13;
            r5 = r14;
     */
    /* JADX WARNING: Missing block: B:49:0x00ce, code skipped:
            android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Missing block: B:53:0x00dd, code skipped:
            r8.recycle();
     */
    /* JADX WARNING: Missing block: B:55:0x00e1, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String[] findFocus(int accessibilityWindowId, long accessibilityNodeId, int focusType, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        Throwable th;
        Region partialInteractiveRegion = Region.obtain();
        synchronized (this.mLock) {
            try {
                if (isCalledForCurrentUserLocked()) {
                    int i = focusType;
                    int resolvedWindowId = resolveAccessibilityWindowIdForFindFocusLocked(accessibilityWindowId, i);
                    if (this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId)) {
                        RemoteAccessibilityConnection connection = this.mSystemSupport.getConnectionLocked(resolvedWindowId);
                        if (connection == null) {
                            return null;
                        }
                        if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                            partialInteractiveRegion.recycle();
                            partialInteractiveRegion = null;
                        }
                        try {
                            MagnificationSpec spec = this.mSystemSupport.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                        } catch (Throwable th2) {
                            th = th2;
                            Region region = partialInteractiveRegion;
                            throw th;
                        }
                    }
                    return null;
                }
                return null;
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:20:0x003f, code skipped:
            r3 = android.os.Binder.getCallingPid();
            r5 = r1.mSystemSupport.replaceCallbackIfNeeded(r32, r12, r31, r3, r33);
            r6 = android.os.Binder.getCallingUid();
            r7 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            r13.getRemote().focusSearch(r28, r30, r2, r31, r5, r1.mFetchFlags, r3, r33, r25);
            r0 = r1.mSecurityPolicy.computeValidReportedPackages(r6, r13.getPackageName(), r13.getUid());
     */
    /* JADX WARNING: Missing block: B:23:0x0080, code skipped:
            android.os.Binder.restoreCallingIdentity(r7);
     */
    /* JADX WARNING: Missing block: B:24:0x0083, code skipped:
            if (r2 == null) goto L_0x0092;
     */
    /* JADX WARNING: Missing block: B:26:0x008d, code skipped:
            if (android.os.Binder.isProxy(r13.getRemote()) == false) goto L_0x0092;
     */
    /* JADX WARNING: Missing block: B:27:0x008f, code skipped:
            r2.recycle();
     */
    /* JADX WARNING: Missing block: B:28:0x0092, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:29:0x0093, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:30:0x0094, code skipped:
            android.os.Binder.restoreCallingIdentity(r7);
     */
    /* JADX WARNING: Missing block: B:34:0x00a3, code skipped:
            r2.recycle();
     */
    /* JADX WARNING: Missing block: B:35:0x00a6, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:37:0x00a8, code skipped:
            android.os.Binder.restoreCallingIdentity(r7);
     */
    /* JADX WARNING: Missing block: B:41:0x00b7, code skipped:
            r2.recycle();
     */
    /* JADX WARNING: Missing block: B:42:0x00ba, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String[] focusSearch(int accessibilityWindowId, long accessibilityNodeId, int direction, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        Region partialInteractiveRegion = Region.obtain();
        synchronized (this.mLock) {
            try {
                if (isCalledForCurrentUserLocked()) {
                    int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                    if (this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId)) {
                        RemoteAccessibilityConnection connection = this.mSystemSupport.getConnectionLocked(resolvedWindowId);
                        if (connection == null) {
                            return null;
                        }
                        if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                            partialInteractiveRegion.recycle();
                            partialInteractiveRegion = null;
                        }
                        MagnificationSpec spec = this.mSystemSupport.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                    } else {
                        return null;
                    }
                }
                return null;
            } catch (Throwable th) {
                while (true) {
                    throw th;
                }
            }
        }
    }

    public void sendGesture(int sequence, ParceledListSlice gestureSteps) {
    }

    public boolean performAccessibilityAction(int accessibilityWindowId, long accessibilityNodeId, int action, Bundle arguments, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        synchronized (this.mLock) {
            if (isCalledForCurrentUserLocked()) {
                int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                if (this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId)) {
                    return this.mSystemSupport.performAccessibilityAction(resolvedWindowId, accessibilityNodeId, action, arguments, interactionId, callback, this.mFetchFlags, interrogatingTid);
                }
                return false;
            }
            return false;
        }
    }

    public boolean performGlobalAction(int action) {
        synchronized (this.mLock) {
            if (isCalledForCurrentUserLocked()) {
                return this.mGlobalActionPerformer.performGlobalAction(action);
            }
            return false;
        }
    }

    public boolean isFingerprintGestureDetectionAvailable() {
        boolean z = false;
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint") || !isCapturingFingerprintGestures()) {
            return false;
        }
        FingerprintGestureDispatcher dispatcher = this.mSystemSupport.getFingerprintGestureDispatcher();
        if (dispatcher != null && dispatcher.isFingerprintGestureDetectionAvailable()) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            r0 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:11:?, code skipped:
            r2 = r3.mSystemSupport.getMagnificationController().getScale();
     */
    /* JADX WARNING: Missing block: B:14:0x0021, code skipped:
            android.os.Binder.restoreCallingIdentity(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public float getMagnificationScale() {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return 1.0f;
            }
        }
        return r2;
    }

    /* JADX WARNING: Missing block: B:12:0x002b, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Region getMagnificationRegion() {
        synchronized (this.mLock) {
            Region region = Region.obtain();
            if (isCalledForCurrentUserLocked()) {
                MagnificationController magnificationController = this.mSystemSupport.getMagnificationController();
                boolean registeredJustForThisCall = registerMagnificationIfNeeded(magnificationController);
                long identity = Binder.clearCallingIdentity();
                try {
                    magnificationController.getMagnificationRegion(region);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                    if (registeredJustForThisCall) {
                        magnificationController.unregister();
                    }
                }
            } else {
                return region;
            }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x0028, code skipped:
            return r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public float getMagnificationCenterX() {
        synchronized (this.mLock) {
            if (isCalledForCurrentUserLocked()) {
                MagnificationController magnificationController = this.mSystemSupport.getMagnificationController();
                boolean registeredJustForThisCall = registerMagnificationIfNeeded(magnificationController);
                long identity = Binder.clearCallingIdentity();
                try {
                    float centerX = magnificationController.getCenterX();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                    if (registeredJustForThisCall) {
                        magnificationController.unregister();
                    }
                }
            } else {
                return 0.0f;
            }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x0028, code skipped:
            return r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public float getMagnificationCenterY() {
        synchronized (this.mLock) {
            if (isCalledForCurrentUserLocked()) {
                MagnificationController magnificationController = this.mSystemSupport.getMagnificationController();
                boolean registeredJustForThisCall = registerMagnificationIfNeeded(magnificationController);
                long identity = Binder.clearCallingIdentity();
                try {
                    float centerY = magnificationController.getCenterY();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                    if (registeredJustForThisCall) {
                        magnificationController.unregister();
                    }
                }
            } else {
                return 0.0f;
            }
        }
    }

    private boolean registerMagnificationIfNeeded(MagnificationController magnificationController) {
        if (magnificationController.isRegisteredLocked() || !this.mSecurityPolicy.canControlMagnification(this)) {
            return false;
        }
        magnificationController.register();
        return true;
    }

    /* JADX WARNING: Missing block: B:12:0x0017, code skipped:
            r0 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:14:?, code skipped:
            r2 = r3.mSystemSupport.getMagnificationController().reset(r4);
     */
    /* JADX WARNING: Missing block: B:17:0x002a, code skipped:
            android.os.Binder.restoreCallingIdentity(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean resetMagnification(boolean animate) {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return false;
            } else if (!this.mSecurityPolicy.canControlMagnification(this)) {
                return false;
            }
        }
        return r2;
    }

    /* JADX WARNING: Missing block: B:19:0x0038, code skipped:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setMagnificationScaleAndCenter(float scale, float centerX, float centerY, boolean animate) {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return false;
            } else if (this.mSecurityPolicy.canControlMagnification(this)) {
                long identity = Binder.clearCallingIdentity();
                try {
                    MagnificationController magnificationController = this.mSystemSupport.getMagnificationController();
                    if (!magnificationController.isRegisteredLocked()) {
                        magnificationController.register();
                    }
                    boolean scaleAndCenter = magnificationController.setScaleAndCenter(scale, centerX, centerY, animate, this.mId);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            } else {
                return false;
            }
        }
    }

    public void setMagnificationCallbackEnabled(boolean enabled) {
        this.mInvocationHandler.setMagnificationCallbackEnabled(enabled);
    }

    public boolean isMagnificationCallbackEnabled() {
        return this.mInvocationHandler.mIsMagnificationCallbackEnabled;
    }

    public void setSoftKeyboardCallbackEnabled(boolean enabled) {
        this.mInvocationHandler.setSoftKeyboardCallbackEnabled(enabled);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, LOG_TAG, pw)) {
            synchronized (this.mLock) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Service[label=");
                stringBuilder.append(this.mAccessibilityServiceInfo.getResolveInfo().loadLabel(this.mContext.getPackageManager()));
                pw.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", feedbackType");
                stringBuilder.append(AccessibilityServiceInfo.feedbackTypeToString(this.mFeedbackType));
                pw.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", capabilities=");
                stringBuilder.append(this.mAccessibilityServiceInfo.getCapabilities());
                pw.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", eventTypes=");
                stringBuilder.append(AccessibilityEvent.eventTypeToString(this.mEventTypes));
                pw.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", notificationTimeout=");
                stringBuilder.append(this.mNotificationTimeout);
                pw.append(stringBuilder.toString());
                pw.append("]");
            }
        }
    }

    public void onAdded() {
        long identity = Binder.clearCallingIdentity();
        try {
            this.mWindowManagerService.addWindowToken(this.mOverlayWindowToken, 2032, 0);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onRemoved() {
        long identity = Binder.clearCallingIdentity();
        try {
            this.mWindowManagerService.removeWindowToken(this.mOverlayWindowToken, true, 0);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void resetLocked() {
        this.mSystemSupport.getKeyEventDispatcher().flush(this);
        try {
            if (this.mServiceInterface != null) {
                this.mServiceInterface.init(null, this.mId, null);
            }
            if (this.mService != null) {
                this.mService.unlinkToDeath(this, 0);
                this.mService = null;
            }
        } catch (RemoteException | NoSuchElementException e) {
        }
        this.mServiceInterface = null;
        this.mReceivedAccessibilityButtonCallbackSinceBind = false;
    }

    public boolean isConnectedLocked() {
        return this.mService != null;
    }

    public void notifyAccessibilityEvent(AccessibilityEvent event) {
        synchronized (this.mLock) {
            int eventType = event.getEventType();
            boolean serviceWantsEvent = wantsEventLocked(event);
            int i = 0;
            boolean requiredForCacheConsistency = this.mUsesAccessibilityCache && (4307005 & eventType) != 0;
            if (serviceWantsEvent || requiredForCacheConsistency) {
                Message message;
                AccessibilityEvent newEvent = AccessibilityEvent.obtain(event);
                if (this.mNotificationTimeout <= 0 || eventType == 2048) {
                    message = this.mEventDispatchHandler.obtainMessage(eventType, newEvent);
                } else {
                    AccessibilityEvent oldEvent = (AccessibilityEvent) this.mPendingEvents.get(eventType);
                    this.mPendingEvents.put(eventType, newEvent);
                    if (oldEvent != null) {
                        this.mEventDispatchHandler.removeMessages(eventType);
                        oldEvent.recycle();
                    }
                    message = this.mEventDispatchHandler.obtainMessage(eventType);
                }
                if (serviceWantsEvent) {
                    i = 1;
                }
                message.arg1 = i;
                this.mEventDispatchHandler.sendMessageDelayed(message, this.mNotificationTimeout);
                return;
            }
        }
    }

    private boolean wantsEventLocked(AccessibilityEvent event) {
        boolean z = false;
        if (!canReceiveEventsLocked()) {
            return false;
        }
        if (event.getWindowId() != -1 && !event.isImportantForAccessibility() && (this.mFetchFlags & 8) == 0) {
            return false;
        }
        int eventType = event.getEventType();
        if ((this.mEventTypes & eventType) != eventType) {
            return false;
        }
        Set<String> packageNames = this.mPackageNames;
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : null;
        if (packageNames.isEmpty() || packageNames.contains(packageName)) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:20:?, code skipped:
            r1.onAccessibilityEvent(r7, r8);
     */
    /* JADX WARNING: Missing block: B:23:0x003f, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:25:?, code skipped:
            r2 = LOG_TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Error during sending ");
            r3.append(r7);
            r3.append(" to ");
            r3.append(r1);
            android.util.Slog.e(r2, r3.toString(), r0);
     */
    /* JADX WARNING: Missing block: B:27:0x0060, code skipped:
            r7.recycle();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void notifyAccessibilityEventInternal(int eventType, AccessibilityEvent event, boolean serviceWantsEvent) {
        synchronized (this.mLock) {
            IAccessibilityServiceClient listener = this.mServiceInterface;
            if (listener == null) {
                return;
            }
            if (event == null) {
                event = (AccessibilityEvent) this.mPendingEvents.get(eventType);
                if (event == null) {
                    return;
                }
                this.mPendingEvents.remove(eventType);
            }
            if (this.mSecurityPolicy.canRetrieveWindowContentLocked(this)) {
                event.setConnectionId(this.mId);
            } else {
                event.setSource((View) null);
            }
            event.setSealed(true);
        }
        event.recycle();
    }

    public void notifyGesture(int gestureId) {
        this.mInvocationHandler.obtainMessage(1, gestureId, 0).sendToTarget();
    }

    public void notifyClearAccessibilityNodeInfoCache() {
        this.mInvocationHandler.sendEmptyMessage(2);
    }

    public void notifyMagnificationChangedLocked(Region region, float scale, float centerX, float centerY) {
        this.mInvocationHandler.notifyMagnificationChangedLocked(region, scale, centerX, centerY);
    }

    public void notifySoftKeyboardShowModeChangedLocked(int showState) {
        this.mInvocationHandler.notifySoftKeyboardShowModeChangedLocked(showState);
    }

    public void notifyAccessibilityButtonClickedLocked() {
        this.mInvocationHandler.notifyAccessibilityButtonClickedLocked();
    }

    public void notifyAccessibilityButtonAvailabilityChangedLocked(boolean available) {
        this.mInvocationHandler.notifyAccessibilityButtonAvailabilityChangedLocked(available);
    }

    private void notifyMagnificationChangedInternal(Region region, float scale, float centerX, float centerY) {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                listener.onMagnificationChanged(region, scale, centerX, centerY);
            } catch (RemoteException re) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error sending magnification changes to ");
                stringBuilder.append(this.mService);
                Slog.e(str, stringBuilder.toString(), re);
            }
        }
    }

    private void notifySoftKeyboardShowModeChangedInternal(int showState) {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                listener.onSoftKeyboardShowModeChanged(showState);
            } catch (RemoteException re) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error sending soft keyboard show mode changes to ");
                stringBuilder.append(this.mService);
                Slog.e(str, stringBuilder.toString(), re);
            }
        }
    }

    private void notifyAccessibilityButtonClickedInternal() {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                listener.onAccessibilityButtonClicked();
            } catch (RemoteException re) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error sending accessibility button click to ");
                stringBuilder.append(this.mService);
                Slog.e(str, stringBuilder.toString(), re);
            }
        }
    }

    private void notifyAccessibilityButtonAvailabilityChangedInternal(boolean available) {
        if (!this.mReceivedAccessibilityButtonCallbackSinceBind || this.mLastAccessibilityButtonCallbackState != available) {
            this.mReceivedAccessibilityButtonCallbackSinceBind = true;
            this.mLastAccessibilityButtonCallbackState = available;
            IAccessibilityServiceClient listener = getServiceInterfaceSafely();
            if (listener != null) {
                try {
                    listener.onAccessibilityButtonAvailabilityChanged(available);
                } catch (RemoteException re) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error sending accessibility button availability change to ");
                    stringBuilder.append(this.mService);
                    Slog.e(str, stringBuilder.toString(), re);
                }
            }
        }
    }

    private void notifyGestureInternal(int gestureId) {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                listener.onGesture(gestureId);
            } catch (RemoteException re) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error during sending gesture ");
                stringBuilder.append(gestureId);
                stringBuilder.append(" to ");
                stringBuilder.append(this.mService);
                Slog.e(str, stringBuilder.toString(), re);
            }
        }
    }

    private void notifyClearAccessibilityCacheInternal() {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                listener.clearAccessibilityCache();
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error during requesting accessibility info cache to be cleared.", re);
            }
        }
    }

    private IAccessibilityServiceClient getServiceInterfaceSafely() {
        IAccessibilityServiceClient iAccessibilityServiceClient;
        synchronized (this.mLock) {
            iAccessibilityServiceClient = this.mServiceInterface;
        }
        return iAccessibilityServiceClient;
    }

    private int resolveAccessibilityWindowIdLocked(int accessibilityWindowId) {
        if (accessibilityWindowId == HwBootFail.STAGE_BOOT_SUCCESS) {
            return this.mSecurityPolicy.getActiveWindowId();
        }
        return accessibilityWindowId;
    }

    private int resolveAccessibilityWindowIdForFindFocusLocked(int windowId, int focusType) {
        if (windowId == HwBootFail.STAGE_BOOT_SUCCESS) {
            return this.mSecurityPolicy.mActiveWindowId;
        }
        if (windowId == -2) {
            if (focusType == 1) {
                return this.mSecurityPolicy.mFocusedWindowId;
            }
            if (focusType == 2) {
                return this.mSecurityPolicy.mAccessibilityFocusedWindowId;
            }
        }
        return windowId;
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }
}
