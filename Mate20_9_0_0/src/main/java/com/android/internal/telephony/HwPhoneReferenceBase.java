package com.android.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import huawei.cust.HwCfgFilePolicy;

public abstract class HwPhoneReferenceBase {
    private static final String EXTRA_LAA_STATE = "laa_state";
    private static final String LAA_STATE_CHANGE_ACTION = "com.huawei.laa.action.STATE_CHANGE_ACTION";
    private static String LOG_TAG = "HwPhoneReferenceBase";
    protected static final int NVCFG_RESULT_FAILED = 3;
    protected static final int NVCFG_RESULT_FINISHED = 1;
    protected static final int NVCFG_RESULT_MODEM_RESET = 2;
    protected static final int NVCFG_RESULT_REFRESHED = 0;
    protected static final String PROP_NVCFG_RESULT_FILE = "persist.radio.nvcfg_file";
    private GsmCdmaPhone mGsmCdmaPhone;
    int mPhoneId = this.mGsmCdmaPhone.getPhoneId();
    private String subTag;

    public HwPhoneReferenceBase(GsmCdmaPhone phone) {
        this.mGsmCdmaPhone = phone;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(LOG_TAG);
        stringBuilder.append("[");
        stringBuilder.append(this.mGsmCdmaPhone.getPhoneId());
        stringBuilder.append("]");
        this.subTag = stringBuilder.toString();
    }

    public boolean beforeHandleMessage(Message msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("beforeHandleMessage what = ");
        stringBuilder.append(msg.what);
        logd(stringBuilder.toString());
        boolean msgHandled = true;
        int i = msg.what;
        if (i != 104) {
            switch (i) {
                case 112:
                    logd("EVENT_HW_LAA_STATE_CHANGED");
                    onLaaStageChanged(msg);
                    break;
                case 113:
                    logd("EVENT_UNSOL_HW_CALL_ALT_SRV_DONE");
                    handleUnsolCallAltSrv(msg);
                    break;
                case 114:
                    logd("EVENT_UNSOL_SIM_NVCFG_FINISHED");
                    handleUnsolSimNvcfgChange(msg);
                    break;
                case 115:
                    logd("EVENT_GET_NVCFG_RESULT_INFO_DONE");
                    handleGetNvcfgResultInfoDone(msg);
                    break;
                default:
                    msgHandled = false;
                    if (msg.what >= 100) {
                        msgHandled = true;
                    }
                    if (!msgHandled) {
                        logd("unhandle event");
                        break;
                    }
                    break;
            }
        }
        AsyncResult ar = msg.obj;
        setEccNumbers((String) ar.result);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Handle EVENT_ECC_NUM:");
        stringBuilder2.append((String) ar.result);
        logd(stringBuilder2.toString());
        return msgHandled;
    }

    private void logd(String msg) {
        Rlog.d(this.subTag, msg);
    }

    private void loge(String msg) {
        Rlog.e(this.subTag, msg);
    }

    private void setEccNumbers(String value) {
        StringBuilder stringBuilder;
        try {
            if (!needSetEccNumbers()) {
                value = "";
            }
            if (this.mGsmCdmaPhone.getSubId() <= 0) {
                SystemProperties.set("ril.ecclist", value);
            } else {
                SystemProperties.set("ril.ecclist1", value);
            }
        } catch (RuntimeException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("setEccNumbers RuntimeException: ");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("setEccNumbers Exception: ");
            stringBuilder.append(e2);
            loge(stringBuilder.toString());
        }
    }

    private boolean needSetEccNumbers() {
        boolean z = true;
        if (!TelephonyManager.getDefault().isMultiSimEnabled() || !SystemProperties.getBoolean("ro.config.hw_ecc_with_sim_card", false)) {
            return true;
        }
        int i;
        boolean hasPresentCard = false;
        int simCount = TelephonyManager.getDefault().getSimCount();
        for (i = 0; i < simCount; i++) {
            if (TelephonyManager.getDefault().getSimState(i) != 1) {
                hasPresentCard = true;
                break;
            }
        }
        i = SubscriptionController.getInstance().getSlotIndex(this.mGsmCdmaPhone.getSubId());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("needSetEccNumbers  slotId = ");
        stringBuilder.append(i);
        stringBuilder.append(" hasPresentCard = ");
        stringBuilder.append(hasPresentCard);
        logd(stringBuilder.toString());
        if (hasPresentCard && TelephonyManager.getDefault().getSimState(i) == 1) {
            z = false;
        }
        return z;
    }

