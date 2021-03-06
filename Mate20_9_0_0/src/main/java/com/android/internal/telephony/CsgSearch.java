package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import com.android.internal.telephony.uicc.IccRecords;

public abstract class CsgSearch extends Handler {
    protected static final int EVENT_CSG_MANUAL_SCAN_DONE = 2;
    protected static final int EVENT_CSG_MANUAL_SELECT_DONE = 3;
    protected static final int EVENT_CSG_OCSGL_LOADED = 7;
    protected static final int EVENT_CSG_PERIODIC_SCAN_DONE = 5;
    protected static final int EVENT_CSG_PERIODIC_SEARCH_TIMEOUT = 4;
    protected static final int EVENT_CSG_PERIODIC_SELECT_DONE = 6;
    protected static final int EVENT_GET_AVAILABLE_CSG_NETWORK_DONE = 0;
    protected static final int EVENT_SELECT_CSG_NETWORK_DONE = 1;
    private static final String LOG_TAG = "CsgSearch";
    protected static final String OPERATOR_NAME_ATT_MICROCELL = "AT&T MicroCell";
    protected static final int TIMER_CSG_PERIODIC_SEARCH = 7200000;
    public static final boolean isVZW;
    protected static boolean mIsSupportCsgSearch = SystemProperties.getBoolean("ro.config.att.csg", false);
    protected GsmCdmaPhone mPhone;

    abstract void getAvailableCSGNetworks(Message message);

    abstract void handleCsgNetworkQueryResult(AsyncResult asyncResult);

    abstract void selectCSGNetwork(Message message);

    static {
        boolean z = false;
        if ("389".equals(SystemProperties.get("ro.config.hw_opta")) && "840".equals(SystemProperties.get("ro.config.hw_optb"))) {
            z = true;
        }
        isVZW = z;
    }

    public static boolean isSupportCsgSearch() {
        return mIsSupportCsgSearch;
    }

    public CsgSearch(GsmCdmaPhone phone) {
        this.mPhone = phone;
    }

    public void handleMessage(Message msg) {
        AsyncResult ar;
        String str;
        StringBuilder stringBuilder;
        switch (msg.what) {
            case 0:
                Rlog.d(LOG_TAG, "=csg= Receved EVENT_GET_AVAILABLE_CSG_NETWORK_DONE.");
                ar = (AsyncResult) msg.obj;
                if (ar == null) {
                    Rlog.e(LOG_TAG, "=csg=  ar is null, the code should never come here!!");
                    return;
                } else {
                    handleCsgNetworkQueryResult(ar);
                    return;
                }
            case 2:
                Rlog.d(LOG_TAG, "=csg= Receved EVENT_CSG_MANUAL_SCAN_DONE.");
                ar = (AsyncResult) msg.obj;
                if (ar == null) {
                    Rlog.e(LOG_TAG, "=csg= ar is null, the code should never come here!!");
                    return;
                } else if (ar.exception != null) {
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("=csg= Manual Search: get avaiable CSG list failed! -> response ");
                    stringBuilder.append(ar.exception);
                    Rlog.e(str, stringBuilder.toString());
                    AsyncResult.forMessage((Message) ar.userObj, null, ar.exception);
                    ((Message) ar.userObj).sendToTarget();
                    return;
                } else {
                    Rlog.i(LOG_TAG, "=csg= Manual Search: get avaiable CSG list success -> select Csg! ");
                    if (isVZW) {
                        AsyncResult.forMessage((Message) ar.userObj, ar.result, null);
                        ((Message) ar.userObj).sendToTarget();
                        return;
                    }
                    selectCSGNetwork(obtainMessage(EVENT_CSG_MANUAL_SELECT_DONE, ar));
                    return;
                }
            case EVENT_CSG_MANUAL_SELECT_DONE /*3*/:
                Rlog.d(LOG_TAG, "=csg= EVENT_CSG_MANUAL_SELECT_DONE!");
                ar = (AsyncResult) msg.obj;
                if (ar == null) {
                    Rlog.e(LOG_TAG, "=csg= ar is null, the code should never come here!!");
                    return;
                }
                if (ar.exception != null) {
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("=csg= Manual Search: CSG-ID selection is failed! ");
                    stringBuilder.append(ar.exception);
                    Rlog.i(str, stringBuilder.toString());
                } else {
                    Rlog.i(LOG_TAG, "=csg= Manual Search: CSG-ID selection is success! ");
                }
                AsyncResult arUsrObj = ar.userObj;
                if (arUsrObj == null) {
                    Rlog.e(LOG_TAG, "=csg= ar is null, the code should never come here!!");
                    return;
                }
                AsyncResult.forMessage((Message) arUsrObj.userObj, null, ar.exception);
                ((Message) arUsrObj.userObj).sendToTarget();
                return;
            case 4:
                Rlog.d(LOG_TAG, "=csg= EVENT_CSG_PERIODIC_SEARCH_TIMEOUT!");
                trigerPeriodicCsgSearch();
                Rlog.d(LOG_TAG, "=csg=  launch next Csg Periodic search timer!");
                judgeToLaunchCsgPeriodicSearchTimer();
                return;
            case EVENT_CSG_PERIODIC_SELECT_DONE /*6*/:
                Rlog.d(LOG_TAG, "=csg= EVENT_CSG_PERIODIC_SELECT_DONE!");
                ar = msg.obj;
                if (ar == null) {
                    Rlog.e(LOG_TAG, "=csg= ar is null, the code should never come here!!");
                    return;
                } else if (ar.exception != null) {
                    String str2 = LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("=csg= Periodic Search: CSG-ID selection is failed! ");
                    stringBuilder2.append(ar.exception);
                    Rlog.e(str2, stringBuilder2.toString());
                    return;
                } else {
                    Rlog.e(LOG_TAG, "=csg= Periodic Search: CSG-ID selection is success! ");
                    return;
                }
            case EVENT_CSG_OCSGL_LOADED /*7*/:
                Rlog.d(LOG_TAG, "=csg= EVENT_CSG_OCSGL_LOADED!");
                judgeToLaunchCsgPeriodicSearchTimer();
                return;
            default:
                String str3 = LOG_TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("unexpected event not handled: ");
                stringBuilder3.append(msg.what);
                Rlog.e(str3, stringBuilder3.toString());
                return;
        }
    }

