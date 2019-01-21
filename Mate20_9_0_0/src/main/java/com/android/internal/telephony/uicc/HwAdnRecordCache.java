package com.android.internal.telephony.uicc;

import android.os.Message;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.gsm.UsimPhoneBookManager;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.huawei.utils.reflect.EasyInvokeFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

public class HwAdnRecordCache extends AdnRecordCache {
    private static String TAG = "HwAdnRecordCache";
    private static final int USIM_ADN_MAX_LENGTH_WITHOUT_EXT = 20;
    private static final int USIM_EFANR_TAG = 196;
    private static final int USIM_EFEMAIL_TAG = 202;
    private static final int USIM_EFEXT1_TAG = 194;
    private static AdnRecordCacheUtils adnRecordCacheUtils = ((AdnRecordCacheUtils) EasyInvokeFactory.getInvokeUtils(AdnRecordCacheUtils.class));
    public static final AtomicReference<Integer> s_efid = new AtomicReference();
    public static final AtomicReference<Integer> s_index = new AtomicReference();
    private static UiccCardApplicationUtils uiccCardApplicationUtils = new UiccCardApplicationUtils();
    private int mAdncountofIcc = 0;

    public HwAdnRecordCache(IccFileHandler fh) {
        super(fh);
    }

    protected void updateAdnRecordId(AdnRecord adn, int efid, int index) {
        if (!(adn == null || efid == 20272)) {
            adn.mEfid = efid;
            adn.mRecordNumber = index;
        }
        if (efid != 20272) {
            s_efid.set(Integer.valueOf(efid));
            s_index.set(Integer.valueOf(index));
        } else if (adn != null) {
            s_efid.set(Integer.valueOf(adn.mEfid));
            s_index.set(Integer.valueOf(adn.mRecordNumber));
        }
    }

