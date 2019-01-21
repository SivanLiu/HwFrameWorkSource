package com.android.internal.telephony.uicc;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.telephony.HwIccCardConstants.HwState;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.AbstractIccCardProxy.IccCardProxyReference;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.PersoSubState;
import com.android.internal.telephony.vsim.HwVSimUtils;
import huawei.android.telephony.wrapper.WrapperFactory;

public class HwIccCardProxyReference extends Handler implements IccCardProxyReference {
    private static final int EVENT_FDN_STATUS_CHANGED = 1;
    private static final int EVENT_ICC_REFRESH = 101;
    public static final String INTENT_KEY_SUBSCRIPTION_MODE = "sub_mode";
    private static final String LOG_TAG = "HwIccCardProxyReference";
    private static final int PHY_SLOT_0 = 0;
    private static IccCardProxyUtils iccCardProxyUtils = new IccCardProxyUtils();
    private boolean mBroadcastForNotCT4GCardDone = false;
    HwState mExternalState = HwState.UNKNOWN;
    private UiccProfile mIccCardProxy;
    boolean mIccCardStateHW;
    private final Object mLock = new Object();
    private RegistrantList mNetworkLockedRegistrants = new RegistrantList();
    private UiccCardApplication mUiccCardApplication = null;

    /* renamed from: com.android.internal.telephony.uicc.HwIccCardProxyReference$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$HwIccCardConstants$HwState = new int[HwState.values().length];
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState = new int[PersoSubState.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_SIM_NETWORK.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_SIM_NETWORK_SUBSET.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_SIM_CORPORATE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_SIM_SERVICE_PROVIDER.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_SIM_SIM.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_SIM_NETWORK_PUK.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_SIM_CORPORATE_PUK.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_SIM_SERVICE_PROVIDER_PUK.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_SIM_SIM_PUK.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_RUIM_NETWORK1.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_RUIM_NETWORK2.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_RUIM_HRPD.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_RUIM_CORPORATE.ordinal()] = 14;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_RUIM_SERVICE_PROVIDER.ordinal()] = 15;
            } catch (NoSuchFieldError e15) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_RUIM_RUIM.ordinal()] = 16;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_RUIM_NETWORK1_PUK.ordinal()] = 17;
            } catch (NoSuchFieldError e17) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_RUIM_NETWORK2_PUK.ordinal()] = 18;
            } catch (NoSuchFieldError e18) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_RUIM_HRPD_PUK.ordinal()] = 19;
            } catch (NoSuchFieldError e19) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_RUIM_CORPORATE_PUK.ordinal()] = 20;
            } catch (NoSuchFieldError e20) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_RUIM_SERVICE_PROVIDER_PUK.ordinal()] = 21;
            } catch (NoSuchFieldError e21) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[PersoSubState.PERSOSUBSTATE_RUIM_RUIM_PUK.ordinal()] = 22;
            } catch (NoSuchFieldError e22) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$HwIccCardConstants$HwState[HwState.NETWORK_LOCKED.ordinal()] = 1;
            } catch (NoSuchFieldError e23) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$HwIccCardConstants$HwState[HwState.SIM_NETWORK_SUBSET_LOCKED.ordinal()] = 2;
            } catch (NoSuchFieldError e24) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$HwIccCardConstants$HwState[HwState.SIM_CORPORATE_LOCKED.ordinal()] = 3;
            } catch (NoSuchFieldError e25) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$HwIccCardConstants$HwState[HwState.SIM_SERVICE_PROVIDER_LOCKED.ordinal()] = 4;
            } catch (NoSuchFieldError e26) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$HwIccCardConstants$HwState[HwState.SIM_SIM_LOCKED.ordinal()] = 5;
            } catch (NoSuchFieldError e27) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$HwIccCardConstants$HwState[HwState.SIM_NETWORK_LOCKED_PUK.ordinal()] = 6;
            } catch (NoSuchFieldError e28) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$HwIccCardConstants$HwState[HwState.SIM_NETWORK_SUBSET_LOCKED_PUK.ordinal()] = 7;
            } catch (NoSuchFieldError e29) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$HwIccCardConstants$HwState[HwState.SIM_CORPORATE_LOCKED_PUK.ordinal()] = 8;
            } catch (NoSuchFieldError e30) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$HwIccCardConstants$HwState[HwState.SIM_SERVICE_PROVIDER_LOCKED_PUK.ordinal()] = 9;
            } catch (NoSuchFieldError e31) {
            }
        }
    }

    public HwIccCardProxyReference(UiccProfile iccCardProxy) {
        this.mIccCardProxy = iccCardProxy;
    }

    public void custSetExternalState(PersoSubState ps) {
        synchronized (this.mLock) {
            HwState oldState = this.mExternalState;
            this.mExternalState = processPersoSubState(ps);
            int callbackHwState = processHwState(this.mExternalState);
            if (oldState != this.mExternalState) {
                setSystemProperty("gsm.sim.state", iccCardProxyUtils.getPhoneId(this.mIccCardProxy), this.mExternalState.toString());
                broadcastIccStateChangedIntent(this.mExternalState.getIntentString(), this.mExternalState.getReason());
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mExternalState = ");
                stringBuilder.append(this.mExternalState);
                stringBuilder.append(", mExternalState = ");
                stringBuilder.append(this.mExternalState.toString());
                stringBuilder.append(", getIntentString = ");
                stringBuilder.append(this.mExternalState.getIntentString());
                stringBuilder.append(", getReason = ");
                stringBuilder.append(this.mExternalState.getReason());
                logd(stringBuilder.toString());
                this.mNetworkLockedRegistrants.notifyRegistrants(new AsyncResult(null, Integer.valueOf(callbackHwState), null));
            }
        }
    }

    int processHwState(HwState hs) {
        switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$HwIccCardConstants$HwState[hs.ordinal()]) {
            case 1:
                return 3;
            case 2:
                return 4;
            case 3:
                return 5;
            case 4:
                return 6;
            case 5:
                return 7;
            case 6:
                return 8;
            case 7:
                return 9;
            case 8:
                return 10;
            case 9:
                return 11;
            default:
                return 0;
        }
    }

    HwState processPersoSubState(PersoSubState ps) {
        HwState result = HwState.UNKNOWN;
        switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[ps.ordinal()]) {
            case 1:
                return HwState.NETWORK_LOCKED;
            case 2:
                return HwState.SIM_NETWORK_SUBSET_LOCKED;
            case 3:
                return HwState.SIM_CORPORATE_LOCKED;
            case 4:
                return HwState.SIM_SERVICE_PROVIDER_LOCKED;
            case 5:
                return HwState.SIM_SIM_LOCKED;
            case 6:
                return HwState.SIM_NETWORK_LOCKED_PUK;
            case 7:
                return HwState.SIM_NETWORK_SUBSET_LOCKED_PUK;
            case 8:
                return HwState.SIM_CORPORATE_LOCKED_PUK;
            case 9:
                return HwState.SIM_SERVICE_PROVIDER_LOCKED_PUK;
            default:
                return result;
        }
    }

    void broadcastIccStateChangedIntent(String value, String reason) {
        synchronized (this.mLock) {
            if (iccCardProxyUtils.getPhoneId(this.mIccCardProxy) == 0 || iccCardProxyUtils.getPhoneId(this.mIccCardProxy) == 1) {
                Intent intent = new Intent("android.intent.action.SIM_STATE_CHANGED");
                intent.addFlags(536870912);
                intent.putExtra("phoneName", "Phone");
                intent.putExtra("ss", value);
                intent.putExtra("reason", reason);
                SubscriptionManager.putPhoneIdAndSubIdExtra(intent, iccCardProxyUtils.getPhoneId(this.mIccCardProxy));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Broadcasting intent ACTION_SIM_STATE_CHANGED ");
                stringBuilder.append(value);
                stringBuilder.append(" reason ");
                stringBuilder.append(reason);
                stringBuilder.append(" for mPhoneId : ");
                stringBuilder.append(iccCardProxyUtils.getPhoneId(this.mIccCardProxy));
                logd(stringBuilder.toString());
                ActivityManagerNative.broadcastStickyIntent(intent, "android.permission.READ_PHONE_STATE", -1);
                return;
            }
            logd("broadcastIccStateChangedIntent: Card Index is not set; Return!!");
        }
    }

    private void setSystemProperty(String property, int slotId, String value) {
        TelephonyManager.setTelephonyProperty(SubscriptionController.getInstance().getSubId(slotId)[0], property, value);
    }

    public void supplyDepersonalization(String pin, int type, Message onComplete) {
        logd("supplyDepersonalization");
        synchronized (this.mLock) {
            iccCardProxyUtils.getCi(this.mIccCardProxy).supplyDepersonalization(pin, type, onComplete);
        }
    }

    void logd(String s) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(iccCardProxyUtils.getPhoneId(this.mIccCardProxy));
        stringBuilder.append("]");
        stringBuilder.append(s);
        Rlog.d(str, stringBuilder.toString());
    }

    public boolean getIccCardStateHW() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIccCardStateHW;
        }
        return z;
    }

    public void custUpdateExternalState(State s) {
        if (s == State.ABSENT || s == State.PIN_REQUIRED || s == State.PUK_REQUIRED || s == State.NETWORK_LOCKED) {
            this.mIccCardStateHW = true;
        } else {
            this.mIccCardStateHW = false;
        }
    }

    public void custRegisterForNetworkLocked(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            this.mNetworkLockedRegistrants.add(new Registrant(h, what, obj));
        }
    }

    public void custUnregisterForNetworkLocked(Handler h) {
        synchronized (this.mLock) {
            this.mNetworkLockedRegistrants.remove(h);
        }
    }

    public void handleMessageExtend(Message msg) {
        if (IccCardProxyUtils.getEventRadioOffOrUnavailable() == msg.what) {
            if (SystemProperties.getInt("persist.radio.apm_sim_not_pwdn", 0) == 0) {
                rlog("persist.radio.apm_sim_not_pwdn is not set");
                this.mIccCardProxy.getHandler().handleMessage(msg);
                return;
            }
            iccCardProxyUtils.setRadioOn(this.mIccCardProxy, false);
            rlog("persist.radio.apm_sim_not_pwdn is set");
        } else if (msg.what != EVENT_ICC_REFRESH) {
            rlog("not handled event");
        } else {
            rlog("handleMessage get message EVENT_ICC_REFRESH");
            iccCardProxyUtils.broadcastIccStateChangedIntent(this.mIccCardProxy, "SIM_REFRESH", null);
        }
    }

    public boolean updateExternalStateDeactived() {
        if (!WrapperFactory.getMSimTelephonyManagerWrapper().isMultiSimEnabled() || iccCardProxyUtils.getUiccCard(this.mIccCardProxy).getNumApplications() <= 0 || iccCardProxyUtils.getUiccCard(this.mIccCardProxy).hasAppActived()) {
            return false;
        }
        iccCardProxyUtils.setExternalState(this.mIccCardProxy, State.DEACTIVED);
        return true;
    }

    public String getIccStateIntentString(State state) {
        if (state == State.DEACTIVED) {
            return "DEACTIVED";
        }
        return iccCardProxyUtils.getIccStateIntentString(this.mIccCardProxy, state);
    }

    public void registerUiccCardEventsExtend() {
        if (iccCardProxyUtils.getIccRecords(this.mIccCardProxy) != null) {
            iccCardProxyUtils.getIccRecords(this.mIccCardProxy).registerForIccRefresh(this.mIccCardProxy.getHandler(), EVENT_ICC_REFRESH, null);
        }
    }

    public void unregisterUiccCardEventsExtend() {
        if (iccCardProxyUtils.getIccRecords(this.mIccCardProxy) != null) {
            iccCardProxyUtils.getIccRecords(this.mIccCardProxy).unRegisterForIccRefresh(this.mIccCardProxy.getHandler());
        }
    }

    public void setUiccApplication(UiccCardApplication uiccCardApplication) {
        this.mUiccCardApplication = uiccCardApplication;
    }

    public void registerForFdnStatusChange(Handler h) {
        if (this.mUiccCardApplication != null && HuaweiTelephonyConfigs.isPsRestrictedByFdn()) {
            this.mUiccCardApplication.registerForFdnStatusChange(h, 1, null);
        }
    }

    public void unregisterForFdnStatusChange(Handler h) {
        if (this.mUiccCardApplication != null && HuaweiTelephonyConfigs.isPsRestrictedByFdn()) {
            this.mUiccCardApplication.unregisterForFdnStatusChange(h);
        }
    }

    public void queryFdn() {
        if (this.mUiccCardApplication != null && HuaweiTelephonyConfigs.isPsRestrictedByFdn()) {
            this.mUiccCardApplication.queryFdn();
        }
    }

    public void handleCustMessage(Message msg) {
        if (msg.what == 1) {
            boolean isFdnActivated = false;
            int[] subId = SubscriptionController.getInstance().getSubId(iccCardProxyUtils.getPhoneId(this.mIccCardProxy));
            if (this.mUiccCardApplication != null) {
                boolean z = this.mUiccCardApplication.getIccFdnAvailable() && this.mUiccCardApplication.getIccFdnEnabled();
                isFdnActivated = z;
            }
            if (subId != null) {
                if (subId[0] == 0) {
                    SystemProperties.set("gsm.hw.fdn.activated1", String.valueOf(isFdnActivated));
                } else if (subId[0] == 1) {
                    SystemProperties.set("gsm.hw.fdn.activated2", String.valueOf(isFdnActivated));
                }
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("fddn EVENT_FDN_STATUS_CHANGED ,set PROPERTY_FDN_ACTIVATED to:");
                stringBuilder.append(String.valueOf(isFdnActivated));
                stringBuilder.append(" ,subId:");
                stringBuilder.append(subId[0]);
                Rlog.d(str, stringBuilder.toString());
                UiccController.getInstance().notifyFdnStatusChange();
            }
        }
    }

    private void rlog(String s) {
        Rlog.d(LOG_TAG, s);
    }

    public int processCurrentAppType(UiccCard uiccCard, int defaultValue, int cardIndex) {
        int currentAppType = defaultValue;
        boolean IS_CHINA_TELECOM = SystemProperties.get("ro.config.hw_opta", "0").equals("92") && SystemProperties.get("ro.config.hw_optb", "0").equals("156");
        boolean isCDMASimCard = HwTelephonyManagerInner.getDefault().isCDMASimCard(cardIndex);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mCardIndex = ");
        stringBuilder.append(cardIndex);
        stringBuilder.append(", isCDMASimCard = ");
        stringBuilder.append(isCDMASimCard);
        Rlog.d(str, stringBuilder.toString());
        if (HwModemCapability.isCapabilitySupport(14)) {
            if ((isCDMASimCard || IS_CHINA_TELECOM) && 2 == currentAppType && uiccCard.getCdmaSubscriptionAppIndex() < 0 && uiccCard.getGsmUmtsSubscriptionAppIndex() >= 0) {
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("defaultValue = ");
                stringBuilder.append(defaultValue);
                stringBuilder.append(", currentAppType = ");
                stringBuilder.append(currentAppType);
                Rlog.d(str, stringBuilder.toString());
                currentAppType = 1;
            }
            if (1 == currentAppType && uiccCard.getCdmaSubscriptionAppIndex() >= 0) {
                currentAppType = 2;
                Rlog.d(LOG_TAG, "mCurrentAppType changes from APP_FAM_3GPP to APP_FAM_3GPP2");
            }
            return currentAppType;
        }
        if (true == IS_CHINA_TELECOM || isCDMASimCard) {
            Phone phone = PhoneFactory.getPhone(cardIndex);
            if (phone != null && 1 == phone.getPhoneType()) {
                currentAppType = 1;
            }
            if (phone != null && 2 == phone.getPhoneType()) {
                currentAppType = 2;
            }
        }
        return currentAppType;
    }

    public Integer getUiccIndex(Message msg) {
        Integer index = Integer.valueOf(null);
        if (msg == null || msg.obj == null || !(msg.obj instanceof AsyncResult)) {
            return index;
        }
        AsyncResult ar = msg.obj;
        if (ar.result == null || !(ar.result instanceof Integer)) {
            return index;
        }
        return ar.result;
    }

    private boolean isAirplaneMode(Context context) {
        return System.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0;
    }

    public boolean isSimAbsent(Context context, UiccCard uiccCard, boolean radioOn) {
        boolean mDSDSPowerup = SystemProperties.getBoolean("ro.config.hw_dsdspowerup", false);
        if ((!isAirplaneMode(context) || mDSDSPowerup) && uiccCard != null) {
            return true;
        }
        return false;
    }

    public void processSimLockStateForCT() {
        State newState = this.mIccCardProxy.getState();
        int mSlotId = iccCardProxyUtils.getPhoneId(this.mIccCardProxy);
        boolean IS_CHINA_TELECOM = SystemProperties.get("ro.config.hw_opta", "0").equals("92") && SystemProperties.get("ro.config.hw_optb", "0").equals("156");
        if (IS_CHINA_TELECOM && mSlotId == 0 && SystemProperties.getBoolean("ro.hwpp.simlock_no_pop_for_ct", false)) {
            if (State.CARD_IO_ERROR == newState && !this.mBroadcastForNotCT4GCardDone) {
                this.mBroadcastForNotCT4GCardDone = true;
                broadcastForHwCardManager();
            } else if (State.ABSENT == newState) {
                rlog("reset mBroadcastForNotCT4GCardDone!");
                this.mBroadcastForNotCT4GCardDone = false;
            }
        }
    }

    private void broadcastForHwCardManager() {
        rlog("[broadcastForHwCardManager]");
        Intent intent = new Intent("com.huawei.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        intent.putExtra("popupDialog", "true");
        ActivityManagerNative.broadcastStickyIntent(intent, "android.permission.READ_PHONE_STATE", -1);
    }

    public void custResetExternalState(State s) {
        synchronized (this.mLock) {
            this.mExternalState = HwState.UNKNOWN;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[custResetExternalState] reset mExternalState when State is");
            stringBuilder.append(s);
            logd(stringBuilder.toString());
        }
    }

    public State modifySimStateForVsim(int phoneId, State s) {
        return HwVSimUtils.modifySimStateForVsim(phoneId, s);
    }
}
