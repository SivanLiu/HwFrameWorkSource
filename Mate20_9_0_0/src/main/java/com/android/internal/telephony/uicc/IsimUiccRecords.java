package com.android.internal.telephony.uicc;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.gsm.SimTlv;
import com.android.internal.telephony.uicc.IccRecords.IccRecordLoaded;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

public class IsimUiccRecords extends IccRecords implements IsimRecords {
    private static final boolean DBG = true;
    private static final boolean DUMP_RECORDS = false;
    private static final int EVENT_APP_READY = 1;
    private static final int EVENT_ISIM_AUTHENTICATE_DONE = 91;
    public static final String INTENT_ISIM_REFRESH = "com.android.intent.isim_refresh";
    protected static final String LOG_TAG = "IsimUiccRecords";
    private static final int TAG_ISIM_VALUE = 128;
    private static final boolean VDBG = false;
    private String auth_rsp;
    private String mIsimDomain;
    private String mIsimImpi;
    private String[] mIsimImpu;
    private String mIsimIst;
    private String[] mIsimPcscf;
    private final Object mLock;

    private class EfIsimDomainLoaded implements IccRecordLoaded {
        private EfIsimDomainLoaded() {
        }

        public String getEfName() {
            return "EF_ISIM_DOMAIN";
        }

        public void onRecordLoaded(AsyncResult ar) {
            IsimUiccRecords.this.mIsimDomain = IsimUiccRecords.isimTlvToString(ar.result);
        }
    }

    private class EfIsimImpiLoaded implements IccRecordLoaded {
        private EfIsimImpiLoaded() {
        }

        public String getEfName() {
            return "EF_ISIM_IMPI";
        }

        public void onRecordLoaded(AsyncResult ar) {
            IsimUiccRecords.this.mIsimImpi = IsimUiccRecords.isimTlvToString(ar.result);
        }
    }

    private class EfIsimImpuLoaded implements IccRecordLoaded {
        private EfIsimImpuLoaded() {
        }

        public String getEfName() {
            return "EF_ISIM_IMPU";
        }

        public void onRecordLoaded(AsyncResult ar) {
            ArrayList<byte[]> impuList = ar.result;
            IsimUiccRecords isimUiccRecords = IsimUiccRecords.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EF_IMPU record count: ");
            stringBuilder.append(impuList.size());
            isimUiccRecords.log(stringBuilder.toString());
            IsimUiccRecords.this.mIsimImpu = new String[impuList.size()];
            int i = 0;
            Iterator it = impuList.iterator();
            while (it.hasNext()) {
                int i2 = i + 1;
                IsimUiccRecords.this.mIsimImpu[i] = IsimUiccRecords.isimTlvToString((byte[]) it.next());
                i = i2;
            }
        }
    }

    private class EfIsimIstLoaded implements IccRecordLoaded {
        private EfIsimIstLoaded() {
        }

        public String getEfName() {
            return "EF_ISIM_IST";
        }

        public void onRecordLoaded(AsyncResult ar) {
            IsimUiccRecords.this.mIsimIst = IccUtils.bytesToHexString(ar.result);
        }
    }

    private class EfIsimPcscfLoaded implements IccRecordLoaded {
        private EfIsimPcscfLoaded() {
        }

        public String getEfName() {
            return "EF_ISIM_PCSCF";
        }

        public void onRecordLoaded(AsyncResult ar) {
            ArrayList<byte[]> pcscflist = ar.result;
            IsimUiccRecords isimUiccRecords = IsimUiccRecords.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EF_PCSCF record count: ");
            stringBuilder.append(pcscflist.size());
            isimUiccRecords.log(stringBuilder.toString());
            IsimUiccRecords.this.mIsimPcscf = new String[pcscflist.size()];
            int i = 0;
            Iterator it = pcscflist.iterator();
            while (it.hasNext()) {
                int i2 = i + 1;
                IsimUiccRecords.this.mIsimPcscf[i] = IsimUiccRecords.isimTlvToString((byte[]) it.next());
                i = i2;
            }
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IsimUiccRecords: ");
        stringBuilder.append(super.toString());
        stringBuilder.append("");
        return stringBuilder.toString();
    }

