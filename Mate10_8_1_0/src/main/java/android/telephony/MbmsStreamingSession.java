package android.telephony;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.RemoteException;
import android.telephony.mbms.InternalStreamingServiceCallback;
import android.telephony.mbms.InternalStreamingSessionCallback;
import android.telephony.mbms.MbmsStreamingSessionCallback;
import android.telephony.mbms.MbmsUtils;
import android.telephony.mbms.StreamingService;
import android.telephony.mbms.StreamingServiceCallback;
import android.telephony.mbms.StreamingServiceInfo;
import android.telephony.mbms.vendor.IMbmsStreamingService;
import android.telephony.mbms.vendor.IMbmsStreamingService.Stub;
import android.util.ArraySet;
import android.util.Log;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MbmsStreamingSession implements AutoCloseable {
    private static final String LOG_TAG = "MbmsStreamingSession";
    public static final String MBMS_STREAMING_SERVICE_ACTION = "android.telephony.action.EmbmsStreaming";
    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);
    private final Context mContext;
    private DeathRecipient mDeathRecipient = new DeathRecipient() {
        public void binderDied() {
            MbmsStreamingSession.sIsInitialized.set(false);
            MbmsStreamingSession.this.sendErrorToApp(3, "Received death notification");
        }
    };
    private InternalStreamingSessionCallback mInternalCallback;
    private Set<StreamingService> mKnownActiveStreamingServices = new ArraySet();
    private AtomicReference<IMbmsStreamingService> mService = new AtomicReference(null);
    private int mSubscriptionId = -1;

    private MbmsStreamingSession(Context context, MbmsStreamingSessionCallback callback, int subscriptionId, Handler handler) {
        this.mContext = context;
        this.mSubscriptionId = subscriptionId;
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        this.mInternalCallback = new InternalStreamingSessionCallback(callback, handler);
    }

    public static MbmsStreamingSession create(Context context, final MbmsStreamingSessionCallback callback, int subscriptionId, Handler handler) {
        if (sIsInitialized.compareAndSet(false, true)) {
            MbmsStreamingSession session = new MbmsStreamingSession(context, callback, subscriptionId, handler);
            final int result = session.bindAndInitialize();
            if (result == 0) {
                return session;
            }
            sIsInitialized.set(false);
            handler.post(new Runnable() {
                public void run() {
                    callback.onError(result, null);
                }
            });
            return null;
        }
        throw new IllegalStateException("Cannot create two instances of MbmsStreamingSession");
    }

    public static MbmsStreamingSession create(Context context, MbmsStreamingSessionCallback callback, Handler handler) {
        return create(context, callback, SubscriptionManager.getDefaultSubscriptionId(), handler);
    }

    public void close() {
        try {
            IMbmsStreamingService streamingService = (IMbmsStreamingService) this.mService.get();
            if (streamingService != null) {
                streamingService.dispose(this.mSubscriptionId);
                for (StreamingService s : this.mKnownActiveStreamingServices) {
                    s.getCallback().stop();
                }
                this.mKnownActiveStreamingServices.clear();
                this.mService.set(null);
                sIsInitialized.set(false);
                this.mInternalCallback.stop();
            }
        } catch (RemoteException e) {
        } finally {
            this.mService.set(null);
            sIsInitialized.set(false);
            this.mInternalCallback.stop();
        }
    }

    public void requestUpdateStreamingServices(List<String> serviceClassList) {
        IMbmsStreamingService streamingService = (IMbmsStreamingService) this.mService.get();
        if (streamingService == null) {
            throw new IllegalStateException("Middleware not yet bound");
        }
        try {
            int returnCode = streamingService.requestUpdateStreamingServices(this.mSubscriptionId, serviceClassList);
            if (returnCode != 0) {
                sendErrorToApp(returnCode, null);
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Remote process died");
            this.mService.set(null);
            sIsInitialized.set(false);
            sendErrorToApp(3, null);
        }
    }

    public StreamingService startStreaming(StreamingServiceInfo serviceInfo, StreamingServiceCallback callback, Handler handler) {
        IMbmsStreamingService streamingService = (IMbmsStreamingService) this.mService.get();
        if (streamingService == null) {
            throw new IllegalStateException("Middleware not yet bound");
        }
        InternalStreamingServiceCallback serviceCallback = new InternalStreamingServiceCallback(callback, handler);
        StreamingService serviceForApp = new StreamingService(this.mSubscriptionId, streamingService, this, serviceInfo, serviceCallback);
        this.mKnownActiveStreamingServices.add(serviceForApp);
        try {
            int returnCode = streamingService.startStreaming(this.mSubscriptionId, serviceInfo.getServiceId(), serviceCallback);
            if (returnCode == 0) {
                return serviceForApp;
            }
            sendErrorToApp(returnCode, null);
            return null;
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Remote process died");
            this.mService.set(null);
            sIsInitialized.set(false);
            sendErrorToApp(3, null);
            return null;
        }
    }

    public void onStreamingServiceStopped(StreamingService service) {
        this.mKnownActiveStreamingServices.remove(service);
    }

    private int bindAndInitialize() {
        return MbmsUtils.startBinding(this.mContext, MBMS_STREAMING_SERVICE_ACTION, new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                IMbmsStreamingService streamingService = Stub.asInterface(service);
                try {
                    int result = streamingService.initialize(MbmsStreamingSession.this.mInternalCallback, MbmsStreamingSession.this.mSubscriptionId);
                    if (result != 0) {
                        MbmsStreamingSession.this.sendErrorToApp(result, "Error returned during initialization");
                        MbmsStreamingSession.sIsInitialized.set(false);
                        return;
                    }
                    try {
                        streamingService.asBinder().linkToDeath(MbmsStreamingSession.this.mDeathRecipient, 0);
                        MbmsStreamingSession.this.mService.set(streamingService);
                    } catch (RemoteException e) {
                        MbmsStreamingSession.this.sendErrorToApp(3, "Middleware lost during initialization");
                        MbmsStreamingSession.sIsInitialized.set(false);
                    }
                } catch (RemoteException e2) {
                    Log.e(MbmsStreamingSession.LOG_TAG, "Service died before initialization");
                    MbmsStreamingSession.this.sendErrorToApp(103, e2.toString());
                    MbmsStreamingSession.sIsInitialized.set(false);
                } catch (RuntimeException e3) {
                    Log.e(MbmsStreamingSession.LOG_TAG, "Runtime exception during initialization");
                    MbmsStreamingSession.this.sendErrorToApp(103, e3.toString());
                    MbmsStreamingSession.sIsInitialized.set(false);
                }
            }

            public void onServiceDisconnected(ComponentName name) {
                MbmsStreamingSession.sIsInitialized.set(false);
                MbmsStreamingSession.this.mService.set(null);
            }
        });
    }

    private void sendErrorToApp(int errorCode, String message) {
        try {
            this.mInternalCallback.onError(errorCode, message);
        } catch (RemoteException e) {
        }
    }
}
