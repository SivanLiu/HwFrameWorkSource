package com.android.internal.telephony.gsm;

import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SMSDispatcher.SmsTracker;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase.SubmitPduBase;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.util.SMSDispatcherUtil;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public abstract class GsmSMSDispatcher extends SMSDispatcher {
    private static final int EVENT_NEW_SMS_STATUS_REPORT = 100;
    private static final String TAG = "GsmSMSDispatcher";
    private GsmInboundSmsHandler mGsmInboundSmsHandler;
    private AtomicReference<IccRecords> mIccRecords = new AtomicReference();
    private AtomicReference<UiccCardApplication> mUiccApplication = new AtomicReference();
    protected UiccController mUiccController = null;

    public GsmSMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController, GsmInboundSmsHandler gsmInboundSmsHandler) {
        super(phone, smsDispatchersController);
        this.mCi.setOnSmsStatus(this, 100, null);
        this.mGsmInboundSmsHandler = gsmInboundSmsHandler;
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 15, null);
        Rlog.d(TAG, "GsmSMSDispatcher created");
    }

    public void dispose() {
        super.dispose();
        this.mCi.unSetOnSmsStatus(this);
        this.mUiccController.unregisterForIccChanged(this);
    }

    protected String getFormat() {
        return "3gpp";
    }

    public void handleMessage(Message msg) {
        int i = msg.what;
        if (i != 100) {
            switch (i) {
                case 14:
                    this.mGsmInboundSmsHandler.sendMessage(1, msg.obj);
                    return;
                case 15:
                    onUpdateIccAvailability();
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }
        handleStatusReport((AsyncResult) msg.obj);
    }

    protected boolean shouldBlockSmsForEcbm() {
        return false;
    }

    protected SubmitPduBase getSubmitPdu(String scAddr, String destAddr, String message, boolean statusReportRequested, SmsHeader smsHeader, int priority, int validityPeriod) {
        return SMSDispatcherUtil.getSubmitPduGsm(scAddr, destAddr, message, statusReportRequested, validityPeriod);
    }

    protected SubmitPduBase getSubmitPdu(String scAddr, String destAddr, int destPort, byte[] message, boolean statusReportRequested) {
        return SMSDispatcherUtil.getSubmitPduGsm(scAddr, destAddr, destPort, message, statusReportRequested);
    }

    protected TextEncodingDetails calculateLength(CharSequence messageBody, boolean use7bitOnly) {
        return SMSDispatcherUtil.calculateLengthGsm(messageBody, use7bitOnly);
    }

    private void handleStatusReport(AsyncResult ar) {
        byte[] pdu = ar.result;
        SmsMessage sms = SmsMessage.newFromCDS(pdu);
        if (sms != null) {
            int messageRef = sms.mMessageRef;
            int i = 0;
            int count = this.deliveryPendingList.size();
            while (i < count) {
                SmsTracker tracker = (SmsTracker) this.deliveryPendingList.get(i);
                if (tracker.mMessageRef != messageRef) {
                    i++;
                } else if (((Boolean) this.mSmsDispatchersController.handleSmsStatusReport(tracker, getFormat(), pdu).second).booleanValue()) {
                    this.deliveryPendingList.remove(i);
                }
            }
        }
        this.mCi.acknowledgeLastIncomingGsmSms(true, 1, null);
    }

    protected void sendSms(SmsTracker tracker) {
        String str;
        StringBuilder stringBuilder;
        HashMap<String, Object> map = tracker.getData();
        byte[] pdu = (byte[]) map.get("pdu");
        if (tracker.mRetryCount > 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("sendSms:  mRetryCount=");
            stringBuilder.append(tracker.mRetryCount);
            stringBuilder.append(" mMessageRef=");
            stringBuilder.append(tracker.mMessageRef);
            stringBuilder.append(" SS=");
            stringBuilder.append(this.mPhone.getServiceState().getState());
            Rlog.d(str, stringBuilder.toString());
            if ((pdu[0] & 1) == 1) {
                pdu[0] = (byte) (pdu[0] | 4);
                pdu[1] = (byte) tracker.mMessageRef;
            }
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("sendSms:  isIms()=");
        stringBuilder.append(isIms());
        stringBuilder.append(" mRetryCount=");
        stringBuilder.append(tracker.mRetryCount);
        stringBuilder.append(" mImsRetry=");
        stringBuilder.append(tracker.mImsRetry);
        stringBuilder.append(" mMessageRef=");
        stringBuilder.append(tracker.mMessageRef);
        stringBuilder.append(" mUsesImsServiceForIms=");
        stringBuilder.append(tracker.mUsesImsServiceForIms);
        stringBuilder.append(" SS=");
        stringBuilder.append(this.mPhone.getServiceState().getState());
        Rlog.d(str, stringBuilder.toString());
        int ss = this.mPhone.getServiceState().getState();
        if (isIms() || ss == 0) {
            byte[] smsc = (byte[]) map.get("smsc");
            Message reply = obtainMessage(2, tracker);
            if ((tracker.mImsRetry != 0 || isIms()) && !tracker.mUsesImsServiceForIms) {
                this.mCi.sendImsGsmSms(IccUtils.bytesToHexString(smsc), IccUtils.bytesToHexString(pdu), tracker.mImsRetry, tracker.mMessageRef, reply);
                tracker.mImsRetry++;
            } else if (tracker.mRetryCount == 0 && tracker.mExpectMore) {
                this.mCi.sendSMSExpectMore(IccUtils.bytesToHexString(smsc), IccUtils.bytesToHexString(pdu), reply);
            } else {
                this.mCi.sendSMS(IccUtils.bytesToHexString(smsc), IccUtils.bytesToHexString(pdu), reply);
            }
            return;
        }
        tracker.onFailed(this.mContext, SMSDispatcher.getNotInServiceError(ss), 0);
    }

    protected UiccCardApplication getUiccCardApplication() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("GsmSMSDispatcher: subId = ");
        stringBuilder.append(this.mPhone.getSubId());
        stringBuilder.append(" slotId = ");
        stringBuilder.append(this.mPhone.getPhoneId());
        Rlog.d(str, stringBuilder.toString());
        return this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneId(), 1);
    }

    private void onUpdateIccAvailability() {
        if (this.mUiccController != null) {
            UiccCardApplication newUiccApplication = getUiccCardApplication();
            UiccCardApplication app = (UiccCardApplication) this.mUiccApplication.get();
            if (app != newUiccApplication) {
                if (app != null) {
                    Rlog.d(TAG, "Removing stale icc objects.");
                    if (this.mIccRecords.get() != null) {
                        ((IccRecords) this.mIccRecords.get()).unregisterForNewSms(this);
                    }
                    this.mIccRecords.set(null);
                    this.mUiccApplication.set(null);
                }
                if (newUiccApplication != null) {
                    Rlog.d(TAG, "New Uicc application found");
                    this.mUiccApplication.set(newUiccApplication);
                    this.mIccRecords.set(newUiccApplication.getIccRecords());
                    if (this.mIccRecords.get() != null) {
                        ((IccRecords) this.mIccRecords.get()).registerForNewSms(this, 14, null);
                    }
                }
            }
        }
    }
}
