package com.android.internal.telephony.imsphone;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telecom.VideoProfile;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsExternalCallState;
import android.util.ArrayMap;
import android.util.Log;
import com.android.ims.ImsExternalCallStateListener;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.PhoneConstants.State;
import com.android.internal.telephony.imsphone.ImsExternalConnection.Listener;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker.PhoneStateListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ImsExternalCallTracker implements PhoneStateListener {
    private static final int EVENT_VIDEO_CAPABILITIES_CHANGED = 1;
    public static final String EXTRA_IMS_EXTERNAL_CALL_ID = "android.telephony.ImsExternalCallTracker.extra.EXTERNAL_CALL_ID";
    public static final String TAG = "ImsExternalCallTracker";
    private ImsPullCall mCallPuller;
    private final ImsCallNotify mCallStateNotifier;
    private Map<Integer, Boolean> mExternalCallPullableState = new ArrayMap();
    private final ExternalCallStateListener mExternalCallStateListener;
    private final ExternalConnectionListener mExternalConnectionListener = new ExternalConnectionListener();
    private Map<Integer, ImsExternalConnection> mExternalConnections = new ArrayMap();
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                ImsExternalCallTracker.this.handleVideoCapabilitiesChanged((AsyncResult) msg.obj);
            }
        }
    };
    private boolean mHasActiveCalls;
    private boolean mIsVideoCapable;
    private final ImsPhone mPhone;

    public class ExternalCallStateListener extends ImsExternalCallStateListener {
        public void onImsExternalCallStateUpdate(List<ImsExternalCallState> externalCallState) {
            ImsExternalCallTracker.this.refreshExternalCallState(externalCallState);
        }
    }

    public interface ImsCallNotify {
        void notifyPreciseCallStateChanged();

        void notifyUnknownConnection(Connection connection);
    }

    public class ExternalConnectionListener implements Listener {
        public void onPullExternalCall(ImsExternalConnection connection) {
            String str = ImsExternalCallTracker.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onPullExternalCall: connection = ");
            stringBuilder.append(connection);
            Log.d(str, stringBuilder.toString());
            if (ImsExternalCallTracker.this.mCallPuller == null) {
                Log.e(ImsExternalCallTracker.TAG, "onPullExternalCall : No call puller defined");
            } else {
                ImsExternalCallTracker.this.mCallPuller.pullExternalCall(connection.getAddress(), connection.getVideoState(), connection.getCallId());
            }
        }
    }

    @VisibleForTesting
    public ImsExternalCallTracker(ImsPhone phone, ImsPullCall callPuller, ImsCallNotify callNotifier) {
        this.mPhone = phone;
        this.mCallStateNotifier = callNotifier;
        this.mExternalCallStateListener = new ExternalCallStateListener();
        this.mCallPuller = callPuller;
    }

    public ImsExternalCallTracker(ImsPhone phone) {
        this.mPhone = phone;
        this.mCallStateNotifier = new ImsCallNotify() {
            public void notifyUnknownConnection(Connection c) {
                ImsExternalCallTracker.this.mPhone.notifyUnknownConnection(c);
            }

            public void notifyPreciseCallStateChanged() {
                ImsExternalCallTracker.this.mPhone.notifyPreciseCallStateChanged();
            }
        };
        this.mExternalCallStateListener = new ExternalCallStateListener();
        registerForNotifications();
    }

    public void tearDown() {
        unregisterForNotifications();
    }

    public void setCallPuller(ImsPullCall callPuller) {
        this.mCallPuller = callPuller;
    }

    public ExternalCallStateListener getExternalCallStateListener() {
        return this.mExternalCallStateListener;
    }

    public void onPhoneStateChanged(State oldState, State newState) {
        this.mHasActiveCalls = newState != State.IDLE;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onPhoneStateChanged : hasActiveCalls = ");
        stringBuilder.append(this.mHasActiveCalls);
        Log.i(str, stringBuilder.toString());
        refreshCallPullState();
    }

    private void registerForNotifications() {
        if (this.mPhone != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Registering: ");
            stringBuilder.append(this.mPhone);
            Log.d(str, stringBuilder.toString());
            this.mPhone.getDefaultPhone().registerForVideoCapabilityChanged(this.mHandler, 1, null);
        }
    }

    private void unregisterForNotifications() {
        if (this.mPhone != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unregistering: ");
            stringBuilder.append(this.mPhone);
            Log.d(str, stringBuilder.toString());
            this.mPhone.unregisterForVideoCapabilityChanged(this.mHandler);
        }
    }

    public void refreshExternalCallState(List<ImsExternalCallState> externalCallStates) {
        Log.d(TAG, "refreshExternalCallState");
        Iterator<Entry<Integer, ImsExternalConnection>> connectionIterator = this.mExternalConnections.entrySet().iterator();
        boolean wasCallRemoved = false;
        while (connectionIterator.hasNext()) {
            Entry<Integer, ImsExternalConnection> entry = (Entry) connectionIterator.next();
            if (!containsCallId(externalCallStates, ((Integer) entry.getKey()).intValue())) {
                ImsExternalConnection externalConnection = (ImsExternalConnection) entry.getValue();
                externalConnection.setTerminated();
                externalConnection.removeListener(this.mExternalConnectionListener);
                connectionIterator.remove();
                wasCallRemoved = true;
            }
        }
        if (wasCallRemoved) {
            this.mCallStateNotifier.notifyPreciseCallStateChanged();
        }
        if (externalCallStates != null && !externalCallStates.isEmpty()) {
            for (ImsExternalCallState callState : externalCallStates) {
                if (this.mExternalConnections.containsKey(Integer.valueOf(callState.getCallId()))) {
                    updateExistingConnection((ImsExternalConnection) this.mExternalConnections.get(Integer.valueOf(callState.getCallId())), callState);
                } else {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("refreshExternalCallState: got = ");
                    stringBuilder.append(callState);
                    Log.d(str, stringBuilder.toString());
                    if (callState.getCallState() == 1) {
                        createExternalConnection(callState);
                    }
                }
            }
        }
    }

    public Connection getConnectionById(int callId) {
        return (Connection) this.mExternalConnections.get(Integer.valueOf(callId));
    }

    private void createExternalConnection(ImsExternalCallState state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createExternalConnection : state = ");
        stringBuilder.append(state);
        Log.i(str, stringBuilder.toString());
        int videoState = ImsCallProfile.getVideoStateFromCallType(state.getCallType());
        boolean isCallPullPermitted = isCallPullPermitted(state.isCallPullable(), videoState);
        ImsExternalConnection connection = new ImsExternalConnection(this.mPhone, state.getCallId(), state.getAddress(), isCallPullPermitted);
        connection.setVideoState(videoState);
        connection.addListener(this.mExternalConnectionListener);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("createExternalConnection - pullable state : externalCallId = ");
        stringBuilder2.append(connection.getCallId());
        stringBuilder2.append(" ; isPullable = ");
        stringBuilder2.append(isCallPullPermitted);
        stringBuilder2.append(" ; networkPullable = ");
        stringBuilder2.append(state.isCallPullable());
        stringBuilder2.append(" ; isVideo = ");
        stringBuilder2.append(VideoProfile.isVideo(videoState));
        stringBuilder2.append(" ; videoEnabled = ");
        stringBuilder2.append(this.mIsVideoCapable);
        stringBuilder2.append(" ; hasActiveCalls = ");
        stringBuilder2.append(this.mHasActiveCalls);
        Log.d(str2, stringBuilder2.toString());
        this.mExternalConnections.put(Integer.valueOf(connection.getCallId()), connection);
        this.mExternalCallPullableState.put(Integer.valueOf(connection.getCallId()), Boolean.valueOf(state.isCallPullable()));
        this.mCallStateNotifier.notifyUnknownConnection(connection);
    }

    private void updateExistingConnection(ImsExternalConnection connection, ImsExternalCallState state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateExistingConnection : state = ");
        stringBuilder.append(state);
        Log.i(str, stringBuilder.toString());
        Call.State existingState = connection.getState();
        Call.State newState = state.getCallState() == 1 ? Call.State.ACTIVE : Call.State.DISCONNECTED;
        if (existingState != newState) {
            if (newState == Call.State.ACTIVE) {
                connection.setActive();
            } else {
                connection.setTerminated();
                connection.removeListener(this.mExternalConnectionListener);
                this.mExternalConnections.remove(Integer.valueOf(connection.getCallId()));
                this.mExternalCallPullableState.remove(Integer.valueOf(connection.getCallId()));
                this.mCallStateNotifier.notifyPreciseCallStateChanged();
            }
        }
        int newVideoState = ImsCallProfile.getVideoStateFromCallType(state.getCallType());
        if (newVideoState != connection.getVideoState()) {
            connection.setVideoState(newVideoState);
        }
        this.mExternalCallPullableState.put(Integer.valueOf(state.getCallId()), Boolean.valueOf(state.isCallPullable()));
        boolean isCallPullPermitted = isCallPullPermitted(state.isCallPullable(), newVideoState);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("updateExistingConnection - pullable state : externalCallId = ");
        stringBuilder2.append(connection.getCallId());
        stringBuilder2.append(" ; isPullable = ");
        stringBuilder2.append(isCallPullPermitted);
        stringBuilder2.append(" ; networkPullable = ");
        stringBuilder2.append(state.isCallPullable());
        stringBuilder2.append(" ; isVideo = ");
        stringBuilder2.append(VideoProfile.isVideo(connection.getVideoState()));
        stringBuilder2.append(" ; videoEnabled = ");
        stringBuilder2.append(this.mIsVideoCapable);
        stringBuilder2.append(" ; hasActiveCalls = ");
        stringBuilder2.append(this.mHasActiveCalls);
        Log.d(str2, stringBuilder2.toString());
        connection.setIsPullable(isCallPullPermitted);
    }

    private void refreshCallPullState() {
        Log.d(TAG, "refreshCallPullState");
        for (ImsExternalConnection imsExternalConnection : this.mExternalConnections.values()) {
            boolean isNetworkPullable = ((Boolean) this.mExternalCallPullableState.get(Integer.valueOf(imsExternalConnection.getCallId()))).booleanValue();
            boolean isCallPullPermitted = isCallPullPermitted(isNetworkPullable, imsExternalConnection.getVideoState());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("refreshCallPullState : externalCallId = ");
            stringBuilder.append(imsExternalConnection.getCallId());
            stringBuilder.append(" ; isPullable = ");
            stringBuilder.append(isCallPullPermitted);
            stringBuilder.append(" ; networkPullable = ");
            stringBuilder.append(isNetworkPullable);
            stringBuilder.append(" ; isVideo = ");
            stringBuilder.append(VideoProfile.isVideo(imsExternalConnection.getVideoState()));
            stringBuilder.append(" ; videoEnabled = ");
            stringBuilder.append(this.mIsVideoCapable);
            stringBuilder.append(" ; hasActiveCalls = ");
            stringBuilder.append(this.mHasActiveCalls);
            Log.d(str, stringBuilder.toString());
            imsExternalConnection.setIsPullable(isCallPullPermitted);
        }
    }

    private boolean containsCallId(List<ImsExternalCallState> externalCallStates, int callId) {
        if (externalCallStates == null) {
            return false;
        }
        for (ImsExternalCallState state : externalCallStates) {
            if (state.getCallId() == callId) {
                return true;
            }
        }
        return false;
    }

    private void handleVideoCapabilitiesChanged(AsyncResult ar) {
        this.mIsVideoCapable = ((Boolean) ar.result).booleanValue();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleVideoCapabilitiesChanged : isVideoCapable = ");
        stringBuilder.append(this.mIsVideoCapable);
        Log.i(str, stringBuilder.toString());
        refreshCallPullState();
    }

    private boolean isCallPullPermitted(boolean isNetworkPullable, int videoState) {
        if ((!VideoProfile.isVideo(videoState) || this.mIsVideoCapable) && !this.mHasActiveCalls) {
            return isNetworkPullable;
        }
        return false;
    }
}
