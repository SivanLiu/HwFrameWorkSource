package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.AdnRecordCache;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class IccPhoneBookInterfaceManager extends AbstractIccPhoneBookInterfaceManager {
    protected static final boolean ALLOW_SIM_OP_IN_UI_THREAD = false;
    protected static final boolean DBG = true;
    protected static final int EVENT_GET_SIZE_DONE = 1;
    protected static final int EVENT_LOAD_DONE = 2;
    protected static final int EVENT_UPDATE_DONE = 3;
    static final String LOG_TAG = "IccPhoneBookIM";
    protected AdnRecordCache mAdnCache;
    protected Handler mBaseHandler = new Handler() {
        public void handleMessage(Message msg) {
            boolean z = false;
            AsyncResult ar;
            IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager;
            switch (msg.what) {
                case 1:
                    ar = (AsyncResult) msg.obj;
                    synchronized (IccPhoneBookInterfaceManager.this.mLock) {
                        if (ar.exception == null) {
                            IccPhoneBookInterfaceManager.this.mRecordSize = (int[]) ar.result;
                            iccPhoneBookInterfaceManager = IccPhoneBookInterfaceManager.this;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("GET_RECORD_SIZE Size ");
                            stringBuilder.append(IccPhoneBookInterfaceManager.this.mRecordSize[0]);
                            stringBuilder.append(" total ");
                            stringBuilder.append(IccPhoneBookInterfaceManager.this.mRecordSize[1]);
                            stringBuilder.append(" #record ");
                            stringBuilder.append(IccPhoneBookInterfaceManager.this.mRecordSize[2]);
                            iccPhoneBookInterfaceManager.logd(stringBuilder.toString());
                        }
                        notifyPending(ar);
                    }
                    return;
                case 2:
                    ar = (AsyncResult) msg.obj;
                    synchronized (IccPhoneBookInterfaceManager.this.mLock) {
                        if (ar.exception == null) {
                            IccPhoneBookInterfaceManager.this.mRecords = (List) ar.result;
                        } else {
                            IccPhoneBookInterfaceManager.this.logd("Cannot load ADN records");
                            IccPhoneBookInterfaceManager.this.mRecords = null;
                        }
                        notifyPending(ar);
                    }
                    return;
                case 3:
                    ar = msg.obj;
                    synchronized (IccPhoneBookInterfaceManager.this.mLock) {
                        iccPhoneBookInterfaceManager = IccPhoneBookInterfaceManager.this;
                        if (ar.exception == null) {
                            z = true;
                        }
                        iccPhoneBookInterfaceManager.mSuccess = z;
                        notifyPending(ar);
                    }
                    return;
                default:
                    return;
            }
        }

        private void notifyPending(AsyncResult ar) {
            if (ar.userObj != null) {
                ar.userObj.set(true);
            }
            IccPhoneBookInterfaceManager.this.mLock.notifyAll();
        }
    };
    protected final Object mLock = new Object();
    protected Object mLock2 = new Object();
    protected Phone mPhone;
    protected int[] mRecordSize;
    protected List<AdnRecord> mRecords;
    protected boolean mSuccess;

    public IccPhoneBookInterfaceManager(Phone phone) {
        this.mPhone = phone;
        IccRecords r = phone.getIccRecords();
        if (r != null) {
            this.mAdnCache = r.getAdnCache();
        }
    }

    public void dispose() {
        if (this.mRecords != null) {
            this.mRecords.clear();
        }
    }

    public void updateIccRecords(IccRecords iccRecords) {
        if (iccRecords != null) {
            this.mAdnCache = iccRecords.getAdnCache();
        } else {
            this.mAdnCache = null;
        }
    }

    protected void logd(String msg) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[IccPbInterfaceManager] ");
        stringBuilder.append(msg);
        Rlog.d(str, stringBuilder.toString());
    }

    protected void loge(String msg) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[IccPbInterfaceManager] ");
        stringBuilder.append(msg);
        Rlog.e(str, stringBuilder.toString());
    }

    public boolean updateAdnRecordsInEfBySearch(int efid, String oldTag, String oldPhoneNumber, String newTag, String newPhoneNumber, String pin2) {
        Throwable th;
        synchronized (this.mLock2) {
            try {
                if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") == 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateAdnRecordsInEfBySearch: efid=0x");
                    stringBuilder.append(Integer.toHexString(efid).toUpperCase());
                    stringBuilder.append(" (");
                    stringBuilder.append(oldTag);
                    stringBuilder.append(")==> (");
                    stringBuilder.append(newTag);
                    stringBuilder.append(")");
                    logd(stringBuilder.toString());
                    int efid2 = updateEfForIccType(efid);
                    try {
                        synchronized (this.mLock) {
                            checkThread();
                            this.mSuccess = false;
                            AtomicBoolean status = new AtomicBoolean(false);
                            Message response = this.mBaseHandler.obtainMessage(3, status);
                            AdnRecord oldAdn = new AdnRecord(oldTag, oldPhoneNumber);
                            AdnRecord newAdn = new AdnRecord(newTag, newPhoneNumber);
                            if (this.mAdnCache != null) {
                                this.mAdnCache.updateAdnBySearch(efid2, oldAdn, newAdn, pin2, response);
                                waitForResult(status);
                            } else {
                                loge("Failure while trying to update by search due to uninitialised adncache");
                            }
                        }
                        return this.mSuccess;
                    } catch (Throwable th2) {
                        th = th2;
                        efid = efid2;
                    }
                } else {
                    throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:22:0x007d, code skipped:
            return r1.mSuccess;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean updateAdnRecordsInEfByIndex(int efid, String newTag, String newPhoneNumber, int index, String pin2) {
        Throwable th;
        String str;
        String str2 = newTag;
        synchronized (this.mLock2) {
            int i;
            try {
                if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") == 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateAdnRecordsInEfByIndex: efid=0x");
                    stringBuilder.append(Integer.toHexString(efid).toUpperCase());
                    stringBuilder.append(" Index=");
                    i = index;
                    try {
                        stringBuilder.append(i);
                        stringBuilder.append(" ==> (");
                        stringBuilder.append(str2);
                        stringBuilder.append(")");
                        logd(stringBuilder.toString());
                        synchronized (this.mLock) {
                            AtomicBoolean status;
                            Message response;
                            try {
                                checkThread();
                                this.mSuccess = false;
                                status = new AtomicBoolean(false);
                                response = this.mBaseHandler.obtainMessage(3, status);
                            } catch (Throwable th2) {
                                th = th2;
                                str = newPhoneNumber;
                                throw th;
                            }
                            try {
                                AdnRecord newAdn = new AdnRecord(str2, newPhoneNumber);
                                if (this.mAdnCache != null) {
                                    this.mAdnCache.updateAdnByIndex(efid, newAdn, i, pin2, response);
                                    waitForResult(status);
                                } else {
                                    loge("Failure while trying to update by index due to uninitialised adncache");
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                throw th;
                            }
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        str = newPhoneNumber;
                        throw th;
                    }
                }
                str = newPhoneNumber;
                i = index;
                throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
            } catch (Throwable th5) {
                th = th5;
                str = newPhoneNumber;
                i = index;
                throw th;
            }
        }
    }

    public int[] getAdnRecordsSize(int efid) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getAdnRecordsSize: efid=");
        stringBuilder.append(efid);
        logd(stringBuilder.toString());
        synchronized (this.mLock) {
            checkThread();
            this.mRecordSize = new int[3];
            AtomicBoolean status = new AtomicBoolean(false);
            Message response = this.mBaseHandler.obtainMessage(1, status);
            IccFileHandler fh = this.mPhone.getIccFileHandler();
            if (fh != null) {
                fh.getEFLinearRecordSize(efid, response);
                waitForResult(status);
            }
        }
        return this.mRecordSize;
    }

    public List<AdnRecord> getAdnRecordsInEf(int efid) {
        List list;
        synchronized (this.mLock2) {
            if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") == 0) {
                efid = updateEfForIccType(efid);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getAdnRecordsInEF: efid=0x");
                stringBuilder.append(Integer.toHexString(efid).toUpperCase());
                logd(stringBuilder.toString());
                synchronized (this.mLock) {
                    checkThread();
                    AtomicBoolean status = new AtomicBoolean(false);
                    Message response = this.mBaseHandler.obtainMessage(2, status);
                    if (this.mAdnCache != null) {
                        this.mAdnCache.requestLoadAllAdnLike(efid, this.mAdnCache.extensionEfForEf(efid), response);
                        waitForResult(status);
                    } else {
                        loge("Failure while trying to load from SIM due to uninitialised adncache");
                    }
                }
                list = this.mRecords;
            } else {
                throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
            }
        }
        return list;
    }

    protected void checkThread() {
        if (this.mBaseHandler.getLooper().equals(Looper.myLooper())) {
            loge("query() called on the main UI thread!");
            throw new IllegalStateException("You cannot call query on this provder from the main UI thread.");
        }
    }

    protected void waitForResult(AtomicBoolean status) {
        while (!status.get()) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                logd("interrupted while trying to update by search");
            }
        }
    }

    private int updateEfForIccType(int efid) {
        if (efid == 28474 && (this.mPhone.getCurrentUiccAppType() == AppType.APPTYPE_USIM || this.mPhone.getCurrentUiccAppType() == AppType.APPTYPE_CSIM || this.mPhone.getCurrentUiccAppType() == AppType.APPTYPE_ISIM)) {
            return updateEfFor3gCardType(efid);
        }
        return efid;
    }

    public int getAlphaTagEncodingLength(String alphaTag) {
        return HwTelephonyFactory.getHwUiccManager().getAlphaTagEncodingLength(alphaTag);
    }

    public int updateEfForIccTypeHw(int efid) {
        return updateEfForIccType(efid);
    }
}