    public int getUsimExtensionEfForAdnEf(int AdnEfid) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getUsimExtensionEfForAdnEf AdnEfid = ");
        stringBuilder.append(AdnEfid);
        logd(stringBuilder.toString());
        if (uiccCardApplicationUtils.getUiccCard(adnRecordCacheUtils.getFh(this).mParentApp).isApplicationOnIcc(AppType.APPTYPE_USIM)) {
            logd("getUsimExtensionEfForAdnEf sim application is on APPTYPE_USIM");
            int pbrSize = adnRecordCacheUtils.getUsimPhoneBookManager(this).getPbrFileSize();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getUsimExtensionEfForAdnEf pbrSize = ");
            stringBuilder2.append(pbrSize);
            logd(stringBuilder2.toString());
            if (pbrSize <= 0) {
                return -1;
            }
            for (int loop = 0; loop < pbrSize; loop++) {
                int efid = adnRecordCacheUtils.getUsimPhoneBookManager(this).getEFidInPBR(loop, 192);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("getUsimExtensionEfForAdnEf loop = ");
                stringBuilder3.append(loop);
                stringBuilder3.append(" ; efid = ");
                stringBuilder3.append(efid);
                logd(stringBuilder3.toString());
                if (AdnEfid == efid) {
                    int extensionEF = adnRecordCacheUtils.getUsimPhoneBookManager(this).getEFidInPBR(loop, USIM_EFEXT1_TAG);
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("getUsimExtensionEfForAdnEf extensionEF = ");
                    stringBuilder4.append(extensionEF);
                    logd(stringBuilder4.toString());
                    if (extensionEF < 0) {
                        return -1;
                    }
                    return extensionEF;
                }
            }
        }
        logd("getUsimExtensionEfForAdnEf no match pbr return -1");
        return -1;
    }

    public int extensionEfForEf(int efid) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("extensionEfForEf efid = ");
        stringBuilder.append(efid);
        logd(stringBuilder.toString());
        if (efid == 20272) {
            return 0;
        }
        if (efid == 28480) {
            return 28490;
        }
        if (efid == 28489) {
            return 28492;
        }
        if (efid == 28615) {
            return 28616;
        }
        switch (efid) {
            case 28474:
                return 28490;
            case 28475:
                return 28491;
            default:
                return getUsimExtensionEfForAdnEf(efid);
        }
    }

    public void updateAdnBySearch(int efid, AdnRecord oldAdn, AdnRecord newAdn, String pin2, Message response) {
        int i = efid;
        AdnRecord adnRecord = oldAdn;
        AdnRecord adnRecord2 = newAdn;
        Message message = response;
        if (IccRecords.getEmailAnrSupport()) {
            int extensionEF = extensionEfForEf(efid);
            if (extensionEF < 0) {
                AdnRecordCacheUtils adnRecordCacheUtils = adnRecordCacheUtils;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("EF is not known ADN-like EF:");
                stringBuilder.append(i);
                adnRecordCacheUtils.sendErrorResponse(this, message, stringBuilder.toString());
                return;
            }
            ArrayList<AdnRecord> oldAdnList;
            ArrayList<AdnRecord> oldAdnList2 = null;
            int i2 = 20272;
            if (i == 20272) {
                try {
                    oldAdnList = getUsimPhoneBookManager().loadEfFilesFromUsim();
                } catch (NullPointerException e) {
                    oldAdnList = null;
                }
            } else {
                oldAdnList = getRecordsIfLoaded(efid);
            }
            AdnRecordCacheUtils adnRecordCacheUtils2;
            StringBuilder stringBuilder2;
            if (oldAdnList == null) {
                adnRecordCacheUtils2 = adnRecordCacheUtils;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Adn list not exist for EF:");
                stringBuilder2.append(i);
                adnRecordCacheUtils2.sendErrorResponse(this, message, stringBuilder2.toString());
                return;
            }
            int index;
            int index2 = -1;
            int prePbrIndex = -2;
            int anrNum = 0;
            int emailNum = 0;
            Iterator<AdnRecord> it = oldAdnList.iterator();
            int count = 1;
            while (it.hasNext()) {
                int index3;
                int prePbrIndex2;
                Iterator<AdnRecord> it2;
                AdnRecord nextAdnRecord = (AdnRecord) it.next();
                boolean isEmailOrAnrIsFull = false;
                if (i == i2) {
                    index3 = index2;
                    index2 = getUsimPhoneBookManager().getPbrIndexBy(count - 1);
                    if (index2 != prePbrIndex) {
                        anrNum = getUsimPhoneBookManager().getEmptyAnrNum_Pbrindex(index2);
                        emailNum = getUsimPhoneBookManager().getEmptyEmailNum_Pbrindex(index2);
                        prePbrIndex = index2;
                        String str = TAG;
                        prePbrIndex2 = prePbrIndex;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        it2 = it;
                        stringBuilder3.append("updateAdnBySearch, pbrIndex: ");
                        stringBuilder3.append(index2);
                        stringBuilder3.append(" anrNum:");
                        stringBuilder3.append(anrNum);
                        stringBuilder3.append(" emailNum:");
                        stringBuilder3.append(emailNum);
                        Rlog.d(str, stringBuilder3.toString());
                    } else {
                        it2 = it;
                        prePbrIndex2 = prePbrIndex;
                    }
                    if ((anrNum == 0 && oldAdn.getAdditionalNumbers() == null && newAdn.getAdditionalNumbers() != null) || (emailNum == 0 && oldAdn.getEmails() == null && newAdn.getEmails() != null)) {
                        isEmailOrAnrIsFull = true;
                    }
                    prePbrIndex = prePbrIndex2;
                } else {
                    index3 = index2;
                    it2 = it;
                }
                if (!isEmailOrAnrIsFull && adnRecord.isEqual(nextAdnRecord)) {
                    index = count;
                    index3 = prePbrIndex;
                    prePbrIndex2 = anrNum;
                    it2 = emailNum;
                    break;
                }
                count++;
                index2 = index3;
                it = it2;
                i2 = 20272;
            }
            index = index2;
            String str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateAdnBySearch  index :");
            stringBuilder2.append(index);
            Rlog.d(str2, stringBuilder2.toString());
            if (index == -1) {
                adnRecordCacheUtils2 = adnRecordCacheUtils;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Adn record don't exist for ");
                stringBuilder2.append(adnRecord);
                adnRecordCacheUtils2.sendErrorResponse(this, message, stringBuilder2.toString());
                return;
            }
            AdnRecord foundAdn;
            if (i == 20272) {
                foundAdn = (AdnRecord) oldAdnList.get(index - 1);
                adnRecord2.mEfid = foundAdn.mEfid;
                adnRecord2.mExtRecord = foundAdn.mExtRecord;
                adnRecord2.mRecordNumber = foundAdn.mRecordNumber;
                adnRecord.setAdditionalNumbers(foundAdn.getAdditionalNumbers());
                adnRecord.setEmails(foundAdn.getEmails());
                adnRecord2.updateAnrEmailArray(adnRecord, getUsimPhoneBookManager().getEmailFilesCountEachAdn(), getUsimPhoneBookManager().getAnrFilesCountEachAdn());
            } else if (i == 28474) {
                foundAdn = (AdnRecord) oldAdnList.get(index - 1);
                adnRecord2.mEfid = foundAdn.mEfid;
                adnRecord2.mExtRecord = foundAdn.mExtRecord;
                adnRecord2.mRecordNumber = foundAdn.mRecordNumber;
            }
            if (((Message) this.mUserWriteResponse.get(i)) != null) {
                adnRecordCacheUtils2 = adnRecordCacheUtils;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Have pending update for EF:");
                stringBuilder2.append(i);
                adnRecordCacheUtils2.sendErrorResponse(this, message, stringBuilder2.toString());
                return;
            } else if (i == 20272) {
                updateEmailAndAnr(i, adnRecord, adnRecord2, index, pin2, message);
            } else if (getUsimPhoneBookManager().updateExt1File(index, adnRecord, adnRecord2, extensionEF)) {
                this.mUserWriteResponse.put(i, message);
                new AdnRecordLoader(adnRecordCacheUtils.getFh(this)).updateEF(adnRecord2, i, extensionEF, index, pin2, obtainMessage(2, i, index, adnRecord2));
            } else {
                adnRecordCacheUtils.sendErrorResponse(this, message, "update ext1 failed");
                return;
            }
        }
        super.updateAdnBySearch(efid, oldAdn, newAdn, pin2, response);
    }

    private void updateEmailAndAnr(int efid, AdnRecord oldAdn, AdnRecord newAdn, int index, String pin2, Message response) {
        int extensionEF = extensionEfForEf(newAdn.mEfid);
        if (!updateUsimRecord(oldAdn, newAdn, index, 202)) {
            adnRecordCacheUtils.sendErrorResponse(this, response, "update email failed");
        } else if (!updateUsimRecord(oldAdn, newAdn, index, USIM_EFANR_TAG)) {
            adnRecordCacheUtils.sendErrorResponse(this, response, "update anr failed");
        } else if (getUsimPhoneBookManager().updateExt1File(index, oldAdn, newAdn, USIM_EFEXT1_TAG)) {
            this.mUserWriteResponse.put(efid, response);
            new AdnRecordLoader(adnRecordCacheUtils.getFh(this)).updateEF(newAdn, newAdn.mEfid, extensionEF, newAdn.mRecordNumber, pin2, obtainMessage(2, efid, index, newAdn));
        } else {
            adnRecordCacheUtils.sendErrorResponse(this, response, "update ext1 failed");
        }
    }

    private boolean updateAnrEmailFile(String oldRecord, String newRecord, int index, int tag, int efidIndex) {
        if (tag == USIM_EFANR_TAG) {
            return getUsimPhoneBookManager().updateAnrFile(index, oldRecord, newRecord, efidIndex);
        }
        if (tag != 202) {
            return false;
        }
        try {
            return getUsimPhoneBookManager().updateEmailFile(index, oldRecord, newRecord, efidIndex);
        } catch (RuntimeException e) {
            Rlog.e(TAG, "update usim record failed", e);
            return false;
        }
    }

    private boolean updateUsimRecord(AdnRecord oldAdn, AdnRecord newAdn, int index, int tag) {
        String[] oldRecords;
        String[] newRecords;
        int i = tag;
        int i2 = 0;
        if (i == USIM_EFANR_TAG) {
            oldRecords = oldAdn.getAdditionalNumbers();
            newRecords = newAdn.getAdditionalNumbers();
        } else if (i != 202) {
            return false;
        } else {
            oldRecords = oldAdn.getEmails();
            newRecords = newAdn.getEmails();
        }
        String[] oldRecords2 = oldRecords;
        String[] newRecords2 = newRecords;
        boolean z = oldRecords2 == null && newRecords2 == null;
        boolean isAllEmpty = z;
        z = oldRecords2 == null && newRecords2 != null;
        boolean isOldEmpty = z;
        z = oldRecords2 != null && newRecords2 == null;
        boolean isNewEmpty = z;
        if (isAllEmpty) {
            Rlog.e(TAG, "Both old and new EMAIL/ANR are null");
            return true;
        }
        boolean success;
        if (isOldEmpty) {
            success = true;
            while (i2 < newRecords2.length) {
                if (!TextUtils.isEmpty(newRecords2[i2])) {
                    success = updateAnrEmailFile(null, newRecords2[i2], index, i, i2) & success;
                }
                i2++;
            }
        } else if (isNewEmpty) {
            success = true;
            while (i2 < oldRecords2.length) {
                if (!TextUtils.isEmpty(oldRecords2[i2])) {
                    success = updateAnrEmailFile(oldRecords2[i2], null, index, i, i2) & success;
                }
                i2++;
            }
        } else {
            int maxLen = oldRecords2.length > newRecords2.length ? oldRecords2.length : newRecords2.length;
            boolean success2 = true;
            int i3 = 0;
            while (true) {
                int i4 = i3;
                if (i4 >= maxLen) {
                    break;
                }
                int i5;
                int maxLen2;
                String str = null;
                String oldRecord = i4 >= oldRecords2.length ? null : oldRecords2[i4];
                if (i4 < newRecords2.length) {
                    str = newRecords2[i4];
                }
                String newRecord = str;
                z = (TextUtils.isEmpty(oldRecord) && TextUtils.isEmpty(newRecord)) || !(oldRecord == null || newRecord == null || !oldRecord.equals(newRecord));
                if (z) {
                    i5 = i4;
                    maxLen2 = maxLen;
                } else {
                    i5 = i4;
                    maxLen2 = maxLen;
                    success2 &= updateAnrEmailFile(oldRecord, newRecord, index, i, i5);
                }
                i3 = i5 + 1;
                maxLen = maxLen2;
            }
            success = success2;
        }
        return success;
    }

    public void updateUsimAdnByIndexHW(int efid, AdnRecord newAdn, int sEf_id, int recordIndex, String pin2, Message response) {
        int i = efid;
        AdnRecord adnRecord = newAdn;
        int i2 = sEf_id;
        Message message = response;
        int extensionEF = extensionEfForEf(efid);
        if (extensionEF < 0) {
            AdnRecordCacheUtils adnRecordCacheUtils = adnRecordCacheUtils;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EF is not known ADN-like EF:");
            stringBuilder.append(i);
            adnRecordCacheUtils.sendErrorResponse(this, message, stringBuilder.toString());
            return;
        }
        ArrayList<AdnRecord> oldAdnList;
        ArrayList<AdnRecord> oldAdnList2 = null;
        if (i == 20272) {
            try {
                oldAdnList = getUsimPhoneBookManager().loadEfFilesFromUsim();
            } catch (NullPointerException e) {
                oldAdnList = null;
            }
        } else {
            oldAdnList = getRecordsIfLoaded(efid);
        }
        AdnRecordCacheUtils adnRecordCacheUtils2;
        StringBuilder stringBuilder2;
        if (oldAdnList == null) {
            adnRecordCacheUtils2 = adnRecordCacheUtils;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Adn list not exist for EF:");
            stringBuilder2.append(i);
            adnRecordCacheUtils2.sendErrorResponse(this, message, stringBuilder2.toString());
            return;
        }
        int index = recordIndex;
        if (i == 20272) {
            int pbrIndex = getUsimPhoneBookManager().getPbrIndexByEfid(i2);
            index += getUsimPhoneBookManager().getInitIndexByPbr(pbrIndex);
            String str = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("sEf_id ");
            stringBuilder3.append(i2);
            stringBuilder3.append(" index ");
            stringBuilder3.append(index);
            stringBuilder3.append(" pbrIndex ");
            stringBuilder3.append(pbrIndex);
            Rlog.d(str, stringBuilder3.toString());
            AdnRecord foundAdn = (AdnRecord) oldAdnList.get(index - 1);
            adnRecord.mEfid = foundAdn.mEfid;
            adnRecord.mExtRecord = foundAdn.mExtRecord;
            adnRecord.mRecordNumber = foundAdn.mRecordNumber;
        } else if (i == 28474) {
            AdnRecord foundAdn2 = (AdnRecord) oldAdnList.get(index - 1);
            adnRecord.mEfid = foundAdn2.mEfid;
            adnRecord.mExtRecord = foundAdn2.mExtRecord;
            adnRecord.mRecordNumber = foundAdn2.mRecordNumber;
        }
        int index2 = index;
        if (((Message) this.mUserWriteResponse.get(i)) != null) {
            adnRecordCacheUtils2 = adnRecordCacheUtils;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Have pending update for EF:");
            stringBuilder2.append(i);
            adnRecordCacheUtils2.sendErrorResponse(this, message, stringBuilder2.toString());
            return;
        }
        if (i == 20272) {
            updateEmailAndAnr(i, (AdnRecord) oldAdnList.get(index2 - 1), adnRecord, index2, pin2, message);
        } else if (getUsimPhoneBookManager().updateExt1File(index2, (AdnRecord) oldAdnList.get(index2 - 1), adnRecord, extensionEF)) {
            this.mUserWriteResponse.put(i, message);
            new AdnRecordLoader(adnRecordCacheUtils.getFh(this)).updateEF(adnRecord, i, extensionEF, index2, pin2, obtainMessage(2, i, index2, adnRecord));
        } else {
            adnRecordCacheUtils.sendErrorResponse(this, message, "update ext1 failed");
        }
    }

    public int getAnrCountHW() {
        return getUsimPhoneBookManager().getAnrCount();
    }

    public int getEmailCountHW() {
        return getUsimPhoneBookManager().getEmailCount();
    }

    public int getSpareAnrCountHW() {
        return getUsimPhoneBookManager().getSpareAnrCount();
    }

    public int getSpareEmailCountHW() {
        return getUsimPhoneBookManager().getSpareEmailCount();
    }

    public int getAdnCountHW() {
        return this.mAdncountofIcc;
    }

    public void setAdnCountHW(int count) {
        this.mAdncountofIcc = count;
    }

    public int getUsimAdnCountHW() {
        return getUsimPhoneBookManager().getUsimAdnCount();
    }

    public UsimPhoneBookManager getUsimPhoneBookManager() {
        return adnRecordCacheUtils.getUsimPhoneBookManager(this);
    }

    public int getRecordsSizeByIdInAdnlist(int efid) {
        ArrayList<AdnRecord> adnList = getRecordsIfLoaded(efid);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getRecordsSizeByIdInAdnlist efid ");
        stringBuilder.append(efid);
        Rlog.i(str, stringBuilder.toString());
        if (adnList == null) {
            return 0;
        }
        return adnList.size();
    }

    public int getRecordsFreeSizeByIdInAdnlist(int efid) {
        int RecordsFreeSize = 0;
        ArrayList<AdnRecord> adnList = getRecordsIfLoaded(efid);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getRecordsFreeSizeByIdInAdnlist efid ");
        stringBuilder.append(efid);
        Rlog.i(str, stringBuilder.toString());
        if (adnList == null) {
            return 0;
        }
        int adnListSize = adnList.size();
        for (int i = 0; i < adnListSize; i++) {
            if (((AdnRecord) adnList.get(i)).isEmpty()) {
                RecordsFreeSize++;
            }
        }
        return RecordsFreeSize;
    }

    public int[] getRecordsSizeHW() {
        int[] recordSize = new int[9];
        Rlog.i(TAG, "getRecordsSize(): enter.");
        int i = 0;
        for (int i2 = 0; i2 < recordSize.length; i2++) {
            recordSize[i2] = -1;
        }
        recordSize[0] = 0;
        boolean isCsim3Gphonebook = false;
        if ((adnRecordCacheUtils.getFh(this) instanceof CsimFileHandler) && adnRecordCacheUtils.getFh(this).getIccRecords() != null) {
            isCsim3Gphonebook = adnRecordCacheUtils.getFh(this).getIccRecords().has3Gphonebook();
        }
        boolean isUsim3Gphonebook = false;
        if ((adnRecordCacheUtils.getFh(this) instanceof UsimFileHandler) && adnRecordCacheUtils.getFh(this).getIccRecords() != null) {
            isUsim3Gphonebook = adnRecordCacheUtils.getFh(this).getIccRecords().has3Gphonebook();
        }
        if (isUsim3Gphonebook || isCsim3Gphonebook || (adnRecordCacheUtils.getFh(this) instanceof IsimFileHandler)) {
            Rlog.i(TAG, "getRecordsSize(): usim card branch.");
            if (getUsimPhoneBookManager() == null) {
                return recordSize;
            }
            if (getUsimAdnCountHW() > 0) {
                recordSize[2] = getUsimAdnCountHW();
                recordSize[1] = getUsimPhoneBookManager().getAdnRecordsFreeSize();
            }
            if (getEmailCountHW() > 0) {
                recordSize[5] = getEmailCountHW();
                if (!(recordSize[5] == -1 || recordSize[5] == 0)) {
                    recordSize[4] = getUsimPhoneBookManager().getSpareEmailCount();
                    recordSize[3] = 1;
                }
            }
            if (getAnrCountHW() > 0) {
                recordSize[8] = getAnrCountHW();
                if (!(recordSize[8] == -1 || recordSize[8] == 0)) {
                    recordSize[7] = getUsimPhoneBookManager().getSpareAnrCount();
                    recordSize[6] = 2;
                }
            }
        } else {
            Rlog.i(TAG, "getRecordsSize(): sim card branch.");
            recordSize[1] = getRecordsFreeSizeByIdInAdnlist(28474);
            recordSize[2] = getRecordsSizeByIdInAdnlist(28474);
        }
        while (i < recordSize.length) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getRecordsSize(): recordSize[");
            stringBuilder.append(i);
            stringBuilder.append("] = ");
            stringBuilder.append(recordSize[i]);
            Rlog.i(str, stringBuilder.toString());
            i++;
        }
        return recordSize;
    }

    public void updateUsimPhoneBookRecord(AdnRecord adn, int efid, int index) {
        if (20272 == efid) {
            ArrayList<AdnRecord> tempAdnList = getUsimPhoneBookManager().loadEfFilesFromUsim();
            if (tempAdnList != null) {
                tempAdnList.set(index - 1, adn);
            } else {
                Rlog.e(TAG, "loadEfFilesFromUsim result null.");
            }
        }
    }

    public int getExt1CountHW() {
        return getUsimPhoneBookManager().getExt1Count();
    }

    public ArrayList<AdnRecord> getAdnFilesForSim() {
        return (ArrayList) this.mAdnLikeFiles.get(28474);
    }

    public int getSpareExt1CountHW() {
        if (getExt1CountHW() > 0) {
            return getUsimPhoneBookManager().getSpareExt1Count();
        }
        return -1;
    }

    public void reset() {
        super.reset();
        this.mAdncountofIcc = 0;
    }

    private void logd(String msg) {
        Rlog.d(TAG, msg);
    }
}
