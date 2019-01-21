package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandException.Error;
import java.util.ArrayList;

public class HwHisiCsgSearch extends CsgSearch {
    private static final String LOG_TAG = "HwHisiCsgSearch";
    public static final boolean isVZW;

    private class CSGNetworkList {
        public ArrayList<HwHisiCsgNetworkInfo> mCSGNetworks = new ArrayList();
        private HwHisiCsgNetworkInfo mCurSelectingCsgNetwork = null;

        public CSGNetworkList(ArrayList<HwHisiCsgNetworkInfo> csgNetworkInfos) {
            copyFrom(csgNetworkInfos);
        }

        private void copyFrom(ArrayList<HwHisiCsgNetworkInfo> csgNetworkInfos) {
            int csgNetworkInfosSize = csgNetworkInfos.size();
            for (int i = 0; i < csgNetworkInfosSize; i++) {
                this.mCSGNetworks.add((HwHisiCsgNetworkInfo) csgNetworkInfos.get(i));
            }
        }

        public HwHisiCsgNetworkInfo getCurrentSelectingCsgNetwork() {
            return this.mCurSelectingCsgNetwork;
        }

        public boolean isToBeSearchedCsgListsEmpty() {
            return this.mCSGNetworks.isEmpty();
        }

        public HwHisiCsgNetworkInfo getToBeRegsiteredCSGNetwork() {
            this.mCurSelectingCsgNetwork = null;
            if (this.mCSGNetworks == null) {
                Rlog.e(HwHisiCsgSearch.LOG_TAG, "=csg= input param is null, not should be here!");
                return this.mCurSelectingCsgNetwork;
            }
            int list_size = this.mCSGNetworks.size();
            for (int i = 0; i < list_size; i++) {
                HwHisiCsgNetworkInfo csgInfo = (HwHisiCsgNetworkInfo) this.mCSGNetworks.get(i);
                if (!csgInfo.isSelectedFail) {
                    this.mCurSelectingCsgNetwork = csgInfo;
                    break;
                }
                Rlog.d(HwHisiCsgSearch.LOG_TAG, "=csg=  had selected and failed, so not reselect again!");
            }
            return this.mCurSelectingCsgNetwork;
        }
    }

    static {
        boolean z = "389".equals(SystemProperties.get("ro.config.hw_opta")) && "840".equals(SystemProperties.get("ro.config.hw_optb"));
        isVZW = z;
    }

    public HwHisiCsgSearch(GsmCdmaPhone phone) {
        super(phone);
    }

