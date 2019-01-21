package android.content.pm;

import android.annotation.SystemApi;
import android.content.Context;
import android.content.pm.ApplicationInfoProto.Detail;
import android.content.pm.ApplicationInfoProto.Version;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Printer;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.R;
import com.android.internal.util.ArrayUtils;
import com.android.server.SystemConfig;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

public class ApplicationInfo extends PackageItemInfo implements Parcelable {
    public static final int CATEGORY_AUDIO = 1;
    public static final int CATEGORY_GAME = 0;
    public static final int CATEGORY_IMAGE = 3;
    public static final int CATEGORY_MAPS = 6;
    public static final int CATEGORY_NEWS = 5;
    public static final int CATEGORY_PRODUCTIVITY = 7;
    public static final int CATEGORY_SOCIAL = 4;
    public static final int CATEGORY_UNDEFINED = -1;
    public static final int CATEGORY_VIDEO = 2;
    public static final Creator<ApplicationInfo> CREATOR = new Creator<ApplicationInfo>() {
        public ApplicationInfo createFromParcel(Parcel source) {
            return new ApplicationInfo(source, null);
        }

        public ApplicationInfo[] newArray(int size) {
            return new ApplicationInfo[size];
        }
    };
    public static final int FLAG_ALLOW_BACKUP = 32768;
    public static final int FLAG_ALLOW_CLEAR_USER_DATA = 64;
    public static final int FLAG_ALLOW_TASK_REPARENTING = 32;
    public static final int FLAG_DEBUGGABLE = 2;
    public static final int FLAG_EXTERNAL_STORAGE = 262144;
    public static final int FLAG_EXTRACT_NATIVE_LIBS = 268435456;
    public static final int FLAG_FACTORY_TEST = 16;
    public static final int FLAG_FULL_BACKUP_ONLY = 67108864;
    public static final int FLAG_HARDWARE_ACCELERATED = 536870912;
    public static final int FLAG_HAS_CODE = 4;
    public static final int FLAG_INSTALLED = 8388608;
    public static final int FLAG_IS_DATA_ONLY = 16777216;
    @Deprecated
    public static final int FLAG_IS_GAME = 33554432;
    public static final int FLAG_KILL_AFTER_RESTORE = 65536;
    public static final int FLAG_LARGE_HEAP = 1048576;
    public static final int FLAG_MULTIARCH = Integer.MIN_VALUE;
    public static final int FLAG_PERSISTENT = 8;
    public static final int FLAG_RESIZEABLE_FOR_SCREENS = 4096;
    public static final int FLAG_RESTORE_ANY_VERSION = 131072;
    public static final int FLAG_STOPPED = 2097152;
    public static final int FLAG_SUPPORTS_LARGE_SCREENS = 2048;
    public static final int FLAG_SUPPORTS_NORMAL_SCREENS = 1024;
    public static final int FLAG_SUPPORTS_RTL = 4194304;
    public static final int FLAG_SUPPORTS_SCREEN_DENSITIES = 8192;
    public static final int FLAG_SUPPORTS_SMALL_SCREENS = 512;
    public static final int FLAG_SUPPORTS_XLARGE_SCREENS = 524288;
    public static final int FLAG_SUSPENDED = 1073741824;
    public static final int FLAG_SYSTEM = 1;
    public static final int FLAG_TEST_ONLY = 256;
    public static final int FLAG_UPDATED_SYSTEM_APP = 128;
    public static final int FLAG_USES_CLEARTEXT_TRAFFIC = 134217728;
    public static final int FLAG_VM_SAFE_MODE = 16384;
    public static final int HIDDEN_API_ENFORCEMENT_BLACK = 3;
    public static final int HIDDEN_API_ENFORCEMENT_DARK_GREY_AND_BLACK = 2;
    public static final int HIDDEN_API_ENFORCEMENT_DEFAULT = -1;
    public static final int HIDDEN_API_ENFORCEMENT_JUST_WARN = 1;
    private static final int HIDDEN_API_ENFORCEMENT_MAX = 3;
    public static final int HIDDEN_API_ENFORCEMENT_NONE = 0;
    public static final String METADATA_PRELOADED_FONTS = "preloaded_fonts";
    public static final int PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE = 1024;
    public static final int PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE_VIA_SDK_VERSION = 4096;
    public static final int PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_UNRESIZEABLE = 2048;
    public static final int PRIVATE_FLAG_BACKUP_IN_FOREGROUND = 8192;
    public static final int PRIVATE_FLAG_CANT_SAVE_STATE = 2;
    public static final int PRIVATE_FLAG_DEFAULT_TO_DEVICE_PROTECTED_STORAGE = 32;
    public static final int PRIVATE_FLAG_DIRECT_BOOT_AWARE = 64;
    public static final int PRIVATE_FLAG_FORWARD_LOCK = 4;
    public static final int PRIVATE_FLAG_HAS_DOMAIN_URLS = 16;
    public static final int PRIVATE_FLAG_HIDDEN = 1;
    public static final int PRIVATE_FLAG_INSTANT = 128;
    public static final int PRIVATE_FLAG_ISOLATED_SPLIT_LOADING = 32768;
    public static final int PRIVATE_FLAG_OEM = 131072;
    public static final int PRIVATE_FLAG_PARTIALLY_DIRECT_BOOT_AWARE = 256;
    public static final int PRIVATE_FLAG_PRIVILEGED = 8;
    public static final int PRIVATE_FLAG_PRODUCT = 524288;
    public static final int PRIVATE_FLAG_REQUIRED_FOR_SYSTEM_USER = 512;
    public static final int PRIVATE_FLAG_SIGNED_WITH_PLATFORM_KEY = 1048576;
    public static final int PRIVATE_FLAG_STATIC_SHARED_LIBRARY = 16384;
    public static final int PRIVATE_FLAG_VENDOR = 262144;
    public static final int PRIVATE_FLAG_VIRTUAL_PRELOAD = 65536;
    public String appComponentFactory;
    public String backupAgentName;
    public int category;
    public String classLoaderName;
    public String className;
    public int compatibleWidthLimitDp;
    public int compileSdkVersion;
    public String compileSdkVersionCodename;
    @SystemApi
    public String credentialProtectedDataDir;
    public String dataDir;
    public int descriptionRes;
    public String deviceProtectedDataDir;
    public boolean enabled;
    public int enabledSetting;
    public int flags;
    public int fullBackupContent;
    public int gestnav_extra_flags;
    public int hasDefaultNoFullScreen;
    public int hwFlags;
    public int hwHbsUid;
    public int hwThemeType;
    public int installLocation;
    public int largestWidthLimitDp;
    public long longVersionCode;
    private int mHiddenApiPolicy;
    public String manageSpaceActivityName;
    public float maxAspectRatio;
    public int minEmuiSdkVersion;
    public int minEmuiSysImgVersion;
    public int minSdkVersion;
    public String nativeLibraryDir;
    public String nativeLibraryRootDir;
    public boolean nativeLibraryRootRequiresIsa;
    public int networkSecurityConfigRes;
    public String permission;
    public String primaryCpuAbi;
    public int privateFlags;
    public String processName;
    public String publicSourceDir;
    public int requiresSmallestWidthDp;
    public String[] resourceDirs;
    public String scanPublicSourceDir;
    public String scanSourceDir;
    public String seInfo;
    public String seInfoUser;
    public String secondaryCpuAbi;
    public String secondaryNativeLibraryDir;
    public String[] sharedLibraryFiles;
    public String sourceDir;
    public String[] splitClassLoaderNames;
    public SparseArray<int[]> splitDependencies;
    public String[] splitNames;
    public String[] splitPublicSourceDirs;
    public String[] splitSourceDirs;
    public UUID storageUuid;
    public int targetEmuiSdkVersion;
    @SystemApi
    public int targetSandboxVersion;
    public int targetSdkVersion;
    public String taskAffinity;
    public int theme;
    public int uiOptions;
    public int uid;
    @Deprecated
    public int versionCode;
    @Deprecated
    public String volumeUuid;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ApplicationInfoPrivateFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Category {
    }

