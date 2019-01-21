package com.android.internal.telephony.cdma;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.HwTelephony.NumMatchs;
import android.provider.HwTelephony.VirtualNets;
import android.provider.Settings.System;
import android.provider.Telephony.GlobalMatchs;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.PhoneStateListener;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.ims.HwImsManagerInner;
import com.android.ims.ImsException;
import com.android.internal.telephony.AbstractGsmCdmaPhone.CDMAPhoneReference;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.HwPhoneReferenceBase;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.IccRecords;
import huawei.cust.HwCfgFilePolicy;

public class HwCDMAPhoneReference extends HwPhoneReferenceBase implements CDMAPhoneReference {
    private static final int ECC_NOCARD_INDEX = 4;
    private static final int ECC_WITHCARD_INDEX = 3;
    private static final int EVENT_RADIO_ON = 5;
    private static final String LOG_TAG = "HwCDMAPhoneReference";
    private static final int MEID_LENGTH = 14;
    private static final int NAME_INDEX = 1;
    private static final int NUMERIC_INDEX = 2;
    private static final String PROPERTY_GLOBAL_FORCE_TO_SET_ECC = "ril.force_to_set_ecc";
    private static CDMAPhoneUtils cdmaPhoneUtils = new CDMAPhoneUtils();
    private int mLteReleaseVersion;
    private String mPESN;
    private GsmCdmaPhone mPhone;
    private int mPhoneId = 0;
    private final PhoneStateListener mPhoneStateListener;
    private int mSlotId = 0;
    private TelephonyManager mTelephonyManager;
    private String preOperatorNumeric = "";
    private String subTag;

    public HwCDMAPhoneReference(GsmCdmaPhone cdmaPhone) {
        super(cdmaPhone);
        this.mPhone = cdmaPhone;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwCDMAPhoneReference[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("]");
        this.subTag = stringBuilder.toString();
        this.mPhoneId = this.mPhone.getPhoneId();
        this.mSlotId = SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mPhoneId);
        this.mTelephonyManager = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        this.mPhoneStateListener = new PhoneStateListener(Integer.valueOf(this.mSlotId)) {
            public void onServiceStateChanged(ServiceState serviceState) {
                String hplmn = null;
                if (HwCDMAPhoneReference.this.mPhone.mIccRecords.get() != null) {
                    hplmn = ((IccRecords) HwCDMAPhoneReference.this.mPhone.mIccRecords.get()).getOperatorNumeric();
                }
                if (TelephonyManager.getDefault().getCurrentPhoneType(HwCDMAPhoneReference.this.mSlotId) != 2 || !SystemProperties.getBoolean("ro.config.hw_eccNumUseRplmn", false)) {
                    return;
                }
                if (TelephonyManager.getDefault().isNetworkRoaming(HwCDMAPhoneReference.this.mSlotId)) {
                    HwCDMAPhoneReference.this.globalEccCustom(serviceState.getOperatorNumeric());
                } else if (hplmn != null) {
                    HwCDMAPhoneReference.this.globalEccCustom(hplmn);
                }
            }
        };
        startListen();
    }

    public String getMeid() {
        logd("[HwCDMAPhoneReference]getMeid() = xxxxxx");
        return cdmaPhoneUtils.getMeid(this.mPhone);
    }

    public String getPesn() {
        return this.mPESN;
    }

    public void registerForLineControlInfo(Handler h, int what, Object obj) {
        logd("some apk registerForLineControlInfo");
        this.mPhone.mCT.registerForLineControlInfo(h, what, obj);
    }

    public void unregisterForLineControlInfo(Handler h) {
        logd("some apk unregisterForLineControlInfo");
        this.mPhone.mCT.unregisterForLineControlInfo(h);
    }

    public void startListen() {
        this.mTelephonyManager.listen(this.mPhoneStateListener, 1);
    }