    public void handleMessage(Message msg) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("msg id is ");
        stringBuilder.append(msg.what);
        Rlog.d(str, stringBuilder.toString());
        int i = msg.what;
        AsyncResult ar;
        String str2;
        if (i == 1) {
            Rlog.d(LOG_TAG, "=csg=  Receved EVENT_SELECT_CSG_NETWORK_DONE.");
            ar = (AsyncResult) msg.obj;
            if (ar == null) {
                Rlog.e(LOG_TAG, "=csg=  ar is null, the code should never come here!!");
                return;
            }
            Message onComplete = ar.userObj;
            if (onComplete == null) {
                Rlog.e(LOG_TAG, "=csg=  ar.userObj is null, the code should never come here!!");
            } else if (ar.exception != null) {
                String str3 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("=csg= select CSG failed! ");
                stringBuilder2.append(ar.exception);
                Rlog.e(str3, stringBuilder2.toString());
                if (isVZW) {
                    AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                    onComplete.sendToTarget();
                    return;
                }
                CSGNetworkList csgNetworklist = onComplete.obj.result;
                HwHisiCsgNetworkInfo curSelectingCsgNetwork = csgNetworklist.getCurrentSelectingCsgNetwork();
                if (curSelectingCsgNetwork == null) {
                    Rlog.i(LOG_TAG, "=csg= current select CSG is null->maybe loop end. response result.");
                    AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                    onComplete.sendToTarget();
                    return;
                }
                curSelectingCsgNetwork.isSelectedFail = true;
                str2 = LOG_TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("=csg= mark  current CSG-ID item Failed! ");
                stringBuilder3.append(csgNetworklist.mCurSelectingCsgNetwork);
                Rlog.e(str2, stringBuilder3.toString());
                selectCSGNetwork(onComplete);
            } else {
                AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                onComplete.sendToTarget();
            }
        } else if (i != 5) {
            super.handleMessage(msg);
        } else {
            Rlog.i(LOG_TAG, "=csg= Receved EVENT_CSG_PERIODIC_SCAN_DONE.");
            ar = msg.obj;
            if (ar == null) {
                Rlog.e(LOG_TAG, "=csg= ar is null, the code should never come here!!");
            } else if (ar.exception != null || ar.result == null) {
                str2 = LOG_TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("=csg= Periodic Search: get avaiable CSG list failed! ");
                stringBuilder4.append(ar.exception);
                Rlog.e(str2, stringBuilder4.toString());
            } else {
                CSGNetworkList csgNetworklist2 = ar.result;
                Rlog.d(LOG_TAG, "=csg= Periodic Search: get avaiable CSG list success -> select Csg! ");
                if (csgNetworklist2.isToBeSearchedCsgListsEmpty()) {
                    Rlog.i(LOG_TAG, "=csg= Periodic Search: no avaiable CSG-ID -> cancel periodic search! ");
                    cancelCsgPeriodicSearchTimer();
                    return;
                }
                selectCSGNetwork(obtainMessage(6, ar));
            }
        }
    }

    void getAvailableCSGNetworks(Message response) {
        Rlog.d(LOG_TAG, "=csg=  getAvailableCSGNetworks...");
        this.mPhone.mCi.getAvailableCSGNetworks(obtainMessage(null, response));
    }

    void handleCsgNetworkQueryResult(AsyncResult ar) {
        Rlog.d(LOG_TAG, "=csg=  handleCsgNetworkQueryResult...");
        if (ar == null || ar.userObj == null) {
            Rlog.e(LOG_TAG, "=csg=  ar or userObj is null, the code should never come here!!");
        } else if (ar.exception != null) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("=csg=  exception happen: ");
            stringBuilder.append(ar.exception);
            Rlog.e(str, stringBuilder.toString());
            AsyncResult.forMessage((Message) ar.userObj, null, ar.exception);
            ((Message) ar.userObj).sendToTarget();
        } else {
            if (ar.result == null) {
                Rlog.e(LOG_TAG, "=csg=  result is null: ");
                AsyncResult.forMessage((Message) ar.userObj, null, new CommandException(Error.GENERIC_FAILURE));
                ((Message) ar.userObj).sendToTarget();
            } else if (isVZW) {
                AsyncResult.forMessage((Message) ar.userObj, (ArrayList) ar.result, null);
                ((Message) ar.userObj).sendToTarget();
            } else {
                AsyncResult.forMessage((Message) ar.userObj, new CSGNetworkList((ArrayList) ar.result), null);
                ((Message) ar.userObj).sendToTarget();
            }
        }
    }

    void selectCSGNetwork(Message response) {
        AsyncResult ar = response.obj;
        if (ar == null || ar.result == null) {
            Rlog.e(LOG_TAG, "=csg= parsed CSG list is null, return exception");
            AsyncResult.forMessage(response, null, new CommandException(Error.GENERIC_FAILURE));
            response.sendToTarget();
            return;
        }
        CSGNetworkList csgNetworklist = ar.result;
        if (csgNetworklist.mCSGNetworks.size() > 0) {
            HwHisiCsgNetworkInfo curSelCsgNetwork = csgNetworklist.getToBeRegsiteredCSGNetwork();
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("to be registered CSG info is ");
            stringBuilder.append(curSelCsgNetwork);
            Rlog.d(str, stringBuilder.toString());
            if (curSelCsgNetwork != null && !curSelCsgNetwork.isEmpty()) {
                this.mPhone.mCi.setCSGNetworkSelectionModeManual(curSelCsgNetwork, obtainMessage(1, response));
                return;
            } else if (curSelCsgNetwork == null || !curSelCsgNetwork.isEmpty()) {
                Rlog.e(LOG_TAG, "=csg= not find suitable CSG-ID, Select CSG fail!");
                AsyncResult.forMessage(response, null, new CommandException(Error.GENERIC_FAILURE));
                response.sendToTarget();
                return;
            } else {
                Rlog.e(LOG_TAG, "=csg= not find suitable CSG-ID, so finish Select! ");
                AsyncResult.forMessage(response, null, null);
                response.sendToTarget();
                return;
            }
        }
        Rlog.e(LOG_TAG, "=csg= mCSGNetworks is not initailized, return with exception");
        AsyncResult.forMessage(response, null, new CommandException(Error.GENERIC_FAILURE));
        response.sendToTarget();
    }

    public void selectExtendersCSGNetwork(HwHisiCsgNetworkInfo csgNetworkInfo, Message response) {
        if (csgNetworkInfo != null && !csgNetworkInfo.isEmpty()) {
            Rlog.e(LOG_TAG, "Select csg !");
            this.mPhone.mCi.setCSGNetworkSelectionModeManual(csgNetworkInfo, obtainMessage(1, response));
        }
    }
}
