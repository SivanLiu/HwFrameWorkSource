package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import android.util.SparseArray;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.gsm.UsimPhoneBookManager;
import java.util.ArrayList;
import java.util.Iterator;

public class AdnRecordCache extends AbstractAdnRecordCache implements IccConstants {
    static final int EVENT_LOAD_ALL_ADN_LIKE_DONE = 1;
    static final int EVENT_UPDATE_ADN_DONE = 2;
    SparseArray<ArrayList<AdnRecord>> mAdnLikeFiles = new SparseArray();
    SparseArray<ArrayList<Message>> mAdnLikeWaiters = new SparseArray();
    private IccFileHandler mFh;
    SparseArray<Message> mUserWriteResponse = new SparseArray();
    private UsimPhoneBookManager mUsimPhoneBookManager;

    AdnRecordCache(IccFileHandler fh) {
        this.mFh = fh;
        if (AbstractIccRecords.getEmailAnrSupport()) {
            this.mUsimPhoneBookManager = HwTelephonyFactory.getHwUiccManager().createHwUsimPhoneBookManagerEmailAnr(this.mFh, this);
        } else {
            this.mUsimPhoneBookManager = HwTelephonyFactory.getHwUiccManager().createHwUsimPhoneBookManager(this.mFh, this);
        }
    }

    public void reset() {
        this.mAdnLikeFiles.clear();
        this.mUsimPhoneBookManager.reset();
        clearWaiters();
        clearUserWriters();
    }

    private void clearWaiters() {
        int size = this.mAdnLikeWaiters.size();
        for (int i = 0; i < size; i++) {
            notifyWaiters((ArrayList) this.mAdnLikeWaiters.valueAt(i), new AsyncResult(null, null, new RuntimeException("AdnCache reset")));
        }
        this.mAdnLikeWaiters.clear();
    }

    private void clearUserWriters() {
        int size = this.mUserWriteResponse.size();
        for (int i = 0; i < size; i++) {
            sendErrorResponse((Message) this.mUserWriteResponse.valueAt(i), "AdnCace reset");
        }
        this.mUserWriteResponse.clear();
    }

    public ArrayList<AdnRecord> getRecordsIfLoaded(int efid) {
        return (ArrayList) this.mAdnLikeFiles.get(efid);
    }

    public int extensionEfForEf(int efid) {
        if (efid == IccConstants.EF_PBR) {
            return 0;
        }
        if (efid == IccConstants.EF_MSISDN) {
            return IccConstants.EF_EXT1;
        }
        if (efid == IccConstants.EF_SDN) {
            return IccConstants.EF_EXT3;
        }
        if (efid == IccConstants.EF_MBDN) {
            return IccConstants.EF_EXT6;
        }
        switch (efid) {
            case 28474:
                return IccConstants.EF_EXT1;
            case IccConstants.EF_FDN /*28475*/:
                return IccConstants.EF_EXT2;
            default:
                return -1;
        }
    }

    private void sendErrorResponse(Message response, String errString) {
        if (response != null) {
            AsyncResult.forMessage(response).exception = new RuntimeException(errString);
            response.sendToTarget();
        }
    }

    public void updateAdnByIndex(int efid, AdnRecord adn, int recordIndex, String pin2, Message response) {
        int extensionEF = extensionEfForEf(efid);
        StringBuilder stringBuilder;
        if (extensionEF < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("EF is not known ADN-like EF:0x");
            stringBuilder.append(Integer.toHexString(efid).toUpperCase());
            sendErrorResponse(response, stringBuilder.toString());
        } else if (((Message) this.mUserWriteResponse.get(efid)) != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Have pending update for EF:0x");
            stringBuilder.append(Integer.toHexString(efid).toUpperCase());
            sendErrorResponse(response, stringBuilder.toString());
        } else {
            this.mUserWriteResponse.put(efid, response);
            new AdnRecordLoader(this.mFh).updateEF(adn, efid, extensionEF, recordIndex, pin2, obtainMessage(2, efid, recordIndex, adn));
        }
    }

    public void updateAdnBySearch(int efid, AdnRecord oldAdn, AdnRecord newAdn, String pin2, Message response) {
        int efid2 = efid;
        AdnRecord adnRecord = oldAdn;
        AdnRecord adnRecord2 = newAdn;
        Message message = response;
        int extensionEF = extensionEfForEf(efid);
        StringBuilder stringBuilder;
        if (extensionEF < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("EF is not known ADN-like EF:0x");
            stringBuilder.append(Integer.toHexString(efid).toUpperCase());
            sendErrorResponse(message, stringBuilder.toString());
            return;
        }
        ArrayList<AdnRecord> oldAdnList;
        if (efid2 == IccConstants.EF_PBR) {
            oldAdnList = this.mUsimPhoneBookManager.loadEfFilesFromUsim();
        } else {
            oldAdnList = getRecordsIfLoaded(efid);
        }
        ArrayList<AdnRecord> oldAdnList2 = oldAdnList;
        if (oldAdnList2 == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Adn list not exist for EF:0x");
            stringBuilder.append(Integer.toHexString(efid).toUpperCase());
            sendErrorResponse(message, stringBuilder.toString());
            return;
        }
        int index = -1;
        Iterator<AdnRecord> it = oldAdnList2.iterator();
        int count = 1;
        while (it.hasNext()) {
            if (adnRecord.isEqual((AdnRecord) it.next())) {
                index = count;
                break;
            }
            count++;
        }
        if (index == -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Adn record don't exist for ");
            stringBuilder.append(adnRecord);
            sendErrorResponse(message, stringBuilder.toString());
            return;
        }
        if (efid2 == IccConstants.EF_PBR) {
            AdnRecord foundAdn = (AdnRecord) oldAdnList2.get(index - 1);
            efid2 = foundAdn.mEfid;
            extensionEF = foundAdn.mExtRecord;
            index = foundAdn.mRecordNumber;
            adnRecord2.mEfid = efid2;
            adnRecord2.mExtRecord = extensionEF;
            adnRecord2.mRecordNumber = index;
        }
        int extensionEF2 = extensionEF;
        int index2 = index;
        if (((Message) this.mUserWriteResponse.get(efid2)) != null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Have pending update for EF:0x");
            stringBuilder2.append(Integer.toHexString(efid2).toUpperCase());
            sendErrorResponse(message, stringBuilder2.toString());
            return;
        }
        this.mUserWriteResponse.put(efid2, message);
        new AdnRecordLoader(this.mFh).updateEF(adnRecord2, efid2, extensionEF2, index2, pin2, obtainMessage(2, efid2, index2, adnRecord2));
    }

