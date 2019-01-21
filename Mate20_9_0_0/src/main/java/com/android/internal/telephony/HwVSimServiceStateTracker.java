package com.android.internal.telephony;

import android.content.Intent;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.uicc.HwVSimUiccController;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.vsim.HwVSimConstants;
import java.io.FileDescriptor;
import java.io.PrintWriter;

final class HwVSimServiceStateTracker extends HwVSimSSTBridge {
    private static final int EVENT_NETWORK_REJECTED_CASE = 106;
    private static final int EVENT_RPLMNS_STATE_CHANGED = 105;
    private static final int EVENT_VSIM_AP_TRAFFIC = 103;
    private static final int EVENT_VSIM_BASE = 100;
    private static final int EVENT_VSIM_PLMN_SELINFO = 102;
    private static final int EVENT_VSIM_RDH_NEEDED = 101;
    private static final int EVENT_VSIM_TIMER_TASK_EXPIRED = 104;
    private static final String PROPERTY_GLOBAL_OPERATOR_NUMERIC = "ril.operator.numeric";
    private static final int VSIM_AUTH_FAIL_CAUSE = 65538;
    private static final boolean sIsPlatformSupportVSim = SystemProperties.getBoolean("ro.radio.vsim_support", false);
    private String LOG_TAG = "VSimSST";
    private HwVSimUiccController mVSimUiccController;

