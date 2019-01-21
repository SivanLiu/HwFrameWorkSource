package com.huawei.internal.telephony.gsm;

import android.os.SystemProperties;
import android.telephony.Rlog;
import java.util.ArrayList;
import java.util.Arrays;

public class HwCustGsmCellBroadcastHandler {
    private static final int ETWS_ARRAY_MAX = 50;
    private static final String FILLED_STRING_WHEN_BLOCK_IS_NULL_MSG = SystemProperties.get("ro.config.hw_cbs_del_msg", "2B");
    private static final boolean IS_ETWS_DROP_LATE_PN = SystemProperties.getBoolean("ro.config.drop_latePN", false);
    private static String TAG = "CBS_Handler";
    private ArrayList<String> mRcvEtwsSn = null;
    private int mRcvNumSn = 0;

    public HwCustGsmCellBroadcastHandler() {
        initSNArrayList();
    }

    public byte[] cbsPduAfterDiscardNullBlock(byte[] receivedPdu) {
        int cbsPduLength = receivedPdu.length;
        if (cbsPduLength <= 0) {
            return receivedPdu;
        }
        StringBuilder sb = new StringBuilder();
        for (int j = cbsPduLength - 1; j > 0; j--) {
            int b = receivedPdu[j] & 255;
            if (b < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(b));
            if (!sb.toString().equalsIgnoreCase(FILLED_STRING_WHEN_BLOCK_IS_NULL_MSG)) {
                break;
            }
            cbsPduLength--;
            sb.delete(0, sb.length());
        }
        return Arrays.copyOf(receivedPdu, cbsPduLength);
    }

    /* JADX WARNING: Missing block: B:14:0x008e, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean checkETWSBeLatePN(boolean isEmergency, boolean isEtws, boolean isPrimary, int serialnum, int msgidentify) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("EmergencyMsg:");
        stringBuilder.append(isEmergency);
        stringBuilder.append(", EtwsMsg:");
        stringBuilder.append(isEtws);
        stringBuilder.append(", isPn:");
        stringBuilder.append(isPrimary);
        Rlog.w(str, stringBuilder.toString());
        if (!IS_ETWS_DROP_LATE_PN || this.mRcvEtwsSn == null || !isEmergency || !isEtws) {
            return false;
        }
        str = new StringBuilder();
        str.append("<");
        str.append(Integer.toString(serialnum));
        str.append("|");
        str.append(Integer.toString(msgidentify));
        str.append(">");
        str = str.toString();
        boolean snexisted = this.mRcvEtwsSn.contains(str);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("snexisted = ");
        stringBuilder2.append(snexisted);
        Rlog.w(str2, stringBuilder2.toString());
        if (!isPrimary) {
            this.mRcvEtwsSn.set(this.mRcvNumSn % 50, str);
            this.mRcvNumSn++;
            return false;
        } else if (snexisted) {
            return true;
        } else {
            return false;
        }
    }

    private void initSNArrayList() {
        if (IS_ETWS_DROP_LATE_PN) {
            this.mRcvEtwsSn = new ArrayList();
            for (int index = 0; index < 50; index++) {
                this.mRcvEtwsSn.add("");
            }
        }
    }
}
