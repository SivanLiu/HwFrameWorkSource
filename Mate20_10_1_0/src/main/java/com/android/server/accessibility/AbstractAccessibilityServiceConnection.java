package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.accessibilityservice.IAccessibilityServiceConnection;
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
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Slog;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MagnificationSpec;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.DumpUtils;
import com.android.server.accessibility.AccessibilityManagerService;
import com.android.server.accessibility.FingerprintGestureDispatcher;
import com.android.server.accessibility.KeyEventDispatcher;
import com.android.server.wm.WindowManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

abstract class AbstractAccessibilityServiceConnection extends IAccessibilityServiceConnection.Stub implements ServiceConnection, IBinder.DeathRecipient, KeyEventDispatcher.KeyEventFilter, FingerprintGestureDispatcher.FingerprintGestureClient {
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
    final SparseArray<AccessibilityEvent> mPendingEvents = new SparseArray<>();
    boolean mReceivedAccessibilityButtonCallbackSinceBind;
    boolean mRequestAccessibilityButton;
    boolean mRequestFilterKeyEvents;
    boolean mRequestTouchExplorationMode;
    boolean mRetrieveInteractiveWindows;
    protected final AccessibilityManagerService.SecurityPolicy mSecurityPolicy;
    IBinder mService;
    IAccessibilityServiceClient mServiceInterface;
    protected final SystemSupport mSystemSupport;
    boolean mUsesAccessibilityCache = false;
    protected final WindowManagerInternal mWindowManagerService;

    public interface SystemSupport {
        void ensureWindowsAvailableTimed();

        MagnificationSpec getCompatibleMagnificationSpecLocked(int i);

        AccessibilityManagerService.RemoteAccessibilityConnection getConnectionLocked(int i);

        int getCurrentUserIdLocked();

        FingerprintGestureDispatcher getFingerprintGestureDispatcher();

        KeyEventDispatcher getKeyEventDispatcher();

        MagnificationController getMagnificationController();

        MotionEventInjector getMotionEventInjectorLocked();

        PendingIntent getPendingIntentActivity(Context context, int i, Intent intent, int i2);

        boolean isAccessibilityButtonShown();

        void onClientChangeLocked(boolean z);

        boolean performAccessibilityAction(int i, long j, int i2, Bundle bundle, int i3, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i4, long j2);

        void persistComponentNamesToSettingLocked(String str, Set<ComponentName> set, int i);

        IAccessibilityInteractionConnectionCallback replaceCallbackIfNeeded(IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i, int i2, int i3, long j);
    }

    /* access modifiers changed from: protected */
    public abstract boolean isCalledForCurrentUserLocked();

