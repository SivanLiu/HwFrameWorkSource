package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Slog;
import android.view.accessibility.AccessibilityEvent;
import com.android.internal.util.DumpUtils;
import com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport;
import com.android.server.accessibility.AccessibilityManagerService.SecurityPolicy;
import com.android.server.wm.WindowManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;

class UiAutomationManager {
    private static final ComponentName COMPONENT_NAME = new ComponentName("com.android.server.accessibility", "UiAutomation");
    private static final String LOG_TAG = "UiAutomationManager";
    private SystemSupport mSystemSupport;
    private int mUiAutomationFlags;
    private UiAutomationService mUiAutomationService;
    private AccessibilityServiceInfo mUiAutomationServiceInfo;
    private IBinder mUiAutomationServiceOwner;
    private final DeathRecipient mUiAutomationServiceOwnerDeathRecipient = new DeathRecipient() {
        public void binderDied() {
            try {
                UiAutomationManager.this.mUiAutomationServiceOwner.unlinkToDeath(this, 0);
            } catch (Exception e) {
                Slog.e(UiAutomationManager.LOG_TAG, "unlinkToDeath server error", e);
            }
            UiAutomationManager.this.mUiAutomationServiceOwner = null;
            if (UiAutomationManager.this.mUiAutomationService != null) {
                UiAutomationManager.this.destroyUiAutomationService();
            }
        }
    };

    private class UiAutomationService extends AbstractAccessibilityServiceConnection {
        private final Handler mMainHandler;
        final /* synthetic */ UiAutomationManager this$0;

        UiAutomationService(UiAutomationManager uiAutomationManager, Context context, AccessibilityServiceInfo accessibilityServiceInfo, int id, Handler mainHandler, Object lock, SecurityPolicy securityPolicy, SystemSupport systemSupport, WindowManagerInternal windowManagerInternal, GlobalActionPerformer globalActionPerfomer) {
            this.this$0 = uiAutomationManager;
            super(context, UiAutomationManager.COMPONENT_NAME, accessibilityServiceInfo, id, mainHandler, lock, securityPolicy, systemSupport, windowManagerInternal, globalActionPerfomer);
            this.mMainHandler = mainHandler;
        }

        void connectServiceUnknownThread() {
            this.mMainHandler.post(new -$$Lambda$UiAutomationManager$UiAutomationService$z2oxrodQt4ZxyzsfB6p_GYgwxqk(this));
        }

        public static /* synthetic */ void lambda$connectServiceUnknownThread$0(UiAutomationService uiAutomationService) {
            try {
                IAccessibilityServiceClient serviceInterface;
                IBinder service;
                synchronized (uiAutomationService.mLock) {
                    serviceInterface = uiAutomationService.mServiceInterface;
                    uiAutomationService.mService = serviceInterface == null ? null : uiAutomationService.mServiceInterface.asBinder();
                    service = uiAutomationService.mService;
                }
                if (serviceInterface != null) {
                    service.linkToDeath(uiAutomationService, 0);
                    serviceInterface.init(uiAutomationService, uiAutomationService.mId, uiAutomationService.mOverlayWindowToken);
                }
            } catch (RemoteException re) {
                Slog.w(UiAutomationManager.LOG_TAG, "Error initialized connection", re);
                uiAutomationService.this$0.destroyUiAutomationService();
            }
        }

        public void binderDied() {
            this.this$0.destroyUiAutomationService();
        }

        protected boolean isCalledForCurrentUserLocked() {
            return true;
        }

