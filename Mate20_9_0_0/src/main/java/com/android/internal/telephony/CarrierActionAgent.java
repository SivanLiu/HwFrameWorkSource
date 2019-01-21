package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.provider.Settings.Global;
import android.provider.Telephony.Carriers;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class CarrierActionAgent extends Handler {
    public static final int CARRIER_ACTION_REPORT_DEFAULT_NETWORK_STATUS = 3;
    public static final int CARRIER_ACTION_RESET = 2;
    public static final int CARRIER_ACTION_SET_METERED_APNS_ENABLED = 0;
    public static final int CARRIER_ACTION_SET_RADIO_ENABLED = 1;
    private static final boolean DBG = true;
    public static final int EVENT_APM_SETTINGS_CHANGED = 4;
    public static final int EVENT_APN_SETTINGS_CHANGED = 8;
    public static final int EVENT_DATA_ROAMING_OFF = 6;
    public static final int EVENT_MOBILE_DATA_SETTINGS_CHANGED = 5;
    public static final int EVENT_SIM_STATE_CHANGED = 7;
    private static final String LOG_TAG = "CarrierActionAgent";
    private static final boolean VDBG = Rlog.isLoggable(LOG_TAG, 2);
    private Boolean mCarrierActionOnMeteredApnEnabled = Boolean.valueOf(true);
    private Boolean mCarrierActionOnRadioEnabled = Boolean.valueOf(true);
    private Boolean mCarrierActionReportDefaultNetworkStatus = Boolean.valueOf(false);
    private RegistrantList mDefaultNetworkReportRegistrants = new RegistrantList();
    private RegistrantList mMeteredApnEnableRegistrants = new RegistrantList();
    private LocalLog mMeteredApnEnabledLog = new LocalLog(10);
    private final Phone mPhone;
    private RegistrantList mRadioEnableRegistrants = new RegistrantList();
    private LocalLog mRadioEnabledLog = new LocalLog(10);
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String iccState = intent.getStringExtra("ss");
            if ("android.intent.action.SIM_STATE_CHANGED".equals(action) && !intent.getBooleanExtra("rebroadcastOnUnlock", false)) {
                CarrierActionAgent.this.sendMessage(CarrierActionAgent.this.obtainMessage(7, iccState));
            }
        }
    };
    private LocalLog mReportDefaultNetworkStatusLog = new LocalLog(10);
    private final SettingsObserver mSettingsObserver;

    public CarrierActionAgent(Phone phone) {
        this.mPhone = phone;
        this.mPhone.getContext().registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
        this.mSettingsObserver = new SettingsObserver(this.mPhone.getContext(), this);
        log("Creating CarrierActionAgent");
    }

    public void handleMessage(Message msg) {
        Boolean enabled = getCarrierActionEnabled(msg.what);
        if (enabled == null || enabled.booleanValue() != ((Boolean) msg.obj).booleanValue()) {
            StringBuilder stringBuilder;
            LocalLog localLog;
            StringBuilder stringBuilder2;
            switch (msg.what) {
                case 0:
                    this.mCarrierActionOnMeteredApnEnabled = Boolean.valueOf(((Boolean) msg.obj).booleanValue());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("SET_METERED_APNS_ENABLED: ");
                    stringBuilder.append(this.mCarrierActionOnMeteredApnEnabled);
                    log(stringBuilder.toString());
                    localLog = this.mMeteredApnEnabledLog;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("SET_METERED_APNS_ENABLED: ");
                    stringBuilder2.append(this.mCarrierActionOnMeteredApnEnabled);
                    localLog.log(stringBuilder2.toString());
                    this.mMeteredApnEnableRegistrants.notifyRegistrants(new AsyncResult(null, this.mCarrierActionOnMeteredApnEnabled, null));
                    break;
                case 1:
                    this.mCarrierActionOnRadioEnabled = Boolean.valueOf(((Boolean) msg.obj).booleanValue());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("SET_RADIO_ENABLED: ");
                    stringBuilder.append(this.mCarrierActionOnRadioEnabled);
                    log(stringBuilder.toString());
                    localLog = this.mRadioEnabledLog;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("SET_RADIO_ENABLED: ");
                    stringBuilder2.append(this.mCarrierActionOnRadioEnabled);
                    localLog.log(stringBuilder2.toString());
                    this.mRadioEnableRegistrants.notifyRegistrants(new AsyncResult(null, this.mCarrierActionOnRadioEnabled, null));
                    break;
                case 2:
                    log("CARRIER_ACTION_RESET");
                    carrierActionReset();
                    break;
                case 3:
                    this.mCarrierActionReportDefaultNetworkStatus = Boolean.valueOf(((Boolean) msg.obj).booleanValue());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("CARRIER_ACTION_REPORT_AT_DEFAULT_NETWORK_STATUS: ");
                    stringBuilder.append(this.mCarrierActionReportDefaultNetworkStatus);
                    log(stringBuilder.toString());
                    localLog = this.mReportDefaultNetworkStatusLog;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("REGISTER_DEFAULT_NETWORK_STATUS: ");
                    stringBuilder2.append(this.mCarrierActionReportDefaultNetworkStatus);
                    localLog.log(stringBuilder2.toString());
                    this.mDefaultNetworkReportRegistrants.notifyRegistrants(new AsyncResult(null, this.mCarrierActionReportDefaultNetworkStatus, null));
                    break;
                case 4:
                    log("EVENT_APM_SETTINGS_CHANGED");
                    if (Global.getInt(this.mPhone.getContext().getContentResolver(), "airplane_mode_on", 0) != 0) {
                        carrierActionReset();
                        break;
                    }
                    break;
                case 5:
                    log("EVENT_MOBILE_DATA_SETTINGS_CHANGED");
                    if (!this.mPhone.isUserDataEnabled()) {
                        carrierActionReset();
                        break;
                    }
                    break;
                case 6:
                    log("EVENT_DATA_ROAMING_OFF");
                    carrierActionReset();
                    break;
                case 7:
                    String iccState = msg.obj;
                    if (!"LOADED".equals(iccState)) {
                        if ("ABSENT".equals(iccState)) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("EVENT_SIM_STATE_CHANGED status: ");
                            stringBuilder2.append(iccState);
                            log(stringBuilder2.toString());
                            carrierActionReset();
                            this.mSettingsObserver.unobserve();
                            if (this.mPhone.getServiceStateTracker() != null) {
                                this.mPhone.getServiceStateTracker().unregisterForDataRoamingOff(this);
                                break;
                            }
                        }
                    }
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("EVENT_SIM_STATE_CHANGED status: ");
                    stringBuilder3.append(iccState);
                    log(stringBuilder3.toString());
                    carrierActionReset();
                    String mobileData = "mobile_data";
                    if (TelephonyManager.getDefault().getSimCount() != 1) {
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append(mobileData);
                        stringBuilder4.append(this.mPhone.getSubId());
                        mobileData = stringBuilder4.toString();
                    }
                    this.mSettingsObserver.observe(Global.getUriFor(mobileData), 5);
                    this.mSettingsObserver.observe(Global.getUriFor("airplane_mode_on"), 4);
                    this.mSettingsObserver.observe(Carriers.CONTENT_URI, 8);
                    if (this.mPhone.getServiceStateTracker() != null) {
                        this.mPhone.getServiceStateTracker().registerForDataRoamingOff(this, 6, null, false);
                        break;
                    }
                    break;
                case 8:
                    log("EVENT_APN_SETTINGS_CHANGED");
                    carrierActionReset();
                    break;
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown carrier action: ");
                    stringBuilder.append(msg.what);
                    loge(stringBuilder.toString());
                    break;
            }
        }
    }

    public void carrierActionSetRadioEnabled(boolean enabled) {
        sendMessage(obtainMessage(1, Boolean.valueOf(enabled)));
    }

    public void carrierActionSetMeteredApnsEnabled(boolean enabled) {
        sendMessage(obtainMessage(0, Boolean.valueOf(enabled)));
    }

    public void carrierActionReportDefaultNetworkStatus(boolean report) {
        sendMessage(obtainMessage(3, Boolean.valueOf(report)));
    }

    private void carrierActionReset() {
        carrierActionReportDefaultNetworkStatus(false);
        carrierActionSetMeteredApnsEnabled(true);
        carrierActionSetRadioEnabled(true);
        this.mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(new Intent("com.android.internal.telephony.CARRIER_SIGNAL_RESET"));
    }

    private RegistrantList getRegistrantsFromAction(int action) {
        if (action == 3) {
            return this.mDefaultNetworkReportRegistrants;
        }
        switch (action) {
            case 0:
                return this.mMeteredApnEnableRegistrants;
            case 1:
                return this.mRadioEnableRegistrants;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported action: ");
                stringBuilder.append(action);
                loge(stringBuilder.toString());
                return null;
        }
    }

    private Boolean getCarrierActionEnabled(int action) {
        if (action == 3) {
            return this.mCarrierActionReportDefaultNetworkStatus;
        }
        switch (action) {
            case 0:
                return this.mCarrierActionOnMeteredApnEnabled;
            case 1:
                return this.mCarrierActionOnRadioEnabled;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported action: ");
                stringBuilder.append(action);
                loge(stringBuilder.toString());
                return null;
        }
    }

    public void registerForCarrierAction(int action, Handler h, int what, Object obj, boolean notifyNow) {
        Boolean carrierAction = getCarrierActionEnabled(action);
        if (carrierAction != null) {
            RegistrantList list = getRegistrantsFromAction(action);
            Registrant r = new Registrant(h, what, obj);
            list.add(r);
            if (notifyNow) {
                r.notifyRegistrant(new AsyncResult(null, carrierAction, null));
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid carrier action: ");
        stringBuilder.append(action);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void unregisterForCarrierAction(Handler h, int action) {
        RegistrantList list = getRegistrantsFromAction(action);
        if (list != null) {
            list.remove(h);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid carrier action: ");
        stringBuilder.append(action);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    @VisibleForTesting
    public ContentObserver getContentObserver() {
        return this.mSettingsObserver;
    }

    private void log(String s) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("]");
        stringBuilder.append(s);
        Rlog.d(str, stringBuilder.toString());
    }

    private void loge(String s) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("]");
        stringBuilder.append(s);
        Rlog.e(str, stringBuilder.toString());
    }

    private void logv(String s) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("]");
        stringBuilder.append(s);
        Rlog.v(str, stringBuilder.toString());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        pw.println(" mCarrierActionOnMeteredApnsEnabled Log:");
        ipw.increaseIndent();
        this.mMeteredApnEnabledLog.dump(fd, ipw, args);
        ipw.decreaseIndent();
        pw.println(" mCarrierActionOnRadioEnabled Log:");
        ipw.increaseIndent();
        this.mRadioEnabledLog.dump(fd, ipw, args);
        ipw.decreaseIndent();
        pw.println(" mCarrierActionReportDefaultNetworkStatus Log:");
        ipw.increaseIndent();
        this.mReportDefaultNetworkStatusLog.dump(fd, ipw, args);
        ipw.decreaseIndent();
    }
}
