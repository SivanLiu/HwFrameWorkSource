package com.android.internal.telephony.gsm;

import android.telephony.Rlog;
import com.android.internal.telephony.uicc.IccUtils;
import java.util.ArrayList;

public final class HwPnnRecords {
    private static final boolean DBG = true;
    static final String TAG = "HwPnnRecords";
    private String mCurrentEons = null;
    private ArrayList<PnnRecord> mRecords = new ArrayList();

    public static class PnnRecord {
        static final int TAG_ADDL_INFO = 128;
        static final int TAG_FULL_NAME_IEI = 67;
        static final int TAG_SHORT_NAME_IEI = 69;
        private String mAddlInfo = null;
        private String mFullName = null;
        private String mShortName = null;

        PnnRecord(byte[] record) {
            StringBuilder stringBuilder;
            SimTlv tlv = new SimTlv(record, 0, record.length);
            if (tlv.isValidObject() && tlv.getTag() == 67) {
                this.mFullName = IccUtils.networkNameToString(tlv.getData(), 0, tlv.getData().length);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid tlv Object for Full Name, tag= ");
                stringBuilder.append(tlv.getTag());
                stringBuilder.append(", valid=");
                stringBuilder.append(tlv.isValidObject());
                HwPnnRecords.log(stringBuilder.toString());
            }
            tlv.nextObject();
            if (tlv.isValidObject() && tlv.getTag() == TAG_SHORT_NAME_IEI) {
                this.mShortName = IccUtils.networkNameToString(tlv.getData(), 0, tlv.getData().length);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid tlv Object for Short Name, tag= ");
                stringBuilder.append(tlv.getTag());
                stringBuilder.append(", valid=");
                stringBuilder.append(tlv.isValidObject());
                HwPnnRecords.log(stringBuilder.toString());
            }
            tlv.nextObject();
            if (tlv.isValidObject() && tlv.getTag() == TAG_ADDL_INFO) {
                this.mAddlInfo = IccUtils.networkNameToString(tlv.getData(), 0, tlv.getData().length);
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid tlv Object for Addl Info, tag= ");
            stringBuilder.append(tlv.getTag());
            stringBuilder.append(", valid=");
            stringBuilder.append(tlv.isValidObject());
            HwPnnRecords.log(stringBuilder.toString());
        }

        public String getFullName() {
            return this.mFullName;
        }

        public String getShortName() {
            return this.mShortName;
        }

        public String getAddlInfo() {
            return this.mAddlInfo;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Full Name=");
            stringBuilder.append(this.mFullName);
            stringBuilder.append(", Short Name=");
            stringBuilder.append(this.mShortName);
            stringBuilder.append(", Additional Info=");
            stringBuilder.append(this.mAddlInfo);
            return stringBuilder.toString();
        }
    }

    HwPnnRecords(ArrayList<byte[]> records) {
        if (records != null) {
            int list_size = records.size();
            for (int i = 0; i < list_size; i++) {
                this.mRecords.add(new PnnRecord((byte[]) records.get(i)));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Record ");
                stringBuilder.append(this.mRecords.size());
                stringBuilder.append(": ");
                stringBuilder.append(this.mRecords.get(this.mRecords.size() - 1));
                log(stringBuilder.toString());
            }
        }
    }

    public static void log(String s) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[HwPnnRecords EONS] ");
        stringBuilder.append(s);
        Rlog.d(str, stringBuilder.toString());
    }

    public static void loge(String s) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[HwPnnRecords EONS] ");
        stringBuilder.append(s);
        Rlog.e(str, stringBuilder.toString());
    }

    public int size() {
        return this.mRecords != null ? this.mRecords.size() : 0;
    }

    public String getCurrentEons() {
        return this.mCurrentEons;
    }

    public String getNameFromPnnRecord(int recordNumber, boolean update) {
        String fullName = null;
        String ShortName = null;
        StringBuilder stringBuilder;
        if (recordNumber < 1 || recordNumber > this.mRecords.size()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid PNN record number ");
            stringBuilder.append(recordNumber);
            loge(stringBuilder.toString());
        } else {
            fullName = ((PnnRecord) this.mRecords.get(recordNumber - 1)).getFullName();
            ShortName = ((PnnRecord) this.mRecords.get(recordNumber - 1)).getShortName();
            stringBuilder = new StringBuilder();
            stringBuilder.append("getNameFromPnnRecord and  fullName is");
            stringBuilder.append(fullName);
            stringBuilder.append("and ShortName is");
            stringBuilder.append(ShortName);
            log(stringBuilder.toString());
        }
        if (update) {
            if (fullName != null && !fullName.equals("")) {
                this.mCurrentEons = fullName;
            } else if (fullName == null || !fullName.equals("") || ShortName == null || ShortName.equals("")) {
                this.mCurrentEons = fullName;
            } else {
                this.mCurrentEons = ShortName;
            }
        }
        return fullName;
    }
}
