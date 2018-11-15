package com.android.server.devicepolicy;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
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
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hdm.HwDeviceManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.IPowerManager.Stub;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
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
import com.android.server.devicepolicy.AbsDevicePolicyManagerService.EffectedItem;
import com.android.server.devicepolicy.AbsDevicePolicyManagerService.HwActiveAdmin;
import com.android.server.devicepolicy.DevicePolicyManagerService.ActiveAdmin;
import com.android.server.devicepolicy.DevicePolicyManagerService.DevicePolicyData;
import com.android.server.devicepolicy.PolicyStruct.PolicyItem;
import com.android.server.devicepolicy.PolicyStruct.PolicyType;
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
import com.android.server.devicepolicy.plugins.HwEmailMDMPlugin;
import com.android.server.devicepolicy.plugins.HwSystemManagerPlugin;
import com.android.server.devicepolicy.plugins.PhoneManagerPlugin;
import com.android.server.devicepolicy.plugins.SettingsMDMPlugin;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.mtm.taskstatus.ProcessCleaner;
import com.android.server.security.securitydiagnose.HwSecDiagnoseConstant;
import com.huawei.android.widget.LockPatternUtilsEx;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class HwDevicePolicyManagerService extends DevicePolicyManagerService implements IHwDevicePolicyManager {
    public static final int CERTIFICATE_PEM_BASE64 = 1;
    public static final int CERTIFICATE_PKCS12 = 0;
    private static final boolean DBG = false;
    public static final String DESCRIPTOR = "com.android.internal.widget.ILockSettings";
    private static final String DEVICE_POLICIES_1_XML = "device_policies_1.xml";
    protected static final String DPMDESCRIPTOR = "android.app.admin.IDevicePolicyManager";
    public static final String DYNAMIC_ROOT_PROP = "persist.sys.root.status";
    public static final String DYNAMIC_ROOT_STATE_SAFE = "0";
    private static final String EXCHANGE_DOMAIN = "domain";
    private static final int EXCHANGE_PROVIDER_MAX_NUM = 20;
    private static final int FAILED = -1;
    private static final Set<String> HWDEVICE_OWNER_USER_RESTRICTIONS = new HashSet();
    private static final String KEY = "key";
    private static final int MAX_QUERY_PROCESS = 10000;
    private static final String MDM_VPN_PERMISSION = "com.huawei.permission.sec.MDM_VPN";
    public static final int NOT_SUPPORT_SD_CRYPT = -1;
    private static final int OFF = 0;
    private static final int ON = 1;
    private static final String PASSWORD_CHANGE_EXTEND_TIME = "pwd-password-change-extendtime";
    public static final String PRIVACY_MODE_ON = "privacy_mode_on";
    public static final int SD_CRYPT_STATE_DECRYPTED = 1;
    public static final int SD_CRYPT_STATE_DECRYPTING = 4;
    public static final int SD_CRYPT_STATE_ENCRYPTED = 2;
    public static final int SD_CRYPT_STATE_ENCRYPTING = 3;
    public static final int SD_CRYPT_STATE_INVALID = 0;
    public static final int SD_CRYPT_STATE_MISMATCH = 5;
    public static final int SD_CRYPT_STATE_WAIT_UNLOCK = 6;
    private static final String SETTINGS_MENUS_REMOVE = "settings_menus_remove";
    private static final int STATUS_BAR_DISABLE_MASK = 34013184;
    private static final int SUCCEED = 1;
    private static final String TAG = "HwDPMS";
    private static final String USB_STORAGE = "usb";
    private static ArrayList<String> USER_ISOLATION_POLICY_LIST = new ArrayList<String>() {
        {
            add("email-disable-delete-account");
            add("email-disable-add-account");
            add("allowing-addition-black-list");
        }
    };
    private static boolean isSimplePwdOpen = SystemProperties.getBoolean("ro.config.not_allow_simple_pwd", false);
    private static final boolean isSupportCrypt = SystemProperties.getBoolean("ro.config.support_sdcard_crypt", true);
    private static final boolean mHasHwMdmFeature = true;
    public static final int transaction_setActiveVisitorPasswordState = 1003;
    private boolean hasInit = false;
    private final Context mContext;
    private AlertDialog mErrorDialog;
    private HwAdminCache mHwAdminCache;
    private HwFrameworkMonitor mMonitor = HwFrameworkFactory.getHwFrameworkMonitor();
    private TransactionProcessor mProcessor = null;
    private final UserManager mUserManager;
    final SparseArray<DeviceVisitorPolicyData> mVisitorUserData = new SparseArray();

    /* renamed from: com.android.server.devicepolicy.HwDevicePolicyManagerService$5 */
    static /* synthetic */ class AnonymousClass5 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType = new int[PolicyType.values().length];

        static {
            try {
                $SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[PolicyType.STATE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[PolicyType.LIST.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[PolicyType.CONFIGURATION.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
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

        public DeviceVisitorPolicyData(int userHandle) {
            this.mUserHandle = userHandle;
        }
    }

    static {
        HWDEVICE_OWNER_USER_RESTRICTIONS.add("no_usb_file_transfer");
        HWDEVICE_OWNER_USER_RESTRICTIONS.add("no_physical_media");
        HWDEVICE_OWNER_USER_RESTRICTIONS.add("no_outgoing_calls");
        HWDEVICE_OWNER_USER_RESTRICTIONS.add("no_sms");
        HWDEVICE_OWNER_USER_RESTRICTIONS.add("no_config_tethering");
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
                    for (PolicyItem item : struct.getPolicyItems()) {
                        if (item == null) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("policyItem is null in plugin: ");
                            stringBuilder.append(plugin.getPluginName());
                            HwLog.w(str, stringBuilder.toString());
                        }
                    }
                    addPolicyStruct(struct);
                }
            }
        }
    }

    DeviceVisitorPolicyData getVisitorUserData(int userHandle) {
        DeviceVisitorPolicyData policy;
        synchronized (getLockObject()) {
            policy = (DeviceVisitorPolicyData) this.mVisitorUserData.get(userHandle);
            if (policy == null) {
                policy = new DeviceVisitorPolicyData(userHandle);
                this.mVisitorUserData.append(userHandle, policy);
                loadVisitorSettingsLocked(policy, userHandle);
            }
        }
        return policy;
    }

    private static JournaledFile makeJournaledFile2(int userHandle) {
        String base;
        if (userHandle == 0) {
            base = "/data/system/device_policies_1.xml";
        } else {
            base = new File(Environment.getUserSystemDirectory(userHandle), DEVICE_POLICIES_1_XML).getAbsolutePath();
        }
        File file = new File(base);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(base);
        stringBuilder.append(".tmp");
        return new JournaledFile(file, new File(stringBuilder.toString()));
    }

    private void saveVisitorSettingsLock(int userHandle) {
        DeviceVisitorPolicyData policy = getVisitorUserData(userHandle);
        JournaledFile journal = makeJournaledFile2(userHandle);
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(journal.chooseForWrite(), false);
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(stream, "utf-8");
            out.startDocument(null, Boolean.valueOf(true));
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
                stream.close();
            } catch (IOException e) {
            }
        } catch (IOException e2) {
            journal.rollback();
            if (stream != null) {
                stream.close();
            }
        } catch (Throwable th) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e3) {
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:27:?, code:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:78:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadVisitorSettingsLocked(DeviceVisitorPolicyData policy, int userHandle) {
        String str;
        StringBuilder stringBuilder;
        FileInputStream stream = null;
        File file = makeJournaledFile2(userHandle).chooseForRead();
        try {
            int type;
            String tag;
            stream = new FileInputStream(file);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, null);
            while (true) {
                int next = parser.next();
                type = next;
                if (next == 1 || type == 2) {
                    tag = parser.getName();
                }
            }
            tag = parser.getName();
            if ("policies".equals(tag)) {
                type = parser.next();
                int outerDepth = parser.getDepth();
                while (true) {
                    int next2 = parser.next();
                    type = next2;
                    if (next2 == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                        try {
                            break;
                        } catch (IOException e) {
                            return;
                        }
                    } else if (type != 3) {
                        if (type != 4) {
                            tag = parser.getName();
                            if ("active-password2".equals(tag)) {
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
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Unknown tag: ");
                                stringBuilder2.append(tag);
                                Slog.w(str2, stringBuilder2.toString());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                }
            } else {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Settings do not start with policies tag: found ");
                stringBuilder3.append(tag);
                throw new XmlPullParserException(stringBuilder3.toString());
            }
        } catch (NullPointerException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e2);
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (NumberFormatException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e3);
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (XmlPullParserException e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e4);
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (FileNotFoundException e5) {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e6) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e6);
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (IndexOutOfBoundsException e7) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e7);
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (Throwable th) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e8) {
                }
            }
        }
    }

    private boolean isPrivacyModeEnabled() {
        return Secure.getInt(this.mContext.getContentResolver(), PRIVACY_MODE_ON, 0) == 1 && isFeatrueSupported();
    }

    private static boolean isFeatrueSupported() {
        return SystemProperties.getBoolean("ro.config.hw_privacymode", false);
    }

    public void systemReady(int phase) {
        super.systemReady(phase);
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
    }

    /* JADX WARNING: Missing block: B:32:0x0071, code:
            return r1;
     */
    /* JADX WARNING: Missing block: B:34:0x0073, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isActivePasswordSufficient(int userHandle, boolean parent) {
        if (!isPrivacyModeEnabled()) {
            return super.isActivePasswordSufficient(userHandle, parent);
        }
        boolean z = false;
        if (!super.isActivePasswordSufficient(userHandle, parent)) {
            return false;
        }
        Slog.w(TAG, "super is ActivePassword Sufficient");
        if (!this.mHasFeature) {
            return true;
        }
        synchronized (getLockObject()) {
            DeviceVisitorPolicyData policy = getVisitorUserData(userHandle);
            if (policy.mActivePasswordQuality < getPasswordQuality(null, userHandle, parent) || policy.mActivePasswordLength < getPasswordMinimumLength(null, userHandle, parent)) {
            } else if (policy.mActivePasswordQuality != 393216) {
                return true;
            } else if (policy.mActivePasswordUpperCase >= getPasswordMinimumUpperCase(null, userHandle, parent) && policy.mActivePasswordLowerCase >= getPasswordMinimumLowerCase(null, userHandle, parent) && policy.mActivePasswordLetters >= getPasswordMinimumLetters(null, userHandle, parent) && policy.mActivePasswordNumeric >= getPasswordMinimumNumeric(null, userHandle, parent) && policy.mActivePasswordSymbols >= getPasswordMinimumSymbols(null, userHandle, parent) && policy.mActivePasswordNonLetter >= getPasswordMinimumNonLetter(null, userHandle, parent)) {
                z = true;
            }
        }
    }

    public void setActiveVisitorPasswordState(int quality, int length, int letters, int uppercase, int lowercase, int numbers, int symbols, int nonletter, int userHandle) {
        if (this.mHasFeature) {
            enforceFullCrossUsersPermission(userHandle);
            this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
            DeviceVisitorPolicyData p = getVisitorUserData(userHandle);
            validateQualityConstant(quality);
            synchronized (getLockObject()) {
                if (!(p.mActivePasswordQuality == quality && p.mActivePasswordLength == length && p.mFailedPasswordAttempts == 0 && p.mActivePasswordLetters == letters && p.mActivePasswordUpperCase == uppercase && p.mActivePasswordLowerCase == lowercase && p.mActivePasswordNumeric == numbers && p.mActivePasswordSymbols == symbols && p.mActivePasswordNonLetter == nonletter)) {
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

    void setAllowSimplePassword(ComponentName who, boolean mode, int userHandle) {
        if (this.mHasFeature && isSimplePwdOpen) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setAllowSimplePassword mode =");
            stringBuilder.append(mode);
            HwLog.d(str, stringBuilder.toString());
            synchronized (getLockObject()) {
                if (who != null) {
                    ActiveAdmin ap = getActiveAdminForCallerLocked(who, null);
                    if (ap.allowSimplePassword != mode) {
                        ap.allowSimplePassword = mode;
                        saveSettingsLocked(userHandle);
                    }
                } else {
                    throw new NullPointerException("ComponentName is null");
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:14:0x001c, code:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getAllowSimplePassword(ComponentName who, int userHandle) {
        if (!this.mHasFeature || !isSimplePwdOpen) {
            return true;
        }
        synchronized (getLockObject()) {
            boolean mode = true;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                boolean z = admin != null ? admin.allowSimplePassword : true;
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (mode && mode != admin2.allowSimplePassword) {
                        mode = admin2.allowSimplePassword;
                    }
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getAllowSimplePassword mode =");
                stringBuilder.append(mode);
                HwLog.d(str, stringBuilder.toString());
                return mode;
            }
        }
    }

    void saveCurrentPwdStatus(boolean isCurrentPwdSimple, int userHandle) {
        if (this.mHasFeature && isSimplePwdOpen) {
            synchronized (getLockObject()) {
                getUserData(userHandle).mIsCurrentPwdSimple = isCurrentPwdSimple;
            }
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        int i = code;
        Parcel parcel = data;
        Parcel parcel2 = reply;
        if (this.mProcessor.processTransaction(i, parcel, parcel2) || this.mProcessor.processTransactionWithPolicyName(i, parcel, parcel2)) {
            return true;
        }
        if (i != 1003) {
            ComponentName _arg0 = null;
            boolean _arg1 = false;
            switch (i) {
                case 7001:
                    if (isSimplePwdOpen) {
                        parcel.enforceInterface(DPMDESCRIPTOR);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setAllowSimplePassword(_arg0, _arg1, data.readInt());
                        reply.writeNoException();
                        return true;
                    }
                    break;
                case 7002:
                    if (isSimplePwdOpen) {
                        parcel.enforceInterface(DPMDESCRIPTOR);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        boolean _result = getAllowSimplePassword(_arg0, data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    }
                    break;
                case 7003:
                    if (isSimplePwdOpen) {
                        parcel.enforceInterface(DPMDESCRIPTOR);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        saveCurrentPwdStatus(_arg1, data.readInt());
                        reply.writeNoException();
                        return true;
                    }
                    break;
            }
            return super.onTransact(code, data, reply, flags);
        }
        Slog.w(TAG, "transaction_setActiveVisitorPasswordState");
        parcel.enforceInterface("com.android.internal.widget.ILockSettings");
        setActiveVisitorPasswordState(data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt());
        reply.writeNoException();
        return true;
    }

    private void listenForUserSwitches() {
        try {
            ActivityManagerNative.getDefault().registerUserSwitchObserver(new SynchronousUserSwitchObserver() {
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid userId ");
            stringBuilder.append(userHandle);
            stringBuilder.append(",should be:");
            stringBuilder.append(0);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    /* JADX WARNING: Missing block: B:15:0x003d, code:
            return;
     */
    /* JADX WARNING: Missing block: B:22:0x0050, code:
            if (r7.mHwAdminCache == null) goto L_0x005c;
     */
    /* JADX WARNING: Missing block: B:23:0x0052, code:
            r7.mHwAdminCache.syncHwAdminCache(0, isWifiDisabled(null, r10));
     */
    /* JADX WARNING: Missing block: B:24:0x005c, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setWifiDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_WIFI", "does not have wifi MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableWifi != disabled) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        WifiManager mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
                        if (!mWifiManager.isWifiEnabled() || !disabled || mWifiManager.setWifiEnabled(false)) {
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

    public boolean isWifiDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                boolean z = getHwActiveAdmin(who, userHandle).disableWifi;
                return z;
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            int i = 0;
            while (i < N) {
                ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                if (admin.mHwActiveAdmin == null || !admin.mHwActiveAdmin.disableWifi) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isBluetoothDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                boolean z = getHwActiveAdmin(who, userHandle).disableBluetooth;
                return z;
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            int i = 0;
            while (i < N) {
                ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                if (admin.mHwActiveAdmin == null || !admin.mHwActiveAdmin.disableBluetooth) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:15:0x003f, code:
            return;
     */
    /* JADX WARNING: Missing block: B:22:0x0052, code:
            if (r7.mHwAdminCache == null) goto L_0x0060;
     */
    /* JADX WARNING: Missing block: B:23:0x0054, code:
            r7.mHwAdminCache.syncHwAdminCache(8, isBluetoothDisabled(null, r10));
     */
    /* JADX WARNING: Missing block: B:24:0x0060, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setBluetoothDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_BLUETOOTH", "does not have bluethooth MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableBluetooth != disabled) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        BluetoothAdapter mBTAdapter = ((BluetoothManager) this.mContext.getSystemService("bluetooth")).getAdapter();
                        if (!mBTAdapter.isEnabled() || !disabled || mBTAdapter.disable()) {
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

    public void setWifiApDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_WIFI", "does not have Wifi AP MDM permission!");
        synchronized (getLockObject()) {
            enforceUserRestrictionPermission(who, "no_config_tethering", userHandle);
            HwActiveAdmin ap = getHwActiveAdmin(who, userHandle);
            if (ap.disableWifiAp != disabled) {
                ap.disableWifiAp = disabled;
                saveSettingsLocked(userHandle);
            }
            hwSyncDeviceCapabilitiesLocked("no_config_tethering", userHandle);
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0012, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isWifiApDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableWifiAp;
                }
            } else if (this.mUserManager.hasUserRestriction("no_config_tethering", new UserHandle(userHandle))) {
                DevicePolicyData policy = getUserData(userHandle);
                int N = policy.mAdminList.size();
                int i = 0;
                while (i < N) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin == null || !admin2.mHwActiveAdmin.disableWifiAp) {
                        i++;
                    } else {
                        return true;
                    }
                }
                return false;
            } else {
                return false;
            }
        }
    }

    public void setBootLoaderDisabled(ComponentName who, boolean disabled, int userHandle) {
    }

    public boolean isBootLoaderDisabled(ComponentName who, int userHandle) {
        return false;
    }

    public void setUSBDataDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_USB", "does not have USB MDM permission!");
        synchronized (getLockObject()) {
            enforceUserRestrictionPermission(who, "no_usb_file_transfer", userHandle);
            HwActiveAdmin ap = getHwActiveAdmin(who, userHandle);
            if (ap.disableUSBData != disabled) {
                ap.disableUSBData = disabled;
                saveSettingsLocked(userHandle);
            }
            hwSyncDeviceCapabilitiesLocked("no_usb_file_transfer", userHandle);
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0012, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isUSBDataDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                HwActiveAdmin ap = getHwActiveAdmin(who, userHandle);
                if (ap != null) {
                    z = ap.disableUSBData;
                }
            } else if (this.mUserManager.hasUserRestriction("no_usb_file_transfer", new UserHandle(userHandle))) {
                DevicePolicyData policy = getUserData(userHandle);
                int N = policy.mAdminList.size();
                int i = 0;
                while (i < N) {
                    ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin.mHwActiveAdmin == null || !admin.mHwActiveAdmin.disableUSBData) {
                        i++;
                    } else {
                        return true;
                    }
                }
                return false;
            } else {
                return false;
            }
        }
    }

    public void setExternalStorageDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_SDCARD", "does not have SDCARD MDM permission!");
        synchronized (getLockObject()) {
            enforceUserRestrictionPermission(who, "no_physical_media", userHandle);
            HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
            if (admin.disableExternalStorage != disabled) {
                admin.disableExternalStorage = disabled;
                saveSettingsLocked(userHandle);
            }
            hwSyncDeviceCapabilitiesLocked("no_physical_media", userHandle);
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0012, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isExternalStorageDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableExternalStorage;
                }
            } else if (this.mUserManager.hasUserRestriction("no_physical_media", new UserHandle(userHandle))) {
                DevicePolicyData policy = getUserData(userHandle);
                int N = policy.mAdminList.size();
                int i = 0;
                while (i < N) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin == null || !admin2.mHwActiveAdmin.disableExternalStorage) {
                        i++;
                    } else {
                        return true;
                    }
                }
                return false;
            } else {
                return false;
            }
        }
    }

    public void setNFCDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_NFC", "does not have NFC MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableNFC != disabled) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this.mContext);
                        if (nfcAdapter != null) {
                            boolean nfcOriginalState = nfcAdapter.isEnabled();
                            if (disabled && nfcOriginalState) {
                                nfcAdapter.disable();
                            }
                        }
                        admin.disableNFC = disabled;
                        saveSettingsLocked(userHandle);
                    } finally {
                        Binder.restoreCallingIdentity(callingId);
                    }
                }
            } else {
                throw new NullPointerException("ComponentName is null");
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0012, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isNFCDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableNFC;
                }
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                int N = policy.mAdminList.size();
                int i = 0;
                while (i < N) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin == null || !admin2.mHwActiveAdmin.disableNFC) {
                        i++;
                    } else {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public void setDataConnectivityDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_CONNECTIVITY", "Does not hava data connectivity MDM permission.");
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableDataConnectivity != disabled) {
                    admin.disableDataConnectivity = disabled;
                    saveSettingsLocked(userHandle);
                }
                if (disabled) {
                    TelephonyManager.from(this.mContext).setDataEnabled(disabled ^ 1);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0012, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isDataConnectivityDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableDataConnectivity;
                }
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                int N = policy.mAdminList.size();
                int i = 0;
                while (i < N) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin == null || !admin2.mHwActiveAdmin.disableDataConnectivity) {
                        i++;
                    } else {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public void setVoiceDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_PHONE", "Does not hava phone disable MDM permission.");
        synchronized (getLockObject()) {
            enforceUserRestrictionPermission(who, "no_outgoing_calls", userHandle);
            HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
            if (admin.disableVoice != disabled) {
                admin.disableVoice = disabled;
                saveSettingsLocked(userHandle);
            }
            hwSyncDeviceCapabilitiesLocked("no_outgoing_calls", userHandle);
        }
        if (this.mHwAdminCache != null) {
            this.mHwAdminCache.syncHwAdminCache(1, isVoiceDisabled(null, userHandle));
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0012, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isVoiceDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableVoice;
                }
            } else if (this.mUserManager.hasUserRestriction("no_outgoing_calls", new UserHandle(userHandle))) {
                DevicePolicyData policy = getUserData(userHandle);
                int N = policy.mAdminList.size();
                int i = 0;
                while (i < N) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin == null || !admin2.mHwActiveAdmin.disableVoice) {
                        i++;
                    } else {
                        return true;
                    }
                }
                return false;
            } else {
                return false;
            }
        }
    }

    public void setSMSDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_MMS", "Does not hava SMS disable MDM permission.");
        synchronized (getLockObject()) {
            enforceUserRestrictionPermission(who, "no_sms", userHandle);
            HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
            if (admin.disableSMS != disabled) {
                admin.disableSMS = disabled;
                saveSettingsLocked(userHandle);
            }
            hwSyncDeviceCapabilitiesLocked("no_sms", userHandle);
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0012, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isSMSDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableSMS;
                }
            } else if (this.mUserManager.hasUserRestriction("no_sms", new UserHandle(userHandle))) {
                DevicePolicyData policy = getUserData(userHandle);
                int N = policy.mAdminList.size();
                int i = 0;
                while (i < N) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin == null || !admin2.mHwActiveAdmin.disableSMS) {
                        i++;
                    } else {
                        return true;
                    }
                }
                return false;
            } else {
                return false;
            }
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0036, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setStatusBarExpandPanelDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have DEVICE MANAGER permission!");
        synchronized (getLockObject()) {
            HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
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
                return false;
            }
            statusBar.disable(STATUS_BAR_DISABLE_MASK);
            Binder.restoreCallingIdentity(callingId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "failed to set statusBar disabled.");
            return false;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private boolean setStatusBarPanelEnableInternal(boolean forceEnable, int userHandle) {
        long callingId = Binder.clearCallingIdentity();
        try {
            StatusBarManager statusBar = (StatusBarManager) this.mContext.getSystemService("statusbar");
            if (statusBar == null) {
                Log.w(TAG, "statusBar is null");
                return false;
            }
            if (forceEnable) {
                statusBar.disable(0);
            } else if (!isStatusBarExpandPanelDisabled(false, userHandle)) {
                statusBar.disable(0);
            }
            Binder.restoreCallingIdentity(callingId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "failed to set statusBar enabled.");
            return false;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0012, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isStatusBarExpandPanelDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableStatusBarExpandPanel;
                }
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                int N = policy.mAdminList.size();
                int i = 0;
                while (i < N) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin == null || !admin2.mHwActiveAdmin.disableStatusBarExpandPanel) {
                        i++;
                    } else {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public void hangupCalling(ComponentName who, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_PHONE", "Does not hava hangup calling permission.");
        synchronized (getLockObject()) {
            if (who != null) {
                if (getHwActiveAdmin(who, userHandle) != null) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        TelephonyManager.from(this.mContext).endCall();
                        UiThread.getHandler().post(new Runnable() {
                            public void run() {
                                if (HwDevicePolicyManagerService.this.mErrorDialog != null) {
                                    HwDevicePolicyManagerService.this.mErrorDialog.dismiss();
                                    HwDevicePolicyManagerService.this.mErrorDialog = null;
                                }
                                HwDevicePolicyManagerService.this.mErrorDialog = new Builder(HwDevicePolicyManagerService.this.mContext, 33947691).setMessage(33686011).setPositiveButton(33686121, new OnClickListener() {
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
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    public void installPackage(ComponentName who, String packagePath, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "Does not hava application management permission.");
        synchronized (getLockObject()) {
            if (who != null) {
                if (getHwActiveAdmin(who, userHandle) != null) {
                    installPackage(packagePath, who.getPackageName());
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    public void uninstallPackage(ComponentName who, String packageName, boolean keepData, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "Does not hava application management permission.");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageName(packageName)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (getHwActiveAdmin(who, userHandle) != null) {
                        uninstallPackage(packageName, keepData);
                    }
                } else {
                    throw new IllegalArgumentException("ComponentName is null");
                }
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("packageName:");
        stringBuilder.append(packageName);
        stringBuilder.append(" is invalid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void clearPackageData(ComponentName who, String packageName, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have wifi MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageName(packageName)) {
            synchronized (getLockObject()) {
                if (who == null) {
                    throw new IllegalArgumentException("ComponentName is null");
                } else if (TextUtils.isEmpty(packageName)) {
                    throw new IllegalArgumentException("packageNames is null or empty");
                } else {
                    enforceCheckNotSystemApp(packageName, userHandle);
                    if (getHwActiveAdmin(who, userHandle) != null) {
                        long id = Binder.clearCallingIdentity();
                        try {
                            ((ActivityManager) this.mContext.getSystemService("activity")).clearApplicationUserData(packageName, null);
                        } finally {
                            Binder.restoreCallingIdentity(id);
                        }
                    }
                }
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("packageName:");
        stringBuilder.append(packageName);
        stringBuilder.append(" is invalid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void enableInstallPackage(ComponentName who, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have wifi MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    admin.disableInstallSource = false;
                    admin.installSourceWhitelist = null;
                }
                saveSettingsLocked(userHandle);
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        if (this.mHwAdminCache != null) {
            this.mHwAdminCache.syncHwAdminCache(2, isInstallSourceDisabled(null, userHandle));
        }
    }

    public void disableInstallSource(ComponentName who, List<String> whitelist, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have wifi MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(whitelist)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (whitelist != null) {
                        if (!whitelist.isEmpty()) {
                            HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
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
            if (this.mHwAdminCache != null) {
                this.mHwAdminCache.syncHwAdminCache(2, isInstallSourceDisabled(null, userHandle));
                this.mHwAdminCache.syncHwAdminCache(3, getInstallPackageSourceWhiteList(null, userHandle));
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("packageName:");
        stringBuilder.append(whitelist);
        stringBuilder.append(" is invalid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:9:0x0012, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isInstallSourceDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            boolean z = false;
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin != null) {
                    z = admin.disableInstallSource;
                }
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                int N = policy.mAdminList.size();
                int i = 0;
                while (i < N) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin == null || !admin2.mHwActiveAdmin.disableInstallSource) {
                        i++;
                    } else {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x001d, code:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<String> getInstallPackageSourceWhiteList(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                List<String> list = (admin.installSourceWhitelist == null || admin.installSourceWhitelist.isEmpty()) ? null : admin.installSourceWhitelist;
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                ArrayList<String> whiteList = new ArrayList();
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin != null) {
                        HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(whiteList, admin2.mHwActiveAdmin.installSourceWhitelist);
                    }
                }
                return whiteList;
            }
        }
    }

    public void addPersistentApp(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "Does not hava manager app MDM permission.");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                            if (admin.persistentAppList == null) {
                                admin.persistentAppList = new ArrayList();
                            }
                            HwDevicePolicyManagerServiceUtil.isOverLimit(getPersistentApp(null, userHandle), packageNames, 3);
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
            if (this.mHwAdminCache != null) {
                this.mHwAdminCache.syncHwAdminCache(4, getPersistentApp(null, userHandle));
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("packageName:");
        stringBuilder.append(packageNames);
        stringBuilder.append(" is invalid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

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
            if (this.mHwAdminCache != null) {
                this.mHwAdminCache.syncHwAdminCache(4, getPersistentApp(null, userHandle));
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("packageName:");
        stringBuilder.append(packageNames);
        stringBuilder.append(" is invalid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void sendPersistentAppToIAware(int userHandle) {
        List<String> persistAppList = getPersistentApp(null, userHandle);
        String str;
        StringBuilder stringBuilder;
        if (persistAppList == null || persistAppList.size() <= 0) {
            ProcessCleaner.getInstance(this.mContext).removeProtectedListFromMDM();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("removeProtectedListFromMDM for user ");
            stringBuilder.append(userHandle);
            Slog.d(str, stringBuilder.toString());
            return;
        }
        ProcessCleaner.getInstance(this.mContext).setProtectedListFromMDM(persistAppList);
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("setProtectedListFromMDM for user ");
        stringBuilder.append(userHandle);
        stringBuilder.append(":");
        stringBuilder.append(persistAppList);
        Slog.d(str, stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:11:0x001d, code:
            return r1;
     */
    /* JADX WARNING: Missing block: B:23:0x0051, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<String> getPersistentApp(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            List<String> list = null;
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (!(admin.persistentAppList == null || admin.persistentAppList.isEmpty())) {
                    list = admin.persistentAppList;
                }
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                ArrayList<String> totalList = new ArrayList();
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin != null) {
                        HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(totalList, admin2.mHwActiveAdmin.persistentAppList);
                    }
                }
                if (!totalList.isEmpty()) {
                    list = totalList;
                }
            }
        }
    }

    public void addDisallowedRunningApp(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "Does not hava manager app MDM permission.");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                            if (admin.disallowedRunningAppList == null) {
                                admin.disallowedRunningAppList = new ArrayList();
                            }
                            HwDevicePolicyManagerServiceUtil.isOverLimit(admin.disallowedRunningAppList, packageNames);
                            filterOutSystemAppList(packageNames, userHandle);
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
            if (this.mHwAdminCache != null) {
                this.mHwAdminCache.syncHwAdminCache(5, getDisallowedRunningApp(null, userHandle));
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("packageName:");
        stringBuilder.append(packageNames);
        stringBuilder.append(" is invalid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

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
            if (this.mHwAdminCache != null) {
                this.mHwAdminCache.syncHwAdminCache(5, getDisallowedRunningApp(null, userHandle));
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("packageName:");
        stringBuilder.append(packageNames);
        stringBuilder.append(" is invalid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:11:0x001d, code:
            return r1;
     */
    /* JADX WARNING: Missing block: B:23:0x0051, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<String> getDisallowedRunningApp(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            List<String> list = null;
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (!(admin.disallowedRunningAppList == null || admin.disallowedRunningAppList.isEmpty())) {
                    list = admin.disallowedRunningAppList;
                }
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                ArrayList<String> totalList = new ArrayList();
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin != null) {
                        HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(totalList, admin2.mHwActiveAdmin.disallowedRunningAppList);
                    }
                }
                if (!totalList.isEmpty()) {
                    list = totalList;
                }
            }
        }
    }

    public void addInstallPackageWhiteList(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app_management MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
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
            if (this.mHwAdminCache != null) {
                this.mHwAdminCache.syncHwAdminCache(6, getInstallPackageWhiteList(null, userHandle));
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("packageName:");
        stringBuilder.append(packageNames);
        stringBuilder.append(" is invalid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

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
            if (this.mHwAdminCache != null) {
                this.mHwAdminCache.syncHwAdminCache(6, getInstallPackageWhiteList(null, userHandle));
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("packageName:");
        stringBuilder.append(packageNames);
        stringBuilder.append(" is invalid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:11:0x001d, code:
            return r1;
     */
    /* JADX WARNING: Missing block: B:23:0x0051, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<String> getInstallPackageWhiteList(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            List<String> list = null;
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (!(admin.installPackageWhitelist == null || admin.installPackageWhitelist.isEmpty())) {
                    list = admin.installPackageWhitelist;
                }
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                ArrayList<String> whitelist = new ArrayList();
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin != null) {
                        HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(whitelist, admin2.mHwActiveAdmin.installPackageWhitelist);
                    }
                }
                if (!whitelist.isEmpty()) {
                    list = whitelist;
                }
            }
        }
    }

    public void addDisallowedUninstallPackages(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app_management MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
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
            if (this.mHwAdminCache != null) {
                this.mHwAdminCache.syncHwAdminCache(7, getDisallowedUninstallPackageList(null, userHandle));
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("packageName:");
        stringBuilder.append(packageNames);
        stringBuilder.append(" is invalid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

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
            if (this.mHwAdminCache != null) {
                this.mHwAdminCache.syncHwAdminCache(7, getDisallowedUninstallPackageList(null, userHandle));
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("packageName:");
        stringBuilder.append(packageNames);
        stringBuilder.append(" is invalid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:11:0x001d, code:
            return r1;
     */
    /* JADX WARNING: Missing block: B:23:0x0051, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<String> getDisallowedUninstallPackageList(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            List<String> list = null;
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (!(admin.disallowedUninstallPackageList == null || admin.disallowedUninstallPackageList.isEmpty())) {
                    list = admin.disallowedUninstallPackageList;
                }
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                ArrayList<String> blacklist = new ArrayList();
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin != null) {
                        HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(blacklist, admin2.mHwActiveAdmin.disallowedUninstallPackageList);
                    }
                }
                if (!blacklist.isEmpty()) {
                    list = blacklist;
                }
            }
        }
    }

    public void addDisabledDeactivateMdmPackages(ComponentName who, List<String> packageNames, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app_management MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                            if (admin.disabledDeactiveMdmPackagesList == null) {
                                admin.disabledDeactiveMdmPackagesList = new ArrayList();
                            }
                            HwDevicePolicyManagerServiceUtil.isOverLimit(admin.disabledDeactiveMdmPackagesList, packageNames);
                            filterOutSystemAppList(packageNames, userHandle);
                            HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(admin.disabledDeactiveMdmPackagesList, packageNames);
                            saveSettingsLocked(userHandle);
                        }
                    }
                    throw new IllegalArgumentException("packageNames is null or empty");
                }
                throw new IllegalArgumentException("ComponentName is null");
            }
            if (this.mHwAdminCache != null) {
                this.mHwAdminCache.syncHwAdminCache(18, getDisabledDeactivateMdmPackageList(null, userHandle));
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("packageName:");
        stringBuilder.append(packageNames);
        stringBuilder.append(" is invalid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

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
            if (this.mHwAdminCache != null) {
                this.mHwAdminCache.syncHwAdminCache(18, getDisabledDeactivateMdmPackageList(null, userHandle));
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("packageName:");
        stringBuilder.append(packageNames);
        stringBuilder.append(" is invalid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:11:0x001d, code:
            return r1;
     */
    /* JADX WARNING: Missing block: B:23:0x0051, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<String> getDisabledDeactivateMdmPackageList(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            List<String> list = null;
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (!(admin.disabledDeactiveMdmPackagesList == null || admin.disabledDeactiveMdmPackagesList.isEmpty())) {
                    list = admin.disabledDeactiveMdmPackagesList;
                }
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                ArrayList<String> blacklist = new ArrayList();
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin != null) {
                        HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(blacklist, admin2.mHwActiveAdmin.disabledDeactiveMdmPackagesList);
                    }
                }
                if (!blacklist.isEmpty()) {
                    list = blacklist;
                }
            }
        }
    }

    public void killApplicationProcess(ComponentName who, String packageName, int userHandle) {
        if (HwDevicePolicyManagerServiceUtil.isValidatePackageName(packageName)) {
            enforceHwCrossUserPermission(userHandle);
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "Does not hava application management permission.");
            synchronized (getLockObject()) {
                if (who == null) {
                    throw new IllegalArgumentException("ComponentName is null");
                } else if (TextUtils.isEmpty(packageName)) {
                    throw new IllegalArgumentException("Package name is empty");
                } else if (packageName.equals(who.getPackageName())) {
                    throw new IllegalArgumentException("Can not kill the caller application");
                } else {
                    enforceCheckNotSystemApp(packageName, userHandle);
                    if (getHwActiveAdmin(who, userHandle) != null) {
                        killApplicationInner(packageName);
                    }
                }
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("packageName:");
        stringBuilder.append(packageName);
        stringBuilder.append(" is invalid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void killApplicationInner(String packageName) {
        long ident = Binder.clearCallingIdentity();
        try {
            ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
            for (RunningTaskInfo ti : am.getRunningTasks(10000)) {
                if (packageName.equals(ti.baseActivity.getPackageName())) {
                    ActivityManager.getService().removeTask(ti.id);
                    am.forceStopPackage(packageName);
                    break;
                }
            }
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("killApplicationInner exception is ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
        Binder.restoreCallingIdentity(ident);
    }

    public void shutdownOrRebootDevice(int code, ComponentName who, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                getHwActiveAdmin(who, userHandle);
                long callingId = Binder.clearCallingIdentity();
                try {
                    IPowerManager power = Stub.asInterface(ServiceManager.getService("power"));
                    if (power == null) {
                        Binder.restoreCallingIdentity(callingId);
                        return;
                    }
                    if (code == 1501) {
                        power.shutdown(false, null, false);
                    } else if (code == 1502) {
                        power.reboot(false, null, false);
                    }
                    Binder.restoreCallingIdentity(callingId);
                } catch (RemoteException e) {
                    try {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("exception is ");
                        stringBuilder.append(e.getMessage());
                        Log.e(str, stringBuilder.toString());
                    } finally {
                        Binder.restoreCallingIdentity(callingId);
                    }
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    public void configExchangeMailProvider(ComponentName who, Bundle para, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_EMAIL", "does not have EMAIL MDM permission!");
        synchronized (getLockObject()) {
            if (who == null || para == null) {
                throw new IllegalArgumentException("ComponentName or para is null");
            } else if (HwDevicePolicyManagerServiceUtil.isValidExchangeParameter(para)) {
                HwActiveAdmin ap = getHwActiveAdmin(who, userHandle);
                if (ap.mailProviderlist == null) {
                    ap.mailProviderlist = new ArrayList();
                    ap.mailProviderlist.add(para);
                    saveSettingsLocked(userHandle);
                } else if (ap.mailProviderlist.size() + 1 <= 20) {
                    boolean isAlready = false;
                    Bundle provider = null;
                    for (Bundle each : ap.mailProviderlist) {
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

    /* JADX WARNING: Missing block: B:23:0x0044, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Bundle getMailProviderForDomain(ComponentName who, String domain, int userHandle) {
        Bundle bundle = null;
        if (userHandle != 0) {
            return null;
        }
        if (TextUtils.isEmpty(domain)) {
            throw new IllegalArgumentException("domain is empty.");
        }
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.mailProviderlist == null) {
                    return null;
                }
                boolean matched = false;
                Bundle retProvider = null;
                for (Bundle provider : admin.mailProviderlist) {
                    matched = HwDevicePolicyManagerServiceUtil.matchProvider(domain, provider.getString("domain"));
                    if (matched) {
                        retProvider = provider;
                        break;
                    }
                }
                if (matched) {
                    bundle = retProvider;
                }
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
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
    }

    public boolean isRooted(ComponentName who, int userHandle) {
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                getHwActiveAdmin(who, userHandle);
                String currentState = SystemProperties.get("persist.sys.root.status");
                if (TextUtils.isEmpty(currentState) || !"0".equals(currentState)) {
                    return true;
                }
                return false;
            }
            throw new IllegalArgumentException("ComponentName is null");
        }
    }

    public void setSafeModeDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have DEVICE MANAGER permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableSafeMode != disabled) {
                    admin.disableSafeMode = disabled;
                    saveSettingsLocked(userHandle);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        if (this.mHwAdminCache != null) {
            this.mHwAdminCache.syncHwAdminCache(10, isSafeModeDisabled(null, userHandle));
        }
    }

    public boolean isSafeModeDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                boolean z = getHwActiveAdmin(who, userHandle).disableSafeMode;
                return z;
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            int i = 0;
            while (i < N) {
                ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                if (admin.mHwActiveAdmin == null || !admin.mHwActiveAdmin.disableSafeMode) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    public void setAdbDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_USB", "does not have MDM_USB permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableAdb != disabled) {
                    admin.disableAdb = disabled;
                    saveSettingsLocked(userHandle);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        long identityToken = Binder.clearCallingIdentity();
        if (disabled) {
            if (Global.getInt(this.mContext.getContentResolver(), "adb_enabled", 0) > 0) {
                Global.putInt(this.mContext.getContentResolver(), "adb_enabled", 0);
            }
        }
        Binder.restoreCallingIdentity(identityToken);
        if (this.mHwAdminCache != null) {
            this.mHwAdminCache.syncHwAdminCache(11, isAdbDisabled(null, userHandle));
        }
    }

    public boolean isAdbDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                boolean z = getHwActiveAdmin(who, userHandle).disableAdb;
                return z;
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            int i = 0;
            while (i < N) {
                ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                if (admin.mHwActiveAdmin == null || !admin.mHwActiveAdmin.disableAdb) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    public void setUSBOtgDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_USB", "does not have MDM_USB permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
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
        for (StorageVolume storageVolume : sm.getVolumeList()) {
            if (storageVolume.isRemovable()) {
                if ("mounted".equals(sm.getVolumeState(storageVolume.getPath()))) {
                    VolumeInfo volumeInfo = sm.findVolumeByUuid(storageVolume.getUuid());
                    if (volumeInfo != null) {
                        DiskInfo diskInfo = volumeInfo.getDisk();
                        if (diskInfo != null && diskInfo.isUsb()) {
                            Slog.e(TAG, "find usb otg device mounted , umounted it");
                            sm.unmount(storageVolume.getId());
                        }
                    }
                }
            }
        }
        Binder.restoreCallingIdentity(identityToken);
        if (this.mHwAdminCache != null) {
            this.mHwAdminCache.syncHwAdminCache(12, isUSBOtgDisabled(null, userHandle));
        }
    }

    public boolean isUSBOtgDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                boolean z = getHwActiveAdmin(who, userHandle).disableUSBOtg;
                return z;
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            int i = 0;
            while (i < N) {
                ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                if (admin.mHwActiveAdmin == null || !admin.mHwActiveAdmin.disableUSBOtg) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    public void setGPSDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_LOCATION", "does not have MDM_LOCATION permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableGPS != disabled) {
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
        if (this.mHwAdminCache != null) {
            this.mHwAdminCache.syncHwAdminCache(13, isGPSDisabled(null, userHandle));
        }
    }

    public boolean isGPSDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                boolean z = getHwActiveAdmin(who, userHandle).disableGPS;
                return z;
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            int i = 0;
            while (i < N) {
                ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                if (admin.mHwActiveAdmin == null || !admin.mHwActiveAdmin.disableGPS) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    public void turnOnGPS(ComponentName who, boolean on, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_LOCATION", "does not have MDM_LOCATION permission!");
        if (who != null) {
            synchronized (getLockObject()) {
                getHwActiveAdmin(who, userHandle);
            }
            if (isGPSTurnOn(who, userHandle) != on) {
                long identityToken = Binder.clearCallingIdentity();
                if (!Secure.setLocationProviderEnabledForUser(this.mContext.getContentResolver(), "gps", on, ActivityManager.getCurrentUser())) {
                    Log.e(TAG, "setLocationProviderEnabledForUser failed");
                }
                Binder.restoreCallingIdentity(identityToken);
                return;
            }
            return;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    public boolean isGPSTurnOn(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            getHwActiveAdmin(who, userHandle);
        }
        long identityToken = Binder.clearCallingIdentity();
        boolean isGPSEnabled = Secure.isLocationProviderEnabledForUser(this.mContext.getContentResolver(), "gps", ActivityManager.getCurrentUser());
        Binder.restoreCallingIdentity(identityToken);
        return isGPSEnabled;
    }

    public void setTaskButtonDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have DEVICE MANAGER permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableTaskKey != disabled) {
                    admin.disableTaskKey = disabled;
                    saveSettingsLocked(userHandle);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        if (this.mHwAdminCache != null) {
            this.mHwAdminCache.syncHwAdminCache(15, isTaskButtonDisabled(null, userHandle));
        }
    }

    public boolean isTaskButtonDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                boolean z = getHwActiveAdmin(who, userHandle).disableTaskKey;
                return z;
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            int i = 0;
            while (i < N) {
                ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                if (admin.mHwActiveAdmin == null || !admin.mHwActiveAdmin.disableTaskKey) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    public void setHomeButtonDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have DEVICE MANAGER permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableHomeKey != disabled) {
                    admin.disableHomeKey = disabled;
                    saveSettingsLocked(userHandle);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        if (this.mHwAdminCache != null) {
            this.mHwAdminCache.syncHwAdminCache(14, isHomeButtonDisabled(null, userHandle));
        }
    }

    public boolean isHomeButtonDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                boolean z = getHwActiveAdmin(who, userHandle).disableHomeKey;
                return z;
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            int i = 0;
            while (i < N) {
                ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                if (admin.mHwActiveAdmin == null || !admin.mHwActiveAdmin.disableHomeKey) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    public void setBackButtonDisabled(ComponentName who, boolean disabled, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have DEVICE MANAGER permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (admin.disableBackKey != disabled) {
                    admin.disableBackKey = disabled;
                    saveSettingsLocked(userHandle);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        if (this.mHwAdminCache != null) {
            this.mHwAdminCache.syncHwAdminCache(16, isBackButtonDisabled(null, userHandle));
        }
    }

    public boolean isBackButtonDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                boolean z = getHwActiveAdmin(who, userHandle).disableBackKey;
                return z;
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            int i = 0;
            while (i < N) {
                ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                if (admin.mHwActiveAdmin == null || !admin.mHwActiveAdmin.disableBackKey) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    public void setSysTime(ComponentName who, long millis, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device manager MDM permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                getHwActiveAdmin(who, userHandle);
                long id = Binder.clearCallingIdentity();
                SystemClock.setCurrentTimeMillis(millis);
                Binder.restoreCallingIdentity(id);
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    /* JADX WARNING: Missing block: B:23:0x0075, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
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
                            String oldMenus = Global.getStringForUser(this.mContext.getContentResolver(), SETTINGS_MENUS_REMOVE, userHandle);
                            String splitter = ",";
                            StringBuffer newMenus = new StringBuffer();
                            if (!TextUtils.isEmpty(oldMenus)) {
                                newMenus.append(oldMenus);
                            }
                            for (String menu : menusToDelete) {
                                if (oldMenus == null || !oldMenus.contains(menu)) {
                                    newMenus.append(menu);
                                    newMenus.append(splitter);
                                }
                            }
                            Global.putStringForUser(this.mContext.getContentResolver(), SETTINGS_MENUS_REMOVE, newMenus.toString(), userHandle);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(callingId);
                    }
                }
                Global.putStringForUser(this.mContext.getContentResolver(), SETTINGS_MENUS_REMOVE, "", userHandle);
                Binder.restoreCallingIdentity(callingId);
                return;
            }
            throw new IllegalArgumentException("ComponentName is null");
        }
    }

    public void setDefaultLauncher(ComponentName who, String packageName, String className, int userHandle) {
        if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("packageName is null or empty");
        } else if (TextUtils.isEmpty(className)) {
            throw new IllegalArgumentException("className is null or empty");
        } else {
            enforceHwCrossUserPermission(userHandle);
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.SDK_LAUNCHER", "Does not have sdk_launcher permission.");
            synchronized (getLockObject()) {
                if (who != null) {
                    HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                    long callingId = Binder.clearCallingIdentity();
                    LauncherUtils.setDefaultLauncher(this.mContext, packageName, className);
                    Binder.restoreCallingIdentity(callingId);
                    admin.disableChangeLauncher = true;
                    saveSettingsLocked(userHandle);
                    if (this.mHwAdminCache != null) {
                        this.mHwAdminCache.syncHwAdminCache(17, isChangeLauncherDisabled(null, userHandle));
                    }
                } else {
                    throw new IllegalArgumentException("ComponentName is null");
                }
            }
        }
    }

    public void clearDefaultLauncher(ComponentName who, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.SDK_LAUNCHER", "Does not have sdk_launcher permission.");
        synchronized (getLockObject()) {
            if (who != null) {
                getHwActiveAdmin(who, userHandle).disableChangeLauncher = false;
                saveSettingsLocked(userHandle);
                if (this.mHwAdminCache != null) {
                    this.mHwAdminCache.syncHwAdminCache(17, isChangeLauncherDisabled(null, userHandle));
                }
                long callingId = Binder.clearCallingIdentity();
                LauncherUtils.clearDefaultLauncher(this.mContext);
                Binder.restoreCallingIdentity(callingId);
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    public boolean isChangeLauncherDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                boolean z = getHwActiveAdmin(who, userHandle).disableChangeLauncher;
                return z;
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            int i = 0;
            while (i < N) {
                ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                if (admin.mHwActiveAdmin == null || !admin.mHwActiveAdmin.disableChangeLauncher) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    public Bitmap captureScreen(ComponentName who, int userHandle) {
        Bitmap bmp;
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_CAPTURE_SCREEN", "Does not have MDM_CAPTURE_SCREEN permission.");
        synchronized (getLockObject()) {
            if (who != null) {
                getHwActiveAdmin(who, userHandle);
                long callingId = Binder.clearCallingIdentity();
                bmp = CaptureScreenUtils.captureScreen(this.mContext);
                Binder.restoreCallingIdentity(callingId);
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        return bmp;
    }

    public void addApn(ComponentName who, Map<String, String> apnInfo, int userHandle) {
        if (apnInfo == null || apnInfo.isEmpty()) {
            throw new IllegalArgumentException("apnInfo is empty.");
        }
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APN", "Does not have apn permission.");
        synchronized (getLockObject()) {
            if (who != null) {
                getHwActiveAdmin(who, userHandle);
                long callingId = Binder.clearCallingIdentity();
                ApnUtils.addApn(this.mContext.getContentResolver(), apnInfo);
                Binder.restoreCallingIdentity(callingId);
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    public void deleteApn(ComponentName who, String apnId, int userHandle) {
        if (TextUtils.isEmpty(apnId)) {
            throw new IllegalArgumentException("apnId is empty.");
        }
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APN", "Does not have apn permission.");
        synchronized (getLockObject()) {
            if (who != null) {
                getHwActiveAdmin(who, userHandle);
                long callingId = Binder.clearCallingIdentity();
                ApnUtils.deleteApn(this.mContext.getContentResolver(), apnId);
                Binder.restoreCallingIdentity(callingId);
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    public void updateApn(ComponentName who, Map<String, String> apnInfo, String apnId, int userHandle) {
        if (apnInfo == null || apnInfo.isEmpty()) {
            throw new IllegalArgumentException("apnInfo is empty.");
        } else if (TextUtils.isEmpty(apnId)) {
            throw new IllegalArgumentException("apnId is empty.");
        } else {
            enforceHwCrossUserPermission(userHandle);
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APN", "Does not have apn permission.");
            synchronized (getLockObject()) {
                if (who != null) {
                    getHwActiveAdmin(who, userHandle);
                    long callingId = Binder.clearCallingIdentity();
                    ApnUtils.updateApn(this.mContext.getContentResolver(), apnInfo, apnId);
                    Binder.restoreCallingIdentity(callingId);
                } else {
                    throw new IllegalArgumentException("ComponentName is null");
                }
            }
        }
    }

    public void setPreferApn(ComponentName who, String apnId, int userHandle) {
        if (TextUtils.isEmpty(apnId)) {
            throw new IllegalArgumentException("apnId is empty.");
        }
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APN", "Does not have apn permission.");
        synchronized (getLockObject()) {
            if (who != null) {
                getHwActiveAdmin(who, userHandle);
                long callingId = Binder.clearCallingIdentity();
                ApnUtils.setPreferApn(this.mContext.getContentResolver(), apnId);
                Binder.restoreCallingIdentity(callingId);
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    public List<String> queryApn(ComponentName who, Map<String, String> apnInfo, int userHandle) {
        if (apnInfo == null || apnInfo.isEmpty()) {
            throw new IllegalArgumentException("apnInfo is empty.");
        }
        List<String> ids;
        enforceHwCrossUserPermission(userHandle);
        synchronized (getLockObject()) {
            if (who != null) {
                getHwActiveAdmin(who, userHandle);
                long callingId = Binder.clearCallingIdentity();
                ids = ApnUtils.queryApn(this.mContext.getContentResolver(), apnInfo);
                Binder.restoreCallingIdentity(callingId);
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        return ids;
    }

    public Map<String, String> getApnInfo(ComponentName who, String apnId, int userHandle) {
        if (TextUtils.isEmpty(apnId)) {
            throw new IllegalArgumentException("apnId is empty.");
        }
        Map<String, String> apnInfo;
        enforceHwCrossUserPermission(userHandle);
        synchronized (getLockObject()) {
            if (who != null) {
                getHwActiveAdmin(who, userHandle);
                long callingId = Binder.clearCallingIdentity();
                apnInfo = ApnUtils.getApnInfo(this.mContext.getContentResolver(), apnId);
                Binder.restoreCallingIdentity(callingId);
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        return apnInfo;
    }

    public void addNetworkAccessWhitelist(ComponentName who, List<String> addrList, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_NETWORK_MANAGER", "does not have network_manager MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidIPAddrs(addrList)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
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
            if (this.mHwAdminCache != null) {
                this.mHwAdminCache.syncHwAdminCache(9, getNetworkAccessWhitelist(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("addrlist invalid");
    }

    public void removeNetworkAccessWhitelist(ComponentName who, List<String> addrList, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_NETWORK_MANAGER", "does not have network_manager MDM permission!");
        if (HwDevicePolicyManagerServiceUtil.isValidIPAddrs(addrList)) {
            synchronized (getLockObject()) {
                if (who != null) {
                    HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                    HwDevicePolicyManagerServiceUtil.removeItemsFromList(admin.networkAccessWhitelist, addrList);
                    saveSettingsLocked(userHandle);
                    setNetworkAccessWhitelist(admin.networkAccessWhitelist);
                } else {
                    throw new IllegalArgumentException("ComponentName is null");
                }
            }
            if (this.mHwAdminCache != null) {
                this.mHwAdminCache.syncHwAdminCache(9, getNetworkAccessWhitelist(null, userHandle));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("addrlist invalid");
    }

    /* JADX WARNING: Missing block: B:11:0x001d, code:
            return r1;
     */
    /* JADX WARNING: Missing block: B:23:0x0051, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<String> getNetworkAccessWhitelist(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            List<String> list = null;
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
                if (!(admin.networkAccessWhitelist == null || admin.networkAccessWhitelist.isEmpty())) {
                    list = admin.networkAccessWhitelist;
                }
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                ArrayList<String> addrList = new ArrayList();
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin2.mHwActiveAdmin != null) {
                        HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(addrList, admin2.mHwActiveAdmin.networkAccessWhitelist);
                    }
                }
                if (!addrList.isEmpty()) {
                    list = addrList;
                }
            }
        }
    }

    private void setNetworkAccessWhitelist(List<String> whitelist) {
        String DESCRIPTOR_NETWORKMANAGEMENT_SERVICE = "android.os.INetworkManagementService";
        IBinder b = ServiceManager.getService("network_management");
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        if (b != null) {
            try {
                _data.writeInterfaceToken("android.os.INetworkManagementService");
                _data.writeStringList(whitelist);
                b.transact(HwArbitrationDEFS.MSG_STREAMING_VIDEO_BAD, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                Log.e(TAG, "setNetworkAccessWhitelist error", localRemoteException);
            } catch (Throwable th) {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
    }

    public boolean getHwAdminCachedValue(int code) {
        int type = -1;
        if (code != 4009) {
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
                                default:
                                    switch (code) {
                                        case 5021:
                                            type = 29;
                                            break;
                                        case 5022:
                                            type = 32;
                                            break;
                                    }
                                    break;
                            }
                    }
            }
        }
        type = 8;
        if (this.mHwAdminCache == null || type == -1) {
            return false;
        }
        return this.mHwAdminCache.getCachedValue(type);
    }

    public List<String> getHwAdminCachedList(int code) {
        List<String> result = null;
        int type = -1;
        if (code != 4010) {
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
                default:
                    switch (code) {
                        case 4019:
                            type = 18;
                            break;
                        case 4020:
                            type = 20;
                            break;
                        default:
                            switch (code) {
                                case 4027:
                                    type = 27;
                                    break;
                                case 4028:
                                    type = 28;
                                    break;
                            }
                            break;
                    }
            }
        }
        type = 9;
        if (!(this.mHwAdminCache == null || type == -1)) {
            result = this.mHwAdminCache.getCachedList(type);
        }
        return result == null ? new ArrayList() : result;
    }

    public Bundle getHwAdminCachedBundle(String policyName) {
        if (this.mHwAdminCache != null) {
            return this.mHwAdminCache.getCachedBundle(policyName);
        }
        return null;
    }

    private void enforceUserRestrictionPermission(ComponentName who, String key, int userHandle) {
        long id = Binder.clearCallingIdentity();
        try {
            UserInfo info = this.mUserManager.getUserInfo(userHandle);
            if (info == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid user: ");
                stringBuilder.append(userHandle);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (info.isGuest()) {
                throw new IllegalStateException("Cannot call this method on a guest");
            } else if (who == null) {
                throw new IllegalArgumentException("Component is null");
            } else if (userHandle != 0 && HWDEVICE_OWNER_USER_RESTRICTIONS.contains(key)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Cannot set user restriction ");
                stringBuilder2.append(key);
                throw new SecurityException(stringBuilder2.toString());
            }
        } finally {
            Binder.restoreCallingIdentity(id);
        }
    }

    private HwActiveAdmin getHwActiveAdmin(ComponentName who, int userHandle) {
        ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
        StringBuilder stringBuilder;
        if (admin == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No active admin owned by uid ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", ComponentName:");
            stringBuilder.append(who);
            throw new SecurityException(stringBuilder.toString());
        } else if (admin.getUid() == Binder.getCallingUid()) {
            HwActiveAdmin hwadmin = admin.mHwActiveAdmin;
            if (hwadmin != null) {
                return hwadmin;
            }
            hwadmin = new HwActiveAdmin();
            admin.mHwActiveAdmin = hwadmin;
            return hwadmin;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Admin ");
            stringBuilder.append(who);
            stringBuilder.append(" is not owned by uid ");
            stringBuilder.append(Binder.getCallingUid());
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private void setHwUserRestriction(String key, boolean disable, int userHandle) {
        UserHandle user = new UserHandle(userHandle);
        boolean alreadyRestricted = this.mUserManager.hasUserRestriction(key, user);
        if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setUserRestriction for (");
            stringBuilder.append(key);
            stringBuilder.append(", ");
            stringBuilder.append(userHandle);
            stringBuilder.append("), is alreadyRestricted: ");
            stringBuilder.append(alreadyRestricted);
            Log.i(str, stringBuilder.toString());
        }
        long id = Binder.clearCallingIdentity();
        if (disable && !alreadyRestricted) {
            try {
                if ("no_config_tethering".equals(key)) {
                    if (((WifiManager) this.mContext.getSystemService("wifi")).isWifiApEnabled()) {
                        ((ConnectivityManager) this.mContext.getSystemService("connectivity")).stopTethering(0);
                        ((NotificationManager) this.mContext.getSystemService("notification")).cancelAsUser(null, 17303548, UserHandle.ALL);
                    }
                } else if ("no_physical_media".equals(key)) {
                    boolean hasExternalSdcard = StorageUtils.hasExternalSdcard(this.mContext);
                    boolean dafaultIsSdcard = DefaultStorageLocation.isSdcard();
                    if (hasExternalSdcard && !dafaultIsSdcard) {
                        Log.w(TAG, "call doUnMount");
                        StorageUtils.doUnMount(this.mContext);
                    } else if (hasExternalSdcard && dafaultIsSdcard && StorageUtils.isSwitchPrimaryVolumeSupported()) {
                        throw new IllegalStateException("could not disable sdcard when it is primary card.");
                    }
                } else if ("no_usb_file_transfer".equals(key)) {
                    if (disable) {
                        Global.putStringForUser(this.mContext.getContentResolver(), "adb_enabled", "0", userHandle);
                    }
                    this.mUserManager.setUserRestriction("no_debugging_features", true, user);
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(id);
            }
        }
        this.mUserManager.setUserRestriction(key, disable, user);
        if ("no_usb_file_transfer".equals(key) && !disable) {
            this.mUserManager.setUserRestriction("no_debugging_features", false, user);
        }
        Binder.restoreCallingIdentity(id);
        sendHwChangedNotification(userHandle);
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
        int userHandle = user.getIdentifier();
        return this.mUserManager.hasUserRestriction(key, user);
    }

    protected void syncHwDeviceSettingsLocked(int userHandle) {
        String str;
        StringBuilder stringBuilder;
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
        } catch (Exception e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("syncHwDeviceSettingsLocked exception is ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        try {
            syncHwAdminCache(userHandle);
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("syncHwAdminCache exception is ");
            stringBuilder.append(e2.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        sendPersistentAppToIAware(userHandle);
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
        this.mHwAdminCache.syncHwAdminCache(10, isSafeModeDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(11, isAdbDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(12, isUSBOtgDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(13, isGPSDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(14, isHomeButtonDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(15, isTaskButtonDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(16, isBackButtonDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(17, isChangeLauncherDisabled(null, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.INSTALL_APKS_BLACK_LIST_POLICY, getPolicy(null, HwAdminCache.INSTALL_APKS_BLACK_LIST_POLICY, userHandle));
        this.mHwAdminCache.syncHwAdminCache(HwAdminCache.DISABLE_SCREEN_CAPTURE_POLICY, getPolicy(null, HwAdminCache.DISABLE_SCREEN_CAPTURE_POLICY, userHandle));
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
        this.mHwAdminCache.syncHwAdminCache("policy-single-app", getPolicy(null, "policy-single-app", userHandle));
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
        this.mHwAdminCache.syncHwAdminCache("disable-fingerprint-authentication", getPolicy(null, "disable-fingerprint-authentication", userHandle));
        this.mHwAdminCache.syncHwAdminCache("force-enable-BT", getPolicy(null, "force-enable-BT", userHandle));
        this.mHwAdminCache.syncHwAdminCache("force-enable-wifi", getPolicy(null, "force-enable-wifi", userHandle));
        this.mHwAdminCache.syncHwAdminCache(SettingsMDMPlugin.POLICY_ACCESSIBILITY_SERVICES_WHITE_LIST, getPolicy(null, SettingsMDMPlugin.POLICY_ACCESSIBILITY_SERVICES_WHITE_LIST, userHandle));
        this.mHwAdminCache.syncHwAdminCache("policy-file-share-disabled", getPolicy(null, "policy-file-share-disabled", userHandle));
        this.mHwAdminCache.syncHwAdminCache(SettingsMDMPlugin.DISABLED_ANDROID_ANIMATION, getPolicy(null, SettingsMDMPlugin.DISABLED_ANDROID_ANIMATION, userHandle));
    }

    private void hwSyncDeviceCapabilitiesLocked(String restriction, int userHandle) {
        boolean disabled = false;
        boolean alreadyRestricted = haveHwUserRestriction(restriction, userHandle);
        DevicePolicyData policy = getUserData(userHandle);
        int N = policy.mAdminList.size();
        for (int i = 0; i < N; i++) {
            if (isUserRestrictionDisabled(restriction, ((ActiveAdmin) policy.mAdminList.get(i)).mHwActiveAdmin)) {
                disabled = true;
                break;
            }
        }
        if (disabled != alreadyRestricted) {
            if (HWFLOW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Set ");
                stringBuilder.append(restriction);
                stringBuilder.append(" to ");
                stringBuilder.append(disabled);
                Log.i(str, stringBuilder.toString());
            }
            setHwUserRestriction(restriction, disabled, userHandle);
        }
    }

    private void hwSyncDeviceStatusBarLocked(int userHandle) {
        if (isStatusBarExpandPanelDisabled(false, userHandle)) {
            setStatusBarPanelDisabledInternal(userHandle);
        } else {
            setStatusBarPanelEnableInternal(true, userHandle);
        }
    }

    private boolean isUserRestrictionDisabled(String restriction, HwActiveAdmin admin) {
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
        if ("no_physical_media".equals(restriction) && admin.disableExternalStorage) {
            return true;
        }
        return false;
    }

    private void installPackage(String packagePath, String installerPackageName) {
        if (TextUtils.isEmpty(packagePath)) {
            throw new IllegalArgumentException("Install package path is empty");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            final File tempFile = new File(packagePath.trim()).getCanonicalFile();
            if (tempFile.getName().endsWith(".apk")) {
                PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                pm.installPackageAsUser(Uri.fromFile(tempFile).getPath(), new PackageInstallObserver() {
                    public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras) {
                        if (1 != returnCode) {
                            String str = HwDevicePolicyManagerService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("The package ");
                            stringBuilder.append(tempFile.getName());
                            stringBuilder.append("installed failed, error code: ");
                            stringBuilder.append(returnCode);
                            Log.e(str, stringBuilder.toString());
                        }
                    }
                }.getBinder(), 2, installerPackageName, 0);
                Binder.restoreCallingIdentity(ident);
                return;
            }
            Binder.restoreCallingIdentity(ident);
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Get canonical file failed for package path: ");
            stringBuilder.append(packagePath);
            stringBuilder.append(", error: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void uninstallPackage(String packageName, boolean keepData) {
        if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("Uninstall package name is empty");
        } else if (HwDevicePolicyManagerServiceUtil.isValidatePackageName(packageName)) {
            long ident = Binder.clearCallingIdentity();
            try {
                PackageManager pm = this.mContext.getPackageManager();
                if (pm.getApplicationInfo(packageName, null) == null) {
                    Binder.restoreCallingIdentity(ident);
                    return;
                }
                pm.deletePackage(packageName, null, keepData);
                Binder.restoreCallingIdentity(ident);
            } catch (NameNotFoundException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Name not found for package: ");
                stringBuilder.append(packageName);
                Log.e(str, stringBuilder.toString());
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("packageName:");
            stringBuilder2.append(packageName);
            stringBuilder2.append(" is invalid.");
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
    }

    private void filterOutSystemAppList(List<String> packageNames, int userHandle) {
        List<String> systemAppList = new ArrayList();
        String name;
        try {
            for (String name2 : packageNames) {
                if (isSystemAppExcludePreInstalled(name2)) {
                    systemAppList.add(name2);
                }
            }
            if (!systemAppList.isEmpty()) {
                packageNames.removeAll(systemAppList);
            }
        } catch (Exception e) {
            name2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("filterOutSystemAppList exception is ");
            stringBuilder.append(e);
            Log.e(name2, stringBuilder.toString());
        }
    }

    private void enforceCheckNotSystemApp(String packageName, int userHandle) {
        if (isSystemAppExcludePreInstalled(packageName)) {
            throw new IllegalArgumentException("could not operate system app");
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
        }
        restoreCallingIdentity(id);
        return false;
    }

    private boolean isSystemAppExcludePreInstalled(IPackageManager pm, String packageName, int userId) throws RemoteException {
        if (packageName == null || packageName.equals("")) {
            return false;
        }
        int flags = 0;
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 8192, userId);
            if (appInfo == null) {
                return false;
            }
            int flags2 = appInfo.flags;
            boolean flag = true;
            if ((flags2 & 1) == 0) {
                Log.d(TAG, "packageName is not systemFlag");
                flag = false;
            } else if (!((flags2 & 1) == 0 || (flags2 & 33554432) == 0)) {
                Log.w(TAG, "SystemApp preInstalledFlag");
                flag = false;
            }
            int hwFlags = appInfo.hwFlags;
            if (!((flags2 & 1) == 0 || (hwFlags & 33554432) == 0)) {
                flag = false;
                Log.d(TAG, "packageName is not systemFlag");
            }
            if ((hwFlags & 67108864) != 0) {
                flag = false;
            }
            return flag;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("could not get appInfo, exception is ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public int getSDCardEncryptionStatus() {
        int i = -1;
        if (!isSupportCrypt) {
            return -1;
        }
        String sdStatus = SystemProperties.get("vold.cryptsd.state");
        switch (sdStatus.hashCode()) {
            case -1512632483:
                if (sdStatus.equals("encrypting")) {
                    i = 1;
                    break;
                }
                break;
            case -1298848381:
                if (sdStatus.equals("enable")) {
                    i = 4;
                    break;
                }
                break;
            case -1212575282:
                if (sdStatus.equals("mismatch")) {
                    i = 5;
                    break;
                }
                break;
            case 395619662:
                if (sdStatus.equals("wait_unlock")) {
                    i = 6;
                    break;
                }
                break;
            case 1671308008:
                if (sdStatus.equals("disable")) {
                    i = 3;
                    break;
                }
                break;
            case 1959784951:
                if (sdStatus.equals("invalid")) {
                    i = 0;
                    break;
                }
                break;
            case 2066069301:
                if (sdStatus.equals("decrypting")) {
                    i = 2;
                    break;
                }
                break;
        }
        switch (i) {
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

    public void setSDCardDecryptionDisabled(ComponentName who, boolean disabled, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdminForCallerLocked(who);
                if (admin.disableDecryptSDCard != disabled) {
                    admin.disableDecryptSDCard = disabled;
                    saveSettingsLocked(userHandle);
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        if (this.mHwAdminCache != null) {
            this.mHwAdminCache.syncHwAdminCache(19, isSDCardDecryptionDisabled(null, userHandle));
        }
    }

    public boolean isSDCardDecryptionDisabled(ComponentName who, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                boolean z = getHwActiveAdminUncheckedLocked(who, userHandle).disableDecryptSDCard;
                return z;
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            int i = 0;
            while (i < N) {
                ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                if (admin.mHwActiveAdmin == null || !admin.mHwActiveAdmin.disableDecryptSDCard) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    private HwActiveAdmin getHwActiveAdminUncheckedLocked(ComponentName who, int userHandle) {
        ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
        if (admin != null) {
            HwActiveAdmin hwadmin = admin.mHwActiveAdmin;
            if (hwadmin != null) {
                return hwadmin;
            }
            hwadmin = new HwActiveAdmin();
            admin.mHwActiveAdmin = hwadmin;
            return hwadmin;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No active admin owned by uid ");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(", ComponentName:");
        stringBuilder.append(who);
        throw new SecurityException(stringBuilder.toString());
    }

    private HwActiveAdmin getHwActiveAdminForCallerLocked(ComponentName who) {
        ActiveAdmin admin = getActiveAdminForCallerLocked(who, 7);
        if (admin != null) {
            HwActiveAdmin hwadmin = admin.mHwActiveAdmin;
            if (hwadmin != null) {
                return hwadmin;
            }
            hwadmin = new HwActiveAdmin();
            admin.mHwActiveAdmin = hwadmin;
            return hwadmin;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No active admin owned by uid ");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(", ComponentName:");
        stringBuilder.append(who);
        throw new SecurityException(stringBuilder.toString());
    }

    protected void init() {
        if (!this.hasInit) {
            Iterator it = this.globalStructs.iterator();
            while (it.hasNext()) {
                PolicyStruct struct = (PolicyStruct) it.next();
                if (struct != null) {
                    struct.getOwner().init(struct);
                }
            }
            this.hasInit = true;
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
        addPlugin(new DeviceRestrictionPlugin(context));
        addPlugin(new SettingsMDMPlugin(context));
        addPlugin(new DeviceWifiPlugin(context));
        addPlugin(new DeviceBluetoothPlugin(context));
        addPlugin(new DeviceLocationPlugin(context));
        addPlugin(new DeviceP2PPlugin(context));
        addPlugin(new DeviceInfraredPlugin(context));
        addPlugin(new DeviceControlPlugin(context));
        addPlugin(new DevicePackageManagerPlugin(context));
    }

    private void addPlugin(DevicePolicyPlugin plugin) {
        if (plugin != null) {
            this.globalPlugins.add(plugin);
        }
    }

    private void addPolicyStruct(PolicyStruct struct) {
        if (struct != null) {
            this.globalStructs.add(struct);
            for (PolicyItem item : struct.getPolicyItems()) {
                globalPolicyItems.put(item.getPolicyName(), item);
            }
        }
    }

    public void bdReport(int eventID, String eventMsg) {
        if (this.mContext != null) {
            Flog.bdReport(this.mContext, eventID, eventMsg);
        }
    }

    /* JADX WARNING: Missing block: B:94:0x024f, code:
            if (r6 != 1) goto L_0x025b;
     */
    /* JADX WARNING: Missing block: B:95:0x0251, code:
            r1.mHwAdminCache.syncHwAdminCache(r3, getPolicy(null, r3, r5));
     */
    /* JADX WARNING: Missing block: B:96:0x025b, code:
            if (r22 == false) goto L_0x0261;
     */
    /* JADX WARNING: Missing block: B:97:0x025d, code:
            if (r6 != 1) goto L_0x0261;
     */
    /* JADX WARNING: Missing block: B:98:0x025f, code:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:99:0x0261, code:
            r21.onSetPolicyCompleted(r2, r3, r0);
     */
    /* JADX WARNING: Missing block: B:100:0x0266, code:
            return r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int setPolicy(ComponentName who, String policyName, Bundle policyData, int userHandle) {
        Throwable th;
        DevicePolicyPlugin devicePolicyPlugin;
        int result;
        boolean golbalPolicyChanged;
        DevicePolicyPlugin plugin;
        boolean golbalPolicyChanged2;
        ComponentName componentName = who;
        String str = policyName;
        Bundle bundle = policyData;
        int i = userHandle;
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setPolicy, policyName = ");
        stringBuilder.append(str);
        stringBuilder.append(", caller :");
        stringBuilder.append(componentName == null ? "null" : who.flattenToString());
        HwLog.d(str2, stringBuilder.toString());
        if (componentName != null) {
            DevicePolicyPlugin plugin2 = findPluginByPolicyName(str);
            String str3;
            StringBuilder stringBuilder2;
            if (plugin2 == null) {
                str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("no plugin found, pluginName = ");
                stringBuilder2.append(str);
                stringBuilder2.append(", caller :");
                stringBuilder2.append(who.flattenToString());
                HwLog.e(str3, stringBuilder2.toString());
                return -1;
            } else if (plugin2.checkCallingPermission(componentName, str)) {
                boolean golbalPolicyChanged3 = false;
                PolicyStruct struct = findPolicyStructByPolicyName(str);
                synchronized (getLockObject()) {
                    try {
                        int result2;
                        long beginTime;
                        boolean onSetPolicyResult;
                        long endTime;
                        String str4;
                        StringBuilder stringBuilder3;
                        HwActiveAdmin admin = getHwActiveAdminUncheckedLocked(componentName, i);
                        PolicyItem item = null;
                        PolicyItem newItem = null;
                        if (struct != null) {
                            try {
                                item = (PolicyItem) admin.adminPolicyItems.get(str);
                                PolicyItem oldItem = struct.getItemByPolicyName(str);
                                PolicyItem item2;
                                if (oldItem != null) {
                                    newItem = new PolicyItem(str, oldItem.getItemType());
                                    if (item == null) {
                                        try {
                                            newItem.copyFrom(oldItem);
                                            newItem.addAttrValues(newItem, bundle);
                                        } catch (Throwable th2) {
                                            th = th2;
                                            devicePolicyPlugin = plugin2;
                                        }
                                    } else {
                                        newItem.deepCopyFrom(item);
                                    }
                                    newItem.addAttrValues(newItem, bundle);
                                    PolicyItem combinedItem = combinePoliciesWithPolicyChanged(componentName, newItem, str, i);
                                    PolicyItem globalItem = (PolicyItem) globalPolicyItems.get(str);
                                    if (globalItem == null) {
                                        result = 1;
                                        try {
                                            result2 = TAG;
                                            golbalPolicyChanged = false;
                                            StringBuilder stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("no policy item found, pluginName = ");
                                            stringBuilder4.append(str);
                                            stringBuilder4.append(", caller :");
                                            stringBuilder4.append(who.flattenToString());
                                            HwLog.e(result2, stringBuilder4.toString());
                                            return -1;
                                        } catch (Throwable th3) {
                                            th = th3;
                                            devicePolicyPlugin = plugin2;
                                            result2 = result;
                                            golbalPolicyChanged3 = golbalPolicyChanged;
                                            while (true) {
                                                break;
                                            }
                                            throw th;
                                        }
                                    }
                                    result = 1;
                                    golbalPolicyChanged = false;
                                    item2 = item;
                                    if (globalItem.equals(combinedItem) != 0) {
                                        newItem.setGlobalPolicyChanged(2);
                                        golbalPolicyChanged3 = false;
                                    } else {
                                        newItem.setGlobalPolicyChanged(1);
                                        golbalPolicyChanged3 = true;
                                    }
                                    item = item2;
                                } else {
                                    result = 1;
                                    golbalPolicyChanged = false;
                                    item2 = item;
                                    str2 = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("no policy item found, pluginName = ");
                                    stringBuilder.append(str);
                                    stringBuilder.append(", caller :");
                                    stringBuilder.append(who.flattenToString());
                                    HwLog.e(str2, stringBuilder.toString());
                                    return -1;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                result = 1;
                                golbalPolicyChanged = false;
                                devicePolicyPlugin = plugin2;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        }
                        result = 1;
                        golbalPolicyChanged = false;
                        try {
                            str2 = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("when setPolicy, is global PolicyChanged ? = ");
                            stringBuilder.append(golbalPolicyChanged3);
                            HwLog.i(str2, stringBuilder.toString());
                            beginTime = System.currentTimeMillis();
                            str2 = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("onSetPolicy, begin time: ");
                            stringBuilder.append(beginTime);
                            HwLog.i(str2, stringBuilder.toString());
                            onSetPolicyResult = plugin2.onSetPolicy(componentName, str, bundle, golbalPolicyChanged3);
                            endTime = System.currentTimeMillis();
                            str4 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("onSetPolicy, costs time: ");
                            plugin = plugin2;
                            golbalPolicyChanged2 = golbalPolicyChanged3;
                        } catch (Throwable th5) {
                            th = th5;
                            devicePolicyPlugin = plugin2;
                            golbalPolicyChanged2 = golbalPolicyChanged3;
                            result2 = result;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                        try {
                            int result3;
                            stringBuilder3.append(endTime - beginTime);
                            HwLog.i(str4, stringBuilder3.toString());
                            boolean onSetPolicyResult2;
                            if (onSetPolicyResult) {
                                try {
                                    admin.adminPolicyItems.put(str, newItem);
                                    if (newItem.getItemType() == PolicyType.CONFIGURATION) {
                                        globalPolicyItems.put(str, newItem);
                                        Iterator it = this.globalStructs.iterator();
                                        while (it.hasNext()) {
                                            golbalPolicyChanged3 = false;
                                            for (String name : ((PolicyStruct) it.next()).getPolicyMap().keySet()) {
                                                onSetPolicyResult2 = onSetPolicyResult;
                                                if (newItem.getPolicyName().equals(name)) {
                                                    struct.addPolicyItem(newItem);
                                                    golbalPolicyChanged3 = true;
                                                    break;
                                                }
                                                onSetPolicyResult = onSetPolicyResult2;
                                            }
                                            onSetPolicyResult2 = onSetPolicyResult;
                                            if (golbalPolicyChanged3) {
                                                break;
                                            }
                                            onSetPolicyResult = onSetPolicyResult2;
                                        }
                                    }
                                    saveSettingsLocked(i);
                                    onSetPolicyResult = false;
                                    combineAllPolicies(i, false);
                                    result3 = 1;
                                } catch (Throwable th6) {
                                    th = th6;
                                    result2 = result;
                                    devicePolicyPlugin = plugin;
                                    golbalPolicyChanged3 = golbalPolicyChanged2;
                                    while (true) {
                                        break;
                                    }
                                    throw th;
                                }
                            }
                            onSetPolicyResult2 = onSetPolicyResult;
                            onSetPolicyResult = false;
                            String str5 = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("onSetPolicy failed, pluginName = ");
                            stringBuilder.append(str);
                            stringBuilder.append(", caller :");
                            stringBuilder.append(who.flattenToString());
                            HwLog.e(str5, stringBuilder.toString());
                            result3 = -1;
                            result2 = result3;
                            try {
                            } catch (Throwable th7) {
                                th = th7;
                                devicePolicyPlugin = plugin;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        } catch (Throwable th8) {
                            th = th8;
                            devicePolicyPlugin = plugin;
                            result2 = result;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                    } catch (Throwable th9) {
                        th = th9;
                        result = 1;
                        devicePolicyPlugin = plugin2;
                        golbalPolicyChanged = false;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                }
            } else {
                str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("permission denied: ");
                stringBuilder2.append(who.flattenToString());
                HwLog.e(str3, stringBuilder2.toString());
                return -1;
            }
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    public Bundle getPolicy(ComponentName who, String policyName, int userHandle) {
        DevicePolicyPlugin plugin = findPluginByPolicyName(policyName);
        String str;
        StringBuilder stringBuilder;
        if (plugin == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("no plugin found, policyName = ");
            stringBuilder.append(policyName);
            stringBuilder.append(", caller :");
            stringBuilder.append(who == null ? "null" : who.flattenToString());
            HwLog.e(str, stringBuilder.toString());
            return null;
        }
        String str2;
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("get :");
        stringBuilder.append(policyName);
        if (who == null) {
            str2 = "";
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" ,cal :");
            stringBuilder2.append(who.flattenToString());
            str2 = stringBuilder2.toString();
        }
        stringBuilder.append(str2);
        HwLog.d(str, stringBuilder.toString());
        Bundle resultBundle = null;
        synchronized (getLockObject()) {
            if (who != null) {
                PolicyItem item = (PolicyItem) getHwActiveAdminUncheckedLocked(who, userHandle).adminPolicyItems.get(policyName);
                if (item != null) {
                    resultBundle = item.combineAllAttributes();
                }
            } else if (USER_ISOLATION_POLICY_LIST.contains(policyName)) {
                resultBundle = combinePoliciesAsUser(policyName, userHandle).combineAllAttributes();
            } else if (globalPolicyItems.get(policyName) != null) {
                resultBundle = ((PolicyItem) globalPolicyItems.get(policyName)).combineAllAttributes();
            }
            notifyOnGetPolicy(plugin, who, policyName, resultBundle);
        }
        return resultBundle;
    }

    private void notifyOnGetPolicy(DevicePolicyPlugin plugin, ComponentName who, String policyName, Bundle policyData) {
        plugin.onGetPolicy(who, policyName, policyData);
    }

    /* JADX WARNING: Missing block: B:66:0x0188, code:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:67:0x0189, code:
            if (r6 != 1) goto L_0x0195;
     */
    /* JADX WARNING: Missing block: B:68:0x018b, code:
            r1.mHwAdminCache.syncHwAdminCache(r3, getPolicy(null, r3, r5));
     */
    /* JADX WARNING: Missing block: B:69:0x0195, code:
            if (r22 == false) goto L_0x019a;
     */
    /* JADX WARNING: Missing block: B:70:0x0197, code:
            if (r6 != 1) goto L_0x019a;
     */
    /* JADX WARNING: Missing block: B:71:0x019a, code:
            r0 = false;
     */
    /* JADX WARNING: Missing block: B:72:0x019b, code:
            r21.onRemovePolicyCompleted(r2, r3, r0);
     */
    /* JADX WARNING: Missing block: B:73:0x01a0, code:
            return r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int removePolicy(ComponentName who, String policyName, Bundle policyData, int userHandle) {
        Throwable th;
        PolicyStruct policyStruct;
        int result;
        boolean z;
        int i;
        ComponentName componentName = who;
        String str = policyName;
        Bundle bundle = policyData;
        int i2 = userHandle;
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("removePolicy, policyName = ");
        stringBuilder.append(str);
        stringBuilder.append(", caller :");
        stringBuilder.append(componentName == null ? "null" : who.flattenToString());
        HwLog.d(str2, stringBuilder.toString());
        if (componentName != null) {
            DevicePolicyPlugin plugin = findPluginByPolicyName(str);
            String str3;
            StringBuilder stringBuilder2;
            if (plugin == null) {
                str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("no plugin found, pluginName = ");
                stringBuilder2.append(str);
                stringBuilder2.append(", caller :");
                stringBuilder2.append(who.flattenToString());
                HwLog.e(str3, stringBuilder2.toString());
                return -1;
            } else if (plugin.checkCallingPermission(componentName, str)) {
                PolicyStruct struct = findPolicyStructByPolicyName(str);
                synchronized (getLockObject()) {
                    try {
                        boolean golbalPolicyChanged;
                        long beginTime;
                        String str4;
                        HwActiveAdmin admin = getHwActiveAdminUncheckedLocked(componentName, i2);
                        PolicyItem item = null;
                        PolicyItem newItem = null;
                        if (struct != null) {
                            try {
                                item = (PolicyItem) admin.adminPolicyItems.get(str);
                                if (item != null) {
                                    newItem = new PolicyItem(str, struct.getItemByPolicyName(str).getItemType());
                                    newItem.deepCopyFrom(item);
                                    newItem.removeAttrValues(newItem, bundle);
                                    PolicyItem combinedItem = combinePoliciesWithPolicyChanged(componentName, newItem, str, i2);
                                    PolicyItem globalItem = (PolicyItem) globalPolicyItems.get(str);
                                    if (globalItem == null) {
                                        newItem.setGlobalPolicyChanged(0);
                                        golbalPolicyChanged = false;
                                    } else if (globalItem.equals(combinedItem)) {
                                        newItem.setGlobalPolicyChanged(2);
                                        golbalPolicyChanged = false;
                                    } else {
                                        newItem.setGlobalPolicyChanged(1);
                                        golbalPolicyChanged = true;
                                    }
                                } else {
                                    golbalPolicyChanged = false;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                policyStruct = struct;
                                while (true) {
                                    try {
                                        break;
                                    } catch (Throwable th3) {
                                        th = th3;
                                    }
                                }
                                throw th;
                            }
                        }
                        golbalPolicyChanged = false;
                        try {
                            String str5 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("when removePolicy,  is global PolicyChanged ? = ");
                            stringBuilder3.append(golbalPolicyChanged);
                            HwLog.i(str5, stringBuilder3.toString());
                            beginTime = System.currentTimeMillis();
                            str4 = TAG;
                            result = 1;
                        } catch (Throwable th4) {
                            th = th4;
                            result = 1;
                            z = golbalPolicyChanged;
                            policyStruct = struct;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                        try {
                            stringBuilder = new StringBuilder();
                        } catch (Throwable th5) {
                            th = th5;
                            z = golbalPolicyChanged;
                            policyStruct = struct;
                            i = result;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                        try {
                            stringBuilder.append("onRemovePolicy, begin time: ");
                            stringBuilder.append(beginTime);
                            HwLog.i(str4, stringBuilder.toString());
                            boolean onRemoveResult = plugin.onRemovePolicy(componentName, str, bundle, golbalPolicyChanged);
                            long endTime = System.currentTimeMillis();
                            String str6 = TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            DevicePolicyPlugin plugin2 = plugin;
                            try {
                                stringBuilder4.append("onRemovePolicy, costs time: ");
                                z = golbalPolicyChanged;
                                try {
                                    stringBuilder4.append(endTime - beginTime);
                                    HwLog.i(str6, stringBuilder4.toString());
                                    if (onRemoveResult) {
                                        if (item != null) {
                                            try {
                                                if (item.getItemType() == PolicyType.LIST && bundle != null) {
                                                    admin.adminPolicyItems.put(str, newItem);
                                                    saveSettingsLocked(i2);
                                                    combineAllPolicies(i2, true);
                                                    i = 1;
                                                }
                                            } catch (Throwable th6) {
                                                th = th6;
                                                i = result;
                                                plugin = plugin2;
                                                golbalPolicyChanged = z;
                                                while (true) {
                                                    break;
                                                }
                                                throw th;
                                            }
                                        }
                                        admin.adminPolicyItems.remove(str);
                                        saveSettingsLocked(i2);
                                        combineAllPolicies(i2, true);
                                        i = 1;
                                    } else {
                                        String str7 = TAG;
                                        StringBuilder stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("onSetPolicy failed, pluginName = ");
                                        stringBuilder5.append(str);
                                        stringBuilder5.append(", caller :");
                                        stringBuilder5.append(who.flattenToString());
                                        HwLog.e(str7, stringBuilder5.toString());
                                        i = -1;
                                    }
                                } catch (Throwable th7) {
                                    th = th7;
                                    plugin = plugin2;
                                    i = result;
                                    while (true) {
                                        break;
                                    }
                                    throw th;
                                }
                                try {
                                } catch (Throwable th8) {
                                    th = th8;
                                    plugin = plugin2;
                                    while (true) {
                                        break;
                                    }
                                    throw th;
                                }
                            } catch (Throwable th9) {
                                th = th9;
                                z = golbalPolicyChanged;
                                plugin = plugin2;
                                i = result;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            z = golbalPolicyChanged;
                            i = result;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        result = 1;
                        policyStruct = struct;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                }
            } else {
                str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("permission denied: ");
                stringBuilder2.append(who.flattenToString());
                HwLog.e(str3, stringBuilder2.toString());
                return -1;
            }
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    protected void notifyPlugins(ComponentName who, int userHandle) {
        HwDevicePolicyManagerService hwDevicePolicyManagerService = this;
        ComponentName componentName = who;
        int i = userHandle;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyPlugins: ");
        stringBuilder.append(componentName == null ? "null" : who.flattenToString());
        stringBuilder.append(" userId: ");
        stringBuilder.append(i);
        HwLog.d(str, stringBuilder.toString());
        ActiveAdmin activeAdminToRemove = getActiveAdminUncheckedLocked(who, userHandle);
        if (activeAdminToRemove != null && activeAdminToRemove.mHwActiveAdmin != null && activeAdminToRemove.mHwActiveAdmin.adminPolicyItems != null && !activeAdminToRemove.mHwActiveAdmin.adminPolicyItems.isEmpty()) {
            Iterator it = hwDevicePolicyManagerService.globalStructs.iterator();
            while (it.hasNext()) {
                PolicyStruct struct = (PolicyStruct) it.next();
                if (struct != null) {
                    ArrayList<PolicyItem> removedPluginItems = new ArrayList();
                    for (PolicyItem removedItem : activeAdminToRemove.mHwActiveAdmin.adminPolicyItems.values()) {
                        if (removedItem != null) {
                            PolicyItem combinedItem = hwDevicePolicyManagerService.combinePoliciesWithoutRemovedPolicyItem(componentName, removedItem.getPolicyName(), i);
                            if (((PolicyItem) globalPolicyItems.get(removedItem.getPolicyName())) == null) {
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
                    if (!removedPluginItems.isEmpty()) {
                        DevicePolicyPlugin plugin = struct.getOwner();
                        if (plugin == null) {
                            HwLog.w(TAG, " policy struct has no owner");
                            return;
                        }
                        hwDevicePolicyManagerService.effectedItems.add(new EffectedItem(componentName, plugin, removedPluginItems));
                        long beginTime = System.currentTimeMillis();
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("onActiveAdminRemoved, begin time: ");
                        stringBuilder2.append(beginTime);
                        HwLog.i(str2, stringBuilder2.toString());
                        plugin.onActiveAdminRemoved(componentName, removedPluginItems);
                        long endTime = System.currentTimeMillis();
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("onActiveAdminRemoved, costs time: ");
                        stringBuilder3.append(endTime - beginTime);
                        HwLog.i(str3, stringBuilder3.toString());
                    }
                    hwDevicePolicyManagerService = this;
                    componentName = who;
                }
            }
        }
    }

    protected void removeActiveAdminCompleted(ComponentName who) {
        synchronized (getLockObject()) {
            if (!this.effectedItems.isEmpty()) {
                Iterator<EffectedItem> it = this.effectedItems.iterator();
                while (it.hasNext()) {
                    EffectedItem effectedItem = (EffectedItem) it.next();
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
            if (struct != null) {
                if (struct.containsPolicyName(policyName)) {
                    return struct;
                }
            }
        }
        return null;
    }

    private void combineAllPolicies(int userHandle, boolean shouldChange) {
        synchronized (getLockObject()) {
            DevicePolicyData policy = getUserData(userHandle);
            Iterator it = this.globalStructs.iterator();
            while (it.hasNext()) {
                PolicyStruct struct = (PolicyStruct) it.next();
                for (String policyName : struct.getPolicyMap().keySet()) {
                    PolicyItem globalItem = new PolicyItem(policyName, struct.getItemByPolicyName(policyName).getItemType());
                    globalItem.copyFrom(struct.getItemByPolicyName(policyName));
                    int N = policy.mAdminList.size();
                    int i;
                    ActiveAdmin admin;
                    PolicyItem adminItem;
                    if (globalItem.getItemType() != PolicyType.CONFIGURATION) {
                        for (i = 0; i < N; i++) {
                            admin = (ActiveAdmin) policy.mAdminList.get(i);
                            if (admin.mHwActiveAdmin != null) {
                                adminItem = (PolicyItem) admin.mHwActiveAdmin.adminPolicyItems.get(policyName);
                                if (adminItem != null && adminItem.hasAnyNonNullAttribute()) {
                                    traverseCombinePolicyItem(globalItem, adminItem);
                                }
                            }
                        }
                    } else if (shouldChange) {
                        for (i = N - 1; i >= 0; i--) {
                            admin = (ActiveAdmin) policy.mAdminList.get(i);
                            if (admin.mHwActiveAdmin != null) {
                                adminItem = (PolicyItem) admin.mHwActiveAdmin.adminPolicyItems.get(policyName);
                                if (adminItem != null) {
                                    globalItem = adminItem;
                                    String str = TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("global policy will change: ");
                                    stringBuilder.append(policyName);
                                    HwLog.w(str, stringBuilder.toString());
                                    break;
                                }
                            }
                        }
                    } else {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("global policy will not change: ");
                        stringBuilder2.append(policyName);
                        HwLog.w(str2, stringBuilder2.toString());
                    }
                    globalPolicyItems.put(policyName, globalItem);
                    struct.addPolicyItem(globalItem);
                }
            }
        }
    }

    private PolicyItem combinePoliciesWithPolicyChanged(ComponentName who, PolicyItem newItem, String policyName, int userHandle) {
        PolicyItem globalAdminItem;
        synchronized (getLockObject()) {
            DevicePolicyData policy = getUserData(userHandle);
            ActiveAdmin activeAdmin = getActiveAdminUncheckedLocked(who, userHandle);
            ArrayList<ActiveAdmin> adminList = new ArrayList();
            Iterator it = policy.mAdminList.iterator();
            while (it.hasNext()) {
                adminList.add((ActiveAdmin) it.next());
            }
            if (activeAdmin != null && adminList.size() > 0) {
                adminList.remove(activeAdmin);
            }
            PolicyStruct struct = findPolicyStructByPolicyName(policyName);
            globalAdminItem = new PolicyItem(policyName, struct.getItemByPolicyName(policyName).getItemType());
            globalAdminItem.copyFrom(struct.getItemByPolicyName(policyName));
            int N = adminList.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin1 = (ActiveAdmin) adminList.get(i);
                if (admin1.mHwActiveAdmin != null) {
                    PolicyItem adminItem = (PolicyItem) admin1.mHwActiveAdmin.adminPolicyItems.get(policyName);
                    if (adminItem != null && adminItem.hasAnyNonNullAttribute()) {
                        traverseCombinePolicyItem(globalAdminItem, adminItem);
                    }
                }
            }
            traverseCombinePolicyItem(globalAdminItem, newItem);
        }
        return globalAdminItem;
    }

    private PolicyItem combinePoliciesAsUser(String policyName, int userHandle) {
        PolicyItem resultPolicyItem;
        synchronized (getLockObject()) {
            DevicePolicyData policy = getUserData(userHandle);
            ArrayList<ActiveAdmin> adminList = new ArrayList();
            Iterator it = policy.mAdminList.iterator();
            while (it.hasNext()) {
                adminList.add((ActiveAdmin) it.next());
            }
            PolicyStruct struct = findPolicyStructByPolicyName(policyName);
            resultPolicyItem = new PolicyItem(policyName, struct.getItemByPolicyName(policyName).getItemType());
            resultPolicyItem.copyFrom(struct.getItemByPolicyName(policyName));
            int size = adminList.size();
            for (int i = 0; i < size; i++) {
                ActiveAdmin admin = (ActiveAdmin) adminList.get(i);
                if (admin.mHwActiveAdmin != null) {
                    PolicyItem policyItemAsAdmin = (PolicyItem) admin.mHwActiveAdmin.adminPolicyItems.get(policyName);
                    if (policyItemAsAdmin != null && policyItemAsAdmin.hasAnyNonNullAttribute()) {
                        traverseCombinePolicyItem(resultPolicyItem, policyItemAsAdmin);
                    }
                }
            }
        }
        return resultPolicyItem;
    }

    private PolicyItem combinePoliciesWithoutRemovedPolicyItem(ComponentName who, String policyName, int userHandle) {
        DevicePolicyData policy = getUserData(userHandle);
        ArrayList<ActiveAdmin> adminList = new ArrayList();
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
        PolicyItem oldItem = findPolicyStructByPolicyName(policyName).getItemByPolicyName(policyName);
        PolicyItem globalAdminItem = null;
        if (oldItem != null) {
            globalAdminItem = new PolicyItem(policyName, oldItem.getItemType());
            globalAdminItem.copyFrom(oldItem);
            int N = adminList.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin1 = (ActiveAdmin) adminList.get(i);
                if (admin1.mHwActiveAdmin != null) {
                    PolicyItem adminItem = (PolicyItem) admin1.mHwActiveAdmin.adminPolicyItems.get(policyName);
                    if (adminItem != null && adminItem.hasAnyNonNullAttribute() && adminItem.getPolicyName().equals(policyName)) {
                        traverseCombinePolicyItem(globalAdminItem, adminItem);
                    }
                }
            }
        }
        return globalAdminItem;
    }

    private void traverseCombinePolicyItem(PolicyItem oldRoot, PolicyItem newRoot) {
        if (oldRoot != null && newRoot != null) {
            oldRoot.setAttributes(combineAttributes(oldRoot.getAttributes(), newRoot.getAttributes(), oldRoot));
            int n = oldRoot.getChildItem().size();
            for (int i = 0; i < n; i++) {
                traverseCombinePolicyItem((PolicyItem) oldRoot.getChildItem().get(i), (PolicyItem) newRoot.getChildItem().get(i));
            }
        }
    }

    private Bundle combineAttributes(Bundle oldAttr, Bundle newAttr, PolicyItem item) {
        switch (AnonymousClass5.$SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[item.getItemType().ordinal()]) {
            case 1:
                for (String key : newAttr.keySet()) {
                    if (newAttr.get(key) != null) {
                        boolean state = oldAttr.getBoolean(key) || newAttr.getBoolean(key);
                        oldAttr.putBoolean(key, state);
                    }
                }
                break;
            case 2:
                for (String key2 : newAttr.keySet()) {
                    if (newAttr.get(key2) != null) {
                        ArrayList<String> oldPolicyList = oldAttr.getStringArrayList(key2);
                        ArrayList<String> newPolicyList = newAttr.getStringArrayList(key2);
                        if (oldPolicyList == null) {
                            oldPolicyList = new ArrayList();
                        }
                        HwDevicePolicyManagerServiceUtil.addListWithoutDuplicate(oldPolicyList, newPolicyList);
                        oldAttr.putStringArrayList(key2, oldPolicyList);
                    }
                }
                break;
            case 3:
                for (String key22 : newAttr.keySet()) {
                    if (newAttr.get(key22) != null) {
                        oldAttr.putString(key22, newAttr.getString(key22));
                    }
                }
                break;
        }
        return oldAttr;
    }

    public ArrayList<String> queryBrowsingHistory(ComponentName who, int userHandle) {
        String policyName = "network-black-list";
        ArrayList<String> historyList = new ArrayList();
        if (who != null) {
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_NETWORK_MANAGER", "does not have network MDM permission!");
            enforceHwCrossUserPermission(userHandle);
            getHwActiveAdmin(who, userHandle);
            DevicePolicyPlugin plugin = findPluginByPolicyName("network-black-list");
            if (plugin == null || !(plugin instanceof DeviceNetworkPlugin)) {
                HwLog.e(TAG, "no DeviceNetworkPlugin found, pluginName = network-black-list");
                return historyList;
            }
            DeviceNetworkPlugin deviceNetworkPlugin = (DeviceNetworkPlugin) plugin;
            long callingId = Binder.clearCallingIdentity();
            historyList = deviceNetworkPlugin.queryBrowsingHistory();
            Binder.restoreCallingIdentity(callingId);
            return historyList;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    /* JADX WARNING: Removed duplicated region for block: B:36:0x00b9 A:{PHI: r4 , ExcHandler: org.xmlpull.v1.XmlPullParserException (e org.xmlpull.v1.XmlPullParserException), Splitter: B:9:0x0064} */
    /* JADX WARNING: Missing block: B:38:?, code:
            com.android.server.devicepolicy.HwLog.e(TAG, "XmlPullParserException | IOException");
     */
    /* JADX WARNING: Missing block: B:40:?, code:
            libcore.io.IoUtils.closeQuietly(r4);
     */
    /* JADX WARNING: Missing block: B:42:0x00c6, code:
            com.android.server.devicepolicy.HwLog.d(TAG, "Can't find HwPolicy");
     */
    /* JADX WARNING: Missing block: B:43:0x00cd, code:
            return false;
     */
    /* JADX WARNING: Missing block: B:45:?, code:
            libcore.io.IoUtils.closeQuietly(r4);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean hasHwPolicy(int userHandle) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hasHwPolicy, userHandle :");
        stringBuilder.append(userHandle);
        HwLog.d(str, stringBuilder.toString());
        synchronized (getLockObject()) {
            String base;
            String DEVICE_POLICIES_XML = "device_policies.xml";
            if (userHandle == 0) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("/data/system/");
                stringBuilder2.append(DEVICE_POLICIES_XML);
                base = stringBuilder2.toString();
            } else {
                base = new File(Environment.getUserSystemDirectory(userHandle), DEVICE_POLICIES_XML).getAbsolutePath();
            }
            File file = new File(base);
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(base);
            stringBuilder3.append(".tmp");
            JournaledFile journal = new JournaledFile(file, new File(stringBuilder3.toString()));
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(journal.chooseForRead());
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, StandardCharsets.UTF_8.name());
                while (true) {
                    int next = parser.next();
                    int type = next;
                    if (next != 1) {
                        if (type == 2) {
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
                                HwLog.d(TAG, "Can't find HwPolicy");
                                IoUtils.closeQuietly(stream);
                                return false;
                            }
                        }
                    }
                    break;
                }
            } catch (XmlPullParserException e) {
            }
        }
    }

    protected boolean isSecureBlockEncrypted() {
        if (!StorageManager.isBlockEncrypted()) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        boolean isSecure;
        try {
            isSecure = IStorageManager.Stub.asInterface(ServiceManager.getService("mount")).isSecure();
            return isSecure;
        } catch (RemoteException e) {
            isSecure = TAG;
            Log.e(isSecure, "Error getting encryption type");
            return false;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX WARNING: Missing block: B:68:0x014b, code:
            return 1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int configVpnProfile(ComponentName who, Bundle para, int userHandle) {
        ComponentName componentName = who;
        Bundle bundle = para;
        int i = userHandle;
        enforceHwCrossUserPermission(i);
        this.mContext.enforceCallingOrSelfPermission(MDM_VPN_PERMISSION, "NEED MDM_VPN PERMISSION");
        synchronized (getLockObject()) {
            KeyStore mKeyStore = KeyStore.getInstance();
            boolean paraIsNull = componentName == null || bundle == null;
            if (paraIsNull) {
                Log.e(TAG, "Bundle para is null or componentName is null!");
                return -1;
            } else if (isValidVpnConfig(bundle)) {
                VpnProfile profile = getProfile(bundle);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("VPN_");
                stringBuilder.append(profile.key);
                if (mKeyStore.put(stringBuilder.toString(), profile.encode(), -1, 0)) {
                    Bundle speProvider;
                    String key = bundle.getString("key");
                    DevicePolicyData policy = getUserData(i);
                    int N = policy.mAdminList.size();
                    int i2 = 0;
                    while (i2 < N) {
                        KeyStore mKeyStore2;
                        ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i2);
                        if (admin.mHwActiveAdmin == null || admin.mHwActiveAdmin.vpnProviderlist == null) {
                            mKeyStore2 = mKeyStore;
                        } else {
                            speProvider = null;
                            Iterator it = admin.mHwActiveAdmin.vpnProviderlist.iterator();
                            while (it.hasNext()) {
                                mKeyStore2 = mKeyStore;
                                Iterator it2 = it;
                                Bundle provider = (Bundle) it.next();
                                if (key.equals(provider.getString("key"))) {
                                    speProvider = provider;
                                    break;
                                }
                                mKeyStore = mKeyStore2;
                                it = it2;
                            }
                            mKeyStore2 = mKeyStore;
                            if (speProvider != null) {
                                admin.mHwActiveAdmin.vpnProviderlist.remove(speProvider);
                                saveSettingsLocked(i);
                            }
                        }
                        i2++;
                        mKeyStore = mKeyStore2;
                    }
                    HwActiveAdmin ap = getHwActiveAdmin(componentName, i);
                    if (ap.vpnProviderlist != null) {
                        boolean isAlready = false;
                        speProvider = null;
                        for (Bundle provider2 : ap.vpnProviderlist) {
                            boolean paraIsNotNull = provider2 == null || isEmpty(provider2.getString("key"));
                            if (!paraIsNotNull) {
                                if (key.equals(provider2.getString("key"))) {
                                    isAlready = true;
                                    speProvider = provider2;
                                    break;
                                }
                            }
                            paraIsNotNull = who;
                        }
                        boolean z = isAlready && speProvider != null;
                        if (z) {
                            ap.vpnProviderlist.remove(speProvider);
                        }
                        ap.vpnProviderlist.add(bundle);
                        saveSettingsLocked(i);
                    } else {
                        ap.vpnProviderlist = new ArrayList();
                        ap.vpnProviderlist.add(bundle);
                        saveSettingsLocked(i);
                    }
                } else {
                    Log.e(TAG, "Set vpn failed, check the config.");
                    return -1;
                }
            } else {
                Log.e(TAG, "This Config isn't valid vpnConfig");
                return -1;
            }
        }
    }

    public int removeVpnProfile(ComponentName who, Bundle para, int userHandle) {
        int i = userHandle;
        String key = para.getString("key");
        if (who == null || isEmpty(key)) {
            Log.e(TAG, "ComponentName or key is empty.");
            return -1;
        }
        enforceHwCrossUserPermission(i);
        this.mContext.enforceCallingOrSelfPermission(MDM_VPN_PERMISSION, "NEED MDM_VPN PERMISSION");
        synchronized (getLockObject()) {
            KeyStore mKeyStore = KeyStore.getInstance();
            boolean hasDeleted = false;
            DevicePolicyData policy = getUserData(i);
            int N = policy.mAdminList.size();
            for (int i2 = 0; i2 < N; i2++) {
                ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i2);
                if (!(admin.mHwActiveAdmin == null || admin.mHwActiveAdmin.vpnProviderlist == null)) {
                    Bundle specProvider = null;
                    for (Bundle provider : admin.mHwActiveAdmin.vpnProviderlist) {
                        if (key.equals(provider.getString("key"))) {
                            specProvider = provider;
                            break;
                        }
                    }
                    if (specProvider != null) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("VPN_");
                        stringBuilder.append(key);
                        if (mKeyStore.delete(stringBuilder.toString())) {
                            admin.mHwActiveAdmin.vpnProviderlist.remove(specProvider);
                            saveSettingsLocked(i);
                            hasDeleted = true;
                        } else {
                            Log.e(TAG, "Delete vpn failed, check the key.");
                            return -1;
                        }
                    }
                    continue;
                }
            }
            if (hasDeleted) {
                return 1;
            }
            return -1;
        }
    }

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
                HwActiveAdmin hwAdmin = getHwActiveAdmin(who, userHandle);
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
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
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

    public Bundle getVpnList(ComponentName who, Bundle keyWords, int userHandle) {
        enforceHwCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission(MDM_VPN_PERMISSION, "NEED MDM_VPN PERMISSION");
        synchronized (getLockObject()) {
            ArrayList<String> vpnKeyList = new ArrayList();
            Bundle vpnListBundle = new Bundle();
            if (who != null) {
                HwActiveAdmin admin = getHwActiveAdmin(who, userHandle);
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
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin2 = (ActiveAdmin) policy.mAdminList.get(i);
                if (!(admin2.mHwActiveAdmin == null || admin2.mHwActiveAdmin.vpnProviderlist == null)) {
                    for (Bundle provider2 : admin2.mHwActiveAdmin.vpnProviderlist) {
                        if (!(isEmpty(provider2.getString("key")) || vpnKeyList.contains(provider2.getString("key")))) {
                            vpnKeyList.add(provider2.getString("key"));
                        }
                    }
                }
            }
            vpnListBundle.putStringArrayList("keylist", vpnKeyList);
            return vpnListBundle;
        }
    }

    private VpnProfile getProfile(Bundle vpnBundle) {
        VpnProfile profile = new VpnProfile(vpnBundle.getString("key"));
        profile.name = vpnBundle.getString("name");
        profile.type = Integer.parseInt(vpnBundle.getString(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE));
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

    /* JADX WARNING: Missing block: B:23:0x007c, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isValidVpnConfig(Bundle para) {
        if (para == null || isEmpty(para.getString("key")) || isEmpty(para.getString("name")) || isEmpty(para.getString(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE)) || isEmpty(para.getString("server")) || Integer.parseInt(para.getString(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE)) < 0 || Integer.parseInt(para.getString(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE)) > 6) {
            return false;
        }
        switch (Integer.parseInt(para.getString(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE))) {
            case 2:
            case 4:
                return isEmpty(para.getString("ipsecSecret")) ^ true;
            case 3:
            case 5:
                return isEmpty(para.getString("ipsecUserCert")) ^ true;
            default:
                return true;
        }
    }

    public static boolean isEmpty(String str) {
        return TextUtils.isEmpty(str);
    }

    public boolean getScreenCaptureDisabled(ComponentName who, int userHandle) {
        if (who != null) {
            return super.getScreenCaptureDisabled(who, userHandle);
        }
        boolean z = super.getScreenCaptureDisabled(who, userHandle) || HwDeviceManager.mdmDisallowOp(20, null);
        return z;
    }

    public boolean formatSDCard(ComponentName who, String diskId, int userHandle) {
        boolean z;
        if (who != null) {
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_SDCARD", "does not have sd card MDM permission!");
            synchronized (getLockObject()) {
                if (getActiveAdminUncheckedLocked(who, userHandle) != null) {
                    long token = Binder.clearCallingIdentity();
                    try {
                        ((StorageManager) this.mContext.getSystemService("storage")).partitionPublic(diskId);
                        Binder.restoreCallingIdentity(token);
                    } catch (Exception e) {
                        try {
                            z = TAG;
                            HwLog.e(z, "format sd card data error!");
                            z = false;
                        } finally {
                            Binder.restoreCallingIdentity(token);
                        }
                        return z;
                    }
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("No active admin owned by uid ");
                stringBuilder.append(Binder.getCallingUid());
                stringBuilder.append(", ComponentName:");
                stringBuilder.append(who);
                throw new SecurityException(stringBuilder.toString());
            }
            return true;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

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
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No active admin owned by uid ");
                    stringBuilder.append(Binder.getCallingUid());
                    stringBuilder.append(", ComponentName:");
                    stringBuilder.append(who);
                    throw new SecurityException(stringBuilder.toString());
                }
            }
            return;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    public boolean isAccountDisabled(ComponentName who, String accountType, int userHandle) {
        synchronized (getLockObject()) {
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                if (admin != null) {
                    boolean contains = admin.accountTypesWithManagementDisabled.contains(accountType);
                    return contains;
                }
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            for (int i = 0; i < N; i++) {
                if (((ActiveAdmin) policy.mAdminList.get(i)).accountTypesWithManagementDisabled.contains(accountType)) {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0025, code:
            r9 = false;
     */
    /* JADX WARNING: Missing block: B:10:0x0028, code:
            if (r3 != 0) goto L_0x0034;
     */
    /* JADX WARNING: Missing block: B:13:?, code:
            r9 = com.android.server.devicepolicy.CertInstallHelper.installPkcs12Cert(r22, r4, r5, r6);
     */
    /* JADX WARNING: Missing block: B:15:0x0034, code:
            r10 = r22;
     */
    /* JADX WARNING: Missing block: B:16:0x0036, code:
            if (r3 != 1) goto L_0x009d;
     */
    /* JADX WARNING: Missing block: B:18:0x003c, code:
            r9 = com.android.server.devicepolicy.CertInstallHelper.installX509Cert(r4, r5, r6);
     */
    /* JADX WARNING: Missing block: B:19:0x003e, code:
            if (r24 == false) goto L_0x009c;
     */
    /* JADX WARNING: Missing block: B:20:0x0040, code:
            r12 = r1.mInjector.binderGetCallingUid();
            r13 = r1.mInjector.binderClearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:22:?, code:
            r7 = android.security.KeyChain.bindAsUser(r1.mContext, android.os.UserHandle.getUserHandleForUid(r12));
     */
    /* JADX WARNING: Missing block: B:24:?, code:
            r7.getService().setGrant(r12, r5, true);
     */
    /* JADX WARNING: Missing block: B:26:?, code:
            r7.close();
     */
    /* JADX WARNING: Missing block: B:27:0x0061, code:
            r1.mInjector.binderRestoreCallingIdentity(r13);
     */
    /* JADX WARNING: Missing block: B:28:0x0067, code:
            return true;
     */
    /* JADX WARNING: Missing block: B:30:0x006a, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:32:?, code:
            r16 = r0;
            com.android.server.devicepolicy.HwLog.e(TAG, "set grant certificate");
     */
    /* JADX WARNING: Missing block: B:34:?, code:
            r7.close();
     */
    /* JADX WARNING: Missing block: B:35:0x0079, code:
            r7.close();
     */
    /* JADX WARNING: Missing block: B:40:?, code:
            com.android.server.devicepolicy.HwLog.w(TAG, "Interrupted while set granting certificate");
            java.lang.Thread.currentThread().interrupt();
     */
    /* JADX WARNING: Missing block: B:41:0x008e, code:
            r1.mInjector.binderRestoreCallingIdentity(r13);
     */
    /* JADX WARNING: Missing block: B:42:0x0095, code:
            return false;
     */
    /* JADX WARNING: Missing block: B:43:0x0096, code:
            r1.mInjector.binderRestoreCallingIdentity(r13);
     */
    /* JADX WARNING: Missing block: B:45:0x009c, code:
            return r9;
     */
    /* JADX WARNING: Missing block: B:48:0x00a5, code:
            throw new java.lang.IllegalArgumentException("the type of the installed cert is not illegal");
     */
    /* JADX WARNING: Missing block: B:49:0x00a6, code:
            com.android.server.devicepolicy.HwLog.e(TAG, "throw error when install cert");
     */
    /* JADX WARNING: Missing block: B:50:0x00b0, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean installCertificateWithType(ComponentName who, int type, byte[] certBuffer, String alias, String password, int certInstallType, boolean requestAccess, int userHandle) {
        Throwable th;
        ComponentName componentName = who;
        int i = type;
        byte[] bArr = certBuffer;
        String str = alias;
        int i2 = certInstallType;
        String str2;
        if (componentName != null) {
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have install cert MDM permission!");
            synchronized (getLockObject()) {
                try {
                    if (getActiveAdminUncheckedLocked(componentName, userHandle) != null) {
                    } else {
                        str2 = password;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("No active admin owned by uid ");
                        stringBuilder.append(Binder.getCallingUid());
                        stringBuilder.append(", ComponentName:");
                        stringBuilder.append(componentName);
                        throw new SecurityException(stringBuilder.toString());
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
        str2 = password;
        int i3 = userHandle;
        throw new IllegalArgumentException("ComponentName is null");
    }

    protected long getUsrSetExtendTime() {
        String value = getPolicy(null, PASSWORD_CHANGE_EXTEND_TIME, UserHandle.myUserId()).getString("value");
        if (value == null || "".equals(value)) {
            return super.getUsrSetExtendTime();
        }
        return Long.parseLong(value);
    }

    public void setSilentActiveAdmin(ComponentName who, int userHandle) {
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have PERMISSION_MDM_DEVICE_MANAGER permission!");
        synchronized (getLockObject()) {
            if (who != null) {
                if (!isAdminActive(who, userHandle)) {
                    HwLog.d(TAG, "setSilentActiveAdmin, mHasHwMdmFeature active supported.");
                    long identityToken = Binder.clearCallingIdentity();
                    try {
                        setActiveAdmin(who, true, userHandle);
                    } finally {
                        Binder.restoreCallingIdentity(identityToken);
                    }
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
    }

    protected void monitorFactoryReset(String component, String reason) {
        if (this.mMonitor == null || TextUtils.isEmpty(reason)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("monitorFactoryReset: Invalid parameter,mMonitor=");
            stringBuilder.append(this.mMonitor);
            stringBuilder.append(", reason=");
            stringBuilder.append(reason);
            HwLog.e(str, stringBuilder.toString());
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("component", component);
        bundle.putString("reason", reason);
        this.mMonitor.monitor(907400018, bundle);
    }

    protected void clearWipeDataFactoryLowlevel(String reason, boolean wipeEuicc) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("wipeData, reason=");
        stringBuilder.append(reason);
        stringBuilder.append(", wipeEuicc=");
        stringBuilder.append(wipeEuicc);
        HwLog.d(str, stringBuilder.toString());
        Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
        intent.addFlags(285212672);
        intent.putExtra("masterClearWipeDataFactoryLowlevel", true);
        intent.putExtra("wipeEuicc", wipeEuicc);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
    }

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
                    System.putStringForUser(this.mContext.getContentResolver(), "system_locales", locale.toLanguageTag(), userHandle);
                    BackupManager.dataChanged("com.android.providers.settings");
                    Binder.restoreCallingIdentity(callingId);
                } catch (Exception e) {
                    try {
                        Slog.w(TAG, "failed to set system language");
                    } finally {
                        Binder.restoreCallingIdentity(callingId);
                    }
                }
            } else {
                throw new IllegalArgumentException("ComponentName is null");
            }
        }
        return 1;
    }

    public void setDeviceOwnerApp(ComponentName admin, String ownerName, int userId) {
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
        enforceHwCrossUserPermission(userId);
        synchronized (getLockObject()) {
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                this.mIsMDMDeviceOwnerAPI = true;
                super.setDeviceOwner(admin, ownerName, userId);
            } finally {
                this.mIsMDMDeviceOwnerAPI = false;
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
    }

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
                super.clearDeviceOwner(component.getPackageName());
                this.mIsMDMDeviceOwnerAPI = false;
            } catch (Throwable th) {
                this.mIsMDMDeviceOwnerAPI = false;
            }
        }
    }

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
                    return;
                } catch (Exception e) {
                    HwLog.e(TAG, "Can not calling the remote function to set data enabled!");
                    return;
                }
            }
            phone.disableDataConnectivity();
            return;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    public boolean setCarrierLockScreenPassword(ComponentName who, String password, String phoneNumber, int userHandle) {
        boolean extendLockScreenPassword;
        if (who != null) {
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_KEYGUARD", "does not have keyguard MDM permission!");
            synchronized (getLockObject()) {
                if (getActiveAdminUncheckedLocked(who, userHandle) != null) {
                    long token = Binder.clearCallingIdentity();
                    try {
                        extendLockScreenPassword = new LockPatternUtilsEx(this.mContext).setExtendLockScreenPassword(password, phoneNumber, userHandle);
                    } catch (Exception e) {
                        extendLockScreenPassword = false;
                        return extendLockScreenPassword;
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No active admin owned by uid ");
                    stringBuilder.append(Binder.getCallingUid());
                    stringBuilder.append(", ComponentName:");
                    stringBuilder.append(who);
                    throw new SecurityException(stringBuilder.toString());
                }
            }
            return extendLockScreenPassword;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }

    public boolean clearCarrierLockScreenPassword(ComponentName who, String password, int userHandle) {
        boolean clearExtendLockScreenPassword;
        if (who != null) {
            this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_KEYGUARD", "does not have keyguard MDM permission!");
            synchronized (getLockObject()) {
                if (getActiveAdminUncheckedLocked(who, userHandle) != null) {
                    long token = Binder.clearCallingIdentity();
                    try {
                        clearExtendLockScreenPassword = new LockPatternUtilsEx(this.mContext).clearExtendLockScreenPassword(password, userHandle);
                        Binder.restoreCallingIdentity(token);
                    } catch (Exception e) {
                        try {
                            clearExtendLockScreenPassword = TAG;
                            HwLog.e(clearExtendLockScreenPassword, "clear extended keyguard password error!");
                            clearExtendLockScreenPassword = false;
                        } finally {
                            Binder.restoreCallingIdentity(token);
                        }
                        return clearExtendLockScreenPassword;
                    }
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("No active admin owned by uid ");
                stringBuilder.append(Binder.getCallingUid());
                stringBuilder.append(", ComponentName:");
                stringBuilder.append(who);
                throw new SecurityException(stringBuilder.toString());
            }
            return clearExtendLockScreenPassword;
        }
        throw new IllegalArgumentException("ComponentName is null");
    }
}
