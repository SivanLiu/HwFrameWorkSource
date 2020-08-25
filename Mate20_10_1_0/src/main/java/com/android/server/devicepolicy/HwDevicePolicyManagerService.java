package com.android.server.devicepolicy;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.NotificationManager;
import android.app.PackageInstallObserver;
import android.app.StatusBarManager;
import android.app.SynchronousUserSwitchObserver;
import android.app.backup.BackupManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.common.HwFrameworkFactory;
import android.common.HwFrameworkMonitor;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.security.KeyChain;
import android.security.KeyStore;
import android.telecom.TelecomManager;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Flog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.net.VpnProfile;
import com.android.internal.telephony.ITelephony;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.JournaledFile;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.devicepolicy.AbsDevicePolicyManagerService;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import com.android.server.devicepolicy.PolicyStruct;
import com.android.server.devicepolicy.plugins.DeviceApplicationPlugin;
import com.android.server.devicepolicy.plugins.DeviceBluetoothPlugin;
import com.android.server.devicepolicy.plugins.DeviceCameraPlugin;
import com.android.server.devicepolicy.plugins.DeviceControlPlugin;
import com.android.server.devicepolicy.plugins.DeviceFirewallManagerImpl;
import com.android.server.devicepolicy.plugins.DeviceInfraredPlugin;
import com.android.server.devicepolicy.plugins.DeviceLocationPlugin;
import com.android.server.devicepolicy.plugins.DeviceNetworkPlugin;
import com.android.server.devicepolicy.plugins.DeviceP2PPlugin;
import com.android.server.devicepolicy.plugins.DevicePackageManagerPlugin;
import com.android.server.devicepolicy.plugins.DevicePasswordPlugin;
import com.android.server.devicepolicy.plugins.DeviceRestrictionPlugin;
import com.android.server.devicepolicy.plugins.DeviceStorageManagerPlugin;
import com.android.server.devicepolicy.plugins.DeviceTelephonyPlugin;
import com.android.server.devicepolicy.plugins.DeviceVpnManagerImpl;
import com.android.server.devicepolicy.plugins.DeviceWifiPlugin;
import com.android.server.devicepolicy.plugins.FrameworkTestPlugin;
import com.android.server.devicepolicy.plugins.HwEmailMDMPlugin;
import com.android.server.devicepolicy.plugins.HwSystemManagerPlugin;
import com.android.server.devicepolicy.plugins.PhoneManagerPlugin;
import com.android.server.devicepolicy.plugins.SettingsMDMPlugin;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.mtm.taskstatus.ProcessCleaner;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.huawei.android.app.HwActivityTaskManager;
import com.huawei.android.widget.LockPatternUtilsEx;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
import com.huawei.systemmanager.appcontrol.iaware.HwAppStartupSettingEx;
import com.huawei.systemmanager.appcontrol.iaware.HwIAwareManager;
import com.huawei.systemmanager.appcontrol.iaware.IMultiTaskManager;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public class HwDevicePolicyManagerService extends DevicePolicyManagerService implements IHwDevicePolicyManager {
    private static final int AS_MODIFIER_USER = 1;
    private static final int AS_TP_SHW = 0;
    private static final int AS_TP_SLF = 1;
    private static final int AS_TP_SMT = 0;
    private static final String ATTR_VALUE = "value";
    public static final int CERTIFICATE_PEM_BASE64 = 1;
    public static final int CERTIFICATE_PKCS12 = 0;
    private static final Set<Integer> DA_DISALLOWED_POLICIES = new ArraySet();
    public static final String DESCRIPTOR = "com.android.internal.widget.ILockSettings";
    private static final String DEVICE_POLICIES_1_XML = "device_policies_1.xml";
    private static final String DEVICE_POLICIES_XML = "device_policies.xml";
    protected static final String DPMDESCRIPTOR = "android.app.admin.IDevicePolicyManager";
    public static final String DYNAMIC_ROOT_PROP = "persist.sys.root.status";
    public static final String DYNAMIC_ROOT_STATE_SAFE = "0";
    private static final String EXCHANGE_DOMAIN = "domain";
    private static final int EXCHANGE_PROVIDER_MAX_NUM = 20;
    private static final int FAILED = -1;
    private static final Set<String> HWDEVICE_OWNER_USER_RESTRICTIONS = new HashSet();
    private static final boolean IS_DEBUG = false;
    private static final boolean IS_HAS_HW_MDM_FEATURE = true;
    private static final boolean IS_SUPPORT_CRYPT = SystemProperties.getBoolean("ro.config.support_sdcard_crypt", true);
    private static final String KEY = "key";
    private static final int MAX_QUERY_PROCESS = 10000;
    private static final double MAX_RETRY_TIMES = 3.0d;
    private static final String MDM_VPN_PERMISSION = "com.huawei.permission.sec.MDM_VPN";
    private static final int[] MODIFY_CATEGARY = {1, 1, 1, 1};
    public static final int NOT_SUPPORT_SD_CRYPT = -1;
    private static final int OFF = 0;
    private static final int ON = 1;
    private static final String PASSWORD_CHANGE_EXTEND_TIME = "pwd-password-change-extendtime";
    private static final int PERSIST_APP_LIMITS = 10;
    private static final int[] POLICY_CATEGARY = {0, 1, 1, 1};
    public static final String PRIVACY_MODE_ON = "privacy_mode_on";
    public static final int SD_CRYPT_STATE_DECRYPTED = 1;
    public static final int SD_CRYPT_STATE_DECRYPTING = 4;
    public static final int SD_CRYPT_STATE_ENCRYPTED = 2;
    public static final int SD_CRYPT_STATE_ENCRYPTING = 3;
    public static final int SD_CRYPT_STATE_INVALID = 0;
    public static final int SD_CRYPT_STATE_MISMATCH = 5;
    public static final int SD_CRYPT_STATE_WAIT_UNLOCK = 6;
    private static final String SETTINGS_MENUS_REMOVE = "settings_menus_remove";
    private static final int[] SHOW_CATAGARY = {0, 0, 0, 0};
    private static final int STATUS_BAR_DISABLE_MASK = 34013184;
    private static final int SUCCEED = 1;
    private static final String TAG = "HwDPMS";
    public static final int TRANSACTION_SET_ACTIVE_VISITOR_PASSWORD_STATE = 1003;
    private static final String USB_STORAGE = "usb";
    private static final long WAIT_FOR_IAWAREREADY_INTERVAL = 3000;
    private static boolean isSimplePwdOpen = SystemProperties.getBoolean("ro.config.not_allow_simple_pwd", false);
    private static ArrayList<String> userIsolationPolicyList = new ArrayList<String>() {
        /* class com.android.server.devicepolicy.HwDevicePolicyManagerService.AnonymousClass1 */

        {
            add("email-disable-delete-account");
            add("email-disable-add-account");
            add("allowing-addition-black-list");
        }
    };
    private final String descriptorNetworkmanagementService = "android.os.INetworkManagementService";
    private boolean isHasInit = false;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public AlertDialog mErrorDialog;
    private HwAdminCache mHwAdminCache;
    private HwFrameworkMonitor mMonitor = HwFrameworkFactory.getHwFrameworkMonitor();
    private TransactionProcessor mProcessor = null;
    private final UserManager mUserManager;
    final SparseArray<DeviceVisitorPolicyData> mVisitorUserData = new SparseArray<>();

    static {
        DA_DISALLOWED_POLICIES.add(8);
        DA_DISALLOWED_POLICIES.add(9);
        DA_DISALLOWED_POLICIES.add(6);
        DA_DISALLOWED_POLICIES.add(0);
        HWDEVICE_OWNER_USER_RESTRICTIONS.add("no_usb_file_transfer");
        HWDEVICE_OWNER_USER_RESTRICTIONS.add("no_physical_media");
        HWDEVICE_OWNER_USER_RESTRICTIONS.add("no_outgoing_calls");
        HWDEVICE_OWNER_USER_RESTRICTIONS.add("no_sms");
        HWDEVICE_OWNER_USER_RESTRICTIONS.add("no_config_tethering");
    }

    private static class DeviceVisitorPolicyData {
        int mActivePasswordLength = 0;
        int mActivePasswordLetters = 0;
        int mActivePasswordLowerCase = 0;
        int mActivePasswordNonLetter = 0;
        int mActivePasswordNumeric = 0;
        int mActivePasswordQuality = 0;
        int mActivePasswordSymbols = 0;
        int mActivePasswordUpperCase = 0;
        int mFailedPasswordAttempts = 0;
        int mUserHandle;

        DeviceVisitorPolicyData(int userHandle) {
            this.mUserHandle = userHandle;
        }
    }

    public HwDevicePolicyManagerService(Context context) {
        super(context);
        this.mContext = context;
        this.mUserManager = UserManager.get(context);
        HwDevicePolicyManagerServiceUtil.initialize(context);
        this.mProcessor = new TransactionProcessor(this);
        this.mHwAdminCache = new HwAdminCache();
        addDevicePolicyPlugins(context);
        if (this.globalPlugins.size() > 0) {
            Iterator it = this.globalPlugins.iterator();
            while (it.hasNext()) {
                DevicePolicyPlugin plugin = (DevicePolicyPlugin) it.next();
                if (plugin != null) {
                    PolicyStruct struct = plugin.getPolicyStruct();
                    for (PolicyStruct.PolicyItem item : struct.getPolicyItems()) {
                        if (item == null) {
                            HwLog.w(TAG, "policyItem is null in plugin: " + plugin.getPluginName());
                        }
                    }
                    addPolicyStruct(struct);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public DeviceVisitorPolicyData getVisitorUserData(int userHandle) {
        DeviceVisitorPolicyData policy;
        synchronized (getLockObject()) {
            policy = this.mVisitorUserData.get(userHandle);
            if (policy == null) {
                policy = new DeviceVisitorPolicyData(userHandle);
                this.mVisitorUserData.append(userHandle, policy);
                loadVisitorSettingsLocked(policy, userHandle);
            }
        }
        return policy;
    }

    private static JournaledFile makeJournaledFile2(int userHandle) {
        String path = "";
        try {
            path = new File(Environment.getUserSystemDirectory(userHandle), DEVICE_POLICIES_1_XML).getCanonicalPath();
        } catch (IOException e) {
            HwLog.e(TAG, "makeJournaledFile2 : Invalid file path");
        }
        String base = userHandle == 0 ? "/data/system/device_policies_1.xml" : path;
        File file = new File(base);
        return new JournaledFile(file, new File(base + ".tmp"));
    }

    private void saveVisitorSettingsLock(int userHandle) {
        DeviceVisitorPolicyData policy = getVisitorUserData(userHandle);
        JournaledFile journal = makeJournaledFile2(userHandle);
        FileOutputStream stream = null;
        try {
            FileOutputStream stream2 = new FileOutputStream(journal.chooseForWrite(), false);
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(stream2, "utf-8");
            out.startDocument(null, true);
            out.startTag(null, "policies");
            if (!(policy.mActivePasswordQuality == 0 && policy.mActivePasswordLength == 0 && policy.mActivePasswordUpperCase == 0 && policy.mActivePasswordLowerCase == 0 && policy.mActivePasswordLetters == 0 && policy.mActivePasswordNumeric == 0 && policy.mActivePasswordSymbols == 0 && policy.mActivePasswordNonLetter == 0)) {
                out.startTag(null, "active-password2");
                out.attribute(null, "quality", Integer.toString(policy.mActivePasswordQuality));
                out.attribute(null, "length", Integer.toString(policy.mActivePasswordLength));
                out.attribute(null, "uppercase", Integer.toString(policy.mActivePasswordUpperCase));
                out.attribute(null, "lowercase", Integer.toString(policy.mActivePasswordLowerCase));
                out.attribute(null, "letters", Integer.toString(policy.mActivePasswordLetters));
                out.attribute(null, "numeric", Integer.toString(policy.mActivePasswordNumeric));
                out.attribute(null, "symbols", Integer.toString(policy.mActivePasswordSymbols));
                out.attribute(null, "nonletter", Integer.toString(policy.mActivePasswordNonLetter));
                out.endTag(null, "active-password2");
            }
            out.endTag(null, "policies");
            out.endDocument();
            journal.commit();
            try {
                stream2.close();
            } catch (IOException e) {
                HwLog.e(TAG, "cannot close the stream.");
            }
        } catch (IOException e2) {
            journal.rollback();
            HwLog.e(TAG, "saveVisitorSettingsLock happend IOException");
            if (0 != 0) {
                stream.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    stream.close();
                } catch (IOException e3) {
                    HwLog.e(TAG, "cannot close the stream.");
                }
            }
            throw th;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:27:?, code lost:
        r7.close();
     */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x01d7 A[SYNTHETIC, Splitter:B:70:0x01d7] */
    private void loadVisitorSettingsLocked(DeviceVisitorPolicyData policy, int userHandle) {
        Throwable th;
        int i;
        FileInputStream stream = null;
        File file = makeJournaledFile2(userHandle).chooseForRead();
        try {
            stream = new FileInputStream(file);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, null);
            while (true) {
                int type = parser.next();
                i = 1;
                if (type == 1 || type == 2) {
                    String tag = parser.getName();
                }
            }
            String tag2 = parser.getName();
            if ("policies".equals(tag2)) {
                parser.next();
                int outerDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == i || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                        try {
                            break;
                        } catch (IOException e) {
                        }
                    } else {
                        if (type2 != 3) {
                            if (type2 != 4) {
                                String tag3 = parser.getName();
                                if ("active-password2".equals(tag3)) {
                                    policy.mActivePasswordQuality = Integer.parseInt(parser.getAttributeValue(null, "quality"));
                                    policy.mActivePasswordLength = Integer.parseInt(parser.getAttributeValue(null, "length"));
                                    policy.mActivePasswordUpperCase = Integer.parseInt(parser.getAttributeValue(null, "uppercase"));
                                    policy.mActivePasswordLowerCase = Integer.parseInt(parser.getAttributeValue(null, "lowercase"));
                                    policy.mActivePasswordLetters = Integer.parseInt(parser.getAttributeValue(null, "letters"));
                                    policy.mActivePasswordNumeric = Integer.parseInt(parser.getAttributeValue(null, "numeric"));
                                    policy.mActivePasswordSymbols = Integer.parseInt(parser.getAttributeValue(null, "symbols"));
                                    policy.mActivePasswordNonLetter = Integer.parseInt(parser.getAttributeValue(null, "nonletter"));
                                    XmlUtils.skipCurrentTag(parser);
                                } else {
                                    Slog.w(TAG, "Unknown tag: " + tag3);
                                    XmlUtils.skipCurrentTag(parser);
                                }
                            }
                        }
                        i = 1;
                    }
                }
            }
            throw new XmlPullParserException("Settings do not start with policies tag: found " + tag2);
            HwLog.e(TAG, "cannot close the stream.");
        } catch (NullPointerException e2) {
            Slog.w(TAG, "failed parsing " + file + " " + e2);
            if (0 != 0) {
                stream.close();
            }
        } catch (NumberFormatException e3) {
            Slog.w(TAG, "failed parsing " + file + " " + e3);
            if (0 != 0) {
                stream.close();
            }
        } catch (XmlPullParserException e4) {
            Slog.w(TAG, "failed parsing " + file + " " + e4);
            if (0 != 0) {
                stream.close();
            }
        } catch (FileNotFoundException e5) {
            if (0 != 0) {
                try {
                    stream.close();
                } catch (IOException e6) {
                }
            }
        } catch (IOException e7) {
            Slog.w(TAG, "failed parsing " + file + " " + e7);
            if (0 != 0) {
                stream.close();
            }
        } catch (IndexOutOfBoundsException e8) {
            Slog.w(TAG, "failed parsing " + file + " " + e8);
            if (0 != 0) {
                try {
                    stream.close();
                } catch (IOException e9) {
                    HwLog.e(TAG, "cannot close the stream.");
                }
            }
        } catch (Throwable th2) {
            th = th2;
            stream = null;
            if (stream != null) {
            }
            throw th;
        }
    }

    private boolean isPrivacyModeEnabled() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), PRIVACY_MODE_ON, 0) == 1 && isFeatrueSupported();
    }

    private static boolean isFeatrueSupported() {
        return SystemProperties.getBoolean("ro.config.hw_privacymode", false);
    }

    public void systemReady(int phase) {
        HwDevicePolicyManagerService.super.systemReady(phase);
        if (isFeatrueSupported()) {
            if (this.mHasFeature) {
                Slog.w(TAG, "systemReady");
                synchronized (getLockObject()) {
                    loadVisitorSettingsLocked(getVisitorUserData(0), 0);
                }
            } else {
                return;
            }
        }
        if (phase == 1000) {
            listenForUserSwitches();
        }
        if (phase == 550) {
            Log.i(TAG, "systemReady to setDpcInAELaunchableAndBackgroundRunnable phase:" + phase);
            setDpcInAELaunchableAndBackgroundRunnable(false);
        }
    }

    public boolean isActivePasswordSufficient(int userHandle, boolean parent) {
        if (!isPrivacyModeEnabled()) {
            return HwDevicePolicyManagerService.super.isActivePasswordSufficient(userHandle, parent);
        }
        boolean z = false;
        if (!HwDevicePolicyManagerService.super.isActivePasswordSufficient(userHandle, parent)) {
            return false;
        }
        Slog.w(TAG, "super is ActivePassword Sufficient");
        if (!this.mHasFeature) {
            return true;
        }
        synchronized (getLockObject()) {
            DeviceVisitorPolicyData policy = getVisitorUserData(userHandle);
            if (policy.mActivePasswordQuality >= getPasswordQuality(null, userHandle, parent)) {
                if (policy.mActivePasswordLength >= getPasswordMinimumLength(null, userHandle, parent)) {
                    if (policy.mActivePasswordQuality != 393216) {
                        return true;
                    }
                    if (policy.mActivePasswordUpperCase >= getPasswordMinimumUpperCase(null, userHandle, parent) && policy.mActivePasswordLowerCase >= getPasswordMinimumLowerCase(null, userHandle, parent) && policy.mActivePasswordLetters >= getPasswordMinimumLetters(null, userHandle, parent) && policy.mActivePasswordNumeric >= getPasswordMinimumNumeric(null, userHandle, parent) && policy.mActivePasswordSymbols >= getPasswordMinimumSymbols(null, userHandle, parent) && policy.mActivePasswordNonLetter >= getPasswordMinimumNonLetter(null, userHandle, parent)) {
                        z = true;
                    }
                    return z;
                }
            }
            return false;
        }
    }

    public void setActiveVisitorPasswordState(int quality, int length, int letters, int uppercase, int lowercase, int numbers, int symbols, int nonletter, int userHandle) {
        if (this.mHasFeature) {
            enforceFullCrossUsersPermission(userHandle);
            this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
            DeviceVisitorPolicyData p = getVisitorUserData(userHandle);
            validateQualityConstant(quality);
            synchronized (getLockObject()) {
                if (!(p.mActivePasswordQuality == quality && p.mActivePasswordLength == length && p.mFailedPasswordAttempts == 0 && p.mActivePasswordUpperCase == uppercase && p.mActivePasswordLowerCase == lowercase && p.mActivePasswordNumeric == numbers && p.mActivePasswordSymbols == symbols && p.mActivePasswordLetters == letters && p.mActivePasswordNonLetter == nonletter)) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        p.mActivePasswordQuality = quality;
                        p.mActivePasswordLength = length;
                        p.mActivePasswordLetters = letters;
                        p.mActivePasswordLowerCase = lowercase;
                        p.mActivePasswordUpperCase = uppercase;
                        p.mActivePasswordNumeric = numbers;
                        p.mActivePasswordSymbols = symbols;
                        p.mActivePasswordNonLetter = nonletter;
                        p.mFailedPasswordAttempts = 0;
                        saveVisitorSettingsLock(userHandle);
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setAllowSimplePassword(ComponentName who, boolean mode, int userHandle) {
        if (this.mHasFeature && isSimplePwdOpen) {
            HwLog.d(TAG, "setAllowSimplePassword mode =" + mode);
            synchronized (getLockObject()) {
                if (who != null) {
                    try {
                        ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0);
                        if (ap.allowSimplePassword != mode) {
                            ap.allowSimplePassword = mode;
                            saveSettingsLocked(userHandle);
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                } else {
                    throw new NullPointerException("ComponentName is null");
                }
            }
        }
    }

    public boolean getAllowSimplePassword(ComponentName who, int userHandle) {
        if (!this.mHasFeature || !isSimplePwdOpen) {
            return true;
        }
        synchronized (getLockObject()) {
            boolean isAllowSimplePassword = true;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                return admin != null ? admin.allowSimplePassword : true;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin2 = (ActiveAdmin) it.next();
                if (isAllowSimplePassword && isAllowSimplePassword != admin2.allowSimplePassword) {
                    isAllowSimplePassword = admin2.allowSimplePassword;
                }
            }
            HwLog.d(TAG, "getAllowSimplePassword mode =" + isAllowSimplePassword);
            return isAllowSimplePassword;
        }
    }

    /* access modifiers changed from: package-private */
    public void saveCurrentPwdStatus(boolean isCurrentPwdSimple, int userHandle) {
        if (this.mHasFeature && isSimplePwdOpen) {
            synchronized (getLockObject()) {
                getUserData(userHandle).mIsCurrentPwdSimple = isCurrentPwdSimple;
            }
        }
    }

    private void reponseSetActiveVisitorPasswordState(Parcel data) {
        if (data != null) {
            Slog.w(TAG, "TRANSACTION_SET_ACTIVE_VISITOR_PASSWORD_STATE");
            data.enforceInterface("com.android.internal.widget.ILockSettings");
            setActiveVisitorPasswordState(data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt());
        }
    }

    private void responseGetAllowSimplePassword(Parcel data, Parcel reply) {
        ComponentName arg0;
        if (data != null && reply != null) {
            data.enforceInterface(DPMDESCRIPTOR);
            if (data.readInt() != 0) {
                arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
            } else {
                arg0 = null;
            }
            boolean allowSimplePassword = getAllowSimplePassword(arg0, data.readInt());
            reply.writeNoException();
            reply.writeInt(allowSimplePassword ? 1 : 0);
        }
    }

    private void responseSetAllowSimplePassword(Parcel data, Parcel reply) {
        ComponentName arg0;
        if (data != null && reply != null) {
            data.enforceInterface(DPMDESCRIPTOR);
            if (data.readInt() != 0) {
                arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
            } else {
                arg0 = null;
            }
            setAllowSimplePassword(arg0, data.readInt() != 0, data.readInt());
            reply.writeNoException();
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (this.mProcessor.processTransaction(code, data, reply) || this.mProcessor.processTransactionWithPolicyName(code, data, reply)) {
            return true;
        }
        if (code != 1003) {
            switch (code) {
                case 7001:
                    if (isSimplePwdOpen) {
                        responseSetAllowSimplePassword(data, reply);
                        return true;
                    }
                    break;
                case 7002:
                    if (isSimplePwdOpen) {
                        responseGetAllowSimplePassword(data, reply);
                        return true;
                    }
                    break;
                case 7003:
                    if (isSimplePwdOpen) {
                        data.enforceInterface(DPMDESCRIPTOR);
                        saveCurrentPwdStatus(data.readInt() != 0, data.readInt());
                        reply.writeNoException();
                        return true;
                    }
                    break;
            }
            return HwDevicePolicyManagerService.super.onTransact(code, data, reply, flags);
        }
        reponseSetActiveVisitorPasswordState(data);
        reply.writeNoException();
        return true;
    }

    private void listenForUserSwitches() {
        try {
            ActivityManagerNative.getDefault().registerUserSwitchObserver(new SynchronousUserSwitchObserver() {
                /* class com.android.server.devicepolicy.HwDevicePolicyManagerService.AnonymousClass2 */

                public void onUserSwitching(int newUserId) throws RemoteException {
                }

                public void onUserSwitchComplete(int newUserId) throws RemoteException {
                    HwDevicePolicyManagerService.this.syncHwDeviceSettingsLocked(newUserId);
                }

                public void onForegroundProfileSwitch(int newProfileId) {
                }
            }, TAG);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to listen for user switching event", e);
        }
    }

    private void enforceHwCrossUserPermission(int userHandle) {
        enforceFullCrossUsersPermission(userHandle);
        if (userHandle != 0) {
            throw new IllegalArgumentException("Invalid userId " + userHandle + ",should be:" + 0);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0053, code lost:
        r0 = r7.mHwAdminCache;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0055, code lost:
        if (r0 == null) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0057, code lost:
        r0.syncHwAdminCache(0, isWifiDisabled(null, r10));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:?, code lost:
        return;
     */
    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setWifiDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_WIFI", "does not have wifi MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableWifi != disabled) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(DevSchedFeatureRT.WIFI_FEATURE);
                        if (!(wifiManager.isWifiEnabled() && disabled && !wifiManager.setWifiEnabled(false))) {
                            Binder.restoreCallingIdentity(callingId);
                            admin.disableWifi = disabled;
                            saveSettingsLocked(userHandle);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(callingId);
                    }
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isWifiDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                return getHwActiveAdmin(who, userHandle).disableWifi;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (admin.mHwActiveAdmin != null && admin.mHwActiveAdmin.disableWifi) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isBluetoothDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                return getHwActiveAdmin(who, userHandle).disableBluetooth;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (admin.mHwActiveAdmin != null && admin.mHwActiveAdmin.disableBluetooth) {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0055, code lost:
        r0 = r8.mHwAdminCache;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0057, code lost:
        if (r0 == null) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0059, code lost:
        r0.syncHwAdminCache(8, isBluetoothDisabled(null, r11));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:?, code lost:
        return;
     */
    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setBluetoothDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_BLUETOOTH", "does not have bluethooth MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableBluetooth != disabled) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        BluetoothAdapter bluetoothAdapter = ((BluetoothManager) this.mContext.getSystemService("bluetooth")).getAdapter();
                        if (!(bluetoothAdapter.isEnabled() && disabled) || bluetoothAdapter.disable()) {
                            Binder.restoreCallingIdentity(callingId);
                            admin.disableBluetooth = disabled;
                            saveSettingsLocked(userHandle);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(callingId);
                    }
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setWifiApDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_WIFI", "does not have Wifi AP MDM permission!");
        synchronized (getLockObject()) {
            enforceUserRestrictionPermission(who, "no_config_tethering", userHandle);
            AbsDevicePolicyManagerService.HwActiveAdmin ap = getHwActiveAdmin(who, userHandle);
            if (ap.disableWifiAp != disabled) {
                ap.disableWifiAp = disabled;
                saveSettingsLocked(userHandle);
            }
            hwSyncDeviceCapabilitiesLocked("no_config_tethering", userHandle);
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isWifiApDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableWifiAp;
                }
                return z;
            } else if (!this.mUserManager.hasUserRestriction("no_config_tethering", new UserHandle(userHandle))) {
                return false;
            } else {
                Iterator it = getUserData(userHandle).mAdminList.iterator();
                while (it.hasNext()) {
                    ActiveAdmin admin2 = (ActiveAdmin) it.next();
                    if (admin2.mHwActiveAdmin != null && admin2.mHwActiveAdmin.disableWifiAp) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setBootLoaderDisabled(ComponentName who, boolean disabled, int userHandle) {
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isBootLoaderDisabled(ComponentName who, int userHandle) {
        return false;
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setUSBDataDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_USB", "does not have USB MDM permission!");
        synchronized (getLockObject()) {
            enforceUserRestrictionPermission(who, "no_usb_file_transfer", userHandle);
            AbsDevicePolicyManagerService.HwActiveAdmin ap = getHwActiveAdmin(who, userHandle);
            if (ap.disableUSBData != disabled) {
                ap.disableUSBData = disabled;
                saveSettingsLocked(userHandle);
            }
            hwSyncDeviceCapabilitiesLocked("no_usb_file_transfer", userHandle);
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isUSBDataDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin ap = getHwActiveAdmin(who, userHandle);
                if (ap != null) {
                    z = ap.disableUSBData;
                }
                return z;
            } else if (!this.mUserManager.hasUserRestriction("no_usb_file_transfer", new UserHandle(userHandle))) {
                return false;
            } else {
                Iterator it = getUserData(userHandle).mAdminList.iterator();
                while (it.hasNext()) {
                    ActiveAdmin admin = (ActiveAdmin) it.next();
                    if (admin.mHwActiveAdmin != null && admin.mHwActiveAdmin.disableUSBData) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setExternalStorageDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_SDCARD", "does not have SDCARD MDM permission!");
        synchronized (getLockObject()) {
            enforceUserRestrictionPermission(who, "no_physical_media", userHandle);
            AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
            if (admin.disableExternalStorage != disabled) {
                admin.disableExternalStorage = disabled;
                saveSettingsLocked(userHandle);
            }
            hwSyncDeviceCapabilitiesLocked("no_physical_media", userHandle);
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isExternalStorageDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableExternalStorage;
                }
                return z;
            } else if (!this.mUserManager.hasUserRestriction("no_physical_media", new UserHandle(userHandle))) {
                return false;
            } else {
                Iterator it = getUserData(userHandle).mAdminList.iterator();
                while (it.hasNext()) {
                    ActiveAdmin admin2 = (ActiveAdmin) it.next();
                    if (admin2.mHwActiveAdmin != null && admin2.mHwActiveAdmin.disableExternalStorage) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /* JADX INFO: finally extract failed */
    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setNFCDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_NFC", "does not have NFC MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (disabled != admin.disableNFC) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this.mContext);
                        if (nfcAdapter != null) {
                            boolean isNfcEnabled = nfcAdapter.isEnabled();
                            if (disabled && isNfcEnabled) {
                                nfcAdapter.disable();
                            }
                        }
                        Binder.restoreCallingIdentity(callingId);
                        admin.disableNFC = disabled;
                        saveSettingsLocked(userHandle);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(callingId);
                        throw th;
                    }
                }
            } else {
                throw new NullPointerException("ComponentName is null");
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isNFCDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableNFC;
                }
                return z;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin2 = (ActiveAdmin) it.next();
                if (admin2.mHwActiveAdmin != null && admin2.mHwActiveAdmin.disableNFC) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setDataConnectivityDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_CONNECTIVITY", "Does not hava data connectivity MDM permission.");
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableDataConnectivity != disabled) {
                    admin.disableDataConnectivity = disabled;
                    saveSettingsLocked(userHandle);
                }
                if (disabled) {
                    try {
                        ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).disableDataConnectivity();
                    } catch (RemoteException e) {
                        HwLog.e(TAG, "Can not calling the remote function to set data enabled!");
                    }
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isDataConnectivityDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableDataConnectivity;
                }
                return z;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin2 = (ActiveAdmin) it.next();
                if (admin2.mHwActiveAdmin != null && admin2.mHwActiveAdmin.disableDataConnectivity) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setVoiceDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_PHONE", "Does not hava phone disable MDM permission.");
        synchronized (getLockObject()) {
            enforceUserRestrictionPermission(who, "no_outgoing_calls", userHandle);
            AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
            if (admin.disableVoice != disabled) {
                admin.disableVoice = disabled;
                saveSettingsLocked(userHandle);
            }
            hwSyncDeviceCapabilitiesLocked("no_outgoing_calls", userHandle);
        }
        HwAdminCache hwAdminCache = this.mHwAdminCache;
        if (hwAdminCache != null) {
            hwAdminCache.syncHwAdminCache(1, isVoiceDisabled(null, userHandle));
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isVoiceDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableVoice;
                }
                return z;
            } else if (!this.mUserManager.hasUserRestriction("no_outgoing_calls", new UserHandle(userHandle))) {
                return false;
            } else {
                Iterator it = getUserData(userHandle).mAdminList.iterator();
                while (it.hasNext()) {
                    ActiveAdmin admin2 = (ActiveAdmin) it.next();
                    if (admin2.mHwActiveAdmin != null && admin2.mHwActiveAdmin.disableVoice) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setSMSDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_MMS", "Does not hava SMS disable MDM permission.");
        synchronized (getLockObject()) {
            enforceUserRestrictionPermission(who, "no_sms", userHandle);
            AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
            if (disabled != admin.disableSMS) {
                admin.disableSMS = disabled;
                saveSettingsLocked(userHandle);
            }
            hwSyncDeviceCapabilitiesLocked("no_sms", userHandle);
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isSMSDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableSMS;
                }
                return z;
            } else if (!this.mUserManager.hasUserRestriction("no_sms", new UserHandle(userHandle))) {
                return false;
            } else {
                Iterator it = getUserData(userHandle).mAdminList.iterator();
                while (it.hasNext()) {
                    ActiveAdmin admin2 = (ActiveAdmin) it.next();
                    if (admin2.mHwActiveAdmin != null && admin2.mHwActiveAdmin.disableSMS) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setStatusBarExpandPanelDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have DEVICE MANAGER permission!");
        synchronized (getLockObject()) {
            AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
            if (admin.disableStatusBarExpandPanel != disabled) {
                if (!disabled || setStatusBarPanelDisabledInternal(userHandle)) {
                    admin.disableStatusBarExpandPanel = disabled;
                    saveSettingsLocked(userHandle);
                    if (!disabled) {
                        setStatusBarPanelEnableInternal(false, userHandle);
                    }
                } else {
                    Log.w(TAG, "cannot set statusBar disabled");
                }
            }
        }
    }

    private boolean setStatusBarPanelDisabledInternal(int userHandle) {
        long callingId = Binder.clearCallingIdentity();
        try {
            StatusBarManager statusBar = (StatusBarManager) this.mContext.getSystemService("statusbar");
            if (statusBar == null) {
                Log.w(TAG, "statusBar is null");
                Binder.restoreCallingIdentity(callingId);
                return false;
            }
            statusBar.disable(STATUS_BAR_DISABLE_MASK);
            Binder.restoreCallingIdentity(callingId);
            return true;
        } catch (ClassCastException e) {
            Log.e(TAG, "failed to set statusBar disabled. CalssCastException");
        } catch (Exception e2) {
            Log.e(TAG, "failed to set statusBar disabled.");
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(callingId);
            throw th;
        }
        Binder.restoreCallingIdentity(callingId);
        return false;
    }

    private boolean setStatusBarPanelEnableInternal(boolean forceEnable, int userHandle) {
        long callingId = Binder.clearCallingIdentity();
        try {
            StatusBarManager statusBar = (StatusBarManager) this.mContext.getSystemService("statusbar");
            if (statusBar == null) {
                Log.w(TAG, "statusBar is null");
                Binder.restoreCallingIdentity(callingId);
                return false;
            }
            if (forceEnable) {
                statusBar.disable(0);
            } else if (!isStatusBarExpandPanelDisabled(null, userHandle)) {
                statusBar.disable(0);
            }
            Binder.restoreCallingIdentity(callingId);
            return true;
        } catch (ClassCastException e) {
            Log.e(TAG, "failed to set statusBar enabled. ClassCastException");
        } catch (Exception e2) {
            Log.e(TAG, "failed to set statusBar enabled.");
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(callingId);
            throw th;
        }
        Binder.restoreCallingIdentity(callingId);
        return false;
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isStatusBarExpandPanelDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableStatusBarExpandPanel;
                }
                return z;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin2 = (ActiveAdmin) it.next();
                if (admin2.mHwActiveAdmin != null && admin2.mHwActiveAdmin.disableStatusBarExpandPanel) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void hangupCalling(ComponentName who, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_PHONE", "Does not hava hangup calling permission.");
        synchronized (getLockObject()) {
            if (who == null) {
                throw new IllegalArgumentException("ComponentName is null");
            } else if (getHwActiveAdmin(who, userHandle) != null) {
                long ident = Binder.clearCallingIdentity();
                try {
                    TelecomManager.from(this.mContext).endCall();
                    UiThread.getHandler().post(new Runnable() {
                        /* class com.android.server.devicepolicy.HwDevicePolicyManagerService.AnonymousClass3 */

                        public void run() {
                            if (HwDevicePolicyManagerService.this.mErrorDialog != null) {
                                HwDevicePolicyManagerService.this.mErrorDialog.dismiss();
                                AlertDialog unused = HwDevicePolicyManagerService.this.mErrorDialog = null;
                            }
                            HwDevicePolicyManagerService hwDevicePolicyManagerService = HwDevicePolicyManagerService.this;
                            AlertDialog unused2 = hwDevicePolicyManagerService.mErrorDialog = new AlertDialog.Builder(hwDevicePolicyManagerService.mContext, 33947691).setMessage(33686011).setPositiveButton(33686081, new DialogInterface.OnClickListener() {
                                /* class com.android.server.devicepolicy.HwDevicePolicyManagerService.AnonymousClass3.AnonymousClass1 */

                                public void onClick(DialogInterface dialog, int which) {
                                    HwDevicePolicyManagerService.this.mErrorDialog.dismiss();
                                }
                            }).setCancelable(true).create();
                            HwDevicePolicyManagerService.this.mErrorDialog.getWindow().setType(2003);
                            HwDevicePolicyManagerService.this.mErrorDialog.show();
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x008d, code lost:
        if (0 != 0) goto L_0x00b7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x00a2, code lost:
        if (1 == 0) goto L_0x00b8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x00b5, code lost:
        if (1 == 0) goto L_0x00b8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x00b7, code lost:
        r5 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x00b8, code lost:
        if (r5 == null) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x00ba, code lost:
        r5.commit(new com.android.server.devicepolicy.HwDevicePolicyManagerService.LocalIntentReceiver(r19, r21.getPackageName(), r8).getIntentSender());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:?, code lost:
        return;
     */
    private void commitInstall(Context context, ComponentName who, Uri uri) {
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(1);
        params.installReason = 9;
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.Session session = null;
        session = null;
        int sessionId = 0;
        sessionId = 0;
        sessionId = 0;
        sessionId = 0;
        try {
            sessionId = packageInstaller.createSession(params);
            session = packageInstaller.openSession(sessionId);
            InputStream in = this.mContext.getContentResolver().openInputStream(uri);
            if (in == null) {
                closeIO(session);
                closeIO(in);
                closeIO(null);
                closeIO(session);
                if (0 == 0) {
                    return;
                }
                return;
            }
            byte[] buffer = new byte[HighBitsCompModeID.MODE_COLOR_ENHANCE];
            OutputStream out = session.openWrite(who.getPackageName(), 0, (long) in.available());
            if (out == null) {
                closeIO(in);
                closeIO(session);
                closeIO(in);
                closeIO(out);
                closeIO(session);
                if (0 == 0) {
                    return;
                }
                return;
            }
            while (true) {
                int length = in.read(buffer);
                if (length == -1) {
                    break;
                }
                out.write(buffer, 0, length);
            }
            session.fsync(out);
            closeIO(in);
            closeIO(out);
            closeIO(session);
        } catch (IOException e) {
            Log.w(TAG, "commitInstall IOException");
            closeIO(null);
            closeIO(null);
            closeIO(null);
        } catch (SecurityException e2) {
            Log.w(TAG, "commitInstall SecurityException");
            closeIO(null);
            closeIO(null);
            closeIO(null);
        } catch (Throwable th) {
            closeIO(null);
            closeIO(null);
            closeIO(null);
            if (0 != 0) {
            }
            throw th;
        }
    }

    private void closeIO(Closeable close) {
        if (close != null) {
            try {
                close.close();
            } catch (IOException e) {
                Log.w(TAG, "closeIO IOException when try to close");
            }
        }
    }

    private class LocalIntentReceiver {
        private IIntentSender.Stub mLocalSender = new IIntentSender.Stub() {
            /* class com.android.server.devicepolicy.HwDevicePolicyManagerService.LocalIntentReceiver.AnonymousClass1 */

            public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
                String pkgName = intent.getStringExtra("android.content.pm.extra.PACKAGE_NAME");
                int installStatus = intent.getIntExtra("android.content.pm.extra.STATUS", 1);
                LocalIntentReceiver.this.sendMdmPackageInstallBroadcast(pkgName, installStatus);
                if (installStatus == 0 && LocalIntentReceiver.this.reportSessionId == intent.getIntExtra("android.content.pm.extra.SESSION_ID", 0)) {
                    BdReportUtils.reportInstallPkgData(LocalIntentReceiver.this.reportOwnerPkgName, pkgName, HwDevicePolicyManagerService.this.mContext);
                }
            }
        };
        /* access modifiers changed from: private */
        public String reportOwnerPkgName;
        /* access modifiers changed from: private */
        public int reportSessionId;

        /* access modifiers changed from: private */
        public void sendMdmPackageInstallBroadcast(String pkgName, int installStatus) {
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putInt("returnCode", installStatus);
            bundle.putString("packageName", pkgName);
            intent.putExtras(bundle);
            intent.setPackage(this.reportOwnerPkgName);
            intent.setAction("com.huawei.intent.action.InstallMdmPackage");
            HwDevicePolicyManagerService.this.mContext.sendBroadcast(intent);
        }

        LocalIntentReceiver(String ownerPkgName, int sessionId) {
            this.reportOwnerPkgName = ownerPkgName;
            this.reportSessionId = sessionId;
        }

        public IntentSender getIntentSender() {
            return new IntentSender(this.mLocalSender);
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void installPackage(ComponentName who, String packagePath, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "Does not hava application management permission.");
        synchronized (getLockObject()) {
            if (who == null) {
                throw new IllegalArgumentException("ComponentName is null");
            } else if (getHwActiveAdmin(who, userHandle) != null) {
                if (!TextUtils.isEmpty(packagePath)) {
                    Uri uri = Uri.parse(packagePath);
                    if (uri == null || !"content".equalsIgnoreCase(uri.getScheme())) {
                        installPackage(packagePath, who.getPackageName());
                    } else {
                        long callingId = Binder.clearCallingIdentity();
                        try {
                            commitInstall(this.mContext, who, uri);
                        } finally {
                            Binder.restoreCallingIdentity(callingId);
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Install package path is empty");
                }
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void uninstallPackage(ComponentName who, String packageName, boolean keepData, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "Does not hava application management permission.");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageName(packageName)) {
            synchronized (getLockObject()) {
                if (who == null) {
                    throw new IllegalArgumentException("ComponentName is null");
                } else if (getHwActiveAdmin(who, userHandle) != null) {
                    uninstallPackage(packageName, keepData);
                }
            }
            BdReportUtils.reportUninstallPkgData(who.getPackageName(), packageName, this.mContext);
            return;
        }
        throw new IllegalArgumentException("packageName:" + packageName + " is invalid.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void clearPackageData(ComponentName who, String packageName, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have wifi MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageName(packageName)) {
            synchronized (getLockObject()) {
                if (who == null) {
                    throw new IllegalArgumentException("ComponentName is null");
                } else if (!TextUtils.isEmpty(packageName)) {
                    enforceCheckNotActiveAdminApp(packageName, userHandle);
                    if (getHwActiveAdmin(who, userHandle) != null) {
                        long id = Binder.clearCallingIdentity();
                        try {
                            ((ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG)).clearApplicationUserData(packageName, null);
                        } finally {
                            Binder.restoreCallingIdentity(id);
                        }
                    }
                } else {
                    throw new IllegalArgumentException("packageNames is null or empty");
                }
            }
            return;
        }
        throw new IllegalArgumentException("packageName:" + packageName + " is invalid.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void enableInstallPackage(ComponentName who, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have wifi MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    admin.disableInstallSource = false;
                    admin.installSourceWhitelist = null;
                }
                saveSettingsLocked(userHandle);
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        HwAdminCache hwAdminCache = this.mHwAdminCache;
        if (hwAdminCache != null) {
            hwAdminCache.syncHwAdminCache(2, isInstallSourceDisabled(null, userHandle));
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void disableInstallSource(ComponentName who, List<String> whitelist, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have wifi MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(whitelist)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (whitelist != null) {
                        if (!whitelist.isEmpty()) {
                            AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                            admin.disableInstallSource = true;
                            if (admin.installSourceWhitelist == null) {
                                admin.installSourceWhitelist = new ArrayList();
                            }
                            HwDevicePolicyManagerServiceUtil.isOverLimit(admin.installSourceWhitelist, whitelist);
                            HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(admin.installSourceWhitelist, whitelist);
                            saveSettingsLocked(userHandle);
                        }
                    }
                    throw new IllegalArgumentException("packageNames is null or empty");
                }
                throw new IllegalArgumentException("ComponentName is null");
            }
            HwAdminCache hwAdminCache = this.mHwAdminCache;
            if (hwAdminCache != null) {
                hwAdminCache.syncHwAdminCache(2, isInstallSourceDisabled(null, userHandle));
                this.mHwAdminCache.syncHwAdminCache(3, getInstallPackageSourceWhiteList(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("packageName:" + whitelist + " is invalid.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isInstallSourceDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableInstallSource;
                }
                return z;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin2 = (ActiveAdmin) it.next();
                if (admin2.mHwActiveAdmin != null && admin2.mHwActiveAdmin.disableInstallSource) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public List<String> getInstallPackageSourceWhiteList(ComponentName who, int userHandle) {
        List<String> list;
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.installSourceWhitelist != null) {
                    if (!admin.installSourceWhitelist.isEmpty()) {
                        list = admin.installSourceWhitelist;
                        return list;
                    }
                }
                list = null;
                return list;
            }
            DevicePolicyData policy = getUserData(userHandle);
            ArrayList<String> whiteList = new ArrayList<>();
            Iterator it = policy.mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin2 = (ActiveAdmin) it.next();
                if (admin2.mHwActiveAdmin != null) {
                    HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(whiteList, admin2.mHwActiveAdmin.installSourceWhitelist);
                }
            }
            return whiteList;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void addPersistentApp(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "Does not hava manager app MDM permission.");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                            if (admin.persistentAppList == null) {
                                admin.persistentAppList = new ArrayList();
                            }
                            HwDevicePolicyManagerServiceUtil.isOverLimit(getPersistentApp(null, userHandle), packageNames, 10);
                            filterOutSystemAppList(packageNames, userHandle);
                            HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(admin.persistentAppList, packageNames);
                            saveSettingsLocked(userHandle);
                            sendPersistentAppToIAware(userHandle);
                        }
                    }
                    throw new IllegalArgumentException("packageNames is null or empty");
                }
                throw new IllegalArgumentException("ComponentName is null");
            }
            HwAdminCache hwAdminCache = this.mHwAdminCache;
            if (hwAdminCache != null) {
                hwAdminCache.syncHwAdminCache(4, getPersistentApp(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("packageName:" + packageNames + " is invalid.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void removePersistentApp(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "Does not hava manager app MDM permission.");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            HwDevicePolicyManagerServiceUtil.removeItemsFromList(getHwActiveAdmin(who, userHandle).persistentAppList, packageNames);
                            saveSettingsLocked(userHandle);
                            sendPersistentAppToIAware(userHandle);
                        }
                    }
                    throw new IllegalArgumentException("packageNames is null or empty");
                }
                throw new IllegalArgumentException("ComponentName is null");
            }
            HwAdminCache hwAdminCache = this.mHwAdminCache;
            if (hwAdminCache != null) {
                hwAdminCache.syncHwAdminCache(4, getPersistentApp(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("packageName:" + packageNames + " is invalid.");
    }

    private void sendPersistentAppToIAware(int userHandle) {
        List<String> persistAppList = getPersistentApp(null, userHandle);
        if (persistAppList == null || persistAppList.size() <= 0) {
            ProcessCleaner.getInstance(this.mContext).removeProtectedListFromMDM();
            Slog.d(TAG, "removeProtectedListFromMDM for user " + userHandle);
            return;
        }
        ProcessCleaner.getInstance(this.mContext).setProtectedListFromMDM(persistAppList);
        Slog.d(TAG, "setProtectedListFromMDM for user " + userHandle + AwarenessInnerConstants.COLON_KEY + persistAppList);
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public List<String> getPersistentApp(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            List<String> list = null;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.persistentAppList != null) {
                    if (!admin.persistentAppList.isEmpty()) {
                        list = admin.persistentAppList;
                    }
                }
                return list;
            }
            DevicePolicyData policy = getUserData(userHandle);
            ArrayList<String> totalList = new ArrayList<>();
            Iterator it = policy.mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin2 = (ActiveAdmin) it.next();
                if (admin2.mHwActiveAdmin != null) {
                    HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(totalList, admin2.mHwActiveAdmin.persistentAppList);
                }
            }
            if (!totalList.isEmpty()) {
                list = totalList;
            }
            return list;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void addDisallowedRunningApp(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "Does not hava manager app MDM permission.");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                            if (admin.disallowedRunningAppList == null) {
                                admin.disallowedRunningAppList = new ArrayList();
                            }
                            HwDevicePolicyManagerServiceUtil.isOverLimit(admin.disallowedRunningAppList, packageNames);
                            HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(admin.disallowedRunningAppList, packageNames);
                            saveSettingsLocked(userHandle);
                            for (String packageName : packageNames) {
                                killApplicationInner(packageName);
                            }
                        }
                    }
                    throw new IllegalArgumentException("packageNames is null or empty");
                }
                throw new IllegalArgumentException("ComponentName is null");
            }
            HwAdminCache hwAdminCache = this.mHwAdminCache;
            if (hwAdminCache != null) {
                hwAdminCache.syncHwAdminCache(5, getDisallowedRunningApp(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("packageName:" + packageNames + " is invalid.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void removeDisallowedRunningApp(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "Does not hava manager app MDM permission.");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            HwDevicePolicyManagerServiceUtil.removeItemsFromList(getHwActiveAdmin(who, userHandle).disallowedRunningAppList, packageNames);
                            saveSettingsLocked(userHandle);
                        }
                    }
                    throw new IllegalArgumentException("packageNames is null or empty");
                }
                throw new IllegalArgumentException("ComponentName is null");
            }
            HwAdminCache hwAdminCache = this.mHwAdminCache;
            if (hwAdminCache != null) {
                hwAdminCache.syncHwAdminCache(5, getDisallowedRunningApp(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("packageName:" + packageNames + " is invalid.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public List<String> getDisallowedRunningApp(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            List<String> list = null;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disallowedRunningAppList != null) {
                    if (!admin.disallowedRunningAppList.isEmpty()) {
                        list = admin.disallowedRunningAppList;
                    }
                }
                return list;
            }
            DevicePolicyData policy = getUserData(userHandle);
            ArrayList<String> totalList = new ArrayList<>();
            Iterator it = policy.mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin2 = (ActiveAdmin) it.next();
                if (admin2.mHwActiveAdmin != null) {
                    HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(totalList, admin2.mHwActiveAdmin.disallowedRunningAppList);
                }
            }
            if (!totalList.isEmpty()) {
                list = totalList;
            }
            return list;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void addInstallPackageWhiteList(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app_management MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                            if (admin.installPackageWhitelist == null) {
                                admin.installPackageWhitelist = new ArrayList();
                            }
                            HwDevicePolicyManagerServiceUtil.isOverLimit(admin.installPackageWhitelist, packageNames);
                            HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(admin.installPackageWhitelist, packageNames);
                            saveSettingsLocked(userHandle);
                        }
                    }
                    throw new IllegalArgumentException("packageNames is null or empty");
                }
                throw new IllegalArgumentException("ComponentName is null");
            }
            HwAdminCache hwAdminCache = this.mHwAdminCache;
            if (hwAdminCache != null) {
                hwAdminCache.syncHwAdminCache(6, getInstallPackageWhiteList(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("packageName:" + packageNames + " is invalid.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void removeInstallPackageWhiteList(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app_management MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            HwDevicePolicyManagerServiceUtil.removeItemsFromList(getHwActiveAdmin(who, userHandle).installPackageWhitelist, packageNames);
                            saveSettingsLocked(userHandle);
                        }
                    }
                    throw new IllegalArgumentException("packageNames is null or empty");
                }
                throw new IllegalArgumentException("ComponentName is null");
            }
            HwAdminCache hwAdminCache = this.mHwAdminCache;
            if (hwAdminCache != null) {
                hwAdminCache.syncHwAdminCache(6, getInstallPackageWhiteList(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("packageName:" + packageNames + " is invalid.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public List<String> getInstallPackageWhiteList(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            List<String> list = null;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.installPackageWhitelist != null) {
                    if (!admin.installPackageWhitelist.isEmpty()) {
                        list = admin.installPackageWhitelist;
                    }
                }
                return list;
            }
            DevicePolicyData policy = getUserData(userHandle);
            ArrayList<String> whitelist = new ArrayList<>();
            Iterator it = policy.mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin2 = (ActiveAdmin) it.next();
                if (admin2.mHwActiveAdmin != null) {
                    HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(whitelist, admin2.mHwActiveAdmin.installPackageWhitelist);
                }
            }
            if (!whitelist.isEmpty()) {
                list = whitelist;
            }
            return list;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void addDisallowedUninstallPackages(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app_management MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                            if (admin.disallowedUninstallPackageList == null) {
                                admin.disallowedUninstallPackageList = new ArrayList();
                            }
                            HwDevicePolicyManagerServiceUtil.isOverLimit(admin.disallowedUninstallPackageList, packageNames);
                            filterOutSystemAppList(packageNames, userHandle);
                            HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(admin.disallowedUninstallPackageList, packageNames);
                            saveSettingsLocked(userHandle);
                        }
                    }
                    throw new IllegalArgumentException("packageNames is null or empty");
                }
                throw new IllegalArgumentException("ComponentName is null");
            }
            HwAdminCache hwAdminCache = this.mHwAdminCache;
            if (hwAdminCache != null) {
                hwAdminCache.syncHwAdminCache(7, getDisallowedUninstallPackageList(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("packageName:" + packageNames + " is invalid.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void removeDisallowedUninstallPackages(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app_management MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            HwDevicePolicyManagerServiceUtil.removeItemsFromList(getHwActiveAdmin(who, userHandle).disallowedUninstallPackageList, packageNames);
                            saveSettingsLocked(userHandle);
                        }
                    }
                    throw new IllegalArgumentException("packageNames is null or empty");
                }
                throw new IllegalArgumentException("ComponentName is null");
            }
            HwAdminCache hwAdminCache = this.mHwAdminCache;
            if (hwAdminCache != null) {
                hwAdminCache.syncHwAdminCache(7, getDisallowedUninstallPackageList(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("packageName:" + packageNames + " is invalid.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public List<String> getDisallowedUninstallPackageList(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            List<String> list = null;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disallowedUninstallPackageList != null) {
                    if (!admin.disallowedUninstallPackageList.isEmpty()) {
                        list = admin.disallowedUninstallPackageList;
                    }
                }
                return list;
            }
            DevicePolicyData policy = getUserData(userHandle);
            ArrayList<String> blacklist = new ArrayList<>();
            Iterator it = policy.mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin2 = (ActiveAdmin) it.next();
                if (admin2.mHwActiveAdmin != null) {
                    HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(blacklist, admin2.mHwActiveAdmin.disallowedUninstallPackageList);
                }
            }
            if (!blacklist.isEmpty()) {
                list = blacklist;
            }
            return list;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void addDisabledDeactivateMdmPackages(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app_management MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                            if (admin.disabledDeactiveMdmPackagesList == null) {
                                admin.disabledDeactiveMdmPackagesList = new ArrayList();
                            }
                            HwDevicePolicyManagerServiceUtil.isOverLimit(admin.disabledDeactiveMdmPackagesList, packageNames);
                            HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(admin.disabledDeactiveMdmPackagesList, packageNames);
                            saveSettingsLocked(userHandle);
                        }
                    }
                    throw new IllegalArgumentException("packageNames is null or empty");
                }
                throw new IllegalArgumentException("ComponentName is null");
            }
            HwAdminCache hwAdminCache = this.mHwAdminCache;
            if (hwAdminCache != null) {
                hwAdminCache.syncHwAdminCache(18, getDisabledDeactivateMdmPackageList(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("packageName:" + packageNames + " is invalid.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void removeDisabledDeactivateMdmPackages(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app_management MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            HwDevicePolicyManagerServiceUtil.removeItemsFromList(getHwActiveAdmin(who, userHandle).disabledDeactiveMdmPackagesList, packageNames);
                            saveSettingsLocked(userHandle);
                        }
                    }
                    throw new IllegalArgumentException("packageNames is null or empty");
                }
                throw new IllegalArgumentException("ComponentName is null");
            }
            HwAdminCache hwAdminCache = this.mHwAdminCache;
            if (hwAdminCache != null) {
                hwAdminCache.syncHwAdminCache(18, getDisabledDeactivateMdmPackageList(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("packageName:" + packageNames + " is invalid.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public List<String> getDisabledDeactivateMdmPackageList(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            List<String> list = null;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disabledDeactiveMdmPackagesList != null) {
                    if (!admin.disabledDeactiveMdmPackagesList.isEmpty()) {
                        list = admin.disabledDeactiveMdmPackagesList;
                    }
                }
                return list;
            }
            DevicePolicyData policy = getUserData(userHandle);
            ArrayList<String> blacklist = new ArrayList<>();
            Iterator it = policy.mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin2 = (ActiveAdmin) it.next();
                if (admin2.mHwActiveAdmin != null) {
                    HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(blacklist, admin2.mHwActiveAdmin.disabledDeactiveMdmPackagesList);
                }
            }
            if (!blacklist.isEmpty()) {
                list = blacklist;
            }
            return list;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void killApplicationProcess(ComponentName who, String packageName, int userHandle) {
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageName(packageName)) {
            enforceHwCrossUserPermission(userHandle);
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "Does not hava application management permission.");
            synchronized (getLockObject()) {
                if (who != null) {
                    try {
                        if (TextUtils.isEmpty(packageName)) {
                            throw new IllegalArgumentException("Package name is empty");
                        } else if (!packageName.equals(who.getPackageName())) {
                            enforceCheckNotActiveAdminApp(packageName, userHandle);
                            if (getHwActiveAdmin(who, userHandle) != null) {
                                killApplicationInner(packageName);
                            }
                        } else {
                            throw new IllegalArgumentException("Can not kill the caller application");
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                } else {
                    throw new IllegalArgumentException("ComponentName is null");
                }
            }
            return;
        }
        throw new IllegalArgumentException("packageName:" + packageName + " is invalid.");
    }

    private void killApplicationInner(String packageName) {
        long ident = Binder.clearCallingIdentity();
        try {
            ActivityManager am = (ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
            Iterator<ActivityManager.RunningTaskInfo> it = am.getRunningTasks(10000).iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ActivityManager.RunningTaskInfo ti = it.next();
                if (packageName.equals(ti.baseActivity.getPackageName())) {
                    try {
                        ActivityManager.getService().removeTask(ti.id);
                    } catch (RemoteException e) {
                        Log.e(TAG, "killApplicationInner exception is " + e.getMessage());
                    }
                    am.forceStopPackage(packageName);
                    break;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void shutdownOrRebootDevice(int code, ComponentName who, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                getHwActiveAdmin(who, userHandle);
                long callingId = Binder.clearCallingIdentity();
                try {
                    IPowerManager power = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
                    if (power != null) {
                        if (code == 1501) {
                            power.shutdown(false, (String) null, false);
                        } else if (code == 1502) {
                            power.reboot(false, (String) null, false);
                        }
                        Binder.restoreCallingIdentity(callingId);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "exception is " + e.getMessage());
                } finally {
                    Binder.restoreCallingIdentity(callingId);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void configExchangeMailProvider(ComponentName who, Bundle para, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_EMAIL", "does not have EMAIL MDM permission!");
        synchronized (getLockObject()) {
            if (who == null || para == null) {
                throw new IllegalArgumentException("ComponentName or para is null");
            } else if (HwDevicePolicyManagerServiceUtil.isValidExchangeParameter(para)) {
                AbsDevicePolicyManagerService.HwActiveAdmin ap = getHwActiveAdmin(who, userHandle);
                if (ap.mailProviderlist == null) {
                    ap.mailProviderlist = new ArrayList();
                    ap.mailProviderlist.add(para);
                    saveSettingsLocked(userHandle);
                } else if (ap.mailProviderlist.size() + 1 <= 20) {
                    boolean isAlready = false;
                    Bundle provider = null;
                    Iterator it = ap.mailProviderlist.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        Bundle each = (Bundle) it.next();
                        if (HwDevicePolicyManagerServiceUtil.matchProvider(para.getString("domain"), each.getString("domain"))) {
                            isAlready = true;
                            provider = each;
                            break;
                        }
                    }
                    if (isAlready && provider != null) {
                        ap.mailProviderlist.remove(provider);
                    }
                    ap.mailProviderlist.add(para);
                    saveSettingsLocked(userHandle);
                } else {
                    throw new IllegalArgumentException("already exceeds max number.");
                }
            } else {
                throw new IllegalArgumentException("some paremeter is null");
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public Bundle getMailProviderForDomain(ComponentName who, String domain, int userHandle) {
        Bundle bundle = null;
        if (userHandle != 0) {
            return null;
        }
        if (!TextUtils.isEmpty(domain)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                    if (admin.mailProviderlist == null) {
                        return null;
                    }
                    boolean isMatched = false;
                    Bundle retProvider = null;
                    Iterator it = admin.mailProviderlist.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        Bundle provider = (Bundle) it.next();
                        isMatched = HwDevicePolicyManagerServiceUtil.matchProvider(domain, provider.getString("domain"));
                        if (isMatched) {
                            retProvider = provider;
                            break;
                        }
                    }
                    if (isMatched) {
                        bundle = retProvider;
                    }
                    return bundle;
                }
                Iterator it2 = getUserData(userHandle).mAdminList.iterator();
                while (it2.hasNext()) {
                    ActiveAdmin admin2 = (ActiveAdmin) it2.next();
                    if (!(admin2.mHwActiveAdmin == null || admin2.mHwActiveAdmin.mailProviderlist == null)) {
                        for (Bundle provider2 : admin2.mHwActiveAdmin.mailProviderlist) {
                            if (HwDevicePolicyManagerServiceUtil.matchProvider(domain, provider2.getString("domain"))) {
                                return provider2;
                            }
                        }
                        continue;
                    }
                }
                return null;
            }
        }
        throw new IllegalArgumentException("domain is empty.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isRooted(ComponentName who, int userHandle) {
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                try {
                    getHwActiveAdmin(who, userHandle);
                    String currentState = SystemProperties.get("persist.sys.root.status");
                    if (!TextUtils.isEmpty(currentState)) {
                        if ("0".equals(currentState)) {
                            return false;
                        }
                    }
                    return true;
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setSafeModeDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have DEVICE MANAGER permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableSafeMode != disabled) {
                    admin.disableSafeMode = disabled;
                    saveSettingsLocked(userHandle);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        syncHwAdminSafeModeCache(userHandle);
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isSafeModeDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                return getHwActiveAdmin(who, userHandle).disableSafeMode;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (admin.mHwActiveAdmin != null && admin.mHwActiveAdmin.disableSafeMode) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setAdbDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_USB", "does not have MDM_USB permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableAdb != disabled) {
                    admin.disableAdb = disabled;
                    saveSettingsLocked(userHandle);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        HwAdminCache hwAdminCache = this.mHwAdminCache;
        if (hwAdminCache != null) {
            hwAdminCache.syncHwAdminCache(11, isAdbDisabled(null, userHandle));
        }
        long identityToken = Binder.clearCallingIdentity();
        if (disabled) {
            applyAdbDisabled();
        }
        Binder.restoreCallingIdentity(identityToken);
    }

    private void applyAdbDisabled() {
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled", 0) > 0) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "adb_enabled", 0);
            return;
        }
        UsbManager usbManager = (UsbManager) this.mContext.getSystemService(USB_STORAGE);
        if (usbManager == null) {
            Slog.e(TAG, "usbManager is null, return!!");
        } else {
            usbManager.setCurrentFunctions(usbManager.getCurrentFunctions());
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isAdbDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                return getHwActiveAdmin(who, userHandle).disableAdb;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (admin.mHwActiveAdmin != null && admin.mHwActiveAdmin.disableAdb) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setUSBOtgDisabled(ComponentName who, boolean disabled, int userHandle) {
        VolumeInfo volumeInfo;
        DiskInfo diskInfo;
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_USB", "does not have MDM_USB permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableUSBOtg != disabled) {
                    admin.disableUSBOtg = disabled;
                    saveSettingsLocked(userHandle);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        long identityToken = Binder.clearCallingIdentity();
        StorageManager sm = (StorageManager) this.mContext.getSystemService("storage");
        StorageVolume[] volumeList = sm.getVolumeList();
        for (StorageVolume storageVolume : volumeList) {
            if (storageVolume.isRemovable() && "mounted".equals(sm.getVolumeState(storageVolume.getPath())) && (volumeInfo = sm.findVolumeByUuid(storageVolume.getUuid())) != null && (diskInfo = volumeInfo.getDisk()) != null && diskInfo.isUsb()) {
                Slog.e(TAG, "find usb otg device mounted , umounted it");
                sm.unmount(storageVolume.getId());
            }
        }
        Binder.restoreCallingIdentity(identityToken);
        HwAdminCache hwAdminCache = this.mHwAdminCache;
        if (hwAdminCache != null) {
            hwAdminCache.syncHwAdminCache(12, isUSBOtgDisabled(null, userHandle));
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isUSBOtgDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                return getHwActiveAdmin(who, userHandle).disableUSBOtg;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (admin.mHwActiveAdmin != null && admin.mHwActiveAdmin.disableUSBOtg) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setGPSDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_LOCATION", "does not have MDM_LOCATION permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (disabled != admin.disableGPS) {
                    admin.disableGPS = disabled;
                    saveSettingsLocked(userHandle);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        if (isGPSTurnOn(who, userHandle) && disabled) {
            turnOnGPS(who, false, userHandle);
        }
        HwAdminCache hwAdminCache = this.mHwAdminCache;
        if (hwAdminCache != null) {
            hwAdminCache.syncHwAdminCache(13, isGPSDisabled(null, userHandle));
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isGPSDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                return getHwActiveAdmin(who, userHandle).disableGPS;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (admin.mHwActiveAdmin != null && admin.mHwActiveAdmin.disableGPS) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void turnOnGPS(ComponentName who, boolean on, int userHandle) {
        int locationMode;
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_LOCATION", "does not have MDM_LOCATION permission!");
        if (who != null) {
            synchronized (getLockObject()) {
                getHwActiveAdmin(who, userHandle);
            }
            if (isGPSTurnOn(who, userHandle) != on) {
                long identityToken = Binder.clearCallingIdentity();
                int locationMode2 = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "location_mode", 0, ActivityManager.getCurrentUser());
                if (on) {
                    locationMode = locationMode2 | 1;
                } else {
                    locationMode = locationMode2 & 2;
                }
                if (!Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "location_mode", locationMode, ActivityManager.getCurrentUser())) {
                    Log.e(TAG, "setLocationProviderEnabledForUser failed");
                }
                Binder.restoreCallingIdentity(identityToken);
                return;
            }
            return;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isGPSTurnOn(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            getHwActiveAdmin(who, userHandle);
        }
        long identityToken = Binder.clearCallingIdentity();
        boolean isGPSEnabled = false;
        int locationMode = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "location_mode", 0, ActivityManager.getCurrentUser());
        if (locationMode == 3 || locationMode == 2) {
            isGPSEnabled = true;
        }
        Binder.restoreCallingIdentity(identityToken);
        return isGPSEnabled;
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setTaskButtonDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have DEVICE MANAGER permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableTaskKey != disabled) {
                    admin.disableTaskKey = disabled;
                    saveSettingsLocked(userHandle);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        HwAdminCache hwAdminCache = this.mHwAdminCache;
        if (hwAdminCache != null) {
            hwAdminCache.syncHwAdminCache(15, isTaskButtonDisabled(null, userHandle));
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isTaskButtonDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                return getHwActiveAdmin(who, userHandle).disableTaskKey;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (admin.mHwActiveAdmin != null && admin.mHwActiveAdmin.disableTaskKey) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setHomeButtonDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have DEVICE MANAGER permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableHomeKey != disabled) {
                    admin.disableHomeKey = disabled;
                    saveSettingsLocked(userHandle);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        HwAdminCache hwAdminCache = this.mHwAdminCache;
        if (hwAdminCache != null) {
            hwAdminCache.syncHwAdminCache(14, isHomeButtonDisabled(null, userHandle));
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isHomeButtonDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                return getHwActiveAdmin(who, userHandle).disableHomeKey;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (admin.mHwActiveAdmin != null && admin.mHwActiveAdmin.disableHomeKey) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setBackButtonDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have DEVICE MANAGER permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableBackKey != disabled) {
                    admin.disableBackKey = disabled;
                    saveSettingsLocked(userHandle);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        HwAdminCache hwAdminCache = this.mHwAdminCache;
        if (hwAdminCache != null) {
            hwAdminCache.syncHwAdminCache(16, isBackButtonDisabled(null, userHandle));
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isBackButtonDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                return getHwActiveAdmin(who, userHandle).disableBackKey;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (admin.mHwActiveAdmin != null && admin.mHwActiveAdmin.disableBackKey) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setSysTime(ComponentName who, long millis, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device manager MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                try {
                    getHwActiveAdmin(who, userHandle);
                    long id = Binder.clearCallingIdentity();
                    SystemClock.setCurrentTimeMillis(millis);
                    Binder.restoreCallingIdentity(id);
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setCustomSettingsMenu(ComponentName who, List<String> menusToDelete, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                getHwActiveAdmin(who, userHandle);
                long callingId = Binder.clearCallingIdentity();
                if (menusToDelete != null) {
                    try {
                        if (!menusToDelete.isEmpty()) {
                            String oldMenus = Settings.Global.getStringForUser(this.mContext.getContentResolver(), SETTINGS_MENUS_REMOVE, userHandle);
                            StringBuffer newMenus = new StringBuffer();
                            if (!TextUtils.isEmpty(oldMenus)) {
                                newMenus.append(oldMenus);
                            }
                            for (String menu : menusToDelete) {
                                if (oldMenus == null || !oldMenus.contains(menu)) {
                                    newMenus.append(menu);
                                    newMenus.append(",");
                                }
                            }
                            Settings.Global.putStringForUser(this.mContext.getContentResolver(), SETTINGS_MENUS_REMOVE, newMenus.toString(), userHandle);
                            return;
                        }
                    } finally {
                        Binder.restoreCallingIdentity(callingId);
                    }
                }
                Settings.Global.putStringForUser(this.mContext.getContentResolver(), SETTINGS_MENUS_REMOVE, "", userHandle);
                Binder.restoreCallingIdentity(callingId);
                return;
            }
            throw new IllegalArgumentException("ComponentName is null");
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setDefaultLauncher(ComponentName who, String packageName, String className, int userHandle) {
        if (!LauncherUtils.checkPkgAndClassNameValid(packageName, className)) {
            throw new IllegalArgumentException("packageName or className is invalid");
        } else if (LauncherUtils.checkLauncherPermisson(packageName)) {
            Bundle result = getDefaultLauncher(null, userHandle);
            if (result == null || TextUtils.isEmpty(result.getString("value", ""))) {
                enforceHwCrossUserPermission(userHandle);
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.SDK_LAUNCHER", "Does not have sdk_launcher permission.");
                synchronized (getLockObject()) {
                    if (who != null) {
                        try {
                            AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                            if (LauncherUtils.setDefaultLauncher(this.mContext, packageName, className, this.mIPackageManager, userHandle)) {
                                admin.disableChangeLauncher = true;
                                admin.defaultLauncher = packageName + "/" + className;
                                saveSettingsLocked(userHandle);
                                if (this.mHwAdminCache != null) {
                                    this.mHwAdminCache.syncHwAdminCache(17, isChangeLauncherDisabled(null, userHandle));
                                    Bundle bundle = new Bundle();
                                    bundle.putString("value", admin.defaultLauncher);
                                    this.mHwAdminCache.syncHwAdminCache(HwAdminCache.POLICY_DEFAULTE_LAUNCHER, bundle);
                                }
                            } else {
                                Log.w(TAG, "set default launcher failed.");
                            }
                        } catch (Throwable th) {
                            throw th;
                        }
                    } else {
                        throw new IllegalArgumentException("ComponentName is null");
                    }
                }
                return;
            }
            throw new IllegalArgumentException("the device is already hava third default launcher, you must clear it first");
        } else {
            throw new IllegalArgumentException("The Launcher's signature is different from the host app's!");
        }
    }

    private Bundle getDefaultLauncher(ComponentName who, int userHandle) {
        Bundle bundle = new Bundle();
        synchronized (getLockObject()) {
            if (who != null) {
                bundle.putString("value", getHwActiveAdmin(who, userHandle).defaultLauncher);
                return bundle;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (admin.mHwActiveAdmin != null) {
                    String defalutLauncher = admin.mHwActiveAdmin.defaultLauncher;
                    if (!TextUtils.isEmpty(defalutLauncher)) {
                        bundle.putString("value", defalutLauncher);
                        return bundle;
                    }
                }
            }
            return null;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void clearDefaultLauncher(ComponentName who, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.SDK_LAUNCHER", "Does not have sdk_launcher permission.");
        synchronized (getLockObject()) {
            if (who != null) {
                try {
                    AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                    if (admin.disableChangeLauncher && LauncherUtils.clearDefaultLauncher(this.mContext, this.mIPackageManager, userHandle)) {
                        admin.disableChangeLauncher = false;
                        admin.defaultLauncher = "";
                        saveSettingsLocked(userHandle);
                        if (this.mHwAdminCache != null) {
                            this.mHwAdminCache.syncHwAdminCache(17, isChangeLauncherDisabled(null, userHandle));
                        }
                    }
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    public boolean isChangeLauncherDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                return getHwActiveAdmin(who, userHandle).disableChangeLauncher;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (admin.mHwActiveAdmin != null && admin.mHwActiveAdmin.disableChangeLauncher) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public Bitmap captureScreen(ComponentName who, int userHandle) {
        Bitmap bmp;
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_CAPTURE_SCREEN", "Does not have MDM_CAPTURE_SCREEN permission.");
        synchronized (getLockObject()) {
            if (who != null) {
                try {
                    getHwActiveAdmin(who, userHandle);
                    long callingId = Binder.clearCallingIdentity();
                    bmp = CaptureScreenUtils.captureScreen(this.mContext);
                    Binder.restoreCallingIdentity(callingId);
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        return bmp;
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void addApn(ComponentName who, Map<String, String> apnInfo, int userHandle) {
        if (apnInfo == null || apnInfo.isEmpty()) {
            throw new IllegalArgumentException("apnInfo is empty.");
        }
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APN", "Does not have apn permission.");
        synchronized (getLockObject()) {
            if (who != null) {
                try {
                    getHwActiveAdmin(who, userHandle);
                    long callingId = Binder.clearCallingIdentity();
                    ApnUtils.addApn(this.mContext.getContentResolver(), apnInfo);
                    Binder.restoreCallingIdentity(callingId);
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void deleteApn(ComponentName who, String apnId, int userHandle) {
        if (!TextUtils.isEmpty(apnId)) {
            enforceHwCrossUserPermission(userHandle);
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APN", "Does not have apn permission.");
            synchronized (getLockObject()) {
                if (who != null) {
                    try {
                        getHwActiveAdmin(who, userHandle);
                        long callingId = Binder.clearCallingIdentity();
                        ApnUtils.deleteApn(this.mContext.getContentResolver(), apnId);
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Throwable th) {
                        throw th;
                    }
                } else {
                    throw new IllegalArgumentException("ComponentName is null");
                }
            }
            return;
        }
        throw new IllegalArgumentException("apnId is empty.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void updateApn(ComponentName who, Map<String, String> apnInfo, String apnId, int userHandle) {
        if (apnInfo == null || apnInfo.isEmpty()) {
            throw new IllegalArgumentException("apnInfo is empty.");
        } else if (!TextUtils.isEmpty(apnId)) {
            enforceHwCrossUserPermission(userHandle);
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APN", "Does not have apn permission.");
            synchronized (getLockObject()) {
                if (who != null) {
                    try {
                        getHwActiveAdmin(who, userHandle);
                        long callingId = Binder.clearCallingIdentity();
                        ApnUtils.updateApn(this.mContext.getContentResolver(), apnInfo, apnId);
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Throwable th) {
                        throw th;
                    }
                } else {
                    throw new IllegalArgumentException("ComponentName is null");
                }
            }
        } else {
            throw new IllegalArgumentException("apnId is empty.");
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setPreferApn(ComponentName who, String apnId, int userHandle) {
        if (!TextUtils.isEmpty(apnId)) {
            enforceHwCrossUserPermission(userHandle);
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APN", "Does not have apn permission.");
            synchronized (getLockObject()) {
                if (who != null) {
                    try {
                        getHwActiveAdmin(who, userHandle);
                        long callingId = Binder.clearCallingIdentity();
                        ApnUtils.setPreferApn(this.mContext.getContentResolver(), apnId);
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Throwable th) {
                        throw th;
                    }
                } else {
                    throw new IllegalArgumentException("ComponentName is null");
                }
            }
            return;
        }
        throw new IllegalArgumentException("apnId is empty.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public List<String> queryApn(ComponentName who, Map<String, String> apnInfo, int userHandle) {
        List<String> ids;
        if (apnInfo == null || apnInfo.isEmpty()) {
            throw new IllegalArgumentException("apnInfo is empty.");
        }
        enforceHwCrossUserPermission(userHandle);
        synchronized (getLockObject()) {
            if (who != null) {
                try {
                    getHwActiveAdmin(who, userHandle);
                    long callingId = Binder.clearCallingIdentity();
                    ids = ApnUtils.queryApn(this.mContext.getContentResolver(), apnInfo);
                    Binder.restoreCallingIdentity(callingId);
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        return ids;
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public Map<String, String> getApnInfo(ComponentName who, String apnId, int userHandle) {
        Map<String, String> apnInfo;
        if (!TextUtils.isEmpty(apnId)) {
            enforceHwCrossUserPermission(userHandle);
            synchronized (getLockObject()) {
                if (who != null) {
                    try {
                        getHwActiveAdmin(who, userHandle);
                        long callingId = Binder.clearCallingIdentity();
                        apnInfo = ApnUtils.getApnInfo(this.mContext.getContentResolver(), apnId);
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Throwable th) {
                        throw th;
                    }
                } else {
                    throw new IllegalArgumentException("ComponentName is null");
                }
            }
            return apnInfo;
        }
        throw new IllegalArgumentException("apnId is empty.");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void addNetworkAccessWhitelist(ComponentName who, List<String> addrList, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_NETWORK_MANAGER", "does not have network_manager MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidIPAddrs(addrList)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                    if (admin.networkAccessWhitelist == null) {
                        admin.networkAccessWhitelist = new ArrayList();
                    }
                    HwDevicePolicyManagerServiceUtil.isAddrOverLimit(admin.networkAccessWhitelist, addrList);
                    HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(admin.networkAccessWhitelist, addrList);
                    saveSettingsLocked(userHandle);
                    setNetworkAccessWhitelist(admin.networkAccessWhitelist);
                } else {
                    throw new IllegalArgumentException("ComponentName is null");
                }
            }
            HwAdminCache hwAdminCache = this.mHwAdminCache;
            if (hwAdminCache != null) {
                hwAdminCache.syncHwAdminCache(9, getNetworkAccessWhitelist(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("addrlist invalid");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void removeNetworkAccessWhitelist(ComponentName who, List<String> addrList, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_NETWORK_MANAGER", "does not have network_manager MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidIPAddrs(addrList)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                    HwDevicePolicyManagerServiceUtil.removeItemsFromList(admin.networkAccessWhitelist, addrList);
                    saveSettingsLocked(userHandle);
                    setNetworkAccessWhitelist(admin.networkAccessWhitelist);
                } else {
                    throw new IllegalArgumentException("ComponentName is null");
                }
            }
            HwAdminCache hwAdminCache = this.mHwAdminCache;
            if (hwAdminCache != null) {
                hwAdminCache.syncHwAdminCache(9, getNetworkAccessWhitelist(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("addrlist invalid");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public List<String> getNetworkAccessWhitelist(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            List<String> list = null;
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.networkAccessWhitelist != null) {
                    if (!admin.networkAccessWhitelist.isEmpty()) {
                        list = admin.networkAccessWhitelist;
                    }
                }
                return list;
            }
            DevicePolicyData policy = getUserData(userHandle);
            ArrayList<String> addrList = new ArrayList<>();
            Iterator it = policy.mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin2 = (ActiveAdmin) it.next();
                if (admin2.mHwActiveAdmin != null) {
                    HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(addrList, admin2.mHwActiveAdmin.networkAccessWhitelist);
                }
            }
            if (!addrList.isEmpty()) {
                list = addrList;
            }
            return list;
        }
    }

    private void setNetworkAccessWhitelist(List<String> whitelist) {
        IBinder b = ServiceManager.getService("network_management");
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        if (b != null) {
            try {
                data.writeInterfaceToken("android.os.INetworkManagementService");
                data.writeStringList(whitelist);
                b.transact(HwArbitrationDEFS.MSG_STREAMING_VIDEO_BAD, data, reply, 0);
                reply.readException();
            } catch (RemoteException localRemoteException) {
                Log.e(TAG, "setNetworkAccessWhitelist error", localRemoteException);
            } catch (Throwable th) {
                reply.recycle();
                data.recycle();
                throw th;
            }
        }
        reply.recycle();
        data.recycle();
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean getHwAdminCachedValue(int code) {
        int type = -1;
        if (code == 4009) {
            type = 8;
        } else if (code == 5021) {
            type = 29;
        } else if (code != 5022) {
            switch (code) {
                case 4001:
                    type = 0;
                    break;
                case 4002:
                    type = 1;
                    break;
                case 4003:
                    type = 2;
                    break;
                default:
                    switch (code) {
                        case 4011:
                            type = 10;
                            break;
                        case 4012:
                            type = 11;
                            break;
                        case 4013:
                            type = 12;
                            break;
                        case 4014:
                            type = 13;
                            break;
                        case 4015:
                            type = 14;
                            break;
                        case 4016:
                            type = 15;
                            break;
                        case 4017:
                            type = 16;
                            break;
                        case 4018:
                            type = 17;
                            break;
                        default:
                            switch (code) {
                                case 4021:
                                    type = 21;
                                    break;
                                case 4022:
                                    type = 22;
                                    break;
                                case 4023:
                                    type = 23;
                                    break;
                                case 4024:
                                    type = 24;
                                    break;
                                case 4025:
                                    type = 25;
                                    break;
                                case 4026:
                                    type = 26;
                                    break;
                            }
                    }
            }
        } else {
            type = 32;
        }
        HwAdminCache hwAdminCache = this.mHwAdminCache;
        if (hwAdminCache == null || type == -1) {
            return false;
        }
        return hwAdminCache.getCachedValue(type);
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public List<String> getHwAdminCachedList(int code) {
        List<String> result = null;
        int type = -1;
        if (code == 4010) {
            type = 9;
        } else if (code == 4019) {
            type = 18;
        } else if (code == 4020) {
            type = 20;
        } else if (code == 4027) {
            type = 27;
        } else if (code != 4028) {
            switch (code) {
                case 4004:
                    type = 3;
                    break;
                case 4005:
                    type = 4;
                    break;
                case 4006:
                    type = 5;
                    break;
                case 4007:
                    type = 6;
                    break;
                case 4008:
                    type = 7;
                    break;
            }
        } else {
            type = 28;
        }
        HwAdminCache hwAdminCache = this.mHwAdminCache;
        if (!(hwAdminCache == null || type == -1)) {
            result = hwAdminCache.getCachedList(type);
        }
        return result == null ? new ArrayList() : result;
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public Bundle getHwAdminCachedBundle(String policyName) {
        HwAdminCache hwAdminCache = this.mHwAdminCache;
        if (hwAdminCache != null) {
            return hwAdminCache.getCachedBundle(policyName);
        }
        return null;
    }

    private void enforceUserRestrictionPermission(ComponentName who, String key, int userHandle) {
        long id = Binder.clearCallingIdentity();
        try {
            UserInfo info = this.mUserManager.getUserInfo(userHandle);
            if (info == null) {
                throw new IllegalArgumentException("Invalid user: " + userHandle);
            } else if (info.isGuest()) {
                throw new IllegalStateException("Cannot call this method on a guest");
            } else if (who == null) {
                throw new IllegalArgumentException("Component is null");
            } else if (userHandle != 0 && HWDEVICE_OWNER_USER_RESTRICTIONS.contains(key)) {
                throw new SecurityException("Cannot set user restriction " + key);
            }
        } finally {
            Binder.restoreCallingIdentity(id);
        }
    }

    private AbsDevicePolicyManagerService.HwActiveAdmin getHwActiveAdmin(ComponentName who, int userHandle) {
        ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
        if (admin == null) {
            throw new SecurityException("No active admin owned by uid " + Binder.getCallingUid() + ", ComponentName:" + who);
        } else if (admin.getUid() == Binder.getCallingUid()) {
            AbsDevicePolicyManagerService.HwActiveAdmin hwadmin = admin.mHwActiveAdmin;
            if (hwadmin != null) {
                return hwadmin;
            }
            AbsDevicePolicyManagerService.HwActiveAdmin hwadmin2 = new AbsDevicePolicyManagerService.HwActiveAdmin();
            admin.mHwActiveAdmin = hwadmin2;
            return hwadmin2;
        } else {
            throw new SecurityException("Admin " + who + " is not owned by uid " + Binder.getCallingUid());
        }
    }

    private void setHwUserRestriction(String key, boolean disable, int userHandle) {
        UserHandle user = new UserHandle(userHandle);
        boolean isAlreadyRestricted = this.mUserManager.hasUserRestriction(key, user);
        long id = Binder.clearCallingIdentity();
        if (disable && !isAlreadyRestricted) {
            try {
                if ("no_config_tethering".equals(key)) {
                    cancelNotificationAsUser();
                } else if ("no_physical_media".equals(key)) {
                    boolean isHasExternalSdcard = StorageUtils.hasExternalSdcard(this.mContext);
                    boolean isDafaultIsSdcard = DefaultStorageLocation.isSdcard();
                    if (isHasExternalSdcard && !isDafaultIsSdcard) {
                        Log.w(TAG, "call doUnMount");
                        StorageUtils.doUnMount(this.mContext);
                    } else if (isHasExternalSdcard && isDafaultIsSdcard) {
                        if (StorageUtils.isSwitchPrimaryVolumeSupported()) {
                            throw new IllegalStateException("could not disable sdcard when it is primary card.");
                        }
                    }
                } else if ("no_usb_file_transfer".equals(key)) {
                    if (disable) {
                        Settings.Global.putStringForUser(this.mContext.getContentResolver(), "adb_enabled", "0", userHandle);
                    }
                    this.mUserManager.setUserRestriction("no_debugging_features", true, user);
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(id);
                throw th;
            }
        }
        this.mUserManager.setUserRestriction(key, disable, user);
        if ("no_usb_file_transfer".equals(key) && !disable) {
            this.mUserManager.setUserRestriction("no_debugging_features", false, user);
        }
        Binder.restoreCallingIdentity(id);
        sendHwChangedNotification(userHandle);
    }

    private void cancelNotificationAsUser() {
        if (((WifiManager) this.mContext.getSystemService(DevSchedFeatureRT.WIFI_FEATURE)).isWifiApEnabled()) {
            ((ConnectivityManager) this.mContext.getSystemService("connectivity")).stopTethering(0);
            ((NotificationManager) this.mContext.getSystemService("notification")).cancelAsUser(null, 17303610, UserHandle.ALL);
        }
    }

    private void sendHwChangedNotification(int userHandle) {
        Intent intent = new Intent("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        intent.setFlags(1073741824);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, new UserHandle(userHandle));
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean haveHwUserRestriction(String key, int userid) {
        UserHandle user = new UserHandle(userid);
        user.getIdentifier();
        return this.mUserManager.hasUserRestriction(key, user);
    }

    /* access modifiers changed from: protected */
    public void syncHwDeviceSettingsLocked(int userHandle) {
        if (userHandle != 0) {
            Log.w(TAG, "userHandle is not USER_OWNER, return ");
            return;
        }
        combineAllPolicies(userHandle, true);
        try {
            synchronized (getLockObject()) {
                for (String s : HWDEVICE_OWNER_USER_RESTRICTIONS) {
                    hwSyncDeviceCapabilitiesLocked(s, userHandle);
                }
            }
            hwSyncDeviceStatusBarLocked(userHandle);
        } catch (SecurityException e) {
            Log.e(TAG, "syncHwDeviceSettingsLocked SecurityException is happened");
        } catch (Exception e2) {
            Log.e(TAG, "syncHwDeviceSettingsLocked exception is happened");
        }
        try {
            syncHwAdminCache(userHandle);
        } catch (Exception e3) {
            Log.e(TAG, "syncHwAdminCache exception is happened");
        }
        sendPersistentAppToIAware(userHandle);
    }

    private void syncHwAdminSafeModeCache(int userHandle) {
        if (this.mHwAdminCache != null) {
            boolean isDisableSafeMode = isSafeModeDisabled(null, userHandle);
            this.mHwAdminCache.syncHwAdminCache(10, isDisableSafeMode);
            long identityToken = Binder.clearCallingIdentity();
            if (isDisableSafeMode) {
                Settings.Global.putInt(this.mContext.getContentResolver(), "isSafeModeDisabled", 1);
            } else {
                Settings.Global.putInt(this.mContext.getContentResolver(), "isSafeModeDisabled", 0);
            }
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private void syncHwAdminCache(int userHandle) {
        if (this.mHwAdminCache == null) {
            this.mHwAdminCache = new HwAdminCache();
        }
        this.mHwAdminCache.syncHwAdminCache(0, isWifiDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(8, isBluetoothDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(1, isVoiceDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(2, isInstallSourceDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(3, getInstallPackageSourceWhiteList(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(4, getPersistentApp(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(5, getDisallowedRunningApp(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(6, getInstallPackageWhiteList(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(7, getDisallowedUninstallPackageList(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(18, getDisabledDeactivateMdmPackageList(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(9, getNetworkAccessWhitelist(null, userHandle));
        syncHwAdminSafeModeCache(userHandle);
        this.mHwAdminCache.syncHwAdminCache(11, isAdbDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(12, isUSBOtgDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(13, isGPSDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(14, isHomeButtonDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(15, isTaskButtonDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(16, isBackButtonDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(17, isChangeLauncherDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.INSTALL_APKS_BLACK_LIST_POLICY, getPolicy(null, HwAdminCache.INSTALL_APKS_BLACK_LIST_POLICY, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.DISABLE_SCREEN_CAPTURE_POLICY, getPolicy(null, HwAdminCache.DISABLE_SCREEN_CAPTURE_POLICY, userHandle));
        syncHwAdminCacheNextPart(userHandle);
        syncHwAdminCacheAnotherPart(userHandle);
        syncHwAdminCacheFinallyPart(userHandle);
    }

    private void syncHwAdminCacheFinallyPart(int userHandle) {
        if (this.mHwAdminCache == null) {
            this.mHwAdminCache = new HwAdminCache();
        }
        this.mHwAdminCache.syncHwAdminCache("disable-fingerprint-authentication", getPolicy(null, "disable-fingerprint-authentication", userHandle));
        this.mHwAdminCache.syncHwAdminCache("force-enable-BT", getPolicy(null, "force-enable-BT", userHandle));
        this.mHwAdminCache.syncHwAdminCache("force-enable-wifi", getPolicy(null, "force-enable-wifi", userHandle));
        this.mHwAdminCache.syncHwAdminCache(SettingsMDMPlugin.POLICY_ACCESSIBILITY_SERVICES_WHITE_LIST, getPolicy(null, SettingsMDMPlugin.POLICY_ACCESSIBILITY_SERVICES_WHITE_LIST, userHandle));
        this.mHwAdminCache.syncHwAdminCache("policy-file-share-disabled", getPolicy(null, "policy-file-share-disabled", userHandle));
        this.mHwAdminCache.syncHwAdminCache(SettingsMDMPlugin.POLICY_PHONE_FIND, getPolicy(null, SettingsMDMPlugin.POLICY_PHONE_FIND, userHandle));
        this.mHwAdminCache.syncHwAdminCache(SettingsMDMPlugin.POLICY_PARENT_CONTROL, getPolicy(null, SettingsMDMPlugin.POLICY_PARENT_CONTROL, userHandle));
        this.mHwAdminCache.syncHwAdminCache(SettingsMDMPlugin.POLICY_SIM_LOCK, getPolicy(null, SettingsMDMPlugin.POLICY_SIM_LOCK, userHandle));
        this.mHwAdminCache.syncHwAdminCache(SettingsMDMPlugin.POLICY_APPLICATION_LOCK, getPolicy(null, SettingsMDMPlugin.POLICY_APPLICATION_LOCK, userHandle));
        this.mHwAdminCache.syncHwAdminCache(SettingsMDMPlugin.DISABLED_ANDROID_ANIMATION, getPolicy(null, SettingsMDMPlugin.DISABLED_ANDROID_ANIMATION, userHandle));
        this.mHwAdminCache.syncHwAdminCache(SettingsMDMPlugin.POLICY_FORCE_ENCRYPT_SDCARD, getPolicy(null, SettingsMDMPlugin.POLICY_FORCE_ENCRYPT_SDCARD, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.POLICY_DISABLE_MULTIWINDOW, getPolicy(null, HwAdminCache.POLICY_DISABLE_MULTIWINDOW, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.POLICY_NETWORK_WHITE_IP_LIST, getPolicy(null, HwAdminCache.POLICY_NETWORK_WHITE_IP_LIST, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.POLICY_NETWORK_WHITE_DOMAIN_LIST, getPolicy(null, HwAdminCache.POLICY_NETWORK_WHITE_DOMAIN_LIST, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.POLICY_NETWORK_BLACK_IP_LIST, getPolicy(null, HwAdminCache.POLICY_NETWORK_BLACK_IP_LIST, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.POLICY_NETWORK_BLACK_DOMAIN_LIST, getPolicy(null, HwAdminCache.POLICY_NETWORK_BLACK_DOMAIN_LIST, userHandle));
    }

    private void syncHwAdminCacheAnotherPart(int userHandle) {
        if (this.mHwAdminCache == null) {
            this.mHwAdminCache = new HwAdminCache();
        }
        this.mHwAdminCache.syncHwAdminCache("policy-single-app", getPolicy(null, "policy-single-app", userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.POLICY_DEFAULTE_LAUNCHER, getDefaultLauncher(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache("disable-change-wallpaper", getPolicy(null, "disable-change-wallpaper", userHandle));
        this.mHwAdminCache.syncHwAdminCache(SettingsMDMPlugin.POLICY_FORBIDDEN_SCREEN_OFF, getPolicy(null, SettingsMDMPlugin.POLICY_FORBIDDEN_SCREEN_OFF, userHandle));
        this.mHwAdminCache.syncHwAdminCache("disable-power-shutdown", getPolicy(null, "disable-power-shutdown", userHandle));
        this.mHwAdminCache.syncHwAdminCache("disable-shutdownmenu", getPolicy(null, "disable-shutdownmenu", userHandle));
        this.mHwAdminCache.syncHwAdminCache("disable-volume", getPolicy(null, "disable-volume", userHandle));
        this.mHwAdminCache.syncHwAdminCache(SettingsMDMPlugin.POLICY_FORBIDDEN_LOCATION_SERVICE, getPolicy(null, SettingsMDMPlugin.POLICY_FORBIDDEN_LOCATION_SERVICE, userHandle));
        this.mHwAdminCache.syncHwAdminCache(SettingsMDMPlugin.POLICY_FORBIDDEN_LOCATION_MODE, getPolicy(null, SettingsMDMPlugin.POLICY_FORBIDDEN_LOCATION_MODE, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.DISABLE_SYNC, getPolicy(null, HwAdminCache.DISABLE_SYNC, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.DISABLE_PASSIVE_PROVIDER_POLICY, getPolicy(null, HwAdminCache.DISABLE_PASSIVE_PROVIDER_POLICY, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.DISABLE_WIFIP2P_POLICY, getPolicy(null, HwAdminCache.DISABLE_WIFIP2P_POLICY, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.DISABLE_INFRARED_POLICY, getPolicy(null, HwAdminCache.DISABLE_INFRARED_POLICY, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.DISABLE_SCREEN_TURN_OFF, getPolicy(null, HwAdminCache.DISABLE_SCREEN_TURN_OFF, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.UNAVAILABLE_SSID_LIST, getPolicy(null, HwAdminCache.UNAVAILABLE_SSID_LIST, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.DISABLE_STATUS_BAR, getPolicy(null, HwAdminCache.DISABLE_STATUS_BAR, userHandle));
    }

    private void syncHwAdminCacheNextPart(int userHandle) {
        if (this.mHwAdminCache == null) {
            this.mHwAdminCache = new HwAdminCache();
        }
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.DISABLE_APPLICATIONS_LIST_POLICY, getPolicy(null, HwAdminCache.DISABLE_APPLICATIONS_LIST_POLICY, userHandle));
        this.mHwAdminCache.syncHwAdminCache("disable-clipboard", getPolicy(null, "disable-clipboard", userHandle));
        this.mHwAdminCache.syncHwAdminCache("disable-google-account-autosync", getPolicy(null, "disable-google-account-autosync", userHandle));
        this.mHwAdminCache.syncHwAdminCache("ignore-frequent-relaunch-app", getPolicy(null, "ignore-frequent-relaunch-app", userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.DISABLE_SDWRITING_POLICY, getPolicy(null, HwAdminCache.DISABLE_SDWRITING_POLICY, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.DISABLE_NOTIFICATION_POLICY, getPolicy(null, HwAdminCache.DISABLE_NOTIFICATION_POLICY, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.DISABLE_MICROPHONE, getPolicy(null, HwAdminCache.DISABLE_MICROPHONE, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.DISABLE_NAVIGATIONBAR_POLICY, getPolicy(null, HwAdminCache.DISABLE_NAVIGATIONBAR_POLICY, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.SUPER_WHITE_LIST_APP, getPolicy(null, HwAdminCache.SUPER_WHITE_LIST_APP, userHandle));
        this.mHwAdminCache.syncHwAdminCache(SettingsMDMPlugin.POLICY_FORBIDDEN_NETWORK_LOCATION, getPolicy(null, SettingsMDMPlugin.POLICY_FORBIDDEN_NETWORK_LOCATION, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.DISABLE_HEADPHONE, getPolicy(null, HwAdminCache.DISABLE_HEADPHONE, userHandle));
        this.mHwAdminCache.syncHwAdminCache("disable-send-notification", getPolicy(null, "disable-send-notification", userHandle));
    }

    private void hwSyncDeviceCapabilitiesLocked(String restriction, int userHandle) {
        boolean isDisabled = false;
        boolean isAlreadyRestricted = haveHwUserRestriction(restriction, userHandle);
        Iterator it = getUserData(userHandle).mAdminList.iterator();
        while (true) {
            if (it.hasNext()) {
                if (isUserRestrictionDisabled(restriction, ((ActiveAdmin) it.next()).mHwActiveAdmin)) {
                    isDisabled = true;
                    break;
                }
            } else {
                break;
            }
        }
        if (isDisabled != isAlreadyRestricted) {
            if (HWFLOW) {
                Log.i(TAG, "Set " + restriction + " to " + isDisabled);
            }
            setHwUserRestriction(restriction, isDisabled, userHandle);
        }
    }

    private void hwSyncDeviceStatusBarLocked(int userHandle) {
        if (isStatusBarExpandPanelDisabled(null, userHandle)) {
            setStatusBarPanelDisabledInternal(userHandle);
        } else {
            setStatusBarPanelEnableInternal(true, userHandle);
        }
    }

    private boolean isUserRestrictionDisabled(String restriction, AbsDevicePolicyManagerService.HwActiveAdmin admin) {
        if (admin == null) {
            return false;
        }
        if ("no_usb_file_transfer".equals(restriction) && admin.disableUSBData) {
            return true;
        }
        if ("no_outgoing_calls".equals(restriction) && admin.disableVoice) {
            return true;
        }
        if ("no_sms".equals(restriction) && admin.disableSMS) {
            return true;
        }
        if ("no_config_tethering".equals(restriction) && admin.disableWifiAp) {
            return true;
        }
        if (!"no_physical_media".equals(restriction) || !admin.disableExternalStorage) {
            return false;
        }
        return true;
    }

    private void installPackage(String packagePath, final String installerPackageName) {
        if (!TextUtils.isEmpty(packagePath)) {
            long ident = Binder.clearCallingIdentity();
            try {
                final File tempFile = new File(packagePath.trim()).getCanonicalFile();
                if (!tempFile.getName().endsWith(".apk")) {
                    Binder.restoreCallingIdentity(ident);
                    return;
                }
                Uri packageURI = Uri.fromFile(tempFile);
                ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).installPackageAsUser(packageURI.getPath(), new PackageInstallObserver() {
                    /* class com.android.server.devicepolicy.HwDevicePolicyManagerService.AnonymousClass4 */

                    public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras) {
                        BdReportUtils.reportInstallPkgData(installerPackageName, basePackageName, HwDevicePolicyManagerService.this.mContext);
                        if (returnCode != 1) {
                            Log.e(HwDevicePolicyManagerService.TAG, "The package " + tempFile.getName() + "installed failed, error code: " + returnCode);
                        }
                    }
                }.getBinder(), 2, installerPackageName, 0);
                Binder.restoreCallingIdentity(ident);
            } catch (IOException e) {
                Log.e(TAG, "Get canonical file failed for package path: " + packagePath + ", error: " + e.getMessage());
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } else {
            throw new IllegalArgumentException("Install package path is empty");
        }
    }

    private void uninstallPackage(String packageName, boolean keepData) {
        if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("Uninstall package name is empty");
        } else if (HwDevicePolicyManagerServiceUtil.isValidatePackageName(packageName)) {
            long ident = Binder.clearCallingIdentity();
            try {
                PackageManager pm = this.mContext.getPackageManager();
                int i = 0;
                if (pm.getApplicationInfo(packageName, 0) == null) {
                    Binder.restoreCallingIdentity(ident);
                    return;
                }
                if (keepData) {
                    i = 1;
                }
                pm.deletePackage(packageName, null, i);
                Binder.restoreCallingIdentity(ident);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Name not found for package: " + packageName);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } else {
            throw new IllegalArgumentException("packageName:" + packageName + " is invalid.");
        }
    }

    private void filterOutSystemAppList(List<String> packageNames, int userHandle) {
        List<String> systemAppList = new ArrayList<>();
        try {
            for (String name : packageNames) {
                if (isSystemAppExcludePreInstalled(name)) {
                    systemAppList.add(name);
                }
            }
            if (!systemAppList.isEmpty()) {
                packageNames.removeAll(systemAppList);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "filterOutSystemAppList RuntimeException is happened");
        } catch (Exception e2) {
            Log.e(TAG, "filterOutSystemAppList exception is happened");
        }
    }

    private void enforceCheckNotActiveAdminApp(String packageName, int userHandle) {
        if (packageHasActiveAdmins(packageName, userHandle)) {
            throw new IllegalArgumentException("could not operate active admin app");
        }
    }

    private boolean isSystemAppExcludePreInstalled(String packageName) {
        long id = Binder.clearCallingIdentity();
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            if (pm == null) {
                restoreCallingIdentity(id);
                return false;
            }
            int userId = UserHandle.getCallingUserId();
            UserManager um = UserManager.get(this.mContext);
            if (um == null) {
                Log.e(TAG, "failed to get um");
                restoreCallingIdentity(id);
                return false;
            }
            UserInfo primaryUser = um.getProfileParent(userId);
            if (primaryUser == null) {
                primaryUser = um.getUserInfo(userId);
            }
            boolean isSystemAppExcludePreInstalled = isSystemAppExcludePreInstalled(pm, packageName, primaryUser.id);
            restoreCallingIdentity(id);
            return isSystemAppExcludePreInstalled;
        } catch (RemoteException e) {
            HwLog.e(TAG, "failed to check system app, RemoteException is  ");
        } catch (Exception e2) {
            HwLog.e(TAG, "failed to check system app ");
        } catch (Throwable th) {
            restoreCallingIdentity(id);
            throw th;
        }
        restoreCallingIdentity(id);
        return false;
    }

    private boolean isSystemAppExcludePreInstalled(IPackageManager pm, String packageName, int userId) throws RemoteException {
        if (packageName == null || packageName.equals("")) {
            return false;
        }
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 8192, userId);
            if (appInfo == null) {
                return false;
            }
            int flags = appInfo.flags;
            boolean isPreInstalled = true;
            if ((flags & 1) == 0) {
                Log.d(TAG, "packageName is not systemFlag");
                isPreInstalled = false;
            } else if (!((flags & 1) == 0 || (flags & 33554432) == 0)) {
                Log.w(TAG, "SystemApp preInstalledFlag");
                isPreInstalled = false;
            }
            int hwFlags = appInfo.hwFlags;
            if (!((flags & 1) == 0 || (hwFlags & 33554432) == 0)) {
                isPreInstalled = false;
                Log.d(TAG, "packageName is not systemFlag");
            }
            if ((hwFlags & 67108864) != 0) {
                return false;
            }
            return isPreInstalled;
        } catch (Exception e) {
            Log.e(TAG, "could not get appInfo, exception is hadppened");
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public int getSDCardEncryptionStatus() {
        char c = 65535;
        if (!IS_SUPPORT_CRYPT) {
            return -1;
        }
        String sdStatus = SystemProperties.get("vold.cryptsd.state");
        switch (sdStatus.hashCode()) {
            case -1512632483:
                if (sdStatus.equals("encrypting")) {
                    c = 1;
                    break;
                }
                break;
            case -1298848381:
                if (sdStatus.equals("enable")) {
                    c = 4;
                    break;
                }
                break;
            case -1212575282:
                if (sdStatus.equals("mismatch")) {
                    c = 5;
                    break;
                }
                break;
            case 395619662:
                if (sdStatus.equals("wait_unlock")) {
                    c = 6;
                    break;
                }
                break;
            case 1671308008:
                if (sdStatus.equals("disable")) {
                    c = 3;
                    break;
                }
                break;
            case 1959784951:
                if (sdStatus.equals("invalid")) {
                    c = 0;
                    break;
                }
                break;
            case 2066069301:
                if (sdStatus.equals("decrypting")) {
                    c = 2;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return 0;
            case 1:
                return 3;
            case 2:
                return 4;
            case 3:
                return 1;
            case 4:
                return 2;
            case 5:
                return 5;
            case 6:
                return 6;
            default:
                return 0;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setSDCardDecryptionDisabled(ComponentName who, boolean disabled, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdminForCallerLocked(who);
                if (admin.disableDecryptSDCard != disabled) {
                    admin.disableDecryptSDCard = disabled;
                    saveSettingsLocked(userHandle);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        HwAdminCache hwAdminCache = this.mHwAdminCache;
        if (hwAdminCache != null) {
            hwAdminCache.syncHwAdminCache(19, isSDCardDecryptionDisabled(null, userHandle));
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isSDCardDecryptionDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                return getHwActiveAdminUncheckedLocked(who, userHandle).disableDecryptSDCard;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (admin.mHwActiveAdmin != null && admin.mHwActiveAdmin.disableDecryptSDCard) {
                    return true;
                }
            }
            return false;
        }
    }

    private AbsDevicePolicyManagerService.HwActiveAdmin getHwActiveAdminUncheckedLocked(ComponentName who, int userHandle) {
        ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
        if (admin != null) {
            AbsDevicePolicyManagerService.HwActiveAdmin hwadmin = admin.mHwActiveAdmin;
            if (hwadmin != null) {
                return hwadmin;
            }
            AbsDevicePolicyManagerService.HwActiveAdmin hwadmin2 = new AbsDevicePolicyManagerService.HwActiveAdmin();
            admin.mHwActiveAdmin = hwadmin2;
            return hwadmin2;
        }
        throw new SecurityException("No active admin owned by uid " + Binder.getCallingUid() + ", ComponentName:" + who);
    }

    private AbsDevicePolicyManagerService.HwActiveAdmin getHwActiveAdminForCallerLocked(ComponentName who) {
        ActiveAdmin admin = getActiveAdminForCallerLocked(who, 7);
        if (admin != null) {
            AbsDevicePolicyManagerService.HwActiveAdmin hwadmin = admin.mHwActiveAdmin;
            if (hwadmin != null) {
                return hwadmin;
            }
            AbsDevicePolicyManagerService.HwActiveAdmin hwadmin2 = new AbsDevicePolicyManagerService.HwActiveAdmin();
            admin.mHwActiveAdmin = hwadmin2;
            return hwadmin2;
        }
        throw new SecurityException("No active admin owned by uid " + Binder.getCallingUid() + ", ComponentName:" + who);
    }

    /* access modifiers changed from: protected */
    public void init() {
        if (!this.isHasInit) {
            Iterator it = this.globalStructs.iterator();
            while (it.hasNext()) {
                PolicyStruct struct = (PolicyStruct) it.next();
                if (struct != null) {
                    struct.getOwner().init(struct);
                }
            }
            this.isHasInit = true;
        }
    }

    private void addDevicePolicyPlugins(Context context) {
        addPlugin(new DeviceNetworkPlugin(context));
        addPlugin(new DeviceRestrictionPlugin(context));
        addPlugin(new HwSystemManagerPlugin(context));
        addPlugin(new PhoneManagerPlugin(context));
        addPlugin(new HwEmailMDMPlugin(context));
        addPlugin(new DeviceVpnManagerImpl(context));
        addPlugin(new DeviceFirewallManagerImpl(context));
        addPlugin(new DeviceApplicationPlugin(context));
        addPlugin(new DeviceTelephonyPlugin(context));
        addPlugin(new DeviceCameraPlugin(context));
        addPlugin(new DeviceStorageManagerPlugin(context));
        addPlugin(new DevicePasswordPlugin(context));
        addPlugin(new SettingsMDMPlugin(context));
        addPlugin(new DeviceWifiPlugin(context));
        addPlugin(new DeviceBluetoothPlugin(context));
        addPlugin(new DeviceLocationPlugin(context));
        addPlugin(new DeviceP2PPlugin(context));
        addPlugin(new DeviceInfraredPlugin(context));
        addPlugin(new DeviceControlPlugin(context));
        addPlugin(new DevicePackageManagerPlugin(context));
        addPlugin(new FrameworkTestPlugin(context));
    }

    private void addPlugin(DevicePolicyPlugin plugin) {
        if (plugin != null) {
            this.globalPlugins.add(plugin);
        }
    }

    private void addPolicyStruct(PolicyStruct struct) {
        if (struct != null) {
            this.globalStructs.add(struct);
            for (PolicyStruct.PolicyItem item : struct.getPolicyItems()) {
                globalPolicyItems.put(item.getPolicyName(), item);
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void bdReport(int eventID, String eventMsg) {
        Context context = this.mContext;
        if (context != null) {
            Flog.bdReport(context, eventID, eventMsg);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:72:0x0204, code lost:
        if (r6 != 1) goto L_0x0210;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x0206, code lost:
        r22.mHwAdminCache.syncHwAdminCache(r24, getPolicy(null, r24, r26));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:74:0x0210, code lost:
        if (r8 == false) goto L_0x0216;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:76:0x0213, code lost:
        if (r6 != 1) goto L_0x0216;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:0x0215, code lost:
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:78:0x0216, code lost:
        r7.onSetPolicyCompleted(r23, r24, r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x0219, code lost:
        return r6;
     */
    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public int setPolicy(ComponentName who, String policyName, Bundle policyData, int userHandle) {
        String str;
        boolean z;
        int result;
        boolean isSetPolicyResult;
        boolean isSetPolicyResult2;
        StringBuilder sb = new StringBuilder();
        sb.append("setPolicy, policyName = ");
        sb.append(policyName);
        sb.append(", caller :");
        if (who == null) {
            str = "null";
        } else {
            str = who.flattenToString();
        }
        sb.append(str);
        HwLog.i(TAG, sb.toString());
        if (who != null) {
            DevicePolicyPlugin plugin = findPluginByPolicyName(policyName);
            if (plugin == null) {
                HwLog.e(TAG, "no plugin found, pluginName = " + policyName + ", caller :" + who.flattenToString());
                return -1;
            } else if (!plugin.checkCallingPermission(who, policyName)) {
                HwLog.e(TAG, "permission denied: " + who.flattenToString());
                return -1;
            } else {
                boolean isGolbalPolicyChanged = false;
                PolicyStruct struct = findPolicyStructByPolicyName(policyName);
                synchronized (getLockObject()) {
                    try {
                        AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdminUncheckedLocked(who, userHandle);
                        PolicyStruct.PolicyItem newItem = null;
                        if (struct != null) {
                            PolicyStruct.PolicyItem item = (PolicyStruct.PolicyItem) admin.adminPolicyItems.get(policyName);
                            PolicyStruct.PolicyItem oldItem = struct.getItemByPolicyName(policyName);
                            if (oldItem != null) {
                                newItem = new PolicyStruct.PolicyItem(policyName, oldItem.getItemType(), struct);
                                if (item == null) {
                                    try {
                                        newItem.copyFrom(oldItem);
                                        newItem.addAttrValues(newItem, policyData);
                                    } catch (Throwable th) {
                                        th = th;
                                    }
                                } else {
                                    newItem.deepCopyFrom(item);
                                }
                                newItem.addAttrValues(newItem, policyData);
                                PolicyStruct.PolicyItem combinedItem = combinePoliciesWithPolicyChanged(who, newItem, policyName, userHandle);
                                PolicyStruct.PolicyItem globalItem = (PolicyStruct.PolicyItem) globalPolicyItems.get(policyName);
                                if (globalItem == null) {
                                    try {
                                        HwLog.e(TAG, "no policy item found, pluginName = " + policyName + ", caller :" + who.flattenToString());
                                        return -1;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        throw th;
                                    }
                                } else {
                                    z = true;
                                    if (globalItem.equals(combinedItem)) {
                                        newItem.setGlobalPolicyChanged(2);
                                        isGolbalPolicyChanged = false;
                                    } else {
                                        newItem.setGlobalPolicyChanged(1);
                                        isGolbalPolicyChanged = true;
                                    }
                                }
                            } else {
                                HwLog.e(TAG, "no policy item found, pluginName = " + policyName + ", caller :" + who.flattenToString());
                                return -1;
                            }
                        } else {
                            z = true;
                        }
                        try {
                            HwLog.d(TAG, "when setPolicy, is global PolicyChanged ? = " + isGolbalPolicyChanged);
                            boolean isSetPolicyResult3 = plugin.onSetPolicy(who, policyName, policyData, isGolbalPolicyChanged);
                            if (isSetPolicyResult3) {
                                admin.adminPolicyItems.put(policyName, newItem);
                                if (newItem.getItemType() == PolicyStruct.PolicyType.CONFIGURATION) {
                                    globalPolicyItems.put(policyName, newItem);
                                    Iterator it = this.globalStructs.iterator();
                                    while (true) {
                                        if (!it.hasNext()) {
                                            break;
                                        }
                                        boolean isFound = false;
                                        Iterator it2 = ((PolicyStruct) it.next()).getPolicyMap().keySet().iterator();
                                        while (true) {
                                            if (!it2.hasNext()) {
                                                isSetPolicyResult2 = isSetPolicyResult3;
                                                break;
                                            }
                                            isSetPolicyResult2 = isSetPolicyResult3;
                                            if (newItem.getPolicyName().equals((String) it2.next())) {
                                                struct.addPolicyItem(newItem);
                                                isFound = true;
                                                break;
                                            }
                                            isSetPolicyResult3 = isSetPolicyResult2;
                                        }
                                        if (isFound) {
                                            break;
                                        }
                                        isSetPolicyResult3 = isSetPolicyResult2;
                                    }
                                }
                                saveSettingsLocked(userHandle);
                                isSetPolicyResult = false;
                                combineAllPolicies(userHandle, false);
                                result = 1;
                            } else {
                                isSetPolicyResult = false;
                                HwLog.e(TAG, "onSetPolicy failed, pluginName = " + policyName + ", caller :" + who.flattenToString());
                                result = -1;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        throw th;
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("ComponentName is null");
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public Bundle getPolicy(ComponentName who, String policyName, int userHandle) {
        String str;
        String str2;
        DevicePolicyPlugin plugin = findPluginByPolicyName(policyName);
        if (plugin == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("no plugin found, policyName = ");
            sb.append(policyName);
            sb.append(", caller :");
            if (who == null) {
                str2 = "null";
            } else {
                str2 = who.flattenToString();
            }
            sb.append(str2);
            HwLog.e(TAG, sb.toString());
            return null;
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("get :");
        sb2.append(policyName);
        if (who == null) {
            str = "";
        } else {
            str = " ,cal :" + who.flattenToString();
        }
        sb2.append(str);
        HwLog.d(TAG, sb2.toString());
        Bundle resultBundle = null;
        synchronized (getLockObject()) {
            if (who == null) {
                PolicyStruct.PolicyItem item = (PolicyStruct.PolicyItem) globalPolicyItems.get(policyName);
                if (!userIsolationPolicyList.contains(policyName)) {
                    if (item == null || !item.isSuppportMultipleUsers()) {
                        if (globalPolicyItems.get(policyName) != null) {
                            resultBundle = ((PolicyStruct.PolicyItem) globalPolicyItems.get(policyName)).combineAllAttributes();
                        }
                    }
                }
                resultBundle = combinePoliciesAsUser(policyName, userHandle).combineAllAttributes();
            } else {
                PolicyStruct.PolicyItem item2 = (PolicyStruct.PolicyItem) getHwActiveAdminUncheckedLocked(who, userHandle).adminPolicyItems.get(policyName);
                if (("update-sys-app-install-list".equals(policyName) || "update-sys-app-undetachable-install-list".equals(policyName)) && globalPolicyItems.get(policyName) != null) {
                    resultBundle = ((PolicyStruct.PolicyItem) globalPolicyItems.get(policyName)).combineAllAttributes();
                } else if (item2 != null) {
                    resultBundle = item2.combineAllAttributes();
                }
            }
            notifyOnGetPolicy(plugin, who, policyName, resultBundle);
        }
        return resultBundle;
    }

    private void notifyOnGetPolicy(DevicePolicyPlugin plugin, ComponentName who, String policyName, Bundle policyData) {
        plugin.onGetPolicy(who, policyName, policyData);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:52:0x015e, code lost:
        if (r6 == 1) goto L_0x0162;
     */
    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public int removePolicy(ComponentName who, String policyName, Bundle policyData, int userHandle) {
        String str;
        boolean isGolbalPolicyChanged;
        int result;
        boolean z;
        StringBuilder sb = new StringBuilder();
        sb.append("removePolicy, policyName = ");
        sb.append(policyName);
        sb.append(", caller :");
        if (who == null) {
            str = "null";
        } else {
            str = who.flattenToString();
        }
        sb.append(str);
        HwLog.i(TAG, sb.toString());
        if (who != null) {
            DevicePolicyPlugin plugin = findPluginByPolicyName(policyName);
            if (plugin == null) {
                HwLog.e(TAG, "no plugin found, pluginName = " + policyName + ", caller :" + who.flattenToString());
                return -1;
            } else if (!plugin.checkCallingPermission(who, policyName)) {
                HwLog.e(TAG, "permission denied: " + who.flattenToString());
                return -1;
            } else {
                PolicyStruct struct = findPolicyStructByPolicyName(policyName);
                synchronized (getLockObject()) {
                    AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdminUncheckedLocked(who, userHandle);
                    PolicyStruct.PolicyItem item = null;
                    PolicyStruct.PolicyItem newItem = null;
                    if (struct != null) {
                        item = (PolicyStruct.PolicyItem) admin.adminPolicyItems.get(policyName);
                        if (item != null) {
                            newItem = new PolicyStruct.PolicyItem(policyName, struct.getItemByPolicyName(policyName).getItemType(), struct);
                            newItem.deepCopyFrom(item);
                            newItem.removeAttrValues(newItem, policyData);
                            PolicyStruct.PolicyItem combinedItem = combinePoliciesWithPolicyChanged(who, newItem, policyName, userHandle);
                            PolicyStruct.PolicyItem globalItem = (PolicyStruct.PolicyItem) globalPolicyItems.get(policyName);
                            if (globalItem == null) {
                                newItem.setGlobalPolicyChanged(0);
                                isGolbalPolicyChanged = false;
                            } else if (globalItem.equals(combinedItem)) {
                                newItem.setGlobalPolicyChanged(2);
                                isGolbalPolicyChanged = false;
                            } else {
                                newItem.setGlobalPolicyChanged(1);
                                isGolbalPolicyChanged = true;
                            }
                        } else {
                            isGolbalPolicyChanged = false;
                        }
                    } else {
                        isGolbalPolicyChanged = false;
                    }
                    HwLog.d(TAG, "when removePolicy, is global PolicyChanged ? = " + isGolbalPolicyChanged);
                    if (plugin.onRemovePolicy(who, policyName, policyData, isGolbalPolicyChanged)) {
                        if (!((item == null || policyData == null) ? false : true) || !(item.getItemType() == PolicyStruct.PolicyType.LIST || item.getItemType() == PolicyStruct.PolicyType.CONFIGLIST)) {
                            admin.adminPolicyItems.remove(policyName);
                        } else {
                            admin.adminPolicyItems.put(policyName, newItem);
                        }
                        saveSettingsLocked(userHandle);
                        combineAllPolicies(userHandle, true);
                        result = 1;
                    } else {
                        HwLog.e(TAG, "onSetPolicy failed, pluginName = " + policyName + ", caller :" + who.flattenToString());
                        result = -1;
                    }
                }
                if (result == 1) {
                    this.mHwAdminCache.syncHwAdminCache(policyName, getPolicy(null, policyName, userHandle));
                }
                if (isGolbalPolicyChanged) {
                    z = true;
                }
                z = false;
                plugin.onRemovePolicyCompleted(who, policyName, z);
                return result;
            }
        } else {
            throw new IllegalArgumentException("ComponentName is null");
        }
    }

    /* access modifiers changed from: protected */
    public void notifyPlugins(ComponentName who, int userHandle) {
        StringBuilder sb = new StringBuilder();
        sb.append("notifyPlugins: ");
        sb.append(who == null ? "null" : who.flattenToString());
        sb.append(" userId: ");
        sb.append(userHandle);
        HwLog.i(TAG, sb.toString());
        ActiveAdmin activeAdminToRemove = getActiveAdminUncheckedLocked(who, userHandle);
        if (activeAdminToRemove != null && activeAdminToRemove.mHwActiveAdmin != null) {
            if (activeAdminToRemove.mHwActiveAdmin.disableChangeLauncher) {
                LauncherUtils.clearDefaultLauncher(this.mContext, this.mIPackageManager, userHandle);
            }
            if (activeAdminToRemove.mHwActiveAdmin.adminPolicyItems != null && !activeAdminToRemove.mHwActiveAdmin.adminPolicyItems.isEmpty()) {
                Iterator it = this.globalStructs.iterator();
                while (it.hasNext()) {
                    PolicyStruct struct = (PolicyStruct) it.next();
                    if (struct != null) {
                        ArrayList<PolicyStruct.PolicyItem> removedPluginItems = new ArrayList<>();
                        for (PolicyStruct.PolicyItem removedItem : activeAdminToRemove.mHwActiveAdmin.adminPolicyItems.values()) {
                            if (removedItem != null) {
                                PolicyStruct.PolicyItem combinedItem = combinePoliciesWithoutRemovedPolicyItem(who, removedItem.getPolicyName(), userHandle);
                                if (((PolicyStruct.PolicyItem) globalPolicyItems.get(removedItem.getPolicyName())) == null) {
                                    removedItem.setGlobalPolicyChanged(0);
                                } else if (removedItem.equals(combinedItem)) {
                                    removedItem.setGlobalPolicyChanged(2);
                                } else {
                                    removedItem.setGlobalPolicyChanged(1);
                                    globalPolicyItems.put(removedItem.getPolicyName(), combinedItem);
                                }
                                if (struct.containsPolicyName(removedItem.getPolicyName())) {
                                    removedPluginItems.add(removedItem);
                                }
                            }
                        }
                        notifyPlugins(who, removedPluginItems, struct, userHandle);
                    }
                }
            }
        }
    }

    private void notifyPlugins(ComponentName who, ArrayList<PolicyStruct.PolicyItem> removedPluginItems, PolicyStruct struct, int userHandle) {
        if (!removedPluginItems.isEmpty()) {
            DevicePolicyPlugin plugin = struct.getOwner();
            if (plugin == null) {
                HwLog.w(TAG, " policy struct has no owner");
                return;
            }
            this.effectedItems.add(new AbsDevicePolicyManagerService.EffectedItem(who, plugin, removedPluginItems));
            HwLog.i(TAG, "onActiveAdminRemoving, userHandle: " + userHandle);
            plugin.onActiveAdminRemoved(who, removedPluginItems, userHandle);
        }
    }

    /* access modifiers changed from: protected */
    public void removeActiveAdminCompleted(ComponentName who) {
        synchronized (getLockObject()) {
            if (!this.effectedItems.isEmpty()) {
                Iterator<AbsDevicePolicyManagerService.EffectedItem> it = this.effectedItems.iterator();
                while (it.hasNext()) {
                    AbsDevicePolicyManagerService.EffectedItem effectedItem = it.next();
                    DevicePolicyPlugin plugin = effectedItem.effectedPlugin;
                    if (plugin != null) {
                        if (who.equals(effectedItem.effectedAdmin)) {
                            plugin.onActiveAdminRemovedCompleted(effectedItem.effectedAdmin, effectedItem.effectedPolicies);
                            it.remove();
                        }
                    }
                }
            }
        }
    }

    private DevicePolicyPlugin findPluginByPolicyName(String policyName) {
        PolicyStruct struct = findPolicyStructByPolicyName(policyName);
        if (struct != null) {
            return struct.getOwner();
        }
        return null;
    }

    private PolicyStruct findPolicyStructByPolicyName(String policyName) {
        Iterator it = this.globalStructs.iterator();
        while (it.hasNext()) {
            PolicyStruct struct = (PolicyStruct) it.next();
            if (struct != null && struct.containsPolicyName(policyName)) {
                return struct;
            }
        }
        return null;
    }

    private void combineAllPolicies(int userHandle, boolean shouldChange) {
        PolicyStruct.PolicyItem adminItem;
        synchronized (getLockObject()) {
            DevicePolicyData policy = getUserData(userHandle);
            Iterator it = this.globalStructs.iterator();
            while (it.hasNext()) {
                PolicyStruct struct = (PolicyStruct) it.next();
                for (String policyName : struct.getPolicyMap().keySet()) {
                    Bundle bundle = new Bundle();
                    if ("update-sys-app-install-list".equals(policyName) || "update-sys-app-undetachable-install-list".equals(policyName)) {
                        bundle = struct.getItemByPolicyName(policyName).getAttributes();
                    }
                    PolicyStruct.PolicyItem globalItem = new PolicyStruct.PolicyItem(policyName, struct.getItemByPolicyName(policyName).getItemType(), struct);
                    globalItem.copyFrom(struct.getItemByPolicyName(policyName));
                    int adminSize = policy.mAdminList.size();
                    if (globalItem.getItemType() != PolicyStruct.PolicyType.CONFIGURATION) {
                        for (int i = 0; i < adminSize; i++) {
                            ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                            if (!(admin.mHwActiveAdmin == null || (adminItem = (PolicyStruct.PolicyItem) admin.mHwActiveAdmin.adminPolicyItems.get(policyName)) == null || !adminItem.hasAnyNonNullAttribute())) {
                                traverseCombinePolicyItem(globalItem, adminItem);
                            }
                        }
                    } else if (shouldChange) {
                        globalItem = findGlobleItem(adminSize, policy.mAdminList, policyName, globalItem);
                        if ("update-sys-app-install-list".equals(policyName) || "update-sys-app-undetachable-install-list".equals(policyName)) {
                            globalItem.setAttributes(bundle);
                        }
                    } else {
                        HwLog.w(TAG, "global policy will not change: " + policyName);
                    }
                    globalPolicyItems.put(policyName, globalItem);
                    struct.addPolicyItem(globalItem);
                }
            }
        }
    }

    private PolicyStruct.PolicyItem findGlobleItem(int adminSize, ArrayList<ActiveAdmin> adminList, String policyName, PolicyStruct.PolicyItem globalItem) {
        PolicyStruct.PolicyItem findItem;
        if (adminList == null || globalItem == null) {
            return globalItem;
        }
        int i = adminSize - 1;
        while (i >= 0) {
            ActiveAdmin admin = adminList.get(i);
            if (admin.mHwActiveAdmin == null || (findItem = (PolicyStruct.PolicyItem) admin.mHwActiveAdmin.adminPolicyItems.get(policyName)) == null) {
                i--;
            } else {
                HwLog.w(TAG, "global policy will change: " + policyName);
                return findItem;
            }
        }
        return globalItem;
    }

    private PolicyStruct.PolicyItem combinePoliciesWithPolicyChanged(ComponentName who, PolicyStruct.PolicyItem newItem, String policyName, int userHandle) {
        PolicyStruct.PolicyItem globalAdminItem;
        PolicyStruct.PolicyItem adminItem;
        synchronized (getLockObject()) {
            DevicePolicyData policy = getUserData(userHandle);
            ActiveAdmin activeAdmin = getActiveAdminUncheckedLocked(who, userHandle);
            ArrayList<ActiveAdmin> adminList = new ArrayList<>();
            Iterator it = policy.mAdminList.iterator();
            while (it.hasNext()) {
                adminList.add((ActiveAdmin) it.next());
            }
            if (activeAdmin != null && adminList.size() > 0) {
                adminList.remove(activeAdmin);
            }
            PolicyStruct struct = findPolicyStructByPolicyName(policyName);
            globalAdminItem = new PolicyStruct.PolicyItem(policyName, struct.getItemByPolicyName(policyName).getItemType(), struct);
            globalAdminItem.copyFrom(struct.getItemByPolicyName(policyName));
            Iterator<ActiveAdmin> it2 = adminList.iterator();
            while (it2.hasNext()) {
                ActiveAdmin admin = it2.next();
                if (!(admin.mHwActiveAdmin == null || (adminItem = (PolicyStruct.PolicyItem) admin.mHwActiveAdmin.adminPolicyItems.get(policyName)) == null || !adminItem.hasAnyNonNullAttribute())) {
                    traverseCombinePolicyItem(globalAdminItem, adminItem);
                }
            }
            traverseCombinePolicyItem(globalAdminItem, newItem);
        }
        return globalAdminItem;
    }

    private PolicyStruct.PolicyItem combinePoliciesAsUser(String policyName, int userHandle) {
        PolicyStruct.PolicyItem resultPolicyItem;
        PolicyStruct.PolicyItem policyItemAsAdmin;
        synchronized (getLockObject()) {
            DevicePolicyData policy = getUserData(userHandle);
            ArrayList<ActiveAdmin> adminList = new ArrayList<>();
            Iterator it = policy.mAdminList.iterator();
            while (it.hasNext()) {
                adminList.add((ActiveAdmin) it.next());
            }
            PolicyStruct struct = findPolicyStructByPolicyName(policyName);
            resultPolicyItem = new PolicyStruct.PolicyItem(policyName, struct.getItemByPolicyName(policyName).getItemType(), struct);
            resultPolicyItem.copyFrom(struct.getItemByPolicyName(policyName));
            Iterator<ActiveAdmin> it2 = adminList.iterator();
            while (it2.hasNext()) {
                ActiveAdmin admin = it2.next();
                if (!(admin.mHwActiveAdmin == null || (policyItemAsAdmin = (PolicyStruct.PolicyItem) admin.mHwActiveAdmin.adminPolicyItems.get(policyName)) == null || !policyItemAsAdmin.hasAnyNonNullAttribute())) {
                    traverseCombinePolicyItem(resultPolicyItem, policyItemAsAdmin);
                }
            }
        }
        return resultPolicyItem;
    }

    private PolicyStruct.PolicyItem combinePoliciesWithoutRemovedPolicyItem(ComponentName who, String policyName, int userHandle) {
        PolicyStruct.PolicyItem adminItem;
        DevicePolicyData policy = getUserData(userHandle);
        ArrayList<ActiveAdmin> adminList = new ArrayList<>();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdmin = getActiveAdminUncheckedLocked(who, userHandle);
            Iterator it = policy.mAdminList.iterator();
            while (it.hasNext()) {
                adminList.add((ActiveAdmin) it.next());
            }
            if (activeAdmin != null && adminList.size() > 0) {
                adminList.remove(activeAdmin);
            }
        }
        PolicyStruct struct = findPolicyStructByPolicyName(policyName);
        PolicyStruct.PolicyItem oldItem = struct.getItemByPolicyName(policyName);
        PolicyStruct.PolicyItem globalAdminItem = null;
        if (oldItem != null) {
            globalAdminItem = new PolicyStruct.PolicyItem(policyName, oldItem.getItemType(), struct);
            globalAdminItem.copyFrom(oldItem);
            int adminSize = adminList.size();
            for (int i = 0; i < adminSize; i++) {
                ActiveAdmin admin = adminList.get(i);
                if (admin.mHwActiveAdmin != null && (adminItem = (PolicyStruct.PolicyItem) admin.mHwActiveAdmin.adminPolicyItems.get(policyName)) != null && adminItem.hasAnyNonNullAttribute() && adminItem.getPolicyName().equals(policyName)) {
                    traverseCombinePolicyItem(globalAdminItem, adminItem);
                }
            }
        }
        return globalAdminItem;
    }

    private void traverseCombinePolicyItem(PolicyStruct.PolicyItem oldRoot, PolicyStruct.PolicyItem newRoot) {
        if (oldRoot != null && newRoot != null) {
            oldRoot.setAttributes(combineAttributes(oldRoot.getAttributes(), newRoot.getAttributes(), oldRoot));
            int n = oldRoot.getChildItem().size();
            ArrayList<PolicyStruct.PolicyItem> leafItems = oldRoot.getChildItem();
            for (int i = 0; i < n; i++) {
                if (oldRoot.getItemType() == PolicyStruct.PolicyType.CONFIGLIST && leafItems.get(i).getPolicyStruct() == null) {
                    leafItems.get(i).setPolicyStruct(oldRoot.getPolicyStruct());
                }
                traverseCombinePolicyItem((PolicyStruct.PolicyItem) oldRoot.getChildItem().get(i), (PolicyStruct.PolicyItem) newRoot.getChildItem().get(i));
            }
        }
    }

    /* renamed from: com.android.server.devicepolicy.HwDevicePolicyManagerService$6  reason: invalid class name */
    static /* synthetic */ class AnonymousClass6 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType = new int[PolicyStruct.PolicyType.values().length];

        static {
            try {
                $SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[PolicyStruct.PolicyType.STATE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[PolicyStruct.PolicyType.LIST.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[PolicyStruct.PolicyType.CONFIGLIST.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[PolicyStruct.PolicyType.CONFIGURATION.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    private Bundle combineAttributes(Bundle oldAttr, Bundle newAttr, PolicyStruct.PolicyItem item) {
        int i = AnonymousClass6.$SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[item.getItemType().ordinal()];
        if (i == 1) {
            for (String key : newAttr.keySet()) {
                if (newAttr.get(key) != null) {
                    oldAttr.putBoolean(key, oldAttr.getBoolean(key) || newAttr.getBoolean(key));
                }
            }
        } else if (i == 2) {
            for (String key2 : newAttr.keySet()) {
                if (newAttr.get(key2) != null) {
                    ArrayList<String> oldPolicyList = oldAttr.getStringArrayList(key2);
                    ArrayList<String> newPolicyList = newAttr.getStringArrayList(key2);
                    if (oldPolicyList == null) {
                        oldPolicyList = new ArrayList<>();
                    }
                    HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(oldPolicyList, newPolicyList);
                    oldAttr.putStringArrayList(key2, oldPolicyList);
                }
            }
        } else if (i == 3) {
            for (String key3 : newAttr.keySet()) {
                if (newAttr.get(key3) != null) {
                    ArrayList<String> oldConfigList = oldAttr.getStringArrayList(key3);
                    if (oldConfigList == null) {
                        oldConfigList = new ArrayList<>();
                    }
                    item.addAndUpdateConfigurationList(oldConfigList, newAttr.getStringArrayList(key3));
                    oldAttr.putStringArrayList(key3, oldConfigList);
                }
            }
        } else if (i == 4) {
            for (String key4 : newAttr.keySet()) {
                if (newAttr.get(key4) != null) {
                    oldAttr.putString(key4, newAttr.getString(key4));
                }
            }
        }
        return oldAttr;
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public ArrayList<String> queryBrowsingHistory(ComponentName who, int userHandle) {
        ArrayList<String> historyList = new ArrayList<>();
        if (who != null) {
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_NETWORK_MANAGER", "does not have network MDM permission!");
            enforceHwCrossUserPermission(userHandle);
            getHwActiveAdmin(who, userHandle);
            DevicePolicyPlugin plugin = findPluginByPolicyName("network-black-list");
            if (plugin == null || !(plugin instanceof DeviceNetworkPlugin)) {
                HwLog.e(TAG, "no DeviceNetworkPlugin found, pluginName = network-black-list");
                return historyList;
            }
            long callingId = Binder.clearCallingIdentity();
            ArrayList<String> historyList2 = ((DeviceNetworkPlugin) plugin).queryBrowsingHistory();
            Binder.restoreCallingIdentity(callingId);
            return historyList2;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean hasHwPolicy(int userHandle) {
        HwLog.d(TAG, "hasHwPolicy, userHandle :" + userHandle);
        synchronized (getLockObject()) {
            String path = "";
            try {
                path = new File(Environment.getUserSystemDirectory(userHandle), DEVICE_POLICIES_XML).getCanonicalPath();
            } catch (IOException e) {
                HwLog.e(TAG, "hasHwPolicy : Invalid file path");
            }
            String base = userHandle == 0 ? "/data/system/device_policies.xml" : path;
            File file = new File(base);
            FileInputStream stream = new FileInputStream(new JournaledFile(file, new File(base + ".tmp")).chooseForRead());
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, StandardCharsets.UTF_8.name());
            while (true) {
                int next = parser.next();
                int type = next;
                if (next == 1) {
                    IoUtils.closeQuietly(stream);
                    break;
                } else if (type == 2) {
                    if ("hw_policy".equals(parser.getName())) {
                        while (true) {
                            int next2 = parser.next();
                            type = next2;
                            if (next2 == 1 || type == 3) {
                                break;
                            } else if (type == 2) {
                                HwLog.d(TAG, "find HwPolicy");
                                IoUtils.closeQuietly(stream);
                                return true;
                            }
                        }
                    }
                    if (type == 1) {
                        try {
                            HwLog.d(TAG, "Can't find HwPolicy");
                            return false;
                        } catch (IOException | XmlPullParserException e2) {
                            HwLog.e(TAG, "XmlPullParserException | IOException");
                        } finally {
                            IoUtils.closeQuietly(stream);
                        }
                    }
                }
            }
            HwLog.d(TAG, "Can't find HwPolicy");
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public boolean isSecureBlockEncrypted() {
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x0029  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0032  */
    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public int configVpnProfile(ComponentName who, Bundle para, int userHandle) {
        boolean isParaNull;
        Bundle keyStore;
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission(MDM_VPN_PERMISSION, "NEED MDM_VPN PERMISSION");
        synchronized (getLockObject()) {
            Bundle provider = KeyStore.getInstance();
            if (who != null) {
                if (para != null) {
                    isParaNull = false;
                    if (!isParaNull) {
                        Log.e(TAG, "Bundle para is null or componentName is null!");
                        return -1;
                    } else if (!isValidVpnConfig(para)) {
                        Log.e(TAG, "This Config isn't valid vpnConfig");
                        return -1;
                    } else {
                        VpnProfile profile = getProfile(para);
                        if (!provider.put("VPN_" + profile.key, profile.encode(), -1, 0)) {
                            Log.e(TAG, "Set vpn failed, check the config.");
                            return -1;
                        }
                        String key = para.getString("key");
                        Iterator it = getUserData(userHandle).mAdminList.iterator();
                        while (it.hasNext()) {
                            ActiveAdmin admin = (ActiveAdmin) it.next();
                            if (admin.mHwActiveAdmin == null || admin.mHwActiveAdmin.vpnProviderlist == null) {
                                keyStore = provider;
                            } else {
                                Bundle speProvider = null;
                                Iterator it2 = admin.mHwActiveAdmin.vpnProviderlist.iterator();
                                while (true) {
                                    if (!it2.hasNext()) {
                                        keyStore = provider;
                                        break;
                                    }
                                    Bundle provider2 = (Bundle) it2.next();
                                    keyStore = provider;
                                    if (key.equals(provider2.getString("key"))) {
                                        speProvider = provider2;
                                        break;
                                    }
                                    provider = keyStore;
                                }
                                if (speProvider != null) {
                                    admin.mHwActiveAdmin.vpnProviderlist.remove(speProvider);
                                    saveSettingsLocked(userHandle);
                                }
                            }
                            provider = keyStore;
                        }
                        configVpnProfile(getHwActiveAdmin(who, userHandle), key, para, userHandle);
                        return 1;
                    }
                }
            }
            isParaNull = true;
            if (!isParaNull) {
            }
        }
    }

    private void configVpnProfile(AbsDevicePolicyManagerService.HwActiveAdmin ap, String key, Bundle para, int userHandle) {
        boolean isNeedDelete;
        if (ap != null) {
            if (ap.vpnProviderlist != null) {
                boolean isAlready = false;
                Bundle delProvider = null;
                Iterator it = ap.vpnProviderlist.iterator();
                while (true) {
                    isNeedDelete = true;
                    if (!it.hasNext()) {
                        break;
                    }
                    Bundle provider = (Bundle) it.next();
                    if (!(provider == null || isEmpty(provider.getString("key"))) && key.equals(provider.getString("key"))) {
                        isAlready = true;
                        delProvider = provider;
                        break;
                    }
                }
                if (!isAlready || delProvider == null) {
                    isNeedDelete = false;
                }
                if (isNeedDelete) {
                    ap.vpnProviderlist.remove(delProvider);
                }
                ap.vpnProviderlist.add(para);
                saveSettingsLocked(userHandle);
                return;
            }
            ap.vpnProviderlist = new ArrayList();
            ap.vpnProviderlist.add(para);
            saveSettingsLocked(userHandle);
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public int removeVpnProfile(ComponentName who, Bundle para, int userHandle) {
        String key = para.getString("key");
        if (who == null || isEmpty(key)) {
            Log.e(TAG, "ComponentName or key is empty.");
            return -1;
        }
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission(MDM_VPN_PERMISSION, "NEED MDM_VPN PERMISSION");
        synchronized (getLockObject()) {
            KeyStore keyStore = KeyStore.getInstance();
            boolean isDeleted = false;
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (!(admin.mHwActiveAdmin == null || admin.mHwActiveAdmin.vpnProviderlist == null)) {
                    Bundle specProvider = null;
                    Iterator it2 = admin.mHwActiveAdmin.vpnProviderlist.iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            break;
                        }
                        Bundle provider = (Bundle) it2.next();
                        if (key.equals(provider.getString("key"))) {
                            specProvider = provider;
                            break;
                        }
                    }
                    if (specProvider != null) {
                        if (!keyStore.delete("VPN_" + key)) {
                            Log.e(TAG, "Delete vpn failed, check the key.");
                            return -1;
                        }
                        admin.mHwActiveAdmin.vpnProviderlist.remove(specProvider);
                        saveSettingsLocked(userHandle);
                        isDeleted = true;
                    } else {
                        continue;
                    }
                }
            }
            if (isDeleted) {
                return 1;
            }
            return -1;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public Bundle getVpnProfile(ComponentName who, Bundle keyWords, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission(MDM_VPN_PERMISSION, "NEED MDM_VPN PERMISSION");
        String key = keyWords.getString("key");
        if (isEmpty(key)) {
            Log.e(TAG, "key is null or empty.");
            return null;
        }
        synchronized (getLockObject()) {
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin hwAdmin = getHwActiveAdmin(who, userHandle);
                if (hwAdmin.vpnProviderlist == null) {
                    return null;
                }
                for (Bundle provider : hwAdmin.vpnProviderlist) {
                    if (key.equals(provider.getString("key"))) {
                        return provider;
                    }
                }
                return null;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (!(admin.mHwActiveAdmin == null || admin.mHwActiveAdmin.vpnProviderlist == null)) {
                    for (Bundle provider2 : admin.mHwActiveAdmin.vpnProviderlist) {
                        if (key.equals(provider2.getString("key"))) {
                            return provider2;
                        }
                    }
                    continue;
                }
            }
            return null;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public Bundle getVpnList(ComponentName who, Bundle keyWords, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission(MDM_VPN_PERMISSION, "NEED MDM_VPN PERMISSION");
        synchronized (getLockObject()) {
            ArrayList<String> vpnKeyList = new ArrayList<>();
            Bundle vpnListBundle = new Bundle();
            if (who != null) {
                AbsDevicePolicyManagerService.HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.vpnProviderlist == null) {
                    return null;
                }
                for (Bundle provider : admin.vpnProviderlist) {
                    if (!isEmpty(provider.getString("key"))) {
                        vpnKeyList.add(provider.getString("key"));
                    }
                }
                vpnListBundle.putStringArrayList("keylist", vpnKeyList);
                return vpnListBundle;
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin2 = (ActiveAdmin) it.next();
                if (!(admin2.mHwActiveAdmin == null || admin2.mHwActiveAdmin.vpnProviderlist == null)) {
                    filterVpnKeyList(admin2.mHwActiveAdmin.vpnProviderlist, vpnKeyList);
                }
            }
            vpnListBundle.putStringArrayList("keylist", vpnKeyList);
            return vpnListBundle;
        }
    }

    private void filterVpnKeyList(List<Bundle> vpnProviderlist, ArrayList<String> vpnKeyList) {
        if (vpnProviderlist != null && vpnKeyList != null) {
            for (Bundle provider : vpnProviderlist) {
                if (!isEmpty(provider.getString("key")) && !vpnKeyList.contains(provider.getString("key"))) {
                    vpnKeyList.add(provider.getString("key"));
                }
            }
        }
    }

    private VpnProfile getProfile(Bundle vpnBundle) {
        VpnProfile profile = new VpnProfile(vpnBundle.getString("key"));
        profile.name = vpnBundle.getString("name");
        try {
            profile.type = Integer.parseInt(vpnBundle.getString(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE));
        } catch (NumberFormatException e) {
            HwLog.e(TAG, "proxyPort : NumberFormatException");
        }
        profile.server = vpnBundle.getString("server");
        profile.username = vpnBundle.getString("username");
        profile.password = vpnBundle.getString("password");
        switch (profile.type) {
            case 0:
                profile.mppe = Boolean.parseBoolean(vpnBundle.getString("mppe"));
                break;
            case 1:
                profile.l2tpSecret = vpnBundle.getString("l2tpSecret");
                break;
            case 2:
                profile.l2tpSecret = vpnBundle.getString("l2tpSecret");
                profile.ipsecIdentifier = vpnBundle.getString("ipsecIdentifier");
                profile.ipsecSecret = vpnBundle.getString("ipsecSecret");
                break;
            case 3:
                profile.l2tpSecret = vpnBundle.getString("l2tpSecret");
                profile.ipsecUserCert = vpnBundle.getString("ipsecUserCert");
                profile.ipsecCaCert = vpnBundle.getString("ipsecCaCert");
                profile.ipsecServerCert = vpnBundle.getString("ipsecServerCert");
                break;
            case 4:
                profile.ipsecIdentifier = vpnBundle.getString("ipsecIdentifier");
                profile.ipsecSecret = vpnBundle.getString("ipsecSecret");
                break;
            case 5:
                profile.ipsecUserCert = vpnBundle.getString("ipsecUserCert");
                profile.ipsecCaCert = vpnBundle.getString("ipsecCaCert");
                profile.ipsecServerCert = vpnBundle.getString("ipsecServerCert");
                break;
            case 6:
                profile.ipsecCaCert = vpnBundle.getString("ipsecCaCert");
                profile.ipsecServerCert = vpnBundle.getString("ipsecServerCert");
                break;
        }
        return profile;
    }

    private boolean isValidVpnConfig(Bundle para) {
        if (para == null) {
            return false;
        }
        boolean isKeyAndNameEmpty = isEmpty(para.getString("key")) || isEmpty(para.getString("name"));
        boolean isTypeAndServerEmpty = isEmpty(para.getString(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE)) || isEmpty(para.getString("server"));
        if (isKeyAndNameEmpty || isTypeAndServerEmpty) {
            return false;
        }
        try {
            if (Integer.parseInt(para.getString(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE)) >= 0) {
                if (Integer.parseInt(para.getString(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE)) <= 6) {
                    int parseInt = Integer.parseInt(para.getString(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE));
                    if (parseInt != 2) {
                        if (parseInt != 3) {
                            if (parseInt != 4) {
                                if (parseInt != 5) {
                                    return true;
                                }
                            }
                        }
                        return !isEmpty(para.getString("ipsecUserCert"));
                    }
                    return !isEmpty(para.getString("ipsecSecret"));
                }
            }
            return false;
        } catch (NumberFormatException e) {
            HwLog.e(TAG, "proxyPort : NumberFormatException");
            return false;
        }
    }

    public static boolean isEmpty(String str) {
        return TextUtils.isEmpty(str);
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean formatSDCard(ComponentName who, String diskId, int userHandle) {
        if (who != null) {
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_SDCARD", "does not have sd card MDM permission!");
            synchronized (getLockObject()) {
                if (getActiveAdminUncheckedLocked(who, userHandle) != null) {
                    long token = Binder.clearCallingIdentity();
                    try {
                        ((StorageManager) this.mContext.getSystemService("storage")).partitionPublic(diskId);
                    } catch (ClassCastException e) {
                        HwLog.e(TAG, "format sd card data error! ClassCastException");
                        return false;
                    } catch (Exception e2) {
                        HwLog.e(TAG, "format sd card data error!");
                        return false;
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                } else {
                    throw new SecurityException("No active admin owned by uid " + Binder.getCallingUid() + ", ComponentName:" + who);
                }
            }
            return true;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setAccountDisabled(ComponentName who, String accountType, boolean disabled, int userHandle) {
        if (who != null) {
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app management MDM permission!");
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                if (admin != null) {
                    if (disabled) {
                        admin.accountTypesWithManagementDisabled.add(accountType);
                    } else {
                        admin.accountTypesWithManagementDisabled.remove(accountType);
                    }
                    saveSettingsLocked(userHandle);
                } else {
                    throw new SecurityException("No active admin owned by uid " + Binder.getCallingUid() + ", ComponentName:" + who);
                }
            }
            return;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean isAccountDisabled(ComponentName who, String accountType, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                if (admin != null) {
                    return admin.accountTypesWithManagementDisabled.contains(accountType);
                }
            }
            Iterator it = getUserData(userHandle).mAdminList.iterator();
            while (it.hasNext()) {
                if (((ActiveAdmin) it.next()).accountTypesWithManagementDisabled.contains(accountType)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean installCertificateWithType(ComponentName who, int type, byte[] certBuffer, String alias, String password, int certInstallType, boolean requestAccess, int userHandle) {
        boolean isSuccess;
        long id;
        checkEnvBeforeCallInterface(who, userHandle);
        if (type == 0) {
            try {
                isSuccess = CertInstallHelper.installPkcs12Cert(password, certBuffer, alias, certInstallType);
            } catch (Exception e) {
                HwLog.e(TAG, "throw error when install cert");
                return false;
            }
        } else if (type == 1) {
            isSuccess = CertInstallHelper.installX509Cert(certBuffer, alias, certInstallType);
        } else {
            HwLog.e(TAG, "throw error when install cert");
            return false;
        }
        if (!requestAccess) {
            return isSuccess;
        }
        int callingUid = this.mInjector.binderGetCallingUid();
        id = this.mInjector.binderClearCallingIdentity();
        try {
            KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, UserHandle.getUserHandleForUid(callingUid));
            try {
                keyChainConnection.getService().setGrant(callingUid, alias, true);
                keyChainConnection.close();
                this.mInjector.binderRestoreCallingIdentity(id);
                return true;
            } catch (RemoteException e2) {
                HwLog.e(TAG, "set grant certificate");
                keyChainConnection.close();
            } catch (Throwable th) {
                keyChainConnection.close();
                throw th;
            }
        } catch (InterruptedException e3) {
            HwLog.w(TAG, "Interrupted while set granting certificate");
            Thread.currentThread().interrupt();
        } catch (Throwable th2) {
            this.mInjector.binderRestoreCallingIdentity(id);
            throw th2;
        }
        this.mInjector.binderRestoreCallingIdentity(id);
        return false;
    }

    private void checkEnvBeforeCallInterface(ComponentName who, int userHandle) {
        if (who != null) {
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have install cert MDM permission!");
            synchronized (getLockObject()) {
                if (getActiveAdminUncheckedLocked(who, userHandle) == null) {
                    throw new SecurityException("No active admin owned by uid " + Binder.getCallingUid() + ", ComponentName:" + who);
                }
            }
            return;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    /* access modifiers changed from: protected */
    public long getUsrSetExtendTime() {
        String value = getPolicy(null, PASSWORD_CHANGE_EXTEND_TIME, UserHandle.myUserId()).getString("value");
        if (value == null || "".equals(value)) {
            return HwDevicePolicyManagerService.super.getUsrSetExtendTime();
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            HwLog.e(TAG, "getUsrSetExtendTime : NumberFormatException");
            return HwDevicePolicyManagerService.super.getUsrSetExtendTime();
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setSilentActiveAdmin(ComponentName who, int userHandle) {
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have PERMISSION_MDM_DEVICE_MANAGER permission!");
        synchronized (getLockObject()) {
            if (who == null) {
                throw new IllegalArgumentException("ComponentName is null");
            } else if (!isAdminActive(who, userHandle)) {
                HwLog.d(TAG, "setSilentActiveAdmin, IS_HAS_HW_MDM_FEATURE active supported.");
                long identityToken = Binder.clearCallingIdentity();
                try {
                    setActiveAdmin(who, true, userHandle);
                } finally {
                    Binder.restoreCallingIdentity(identityToken);
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void monitorFactoryReset(String component, String reason) {
        if (this.mMonitor == null || TextUtils.isEmpty(reason)) {
            HwLog.e(TAG, "monitorFactoryReset: Invalid parameter,mMonitor=" + this.mMonitor + ", reason=" + reason);
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("component", component);
        bundle.putString("reason", reason);
        this.mMonitor.monitor(907400018, bundle);
    }

    /* access modifiers changed from: protected */
    public void clearWipeDataFactoryLowlevel(String reason, boolean wipeEuicc) {
        HwLog.d(TAG, "wipeData, reason=" + reason + ", wipeEuicc=" + wipeEuicc);
        Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
        intent.addFlags(285212672);
        intent.putExtra("masterClearWipeDataFactoryLowlevel", true);
        intent.putExtra("com.android.internal.intent.extra.WIPE_ESIMS", wipeEuicc);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
    }

    public void loadHwSpecialPolicyFromXml(XmlPullParser parser) {
        if (parser != null) {
            String tag = parser.getName();
            if ("update-sys-app-install-list".equals(tag) || "update-sys-app-undetachable-install-list".equals(tag)) {
                String value = parser.getAttributeValue(null, "value");
                Bundle bundle = new Bundle();
                bundle.putString("value", value);
                HwLog.d(TAG, "loadHwSpecialPolicyFromXml value:" + value);
                int size = this.globalStructs.size();
                for (int i = 0; i < size; i++) {
                    PolicyStruct.PolicyItem globalItem = ((PolicyStruct) this.globalStructs.get(i)).getPolicyItem(tag);
                    if (globalItem != null) {
                        globalItem.setAttributes(bundle);
                        HwLog.d(TAG, "loadHwSpecialPolicyFromXml find:" + tag + " in globalStructs and update its value");
                    }
                }
            }
        }
    }

    public void setHwSpecialPolicyToXml(XmlSerializer out) {
        Bundle bundle;
        if (out != null) {
            ArrayList<String> specialPolicies = new ArrayList<>();
            specialPolicies.add("update-sys-app-install-list");
            specialPolicies.add("update-sys-app-undetachable-install-list");
            int size = specialPolicies.size();
            for (int i = 0; i < size; i++) {
                String policyName = specialPolicies.get(i);
                PolicyStruct.PolicyItem globalItem = (PolicyStruct.PolicyItem) globalPolicyItems.get(policyName);
                if (!(globalItem == null || (bundle = globalItem.getAttributes()) == null || bundle.isEmpty())) {
                    String value = bundle.getString("value");
                    HwLog.d(TAG, "setHwSpecialPolicyToXml " + value);
                    if (!TextUtils.isEmpty(value)) {
                        try {
                            out.startTag(null, policyName);
                            out.attribute(null, "value", value);
                            out.endTag(null, policyName);
                        } catch (IOException e) {
                            HwLog.d(TAG, "failed parsing when try to setHwSpecialPolicyToXml");
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public int setSystemLanguage(ComponentName who, Bundle bundle, int userHandle) {
        if (bundle == null || TextUtils.isEmpty(bundle.getString("locale"))) {
            throw new IllegalArgumentException("locale is empty.");
        }
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "Does not have mdm_device_manager permission.");
        synchronized (getLockObject()) {
            if (who != null) {
                getHwActiveAdmin(who, userHandle);
                long callingId = Binder.clearCallingIdentity();
                try {
                    Locale locale = Locale.forLanguageTag(bundle.getString("locale"));
                    IActivityManager am = ActivityManagerNative.getDefault();
                    Configuration config = am.getConfiguration();
                    config.setLocale(locale);
                    config.userSetLocale = true;
                    am.updateConfiguration(config);
                    Settings.System.putStringForUser(this.mContext.getContentResolver(), "system_locales", locale.toLanguageTag(), userHandle);
                    BackupManager.dataChanged("com.android.providers.settings");
                } catch (RemoteException e) {
                    Slog.w(TAG, "failed to set system language RemoteException");
                } catch (Exception e2) {
                    Slog.w(TAG, "failed to set system language");
                } finally {
                    Binder.restoreCallingIdentity(callingId);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        return 1;
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void setDeviceOwnerApp(ComponentName admin, String ownerName, int userId) {
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
        enforceHwCrossUserPermission(userId);
        synchronized (getLockObject()) {
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                this.mIsMDMDeviceOwnerAPI = true;
                setDeviceOwner(admin, ownerName, userId);
            } finally {
                this.mIsMDMDeviceOwnerAPI = false;
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
    }

    public boolean setDeviceOwner(ComponentName admin, String ownerName, int userId) {
        boolean isSuccess = HwDevicePolicyManagerService.super.setDeviceOwner(admin, ownerName, userId);
        HwDevicePolicyManagerServiceUtil.collectMdmDoSuccessDftData(admin.getPackageName());
        setDpcInAELaunchableAndBackgroundRunnable(true);
        return isSuccess;
    }

    public boolean setProfileOwner(ComponentName who, String ownerName, int userHandle) {
        boolean isSuccess = HwDevicePolicyManagerService.super.setProfileOwner(who, ownerName, userHandle);
        HwDevicePolicyManagerServiceUtil.collectMdmWpSuccessDftData(who.getPackageName());
        setDpcInAELaunchableAndBackgroundRunnable(true);
        return isSuccess;
    }

    /* JADX INFO: finally extract failed */
    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void clearDeviceOwnerApp(int userId) {
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
        enforceHwCrossUserPermission(userId);
        synchronized (getLockObject()) {
            try {
                ComponentName component = this.mOwners.getDeviceOwnerComponent();
                if (!this.mOwners.hasDeviceOwner() || component == null) {
                    throw new IllegalArgumentException("The device owner is not set up.");
                }
                this.mIsMDMDeviceOwnerAPI = true;
                HwDevicePolicyManagerService.super.clearDeviceOwner(component.getPackageName());
                this.mIsMDMDeviceOwnerAPI = false;
            } catch (Throwable th) {
                this.mIsMDMDeviceOwnerAPI = false;
                throw th;
            }
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void turnOnMobiledata(ComponentName who, boolean on, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_NETWORK_MANAGER", "does not have MDM_NETWORK_MANAGER permission!");
        if (who != null) {
            synchronized (getLockObject()) {
                getHwActiveAdmin(who, userHandle);
            }
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            if (on) {
                try {
                    phone.enableDataConnectivity();
                } catch (RemoteException e) {
                    HwLog.e(TAG, "Can not calling the remote function to set data enabled!");
                }
            } else {
                phone.disableDataConnectivity();
            }
        } else {
            throw new IllegalArgumentException("ComponentName is null");
        }
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean setCarrierLockScreenPassword(ComponentName who, String password, String phoneNumber, int userHandle) {
        boolean extendLockScreenPassword;
        if (who != null) {
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_KEYGUARD", "does not have keyguard MDM permission!");
            synchronized (getLockObject()) {
                if (getActiveAdminUncheckedLocked(who, userHandle) != null) {
                    long token = Binder.clearCallingIdentity();
                    try {
                        extendLockScreenPassword = new LockPatternUtilsEx(this.mContext).setExtendLockScreenPassword(password, phoneNumber, userHandle);
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                } else {
                    throw new SecurityException("No active admin owned by uid " + Binder.getCallingUid() + ", ComponentName:" + who);
                }
            }
            return extendLockScreenPassword;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean clearCarrierLockScreenPassword(ComponentName who, String password, int userHandle) {
        boolean clearExtendLockScreenPassword;
        if (who != null) {
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_KEYGUARD", "does not have keyguard MDM permission!");
            synchronized (getLockObject()) {
                if (getActiveAdminUncheckedLocked(who, userHandle) != null) {
                    long token = Binder.clearCallingIdentity();
                    try {
                        clearExtendLockScreenPassword = new LockPatternUtilsEx(this.mContext).clearExtendLockScreenPassword(password, userHandle);
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                } else {
                    throw new SecurityException("No active admin owned by uid " + Binder.getCallingUid() + ", ComponentName:" + who);
                }
            }
            return clearExtendLockScreenPassword;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public void resetNetorkSetting(ComponentName who, int userHandle) {
        if (who != null) {
            synchronized (getLockObject()) {
                getHwActiveAdmin(who, userHandle);
            }
            enforceHwCrossUserPermission(userHandle);
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_NETWORK_MANAGER", "does not have com.huawei.permission.sec.MDM_NETWORK_MANAGER !");
            Intent intent = new Intent("com.android.settings.mdm.receiver.action.MDMPolicyResetNetworkSetting");
            intent.setComponent(new ComponentName("com.android.settings", SettingsMDMPlugin.SETTINGS_MDM_RECEIVER));
            this.mContext.sendBroadcast(intent);
            return;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public Bundle getTopAppPackageName(ComponentName who, int userHandle) {
        if (who != null) {
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "Does not hava manager app MDM permission.");
            synchronized (getLockObject()) {
                getHwActiveAdmin(who, userHandle);
            }
            long callingId = Binder.clearCallingIdentity();
            Bundle bundle = new Bundle();
            ActivityInfo lastResumeActivity = HwActivityTaskManager.getLastResumedActivity();
            if (lastResumeActivity != null) {
                bundle.putString("value", lastResumeActivity.packageName);
            }
            Binder.restoreCallingIdentity(callingId);
            return bundle;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    @Override // com.android.server.devicepolicy.IHwDevicePolicyManager
    public boolean setDefaultDataCard(ComponentName who, int slotId, Message response, int userHandle) {
        if (who != null) {
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_NETWORK_MANAGER", "does not have PERMISSION_MDM_NETWORK_MANAGER!");
            synchronized (getLockObject()) {
                getHwActiveAdmin(who, userHandle);
            }
            long token = Binder.clearCallingIdentity();
            if (slotId >= 0) {
                try {
                    if (slotId < TelephonyManager.getDefault().getPhoneCount()) {
                        if (isAirplaneModeOn(this.mContext)) {
                            Log.i(TAG, "In air plane mode, can not change slot.");
                            return sendMessageForSetDefaultDataCard(response, "AIR_PLANE_MODE_ON");
                        } else if (HwTelephonyManagerInner.getDefault().getSubState((long) slotId) == 0) {
                            Log.i(TAG, "Target slot id [" + slotId + "] is inactive, can not change slot.");
                            boolean sendMessageForSetDefaultDataCard = sendMessageForSetDefaultDataCard(response, "SUBSCRIPTION_INACTIVE");
                            Binder.restoreCallingIdentity(token);
                            return sendMessageForSetDefaultDataCard;
                        } else if (HwTelephonyManagerInner.getDefault().getDefault4GSlotId() == slotId) {
                            Log.i(TAG, "Main slot id is " + slotId + ", no need to change.");
                            boolean sendMessageForSetDefaultDataCard2 = sendMessageForSetDefaultDataCard(response, null);
                            Binder.restoreCallingIdentity(token);
                            return sendMessageForSetDefaultDataCard2;
                        } else if (!HwTelephonyManagerInner.getDefault().isSetDefault4GSlotIdEnabled()) {
                            Log.i(TAG, "Can not set default main slot:" + slotId);
                            boolean sendMessageForSetDefaultDataCard3 = sendMessageForSetDefaultDataCard(response, "GENERIC_FAILURE");
                            Binder.restoreCallingIdentity(token);
                            return sendMessageForSetDefaultDataCard3;
                        } else {
                            HwTelephonyManagerInner.getDefault().setDefault4GSlotId(slotId, response);
                            Binder.restoreCallingIdentity(token);
                            return true;
                        }
                    }
                } catch (IllegalStateException e) {
                    Log.w(TAG, "IllegalStateException occured when set default data card!");
                    return sendMessageForSetDefaultDataCard(response, "GENERIC_FAILURE");
                } catch (Exception e2) {
                    Log.w(TAG, "Set default data card error!");
                    return sendMessageForSetDefaultDataCard(response, "GENERIC_FAILURE");
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
            Log.i(TAG, "Invalid slot ID = " + slotId);
            boolean sendMessageForSetDefaultDataCard4 = sendMessageForSetDefaultDataCard(response, "GENERIC_FAILURE");
            Binder.restoreCallingIdentity(token);
            return sendMessageForSetDefaultDataCard4;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    private boolean isAirplaneModeOn(Context context) {
        if (context == null || Settings.Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) == 0) {
            return false;
        }
        return true;
    }

    private boolean sendMessageForSetDefaultDataCard(Message response, String exception) {
        if (response == null || response.replyTo == null) {
            return false;
        }
        Bundle data = new Bundle();
        if (exception != null) {
            data.putBoolean("RESULT", false);
            data.putString("EXCEPTION", exception);
        } else {
            data.putBoolean("RESULT", true);
        }
        response.setData(data);
        try {
            response.replyTo.send(response);
            if (exception == null) {
                return true;
            }
            return false;
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException occured when send response to the third party apk!");
            return false;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isActiveAdminWithPolicyForUserLocked(ActiveAdmin admin, int reqPolicy, int userId) {
        boolean isLocked = HwDevicePolicyManagerService.super.isActiveAdminWithPolicyForUserLocked(admin, reqPolicy, userId);
        Bundle bundle = getPolicy(null, "policy-deprecated-admin-interfaces-enabled", userId);
        boolean isSupportDeprecated = false;
        if (bundle == null) {
            HwLog.e(TAG, "The bundle is null");
        } else {
            isSupportDeprecated = bundle.getBoolean("value");
        }
        if (!isLocked) {
            return DA_DISALLOWED_POLICIES.contains(Integer.valueOf(reqPolicy)) && isSupportDeprecated;
        }
        return isLocked;
    }

    private void setDpcInAELaunchableAndBackgroundRunnable(boolean executeImmediately) {
        Log.i(TAG, "setDpcInAELaunchableAndBackgroundRunnable from calling uuid" + Binder.getCallingUid());
        ComponentName deviceOwnerComponent = null;
        try {
            deviceOwnerComponent = getDeviceOwnerComponent(true);
        } catch (SecurityException e) {
            Log.e(TAG, "setDpcInAE->getDeviceOwnerComponent failed with SecurityException");
        }
        if (deviceOwnerComponent != null) {
            setPackageLaunchableAndBackgroundRunable(deviceOwnerComponent.getPackageName(), executeImmediately);
        } else {
            Log.i(TAG, "No device owner found");
        }
        for (UserInfo ui : this.mUserManager.getUsers(true)) {
            Log.i(TAG, "setDpcInAELaunchableAndBackgroundRunnable iterate user with id:" + ui.id);
            ComponentName profileName = getProfileOwner(ui.id);
            if (profileName != null) {
                setPackageLaunchableAndBackgroundRunable(profileName.getPackageName(), executeImmediately);
            } else {
                Log.i(TAG, "No profile owner found");
            }
        }
    }

    private void setPackageLaunchableAndBackgroundRunable(String packageName, boolean executeImmediately) {
        final List<HwAppStartupSettingEx> aePackageConfigs = new ArrayList<>();
        aePackageConfigs.add(new HwAppStartupSettingEx(packageName, POLICY_CATEGARY, MODIFY_CATEGARY, SHOW_CATAGARY));
        Runnable runnable = new Runnable() {
            /* class com.android.server.devicepolicy.HwDevicePolicyManagerService.AnonymousClass5 */

            public void run() {
                boolean isIAwareAvailable = false;
                int i = 0;
                while (true) {
                    if (((double) i) >= HwDevicePolicyManagerService.MAX_RETRY_TIMES) {
                        break;
                    }
                    IMultiTaskManager itf = HwIAwareManager.getMultiTaskManager();
                    if (itf != null) {
                        long identity = Binder.clearCallingIdentity();
                        try {
                            boolean isResult = itf.updateAppStartupSettings(aePackageConfigs, false);
                            isIAwareAvailable = true;
                            HwLog.i(HwDevicePolicyManagerService.TAG, "setPackageLaunchableAndBackgroundRunable result:" + isResult);
                        } catch (RemoteException e) {
                            HwLog.e(HwDevicePolicyManagerService.TAG, "updateStartupSettings ex");
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(identity);
                            throw th;
                        }
                        Binder.restoreCallingIdentity(identity);
                        break;
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e2) {
                        HwLog.e(HwDevicePolicyManagerService.TAG, "reTry app whitelist failed");
                    }
                    i++;
                }
                if (!isIAwareAvailable) {
                    HwLog.e(HwDevicePolicyManagerService.TAG, "IMultiTskMngerService unavailable after times retry ");
                }
            }
        };
        ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
        if (!executeImmediately) {
            worker.schedule(runnable, 3000, TimeUnit.MILLISECONDS);
        } else {
            worker.schedule(runnable, 0, TimeUnit.MILLISECONDS);
        }
    }
}