    public void afterHandleMessage(Message msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleMessage what = ");
        stringBuilder.append(msg.what);
        logd(stringBuilder.toString());
        int i = msg.what;
        if (i == 1 || i == 5) {
            logd("Radio available or on, get lte release version");
            this.mPhone.mCi.getLteReleaseVersion(this.mPhone.obtainMessage(108));
        } else if (i != 21) {
            logd("unhandle event");
        } else {
            logd("handleMessage EVENT_GET_DEVICE_IDENTITY_DONE");
            if (cdmaPhoneUtils.getMeid(this.mPhone) != null && cdmaPhoneUtils.getMeid(this.mPhone).length() > 14) {
                cdmaPhoneUtils.setMeid(this.mPhone, cdmaPhoneUtils.getMeid(this.mPhone).substring(cdmaPhoneUtils.getMeid(this.mPhone).length() - 14));
            }
            AsyncResult ar = msg.obj;
            if (ar.exception == null) {
                String[] respId = ar.result;
                if (respId != null) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("handleMessage respId.length = ");
                    stringBuilder2.append(respId.length);
                    logd(stringBuilder2.toString());
                }
                if (respId != null && respId.length >= 4) {
                    logd("handleMessage mPESN = xxxxxx");
                    this.mPESN = respId[2];
                }
            }
        }
    }

    public void closeRrc() {
        try {
            this.mPhone.mCi.getClass().getMethod("closeRrc", new Class[0]).invoke(this.mPhone.mCi, new Object[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void switchVoiceCallBackgroundState(int state) {
        this.mPhone.mCT.switchVoiceCallBackgroundState(state);
    }

    public void riseCdmaCutoffFreq(boolean on) {
        this.mPhone.mCi.riseCdmaCutoffFreq(on, null);
    }

    public boolean beforeHandleMessage(Message msg) {
        boolean msgHandled;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("beforeHandleMessage what = ");
        stringBuilder.append(msg.what);
        logd(stringBuilder.toString());
        int i = msg.what;
        if (i == 108) {
            logd("onGetLteReleaseVersionDone:");
            AsyncResult ar = msg.obj;
            msgHandled = true;
            if (ar.exception == null) {
                int[] resultint = ar.result;
                if (resultint != null) {
                    if (resultint.length != 0) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("onGetLteReleaseVersionDone: result=");
                        stringBuilder2.append(resultint[0]);
                        logd(stringBuilder2.toString());
                        switch (resultint[0]) {
                            case 0:
                                this.mLteReleaseVersion = 0;
                                break;
                            case 1:
                                this.mLteReleaseVersion = 1;
                                break;
                            case 2:
                                this.mLteReleaseVersion = 2;
                                break;
                            case 3:
                                this.mLteReleaseVersion = 3;
                                break;
                            default:
                                this.mLteReleaseVersion = 0;
                                break;
                        }
                    }
                }
                logd("Error in get lte release version: null resultint");
            } else {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Error in get lte release version:");
                stringBuilder3.append(ar.exception);
                logd(stringBuilder3.toString());
            }
        } else if (i == 111) {
            logd("beforeHandleMessage handled->EVENT_SET_MODE_TO_AUTO ");
            msgHandled = true;
            this.mPhone.setNetworkSelectionModeAutomatic(null);
        } else if (i != 1000) {
            return super.beforeHandleMessage(msg);
        } else {
            logd("beforeHandleMessage handled->RETRY_GET_DEVICE_ID ");
            msgHandled = true;
            if (msg.arg2 == 2) {
                logd("start retry get DEVICE_ID_MASK_ALL");
                this.mPhone.mCi.getDeviceIdentity(this.mPhone.obtainMessage(21, msg.arg1, 0, null));
            } else {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("EVENT_RETRY_GET_DEVICE_ID msg.arg2:");
                stringBuilder4.append(msg.arg2);
                stringBuilder4.append(", error!!");
                logd(stringBuilder4.toString());
            }
        }
        return msgHandled;
    }

    public boolean isCTSimCard(int slotId) {
        return HwTelephonyManagerInner.getDefault().isCTSimCard(slotId);
    }

    public void setLTEReleaseVersion(int state, Message response) {
        this.mPhone.mCi.setLTEReleaseVersion(state, response);
    }

    public int getLteReleaseVersion() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getLteReleaseVersion: ");
        stringBuilder.append(this.mLteReleaseVersion);
        logd(stringBuilder.toString());
        return this.mLteReleaseVersion;
    }

    public boolean isChinaTelecom(int slotId) {
        return HwTelephonyManagerInner.getDefault().isChinaTelecom(slotId);
    }

    public void selectNetworkManually(OperatorInfo network, Message response) {
        loge("selectNetworkManually: not possible in CDMA");
        if (response != null) {
            AsyncResult.forMessage(response).exception = new CommandException(Error.REQUEST_NOT_SUPPORTED);
            response.sendToTarget();
        }
    }

    public void setNetworkSelectionModeAutomatic(Message response) {
        loge("method setNetworkSelectionModeAutomatic is NOT supported in CDMA!");
        if (response != null) {
            Rlog.e(LOG_TAG, "setNetworkSelectionModeAutomatic: not possible in CDMA- Posting exception");
            AsyncResult.forMessage(response).exception = new CommandException(Error.REQUEST_NOT_SUPPORTED);
            response.sendToTarget();
        }
    }

    public void registerForHWBuffer(Handler h, int what, Object obj) {
        this.mPhone.mCi.registerForHWBuffer(h, what, obj);
    }

    public void unregisterForHWBuffer(Handler h) {
        this.mPhone.mCi.unregisterForHWBuffer(h);
    }

    public void sendHWSolicited(Message reqMsg, int event, byte[] reqData) {
        this.mPhone.mCi.sendHWBufferSolicited(reqMsg, event, reqData);
    }

    public boolean cmdForECInfo(int event, int action, byte[] buf) {
        return this.mPhone.mCi.cmdForECInfo(event, action, buf);
    }

    public void notifyCellularCommParaReady(int paratype, int pathtype, Message response) {
        this.mPhone.mCi.notifyCellularCommParaReady(paratype, pathtype, response);
    }

    private void logd(String msg) {
        Rlog.d(this.subTag, msg);
    }

    private void loge(String msg) {
        Rlog.e(this.subTag, msg);
    }

    public void processEccNumber(ServiceStateTracker cSST) {
        if (SystemProperties.getBoolean("ro.config.hw_globalEcc", false) && SystemProperties.getBoolean("ro.config.hw_eccNumUseRplmn", false)) {
            logd("EVENT_RUIM_RECORDS_LOADED!!!!");
            SystemProperties.set(PROPERTY_GLOBAL_FORCE_TO_SET_ECC, "ruim_present");
            String hplmn = this.mPhone.getOperatorNumeric();
            boolean isRoaming = cSST.mSS.getRoaming();
            String rplmn = cSST.mSS.getOperatorNumeric();
            if (TextUtils.isEmpty(hplmn)) {
                logd("received EVENT_SIM_RECORDS_LOADED but not hplmn !!!!");
            } else if (isRoaming) {
                globalEccCustom(rplmn);
            } else {
                globalEccCustom(hplmn);
            }
        }
    }

    public void globalEccCustom(String operatorNumeric) {
        String str = operatorNumeric;
        String ecclist_withcard = null;
        String ecclist_withcard2 = null;
        String forceEccState = SystemProperties.get(PROPERTY_GLOBAL_FORCE_TO_SET_ECC, "invalid");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[SLOT");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append("]GECC-globalEccCustom: operator numeric = ");
        stringBuilder.append(str);
        stringBuilder.append("; preOperatorNumeric = ");
        stringBuilder.append(this.preOperatorNumeric);
        stringBuilder.append(";forceEccState  = ");
        stringBuilder.append(forceEccState);
        logd(stringBuilder.toString());
        boolean isValid = (TextUtils.isEmpty(operatorNumeric) || (str.equals(this.preOperatorNumeric) && "invalid".equals(forceEccState))) ? false : true;
        if (isValid) {
            StringBuilder stringBuilder2;
            this.preOperatorNumeric = str;
            SystemProperties.set(PROPERTY_GLOBAL_FORCE_TO_SET_ECC, "invalid");
            if (HwTelephonyFactory.getHwPhoneManager().isSupportEccFormVirtualNet()) {
                ecclist_withcard = HwTelephonyFactory.getHwPhoneManager().getVirtualNetEccWihCard(this.mPhoneId);
                ecclist_withcard2 = HwTelephonyFactory.getHwPhoneManager().getVirtualNetEccNoCard(this.mPhoneId);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("try to get Ecc form virtualNet ecclist_withcard=");
                stringBuilder3.append(ecclist_withcard);
                stringBuilder3.append(" ecclist_nocard=");
                stringBuilder3.append(ecclist_withcard2);
                logd(stringBuilder3.toString());
            }
            String ecclist_nocard = ecclist_withcard2;
            ecclist_withcard2 = ecclist_withcard;
            if (virtualNetEccFormCarrier(this.mPhoneId)) {
                int slotId = SubscriptionManager.getSlotIndex(this.mPhoneId);
                try {
                    ecclist_withcard2 = (String) HwCfgFilePolicy.getValue("virtual_ecclist_withcard", slotId, String.class);
                    ecclist_nocard = (String) HwCfgFilePolicy.getValue("virtual_ecclist_nocard", slotId, String.class);
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("try to get Ecc form virtualNet virtual_ecclist from carrier.xml =");
                    stringBuilder2.append(ecclist_withcard2);
                    stringBuilder2.append(" ecclist_nocard=");
                    stringBuilder2.append(ecclist_nocard);
                    logd(stringBuilder2.toString());
                } catch (Exception e) {
                    logd("Failed to get ecclist in carrier");
                }
            }
            String custEcc = getCustEccList(operatorNumeric);
            if (!TextUtils.isEmpty(custEcc)) {
                String[] custEccArray = custEcc.split(":");
                if (custEccArray.length == 3 && custEccArray[0].equals(str) && !TextUtils.isEmpty(custEccArray[1]) && !TextUtils.isEmpty(custEccArray[2])) {
                    ecclist_withcard2 = custEccArray[1];
                    ecclist_nocard = custEccArray[2];
                }
            }
            if (ecclist_withcard2 == null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("numeric=\"");
                stringBuilder2.append(str);
                stringBuilder2.append("\"");
                Cursor cursor = this.mPhone.getContext().getContentResolver().query(GlobalMatchs.CONTENT_URI, new String[]{"_id", NumMatchs.NAME, "numeric", VirtualNets.ECC_WITH_CARD, VirtualNets.ECC_NO_CARD}, stringBuilder2.toString(), null, NumMatchs.DEFAULT_SORT_ORDER);
                if (cursor == null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("[SLOT");
                    stringBuilder2.append(this.mPhoneId);
                    stringBuilder2.append("]GECC-globalEccCustom: No matched emergency numbers in db.");
                    logd(stringBuilder2.toString());
                    this.mPhone.mCi.requestSetEmergencyNumbers("", "");
                    return;
                }
                try {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        ecclist_withcard2 = cursor.getString(3);
                        ecclist_nocard = cursor.getString(4);
                        cursor.moveToNext();
                    }
                } catch (RuntimeException ex) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("[SLOT");
                    stringBuilder4.append(this.mPhoneId);
                    stringBuilder4.append("]globalEccCustom: global version cause exception!");
                    stringBuilder4.append(ex.toString());
                    logd(stringBuilder4.toString());
                } catch (Throwable th) {
                    cursor.close();
                }
                cursor.close();
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[SLOT");
            stringBuilder2.append(this.mPhoneId);
            stringBuilder2.append("]GECC-globalEccCustom: ecc_withcard = ");
            stringBuilder2.append(ecclist_withcard2);
            stringBuilder2.append(", ecc_nocard = ");
            stringBuilder2.append(ecclist_nocard);
            logd(stringBuilder2.toString());
            boolean z = (TextUtils.isEmpty(ecclist_withcard2) && TextUtils.isEmpty(ecclist_nocard)) ? false : true;
            boolean validEcclist = z;
            ecclist_withcard2 = ecclist_withcard2 != null ? ecclist_withcard2 : "";
            String ecclist_nocard2 = ecclist_nocard != null ? ecclist_nocard : "";
            if (validEcclist) {
                this.mPhone.mCi.requestSetEmergencyNumbers(ecclist_withcard2, ecclist_nocard2);
            } else {
                this.mPhone.mCi.requestSetEmergencyNumbers("", "");
            }
        }
    }

    private String getCustEccList(String operatorNumeric) {
        String custEccList = null;
        String matchEccList = "";
        try {
            custEccList = System.getString(this.mPhone.getContext().getContentResolver(), "hw_cust_emergency_nums");
        } catch (RuntimeException e) {
            Rlog.e(LOG_TAG, "Failed to load vmNum from SettingsEx", e);
        }
        if (TextUtils.isEmpty(custEccList) || TextUtils.isEmpty(operatorNumeric)) {
            return matchEccList;
        }
        String[] custEccListItems = custEccList.split(";");
        for (int i = 0; i < custEccListItems.length; i++) {
            String[] custItem = custEccListItems[i].split(":");
            if (custItem.length == 3 && custItem[0].equals(operatorNumeric)) {
                matchEccList = custEccListItems[i];
                break;
            }
        }
        return matchEccList;
    }

    public void updateWfcMode(Context context, boolean roaming, int subId) throws ImsException {
        HwImsManagerInner.updateWfcMode(context, roaming, subId);
    }

    public boolean isDualImsAvailable() {
        return HwImsManagerInner.isDualImsAvailable();
    }
}
