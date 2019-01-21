package com.android.internal.telephony;

import android.os.RemoteException;
import android.telephony.Rlog;
import android.telephony.SmsMessage;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.aidl.IImsSmsListener.Stub;
import android.telephony.ims.feature.ImsFeature.Capabilities;
import android.telephony.ims.feature.ImsFeature.CapabilityCallback;
import android.telephony.ims.stub.ImsRegistrationImplBase.Callback;
import android.util.Pair;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsManager.Connector;
import com.android.ims.ImsManager.Connector.Listener;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.SMSDispatcher.SmsTracker;
import com.android.internal.telephony.SmsMessageBase.SubmitPduBase;
import com.android.internal.telephony.util.SMSDispatcherUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ImsSmsDispatcher extends SMSDispatcher {
    private static final String TAG = "ImsSmsDispacher";
    private CapabilityCallback mCapabilityCallback = new CapabilityCallback() {
        public void onCapabilitiesStatusChanged(Capabilities config) {
            synchronized (ImsSmsDispatcher.this.mLock) {
                ImsSmsDispatcher.this.mIsSmsCapable = config.isCapable(8);
            }
        }
    };
    private final Connector mImsManagerConnector = new Connector(this.mContext, this.mPhone.getPhoneId(), new Listener() {
        public void connectionReady(ImsManager manager) throws ImsException {
            Rlog.d(ImsSmsDispatcher.TAG, "ImsManager: connection ready.");
            ImsSmsDispatcher.this.setListeners();
            synchronized (ImsSmsDispatcher.this.mLock) {
                ImsSmsDispatcher.this.mIsImsServiceUp = true;
            }
        }

        public void connectionUnavailable() {
            Rlog.d(ImsSmsDispatcher.TAG, "ImsManager: connection unavailable.");
            synchronized (ImsSmsDispatcher.this.mLock) {
                ImsSmsDispatcher.this.mIsImsServiceUp = false;
            }
        }
    });
    private final IImsSmsListener mImsSmsListener = new Stub() {
        public void onSendSmsResult(int token, int messageRef, int status, int reason) throws RemoteException {
            String str = ImsSmsDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSendSmsResult token=");
            stringBuilder.append(token);
            stringBuilder.append(" messageRef=");
            stringBuilder.append(messageRef);
            stringBuilder.append(" status=");
            stringBuilder.append(status);
            stringBuilder.append(" reason=");
            stringBuilder.append(reason);
            Rlog.d(str, stringBuilder.toString());
            SmsTracker tracker = (SmsTracker) ImsSmsDispatcher.this.mTrackers.get(Integer.valueOf(token));
            if (tracker != null) {
                tracker.mMessageRef = messageRef;
                switch (status) {
                    case 1:
                        tracker.onSent(ImsSmsDispatcher.this.mContext);
                        return;
                    case 2:
                        tracker.onFailed(ImsSmsDispatcher.this.mContext, reason, 0);
                        ImsSmsDispatcher.this.mTrackers.remove(Integer.valueOf(token));
                        return;
                    case 3:
                        tracker.mRetryCount++;
                        ImsSmsDispatcher.this.sendSms(tracker);
                        return;
                    case 4:
                        ImsSmsDispatcher.this.fallbackToPstn(token, tracker);
                        return;
                    default:
                        return;
                }
            }
            throw new IllegalArgumentException("Invalid token.");
        }

        public void onSmsStatusReportReceived(int token, int messageRef, String format, byte[] pdu) throws RemoteException {
            Rlog.d(ImsSmsDispatcher.TAG, "Status report received.");
            SmsTracker tracker = (SmsTracker) ImsSmsDispatcher.this.mTrackers.get(Integer.valueOf(token));
            if (tracker != null) {
                Pair<Boolean, Boolean> result = ImsSmsDispatcher.this.mSmsDispatchersController.handleSmsStatusReport(tracker, format, pdu);
                String str = ImsSmsDispatcher.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Status report handle result, success: ");
                stringBuilder.append(result.first);
                stringBuilder.append("complete: ");
                stringBuilder.append(result.second);
                Rlog.d(str, stringBuilder.toString());
                try {
                    int i;
                    ImsManager access$300 = ImsSmsDispatcher.this.getImsManager();
                    if (((Boolean) result.first).booleanValue()) {
                        i = 1;
                    } else {
                        i = 2;
                    }
                    access$300.acknowledgeSmsReport(token, messageRef, i);
                } catch (ImsException e) {
                    String str2 = ImsSmsDispatcher.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failed to acknowledgeSmsReport(). Error: ");
                    stringBuilder2.append(e.getMessage());
                    Rlog.e(str2, stringBuilder2.toString());
                }
                if (((Boolean) result.second).booleanValue()) {
                    ImsSmsDispatcher.this.mTrackers.remove(Integer.valueOf(token));
                    return;
                }
                return;
            }
            throw new RemoteException("Invalid token.");
        }

        public void onSmsReceived(int token, String format, byte[] pdu) throws RemoteException {
            Rlog.d(ImsSmsDispatcher.TAG, "SMS received.");
            SmsMessage message = SmsMessage.createFromPdu(pdu, format);
            ImsSmsDispatcher.this.mSmsDispatchersController.injectSmsPdu(message, format, new -$$Lambda$ImsSmsDispatcher$3$q7JFSZBuWsj-jBm5R51WxdJYNxc(this, message, token), true);
        }

        public static /* synthetic */ void lambda$onSmsReceived$0(AnonymousClass3 anonymousClass3, SmsMessage message, int token, int result) {
            int mappedResult;
            String str = ImsSmsDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SMS handled result: ");
            stringBuilder.append(result);
            Rlog.d(str, stringBuilder.toString());
            if (result != 1) {
                switch (result) {
                    case 3:
                        mappedResult = 3;
                        break;
                    case 4:
                        mappedResult = 4;
                        break;
                    default:
                        mappedResult = 2;
                        break;
                }
            }
            mappedResult = 1;
            if (message != null) {
                try {
                    if (message.mWrappedSmsMessage != null) {
                        ImsSmsDispatcher.this.getImsManager().acknowledgeSms(token, message.mWrappedSmsMessage.mMessageRef, mappedResult);
                        return;
                    }
                } catch (ImsException e) {
                    String str2 = ImsSmsDispatcher.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failed to acknowledgeSms(). Error: ");
                    stringBuilder2.append(e.getMessage());
                    Rlog.e(str2, stringBuilder2.toString());
                    return;
                }
            }
            Rlog.w(ImsSmsDispatcher.TAG, "SMS Received with a PDU that could not be parsed.");
            ImsSmsDispatcher.this.getImsManager().acknowledgeSms(token, 0, mappedResult);
        }
    };
    private volatile boolean mIsImsServiceUp;
    private volatile boolean mIsRegistered;
    private volatile boolean mIsSmsCapable;
    private final Object mLock = new Object();
    @VisibleForTesting
    public AtomicInteger mNextToken = new AtomicInteger();
    private Callback mRegistrationCallback = new Callback() {
        public void onRegistered(int imsRadioTech) {
            String str = ImsSmsDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onImsConnected imsRadioTech=");
            stringBuilder.append(imsRadioTech);
            Rlog.d(str, stringBuilder.toString());
            synchronized (ImsSmsDispatcher.this.mLock) {
                ImsSmsDispatcher.this.mIsRegistered = true;
            }
        }

        public void onRegistering(int imsRadioTech) {
            String str = ImsSmsDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onImsProgressing imsRadioTech=");
            stringBuilder.append(imsRadioTech);
            Rlog.d(str, stringBuilder.toString());
            synchronized (ImsSmsDispatcher.this.mLock) {
                ImsSmsDispatcher.this.mIsRegistered = false;
            }
        }

        public void onDeregistered(ImsReasonInfo info) {
            String str = ImsSmsDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onImsDisconnected imsReasonInfo=");
            stringBuilder.append(info);
            Rlog.d(str, stringBuilder.toString());
            synchronized (ImsSmsDispatcher.this.mLock) {
                ImsSmsDispatcher.this.mIsRegistered = false;
            }
        }
    };
    @VisibleForTesting
    public Map<Integer, SmsTracker> mTrackers = new ConcurrentHashMap();

    public ImsSmsDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        super(phone, smsDispatchersController);
        this.mImsManagerConnector.connect();
    }

    private void setListeners() throws ImsException {
        getImsManager().addRegistrationCallback(this.mRegistrationCallback);
        getImsManager().addCapabilitiesCallback(this.mCapabilityCallback);
        getImsManager().setSmsListener(this.mImsSmsListener);
        getImsManager().onSmsReady();
    }

    public boolean isAvailable() {
        boolean z;
        synchronized (this.mLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isAvailable: up=");
            stringBuilder.append(this.mIsImsServiceUp);
            stringBuilder.append(", reg= ");
            stringBuilder.append(this.mIsRegistered);
            stringBuilder.append(", cap= ");
            stringBuilder.append(this.mIsSmsCapable);
            Rlog.d(str, stringBuilder.toString());
            z = this.mIsImsServiceUp && this.mIsRegistered && this.mIsSmsCapable;
        }
        return z;
    }

    protected String getFormat() {
        try {
            return getImsManager().getSmsFormat();
        } catch (ImsException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to get sms format. Error: ");
            stringBuilder.append(e.getMessage());
            Rlog.e(str, stringBuilder.toString());
            return "unknown";
        }
    }

    protected boolean shouldBlockSmsForEcbm() {
        return false;
    }

    protected SubmitPduBase getSubmitPdu(String scAddr, String destAddr, String message, boolean statusReportRequested, SmsHeader smsHeader, int priority, int validityPeriod) {
        return SMSDispatcherUtil.getSubmitPdu(isCdmaMo(), scAddr, destAddr, message, statusReportRequested, smsHeader, priority, validityPeriod);
    }

    protected SubmitPduBase getSubmitPdu(String scAddr, String destAddr, int destPort, byte[] message, boolean statusReportRequested) {
        return SMSDispatcherUtil.getSubmitPdu(isCdmaMo(), scAddr, destAddr, destPort, message, statusReportRequested);
    }

    protected TextEncodingDetails calculateLength(CharSequence messageBody, boolean use7bitOnly) {
        return SMSDispatcherUtil.calculateLength(isCdmaMo(), messageBody, use7bitOnly);
    }

    public void sendSms(SmsTracker tracker) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendSms:  mRetryCount=");
        stringBuilder.append(tracker.mRetryCount);
        stringBuilder.append(" mMessageRef=");
        stringBuilder.append(tracker.mMessageRef);
        stringBuilder.append(" SS=");
        stringBuilder.append(this.mPhone.getServiceState().getState());
        Rlog.d(str, stringBuilder.toString());
        tracker.mUsesImsServiceForIms = true;
        HashMap<String, Object> map = tracker.getData();
        byte[] pdu = (byte[]) map.get("pdu");
        byte[] smsc = (byte[]) map.get("smsc");
        boolean isRetry = tracker.mRetryCount > 0;
        if ("3gpp".equals(getFormat()) && tracker.mRetryCount > 0 && (pdu[0] & 1) == 1) {
            pdu[0] = (byte) (pdu[0] | 4);
            pdu[1] = (byte) tracker.mMessageRef;
        }
        int token = this.mNextToken.incrementAndGet();
        this.mTrackers.put(Integer.valueOf(token), tracker);
        try {
            getImsManager().sendSms(token, tracker.mMessageRef, getFormat(), smsc != null ? new String(smsc) : null, isRetry, pdu);
        } catch (ImsException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("sendSms failed. Falling back to PSTN. Error: ");
            stringBuilder2.append(e.getMessage());
            Rlog.e(str2, stringBuilder2.toString());
            fallbackToPstn(token, tracker);
        }
    }

    private ImsManager getImsManager() {
        return ImsManager.getInstance(this.mContext, this.mPhone.getPhoneId());
    }

    @VisibleForTesting
    public void fallbackToPstn(int token, SmsTracker tracker) {
        this.mSmsDispatchersController.sendRetrySms(tracker);
        this.mTrackers.remove(Integer.valueOf(token));
    }

    protected boolean isCdmaMo() {
        return this.mSmsDispatchersController.isCdmaFormat(getFormat());
    }
}
