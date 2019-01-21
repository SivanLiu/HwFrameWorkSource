package com.android.internal.telephony.uicc;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccRecords.IccRecordLoaded;
import com.android.internal.util.BitwiseInputStream;
import com.google.android.mms.pdu.CharacterSets;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

public class RuimRecords extends IccRecords {
    private static final int EVENT_APP_LOCKED = 32;
    private static final int EVENT_APP_NETWORK_LOCKED = 33;
    private static final int EVENT_GET_ALL_SMS_DONE = 18;
    protected static final int EVENT_GET_CDMA_GSM_IMSI_DONE = 37;
    private static final int EVENT_GET_CDMA_SUBSCRIPTION_DONE = 10;
    private static final int EVENT_GET_DEVICE_IDENTITY_DONE = 4;
    private static final int EVENT_GET_ICCID_DONE = 5;
    private static final int EVENT_GET_IMSI_DONE = 3;
    private static final int EVENT_GET_SIM_APP_IMSI_DONE = 38;
    private static final int EVENT_GET_SMS_DONE = 22;
    private static final int EVENT_GET_SST_DONE = 17;
    private static final int EVENT_MARK_SMS_READ_DONE = 19;
    private static final int EVENT_SMS_ON_RUIM = 21;
    private static final int EVENT_UPDATE_DONE = 14;
    private static final boolean IS_MODEM_CAPABILITY_GET_ICCID_AT = HwModemCapability.isCapabilitySupport(19);
    static final String LOG_TAG = "RuimRecords";
    private static boolean PLUS_TRANFER_IN_AP = (HwModemCapability.isCapabilitySupport(2) ^ 1);
    protected String mCdmaGsmImsi;
    boolean mCsimSpnDisplayCondition;
    private byte[] mEFli;
    private byte[] mEFpl;
    private String mHomeNetworkId;
    private String mHomeSystemId;
    private String mMin;
    private String mMin2Min1;
    private String mMyMobileNumber;
    private String mNai;
    private boolean mOtaCommited;
    private String mPrlVersion;
    private boolean mRecordsRequired;

    private class EfCsimCdmaHomeLoaded implements IccRecordLoaded {
        private EfCsimCdmaHomeLoaded() {
        }

        public String getEfName() {
            return "EF_CSIM_CDMAHOME";
        }

        public void onRecordLoaded(AsyncResult ar) {
            ArrayList<byte[]> dataList = ar.result;
            RuimRecords ruimRecords = RuimRecords.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CSIM_CDMAHOME data size=");
            stringBuilder.append(dataList.size());
            ruimRecords.log(stringBuilder.toString());
            if (!dataList.isEmpty()) {
                StringBuilder sidBuf = new StringBuilder();
                stringBuilder = new StringBuilder();
                Iterator it = dataList.iterator();
                while (it.hasNext()) {
                    byte[] data = (byte[]) it.next();
                    if (data.length == 5) {
                        int nid = ((data[3] & 255) << 8) | (data[2] & 255);
                        sidBuf.append(((data[1] & 255) << 8) | (data[0] & 255));
                        sidBuf.append(',');
                        stringBuilder.append(nid);
                        stringBuilder.append(',');
                    }
                }
                sidBuf.setLength(sidBuf.length() - 1);
                stringBuilder.setLength(stringBuilder.length() - 1);
                RuimRecords.this.mHomeSystemId = sidBuf.toString();
                RuimRecords.this.mHomeNetworkId = stringBuilder.toString();
            }
        }
    }

    private class EfCsimEprlLoaded implements IccRecordLoaded {
        private EfCsimEprlLoaded() {
        }

        public String getEfName() {
            return "EF_CSIM_EPRL";
        }

        public void onRecordLoaded(AsyncResult ar) {
            RuimRecords.this.onGetCSimEprlDone(ar);
        }
    }

    private class EfCsimImsimLoaded implements IccRecordLoaded {
        private EfCsimImsimLoaded() {
        }

