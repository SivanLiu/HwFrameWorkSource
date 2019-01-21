package com.android.internal.telephony.uicc;

import android.app.ActivityManagerNative;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.HwTelephony.VirtualNets;
import android.provider.Settings.System;
import android.provider.SettingsEx.Systemex;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.telephony.HwAddonTelephonyFactory;
import com.android.internal.telephony.HwCarrierConfigCardManager;
import com.android.internal.telephony.HwHotplugController;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.HwSubscriptionManager;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.HwVolteChrManagerImpl;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.dataconnection.ApnReminder;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConfig;
import com.android.internal.telephony.gsm.HwEons;
import com.android.internal.telephony.gsm.HwEons.CphsType;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.UsimServiceTable.UsimService;
import com.android.internal.telephony.vsim.HwVSimConstants;
import huawei.cust.HwCustUtils;
import java.util.ArrayList;
import java.util.Arrays;

public class HwSIMRecords extends SIMRecords {
    public static final String ANY_SIM_DETECTED = "any_sim_detect";
    private static final int EVENT_GET_ACTING_HPLMN_DONE = 201;
    private static final int EVENT_GET_ALL_OPL_RECORDS_DONE = 101;
    private static final int EVENT_GET_ALL_PNN_RECORDS_DONE = 102;
    private static final int EVENT_GET_CARRIER_FILE_DONE = 4;
    private static final int EVENT_GET_GID1_HW_DONE = 1;
    private static final int EVENT_GET_GID1_HW_DONE_EX = 3;
    private static final int EVENT_GET_PBR_DONE = 233;
    private static final int EVENT_GET_SPECIAL_FILE_DONE = 2;
    private static final int EVENT_GET_SPN = 103;
    private static final int EVENT_GET_SPN_CPHS_DONE = 104;
    private static final int EVENT_GET_SPN_SHORT_CPHS_DONE = 105;
    private static final int EVENT_HW_CUST_BASE = 100;
    private static final boolean IS_DELAY_UPDATENAME = SystemProperties.getBoolean("ro.config.delay_updatename", false);
    private static final boolean IS_MODEM_CAPABILITY_GET_ICCID_AT = HwModemCapability.isCapabilitySupport(19);
    public static final String MULTI_PDP_PLMN_MATCHED = "multi_pdp_plmn_matched";
    private static final int SST_PNN_ENABLED = 48;
    private static final int SST_PNN_MASK = 48;
    private static final int SST_PNN_OFFSET = 12;
    private static final String TAG = "HwSIMRecords";
    private static final boolean isMultiSimEnabled = TelephonyManager.getDefault().isMultiSimEnabled();
    private static final String pRefreshMultifileProp = "gsm.sim.refresh.multifile";
    private static final String pRefreshMultifilePropExtra = "gsm.sim.refresh.multifile.extra";
    private static String[] strEFIDs = new String[30];
    private static UiccCardApplicationUtils uiccCardApplicationUtils = new UiccCardApplicationUtils();
    protected boolean bNeedSendRefreshBC = false;
    private GlobalChecker globalChecker = new GlobalChecker(this);
    private Handler handlerEx = new Handler() {
        /* JADX WARNING: Missing block: B:35:0x022d, code skipped:
            if (r0 != false) goto L_0x022f;
     */
        /* JADX WARNING: Missing block: B:36:0x022f, code skipped:
            r13.this$0.onRecordLoaded();
     */
        /* JADX WARNING: Missing block: B:41:0x023f, code skipped:
            if (null == null) goto L_0x0242;
     */
        /* JADX WARNING: Missing block: B:42:0x0242, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message msg) {
            boolean isRecordLoadResponse = false;
            HwSIMRecords hwSIMRecords;
            StringBuilder stringBuilder;
            if (HwSIMRecords.this.mDestroyed.get()) {
                hwSIMRecords = HwSIMRecords.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Received message ");
                stringBuilder.append(msg);
                stringBuilder.append("[");
                stringBuilder.append(msg.what);
                stringBuilder.append("]  while being destroyed. Ignoring.");
                hwSIMRecords.loge(stringBuilder.toString());
                return;
            }
            try {
                AsyncResult ar;
                HwSIMRecords hwSIMRecords2;
                StringBuilder stringBuilder2;
                Bundle bundle;
                String filePath;
                switch (msg.what) {
                    case 1:
                        isRecordLoadResponse = true;
                        ar = (AsyncResult) msg.obj;
                        if (ar.exception == null) {
                            HwSIMRecords.this.mEfGid1 = (byte[]) ar.result;
                            hwSIMRecords2 = HwSIMRecords.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("mEfGid1: ");
                            stringBuilder2.append(IccUtils.bytesToHexString(HwSIMRecords.this.mEfGid1));
                            hwSIMRecords2.log(stringBuilder2.toString());
                            break;
                        }
                        hwSIMRecords2 = HwSIMRecords.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Get GID1 failed, the exception: ");
                        stringBuilder2.append(ar.exception);
                        hwSIMRecords2.log(stringBuilder2.toString());
                        HwSIMRecords.this.globalChecker.loadGID1Ex();
                        break;
                    case 2:
                        isRecordLoadResponse = true;
                        ar = (AsyncResult) msg.obj;
                        if (ar.exception == null) {
                            bundle = msg.getData();
                            filePath = bundle.getString(VirtualNets.MATCH_PATH);
                            String fileId = bundle.getString(VirtualNets.MATCH_FILE);
                            byte[] bytes = ar.result;
                            if (HwSIMRecords.isMultiSimEnabled) {
                                HwTelephonyFactory.getHwPhoneManager().addVirtualNetSpecialFile(filePath, fileId, bytes, HwSIMRecords.this.getSlotId());
                            } else {
                                HwTelephonyFactory.getHwPhoneManager().addVirtualNetSpecialFile(filePath, fileId, bytes);
                            }
                            if (HwSIMRecords.this.mHwCustHwSIMRecords != null) {
                                HwSIMRecords.this.mHwCustHwSIMRecords.addHwVirtualNetSpecialFiles(filePath, fileId, bytes, HwSIMRecords.this.getSlotId());
                            }
                            HwSIMRecords hwSIMRecords3 = HwSIMRecords.this;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("load Specifile: ");
                            stringBuilder3.append(filePath);
                            stringBuilder3.append(" ");
                            stringBuilder3.append(fileId);
                            stringBuilder3.append(" = ");
                            stringBuilder3.append(IccUtils.bytesToHexString(HwSIMRecords.this.mEfGid1));
                            hwSIMRecords3.log(stringBuilder3.toString());
                            break;
                        }
                        break;
                    case 3:
                        isRecordLoadResponse = true;
                        ar = (AsyncResult) msg.obj;
                        if (ar.exception == null) {
                            HwSIMRecords.this.mEfGid1 = (byte[]) ar.result;
                            hwSIMRecords2 = HwSIMRecords.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("mEfGid1_ex: ");
                            stringBuilder2.append(IccUtils.bytesToHexString(HwSIMRecords.this.mEfGid1));
                            hwSIMRecords2.log(stringBuilder2.toString());
                            break;
                        }
                        hwSIMRecords2 = HwSIMRecords.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Get GID1_EX failed, the exception: ");
                        stringBuilder2.append(ar.exception);
                        hwSIMRecords2.log(stringBuilder2.toString());
                        break;
                    case 4:
                        ar = msg.obj;
                        if (ar.exception == null) {
                            bundle = msg.getData();
                            filePath = bundle.getString(VirtualNets.MATCH_PATH);
                            String carrierFileId = bundle.getString(VirtualNets.MATCH_FILE);
                            String carrierFileValue = IccUtils.bytesToHexString((byte[]) ar.result);
                            HwSIMRecords.this.mHwCarrierCardManager.addSpecialFileResult(true, filePath, carrierFileId, carrierFileValue, HwSIMRecords.this.getSlotId());
                            HwSIMRecords hwSIMRecords4 = HwSIMRecords.this;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Carrier load Specialfile: ");
                            stringBuilder4.append(filePath);
                            stringBuilder4.append(" ");
                            stringBuilder4.append(carrierFileId);
                            stringBuilder4.append(" = ");
                            stringBuilder4.append(carrierFileValue);
                            hwSIMRecords4.log(stringBuilder4.toString());
                            break;
                        }
                        HwSIMRecords hwSIMRecords5;
                        StringBuilder stringBuilder5;
                        String carrierFilePath = null;
                        filePath = null;
                        Bundle carrierBundle = msg.getData();
                        if (carrierBundle != null) {
                            carrierFilePath = carrierBundle.getString(VirtualNets.MATCH_PATH);
                            filePath = carrierBundle.getString(VirtualNets.MATCH_FILE);
                            hwSIMRecords5 = HwSIMRecords.this;
                            stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("load Specialfile: ");
                            stringBuilder5.append(carrierFilePath);
                            stringBuilder5.append(" ");
                            stringBuilder5.append(filePath);
                            stringBuilder5.append(" fail!");
                            hwSIMRecords5.log(stringBuilder5.toString());
                        }
                        HwSIMRecords.this.mHwCarrierCardManager.addSpecialFileResult(false, carrierFilePath, filePath, null, HwSIMRecords.this.getSlotId());
                        hwSIMRecords5 = HwSIMRecords.this;
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("exception=");
                        stringBuilder5.append(ar.exception);
                        hwSIMRecords5.log(stringBuilder5.toString());
                        break;
                    default:
                        hwSIMRecords = HwSIMRecords.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("unknown Event: ");
                        stringBuilder.append(msg.what);
                        hwSIMRecords.log(stringBuilder.toString());
                        break;
                }
            } catch (RuntimeException exc) {
                HwSIMRecords.this.logw("Exception parsing SIM record", exc);
            } catch (Throwable th) {
                if (null != null) {
                    HwSIMRecords.this.onRecordLoaded();
                }
            }
        }
    };
    private boolean isEnsEnabled = SystemProperties.getBoolean("ro.config.hw_is_ens_enabled", false);
    private String mActingHplmn = "";
    byte[] mEfGid1 = null;
    HwEons mEons = new HwEons();
    HwCarrierConfigCardManager mHwCarrierCardManager;
    private HwCustHwSIMRecords mHwCustHwSIMRecords;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if ("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE".equals(intent.getAction())) {
                    HwSIMRecords hwSIMRecords = HwSIMRecords.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Receives ACTION_SET_RADIO_CAPABILITY_DONE on slot ");
                    stringBuilder.append(HwSIMRecords.this.getSlotId());
                    hwSIMRecords.log(stringBuilder.toString());
                    boolean bNeedFetchRecords = HwFullNetworkConfig.IS_FAST_SWITCH_SIMSLOT && HwSIMRecords.this.mIsSimPowerDown && HwSIMRecords.this.mParentApp != null && AppState.APPSTATE_READY == HwSIMRecords.this.mParentApp.getState();
                    if (bNeedFetchRecords) {
                        HwSIMRecords.this.log("fetchSimRecords again.");
                        HwSIMRecords.this.mIsSimPowerDown = false;
                        HwSIMRecords.this.fetchSimRecords();
                    }
                }
            }
        }
    };
    private boolean mIsSimPowerDown = false;
    private ArrayList<byte[]> mPnnRecords = null;
    private boolean mSstPnnVaild = true;

    private class GlobalChecker {
        private SIMRecords mSimRecords;

        public GlobalChecker(SIMRecords simRecords) {
            this.mSimRecords = simRecords;
        }

        public void onOperatorNumericLoaded() {
            loadVirtualNetSpecialFiles();
            checkMultiPdpConfig();
            if (HwSIMRecords.isMultiSimEnabled) {
                checkDataServiceRemindMsim();
            } else {
                checkDataServiceRemind();
            }
            checkGsmOnlyDataNotAllowed();
            if (HwSIMRecords.this.mHwCustHwSIMRecords != null) {
                HwSIMRecords.this.mHwCustHwSIMRecords.setVmPriorityModeInClaro(HwSIMRecords.this.mVmConfig);
            }
        }

        private void checkDataServiceRemindMsim() {
            int lSimSlotVal = HwSIMRecords.this.getSlotId();
            int lDataVal = HwTelephonyManagerInner.getDefault().getPreferredDataSubscription();
            boolean hasTwoCard = true;
            if (lSimSlotVal == 1 && lDataVal == 0) {
                hasTwoCard = TelephonyManager.getDefault().hasIccCard(lDataVal);
            }
            if (lSimSlotVal == 0) {
                SystemProperties.set("gsm.huawei.RemindDataService", "false");
            } else if (1 == lSimSlotVal) {
                SystemProperties.set("gsm.huawei.RemindDataService_1", "false");
            }
            String plmnsConfig = Systemex.getString(HwSIMRecords.this.mContext.getContentResolver(), "plmn_remind_data_service");
            if (plmnsConfig == null) {
                plmnsConfig = "26006,26003";
            }
            for (String plmn : plmnsConfig.split(",")) {
                if (plmn != null && plmn.equals(HwSIMRecords.this.getOperatorNumeric())) {
                    if (!"true".equals(System.getString(HwSIMRecords.this.mContext.getContentResolver(), HwSIMRecords.ANY_SIM_DETECTED)) && (lDataVal == lSimSlotVal || !hasTwoCard)) {
                        ((TelephonyManager) HwSIMRecords.this.mContext.getSystemService("phone")).setDataEnabled(false);
                        System.putString(HwSIMRecords.this.mContext.getContentResolver(), HwSIMRecords.ANY_SIM_DETECTED, "true");
                    }
                    if (lSimSlotVal == 0) {
                        SystemProperties.set("gsm.huawei.RemindDataService", "true");
                    } else if (1 == lSimSlotVal) {
                        SystemProperties.set("gsm.huawei.RemindDataService_1", "true");
                    }
                }
            }
            System.putString(HwSIMRecords.this.mContext.getContentResolver(), HwSIMRecords.ANY_SIM_DETECTED, "true");
        }

        public void onAllRecordsLoaded() {
            updateCarrierFileIfNeed();
            HwSIMRecords.this.mVmConfig.clearVoicemailLoadedFlag();
            loadVirtualNet();
            if (HwSIMRecords.isMultiSimEnabled) {
                ApnReminder apnReminder = ApnReminder.getInstance(HwSIMRecords.this.mContext, HwSIMRecords.this.getSlotId());
                apnReminder.setGID1(HwSIMRecords.this.mEfGid1);
                apnReminder.setPlmnAndImsi(HwSIMRecords.this.getOperatorNumeric(), HwSIMRecords.this.mImsi);
            } else {
                ApnReminder.getInstance(HwSIMRecords.this.mContext).setGID1(HwSIMRecords.this.mEfGid1);
                ApnReminder.getInstance(HwSIMRecords.this.mContext).setPlmnAndImsi(HwSIMRecords.this.getOperatorNumeric(), HwSIMRecords.this.mImsi);
            }
            sendSimRecordsReadyBroadcast();
            if (HwSIMRecords.this.mHwCustHwSIMRecords != null) {
                HwSIMRecords.this.mHwCustHwSIMRecords.refreshDataRoamingSettings();
            }
            if (HwSIMRecords.this.mHwCustHwSIMRecords != null) {
                HwSIMRecords.this.mHwCustHwSIMRecords.refreshMobileDataAlwaysOnSettings();
            }
            if (HwSIMRecords.IS_DELAY_UPDATENAME && HwSIMRecords.this.mPnnRecords != null && HwSIMRecords.this.isNeedSetPnn()) {
                try {
                    HwSIMRecords.this.mEons.setPnnData(HwSIMRecords.this.mPnnRecords);
                    HwSIMRecords.this.mRecordsEventsRegistrants.notifyResult(Integer.valueOf(100));
                } catch (RuntimeException exc) {
                    HwSIMRecords.this.logw("Exception set PNN record", exc);
                }
            }
            updateClatForMobile();
        }

        private void updateClatForMobile() {
            SubscriptionController subController = SubscriptionController.getInstance();
            if (subController != null && HwSIMRecords.this.getSlotId() == subController.getDefaultDataSubId()) {
                String mccMnc = HwSIMRecords.this.getOperatorNumeric();
                try {
                    String plmnsConfig = System.getString(HwSIMRecords.this.mContext.getContentResolver(), "disable_mobile_clatd");
                    if (TextUtils.isEmpty(plmnsConfig) || TextUtils.isEmpty(mccMnc)) {
                        Rlog.d("SIMRecords", "plmnsConfig is null, return");
                    } else if (plmnsConfig.contains(mccMnc)) {
                        Rlog.d("SIMRecords", "disable clatd!");
                        SystemProperties.set("gsm.net.doxlat", "false");
                    } else {
                        SystemProperties.set("gsm.net.doxlat", "true");
                    }
                } catch (Exception e) {
                    HwSIMRecords hwSIMRecords = HwSIMRecords.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception e = ");
                    stringBuilder.append(e);
                    hwSIMRecords.loge(stringBuilder.toString());
                }
            }
        }

        private void sendSimRecordsReadyBroadcast() {
            String operatorNumeric = HwSIMRecords.this.getOperatorNumeric();
            String imsi = HwSIMRecords.this.getIMSI();
            Rlog.d("SIMRecords", "broadcast TelephonyIntents.ACTION_SIM_RECORDS_READY");
            Intent intent = new Intent("com.huawei.intent.action.ACTION_SIM_RECORDS_READY");
            intent.addFlags(536870912);
            intent.putExtra("mccMnc", operatorNumeric);
            intent.putExtra(HwVSimConstants.ENABLE_PARA_IMSI, imsi);
            if (!(!TelephonyManager.getDefault().isMultiSimEnabled() || HwSIMRecords.this.mParentApp == null || HwSIMRecords.this.mParentApp.getUiccCard() == null)) {
                int[] subId = SubscriptionManager.getSubId(HwSIMRecords.this.mParentApp.getUiccCard().getPhoneId());
                if (subId != null && subId.length > 0) {
                    SubscriptionManager.putPhoneIdAndSubIdExtra(intent, SubscriptionManager.getPhoneId(subId[0]));
                }
            }
            ActivityManagerNative.broadcastStickyIntent(intent, null, 0);
        }

        private void checkMultiPdpConfig() {
            String plmnsConfig = Systemex.getString(HwSIMRecords.this.mContext.getContentResolver(), HwSIMRecords.MULTI_PDP_PLMN_MATCHED);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkMultiPdpConfig plmnsConfig = ");
            stringBuilder.append(plmnsConfig);
            Rlog.d("SIMRecords", stringBuilder.toString());
            if (plmnsConfig != null) {
                String[] plmns = plmnsConfig.split(",");
                int length = plmns.length;
                int i = 0;
                while (i < length) {
                    String plmn = plmns[i];
                    if (plmn == null || !plmn.equals(HwSIMRecords.this.getOperatorNumeric())) {
                        i++;
                    } else {
                        SystemProperties.set("gsm.multipdp.plmn.matched", "true");
                        return;
                    }
                }
            }
            SystemProperties.set("gsm.multipdp.plmn.matched", "false");
        }

        private void checkDataServiceRemind() {
            SystemProperties.set("gsm.huawei.RemindDataService", "false");
            String plmnsConfig = Systemex.getString(HwSIMRecords.this.mContext.getContentResolver(), "plmn_remind_data_service");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkDataServiceRemind plmnsConfig = ");
            stringBuilder.append(plmnsConfig);
            Rlog.d("SIMRecords", stringBuilder.toString());
            if (plmnsConfig == null) {
                plmnsConfig = "26006,26003";
            }
            String[] plmns = plmnsConfig.split(",");
            int length = plmns.length;
            int i = 0;
            while (i < length) {
                String plmn = plmns[i];
                if (plmn == null || !plmn.equals(HwSIMRecords.this.getOperatorNumeric())) {
                    i++;
                } else {
                    if (!"true".equals(System.getString(HwSIMRecords.this.mContext.getContentResolver(), HwSIMRecords.ANY_SIM_DETECTED))) {
                        ((TelephonyManager) HwSIMRecords.this.mContext.getSystemService("phone")).setDataEnabled(false);
                    }
                    System.putString(HwSIMRecords.this.mContext.getContentResolver(), HwSIMRecords.ANY_SIM_DETECTED, "true");
                    SystemProperties.set("gsm.huawei.RemindDataService", "true");
                    return;
                }
            }
            System.putString(HwSIMRecords.this.mContext.getContentResolver(), HwSIMRecords.ANY_SIM_DETECTED, "true");
        }

        private void checkGsmOnlyDataNotAllowed() {
            if (HwSIMRecords.isMultiSimEnabled) {
                int[] subIds = SubscriptionManager.getSubId(HwSIMRecords.this.getSlotId());
                if (subIds != null) {
                    TelephonyManager.setTelephonyProperty(subIds[0], "gsm.data.gsm_only_not_allow_ps", "false");
                } else {
                    return;
                }
            }
            SystemProperties.set("gsm.data.gsm_only_not_allow_ps", "false");
            String plmnGsmonlyPsNotallowd = System.getString(HwSIMRecords.this.mContext.getContentResolver(), "hw_2gonly_psnotallowed");
            if (plmnGsmonlyPsNotallowd == null || "".equals(plmnGsmonlyPsNotallowd)) {
                plmnGsmonlyPsNotallowd = "23410";
            }
            String hplmn = HwSIMRecords.this.getOperatorNumeric();
            if (hplmn == null || "".equals(hplmn)) {
                HwSIMRecords.this.log("is2GonlyPsAllowed home plmn not ready");
                return;
            }
            String[] plmnCustomArray = plmnGsmonlyPsNotallowd.split(",");
            int regplmnCustomArrayLen = plmnCustomArray.length;
            for (int i = 0; i < regplmnCustomArrayLen; i++) {
                HwSIMRecords hwSIMRecords = HwSIMRecords.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("is2GonlyPsAllowed plmnCustomArray[");
                stringBuilder.append(i);
                stringBuilder.append("] = ");
                stringBuilder.append(plmnCustomArray[i]);
                hwSIMRecords.log(stringBuilder.toString());
                if (hplmn.equals(plmnCustomArray[i])) {
                    if (HwSIMRecords.isMultiSimEnabled) {
                        TelephonyManager.setTelephonyProperty(SubscriptionManager.getSubId(HwSIMRecords.this.getSlotId())[0], "gsm.data.gsm_only_not_allow_ps", "true");
                    } else {
                        SystemProperties.set("gsm.data.gsm_only_not_allow_ps", "true");
                    }
                    return;
                }
            }
        }

        public void loadGID1() {
            HwSIMRecords.this.mFh.loadEFTransparent(28478, HwSIMRecords.this.handlerEx.obtainMessage(1));
            HwSIMRecords hwSIMRecords = HwSIMRecords.this;
            hwSIMRecords.mRecordsToLoad++;
        }

        public void loadGID1Ex() {
            if ((HwSIMRecords.this.mFh instanceof UsimFileHandler) && !"3F007FFF".equals(HwSIMRecords.this.mFh.getEFPath(28478))) {
                HwSIMRecords.this.mFh.loadEFTransparent("3F007FFF", 28478, HwSIMRecords.this.handlerEx.obtainMessage(3), true);
                HwSIMRecords hwSIMRecords = HwSIMRecords.this;
                hwSIMRecords.mRecordsToLoad++;
            }
        }

        public void loadVirtualNetSpecialFiles() {
            String homeNumeric = getHomeNumericAndSetRoaming();
            HwSIMRecords hwSIMRecords = HwSIMRecords.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("GlobalChecker onOperatorNumericLoaded(): homeNumeric = ");
            stringBuilder.append(homeNumeric);
            hwSIMRecords.log(stringBuilder.toString());
            if (homeNumeric != null) {
                HwTelephonyFactory.getHwPhoneManager().loadVirtualNetSpecialFiles(homeNumeric, this.mSimRecords);
            } else {
                HwTelephonyFactory.getHwPhoneManager().loadVirtualNetSpecialFiles(HwSIMRecords.this.getOperatorNumeric(), this.mSimRecords);
            }
        }

        public void loadVirtualNet() {
            String homeNumeric = getHomeNumeric();
            HwSIMRecords hwSIMRecords = HwSIMRecords.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("GlobalChecker onAllRecordsLoaded(): homeNumeric = ");
            stringBuilder.append(homeNumeric);
            hwSIMRecords.log(stringBuilder.toString());
            if (homeNumeric != null) {
                HwTelephonyFactory.getHwPhoneManager().loadVirtualNet(homeNumeric, this.mSimRecords);
            } else {
                HwTelephonyFactory.getHwPhoneManager().loadVirtualNet(HwSIMRecords.this.getOperatorNumeric(), this.mSimRecords);
            }
        }

        public String getHomeNumericAndSetRoaming() {
            if (HwSIMRecords.isMultiSimEnabled) {
                HwTelephonyFactory.getHwPhoneManager().setRoamingBrokerOperator(HwSIMRecords.this.getOperatorNumeric(), HwSIMRecords.this.getSlotId());
                HwTelephonyFactory.getHwPhoneManager().setRoamingBrokerImsi(HwSIMRecords.this.mImsi, Integer.valueOf(HwSIMRecords.this.getSlotId()));
                if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(HwSIMRecords.this.getSlotId()))) {
                    return HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric(Integer.valueOf(HwSIMRecords.this.getSlotId()));
                }
                return null;
            }
            HwTelephonyFactory.getHwPhoneManager().setRoamingBrokerOperator(HwSIMRecords.this.getOperatorNumeric());
            HwTelephonyFactory.getHwPhoneManager().setRoamingBrokerImsi(HwSIMRecords.this.mImsi);
            if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated()) {
                return HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric();
            }
            return null;
        }

        public String getHomeNumeric() {
            if (HwSIMRecords.isMultiSimEnabled) {
                if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(HwSIMRecords.this.getSlotId()))) {
                    return HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric(Integer.valueOf(HwSIMRecords.this.getSlotId()));
                }
                return null;
            } else if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated()) {
                return HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric();
            } else {
                return null;
            }
        }

        public void onIccIdLoadedHw() {
            if (HwSIMRecords.isMultiSimEnabled) {
                HwTelephonyFactory.getHwPhoneManager().setRoamingBrokerIccId(HwSIMRecords.this.mIccId, HwSIMRecords.this.getSlotId());
            } else {
                HwTelephonyFactory.getHwPhoneManager().setRoamingBrokerIccId(HwSIMRecords.this.mIccId);
            }
            HwTelephonyFactory.getHwPhoneManager().setMccTableIccId(HwSIMRecords.this.mIccId);
        }

        public void onImsiLoaded() {
            if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onImsiLoaded mPhoneId = ");
                stringBuilder.append(HwSIMRecords.this.getSlotId());
                Rlog.d("SIMRecords", stringBuilder.toString());
                if (HwSIMRecords.this.getSlotId() == 1 && TelephonyManager.getDefault().getSimState(0) == 5) {
                    return;
                }
            }
            HwTelephonyFactory.getHwPhoneManager().setMccTableImsi(HwSIMRecords.this.mImsi);
        }

        private void updateCarrierFileIfNeed() {
            if (HwSIMRecords.this.mHwCarrierCardManager != null) {
                HwSIMRecords.this.mHwCarrierCardManager.updateCarrierFileIfNeed(HwSIMRecords.this.getSlotId(), this.mSimRecords);
            }
        }
    }

    protected class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            HwSIMRecords.this.mFdnRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
        }
    }

    public HwSIMRecords(UiccCardApplication app, Context c, CommandsInterface ci) {
        super(app, c, ci);
        initEventIdMap();
        this.mHwCustHwSIMRecords = (HwCustHwSIMRecords) HwCustUtils.createObj(HwCustHwSIMRecords.class, new Object[]{this, c});
        this.mHwCarrierCardManager = HwCarrierConfigCardManager.getDefault(c);
        this.mHwCarrierCardManager.reportIccRecordInstance(getSlotId(), this);
        if (getIccidSwitch()) {
            if (IS_MODEM_CAPABILITY_GET_ICCID_AT) {
                this.mCi.getICCID(obtainMessage(getEventIdFromMap("EVENT_GET_ICCID_DONE")));
            } else {
                this.mFh.loadEFTransparent(12258, obtainMessage(getEventIdFromMap("EVENT_GET_ICCID_DONE")));
            }
            this.mRecordsToLoad++;
        }
        addIntentFilter(c);
    }

    private void addIntentFilter(Context c) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        c.registerReceiver(this.mIntentReceiver, filter);
    }

    /* JADX WARNING: Missing block: B:64:0x0260, code skipped:
            if (r0 != false) goto L_0x0262;
     */
    /* JADX WARNING: Missing block: B:65:0x0262, code skipped:
            onRecordLoaded();
     */
    /* JADX WARNING: Missing block: B:70:0x026e, code skipped:
            if (null == null) goto L_0x0271;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleMessage(Message msg) {
        boolean isRecordLoadResponse = false;
        if (this.mDestroyed.get()) {
            loge("Received message while being destroyed. Ignoring.");
            return;
        }
        if (getEventIdFromMap("EVENT_GET_MBDN_DONE") == msg.what) {
            this.mVmConfig.setVoicemailOnSIM(null, null);
            super.handleMessage(msg);
            this.mVmConfig.setVoicemailOnSIM(this.mVoiceMailNum, this.mVoiceMailTag);
        } else {
            try {
                int i = msg.what;
                if (i != 42) {
                    int i2 = 0;
                    AsyncResult ar;
                    StringBuilder stringBuilder;
                    StringBuilder stringBuilder2;
                    if (i == 201) {
                        isRecordLoadResponse = true;
                        ar = (AsyncResult) msg.obj;
                        log("EVENT_GET_ACTING_HPLMN_DONE");
                        if (ar.exception != null) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Exception in get acting hplmn ");
                            stringBuilder.append(ar.exception);
                            loge(stringBuilder.toString());
                        } else {
                            int[] mHplmnData = getSimPlmnDigits((byte[]) ar.result);
                            if (15 == mHplmnData[0]) {
                                this.mActingHplmn = "";
                            } else {
                                int length;
                                StringBuffer buffer = new StringBuffer();
                                if (15 == mHplmnData[5]) {
                                    length = 5;
                                } else {
                                    length = 6;
                                }
                                while (i2 < length) {
                                    buffer.append(mHplmnData[i2]);
                                    i2++;
                                }
                                this.mActingHplmn = buffer.toString();
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("length of mHplmnData =");
                                stringBuilder2.append(length);
                                stringBuilder2.append(", mActingHplmn = ");
                                stringBuilder2.append(this.mActingHplmn);
                                log(stringBuilder2.toString());
                            }
                        }
                    } else if (i != EVENT_GET_PBR_DONE) {
                        switch (i) {
                            case EVENT_GET_ALL_OPL_RECORDS_DONE /*101*/:
                                isRecordLoadResponse = true;
                                ar = (AsyncResult) msg.obj;
                                if (ar.exception == null) {
                                    this.mEons.setOplData((ArrayList) ar.result);
                                    this.mRecordsEventsRegistrants.notifyResult(Integer.valueOf(100));
                                    break;
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("[EONS] Exception in fetching OPL Records: ");
                                stringBuilder2.append(ar.exception);
                                Rlog.e("SIMRecords", stringBuilder2.toString());
                                this.mEons.resetOplData();
                                break;
                            case EVENT_GET_ALL_PNN_RECORDS_DONE /*102*/:
                                isRecordLoadResponse = true;
                                ar = (AsyncResult) msg.obj;
                                if (ar.exception == null) {
                                    if (!IS_DELAY_UPDATENAME) {
                                        this.mEons.setPnnData((ArrayList) ar.result);
                                        this.mRecordsEventsRegistrants.notifyResult(Integer.valueOf(100));
                                        break;
                                    }
                                    this.mPnnRecords = new ArrayList();
                                    this.mPnnRecords = (ArrayList) ar.result;
                                    break;
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("[EONS] Exception in fetching PNN Records: ");
                                stringBuilder2.append(ar.exception);
                                Rlog.e("SIMRecords", stringBuilder2.toString());
                                this.mEons.resetPnnData();
                                break;
                            case EVENT_GET_SPN /*103*/:
                                isRecordLoadResponse = true;
                                ar = (AsyncResult) msg.obj;
                                if (ar.exception == null) {
                                    byte[] data = (byte[]) ar.result;
                                    this.mSpnDisplayCondition = HwSubscriptionManager.SUB_INIT_STATE & data[0];
                                    String spn = IccUtils.adnStringFieldToString(data, 1, data.length - 1);
                                    setServiceProviderName(spn);
                                    setSystemProperty("gsm.sim.operator.alpha", spn);
                                    this.mRecordsEventsRegistrants.notifyResult(Integer.valueOf(2));
                                    break;
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("[EONS] Exception in reading EF_SPN: ");
                                stringBuilder2.append(ar.exception);
                                Rlog.e("SIMRecords", stringBuilder2.toString());
                                this.mSpnDisplayCondition = -1;
                                break;
                            case EVENT_GET_SPN_CPHS_DONE /*104*/:
                                isRecordLoadResponse = true;
                                ar = (AsyncResult) msg.obj;
                                if (ar.exception == null) {
                                    this.mEons.setCphsData(CphsType.LONG, (byte[]) ar.result);
                                    break;
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("[EONS] Exception in reading EF_SPN_CPHS: ");
                                stringBuilder2.append(ar.exception);
                                Rlog.e("SIMRecords", stringBuilder2.toString());
                                this.mEons.resetCphsData(CphsType.LONG);
                                break;
                            case EVENT_GET_SPN_SHORT_CPHS_DONE /*105*/:
                                isRecordLoadResponse = true;
                                ar = msg.obj;
                                if (ar.exception == null) {
                                    this.mEons.setCphsData(CphsType.SHORT, (byte[]) ar.result);
                                    break;
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("[EONS] Exception in reading EF_SPN_SHORT_CPHS: ");
                                stringBuilder2.append(ar.exception);
                                Rlog.e("SIMRecords", stringBuilder2.toString());
                                this.mEons.resetCphsData(CphsType.SHORT);
                                break;
                            default:
                                super.handleMessage(msg);
                                break;
                        }
                    } else {
                        isRecordLoadResponse = true;
                        ar = (AsyncResult) msg.obj;
                        if (ar.exception == null) {
                            this.mIs3Gphonebook = true;
                        } else if ((ar.exception instanceof CommandException) && Error.SIM_ABSENT == ((CommandException) ar.exception).getCommandError()) {
                            this.mIsSimPowerDown = true;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Get PBR Done,mIsSimPowerDown: ");
                            stringBuilder.append(this.mIsSimPowerDown);
                            log(stringBuilder.toString());
                        }
                        this.mIsGetPBRDone = true;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Get PBR Done,mIs3Gphonebook: ");
                        stringBuilder.append(this.mIs3Gphonebook);
                        log(stringBuilder.toString());
                    }
                } else {
                    log("EVENT_GET_SIM_MATCHED_FILE_DONE");
                    isRecordLoadResponse = true;
                    onGetSimMatchedFileDone(msg);
                }
            } catch (RuntimeException exc) {
                logw("Exception parsing SIM record", exc);
            } catch (Throwable th) {
                if (null != null) {
                    onRecordLoaded();
                }
            }
        }
    }

