package com.android.internal.telephony;

import android.content.ContentValues;
import android.os.Message;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.gsm.UsimPhoneBookManager;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class HwIccPhoneBookInterfaceManager extends IccPhoneBookInterfaceManager {
    private static final boolean DBG = false;
    private static final String LOG_TAG = "HwIccPhoneBookInterfaceManager";
    private UsimPhoneBookManager mUsimPhoneBookManager;

    public HwIccPhoneBookInterfaceManager(Phone phone) {
        super(phone);
    }

    /* JADX WARNING: Missing block: B:46:0x00ed, code skipped:
            return r1.mSuccess;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean updateAdnRecordsWithContentValuesInEfBySearchHW(int efid, ContentValues values, String pin2) {
        Throwable th;
        ContentValues contentValues = values;
        synchronized (this.mLock2) {
            int efid2;
            try {
                if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
                    throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
                } else if (contentValues == null) {
                    Rlog.e(LOG_TAG, "input values is null.");
                    return false;
                } else {
                    String oldTag = contentValues.getAsString("tag");
                    String newTag = contentValues.getAsString("newTag");
                    String oldPhoneNumber = contentValues.getAsString("number");
                    String newPhoneNumber = contentValues.getAsString("newNumber");
                    String oldEmail = contentValues.getAsString("emails");
                    String newEmail = contentValues.getAsString("newEmails");
                    String oldAnr = contentValues.getAsString("anrs");
                    String newAnr = contentValues.getAsString("newAnrs");
                    String[] oldEmailArray = TextUtils.isEmpty(oldEmail) ? null : new String[]{oldEmail};
                    String[] newEmailArray = TextUtils.isEmpty(newEmail) ? null : new String[]{newEmail};
                    String[] oldAnrArray = TextUtils.isEmpty(oldAnr) ? null : new String[]{oldAnr};
                    String[] newAnrArray = TextUtils.isEmpty(newAnr) ? null : new String[]{newAnr};
                    int efid3 = updateEfForIccTypeHw(efid);
                    try {
                        String str = LOG_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("updateAdnRecordsWithContentValuesInEfBySearch: efid=");
                        efid2 = efid3;
                        try {
                            stringBuilder.append(efid2);
                            stringBuilder.append(", pin2=xxxx");
                            Rlog.i(str, stringBuilder.toString());
                            synchronized (this.mLock) {
                                try {
                                    checkThread();
                                    this.mSuccess = false;
                                    AtomicBoolean status = new AtomicBoolean(false);
                                    try {
                                        Message response = this.mBaseHandler.obtainMessage(3, status);
                                        AdnRecord oldAdn = new AdnRecord(oldTag, oldPhoneNumber, oldEmailArray, oldAnrArray);
                                        AdnRecord newAdn = new AdnRecord(newTag, newPhoneNumber, newEmailArray, newAnrArray);
                                        if (this.mAdnCache != null) {
                                            this.mAdnCache.updateAdnBySearch(efid2, oldAdn, newAdn, pin2, response);
                                            waitForResult(status);
                                        } else {
                                            Rlog.e(LOG_TAG, "Failure while trying to update by search due to uninitialised adncache");
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                        throw th;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    String str2 = oldAnr;
                                    throw th;
                                }
                            }
                        } catch (Throwable th4) {
                            th = th4;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        efid2 = efid3;
                        throw th;
                    }
                }
            } catch (Throwable th6) {
                th = th6;
                efid2 = efid;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:30:0x0087, code skipped:
            return r1.mSuccess;
     */
    /* JADX WARNING: Missing block: B:31:0x0088, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:32:0x0089, code skipped:
            r4 = r7;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean updateUsimAdnRecordsInEfByIndexHW(int efid, String newTag, String newPhoneNumber, String[] newEmails, String[] newAnrNumbers, int sEf_id, int index, String pin2) {
        Throwable th;
        String str;
        String str2;
        synchronized (this.mLock2) {
            int i;
            int i2;
            int i3;
            try {
                if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") == 0) {
                    String str3 = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateUsimAdnRecordsInEfByIndexHW: efid=");
                    try {
                        stringBuilder.append(efid);
                        stringBuilder.append(" sEf_id=");
                        i = sEf_id;
                        try {
                            stringBuilder.append(i);
                            stringBuilder.append(" Index=");
                            i2 = index;
                        } catch (Throwable th2) {
                            th = th2;
                            str = newTag;
                            str2 = newPhoneNumber;
                            i2 = index;
                            throw th;
                        }
                        try {
                            stringBuilder.append(i2);
                            stringBuilder.append(" pin2=xxxx");
                            Rlog.i(str3, stringBuilder.toString());
                            synchronized (this.mLock) {
                                AtomicBoolean status;
                                Message response;
                                try {
                                    checkThread();
                                    this.mSuccess = false;
                                    status = new AtomicBoolean(false);
                                    response = this.mBaseHandler.obtainMessage(3, status);
                                } catch (Throwable th3) {
                                    th = th3;
                                    str = newTag;
                                    str2 = newPhoneNumber;
                                    throw th;
                                }
                                try {
                                    AdnRecord newAdn = new AdnRecord(newTag, newPhoneNumber, newEmails, newAnrNumbers);
                                    int efid2 = updateEfForIccTypeHw(efid);
                                    try {
                                        if (this.mAdnCache != null) {
                                            this.mAdnCache.updateUsimAdnByIndexHW(efid2, newAdn, i, i2, pin2, response);
                                            waitForResult(status);
                                        } else {
                                            Rlog.e(LOG_TAG, "Failure while trying to update by index due to uninitialised adncache");
                                        }
                                    } catch (Throwable th4) {
                                        th = th4;
                                        throw th;
                                    }
                                } catch (Throwable th5) {
                                    th = th5;
                                    throw th;
                                }
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            str = newTag;
                            str2 = newPhoneNumber;
                            throw th;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        str = newTag;
                        str2 = newPhoneNumber;
                        i = sEf_id;
                        i2 = index;
                        throw th;
                    }
                }
                i3 = efid;
                str = newTag;
                str2 = newPhoneNumber;
                i = sEf_id;
                i2 = index;
                throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
            } catch (Throwable th8) {
                th = th8;
                i3 = efid;
                str = newTag;
                str2 = newPhoneNumber;
                i = sEf_id;
                i2 = index;
                throw th;
            }
        }
    }

    public int getAdnCountHW() {
        if (this.mAdnCache == null) {
            Rlog.e(LOG_TAG, "mAdnCache is NULL when getAdnCountHW.");
            return 0;
        } else if (this.mPhone == null || (this.mPhone.getCurrentUiccAppType() != AppType.APPTYPE_USIM && this.mPhone.getCurrentUiccAppType() != AppType.APPTYPE_CSIM && this.mPhone.getCurrentUiccAppType() != AppType.APPTYPE_ISIM)) {
            return this.mAdnCache.getAdnCountHW();
        } else {
            return this.mAdnCache.getUsimAdnCountHW();
        }
    }

    public int getAnrCountHW() {
        if (this.mAdnCache != null) {
            return this.mAdnCache.getAnrCountHW();
        }
        Rlog.e(LOG_TAG, "mAdnCache is NULL when getAnrCountHW.");
        return 0;
    }

    public int getEmailCountHW() {
        if (this.mAdnCache != null) {
            return this.mAdnCache.getEmailCountHW();
        }
        Rlog.e(LOG_TAG, "mAdnCache is NULL when getEmailCountHW.");
        return 0;
    }

    public int getSpareAnrCountHW() {
        if (this.mAdnCache != null) {
            return this.mAdnCache.getSpareAnrCountHW();
        }
        Rlog.e(LOG_TAG, "mAdnCache is NULL when getSpareAnrCountHW.");
        return 0;
    }

    public int getSpareEmailCountHW() {
        if (this.mAdnCache != null) {
            return this.mAdnCache.getSpareEmailCountHW();
        }
        Rlog.e(LOG_TAG, "mAdnCache is NULL when getSpareEmailCountHW.");
        return 0;
    }

    public int[] getRecordsSizeHW() {
        if (!IccRecords.getEmailAnrSupport()) {
            Rlog.e(LOG_TAG, "getRecordsSize return null as prop not open.");
            return null;
        } else if (this.mPhone == null || this.mPhone.mIccRecords.get() == null || ((IccRecords) this.mPhone.mIccRecords.get()).isGetPBRDone()) {
            synchronized (this.mLock2) {
                if (getAdnCountHW() == 0) {
                    Rlog.e(LOG_TAG, "getRecordsSize: adn is not ever read!");
                    getAdnRecordsInEf(28474);
                }
                Rlog.d(LOG_TAG, "getRecordsSize: adn all loaded!");
                synchronized (this.mLock) {
                    checkThread();
                    if (this.mAdnCache != null) {
                        int[] recordsSizeHW = this.mAdnCache.getRecordsSizeHW();
                        return recordsSizeHW;
                    }
                    Rlog.e(LOG_TAG, "mAdnCache is NULL when getRecordsSizeHW.");
                    return null;
                }
            }
        } else {
            Rlog.e(LOG_TAG, "getRecordsSize(): is not get PBR done, please wait!");
            return null;
        }
    }

    public int updateEfFor3gCardType(int efid) {
        if (this.mPhone == null || this.mPhone.mIccRecords.get() == null) {
            Rlog.d(LOG_TAG, "Translate EF_ADN to EF_PBR");
            return 20272;
        } else if (((IccRecords) this.mPhone.mIccRecords.get()).has3Gphonebook()) {
            Rlog.d(LOG_TAG, "Translate EF_ADN to EF_PBR");
            return 20272;
        } else {
            Rlog.d(LOG_TAG, "updateEfForIccType use EF_ADN");
            return efid;
        }
    }

    public int[] getAdnRecordsSize(int efid) {
        synchronized (this.mLock2) {
            efid = updateEfForIccTypeHw(efid);
            int[] iArr;
            if (20272 == efid) {
                this.mRecordSize = new int[3];
                IccFileHandler fh = this.mPhone.getIccFileHandler();
                if (fh != null) {
                    if (this.mPhone.mIccRecords != null) {
                        IccRecords r = (IccRecords) this.mPhone.mIccRecords.get();
                        if (r != null) {
                            this.mAdnCache = r.getAdnCache();
                        }
                    }
                    if (this.mAdnCache != null) {
                        this.mUsimPhoneBookManager = this.mAdnCache.getUsimPhoneBookManager();
                        if (this.mUsimPhoneBookManager != null) {
                            this.mUsimPhoneBookManager.setIccFileHandler(fh);
                            this.mRecordSize = this.mUsimPhoneBookManager.getAdnRecordsSizeFromEF();
                        }
                        if (this.mRecordSize == null) {
                            loge("null == mRecordSize");
                            iArr = new int[3];
                            return iArr;
                        }
                    }
                    loge("Failure while trying to load from SIM due to uninitialised adncache");
                    return null;
                }
                iArr = Arrays.copyOf(this.mRecordSize, 3);
                return iArr;
            }
            iArr = super.getAdnRecordsSize(efid);
            return iArr;
        }
    }

    public int getSpareExt1CountHW() {
        if (this.mAdnCache != null) {
            return this.mAdnCache.getSpareExt1CountHW();
        }
        Rlog.e(LOG_TAG, "mAdnCache is NULL when getSpareExt1CountHW.");
        return -1;
    }
}
