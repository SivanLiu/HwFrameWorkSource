package com.android.internal.telephony.imsphone;

import android.telecom.ConferenceParticipant;
import android.telephony.Rlog;
import com.android.ims.ImsCall;
import com.android.ims.ImsException;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Call.State;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import java.util.Iterator;
import java.util.List;

public class ImsPhoneCall extends Call {
    public static final String CONTEXT_BACKGROUND = "BG";
    public static final String CONTEXT_FOREGROUND = "FG";
    public static final String CONTEXT_HANDOVER = "HO";
    public static final String CONTEXT_RINGING = "RG";
    public static final String CONTEXT_UNKNOWN = "UK";
    private static final boolean DBG = Rlog.isLoggable(LOG_TAG, 3);
    private static final boolean FORCE_DEBUG = false;
    private static final String LOG_TAG = "ImsPhoneCall";
    private static final boolean VDBG = Rlog.isLoggable(LOG_TAG, 2);
    private final String mCallContext;
    ImsPhoneCallTracker mOwner;
    private boolean mRingbackTonePlayed;

    ImsPhoneCall() {
        this.mRingbackTonePlayed = false;
        this.mCallContext = CONTEXT_UNKNOWN;
    }

    public ImsPhoneCall(ImsPhoneCallTracker owner, String context) {
        this.mRingbackTonePlayed = false;
        this.mOwner = owner;
        this.mCallContext = context;
    }

    public void dispose() {
        try {
            this.mOwner.hangup(this);
        } catch (CallStateException e) {
        } finally {
            int s = this.mConnections.size();
            for (int i = 0; i < s; i++) {
                ((ImsPhoneConnection) this.mConnections.get(i)).onDisconnect(14);
            }
        }
    }

    public List<Connection> getConnections() {
        return this.mConnections;
    }

    public Phone getPhone() {
        return this.mOwner.mPhone;
    }

    protected void setState(State newState) {
        super.setState(newState);
    }

    public boolean isMultiparty() {
        synchronized (ImsPhoneCall.class) {
            int s = this.mConnections.size();
            for (int i = 0; i < s; i++) {
                if (((ImsPhoneConnection) this.mConnections.get(i)).isMultiparty()) {
                    return true;
                }
            }
            return false;
        }
    }

    public void hangup() throws CallStateException {
        this.mOwner.hangup(this);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ImsPhoneCall ");
        sb.append(this.mCallContext);
        sb.append(" state: ");
        sb.append(this.mState.toString());
        sb.append(" ");
        if (this.mConnections.size() > 1) {
            sb.append(" ERROR_MULTIPLE ");
        }
        Iterator it = this.mConnections.iterator();
        while (it.hasNext()) {
            sb.append((Connection) it.next());
            sb.append(" ");
        }
        sb.append("]");
        return sb.toString();
    }

    public List<ConferenceParticipant> getConferenceParticipants() {
        ImsCall call = getImsCall();
        if (call == null) {
            return null;
        }
        return call.getConferenceParticipants();
    }

