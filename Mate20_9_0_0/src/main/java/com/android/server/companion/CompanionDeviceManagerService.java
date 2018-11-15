package com.android.server.companion;

import android.app.PendingIntent;
import android.companion.AssociationRequest;
import android.companion.ICompanionDeviceDiscoveryService;
import android.companion.ICompanionDeviceDiscoveryServiceCallback;
import android.companion.ICompanionDeviceManager;
import android.companion.IFindDeviceCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.NetworkPolicyManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IDeviceIdleController;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.provider.SettingsStringUtil.ComponentNameSet;
import android.text.BidiFormatter;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.ExceptionUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IAppOpsService.Stub;
import com.android.internal.content.PackageMonitor;
import com.android.internal.notification.NotificationAccessConfirmationActivityContract;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.FgThread;
import com.android.server.SystemService;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class CompanionDeviceManagerService extends SystemService implements DeathRecipient {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "CompanionDeviceManagerService";
    private static final ComponentName SERVICE_TO_BIND_TO = ComponentName.createRelative("com.android.companiondevicemanager", ".DeviceDiscoveryService");
    private static final String XML_ATTR_DEVICE = "device";
    private static final String XML_ATTR_PACKAGE = "package";
    private static final String XML_FILE_NAME = "companion_device_manager_associations.xml";
    private static final String XML_TAG_ASSOCIATION = "association";
    private static final String XML_TAG_ASSOCIATIONS = "associations";
    private IAppOpsService mAppOpsManager = Stub.asInterface(ServiceManager.getService("appops"));
    private String mCallingPackage;
    private IFindDeviceCallback mFindDeviceCallback;
    private IDeviceIdleController mIdleController = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
    private final CompanionDeviceManagerImpl mImpl = new CompanionDeviceManagerImpl();
    private final Object mLock = new Object();
    private AssociationRequest mRequest;
    private ServiceConnection mServiceConnection;
    private final ConcurrentMap<Integer, AtomicFile> mUidToStorage = new ConcurrentHashMap();

    private class Association {
        public final String companionAppPackage;
        public final String deviceAddress;
        public final int uid;

        /* synthetic */ Association(CompanionDeviceManagerService x0, int x1, String x2, String x3, AnonymousClass1 x4) {
            this(x1, x2, x3);
        }

        private Association(int uid, String deviceAddress, String companionAppPackage) {
            this.uid = uid;
            this.deviceAddress = (String) Preconditions.checkNotNull(deviceAddress);
            this.companionAppPackage = (String) Preconditions.checkNotNull(companionAppPackage);
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Association that = (Association) o;
            return (this.uid == that.uid && this.deviceAddress.equals(that.deviceAddress)) ? this.companionAppPackage.equals(that.companionAppPackage) : false;
        }

        public int hashCode() {
            return (31 * ((31 * this.uid) + this.deviceAddress.hashCode())) + this.companionAppPackage.hashCode();
        }
    }

    class CompanionDeviceManagerImpl extends ICompanionDeviceManager.Stub {
        CompanionDeviceManagerImpl() {
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (Throwable e) {
                Slog.e(CompanionDeviceManagerService.LOG_TAG, "Error during IPC", e);
                RuntimeException propagate = ExceptionUtils.propagate(e, RemoteException.class);
            }
        }

        public void associate(AssociationRequest request, IFindDeviceCallback callback, String callingPackage) throws RemoteException {
            Preconditions.checkNotNull(request, "Request cannot be null");
            Preconditions.checkNotNull(callback, "Callback cannot be null");
            checkCallerIsSystemOr(callingPackage);
            int userId = CompanionDeviceManagerService.getCallingUserId();
            checkUsesFeature(callingPackage, userId);
            long callingIdentity = Binder.clearCallingIdentity();
            try {
                CompanionDeviceManagerService.this.getContext().bindServiceAsUser(new Intent().setComponent(CompanionDeviceManagerService.SERVICE_TO_BIND_TO), CompanionDeviceManagerService.this.createServiceConnection(request, callback, callingPackage), 1, UserHandle.of(userId));
            } finally {
                Binder.restoreCallingIdentity(callingIdentity);
            }
        }

        public void stopScan(AssociationRequest request, IFindDeviceCallback callback, String callingPackage) {
            if (Objects.equals(request, CompanionDeviceManagerService.this.mRequest) && Objects.equals(callback, CompanionDeviceManagerService.this.mFindDeviceCallback) && Objects.equals(callingPackage, CompanionDeviceManagerService.this.mCallingPackage)) {
                CompanionDeviceManagerService.this.cleanup();
            }
        }

        public List<String> getAssociations(String callingPackage, int userId) throws RemoteException {
            checkCallerIsSystemOr(callingPackage, userId);
            checkUsesFeature(callingPackage, CompanionDeviceManagerService.getCallingUserId());
            return new ArrayList(CollectionUtils.map(CompanionDeviceManagerService.this.readAllAssociations(userId, callingPackage), -$$Lambda$CompanionDeviceManagerService$CompanionDeviceManagerImpl$bdv3Vfadbb8b9nrSgkARO4oYOXU.INSTANCE));
        }

        public void disassociate(String deviceMacAddress, String callingPackage) throws RemoteException {
            Preconditions.checkNotNull(deviceMacAddress);
            checkCallerIsSystemOr(callingPackage);
            checkUsesFeature(callingPackage, CompanionDeviceManagerService.getCallingUserId());
            CompanionDeviceManagerService.this.removeAssociation(CompanionDeviceManagerService.getCallingUserId(), callingPackage, deviceMacAddress);
        }

        private void checkCallerIsSystemOr(String pkg) throws RemoteException {
            checkCallerIsSystemOr(pkg, CompanionDeviceManagerService.getCallingUserId());
        }

        private void checkCallerIsSystemOr(String pkg, int userId) throws RemoteException {
            if (!CompanionDeviceManagerService.isCallerSystem()) {
                Preconditions.checkArgument(CompanionDeviceManagerService.getCallingUserId() == userId, "Must be called by either same user or system");
                CompanionDeviceManagerService.this.mAppOpsManager.checkPackage(Binder.getCallingUid(), pkg);
            }
        }

        public PendingIntent requestNotificationAccess(ComponentName component) throws RemoteException {
            String callingPackage = component.getPackageName();
            checkCanCallNotificationApi(callingPackage);
            int userId = CompanionDeviceManagerService.getCallingUserId();
            String packageTitle = BidiFormatter.getInstance().unicodeWrap(CompanionDeviceManagerService.this.getPackageInfo(callingPackage, userId).applicationInfo.loadSafeLabel(CompanionDeviceManagerService.this.getContext().getPackageManager()).toString());
            long identity = Binder.clearCallingIdentity();
            try {
                PendingIntent activity = PendingIntent.getActivity(CompanionDeviceManagerService.this.getContext(), 0, NotificationAccessConfirmationActivityContract.launcherIntent(userId, component, packageTitle), 1409286144);
                return activity;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean hasNotificationAccess(ComponentName component) throws RemoteException {
            checkCanCallNotificationApi(component.getPackageName());
            return new ComponentNameSet(Secure.getString(CompanionDeviceManagerService.this.getContext().getContentResolver(), "enabled_notification_listeners")).contains(component);
        }

        private void checkCanCallNotificationApi(String callingPackage) throws RemoteException {
            checkCallerIsSystemOr(callingPackage);
            int userId = CompanionDeviceManagerService.getCallingUserId();
            Preconditions.checkState(ArrayUtils.isEmpty(CompanionDeviceManagerService.this.readAllAssociations(userId, callingPackage)) ^ 1, "App must have an association before calling this API");
            checkUsesFeature(callingPackage, userId);
        }

        private void checkUsesFeature(String pkg, int userId) {
            if (!CompanionDeviceManagerService.isCallerSystem()) {
                FeatureInfo[] reqFeatures = CompanionDeviceManagerService.this.getPackageInfo(pkg, userId).reqFeatures;
                String requiredFeature = "android.software.companion_device_setup";
                int numFeatures = ArrayUtils.size(reqFeatures);
                int i = 0;
                while (i < numFeatures) {
                    if (!requiredFeature.equals(reqFeatures[i].name)) {
                        i++;
                    } else {
                        return;
                    }
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Must declare uses-feature ");
                stringBuilder.append(requiredFeature);
                stringBuilder.append(" in manifest to use this API");
                throw new IllegalStateException(stringBuilder.toString());
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            new ShellCmd(CompanionDeviceManagerService.this, null).exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    private class ShellCmd extends ShellCommand {
        public static final String USAGE = "help\nlist USER_ID\nassociate USER_ID PACKAGE MAC_ADDRESS\ndisassociate USER_ID PACKAGE MAC_ADDRESS";

        private ShellCmd() {
        }

        /* synthetic */ ShellCmd(CompanionDeviceManagerService x0, AnonymousClass1 x1) {
            this();
        }

        /* JADX WARNING: Removed duplicated region for block: B:17:0x0038  */
        /* JADX WARNING: Removed duplicated region for block: B:21:0x0061  */
        /* JADX WARNING: Removed duplicated region for block: B:20:0x004f  */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x003d  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x0038  */
        /* JADX WARNING: Removed duplicated region for block: B:21:0x0061  */
        /* JADX WARNING: Removed duplicated region for block: B:20:0x004f  */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x003d  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x0038  */
        /* JADX WARNING: Removed duplicated region for block: B:21:0x0061  */
        /* JADX WARNING: Removed duplicated region for block: B:20:0x004f  */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x003d  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public int onCommand(String cmd) {
            int hashCode = cmd.hashCode();
            if (hashCode != 3322014) {
                if (hashCode != 784321104) {
                    if (hashCode == 1586499358 && cmd.equals("associate")) {
                        hashCode = 1;
                        switch (hashCode) {
                            case 0:
                                CollectionUtils.forEach(CompanionDeviceManagerService.this.readAllAssociations(getNextArgInt()), new -$$Lambda$CompanionDeviceManagerService$ShellCmd$spuk4wZBlDmxSJgcFgRkfptYY8g(this));
                                break;
                            case 1:
                                CompanionDeviceManagerService.this.addAssociation(getNextArgInt(), getNextArgRequired(), getNextArgRequired());
                                break;
                            case 2:
                                CompanionDeviceManagerService.this.removeAssociation(getNextArgInt(), getNextArgRequired(), getNextArgRequired());
                                break;
                            default:
                                return handleDefaultCommands(cmd);
                        }
                        return 0;
                    }
                } else if (cmd.equals("disassociate")) {
                    hashCode = 2;
                    switch (hashCode) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                        default:
                            break;
                    }
                    return 0;
                }
            } else if (cmd.equals("list")) {
                hashCode = 0;
                switch (hashCode) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    default:
                        break;
                }
                return 0;
            }
            hashCode = -1;
            switch (hashCode) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    break;
                default:
                    break;
            }
            return 0;
        }

        public static /* synthetic */ void lambda$onCommand$0(ShellCmd shellCmd, Association a) throws Exception {
            PrintWriter outPrintWriter = shellCmd.getOutPrintWriter();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(a.companionAppPackage);
            stringBuilder.append(" ");
            stringBuilder.append(a.deviceAddress);
            outPrintWriter.println(stringBuilder.toString());
        }

        private int getNextArgInt() {
            return Integer.parseInt(getNextArgRequired());
        }

        public void onHelp() {
            getOutPrintWriter().println(USAGE);
        }
    }

    public CompanionDeviceManagerService(Context context) {
        super(context);
        registerPackageMonitor();
    }

    private void registerPackageMonitor() {
        new PackageMonitor() {
            public void onPackageRemoved(String packageName, int uid) {
                CompanionDeviceManagerService.this.updateAssociations(new -$$Lambda$CompanionDeviceManagerService$1$EelUlD0Ldboon98oq6H5kDCPW9I(packageName), getChangingUserId());
            }

            public void onPackageModified(String packageName) {
                int userId = getChangingUserId();
                if (!ArrayUtils.isEmpty(CompanionDeviceManagerService.this.readAllAssociations(userId, packageName))) {
                    CompanionDeviceManagerService.this.updateSpecialAccessPermissionForAssociatedPackage(packageName, userId);
                }
            }
        }.register(getContext(), FgThread.get().getLooper(), UserHandle.ALL, true);
    }

    public void onStart() {
        publishBinderService("companiondevice", this.mImpl);
    }

    public void binderDied() {
        Handler.getMain().post(new -$$Lambda$CompanionDeviceManagerService$pG_kG2extKjHVEAFcCd4MLP2mkk(this));
    }

    private void cleanup() {
        synchronized (this.mLock) {
            this.mServiceConnection = unbind(this.mServiceConnection);
            this.mFindDeviceCallback = (IFindDeviceCallback) unlinkToDeath(this.mFindDeviceCallback, this, 0);
            this.mRequest = null;
            this.mCallingPackage = null;
        }
    }

    private static <T extends IInterface> T unlinkToDeath(T iinterface, DeathRecipient deathRecipient, int flags) {
        if (iinterface != null) {
            iinterface.asBinder().unlinkToDeath(deathRecipient, flags);
        }
        return null;
    }

    private ServiceConnection unbind(ServiceConnection conn) {
        if (conn != null) {
            getContext().unbindService(conn);
        }
        return null;
    }

    private static int getCallingUserId() {
        return UserHandle.getUserId(Binder.getCallingUid());
    }

    private static boolean isCallerSystem() {
        return Binder.getCallingUid() == 1000;
    }

    private ServiceConnection createServiceConnection(final AssociationRequest request, final IFindDeviceCallback findDeviceCallback, final String callingPackage) {
        this.mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                CompanionDeviceManagerService.this.mFindDeviceCallback = findDeviceCallback;
                CompanionDeviceManagerService.this.mRequest = request;
                CompanionDeviceManagerService.this.mCallingPackage = callingPackage;
                try {
                    CompanionDeviceManagerService.this.mFindDeviceCallback.asBinder().linkToDeath(CompanionDeviceManagerService.this, 0);
                    try {
                        ICompanionDeviceDiscoveryService.Stub.asInterface(service).startDiscovery(request, callingPackage, findDeviceCallback, CompanionDeviceManagerService.this.getServiceCallback());
                    } catch (RemoteException e) {
                        Log.e(CompanionDeviceManagerService.LOG_TAG, "Error while initiating device discovery", e);
                    }
                } catch (RemoteException e2) {
                    CompanionDeviceManagerService.this.cleanup();
                }
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };
        return this.mServiceConnection;
    }

    private ICompanionDeviceDiscoveryServiceCallback.Stub getServiceCallback() {
        return new ICompanionDeviceDiscoveryServiceCallback.Stub() {
            public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                try {
                    return super.onTransact(code, data, reply, flags);
                } catch (Throwable e) {
                    Slog.e(CompanionDeviceManagerService.LOG_TAG, "Error during IPC", e);
                    RuntimeException propagate = ExceptionUtils.propagate(e, RemoteException.class);
                }
            }

            public void onDeviceSelected(String packageName, int userId, String deviceAddress) {
                CompanionDeviceManagerService.this.addAssociation(userId, packageName, deviceAddress);
                CompanionDeviceManagerService.this.cleanup();
            }

            public void onDeviceSelectionCancel() {
                CompanionDeviceManagerService.this.cleanup();
            }
        };
    }

    void addAssociation(int userId, String packageName, String deviceAddress) {
        updateSpecialAccessPermissionForAssociatedPackage(packageName, userId);
        recordAssociation(packageName, deviceAddress);
    }

    void removeAssociation(int userId, String pkg, String deviceMacAddress) {
        updateAssociations(new -$$Lambda$CompanionDeviceManagerService$utOm0rPFb4x9GgnuV9fsUZ-eMfY(this, userId, deviceMacAddress, pkg));
    }

    private void updateSpecialAccessPermissionForAssociatedPackage(String packageName, int userId) {
        PackageInfo packageInfo = getPackageInfo(packageName, userId);
        if (packageInfo != null) {
            Binder.withCleanCallingIdentity(PooledLambda.obtainRunnable(-$$Lambda$CompanionDeviceManagerService$wnUkAY8uXyjMGM59-bNpzLLMJ1I.INSTANCE, this, packageInfo).recycleOnUse());
        }
    }

    private void updateSpecialAccessPermissionAsSystem(PackageInfo packageInfo) {
        try {
            if (containsEither(packageInfo.requestedPermissions, "android.permission.RUN_IN_BACKGROUND", "android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND")) {
                this.mIdleController.addPowerSaveWhitelistApp(packageInfo.packageName);
            } else {
                this.mIdleController.removePowerSaveWhitelistApp(packageInfo.packageName);
            }
        } catch (RemoteException e) {
        }
        NetworkPolicyManager networkPolicyManager = NetworkPolicyManager.from(getContext());
        if (containsEither(packageInfo.requestedPermissions, "android.permission.USE_DATA_IN_BACKGROUND", "android.permission.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND")) {
            networkPolicyManager.addUidPolicy(packageInfo.applicationInfo.uid, 4);
        } else {
            networkPolicyManager.removeUidPolicy(packageInfo.applicationInfo.uid, 4);
        }
    }

    private static <T> boolean containsEither(T[] array, T a, T b) {
        return ArrayUtils.contains(array, a) || ArrayUtils.contains(array, b);
    }

    private PackageInfo getPackageInfo(String packageName, int userId) {
        return (PackageInfo) Binder.withCleanCallingIdentity(PooledLambda.obtainSupplier(-$$Lambda$CompanionDeviceManagerService$0VKz9ecFqvfFXzRrfaz-Pf5wW2s.INSTANCE, getContext(), packageName, Integer.valueOf(userId)).recycleOnUse());
    }

    static /* synthetic */ PackageInfo lambda$getPackageInfo$1(Context context, String pkg, Integer id) {
        try {
            return context.getPackageManager().getPackageInfoAsUser(pkg, 20480, id.intValue());
        } catch (NameNotFoundException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to get PackageInfo for package ");
            stringBuilder.append(pkg);
            Slog.e(str, stringBuilder.toString(), e);
            return null;
        }
    }

    private void recordAssociation(String priviledgedPackage, String deviceAddress) {
        updateAssociations(new -$$Lambda$CompanionDeviceManagerService$pF7vjIJpy5wI-u498jmFdSjoS_0(this, getCallingUserId(), deviceAddress, priviledgedPackage));
    }

    private void updateAssociations(Function<Set<Association>, Set<Association>> update) {
        updateAssociations(update, getCallingUserId());
    }

    private void updateAssociations(Function<Set<Association>, Set<Association>> update, int userId) {
        AtomicFile file = getStorageFileForUser(userId);
        synchronized (file) {
            Set<Association> associations = readAllAssociations(userId);
            Set<Association> old = CollectionUtils.copyOf(associations);
            associations = (Set) update.apply(associations);
            if (CollectionUtils.size(old) == CollectionUtils.size(associations)) {
                return;
            }
            file.write(new -$$Lambda$CompanionDeviceManagerService$_wqnNKMj0AXNyFu-i6lXk6tA3xs(associations));
        }
    }

    static /* synthetic */ void lambda$updateAssociations$4(Set finalAssociations, FileOutputStream out) {
        XmlSerializer xml = Xml.newSerializer();
        try {
            xml.setOutput(out, StandardCharsets.UTF_8.name());
            xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            xml.startDocument(null, Boolean.valueOf(true));
            xml.startTag(null, XML_TAG_ASSOCIATIONS);
            CollectionUtils.forEach(finalAssociations, new -$$Lambda$CompanionDeviceManagerService$_WjcclQ59faBsgHHLmf5Dm8Zo8k(xml));
            xml.endTag(null, XML_TAG_ASSOCIATIONS);
            xml.endDocument();
        } catch (Exception e) {
            Slog.e(LOG_TAG, "Error while writing associations file", e);
            throw ExceptionUtils.propagate(e);
        }
    }

    private AtomicFile getStorageFileForUser(int uid) {
        return (AtomicFile) this.mUidToStorage.computeIfAbsent(Integer.valueOf(uid), -$$Lambda$CompanionDeviceManagerService$bh5xRJq9-CRJoXvmerYRNjK1xEQ.INSTANCE);
    }

    private Set<Association> readAllAssociations(int userId) {
        return readAllAssociations(userId, null);
    }

    /* JADX WARNING: Removed duplicated region for block: B:54:0x0097 A:{Splitter: B:48:0x008c, ExcHandler: org.xmlpull.v1.XmlPullParserException (e org.xmlpull.v1.XmlPullParserException)} */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x009c A:{Splitter: B:5:0x0018, ExcHandler: org.xmlpull.v1.XmlPullParserException (e org.xmlpull.v1.XmlPullParserException)} */
    /* JADX WARNING: Missing block: B:54:0x0097, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:55:0x0098, code:
            r4 = r8;
     */
    /* JADX WARNING: Missing block: B:57:0x009c, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:59:?, code:
            android.util.Slog.e(LOG_TAG, "Error while reading associations file", r0);
     */
    /* JADX WARNING: Missing block: B:61:0x00a5, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Set<Association> readAllAssociations(int userId, String packageFilter) {
        ArraySet<Association> result;
        FileInputStream in;
        ArraySet<Association> result2;
        Throwable th;
        Throwable th2;
        Throwable result3;
        ArraySet<Association> result4;
        String str = packageFilter;
        AtomicFile file = getStorageFileForUser(userId);
        if (!file.getBaseFile().exists()) {
            return null;
        }
        result = null;
        XmlPullParser parser = Xml.newPullParser();
        synchronized (file) {
            try {
                in = file.openRead();
                try {
                    parser.setInput(in, StandardCharsets.UTF_8.name());
                    while (true) {
                        int next = parser.next();
                        int type = next;
                        if (next == 1) {
                            break;
                        } else if (type == 2 || XML_TAG_ASSOCIATIONS.equals(parser.getName())) {
                            String appPackage = parser.getAttributeValue(null, "package");
                            String deviceAddress = parser.getAttributeValue(null, XML_ATTR_DEVICE);
                            if (appPackage != null) {
                                if (deviceAddress != null) {
                                    if (str == null || str.equals(appPackage)) {
                                        result = ArrayUtils.add(result, new Association(this, userId, deviceAddress, appPackage, null));
                                    }
                                }
                            }
                        }
                    }
                    if (in != null) {
                        in.close();
                    }
                } catch (Throwable th3) {
                    th2 = th3;
                    result4 = result2;
                    result3 = th;
                }
                try {
                    return result;
                } catch (Throwable th4) {
                    th2 = th4;
                    throw th2;
                }
            } catch (XmlPullParserException e) {
            }
        }
        throw th;
        th = th2;
        if (in != null) {
            if (result3 != null) {
                try {
                    in.close();
                } catch (Throwable th22) {
                    try {
                        result3.addSuppressed(th22);
                    } catch (XmlPullParserException e2) {
                    } catch (Throwable th5) {
                        th22 = th5;
                        result = result4;
                        throw th22;
                    }
                }
            }
            in.close();
        }
        throw th;
    }
}