    public void requestLoadAllAdnLike(int efid, int extensionEf, Message response) {
        ArrayList<AdnRecord> result;
        if (efid == IccConstants.EF_PBR) {
            result = this.mUsimPhoneBookManager.loadEfFilesFromUsim();
        } else {
            result = getRecordsIfLoaded(efid);
        }
        if (result != null) {
            if (response != null) {
                AsyncResult.forMessage(response).result = result;
                response.sendToTarget();
            }
            return;
        }
        ArrayList<Message> waiters = (ArrayList) this.mAdnLikeWaiters.get(efid);
        if (waiters != null) {
            waiters.add(response);
            return;
        }
        waiters = new ArrayList();
        waiters.add(response);
        this.mAdnLikeWaiters.put(efid, waiters);
        if (extensionEf < 0) {
            if (response != null) {
                AsyncResult forMessage = AsyncResult.forMessage(response);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("EF is not known ADN-like EF:0x");
                stringBuilder.append(Integer.toHexString(efid).toUpperCase());
                forMessage.exception = new RuntimeException(stringBuilder.toString());
                response.sendToTarget();
            }
            return;
        }
        new AdnRecordLoader(this.mFh).loadAllFromEF(efid, extensionEf, obtainMessage(1, efid, 0));
    }

    private void notifyWaiters(ArrayList<Message> waiters, AsyncResult ar) {
        if (waiters != null) {
            int s = waiters.size();
            for (int i = 0; i < s; i++) {
                Message waiter = (Message) waiters.get(i);
                AsyncResult.forMessage(waiter, ar.result, ar.exception);
                waiter.sendToTarget();
            }
        }
    }

    public void handleMessage(Message msg) {
        AsyncResult ar;
        int efid;
        switch (msg.what) {
            case 1:
                ar = (AsyncResult) msg.obj;
                efid = msg.arg1;
                ArrayList<Message> waiters = (ArrayList) this.mAdnLikeWaiters.get(efid);
                this.mAdnLikeWaiters.delete(efid);
                if (ar.exception == null) {
                    this.mAdnLikeFiles.put(efid, (ArrayList) ar.result);
                }
                notifyWaiters(waiters, ar);
                if (AbstractIccRecords.getEmailAnrSupport() && this.mAdnLikeFiles.get(28474) != null) {
                    setAdnCountHW(((ArrayList) this.mAdnLikeFiles.get(28474)).size());
                }
                if (AbstractIccRecords.getAdnLongNumberSupport() && this.mAdnLikeFiles.get(28474) != null && getUsimPhoneBookManager() != null) {
                    getUsimPhoneBookManager().readExt1FileForSim(efid);
                    return;
                }
                return;
            case 2:
                ar = msg.obj;
                efid = msg.arg1;
                int index = msg.arg2;
                AdnRecord adn = ar.userObj;
                updateAdnRecordId(adn, efid, index);
                if (ar.exception == null) {
                    if (this.mAdnLikeFiles.get(efid) != null) {
                        StringBuilder stringBuilder;
                        if (index < 1 || index > ((ArrayList) this.mAdnLikeFiles.get(efid)).size()) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("IndexOutOfBounds index = ");
                            stringBuilder.append(index);
                            stringBuilder.append(",size = ");
                            stringBuilder.append(((ArrayList) this.mAdnLikeFiles.get(efid)).size());
                            Rlog.d("ADN RECORD", stringBuilder.toString());
                        } else {
                            ((ArrayList) this.mAdnLikeFiles.get(efid)).set(index - 1, adn);
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("index is ");
                        stringBuilder.append(index);
                        Rlog.d("ADN RECORD", stringBuilder.toString());
                    } else {
                        Rlog.e("ADN RECORD", "mAdnLikeFiles.get is null");
                    }
                    if (AbstractIccRecords.getEmailAnrSupport()) {
                        updateUsimPhoneBookRecord(adn, efid, index);
                    } else {
                        this.mUsimPhoneBookManager.invalidateCache();
                    }
                }
                Message response = (Message) this.mUserWriteResponse.get(efid);
                this.mUserWriteResponse.delete(efid);
                if (response != null) {
                    AsyncResult.forMessage(response, null, ar.exception);
                    response.sendToTarget();
                    return;
                }
                return;
            default:
                return;
        }
    }
}