    public void registerForCsgRecordsLoadedEvent() {
        IccRecords r = (IccRecords) this.mPhone.mIccRecords.get();
        if (r != null) {
            r.registerForCsgRecordsLoaded(this, EVENT_CSG_OCSGL_LOADED, null);
        }
    }

    public void unregisterForCsgRecordsLoadedEvent() {
        IccRecords r = (IccRecords) this.mPhone.mIccRecords.get();
        if (r != null) {
            r.unregisterForCsgRecordsLoaded(this);
        }
    }

    public boolean isCsgAwareUicc() {
        IccRecords r = (IccRecords) this.mPhone.mIccRecords.get();
        if (r == null || r.getOcsgl().length > 0 || r.getCsglexist()) {
            return true;
        }
        Rlog.d(LOG_TAG, "=csg=  EF-Operator not present =>CSG not Aware UICC");
        return false;
    }

    public void judgeToLaunchCsgPeriodicSearchTimer() {
        if (!CsgSearchFactory.isHisiChipset()) {
            boolean isLaunchTimer = false;
            IccRecords r = (IccRecords) this.mPhone.mIccRecords.get();
            ServiceState ss = this.mPhone.getServiceState();
            String operatorAlpha = SystemProperties.get("gsm.operator.alpha", "");
            if (!(ss == null || ((ss.getVoiceRegState() != 0 && ss.getDataRegState() != 0) || ss.getRoaming() || operatorAlpha == null || OPERATOR_NAME_ATT_MICROCELL.equals(operatorAlpha)))) {
                isLaunchTimer = true;
            }
            if (isLaunchTimer && r != null) {
                byte[] csgLists = r.getOcsgl();
                if (true == r.getCsglexist() && csgLists.length == 0) {
                    Rlog.d(LOG_TAG, "=csg= EFOCSGL is empty, not trigger periodic search!");
                    isLaunchTimer = false;
                }
            }
            if (isLaunchTimer) {
                launchCsgPeriodicSearchTimer();
            } else {
                cancelCsgPeriodicSearchTimer();
            }
        }
    }

    public void launchCsgPeriodicSearchTimer() {
        if (!hasMessages(4)) {
            Rlog.d(LOG_TAG, "=csg= lauch periodic search timer!");
            sendEmptyMessageDelayed(4, 7200000);
        }
    }

    public void cancelCsgPeriodicSearchTimer() {
        if (hasMessages(4)) {
            Rlog.d(LOG_TAG, "=csg= cancel periodic search timer!");
            removeMessages(4);
        }
    }

    private void trigerPeriodicCsgSearch() {
        getAvailableCSGNetworks(obtainMessage(5));
    }

    public void selectCsgNetworkManually(Message response) {
        Rlog.i(LOG_TAG, "start manual select CSG network...");
        getAvailableCSGNetworks(obtainMessage(2, response));
    }

    public void selectExtendersCSGNetwork(HwHisiCsgNetworkInfo csgNetworkInfo, Message response) {
        Rlog.i(LOG_TAG, "selectExtendersCSGNetwork...");
    }
}
