package com.android.location.provider;

import android.location.ILocationManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.WorkSource;
import android.util.Log;
import com.android.internal.location.ILocationProvider;
import com.android.internal.location.ILocationProviderManager;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public abstract class LocationProviderBase {
    public static final String EXTRA_NO_GPS_LOCATION = "noGPSLocation";
    public static final String FUSED_PROVIDER = "fused";
    /* access modifiers changed from: private */
    public final ArrayList<String> mAdditionalProviderPackages;
    /* access modifiers changed from: private */
    public final IBinder mBinder = new Service();
    /* access modifiers changed from: private */
    public volatile boolean mEnabled;
    @Deprecated
    protected final ILocationManager mLocationManager = ILocationManager.Stub.asInterface(ServiceManager.getService("location"));
    /* access modifiers changed from: private */
    public volatile ILocationProviderManager mManager = null;
    /* access modifiers changed from: private */
    public volatile ProviderProperties mProperties;
    /* access modifiers changed from: private */
    public final String mTag;

    /* access modifiers changed from: protected */
    public abstract void onSetRequest(ProviderRequestUnbundled providerRequestUnbundled, WorkSource workSource);

    /* JADX WARN: Type inference failed for: r0v0, types: [com.android.location.provider.LocationProviderBase$Service, android.os.IBinder] */
    public LocationProviderBase(String tag, ProviderPropertiesUnbundled properties) {
        this.mTag = tag;
        this.mProperties = properties.getProviderProperties();
        this.mEnabled = true;
        this.mAdditionalProviderPackages = new ArrayList<>(0);
    }

    public IBinder getBinder() {
        return this.mBinder;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x000e, code lost:
        if (r0 == null) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:?, code lost:
        r0.onSetEnabled(r3.mEnabled);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0016, code lost:
        r1 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0017, code lost:
        android.util.Log.w(r3.mTag, r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x000c, code lost:
        r0 = r3.mManager;
     */
    public void setEnabled(boolean enabled) {
        synchronized (this.mBinder) {
            if (this.mEnabled != enabled) {
                this.mEnabled = enabled;
            }
        }
    }

    public void setProperties(ProviderPropertiesUnbundled properties) {
        synchronized (this.mBinder) {
            this.mProperties = properties.getProviderProperties();
        }
        ILocationProviderManager manager = this.mManager;
        if (manager != null) {
            try {
                manager.onSetProperties(this.mProperties);
            } catch (RemoteException | RuntimeException e) {
                Log.w(this.mTag, e);
            }
        }
    }

    public void setAdditionalProviderPackages(List<String> packageNames) {
        synchronized (this.mBinder) {
            this.mAdditionalProviderPackages.clear();
            this.mAdditionalProviderPackages.addAll(packageNames);
        }
        ILocationProviderManager manager = this.mManager;
        if (manager != null) {
            try {
                manager.onSetAdditionalProviderPackages(this.mAdditionalProviderPackages);
            } catch (RemoteException | RuntimeException e) {
                Log.w(this.mTag, e);
            }
        }
    }

    public boolean isEnabled() {
        return this.mEnabled;
    }

    public void reportLocation(Location location) {
        ILocationProviderManager manager = this.mManager;
        if (manager != null) {
            try {
                manager.onReportLocation(location);
            } catch (RemoteException | RuntimeException e) {
                Log.w(this.mTag, e);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onInit() {
        onEnable();
    }

    /* access modifiers changed from: protected */
    @Deprecated
    public void onEnable() {
    }

    /* access modifiers changed from: protected */
    @Deprecated
    public void onDisable() {
    }

    /* access modifiers changed from: protected */
    @Deprecated
    public void onDump(FileDescriptor fd, PrintWriter pw, String[] args) {
    }

    /* access modifiers changed from: protected */
    @Deprecated
    public int onGetStatus(Bundle extras) {
        return 2;
    }

    /* access modifiers changed from: protected */
    @Deprecated
    public long onGetStatusUpdateTime() {
        return 0;
    }

    /* access modifiers changed from: protected */
    public boolean onSendExtraCommand(String command, Bundle extras) {
        return false;
    }

    private final class Service extends ILocationProvider.Stub {
        private Service() {
        }

        public void setLocationProviderManager(ILocationProviderManager manager) {
            synchronized (LocationProviderBase.this.mBinder) {
                try {
                    if (!LocationProviderBase.this.mAdditionalProviderPackages.isEmpty()) {
                        manager.onSetAdditionalProviderPackages(LocationProviderBase.this.mAdditionalProviderPackages);
                    }
                    manager.onSetProperties(LocationProviderBase.this.mProperties);
                    manager.onSetEnabled(LocationProviderBase.this.mEnabled);
                } catch (RemoteException e) {
                    Log.w(LocationProviderBase.this.mTag, e);
                }
                ILocationProviderManager unused = LocationProviderBase.this.mManager = manager;
            }
            LocationProviderBase.this.onInit();
        }

        public void setRequest(ProviderRequest request, WorkSource ws) {
            LocationProviderBase.this.onSetRequest(new ProviderRequestUnbundled(request), ws);
        }

        public int getStatus(Bundle extras) {
            return LocationProviderBase.this.onGetStatus(extras);
        }

        public long getStatusUpdateTime() {
            return LocationProviderBase.this.onGetStatusUpdateTime();
        }

        public void sendExtraCommand(String command, Bundle extras) {
            LocationProviderBase.this.onSendExtraCommand(command, extras);
        }
    }
}
