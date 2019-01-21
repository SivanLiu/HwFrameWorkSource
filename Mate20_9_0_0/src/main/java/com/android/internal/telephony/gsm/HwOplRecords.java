package com.android.internal.telephony.gsm;

import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.HwSubscriptionManager;
import java.util.ArrayList;

public final class HwOplRecords {
    private static final boolean DBG = true;
    static final String TAG = "HwOplRecords";
    static final int wildCardDigit = 13;
    private ArrayList<OplRecord> mRecords = new ArrayList();

    public static class OplRecord {
        private int mLac1;
        private int mLac2;
        private int[] mPlmn = new int[]{0, 0, 0, 0, 0, 0};
        private int mPnnRecordNumber;

        OplRecord(byte[] record) {
            getPlmn(record);
            getLac(record);
            this.mPnnRecordNumber = record[7] & HwSubscriptionManager.SUB_INIT_STATE;
        }

        private void getPlmn(byte[] record) {
            this.mPlmn[0] = record[0] & 15;
            this.mPlmn[1] = (record[0] >> 4) & 15;
            this.mPlmn[2] = record[1] & 15;
            this.mPlmn[3] = record[2] & 15;
            this.mPlmn[4] = (record[2] >> 4) & 15;
            this.mPlmn[5] = (record[1] >> 4) & 15;
            if (this.mPlmn[5] == 15) {
                this.mPlmn[5] = 0;
            }
        }

        private void getLac(byte[] record) {
            this.mLac1 = ((record[3] & HwSubscriptionManager.SUB_INIT_STATE) << 8) | (record[4] & HwSubscriptionManager.SUB_INIT_STATE);
            this.mLac2 = ((record[5] & HwSubscriptionManager.SUB_INIT_STATE) << 8) | (record[6] & HwSubscriptionManager.SUB_INIT_STATE);
        }

        public int getPnnRecordNumber() {
            return this.mPnnRecordNumber;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PLMN=");
            stringBuilder.append(Integer.toHexString(this.mPlmn[0]));
            stringBuilder.append(Integer.toHexString(this.mPlmn[1]));
            stringBuilder.append(Integer.toHexString(this.mPlmn[2]));
            stringBuilder.append(Integer.toHexString(this.mPlmn[3]));
            stringBuilder.append(Integer.toHexString(this.mPlmn[4]));
            stringBuilder.append(Integer.toHexString(this.mPlmn[5]));
            stringBuilder.append(", LAC1=");
            stringBuilder.append(this.mLac1);
            stringBuilder.append(", LAC2=");
            stringBuilder.append(this.mLac2);
            stringBuilder.append(", PNN Record=");
            stringBuilder.append(this.mPnnRecordNumber);
            return stringBuilder.toString();
        }
    }

    HwOplRecords(ArrayList<byte[]> records) {
        if (records != null) {
            int list_size = records.size();
            for (int i = 0; i < list_size; i++) {
                this.mRecords.add(new OplRecord((byte[]) records.get(i)));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Record ");
                stringBuilder.append(this.mRecords.size());
                stringBuilder.append(": ");
                stringBuilder.append(this.mRecords.get(this.mRecords.size() - 1));
                log(stringBuilder.toString());
            }
        }
    }

    private void log(String s) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[HwOplRecords EONS] ");
        stringBuilder.append(s);
        Rlog.d(str, stringBuilder.toString());
    }

    private void loge(String s) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[HwOplRecords EONS] ");
        stringBuilder.append(s);
        Rlog.e(str, stringBuilder.toString());
    }

    public int size() {
        return this.mRecords != null ? this.mRecords.size() : 0;
    }

    public int getMatchingPnnRecord(String operator, int lac, boolean useLac) {
        int[] bcchPlmn = new int[]{0, 0, 0, 0, 0, 0};
        if (TextUtils.isEmpty(operator)) {
            loge("No registered operator.");
            return 0;
        } else if (useLac && lac == -1) {
            loge("Invalid LAC");
            return 0;
        } else {
            int length = operator.length();
            if (length == 5 || length == 6) {
                int i;
                for (i = 0; i < length; i++) {
                    bcchPlmn[i] = operator.charAt(i) - 48;
                }
                if (this.mRecords != null) {
                    int list_size = this.mRecords.size();
                    for (i = 0; i < list_size; i++) {
                        OplRecord record = (OplRecord) this.mRecords.get(i);
                        if (matchPlmn(record.mPlmn, bcchPlmn) && (!useLac || (record.mLac1 <= lac && lac <= record.mLac2))) {
                            return record.getPnnRecordNumber();
                        }
                    }
                }
                loge("No matching OPL record found.");
                return 0;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid registered operator length ");
            stringBuilder.append(length);
            loge(stringBuilder.toString());
            return 0;
        }
    }

    private boolean matchPlmn(int[] simPlmn, int[] bcchPlmn) {
        boolean match = true;
        for (int i = 0; i < bcchPlmn.length; i++) {
            int i2 = (bcchPlmn[i] == simPlmn[i] || simPlmn[i] == 13) ? 1 : 0;
            match &= i2;
        }
        return match;
    }
}