    public void onReady() {
        super.onReady();
        if (this.bNeedSendRefreshBC && HW_SIM_REFRESH) {
            this.bNeedSendRefreshBC = false;
            synchronized (this) {
                this.mIccRefreshRegistrants.notifyRegistrants();
            }
        }
    }

    public boolean beforeHandleSimRefresh(IccRefreshResponse refreshResponse) {
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            ApnReminder.getInstance(this.mContext, getSlotId()).getCust().setSimRefreshingState(true);
        } else {
            ApnReminder.getInstance(this.mContext).getCust().setSimRefreshingState(true);
        }
        int slotId = HwAddonTelephonyFactory.getTelephony().getDefault4GSlotId();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("beforeHandleSimRefresh->getDefault4GSlotId, slotId: ");
        stringBuilder.append(slotId);
        rlog(stringBuilder.toString());
        switch (refreshResponse.refreshResult) {
            case 0:
                rlog("beforeHandleSimRefresh with REFRESH_RESULT_FILE_UPDATE");
                if (HW_IS_CHINA_TELECOM && this.mParentApp != null && uiccCardApplicationUtils.getUiccCard(this.mParentApp) == UiccController.getInstance().getUiccCard(slotId)) {
                    rlog("Do not handleSimRefresh with SIM_FILE_UPDATED sent by RUIM.");
                    return true;
                } else if (hwCustHandleSimRefresh(refreshResponse.efId)) {
                    return true;
                }
                break;
            case 1:
                rlog("beforeHandleSimRefresh with SIM_REFRESH_INIT");
                if (!HW_IS_CHINA_TELECOM || this.mParentApp == null || uiccCardApplicationUtils.getUiccCard(this.mParentApp) != UiccController.getInstance().getUiccCard(slotId)) {
                    if (HW_SIM_REFRESH) {
                        this.bNeedSendRefreshBC = true;
                        break;
                    }
                }
                rlog("Do not handleSimRefresh with REFRESH_RESULT_INIT sent by RUIM.");
                return true;
                break;
            case 2:
                rlog("beforeHandleSimRefresh with SIM_REFRESH_RESET");
                break;
            default:
                rlog("beforeHandleSimRefresh with unknown operation");
                break;
        }
        return false;
    }

    private boolean hwCustHandleSimRefresh(int efid) {
        int i = 0;
        if (HwVolteChrManagerImpl.MAX_MONITOR_TIME == efid) {
            String strEFID = SystemProperties.get(pRefreshMultifileProp, "");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("The strEFID is: ");
            stringBuilder.append(strEFID);
            rlog(stringBuilder.toString());
            if (strEFID.isEmpty()) {
                rlog("handleSimRefresh with no multifile found");
                return false;
            }
            StringBuilder stringBuilder2;
            SystemProperties.set(pRefreshMultifileProp, "");
            String strEFIDExtra = SystemProperties.get(pRefreshMultifilePropExtra, "");
            if (!strEFIDExtra.isEmpty()) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("The strEFIDExtra is: ");
                stringBuilder2.append(strEFIDExtra);
                rlog(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(strEFID);
                stringBuilder2.append(',');
                stringBuilder2.append(strEFIDExtra);
                strEFID = stringBuilder2.toString();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("The strEFID is: ");
                stringBuilder2.append(strEFID);
                rlog(stringBuilder2.toString());
                SystemProperties.set(pRefreshMultifilePropExtra, "");
            }
            strEFIDs = strEFID.split(",");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("strEFIDs.length()");
            stringBuilder2.append(strEFIDs.length);
            rlog(stringBuilder2.toString());
            while (i < strEFIDs.length) {
                try {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("handleSimRefresh with strEFIDs[i]: ");
                    stringBuilder2.append(strEFIDs[i]);
                    rlog(stringBuilder2.toString());
                    int EFID = Integer.parseInt(strEFIDs[i], 16);
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("handleSimRefresh with EFID: ");
                    stringBuilder3.append(EFID);
                    rlog(stringBuilder3.toString());
                    handleFileUpdate(EFID);
                } catch (NumberFormatException e) {
                    rlog("handleSimRefresh with convert EFID from String to Int error");
                }
                i++;
            }
            rlog("notify mIccRefreshRegistrants");
            synchronized (this) {
                this.mIccRefreshRegistrants.notifyRegistrants();
            }
            return true;
        }
        rlog("refresh with only one EF ID");
        return false;
    }

    public boolean afterHandleSimRefresh(IccRefreshResponse refreshResponse) {
        switch (refreshResponse.refreshResult) {
            case 0:
                rlog("afterHandleSimRefresh with REFRESH_RESULT_FILE_UPDATE");
                synchronized (this) {
                    this.mIccRefreshRegistrants.notifyRegistrants();
                }
                break;
            case 1:
                rlog("afterHandleSimRefresh with SIM_REFRESH_INIT");
                break;
            case 2:
                rlog("afterHandleSimRefresh with SIM_REFRESH_RESET");
                if (HW_SIM_REFRESH) {
                    this.bNeedSendRefreshBC = true;
                    break;
                }
                break;
            default:
                rlog("afterHandleSimRefresh with unknown operation");
                break;
        }
        return false;
    }

    protected static void rlog(String string) {
        Rlog.d(TAG, string);
    }

    public byte[] getGID1() {
        if (this.mEfGid1 != null) {
            return Arrays.copyOf(this.mEfGid1, this.mEfGid1.length);
        }
        return new byte[]{(byte) 0};
    }

    public void setVoiceMailNumber(String voiceNumber) {
        this.mVoiceMailNum = voiceNumber;
    }

    public void loadFile(String matchPath, String matchFile) {
        if (matchPath != null && matchPath.length() >= 2) {
            int i = 0;
            if (matchPath.substring(0, 2).equalsIgnoreCase("0x") && matchFile != null && matchFile.length() >= 2 && matchFile.substring(0, 2).equalsIgnoreCase("0x")) {
                String matchFileString = matchFile.substring(2);
                int matchField = 0;
                while (i < matchFileString.length()) {
                    matchField = (int) (((double) matchField) + (Math.pow(16.0d, (double) ((matchFileString.length() - i) - 1)) * ((double) HwIccUtils.hexCharToInt(matchFileString.charAt(i)))));
                    i++;
                }
                Message message = this.handlerEx.obtainMessage(2);
                Bundle data = new Bundle();
                data.putString(VirtualNets.MATCH_PATH, matchPath);
                data.putString(VirtualNets.MATCH_FILE, matchFile);
                message.setData(data);
                this.mRecordsToLoad++;
                this.mFh.loadEFTransparent(matchPath.substring(2), matchField, message);
            }
        }
    }

    public boolean loadSpecialPathFile(String matchPath, String matchFile, int msgType) {
        int i = 0;
        if (matchPath == null || matchPath.length() < 2 || !matchPath.substring(0, 2).equalsIgnoreCase("0x") || matchFile == null || matchFile.length() < 2 || !matchFile.substring(0, 2).equalsIgnoreCase("0x")) {
            return false;
        }
        String matchFileString = matchFile.substring(2);
        int matchField = 0;
        while (i < matchFileString.length()) {
            matchField = (int) (((double) matchField) + (Math.pow(16.0d, (double) ((matchFileString.length() - i) - 1)) * ((double) HwIccUtils.hexCharToInt(matchFileString.charAt(i)))));
            i++;
        }
        Message message = this.handlerEx.obtainMessage(msgType);
        Bundle data = new Bundle();
        data.putString(VirtualNets.MATCH_PATH, matchPath);
        data.putString(VirtualNets.MATCH_FILE, matchFile);
        message.setData(data);
        this.mFh.loadEFTransparent(matchPath.substring(2), matchField, message);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("loadSpecialPathFile: matchPath:");
        stringBuilder.append(matchPath);
        stringBuilder.append(" matchFile:");
        stringBuilder.append(matchFile);
        stringBuilder.append(" msgType:");
        stringBuilder.append(msgType);
        log(stringBuilder.toString());
        return true;
    }

    public boolean loadCarrierFile(String matchPath, String matchFile) {
        return loadSpecialPathFile(matchPath, matchFile, 4);
    }

    protected void onOperatorNumericLoadedHw() {
        this.globalChecker.onOperatorNumericLoaded();
        onImsiAndAdLoadedHw(this.mImsi);
    }

    protected void onAllRecordsLoadedHw() {
        updateSarMnc(this.mImsi);
        this.globalChecker.onAllRecordsLoaded();
    }

    protected void loadGID1() {
        this.globalChecker.loadGID1();
    }

    protected void onIccIdLoadedHw() {
        this.globalChecker.onIccIdLoadedHw();
        processGetIccIdDone(this.mIccId);
        if (getIccidSwitch()) {
            sendIccidDoneBroadcast(this.mIccId);
        }
    }

    public void processGetIccIdDone(String iccid) {
        if (HwHotplugController.IS_HOTSWAP_SUPPORT) {
            HwHotplugController.getInstance().onHotplugIccIdChanged(iccid, getSlotId());
        }
        updateCarrierFile(getSlotId(), 1, iccid);
    }

    protected void onImsiLoadedHw() {
        this.globalChecker.onImsiLoaded();
    }

    private void onImsiAndAdLoadedHw(String imsi) {
        String rbImsi = null;
        String rbMccmnc = null;
        if (imsi != null) {
            String mccmnc;
            if (3 == this.mMncLength) {
                mccmnc = imsi.substring(0, 6);
            } else {
                mccmnc = imsi.substring(0, 5);
            }
            if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(getSlotId()))) {
                rbImsi = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerImsi(Integer.valueOf(getSlotId()));
                rbMccmnc = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric(Integer.valueOf(getSlotId()));
            }
            if (rbImsi == null || rbImsi.length() <= 0 || rbMccmnc == null || rbMccmnc.length() <= 0) {
                updateCarrierFile(getSlotId(), 2, imsi);
                updateCarrierFile(getSlotId(), 3, mccmnc);
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Set RoamingBroker mccmnc=");
            stringBuilder.append(rbMccmnc);
            Rlog.d("SIMRecords", stringBuilder.toString());
            updateCarrierFile(getSlotId(), 2, rbImsi);
            updateCarrierFile(getSlotId(), 3, rbMccmnc);
        }
    }

    protected void updateCarrierFile(int slotId, int fileType, String fileValue) {
        this.mHwCarrierCardManager.updateCarrierFile(slotId, fileType, fileValue);
    }

    protected void custMncLength(String mcc) {
        String mncHaving2Digits = SystemProperties.get("ro.config.mnc_having_2digits", "");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mnc_having_2digits = ");
        stringBuilder.append(mncHaving2Digits);
        Rlog.d("SIMRecords", stringBuilder.toString());
        if (mncHaving2Digits != null) {
            int i = 0;
            String custMccmncCode = this.mImsi.substring(0, 5);
            String[] plmns = mncHaving2Digits.split(",");
            int length = plmns.length;
            while (i < length) {
                if (custMccmncCode.equals(plmns[i])) {
                    this.mMncLength = 2;
                    return;
                }
                i++;
            }
        } else if (mcc.equals("416") && 3 == this.mMncLength) {
            Rlog.d("SIMRecords", "SIMRecords: customize for Jordan sim card, make the mcnLength to 2");
            this.mMncLength = 2;
        }
    }

    public String getOperatorNumericEx(ContentResolver cr, String name) {
        if (cr == null || this.mImsi == null || "".equals(this.mImsi) || name == null || "".equals(name)) {
            return getOperatorNumeric();
        }
        String hwImsiPlmnEx = System.getString(cr, name);
        if (!(hwImsiPlmnEx == null || "".equals(hwImsiPlmnEx))) {
            for (String plmn_item : hwImsiPlmnEx.split(",")) {
                if (this.mImsi.startsWith(plmn_item)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getOperatorNumericEx: ");
                    stringBuilder.append(plmn_item);
                    rlog(stringBuilder.toString());
                    return plmn_item;
                }
            }
        }
        return getOperatorNumeric();
    }

    public String getVoiceMailNumber() {
        ApnReminder apnReminder;
        if (isMultiSimEnabled) {
            apnReminder = ApnReminder.getInstance(this.mContext, getSlotId());
        } else {
            apnReminder = ApnReminder.getInstance(this.mContext);
        }
        if (!apnReminder.isPopupApnSettingsEmpty()) {
            rlog("getVoiceMailNumber: PopupApnSettings not empty");
            if (this.mVmConfig != null) {
                this.mVmConfig.resetVoiceMailLoadFlag();
                long token = Binder.clearCallingIdentity();
                try {
                    setVoiceMailByCountry(getOperatorNumeric());
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }
        return super.getVoiceMailNumber();
    }

    protected void resetRecords() {
        super.resetRecords();
        this.mIs3Gphonebook = false;
        this.mIsGetPBRDone = false;
        this.mIsSimPowerDown = false;
        this.mSstPnnVaild = true;
        this.mPnnRecords = null;
    }

    protected void getPbrRecordSize() {
        this.mFh.loadEFLinearFixedAll(20272, obtainMessage(EVENT_GET_PBR_DONE));
        this.mRecordsToLoad++;
    }

    public int getSlotId() {
        if (this.mParentApp != null && this.mParentApp.getUiccCard() != null) {
            return this.mParentApp.getUiccCard().getPhoneId();
        }
        log("error , mParentApp.getUiccCard  is null");
        return 0;
    }

    protected void setVoiceMailByCountry(String spn) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setVoiceMailByCountry spn ");
        stringBuilder.append(spn);
        stringBuilder.append(" for slot");
        stringBuilder.append(getSlotId());
        log(stringBuilder.toString());
        String number;
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(getSlotId()))) {
                number = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerVoicemail(getSlotId());
                spn = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric(Integer.valueOf(getSlotId()));
                if (this.mVmConfig.containsCarrier(spn, getSlotId())) {
                    if (TextUtils.isEmpty(number)) {
                        this.mIsVoiceMailFixed = this.mVmConfig.getVoiceMailFixed(spn, getSlotId());
                        this.mVoiceMailNum = this.mVmConfig.getVoiceMailNumber(spn, getSlotId());
                    } else {
                        this.mIsVoiceMailFixed = true;
                        this.mVoiceMailNum = number;
                    }
                    this.mVoiceMailTag = this.mVmConfig.getVoiceMailTag(spn, getSlotId());
                }
            } else if (this.mVmConfig.containsCarrier(spn, getSlotId())) {
                this.mIsVoiceMailFixed = this.mVmConfig.getVoiceMailFixed(spn, getSlotId());
                this.mVoiceMailNum = this.mVmConfig.getVoiceMailNumber(spn, getSlotId());
                this.mVoiceMailTag = this.mVmConfig.getVoiceMailTag(spn, getSlotId());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("VoiceMailConfig doesn't contains the carrier");
                stringBuilder.append(spn);
                stringBuilder.append(" for slot");
                stringBuilder.append(getSlotId());
                log(stringBuilder.toString());
            }
        } else if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated()) {
            spn = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric();
            number = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerVoicemail();
            String previousOp = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric();
            spn = previousOp != null ? previousOp : spn;
            if (this.mVmConfig.containsCarrier(spn)) {
                if (TextUtils.isEmpty(number)) {
                    this.mIsVoiceMailFixed = this.mVmConfig.getVoiceMailFixed(spn);
                    this.mVoiceMailNum = this.mVmConfig.getVoiceMailNumber(spn);
                } else {
                    this.mIsVoiceMailFixed = true;
                    this.mVoiceMailNum = number;
                }
                this.mVoiceMailTag = this.mVmConfig.getVoiceMailTag(spn);
            }
        } else {
            super.setVoiceMailByCountry(spn);
        }
    }

    protected boolean checkFileInServiceTable(int efid, UsimServiceTable usimServiceTable, byte[] data) {
        boolean serviceStatus = true;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("check file status in serivce table ");
        stringBuilder.append(efid);
        rlog(stringBuilder.toString());
        int mSstSpnValue;
        StringBuilder stringBuilder2;
        if (efid == 28486) {
            rlog("check EF_SPN serivice in serivice table!!");
            if (this.mParentApp.getUiccCard().isApplicationOnIcc(AppType.APPTYPE_USIM)) {
                if (usimServiceTable == null || usimServiceTable.isAvailable(UsimService.SPN)) {
                    return true;
                }
                rlog("EF_SPN is disable in 3G card!!");
                return false;
            } else if (!this.mParentApp.getUiccCard().isApplicationOnIcc(AppType.APPTYPE_SIM)) {
                return true;
            } else {
                mSstSpnValue = (data[4] & 15) & 3;
                if (3 == mSstSpnValue) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("SST: 2G Sim,SPNVALUE enabled SPNVALUE = ");
                    stringBuilder2.append(mSstSpnValue);
                    rlog(stringBuilder2.toString());
                    return true;
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SST: 2G Sim,SPNVALUE disabled  SPNVALUE = ");
                stringBuilder2.append(mSstSpnValue);
                rlog(stringBuilder2.toString());
                return false;
            }
        } else if (efid != 28613) {
            return true;
        } else {
            rlog("check EF_PNN serivice in serivice table!!");
            if (this.mParentApp.getUiccCard().isApplicationOnIcc(AppType.APPTYPE_USIM)) {
                if (!(usimServiceTable == null || usimServiceTable.isAvailable(UsimService.PLMN_NETWORK_NAME))) {
                    rlog("EF_PNN is disable in 3G or 4G card!!");
                    serviceStatus = false;
                }
            } else if (this.mParentApp.getUiccCard().isApplicationOnIcc(AppType.APPTYPE_SIM) && data != null && data.length > 12) {
                mSstSpnValue = data[12] & 48;
                if (48 == mSstSpnValue) {
                    serviceStatus = true;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("SST: 2G Sim,PNNVALUE enabled PnnVALUE = ");
                    stringBuilder2.append(mSstSpnValue);
                    rlog(stringBuilder2.toString());
                } else {
                    serviceStatus = false;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("SST: 2G Sim,PNNVALUE disabled  PnnVALUE = ");
                    stringBuilder2.append(mSstSpnValue);
                    rlog(stringBuilder2.toString());
                }
            }
            this.mSstPnnVaild = serviceStatus;
            return serviceStatus;
        }
    }

    protected void loadEons() {
        this.mFh.loadEFLinearFixedAll(28614, obtainMessage(EVENT_GET_ALL_OPL_RECORDS_DONE));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixedAll(28613, obtainMessage(EVENT_GET_ALL_PNN_RECORDS_DONE));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(28436, obtainMessage(EVENT_GET_SPN_CPHS_DONE));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(28440, obtainMessage(EVENT_GET_SPN_SHORT_CPHS_DONE));
        this.mRecordsToLoad++;
    }

    public String getEons() {
        return this.mEons.getEons();
    }

    public boolean isEonsDisabled() {
        return this.mEons.isEonsDisabled();
    }

    public boolean updateEons(String regOperator, int lac) {
        return this.mEons.updateEons(regOperator, lac, getOperatorNumeric());
    }

    public ArrayList<OperatorInfo> getEonsForAvailableNetworks(ArrayList<OperatorInfo> avlNetworks) {
        return this.mEons.getEonsForAvailableNetworks(avlNetworks);
    }

    protected void initFdnPsStatus(int slotId) {
        if (HuaweiTelephonyConfigs.isPsRestrictedByFdn()) {
            new QueryHandler(this.mContext.getContentResolver()).startQuery(0, null, ContentUris.withAppendedId(Uri.parse("content://icc/fdn/subId/"), (long) slotId), new String[]{"number"}, null, null, null);
        }
    }

    public void sendDualSimChangeBroadcast(boolean isSimImsiRefreshing, String mLastImsi, String mImsi) {
        if (isSimImsiRefreshing && mLastImsi != null && mImsi != null && !mLastImsi.equals(mImsi)) {
            ActivityManagerNative.broadcastStickyIntent(new Intent("android.intent.action.ACTION_DUAL_SIM_IMSI_CHANGE"), null, 0);
            Rlog.d("SIMRecords", "dual sim imsi change");
        }
    }

    public void loadCardSpecialFile(int fileid) {
        if (fileid != 20276) {
            Rlog.d("SIMRecords", "no fileid found for load");
        } else if (this.isEnsEnabled) {
            this.mFh.loadEFTransparent(20276, obtainMessage(201));
            this.mRecordsToLoad++;
        }
    }

    public String getActingHplmn() {
        return this.mActingHplmn;
    }

    private int[] getSimPlmnDigits(byte[] data) {
        if (data == null) {
            return new int[]{15};
        }
        int[] simPlmn = new int[]{0, 0, 0, 0, 0, 0};
        simPlmn[0] = data[0] & 15;
        simPlmn[1] = (data[0] >> 4) & 15;
        simPlmn[2] = data[1] & 15;
        simPlmn[3] = data[2] & 15;
        simPlmn[4] = (data[2] >> 4) & 15;
        simPlmn[5] = (data[1] >> 4) & 15;
        return simPlmn;
    }

    protected void refreshCardType() {
        if (this.mHwCustHwSIMRecords != null) {
            this.mHwCustHwSIMRecords.refreshCardType();
        }
    }

    private boolean isNeedSetPnn() {
        if (this.mSstPnnVaild) {
            return true;
        }
        String mccmnc = getOperatorNumeric();
        String plmnsConfig = System.getString(this.mContext.getContentResolver(), "hw_sst_pnn_by_mccmnc");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isNeedSetPnn: mccmnc = ");
        stringBuilder.append(mccmnc);
        stringBuilder.append(" plmnsConfig = ");
        stringBuilder.append(plmnsConfig);
        Rlog.d("SIMRecords", stringBuilder.toString());
        if (TextUtils.isEmpty(plmnsConfig) || TextUtils.isEmpty(mccmnc)) {
            return true;
        }
        String[] plmns = plmnsConfig.split(",");
        int length = plmns.length;
        int i = 0;
        while (i < length) {
            String plmn = plmns[i];
            if (plmn == null || !plmn.equals(mccmnc)) {
                i++;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("isNeedSetPnn: mccmnc = ");
                stringBuilder.append(mccmnc);
                stringBuilder.append(" no need set PNN from card.");
                Rlog.d("SIMRecords", stringBuilder.toString());
                return false;
            }
        }
        return true;
    }

    public boolean isHwCustDataRoamingOpenArea() {
        if (this.mHwCustHwSIMRecords != null) {
            return this.mHwCustHwSIMRecords.isHwCustDataRoamingOpenArea();
        }
        return false;
    }

    public void dispose() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Disposing HwSimRecords ");
        stringBuilder.append(this);
        log(stringBuilder.toString());
        this.mHwCarrierCardManager.destory(getSlotId(), this);
        this.mContext.unregisterReceiver(this.mIntentReceiver);
        super.dispose();
    }

    protected void loadSimMatchedFileFromRilCache() {
        if (this.mCi != null) {
            this.mCi.getSimMatchedFileFromRilCache(28589, obtainMessage(42));
            this.mRecordsToLoad++;
            this.mCi.getSimMatchedFileFromRilCache(28472, obtainMessage(42));
            this.mRecordsToLoad++;
            this.mCi.getSimMatchedFileFromRilCache(28478, obtainMessage(42));
            this.mRecordsToLoad++;
            this.mCi.getSimMatchedFileFromRilCache(28479, obtainMessage(42));
            this.mRecordsToLoad++;
        }
    }

    protected void onGetSimMatchedFileDone(Message msg) {
        if (msg != null) {
            AsyncResult asyncResult = (AsyncResult) msg.obj;
            AsyncResult ar = asyncResult;
            if (asyncResult != null) {
                IccIoResult resultEx = ar.result;
                int fileId = resultEx.getFileId();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onGetSimMatchedFileDone: isValid=");
                stringBuilder.append(resultEx.isValidIccioResult());
                stringBuilder.append(", fileId=0x");
                stringBuilder.append(Integer.toHexString(fileId));
                log(stringBuilder.toString());
                if (resultEx.isValidIccioResult()) {
                    Message response = obtainMessage(getOriginalSimIoEventId(fileId));
                    if (!(fileId == 28436 || fileId == 28440 || fileId == 28472 || fileId == 28486)) {
                        if (fileId != 28589) {
                            switch (fileId) {
                                case 28478:
                                case 28479:
                                    break;
                                default:
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("onGetSimMatchedFileDone: do nothing for fileId = 0x");
                                    stringBuilder2.append(Integer.toHexString(fileId));
                                    loge(stringBuilder2.toString());
                                    break;
                            }
                        }
                        IccIoResult result = new IccIoResult(resultEx.sw1, resultEx.sw2, IccUtils.bytesToHexString(resultEx.payload));
                        this.mRecordsToLoad++;
                        AsyncResult.forMessage(response, result, ar.exception);
                        response.sendToTarget();
                        return;
                    }
                    byte[] data = resultEx.payload;
                    this.mRecordsToLoad++;
                    AsyncResult.forMessage(response, data, ar.exception);
                    response.sendToTarget();
                    return;
                }
                executOriginalSimIoRequest(fileId);
                return;
            }
        }
        loge("onGetSimMatchedFileDone: msg or AsyncResult is null, return.");
    }

    private void executOriginalSimIoRequest(int fileId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("executOriginalSimIoRequest for fileId = 0x");
        stringBuilder.append(Integer.toHexString(fileId));
        log(stringBuilder.toString());
        if (!(fileId == 28436 || fileId == 28440 || fileId == 28472 || fileId == 28486)) {
            if (fileId != 28589) {
                switch (fileId) {
                    case 28478:
                    case 28479:
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("executOriginalSimIoRequest: do nothing for fileId=0x");
                        stringBuilder.append(Integer.toHexString(fileId));
                        loge(stringBuilder.toString());
                        return;
                }
            }
            CommandsInterface commandsInterface = this.mCi;
            IccFileHandler iccFileHandler = this.mFh;
            commandsInterface.iccIOForApp(176, 28589, this.mFh.getEFPath(28589), 0, 0, 4, null, null, this.mParentApp.getAid(), obtainMessage(getOriginalSimIoEventId(fileId)));
            this.mRecordsToLoad++;
            return;
        }
        this.mFh.loadEFTransparent(fileId, obtainMessage(getOriginalSimIoEventId(fileId)));
        this.mRecordsToLoad++;
    }

    private int getOriginalSimIoEventId(int fileId) {
        if (!(fileId == 28436 || fileId == 28440)) {
            if (fileId == 28472) {
                return getEventIdFromMap("EVENT_GET_SST_DONE");
            }
            if (fileId != 28486) {
                if (fileId == 28589) {
                    return getEventIdFromMap("EVENT_GET_AD_DONE");
                }
                switch (fileId) {
                    case 28478:
                        return getEventIdFromMap("EVENT_GET_GID1_DONE");
                    case 28479:
                        return getEventIdFromMap("EVENT_GET_GID2_DONE");
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getOriginalSimIoEventId: Error, do nothing for fileId= 0x");
                        stringBuilder.append(Integer.toHexString(fileId));
                        loge(stringBuilder.toString());
                        return -1;
                }
            }
        }
        return getEventIdFromMap("EVENT_GET_SPN_DONE");
    }
}
