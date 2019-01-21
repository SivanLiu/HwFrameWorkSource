package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.uicc.HwRuimRecords;
import com.android.internal.telephony.uicc.HwSIMRecords;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import huawei.cust.HwCarrierConfigPolicy;
import huawei.cust.aidl.SimFileInfo;
import huawei.cust.aidl.SimMatchRule;
import huawei.cust.aidl.SpecialFile;
import java.util.List;

public class HwCarrierConfigCardManager {
    public static final int HW_CARRIER_FILE_AFTER_LOADED = 8;
    public static final int HW_CARRIER_FILE_C_IMSI = 4;
    public static final int HW_CARRIER_FILE_GID1 = 5;
    public static final int HW_CARRIER_FILE_GID2 = 6;
    public static final int HW_CARRIER_FILE_G_IMSI = 2;
    public static final int HW_CARRIER_FILE_G_MCCMNC = 3;
    public static final int HW_CARRIER_FILE_ICCID = 1;
    public static final int HW_CARRIER_FILE_SPN = 7;
    public static final int HW_CDMA_MCCMNC_LENGTH = 5;
    private static final int INVALID = -1;
    private static final String LOG_TAG = "HwCarrierConfigCardManager";
    private static final int MAX_PHONE_COUNT = TelephonyManager.getDefault().getPhoneCount();
    public static final int RULE_GID1 = 16;
    public static final int RULE_GID2 = 32;
    public static final int RULE_ICCID = 2;
    public static final int RULE_IMSI = 4;
    public static final int RULE_MCCMNC = 1;
    public static final int RULE_NONE = 0;
    public static final int RULE_SPECIAL = 64;
    public static final int RULE_SPN = 8;
    public static final int RULE_UNKNOW = -1;
    public static final int SENSITIVE_MSG_PRINT_MAX_LEN = 8;
    public static final int SPECIAL_FILE_STATE_DONE = 2;
    public static final int SPECIAL_FILE_STATE_NONE = 0;
    public static final int SPECIAL_FILE_STATE_REQUESTING = 1;
    private static final boolean isMultiSimEnabled = TelephonyManager.getDefault().isMultiSimEnabled();
    private static HwCarrierConfigCardRecord[] mCardRecords = new HwCarrierConfigCardRecord[MAX_PHONE_COUNT];
    private static HwCarrierConfigPolicy mCarrierPolicy;
    private static SimMatchRule[] mCarrierPolicyRule = new SimMatchRule[MAX_PHONE_COUNT];
    private static boolean[] mCarrierPolicyUpdated = new boolean[MAX_PHONE_COUNT];
    private static final Object mLock = new Object();
    private static HwRuimRecords[] mRuimRecords = new HwRuimRecords[MAX_PHONE_COUNT];
    private static HwSIMRecords[] mSimRecords = new HwSIMRecords[MAX_PHONE_COUNT];
    private static int[] mSpecialFileCount = new int[MAX_PHONE_COUNT];
    private static int[] mSpecialFileRequestState = new int[MAX_PHONE_COUNT];
    private static HwCarrierConfigCardManager sInstance;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                HwCarrierConfigCardManager.loge("intent is null, return");
                return;
            }
            int slotId = -1;
            if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {
                String simState = "UNKNOWN";
                try {
                    simState = (String) intent.getExtra("ss");
                    slotId = intent.getIntExtra("slot", -1000);
                } catch (ClassCastException ex) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Get Intent Extra ClassCastException ex:");
                    stringBuilder.append(ex.getMessage());
                    HwCarrierConfigCardManager.logd(stringBuilder.toString());
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("receive broadcast intent SIM_STATE_CHANGED, slotId:");
                stringBuilder2.append(slotId);
                stringBuilder2.append(" simState:");
                stringBuilder2.append(simState);
                HwCarrierConfigCardManager.logd(stringBuilder2.toString());
                if (!HwCarrierConfigCardManager.this.checkParaAndData(slotId)) {
                    HwCarrierConfigCardManager.loge("onReceive ACTION_SIM_STATE_CHANGED para error!", slotId);
                } else if ("ABSENT".equals(simState) || "LOCKED".equals(simState) || "CARD_IO_ERROR".equals(simState)) {
                    HwCarrierConfigCardManager.this.destoryAndUpdateSimFileInfo(slotId);
                }
            } else if ("com.huawei.intent.action.ACTION_SUBSCRIPTION_SET_UICC_RESULT".equals(intent.getAction())) {
                int slotId2 = -1;
                int subState = -1;
                int result = -1;
                try {
                    slotId2 = ((Integer) intent.getExtra("subscription", Integer.valueOf(-1))).intValue();
                    subState = ((Integer) intent.getExtra("newSubState", Integer.valueOf(-1))).intValue();
                    result = ((Integer) intent.getExtra("operationResult", Integer.valueOf(-1))).intValue();
                } catch (ClassCastException ex2) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Get Intent Extra ClassCastException ex:");
                    stringBuilder3.append(ex2.getMessage());
                    HwCarrierConfigCardManager.logd(stringBuilder3.toString());
                }
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("received broadcast intent ACTION_SUBSCRIPTION_SET_UICC_RESULT,slotId:");
                stringBuilder4.append(slotId2);
                stringBuilder4.append(" subState:");
                stringBuilder4.append(subState);
                stringBuilder4.append(" result:");
                stringBuilder4.append(result);
                HwCarrierConfigCardManager.logd(stringBuilder4.toString());
                if (!HwCarrierConfigCardManager.this.checkParaAndData(slotId2)) {
                    HwCarrierConfigCardManager.loge("onReceive ACTION_SUBSCRIPTION_SET_UICC_RESULT para error!", slotId2);
                } else if (result == 0 && subState == 0) {
                    HwCarrierConfigCardManager.this.destoryAndUpdateSimFileInfo(slotId2);
                }
            }
        }
    };

    private HwCarrierConfigCardManager(Context context) {
        for (int i = 0; i < MAX_PHONE_COUNT; i++) {
            mCarrierPolicyRule[i] = null;
            mCarrierPolicyUpdated[i] = false;
            mSpecialFileCount[i] = 0;
            mSpecialFileRequestState[i] = 0;
            mCardRecords[i] = new HwCarrierConfigCardRecord(i);
            mSimRecords[i] = null;
            mRuimRecords[i] = null;
        }
        mCarrierPolicy = HwCarrierConfigPolicy.getDefault();
        IntentFilter filter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        if (true == HwModemCapability.isCapabilitySupport(9)) {
            filter.addAction("com.huawei.intent.action.ACTION_SUBSCRIPTION_SET_UICC_RESULT");
        } else {
            logd("ACTION_SUBSCRIPTION_SET_UICC_RESULT action Not process for HISI");
        }
        context.registerReceiver(this.mReceiver, filter);
        logd("HwCarrierConfigCardManager constructor!");
    }

    public static HwCarrierConfigCardManager getDefault(Context context) {
        HwCarrierConfigCardManager hwCarrierConfigCardManager;
        logd("HwCarrierConfigCardManager getDefault()");
        synchronized (mLock) {
            if (sInstance == null) {
                sInstance = new HwCarrierConfigCardManager(context);
            }
            hwCarrierConfigCardManager = sInstance;
        }
        return hwCarrierConfigCardManager;
    }

    public void reportIccRecordInstance(int slotId, IccRecords iccRecords) {
        if (checkParaAndData(slotId)) {
            if (iccRecords instanceof HwSIMRecords) {
                mSimRecords[slotId] = (HwSIMRecords) iccRecords;
                mCardRecords[slotId].setGsmCardMode(true);
            } else if (iccRecords instanceof HwRuimRecords) {
                mRuimRecords[slotId] = (HwRuimRecords) iccRecords;
                mCardRecords[slotId].seCDMACardMode(true);
            }
            if (mRuimRecords[slotId] != null) {
                mCardRecords[slotId].setCDMAValid(true);
            }
            return;
        }
        loge("reportIccRecordInstance para error!", slotId);
    }

    public void updateCarrierFile(int slotId, int fileType, String fileValue) {
        try {
            if (checkParaAndData(slotId)) {
                StringBuilder stringBuilder;
                switch (fileType) {
                    case 1:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateIccid iccid=");
                        stringBuilder.append(givePrintableMsg(fileValue));
                        logd(stringBuilder.toString(), slotId);
                        mCardRecords[slotId].setIccid(fileValue);
                        break;
                    case 2:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateGImsi mccmnc=");
                        stringBuilder.append(givePrintableMsg(fileValue));
                        logd(stringBuilder.toString(), slotId);
                        mCardRecords[slotId].setGImsi(fileValue);
                        break;
                    case 3:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateGMCCMNC mccmnc=");
                        stringBuilder.append(fileValue);
                        logd(stringBuilder.toString(), slotId);
                        mCardRecords[slotId].setGMccmnc(fileValue);
                        break;
                    case 4:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateCImsi mccmnc=");
                        stringBuilder.append(givePrintableMsg(fileValue));
                        logd(stringBuilder.toString(), slotId);
                        mCardRecords[slotId].setCImsi(fileValue);
                        if (fileValue != null && fileValue.length() > 5) {
                            mCardRecords[slotId].setCMccmnc(fileValue.substring(0, 5));
                            break;
                        }
                    case 5:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("update gid1=");
                        stringBuilder.append(fileValue);
                        logd(stringBuilder.toString(), slotId);
                        mCardRecords[slotId].setGid1(fileValue);
                        break;
                    case 6:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("update gid2=");
                        stringBuilder.append(fileValue);
                        logd(stringBuilder.toString(), slotId);
                        mCardRecords[slotId].setGid2(fileValue);
                        break;
                    case 7:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("update spn=");
                        stringBuilder.append(fileValue);
                        logd(stringBuilder.toString(), slotId);
                        mCardRecords[slotId].setSpn(fileValue);
                        break;
                    case 8:
                        loge("HW_CARRIER_FILE_AFTER_LOADED", slotId);
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateCarrierFile unknow fileType=");
                        stringBuilder.append(fileType);
                        loge(stringBuilder.toString(), slotId);
                        return;
                }
                checkSimMatchInfo(slotId);
                return;
            }
            loge("updateCarrierFile para error!", slotId);
        } catch (Exception ex) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateCarrierFile Exception: ");
            stringBuilder2.append(ex.getMessage());
            loge(stringBuilder2.toString());
        }
    }

    public void addSpecialFileResult(boolean result, String filePath, String fileId, String value, int slotId) {
        if (!checkParaAndData(slotId)) {
            loge("addSpecialFileResult para error!", slotId);
        } else if (2 == mSpecialFileRequestState[slotId]) {
            loge("addSpecialFile has finished!", slotId);
        } else {
            StringBuilder stringBuilder;
            if (result) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("add filePath=");
                stringBuilder.append(filePath);
                stringBuilder.append(" fileId=");
                stringBuilder.append(fileId);
                stringBuilder.append(" value=");
                stringBuilder.append(value);
                logd(stringBuilder.toString(), slotId);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("addSpecialFileResult fail, filePath=");
                stringBuilder.append(filePath);
                stringBuilder.append(" fileId=");
                stringBuilder.append(fileId);
                loge(stringBuilder.toString(), slotId);
            }
            mCardRecords[slotId].setSpecialFileValue(filePath, fileId, value);
            int[] iArr = mSpecialFileCount;
            iArr[slotId] = iArr[slotId] - 1;
            if (mSpecialFileCount[slotId] == 0) {
                reportSpecialFileComplete(slotId);
            }
        }
    }

    private boolean checkParaAndData(int slotId) {
        if (slotId < 0 || slotId >= MAX_PHONE_COUNT) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error slotId:");
            stringBuilder.append(slotId);
            loge(stringBuilder.toString());
            return false;
        } else if (mCardRecords[slotId] != null) {
            return true;
        } else {
            loge("mCardRecords is null!", slotId);
            return false;
        }
    }

    private boolean mandatoryInfoCheckPass(int slotId) {
        if (checkParaAndData(slotId)) {
            int recordFlag = mCardRecords[slotId].getRecordFlag();
            if (7 == (recordFlag & 7)) {
                return true;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" recordFlag check fail, recordFlag=0x");
            stringBuilder.append(Integer.toHexString(recordFlag));
            logd(stringBuilder.toString(), slotId);
            return false;
        }
        loge("mandatoryInfoCheckPass para error!", slotId);
        return false;
    }

    private void checkSimMatchInfo(int slotId) {
        if (!checkParaAndData(slotId)) {
            loge("checkSimMatchInfo para error!", slotId);
        } else if (mCarrierPolicy == null) {
            loge("mCarrierPolicy is null!", slotId);
        } else if (!mandatoryInfoCheckPass(slotId)) {
        } else {
            if (mCarrierPolicyUpdated[slotId]) {
                logd("already called updateSimFileInfo before", slotId);
                return;
            }
            SimMatchRule simRule;
            if (mCarrierPolicyRule[slotId] == null) {
                logd("querySimMatchRule", slotId);
                simRule = mCarrierPolicy.querySimMatchRule(mCardRecords[slotId].getMccmnc(), mCardRecords[slotId].getIccid(), mCardRecords[slotId].getImsi(), slotId);
                mCarrierPolicyRule[slotId] = simRule;
            } else {
                simRule = mCarrierPolicyRule[slotId];
            }
            if (simRule == null) {
                logd("querySimMatchRule return null, query later!", slotId);
                return;
            }
            int ruleRet = simRule.getRule();
            if (ruleRet == 0) {
                logd("no need to call updateSimFileInfo", slotId);
                return;
            }
            StringBuilder stringBuilder;
            if ((ruleRet & 64) != 0) {
                if (1 == mSpecialFileRequestState[slotId]) {
                    logd("is requesting special file", slotId);
                    return;
                } else if (mSpecialFileRequestState[slotId] == 0) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("need to requet special files:");
                    stringBuilder.append(simRule.getSpecialFiles());
                    logd(stringBuilder.toString(), slotId);
                    if (requestSpecialFile(slotId, simRule.getSpecialFiles())) {
                        mSpecialFileRequestState[slotId] = 1;
                        logd("sent request special files!", slotId);
                        return;
                    }
                    loge("requestSpecialFile return fail!", slotId);
                    mSpecialFileRequestState[slotId] = 2;
                    mCardRecords[slotId].addSpecialFlag();
                }
            }
            if (mSimRecords == null) {
                ruleRet &= -57;
                logd("maybe single Ruim card, clear rule_spn/rule_gid1/rule_gid2!", slotId);
            }
            int recordFlag = mCardRecords[slotId].getRecordFlag();
            if (((~recordFlag) & ruleRet) != 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("sim info is not complete, ruleRet=0x");
                stringBuilder.append(Integer.toHexString(simRule.getRule()));
                stringBuilder.append(" recordFlag=0x");
                stringBuilder.append(Integer.toHexString(recordFlag));
                logd(stringBuilder.toString(), slotId);
                return;
            }
            SimFileInfo simInfo = new SimFileInfo();
            stringBuilder = new StringBuilder();
            stringBuilder.append("ruleRet=0x");
            stringBuilder.append(Integer.toHexString(ruleRet));
            stringBuilder.append(" recordFlag=0x");
            stringBuilder.append(Integer.toHexString(recordFlag));
            logd(stringBuilder.toString(), slotId);
            if ((4 & ruleRet) != 0) {
                simInfo.setImsi(mCardRecords[slotId].getImsi());
            }
            if ((1 & ruleRet) != 0) {
                simInfo.setMccMnc(mCardRecords[slotId].getMccmnc());
            }
            if ((2 & ruleRet) != 0) {
                simInfo.setIccid(mCardRecords[slotId].getIccid());
            }
            if ((8 & ruleRet) != 0) {
                simInfo.setSpn(mCardRecords[slotId].getSpn());
            }
            if ((16 & ruleRet) != 0) {
                simInfo.setGid1(mCardRecords[slotId].getGid1());
            }
            if ((32 & ruleRet) != 0) {
                simInfo.setGid2(mCardRecords[slotId].getGid2());
            }
            if ((64 & ruleRet) != 0) {
                simInfo.setSpecialFiles(mCardRecords[slotId].getSpecialFiles());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateSimFileInfo to policy simInfo=");
            stringBuilder.append(simInfo);
            logd(stringBuilder.toString(), slotId);
            mCarrierPolicy.updateSimFileInfo(simInfo, slotId);
            mCarrierPolicyUpdated[slotId] = true;
        }
    }

    private boolean requestSpecialFile(int slotId, List<SpecialFile> specialFiles) {
        if (!checkParaAndData(slotId)) {
            loge("requestSpecialFile para error!", slotId);
            return false;
        } else if (specialFiles == null || mSimRecords[slotId] == null) {
            mCardRecords[slotId].setSpecialFiles(specialFiles);
            return false;
        } else {
            mSpecialFileCount[slotId] = 0;
            mCardRecords[slotId].setSpecialFiles(specialFiles);
            for (SpecialFile sp : specialFiles) {
                if (sp.getFileId() != null) {
                    if (mSimRecords[slotId].loadCarrierFile(sp.getFilePath(), sp.getFileId())) {
                        int[] iArr = mSpecialFileCount;
                        iArr[slotId] = iArr[slotId] + 1;
                    } else {
                        loge("loadCarrierFile fail, check filePath or fileId!", slotId);
                        mSpecialFileCount[slotId] = 0;
                        return false;
                    }
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("request ");
            stringBuilder.append(mSpecialFileCount[slotId]);
            stringBuilder.append(" files.");
            logd(stringBuilder.toString(), slotId);
            if (mSpecialFileCount[slotId] != 0) {
                return true;
            }
            logd("no file in the specialFile list!", slotId);
            return false;
        }
    }

    private void reportSpecialFileComplete(int slotId) {
        if (checkParaAndData(slotId)) {
            logd("reportSpecialFileComplete", slotId);
            mSpecialFileRequestState[slotId] = 2;
            mCardRecords[slotId].addSpecialFlag();
            checkSimMatchInfo(slotId);
            return;
        }
        loge("reportSpecialFileComplete para error!", slotId);
    }

    public void destory(int slotId, IccRecords iccRecords) {
        if (checkParaAndData(slotId)) {
            mCarrierPolicyRule[slotId] = null;
            mCarrierPolicyUpdated[slotId] = false;
            mSpecialFileCount[slotId] = 0;
            mSpecialFileRequestState[slotId] = 0;
            if (mCardRecords[slotId] != null) {
                mCardRecords[slotId].dispose();
            }
            if (mSimRecords[slotId] == iccRecords) {
                mSimRecords[slotId] = null;
            }
            if (mRuimRecords[slotId] == iccRecords) {
                mRuimRecords[slotId] = null;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("clear Records for slotId=");
            stringBuilder.append(slotId);
            logd(stringBuilder.toString());
            return;
        }
        loge("destory para error!", slotId);
    }

    private static String givePrintableMsg(String msg) {
        if (msg == null) {
            return null;
        }
        if (msg.length() <= 8) {
            return msg;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(msg.substring(0, 8));
        stringBuilder.append("XXXXXXXXXXX");
        return stringBuilder.toString();
    }

    private static void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private static void logd(String msg, int slotId) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(slotId);
        stringBuilder.append("]");
        stringBuilder.append(msg);
        Rlog.d(str, stringBuilder.toString());
    }

    private static void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    private static void loge(String msg, int slotId) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(slotId);
        stringBuilder.append("]");
        stringBuilder.append(msg);
        Rlog.e(str, stringBuilder.toString());
    }

    private void destoryAndUpdateSimFileInfo(int slotId) {
        if (!checkParaAndData(slotId)) {
            loge("destoryAndUpdateSimFileInfo para error!", slotId);
        } else if (mCarrierPolicy == null) {
            loge("mCarrierPolicy is null!", slotId);
        } else {
            mCarrierPolicy.updateSimFileInfo(null, slotId);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("destoryAndUpdateSimFileInfo slotId=");
            stringBuilder.append(slotId);
            logd(stringBuilder.toString());
        }
    }

    public void updateCarrierFileIfNeed(int slotId, SIMRecords simRecords) {
        if (!checkParaAndData(slotId) || mCarrierPolicyUpdated[slotId] || simRecords == null) {
            loge("updateCarrierFileIfNeed: do nothing.");
            return;
        }
        loge("updateCarrierFileIfNeed: Maybe some exceptions occur, update carrier policy here.", slotId);
        mCardRecords[slotId].setIccid(simRecords.getIccId());
        String imsi = simRecords.getIMSI();
        String mccmnc = simRecords.getOperatorNumeric();
        if (isMultiSimEnabled) {
            if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(slotId))) {
                logd("updateCarrierFileIfNeed: MultiSimEnabled, RoamingBrokerActivated, use home imsi and mccmnc", slotId);
                imsi = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerImsi(Integer.valueOf(slotId));
                mccmnc = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric(Integer.valueOf(slotId));
            }
        } else if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated()) {
            logd("updateCarrierFileIfNeed: SingleSimEnabled, RoamingBrokerActivated, use home imsi and mccmnc", slotId);
            imsi = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerImsi();
            mccmnc = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric();
        }
        mCardRecords[slotId].setGImsi(imsi);
        mCardRecords[slotId].setGMccmnc(mccmnc);
        mCardRecords[slotId].setSpn(simRecords.getServiceProviderName());
        mCardRecords[slotId].setGid1(simRecords.getGid1());
        mCardRecords[slotId].setGid2(simRecords.getGid2());
        mCardRecords[slotId].addSpecialFlag();
        updateCarrierFile(slotId, 8, null);
    }

    public void setSingleModeCdmaCard(int slotId, boolean value) {
        if (checkParaAndData(slotId)) {
            mCardRecords[slotId].setSingleModeCdmaCard(value);
        } else {
            loge("setSingleModeCdmaCard para error!", slotId);
        }
    }
}
