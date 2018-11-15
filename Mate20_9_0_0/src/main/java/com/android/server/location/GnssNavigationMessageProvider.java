package com.android.server.location;

import android.location.GnssNavigationMessage;
import android.location.IGnssNavigationMessageListener;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;

public abstract class GnssNavigationMessageProvider extends RemoteListenerHelper<IGnssNavigationMessageListener> {
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String TAG = "GnssNavigationMessageProvider";
    private boolean mCollectionStarted;
    private final GnssNavigationMessageProviderNative mNative;

    @VisibleForTesting
    static class GnssNavigationMessageProviderNative {
        GnssNavigationMessageProviderNative() {
        }

        public boolean isNavigationMessageSupported() {
            return GnssNavigationMessageProvider.native_is_navigation_message_supported();
        }

        public boolean startNavigationMessageCollection() {
            return GnssNavigationMessageProvider.native_start_navigation_message_collection();
        }

        public boolean stopNavigationMessageCollection() {
            return GnssNavigationMessageProvider.native_stop_navigation_message_collection();
        }
    }

    private static class StatusChangedOperation implements ListenerOperation<IGnssNavigationMessageListener> {
        private final int mStatus;

        public StatusChangedOperation(int status) {
            this.mStatus = status;
        }

        public void execute(IGnssNavigationMessageListener listener) throws RemoteException {
            listener.onStatusChanged(this.mStatus);
        }
    }

    private static native boolean native_is_navigation_message_supported();

    private static native boolean native_start_navigation_message_collection();

    private static native boolean native_stop_navigation_message_collection();

    protected GnssNavigationMessageProvider(Handler handler) {
        this(handler, new GnssNavigationMessageProviderNative());
    }

    @VisibleForTesting
    GnssNavigationMessageProvider(Handler handler, GnssNavigationMessageProviderNative aNative) {
        super(handler, TAG);
        this.mNative = aNative;
    }

    void resumeIfStarted() {
        if (DEBUG) {
            Log.d(TAG, "resumeIfStarted");
        }
        if (this.mCollectionStarted) {
            this.mNative.startNavigationMessageCollection();
        }
    }

    protected boolean isAvailableInPlatform() {
        return this.mNative.isNavigationMessageSupported();
    }

    protected int registerWithService() {
        if (!this.mNative.startNavigationMessageCollection()) {
            return 4;
        }
        this.mCollectionStarted = true;
        return 0;
    }

    protected void unregisterFromService() {
        if (this.mNative.stopNavigationMessageCollection()) {
            this.mCollectionStarted = false;
        }
    }

    public void onNavigationMessageAvailable(final GnssNavigationMessage event) {
        foreach(new ListenerOperation<IGnssNavigationMessageListener>() {
            public void execute(IGnssNavigationMessageListener listener) throws RemoteException {
                listener.onGnssNavigationMessageReceived(event);
            }
        });
    }

    public void onCapabilitiesUpdated(boolean isGnssNavigationMessageSupported) {
        setSupported(isGnssNavigationMessageSupported);
        updateResult();
    }

    public void onGpsEnabledChanged() {
        tryUpdateRegistrationWithService();
        updateResult();
    }

    protected ListenerOperation<IGnssNavigationMessageListener> getHandlerOperation(int result) {
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