    public IsimUiccRecords(UiccCardApplication app, Context c, CommandsInterface ci) {
        super(app, c, ci);
        this.mLock = new Object();
        this.mAdnCache = new AdnRecordCache(this.mFh);
        this.mRecordsRequested = false;
        this.mLockedRecordsReqReason = 0;
        this.mRecordsToLoad = 0;
        resetRecords();
        this.mParentApp.registerForReady(this, 1, null);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IsimUiccRecords X ctor this=");
        stringBuilder.append(this);
        log(stringBuilder.toString());
    }

    public void dispose() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Disposing ");
        stringBuilder.append(this);
        log(stringBuilder.toString());
        this.mCi.unregisterForIccRefresh(this);
        this.mParentApp.unregisterForReady(this);
        resetRecords();
        super.dispose();
    }

    public void handleMessage(Message msg) {
        StringBuilder stringBuilder;
        if (this.mDestroyed.get()) {
            String str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Received message ");
            stringBuilder.append(msg);
            stringBuilder.append("[");
            stringBuilder.append(msg.what);
            stringBuilder.append("] while being destroyed. Ignoring.");
            Rlog.e(str, stringBuilder.toString());
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("IsimUiccRecords: handleMessage ");
        stringBuilder2.append(msg);
        stringBuilder2.append("[");
        stringBuilder2.append(msg.what);
        stringBuilder2.append("] ");
        loge(stringBuilder2.toString());
        try {
            int i = msg.what;
            if (i == 1) {
                onReady();
            } else if (i == 31) {
                broadcastRefresh();
                super.handleMessage(msg);
            } else if (i != 91) {
                super.handleMessage(msg);
            } else {
                AsyncResult ar = msg.obj;
                log("EVENT_ISIM_AUTHENTICATE_DONE");
                if (ar.exception != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception ISIM AKA: ");
                    stringBuilder.append(ar.exception);
                    log(stringBuilder.toString());
                } else {
                    try {
                        this.auth_rsp = (String) ar.result;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("ISIM AKA: auth_rsp = ");
                        stringBuilder.append(this.auth_rsp);
                        log(stringBuilder.toString());
                    } catch (Exception e) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Failed to parse ISIM AKA contents: ");
                        stringBuilder3.append(e);
                        log(stringBuilder3.toString());
                    }
                }
                synchronized (this.mLock) {
                    this.mLock.notifyAll();
                }
            }
        } catch (RuntimeException exc) {
            Rlog.w(LOG_TAG, "Exception parsing SIM record", exc);
        }
    }

