package com.android.ims;

import android.app.PendingIntent;
import android.common.HwFrameworkFactory;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsServiceProxy;
import android.telephony.ims.ImsServiceProxy.INotifyStatusChanged;
import android.telephony.ims.ImsServiceProxyCompat;
import android.util.Log;
import com.android.ims.-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng.AnonymousClass2;
import com.android.ims.-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng.AnonymousClass3;
import com.android.ims.-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng.AnonymousClass4;
import com.android.ims.-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng.AnonymousClass5;
import com.android.ims.-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng.AnonymousClass6;
import com.android.ims.-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng.AnonymousClass7;
import com.android.ims.ImsCall.Listener;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsConfig;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsRegistrationListener.Stub;
import com.android.ims.internal.IImsServiceController;
import com.android.ims.internal.IImsUt;
import com.android.ims.internal.ImsCallSession;
import com.android.internal.telephony.ExponentialBackoff;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ImsManager {
    public static final String ACTION_IMS_INCOMING_CALL = "com.android.ims.IMS_INCOMING_CALL";
    public static final String ACTION_IMS_REGISTRATION_ERROR = "com.android.ims.REGISTRATION_ERROR";
    public static final String ACTION_IMS_SERVICE_DOWN = "com.android.ims.IMS_SERVICE_DOWN";
    public static final String ACTION_IMS_SERVICE_UP = "com.android.ims.IMS_SERVICE_UP";
    private static final long BACKOFF_INITIAL_DELAY_MS = 500;
    private static final long BACKOFF_MAX_DELAY_MS = 300000;
    private static final int BACKOFF_MULTIPLIER = 2;
    public static final int CALL_ACTIVE = 1;
    public static final int CALL_ALERTING = 4;
    public static final int CALL_DIALING = 3;
    public static final int CALL_END = 7;
    public static final int CALL_HOLD = 2;
    public static final int CALL_INCOMING = 5;
    public static final int CALL_WAITING = 6;
    private static final String DATA_ENABLED_PROP = "net.lte.ims.data.enabled";
    private static final boolean DBG = true;
    public static final String EXTRA_CALL_ID = "android:imsCallID";
    public static final String EXTRA_IS_UNKNOWN_CALL = "android:isUnknown";
    public static final String EXTRA_PHONE_ID = "android:phone_id";
    public static final String EXTRA_SERVICE_ID = "android:imsServiceId";
    public static final String EXTRA_UNKNOWN_CALL_STATE = "codeaurora.unknownCallState";
    public static final String EXTRA_USSD = "android:ussd";
    public static final String FALSE = "false";
    private static final String IMS_SERVICE = "ims";
    public static final int INCOMING_CALL_RESULT_CODE = 101;
    private static final int MAX_RECENT_DISCONNECT_REASONS = 16;
    public static final String PROPERTY_DBG_ALLOW_IMS_OFF_OVERRIDE = "persist.dbg.allow_ims_off";
    public static final int PROPERTY_DBG_ALLOW_IMS_OFF_OVERRIDE_DEFAULT = 0;
    public static final String PROPERTY_DBG_VOLTE_AVAIL_OVERRIDE = "persist.dbg.volte_avail_ovr";
    public static final int PROPERTY_DBG_VOLTE_AVAIL_OVERRIDE_DEFAULT = 0;
    public static final String PROPERTY_DBG_VT_AVAIL_OVERRIDE = "persist.dbg.vt_avail_ovr";
    public static final int PROPERTY_DBG_VT_AVAIL_OVERRIDE_DEFAULT = 0;
    public static final String PROPERTY_DBG_WFC_AVAIL_OVERRIDE = "persist.dbg.wfc_avail_ovr";
    public static final int PROPERTY_DBG_WFC_AVAIL_OVERRIDE_DEFAULT = 0;
    public static final String PROP_VILTE_ENABLE = "ro.config.hw_vtlte_on";
    public static final String PROP_VOLTE_ENABLE = "ro.config.hw_volte_on";
    public static final String PROP_VOWIFI_ENABLE = "ro.config.hw_vowifi";
    private static final String TAG = "ImsManager";
    public static final String TRUE = "true";
    private static final String VOLTE_PROVISIONED_PROP = "net.lte.ims.volte.provisioned";
    private static final int VOWIFI_PREFER_INVALID = 3;
    private static final String VT_PROVISIONED_PROP = "net.lte.ims.vt.provisioned";
    private static final String WFC_PROVISIONED_PROP = "net.lte.ims.wfc.provisioned";
    private static final boolean isATT;
    private static HashMap<Integer, ImsManager> sImsManagerInstances = new HashMap();
    private static int userSelectWfcMode = 3;
    private ImsConfig mConfig = null;
    private final boolean mConfigDynamicBind;
    private CarrierConfigManager mConfigManager;
    private boolean mConfigUpdated = false;
    private Context mContext;
    private ImsServiceDeathRecipient mDeathRecipient = new ImsServiceDeathRecipient();
    private ImsEcbm mEcbm = null;
    private boolean mHasRegisteredForProxy = false;
    private final Object mHasRegisteredLock = new Object();
    private ImsConfigListener mImsConfigListener;
    private ImsServiceProxyCompat mImsServiceProxy = null;
    private ImsMultiEndpoint mMultiEndpoint = null;
    private int mPhoneId;
    private ExponentialBackoff mProvisionBackoff;
    private ConcurrentLinkedDeque<ImsReasonInfo> mRecentDisconnectReasons = new ConcurrentLinkedDeque();
    private final ImsRegistrationListenerProxy mRegistrationListenerProxy = new ImsRegistrationListenerProxy();
    private final Set<ImsConnectionStateListener> mRegistrationListeners = new HashSet();
    private Set<INotifyStatusChanged> mStatusCallbacks = new HashSet();
    private ImsUt mUt = null;

    private class AsyncUpdateProvisionedValues extends AsyncTask<Void, Void, Boolean> {
        private AsyncUpdateProvisionedValues() {
        }

        protected Boolean doInBackground(Void... params) {
            ImsManager.this.setVolteProvisionedProperty(false);
            ImsManager.this.setWfcProvisionedProperty(false);
            ImsManager.this.setVtProvisionedProperty(false);
            try {
                ImsConfig config = ImsManager.this.getConfigInterface();
                if (config != null) {
                    ImsManager.this.setVolteProvisionedProperty(getProvisionedBool(config, 10));
                    Rlog.d(ImsManager.TAG, "isVoLteProvisioned = " + ImsManager.this.isVolteProvisioned());
                    ImsManager.this.setWfcProvisionedProperty(getProvisionedBool(config, 28));
                    Rlog.d(ImsManager.TAG, "isWfcProvisioned = " + ImsManager.this.isWfcProvisioned());
                    ImsManager.this.setVtProvisionedProperty(getProvisionedBool(config, 11));
                    Rlog.d(ImsManager.TAG, "isVtProvisioned = " + ImsManager.this.isVtProvisioned());
                }
                return Boolean.valueOf(ImsManager.DBG);
            } catch (ImsException ie) {
                Rlog.e(ImsManager.TAG, "AsyncUpdateProvisionedValues error: ", ie);
                return Boolean.valueOf(false);
            }
        }

        protected void onPostExecute(Boolean completed) {
            if (ImsManager.this.mProvisionBackoff != null) {
                if (completed.booleanValue()) {
                    ImsManager.this.mProvisionBackoff.stop();
                } else {
                    ImsManager.this.mProvisionBackoff.notifyFailed();
                }
            }
        }

        private boolean getProvisionedBool(ImsConfig config, int item) throws ImsException {
            if (config.getProvisionedValue(item) == -1) {
                throw new ImsException("getProvisionedBool failed with error for item: " + item, 103);
            } else if (config.getProvisionedValue(item) == 1) {
                return ImsManager.DBG;
            } else {
                return false;
            }
        }
    }

    private class ImsRegistrationListenerBase extends Stub {
        private ImsRegistrationListenerBase() {
        }

        public void registrationConnected() throws RemoteException {
        }

        public void registrationProgressing() throws RemoteException {
        }

        public void registrationConnectedWithRadioTech(int imsRadioTech) throws RemoteException {
        }

        public void registrationProgressingWithRadioTech(int imsRadioTech) throws RemoteException {
        }

        public void registrationDisconnected(ImsReasonInfo imsReasonInfo) throws RemoteException {
        }

        public void registrationResumed() throws RemoteException {
        }

        public void registrationSuspended() throws RemoteException {
        }

        public void registrationServiceCapabilityChanged(int serviceClass, int event) throws RemoteException {
        }

        public void registrationFeatureCapabilityChanged(int serviceClass, int[] enabledFeatures, int[] disabledFeatures) throws RemoteException {
        }

        public void voiceMessageCountUpdate(int count) throws RemoteException {
        }

        public void registrationAssociatedUriChanged(Uri[] uris) throws RemoteException {
        }

        public void registrationChangeFailed(int targetAccessTech, ImsReasonInfo imsReasonInfo) throws RemoteException {
        }
    }

    private class ImsRegistrationListenerProxy extends Stub {
        private ImsRegistrationListenerProxy() {
        }

        @Deprecated
        public void registrationConnected() {
            ImsManager.log("registrationConnected ::");
            synchronized (ImsManager.this.mRegistrationListeners) {
                ImsManager.this.mRegistrationListeners.forEach(-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng.$INST$0);
            }
        }

        @Deprecated
        public void registrationProgressing() {
            ImsManager.log("registrationProgressing ::");
            synchronized (ImsManager.this.mRegistrationListeners) {
                ImsManager.this.mRegistrationListeners.forEach(-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng.$INST$1);
            }
        }

        public void registrationConnectedWithRadioTech(int imsRadioTech) {
            ImsManager.log("registrationConnectedWithRadioTech :: imsRadioTech=" + imsRadioTech);
            synchronized (ImsManager.this.mRegistrationListeners) {
                ImsManager.this.mRegistrationListeners.forEach(new AnonymousClass4((byte) 0, imsRadioTech));
            }
        }

        public void registrationProgressingWithRadioTech(int imsRadioTech) {
            ImsManager.log("registrationProgressingWithRadioTech :: imsRadioTech=" + imsRadioTech);
            synchronized (ImsManager.this.mRegistrationListeners) {
                ImsManager.this.mRegistrationListeners.forEach(new AnonymousClass4((byte) 1, imsRadioTech));
            }
        }

        public void registrationDisconnected(ImsReasonInfo imsReasonInfo) {
            ImsManager.log("registrationDisconnected :: imsReasonInfo" + imsReasonInfo);
            ImsManager.this.addToRecentDisconnectReasons(imsReasonInfo);
            synchronized (ImsManager.this.mRegistrationListeners) {
                ImsManager.this.mRegistrationListeners.forEach(new AnonymousClass3((byte) 1, imsReasonInfo));
            }
        }

        public void registrationResumed() {
            ImsManager.log("registrationResumed ::");
            synchronized (ImsManager.this.mRegistrationListeners) {
                ImsManager.this.mRegistrationListeners.forEach(-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng.$INST$2);
            }
        }

        public void registrationSuspended() {
            ImsManager.log("registrationSuspended ::");
            synchronized (ImsManager.this.mRegistrationListeners) {
                ImsManager.this.mRegistrationListeners.forEach(-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng.$INST$4);
            }
        }

        public void registrationServiceCapabilityChanged(int serviceClass, int event) {
            ImsManager.log("registrationServiceCapabilityChanged :: serviceClass=" + serviceClass + ", event=" + event);
            synchronized (ImsManager.this.mRegistrationListeners) {
                ImsManager.this.mRegistrationListeners.forEach(-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng.$INST$3);
            }
        }

        public void registrationFeatureCapabilityChanged(int serviceClass, int[] enabledFeatures, int[] disabledFeatures) {
            ImsManager.log("registrationFeatureCapabilityChanged :: serviceClass=" + serviceClass);
            synchronized (ImsManager.this.mRegistrationListeners) {
                ImsManager.this.mRegistrationListeners.forEach(new AnonymousClass7(serviceClass, enabledFeatures, disabledFeatures));
            }
        }

        public void voiceMessageCountUpdate(int count) {
            ImsManager.log("voiceMessageCountUpdate :: count=" + count);
            synchronized (ImsManager.this.mRegistrationListeners) {
                ImsManager.this.mRegistrationListeners.forEach(new AnonymousClass4((byte) 2, count));
            }
        }

        public void registrationAssociatedUriChanged(Uri[] uris) {
            ImsManager.log("registrationAssociatedUriChanged ::");
            synchronized (ImsManager.this.mRegistrationListeners) {
                ImsManager.this.mRegistrationListeners.forEach(new AnonymousClass3((byte) 0, uris));
            }
        }

        public void registrationChangeFailed(int targetAccessTech, ImsReasonInfo imsReasonInfo) {
            ImsManager.log("registrationChangeFailed :: targetAccessTech=" + targetAccessTech + ", imsReasonInfo=" + imsReasonInfo);
            synchronized (ImsManager.this.mRegistrationListeners) {
                ImsManager.this.mRegistrationListeners.forEach(new AnonymousClass6(targetAccessTech, imsReasonInfo));
            }
        }
    }

    private class ImsServiceDeathRecipient implements DeathRecipient {
        private ImsServiceDeathRecipient() {
        }

        public void binderDied() {
            ImsManager.this.mImsServiceProxy = null;
            ImsManager.this.mUt = null;
            ImsManager.this.mConfig = null;
            ImsManager.this.mEcbm = null;
            ImsManager.this.mMultiEndpoint = null;
        }
    }

    static {
        boolean equals;
        if ("07".equals(SystemProperties.get("ro.config.hw_opta"))) {
            equals = "840".equals(SystemProperties.get("ro.config.hw_optb"));
        } else {
            equals = false;
        }
        isATT = equals;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static ImsManager getInstance(Context context, int phoneId) {
        synchronized (sImsManagerInstances) {
            if (sImsManagerInstances.containsKey(Integer.valueOf(phoneId))) {
                ImsManager m = (ImsManager) sImsManagerInstances.get(Integer.valueOf(phoneId));
                if (m != null) {
                    m.connectIfServiceIsAvailable();
                }
            } else {
                ImsManager mgr = new ImsManager(context, phoneId);
                sImsManagerInstances.put(Integer.valueOf(phoneId), mgr);
                return mgr;
            }
        }
    }

    public static boolean isEnhanced4gLteModeSettingEnabledByUser(Context context) {
        boolean z = DBG;
        if (!getBooleanCarrierConfig(context, "editable_enhanced_4g_lte_bool")) {
            return DBG;
        }
        int enabled = Global.getInt(context.getContentResolver(), "volte_vt_enabled", 1);
        log("isEnhanced4gLteModeSettingEnabledByUser result -> " + enabled);
        if (enabled != 1) {
            z = false;
        }
        return z;
    }

    public boolean isEnhanced4gLteModeSettingEnabledByUserForSlot() {
        boolean z = DBG;
        if (!getBooleanCarrierConfigForSlot("editable_enhanced_4g_lte_bool")) {
            return DBG;
        }
        if (Global.getInt(this.mContext.getContentResolver(), "volte_vt_enabled", 1) != 1) {
            z = false;
        }
        return z;
    }

    public static void setEnhanced4gLteModeSetting(Context context, boolean enabled) {
        int value = enabled ? 1 : 0;
        log("setEnhanced4gLteModeSetting value : " + value);
        Global.putInt(context.getContentResolver(), "volte_vt_enabled", value);
        if (isNonTtyOrTtyOnVolteEnabled(context)) {
            int default4GSlotId = HwFrameworkFactory.getHwInnerTelephonyManager().getDefault4GSlotId();
            log("setEnhanced4gLteModeSetting  default4GSlotId is " + default4GSlotId);
            ImsManager imsManager = getInstance(context, SubscriptionManager.getPhoneId(default4GSlotId));
            if (imsManager != null) {
                try {
                    imsManager.setAdvanced4GMode(enabled);
                } catch (ImsException e) {
                }
            }
        }
    }

    public void setEnhanced4gLteModeSettingForSlot(boolean enabled) {
        int value = (!getBooleanCarrierConfigForSlot("editable_enhanced_4g_lte_bool") || enabled) ? 1 : 0;
        try {
            if (Global.getInt(this.mContext.getContentResolver(), "volte_vt_enabled") == value) {
                return;
            }
        } catch (SettingNotFoundException e) {
        }
        Global.putInt(this.mContext.getContentResolver(), "volte_vt_enabled", value);
        if (isNonTtyOrTtyOnVolteEnabledForSlot()) {
            try {
                setAdvanced4GMode(enabled);
            } catch (ImsException e2) {
            }
        }
    }

    public static boolean isNonTtyOrTtyOnVolteEnabled(Context context) {
        boolean z = DBG;
        if (getBooleanCarrierConfig(context, "carrier_volte_tty_supported_bool")) {
            return DBG;
        }
        TelecomManager tm = (TelecomManager) context.getSystemService("telecom");
        if (tm == null) {
            Log.w(TAG, "isNonTtyOrTtyOnVolteEnabled: telecom not available");
            return DBG;
        }
        if (tm.getCurrentTtyMode() != 0) {
            z = false;
        }
        return z;
    }

    public boolean isNonTtyOrTtyOnVolteEnabledForSlot() {
        boolean z = DBG;
        if (getBooleanCarrierConfigForSlot("carrier_volte_tty_supported_bool")) {
            return DBG;
        }
        TelecomManager tm = (TelecomManager) this.mContext.getSystemService("telecom");
        if (tm == null) {
            Log.w(TAG, "isNonTtyOrTtyOnVolteEnabledForSlot: telecom not available");
            return DBG;
        }
        if (tm.getCurrentTtyMode() != 0) {
            z = false;
        }
        return z;
    }

    public static boolean isVolteEnabledByPlatform(Context context) {
        if (SystemProperties.getInt(PROPERTY_DBG_VOLTE_AVAIL_OVERRIDE, 0) == 1) {
            return DBG;
        }
        if (!SystemProperties.getBoolean(PROP_VOLTE_ENABLE, false)) {
            return false;
        }
        boolean isGbaValid;
        if (context.getResources().getBoolean(17956922) && getBooleanCarrierConfig(context, "carrier_volte_available_bool")) {
            isGbaValid = isGbaValid(context);
        } else {
            isGbaValid = false;
        }
        log("isVolteEnabledByPlatform result -> " + isGbaValid);
        return isGbaValid;
    }

    public boolean isVolteEnabledByPlatformForSlot() {
        boolean z = false;
        if (SystemProperties.getInt(PROPERTY_DBG_VOLTE_AVAIL_OVERRIDE, 0) == 1) {
            return DBG;
        }
        if (this.mContext.getResources().getBoolean(17956922) && getBooleanCarrierConfigForSlot("carrier_volte_available_bool")) {
            z = isGbaValidForSlot();
        }
        return z;
    }

    public static boolean isVolteProvisionedOnDevice(Context context) {
        if (getBooleanCarrierConfig(context, "carrier_volte_provisioning_required_bool")) {
            ImsManager mgr = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
            if (mgr != null) {
                return mgr.isVolteProvisioned();
            }
        }
        return DBG;
    }

    public boolean isVolteProvisionedOnDeviceForSlot() {
        if (getBooleanCarrierConfigForSlot("carrier_volte_provisioning_required_bool")) {
            return isVolteProvisioned();
        }
        return DBG;
    }

    public static boolean isWfcProvisionedOnDevice(Context context) {
        if (getBooleanCarrierConfig(context, "carrier_volte_override_wfc_provisioning_bool") && !isVolteProvisionedOnDevice(context)) {
            return false;
        }
        if (getBooleanCarrierConfig(context, "carrier_volte_provisioning_required_bool")) {
            ImsManager mgr = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
            if (mgr != null) {
                return mgr.isWfcProvisioned();
            }
        }
        return DBG;
    }

    public boolean isWfcProvisionedOnDeviceForSlot() {
        if (getBooleanCarrierConfigForSlot("carrier_volte_override_wfc_provisioning_bool") && !isVolteProvisionedOnDeviceForSlot()) {
            return false;
        }
        if (getBooleanCarrierConfigForSlot("carrier_volte_provisioning_required_bool")) {
            return isWfcProvisioned();
        }
        return DBG;
    }

    public static boolean isVtProvisionedOnDevice(Context context) {
        if (getBooleanCarrierConfig(context, "carrier_volte_provisioning_required_bool")) {
            ImsManager mgr = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
            if (mgr != null) {
                return mgr.isVtProvisioned();
            }
        }
        return DBG;
    }

    public boolean isVtProvisionedOnDeviceForSlot() {
        if (getBooleanCarrierConfigForSlot("carrier_volte_provisioning_required_bool")) {
            return isVtProvisioned();
        }
        return DBG;
    }

    public static boolean isVtEnabledByPlatform(Context context) {
        if (SystemProperties.getInt(PROPERTY_DBG_VT_AVAIL_OVERRIDE, 0) == 1) {
            return DBG;
        }
        if (SystemProperties.getBoolean(PROP_VILTE_ENABLE, false)) {
            boolean isGbaValid;
            if (context.getResources().getBoolean(17956923) && getBooleanCarrierConfig(context, "carrier_vt_available_bool")) {
                isGbaValid = isGbaValid(context);
            } else {
                isGbaValid = false;
            }
            boolean isVoWiFiRegstered = ((TelephonyManager) context.getSystemService("phone")).isWifiCallingAvailable();
            log("isVtEnabledByPlatform isVoWiFiRegstered is  " + isVoWiFiRegstered);
            if (isGbaValid && isVoWiFiRegstered) {
                isGbaValid &= getBooleanCarrierConfig(context, "carrier_viwifi_available_bool");
            }
            return isGbaValid;
        }
        log("isVtEnabledByPlatform ro.config.hw_vtlte_on is false");
        return false;
    }

    public boolean isVtEnabledByPlatformForSlot() {
        boolean z = false;
        if (SystemProperties.getInt(PROPERTY_DBG_VT_AVAIL_OVERRIDE, 0) == 1) {
            return DBG;
        }
        if (this.mContext.getResources().getBoolean(17956923) && getBooleanCarrierConfigForSlot("carrier_vt_available_bool")) {
            z = isGbaValidForSlot();
        }
        return z;
    }

    public static boolean isVtEnabledByUser(Context context) {
        if (Global.getInt(context.getContentResolver(), "vt_ims_enabled", 1) == 1) {
            return DBG;
        }
        return false;
    }

    public boolean isVtEnabledByUserForSlot() {
        if (Global.getInt(this.mContext.getContentResolver(), "vt_ims_enabled", 1) == 1) {
            return DBG;
        }
        return false;
    }

    public static void setVtSetting(Context context, boolean enabled) {
        int i = 1;
        Global.putInt(context.getContentResolver(), "vt_ims_enabled", enabled ? 1 : 0);
        ImsManager imsManager = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        if (imsManager != null) {
            try {
                ImsConfig config = imsManager.getConfigInterface();
                if (!enabled) {
                    i = 0;
                }
                config.setFeatureValue(1, 13, i, imsManager.mImsConfigListener);
                if (enabled) {
                    log("setVtSetting() : turnOnIms");
                    imsManager.turnOnIms();
                } else if (!isTurnOffImsAllowedByPlatform(context)) {
                } else {
                    if (!isVolteEnabledByPlatform(context) || (isEnhanced4gLteModeSettingEnabledByUser(context) ^ 1) != 0) {
                        log("setVtSetting() : imsServiceAllowTurnOff -> turnOffIms");
                        imsManager.turnOffIms();
                    }
                }
            } catch (ImsException e) {
                loge("setVtSetting(): ", e);
            }
        }
    }

    public void setVtSettingForSlot(boolean enabled) {
        int i = 1;
        Global.putInt(this.mContext.getContentResolver(), "vt_ims_enabled", enabled ? 1 : 0);
        try {
            ImsConfig config = getConfigInterface();
            if (!enabled) {
                i = 0;
            }
            config.setFeatureValue(1, 13, i, this.mImsConfigListener);
            if (enabled) {
                log("setVtSettingForSlot() : turnOnIms");
                turnOnIms();
            } else if (!isVolteEnabledByPlatformForSlot()) {
            } else {
                if (!isVolteEnabledByPlatformForSlot() || (isEnhanced4gLteModeSettingEnabledByUserForSlot() ^ 1) != 0) {
                    log("setVtSettingForSlot() : imsServiceAllowTurnOff -> turnOffIms");
                    turnOffIms();
                }
            }
        } catch (ImsException e) {
            loge("setVtSettingForSlot(): ", e);
        }
    }

    private static boolean isTurnOffImsAllowedByPlatform(Context context) {
        if (SystemProperties.getInt(PROPERTY_DBG_ALLOW_IMS_OFF_OVERRIDE, 0) == 1) {
            return DBG;
        }
        return getBooleanCarrierConfig(context, "carrier_allow_turnoff_ims_bool");
    }

    private boolean isTurnOffImsAllowedByPlatformForSlot() {
        if (SystemProperties.getInt(PROPERTY_DBG_ALLOW_IMS_OFF_OVERRIDE, 0) == 1) {
            return DBG;
        }
        return getBooleanCarrierConfigForSlot("carrier_allow_turnoff_ims_bool");
    }

    public static boolean isWfcEnabledByUser(Context context) {
        int i;
        ContentResolver contentResolver = context.getContentResolver();
        String str = "wfc_ims_enabled";
        if (getBooleanCarrierConfig(context, "carrier_default_wfc_ims_enabled_bool")) {
            i = 1;
        } else {
            i = 0;
        }
        int enabled = Global.getInt(contentResolver, str, i);
        log("isWfcEnabledByUser result -> " + enabled);
        if (enabled == 1) {
            return DBG;
        }
        return false;
    }

    public boolean isWfcEnabledByUserForSlot() {
        int i;
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String str = "wfc_ims_enabled";
        if (getBooleanCarrierConfigForSlot("carrier_default_wfc_ims_enabled_bool")) {
            i = 1;
        } else {
            i = 0;
        }
        if (Global.getInt(contentResolver, str, i) == 1) {
            return DBG;
        }
        return false;
    }

    public static void setWfcSetting(Context context, boolean enabled) {
        int i = 1;
        int i2 = 0;
        Global.putInt(context.getContentResolver(), "wfc_ims_enabled", enabled ? 1 : 0);
        ImsManager imsManager = getInstance(context, HwFrameworkFactory.getHwInnerTelephonyManager().getDefault4GSlotId());
        if (imsManager != null) {
            try {
                boolean isNetworkRoaming;
                ImsConfig config = imsManager.getConfigInterface();
                TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
                if (tm != null) {
                    isNetworkRoaming = tm.isNetworkRoaming();
                } else {
                    isNetworkRoaming = false;
                }
                Boolean isRoaming = Boolean.valueOf(isNetworkRoaming);
                Boolean isVowifiEnable = Boolean.valueOf(isWfcEnabledByPlatform(context));
                if (isVowifiEnable.booleanValue() && 3 == userSelectWfcMode) {
                    userSelectWfcMode = getWfcMode(context, isRoaming.booleanValue());
                }
                if (enabled) {
                    i2 = 1;
                }
                config.setFeatureValue(2, 18, i2, imsManager.mImsConfigListener);
                if (enabled) {
                    if (isVowifiEnable.booleanValue()) {
                        log("isVowifiEnable = true, setWfcModeInternal - setting = " + userSelectWfcMode);
                        setWfcModeInternal(context, userSelectWfcMode);
                    }
                    log("setWfcSetting() : turnOnIms");
                    imsManager.turnOnIms();
                } else if (isTurnOffImsAllowedByPlatform(context) && !(isVolteEnabledByPlatform(context) && (isEnhanced4gLteModeSettingEnabledByUser(context) ^ 1) == 0)) {
                    log("setWfcSetting() : imsServiceAllowTurnOff -> turnOffIms");
                    imsManager.turnOffIms();
                }
                if (enabled) {
                    i = getWfcMode(context, isRoaming.booleanValue());
                }
                setWfcModeInternal(context, i);
            } catch (ImsException e) {
                loge("setWfcSetting(): ", e);
            }
        }
    }

    public void setWfcSettingForSlot(boolean enabled) {
        Global.putInt(this.mContext.getContentResolver(), "wfc_ims_enabled", enabled ? 1 : 0);
        setWfcNonPersistentForSlot(enabled, getWfcModeForSlot());
    }

    public void setWfcNonPersistentForSlot(boolean enabled, int wfcMode) {
        int imsFeatureValue = enabled ? 1 : 0;
        int imsWfcModeFeatureValue = enabled ? wfcMode : 1;
        try {
            getConfigInterface().setFeatureValue(2, 18, imsFeatureValue, this.mImsConfigListener);
            if (enabled) {
                log("setWfcSettingForSlot() : turnOnIms");
                turnOnIms();
            } else if (isTurnOffImsAllowedByPlatformForSlot() && !(isVolteEnabledByPlatformForSlot() && (isEnhanced4gLteModeSettingEnabledByUserForSlot() ^ 1) == 0)) {
                log("setWfcSettingForSlot() : imsServiceAllowTurnOff -> turnOffIms");
                turnOffIms();
            }
            setWfcModeInternalForSlot(imsWfcModeFeatureValue);
        } catch (ImsException e) {
            loge("setWfcSettingForSlot(): ", e);
        }
    }

    public static int getWfcMode(Context context) {
        int setting = Global.getInt(context.getContentResolver(), "wfc_ims_mode", getIntCarrierConfig(context, "carrier_default_wfc_ims_mode_int"));
        log("getWfcMode - setting=" + setting);
        return setting;
    }

    public int getWfcModeForSlot() {
        int setting = Global.getInt(this.mContext.getContentResolver(), "wfc_ims_mode", getIntCarrierConfigForSlot("carrier_default_wfc_ims_mode_int"));
        log("getWfcMode - setting=" + setting);
        return setting;
    }

    public static void setWfcMode(Context context, int wfcMode) {
        log("setWfcMode - setting=" + wfcMode);
        Global.putInt(context.getContentResolver(), "wfc_ims_mode", wfcMode);
        if (Boolean.valueOf(isWfcEnabledByPlatform(context)).booleanValue()) {
            userSelectWfcMode = wfcMode;
        }
        setWfcModeInternal(context, wfcMode);
    }

    public void setWfcModeForSlot(int wfcMode) {
        log("setWfcModeForSlot - setting=" + wfcMode);
        Global.putInt(this.mContext.getContentResolver(), "wfc_ims_mode", wfcMode);
        setWfcModeInternalForSlot(wfcMode);
    }

    public static int getWfcMode(Context context, boolean roaming) {
        if (checkCarrierConfigKeyExist(context, "carrier_default_wfc_ims_roaming_mode_int").booleanValue() && (roaming ^ 1) == 0) {
            int setting = Global.getInt(context.getContentResolver(), "wfc_ims_roaming_mode", getIntCarrierConfig(context, "carrier_default_wfc_ims_roaming_mode_int"));
            log("getWfcMode (roaming) - setting=" + setting);
            return setting;
        }
        setting = Global.getInt(context.getContentResolver(), "wfc_ims_mode", getIntCarrierConfig(context, "carrier_default_wfc_ims_mode_int"));
        log("getWfcMode - setting=" + setting);
        return setting;
    }

    public int getWfcModeForSlot(boolean roaming) {
        if (roaming) {
            int setting = Global.getInt(this.mContext.getContentResolver(), "wfc_ims_roaming_mode", getIntCarrierConfigForSlot("carrier_default_wfc_ims_roaming_mode_int"));
            log("getWfcModeForSlot (roaming) - setting=" + setting);
            return setting;
        }
        setting = Global.getInt(this.mContext.getContentResolver(), "wfc_ims_mode", getIntCarrierConfigForSlot("carrier_default_wfc_ims_mode_int"));
        log("getWfcModeForSlot - setting=" + setting);
        return setting;
    }

    public static void setWfcMode(Context context, int wfcMode, boolean roaming) {
        if (isWfcEnabledByPlatform(context)) {
            boolean hasCust = checkCarrierConfigKeyExist(context, "carrier_default_wfc_ims_roaming_mode_int").booleanValue();
            if (hasCust && (roaming ^ 1) == 0) {
                log("setWfcMode (roaming) - setting=" + wfcMode);
                Global.putInt(context.getContentResolver(), "wfc_ims_roaming_mode", wfcMode);
            } else {
                log("setWfcMode - setting=" + wfcMode);
                Global.putInt(context.getContentResolver(), "wfc_ims_mode", wfcMode);
            }
            userSelectWfcMode = wfcMode;
            TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
            if (tm == null || roaming != tm.isNetworkRoaming()) {
                if ((hasCust ^ 1) == 0) {
                    return;
                }
            }
            setWfcModeInternal(context, wfcMode);
        }
    }

    public void setWfcModeForSlot(int wfcMode, boolean roaming) {
        if (roaming) {
            log("setWfcModeForSlot (roaming) - setting=" + wfcMode);
            Global.putInt(this.mContext.getContentResolver(), "wfc_ims_roaming_mode", wfcMode);
        } else {
            log("setWfcModeForSlot - setting=" + wfcMode);
            Global.putInt(this.mContext.getContentResolver(), "wfc_ims_mode", wfcMode);
        }
        int[] subIds = SubscriptionManager.getSubId(this.mPhoneId);
        int subId = -1;
        if (subIds != null && subIds.length >= 1) {
            subId = subIds[0];
        }
        if (roaming == ((TelephonyManager) this.mContext.getSystemService("phone")).isNetworkRoaming(subId)) {
            setWfcModeInternalForSlot(wfcMode);
        }
    }

    private static void setWfcModeInternal(Context context, final int wfcMode) {
        final ImsManager imsManager = getInstance(context, HwFrameworkFactory.getHwInnerTelephonyManager().getDefault4GSlotId());
        if (imsManager != null) {
            int value = wfcMode;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        imsManager.getConfigInterface().setProvisionedValue(27, wfcMode);
                    } catch (ImsException e) {
                    }
                }
            }).start();
        }
    }

    private void setWfcModeInternalForSlot(int wfcMode) {
        int value = wfcMode;
        new Thread(new AnonymousClass5((byte) 0, wfcMode, this)).start();
    }

    /* synthetic */ void lambda$-com_android_ims_ImsManager_46661(int value) {
        try {
            getConfigInterface().setProvisionedValue(27, value);
        } catch (ImsException e) {
        }
    }

    public static boolean isWfcRoamingEnabledByUser(Context context) {
        int i;
        ContentResolver contentResolver = context.getContentResolver();
        String str = "wfc_ims_roaming_enabled";
        if (getBooleanCarrierConfig(context, "carrier_default_wfc_ims_roaming_enabled_bool")) {
            i = 1;
        } else {
            i = 0;
        }
        if (Global.getInt(contentResolver, str, i) == 1) {
            return DBG;
        }
        return false;
    }

    public boolean isWfcRoamingEnabledByUserForSlot() {
        int i;
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String str = "wfc_ims_roaming_enabled";
        if (getBooleanCarrierConfigForSlot("carrier_default_wfc_ims_roaming_enabled_bool")) {
            i = 1;
        } else {
            i = 0;
        }
        if (Global.getInt(contentResolver, str, i) == 1) {
            return DBG;
        }
        return false;
    }

    public static void setWfcRoamingSetting(Context context, boolean enabled) {
        int i;
        ContentResolver contentResolver = context.getContentResolver();
        String str = "wfc_ims_roaming_enabled";
        if (enabled) {
            i = 1;
        } else {
            i = 0;
        }
        Global.putInt(contentResolver, str, i);
        ImsManager imsManager = getInstance(context, HwFrameworkFactory.getHwInnerTelephonyManager().getDefault4GSlotId());
        if (imsManager != null) {
            imsManager.setWfcRoamingSettingInternal(enabled);
        }
    }

    public void setWfcRoamingSettingForSlot(boolean enabled) {
        int i;
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String str = "wfc_ims_roaming_enabled";
        if (enabled) {
            i = 1;
        } else {
            i = 0;
        }
        Global.putInt(contentResolver, str, i);
        setWfcRoamingSettingInternal(enabled);
    }

    private void setWfcRoamingSettingInternal(boolean enabled) {
        int value;
        if (enabled) {
            value = 1;
        } else {
            value = 0;
        }
        new Thread(new AnonymousClass5((byte) 1, value, this)).start();
    }

    /* synthetic */ void lambda$-com_android_ims_ImsManager_49736(int value) {
        try {
            getConfigInterface().setProvisionedValue(26, value);
        } catch (ImsException e) {
        }
    }

    public static boolean isWfcEnabledByPlatform(Context context) {
        if (SystemProperties.getInt(PROPERTY_DBG_WFC_AVAIL_OVERRIDE, 0) == 1) {
            return DBG;
        }
        if (!SystemProperties.getBoolean(PROP_VOWIFI_ENABLE, false)) {
            return false;
        }
        boolean isGbaValid;
        boolean result1 = context.getResources().getBoolean(17956924);
        boolean result2 = getBooleanCarrierConfig(context, "carrier_wfc_ims_available_bool");
        log("Vowifi sim adp : Device =" + result1 + " XML_CarrierConfig =" + result2 + " GbaValid =" + isGbaValid(context));
        if (context.getResources().getBoolean(17956924) && getBooleanCarrierConfig(context, "carrier_wfc_ims_available_bool")) {
            isGbaValid = isGbaValid(context);
        } else {
            isGbaValid = false;
        }
        return isGbaValid;
    }

    public boolean isWfcEnabledByPlatformForSlot() {
        boolean z = false;
        if (SystemProperties.getInt(PROPERTY_DBG_WFC_AVAIL_OVERRIDE, 0) == 1) {
            return DBG;
        }
        if (this.mContext.getResources().getBoolean(17956924) && getBooleanCarrierConfigForSlot("carrier_wfc_ims_available_bool")) {
            z = isGbaValidForSlot();
        }
        return z;
    }

    private static boolean isGbaValid(Context context) {
        if (!getBooleanCarrierConfig(context, "carrier_ims_gba_required_bool")) {
            return DBG;
        }
        String efIst = TelephonyManager.getDefault().getIsimIst();
        if (efIst == null) {
            loge("ISF is NULL");
            return DBG;
        }
        boolean result = (efIst == null || efIst.length() <= 1) ? false : (((byte) efIst.charAt(1)) & 2) != 0 ? DBG : false;
        log("GBA capable=" + result + ", ISF=" + efIst);
        return result;
    }

    private boolean isGbaValidForSlot() {
        if (!getBooleanCarrierConfigForSlot("carrier_ims_gba_required_bool")) {
            return DBG;
        }
        String efIst = TelephonyManager.getDefault().getIsimIst();
        if (efIst == null) {
            loge("isGbaValidForSlot - ISF is NULL");
            return DBG;
        }
        boolean result = (efIst == null || efIst.length() <= 1) ? false : (((byte) efIst.charAt(1)) & 2) != 0 ? DBG : false;
        log("isGbaValidForSlot - GBA capable=" + result + ", ISF=" + efIst);
        return result;
    }

    public static void onProvisionedValueChanged(Context context, int item, String value) {
        Rlog.d(TAG, "onProvisionedValueChanged: item=" + item + " val=" + value);
        ImsManager mgr = getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
        switch (item) {
            case 10:
                mgr.setVolteProvisionedProperty(value.equals("1"));
                Rlog.d(TAG, "isVoLteProvisioned = " + mgr.isVolteProvisioned());
                return;
            case 11:
                mgr.setVtProvisionedProperty(value.equals("1"));
                Rlog.d(TAG, "isVtProvisioned = " + mgr.isVtProvisioned());
                return;
            case 28:
                mgr.setWfcProvisionedProperty(value.equals("1"));
                Rlog.d(TAG, "isWfcProvisioned = " + mgr.isWfcProvisioned());
                return;
            default:
                return;
        }
    }

    private void handleUpdateProvisionedValues() {
        if (getBooleanCarrierConfigForSlot("carrier_volte_provisioning_required_bool")) {
            new AsyncUpdateProvisionedValues().execute(new Void[0]);
        }
    }

    private void updateProvisionedValues() {
        if (this.mProvisionBackoff != null) {
            this.mProvisionBackoff.start();
        } else {
            handleUpdateProvisionedValues();
        }
    }

    public static void updateImsServiceConfig(Context context, int phoneId, boolean force) {
        if (force || TelephonyManager.getDefault().getSimState() == 5) {
            ImsManager imsManager = getInstance(context, phoneId);
            if (imsManager != null && (!imsManager.mConfigUpdated || force)) {
                try {
                    imsManager.updateProvisionedValues();
                    if (((imsManager.updateVolteFeatureValue() | imsManager.updateWfcFeatureAndProvisionedValues()) | imsManager.updateVideoCallFeatureValue()) || (isTurnOffImsAllowedByPlatform(context) ^ 1) != 0) {
                        log("updateImsServiceConfig: turnOnIms");
                        imsManager.turnOnIms();
                    } else {
                        log("updateImsServiceConfig: turnOffIms");
                        imsManager.turnOffIms();
                    }
                    imsManager.mConfigUpdated = DBG;
                } catch (ImsException e) {
                    loge("updateImsServiceConfig: ", e);
                    imsManager.mConfigUpdated = false;
                }
            }
            return;
        }
        log("updateImsServiceConfig: SIM not ready");
    }

    public void updateImsServiceConfigForSlot(boolean force) {
        if (force || TelephonyManager.getDefault().getSimState() == 5) {
            if (!this.mConfigUpdated || force) {
                try {
                    updateProvisionedValues();
                    if (((updateVolteFeatureValue() | updateWfcFeatureAndProvisionedValues()) | updateVideoCallFeatureValue()) || (isTurnOffImsAllowedByPlatformForSlot() ^ 1) != 0) {
                        log("updateImsServiceConfigForSlot: turnOnIms");
                        turnOnIms();
                    } else {
                        log("updateImsServiceConfigForSlot: turnOffIms");
                        turnOffIms();
                    }
                    this.mConfigUpdated = DBG;
                } catch (ImsException e) {
                    loge("updateImsServiceConfigForSlot: ", e);
                    this.mConfigUpdated = false;
                }
            }
            return;
        }
        log("updateImsServiceConfigForSlot: SIM not ready");
    }

    private boolean updateVolteFeatureValue() throws ImsException {
        int i;
        boolean available = isVolteEnabledByPlatformForSlot();
        boolean enabled = isEnhanced4gLteModeSettingEnabledByUserForSlot();
        boolean isNonTty = isNonTtyOrTtyOnVolteEnabledForSlot();
        boolean z = (available && enabled) ? isNonTty : false;
        log("updateVolteFeatureValue: available = " + available + ", enabled = " + enabled + ", nonTTY = " + isNonTty);
        ImsConfig configInterface = getConfigInterface();
        if (z) {
            i = 1;
        } else {
            i = 0;
        }
        configInterface.setFeatureValue(0, 13, i, this.mImsConfigListener);
        return z;
    }

    private boolean updateVideoCallFeatureValue() throws ImsException {
        int i;
        boolean available = isVtEnabledByPlatformForSlot();
        boolean enabled = isVtEnabledByUserForSlot();
        boolean isNonTty = isNonTtyOrTtyOnVolteEnabledForSlot();
        boolean isDataEnabled = isDataEnabled();
        boolean isFeatureOn = (available && enabled && isNonTty) ? !getBooleanCarrierConfig(this.mContext, "ignore_data_enabled_changed_for_video_calls") ? isDataEnabled : DBG : false;
        log("updateVideoCallFeatureValue: available = " + available + ", enabled = " + enabled + ", nonTTY = " + isNonTty + ", data enabled = " + isDataEnabled);
        ImsConfig configInterface = getConfigInterface();
        if (isFeatureOn) {
            i = 1;
        } else {
            i = 0;
        }
        configInterface.setFeatureValue(1, 13, i, this.mImsConfigListener);
        return isFeatureOn;
    }

    private boolean updateWfcFeatureAndProvisionedValues() throws ImsException {
        int i;
        boolean isNetworkRoaming = TelephonyManager.getDefault().isNetworkRoaming();
        boolean available = isWfcEnabledByPlatformForSlot();
        boolean enabled = isWfcEnabledByUserForSlot();
        int mode = getWfcModeForSlot(isNetworkRoaming);
        boolean roaming = isWfcRoamingEnabledByUserForSlot();
        boolean z = available ? enabled : false;
        log("updateWfcFeatureAndProvisionedValues: available = " + available + ", enabled = " + enabled + ", mode = " + mode + ", roaming = " + roaming);
        ImsConfig configInterface = getConfigInterface();
        if (z) {
            i = 1;
        } else {
            i = 0;
        }
        configInterface.setFeatureValue(2, 18, i, this.mImsConfigListener);
        if (!z) {
            mode = 1;
            roaming = false;
        }
        setWfcModeInternal(this.mContext, mode);
        setWfcRoamingSettingInternal(roaming);
        return z;
    }

    public ImsManager(Context context, int phoneId) {
        this.mContext = context;
        this.mPhoneId = phoneId;
        this.mConfigDynamicBind = this.mContext.getResources().getBoolean(17956941);
        this.mConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        if (Looper.getMainLooper() != null) {
            this.mProvisionBackoff = new ExponentialBackoff(BACKOFF_INITIAL_DELAY_MS, BACKOFF_MAX_DELAY_MS, 2, new Handler(Looper.getMainLooper()), new AnonymousClass2(this));
        }
        createImsService();
    }

    /* synthetic */ void -com_android_ims_ImsManager-mthref-0() {
        handleUpdateProvisionedValues();
    }

    public boolean isDynamicBinding() {
        return this.mConfigDynamicBind;
    }

    public boolean isServiceAvailable() {
        connectIfServiceIsAvailable();
        log("isServiceAvailable: end mImsServiceProxy=" + this.mImsServiceProxy + ",mImsServiceProxy.isBinderAlive()=" + this.mImsServiceProxy.isBinderAlive());
        return this.mImsServiceProxy.isBinderAlive();
    }

    public void connectIfServiceIsAvailable() {
        if (this.mImsServiceProxy == null || (this.mImsServiceProxy.isBinderAlive() ^ 1) != 0) {
            createImsService();
        }
    }

    public void setImsConfigListener(ImsConfigListener listener) {
        this.mImsConfigListener = listener;
    }

    public void addNotifyStatusChangedCallbackIfAvailable(INotifyStatusChanged c) throws ImsException {
        if (!this.mImsServiceProxy.isBinderAlive()) {
            throw new ImsException("Binder is not active!", 106);
        } else if (c != null) {
            this.mStatusCallbacks.add(c);
        }
    }

    public int open(int serviceClass, PendingIntent incomingCallPendingIntent, ImsConnectionStateListener listener) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        if (incomingCallPendingIntent == null) {
            throw new NullPointerException("incomingCallPendingIntent can't be null");
        } else if (listener == null) {
            throw new NullPointerException("listener can't be null");
        } else {
            try {
                int result = this.mImsServiceProxy.startSession(incomingCallPendingIntent, new ImsRegistrationListenerProxy());
                addRegistrationListener(listener);
                log("open: Session started and registration listener added.");
                if (result > 0) {
                    return result;
                }
                throw new ImsException("open()", result * -1);
            } catch (RemoteException e) {
                throw new ImsException("open()", e, 106);
            }
        }
    }

    public void addRegistrationListener(int serviceClass, ImsConnectionStateListener listener) throws ImsException {
        addRegistrationListener(listener);
    }

    public void addRegistrationListener(ImsConnectionStateListener listener) throws ImsException {
        if (listener == null) {
            throw new NullPointerException("listener can't be null");
        }
        synchronized (this.mHasRegisteredLock) {
            if (!this.mHasRegisteredForProxy) {
                try {
                    checkAndThrowExceptionIfServiceUnavailable();
                    this.mImsServiceProxy.addRegistrationListener(this.mRegistrationListenerProxy);
                    log("RegistrationListenerProxy registered.");
                    this.mHasRegisteredForProxy = DBG;
                } catch (RemoteException e) {
                    throw new ImsException("addRegistrationListener()", e, 106);
                }
            }
        }
        synchronized (this.mRegistrationListeners) {
            log("Local registration listener added: " + listener);
            this.mRegistrationListeners.add(listener);
        }
    }

    public void removeRegistrationListener(ImsConnectionStateListener listener) throws ImsException {
        if (listener == null) {
            throw new NullPointerException("listener can't be null");
        }
        synchronized (this.mRegistrationListeners) {
            log("Local registration listener removed: " + listener);
            this.mRegistrationListeners.remove(listener);
        }
    }

    public void close(int sessionId) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            this.mImsServiceProxy.endSession(sessionId);
            this.mUt = null;
            this.mConfig = null;
            this.mEcbm = null;
            this.mMultiEndpoint = null;
        } catch (RemoteException e) {
            throw new ImsException("close()", e, 106);
        } catch (Throwable th) {
            this.mUt = null;
            this.mConfig = null;
            this.mEcbm = null;
            this.mMultiEndpoint = null;
        }
    }

    public ImsUtInterface getSupplementaryServiceConfiguration() throws ImsException {
        if (this.mUt != null && this.mUt.isBinderAlive()) {
            return this.mUt;
        }
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            IImsUt iUt = this.mImsServiceProxy.getUtInterface();
            if (iUt == null) {
                throw new ImsException("getSupplementaryServiceConfiguration()", 801);
            }
            log("getSupplementaryServiceConfiguration: iUt = " + iUt + ", mPhoneId = " + this.mPhoneId);
            this.mUt = new ImsUt(iUt, this.mPhoneId);
            return this.mUt;
        } catch (RemoteException e) {
            throw new ImsException("getSupplementaryServiceConfiguration()", e, 106);
        }
    }

    public boolean isConnected(int serviceType, int callType) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            return this.mImsServiceProxy.isConnected(serviceType, callType);
        } catch (RemoteException e) {
            throw new ImsException("isServiceConnected()", e, 106);
        }
    }

    public boolean isOpened() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            return this.mImsServiceProxy.isOpened();
        } catch (RemoteException e) {
            throw new ImsException("isOpened()", e, 106);
        }
    }

    public ImsCallProfile createCallProfile(int sessionId, int serviceType, int callType) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            return this.mImsServiceProxy.createCallProfile(sessionId, serviceType, callType);
        } catch (RemoteException e) {
            throw new ImsException("createCallProfile()", e, 106);
        }
    }

    public ImsCall makeCall(int sessionId, ImsCallProfile profile, String[] callees, Listener listener) throws ImsException {
        log("makeCall :: sessionId=" + sessionId + ", profile=" + profile);
        checkAndThrowExceptionIfServiceUnavailable();
        ImsCall call = new ImsCall(this.mContext, profile);
        call.setListener(listener);
        ImsCallSession session = createCallSession(sessionId, profile);
        if (profile.getCallExtraBoolean("isConferenceUri", false) || callees == null || callees.length != 1) {
            call.start(session, callees);
        } else {
            call.start(session, callees[0]);
        }
        return call;
    }

    public ImsCall takeCall(int sessionId, Intent incomingCallIntent, Listener listener) throws ImsException {
        log("takeCall :: sessionId=" + sessionId + ", incomingCall=" + incomingCallIntent);
        checkAndThrowExceptionIfServiceUnavailable();
        if (incomingCallIntent == null) {
            throw new ImsException("Can't retrieve session with null intent", INCOMING_CALL_RESULT_CODE);
        } else if (sessionId != getImsSessionId(incomingCallIntent)) {
            throw new ImsException("Service id is mismatched in the incoming call intent", INCOMING_CALL_RESULT_CODE);
        } else {
            String callId = getCallId(incomingCallIntent);
            if (callId == null) {
                throw new ImsException("Call ID missing in the incoming call intent", INCOMING_CALL_RESULT_CODE);
            }
            try {
                IImsCallSession session = this.mImsServiceProxy.getPendingCallSession(sessionId, callId);
                if (session == null) {
                    throw new ImsException("No pending session for the call", 107);
                }
                ImsCall call = new ImsCall(this.mContext, session.getCallProfile());
                call.attachSession(new ImsCallSession(session));
                call.setListener(listener);
                return call;
            } catch (Throwable t) {
                ImsException imsException = new ImsException("takeCall()", t, 0);
            }
        }
    }

    public ImsConfig getConfigInterface() throws ImsException {
        if (this.mConfig != null && this.mConfig.isBinderAlive()) {
            return this.mConfig;
        }
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            IImsConfig config = this.mImsServiceProxy.getConfigInterface();
            if (config == null) {
                throw new ImsException("getConfigInterface()", 131);
            }
            this.mConfig = new ImsConfig(config, this.mContext);
            log("getConfigInterface(), mConfig= " + this.mConfig);
            return this.mConfig;
        } catch (RemoteException e) {
            throw new ImsException("getConfigInterface()", e, 106);
        }
    }

    public void setTtyMode(int ttyMode) throws ImsException {
        boolean z = false;
        if (!getBooleanCarrierConfigForSlot("carrier_volte_tty_supported_bool")) {
            if (ttyMode == 0) {
                z = isEnhanced4gLteModeSettingEnabledByUserForSlot();
            }
            setAdvanced4GMode(z);
        }
    }

    public void setUiTTYMode(Context context, int uiTtyMode, Message onComplete) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            this.mImsServiceProxy.setUiTTYMode(uiTtyMode, onComplete);
        } catch (RemoteException e) {
            throw new ImsException("setTTYMode()", e, 106);
        }
    }

    private ImsReasonInfo makeACopy(ImsReasonInfo imsReasonInfo) {
        Parcel p = Parcel.obtain();
        imsReasonInfo.writeToParcel(p, 0);
        p.setDataPosition(0);
        ImsReasonInfo clonedReasonInfo = (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(p);
        p.recycle();
        return clonedReasonInfo;
    }

    public ArrayList<ImsReasonInfo> getRecentImsDisconnectReasons() {
        ArrayList<ImsReasonInfo> disconnectReasons = new ArrayList();
        for (ImsReasonInfo reason : this.mRecentDisconnectReasons) {
            disconnectReasons.add(makeACopy(reason));
        }
        return disconnectReasons;
    }

    public int getImsServiceStatus() throws ImsException {
        return this.mImsServiceProxy.getFeatureStatus();
    }

    private static boolean getBooleanCarrierConfig(Context context, String key) {
        CarrierConfigManager configManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        PersistableBundle b = null;
        if (configManager != null) {
            b = configManager.getConfigForSubId(HwFrameworkFactory.getHwInnerTelephonyManager().getDefault4GSlotId());
        }
        if (b != null) {
            return b.getBoolean(key);
        }
        return CarrierConfigManager.getDefaultConfig().getBoolean(key);
    }

    private boolean getBooleanCarrierConfigForSlot(String key) {
        int[] subIds = SubscriptionManager.getSubId(this.mPhoneId);
        int subId = -1;
        if (subIds != null && subIds.length >= 1) {
            subId = subIds[0];
        }
        PersistableBundle b = null;
        if (this.mConfigManager != null) {
            b = this.mConfigManager.getConfigForSubId(subId);
        }
        if (b != null) {
            return b.getBoolean(key);
        }
        return CarrierConfigManager.getDefaultConfig().getBoolean(key);
    }

    private static int getIntCarrierConfig(Context context, String key) {
        CarrierConfigManager configManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        PersistableBundle b = null;
        if (configManager != null) {
            b = configManager.getConfigForSubId(HwFrameworkFactory.getHwInnerTelephonyManager().getDefault4GSlotId());
        }
        if (b != null) {
            return b.getInt(key);
        }
        return CarrierConfigManager.getDefaultConfig().getInt(key);
    }

    private static Boolean checkCarrierConfigKeyExist(Context context, String key) {
        Boolean ifExist = Boolean.valueOf(false);
        CarrierConfigManager configManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        PersistableBundle b = null;
        if (configManager != null) {
            b = configManager.getConfigForSubId(HwFrameworkFactory.getHwInnerTelephonyManager().getDefault4GSlotId());
        }
        if (!(b == null || b.get(key) == null)) {
            ifExist = Boolean.valueOf(DBG);
        }
        log("carrierConfig key[" + key + "] " + (ifExist.booleanValue() ? "exists" : "does not exist"));
        return ifExist;
    }

    private int getIntCarrierConfigForSlot(String key) {
        int[] subIds = SubscriptionManager.getSubId(this.mPhoneId);
        int subId = -1;
        if (subIds != null && subIds.length >= 1) {
            subId = subIds[0];
        }
        PersistableBundle b = null;
        if (this.mConfigManager != null) {
            b = this.mConfigManager.getConfigForSubId(subId);
        }
        if (b != null) {
            return b.getInt(key);
        }
        return CarrierConfigManager.getDefaultConfig().getInt(key);
    }

    private static String getCallId(Intent incomingCallIntent) {
        if (incomingCallIntent == null) {
            return null;
        }
        return incomingCallIntent.getStringExtra(EXTRA_CALL_ID);
    }

    private static int getImsSessionId(Intent incomingCallIntent) {
        if (incomingCallIntent == null) {
            return -1;
        }
        return incomingCallIntent.getIntExtra(EXTRA_SERVICE_ID, -1);
    }

    private void checkAndThrowExceptionIfServiceUnavailable() throws ImsException {
        if (this.mImsServiceProxy == null || (this.mImsServiceProxy.isBinderAlive() ^ 1) != 0) {
            createImsService();
            if (this.mImsServiceProxy == null) {
                throw new ImsException("Service is unavailable", 106);
            }
        }
    }

    private void createImsService() {
        if (this.mConfigDynamicBind) {
            Rlog.i(TAG, "Creating ImsService using ImsResolver");
            this.mImsServiceProxy = getServiceProxy();
        } else {
            Rlog.i(TAG, "Creating ImsService using ServiceManager");
            this.mImsServiceProxy = getServiceProxyCompat();
        }
        synchronized (this.mHasRegisteredLock) {
            this.mHasRegisteredForProxy = false;
        }
    }

    private ImsServiceProxyCompat getServiceProxyCompat() {
        IBinder binder = ServiceManager.checkService(IMS_SERVICE);
        if (binder != null) {
            try {
                binder.linkToDeath(this.mDeathRecipient, 0);
            } catch (RemoteException e) {
            }
        }
        return new ImsServiceProxyCompat(this.mPhoneId, binder);
    }

    private ImsServiceProxy getServiceProxy() {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        ImsServiceProxy serviceProxy = new ImsServiceProxy(this.mPhoneId, 1);
        serviceProxy.setStatusCallback(new com.android.ims.-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng.AnonymousClass1(this));
        IImsServiceController b = tm.getImsServiceControllerAndListen(this.mPhoneId, 1, serviceProxy.getListener());
        if (b != null) {
            serviceProxy.setBinder(b.asBinder());
            serviceProxy.getFeatureStatus();
        } else {
            Rlog.w(TAG, "getServiceProxy: b is null! Phone Id: " + this.mPhoneId);
        }
        return serviceProxy;
    }

    /* synthetic */ void lambda$-com_android_ims_ImsManager_96779() {
        this.mStatusCallbacks.forEach(-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng.$INST$5);
    }

    private ImsCallSession createCallSession(int serviceId, ImsCallProfile profile) throws ImsException {
        try {
            return new ImsCallSession(this.mImsServiceProxy.createCallSession(serviceId, profile, null));
        } catch (RemoteException e) {
            Rlog.w(TAG, "CreateCallSession: Error, remote exception: " + e.getMessage());
            throw new ImsException("createCallSession()", e, 106);
        }
    }

    private static void log(String s) {
        Rlog.d(TAG, s);
    }

    private static void loge(String s) {
        Rlog.e(TAG, s);
    }

    private static void loge(String s, Throwable t) {
        Rlog.e(TAG, s, t);
    }

    private void turnOnIms() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        if (!isATT || (isEnhanced4gLteModeSettingEnabledByUser(this.mContext) ^ 1) == 0) {
            try {
                this.mImsServiceProxy.turnOnIms();
                return;
            } catch (RemoteException e) {
                throw new ImsException("turnOnIms() ", e, 106);
            }
        }
        log("turnOnIms: Enhanced LTE Service is off, return.");
    }

    private boolean isImsTurnOffAllowed() {
        if (isATT) {
            return isTurnOffImsAllowedByPlatformForSlot();
        }
        boolean z;
        if (!isTurnOffImsAllowedByPlatformForSlot()) {
            z = false;
        } else if (isWfcEnabledByPlatformForSlot()) {
            z = isWfcEnabledByUserForSlot() ^ 1;
        } else {
            z = DBG;
        }
        return z;
    }

    private void setLteFeatureValues(boolean turnOn) {
        int i = 1;
        log("setLteFeatureValues: " + turnOn);
        try {
            ImsConfig config = getConfigInterface();
            if (config != null) {
                int i2;
                if (turnOn) {
                    i2 = 1;
                } else {
                    i2 = 0;
                }
                config.setFeatureValue(0, 13, i2, this.mImsConfigListener);
                if (isVolteEnabledByPlatformForSlot()) {
                    boolean enableViLte = (turnOn && isVtEnabledByUserForSlot()) ? !getBooleanCarrierConfig(this.mContext, "ignore_data_enabled_changed_for_video_calls") ? isDataEnabled() : DBG : false;
                    if (enableViLte) {
                        i2 = 1;
                    } else {
                        i2 = 0;
                    }
                    config.setFeatureValue(1, 13, i2, this.mImsConfigListener);
                }
                if (isATT && isWfcEnabledByPlatform(this.mContext)) {
                    log("set feature value of vowifi if wifi calling switcher is on.");
                    if (!(turnOn ? isWfcEnabledByUser(this.mContext) : false)) {
                        i = 0;
                    }
                    config.setFeatureValue(2, 18, i, this.mImsConfigListener);
                }
            }
        } catch (ImsException e) {
            loge("setLteFeatureValues: exception ", e);
        }
    }

    private void setAdvanced4GMode(boolean turnOn) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        if (turnOn) {
            setLteFeatureValues(turnOn);
            log("setAdvanced4GMode: turnOnIms");
            turnOnIms();
            return;
        }
        if (isImsTurnOffAllowed()) {
            log("setAdvanced4GMode: turnOffIms");
            turnOffIms();
        }
        setLteFeatureValues(turnOn);
    }

    private void turnOffIms() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        if (isATT && isEnhanced4gLteModeSettingEnabledByUser(this.mContext)) {
            log("turnOffIms: Enhanced LTE Service is on, return.");
            return;
        }
        try {
            this.mImsServiceProxy.turnOffIms();
        } catch (RemoteException e) {
            throw new ImsException("turnOffIms() ", e, 106);
        }
    }

    private void addToRecentDisconnectReasons(ImsReasonInfo reason) {
        if (reason != null) {
            while (this.mRecentDisconnectReasons.size() >= MAX_RECENT_DISCONNECT_REASONS) {
                this.mRecentDisconnectReasons.removeFirst();
            }
            this.mRecentDisconnectReasons.addLast(reason);
        }
    }

    public ImsEcbm getEcbmInterface(int serviceId) throws ImsException {
        if (this.mEcbm != null && this.mEcbm.isBinderAlive()) {
            return this.mEcbm;
        }
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            IImsEcbm iEcbm = this.mImsServiceProxy.getEcbmInterface();
            if (iEcbm == null) {
                throw new ImsException("getEcbmInterface()", 901);
            }
            this.mEcbm = new ImsEcbm(iEcbm);
            return this.mEcbm;
        } catch (RemoteException e) {
            throw new ImsException("getEcbmInterface()", e, 106);
        }
    }

    public ImsMultiEndpoint getMultiEndpointInterface(int serviceId) throws ImsException {
        if (this.mMultiEndpoint != null && this.mMultiEndpoint.isBinderAlive()) {
            return this.mMultiEndpoint;
        }
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            IImsMultiEndpoint iImsMultiEndpoint = this.mImsServiceProxy.getMultiEndpointInterface();
            if (iImsMultiEndpoint == null) {
                throw new ImsException("getMultiEndpointInterface()", 902);
            }
            this.mMultiEndpoint = new ImsMultiEndpoint(iImsMultiEndpoint);
            return this.mMultiEndpoint;
        } catch (RemoteException e) {
            throw new ImsException("getMultiEndpointInterface()", e, 106);
        }
    }

    public static void factoryReset(Context context) {
        int i;
        int i2 = 0;
        Global.putInt(context.getContentResolver(), "volte_vt_enabled", 1);
        ContentResolver contentResolver = context.getContentResolver();
        String str = "wfc_ims_enabled";
        if (getBooleanCarrierConfig(context, "carrier_default_wfc_ims_enabled_bool")) {
            i = 1;
        } else {
            i = 0;
        }
        Global.putInt(contentResolver, str, i);
        Global.putInt(context.getContentResolver(), "wfc_ims_mode", getIntCarrierConfig(context, "carrier_default_wfc_ims_mode_int"));
        Global.putInt(context.getContentResolver(), "wfc_ims_roaming_mode", getIntCarrierConfig(context, "carrier_default_wfc_ims_roaming_mode_int"));
        ContentResolver contentResolver2 = context.getContentResolver();
        String str2 = "wfc_ims_roaming_enabled";
        if (getBooleanCarrierConfig(context, "carrier_default_wfc_ims_roaming_enabled_bool")) {
            i2 = 1;
        }
        Global.putInt(contentResolver2, str2, i2);
        Global.putInt(context.getContentResolver(), "vt_ims_enabled", 1);
        updateImsServiceConfig(context, HwFrameworkFactory.getHwInnerTelephonyManager().getDefault4GSlotId(), DBG);
    }

    public void factoryResetSlot() {
        int i;
        int i2 = 0;
        Global.putInt(this.mContext.getContentResolver(), "volte_vt_enabled", 1);
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String str = "wfc_ims_enabled";
        if (getBooleanCarrierConfigForSlot("carrier_default_wfc_ims_enabled_bool")) {
            i = 1;
        } else {
            i = 0;
        }
        Global.putInt(contentResolver, str, i);
        Global.putInt(this.mContext.getContentResolver(), "wfc_ims_mode", getIntCarrierConfigForSlot("carrier_default_wfc_ims_mode_int"));
        ContentResolver contentResolver2 = this.mContext.getContentResolver();
        String str2 = "wfc_ims_roaming_enabled";
        if (getBooleanCarrierConfigForSlot("carrier_default_wfc_ims_roaming_enabled_bool")) {
            i2 = 1;
        }
        Global.putInt(contentResolver2, str2, i2);
        Global.putInt(this.mContext.getContentResolver(), "vt_ims_enabled", 1);
        updateImsServiceConfigForSlot(DBG);
    }

    private boolean isDataEnabled() {
        return SystemProperties.getBoolean(DATA_ENABLED_PROP, DBG);
    }

    public void setDataEnabled(boolean enabled) {
        log("setDataEnabled: " + enabled);
        SystemProperties.set(DATA_ENABLED_PROP, enabled ? TRUE : FALSE);
    }

    private boolean isVolteProvisioned() {
        return SystemProperties.getBoolean(VOLTE_PROVISIONED_PROP, DBG);
    }

    private void setVolteProvisionedProperty(boolean provisioned) {
        SystemProperties.set(VOLTE_PROVISIONED_PROP, provisioned ? TRUE : FALSE);
    }

    private boolean isWfcProvisioned() {
        return SystemProperties.getBoolean(WFC_PROVISIONED_PROP, DBG);
    }

    private void setWfcProvisionedProperty(boolean provisioned) {
        SystemProperties.set(WFC_PROVISIONED_PROP, provisioned ? TRUE : FALSE);
    }

    private boolean isVtProvisioned() {
        return SystemProperties.getBoolean(VT_PROVISIONED_PROP, DBG);
    }

    private void setVtProvisionedProperty(boolean provisioned) {
        SystemProperties.set(VT_PROVISIONED_PROP, provisioned ? TRUE : FALSE);
    }

    public ImsConfigListener getImsConfigListener() {
        return this.mImsConfigListener;
    }

    public ImsServiceProxyCompat getImsServiceProxy() {
        return this.mImsServiceProxy;
    }

    public void createImsServiceProxy(ImsServiceProxyCompat imsServiceProxyCompat) {
        this.mImsServiceProxy = imsServiceProxyCompat;
    }

    public Set<INotifyStatusChanged> getStatusCallbacks() {
        return this.mStatusCallbacks;
    }

    public ImsServiceDeathRecipient getImsServiceDeathRecipient() {
        return this.mDeathRecipient;
    }

    public boolean getConfigUpdated() {
        return this.mConfigUpdated;
    }

    public void setConfigUpdated(boolean isconfigupdated) {
        this.mConfigUpdated = isconfigupdated;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("ImsManager:");
        pw.println("  mPhoneId = " + this.mPhoneId);
        pw.println("  mConfigUpdated = " + this.mConfigUpdated);
        pw.println("  mImsServiceProxy = " + this.mImsServiceProxy);
        pw.println("  mDataEnabled = " + isDataEnabled());
        pw.println("  ignoreDataEnabledChanged = " + getBooleanCarrierConfig(this.mContext, "ignore_data_enabled_changed_for_video_calls"));
        pw.println("  isGbaValid = " + isGbaValidForSlot());
        pw.println("  isImsTurnOffAllowed = " + isImsTurnOffAllowed());
        pw.println("  isNonTtyOrTtyOnVolteEnabled = " + isNonTtyOrTtyOnVolteEnabledForSlot());
        pw.println("  isVolteEnabledByPlatform = " + isVolteEnabledByPlatformForSlot());
        pw.println("  isVolteProvisionedOnDevice = " + isVolteProvisionedOnDeviceForSlot());
        pw.println("  isEnhanced4gLteModeSettingEnabledByUser = " + isEnhanced4gLteModeSettingEnabledByUserForSlot());
        pw.println("  isVtEnabledByPlatform = " + isVtEnabledByPlatformForSlot());
        pw.println("  isVtEnabledByUser = " + isVtEnabledByUserForSlot());
        pw.println("  isWfcEnabledByPlatform = " + isWfcEnabledByPlatformForSlot());
        pw.println("  isWfcEnabledByUser = " + isWfcEnabledByUserForSlot());
        pw.println("  getWfcMode = " + getWfcModeForSlot(false));
        pw.println("  getWfcMode(roaming) = " + getWfcModeForSlot(DBG));
        pw.println("  isWfcRoamingEnabledByUser = " + isWfcRoamingEnabledByUserForSlot());
        pw.println("  isVtProvisionedOnDevice = " + isVtProvisionedOnDeviceForSlot());
        pw.println("  isWfcProvisionedOnDevice = " + isWfcProvisionedOnDeviceForSlot());
        pw.flush();
    }
}
