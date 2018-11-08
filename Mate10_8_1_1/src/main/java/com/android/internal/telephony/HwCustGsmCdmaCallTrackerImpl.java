package com.android.internal.telephony;

import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import java.util.List;

public class HwCustGsmCdmaCallTrackerImpl extends HwCustGsmCdmaCallTracker {
    private static final boolean IS_REJECT_HW_CALL = SystemProperties.getBoolean("ro.config.isRejectHwCall", false);
    public static final String LOG_TAG = "HwCustGsmCdmaCallTrackerImpl";
    protected static final boolean REJCALL_RINGING_REJECT = SystemProperties.getBoolean("ro.config.ringing_reject", false);
    private int mRejectCause = -1;
    private String mTelecomCallId = null;

    public HwCustGsmCdmaCallTrackerImpl(GsmCdmaPhone gsmPhone) {
        super(gsmPhone);
    }

    public void rejectCallForCause(CommandsInterface ci, GsmCdmaCall ringCall, Message message) {
        if (ringCall != null && (ringCall.isRinging() ^ 1) == 0) {
            Rlog.d(LOG_TAG, "rejectCallForCause, cause:" + this.mRejectCause);
            int count = ringCall.mConnections.size();
            for (int i = 0; i < count; i++) {
                GsmCdmaConnection cn = (GsmCdmaConnection) ringCall.mConnections.get(i);
                try {
                    if (!cn.mDisconnected) {
                        Rlog.d(LOG_TAG, "rejectCallForCause start");
                        TelephonyMetrics.getInstance().writeRilHangup(ringCall.getPhone().getPhoneId(), cn, cn.getGsmCdmaIndex());
                        if (this.mRejectCause == -1) {
                            ci.hangupConnection(cn.getGsmCdmaIndex(), message);
                        } else {
                            ci.rejectCallForCause(cn.getGsmCdmaIndex(), this.mRejectCause, message);
                        }
                    }
                } catch (CallStateException ex) {
                    Rlog.e(LOG_TAG, "hangupConnectionByIndex caught " + ex);
                }
            }
            clearCallRejectMark();
        }
    }

    public int getRejectCallCause(GsmCdmaCall ringCall) {
        if (REJCALL_RINGING_REJECT) {
            this.mRejectCause = 1;
            return 1;
        } else if (!IS_REJECT_HW_CALL) {
            Rlog.d(LOG_TAG, "IS_REJECT_HW_CALL is false");
            return -1;
        } else if (!HuaweiTelephonyConfigs.isHisiPlatform()) {
            Rlog.d(LOG_TAG, "Platform is not Hisi");
            return -1;
        } else if (this.mGsmPhone == null || 1 != this.mGsmPhone.getPhoneType()) {
            Rlog.d(LOG_TAG, "PhoneType is not GSM");
            return -1;
        } else if (ringCall.isRinging()) {
            List<Connection> cs = ringCall.getConnections();
            if (cs == null || cs.isEmpty()) {
                Rlog.d(LOG_TAG, "Ringing call not has Connection");
                return -1;
            } else if (this.mTelecomCallId == null || this.mTelecomCallId.isEmpty()) {
                Rlog.i(LOG_TAG, "getRejectCallCause - mTelecomCallId is null!");
                return -1;
            } else {
                for (Connection c : cs) {
                    if (this.mTelecomCallId.equals(c.getTelecomCallId())) {
                        return this.mRejectCause;
                    }
                }
                return -1;
            }
        } else {
            Rlog.d(LOG_TAG, "There is no a ringing call");
            return -1;
        }
    }

    public void markCallRejectCause(String telecomCallId, int cause) {
        Rlog.d(LOG_TAG, "markCallRejectCause, telecomCallId: " + telecomCallId + " cause:" + cause);
        this.mTelecomCallId = telecomCallId;
        this.mRejectCause = cause;
    }

    private void clearCallRejectMark() {
        Rlog.i(LOG_TAG, "clearCallRejectMark");
        this.mTelecomCallId = null;
        this.mRejectCause = -1;
    }
}