    protected void onLaaStageChanged(Message msg) {
        AsyncResult ar = msg.obj;
        if (ar == null || ar.exception != null) {
            logd("onLaaStageChanged:don't sendBroadcast LAA_STATE_CHANGE_ACTION");
            return;
        }
        int[] result = ar.result;
        Intent intent = new Intent(LAA_STATE_CHANGE_ACTION);
        intent.putExtra(EXTRA_LAA_STATE, result[0]);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendBroadcast com.huawei.laa.action.STATE_CHANGE_ACTION Laa_state=");
        stringBuilder.append(result[0]);
        logd(stringBuilder.toString());
        Context context = this.mGsmCdmaPhone.getContext();
        if (context != null) {
            context.sendBroadcast(intent);
        }
    }

    public void handleUnsolCallAltSrv(Message msg) {
        AsyncResult ar = msg.obj;
        logd("handleUnsolCallAltSrv");
        if (ar == null || ar.exception != null) {
            logd("handleUnsolCallAltSrv: ar or ar.exception is null");
            return;
        }
        IPhoneCallback callback = ar.userObj;
        if (callback != null) {
            try {
                callback.onCallback1(this.mPhoneId);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleUnsolCallAltSrv,onCallback1 for subId=");
                stringBuilder.append(this.mPhoneId);
                logd(stringBuilder.toString());
                return;
            } catch (RemoteException ex) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("handleUnsolCallAltSrv:onCallback1 RemoteException:");
                stringBuilder2.append(ex);
                logd(stringBuilder2.toString());
                return;
            }
        }
        logd("handleUnsolCallAltSrv: callback is null");
    }

    public boolean virtualNetEccFormCarrier(int mPhoneId) {
        try {
            Boolean supportVmEccState = (Boolean) HwCfgFilePolicy.getValue("support_vn_ecc", SubscriptionManager.getSlotIndex(mPhoneId), Boolean.class);
            if (supportVmEccState != null) {
                return supportVmEccState.booleanValue();
            }
            return false;
        } catch (Exception e) {
            logd("Failed to get support_vm_ecc in carrier");
            return false;
        }
    }

    protected void handleUnsolSimNvcfgChange(Message msg) {
        if (msg == null) {
            loge("handleUnsolSimNvcfgChange: msg is null, return.");
            return;
        }
        AsyncResult ar = msg.obj;
        if (ar == null || ar.exception != null) {
            loge("handleUnsolSimNvcfgChange: ar is null or exception occurs.");
        } else {
            int nvcfgResult = ((Integer) ar.result).intValue();
            if (nvcfgResult != 0 && HwTelephonyManagerInner.getDefault().getDefault4GSlotId() == this.mPhoneId) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleUnsolSimNvcfgChange: nvcfgResult=");
                stringBuilder.append(nvcfgResult);
                stringBuilder.append(", get NVCFG for mainSlot:");
                stringBuilder.append(this.mPhoneId);
                logd(stringBuilder.toString());
                this.mGsmCdmaPhone.mCi.getNvcfgMatchedResult(this.mGsmCdmaPhone.obtainMessage(115));
            }
        }
    }

    protected void handleGetNvcfgResultInfoDone(Message msg) {
        if (HwTelephonyManagerInner.getDefault().getDefault4GSlotId() != this.mPhoneId || msg == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleGetNvcfgResultDone: mPhoneId=");
            stringBuilder.append(this.mPhoneId);
            stringBuilder.append(" is not main Slot, or msg is null, return.");
            loge(stringBuilder.toString());
            return;
        }
        AsyncResult ar = msg.obj;
        if (ar == null || ar.exception != null) {
            loge("handleGetNvcfgResultDone: ar is null or exception occurs.");
        } else {
            String nvcfgResultInfo = ar.result;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("handleGetNvcfgResultDone: nvcfgResultInfo=");
            stringBuilder2.append(nvcfgResultInfo);
            logd(stringBuilder2.toString());
            if (!TextUtils.isEmpty(nvcfgResultInfo)) {
                try {
                    SystemProperties.set(PROP_NVCFG_RESULT_FILE, nvcfgResultInfo);
                } catch (RuntimeException e) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("handleGetNvcfgResultDone: RuntimeException e=");
                    stringBuilder3.append(e);
                    loge(stringBuilder3.toString());
                }
            }
        }
    }
}
