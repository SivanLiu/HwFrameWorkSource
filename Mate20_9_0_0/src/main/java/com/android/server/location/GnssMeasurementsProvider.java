package com.android.server.location;

import android.content.Context;
import android.location.GnssMeasurementsEvent;
import android.location.IGnssMeasurementsListener;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;

public abstract class GnssMeasurementsProvider extends RemoteListenerHelper<IGnssMeasurementsListener> {
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String TAG = "GnssMeasurementsProvider";
    private final Context mContext;
    private boolean mEnableFullTracking;
    private boolean mIsCollectionStarted;
    private final GnssMeasurementProviderNative mNative;

    @VisibleForTesting
    static class GnssMeasurementProviderNative {
        GnssMeasurementProviderNative() {
        }

        public boolean isMeasurementSupported() {
            return GnssMeasurementsProvider.native_is_measurement_supported();
        }

        public boolean startMeasurementCollection(boolean enableFullTracking) {
            return GnssMeasurementsProvider.native_start_measurement_collection(enableFullTracking);
        }

        public boolean stopMeasurementCollection() {
            return GnssMeasurementsProvider.native_stop_measurement_collection();
        }
    }

    private static class StatusChangedOperation implements ListenerOperation<IGnssMeasurementsListener> {
        private final int mStatus;

        public StatusChangedOperation(int status) {
            this.mStatus = status;
        }

        public void execute(IGnssMeasurementsListener listener) throws RemoteException {
            listener.onStatusChanged(this.mStatus);
        }
    }

    private static native boolean native_is_measurement_supported();

    private static native boolean native_start_measurement_collection(boolean z);

    private static native boolean native_stop_measurement_collection();

    protected GnssMeasurementsProvider(Context context, Handler handler) {
        this(context, handler, new GnssMeasurementProviderNative());
    }

    @VisibleForTesting
    GnssMeasurementsProvider(Context context, Handler handler, GnssMeasurementProviderNative aNative) {
        super(handler, TAG);
        this.mContext = context;
        this.mNative = aNative;
    }

    void resumeIfStarted() {
        if (DEBUG) {
            Log.d(TAG, "resumeIfStarted");
        }
        if (this.mIsCollectionStarted) {
            this.mNative.startMeasurementCollection(this.mEnableFullTracking);
        }
    }

    public boolean isAvailableInPlatform() {
        return this.mNative.isMeasurementSupported();
    }

    protected int registerWithService() {
        boolean enableFullTracking = Secure.getInt(this.mContext.getContentResolver(), "development_settings_enabled", 0) == 1 && Global.getInt(this.mContext.getContentResolver(), "enable_gnss_raw_meas_full_tracking", 0) == 1;
        if (!this.mNative.startMeasurementCollection(enableFullTracking)) {
            return 4;
        }
        this.mIsCollectionStarted = true;
        this.mEnableFullTracking = enableFullTracking;
        return 0;
    }

    protected void unregisterFromService() {
        if (this.mNative.stopMeasurementCollection()) {
            this.mIsCollectionStarted = false;
        }
    }

    public void onMeasurementsAvailable(GnssMeasurementsEvent event) {
        foreach(new -$$Lambda$GnssMeasurementsProvider$865xzodmeiSeR2xhh7cKZjiZkhE(event));
    }

    public void onCapabilitiesUpdated(boolean isGnssMeasurementsSupported) {
        setSupported(isGnssMeasurementsSupported);
        updateResult();
    }

    public void onGpsEnabledChanged() {
        tryUpdateRegistrationWithService();
        updateResult();
    }

    protected ListenerOperation<IGnssMeasurementsListener> getHandlerOperation(int result) {
        int status;
        switch (result) {
            case 0:
                status = 1;
                break;
            case 1:
            case 2:
            case 4:
                status = 0;
                break;
            case 3:
                status = 2;
                break;
            case 5:
                return null;
            case 6:
                status = 3;
                break;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unhandled addListener result: ");
                stringBuilder.append(result);
                Log.v(str, stringBuilder.toString());
                return null;
        }
        return new StatusChangedOperation(status);
    }
}
