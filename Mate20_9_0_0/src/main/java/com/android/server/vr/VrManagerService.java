package com.android.server.vr;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerInternal.ScreenObserver;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.app.NotificationManager;
import android.app.Vr2dDisplayProperties;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.display.DisplayManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.service.vr.IPersistentVrStateCallbacks;
import android.service.vr.IVrListener;
import android.service.vr.IVrListener.Stub;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.view.inputmethod.InputMethodManagerInternal;
import com.android.internal.util.DumpUtils;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import com.android.server.SystemService;
import com.android.server.utils.ManagedApplicationService;
import com.android.server.utils.ManagedApplicationService.BinderChecker;
import com.android.server.utils.ManagedApplicationService.EventCallback;
import com.android.server.utils.ManagedApplicationService.LogEvent;
import com.android.server.utils.ManagedApplicationService.LogFormattable;
import com.android.server.utils.ManagedApplicationService.PendingEvent;
import com.android.server.vr.EnabledComponentsObserver.EnabledComponentChangeListener;
import com.android.server.wm.WindowManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;

public class VrManagerService extends SystemService implements EnabledComponentChangeListener, ScreenObserver {
    static final boolean DBG = false;
    private static final int EVENT_LOG_SIZE = 64;
    private static final int FLAG_ALL = 7;
    private static final int FLAG_AWAKE = 1;
    private static final int FLAG_KEYGUARD_UNLOCKED = 4;
    private static final int FLAG_NONE = 0;
    private static final int FLAG_SCREEN_ON = 2;
    private static final int INVALID_APPOPS_MODE = -1;
    private static final int MSG_PENDING_VR_STATE_CHANGE = 1;
    private static final int MSG_PERSISTENT_VR_MODE_STATE_CHANGE = 2;
    private static final int MSG_VR_STATE_CHANGE = 0;
    private static final int PENDING_STATE_DELAY_MS = 300;
    public static final String TAG = "VrManagerService";
    private static final BinderChecker sBinderChecker = new BinderChecker() {
        public IInterface asInterface(IBinder binder) {
            return Stub.asInterface(binder);
        }

        public boolean checkType(IInterface service) {
            return service instanceof IVrListener;
        }
    };
    private boolean mBootsToVr;
    private EnabledComponentsObserver mComponentObserver;
    private Context mContext;
    private ManagedApplicationService mCurrentVrCompositorService;
    private ComponentName mCurrentVrModeComponent;
    private int mCurrentVrModeUser;
    private ManagedApplicationService mCurrentVrService;
    private ComponentName mDefaultVrService;
    private final EventCallback mEventCallback = new EventCallback() {
        public void onServiceEvent(LogEvent event) {
            ComponentName component;
            VrManagerService.this.logEvent(event);
            synchronized (VrManagerService.this.mLock) {
                component = VrManagerService.this.mCurrentVrService == null ? null : VrManagerService.this.mCurrentVrService.getComponent();
                if (component != null && component.equals(event.component) && (event.event == 2 || event.event == 3)) {
                    VrManagerService.this.callFocusedActivityChangedLocked();
                }
            }
            if (!VrManagerService.this.mBootsToVr && event.event == 4) {
                if (component == null || component.equals(event.component)) {
                    Slog.e(VrManagerService.TAG, "VrListenerSevice has died permanently, leaving system VR mode.");
                    VrManagerService.this.setPersistentVrModeEnabled(false);
                }
            }
        }
    };
    private boolean mGuard;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            boolean z = false;
            boolean state;
            int i;
            switch (msg.what) {
                case 0:
                    if (msg.arg1 == 1) {
                        z = true;
                    }
                    state = z;
                    i = VrManagerService.this.mVrStateRemoteCallbacks.beginBroadcast();
                    while (i > 0) {
                        i--;
                        try {
                            ((IVrStateCallbacks) VrManagerService.this.mVrStateRemoteCallbacks.getBroadcastItem(i)).onVrStateChanged(state);
                        } catch (RemoteException e) {
                        }
                    }
                    VrManagerService.this.mVrStateRemoteCallbacks.finishBroadcast();
                    return;
                case 1:
                    synchronized (VrManagerService.this.mLock) {
                        if (VrManagerService.this.mVrModeAllowed) {
                            VrManagerService.this.consumeAndApplyPendingStateLocked();
                        }
                    }
                    return;
                case 2:
                    if (msg.arg1 == 1) {
                        z = true;
                    }
                    state = z;
                    i = VrManagerService.this.mPersistentVrStateRemoteCallbacks.beginBroadcast();
                    while (i > 0) {
                        i--;
                        try {
                            ((IPersistentVrStateCallbacks) VrManagerService.this.mPersistentVrStateRemoteCallbacks.getBroadcastItem(i)).onPersistentVrStateChanged(state);
                        } catch (RemoteException e2) {
                        }
                    }
                    VrManagerService.this.mPersistentVrStateRemoteCallbacks.finishBroadcast();
                    return;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown message type: ");
                    stringBuilder.append(msg.what);
                    throw new IllegalStateException(stringBuilder.toString());
            }
        }
    };
    private final Object mLock = new Object();
    private boolean mLogLimitHit;
    private final ArrayDeque<LogFormattable> mLoggingDeque = new ArrayDeque(64);
    private final NotificationAccessManager mNotifAccessManager = new NotificationAccessManager(this, null);
    private INotificationManager mNotificationManager;
    private final IBinder mOverlayToken = new Binder();
    private VrState mPendingState;
    private boolean mPersistentVrModeEnabled;
    private final RemoteCallbackList<IPersistentVrStateCallbacks> mPersistentVrStateRemoteCallbacks = new RemoteCallbackList();
    private int mPreviousCoarseLocationMode = -1;
    private int mPreviousManageOverlayMode = -1;
    private boolean mRunning2dInVr;
    private boolean mStandby;
    private int mSystemSleepFlags = 5;
    private boolean mUseStandbyToExitVrMode;
    private boolean mUserUnlocked;
    private Vr2dDisplay mVr2dDisplay;
    private int mVrAppProcessId;
    private final IVrManager mVrManager = new IVrManager.Stub() {
        public void registerListener(IVrStateCallbacks cb) {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
            if (cb != null) {
                VrManagerService.this.addStateCallback(cb);
                return;
            }
            throw new IllegalArgumentException("Callback binder object is null.");
        }

        public void unregisterListener(IVrStateCallbacks cb) {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
            if (cb != null) {
                VrManagerService.this.removeStateCallback(cb);
                return;
            }
            throw new IllegalArgumentException("Callback binder object is null.");
        }

        public void registerPersistentVrStateListener(IPersistentVrStateCallbacks cb) {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
            if (cb != null) {
                VrManagerService.this.addPersistentStateCallback(cb);
                return;
            }
            throw new IllegalArgumentException("Callback binder object is null.");
        }

        public void unregisterPersistentVrStateListener(IPersistentVrStateCallbacks cb) {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
            if (cb != null) {
                VrManagerService.this.removePersistentStateCallback(cb);
                return;
            }
            throw new IllegalArgumentException("Callback binder object is null.");
        }

        public boolean getVrModeState() {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
            return VrManagerService.this.getVrMode();
        }

        public boolean getPersistentVrModeEnabled() {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
            return VrManagerService.this.getPersistentVrMode();
        }

        public void setPersistentVrModeEnabled(boolean enabled) {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.RESTRICTED_VR_ACCESS");
            VrManagerService.this.setPersistentVrModeEnabled(enabled);
        }

        public void setVr2dDisplayProperties(Vr2dDisplayProperties vr2dDisplayProp) {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.RESTRICTED_VR_ACCESS");
            VrManagerService.this.setVr2dDisplayProperties(vr2dDisplayProp);
        }

        public int getVr2dDisplayId() {
            return VrManagerService.this.getVr2dDisplayId();
        }

        public void setAndBindCompositor(String componentName) {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.RESTRICTED_VR_ACCESS");
            VrManagerService.this.setAndBindCompositor(componentName == null ? null : ComponentName.unflattenFromString(componentName));
        }

        public void setStandbyEnabled(boolean standby) {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER");
            VrManagerService.this.setStandbyEnabled(standby);
        }

        public void setVrInputMethod(ComponentName componentName) {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.RESTRICTED_VR_ACCESS");
            ((InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class)).startVrInputMethodNoCheck(componentName);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(VrManagerService.this.mContext, VrManagerService.TAG, pw)) {
                int i;
                pw.println("********* Dump of VrManagerService *********");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("VR mode is currently: ");
                stringBuilder.append(VrManagerService.this.mVrModeAllowed ? "allowed" : "disallowed");
                pw.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("Persistent VR mode is currently: ");
                stringBuilder.append(VrManagerService.this.mPersistentVrModeEnabled ? "enabled" : "disabled");
                pw.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("Currently bound VR listener service: ");
                stringBuilder.append(VrManagerService.this.mCurrentVrService == null ? "None" : VrManagerService.this.mCurrentVrService.getComponent().flattenToString());
                pw.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("Currently bound VR compositor service: ");
                stringBuilder.append(VrManagerService.this.mCurrentVrCompositorService == null ? "None" : VrManagerService.this.mCurrentVrCompositorService.getComponent().flattenToString());
                pw.println(stringBuilder.toString());
                pw.println("Previous state transitions:\n");
                String tab = "  ";
                VrManagerService.this.dumpStateTransitions(pw);
                pw.println("\n\nRemote Callbacks:");
                int i2 = VrManagerService.this.mVrStateRemoteCallbacks.beginBroadcast();
                while (true) {
                    i = i2 - 1;
                    if (i2 <= 0) {
                        break;
                    }
                    pw.print(tab);
                    pw.print(VrManagerService.this.mVrStateRemoteCallbacks.getBroadcastItem(i));
                    if (i > 0) {
                        pw.println(",");
                    }
                    i2 = i;
                }
                VrManagerService.this.mVrStateRemoteCallbacks.finishBroadcast();
                pw.println("\n\nPersistent Vr State Remote Callbacks:");
                i2 = VrManagerService.this.mPersistentVrStateRemoteCallbacks.beginBroadcast();
                while (true) {
                    i = i2 - 1;
                    if (i2 <= 0) {
                        break;
                    }
                    pw.print(tab);
                    pw.print(VrManagerService.this.mPersistentVrStateRemoteCallbacks.getBroadcastItem(i));
                    if (i > 0) {
                        pw.println(",");
                    }
                    i2 = i;
                }
                VrManagerService.this.mPersistentVrStateRemoteCallbacks.finishBroadcast();
                pw.println("\n");
                pw.println("Installed VrListenerService components:");
                i2 = VrManagerService.this.mCurrentVrModeUser;
                ArraySet<ComponentName> installed = VrManagerService.this.mComponentObserver.getInstalled(i2);
                if (installed == null || installed.size() == 0) {
                    pw.println("None");
                } else {
                    Iterator it = installed.iterator();
                    while (it.hasNext()) {
                        ComponentName n = (ComponentName) it.next();
                        pw.print(tab);
                        pw.println(n.flattenToString());
                    }
                }
                pw.println("Enabled VrListenerService components:");
                ArraySet<ComponentName> enabled = VrManagerService.this.mComponentObserver.getEnabled(i2);
                if (enabled == null || enabled.size() == 0) {
                    pw.println("None");
                } else {
                    Iterator it2 = enabled.iterator();
                    while (it2.hasNext()) {
                        ComponentName n2 = (ComponentName) it2.next();
                        pw.print(tab);
                        pw.println(n2.flattenToString());
                    }
                }
                pw.println("\n");
                pw.println("********* End of VrManagerService Dump *********");
            }
        }
    };
    private boolean mVrModeAllowed;
    private boolean mVrModeEnabled;
    private final RemoteCallbackList<IVrStateCallbacks> mVrStateRemoteCallbacks = new RemoteCallbackList();
    private boolean mWasDefaultGranted;

    private final class NotificationAccessManager {
        private final SparseArray<ArraySet<String>> mAllowedPackages;
        private final ArrayMap<String, Integer> mNotificationAccessPackageToUserId;

        private NotificationAccessManager() {
            this.mAllowedPackages = new SparseArray();
            this.mNotificationAccessPackageToUserId = new ArrayMap();
        }

        /* synthetic */ NotificationAccessManager(VrManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void update(Collection<String> packageNames) {
            String pkg;
            int currentUserId = ActivityManager.getCurrentUser();
            ArraySet<String> allowed = (ArraySet) this.mAllowedPackages.get(currentUserId);
            if (allowed == null) {
                allowed = new ArraySet();
            }
            for (int i = this.mNotificationAccessPackageToUserId.size() - 1; i >= 0; i--) {
                int grantUserId = ((Integer) this.mNotificationAccessPackageToUserId.valueAt(i)).intValue();
                if (grantUserId != currentUserId) {
                    String packageName = (String) this.mNotificationAccessPackageToUserId.keyAt(i);
                    VrManagerService.this.revokeNotificationListenerAccess(packageName, grantUserId);
                    VrManagerService.this.revokeNotificationPolicyAccess(packageName);
                    VrManagerService.this.revokeCoarseLocationPermissionIfNeeded(packageName, grantUserId);
                    this.mNotificationAccessPackageToUserId.removeAt(i);
                }
            }
            Iterator it = allowed.iterator();
            while (it.hasNext()) {
                pkg = (String) it.next();
                if (!packageNames.contains(pkg)) {
                    VrManagerService.this.revokeNotificationListenerAccess(pkg, currentUserId);
                    VrManagerService.this.revokeNotificationPolicyAccess(pkg);
                    VrManagerService.this.revokeCoarseLocationPermissionIfNeeded(pkg, currentUserId);
                    this.mNotificationAccessPackageToUserId.remove(pkg);
                }
            }
            for (String pkg2 : packageNames) {
                if (!allowed.contains(pkg2)) {
                    VrManagerService.this.grantNotificationPolicyAccess(pkg2);
                    VrManagerService.this.grantNotificationListenerAccess(pkg2, currentUserId);
                    VrManagerService.this.grantCoarseLocationPermissionIfNeeded(pkg2, currentUserId);
                    this.mNotificationAccessPackageToUserId.put(pkg2, Integer.valueOf(currentUserId));
                }
            }
            allowed.clear();
            allowed.addAll(packageNames);
            this.mAllowedPackages.put(currentUserId, allowed);
        }
    }

    private final class LocalService extends VrManagerInternal {
        private LocalService() {
        }

        /* synthetic */ LocalService(VrManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void setVrMode(boolean enabled, ComponentName packageName, int userId, int processId, ComponentName callingPackage) {
            VrManagerService.this.setVrMode(enabled, packageName, userId, processId, callingPackage);
        }

        public void onScreenStateChanged(boolean isScreenOn) {
            VrManagerService.this.setScreenOn(isScreenOn);
        }

        public boolean isCurrentVrListener(String packageName, int userId) {
            return VrManagerService.this.isCurrentVrListener(packageName, userId);
        }

        public int hasVrPackage(ComponentName packageName, int userId) {
            return VrManagerService.this.hasVrPackage(packageName, userId);
        }

        public void setPersistentVrModeEnabled(boolean enabled) {
            VrManagerService.this.setPersistentVrModeEnabled(enabled);
        }

        public void setVr2dDisplayProperties(Vr2dDisplayProperties compatDisplayProp) {
            VrManagerService.this.setVr2dDisplayProperties(compatDisplayProp);
        }

        public int getVr2dDisplayId() {
            return VrManagerService.this.getVr2dDisplayId();
        }

        public void addPersistentVrModeStateListener(IPersistentVrStateCallbacks listener) {
            VrManagerService.this.addPersistentStateCallback(listener);
        }
    }

    private static class SettingEvent implements LogFormattable {
        public final long timestamp = System.currentTimeMillis();
        public final String what;

        SettingEvent(String what) {
            this.what = what;
        }

        public String toLogString(SimpleDateFormat dateFormat) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(dateFormat.format(new Date(this.timestamp)));
            stringBuilder.append("   ");
            stringBuilder.append(this.what);
            return stringBuilder.toString();
        }
    }

    private static class VrState implements LogFormattable {
        final ComponentName callingPackage;
        final boolean defaultPermissionsGranted;
        final boolean enabled;
        final int processId;
        final boolean running2dInVr;
        final ComponentName targetPackageName;
        final long timestamp;
        final int userId;

        VrState(boolean enabled, boolean running2dInVr, ComponentName targetPackageName, int userId, int processId, ComponentName callingPackage) {
            this.enabled = enabled;
            this.running2dInVr = running2dInVr;
            this.userId = userId;
            this.processId = processId;
            this.targetPackageName = targetPackageName;
            this.callingPackage = callingPackage;
            this.defaultPermissionsGranted = false;
            this.timestamp = System.currentTimeMillis();
        }

        VrState(boolean enabled, boolean running2dInVr, ComponentName targetPackageName, int userId, int processId, ComponentName callingPackage, boolean defaultPermissionsGranted) {
            this.enabled = enabled;
            this.running2dInVr = running2dInVr;
            this.userId = userId;
            this.processId = processId;
            this.targetPackageName = targetPackageName;
            this.callingPackage = callingPackage;
            this.defaultPermissionsGranted = defaultPermissionsGranted;
            this.timestamp = System.currentTimeMillis();
        }

        public String toLogString(SimpleDateFormat dateFormat) {
            String tab = "  ";
            String newLine = "\n";
            StringBuilder sb = new StringBuilder(dateFormat.format(new Date(this.timestamp)));
            sb.append(tab);
            sb.append("State changed to:");
            sb.append(tab);
            sb.append(this.enabled ? "ENABLED" : "DISABLED");
            sb.append(newLine);
            if (this.enabled) {
                String str;
                sb.append(tab);
                sb.append("User=");
                sb.append(this.userId);
                sb.append(newLine);
                sb.append(tab);
                sb.append("Current VR Activity=");
                sb.append(this.callingPackage == null ? "None" : this.callingPackage.flattenToString());
                sb.append(newLine);
                sb.append(tab);
                sb.append("Bound VrListenerService=");
                if (this.targetPackageName == null) {
                    str = "None";
                } else {
                    str = this.targetPackageName.flattenToString();
                }
                sb.append(str);
                sb.append(newLine);
                if (this.defaultPermissionsGranted) {
                    sb.append(tab);
                    sb.append("Default permissions granted to the bound VrListenerService.");
                    sb.append(newLine);
                }
            }
            return sb.toString();
        }
    }

    private static native void initializeNative();

    private static native void setVrModeNative(boolean z);

    private void updateVrModeAllowedLocked() {
        boolean ignoreSleepFlags = this.mBootsToVr && this.mUseStandbyToExitVrMode;
        boolean disallowedByStandby = this.mStandby && this.mUseStandbyToExitVrMode;
        boolean allowed = (this.mSystemSleepFlags == 7 || ignoreSleepFlags) && this.mUserUnlocked && !disallowedByStandby;
        if (this.mVrModeAllowed != allowed) {
            this.mVrModeAllowed = allowed;
            if (this.mVrModeAllowed) {
                if (this.mBootsToVr) {
                    setPersistentVrModeEnabled(true);
                }
                if (this.mBootsToVr && !this.mVrModeEnabled) {
                    setVrMode(true, this.mDefaultVrService, 0, -1, null);
                    return;
                }
                return;
            }
            VrState vrState;
            setPersistentModeAndNotifyListenersLocked(false);
            if (!this.mVrModeEnabled || this.mCurrentVrService == null) {
                vrState = null;
            } else {
                VrState vrState2 = new VrState(this.mVrModeEnabled, this.mRunning2dInVr, this.mCurrentVrService.getComponent(), this.mCurrentVrService.getUserId(), this.mVrAppProcessId, this.mCurrentVrModeComponent);
            }
            this.mPendingState = vrState;
            updateCurrentVrServiceLocked(false, false, null, 0, -1, null);
        }
    }

    private void setScreenOn(boolean isScreenOn) {
        setSystemState(2, isScreenOn);
    }

    public void onAwakeStateChanged(boolean isAwake) {
        setSystemState(1, isAwake);
    }

    public void onKeyguardStateChanged(boolean isShowing) {
        setSystemState(4, isShowing ^ 1);
    }

    private void setSystemState(int flags, boolean isOn) {
        synchronized (this.mLock) {
            int oldState = this.mSystemSleepFlags;
            if (isOn) {
                this.mSystemSleepFlags |= flags;
            } else {
                this.mSystemSleepFlags &= ~flags;
            }
            if (oldState != this.mSystemSleepFlags) {
                updateVrModeAllowedLocked();
            }
        }
    }

    private String getStateAsString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((this.mSystemSleepFlags & 1) != 0 ? "awake, " : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        stringBuilder.append((this.mSystemSleepFlags & 2) != 0 ? "screen_on, " : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        stringBuilder.append((this.mSystemSleepFlags & 4) != 0 ? "keyguard_off" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        return stringBuilder.toString();
    }

    private void setUserUnlocked() {
        synchronized (this.mLock) {
            this.mUserUnlocked = true;
            updateVrModeAllowedLocked();
        }
    }

    private void setStandbyEnabled(boolean standby) {
        synchronized (this.mLock) {
            if (this.mBootsToVr) {
                this.mStandby = standby;
                updateVrModeAllowedLocked();
                return;
            }
            Slog.e(TAG, "Attempting to set standby mode on a non-standalone device");
        }
    }

    public void onEnabledComponentChanged() {
        synchronized (this.mLock) {
            ArraySet<ComponentName> enabledListeners = this.mComponentObserver.getEnabled(ActivityManager.getCurrentUser());
            if (enabledListeners == null) {
                return;
            }
            ArraySet<String> enabledPackages = new ArraySet();
            Iterator it = enabledListeners.iterator();
            while (it.hasNext()) {
                ComponentName n = (ComponentName) it.next();
                if (isDefaultAllowed(n.getPackageName())) {
                    enabledPackages.add(n.getPackageName());
                }
            }
            this.mNotifAccessManager.update(enabledPackages);
            if (this.mVrModeAllowed) {
                consumeAndApplyPendingStateLocked(false);
                if (this.mCurrentVrService == null) {
                    return;
                }
                updateCurrentVrServiceLocked(this.mVrModeEnabled, this.mRunning2dInVr, this.mCurrentVrService.getComponent(), this.mCurrentVrService.getUserId(), this.mVrAppProcessId, this.mCurrentVrModeComponent);
                return;
            }
        }
    }

    private void enforceCallerPermissionAnyOf(String... permissions) {
        int length = permissions.length;
        int i = 0;
        while (i < length) {
            if (this.mContext.checkCallingOrSelfPermission(permissions[i]) != 0) {
                i++;
            } else {
                return;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Caller does not hold at least one of the permissions: ");
        stringBuilder.append(Arrays.toString(permissions));
        throw new SecurityException(stringBuilder.toString());
    }

    public VrManagerService(Context context) {
        super(context);
    }

    public void onStart() {
        synchronized (this.mLock) {
            initializeNative();
            this.mContext = getContext();
        }
        boolean z = false;
        this.mBootsToVr = SystemProperties.getBoolean("ro.boot.vr", false);
        if (this.mBootsToVr && SystemProperties.getBoolean("persist.vr.use_standby_to_exit_vr_mode", true)) {
            z = true;
        }
        this.mUseStandbyToExitVrMode = z;
        publishLocalService(VrManagerInternal.class, new LocalService(this, null));
        publishBinderService("vrmanager", this.mVrManager.asBinder());
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).registerScreenObserver(this);
            this.mNotificationManager = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
            synchronized (this.mLock) {
                Looper looper = Looper.getMainLooper();
                Handler handler = new Handler(looper);
                ArrayList<EnabledComponentChangeListener> listeners = new ArrayList();
                listeners.add(this);
                this.mComponentObserver = EnabledComponentsObserver.build(this.mContext, handler, "enabled_vr_listeners", looper, "android.permission.BIND_VR_LISTENER_SERVICE", "android.service.vr.VrListenerService", this.mLock, listeners);
                this.mComponentObserver.rebuildAll();
            }
            ArraySet<ComponentName> defaultVrComponents = SystemConfig.getInstance().getDefaultVrComponents();
            if (defaultVrComponents.size() > 0) {
                this.mDefaultVrService = (ComponentName) defaultVrComponents.valueAt(0);
            } else {
                Slog.i(TAG, "No default vr listener service found.");
            }
            this.mVr2dDisplay = new Vr2dDisplay((DisplayManager) getContext().getSystemService("display"), (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class), (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class), this.mVrManager);
            this.mVr2dDisplay.init(getContext(), this.mBootsToVr);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.USER_UNLOCKED");
            getContext().registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                        VrManagerService.this.setUserUnlocked();
                    }
                }
            }, intentFilter);
        }
    }

    public void onStartUser(int userHandle) {
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
        }
    }

    public void onSwitchUser(int userHandle) {
        FgThread.getHandler().post(new -$$Lambda$VrManagerService$hhbi29QXTMTcQg-S7n5SpAawSZs(this));
    }

    public static /* synthetic */ void lambda$onSwitchUser$0(VrManagerService vrManagerService) {
        synchronized (vrManagerService.mLock) {
            vrManagerService.mComponentObserver.onUsersChanged();
        }
    }

    public void onStopUser(int userHandle) {
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
        }
    }

    public void onCleanupUser(int userHandle) {
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
        }
    }

    private void updateOverlayStateLocked(String exemptedPackage, int newUserId, int oldUserId) {
        String[] exemptions;
        AppOpsManager appOpsManager = (AppOpsManager) getContext().getSystemService(AppOpsManager.class);
        if (oldUserId != newUserId) {
            appOpsManager.setUserRestrictionForUser(24, false, this.mOverlayToken, null, oldUserId);
        }
        if (exemptedPackage == null) {
            exemptions = new String[0];
        } else {
            exemptions = new String[]{exemptedPackage};
        }
        appOpsManager.setUserRestrictionForUser(24, this.mVrModeEnabled, this.mOverlayToken, exemptions, newUserId);
    }

    private void updateDependentAppOpsLocked(String newVrServicePackage, int newUserId, String oldVrServicePackage, int oldUserId) {
        if (!Objects.equals(newVrServicePackage, oldVrServicePackage)) {
            long identity = Binder.clearCallingIdentity();
            try {
                updateOverlayStateLocked(newVrServicePackage, newUserId, oldUserId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private boolean updateCurrentVrServiceLocked(boolean enabled, boolean running2dInVr, ComponentName component, int userId, int processId, ComponentName calling) {
        Throwable validUserComponent;
        int i;
        boolean z = running2dInVr;
        ComponentName componentName = component;
        int i2 = userId;
        ComponentName componentName2 = calling;
        boolean sendUpdatedCaller = false;
        long identity = Binder.clearCallingIdentity();
        boolean goingIntoVrMode = false;
        boolean validUserComponent2 = this.mComponentObserver.isValid(componentName, i2) == 0;
        if (validUserComponent2 && enabled) {
            goingIntoVrMode = true;
        }
        if (this.mVrModeEnabled || goingIntoVrMode) {
            String oldVrServicePackage;
            int oldUserId;
            boolean nothingChanged;
            boolean sendUpdatedCaller2;
            boolean sendUpdatedCaller3;
            try {
                if (this.mCurrentVrService != null) {
                    try {
                        oldVrServicePackage = this.mCurrentVrService.getComponent().getPackageName();
                    } catch (Throwable th) {
                        validUserComponent = th;
                        i = processId;
                    }
                } else {
                    oldVrServicePackage = null;
                }
                oldUserId = this.mCurrentVrModeUser;
                changeVrModeLocked(goingIntoVrMode);
                nothingChanged = false;
                if (goingIntoVrMode) {
                    sendUpdatedCaller3 = false;
                    if (this.mCurrentVrService == null) {
                        createAndConnectService(componentName, i2);
                        sendUpdatedCaller2 = true;
                    } else if (this.mCurrentVrService.disconnectIfNotMatching(componentName, i2)) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("VR mode component changed to ");
                        stringBuilder.append(componentName);
                        stringBuilder.append(", disconnecting ");
                        stringBuilder.append(this.mCurrentVrService.getComponent());
                        stringBuilder.append(" for user ");
                        stringBuilder.append(this.mCurrentVrService.getUserId());
                        Slog.i(str, stringBuilder.toString());
                        updateCompositorServiceLocked(-10000, null);
                        createAndConnectService(componentName, i2);
                        sendUpdatedCaller2 = true;
                    } else {
                        nothingChanged = true;
                    }
                    sendUpdatedCaller3 = sendUpdatedCaller2;
                } else {
                    try {
                        if (this.mCurrentVrService != null) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            sendUpdatedCaller3 = false;
                            stringBuilder2.append("Leaving VR mode, disconnecting ");
                            stringBuilder2.append(this.mCurrentVrService.getComponent());
                            stringBuilder2.append(" for user ");
                            stringBuilder2.append(this.mCurrentVrService.getUserId());
                            Slog.i(str2, stringBuilder2.toString());
                            this.mCurrentVrService.disconnect();
                            updateCompositorServiceLocked(-10000, null);
                            this.mCurrentVrService = null;
                        } else {
                            sendUpdatedCaller3 = false;
                            nothingChanged = true;
                        }
                    } catch (Throwable th2) {
                        validUserComponent = th2;
                        i = processId;
                        Binder.restoreCallingIdentity(identity);
                        throw validUserComponent;
                    }
                }
                if (((componentName2 != null || this.mPersistentVrModeEnabled) && !Objects.equals(componentName2, this.mCurrentVrModeComponent)) || this.mRunning2dInVr != z) {
                    sendUpdatedCaller2 = true;
                } else {
                    sendUpdatedCaller2 = sendUpdatedCaller3;
                }
            } catch (Throwable th3) {
                validUserComponent = th3;
                i = processId;
                sendUpdatedCaller3 = false;
                Binder.restoreCallingIdentity(identity);
                throw validUserComponent;
            }
            try {
                this.mCurrentVrModeComponent = componentName2;
                this.mRunning2dInVr = z;
            } catch (Throwable th4) {
                validUserComponent = th4;
                i = processId;
                Binder.restoreCallingIdentity(identity);
                throw validUserComponent;
            }
            try {
                this.mVrAppProcessId = processId;
                if (this.mCurrentVrModeUser != i2) {
                    this.mCurrentVrModeUser = i2;
                    sendUpdatedCaller = true;
                } else {
                    sendUpdatedCaller = sendUpdatedCaller2;
                }
                try {
                    sendUpdatedCaller2 = this.mCurrentVrService != null ? this.mCurrentVrService.getComponent().getPackageName() : false;
                    int newUserId = this.mCurrentVrModeUser;
                    updateDependentAppOpsLocked(sendUpdatedCaller2, newUserId, oldVrServicePackage, oldUserId);
                    if (this.mCurrentVrService != 0 && sendUpdatedCaller) {
                        callFocusedActivityChangedLocked();
                    }
                    if (!nothingChanged) {
                        logStateLocked();
                    }
                    Binder.restoreCallingIdentity(identity);
                    return validUserComponent2;
                } catch (Throwable th5) {
                    validUserComponent = th5;
                    Binder.restoreCallingIdentity(identity);
                    throw validUserComponent;
                }
            } catch (Throwable th6) {
                validUserComponent = th6;
                Binder.restoreCallingIdentity(identity);
                throw validUserComponent;
            }
        }
        Binder.restoreCallingIdentity(identity);
        return validUserComponent2;
    }

    private void callFocusedActivityChangedLocked() {
        final ComponentName c = this.mCurrentVrModeComponent;
        final boolean b = this.mRunning2dInVr;
        final int pid = this.mVrAppProcessId;
        this.mCurrentVrService.sendEvent(new PendingEvent() {
            public void runEvent(IInterface service) throws RemoteException {
                ((IVrListener) service).focusedActivityChanged(c, b, pid);
            }
        });
    }

    private boolean isDefaultAllowed(String packageName) {
        ApplicationInfo info = null;
        try {
            info = this.mContext.getPackageManager().getApplicationInfo(packageName, 128);
        } catch (NameNotFoundException e) {
        }
        if (info == null || (!info.isSystemApp() && !info.isUpdatedSystemApp())) {
            return false;
        }
        return true;
    }

    private void grantNotificationPolicyAccess(String pkg) {
        ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).setNotificationPolicyAccessGranted(pkg, true);
    }

    private void revokeNotificationPolicyAccess(String pkg) {
        NotificationManager nm = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        nm.removeAutomaticZenRules(pkg);
        nm.setNotificationPolicyAccessGranted(pkg, false);
    }

    private void grantNotificationListenerAccess(String pkg, int userId) {
        NotificationManager nm = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        Iterator it = EnabledComponentsObserver.loadComponentNames(this.mContext.getPackageManager(), userId, "android.service.notification.NotificationListenerService", "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE").iterator();
        while (it.hasNext()) {
            ComponentName c = (ComponentName) it.next();
            if (Objects.equals(c.getPackageName(), pkg)) {
                nm.setNotificationListenerAccessGrantedForUser(c, userId, true);
            }
        }
    }

    private void revokeNotificationListenerAccess(String pkg, int userId) {
        NotificationManager nm = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        for (ComponentName component : nm.getEnabledNotificationListeners(userId)) {
            if (component != null && component.getPackageName().equals(pkg)) {
                nm.setNotificationListenerAccessGrantedForUser(component, userId, false);
            }
        }
    }

    private void grantCoarseLocationPermissionIfNeeded(String pkg, int userId) {
        if (!isPermissionUserUpdated("android.permission.ACCESS_COARSE_LOCATION", pkg, userId)) {
            try {
                this.mContext.getPackageManager().grantRuntimePermission(pkg, "android.permission.ACCESS_COARSE_LOCATION", new UserHandle(userId));
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Could not grant coarse location permission, package ");
                stringBuilder.append(pkg);
                stringBuilder.append(" was removed.");
                Slog.w(str, stringBuilder.toString());
            }
        }
    }

    private void revokeCoarseLocationPermissionIfNeeded(String pkg, int userId) {
        if (!isPermissionUserUpdated("android.permission.ACCESS_COARSE_LOCATION", pkg, userId)) {
            try {
                this.mContext.getPackageManager().revokeRuntimePermission(pkg, "android.permission.ACCESS_COARSE_LOCATION", new UserHandle(userId));
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Could not revoke coarse location permission, package ");
                stringBuilder.append(pkg);
                stringBuilder.append(" was removed.");
                Slog.w(str, stringBuilder.toString());
            }
        }
    }

    private boolean isPermissionUserUpdated(String permission, String pkg, int userId) {
        return (this.mContext.getPackageManager().getPermissionFlags(permission, pkg, new UserHandle(userId)) & 3) != 0;
    }

    private ArraySet<String> getNotificationListeners(ContentResolver resolver, int userId) {
        String flat = Secure.getStringForUser(resolver, "enabled_notification_listeners", userId);
        ArraySet<String> current = new ArraySet();
        if (flat != null) {
            for (String s : flat.split(":")) {
                if (!TextUtils.isEmpty(s)) {
                    current.add(s);
                }
            }
        }
        return current;
    }

    private static String formatSettings(Collection<String> c) {
        if (c == null || c.isEmpty()) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        StringBuilder b = new StringBuilder();
        boolean start = true;
        for (String s : c) {
            if (!BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(s)) {
                if (!start) {
                    b.append(':');
                }
                b.append(s);
                start = false;
            }
        }
        return b.toString();
    }

    private void createAndConnectService(ComponentName component, int userId) {
        this.mCurrentVrService = createVrListenerService(component, userId);
        this.mCurrentVrService.connect();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Connecting ");
        stringBuilder.append(component);
        stringBuilder.append(" for user ");
        stringBuilder.append(userId);
        Slog.i(str, stringBuilder.toString());
    }

    private void changeVrModeLocked(boolean enabled) {
        if (this.mVrModeEnabled != enabled) {
            this.mVrModeEnabled = enabled;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("VR mode ");
            stringBuilder.append(this.mVrModeEnabled ? "enabled" : "disabled");
            Slog.i(str, stringBuilder.toString());
            setVrModeNative(this.mVrModeEnabled);
            setHwEnviroment(enabled);
            onVrModeChangedLocked();
        }
    }

    private void setHwEnviroment(boolean enabled) {
        SystemProperties.set("persist.sys.ui.hw", enabled ? "true" : "false");
    }

    private void onVrModeChangedLocked() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(0, this.mVrModeEnabled, 0));
    }

    private ManagedApplicationService createVrListenerService(ComponentName component, int userId) {
        int i;
        if (this.mBootsToVr) {
            i = 1;
        } else {
            i = 2;
        }
        return ManagedApplicationService.build(this.mContext, component, userId, 17041345, "android.settings.VR_LISTENER_SETTINGS", sBinderChecker, true, i, this.mHandler, this.mEventCallback);
    }

    private ManagedApplicationService createVrCompositorService(ComponentName component, int userId) {
        int i;
        if (this.mBootsToVr) {
            i = 1;
        } else {
            i = 3;
        }
        return ManagedApplicationService.build(this.mContext, component, userId, 0, null, null, true, i, this.mHandler, this.mEventCallback);
    }

    private void consumeAndApplyPendingStateLocked() {
        consumeAndApplyPendingStateLocked(true);
    }

    private void consumeAndApplyPendingStateLocked(boolean disconnectIfNoPendingState) {
        if (this.mPendingState != null) {
            updateCurrentVrServiceLocked(this.mPendingState.enabled, this.mPendingState.running2dInVr, this.mPendingState.targetPackageName, this.mPendingState.userId, this.mPendingState.processId, this.mPendingState.callingPackage);
            this.mPendingState = null;
        } else if (disconnectIfNoPendingState) {
            updateCurrentVrServiceLocked(false, false, null, 0, -1, null);
        }
    }

    private void logStateLocked() {
        ComponentName componentName;
        if (this.mCurrentVrService == null) {
            componentName = null;
        } else {
            componentName = this.mCurrentVrService.getComponent();
        }
        logEvent(new VrState(this.mVrModeEnabled, this.mRunning2dInVr, componentName, this.mCurrentVrModeUser, this.mVrAppProcessId, this.mCurrentVrModeComponent, this.mWasDefaultGranted));
    }

    private void logEvent(LogFormattable event) {
        synchronized (this.mLoggingDeque) {
            if (this.mLoggingDeque.size() == 64) {
                this.mLoggingDeque.removeFirst();
                this.mLogLimitHit = true;
            }
            this.mLoggingDeque.add(event);
        }
    }

    private void dumpStateTransitions(PrintWriter pw) {
        SimpleDateFormat d = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
        synchronized (this.mLoggingDeque) {
            if (this.mLoggingDeque.size() == 0) {
                pw.print("  ");
                pw.println("None");
            }
            if (this.mLogLimitHit) {
                pw.println("...");
            }
            Iterator it = this.mLoggingDeque.iterator();
            while (it.hasNext()) {
                pw.println(((LogFormattable) it.next()).toLogString(d));
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0023  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x0020  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0041  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x003d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setVrMode(boolean enabled, ComponentName targetPackageName, int userId, int processId, ComponentName callingPackage) {
        synchronized (this.mLock) {
            boolean z;
            boolean targetEnabledState;
            ComponentName targetListener;
            ComponentName targetListener2;
            VrState pending;
            boolean running2dInVr = false;
            if (!enabled) {
                if (!this.mPersistentVrModeEnabled) {
                    z = false;
                    targetEnabledState = z;
                    if (!enabled && this.mPersistentVrModeEnabled) {
                        running2dInVr = true;
                    }
                    if (running2dInVr) {
                        targetListener = targetPackageName;
                    } else {
                        targetListener = this.mDefaultVrService;
                    }
                    targetListener2 = targetListener;
                    pending = new VrState(targetEnabledState, running2dInVr, targetListener2, userId, processId, callingPackage);
                    if (this.mVrModeAllowed) {
                        this.mPendingState = pending;
                        return;
                    } else if (targetEnabledState || this.mCurrentVrService == null) {
                        this.mHandler.removeMessages(1);
                        this.mPendingState = null;
                        if (targetListener2 == null) {
                            return;
                        }
                        updateCurrentVrServiceLocked(targetEnabledState, running2dInVr, targetListener2, userId, processId, callingPackage);
                        return;
                    } else {
                        if (this.mPendingState == null) {
                            this.mHandler.sendEmptyMessageDelayed(1, 300);
                        }
                        this.mPendingState = pending;
                        return;
                    }
                }
            }
            z = true;
            targetEnabledState = z;
            running2dInVr = true;
            if (running2dInVr) {
            }
            targetListener2 = targetListener;
            pending = new VrState(targetEnabledState, running2dInVr, targetListener2, userId, processId, callingPackage);
            if (this.mVrModeAllowed) {
            }
        }
    }

    private void setPersistentVrModeEnabled(boolean enabled) {
        synchronized (this.mLock) {
            setPersistentModeAndNotifyListenersLocked(enabled);
            if (!enabled) {
                setVrMode(false, null, 0, -1, null);
            }
        }
    }

    public void setVr2dDisplayProperties(Vr2dDisplayProperties compatDisplayProp) {
        long token = Binder.clearCallingIdentity();
        try {
            if (this.mVr2dDisplay != null) {
                this.mVr2dDisplay.setVirtualDisplayProperties(compatDisplayProp);
                return;
            }
            Binder.restoreCallingIdentity(token);
            Slog.w(TAG, "Vr2dDisplay is null!");
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private int getVr2dDisplayId() {
        if (this.mVr2dDisplay != null) {
            return this.mVr2dDisplay.getVirtualDisplayId();
        }
        Slog.w(TAG, "Vr2dDisplay is null!");
        return -1;
    }

    private void setAndBindCompositor(ComponentName componentName) {
        int userId = UserHandle.getCallingUserId();
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                updateCompositorServiceLocked(userId, componentName);
            }
            Binder.restoreCallingIdentity(token);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void updateCompositorServiceLocked(int userId, ComponentName componentName) {
        String str;
        StringBuilder stringBuilder;
        if (this.mCurrentVrCompositorService != null && this.mCurrentVrCompositorService.disconnectIfNotMatching(componentName, userId)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Disconnecting compositor service: ");
            stringBuilder.append(this.mCurrentVrCompositorService.getComponent());
            Slog.i(str, stringBuilder.toString());
            this.mCurrentVrCompositorService = null;
        }
        if (componentName != null && this.mCurrentVrCompositorService == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Connecting compositor service: ");
            stringBuilder.append(componentName);
            Slog.i(str, stringBuilder.toString());
            this.mCurrentVrCompositorService = createVrCompositorService(componentName, userId);
            this.mCurrentVrCompositorService.connect();
        }
    }

    private void setPersistentModeAndNotifyListenersLocked(boolean enabled) {
        if (this.mPersistentVrModeEnabled != enabled) {
            String eventName = new StringBuilder();
            eventName.append("Persistent VR mode ");
            eventName.append(enabled ? "enabled" : "disabled");
            eventName = eventName.toString();
            Slog.i(TAG, eventName);
            logEvent(new SettingEvent(eventName));
            this.mPersistentVrModeEnabled = enabled;
            this.mHandler.sendMessage(this.mHandler.obtainMessage(2, this.mPersistentVrModeEnabled, 0));
        }
    }

    private int hasVrPackage(ComponentName targetPackageName, int userId) {
        int isValid;
        synchronized (this.mLock) {
            isValid = this.mComponentObserver.isValid(targetPackageName, userId);
        }
        return isValid;
    }

    /* JADX WARNING: Missing block: B:13:0x0025, code:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isCurrentVrListener(String packageName, int userId) {
        synchronized (this.mLock) {
            boolean z = false;
            if (this.mCurrentVrService == null) {
                return false;
            } else if (this.mCurrentVrService.getComponent().getPackageName().equals(packageName) && userId == this.mCurrentVrService.getUserId()) {
                z = true;
            }
        }
    }

    private void addStateCallback(IVrStateCallbacks cb) {
        this.mVrStateRemoteCallbacks.register(cb);
    }

    private void removeStateCallback(IVrStateCallbacks cb) {
        this.mVrStateRemoteCallbacks.unregister(cb);
    }

    private void addPersistentStateCallback(IPersistentVrStateCallbacks cb) {
        this.mPersistentVrStateRemoteCallbacks.register(cb);
    }

    private void removePersistentStateCallback(IPersistentVrStateCallbacks cb) {
        this.mPersistentVrStateRemoteCallbacks.unregister(cb);
    }

    private boolean getVrMode() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mVrModeEnabled;
        }
        return z;
    }

    private boolean getPersistentVrMode() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mPersistentVrModeEnabled;
        }
        return z;
    }
}