        public String getEfName() {
            return "EF_CSIM_IMSIM";
        }

        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = ar.result;
            boolean provisioned = (data[7] & 128) == 128;
            if (true == HwModemCapability.isCapabilitySupport(18)) {
                try {
                    RuimRecords.this.mImsi = RuimRecords.this.decodeCdmaImsi(data);
                    RuimRecords.this.mImsiReadyRegistrants.notifyRegistrants();
                    RuimRecords ruimRecords = RuimRecords.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("IMSI: ");
                    stringBuilder.append(RuimRecords.this.mImsi.substring(0, 5));
                    stringBuilder.append("xxxxxxxxx");
                    ruimRecords.log(stringBuilder.toString());
                    RuimRecords.this.updateMccMncConfigWithCplmn(RuimRecords.this.getRUIMOperatorNumeric());
                    if (!(RuimRecords.this.mParentApp == null || RuimRecords.this.mParentApp.getUiccCard() == null)) {
                        UiccCardApplication simApp = RuimRecords.this.mParentApp.getUiccCard().getApplication(1);
                        if (simApp != null) {
                            RuimRecords.this.mCi.getIMSIForApp(simApp.getAid(), RuimRecords.this.obtainMessage(38));
                        }
                    }
                } catch (RuntimeException e) {
                    RuimRecords ruimRecords2 = RuimRecords.this;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Illegal IMSI from CSIM_IMSIM=");
                    stringBuilder2.append(IccUtils.bytesToHexString(data));
                    ruimRecords2.loge(stringBuilder2.toString());
                }
            }
            if (provisioned) {
                int first3digits = ((data[2] & 3) << 8) + (data[1] & 255);
                int second3digits = (((data[5] & 255) << 8) | (data[4] & 255)) >> 6;
                int digit7 = (data[4] >> 2) & 15;
                if (digit7 > 9) {
                    digit7 = 0;
                }
                int last3digits = (data[3] & 255) | ((data[4] & 3) << 8);
                first3digits = RuimRecords.this.adjstMinDigits(first3digits);
                second3digits = RuimRecords.this.adjstMinDigits(second3digits);
                last3digits = RuimRecords.this.adjstMinDigits(last3digits);
                StringBuilder builder = new StringBuilder();
                builder.append(String.format(Locale.US, "%03d", new Object[]{Integer.valueOf(first3digits)}));
                builder.append(String.format(Locale.US, "%03d", new Object[]{Integer.valueOf(second3digits)}));
                builder.append(String.format(Locale.US, "%d", new Object[]{Integer.valueOf(digit7)}));
                builder.append(String.format(Locale.US, "%03d", new Object[]{Integer.valueOf(last3digits)}));
                RuimRecords.this.mMin = builder.toString();
                RuimRecords ruimRecords3 = RuimRecords.this;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("min present=");
                stringBuilder3.append(RuimRecords.this.mMin);
                ruimRecords3.log(stringBuilder3.toString());
                return;
            }
            RuimRecords.this.log("min not present");
        }
    }

    private class EfCsimLiLoaded implements IccRecordLoaded {
        private EfCsimLiLoaded() {
        }

        public String getEfName() {
            return "EF_CSIM_LI";
        }

        public void onRecordLoaded(AsyncResult ar) {
            RuimRecords.this.mEFli = (byte[]) ar.result;
            for (int i = 0; i < RuimRecords.this.mEFli.length; i += 2) {
                switch (RuimRecords.this.mEFli[i + 1]) {
                    case (byte) 1:
                        RuimRecords.this.mEFli[i] = (byte) 101;
                        RuimRecords.this.mEFli[i + 1] = (byte) 110;
                        break;
                    case (byte) 2:
                        RuimRecords.this.mEFli[i] = (byte) 102;
                        RuimRecords.this.mEFli[i + 1] = (byte) 114;
                        break;
                    case (byte) 3:
                        RuimRecords.this.mEFli[i] = (byte) 101;
                        RuimRecords.this.mEFli[i + 1] = (byte) 115;
                        break;
                    case (byte) 4:
                        RuimRecords.this.mEFli[i] = (byte) 106;
                        RuimRecords.this.mEFli[i + 1] = (byte) 97;
                        break;
                    case (byte) 5:
                        RuimRecords.this.mEFli[i] = (byte) 107;
                        RuimRecords.this.mEFli[i + 1] = (byte) 111;
                        break;
                    case (byte) 6:
                        RuimRecords.this.mEFli[i] = (byte) 122;
                        RuimRecords.this.mEFli[i + 1] = (byte) 104;
                        break;
                    case (byte) 7:
                        RuimRecords.this.mEFli[i] = (byte) 104;
                        RuimRecords.this.mEFli[i + 1] = (byte) 101;
                        break;
                    default:
                        RuimRecords.this.mEFli[i] = (byte) 32;
                        RuimRecords.this.mEFli[i + 1] = (byte) 32;
                        break;
                }
            }
            RuimRecords ruimRecords = RuimRecords.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EF_LI=");
            stringBuilder.append(IccUtils.bytesToHexString(RuimRecords.this.mEFli));
            ruimRecords.log(stringBuilder.toString());
        }
    }

    private class EfCsimMdnLoaded implements IccRecordLoaded {
        private EfCsimMdnLoaded() {
        }

        public String getEfName() {
            return "EF_CSIM_MDN";
        }

        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = ar.result;
            int mdnDigitsNum = data[0] & 15;
            RuimRecords.this.mMdn = IccUtils.cdmaBcdToStringHw(data, 1, mdnDigitsNum);
            if (RuimRecords.this.mMdn != null) {
                RuimRecords.this.log("CSIM MDN= ****");
            }
        }
    }

    private class EfCsimMipUppLoaded implements IccRecordLoaded {
        private EfCsimMipUppLoaded() {
        }

        public String getEfName() {
            return "EF_CSIM_MIPUPP";
        }

        boolean checkLengthLegal(int length, int expectLength) {
            if (length >= expectLength) {
                return true;
            }
            String str = RuimRecords.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CSIM MIPUPP format error, length = ");
            stringBuilder.append(length);
            stringBuilder.append("expected length at least =");
            stringBuilder.append(expectLength);
            Log.e(str, stringBuilder.toString());
            return false;
        }

        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = (byte[]) ar.result;
            if (data.length < 1) {
                Log.e(RuimRecords.LOG_TAG, "MIPUPP read error");
                return;
            }
            BitwiseInputStream bitStream = new BitwiseInputStream(data);
            int i = 8;
            try {
                int mipUppLength = bitStream.read(8) << 3;
                if (checkLengthLegal(mipUppLength, 1)) {
                    mipUppLength--;
                    if (bitStream.read(1) == 1) {
                        if (checkLengthLegal(mipUppLength, 11)) {
                            bitStream.skip(11);
                            mipUppLength -= 11;
                        } else {
                            return;
                        }
                    }
                    if (checkLengthLegal(mipUppLength, 4)) {
                        int numNai = bitStream.read(4);
                        int index1 = 0;
                        int mipUppLength2 = mipUppLength - 4;
                        mipUppLength = 0;
                        while (mipUppLength < numNai && checkLengthLegal(mipUppLength2, 4)) {
                            int naiEntryIndex = bitStream.read(4);
                            mipUppLength2 -= 4;
                            if (checkLengthLegal(mipUppLength2, i)) {
                                int naiLength = bitStream.read(i);
                                mipUppLength2 -= 8;
                                if (naiEntryIndex == 0) {
                                    if (checkLengthLegal(mipUppLength2, naiLength << 3)) {
                                        char[] naiCharArray = new char[naiLength];
                                        while (true) {
                                            int index12 = index1;
                                            if (index12 >= naiLength) {
                                                break;
                                            }
                                            naiCharArray[index12] = (char) (bitStream.read(i) & 255);
                                            index1 = index12 + 1;
                                        }
                                        RuimRecords.this.mNai = new String(naiCharArray);
                                        if (Log.isLoggable(RuimRecords.LOG_TAG, 2)) {
                                            String str = RuimRecords.LOG_TAG;
                                            StringBuilder stringBuilder = new StringBuilder();
                                            stringBuilder.append("MIPUPP Nai = ");
                                            stringBuilder.append(RuimRecords.this.mNai);
                                            Log.v(str, stringBuilder.toString());
                                        }
                                        return;
                                    }
                                    return;
                                } else if (checkLengthLegal(mipUppLength2, (naiLength << 3) + 102)) {
                                    bitStream.skip((naiLength << 3) + 101);
                                    mipUppLength2 -= (naiLength << 3) + 102;
                                    if (bitStream.read(1) == 1) {
                                        if (checkLengthLegal(mipUppLength2, 32)) {
                                            bitStream.skip(32);
                                            mipUppLength2 -= 32;
                                        } else {
                                            return;
                                        }
                                    }
                                    if (checkLengthLegal(mipUppLength2, 5)) {
                                        bitStream.skip(4);
                                        mipUppLength2 = (mipUppLength2 - 4) - 1;
                                        if (bitStream.read(1) == 1) {
                                            if (checkLengthLegal(mipUppLength2, 32)) {
                                                bitStream.skip(32);
                                                mipUppLength2 -= 32;
                                            } else {
                                                return;
                                            }
                                        }
                                        mipUppLength++;
                                        i = 8;
                                    } else {
                                        return;
                                    }
                                } else {
                                    return;
                                }
                            }
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(RuimRecords.LOG_TAG, "MIPUPP read Exception error!");
            }
        }
    }

    private class EfCsimSpnLoaded implements IccRecordLoaded {
        private EfCsimSpnLoaded() {
        }

        public String getEfName() {
            return "EF_CSIM_SPN";
        }

        /* JADX WARNING: Missing block: B:24:0x0078, code skipped:
            r11.this$0.setServiceProviderName(com.android.internal.telephony.GsmAlphabet.gsm7BitPackedToString(r5, 0, (r6 * 8) / 7));
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = ar.result;
            RuimRecords ruimRecords = RuimRecords.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CSIM_SPN=");
            stringBuilder.append(IccUtils.bytesToHexString(data));
            ruimRecords.log(stringBuilder.toString());
            RuimRecords.this.mCsimSpnDisplayCondition = (data[0] & 1) != 0;
            int encoding = data[1];
            int language = data[2];
            int len = 32;
            byte[] spnData = new byte[32];
            if (data.length - 3 < 32) {
                len = data.length - 3;
            }
            System.arraycopy(data, 3, spnData, 0, len);
            int numBytes = 0;
            while (numBytes < spnData.length && (spnData[numBytes] & 255) != 255) {
                numBytes++;
            }
            if (numBytes == 0) {
                RuimRecords.this.setServiceProviderName("");
                return;
            }
            if (encoding != 0) {
                switch (encoding) {
                    case 2:
                        String spn = new String(spnData, 0, numBytes, "US-ASCII");
                        if (!TextUtils.isPrintableAsciiOnly(spn)) {
                            RuimRecords ruimRecords2 = RuimRecords.this;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Some corruption in SPN decoding = ");
                            stringBuilder2.append(spn);
                            ruimRecords2.log(stringBuilder2.toString());
                            RuimRecords.this.log("Using ENCODING_GSM_7BIT_ALPHABET scheme...");
                            RuimRecords.this.setServiceProviderName(GsmAlphabet.gsm7BitPackedToString(spnData, 0, (numBytes * 8) / 7));
                            break;
                        }
                        RuimRecords.this.setServiceProviderName(spn);
                        break;
                    case 3:
                        break;
                    case 4:
                        RuimRecords.this.setServiceProviderName(new String(spnData, 0, numBytes, CharacterSets.MIMENAME_UTF_16));
                        break;
                    default:
                        switch (encoding) {
                            case 8:
                                break;
                            case 9:
                                break;
                            default:
                                try {
                                    RuimRecords.this.log("SPN encoding not supported");
                                    break;
                                } catch (Exception e) {
                                    RuimRecords ruimRecords3 = RuimRecords.this;
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("spn decode error: ");
                                    stringBuilder3.append(e);
                                    ruimRecords3.log(stringBuilder3.toString());
                                    break;
                                }
                        }
                }
            }
            RuimRecords.this.setServiceProviderName(new String(spnData, 0, numBytes, "ISO-8859-1"));
            RuimRecords ruimRecords4 = RuimRecords.this;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("spn=");
            stringBuilder4.append(RuimRecords.this.getServiceProviderName());
            ruimRecords4.log(stringBuilder4.toString());
            ruimRecords4 = RuimRecords.this;
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("spnCondition=");
            stringBuilder4.append(RuimRecords.this.mCsimSpnDisplayCondition);
            ruimRecords4.log(stringBuilder4.toString());
            RuimRecords.this.mTelephonyManager.setSimOperatorNameForPhone(RuimRecords.this.mParentApp.getPhoneId(), RuimRecords.this.getServiceProviderName());
        }
    }

    private class EfPlLoaded implements IccRecordLoaded {
        private EfPlLoaded() {
        }

        public String getEfName() {
            return "EF_PL";
        }

        public void onRecordLoaded(AsyncResult ar) {
            RuimRecords.this.mEFpl = (byte[]) ar.result;
            RuimRecords ruimRecords = RuimRecords.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EF_PL=");
            stringBuilder.append(IccUtils.bytesToHexString(RuimRecords.this.mEFpl));
            ruimRecords.log(stringBuilder.toString());
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RuimRecords: ");
        stringBuilder.append(super.toString());
        stringBuilder.append(" m_ota_commited");
        stringBuilder.append(this.mOtaCommited);
        stringBuilder.append(" mMyMobileNumber=xxxx mMin2Min1=");
        stringBuilder.append(this.mMin2Min1);
        stringBuilder.append(" mPrlVersion=");
        stringBuilder.append(this.mPrlVersion);
        stringBuilder.append(" mEFpl=");
        stringBuilder.append(this.mEFpl);
        stringBuilder.append(" mEFli=");
        stringBuilder.append(this.mEFli);
        stringBuilder.append(" mCsimSpnDisplayCondition=");
        stringBuilder.append(this.mCsimSpnDisplayCondition);
        stringBuilder.append(" mMdn=xxxx mMin=xxxx mHomeSystemId=");
        stringBuilder.append(this.mHomeSystemId);
        stringBuilder.append(" mHomeNetworkId=");
        stringBuilder.append(this.mHomeNetworkId);
        return stringBuilder.toString();
    }

    public String getCdmaGsmImsi() {
        return this.mCdmaGsmImsi;
    }

    public RuimRecords(UiccCardApplication app, Context c, CommandsInterface ci) {
        super(app, c, ci);
        this.mOtaCommited = false;
        this.mRecordsRequired = true;
        this.mEFpl = null;
        this.mEFli = null;
        this.mCsimSpnDisplayCondition = false;
        this.mAdnCache = HwTelephonyFactory.getHwUiccManager().createHwAdnRecordCache(this.mFh);
        this.mRecordsRequested = false;
        this.mLockedRecordsReqReason = 0;
        this.mRecordsToLoad = 0;
        resetRecords();
        this.mParentApp.registerForReady(this, 1, null);
        this.mParentApp.registerForLocked(this, 32, null);
        this.mParentApp.registerForNetworkLocked(this, 33, null);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RuimRecords X ctor this=");
        stringBuilder.append(this);
        log(stringBuilder.toString());
    }

    public void dispose() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Disposing RuimRecords ");
        stringBuilder.append(this);
        log(stringBuilder.toString());
        this.mParentApp.unregisterForReady(this);
        this.mParentApp.unregisterForLocked(this);
        this.mParentApp.unregisterForNetworkLocked(this);
        resetRecords();
        super.dispose();
    }

    protected void finalize() {
        log("RuimRecords finalized");
    }

    protected void resetRecords() {
        this.mMncLength = -1;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setting0 mMncLength");
        stringBuilder.append(this.mMncLength);
        log(stringBuilder.toString());
        this.mIccId = null;
        this.mFullIccId = null;
        this.mAdnCache.reset();
        this.mRecordsRequested = false;
        this.mLockedRecordsReqReason = 0;
        this.mLoaded.set(false);
        this.mImsiLoad = false;
    }

    public String getMdnNumber() {
        return this.mMyMobileNumber;
    }

    public String getCdmaMin() {
        return this.mMin2Min1;
    }

    public String getPrlVersion() {
        return this.mPrlVersion;
    }

    public String getNAI() {
        return this.mNai;
    }

    public void setVoiceMailNumber(String alphaTag, String voiceNumber, Message onComplete) {
        AsyncResult.forMessage(onComplete).exception = new IccException("setVoiceMailNumber not implemented");
        onComplete.sendToTarget();
        loge("method setVoiceMailNumber is not implemented");
    }

    public void onRefresh(boolean fileChanged, int[] fileList) {
        if (fileChanged) {
            fetchRuimRecords();
        }
    }

    private int adjstMinDigits(int digits) {
        digits += 111;
        digits = digits % 10 == 0 ? digits - 10 : digits;
        digits = (digits / 10) % 10 == 0 ? digits - 100 : digits;
        return (digits / 100) % 10 == 0 ? digits - 1000 : digits;
    }

    public String getRUIMOperatorNumeric() {
        String imsi = getIMSI();
        if (imsi == null) {
            return null;
        }
        if (this.mMncLength != -1 && this.mMncLength != 0) {
            return imsi.substring(0, 3 + this.mMncLength);
        }
        try {
            return this.mImsi.substring(0, 3 + MccTable.smallestDigitsMccForMnc(Integer.parseInt(this.mImsi.substring(0, 3))));
        } catch (RuntimeException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mImsi is not avalible,parseInt error,");
            stringBuilder.append(e.getMessage());
            stringBuilder.append(",so return null !");
            log(stringBuilder.toString());
            return null;
        }
    }

    public String getOperatorNumeric() {
        if (this.mImsi == null) {
            String tempOperatorNumeric = SystemProperties.get(GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("imsi is null tempOperatorNumeric = ");
            stringBuilder.append(tempOperatorNumeric);
            log(stringBuilder.toString());
            return tempOperatorNumeric;
        } else if (this.mMncLength != -1 && this.mMncLength != 0) {
            return this.mImsi.substring(0, 3 + this.mMncLength);
        } else {
            try {
                return this.mImsi.substring(0, 3 + MccTable.smallestDigitsMccForMnc(Integer.parseInt(this.mImsi.substring(0, 3))));
            } catch (RuntimeException e) {
                String tempOperatorNumeric2 = SystemProperties.get(GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mImsi is not avalible,parseInt error,");
                stringBuilder2.append(e.getMessage());
                stringBuilder2.append(",so return tempOperatorNumeric !");
                log(stringBuilder2.toString());
                return tempOperatorNumeric2;
            }
        }
    }

    private void onGetCSimEprlDone(AsyncResult ar) {
        byte[] data = ar.result;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CSIM_EPRL=");
        stringBuilder.append(IccUtils.bytesToHexString(data));
        log(stringBuilder.toString());
        if (data.length > 3) {
            this.mPrlVersion = Integer.toString(((data[2] & 255) << 8) | (data[3] & 255));
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("CSIM PRL version=");
        stringBuilder.append(this.mPrlVersion);
        log(stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:29:0x014b, code skipped:
            if (r0 != false) goto L_0x014d;
     */
    /* JADX WARNING: Missing block: B:30:0x014d, code skipped:
            onRecordLoaded();
     */
    /* JADX WARNING: Missing block: B:35:0x015b, code skipped:
            if (null == null) goto L_0x015e;
     */
    /* JADX WARNING: Missing block: B:36:0x015e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleMessage(Message msg) {
        boolean isRecordLoadResponse = false;
        if (this.mDestroyed.get()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Received message ");
            stringBuilder.append(msg);
            stringBuilder.append("[");
            stringBuilder.append(msg.what);
            stringBuilder.append("] while being destroyed. Ignoring.");
            loge(stringBuilder.toString());
            return;
        }
        try {
            AsyncResult ar;
            StringBuilder stringBuilder2;
            switch (msg.what) {
                case 1:
                    onReady();
                    break;
                case 3:
                    isRecordLoadResponse = true;
                    onGetImsiDone(msg);
                    break;
                case 4:
                    log("Event EVENT_GET_DEVICE_IDENTITY_DONE Received");
                    break;
                case 5:
                    isRecordLoadResponse = true;
                    ar = (AsyncResult) msg.obj;
                    byte[] data = ar.result;
                    if (ar.exception == null) {
                        this.mIccId = HwTelephonyFactory.getHwUiccManager().bcdIccidToString(data, 0, data.length);
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("iccid: ");
                        stringBuilder3.append(SubscriptionInfo.givePrintableIccid(this.mIccId));
                        log(stringBuilder3.toString());
                        this.mFullIccId = IccUtils.bchToString(data, 0, data.length);
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("iccid: ");
                        stringBuilder2.append(SubscriptionInfo.givePrintableIccid(this.mFullIccId));
                        log(stringBuilder2.toString());
                        onIccIdLoadedHw();
                        this.mIccIDLoadRegistrants.notifyRegistrants(ar);
                        break;
                    }
                    this.mIccIDLoadRegistrants.notifyRegistrants(ar);
                    break;
                case 10:
                    ar = (AsyncResult) msg.obj;
                    String[] localTemp = ar.result;
                    if (ar.exception == null) {
                        this.mMyMobileNumber = localTemp[0];
                        this.mMin2Min1 = localTemp[3];
                        this.mPrlVersion = localTemp[4];
                        log("EVENT_GET_CDMA_SUBSCRIPTION_DONE");
                        break;
                    }
                    break;
                case 14:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Rlog.i(LOG_TAG, "RuimRecords update failed", ar.exception);
                        break;
                    }
                    break;
                case 17:
                    log("Event EVENT_GET_SST_DONE Received");
                    break;
                case 18:
                case 19:
                case 21:
                case 22:
                    String str = LOG_TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Event not supported: ");
                    stringBuilder2.append(msg.what);
                    Rlog.w(str, stringBuilder2.toString());
                    break;
                case 32:
                case 33:
                    onLocked(msg.what);
                    break;
                case 38:
                    log("get SIM_APP_IMSI");
                    ar = msg.obj;
                    if (ar.exception == null) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(this.mImsi);
                        stringBuilder2.append(",");
                        stringBuilder2.append((String) ar.result);
                        this.mCdmaGsmImsi = stringBuilder2.toString();
                        break;
                    }
                    String str2 = LOG_TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Exception querying SIM APP IMSI, Exception:");
                    stringBuilder4.append(ar.exception);
                    Rlog.e(str2, stringBuilder4.toString());
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        } catch (RuntimeException exc) {
            Rlog.w(LOG_TAG, "Exception parsing RUIM record", exc);
        } catch (Throwable th) {
            if (null != null) {
                onRecordLoaded();
            }
        }
    }

    private static String[] getAssetLanguages(Context ctx) {
        String[] locales = ctx.getAssets().getLocales();
        String[] localeLangs = new String[locales.length];
        for (int i = 0; i < locales.length; i++) {
            String localeStr = locales[i];
            int separator = localeStr.indexOf(45);
            if (separator < 0) {
                localeLangs[i] = localeStr;
            } else {
                localeLangs[i] = localeStr.substring(0, separator);
            }
        }
        return localeLangs;
    }

    protected void onRecordLoaded() {
        this.mRecordsToLoad--;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onRecordLoaded ");
        stringBuilder.append(this.mRecordsToLoad);
        stringBuilder.append(" requested: ");
        stringBuilder.append(this.mRecordsRequested);
        stringBuilder.append(", ");
        stringBuilder.append(getSlotId());
        log(stringBuilder.toString());
        if (getRecordsLoaded()) {
            onAllRecordsLoaded();
        } else if (getLockedRecordsLoaded() || getNetworkLockedRecordsLoaded()) {
            onLockedAllRecordsLoaded();
        } else if (this.mRecordsToLoad < 0) {
            loge("recordsToLoad <0, programmer error suspected");
            this.mRecordsToLoad = 0;
        }
    }

    private void onLockedAllRecordsLoaded() {
        if (this.mLockedRecordsReqReason == 1) {
            this.mLockedRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
        } else if (this.mLockedRecordsReqReason == 2) {
            this.mNetworkLockedRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onLockedAllRecordsLoaded: unexpected mLockedRecordsReqReason ");
            stringBuilder.append(this.mLockedRecordsReqReason);
            loge(stringBuilder.toString());
        }
    }

    protected void onAllRecordsLoaded() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("record load complete");
        stringBuilder.append(getSlotId());
        log(stringBuilder.toString());
        if (PLUS_TRANFER_IN_AP && this.mImsi != null) {
            SystemProperties.set("ril.radio.cdma.icc_mcc", this.mImsi.substring(0, 3));
        }
        if (Resources.getSystem().getBoolean(17957064)) {
            setSimLanguage(this.mEFli, this.mEFpl);
        }
        this.mLoaded.set(true);
        this.mRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
        if (!TextUtils.isEmpty(this.mMdn)) {
            int subId = SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mParentApp.getUiccProfile().getPhoneId());
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                SubscriptionManager.from(this.mContext).setDisplayNumber(this.mMdn, subId);
            } else {
                log("Cannot call setDisplayNumber: invalid subId");
            }
        }
    }

    public void onReady() {
        fetchRuimRecords();
        this.mCi.getCDMASubscription(obtainMessage(10));
    }

    public void onGetImsiDone(Message msg) {
        if (msg != null) {
            AsyncResult ar = msg.obj;
            if (ar.exception != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception querying IMSI, Exception:");
                stringBuilder.append(ar.exception);
                loge(stringBuilder.toString());
                return;
            }
            this.mImsi = (String) ar.result;
            if (this.mImsi != null && (this.mImsi.length() < 6 || this.mImsi.length() > 15)) {
                loge("invalid IMSI ");
                this.mImsi = null;
            }
            if (this.mImsi != null) {
                try {
                    Integer.parseInt(this.mImsi.substring(0, 3));
                } catch (NumberFormatException e) {
                    loge("invalid numberic IMSI ");
                    this.mImsi = null;
                }
            }
            String operatorNumeric = getRUIMOperatorNumeric();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("NO update mccmnc=");
            stringBuilder2.append(operatorNumeric);
            log(stringBuilder2.toString());
            updateMccMncConfigWithCplmn(operatorNumeric);
            this.mImsiLoad = true;
            this.mImsiReadyRegistrants.notifyRegistrants();
        }
    }

    void recordsRequired() {
        log("recordsRequired");
        this.mRecordsRequired = true;
        fetchRuimRecords();
    }

    private void onLocked(int msg) {
        log("only fetch EF_ICCID in locked state");
        this.mLockedRecordsReqReason = msg == 32 ? 1 : 2;
        this.mFh.loadEFTransparent(IccConstants.EF_ICCID, obtainMessage(5));
        this.mRecordsToLoad++;
    }

    private void fetchRuimRecords() {
        if (this.mParentApp != null) {
            StringBuilder stringBuilder;
            if (!this.mRecordsRequested && this.mRecordsRequired && AppState.APPSTATE_READY == this.mParentApp.getState()) {
                this.mRecordsRequested = true;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("fetchRuimRecords ");
                stringBuilder2.append(this.mRecordsToLoad);
                log(stringBuilder2.toString());
                if (!HwModemCapability.isCapabilitySupport(18)) {
                    this.mCi.getCdmaGsmImsi(obtainMessage(37));
                    this.mCi.getIMSIForApp(this.mParentApp.getAid(), obtainMessage(3));
                    this.mRecordsToLoad++;
                }
                if (!getIccidSwitch()) {
                    if (IS_MODEM_CAPABILITY_GET_ICCID_AT) {
                        this.mCi.getICCID(obtainMessage(5));
                    } else {
                        this.mFh.loadEFTransparent(IccConstants.EF_ICCID, obtainMessage(5));
                    }
                    this.mRecordsToLoad++;
                }
                getPbrRecordSize();
                this.mFh.loadEFTransparent(IccConstants.EF_PL, obtainMessage(100, new EfPlLoaded()));
                this.mRecordsToLoad++;
                this.mFh.loadEFTransparent(28474, obtainMessage(100, new EfCsimLiLoaded()));
                this.mRecordsToLoad++;
                this.mFh.loadEFTransparent(28481, obtainMessage(100, new EfCsimSpnLoaded()));
                this.mRecordsToLoad++;
                this.mFh.loadEFLinearFixed(IccConstants.EF_CSIM_MDN, 1, obtainMessage(100, new EfCsimMdnLoaded()));
                this.mRecordsToLoad++;
                this.mFh.loadEFTransparent(IccConstants.EF_CSIM_IMSIM, obtainMessage(100, new EfCsimImsimLoaded()));
                this.mRecordsToLoad++;
                this.mFh.loadEFLinearFixedAll(IccConstants.EF_CSIM_CDMAHOME, obtainMessage(100, new EfCsimCdmaHomeLoaded()));
                this.mRecordsToLoad++;
                this.mFh.loadEFTransparent(IccConstants.EF_CSIM_EPRL, 4, obtainMessage(100, new EfCsimEprlLoaded()));
                this.mRecordsToLoad++;
                this.mFh.loadEFTransparent(IccConstants.EF_CSIM_MIPUPP, obtainMessage(100, new EfCsimMipUppLoaded()));
                this.mRecordsToLoad++;
                stringBuilder = new StringBuilder();
                stringBuilder.append("fetchRuimRecords ");
                stringBuilder.append(this.mRecordsToLoad);
                stringBuilder.append(" requested: ");
                stringBuilder.append(this.mRecordsRequested);
                log(stringBuilder.toString());
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("fetchRuimRecords: Abort fetching records mRecordsRequested = ");
            stringBuilder.append(this.mRecordsRequested);
            stringBuilder.append(" state = ");
            stringBuilder.append(this.mParentApp.getState());
            stringBuilder.append(" required = ");
            stringBuilder.append(this.mRecordsRequired);
            log(stringBuilder.toString());
        }
    }

    public int getDisplayRule(ServiceState serviceState) {
        return 0;
    }

    public boolean isProvisioned() {
        if (SystemProperties.getBoolean("persist.radio.test-csim", false)) {
            return true;
        }
        if (this.mParentApp == null) {
            return false;
        }
        return (this.mParentApp.getType() == AppType.APPTYPE_CSIM && (this.mMdn == null || this.mMin == null)) ? false : true;
    }

    public void setVoiceMessageWaiting(int line, int countWaiting) {
        log("RuimRecords:setVoiceMessageWaiting - NOP for CDMA");
    }

    public int getVoiceMessageCount() {
        log("RuimRecords:getVoiceMessageCount - NOP for CDMA");
        return 0;
    }

    protected void handleFileUpdate(int efid) {
        this.mAdnCache.reset();
        fetchRuimRecords();
    }

    public String getMdn() {
        return this.mMdn;
    }

    public String getMin() {
        return this.mMin;
    }

    public String getSid() {
        return this.mHomeSystemId;
    }

    public String getNid() {
        return this.mHomeNetworkId;
    }

    public boolean getCsimSpnDisplayCondition() {
        return this.mCsimSpnDisplayCondition;
    }

    protected void log(String s) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[RuimRecords] ");
        stringBuilder.append(s);
        Rlog.d(str, stringBuilder.toString());
    }

    protected void loge(String s) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[RuimRecords] ");
        stringBuilder.append(s);
        Rlog.e(str, stringBuilder.toString());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RuimRecords: ");
        stringBuilder.append(this);
        pw.println(stringBuilder.toString());
        pw.println(" extends:");
        super.dump(fd, pw, args);
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mOtaCommited=");
        stringBuilder.append(this.mOtaCommited);
        pw.println(stringBuilder.toString());
        pw.println(" mMyMobileNumber=xxx");
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mMin2Min1=");
        stringBuilder.append(this.mMin2Min1);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mPrlVersion=");
        stringBuilder.append(this.mPrlVersion);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mEFpl[]=");
        stringBuilder.append(Arrays.toString(this.mEFpl));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mEFli[]=");
        stringBuilder.append(Arrays.toString(this.mEFli));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCsimSpnDisplayCondition=");
        stringBuilder.append(this.mCsimSpnDisplayCondition);
        pw.println(stringBuilder.toString());
        pw.println(" mMdn=xxxx");
        pw.println(" mMin=xxxx");
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mHomeSystemId=");
        stringBuilder.append(this.mHomeSystemId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mHomeNetworkId=");
        stringBuilder.append(this.mHomeNetworkId);
        pw.println(stringBuilder.toString());
        pw.flush();
    }

    private void updateMccMncConfigWithCplmn(String operatorNumeric) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateMccMncConfigWithCplmn: ");
        stringBuilder.append(operatorNumeric);
        log(stringBuilder.toString());
        if (operatorNumeric != null && operatorNumeric.length() >= 5) {
            setSystemProperty("gsm.sim.operator.numeric", operatorNumeric);
            MccTable.updateMccMncConfiguration(this.mContext, operatorNumeric, false);
        }
    }
}
