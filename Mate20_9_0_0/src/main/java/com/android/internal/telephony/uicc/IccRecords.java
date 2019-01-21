package com.android.internal.telephony.uicc;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class IccRecords extends AbstractIccRecords implements IccConstants {
    public static final int CALL_FORWARDING_STATUS_DISABLED = 0;
    public static final int CALL_FORWARDING_STATUS_ENABLED = 1;
    public static final int CALL_FORWARDING_STATUS_UNKNOWN = -1;
    protected static final boolean DBG = true;
    public static final int DEFAULT_VOICE_MESSAGE_COUNT = -2;
    static final Set<Integer> EFID_SET = new HashSet(Arrays.asList(new Integer[]{Integer.valueOf(IccConstants.EF_ICCID), Integer.valueOf(IccConstants.EF_IST), Integer.valueOf(IccConstants.EF_AD), Integer.valueOf(IccConstants.EF_GID1), Integer.valueOf(IccConstants.EF_GID2), Integer.valueOf(IccConstants.EF_SST), Integer.valueOf(IccConstants.EF_SPN), Integer.valueOf(IccConstants.EF_SPN_CPHS), Integer.valueOf(IccConstants.EF_SPN_SHORT_CPHS), Integer.valueOf(IccConstants.EF_IMPI)}));
    private static final int EVENT_AKA_AUTHENTICATE_DONE = 90;
    protected static final int EVENT_APP_READY = 1;
    public static final int EVENT_CFI = 1;
    public static final int EVENT_GET_ICC_RECORD_DONE = 100;
    public static final int EVENT_MWI = 0;
    public static final int EVENT_REFRESH = 31;
    public static final int EVENT_SPN = 2;
    protected static final int HANDLER_ACTION_BASE = 1238272;
    protected static final int HANDLER_ACTION_NONE = 1238272;
    protected static final int HANDLER_ACTION_SEND_RESPONSE = 1238273;
    protected static final int LOCKED_RECORDS_REQ_REASON_LOCKED = 1;
    protected static final int LOCKED_RECORDS_REQ_REASON_NETWORK_LOCKED = 2;
    protected static final int LOCKED_RECORDS_REQ_REASON_NONE = 0;
    public static final String PROPERTY_MCC_MATCHING_FYROM = "persist.sys.mcc_match_fyrom";
    public static final int SPN_RULE_SHOW_PLMN = 2;
    public static final int SPN_RULE_SHOW_SPN = 1;
    protected static final int UNINITIALIZED = -1;
    protected static final int UNKNOWN = 0;
    public static final int UNKNOWN_VOICE_MESSAGE_COUNT = -1;
    protected static final boolean VDBG = false;
    protected static AtomicInteger sNextRequestId = new AtomicInteger(1);
    private IccIoResult auth_rsp;
    protected AdnRecordCache mAdnCache;
    CarrierTestOverride mCarrierTestOverride;
    protected CommandsInterface mCi;
    protected Context mContext;
    protected AtomicBoolean mDestroyed = new AtomicBoolean(false);
    protected String[] mEhplmns;
    public RegistrantList mFdnRecordsLoadedRegistrants = new RegistrantList();
    protected IccFileHandler mFh;
    protected String[] mFplmns;
    protected String mFullIccId;
    protected String mGid1;
    protected String mGid2;
    protected PlmnActRecord[] mHplmnActRecords;
    protected String mIccId;
    protected String mImsi;
    protected RegistrantList mImsiReadyRegistrants = new RegistrantList();
    protected boolean mIsVoiceMailFixed = false;
    protected AtomicBoolean mLoaded = new AtomicBoolean(false);
    private final Object mLock = new Object();
    protected RegistrantList mLockedRecordsLoadedRegistrants = new RegistrantList();
    protected int mLockedRecordsReqReason = 0;
    protected int mMailboxIndex = 0;
    protected int mMncLength = -1;
    protected String mMsisdn = null;
    protected String mMsisdnTag = null;
    protected RegistrantList mNetworkLockedRecordsLoadedRegistrants = new RegistrantList();
    protected RegistrantList mNetworkSelectionModeAutomaticRegistrants = new RegistrantList();
    protected String mNewMsisdn = null;
    protected String mNewMsisdnTag = null;
    protected RegistrantList mNewSmsRegistrants = new RegistrantList();
    protected String mNewVoiceMailNum = null;
    protected String mNewVoiceMailTag = null;
    protected PlmnActRecord[] mOplmnActRecords;
    protected UiccCardApplication mParentApp;
    protected final HashMap<Integer, Message> mPendingResponses = new HashMap();
    protected PlmnActRecord[] mPlmnActRecords;
    protected String mPnnHomeName;
    protected String mPrefLang;
    protected RegistrantList mRecordsEventsRegistrants = new RegistrantList();
    protected RegistrantList mRecordsLoadedRegistrants = new RegistrantList();
    protected RegistrantList mRecordsOverrideRegistrants = new RegistrantList();
    protected boolean mRecordsRequested = false;
    protected int mRecordsToLoad;
    private String mSpn;
    protected RegistrantList mSpnUpdatedRegistrants = new RegistrantList();
    protected TelephonyManager mTelephonyManager;
    protected String mVoiceMailNum = null;
    protected String mVoiceMailTag = null;

    public interface IccRecordLoaded {
        String getEfName();

        void onRecordLoaded(AsyncResult asyncResult);
    }

    public abstract int getDisplayRule(ServiceState serviceState);

    public abstract int getVoiceMessageCount();

    protected abstract void handleFileUpdate(int i);

    protected abstract void log(String str);

    protected abstract void loge(String str);

    protected abstract void onAllRecordsLoaded();

    public abstract void onReady();

    protected abstract void onRecordLoaded();

    public abstract void onRefresh(boolean z, int[] iArr);

    public abstract void setVoiceMailNumber(String str, String str2, Message message);

    public abstract void setVoiceMessageWaiting(int i, int i2);

    public String toString() {
        StringBuilder stringBuilder;
        String stringBuilder2;
        String iccIdToPrint = SubscriptionInfo.givePrintableIccid(this.mFullIccId);
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("mDestroyed=");
        stringBuilder3.append(this.mDestroyed);
        stringBuilder3.append(" mContext=");
        stringBuilder3.append(this.mContext);
        stringBuilder3.append(" mCi=");
        stringBuilder3.append(this.mCi);
        stringBuilder3.append(" mFh=");
        stringBuilder3.append(this.mFh);
        stringBuilder3.append(" mParentApp=");
        stringBuilder3.append(this.mParentApp);
        stringBuilder3.append(" recordsToLoad=");
        stringBuilder3.append(this.mRecordsToLoad);
        stringBuilder3.append(" adnCache=");
        stringBuilder3.append(this.mAdnCache);
        stringBuilder3.append(" recordsRequested=");
        stringBuilder3.append(this.mRecordsRequested);
        stringBuilder3.append(" lockedRecordsReqReason=");
        stringBuilder3.append(this.mLockedRecordsReqReason);
        stringBuilder3.append(" iccid=");
        stringBuilder3.append(iccIdToPrint);
        if (this.mCarrierTestOverride.isInTestMode()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("mFakeIccid=");
            stringBuilder.append(this.mCarrierTestOverride.getFakeIccid());
            stringBuilder2 = stringBuilder.toString();
        } else {
            stringBuilder2 = "";
        }
        stringBuilder3.append(stringBuilder2);
        stringBuilder3.append(" msisdnTag=");
        stringBuilder3.append(this.mMsisdnTag);
        stringBuilder3.append(" voiceMailNum=");
        stringBuilder3.append(Rlog.pii(false, this.mVoiceMailNum));
        stringBuilder3.append(" voiceMailTag=");
        stringBuilder3.append(this.mVoiceMailTag);
        stringBuilder3.append(" newVoiceMailNum=");
        stringBuilder3.append(Rlog.pii(false, this.mNewVoiceMailNum));
        stringBuilder3.append(" newVoiceMailTag=");
        stringBuilder3.append(this.mNewVoiceMailTag);
        stringBuilder3.append(" isVoiceMailFixed=");
        stringBuilder3.append(this.mIsVoiceMailFixed);
        stringBuilder3.append(" mImsi=");
        if (this.mImsi != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.mImsi.substring(0, 6));
            stringBuilder.append(Rlog.pii(false, this.mImsi.substring(6)));
            stringBuilder2 = stringBuilder.toString();
        } else {
            stringBuilder2 = "null";
        }
        stringBuilder3.append(stringBuilder2);
        if (this.mCarrierTestOverride.isInTestMode()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mFakeImsi=");
            stringBuilder.append(this.mCarrierTestOverride.getFakeIMSI());
            stringBuilder2 = stringBuilder.toString();
        } else {
            stringBuilder2 = "";
        }
        stringBuilder3.append(stringBuilder2);
        stringBuilder3.append(" mncLength=");
        stringBuilder3.append(this.mMncLength);
        stringBuilder3.append(" mailboxIndex=");
        stringBuilder3.append(this.mMailboxIndex);
        stringBuilder3.append(" spn=");
        stringBuilder3.append(this.mSpn);
        if (this.mCarrierTestOverride.isInTestMode()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mFakeSpn=");
            stringBuilder.append(this.mCarrierTestOverride.getFakeSpn());
            stringBuilder2 = stringBuilder.toString();
        } else {
            stringBuilder2 = "";
        }
        stringBuilder3.append(stringBuilder2);
        return stringBuilder3.toString();
    }

    public IccRecords(UiccCardApplication app, Context c, CommandsInterface ci) {
        AbstractIccRecords.loadEmailAnrSupportFlag(c);
        AbstractIccRecords.loadAdnLongNumberFlag(c);
        this.mContext = c;
        this.mCi = ci;
        this.mFh = app.getIccFileHandler();
        this.mParentApp = app;
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mCarrierTestOverride = new CarrierTestOverride();
        this.mCi.registerForIccRefresh(this, 31, null);
    }

    public void setCarrierTestOverride(String mccmnc, String imsi, String iccid, String gid1, String gid2, String pnn, String spn) {
        this.mCarrierTestOverride.override(mccmnc, imsi, iccid, gid1, gid2, pnn, spn);
        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), spn);
        this.mTelephonyManager.setSimOperatorNumericForPhone(this.mParentApp.getPhoneId(), mccmnc);
        this.mRecordsLoadedRegistrants.notifyRegistrants();
    }

    public void dispose() {
        this.mDestroyed.set(true);
        this.auth_rsp = null;
        synchronized (this.mLock) {
            this.mLock.notifyAll();
        }
        this.mCi.unregisterForIccRefresh(this);
        this.mParentApp = null;
        this.mFh = null;
        this.mCi = null;
        this.mContext = null;
        if (this.mAdnCache != null) {
            this.mAdnCache.reset();
        }
        this.mLoaded.set(false);
    }

    void recordsRequired() {
    }

    public AdnRecordCache getAdnCache() {
        return this.mAdnCache;
    }

    public int storePendingResponseMessage(Message msg) {
        int key = sNextRequestId.getAndIncrement();
        synchronized (this.mPendingResponses) {
            this.mPendingResponses.put(Integer.valueOf(key), msg);
        }
        return key;
    }

    public Message retrievePendingResponseMessage(Integer key) {
        Message message;
        synchronized (this.mPendingResponses) {
            message = (Message) this.mPendingResponses.remove(key);
        }
        return message;
    }

    public String getIccId() {
        if (!this.mCarrierTestOverride.isInTestMode() || this.mCarrierTestOverride.getFakeIccid() == null) {
            return this.mIccId;
        }
        return this.mCarrierTestOverride.getFakeIccid();
    }

    public String getFullIccId() {
        return this.mFullIccId;
    }

    public void registerForRecordsLoaded(Handler h, int what, Object obj) {
        if (!this.mDestroyed.get()) {
            int i = this.mRecordsLoadedRegistrants.size() - 1;
            while (i >= 0) {
                Handler rH = ((Registrant) this.mRecordsLoadedRegistrants.get(i)).getHandler();
                if (rH == null || rH != h) {
                    i--;
                } else {
                    return;
                }
            }
            Registrant r = new Registrant(h, what, obj);
            this.mRecordsLoadedRegistrants.add(r);
            if (this.mRecordsToLoad == 0 && this.mRecordsRequested && this.mParentApp != null && AppState.APPSTATE_READY == this.mParentApp.getState()) {
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    public void unregisterForRecordsLoaded(Handler h) {
        this.mRecordsLoadedRegistrants.remove(h);
    }

    public void registerForFdnRecordsLoaded(Handler h, int what, Object obj) {
        if (!this.mDestroyed.get()) {
            this.mFdnRecordsLoadedRegistrants.add(new Registrant(h, what, obj));
        }
    }

    public void unregisterForFdnRecordsLoaded(Handler h) {
        this.mFdnRecordsLoadedRegistrants.remove(h);
    }

    public void unregisterForRecordsOverride(Handler h) {
        this.mRecordsOverrideRegistrants.remove(h);
    }

    public void registerForRecordsOverride(Handler h, int what, Object obj) {
        if (!this.mDestroyed.get()) {
            Registrant r = new Registrant(h, what, obj);
            this.mRecordsOverrideRegistrants.add(r);
            if (getRecordsLoaded()) {
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    public void registerForLockedRecordsLoaded(Handler h, int what, Object obj) {
        if (!this.mDestroyed.get()) {
            Registrant r = new Registrant(h, what, obj);
            this.mLockedRecordsLoadedRegistrants.add(r);
            if (getLockedRecordsLoaded()) {
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    public void unregisterForLockedRecordsLoaded(Handler h) {
        this.mLockedRecordsLoadedRegistrants.remove(h);
    }

    public void registerForNetworkLockedRecordsLoaded(Handler h, int what, Object obj) {
        if (!this.mDestroyed.get()) {
            Registrant r = new Registrant(h, what, obj);
            this.mNetworkLockedRecordsLoadedRegistrants.add(r);
            if (getNetworkLockedRecordsLoaded()) {
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    public void unregisterForNetworkLockedRecordsLoaded(Handler h) {
        this.mNetworkLockedRecordsLoadedRegistrants.remove(h);
    }

    public void registerForImsiReady(Handler h, int what, Object obj) {
        if (!this.mDestroyed.get()) {
            Registrant r = new Registrant(h, what, obj);
            this.mImsiReadyRegistrants.add(r);
            if (!(getIMSI() == null || this.mParentApp == null || AppState.APPSTATE_READY != this.mParentApp.getState())) {
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    public void unregisterForImsiReady(Handler h) {
        this.mImsiReadyRegistrants.remove(h);
    }

    public void registerForSpnUpdate(Handler h, int what, Object obj) {
        if (!this.mDestroyed.get()) {
            Registrant r = new Registrant(h, what, obj);
            this.mSpnUpdatedRegistrants.add(r);
            if (!TextUtils.isEmpty(this.mSpn)) {
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    public void unregisterForSpnUpdate(Handler h) {
        this.mSpnUpdatedRegistrants.remove(h);
    }

    public void registerForRecordsEvents(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mRecordsEventsRegistrants.add(r);
        r.notifyResult(Integer.valueOf(0));
        r.notifyResult(Integer.valueOf(1));
    }

    public void unregisterForRecordsEvents(Handler h) {
        this.mRecordsEventsRegistrants.remove(h);
    }

    public void registerForNewSms(Handler h, int what, Object obj) {
        this.mNewSmsRegistrants.add(new Registrant(h, what, obj));
    }

    public void unregisterForNewSms(Handler h) {
        this.mNewSmsRegistrants.remove(h);
    }

    public void registerForNetworkSelectionModeAutomatic(Handler h, int what, Object obj) {
        this.mNetworkSelectionModeAutomaticRegistrants.add(new Registrant(h, what, obj));
    }

    public void unregisterForNetworkSelectionModeAutomatic(Handler h) {
        this.mNetworkSelectionModeAutomaticRegistrants.remove(h);
    }

    public synchronized void registerForLoadIccID(Handler h, int what, Object obj) {
        super.registerForLoadIccID(h, what, obj);
        if (!TextUtils.isEmpty(this.mIccId)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mIccId exist before registerForLoadIccID. mIccId = ");
            stringBuilder.append(SubscriptionInfo.givePrintableIccid(this.mIccId));
            log(stringBuilder.toString());
            Object r = this.mIccId;
            Message m = Message.obtain(h, what, obj);
            AsyncResult.forMessage(m, r, null);
            m.sendToTarget();
        }
    }

    public String getIMSI() {
        if (!this.mCarrierTestOverride.isInTestMode() || this.mCarrierTestOverride.getFakeIMSI() == null) {
            return this.mImsi;
        }
        return this.mCarrierTestOverride.getFakeIMSI();
    }

    public void setImsi(String imsi) {
        this.mImsi = imsi;
        this.mImsiReadyRegistrants.notifyRegistrants();
    }

    public String getNAI() {
        return null;
    }

    public String getMsisdnNumber() {
        return this.mMsisdn;
    }

    public String getGid1() {
        if (!this.mCarrierTestOverride.isInTestMode() || this.mCarrierTestOverride.getFakeGid1() == null) {
            return this.mGid1;
        }
        return this.mCarrierTestOverride.getFakeGid1();
    }

    public String getGid2() {
        if (!this.mCarrierTestOverride.isInTestMode() || this.mCarrierTestOverride.getFakeGid2() == null) {
            return this.mGid2;
        }
        return this.mCarrierTestOverride.getFakeGid2();
    }

    public String getPnnHomeName() {
        if (!this.mCarrierTestOverride.isInTestMode() || this.mCarrierTestOverride.getFakePnnHomeName() == null) {
            return this.mPnnHomeName;
        }
        return this.mCarrierTestOverride.getFakePnnHomeName();
    }

    public void setMsisdnNumber(String alphaTag, String number, Message onComplete) {
        loge("setMsisdn() should not be invoked on base IccRecords");
        AsyncResult.forMessage(onComplete).exception = new IccIoResult(106, 130, (byte[]) null).getException();
        onComplete.sendToTarget();
    }

    public String getMsisdnAlphaTag() {
        return this.mMsisdnTag;
    }

    public String getVoiceMailNumber() {
        return this.mVoiceMailNum;
    }

    public String getServiceProviderName() {
        if (this.mCarrierTestOverride.isInTestMode() && this.mCarrierTestOverride.getFakeSpn() != null) {
            return this.mCarrierTestOverride.getFakeSpn();
        }
        String providerName = this.mSpn;
        UiccCardApplication parentApp = this.mParentApp;
        if (parentApp != null) {
            UiccProfile profile = parentApp.getUiccProfile();
            if (profile != null) {
                String brandOverride = profile.getOperatorBrandOverride();
                StringBuilder stringBuilder;
                if (brandOverride != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("override, providerName=");
                    stringBuilder.append(providerName);
                    log(stringBuilder.toString());
                    providerName = brandOverride;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("no brandOverride, providerName=");
                    stringBuilder.append(providerName);
                    log(stringBuilder.toString());
                }
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("card is null, providerName=");
                stringBuilder2.append(providerName);
                log(stringBuilder2.toString());
            }
        } else {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("mParentApp is null, providerName=");
            stringBuilder3.append(providerName);
            log(stringBuilder3.toString());
        }
        return providerName;
    }

    protected void setServiceProviderName(String spn) {
        if (!TextUtils.equals(this.mSpn, spn)) {
            this.mSpnUpdatedRegistrants.notifyRegistrants();
            this.mSpn = spn;
        }
    }

    public String getVoiceMailAlphaTag() {
        return this.mVoiceMailTag;
    }

    public boolean getRecordsLoaded() {
        return this.mRecordsToLoad == 0 && this.mRecordsRequested;
    }

    protected boolean getLockedRecordsLoaded() {
        return this.mRecordsToLoad == 0 && this.mLockedRecordsReqReason == 1;
    }

    protected boolean getNetworkLockedRecordsLoaded() {
        return this.mRecordsToLoad == 0 && this.mLockedRecordsReqReason == 2;
    }

    public void handleMessage(Message msg) {
        StringBuilder stringBuilder;
        int i = msg.what;
        AsyncResult ar;
        StringBuilder stringBuilder2;
        if (i == 31) {
            ar = (AsyncResult) msg.obj;
            log("Card REFRESH occurred: ");
            if (ar.exception == null) {
                handleRefresh((IccRefreshResponse) ar.result);
                return;
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Icc refresh Exception: ");
            stringBuilder2.append(ar.exception);
            loge(stringBuilder2.toString());
        } else if (i == EVENT_AKA_AUTHENTICATE_DONE) {
            ar = msg.obj;
            this.auth_rsp = null;
            log("EVENT_AKA_AUTHENTICATE_DONE");
            if (ar.exception != null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Exception ICC SIM AKA: ");
                stringBuilder2.append(ar.exception);
                loge(stringBuilder2.toString());
            } else {
                try {
                    this.auth_rsp = (IccIoResult) ar.result;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ICC SIM AKA: auth_rsp = ");
                    stringBuilder2.append(this.auth_rsp);
                    log(stringBuilder2.toString());
                } catch (Exception e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to parse ICC SIM AKA contents: ");
                    stringBuilder.append(e);
                    loge(stringBuilder.toString());
                }
            }
            synchronized (this.mLock) {
                this.mLock.notifyAll();
            }
        } else if (i != 100) {
            super.handleMessage(msg);
        } else {
            try {
                ar = msg.obj;
                IccRecordLoaded recordLoaded = ar.userObj;
                stringBuilder = new StringBuilder();
                stringBuilder.append(recordLoaded.getEfName());
                stringBuilder.append(" LOADED");
                log(stringBuilder.toString());
                if (ar.exception != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Record Load Exception: ");
                    stringBuilder.append(ar.exception);
                    loge(stringBuilder.toString());
                } else {
                    recordLoaded.onRecordLoaded(ar);
                }
            } catch (RuntimeException exc) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Exception parsing SIM record: ");
                stringBuilder2.append(exc);
                loge(stringBuilder2.toString());
            } catch (Throwable th) {
                onRecordLoaded();
            }
            onRecordLoaded();
        }
    }

    public String getSimLanguage() {
        return this.mPrefLang;
    }

    protected void setSimLanguage(byte[] efLi, byte[] efPl) {
        StringBuilder stringBuilder;
        String[] locales = this.mContext.getAssets().getLocales();
        try {
            this.mPrefLang = findBestLanguage(efLi, locales);
        } catch (UnsupportedEncodingException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to parse EF-LI: ");
            stringBuilder.append(Arrays.toString(efLi));
            log(stringBuilder.toString());
        }
        if (this.mPrefLang == null) {
            try {
                this.mPrefLang = findBestLanguage(efPl, locales);
            } catch (UnsupportedEncodingException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to parse EF-PL: ");
                stringBuilder.append(Arrays.toString(efLi));
                log(stringBuilder.toString());
            }
        }
    }

    protected static String findBestLanguage(byte[] languages, String[] locales) throws UnsupportedEncodingException {
        if (languages == null || locales == null) {
            return null;
        }
        for (int i = 0; i + 1 < languages.length; i += 2) {
            String lang = new String(languages, i, 2, "ISO-8859-1");
            int j = 0;
            while (j < locales.length) {
                if (locales[j] != null && locales[j].length() >= 2 && locales[j].substring(0, 2).equalsIgnoreCase(lang)) {
                    return lang;
                }
                j++;
            }
        }
        return null;
    }

    protected void onIccRefreshInit() {
        this.mAdnCache.reset();
        this.mMncLength = -1;
        UiccCardApplication parentApp = this.mParentApp;
        if (parentApp != null && parentApp.getState() == AppState.APPSTATE_READY) {
            sendMessage(obtainMessage(1));
        }
    }

    protected void handleRefresh(IccRefreshResponse refreshResponse) {
        if (refreshResponse == null) {
            log("handleRefresh received without input");
        } else if (TextUtils.isEmpty(refreshResponse.aid) || refreshResponse.aid.equals(this.mParentApp.getAid())) {
            if (this instanceof SIMRecords) {
                log("before sim refresh");
                if (beforeHandleSimRefresh(refreshResponse)) {
                    return;
                }
            } else if (this instanceof RuimRecords) {
                log("before ruim refresh");
                if (beforeHandleRuimRefresh(refreshResponse)) {
                    return;
                }
            }
            switch (refreshResponse.refreshResult) {
                case 0:
                    log("handleRefresh with SIM_FILE_UPDATED");
                    handleFileUpdate(refreshResponse.efId);
                    break;
                case 1:
                    log("handleRefresh with SIM_REFRESH_INIT");
                    onIccRefreshInit();
                    if (this instanceof SIMRecords) {
                        log("sim refresh init");
                        this.mParentApp.queryFdn();
                        break;
                    }
                    break;
                case 2:
                    log("handleRefresh with SIM_REFRESH_RESET");
                    break;
                default:
                    log("handleRefresh with unknown operation");
                    break;
            }
            if (this instanceof SIMRecords) {
                log("after sim refresh");
                if (afterHandleSimRefresh(refreshResponse)) {
                }
            } else if (this instanceof RuimRecords) {
                log("after ruim refresh");
                if (afterHandleRuimRefresh(refreshResponse)) {
                }
            }
        }
    }

    public boolean isCspPlmnEnabled() {
        return false;
    }

    public String getOperatorNumeric() {
        return null;
    }

    public int getVoiceCallForwardingFlag() {
        return -1;
    }

    public void setVoiceCallForwardingFlag(int line, boolean enable, String number) {
    }

    public boolean isLoaded() {
        return this.mLoaded.get();
    }

    public boolean isProvisioned() {
        return true;
    }

    public IsimRecords getIsimRecords() {
        return null;
    }

    public UsimServiceTable getUsimServiceTable() {
        return null;
    }

    protected void setSystemProperty(String key, String val) {
        TelephonyManager.getDefault();
        TelephonyManager.setTelephonyProperty(this.mParentApp.getPhoneId(), key, val);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[key, value]=");
        stringBuilder.append(key);
        stringBuilder.append(", ");
        stringBuilder.append(val);
        log(stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:14:0x0029, code skipped:
            if (r6.auth_rsp != null) goto L_0x0031;
     */
    /* JADX WARNING: Missing block: B:15:0x002b, code skipped:
            loge("getIccSimChallengeResponse: No authentication response");
     */
    /* JADX WARNING: Missing block: B:16:0x0030, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:17:0x0031, code skipped:
            log("getIccSimChallengeResponse: return auth_rsp");
     */
    /* JADX WARNING: Missing block: B:18:0x003f, code skipped:
            return android.util.Base64.encodeToString(r6.auth_rsp.payload, 2);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String getIccSimChallengeResponse(int authContext, String data) {
        log("getIccSimChallengeResponse:");
        try {
            synchronized (this.mLock) {
                CommandsInterface ci = this.mCi;
                UiccCardApplication parentApp = this.mParentApp;
                if (ci == null || parentApp == null) {
                    loge("getIccSimChallengeResponse: Fail, ci or parentApp is null");
                    return null;
                }
                ci.requestIccSimAuthentication(authContext, data, parentApp.getAid(), obtainMessage(EVENT_AKA_AUTHENTICATE_DONE));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    loge("getIccSimChallengeResponse: Fail, interrupted while trying to request Icc Sim Auth");
                    return null;
                }
            }
        } catch (Exception e2) {
            loge("getIccSimChallengeResponse: Fail while trying to request Icc Sim Auth");
            return null;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        int i;
        StringBuilder stringBuilder;
        String stringBuilder2;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("IccRecords: ");
        stringBuilder3.append(this);
        pw.println(stringBuilder3.toString());
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mDestroyed=");
        stringBuilder3.append(this.mDestroyed);
        pw.println(stringBuilder3.toString());
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mCi=");
        stringBuilder3.append(this.mCi);
        pw.println(stringBuilder3.toString());
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mFh=");
        stringBuilder3.append(this.mFh);
        pw.println(stringBuilder3.toString());
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mParentApp=");
        stringBuilder3.append(this.mParentApp);
        pw.println(stringBuilder3.toString());
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" recordsLoadedRegistrants: size=");
        stringBuilder3.append(this.mRecordsLoadedRegistrants.size());
        pw.println(stringBuilder3.toString());
        for (i = 0; i < this.mRecordsLoadedRegistrants.size(); i++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  recordsLoadedRegistrants[");
            stringBuilder.append(i);
            stringBuilder.append("]=");
            stringBuilder.append(((Registrant) this.mRecordsLoadedRegistrants.get(i)).getHandler());
            pw.println(stringBuilder.toString());
        }
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append(" mLockedRecordsLoadedRegistrants: size=");
        stringBuilder4.append(this.mLockedRecordsLoadedRegistrants.size());
        pw.println(stringBuilder4.toString());
        for (i = 0; i < this.mLockedRecordsLoadedRegistrants.size(); i++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mLockedRecordsLoadedRegistrants[");
            stringBuilder.append(i);
            stringBuilder.append("]=");
            stringBuilder.append(((Registrant) this.mLockedRecordsLoadedRegistrants.get(i)).getHandler());
            pw.println(stringBuilder.toString());
        }
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append(" mNetworkLockedRecordsLoadedRegistrants: size=");
        stringBuilder4.append(this.mNetworkLockedRecordsLoadedRegistrants.size());
        pw.println(stringBuilder4.toString());
        for (i = 0; i < this.mNetworkLockedRecordsLoadedRegistrants.size(); i++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mLockedRecordsLoadedRegistrants[");
            stringBuilder.append(i);
            stringBuilder.append("]=");
            stringBuilder.append(((Registrant) this.mNetworkLockedRecordsLoadedRegistrants.get(i)).getHandler());
            pw.println(stringBuilder.toString());
        }
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append(" mImsiReadyRegistrants: size=");
        stringBuilder4.append(this.mImsiReadyRegistrants.size());
        pw.println(stringBuilder4.toString());
        for (i = 0; i < this.mImsiReadyRegistrants.size(); i++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mImsiReadyRegistrants[");
            stringBuilder.append(i);
            stringBuilder.append("]=");
            stringBuilder.append(((Registrant) this.mImsiReadyRegistrants.get(i)).getHandler());
            pw.println(stringBuilder.toString());
        }
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append(" mRecordsEventsRegistrants: size=");
        stringBuilder4.append(this.mRecordsEventsRegistrants.size());
        pw.println(stringBuilder4.toString());
        for (i = 0; i < this.mRecordsEventsRegistrants.size(); i++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mRecordsEventsRegistrants[");
            stringBuilder.append(i);
            stringBuilder.append("]=");
            stringBuilder.append(((Registrant) this.mRecordsEventsRegistrants.get(i)).getHandler());
            pw.println(stringBuilder.toString());
        }
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append(" mNewSmsRegistrants: size=");
        stringBuilder4.append(this.mNewSmsRegistrants.size());
        pw.println(stringBuilder4.toString());
        for (i = 0; i < this.mNewSmsRegistrants.size(); i++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mNewSmsRegistrants[");
            stringBuilder.append(i);
            stringBuilder.append("]=");
            stringBuilder.append(((Registrant) this.mNewSmsRegistrants.get(i)).getHandler());
            pw.println(stringBuilder.toString());
        }
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append(" mNetworkSelectionModeAutomaticRegistrants: size=");
        stringBuilder4.append(this.mNetworkSelectionModeAutomaticRegistrants.size());
        pw.println(stringBuilder4.toString());
        for (i = 0; i < this.mNetworkSelectionModeAutomaticRegistrants.size(); i++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mNetworkSelectionModeAutomaticRegistrants[");
            stringBuilder.append(i);
            stringBuilder.append("]=");
            stringBuilder.append(((Registrant) this.mNetworkSelectionModeAutomaticRegistrants.get(i)).getHandler());
            pw.println(stringBuilder.toString());
        }
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append(" mRecordsRequested=");
        stringBuilder4.append(this.mRecordsRequested);
        pw.println(stringBuilder4.toString());
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append(" mLockedRecordsReqReason=");
        stringBuilder4.append(this.mLockedRecordsReqReason);
        pw.println(stringBuilder4.toString());
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append(" mRecordsToLoad=");
        stringBuilder4.append(this.mRecordsToLoad);
        pw.println(stringBuilder4.toString());
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append(" mRdnCache=");
        stringBuilder4.append(this.mAdnCache);
        pw.println(stringBuilder4.toString());
        String iccIdToPrint = SubscriptionInfo.givePrintableIccid(this.mFullIccId);
        stringBuilder = new StringBuilder();
        stringBuilder.append(" iccid=");
        stringBuilder.append(iccIdToPrint);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mMsisdn=");
        stringBuilder.append(Rlog.pii(false, this.mMsisdn));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mMsisdnTag=");
        stringBuilder.append(this.mMsisdnTag);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mVoiceMailNum=");
        stringBuilder.append(Rlog.pii(false, this.mVoiceMailNum));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mVoiceMailTag=");
        stringBuilder.append(this.mVoiceMailTag);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mNewVoiceMailNum=");
        stringBuilder.append(Rlog.pii(false, this.mNewVoiceMailNum));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mNewVoiceMailTag=");
        stringBuilder.append(this.mNewVoiceMailTag);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mIsVoiceMailFixed=");
        stringBuilder.append(this.mIsVoiceMailFixed);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mImsi=");
        if (this.mImsi != null) {
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append(this.mImsi.substring(0, 6));
            stringBuilder5.append(Rlog.pii(false, this.mImsi.substring(6)));
            stringBuilder2 = stringBuilder5.toString();
        } else {
            stringBuilder2 = "null";
        }
        stringBuilder.append(stringBuilder2);
        pw.println(stringBuilder.toString());
        if (this.mCarrierTestOverride.isInTestMode()) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" mFakeImsi=");
            stringBuilder3.append(this.mCarrierTestOverride.getFakeIMSI());
            pw.println(stringBuilder3.toString());
        }
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mMncLength=");
        stringBuilder3.append(this.mMncLength);
        pw.println(stringBuilder3.toString());
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mMailboxIndex=");
        stringBuilder3.append(this.mMailboxIndex);
        pw.println(stringBuilder3.toString());
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mSpn=");
        stringBuilder3.append(this.mSpn);
        pw.println(stringBuilder3.toString());
        if (this.mCarrierTestOverride.isInTestMode()) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" mFakeSpn=");
            stringBuilder3.append(this.mCarrierTestOverride.getFakeSpn());
            pw.println(stringBuilder3.toString());
        }
        pw.flush();
    }

    public String getCdmaGsmImsi() {
        return null;
    }

    public void disableRequestIccRecords() {
        this.mRecordsRequested = false;
    }
}
