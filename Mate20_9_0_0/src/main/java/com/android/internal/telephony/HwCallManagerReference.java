package com.android.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.AbstractCallManager.CallManagerReference;
import com.android.internal.telephony.AbstractCallManager.disconnectCallback;
import com.android.internal.telephony.PhoneConstants.State;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.uicc.IccUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class HwCallManagerReference extends Handler implements CallManagerReference {
    private static final int EVENT_DISCONNECT = 102;
    private static final int EVENT_HUAWEI_BASE = 500;
    private static final int EVENT_HW_BUFFER_SOLICITED = 502;
    private static final int EVENT_HW_BUFFER_UNSOLICITED = 501;
    private static final int EVENT_PHONE_STATE_CHANGED = 101;
    private static final int EVENT_RESET_SPEECH_INFO_DB_DELAYED = 503;
    private static final int EVENT_UNSOl_SPEECH_INFO = 103;
    private static final boolean IS_AMR_WB_SHOW_HD = SystemProperties.getBoolean("ro.config.amr_wb_show_hd", false);
    private static final String LOG_TAG = "HwCallManagerReference";
    public static final String OPERATOR_CUSTOMER_WB_SHOW_HD = "wb_show_hd";
    private static final int RESET_WB_DB_TIME = 200;
    private static final boolean VDBG = true;
    private static boolean mIsLocalHangupSpeedUp = SystemProperties.get("ro.config.hw_hangup_speedup", "true").equals("true");
    private static final boolean mSupportAdjustSpeechCodec = HwModemCapability.isCapabilitySupport(11);
    private int mActiveSub = 0;
    protected final RegistrantList mActiveSubChangeRegistrants = new RegistrantList();
    private CallManager mCM;
    protected disconnectCallback mCallbackCallNotifier;
    protected disconnectCallback mCallbackInCallScreen;
    public final RegistrantList mEncryptCallRegistrants = new RegistrantList();

    public static class HWBuffer {
        public static final int BUFFER_SIZE = 120;
        public static final int BUFLEN_SIZE = 1;
        public static final int EVENT_SIZE = 4;
        public static final String LOG_TAG = "CM_HWBUFFER";
        public byte[] buffer = null;
        public int event;
        public Throwable exception = null;
        private boolean isAvailable = false;
        public byte length = (byte) 0;
        public Message reqMsg = null;

        public HWBuffer(int event, byte[] buffer) {
            int i = 0;
            this.event = event;
            if (buffer != null) {
                i = buffer.length;
            }
            this.length = (byte) i;
            if (this.length > (byte) 0 && (byte) 120 >= this.length) {
                this.buffer = Arrays.copyOf(buffer, buffer.length);
            }
            if (-1 < event) {
                this.isAvailable = true;
            }
        }

        public boolean isAvail() {
            return this.isAvailable;
        }

        public boolean isSolicited() {
            return this.reqMsg != null;
        }

        public boolean isException() {
            return this.exception != null;
        }

        public byte[] toArray() {
            if (isAvail()) {
                ByteBuffer buf = ByteBuffer.wrap(new byte[(5 + this.length)]);
                if (HuaweiTelephonyConfigs.isHisiPlatform()) {
                    buf.order(ByteOrder.nativeOrder());
                }
                buf.putInt(this.event);
                buf.put(this.length);
                if (this.length > (byte) 0 && (byte) 120 >= this.length) {
                    buf.put(this.buffer);
                }
                return buf.array();
            }
            Rlog.e(LOG_TAG, "this(HWBuffer) is unavailable!!!");
            return new byte[0];
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HWBuffer>>>{ event:");
            stringBuilder.append(this.event);
            stringBuilder.append(", length:");
            stringBuilder.append(this.length);
            stringBuilder.append(", buffer:");
            stringBuilder.append(IccUtils.bytesToHexString(this.buffer));
            stringBuilder.append(" }.");
            return stringBuilder.toString();
        }

        public void sendRespToTarget(Object obj) {
            if (isAvail() && isSolicited() && this.reqMsg.getTarget() != null) {
                this.reqMsg.obj = obj;
                this.reqMsg.sendToTarget();
                return;
            }
            Rlog.v(LOG_TAG, "sendRespToTarget() Failed, HWBuffer not available OR reqMsg not found Target(@Handle)!");
        }

        public static HWBuffer makeHWBuffer(byte[] data) {
            return makeHWBuffer(data, false);
        }

        static HWBuffer makeHWBuffer(byte[] data, boolean isOrder) {
            if (data == null) {
                return null;
            }
            byte[] _buffer = null;
            try {
                ByteBuffer byteData = ByteBuffer.wrap(data);
                if (isOrder) {
                    byteData.order(ByteOrder.nativeOrder());
                }
                int _event = byteData.getInt();
                byte _length = byteData.get();
                if (_length > (byte) 0) {
                    _buffer = new byte[_length];
                    byteData.get(_buffer, 0, _length);
                }
                return new HWBuffer(_event, _buffer);
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "data[] parse execption, please check data format>>>[event(4):buf_len(1):buffer(<120)] !!!");
                HWBuffer hwBuf = new HWBuffer();
                hwBuf.exception = e;
                return hwBuf;
            }
        }

        public static HWBuffer makeHWBuffer(AsyncResult ar) {
            return makeHWBuffer(ar, false);
        }

        static HWBuffer makeHWBuffer(AsyncResult ar, boolean isOrder) {
            HWBuffer hwBuf = makeHWBuffer((byte[]) ar.result, isOrder);
            if (hwBuf == null) {
                hwBuf = new HWBuffer();
            }
            if (hwBuf.isAvail()) {
                if (ar.exception != null) {
                    hwBuf.exception = ar.exception;
                }
                if (ar.userObj != null) {
                    hwBuf.reqMsg = (Message) ar.userObj;
                }
            }
            return hwBuf;
        }
    }

    public HwCallManagerReference(CallManager cm) {
        this.mCM = cm;
    }

    public void setInCallScreenDisconnectCallback(disconnectCallback callback) {
        this.mCallbackInCallScreen = callback;
    }

    public void setCallNotifierDisconnectCallback(disconnectCallback callback) {
        this.mCallbackCallNotifier = callback;
    }

    public void inCallScreenDisconnectNotify(AsyncResult r) {
        if (this.mCallbackInCallScreen != null) {
            this.mCallbackInCallScreen.disconnectNotify(r);
        }
    }

    public void calllNotifierDisconnectNotify(AsyncResult r) {
        if (this.mCallbackCallNotifier != null) {
            this.mCallbackCallNotifier.disconnectNotify(r);
        }
    }

    public void disconnectNotify(Message msg) {
        if (mIsLocalHangupSpeedUp) {
            inCallScreenDisconnectNotify((AsyncResult) msg.obj);
            calllNotifierDisconnectNotify((AsyncResult) msg.obj);
        }
    }

    public void registerForEncryptedCall(Handler h, int what, Object obj) {
        this.mEncryptCallRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForEncryptedCall(Handler h) {
        this.mEncryptCallRegistrants.remove(h);
    }

    public void cmdForEncryptedCall(Phone phone, int cmd, byte[] reqData) {
        cmdForEncryptedCall(phone, null, cmd, reqData);
    }

    private void cmdForEncryptedCall(Phone phone, Message reqMsg, int cmd, byte[] reqData) {
        sendHWSolicited(phone, reqMsg, 0, new HWBuffer(cmd, reqData).toArray());
    }

    public void resultForKMCRemoteCmd(Phone phone, int cmd, int reqData) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("resultForKMCRemoteCmd() cmd ");
        stringBuilder.append(cmd);
        stringBuilder.append(" reqdata ");
        stringBuilder.append(reqData);
        Rlog.d(str, stringBuilder.toString());
        resultForKMCRemoteCmd(phone, null, cmd, new byte[]{(byte) reqData});
    }

    private void resultForKMCRemoteCmd(Phone phone, Message reqMsg, int cmd, byte[] reqData) {
        sendHWSolicited(phone, reqMsg, 1, new HWBuffer(cmd, reqData).toArray());
    }

    public void setConnEncryptCallByNumber(Phone phone, String number, boolean val) {
        if (phone.getPhoneType() == 2) {
            ((GsmCdmaCallTracker) ((GsmCdmaPhone) phone).getCallTracker()).setConnEncryptCallByNumber(number, val);
        }
    }

    private void processSolicitedHWResponse(AsyncResult ar) {
        HWBuffer hwBuf = HWBuffer.makeHWBuffer(ar, true);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("received SolicitedHWResponse hwBuf:");
        stringBuilder.append(hwBuf.toString());
        Rlog.v(str, stringBuilder.toString());
        if (hwBuf.isAvail()) {
            switch (hwBuf.event) {
                case 0:
                case 1:
                    Rlog.d(LOG_TAG, "[Solicited] HW_ENCRYPT_CALL or HW_KMC_REMOTE_COMMUNICATION.");
                    hwBuf.sendRespToTarget(hwBuf.buffer);
                    return;
                default:
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("CallManager be untreated for event[");
                    stringBuilder.append(hwBuf.event);
                    stringBuilder.append("] in processSolicitedHWResponse()! please user handle");
                    Rlog.w(str, stringBuilder.toString());
                    hwBuf.sendRespToTarget(hwBuf.buffer);
                    return;
            }
        }
        Rlog.e(LOG_TAG, "processSolicitedHWResponse(ar) is unavailable!!!");
    }

    private void processUnSolicitedHWResponse(AsyncResult ar) {
        HWBuffer hwBuf = HWBuffer.makeHWBuffer(ar, true);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("received UnSolicitedHWResponse hwBuf:");
        stringBuilder.append(hwBuf.toString());
        Rlog.v(str, stringBuilder.toString());
        if (hwBuf.isAvail()) {
            switch (hwBuf.event) {
                case 0:
                case 1:
                    Rlog.d(LOG_TAG, "[UnSolicited] HW_ENCRYPT_CALL or HW_KMC_REMOTE_COMMUNICATION.");
                    this.mEncryptCallRegistrants.notifyRegistrants(new AsyncResult(null, hwBuf.buffer, hwBuf.exception));
                    return;
                default:
                    Rlog.w(LOG_TAG, "not found event for processUnSolicitedHWResponse()");
                    return;
            }
        }
        Rlog.e(LOG_TAG, "processUnSolicitedHWResponse(ar) is unavailable!!!");
    }

    private void sendHWSolicited(Phone phone, Message reqMsg, int event, byte[] reqData) {
        if (phone == null) {
            Rlog.w(LOG_TAG, "sendHWSolicited() phone parameter is invalid");
            return;
        }
        phone.sendHWSolicited(obtainMessage(EVENT_HW_BUFFER_SOLICITED, reqMsg), event, reqData);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendHWSolicited completed, phone: ");
        stringBuilder.append(phone);
        stringBuilder.append(", event=");
        stringBuilder.append(event);
        Rlog.d(str, stringBuilder.toString());
    }

    public void handleMessage(Message msg) {
        int i = msg.what;
        switch (i) {
            case EVENT_PHONE_STATE_CHANGED /*101*/:
                Rlog.d(LOG_TAG, " handleMessage (EVENT_PHONE_STATE_CHANGED)");
                onPhoneStateChanged((AsyncResult) msg.obj);
                return;
            case EVENT_DISCONNECT /*102*/:
                Rlog.d(LOG_TAG, " handleMessage (EVENT_DISCONNECT)");
                onEventDisconnect((AsyncResult) msg.obj);
                return;
            case EVENT_UNSOl_SPEECH_INFO /*103*/:
                Rlog.d(LOG_TAG, " handleMessage (EVENT_UNSOl_SPEECH_INFO)");
                onUnsoSpeechInfo((AsyncResult) msg.obj);
                return;
            default:
                switch (i) {
                    case EVENT_HW_BUFFER_UNSOLICITED /*501*/:
                        Rlog.d(LOG_TAG, "handleMessage (EVENT_HW_BUFFER_UNSOLICITED)");
                        processUnSolicitedHWResponse(msg.obj);
                        return;
                    case EVENT_HW_BUFFER_SOLICITED /*502*/:
                        Rlog.d(LOG_TAG, "handleMessage (EVENT_HW_BUFFER_SOLICITED)");
                        AsyncResult soAr = msg.obj;
                        if (!HuaweiTelephonyConfigs.isHisiPlatform()) {
                            processSolicitedHWResponse(soAr);
                            return;
                        } else if (soAr.exception == null) {
                            Rlog.i(LOG_TAG, "encrypted call SOLICITED res is ok");
                            return;
                        } else {
                            String str = LOG_TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("encrypted call SOLICITED exception ");
                            stringBuilder.append(soAr.exception);
                            Rlog.e(str, stringBuilder.toString());
                            return;
                        }
                    case EVENT_RESET_SPEECH_INFO_DB_DELAYED /*503*/:
                        Phone phone = msg.obj;
                        Rlog.d(LOG_TAG, "handleMessage (EVENT_RESET_SPEECH_INFO_DB_DELAYED)");
                        if (phone != null && phone.getContext() != null) {
                            System.putInt(phone.getContext().getContentResolver(), OPERATOR_CUSTOMER_WB_SHOW_HD, 1);
                            return;
                        }
                        return;
                    default:
                        return;
                }
        }
    }

    private void onEventDisconnect(AsyncResult ar) {
        if (ar != null) {
            Phone phone = ar.userObj;
            if (phone != null) {
                int i = 1;
                if (phone.getState() == State.IDLE) {
                    phone.setSpeechInfoCodec(1);
                    if (IS_AMR_WB_SHOW_HD) {
                        sendMessageDelayed(obtainMessage(EVENT_RESET_SPEECH_INFO_DB_DELAYED, phone), 200);
                        Rlog.d(LOG_TAG, "onEventDisconnect: voice call end, Reset speechCodec to NB after 200ms.");
                    }
                }
                int disConnectSub = phone.getSubId();
                if (disConnectSub != 0) {
                    i = 0;
                }
                int otherSub = i;
                State otherState = this.mCM.getState(otherSub);
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DisConnectSub : ");
                stringBuilder.append(disConnectSub);
                stringBuilder.append(", otherSub : ");
                stringBuilder.append(otherSub);
                stringBuilder.append(", otherState :");
                stringBuilder.append(otherState);
                Rlog.d(str, stringBuilder.toString());
                if (otherState == State.OFFHOOK) {
                    setAudioParameters(getPhone((long) otherSub));
                }
            }
        }
    }

    private void onUnsoSpeechInfo(AsyncResult ar) {
        if (ar != null) {
            Phone phone = ar.userObj;
            if (phone != null) {
                int subId = phone.getSubId();
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("subid : ");
                stringBuilder.append(subId);
                stringBuilder.append(", mActiveSub : ");
                stringBuilder.append(this.mActiveSub);
                Rlog.d(str, stringBuilder.toString());
                int[] intResult = ar.result;
                if (intResult == null || intResult.length == 0) {
                    if (IS_AMR_WB_SHOW_HD) {
                        Rlog.d(LOG_TAG, "intResult==null or intResult.length == 0, reset SpeechCodec to NB.");
                        setSpeechCodec(1, false, phone);
                    }
                    return;
                }
                int speechCodec = intResult[0];
                if (IS_AMR_WB_SHOW_HD) {
                    boolean isSpeechCodecWB = false;
                    if (2 == speechCodec) {
                        isSpeechCodecWB = true;
                    }
                    String str2 = LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("speechCodec : ");
                    stringBuilder2.append(speechCodec);
                    stringBuilder2.append(", isSpeechCodecWB : ");
                    stringBuilder2.append(isSpeechCodecWB);
                    stringBuilder2.append(", phoneId:");
                    stringBuilder2.append(phone.getPhoneId());
                    Rlog.d(str2, stringBuilder2.toString());
                    setSpeechCodec(speechCodec, isSpeechCodecWB, phone);
                }
                phone.setSpeechInfoCodec(speechCodec);
                if (subId == this.mActiveSub) {
                    setAudioParameters(phone);
                }
            }
        }
    }

    public void setSpeechCodec(int speechCodec, boolean isSpeechCodecWB, Phone phone) {
        if (phone == null) {
            Rlog.e(LOG_TAG, "setSpeechCode: phone is null , return .");
            return;
        }
        System.putInt(phone.getContext().getContentResolver(), OPERATOR_CUSTOMER_WB_SHOW_HD, speechCodec);
        Intent intent = new Intent("com.huawei.intent.action.SPEECH_CODEC_WB");
        intent.addFlags(536870912);
        intent.putExtra("speechCodecWb", speechCodec);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, phone.getPhoneId());
        phone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void registerForSubscriptionChange(Handler h, int what, Object obj) {
        this.mActiveSubChangeRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForSubscriptionChange(Handler h) {
        this.mActiveSubChangeRegistrants.remove(h);
    }

    public void setActiveSubscription(int subscription) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setActiveSubscription existing:");
        stringBuilder.append(this.mActiveSub);
        stringBuilder.append("new = ");
        stringBuilder.append(subscription);
        Rlog.d(str, stringBuilder.toString());
        this.mActiveSub = subscription;
        this.mActiveSubChangeRegistrants.notifyRegistrants(new AsyncResult(null, Integer.valueOf(this.mActiveSub), null));
    }

    public int getActiveSubscription() {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getActiveSubscription  = ");
        stringBuilder.append(this.mActiveSub);
        Rlog.d(str, stringBuilder.toString());
        return this.mActiveSub;
    }

    public void registerForPhoneStates(Phone phone) {
        phone.registerForHWBuffer(this, EVENT_HW_BUFFER_UNSOLICITED, null);
        if (mSupportAdjustSpeechCodec) {
            phone.registerForPreciseCallStateChanged(this, EVENT_PHONE_STATE_CHANGED, null);
            phone.registerForDisconnect(this, EVENT_DISCONNECT, phone);
            phone.registerForUnsolSpeechInfo(this, EVENT_UNSOl_SPEECH_INFO, phone);
        }
    }

    public void unregisterForPhoneStates(Phone phone) {
        phone.unregisterForHWBuffer(this);
        if (mSupportAdjustSpeechCodec) {
            phone.unregisterForPreciseCallStateChanged(this);
            phone.unregisterForUnsolSpeechInfo(this);
            phone.unregisterForDisconnect(this);
        }
    }

    public void onSwitchToOtherActiveSub(Phone currentPhone) {
        if (!mSupportAdjustSpeechCodec) {
            return;
        }
        if (currentPhone == null) {
            Rlog.d(LOG_TAG, "onSwitchToOtherActiveSub currentPhone is NULL! ");
            return;
        }
        int currentSub = currentPhone.getSubId();
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSwitchToOtherActiveSub currentSub = ");
        stringBuilder.append(currentSub);
        stringBuilder.append(" mActiveSub = ");
        stringBuilder.append(this.mActiveSub);
        Rlog.d(str, stringBuilder.toString());
        if (currentSub == this.mActiveSub) {
            State state = this.mCM.getState(this.mActiveSub);
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onSwitchToOtherActiveSub currentSub = ");
            stringBuilder2.append(currentSub);
            stringBuilder2.append(", state = ");
            stringBuilder2.append(state);
            Rlog.d(str2, stringBuilder2.toString());
            if (state == State.OFFHOOK) {
                setAudioParameters(currentPhone);
            }
        }
    }

    private void setAudioParameters(Phone phone) {
        if (phone != null) {
            Context context = phone.getContext();
            String speechInfo = phone.getSpeechInfoCodec();
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setAudioParameters speechInfo = ");
            stringBuilder.append(speechInfo);
            Rlog.d(str, stringBuilder.toString());
            if (!speechInfo.equals("")) {
                ((AudioManager) context.getSystemService("audio")).setParameters(speechInfo);
            }
        }
    }

    private void onPhoneStateChanged(AsyncResult r) {
        if (this.mCM.getState(this.mActiveSub) == State.OFFHOOK) {
            setAudioParameters(getPhone((long) this.mActiveSub));
        }
    }

    private Phone getPhone(long subId) {
        for (Phone phone : this.mCM.getAllPhones()) {
            if (((long) phone.getSubId()) == subId && !(phone instanceof ImsPhone)) {
                return phone;
            }
        }
        return null;
    }
}