    public static class DisplayNameComparator implements Comparator<ApplicationInfo> {
        private PackageManager mPM;
        private final Collator sCollator = Collator.getInstance();

        public DisplayNameComparator(PackageManager pm) {
            this.mPM = pm;
        }

        public final int compare(ApplicationInfo aa, ApplicationInfo ab) {
            CharSequence sa = this.mPM.getApplicationLabel(aa);
            if (sa == null) {
                sa = aa.packageName;
            }
            CharSequence sb = this.mPM.getApplicationLabel(ab);
            if (sb == null) {
                sb = ab.packageName;
            }
            return this.sCollator.compare(sa.toString(), sb.toString());
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface HiddenApiEnforcementPolicy {
    }

    /* synthetic */ ApplicationInfo(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public static CharSequence getCategoryTitle(Context context, int category) {
        switch (category) {
            case 0:
                return context.getText(R.string.app_category_game);
            case 1:
                return context.getText(R.string.app_category_audio);
            case 2:
                return context.getText(R.string.app_category_video);
            case 3:
                return context.getText(R.string.app_category_image);
            case 4:
                return context.getText(R.string.app_category_social);
            case 5:
                return context.getText(R.string.app_category_news);
            case 6:
                return context.getText(R.string.app_category_maps);
            case 7:
                return context.getText(R.string.app_category_productivity);
            default:
                return null;
        }
    }

    public static boolean isValidHiddenApiEnforcementPolicy(int policy) {
        return policy >= -1 && policy <= 3;
    }

    public void dump(Printer pw, String prefix) {
        dump(pw, prefix, 3);
    }

    public void dump(Printer pw, String prefix, int dumpFlags) {
        StringBuilder stringBuilder;
        super.dumpFront(pw, prefix);
        if (!((dumpFlags & 1) == 0 || this.className == null)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("className=");
            stringBuilder.append(this.className);
            pw.println(stringBuilder.toString());
        }
        if (this.permission != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("permission=");
            stringBuilder.append(this.permission);
            pw.println(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("processName=");
        stringBuilder.append(this.processName);
        pw.println(stringBuilder.toString());
        if ((dumpFlags & 1) != 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("taskAffinity=");
            stringBuilder.append(this.taskAffinity);
            pw.println(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("uid=");
        stringBuilder.append(this.uid);
        stringBuilder.append(" flags=0x");
        stringBuilder.append(Integer.toHexString(this.flags));
        stringBuilder.append(" privateFlags=0x");
        stringBuilder.append(Integer.toHexString(this.privateFlags));
        stringBuilder.append(" theme=0x");
        stringBuilder.append(Integer.toHexString(this.theme));
        pw.println(stringBuilder.toString());
        if ((dumpFlags & 1) != 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("requiresSmallestWidthDp=");
            stringBuilder.append(this.requiresSmallestWidthDp);
            stringBuilder.append(" compatibleWidthLimitDp=");
            stringBuilder.append(this.compatibleWidthLimitDp);
            stringBuilder.append(" largestWidthLimitDp=");
            stringBuilder.append(this.largestWidthLimitDp);
            pw.println(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("sourceDir=");
        stringBuilder.append(this.sourceDir);
        pw.println(stringBuilder.toString());
        if (!Objects.equals(this.sourceDir, this.publicSourceDir)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("publicSourceDir=");
            stringBuilder.append(this.publicSourceDir);
            pw.println(stringBuilder.toString());
        }
        if (!ArrayUtils.isEmpty(this.splitSourceDirs)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("splitSourceDirs=");
            stringBuilder.append(Arrays.toString(this.splitSourceDirs));
            pw.println(stringBuilder.toString());
        }
        if (!(ArrayUtils.isEmpty(this.splitPublicSourceDirs) || Arrays.equals(this.splitSourceDirs, this.splitPublicSourceDirs))) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("splitPublicSourceDirs=");
            stringBuilder.append(Arrays.toString(this.splitPublicSourceDirs));
            pw.println(stringBuilder.toString());
        }
        if (this.resourceDirs != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("resourceDirs=");
            stringBuilder.append(Arrays.toString(this.resourceDirs));
            pw.println(stringBuilder.toString());
        }
        if (!((dumpFlags & 1) == 0 || this.seInfo == null)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("seinfo=");
            stringBuilder.append(this.seInfo);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("seinfoUser=");
            stringBuilder.append(this.seInfoUser);
            pw.println(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("dataDir=");
        stringBuilder.append(this.dataDir);
        pw.println(stringBuilder.toString());
        if ((dumpFlags & 1) != 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("deviceProtectedDataDir=");
            stringBuilder.append(this.deviceProtectedDataDir);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("credentialProtectedDataDir=");
            stringBuilder.append(this.credentialProtectedDataDir);
            pw.println(stringBuilder.toString());
            if (this.sharedLibraryFiles != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("sharedLibraryFiles=");
                stringBuilder.append(Arrays.toString(this.sharedLibraryFiles));
                pw.println(stringBuilder.toString());
            }
        }
        if (this.classLoaderName != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("classLoaderName=");
            stringBuilder.append(this.classLoaderName);
            pw.println(stringBuilder.toString());
        }
        if (!ArrayUtils.isEmpty(this.splitClassLoaderNames)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("splitClassLoaderNames=");
            stringBuilder.append(Arrays.toString(this.splitClassLoaderNames));
            pw.println(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("enabled=");
        stringBuilder.append(this.enabled);
        stringBuilder.append(" minSdkVersion=");
        stringBuilder.append(this.minSdkVersion);
        stringBuilder.append(" targetSdkVersion=");
        stringBuilder.append(this.targetSdkVersion);
        stringBuilder.append(" versionCode=");
        stringBuilder.append(this.longVersionCode);
        stringBuilder.append(" targetSandboxVersion=");
        stringBuilder.append(this.targetSandboxVersion);
        pw.println(stringBuilder.toString());
        if ((dumpFlags & 1) != 0) {
            if (this.manageSpaceActivityName != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("manageSpaceActivityName=");
                stringBuilder.append(this.manageSpaceActivityName);
                pw.println(stringBuilder.toString());
            }
            if (this.descriptionRes != 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("description=0x");
                stringBuilder.append(Integer.toHexString(this.descriptionRes));
                pw.println(stringBuilder.toString());
            }
            if (this.uiOptions != 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("uiOptions=0x");
                stringBuilder.append(Integer.toHexString(this.uiOptions));
                pw.println(stringBuilder.toString());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("supportsRtl=");
            stringBuilder.append(hasRtlSupport() ? "true" : "false");
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append(" hwFlags=0x");
            stringBuilder.append(Integer.toHexString(this.hwFlags));
            pw.println(stringBuilder.toString());
            if (this.fullBackupContent > 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("fullBackupContent=@xml/");
                stringBuilder.append(this.fullBackupContent);
                pw.println(stringBuilder.toString());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("fullBackupContent=");
                stringBuilder.append(this.fullBackupContent < 0 ? "false" : "true");
                pw.println(stringBuilder.toString());
            }
            if (this.networkSecurityConfigRes != 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("networkSecurityConfigRes=0x");
                stringBuilder.append(Integer.toHexString(this.networkSecurityConfigRes));
                pw.println(stringBuilder.toString());
            }
            if (this.category != -1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("category=");
                stringBuilder.append(this.category);
                pw.println(stringBuilder.toString());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("HiddenApiEnforcementPolicy=");
            stringBuilder.append(getHiddenApiEnforcementPolicy());
            pw.println(stringBuilder.toString());
            if (this.maxAspectRatio != 0.0f) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("maxAspectRatio=");
                stringBuilder.append(this.maxAspectRatio);
                pw.println(stringBuilder.toString());
            }
            if (this.hasDefaultNoFullScreen != 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("hasDefaultNoFullScreen=");
                stringBuilder.append(this.hasDefaultNoFullScreen);
                pw.println(stringBuilder.toString());
            }
        }
        super.dumpBack(pw, prefix);
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId, int dumpFlags) {
        ProtoOutputStream protoOutputStream = proto;
        long token = proto.start(fieldId);
        super.writeToProto(protoOutputStream, 1146756268033L);
        protoOutputStream.write(1138166333442L, this.permission);
        protoOutputStream.write(1138166333443L, this.processName);
        protoOutputStream.write(1120986464260L, this.uid);
        protoOutputStream.write(1120986464261L, this.flags);
        protoOutputStream.write(1120986464262L, this.privateFlags);
        protoOutputStream.write(1120986464263L, this.theme);
        protoOutputStream.write(1138166333448L, this.sourceDir);
        if (!Objects.equals(this.sourceDir, this.publicSourceDir)) {
            protoOutputStream.write(1138166333449L, this.publicSourceDir);
        }
        boolean z = false;
        if (!ArrayUtils.isEmpty(this.splitSourceDirs)) {
            for (String dir : this.splitSourceDirs) {
                protoOutputStream.write(2237677961226L, dir);
            }
        }
        if (!(ArrayUtils.isEmpty(this.splitPublicSourceDirs) || Arrays.equals(this.splitSourceDirs, this.splitPublicSourceDirs))) {
            for (String dir2 : this.splitPublicSourceDirs) {
                protoOutputStream.write(2237677961227L, dir2);
            }
        }
        if (this.resourceDirs != null) {
            for (String dir3 : this.resourceDirs) {
                protoOutputStream.write(2237677961228L, dir3);
            }
        }
        protoOutputStream.write(ApplicationInfoProto.DATA_DIR, this.dataDir);
        protoOutputStream.write(ApplicationInfoProto.CLASS_LOADER_NAME, this.classLoaderName);
        if (!ArrayUtils.isEmpty(this.splitClassLoaderNames)) {
            for (String dir32 : this.splitClassLoaderNames) {
                protoOutputStream.write(ApplicationInfoProto.SPLIT_CLASS_LOADER_NAMES, dir32);
            }
        }
        long versionToken = protoOutputStream.start(1146756268048L);
        protoOutputStream.write(Version.ENABLED, this.enabled);
        protoOutputStream.write(1120986464258L, this.minSdkVersion);
        protoOutputStream.write(1120986464259L, this.targetSdkVersion);
        protoOutputStream.write(1120986464260L, this.longVersionCode);
        protoOutputStream.write(1120986464261L, this.targetSandboxVersion);
        protoOutputStream.end(versionToken);
        if ((dumpFlags & 1) != 0) {
            long detailToken = protoOutputStream.start(ApplicationInfoProto.DETAIL);
            if (this.className != null) {
                protoOutputStream.write(1138166333441L, this.className);
            }
            protoOutputStream.write(1138166333442L, this.taskAffinity);
            protoOutputStream.write(1120986464259L, this.requiresSmallestWidthDp);
            protoOutputStream.write(1120986464260L, this.compatibleWidthLimitDp);
            protoOutputStream.write(1120986464261L, this.largestWidthLimitDp);
            if (this.seInfo != null) {
                protoOutputStream.write(1138166333446L, this.seInfo);
                protoOutputStream.write(1138166333447L, this.seInfoUser);
            }
            protoOutputStream.write(1138166333448L, this.deviceProtectedDataDir);
            protoOutputStream.write(1138166333449L, this.credentialProtectedDataDir);
            if (this.sharedLibraryFiles != null) {
                for (String f : this.sharedLibraryFiles) {
                    protoOutputStream.write(2237677961226L, f);
                }
            }
            if (this.manageSpaceActivityName != null) {
                protoOutputStream.write(Detail.MANAGE_SPACE_ACTIVITY_NAME, this.manageSpaceActivityName);
            }
            if (this.descriptionRes != 0) {
                protoOutputStream.write(Detail.DESCRIPTION_RES, this.descriptionRes);
            }
            if (this.uiOptions != 0) {
                protoOutputStream.write(Detail.UI_OPTIONS, this.uiOptions);
            }
            protoOutputStream.write(1133871366158L, hasRtlSupport());
            if (this.fullBackupContent > 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("@xml/");
                stringBuilder.append(this.fullBackupContent);
                protoOutputStream.write(1138166333455L, stringBuilder.toString());
            } else {
                if (this.fullBackupContent == 0) {
                    z = true;
                }
                protoOutputStream.write(Detail.IS_FULL_BACKUP, z);
            }
            if (this.networkSecurityConfigRes != 0) {
                protoOutputStream.write(Detail.NETWORK_SECURITY_CONFIG_RES, this.networkSecurityConfigRes);
            }
            if (this.category != -1) {
                protoOutputStream.write(Detail.CATEGORY, this.category);
            }
            protoOutputStream.end(detailToken);
        }
        protoOutputStream.end(token);
    }

    public boolean hasRtlSupport() {
        return (this.flags & 4194304) == 4194304;
    }

    public boolean hasCode() {
        return (this.flags & 4) != 0;
    }

    public ApplicationInfo() {
        this.fullBackupContent = 0;
        this.uiOptions = 0;
        this.flags = 0;
        this.hwFlags = 0;
        this.hwHbsUid = -1;
        this.requiresSmallestWidthDp = 0;
        this.compatibleWidthLimitDp = 0;
        this.largestWidthLimitDp = 0;
        this.enabled = true;
        this.enabledSetting = 0;
        this.installLocation = -1;
        this.category = -1;
        this.mHiddenApiPolicy = -1;
    }

    public ApplicationInfo(ApplicationInfo orig) {
        super((PackageItemInfo) orig);
        this.fullBackupContent = 0;
        this.uiOptions = 0;
        this.flags = 0;
        this.hwFlags = 0;
        this.hwHbsUid = -1;
        this.requiresSmallestWidthDp = 0;
        this.compatibleWidthLimitDp = 0;
        this.largestWidthLimitDp = 0;
        this.enabled = true;
        this.enabledSetting = 0;
        this.installLocation = -1;
        this.category = -1;
        this.mHiddenApiPolicy = -1;
        this.taskAffinity = orig.taskAffinity;
        this.permission = orig.permission;
        this.processName = orig.processName;
        this.className = orig.className;
        this.theme = orig.theme;
        this.flags = orig.flags;
        this.privateFlags = orig.privateFlags;
        this.hwFlags = orig.hwFlags;
        this.requiresSmallestWidthDp = orig.requiresSmallestWidthDp;
        this.compatibleWidthLimitDp = orig.compatibleWidthLimitDp;
        this.largestWidthLimitDp = orig.largestWidthLimitDp;
        this.volumeUuid = orig.volumeUuid;
        this.storageUuid = orig.storageUuid;
        this.scanSourceDir = orig.scanSourceDir;
        this.scanPublicSourceDir = orig.scanPublicSourceDir;
        this.sourceDir = orig.sourceDir;
        this.publicSourceDir = orig.publicSourceDir;
        this.splitNames = orig.splitNames;
        this.splitSourceDirs = orig.splitSourceDirs;
        this.splitPublicSourceDirs = orig.splitPublicSourceDirs;
        this.splitDependencies = orig.splitDependencies;
        this.nativeLibraryDir = orig.nativeLibraryDir;
        this.secondaryNativeLibraryDir = orig.secondaryNativeLibraryDir;
        this.nativeLibraryRootDir = orig.nativeLibraryRootDir;
        this.nativeLibraryRootRequiresIsa = orig.nativeLibraryRootRequiresIsa;
        this.primaryCpuAbi = orig.primaryCpuAbi;
        this.secondaryCpuAbi = orig.secondaryCpuAbi;
        this.resourceDirs = orig.resourceDirs;
        this.seInfo = orig.seInfo;
        this.seInfoUser = orig.seInfoUser;
        this.sharedLibraryFiles = orig.sharedLibraryFiles;
        this.dataDir = orig.dataDir;
        this.deviceProtectedDataDir = orig.deviceProtectedDataDir;
        this.credentialProtectedDataDir = orig.credentialProtectedDataDir;
        this.uid = orig.uid;
        this.minSdkVersion = orig.minSdkVersion;
        this.targetSdkVersion = orig.targetSdkVersion;
        setVersionCode(orig.longVersionCode);
        this.enabled = orig.enabled;
        this.enabledSetting = orig.enabledSetting;
        this.installLocation = orig.installLocation;
        this.manageSpaceActivityName = orig.manageSpaceActivityName;
        this.descriptionRes = orig.descriptionRes;
        this.uiOptions = orig.uiOptions;
        this.backupAgentName = orig.backupAgentName;
        this.fullBackupContent = orig.fullBackupContent;
        this.networkSecurityConfigRes = orig.networkSecurityConfigRes;
        this.category = orig.category;
        this.targetSandboxVersion = orig.targetSandboxVersion;
        this.classLoaderName = orig.classLoaderName;
        this.splitClassLoaderNames = orig.splitClassLoaderNames;
        this.appComponentFactory = orig.appComponentFactory;
        this.compileSdkVersion = orig.compileSdkVersion;
        this.compileSdkVersionCodename = orig.compileSdkVersionCodename;
        this.mHiddenApiPolicy = orig.mHiddenApiPolicy;
        this.maxAspectRatio = orig.maxAspectRatio;
        this.hasDefaultNoFullScreen = orig.hasDefaultNoFullScreen;
        this.minEmuiSdkVersion = orig.minEmuiSdkVersion;
        this.targetEmuiSdkVersion = orig.targetEmuiSdkVersion;
        this.hwThemeType = orig.hwThemeType;
        this.minEmuiSysImgVersion = orig.minEmuiSysImgVersion;
        this.gestnav_extra_flags = orig.gestnav_extra_flags;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ApplicationInfo{");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append(" ");
        stringBuilder.append(this.packageName);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int parcelableFlags) {
        super.writeToParcel(dest, parcelableFlags);
        dest.writeString(this.taskAffinity);
        dest.writeString(this.permission);
        dest.writeString(this.processName);
        dest.writeString(this.className);
        dest.writeInt(this.theme);
        dest.writeInt(this.flags);
        dest.writeInt(this.privateFlags);
        dest.writeInt(this.hwFlags);
        dest.writeInt(this.requiresSmallestWidthDp);
        dest.writeInt(this.compatibleWidthLimitDp);
        dest.writeInt(this.largestWidthLimitDp);
        if (this.storageUuid != null) {
            dest.writeInt(1);
            dest.writeLong(this.storageUuid.getMostSignificantBits());
            dest.writeLong(this.storageUuid.getLeastSignificantBits());
        } else {
            dest.writeInt(0);
        }
        dest.writeString(this.scanSourceDir);
        dest.writeString(this.scanPublicSourceDir);
        dest.writeString(this.sourceDir);
        dest.writeString(this.publicSourceDir);
        dest.writeStringArray(this.splitNames);
        dest.writeStringArray(this.splitSourceDirs);
        dest.writeStringArray(this.splitPublicSourceDirs);
        dest.writeSparseArray(this.splitDependencies);
        dest.writeString(this.nativeLibraryDir);
        dest.writeString(this.secondaryNativeLibraryDir);
        dest.writeString(this.nativeLibraryRootDir);
        dest.writeInt(this.nativeLibraryRootRequiresIsa);
        dest.writeString(this.primaryCpuAbi);
        dest.writeString(this.secondaryCpuAbi);
        dest.writeStringArray(this.resourceDirs);
        dest.writeString(this.seInfo);
        dest.writeString(this.seInfoUser);
        dest.writeStringArray(this.sharedLibraryFiles);
        dest.writeString(this.dataDir);
        dest.writeString(this.deviceProtectedDataDir);
        dest.writeString(this.credentialProtectedDataDir);
        dest.writeInt(this.uid);
        dest.writeInt(this.minSdkVersion);
        dest.writeInt(this.targetSdkVersion);
        dest.writeLong(this.longVersionCode);
        dest.writeInt(this.enabled);
        dest.writeInt(this.enabledSetting);
        dest.writeInt(this.installLocation);
        dest.writeString(this.manageSpaceActivityName);
        dest.writeString(this.backupAgentName);
        dest.writeInt(this.descriptionRes);
        dest.writeInt(this.uiOptions);
        dest.writeInt(this.fullBackupContent);
        dest.writeInt(this.networkSecurityConfigRes);
        dest.writeInt(this.category);
        dest.writeInt(this.targetSandboxVersion);
        dest.writeString(this.classLoaderName);
        dest.writeStringArray(this.splitClassLoaderNames);
        dest.writeInt(this.compileSdkVersion);
        dest.writeString(this.compileSdkVersionCodename);
        dest.writeString(this.appComponentFactory);
        dest.writeInt(this.mHiddenApiPolicy);
        dest.writeString(String.valueOf(this.maxAspectRatio));
        dest.writeInt(this.hasDefaultNoFullScreen);
        dest.writeInt(this.minEmuiSdkVersion);
        dest.writeInt(this.targetEmuiSdkVersion);
        dest.writeInt(this.hwThemeType);
        dest.writeInt(this.minEmuiSysImgVersion);
        dest.writeInt(this.gestnav_extra_flags);
    }

    private ApplicationInfo(Parcel source) {
        super(source);
        boolean z = false;
        this.fullBackupContent = 0;
        this.uiOptions = 0;
        this.flags = 0;
        this.hwFlags = 0;
        this.hwHbsUid = -1;
        this.requiresSmallestWidthDp = 0;
        this.compatibleWidthLimitDp = 0;
        this.largestWidthLimitDp = 0;
        this.enabled = true;
        this.enabledSetting = 0;
        this.installLocation = -1;
        this.category = -1;
        this.mHiddenApiPolicy = -1;
        this.taskAffinity = source.readString();
        this.permission = source.readString();
        this.processName = source.readString();
        this.className = source.readString();
        this.theme = source.readInt();
        this.flags = source.readInt();
        this.privateFlags = source.readInt();
        this.hwFlags = source.readInt();
        this.requiresSmallestWidthDp = source.readInt();
        this.compatibleWidthLimitDp = source.readInt();
        this.largestWidthLimitDp = source.readInt();
        if (source.readInt() != 0) {
            this.storageUuid = new UUID(source.readLong(), source.readLong());
            this.volumeUuid = StorageManager.convert(this.storageUuid);
        }
        this.scanSourceDir = source.readString();
        this.scanPublicSourceDir = source.readString();
        this.sourceDir = source.readString();
        this.publicSourceDir = source.readString();
        this.splitNames = source.readStringArray();
        this.splitSourceDirs = source.readStringArray();
        this.splitPublicSourceDirs = source.readStringArray();
        this.splitDependencies = source.readSparseArray(null);
        this.nativeLibraryDir = source.readString();
        this.secondaryNativeLibraryDir = source.readString();
        this.nativeLibraryRootDir = source.readString();
        this.nativeLibraryRootRequiresIsa = source.readInt() != 0;
        this.primaryCpuAbi = source.readString();
        this.secondaryCpuAbi = source.readString();
        this.resourceDirs = source.readStringArray();
        this.seInfo = source.readString();
        this.seInfoUser = source.readString();
        this.sharedLibraryFiles = source.readStringArray();
        this.dataDir = source.readString();
        this.deviceProtectedDataDir = source.readString();
        this.credentialProtectedDataDir = source.readString();
        this.uid = source.readInt();
        this.minSdkVersion = source.readInt();
        this.targetSdkVersion = source.readInt();
        setVersionCode(source.readLong());
        if (source.readInt() != 0) {
            z = true;
        }
        this.enabled = z;
        this.enabledSetting = source.readInt();
        this.installLocation = source.readInt();
        this.manageSpaceActivityName = source.readString();
        this.backupAgentName = source.readString();
        this.descriptionRes = source.readInt();
        this.uiOptions = source.readInt();
        this.fullBackupContent = source.readInt();
        this.networkSecurityConfigRes = source.readInt();
        this.category = source.readInt();
        this.targetSandboxVersion = source.readInt();
        this.classLoaderName = source.readString();
        this.splitClassLoaderNames = source.readStringArray();
        this.compileSdkVersion = source.readInt();
        this.compileSdkVersionCodename = source.readString();
        this.appComponentFactory = source.readString();
        this.mHiddenApiPolicy = source.readInt();
        String aspect = source.readString();
        this.maxAspectRatio = aspect != null ? Float.parseFloat(aspect) : 0.0f;
        this.hasDefaultNoFullScreen = source.readInt();
        this.minEmuiSdkVersion = source.readInt();
        this.targetEmuiSdkVersion = source.readInt();
        this.hwThemeType = source.readInt();
        this.minEmuiSysImgVersion = source.readInt();
        this.gestnav_extra_flags = source.readInt();
    }

    public CharSequence loadDescription(PackageManager pm) {
        if (this.descriptionRes != 0) {
            CharSequence label = pm.getText(this.packageName, this.descriptionRes, this);
            if (label != null) {
                return label;
            }
        }
        return null;
    }

    public void disableCompatibilityMode() {
        this.flags |= 540160;
    }

    public boolean usesCompatibilityMode() {
        return this.targetSdkVersion < 4 || (this.flags & 540160) == 0;
    }

    public void initForUser(int userId) {
        this.uid = UserHandle.getUid(userId, UserHandle.getAppId(this.uid));
        if ("android".equals(this.packageName)) {
            this.dataDir = Environment.getDataSystemDirectory().getAbsolutePath();
            return;
        }
        this.deviceProtectedDataDir = Environment.getDataUserDePackageDirectory(this.volumeUuid, userId, this.packageName).getAbsolutePath();
        this.credentialProtectedDataDir = Environment.getDataUserCePackageDirectory(this.volumeUuid, userId, this.packageName).getAbsolutePath();
        if ((this.privateFlags & 32) != 0) {
            this.dataDir = this.deviceProtectedDataDir;
        } else {
            this.dataDir = this.credentialProtectedDataDir;
        }
    }

    private boolean isPackageWhitelistedForHiddenApis() {
        return SystemConfig.getInstance().getHiddenApiWhitelistedApps().contains(this.packageName);
    }

    private boolean isAllowedToUseHiddenApis() {
        return isSignedWithPlatformKey() || (isPackageWhitelistedForHiddenApis() && (isSystemApp() || isUpdatedSystemApp()));
    }

    public int getHiddenApiEnforcementPolicy() {
        if (isAllowedToUseHiddenApis()) {
            return 0;
        }
        if (this.mHiddenApiPolicy != -1) {
            return this.mHiddenApiPolicy;
        }
        if (this.targetSdkVersion < 28) {
            return 3;
        }
        return 2;
    }

    public void setHiddenApiEnforcementPolicy(int policy) {
        if (isValidHiddenApiEnforcementPolicy(policy)) {
            this.mHiddenApiPolicy = policy;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid API enforcement policy: ");
        stringBuilder.append(policy);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void maybeUpdateHiddenApiEnforcementPolicy(int policyPreP, int policyP) {
        if (!isPackageWhitelistedForHiddenApis()) {
            if (this.targetSdkVersion < 28) {
                setHiddenApiEnforcementPolicy(policyPreP);
            } else if (this.targetSdkVersion >= 28) {
                setHiddenApiEnforcementPolicy(policyP);
            }
        }
    }

    public void setVersionCode(long newVersionCode) {
        this.longVersionCode = newVersionCode;
        this.versionCode = (int) newVersionCode;
    }

    public Drawable loadDefaultIcon(PackageManager pm) {
        if ((this.flags & 262144) == 0 || !isPackageUnavailable(pm)) {
            return pm.getDefaultActivityIcon();
        }
        return Resources.getSystem().getDrawable(R.drawable.sym_app_on_sd_unavailable_icon);
    }

    private boolean isPackageUnavailable(PackageManager pm) {
        boolean z = true;
        try {
            if (pm.getPackageInfo(this.packageName, 0) != null) {
                z = false;
            }
            return z;
        } catch (NameNotFoundException e) {
            return true;
        }
    }

    public boolean isDefaultToDeviceProtectedStorage() {
        return (this.privateFlags & 32) != 0;
    }

    public boolean isDirectBootAware() {
        return (this.privateFlags & 64) != 0;
    }

    public boolean isEncryptionAware() {
        return isDirectBootAware() || isPartiallyDirectBootAware();
    }

    public boolean isExternal() {
        return (this.flags & 262144) != 0;
    }

    public boolean isExternalAsec() {
        return TextUtils.isEmpty(this.volumeUuid) && isExternal();
    }

    public boolean isForwardLocked() {
        return (this.privateFlags & 4) != 0;
    }

    @SystemApi
    public boolean isInstantApp() {
        return (this.privateFlags & 128) != 0;
    }

    public boolean isInternal() {
        return (this.flags & 262144) == 0;
    }

    public boolean isOem() {
        return (this.privateFlags & 131072) != 0;
    }

    public boolean isPartiallyDirectBootAware() {
        return (this.privateFlags & 256) != 0;
    }

    public boolean isSignedWithPlatformKey() {
        return (this.privateFlags & 1048576) != 0;
    }

    public boolean isPrivilegedApp() {
        return (this.privateFlags & 8) != 0;
    }

    public boolean isRequiredForSystemUser() {
        return (this.privateFlags & 512) != 0;
    }

    public boolean isStaticSharedLibrary() {
        return (this.privateFlags & 16384) != 0;
    }

    public boolean isSystemApp() {
        return (this.flags & 1) != 0;
    }

    public boolean isUpdatedSystemApp() {
        return (this.flags & 128) != 0;
    }

    public boolean isVendor() {
        return (this.privateFlags & 262144) != 0;
    }

    public boolean isProduct() {
        return (this.privateFlags & 524288) != 0;
    }

    public boolean isVirtualPreload() {
        return (this.privateFlags & 65536) != 0;
    }

    public boolean requestsIsolatedSplitLoading() {
        return (this.privateFlags & 32768) != 0;
    }

    protected ApplicationInfo getApplicationInfo() {
        return this;
    }

    public void setCodePath(String codePath) {
        this.scanSourceDir = codePath;
    }

    public void setBaseCodePath(String baseCodePath) {
        this.sourceDir = baseCodePath;
    }

    public void setSplitCodePaths(String[] splitCodePaths) {
        this.splitSourceDirs = splitCodePaths;
    }

    public void setResourcePath(String resourcePath) {
        this.scanPublicSourceDir = resourcePath;
    }

    public void setBaseResourcePath(String baseResourcePath) {
        this.publicSourceDir = baseResourcePath;
    }

    public void setSplitResourcePaths(String[] splitResourcePaths) {
        this.splitPublicSourceDirs = splitResourcePaths;
    }

    public String getCodePath() {
        return this.scanSourceDir;
    }

    public String getBaseCodePath() {
        return this.sourceDir;
    }

    public String[] getSplitCodePaths() {
        return this.splitSourceDirs;
    }

    public String getResourcePath() {
        return this.scanPublicSourceDir;
    }

    public String getBaseResourcePath() {
        return this.publicSourceDir;
    }

    public String[] getSplitResourcePaths() {
        return this.splitPublicSourceDirs;
    }
}
