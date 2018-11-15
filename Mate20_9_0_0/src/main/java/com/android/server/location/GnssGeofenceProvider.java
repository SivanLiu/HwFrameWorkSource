package com.android.server.location;

import android.location.IGpsGeofenceHardware.Stub;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

class GnssGeofenceProvider extends Stub {
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String TAG = "GnssGeofenceProvider";
    private HandlerThread mGeoFenceThread;
    private final SparseArray<GeofenceEntry> mGeofenceEntries;
    private final Handler mHandler;
    private final GnssGeofenceProviderNative mNative;

    private static class GeofenceEntry {
        public int geofenceId;
        public int lastTransition;
        public double latitude;
        public double longitude;
        public int monitorTransitions;
        public int notificationResponsiveness;
        public boolean paused;
        public double radius;
        public int unknownTimer;

        private GeofenceEntry() {
        }
    }

    @VisibleForTesting
    static class GnssGeofenceProviderNative {
        GnssGeofenceProviderNative() {
        }

        public boolean isGeofenceSupported() {
            Log.d(GnssGeofenceProvider.TAG, "calling isGeofenceSupported");
            return GnssGeofenceProvider.native_is_geofence_supported();
        }

        public boolean addGeofence(int geofenceId, double latitude, double longitude, double radius, int lastTransition, int monitorTransitions, int notificationResponsiveness, int unknownTimer) {
            String str = GnssGeofenceProvider.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("calling addGeofence, geofenceId=");
            stringBuilder.append(geofenceId);
            Log.d(str, stringBuilder.toString());
            return GnssGeofenceProvider.native_add_geofence(geofenceId, latitude, longitude, radius, lastTransition, monitorTransitions, notificationResponsiveness, unknownTimer);
        }

        public boolean removeGeofence(int geofenceId) {
            String str = GnssGeofenceProvider.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("calling removeGeofence, geofenceId=");
            stringBuilder.append(geofenceId);
            Log.d(str, stringBuilder.toString());
            return GnssGeofenceProvider.native_remove_geofence(geofenceId);
        }

        public boolean resumeGeofence(int geofenceId, int transitions) {
            String str = GnssGeofenceProvider.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("calling resumeGeofence, geofenceId=");
            stringBuilder.append(geofenceId);
            Log.d(str, stringBuilder.toString());
            return GnssGeofenceProvider.native_resume_geofence(geofenceId, transitions);
        }

        public boolean pauseGeofence(int geofenceId) {
            String str = GnssGeofenceProvider.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("calling pauseGeofence, geofenceId=");
            stringBuilder.append(geofenceId);
            Log.d(str, stringBuilder.toString());
            return GnssGeofenceProvider.native_pause_geofence(geofenceId);
        }
    }

    private static native boolean native_add_geofence(int i, double d, double d2, double d3, int i2, int i3, int i4, int i5);

    private static native boolean native_is_geofence_supported();

    private static native boolean native_pause_geofence(int i);

    private static native boolean native_remove_geofence(int i);

    private static native boolean native_resume_geofence(int i, int i2);

    GnssGeofenceProvider(Looper looper) {
        this(looper, new GnssGeofenceProviderNative());
    }

    @VisibleForTesting
    GnssGeofenceProvider(Looper looper, GnssGeofenceProviderNative gnssGeofenceProviderNative) {
        this.mGeofenceEntries = new SparseArray();
        this.mGeoFenceThread = new HandlerThread("GeoFenceThread");
        this.mGeoFenceThread.start();
        this.mHandler = new Handler(this.mGeoFenceThread.getLooper());
        this.mNative = gnssGeofenceProviderNative;
    }

    void resumeIfStarted() {
        if (DEBUG) {
            Log.d(TAG, "resumeIfStarted");
        }
        this.mHandler.post(new -$$Lambda$GnssGeofenceProvider$x-gy6KDILxd4rIEjriAkYQ46QwA(this));
    }

