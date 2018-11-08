package android.telephony.mbms.vendor;

import android.net.Uri;
import android.os.Binder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.telephony.mbms.IMbmsStreamingSessionCallback;
import android.telephony.mbms.IStreamingServiceCallback;
import android.telephony.mbms.MbmsStreamingSessionCallback;
import android.telephony.mbms.StreamingServiceCallback;
import android.telephony.mbms.StreamingServiceInfo;
import android.telephony.mbms.vendor.IMbmsStreamingService.Stub;
import java.util.List;

public class MbmsStreamingServiceBase extends Stub {
    public int initialize(MbmsStreamingSessionCallback callback, int subscriptionId) throws RemoteException {
        return 0;
    }

    public final int initialize(final IMbmsStreamingSessionCallback callback, final int subscriptionId) throws RemoteException {
        final int uid = Binder.getCallingUid();
        callback.asBinder().linkToDeath(new DeathRecipient() {
            public void binderDied() {
                MbmsStreamingServiceBase.this.onAppCallbackDied(uid, subscriptionId);
            }
        }, 0);
        return initialize(new MbmsStreamingSessionCallback() {
            public void onError(int errorCode, String message) {
                try {
                    callback.onError(errorCode, message);
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(uid, subscriptionId);
                }
            }

            public void onStreamingServicesUpdated(List<StreamingServiceInfo> services) {
                try {
                    callback.onStreamingServicesUpdated(services);
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(uid, subscriptionId);
                }
            }

            public void onMiddlewareReady() {
                try {
                    callback.onMiddlewareReady();
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(uid, subscriptionId);
                }
            }
        }, subscriptionId);
    }

    public int requestUpdateStreamingServices(int subscriptionId, List<String> list) throws RemoteException {
        return 0;
    }

    public int startStreaming(int subscriptionId, String serviceId, StreamingServiceCallback callback) throws RemoteException {
        return 0;
    }

    public int startStreaming(final int subscriptionId, String serviceId, final IStreamingServiceCallback callback) throws RemoteException {
        final int uid = Binder.getCallingUid();
        callback.asBinder().linkToDeath(new DeathRecipient() {
            public void binderDied() {
                MbmsStreamingServiceBase.this.onAppCallbackDied(uid, subscriptionId);
            }
        }, 0);
        return startStreaming(subscriptionId, serviceId, new StreamingServiceCallback() {
            public void onError(int errorCode, String message) {
                try {
                    callback.onError(errorCode, message);
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(uid, subscriptionId);
                }
            }

            public void onStreamStateUpdated(int state, int reason) {
                try {
                    callback.onStreamStateUpdated(state, reason);
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(uid, subscriptionId);
                }
            }

            public void onMediaDescriptionUpdated() {
                try {
                    callback.onMediaDescriptionUpdated();
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(uid, subscriptionId);
                }
            }

            public void onBroadcastSignalStrengthUpdated(int signalStrength) {
                try {
                    callback.onBroadcastSignalStrengthUpdated(signalStrength);
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(uid, subscriptionId);
                }
            }

            public void onStreamMethodUpdated(int methodType) {
                try {
                    callback.onStreamMethodUpdated(methodType);
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(uid, subscriptionId);
                }
            }
        });
    }

    public Uri getPlaybackUri(int subscriptionId, String serviceId) throws RemoteException {
        return null;
    }

    public void stopStreaming(int subscriptionId, String serviceId) throws RemoteException {
    }

    public void dispose(int subscriptionId) throws RemoteException {
    }

    public void onAppCallbackDied(int uid, int subscriptionId) {
    }
}