    public void attach(Connection conn) {
        if (VDBG) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("attach : ");
            stringBuilder.append(this.mCallContext);
            stringBuilder.append(" conn = ");
            stringBuilder.append(conn);
            Rlog.v(str, stringBuilder.toString());
        }
        synchronized (ImsPhoneCall.class) {
            clearDisconnected();
            this.mConnections.add(conn);
        }
        this.mOwner.logState();
    }

    public void attach(Connection conn, State state) {
        if (VDBG) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("attach : ");
            stringBuilder.append(this.mCallContext);
            stringBuilder.append(" state = ");
            stringBuilder.append(state.toString());
            Rlog.v(str, stringBuilder.toString());
        }
        attach(conn);
        this.mState = state;
    }

    public void attachFake(Connection conn, State state) {
        attach(conn, state);
    }

    public boolean connectionDisconnected(ImsPhoneConnection conn) {
        if (this.mState != State.DISCONNECTED) {
            boolean hasOnlyDisconnectedConnections = true;
            int s = this.mConnections.size();
            for (int i = 0; i < s; i++) {
                if (((Connection) this.mConnections.get(i)).getState() != State.DISCONNECTED) {
                    hasOnlyDisconnectedConnections = false;
                    break;
                }
            }
            if (hasOnlyDisconnectedConnections) {
                this.mState = State.DISCONNECTED;
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("connectionDisconnected : ");
                stringBuilder.append(this.mCallContext);
                stringBuilder.append(" state = ");
                stringBuilder.append(this.mState);
                Rlog.v(str, stringBuilder.toString());
                return true;
            }
        }
        return false;
    }

    public void detach(ImsPhoneConnection conn) {
        if (VDBG) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("detach : ");
            stringBuilder.append(this.mCallContext);
            stringBuilder.append(" conn = ");
            stringBuilder.append(conn);
            Rlog.v(str, stringBuilder.toString());
        }
        synchronized (ImsPhoneCall.class) {
            this.mConnections.remove(conn);
            clearDisconnected();
        }
        this.mOwner.logState();
    }

    boolean isFull() {
        return this.mConnections.size() == 5;
    }

    void onHangupLocal() {
        int s = this.mConnections.size();
        for (int i = 0; i < s; i++) {
            ((ImsPhoneConnection) this.mConnections.get(i)).onHangupLocal();
        }
        this.mState = State.DISCONNECTING;
        if (VDBG) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onHangupLocal : ");
            stringBuilder.append(this.mCallContext);
            stringBuilder.append(" state = ");
            stringBuilder.append(this.mState);
            Rlog.v(str, stringBuilder.toString());
        }
    }

    public void clearDisconnected() {
        for (int i = this.mConnections.size() - 1; i >= 0; i--) {
            ImsPhoneConnection cn = (ImsPhoneConnection) this.mConnections.get(i);
            if (cn == null || cn.getState() == State.DISCONNECTED) {
                this.mConnections.remove(i);
            }
        }
        if (this.mConnections.size() == 0) {
            this.mState = State.IDLE;
        }
    }

    @VisibleForTesting
    public ImsPhoneConnection getFirstConnection() {
        if (this.mConnections.size() == 0) {
            return null;
        }
        return (ImsPhoneConnection) this.mConnections.get(0);
    }

    void setMute(boolean mute) {
        ImsCall imsCall = getFirstConnection() == null ? null : getFirstConnection().getImsCall();
        if (imsCall != null) {
            try {
                imsCall.setMute(mute);
            } catch (ImsException e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setMute failed : ");
                stringBuilder.append(e.getMessage());
                Rlog.e(str, stringBuilder.toString());
            }
        }
    }

    void merge(ImsPhoneCall that, State state) {
        ImsPhoneConnection imsPhoneConnection = getFirstConnection();
        if (imsPhoneConnection != null) {
            long conferenceConnectTime = imsPhoneConnection.getConferenceConnectTime();
            if (conferenceConnectTime > 0) {
                imsPhoneConnection.setConnectTime(conferenceConnectTime);
                imsPhoneConnection.setConnectTimeReal(imsPhoneConnection.getConnectTimeReal());
            } else if (DBG) {
                Rlog.d(LOG_TAG, "merge: conference connect time is 0");
            }
        }
        if (DBG) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("merge(");
            stringBuilder.append(this.mCallContext);
            stringBuilder.append("): ");
            stringBuilder.append(that);
            stringBuilder.append("state = ");
            stringBuilder.append(state);
            Rlog.d(str, stringBuilder.toString());
        }
    }

    @VisibleForTesting
    public ImsCall getImsCall() {
        ImsCall imsCall;
        synchronized (ImsPhoneCall.class) {
            imsCall = getFirstConnection() == null ? null : getFirstConnection().getImsCall();
        }
        return imsCall;
    }

    static boolean isLocalTone(ImsCall imsCall) {
        boolean z = false;
        if (imsCall == null || imsCall.getCallProfile() == null || imsCall.getCallProfile().mMediaProfile == null) {
            return false;
        }
        if (imsCall.getCallProfile().mMediaProfile.mAudioDirection == 0) {
            z = true;
        }
        return z;
    }

    public boolean update(ImsPhoneConnection conn, ImsCall imsCall, State state) {
        boolean changed = false;
        State oldState = this.mState;
        if (state == State.ALERTING) {
            if (this.mRingbackTonePlayed && !isLocalTone(imsCall)) {
                this.mOwner.mPhone.stopRingbackTone();
                this.mRingbackTonePlayed = false;
            } else if (!this.mRingbackTonePlayed && isLocalTone(imsCall)) {
                this.mOwner.mPhone.startRingbackTone();
                this.mRingbackTonePlayed = true;
            }
        } else if (this.mRingbackTonePlayed) {
            this.mOwner.mPhone.stopRingbackTone();
            this.mRingbackTonePlayed = false;
        }
        if (state != this.mState && state != State.DISCONNECTED) {
            this.mState = state;
            changed = true;
        } else if (state == State.DISCONNECTED) {
            changed = true;
        }
        if (VDBG) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("update : ");
            stringBuilder.append(this.mCallContext);
            stringBuilder.append(" state: ");
            stringBuilder.append(oldState);
            stringBuilder.append(" --> ");
            stringBuilder.append(this.mState);
            Rlog.v(str, stringBuilder.toString());
        }
        return changed;
    }

    ImsPhoneConnection getHandoverConnection() {
        return (ImsPhoneConnection) getEarliestConnection();
    }

    public void switchWith(ImsPhoneCall that) {
        if (VDBG) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("switchWith : switchCall = ");
            stringBuilder.append(this);
            stringBuilder.append(" withCall = ");
            stringBuilder.append(that);
            Rlog.v(str, stringBuilder.toString());
        }
        synchronized (ImsPhoneCall.class) {
            ImsPhoneCall tmp = new ImsPhoneCall();
            tmp.takeOver(this);
            takeOver(that);
            that.takeOver(tmp);
        }
        this.mOwner.logState();
    }

    private void takeOver(ImsPhoneCall that) {
        this.mConnections = that.mConnections;
        this.mState = that.mState;
        Iterator it = this.mConnections.iterator();
        while (it.hasNext()) {
            ((ImsPhoneConnection) ((Connection) it.next())).changeParent(this);
        }
    }
}