    public static /* synthetic */ void lambda$resumeIfStarted$0(GnssGeofenceProvider gnssGeofenceProvider) {
        for (int i = 0; i < gnssGeofenceProvider.mGeofenceEntries.size(); i++) {
            GeofenceEntry entry = (GeofenceEntry) gnssGeofenceProvider.mGeofenceEntries.valueAt(i);
            if (gnssGeofenceProvider.mNative.addGeofence(entry.geofenceId, entry.latitude, entry.longitude, entry.radius, entry.lastTransition, entry.monitorTransitions, entry.notificationResponsiveness, entry.unknownTimer) && entry.paused) {
                gnssGeofenceProvider.mNative.pauseGeofence(entry.geofenceId);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x0015 A:{Splitter: B:1:0x000a, ExcHandler: java.lang.InterruptedException (r1_4 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:4:0x0015, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x0016, code:
            android.util.Log.e(TAG, "Failed running callable.", r1);
     */
    /* JADX WARNING: Missing block: B:6:0x001e, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean runOnHandlerThread(Callable<Boolean> callable) {
        FutureTask<Boolean> futureTask = new FutureTask(callable);
        this.mHandler.post(futureTask);
        try {
            return ((Boolean) futureTask.get()).booleanValue();
        } catch (Exception e) {
        }
    }

    public boolean isHardwareGeofenceSupported() {
        GnssGeofenceProviderNative gnssGeofenceProviderNative = this.mNative;
        Objects.requireNonNull(gnssGeofenceProviderNative);
        return runOnHandlerThread(new -$$Lambda$nmIoImstXHuMaecjUXtG9FcFizs(gnssGeofenceProviderNative));
    }

    public boolean addCircularHardwareGeofence(int geofenceId, double latitude, double longitude, double radius, int lastTransition, int monitorTransitions, int notificationResponsiveness, int unknownTimer) {
        return runOnHandlerThread(new -$$Lambda$GnssGeofenceProvider$n5osOgh5pgunifw_x5yjaRzShkA(this, geofenceId, latitude, longitude, radius, lastTransition, monitorTransitions, notificationResponsiveness, unknownTimer));
    }

    public static /* synthetic */ Boolean lambda$addCircularHardwareGeofence$1(GnssGeofenceProvider gnssGeofenceProvider, int geofenceId, double latitude, double longitude, double radius, int lastTransition, int monitorTransitions, int notificationResponsiveness, int unknownTimer) throws Exception {
        GnssGeofenceProvider gnssGeofenceProvider2 = gnssGeofenceProvider;
        int i = geofenceId;
        boolean added = gnssGeofenceProvider2.mNative.addGeofence(i, latitude, longitude, radius, lastTransition, monitorTransitions, notificationResponsiveness, unknownTimer);
        if (added) {
            GeofenceEntry entry = new GeofenceEntry();
            entry.geofenceId = i;
            entry.latitude = latitude;
            entry.longitude = longitude;
            entry.radius = radius;
            entry.lastTransition = lastTransition;
            entry.monitorTransitions = monitorTransitions;
            entry.notificationResponsiveness = notificationResponsiveness;
            entry.unknownTimer = unknownTimer;
            gnssGeofenceProvider2.mGeofenceEntries.put(i, entry);
        } else {
            double d = latitude;
            double d2 = longitude;
            double d3 = radius;
            int i2 = lastTransition;
            int i3 = monitorTransitions;
            int i4 = notificationResponsiveness;
            int i5 = unknownTimer;
        }
        return Boolean.valueOf(added);
    }

    public boolean removeHardwareGeofence(int geofenceId) {
        return runOnHandlerThread(new -$$Lambda$GnssGeofenceProvider$EVVg0uE1k4gFEkVWlkxnKMCHrGA(this, geofenceId));
    }

    public static /* synthetic */ Boolean lambda$removeHardwareGeofence$2(GnssGeofenceProvider gnssGeofenceProvider, int geofenceId) throws Exception {
        boolean removed = gnssGeofenceProvider.mNative.removeGeofence(geofenceId);
        if (removed) {
            gnssGeofenceProvider.mGeofenceEntries.remove(geofenceId);
        }
        return Boolean.valueOf(removed);
    }

    public boolean pauseHardwareGeofence(int geofenceId) {
        return runOnHandlerThread(new -$$Lambda$GnssGeofenceProvider$ZddVrECW8W1fDH3yk5jjvded6Rs(this, geofenceId));
    }

    public static /* synthetic */ Boolean lambda$pauseHardwareGeofence$3(GnssGeofenceProvider gnssGeofenceProvider, int geofenceId) throws Exception {
        boolean paused = gnssGeofenceProvider.mNative.pauseGeofence(geofenceId);
        if (paused) {
            GeofenceEntry entry = (GeofenceEntry) gnssGeofenceProvider.mGeofenceEntries.get(geofenceId);
            if (entry != null) {
                entry.paused = true;
            }
        }
        return Boolean.valueOf(paused);
    }

    public boolean resumeHardwareGeofence(int geofenceId, int monitorTransitions) {
        return runOnHandlerThread(new -$$Lambda$GnssGeofenceProvider$X5bvoYFvm378No3aV2K7Jynm32c(this, geofenceId, monitorTransitions));
    }

    public static /* synthetic */ Boolean lambda$resumeHardwareGeofence$4(GnssGeofenceProvider gnssGeofenceProvider, int geofenceId, int monitorTransitions) throws Exception {
        boolean resumed = gnssGeofenceProvider.mNative.resumeGeofence(geofenceId, monitorTransitions);
        if (resumed) {
            GeofenceEntry entry = (GeofenceEntry) gnssGeofenceProvider.mGeofenceEntries.get(geofenceId);
            if (entry != null) {
                entry.paused = false;
                entry.monitorTransitions = monitorTransitions;
            }
        }
        return Boolean.valueOf(resumed);
    }
}
