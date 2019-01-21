package com.android.internal.telephony.gsm;

import android.os.AsyncResult;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.HwSubscriptionManager;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.AdnRecordCache;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HwUsimPhoneBookManagerEmailAnr extends UsimPhoneBookManager {
    private static final int ADN_RECORD_LENGTH_DEFAULT = 20;
    private static final int ANR_ADDITIONAL_NUMBER_END_ID = 12;
    private static final int ANR_ADDITIONAL_NUMBER_START_ID = 3;
    private static final int ANR_ADN_RECORD_IDENTIFIER_ID = 16;
    private static final int ANR_ADN_SFI_ID = 15;
    private static final int ANR_BCD_NUMBER_LENGTH = 1;
    private static final int ANR_CAPABILITY_ID = 13;
    private static final int ANR_DESCRIPTION_ID = 0;
    private static final int ANR_EXTENSION_ID = 14;
    private static final int ANR_RECORD_LENGTH = 15;
    private static final int ANR_TON_NPI_ID = 2;
    private static final int DATA_DESCRIPTION_ID_IN_EFEXT1 = 2;
    private static final int DATA_SIZE_IN_EFEXT1 = 13;
    private static final boolean DBG = true;
    private static final int EVENT_ANR_LOAD_DONE = 5;
    private static final int EVENT_EF_ANR_RECORD_SIZE_DONE = 7;
    private static final int EVENT_EF_EMAIL_RECORD_SIZE_DONE = 6;
    private static final int EVENT_EF_EXT1_RECORD_SIZE_DONE = 13;
    private static final int EVENT_EF_IAP_RECORD_SIZE_DONE = 10;
    private static final int EVENT_EMAIL_LOAD_DONE = 4;
    private static final int EVENT_EXT1_LOAD_DONE = 12;
    protected static final int EVENT_GET_SIZE_DONE = 101;
    private static final int EVENT_IAP_LOAD_DONE = 3;
    private static final int EVENT_PBR_LOAD_DONE = 1;
    private static final int EVENT_UPDATE_ANR_RECORD_DONE = 9;
    private static final int EVENT_UPDATE_EMAIL_RECORD_DONE = 8;
    private static final int EVENT_UPDATE_EXT1_RECORD_DONE = 14;
    private static final int EVENT_UPDATE_IAP_RECORD_DONE = 11;
    private static final int EVENT_USIM_ADN_LOAD_DONE = 2;
    private static final int EXT1_RECORD_LENGTH_MAX_DEFAULT = 10;
    private static final int EXT_DESCRIPTION_ID_IN_EFEXT1 = 0;
    private static final int EXT_TAG_IN_EFEXT1 = 2;
    private static final int FREE_TAG_IN_EFEXT1 = 0;
    private static final int LENGTH_DESCRIPTION_ID_IN_EFEXT1 = 1;
    private static final String LOG_TAG = "HwUsimPhoneBookManagerEmailAnr";
    private static final int MAX_NUMBER_SIZE_BYTES = 11;
    private static final int RECORDS_SIZE_ARRAY_VALID_LENGTH = 3;
    private static final int RECORDS_TOTAL_NUMBER_ARRAY_INDEX = 2;
    private static final int USIM_EFAAS_TAG = 199;
    private static final int USIM_EFADN_TAG = 192;
    private static final int USIM_EFANR_TAG = 196;
    private static final int USIM_EFCCP1_TAG = 203;
    private static final int USIM_EFEMAIL_TAG = 202;
    private static final int USIM_EFEXT1_TAG = 194;
    private static final int USIM_EFGRP_TAG = 198;
    private static final int USIM_EFGSD_TAG = 200;
    private static final int USIM_EFIAP_TAG = 193;
    private static final int USIM_EFPBC_TAG = 197;
    private static final int USIM_EFSNE_TAG = 195;
    private static final int USIM_EFUID_TAG = 201;
    private static final int USIM_TYPE1_TAG = 168;
    private static final int USIM_TYPE2_TAG = 169;
    private static final int USIM_TYPE3_TAG = 170;
    private AdnRecordCache mAdnCache;
    private ArrayList<Integer> mAdnLengthList;
    private Map<Integer, ArrayList<byte[]>> mAnrFileRecord;
    private Map<Integer, ArrayList<Integer>> mAnrFlags;
    private ArrayList<Integer>[] mAnrFlagsRecord;
    private boolean mAnrPresentInIap;
    private int mAnrTagNumberInIap;
    private Map<Integer, ArrayList<byte[]>> mEmailFileRecord;
    private Map<Integer, ArrayList<Integer>> mEmailFlags;
    private ArrayList<Integer>[] mEmailFlagsRecord;
    private boolean mEmailPresentInIap;
    private int mEmailTagNumberInIap;
    private Map<Integer, ArrayList<byte[]>> mExt1FileRecord;
    private Map<Integer, ArrayList<Integer>> mExt1Flags;
    private ArrayList<Integer>[] mExt1FlagsRecord;
    private IccFileHandler mFh;
    private Map<Integer, ArrayList<byte[]>> mIapFileRecord;
    private boolean mIapPresent;
    private Boolean mIsPbrPresent;
    private Object mLock;
    private PbrFile mPbrFile;
    private ArrayList<AdnRecord> mPhoneBookRecords;
    private Map<Integer, ArrayList<Integer>> mRecordNums;
    private int[] mRecordSize;
    private boolean mRefreshCache;
    private boolean mSuccess;
    private int[] temRecordSize;

    private class PbrFile {
        boolean isInvalidAnrType = false;
        boolean isInvalidEmailType = false;
        boolean isNoAnrExist = false;
        boolean isNoEmailExist = false;
        HashMap<Integer, ArrayList<Integer>> mAnrFileIds = new HashMap();
        HashMap<Integer, ArrayList<Integer>> mEmailFileIds = new HashMap();
        HashMap<Integer, Map<Integer, Integer>> mFileIds = new HashMap();

        PbrFile(ArrayList<byte[]> records) {
            int recNum = 0;
            if (records != null) {
                int list_size = records.size();
                for (int i = 0; i < list_size; i++) {
                    byte[] record = (byte[]) records.get(i);
                    String str = HwUsimPhoneBookManagerEmailAnr.LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("before making TLVs, data is ");
                    stringBuilder.append(IccUtils.bytesToHexString(record));
                    Rlog.d(str, stringBuilder.toString());
                    if (!(record == null || IccUtils.bytesToHexString(record).startsWith("ffff"))) {
                        SimTlv recTlv = new SimTlv(record, 0, record.length);
                        if (recTlv.isValidObject()) {
                            parseTag(recTlv, recNum);
                            if (this.mFileIds.get(Integer.valueOf(recNum)) != null) {
                                recNum++;
                            }
                        } else {
                            Rlog.d(HwUsimPhoneBookManagerEmailAnr.LOG_TAG, "null == recTlv || !recTlv.isValidObject() is true");
                        }
                    }
                }
            }
        }

        void parseTag(SimTlv tlv, int recNum) {
            Rlog.d(HwUsimPhoneBookManagerEmailAnr.LOG_TAG, "parseTag: recNum=xxxxxx");
            HwUsimPhoneBookManagerEmailAnr.this.mIapPresent = false;
            Map<Integer, Integer> val = new HashMap();
            ArrayList<Integer> anrList = new ArrayList();
            ArrayList<Integer> emailList = new ArrayList();
            while (true) {
                ArrayList<Integer> emailList2 = emailList;
                int tag = tlv.getTag();
                switch (tag) {
                    case HwUsimPhoneBookManagerEmailAnr.USIM_TYPE1_TAG /*168*/:
                    case HwUsimPhoneBookManagerEmailAnr.USIM_TYPE2_TAG /*169*/:
                    case HwUsimPhoneBookManagerEmailAnr.USIM_TYPE3_TAG /*170*/:
                        byte[] data = tlv.getData();
                        if (data != null && data.length != 0) {
                            parseEf(new SimTlv(data, 0, data.length), val, tag, anrList, emailList2);
                            break;
                        } else if (tag == HwUsimPhoneBookManagerEmailAnr.USIM_TYPE1_TAG) {
                            Rlog.d(HwUsimPhoneBookManagerEmailAnr.LOG_TAG, "parseTag: invalid A8 data, ignore the whole record");
                            return;
                        }
                        break;
                }
                if (tlv.nextObject()) {
                    emailList = emailList2;
                } else {
                    String str;
                    StringBuilder stringBuilder;
                    if (anrList.size() != 0) {
                        this.mAnrFileIds.put(Integer.valueOf(recNum), anrList);
                        str = HwUsimPhoneBookManagerEmailAnr.LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("parseTag: recNum=xxxxxx ANR file list:");
                        stringBuilder.append(anrList);
                        Rlog.d(str, stringBuilder.toString());
                    }
                    if (emailList2.size() != 0) {
                        str = HwUsimPhoneBookManagerEmailAnr.LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("parseTag: recNum=xxxxxx EMAIL file list:");
                        stringBuilder.append(emailList2);
                        Rlog.d(str, stringBuilder.toString());
                        this.mEmailFileIds.put(Integer.valueOf(recNum), emailList2);
                    }
                    this.mFileIds.put(Integer.valueOf(recNum), val);
                    if (val.size() != 0) {
                        if (!val.containsKey(Integer.valueOf(202))) {
                            this.isNoEmailExist = true;
                        }
                        if (!val.containsKey(Integer.valueOf(HwUsimPhoneBookManagerEmailAnr.USIM_EFANR_TAG))) {
                            this.isNoAnrExist = true;
                        }
                    }
                    return;
                }
            }
        }

        void parseEf(SimTlv tlv, Map<Integer, Integer> val, int parentTag, ArrayList<Integer> anrList, ArrayList<Integer> emailList) {
            int tagNumberWithinParentTag = 0;
            do {
                int tag = tlv.getTag();
                if (parentTag == HwUsimPhoneBookManagerEmailAnr.USIM_TYPE1_TAG && tag == HwUsimPhoneBookManagerEmailAnr.USIM_EFIAP_TAG) {
                    HwUsimPhoneBookManagerEmailAnr.this.mIapPresent = true;
                }
                if (parentTag != HwUsimPhoneBookManagerEmailAnr.USIM_TYPE2_TAG || HwUsimPhoneBookManagerEmailAnr.this.mIapPresent) {
                    StringBuilder stringBuilder;
                    if (!HwUsimPhoneBookManagerEmailAnr.this.mEmailPresentInIap && parentTag == HwUsimPhoneBookManagerEmailAnr.USIM_TYPE2_TAG && HwUsimPhoneBookManagerEmailAnr.this.mIapPresent && tag == 202) {
                        HwUsimPhoneBookManagerEmailAnr.this.mEmailPresentInIap = true;
                        HwUsimPhoneBookManagerEmailAnr.this.mEmailTagNumberInIap = tagNumberWithinParentTag;
                        HwUsimPhoneBookManagerEmailAnr hwUsimPhoneBookManagerEmailAnr = HwUsimPhoneBookManagerEmailAnr.this;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("parseEf: EmailPresentInIap tag = ");
                        stringBuilder2.append(HwUsimPhoneBookManagerEmailAnr.this.mEmailTagNumberInIap);
                        hwUsimPhoneBookManagerEmailAnr.log(stringBuilder2.toString());
                    }
                    if (!HwUsimPhoneBookManagerEmailAnr.this.mAnrPresentInIap && parentTag == HwUsimPhoneBookManagerEmailAnr.USIM_TYPE2_TAG && HwUsimPhoneBookManagerEmailAnr.this.mIapPresent && tag == HwUsimPhoneBookManagerEmailAnr.USIM_EFANR_TAG) {
                        HwUsimPhoneBookManagerEmailAnr.this.mAnrPresentInIap = true;
                        HwUsimPhoneBookManagerEmailAnr.this.mAnrTagNumberInIap = tagNumberWithinParentTag;
                        HwUsimPhoneBookManagerEmailAnr hwUsimPhoneBookManagerEmailAnr2 = HwUsimPhoneBookManagerEmailAnr.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("parseEf: AnrPresentInIap tag = ");
                        stringBuilder.append(HwUsimPhoneBookManagerEmailAnr.this.mAnrTagNumberInIap);
                        hwUsimPhoneBookManagerEmailAnr2.log(stringBuilder.toString());
                    }
                    switch (tag) {
                        case HwUsimPhoneBookManagerEmailAnr.USIM_EFADN_TAG /*192*/:
                        case HwUsimPhoneBookManagerEmailAnr.USIM_EFIAP_TAG /*193*/:
                        case HwUsimPhoneBookManagerEmailAnr.USIM_EFEXT1_TAG /*194*/:
                        case HwUsimPhoneBookManagerEmailAnr.USIM_EFSNE_TAG /*195*/:
                        case HwUsimPhoneBookManagerEmailAnr.USIM_EFANR_TAG /*196*/:
                        case HwUsimPhoneBookManagerEmailAnr.USIM_EFPBC_TAG /*197*/:
                        case HwUsimPhoneBookManagerEmailAnr.USIM_EFGRP_TAG /*198*/:
                        case HwUsimPhoneBookManagerEmailAnr.USIM_EFAAS_TAG /*199*/:
                        case 200:
                        case 201:
                        case 202:
                        case 203:
                            byte[] data = tlv.getData();
                            if (data != null && data.length >= 2) {
                                int efid = (data[1] & HwSubscriptionManager.SUB_INIT_STATE) | ((data[0] & HwSubscriptionManager.SUB_INIT_STATE) << 8);
                                String str;
                                if (!val.containsKey(Integer.valueOf(tag))) {
                                    if (!(shouldIgnoreEmail(tag, parentTag) || shouldIgnoreAnr(tag, parentTag))) {
                                        val.put(Integer.valueOf(tag), Integer.valueOf(efid));
                                        if (parentTag == HwUsimPhoneBookManagerEmailAnr.USIM_TYPE1_TAG) {
                                            if (tag == HwUsimPhoneBookManagerEmailAnr.USIM_EFANR_TAG) {
                                                anrList.add(Integer.valueOf(efid));
                                            } else if (tag == 202) {
                                                emailList.add(Integer.valueOf(efid));
                                            }
                                        }
                                        str = HwUsimPhoneBookManagerEmailAnr.LOG_TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("parseEf.put(");
                                        stringBuilder.append(tag);
                                        stringBuilder.append(",");
                                        stringBuilder.append(efid);
                                        stringBuilder.append(") parent tag:");
                                        stringBuilder.append(parentTag);
                                        Rlog.d(str, stringBuilder.toString());
                                        break;
                                    }
                                }
                                str = HwUsimPhoneBookManagerEmailAnr.LOG_TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("already have (");
                                stringBuilder.append(tag);
                                stringBuilder.append(",");
                                stringBuilder.append(efid);
                                stringBuilder.append(") parent tag:");
                                stringBuilder.append(parentTag);
                                Rlog.d(str, stringBuilder.toString());
                                break;
                            }
                    }
                    tagNumberWithinParentTag++;
                }
            } while (tlv.nextObject());
        }

        boolean shouldIgnoreEmail(int tag, int parentTag) {
            if (tag == 202 && (this.isInvalidEmailType || (parentTag != HwUsimPhoneBookManagerEmailAnr.USIM_TYPE1_TAG && parentTag != HwUsimPhoneBookManagerEmailAnr.USIM_TYPE2_TAG))) {
                HwUsimPhoneBookManagerEmailAnr.this.log("parseEf: invalid Email type!");
                this.isInvalidEmailType = true;
                return true;
            } else if (tag != 202 || !this.isNoEmailExist) {
                return false;
            } else {
                HwUsimPhoneBookManagerEmailAnr.this.log("parseEf: isNoEmailExist");
                return true;
            }
        }

        boolean shouldIgnoreAnr(int tag, int parentTag) {
            if (tag == HwUsimPhoneBookManagerEmailAnr.USIM_EFANR_TAG && (this.isInvalidAnrType || (parentTag != HwUsimPhoneBookManagerEmailAnr.USIM_TYPE1_TAG && parentTag != HwUsimPhoneBookManagerEmailAnr.USIM_TYPE2_TAG))) {
                HwUsimPhoneBookManagerEmailAnr.this.log("parseEf: invalid Anr type!");
                this.isInvalidAnrType = true;
                return true;
            } else if (tag != HwUsimPhoneBookManagerEmailAnr.USIM_EFANR_TAG || !this.isNoAnrExist) {
                return false;
            } else {
                HwUsimPhoneBookManagerEmailAnr.this.log("parseEf: isNoAnrExist");
                return true;
            }
        }
    }

    public HwUsimPhoneBookManagerEmailAnr(IccFileHandler fh) {
        super(fh, null);
        this.mLock = new Object();
        this.mEmailPresentInIap = false;
        this.mEmailTagNumberInIap = 0;
        this.mAnrPresentInIap = false;
        this.mAnrTagNumberInIap = 0;
        this.mIapPresent = false;
        this.mAdnLengthList = null;
        this.mSuccess = false;
        this.mRefreshCache = false;
        this.mRecordSize = new int[3];
        this.temRecordSize = new int[3];
        this.mFh = fh;
        this.mPhoneBookRecords = new ArrayList();
        this.mPbrFile = null;
        this.mIsPbrPresent = Boolean.valueOf(true);
    }

    public HwUsimPhoneBookManagerEmailAnr(IccFileHandler fh, AdnRecordCache cache) {
        super(fh, cache);
        this.mLock = new Object();
        this.mEmailPresentInIap = false;
        this.mEmailTagNumberInIap = 0;
        this.mAnrPresentInIap = false;
        this.mAnrTagNumberInIap = 0;
        this.mIapPresent = false;
        this.mAdnLengthList = null;
        this.mSuccess = false;
        this.mRefreshCache = false;
        this.mRecordSize = new int[3];
        this.temRecordSize = new int[3];
        this.mFh = fh;
        this.mPhoneBookRecords = new ArrayList();
        this.mAdnLengthList = new ArrayList();
        this.mIapFileRecord = new HashMap();
        this.mEmailFileRecord = new HashMap();
        this.mAnrFileRecord = new HashMap();
        this.mRecordNums = new HashMap();
        this.mPbrFile = null;
        this.mAnrFlags = new HashMap();
        this.mEmailFlags = new HashMap();
        if (IccRecords.getAdnLongNumberSupport()) {
            initExt1FileRecordAndFlags();
        }
        this.mIsPbrPresent = Boolean.valueOf(true);
        this.mAdnCache = cache;
    }

    public void reset() {
        if (!(this.mAnrFlagsRecord == null || this.mEmailFlagsRecord == null || this.mPbrFile == null)) {
            int pbsFileSize = this.mPbrFile.mFileIds.size();
            for (int i = 0; i < pbsFileSize; i++) {
                this.mAnrFlagsRecord[i].clear();
                this.mEmailFlagsRecord[i].clear();
            }
        }
        if (IccRecords.getAdnLongNumberSupport()) {
            resetExt1Variables();
        }
        this.mAnrFlags.clear();
        this.mEmailFlags.clear();
        this.mPhoneBookRecords.clear();
        this.mIapFileRecord.clear();
        this.mEmailFileRecord.clear();
        this.mAnrFileRecord.clear();
        this.mRecordNums.clear();
        this.mPbrFile = null;
        this.mAdnLengthList.clear();
        this.mIsPbrPresent = Boolean.valueOf(true);
        this.mRefreshCache = false;
    }

    /* JADX WARNING: Missing block: B:44:0x0097, code skipped:
            return r6.mPhoneBookRecords;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ArrayList<AdnRecord> loadEfFilesFromUsim() {
        synchronized (this.mLock) {
            int i = 0;
            if (!this.mPhoneBookRecords.isEmpty()) {
                if (this.mRefreshCache) {
                    this.mRefreshCache = false;
                    refreshCache();
                }
                ArrayList arrayList = this.mPhoneBookRecords;
                return arrayList;
            } else if (this.mIsPbrPresent.booleanValue()) {
                if (this.mPbrFile == null) {
                    readPbrFileAndWait();
                }
                if (this.mPbrFile == null) {
                    return null;
                }
                int i2;
                int numRecs = this.mPbrFile.mFileIds.size();
                if (this.mAnrFlagsRecord == null && this.mEmailFlagsRecord == null) {
                    this.mAnrFlagsRecord = new ArrayList[numRecs];
                    this.mEmailFlagsRecord = new ArrayList[numRecs];
                    for (i2 = 0; i2 < numRecs; i2++) {
                        this.mAnrFlagsRecord[i2] = new ArrayList();
                        this.mEmailFlagsRecord[i2] = new ArrayList();
                    }
                }
                if (this.mAdnLengthList != null && this.mAdnLengthList.size() == 0) {
                    for (i2 = 0; i2 < numRecs; i2++) {
                        this.mAdnLengthList.add(Integer.valueOf(0));
                    }
                }
                while (i < numRecs) {
                    readAdnFileAndWait(i);
                    readEmailFileAndWait(i);
                    readAnrFileAndWait(i);
                    i++;
                }
                if (IccRecords.getAdnLongNumberSupport()) {
                    loadExt1FilesFromUsim(numRecs);
                }
            } else {
                return null;
            }
        }
    }

    private void refreshCache() {
        if (this.mPbrFile != null) {
            this.mPhoneBookRecords.clear();
            int numRecs = this.mPbrFile.mFileIds.size();
            for (int i = 0; i < numRecs; i++) {
                readAdnFileAndWait(i);
            }
        }
    }

    public void invalidateCache() {
        this.mRefreshCache = true;
    }

    private void readPbrFileAndWait() {
        this.mFh.loadEFLinearFixedAll(20272, obtainMessage(1));
        try {
            this.mLock.wait();
        } catch (InterruptedException e) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readAdnFileAndWait");
        }
    }

    private void readEmailFileAndWait(int recNum) {
        if (this.mPbrFile == null) {
            Rlog.e(LOG_TAG, "mPbrFile is NULL, exiting from readEmailFileAndWait");
            return;
        }
        Map<Integer, Integer> fileIds = (Map) this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
        if (fileIds != null && fileIds.containsKey(Integer.valueOf(202))) {
            int efid;
            if (this.mEmailPresentInIap) {
                if (fileIds.containsKey(Integer.valueOf(USIM_EFIAP_TAG))) {
                    readIapFileAndWait(((Integer) fileIds.get(Integer.valueOf(USIM_EFIAP_TAG))).intValue(), recNum);
                } else {
                    log("fileIds don't contain USIM_EFIAP_TAG");
                }
                if (hasRecordIn(this.mIapFileRecord, recNum)) {
                    this.mFh.loadEFLinearFixedAll(((Integer) fileIds.get(Integer.valueOf(202))).intValue(), obtainMessage(4, Integer.valueOf(recNum)));
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("readEmailFileAndWait email efid is : ");
                    stringBuilder.append(fileIds.get(Integer.valueOf(202)));
                    log(stringBuilder.toString());
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                        Rlog.e(LOG_TAG, "Interrupted Exception in readEmailFileAndWait");
                    }
                } else {
                    Rlog.e(LOG_TAG, "Error: IAP file is empty");
                    return;
                }
            }
            Iterator it = ((ArrayList) this.mPbrFile.mEmailFileIds.get(Integer.valueOf(recNum))).iterator();
            while (it.hasNext()) {
                efid = ((Integer) it.next()).intValue();
                this.mFh.loadEFLinearFixedPartHW(efid, getValidRecordNums(recNum), obtainMessage(4, Integer.valueOf(recNum)));
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("readEmailFileAndWait email efid is : ");
                stringBuilder2.append(efid);
                stringBuilder2.append(" recNum:");
                stringBuilder2.append(recNum);
                log(stringBuilder2.toString());
                try {
                    this.mLock.wait();
                } catch (InterruptedException e2) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in readEmailFileAndWait");
                }
            }
            ArrayList<byte[]> emailFileArray = (ArrayList) this.mEmailFileRecord.get(Integer.valueOf(recNum));
            if (emailFileArray != null) {
                efid = emailFileArray.size();
                for (int m = 0; m < efid; m++) {
                    this.mEmailFlagsRecord[recNum].add(Integer.valueOf(0));
                }
            }
            this.mEmailFlags.put(Integer.valueOf(recNum), this.mEmailFlagsRecord[recNum]);
            if (hasRecordIn(this.mEmailFileRecord, recNum)) {
                updatePhoneAdnRecordWithEmail(recNum);
            } else {
                Rlog.e(LOG_TAG, "Error: Email file is empty");
            }
        }
    }

    private void readAnrFileAndWait(int recNum) {
        Map<Integer, Integer> fileIds = initFileIds(recNum);
        boolean shouldInterrupt = fileIds == null || fileIds.isEmpty();
        if (!shouldInterrupt && fileIds.containsKey(Integer.valueOf(USIM_EFANR_TAG))) {
            int efid;
            if (this.mAnrPresentInIap) {
                if (fileIds.containsKey(Integer.valueOf(USIM_EFIAP_TAG))) {
                    readIapFileAndWait(((Integer) fileIds.get(Integer.valueOf(USIM_EFIAP_TAG))).intValue(), recNum);
                } else {
                    log("fileIds don't contain USIM_EFIAP_TAG");
                }
                if (hasRecordIn(this.mIapFileRecord, recNum)) {
                    this.mFh.loadEFLinearFixedAll(((Integer) fileIds.get(Integer.valueOf(USIM_EFANR_TAG))).intValue(), obtainMessage(5, Integer.valueOf(recNum)));
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("readAnrFileAndWait anr efid is : ");
                    stringBuilder.append(fileIds.get(Integer.valueOf(USIM_EFANR_TAG)));
                    log(stringBuilder.toString());
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                        Rlog.e(LOG_TAG, "Interrupted Exception in readEmailFileAndWait");
                    }
                } else {
                    Rlog.e(LOG_TAG, "Error: IAP file is empty");
                    return;
                }
            }
            Iterator it = ((ArrayList) this.mPbrFile.mAnrFileIds.get(Integer.valueOf(recNum))).iterator();
            while (it.hasNext()) {
                efid = ((Integer) it.next()).intValue();
                this.mFh.loadEFLinearFixedPartHW(efid, getValidRecordNums(recNum), obtainMessage(5, Integer.valueOf(recNum)));
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("readAnrFileAndWait anr efid is : ");
                stringBuilder2.append(efid);
                stringBuilder2.append(" recNum:");
                stringBuilder2.append(recNum);
                log(stringBuilder2.toString());
                try {
                    this.mLock.wait();
                } catch (InterruptedException e2) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in readEmailFileAndWait");
                }
            }
            ArrayList<byte[]> anrFileArray = (ArrayList) this.mAnrFileRecord.get(Integer.valueOf(recNum));
            if (anrFileArray != null) {
                efid = anrFileArray.size();
                for (int m = 0; m < efid; m++) {
                    this.mAnrFlagsRecord[recNum].add(Integer.valueOf(0));
                }
            }
            this.mAnrFlags.put(Integer.valueOf(recNum), this.mAnrFlagsRecord[recNum]);
            if (hasRecordIn(this.mAnrFileRecord, recNum)) {
                updatePhoneAdnRecordWithAnr(recNum);
            } else {
                Rlog.e(LOG_TAG, "Error: Anr file is empty");
            }
        }
    }

    private Map<Integer, Integer> initFileIds(int recNum) {
        if (this.mPbrFile != null) {
            return (Map) this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
        }
        Rlog.e(LOG_TAG, "mPbrFile is NULL, exiting from readAnrFileAndWait");
        return null;
    }

    private void readIapFileAndWait(int efid, int recNum) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pbrIndex is ");
        stringBuilder.append(recNum);
        stringBuilder.append(",iap efid is : ");
        stringBuilder.append(efid);
        log(stringBuilder.toString());
        this.mFh.loadEFLinearFixedPartHW(efid, getValidRecordNums(recNum), obtainMessage(3, Integer.valueOf(recNum)));
        try {
            this.mLock.wait();
        } catch (InterruptedException e) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readIapFileAndWait");
        }
    }

    public boolean updateEmailFile(int adnRecNum, String oldEmail, String newEmail, int efidIndex) {
        int pbrIndex = getPbrIndexBy(adnRecNum - 1);
        int efid = getEfidByTag(pbrIndex, 202, efidIndex);
        if (oldEmail == null) {
            oldEmail = "";
        }
        if (newEmail == null) {
            newEmail = "";
        }
        String emails = new StringBuilder();
        emails.append(oldEmail);
        emails.append(",");
        emails.append(newEmail);
        emails = emails.toString();
        this.mSuccess = false;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateEmailFile  efid");
        stringBuilder.append(efid);
        stringBuilder.append(" adnRecNum: ");
        stringBuilder.append(adnRecNum);
        log(stringBuilder.toString());
        if (efid == -1) {
            return this.mSuccess;
        }
        if (!this.mEmailPresentInIap || !TextUtils.isEmpty(oldEmail) || TextUtils.isEmpty(newEmail)) {
            this.mSuccess = true;
        } else if (getEmptyEmailNum_Pbrindex(pbrIndex) == 0) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateEmailFile getEmptyEmailNum_Pbrindex=0, pbrIndex is ");
            stringBuilder2.append(pbrIndex);
            log(stringBuilder2.toString());
            this.mSuccess = false;
            return this.mSuccess;
        } else {
            this.mSuccess = updateIapFile(adnRecNum, oldEmail, newEmail, 202);
        }
        if (this.mSuccess) {
            synchronized (this.mLock) {
                this.mFh.getEFLinearRecordSize(efid, obtainMessage(6, adnRecNum, efid, emails));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "interrupted while trying to update by search");
                }
            }
        }
        if (this.mEmailPresentInIap && this.mSuccess && !TextUtils.isEmpty(oldEmail) && TextUtils.isEmpty(newEmail)) {
            this.mSuccess = updateIapFile(adnRecNum, oldEmail, newEmail, 202);
        }
        return this.mSuccess;
    }

    public boolean updateAnrFile(int adnRecNum, String oldAnr, String newAnr, int efidIndex) {
        int pbrIndex = getPbrIndexBy(adnRecNum - 1);
        int efid = getEfidByTag(pbrIndex, USIM_EFANR_TAG, efidIndex);
        if (oldAnr == null) {
            oldAnr = "";
        }
        if (newAnr == null) {
            newAnr = "";
        }
        String anrs = new StringBuilder();
        anrs.append(oldAnr);
        anrs.append(",");
        anrs.append(newAnr);
        anrs = anrs.toString();
        this.mSuccess = false;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateAnrFile  efid");
        stringBuilder.append(efid);
        stringBuilder.append(", adnRecNum: ");
        stringBuilder.append(adnRecNum);
        log(stringBuilder.toString());
        if (efid == -1) {
            return this.mSuccess;
        }
        if (!this.mAnrPresentInIap || !TextUtils.isEmpty(oldAnr) || TextUtils.isEmpty(newAnr)) {
            this.mSuccess = true;
        } else if (getEmptyAnrNum_Pbrindex(pbrIndex) == 0) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateAnrFile getEmptyAnrNum_Pbrindex=0, pbrIndex is ");
            stringBuilder2.append(pbrIndex);
            log(stringBuilder2.toString());
            this.mSuccess = false;
            return this.mSuccess;
        } else {
            this.mSuccess = updateIapFile(adnRecNum, oldAnr, newAnr, USIM_EFANR_TAG);
        }
        synchronized (this.mLock) {
            this.mFh.getEFLinearRecordSize(efid, obtainMessage(7, adnRecNum, efid, anrs));
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "interrupted while trying to update by search");
            }
        }
        if (this.mAnrPresentInIap && this.mSuccess && !TextUtils.isEmpty(oldAnr) && TextUtils.isEmpty(newAnr)) {
            this.mSuccess = updateIapFile(adnRecNum, oldAnr, newAnr, USIM_EFANR_TAG);
        }
        return this.mSuccess;
    }

    private boolean updateIapFile(int adnRecNum, String oldValue, String newValue, int tag) {
        int efid = getEfidByTag(getPbrIndexBy(adnRecNum - 1), USIM_EFIAP_TAG, 0);
        this.mSuccess = false;
        int recordNumber = -1;
        if (efid == -1) {
            return this.mSuccess;
        }
        if (tag == USIM_EFANR_TAG) {
            recordNumber = getAnrRecNumber(adnRecNum - 1, this.mPhoneBookRecords.size(), oldValue);
        } else if (tag == 202) {
            recordNumber = getEmailRecNumber(adnRecNum - 1, this.mPhoneBookRecords.size(), oldValue);
        }
        if (TextUtils.isEmpty(newValue)) {
            recordNumber = -1;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateIapFile  efid=");
        stringBuilder.append(efid);
        stringBuilder.append(", recordNumber= ");
        stringBuilder.append(recordNumber);
        stringBuilder.append(", adnRecNum=");
        stringBuilder.append(adnRecNum);
        log(stringBuilder.toString());
        synchronized (this.mLock) {
            this.mFh.getEFLinearRecordSize(efid, obtainMessage(10, adnRecNum, recordNumber, Integer.valueOf(tag)));
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "interrupted while trying to update by search");
            }
        }
        return this.mSuccess;
    }

    private int getEfidByTag(int recNum, int tag, int efidIndex) {
        if (this.mPbrFile == null || this.mPbrFile.mFileIds == null) {
            Rlog.e(LOG_TAG, "mPbrFile is NULL, exiting from getEfidByTag");
            return -1;
        }
        Map<Integer, Integer> fileIds = (Map) this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
        if (fileIds == null || !fileIds.containsKey(Integer.valueOf(tag))) {
            return -1;
        }
        int efid;
        if (!this.mEmailPresentInIap && 202 == tag) {
            efid = ((Integer) ((ArrayList) this.mPbrFile.mEmailFileIds.get(Integer.valueOf(recNum))).get(efidIndex)).intValue();
        } else if (this.mAnrPresentInIap || USIM_EFANR_TAG != tag) {
            efid = ((Integer) fileIds.get(Integer.valueOf(tag))).intValue();
        } else {
            efid = ((Integer) ((ArrayList) this.mPbrFile.mAnrFileIds.get(Integer.valueOf(recNum))).get(efidIndex)).intValue();
        }
        return efid;
    }

    public int getPbrIndexBy(int adnIndex) {
        int len = this.mAdnLengthList.size();
        int size = 0;
        for (int i = 0; i < len; i++) {
            size += ((Integer) this.mAdnLengthList.get(i)).intValue();
            if (adnIndex < size) {
                return i;
            }
        }
        return -1;
    }

    public int getPbrIndexByEfid(int efid) {
        if (!(this.mPbrFile == null || this.mPbrFile.mFileIds == null)) {
            int pbrFileIdSize = this.mPbrFile.mFileIds.size();
            for (int i = 0; i < pbrFileIdSize; i++) {
                Map<Integer, Integer> val = (Map) this.mPbrFile.mFileIds.get(Integer.valueOf(i));
                if (val != null && val.containsValue(Integer.valueOf(efid))) {
                    return i;
                }
            }
        }
        return 0;
    }

    public int getInitIndexByPbr(int pbrIndex) {
        return getInitIndexBy(pbrIndex);
    }

    private int getInitIndexBy(int pbrIndex) {
        int index = 0;
        while (pbrIndex > 0) {
            index += ((Integer) this.mAdnLengthList.get(pbrIndex - 1)).intValue();
            pbrIndex--;
        }
        return index;
    }

    private boolean hasRecordIn(ArrayList<Integer> record, int pbrIndex) {
        if (record == null || record.isEmpty() || record.size() <= pbrIndex) {
            return false;
        }
        return true;
    }

    private boolean hasRecordIn(Map<Integer, ArrayList<byte[]>> record, int pbrIndex) {
        if (record == null || record.isEmpty()) {
            return false;
        }
        try {
            if (record.get(Integer.valueOf(pbrIndex)) == null) {
                return false;
            }
            return true;
        } catch (IndexOutOfBoundsException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("record is empty in pbrIndex");
            stringBuilder.append(pbrIndex);
            Rlog.e(str, stringBuilder.toString());
            return false;
        }
    }

    private void updatePhoneAdnRecordWithEmail(int pbrIndex) {
        if (hasRecordIn(this.mEmailFileRecord, pbrIndex)) {
            int numAdnRecs = ((Integer) this.mAdnLengthList.get(pbrIndex)).intValue();
            if (this.mEmailPresentInIap && hasRecordIn(this.mIapFileRecord, pbrIndex)) {
                int adnRecIndex;
                int i = 0;
                while (i < numAdnRecs) {
                    try {
                        byte[] record = (byte[]) ((ArrayList) this.mIapFileRecord.get(Integer.valueOf(pbrIndex))).get(i);
                        int recNum = 0;
                        try {
                            byte recNum2 = record[this.mEmailTagNumberInIap];
                            if (recNum2 > (byte) 0) {
                                String[] emails = new String[]{readEmailRecord(recNum2 - 1, pbrIndex, 0)};
                                adnRecIndex = getInitIndexBy(pbrIndex) + i;
                                AdnRecord rec = (AdnRecord) this.mPhoneBookRecords.get(adnRecIndex);
                                if (!(rec == null || TextUtils.isEmpty(emails[0]))) {
                                    rec.setEmails(emails);
                                    this.mPhoneBookRecords.set(adnRecIndex, rec);
                                    ((ArrayList) this.mEmailFlags.get(Integer.valueOf(pbrIndex))).set(recNum2 - 1, Integer.valueOf(1));
                                }
                            }
                            i++;
                        } catch (IndexOutOfBoundsException e) {
                            String str = LOG_TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("updatePhoneAdnRecordWithEmail: IndexOutOfBoundsException mEmailTagNumberInIap: ");
                            stringBuilder.append(this.mEmailTagNumberInIap);
                            stringBuilder.append(" len:");
                            stringBuilder.append(record.length);
                            Rlog.e(str, stringBuilder.toString());
                        }
                    } catch (IndexOutOfBoundsException e2) {
                        Rlog.e(LOG_TAG, "Error: Improper ICC card: No IAP record for ADN, continuing");
                    }
                }
                i = ((ArrayList) this.mEmailFileRecord.get(Integer.valueOf(pbrIndex))).size();
                for (int index = 0; index < i; index++) {
                    if (1 != ((Integer) ((ArrayList) this.mEmailFlags.get(Integer.valueOf(pbrIndex))).get(index)).intValue()) {
                        if (!"".equals(readEmailRecord(index, pbrIndex, 0))) {
                            byte[] emailRec = (byte[]) ((ArrayList) this.mEmailFileRecord.get(Integer.valueOf(pbrIndex))).get(index);
                            for (adnRecIndex = 0; adnRecIndex < emailRec.length; adnRecIndex++) {
                                emailRec[adnRecIndex] = (byte) -1;
                            }
                        }
                    }
                }
                log("updatePhoneAdnRecordWithEmail: no need to parse type1 EMAIL file");
                return;
            }
            int len = ((Integer) this.mAdnLengthList.get(pbrIndex)).intValue();
            if (!this.mEmailPresentInIap) {
                parseType1EmailFile(len, pbrIndex);
            }
        }
    }

    private void updatePhoneAdnRecordWithAnr(int pbrIndex) {
        if (hasRecordIn(this.mAnrFileRecord, pbrIndex)) {
            int numAdnRecs = ((Integer) this.mAdnLengthList.get(pbrIndex)).intValue();
            if (this.mAnrPresentInIap && hasRecordIn(this.mIapFileRecord, pbrIndex)) {
                int adnRecIndex;
                int i = 0;
                while (i < numAdnRecs) {
                    try {
                        byte[] record = (byte[]) ((ArrayList) this.mIapFileRecord.get(Integer.valueOf(pbrIndex))).get(i);
                        int recNum = 0;
                        try {
                            byte recNum2 = record[this.mAnrTagNumberInIap];
                            if (recNum2 > (byte) 0) {
                                String[] anrs = new String[]{readAnrRecord(recNum2 - 1, pbrIndex, 0)};
                                adnRecIndex = getInitIndexBy(pbrIndex) + i;
                                AdnRecord rec = (AdnRecord) this.mPhoneBookRecords.get(adnRecIndex);
                                if (!(rec == null || TextUtils.isEmpty(anrs[0]))) {
                                    rec.setAdditionalNumbers(anrs);
                                    this.mPhoneBookRecords.set(adnRecIndex, rec);
                                    ((ArrayList) this.mAnrFlags.get(Integer.valueOf(pbrIndex))).set(recNum2 - 1, Integer.valueOf(1));
                                }
                            }
                            i++;
                        } catch (IndexOutOfBoundsException e) {
                            String str = LOG_TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("updatePhoneAdnRecordWithAnr: IndexOutOfBoundsException mAnrTagNumberInIap: ");
                            stringBuilder.append(this.mAnrTagNumberInIap);
                            stringBuilder.append(" len:");
                            stringBuilder.append(record.length);
                            Rlog.e(str, stringBuilder.toString());
                        }
                    } catch (IndexOutOfBoundsException e2) {
                        Rlog.e(LOG_TAG, "Error: Improper ICC card: No IAP record for ADN, continuing");
                    }
                }
                i = ((ArrayList) this.mAnrFileRecord.get(Integer.valueOf(pbrIndex))).size();
                for (int index = 0; index < i; index++) {
                    if (1 != ((Integer) ((ArrayList) this.mAnrFlags.get(Integer.valueOf(pbrIndex))).get(index)).intValue()) {
                        if (!"".equals(readAnrRecord(index, pbrIndex, 0))) {
                            byte[] anrRec = (byte[]) ((ArrayList) this.mAnrFileRecord.get(Integer.valueOf(pbrIndex))).get(index);
                            for (adnRecIndex = 0; adnRecIndex < anrRec.length; adnRecIndex++) {
                                anrRec[adnRecIndex] = (byte) -1;
                            }
                        }
                    }
                }
                log("updatePhoneAdnRecordWithAnr: no need to parse type1 ANR file");
                return;
            }
            if (!this.mAnrPresentInIap) {
                parseType1AnrFile(numAdnRecs, pbrIndex);
            }
        }
    }

    void parseType1EmailFile(int numRecs, int pbrIndex) {
        int numEmailFiles = ((ArrayList) this.mPbrFile.mEmailFileIds.get(Integer.valueOf(pbrIndex))).size();
        ArrayList<String> emailList = new ArrayList();
        int adnInitIndex = getInitIndexBy(pbrIndex);
        if (hasRecordIn(this.mEmailFileRecord, pbrIndex)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parseType1EmailFile: pbrIndex is: ");
            stringBuilder.append(pbrIndex);
            stringBuilder.append(", numRecs is: ");
            stringBuilder.append(numRecs);
            log(stringBuilder.toString());
            for (int i = 0; i < numRecs; i++) {
                emailList.clear();
                int count = 0;
                for (int j = 0; j < numEmailFiles; j++) {
                    String email = readEmailRecord(i, pbrIndex, j * numRecs);
                    emailList.add(email);
                    if (TextUtils.isEmpty(email)) {
                        email = "";
                    } else {
                        count++;
                        ((ArrayList) this.mEmailFlags.get(Integer.valueOf(pbrIndex))).set((j * numRecs) + i, Integer.valueOf(1));
                    }
                }
                if (count != 0) {
                    AdnRecord rec = (AdnRecord) this.mPhoneBookRecords.get(i + adnInitIndex);
                    if (rec != null) {
                        String[] emails = new String[emailList.size()];
                        System.arraycopy(emailList.toArray(), 0, emails, 0, emailList.size());
                        rec.setEmails(emails);
                        this.mPhoneBookRecords.set(i + adnInitIndex, rec);
                    }
                }
            }
        }
    }

    void parseType1AnrFile(int numRecs, int pbrIndex) {
        int numAnrFiles = ((ArrayList) this.mPbrFile.mAnrFileIds.get(Integer.valueOf(pbrIndex))).size();
        ArrayList<String> anrList = new ArrayList();
        int adnInitIndex = getInitIndexBy(pbrIndex);
        if (hasRecordIn(this.mAnrFileRecord, pbrIndex)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parseType1AnrFile: pbrIndex is: ");
            stringBuilder.append(pbrIndex);
            stringBuilder.append(", numRecs is: ");
            stringBuilder.append(numRecs);
            stringBuilder.append(", numAnrFiles ");
            stringBuilder.append(numAnrFiles);
            log(stringBuilder.toString());
            for (int i = 0; i < numRecs; i++) {
                anrList.clear();
                int count = 0;
                for (int j = 0; j < numAnrFiles; j++) {
                    String anr = readAnrRecord(i, pbrIndex, j * numRecs);
                    anrList.add(anr);
                    if (TextUtils.isEmpty(anr)) {
                        anr = "";
                    } else {
                        count++;
                        ((ArrayList) this.mAnrFlags.get(Integer.valueOf(pbrIndex))).set((j * numRecs) + i, Integer.valueOf(1));
                    }
                }
                if (count != 0) {
                    AdnRecord rec = (AdnRecord) this.mPhoneBookRecords.get(i + adnInitIndex);
                    if (rec != null) {
                        String[] anrs = new String[anrList.size()];
                        System.arraycopy(anrList.toArray(), 0, anrs, 0, anrList.size());
                        rec.setAdditionalNumbers(anrs);
                        this.mPhoneBookRecords.set(i + adnInitIndex, rec);
                    }
                }
            }
        }
    }

    private String readEmailRecord(int recNum, int pbrIndex, int offSet) {
        if (!hasRecordIn(this.mEmailFileRecord, pbrIndex)) {
            return null;
        }
        try {
            byte[] emailRec = (byte[]) ((ArrayList) this.mEmailFileRecord.get(Integer.valueOf(pbrIndex))).get(recNum + offSet);
            return IccUtils.adnStringFieldToString(emailRec, null, emailRec.length - 2);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private String readAnrRecord(int recNum, int pbrIndex, int offSet) {
        if (!hasRecordIn(this.mAnrFileRecord, pbrIndex)) {
            return null;
        }
        try {
            byte[] anrRec = (byte[]) ((ArrayList) this.mAnrFileRecord.get(Integer.valueOf(pbrIndex))).get(recNum + offSet);
            int numberLength = HwSubscriptionManager.SUB_INIT_STATE & anrRec[1];
            if (numberLength > 11) {
                return "";
            }
            return PhoneNumberUtils.calledPartyBCDToString(anrRec, 2, numberLength);
        } catch (IndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "Error: Improper ICC card: No anr record for ADN, continuing");
            return null;
        }
    }

    private void readAdnFileAndWait(int recNum) {
        if (this.mPbrFile == null) {
            Rlog.e(LOG_TAG, "mPbrFile is NULL, exiting from readAdnFileAndWait");
            return;
        }
        Map<Integer, Integer> fileIds = (Map) this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
        if (fileIds != null && !fileIds.isEmpty()) {
            int extEf = 0;
            if (fileIds.containsKey(Integer.valueOf(USIM_EFEXT1_TAG))) {
                extEf = ((Integer) fileIds.get(Integer.valueOf(USIM_EFEXT1_TAG))).intValue();
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("readAdnFileAndWait adn efid is : ");
            stringBuilder.append(fileIds.get(Integer.valueOf(USIM_EFADN_TAG)));
            log(stringBuilder.toString());
            if (fileIds.containsKey(Integer.valueOf(USIM_EFADN_TAG))) {
                this.mAdnCache.requestLoadAllAdnLike(((Integer) fileIds.get(Integer.valueOf(USIM_EFADN_TAG))).intValue(), extEf, obtainMessage(2, Integer.valueOf(recNum)));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in readAdnFileAndWait");
                }
            }
        }
    }

    private int getEmailRecNumber(int adnRecIndex, int numRecs, String oldEmail) {
        int pbrIndex = getPbrIndexBy(adnRecIndex);
        int recordIndex = adnRecIndex - getInitIndexBy(pbrIndex);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getEmailRecNumber adnRecIndex is: ");
        stringBuilder.append(adnRecIndex);
        stringBuilder.append(", recordIndex is :");
        stringBuilder.append(recordIndex);
        log(stringBuilder.toString());
        if (!hasRecordIn(this.mEmailFileRecord, pbrIndex)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getEmailRecNumber recordNumber is: ");
            stringBuilder.append(-1);
            log(stringBuilder.toString());
            return -1;
        } else if (!this.mEmailPresentInIap || !hasRecordIn(this.mIapFileRecord, pbrIndex)) {
            return recordIndex + 1;
        } else {
            byte[] record = null;
            try {
                record = (byte[]) ((ArrayList) this.mIapFileRecord.get(Integer.valueOf(pbrIndex))).get(recordIndex);
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "IndexOutOfBoundsException in getEmailRecNumber");
            }
            if (record == null || this.mEmailTagNumberInIap >= record.length || record[this.mEmailTagNumberInIap] == (byte) -1 || (record[this.mEmailTagNumberInIap] & HwSubscriptionManager.SUB_INIT_STATE) <= 0 || (record[this.mEmailTagNumberInIap] & HwSubscriptionManager.SUB_INIT_STATE) > ((ArrayList) this.mEmailFileRecord.get(Integer.valueOf(pbrIndex))).size()) {
                int recsSize = ((ArrayList) this.mEmailFileRecord.get(Integer.valueOf(pbrIndex))).size();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getEmailRecNumber recsSize is: ");
                stringBuilder2.append(recsSize);
                log(stringBuilder2.toString());
                if (TextUtils.isEmpty(oldEmail)) {
                    for (int i = 0; i < recsSize; i++) {
                        if (TextUtils.isEmpty(readEmailRecord(i, pbrIndex, 0))) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("getEmailRecNumber: Got empty record.Email record num is :");
                            stringBuilder2.append(i + 1);
                            log(stringBuilder2.toString());
                            return i + 1;
                        }
                    }
                }
                log("getEmailRecNumber: no email record index found");
                return -1;
            }
            int recordNumber = record[this.mEmailTagNumberInIap] & HwSubscriptionManager.SUB_INIT_STATE;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" getEmailRecNumber: record is ");
            stringBuilder3.append(IccUtils.bytesToHexString(record));
            stringBuilder3.append(", the email recordNumber is :");
            stringBuilder3.append(recordNumber);
            log(stringBuilder3.toString());
            return recordNumber;
        }
    }

    private int getAnrRecNumber(int adnRecIndex, int numRecs, String oldAnr) {
        int pbrIndex = getPbrIndexBy(adnRecIndex);
        int recordIndex = adnRecIndex - getInitIndexBy(pbrIndex);
        if (!hasRecordIn(this.mAnrFileRecord, pbrIndex)) {
            return -1;
        }
        if (!this.mAnrPresentInIap || !hasRecordIn(this.mIapFileRecord, pbrIndex)) {
            return recordIndex + 1;
        }
        byte[] record = null;
        try {
            record = (byte[]) ((ArrayList) this.mIapFileRecord.get(Integer.valueOf(pbrIndex))).get(recordIndex);
        } catch (IndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "IndexOutOfBoundsException in getAnrRecNumber");
        }
        if (record == null || this.mAnrTagNumberInIap >= record.length || record[this.mAnrTagNumberInIap] == (byte) -1 || (record[this.mAnrTagNumberInIap] & HwSubscriptionManager.SUB_INIT_STATE) <= 0 || (record[this.mAnrTagNumberInIap] & HwSubscriptionManager.SUB_INIT_STATE) > ((ArrayList) this.mAnrFileRecord.get(Integer.valueOf(pbrIndex))).size()) {
            int recsSize = ((ArrayList) this.mAnrFileRecord.get(Integer.valueOf(pbrIndex))).size();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAnrRecNumber: anr record size is :");
            stringBuilder.append(recsSize);
            log(stringBuilder.toString());
            if (TextUtils.isEmpty(oldAnr)) {
                for (int i = 0; i < recsSize; i++) {
                    if (TextUtils.isEmpty(readAnrRecord(i, pbrIndex, 0))) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("getAnrRecNumber: Empty anr record. Anr record num is :");
                        stringBuilder.append(i + 1);
                        log(stringBuilder.toString());
                        return i + 1;
                    }
                }
            }
            log("getAnrRecNumber: no anr record index found");
            return -1;
        }
        int recordNumber = record[this.mAnrTagNumberInIap] & HwSubscriptionManager.SUB_INIT_STATE;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getAnrRecNumber: recnum from iap is :");
        stringBuilder2.append(recordNumber);
        log(stringBuilder2.toString());
        return recordNumber;
    }

    private byte[] buildEmailData(int length, int adnRecIndex, String email) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) -1;
        }
        if (TextUtils.isEmpty(email)) {
            log("[buildEmailData] Empty email record");
            return data;
        }
        byte[] byteEmail = GsmAlphabet.stringToGsm8BitPacked(email);
        if (byteEmail.length > data.length) {
            System.arraycopy(byteEmail, 0, data, 0, data.length);
        } else {
            System.arraycopy(byteEmail, 0, data, 0, byteEmail.length);
        }
        int recordIndex = adnRecIndex - getInitIndexBy(getPbrIndexBy(adnRecIndex));
        if (this.mEmailPresentInIap) {
            data[length - 1] = (byte) (recordIndex + 1);
        }
        return data;
    }

    private byte[] buildAnrData(int length, int adnRecIndex, String anr) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) -1;
        }
        if (length < 15) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("The length is invalid ");
            stringBuilder.append(length);
            log(stringBuilder.toString());
            return data;
        } else if (TextUtils.isEmpty(anr)) {
            log("[buildAnrData] Empty anr record");
            return data;
        } else {
            data[0] = (byte) 0;
            byte[] byteAnr = PhoneNumberUtils.numberToCalledPartyBCD(anr);
            if (byteAnr == null) {
                return null;
            }
            if (byteAnr.length > 11) {
                System.arraycopy(byteAnr, 0, data, 2, 11);
                data[1] = (byte) 11;
            } else {
                System.arraycopy(byteAnr, 0, data, 2, byteAnr.length);
                data[1] = (byte) byteAnr.length;
            }
            data[13] = (byte) -1;
            data[14] = (byte) -1;
            if (length == 17) {
                data[16] = (byte) ((adnRecIndex - getInitIndexBy(getPbrIndexBy(adnRecIndex))) + 1);
            }
            return data;
        }
    }

    private void createPbrFile(ArrayList<byte[]> records) {
        if (records == null) {
            this.mPbrFile = null;
            this.mIsPbrPresent = Boolean.valueOf(false);
            return;
        }
        this.mPbrFile = new PbrFile(records);
    }

    private void putValidRecNums(int pbrIndex) {
        ArrayList<Integer> recordNums = new ArrayList();
        int initAdnIndex = getInitIndexBy(pbrIndex);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pbr index is ");
        stringBuilder.append(pbrIndex);
        stringBuilder.append(", initAdnIndex is ");
        stringBuilder.append(initAdnIndex);
        log(stringBuilder.toString());
        int adnListLengh = 0;
        if (this.mAdnLengthList != null) {
            adnListLengh = ((Integer) this.mAdnLengthList.get(pbrIndex)).intValue();
        }
        for (int i = 0; i < adnListLengh; i++) {
            recordNums.add(Integer.valueOf(i + 1));
        }
        if (recordNums.size() == 0) {
            recordNums.add(Integer.valueOf(1));
        }
        this.mRecordNums.put(Integer.valueOf(pbrIndex), recordNums);
    }

    private ArrayList<Integer> getValidRecordNums(int pbrIndex) {
        return (ArrayList) this.mRecordNums.get(Integer.valueOf(pbrIndex));
    }

    /* JADX WARNING: Removed duplicated region for block: B:264:0x066d  */
    /* JADX WARNING: Removed duplicated region for block: B:255:0x065e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleMessage(Message msg) {
        Message message = msg;
        String oldAnr = null;
        String newAnr = null;
        int i = message.what;
        String oldAnr2;
        String str;
        String str2;
        AsyncResult oldAnr3;
        StringBuilder stringBuilder;
        if (i != EVENT_GET_SIZE_DONE) {
            byte b = (byte) -1;
            ArrayList<byte[]> tmpList;
            StringBuilder stringBuilder2;
            int adnRecIndex;
            int efid;
            boolean z;
            String oldEmail;
            int[] recordSize;
            int recordNumber;
            byte[] data;
            StringBuilder stringBuilder3;
            int efid2;
            byte[] newAnr2;
            int adnRecIndex2;
            int tag;
            switch (i) {
                case 1:
                    oldAnr2 = null;
                    str = null;
                    str2 = null;
                    log("Loading PBR done");
                    oldAnr3 = (AsyncResult) message.obj;
                    if (oldAnr3.exception == null) {
                        createPbrFile((ArrayList) oldAnr3.result);
                    }
                    synchronized (this.mLock) {
                        this.mLock.notify();
                    }
                    break;
                case 2:
                    oldAnr2 = null;
                    str = null;
                    str2 = null;
                    log("Loading USIM ADN records done");
                    oldAnr3 = (AsyncResult) message.obj;
                    newAnr = ((Integer) oldAnr3.userObj).intValue();
                    if (oldAnr3.exception == null) {
                        try {
                            this.mPhoneBookRecords.addAll((ArrayList) oldAnr3.result);
                            while (newAnr > this.mAdnLengthList.size()) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("add empty item,pbrIndex=");
                                stringBuilder.append(newAnr);
                                stringBuilder.append(" mAdnLengthList.size=");
                                stringBuilder.append(this.mAdnLengthList.size());
                                log(stringBuilder.toString());
                                this.mAdnLengthList.add(Integer.valueOf(0));
                            }
                            this.mAdnLengthList.set(newAnr, Integer.valueOf(((ArrayList) oldAnr3.result).size()));
                            putValidRecNums(newAnr);
                        } catch (Exception e) {
                            log("Interrupted Exception in getAdnRecordsSizeAndWait");
                        }
                    } else {
                        log("can't load USIM ADN records");
                    }
                    synchronized (this.mLock) {
                        this.mLock.notify();
                    }
                    break;
                case 3:
                    oldAnr2 = null;
                    str = null;
                    str2 = null;
                    log("Loading USIM IAP records done");
                    oldAnr3 = (AsyncResult) message.obj;
                    newAnr = ((Integer) oldAnr3.userObj).intValue();
                    if (oldAnr3.exception == null) {
                        this.mIapFileRecord.put(Integer.valueOf(newAnr), (ArrayList) oldAnr3.result);
                    }
                    synchronized (this.mLock) {
                        this.mLock.notify();
                    }
                    break;
                case 4:
                    oldAnr2 = null;
                    str = null;
                    str2 = null;
                    log("Loading USIM Email records done");
                    oldAnr3 = (AsyncResult) message.obj;
                    newAnr = ((Integer) oldAnr3.userObj).intValue();
                    if (oldAnr3.exception == null && this.mPbrFile != null) {
                        tmpList = (ArrayList) this.mEmailFileRecord.get(Integer.valueOf(newAnr));
                        if (tmpList == null) {
                            this.mEmailFileRecord.put(Integer.valueOf(newAnr), (ArrayList) oldAnr3.result);
                        } else {
                            tmpList.addAll((ArrayList) oldAnr3.result);
                            this.mEmailFileRecord.put(Integer.valueOf(newAnr), tmpList);
                        }
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("handlemessage EVENT_EMAIL_LOAD_DONE size is: ");
                        stringBuilder2.append(((ArrayList) this.mEmailFileRecord.get(Integer.valueOf(newAnr))).size());
                        log(stringBuilder2.toString());
                    }
                    synchronized (this.mLock) {
                        this.mLock.notify();
                    }
                    break;
                case 5:
                    oldAnr2 = null;
                    str = null;
                    str2 = null;
                    log("Loading USIM Anr records done");
                    oldAnr3 = (AsyncResult) message.obj;
                    newAnr = ((Integer) oldAnr3.userObj).intValue();
                    if (oldAnr3.exception == null && this.mPbrFile != null) {
                        tmpList = (ArrayList) this.mAnrFileRecord.get(Integer.valueOf(newAnr));
                        if (tmpList == null) {
                            this.mAnrFileRecord.put(Integer.valueOf(newAnr), (ArrayList) oldAnr3.result);
                        } else {
                            tmpList.addAll((ArrayList) oldAnr3.result);
                            this.mAnrFileRecord.put(Integer.valueOf(newAnr), tmpList);
                        }
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("handlemessage EVENT_ANR_LOAD_DONE size is: ");
                        stringBuilder2.append(((ArrayList) this.mAnrFileRecord.get(Integer.valueOf(newAnr))).size());
                        log(stringBuilder2.toString());
                    }
                    synchronized (this.mLock) {
                        this.mLock.notify();
                    }
                    break;
                case 6:
                    String newEmail;
                    String oldEmail2;
                    oldAnr2 = null;
                    str = null;
                    str2 = null;
                    log("Loading EF_EMAIL_RECORD_SIZE_DONE");
                    oldAnr3 = (AsyncResult) message.obj;
                    newAnr = oldAnr3.userObj;
                    adnRecIndex = message.arg1 - 1;
                    efid = message.arg2;
                    String[] email = newAnr.split(",");
                    if (email.length == 1) {
                        z = false;
                        oldEmail = email[0];
                        newEmail = "";
                    } else {
                        z = false;
                        if (email.length > 1) {
                            oldEmail2 = email[0];
                            newEmail = email[1];
                            oldEmail = oldEmail2;
                        } else {
                            oldEmail2 = null;
                            oldEmail = str2;
                            if (oldAnr3.exception == null) {
                                this.mSuccess = z;
                                synchronized (this.mLock) {
                                    this.mLock.notify();
                                }
                                return;
                            }
                            recordSize = (int[]) oldAnr3.result;
                            recordNumber = getEmailRecNumber(adnRecIndex, this.mPhoneBookRecords.size(), oldEmail);
                            AsyncResult ar;
                            String str3;
                            if (recordSize.length == 3 && recordNumber <= recordSize[2]) {
                                if (recordNumber > 0) {
                                    data = buildEmailData(recordSize[0], adnRecIndex, oldEmail2);
                                    int actualRecNumber = recordNumber;
                                    if (this.mEmailPresentInIap) {
                                        ar = oldAnr3;
                                        str3 = newAnr;
                                    } else {
                                        ar = oldAnr3;
                                        oldAnr = ((ArrayList) this.mPbrFile.mEmailFileIds.get(Integer.valueOf(getPbrIndexBy(adnRecIndex)))).indexOf(Integer.valueOf(efid));
                                        if (oldAnr == -1) {
                                            stringBuilder3 = new StringBuilder();
                                            str3 = newAnr;
                                            stringBuilder3.append("wrong efid index:");
                                            stringBuilder3.append(efid);
                                            log(stringBuilder3.toString());
                                            return;
                                        }
                                        str3 = newAnr;
                                        actualRecNumber = recordNumber + (((Integer) this.mAdnLengthList.get(getPbrIndexBy(adnRecIndex))).intValue() * oldAnr);
                                        newAnr = new StringBuilder();
                                        newAnr.append("EMAIL index:");
                                        newAnr.append(oldAnr);
                                        newAnr.append(" efid:");
                                        newAnr.append(efid);
                                        newAnr.append(" actual RecNumber:");
                                        newAnr.append(actualRecNumber);
                                        log(newAnr.toString());
                                    }
                                    this.mFh.updateEFLinearFixed(efid, recordNumber, data, null, obtainMessage(8, actualRecNumber, adnRecIndex, data));
                                    str2 = oldEmail;
                                    break;
                                }
                                ar = oldAnr3;
                                str3 = newAnr;
                            } else {
                                ar = oldAnr3;
                                str3 = newAnr;
                            }
                            this.mSuccess = false;
                            synchronized (this.mLock) {
                                this.mLock.notify();
                            }
                            return;
                        }
                    }
                    oldEmail2 = newEmail;
                    if (oldAnr3.exception == null) {
                    }
                    break;
                case 7:
                    String oldAnr4;
                    oldAnr2 = null;
                    str = null;
                    str2 = null;
                    log("Loading EF_ANR_RECORD_SIZE_DONE");
                    oldAnr3 = (AsyncResult) message.obj;
                    newAnr = oldAnr3.userObj;
                    efid = message.arg1 - 1;
                    efid2 = message.arg2;
                    String[] anr = newAnr.split(",");
                    if (anr.length == 1) {
                        z = false;
                        oldAnr4 = anr[0];
                        oldEmail = "";
                    } else {
                        z = false;
                        if (anr.length > 1) {
                            oldAnr4 = anr[0];
                            oldEmail = anr[1];
                        } else {
                            oldAnr4 = oldAnr2;
                            oldEmail = str;
                        }
                    }
                    if (oldAnr3.exception != null) {
                        this.mSuccess = z;
                        synchronized (this.mLock) {
                            this.mLock.notify();
                        }
                        return;
                    }
                    int[] recordSize2 = (int[]) oldAnr3.result;
                    adnRecIndex = getAnrRecNumber(efid, this.mPhoneBookRecords.size(), oldAnr4);
                    AsyncResult ar2;
                    String anrs;
                    String newAnr3;
                    if (recordSize2.length == 3 && adnRecIndex <= recordSize2[2]) {
                        if (adnRecIndex > 0) {
                            byte[] data2 = buildAnrData(recordSize2[0], efid, oldEmail);
                            if (data2 != null) {
                                ar2 = oldAnr3;
                                i = adnRecIndex;
                                if (this.mAnrPresentInIap == null) {
                                    i = ((ArrayList) this.mPbrFile.mAnrFileIds.get(Integer.valueOf(getPbrIndexBy(efid)))).indexOf(Integer.valueOf(efid2));
                                    if (i == -1) {
                                        oldAnr = new StringBuilder();
                                        anrs = newAnr;
                                        oldAnr.append("wrong efid index:");
                                        oldAnr.append(efid2);
                                        log(oldAnr.toString());
                                        return;
                                    }
                                    anrs = newAnr;
                                    oldAnr = (((Integer) this.mAdnLengthList.get(getPbrIndexBy(efid))).intValue() * i) + adnRecIndex;
                                    newAnr = new StringBuilder();
                                    newAnr3 = oldEmail;
                                    newAnr.append("ANR index:");
                                    newAnr.append(i);
                                    newAnr.append(" efid:");
                                    newAnr.append(efid2);
                                    newAnr.append(" actual RecNumber:");
                                    newAnr.append(oldAnr);
                                    log(newAnr.toString());
                                } else {
                                    anrs = newAnr;
                                    newAnr3 = oldEmail;
                                    oldAnr = i;
                                }
                                this.mFh.updateEFLinearFixed(efid2, adnRecIndex, data2, null, obtainMessage(9, oldAnr, efid, data2));
                                oldAnr2 = oldAnr4;
                                str = newAnr3;
                                break;
                            }
                            this.mSuccess = false;
                            ar2 = oldAnr3;
                            synchronized (this.mLock) {
                                this.mLock.notify();
                            }
                            return;
                        }
                        ar2 = oldAnr3;
                        anrs = newAnr;
                        newAnr3 = oldEmail;
                    } else {
                        ar2 = oldAnr3;
                        anrs = newAnr;
                        newAnr3 = oldEmail;
                    }
                    this.mSuccess = false;
                    synchronized (this.mLock) {
                        this.mLock.notify();
                    }
                    return;
                case 8:
                    oldAnr2 = null;
                    str = null;
                    str2 = null;
                    log("Loading UPDATE_EMAIL_RECORD_DONE");
                    oldAnr3 = (AsyncResult) message.obj;
                    if (oldAnr3.exception != null) {
                        this.mSuccess = false;
                    }
                    newAnr2 = (byte[]) oldAnr3.userObj;
                    oldEmail = message.arg1;
                    efid2 = getPbrIndexBy(message.arg2);
                    log("EVENT_UPDATE_EMAIL_RECORD_DONE");
                    this.mSuccess = true;
                    if (hasRecordIn(this.mEmailFileRecord, efid2)) {
                        ((ArrayList) this.mEmailFileRecord.get(Integer.valueOf(efid2))).set(oldEmail - 1, newAnr2);
                        i = 0;
                        while (i < newAnr2.length) {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("EVENT_UPDATE_EMAIL_RECORD_DONE data = ");
                            stringBuilder3.append(newAnr2[i]);
                            stringBuilder3.append(",i is ");
                            stringBuilder3.append(i);
                            log(stringBuilder3.toString());
                            if (newAnr2[i] != (byte) -1) {
                                log("EVENT_UPDATE_EMAIL_RECORD_DONE data !=0xff");
                                ((ArrayList) this.mEmailFlags.get(Integer.valueOf(efid2))).set(oldEmail - 1, Integer.valueOf(1));
                            } else {
                                ((ArrayList) this.mEmailFlags.get(Integer.valueOf(efid2))).set(oldEmail - 1, Integer.valueOf(0));
                                i++;
                            }
                        }
                    } else {
                        log("Email record is empty");
                    }
                    synchronized (this.mLock) {
                        this.mLock.notify();
                    }
                    break;
                case 9:
                    oldAnr2 = null;
                    str = null;
                    str2 = null;
                    log("Loading UPDATE_ANR_RECORD_DONE");
                    oldAnr3 = (AsyncResult) message.obj;
                    newAnr2 = (byte[]) oldAnr3.userObj;
                    oldEmail = message.arg1;
                    adnRecIndex = getPbrIndexBy(message.arg2);
                    if (oldAnr3.exception != null) {
                        this.mSuccess = false;
                    }
                    log("EVENT_UPDATE_ANR_RECORD_DONE");
                    this.mSuccess = true;
                    if (hasRecordIn(this.mAnrFileRecord, adnRecIndex)) {
                        ((ArrayList) this.mAnrFileRecord.get(Integer.valueOf(adnRecIndex))).set(oldEmail - 1, newAnr2);
                        i = 0;
                        while (i < newAnr2.length) {
                            if (newAnr2[i] != (byte) -1) {
                                ((ArrayList) this.mAnrFlags.get(Integer.valueOf(adnRecIndex))).set(oldEmail - 1, Integer.valueOf(1));
                            } else {
                                ((ArrayList) this.mAnrFlags.get(Integer.valueOf(adnRecIndex))).set(oldEmail - 1, Integer.valueOf(0));
                                i++;
                            }
                        }
                    } else {
                        log("Anr record is empty");
                    }
                    synchronized (this.mLock) {
                        this.mLock.notify();
                    }
                    break;
                case 10:
                    oldAnr2 = null;
                    str = null;
                    log("EVENT_EF_IAP_RECORD_SIZE_DONE");
                    oldAnr3 = (AsyncResult) message.obj;
                    newAnr = message.arg2;
                    adnRecIndex2 = message.arg1 - 1;
                    efid2 = getEfidByTag(getPbrIndexBy(adnRecIndex2), USIM_EFIAP_TAG, 0);
                    tag = ((Integer) oldAnr3.userObj).intValue();
                    if (oldAnr3.exception == null) {
                        adnRecIndex = getPbrIndexBy(adnRecIndex2);
                        efid2 = getEfidByTag(adnRecIndex, USIM_EFIAP_TAG, 0);
                        int[] recordSize3 = (int[]) oldAnr3.result;
                        recordNumber = adnRecIndex2 - getInitIndexBy(adnRecIndex);
                        stringBuilder = new StringBuilder();
                        AsyncResult ar3 = oldAnr3;
                        stringBuilder.append("handleIAP_RECORD_SIZE_DONE adnRecIndex is: ");
                        stringBuilder.append(adnRecIndex2);
                        stringBuilder.append(", recordNumber is: ");
                        stringBuilder.append(newAnr);
                        stringBuilder.append(", recordIndex is: ");
                        stringBuilder.append(recordNumber);
                        log(stringBuilder.toString());
                        if (!isIapRecordParamInvalid(recordSize3, recordNumber, newAnr)) {
                            if (!hasRecordIn(this.mIapFileRecord, adnRecIndex)) {
                                str2 = null;
                                break;
                            }
                            data = (byte[]) ((ArrayList) this.mIapFileRecord.get(Integer.valueOf(adnRecIndex))).get(recordNumber);
                            oldAnr = new byte[data.length];
                            str2 = null;
                            System.arraycopy(data, 0, oldAnr, 0, oldAnr.length);
                            if (tag == USIM_EFANR_TAG) {
                                oldAnr[this.mAnrTagNumberInIap] = (byte) newAnr;
                            } else if (tag == 202) {
                                oldAnr[this.mEmailTagNumberInIap] = (byte) newAnr;
                            }
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(" IAP  efid= ");
                            stringBuilder2.append(efid2);
                            stringBuilder2.append(", update IAP index= ");
                            stringBuilder2.append(recordNumber);
                            stringBuilder2.append(" with value= ");
                            stringBuilder2.append(IccUtils.bytesToHexString(oldAnr));
                            log(stringBuilder2.toString());
                            this.mFh.updateEFLinearFixed(efid2, recordNumber + 1, oldAnr, null, obtainMessage(11, adnRecIndex2, newAnr, oldAnr));
                            break;
                        }
                        this.mSuccess = false;
                        synchronized (this.mLock) {
                            this.mLock.notify();
                        }
                        return;
                    }
                    this.mSuccess = false;
                    synchronized (this.mLock) {
                        this.mLock.notify();
                    }
                    return;
                case 11:
                    oldAnr2 = null;
                    str = null;
                    log("EVENT_UPDATE_IAP_RECORD_DONE");
                    oldAnr3 = (AsyncResult) message.obj;
                    if (oldAnr3.exception != null) {
                        this.mSuccess = false;
                    }
                    newAnr2 = (byte[]) oldAnr3.userObj;
                    adnRecIndex2 = message.arg1;
                    adnRecIndex = getPbrIndexBy(adnRecIndex2);
                    efid = adnRecIndex2 - getInitIndexBy(adnRecIndex);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("handleMessage EVENT_UPDATE_IAP_RECORD_DONE recordIndex is: ");
                    stringBuilder.append(efid);
                    stringBuilder.append(", adnRecIndex is: ");
                    stringBuilder.append(adnRecIndex2);
                    log(stringBuilder.toString());
                    this.mSuccess = true;
                    if (hasRecordIn(this.mIapFileRecord, adnRecIndex)) {
                        ((ArrayList) this.mIapFileRecord.get(Integer.valueOf(adnRecIndex))).set(efid, newAnr2);
                        log("Iap record is added");
                    } else {
                        log("Iap record is empty");
                    }
                    synchronized (this.mLock) {
                        this.mLock.notify();
                    }
                    break;
                case 12:
                    oldAnr2 = null;
                    str = null;
                    log("LOAD_EXT1_RECORDS_DONE");
                    oldAnr3 = (AsyncResult) message.obj;
                    newAnr = message.arg1;
                    adnRecIndex2 = message.arg2;
                    if (oldAnr3.exception == null) {
                        tmpList = (ArrayList) this.mExt1FileRecord.get(Integer.valueOf(newAnr));
                        if (tmpList == null) {
                            this.mExt1FileRecord.put(Integer.valueOf(newAnr), (ArrayList) oldAnr3.result);
                        } else {
                            tmpList.addAll((ArrayList) oldAnr3.result);
                            this.mExt1FileRecord.put(Integer.valueOf(newAnr), tmpList);
                        }
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("handlemessage EVENT_EXT1_LOAD_DONE size is: ");
                        stringBuilder4.append(((ArrayList) this.mExt1FileRecord.get(Integer.valueOf(newAnr))).size());
                        log(stringBuilder4.toString());
                        ArrayList<byte[]> ext1FileArray = (ArrayList) this.mExt1FileRecord.get(Integer.valueOf(newAnr));
                        if (ext1FileArray != null) {
                            if (this.mExt1FlagsRecord == null) {
                                this.mExt1FlagsRecord = new ArrayList[(newAnr + 1)];
                                this.mExt1FlagsRecord[newAnr] = new ArrayList();
                            }
                            if (newAnr < this.mExt1FlagsRecord.length) {
                                efid = ext1FileArray.size();
                                for (efid2 = 0; efid2 < efid; efid2++) {
                                    this.mExt1FlagsRecord[newAnr].add(Integer.valueOf(0));
                                }
                            }
                        }
                        this.mExt1Flags.put(Integer.valueOf(newAnr), this.mExt1FlagsRecord[newAnr]);
                        if (adnRecIndex2 == 28474) {
                            updateExt1RecordFlagsForSim(newAnr);
                        } else {
                            updateExt1RecordFlags(newAnr);
                        }
                    }
                    synchronized (this.mLock) {
                        this.mLock.notify();
                    }
                    break;
                case 13:
                    oldAnr2 = null;
                    str = null;
                    log("LOAD_EXT1_RECORD_SIZE_DONE");
                    oldAnr3 = (AsyncResult) message.obj;
                    AdnRecord newAnr4 = (AdnRecord) oldAnr3.userObj;
                    String mNumber = newAnr4.getNumber();
                    efid2 = message.arg1 - 1;
                    tag = message.arg2;
                    if (oldAnr3.exception == null) {
                        recordSize = (int[]) oldAnr3.result;
                        recordNumber = newAnr4.getExtRecord();
                        if (recordSize.length == 3 && recordNumber <= recordSize[2] && recordNumber > 0) {
                            String newExt1;
                            if (mNumber.length() > 20) {
                                newExt1 = mNumber.substring(20);
                            } else {
                                newExt1 = "";
                                newAnr4.setExtRecord(HwSubscriptionManager.SUB_INIT_STATE);
                            }
                            data = buildExt1Data(recordSize[0], efid2, newExt1);
                            this.mFh.updateEFLinearFixed(tag, recordNumber, data, null, obtainMessage(14, recordNumber, efid2, data));
                            break;
                        }
                        this.mSuccess = false;
                        synchronized (this.mLock) {
                            this.mLock.notify();
                        }
                        return;
                    }
                    this.mSuccess = false;
                    synchronized (this.mLock) {
                        this.mLock.notify();
                    }
                    return;
                case 14:
                    log("UPDATE_EXT1_RECORD_DONE");
                    AsyncResult ar4 = message.obj;
                    if (ar4.exception == null) {
                        byte[] data3 = (byte[]) ar4.userObj;
                        adnRecIndex2 = message.arg1;
                        efid2 = getPbrIndexBy(message.arg2);
                        this.mSuccess = true;
                        if (hasRecordIn(this.mExt1FileRecord, efid2)) {
                            ((ArrayList) this.mExt1FileRecord.get(Integer.valueOf(efid2))).set(adnRecIndex2 - 1, data3);
                            i = 0;
                            while (i < data3.length) {
                                if (data3[i] == b || data3[i] == (byte) 0) {
                                    oldAnr2 = oldAnr;
                                    str = newAnr;
                                    ((ArrayList) this.mExt1Flags.get(Integer.valueOf(efid2))).set(adnRecIndex2 - 1, Integer.valueOf(0));
                                    i++;
                                    oldAnr = oldAnr2;
                                    newAnr = str;
                                    b = (byte) -1;
                                } else {
                                    log("EVENT_UPDATE_EXT1_RECORD_DONE data !=0xff and 0x00");
                                    ((ArrayList) this.mExt1Flags.get(Integer.valueOf(efid2))).set(adnRecIndex2 - 1, Integer.valueOf(1));
                                    str = newAnr;
                                }
                            }
                            str = newAnr;
                        } else {
                            oldAnr2 = null;
                            str = null;
                            log("Ext1 record is empty");
                        }
                        synchronized (this.mLock) {
                            this.mLock.notify();
                        }
                        break;
                    }
                    this.mSuccess = false;
                    synchronized (this.mLock) {
                        this.mLock.notify();
                    }
                    return;
                default:
                    oldAnr2 = null;
                    str = null;
                    break;
            }
            str2 = null;
        } else {
            oldAnr2 = null;
            str = null;
            str2 = null;
            oldAnr3 = message.obj;
            synchronized (this.mLock) {
                if (oldAnr3.exception == null) {
                    this.mRecordSize = (int[]) oldAnr3.result;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("GET_RECORD_SIZE Size ");
                    stringBuilder.append(this.mRecordSize[0]);
                    stringBuilder.append(" total ");
                    stringBuilder.append(this.mRecordSize[1]);
                    stringBuilder.append(" #record ");
                    stringBuilder.append(this.mRecordSize[2]);
                    log(stringBuilder.toString());
                }
                this.mLock.notify();
            }
        }
    }

    private boolean isIapRecordParamInvalid(int[] recordSize, int recordIndex, int recordNumber) {
        return 3 != recordSize.length || recordIndex + 1 > recordSize[2] || recordIndex < 0 || recordNumber == 0;
    }

    private void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    public int getAnrCount() {
        StringBuilder stringBuilder;
        int count = 0;
        int j = 0;
        if (this.mAnrPresentInIap && hasRecordIn(this.mIapFileRecord, 0)) {
            try {
                byte[] record = (byte[]) ((ArrayList) this.mIapFileRecord.get(Integer.valueOf(0))).get(0);
                if (record != null && this.mAnrTagNumberInIap >= record.length) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getAnrCount mAnrTagNumberInIap: ");
                    stringBuilder.append(this.mAnrTagNumberInIap);
                    stringBuilder.append(" len:");
                    stringBuilder.append(record.length);
                    log(stringBuilder.toString());
                    return 0;
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "Error: getAnrCount ICC card: No IAP record for ADN, continuing");
                return 0;
            }
        }
        while (j < this.mAnrFlags.size()) {
            count += ((ArrayList) this.mAnrFlags.get(Integer.valueOf(j))).size();
            j++;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("getAnrCount count is: ");
        stringBuilder.append(count);
        log(stringBuilder.toString());
        return count;
    }

    public int getEmailCount() {
        StringBuilder stringBuilder;
        int count = 0;
        int j = 0;
        if (this.mEmailPresentInIap && hasRecordIn(this.mIapFileRecord, 0)) {
            try {
                byte[] record = (byte[]) ((ArrayList) this.mIapFileRecord.get(Integer.valueOf(0))).get(0);
                if (record != null && this.mEmailTagNumberInIap >= record.length) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getEmailCount mEmailTagNumberInIap: ");
                    stringBuilder.append(this.mEmailTagNumberInIap);
                    stringBuilder.append(" len:");
                    stringBuilder.append(record.length);
                    log(stringBuilder.toString());
                    return 0;
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "Error: getEmailCount ICC card: No IAP record for ADN, continuing");
                return 0;
            }
        }
        while (j < this.mEmailFlags.size()) {
            count += ((ArrayList) this.mEmailFlags.get(Integer.valueOf(j))).size();
            j++;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("getEmailCount count is: ");
        stringBuilder.append(count);
        log(stringBuilder.toString());
        return count;
    }

    public int getSpareAnrCount() {
        int pbrIndex = this.mAnrFlags.size();
        int count = 0;
        int j = 0;
        while (j < pbrIndex) {
            int anrFlagSize = 0;
            if (this.mAnrFlags.get(Integer.valueOf(j)) != null) {
                anrFlagSize = ((ArrayList) this.mAnrFlags.get(Integer.valueOf(j))).size();
            }
            int count2 = count;
            for (count = 0; count < anrFlagSize; count++) {
                if (((Integer) ((ArrayList) this.mAnrFlags.get(Integer.valueOf(j))).get(count)).intValue() == 0) {
                    count2++;
                }
            }
            j++;
            count = count2;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getSpareAnrCount count is");
        stringBuilder.append(count);
        log(stringBuilder.toString());
        return count;
    }

    public int getSpareEmailCount() {
        int pbrIndex = this.mEmailFlags.size();
        int count = 0;
        int j = 0;
        while (j < pbrIndex) {
            int emailFlagSize = 0;
            if (this.mEmailFlags.get(Integer.valueOf(j)) != null) {
                emailFlagSize = ((ArrayList) this.mEmailFlags.get(Integer.valueOf(j))).size();
            }
            int count2 = count;
            for (count = 0; count < emailFlagSize; count++) {
                if (((Integer) ((ArrayList) this.mEmailFlags.get(Integer.valueOf(j))).get(count)).intValue() == 0) {
                    count2++;
                }
            }
            j++;
            count = count2;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getSpareEmailCount count is: ");
        stringBuilder.append(count);
        log(stringBuilder.toString());
        return count;
    }

    public int getUsimAdnCount() {
        if (this.mPhoneBookRecords == null || this.mPhoneBookRecords.isEmpty()) {
            return 0;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getUsimAdnCount count is");
        stringBuilder.append(this.mPhoneBookRecords.size());
        log(stringBuilder.toString());
        return this.mPhoneBookRecords.size();
    }

    public int getEmptyEmailNum_Pbrindex(int pbrindex) {
        int count = 0;
        if (!this.mEmailPresentInIap) {
            return 1;
        }
        if (this.mEmailFlags.containsKey(Integer.valueOf(pbrindex))) {
            int size = ((ArrayList) this.mEmailFlags.get(Integer.valueOf(pbrindex))).size();
            for (int i = 0; i < size; i++) {
                if (((Integer) ((ArrayList) this.mEmailFlags.get(Integer.valueOf(pbrindex))).get(i)).intValue() == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getEmptyAnrNum_Pbrindex(int pbrindex) {
        int count = 0;
        if (!this.mAnrPresentInIap) {
            return 1;
        }
        if (this.mAnrFlags.containsKey(Integer.valueOf(pbrindex))) {
            int size = ((ArrayList) this.mAnrFlags.get(Integer.valueOf(pbrindex))).size();
            for (int i = 0; i < size; i++) {
                if (((Integer) ((ArrayList) this.mAnrFlags.get(Integer.valueOf(pbrindex))).get(i)).intValue() == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getEmailFilesCountEachAdn() {
        if (this.mPbrFile == null) {
            Rlog.e(LOG_TAG, "mPbrFile is NULL, exiting from getEmailFilesCountEachAdn");
            return 0;
        }
        Map<Integer, Integer> fileIds = (Map) this.mPbrFile.mFileIds.get(Integer.valueOf(0));
        if (fileIds == null || !fileIds.containsKey(Integer.valueOf(202))) {
            return 0;
        }
        if (this.mEmailPresentInIap) {
            return 1;
        }
        return ((ArrayList) this.mPbrFile.mEmailFileIds.get(Integer.valueOf(0))).size();
    }

    public int getAnrFilesCountEachAdn() {
        if (this.mPbrFile == null) {
            Rlog.e(LOG_TAG, "mPbrFile is NULL, exiting from getAnrFilesCountEachAdn");
            return 0;
        }
        Map<Integer, Integer> fileIds = (Map) this.mPbrFile.mFileIds.get(Integer.valueOf(0));
        if (fileIds == null || !fileIds.containsKey(Integer.valueOf(USIM_EFANR_TAG))) {
            return 0;
        }
        if (this.mAnrPresentInIap) {
            return 1;
        }
        return ((ArrayList) this.mPbrFile.mAnrFileIds.get(Integer.valueOf(0))).size();
    }

    public int getAdnRecordsFreeSize() {
        int freeRecs = 0;
        log("getAdnRecordsFreeSize(): enter.");
        int totalRecs = getUsimAdnCount();
        if (totalRecs != 0) {
            for (int i = 0; i < totalRecs; i++) {
                if (((AdnRecord) this.mPhoneBookRecords.get(i)).isEmpty()) {
                    freeRecs++;
                }
            }
        } else {
            log("getAdnRecordsFreeSize(): error. ");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getAdnRecordsFreeSize(): freeRecs = ");
        stringBuilder.append(freeRecs);
        log(stringBuilder.toString());
        return freeRecs;
    }

    public ArrayList<AdnRecord> getPhonebookRecords() {
        if (this.mPhoneBookRecords.isEmpty()) {
            return null;
        }
        return this.mPhoneBookRecords;
    }

    public void setIccFileHandler(IccFileHandler fh) {
        this.mFh = fh;
    }

    public int[] getAdnRecordsSizeFromEF() {
        synchronized (this.mLock) {
            if (this.mIsPbrPresent.booleanValue()) {
                if (this.mPbrFile == null) {
                    readPbrFileAndWait();
                }
                if (this.mPbrFile == null) {
                    return null;
                }
                int numRecs = this.mPbrFile.mFileIds.size();
                this.temRecordSize[0] = 0;
                this.temRecordSize[1] = 0;
                this.temRecordSize[2] = 0;
                for (int i = 0; i < numRecs; i++) {
                    this.mRecordSize[0] = 0;
                    this.mRecordSize[1] = 0;
                    this.mRecordSize[2] = 0;
                    getAdnRecordsSizeAndWait(i);
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getAdnRecordsSizeFromEF: recordSize[2]=");
                    stringBuilder.append(this.mRecordSize[2]);
                    Rlog.d(str, stringBuilder.toString());
                    if (this.mRecordSize[0] != 0) {
                        this.temRecordSize[0] = this.mRecordSize[0];
                    }
                    if (this.mRecordSize[1] != 0) {
                        this.temRecordSize[1] = this.mRecordSize[1];
                    }
                    this.temRecordSize[2] = this.mRecordSize[2] + this.temRecordSize[2];
                }
                String str2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getAdnRecordsSizeFromEF: temRecordSize[2]=");
                stringBuilder2.append(this.temRecordSize[2]);
                Rlog.d(str2, stringBuilder2.toString());
                int[] iArr = this.temRecordSize;
                return iArr;
            }
            return null;
        }
    }

    public void getAdnRecordsSizeAndWait(int recNum) {
        if (this.mPbrFile != null) {
            Map<Integer, Integer> fileIds = (Map) this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
            if (fileIds != null && !fileIds.isEmpty()) {
                int efid = ((Integer) fileIds.get(Integer.valueOf(USIM_EFADN_TAG))).intValue();
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getAdnRecordsSize: efid=");
                stringBuilder.append(efid);
                Rlog.d(str, stringBuilder.toString());
                this.mFh.getEFLinearRecordSize(efid, obtainMessage(EVENT_GET_SIZE_DONE));
                boolean isWait = true;
                while (isWait) {
                    try {
                        this.mLock.wait();
                        isWait = false;
                    } catch (InterruptedException e) {
                        Rlog.e(LOG_TAG, "Interrupted Exception in getAdnRecordsSizeAndWait");
                    }
                }
            }
        }
    }

    public int getPbrFileSize() {
        int size = 0;
        if (!(this.mPbrFile == null || this.mPbrFile.mFileIds == null)) {
            size = this.mPbrFile.mFileIds.size();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPbrFileSize:");
        stringBuilder.append(size);
        log(stringBuilder.toString());
        return size;
    }

    public int getEFidInPBR(int recNum, int tag) {
        int efid = 0;
        if (this.mPbrFile == null) {
            return 0;
        }
        Map<Integer, Integer> fileIds = (Map) this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
        if (fileIds == null) {
            return 0;
        }
        if (fileIds.containsKey(Integer.valueOf(tag))) {
            efid = ((Integer) fileIds.get(Integer.valueOf(tag))).intValue();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getEFidInPBR, efid = ");
        stringBuilder.append(efid);
        stringBuilder.append(", recNum = ");
        stringBuilder.append(recNum);
        stringBuilder.append(", tag = ");
        stringBuilder.append(tag);
        log(stringBuilder.toString());
        return efid;
    }

    private void initExt1FileRecordAndFlags() {
        this.mExt1FileRecord = new HashMap();
        this.mExt1Flags = new HashMap();
    }

    private void resetExt1Variables() {
        int i = 0;
        if (this.mExt1FlagsRecord != null && this.mPbrFile != null) {
            while (true) {
                int i2 = i;
                if (i2 >= this.mExt1FlagsRecord.length) {
                    break;
                }
                this.mExt1FlagsRecord[i2].clear();
                i = i2 + 1;
            }
        } else if (this.mExt1FlagsRecord != null && this.mPbrFile == null) {
            this.mExt1FlagsRecord[0].clear();
        }
        this.mExt1Flags.clear();
        this.mExt1FileRecord.clear();
    }

    private void loadExt1FilesFromUsim(int numRecs) {
        this.mExt1FlagsRecord = new ArrayList[numRecs];
        int i = 0;
        for (int i2 = 0; i2 < numRecs; i2++) {
            this.mExt1FlagsRecord[i2] = new ArrayList();
        }
        while (i < numRecs) {
            readExt1FileAndWait(i);
            i++;
        }
    }

    private void readExt1FileAndWait(int recNum) {
        if (this.mPbrFile == null) {
            Rlog.e(LOG_TAG, "mPbrFile is NULL, exiting from readExt1FileAndWait");
            return;
        }
        Map<Integer, Integer> fileIds = (Map) this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
        if (fileIds == null || fileIds.isEmpty()) {
            Rlog.e(LOG_TAG, "fileIds is NULL, exiting from readExt1FileAndWait");
            return;
        }
        if (fileIds.containsKey(Integer.valueOf(USIM_EFEXT1_TAG))) {
            this.mFh.loadEFLinearFixedAll(((Integer) fileIds.get(Integer.valueOf(USIM_EFEXT1_TAG))).intValue(), obtainMessage(12, recNum, ((Integer) fileIds.get(Integer.valueOf(USIM_EFEXT1_TAG))).intValue()));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("readExt1FileAndWait EXT1 efid is : ");
            stringBuilder.append(fileIds.get(Integer.valueOf(USIM_EFEXT1_TAG)));
            log(stringBuilder.toString());
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readAdnFileAndWait");
            }
        }
    }

    private void updateExt1RecordFlags(int pbrIndex) {
        if (hasRecordIn(this.mExt1FileRecord, pbrIndex) && hasRecordIn(this.mAdnLengthList, pbrIndex)) {
            int i;
            int numAdnRecs = ((Integer) this.mAdnLengthList.get(pbrIndex)).intValue();
            for (i = 0; i < numAdnRecs; i++) {
                AdnRecord rec = (AdnRecord) this.mPhoneBookRecords.get(getInitIndexBy(pbrIndex) + i);
                if (rec != null && rec.getExtRecord() != HwSubscriptionManager.SUB_INIT_STATE && rec.getExtRecord() > 0 && rec.getExtRecord() <= ((ArrayList) this.mExt1Flags.get(Integer.valueOf(pbrIndex))).size()) {
                    ((ArrayList) this.mExt1Flags.get(Integer.valueOf(pbrIndex))).set(rec.getExtRecord() - 1, Integer.valueOf(1));
                }
            }
            i = ((ArrayList) this.mExt1FileRecord.get(Integer.valueOf(pbrIndex))).size();
            for (int index = 0; index < i; index++) {
                if (1 != ((Integer) ((ArrayList) this.mExt1Flags.get(Integer.valueOf(pbrIndex))).get(index)).intValue()) {
                    byte[] extRec = (byte[]) ((ArrayList) this.mExt1FileRecord.get(Integer.valueOf(pbrIndex))).get(index);
                    String extRecord = readExt1Record(pbrIndex, index, 0);
                    if (extRec != null && extRec.length > 0 && extRec[0] == (byte) 2 && "".equals(extRecord)) {
                        for (int i2 = 0; i2 < extRec.length; i2++) {
                            extRec[i2] = (byte) -1;
                        }
                    }
                }
            }
            log("updateExt1RecordFlags done");
        }
    }

    public void readExt1FileForSim(int efid) {
        if (efid == 28474) {
            this.mFh.loadEFLinearFixedAll(28490, obtainMessage(12, 0, efid));
            log("readExt1FileForSim Ext1 efid is : 28490");
        }
    }

    private void updateExt1RecordFlagsForSim(int recNum) {
        int i;
        this.mPhoneBookRecords = this.mAdnCache.getAdnFilesForSim();
        int numAdnRecs = this.mPhoneBookRecords.size();
        if (this.mAdnLengthList.size() == 0) {
            this.mAdnLengthList.add(Integer.valueOf(0));
        }
        this.mAdnLengthList.set(recNum, Integer.valueOf(numAdnRecs));
        for (i = 0; i < numAdnRecs; i++) {
            AdnRecord rec = (AdnRecord) this.mPhoneBookRecords.get(i);
            if (rec != null && rec.getExtRecord() != HwSubscriptionManager.SUB_INIT_STATE && rec.getExtRecord() > 0 && rec.getExtRecord() <= ((ArrayList) this.mExt1Flags.get(Integer.valueOf(recNum))).size()) {
                ((ArrayList) this.mExt1Flags.get(Integer.valueOf(recNum))).set(rec.getExtRecord() - 1, Integer.valueOf(1));
            }
        }
        i = ((ArrayList) this.mExt1FileRecord.get(Integer.valueOf(recNum))).size();
        for (int index = 0; index < i; index++) {
            if (1 != ((Integer) ((ArrayList) this.mExt1Flags.get(Integer.valueOf(recNum))).get(index)).intValue()) {
                byte[] extRec = (byte[]) ((ArrayList) this.mExt1FileRecord.get(Integer.valueOf(recNum))).get(index);
                String extRecord = readExt1Record(recNum, index, 0);
                if (extRec != null && extRec.length > 0 && extRec[0] == (byte) 2 && "".equals(extRecord)) {
                    for (int i2 = 0; i2 < extRec.length; i2++) {
                        extRec[i2] = (byte) -1;
                    }
                }
            }
        }
        log("updateExt1RecordFlags done");
    }

    public boolean updateExt1File(int adnRecNum, AdnRecord oldAdnRecord, AdnRecord newAdnRecord, int tagOrEfid) {
        int pbrIndex = getPbrIndexBy(adnRecNum - 1);
        String oldNumber = oldAdnRecord.getNumber();
        String newNumber = newAdnRecord.getNumber();
        this.mSuccess = false;
        if (IccRecords.getAdnLongNumberSupport()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateExt1File adnRecNum: ");
            stringBuilder.append(adnRecNum);
            log(stringBuilder.toString());
            if (oldNumber == null || newNumber == null || oldNumber.length() > 20 || newNumber.length() > 20) {
                int efid;
                if (tagOrEfid == USIM_EFEXT1_TAG) {
                    if (this.mPbrFile == null || this.mPbrFile.mFileIds == null) {
                        Rlog.e(LOG_TAG, "mPbrFile is NULL, exiting from updateExt1File");
                        return this.mSuccess;
                    }
                    Map<Integer, Integer> fileIds = (Map) this.mPbrFile.mFileIds.get(Integer.valueOf(pbrIndex));
                    if (fileIds == null) {
                        return this.mSuccess;
                    }
                    if (!fileIds.containsKey(Integer.valueOf(tagOrEfid))) {
                        return this.mSuccess;
                    }
                    efid = ((Integer) fileIds.get(Integer.valueOf(tagOrEfid))).intValue();
                } else if (tagOrEfid != 28490) {
                    return this.mSuccess;
                } else {
                    efid = tagOrEfid;
                }
                int efid2 = efid;
                if (oldAdnRecord.getExtRecord() == HwSubscriptionManager.SUB_INIT_STATE && !TextUtils.isEmpty(newNumber)) {
                    efid = getExt1RecNumber(adnRecNum);
                    if (efid == -1) {
                        return this.mSuccess;
                    }
                    newAdnRecord.setExtRecord(efid);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Index Number in Ext is ");
                    stringBuilder2.append(efid);
                    log(stringBuilder2.toString());
                }
                synchronized (this.mLock) {
                    this.mFh.getEFLinearRecordSize(efid2, obtainMessage(13, adnRecNum, efid2, newAdnRecord));
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                        Rlog.e(LOG_TAG, "interrupted while trying to update by search");
                    }
                }
                return this.mSuccess;
            }
            this.mSuccess = true;
            return this.mSuccess;
        }
        this.mSuccess = true;
        return this.mSuccess;
    }

    private String readExt1Record(int pbrIndex, int recNum, int offset) {
        if (!hasRecordIn(this.mExt1FileRecord, pbrIndex)) {
            return null;
        }
        try {
            byte[] extRec = (byte[]) ((ArrayList) this.mExt1FileRecord.get(Integer.valueOf(pbrIndex))).get(recNum + offset);
            if (extRec == null) {
                return "";
            }
            if (extRec.length != 13) {
                return "";
            }
            if ((extRec[0] & HwSubscriptionManager.SUB_INIT_STATE) == 0) {
                return "";
            }
            int numberLength = extRec[1] & HwSubscriptionManager.SUB_INIT_STATE;
            if (numberLength > 10) {
                return "";
            }
            return PhoneNumberUtils.calledPartyBCDFragmentToString(extRec, 2, numberLength);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private byte[] buildExt1Data(int length, int adnRecIndex, String ext) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) -1;
        }
        data[0] = (byte) 0;
        if (TextUtils.isEmpty(ext) || length != 13) {
            log("[buildExtData] Empty ext1 record");
            return data;
        }
        byte[] byteExt = PhoneNumberUtils.numberToCalledPartyBCD(ext);
        if (byteExt == null) {
            return data;
        }
        data[0] = (byte) 2;
        if (byteExt.length > 11) {
            System.arraycopy(byteExt, 1, data, 2, 10);
            data[1] = (byte) 10;
        } else {
            System.arraycopy(byteExt, 1, data, 2, byteExt.length - 1);
            data[1] = (byte) (byteExt.length - 1);
        }
        return data;
    }

    private int getExt1RecNumber(int adnRecIndex) {
        int pbrIndex = getPbrIndexBy(adnRecIndex - 1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getExt1RecNumber adnRecIndex is: ");
        stringBuilder.append(adnRecIndex);
        log(stringBuilder.toString());
        if (!hasRecordIn(this.mExt1FileRecord, pbrIndex)) {
            return -1;
        }
        int extRecordNumber = ((AdnRecord) this.mPhoneBookRecords.get(adnRecIndex - 1)).getExtRecord();
        if (extRecordNumber != HwSubscriptionManager.SUB_INIT_STATE && extRecordNumber > 0 && extRecordNumber <= ((ArrayList) this.mExt1FileRecord.get(Integer.valueOf(pbrIndex))).size()) {
            return extRecordNumber;
        }
        int recordSize = ((ArrayList) this.mExt1FileRecord.get(Integer.valueOf(pbrIndex))).size();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("ext record Size: ");
        stringBuilder2.append(recordSize);
        log(stringBuilder2.toString());
        for (int i = 0; i < recordSize; i++) {
            if (TextUtils.isEmpty(readExt1Record(pbrIndex, i, 0))) {
                return i + 1;
            }
        }
        return -1;
    }

    public int getExt1Count() {
        int count = 0;
        for (int j = 0; j < this.mExt1Flags.size(); j++) {
            count += ((ArrayList) this.mExt1Flags.get(Integer.valueOf(j))).size();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getExt1Count count is: ");
        stringBuilder.append(count);
        log(stringBuilder.toString());
        return count;
    }

    public int getSpareExt1Count() {
        int pbrIndex = this.mExt1Flags.size();
        int count = 0;
        int j = 0;
        while (j < pbrIndex) {
            int extFlagsSize = ((ArrayList) this.mExt1Flags.get(Integer.valueOf(j))).size();
            int count2 = count;
            for (count = 0; count < extFlagsSize; count++) {
                if (((Integer) ((ArrayList) this.mExt1Flags.get(Integer.valueOf(j))).get(count)).intValue() == 0) {
                    count2++;
                }
            }
            j++;
            count = count2;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getSpareExt1Count count is: ");
        stringBuilder.append(count);
        log(stringBuilder.toString());
        return count;
    }
}
