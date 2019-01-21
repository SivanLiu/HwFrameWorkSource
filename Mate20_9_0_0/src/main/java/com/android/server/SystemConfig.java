package com.android.server;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.pm.FeatureInfo;
import android.os.Build.VERSION;
import android.os.Environment;
import android.os.Process;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Protocol;
import com.android.internal.util.XmlUtils;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class SystemConfig {
    private static final int ALLOW_ALL = -1;
    private static final int ALLOW_APP_CONFIGS = 8;
    private static final int ALLOW_FEATURES = 1;
    private static final int ALLOW_HIDDENAPI_WHITELISTING = 64;
    private static final int ALLOW_LIBS = 2;
    private static final int ALLOW_OEM_PERMISSIONS = 32;
    private static final int ALLOW_PERMISSIONS = 4;
    private static final int ALLOW_PRIVAPP_PERMISSIONS = 16;
    private static final String CHARACTERISTICS = SystemProperties.get("ro.build.characteristics", "");
    private static final boolean IS_CHINA = "CN".equalsIgnoreCase(SystemProperties.get("ro.product.locale.region", ""));
    static final String TAG = "SystemConfig";
    static SystemConfig sInstance;
    final int CUST_TYPE_CONFIG = 0;
    final ArraySet<String> mAllowImplicitBroadcasts = new ArraySet();
    final ArraySet<String> mAllowInDataUsageSave = new ArraySet();
    final ArraySet<String> mAllowInPowerSave = new ArraySet();
    final ArraySet<String> mAllowInPowerSaveExceptIdle = new ArraySet();
    final ArraySet<String> mAllowUnthrottledLocation = new ArraySet();
    final ArrayMap<String, FeatureInfo> mAvailableFeatures = new ArrayMap();
    final ArraySet<ComponentName> mBackupTransportWhitelist = new ArraySet();
    final ArraySet<ComponentName> mDefaultVrComponents = new ArraySet();
    final ArraySet<String> mDisabledUntilUsedPreinstalledCarrierApps = new ArraySet();
    final ArrayMap<String, List<String>> mDisabledUntilUsedPreinstalledCarrierAssociatedApps = new ArrayMap();
    int[] mGlobalGids;
    final ArraySet<String> mHiddenApiPackageWhitelist = new ArraySet();
    final ArraySet<String> mLinkedApps = new ArraySet();
    final ArrayMap<String, ArrayMap<String, Boolean>> mOemPermissions = new ArrayMap();
    final ArrayMap<String, PermissionEntry> mPermissions = new ArrayMap();
    final ArrayMap<String, ArraySet<String>> mPrivAppDenyPermissions = new ArrayMap();
    final ArrayMap<String, ArraySet<String>> mPrivAppPermissions = new ArrayMap();
    final ArrayMap<String, ArraySet<String>> mProductPrivAppDenyPermissions = new ArrayMap();
    final ArrayMap<String, ArraySet<String>> mProductPrivAppPermissions = new ArrayMap();
    final ArrayMap<String, String> mSharedLibraries = new ArrayMap();
    final SparseArray<ArraySet<String>> mSystemPermissions = new SparseArray();
    final ArraySet<String> mSystemUserBlacklistedApps = new ArraySet();
    final ArraySet<String> mSystemUserWhitelistedApps = new ArraySet();
    final ArraySet<String> mUnavailableFeatures = new ArraySet();
    final ArrayMap<String, ArraySet<String>> mVendorPrivAppDenyPermissions = new ArrayMap();
    final ArrayMap<String, ArraySet<String>> mVendorPrivAppPermissions = new ArrayMap();

    public static final class PermissionEntry {
        public int[] gids;
        public final String name;
        public boolean perUser;

        PermissionEntry(String name, boolean perUser) {
            this.name = name;
            this.perUser = perUser;
        }
    }

    public static SystemConfig getInstance() {
        SystemConfig systemConfig;
        synchronized (SystemConfig.class) {
            if (sInstance == null) {
                sInstance = new SystemConfig();
            }
            systemConfig = sInstance;
        }
        return systemConfig;
    }

    public int[] getGlobalGids() {
        return this.mGlobalGids;
    }

    public SparseArray<ArraySet<String>> getSystemPermissions() {
        return this.mSystemPermissions;
    }

    public ArrayMap<String, String> getSharedLibraries() {
        return this.mSharedLibraries;
    }

    public ArrayMap<String, FeatureInfo> getAvailableFeatures() {
        return this.mAvailableFeatures;
    }

    public ArrayMap<String, PermissionEntry> getPermissions() {
        return this.mPermissions;
    }

    public ArraySet<String> getAllowImplicitBroadcasts() {
        return this.mAllowImplicitBroadcasts;
    }

    public ArraySet<String> getAllowInPowerSaveExceptIdle() {
        return this.mAllowInPowerSaveExceptIdle;
    }

    public ArraySet<String> getAllowInPowerSave() {
        return this.mAllowInPowerSave;
    }

    public ArraySet<String> getAllowInDataUsageSave() {
        return this.mAllowInDataUsageSave;
    }

    public ArraySet<String> getAllowUnthrottledLocation() {
        return this.mAllowUnthrottledLocation;
    }

    public ArraySet<String> getLinkedApps() {
        return this.mLinkedApps;
    }

    public ArraySet<String> getSystemUserWhitelistedApps() {
        return this.mSystemUserWhitelistedApps;
    }

    public ArraySet<String> getSystemUserBlacklistedApps() {
        return this.mSystemUserBlacklistedApps;
    }

    public ArraySet<String> getHiddenApiWhitelistedApps() {
        return this.mHiddenApiPackageWhitelist;
    }

    public ArraySet<ComponentName> getDefaultVrComponents() {
        return this.mDefaultVrComponents;
    }

    public ArraySet<ComponentName> getBackupTransportWhitelist() {
        return this.mBackupTransportWhitelist;
    }

    public ArraySet<String> getDisabledUntilUsedPreinstalledCarrierApps() {
        return this.mDisabledUntilUsedPreinstalledCarrierApps;
    }

    public ArrayMap<String, List<String>> getDisabledUntilUsedPreinstalledCarrierAssociatedApps() {
        return this.mDisabledUntilUsedPreinstalledCarrierAssociatedApps;
    }

    public ArraySet<String> getPrivAppPermissions(String packageName) {
        return (ArraySet) this.mPrivAppPermissions.get(packageName);
    }

    public ArraySet<String> getPrivAppDenyPermissions(String packageName) {
        return (ArraySet) this.mPrivAppDenyPermissions.get(packageName);
    }

    public ArraySet<String> getVendorPrivAppPermissions(String packageName) {
        return (ArraySet) this.mVendorPrivAppPermissions.get(packageName);
    }

    public ArraySet<String> getVendorPrivAppDenyPermissions(String packageName) {
        return (ArraySet) this.mVendorPrivAppDenyPermissions.get(packageName);
    }

    public ArraySet<String> getProductPrivAppPermissions(String packageName) {
        return (ArraySet) this.mProductPrivAppPermissions.get(packageName);
    }

    public ArraySet<String> getProductPrivAppDenyPermissions(String packageName) {
        return (ArraySet) this.mProductPrivAppDenyPermissions.get(packageName);
    }

    public Map<String, Boolean> getOemPermissions(String packageName) {
        Map<String, Boolean> oemPermissions = (Map) this.mOemPermissions.get(packageName);
        if (oemPermissions != null) {
            return oemPermissions;
        }
        return Collections.emptyMap();
    }

    SystemConfig() {
        readPermissions(Environment.buildPath(Environment.getRootDirectory(), new String[]{"etc", "sysconfig"}), -1);
        readPermissions(Environment.buildPath(Environment.getRootDirectory(), new String[]{"etc", "permissions"}), -1);
        int vendorPermissionFlag = 19;
        if (VERSION.FIRST_SDK_INT <= 27) {
            vendorPermissionFlag = 19 | 12;
        }
        readPermissions(Environment.buildPath(Environment.getVendorDirectory(), new String[]{"etc", "sysconfig"}), vendorPermissionFlag);
        readPermissions(Environment.buildPath(Environment.getVendorDirectory(), new String[]{"etc", "permissions"}), vendorPermissionFlag);
        int odmPermissionFlag = vendorPermissionFlag;
        readPermissions(Environment.buildPath(Environment.getOdmDirectory(), new String[]{"etc", "sysconfig"}), odmPermissionFlag);
        readPermissions(Environment.buildPath(Environment.getOdmDirectory(), new String[]{"etc", "permissions"}), odmPermissionFlag);
        readPermissions(Environment.buildPath(Environment.getOemDirectory(), new String[]{"etc", "sysconfig"}), 33);
        readPermissions(Environment.buildPath(Environment.getOemDirectory(), new String[]{"etc", "permissions"}), 33);
        readPermissions(Environment.buildPath(Environment.getProductDirectory(), new String[]{"etc", "sysconfig"}), 31);
        readPermissions(Environment.buildPath(Environment.getProductDirectory(), new String[]{"etc", "permissions"}), 31);
        readCustPermissions();
        if (SystemProperties.getBoolean("persist.graphics.vulkan.disable", false)) {
            removeFeature("android.hardware.vulkan.level");
            removeFeature("android.hardware.vulkan.version");
        }
        int value = SystemProperties.getInt("ro.opengles.version", 0);
        if (value > 0 && value == Protocol.BASE_DHCP && this.mAvailableFeatures.remove("android.hardware.opengles.aep") != null) {
            Slog.d(TAG, "Removed android.hardware.opengles.aep feature for opengles 3.0");
        }
    }

    void readCustPermissions() {
        int i = 0;
        String[] dirs = new String[0];
        String sysPath = getCanonicalPathOrNull(Environment.buildPath(Environment.getRootDirectory(), new String[]{"etc"}));
        try {
            dirs = HwCfgFilePolicy.getCfgPolicyDir(0);
        } catch (NoClassDefFoundError e) {
            Slog.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        }
        if (sysPath != null && dirs.length > 0) {
            int length = dirs.length;
            while (i < length) {
                File file = new File(dirs[i]);
                String dirPath = getCanonicalPathOrNull(file);
                if (!(dirPath == null || dirPath.equals(sysPath))) {
                    readPermissions(Environment.buildPath(file, new String[]{"sysconfig"}), -1);
                    readPermissions(Environment.buildPath(file, new String[]{"permissions"}), -1);
                }
                i++;
            }
        }
    }

    String getCanonicalPathOrNull(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            Slog.d(TAG, "Unable to resolve canonical");
            return null;
        }
    }

    void readPermissions(File libraryDir, int permissionFlag) {
        String str;
        StringBuilder stringBuilder;
        if (!libraryDir.exists() || !libraryDir.isDirectory()) {
            if (permissionFlag == -1) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("No directory ");
                stringBuilder.append(libraryDir);
                stringBuilder.append(", skipping");
                Slog.w(str, stringBuilder.toString());
            }
        } else if (libraryDir.canRead()) {
            File platformFile = null;
            for (File f : libraryDir.listFiles()) {
                String str2;
                StringBuilder stringBuilder2;
                if (f.getPath().endsWith("etc/permissions/platform.xml")) {
                    platformFile = f;
                } else if (!f.getPath().endsWith(".xml")) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Non-xml file ");
                    stringBuilder2.append(f);
                    stringBuilder2.append(" in ");
                    stringBuilder2.append(libraryDir);
                    stringBuilder2.append(" directory, ignoring");
                    Slog.i(str2, stringBuilder2.toString());
                } else if (f.canRead()) {
                    readPermissionsFromXml(f, permissionFlag);
                } else {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Permissions library file ");
                    stringBuilder2.append(f);
                    stringBuilder2.append(" cannot be read");
                    Slog.w(str2, stringBuilder2.toString());
                }
            }
            if (platformFile != null) {
                readPermissionsFromXml(platformFile, permissionFlag);
            }
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Directory ");
            stringBuilder.append(libraryDir);
            stringBuilder.append(" cannot be read");
            Slog.w(str, stringBuilder.toString());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:271:0x0731 A:{Catch:{ XmlPullParserException -> 0x07d9, IOException -> 0x07d7, all -> 0x07d2 }} */
    /* JADX WARNING: Removed duplicated region for block: B:270:0x0729 A:{Catch:{ XmlPullParserException -> 0x07d9, IOException -> 0x07d7, all -> 0x07d2 }} */
    /* JADX WARNING: Removed duplicated region for block: B:290:0x07c6 A:{Catch:{ XmlPullParserException -> 0x07d9, IOException -> 0x07d7, all -> 0x07d2 }} */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x0028 A:{Catch:{ XmlPullParserException -> 0x07dd, IOException -> 0x07e3, all -> 0x07e0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:316:0x0816  */
    /* JADX WARNING: Removed duplicated region for block: B:315:0x080a  */
    /* JADX WARNING: Removed duplicated region for block: B:319:0x081d  */
    /* JADX WARNING: Removed duplicated region for block: B:323:0x082e  */
    /* JADX WARNING: Removed duplicated region for block: B:322:0x0828  */
    /* JADX WARNING: Removed duplicated region for block: B:327:0x083f A:{LOOP_END, LOOP:2: B:325:0x0839->B:327:0x083f} */
    /* JADX WARNING: Removed duplicated region for block: B:315:0x080a  */
    /* JADX WARNING: Removed duplicated region for block: B:316:0x0816  */
    /* JADX WARNING: Removed duplicated region for block: B:319:0x081d  */
    /* JADX WARNING: Removed duplicated region for block: B:322:0x0828  */
    /* JADX WARNING: Removed duplicated region for block: B:323:0x082e  */
    /* JADX WARNING: Removed duplicated region for block: B:327:0x083f A:{LOOP_END, LOOP:2: B:325:0x0839->B:327:0x083f} */
    /* JADX WARNING: Removed duplicated region for block: B:316:0x0816  */
    /* JADX WARNING: Removed duplicated region for block: B:315:0x080a  */
    /* JADX WARNING: Removed duplicated region for block: B:319:0x081d  */
    /* JADX WARNING: Removed duplicated region for block: B:323:0x082e  */
    /* JADX WARNING: Removed duplicated region for block: B:322:0x0828  */
    /* JADX WARNING: Removed duplicated region for block: B:327:0x083f A:{LOOP_END, LOOP:2: B:325:0x0839->B:327:0x083f} */
    /* JADX WARNING: Removed duplicated region for block: B:315:0x080a  */
    /* JADX WARNING: Removed duplicated region for block: B:316:0x0816  */
    /* JADX WARNING: Removed duplicated region for block: B:319:0x081d  */
    /* JADX WARNING: Removed duplicated region for block: B:322:0x0828  */
    /* JADX WARNING: Removed duplicated region for block: B:323:0x082e  */
    /* JADX WARNING: Removed duplicated region for block: B:327:0x083f A:{LOOP_END, LOOP:2: B:325:0x0839->B:327:0x083f} */
    /* JADX WARNING: Removed duplicated region for block: B:316:0x0816  */
    /* JADX WARNING: Removed duplicated region for block: B:315:0x080a  */
    /* JADX WARNING: Removed duplicated region for block: B:319:0x081d  */
    /* JADX WARNING: Removed duplicated region for block: B:323:0x082e  */
    /* JADX WARNING: Removed duplicated region for block: B:322:0x0828  */
    /* JADX WARNING: Removed duplicated region for block: B:327:0x083f A:{LOOP_END, LOOP:2: B:325:0x0839->B:327:0x083f} */
    /* JADX WARNING: Removed duplicated region for block: B:315:0x080a  */
    /* JADX WARNING: Removed duplicated region for block: B:316:0x0816  */
    /* JADX WARNING: Removed duplicated region for block: B:319:0x081d  */
    /* JADX WARNING: Removed duplicated region for block: B:322:0x0828  */
    /* JADX WARNING: Removed duplicated region for block: B:323:0x082e  */
    /* JADX WARNING: Removed duplicated region for block: B:327:0x083f A:{LOOP_END, LOOP:2: B:325:0x0839->B:327:0x083f} */
    /* JADX WARNING: Removed duplicated region for block: B:301:0x07e3 A:{ExcHandler: IOException (e java.io.IOException), Splitter:B:4:0x0013, PHI: r4 } */
    /* JADX WARNING: Removed duplicated region for block: B:299:0x07e0 A:{ExcHandler: all (th java.lang.Throwable), Splitter:B:4:0x0013, PHI: r4 } */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:299:0x07e0, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:300:0x07e1, code skipped:
            r3 = r4;
     */
    /* JADX WARNING: Missing block: B:301:0x07e3, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:302:0x07e4, code skipped:
            r22 = r4;
     */
    /* JADX WARNING: Missing block: B:308:0x07f7, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:309:0x07f8, code skipped:
            r3 = r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readPermissionsFromXml(File permFile, int permissionFlag) {
        XmlPullParserException e;
        FileReader permReader;
        IOException e2;
        FileReader permReader2;
        Iterator it;
        Throwable th;
        File file = permFile;
        int i = permissionFlag;
        String str = null;
        FileReader permReader3 = null;
        String str2;
        try {
            permReader3 = new FileReader(file);
            boolean lowRam = ActivityManager.isLowRamDeviceStatic();
            try {
                int type;
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(permReader3);
                while (true) {
                    int next = parser.next();
                    type = next;
                    int i2 = 1;
                    if (next == 2 || type == 1) {
                        int type2;
                        if (type != 2) {
                            int i3;
                            if (!parser.getName().equals("permissions")) {
                                try {
                                    if (!parser.getName().equals("config")) {
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Unexpected start tag in ");
                                        stringBuilder.append(file);
                                        stringBuilder.append(": found ");
                                        stringBuilder.append(parser.getName());
                                        stringBuilder.append(", expected 'permissions' or 'config'");
                                        throw new XmlPullParserException(stringBuilder.toString());
                                    }
                                } catch (XmlPullParserException e3) {
                                    e = e3;
                                    permReader = permReader3;
                                } catch (IOException e4) {
                                    e2 = e4;
                                    permReader2 = permReader3;
                                    try {
                                        Slog.w(TAG, "Got exception parsing permissions.", e2);
                                        IoUtils.closeQuietly(permReader2);
                                        if (StorageManager.isFileEncryptedNativeOnly()) {
                                        }
                                        if (StorageManager.hasAdoptable()) {
                                        }
                                        if (ActivityManager.isLowRamDeviceStatic()) {
                                        }
                                        it = this.mUnavailableFeatures.iterator();
                                        while (it.hasNext()) {
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                        permReader = permReader2;
                                        IoUtils.closeQuietly(permReader);
                                        throw th;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    permReader = permReader3;
                                    IoUtils.closeQuietly(permReader);
                                    throw th;
                                }
                            }
                            boolean allowAll = i == -1;
                            boolean allowLibs = (i & 2) != 0;
                            boolean allowFeatures = (i & 1) != 0;
                            boolean allowPermissions = (i & 4) != 0;
                            boolean allowAppConfigs = (i & 8) != 0;
                            boolean allowPrivappPermissions = (i & 16) != 0;
                            boolean allowOemPermissions = (i & 32) != 0;
                            boolean allowApiWhitelisting = (i & 64) != 0;
                            while (true) {
                                XmlUtils.nextElement(parser);
                                if (parser.getEventType() == i2) {
                                    break;
                                }
                                boolean allowPermissions2;
                                String name = parser.getName();
                                String gidStr;
                                if ("group".equals(name) && allowAll) {
                                    gidStr = parser.getAttributeValue(str, "gid");
                                    if (gidStr != null) {
                                        this.mGlobalGids = ArrayUtils.appendInt(this.mGlobalGids, Process.getGidForName(gidStr));
                                        type2 = type;
                                    } else {
                                        str = TAG;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        type2 = type;
                                        stringBuilder2.append("<group> without gid in ");
                                        stringBuilder2.append(file);
                                        stringBuilder2.append(" at ");
                                        stringBuilder2.append(parser.getPositionDescription());
                                        Slog.w(str, stringBuilder2.toString());
                                    }
                                    XmlUtils.skipCurrentTag(parser);
                                } else {
                                    type2 = type;
                                    StringBuilder stringBuilder3;
                                    String str3;
                                    StringBuilder stringBuilder4;
                                    if ("permission".equals(name) && allowPermissions) {
                                        str = parser.getAttributeValue(null, "name");
                                        if (str == null) {
                                            str2 = TAG;
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("<permission> without name in ");
                                            stringBuilder3.append(file);
                                            stringBuilder3.append(" at ");
                                            stringBuilder3.append(parser.getPositionDescription());
                                            Slog.w(str2, stringBuilder3.toString());
                                            XmlUtils.skipCurrentTag(parser);
                                        } else {
                                            readPermission(parser, str.intern());
                                            permReader2 = permReader3;
                                            allowPermissions2 = allowPermissions;
                                        }
                                    } else if ("assign-permission".equals(name) && allowPermissions) {
                                        str = parser.getAttributeValue(null, "name");
                                        if (str == null) {
                                            str2 = TAG;
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("<assign-permission> without name in ");
                                            stringBuilder3.append(file);
                                            stringBuilder3.append(" at ");
                                            stringBuilder3.append(parser.getPositionDescription());
                                            Slog.w(str2, stringBuilder3.toString());
                                            XmlUtils.skipCurrentTag(parser);
                                        } else {
                                            str2 = parser.getAttributeValue(null, "uid");
                                            if (str2 == null) {
                                                str3 = TAG;
                                                StringBuilder stringBuilder5 = new StringBuilder();
                                                allowPermissions2 = allowPermissions;
                                                stringBuilder5.append("<assign-permission> without uid in ");
                                                stringBuilder5.append(file);
                                                stringBuilder5.append(" at ");
                                                stringBuilder5.append(parser.getPositionDescription());
                                                Slog.w(str3, stringBuilder5.toString());
                                                XmlUtils.skipCurrentTag(parser);
                                                permReader2 = permReader3;
                                            } else {
                                                allowPermissions2 = allowPermissions;
                                                type = Process.getUidForName(str2);
                                                if (type < 0) {
                                                    gidStr = TAG;
                                                    stringBuilder4 = new StringBuilder();
                                                    permReader2 = permReader3;
                                                    try {
                                                        stringBuilder4.append("<assign-permission> with unknown uid \"");
                                                        stringBuilder4.append(str2);
                                                        stringBuilder4.append("  in ");
                                                        stringBuilder4.append(file);
                                                        stringBuilder4.append(" at ");
                                                        stringBuilder4.append(parser.getPositionDescription());
                                                        Slog.w(gidStr, stringBuilder4.toString());
                                                        XmlUtils.skipCurrentTag(parser);
                                                    } catch (XmlPullParserException e5) {
                                                        e = e5;
                                                        permReader = permReader2;
                                                        try {
                                                            Slog.w(TAG, "Got exception parsing permissions.", e);
                                                            IoUtils.closeQuietly(permReader);
                                                            if (StorageManager.isFileEncryptedNativeOnly()) {
                                                            }
                                                            if (StorageManager.hasAdoptable()) {
                                                            }
                                                            if (ActivityManager.isLowRamDeviceStatic()) {
                                                            }
                                                            it = this.mUnavailableFeatures.iterator();
                                                            while (it.hasNext()) {
                                                            }
                                                        } catch (Throwable th4) {
                                                            th = th4;
                                                            IoUtils.closeQuietly(permReader);
                                                            throw th;
                                                        }
                                                    } catch (IOException e6) {
                                                        e2 = e6;
                                                        Slog.w(TAG, "Got exception parsing permissions.", e2);
                                                        IoUtils.closeQuietly(permReader2);
                                                        if (StorageManager.isFileEncryptedNativeOnly()) {
                                                        }
                                                        if (StorageManager.hasAdoptable()) {
                                                        }
                                                        if (ActivityManager.isLowRamDeviceStatic()) {
                                                        }
                                                        it = this.mUnavailableFeatures.iterator();
                                                        while (it.hasNext()) {
                                                        }
                                                    } catch (Throwable th5) {
                                                        th = th5;
                                                        permReader = permReader2;
                                                        IoUtils.closeQuietly(permReader);
                                                        throw th;
                                                    }
                                                }
                                                permReader2 = permReader3;
                                                FileReader perm = str.intern();
                                                permReader3 = (ArraySet) this.mSystemPermissions.get(type);
                                                if (permReader3 == null) {
                                                    permReader3 = new ArraySet();
                                                    this.mSystemPermissions.put(type, permReader3);
                                                }
                                                permReader3.add(perm);
                                                XmlUtils.skipCurrentTag(parser);
                                            }
                                            str2 = null;
                                            str = str2;
                                            type = type2;
                                            allowPermissions = allowPermissions2;
                                            permReader3 = permReader2;
                                            i = permissionFlag;
                                            i2 = 1;
                                        }
                                    } else {
                                        permReader2 = permReader3;
                                        allowPermissions2 = allowPermissions;
                                        String str4;
                                        StringBuilder stringBuilder6;
                                        if ("library".equals(name) && allowLibs) {
                                            str = parser.getAttributeValue(null, "name");
                                            str2 = parser.getAttributeValue(null, "file");
                                            if (str == null) {
                                                str4 = TAG;
                                                stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("<library> without name in ");
                                                stringBuilder3.append(file);
                                                stringBuilder3.append(" at ");
                                                stringBuilder3.append(parser.getPositionDescription());
                                                Slog.w(str4, stringBuilder3.toString());
                                            } else if (str2 == null) {
                                                str4 = TAG;
                                                stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("<library> without file in ");
                                                stringBuilder3.append(file);
                                                stringBuilder3.append(" at ");
                                                stringBuilder3.append(parser.getPositionDescription());
                                                Slog.w(str4, stringBuilder3.toString());
                                            } else {
                                                this.mSharedLibraries.put(str, str2);
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                            str2 = null;
                                            str = str2;
                                            type = type2;
                                            allowPermissions = allowPermissions2;
                                            permReader3 = permReader2;
                                            i = permissionFlag;
                                            i2 = 1;
                                        } else if ("feature".equals(name) && allowFeatures) {
                                            boolean allowed;
                                            str = parser.getAttributeValue(null, "name");
                                            i = XmlUtils.readIntAttribute(parser, "version", 0);
                                            if (lowRam) {
                                                allowed = "true".equals(parser.getAttributeValue(null, "notLowRam")) ^ 1;
                                            } else {
                                                allowed = true;
                                            }
                                            if (str == null) {
                                                str3 = TAG;
                                                stringBuilder4 = new StringBuilder();
                                                stringBuilder4.append("<feature> without name in ");
                                                stringBuilder4.append(file);
                                                stringBuilder4.append(" at ");
                                                stringBuilder4.append(parser.getPositionDescription());
                                                Slog.w(str3, stringBuilder4.toString());
                                            } else if (allowed) {
                                                if (IS_CHINA && "android.software.home_screen".equals(str) && (PhoneConstants.APN_TYPE_DEFAULT.equals(CHARACTERISTICS) || "tablet".equals(CHARACTERISTICS))) {
                                                    Slog.w(TAG, "<feature> android.software.home_screen is disabled in china area");
                                                } else {
                                                    addFeature(str, i);
                                                }
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                            str2 = null;
                                            str = str2;
                                            type = type2;
                                            allowPermissions = allowPermissions2;
                                            permReader3 = permReader2;
                                            i = permissionFlag;
                                            i2 = 1;
                                        } else if ("unavailable-feature".equals(name) && allowFeatures) {
                                            str = parser.getAttributeValue(null, "name");
                                            if (str == null) {
                                                str2 = TAG;
                                                stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("<unavailable-feature> without name in ");
                                                stringBuilder6.append(file);
                                                stringBuilder6.append(" at ");
                                                stringBuilder6.append(parser.getPositionDescription());
                                                Slog.w(str2, stringBuilder6.toString());
                                            } else {
                                                this.mUnavailableFeatures.add(str);
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                            str2 = null;
                                            str = str2;
                                            type = type2;
                                            allowPermissions = allowPermissions2;
                                            permReader3 = permReader2;
                                            i = permissionFlag;
                                            i2 = 1;
                                        } else if ("allow-in-power-save-except-idle".equals(name) && allowAll) {
                                            str = parser.getAttributeValue(null, "package");
                                            if (str == null) {
                                                str2 = TAG;
                                                stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("<allow-in-power-save-except-idle> without package in ");
                                                stringBuilder6.append(file);
                                                stringBuilder6.append(" at ");
                                                stringBuilder6.append(parser.getPositionDescription());
                                                Slog.w(str2, stringBuilder6.toString());
                                            } else {
                                                this.mAllowInPowerSaveExceptIdle.add(str);
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                            str2 = null;
                                            str = str2;
                                            type = type2;
                                            allowPermissions = allowPermissions2;
                                            permReader3 = permReader2;
                                            i = permissionFlag;
                                            i2 = 1;
                                        } else if ("allow-in-power-save".equals(name) && allowAll) {
                                            str = parser.getAttributeValue(null, "package");
                                            if (str == null) {
                                                str2 = TAG;
                                                stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("<allow-in-power-save> without package in ");
                                                stringBuilder6.append(file);
                                                stringBuilder6.append(" at ");
                                                stringBuilder6.append(parser.getPositionDescription());
                                                Slog.w(str2, stringBuilder6.toString());
                                            } else {
                                                this.mAllowInPowerSave.add(str);
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                            str2 = null;
                                            str = str2;
                                            type = type2;
                                            allowPermissions = allowPermissions2;
                                            permReader3 = permReader2;
                                            i = permissionFlag;
                                            i2 = 1;
                                        } else if ("allow-in-data-usage-save".equals(name) && allowAll) {
                                            str = parser.getAttributeValue(null, "package");
                                            if (str == null) {
                                                str2 = TAG;
                                                stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("<allow-in-data-usage-save> without package in ");
                                                stringBuilder6.append(file);
                                                stringBuilder6.append(" at ");
                                                stringBuilder6.append(parser.getPositionDescription());
                                                Slog.w(str2, stringBuilder6.toString());
                                            } else {
                                                this.mAllowInDataUsageSave.add(str);
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                            str2 = null;
                                            str = str2;
                                            type = type2;
                                            allowPermissions = allowPermissions2;
                                            permReader3 = permReader2;
                                            i = permissionFlag;
                                            i2 = 1;
                                        } else if ("allow-unthrottled-location".equals(name) && allowAll) {
                                            str = parser.getAttributeValue(null, "package");
                                            if (str == null) {
                                                str2 = TAG;
                                                stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("<allow-unthrottled-location> without package in ");
                                                stringBuilder6.append(file);
                                                stringBuilder6.append(" at ");
                                                stringBuilder6.append(parser.getPositionDescription());
                                                Slog.w(str2, stringBuilder6.toString());
                                            } else {
                                                this.mAllowUnthrottledLocation.add(str);
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                            str2 = null;
                                            str = str2;
                                            type = type2;
                                            allowPermissions = allowPermissions2;
                                            permReader3 = permReader2;
                                            i = permissionFlag;
                                            i2 = 1;
                                        } else if ("allow-implicit-broadcast".equals(name) && allowAll) {
                                            str = parser.getAttributeValue(null, "action");
                                            if (str == null) {
                                                str2 = TAG;
                                                stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("<allow-implicit-broadcast> without action in ");
                                                stringBuilder6.append(file);
                                                stringBuilder6.append(" at ");
                                                stringBuilder6.append(parser.getPositionDescription());
                                                Slog.w(str2, stringBuilder6.toString());
                                            } else {
                                                this.mAllowImplicitBroadcasts.add(str);
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                            str2 = null;
                                            str = str2;
                                            type = type2;
                                            allowPermissions = allowPermissions2;
                                            permReader3 = permReader2;
                                            i = permissionFlag;
                                            i2 = 1;
                                        } else if ("app-link".equals(name) && allowAppConfigs) {
                                            str = parser.getAttributeValue(null, "package");
                                            if (str == null) {
                                                str2 = TAG;
                                                stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("<app-link> without package in ");
                                                stringBuilder6.append(file);
                                                stringBuilder6.append(" at ");
                                                stringBuilder6.append(parser.getPositionDescription());
                                                Slog.w(str2, stringBuilder6.toString());
                                            } else {
                                                this.mLinkedApps.add(str);
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                        } else if ("system-user-whitelisted-app".equals(name) && allowAppConfigs) {
                                            str = parser.getAttributeValue(null, "package");
                                            if (str == null) {
                                                str2 = TAG;
                                                stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("<system-user-whitelisted-app> without package in ");
                                                stringBuilder6.append(file);
                                                stringBuilder6.append(" at ");
                                                stringBuilder6.append(parser.getPositionDescription());
                                                Slog.w(str2, stringBuilder6.toString());
                                            } else {
                                                this.mSystemUserWhitelistedApps.add(str);
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                        } else if ("system-user-blacklisted-app".equals(name) && allowAppConfigs) {
                                            str = parser.getAttributeValue(null, "package");
                                            if (str == null) {
                                                str2 = TAG;
                                                stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("<system-user-blacklisted-app without package in ");
                                                stringBuilder6.append(file);
                                                stringBuilder6.append(" at ");
                                                stringBuilder6.append(parser.getPositionDescription());
                                                Slog.w(str2, stringBuilder6.toString());
                                            } else {
                                                this.mSystemUserBlacklistedApps.add(str);
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                        } else if ("default-enabled-vr-app".equals(name) && allowAppConfigs) {
                                            str = parser.getAttributeValue(null, "package");
                                            str2 = parser.getAttributeValue(null, "class");
                                            if (str == null) {
                                                str4 = TAG;
                                                stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("<default-enabled-vr-app without package in ");
                                                stringBuilder3.append(file);
                                                stringBuilder3.append(" at ");
                                                stringBuilder3.append(parser.getPositionDescription());
                                                Slog.w(str4, stringBuilder3.toString());
                                            } else if (str2 == null) {
                                                str4 = TAG;
                                                stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("<default-enabled-vr-app without class in ");
                                                stringBuilder3.append(file);
                                                stringBuilder3.append(" at ");
                                                stringBuilder3.append(parser.getPositionDescription());
                                                Slog.w(str4, stringBuilder3.toString());
                                            } else {
                                                this.mDefaultVrComponents.add(new ComponentName(str, str2));
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                        } else if ("backup-transport-whitelisted-service".equals(name) && allowFeatures) {
                                            str = parser.getAttributeValue(null, "service");
                                            if (str == null) {
                                                str2 = TAG;
                                                stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("<backup-transport-whitelisted-service> without service in ");
                                                stringBuilder6.append(file);
                                                stringBuilder6.append(" at ");
                                                stringBuilder6.append(parser.getPositionDescription());
                                                Slog.w(str2, stringBuilder6.toString());
                                            } else {
                                                ComponentName cn = ComponentName.unflattenFromString(str);
                                                if (cn == null) {
                                                    str4 = TAG;
                                                    stringBuilder3 = new StringBuilder();
                                                    stringBuilder3.append("<backup-transport-whitelisted-service> with invalid service name ");
                                                    stringBuilder3.append(str);
                                                    stringBuilder3.append(" in ");
                                                    stringBuilder3.append(file);
                                                    stringBuilder3.append(" at ");
                                                    stringBuilder3.append(parser.getPositionDescription());
                                                    Slog.w(str4, stringBuilder3.toString());
                                                } else {
                                                    this.mBackupTransportWhitelist.add(cn);
                                                }
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                        } else if ("disabled-until-used-preinstalled-carrier-associated-app".equals(name) && allowAppConfigs) {
                                            str = parser.getAttributeValue(null, "package");
                                            str2 = parser.getAttributeValue(null, "carrierAppPackage");
                                            if (str != null) {
                                                if (str2 != null) {
                                                    List<String> associatedPkgs = (List) this.mDisabledUntilUsedPreinstalledCarrierAssociatedApps.get(str2);
                                                    if (associatedPkgs == null) {
                                                        associatedPkgs = new ArrayList();
                                                        this.mDisabledUntilUsedPreinstalledCarrierAssociatedApps.put(str2, associatedPkgs);
                                                    }
                                                    associatedPkgs.add(str);
                                                    XmlUtils.skipCurrentTag(parser);
                                                }
                                            }
                                            str4 = TAG;
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("<disabled-until-used-preinstalled-carrier-associated-app without package or carrierAppPackage in ");
                                            stringBuilder3.append(file);
                                            stringBuilder3.append(" at ");
                                            stringBuilder3.append(parser.getPositionDescription());
                                            Slog.w(str4, stringBuilder3.toString());
                                            XmlUtils.skipCurrentTag(parser);
                                        } else if ("disabled-until-used-preinstalled-carrier-app".equals(name) && allowAppConfigs) {
                                            str = parser.getAttributeValue(null, "package");
                                            if (str == null) {
                                                str2 = TAG;
                                                stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("<disabled-until-used-preinstalled-carrier-app> without package in ");
                                                stringBuilder6.append(file);
                                                stringBuilder6.append(" at ");
                                                stringBuilder6.append(parser.getPositionDescription());
                                                Slog.w(str2, stringBuilder6.toString());
                                            } else {
                                                this.mDisabledUntilUsedPreinstalledCarrierApps.add(str);
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                        } else if ("privapp-permissions".equals(name) && allowPrivappPermissions) {
                                            boolean vendor;
                                            boolean product;
                                            if (!permFile.toPath().startsWith(Environment.getVendorDirectory().toPath())) {
                                                if (!permFile.toPath().startsWith(Environment.getOdmDirectory().toPath())) {
                                                    vendor = false;
                                                    product = permFile.toPath().startsWith(Environment.getProductDirectory().toPath());
                                                    if (!vendor) {
                                                        readPrivAppPermissions(parser, this.mVendorPrivAppPermissions, this.mVendorPrivAppDenyPermissions);
                                                    } else if (product) {
                                                        readPrivAppPermissions(parser, this.mProductPrivAppPermissions, this.mProductPrivAppDenyPermissions);
                                                    } else {
                                                        readPrivAppPermissions(parser, this.mPrivAppPermissions, this.mPrivAppDenyPermissions);
                                                    }
                                                }
                                            }
                                            vendor = true;
                                            product = permFile.toPath().startsWith(Environment.getProductDirectory().toPath());
                                            if (!vendor) {
                                            }
                                        } else if ("oem-permissions".equals(name) && allowOemPermissions) {
                                            readOemPermissions(parser);
                                        } else if ("hidden-api-whitelisted-app".equals(name) && allowApiWhitelisting) {
                                            str2 = null;
                                            str = parser.getAttributeValue(null, "package");
                                            if (str == null) {
                                                str4 = TAG;
                                                stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("<hidden-api-whitelisted-app> without package in ");
                                                stringBuilder3.append(file);
                                                stringBuilder3.append(" at ");
                                                stringBuilder3.append(parser.getPositionDescription());
                                                Slog.w(str4, stringBuilder3.toString());
                                            } else {
                                                this.mHiddenApiPackageWhitelist.add(str);
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                            str = str2;
                                            type = type2;
                                            allowPermissions = allowPermissions2;
                                            permReader3 = permReader2;
                                            i = permissionFlag;
                                            i2 = 1;
                                        } else {
                                            str2 = null;
                                            str = TAG;
                                            permReader3 = new StringBuilder();
                                            permReader3.append("Tag ");
                                            permReader3.append(name);
                                            permReader3.append(" is unknown or not allowed in ");
                                            permReader3.append(permFile.getParent());
                                            Slog.w(str, permReader3.toString());
                                            XmlUtils.skipCurrentTag(parser);
                                            str = str2;
                                            type = type2;
                                            allowPermissions = allowPermissions2;
                                            permReader3 = permReader2;
                                            i = permissionFlag;
                                            i2 = 1;
                                        }
                                    }
                                    str2 = null;
                                    str = str2;
                                    type = type2;
                                    allowPermissions = allowPermissions2;
                                    permReader3 = permReader2;
                                    i = permissionFlag;
                                    i2 = 1;
                                }
                                permReader2 = permReader3;
                                allowPermissions2 = allowPermissions;
                                str2 = null;
                                str = str2;
                                type = type2;
                                allowPermissions = allowPermissions2;
                                permReader3 = permReader2;
                                i = permissionFlag;
                                i2 = 1;
                            }
                            IoUtils.closeQuietly(permReader3);
                            permReader = permReader3;
                            if (StorageManager.isFileEncryptedNativeOnly()) {
                                i3 = 0;
                                addFeature("android.software.file_based_encryption", 0);
                                addFeature("android.software.securely_removes_users", 0);
                            } else {
                                i3 = 0;
                            }
                            if (StorageManager.hasAdoptable()) {
                                addFeature("android.software.adoptable_storage", i3);
                            }
                            if (ActivityManager.isLowRamDeviceStatic()) {
                                addFeature("android.hardware.ram.low", i3);
                            } else {
                                addFeature("android.hardware.ram.normal", i3);
                            }
                            it = this.mUnavailableFeatures.iterator();
                            while (it.hasNext()) {
                                removeFeature((String) it.next());
                            }
                        }
                        type2 = type;
                        throw new XmlPullParserException("No start tag found");
                    }
                }
                if (type != 2) {
                }
            } catch (XmlPullParserException e7) {
                e = e7;
                permReader = permReader3;
                Slog.w(TAG, "Got exception parsing permissions.", e);
                IoUtils.closeQuietly(permReader);
                if (StorageManager.isFileEncryptedNativeOnly()) {
                }
                if (StorageManager.hasAdoptable()) {
                }
                if (ActivityManager.isLowRamDeviceStatic()) {
                }
                it = this.mUnavailableFeatures.iterator();
                while (it.hasNext()) {
                }
            } catch (IOException e8) {
            } catch (Throwable th6) {
            }
        } catch (FileNotFoundException e9) {
            str2 = TAG;
            StringBuilder stringBuilder7 = new StringBuilder();
            stringBuilder7.append("Couldn't find or open permissions file ");
            stringBuilder7.append(file);
            Slog.w(str2, stringBuilder7.toString());
        }
    }

    private void addFeature(String name, int version) {
        FeatureInfo fi = (FeatureInfo) this.mAvailableFeatures.get(name);
        if (fi == null) {
            fi = new FeatureInfo();
            fi.name = name;
            fi.version = version;
            this.mAvailableFeatures.put(name, fi);
            return;
        }
        fi.version = Math.max(fi.version, version);
    }

    private void removeFeature(String name) {
        if (this.mAvailableFeatures.remove(name) != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Removed unavailable feature ");
            stringBuilder.append(name);
            Slog.d(str, stringBuilder.toString());
        }
    }

    void readPermission(XmlPullParser parser, String name) throws IOException, XmlPullParserException {
        if (this.mPermissions.containsKey(name)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Duplicate permission definition for ");
            stringBuilder.append(name);
            throw new IllegalStateException(stringBuilder.toString());
        }
        PermissionEntry perm = new PermissionEntry(name, XmlUtils.readBooleanAttribute(parser, "perUser", false));
        this.mPermissions.put(name, perm);
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if ("group".equals(parser.getName())) {
                        String gidStr = parser.getAttributeValue(null, "gid");
                        if (gidStr != null) {
                            perm.gids = ArrayUtils.appendInt(perm.gids, Process.getGidForName(gidStr));
                        } else {
                            String str = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("<group> without gid at ");
                            stringBuilder2.append(parser.getPositionDescription());
                            Slog.w(str, stringBuilder2.toString());
                        }
                    }
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    private void readPrivAppPermissions(XmlPullParser parser, ArrayMap<String, ArraySet<String>> grantMap, ArrayMap<String, ArraySet<String>> denyMap) throws IOException, XmlPullParserException {
        String packageName = parser.getAttributeValue(null, "package");
        if (TextUtils.isEmpty(packageName)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("package is required for <privapp-permissions> in ");
            stringBuilder.append(parser.getPositionDescription());
            Slog.w(str, stringBuilder.toString());
            return;
        }
        ArraySet<String> permissions = (ArraySet) grantMap.get(packageName);
        if (permissions == null) {
            permissions = new ArraySet();
        }
        ArraySet<String> denyPermissions = (ArraySet) denyMap.get(packageName);
        int depth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, depth)) {
            String name = parser.getName();
            String permName;
            String str2;
            StringBuilder stringBuilder2;
            if ("permission".equals(name)) {
                permName = parser.getAttributeValue(null, "name");
                if (TextUtils.isEmpty(permName)) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("name is required for <permission> in ");
                    stringBuilder2.append(parser.getPositionDescription());
                    Slog.w(str2, stringBuilder2.toString());
                } else {
                    permissions.add(permName);
                }
            } else if ("deny-permission".equals(name)) {
                permName = parser.getAttributeValue(null, "name");
                if (TextUtils.isEmpty(permName)) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("name is required for <deny-permission> in ");
                    stringBuilder2.append(parser.getPositionDescription());
                    Slog.w(str2, stringBuilder2.toString());
                } else {
                    if (denyPermissions == null) {
                        denyPermissions = new ArraySet();
                    }
                    denyPermissions.add(permName);
                }
            }
        }
        grantMap.put(packageName, permissions);
        if (denyPermissions != null) {
            denyMap.put(packageName, denyPermissions);
        }
    }

    void readOemPermissions(XmlPullParser parser) throws IOException, XmlPullParserException {
        String packageName = parser.getAttributeValue(null, "package");
        if (TextUtils.isEmpty(packageName)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("package is required for <oem-permissions> in ");
            stringBuilder.append(parser.getPositionDescription());
            Slog.w(str, stringBuilder.toString());
            return;
        }
        ArrayMap<String, Boolean> permissions = (ArrayMap) this.mOemPermissions.get(packageName);
        if (permissions == null) {
            permissions = new ArrayMap();
        }
        int depth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, depth)) {
            String name = parser.getName();
            String permName;
            String str2;
            StringBuilder stringBuilder2;
            if ("permission".equals(name)) {
                permName = parser.getAttributeValue(null, "name");
                if (TextUtils.isEmpty(permName)) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("name is required for <permission> in ");
                    stringBuilder2.append(parser.getPositionDescription());
                    Slog.w(str2, stringBuilder2.toString());
                } else {
                    permissions.put(permName, Boolean.TRUE);
                }
            } else if ("deny-permission".equals(name)) {
                permName = parser.getAttributeValue(null, "name");
                if (TextUtils.isEmpty(permName)) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("name is required for <deny-permission> in ");
                    stringBuilder2.append(parser.getPositionDescription());
                    Slog.w(str2, stringBuilder2.toString());
                } else {
                    permissions.put(permName, Boolean.FALSE);
                }
            }
        }
        this.mOemPermissions.put(packageName, permissions);
    }
}
