package android.telephony;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.cover.CoverManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.FreezeScreenScene;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.HwTelephonyIntentsInner;
import com.android.internal.telephony.HwTelephonyProperties;
import com.android.internal.telephony.IHwTelephony;
import com.android.internal.telephony.IHwTelephony.Stub;
import com.android.internal.telephony.IPhoneCallback;
import huawei.android.app.admin.HwDevicePolicyManagerEx;
import java.util.List;

public class HwTelephonyManagerInner {
    public static final String CARD_TYPE_SIM1 = "gsm.sim1.type";
    public static final String CARD_TYPE_SIM2 = "gsm.sim2.type";
    private static final String[] CDMA_CPLMNS = new String[]{"46003", "45502", "46012"};
    public static final int CDMA_MODE = 0;
    private static final String CHR_BROADCAST_PERMISSION = "com.huawei.android.permission.GET_CHR_DATA";
    public static final int CT_NATIONAL_ROAMING_CARD = 41;
    public static final int CU_DUAL_MODE_CARD = 42;
    private static final String DISABLE_PUSH = "disable-push";
    public static final int DUAL_MODE_CG_CARD = 40;
    public static final int DUAL_MODE_TELECOM_LTE_CARD = 43;
    public static final int DUAL_MODE_UG_CARD = 50;
    private static final int ERROR = -1;
    public static final int EXTRA_VALUE_NEW_SIM = 1;
    public static final int EXTRA_VALUE_NOCHANGE = 4;
    public static final int EXTRA_VALUE_REMOVE_SIM = 2;
    public static final int EXTRA_VALUE_REPOSITION_SIM = 3;
    public static final int EXTR_VALUE_INSERT_SAME_SIM = 5;
    private static final String GC_ICCID = "8985231";
    public static final int GSM_MODE = 1;
    public static final String INTENT_KEY_DETECT_STATUS = "simDetectStatus";
    public static final String INTENT_KEY_NEW_SIM_SLOT = "newSIMSlot";
    public static final String INTENT_KEY_NEW_SIM_STATUS = "newSIMStatus";
    public static final String INTENT_KEY_SIM_COUNT = "simCount";
    private static final int LTE_OFF = 0;
    public static final int NOTIFY_CMODEM_STATUS_FAIL = -1;
    public static final int NOTIFY_CMODEM_STATUS_SUCCESS = 1;
    public static final int PHONE_EVENT_IMSA_TO_MAPCON = 4;
    public static final int PHONE_EVENT_RADIO_AVAILABLE = 1;
    public static final int PHONE_EVENT_RADIO_UNAVAILABLE = 2;
    private static final String PROP_LTETDD_ENABLED = "persist.radio.ltetdd_enabled";
    private static final String PROP_LTE_ENABLED = "persist.radio.lte_enabled";
    private static final String PROP_VALUE_C_CARD_PLMN = "gsm.sim.c_card.plmn";
    public static final int ROAM_MODE = 2;
    private static final int SERVICE_2G_OFF = 0;
    public static final int SINGLE_MODE_RUIM_CARD = 30;
    public static final int SINGLE_MODE_SIM_CARD = 10;
    public static final int SINGLE_MODE_USIM_CARD = 20;
    private static final int SUB_1 = 1;
    public static final int SUPPORT_SYSTEMAPP_GET_DEVICEID = 1;
    private static final String TAG = "HwTelephonyManagerInner";
    public static final int UNKNOWN_CARD = -1;
    private static String callingAppName = "";
    private static boolean haveCheckedAppName = false;
    private static String mDeviceIdAll = null;
    private static String mDeviceIdIMEI = null;
    private static HwTelephonyManagerInner sInstance = new HwTelephonyManagerInner();
    private HwDevicePolicyManagerEx mDpm = new HwDevicePolicyManagerEx();

    public enum DataSettingModeType {
        MODE_LTE_OFF,
        MODE_LTETDD_ONLY,
        MODE_LTE_AND_AUTO,
        MODE_ERROR
    }

    private HwTelephonyManagerInner() {
    }

    public static HwTelephonyManagerInner getDefault() {
        return sInstance;
    }

    private IHwTelephony getIHwTelephony() throws RemoteException {
        IHwTelephony iHwTelephony = Stub.asInterface(ServiceManager.getService("phone_huawei"));
        if (iHwTelephony != null) {
            return iHwTelephony;
        }
        throw new RemoteException("getIHwTelephony return null");
    }

    public String getDemoString() {
        try {
            return getIHwTelephony().getDemoString();
        } catch (RemoteException e) {
            Rlog.e(TAG, "getDemoString RemoteException");
            return "ERROR";
        }
    }

    private int getDefaultSim() {
        return 0;
    }

    public String getMeid() {
        return getMeid(getDefaultSim());
    }

