package com.android.server.backup;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.util.Preconditions;
import com.android.server.HwServiceFactory;
import com.android.server.Watchdog;
import com.android.server.backup.transport.OnTransportRegisteredListener;
import com.android.server.backup.transport.TransportClient;
import com.android.server.backup.transport.TransportClientManager;
import com.android.server.backup.transport.TransportNotAvailableException;
import com.android.server.backup.transport.TransportNotRegisteredException;
import com.android.server.backup.transport.TransportStats;
import com.android.server.rms.IHwIpcMonitor;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TransportManager {
    @VisibleForTesting
    public static final String SERVICE_ACTION_TRANSPORT_HOST = "android.backup.TRANSPORT_HOST";
    private static final String TAG = "BackupTransportManager";
    private IHwIpcMonitor mBackupIpcMonitor;
    private final Context mContext;
    @GuardedBy("mTransportLock")
    private volatile String mCurrentTransportName;
    private OnTransportRegisteredListener mOnTransportRegisteredListener = -$$Lambda$TransportManager$Z9ckpFUW2V4jkdHnyXIEiLuAoBc.INSTANCE;
    private final PackageManager mPackageManager;
    @GuardedBy("mTransportLock")
    private final Map<ComponentName, TransportDescription> mRegisteredTransportsDescriptionMap = new ArrayMap();
    private final TransportClientManager mTransportClientManager;
    private final Object mTransportLock = new Object();
    private final Intent mTransportServiceIntent = new Intent(SERVICE_ACTION_TRANSPORT_HOST);
    private final TransportStats mTransportStats;
    private final Set<ComponentName> mTransportWhitelist;

    private static class TransportDescription {
        private Intent configurationIntent;
        private String currentDestinationString;
        private Intent dataManagementIntent;
        private String dataManagementLabel;
        private String name;
        private final String transportDirName;

        private TransportDescription(String name, String transportDirName, Intent configurationIntent, String currentDestinationString, Intent dataManagementIntent, String dataManagementLabel) {
            this.name = name;
            this.transportDirName = transportDirName;
            this.configurationIntent = configurationIntent;
            this.currentDestinationString = currentDestinationString;
            this.dataManagementIntent = dataManagementIntent;
            this.dataManagementLabel = dataManagementLabel;
        }
    }

    static /* synthetic */ void lambda$new$0(String c, String n) {
    }

    TransportManager(Context context, Set<ComponentName> whitelist, String selectedTransport) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mTransportWhitelist = (Set) Preconditions.checkNotNull(whitelist);
        this.mCurrentTransportName = selectedTransport;
        this.mTransportStats = new TransportStats();
        this.mTransportClientManager = new TransportClientManager(context, this.mTransportStats);
    }

    @VisibleForTesting
    TransportManager(Context context, Set<ComponentName> whitelist, String selectedTransport, TransportClientManager transportClientManager) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mTransportWhitelist = (Set) Preconditions.checkNotNull(whitelist);
        this.mCurrentTransportName = selectedTransport;
        this.mTransportStats = new TransportStats();
        this.mTransportClientManager = transportClientManager;
        if (this.mBackupIpcMonitor == null) {
            this.mBackupIpcMonitor = HwServiceFactory.getIHwIpcMonitor(this.mTransportLock, HealthServiceWrapper.INSTANCE_HEALTHD, "backupTransport");
            if (this.mBackupIpcMonitor != null) {
                Watchdog.getInstance().addIpcMonitor(this.mBackupIpcMonitor);
            }
        }
    }

    public void setOnTransportRegisteredListener(OnTransportRegisteredListener listener) {
        this.mOnTransportRegisteredListener = listener;
    }

    void onPackageAdded(String packageName) {
        registerTransportsFromPackage(packageName, -$$Lambda$TransportManager$4ND1hZMerK5gHU67okq6DZjKDQw.INSTANCE);
    }

    void onPackageRemoved(String packageName) {
        synchronized (this.mTransportLock) {
            this.mRegisteredTransportsDescriptionMap.keySet().removeIf(fromPackageFilter(packageName));
        }
    }

    void onPackageChanged(String packageName, String... components) {
        Set<ComponentName> transportComponents = new ArraySet(components.length);
        for (String componentName : components) {
            transportComponents.add(new ComponentName(packageName, componentName));
        }
        synchronized (this.mTransportLock) {
            Set keySet = this.mRegisteredTransportsDescriptionMap.keySet();
            Objects.requireNonNull(transportComponents);
            keySet.removeIf(new -$$Lambda$-xfpm33S8Jqv3KpU_-llxhj8ZPI(transportComponents));
        }
        Objects.requireNonNull(transportComponents);
        registerTransportsFromPackage(packageName, new -$$Lambda$-xfpm33S8Jqv3KpU_-llxhj8ZPI(transportComponents));
    }

    ComponentName[] getRegisteredTransportComponents() {
        ComponentName[] componentNameArr;
        synchronized (this.mTransportLock) {
            componentNameArr = (ComponentName[]) this.mRegisteredTransportsDescriptionMap.keySet().toArray(new ComponentName[this.mRegisteredTransportsDescriptionMap.size()]);
        }
        return componentNameArr;
    }

    String[] getRegisteredTransportNames() {
        String[] transportNames;
        synchronized (this.mTransportLock) {
            transportNames = new String[this.mRegisteredTransportsDescriptionMap.size()];
            int i = 0;
            for (TransportDescription description : this.mRegisteredTransportsDescriptionMap.values()) {
                transportNames[i] = description.name;
                i++;
            }
        }
        return transportNames;
    }

    Set<ComponentName> getTransportWhitelist() {
        return this.mTransportWhitelist;
    }

    String getCurrentTransportName() {
        return this.mCurrentTransportName;
    }

    public String getTransportName(ComponentName transportComponent) throws TransportNotRegisteredException {
        String access$000;
        synchronized (this.mTransportLock) {
            access$000 = getRegisteredTransportDescriptionOrThrowLocked(transportComponent).name;
        }
        return access$000;
    }

    public String getTransportDirName(ComponentName transportComponent) throws TransportNotRegisteredException {
        String access$100;
        synchronized (this.mTransportLock) {
            access$100 = getRegisteredTransportDescriptionOrThrowLocked(transportComponent).transportDirName;
        }
        return access$100;
    }

    public String getTransportDirName(String transportName) throws TransportNotRegisteredException {
        String access$100;
        synchronized (this.mTransportLock) {
            access$100 = getRegisteredTransportDescriptionOrThrowLocked(transportName).transportDirName;
        }
        return access$100;
    }

    public Intent getTransportConfigurationIntent(String transportName) throws TransportNotRegisteredException {
        Intent access$200;
        synchronized (this.mTransportLock) {
            access$200 = getRegisteredTransportDescriptionOrThrowLocked(transportName).configurationIntent;
        }
        return access$200;
    }

    public String getTransportCurrentDestinationString(String transportName) throws TransportNotRegisteredException {
        String access$300;
        synchronized (this.mTransportLock) {
            access$300 = getRegisteredTransportDescriptionOrThrowLocked(transportName).currentDestinationString;
        }
        return access$300;
    }

    public Intent getTransportDataManagementIntent(String transportName) throws TransportNotRegisteredException {
        Intent access$400;
        synchronized (this.mTransportLock) {
            access$400 = getRegisteredTransportDescriptionOrThrowLocked(transportName).dataManagementIntent;
        }
        return access$400;
    }

    public String getTransportDataManagementLabel(String transportName) throws TransportNotRegisteredException {
        String access$500;
        synchronized (this.mTransportLock) {
            access$500 = getRegisteredTransportDescriptionOrThrowLocked(transportName).dataManagementLabel;
        }
        return access$500;
    }

    public boolean isTransportRegistered(String transportName) {
        boolean z;
        synchronized (this.mTransportLock) {
            z = getRegisteredTransportEntryLocked(transportName) != null;
        }
        return z;
    }

    public void forEachRegisteredTransport(Consumer<String> transportConsumer) {
        synchronized (this.mTransportLock) {
            for (TransportDescription transportDescription : this.mRegisteredTransportsDescriptionMap.values()) {
                transportConsumer.accept(transportDescription.name);
            }
        }
    }

    public void updateTransportAttributes(ComponentName transportComponent, String name, Intent configurationIntent, String currentDestinationString, Intent dataManagementIntent, String dataManagementLabel) {
        synchronized (this.mTransportLock) {
            TransportDescription description = (TransportDescription) this.mRegisteredTransportsDescriptionMap.get(transportComponent);
            String str;
            StringBuilder stringBuilder;
            if (description == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Transport ");
                stringBuilder.append(name);
                stringBuilder.append(" not registered tried to change description");
                Slog.e(str, stringBuilder.toString());
                return;
            }
            description.name = name;
            description.configurationIntent = configurationIntent;
            description.currentDestinationString = currentDestinationString;
            description.dataManagementIntent = dataManagementIntent;
            description.dataManagementLabel = dataManagementLabel;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Transport ");
            stringBuilder.append(name);
            stringBuilder.append(" updated its attributes");
            Slog.d(str, stringBuilder.toString());
        }
    }

    @GuardedBy("mTransportLock")
    private TransportDescription getRegisteredTransportDescriptionOrThrowLocked(ComponentName transportComponent) throws TransportNotRegisteredException {
        TransportDescription description = (TransportDescription) this.mRegisteredTransportsDescriptionMap.get(transportComponent);
        if (description != null) {
            return description;
        }
        throw new TransportNotRegisteredException(transportComponent);
    }

    @GuardedBy("mTransportLock")
    private TransportDescription getRegisteredTransportDescriptionOrThrowLocked(String transportName) throws TransportNotRegisteredException {
        TransportDescription description = getRegisteredTransportDescriptionLocked(transportName);
        if (description != null) {
            return description;
        }
        throw new TransportNotRegisteredException(transportName);
    }

    @GuardedBy("mTransportLock")
    private ComponentName getRegisteredTransportComponentLocked(String transportName) {
        Entry<ComponentName, TransportDescription> entry = getRegisteredTransportEntryLocked(transportName);
        return entry == null ? null : (ComponentName) entry.getKey();
    }

    @GuardedBy("mTransportLock")
    private TransportDescription getRegisteredTransportDescriptionLocked(String transportName) {
        Entry<ComponentName, TransportDescription> entry = getRegisteredTransportEntryLocked(transportName);
        return entry == null ? null : (TransportDescription) entry.getValue();
    }

    @GuardedBy("mTransportLock")
    private Entry<ComponentName, TransportDescription> getRegisteredTransportEntryLocked(String transportName) {
        for (Entry<ComponentName, TransportDescription> entry : this.mRegisteredTransportsDescriptionMap.entrySet()) {
            if (transportName.equals(((TransportDescription) entry.getValue()).name)) {
                return entry;
            }
        }
        return null;
    }

    public TransportClient getTransportClient(String transportName, String caller) {
        try {
            return getTransportClientOrThrow(transportName, caller);
        } catch (TransportNotRegisteredException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Transport ");
            stringBuilder.append(transportName);
            stringBuilder.append(" not registered");
            Slog.w(str, stringBuilder.toString());
            return null;
        }
    }

    public TransportClient getTransportClientOrThrow(String transportName, String caller) throws TransportNotRegisteredException {
        TransportClient transportClient;
        synchronized (this.mTransportLock) {
            ComponentName component = getRegisteredTransportComponentLocked(transportName);
            if (component != null) {
                transportClient = this.mTransportClientManager.getTransportClient(component, caller);
            } else {
                throw new TransportNotRegisteredException(transportName);
            }
        }
        return transportClient;
    }

    public TransportClient getCurrentTransportClient(String caller) {
        TransportClient transportClient;
        synchronized (this.mTransportLock) {
            transportClient = getTransportClient(this.mCurrentTransportName, caller);
        }
        return transportClient;
    }

    public TransportClient getCurrentTransportClientOrThrow(String caller) throws TransportNotRegisteredException {
        TransportClient transportClientOrThrow;
        synchronized (this.mTransportLock) {
            transportClientOrThrow = getTransportClientOrThrow(this.mCurrentTransportName, caller);
        }
        return transportClientOrThrow;
    }

    public void disposeOfTransportClient(TransportClient transportClient, String caller) {
        this.mTransportClientManager.disposeOfTransportClient(transportClient, caller);
    }

    @Deprecated
    String selectTransport(String transportName) {
        String prevTransport;
        synchronized (this.mTransportLock) {
            prevTransport = this.mCurrentTransportName;
            this.mCurrentTransportName = transportName;
        }
        return prevTransport;
    }

    public int registerAndSelectTransport(ComponentName transportComponent) {
        synchronized (this.mTransportLock) {
            try {
                selectTransport(getTransportName(transportComponent));
            } catch (TransportNotRegisteredException e) {
                int result = registerTransport(transportComponent);
                if (result != 0) {
                    return result;
                }
                synchronized (this.mTransportLock) {
                    try {
                        selectTransport(getTransportName(transportComponent));
                        return 0;
                    } catch (TransportNotRegisteredException e2) {
                        Slog.wtf(TAG, "Transport got unregistered");
                        return -1;
                    }
                }
            } catch (Throwable th) {
            }
        }
        return 0;
    }

    public void registerTransports() {
        registerTransportsForIntent(this.mTransportServiceIntent, -$$Lambda$TransportManager$Qbutmzd17ICwZdy0UzRrO-3_VK0.INSTANCE);
    }

    private void registerTransportsFromPackage(String packageName, Predicate<ComponentName> transportComponentFilter) {
        try {
            this.mPackageManager.getPackageInfo(packageName, 0);
            registerTransportsForIntent(new Intent(this.mTransportServiceIntent).setPackage(packageName), transportComponentFilter.and(fromPackageFilter(packageName)));
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Trying to register transports from package not found ");
            stringBuilder.append(packageName);
            Slog.e(str, stringBuilder.toString());
        }
    }

    private void registerTransportsForIntent(Intent intent, Predicate<ComponentName> transportComponentFilter) {
        List<ResolveInfo> hosts = this.mPackageManager.queryIntentServicesAsUser(intent, 0, 0);
        if (hosts != null) {
            for (ResolveInfo host : hosts) {
                ComponentName transportComponent = host.serviceInfo.getComponentName();
                if (transportComponentFilter.test(transportComponent) && isTransportTrusted(transportComponent)) {
                    registerTransport(transportComponent);
                }
            }
        }
    }

    private boolean isTransportTrusted(ComponentName transport) {
        if (this.mTransportWhitelist.contains(transport)) {
            try {
                if ((this.mPackageManager.getPackageInfo(transport.getPackageName(), 0).applicationInfo.privateFlags & 8) != 0) {
                    return true;
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Transport package ");
                stringBuilder.append(transport.getPackageName());
                stringBuilder.append(" not privileged");
                Slog.w(str, stringBuilder.toString());
                return false;
            } catch (NameNotFoundException e) {
                Slog.w(TAG, "Package not found.", e);
                return false;
            }
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("BackupTransport ");
        stringBuilder2.append(transport.flattenToShortString());
        stringBuilder2.append(" not whitelisted.");
        Slog.w(str2, stringBuilder2.toString());
        return false;
    }

    private int registerTransport(ComponentName transportComponent) {
        checkCanUseTransport();
        if (!isTransportTrusted(transportComponent)) {
            return -2;
        }
        String transportString = transportComponent.flattenToShortString();
        String callerLogString = "TransportManager.registerTransport()";
        Bundle extras = new Bundle();
        extras.putBoolean("android.app.backup.extra.TRANSPORT_REGISTRATION", true);
        TransportClient transportClient = this.mTransportClientManager.getTransportClient(transportComponent, extras, callerLogString);
        int result = -1;
        String transportName;
        try {
            IBackupTransport transport = transportClient.connectOrThrow(callerLogString);
            String transportDirName;
            try {
                transportName = transport.name();
                transportDirName = transport.transportDirName();
                registerTransport(transportComponent, transport);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Transport ");
                stringBuilder.append(transportString);
                stringBuilder.append(" registered");
                Slog.d(str, stringBuilder.toString());
                this.mOnTransportRegisteredListener.onTransportRegistered(transportName, transportDirName);
                result = 0;
            } catch (RemoteException e) {
                transportDirName = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Transport ");
                stringBuilder2.append(transportString);
                stringBuilder2.append(" died while registering");
                Slog.e(transportDirName, stringBuilder2.toString());
            }
            this.mTransportClientManager.disposeOfTransportClient(transportClient, callerLogString);
            return result;
        } catch (TransportNotAvailableException e2) {
            transportName = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Couldn't connect to transport ");
            stringBuilder3.append(transportString);
            stringBuilder3.append(" for registration");
            Slog.e(transportName, stringBuilder3.toString());
            this.mTransportClientManager.disposeOfTransportClient(transportClient, callerLogString);
            return -1;
        }
    }

    private void registerTransport(ComponentName transportComponent, IBackupTransport transport) throws RemoteException {
        checkCanUseTransport();
        TransportDescription description = new TransportDescription(transport.name(), transport.transportDirName(), transport.configurationIntent(), transport.currentDestinationString(), transport.dataManagementIntent(), transport.dataManagementLabel());
        synchronized (this.mTransportLock) {
            this.mRegisteredTransportsDescriptionMap.put(transportComponent, description);
        }
    }

    private void checkCanUseTransport() {
        Preconditions.checkState(Thread.holdsLock(this.mTransportLock) ^ 1, "Can't call transport with transport lock held");
    }

    public void dumpTransportClients(PrintWriter pw) {
        this.mTransportClientManager.dump(pw);
    }

    public void dumpTransportStats(PrintWriter pw) {
        this.mTransportStats.dump(pw);
    }

    private static Predicate<ComponentName> fromPackageFilter(String packageName) {
        return new -$$Lambda$TransportManager$_dxJobf45tWiMkaNlKY-z26kB2Q(packageName);
    }
}
