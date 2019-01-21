package com.android.internal.telephony.uicc;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.telephony.Rlog;
import android.util.Log;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;

public class HwVSimIccCardProxy extends AbstractIccCardProxy {
    private static final int EVENT_APP_READY = 5;
    private static final int EVENT_ICC_CHANGED = 3;
    private static final int EVENT_RADIO_OFF_OR_UNAVAILABLE = 1;
    private static final int EVENT_RADIO_ON = 2;
    private static boolean HWDBG = false;
    private static boolean HWFLOW = false;
    private static final String LOG_TAG = "VSimIccCardProxy";
    private CommandsInterface mCi;
    private Context mContext;
    private int mCurrentAppType = 1;
    private State mExternalState = State.UNKNOWN;
    private final Handler mHandler = new VsimIccCardProxyHandler(this);
    private IccRecords mIccRecords = null;
    private boolean mInitialized = false;
    private final Object mLock = new Object();
    private boolean mRadioOn = false;
    private UiccCardApplication mUiccApplication = null;
    private UiccCard mUiccCard = null;
    private HwVSimUiccController mUiccController = null;
    private UiccSlot mUiccSlot = null;

    /* renamed from: com.android.internal.telephony.uicc.HwVSimIccCardProxy$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$IccCardConstants$State = new int[State.values().length];
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState = new int[AppState.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[State.ABSENT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[State.PIN_REQUIRED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[State.PUK_REQUIRED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[State.NETWORK_LOCKED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[State.READY.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[State.NOT_READY.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[State.PERM_DISABLED.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[State.CARD_IO_ERROR.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[State.CARD_RESTRICTED.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[State.LOADED.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState[AppState.APPSTATE_UNKNOWN.ordinal()] = 1;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState[AppState.APPSTATE_READY.ordinal()] = 2;
            } catch (NoSuchFieldError e12) {
            }
        }
    }

    private static class VsimIccCardProxyHandler extends Handler {
        WeakReference<HwVSimIccCardProxy> mWeakReference;

        VsimIccCardProxyHandler(HwVSimIccCardProxy hwVSimIccCardProxy) {
            this.mWeakReference = new WeakReference(hwVSimIccCardProxy);
        }

        public void handleMessage(Message msg) {
            HwVSimIccCardProxy mVSimIccCardProxy = (HwVSimIccCardProxy) this.mWeakReference.get();
            if (mVSimIccCardProxy == null) {
                HwVSimIccCardProxy.slogi("mVSimIccCardProxy is null, return.");
                return;
            }
            int i = msg.what;
            if (i != 5) {
                switch (i) {
                    case 1:
                        HwVSimIccCardProxy.slogi("EVENT_RADIO_OFF_OR_UNAVAILABLE");
                        mVSimIccCardProxy.mRadioOn = false;
                        if (RadioState.RADIO_UNAVAILABLE != mVSimIccCardProxy.mCi.getRadioState()) {
                            mVSimIccCardProxy.setExternalState(State.ABSENT);
                            break;
                        } else {
                            mVSimIccCardProxy.setExternalState(State.NOT_READY);
                            break;
                        }
                    case 2:
                        HwVSimIccCardProxy.slogi("EVENT_RADIO_ON");
                        mVSimIccCardProxy.mRadioOn = true;
                        if (!mVSimIccCardProxy.mInitialized) {
                            mVSimIccCardProxy.mInitialized = true;
                            sendMessage(obtainMessage(3));
                            break;
                        }
                        break;
                    case 3:
                        HwVSimIccCardProxy.slogi("EVENT_ICC_CHANGED");
                        if (mVSimIccCardProxy.mInitialized) {
                            mVSimIccCardProxy.updateIccAvailability();
                            break;
                        }
                        break;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unhandled message with number: ");
                        stringBuilder.append(msg.what);
                        HwVSimIccCardProxy.slogi(stringBuilder.toString());
                        break;
                }
            }
            HwVSimIccCardProxy.slogi("EVENT_APP_READY");
            mVSimIccCardProxy.setExternalState(State.READY);
        }
    }

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(LOG_TAG, 3));
        HWDBG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(LOG_TAG, 4)))) {
            z = false;
        }
        HWFLOW = z;
    }

    public HwVSimIccCardProxy(Context context, CommandsInterface ci) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ctor: ci=");
        stringBuilder.append(ci);
        logd(stringBuilder.toString());
        this.mContext = context;
        this.mCi = ci;
        this.mUiccController = HwVSimUiccController.getInstance();
        this.mUiccController.registerForIccChanged(this.mHandler, 3, null);
        this.mCi.registerForOn(this.mHandler, 2, null);
        this.mCi.registerForOffOrNotAvailable(this.mHandler, 1, null);
        setExternalState(State.NOT_READY, false);
    }

    public void dispose() {
        synchronized (this.mLock) {
            logd("Disposing");
            this.mUiccController.unregisterForIccChanged(this.mHandler);
            this.mUiccController = null;
        }
    }

    private void updateIccAvailability() {
        logd("updateIccAvailability");
        synchronized (this.mLock) {
            UiccSlot newSlot = this.mUiccController.getUiccSlot();
            UiccCard newCard = this.mUiccController.getUiccCard();
            UiccCardApplication newApp = null;
            IccRecords newRecords = null;
            if (newCard != null) {
                newApp = newCard.getApplication(this.mCurrentAppType);
                if (newApp != null) {
                    newRecords = newApp.getIccRecords();
                }
            }
            if (!(this.mIccRecords == newRecords && this.mUiccApplication == newApp && this.mUiccCard == newCard && this.mUiccSlot == newSlot)) {
                logi("Icc changed. Reregestering.");
                unregisterUiccCardEvents();
                this.mUiccSlot = newSlot;
                this.mUiccCard = newCard;
                this.mUiccApplication = newApp;
                this.mIccRecords = newRecords;
                registerUiccCardEvents();
            }
            updateExternalState();
        }
    }

    private void updateExternalState() {
        logd("updateExternalState");
        if (this.mUiccCard == null || this.mUiccCard.getCardState() == CardState.CARDSTATE_ABSENT) {
            if ((this.mRadioOn || !isAirplaneMode()) && this.mUiccCard != null) {
                logd("Radio on or airplanemode off, set state to ABSENT");
                setExternalState(State.ABSENT);
            } else {
                logd("Radio off or uicccard is null, set state to NOT_READY");
                setExternalState(State.NOT_READY);
            }
        } else if (this.mUiccCard.getCardState() == CardState.CARDSTATE_ERROR) {
            setExternalState(State.CARD_IO_ERROR);
        } else if (this.mUiccApplication == null) {
            logd("mUiccApplication not found");
            setExternalState(State.NOT_READY);
        } else {
            switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState[this.mUiccApplication.getState().ordinal()]) {
                case 1:
                    logd("APPSTATE_UNKNOWN");
                    setExternalState(State.UNKNOWN);
                    break;
                case 2:
                    logd("APPSTATE_READY");
                    setExternalState(State.READY);
                    break;
            }
        }
    }

    private void registerUiccCardEvents() {
        if (this.mUiccApplication != null) {
            this.mUiccApplication.registerForReady(this.mHandler, 5, null);
        }
    }

    private void unregisterUiccCardEvents() {
        if (this.mUiccApplication != null) {
            this.mUiccApplication.unregisterForReady(this.mHandler);
        }
    }

    private boolean isAirplaneMode() {
        return System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0;
    }

    private String getIccStateIntentString(State state) {
        switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()]) {
            case 1:
                return "ABSENT";
            case 2:
                return "LOCKED";
            case 3:
                return "LOCKED";
            case 4:
                return "LOCKED";
            case 5:
                return "READY";
            case 6:
                return "NOT_READY";
            case 7:
                return "LOCKED";
            case 8:
                return "CARD_IO_ERROR";
            case 9:
                return "CARD_RESTRICTED";
            case 10:
                return "LOADED";
            default:
                return "UNKNOWN";
        }
    }

    private String getIccStateReason(State state) {
        switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()]) {
            case 2:
                return "PIN";
            case 3:
                return "PUK";
            case 4:
                return "NETWORK";
            case 7:
                return "PERM_DISABLED";
            case 8:
                return "CARD_IO_ERROR";
            case 9:
                return "CARD_RESTRICTED";
            default:
                return null;
        }
    }

    public void broadcastIccStateChangedIntent(State state) {
        broadcastIccStateChangedIntent(getIccStateIntentString(state), getIccStateReason(state));
    }

    private void broadcastIccStateChangedIntent(String value, String reason) {
        synchronized (this.mLock) {
            Intent intent = new Intent("com.huawei.vsim.action.VSIM_STATE_CHANGED");
            intent.addFlags(67108864);
            intent.putExtra("phoneName", "Phone");
            intent.putExtra("ss", value);
            intent.putExtra("reason", reason);
            intent.putExtra("subscription", 2);
            intent.putExtra("phone", 2);
            intent.putExtra("slot", 2);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("broadcastIccStateChangedIntent intent ACTION_VSIM_STATE_CHANGED value=");
            stringBuilder.append(value);
            stringBuilder.append(" reason=");
            stringBuilder.append(reason);
            stringBuilder.append(" for VSIM");
            logi(stringBuilder.toString());
            ActivityManagerNative.broadcastStickyIntent(intent, "android.permission.READ_PHONE_STATE", -1);
        }
    }

    private void setExternalState(State newState, boolean override) {
        synchronized (this.mLock) {
            StringBuilder stringBuilder;
            if (!override) {
                try {
                    if (newState == this.mExternalState) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("setExternalState: !override and newstate unchanged from ");
                        stringBuilder.append(newState);
                        loge(stringBuilder.toString());
                        return;
                    }
                } catch (Throwable th) {
                }
            }
            this.mExternalState = newState;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setExternalState: mExternalState=");
            stringBuilder.append(this.mExternalState);
            logi(stringBuilder.toString());
            SystemProperties.set("gsm.vsim.state", getState().toString());
            broadcastIccStateChangedIntent(getIccStateIntentString(this.mExternalState), getIccStateReason(this.mExternalState));
        }
    }

    private void setExternalState(State newState) {
        setExternalState(newState, false);
    }

    public State getState() {
        State state;
        synchronized (this.mLock) {
            state = this.mExternalState;
        }
        return state;
    }

    public IccRecords getIccRecords() {
        synchronized (this.mLock) {
        }
        return null;
    }

    public void registerForNetworkLocked(Handler h, int what, Object obj) {
    }

    public void unregisterForNetworkLocked(Handler h) {
    }

    public void supplyPin(String pin, Message onComplete) {
        loge("supplyPin not supported by vsim");
        synchronized (this.mLock) {
            if (onComplete != null) {
                try {
                    AsyncResult.forMessage(onComplete).exception = new RuntimeException("ICC card is absent.");
                    onComplete.sendToTarget();
                } catch (Throwable th) {
                }
            }
        }
    }

    public void supplyPuk(String puk, String newPin, Message onComplete) {
    }

    public void supplyPin2(String pin2, Message onComplete) {
    }

    public void supplyPuk2(String puk2, String newPin2, Message onComplete) {
    }

    public void supplyNetworkDepersonalization(String pin, Message onComplete) {
    }

    public boolean getIccLockEnabled() {
        return false;
    }

    public boolean getIccFdnEnabled() {
        return false;
    }

    public boolean getIccFdnAvailable() {
        return false;
    }

    public boolean getIccPin2Blocked() {
        return false;
    }

    public boolean getIccPuk2Blocked() {
        return false;
    }

    public void setIccLockEnabled(boolean enabled, String password, Message onComplete) {
    }

    public void setIccFdnEnabled(boolean enabled, String password, Message onComplete) {
    }

    public void changeIccLockPassword(String oldPassword, String newPassword, Message onComplete) {
    }

    public void changeIccFdnPassword(String oldPassword, String newPassword, Message onComplete) {
    }

    public String getServiceProviderName() {
        synchronized (this.mLock) {
        }
        return null;
    }

    public boolean isApplicationOnIcc(AppType type) {
        return false;
    }

    public boolean hasIccCard() {
        boolean z;
        synchronized (this.mLock) {
            z = (this.mUiccCard == null || this.mUiccCard.getCardState() == CardState.CARDSTATE_ABSENT) ? false : true;
        }
        return z;
    }

    private void logd(String s) {
        if (HWDBG) {
            Rlog.d(LOG_TAG, s);
        }
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    private void logi(String msg) {
        if (HWFLOW) {
            Rlog.i(LOG_TAG, msg);
        }
    }

    private static void slogi(String msg) {
        if (HWFLOW) {
            Rlog.i(LOG_TAG, msg);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("VSimIccCardProxy: ");
        stringBuilder.append(this);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mContext=");
        stringBuilder.append(this.mContext);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCi=");
        stringBuilder.append(this.mCi);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mUiccController=");
        stringBuilder.append(this.mUiccController);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mUiccCard=");
        stringBuilder.append(this.mUiccCard);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mUiccApplication=");
        stringBuilder.append(this.mUiccApplication);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mIccRecords=");
        stringBuilder.append(this.mIccRecords);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mExternalState=");
        stringBuilder.append(this.mExternalState);
        pw.println(stringBuilder.toString());
        pw.flush();
    }

    public void supplyDepersonalization(String pin, int type, Message onComplete) {
    }
}