    public String getMeid(int slotId) {
        try {
            return getIHwTelephony().getMeidForSubscriber(slotId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getPesn() {
        return getPesn(getDefaultSim());
    }

    public String getPesn(int slotId) {
        try {
            return getIHwTelephony().getPesnForSubscriber(slotId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getNVESN() {
        try {
            return getIHwTelephony().getNVESN();
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public void closeRrc() {
        if (1000 == Binder.getCallingUid()) {
            try {
                getIHwTelephony().closeRrc();
            } catch (RemoteException | NullPointerException e) {
            }
        }
    }

    public int getSubState(long subId) {
        try {
            return getIHwTelephony().getSubState((int) subId);
        } catch (RemoteException e) {
            return 0;
        }
    }

    public void setUserPrefDataSlotId(int slotId) {
        try {
            getIHwTelephony().setUserPrefDataSlotId(slotId);
        } catch (RemoteException | NullPointerException e) {
        }
    }

    public boolean checkCdmaSlaveCardMode(int mode) {
        String commrilMode = SystemProperties.get(HwTelephonyProperties.PROPERTY_COMMRIL_MODE, "NON_MODE");
        String cg_standby_mode = SystemProperties.get(HwTelephonyProperties.PROPERTY_CG_STANDBY_MODE, "home");
        if (!isFullNetworkSupported() || !"CG_MODE".equals(commrilMode)) {
            return false;
        }
        switch (mode) {
            case 0:
                if (!"roam_gsm".equals(cg_standby_mode)) {
                    return true;
                }
                break;
            case 1:
                if ("roam_gsm".equals(cg_standby_mode)) {
                    return true;
                }
                break;
            case 2:
                if (!"home".equals(cg_standby_mode)) {
                    return true;
                }
                break;
        }
        return false;
    }

    public boolean isFullNetworkSupported() {
        return SystemProperties.getBoolean(HwTelephonyProperties.PROPERTY_FULL_NETWORK_SUPPORT, false);
    }

    public boolean isChinaTelecom(int slotId) {
        return HuaweiTelephonyConfigs.isChinaTelecom() || isCTSimCard(slotId);
    }

    public boolean isCTSimCard(int slotId) {
        boolean isCTCardType;
        boolean result;
        String cplmn;
        int cardType = getCardType(slotId);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[isCTSimCard]: cardType = ");
        stringBuilder.append(cardType);
        Rlog.d(str, stringBuilder.toString());
        if (cardType == 30 || cardType == 41 || cardType == 43) {
            isCTCardType = true;
        } else {
            isCTCardType = false;
        }
        if (!isCTCardType || HwModemCapability.isCapabilitySupport(9)) {
            result = isCTCardType;
        } else {
            boolean isCdmaCplmn = false;
            cplmn = getCplmn(slotId);
            for (String mccmnc : CDMA_CPLMNS) {
                if (mccmnc.equals(cplmn)) {
                    isCdmaCplmn = true;
                    break;
                }
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[isCTSimCard]: hisi cdma  isCdmaCplmn = ");
            stringBuilder2.append(isCdmaCplmn);
            Rlog.d(str2, stringBuilder2.toString());
            result = isCdmaCplmn;
            if (TextUtils.isEmpty(cplmn)) {
                try {
                    result = getIHwTelephony().isCtSimCard(slotId);
                } catch (RemoteException e) {
                    Rlog.e(TAG, "isCTSimCard RemoteException");
                }
            }
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[isCTSimCard]: hisi cdma  isCdmaCplmn according iccid = ");
            stringBuilder2.append(result);
            Rlog.d(str2, stringBuilder2.toString());
        }
        if (result) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("gsm.sim.preiccid_");
            stringBuilder3.append(slotId);
            str = SystemProperties.get(stringBuilder3.toString(), "");
            if (GC_ICCID.equals(str)) {
                result = false;
                cplmn = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Hongkong GC card is not CT card:");
                stringBuilder4.append(str);
                Rlog.d(cplmn, stringBuilder4.toString());
            }
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("[isCTSimCard]: result = ");
        stringBuilder.append(result);
        Rlog.d(str, stringBuilder.toString());
        return result;
    }

    private String getCplmn(int slotId) {
        String result = "";
        String value = SystemProperties.get(PROP_VALUE_C_CARD_PLMN, "");
        if (!(value == null || "".equals(value))) {
            String[] substr = value.split(",");
            if (substr.length == 2 && Integer.parseInt(substr[1]) == slotId) {
                result = substr[0];
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCplmn for Slot : ");
        stringBuilder.append(slotId);
        stringBuilder.append(" result is : ");
        stringBuilder.append(result);
        Rlog.d(str, stringBuilder.toString());
        return result;
    }

    public boolean isCDMASimCard(int slotId) {
        int cardType = getCardType(slotId);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[isCDMASimCard]: cardType = ");
        stringBuilder.append(cardType);
        Rlog.d(str, stringBuilder.toString());
        if (!(cardType == 30 || cardType == 43)) {
            switch (cardType) {
                case 40:
                case 41:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    public int getCardType(int slotId) {
        if (slotId == 0) {
            return SystemProperties.getInt(CARD_TYPE_SIM1, -1);
        }
        if (slotId == 1) {
            return SystemProperties.getInt(CARD_TYPE_SIM2, -1);
        }
        return -1;
    }

    public boolean isDomesticCard(int slotId) {
        try {
            return getIHwTelephony().isDomesticCard(slotId);
        } catch (RemoteException e) {
            return true;
        } catch (NullPointerException e2) {
            return true;
        }
    }

    public boolean isCTCdmaCardInGsmMode() {
        try {
            return getIHwTelephony().isCTCdmaCardInGsmMode();
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public void setDefaultMobileEnable(boolean enabled) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setDefaultMobileEnable to ");
            stringBuilder.append(enabled);
            Rlog.d(str, stringBuilder.toString());
            getIHwTelephony().setDefaultMobileEnable(enabled);
        } catch (RemoteException | NullPointerException e) {
        }
    }

    public void setDataEnabledWithoutPromp(boolean enabled) {
        try {
            getIHwTelephony().setDataEnabledWithoutPromp(enabled);
        } catch (RemoteException | NullPointerException e) {
        }
    }

    public void setDataRoamingEnabledWithoutPromp(boolean enabled) {
        try {
            getIHwTelephony().setDataRoamingEnabledWithoutPromp(enabled);
        } catch (RemoteException e) {
            Rlog.e(TAG, "setDataRoamingEnabledWithoutPromp RemoteException");
        }
    }

    public int getDataState(long subId) {
        if (subId >= 0) {
            try {
                if (subId < ((long) TelephonyManager.getDefault().getPhoneCount())) {
                    return getIHwTelephony().getDataStateForSubscriber((int) subId);
                }
            } catch (RemoteException e) {
                return 0;
            } catch (NullPointerException e2) {
                return 0;
            }
        }
        return 0;
    }

    public void setLteServiceAbility(int ability) {
        try {
            getIHwTelephony().setLteServiceAbility(ability);
        } catch (RemoteException e) {
            Rlog.e(TAG, "setLteServiceAbility RemoteException");
        }
    }

    public int getLteServiceAbility() {
        try {
            return getIHwTelephony().getLteServiceAbility();
        } catch (RemoteException e) {
            return 0;
        }
    }

    public boolean isDualImsSupported() {
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            return HwModemCapability.isCapabilitySupport(21);
        }
        return false;
    }

    public boolean isImeiBindSlotSupported() {
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            return HwModemCapability.isCapabilitySupport(26);
        }
        return false;
    }

    public int getLteServiceAbility(int subId) {
        try {
            return getIHwTelephony().getLteServiceAbilityForSubId(subId);
        } catch (RemoteException e) {
            Rlog.e(TAG, "getLteServiceAbility RemoteException");
            return 0;
        }
    }

    public void setLteServiceAbility(int subId, int ability) {
        try {
            getIHwTelephony().setLteServiceAbilityForSubId(subId, ability);
        } catch (RemoteException e) {
            Rlog.e(TAG, "setLteServiceAbility RemoteException");
        }
    }

    public void setImsRegistrationState(int subId, boolean registered) {
        try {
            getIHwTelephony().setImsRegistrationStateForSubId(subId, registered);
        } catch (RemoteException e) {
            Rlog.e(TAG, "setImsRegistrationState RemoteException");
        }
    }

    public boolean isImsRegistered(int subId) {
        try {
            return getIHwTelephony().isImsRegisteredForSubId(subId);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isVolteAvailable(int subId) {
        try {
            return getIHwTelephony().isVolteAvailableForSubId(subId);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public boolean isVideoTelephonyAvailable(int subId) {
        try {
            return getIHwTelephony().isVideoTelephonyAvailableForSubId(subId);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isWifiCallingAvailable(int subId) {
        try {
            return getIHwTelephony().isWifiCallingAvailableForSubId(subId);
        } catch (RemoteException e) {
            return false;
        }
    }

    public void set2GServiceAbility(int ability) {
        try {
            getIHwTelephony().set2GServiceAbility(ability);
        } catch (RemoteException e) {
            Rlog.e(TAG, "set2GServiceAbility failed ,RemoteException");
        }
    }

    public int get2GServiceAbility() {
        try {
            return getIHwTelephony().get2GServiceAbility();
        } catch (RemoteException e) {
            return 0;
        }
    }

    public boolean isSubDeactivedByPowerOff(long sub) {
        Rlog.d(TAG, "In isSubDeactivedByPowerOff");
        try {
            return getIHwTelephony().isSubDeactivedByPowerOff(sub);
        } catch (RemoteException e) {
            Rlog.e(TAG, "isSubDeactivedByPowerOff RemoteException");
            return false;
        }
    }

    public boolean isNeedToRadioPowerOn(long sub) {
        Rlog.d(TAG, "In isNeedToRadioPowerOn");
        try {
            return getIHwTelephony().isNeedToRadioPowerOn(sub);
        } catch (RemoteException e) {
            Rlog.e(TAG, "isNeedToRadioPowerOn RemoteException");
            return true;
        }
    }

    public boolean isCardPresent(int slotId) {
        return TelephonyManager.getDefault().getSimState(slotId) != 1;
    }

    public void updateCrurrentPhone(int lteSlot) {
        try {
            getIHwTelephony().updateCrurrentPhone(lteSlot);
        } catch (RemoteException | NullPointerException e) {
        }
    }

    public void setDefaultDataSlotId(int slotId) {
        try {
            getIHwTelephony().setDefaultDataSlotId(slotId);
        } catch (RemoteException | NullPointerException e) {
        }
    }

    public int getDefault4GSlotId() {
        try {
            return getIHwTelephony().getDefault4GSlotId();
        } catch (RemoteException e) {
            return 0;
        }
    }

    public void setDefault4GSlotId(int slotId, Message msg) {
        Rlog.d(TAG, "In setDefault4GSlotId");
        try {
            getIHwTelephony().setDefault4GSlotId(slotId, msg);
        } catch (RemoteException e) {
            Rlog.e(TAG, "setDefault4GSlotId RemoteException");
        }
    }

    public boolean isSetDefault4GSlotIdEnabled() {
        Rlog.d(TAG, "In isSetDefault4GSlotIdEnabled");
        try {
            return getIHwTelephony().isSetDefault4GSlotIdEnabled();
        } catch (RemoteException e) {
            Rlog.e(TAG, "isSetDefault4GSlotIdEnabled RemoteException");
            return false;
        }
    }

    public void waitingSetDefault4GSlotDone(boolean waiting) {
        Rlog.d(TAG, "In waitingSetDefault4GSlotDone");
        try {
            getIHwTelephony().waitingSetDefault4GSlotDone(waiting);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RemoteException ex = ");
            stringBuilder.append(ex);
            Rlog.e(str, stringBuilder.toString());
        }
    }

    public String getIccATR() {
        String strATR = SystemProperties.get("gsm.sim.hw_atr", "null");
        String strATR1 = SystemProperties.get("gsm.sim.hw_atr1", "null");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(strATR);
        stringBuilder.append(",");
        stringBuilder.append(strATR1);
        strATR = stringBuilder.toString();
        String str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getIccATR: [");
        stringBuilder2.append(strATR);
        stringBuilder2.append("]");
        Rlog.d(str, stringBuilder2.toString());
        return strATR;
    }

    public DataSettingModeType getDataSettingMode() {
        boolean isLteEnabled = SystemProperties.getBoolean(PROP_LTE_ENABLED, true);
        boolean isLteTddEnabled = SystemProperties.getBoolean(PROP_LTETDD_ENABLED, false);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("in getDataSettingMode isLteEnabled=");
        stringBuilder.append(isLteEnabled);
        stringBuilder.append(" isLteTddEnabled=");
        stringBuilder.append(isLteTddEnabled);
        Rlog.d(str, stringBuilder.toString());
        if (!isLteEnabled) {
            return DataSettingModeType.MODE_LTE_OFF;
        }
        if (isLteTddEnabled) {
            return DataSettingModeType.MODE_LTETDD_ONLY;
        }
        return DataSettingModeType.MODE_LTE_AND_AUTO;
    }

    private void doSetPreferredNetworkType(int nwMode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[enter]doSetPreferredNetworkType nwMode:");
        stringBuilder.append(nwMode);
        Rlog.d(str, stringBuilder.toString());
        try {
            getIHwTelephony().setPreferredNetworkType(nwMode);
        } catch (RemoteException e) {
        } catch (Exception e2) {
            Rlog.e(TAG, "doSetPreferredNetworkType failed!");
        }
    }

    private void doSetDataSettingModeFromLteAndAuto(DataSettingModeType dataMode) {
        if (AnonymousClass1.$SwitchMap$android$telephony$HwTelephonyManagerInner$DataSettingModeType[dataMode.ordinal()] != 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("doSetDataSettingModeFromLteAndAuto failed! param err mode =");
            stringBuilder.append(dataMode);
            Rlog.e(str, stringBuilder.toString());
            return;
        }
        doSetPreferredNetworkType(30);
    }

    private void doSetDataSettingModeFromLteTddOnly(DataSettingModeType dataMode) {
        if (AnonymousClass1.$SwitchMap$android$telephony$HwTelephonyManagerInner$DataSettingModeType[dataMode.ordinal()] != 2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("doSetDataSettingModeLteTddOnly failed! param err mode =");
            stringBuilder.append(dataMode);
            Rlog.e(str, stringBuilder.toString());
            return;
        }
        doSetPreferredNetworkType(61);
    }

    public void setDataSettingMode(DataSettingModeType dataMode) {
        if (dataMode == DataSettingModeType.MODE_LTETDD_ONLY || dataMode == DataSettingModeType.MODE_LTE_AND_AUTO) {
            switch (dataMode) {
                case MODE_LTETDD_ONLY:
                    doSetDataSettingModeFromLteAndAuto(dataMode);
                    break;
                case MODE_LTE_AND_AUTO:
                    doSetDataSettingModeFromLteTddOnly(dataMode);
                    break;
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDataSettingMode failed! param err mode =");
        stringBuilder.append(dataMode);
        Rlog.e(str, stringBuilder.toString());
    }

    public boolean isSubDeactived(int subId) {
        return false;
    }

    public int getPreferredDataSubscription() {
        try {
            return getIHwTelephony().getPreferredDataSubscription();
        } catch (RemoteException e) {
            return -1;
        }
    }

    public int getOnDemandDataSubId() {
        try {
            return getIHwTelephony().getOnDemandDataSubId();
        } catch (RemoteException e) {
            return -1;
        }
    }

    public String getCdmaGsmImsi() {
        try {
            return getIHwTelephony().getCdmaGsmImsi();
        } catch (RemoteException e) {
            return null;
        }
    }

    public String getCdmaGsmImsiForSubId(int subId) {
        try {
            return getIHwTelephony().getCdmaGsmImsiForSubId(subId);
        } catch (RemoteException e) {
            Rlog.e(TAG, "getCdmaGsmImsiForSubId RemoteException");
            return null;
        }
    }

    public int getUiccCardType(int slotId) {
        try {
            return getIHwTelephony().getUiccCardType(slotId);
        } catch (RemoteException e) {
            return -1;
        }
    }

    public CellLocation getCellLocation(int slotId) {
        try {
            Bundle bundle = getIHwTelephony().getCellLocation(slotId);
            if (bundle != null) {
                if (!bundle.isEmpty()) {
                    CellLocation cl = CellLocation.newFromBundle(bundle, slotId);
                    if (cl == null || cl.isEmpty()) {
                        return null;
                    }
                    return cl;
                }
            }
            return null;
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getCdmaMlplVersion() {
        try {
            return getIHwTelephony().getCdmaMlplVersion();
        } catch (RemoteException e) {
            return null;
        }
    }

    public String getCdmaMsplVersion() {
        try {
            return getIHwTelephony().getCdmaMsplVersion();
        } catch (RemoteException e) {
            return null;
        }
    }

    public void printCallingAppNameInfo(boolean enable, Context context) {
        if (context != null) {
            int callingPid = Process.myPid();
            String appName = "";
            ActivityManager am = (ActivityManager) context.getSystemService(FreezeScreenScene.ACTIVITY_PARAM);
            if (am != null) {
                List<RunningAppProcessInfo> appProcessList = am.getRunningAppProcesses();
                if (appProcessList != null) {
                    for (RunningAppProcessInfo appProcess : appProcessList) {
                        if (appProcess.pid == callingPid) {
                            appName = appProcess.processName;
                        }
                    }
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setDataEnabled: calling app is( ");
                    stringBuilder.append(appName);
                    stringBuilder.append(" ) setEanble( ");
                    stringBuilder.append(enable);
                    stringBuilder.append(" )");
                    Rlog.d(str, stringBuilder.toString());
                    triggerChrAppCloseDataSwitch(appName, enable, context);
                }
            }
        }
    }

    public boolean isAppInWhiteList(String appName) {
        if (CoverManager.HALL_STATE_RECEIVER_PHONE.equals(appName) || "system".equals(appName) || "com.android.systemui".equals(appName) || "com.android.settings".equals(appName) || "com.huawei.systemmanager".equals(appName) || "com.huawei.vassistant".equals(appName) || "com.huawei.systemmanager:service".equals(appName)) {
            return true;
        }
        return false;
    }

    public void triggerChrAppCloseDataSwitch(String appName, boolean enable, Context context) {
        if (appName != null && context != null) {
            if (!isAppInWhiteList(appName)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("app");
                stringBuilder.append(appName);
                stringBuilder.append(" operate data switch! trigger Chr!");
                Rlog.d(str, stringBuilder.toString());
                Intent apkIntent = new Intent(HwTelephonyIntentsInner.INTENT_DS_APP_CLOSE_DATA_SWITCH);
                apkIntent.putExtra("appname", appName);
                context.sendBroadcast(apkIntent, CHR_BROADCAST_PERMISSION);
            }
            TelephonyManager.getDefault().setDataEnabledProperties(appName, enable);
        }
    }

    public String getUniqueDeviceId(int scope) {
        if (scope == 0) {
            try {
                if (mDeviceIdAll == null) {
                    mDeviceIdAll = getIHwTelephony().getUniqueDeviceId(0);
                }
                return mDeviceIdAll;
            } catch (RemoteException e) {
                Rlog.e(TAG, "getUniqueDeviceId RemoteException");
                return null;
            } catch (NullPointerException e2) {
                Rlog.e(TAG, "getUniqueDeviceId NullPointerException");
                return null;
            }
        } else if (scope != 1) {
            return getIHwTelephony().getUniqueDeviceId(scope);
        } else {
            if (mDeviceIdIMEI == null) {
                mDeviceIdIMEI = getIHwTelephony().getUniqueDeviceId(1);
            }
            return mDeviceIdIMEI;
        }
    }

    public boolean isLTESupported() {
        try {
            return getIHwTelephony().isLTESupported();
        } catch (RemoteException e) {
            return true;
        } catch (NullPointerException e2) {
            return true;
        }
    }

    public int getSpecCardType(int slotId) {
        try {
            return getIHwTelephony().getSpecCardType(slotId);
        } catch (RemoteException e) {
            return -1;
        }
    }

    public boolean isCardUimLocked(int slotId) {
        try {
            return getIHwTelephony().isCardUimLocked(slotId);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isRadioOn(int slot) {
        try {
            return getIHwTelephony().isRadioOn(slot);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isPlatformSupportVsim() {
        return HwVSimManager.getDefault().isPlatformSupportVsim();
    }

    public boolean hasIccCardForVSim(int slotId) {
        return HwVSimManager.getDefault().hasIccCardForVSim(slotId);
    }

    public int getSimStateForVSim(int slotIdx) {
        return HwVSimManager.getDefault().getSimStateForVSim(slotIdx);
    }

    public int getVSimSubId() {
        return HwVSimManager.getDefault().getVSimSubId();
    }

    public int enableVSim(String imsi, int cardtype, int apntype, String acqorder, String challenge) {
        return HwVSimManager.getDefault().enableVSim(1, imsi, cardtype, apntype, acqorder, challenge);
    }

    public boolean disableVSim() {
        return HwVSimManager.getDefault().disableVSim();
    }

    public int setApn(int cardtype, int apntype, String challenge) {
        return HwVSimManager.getDefault().enableVSim(2, null, cardtype, apntype, null, challenge);
    }

    public int getSimMode(int subId) {
        return HwVSimManager.getDefault().getSimMode(subId);
    }

    public void recoverSimMode() {
        HwVSimManager.getDefault().recoverSimMode();
    }

    public String getRegPlmn(int subId) {
        return HwVSimManager.getDefault().getRegPlmn(subId);
    }

    public String getTrafficData() {
        return HwVSimManager.getDefault().getTrafficData();
    }

    public Boolean clearTrafficData() {
        return HwVSimManager.getDefault().clearTrafficData();
    }

    public int getSimStateViaSysinfoEx(int subId) {
        return HwVSimManager.getDefault().getSimStateViaSysinfoEx(subId);
    }

    public int getCpserr(int subId) {
        return HwVSimManager.getDefault().getCpserr(subId);
    }

    public int scanVsimAvailableNetworks(int subId, int type) {
        return HwVSimManager.getDefault().scanVsimAvailableNetworks(subId, type);
    }

    public boolean setUserReservedSubId(int subId) {
        return HwVSimManager.getDefault().setUserReservedSubId(subId);
    }

    public int getUserReservedSubId() {
        return HwVSimManager.getDefault().getUserReservedSubId();
    }

    public String getDevSubMode(int subscription) {
        return HwVSimManager.getDefault().getDevSubMode(subscription);
    }

    public String getPreferredNetworkTypeForVSim(int subscription) {
        return HwVSimManager.getDefault().getPreferredNetworkTypeForVSim(subscription);
    }

    public int getVSimCurCardType() {
        return HwVSimManager.getDefault().getVSimCurCardType();
    }

    public int getVSimFineState() {
        return 0;
    }

    public int getVSimCachedSubId() {
        return -1;
    }

    public boolean getWaitingSwitchBalongSlot() {
        try {
            return getIHwTelephony().getWaitingSwitchBalongSlot();
        } catch (RemoteException e) {
            return false;
        }
    }

    public String getCallingAppName(Context context) {
        if (context == null) {
            return "";
        }
        if (!haveCheckedAppName) {
            int callingPid = Process.myPid();
            ActivityManager am = (ActivityManager) context.getSystemService(FreezeScreenScene.ACTIVITY_PARAM);
            if (am == null) {
                return "";
            }
            List<RunningAppProcessInfo> appProcessList = am.getRunningAppProcesses();
            if (appProcessList == null) {
                return "";
            }
            for (RunningAppProcessInfo appProcess : appProcessList) {
                if (appProcess.pid == callingPid) {
                    setCallingAppName(appProcess.processName);
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setCallingAppName : ");
                    stringBuilder.append(appProcess.processName);
                    Rlog.d(str, stringBuilder.toString());
                    break;
                }
            }
            setHaveCheckedAppName(true);
        }
        return callingAppName;
    }

    private static void setCallingAppName(String name) {
        callingAppName = name;
    }

    private static void setHaveCheckedAppName(boolean value) {
        haveCheckedAppName = value;
    }

    public boolean setISMCOEX(String ATCommand) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setISMCOEX = ");
            stringBuilder.append(ATCommand);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().setISMCOEX(ATCommand);
        } catch (RemoteException e) {
            Rlog.e(TAG, "setISMCOEX RemoteException");
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "setISMCOEX NullPointerException");
            return false;
        }
    }

    public String[] queryServiceCellBand() {
        Rlog.d(TAG, "queryServiceCellBand.");
        try {
            return getIHwTelephony().queryServiceCellBand();
        } catch (RemoteException e) {
            Rlog.e(TAG, "queryServiceCellBand RemoteException");
            return new String[0];
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "queryServiceCellBand NullPointerException");
            return new String[0];
        }
    }

    public boolean registerForRadioAvailable(IPhoneCallback callback) {
        try {
            Rlog.d(TAG, "registerForRadioAvailable");
            return getIHwTelephony().registerForRadioAvailable(callback);
        } catch (RemoteException e) {
            Rlog.e(TAG, "registerForRadioAvailable RemoteException");
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "registerForRadioAvailable NullPointerException");
            return false;
        }
    }

    public boolean unregisterForRadioAvailable(IPhoneCallback callback) {
        try {
            Rlog.d(TAG, "unregisterForRadioAvailable");
            return getIHwTelephony().unregisterForRadioAvailable(callback);
        } catch (RemoteException e) {
            Rlog.e(TAG, "unregisterForRadioAvailable RemoteException");
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "unregisterForRadioAvailable NullPointerException");
            return false;
        }
    }

    public boolean registerForRadioNotAvailable(IPhoneCallback callback) {
        try {
            Rlog.d(TAG, "registerForRadioNotAvailable");
            return getIHwTelephony().registerForRadioNotAvailable(callback);
        } catch (RemoteException e) {
            Rlog.e(TAG, "registerForRadioNotAvailable RemoteException");
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "registerForRadioNotAvailable NullPointerException");
            return false;
        }
    }

    public boolean unregisterForRadioNotAvailable(IPhoneCallback callback) {
        try {
            Rlog.d(TAG, "unregisterForRadioNotAvailable");
            return getIHwTelephony().unregisterForRadioNotAvailable(callback);
        } catch (RemoteException e) {
            Rlog.e(TAG, "unregisterForRadioNotAvailable RemoteException");
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "unregisterForRadioNotAvailable NullPointerException");
            return false;
        }
    }

    public boolean registerCommonImsaToMapconInfo(IPhoneCallback callback) {
        try {
            Rlog.d(TAG, "registerCommonImsaToMapconInfo");
            return getIHwTelephony().registerCommonImsaToMapconInfo(callback);
        } catch (RemoteException e) {
            Rlog.e(TAG, "registerCommonImsaToMapconInfo RemoteException");
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "registerCommonImsaToMapconInfo NullPointerException");
            return false;
        }
    }

    public boolean unregisterCommonImsaToMapconInfo(IPhoneCallback callback) {
        try {
            Rlog.d(TAG, "unregisterCommonImsaToMapconInfo");
            return getIHwTelephony().unregisterCommonImsaToMapconInfo(callback);
        } catch (RemoteException e) {
            Rlog.e(TAG, "unregisterCommonImsaToMapconInfo RemoteException");
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "unregisterCommonImsaToMapconInfo NullPointerException");
            return false;
        }
    }

    public boolean isRadioAvailable() {
        try {
            Rlog.d(TAG, "isRadioAvailable");
            return getIHwTelephony().isRadioAvailable();
        } catch (RemoteException e) {
            Rlog.e(TAG, "isRadioAvailable RemoteException");
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "isRadioAvailable NullPointerException");
            return false;
        }
    }

    public void setImsSwitch(boolean value) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setImsSwitch");
            stringBuilder.append(value);
            Rlog.d(str, stringBuilder.toString());
            getIHwTelephony().setImsSwitch(value);
        } catch (RemoteException e) {
            Rlog.e(TAG, "setImsSwitch RemoteException");
        }
    }

    public boolean getImsSwitch() {
        try {
            Rlog.d(TAG, "getImsSwitch");
            return getIHwTelephony().getImsSwitch();
        } catch (RemoteException e) {
            Rlog.e(TAG, "getImsSwitch RemoteException");
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "getImsSwitch NullPointerException");
            return false;
        }
    }

    public void setImsDomainConfig(int domainType) {
        try {
            Rlog.d(TAG, "setImsDomainConfig");
            getIHwTelephony().setImsDomainConfig(domainType);
        } catch (RemoteException e) {
            Rlog.e(TAG, "setImsDomainConfig RemoteException");
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "setImsDomainConfig NullPointerException");
        }
    }

    public boolean handleMapconImsaReq(byte[] Msg) {
        try {
            Rlog.d(TAG, "handleMapconImsaReq");
            return getIHwTelephony().handleMapconImsaReq(Msg);
        } catch (RemoteException e) {
            Rlog.e(TAG, "handleMapconImsaReq RemoteException");
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "handleMapconImsaReq NullPointerException");
            return false;
        }
    }

    public int getUiccAppType() {
        try {
            Rlog.d(TAG, "getUiccAppType");
            return getIHwTelephony().getUiccAppType();
        } catch (RemoteException e) {
            Rlog.e(TAG, "getUiccAppType RemoteException");
            return 0;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "getUiccAppType NullPointerException");
            return 0;
        }
    }

    public int getImsDomain() {
        try {
            Rlog.d(TAG, "getImsDomain");
            return getIHwTelephony().getImsDomain();
        } catch (RemoteException e) {
            Rlog.e(TAG, "getImsDomain RemoteException");
            return -1;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "getImsDomain NullPointerException");
            return -1;
        }
    }

    public UiccAuthResponse handleUiccAuth(int auth_type, byte[] rand, byte[] auth) {
        try {
            Rlog.d(TAG, "handleUiccAuth");
            return getIHwTelephony().handleUiccAuth(auth_type, rand, auth);
        } catch (RemoteException e) {
            Rlog.e(TAG, "handleUiccAuth RemoteException");
            return null;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "handleUiccAuth NullPointerException");
            return null;
        }
    }

    public boolean registerForPhoneEvent(int phoneId, IPhoneCallback callback, int events) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("registerForPhoneEvent, phoneId = ");
            stringBuilder.append(phoneId);
            stringBuilder.append(" events = ");
            stringBuilder.append(events);
            stringBuilder.append(" callback = ");
            stringBuilder.append(callback);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().registerForPhoneEvent(phoneId, callback, events);
        } catch (RemoteException e) {
            Rlog.e(TAG, "registerForPhoneEvent RemoteException");
            return false;
        }
    }

    public boolean unregisterForPhoneEvent(IPhoneCallback callback) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unregisterForPhoneEvent, callback = ");
            stringBuilder.append(callback);
            Rlog.d(str, stringBuilder.toString());
            getIHwTelephony().unregisterForPhoneEvent(callback);
            return true;
        } catch (RemoteException e) {
            Rlog.e(TAG, "unregisterForPhoneEvent RemoteException");
            return false;
        }
    }

    public boolean isRadioAvailable(int phoneId) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isRadioAvailable, phoneId = ");
            stringBuilder.append(phoneId);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().isRadioAvailableByPhoneId(phoneId);
        } catch (RemoteException e) {
            Rlog.e(TAG, "isRadioAvailable RemoteException");
            return false;
        }
    }

    public void setImsSwitch(int phoneId, boolean value) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setImsSwitch, phoneId = ");
            stringBuilder.append(phoneId);
            stringBuilder.append(", value = ");
            stringBuilder.append(value);
            Rlog.d(str, stringBuilder.toString());
            getIHwTelephony().setImsSwitchByPhoneId(phoneId, value);
        } catch (RemoteException e) {
            Rlog.e(TAG, "setImsSwitch RemoteException");
        }
    }

    public boolean getImsSwitch(int phoneId) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getImsSwitch, phoneId = ");
            stringBuilder.append(phoneId);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().getImsSwitchByPhoneId(phoneId);
        } catch (RemoteException e) {
            Rlog.e(TAG, "getImsSwitch RemoteException");
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "getImsSwitch NullPointerException");
            return false;
        }
    }

    public void setImsDomainConfig(int phoneId, int domainType) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setImsDomainConfig, phoneId = ");
            stringBuilder.append(phoneId);
            Rlog.d(str, stringBuilder.toString());
            getIHwTelephony().setImsDomainConfigByPhoneId(phoneId, domainType);
        } catch (RemoteException e) {
            Rlog.e(TAG, "setImsDomainConfig RemoteException");
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "setImsDomainConfig NullPointerException");
        }
    }

    public boolean handleMapconImsaReq(int phoneId, byte[] Msg) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleMapconImsaReq, phoneId = ");
            stringBuilder.append(phoneId);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().handleMapconImsaReqByPhoneId(phoneId, Msg);
        } catch (RemoteException e) {
            Rlog.e(TAG, "handleMapconImsaReq RemoteException");
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "handleMapconImsaReq NullPointerException");
            return false;
        }
    }

    public int getUiccAppType(int phoneId) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getUiccAppType, phoneId = ");
            stringBuilder.append(phoneId);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().getUiccAppTypeByPhoneId(phoneId);
        } catch (RemoteException e) {
            Rlog.e(TAG, "getUiccAppType RemoteException");
            return 0;
        }
    }

    public int getImsDomain(int phoneId) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getImsDomain, phoneId = ");
            stringBuilder.append(phoneId);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().getImsDomainByPhoneId(phoneId);
        } catch (RemoteException e) {
            Rlog.e(TAG, "getImsDomain RemoteException");
            return -1;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "getImsDomain NullPointerException");
            return -1;
        }
    }

    public UiccAuthResponse handleUiccAuth(int phoneId, int auth_type, byte[] rand, byte[] auth) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleUiccAuth, phoneId = ");
            stringBuilder.append(phoneId);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().handleUiccAuthByPhoneId(phoneId, auth_type, rand, auth);
        } catch (RemoteException e) {
            Rlog.e(TAG, "handleUiccAuth RemoteException");
            return null;
        }
    }

    public boolean cmdForECInfo(int event, int action, byte[] buf) {
        try {
            return getIHwTelephony().cmdForECInfo(event, action, buf);
        } catch (RemoteException e) {
            Rlog.e(TAG, "cmdForECInfo RemoteException");
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "cmdForECInfo NullPointerException");
            return false;
        }
    }

    public void notifyCModemStatus(int status, PhoneCallback callback) {
        try {
            Rlog.d(TAG, "notifyCModemStatus");
            getIHwTelephony().notifyCModemStatus(status, callback.mCallbackStub);
        } catch (RemoteException e) {
            Rlog.e(TAG, "notifyCModemStatus RemoteException");
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "notifyCModemStatus NullPointerException");
        }
    }

    public boolean notifyDeviceState(String device, String state, String extras) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyDeviceState, device =");
            stringBuilder.append(device);
            stringBuilder.append(", state = ");
            stringBuilder.append(state);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().notifyDeviceState(device, state, extras);
        } catch (RemoteException e) {
            Rlog.e(TAG, "notifyDeviceState RemoteException");
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "notifyDeviceState NullPointerException");
            return false;
        }
    }

    public boolean isDataConnectivityDisabled(int slotId, String tag) {
        Bundle bundle = this.mDpm.getPolicy(null, tag);
        boolean allow = false;
        if (bundle != null) {
            allow = bundle.getBoolean("value");
        }
        return true == allow && 1 == slotId;
    }

    public void notifyCellularCommParaReady(int paratype, int pathtype, Message response) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyCellularCommParaReady: paratype = ");
            stringBuilder.append(paratype);
            stringBuilder.append(", pathtype = ");
            stringBuilder.append(pathtype);
            Rlog.d(str, stringBuilder.toString());
            getIHwTelephony().notifyCellularCommParaReady(paratype, pathtype, response);
        } catch (RemoteException e) {
            Rlog.e(TAG, "notifyCellularCommParaReady RemoteException");
        }
    }

    public boolean isRoamingPushDisabled() {
        Bundle bundle = this.mDpm.getPolicy(null, DISABLE_PUSH);
        Boolean allow = Boolean.valueOf(null);
        if (bundle != null) {
            allow = Boolean.valueOf(bundle.getBoolean("value"));
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isRoamingPushDisabled: ");
            stringBuilder.append(allow);
            Rlog.d(str, stringBuilder.toString());
            return allow.booleanValue();
        }
        Rlog.d(TAG, "has not set the allow, return default false");
        return allow.booleanValue();
    }

    public boolean setPinLockEnabled(boolean enablePinLock, String password, int subId) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setPinLockEnabled, enablePinLock =");
            stringBuilder.append(enablePinLock);
            stringBuilder.append(", subId = ");
            stringBuilder.append(subId);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().setPinLockEnabled(enablePinLock, password, subId);
        } catch (RemoteException e) {
            Rlog.e(TAG, "notifyDeviceState RemoteException");
            return false;
        }
    }

    public boolean changeSimPinCode(String oldPinCode, String newPinCode, int subId) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("changeSimPinCode, subId = ");
            stringBuilder.append(subId);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().changeSimPinCode(oldPinCode, newPinCode, subId);
        } catch (RemoteException e) {
            Rlog.e(TAG, "notifyDeviceState RemoteException");
            return false;
        }
    }

    public boolean sendPseudocellCellInfo(int type, int lac, int cid, int radioTech, String plmn, int subId) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendPseudocellCellInfo, type =");
            stringBuilder.append(type);
            stringBuilder.append(", lac = ");
            stringBuilder.append(lac);
            stringBuilder.append(", cid = ");
            stringBuilder.append(cid);
            stringBuilder.append(", radioTech = ");
            stringBuilder.append(radioTech);
            stringBuilder.append(", plmn = ");
            stringBuilder.append(plmn);
            stringBuilder.append(", subId = ");
            stringBuilder.append(subId);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().sendPseudocellCellInfo(type, lac, cid, radioTech, plmn, subId);
        } catch (RemoteException ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("sendPseudocellCellInfo RemoteException:");
            stringBuilder2.append(ex.getMessage());
            Rlog.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    public boolean sendLaaCmd(int cmd, String reserved, Message response) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendLaaCmd: cmd = ");
            stringBuilder.append(cmd);
            stringBuilder.append(", reserved = ");
            stringBuilder.append(reserved);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().sendLaaCmd(cmd, reserved, response);
        } catch (RemoteException e) {
            Rlog.e(TAG, "sendLaaCmd RemoteException");
            return false;
        }
    }

    public boolean getLaaDetailedState(String reserved, Message response) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getLaaDetailedState: reserved = ");
            stringBuilder.append(reserved);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().getLaaDetailedState(reserved, response);
        } catch (RemoteException e) {
            Rlog.e(TAG, "getLaaDetailedState RemoteException");
            return false;
        }
    }

    public void registerForCallAltSrv(int subId, IPhoneCallback callback) {
        try {
            Rlog.d(TAG, "registerForCallAltSrv");
            getIHwTelephony().registerForCallAltSrv(subId, callback);
        } catch (RemoteException e) {
            Rlog.e(TAG, "registerForCallAltSrv RemoteException");
        }
    }

    public void unregisterForCallAltSrv(int subId) {
        try {
            Rlog.d(TAG, "unregisterForCallAltSrv");
            getIHwTelephony().unregisterForCallAltSrv(subId);
        } catch (RemoteException e) {
            Rlog.e(TAG, "unregisterForCallAltSrv RemoteException");
        }
    }

    public int invokeOemRilRequestRaw(int phoneId, byte[] oemReq, byte[] oemResp) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invokeOemRilRequestRaw, phoneId = ");
            stringBuilder.append(phoneId);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().invokeOemRilRequestRaw(phoneId, oemReq, oemResp);
        } catch (RemoteException e) {
            Rlog.e(TAG, "invokeOemRilRequestRaw RemoteException");
            return -1;
        }
    }

    public boolean isCspPlmnEnabled(int subId) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isCspPlmnEnabled for subId: ");
            stringBuilder.append(subId);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().isCspPlmnEnabled(subId);
        } catch (RemoteException e) {
            Rlog.e(TAG, "isCspPlmnEnabled RemoteException");
            return false;
        }
    }

    public void setCallForwardingOption(int subId, int commandInterfaceCFAction, int commandInterfaceCFReason, String dialingNumber, int timerSeconds, Message response) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setCallForwardingOption subId:");
            stringBuilder.append(subId);
            stringBuilder.append(", commandInterfaceCFAction:");
            stringBuilder.append(commandInterfaceCFAction);
            stringBuilder.append(", commandInterfaceCFReason:");
            stringBuilder.append(commandInterfaceCFReason);
            stringBuilder.append(", timerSeconds: ");
            stringBuilder.append(timerSeconds);
            Rlog.d(str, stringBuilder.toString());
            getIHwTelephony().setCallForwardingOption(subId, commandInterfaceCFAction, commandInterfaceCFReason, dialingNumber, timerSeconds, response);
        } catch (RemoteException e) {
            Rlog.e(TAG, "setCallForwardingOption RemoteException");
        }
    }

    public void getCallForwardingOption(int subId, int commandInterfaceCFReason, Message response) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCallForwardingOption subId:");
            stringBuilder.append(subId);
            stringBuilder.append(", commandInterfaceCFReason:");
            stringBuilder.append(commandInterfaceCFReason);
            Rlog.d(str, stringBuilder.toString());
            getIHwTelephony().getCallForwardingOption(subId, commandInterfaceCFReason, response);
        } catch (RemoteException e) {
            Rlog.e(TAG, "getCallForwardingOption RemoteException");
        }
    }

    public boolean setSubscription(int subId, boolean activate, Message response) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setSubscription subId:");
            stringBuilder.append(subId);
            stringBuilder.append(", activate: ");
            stringBuilder.append(activate);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().setSubscription(subId, activate, response);
        } catch (RemoteException e) {
            Rlog.e(TAG, "setSubscription RemoteException");
            return false;
        }
    }

    public String getImsImpu(int subId) {
        try {
            Rlog.d(TAG, "getImsImpu");
            return getIHwTelephony().getImsImpu(subId);
        } catch (RemoteException e) {
            Rlog.e(TAG, "getImsImpu RemoteException");
            return null;
        }
    }

    public String getLine1NumberFromImpu(int subId) {
        try {
            Rlog.d(TAG, "getLine1NumberFromImpu");
            return getIHwTelephony().getLine1NumberFromImpu(subId);
        } catch (RemoteException e) {
            Rlog.e(TAG, "getLine1NumberFromImpu RemoteException");
            return null;
        }
    }

    public boolean isSecondaryCardGsmOnly() {
        try {
            return getIHwTelephony().isSecondaryCardGsmOnly();
        } catch (RemoteException e) {
            Rlog.e(TAG, "isSecondaryCardGsmOnly RemoteException");
            return false;
        }
    }

    public boolean isVSimEnabled() {
        return HwVSimManager.getDefault().isVSimEnabled();
    }

    public boolean bindSimToProfile(int slotId) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bindSimToProfile slotId = ");
            stringBuilder.append(slotId);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().bindSimToProfile(slotId);
        } catch (RemoteException e) {
            Rlog.e(TAG, "bindSimToProfile RemoteException");
            return false;
        }
    }

    public boolean setLine1Number(int subId, String alphaTag, String number, Message onComplete) {
        try {
            return getIHwTelephony().setLine1Number(subId, alphaTag, number, onComplete);
        } catch (RemoteException e) {
            Rlog.e(TAG, "setLine1Number RemoteException");
            return false;
        }
    }

    public boolean setDeepNoDisturbState(int slotId, int state) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setDeepNoDisturbState slotId = ");
            stringBuilder.append(slotId);
            stringBuilder.append(" state = ");
            stringBuilder.append(state);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().setDeepNoDisturbState(slotId, state);
        } catch (RemoteException ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setDeepNoDisturbState RemoteException:");
            stringBuilder2.append(ex);
            Rlog.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    public void informModemTetherStatusToChangeGRO(int enable, String faceName) {
        try {
            getIHwTelephony().informModemTetherStatusToChangeGRO(enable, faceName);
        } catch (RemoteException e) {
            Rlog.e(TAG, "sendUSBinformationToRIL RemoteException");
        }
    }

    public boolean sendSimMatchedOperatorInfo(int slotId, String opKey, String opName, int state, String reserveField) {
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendSimMatchedOperatorInfo slotId = ");
            stringBuilder.append(slotId);
            stringBuilder.append(", opKey = ");
            stringBuilder.append(opKey);
            stringBuilder.append(", opName =");
            stringBuilder.append(opName);
            stringBuilder.append(", state = ");
            stringBuilder.append(state);
            stringBuilder.append(", reserveField = ");
            stringBuilder.append(reserveField);
            Rlog.d(str, stringBuilder.toString());
            return getIHwTelephony().sendSimMatchedOperatorInfo(slotId, opKey, opName, state, reserveField);
        } catch (RemoteException e) {
            Rlog.e(TAG, "sendSimMatchedOperatorInfo RemoteException");
            return false;
        }
    }

    public boolean is4RMimoEnabled(int subId) {
        try {
            return getIHwTelephony().is4RMimoEnabled(subId);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("is4RMimoEnabled RemoteException:");
            stringBuilder.append(ex);
            Rlog.e(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean getAntiFakeBaseStation(Message response) {
        try {
            Rlog.d(TAG, "getAntiFakeBaseStation");
            return getIHwTelephony().getAntiFakeBaseStation(response);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAntiFakeBaseStation RemoteException");
            stringBuilder.append(ex);
            Rlog.e(str, stringBuilder.toString());
            return false;
        }
    }

    public byte[] getCardTrayInfo() {
        try {
            return getIHwTelephony().getCardTrayInfo();
        } catch (RemoteException e) {
            Rlog.e(TAG, "getCardTrayInfo RemoteException");
            return new byte[0];
        }
    }

    public boolean registerForAntiFakeBaseStation(IPhoneCallback callback) {
        try {
            Rlog.d(TAG, "registerForAntiFakeBaseStation");
            return getIHwTelephony().registerForAntiFakeBaseStation(callback);
        } catch (RemoteException e) {
            Rlog.e(TAG, "registerForAntiFakeBaseStation RemoteException");
            return false;
        }
    }

    public boolean unregisterForAntiFakeBaseStation() {
        try {
            Rlog.d(TAG, "unregisterForAntiFakeBaseStation");
            return getIHwTelephony().unregisterForAntiFakeBaseStation();
        } catch (RemoteException e) {
            Rlog.e(TAG, "unregisterForAntiFakeBaseStation RemoteException");
            return false;
        }
    }
}
