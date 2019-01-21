package com.android.internal.telephony.cdma;

import android.content.Intent;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants.State;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SMSDispatcher.SmsTracker;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsMessageBase.SubmitPduBase;
import com.android.internal.telephony.util.SMSDispatcherUtil;

public abstract class CdmaSMSDispatcher extends SMSDispatcher {
    private static final String TAG = "CdmaSMSDispatcher";
    private static final boolean VDBG = true;

    public CdmaSMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        super(phone, smsDispatchersController);
        Rlog.d(TAG, "CdmaSMSDispatcher created");
    }

    public String getFormat() {
        return "3gpp2";
    }

    public void sendStatusReportMessage(SmsMessage sms) {
        Rlog.d(TAG, "sending EVENT_HANDLE_STATUS_REPORT message");
        sendMessage(obtainMessage(10, sms));
    }

    protected void handleStatusReport(Object o) {
        if (o instanceof SmsMessage) {
            Rlog.d(TAG, "calling handleCdmaStatusReport()");
            handleCdmaStatusReport((SmsMessage) o);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleStatusReport() called for object type ");
        stringBuilder.append(o.getClass().getName());
        Rlog.e(str, stringBuilder.toString());
    }

    protected boolean shouldBlockSmsForEcbm() {
        return this.mPhone.isInEcm() && isCdmaMo() && !isIms();
    }

    protected SubmitPduBase getSubmitPdu(String scAddr, String destAddr, String message, boolean statusReportRequested, SmsHeader smsHeader, int priority, int validityPeriod) {
        return SMSDispatcherUtil.getSubmitPduCdma(scAddr, destAddr, message, statusReportRequested, smsHeader, priority);
    }

    protected SubmitPduBase getSubmitPdu(String scAddr, String destAddr, int destPort, byte[] message, boolean statusReportRequested) {
        return SMSDispatcherUtil.getSubmitPduCdma(scAddr, destAddr, destPort, message, statusReportRequested);
    }

    protected TextEncodingDetails calculateLength(CharSequence messageBody, boolean use7bitOnly) {
        return SMSDispatcherUtil.calculateLengthCdma(messageBody, use7bitOnly);
    }

    private void handleCdmaStatusReport(SmsMessage sms) {
        int i = 0;
        int count = this.deliveryPendingList.size();
        while (i < count) {
            SmsTracker tracker = (SmsTracker) this.deliveryPendingList.get(i);
            if (tracker.mMessageRef != sms.mMessageRef) {
                i++;
            } else if (((Boolean) this.mSmsDispatchersController.handleSmsStatusReport(tracker, getFormat(), sms.getPdu()).second).booleanValue()) {
                this.deliveryPendingList.remove(i);
                return;
            } else {
                return;
            }
        }
    }

    public void sendSms(SmsTracker tracker) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
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
        boolean imsSmsDisabled = false;
        if (isIms() || ss == 0) {
            Message reply = obtainMessage(2, tracker);
            byte[] pdu = (byte[]) tracker.getData().get("pdu");
            int currentDataNetwork = this.mPhone.getServiceState().getDataNetworkType();
            if ((currentDataNetwork == 14 || (ServiceState.isLte(currentDataNetwork) && !this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed())) && this.mPhone.getServiceState().getVoiceNetworkType() == 7 && ((GsmCdmaPhone) this.mPhone).mCT.mState != State.IDLE) {
                imsSmsDisabled = true;
            }
            if ((tracker.mImsRetry == 0 && !isIms()) || imsSmsDisabled || tracker.mUsesImsServiceForIms) {
                this.mCi.sendCdmaSms(pdu, reply);
            } else {
                this.mCi.sendImsCdmaSms(pdu, tracker.mImsRetry, tracker.mMessageRef, reply);
                tracker.mImsRetry++;
            }
            return;
        }
        tracker.onFailed(this.mContext, SMSDispatcher.getNotInServiceError(ss), 0);
    }

    public void dispatchCTAutoRegSmsPdus(SmsMessageBase smsb) {
        byte[][] pdus = new byte[][]{((SmsMessage) smsb).getUserData()};
        Intent intent = new Intent("android.provider.Telephony.CT_AUTO_REG_RECV_CONFIRM_ACK");
        intent.putExtra("pdus", pdus);
        intent.putExtra("CdmaSubscription", getSubId());
        intent.addFlags(134217728);
        this.mContext.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS", null, this, -1, null, null);
        Rlog.d(TAG, "dispatchCTAutoRegSmsPdus end. Broadcast send to apk!");
    }
}
