package com.android.internal.telephony.uicc;

import android.app.ActivityManager;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings.Global;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccAccessRule;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.PersoSubState;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.IccCardStatus.PinState;
import com.android.internal.telephony.uicc.euicc.EuiccCard;
import com.android.internal.telephony.vsim.VSimUtilsInner;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UiccProfile extends AbstractIccCardProxy {
    protected static final boolean DBG = true;
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    public static final int EVENT_APP_READY = 3;
    private static final int EVENT_CARRIER_CONFIG_CHANGED = 14;
    private static final int EVENT_CARRIER_PRIVILEGES_LOADED = 13;
    private static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 101;
    private static final int EVENT_CLOSE_LOGICAL_CHANNEL_DONE = 9;
    private static final int EVENT_EID_READY = 6;
    private static final int EVENT_ICC_CHANGED = 1001;
    private static final int EVENT_ICC_LOCKED = 2;
    private static final int EVENT_ICC_RECORD_EVENTS = 7;
    private static final int EVENT_IMSI_READY = 100;
    private static final int EVENT_NETWORK_LOCKED = 5;
    private static final int EVENT_OPEN_LOGICAL_CHANNEL_DONE = 8;
    private static final int EVENT_RADIO_OFF_OR_UNAVAILABLE = 1;
    private static final int EVENT_RECORDS_LOADED = 4;
    private static final int EVENT_SIM_IO_DONE = 12;
    private static final int EVENT_TRANSMIT_APDU_BASIC_CHANNEL_DONE = 11;
    private static final int EVENT_TRANSMIT_APDU_LOGICAL_CHANNEL_DONE = 10;
    protected static final String LOG_TAG = "UiccProfile";
    private static final String OPERATOR_BRAND_OVERRIDE_PREFIX = "operator_branding_";
    private static final boolean VDBG = false;
    private RegistrantList mCarrierPrivilegeRegistrants;
    private UiccCarrierPrivilegeRules mCarrierPrivilegeRules;
    private CatService mCatService;
    private CdmaSubscriptionSourceManager mCdmaSSM;
    private int mCdmaSubscriptionAppIndex;
    private CommandsInterface mCi;
    private Context mContext;
    private int mCurrentAppType;
    private boolean mDisposed;
    private State mExternalState;
    private int mGsmUmtsSubscriptionAppIndex;
    @VisibleForTesting
    public final Handler mHandler;
    private IccRecords mIccRecords;
    private int mImsSubscriptionAppIndex;
    private final Object mLock;
    private RegistrantList mNetworkLockedRegistrants;
    private RegistrantList mOperatorBrandOverrideRegistrants;
    private final int mPhoneId;
    private final ContentObserver mProvisionCompleteContentObserver;
    private final BroadcastReceiver mReceiver;
    private TelephonyManager mTelephonyManager;
    private UiccCardApplication mUiccApplication;
    private UiccCardApplication[] mUiccApplications = new UiccCardApplication[8];
    private final UiccCard mUiccCard;
    private PinState mUniversalPinState;

    /* renamed from: com.android.internal.telephony.uicc.UiccProfile$4 */
    static /* synthetic */ class AnonymousClass4 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$IccCardConstants$State = new int[State.values().length];

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
            $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState = new int[AppState.values().length];
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

    public Handler getHandler() {
        return this.mHandler;
    }

    public UiccProfile(Context c, CommandsInterface ci, IccCardStatus ics, int phoneId, UiccCard uiccCard, Object lock) {
        boolean z = false;
        this.mDisposed = false;
        this.mCdmaSSM = null;
        this.mCarrierPrivilegeRegistrants = new RegistrantList();
        this.mOperatorBrandOverrideRegistrants = new RegistrantList();
        this.mNetworkLockedRegistrants = new RegistrantList();
        this.mCurrentAppType = 1;
        this.mUiccApplication = null;
        this.mIccRecords = null;
        this.mExternalState = State.UNKNOWN;
        this.mProvisionCompleteContentObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                UiccProfile.this.mContext.getContentResolver().unregisterContentObserver(this);
                for (String pkgName : UiccProfile.this.getUninstalledCarrierPackages()) {
                    InstallCarrierAppUtils.showNotification(UiccProfile.this.mContext, pkgName);
                    InstallCarrierAppUtils.registerPackageInstallReceiver(UiccProfile.this.mContext);
                }
            }
        };
        this.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    UiccProfile.this.mHandler.sendMessage(UiccProfile.this.mHandler.obtainMessage(14));
                }
            }
        };
        this.mHandler = new Handler() {
            public void handleMessage(Message msg) {
                StringBuilder stringBuilder;
                if (!UiccProfile.this.mDisposed || msg.what == 8 || msg.what == 9 || msg.what == 10 || msg.what == 11 || msg.what == 12) {
                    UiccProfile uiccProfile = UiccProfile.this;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("handleMessage: Received ");
                    stringBuilder2.append(msg.what);
                    stringBuilder2.append(" for phoneId ");
                    stringBuilder2.append(UiccProfile.this.mPhoneId);
                    uiccProfile.loglocal(stringBuilder2.toString());
                    int i = msg.what;
                    if (i != 1001) {
                        switch (i) {
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 6:
                                break;
                            case 5:
                                UiccProfile.this.mNetworkLockedRegistrants.notifyRegistrants();
                                break;
                            case 7:
                                if (UiccProfile.this.mCurrentAppType == 1 && UiccProfile.this.mIccRecords != null && ((Integer) ((AsyncResult) msg.obj).result).intValue() == 2) {
                                    UiccProfile.this.mTelephonyManager.setSimOperatorNameForPhone(UiccProfile.this.mPhoneId, UiccProfile.this.mIccRecords.getServiceProviderName());
                                    break;
                                }
                            case 8:
                            case 9:
                            case 10:
                            case 11:
                            case 12:
                                AsyncResult ar = msg.obj;
                                if (ar.exception != null) {
                                    UiccProfile uiccProfile2 = UiccProfile.this;
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("handleMessage: Exception ");
                                    stringBuilder3.append(ar.exception);
                                    uiccProfile2.loglocal(stringBuilder3.toString());
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("handleMessage: Error in SIM access with exception");
                                    stringBuilder2.append(ar.exception);
                                    UiccProfile.log(stringBuilder2.toString());
                                }
                                AsyncResult.forMessage((Message) ar.userObj, ar.result, ar.exception);
                                ((Message) ar.userObj).sendToTarget();
                                break;
                            case 13:
                                UiccProfile.this.onCarrierPrivilegesLoadedMessage();
                                UiccProfile.this.updateExternalState();
                                break;
                            case 14:
                                UiccProfile.this.handleCarrierNameOverride();
                                break;
                            default:
                                switch (i) {
                                    case 100:
                                        UiccProfile.broadcastIccStateChangedIntent("IMSI", null, UiccProfile.this.mPhoneId);
                                        break;
                                    case 101:
                                        UiccProfile.this.updateActiveRecord();
                                        break;
                                    default:
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("handleMessage: Unhandled message with number: ");
                                        stringBuilder.append(msg.what);
                                        UiccProfile.loge(stringBuilder.toString());
                                        UiccProfile.this.handleMessageExtend(msg);
                                        break;
                                }
                        }
                        UiccProfile.this.updateExternalState();
                    } else {
                        UiccProfile.this.handleIccChangedEvent(msg);
                    }
                    UiccProfile.this.handleCustMessage(msg);
                    return;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleMessage: Received ");
                stringBuilder.append(msg.what);
                stringBuilder.append(" after dispose(); ignoring the message");
                UiccProfile.loge(stringBuilder.toString());
            }
        };
        log("Creating profile");
        this.mLock = lock;
        this.mUiccCard = uiccCard;
        this.mPhoneId = phoneId;
        this.mCdmaSSM = CdmaSubscriptionSourceManager.getInstance(c, ci, this.mHandler, 101, null);
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            if (phone.getPhoneType() == 1) {
                z = true;
            }
            setCurrentAppType(z);
        }
        if (this.mUiccCard instanceof EuiccCard) {
            ((EuiccCard) this.mUiccCard).registerForEidReady(this.mHandler, 6, null);
        }
        update(c, ci, ics);
        ci.registerForOffOrNotAvailable(this.mHandler, 1, null);
        resetProperties();
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        c.registerReceiver(this.mReceiver, intentfilter);
        UiccController.getInstance().registerForIccChanged(this.mHandler, 1001, null);
    }

    public void dispose() {
        log("Disposing profile");
        if (this.mUiccCard instanceof EuiccCard) {
            ((EuiccCard) this.mUiccCard).unregisterForEidReady(this.mHandler);
        }
        synchronized (this.mLock) {
            unregisterAllAppEvents();
            unregisterCurrAppEvents();
            InstallCarrierAppUtils.hideAllNotifications(this.mContext);
            InstallCarrierAppUtils.unregisterPackageInstallReceiver(this.mContext);
            this.mCi.unregisterForOffOrNotAvailable(this.mHandler);
            this.mContext.unregisterReceiver(this.mReceiver);
            if (this.mCatService != null) {
                this.mCatService.dispose();
            }
            for (UiccCardApplication app : this.mUiccApplications) {
                if (app != null) {
                    app.dispose();
                }
            }
            this.mCatService = null;
            this.mUiccApplications = null;
            this.mCarrierPrivilegeRules = null;
            this.mDisposed = true;
            this.mCdmaSSM.dispose(this.mHandler);
        }
    }

    public void setVoiceRadioTech(int radioTech) {
        synchronized (this.mLock) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Setting radio tech ");
            stringBuilder.append(ServiceState.rilRadioTechnologyToString(radioTech));
            log(stringBuilder.toString());
            setCurrentAppType(ServiceState.isGsm(radioTech));
            updateIccAvailability(false);
            updateActiveRecord();
        }
    }

    private void updateActiveRecord() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateActiveRecord app type = ");
        stringBuilder.append(this.mCurrentAppType);
        stringBuilder.append("mIccRecords = ");
        stringBuilder.append(this.mIccRecords);
        log(stringBuilder.toString());
        if (this.mIccRecords != null) {
            if (this.mCurrentAppType == 2) {
                if (this.mCdmaSSM.getCdmaSubscriptionSource() == 0) {
                    log("Setting Ruim Record as active");
                    this.mIccRecords.recordsRequired();
                }
            } else if (this.mCurrentAppType == 1) {
                log("Setting SIM Record as active");
                this.mIccRecords.recordsRequired();
            }
        }
    }

    private void setCurrentAppType(boolean isGsm) {
        synchronized (this.mLock) {
            boolean isLteOnCdmaMode = TelephonyManager.getLteOnCdmaModeStatic() == 1;
            if (!isGsm) {
                if (!isLteOnCdmaMode) {
                    this.mCurrentAppType = 2;
                }
            }
            this.mCurrentAppType = 1;
        }
    }

    private void handleCarrierNameOverride() {
        SubscriptionController subCon = SubscriptionController.getInstance();
        int subId = subCon.getSubIdUsingPhoneId(this.mPhoneId);
        if (subId == -1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("subId not valid for Phone ");
            stringBuilder.append(this.mPhoneId);
            loge(stringBuilder.toString());
            return;
        }
        CarrierConfigManager configLoader = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (configLoader == null) {
            loge("Failed to load a Carrier Config");
            return;
        }
        PersistableBundle config = configLoader.getConfigForSubId(subId);
        boolean preferCcName = config.getBoolean("carrier_name_override_bool", false);
        String ccName = config.getString("carrier_name_string");
        if (preferCcName || (TextUtils.isEmpty(getServiceProviderName()) && !TextUtils.isEmpty(ccName))) {
            if (this.mIccRecords != null) {
                this.mIccRecords.setServiceProviderName(ccName);
            }
            this.mTelephonyManager.setSimOperatorNameForPhone(this.mPhoneId, ccName);
            this.mOperatorBrandOverrideRegistrants.notifyRegistrants();
        }
        updateCarrierNameForSubscription(subCon, subId);
    }

    private void updateCarrierNameForSubscription(SubscriptionController subCon, int subId) {
        SubscriptionInfo subInfo = subCon.getActiveSubscriptionInfo(subId, this.mContext.getOpPackageName());
        if (subInfo != null && subInfo.getNameSource() != 2) {
            CharSequence oldSubName = subInfo.getDisplayName();
            String newCarrierName = this.mTelephonyManager.getSimOperatorName(subId);
            if (!(TextUtils.isEmpty(newCarrierName) || newCarrierName.equals(oldSubName))) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sim name[");
                stringBuilder.append(this.mPhoneId);
                stringBuilder.append("] = ");
                stringBuilder.append(newCarrierName);
                log(stringBuilder.toString());
                subCon.setDisplayName(newCarrierName, subId);
            }
        }
    }

    private void handleIccChangedEvent(Message msg) {
        Integer index = getUiccIndex(msg);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleIccChangedEvent index = ");
        stringBuilder.append(index);
        stringBuilder.append(", mPhoneId = ");
        stringBuilder.append(this.mPhoneId);
        log(stringBuilder.toString());
        if (index == null || index.equals(Integer.valueOf(this.mPhoneId))) {
            updateIccAvailability(false);
        }
    }

    private void updateIccAvailability(boolean allAppsChanged) {
        synchronized (this.mLock) {
            IccRecords newRecords = null;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mCurrentAppType = ");
            stringBuilder.append(this.mCurrentAppType);
            stringBuilder.append(", mPhoneId = ");
            stringBuilder.append(this.mPhoneId);
            log(stringBuilder.toString());
            if (this.mUiccCard != null) {
                this.mCurrentAppType = processCurrentAppType(this.mUiccCard, this.mCurrentAppType, this.mPhoneId);
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("mCurrentAppType = ");
            stringBuilder.append(this.mCurrentAppType);
            log(stringBuilder.toString());
            UiccCardApplication newApp = getApplication(this.mCurrentAppType);
            StringBuilder stringBuilder2;
            if (this.mUiccCard != null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("UiccCard is not null, newApp = ");
                stringBuilder2.append(newApp);
                log(stringBuilder2.toString());
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("UiccCard is null, newApp = ");
                stringBuilder2.append(newApp);
                log(stringBuilder2.toString());
            }
            if (newApp != null) {
                newRecords = newApp.getIccRecords();
            }
            if (allAppsChanged) {
                unregisterAllAppEvents();
                registerAllAppEvents();
            }
            if (!(this.mIccRecords == newRecords && this.mUiccApplication == newApp)) {
                log("Icc changed. Reregistering.");
                unregisterCurrAppEvents();
                this.mUiccApplication = newApp;
                setUiccApplication(newApp);
                queryFdn();
                this.mIccRecords = newRecords;
                registerCurrAppEvents();
                updateActiveRecord();
            }
            updateExternalState();
        }
    }

    void resetProperties() {
        if (this.mCurrentAppType == 1) {
            log("update icc_operator_numeric=");
            this.mTelephonyManager.setSimOperatorNumericForPhone(this.mPhoneId, "");
            this.mTelephonyManager.setSimCountryIsoForPhone(this.mPhoneId, "");
            this.mTelephonyManager.setSimOperatorNameForPhone(this.mPhoneId, "");
        }
    }

    @VisibleForTesting(visibility = Visibility.PRIVATE)
    public void updateExternalState() {
        if (this.mUiccCard.getCardState() == CardState.CARDSTATE_ABSENT) {
            RadioState radioState = this.mCi.getRadioState();
            if (radioState == RadioState.RADIO_ON) {
                setExternalState(State.ABSENT);
            } else {
                if (isSimAbsent(this.mContext, this.mUiccCard, radioState == RadioState.RADIO_ON) || (HuaweiTelephonyConfigs.isQcomPlatform() && radioState == RadioState.RADIO_OFF)) {
                    setExternalState(State.ABSENT);
                    log("updateExternalState ABSENT");
                } else {
                    setExternalState(State.NOT_READY);
                }
            }
        } else if (this.mUiccCard.getCardState() == CardState.CARDSTATE_ERROR) {
            setExternalState(State.CARD_IO_ERROR);
        } else if (this.mUiccCard.getCardState() == CardState.CARDSTATE_RESTRICTED) {
            setExternalState(State.CARD_RESTRICTED);
        } else if ((this.mUiccCard instanceof EuiccCard) && ((EuiccCard) this.mUiccCard).getEid() == null) {
            log("EID is not ready yet.");
        } else if (this.mUiccApplication == null) {
            loge("updateExternalState: setting state to NOT_READY because mUiccApplication is null");
            setExternalState(State.NOT_READY);
        } else {
            boolean cardLocked = false;
            State lockedState = null;
            AppState appState = this.mUiccApplication.getState();
            if (this.mUiccApplication.getPin1State() == PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
                cardLocked = true;
                lockedState = State.PERM_DISABLED;
            } else if (appState == AppState.APPSTATE_PIN) {
                cardLocked = true;
                lockedState = State.PIN_REQUIRED;
            } else if (appState == AppState.APPSTATE_PUK) {
                cardLocked = true;
                lockedState = State.PUK_REQUIRED;
            } else if (appState == AppState.APPSTATE_SUBSCRIPTION_PERSO) {
                if (this.mUiccApplication.getPersoSubState() == PersoSubState.PERSOSUBSTATE_SIM_NETWORK) {
                    cardLocked = true;
                    lockedState = State.NETWORK_LOCKED;
                } else if (this.mUiccApplication.getPersoSubState() == PersoSubState.PERSOSUBSTATE_READY || this.mUiccApplication.getPersoSubState() == PersoSubState.PERSOSUBSTATE_UNKNOWN) {
                    setExternalState(State.UNKNOWN);
                    custResetExternalState(State.UNKNOWN);
                    return;
                } else {
                    custSetExternalState(this.mUiccApplication.getPersoSubState());
                    this.mExternalState = State.NETWORK_LOCKED;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateExternalState: set mPhoneId=");
                    stringBuilder.append(this.mPhoneId);
                    stringBuilder.append(" mExternalState=");
                    stringBuilder.append(this.mExternalState);
                    loge(stringBuilder.toString());
                    return;
                }
            }
            if (cardLocked) {
                if (this.mIccRecords == null || !(this.mIccRecords.getLockedRecordsLoaded() || this.mIccRecords.getNetworkLockedRecordsLoaded())) {
                    setExternalState(State.NOT_READY);
                } else {
                    setExternalState(lockedState);
                }
                return;
            }
            switch (appState) {
                case APPSTATE_UNKNOWN:
                    setExternalState(State.NOT_READY);
                    break;
                case APPSTATE_READY:
                    checkAndUpdateIfAnyAppToBeIgnored();
                    if (areAllApplicationsReady()) {
                        if (!areAllRecordsLoaded() || !areCarrierPriviligeRulesLoaded()) {
                            setExternalState(State.READY);
                            break;
                        } else {
                            setExternalState(State.LOADED);
                            break;
                        }
                    }
                    setExternalState(State.NOT_READY);
                    break;
            }
            custUpdateExternalState(getState());
        }
    }

    private void registerAllAppEvents() {
        for (UiccCardApplication app : this.mUiccApplications) {
            if (app != null) {
                app.registerForReady(this.mHandler, 3, null);
                IccRecords ir = app.getIccRecords();
                if (ir != null) {
                    ir.registerForRecordsLoaded(this.mHandler, 4, null);
                    ir.registerForRecordsEvents(this.mHandler, 7, null);
                }
            }
        }
    }

    private void unregisterAllAppEvents() {
        for (UiccCardApplication app : this.mUiccApplications) {
            if (app != null) {
                app.unregisterForReady(this.mHandler);
                IccRecords ir = app.getIccRecords();
                if (ir != null) {
                    ir.unregisterForRecordsLoaded(this.mHandler);
                    ir.unregisterForRecordsEvents(this.mHandler);
                }
            }
        }
    }

    private void registerCurrAppEvents() {
        if (this.mIccRecords != null) {
            this.mIccRecords.registerForImsiReady(this.mHandler, 100, null);
            this.mIccRecords.registerForLockedRecordsLoaded(this.mHandler, 2, null);
            this.mIccRecords.registerForNetworkLockedRecordsLoaded(this.mHandler, 5, null);
        }
        registerForFdnStatusChange(this.mHandler);
        registerUiccCardEventsExtend();
    }

    private void unregisterCurrAppEvents() {
        if (this.mIccRecords != null) {
            this.mIccRecords.unregisterForImsiReady(this.mHandler);
            this.mIccRecords.unregisterForLockedRecordsLoaded(this.mHandler);
            this.mIccRecords.unregisterForNetworkLockedRecordsLoaded(this.mHandler);
        }
        unregisterForFdnStatusChange(this.mHandler);
        unregisterUiccCardEventsExtend();
    }

    static void broadcastIccStateChangedIntent(String value, String reason, int phoneId) {
        if (SubscriptionManager.isValidSlotIndex(phoneId)) {
            Intent intent = new Intent("android.intent.action.SIM_STATE_CHANGED");
            intent.addFlags(67108864);
            intent.putExtra("phoneName", "Phone");
            intent.putExtra("ss", value);
            intent.putExtra("reason", reason);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, phoneId);
            VSimUtilsInner.putVSimExtraForIccStateChanged(intent, phoneId, value);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("broadcastIccStateChangedIntent intent ACTION_SIM_STATE_CHANGED value=");
            stringBuilder.append(value);
            stringBuilder.append(" reason=");
            stringBuilder.append(reason);
            stringBuilder.append(" for phoneId=");
            stringBuilder.append(phoneId);
            log(stringBuilder.toString());
            ActivityManager.broadcastStickyIntent(intent, 51, -1);
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("broadcastIccStateChangedIntent: phoneId=");
        stringBuilder2.append(phoneId);
        stringBuilder2.append(" is invalid; Return!!");
        loge(stringBuilder2.toString());
    }

    private void setExternalState(State newState, boolean override) {
        synchronized (this.mLock) {
            StringBuilder stringBuilder;
            if (!SubscriptionManager.isValidSlotIndex(this.mPhoneId)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("setExternalState: mPhoneId=");
                stringBuilder.append(this.mPhoneId);
                stringBuilder.append(" is invalid; Return!!");
                loge(stringBuilder.toString());
            } else if (override || newState != this.mExternalState) {
                if (State.ABSENT == newState || State.NOT_READY == newState) {
                    custResetExternalState(newState);
                }
                if (blockPinStateForDualCards(newState)) {
                    return;
                }
                this.mExternalState = modifySimStateForVsim(this.mPhoneId, newState);
                if (this.mExternalState == State.LOADED && this.mIccRecords != null) {
                    String operator = this.mIccRecords.getOperatorNumeric();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setExternalState: operator=");
                    stringBuilder2.append(operator);
                    stringBuilder2.append(" mPhoneId=");
                    stringBuilder2.append(this.mPhoneId);
                    log(stringBuilder2.toString());
                    if (TextUtils.isEmpty(operator)) {
                        loge("setExternalState: state LOADED; Operator name is null");
                    } else {
                        this.mTelephonyManager.setSimOperatorNumericForPhone(this.mPhoneId, operator);
                        String countryCode = operator.substring(null, 3);
                        if (countryCode != null) {
                            try {
                                this.mTelephonyManager.setSimCountryIsoForPhone(this.mPhoneId, MccTable.countryCodeForMcc(Integer.parseInt(countryCode)));
                            } catch (Exception e) {
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("countryCodeForMcc error for countryCode = ");
                                stringBuilder3.append(countryCode);
                                loge(stringBuilder3.toString());
                            }
                        } else {
                            loge("setExternalState: state LOADED; Country code is null");
                        }
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("setExternalState: set mPhoneId=");
                stringBuilder.append(this.mPhoneId);
                stringBuilder.append(" mExternalState=");
                stringBuilder.append(this.mExternalState);
                log(stringBuilder.toString());
                this.mTelephonyManager.setSimStateForPhone(this.mPhoneId, getState().toString());
                processSimLockStateForCT();
                UiccController.updateInternalIccState(getIccStateIntentString(this.mExternalState), getIccStateReason(this.mExternalState), this.mPhoneId);
                broadcastIccStateChangedIntent(getIccStateIntentString(this.mExternalState), getIccStateReason(this.mExternalState), this.mPhoneId);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("setExternalState: !override and newstate unchanged from ");
                stringBuilder.append(newState);
                log(stringBuilder.toString());
            }
        }
    }

    public boolean blockPinStateForDualCards(State s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("blockPinStateForDualCards s = ");
        stringBuilder.append(s);
        stringBuilder.append(", mPhoneId ");
        stringBuilder.append(this.mPhoneId);
        log(stringBuilder.toString());
        if (s.isPinLocked()) {
            if (VSimUtilsInner.isVSimInProcess()) {
                if (VSimUtilsInner.needBlockPin(this.mPhoneId)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("vsim block pin for phone id ");
                    stringBuilder.append(this.mPhoneId);
                    log(stringBuilder.toString());
                    return true;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("vsim no need block pin for phone id ");
                stringBuilder.append(this.mPhoneId);
                stringBuilder.append(", just pass");
                log(stringBuilder.toString());
            } else if ((SystemProperties.getBoolean("persist.sys.dualcards", false) || SystemProperties.getBoolean("ro.config.full_network_support", false)) && HwTelephonyFactory.getHwUiccManager().getSwitchingSlot()) {
                log("setExternalState getWaitingSwitchBalongSlot is true, so return");
                return true;
            }
        }
        return false;
    }

    private void setExternalState(State newState) {
        setExternalState(newState, false);
    }

    public boolean getIccRecordsLoaded() {
        synchronized (this.mLock) {
            if (this.mIccRecords != null) {
                boolean recordsLoaded = this.mIccRecords.getRecordsLoaded();
                return recordsLoaded;
            }
            return false;
        }
    }

    private String getIccStateIntentString(State state) {
        switch (AnonymousClass4.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()]) {
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
        switch (AnonymousClass4.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()]) {
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

    public State getState() {
        if (!VSimUtilsInner.isPlatformTwoModems() || this.mCi == null || this.mCi.isRadioAvailable()) {
            State state;
            synchronized (this.mLock) {
                state = this.mExternalState;
            }
            return state;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[2Cards]pending sub");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append(" getState return ABSENT!");
        log(stringBuilder.toString());
        return State.ABSENT;
    }

    public IccRecords getIccRecords() {
        IccRecords iccRecords;
        synchronized (this.mLock) {
            iccRecords = this.mIccRecords;
        }
        return iccRecords;
    }

    public void registerForNetworkLocked(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mNetworkLockedRegistrants.add(r);
            if (getState() == State.NETWORK_LOCKED) {
                r.notifyRegistrant();
            }
            custRegisterForNetworkLocked(h, what, obj);
        }
    }

    public void unregisterForNetworkLocked(Handler h) {
        synchronized (this.mLock) {
            this.mNetworkLockedRegistrants.remove(h);
            custUnregisterForNetworkLocked(h);
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0022, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void supplyPin(String pin, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyPin(pin, onComplete);
            } else if (onComplete != null) {
                AsyncResult.forMessage(onComplete).exception = new RuntimeException("ICC card is absent.");
                onComplete.sendToTarget();
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0022, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void supplyPuk(String puk, String newPin, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyPuk(puk, newPin, onComplete);
            } else if (onComplete != null) {
                AsyncResult.forMessage(onComplete).exception = new RuntimeException("ICC card is absent.");
                onComplete.sendToTarget();
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0022, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void supplyPin2(String pin2, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyPin2(pin2, onComplete);
            } else if (onComplete != null) {
                AsyncResult.forMessage(onComplete).exception = new RuntimeException("ICC card is absent.");
                onComplete.sendToTarget();
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0022, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void supplyPuk2(String puk2, String newPin2, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyPuk2(puk2, newPin2, onComplete);
            } else if (onComplete != null) {
                AsyncResult.forMessage(onComplete).exception = new RuntimeException("ICC card is absent.");
                onComplete.sendToTarget();
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0022, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void supplyNetworkDepersonalization(String pin, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyNetworkDepersonalization(pin, onComplete);
            } else if (onComplete != null) {
                AsyncResult.forMessage(onComplete).exception = new RuntimeException("CommandsInterface is not set.");
                onComplete.sendToTarget();
            }
        }
    }

    public boolean getIccLockEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mUiccApplication != null && this.mUiccApplication.getIccLockEnabled();
        }
        return z;
    }

    public boolean getIccFdnEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mUiccApplication != null && this.mUiccApplication.getIccFdnEnabled();
        }
        return z;
    }

    public boolean getIccFdnAvailable() {
        return this.mUiccApplication != null ? this.mUiccApplication.getIccFdnAvailable() : false;
    }

    public boolean getIccPin2Blocked() {
        return this.mUiccApplication != null && this.mUiccApplication.getIccPin2Blocked();
    }

    public boolean getIccPuk2Blocked() {
        return this.mUiccApplication != null && this.mUiccApplication.getIccPuk2Blocked();
    }

    /* JADX WARNING: Missing block: B:11:0x0022, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setIccLockEnabled(boolean enabled, String password, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.setIccLockEnabled(enabled, password, onComplete);
            } else if (onComplete != null) {
                AsyncResult.forMessage(onComplete).exception = new RuntimeException("ICC card is absent.");
                onComplete.sendToTarget();
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0022, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setIccFdnEnabled(boolean enabled, String password, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.setIccFdnEnabled(enabled, password, onComplete);
            } else if (onComplete != null) {
                AsyncResult.forMessage(onComplete).exception = new RuntimeException("ICC card is absent.");
                onComplete.sendToTarget();
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0022, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void changeIccLockPassword(String oldPassword, String newPassword, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.changeIccLockPassword(oldPassword, newPassword, onComplete);
            } else if (onComplete != null) {
                AsyncResult.forMessage(onComplete).exception = new RuntimeException("ICC card is absent.");
                onComplete.sendToTarget();
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0022, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void changeIccFdnPassword(String oldPassword, String newPassword, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.changeIccFdnPassword(oldPassword, newPassword, onComplete);
            } else if (onComplete != null) {
                AsyncResult.forMessage(onComplete).exception = new RuntimeException("ICC card is absent.");
                onComplete.sendToTarget();
            }
        }
    }

    public String getServiceProviderName() {
        synchronized (this.mLock) {
            if (this.mIccRecords != null) {
                String serviceProviderName = this.mIccRecords.getServiceProviderName();
                return serviceProviderName;
            }
            return null;
        }
    }

    public boolean hasIccCard() {
        if (this.mUiccCard.getCardState() != CardState.CARDSTATE_ABSENT) {
            return true;
        }
        loge("hasIccCard: UiccProfile is not null but UiccCard is null or card state is ABSENT");
        return false;
    }

    public void update(Context c, CommandsInterface ci, IccCardStatus ics) {
        synchronized (this.mLock) {
            this.mUniversalPinState = ics.mUniversalPinState;
            this.mGsmUmtsSubscriptionAppIndex = ics.mGsmUmtsSubscriptionAppIndex;
            this.mCdmaSubscriptionAppIndex = ics.mCdmaSubscriptionAppIndex;
            this.mImsSubscriptionAppIndex = ics.mImsSubscriptionAppIndex;
            this.mContext = c;
            this.mCi = ci;
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(ics.mApplications.length);
            stringBuilder.append(" applications");
            log(stringBuilder.toString());
            for (int i = 0; i < this.mUiccApplications.length; i++) {
                if (this.mUiccApplications[i] == null) {
                    if (i < ics.mApplications.length) {
                        this.mUiccApplications[i] = new UiccCardApplication(this, ics.mApplications[i], this.mContext, this.mCi);
                    }
                } else if (i >= ics.mApplications.length) {
                    this.mUiccApplications[i].dispose();
                    this.mUiccApplications[i] = null;
                } else {
                    this.mUiccApplications[i].update(ics.mApplications[i], this.mContext, this.mCi);
                }
            }
            createAndUpdateCatServiceLocked();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Before privilege rules: ");
            stringBuilder.append(this.mCarrierPrivilegeRules);
            stringBuilder.append(" : ");
            stringBuilder.append(ics.mCardState);
            log(stringBuilder.toString());
            if (this.mCarrierPrivilegeRules == null && ics.mCardState == CardState.CARDSTATE_PRESENT) {
                this.mCarrierPrivilegeRules = new UiccCarrierPrivilegeRules(this, this.mHandler.obtainMessage(13));
            } else if (!(this.mCarrierPrivilegeRules == null || ics.mCardState == CardState.CARDSTATE_PRESENT)) {
                this.mCarrierPrivilegeRules = null;
            }
            sanitizeApplicationIndexesLocked();
            updateIccAvailability(true);
        }
    }

    private void createAndUpdateCatServiceLocked() {
        if (VSimUtilsInner.isVSimSub(this.mPhoneId)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createAndUpdateCatService, nothing for vsim sub ");
            stringBuilder.append(this.mPhoneId);
            log(stringBuilder.toString());
            return;
        }
        if (this.mUiccApplications.length <= 0 || this.mUiccApplications[0] == null) {
            if (this.mCatService != null) {
                this.mCatService.dispose();
            }
            this.mCatService = null;
        } else if (this.mCatService == null) {
            this.mCatService = CatService.getInstance(this.mCi, this.mContext, this, this.mPhoneId);
        } else {
            this.mCatService.update(this.mCi, this.mContext, this);
        }
    }

    public CatService getCatService() {
        return this.mCatService;
    }

    protected void finalize() {
        log("UiccProfile finalized");
    }

    private void sanitizeApplicationIndexesLocked() {
        this.mGsmUmtsSubscriptionAppIndex = checkIndexLocked(this.mGsmUmtsSubscriptionAppIndex, AppType.APPTYPE_SIM, AppType.APPTYPE_USIM);
        this.mCdmaSubscriptionAppIndex = checkIndexLocked(this.mCdmaSubscriptionAppIndex, AppType.APPTYPE_RUIM, AppType.APPTYPE_CSIM);
        this.mImsSubscriptionAppIndex = checkIndexLocked(this.mImsSubscriptionAppIndex, AppType.APPTYPE_ISIM, null);
    }

    private boolean isSupportedApplication(UiccCardApplication app) {
        if (app.getType() == AppType.APPTYPE_USIM || app.getType() == AppType.APPTYPE_CSIM || app.getType() == AppType.APPTYPE_SIM || app.getType() == AppType.APPTYPE_RUIM) {
            return true;
        }
        return false;
    }

    private void checkAndUpdateIfAnyAppToBeIgnored() {
        boolean[] appReadyStateTracker = new boolean[(AppType.APPTYPE_ISIM.ordinal() + 1)];
        int i = 0;
        for (UiccCardApplication app : this.mUiccApplications) {
            if (app != null && isSupportedApplication(app) && app.isReady()) {
                appReadyStateTracker[app.getType().ordinal()] = true;
            }
        }
        UiccCardApplication[] uiccCardApplicationArr = this.mUiccApplications;
        int length = uiccCardApplicationArr.length;
        while (i < length) {
            UiccCardApplication app2 = uiccCardApplicationArr[i];
            if (app2 != null && isSupportedApplication(app2) && !app2.isReady() && appReadyStateTracker[app2.getType().ordinal()]) {
                app2.setAppIgnoreState(true);
            }
            i++;
        }
    }

    private boolean areAllApplicationsReady() {
        boolean z = false;
        for (UiccCardApplication app : this.mUiccApplications) {
            if (app != null && isSupportedApplication(app) && !app.isReady() && !app.isAppIgnored()) {
                return false;
            }
        }
        if (this.mUiccApplication != null) {
            z = true;
        }
        return z;
    }

    private boolean areAllRecordsLoaded() {
        boolean z = false;
        for (UiccCardApplication app : this.mUiccApplications) {
            if (!(app == null || !isSupportedApplication(app) || app.isAppIgnored())) {
                IccRecords ir = app.getIccRecords();
                if (ir == null || !ir.isLoaded()) {
                    return false;
                }
            }
        }
        if (this.mUiccApplication != null) {
            z = true;
        }
        return z;
    }

    private int checkIndexLocked(int index, AppType expectedAppType, AppType altExpectedAppType) {
        StringBuilder stringBuilder;
        if (this.mUiccApplications == null || index >= this.mUiccApplications.length) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("App index ");
            stringBuilder.append(index);
            stringBuilder.append(" is invalid since there are no applications");
            loge(stringBuilder.toString());
            return -1;
        } else if (index < 0) {
            return -1;
        } else {
            if (this.mUiccApplications[index].getType() == expectedAppType || this.mUiccApplications[index].getType() == altExpectedAppType) {
                return index;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("App index ");
            stringBuilder.append(index);
            stringBuilder.append(" is invalid since it's not ");
            stringBuilder.append(expectedAppType);
            stringBuilder.append(" and not ");
            stringBuilder.append(altExpectedAppType);
            loge(stringBuilder.toString());
            return -1;
        }
    }

    public void registerForOpertorBrandOverride(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            this.mOperatorBrandOverrideRegistrants.add(new Registrant(h, what, obj));
        }
    }

    public void registerForCarrierPrivilegeRulesLoaded(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mCarrierPrivilegeRegistrants.add(r);
            if (areCarrierPriviligeRulesLoaded()) {
                r.notifyRegistrant();
            }
        }
    }

    public void unregisterForCarrierPrivilegeRulesLoaded(Handler h) {
        synchronized (this.mLock) {
            this.mCarrierPrivilegeRegistrants.remove(h);
        }
    }

    public void unregisterForOperatorBrandOverride(Handler h) {
        synchronized (this.mLock) {
            this.mOperatorBrandOverrideRegistrants.remove(h);
        }
    }

    static boolean isPackageInstalled(Context context, String pkgName) {
        StringBuilder stringBuilder;
        try {
            context.getPackageManager().getPackageInfo(pkgName, 1);
            stringBuilder = new StringBuilder();
            stringBuilder.append(pkgName);
            stringBuilder.append(" is installed.");
            log(stringBuilder.toString());
            return true;
        } catch (NameNotFoundException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(pkgName);
            stringBuilder.append(" is not installed.");
            log(stringBuilder.toString());
            return false;
        }
    }

    private void promptInstallCarrierApp(String pkgName) {
        if (!TextUtils.isEmpty(pkgName)) {
            this.mContext.startActivity(InstallCarrierAppTrampolineActivity.get(this.mContext, pkgName));
        }
    }

    private void onCarrierPrivilegesLoadedMessage() {
        UsageStatsManager usm = (UsageStatsManager) this.mContext.getSystemService("usagestats");
        if (usm != null) {
            usm.onCarrierPrivilegedAppsChanged();
        }
        InstallCarrierAppUtils.hideAllNotifications(this.mContext);
        InstallCarrierAppUtils.unregisterPackageInstallReceiver(this.mContext);
        synchronized (this.mLock) {
            this.mCarrierPrivilegeRegistrants.notifyRegistrants();
            boolean z = true;
            if (Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 1) != 1) {
                z = false;
            }
            if (z) {
                for (String pkgName : getUninstalledCarrierPackages()) {
                    promptInstallCarrierApp(pkgName);
                }
            } else {
                this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("device_provisioned"), false, this.mProvisionCompleteContentObserver);
            }
        }
    }

    private Set<String> getUninstalledCarrierPackages() {
        String whitelistSetting = Global.getString(this.mContext.getContentResolver(), "carrier_app_whitelist");
        if (TextUtils.isEmpty(whitelistSetting)) {
            return Collections.emptySet();
        }
        Map<String, String> certPackageMap = parseToCertificateToPackageMap(whitelistSetting);
        if (certPackageMap.isEmpty()) {
            return Collections.emptySet();
        }
        if (this.mCarrierPrivilegeRules == null) {
            return Collections.emptySet();
        }
        Set<String> uninstalledCarrierPackages = new ArraySet();
        for (UiccAccessRule accessRule : this.mCarrierPrivilegeRules.getAccessRules()) {
            String pkgName = (String) certPackageMap.get(accessRule.getCertificateHexString().toUpperCase());
            if (!(TextUtils.isEmpty(pkgName) || isPackageInstalled(this.mContext, pkgName))) {
                uninstalledCarrierPackages.add(pkgName);
            }
        }
        return uninstalledCarrierPackages;
    }

    @VisibleForTesting
    public static Map<String, String> parseToCertificateToPackageMap(String whitelistSetting) {
        String pairDelim = "\\s*;\\s*";
        String keyValueDelim = "\\s*:\\s*";
        List<String> keyValuePairList = Arrays.asList(whitelistSetting.split("\\s*;\\s*"));
        if (keyValuePairList.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new ArrayMap(keyValuePairList.size());
        for (String keyValueString : keyValuePairList) {
            String[] keyValue = keyValueString.split("\\s*:\\s*");
            if (keyValue.length == 2) {
                map.put(keyValue[0].toUpperCase(), keyValue[1]);
            } else {
                loge("Incorrect length of key-value pair in carrier app whitelist map.  Length should be exactly 2");
            }
        }
        return map;
    }

    public boolean isApplicationOnIcc(AppType type) {
        synchronized (this.mLock) {
            int i = 0;
            while (i < this.mUiccApplications.length) {
                if (this.mUiccApplications[i] == null || this.mUiccApplications[i].getType() != type) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    public PinState getUniversalPinState() {
        PinState pinState;
        synchronized (this.mLock) {
            pinState = this.mUniversalPinState;
        }
        return pinState;
    }

    public UiccCardApplication getApplication(int family) {
        synchronized (this.mLock) {
            int index = 8;
            switch (family) {
                case 1:
                    index = this.mGsmUmtsSubscriptionAppIndex;
                    break;
                case 2:
                    index = this.mCdmaSubscriptionAppIndex;
                    break;
                case 3:
                    try {
                        index = this.mImsSubscriptionAppIndex;
                        break;
                    } catch (Throwable th) {
                    }
            }
            if (index < 0 || this.mUiccApplications == null || index >= this.mUiccApplications.length) {
                return null;
            }
            UiccCardApplication uiccCardApplication = this.mUiccApplications[index];
            return uiccCardApplication;
        }
    }

    public UiccCardApplication getApplicationIndex(int index) {
        synchronized (this.mLock) {
            if (index >= 0) {
                try {
                    if (index < this.mUiccApplications.length) {
                        UiccCardApplication uiccCardApplication = this.mUiccApplications[index];
                        return uiccCardApplication;
                    }
                } catch (Throwable th) {
                }
            }
            return null;
        }
    }

    public UiccCardApplication getApplicationByType(int type) {
        synchronized (this.mLock) {
            int i = 0;
            while (i < this.mUiccApplications.length) {
                if (this.mUiccApplications[i] == null || this.mUiccApplications[i].getType().ordinal() != type) {
                    i++;
                } else {
                    UiccCardApplication uiccCardApplication = this.mUiccApplications[i];
                    return uiccCardApplication;
                }
            }
            return null;
        }
    }

    public boolean resetAppWithAid(String aid) {
        boolean changed;
        synchronized (this.mLock) {
            changed = false;
            int i = 0;
            while (i < this.mUiccApplications.length) {
                if (this.mUiccApplications[i] != null && (TextUtils.isEmpty(aid) || aid.equals(this.mUiccApplications[i].getAid()))) {
                    this.mUiccApplications[i].dispose();
                    this.mUiccApplications[i] = null;
                    changed = true;
                }
                i++;
            }
            if (TextUtils.isEmpty(aid) && this.mCarrierPrivilegeRules != null) {
                this.mCarrierPrivilegeRules = null;
                changed = true;
            }
        }
        return changed;
    }

    public void iccOpenLogicalChannel(String aid, int p2, Message response) {
        if (Log.HWINFO) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iccOpenLogicalChannel: ");
            stringBuilder.append(aid);
            stringBuilder.append(" , ");
            stringBuilder.append(p2);
            stringBuilder.append(" by pid:");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(" uid:");
            stringBuilder.append(Binder.getCallingUid());
            loglocal(stringBuilder.toString());
        }
        this.mCi.iccOpenLogicalChannel(aid, p2, this.mHandler.obtainMessage(8, response));
    }

    public void iccCloseLogicalChannel(int channel, Message response) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("iccCloseLogicalChannel: ");
        stringBuilder.append(channel);
        loglocal(stringBuilder.toString());
        this.mCi.iccCloseLogicalChannel(channel, this.mHandler.obtainMessage(9, response));
    }

    public void iccTransmitApduLogicalChannel(int channel, int cla, int command, int p1, int p2, int p3, String data, Message response) {
        this.mCi.iccTransmitApduLogicalChannel(channel, cla, command, p1, p2, p3, data, this.mHandler.obtainMessage(10, response));
    }

    public void iccTransmitApduBasicChannel(int cla, int command, int p1, int p2, int p3, String data, Message response) {
        this.mCi.iccTransmitApduBasicChannel(cla, command, p1, p2, p3, data, this.mHandler.obtainMessage(11, response));
    }

    public void iccExchangeSimIO(int fileID, int command, int p1, int p2, int p3, String pathID, Message response) {
        this.mCi.iccIO(command, fileID, pathID, p1, p2, p3, null, null, this.mHandler.obtainMessage(12, response));
    }

    public void sendEnvelopeWithStatus(String contents, Message response) {
        this.mCi.sendEnvelopeWithStatus(contents, response);
    }

    public int getNumApplications() {
        int count = 0;
        for (UiccCardApplication a : this.mUiccApplications) {
            if (a != null) {
                count++;
            }
        }
        return count;
    }

    public int getPhoneId() {
        return this.mPhoneId;
    }

    public boolean areCarrierPriviligeRulesLoaded() {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        return carrierPrivilegeRules == null || carrierPrivilegeRules.areCarrierPriviligeRulesLoaded();
    }

    public boolean hasCarrierPrivilegeRules() {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        return carrierPrivilegeRules != null && carrierPrivilegeRules.hasCarrierPrivilegeRules();
    }

    public int getCarrierPrivilegeStatus(Signature signature, String packageName) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return -1;
        }
        return carrierPrivilegeRules.getCarrierPrivilegeStatus(signature, packageName);
    }

    public int getCarrierPrivilegeStatus(PackageManager packageManager, String packageName) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return -1;
        }
        return carrierPrivilegeRules.getCarrierPrivilegeStatus(packageManager, packageName);
    }

    public int getCarrierPrivilegeStatus(PackageInfo packageInfo) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return -1;
        }
        return carrierPrivilegeRules.getCarrierPrivilegeStatus(packageInfo);
    }

    public int getCarrierPrivilegeStatusForCurrentTransaction(PackageManager packageManager) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return -1;
        }
        return carrierPrivilegeRules.getCarrierPrivilegeStatusForCurrentTransaction(packageManager);
    }

    public int getCarrierPrivilegeStatusForUid(PackageManager packageManager, int uid) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return -1;
        }
        return carrierPrivilegeRules.getCarrierPrivilegeStatusForUid(packageManager, uid);
    }

    public List<String> getCarrierPackageNamesForIntent(PackageManager packageManager, Intent intent) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return null;
        }
        return carrierPrivilegeRules.getCarrierPackageNamesForIntent(packageManager, intent);
    }

    private UiccCarrierPrivilegeRules getCarrierPrivilegeRules() {
        UiccCarrierPrivilegeRules uiccCarrierPrivilegeRules;
        synchronized (this.mLock) {
            uiccCarrierPrivilegeRules = this.mCarrierPrivilegeRules;
        }
        return uiccCarrierPrivilegeRules;
    }

    public boolean setOperatorBrandOverride(String brand) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setOperatorBrandOverride: ");
        stringBuilder.append(brand);
        log(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("current iccId: ");
        stringBuilder.append(SubscriptionInfo.givePrintableIccid(getIccId()));
        log(stringBuilder.toString());
        String iccId = getIccId();
        if (TextUtils.isEmpty(iccId)) {
            return false;
        }
        Editor spEditor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        String key = new StringBuilder();
        key.append(OPERATOR_BRAND_OVERRIDE_PREFIX);
        key.append(iccId);
        key = key.toString();
        if (brand == null) {
            spEditor.remove(key).commit();
        } else {
            spEditor.putString(key, brand).commit();
        }
        this.mOperatorBrandOverrideRegistrants.notifyRegistrants();
        return true;
    }

    public String getOperatorBrandOverride() {
        String iccId = getIccId();
        if (TextUtils.isEmpty(iccId)) {
            return null;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(OPERATOR_BRAND_OVERRIDE_PREFIX);
        stringBuilder.append(iccId);
        return sp.getString(stringBuilder.toString(), null);
    }

    public String getIccId() {
        for (UiccCardApplication app : this.mUiccApplications) {
            if (app != null) {
                IccRecords ir = app.getIccRecords();
                if (!(ir == null || ir.getIccId() == null)) {
                    return ir.getIccId();
                }
            }
        }
        return null;
    }

    private static void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private static void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    private void loglocal(String msg) {
        LocalLog localLog = UiccController.sLocalLog;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UiccProfile[");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append("]: ");
        stringBuilder.append(msg);
        localLog.log(stringBuilder.toString());
    }

    @VisibleForTesting
    public void refresh() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(13));
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        int i;
        StringBuilder stringBuilder;
        pw.println("UiccProfile:");
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mCi=");
        stringBuilder2.append(this.mCi);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mCatService=");
        stringBuilder2.append(this.mCatService);
        pw.println(stringBuilder2.toString());
        int i2 = 0;
        for (i = 0; i < this.mCarrierPrivilegeRegistrants.size(); i++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mCarrierPrivilegeRegistrants[");
            stringBuilder.append(i);
            stringBuilder.append("]=");
            stringBuilder.append(((Registrant) this.mCarrierPrivilegeRegistrants.get(i)).getHandler());
            pw.println(stringBuilder.toString());
        }
        for (i = 0; i < this.mOperatorBrandOverrideRegistrants.size(); i++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mOperatorBrandOverrideRegistrants[");
            stringBuilder.append(i);
            stringBuilder.append("]=");
            stringBuilder.append(((Registrant) this.mOperatorBrandOverrideRegistrants.get(i)).getHandler());
            pw.println(stringBuilder.toString());
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mUniversalPinState=");
        stringBuilder3.append(this.mUniversalPinState);
        pw.println(stringBuilder3.toString());
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mGsmUmtsSubscriptionAppIndex=");
        stringBuilder3.append(this.mGsmUmtsSubscriptionAppIndex);
        pw.println(stringBuilder3.toString());
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mCdmaSubscriptionAppIndex=");
        stringBuilder3.append(this.mCdmaSubscriptionAppIndex);
        pw.println(stringBuilder3.toString());
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mImsSubscriptionAppIndex=");
        stringBuilder3.append(this.mImsSubscriptionAppIndex);
        pw.println(stringBuilder3.toString());
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mUiccApplications: length=");
        stringBuilder3.append(this.mUiccApplications.length);
        pw.println(stringBuilder3.toString());
        for (i = 0; i < this.mUiccApplications.length; i++) {
            if (this.mUiccApplications[i] == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  mUiccApplications[");
                stringBuilder.append(i);
                stringBuilder.append("]=");
                stringBuilder.append(null);
                pw.println(stringBuilder.toString());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  mUiccApplications[");
                stringBuilder.append(i);
                stringBuilder.append("]=");
                stringBuilder.append(this.mUiccApplications[i].getType());
                stringBuilder.append(" ");
                stringBuilder.append(this.mUiccApplications[i]);
                pw.println(stringBuilder.toString());
            }
        }
        pw.println();
        for (UiccCardApplication app : this.mUiccApplications) {
            if (app != null) {
                app.dump(fd, pw, args);
                pw.println();
            }
        }
        for (UiccCardApplication app2 : this.mUiccApplications) {
            if (app2 != null) {
                IccRecords ir = app2.getIccRecords();
                if (ir != null) {
                    ir.dump(fd, pw, args);
                    pw.println();
                }
            }
        }
        if (this.mCarrierPrivilegeRules == null) {
            pw.println(" mCarrierPrivilegeRules: null");
        } else {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" mCarrierPrivilegeRules: ");
            stringBuilder3.append(this.mCarrierPrivilegeRules);
            pw.println(stringBuilder3.toString());
            this.mCarrierPrivilegeRules.dump(fd, pw, args);
        }
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mCarrierPrivilegeRegistrants: size=");
        stringBuilder3.append(this.mCarrierPrivilegeRegistrants.size());
        pw.println(stringBuilder3.toString());
        for (i = 0; i < this.mCarrierPrivilegeRegistrants.size(); i++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mCarrierPrivilegeRegistrants[");
            stringBuilder.append(i);
            stringBuilder.append("]=");
            stringBuilder.append(((Registrant) this.mCarrierPrivilegeRegistrants.get(i)).getHandler());
            pw.println(stringBuilder.toString());
        }
        pw.flush();
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
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mCurrentAppType=");
        stringBuilder2.append(this.mCurrentAppType);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mUiccCard=");
        stringBuilder2.append(this.mUiccCard);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mUiccApplication=");
        stringBuilder2.append(this.mUiccApplication);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mIccRecords=");
        stringBuilder2.append(this.mIccRecords);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mExternalState=");
        stringBuilder2.append(this.mExternalState);
        pw.println(stringBuilder2.toString());
        pw.flush();
    }

    public int getPhoneIdHw() {
        return this.mPhoneId;
    }

    public CommandsInterface getCiHw() {
        return this.mCi;
    }

    public static int getEventRadioOffOrUnavailableHw() {
        return 1;
    }

    public static int getEventAppReadyHw() {
        return 3;
    }

    public UiccCard getUiccCardHw() {
        return this.mUiccCard;
    }

    public IccRecords getIccRecordsHw() {
        return this.mIccRecords;
    }

    @Deprecated
    public void setRadioOnHw(boolean value) {
    }

    @Deprecated
    public void registerUiccCardEventsHw() {
    }

    @Deprecated
    public void unregisterUiccCardEventsHw() {
    }

    public void broadcastIccStateChangedIntentHw(String value, String reason) {
        broadcastIccStateChangedIntent(value, reason, this.mPhoneId);
    }

    public void setExternalStateHw(State newState) {
        setExternalState(newState);
    }

    public String getIccStateIntentStringHw(State state) {
        return getIccStateIntentString(state);
    }

    public int getGsmUmtsSubscriptionAppIndex() {
        return this.mGsmUmtsSubscriptionAppIndex;
    }

    public int getCdmaSubscriptionAppIndex() {
        return this.mCdmaSubscriptionAppIndex;
    }
}
