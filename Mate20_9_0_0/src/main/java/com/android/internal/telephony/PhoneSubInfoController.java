package com.android.internal.telephony;

import android.app.AppOpsManager;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;

public class PhoneSubInfoController extends AbstractPhoneSubInfo {
    private static final boolean DBG = true;
    public static final boolean IS_QCRIL_CROSS_MAPPING = SystemProperties.getBoolean("ro.hwpp.qcril_cross_mapping", false);
    public static final int SUB1 = 0;
    public static final int SUB2 = 1;
    private static final String TAG = "PhoneSubInfoController";
    private static final boolean VDBG = false;
    private final AppOpsManager mAppOps;
    private final Context mContext;
    private final Phone[] mPhone;

    public PhoneSubInfoController(Context context, Phone[] phone) {
        this.mPhone = phone;
        if (ServiceManager.getService("iphonesubinfo") == null) {
            ServiceManager.addService("iphonesubinfo", this);
        }
        this.mContext = context;
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
    }

    public String getDeviceId(String callingPackage) {
        return getDeviceIdForPhone(SubscriptionManager.getPhoneId(getDefaultSubscription()), callingPackage);
    }

    public String getDeviceIdForPhone(int phoneId, String callingPackage) {
        phoneId = getIMEIExProcess(phoneId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            phoneId = 0;
        }
        Phone phone = this.mPhone[phoneId];
        if (phone != null) {
            try {
                if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, phone.getSubId(), callingPackage, "getDeviceId")) {
                    return null;
                }
            } catch (SecurityException phoneStateException) {
                if (!isSystemApp(callingPackage)) {
                    throw phoneStateException;
                }
            }
            return phone.getDeviceId();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDeviceIdForPhone phone ");
        stringBuilder.append(phoneId);
        stringBuilder.append(" is null");
        loge(stringBuilder.toString());
        return null;
    }

    public String getNaiForSubscriber(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getNai phone is null for Subscription:");
            stringBuilder.append(subId);
            loge(stringBuilder.toString());
            return null;
        } else if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, "getNai")) {
            return phone.getNai();
        } else {
            return null;
        }
    }

    public String getImeiForSubscriber(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            try {
                if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, "getImei")) {
                    return null;
                }
            } catch (SecurityException phoneStateException) {
                if (!isSystemApp(callingPackage)) {
                    throw phoneStateException;
                }
            }
            return phone.getImei();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDeviceId phone is null for Subscription:");
        stringBuilder.append(subId);
        loge(stringBuilder.toString());
        return null;
    }

    public ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int subId, int keyType, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCarrierInfoForImsiEncryption phone is null for Subscription:");
            stringBuilder.append(subId);
            loge(stringBuilder.toString());
            return null;
        } else if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, "getCarrierInfoForImsiEncryption")) {
            return phone.getCarrierInfoForImsiEncryption(keyType);
        } else {
            return null;
        }
    }

    public void setCarrierInfoForImsiEncryption(int subId, String callingPackage, ImsiEncryptionInfo imsiEncryptionInfo) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            enforceModifyPermission();
            phone.setCarrierInfoForImsiEncryption(imsiEncryptionInfo);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCarrierInfoForImsiEncryption phone is null for Subscription:");
        stringBuilder.append(subId);
        loge(stringBuilder.toString());
    }

    public void resetCarrierKeysForImsiEncryption(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            enforceModifyPermission();
            phone.resetCarrierKeysForImsiEncryption();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("resetCarrierKeysForImsiEncryption phone is null for Subscription:");
        stringBuilder.append(subId);
        loge(stringBuilder.toString());
    }

    public String getDeviceSvn(String callingPackage) {
        return getDeviceSvnUsingSubId(getDefaultSubscription(), callingPackage);
    }

    public String getDeviceSvnUsingSubId(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone == null) {
            loge("getDeviceSvn phone is null");
            return null;
        } else if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, "getDeviceSvn")) {
            return phone.getDeviceSvn();
        } else {
            return null;
        }
    }

    public String getSubscriberId(String callingPackage) {
        return getSubscriberIdForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getSubscriberIdForSubscriber(int subId, String callingPackage) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.TELEPHONY_GETSUBSCRIBERIDFORSUBSCRIBER);
        }
        Phone phone = getPhone(subId);
        if (phone == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getSubscriberId phone is null for Subscription:");
            stringBuilder.append(subId);
            loge(stringBuilder.toString());
            return null;
        } else if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, "getSubscriberId")) {
            return phone.getSubscriberId();
        } else {
            return null;
        }
    }

    public String getIccSerialNumber(String callingPackage) {
        return getIccSerialNumberForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getIccSerialNumberForSubscriber(int subId, String callingPackage) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.TELEPHONY_GETICCSERIALNUMBERFORSUBSCRIBER);
        }
        Phone phone = getPhone(subId);
        if (phone == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getIccSerialNumber phone is null for Subscription:");
            stringBuilder.append(subId);
            loge(stringBuilder.toString());
            return null;
        } else if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, "getIccSerialNumber")) {
            return phone.getIccSerialNumber();
        } else {
            return null;
        }
    }

    public String getLine1Number(String callingPackage) {
        return getLine1NumberForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getLine1NumberForSubscriber(int subId, String callingPackage) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.TELEPHONY_GETLINE1NUMBERFORSUBSCRIBER);
        }
        Phone phone = getPhone(subId);
        if (phone == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getLine1Number phone is null for Subscription:");
            stringBuilder.append(subId);
            loge(stringBuilder.toString());
            return null;
        } else if (TelephonyPermissions.checkCallingOrSelfReadPhoneNumber(this.mContext, subId, callingPackage, "getLine1Number")) {
            return phone.getLine1Number();
        } else {
            return null;
        }
    }

    public String getLine1AlphaTag(String callingPackage) {
        return getLine1AlphaTagForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getLine1AlphaTagForSubscriber(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getLine1AlphaTag phone is null for Subscription:");
            stringBuilder.append(subId);
            loge(stringBuilder.toString());
            return null;
        } else if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, "getLine1AlphaTag")) {
            return phone.getLine1AlphaTag();
        } else {
            return null;
        }
    }

    public String getMsisdn(String callingPackage) {
        return getMsisdnForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getMsisdnForSubscriber(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getMsisdn phone is null for Subscription:");
            stringBuilder.append(subId);
            loge(stringBuilder.toString());
            return null;
        } else if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, "getMsisdn")) {
            return phone.getMsisdn();
        } else {
            return null;
        }
    }

    public String getVoiceMailNumber(String callingPackage) {
        return getVoiceMailNumberForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getVoiceMailNumberForSubscriber(int subId, String callingPackage) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.TELEPHONY_GETVOICEMAILNUMBERFORSUBSCRIBER);
        }
        Phone phone = getPhone(subId);
        if (phone == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getVoiceMailNumber phone is null for Subscription:");
            stringBuilder.append(subId);
            loge(stringBuilder.toString());
            return null;
        } else if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, "getVoiceMailNumber")) {
            return PhoneNumberUtils.extractNetworkPortion(phone.getVoiceMailNumber());
        } else {
            return null;
        }
    }

    public String getCompleteVoiceMailNumber() {
        return getCompleteVoiceMailNumberForSubscriber(getDefaultSubscription());
    }

    public String getCompleteVoiceMailNumberForSubscriber(int subId) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.CALL_PRIVILEGED", "Requires CALL_PRIVILEGED");
            return phone.getVoiceMailNumber();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCompleteVoiceMailNumber phone is null for Subscription:");
        stringBuilder.append(subId);
        loge(stringBuilder.toString());
        return null;
    }

    public String getVoiceMailAlphaTag(String callingPackage) {
        return getVoiceMailAlphaTagForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getVoiceMailAlphaTagForSubscriber(int subId, String callingPackage) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.TELEPHONY_GETVOICEMAILALPHATAGFORSUBSCRIBER);
        }
        Phone phone = getPhone(subId);
        if (phone == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getVoiceMailAlphaTag phone is null for Subscription:");
            stringBuilder.append(subId);
            loge(stringBuilder.toString());
            return null;
        } else if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, "getVoiceMailAlphaTag")) {
            return phone.getVoiceMailAlphaTag();
        } else {
            return null;
        }
    }

    private Phone getPhone(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            phoneId = 0;
        }
        return this.mPhone[phoneId];
    }

    private void enforcePrivilegedPermissionOrCarrierPrivilege(int subId, String message) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE") != 0) {
            TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(subId, message);
        }
    }

    private void enforceModifyPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", "Requires MODIFY_PHONE_STATE");
    }

    private int getDefaultSubscription() {
        return PhoneFactory.getDefaultSubscription();
    }

    public String getIsimImpi(int subId) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
            IsimRecords isim = phone.getIsimRecords();
            if (isim != null) {
                return isim.getIsimImpi();
            }
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getIsimImpi phone is null for Subscription:");
        stringBuilder.append(subId);
        loge(stringBuilder.toString());
        return null;
    }

    public String getIsimDomain(int subId) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
            IsimRecords isim = phone.getIsimRecords();
            if (isim != null) {
                return isim.getIsimDomain();
            }
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getIsimDomain phone is null for Subscription:");
        stringBuilder.append(subId);
        loge(stringBuilder.toString());
        return null;
    }

    public String[] getIsimImpu(int subId) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
            IsimRecords isim = phone.getIsimRecords();
            if (isim != null) {
                return isim.getIsimImpu();
            }
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getIsimImpu phone is null for Subscription:");
        stringBuilder.append(subId);
        loge(stringBuilder.toString());
        return null;
    }

    public String getIsimIst(int subId) throws RemoteException {
        Phone phone = getPhone(subId);
        if (phone != null) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
            IsimRecords isim = phone.getIsimRecords();
            if (isim != null) {
                return isim.getIsimIst();
            }
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getIsimIst phone is null for Subscription:");
        stringBuilder.append(subId);
        loge(stringBuilder.toString());
        return null;
    }

    public String[] getIsimPcscf(int subId) throws RemoteException {
        Phone phone = getPhone(subId);
        if (phone != null) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
            IsimRecords isim = phone.getIsimRecords();
            if (isim != null) {
                return isim.getIsimPcscf();
            }
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getIsimPcscf phone is null for Subscription:");
        stringBuilder.append(subId);
        loge(stringBuilder.toString());
        return null;
    }

    public String getIccSimChallengeResponse(int subId, int appType, int authType, String data) throws RemoteException {
        enforcePrivilegedPermissionOrCarrierPrivilege(subId, "getIccSimChallengeResponse");
        UiccCard uiccCard = getPhone(subId).getUiccCard();
        if (uiccCard == null) {
            loge("getIccSimChallengeResponse() UiccCard is null");
            return null;
        }
        UiccCardApplication uiccApp = uiccCard.getApplicationByType(appType);
        StringBuilder stringBuilder;
        if (uiccApp == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getIccSimChallengeResponse() no app with specified type -- ");
            stringBuilder.append(appType);
            loge(stringBuilder.toString());
            return null;
        }
        if (Log.HWINFO) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getIccSimChallengeResponse() found app ");
            stringBuilder.append(uiccApp.getAid());
            stringBuilder.append(" specified type -- ");
            stringBuilder.append(appType);
            loge(stringBuilder.toString());
        }
        if (authType == 128 || authType == 129) {
            return uiccApp.getIccRecords().getIccSimChallengeResponse(authType, data);
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("getIccSimChallengeResponse() unsupported authType: ");
        stringBuilder.append(authType);
        loge(stringBuilder.toString());
        return null;
    }

    public String getGroupIdLevel1ForSubscriber(int subId, String callingPackage) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.TELEPHONY_GETGROUPIDLEVEL1FORSUBSCRIBER);
        }
        Phone phone = getPhone(subId);
        if (phone == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getGroupIdLevel1 phone is null for Subscription:");
            stringBuilder.append(subId);
            loge(stringBuilder.toString());
            return null;
        } else if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, "getGroupIdLevel1")) {
            return phone.getGroupIdLevel1();
        } else {
            return null;
        }
    }

    private boolean isSystemApp(String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            return false;
        }
        StringBuilder stringBuilder;
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            if (!(appInfo == null || (appInfo.flags & 1) == 0)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(packageName);
                stringBuilder.append(" allowed.");
                log(stringBuilder.toString());
                return true;
            }
        } catch (NameNotFoundException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(packageName);
            stringBuilder.append(" not found.");
            loge(stringBuilder.toString());
        }
        return false;
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void loge(String s) {
        Rlog.e(TAG, s);
    }

    public Phone getPhoneHw(int subId) {
        return getPhone(subId);
    }

    public Context getContextHw() {
        return this.mContext;
    }

    public int getIMEIExProcess(int subId) {
        if (!IS_QCRIL_CROSS_MAPPING && !PhoneFactory.IS_QCOM_DUAL_LTE_STACK) {
            return subId;
        }
        int mainslot = HwFrameworkFactory.getHwInnerTelephonyManager().getDefault4GSlotId();
        subId = mainslot == subId ? 0 : 1;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getIMEIExProcess after comparesubId=");
        stringBuilder.append(subId);
        stringBuilder.append(",mainslot=");
        stringBuilder.append(mainslot);
        Rlog.i(str, stringBuilder.toString());
        return subId;
    }
}
