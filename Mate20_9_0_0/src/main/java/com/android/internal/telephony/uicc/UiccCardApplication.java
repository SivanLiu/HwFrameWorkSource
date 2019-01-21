package com.android.internal.telephony.uicc;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.PersoSubState;
import com.android.internal.telephony.uicc.IccCardStatus.PinState;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class UiccCardApplication {
    public static final int AUTH_CONTEXT_EAP_AKA = 129;
    public static final int AUTH_CONTEXT_EAP_SIM = 128;
    public static final int AUTH_CONTEXT_UNDEFINED = -1;
    private static final boolean DBG = true;
    private static final int EVENT_CHANGE_FACILITY_FDN_DONE = 5;
    private static final int EVENT_CHANGE_FACILITY_LOCK_DONE = 7;
    private static final int EVENT_CHANGE_PIN1_DONE = 2;
    private static final int EVENT_CHANGE_PIN2_DONE = 3;
    private static final int EVENT_PIN1_PUK1_DONE = 1;
    private static final int EVENT_PIN2_PUK2_DONE = 8;
    private static final int EVENT_QUERY_FACILITY_FDN_DONE = 4;
    private static final int EVENT_QUERY_FACILITY_LOCK_DONE = 6;
    private static final int EVENT_RADIO_UNAVAILABLE = 9;
    private static final String LOG_TAG = "UiccCardApplication";
    private String mAid;
    private String mAppLabel;
    private AppState mAppState;
    private AppType mAppType;
    private int mAuthContext;
    private CommandsInterface mCi;
    private Context mContext;
    private boolean mDesiredFdnEnabled;
    private boolean mDesiredPinLocked;
    private boolean mDestroyed;
    private RegistrantList mFdnStatusChangeRegistrants;
    private RegistrantList mGetAdDoneRegistrants;
    private Handler mHandler;
    private boolean mIccFdnAvailable;
    private boolean mIccFdnEnabled;
    private IccFileHandler mIccFh;
    private boolean mIccLockEnabled;
    private IccRecords mIccRecords;
    private boolean mIgnoreApp;
    private final Object mLock = new Object();
    private RegistrantList mNetworkLockedRegistrants;
    private PersoSubState mPersoSubState;
    private boolean mPin1Replaced;
    private PinState mPin1State;
    private PinState mPin2State;
    private RegistrantList mPinLockedRegistrants;
    private RegistrantList mReadyRegistrants;
    private UiccProfile mUiccProfile;

    public UiccCardApplication(UiccProfile uiccProfile, IccCardApplicationStatus as, Context c, CommandsInterface ci) {
        boolean z = true;
        this.mIccFdnAvailable = true;
        this.mReadyRegistrants = new RegistrantList();
        this.mPinLockedRegistrants = new RegistrantList();
        this.mNetworkLockedRegistrants = new RegistrantList();
        this.mGetAdDoneRegistrants = new RegistrantList();
        this.mFdnStatusChangeRegistrants = new RegistrantList();
        this.mHandler = new Handler() {
            public void handleMessage(Message msg) {
                UiccCardApplication uiccCardApplication;
                StringBuilder stringBuilder;
                if (UiccCardApplication.this.mDestroyed) {
                    if (msg.what == 1) {
                        uiccCardApplication = UiccCardApplication.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Received message ");
                        stringBuilder.append(msg);
                        stringBuilder.append("[");
                        stringBuilder.append(msg.what);
                        stringBuilder.append("] while being destroyed. continue for PIN.");
                        uiccCardApplication.loge(stringBuilder.toString());
                    } else {
                        uiccCardApplication = UiccCardApplication.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Received message ");
                        stringBuilder.append(msg);
                        stringBuilder.append("[");
                        stringBuilder.append(msg.what);
                        stringBuilder.append("] while being destroyed. Ignoring.");
                        uiccCardApplication.loge(stringBuilder.toString());
                        return;
                    }
                }
                switch (msg.what) {
                    case 1:
                    case 2:
                    case 3:
                    case 8:
                        AsyncResult ar = (AsyncResult) msg.obj;
                        int attemptsRemaining = UiccCardApplication.this.parsePinPukErrorResult(ar);
                        Message response = ar.userObj;
                        AsyncResult.forMessage(response).exception = ar.exception;
                        response.arg1 = attemptsRemaining;
                        response.sendToTarget();
                        break;
                    case 4:
                        UiccCardApplication.this.onQueryFdnEnabled((AsyncResult) msg.obj);
                        break;
                    case 5:
                        UiccCardApplication.this.onChangeFdnDone((AsyncResult) msg.obj);
                        break;
                    case 6:
                        UiccCardApplication.this.onQueryFacilityLock((AsyncResult) msg.obj);
                        break;
                    case 7:
                        UiccCardApplication.this.onChangeFacilityLock(msg.obj);
                        break;
                    case 9:
                        UiccCardApplication.this.log("handleMessage (EVENT_RADIO_UNAVAILABLE)");
                        UiccCardApplication.this.mAppState = AppState.APPSTATE_UNKNOWN;
                        break;
                    default:
                        uiccCardApplication = UiccCardApplication.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown Event ");
                        stringBuilder.append(msg.what);
                        uiccCardApplication.loge(stringBuilder.toString());
                        break;
                }
            }
        };
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Creating UiccApp: ");
        stringBuilder.append(as);
        log(stringBuilder.toString());
        this.mUiccProfile = uiccProfile;
        this.mAppState = as.app_state;
        this.mAppType = as.app_type;
        this.mAuthContext = getAuthContext(this.mAppType);
        this.mPersoSubState = as.perso_substate;
        this.mAid = as.aid;
        this.mAppLabel = as.app_label;
        if (as.pin1_replaced == 0) {
            z = false;
        }
        this.mPin1Replaced = z;
        this.mPin1State = as.pin1;
        this.mPin2State = as.pin2;
        this.mIgnoreApp = false;
        this.mContext = c;
        this.mCi = ci;
        this.mIccFh = createIccFileHandler(as.app_type);
        this.mIccRecords = createIccRecords(as.app_type, this.mContext, this.mCi);
        if (this.mAppState == AppState.APPSTATE_READY) {
            queryFdn();
            queryPin1State();
        }
        this.mCi.registerForNotAvailable(this.mHandler, 9, null);
    }

    /* JADX WARNING: Missing block: B:48:0x00f3, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void update(IccCardApplicationStatus as, Context c, CommandsInterface ci) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                loge("Application updated after destroyed! Fix me!");
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mAppType);
            stringBuilder.append(" update. New ");
            stringBuilder.append(as);
            log(stringBuilder.toString());
            this.mContext = c;
            this.mCi = ci;
            AppType oldAppType = this.mAppType;
            AppState oldAppState = this.mAppState;
            PersoSubState oldPersoSubState = this.mPersoSubState;
            this.mAppType = as.app_type;
            this.mAuthContext = getAuthContext(this.mAppType);
            this.mAppState = as.app_state;
            this.mPersoSubState = as.perso_substate;
            this.mAid = as.aid;
            this.mAppLabel = as.app_label;
            this.mPin1Replaced = as.pin1_replaced != 0;
            this.mPin1State = as.pin1;
            this.mPin2State = as.pin2;
            if (this.mAppType != oldAppType) {
                if (this.mIccFh != null) {
                    this.mIccFh.dispose();
                }
                if (this.mIccRecords != null) {
                    this.mIccRecords.dispose();
                }
                this.mIccFh = createIccFileHandler(as.app_type);
                this.mIccRecords = createIccRecords(as.app_type, c, ci);
            }
            if (this.mPersoSubState != oldPersoSubState && this.mPersoSubState == PersoSubState.PERSOSUBSTATE_SIM_NETWORK) {
                notifyNetworkLockedRegistrantsIfNeeded(null);
            }
            if (this.mAppState != oldAppState) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(oldAppType);
                stringBuilder2.append(" changed state: ");
                stringBuilder2.append(oldAppState);
                stringBuilder2.append(" -> ");
                stringBuilder2.append(this.mAppState);
                log(stringBuilder2.toString());
                if (this.mAppState == AppState.APPSTATE_READY) {
                    queryFdn();
                    queryPin1State();
                }
                if ((oldAppState == AppState.APPSTATE_READY && this.mAppState == AppState.APPSTATE_DETECTED && this.mIccRecords != null && HwModemCapability.isCapabilitySupport(9)) || (AppState.APPSTATE_PUK == oldAppState && AppState.APPSTATE_READY == this.mAppState && this.mIccRecords != null)) {
                    this.mIccRecords.disableRequestIccRecords();
                }
                notifyPinLockedRegistrantsIfNeeded(null);
                notifyReadyRegistrantsIfNeeded(null);
            }
        }
    }

    void dispose() {
        synchronized (this.mLock) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mAppType);
            stringBuilder.append(" being Disposed");
            log(stringBuilder.toString());
            this.mDestroyed = true;
            if (this.mIccRecords != null) {
                this.mIccRecords.dispose();
            }
            if (this.mIccFh != null) {
                this.mIccFh.dispose();
            }
            this.mIccRecords = null;
            this.mIccFh = null;
            this.mCi.unregisterForNotAvailable(this.mHandler);
        }
    }

    private IccRecords createIccRecords(AppType type, Context c, CommandsInterface ci) {
        if (type == AppType.APPTYPE_USIM || type == AppType.APPTYPE_SIM) {
            return HwTelephonyFactory.getHwUiccManager().createHwSIMRecords(this, c, ci);
        }
        if (type == AppType.APPTYPE_RUIM || type == AppType.APPTYPE_CSIM) {
            return HwTelephonyFactory.getHwUiccManager().createHwRuimRecords(this, c, ci);
        }
        if (type == AppType.APPTYPE_ISIM) {
            return new IsimUiccRecords(this, c, ci);
        }
        return null;
    }

    private IccFileHandler createIccFileHandler(AppType type) {
        switch (type) {
            case APPTYPE_SIM:
                return new SIMFileHandler(this, this.mAid, this.mCi);
            case APPTYPE_RUIM:
                return new RuimFileHandler(this, this.mAid, this.mCi);
            case APPTYPE_USIM:
                return new UsimFileHandler(this, this.mAid, this.mCi);
            case APPTYPE_CSIM:
                return new CsimFileHandler(this, this.mAid, this.mCi);
            case APPTYPE_ISIM:
                return new IsimFileHandler(this, this.mAid, this.mCi);
            default:
                return null;
        }
    }

    public void queryFdn() {
        this.mCi.queryFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_FD, "", 7, this.mAid, this.mHandler.obtainMessage(4));
    }

    private void onQueryFdnEnabled(AsyncResult ar) {
        synchronized (this.mLock) {
            if (ar.exception != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error in querying facility lock:");
                stringBuilder.append(ar.exception);
                log(stringBuilder.toString());
                return;
            }
            int[] result = ar.result;
            if (result == null || result.length == 0) {
                loge("Bogus facility lock response");
            } else {
                boolean z = false;
                if (result[0] == 2) {
                    this.mIccFdnEnabled = false;
                    this.mIccFdnAvailable = false;
                } else {
                    if (result[0] == 1) {
                        z = true;
                    }
                    this.mIccFdnEnabled = z;
                    this.mIccFdnAvailable = true;
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Query facility FDN : FDN service available: ");
                stringBuilder2.append(this.mIccFdnAvailable);
                stringBuilder2.append(" enabled: ");
                stringBuilder2.append(this.mIccFdnEnabled);
                log(stringBuilder2.toString());
            }
            notifyFdnStatusChange();
        }
    }

    private void onChangeFdnDone(AsyncResult ar) {
        synchronized (this.mLock) {
            int attemptsRemaining = -1;
            StringBuilder stringBuilder;
            if (ar.exception == null) {
                this.mIccFdnEnabled = this.mDesiredFdnEnabled;
                stringBuilder = new StringBuilder();
                stringBuilder.append("EVENT_CHANGE_FACILITY_FDN_DONE: mIccFdnEnabled=");
                stringBuilder.append(this.mIccFdnEnabled);
                log(stringBuilder.toString());
            } else {
                attemptsRemaining = parsePinPukErrorResult(ar);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error change facility fdn with exception ");
                stringBuilder.append(ar.exception);
                loge(stringBuilder.toString());
            }
            Message response = ar.userObj;
            response.arg1 = attemptsRemaining;
            AsyncResult.forMessage(response).exception = ar.exception;
            response.sendToTarget();
            notifyFdnStatusChange();
        }
    }

    private void queryPin1State() {
        this.mCi.queryFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_SIM, "", 7, this.mAid, this.mHandler.obtainMessage(6));
    }

    /* JADX WARNING: Missing block: B:30:0x008f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void onQueryFacilityLock(AsyncResult ar) {
        synchronized (this.mLock) {
            if (ar.exception != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error in querying facility lock:");
                stringBuilder.append(ar.exception);
                log(stringBuilder.toString());
                return;
            }
            int[] ints = ar.result;
            if (ints == null || ints.length == 0) {
                loge("Bogus facility lock response");
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Query facility lock : ");
                boolean z = false;
                stringBuilder2.append(ints[0]);
                log(stringBuilder2.toString());
                if (ints[0] != 0) {
                    z = true;
                }
                this.mIccLockEnabled = z;
                if (this.mIccLockEnabled) {
                    this.mPinLockedRegistrants.notifyRegistrants();
                }
                switch (this.mPin1State) {
                    case PINSTATE_DISABLED:
                        if (this.mIccLockEnabled) {
                            loge("QUERY_FACILITY_LOCK:enabled GET_SIM_STATUS.Pin1:disabled. Fixme");
                            break;
                        }
                        break;
                    case PINSTATE_ENABLED_NOT_VERIFIED:
                    case PINSTATE_ENABLED_VERIFIED:
                    case PINSTATE_ENABLED_BLOCKED:
                    case PINSTATE_ENABLED_PERM_BLOCKED:
                        if (!this.mIccLockEnabled) {
                            loge("QUERY_FACILITY_LOCK:disabled GET_SIM_STATUS.Pin1:enabled. Fixme");
                            break;
                        }
                        break;
                    default:
                        break;
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Ignoring: pin1state=");
                stringBuilder2.append(this.mPin1State);
                log(stringBuilder2.toString());
            }
        }
    }

    private void onChangeFacilityLock(AsyncResult ar) {
        synchronized (this.mLock) {
            int attemptsRemaining = -1;
            StringBuilder stringBuilder;
            if (ar.exception == null) {
                this.mIccLockEnabled = this.mDesiredPinLocked;
                stringBuilder = new StringBuilder();
                stringBuilder.append("EVENT_CHANGE_FACILITY_LOCK_DONE: mIccLockEnabled= ");
                stringBuilder.append(this.mIccLockEnabled);
                log(stringBuilder.toString());
            } else {
                attemptsRemaining = parsePinPukErrorResult(ar);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error change facility lock with exception ");
                stringBuilder.append(ar.exception);
                loge(stringBuilder.toString());
            }
            Message response = ar.userObj;
            AsyncResult.forMessage(response).exception = ar.exception;
            response.arg1 = attemptsRemaining;
            response.sendToTarget();
        }
    }

    private int parsePinPukErrorResult(AsyncResult ar) {
        int[] result = ar.result;
        if (result == null) {
            return -1;
        }
        int attemptsRemaining = -1;
        if (result.length > 0) {
            attemptsRemaining = result[0];
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("parsePinPukErrorResult: attemptsRemaining=");
        stringBuilder.append(attemptsRemaining);
        log(stringBuilder.toString());
        return attemptsRemaining;
    }

    public void registerForReady(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            int i = this.mReadyRegistrants.size() - 1;
            while (i >= 0) {
                Handler rH = ((Registrant) this.mReadyRegistrants.get(i)).getHandler();
                if (rH == null || rH != h) {
                    i--;
                } else {
                    return;
                }
            }
            Registrant r = new Registrant(h, what, obj);
            this.mReadyRegistrants.add(r);
            notifyReadyRegistrantsIfNeeded(r);
        }
    }

    public void unregisterForReady(Handler h) {
        synchronized (this.mLock) {
            this.mReadyRegistrants.remove(h);
        }
    }

    public void registerForGetAdDone(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mGetAdDoneRegistrants.add(r);
            if (this.mIccRecords.getImsiReady() && AppState.APPSTATE_READY == getState()) {
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    public void unregisterForGetAdDone(Handler h) {
        synchronized (this.mLock) {
            this.mGetAdDoneRegistrants.remove(h);
        }
    }

    public void notifyGetAdDone(Registrant r) {
        if (!this.mDestroyed) {
            log("Notifying registrants: notifyGetAdDone");
            if (r == null) {
                log("Notifying registrants: notifyGetAdDone");
                this.mGetAdDoneRegistrants.notifyRegistrants();
            } else {
                log("Notifying 1 registrant: notifyGetAdDone");
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    protected void registerForLocked(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mPinLockedRegistrants.add(r);
            notifyPinLockedRegistrantsIfNeeded(r);
        }
    }

    protected void unregisterForLocked(Handler h) {
        synchronized (this.mLock) {
            this.mPinLockedRegistrants.remove(h);
        }
    }

    protected void registerForNetworkLocked(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mNetworkLockedRegistrants.add(r);
            notifyNetworkLockedRegistrantsIfNeeded(r);
        }
    }

    protected void unregisterForNetworkLocked(Handler h) {
        synchronized (this.mLock) {
            this.mNetworkLockedRegistrants.remove(h);
        }
    }

    public void registerForFdnStatusChange(Handler h, int what, Object obj) {
        if (HuaweiTelephonyConfigs.isPsRestrictedByFdn()) {
            synchronized (this.mLock) {
                this.mFdnStatusChangeRegistrants.add(new Registrant(h, what, obj));
            }
        }
    }

    public void unregisterForFdnStatusChange(Handler h) {
        if (HuaweiTelephonyConfigs.isPsRestrictedByFdn()) {
            synchronized (this.mLock) {
                this.mFdnStatusChangeRegistrants.remove(h);
            }
        }
    }

    public void notifyFdnStatusChange() {
        if (HuaweiTelephonyConfigs.isPsRestrictedByFdn()) {
            synchronized (this.mLock) {
                this.mFdnStatusChangeRegistrants.notifyRegistrants();
            }
        }
    }

    private void notifyReadyRegistrantsIfNeeded(Registrant r) {
        if (!this.mDestroyed && this.mAppState == AppState.APPSTATE_READY) {
            if (this.mPin1State == PinState.PINSTATE_ENABLED_NOT_VERIFIED || this.mPin1State == PinState.PINSTATE_ENABLED_BLOCKED || this.mPin1State == PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
                loge("Sanity check failed! APPSTATE is ready while PIN1 is not verified!!!");
            } else if (r == null) {
                log("Notifying registrants: READY");
                this.mReadyRegistrants.notifyRegistrants();
            } else {
                log("Notifying 1 registrant: READY");
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    private void notifyPinLockedRegistrantsIfNeeded(Registrant r) {
        if (!this.mDestroyed) {
            if (this.mAppState == AppState.APPSTATE_PIN || this.mAppState == AppState.APPSTATE_PUK) {
                if (this.mPin1State == PinState.PINSTATE_ENABLED_VERIFIED || this.mPin1State == PinState.PINSTATE_DISABLED) {
                    loge("Sanity check failed! APPSTATE is locked while PIN1 is not!!!");
                } else if (r == null) {
                    log("Notifying registrants: LOCKED");
                    this.mPinLockedRegistrants.notifyRegistrants();
                } else {
                    log("Notifying 1 registrant: LOCKED");
                    r.notifyRegistrant(new AsyncResult(null, null, null));
                }
            }
        }
    }

    private void notifyNetworkLockedRegistrantsIfNeeded(Registrant r) {
        if (!this.mDestroyed && this.mAppState == AppState.APPSTATE_SUBSCRIPTION_PERSO && this.mPersoSubState == PersoSubState.PERSOSUBSTATE_SIM_NETWORK) {
            if (r == null) {
                log("Notifying registrants: NETWORK_LOCKED");
                this.mNetworkLockedRegistrants.notifyRegistrants();
            } else {
                log("Notifying 1 registrant: NETWORK_LOCED");
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    public AppState getState() {
        AppState appState;
        synchronized (this.mLock) {
            appState = this.mAppState;
        }
        return appState;
    }

    public AppType getType() {
        AppType appType;
        synchronized (this.mLock) {
            appType = this.mAppType;
        }
        return appType;
    }

    public int getAuthContext() {
        int i;
        synchronized (this.mLock) {
            i = this.mAuthContext;
        }
        return i;
    }

    private static int getAuthContext(AppType appType) {
        int i = AnonymousClass2.$SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[appType.ordinal()];
        if (i == 1) {
            return 128;
        }
        if (i != 3) {
            return -1;
        }
        return 129;
    }

    public PersoSubState getPersoSubState() {
        PersoSubState persoSubState;
        synchronized (this.mLock) {
            persoSubState = this.mPersoSubState;
        }
        return persoSubState;
    }

    public String getAid() {
        String str;
        synchronized (this.mLock) {
            str = this.mAid;
        }
        return str;
    }

    public String getAppLabel() {
        return this.mAppLabel;
    }

    public PinState getPin1State() {
        synchronized (this.mLock) {
            PinState universalPinState;
            if (this.mPin1Replaced) {
                universalPinState = this.mUiccProfile.getUniversalPinState();
                return universalPinState;
            }
            universalPinState = this.mPin1State;
            return universalPinState;
        }
    }

    public IccFileHandler getIccFileHandler() {
        IccFileHandler iccFileHandler;
        synchronized (this.mLock) {
            iccFileHandler = this.mIccFh;
        }
        return iccFileHandler;
    }

    public IccRecords getIccRecords() {
        IccRecords iccRecords;
        synchronized (this.mLock) {
            iccRecords = this.mIccRecords;
        }
        return iccRecords;
    }

    public void supplyPin(String pin, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                log("supplyPin:ICC card is Destroyed.");
                AsyncResult.forMessage(onComplete).exception = new RuntimeException("ICC card is Destroyed.");
                onComplete.sendToTarget();
                return;
            }
            this.mCi.supplyIccPinForApp(pin, this.mAid, this.mHandler.obtainMessage(1, onComplete));
        }
    }

    public void supplyPuk(String puk, String newPin, Message onComplete) {
        synchronized (this.mLock) {
            this.mCi.supplyIccPukForApp(puk, newPin, this.mAid, this.mHandler.obtainMessage(1, onComplete));
        }
    }

    public void supplyPin2(String pin2, Message onComplete) {
        synchronized (this.mLock) {
            this.mCi.supplyIccPin2ForApp(pin2, this.mAid, this.mHandler.obtainMessage(8, onComplete));
        }
    }

    public void supplyPuk2(String puk2, String newPin2, Message onComplete) {
        synchronized (this.mLock) {
            this.mCi.supplyIccPuk2ForApp(puk2, newPin2, this.mAid, this.mHandler.obtainMessage(8, onComplete));
        }
    }

    public void supplyNetworkDepersonalization(String pin, Message onComplete) {
        synchronized (this.mLock) {
            log("supplyNetworkDepersonalization");
            this.mCi.supplyNetworkDepersonalization(pin, onComplete);
        }
    }

    public boolean getIccLockEnabled() {
        return this.mIccLockEnabled;
    }

    public boolean getIccFdnEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIccFdnEnabled;
        }
        return z;
    }

    public boolean getIccFdnAvailable() {
        return this.mIccFdnAvailable;
    }

    public void setIccLockEnabled(boolean enabled, String password, Message onComplete) {
        synchronized (this.mLock) {
            this.mDesiredPinLocked = enabled;
            this.mCi.setFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_SIM, enabled, password, 7, this.mAid, this.mHandler.obtainMessage(7, onComplete));
        }
    }

    public void setIccFdnEnabled(boolean enabled, String password, Message onComplete) {
        synchronized (this.mLock) {
            this.mDesiredFdnEnabled = enabled;
            this.mCi.setFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_FD, enabled, password, 15, this.mAid, this.mHandler.obtainMessage(5, onComplete));
        }
    }

    public void changeIccLockPassword(String oldPassword, String newPassword, Message onComplete) {
        synchronized (this.mLock) {
            log("changeIccLockPassword");
            this.mCi.changeIccPinForApp(oldPassword, newPassword, this.mAid, this.mHandler.obtainMessage(2, onComplete));
        }
    }

    public void changeIccFdnPassword(String oldPassword, String newPassword, Message onComplete) {
        synchronized (this.mLock) {
            log("changeIccFdnPassword");
            this.mCi.changeIccPin2ForApp(oldPassword, newPassword, this.mAid, this.mHandler.obtainMessage(3, onComplete));
        }
    }

    public boolean isReady() {
        synchronized (this.mLock) {
            if (this.mAppState != AppState.APPSTATE_READY) {
                return false;
            }
            if (!(this.mPin1State == PinState.PINSTATE_ENABLED_NOT_VERIFIED || this.mPin1State == PinState.PINSTATE_ENABLED_BLOCKED)) {
                if (this.mPin1State != PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
                    return true;
                }
            }
            loge("Sanity check failed! APPSTATE is ready while PIN1 is not verified!!!");
            return false;
        }
    }

    public boolean getIccPin2Blocked() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mPin2State == PinState.PINSTATE_ENABLED_BLOCKED;
        }
        return z;
    }

    public boolean getIccPuk2Blocked() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mPin2State == PinState.PINSTATE_ENABLED_PERM_BLOCKED;
        }
        return z;
    }

    public int getPhoneId() {
        return this.mUiccProfile.getPhoneId();
    }

    public UiccCard getUiccCard() {
        return this.mUiccProfile.getUiccCardHw();
    }

    public boolean isAppIgnored() {
        return this.mIgnoreApp;
    }

    public void setAppIgnoreState(boolean ignore) {
        this.mIgnoreApp = ignore;
    }

    protected UiccProfile getUiccProfile() {
        return this.mUiccProfile;
    }

    private void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        int i;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("UiccCardApplication: ");
        stringBuilder2.append(this);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mUiccProfile=");
        stringBuilder2.append(this.mUiccProfile);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mAppState=");
        stringBuilder2.append(this.mAppState);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mAppType=");
        stringBuilder2.append(this.mAppType);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mPersoSubState=");
        stringBuilder2.append(this.mPersoSubState);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mAid=");
        stringBuilder2.append(this.mAid);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mAppLabel=");
        stringBuilder2.append(this.mAppLabel);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mPin1Replaced=");
        stringBuilder2.append(this.mPin1Replaced);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mPin1State=");
        stringBuilder2.append(this.mPin1State);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mPin2State=");
        stringBuilder2.append(this.mPin2State);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mIccFdnEnabled=");
        stringBuilder2.append(this.mIccFdnEnabled);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mDesiredFdnEnabled=");
        stringBuilder2.append(this.mDesiredFdnEnabled);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mIccLockEnabled=");
        stringBuilder2.append(this.mIccLockEnabled);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mDesiredPinLocked=");
        stringBuilder2.append(this.mDesiredPinLocked);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mCi=");
        stringBuilder2.append(this.mCi);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mIccRecords=");
        stringBuilder2.append(this.mIccRecords);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mIccFh=");
        stringBuilder2.append(this.mIccFh);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mDestroyed=");
        stringBuilder2.append(this.mDestroyed);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mReadyRegistrants: size=");
        stringBuilder2.append(this.mReadyRegistrants.size());
        pw.println(stringBuilder2.toString());
        int i2 = 0;
        for (i = 0; i < this.mReadyRegistrants.size(); i++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mReadyRegistrants[");
            stringBuilder.append(i);
            stringBuilder.append("]=");
            stringBuilder.append(((Registrant) this.mReadyRegistrants.get(i)).getHandler());
            pw.println(stringBuilder.toString());
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mPinLockedRegistrants: size=");
        stringBuilder3.append(this.mPinLockedRegistrants.size());
        pw.println(stringBuilder3.toString());
        for (i = 0; i < this.mPinLockedRegistrants.size(); i++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mPinLockedRegistrants[");
            stringBuilder.append(i);
            stringBuilder.append("]=");
            stringBuilder.append(((Registrant) this.mPinLockedRegistrants.get(i)).getHandler());
            pw.println(stringBuilder.toString());
        }
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mNetworkLockedRegistrants: size=");
        stringBuilder3.append(this.mNetworkLockedRegistrants.size());
        pw.println(stringBuilder3.toString());
        while (i2 < this.mNetworkLockedRegistrants.size()) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("  mNetworkLockedRegistrants[");
            stringBuilder3.append(i2);
            stringBuilder3.append("]=");
            stringBuilder3.append(((Registrant) this.mNetworkLockedRegistrants.get(i2)).getHandler());
            pw.println(stringBuilder3.toString());
            i2++;
        }
        pw.flush();
    }

    public UiccCard getUiccCardHw() {
        return getUiccCard();
    }
}
