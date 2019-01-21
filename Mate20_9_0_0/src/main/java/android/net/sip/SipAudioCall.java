package android.net.sip;

import android.content.Context;
import android.media.AudioManager;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.sip.SimpleSessionDescription.Media;
import android.net.sip.SipSession.State;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Message;
import android.telephony.Rlog;
import android.text.TextUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class SipAudioCall {
    private static final boolean DBG = true;
    private static final boolean DONT_RELEASE_SOCKET = false;
    private static final String LOG_TAG = SipAudioCall.class.getSimpleName();
    private static final boolean RELEASE_SOCKET = true;
    private static final int SESSION_TIMEOUT = 5;
    private static final int TRANSFER_TIMEOUT = 15;
    private AudioGroup mAudioGroup;
    private AudioStream mAudioStream;
    private Context mContext;
    private int mErrorCode = 0;
    private String mErrorMessage;
    private boolean mHold = DONT_RELEASE_SOCKET;
    private boolean mInCall = DONT_RELEASE_SOCKET;
    private Listener mListener;
    private SipProfile mLocalProfile;
    private boolean mMuted = DONT_RELEASE_SOCKET;
    private String mPeerSd;
    private long mSessionId = System.currentTimeMillis();
    private SipSession mSipSession;
    private SipSession mTransferringSession;
    private WifiLock mWifiHighPerfLock;
    private WifiManager mWm;

    public static class Listener {
        public void onReadyToCall(SipAudioCall call) {
            onChanged(call);
        }

        public void onCalling(SipAudioCall call) {
            onChanged(call);
        }

        public void onRinging(SipAudioCall call, SipProfile caller) {
            onChanged(call);
        }

        public void onRingingBack(SipAudioCall call) {
            onChanged(call);
        }

        public void onCallEstablished(SipAudioCall call) {
            onChanged(call);
        }

        public void onCallEnded(SipAudioCall call) {
            onChanged(call);
        }

        public void onCallBusy(SipAudioCall call) {
            onChanged(call);
        }

        public void onCallHeld(SipAudioCall call) {
            onChanged(call);
        }

        public void onError(SipAudioCall call, int errorCode, String errorMessage) {
        }

        public void onChanged(SipAudioCall call) {
        }
    }

    public SipAudioCall(Context context, SipProfile localProfile) {
        this.mContext = context;
        this.mLocalProfile = localProfile;
        this.mWm = (WifiManager) context.getSystemService("wifi");
    }

    public void setListener(Listener listener) {
        setListener(listener, DONT_RELEASE_SOCKET);
    }

    public void setListener(Listener listener, boolean callbackImmediately) {
        this.mListener = listener;
        if (listener != null && callbackImmediately) {
            try {
                if (this.mErrorCode != 0) {
                    listener.onError(this, this.mErrorCode, this.mErrorMessage);
                } else if (!this.mInCall) {
                    int state = getState();
                    if (state == 0) {
                        listener.onReadyToCall(this);
                    } else if (state != 3) {
                        switch (state) {
                            case 5:
                                listener.onCalling(this);
                                return;
                            case State.OUTGOING_CALL_RING_BACK /*6*/:
                                listener.onRingingBack(this);
                                return;
                            default:
                                return;
                        }
                    } else {
                        listener.onRinging(this, getPeerProfile());
                    }
                } else if (this.mHold) {
                    listener.onCallHeld(this);
                } else {
                    listener.onCallEstablished(this);
                }
            } catch (Throwable t) {
                loge("setListener()", t);
            }
        }
    }

    public boolean isInCall() {
        boolean z;
        synchronized (this) {
            z = this.mInCall;
        }
        return z;
    }

    public boolean isOnHold() {
        boolean z;
        synchronized (this) {
            z = this.mHold;
        }
        return z;
    }

    public void close() {
        close(true);
    }

    private synchronized void close(boolean closeRtp) {
        if (closeRtp) {
            try {
                stopCall(true);
            } catch (Throwable th) {
            }
        }
        this.mInCall = DONT_RELEASE_SOCKET;
        this.mHold = DONT_RELEASE_SOCKET;
        this.mSessionId = System.currentTimeMillis();
        this.mErrorCode = 0;
        this.mErrorMessage = null;
        if (this.mSipSession != null) {
            this.mSipSession.setListener(null);
            this.mSipSession = null;
        }
    }

    public SipProfile getLocalProfile() {
        SipProfile sipProfile;
        synchronized (this) {
            sipProfile = this.mLocalProfile;
        }
        return sipProfile;
    }

    public SipProfile getPeerProfile() {
        SipProfile peerProfile;
        synchronized (this) {
            peerProfile = this.mSipSession == null ? null : this.mSipSession.getPeerProfile();
        }
        return peerProfile;
    }

    public int getState() {
        synchronized (this) {
            if (this.mSipSession == null) {
                return 0;
            }
            int state = this.mSipSession.getState();
            return state;
        }
    }

    public SipSession getSipSession() {
        SipSession sipSession;
        synchronized (this) {
            sipSession = this.mSipSession;
        }
        return sipSession;
    }

    private synchronized void transferToNewSession() {
        if (this.mTransferringSession != null) {
            SipSession origin = this.mSipSession;
            this.mSipSession = this.mTransferringSession;
            this.mTransferringSession = null;
            if (this.mAudioStream != null) {
                this.mAudioStream.join(null);
            } else {
                try {
                    this.mAudioStream = new AudioStream(InetAddress.getByName(getLocalIp()));
                } catch (Throwable t) {
                    loge("transferToNewSession():", t);
                }
            }
            if (origin != null) {
                origin.endCall();
            }
            startAudio();
        }
    }

    private android.net.sip.SipSession.Listener createListener() {
        return new android.net.sip.SipSession.Listener() {
            public void onCalling(SipSession session) {
                SipAudioCall sipAudioCall = SipAudioCall.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onCalling: session=");
                stringBuilder.append(session);
                sipAudioCall.log(stringBuilder.toString());
                Listener listener = SipAudioCall.this.mListener;
                if (listener != null) {
                    try {
                        listener.onCalling(SipAudioCall.this);
                    } catch (Throwable t) {
                        SipAudioCall.this.loge("onCalling():", t);
                    }
                }
            }

            public void onRingingBack(SipSession session) {
                SipAudioCall sipAudioCall = SipAudioCall.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onRingingBackk: ");
                stringBuilder.append(session);
                sipAudioCall.log(stringBuilder.toString());
                Listener listener = SipAudioCall.this.mListener;
                if (listener != null) {
                    try {
                        listener.onRingingBack(SipAudioCall.this);
                    } catch (Throwable t) {
                        SipAudioCall.this.loge("onRingingBack():", t);
                    }
                }
            }

            public void onRinging(SipSession session, SipProfile peerProfile, String sessionDescription) {
                synchronized (SipAudioCall.this) {
                    if (SipAudioCall.this.mSipSession != null && SipAudioCall.this.mInCall && session.getCallId().equals(SipAudioCall.this.mSipSession.getCallId())) {
                        try {
                            SipAudioCall.this.mSipSession.answerCall(SipAudioCall.this.createAnswer(sessionDescription).encode(), 5);
                        } catch (Throwable e) {
                            SipAudioCall.this.loge("onRinging():", e);
                            session.endCall();
                        }
                    } else {
                        session.endCall();
                        return;
                    }
                }
            }

            public void onCallEstablished(SipSession session, String sessionDescription) {
                SipAudioCall.this.mPeerSd = sessionDescription;
                SipAudioCall sipAudioCall = SipAudioCall.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onCallEstablished(): ");
                stringBuilder.append(SipAudioCall.this.mPeerSd);
                sipAudioCall.log(stringBuilder.toString());
                if (SipAudioCall.this.mTransferringSession == null || session != SipAudioCall.this.mTransferringSession) {
                    Listener listener = SipAudioCall.this.mListener;
                    if (listener != null) {
                        try {
                            if (SipAudioCall.this.mHold) {
                                listener.onCallHeld(SipAudioCall.this);
                            } else {
                                listener.onCallEstablished(SipAudioCall.this);
                            }
                        } catch (Throwable t) {
                            SipAudioCall.this.loge("onCallEstablished(): ", t);
                        }
                    }
                    return;
                }
                SipAudioCall.this.transferToNewSession();
            }

            public void onCallEnded(SipSession session) {
                SipAudioCall sipAudioCall = SipAudioCall.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onCallEnded: ");
                stringBuilder.append(session);
                stringBuilder.append(" mSipSession:");
                stringBuilder.append(SipAudioCall.this.mSipSession);
                sipAudioCall.log(stringBuilder.toString());
                if (session == SipAudioCall.this.mTransferringSession) {
                    SipAudioCall.this.mTransferringSession = null;
                } else if (SipAudioCall.this.mTransferringSession == null && session == SipAudioCall.this.mSipSession) {
                    Listener listener = SipAudioCall.this.mListener;
                    if (listener != null) {
                        try {
                            listener.onCallEnded(SipAudioCall.this);
                        } catch (Throwable t) {
                            SipAudioCall.this.loge("onCallEnded(): ", t);
                        }
                    }
                    SipAudioCall.this.close();
                }
            }

            public void onCallBusy(SipSession session) {
                SipAudioCall sipAudioCall = SipAudioCall.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onCallBusy: ");
                stringBuilder.append(session);
                sipAudioCall.log(stringBuilder.toString());
                Listener listener = SipAudioCall.this.mListener;
                if (listener != null) {
                    try {
                        listener.onCallBusy(SipAudioCall.this);
                    } catch (Throwable t) {
                        SipAudioCall.this.loge("onCallBusy(): ", t);
                    }
                }
                SipAudioCall.this.close(SipAudioCall.DONT_RELEASE_SOCKET);
            }

            public void onCallChangeFailed(SipSession session, int errorCode, String message) {
                SipAudioCall sipAudioCall = SipAudioCall.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onCallChangedFailed: ");
                stringBuilder.append(message);
                sipAudioCall.log(stringBuilder.toString());
                SipAudioCall.this.mErrorCode = errorCode;
                SipAudioCall.this.mErrorMessage = message;
                Listener listener = SipAudioCall.this.mListener;
                if (listener != null) {
                    try {
                        listener.onError(SipAudioCall.this, SipAudioCall.this.mErrorCode, message);
                    } catch (Throwable t) {
                        SipAudioCall.this.loge("onCallBusy():", t);
                    }
                }
            }

            public void onError(SipSession session, int errorCode, String message) {
                SipAudioCall.this.onError(errorCode, message);
            }

            public void onRegistering(SipSession session) {
            }

            public void onRegistrationTimeout(SipSession session) {
            }

            public void onRegistrationFailed(SipSession session, int errorCode, String message) {
            }

            public void onRegistrationDone(SipSession session, int duration) {
            }

            public void onCallTransferring(SipSession newSession, String sessionDescription) {
                SipAudioCall sipAudioCall = SipAudioCall.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onCallTransferring: mSipSession=");
                stringBuilder.append(SipAudioCall.this.mSipSession);
                stringBuilder.append(" newSession=");
                stringBuilder.append(newSession);
                sipAudioCall.log(stringBuilder.toString());
                SipAudioCall.this.mTransferringSession = newSession;
                if (sessionDescription == null) {
                    try {
                        newSession.makeCall(newSession.getPeerProfile(), SipAudioCall.this.createOffer().encode(), SipAudioCall.TRANSFER_TIMEOUT);
                        return;
                    } catch (Throwable e) {
                        SipAudioCall.this.loge("onCallTransferring()", e);
                        newSession.endCall();
                        return;
                    }
                }
                newSession.answerCall(SipAudioCall.this.createAnswer(sessionDescription).encode(), 5);
            }
        };
    }

    private void onError(int errorCode, String message) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onError: ");
        stringBuilder.append(SipErrorCode.toString(errorCode));
        stringBuilder.append(": ");
        stringBuilder.append(message);
        log(stringBuilder.toString());
        this.mErrorCode = errorCode;
        this.mErrorMessage = message;
        Listener listener = this.mListener;
        if (listener != null) {
            try {
                listener.onError(this, errorCode, message);
            } catch (Throwable t) {
                loge("onError():", t);
            }
        }
        synchronized (this) {
            if (errorCode != -10) {
                try {
                    if (!isInCall()) {
                    }
                } finally {
                }
            }
            close(true);
        }
    }

    public void attachCall(SipSession session, String sessionDescription) throws SipException {
        if (SipManager.isVoipSupported(this.mContext)) {
            synchronized (this) {
                this.mSipSession = session;
                this.mPeerSd = sessionDescription;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("attachCall(): ");
                stringBuilder.append(this.mPeerSd);
                log(stringBuilder.toString());
                try {
                    session.setListener(createListener());
                } catch (Throwable e) {
                    loge("attachCall()", e);
                    throwSipException(e);
                }
            }
            return;
        }
        throw new SipException("VOIP API is not supported");
    }

    public void makeCall(SipProfile peerProfile, SipSession sipSession, int timeout) throws SipException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("makeCall: ");
        stringBuilder.append(peerProfile);
        stringBuilder.append(" session=");
        stringBuilder.append(sipSession);
        stringBuilder.append(" timeout=");
        stringBuilder.append(timeout);
        log(stringBuilder.toString());
        if (SipManager.isVoipSupported(this.mContext)) {
            synchronized (this) {
                this.mSipSession = sipSession;
                try {
                    this.mAudioStream = new AudioStream(InetAddress.getByName(getLocalIp()));
                    sipSession.setListener(createListener());
                    sipSession.makeCall(peerProfile, createOffer().encode(), timeout);
                } catch (IOException e) {
                    loge("makeCall:", e);
                    throw new SipException("makeCall()", e);
                }
            }
            return;
        }
        throw new SipException("VOIP API is not supported");
    }

    public void endCall() throws SipException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("endCall: mSipSession");
        stringBuilder.append(this.mSipSession);
        log(stringBuilder.toString());
        synchronized (this) {
            stopCall(true);
            this.mInCall = DONT_RELEASE_SOCKET;
            if (this.mSipSession != null) {
                this.mSipSession.endCall();
            }
        }
    }

    public void holdCall(int timeout) throws SipException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("holdCall: mSipSession");
        stringBuilder.append(this.mSipSession);
        stringBuilder.append(" timeout=");
        stringBuilder.append(timeout);
        log(stringBuilder.toString());
        synchronized (this) {
            if (this.mHold) {
            } else if (this.mSipSession != null) {
                this.mSipSession.changeCall(createHoldOffer().encode(), timeout);
                this.mHold = true;
                setAudioGroupMode();
            } else {
                loge("holdCall:");
                throw new SipException("Not in a call to hold call");
            }
        }
    }

    public void answerCall(int timeout) throws SipException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("answerCall: mSipSession");
        stringBuilder.append(this.mSipSession);
        stringBuilder.append(" timeout=");
        stringBuilder.append(timeout);
        log(stringBuilder.toString());
        synchronized (this) {
            if (this.mSipSession != null) {
                try {
                    this.mAudioStream = new AudioStream(InetAddress.getByName(getLocalIp()));
                    this.mSipSession.answerCall(createAnswer(this.mPeerSd).encode(), timeout);
                } catch (IOException e) {
                    loge("answerCall:", e);
                    throw new SipException("answerCall()", e);
                }
            }
            throw new SipException("No call to answer");
        }
    }

    public void continueCall(int timeout) throws SipException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("continueCall: mSipSession");
        stringBuilder.append(this.mSipSession);
        stringBuilder.append(" timeout=");
        stringBuilder.append(timeout);
        log(stringBuilder.toString());
        synchronized (this) {
            if (this.mHold) {
                this.mSipSession.changeCall(createContinueOffer().encode(), timeout);
                this.mHold = DONT_RELEASE_SOCKET;
                setAudioGroupMode();
                return;
            }
        }
    }

    private SimpleSessionDescription createOffer() {
        SimpleSessionDescription offer = new SimpleSessionDescription(this.mSessionId, getLocalIp());
        AudioCodec[] codecs = AudioCodec.getCodecs();
        Media media = offer.newMedia("audio", this.mAudioStream.getLocalPort(), 1, "RTP/AVP");
        for (AudioCodec codec : AudioCodec.getCodecs()) {
            media.setRtpPayload(codec.type, codec.rtpmap, codec.fmtp);
        }
        media.setRtpPayload(127, "telephone-event/8000", "0-15");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createOffer: offer=");
        stringBuilder.append(offer);
        log(stringBuilder.toString());
        return offer;
    }

    private SimpleSessionDescription createAnswer(String offerSd) {
        if (TextUtils.isEmpty(offerSd)) {
            return createOffer();
        }
        SimpleSessionDescription offer = new SimpleSessionDescription(offerSd);
        SimpleSessionDescription answer = new SimpleSessionDescription(this.mSessionId, getLocalIp());
        AudioCodec codec = null;
        for (Media media : offer.getMedia()) {
            Media reply;
            String rtpmap;
            if (codec == null && media.getPort() > 0 && "audio".equals(media.getType()) && "RTP/AVP".equals(media.getProtocol())) {
                AudioCodec codec2 = codec;
                for (int type : media.getRtpPayloadTypes()) {
                    codec2 = AudioCodec.getCodec(type, media.getRtpmap(type), media.getFmtp(type));
                    if (codec2 != null) {
                        break;
                    }
                }
                codec = codec2;
                if (codec != null) {
                    reply = answer.newMedia("audio", this.mAudioStream.getLocalPort(), 1, "RTP/AVP");
                    reply.setRtpPayload(codec.type, codec.rtpmap, codec.fmtp);
                    for (int type2 : media.getRtpPayloadTypes()) {
                        rtpmap = media.getRtpmap(type2);
                        if (!(type2 == codec.type || rtpmap == null || !rtpmap.startsWith("telephone-event"))) {
                            reply.setRtpPayload(type2, rtpmap, media.getFmtp(type2));
                        }
                    }
                    if (media.getAttribute("recvonly") != null) {
                        answer.setAttribute("sendonly", "");
                    } else if (media.getAttribute("sendonly") != null) {
                        answer.setAttribute("recvonly", "");
                    } else if (offer.getAttribute("recvonly") != null) {
                        answer.setAttribute("sendonly", "");
                    } else if (offer.getAttribute("sendonly") != null) {
                        answer.setAttribute("recvonly", "");
                    }
                }
            }
            reply = answer.newMedia(media.getType(), 0, 1, media.getProtocol());
            for (String rtpmap2 : media.getFormats()) {
                reply.setFormat(rtpmap2, null);
            }
        }
        if (codec != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createAnswer: answer=");
            stringBuilder.append(answer);
            log(stringBuilder.toString());
            return answer;
        }
        loge("createAnswer: no suitable codes");
        throw new IllegalStateException("Reject SDP: no suitable codecs");
    }

    private SimpleSessionDescription createHoldOffer() {
        SimpleSessionDescription offer = createContinueOffer();
        offer.setAttribute("sendonly", "");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createHoldOffer: offer=");
        stringBuilder.append(offer);
        log(stringBuilder.toString());
        return offer;
    }

    private SimpleSessionDescription createContinueOffer() {
        log("createContinueOffer");
        SimpleSessionDescription offer = new SimpleSessionDescription(this.mSessionId, getLocalIp());
        Media media = offer.newMedia("audio", this.mAudioStream.getLocalPort(), 1, "RTP/AVP");
        AudioCodec codec = this.mAudioStream.getCodec();
        media.setRtpPayload(codec.type, codec.rtpmap, codec.fmtp);
        int dtmfType = this.mAudioStream.getDtmfType();
        if (dtmfType != -1) {
            media.setRtpPayload(dtmfType, "telephone-event/8000", "0-15");
        }
        return offer;
    }

    private void grabWifiHighPerfLock() {
        if (this.mWifiHighPerfLock == null) {
            log("grabWifiHighPerfLock:");
            this.mWifiHighPerfLock = ((WifiManager) this.mContext.getSystemService("wifi")).createWifiLock(3, LOG_TAG);
            this.mWifiHighPerfLock.acquire();
        }
    }

    private void releaseWifiHighPerfLock() {
        if (this.mWifiHighPerfLock != null) {
            log("releaseWifiHighPerfLock:");
            this.mWifiHighPerfLock.release();
            this.mWifiHighPerfLock = null;
        }
    }

    private boolean isWifiOn() {
        return this.mWm.getConnectionInfo().getBSSID() == null ? DONT_RELEASE_SOCKET : true;
    }

    public void toggleMute() {
        synchronized (this) {
            this.mMuted ^= 1;
            setAudioGroupMode();
        }
    }

    public boolean isMuted() {
        boolean z;
        synchronized (this) {
            z = this.mMuted;
        }
        return z;
    }

    public void setSpeakerMode(boolean speakerMode) {
        synchronized (this) {
            ((AudioManager) this.mContext.getSystemService("audio")).setSpeakerphoneOn(speakerMode);
            setAudioGroupMode();
        }
    }

    private boolean isSpeakerOn() {
        return ((AudioManager) this.mContext.getSystemService("audio")).isSpeakerphoneOn();
    }

    public void sendDtmf(int code) {
        sendDtmf(code, null);
    }

    public void sendDtmf(int code, Message result) {
        synchronized (this) {
            AudioGroup audioGroup = getAudioGroup();
            if (!(audioGroup == null || this.mSipSession == null || 8 != getState())) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendDtmf: code=");
                stringBuilder.append(code);
                stringBuilder.append(" result=");
                stringBuilder.append(result);
                log(stringBuilder.toString());
                audioGroup.sendDtmf(code);
            }
            if (result != null) {
                result.sendToTarget();
            }
        }
    }

    public AudioStream getAudioStream() {
        AudioStream audioStream;
        synchronized (this) {
            audioStream = this.mAudioStream;
        }
        return audioStream;
    }

    /* JADX WARNING: Missing block: B:12:0x0016, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public AudioGroup getAudioGroup() {
        synchronized (this) {
            AudioGroup audioGroup;
            if (this.mAudioGroup != null) {
                audioGroup = this.mAudioGroup;
                return audioGroup;
            }
            audioGroup = this.mAudioStream == null ? null : this.mAudioStream.getGroup();
        }
    }

    public void setAudioGroup(AudioGroup group) {
        synchronized (this) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setAudioGroup: group=");
            stringBuilder.append(group);
            log(stringBuilder.toString());
            if (!(this.mAudioStream == null || this.mAudioStream.getGroup() == null)) {
                this.mAudioStream.join(group);
            }
            this.mAudioGroup = group;
        }
    }

    public void startAudio() {
        try {
            startAudioInternal();
        } catch (UnknownHostException e) {
            onError(-7, e.getMessage());
        } catch (Throwable e2) {
            onError(-4, e2.getMessage());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:64:0x011d A:{SYNTHETIC, Splitter:B:64:0x011d} */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00fb  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void startAudioInternal() throws UnknownHostException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startAudioInternal: mPeerSd=");
        stringBuilder.append(this.mPeerSd);
        loge(stringBuilder.toString());
        if (this.mPeerSd != null) {
            stopCall(DONT_RELEASE_SOCKET);
            this.mInCall = true;
            SimpleSessionDescription offer = new SimpleSessionDescription(this.mPeerSd);
            AudioStream stream = this.mAudioStream;
            AudioCodec codec = null;
            String address;
            for (Media media : offer.getMedia()) {
                if (codec == null && media.getPort() > 0 && "audio".equals(media.getType()) && "RTP/AVP".equals(media.getProtocol())) {
                    AudioCodec codec2 = codec;
                    for (int type : media.getRtpPayloadTypes()) {
                        codec2 = AudioCodec.getCodec(type, media.getRtpmap(type), media.getFmtp(type));
                        if (codec2 != null) {
                            break;
                        }
                    }
                    codec = codec2;
                    if (codec != null) {
                        address = media.getAddress();
                        if (address == null) {
                            address = offer.getAddress();
                        }
                        stream.associate(InetAddress.getByName(address), media.getPort());
                        stream.setDtmfType(-1);
                        stream.setCodec(codec);
                        for (int i : media.getRtpPayloadTypes()) {
                            String rtpmap = media.getRtpmap(i);
                            if (!(i == codec.type || rtpmap == null || !rtpmap.startsWith("telephone-event"))) {
                                stream.setDtmfType(i);
                            }
                        }
                        if (this.mHold) {
                            stream.setMode(0);
                        } else if (media.getAttribute("recvonly") != null) {
                            stream.setMode(1);
                        } else if (media.getAttribute("sendonly") != null) {
                            stream.setMode(2);
                        } else if (offer.getAttribute("recvonly") != null) {
                            stream.setMode(1);
                        } else if (offer.getAttribute("sendonly") != null) {
                            stream.setMode(2);
                        } else {
                            stream.setMode(0);
                        }
                        if (codec == null) {
                            if (isWifiOn()) {
                                grabWifiHighPerfLock();
                            }
                            AudioGroup audioGroup = getAudioGroup();
                            if (!this.mHold) {
                                if (audioGroup == null) {
                                    audioGroup = new AudioGroup();
                                }
                                stream.join(audioGroup);
                            }
                            setAudioGroupMode();
                        } else {
                            throw new IllegalStateException("Reject SDP: no suitable codecs");
                        }
                    }
                }
            }
            if (codec == null) {
            }
        } else {
            throw new IllegalStateException("mPeerSd = null");
        }
    }

    private void setAudioGroupMode() {
        AudioGroup audioGroup = getAudioGroup();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setAudioGroupMode: audioGroup=");
        stringBuilder.append(audioGroup);
        log(stringBuilder.toString());
        if (audioGroup == null) {
            return;
        }
        if (this.mHold) {
            audioGroup.setMode(0);
        } else if (this.mMuted) {
            audioGroup.setMode(1);
        } else if (isSpeakerOn()) {
            audioGroup.setMode(3);
        } else {
            audioGroup.setMode(2);
        }
    }

    private void stopCall(boolean releaseSocket) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopCall: releaseSocket=");
        stringBuilder.append(releaseSocket);
        log(stringBuilder.toString());
        releaseWifiHighPerfLock();
        if (this.mAudioStream != null) {
            this.mAudioStream.join(null);
            if (releaseSocket) {
                this.mAudioStream.release();
                this.mAudioStream = null;
            }
        }
    }

    private String getLocalIp() {
        return this.mSipSession.getLocalIp();
    }

    private void throwSipException(Throwable throwable) throws SipException {
        if (throwable instanceof SipException) {
            throw ((SipException) throwable);
        }
        throw new SipException("", throwable);
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, s);
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, s);
    }

    private void loge(String s, Throwable t) {
        Rlog.e(LOG_TAG, s, t);
    }
}
