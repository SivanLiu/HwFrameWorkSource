package com.android.server.location;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.location.GeofenceHardwareService;
import android.hardware.location.IGeofenceHardware;
import android.location.IFusedGeofenceHardware;
import android.location.IGeofenceProvider;
import android.location.IGpsGeofenceHardware;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.FgThread;
import com.android.server.ServiceWatcher;

public final class GeofenceProxy {
    private static final String SERVICE_ACTION = "com.android.location.service.GeofenceProvider";
    private static final String TAG = "GeofenceProxy";
    private final Context mContext;
    /* access modifiers changed from: private */
    public final IFusedGeofenceHardware mFusedGeofenceHardware;
    /* access modifiers changed from: private */
    public volatile IGeofenceHardware mGeofenceHardware;
    /* access modifiers changed from: private */
    public final IGpsGeofenceHardware mGpsGeofenceHardware;
    /* access modifiers changed from: private */
    public final ServiceWatcher mServiceWatcher;
    /* access modifiers changed from: private */
    public final ServiceWatcher.BinderRunner mUpdateGeofenceHardware = new ServiceWatcher.BinderRunner() {
        /* class com.android.server.location.$$Lambda$GeofenceProxy$nfSKchjbT2ANT9GbYwyAcTjzBwQ */

        @Override // com.android.server.ServiceWatcher.BinderRunner
        public final void run(IBinder iBinder) {
            GeofenceProxy.this.lambda$new$0$GeofenceProxy(iBinder);
        }
    };

    public /* synthetic */ void lambda$new$0$GeofenceProxy(IBinder binder) throws RemoteException {
        try {
            IGeofenceProvider.Stub.asInterface(binder).setGeofenceHardware(this.mGeofenceHardware);
        } catch (RemoteException e) {
            Log.w(TAG, e);
        }
    }

    public static GeofenceProxy createAndBind(Context context, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, IGpsGeofenceHardware gpsGeofence, IFusedGeofenceHardware fusedGeofenceHardware) {
        GeofenceProxy proxy = new GeofenceProxy(context, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, gpsGeofence, fusedGeofenceHardware);
        if (proxy.bind()) {
            return proxy;
        }
        return null;
    }

    private GeofenceProxy(Context context, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, IGpsGeofenceHardware gpsGeofence, IFusedGeofenceHardware fusedGeofenceHardware) {
        this.mContext = context;
        this.mServiceWatcher = new ServiceWatcher(context, TAG, SERVICE_ACTION, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, FgThread.getHandler()) {
            /* class com.android.server.location.GeofenceProxy.AnonymousClass1 */

            /* access modifiers changed from: protected */
            @Override // com.android.server.ServiceWatcher
            public void onBind() {
                runOnBinder(GeofenceProxy.this.mUpdateGeofenceHardware);
            }
        };
        this.mGpsGeofenceHardware = gpsGeofence;
        this.mFusedGeofenceHardware = fusedGeofenceHardware;
        this.mGeofenceHardware = null;
    }

    private boolean bind() {
        if (!this.mServiceWatcher.start()) {
            return false;
        }
        Context context = this.mContext;
        context.bindServiceAsUser(new Intent(context, GeofenceHardwareService.class), new GeofenceProxyServiceConnection(), 1, UserHandle.SYSTEM);
        return true;
    }

    private class GeofenceProxyServiceConnection implements ServiceConnection {
        private GeofenceProxyServiceConnection() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            IGeofenceHardware geofenceHardware = IGeofenceHardware.Stub.asInterface(service);
            try {
                if (GeofenceProxy.this.mGpsGeofenceHardware != null) {
                    geofenceHardware.setGpsGeofenceHardware(GeofenceProxy.this.mGpsGeofenceHardware);
                }
                if (GeofenceProxy.this.mFusedGeofenceHardware != null) {
                    geofenceHardware.setFusedGeofenceHardware(GeofenceProxy.this.mFusedGeofenceHardware);
                }
                IGeofenceHardware unused = GeofenceProxy.this.mGeofenceHardware = geofenceHardware;
                GeofenceProxy.this.mServiceWatcher.runOnBinder(GeofenceProxy.this.mUpdateGeofenceHardware);
            } catch (Exception e) {
                Log.w(GeofenceProxy.TAG, e);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            IGeofenceHardware unused = GeofenceProxy.this.mGeofenceHardware = null;
            GeofenceProxy.this.mServiceWatcher.runOnBinder(GeofenceProxy.this.mUpdateGeofenceHardware);
        }
    }
}