    public HwVSimServiceStateTracker(GsmCdmaPhone phone, CommandsInterface ci) {
        super(phone, ci);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.LOG_TAG);
        stringBuilder.append("[VSIM]");
        this.LOG_TAG = stringBuilder.toString();
        this.mCi.setOnVsimRDH(this, EVENT_VSIM_RDH_NEEDED, null);
        this.mCi.setOnVsimRegPLMNSelInfo(this, EVENT_VSIM_PLMN_SELINFO, null);
        this.mCi.setOnVsimApDsFlowInfo(this, EVENT_VSIM_AP_TRAFFIC, null);
        this.mCi.setOnVsimTimerTaskExpired(this, EVENT_VSIM_TIMER_TASK_EXPIRED, null);
        this.mCi.registerForRplmnsStateChanged(this, EVENT_RPLMNS_STATE_CHANGED, null);
        sendMessage(obtainMessage(EVENT_RPLMNS_STATE_CHANGED));
        this.mCi.setOnNetReject(this, EVENT_NETWORK_REJECTED_CASE, null);
        if (sIsPlatformSupportVSim && getPhoneId() == 2) {
            initUiccController();
            registerForIccChanged();
        }
    }

    public void dispose() {
        log("ServiceStateTracker dispose");
        this.mCi.unSetOnVsimRDH(this);
        this.mCi.unSetOnVsimRegPLMNSelInfo(this);
        this.mCi.unSetOnVsimApDsFlowInfo(this);
        this.mCi.unSetOnVsimTimerTaskExpired(this);
        this.mCi.unregisterForRplmnsStateChanged(this);
        this.mCi.unSetOnNetReject(this);
        if (sIsPlatformSupportVSim && getPhoneId() == 2) {
            unregisterForIccChanged();
        }
        super.dispose();
    }

    public void handleMessage(Message msg) {
        AsyncResult ar;
        switch (msg.what) {
            case EVENT_VSIM_RDH_NEEDED /*101*/:
                log("EVENT_VSIM_RDH_NEEDED");
                sendBroadcastRDHNeeded();
                return;
            case EVENT_VSIM_PLMN_SELINFO /*102*/:
                log("EVENT_VSIM_PLMN_SELINFO");
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    int[] response = ar.result;
                    if (response.length != 0) {
                        sendBroadcastRegPLMNSelInfo(response[0], response[1]);
                        return;
                    }
                    return;
                }
                return;
            case EVENT_VSIM_AP_TRAFFIC /*103*/:
                log("EVENT_VSIM_AP_TRAFFIC");
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    String[] response2 = ar.result;
                    if (response2.length != 0) {
                        sendBroadcastApTraffic(response2[0], response2[1], response2[2], response2[3], response2[4], response2[5], response2[6]);
                        return;
                    }
                    return;
                }
                return;
            case EVENT_VSIM_TIMER_TASK_EXPIRED /*104*/:
                log("EVENT_VSIM_TIMER_TASK_EXPIRED");
                ar = msg.obj;
                if (ar.exception == null) {
                    sendBroadcastTimerTaskExpired(((int[]) ar.result)[0]);
                    return;
                }
                return;
            case EVENT_RPLMNS_STATE_CHANGED /*105*/:
                log("EVENT_RPLMNS_STATE_CHANGED");
                sendBroadcastRPLMNChanged(SystemProperties.get(PROPERTY_GLOBAL_OPERATOR_NUMERIC, ""));
                return;
            case EVENT_NETWORK_REJECTED_CASE /*106*/:
                log("EVENT_NETWORK_REJECTED_CASE");
                onNetworkReject(msg.obj);
                return;
            default:
                super.handleMessage(msg);
                return;
        }
    }

    protected void updateVSimOperatorProp() {
        if (this.mSS != null) {
            SystemProperties.set("gsm.operator.alpha.vsim", this.mSS.getOperatorAlphaLong());
        }
    }

    protected UiccCardApplication getVSimUiccCardApplication() {
        HwVSimUiccController hwVSimUiccController = this.mVSimUiccController;
        HwVSimUiccController hwVSimUiccController2 = this.mVSimUiccController;
        return hwVSimUiccController.getUiccCardApplication(1);
    }

    protected void initUiccController() {
        this.mVSimUiccController = HwVSimUiccController.getInstance();
    }

    protected void registerForIccChanged() {
        this.mVSimUiccController.registerForIccChanged(this, 42, null);
    }

    protected void unregisterForIccChanged() {
        this.mVSimUiccController.unregisterForIccChanged(this);
    }

    protected boolean isUiccControllerValid() {
        return this.mVSimUiccController != null;
    }

    protected UiccCard getUiccCard() {
        return this.mVSimUiccController.getUiccCard();
    }

    protected Intent createSpnIntent() {
        return new Intent("com.huawei.vsim.action.SPN_STRINGS_UPDATED_VSIM");
    }

    protected void putPhoneIdAndSubIdExtra(Intent intent, int phoneId) {
        intent.putExtra("subscription", 2);
        intent.putExtra("phone", 2);
        intent.putExtra("slot", 2);
    }

    protected void setNetworkCountryIsoForPhone(TelephonyManager tm, int phoneId, String iso) {
        SystemProperties.set("gsm.operator.iso-country.vsim", iso);
    }

    protected String getNetworkOperatorForPhone(TelephonyManager tm, int phoneId) {
        return SystemProperties.get("gsm.operator.numeric.vsim", "");
    }

    protected void setNetworkOperatorNumericForPhone(TelephonyManager tm, int phoneId, String numeric) {
        SystemProperties.set("gsm.operator.numeric.vsim", numeric);
    }

    protected void log(String s) {
        String str = this.LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[VSimSST] ");
        stringBuilder.append(s);
        Rlog.d(str, stringBuilder.toString());
    }

    protected void loge(String s) {
        String str = this.LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[VSimSST] ");
        stringBuilder.append(s);
        Rlog.e(str, stringBuilder.toString());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("VSimServiceStateTracker extends:");
        super.dump(fd, pw, args);
    }

    private void onNetworkReject(AsyncResult ar) {
        if (ar.exception == null) {
            String[] data = ar.result;
            String rejectplmn = null;
            int rejectdomain = -1;
            int rejectcause = -1;
            int rejectrat = -1;
            if (data.length > 0) {
                try {
                    if (data[0] != null && data[0].length() > 0) {
                        rejectplmn = data[0];
                    }
                    if (data[1] != null && data[1].length() > 0) {
                        rejectdomain = Integer.parseInt(data[1]);
                    }
                    if (data[2] != null && data[2].length() > 0) {
                        rejectcause = Integer.parseInt(data[2]);
                    }
                    if (data[3] != null && data[3].length() > 0) {
                        rejectrat = Integer.parseInt(data[3]);
                    }
                } catch (Exception ex) {
                    Rlog.e(this.LOG_TAG, "error parsing NetworkReject!", ex);
                }
                String str = this.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("NetworkReject:PLMN = ");
                stringBuilder.append(rejectplmn);
                stringBuilder.append(" domain = ");
                stringBuilder.append(rejectdomain);
                stringBuilder.append(" cause = ");
                stringBuilder.append(rejectcause);
                stringBuilder.append(" RAT = ");
                stringBuilder.append(rejectrat);
                Rlog.d(str, stringBuilder.toString());
                if (VSIM_AUTH_FAIL_CAUSE == rejectcause) {
                    sendBroadcastRejInfo(rejectcause, rejectplmn);
                }
            }
        }
    }

    private void sendBroadcastRDHNeeded() {
        log("sendBroadcastRDHNeeded");
        String SUB_ID = HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID;
        Intent intent = new Intent("com.huawei.vsim.action.NEED_NEGOTIATION");
        int subId = getPhoneId();
        intent.putExtra(HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID, subId);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("subId: ");
        stringBuilder.append(subId);
        log(stringBuilder.toString());
        getPhone().getContext().sendBroadcast(intent, HwVSimConstants.VSIM_BUSSINESS_PERMISSION);
    }

    public void sendBroadcastRejInfo(int rejectcause, String plmn) {
        log("sendBroadcastRejInfo");
        String SUB_ID = HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID;
        String ERR_CODE = "errcode";
        String PLMN = "plmn";
        int subId = getPhoneId();
        Intent intent = new Intent("com.huawei.vsim.action.SIM_REJINFO_ACTION");
        intent.putExtra(HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID, subId);
        intent.putExtra("errcode", rejectcause);
        intent.putExtra("plmn", plmn);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("subId: ");
        stringBuilder.append(subId);
        stringBuilder.append(", rejectcause: ");
        stringBuilder.append(rejectcause);
        stringBuilder.append(", plmn: ");
        stringBuilder.append(plmn);
        log(stringBuilder.toString());
        getPhone().getContext().sendBroadcast(intent, HwVSimConstants.VSIM_BUSSINESS_PERMISSION);
    }

    public void sendBroadcastRPLMNChanged(String rplmn) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendBroadcastRPLMNChanged rplmn: ");
        stringBuilder.append(rplmn);
        log(stringBuilder.toString());
        String SUB_ID = HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID;
        String RESIDENT = "resident";
        int subId = getPhoneId();
        Intent intent = new Intent("com.huawei.vsim.action.SIM_RESIDENT_PLMN");
        intent.putExtra(HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID, subId);
        intent.putExtra("resident", rplmn);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("subId: ");
        stringBuilder2.append(subId);
        stringBuilder2.append(" resident: ");
        stringBuilder2.append(rplmn);
        log(stringBuilder2.toString());
        getPhone().getContext().sendBroadcast(intent, HwVSimConstants.VSIM_BUSSINESS_PERMISSION);
    }

    private void sendBroadcastRegPLMNSelInfo(int flag, int result) {
        log("sendBroadcastRegPLMNSelInfo");
        String SUB_ID = HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID;
        String FLAG = "flag";
        String RES = "res";
        Intent intent = new Intent("com.huawei.vsim.action.SIM_PLMN_SELINFO");
        int subId = getPhoneId();
        intent.putExtra(HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID, subId);
        intent.putExtra("flag", flag);
        intent.putExtra("res", result);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("subId: ");
        stringBuilder.append(subId);
        stringBuilder.append(" flag: ");
        stringBuilder.append(flag);
        stringBuilder.append(" result: ");
        stringBuilder.append(result);
        log(stringBuilder.toString());
        getPhone().getContext().sendBroadcast(intent, HwVSimConstants.VSIM_BUSSINESS_PERMISSION);
    }

    private void sendBroadcastApTraffic(String curr_ds_time, String tx_rate, String rx_rate, String curr_tx_flow, String curr_rx_flow, String total_tx_flow, String total_rx_flow) {
        String str = curr_ds_time;
        String str2 = tx_rate;
        String str3 = rx_rate;
        String str4 = curr_tx_flow;
        String str5 = curr_rx_flow;
        String str6 = total_tx_flow;
        String str7 = total_rx_flow;
        log("sendBroadcastApTraffic");
        String CURR_TX_FLOW = "curr_tx_flow";
        String CURR_RX_FLOW = "curr_rx_flow";
        String TOTAL_TX_FLOW = "total_tx_flow";
        String TOTAL_RX_FLOW = "total_rx_flow";
        String SUB_ID = HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID;
        String CURR_DS_TIME = "curr_ds_time";
        Intent intent = new Intent("com.huawei.vsim.action.SIM_TRAFFIC");
        int subId = getPhoneId();
        String TX_RATE = "tx_rate";
        intent.putExtra(HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID, subId);
        intent.putExtra("curr_ds_time", str);
        intent.putExtra("tx_rate", str2);
        intent.putExtra("rx_rate", str3);
        intent.putExtra("curr_tx_flow", str4);
        intent.putExtra("curr_rx_flow", str5);
        intent.putExtra("total_tx_flow", str6);
        intent.putExtra("total_rx_flow", str7);
        StringBuilder stringBuilder = new StringBuilder();
        String RX_RATE = "rx_rate";
        stringBuilder.append("subId: ");
        stringBuilder.append(subId);
        stringBuilder.append(" curr_ds_time: ");
        stringBuilder.append(str);
        stringBuilder.append(" tx_rate: ");
        stringBuilder.append(str2);
        stringBuilder.append(" rx_rate: ");
        stringBuilder.append(str3);
        stringBuilder.append(" curr_tx_flow: ");
        stringBuilder.append(str4);
        stringBuilder.append(" curr_rx_flow: ");
        stringBuilder.append(str5);
        stringBuilder.append(" total_tx_flow: ");
        stringBuilder.append(str6);
        stringBuilder.append(" total_rx_flow: ");
        stringBuilder.append(str7);
        log(stringBuilder.toString());
        getPhone().getContext().sendBroadcast(intent, HwVSimConstants.VSIM_BUSSINESS_PERMISSION);
    }

    private void sendBroadcastTimerTaskExpired(int type) {
        log("sendBroadcastTimerTaskExpired");
        String SUB_ID = HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID;
        String TYPE = HwVSimConstants.EXTRA_NETWORK_SCAN_TYPE;
        Intent intent = new Intent("com.huawei.vsim.action.TIMERTASK_EXPIRED_ACTION");
        int subId = getPhoneId();
        intent.putExtra(HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID, subId);
        intent.putExtra(HwVSimConstants.EXTRA_NETWORK_SCAN_TYPE, type);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("subId: ");
        stringBuilder.append(subId);
        stringBuilder.append(" type: ");
        stringBuilder.append(type);
        log(stringBuilder.toString());
        getPhone().getContext().sendBroadcast(intent, HwVSimConstants.VSIM_BUSSINESS_PERMISSION);
    }
}
