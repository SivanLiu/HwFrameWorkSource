package com.android.internal.telephony.sip;

import android.content.Context;
import android.media.AudioManager;
import android.net.LinkProperties;
import android.net.rtp.AudioGroup;
import android.net.sip.SipAudioCall;
import android.net.sip.SipAudioCall.Listener;
import android.net.sip.SipErrorCode;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipProfile.Builder;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.os.WorkSource;
import android.telephony.CellLocation;
import android.telephony.NetworkScanRequest;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Call.State;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneConstants.DataState;
import com.android.internal.telephony.PhoneInternalInterface.DataActivityState;
import com.android.internal.telephony.PhoneInternalInterface.DialArgs;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.uicc.IccFileHandler;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class SipPhone extends SipPhoneBase {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "SipPhone";
    private static final int TIMEOUT_ANSWER_CALL = 8;
    private static final int TIMEOUT_HOLD_CALL = 15;
    private static final long TIMEOUT_HOLD_PROCESSING = 1000;
    private static final int TIMEOUT_MAKE_CALL = 15;
    private static final boolean VDBG = false;
    private SipCall mBackgroundCall = new SipCall();
    private SipCall mForegroundCall = new SipCall();
    private SipProfile mProfile;
    private SipCall mRingingCall = new SipCall();
    private SipManager mSipManager;
    private long mTimeOfLastValidHoldRequest = System.currentTimeMillis();

    private abstract class SipAudioCallAdapter extends Listener {
        private static final boolean SACA_DBG = true;
        private static final String SACA_TAG = "SipAudioCallAdapter";

        protected abstract void onCallEnded(int i);

        protected abstract void onError(int i);

        private SipAudioCallAdapter() {
        }

        public void onCallEnded(SipAudioCall call) {
            int i;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onCallEnded: call=");
            stringBuilder.append(call);
            log(stringBuilder.toString());
            if (call.isInCall()) {
                i = 2;
            } else {
                i = 1;
            }
            onCallEnded(i);
        }

        public void onCallBusy(SipAudioCall call) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onCallBusy: call=");
            stringBuilder.append(call);
            log(stringBuilder.toString());
            onCallEnded(4);
        }

        public void onError(SipAudioCall call, int errorCode, String errorMessage) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onError: call=");
            stringBuilder.append(call);
            stringBuilder.append(" code=");
            stringBuilder.append(SipErrorCode.toString(errorCode));
            stringBuilder.append(": ");
            stringBuilder.append(errorMessage);
            log(stringBuilder.toString());
            switch (errorCode) {
                case -12:
                    onError(9);
                    return;
                case -11:
                    onError(11);
                    return;
                case -10:
                    onError(14);
                    return;
                case -8:
                    onError(10);
                    return;
                case -7:
                    onError(8);
                    return;
                case -6:
                    onError(7);
                    return;
                case -5:
                case -3:
                    onError(13);
                    return;
                case -2:
                    onError(12);
                    return;
                default:
                    onError(36);
                    return;
            }
        }

        private void log(String s) {
            Rlog.d(SACA_TAG, s);
        }
    }

    private class SipCall extends SipCallBase {
        private static final boolean SC_DBG = true;
        private static final String SC_TAG = "SipCall";
        private static final boolean SC_VDBG = false;

        private SipCall() {
        }

        void reset() {
            log("reset");
            this.mConnections.clear();
            setState(State.IDLE);
        }

        void switchWith(SipCall that) {
            log("switchWith");
            synchronized (SipPhone.class) {
                SipCall tmp = new SipCall();
                tmp.takeOver(this);
                takeOver(that);
                that.takeOver(tmp);
            }
        }

        private void takeOver(SipCall that) {
            log("takeOver");
            this.mConnections = that.mConnections;
            this.mState = that.mState;
            Iterator it = this.mConnections.iterator();
            while (it.hasNext()) {
                ((SipConnection) ((Connection) it.next())).changeOwner(this);
            }
        }

        public Phone getPhone() {
            return SipPhone.this;
        }

        public List<Connection> getConnections() {
            ArrayList arrayList;
            synchronized (SipPhone.class) {
                arrayList = this.mConnections;
            }
            return arrayList;
        }

        Connection dial(String originalNumber) throws SipException {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dial: num=");
            stringBuilder.append("xxx");
            log(stringBuilder.toString());
            String calleeSipUri = originalNumber;
            if (!calleeSipUri.contains("@")) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(SipPhone.this.mProfile.getUserName());
                stringBuilder2.append("@");
                String replaceStr = Pattern.quote(stringBuilder2.toString());
                String uriString = SipPhone.this.mProfile.getUriString();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(calleeSipUri);
                stringBuilder3.append("@");
                calleeSipUri = uriString.replaceFirst(replaceStr, stringBuilder3.toString());
            }
            try {
                SipConnection c = new SipConnection(this, new Builder(calleeSipUri).build(), originalNumber);
                c.dial();
                this.mConnections.add(c);
                setState(State.DIALING);
                return c;
            } catch (ParseException e) {
                throw new SipException("dial", e);
            }
        }

        public void hangup() throws CallStateException {
            synchronized (SipPhone.class) {
                StringBuilder stringBuilder;
                if (this.mState.isAlive()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("hangup: call ");
                    stringBuilder.append(getState());
                    stringBuilder.append(": ");
                    stringBuilder.append(this);
                    stringBuilder.append(" on phone ");
                    stringBuilder.append(getPhone());
                    log(stringBuilder.toString());
                    setState(State.DISCONNECTING);
                    CallStateException excp = null;
                    Iterator it = this.mConnections.iterator();
                    while (it.hasNext()) {
                        try {
                            ((Connection) it.next()).hangup();
                        } catch (CallStateException e) {
                            excp = e;
                        }
                    }
                    if (excp != null) {
                        throw excp;
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("hangup: dead call ");
                stringBuilder.append(getState());
                stringBuilder.append(": ");
                stringBuilder.append(this);
                stringBuilder.append(" on phone ");
                stringBuilder.append(getPhone());
                log(stringBuilder.toString());
            }
        }

        SipConnection initIncomingCall(SipAudioCall sipAudioCall, boolean makeCallWait) {
            SipConnection c = new SipConnection(SipPhone.this, this, sipAudioCall.getPeerProfile());
            this.mConnections.add(c);
            State newState = makeCallWait ? State.WAITING : State.INCOMING;
            c.initIncomingCall(sipAudioCall, newState);
            setState(newState);
            SipPhone.this.notifyNewRingingConnectionP(c);
            return c;
        }

        void rejectCall() throws CallStateException {
            log("rejectCall:");
            hangup();
        }

        void acceptCall() throws CallStateException {
            log("acceptCall: accepting");
            if (this != SipPhone.this.mRingingCall) {
                throw new CallStateException("acceptCall() in a non-ringing call");
            } else if (this.mConnections.size() == 1) {
                ((SipConnection) this.mConnections.get(0)).acceptCall();
            } else {
                throw new CallStateException("acceptCall() in a conf call");
            }
        }

        private boolean isSpeakerOn() {
            return Boolean.valueOf(((AudioManager) SipPhone.this.mContext.getSystemService("audio")).isSpeakerphoneOn()).booleanValue();
        }

        void setAudioGroupMode() {
            AudioGroup audioGroup = getAudioGroup();
            if (audioGroup == null) {
                log("setAudioGroupMode: audioGroup == null ignore");
                return;
            }
            int mode = audioGroup.getMode();
            if (this.mState == State.HOLDING) {
                audioGroup.setMode(0);
            } else if (getMute()) {
                audioGroup.setMode(1);
            } else if (isSpeakerOn()) {
                audioGroup.setMode(3);
            } else {
                audioGroup.setMode(2);
            }
            log(String.format("setAudioGroupMode change: %d --> %d", new Object[]{Integer.valueOf(mode), Integer.valueOf(audioGroup.getMode())}));
        }

        void hold() throws CallStateException {
            log("hold:");
            setState(State.HOLDING);
            Iterator it = this.mConnections.iterator();
            while (it.hasNext()) {
                ((SipConnection) ((Connection) it.next())).hold();
            }
            setAudioGroupMode();
        }

        void unhold() throws CallStateException {
            log("unhold:");
            setState(State.ACTIVE);
            AudioGroup audioGroup = new AudioGroup();
            Iterator it = this.mConnections.iterator();
            while (it.hasNext()) {
                ((SipConnection) ((Connection) it.next())).unhold(audioGroup);
            }
            setAudioGroupMode();
        }

        void setMute(boolean muted) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setMute: muted=");
            stringBuilder.append(muted);
            log(stringBuilder.toString());
            Iterator it = this.mConnections.iterator();
            while (it.hasNext()) {
                ((SipConnection) ((Connection) it.next())).setMute(muted);
            }
        }

        boolean getMute() {
            boolean z = false;
            if (!this.mConnections.isEmpty()) {
                z = ((SipConnection) this.mConnections.get(0)).getMute();
            }
            boolean ret = z;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getMute: ret=");
            stringBuilder.append(ret);
            log(stringBuilder.toString());
            return ret;
        }

        void merge(SipCall that) throws CallStateException {
            log("merge:");
            AudioGroup audioGroup = getAudioGroup();
            for (Connection c : (Connection[]) that.mConnections.toArray(new Connection[that.mConnections.size()])) {
                SipConnection conn = (SipConnection) c;
                add(conn);
                if (conn.getState() == State.HOLDING) {
                    conn.unhold(audioGroup);
                }
            }
            that.setState(State.IDLE);
        }

        private void add(SipConnection conn) {
            log("add:");
            SipCall call = conn.getCall();
            if (call != this) {
                if (call != null) {
                    call.mConnections.remove(conn);
                }
                this.mConnections.add(conn);
                conn.changeOwner(this);
            }
        }

        void sendDtmf(char c) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendDtmf: c=");
            stringBuilder.append(c);
            log(stringBuilder.toString());
            AudioGroup audioGroup = getAudioGroup();
            if (audioGroup == null) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("sendDtmf: audioGroup == null, ignore c=");
                stringBuilder2.append(c);
                log(stringBuilder2.toString());
                return;
            }
            audioGroup.sendDtmf(convertDtmf(c));
        }

        private int convertDtmf(char c) {
            int code = c - 48;
            if (code >= 0 && code <= 9) {
                return code;
            }
            if (c == '#') {
                return 11;
            }
            if (c == '*') {
                return 10;
            }
            switch (c) {
                case 'A':
                    return 12;
                case 'B':
                    return 13;
                case 'C':
                    return 14;
                case 'D':
                    return 15;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("invalid DTMF char: ");
                    stringBuilder.append(c);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        protected void setState(State newState) {
            if (this.mState != newState) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setState: cur state");
                stringBuilder.append(this.mState);
                stringBuilder.append(" --> ");
                stringBuilder.append(newState);
                stringBuilder.append(": ");
                stringBuilder.append(this);
                stringBuilder.append(": on phone ");
                stringBuilder.append(getPhone());
                stringBuilder.append(" ");
                stringBuilder.append(this.mConnections.size());
                log(stringBuilder.toString());
                if (newState == State.ALERTING) {
                    this.mState = newState;
                    SipPhone.this.startRingbackTone();
                } else if (this.mState == State.ALERTING) {
                    SipPhone.this.stopRingbackTone();
                }
                this.mState = newState;
                SipPhone.this.updatePhoneState();
                SipPhone.this.notifyPreciseCallStateChanged();
            }
        }

        void onConnectionStateChanged(SipConnection conn) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onConnectionStateChanged: conn=");
            stringBuilder.append(conn);
            log(stringBuilder.toString());
            if (this.mState != State.ACTIVE) {
                setState(conn.getState());
            }
        }

        void onConnectionEnded(SipConnection conn) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onConnectionEnded: conn=");
            stringBuilder.append(conn);
            log(stringBuilder.toString());
            if (this.mState != State.DISCONNECTED) {
                boolean allConnectionsDisconnected = true;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("---check connections: ");
                stringBuilder2.append(this.mConnections.size());
                log(stringBuilder2.toString());
                Iterator it = this.mConnections.iterator();
                while (it.hasNext()) {
                    Connection c = (Connection) it.next();
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("   state=");
                    stringBuilder3.append(c.getState());
                    stringBuilder3.append(": ");
                    stringBuilder3.append(c);
                    log(stringBuilder3.toString());
                    if (c.getState() != State.DISCONNECTED) {
                        allConnectionsDisconnected = false;
                        break;
                    }
                }
                if (allConnectionsDisconnected) {
                    setState(State.DISCONNECTED);
                }
            }
            SipPhone.this.notifyDisconnectP(conn);
        }

        private AudioGroup getAudioGroup() {
            if (this.mConnections.isEmpty()) {
                return null;
            }
            return ((SipConnection) this.mConnections.get(0)).getAudioGroup();
        }

        private void log(String s) {
            Rlog.d(SC_TAG, s);
        }
    }

    private class SipConnection extends SipConnectionBase {
        private static final boolean SCN_DBG = true;
        private static final String SCN_TAG = "SipConnection";
        private SipAudioCallAdapter mAdapter;
        private boolean mIncoming;
        private String mOriginalNumber;
        private SipCall mOwner;
        private SipProfile mPeer;
        private SipAudioCall mSipAudioCall;
        private State mState;

        public SipConnection(SipCall owner, SipProfile callee, String originalNumber) {
            super(originalNumber);
            this.mState = State.IDLE;
            this.mIncoming = false;
            this.mAdapter = new SipAudioCallAdapter() {
                {
                    SipPhone sipPhone = SipPhone.this;
                }

                protected void onCallEnded(int cause) {
                    if (SipConnection.this.getDisconnectCause() != 3) {
                        SipConnection.this.setDisconnectCause(cause);
                    }
                    synchronized (SipPhone.class) {
                        String sessionState;
                        SipConnection.this.setState(State.DISCONNECTED);
                        SipAudioCall sipAudioCall = SipConnection.this.mSipAudioCall;
                        SipConnection.this.mSipAudioCall = null;
                        if (sipAudioCall == null) {
                            sessionState = "";
                        } else {
                            sessionState = new StringBuilder();
                            sessionState.append(sipAudioCall.getState());
                            sessionState.append(", ");
                            sessionState = sessionState.toString();
                        }
                        SipConnection sipConnection = SipConnection.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("[SipAudioCallAdapter] onCallEnded: ");
                        stringBuilder.append(SipPhone.hidePii(SipConnection.this.mPeer.getUriString()));
                        stringBuilder.append(": ");
                        stringBuilder.append(sessionState);
                        stringBuilder.append("cause: ");
                        stringBuilder.append(SipConnection.this.getDisconnectCause());
                        stringBuilder.append(", on phone ");
                        stringBuilder.append(SipConnection.this.getPhone());
                        sipConnection.log(stringBuilder.toString());
                        if (sipAudioCall != null) {
                            sipAudioCall.setListener(null);
                            sipAudioCall.close();
                        }
                        SipConnection.this.mOwner.onConnectionEnded(SipConnection.this);
                    }
                }

                public void onCallEstablished(SipAudioCall call) {
                    onChanged(call);
                    if (SipConnection.this.mState == State.ACTIVE) {
                        call.startAudio();
                    }
                }

                public void onCallHeld(SipAudioCall call) {
                    onChanged(call);
                    if (SipConnection.this.mState == State.HOLDING) {
                        call.startAudio();
                    }
                }

                public void onChanged(SipAudioCall call) {
                    synchronized (SipPhone.class) {
                        State newState = SipPhone.getCallStateFrom(call);
                        if (SipConnection.this.mState == newState) {
                            return;
                        }
                        if (newState == State.INCOMING) {
                            SipConnection.this.setState(SipConnection.this.mOwner.getState());
                        } else {
                            if (SipConnection.this.mOwner == SipPhone.this.mRingingCall) {
                                if (SipPhone.this.mRingingCall.getState() == State.WAITING) {
                                    try {
                                        SipPhone.this.switchHoldingAndActive();
                                    } catch (CallStateException e) {
                                        onCallEnded(3);
                                        return;
                                    }
                                }
                                SipPhone.this.mForegroundCall.switchWith(SipPhone.this.mRingingCall);
                            }
                            SipConnection.this.setState(newState);
                        }
                        SipConnection.this.mOwner.onConnectionStateChanged(SipConnection.this);
                        SipConnection sipConnection = SipConnection.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("onChanged: ");
                        stringBuilder.append(SipPhone.hidePii(SipConnection.this.mPeer.getUriString()));
                        stringBuilder.append(": ");
                        stringBuilder.append(SipConnection.this.mState);
                        stringBuilder.append(" on phone ");
                        stringBuilder.append(SipConnection.this.getPhone());
                        sipConnection.log(stringBuilder.toString());
                    }
                }

                protected void onError(int cause) {
                    SipConnection sipConnection = SipConnection.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onError: ");
                    stringBuilder.append(cause);
                    sipConnection.log(stringBuilder.toString());
                    onCallEnded(cause);
                }
            };
            this.mOwner = owner;
            this.mPeer = callee;
            this.mOriginalNumber = originalNumber;
        }

        public SipConnection(SipPhone sipPhone, SipCall owner, SipProfile callee) {
            this(owner, callee, sipPhone.getUriString(callee));
        }

        public String getCnapName() {
            String displayName = this.mPeer.getDisplayName();
            return TextUtils.isEmpty(displayName) ? null : displayName;
        }

        public int getNumberPresentation() {
            return 1;
        }

        void initIncomingCall(SipAudioCall sipAudioCall, State newState) {
            setState(newState);
            this.mSipAudioCall = sipAudioCall;
            sipAudioCall.setListener(this.mAdapter);
            this.mIncoming = true;
        }

        void acceptCall() throws CallStateException {
            try {
                this.mSipAudioCall.answerCall(8);
            } catch (SipException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("acceptCall(): ");
                stringBuilder.append(e);
                throw new CallStateException(stringBuilder.toString());
            }
        }

        void changeOwner(SipCall owner) {
            this.mOwner = owner;
        }

        AudioGroup getAudioGroup() {
            if (this.mSipAudioCall == null) {
                return null;
            }
            return this.mSipAudioCall.getAudioGroup();
        }

        void dial() throws SipException {
            setState(State.DIALING);
            this.mSipAudioCall = SipPhone.this.mSipManager.makeAudioCall(SipPhone.this.mProfile, this.mPeer, null, 15);
            this.mSipAudioCall.setListener(this.mAdapter);
        }

        void hold() throws CallStateException {
            setState(State.HOLDING);
            try {
                this.mSipAudioCall.holdCall(15);
            } catch (SipException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hold(): ");
                stringBuilder.append(e);
                throw new CallStateException(stringBuilder.toString());
            }
        }

        void unhold(AudioGroup audioGroup) throws CallStateException {
            this.mSipAudioCall.setAudioGroup(audioGroup);
            setState(State.ACTIVE);
            try {
                this.mSipAudioCall.continueCall(15);
            } catch (SipException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unhold(): ");
                stringBuilder.append(e);
                throw new CallStateException(stringBuilder.toString());
            }
        }

        void setMute(boolean muted) {
            if (this.mSipAudioCall != null && muted != this.mSipAudioCall.isMuted()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setState: prev muted=");
                stringBuilder.append(muted ^ 1);
                stringBuilder.append(" new muted=");
                stringBuilder.append(muted);
                log(stringBuilder.toString());
                this.mSipAudioCall.toggleMute();
            }
        }

        boolean getMute() {
            if (this.mSipAudioCall == null) {
                return false;
            }
            return this.mSipAudioCall.isMuted();
        }

        protected void setState(State state) {
            if (state != this.mState) {
                super.setState(state);
                this.mState = state;
            }
        }

        public State getState() {
            return this.mState;
        }

        public boolean isIncoming() {
            return this.mIncoming;
        }

        public String getAddress() {
            return this.mOriginalNumber;
        }

        public SipCall getCall() {
            return this.mOwner;
        }

        protected Phone getPhone() {
            return this.mOwner.getPhone();
        }

        public void hangup() throws CallStateException {
            synchronized (SipPhone.class) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hangup: conn=");
                stringBuilder.append(SipPhone.hidePii(this.mPeer.getUriString()));
                stringBuilder.append(": ");
                stringBuilder.append(this.mState);
                stringBuilder.append(": on phone ");
                stringBuilder.append(getPhone().getPhoneName());
                log(stringBuilder.toString());
                if (this.mState.isAlive()) {
                    int i = 16;
                    try {
                        SipAudioCall sipAudioCall = this.mSipAudioCall;
                        if (sipAudioCall != null) {
                            sipAudioCall.setListener(null);
                            sipAudioCall.endCall();
                        }
                        SipAudioCallAdapter sipAudioCallAdapter = this.mAdapter;
                        if (this.mState != State.INCOMING) {
                            if (this.mState != State.WAITING) {
                                i = 3;
                                sipAudioCallAdapter.onCallEnded(i);
                            }
                        }
                        sipAudioCallAdapter.onCallEnded(i);
                    } catch (SipException e) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("hangup(): ");
                        stringBuilder2.append(e);
                        throw new CallStateException(stringBuilder2.toString());
                    } catch (Throwable th) {
                        SipAudioCallAdapter sipAudioCallAdapter2 = this.mAdapter;
                        if (this.mState != State.INCOMING) {
                            if (this.mState != State.WAITING) {
                                i = 3;
                                sipAudioCallAdapter2.onCallEnded(i);
                            }
                        }
                        sipAudioCallAdapter2.onCallEnded(i);
                    }
                }
            }
        }

        public void separate() throws CallStateException {
            synchronized (SipPhone.class) {
                SipCall call;
                if (getPhone() == SipPhone.this) {
                    call = (SipCall) SipPhone.this.getBackgroundCall();
                } else {
                    call = (SipCall) SipPhone.this.getForegroundCall();
                }
                if (call.getState() == State.IDLE) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("separate: conn=");
                    stringBuilder.append(this.mPeer.getUriString());
                    stringBuilder.append(" from ");
                    stringBuilder.append(this.mOwner);
                    stringBuilder.append(" back to ");
                    stringBuilder.append(call);
                    log(stringBuilder.toString());
                    Phone originalPhone = getPhone();
                    AudioGroup audioGroup = call.getAudioGroup();
                    call.add(this);
                    this.mSipAudioCall.setAudioGroup(audioGroup);
                    originalPhone.switchHoldingAndActive();
                    call = (SipCall) SipPhone.this.getForegroundCall();
                    this.mSipAudioCall.startAudio();
                    call.onConnectionStateChanged(this);
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("cannot put conn back to a call in non-idle state: ");
                    stringBuilder2.append(call.getState());
                    throw new CallStateException(stringBuilder2.toString());
                }
            }
        }

        public void deflect(String number) throws CallStateException {
            throw new CallStateException("deflect is not supported for SipPhone");
        }

        private void log(String s) {
            Rlog.d(SCN_TAG, s);
        }
    }

    public /* bridge */ /* synthetic */ void activateCellBroadcastSms(int i, Message message) {
        super.activateCellBroadcastSms(i, message);
    }

    public /* bridge */ /* synthetic */ boolean canDial() {
        return super.canDial();
    }

    public /* bridge */ /* synthetic */ boolean disableDataConnectivity() {
        return super.disableDataConnectivity();
    }

    public /* bridge */ /* synthetic */ void disableLocationUpdates() {
        super.disableLocationUpdates();
    }

    public /* bridge */ /* synthetic */ boolean enableDataConnectivity() {
        return super.enableDataConnectivity();
    }

    public /* bridge */ /* synthetic */ void enableLocationUpdates() {
        super.enableLocationUpdates();
    }

    public /* bridge */ /* synthetic */ void getAvailableNetworks(Message message) {
        super.getAvailableNetworks(message);
    }

    public /* bridge */ /* synthetic */ void getCallBarring(String str, String str2, Message message, int i) {
        super.getCallBarring(str, str2, message, i);
    }

    public /* bridge */ /* synthetic */ boolean getCallForwardingIndicator() {
        return super.getCallForwardingIndicator();
    }

    public /* bridge */ /* synthetic */ void getCallForwardingOption(int i, Message message) {
        super.getCallForwardingOption(i, message);
    }

    public /* bridge */ /* synthetic */ void getCellBroadcastSmsConfig(Message message) {
        super.getCellBroadcastSmsConfig(message);
    }

    public /* bridge */ /* synthetic */ CellLocation getCellLocation(WorkSource workSource) {
        return super.getCellLocation(workSource);
    }

    public /* bridge */ /* synthetic */ DataActivityState getDataActivityState() {
        return super.getDataActivityState();
    }

    public /* bridge */ /* synthetic */ DataState getDataConnectionState() {
        return super.getDataConnectionState();
    }

    public /* bridge */ /* synthetic */ DataState getDataConnectionState(String str) {
        return super.getDataConnectionState(str);
    }

    public /* bridge */ /* synthetic */ boolean getDataRoamingEnabled() {
        return super.getDataRoamingEnabled();
    }

    public /* bridge */ /* synthetic */ String getDeviceId() {
        return super.getDeviceId();
    }

    public /* bridge */ /* synthetic */ String getDeviceSvn() {
        return super.getDeviceSvn();
    }

    public /* bridge */ /* synthetic */ String getEsn() {
        return super.getEsn();
    }

    public /* bridge */ /* synthetic */ String getGroupIdLevel1() {
        return super.getGroupIdLevel1();
    }

    public /* bridge */ /* synthetic */ String getGroupIdLevel2() {
        return super.getGroupIdLevel2();
    }

    public /* bridge */ /* synthetic */ IccCard getIccCard() {
        return super.getIccCard();
    }

    public /* bridge */ /* synthetic */ IccFileHandler getIccFileHandler() {
        return super.getIccFileHandler();
    }

    public /* bridge */ /* synthetic */ IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager() {
        return super.getIccPhoneBookInterfaceManager();
    }

    public /* bridge */ /* synthetic */ boolean getIccRecordsLoaded() {
        return super.getIccRecordsLoaded();
    }

    public /* bridge */ /* synthetic */ String getIccSerialNumber() {
        return super.getIccSerialNumber();
    }

    public /* bridge */ /* synthetic */ String getImei() {
        return super.getImei();
    }

    public /* bridge */ /* synthetic */ String getLine1AlphaTag() {
        return super.getLine1AlphaTag();
    }

    public /* bridge */ /* synthetic */ String getLine1Number() {
        return super.getLine1Number();
    }

    public /* bridge */ /* synthetic */ LinkProperties getLinkProperties(String str) {
        return super.getLinkProperties(str);
    }

    public /* bridge */ /* synthetic */ String getMeid() {
        return super.getMeid();
    }

    public /* bridge */ /* synthetic */ boolean getMessageWaitingIndicator() {
        return super.getMessageWaitingIndicator();
    }

    public /* bridge */ /* synthetic */ List getPendingMmiCodes() {
        return super.getPendingMmiCodes();
    }

    public /* bridge */ /* synthetic */ int getPhoneType() {
        return super.getPhoneType();
    }

    public /* bridge */ /* synthetic */ SignalStrength getSignalStrength() {
        return super.getSignalStrength();
    }

    public /* bridge */ /* synthetic */ PhoneConstants.State getState() {
        return super.getState();
    }

    public /* bridge */ /* synthetic */ String getSubscriberId() {
        return super.getSubscriberId();
    }

    public /* bridge */ /* synthetic */ String getVoiceMailAlphaTag() {
        return super.getVoiceMailAlphaTag();
    }

    public /* bridge */ /* synthetic */ String getVoiceMailNumber() {
        return super.getVoiceMailNumber();
    }

    public /* bridge */ /* synthetic */ boolean handleInCallMmiCommands(String str) {
        return super.handleInCallMmiCommands(str);
    }

    public /* bridge */ /* synthetic */ boolean handlePinMmi(String str) {
        return super.handlePinMmi(str);
    }

    public /* bridge */ /* synthetic */ boolean handleUssdRequest(String str, ResultReceiver resultReceiver) {
        return super.handleUssdRequest(str, resultReceiver);
    }

    public /* bridge */ /* synthetic */ boolean isDataAllowed() {
        return super.isDataAllowed();
    }

    public /* bridge */ /* synthetic */ boolean isDataEnabled() {
        return super.isDataEnabled();
    }

    public /* bridge */ /* synthetic */ boolean isUserDataEnabled() {
        return super.isUserDataEnabled();
    }

    public /* bridge */ /* synthetic */ boolean isVideoEnabled() {
        return super.isVideoEnabled();
    }

    public /* bridge */ /* synthetic */ boolean needsOtaServiceProvisioning() {
        return super.needsOtaServiceProvisioning();
    }

    public /* bridge */ /* synthetic */ void notifyCallForwardingIndicator() {
        super.notifyCallForwardingIndicator();
    }

    public /* bridge */ /* synthetic */ void registerForRingbackTone(Handler handler, int i, Object obj) {
        super.registerForRingbackTone(handler, i, obj);
    }

    public /* bridge */ /* synthetic */ void registerForSuppServiceNotification(Handler handler, int i, Object obj) {
        super.registerForSuppServiceNotification(handler, i, obj);
    }

    public /* bridge */ /* synthetic */ void saveClirSetting(int i) {
        super.saveClirSetting(i);
    }

    public /* bridge */ /* synthetic */ void selectNetworkManually(OperatorInfo operatorInfo, boolean z, Message message) {
        super.selectNetworkManually(operatorInfo, z, message);
    }

    public /* bridge */ /* synthetic */ void sendEmergencyCallStateChange(boolean z) {
        super.sendEmergencyCallStateChange(z);
    }

    public /* bridge */ /* synthetic */ void sendUssdResponse(String str) {
        super.sendUssdResponse(str);
    }

    public /* bridge */ /* synthetic */ void setBroadcastEmergencyCallStateChanges(boolean z) {
        super.setBroadcastEmergencyCallStateChanges(z);
    }

    public /* bridge */ /* synthetic */ void setCallBarring(String str, boolean z, String str2, Message message, int i) {
        super.setCallBarring(str, z, str2, message, i);
    }

    public /* bridge */ /* synthetic */ void setCallForwardingOption(int i, int i2, String str, int i3, Message message) {
        super.setCallForwardingOption(i, i2, str, i3, message);
    }

    public /* bridge */ /* synthetic */ void setCellBroadcastSmsConfig(int[] iArr, Message message) {
        super.setCellBroadcastSmsConfig(iArr, message);
    }

    public /* bridge */ /* synthetic */ void setDataRoamingEnabled(boolean z) {
        super.setDataRoamingEnabled(z);
    }

    public /* bridge */ /* synthetic */ boolean setLine1Number(String str, String str2, Message message) {
        return super.setLine1Number(str, str2, message);
    }

    public /* bridge */ /* synthetic */ void setNetworkSelectionModeAutomatic(Message message) {
        super.setNetworkSelectionModeAutomatic(message);
    }

    public /* bridge */ /* synthetic */ void setOnPostDialCharacter(Handler handler, int i, Object obj) {
        super.setOnPostDialCharacter(handler, i, obj);
    }

    public /* bridge */ /* synthetic */ void setRadioPower(boolean z) {
        super.setRadioPower(z);
    }

    public /* bridge */ /* synthetic */ void setUserDataEnabled(boolean z) {
        super.setUserDataEnabled(z);
    }

    public /* bridge */ /* synthetic */ void setVoiceMailNumber(String str, String str2, Message message) {
        super.setVoiceMailNumber(str, str2, message);
    }

    public /* bridge */ /* synthetic */ void startNetworkScan(NetworkScanRequest networkScanRequest, Message message) {
        super.startNetworkScan(networkScanRequest, message);
    }

    public /* bridge */ /* synthetic */ void startRingbackTone() {
        super.startRingbackTone();
    }

    public /* bridge */ /* synthetic */ void stopNetworkScan(Message message) {
        super.stopNetworkScan(message);
    }

    public /* bridge */ /* synthetic */ void stopRingbackTone() {
        super.stopRingbackTone();
    }

    public /* bridge */ /* synthetic */ void unregisterForRingbackTone(Handler handler) {
        super.unregisterForRingbackTone(handler);
    }

    public /* bridge */ /* synthetic */ void unregisterForSuppServiceNotification(Handler handler) {
        super.unregisterForSuppServiceNotification(handler);
    }

    public /* bridge */ /* synthetic */ void updateServiceLocation() {
        super.updateServiceLocation();
    }

    SipPhone(Context context, PhoneNotifier notifier, SipProfile profile) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SIP:");
        stringBuilder.append(profile.getUriString());
        super(stringBuilder.toString(), context, notifier);
        stringBuilder = new StringBuilder();
        stringBuilder.append("new SipPhone: ");
        stringBuilder.append(hidePii(profile.getUriString()));
        log(stringBuilder.toString());
        this.mRingingCall = new SipCall();
        this.mForegroundCall = new SipCall();
        this.mBackgroundCall = new SipCall();
        this.mProfile = profile;
        this.mSipManager = SipManager.newInstance(context);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SipPhone)) {
            return false;
        }
        return this.mProfile.getUriString().equals(((SipPhone) o).mProfile.getUriString());
    }

    public String getSipUri() {
        return this.mProfile.getUriString();
    }

    public boolean equals(SipPhone phone) {
        return getSipUri().equals(phone.getSipUri());
    }

    public Connection takeIncomingCall(Object incomingCall) {
        synchronized (SipPhone.class) {
            if (!(incomingCall instanceof SipAudioCall)) {
                log("takeIncomingCall: ret=null, not a SipAudioCall");
                return null;
            } else if (this.mRingingCall.getState().isAlive()) {
                log("takeIncomingCall: ret=null, ringingCall not alive");
                return null;
            } else if (this.mForegroundCall.getState().isAlive() && this.mBackgroundCall.getState().isAlive()) {
                log("takeIncomingCall: ret=null, foreground and background both alive");
                return null;
            } else {
                StringBuilder stringBuilder;
                try {
                    SipAudioCall sipAudioCall = (SipAudioCall) incomingCall;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("takeIncomingCall: taking call from: ");
                    stringBuilder.append(hidePii(sipAudioCall.getPeerProfile().getUriString()));
                    log(stringBuilder.toString());
                    if (sipAudioCall.getLocalProfile().getUriString().equals(this.mProfile.getUriString())) {
                        SipConnection connection = this.mRingingCall.initIncomingCall(sipAudioCall, this.mForegroundCall.getState().isAlive());
                        if (sipAudioCall.getState() != 3) {
                            log("    takeIncomingCall: call cancelled !!");
                            this.mRingingCall.reset();
                            connection = null;
                        }
                        return connection;
                    }
                    log("takeIncomingCall: NOT taking !!");
                    return null;
                } catch (Exception e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("    takeIncomingCall: exception e=");
                    stringBuilder.append(e);
                    log(stringBuilder.toString());
                    this.mRingingCall.reset();
                }
            }
        }
    }

    public void acceptCall(int videoState) throws CallStateException {
        synchronized (SipPhone.class) {
            if (this.mRingingCall.getState() != State.INCOMING) {
                if (this.mRingingCall.getState() != State.WAITING) {
                    log("acceptCall: throw CallStateException(\"phone not ringing\")");
                    throw new CallStateException("phone not ringing");
                }
            }
            log("acceptCall: accepting");
            this.mRingingCall.setMute(false);
            this.mRingingCall.acceptCall();
        }
    }

    public void rejectCall() throws CallStateException {
        synchronized (SipPhone.class) {
            if (this.mRingingCall.getState().isRinging()) {
                log("rejectCall: rejecting");
                this.mRingingCall.rejectCall();
            } else {
                log("rejectCall: throw CallStateException(\"phone not ringing\")");
                throw new CallStateException("phone not ringing");
            }
        }
    }

    public Connection dial(String dialString, DialArgs dialArgs) throws CallStateException {
        Connection dialInternal;
        synchronized (SipPhone.class) {
            dialInternal = dialInternal(dialString, dialArgs.videoState);
        }
        return dialInternal;
    }

    private Connection dialInternal(String dialString, int videoState) throws CallStateException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dialInternal: dialString=");
        stringBuilder.append(hidePii(dialString));
        log(stringBuilder.toString());
        clearDisconnected();
        if (canDial()) {
            if (this.mForegroundCall.getState() == State.ACTIVE) {
                switchHoldingAndActive();
            }
            if (this.mForegroundCall.getState() == State.IDLE) {
                this.mForegroundCall.setMute(false);
                try {
                    return this.mForegroundCall.dial(dialString);
                } catch (SipException e) {
                    loge("dialInternal: ", e);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("dial error: ");
                    stringBuilder2.append(e);
                    throw new CallStateException(stringBuilder2.toString());
                }
            }
            throw new CallStateException("cannot dial in current state");
        }
        throw new CallStateException("dialInternal: cannot dial in current state");
    }

    public void switchHoldingAndActive() throws CallStateException {
        if (isHoldTimeoutExpired()) {
            log("switchHoldingAndActive: switch fg and bg");
            synchronized (SipPhone.class) {
                this.mForegroundCall.switchWith(this.mBackgroundCall);
                if (this.mBackgroundCall.getState().isAlive()) {
                    this.mBackgroundCall.hold();
                }
                if (this.mForegroundCall.getState().isAlive()) {
                    this.mForegroundCall.unhold();
                }
            }
            return;
        }
        log("switchHoldingAndActive: Disregarded! Under 1000 ms...");
    }

    public boolean canConference() {
        log("canConference: ret=true");
        return true;
    }

    public void conference() throws CallStateException {
        synchronized (SipPhone.class) {
            if (this.mForegroundCall.getState() == State.ACTIVE && this.mForegroundCall.getState() == State.ACTIVE) {
                log("conference: merge fg & bg");
                this.mForegroundCall.merge(this.mBackgroundCall);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("wrong state to merge calls: fg=");
                stringBuilder.append(this.mForegroundCall.getState());
                stringBuilder.append(", bg=");
                stringBuilder.append(this.mBackgroundCall.getState());
                throw new CallStateException(stringBuilder.toString());
            }
        }
    }

    public void conference(Call that) throws CallStateException {
        synchronized (SipPhone.class) {
            if (that instanceof SipCall) {
                this.mForegroundCall.merge((SipCall) that);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("expect ");
                stringBuilder.append(SipCall.class);
                stringBuilder.append(", cannot merge with ");
                stringBuilder.append(that.getClass());
                throw new CallStateException(stringBuilder.toString());
            }
        }
    }

    public boolean canTransfer() {
        return false;
    }

    public void explicitCallTransfer() {
    }

    public void clearDisconnected() {
        synchronized (SipPhone.class) {
            this.mRingingCall.clearDisconnected();
            this.mForegroundCall.clearDisconnected();
            this.mBackgroundCall.clearDisconnected();
            updatePhoneState();
            notifyPreciseCallStateChanged();
        }
    }

    public void sendDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendDtmf called with invalid character '");
            stringBuilder.append(c);
            stringBuilder.append("'");
            loge(stringBuilder.toString());
        } else if (this.mForegroundCall.getState().isAlive()) {
            synchronized (SipPhone.class) {
                this.mForegroundCall.sendDtmf(c);
            }
        }
    }

    public void startDtmf(char c) {
        if (PhoneNumberUtils.is12Key(c)) {
            sendDtmf(c);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startDtmf called with invalid character '");
        stringBuilder.append(c);
        stringBuilder.append("'");
        loge(stringBuilder.toString());
    }

    public void stopDtmf() {
    }

    public void sendBurstDtmf(String dtmfString) {
        loge("sendBurstDtmf() is a CDMA method");
    }

    public void getOutgoingCallerIdDisplay(Message onComplete) {
        AsyncResult.forMessage(onComplete, null, null);
        onComplete.sendToTarget();
    }

    public void setOutgoingCallerIdDisplay(int commandInterfaceCLIRMode, Message onComplete) {
        AsyncResult.forMessage(onComplete, null, null);
        onComplete.sendToTarget();
    }

    public void getCallWaiting(Message onComplete) {
        AsyncResult.forMessage(onComplete, null, null);
        onComplete.sendToTarget();
    }

    public void setCallWaiting(boolean enable, Message onComplete) {
        loge("call waiting not supported");
    }

    public void setEchoSuppressionEnabled() {
        synchronized (SipPhone.class) {
            if (((AudioManager) this.mContext.getSystemService("audio")).getParameters("ec_supported").contains("off")) {
                this.mForegroundCall.setAudioGroupMode();
            }
        }
    }

    public void setMute(boolean muted) {
        synchronized (SipPhone.class) {
            this.mForegroundCall.setMute(muted);
        }
    }

    public boolean getMute() {
        if (this.mForegroundCall.getState().isAlive()) {
            return this.mForegroundCall.getMute();
        }
        return this.mBackgroundCall.getMute();
    }

    public Call getForegroundCall() {
        return this.mForegroundCall;
    }

    public Call getBackgroundCall() {
        return this.mBackgroundCall;
    }

    public Call getRingingCall() {
        return this.mRingingCall;
    }

    public ServiceState getServiceState() {
        return super.getServiceState();
    }

    private String getUriString(SipProfile p) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(p.getUserName());
        stringBuilder.append("@");
        stringBuilder.append(getSipDomain(p));
        return stringBuilder.toString();
    }

    private String getSipDomain(SipProfile p) {
        String domain = p.getSipDomain();
        if (domain.endsWith(":5060")) {
            return domain.substring(0, domain.length() - 5);
        }
        return domain;
    }

    private static State getCallStateFrom(SipAudioCall sipAudioCall) {
        if (sipAudioCall.isOnHold()) {
            return State.HOLDING;
        }
        int sessionState = sipAudioCall.getState();
        if (sessionState == 0) {
            return State.IDLE;
        }
        switch (sessionState) {
            case 3:
            case 4:
                return State.INCOMING;
            case 5:
                return State.DIALING;
            case 6:
                return State.ALERTING;
            case 7:
                return State.DISCONNECTING;
            case 8:
                return State.ACTIVE;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("illegal connection state: ");
                stringBuilder.append(sessionState);
                slog(stringBuilder.toString());
                return State.DISCONNECTED;
        }
    }

    private synchronized boolean isHoldTimeoutExpired() {
        long currTime = System.currentTimeMillis();
        if (currTime - this.mTimeOfLastValidHoldRequest <= TIMEOUT_HOLD_PROCESSING) {
            return false;
        }
        this.mTimeOfLastValidHoldRequest = currTime;
        return true;
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, s);
    }

    private static void slog(String s) {
        Rlog.d(LOG_TAG, s);
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, s);
    }

    private void loge(String s, Exception e) {
        Rlog.e(LOG_TAG, s, e);
    }

    public static String hidePii(String s) {
        return "xxxxx";
    }
}
