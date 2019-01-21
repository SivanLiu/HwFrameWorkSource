package com.android.internal.telephony;

import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.CellLocation;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.TimeUtils;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.vsim.HwVSimController;
import java.io.FileDescriptor;
import java.io.PrintWriter;

abstract class HwVSimSSTBridge extends ServiceStateTracker {
    private String LOG_TAG = "VSimSSTBridge";
    private String mCurDataSpn = null;
    private String mCurPlmn = null;
    private boolean mCurShowPlmn = false;
    private boolean mCurShowSpn = false;
    private String mCurSpn = null;
    private CellLocation mNewCellLoc;
    private GsmCdmaPhone mPhone;

    /* renamed from: com.android.internal.telephony.HwVSimSSTBridge$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$CommandsInterface$RadioState = new int[RadioState.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$CommandsInterface$RadioState[RadioState.RADIO_UNAVAILABLE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandsInterface$RadioState[RadioState.RADIO_OFF.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    protected abstract Intent createSpnIntent();

    protected abstract String getNetworkOperatorForPhone(TelephonyManager telephonyManager, int i);

    protected abstract UiccCard getUiccCard();

    protected abstract UiccCardApplication getVSimUiccCardApplication();

    protected abstract void initUiccController();

    protected abstract boolean isUiccControllerValid();

    protected abstract void putPhoneIdAndSubIdExtra(Intent intent, int i);

    protected abstract void registerForIccChanged();

    protected abstract void setNetworkCountryIsoForPhone(TelephonyManager telephonyManager, int i, String str);

    protected abstract void setNetworkOperatorNumericForPhone(TelephonyManager telephonyManager, int i, String str);

    protected abstract void unregisterForIccChanged();

    protected abstract void updateVSimOperatorProp();

    public HwVSimSSTBridge(GsmCdmaPhone phone, CommandsInterface ci) {
        super(phone, ci);
        initOnce(phone, ci);
    }

    private void initOnce(GsmCdmaPhone phone, CommandsInterface ci) {
        this.mPhone = phone;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.LOG_TAG);
        stringBuilder.append("[SUB");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("]");
        this.LOG_TAG = stringBuilder.toString();
        initUiccController();
        registerForIccChanged();
        this.mNewCellLoc = new GsmCellLocation();
    }

    public void dispose() {
        unregisterForIccChanged();
        super.dispose();
    }

    public void handleMessage(Message msg) {
        if (this.mPhone.isPhoneTypeGsm()) {
            int i = msg.what;
            super.handleMessage(msg);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("not gsm phone, not handle message: ");
        stringBuilder.append(msg.what);
        log(stringBuilder.toString());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("HwVSimSSTBridge extends:");
        super.dump(fd, pw, args);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" mPhone=");
        stringBuilder.append(this.mPhone);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCurSpn=");
        stringBuilder.append(this.mCurSpn);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCurDataSpn=");
        stringBuilder.append(this.mCurDataSpn);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCurShowSpn=");
        stringBuilder.append(this.mCurShowSpn);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCurPlmn=");
        stringBuilder.append(this.mCurPlmn);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCurShowPlmn=");
        stringBuilder.append(this.mCurShowPlmn);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mNewCellLoc=");
        stringBuilder.append(this.mNewCellLoc);
        pw.println(stringBuilder.toString());
    }

    public void updateSpnDisplay() {
        boolean showPlmn;
        String plmn;
        IccRecords iccRecords = this.mIccRecords;
        int rule = iccRecords != null ? iccRecords.getDisplayRule(this.mSS) : 0;
        int ruleFromApk = HwVSimController.getInstance().getRule();
        if (ruleFromApk != -1) {
            rule = ruleFromApk;
        }
        int combinedRegState = HwTelephonyFactory.getHwNetworkManager().getGsmCombinedRegState(this, this.mPhone, this.mSS);
        StringBuilder stringBuilder;
        if (combinedRegState == 1 || combinedRegState == 2) {
            showPlmn = true;
            plmn = Resources.getSystem().getText(17040350).toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateSpnDisplay: radio is on but out of service, set plmn='");
            stringBuilder.append(plmn);
            stringBuilder.append("'");
            log(stringBuilder.toString());
        } else if (combinedRegState == 0) {
            plmn = this.mSS.getOperatorAlphaLong();
            boolean z = !TextUtils.isEmpty(plmn) && (rule & 2) == 2;
            showPlmn = z;
        } else {
            showPlmn = true;
            plmn = Resources.getSystem().getText(17040350).toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateSpnDisplay: radio is off w/ showPlmn=");
            stringBuilder.append(true);
            stringBuilder.append(" plmn=");
            stringBuilder.append(plmn);
            log(stringBuilder.toString());
        }
        String spn = iccRecords != null ? iccRecords.getServiceProviderName() : "";
        String spnFromApk = HwVSimController.getInstance().getSpn();
        if (!TextUtils.isEmpty(spnFromApk)) {
            spn = spnFromApk;
        }
        String dataSpn = spn;
        boolean showSpn = !TextUtils.isEmpty(spn) && (rule & 1) == 1;
        if (this.mSS.getVoiceRegState() == 3 || (showPlmn && TextUtils.equals(spn, plmn))) {
            spn = null;
            showSpn = false;
        }
        if (!(showPlmn == this.mCurShowPlmn && showSpn == this.mCurShowSpn && TextUtils.equals(spn, this.mCurSpn) && TextUtils.equals(dataSpn, this.mCurDataSpn) && TextUtils.equals(plmn, this.mCurPlmn))) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateSpnDisplay: changed sending intent rule=");
            stringBuilder2.append(rule);
            stringBuilder2.append(" showPlmn='%b' plmn='%s' showSpn='%b' spn='%s' dataSpn='%s'");
            log(String.format(stringBuilder2.toString(), new Object[]{Boolean.valueOf(showPlmn), plmn, Boolean.valueOf(showSpn), spn, dataSpn}));
            updateVSimOperatorProp();
            Intent intent = createSpnIntent();
            intent.addFlags(536870912);
            intent.putExtra("showSpn", showSpn);
            intent.putExtra("spn", spn);
            intent.putExtra("spnData", dataSpn);
            intent.putExtra("showPlmn", showPlmn);
            intent.putExtra("plmn", plmn);
            putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
            this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
        this.mCurShowSpn = showSpn;
        this.mCurShowPlmn = showPlmn;
        this.mCurSpn = spn;
        this.mCurDataSpn = dataSpn;
        this.mCurPlmn = plmn;
    }

    protected void onUpdateIccAvailability() {
        if (isUiccControllerValid()) {
            UiccCardApplication newUiccApplication = getVSimUiccCardApplication();
            if (this.mUiccApplcation != newUiccApplication) {
                if (this.mUiccApplcation != null) {
                    log("Removing stale icc objects.");
                    this.mUiccApplcation.unregisterForReady(this);
                    this.mUiccApplcation.unregisterForGetAdDone(this);
                    if (this.mIccRecords != null) {
                        this.mIccRecords.unregisterForRecordsLoaded(this);
                    }
                    this.mIccRecords = null;
                    this.mUiccApplcation = null;
                }
                if (newUiccApplication != null) {
                    log("New card found");
                    this.mUiccApplcation = newUiccApplication;
                    this.mIccRecords = this.mUiccApplcation.getIccRecords();
                    if (this.mPhone.isPhoneTypeGsm()) {
                        this.mUiccApplcation.registerForReady(this, 17, null);
                        this.mUiccApplcation.registerForGetAdDone(this, HwFullNetworkConstants.EVENT_RADIO_UNAVAILABLE, null);
                        if (this.mIccRecords != null) {
                            this.mIccRecords.registerForRecordsLoaded(this, 16, null);
                        }
                    }
                }
            }
        }
    }

    protected void handlePollStateResult(int what, AsyncResult ar) {
        if (ar.userObj == this.mPollingContext) {
            StringBuilder stringBuilder;
            if (ar.exception != null) {
                Error err = null;
                if (ar.exception instanceof CommandException) {
                    err = ((CommandException) ar.exception).getCommandError();
                }
                if (err == Error.RADIO_NOT_AVAILABLE) {
                    cancelPollState();
                    return;
                } else if (err != Error.OP_NOT_ALLOWED_BEFORE_REG_NW) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("RIL implementation has returned an error where it must succeed");
                    stringBuilder.append(ar.exception);
                    loge(stringBuilder.toString());
                }
            } else {
                try {
                    handlePollStateResultMessage(what, ar);
                } catch (RuntimeException ex) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception while polling service state. Probably malformed RIL response.");
                    stringBuilder.append(ex);
                    loge(stringBuilder.toString());
                }
            }
            int[] iArr = this.mPollingContext;
            iArr[0] = iArr[0] - 1;
            if (this.mPollingContext[0] == 0) {
                updateRoamingState();
                pollStateDone();
            }
        }
    }

    public void pollState(boolean modemTriggered) {
        this.mPollingContext = new int[1];
        this.mPollingContext[0] = 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pollState: modemTriggered=");
        stringBuilder.append(modemTriggered);
        log(stringBuilder.toString());
        switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$CommandsInterface$RadioState[this.mCi.getRadioState().ordinal()]) {
            case 1:
                this.mNewSS.setStateOutOfService();
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                pollStateDone();
                return;
            case 2:
                this.mNewSS.setStateOff();
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                if (!(modemTriggered || 18 == this.mSS.getRilDataRadioTechnology())) {
                    pollStateDone();
                    return;
                }
        }
        int[] iArr = this.mPollingContext;
        iArr[0] = iArr[0] + 1;
        this.mCi.getOperator(obtainMessage(6, this.mPollingContext));
        iArr = this.mPollingContext;
        iArr[0] = iArr[0] + 1;
        ((NetworkRegistrationManager) getRegStateManagers().get(1)).getNetworkRegistrationState(2, obtainMessage(5, this.mPollingContext));
        iArr = this.mPollingContext;
        iArr[0] = iArr[0] + 1;
        ((NetworkRegistrationManager) getRegStateManagers().get(1)).getNetworkRegistrationState(1, obtainMessage(4, this.mPollingContext));
        if (this.mPhone.isPhoneTypeGsm()) {
            iArr = this.mPollingContext;
            iArr[0] = iArr[0] + 1;
            this.mCi.getNetworkSelectionMode(obtainMessage(14, this.mPollingContext));
        }
    }

    private void pollStateDone() {
        if (this.mPhone.isPhoneTypeGsm()) {
            pollStateDoneGsm();
        }
    }

    private void pollStateDoneGsm() {
        boolean hasOperatorNumericChanged;
        boolean hasLocationChanged;
        boolean needNotifyData;
        boolean hasLacChanged;
        useDataRegStateForDataOnlyDevices();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Poll ServiceState done:  oldSS=[");
        stringBuilder.append(this.mSS);
        stringBuilder.append("] newSS=[");
        stringBuilder.append(this.mNewSS);
        stringBuilder.append("]");
        log(stringBuilder.toString());
        boolean z = this.mSS.getVoiceRegState() != 0 && this.mNewSS.getVoiceRegState() == 0;
        boolean hasRegistered = z;
        z = this.mSS.getVoiceRegState() == 0 && this.mNewSS.getVoiceRegState() != 0;
        boolean hasDeregistered = z;
        z = this.mSS.getDataRegState() != 0 && this.mNewSS.getDataRegState() == 0;
        boolean hasDataAttached = z;
        z = this.mSS.getDataRegState() == 0 && this.mNewSS.getDataRegState() != 0;
        boolean hasDataDetached = z;
        boolean hasDataRegStateChanged = this.mSS.getDataRegState() != this.mNewSS.getDataRegState();
        if (this.mNewSS.getOperatorNumeric() != null) {
            hasOperatorNumericChanged = this.mNewSS.getOperatorNumeric().equals(this.mSS.getOperatorNumeric()) ^ true;
        } else {
            hasOperatorNumericChanged = false;
        }
        boolean hasRilDataRadioTechnologyChanged = this.mSS.getRilDataRadioTechnology() != this.mNewSS.getRilDataRadioTechnology();
        boolean hasChanged = this.mNewSS.equals(this.mSS) ^ 1;
        boolean hasLocationChanged2 = this.mNewCellLoc.equals(this.mCellLoc) ^ 1;
        boolean needNotifyData2 = this.mSS.getCssIndicator() != this.mNewSS.getCssIndicator();
        boolean hasLacChanged2 = ((GsmCellLocation) this.mNewCellLoc).isNotLacEquals((GsmCellLocation) this.mCellLoc);
        TelephonyManager tm = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        if (hasDataAttached) {
            log("service state hasRegistered , poll signal strength at once");
            sendMessage(obtainMessage(10));
        }
        ServiceState tss = this.mSS;
        this.mSS = this.mNewSS;
        this.mNewSS = tss;
        this.mNewSS.setStateOutOfService();
        GsmCellLocation tcl = (GsmCellLocation) this.mCellLoc;
        this.mCellLoc = this.mNewCellLoc;
        this.mNewCellLoc = tcl;
        if (hasRilDataRadioTechnologyChanged) {
            tm.setDataNetworkTypeForPhone(this.mPhone.getPhoneId(), this.mSS.getRilDataRadioTechnology());
        }
        if (hasRegistered || hasOperatorNumericChanged) {
            if (SystemClock.elapsedRealtime() - this.mLastReceivedNITZReferenceTime > 5000) {
                getNitzState().handleNetworkAvailable();
            }
        } else {
            ServiceState serviceState = tss;
        }
        if (hasDeregistered) {
            getNitzState().handleNetworkUnavailable();
        }
        boolean z2;
        boolean hasRilDataRadioTechnologyChanged2;
        boolean z3;
        if (hasChanged) {
            updateSpnDisplay();
            tm.setNetworkOperatorNameForPhone(this.mPhone.getPhoneId(), this.mSS.getOperatorAlphaLong());
            updateVSimOperatorProp();
            String prevOperatorNumeric = getNetworkOperatorForPhone(tm, this.mPhone.getPhoneId());
            String prevCountryIsoCode = getNetworkCountryIsoForPhone();
            hasRegistered = this.mSS.getOperatorNumeric();
            setNetworkOperatorNumericForPhone(tm, this.mPhone.getPhoneId(), hasRegistered);
            updateCarrierMccMncConfiguration(hasRegistered, prevOperatorNumeric, this.mPhone.getContext());
            if (TextUtils.isEmpty(hasRegistered)) {
                log("operatorNumeric is null");
                setNetworkCountryIsoForPhone(tm, this.mPhone.getPhoneId(), "");
                getNitzState().handleNetworkUnavailable();
                z2 = hasOperatorNumericChanged;
                hasRilDataRadioTechnologyChanged2 = hasRilDataRadioTechnologyChanged;
                z3 = hasChanged;
                hasLocationChanged = hasLocationChanged2;
                needNotifyData = needNotifyData2;
                hasLacChanged = hasLacChanged2;
            } else {
                if (this.mSS.getRilDataRadioTechnology() != 18) {
                    String countryIsoCode;
                    String countryIsoCode2 = "";
                    try {
                        countryIsoCode = MccTable.countryCodeForMcc(Integer.parseInt(hasRegistered.substring(null, 3)));
                        z2 = hasOperatorNumericChanged;
                    } catch (NumberFormatException | StringIndexOutOfBoundsException ex) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("pollStateDone: countryCodeForMcc error: ");
                        stringBuilder2.append(ex);
                        loge(stringBuilder2.toString());
                        countryIsoCode = countryIsoCode2;
                    }
                    setNetworkCountryIsoForPhone(tm, this.mPhone.getPhoneId(), countryIsoCode);
                    z = iccCardExists();
                    hasOperatorNumericChanged = networkCountryIsoChanged(countryIsoCode, prevCountryIsoCode);
                    boolean z4 = z && hasOperatorNumericChanged;
                    boolean countryChanged = z4;
                    needNotifyData = needNotifyData2;
                    hasLacChanged = hasLacChanged2;
                    needNotifyData2 = System.currentTimeMillis();
                    StringBuilder stringBuilder3 = new StringBuilder();
                    hasLocationChanged = hasLocationChanged2;
                    stringBuilder3.append("Before handleNetworkCountryCodeKnown: countryChanged=");
                    hasLocationChanged2 = countryChanged;
                    stringBuilder3.append(hasLocationChanged2);
                    hasRilDataRadioTechnologyChanged2 = hasRilDataRadioTechnologyChanged;
                    stringBuilder3.append(" iccCardExist=");
                    stringBuilder3.append(z);
                    stringBuilder3.append(" countryIsoChanged=");
                    stringBuilder3.append(hasOperatorNumericChanged);
                    stringBuilder3.append(" operatorNumeric=");
                    stringBuilder3.append(hasRegistered);
                    stringBuilder3.append(" prevOperatorNumeric=");
                    stringBuilder3.append(prevOperatorNumeric);
                    stringBuilder3.append(" countryIsoCode=");
                    stringBuilder3.append(countryIsoCode);
                    stringBuilder3.append(" prevCountryIsoCode=");
                    stringBuilder3.append(prevCountryIsoCode);
                    stringBuilder3.append(" ltod=");
                    stringBuilder3.append(TimeUtils.logTimeOfDay(needNotifyData2));
                    log(stringBuilder3.toString());
                    getNitzState().handleNetworkCountryCodeSet(hasLocationChanged2);
                } else {
                    hasRilDataRadioTechnologyChanged2 = hasRilDataRadioTechnologyChanged;
                    z3 = hasChanged;
                    hasLocationChanged = hasLocationChanged2;
                    needNotifyData = needNotifyData2;
                    hasLacChanged = hasLacChanged2;
                }
            }
            tm.setNetworkRoamingForPhone(this.mPhone.getPhoneId(), this.mSS.getVoiceRoaming());
            setRoamingType(this.mSS);
            stringBuilder = new StringBuilder();
            stringBuilder.append("Broadcasting ServiceState : ");
            stringBuilder.append(this.mSS);
            log(stringBuilder.toString());
            this.mPhone.notifyServiceStateChanged(this.mSS);
        } else {
            boolean z5 = hasDeregistered;
            z2 = hasOperatorNumericChanged;
            hasRilDataRadioTechnologyChanged2 = hasRilDataRadioTechnologyChanged;
            z3 = hasChanged;
            hasLocationChanged = hasLocationChanged2;
            needNotifyData = needNotifyData2;
            hasLacChanged = hasLacChanged2;
        }
        if (hasDataAttached) {
            this.mAttachedRegistrants.notifyRegistrants();
        }
        if (hasDataDetached) {
            this.mDetachedRegistrants.notifyRegistrants();
        }
        if (hasDataRegStateChanged || hasRilDataRadioTechnologyChanged) {
            notifyDataRegStateRilRadioTechnologyChanged();
            needNotifyData2 = true;
        } else {
            needNotifyData2 = needNotifyData;
        }
        if (needNotifyData2) {
            this.mPhone.notifyDataConnection(null);
        }
        if (hasLocationChanged) {
            this.mPhone.notifyLocationChanged();
        }
        if (hasLacChanged) {
            Rlog.i(this.LOG_TAG, "LAC changed, update operator name display");
            updateSpnDisplay();
        }
    }

    private void setSignalStrengthDefaultValues() {
        this.mSignalStrength = new SignalStrength(true);
    }

    protected String getNetworkCountryIsoForPhone() {
        return SystemProperties.get("gsm.operator.iso-country.vsim", "");
    }

    private boolean networkCountryIsoChanged(String newCountryIsoCode, String prevCountryIsoCode) {
        if (TextUtils.isEmpty(newCountryIsoCode)) {
            log("countryIsoChanged: no new country ISO code");
            return false;
        } else if (!TextUtils.isEmpty(prevCountryIsoCode)) {
            return newCountryIsoCode.equals(prevCountryIsoCode) ^ 1;
        } else {
            log("countryIsoChanged: no previous country ISO code");
            return true;
        }
    }

    private boolean iccCardExists() {
        if (this.mUiccApplcation == null) {
            return false;
        }
        return this.mUiccApplcation.getState() != AppState.APPSTATE_UNKNOWN;
    }
}