        protected boolean supportsFlagForNotImportantViews(AccessibilityServiceInfo info) {
            return true;
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(this.mContext, UiAutomationManager.LOG_TAG, pw)) {
                synchronized (this.mLock) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Ui Automation[eventTypes=");
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

        public boolean setSoftKeyboardShowMode(int mode) {
            return false;
        }

        public boolean isAccessibilityButtonAvailable() {
            return false;
        }

        public void disableSelf() {
        }

        public void onServiceConnected(ComponentName componentName, IBinder service) {
        }

        public void onServiceDisconnected(ComponentName componentName) {
        }

        public boolean isCapturingFingerprintGestures() {
            return false;
        }

        public void onFingerprintGestureDetectionActiveChanged(boolean active) {
        }

        public void onFingerprintGesture(int gesture) {
        }
    }

    UiAutomationManager() {
    }

    void registerUiTestAutomationServiceLocked(IBinder owner, IAccessibilityServiceClient serviceClient, Context context, AccessibilityServiceInfo accessibilityServiceInfo, int id, Handler mainHandler, Object lock, SecurityPolicy securityPolicy, SystemSupport systemSupport, WindowManagerInternal windowManagerInternal, GlobalActionPerformer globalActionPerfomer, int flags) {
        int i;
        IBinder iBinder = owner;
        IAccessibilityServiceClient iAccessibilityServiceClient = serviceClient;
        AccessibilityServiceInfo accessibilityServiceInfo2 = accessibilityServiceInfo;
        accessibilityServiceInfo2.setComponentName(COMPONENT_NAME);
        if (this.mUiAutomationService == null) {
            try {
                iBinder.linkToDeath(this.mUiAutomationServiceOwnerDeathRecipient, 0);
                SystemSupport systemSupport2 = systemSupport;
                this.mSystemSupport = systemSupport2;
                this.mUiAutomationService = new UiAutomationService(this, context, accessibilityServiceInfo2, id, mainHandler, lock, securityPolicy, systemSupport2, windowManagerInternal, globalActionPerfomer);
                this.mUiAutomationServiceOwner = iBinder;
                this.mUiAutomationFlags = flags;
                this.mUiAutomationServiceInfo = accessibilityServiceInfo2;
                this.mUiAutomationService.mServiceInterface = iAccessibilityServiceClient;
                this.mUiAutomationService.onAdded();
                try {
                    this.mUiAutomationService.mServiceInterface.asBinder().linkToDeath(this.mUiAutomationService, 0);
                    this.mUiAutomationService.connectServiceUnknownThread();
                    return;
                } catch (RemoteException re) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed registering death link: ");
                    stringBuilder.append(re);
                    Slog.e(str, stringBuilder.toString());
                    destroyUiAutomationService();
                    return;
                }
            } catch (RemoteException re2) {
                i = flags;
                Slog.e(LOG_TAG, "Couldn't register for the death of a UiTestAutomationService!", re2);
                return;
            }
        }
        i = flags;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("UiAutomationService ");
        stringBuilder2.append(iAccessibilityServiceClient);
        stringBuilder2.append("already registered!");
        throw new IllegalStateException(stringBuilder2.toString());
    }

    void unregisterUiTestAutomationServiceLocked(IAccessibilityServiceClient serviceClient) {
        if (this.mUiAutomationService == null || serviceClient == null || this.mUiAutomationService.mServiceInterface == null || serviceClient.asBinder() != this.mUiAutomationService.mServiceInterface.asBinder()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UiAutomationService ");
            stringBuilder.append(serviceClient);
            stringBuilder.append(" not registered!");
            throw new IllegalStateException(stringBuilder.toString());
        }
        destroyUiAutomationService();
    }

    void sendAccessibilityEventLocked(AccessibilityEvent event) {
        if (this.mUiAutomationService != null) {
            this.mUiAutomationService.notifyAccessibilityEvent(event);
        }
    }

    boolean isUiAutomationRunningLocked() {
        return this.mUiAutomationService != null;
    }

    boolean suppressingAccessibilityServicesLocked() {
        return this.mUiAutomationService != null && (this.mUiAutomationFlags & 1) == 0;
    }

    boolean isTouchExplorationEnabledLocked() {
        return this.mUiAutomationService != null && this.mUiAutomationService.mRequestTouchExplorationMode;
    }

    boolean canRetrieveInteractiveWindowsLocked() {
        return this.mUiAutomationService != null && this.mUiAutomationService.mRetrieveInteractiveWindows;
    }

    int getRequestedEventMaskLocked() {
        if (this.mUiAutomationService == null) {
            return 0;
        }
        return this.mUiAutomationService.mEventTypes;
    }

    int getRelevantEventTypes() {
        if (this.mUiAutomationService == null) {
            return 0;
        }
        return this.mUiAutomationService.getRelevantEventTypes();
    }

    AccessibilityServiceInfo getServiceInfo() {
        if (this.mUiAutomationService == null) {
            return null;
        }
        return this.mUiAutomationService.getServiceInfo();
    }

    void dumpUiAutomationService(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mUiAutomationService != null) {
            this.mUiAutomationService.dump(fd, pw, args);
        }
    }

    private void destroyUiAutomationService() {
        try {
            this.mUiAutomationService.mServiceInterface.asBinder().unlinkToDeath(this.mUiAutomationService, 0);
            this.mUiAutomationService.onRemoved();
            this.mUiAutomationService.resetLocked();
            this.mUiAutomationService = null;
            this.mUiAutomationFlags = 0;
            if (this.mUiAutomationServiceOwner != null) {
                this.mUiAutomationServiceOwner.unlinkToDeath(this.mUiAutomationServiceOwnerDeathRecipient, 0);
                this.mUiAutomationServiceOwner = null;
            }
        } catch (Exception e) {
            Slog.e(LOG_TAG, "destroyUiAutomationService error", e);
        }
        this.mSystemSupport.onClientChange(false);
    }
}
