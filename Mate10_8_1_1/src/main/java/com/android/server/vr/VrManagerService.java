package com.android.server.vr;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
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
import android.view.WindowManagerInternal;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import com.android.server.SystemService;
import com.android.server.utils.ManagedApplicationService;
import com.android.server.utils.ManagedApplicationService.BinderChecker;
import com.android.server.utils.ManagedApplicationService.EventCallback;
import com.android.server.utils.ManagedApplicationService.LogEvent;
import com.android.server.utils.ManagedApplicationService.LogFormattable;
import com.android.server.vr.EnabledComponentsObserver.EnabledComponentChangeListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;

public class VrManagerService extends SystemService implements EnabledComponentChangeListener {
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
            VrManagerService.this.logEvent(event);
            synchronized (VrManagerService.this.mLock) {
                ComponentName component = VrManagerService.this.mCurrentVrService == null ? null : VrManagerService.this.mCurrentVrService.getComponent();
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
            boolean state;
            int i;
            switch (msg.what) {
                case 0:
                    state = msg.arg1 == 1;
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
                    state = msg.arg1 == 1;
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
                    throw new IllegalStateException("Unknown message type: " + msg.what);
            }
        }
    };
    private final Object mLock = new Object();
    private boolean mLogLimitHit;
    private final ArrayDeque<LogFormattable> mLoggingDeque = new ArrayDeque(64);
    private final NotificationAccessManager mNotifAccessManager = new NotificationAccessManager();
    private INotificationManager mNotificationManager;
    private final IBinder mOverlayToken = new Binder();
    private VrState mPendingState;
    private boolean mPersistentVrModeEnabled;
    private final RemoteCallbackList<IPersistentVrStateCallbacks> mPersistentVrStateRemoteCallbacks = new RemoteCallbackList();
    private int mPreviousCoarseLocationMode = -1;
    private int mPreviousManageOverlayMode = -1;
    private boolean mRunning2dInVr;
    private int mSystemSleepFlags = 5;
    private boolean mUserUnlocked;
    private Vr2dDisplay mVr2dDisplay;
    private int mVrAppProcessId;
    private final IVrManager mVrManager = new IVrManager.Stub() {
        public void registerListener(IVrStateCallbacks cb) {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
            if (cb == null) {
                throw new IllegalArgumentException("Callback binder object is null.");
            }
            VrManagerService.this.addStateCallback(cb);
        }

        public void unregisterListener(IVrStateCallbacks cb) {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
            if (cb == null) {
                throw new IllegalArgumentException("Callback binder object is null.");
            }
            VrManagerService.this.removeStateCallback(cb);
        }

        public void registerPersistentVrStateListener(IPersistentVrStateCallbacks cb) {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
            if (cb == null) {
                throw new IllegalArgumentException("Callback binder object is null.");
            }
            VrManagerService.this.addPersistentStateCallback(cb);
        }

        public void unregisterPersistentVrStateListener(IPersistentVrStateCallbacks cb) {
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
            if (cb == null) {
                throw new IllegalArgumentException("Callback binder object is null.");
            }
            VrManagerService.this.removePersistentStateCallback(cb);
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
            ComponentName componentName2 = null;
            VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.RESTRICTED_VR_ACCESS");
            VrManagerService vrManagerService = VrManagerService.this;
            if (componentName != null) {
                componentName2 = ComponentName.unflattenFromString(componentName);
            }
            vrManagerService.setAndBindCompositor(componentName2);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(VrManagerService.this.mContext, VrManagerService.TAG, pw)) {
                String str;
                int i;
                pw.println("********* Dump of VrManagerService *********");
                pw.println("VR mode is currently: " + (VrManagerService.this.mVrModeAllowed ? "allowed" : "disallowed"));
                pw.println("Persistent VR mode is currently: " + (VrManagerService.this.mPersistentVrModeEnabled ? "enabled" : "disabled"));
                pw.println("Currently bound VR listener service: " + (VrManagerService.this.mCurrentVrCompositorService == null ? "None" : VrManagerService.this.mCurrentVrCompositorService.getComponent().flattenToString()));
                StringBuilder append = new StringBuilder().append("Currently bound VR compositor service: ");
                if (VrManagerService.this.mCurrentVrCompositorService == null) {
                    str = "None";
                } else {
                    str = VrManagerService.this.mCurrentVrCompositorService.getComponent().flattenToString();
                }
                pw.println(append.append(str).toString());
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
                int userId = VrManagerService.this.mCurrentVrModeUser;
                ArraySet<ComponentName> installed = VrManagerService.this.mComponentObserver.getInstalled(userId);
                if (installed == null || installed.size() == 0) {
                    pw.println("None");
                } else {
                    for (ComponentName n : installed) {
                        pw.print(tab);
                        pw.println(n.flattenToString());
                    }
                }
                pw.println("Enabled VrListenerService components:");
                ArraySet<ComponentName> enabled = VrManagerService.this.mComponentObserver.getEnabled(userId);
                if (enabled == null || enabled.size() == 0) {
                    pw.println("None");
                } else {
                    for (ComponentName n2 : enabled) {
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

    private final class LocalService extends VrManagerInternal {
        private LocalService() {
        }

        public void setVrMode(boolean enabled, ComponentName packageName, int userId, int processId, ComponentName callingPackage) {
            VrManagerService.this.setVrMode(enabled, packageName, userId, processId, callingPackage);
        }

        public void onSleepStateChanged(boolean isAsleep) {
            VrManagerService.this.setSleepState(isAsleep);
        }

        public void onScreenStateChanged(boolean isScreenOn) {
            VrManagerService.this.setScreenOn(isScreenOn);
        }

        public void onKeyguardStateChanged(boolean isShowing) {
            VrManagerService.this.setKeyguardShowing(isShowing);
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

    private final class NotificationAccessManager {
        private final SparseArray<ArraySet<String>> mAllowedPackages;
        private final ArrayMap<String, Integer> mNotificationAccessPackageToUserId;

        private NotificationAccessManager() {
            this.mAllowedPackages = new SparseArray();
            this.mNotificationAccessPackageToUserId = new ArrayMap();
        }

        public void update(Collection<String> packageNames) {
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
            for (String pkg : allowed) {
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

    private static class SettingEvent implements LogFormattable {
        public final long timestamp = System.currentTimeMillis();
        public final String what;

        SettingEvent(String what) {
            this.what = what;
        }

        public String toLogString(SimpleDateFormat dateFormat) {
            return dateFormat.format(new Date(this.timestamp)) + "   " + this.what;
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

    private void setSystemState(int r1, boolean r2) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.server.vr.VrManagerService.setSystemState(int, boolean):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.vr.VrManagerService.setSystemState(int, boolean):void");
    }

    private static native void setVrModeNative(boolean z);

    private void updateVrModeAllowedLocked() {
        boolean z = this.mSystemSleepFlags == 7 ? this.mUserUnlocked : false;
        if (this.mVrModeAllowed != z) {
            this.mVrModeAllowed = z;
            if (this.mVrModeAllowed) {
                if (this.mBootsToVr) {
                    setPersistentVrModeEnabled(true);
                }
                if (this.mBootsToVr && (this.mVrModeEnabled ^ 1) != 0) {
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
                vrState = new VrState(this.mVrModeEnabled, this.mRunning2dInVr, this.mCurrentVrService.getComponent(), this.mCurrentVrService.getUserId(), this.mVrAppProcessId, this.mCurrentVrModeComponent);
            }
            this.mPendingState = vrState;
            updateCurrentVrServiceLocked(false, false, null, 0, -1, null);
        }
    }

    private void setSleepState(boolean isAsleep) {
        setSystemState(1, isAsleep ^ 1);
    }

    private void setScreenOn(boolean isScreenOn) {
        setSystemState(2, isScreenOn);
    }

    private void setKeyguardShowing(boolean isShowing) {
        setSystemState(4, isShowing ^ 1);
    }

    private String getStateAsString() {
        return ((this.mSystemSleepFlags & 1) != 0 ? "awake, " : "") + ((this.mSystemSleepFlags & 2) != 0 ? "screen_on, " : "") + ((this.mSystemSleepFlags & 4) != 0 ? "keyguard_off" : "");
    }

    private void setUserUnlocked() {
        synchronized (this.mLock) {
            this.mUserUnlocked = true;
            updateVrModeAllowedLocked();
        }
    }

    public void onEnabledComponentChanged() {
        synchronized (this.mLock) {
            ArraySet<ComponentName> enabledListeners = this.mComponentObserver.getEnabled(ActivityManager.getCurrentUser());
            if (enabledListeners == null) {
                return;
            }
            ArraySet<String> enabledPackages = new ArraySet();
            for (ComponentName n : enabledListeners) {
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
        int i = 0;
        int length = permissions.length;
        while (i < length) {
            if (this.mContext.checkCallingOrSelfPermission(permissions[i]) != 0) {
                i++;
            } else {
                return;
            }
        }
        throw new SecurityException("Caller does not hold at least one of the permissions: " + Arrays.toString(permissions));
    }

    public VrManagerService(Context context) {
        super(context);
    }

    public void onStart() {
        synchronized (this.mLock) {
            initializeNative();
            this.mContext = getContext();
        }
        this.mBootsToVr = SystemProperties.getBoolean("ro.boot.vr", false);
        publishLocalService(VrManagerInternal.class, new LocalService());
        publishBinderService("vrmanager", this.mVrManager.asBinder());
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
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
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
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
        AppOpsManager appOpsManager = (AppOpsManager) getContext().getSystemService(AppOpsManager.class);
        if (oldUserId != newUserId) {
            appOpsManager.setUserRestrictionForUser(24, false, this.mOverlayToken, null, oldUserId);
        }
        appOpsManager.setUserRestrictionForUser(24, this.mVrModeEnabled, this.mOverlayToken, exemptedPackage == null ? new String[0] : new String[]{exemptedPackage}, newUserId);
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

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean updateCurrentVrServiceLocked(boolean enabled, boolean running2dInVr, ComponentName component, int userId, int processId, ComponentName calling) {
        boolean sendUpdatedCaller = false;
        long identity = Binder.clearCallingIdentity();
        try {
            boolean validUserComponent = this.mComponentObserver.isValid(component, userId) == 0;
            boolean z = validUserComponent ? enabled : false;
            if (!this.mVrModeEnabled && (z ^ 1) != 0) {
                return validUserComponent;
            }
            String packageName = this.mCurrentVrService != null ? this.mCurrentVrService.getComponent().getPackageName() : null;
            int oldUserId = this.mCurrentVrModeUser;
            changeVrModeLocked(z);
            boolean nothingChanged = false;
            if (z) {
                if (this.mCurrentVrService == null) {
                    createAndConnectService(component, userId);
                    sendUpdatedCaller = true;
                } else if (this.mCurrentVrService.disconnectIfNotMatching(component, userId)) {
                    Slog.i(TAG, "VR mode component changed to " + component + ", disconnecting " + this.mCurrentVrService.getComponent() + " for user " + this.mCurrentVrService.getUserId());
                    updateCompositorServiceLocked(-10000, null);
                    createAndConnectService(component, userId);
                    sendUpdatedCaller = true;
                } else {
                    nothingChanged = true;
                }
            } else if (this.mCurrentVrService != null) {
                Slog.i(TAG, "Leaving VR mode, disconnecting " + this.mCurrentVrService.getComponent() + " for user " + this.mCurrentVrService.getUserId());
                this.mCurrentVrService.disconnect();
                updateCompositorServiceLocked(-10000, null);
                this.mCurrentVrService = null;
            } else {
                nothingChanged = true;
            }
            if (calling != null || this.mPersistentVrModeEnabled) {
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean isDefaultAllowed(String packageName) {
        ApplicationInfo info = null;
        try {
            info = this.mContext.getPackageManager().getApplicationInfo(packageName, 128);
        } catch (NameNotFoundException e) {
        }
        if (info != null) {
            int i;
            if (info.isSystemApp()) {
                i = 1;
            } else {
                i = info.isUpdatedSystemApp();
            }
            if ((i ^ 1) == 0) {
                return true;
            }
        }
        return false;
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
        for (ComponentName c : EnabledComponentsObserver.loadComponentNames(this.mContext.getPackageManager(), userId, "android.service.notification.NotificationListenerService", "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE")) {
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
                Slog.w(TAG, "Could not grant coarse location permission, package " + pkg + " was removed.");
            }
        }
    }

    private void revokeCoarseLocationPermissionIfNeeded(String pkg, int userId) {
        if (!isPermissionUserUpdated("android.permission.ACCESS_COARSE_LOCATION", pkg, userId)) {
            try {
                this.mContext.getPackageManager().revokeRuntimePermission(pkg, "android.permission.ACCESS_COARSE_LOCATION", new UserHandle(userId));
            } catch (IllegalArgumentException e) {
                Slog.w(TAG, "Could not revoke coarse location permission, package " + pkg + " was removed.");
            }
        }
    }

    private boolean isPermissionUserUpdated(String permission, String pkg, int userId) {
        if ((this.mContext.getPackageManager().getPermissionFlags(permission, pkg, new UserHandle(userId)) & 3) != 0) {
            return true;
        }
        return false;
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
            return "";
        }
        StringBuilder b = new StringBuilder();
        boolean start = true;
        for (String s : c) {
            if (!"".equals(s)) {
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
        Slog.i(TAG, "Connecting " + component + " for user " + userId);
    }

    private void changeVrModeLocked(boolean enabled) {
        if (this.mVrModeEnabled != enabled) {
            this.mVrModeEnabled = enabled;
            Slog.i(TAG, "VR mode " + (this.mVrModeEnabled ? "enabled" : "disabled"));
            setVrModeNative(this.mVrModeEnabled);
            setHwEnviroment(enabled);
            onVrModeChangedLocked();
        }
    }

    private void setHwEnviroment(boolean enabled) {
        SystemProperties.set("persist.sys.ui.hw", enabled ? "true" : "false");
    }

    private void onVrModeChangedLocked() {
        int i;
        Handler handler = this.mHandler;
        Handler handler2 = this.mHandler;
        if (this.mVrModeEnabled) {
            i = 1;
        } else {
            i = 0;
        }
        handler.sendMessage(handler2.obtainMessage(0, i, 0));
    }

    private ManagedApplicationService createVrListenerService(ComponentName component, int userId) {
        int retryType;
        if (this.mBootsToVr) {
            retryType = 1;
        } else {
            retryType = 2;
        }
        return ManagedApplicationService.build(this.mContext, component, userId, 17041221, "android.settings.VR_LISTENER_SETTINGS", sBinderChecker, true, retryType, this.mHandler, this.mEventCallback);
    }

    private ManagedApplicationService createVrCompositorService(ComponentName component, int userId) {
        int retryType;
        if (this.mBootsToVr) {
            retryType = 1;
        } else {
            retryType = 3;
        }
        return ManagedApplicationService.build(this.mContext, component, userId, 0, null, null, true, retryType, this.mHandler, this.mEventCallback);
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
            for (LogFormattable event : this.mLoggingDeque) {
                pw.println(event.toLogString(d));
            }
        }
    }

    private void setVrMode(boolean enabled, ComponentName targetPackageName, int userId, int processId, ComponentName callingPackage) {
        synchronized (this.mLock) {
            ComponentName targetListener;
            boolean z = !enabled ? this.mPersistentVrModeEnabled : true;
            boolean z2 = !enabled ? this.mPersistentVrModeEnabled : false;
            if (z2) {
                targetListener = this.mDefaultVrService;
            } else {
                targetListener = targetPackageName;
            }
            VrState pending = new VrState(z, z2, targetListener, userId, processId, callingPackage);
            if (this.mVrModeAllowed) {
                if (!z) {
                    if (this.mCurrentVrService != null) {
                        if (this.mPendingState == null) {
                            this.mHandler.sendEmptyMessageDelayed(1, 300);
                        }
                        this.mPendingState = pending;
                        return;
                    }
                }
                this.mHandler.removeMessages(1);
                this.mPendingState = null;
                if (targetListener == null) {
                    return;
                }
                updateCurrentVrServiceLocked(z, z2, targetListener, userId, processId, callingPackage);
                return;
            }
            this.mPendingState = pending;
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
        if (this.mVr2dDisplay != null) {
            this.mVr2dDisplay.setVirtualDisplayProperties(compatDisplayProp);
        } else {
            Slog.w(TAG, "Vr2dDisplay is null!");
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
        synchronized (this.mLock) {
            updateCompositorServiceLocked(userId, componentName);
        }
        Binder.restoreCallingIdentity(token);
    }

    private void updateCompositorServiceLocked(int userId, ComponentName componentName) {
        if (this.mCurrentVrCompositorService != null && this.mCurrentVrCompositorService.disconnectIfNotMatching(componentName, userId)) {
            Slog.i(TAG, "Disconnecting compositor service: " + this.mCurrentVrCompositorService.getComponent());
            this.mCurrentVrCompositorService = null;
        }
        if (componentName != null && this.mCurrentVrCompositorService == null) {
            Slog.i(TAG, "Connecting compositor service: " + componentName);
            this.mCurrentVrCompositorService = createVrCompositorService(componentName, userId);
            this.mCurrentVrCompositorService.connect();
        }
    }

    private void setPersistentModeAndNotifyListenersLocked(boolean enabled) {
        if (this.mPersistentVrModeEnabled != enabled) {
            int i;
            String eventName = "Persistent VR mode " + (enabled ? "enabled" : "disabled");
            Slog.i(TAG, eventName);
            logEvent(new SettingEvent(eventName));
            this.mPersistentVrModeEnabled = enabled;
            Handler handler = this.mHandler;
            Handler handler2 = this.mHandler;
            if (this.mPersistentVrModeEnabled) {
                i = 1;
            } else {
                i = 0;
            }
            handler.sendMessage(handler2.obtainMessage(2, i, 0));
        }
    }

    private int hasVrPackage(ComponentName targetPackageName, int userId) {
        int isValid;
        synchronized (this.mLock) {
            isValid = this.mComponentObserver.isValid(targetPackageName, userId);
        }
        return isValid;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isCurrentVrListener(String packageName, int userId) {
        boolean z = false;
        synchronized (this.mLock) {
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