    public AbstractAccessibilityServiceConnection(Context context, ComponentName componentName, AccessibilityServiceInfo accessibilityServiceInfo, int id, Handler mainHandler, Object lock, AccessibilityManagerService.SecurityPolicy securityPolicy, SystemSupport systemSupport, WindowManagerInternal windowManagerInternal, GlobalActionPerformer globalActionPerfomer) {
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
            /* class com.android.server.accessibility.AbstractAccessibilityServiceConnection.AnonymousClass1 */

            public void handleMessage(Message message) {
                AbstractAccessibilityServiceConnection.this.notifyAccessibilityEventInternal(message.what, (AccessibilityEvent) message.obj, message.arg1 != 0);
            }
        };
        setDynamicallyConfigurableProperties(accessibilityServiceInfo);
    }

    @Override // com.android.server.accessibility.KeyEventDispatcher.KeyEventFilter
    public boolean onKeyEvent(KeyEvent keyEvent, int sequenceNumber) {
        if (!this.mRequestFilterKeyEvents || this.mServiceInterface == null || (this.mAccessibilityServiceInfo.getCapabilities() & 8) == 0 || !this.mSecurityPolicy.checkAccessibilityAccess(this)) {
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

    /* access modifiers changed from: protected */
    public boolean supportsFlagForNotImportantViews(AccessibilityServiceInfo info) {
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

    /* access modifiers changed from: package-private */
    public int getRelevantEventTypes() {
        int i;
        if (this.mUsesAccessibilityCache) {
            i = 4307005;
        } else {
            i = 32;
        }
        return i | this.mEventTypes;
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
                this.mSystemSupport.onClientChangeLocked(true);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public List<AccessibilityWindowInfo> getWindows() {
        this.mSystemSupport.ensureWindowsAvailableTimed();
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return null;
            }
            if (!this.mSecurityPolicy.canRetrieveWindowsLocked(this)) {
                return null;
            }
            if (this.mSecurityPolicy.mWindows == null) {
                return null;
            }
            if (!this.mSecurityPolicy.checkAccessibilityAccess(this)) {
                return null;
            }
            List<AccessibilityWindowInfo> windows = new ArrayList<>();
            int windowCount = this.mSecurityPolicy.mWindows.size();
            for (int i = 0; i < windowCount; i++) {
                AccessibilityWindowInfo windowClone = AccessibilityWindowInfo.obtain(this.mSecurityPolicy.mWindows.get(i));
                windowClone.setConnectionId(this.mId);
                windows.add(windowClone);
            }
            return windows;
        }
    }

    public AccessibilityWindowInfo getWindow(int windowId) {
        this.mSystemSupport.ensureWindowsAvailableTimed();
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return null;
            }
            if (!this.mSecurityPolicy.canRetrieveWindowsLocked(this)) {
                return null;
            }
            if (!this.mSecurityPolicy.checkAccessibilityAccess(this)) {
                return null;
            }
            AccessibilityWindowInfo window = this.mSecurityPolicy.findA11yWindowInfoById(windowId);
            if (window == null) {
                return null;
            }
            AccessibilityWindowInfo windowClone = AccessibilityWindowInfo.obtain(window);
            windowClone.setConnectionId(this.mId);
            return windowClone;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0048, code lost:
        if (r26.mSecurityPolicy.checkAccessibilityAccess(r26) != false) goto L_0x004b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x004a, code lost:
        return null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x004b, code lost:
        r3 = android.os.Binder.getCallingPid();
        r5 = r26.mSystemSupport.replaceCallbackIfNeeded(r32, r0, r31, r3, r33);
        r6 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:?, code lost:
        r5.getRemote().findAccessibilityNodeInfosByViewId(r28, r30, r2, r31, r5, r26.mFetchFlags, r3, r33, r25);
        r0 = r26.mSecurityPolicy.computeValidReportedPackages(r5.getPackageName(), r5.getUid());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0088, code lost:
        android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x008b, code lost:
        if (r2 == null) goto L_0x009a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0095, code lost:
        if (android.os.Binder.isProxy(r5.getRemote()) == false) goto L_0x009a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0097, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x009a, code lost:
        return r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x009b, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x009c, code lost:
        android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x00ab, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00ae, code lost:
        throw r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00b0, code lost:
        android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00bf, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00c2, code lost:
        return null;
     */
    public String[] findAccessibilityNodeInfosByViewId(int accessibilityWindowId, long accessibilityNodeId, String viewIdResName, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        Region partialInteractiveRegion = Region.obtain();
        synchronized (this.mLock) {
            this.mUsesAccessibilityCache = true;
            if (!isCalledForCurrentUserLocked()) {
                return null;
            }
            int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
            if (!this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId)) {
                return null;
            }
            AccessibilityManagerService.RemoteAccessibilityConnection connection = this.mSystemSupport.getConnectionLocked(resolvedWindowId);
            if (connection == null) {
                return null;
            }
            if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                partialInteractiveRegion.recycle();
                partialInteractiveRegion = null;
            }
            MagnificationSpec spec = this.mSystemSupport.getCompatibleMagnificationSpecLocked(resolvedWindowId);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0048, code lost:
        if (r26.mSecurityPolicy.checkAccessibilityAccess(r26) != false) goto L_0x004b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x004a, code lost:
        return null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x004b, code lost:
        r3 = android.os.Binder.getCallingPid();
        r5 = r26.mSystemSupport.replaceCallbackIfNeeded(r32, r0, r31, r3, r33);
        r6 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:?, code lost:
        r5.getRemote().findAccessibilityNodeInfosByText(r28, r30, r2, r31, r5, r26.mFetchFlags, r3, r33, r25);
        r0 = r26.mSecurityPolicy.computeValidReportedPackages(r5.getPackageName(), r5.getUid());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0088, code lost:
        android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x008b, code lost:
        if (r2 == null) goto L_0x009a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0095, code lost:
        if (android.os.Binder.isProxy(r5.getRemote()) == false) goto L_0x009a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0097, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x009a, code lost:
        return r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x009b, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x009c, code lost:
        android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x00ab, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00ae, code lost:
        throw r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00b0, code lost:
        android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00bf, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00c2, code lost:
        return null;
     */
    public String[] findAccessibilityNodeInfosByText(int accessibilityWindowId, long accessibilityNodeId, String text, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        Region partialInteractiveRegion = Region.obtain();
        synchronized (this.mLock) {
            this.mUsesAccessibilityCache = true;
            if (!isCalledForCurrentUserLocked()) {
                return null;
            }
            int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
            if (!this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId)) {
                return null;
            }
            AccessibilityManagerService.RemoteAccessibilityConnection connection = this.mSystemSupport.getConnectionLocked(resolvedWindowId);
            if (connection == null) {
                return null;
            }
            if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                partialInteractiveRegion.recycle();
                partialInteractiveRegion = null;
            }
            MagnificationSpec spec = this.mSystemSupport.getCompatibleMagnificationSpecLocked(resolvedWindowId);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0048, code lost:
        if (r26.mSecurityPolicy.checkAccessibilityAccess(r26) != false) goto L_0x004b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x004a, code lost:
        return null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x004b, code lost:
        r3 = android.os.Binder.getCallingPid();
        r5 = r26.mSystemSupport.replaceCallbackIfNeeded(r31, r0, r30, r3, r33);
        r6 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:?, code lost:
        r5.getRemote().findAccessibilityNodeInfoByAccessibilityId(r28, r2, r30, r5, r26.mFetchFlags | r32, r3, r33, r24, r35);
        r0 = r26.mSecurityPolicy.computeValidReportedPackages(r5.getPackageName(), r5.getUid());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0088, code lost:
        android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x008b, code lost:
        if (r2 == null) goto L_0x009a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0095, code lost:
        if (android.os.Binder.isProxy(r5.getRemote()) == false) goto L_0x009a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0097, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x009a, code lost:
        return r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x009b, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x009c, code lost:
        android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x00ab, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00ae, code lost:
        throw r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00b0, code lost:
        android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00bf, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00c2, code lost:
        return null;
     */
    public String[] findAccessibilityNodeInfoByAccessibilityId(int accessibilityWindowId, long accessibilityNodeId, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, long interrogatingTid, Bundle arguments) throws RemoteException {
        Region partialInteractiveRegion = Region.obtain();
        synchronized (this.mLock) {
            this.mUsesAccessibilityCache = true;
            if (!isCalledForCurrentUserLocked()) {
                return null;
            }
            int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
            if (!this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId)) {
                return null;
            }
            AccessibilityManagerService.RemoteAccessibilityConnection connection = this.mSystemSupport.getConnectionLocked(resolvedWindowId);
            if (connection == null) {
                return null;
            }
            if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                partialInteractiveRegion.recycle();
                partialInteractiveRegion = null;
            }
            MagnificationSpec spec = this.mSystemSupport.getCompatibleMagnificationSpecLocked(resolvedWindowId);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:23:0x004a, code lost:
        if (r23.mSecurityPolicy.checkAccessibilityAccess(r23) != false) goto L_0x004d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x004c, code lost:
        return null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x004d, code lost:
        r3 = android.os.Binder.getCallingPid();
        r19 = r23.mSystemSupport.replaceCallbackIfNeeded(r29, r0, r28, r3, r30);
        r20 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:?, code lost:
        r6.getRemote().findFocus(r25, r27, r2, r28, r19, r23.mFetchFlags, r3, r30, r17);
        r0 = r23.mSecurityPolicy.computeValidReportedPackages(r6.getPackageName(), r6.getUid());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0088, code lost:
        android.os.Binder.restoreCallingIdentity(r20);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x008b, code lost:
        if (r2 == null) goto L_0x009a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x0095, code lost:
        if (android.os.Binder.isProxy(r6.getRemote()) == false) goto L_0x009a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x0097, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x009a, code lost:
        return r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x009b, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x009f, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00a2, code lost:
        android.os.Binder.restoreCallingIdentity(r20);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00b1, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00b4, code lost:
        throw r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:49:0x00b8, code lost:
        android.os.Binder.restoreCallingIdentity(r20);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x00c7, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x00ca, code lost:
        return null;
     */
    public String[] findFocus(int accessibilityWindowId, long accessibilityNodeId, int focusType, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        Region partialInteractiveRegion = Region.obtain();
        synchronized (this.mLock) {
            try {
                if (!isCalledForCurrentUserLocked()) {
                    return null;
                }
                try {
                    int resolvedWindowId = resolveAccessibilityWindowIdForFindFocusLocked(accessibilityWindowId, focusType);
                    if (!this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId)) {
                        return null;
                    }
                    AccessibilityManagerService.RemoteAccessibilityConnection connection = this.mSystemSupport.getConnectionLocked(resolvedWindowId);
                    if (connection == null) {
                        return null;
                    }
                    if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                        partialInteractiveRegion.recycle();
                        partialInteractiveRegion = null;
                    }
                    MagnificationSpec spec = this.mSystemSupport.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0045, code lost:
        if (r26.mSecurityPolicy.checkAccessibilityAccess(r26) != false) goto L_0x0048;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0047, code lost:
        return null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0048, code lost:
        r3 = android.os.Binder.getCallingPid();
        r5 = r26.mSystemSupport.replaceCallbackIfNeeded(r32, r0, r31, r3, r33);
        r6 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:?, code lost:
        r5.getRemote().focusSearch(r28, r30, r2, r31, r5, r26.mFetchFlags, r3, r33, r25);
        r0 = r26.mSecurityPolicy.computeValidReportedPackages(r5.getPackageName(), r5.getUid());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0085, code lost:
        android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0088, code lost:
        if (r2 == null) goto L_0x0097;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0092, code lost:
        if (android.os.Binder.isProxy(r5.getRemote()) == false) goto L_0x0097;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0094, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0097, code lost:
        return r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0098, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x0099, code lost:
        android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x00a8, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x00ab, code lost:
        throw r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x00ad, code lost:
        android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x00bc, code lost:
        r2.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00bf, code lost:
        return null;
     */
    public String[] focusSearch(int accessibilityWindowId, long accessibilityNodeId, int direction, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        Region partialInteractiveRegion = Region.obtain();
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return null;
            }
            int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
            if (!this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId)) {
                return null;
            }
            AccessibilityManagerService.RemoteAccessibilityConnection connection = this.mSystemSupport.getConnectionLocked(resolvedWindowId);
            if (connection == null) {
                return null;
            }
            if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                partialInteractiveRegion.recycle();
                partialInteractiveRegion = null;
            }
            MagnificationSpec spec = this.mSystemSupport.getCompatibleMagnificationSpecLocked(resolvedWindowId);
        }
    }

    public void sendGesture(int sequence, ParceledListSlice gestureSteps) {
    }

    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0024, code lost:
        if (r16.mSecurityPolicy.checkAccessibilityAccess(r16) != false) goto L_0x0027;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0026, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x003c, code lost:
        return r16.mSystemSupport.performAccessibilityAction(r0, r18, r20, r21, r22, r23, r16.mFetchFlags, r24);
     */
    public boolean performAccessibilityAction(int accessibilityWindowId, long accessibilityNodeId, int action, Bundle arguments, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return false;
            }
            int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
            if (!this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId)) {
                return false;
            }
        }
    }

    public boolean performGlobalAction(int action) {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return false;
            }
            return this.mGlobalActionPerformer.performGlobalAction(action);
        }
    }

    public boolean isFingerprintGestureDetectionAvailable() {
        FingerprintGestureDispatcher dispatcher;
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint") && isCapturingFingerprintGestures() && (dispatcher = this.mSystemSupport.getFingerprintGestureDispatcher()) != null && dispatcher.isFingerprintGestureDetectionAvailable()) {
            return true;
        }
        return false;
    }

    public float getMagnificationScale(int displayId) {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return 1.0f;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                return this.mSystemSupport.getMagnificationController().getScale(displayId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public Region getMagnificationRegion(int displayId) {
        synchronized (this.mLock) {
            Region region = Region.obtain();
            if (!isCalledForCurrentUserLocked()) {
                return region;
            }
            MagnificationController magnificationController = this.mSystemSupport.getMagnificationController();
            boolean registeredJustForThisCall = registerMagnificationIfNeeded(displayId, magnificationController);
            long identity = Binder.clearCallingIdentity();
            try {
                magnificationController.getMagnificationRegion(displayId, region);
                return region;
            } finally {
                Binder.restoreCallingIdentity(identity);
                if (registeredJustForThisCall) {
                    magnificationController.unregister(displayId);
                }
            }
        }
    }

    public float getMagnificationCenterX(int displayId) {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return 0.0f;
            }
            MagnificationController magnificationController = this.mSystemSupport.getMagnificationController();
            boolean registeredJustForThisCall = registerMagnificationIfNeeded(displayId, magnificationController);
            long identity = Binder.clearCallingIdentity();
            try {
                return magnificationController.getCenterX(displayId);
            } finally {
                Binder.restoreCallingIdentity(identity);
                if (registeredJustForThisCall) {
                    magnificationController.unregister(displayId);
                }
            }
        }
    }

    public float getMagnificationCenterY(int displayId) {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return 0.0f;
            }
            MagnificationController magnificationController = this.mSystemSupport.getMagnificationController();
            boolean registeredJustForThisCall = registerMagnificationIfNeeded(displayId, magnificationController);
            long identity = Binder.clearCallingIdentity();
            try {
                return magnificationController.getCenterY(displayId);
            } finally {
                Binder.restoreCallingIdentity(identity);
                if (registeredJustForThisCall) {
                    magnificationController.unregister(displayId);
                }
            }
        }
    }

    private boolean registerMagnificationIfNeeded(int displayId, MagnificationController magnificationController) {
        if (magnificationController.isRegistered(displayId) || !this.mSecurityPolicy.canControlMagnification(this)) {
            return false;
        }
        magnificationController.register(displayId);
        return true;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0017, code lost:
        r0 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:?, code lost:
        r3 = r5.mSystemSupport.getMagnificationController();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0025, code lost:
        if (r3.reset(r6, r7) != false) goto L_0x002d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x002b, code lost:
        if (r3.isMagnifying(r6) != false) goto L_0x002e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x002d, code lost:
        r2 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0031, code lost:
        return r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0032, code lost:
        r2 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0033, code lost:
        android.os.Binder.restoreCallingIdentity(r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0036, code lost:
        throw r2;
     */
    public boolean resetMagnification(int displayId, boolean animate) {
        synchronized (this.mLock) {
            boolean z = false;
            if (!isCalledForCurrentUserLocked()) {
                return false;
            }
            if (!this.mSecurityPolicy.canControlMagnification(this)) {
                return false;
            }
        }
    }

    public boolean setMagnificationScaleAndCenter(int displayId, float scale, float centerX, float centerY, boolean animate) {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return false;
            }
            if (!this.mSecurityPolicy.canControlMagnification(this)) {
                return false;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                MagnificationController magnificationController = this.mSystemSupport.getMagnificationController();
                if (!magnificationController.isRegistered(displayId)) {
                    magnificationController.register(displayId);
                }
                return magnificationController.setScaleAndCenter(displayId, scale, centerX, centerY, animate, this.mId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void setMagnificationCallbackEnabled(int displayId, boolean enabled) {
        this.mInvocationHandler.setMagnificationCallbackEnabled(displayId, enabled);
    }

    public boolean isMagnificationCallbackEnabled(int displayId) {
        return this.mInvocationHandler.isMagnificationCallbackEnabled(displayId);
    }

    public void setSoftKeyboardCallbackEnabled(boolean enabled) {
        this.mInvocationHandler.setSoftKeyboardCallbackEnabled(enabled);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, LOG_TAG, pw)) {
            synchronized (this.mLock) {
                pw.append((CharSequence) ("Service[label=" + ((Object) this.mAccessibilityServiceInfo.getResolveInfo().loadLabel(this.mContext.getPackageManager()))));
                pw.append((CharSequence) (", feedbackType" + AccessibilityServiceInfo.feedbackTypeToString(this.mFeedbackType)));
                pw.append((CharSequence) (", capabilities=" + this.mAccessibilityServiceInfo.getCapabilities()));
                pw.append((CharSequence) (", eventTypes=" + AccessibilityEvent.eventTypeToString(this.mEventTypes)));
                pw.append((CharSequence) (", notificationTimeout=" + this.mNotificationTimeout));
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
                this.mServiceInterface.init((IAccessibilityServiceConnection) null, this.mId, (IBinder) null);
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
        Message message;
        synchronized (this.mLock) {
            int eventType = event.getEventType();
            boolean serviceWantsEvent = wantsEventLocked(event);
            int i = 1;
            boolean requiredForCacheConsistency = this.mUsesAccessibilityCache && (4307005 & eventType) != 0;
            if (!serviceWantsEvent && !requiredForCacheConsistency) {
                return;
            }
            if (this.mSecurityPolicy.checkAccessibilityAccess(this)) {
                AccessibilityEvent newEvent = AccessibilityEvent.obtain(event);
                if (this.mNotificationTimeout <= 0 || eventType == 2048) {
                    message = this.mEventDispatchHandler.obtainMessage(eventType, newEvent);
                } else {
                    AccessibilityEvent oldEvent = this.mPendingEvents.get(eventType);
                    this.mPendingEvents.put(eventType, newEvent);
                    if (oldEvent != null) {
                        this.mEventDispatchHandler.removeMessages(eventType);
                        oldEvent.recycle();
                    }
                    message = this.mEventDispatchHandler.obtainMessage(eventType);
                }
                if (!serviceWantsEvent) {
                    i = 0;
                }
                message.arg1 = i;
                this.mEventDispatchHandler.sendMessageDelayed(message, this.mNotificationTimeout);
            }
        }
    }

    private boolean wantsEventLocked(AccessibilityEvent event) {
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
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:20:?, code lost:
        r1.onAccessibilityEvent(r7, r8);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x003e, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0040, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:?, code lost:
        android.util.Slog.e(com.android.server.accessibility.AbstractAccessibilityServiceConnection.LOG_TAG, "Error during sending " + r7 + " to " + r1, r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0062, code lost:
        r7.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0065, code lost:
        throw r0;
     */
    public void notifyAccessibilityEventInternal(int eventType, AccessibilityEvent event, boolean serviceWantsEvent) {
        synchronized (this.mLock) {
            IAccessibilityServiceClient listener = this.mServiceInterface;
            if (listener != null) {
                if (event == null) {
                    event = this.mPendingEvents.get(eventType);
                    if (event != null) {
                        this.mPendingEvents.remove(eventType);
                    } else {
                        return;
                    }
                }
                if (this.mSecurityPolicy.canRetrieveWindowContentLocked(this)) {
                    event.setConnectionId(this.mId);
                } else {
                    event.setSource(null);
                }
                event.setSealed(true);
            } else {
                return;
            }
        }
        event.recycle();
    }

    public void notifyGesture(int gestureId) {
        this.mInvocationHandler.obtainMessage(1, gestureId, 0).sendToTarget();
    }

    public void notifyClearAccessibilityNodeInfoCache() {
        this.mInvocationHandler.sendEmptyMessage(2);
    }

    public void notifyMagnificationChangedLocked(int displayId, Region region, float scale, float centerX, float centerY) {
        this.mInvocationHandler.notifyMagnificationChangedLocked(displayId, region, scale, centerX, centerY);
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

    /* access modifiers changed from: private */
    public void notifyMagnificationChangedInternal(int displayId, Region region, float scale, float centerX, float centerY) {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                listener.onMagnificationChanged(displayId, region, scale, centerX, centerY);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error sending magnification changes to " + this.mService, re);
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifySoftKeyboardShowModeChangedInternal(int showState) {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                listener.onSoftKeyboardShowModeChanged(showState);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error sending soft keyboard show mode changes to " + this.mService, re);
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyAccessibilityButtonClickedInternal() {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                listener.onAccessibilityButtonClicked();
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error sending accessibility button click to " + this.mService, re);
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyAccessibilityButtonAvailabilityChangedInternal(boolean available) {
        if (!this.mReceivedAccessibilityButtonCallbackSinceBind || this.mLastAccessibilityButtonCallbackState != available) {
            this.mReceivedAccessibilityButtonCallbackSinceBind = true;
            this.mLastAccessibilityButtonCallbackState = available;
            IAccessibilityServiceClient listener = getServiceInterfaceSafely();
            if (listener != null) {
                try {
                    listener.onAccessibilityButtonAvailabilityChanged(available);
                } catch (RemoteException re) {
                    Slog.e(LOG_TAG, "Error sending accessibility button availability change to " + this.mService, re);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyGestureInternal(int gestureId) {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                listener.onGesture(gestureId);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error during sending gesture " + gestureId + " to " + this.mService, re);
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyClearAccessibilityCacheInternal() {
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
        if (accessibilityWindowId == Integer.MAX_VALUE) {
            return this.mSecurityPolicy.getActiveWindowId();
        }
        return accessibilityWindowId;
    }

    private int resolveAccessibilityWindowIdForFindFocusLocked(int windowId, int focusType) {
        if (windowId == Integer.MAX_VALUE) {
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

    private final class InvocationHandler extends Handler {
        public static final int MSG_CLEAR_ACCESSIBILITY_CACHE = 2;
        private static final int MSG_ON_ACCESSIBILITY_BUTTON_AVAILABILITY_CHANGED = 8;
        private static final int MSG_ON_ACCESSIBILITY_BUTTON_CLICKED = 7;
        public static final int MSG_ON_GESTURE = 1;
        private static final int MSG_ON_MAGNIFICATION_CHANGED = 5;
        private static final int MSG_ON_SOFT_KEYBOARD_STATE_CHANGED = 6;
        private boolean mIsSoftKeyboardCallbackEnabled = false;
        @GuardedBy({"mlock"})
        private final SparseArray<Boolean> mMagnificationCallbackState = new SparseArray<>(0);

        public InvocationHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message message) {
            int type = message.what;
            boolean available = true;
            if (type == 1) {
                AbstractAccessibilityServiceConnection.this.notifyGestureInternal(message.arg1);
            } else if (type == 2) {
                AbstractAccessibilityServiceConnection.this.notifyClearAccessibilityCacheInternal();
            } else if (type == 5) {
                SomeArgs args = (SomeArgs) message.obj;
                float scale = ((Float) args.arg2).floatValue();
                float centerX = ((Float) args.arg3).floatValue();
                float centerY = ((Float) args.arg4).floatValue();
                int displayId = args.argi1;
                AbstractAccessibilityServiceConnection.this.notifyMagnificationChangedInternal(displayId, (Region) args.arg1, scale, centerX, centerY);
                args.recycle();
            } else if (type == 6) {
                AbstractAccessibilityServiceConnection.this.notifySoftKeyboardShowModeChangedInternal(message.arg1);
            } else if (type == 7) {
                AbstractAccessibilityServiceConnection.this.notifyAccessibilityButtonClickedInternal();
            } else if (type == 8) {
                if (message.arg1 == 0) {
                    available = false;
                }
                AbstractAccessibilityServiceConnection.this.notifyAccessibilityButtonAvailabilityChangedInternal(available);
            } else {
                throw new IllegalArgumentException("Unknown message: " + type);
            }
        }

        public void notifyMagnificationChangedLocked(int displayId, Region region, float scale, float centerX, float centerY) {
            synchronized (AbstractAccessibilityServiceConnection.this.mLock) {
                if (this.mMagnificationCallbackState.get(displayId) != null) {
                    SomeArgs args = SomeArgs.obtain();
                    args.arg1 = region;
                    args.arg2 = Float.valueOf(scale);
                    args.arg3 = Float.valueOf(centerX);
                    args.arg4 = Float.valueOf(centerY);
                    args.argi1 = displayId;
                    obtainMessage(5, args).sendToTarget();
                }
            }
        }

        public void setMagnificationCallbackEnabled(int displayId, boolean enabled) {
            synchronized (AbstractAccessibilityServiceConnection.this.mLock) {
                if (enabled) {
                    this.mMagnificationCallbackState.put(displayId, true);
                } else {
                    this.mMagnificationCallbackState.remove(displayId);
                }
            }
        }

        public boolean isMagnificationCallbackEnabled(int displayId) {
            boolean z;
            synchronized (AbstractAccessibilityServiceConnection.this.mLock) {
                z = this.mMagnificationCallbackState.get(displayId) != null;
            }
            return z;
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
            obtainMessage(8, available ? 1 : 0, 0).sendToTarget();
        }
    }
}