    protected void fetchIsimRecords() {
        this.mRecordsRequested = true;
        this.mFh.loadEFTransparent(IccConstants.EF_IMPI, obtainMessage(100, new EfIsimImpiLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_IMPU, obtainMessage(100, new EfIsimImpuLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_DOMAIN, obtainMessage(100, new EfIsimDomainLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_IST, obtainMessage(100, new EfIsimIstLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_PCSCF, obtainMessage(100, new EfIsimPcscfLoaded()));
        this.mRecordsToLoad++;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fetchIsimRecords ");
        stringBuilder.append(this.mRecordsToLoad);
        stringBuilder.append(" requested: ");
        stringBuilder.append(this.mRecordsRequested);
        log(stringBuilder.toString());
    }

    protected void resetRecords() {
        this.mIsimImpi = null;
        this.mIsimDomain = null;
        this.mIsimImpu = null;
        this.mIsimIst = null;
        this.mIsimPcscf = null;
        this.auth_rsp = null;
        this.mRecordsRequested = false;
        this.mLockedRecordsReqReason = 0;
        this.mLoaded.set(false);
        this.mImsiLoad = false;
    }

    private static String isimTlvToString(byte[] record) {
        SimTlv tlv = new SimTlv(record, 0, record.length);
        while (tlv.getTag() != 128) {
            if (!tlv.nextObject()) {
                return null;
            }
        }
        return new String(tlv.getData(), Charset.forName("UTF-8"));
    }

    protected void onRecordLoaded() {
        this.mRecordsToLoad--;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onRecordLoaded ");
        stringBuilder.append(this.mRecordsToLoad);
        stringBuilder.append(" requested: ");
        stringBuilder.append(this.mRecordsRequested);
        log(stringBuilder.toString());
        if (getRecordsLoaded()) {
            onAllRecordsLoaded();
            this.mImsiLoad = true;
        } else if (getLockedRecordsLoaded() || getNetworkLockedRecordsLoaded()) {
            onLockedAllRecordsLoaded();
        } else if (this.mRecordsToLoad < 0) {
            loge("recordsToLoad <0, programmer error suspected");
            this.mRecordsToLoad = 0;
        }
    }

    private void onLockedAllRecordsLoaded() {
        log("SIM locked; record load complete");
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
        log("record load complete");
        this.mLoaded.set(true);
        this.mRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
    }

    protected void handleFileUpdate(int efid) {
        if (efid != IccConstants.EF_IST) {
            if (efid != IccConstants.EF_PCSCF) {
                switch (efid) {
                    case IccConstants.EF_IMPI /*28418*/:
                        this.mFh.loadEFTransparent(IccConstants.EF_IMPI, obtainMessage(100, new EfIsimImpiLoaded()));
                        this.mRecordsToLoad++;
                        return;
                    case IccConstants.EF_DOMAIN /*28419*/:
                        this.mFh.loadEFTransparent(IccConstants.EF_DOMAIN, obtainMessage(100, new EfIsimDomainLoaded()));
                        this.mRecordsToLoad++;
                        return;
                    case IccConstants.EF_IMPU /*28420*/:
                        this.mFh.loadEFLinearFixedAll(IccConstants.EF_IMPU, obtainMessage(100, new EfIsimImpuLoaded()));
                        this.mRecordsToLoad++;
                        return;
                }
            }
            this.mFh.loadEFLinearFixedAll(IccConstants.EF_PCSCF, obtainMessage(100, new EfIsimPcscfLoaded()));
            this.mRecordsToLoad++;
            fetchIsimRecords();
            return;
        }
        this.mFh.loadEFTransparent(IccConstants.EF_IST, obtainMessage(100, new EfIsimIstLoaded()));
        this.mRecordsToLoad++;
    }

    private void broadcastRefresh() {
        Intent intent = new Intent(INTENT_ISIM_REFRESH);
        log("send ISim REFRESH: com.android.intent.isim_refresh");
        this.mContext.sendBroadcast(intent);
    }

    public String getIsimImpi() {
        return this.mIsimImpi;
    }

    public String getIsimDomain() {
        return this.mIsimDomain;
    }

    public String[] getIsimImpu() {
        return this.mIsimImpu != null ? (String[]) this.mIsimImpu.clone() : null;
    }

    public String getIsimIst() {
        return this.mIsimIst;
    }

    public String[] getIsimPcscf() {
        return this.mIsimPcscf != null ? (String[]) this.mIsimPcscf.clone() : null;
    }

    public int getDisplayRule(ServiceState serviceState) {
        return 0;
    }

    public void onReady() {
        fetchIsimRecords();
    }

    public void onRefresh(boolean fileChanged, int[] fileList) {
        if (fileChanged) {
            fetchIsimRecords();
        }
    }

    public void setVoiceMailNumber(String alphaTag, String voiceNumber, Message onComplete) {
    }

    public void setVoiceMessageWaiting(int line, int countWaiting) {
    }

    protected void log(String s) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ISIM] ");
        stringBuilder.append(s);
        Rlog.d(str, stringBuilder.toString());
    }

    protected void loge(String s) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ISIM] ");
        stringBuilder.append(s);
        Rlog.e(str, stringBuilder.toString());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IsimRecords: ");
        stringBuilder.append(this);
        pw.println(stringBuilder.toString());
        pw.println(" extends:");
        super.dump(fd, pw, args);
        pw.flush();
    }

    public int getVoiceMessageCount() {
        return 0;
    }
}
