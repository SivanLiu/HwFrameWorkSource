package android.content.pm;

import android.Manifest.permission;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.Notification;
import android.app.slice.Slice;
import android.common.HwFrameworkFactory;
import android.common.HwFrameworkMonitor;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.pm.ActivityInfo.WindowLayout;
import android.content.pm.PackageParserCacheHelper.ReadHelper;
import android.content.pm.PackageParserCacheHelper.WriteHelper;
import android.content.pm.split.DefaultSplitAssetLoader;
import android.content.pm.split.SplitAssetDependencyLoader;
import android.content.pm.split.SplitAssetLoader;
import android.content.pm.split.SplitDependencyLoader;
import android.content.pm.split.SplitDependencyLoader.IllegalDependencyException;
import android.content.res.ApkAssets;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.hwcontrol.HwWidgetFactory;
import android.hwtheme.HwThemeManager;
import android.media.midi.MidiDeviceInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.PatternMatcher;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.ByteStringUtils;
import android.util.DisplayMetrics;
import android.util.Flog;
import android.util.Log;
import android.util.PackageUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedValue;
import android.util.apk.ApkSignatureVerifier;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.ClassLoaderFactory;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class PackageParser {
    public static final String ANDROID_MANIFEST_FILENAME = "AndroidManifest.xml";
    private static final String ANDROID_RESOURCES = "http://schemas.android.com/apk/res/android";
    public static final String APK_FILE_EXTENSION = ".apk";
    public static final int APK_SIGNING_UNKNOWN = 0;
    public static final int APK_SIGNING_V1 = 1;
    public static final int APK_SIGNING_V2 = 2;
    private static final Set<String> CHILD_PACKAGE_TAGS = new ArraySet();
    private static final int CURRENT_EMUI_SDK_VERSION = SystemProperties.getInt("ro.build.hw_emui_api_level", 0);
    private static final boolean DEBUG_BACKUP = false;
    private static final boolean DEBUG_JAR = false;
    private static final boolean DEBUG_PARSER = false;
    private static final boolean LOG_PARSE_TIMINGS = Build.IS_DEBUGGABLE;
    private static final int LOG_PARSE_TIMINGS_THRESHOLD_MS = 100;
    private static final boolean LOG_UNSAFE_BROADCASTS = false;
    private static final int MAX_PACKAGES_PER_APK = 5;
    private static final String METADATA_GESTURE_NAV_OPTIONS = "hw.gesture_nav_options";
    private static final String METADATA_MAX_ASPECT_RATIO = "android.max_aspect";
    private static final String METADATA_NOTCH_SUPPORT = "android.notch_support";
    private static final String MNT_EXPAND = "/mnt/expand/";
    private static final boolean MULTI_PACKAGE_APK_ENABLED;
    public static final NewPermissionInfo[] NEW_PERMISSIONS = new NewPermissionInfo[]{new NewPermissionInfo(permission.WRITE_EXTERNAL_STORAGE, 4, 0), new NewPermissionInfo(permission.READ_PHONE_STATE, 4, 0)};
    public static final int PARSE_CHATTY = Integer.MIN_VALUE;
    public static final int PARSE_COLLECT_CERTIFICATES = 32;
    private static final int PARSE_DEFAULT_INSTALL_LOCATION = -1;
    private static final int PARSE_DEFAULT_TARGET_SANDBOX = 1;
    public static final int PARSE_ENFORCE_CODE = 64;
    public static final int PARSE_EXTERNAL_STORAGE = 8;
    public static final int PARSE_FORCE_SDK = 128;
    @Deprecated
    public static final int PARSE_FORWARD_LOCK = 4;
    public static final int PARSE_IGNORE_PROCESSES = 2;
    public static final int PARSE_IS_SYSTEM_DIR = 16;
    public static final int PARSE_MUST_BE_APK = 1;
    private static final String PROPERTY_CHILD_PACKAGES_ENABLED = "persist.sys.child_packages_enabled";
    private static final int RECREATE_ON_CONFIG_CHANGES_MASK = 3;
    private static final boolean RIGID_PARSER = false;
    private static final Set<String> SAFE_BROADCASTS = new ArraySet();
    private static final String[] SDK_CODENAMES = VERSION.ACTIVE_CODENAMES;
    private static final int SDK_VERSION = VERSION.SDK_INT;
    public static final SplitPermissionInfo[] SPLIT_PERMISSIONS = new SplitPermissionInfo[]{new SplitPermissionInfo(permission.WRITE_EXTERNAL_STORAGE, new String[]{permission.READ_EXTERNAL_STORAGE}, 10001), new SplitPermissionInfo(permission.READ_CONTACTS, new String[]{permission.READ_CALL_LOG}, 16), new SplitPermissionInfo(permission.WRITE_CONTACTS, new String[]{permission.WRITE_CALL_LOG}, 16)};
    private static final String TAG = "PackageParser";
    private static final String TAG_ADOPT_PERMISSIONS = "adopt-permissions";
    private static final String TAG_APPLICATION = "application";
    private static final String TAG_COMPATIBLE_SCREENS = "compatible-screens";
    private static final String TAG_EAT_COMMENT = "eat-comment";
    private static final String TAG_FEATURE_GROUP = "feature-group";
    private static final String TAG_INSTRUMENTATION = "instrumentation";
    private static final String TAG_KEY_SETS = "key-sets";
    private static final String TAG_MANIFEST = "manifest";
    private static final String TAG_ORIGINAL_PACKAGE = "original-package";
    private static final String TAG_OVERLAY = "overlay";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PACKAGE_VERIFIER = "package-verifier";
    private static final String TAG_PERMISSION = "permission";
    private static final String TAG_PERMISSION_GROUP = "permission-group";
    private static final String TAG_PERMISSION_TREE = "permission-tree";
    private static final String TAG_PROTECTED_BROADCAST = "protected-broadcast";
    private static final String TAG_RESTRICT_UPDATE = "restrict-update";
    private static final String TAG_SUPPORTS_INPUT = "supports-input";
    private static final String TAG_SUPPORT_SCREENS = "supports-screens";
    private static final String TAG_USES_CONFIGURATION = "uses-configuration";
    private static final String TAG_USES_FEATURE = "uses-feature";
    private static final String TAG_USES_GL_TEXTURE = "uses-gl-texture";
    private static final String TAG_USES_PERMISSION = "uses-permission";
    private static final String TAG_USES_PERMISSION_SDK_23 = "uses-permission-sdk-23";
    private static final String TAG_USES_PERMISSION_SDK_M = "uses-permission-sdk-m";
    private static final String TAG_USES_SDK = "uses-sdk";
    private static final String TAG_USES_SPLIT = "uses-split";
    private static float mDefaultMaxAspectRatio = 1.86f;
    private static float mExclusionNavBar = 0.0f;
    private static boolean mFristAddView = true;
    private static boolean mFullScreenDisplay = false;
    private static float mScreenAspectRatio = 0.0f;
    public static final AtomicInteger sCachedPackageReadCount = new AtomicInteger();
    private static boolean sCompatibilityModeEnabled = true;
    private static int sCurrentEmuiSysImgVersion = 0;
    private static final Comparator<String> sSplitNameComparator = new SplitNameComparator();
    @Deprecated
    private String mArchiveSourcePath;
    private File mCacheDir;
    private Callback mCallback;
    private DisplayMetrics mMetrics = new DisplayMetrics();
    private boolean mOnlyCoreApps;
    private int mParseError = 1;
    private ParsePackageItemArgs mParseInstrumentationArgs;
    private String[] mSeparateProcesses;

    public static class ApkLite {
        public final String codePath;
        public final String configForSplit;
        public final boolean coreApp;
        public final boolean debuggable;
        public final boolean extractNativeLibs;
        public final int installLocation;
        public boolean isFeatureSplit;
        public final boolean isolatedSplits;
        public final boolean multiArch;
        public final String packageName;
        public final int revisionCode;
        public final SigningDetails signingDetails;
        public final String splitName;
        public final boolean use32bitAbi;
        public final String usesSplitName;
        public final VerifierInfo[] verifiers;
        public final int versionCode;
        public final int versionCodeMajor;

        public ApkLite(String codePath, String packageName, String splitName, boolean isFeatureSplit, String configForSplit, String usesSplitName, int versionCode, int versionCodeMajor, int revisionCode, int installLocation, List<VerifierInfo> verifiers, SigningDetails signingDetails, boolean coreApp, boolean debuggable, boolean multiArch, boolean use32bitAbi, boolean extractNativeLibs, boolean isolatedSplits) {
            this.codePath = codePath;
            this.packageName = packageName;
            this.splitName = splitName;
            this.isFeatureSplit = isFeatureSplit;
            this.configForSplit = configForSplit;
            this.usesSplitName = usesSplitName;
            this.versionCode = versionCode;
            this.versionCodeMajor = versionCodeMajor;
            this.revisionCode = revisionCode;
            this.installLocation = installLocation;
            this.signingDetails = signingDetails;
            this.verifiers = (VerifierInfo[]) verifiers.toArray(new VerifierInfo[verifiers.size()]);
            this.coreApp = coreApp;
            this.debuggable = debuggable;
            this.multiArch = multiArch;
            this.use32bitAbi = use32bitAbi;
            this.extractNativeLibs = extractNativeLibs;
            this.isolatedSplits = isolatedSplits;
        }

        public long getLongVersionCode() {
            return PackageInfo.composeLongVersionCode(this.versionCodeMajor, this.versionCode);
        }
    }

    private static class CachedComponentArgs {
        ParseComponentArgs mActivityAliasArgs;
        ParseComponentArgs mActivityArgs;
        ParseComponentArgs mProviderArgs;
        ParseComponentArgs mServiceArgs;

        private CachedComponentArgs() {
        }
    }

    public interface Callback {
        String[] getOverlayApks(String str);

        String[] getOverlayPaths(String str, String str2);

        boolean hasFeature(String str);
    }

    public static abstract class Component<II extends IntentInfo> {
        public final String className;
        ComponentName componentName;
        String componentShortName;
        public final ArrayList<II> intents;
        public Bundle metaData;
        public int order;
        public Package owner;

        public Component(Package _owner) {
            this.owner = _owner;
            this.intents = null;
            this.className = null;
        }

        public Component(ParsePackageItemArgs args, PackageItemInfo outInfo) {
            ParsePackageItemArgs parsePackageItemArgs = args;
            this.owner = parsePackageItemArgs.owner;
            this.intents = new ArrayList(0);
            if (PackageParser.parsePackageItemInfo(parsePackageItemArgs.owner, outInfo, parsePackageItemArgs.outError, parsePackageItemArgs.tag, parsePackageItemArgs.sa, true, parsePackageItemArgs.nameRes, parsePackageItemArgs.labelRes, parsePackageItemArgs.iconRes, parsePackageItemArgs.roundIconRes, parsePackageItemArgs.logoRes, parsePackageItemArgs.bannerRes)) {
                this.className = outInfo.name;
                return;
            }
            PackageItemInfo packageItemInfo = outInfo;
            this.className = null;
        }

        public Component(ParseComponentArgs args, ComponentInfo outInfo) {
            this((ParsePackageItemArgs) args, (PackageItemInfo) outInfo);
            if (args.outError[0] == null) {
                if (args.processRes != 0) {
                    String nonConfigurationString;
                    if (this.owner.applicationInfo.targetSdkVersion >= 8) {
                        nonConfigurationString = args.sa.getNonConfigurationString(args.processRes, 1024);
                    } else {
                        nonConfigurationString = args.sa.getNonResourceString(args.processRes);
                    }
                    outInfo.processName = PackageParser.buildProcessName(this.owner.applicationInfo.packageName, this.owner.applicationInfo.processName, nonConfigurationString, args.flags, args.sepProcesses, args.outError);
                }
                if (args.descriptionRes != 0) {
                    outInfo.descriptionRes = args.sa.getResourceId(args.descriptionRes, 0);
                }
                outInfo.enabled = args.sa.getBoolean(args.enabledRes, true);
            }
        }

        public Component(Component<II> clone) {
            this.owner = clone.owner;
            this.intents = clone.intents;
            this.className = clone.className;
            this.componentName = clone.componentName;
            this.componentShortName = clone.componentShortName;
        }

        public ComponentName getComponentName() {
            if (this.componentName != null) {
                return this.componentName;
            }
            if (this.className != null) {
                this.componentName = new ComponentName(this.owner.applicationInfo.packageName, this.className);
            }
            return this.componentName;
        }

        protected Component(Parcel in) {
            this.className = in.readString();
            this.metaData = in.readBundle();
            this.intents = createIntentsList(in);
            this.owner = null;
        }

        protected void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.className);
            dest.writeBundle(this.metaData);
            writeIntentsList(this.intents, dest, flags);
        }

        private static void writeIntentsList(ArrayList<? extends IntentInfo> list, Parcel out, int flags) {
            if (list == null) {
                out.writeInt(-1);
                return;
            }
            int N = list.size();
            out.writeInt(N);
            if (N > 0) {
                int i = 0;
                out.writeString(((IntentInfo) list.get(0)).getClass().getName());
                while (i < N) {
                    ((IntentInfo) list.get(i)).writeIntentInfoToParcel(out, flags);
                    i++;
                }
            }
        }

        private static <T extends IntentInfo> ArrayList<T> createIntentsList(Parcel in) {
            int N = in.readInt();
            if (N == -1) {
                return null;
            }
            if (N == 0) {
                return new ArrayList(0);
            }
            String componentName = in.readString();
            try {
                Constructor<T> cons = Class.forName(componentName).getConstructor(new Class[]{Parcel.class});
                ArrayList<T> intentsList = new ArrayList(N);
                for (int i = 0; i < N; i++) {
                    intentsList.add((IntentInfo) cons.newInstance(new Object[]{in}));
                }
                return intentsList;
            } catch (ReflectiveOperationException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to construct intent list for: ");
                stringBuilder.append(componentName);
                throw new AssertionError(stringBuilder.toString());
            }
        }

        public void appendComponentShortName(StringBuilder sb) {
            ComponentName.appendShortString(sb, this.owner.applicationInfo.packageName, this.className);
        }

        public void printComponentShortName(PrintWriter pw) {
            ComponentName.printShortString(pw, this.owner.applicationInfo.packageName, this.className);
        }

        public void setPackageName(String packageName) {
            this.componentName = null;
            this.componentShortName = null;
        }
    }

    public static class NewPermissionInfo {
        public final int fileVersion;
        public final String name;
        public final int sdkVersion;

        public NewPermissionInfo(String name, int sdkVersion, int fileVersion) {
            this.name = name;
            this.sdkVersion = sdkVersion;
            this.fileVersion = fileVersion;
        }
    }

    public static class PackageLite {
        public final String baseCodePath;
        public final int baseRevisionCode;
        public final String codePath;
        public final String[] configForSplit;
        public final boolean coreApp;
        public final boolean debuggable;
        public final boolean extractNativeLibs;
        public final int installLocation;
        public final boolean[] isFeatureSplits;
        public final boolean isolatedSplits;
        public final boolean multiArch;
        public final String packageName;
        public final String[] splitCodePaths;
        public final String[] splitNames;
        public final int[] splitRevisionCodes;
        public final boolean use32bitAbi;
        public final String[] usesSplitNames;
        public final VerifierInfo[] verifiers;
        public final int versionCode;
        public final int versionCodeMajor;

        public PackageLite(String codePath, ApkLite baseApk, String[] splitNames, boolean[] isFeatureSplits, String[] usesSplitNames, String[] configForSplit, String[] splitCodePaths, int[] splitRevisionCodes) {
            this.packageName = baseApk.packageName;
            this.versionCode = baseApk.versionCode;
            this.versionCodeMajor = baseApk.versionCodeMajor;
            this.installLocation = baseApk.installLocation;
            this.verifiers = baseApk.verifiers;
            this.splitNames = splitNames;
            this.isFeatureSplits = isFeatureSplits;
            this.usesSplitNames = usesSplitNames;
            this.configForSplit = configForSplit;
            this.codePath = codePath;
            this.baseCodePath = baseApk.codePath;
            this.splitCodePaths = splitCodePaths;
            this.baseRevisionCode = baseApk.revisionCode;
            this.splitRevisionCodes = splitRevisionCodes;
            this.coreApp = baseApk.coreApp;
            this.debuggable = baseApk.debuggable;
            this.multiArch = baseApk.multiArch;
            this.use32bitAbi = baseApk.use32bitAbi;
            this.extractNativeLibs = baseApk.extractNativeLibs;
            this.isolatedSplits = baseApk.isolatedSplits;
        }

        public List<String> getAllCodePaths() {
            ArrayList<String> paths = new ArrayList();
            paths.add(this.baseCodePath);
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                Collections.addAll(paths, this.splitCodePaths);
            }
            return paths;
        }
    }

    public static class PackageParserException extends Exception {
        public final int error;

        public PackageParserException(int error, String detailMessage) {
            super(detailMessage);
            this.error = error;
        }

        public PackageParserException(int error, String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
            this.error = error;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ParseFlags {
    }

    static class ParsePackageItemArgs {
        final int bannerRes;
        final int iconRes;
        final int labelRes;
        final int logoRes;
        final int nameRes;
        final String[] outError;
        final Package owner;
        final int roundIconRes;
        TypedArray sa;
        String tag;

        ParsePackageItemArgs(Package _owner, String[] _outError, int _nameRes, int _labelRes, int _iconRes, int _roundIconRes, int _logoRes, int _bannerRes) {
            this.owner = _owner;
            this.outError = _outError;
            this.nameRes = _nameRes;
            this.labelRes = _labelRes;
            this.iconRes = _iconRes;
            this.logoRes = _logoRes;
            this.bannerRes = _bannerRes;
            this.roundIconRes = _roundIconRes;
        }
    }

    private static class SplitNameComparator implements Comparator<String> {
        private SplitNameComparator() {
        }

        public int compare(String lhs, String rhs) {
            if (lhs == null) {
                return -1;
            }
            if (rhs == null) {
                return 1;
            }
            return lhs.compareTo(rhs);
        }
    }

    public static class SplitPermissionInfo {
        public final String[] newPerms;
        public final String rootPerm;
        public final int targetSdk;

        public SplitPermissionInfo(String rootPerm, String[] newPerms, int targetSdk) {
            this.rootPerm = rootPerm;
            this.newPerms = newPerms;
            this.targetSdk = targetSdk;
        }
    }

    public static final class Activity extends Component<ActivityIntentInfo> implements Parcelable {
        public static final Creator CREATOR = new Creator<Activity>() {
            public Activity createFromParcel(Parcel in) {
                return new Activity(in, null);
            }

            public Activity[] newArray(int size) {
                return new Activity[size];
            }
        };
        public final ActivityInfo info;
        private boolean mHasMaxAspectRatio;

        private boolean hasMaxAspectRatio() {
            return this.mHasMaxAspectRatio;
        }

        public Activity(ParseComponentArgs args, ActivityInfo _info) {
            super(args, (ComponentInfo) _info);
            this.info = _info;
            this.info.applicationInfo = args.owner.applicationInfo;
        }

        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        private void setMaxAspectRatio(float maxAspectRatio) {
            if (this.info.resizeMode == 2 || this.info.resizeMode == 1) {
                setOriginMaxRatio(PackageParser.mScreenAspectRatio);
                this.info.maxAspectRatio = 0.0f;
            } else if (PackageParser.mFullScreenDisplay && this.info.applicationInfo.maxAspectRatio >= PackageParser.mScreenAspectRatio) {
                setOriginMaxRatio(PackageParser.mScreenAspectRatio);
                this.info.maxAspectRatio = 0.0f;
            } else if (maxAspectRatio < 1.0f) {
                setOriginMaxRatio(PackageParser.mScreenAspectRatio);
                this.info.maxAspectRatio = 0.0f;
            } else {
                if (PackageParser.mFullScreenDisplay && maxAspectRatio < PackageParser.mScreenAspectRatio) {
                    this.owner.applicationInfo.hasDefaultNoFullScreen = 1;
                    if (maxAspectRatio > PackageParser.mExclusionNavBar) {
                        this.info.originMaxAspectRatio = PackageParser.mExclusionNavBar;
                        this.info.maxAspectRatio = PackageParser.mExclusionNavBar;
                        this.mHasMaxAspectRatio = true;
                        return;
                    }
                }
                setOriginMaxRatio(maxAspectRatio);
                this.info.originMaxAspectRatio = maxAspectRatio;
                this.info.maxAspectRatio = maxAspectRatio;
                this.mHasMaxAspectRatio = true;
            }
        }

        private void setOriginMaxRatio(float maxRatio) {
            this.info.originMaxAspectRatio = maxRatio;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Activity{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(this.info, flags | 2);
            dest.writeBoolean(this.mHasMaxAspectRatio);
        }

        private Activity(Parcel in) {
            super(in);
            this.info = (ActivityInfo) in.readParcelable(Object.class.getClassLoader());
            this.mHasMaxAspectRatio = in.readBoolean();
            Iterator it = this.intents.iterator();
            while (it.hasNext()) {
                ActivityIntentInfo aii = (ActivityIntentInfo) it.next();
                aii.activity = this;
                this.order = Math.max(aii.getOrder(), this.order);
            }
            if (this.info.permission != null) {
                this.info.permission = this.info.permission.intern();
            }
        }
    }

    public static final class CallbackImpl implements Callback {
        private final PackageManager mPm;

        public CallbackImpl(PackageManager pm) {
            this.mPm = pm;
        }

        public boolean hasFeature(String feature) {
            return this.mPm.hasSystemFeature(feature);
        }

        public String[] getOverlayPaths(String targetPackageName, String targetPath) {
            return null;
        }

        public String[] getOverlayApks(String targetPackageName) {
            return null;
        }
    }

    public static final class Instrumentation extends Component<IntentInfo> implements Parcelable {
        public static final Creator CREATOR = new Creator<Instrumentation>() {
            public Instrumentation createFromParcel(Parcel in) {
                return new Instrumentation(in, null);
            }

            public Instrumentation[] newArray(int size) {
                return new Instrumentation[size];
            }
        };
        public final InstrumentationInfo info;

        public Instrumentation(ParsePackageItemArgs args, InstrumentationInfo _info) {
            super(args, (PackageItemInfo) _info);
            this.info = _info;
        }

        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Instrumentation{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(this.info, flags);
        }

        private Instrumentation(Parcel in) {
            super(in);
            this.info = (InstrumentationInfo) in.readParcelable(Object.class.getClassLoader());
            if (this.info.targetPackage != null) {
                this.info.targetPackage = this.info.targetPackage.intern();
            }
            if (this.info.targetProcesses != null) {
                this.info.targetProcesses = this.info.targetProcesses.intern();
            }
        }
    }

    public static final class Package implements Parcelable {
        public static final Creator CREATOR = new Creator<Package>() {
            public Package createFromParcel(Parcel in) {
                return new Package(in);
            }

            public Package[] newArray(int size) {
                return new Package[size];
            }
        };
        public final ArrayList<Activity> activities;
        public ApplicationInfo applicationInfo;
        public String baseCodePath;
        public boolean baseHardwareAccelerated;
        public int baseRevisionCode;
        public ArrayList<Package> childPackages;
        public String codePath;
        public ArrayList<ConfigurationInfo> configPreferences;
        public boolean coreApp;
        public String cpuAbiOverride;
        public ArrayList<FeatureGroupInfo> featureGroups;
        public int installLocation;
        public final ArrayList<Instrumentation> instrumentation;
        public boolean isStub;
        public ArrayList<String> libraryNames;
        public ArrayList<String> mAdoptPermissions;
        public Bundle mAppMetaData;
        public int mCompileSdkVersion;
        public String mCompileSdkVersionCodename;
        public Object mExtras;
        public ArrayMap<String, ArraySet<PublicKey>> mKeySetMapping;
        public long[] mLastPackageUsageTimeInMills;
        public ArrayList<String> mOriginalPackages;
        public String mOverlayCategory;
        public boolean mOverlayIsStatic;
        public int mOverlayPriority;
        public String mOverlayTarget;
        public boolean mPersistentApp;
        public int mPreferredOrder;
        public String mRealPackage;
        public SigningDetails mRealSigningDetails;
        public String mRequiredAccountType;
        public boolean mRequiredForAllUsers;
        public String mRestrictedAccountType;
        public String mSharedUserId;
        public int mSharedUserLabel;
        public SigningDetails mSigningDetails;
        public ArraySet<String> mUpgradeKeySets;
        public int mVersionCode;
        public int mVersionCodeMajor;
        public String mVersionName;
        public String manifestPackageName;
        public String packageName;
        public Package parentPackage;
        public final ArrayList<PermissionGroup> permissionGroups;
        public final ArrayList<Permission> permissions;
        public ArrayList<ActivityIntentInfo> preferredActivityFilters;
        public ArrayList<String> protectedBroadcasts;
        public final ArrayList<Provider> providers;
        public final ArrayList<Activity> receivers;
        public ArrayList<FeatureInfo> reqFeatures;
        public final ArrayList<String> requestedPermissions;
        public byte[] restrictUpdateHash;
        public final ArrayList<Service> services;
        public String[] splitCodePaths;
        public int[] splitFlags;
        public String[] splitNames;
        public int[] splitPrivateFlags;
        public int[] splitRevisionCodes;
        public String staticSharedLibName;
        public long staticSharedLibVersion;
        public boolean use32bitAbi;
        public ArrayList<String> usesLibraries;
        public String[] usesLibraryFiles;
        public ArrayList<String> usesOptionalLibraries;
        public ArrayList<String> usesStaticLibraries;
        public String[][] usesStaticLibrariesCertDigests;
        public long[] usesStaticLibrariesVersions;
        public boolean visibleToInstantApps;
        public String volumeUuid;

        public long getLongVersionCode() {
            return PackageInfo.composeLongVersionCode(this.mVersionCodeMajor, this.mVersionCode);
        }

        public Package(String packageName) {
            this.applicationInfo = new ApplicationInfo();
            this.permissions = new ArrayList(0);
            this.permissionGroups = new ArrayList(0);
            this.activities = new ArrayList(0);
            this.receivers = new ArrayList(0);
            this.providers = new ArrayList(0);
            this.services = new ArrayList(0);
            this.instrumentation = new ArrayList(0);
            this.requestedPermissions = new ArrayList();
            this.staticSharedLibName = null;
            this.staticSharedLibVersion = 0;
            this.libraryNames = null;
            this.usesLibraries = null;
            this.usesStaticLibraries = null;
            this.usesStaticLibrariesVersions = null;
            this.usesStaticLibrariesCertDigests = null;
            this.usesOptionalLibraries = null;
            this.usesLibraryFiles = null;
            this.preferredActivityFilters = null;
            this.mOriginalPackages = null;
            this.mRealPackage = null;
            this.mAdoptPermissions = null;
            this.mAppMetaData = null;
            this.mSigningDetails = SigningDetails.UNKNOWN;
            this.mRealSigningDetails = SigningDetails.UNKNOWN;
            this.mPreferredOrder = 0;
            this.mLastPackageUsageTimeInMills = new long[8];
            this.configPreferences = null;
            this.reqFeatures = null;
            this.featureGroups = null;
            this.mPersistentApp = false;
            this.packageName = packageName;
            this.manifestPackageName = packageName;
            this.applicationInfo.packageName = packageName;
            this.applicationInfo.uid = -1;
        }

        public void setApplicationVolumeUuid(String volumeUuid) {
            UUID storageUuid = StorageManager.convert(volumeUuid);
            this.applicationInfo.volumeUuid = volumeUuid;
            this.applicationInfo.storageUuid = storageUuid;
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).applicationInfo.volumeUuid = volumeUuid;
                    ((Package) this.childPackages.get(i)).applicationInfo.storageUuid = storageUuid;
                }
            }
        }

        public void setApplicationInfoCodePath(String codePath) {
            this.applicationInfo.setCodePath(codePath);
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).applicationInfo.setCodePath(codePath);
                }
            }
        }

        @Deprecated
        public void setApplicationInfoResourcePath(String resourcePath) {
            this.applicationInfo.setResourcePath(resourcePath);
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).applicationInfo.setResourcePath(resourcePath);
                }
            }
        }

        @Deprecated
        public void setApplicationInfoBaseResourcePath(String resourcePath) {
            this.applicationInfo.setBaseResourcePath(resourcePath);
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).applicationInfo.setBaseResourcePath(resourcePath);
                }
            }
        }

        public void setApplicationInfoBaseCodePath(String baseCodePath) {
            this.applicationInfo.setBaseCodePath(baseCodePath);
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).applicationInfo.setBaseCodePath(baseCodePath);
                }
            }
        }

        public List<String> getChildPackageNames() {
            if (this.childPackages == null) {
                return null;
            }
            int childCount = this.childPackages.size();
            List<String> childPackageNames = new ArrayList(childCount);
            for (int i = 0; i < childCount; i++) {
                childPackageNames.add(((Package) this.childPackages.get(i)).packageName);
            }
            return childPackageNames;
        }

        public boolean hasChildPackage(String packageName) {
            int childCount = this.childPackages != null ? this.childPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                if (((Package) this.childPackages.get(i)).packageName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

        public void setApplicationInfoSplitCodePaths(String[] splitCodePaths) {
            this.applicationInfo.setSplitCodePaths(splitCodePaths);
        }

        @Deprecated
        public void setApplicationInfoSplitResourcePaths(String[] resroucePaths) {
            this.applicationInfo.setSplitResourcePaths(resroucePaths);
        }

        public void setSplitCodePaths(String[] codePaths) {
            this.splitCodePaths = codePaths;
        }

        public void setCodePath(String codePath) {
            this.codePath = codePath;
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).codePath = codePath;
                }
            }
        }

        public void setBaseCodePath(String baseCodePath) {
            this.baseCodePath = baseCodePath;
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).baseCodePath = baseCodePath;
                }
            }
        }

        public void setSigningDetails(SigningDetails signingDetails) {
            this.mSigningDetails = signingDetails;
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).mSigningDetails = signingDetails;
                }
            }
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).volumeUuid = volumeUuid;
                }
            }
        }

        public void setApplicationInfoFlags(int mask, int flags) {
            this.applicationInfo.flags = (this.applicationInfo.flags & (~mask)) | (mask & flags);
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).applicationInfo.flags = (this.applicationInfo.flags & (~mask)) | (mask & flags);
                }
            }
        }

        public void setUse32bitAbi(boolean use32bitAbi) {
            this.use32bitAbi = use32bitAbi;
            if (this.childPackages != null) {
                int packageCount = this.childPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    ((Package) this.childPackages.get(i)).use32bitAbi = use32bitAbi;
                }
            }
        }

        public boolean isLibrary() {
            return (this.staticSharedLibName == null && ArrayUtils.isEmpty(this.libraryNames)) ? false : true;
        }

        public List<String> getAllCodePaths() {
            ArrayList<String> paths = new ArrayList();
            paths.add(this.baseCodePath);
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                Collections.addAll(paths, this.splitCodePaths);
            }
            return paths;
        }

        public List<String> getAllCodePathsExcludingResourceOnly() {
            ArrayList<String> paths = new ArrayList();
            if ((this.applicationInfo.flags & 4) != 0) {
                paths.add(this.baseCodePath);
            }
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                for (int i = 0; i < this.splitCodePaths.length; i++) {
                    if ((this.splitFlags[i] & 4) != 0) {
                        paths.add(this.splitCodePaths[i]);
                    }
                }
            }
            return paths;
        }

        public void setPackageName(String newName) {
            int i;
            this.packageName = newName;
            this.applicationInfo.packageName = newName;
            for (i = this.permissions.size() - 1; i >= 0; i--) {
                ((Permission) this.permissions.get(i)).setPackageName(newName);
            }
            for (i = this.permissionGroups.size() - 1; i >= 0; i--) {
                ((PermissionGroup) this.permissionGroups.get(i)).setPackageName(newName);
            }
            for (i = this.activities.size() - 1; i >= 0; i--) {
                ((Activity) this.activities.get(i)).setPackageName(newName);
            }
            for (i = this.receivers.size() - 1; i >= 0; i--) {
                ((Activity) this.receivers.get(i)).setPackageName(newName);
            }
            for (i = this.providers.size() - 1; i >= 0; i--) {
                ((Provider) this.providers.get(i)).setPackageName(newName);
            }
            for (i = this.services.size() - 1; i >= 0; i--) {
                ((Service) this.services.get(i)).setPackageName(newName);
            }
            for (i = this.instrumentation.size() - 1; i >= 0; i--) {
                ((Instrumentation) this.instrumentation.get(i)).setPackageName(newName);
            }
        }

        public boolean hasComponentClassName(String name) {
            int i;
            for (i = this.activities.size() - 1; i >= 0; i--) {
                if (name.equals(((Activity) this.activities.get(i)).className)) {
                    return true;
                }
            }
            for (i = this.receivers.size() - 1; i >= 0; i--) {
                if (name.equals(((Activity) this.receivers.get(i)).className)) {
                    return true;
                }
            }
            for (i = this.providers.size() - 1; i >= 0; i--) {
                if (name.equals(((Provider) this.providers.get(i)).className)) {
                    return true;
                }
            }
            for (i = this.services.size() - 1; i >= 0; i--) {
                if (name.equals(((Service) this.services.get(i)).className)) {
                    return true;
                }
            }
            for (i = this.instrumentation.size() - 1; i >= 0; i--) {
                if (name.equals(((Instrumentation) this.instrumentation.get(i)).className)) {
                    return true;
                }
            }
            return false;
        }

        public void forceResizeableAllActivity() {
            for (int i = 0; i < this.activities.size(); i++) {
                if (2 != ((Activity) this.activities.get(i)).info.resizeMode) {
                    ((Activity) this.activities.get(i)).info.resizeMode = 4;
                }
            }
        }

        public boolean isExternal() {
            return this.applicationInfo.isExternal();
        }

        public boolean isForwardLocked() {
            return this.applicationInfo.isForwardLocked();
        }

        public boolean isOem() {
            return this.applicationInfo.isOem();
        }

        public boolean isVendor() {
            return this.applicationInfo.isVendor();
        }

        public boolean isProduct() {
            return this.applicationInfo.isProduct();
        }

        public boolean isPrivileged() {
            return this.applicationInfo.isPrivilegedApp();
        }

        public boolean isSystem() {
            return this.applicationInfo.isSystemApp();
        }

        public boolean isUpdatedSystemApp() {
            return this.applicationInfo.isUpdatedSystemApp();
        }

        public boolean canHaveOatDir() {
            return ((isSystem() && !isUpdatedSystemApp()) || isForwardLocked() || this.applicationInfo.isExternalAsec()) ? false : true;
        }

        public boolean isMatch(int flags) {
            if ((1048576 & flags) != 0) {
                return isSystem();
            }
            return true;
        }

        public long getLatestPackageUseTimeInMills() {
            long latestUse = 0;
            for (long use : this.mLastPackageUsageTimeInMills) {
                latestUse = Math.max(latestUse, use);
            }
            return latestUse;
        }

        public long getLatestForegroundPackageUseTimeInMills() {
            long latestUse = 0;
            for (int reason : new int[]{0, 2}) {
                latestUse = Math.max(latestUse, this.mLastPackageUsageTimeInMills[reason]);
            }
            return latestUse;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Package{");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
            stringBuilder.append(" ");
            stringBuilder.append(this.packageName);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        public int describeContents() {
            return 0;
        }

        public Package(Parcel dest) {
            this.applicationInfo = new ApplicationInfo();
            boolean z = false;
            this.permissions = new ArrayList(0);
            this.permissionGroups = new ArrayList(0);
            this.activities = new ArrayList(0);
            this.receivers = new ArrayList(0);
            this.providers = new ArrayList(0);
            this.services = new ArrayList(0);
            this.instrumentation = new ArrayList(0);
            this.requestedPermissions = new ArrayList();
            this.staticSharedLibName = null;
            this.staticSharedLibVersion = 0;
            this.libraryNames = null;
            this.usesLibraries = null;
            this.usesStaticLibraries = null;
            this.usesStaticLibrariesVersions = null;
            this.usesStaticLibrariesCertDigests = null;
            this.usesOptionalLibraries = null;
            this.usesLibraryFiles = null;
            this.preferredActivityFilters = null;
            this.mOriginalPackages = null;
            this.mRealPackage = null;
            this.mAdoptPermissions = null;
            this.mAppMetaData = null;
            this.mSigningDetails = SigningDetails.UNKNOWN;
            this.mRealSigningDetails = SigningDetails.UNKNOWN;
            this.mPreferredOrder = 0;
            this.mLastPackageUsageTimeInMills = new long[8];
            this.configPreferences = null;
            this.reqFeatures = null;
            this.featureGroups = null;
            this.mPersistentApp = false;
            ClassLoader boot = Object.class.getClassLoader();
            this.packageName = dest.readString().intern();
            this.manifestPackageName = dest.readString();
            this.splitNames = dest.readStringArray();
            this.volumeUuid = dest.readString();
            this.codePath = dest.readString();
            this.baseCodePath = dest.readString();
            this.splitCodePaths = dest.readStringArray();
            this.baseRevisionCode = dest.readInt();
            this.splitRevisionCodes = dest.createIntArray();
            this.splitFlags = dest.createIntArray();
            this.splitPrivateFlags = dest.createIntArray();
            this.baseHardwareAccelerated = dest.readInt() == 1;
            this.applicationInfo = (ApplicationInfo) dest.readParcelable(boot);
            if (this.applicationInfo.permission != null) {
                this.applicationInfo.permission = this.applicationInfo.permission.intern();
            }
            dest.readParcelableList(this.permissions, boot);
            fixupOwner(this.permissions);
            dest.readParcelableList(this.permissionGroups, boot);
            fixupOwner(this.permissionGroups);
            dest.readParcelableList(this.activities, boot);
            fixupOwner(this.activities);
            dest.readParcelableList(this.receivers, boot);
            fixupOwner(this.receivers);
            dest.readParcelableList(this.providers, boot);
            fixupOwner(this.providers);
            dest.readParcelableList(this.services, boot);
            fixupOwner(this.services);
            dest.readParcelableList(this.instrumentation, boot);
            fixupOwner(this.instrumentation);
            dest.readStringList(this.requestedPermissions);
            internStringArrayList(this.requestedPermissions);
            this.protectedBroadcasts = dest.createStringArrayList();
            internStringArrayList(this.protectedBroadcasts);
            this.parentPackage = (Package) dest.readParcelable(boot);
            this.childPackages = new ArrayList();
            dest.readParcelableList(this.childPackages, boot);
            if (this.childPackages.size() == 0) {
                this.childPackages = null;
            }
            this.staticSharedLibName = dest.readString();
            if (this.staticSharedLibName != null) {
                this.staticSharedLibName = this.staticSharedLibName.intern();
            }
            this.staticSharedLibVersion = dest.readLong();
            this.libraryNames = dest.createStringArrayList();
            internStringArrayList(this.libraryNames);
            this.usesLibraries = dest.createStringArrayList();
            internStringArrayList(this.usesLibraries);
            this.usesOptionalLibraries = dest.createStringArrayList();
            internStringArrayList(this.usesOptionalLibraries);
            this.usesLibraryFiles = dest.readStringArray();
            int libCount = dest.readInt();
            if (libCount > 0) {
                this.usesStaticLibraries = new ArrayList(libCount);
                dest.readStringList(this.usesStaticLibraries);
                internStringArrayList(this.usesStaticLibraries);
                this.usesStaticLibrariesVersions = new long[libCount];
                dest.readLongArray(this.usesStaticLibrariesVersions);
                this.usesStaticLibrariesCertDigests = new String[libCount][];
                for (int i = 0; i < libCount; i++) {
                    this.usesStaticLibrariesCertDigests[i] = dest.createStringArray();
                }
            }
            this.preferredActivityFilters = new ArrayList();
            dest.readParcelableList(this.preferredActivityFilters, boot);
            if (this.preferredActivityFilters.size() == 0) {
                this.preferredActivityFilters = null;
            }
            this.mOriginalPackages = dest.createStringArrayList();
            this.mRealPackage = dest.readString();
            this.mAdoptPermissions = dest.createStringArrayList();
            this.mAppMetaData = dest.readBundle();
            this.mVersionCode = dest.readInt();
            this.mVersionCodeMajor = dest.readInt();
            this.mVersionName = dest.readString();
            if (this.mVersionName != null) {
                this.mVersionName = this.mVersionName.intern();
            }
            this.mSharedUserId = dest.readString();
            if (this.mSharedUserId != null) {
                this.mSharedUserId = this.mSharedUserId.intern();
            }
            this.mSharedUserLabel = dest.readInt();
            this.mSigningDetails = (SigningDetails) dest.readParcelable(boot);
            this.mPreferredOrder = dest.readInt();
            this.configPreferences = new ArrayList();
            dest.readParcelableList(this.configPreferences, boot);
            if (this.configPreferences.size() == 0) {
                this.configPreferences = null;
            }
            this.reqFeatures = new ArrayList();
            dest.readParcelableList(this.reqFeatures, boot);
            if (this.reqFeatures.size() == 0) {
                this.reqFeatures = null;
            }
            this.featureGroups = new ArrayList();
            dest.readParcelableList(this.featureGroups, boot);
            if (this.featureGroups.size() == 0) {
                this.featureGroups = null;
            }
            this.installLocation = dest.readInt();
            this.coreApp = dest.readInt() == 1;
            this.mRequiredForAllUsers = dest.readInt() == 1;
            this.mRestrictedAccountType = dest.readString();
            this.mRequiredAccountType = dest.readString();
            this.mOverlayTarget = dest.readString();
            this.mOverlayCategory = dest.readString();
            this.mOverlayPriority = dest.readInt();
            this.mOverlayIsStatic = dest.readInt() == 1;
            this.mCompileSdkVersion = dest.readInt();
            this.mCompileSdkVersionCodename = dest.readString();
            this.mUpgradeKeySets = dest.readArraySet(boot);
            this.mKeySetMapping = readKeySetMapping(dest);
            this.cpuAbiOverride = dest.readString();
            this.use32bitAbi = dest.readInt() == 1;
            this.restrictUpdateHash = dest.createByteArray();
            if (dest.readInt() == 1) {
                z = true;
            }
            this.visibleToInstantApps = z;
            this.mPersistentApp = dest.readBoolean();
        }

        private static void internStringArrayList(List<String> list) {
            if (list != null) {
                int N = list.size();
                for (int i = 0; i < N; i++) {
                    list.set(i, ((String) list.get(i)).intern());
                }
            }
        }

        private void fixupOwner(List<? extends Component<?>> list) {
            if (list != null) {
                for (Component<?> c : list) {
                    c.owner = this;
                    if (c instanceof Activity) {
                        ((Activity) c).info.applicationInfo = this.applicationInfo;
                        if (PackageParser.mFullScreenDisplay && ((Activity) c).info.maxAspectRatio > PackageParser.mExclusionNavBar && ((Activity) c).info.maxAspectRatio < PackageParser.mScreenAspectRatio) {
                            ((Activity) c).info.maxAspectRatio = PackageParser.mExclusionNavBar;
                        }
                    } else if (c instanceof Service) {
                        ((Service) c).info.applicationInfo = this.applicationInfo;
                    } else if (c instanceof Provider) {
                        ((Provider) c).info.applicationInfo = this.applicationInfo;
                    }
                }
            }
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.packageName);
            dest.writeString(this.manifestPackageName);
            dest.writeStringArray(this.splitNames);
            dest.writeString(this.volumeUuid);
            dest.writeString(this.codePath);
            dest.writeString(this.baseCodePath);
            dest.writeStringArray(this.splitCodePaths);
            dest.writeInt(this.baseRevisionCode);
            dest.writeIntArray(this.splitRevisionCodes);
            dest.writeIntArray(this.splitFlags);
            dest.writeIntArray(this.splitPrivateFlags);
            dest.writeInt(this.baseHardwareAccelerated);
            dest.writeParcelable(this.applicationInfo, flags);
            dest.writeParcelableList(this.permissions, flags);
            dest.writeParcelableList(this.permissionGroups, flags);
            dest.writeParcelableList(this.activities, flags);
            dest.writeParcelableList(this.receivers, flags);
            dest.writeParcelableList(this.providers, flags);
            dest.writeParcelableList(this.services, flags);
            dest.writeParcelableList(this.instrumentation, flags);
            dest.writeStringList(this.requestedPermissions);
            dest.writeStringList(this.protectedBroadcasts);
            dest.writeParcelable(this.parentPackage, flags);
            dest.writeParcelableList(this.childPackages, flags);
            dest.writeString(this.staticSharedLibName);
            dest.writeLong(this.staticSharedLibVersion);
            dest.writeStringList(this.libraryNames);
            dest.writeStringList(this.usesLibraries);
            dest.writeStringList(this.usesOptionalLibraries);
            dest.writeStringArray(this.usesLibraryFiles);
            if (ArrayUtils.isEmpty(this.usesStaticLibraries)) {
                dest.writeInt(-1);
            } else {
                dest.writeInt(this.usesStaticLibraries.size());
                dest.writeStringList(this.usesStaticLibraries);
                dest.writeLongArray(this.usesStaticLibrariesVersions);
                for (String[] usesStaticLibrariesCertDigest : this.usesStaticLibrariesCertDigests) {
                    dest.writeStringArray(usesStaticLibrariesCertDigest);
                }
            }
            dest.writeParcelableList(this.preferredActivityFilters, flags);
            dest.writeStringList(this.mOriginalPackages);
            dest.writeString(this.mRealPackage);
            dest.writeStringList(this.mAdoptPermissions);
            dest.writeBundle(this.mAppMetaData);
            dest.writeInt(this.mVersionCode);
            dest.writeInt(this.mVersionCodeMajor);
            dest.writeString(this.mVersionName);
            dest.writeString(this.mSharedUserId);
            dest.writeInt(this.mSharedUserLabel);
            dest.writeParcelable(this.mSigningDetails, flags);
            dest.writeInt(this.mPreferredOrder);
            dest.writeParcelableList(this.configPreferences, flags);
            dest.writeParcelableList(this.reqFeatures, flags);
            dest.writeParcelableList(this.featureGroups, flags);
            dest.writeInt(this.installLocation);
            dest.writeInt(this.coreApp);
            dest.writeInt(this.mRequiredForAllUsers);
            dest.writeString(this.mRestrictedAccountType);
            dest.writeString(this.mRequiredAccountType);
            dest.writeString(this.mOverlayTarget);
            dest.writeString(this.mOverlayCategory);
            dest.writeInt(this.mOverlayPriority);
            dest.writeInt(this.mOverlayIsStatic);
            dest.writeInt(this.mCompileSdkVersion);
            dest.writeString(this.mCompileSdkVersionCodename);
            dest.writeArraySet(this.mUpgradeKeySets);
            writeKeySetMapping(dest, this.mKeySetMapping);
            dest.writeString(this.cpuAbiOverride);
            dest.writeInt(this.use32bitAbi);
            dest.writeByteArray(this.restrictUpdateHash);
            dest.writeInt(this.visibleToInstantApps);
            dest.writeBoolean(this.mPersistentApp);
        }

        private static void writeKeySetMapping(Parcel dest, ArrayMap<String, ArraySet<PublicKey>> keySetMapping) {
            if (keySetMapping == null) {
                dest.writeInt(-1);
                return;
            }
            int N = keySetMapping.size();
            dest.writeInt(N);
            for (int i = 0; i < N; i++) {
                dest.writeString((String) keySetMapping.keyAt(i));
                ArraySet<PublicKey> keys = (ArraySet) keySetMapping.valueAt(i);
                if (keys == null) {
                    dest.writeInt(-1);
                } else {
                    int M = keys.size();
                    dest.writeInt(M);
                    for (int j = 0; j < M; j++) {
                        dest.writeSerializable((Serializable) keys.valueAt(j));
                    }
                }
            }
        }

        private static ArrayMap<String, ArraySet<PublicKey>> readKeySetMapping(Parcel in) {
            int N = in.readInt();
            if (N == -1) {
                return null;
            }
            ArrayMap<String, ArraySet<PublicKey>> keySetMapping = new ArrayMap();
            for (int i = 0; i < N; i++) {
                String key = in.readString();
                int M = in.readInt();
                if (M == -1) {
                    keySetMapping.put(key, null);
                } else {
                    ArraySet<PublicKey> keys = new ArraySet(M);
                    for (int j = 0; j < M; j++) {
                        keys.add((PublicKey) in.readSerializable());
                    }
                    keySetMapping.put(key, keys);
                }
            }
            return keySetMapping;
        }
    }

    @VisibleForTesting
    public static class ParseComponentArgs extends ParsePackageItemArgs {
        final int descriptionRes;
        final int enabledRes;
        int flags;
        final int processRes;
        final String[] sepProcesses;

        public ParseComponentArgs(Package _owner, String[] _outError, int _nameRes, int _labelRes, int _iconRes, int _roundIconRes, int _logoRes, int _bannerRes, String[] _sepProcesses, int _processRes, int _descriptionRes, int _enabledRes) {
            super(_owner, _outError, _nameRes, _labelRes, _iconRes, _roundIconRes, _logoRes, _bannerRes);
            this.sepProcesses = _sepProcesses;
            this.processRes = _processRes;
            this.descriptionRes = _descriptionRes;
            this.enabledRes = _enabledRes;
        }
    }

    public static final class Permission extends Component<IntentInfo> implements Parcelable {
        public static final Creator CREATOR = new Creator<Permission>() {
            public Permission createFromParcel(Parcel in) {
                return new Permission(in, null);
            }

            public Permission[] newArray(int size) {
                return new Permission[size];
            }
        };
        public PermissionGroup group;
        public final PermissionInfo info;
        public boolean tree;

        public Permission(Package _owner) {
            super(_owner);
            this.info = new PermissionInfo();
        }

        public Permission(Package _owner, PermissionInfo _info) {
            super(_owner);
            this.info = _info;
        }

        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Permission{");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
            stringBuilder.append(" ");
            stringBuilder.append(this.info.name);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(this.info, flags);
            dest.writeInt(this.tree);
            dest.writeParcelable(this.group, flags);
        }

        public boolean isAppOp() {
            return this.info.isAppOp();
        }

        private Permission(Parcel in) {
            super(in);
            ClassLoader boot = Object.class.getClassLoader();
            this.info = (PermissionInfo) in.readParcelable(boot);
            if (this.info.group != null) {
                this.info.group = this.info.group.intern();
            }
            boolean z = true;
            if (in.readInt() != 1) {
                z = false;
            }
            this.tree = z;
            this.group = (PermissionGroup) in.readParcelable(boot);
        }
    }

    public static final class PermissionGroup extends Component<IntentInfo> implements Parcelable {
        public static final Creator CREATOR = new Creator<PermissionGroup>() {
            public PermissionGroup createFromParcel(Parcel in) {
                return new PermissionGroup(in, null);
            }

            public PermissionGroup[] newArray(int size) {
                return new PermissionGroup[size];
            }
        };
        public final PermissionGroupInfo info;

        public PermissionGroup(Package _owner) {
            super(_owner);
            this.info = new PermissionGroupInfo();
        }

        public PermissionGroup(Package _owner, PermissionGroupInfo _info) {
            super(_owner);
            this.info = _info;
        }

        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PermissionGroup{");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
            stringBuilder.append(" ");
            stringBuilder.append(this.info.name);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(this.info, flags);
        }

        private PermissionGroup(Parcel in) {
            super(in);
            this.info = (PermissionGroupInfo) in.readParcelable(Object.class.getClassLoader());
        }
    }

    public static final class Provider extends Component<ProviderIntentInfo> implements Parcelable {
        public static final Creator CREATOR = new Creator<Provider>() {
            public Provider createFromParcel(Parcel in) {
                return new Provider(in, null);
            }

            public Provider[] newArray(int size) {
                return new Provider[size];
            }
        };
        public final ProviderInfo info;
        public boolean syncable;

        public Provider(ParseComponentArgs args, ProviderInfo _info) {
            super(args, (ComponentInfo) _info);
            this.info = _info;
            this.info.applicationInfo = args.owner.applicationInfo;
            this.syncable = false;
        }

        public Provider(Provider existingProvider) {
            super((Component) existingProvider);
            this.info = existingProvider.info;
            this.syncable = existingProvider.syncable;
        }

        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Provider{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(this.info, flags | 2);
            dest.writeInt(this.syncable);
        }

        private Provider(Parcel in) {
            super(in);
            this.info = (ProviderInfo) in.readParcelable(Object.class.getClassLoader());
            boolean z = true;
            if (in.readInt() != 1) {
                z = false;
            }
            this.syncable = z;
            Iterator it = this.intents.iterator();
            while (it.hasNext()) {
                ((ProviderIntentInfo) it.next()).provider = this;
            }
            if (this.info.readPermission != null) {
                this.info.readPermission = this.info.readPermission.intern();
            }
            if (this.info.writePermission != null) {
                this.info.writePermission = this.info.writePermission.intern();
            }
            if (this.info.authority != null) {
                this.info.authority = this.info.authority.intern();
            }
        }
    }

    public static final class Service extends Component<ServiceIntentInfo> implements Parcelable {
        public static final Creator CREATOR = new Creator<Service>() {
            public Service createFromParcel(Parcel in) {
                return new Service(in, null);
            }

            public Service[] newArray(int size) {
                return new Service[size];
            }
        };
        public final ServiceInfo info;

        public Service(ParseComponentArgs args, ServiceInfo _info) {
            super(args, (ComponentInfo) _info);
            this.info = _info;
            this.info.applicationInfo = args.owner.applicationInfo;
        }

        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Service{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(this.info, flags | 2);
        }

        private Service(Parcel in) {
            super(in);
            this.info = (ServiceInfo) in.readParcelable(Object.class.getClassLoader());
            Iterator it = this.intents.iterator();
            while (it.hasNext()) {
                ServiceIntentInfo aii = (ServiceIntentInfo) it.next();
                aii.service = this;
                this.order = Math.max(aii.getOrder(), this.order);
            }
            if (this.info.permission != null) {
                this.info.permission = this.info.permission.intern();
            }
        }
    }

    public static final class SigningDetails implements Parcelable {
        public static final Creator<SigningDetails> CREATOR = new Creator<SigningDetails>() {
            public SigningDetails createFromParcel(Parcel source) {
                if (source.readBoolean()) {
                    return SigningDetails.UNKNOWN;
                }
                return new SigningDetails(source);
            }

            public SigningDetails[] newArray(int size) {
                return new SigningDetails[size];
            }
        };
        private static final int PAST_CERT_EXISTS = 0;
        public static final SigningDetails UNKNOWN = new SigningDetails(null, 0, null, null, null);
        public final Signature[] pastSigningCertificates;
        public final int[] pastSigningCertificatesFlags;
        public final ArraySet<PublicKey> publicKeys;
        @SignatureSchemeVersion
        public final int signatureSchemeVersion;
        public final Signature[] signatures;

        public static class Builder {
            private Signature[] mPastSigningCertificates;
            private int[] mPastSigningCertificatesFlags;
            private int mSignatureSchemeVersion = 0;
            private Signature[] mSignatures;

            public Builder setSignatures(Signature[] signatures) {
                this.mSignatures = signatures;
                return this;
            }

            public Builder setSignatureSchemeVersion(int signatureSchemeVersion) {
                this.mSignatureSchemeVersion = signatureSchemeVersion;
                return this;
            }

            public Builder setPastSigningCertificates(Signature[] pastSigningCertificates) {
                this.mPastSigningCertificates = pastSigningCertificates;
                return this;
            }

            public Builder setPastSigningCertificatesFlags(int[] pastSigningCertificatesFlags) {
                this.mPastSigningCertificatesFlags = pastSigningCertificatesFlags;
                return this;
            }

            private void checkInvariants() {
                if (this.mSignatures != null) {
                    boolean pastMismatch = false;
                    if (this.mPastSigningCertificates == null || this.mPastSigningCertificatesFlags == null) {
                        if (!(this.mPastSigningCertificates == null && this.mPastSigningCertificatesFlags == null)) {
                            pastMismatch = true;
                        }
                    } else if (this.mPastSigningCertificates.length != this.mPastSigningCertificatesFlags.length) {
                        pastMismatch = true;
                    }
                    if (pastMismatch) {
                        throw new IllegalStateException("SigningDetails must have a one to one mapping between pastSigningCertificates and pastSigningCertificatesFlags");
                    }
                    return;
                }
                throw new IllegalStateException("SigningDetails requires the current signing certificates.");
            }

            public SigningDetails build() throws CertificateException {
                checkInvariants();
                return new SigningDetails(this.mSignatures, this.mSignatureSchemeVersion, this.mPastSigningCertificates, this.mPastSigningCertificatesFlags);
            }
        }

        public @interface CertCapabilities {
            public static final int AUTH = 16;
            public static final int INSTALLED_DATA = 1;
            public static final int PERMISSION = 4;
            public static final int ROLLBACK = 8;
            public static final int SHARED_USER_ID = 2;
        }

        public @interface SignatureSchemeVersion {
            public static final int JAR = 1;
            public static final int SIGNING_BLOCK_V2 = 2;
            public static final int SIGNING_BLOCK_V3 = 3;
            public static final int UNKNOWN = 0;
        }

        @VisibleForTesting
        public SigningDetails(Signature[] signatures, @SignatureSchemeVersion int signatureSchemeVersion, ArraySet<PublicKey> keys, Signature[] pastSigningCertificates, int[] pastSigningCertificatesFlags) {
            this.signatures = signatures;
            this.signatureSchemeVersion = signatureSchemeVersion;
            this.publicKeys = keys;
            this.pastSigningCertificates = pastSigningCertificates;
            this.pastSigningCertificatesFlags = pastSigningCertificatesFlags;
        }

        public SigningDetails(Signature[] signatures, @SignatureSchemeVersion int signatureSchemeVersion, Signature[] pastSigningCertificates, int[] pastSigningCertificatesFlags) throws CertificateException {
            this(signatures, signatureSchemeVersion, PackageParser.toSigningKeys(signatures), pastSigningCertificates, pastSigningCertificatesFlags);
        }

        public SigningDetails(Signature[] signatures, @SignatureSchemeVersion int signatureSchemeVersion) throws CertificateException {
            this(signatures, signatureSchemeVersion, null, null);
        }

        public SigningDetails(SigningDetails orig) {
            if (orig != null) {
                if (orig.signatures != null) {
                    this.signatures = (Signature[]) orig.signatures.clone();
                } else {
                    this.signatures = null;
                }
                this.signatureSchemeVersion = orig.signatureSchemeVersion;
                this.publicKeys = new ArraySet(orig.publicKeys);
                if (orig.pastSigningCertificates != null) {
                    this.pastSigningCertificates = (Signature[]) orig.pastSigningCertificates.clone();
                    this.pastSigningCertificatesFlags = (int[]) orig.pastSigningCertificatesFlags.clone();
                    return;
                }
                this.pastSigningCertificates = null;
                this.pastSigningCertificatesFlags = null;
                return;
            }
            this.signatures = null;
            this.signatureSchemeVersion = 0;
            this.publicKeys = null;
            this.pastSigningCertificates = null;
            this.pastSigningCertificatesFlags = null;
        }

        public boolean hasSignatures() {
            return this.signatures != null && this.signatures.length > 0;
        }

        public boolean hasPastSigningCertificates() {
            return this.pastSigningCertificates != null && this.pastSigningCertificates.length > 0;
        }

        public boolean hasAncestorOrSelf(SigningDetails oldDetails) {
            if (this == UNKNOWN || oldDetails == UNKNOWN) {
                return false;
            }
            if (oldDetails.signatures.length > 1) {
                return signaturesMatchExactly(oldDetails);
            }
            return hasCertificate(oldDetails.signatures[0]);
        }

        /* JADX WARNING: Missing block: B:16:0x0030, code skipped:
            return false;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean hasAncestor(SigningDetails oldDetails) {
            if (this != UNKNOWN && oldDetails != UNKNOWN && hasPastSigningCertificates() && oldDetails.signatures.length == 1) {
                for (int i = 0; i < this.pastSigningCertificates.length - 1; i++) {
                    if (this.pastSigningCertificates[i].equals(oldDetails.signatures[i])) {
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean checkCapability(SigningDetails oldDetails, @CertCapabilities int flags) {
            if (this == UNKNOWN || oldDetails == UNKNOWN) {
                return false;
            }
            if (oldDetails.signatures.length > 1) {
                return signaturesMatchExactly(oldDetails);
            }
            return hasCertificate(oldDetails.signatures[0], flags);
        }

        public boolean checkCapabilityRecover(SigningDetails oldDetails, @CertCapabilities int flags) throws CertificateException {
            if (oldDetails == UNKNOWN || this == UNKNOWN) {
                return false;
            }
            if (!hasPastSigningCertificates() || oldDetails.signatures.length != 1) {
                return Signature.areEffectiveMatch(oldDetails.signatures, this.signatures);
            }
            int i = 0;
            while (i < this.pastSigningCertificates.length) {
                if (Signature.areEffectiveMatch(oldDetails.signatures[0], this.pastSigningCertificates[i]) && this.pastSigningCertificatesFlags[i] == flags) {
                    return true;
                }
                i++;
            }
            return false;
        }

        public boolean hasCertificate(Signature signature) {
            return hasCertificateInternal(signature, 0);
        }

        public boolean hasCertificate(Signature signature, @CertCapabilities int flags) {
            return hasCertificateInternal(signature, flags);
        }

        public boolean hasCertificate(byte[] certificate) {
            return hasCertificate(new Signature(certificate));
        }

        private boolean hasCertificateInternal(Signature signature, int flags) {
            boolean z = false;
            if (this == UNKNOWN) {
                return false;
            }
            if (hasPastSigningCertificates()) {
                int i = 0;
                while (i < this.pastSigningCertificates.length - 1) {
                    if (this.pastSigningCertificates[i].equals(signature) && (flags == 0 || (this.pastSigningCertificatesFlags[i] & flags) == flags)) {
                        return true;
                    }
                    i++;
                }
            }
            if (this.signatures.length == 1 && this.signatures[0].equals(signature)) {
                z = true;
            }
            return z;
        }

        public boolean checkCapability(String sha256String, @CertCapabilities int flags) {
            if (this == UNKNOWN) {
                return false;
            }
            if (hasSha256Certificate(ByteStringUtils.fromHexToByteArray(sha256String), flags)) {
                return true;
            }
            return PackageUtils.computeSignaturesSha256Digest(PackageUtils.computeSignaturesSha256Digests(this.signatures)).equals(sha256String);
        }

        public boolean hasSha256Certificate(byte[] sha256Certificate) {
            return hasSha256CertificateInternal(sha256Certificate, 0);
        }

        public boolean hasSha256Certificate(byte[] sha256Certificate, @CertCapabilities int flags) {
            return hasSha256CertificateInternal(sha256Certificate, flags);
        }

        private boolean hasSha256CertificateInternal(byte[] sha256Certificate, int flags) {
            if (this == UNKNOWN) {
                return false;
            }
            if (hasPastSigningCertificates()) {
                int i = 0;
                while (i < this.pastSigningCertificates.length - 1) {
                    if (Arrays.equals(sha256Certificate, PackageUtils.computeSha256DigestBytes(this.pastSigningCertificates[i].toByteArray())) && (flags == 0 || (this.pastSigningCertificatesFlags[i] & flags) == flags)) {
                        return true;
                    }
                    i++;
                }
            }
            if (this.signatures.length == 1) {
                return Arrays.equals(sha256Certificate, PackageUtils.computeSha256DigestBytes(this.signatures[0].toByteArray()));
            }
            return false;
        }

        public boolean signaturesMatchExactly(SigningDetails other) {
            return Signature.areExactMatch(this.signatures, other.signatures);
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            boolean isUnknown = UNKNOWN == this;
            dest.writeBoolean(isUnknown);
            if (!isUnknown) {
                dest.writeTypedArray(this.signatures, flags);
                dest.writeInt(this.signatureSchemeVersion);
                dest.writeArraySet(this.publicKeys);
                dest.writeTypedArray(this.pastSigningCertificates, flags);
                dest.writeIntArray(this.pastSigningCertificatesFlags);
            }
        }

        protected SigningDetails(Parcel in) {
            ClassLoader boot = Object.class.getClassLoader();
            this.signatures = (Signature[]) in.createTypedArray(Signature.CREATOR);
            this.signatureSchemeVersion = in.readInt();
            this.publicKeys = in.readArraySet(boot);
            this.pastSigningCertificates = (Signature[]) in.createTypedArray(Signature.CREATOR);
            this.pastSigningCertificatesFlags = in.createIntArray();
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SigningDetails)) {
                return false;
            }
            SigningDetails that = (SigningDetails) o;
            if (this.signatureSchemeVersion != that.signatureSchemeVersion || !Signature.areExactMatch(this.signatures, that.signatures)) {
                return false;
            }
            if (this.publicKeys != null) {
                if (!this.publicKeys.equals(that.publicKeys)) {
                    return false;
                }
            } else if (that.publicKeys != null) {
                return false;
            }
            if (Arrays.equals(this.pastSigningCertificates, that.pastSigningCertificates) && Arrays.equals(this.pastSigningCertificatesFlags, that.pastSigningCertificatesFlags)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * ((31 * ((31 * ((31 * Arrays.hashCode(this.signatures)) + this.signatureSchemeVersion)) + (this.publicKeys != null ? this.publicKeys.hashCode() : 0))) + Arrays.hashCode(this.pastSigningCertificates))) + Arrays.hashCode(this.pastSigningCertificatesFlags);
        }
    }

    public static abstract class IntentInfo extends IntentFilter {
        public int banner;
        public boolean hasDefault;
        public int icon;
        public int labelRes;
        public int logo;
        public CharSequence nonLocalizedLabel;
        public int preferred;

        protected IntentInfo() {
        }

        protected IntentInfo(Parcel dest) {
            super(dest);
            boolean z = true;
            if (dest.readInt() != 1) {
                z = false;
            }
            this.hasDefault = z;
            this.labelRes = dest.readInt();
            this.nonLocalizedLabel = dest.readCharSequence();
            this.icon = dest.readInt();
            this.logo = dest.readInt();
            this.banner = dest.readInt();
            this.preferred = dest.readInt();
        }

        public void writeIntentInfoToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.hasDefault);
            dest.writeInt(this.labelRes);
            dest.writeCharSequence(this.nonLocalizedLabel);
            dest.writeInt(this.icon);
            dest.writeInt(this.logo);
            dest.writeInt(this.banner);
            dest.writeInt(this.preferred);
        }
    }

    public static final class ActivityIntentInfo extends IntentInfo {
        public Activity activity;

        public ActivityIntentInfo(Activity _activity) {
            this.activity = _activity;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ActivityIntentInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            this.activity.appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public ActivityIntentInfo(Parcel in) {
            super(in);
        }
    }

    public static final class ProviderIntentInfo extends IntentInfo {
        public Provider provider;

        public ProviderIntentInfo(Provider provider) {
            this.provider = provider;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ProviderIntentInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            this.provider.appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public ProviderIntentInfo(Parcel in) {
            super(in);
        }
    }

    public static final class ServiceIntentInfo extends IntentInfo {
        public Service service;

        public ServiceIntentInfo(Service _service) {
            this.service = _service;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ServiceIntentInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            this.service.appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }

        public ServiceIntentInfo(Parcel in) {
            super(in);
        }
    }

    static {
        boolean z = Build.IS_DEBUGGABLE && SystemProperties.getBoolean(PROPERTY_CHILD_PACKAGES_ENABLED, false);
        MULTI_PACKAGE_APK_ENABLED = z;
        CHILD_PACKAGE_TAGS.add(TAG_APPLICATION);
        CHILD_PACKAGE_TAGS.add(TAG_USES_PERMISSION);
        CHILD_PACKAGE_TAGS.add(TAG_USES_PERMISSION_SDK_M);
        CHILD_PACKAGE_TAGS.add(TAG_USES_PERMISSION_SDK_23);
        CHILD_PACKAGE_TAGS.add(TAG_USES_CONFIGURATION);
        CHILD_PACKAGE_TAGS.add(TAG_USES_FEATURE);
        CHILD_PACKAGE_TAGS.add(TAG_FEATURE_GROUP);
        CHILD_PACKAGE_TAGS.add(TAG_USES_SDK);
        CHILD_PACKAGE_TAGS.add(TAG_SUPPORT_SCREENS);
        CHILD_PACKAGE_TAGS.add(TAG_INSTRUMENTATION);
        CHILD_PACKAGE_TAGS.add(TAG_USES_GL_TEXTURE);
        CHILD_PACKAGE_TAGS.add(TAG_COMPATIBLE_SCREENS);
        CHILD_PACKAGE_TAGS.add(TAG_SUPPORTS_INPUT);
        CHILD_PACKAGE_TAGS.add(TAG_EAT_COMMENT);
        SAFE_BROADCASTS.add(Intent.ACTION_BOOT_COMPLETED);
    }

    public static void setCurrentEmuiSysImgVersion(int version) {
        sCurrentEmuiSysImgVersion = version;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCurrentEmuiSysImgVersion version:");
        stringBuilder.append(version);
        Slog.d(str, stringBuilder.toString());
    }

    private static void initFullScreenData() {
        if (mFristAddView) {
            mDefaultMaxAspectRatio = HwFrameworkFactory.getHwPackageParser().getDefaultNonFullMaxRatio();
            mScreenAspectRatio = HwFrameworkFactory.getHwPackageParser().getDeviceMaxRatio();
            mExclusionNavBar = HwFrameworkFactory.getHwPackageParser().getExclusionNavBarMaxRatio();
            mFullScreenDisplay = HwFrameworkFactory.getHwPackageParser().isFullScreenDevice();
            mFristAddView = false;
        }
    }

    public PackageParser() {
        this.mMetrics.setToDefaults();
        initFullScreenData();
    }

    public void setSeparateProcesses(String[] procs) {
        this.mSeparateProcesses = procs;
    }

    public void setOnlyCoreApps(boolean onlyCoreApps) {
        this.mOnlyCoreApps = onlyCoreApps;
    }

    public void setDisplayMetrics(DisplayMetrics metrics) {
        this.mMetrics = metrics;
    }

    public void setCacheDir(File cacheDir) {
        this.mCacheDir = cacheDir;
    }

    public void setCallback(Callback cb) {
        this.mCallback = cb;
    }

    public static final boolean isApkFile(File file) {
        return isApkPath(file.getName());
    }

    public static boolean isApkPath(String path) {
        return path.endsWith(APK_FILE_EXTENSION);
    }

    public static PackageInfo generatePackageInfo(Package p, int[] gids, int flags, long firstInstallTime, long lastUpdateTime, Set<String> grantedPermissions, PackageUserState state) {
        return generatePackageInfo(p, gids, flags, firstInstallTime, lastUpdateTime, grantedPermissions, state, UserHandle.getCallingUserId());
    }

    private static boolean checkUseInstalledOrHidden(int flags, PackageUserState state, ApplicationInfo appInfo) {
        return state.isAvailable(flags) || !(appInfo == null || !appInfo.isSystemApp() || (PackageManager.MATCH_KNOWN_PACKAGES & flags) == 0);
    }

    public static boolean isAvailable(PackageUserState state) {
        return checkUseInstalledOrHidden(0, state, null);
    }

    public static PackageInfo generatePackageInfo(Package p, int[] gids, int flags, long firstInstallTime, long lastUpdateTime, Set<String> grantedPermissions, PackageUserState state, int userId) {
        Package packageR = p;
        int i = flags;
        Set<String> set = grantedPermissions;
        PackageUserState packageUserState = state;
        int i2 = userId;
        int[] iArr;
        if (checkUseInstalledOrHidden(i, packageUserState, packageR.applicationInfo) && packageR.isMatch(i)) {
            int N;
            int N2;
            int num;
            int i3;
            PackageInfo pi = new PackageInfo();
            pi.packageName = packageR.packageName;
            pi.splitNames = packageR.splitNames;
            pi.versionCode = packageR.mVersionCode;
            pi.versionCodeMajor = packageR.mVersionCodeMajor;
            pi.baseRevisionCode = packageR.baseRevisionCode;
            pi.splitRevisionCodes = packageR.splitRevisionCodes;
            pi.versionName = packageR.mVersionName;
            pi.sharedUserId = packageR.mSharedUserId;
            pi.sharedUserLabel = packageR.mSharedUserLabel;
            pi.applicationInfo = generateApplicationInfo(packageR, i, packageUserState, i2);
            pi.installLocation = packageR.installLocation;
            pi.isStub = packageR.isStub;
            pi.coreApp = packageR.coreApp;
            if (!((pi.applicationInfo.flags & 1) == 0 && (pi.applicationInfo.flags & 128) == 0)) {
                pi.requiredForAllUsers = packageR.mRequiredForAllUsers;
            }
            pi.restrictedAccountType = packageR.mRestrictedAccountType;
            pi.requiredAccountType = packageR.mRequiredAccountType;
            pi.overlayTarget = packageR.mOverlayTarget;
            pi.overlayCategory = packageR.mOverlayCategory;
            pi.overlayPriority = packageR.mOverlayPriority;
            pi.mOverlayIsStatic = packageR.mOverlayIsStatic;
            pi.compileSdkVersion = packageR.mCompileSdkVersion;
            pi.compileSdkVersionCodename = packageR.mCompileSdkVersionCodename;
            pi.firstInstallTime = firstInstallTime;
            pi.lastUpdateTime = lastUpdateTime;
            if ((i & 256) != 0) {
                pi.gids = gids;
            } else {
                iArr = gids;
            }
            if ((i & 16384) != 0) {
                N = packageR.configPreferences != null ? packageR.configPreferences.size() : 0;
                if (N > 0) {
                    pi.configPreferences = new ConfigurationInfo[N];
                    packageR.configPreferences.toArray(pi.configPreferences);
                }
                N2 = packageR.reqFeatures != null ? packageR.reqFeatures.size() : 0;
                if (N2 > 0) {
                    pi.reqFeatures = new FeatureInfo[N2];
                    packageR.reqFeatures.toArray(pi.reqFeatures);
                }
                N2 = packageR.featureGroups != null ? packageR.featureGroups.size() : 0;
                if (N2 > 0) {
                    pi.featureGroups = new FeatureGroupInfo[N2];
                    packageR.featureGroups.toArray(pi.featureGroups);
                }
            }
            if ((i & 1) != 0) {
                N2 = packageR.activities.size();
                if (N2 > 0) {
                    ActivityInfo[] res = new ActivityInfo[N2];
                    num = 0;
                    N = 0;
                    while (N < N2) {
                        Activity a = (Activity) packageR.activities.get(N);
                        int N3 = N2;
                        if (packageUserState.isMatch(a.info, i) != 0) {
                            N2 = num + 1;
                            res[num] = generateActivityInfo(a, i, packageUserState, i2);
                            num = N2;
                        }
                        N++;
                        N2 = N3;
                    }
                    pi.activities = (ActivityInfo[]) ArrayUtils.trimToSize(res, num);
                }
            }
            if ((i & 2) != 0) {
                N2 = packageR.receivers.size();
                if (N2 > 0) {
                    ActivityInfo[] res2 = new ActivityInfo[N2];
                    num = 0;
                    i3 = 0;
                    while (i3 < N2) {
                        Activity a2 = (Activity) packageR.receivers.get(i3);
                        int N4 = N2;
                        if (packageUserState.isMatch(a2.info, i) != 0) {
                            N2 = num + 1;
                            res2[num] = generateActivityInfo(a2, i, packageUserState, i2);
                            num = N2;
                        }
                        i3++;
                        N2 = N4;
                    }
                    pi.receivers = (ActivityInfo[]) ArrayUtils.trimToSize(res2, num);
                }
            }
            if ((i & 4) != 0) {
                N2 = packageR.services.size();
                if (N2 > 0) {
                    ServiceInfo[] res3 = new ServiceInfo[N2];
                    num = 0;
                    i3 = 0;
                    while (i3 < N2) {
                        Service s = (Service) packageR.services.get(i3);
                        int N5 = N2;
                        if (packageUserState.isMatch(s.info, i) != 0) {
                            N2 = num + 1;
                            res3[num] = generateServiceInfo(s, i, packageUserState, i2);
                            num = N2;
                        }
                        i3++;
                        N2 = N5;
                    }
                    pi.services = (ServiceInfo[]) ArrayUtils.trimToSize(res3, num);
                }
            }
            if ((i & 8) != 0) {
                N2 = packageR.providers.size();
                if (N2 > 0) {
                    ProviderInfo[] res4 = new ProviderInfo[N2];
                    num = 0;
                    i3 = 0;
                    while (i3 < N2) {
                        Provider pr = (Provider) packageR.providers.get(i3);
                        int N6 = N2;
                        if (packageUserState.isMatch(pr.info, i) != 0) {
                            N2 = num + 1;
                            res4[num] = generateProviderInfo(pr, i, packageUserState, i2);
                            num = N2;
                        }
                        i3++;
                        N2 = N6;
                    }
                    pi.providers = (ProviderInfo[]) ArrayUtils.trimToSize(res4, num);
                }
            }
            if ((i & 16) != 0) {
                N2 = packageR.instrumentation.size();
                if (N2 > 0) {
                    pi.instrumentation = new InstrumentationInfo[N2];
                    for (i3 = 0; i3 < N2; i3++) {
                        pi.instrumentation[i3] = generateInstrumentationInfo((Instrumentation) packageR.instrumentation.get(i3), i);
                    }
                }
            }
            if ((i & 4096) != 0) {
                N2 = packageR.permissions.size();
                if (N2 > 0) {
                    pi.permissions = new PermissionInfo[N2];
                    for (i3 = 0; i3 < N2; i3++) {
                        pi.permissions[i3] = generatePermissionInfo((Permission) packageR.permissions.get(i3), i);
                    }
                }
                N2 = packageR.requestedPermissions.size();
                if (N2 > 0) {
                    pi.requestedPermissions = new String[N2];
                    pi.requestedPermissionsFlags = new int[N2];
                    for (i3 = 0; i3 < N2; i3++) {
                        String perm = (String) packageR.requestedPermissions.get(i3);
                        pi.requestedPermissions[i3] = perm;
                        int[] iArr2 = pi.requestedPermissionsFlags;
                        iArr2[i3] = iArr2[i3] | 1;
                        if (set != null && set.contains(perm)) {
                            iArr2 = pi.requestedPermissionsFlags;
                            iArr2[i3] = iArr2[i3] | 2;
                        }
                    }
                }
            }
            if ((i & 64) != 0) {
                if (packageR.mSigningDetails.hasPastSigningCertificates()) {
                    pi.signatures = new Signature[1];
                    pi.signatures[0] = packageR.mSigningDetails.pastSigningCertificates[0];
                } else if (packageR.mRealSigningDetails.hasSignatures()) {
                    N2 = packageR.mRealSigningDetails.signatures.length;
                    pi.signatures = new Signature[N2];
                    System.arraycopy(packageR.mRealSigningDetails.signatures, 0, pi.signatures, 0, N2);
                } else if (packageR.mSigningDetails.hasSignatures()) {
                    N2 = packageR.mSigningDetails.signatures.length;
                    pi.signatures = new Signature[N2];
                    System.arraycopy(packageR.mSigningDetails.signatures, 0, pi.signatures, 0, N2);
                }
            }
            if ((134217728 & i) != 0) {
                if (packageR.mRealSigningDetails.hasSignatures()) {
                    pi.signingInfo = new SigningInfo(packageR.mRealSigningDetails);
                } else if (packageR.mSigningDetails != SigningDetails.UNKNOWN) {
                    pi.signingInfo = new SigningInfo(packageR.mSigningDetails);
                } else {
                    pi.signingInfo = null;
                }
            }
            return pi;
        }
        iArr = gids;
        long j = firstInstallTime;
        long j2 = lastUpdateTime;
        return null;
    }

    public static PackageLite parsePackageLite(File packageFile, int flags) throws PackageParserException {
        if (packageFile.isDirectory()) {
            return parseClusterPackageLite(packageFile, flags);
        }
        return parseMonolithicPackageLite(packageFile, flags);
    }

    private static PackageLite parseMonolithicPackageLite(File packageFile, int flags) throws PackageParserException {
        Trace.traceBegin(262144, "parseApkLite");
        ApkLite baseApk = parseApkLite(packageFile, flags);
        String packagePath = packageFile.getAbsolutePath();
        Trace.traceEnd(262144);
        return new PackageLite(packagePath, baseApk, null, null, null, null, null, null);
    }

    static PackageLite parseClusterPackageLite(File packageDir, int flags) throws PackageParserException {
        File[] files = packageDir.listFiles();
        int i;
        if (ArrayUtils.isEmpty(files)) {
            File file = packageDir;
            i = flags;
            throw new PackageParserException(-100, "No packages found in split");
        }
        Trace.traceBegin(262144, "parseApkLite");
        ArrayMap<String, ApkLite> apks = new ArrayMap();
        int i2 = 0;
        int versionCode = 0;
        String packageName = null;
        for (File file2 : files) {
            if (isApkFile(file2)) {
                StringBuilder stringBuilder;
                ApkLite lite = parseApkLite(file2, flags);
                HwFrameworkFactory.getHwPackageParser().needStopApp(lite.packageName, file2);
                if (packageName == null) {
                    packageName = lite.packageName;
                    versionCode = lite.versionCode;
                } else if (!packageName.equals(lite.packageName)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Inconsistent package ");
                    stringBuilder.append(lite.packageName);
                    stringBuilder.append(" in ");
                    stringBuilder.append(file2);
                    stringBuilder.append("; expected ");
                    stringBuilder.append(packageName);
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, stringBuilder.toString());
                } else if (versionCode != lite.versionCode) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Inconsistent version ");
                    stringBuilder.append(lite.versionCode);
                    stringBuilder.append(" in ");
                    stringBuilder.append(file2);
                    stringBuilder.append("; expected ");
                    stringBuilder.append(versionCode);
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, stringBuilder.toString());
                }
                if (apks.put(lite.splitName, lite) != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Split name ");
                    stringBuilder.append(lite.splitName);
                    stringBuilder.append(" defined more than once; most recent was ");
                    stringBuilder.append(file2);
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, stringBuilder.toString());
                }
            } else {
                i = flags;
            }
        }
        i = flags;
        Trace.traceEnd(262144);
        ApkLite baseApk = (ApkLite) apks.remove(null);
        if (baseApk != null) {
            int size = apks.size();
            String[] splitNames = null;
            boolean[] isFeatureSplits = null;
            String[] usesSplitNames = null;
            String[] configForSplits = null;
            String[] splitCodePaths = null;
            int[] splitRevisionCodes = null;
            if (size > 0) {
                isFeatureSplits = new boolean[size];
                usesSplitNames = new String[size];
                configForSplits = new String[size];
                splitCodePaths = new String[size];
                splitRevisionCodes = new int[size];
                splitNames = (String[]) apks.keySet().toArray(new String[size]);
                Arrays.sort(splitNames, sSplitNameComparator);
                while (i2 < size) {
                    ApkLite apk = (ApkLite) apks.get(splitNames[i2]);
                    usesSplitNames[i2] = apk.usesSplitName;
                    isFeatureSplits[i2] = apk.isFeatureSplit;
                    configForSplits[i2] = apk.configForSplit;
                    splitCodePaths[i2] = apk.codePath;
                    splitRevisionCodes[i2] = apk.revisionCode;
                    i2++;
                }
            }
            return new PackageLite(packageDir.getAbsolutePath(), baseApk, splitNames, isFeatureSplits, usesSplitNames, configForSplits, splitCodePaths, splitRevisionCodes);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Missing base APK in ");
        stringBuilder2.append(packageDir);
        throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, stringBuilder2.toString());
    }

    public Package parsePackage(File packageFile, int flags, boolean useCaches, int hwFlags) throws PackageParserException {
        Package parsed = useCaches ? getCachedResult(packageFile, flags) : null;
        if (parsed != null) {
            return parsed;
        }
        long j = 0;
        long parseTime = LOG_PARSE_TIMINGS ? SystemClock.uptimeMillis() : 0;
        if (packageFile.isDirectory()) {
            parsed = parseClusterPackage(packageFile, flags, hwFlags);
        } else {
            parsed = parseMonolithicPackage(packageFile, flags, hwFlags);
        }
        if (LOG_PARSE_TIMINGS) {
            j = SystemClock.uptimeMillis();
        }
        long cacheTime = j;
        cacheResult(packageFile, flags, parsed);
        if (LOG_PARSE_TIMINGS) {
            parseTime = cacheTime - parseTime;
            cacheTime = SystemClock.uptimeMillis() - cacheTime;
            if (parseTime + cacheTime > 100) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Parse times for '");
                stringBuilder.append(packageFile);
                stringBuilder.append("': parse=");
                stringBuilder.append(parseTime);
                stringBuilder.append("ms, update_cache=");
                stringBuilder.append(cacheTime);
                stringBuilder.append(" ms");
                Slog.i(str, stringBuilder.toString());
            }
        }
        return parsed;
    }

    public Package parsePackage(File packageFile, int flags) throws PackageParserException {
        return parsePackage(packageFile, flags, false, 0);
    }

    public Package parsePackage(File packageFile, int flags, boolean useCaches) throws PackageParserException {
        return parsePackage(packageFile, flags, useCaches, 0);
    }

    private String getCacheKey(File packageFile, int flags) {
        StringBuilder sb = new StringBuilder(packageFile.getName());
        sb.append('-');
        sb.append(flags);
        return sb.toString();
    }

    @VisibleForTesting
    protected Package fromCacheEntry(byte[] bytes) {
        return fromCacheEntryStatic(bytes);
    }

    @VisibleForTesting
    public static Package fromCacheEntryStatic(byte[] bytes) {
        Parcel p = Parcel.obtain();
        p.unmarshall(bytes, 0, bytes.length);
        p.setDataPosition(0);
        new ReadHelper(p).startAndInstall();
        Package pkg = new Package(p);
        p.recycle();
        sCachedPackageReadCount.incrementAndGet();
        return pkg;
    }

    @VisibleForTesting
    protected byte[] toCacheEntry(Package pkg) {
        return toCacheEntryStatic(pkg);
    }

    @VisibleForTesting
    public static byte[] toCacheEntryStatic(Package pkg) {
        Parcel p = Parcel.obtain();
        WriteHelper helper = new WriteHelper(p);
        pkg.writeToParcel(p, 0);
        helper.finishAndUninstall();
        byte[] serialized = p.marshall();
        p.recycle();
        return serialized;
    }

    private static boolean isCacheUpToDate(File packageFile, File cacheFile) {
        boolean z = false;
        try {
            if (Os.stat(packageFile.getAbsolutePath()).st_mtime < Os.stat(cacheFile.getAbsolutePath()).st_mtime) {
                z = true;
            }
            return z;
        } catch (ErrnoException ee) {
            if (ee.errno != OsConstants.ENOENT) {
                Slog.w("Error while stating package cache : ", ee);
            }
            return false;
        }
    }

    private Package getCachedResult(File packageFile, int flags) {
        if (this.mCacheDir == null) {
            return null;
        }
        File cacheFile = new File(this.mCacheDir, getCacheKey(packageFile, flags));
        try {
            if (!isCacheUpToDate(packageFile, cacheFile)) {
                return null;
            }
            Package p = fromCacheEntry(IoUtils.readFileAsByteArray(cacheFile.getAbsolutePath()));
            if (this.mCallback != null) {
                String[] overlayApks = this.mCallback.getOverlayApks(p.packageName);
                if (overlayApks != null && overlayApks.length > 0) {
                    for (String overlayApk : overlayApks) {
                        if (!isCacheUpToDate(new File(overlayApk), cacheFile)) {
                            return null;
                        }
                    }
                }
            }
            return p;
        } catch (Throwable e) {
            Slog.w(TAG, "Error reading package cache: ", e);
            cacheFile.delete();
            return null;
        }
    }

    private void cacheResult(File packageFile, int flags, Package parsed) {
        if (this.mCacheDir != null) {
            try {
                File cacheFile = new File(this.mCacheDir, getCacheKey(packageFile, flags));
                if (cacheFile.exists() && !cacheFile.delete()) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to delete cache file: ");
                    stringBuilder.append(cacheFile);
                    Slog.e(str, stringBuilder.toString());
                }
                byte[] cacheEntry = toCacheEntry(parsed);
                if (cacheEntry != null) {
                    FileOutputStream fos;
                    try {
                        fos = new FileOutputStream(cacheFile);
                        fos.write(cacheEntry);
                        fos.close();
                    } catch (IOException ioe) {
                        Slog.w(TAG, "Error writing cache entry.", ioe);
                        cacheFile.delete();
                    } catch (Throwable th) {
                        r4.addSuppressed(th);
                    }
                }
            } catch (Throwable e) {
                Slog.w(TAG, "Error saving package cache.", e);
            }
        }
    }

    private Package parseClusterPackage(File packageDir, int flags) throws PackageParserException {
        return parseClusterPackage(packageDir, flags, 0);
    }

    private Package parseClusterPackage(File packageDir, int flags, int hwFlags) throws PackageParserException {
        int i = 0;
        PackageLite lite = parseClusterPackageLite(packageDir, 0);
        if (!this.mOnlyCoreApps || lite.coreApp) {
            SplitAssetLoader assetLoader;
            SparseArray<int[]> splitDependencies = null;
            if (!lite.isolatedSplits || ArrayUtils.isEmpty(lite.splitNames)) {
                assetLoader = new DefaultSplitAssetLoader(lite, flags);
            } else {
                try {
                    splitDependencies = SplitDependencyLoader.createDependenciesFromPackage(lite);
                    assetLoader = new SplitAssetDependencyLoader(lite, splitDependencies, flags);
                } catch (IllegalDependencyException e) {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, e.getMessage());
                }
            }
            try {
                AssetManager assets = assetLoader.getBaseAssetManager();
                File baseApk = new File(lite.baseCodePath);
                Package pkg = parseBaseApk(baseApk, assets, flags, hwFlags);
                if (pkg != null) {
                    if (!ArrayUtils.isEmpty(lite.splitNames)) {
                        int num = lite.splitNames.length;
                        pkg.splitNames = lite.splitNames;
                        pkg.splitCodePaths = lite.splitCodePaths;
                        pkg.splitRevisionCodes = lite.splitRevisionCodes;
                        pkg.splitFlags = new int[num];
                        pkg.splitPrivateFlags = new int[num];
                        pkg.applicationInfo.splitNames = pkg.splitNames;
                        pkg.applicationInfo.splitDependencies = splitDependencies;
                        pkg.applicationInfo.splitClassLoaderNames = new String[num];
                        while (i < num) {
                            parseSplitApk(pkg, i, assetLoader.getSplitAssetManager(i), flags);
                            i++;
                        }
                    }
                    pkg.setCodePath(packageDir.getCanonicalPath());
                    pkg.setUse32bitAbi(lite.use32bitAbi);
                    IoUtils.closeQuietly(assetLoader);
                    return pkg;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to parse base APK: ");
                stringBuilder.append(baseApk);
                throw new PackageParserException(-100, stringBuilder.toString());
            } catch (IOException e2) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to get path: ");
                stringBuilder2.append(lite.baseCodePath);
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, stringBuilder2.toString(), e2);
            } catch (Throwable th) {
                IoUtils.closeQuietly(assetLoader);
            }
        } else {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Not a coreApp: ");
            stringBuilder3.append(packageDir);
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, stringBuilder3.toString());
        }
    }

    @Deprecated
    public Package parseMonolithicPackage(File apkFile, int flags) throws PackageParserException {
        return parseMonolithicPackage(apkFile, flags, 0);
    }

    public Package parseMonolithicPackage(File apkFile, int flags, int hwFlags) throws PackageParserException {
        PackageLite lite = parseMonolithicPackageLite(apkFile, flags);
        if (this.mOnlyCoreApps) {
            if (lite.coreApp) {
                HwFrameworkFactory.getHwPackageParser().needStopApp(lite.packageName, apkFile);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Not a coreApp: ");
                stringBuilder.append(apkFile);
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, stringBuilder.toString());
            }
        }
        SplitAssetLoader assetLoader = new DefaultSplitAssetLoader(lite, flags);
        try {
            Package pkg = parseBaseApk(apkFile, assetLoader.getBaseAssetManager(), flags, hwFlags);
            HwFrameworkFactory.getHwPackageParser().needStopApp(pkg.packageName, apkFile);
            pkg.setCodePath(apkFile.getCanonicalPath());
            pkg.setUse32bitAbi(lite.use32bitAbi);
            IoUtils.closeQuietly(assetLoader);
            return pkg;
        } catch (IOException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to get path: ");
            stringBuilder2.append(apkFile);
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, stringBuilder2.toString(), e);
        } catch (Throwable th) {
            IoUtils.closeQuietly(assetLoader);
        }
    }

    private Package parseBaseApk(File apkFile, AssetManager assets, int flags) throws PackageParserException {
        return parseBaseApk(apkFile, assets, flags, 0);
    }

    private Package parseBaseApk(File apkFile, AssetManager assets, int flags, int hwFlags) throws PackageParserException {
        PackageParserException e;
        Exception e2;
        StringBuilder stringBuilder;
        Throwable th;
        AssetManager assetManager = assets;
        String apkPath = apkFile.getAbsolutePath();
        String volumeUuid = null;
        if (apkPath.startsWith(MNT_EXPAND)) {
            volumeUuid = apkPath.substring(MNT_EXPAND.length(), apkPath.indexOf(47, MNT_EXPAND.length()));
        }
        String volumeUuid2 = volumeUuid;
        this.mParseError = 1;
        this.mArchiveSourcePath = apkFile.getAbsolutePath();
        XmlResourceParser parser = null;
        XmlResourceParser parser2;
        try {
            int cookie = assetManager.findCookieForPath(apkPath);
            if (cookie != 0) {
                parser2 = assetManager.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
                try {
                    String[] outError = new String[1];
                    Package pkg = parseBaseApk(apkPath, new Resources(assetManager, this.mMetrics, null), parser2, flags, outError, hwFlags);
                    if (pkg != null) {
                        pkg.setVolumeUuid(volumeUuid2);
                        pkg.setApplicationVolumeUuid(volumeUuid2);
                        pkg.setBaseCodePath(apkPath);
                        pkg.setSigningDetails(SigningDetails.UNKNOWN);
                        IoUtils.closeQuietly(parser2);
                        return pkg;
                    }
                    int i = this.mParseError;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(apkPath);
                    stringBuilder2.append(" (at ");
                    stringBuilder2.append(parser2.getPositionDescription());
                    stringBuilder2.append("): ");
                    stringBuilder2.append(outError[0]);
                    throw new PackageParserException(i, stringBuilder2.toString());
                } catch (PackageParserException e3) {
                    e = e3;
                    parser = parser2;
                    throw e;
                } catch (Exception e4) {
                    e2 = e4;
                    parser = parser2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to read manifest from ");
                    stringBuilder.append(apkPath);
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, stringBuilder.toString(), e2);
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(parser2);
                    throw th;
                }
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Failed adding asset path: ");
            stringBuilder3.append(apkPath);
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, stringBuilder3.toString());
        } catch (PackageParserException e5) {
            e = e5;
            throw e;
        } catch (Exception e6) {
            e2 = e6;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to read manifest from ");
            stringBuilder.append(apkPath);
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, stringBuilder.toString(), e2);
        } catch (Throwable th3) {
            th = th3;
            parser2 = parser;
            IoUtils.closeQuietly(parser2);
            throw th;
        }
    }

    private void parseSplitApk(Package pkg, int splitIndex, AssetManager assets, int flags) throws PackageParserException {
        PackageParserException e;
        Exception e2;
        StringBuilder stringBuilder;
        Throwable th;
        AssetManager assetManager = assets;
        Package packageR = pkg;
        String apkPath = packageR.splitCodePaths[splitIndex];
        this.mParseError = 1;
        this.mArchiveSourcePath = apkPath;
        XmlResourceParser parser = null;
        try {
            int cookie = assetManager.findCookieForPath(apkPath);
            if (cookie != 0) {
                XmlResourceParser parser2 = assetManager.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
                try {
                    String[] outError = new String[1];
                    Package pkg2 = parseSplitApk(packageR, new Resources(assetManager, this.mMetrics, null), parser2, flags, splitIndex, outError);
                    if (pkg2 != null) {
                        IoUtils.closeQuietly(parser2);
                        return;
                    }
                    try {
                        int i = this.mParseError;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(apkPath);
                        stringBuilder2.append(" (at ");
                        stringBuilder2.append(parser2.getPositionDescription());
                        stringBuilder2.append("): ");
                        stringBuilder2.append(outError[0]);
                        throw new PackageParserException(i, stringBuilder2.toString());
                    } catch (PackageParserException e3) {
                        e = e3;
                        throw e;
                    } catch (Exception e4) {
                        e2 = e4;
                        packageR = pkg2;
                        parser = parser2;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to read manifest from ");
                        stringBuilder.append(apkPath);
                        throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, stringBuilder.toString(), e2);
                    } catch (Throwable th2) {
                        th = th2;
                        packageR = pkg2;
                        parser = parser2;
                        IoUtils.closeQuietly(parser);
                        throw th;
                    }
                } catch (PackageParserException e5) {
                    e = e5;
                    throw e;
                } catch (Exception e6) {
                    e2 = e6;
                    parser = parser2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to read manifest from ");
                    stringBuilder.append(apkPath);
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, stringBuilder.toString(), e2);
                } catch (Throwable th3) {
                    th = th3;
                    parser = parser2;
                    IoUtils.closeQuietly(parser);
                    throw th;
                }
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Failed adding asset path: ");
            stringBuilder3.append(apkPath);
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, stringBuilder3.toString());
        } catch (PackageParserException e7) {
            e = e7;
            throw e;
        } catch (Exception e8) {
            e2 = e8;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to read manifest from ");
            stringBuilder.append(apkPath);
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, stringBuilder.toString(), e2);
        } catch (Throwable th4) {
            th = th4;
            IoUtils.closeQuietly(parser);
            throw th;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:21:0x007b  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Package parseSplitApk(Package pkg, Resources res, XmlResourceParser parser, int flags, int splitIndex, String[] outError) throws XmlPullParserException, IOException, PackageParserException {
        parsePackageSplitNames(parser, parser);
        this.mParseInstrumentationArgs = null;
        boolean foundApp = false;
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                if (!foundApp) {
                    outError[0] = "<manifest> does not contain an <application>";
                    this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_EMPTY;
                }
            } else if (type != 3) {
                if (type != 4) {
                    if (!parser.getName().equals(TAG_APPLICATION)) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown element under <manifest>: ");
                        stringBuilder.append(parser.getName());
                        stringBuilder.append(" at ");
                        stringBuilder.append(this.mArchiveSourcePath);
                        stringBuilder.append(" ");
                        stringBuilder.append(parser.getPositionDescription());
                        Slog.w(str, stringBuilder.toString());
                        XmlUtils.skipCurrentTag(parser);
                    } else if (foundApp) {
                        Slog.w(TAG, "<manifest> has more than one <application>");
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        foundApp = true;
                        if (!parseSplitApplication(pkg, res, parser, flags, splitIndex, outError)) {
                            return null;
                        }
                    }
                }
            }
        }
        if (foundApp) {
        }
        return pkg;
    }

    public static ArraySet<PublicKey> toSigningKeys(Signature[] signatures) throws CertificateException {
        ArraySet<PublicKey> keys = new ArraySet(signatures.length);
        for (Signature publicKey : signatures) {
            keys.add(publicKey.getPublicKey());
        }
        return keys;
    }

    public static void collectCertificates(Package pkg, boolean skipVerify) throws PackageParserException {
        collectCertificatesInternal(pkg, skipVerify);
        int i = 0;
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        while (i < childCount) {
            ((Package) pkg.childPackages.get(i)).mSigningDetails = pkg.mSigningDetails;
            i++;
        }
    }

    private static void collectCertificatesInternal(Package pkg, boolean skipVerify) throws PackageParserException {
        pkg.mSigningDetails = SigningDetails.UNKNOWN;
        Trace.traceBegin(262144, "collectCertificates");
        try {
            collectCertificates(pkg, new File(pkg.baseCodePath), skipVerify);
            if (!ArrayUtils.isEmpty(pkg.splitCodePaths)) {
                for (String file : pkg.splitCodePaths) {
                    collectCertificates(pkg, new File(file), skipVerify);
                }
            }
            Trace.traceEnd(262144);
        } catch (Throwable th) {
            Trace.traceEnd(262144);
        }
    }

    private static void collectCertificates(Package pkg, File apkFile, boolean skipVerify) throws PackageParserException {
        SigningDetails verified;
        String apkPath = apkFile.getAbsolutePath();
        int minSignatureScheme = 1;
        if (pkg.applicationInfo.isStaticSharedLibrary()) {
            minSignatureScheme = 2;
        }
        if (skipVerify) {
            verified = ApkSignatureVerifier.plsCertsNoVerifyOnlyCerts(apkPath, minSignatureScheme);
        } else {
            verified = ApkSignatureVerifier.verify(apkPath, minSignatureScheme);
        }
        if (pkg.mSigningDetails == SigningDetails.UNKNOWN) {
            pkg.mSigningDetails = verified;
        } else if (!Signature.areExactMatch(pkg.mSigningDetails.signatures, verified.signatures)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(apkPath);
            stringBuilder.append(" has mismatched certificates");
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES, stringBuilder.toString());
        }
    }

    private static AssetManager newConfiguredAssetManager() {
        AssetManager assetManager = new AssetManager();
        assetManager.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, VERSION.RESOURCES_SDK_INT);
        return assetManager;
    }

    public static ApkLite parseApkLite(File apkFile, int flags) throws PackageParserException {
        return parseApkLiteInner(apkFile, null, null, flags);
    }

    public static ApkLite parseApkLite(FileDescriptor fd, String debugPathName, int flags) throws PackageParserException {
        return parseApkLiteInner(null, fd, debugPathName, flags);
    }

    /* JADX WARNING: Removed duplicated region for block: B:9:0x0015 A:{PHI: r2 , Catch:{ all -> 0x004b, IOException -> 0x0017, RuntimeException | XmlPullParserException -> 0x0015, RuntimeException | XmlPullParserException -> 0x0015, RuntimeException | XmlPullParserException -> 0x0015, all -> 0x0012 }, ExcHandler: RuntimeException | XmlPullParserException (r1_2 'e' java.lang.Exception A:{Catch:{  }}), Splitter:B:14:0x001f} */
    /* JADX WARNING: Removed duplicated region for block: B:9:0x0015 A:{PHI: r2 , Catch:{ all -> 0x004b, IOException -> 0x0017, RuntimeException | XmlPullParserException -> 0x0015, RuntimeException | XmlPullParserException -> 0x0015, RuntimeException | XmlPullParserException -> 0x0015, all -> 0x0012 }, ExcHandler: RuntimeException | XmlPullParserException (r1_2 'e' java.lang.Exception A:{Catch:{  }}), Splitter:B:14:0x001f} */
    /* JADX WARNING: Missing block: B:9:0x0015, code skipped:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:36:?, code skipped:
            r3 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("Failed to parse ");
            r4.append(r0);
            android.util.Slog.w(r3, r4.toString(), r1);
            r5 = new java.lang.StringBuilder();
            r5.append("Failed to parse ");
            r5.append(r0);
     */
    /* JADX WARNING: Missing block: B:37:0x00a4, code skipped:
            throw new android.content.pm.PackageParser.PackageParserException(android.content.pm.PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, r5.toString(), r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static ApkLite parseApkLiteInner(File apkFile, FileDescriptor fd, String debugPathName, int flags) throws PackageParserException {
        ApkAssets apkAssets;
        Exception e;
        String apkPath = fd != null ? debugPathName : apkFile.getAbsolutePath();
        XmlResourceParser parser = null;
        boolean skipVerify = false;
        if (fd != null) {
            try {
                apkAssets = ApkAssets.loadFromFd(fd, debugPathName, false, false);
            } catch (IOException e2) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to parse ");
                stringBuilder.append(apkPath);
                throw new PackageParserException(-100, stringBuilder.toString());
            } catch (RuntimeException | XmlPullParserException e3) {
            } catch (Throwable th) {
                IoUtils.closeQuietly(parser);
            }
        } else {
            apkAssets = ApkAssets.loadFromPath(apkPath);
        }
        parser = apkAssets.openXml(ANDROID_MANIFEST_FILENAME);
        if ((flags & 32) != 0) {
            Package tempPkg = new Package((String) null);
            if ((flags & 16) != 0) {
                skipVerify = true;
            }
            Trace.traceBegin(262144, "collectCertificates");
            collectCertificates(tempPkg, apkFile, skipVerify);
            Trace.traceEnd(262144);
            e = tempPkg.mSigningDetails;
        } else {
            e = SigningDetails.UNKNOWN;
        }
        ApkLite parseApkLite = parseApkLite(apkPath, parser, parser, e);
        IoUtils.closeQuietly(parser);
        return parseApkLite;
    }

    private static String validateName(String name, boolean requireSeparator, boolean requireFilename) {
        int N = name.length();
        boolean hasSep = false;
        boolean front = true;
        for (int i = 0; i < N; i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                front = false;
            } else if (front || ((c < '0' || c > '9') && c != '_')) {
                if (c == '.') {
                    hasSep = true;
                    front = true;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("bad character '");
                    stringBuilder.append(c);
                    stringBuilder.append("'");
                    return stringBuilder.toString();
                }
            }
        }
        if (requireFilename && !FileUtils.isValidExtFilename(name)) {
            return "Invalid filename";
        }
        String str = (hasSep || !requireSeparator) ? null : "must have at least one '.' separator";
        return str;
    }

    private static Pair<String, String> parsePackageSplitNames(XmlPullParser parser, AttributeSet attrs) throws IOException, XmlPullParserException, PackageParserException {
        int type;
        while (true) {
            int next = parser.next();
            type = next;
            if (next == 2 || type == 1) {
            }
        }
        if (type != 2) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "No start tag found");
        } else if (parser.getName().equals(TAG_MANIFEST)) {
            String error;
            Object intern;
            String packageName = attrs.getAttributeValue(null, "package");
            if (!("android".equals(packageName) || "androidhwext".equals(packageName) || "featurelayerwidget".equals(packageName))) {
                error = validateName(packageName, true, true);
                if (error != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid manifest package: ");
                    stringBuilder.append(error);
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME, stringBuilder.toString());
                }
            }
            String splitName = attrs.getAttributeValue(null, "split");
            if (splitName != null) {
                if (splitName.length() == 0) {
                    splitName = null;
                } else {
                    error = validateName(splitName, false, false);
                    if (error != null) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Invalid manifest split: ");
                        stringBuilder2.append(error);
                        throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME, stringBuilder2.toString());
                    }
                }
            }
            error = packageName.intern();
            if (splitName != null) {
                intern = splitName.intern();
            } else {
                intern = splitName;
            }
            return Pair.create(error, intern);
        } else {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "No <manifest> tag");
        }
    }

    private static ApkLite parseApkLite(String codePath, XmlPullParser parser, AttributeSet attrs, SigningDetails signingDetails) throws IOException, XmlPullParserException, PackageParserException {
        boolean debuggable;
        boolean isolatedSplits;
        AttributeSet attributeSet = attrs;
        Pair<String, String> packageSplit = parsePackageSplitNames(parser, attrs);
        boolean debuggable2 = false;
        boolean multiArch = false;
        boolean use32bitAbi = false;
        boolean extractNativeLibs = true;
        String configForSplit = null;
        String usesSplitName = null;
        boolean isFeatureSplit = false;
        boolean isolatedSplits2 = false;
        boolean coreApp = false;
        int revisionCode = 0;
        int versionCodeMajor = 0;
        int versionCode = 0;
        int installLocation = -1;
        int i = 0;
        while (i < attrs.getAttributeCount()) {
            String attr = attributeSet.getAttributeName(i);
            debuggable = debuggable2;
            if (attr.equals("installLocation")) {
                installLocation = attributeSet.getAttributeIntValue(i, true);
            } else if (attr.equals(HwFrameworkMonitor.KEY_VERSION_CODE)) {
                versionCode = attributeSet.getAttributeIntValue(i, false);
            } else if (attr.equals("versionCodeMajor")) {
                versionCodeMajor = attributeSet.getAttributeIntValue(i, false);
            } else if (attr.equals("revisionCode")) {
                revisionCode = attributeSet.getAttributeIntValue(i, false);
            } else if (attr.equals("coreApp")) {
                coreApp = attributeSet.getAttributeBooleanValue(i, false);
            } else if (attr.equals("isolatedSplits")) {
                isolatedSplits2 = attributeSet.getAttributeBooleanValue(i, false);
            } else if (attr.equals("configForSplit")) {
                configForSplit = attributeSet.getAttributeValue(i);
            } else if (attr.equals("isFeatureSplit")) {
                isFeatureSplit = attributeSet.getAttributeBooleanValue(i, false);
            }
            i++;
            debuggable2 = debuggable;
        }
        debuggable = debuggable2;
        int type = 1;
        i = parser.getDepth() + 1;
        List<VerifierInfo> verifiers = new ArrayList();
        while (true) {
            isolatedSplits = isolatedSplits2;
            int next = parser.next();
            int type2 = next;
            int i2;
            if (next == type) {
                i2 = type2;
                break;
            }
            type = type2;
            int i3;
            if (type == 3 && parser.getDepth() < i) {
                i3 = i;
                i2 = type;
                break;
            }
            if (type != 3) {
                if (type != 4 && parser.getDepth() == i) {
                    i3 = i;
                    if (TAG_PACKAGE_VERIFIER.equals(parser.getName()) != 0) {
                        i = parseVerifier(attrs);
                        if (i != 0) {
                            verifiers.add(i);
                        }
                    } else if (TAG_APPLICATION.equals(parser.getName()) != 0) {
                        i = 0;
                        while (i < attrs.getAttributeCount()) {
                            String attr2 = attributeSet.getAttributeName(i);
                            i2 = type;
                            if ("debuggable".equals(attr2)) {
                                debuggable = attributeSet.getAttributeBooleanValue(i, false);
                            }
                            if ("multiArch".equals(attr2)) {
                                multiArch = attributeSet.getAttributeBooleanValue(i, false);
                            }
                            if ("use32bitAbi".equals(attr2)) {
                                use32bitAbi = attributeSet.getAttributeBooleanValue(i, false);
                            }
                            if ("extractNativeLibs".equals(attr2)) {
                                extractNativeLibs = attributeSet.getAttributeBooleanValue(i, true);
                            }
                            i++;
                            type = i2;
                        }
                        isolatedSplits2 = isolatedSplits;
                        i = i3;
                        type = 1;
                    } else {
                        type = 1;
                        if (TAG_USES_SPLIT.equals(parser.getName()) != 0) {
                            if (usesSplitName != null) {
                                Slog.w(TAG, "Only one <uses-split> permitted. Ignoring others.");
                            } else {
                                usesSplitName = attributeSet.getAttributeValue(ANDROID_RESOURCES, MidiDeviceInfo.PROPERTY_NAME);
                                if (usesSplitName == null) {
                                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "<uses-split> tag requires 'android:name' attribute");
                                }
                            }
                        }
                    }
                }
                i3 = i;
                type = 1;
            } else {
                i3 = i;
                type = 1;
            }
            isolatedSplits2 = isolatedSplits;
            i = i3;
        }
        return new ApkLite(codePath, (String) packageSplit.first, (String) packageSplit.second, isFeatureSplit, configForSplit, usesSplitName, versionCode, versionCodeMajor, revisionCode, installLocation, verifiers, signingDetails, coreApp, debuggable, multiArch, use32bitAbi, extractNativeLibs, isolatedSplits);
    }

    private boolean parseBaseApkChild(Package parentPkg, Resources res, XmlResourceParser parser, int flags, String[] outError) throws XmlPullParserException, IOException {
        Package packageR = parentPkg;
        XmlResourceParser xmlResourceParser = parser;
        String childPackageName = xmlResourceParser.getAttributeValue(null, "package");
        String message;
        if (validateName(childPackageName, true, false) != null) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
            return false;
        } else if (childPackageName.equals(packageR.packageName)) {
            message = new StringBuilder();
            message.append("Child package name cannot be equal to parent package name: ");
            message.append(packageR.packageName);
            message = message.toString();
            Slog.w(TAG, message);
            outError[0] = message;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        } else if (packageR.hasChildPackage(childPackageName)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Duplicate child package:");
            stringBuilder.append(childPackageName);
            message = stringBuilder.toString();
            Slog.w(TAG, message);
            outError[0] = message;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        } else {
            Package childPkg = new Package(childPackageName);
            childPkg.mVersionCode = packageR.mVersionCode;
            childPkg.baseRevisionCode = packageR.baseRevisionCode;
            childPkg.mVersionName = packageR.mVersionName;
            childPkg.applicationInfo.targetSdkVersion = packageR.applicationInfo.targetSdkVersion;
            childPkg.applicationInfo.minSdkVersion = packageR.applicationInfo.minSdkVersion;
            Package childPkg2 = parseBaseApkCommon(childPkg, CHILD_PACKAGE_TAGS, res, xmlResourceParser, flags, outError);
            if (childPkg2 == null) {
                return false;
            }
            if (packageR.childPackages == null) {
                packageR.childPackages = new ArrayList();
            }
            packageR.childPackages.add(childPkg2);
            childPkg2.parentPackage = packageR;
            return true;
        }
    }

    private Package parseBaseApk(String apkPath, Resources res, XmlResourceParser parser, int flags, String[] outError) throws XmlPullParserException, IOException {
        return parseBaseApk(apkPath, res, parser, flags, outError, 0);
    }

    private Package parseBaseApk(String apkPath, Resources res, XmlResourceParser parser, int flags, String[] outError, int hwFlags) throws XmlPullParserException, IOException {
        XmlResourceParser xmlResourceParser = parser;
        String str;
        Resources resources;
        try {
            Pair<String, String> packageSplit = parsePackageSplitNames(xmlResourceParser, xmlResourceParser);
            String pkgName = (String) packageSplit.first;
            String splitName = (String) packageSplit.second;
            if (TextUtils.isEmpty(splitName)) {
                if (this.mCallback != null) {
                    String[] overlayPaths = this.mCallback.getOverlayPaths(pkgName, apkPath);
                    if (overlayPaths != null && overlayPaths.length > 0) {
                        for (String overlayPath : overlayPaths) {
                            res.getAssets().addOverlayPath(overlayPath);
                        }
                    }
                } else {
                    str = apkPath;
                }
                Package pkg = new Package(pkgName);
                pkg.applicationInfo.hwFlags = hwFlags;
                resources = res;
                TypedArray sa = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifest);
                pkg.mVersionCode = sa.getInteger(1, 0);
                pkg.mVersionCodeMajor = sa.getInteger(11, 0);
                pkg.applicationInfo.setVersionCode(pkg.getLongVersionCode());
                pkg.baseRevisionCode = sa.getInteger(5, 0);
                pkg.mVersionName = sa.getNonConfigurationString(2, 0);
                if (pkg.mVersionName != null) {
                    pkg.mVersionName = pkg.mVersionName.intern();
                }
                pkg.coreApp = xmlResourceParser.getAttributeBooleanValue(null, "coreApp", false);
                pkg.mCompileSdkVersion = sa.getInteger(9, 0);
                pkg.applicationInfo.compileSdkVersion = pkg.mCompileSdkVersion;
                pkg.mCompileSdkVersionCodename = sa.getNonConfigurationString(10, 0);
                if (pkg.mCompileSdkVersionCodename != null) {
                    pkg.mCompileSdkVersionCodename = pkg.mCompileSdkVersionCodename.intern();
                }
                pkg.applicationInfo.compileSdkVersionCodename = pkg.mCompileSdkVersionCodename;
                sa.recycle();
                return parseBaseApkCommon(pkg, null, resources, xmlResourceParser, flags, outError);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Expected base APK, but found split ");
            stringBuilder.append(splitName);
            outError[0] = stringBuilder.toString();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
            return null;
        } catch (PackageParserException e) {
            str = apkPath;
            resources = res;
            int i = hwFlags;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:205:0x0583  */
    /* JADX WARNING: Removed duplicated region for block: B:381:0x057d A:{SYNTHETIC} */
    /* JADX WARNING: Missing block: B:85:0x02e0, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Package parseBaseApkCommon(Package pkg, Set<String> acceptedTags, Resources res, XmlResourceParser parser, int flags, String[] outError) throws XmlPullParserException, IOException {
        String nameError;
        StringBuilder stringBuilder;
        ApplicationInfo applicationInfo;
        int next;
        int supportsXLargeScreens;
        int supportsLargeScreens;
        int resizeable;
        int anyDensity;
        int supportsNormalScreens;
        String tagName;
        int i;
        int type;
        Package packageR = pkg;
        Set set = acceptedTags;
        Resources resources = res;
        XmlResourceParser xmlResourceParser = parser;
        int i2 = flags;
        String[] strArr = outError;
        this.mParseInstrumentationArgs = null;
        TypedArray sa = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifest);
        String str = sa.getNonConfigurationString(0, 0);
        int i3 = 3;
        if (str != null && str.length() > 0) {
            nameError = validateName(str, true, false);
            if (nameError == null || "android".equals(packageR.packageName) || "androidhwext".equals(packageR.packageName) || "featurelayerwidget".equals(packageR.packageName)) {
                packageR.mSharedUserId = str.intern();
                packageR.mSharedUserLabel = sa.getResourceId(3, 0);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("<manifest> specifies bad sharedUserId name \"");
                stringBuilder.append(str);
                stringBuilder.append("\": ");
                stringBuilder.append(nameError);
                strArr[0] = stringBuilder.toString();
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID;
                return null;
            }
        }
        packageR.installLocation = sa.getInteger(4, -1);
        packageR.applicationInfo.installLocation = packageR.installLocation;
        int targetSandboxVersion = sa.getInteger(7, 1);
        packageR.applicationInfo.targetSandboxVersion = targetSandboxVersion;
        if ((i2 & 4) != 0) {
            applicationInfo = packageR.applicationInfo;
            applicationInfo.privateFlags |= 4;
        }
        if ((i2 & 8) != 0) {
            applicationInfo = packageR.applicationInfo;
            applicationInfo.flags |= 262144;
        }
        if (sa.getBoolean(6, false)) {
            applicationInfo = packageR.applicationInfo;
            applicationInfo.privateFlags |= 32768;
        }
        int outerDepth = parser.getDepth();
        int resizeable2 = 1;
        int anyDensity2 = 1;
        boolean foundApp = false;
        int supportsXLargeScreens2 = 1;
        TypedArray sa2 = sa;
        int supportsLargeScreens2 = 1;
        int supportsSmallScreens = 1;
        int supportsNormalScreens2 = 1;
        while (true) {
            int outerDepth2 = outerDepth;
            next = parser.next();
            int type2 = next;
            int i4;
            String str2;
            int i5;
            int i6;
            if (next == 1) {
                supportsXLargeScreens = supportsXLargeScreens2;
                supportsLargeScreens = supportsLargeScreens2;
                next = supportsSmallScreens;
                i4 = targetSandboxVersion;
                str2 = str;
                resizeable = resizeable2;
                anyDensity = anyDensity2;
                i5 = outerDepth2;
                i6 = type2;
                supportsNormalScreens = supportsNormalScreens2;
                break;
            }
            int i7;
            Set<String> set2;
            next = type2;
            if (next == i3) {
                i3 = outerDepth2;
                if (parser.getDepth() <= i3) {
                    supportsXLargeScreens = supportsXLargeScreens2;
                    supportsLargeScreens = supportsLargeScreens2;
                    i5 = i3;
                    i4 = targetSandboxVersion;
                    i6 = next;
                    str2 = str;
                    resizeable = resizeable2;
                    anyDensity = anyDensity2;
                    supportsNormalScreens = supportsNormalScreens2;
                    next = supportsSmallScreens;
                    break;
                }
            }
            i3 = outerDepth2;
            if (next != 3) {
                if (next == 4) {
                    supportsXLargeScreens = supportsXLargeScreens2;
                    supportsLargeScreens = supportsLargeScreens2;
                    next = supportsSmallScreens;
                    i5 = i3;
                    i4 = targetSandboxVersion;
                    str2 = str;
                    resizeable = resizeable2;
                    anyDensity = anyDensity2;
                } else {
                    int supportsXLargeScreens3;
                    int supportsLargeScreens3;
                    i4 = targetSandboxVersion;
                    tagName = parser.getName();
                    if (set2 == null || set2.contains(tagName)) {
                        int supportsXLargeScreens4;
                        int supportsLargeScreens4;
                        supportsXLargeScreens3 = supportsXLargeScreens2;
                        supportsLargeScreens3 = supportsLargeScreens2;
                        StringBuilder stringBuilder2;
                        if (tagName.equals(TAG_APPLICATION) == 0) {
                            i = -12;
                            i5 = i3;
                            i6 = next;
                            str2 = str;
                            supportsXLargeScreens4 = supportsXLargeScreens3;
                            supportsLargeScreens4 = supportsLargeScreens3;
                            supportsNormalScreens = supportsNormalScreens2;
                            next = supportsSmallScreens;
                            String str3;
                            if (tagName.equals("overlay") != 0) {
                                supportsXLargeScreens2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestResourceOverlay);
                                packageR.mOverlayTarget = supportsXLargeScreens2.getString(1);
                                packageR.mOverlayCategory = supportsXLargeScreens2.getString(2);
                                packageR.mOverlayPriority = supportsXLargeScreens2.getInt(0, 0);
                                packageR.mOverlayIsStatic = supportsXLargeScreens2.getBoolean(3, false);
                                supportsNormalScreens2 = supportsXLargeScreens2.getString(4);
                                supportsLargeScreens2 = supportsXLargeScreens2.getString(5);
                                supportsXLargeScreens2.recycle();
                                if (packageR.mOverlayTarget == null) {
                                    strArr[0] = "<overlay> does not specify a target package";
                                    this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                    return null;
                                } else if (packageR.mOverlayPriority < 0 || packageR.mOverlayPriority > 9999) {
                                    strArr[0] = "<overlay> priority must be between 0 and 9999";
                                    this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                } else if (checkOverlayRequiredSystemProperty(supportsNormalScreens2, supportsLargeScreens2)) {
                                    XmlUtils.skipCurrentTag(parser);
                                    sa2 = supportsXLargeScreens2;
                                    i7 = 3;
                                } else {
                                    str3 = TAG;
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Skipping target and overlay pair ");
                                    stringBuilder3.append(packageR.mOverlayTarget);
                                    stringBuilder3.append(" and ");
                                    stringBuilder3.append(packageR.baseCodePath);
                                    stringBuilder3.append(": overlay ignored due to required system property: ");
                                    stringBuilder3.append(supportsNormalScreens2);
                                    stringBuilder3.append(" with value: ");
                                    stringBuilder3.append(supportsLargeScreens2);
                                    Slog.i(str3, stringBuilder3.toString());
                                    return null;
                                }
                            }
                            if (tagName.equals(TAG_KEY_SETS) == 0) {
                                if (tagName.equals(TAG_PERMISSION_GROUP) != 0) {
                                    i = 3;
                                    if (parsePermissionGroup(packageR, i2, resources, xmlResourceParser, strArr) == 0) {
                                        return 0;
                                    }
                                }
                                supportsSmallScreens = i;
                                i = 3;
                                if (tagName.equals("permission")) {
                                    if (!parsePermission(packageR, resources, xmlResourceParser, strArr)) {
                                        return null;
                                    }
                                } else if (tagName.equals(TAG_PERMISSION_TREE)) {
                                    if (!parsePermissionTree(packageR, resources, xmlResourceParser, strArr)) {
                                        return null;
                                    }
                                } else if (!tagName.equals(TAG_USES_PERMISSION)) {
                                    String str4;
                                    if (tagName.equals(TAG_USES_PERMISSION_SDK_M) != 0) {
                                        i7 = i;
                                        resizeable = resizeable2;
                                        anyDensity = anyDensity2;
                                        supportsXLargeScreens = supportsXLargeScreens4;
                                        supportsLargeScreens = supportsLargeScreens4;
                                    } else if (tagName.equals(TAG_USES_PERMISSION_SDK_23) != 0) {
                                        i7 = i;
                                        resizeable = resizeable2;
                                        anyDensity = anyDensity2;
                                        supportsXLargeScreens = supportsXLargeScreens4;
                                        supportsLargeScreens = supportsLargeScreens4;
                                    } else if (tagName.equals(TAG_USES_CONFIGURATION) != 0) {
                                        supportsXLargeScreens2 = new ConfigurationInfo();
                                        sa = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestUsesConfiguration);
                                        supportsXLargeScreens2.reqTouchScreen = sa.getInt(0, 0);
                                        supportsXLargeScreens2.reqKeyboardType = sa.getInt(1, 0);
                                        if (sa.getBoolean(2, false)) {
                                            supportsXLargeScreens2.reqInputFeatures |= 1;
                                        }
                                        supportsXLargeScreens2.reqNavigation = sa.getInt(i, 0);
                                        if (sa.getBoolean(4, false)) {
                                            supportsXLargeScreens2.reqInputFeatures = 2 | supportsXLargeScreens2.reqInputFeatures;
                                        }
                                        sa.recycle();
                                        packageR.configPreferences = ArrayUtils.add(packageR.configPreferences, supportsXLargeScreens2);
                                        XmlUtils.skipCurrentTag(parser);
                                        sa2 = sa;
                                        i7 = i;
                                    } else if (tagName.equals(TAG_USES_FEATURE) != 0) {
                                        supportsXLargeScreens2 = parseUsesFeature(resources, xmlResourceParser);
                                        packageR.reqFeatures = ArrayUtils.add(packageR.reqFeatures, supportsXLargeScreens2);
                                        if (supportsXLargeScreens2.name == null) {
                                            ConfigurationInfo cPref = new ConfigurationInfo();
                                            cPref.reqGlEsVersion = supportsXLargeScreens2.reqGlEsVersion;
                                            packageR.configPreferences = ArrayUtils.add(packageR.configPreferences, cPref);
                                        }
                                        XmlUtils.skipCurrentTag(parser);
                                    } else if (tagName.equals(TAG_FEATURE_GROUP) != 0) {
                                        supportsXLargeScreens2 = new FeatureGroupInfo();
                                        ArrayList<FeatureInfo> features = null;
                                        supportsNormalScreens2 = parser.getDepth();
                                        while (true) {
                                            supportsSmallScreens = parser.next();
                                            type = supportsSmallScreens;
                                            int i8;
                                            if (supportsSmallScreens != 1) {
                                                if (type == i && parser.getDepth() <= supportsNormalScreens2) {
                                                    i8 = supportsNormalScreens2;
                                                    break;
                                                }
                                                if (type == i) {
                                                    i8 = supportsNormalScreens2;
                                                } else if (type == 4) {
                                                    i8 = supportsNormalScreens2;
                                                } else {
                                                    str3 = parser.getName();
                                                    if (str3.equals(TAG_USES_FEATURE)) {
                                                        FeatureInfo featureInfo = parseUsesFeature(resources, xmlResourceParser);
                                                        featureInfo.flags |= 1;
                                                        features = ArrayUtils.add(features, featureInfo);
                                                        i8 = supportsNormalScreens2;
                                                    } else {
                                                        str4 = TAG;
                                                        StringBuilder stringBuilder4 = new StringBuilder();
                                                        i8 = supportsNormalScreens2;
                                                        stringBuilder4.append("Unknown element under <feature-group>: ");
                                                        stringBuilder4.append(str3);
                                                        stringBuilder4.append(" at ");
                                                        stringBuilder4.append(this.mArchiveSourcePath);
                                                        stringBuilder4.append(" ");
                                                        stringBuilder4.append(parser.getPositionDescription());
                                                        Slog.w(str4, stringBuilder4.toString());
                                                    }
                                                    XmlUtils.skipCurrentTag(parser);
                                                }
                                                supportsNormalScreens2 = i8;
                                                i = 3;
                                            } else {
                                                i8 = supportsNormalScreens2;
                                                break;
                                            }
                                        }
                                        if (features != null) {
                                            supportsXLargeScreens2.features = new FeatureInfo[features.size()];
                                            supportsXLargeScreens2.features = (FeatureInfo[]) features.toArray(supportsXLargeScreens2.features);
                                        }
                                        packageR.featureGroups = ArrayUtils.add(packageR.featureGroups, supportsXLargeScreens2);
                                        i6 = type;
                                        supportsSmallScreens = next;
                                        supportsNormalScreens2 = supportsNormalScreens;
                                    } else if (tagName.equals(TAG_USES_SDK) != 0) {
                                        TypedArray sa3;
                                        if (SDK_VERSION > 0) {
                                            String minCode;
                                            supportsXLargeScreens2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestUsesSdk);
                                            nameError = null;
                                            type = 0;
                                            str4 = null;
                                            TypedValue val = supportsXLargeScreens2.peekValue(0);
                                            if (val != null) {
                                                int minVers = 1;
                                                if (val.type != 3 || val.string == null) {
                                                    supportsLargeScreens2 = val.data;
                                                    i = supportsLargeScreens2;
                                                    type = supportsLargeScreens2;
                                                } else {
                                                    String charSequence = val.string.toString();
                                                    nameError = charSequence;
                                                    str4 = charSequence;
                                                    i = minVers;
                                                }
                                            } else {
                                                i = 1;
                                            }
                                            val = supportsXLargeScreens2.peekValue(1);
                                            int targetVers;
                                            if (val != null) {
                                                targetVers = type;
                                                if (val.type != 3 || val.string == null) {
                                                    type = val.data;
                                                } else {
                                                    str4 = val.string.toString();
                                                    if (nameError == null) {
                                                        nameError = str4;
                                                    }
                                                    type = targetVers;
                                                }
                                            } else {
                                                targetVers = type;
                                            }
                                            supportsXLargeScreens2.recycle();
                                            if (packageR.packageName != null) {
                                                sa3 = supportsXLargeScreens2;
                                                if (!(packageR.packageName.contains(".cts") == 0 && packageR.packageName.contains(".gts") == 0) && (i > SDK_VERSION || targetVers > SDK_VERSION)) {
                                                    supportsXLargeScreens2 = TAG;
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("cts pkg ");
                                                    stringBuilder2.append(packageR.packageName);
                                                    stringBuilder2.append(" minVers is :");
                                                    stringBuilder2.append(i);
                                                    stringBuilder2.append(",change to SDK_VERSION:");
                                                    stringBuilder2.append(SDK_VERSION);
                                                    Slog.w(supportsXLargeScreens2, stringBuilder2.toString());
                                                    str4 = null;
                                                    nameError = null;
                                                    supportsXLargeScreens2 = SDK_VERSION;
                                                    type = supportsXLargeScreens2;
                                                    i = supportsXLargeScreens2;
                                                    supportsXLargeScreens2 = computeMinSdkVersion(i, nameError, SDK_VERSION, SDK_CODENAMES, strArr);
                                                    if (supportsXLargeScreens2 >= 0) {
                                                        this.mParseError = -12;
                                                        return null;
                                                    }
                                                    boolean defaultToCurrentDevBranch = (i2 & 128) != 0;
                                                    supportsNormalScreens2 = computeTargetSdkVersion(type, str4, SDK_CODENAMES, strArr, defaultToCurrentDevBranch);
                                                    if (supportsNormalScreens2 < 0) {
                                                        this.mParseError = true;
                                                        return null;
                                                    }
                                                    packageR.applicationInfo.minSdkVersion = supportsXLargeScreens2;
                                                    packageR.applicationInfo.targetSdkVersion = supportsNormalScreens2;
                                                } else {
                                                    minCode = nameError;
                                                }
                                            } else {
                                                sa3 = supportsXLargeScreens2;
                                                minCode = nameError;
                                            }
                                            nameError = minCode;
                                            supportsXLargeScreens2 = computeMinSdkVersion(i, nameError, SDK_VERSION, SDK_CODENAMES, strArr);
                                            if (supportsXLargeScreens2 >= 0) {
                                            }
                                        } else {
                                            sa3 = sa2;
                                        }
                                        XmlUtils.skipCurrentTag(parser);
                                        supportsSmallScreens = next;
                                        supportsNormalScreens2 = supportsNormalScreens;
                                        supportsLargeScreens2 = supportsLargeScreens4;
                                        sa2 = sa3;
                                        i7 = 3;
                                        outerDepth = i5;
                                        i3 = i7;
                                        targetSandboxVersion = i4;
                                        str = str2;
                                        supportsXLargeScreens2 = supportsXLargeScreens4;
                                        set2 = acceptedTags;
                                    } else {
                                        if (tagName.equals(TAG_SUPPORT_SCREENS) != 0) {
                                            supportsXLargeScreens2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestSupportsScreens);
                                            packageR.applicationInfo.requiresSmallestWidthDp = supportsXLargeScreens2.getInteger(6, 0);
                                            packageR.applicationInfo.compatibleWidthLimitDp = supportsXLargeScreens2.getInteger(7, 0);
                                            packageR.applicationInfo.largestWidthLimitDp = supportsXLargeScreens2.getInteger(8, 0);
                                            supportsSmallScreens = supportsXLargeScreens2.getInteger(1, next);
                                            supportsNormalScreens2 = supportsXLargeScreens2.getInteger(2, supportsNormalScreens);
                                            type = supportsXLargeScreens2.getInteger(3, supportsLargeScreens4);
                                            supportsLargeScreens2 = supportsXLargeScreens2.getInteger(5, supportsXLargeScreens4);
                                            supportsNormalScreens = supportsXLargeScreens2.getInteger(4, resizeable2);
                                            i = supportsXLargeScreens2.getInteger(0, anyDensity2);
                                            supportsXLargeScreens2.recycle();
                                            XmlUtils.skipCurrentTag(parser);
                                            sa2 = supportsXLargeScreens2;
                                            supportsXLargeScreens4 = supportsLargeScreens2;
                                            supportsLargeScreens2 = type;
                                            anyDensity2 = i;
                                            i7 = 3;
                                            resizeable2 = supportsNormalScreens;
                                        } else {
                                            resizeable = resizeable2;
                                            i = anyDensity2;
                                            supportsXLargeScreens = supportsXLargeScreens4;
                                            type = supportsLargeScreens4;
                                            i7 = 3;
                                            if (tagName.equals(TAG_PROTECTED_BROADCAST) != 0) {
                                                supportsXLargeScreens2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestProtectedBroadcast);
                                                supportsNormalScreens2 = supportsXLargeScreens2.getNonResourceString(0);
                                                supportsXLargeScreens2.recycle();
                                                if (supportsNormalScreens2 != 0) {
                                                    if (packageR.protectedBroadcasts == null) {
                                                        packageR.protectedBroadcasts = new ArrayList();
                                                    }
                                                    if (!packageR.protectedBroadcasts.contains(supportsNormalScreens2)) {
                                                        packageR.protectedBroadcasts.add(supportsNormalScreens2.intern());
                                                    }
                                                }
                                                XmlUtils.skipCurrentTag(parser);
                                            } else if (tagName.equals(TAG_INSTRUMENTATION) != 0) {
                                                if (parseInstrumentation(packageR, resources, xmlResourceParser, strArr) == 0) {
                                                    return 0;
                                                }
                                                supportsLargeScreens = type;
                                                anyDensity = i;
                                                supportsSmallScreens = next;
                                                supportsNormalScreens2 = supportsNormalScreens;
                                                supportsXLargeScreens4 = supportsXLargeScreens;
                                                resizeable2 = resizeable;
                                                supportsLargeScreens2 = supportsLargeScreens;
                                                anyDensity2 = anyDensity;
                                                outerDepth = i5;
                                                i3 = i7;
                                                targetSandboxVersion = i4;
                                                str = str2;
                                                supportsXLargeScreens2 = supportsXLargeScreens4;
                                                set2 = acceptedTags;
                                            } else if (tagName.equals(TAG_ORIGINAL_PACKAGE) != 0) {
                                                supportsXLargeScreens2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestOriginalPackage);
                                                nameError = supportsXLargeScreens2.getNonConfigurationString(0, 0);
                                                if (!packageR.packageName.equals(nameError)) {
                                                    if (packageR.mOriginalPackages == null) {
                                                        packageR.mOriginalPackages = new ArrayList();
                                                        packageR.mRealPackage = packageR.packageName;
                                                    }
                                                    packageR.mOriginalPackages.add(nameError);
                                                }
                                                supportsXLargeScreens2.recycle();
                                                XmlUtils.skipCurrentTag(parser);
                                            } else if (tagName.equals(TAG_ADOPT_PERMISSIONS) != 0) {
                                                supportsXLargeScreens2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestOriginalPackage);
                                                nameError = supportsXLargeScreens2.getNonConfigurationString(0, 0);
                                                supportsXLargeScreens2.recycle();
                                                if (nameError != null) {
                                                    if (packageR.mAdoptPermissions == null) {
                                                        packageR.mAdoptPermissions = new ArrayList();
                                                    }
                                                    packageR.mAdoptPermissions.add(nameError);
                                                }
                                                XmlUtils.skipCurrentTag(parser);
                                            } else {
                                                if (tagName.equals(TAG_USES_GL_TEXTURE) != 0) {
                                                    XmlUtils.skipCurrentTag(parser);
                                                } else if (tagName.equals(TAG_COMPATIBLE_SCREENS) != 0) {
                                                    XmlUtils.skipCurrentTag(parser);
                                                } else if (tagName.equals(TAG_SUPPORTS_INPUT) != 0) {
                                                    XmlUtils.skipCurrentTag(parser);
                                                } else if (tagName.equals(TAG_EAT_COMMENT) != 0) {
                                                    XmlUtils.skipCurrentTag(parser);
                                                } else if (tagName.equals("package") == 0) {
                                                    supportsLargeScreens = type;
                                                    anyDensity = i;
                                                    if (tagName.equals(TAG_RESTRICT_UPDATE) != 0) {
                                                        TypedArray sa4;
                                                        if ((i2 & 16) != 0) {
                                                            supportsXLargeScreens2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestRestrictUpdate);
                                                            nameError = supportsXLargeScreens2.getNonConfigurationString(0, 0);
                                                            supportsXLargeScreens2.recycle();
                                                            packageR.restrictUpdateHash = null;
                                                            if (nameError != null) {
                                                                int hashLength;
                                                                supportsLargeScreens2 = nameError.length();
                                                                byte[] hashBytes = new byte[(supportsLargeScreens2 / 2)];
                                                                type = 0;
                                                                while (type < supportsLargeScreens2) {
                                                                    sa4 = supportsXLargeScreens2;
                                                                    hashLength = supportsLargeScreens2;
                                                                    hashBytes[type / 2] = (byte) ((Character.digit(nameError.charAt(type), 16) << 4) + Character.digit(nameError.charAt(type + 1), 16));
                                                                    type += 2;
                                                                    supportsXLargeScreens2 = sa4;
                                                                    supportsLargeScreens2 = hashLength;
                                                                }
                                                                sa4 = supportsXLargeScreens2;
                                                                hashLength = supportsLargeScreens2;
                                                                packageR.restrictUpdateHash = hashBytes;
                                                            } else {
                                                                sa4 = supportsXLargeScreens2;
                                                            }
                                                        } else {
                                                            sa4 = sa2;
                                                        }
                                                        XmlUtils.skipCurrentTag(parser);
                                                        supportsSmallScreens = next;
                                                        supportsNormalScreens2 = supportsNormalScreens;
                                                        supportsXLargeScreens4 = supportsXLargeScreens;
                                                        resizeable2 = resizeable;
                                                        supportsLargeScreens2 = supportsLargeScreens;
                                                        anyDensity2 = anyDensity;
                                                        sa2 = sa4;
                                                        outerDepth = i5;
                                                        i3 = i7;
                                                        targetSandboxVersion = i4;
                                                        str = str2;
                                                        supportsXLargeScreens2 = supportsXLargeScreens4;
                                                        set2 = acceptedTags;
                                                    } else {
                                                        supportsXLargeScreens2 = TAG;
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("Unknown element under <manifest>: ");
                                                        stringBuilder2.append(parser.getName());
                                                        stringBuilder2.append(" at ");
                                                        stringBuilder2.append(this.mArchiveSourcePath);
                                                        stringBuilder2.append(" ");
                                                        stringBuilder2.append(parser.getPositionDescription());
                                                        Slog.w(supportsXLargeScreens2, stringBuilder2.toString());
                                                        XmlUtils.skipCurrentTag(parser);
                                                    }
                                                } else if (MULTI_PACKAGE_APK_ENABLED == 0) {
                                                    XmlUtils.skipCurrentTag(parser);
                                                } else {
                                                    supportsLargeScreens = type;
                                                    anyDensity = i;
                                                    if (parseBaseApkChild(packageR, resources, xmlResourceParser, i2, strArr) == 0) {
                                                        return 0;
                                                    }
                                                    supportsSmallScreens = next;
                                                    supportsNormalScreens2 = supportsNormalScreens;
                                                    supportsXLargeScreens4 = supportsXLargeScreens;
                                                    resizeable2 = resizeable;
                                                    supportsLargeScreens2 = supportsLargeScreens;
                                                    anyDensity2 = anyDensity;
                                                    outerDepth = i5;
                                                    i3 = i7;
                                                    targetSandboxVersion = i4;
                                                    str = str2;
                                                    supportsXLargeScreens2 = supportsXLargeScreens4;
                                                    set2 = acceptedTags;
                                                }
                                                supportsLargeScreens = type;
                                                anyDensity = i;
                                            }
                                            sa2 = supportsXLargeScreens2;
                                            supportsLargeScreens2 = type;
                                            anyDensity2 = i;
                                            supportsSmallScreens = next;
                                            supportsNormalScreens2 = supportsNormalScreens;
                                            supportsXLargeScreens4 = supportsXLargeScreens;
                                            resizeable2 = resizeable;
                                        }
                                        outerDepth = i5;
                                        i3 = i7;
                                        targetSandboxVersion = i4;
                                        str = str2;
                                        supportsXLargeScreens2 = supportsXLargeScreens4;
                                        set2 = acceptedTags;
                                    }
                                    if (parseUsesPermission(packageR, resources, xmlResourceParser) == 0) {
                                        return 0;
                                    }
                                    supportsSmallScreens = next;
                                    supportsNormalScreens2 = supportsNormalScreens;
                                    supportsXLargeScreens4 = supportsXLargeScreens;
                                    resizeable2 = resizeable;
                                    supportsLargeScreens2 = supportsLargeScreens;
                                    anyDensity2 = anyDensity;
                                    outerDepth = i5;
                                    i3 = i7;
                                    targetSandboxVersion = i4;
                                    str = str2;
                                    supportsXLargeScreens2 = supportsXLargeScreens4;
                                    set2 = acceptedTags;
                                } else if (!parseUsesPermission(packageR, resources, xmlResourceParser)) {
                                    return null;
                                }
                                i7 = i;
                                i7 = i;
                                resizeable = resizeable2;
                                anyDensity = anyDensity2;
                                supportsXLargeScreens = supportsXLargeScreens4;
                                supportsLargeScreens = supportsLargeScreens4;
                                supportsSmallScreens = next;
                                supportsNormalScreens2 = supportsNormalScreens;
                                supportsXLargeScreens4 = supportsXLargeScreens;
                                resizeable2 = resizeable;
                                supportsLargeScreens2 = supportsLargeScreens;
                                anyDensity2 = anyDensity;
                                outerDepth = i5;
                                i3 = i7;
                                targetSandboxVersion = i4;
                                str = str2;
                                supportsXLargeScreens2 = supportsXLargeScreens4;
                                set2 = acceptedTags;
                            } else if (parseKeySets(packageR, resources, xmlResourceParser, strArr) == 0) {
                                return null;
                            } else {
                                supportsXLargeScreens2 = 0;
                                i7 = 3;
                            }
                            resizeable = resizeable2;
                            anyDensity = anyDensity2;
                            supportsXLargeScreens = supportsXLargeScreens4;
                            supportsLargeScreens = supportsLargeScreens4;
                            supportsSmallScreens = next;
                            supportsNormalScreens2 = supportsNormalScreens;
                            supportsXLargeScreens4 = supportsXLargeScreens;
                            resizeable2 = resizeable;
                            supportsLargeScreens2 = supportsLargeScreens;
                            anyDensity2 = anyDensity;
                            outerDepth = i5;
                            i3 = i7;
                            targetSandboxVersion = i4;
                            str = str2;
                            supportsXLargeScreens2 = supportsXLargeScreens4;
                            set2 = acceptedTags;
                            supportsSmallScreens = next;
                            supportsNormalScreens2 = supportsNormalScreens;
                            supportsLargeScreens2 = supportsLargeScreens4;
                            outerDepth = i5;
                            i3 = i7;
                            targetSandboxVersion = i4;
                            str = str2;
                            supportsXLargeScreens2 = supportsXLargeScreens4;
                            set2 = acceptedTags;
                        } else if (foundApp) {
                            Slog.w(TAG, "<manifest> has more than one <application>");
                            XmlUtils.skipCurrentTag(parser);
                        } else {
                            str2 = str;
                            str = supportsNormalScreens2;
                            supportsLargeScreens4 = supportsLargeScreens3;
                            supportsXLargeScreens4 = supportsXLargeScreens3;
                            next = supportsSmallScreens;
                            i = -12;
                            i5 = i3;
                            if (parseBaseApplication(packageR, resources, xmlResourceParser, i2, strArr) == 0) {
                                return 0;
                            }
                            if (packageR.applicationInfo.minEmuiSdkVersion > CURRENT_EMUI_SDK_VERSION && CURRENT_EMUI_SDK_VERSION != 0) {
                                supportsXLargeScreens2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("package requires min EMUI sdk level=");
                                stringBuilder2.append(packageR.applicationInfo.minEmuiSdkVersion);
                                stringBuilder2.append(", current EMUI sdk level=");
                                stringBuilder2.append(CURRENT_EMUI_SDK_VERSION);
                                Slog.e(supportsXLargeScreens2, stringBuilder2.toString());
                                this.mParseError = i;
                                return 0;
                            } else if (packageR.applicationInfo.minEmuiSysImgVersion > sCurrentEmuiSysImgVersion) {
                                supportsXLargeScreens2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(packageR.applicationInfo.packageName);
                                stringBuilder2.append(" requires min system img version = ");
                                stringBuilder2.append(packageR.applicationInfo.minEmuiSysImgVersion);
                                stringBuilder2.append(", current system img version = ");
                                stringBuilder2.append(sCurrentEmuiSysImgVersion);
                                Slog.e(supportsXLargeScreens2, stringBuilder2.toString());
                                this.mParseError = i;
                                return 0;
                            } else {
                                supportsSmallScreens = next;
                                supportsNormalScreens2 = str;
                                foundApp = true;
                            }
                        }
                        supportsLargeScreens2 = supportsLargeScreens4;
                        i7 = 3;
                        outerDepth = i5;
                        i3 = i7;
                        targetSandboxVersion = i4;
                        str = str2;
                        supportsXLargeScreens2 = supportsXLargeScreens4;
                        set2 = acceptedTags;
                    } else {
                        String str5 = TAG;
                        supportsXLargeScreens3 = supportsXLargeScreens2;
                        supportsXLargeScreens2 = new StringBuilder();
                        supportsLargeScreens3 = supportsLargeScreens2;
                        supportsXLargeScreens2.append("Skipping unsupported element under <manifest>: ");
                        supportsXLargeScreens2.append(tagName);
                        supportsXLargeScreens2.append(" at ");
                        supportsXLargeScreens2.append(this.mArchiveSourcePath);
                        supportsXLargeScreens2.append(" ");
                        supportsXLargeScreens2.append(parser.getPositionDescription());
                        Slog.w(str5, supportsXLargeScreens2.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                    next = supportsSmallScreens;
                    i5 = i3;
                    str2 = str;
                    resizeable = resizeable2;
                    anyDensity = anyDensity2;
                    supportsXLargeScreens = supportsXLargeScreens3;
                    supportsLargeScreens = supportsLargeScreens3;
                }
                i7 = 3;
                supportsNormalScreens = supportsNormalScreens2;
            } else {
                supportsXLargeScreens = supportsXLargeScreens2;
                supportsLargeScreens = supportsLargeScreens2;
                next = supportsSmallScreens;
                i7 = 3;
                i5 = i3;
                i4 = targetSandboxVersion;
                str2 = str;
                resizeable = resizeable2;
                anyDensity = anyDensity2;
                supportsNormalScreens = supportsNormalScreens2;
            }
            supportsSmallScreens = next;
            supportsNormalScreens2 = supportsNormalScreens;
            supportsXLargeScreens2 = supportsXLargeScreens;
            outerDepth = i5;
            i3 = i7;
            resizeable2 = resizeable;
            supportsLargeScreens2 = supportsLargeScreens;
            targetSandboxVersion = i4;
            str = str2;
            anyDensity2 = anyDensity;
            set2 = acceptedTags;
        }
        if (foundApp || packageR.instrumentation.size() != 0) {
            supportsNormalScreens2 = 0;
        } else {
            supportsNormalScreens2 = 0;
            strArr[0] = "<manifest> does not contain an <application> or <instrumentation>";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_EMPTY;
        }
        supportsXLargeScreens2 = NEW_PERMISSIONS.length;
        stringBuilder = null;
        for (supportsLargeScreens2 = supportsNormalScreens2; supportsLargeScreens2 < supportsXLargeScreens2; supportsLargeScreens2++) {
            NewPermissionInfo npi = NEW_PERMISSIONS[supportsLargeScreens2];
            if (packageR.applicationInfo.targetSdkVersion >= npi.sdkVersion) {
                break;
            }
            if (!packageR.requestedPermissions.contains(npi.name)) {
                if (stringBuilder == null) {
                    stringBuilder = new StringBuilder(128);
                    stringBuilder.append(packageR.packageName);
                    stringBuilder.append(": compat added ");
                } else {
                    stringBuilder.append(' ');
                }
                stringBuilder.append(npi.name);
                packageR.requestedPermissions.add(npi.name);
            }
        }
        if (stringBuilder != null) {
            Slog.i(TAG, stringBuilder.toString());
        }
        supportsLargeScreens2 = SPLIT_PERMISSIONS.length;
        type = supportsNormalScreens2;
        while (type < supportsLargeScreens2) {
            SplitPermissionInfo spi = SPLIT_PERMISSIONS[type];
            if (packageR.applicationInfo.targetSdkVersion < spi.targetSdk && packageR.requestedPermissions.contains(spi.rootPerm)) {
                for (i = supportsNormalScreens2; i < spi.newPerms.length; i++) {
                    tagName = spi.newPerms[i];
                    if (!packageR.requestedPermissions.contains(tagName)) {
                        packageR.requestedPermissions.add(tagName);
                    }
                }
            }
            type++;
            supportsNormalScreens2 = 0;
        }
        if (next < 0 || (next > 0 && packageR.applicationInfo.targetSdkVersion >= 4)) {
            applicationInfo = packageR.applicationInfo;
            applicationInfo.flags |= 512;
        }
        if (supportsNormalScreens != 0) {
            applicationInfo = packageR.applicationInfo;
            applicationInfo.flags |= 1024;
        }
        if (supportsLargeScreens < 0 || (supportsLargeScreens > 0 && packageR.applicationInfo.targetSdkVersion >= 4)) {
            applicationInfo = packageR.applicationInfo;
            applicationInfo.flags |= 2048;
        }
        if (supportsXLargeScreens < 0 || (supportsXLargeScreens > 0 && packageR.applicationInfo.targetSdkVersion >= 9)) {
            applicationInfo = packageR.applicationInfo;
            applicationInfo.flags |= 524288;
        }
        if (resizeable < 0 || (resizeable > 0 && packageR.applicationInfo.targetSdkVersion >= 4)) {
            applicationInfo = packageR.applicationInfo;
            applicationInfo.flags |= 4096;
        }
        if (anyDensity < 0 || (anyDensity > 0 && packageR.applicationInfo.targetSdkVersion >= 4)) {
            applicationInfo = packageR.applicationInfo;
            applicationInfo.flags |= 8192;
        }
        if (packageR.applicationInfo.usesCompatibilityMode()) {
            adjustPackageToBeUnresizeableAndUnpipable(pkg);
        }
        return packageR;
    }

    private boolean checkOverlayRequiredSystemProperty(String propName, String propValue) {
        boolean z = false;
        String currValue;
        if (!TextUtils.isEmpty(propName) && !TextUtils.isEmpty(propValue)) {
            currValue = SystemProperties.get(propName);
            if (currValue != null && currValue.equals(propValue)) {
                z = true;
            }
            return z;
        } else if (TextUtils.isEmpty(propName) && TextUtils.isEmpty(propValue)) {
            return true;
        } else {
            currValue = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Disabling overlay - incomplete property :'");
            stringBuilder.append(propName);
            stringBuilder.append("=");
            stringBuilder.append(propValue);
            stringBuilder.append("' - require both requiredSystemPropertyName AND requiredSystemPropertyValue to be specified.");
            Slog.w(currValue, stringBuilder.toString());
            return false;
        }
    }

    private void adjustPackageToBeUnresizeableAndUnpipable(Package pkg) {
        Iterator it = pkg.activities.iterator();
        while (it.hasNext()) {
            Activity a = (Activity) it.next();
            a.info.resizeMode = 0;
            ActivityInfo activityInfo = a.info;
            activityInfo.flags &= -4194305;
        }
    }

    public static int computeTargetSdkVersion(int targetVers, String targetCode, String[] platformSdkCodenames, String[] outError, boolean forceCurrentDev) {
        if (targetCode == null) {
            return targetVers;
        }
        if (ArrayUtils.contains(platformSdkCodenames, targetCode) || forceCurrentDev) {
            return 10000;
        }
        StringBuilder stringBuilder;
        if (platformSdkCodenames.length > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Requires development platform ");
            stringBuilder.append(targetCode);
            stringBuilder.append(" (current platform is any of ");
            stringBuilder.append(Arrays.toString(platformSdkCodenames));
            stringBuilder.append(")");
            outError[0] = stringBuilder.toString();
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Requires development platform ");
            stringBuilder.append(targetCode);
            stringBuilder.append(" but this is a release platform.");
            outError[0] = stringBuilder.toString();
        }
        return -1;
    }

    public static int computeMinSdkVersion(int minVers, String minCode, int platformSdkVersion, String[] platformSdkCodenames, String[] outError) {
        StringBuilder stringBuilder;
        if (minCode == null) {
            if (minVers <= platformSdkVersion) {
                return minVers;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Requires newer sdk version #");
            stringBuilder.append(minVers);
            stringBuilder.append(" (current version is #");
            stringBuilder.append(platformSdkVersion);
            stringBuilder.append(")");
            outError[0] = stringBuilder.toString();
            return -1;
        } else if (ArrayUtils.contains(platformSdkCodenames, minCode)) {
            return 10000;
        } else {
            if (platformSdkCodenames.length > 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Requires development platform ");
                stringBuilder.append(minCode);
                stringBuilder.append(" (current platform is any of ");
                stringBuilder.append(Arrays.toString(platformSdkCodenames));
                stringBuilder.append(")");
                outError[0] = stringBuilder.toString();
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Requires development platform ");
                stringBuilder.append(minCode);
                stringBuilder.append(" but this is a release platform.");
                outError[0] = stringBuilder.toString();
            }
            return -1;
        }
    }

    private FeatureInfo parseUsesFeature(Resources res, AttributeSet attrs) {
        FeatureInfo fi = new FeatureInfo();
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestUsesFeature);
        fi.name = sa.getNonResourceString(0);
        fi.version = sa.getInt(3, 0);
        if (fi.name == null) {
            fi.reqGlEsVersion = sa.getInt(1, 0);
        }
        if (sa.getBoolean(2, true)) {
            fi.flags |= 1;
        }
        sa.recycle();
        return fi;
    }

    private boolean parseUsesStaticLibrary(Package pkg, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesStaticLibrary);
        String lname = sa.getNonResourceString(0);
        int version = sa.getInt(1, -1);
        String certSha256Digest = sa.getNonResourceString(2);
        sa.recycle();
        StringBuilder stringBuilder;
        if (lname == null || version < 0 || certSha256Digest == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Bad uses-static-library declaration name: ");
            stringBuilder.append(lname);
            stringBuilder.append(" version: ");
            stringBuilder.append(version);
            stringBuilder.append(" certDigest");
            stringBuilder.append(certSha256Digest);
            outError[0] = stringBuilder.toString();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            XmlUtils.skipCurrentTag(parser);
            return false;
        } else if (pkg.usesStaticLibraries == null || !pkg.usesStaticLibraries.contains(lname)) {
            lname = lname.intern();
            certSha256Digest = certSha256Digest.replace(":", "").toLowerCase();
            String[] additionalCertSha256Digests = EmptyArray.STRING;
            if (pkg.applicationInfo.targetSdkVersion >= 27) {
                additionalCertSha256Digests = parseAdditionalCertificates(res, parser, outError);
                if (additionalCertSha256Digests == null) {
                    return false;
                }
            }
            XmlUtils.skipCurrentTag(parser);
            String[] certSha256Digests = new String[(additionalCertSha256Digests.length + 1)];
            certSha256Digests[0] = certSha256Digest;
            System.arraycopy(additionalCertSha256Digests, 0, certSha256Digests, 1, additionalCertSha256Digests.length);
            pkg.usesStaticLibraries = ArrayUtils.add(pkg.usesStaticLibraries, lname);
            pkg.usesStaticLibrariesVersions = ArrayUtils.appendLong(pkg.usesStaticLibrariesVersions, (long) version, true);
            pkg.usesStaticLibrariesCertDigests = (String[][]) ArrayUtils.appendElement(String[].class, pkg.usesStaticLibrariesCertDigests, certSha256Digests, true);
            return true;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Depending on multiple versions of static library ");
            stringBuilder.append(lname);
            outError[0] = stringBuilder.toString();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            XmlUtils.skipCurrentTag(parser);
            return false;
        }
    }

    private String[] parseAdditionalCertificates(Resources resources, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        String[] certSha256Digests = EmptyArray.STRING;
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                return certSha256Digests;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals("additional-certificate")) {
                        TypedArray sa = resources.obtainAttributes(parser, R.styleable.AndroidManifestAdditionalCertificate);
                        String certSha256Digest = sa.getNonResourceString(0);
                        sa.recycle();
                        if (TextUtils.isEmpty(certSha256Digest)) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Bad additional-certificate declaration with empty certDigest:");
                            stringBuilder.append(certSha256Digest);
                            outError[0] = stringBuilder.toString();
                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                            XmlUtils.skipCurrentTag(parser);
                            sa.recycle();
                            return null;
                        }
                        certSha256Digests = (String[]) ArrayUtils.appendElement(String.class, certSha256Digests, certSha256Digest.replace(":", "").toLowerCase());
                    } else {
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
        return certSha256Digests;
    }

    private boolean parseUsesPermission(Package pkg, Resources res, XmlResourceParser parser) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesPermission);
        String name = sa.getNonResourceString(0);
        int maxSdkVersion = 0;
        TypedValue val = sa.peekValue(1);
        if (val != null && val.type >= 16 && val.type <= 31) {
            maxSdkVersion = val.data;
        }
        String requiredFeature = sa.getNonConfigurationString(2, 0);
        String requiredNotfeature = sa.getNonConfigurationString(3, 0);
        sa.recycle();
        XmlUtils.skipCurrentTag(parser);
        if (name == null) {
            return true;
        }
        if (maxSdkVersion != 0 && maxSdkVersion < VERSION.RESOURCES_SDK_INT) {
            return true;
        }
        if (requiredFeature != null && this.mCallback != null && !this.mCallback.hasFeature(requiredFeature)) {
            return true;
        }
        if (requiredNotfeature != null && this.mCallback != null && this.mCallback.hasFeature(requiredNotfeature)) {
            return true;
        }
        if (pkg.requestedPermissions.indexOf(name) == -1) {
            pkg.requestedPermissions.add(name.intern());
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring duplicate uses-permissions/uses-permissions-sdk-m: ");
            stringBuilder.append(name);
            stringBuilder.append(" in package: ");
            stringBuilder.append(pkg.packageName);
            stringBuilder.append(" at: ");
            stringBuilder.append(parser.getPositionDescription());
            Slog.w(str, stringBuilder.toString());
        }
        return true;
    }

    private static String buildClassName(String pkg, CharSequence clsSeq, String[] outError) {
        if (clsSeq == null || clsSeq.length() <= 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Empty class name in package ");
            stringBuilder.append(pkg);
            outError[0] = stringBuilder.toString();
            return null;
        }
        String cls = clsSeq.toString();
        if (cls.charAt(0) == '.') {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(pkg);
            stringBuilder2.append(cls);
            return stringBuilder2.toString();
        } else if (cls.indexOf(46) >= 0) {
            return cls;
        } else {
            StringBuilder b = new StringBuilder(pkg);
            b.append('.');
            b.append(cls);
            return b.toString();
        }
    }

    private static String buildCompoundName(String pkg, CharSequence procSeq, String type, String[] outError) {
        String proc = procSeq.toString();
        char c = proc.charAt(0);
        if (pkg == null || c != ':') {
            String nameError = validateName(proc, true, false);
            if (nameError == null || HwThemeManager.HWT_USER_SYSTEM.equals(proc)) {
                return proc;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid ");
            stringBuilder.append(type);
            stringBuilder.append(" name ");
            stringBuilder.append(proc);
            stringBuilder.append(" in package ");
            stringBuilder.append(pkg);
            stringBuilder.append(": ");
            stringBuilder.append(nameError);
            outError[0] = stringBuilder.toString();
            return null;
        } else if (proc.length() < 2) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Bad ");
            stringBuilder2.append(type);
            stringBuilder2.append(" name ");
            stringBuilder2.append(proc);
            stringBuilder2.append(" in package ");
            stringBuilder2.append(pkg);
            stringBuilder2.append(": must be at least two characters");
            outError[0] = stringBuilder2.toString();
            return null;
        } else {
            String nameError2 = validateName(proc.substring(1), false, false);
            if (nameError2 != null) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Invalid ");
                stringBuilder3.append(type);
                stringBuilder3.append(" name ");
                stringBuilder3.append(proc);
                stringBuilder3.append(" in package ");
                stringBuilder3.append(pkg);
                stringBuilder3.append(": ");
                stringBuilder3.append(nameError2);
                outError[0] = stringBuilder3.toString();
                return null;
            }
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(pkg);
            stringBuilder4.append(proc);
            return stringBuilder4.toString();
        }
    }

    private static String buildProcessName(String pkg, String defProc, CharSequence procSeq, int flags, String[] separateProcesses, String[] outError) {
        if ((flags & 2) == 0 || HwThemeManager.HWT_USER_SYSTEM.equals(procSeq)) {
            if (separateProcesses != null) {
                for (int i = separateProcesses.length - 1; i >= 0; i--) {
                    String sp = separateProcesses[i];
                    if (sp.equals(pkg) || sp.equals(defProc) || sp.equals(procSeq)) {
                        return pkg;
                    }
                }
            }
            if (procSeq == null || procSeq.length() <= 0) {
                return defProc;
            }
            return TextUtils.safeIntern(buildCompoundName(pkg, procSeq, "process", outError));
        }
        return defProc != null ? defProc : pkg;
    }

    private static String buildTaskAffinityName(String pkg, String defProc, CharSequence procSeq, String[] outError) {
        if (procSeq == null) {
            return defProc;
        }
        if (procSeq.length() <= 0) {
            return null;
        }
        return buildCompoundName(pkg, procSeq, "taskAffinity", outError);
    }

    private boolean parseKeySets(Package owner, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        String str;
        StringBuilder stringBuilder;
        String encodedKey;
        String str2;
        StringBuilder stringBuilder2;
        StringBuilder stringBuilder3;
        Package packageR = owner;
        Resources resources = res;
        XmlResourceParser xmlResourceParser = parser;
        int outerDepth = parser.getDepth();
        int currentKeySetDepth = -1;
        String currentKeySet = null;
        ArrayMap<String, PublicKey> publicKeys = new ArrayMap();
        ArraySet<String> upgradeKeySets = new ArraySet();
        ArrayMap<String, ArraySet<String>> definedKeySets = new ArrayMap();
        ArraySet<String> improperKeySets = new ArraySet();
        while (true) {
            int next = parser.next();
            int type = next;
            int i;
            int i2;
            if (next != 1) {
                int i3;
                if (type == 3 && parser.getDepth() <= outerDepth) {
                    i3 = outerDepth;
                    i = currentKeySetDepth;
                    i2 = type;
                    break;
                }
                if (type != 3) {
                    String tagName = parser.getName();
                    String publicKeyName;
                    if (!tagName.equals("key-set")) {
                        i3 = outerDepth;
                        if (!tagName.equals("public-key")) {
                            i = currentKeySetDepth;
                            i2 = type;
                            if (tagName.equals("upgrade-key-set")) {
                                TypedArray sa = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestUpgradeKeySet);
                                upgradeKeySets.add(sa.getNonResourceString(null));
                                sa.recycle();
                                XmlUtils.skipCurrentTag(parser);
                            } else {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown element under <key-sets>: ");
                                stringBuilder.append(parser.getName());
                                stringBuilder.append(" at ");
                                stringBuilder.append(this.mArchiveSourcePath);
                                stringBuilder.append(" ");
                                stringBuilder.append(parser.getPositionDescription());
                                Slog.w(str, stringBuilder.toString());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        } else if (currentKeySet == null) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Improperly nested 'key-set' tag at ");
                            stringBuilder.append(parser.getPositionDescription());
                            outError[0] = stringBuilder.toString();
                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                            return false;
                        } else {
                            TypedArray sa2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestPublicKey);
                            publicKeyName = sa2.getNonResourceString(0);
                            encodedKey = sa2.getNonResourceString(1);
                            if (encodedKey == null && publicKeys.get(publicKeyName) == null) {
                                currentKeySetDepth = new StringBuilder();
                                currentKeySetDepth.append("'public-key' ");
                                currentKeySetDepth.append(publicKeyName);
                                currentKeySetDepth.append(" must define a public-key value on first use at ");
                                currentKeySetDepth.append(parser.getPositionDescription());
                                outError[0] = currentKeySetDepth.toString();
                                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                sa2.recycle();
                                return false;
                            }
                            i = currentKeySetDepth;
                            i2 = type;
                            if (encodedKey != null) {
                                currentKeySetDepth = parsePublicKey(encodedKey);
                                if (currentKeySetDepth == 0) {
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("No recognized valid key in 'public-key' tag at ");
                                    stringBuilder2.append(parser.getPositionDescription());
                                    stringBuilder2.append(" key-set ");
                                    stringBuilder2.append(currentKeySet);
                                    stringBuilder2.append(" will not be added to the package's defined key-sets.");
                                    Slog.w(str2, stringBuilder2.toString());
                                    sa2.recycle();
                                    improperKeySets.add(currentKeySet);
                                    XmlUtils.skipCurrentTag(parser);
                                } else {
                                    if (publicKeys.get(publicKeyName) == null || ((PublicKey) publicKeys.get(publicKeyName)).equals(currentKeySetDepth)) {
                                        publicKeys.put(publicKeyName, currentKeySetDepth);
                                    } else {
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("Value of 'public-key' ");
                                        stringBuilder3.append(publicKeyName);
                                        stringBuilder3.append(" conflicts with previously defined value at ");
                                        stringBuilder3.append(parser.getPositionDescription());
                                        outError[0] = stringBuilder3.toString();
                                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                        sa2.recycle();
                                        return false;
                                    }
                                }
                            }
                            ((ArraySet) definedKeySets.get(currentKeySet)).add(publicKeyName);
                            sa2.recycle();
                            XmlUtils.skipCurrentTag(parser);
                        }
                        currentKeySetDepth = i;
                    } else if (currentKeySet != null) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Improperly nested 'key-set' tag at ");
                        stringBuilder2.append(parser.getPositionDescription());
                        outError[0] = stringBuilder2.toString();
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    } else {
                        TypedArray sa3 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestKeySet);
                        publicKeyName = sa3.getNonResourceString(null);
                        i3 = outerDepth;
                        definedKeySets.put(publicKeyName, new ArraySet());
                        outerDepth = publicKeyName;
                        currentKeySetDepth = parser.getDepth();
                        sa3.recycle();
                        currentKeySet = outerDepth;
                        i2 = type;
                    }
                    outerDepth = i3;
                    packageR = owner;
                } else if (parser.getDepth() == currentKeySetDepth) {
                    currentKeySet = null;
                    currentKeySetDepth = -1;
                } else {
                    i3 = outerDepth;
                    i = currentKeySetDepth;
                }
                outerDepth = i3;
                currentKeySetDepth = i;
                packageR = owner;
            } else {
                i = currentKeySetDepth;
                i2 = type;
                break;
            }
        }
        Set<String> publicKeyNames = publicKeys.keySet();
        if (publicKeyNames.removeAll(definedKeySets.keySet())) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Package");
            stringBuilder.append(owner.packageName);
            stringBuilder.append(" AndroidManifext.xml 'key-set' and 'public-key' names must be distinct.");
            outError[0] = stringBuilder.toString();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        Package packageR2 = owner;
        packageR2.mKeySetMapping = new ArrayMap();
        for (Entry<String, ArraySet<String>> e : definedKeySets.entrySet()) {
            Set<String> publicKeyNames2;
            str2 = (String) e.getKey();
            if (((ArraySet) e.getValue()).size() == 0) {
                encodedKey = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                publicKeyNames2 = publicKeyNames;
                stringBuilder4.append("Package");
                stringBuilder4.append(packageR2.packageName);
                stringBuilder4.append(" AndroidManifext.xml 'key-set' ");
                stringBuilder4.append(str2);
                stringBuilder4.append(" has no valid associated 'public-key'. Not including in package's defined key-sets.");
                Slog.w(encodedKey, stringBuilder4.toString());
            } else {
                publicKeyNames2 = publicKeyNames;
                if (improperKeySets.contains(str2)) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Package");
                    stringBuilder2.append(packageR2.packageName);
                    stringBuilder2.append(" AndroidManifext.xml 'key-set' ");
                    stringBuilder2.append(str2);
                    stringBuilder2.append(" contained improper 'public-key' tags. Not including in package's defined key-sets.");
                    Slog.w(str, stringBuilder2.toString());
                } else {
                    packageR2.mKeySetMapping.put(str2, new ArraySet());
                    Iterator it = ((ArraySet) e.getValue()).iterator();
                    while (it.hasNext()) {
                        Iterator it2 = it;
                        ((ArraySet) packageR2.mKeySetMapping.get(str2)).add((PublicKey) publicKeys.get((String) it.next()));
                        it = it2;
                    }
                }
            }
            publicKeyNames = publicKeyNames2;
        }
        if (packageR2.mKeySetMapping.keySet().containsAll(upgradeKeySets)) {
            packageR2.mUpgradeKeySets = upgradeKeySets;
            return true;
        }
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Package");
        stringBuilder3.append(packageR2.packageName);
        stringBuilder3.append(" AndroidManifext.xml does not define all 'upgrade-key-set's .");
        outError[0] = stringBuilder3.toString();
        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return false;
    }

    private boolean parsePermissionGroup(Package owner, int flags, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        Package packageR = owner;
        PermissionGroup permissionGroup = new PermissionGroup(packageR);
        Resources resources = res;
        TypedArray sa = resources.obtainAttributes(parser, R.styleable.AndroidManifestPermissionGroup);
        if (parsePackageItemInfo(packageR, permissionGroup.info, outError, "<permission-group>", sa, true, 2, 0, 1, 8, 5, 7)) {
            permissionGroup.info.descriptionRes = sa.getResourceId(4, 0);
            permissionGroup.info.requestRes = sa.getResourceId(9, 0);
            permissionGroup.info.flags = sa.getInt(6, 0);
            permissionGroup.info.priority = sa.getInt(3, 0);
            sa.recycle();
            PermissionGroup perm = permissionGroup;
            Package packageR2 = packageR;
            if (parseAllMetaData(resources, parser, "<permission-group>", permissionGroup, outError)) {
                packageR2.permissionGroups.add(perm);
                return true;
            }
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        sa.recycle();
        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return false;
    }

    private boolean parsePermission(Package owner, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        Package packageR = owner;
        Resources resources = res;
        XmlResourceParser xmlResourceParser = parser;
        TypedArray sa = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestPermission);
        Component perm = new Permission(packageR);
        if (parsePackageItemInfo(packageR, perm.info, outError, "<permission>", sa, true, 2, 0, 1, 9, 6, 8)) {
            perm.info.group = sa.getNonResourceString(4);
            if (perm.info.group != null) {
                perm.info.group = perm.info.group.intern();
            }
            perm.info.descriptionRes = sa.getResourceId(5, 0);
            perm.info.requestRes = sa.getResourceId(10, 0);
            perm.info.protectionLevel = sa.getInt(3, 0);
            perm.info.flags = sa.getInt(7, 0);
            sa.recycle();
            if (perm.info.protectionLevel == -1) {
                outError[0] = "<permission> does not specify protectionLevel";
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                return false;
            }
            perm.info.protectionLevel = PermissionInfo.fixProtectionLevel(perm.info.protectionLevel);
            if (perm.info.getProtectionFlags() == 0 || (perm.info.protectionLevel & 4096) != 0 || (perm.info.protectionLevel & 8192) != 0 || (perm.info.protectionLevel & 15) == 2) {
                Component perm2 = perm;
                Package packageR2 = packageR;
                if (parseAllMetaData(resources, xmlResourceParser, "<permission>", perm2, outError)) {
                    packageR2.permissions.add(perm2);
                    return true;
                }
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                return false;
            }
            outError[0] = "<permission>  protectionLevel specifies a non-instant flag but is not based on signature type";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        sa.recycle();
        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return false;
    }

    private boolean parsePermissionTree(Package owner, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        Package packageR = owner;
        Permission permission = new Permission(packageR);
        Resources resources = res;
        TypedArray sa = resources.obtainAttributes(parser, R.styleable.AndroidManifestPermissionTree);
        if (parsePackageItemInfo(packageR, permission.info, outError, "<permission-tree>", sa, true, 2, 0, 1, 5, 3, 4)) {
            sa.recycle();
            int index = permission.info.name.indexOf(46);
            if (index > 0) {
                index = permission.info.name.indexOf(46, index + 1);
            }
            if (index < 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("<permission-tree> name has less than three segments: ");
                stringBuilder.append(permission.info.name);
                outError[0] = stringBuilder.toString();
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                return false;
            }
            permission.info.descriptionRes = 0;
            permission.info.requestRes = 0;
            permission.info.protectionLevel = 0;
            permission.tree = true;
            Permission perm = permission;
            Package packageR2 = packageR;
            if (parseAllMetaData(resources, parser, "<permission-tree>", permission, outError)) {
                packageR2.permissions.add(perm);
                return true;
            }
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        sa.recycle();
        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return false;
    }

    private Instrumentation parseInstrumentation(Package owner, Resources res, XmlResourceParser parser, String[] outError) throws XmlPullParserException, IOException {
        Resources resources = res;
        XmlResourceParser xmlResourceParser = parser;
        TypedArray sa = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestInstrumentation);
        if (this.mParseInstrumentationArgs == null) {
            this.mParseInstrumentationArgs = new ParsePackageItemArgs(owner, outError, 2, 0, 1, 8, 6, 7);
            this.mParseInstrumentationArgs.tag = "<instrumentation>";
        }
        this.mParseInstrumentationArgs.sa = sa;
        Component a = new Instrumentation(this.mParseInstrumentationArgs, new InstrumentationInfo());
        if (outError[0] != null) {
            sa.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        String str = sa.getNonResourceString(3);
        a.info.targetPackage = str != null ? str.intern() : null;
        String str2 = sa.getNonResourceString(9);
        a.info.targetProcesses = str2 != null ? str2.intern() : null;
        a.info.handleProfiling = sa.getBoolean(4, false);
        a.info.functionalTest = sa.getBoolean(5, false);
        sa.recycle();
        if (a.info.targetPackage == null) {
            outError[0] = "<instrumentation> does not specify targetPackage";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        if (parseAllMetaData(resources, xmlResourceParser, "<instrumentation>", a, outError)) {
            owner.instrumentation.add(a);
            return a;
        }
        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:358:0x07b6 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:325:0x07b2  */
    /* JADX WARNING: Missing block: B:259:0x0608, code skipped:
            r1 = new java.lang.StringBuilder();
            r1.append("Bad static-library declaration name: ");
            r1.append(r10);
            r1.append(" version: ");
            r1.append(r13);
            r9[0] = r1.toString();
            r0.mParseError = android.content.pm.PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            com.android.internal.util.XmlUtils.skipCurrentTag(r37);
     */
    /* JADX WARNING: Missing block: B:260:0x062b, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:315:0x078a, code skipped:
            if (r10.metaData.getBoolean(METADATA_NOTCH_SUPPORT, false) != false) goto L_0x0790;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean parseBaseApplication(Package owner, Resources res, XmlResourceParser parser, int flags, String[] outError) throws XmlPullParserException, IOException {
        PackageParser packageParser = this;
        Package packageR = owner;
        Resources resources = res;
        XmlResourceParser xmlResourceParser = parser;
        String[] strArr = outError;
        PackageItemInfo ai = packageR.applicationInfo;
        String pkgName = packageR.applicationInfo.packageName;
        TypedArray sa = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestApplication);
        TypedArray sa2 = sa;
        String pkgName2 = pkgName;
        PackageItemInfo ai2 = ai;
        String[] strArr2 = strArr;
        if (parsePackageItemInfo(packageR, ai, strArr, "<application>", sa, false, 3, 1, 2, 42, 22, 30)) {
            String pkgName3;
            String backupAgent;
            int i;
            String requiredFeature;
            boolean z;
            String pkgName4;
            int i2;
            TypedArray sa3 = sa2;
            ApplicationInfo ai3 = ai2;
            if (ai3.name != null) {
                ai3.className = ai3.name;
            }
            String manageSpaceActivity = sa3.getNonConfigurationString(4, 1024);
            if (manageSpaceActivity != null) {
                pkgName3 = pkgName2;
                ai3.manageSpaceActivityName = buildClassName(pkgName3, manageSpaceActivity, strArr2);
            } else {
                pkgName3 = pkgName2;
            }
            if (sa3.getBoolean(17, true)) {
                ai3.flags |= 32768;
                backupAgent = sa3.getNonConfigurationString(16, 1024);
                if (backupAgent != null) {
                    ai3.backupAgentName = buildClassName(pkgName3, backupAgent, strArr2);
                    if (sa3.getBoolean(18, true)) {
                        ai3.flags |= 65536;
                    }
                    if (sa3.getBoolean(21, false)) {
                        ai3.flags |= 131072;
                    }
                    if (sa3.getBoolean(32, false)) {
                        ai3.flags |= 67108864;
                    }
                    if (sa3.getBoolean(40, false)) {
                        ai3.privateFlags |= 8192;
                    }
                }
                TypedValue v = sa3.peekValue(35);
                if (v != null) {
                    i = v.resourceId;
                    ai3.fullBackupContent = i;
                    if (i == 0) {
                        ai3.fullBackupContent = v.data == 0 ? -1 : 0;
                    }
                }
            }
            ai3.theme = sa3.getResourceId(0, 0);
            if ("com.google.android.packageinstaller".equals(pkgName3)) {
                ai3.theme = 33951745;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("parseBaseApplication, packageinstaller new themeName = ");
                stringBuilder.append(resources.getResourceName(ai3.theme));
                Flog.i(206, stringBuilder.toString());
            }
            ai3.descriptionRes = sa3.getResourceId(13, 0);
            packageR.mPersistentApp = sa3.getBoolean(8, false);
            if (packageR.mPersistentApp) {
                requiredFeature = sa3.getNonResourceString(45);
                if (requiredFeature == null || packageParser.mCallback.hasFeature(requiredFeature)) {
                    ai3.flags |= 8;
                }
            }
            if (sa3.getBoolean(27, false)) {
                packageR.mRequiredForAllUsers = true;
            }
            backupAgent = sa3.getString(28);
            if (backupAgent != null && backupAgent.length() > 0) {
                packageR.mRestrictedAccountType = backupAgent;
            }
            String requiredAccountType = sa3.getString(29);
            if (requiredAccountType != null && requiredAccountType.length() > 0) {
                packageR.mRequiredAccountType = requiredAccountType;
            }
            if (sa3.getBoolean(10, false)) {
                ai3.flags |= 2;
            }
            if (sa3.getBoolean(20, false)) {
                ai3.flags |= 16384;
            }
            packageR.baseHardwareAccelerated = sa3.getBoolean(23, packageR.applicationInfo.targetSdkVersion >= 14);
            if (packageR.baseHardwareAccelerated) {
                ai3.flags |= 536870912;
            }
            if (sa3.getBoolean(7, true)) {
                ai3.flags |= 4;
            }
            if (sa3.getBoolean(14, false)) {
                ai3.flags |= 32;
            }
            if (sa3.getBoolean(5, true)) {
                ai3.flags |= 64;
            }
            if (packageR.parentPackage == null && sa3.getBoolean(15, false)) {
                ai3.flags |= 256;
            }
            if (sa3.getBoolean(24, false)) {
                ai3.flags |= 1048576;
            }
            if (sa3.getBoolean(36, packageR.applicationInfo.targetSdkVersion < 28)) {
                ai3.flags |= 134217728;
            }
            if (sa3.getBoolean(26, false)) {
                ai3.flags |= 4194304;
            }
            if (sa3.getBoolean(33, false)) {
                ai3.flags |= Integer.MIN_VALUE;
            }
            if (sa3.getBoolean(34, true)) {
                ai3.flags |= 268435456;
            }
            if (sa3.getBoolean(38, false)) {
                ai3.privateFlags |= 32;
            }
            if (sa3.getBoolean(39, false)) {
                ai3.privateFlags |= 64;
            }
            if (sa3.hasValueOrEmpty(37)) {
                if (sa3.getBoolean(37, true)) {
                    ai3.privateFlags |= 1024;
                } else {
                    ai3.privateFlags |= 2048;
                }
            } else if (packageR.applicationInfo.targetSdkVersion >= 24) {
                ai3.privateFlags |= 4096;
            }
            ai3.maxAspectRatio = sa3.getFloat(44, 0.0f);
            ai3.networkSecurityConfigRes = sa3.getResourceId(41, 0);
            ai3.category = sa3.getInt(43, -1);
            String str = sa3.getNonConfigurationString(6, 0);
            requiredFeature = (str == null || str.length() <= 0) ? null : str.intern();
            ai3.permission = requiredFeature;
            if (packageR.applicationInfo.targetSdkVersion >= 8) {
                str = sa3.getNonConfigurationString(12, 1024);
            } else {
                str = sa3.getNonResourceString(12);
            }
            String str2 = str;
            ai3.taskAffinity = buildTaskAffinityName(ai3.packageName, ai3.packageName, str2, strArr2);
            requiredFeature = sa3.getNonResourceString(48);
            if (requiredFeature != null) {
                ai3.appComponentFactory = buildClassName(ai3.packageName, requiredFeature, strArr2);
            }
            if (strArr2[0] == null) {
                CharSequence pname;
                if (packageR.applicationInfo.targetSdkVersion >= 8) {
                    pname = sa3.getNonConfigurationString(11, 1024);
                } else {
                    pname = sa3.getNonResourceString(11);
                }
                z = true;
                pkgName4 = pkgName3;
                ai3.processName = buildProcessName(ai3.packageName, null, pname, flags, packageParser.mSeparateProcesses, strArr2);
                ai3.enabled = sa3.getBoolean(9, true);
                if (sa3.getBoolean(31, false)) {
                    ai3.flags |= 33554432;
                }
                if (sa3.getBoolean(47, false)) {
                    ai3.privateFlags |= 2;
                    if (ai3.processName == null || ai3.processName.equals(ai3.packageName)) {
                        i2 = 0;
                    } else {
                        i2 = 0;
                        strArr2[0] = "cantSaveState applications can not use custom processes";
                    }
                } else {
                    i2 = 0;
                }
            } else {
                String str3 = requiredFeature;
                pkgName4 = pkgName3;
                i2 = 0;
                z = true;
            }
            ai3.uiOptions = sa3.getInt(25, i2);
            ai3.classLoaderName = sa3.getString(46);
            if (ai3.classLoaderName == null || ClassLoaderFactory.isValidClassLoaderName(ai3.classLoaderName)) {
                i2 = 0;
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Invalid class loader name: ");
                stringBuilder2.append(ai3.classLoaderName);
                i2 = 0;
                strArr2[0] = stringBuilder2.toString();
            }
            sa3.recycle();
            if (strArr2[i2] != null) {
                packageParser.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                return i2;
            }
            boolean hasServiceOrder;
            boolean z2;
            int innerDepth = parser.getDepth();
            CachedComponentArgs cachedArgs = new CachedComponentArgs();
            boolean hasActivityOrder = false;
            boolean hasReceiverOrder = false;
            boolean hasServiceOrder2 = false;
            while (true) {
                hasServiceOrder = hasServiceOrder2;
                hasServiceOrder2 = parser.next();
                boolean type = hasServiceOrder2;
                int i3;
                boolean z3;
                String str4;
                ApplicationInfo applicationInfo;
                String str5;
                int type2;
                PackageParser packageParser2;
                XmlResourceParser str6;
                if (hasServiceOrder2 != z) {
                    if (type && parser.getDepth() <= innerDepth) {
                        i3 = innerDepth;
                        z3 = type;
                        str4 = str2;
                        applicationInfo = ai3;
                        str5 = backupAgent;
                        ai3 = strArr2;
                        type2 = resources;
                        packageParser2 = packageParser;
                        backupAgent = pkgName4;
                        str6 = parser;
                        break;
                    }
                    int i4;
                    if (type) {
                        i3 = innerDepth;
                        str4 = str2;
                        applicationInfo = ai3;
                        str5 = backupAgent;
                        ai3 = strArr2;
                        type2 = resources;
                        hasServiceOrder2 = packageParser;
                        backupAgent = pkgName4;
                        str6 = parser;
                    } else if (type) {
                        i4 = 4;
                        i3 = innerDepth;
                        str4 = str2;
                        applicationInfo = ai3;
                        str5 = backupAgent;
                        ai3 = strArr2;
                        type2 = resources;
                        hasServiceOrder2 = packageParser;
                        backupAgent = pkgName4;
                        str6 = parser;
                    } else {
                        requiredAccountType = parser.getName();
                        if (requiredAccountType.equals(Context.ACTIVITY_SERVICE)) {
                            i4 = 4;
                            i3 = innerDepth;
                            str4 = str2;
                            Activity a = packageParser.parseActivity(packageR, resources, parser, flags, strArr2, cachedArgs, false, packageR.baseHardwareAccelerated);
                            if (a == null) {
                                packageParser.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                return false;
                            }
                            boolean hasActivityOrder2 = hasActivityOrder | (a.order != 0 ? z : false);
                            packageR.activities.add(a);
                            str6 = parser;
                            hasActivityOrder = hasActivityOrder2;
                            applicationInfo = ai3;
                            str5 = backupAgent;
                            ai3 = strArr2;
                            type2 = resources;
                            hasServiceOrder2 = packageParser;
                        } else {
                            i4 = 4;
                            i3 = innerDepth;
                            z3 = type;
                            str4 = str2;
                            requiredFeature = requiredAccountType;
                            if (requiredFeature.equals(HwFrameworkMonitor.KEY_RECEIVER)) {
                                applicationInfo = ai3;
                                z2 = z;
                                str5 = backupAgent;
                                hasServiceOrder2 = packageParser;
                                innerDepth = packageParser.parseActivity(packageR, resources, parser, flags, outError, cachedArgs, true, false);
                                if (innerDepth == 0) {
                                    hasServiceOrder2.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                    return false;
                                }
                                type = hasReceiverOrder | (innerDepth.order != 0);
                                packageR = owner;
                                packageR.receivers.add(innerDepth);
                                str6 = parser;
                                ai3 = outError;
                                hasReceiverOrder = type;
                            } else {
                                applicationInfo = ai3;
                                str5 = backupAgent;
                                hasServiceOrder2 = packageParser;
                                if (requiredFeature.equals(Notification.CATEGORY_SERVICE) != 0) {
                                    innerDepth = hasServiceOrder2.parseService(packageR, res, parser, flags, outError, cachedArgs);
                                    if (innerDepth == 0) {
                                        hasServiceOrder2.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                        return false;
                                    }
                                    type = hasServiceOrder | (innerDepth.order != 0);
                                    packageR.services.add(innerDepth);
                                    str6 = parser;
                                    ai3 = outError;
                                    hasServiceOrder = type;
                                } else if (requiredFeature.equals("provider") != 0) {
                                    innerDepth = hasServiceOrder2.parseProvider(packageR, res, parser, flags, outError, cachedArgs);
                                    if (innerDepth == 0) {
                                        hasServiceOrder2.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                        return false;
                                    }
                                    packageR.providers.add(innerDepth);
                                    type2 = res;
                                    str6 = parser;
                                    ai3 = outError;
                                } else if (requiredFeature.equals("activity-alias") != 0) {
                                    innerDepth = hasServiceOrder2.parseActivityAlias(packageR, res, parser, flags, outError, cachedArgs);
                                    if (innerDepth == 0) {
                                        hasServiceOrder2.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                        return false;
                                    }
                                    type = hasActivityOrder | (innerDepth.order != 0);
                                    packageR.activities.add(innerDepth);
                                    str6 = parser;
                                    ai3 = outError;
                                    hasActivityOrder = type;
                                } else if (parser.getName().equals("meta-data") != 0) {
                                    type2 = res;
                                    ai3 = outError;
                                    innerDepth = hasServiceOrder2.parseMetaData(type2, parser, packageR.mAppMetaData, ai3);
                                    packageR.mAppMetaData = innerDepth;
                                    if (innerDepth == 0) {
                                        hasServiceOrder2.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                        return false;
                                    }
                                    innerDepth = HwWidgetFactory.getThemeId(packageR.mAppMetaData, type2);
                                    if (innerDepth != 0) {
                                        applicationInfo.theme = innerDepth;
                                    }
                                    applicationInfo.minEmuiSdkVersion = packageR.mAppMetaData.getInt("huawei.emui_minSdk");
                                    applicationInfo.targetEmuiSdkVersion = packageR.mAppMetaData.getInt("huawei.emui_targetSdk");
                                    applicationInfo.hwThemeType = packageR.mAppMetaData.getInt("hw.theme_type");
                                    applicationInfo.minEmuiSysImgVersion = packageR.mAppMetaData.getInt("huawei.emui_minSysImgVersion", -1);
                                    applicationInfo.gestnav_extra_flags = packageR.mAppMetaData.getInt("huawei.gestnav_extra_flags");
                                } else {
                                    type2 = res;
                                    str6 = parser;
                                    ai3 = outError;
                                    String lname;
                                    if (requiredFeature.equals("static-library") != 0) {
                                        innerDepth = type2.obtainAttributes(str6, R.styleable.AndroidManifestStaticLibrary);
                                        lname = innerDepth.getNonResourceString(0);
                                        int version = innerDepth.getInt(1, -1);
                                        int versionMajor = innerDepth.getInt(2, 0);
                                        innerDepth.recycle();
                                        if (lname == null) {
                                            break;
                                        } else if (version < 0) {
                                            backupAgent = pkgName4;
                                            break;
                                        } else if (packageR.mSharedUserId != null) {
                                            ai3[0] = "sharedUserId not allowed in static shared library";
                                            hasServiceOrder2.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID;
                                            XmlUtils.skipCurrentTag(parser);
                                            return false;
                                        } else if (packageR.staticSharedLibName != null) {
                                            StringBuilder stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("Multiple static-shared libs for package ");
                                            stringBuilder3.append(pkgName4);
                                            ai3[0] = stringBuilder3.toString();
                                            hasServiceOrder2.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                                            XmlUtils.skipCurrentTag(parser);
                                            return false;
                                        } else {
                                            backupAgent = pkgName4;
                                            packageR.staticSharedLibName = lname.intern();
                                            if (version >= 0) {
                                                packageR.staticSharedLibVersion = PackageInfo.composeLongVersionCode(versionMajor, version);
                                            } else {
                                                packageR.staticSharedLibVersion = (long) version;
                                            }
                                            applicationInfo.privateFlags |= 16384;
                                            XmlUtils.skipCurrentTag(parser);
                                            Object obj = innerDepth;
                                        }
                                    } else {
                                        backupAgent = pkgName4;
                                        if (requiredFeature.equals("library") != 0) {
                                            innerDepth = type2.obtainAttributes(str6, R.styleable.AndroidManifestLibrary);
                                            lname = innerDepth.getNonResourceString(0);
                                            innerDepth.recycle();
                                            if (lname != null) {
                                                lname = lname.intern();
                                                if (!ArrayUtils.contains(packageR.libraryNames, lname)) {
                                                    packageR.libraryNames = ArrayUtils.add(packageR.libraryNames, lname);
                                                }
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                        } else if (requiredFeature.equals("uses-static-library")) {
                                            if (!hasServiceOrder2.parseUsesStaticLibrary(packageR, type2, str6, ai3)) {
                                                return false;
                                            }
                                        } else if (requiredFeature.equals("uses-library") != 0) {
                                            innerDepth = type2.obtainAttributes(str6, R.styleable.AndroidManifestUsesLibrary);
                                            lname = innerDepth.getNonResourceString(0);
                                            boolean req = innerDepth.getBoolean(1, true);
                                            innerDepth.recycle();
                                            if (lname != null) {
                                                lname = lname.intern();
                                                if (req) {
                                                    packageR.usesLibraries = ArrayUtils.add(packageR.usesLibraries, lname);
                                                } else {
                                                    packageR.usesOptionalLibraries = ArrayUtils.add(packageR.usesOptionalLibraries, lname);
                                                }
                                            }
                                            XmlUtils.skipCurrentTag(parser);
                                        } else if (requiredFeature.equals("uses-package")) {
                                            XmlUtils.skipCurrentTag(parser);
                                        } else {
                                            requiredAccountType = TAG;
                                            innerDepth = new StringBuilder();
                                            innerDepth.append("Unknown element under <application>: ");
                                            innerDepth.append(requiredFeature);
                                            innerDepth.append(" at ");
                                            innerDepth.append(hasServiceOrder2.mArchiveSourcePath);
                                            innerDepth.append(" ");
                                            innerDepth.append(parser.getPositionDescription());
                                            Slog.w(requiredAccountType, innerDepth.toString());
                                            XmlUtils.skipCurrentTag(parser);
                                        }
                                        TypedArray sa4 = innerDepth;
                                    }
                                }
                            }
                            backupAgent = pkgName4;
                            type2 = res;
                        }
                        backupAgent = pkgName4;
                    }
                    packageParser = hasServiceOrder2;
                    resources = type2;
                    strArr2 = ai3;
                    pkgName4 = backupAgent;
                    backupAgent = str5;
                    hasServiceOrder2 = hasServiceOrder;
                    str2 = str4;
                    innerDepth = i3;
                    z = true;
                    ai3 = applicationInfo;
                } else {
                    i3 = innerDepth;
                    z3 = type;
                    str4 = str2;
                    applicationInfo = ai3;
                    str5 = backupAgent;
                    ai3 = strArr2;
                    type2 = resources;
                    packageParser2 = packageParser;
                    str6 = parser;
                    break;
                }
            }
            if (hasActivityOrder) {
                Collections.sort(packageR.activities, -$$Lambda$PackageParser$0aobsT7Zf7WVZCqMZ5z2clAuQf4.INSTANCE);
            }
            if (hasReceiverOrder) {
                Collections.sort(packageR.receivers, -$$Lambda$PackageParser$0DZRgzfgaIMpCOhJqjw6PUiU5vw.INSTANCE);
            }
            if (hasServiceOrder) {
                Collections.sort(packageR.services, -$$Lambda$PackageParser$M-9fHqS_eEp1oYkuKJhRHOGUxf8.INSTANCE);
            }
            setMaxAspectRatio(owner);
            z2 = false;
            if (packageR.mAppMetaData != null && packageR.mAppMetaData.containsKey(METADATA_NOTCH_SUPPORT)) {
                z2 = packageR.mAppMetaData.getBoolean(METADATA_NOTCH_SUPPORT, false);
            }
            i = 0;
            if (packageR.mAppMetaData != null && packageR.mAppMetaData.containsKey(METADATA_GESTURE_NAV_OPTIONS)) {
                i = packageR.mAppMetaData.getInt(METADATA_GESTURE_NAV_OPTIONS, 0);
            }
            innerDepth = packageR.activities.size();
            for (int i5 = 0; i5 < innerDepth; i5++) {
                Activity t = (Activity) packageR.activities.get(i5);
                if (!z2) {
                    if (t.metaData != null) {
                    }
                    if (t.metaData == null && t.metaData.containsKey(METADATA_GESTURE_NAV_OPTIONS)) {
                        t.info.hwGestureNavOptions = t.metaData.getInt(METADATA_GESTURE_NAV_OPTIONS, i);
                    } else if (i == 0) {
                        t.info.hwGestureNavOptions = i;
                    }
                }
                t.info.hwNotchSupport = true;
                if (t.metaData == null) {
                }
                if (i == 0) {
                }
            }
            PackageBackwardCompatibility.modifySharedLibraries(owner);
            ApplicationInfo applicationInfo2;
            if (hasDomainURLs(owner)) {
                applicationInfo2 = packageR.applicationInfo;
                applicationInfo2.privateFlags |= 16;
            } else {
                applicationInfo2 = packageR.applicationInfo;
                applicationInfo2.privateFlags &= -17;
            }
            return true;
        }
        sa2.recycle();
        packageParser.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return false;
    }

    private static boolean hasDomainURLs(Package pkg) {
        if (pkg == null || pkg.activities == null) {
            return false;
        }
        ArrayList<Activity> activities = pkg.activities;
        int countActivities = activities.size();
        for (int n = 0; n < countActivities; n++) {
            ArrayList<ActivityIntentInfo> filters = ((Activity) activities.get(n)).intents;
            if (filters != null) {
                int countFilters = filters.size();
                for (int m = 0; m < countFilters; m++) {
                    ActivityIntentInfo aii = (ActivityIntentInfo) filters.get(m);
                    if (aii.hasAction("android.intent.action.VIEW") && aii.hasAction("android.intent.action.VIEW") && (aii.hasDataScheme(IntentFilter.SCHEME_HTTP) || aii.hasDataScheme(IntentFilter.SCHEME_HTTPS))) {
                        return true;
                    }
                }
                continue;
            }
        }
        return false;
    }

    private boolean parseSplitApplication(Package owner, Resources res, XmlResourceParser parser, int flags, int splitIndex, String[] outError) throws XmlPullParserException, IOException {
        PackageParser packageParser = this;
        Package packageR = owner;
        Resources resources = res;
        AttributeSet attributeSet = parser;
        String[] strArr = outError;
        TypedArray sa = resources.obtainAttributes(attributeSet, R.styleable.AndroidManifestApplication);
        boolean z = true;
        boolean z2 = true;
        if (sa.getBoolean(7, true)) {
            int[] iArr = packageR.splitFlags;
            iArr[splitIndex] = iArr[splitIndex] | 4;
        }
        String classLoaderName = sa.getString(46);
        int i = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        int i2 = 0;
        if (classLoaderName == null || ClassLoaderFactory.isValidClassLoaderName(classLoaderName)) {
            packageR.applicationInfo.splitClassLoaderNames[splitIndex] = classLoaderName;
            int innerDepth = parser.getDepth();
            while (true) {
                int innerDepth2 = innerDepth;
                boolean next = parser.next();
                boolean type = next;
                boolean z3;
                int i3;
                String str;
                String[] strArr2;
                AttributeSet classLoaderName2;
                Resources resources2;
                PackageParser packageParser2;
                if (next != z) {
                    if (type && parser.getDepth() <= innerDepth2) {
                        z3 = type;
                        i3 = innerDepth2;
                        str = classLoaderName;
                        strArr2 = strArr;
                        classLoaderName2 = attributeSet;
                        resources2 = resources;
                        innerDepth2 = packageR;
                        packageParser2 = packageParser;
                        break;
                    }
                    Package innerDepth3;
                    if (type) {
                        i3 = innerDepth2;
                        innerDepth = i2;
                        i2 = i;
                        str = classLoaderName;
                        strArr2 = strArr;
                        classLoaderName = attributeSet;
                        resources2 = resources;
                        innerDepth3 = packageR;
                        packageParser2 = packageParser;
                    } else if (type == z2) {
                        i3 = innerDepth2;
                        innerDepth = i2;
                        i2 = i;
                        str = classLoaderName;
                        strArr2 = strArr;
                        classLoaderName = attributeSet;
                        resources2 = resources;
                        innerDepth3 = packageR;
                        packageParser2 = packageParser;
                    } else {
                        int type2;
                        CachedComponentArgs cachedArgs = new CachedComponentArgs();
                        String tagName = parser.getName();
                        int i4;
                        if (tagName.equals(Context.ACTIVITY_SERVICE)) {
                            String tagName2 = tagName;
                            i3 = innerDepth2;
                            i4 = i2;
                            i4 = i;
                            str = classLoaderName;
                            ComponentInfo parsedComponent = packageParser.parseActivity(packageR, resources, attributeSet, flags, strArr, cachedArgs, false, packageR.baseHardwareAccelerated);
                            if (parsedComponent == null) {
                                packageParser.mParseError = i4;
                                return false;
                            }
                            innerDepth = 0;
                            packageR.activities.add(parsedComponent);
                            type2 = parsedComponent.info;
                            i2 = i4;
                            strArr2 = strArr;
                            classLoaderName = attributeSet;
                            resources2 = resources;
                            innerDepth3 = packageR;
                            packageParser2 = packageParser;
                            tagName = tagName2;
                        } else {
                            z3 = type;
                            i3 = innerDepth2;
                            innerDepth = i2;
                            i4 = i;
                            str = classLoaderName;
                            if (tagName.equals(HwFrameworkMonitor.KEY_RECEIVER)) {
                                boolean z4 = z2;
                                i2 = i4;
                                innerDepth3 = packageR;
                                packageParser2 = packageParser;
                                type2 = packageParser.parseActivity(packageR, resources, attributeSet, flags, outError, cachedArgs, true, false);
                                if (type2 == 0) {
                                    packageParser2.mParseError = i2;
                                    return innerDepth;
                                }
                                innerDepth3.receivers.add(type2);
                                type2 = type2.info;
                            } else {
                                i2 = i4;
                                innerDepth3 = packageR;
                                packageParser2 = packageParser;
                                if (tagName.equals(Notification.CATEGORY_SERVICE) != 0) {
                                    type2 = packageParser2.parseService(innerDepth3, res, parser, flags, outError, cachedArgs);
                                    if (type2 == 0) {
                                        packageParser2.mParseError = i2;
                                        return innerDepth;
                                    }
                                    innerDepth3.services.add(type2);
                                    type2 = type2.info;
                                } else if (tagName.equals("provider") != 0) {
                                    type2 = packageParser2.parseProvider(innerDepth3, res, parser, flags, outError, cachedArgs);
                                    if (type2 == 0) {
                                        packageParser2.mParseError = i2;
                                        return innerDepth;
                                    }
                                    innerDepth3.providers.add(type2);
                                    type2 = type2.info;
                                } else if (tagName.equals("activity-alias") != 0) {
                                    type2 = packageParser2.parseActivityAlias(innerDepth3, res, parser, flags, outError, cachedArgs);
                                    if (type2 == 0) {
                                        packageParser2.mParseError = i2;
                                        return innerDepth;
                                    }
                                    innerDepth3.activities.add(type2);
                                    type2 = type2.info;
                                } else {
                                    if (parser.getName().equals("meta-data") != 0) {
                                        resources2 = res;
                                        classLoaderName = parser;
                                        strArr2 = outError;
                                        type2 = packageParser2.parseMetaData(resources2, classLoaderName, innerDepth3.mAppMetaData, strArr2);
                                        innerDepth3.mAppMetaData = type2;
                                        if (type2 == 0) {
                                            packageParser2.mParseError = i2;
                                            return innerDepth;
                                        }
                                    }
                                    resources2 = res;
                                    classLoaderName = parser;
                                    strArr2 = outError;
                                    if (tagName.equals("uses-static-library") != 0) {
                                        if (packageParser2.parseUsesStaticLibrary(innerDepth3, resources2, classLoaderName, strArr2) == 0) {
                                            return innerDepth;
                                        }
                                    } else if (tagName.equals("uses-library") != 0) {
                                        type2 = resources2.obtainAttributes(classLoaderName, R.styleable.AndroidManifestUsesLibrary);
                                        String lname = type2.getNonResourceString(innerDepth);
                                        boolean req = type2.getBoolean(1, true);
                                        type2.recycle();
                                        if (lname != null) {
                                            lname = lname.intern();
                                            if (req) {
                                                innerDepth3.usesLibraries = ArrayUtils.add(innerDepth3.usesLibraries, lname);
                                                innerDepth3.usesOptionalLibraries = ArrayUtils.remove(innerDepth3.usesOptionalLibraries, lname);
                                            } else if (!ArrayUtils.contains(innerDepth3.usesLibraries, lname)) {
                                                innerDepth3.usesOptionalLibraries = ArrayUtils.add(innerDepth3.usesOptionalLibraries, lname);
                                            }
                                        }
                                        XmlUtils.skipCurrentTag(parser);
                                        Object obj = type2;
                                    } else if (tagName.equals("uses-package") != 0) {
                                        XmlUtils.skipCurrentTag(parser);
                                    } else {
                                        type2 = TAG;
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Unknown element under <application>: ");
                                        stringBuilder.append(tagName);
                                        stringBuilder.append(" at ");
                                        stringBuilder.append(packageParser2.mArchiveSourcePath);
                                        stringBuilder.append(" ");
                                        stringBuilder.append(parser.getPositionDescription());
                                        Slog.w(type2, stringBuilder.toString());
                                        XmlUtils.skipCurrentTag(parser);
                                    }
                                    type2 = null;
                                }
                            }
                            resources2 = res;
                            classLoaderName = parser;
                            strArr2 = outError;
                        }
                        if (type2 != 0 && type2.splitName == null) {
                            type2.splitName = innerDepth3.splitNames[splitIndex];
                        }
                    }
                    packageParser = packageParser2;
                    packageR = innerDepth3;
                    resources = resources2;
                    Object attributeSet2 = classLoaderName;
                    strArr = strArr2;
                    classLoaderName = str;
                    z2 = true;
                    z = true;
                    i = i2;
                    i2 = innerDepth;
                    innerDepth = i3;
                } else {
                    z3 = type;
                    i3 = innerDepth2;
                    str = classLoaderName;
                    strArr2 = strArr;
                    classLoaderName2 = attributeSet2;
                    resources2 = resources;
                    innerDepth2 = packageR;
                    packageParser2 = packageParser;
                    break;
                }
            }
            return true;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Invalid class loader name: ");
        stringBuilder2.append(classLoaderName);
        strArr[0] = stringBuilder2.toString();
        packageParser.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        return false;
    }

    private static boolean parsePackageItemInfo(Package owner, PackageItemInfo outInfo, String[] outError, String tag, TypedArray sa, boolean nameRequired, int nameRes, int labelRes, int iconRes, int roundIconRes, int logoRes, int bannerRes) {
        Package packageR = owner;
        PackageItemInfo packageItemInfo = outInfo;
        String[] strArr = outError;
        String str = tag;
        TypedArray typedArray = sa;
        if (typedArray == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append(" does not contain any attributes");
            strArr[0] = stringBuilder.toString();
            return false;
        }
        int roundIconVal;
        int iconVal;
        String name = typedArray.getNonConfigurationString(nameRes, 0);
        if (name != null) {
            packageItemInfo.name = buildClassName(packageR.applicationInfo.packageName, name, strArr);
            if (packageItemInfo.name == null) {
                return false;
            }
        } else if (nameRequired) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str);
            stringBuilder2.append(" does not specify android:name");
            strArr[0] = stringBuilder2.toString();
            return false;
        }
        if (Resources.getSystem().getBoolean(R.bool.config_useRoundIcon)) {
            roundIconVal = typedArray.getResourceId(roundIconRes, 0);
        } else {
            int i = roundIconRes;
            roundIconVal = 0;
        }
        if (roundIconVal != 0) {
            packageItemInfo.icon = roundIconVal;
            packageItemInfo.nonLocalizedLabel = null;
            int i2 = iconRes;
        } else {
            iconVal = typedArray.getResourceId(iconRes, 0);
            if (iconVal != 0) {
                packageItemInfo.icon = iconVal;
                packageItemInfo.nonLocalizedLabel = null;
            }
        }
        iconVal = typedArray.getResourceId(logoRes, 0);
        if (iconVal != 0) {
            packageItemInfo.logo = iconVal;
        }
        int bannerVal = typedArray.getResourceId(bannerRes, 0);
        if (bannerVal != 0) {
            packageItemInfo.banner = bannerVal;
        }
        TypedValue v = typedArray.peekValue(labelRes);
        if (v != null) {
            int i3 = v.resourceId;
            packageItemInfo.labelRes = i3;
            if (i3 == 0) {
                packageItemInfo.nonLocalizedLabel = v.coerceToString();
            }
        }
        packageItemInfo.packageName = packageR.packageName;
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:149:0x047c  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x047a  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x0199  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x0190  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x01dd  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x01ee  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x01fe  */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x0210  */
    /* JADX WARNING: Removed duplicated region for block: B:65:0x0221  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x0235  */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x0247  */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x0259  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0257  */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0260  */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x0271  */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x029c  */
    /* JADX WARNING: Removed duplicated region for block: B:92:0x02ad  */
    /* JADX WARNING: Removed duplicated region for block: B:126:0x0404  */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x02b8  */
    /* JADX WARNING: Removed duplicated region for block: B:132:0x0437  */
    /* JADX WARNING: Removed duplicated region for block: B:135:0x044a  */
    /* JADX WARNING: Removed duplicated region for block: B:143:0x0472  */
    /* JADX WARNING: Removed duplicated region for block: B:138:0x0459  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x047a  */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x047c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Activity parseActivity(Package owner, Resources res, XmlResourceParser parser, int flags, String[] outError, CachedComponentArgs cachedArgs, boolean receiver, boolean hardwareAccelerated) throws XmlPullParserException, IOException {
        Package packageR = owner;
        Resources resources = res;
        AttributeSet attributeSet = parser;
        String[] strArr = outError;
        CachedComponentArgs cachedComponentArgs = cachedArgs;
        TypedArray sa = resources.obtainAttributes(attributeSet, R.styleable.AndroidManifestActivity);
        if (cachedComponentArgs.mActivityArgs == null) {
            cachedComponentArgs.mActivityArgs = new ParseComponentArgs(packageR, strArr, 3, 1, 2, 44, 23, 30, this.mSeparateProcesses, 7, 17, 5);
        }
        cachedComponentArgs.mActivityArgs.tag = receiver ? "<receiver>" : "<activity>";
        cachedComponentArgs.mActivityArgs.sa = sa;
        cachedComponentArgs.mActivityArgs.flags = flags;
        Activity a = new Activity(cachedComponentArgs.mActivityArgs, new ActivityInfo());
        if (strArr[0] != null) {
            sa.recycle();
            return null;
        }
        String themeName;
        int i;
        ActivityInfo activityInfo;
        boolean z;
        boolean z2;
        boolean setExported = sa.hasValue(6);
        if (setExported) {
            a.info.exported = sa.getBoolean(6, false);
        }
        a.info.theme = sa.getResourceId(0, 0);
        if ("com.google.android.packageinstaller".equals(packageR.packageName)) {
            if (a.info.theme != 0) {
                themeName = resources.getResourceName(a.info.theme);
                if (themeName.endsWith("AlertDialogActivity")) {
                    a.info.theme = 33951747;
                } else if (themeName.endsWith("GrantPermissions")) {
                    a.info.theme = 33951753;
                } else if (themeName.endsWith("Settings")) {
                    a.info.theme = 33951748;
                } else if (themeName.endsWith("Theme.DeviceDefault.Light.Dialog.NoActionBar")) {
                    a.info.theme = 33951753;
                } else if (themeName.endsWith("Settings.NoActionBar")) {
                    a.info.theme = 33951749;
                } else {
                    a.info.theme = 33951746;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("parseActivity, packageinstaller themeName changes from [");
                stringBuilder.append(themeName);
                stringBuilder.append("] to [");
                stringBuilder.append(resources.getResourceName(a.info.theme));
                stringBuilder.append("]");
                Flog.i(206, stringBuilder.toString());
            } else {
                a.info.theme = 33951746;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("parseActivity, packageinstaller no themeName change to [");
                stringBuilder2.append(resources.getResourceName(a.info.theme));
                stringBuilder2.append("]");
                Flog.i(206, stringBuilder2.toString());
            }
        }
        a.info.uiOptions = sa.getInt(26, a.info.applicationInfo.uiOptions);
        String parentName = sa.getNonConfigurationString(27, 1024);
        if (parentName != null) {
            themeName = buildClassName(a.info.packageName, parentName, strArr);
            if (strArr[0] == null) {
                a.info.parentActivityName = themeName;
            } else {
                String str;
                ActivityInfo activityInfo2;
                ActivityInfo activityInfo3;
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Activity ");
                stringBuilder3.append(a.info.name);
                stringBuilder3.append(" specified invalid parentActivityName ");
                stringBuilder3.append(parentName);
                Log.e(str2, stringBuilder3.toString());
                i = 0;
                strArr[0] = null;
                themeName = sa.getNonConfigurationString(4, i);
                if (themeName != null) {
                    a.info.permission = packageR.applicationInfo.permission;
                } else {
                    a.info.permission = themeName.length() > 0 ? themeName.toString().intern() : null;
                }
                themeName = sa.getNonConfigurationString(8, 1024);
                a.info.taskAffinity = buildTaskAffinityName(packageR.applicationInfo.packageName, packageR.applicationInfo.taskAffinity, themeName, strArr);
                a.info.splitName = sa.getNonConfigurationString(48, 0);
                a.info.flags = 0;
                if (sa.getBoolean(9, false)) {
                    activityInfo = a.info;
                    activityInfo.flags |= 1;
                }
                if (sa.getBoolean(10, false)) {
                    activityInfo = a.info;
                    activityInfo.flags |= 2;
                }
                if (sa.getBoolean(11, false)) {
                    activityInfo = a.info;
                    activityInfo.flags |= 4;
                }
                if (sa.getBoolean(21, false)) {
                    activityInfo = a.info;
                    activityInfo.flags |= 128;
                }
                if (sa.getBoolean(18, false)) {
                    activityInfo = a.info;
                    activityInfo.flags |= 8;
                }
                if (sa.getBoolean(12, false)) {
                    activityInfo = a.info;
                    activityInfo.flags |= 16;
                }
                if (sa.getBoolean(13, false)) {
                    activityInfo = a.info;
                    activityInfo.flags |= 32;
                }
                if (sa.getBoolean(19, (packageR.applicationInfo.flags & 32) == 0)) {
                    activityInfo = a.info;
                    activityInfo.flags |= 64;
                }
                if (sa.getBoolean(22, false)) {
                    activityInfo = a.info;
                    activityInfo.flags |= 256;
                }
                if (sa.getBoolean(29, false) || sa.getBoolean(39, false)) {
                    activityInfo = a.info;
                    activityInfo.flags |= 1024;
                }
                if (sa.getBoolean(24, false)) {
                    activityInfo = a.info;
                    activityInfo.flags |= 2048;
                }
                if (sa.getBoolean(54, false)) {
                    activityInfo = a.info;
                    activityInfo.flags |= 536870912;
                }
                if (receiver) {
                    if (sa.getBoolean(25, hardwareAccelerated)) {
                        activityInfo = a.info;
                        activityInfo.flags |= 512;
                    }
                    str = themeName;
                    a.info.launchMode = sa.getInt(14, 0);
                    a.info.documentLaunchMode = sa.getInt(33, 0);
                    a.info.maxRecents = sa.getInt(34, ActivityManager.getDefaultAppRecentsLimitStatic());
                    a.info.configChanges = getActivityConfigChanges(sa.getInt(16, 0), sa.getInt(47, 0));
                    a.info.softInputMode = sa.getInt(20, 0);
                    a.info.persistableMode = sa.getInteger(32, 0);
                    if (sa.getBoolean(31, false)) {
                        activityInfo2 = a.info;
                        activityInfo2.flags |= Integer.MIN_VALUE;
                    }
                    if (sa.getBoolean(35, false)) {
                        activityInfo2 = a.info;
                        activityInfo2.flags |= 8192;
                    }
                    if (sa.getBoolean(36, false)) {
                        activityInfo2 = a.info;
                        activityInfo2.flags |= 4096;
                    }
                    if (sa.getBoolean(37, false)) {
                        activityInfo2 = a.info;
                        activityInfo2.flags |= 16384;
                    }
                    a.info.screenOrientation = sa.getInt(15, -1);
                    setActivityResizeMode(a.info, sa, packageR);
                    if (sa.getBoolean(41, false)) {
                        activityInfo2 = a.info;
                        activityInfo2.flags |= 4194304;
                    }
                    if (sa.getBoolean(53, false)) {
                        activityInfo2 = a.info;
                        activityInfo2.flags |= 262144;
                    }
                    if (sa.hasValue(50) && sa.getType(50) == 4) {
                        a.setMaxAspectRatio(sa.getFloat(50, 0.0f));
                    }
                    a.info.lockTaskLaunchMode = sa.getInt(38, 0);
                    activityInfo2 = a.info;
                    activityInfo3 = a.info;
                    boolean z3 = sa.getBoolean(42, false);
                    activityInfo3.directBootAware = z3;
                    activityInfo2.encryptionAware = z3;
                    a.info.requestedVrComponent = sa.getString(43);
                    a.info.rotationAnimation = sa.getInt(46, -1);
                    a.info.colorMode = sa.getInt(49, 0);
                    if (sa.getBoolean(51, false)) {
                        activityInfo2 = a.info;
                        activityInfo2.flags |= 8388608;
                    }
                    if (sa.getBoolean(52, false)) {
                        activityInfo2 = a.info;
                        activityInfo2.flags |= 16777216;
                    }
                } else {
                    boolean z4 = hardwareAccelerated;
                    str = themeName;
                    a.info.launchMode = 0;
                    a.info.configChanges = 0;
                    if (sa.getBoolean(28, false)) {
                        activityInfo2 = a.info;
                        activityInfo2.flags |= 1073741824;
                    }
                    activityInfo2 = a.info;
                    activityInfo3 = a.info;
                    z = sa.getBoolean(42, false);
                    activityInfo3.directBootAware = z;
                    activityInfo2.encryptionAware = z;
                }
                if (a.info.directBootAware) {
                    ApplicationInfo applicationInfo = packageR.applicationInfo;
                    applicationInfo.privateFlags |= 256;
                }
                z = sa.getBoolean(45, false);
                if (z) {
                    activityInfo2 = a.info;
                    activityInfo2.flags |= 1048576;
                    packageR.visibleToInstantApps = true;
                }
                sa.recycle();
                TypedArray typedArray;
                if (receiver) {
                    typedArray = 2;
                } else {
                    typedArray = 2;
                    if ((packageR.applicationInfo.privateFlags & 2) != 0 && a.info.processName == packageR.packageName) {
                        z2 = false;
                        strArr[0] = "Heavy-weight applications can not have receivers in main process";
                        if (strArr[z2] != null) {
                            return null;
                        }
                        int outerDepth = parser.getDepth();
                        while (true) {
                            int outerDepth2 = outerDepth;
                            outerDepth = parser.next();
                            int type = outerDepth;
                            int i2;
                            Resources sa2;
                            Package packageR2;
                            String str3;
                            String str4;
                            int i3;
                            String[] strArr2;
                            if (outerDepth == 1) {
                                i2 = outerDepth2;
                                outerDepth2 = attributeSet;
                                sa2 = resources;
                                packageR2 = packageR;
                                str3 = parentName;
                                str4 = str;
                                i3 = type;
                                strArr2 = strArr;
                                break;
                            }
                            i3 = type;
                            TypedArray typedArray2;
                            if (i3 == 3 && parser.getDepth() <= outerDepth2) {
                                typedArray2 = sa;
                                i2 = outerDepth2;
                                outerDepth2 = attributeSet;
                                sa2 = resources;
                                packageR2 = packageR;
                                str3 = parentName;
                                str4 = str;
                                strArr2 = strArr;
                                break;
                            }
                            int i4;
                            if (i3 != 3) {
                                if (i3 == 4) {
                                    int i5 = 4;
                                    typedArray2 = sa;
                                    i2 = outerDepth2;
                                    outerDepth2 = attributeSet;
                                    sa = resources;
                                    packageR2 = packageR;
                                    str3 = parentName;
                                    str4 = str;
                                    strArr2 = strArr;
                                } else {
                                    TypedArray sa3 = sa;
                                    if (parser.getName().equals("intent-filter")) {
                                        str4 = str;
                                        IntentInfo intent = new ActivityIntentInfo(a);
                                        typedArray2 = sa3;
                                        i2 = outerDepth2;
                                        packageR2 = packageR;
                                        if (!parseIntent(resources, attributeSet, true, true, intent, outError)) {
                                            return null;
                                        }
                                        IntentInfo intent2 = intent;
                                        if (intent2.countActions() == null) {
                                            sa = TAG;
                                            StringBuilder stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("No actions in intent filter at ");
                                            stringBuilder4.append(this.mArchiveSourcePath);
                                            stringBuilder4.append(" ");
                                            stringBuilder4.append(parser.getPositionDescription());
                                            Slog.w(sa, stringBuilder4.toString());
                                        } else {
                                            a.order = Math.max(intent2.getOrder(), a.order);
                                            a.intents.add(intent2);
                                        }
                                        sa = z ? true : (receiver || isImplicitlyExposedIntent(intent2) == null) ? z2 : typedArray;
                                        intent2.setVisibilityToInstantApp(sa);
                                        if (intent2.isVisibleToInstantApp()) {
                                            activityInfo3 = a.info;
                                            activityInfo3.flags |= 1048576;
                                        }
                                        if (intent2.isImplicitlyVisibleToInstantApp()) {
                                            activityInfo3 = a.info;
                                            activityInfo3.flags |= 2097152;
                                        }
                                        Object sa4 = res;
                                        outerDepth2 = parser;
                                        strArr2 = outError;
                                        str3 = parentName;
                                    } else {
                                        i2 = outerDepth2;
                                        packageR2 = packageR;
                                        str4 = str;
                                        typedArray2 = sa3;
                                        if (receiver || !parser.getName().equals("preferred")) {
                                            str3 = parentName;
                                            i4 = 1048576;
                                            if (parser.getName().equals("meta-data")) {
                                                sa = res;
                                                outerDepth2 = parser;
                                                strArr2 = outError;
                                                Bundle parseMetaData = parseMetaData(sa, outerDepth2, a.metaData, strArr2);
                                                a.metaData = parseMetaData;
                                                if (parseMetaData == null) {
                                                    return null;
                                                }
                                                HwFrameworkFactory.getHwPackageParser().initMetaData(a);
                                                int themeId = HwWidgetFactory.getThemeId(a.metaData, sa);
                                                if (themeId != 0) {
                                                    a.info.theme = themeId;
                                                }
                                                HwThemeManager.addSimpleUIConfig(a);
                                            } else {
                                                sa = res;
                                                outerDepth2 = parser;
                                                strArr2 = outError;
                                                if (receiver || !parser.getName().equals("layout")) {
                                                    String str5 = TAG;
                                                    StringBuilder stringBuilder5 = new StringBuilder();
                                                    stringBuilder5.append("Problem in package ");
                                                    stringBuilder5.append(this.mArchiveSourcePath);
                                                    stringBuilder5.append(":");
                                                    Slog.w(str5, stringBuilder5.toString());
                                                    if (receiver) {
                                                        str5 = TAG;
                                                        stringBuilder5 = new StringBuilder();
                                                        stringBuilder5.append("Unknown element under <receiver>: ");
                                                        stringBuilder5.append(parser.getName());
                                                        stringBuilder5.append(" at ");
                                                        stringBuilder5.append(this.mArchiveSourcePath);
                                                        stringBuilder5.append(" ");
                                                        stringBuilder5.append(parser.getPositionDescription());
                                                        Slog.w(str5, stringBuilder5.toString());
                                                    } else {
                                                        str5 = TAG;
                                                        stringBuilder5 = new StringBuilder();
                                                        stringBuilder5.append("Unknown element under <activity>: ");
                                                        stringBuilder5.append(parser.getName());
                                                        stringBuilder5.append(" at ");
                                                        stringBuilder5.append(this.mArchiveSourcePath);
                                                        stringBuilder5.append(" ");
                                                        stringBuilder5.append(parser.getPositionDescription());
                                                        Slog.w(str5, stringBuilder5.toString());
                                                    }
                                                    XmlUtils.skipCurrentTag(parser);
                                                } else {
                                                    parseLayout(sa, outerDepth2, a);
                                                }
                                            }
                                        } else {
                                            IntentInfo intent3 = new ActivityIntentInfo(a);
                                            str3 = parentName;
                                            IntentInfo parentName2 = intent3;
                                            if (!parseIntent(res, parser, false, false, intent3, outError)) {
                                                return null;
                                            }
                                            if (parentName2.countActions() == 0) {
                                                themeName = TAG;
                                                sa = new StringBuilder();
                                                sa.append("No actions in preferred at ");
                                                sa.append(this.mArchiveSourcePath);
                                                sa.append(" ");
                                                sa.append(parser.getPositionDescription());
                                                Slog.w(themeName, sa.toString());
                                            } else {
                                                if (packageR2.preferredActivityFilters == null) {
                                                    packageR2.preferredActivityFilters = new ArrayList();
                                                }
                                                packageR2.preferredActivityFilters.add(parentName2);
                                            }
                                            outerDepth = z ? 1 : (receiver || !isImplicitlyExposedIntent(parentName2)) ? z2 : typedArray;
                                            parentName2.setVisibilityToInstantApp(outerDepth);
                                            if (parentName2.isVisibleToInstantApp() != null) {
                                                sa = a.info;
                                                i4 = 1048576;
                                                sa.flags |= 1048576;
                                            } else {
                                                i4 = 1048576;
                                            }
                                            if (parentName2.isImplicitlyVisibleToInstantApp() != null) {
                                                sa = a.info;
                                                sa.flags |= 2097152;
                                            }
                                            sa = res;
                                            outerDepth2 = parser;
                                            strArr2 = outError;
                                        }
                                    }
                                }
                                i4 = 1048576;
                            } else {
                                typedArray2 = sa;
                                i2 = outerDepth2;
                                outerDepth2 = attributeSet;
                                sa = resources;
                                packageR2 = packageR;
                                str3 = parentName;
                                str4 = str;
                                strArr2 = strArr;
                                i4 = 1048576;
                            }
                            Object resources2 = sa;
                            i3 = i4;
                            strArr = strArr2;
                            packageR = packageR2;
                            str = str4;
                            sa = typedArray2;
                            outerDepth = i2;
                            parentName = str3;
                            int i6 = flags;
                            attributeSet = outerDepth2;
                        }
                        if (!setExported) {
                            a.info.exported = a.intents.size() > 0 ? true : z2;
                        }
                        return a;
                    }
                }
                z2 = false;
                if (strArr[z2] != null) {
                }
            }
        }
        i = 0;
        themeName = sa.getNonConfigurationString(4, i);
        if (themeName != null) {
        }
        themeName = sa.getNonConfigurationString(8, 1024);
        a.info.taskAffinity = buildTaskAffinityName(packageR.applicationInfo.packageName, packageR.applicationInfo.taskAffinity, themeName, strArr);
        a.info.splitName = sa.getNonConfigurationString(48, 0);
        a.info.flags = 0;
        if (sa.getBoolean(9, false)) {
        }
        if (sa.getBoolean(10, false)) {
        }
        if (sa.getBoolean(11, false)) {
        }
        if (sa.getBoolean(21, false)) {
        }
        if (sa.getBoolean(18, false)) {
        }
        if (sa.getBoolean(12, false)) {
        }
        if (sa.getBoolean(13, false)) {
        }
        if ((packageR.applicationInfo.flags & 32) == 0) {
        }
        if (sa.getBoolean(19, (packageR.applicationInfo.flags & 32) == 0)) {
        }
        if (sa.getBoolean(22, false)) {
        }
        activityInfo = a.info;
        activityInfo.flags |= 1024;
        if (sa.getBoolean(24, false)) {
        }
        if (sa.getBoolean(54, false)) {
        }
        if (receiver) {
        }
        if (a.info.directBootAware) {
        }
        z = sa.getBoolean(45, false);
        if (z) {
        }
        sa.recycle();
        if (receiver) {
        }
        z2 = false;
        if (strArr[z2] != null) {
        }
    }

    private void setActivityResizeMode(ActivityInfo aInfo, TypedArray sa, Package owner) {
        boolean appResizeable = true;
        boolean appExplicitDefault = (owner.applicationInfo.privateFlags & 3072) != 0;
        if (sa.hasValue(40) || appExplicitDefault) {
            if ((owner.applicationInfo.privateFlags & 1024) == 0) {
                appResizeable = false;
            }
            if (sa.getBoolean(40, appResizeable)) {
                aInfo.resizeMode = 2;
            } else {
                aInfo.resizeMode = 0;
            }
        } else if ((owner.applicationInfo.privateFlags & 4096) != 0) {
            aInfo.resizeMode = 1;
        } else {
            if (aInfo.isFixedOrientationPortrait()) {
                aInfo.resizeMode = 6;
            } else if (aInfo.isFixedOrientationLandscape()) {
                aInfo.resizeMode = 5;
            } else if (aInfo.isFixedOrientation()) {
                aInfo.resizeMode = 7;
            } else {
                aInfo.resizeMode = 4;
            }
        }
    }

    private void setMaxAspectRatio(Package owner) {
        float maxAspectRatio = owner.applicationInfo.targetSdkVersion < 26 ? mDefaultMaxAspectRatio : 0.0f;
        if (owner.applicationInfo.maxAspectRatio != 0.0f) {
            maxAspectRatio = owner.applicationInfo.maxAspectRatio;
        } else if (owner.mAppMetaData != null && owner.mAppMetaData.containsKey(METADATA_MAX_ASPECT_RATIO)) {
            maxAspectRatio = owner.mAppMetaData.getFloat(METADATA_MAX_ASPECT_RATIO, maxAspectRatio);
        }
        Iterator it = owner.activities.iterator();
        while (it.hasNext()) {
            Activity activity = (Activity) it.next();
            if (HwFrameworkFactory.getHwPackageParser().isDefaultFullScreen(activity.info.packageName)) {
                activity.info.maxAspectRatio = 0.0f;
            } else if (!activity.hasMaxAspectRatio()) {
                float activityAspectRatio;
                if (activity.metaData != null) {
                    activityAspectRatio = activity.metaData.getFloat(METADATA_MAX_ASPECT_RATIO, maxAspectRatio);
                } else {
                    activityAspectRatio = maxAspectRatio;
                }
                activity.setMaxAspectRatio(activityAspectRatio);
            }
        }
    }

    public static int getActivityConfigChanges(int configChanges, int recreateOnConfigChanges) {
        return ((~recreateOnConfigChanges) & 3) | configChanges;
    }

    private void parseLayout(Resources res, AttributeSet attrs, Activity a) {
        TypedArray sw = res.obtainAttributes(attrs, R.styleable.AndroidManifestLayout);
        int width = -1;
        float widthFraction = -1.0f;
        int height = -1;
        float heightFraction = -1.0f;
        int widthType = sw.getType(3);
        if (widthType == 6) {
            widthFraction = sw.getFraction(3, 1, 1, -1.0f);
        } else if (widthType == 5) {
            width = sw.getDimensionPixelSize(3, -1);
        }
        int heightType = sw.getType(4);
        if (heightType == 6) {
            heightFraction = sw.getFraction(4, 1, 1, -1.0f);
        } else if (heightType == 5) {
            height = sw.getDimensionPixelSize(4, -1);
        }
        int gravity = sw.getInt(0, 17);
        int minWidth = sw.getDimensionPixelSize(1, -1);
        int minHeight = sw.getDimensionPixelSize(2, -1);
        sw.recycle();
        a.info.windowLayout = new WindowLayout(width, widthFraction, height, heightFraction, gravity, minWidth, minHeight);
    }

    private Activity parseActivityAlias(Package owner, Resources res, XmlResourceParser parser, int flags, String[] outError, CachedComponentArgs cachedArgs) throws XmlPullParserException, IOException {
        Package packageR = owner;
        Resources resources = res;
        AttributeSet attributeSet = parser;
        String[] strArr = outError;
        CachedComponentArgs cachedComponentArgs = cachedArgs;
        TypedArray sa = resources.obtainAttributes(attributeSet, R.styleable.AndroidManifestActivityAlias);
        String targetActivity = sa.getNonConfigurationString(7, 1024);
        if (targetActivity == null) {
            strArr[0] = "<activity-alias> does not specify android:targetActivity";
            sa.recycle();
            return null;
        }
        String targetActivity2 = buildClassName(packageR.applicationInfo.packageName, targetActivity, strArr);
        if (targetActivity2 == null) {
            sa.recycle();
            return null;
        }
        String targetActivity3;
        String targetActivity4;
        Activity target;
        if (cachedComponentArgs.mActivityAliasArgs == null) {
            ParseComponentArgs parseComponentArgs = r8;
            targetActivity3 = targetActivity2;
            ParseComponentArgs parseComponentArgs2 = new ParseComponentArgs(packageR, strArr, 2, 0, 1, 11, 8, 10, this.mSeparateProcesses, 0, 6, 4);
            cachedComponentArgs.mActivityAliasArgs = parseComponentArgs;
            cachedComponentArgs.mActivityAliasArgs.tag = "<activity-alias>";
        } else {
            targetActivity3 = targetActivity2;
        }
        cachedComponentArgs.mActivityAliasArgs.sa = sa;
        cachedComponentArgs.mActivityAliasArgs.flags = flags;
        int NA = packageR.activities.size();
        int i = 0;
        while (i < NA) {
            Activity t = (Activity) packageR.activities.get(i);
            targetActivity4 = targetActivity3;
            if (targetActivity4.equals(t.info.name)) {
                target = t;
                break;
            }
            i++;
            targetActivity3 = targetActivity4;
        }
        targetActivity4 = targetActivity3;
        target = null;
        if (target == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<activity-alias> target activity ");
            stringBuilder.append(targetActivity4);
            stringBuilder.append(" not found in manifest");
            strArr[0] = stringBuilder.toString();
            sa.recycle();
            return null;
        }
        ActivityInfo info = new ActivityInfo();
        info.targetActivity = targetActivity4;
        info.configChanges = target.info.configChanges;
        info.flags = target.info.flags;
        info.icon = target.info.icon;
        info.logo = target.info.logo;
        info.banner = target.info.banner;
        info.labelRes = target.info.labelRes;
        info.nonLocalizedLabel = target.info.nonLocalizedLabel;
        info.launchMode = target.info.launchMode;
        info.lockTaskLaunchMode = target.info.lockTaskLaunchMode;
        info.processName = target.info.processName;
        if (info.descriptionRes == 0) {
            info.descriptionRes = target.info.descriptionRes;
        }
        info.screenOrientation = target.info.screenOrientation;
        info.taskAffinity = target.info.taskAffinity;
        info.theme = target.info.theme;
        info.softInputMode = target.info.softInputMode;
        info.uiOptions = target.info.uiOptions;
        info.parentActivityName = target.info.parentActivityName;
        info.maxRecents = target.info.maxRecents;
        info.windowLayout = target.info.windowLayout;
        info.resizeMode = target.info.resizeMode;
        info.maxAspectRatio = target.info.maxAspectRatio;
        info.requestedVrComponent = target.info.requestedVrComponent;
        boolean z = target.info.directBootAware;
        info.directBootAware = z;
        info.encryptionAware = z;
        Activity a = new Activity(cachedComponentArgs.mActivityAliasArgs, info);
        if (strArr[0] != null) {
            sa.recycle();
            return null;
        }
        String str;
        boolean setExported = sa.hasValue(5);
        if (setExported) {
            a.info.exported = sa.getBoolean(5, false);
        }
        String str2 = sa.getNonConfigurationString(3, 0);
        if (str2 != null) {
            a.info.permission = str2.length() > 0 ? str2.toString().intern() : null;
        }
        String parentName = sa.getNonConfigurationString(9, 1024);
        if (parentName != null) {
            String parentClassName = buildClassName(a.info.packageName, parentName, strArr);
            if (strArr[0] == null) {
                str = str2;
                a.info.parentActivityName = parentClassName;
            } else {
                str = str2;
                str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Activity alias ");
                stringBuilder2.append(a.info.name);
                stringBuilder2.append(" specified invalid parentActivityName ");
                stringBuilder2.append(parentName);
                Log.e(str2, stringBuilder2.toString());
                strArr[0] = null;
            }
        } else {
            str = str2;
        }
        boolean z2 = true;
        boolean visibleToEphemeral = (a.info.flags & 1048576) != 0;
        sa.recycle();
        if (strArr[0] != null) {
            return null;
        }
        boolean z3;
        int outerDepth = parser.getDepth();
        while (true) {
            int outerDepth2 = outerDepth;
            z = parser.next();
            boolean type = z;
            int i2;
            Resources sa2;
            String str3;
            boolean z4;
            if (z == z2) {
                i2 = outerDepth2;
                outerDepth2 = attributeSet;
                sa2 = resources;
                z3 = z2;
                str3 = str;
                z4 = type;
                break;
            }
            z = type;
            TypedArray typedArray;
            if (z && parser.getDepth() <= outerDepth2) {
                z4 = z;
                typedArray = sa;
                i2 = outerDepth2;
                outerDepth2 = attributeSet;
                sa2 = resources;
                str3 = str;
                z3 = true;
                break;
            }
            XmlResourceParser outerDepth3;
            if (z) {
                typedArray = sa;
                i2 = outerDepth2;
                outerDepth3 = attributeSet;
                sa = resources;
                int i3 = 3;
                str3 = str;
                z3 = true;
            } else if (z) {
                typedArray = sa;
                i2 = outerDepth2;
                outerDepth3 = attributeSet;
                sa = resources;
                str3 = str;
                z3 = true;
            } else {
                boolean type2 = z;
                if (parser.getName().equals("intent-filter")) {
                    IntentInfo intent = new ActivityIntentInfo(a);
                    str3 = str;
                    typedArray = sa;
                    i2 = outerDepth2;
                    IntentInfo intent2 = intent;
                    z3 = true;
                    if (!parseIntent(resources, attributeSet, true, true, intent, outError)) {
                        return null;
                    }
                    ActivityInfo activityInfo;
                    if (intent2.countActions() == 0) {
                        str2 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("No actions in intent filter at ");
                        stringBuilder3.append(this.mArchiveSourcePath);
                        stringBuilder3.append(" ");
                        stringBuilder3.append(parser.getPositionDescription());
                        Slog.w(str2, stringBuilder3.toString());
                    } else {
                        a.order = Math.max(intent2.getOrder(), a.order);
                        a.intents.add(intent2);
                    }
                    outerDepth = visibleToEphemeral ? 1 : isImplicitlyExposedIntent(intent2) ? 2 : 0;
                    intent2.setVisibilityToInstantApp(outerDepth);
                    if (intent2.isVisibleToInstantApp()) {
                        activityInfo = a.info;
                        activityInfo.flags |= 1048576;
                    }
                    if (intent2.isImplicitlyVisibleToInstantApp()) {
                        activityInfo = a.info;
                        activityInfo.flags |= 2097152;
                    }
                    outerDepth3 = parser;
                    strArr = outError;
                    sa = res;
                } else {
                    typedArray = sa;
                    i2 = outerDepth2;
                    str3 = str;
                    z4 = type2;
                    z3 = true;
                    if (parser.getName().equals("meta-data")) {
                        outerDepth3 = parser;
                        sa = res;
                        Bundle parseMetaData = parseMetaData(sa, outerDepth3, a.metaData, outError);
                        a.metaData = parseMetaData;
                        if (parseMetaData == null) {
                            return null;
                        }
                        HwThemeManager.addSimpleUIConfig(a);
                    } else {
                        outerDepth3 = parser;
                        strArr = outError;
                        sa = res;
                        str2 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Unknown element under <activity-alias>: ");
                        stringBuilder4.append(parser.getName());
                        stringBuilder4.append(" at ");
                        stringBuilder4.append(this.mArchiveSourcePath);
                        stringBuilder4.append(" ");
                        stringBuilder4.append(parser.getPositionDescription());
                        Slog.w(str2, stringBuilder4.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
            Object resources2 = sa;
            Object attributeSet2 = outerDepth3;
            z2 = z3;
            str = str3;
            sa = typedArray;
            outerDepth = i2;
            int i4 = flags;
        }
        if (!setExported) {
            a.info.exported = a.intents.size() > 0 ? z3 : false;
        }
        return a;
    }

    private Provider parseProvider(Package owner, Resources res, XmlResourceParser parser, int flags, String[] outError, CachedComponentArgs cachedArgs) throws XmlPullParserException, IOException {
        TypedArray sa;
        Package packageR = owner;
        CachedComponentArgs cachedComponentArgs = cachedArgs;
        TypedArray sa2 = res.obtainAttributes(parser, R.styleable.AndroidManifestProvider);
        if (cachedComponentArgs.mProviderArgs == null) {
            ParseComponentArgs parseComponentArgs = r0;
            sa = sa2;
            ParseComponentArgs parseComponentArgs2 = new ParseComponentArgs(packageR, outError, 2, 0, 1, 19, 15, 17, this.mSeparateProcesses, 8, 14, 6);
            cachedComponentArgs.mProviderArgs = parseComponentArgs;
            cachedComponentArgs.mProviderArgs.tag = "<provider>";
        } else {
            sa = sa2;
        }
        TypedArray sa3 = sa;
        cachedComponentArgs.mProviderArgs.sa = sa3;
        cachedComponentArgs.mProviderArgs.flags = flags;
        Provider p = new Provider(cachedComponentArgs.mProviderArgs, new ProviderInfo());
        if (outError[0] != null) {
            sa3.recycle();
            return null;
        }
        ProviderInfo providerInfo;
        boolean providerExportedDefault = false;
        if (packageR.applicationInfo.targetSdkVersion < 17) {
            providerExportedDefault = true;
        }
        p.info.exported = sa3.getBoolean(7, providerExportedDefault);
        String cpname = sa3.getNonConfigurationString(10, 0);
        p.info.isSyncable = sa3.getBoolean(11, false);
        String permission = sa3.getNonConfigurationString(3, 0);
        String str = sa3.getNonConfigurationString(4, 0);
        if (str == null) {
            str = permission;
        }
        if (str == null) {
            p.info.readPermission = packageR.applicationInfo.permission;
        } else {
            p.info.readPermission = str.length() > 0 ? str.toString().intern() : null;
        }
        str = sa3.getNonConfigurationString(5, 0);
        if (str == null) {
            str = permission;
        }
        String str2 = str;
        if (str2 == null) {
            p.info.writePermission = packageR.applicationInfo.permission;
        } else {
            p.info.writePermission = str2.length() > 0 ? str2.toString().intern() : null;
        }
        p.info.grantUriPermissions = sa3.getBoolean(13, false);
        p.info.multiprocess = sa3.getBoolean(9, false);
        p.info.initOrder = sa3.getInt(12, 0);
        p.info.splitName = sa3.getNonConfigurationString(21, 0);
        p.info.flags = 0;
        if (sa3.getBoolean(16, false)) {
            providerInfo = p.info;
            providerInfo.flags |= 1073741824;
        }
        providerInfo = p.info;
        ProviderInfo providerInfo2 = p.info;
        boolean z = sa3.getBoolean(18, false);
        providerInfo2.directBootAware = z;
        providerInfo.encryptionAware = z;
        if (p.info.directBootAware) {
            ApplicationInfo applicationInfo = packageR.applicationInfo;
            applicationInfo.privateFlags |= 256;
        }
        boolean visibleToEphemeral = sa3.getBoolean(20, false);
        if (visibleToEphemeral) {
            providerInfo = p.info;
            providerInfo.flags |= 1048576;
            packageR.visibleToInstantApps = true;
        }
        sa3.recycle();
        if ((packageR.applicationInfo.privateFlags & 2) != 0 && p.info.processName == packageR.packageName) {
            outError[0] = "Heavy-weight applications can not have providers in main process";
            return null;
        } else if (cpname == null) {
            outError[0] = "<provider> does not include authorities attribute";
            return null;
        } else if (cpname.length() <= 0) {
            outError[0] = "<provider> has empty authorities attribute";
            return null;
        } else {
            p.info.authority = cpname.intern();
            if (parseProviderTags(res, parser, visibleToEphemeral, p, outError)) {
                return p;
            }
            return null;
        }
    }

    private boolean parseProviderTags(Resources res, XmlResourceParser parser, boolean visibleToEphemeral, Provider outInfo, String[] outError) throws XmlPullParserException, IOException {
        String[] strArr;
        Resources resources = res;
        XmlResourceParser xmlResourceParser = parser;
        Provider provider = outInfo;
        int outerDepth = parser.getDepth();
        while (true) {
            int outerDepth2 = outerDepth;
            outerDepth = parser.next();
            int type = outerDepth;
            if (outerDepth == 1 || (type == 3 && parser.getDepth() <= outerDepth2)) {
                strArr = outError;
            } else {
                if (!(type == 3 || type == 4)) {
                    if (parser.getName().equals("intent-filter")) {
                        IntentInfo intent = new ProviderIntentInfo(provider);
                        if (!parseIntent(resources, xmlResourceParser, true, false, intent, outError)) {
                            return false;
                        }
                        if (visibleToEphemeral) {
                            intent.setVisibilityToInstantApp(1);
                            ProviderInfo providerInfo = provider.info;
                            providerInfo.flags |= 1048576;
                        }
                        provider.order = Math.max(intent.getOrder(), provider.order);
                        provider.intents.add(intent);
                    } else {
                        if (parser.getName().equals("meta-data")) {
                            Bundle parseMetaData = parseMetaData(resources, xmlResourceParser, provider.metaData, outError);
                            provider.metaData = parseMetaData;
                            if (parseMetaData == null) {
                                return false;
                            }
                        } else {
                            strArr = outError;
                            String str;
                            String path;
                            StringBuilder stringBuilder;
                            if (parser.getName().equals("grant-uri-permission")) {
                                TypedArray sa = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestGrantUriPermission);
                                PatternMatcher pa = null;
                                String str2 = sa.getNonConfigurationString(0, 0);
                                if (str2 != null) {
                                    pa = new PatternMatcher(str2, 0);
                                }
                                str2 = sa.getNonConfigurationString(1, 0);
                                if (str2 != null) {
                                    pa = new PatternMatcher(str2, 1);
                                }
                                str2 = sa.getNonConfigurationString(2, 0);
                                if (str2 != null) {
                                    pa = new PatternMatcher(str2, 2);
                                }
                                sa.recycle();
                                if (pa != null) {
                                    if (provider.info.uriPermissionPatterns == null) {
                                        provider.info.uriPermissionPatterns = new PatternMatcher[1];
                                        provider.info.uriPermissionPatterns[0] = pa;
                                    } else {
                                        int N = provider.info.uriPermissionPatterns.length;
                                        PatternMatcher[] newp = new PatternMatcher[(N + 1)];
                                        System.arraycopy(provider.info.uriPermissionPatterns, 0, newp, 0, N);
                                        newp[N] = pa;
                                        provider.info.uriPermissionPatterns = newp;
                                    }
                                    provider.info.grantUriPermissions = true;
                                    XmlUtils.skipCurrentTag(parser);
                                } else {
                                    str = TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Unknown element under <path-permission>: ");
                                    stringBuilder2.append(parser.getName());
                                    stringBuilder2.append(" at ");
                                    stringBuilder2.append(this.mArchiveSourcePath);
                                    stringBuilder2.append(" ");
                                    stringBuilder2.append(parser.getPositionDescription());
                                    Slog.w(str, stringBuilder2.toString());
                                    XmlUtils.skipCurrentTag(parser);
                                }
                            } else if (parser.getName().equals("path-permission")) {
                                TypedArray sa2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestPathPermission);
                                PathPermission pa2 = null;
                                String permission = sa2.getNonConfigurationString(0, 0);
                                String readPermission = sa2.getNonConfigurationString(1, 0);
                                if (readPermission == null) {
                                    readPermission = permission;
                                }
                                String readPermission2 = readPermission;
                                readPermission = sa2.getNonConfigurationString(2, 0);
                                if (readPermission == null) {
                                    readPermission = permission;
                                }
                                str = readPermission;
                                boolean havePerm = false;
                                if (readPermission2 != null) {
                                    readPermission2 = readPermission2.intern();
                                    havePerm = true;
                                }
                                if (str != null) {
                                    str = str.intern();
                                    havePerm = true;
                                }
                                if (havePerm) {
                                    String path2 = sa2.getNonConfigurationString(3, 0);
                                    if (path2 != null) {
                                        pa2 = new PathPermission(path2, 0, readPermission2, str);
                                    }
                                    path = sa2.getNonConfigurationString(4, 0);
                                    if (path != null) {
                                        pa2 = new PathPermission(path, 1, readPermission2, str);
                                    }
                                    path = sa2.getNonConfigurationString(5, 0);
                                    if (path != null) {
                                        pa2 = new PathPermission(path, 2, readPermission2, str);
                                    }
                                    path = sa2.getNonConfigurationString(6, 0);
                                    if (path != null) {
                                        pa2 = new PathPermission(path, 3, readPermission2, str);
                                    }
                                    sa2.recycle();
                                    String str3;
                                    if (pa2 != null) {
                                        if (provider.info.pathPermissions == null) {
                                            provider.info.pathPermissions = new PathPermission[1];
                                            provider.info.pathPermissions[0] = pa2;
                                            str3 = path;
                                        } else {
                                            int N2 = provider.info.pathPermissions.length;
                                            PathPermission[] newp2 = new PathPermission[(N2 + 1)];
                                            System.arraycopy(provider.info.pathPermissions, 0, newp2, 0, N2);
                                            newp2[N2] = pa2;
                                            provider.info.pathPermissions = newp2;
                                        }
                                        XmlUtils.skipCurrentTag(parser);
                                    } else {
                                        str3 = path;
                                        path = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("No path, pathPrefix, or pathPattern for <path-permission>: ");
                                        stringBuilder.append(parser.getName());
                                        stringBuilder.append(" at ");
                                        stringBuilder.append(this.mArchiveSourcePath);
                                        stringBuilder.append(" ");
                                        stringBuilder.append(parser.getPositionDescription());
                                        Slog.w(path, stringBuilder.toString());
                                        XmlUtils.skipCurrentTag(parser);
                                    }
                                } else {
                                    path = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("No readPermission or writePermssion for <path-permission>: ");
                                    stringBuilder.append(parser.getName());
                                    stringBuilder.append(" at ");
                                    stringBuilder.append(this.mArchiveSourcePath);
                                    stringBuilder.append(" ");
                                    stringBuilder.append(parser.getPositionDescription());
                                    Slog.w(path, stringBuilder.toString());
                                    XmlUtils.skipCurrentTag(parser);
                                }
                            } else {
                                path = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown element under <provider>: ");
                                stringBuilder.append(parser.getName());
                                stringBuilder.append(" at ");
                                stringBuilder.append(this.mArchiveSourcePath);
                                stringBuilder.append(" ");
                                stringBuilder.append(parser.getPositionDescription());
                                Slog.w(path, stringBuilder.toString());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                        outerDepth = outerDepth2;
                    }
                }
                strArr = outError;
                outerDepth = outerDepth2;
            }
        }
        strArr = outError;
        return true;
    }

    private Service parseService(Package owner, Resources res, XmlResourceParser parser, int flags, String[] outError, CachedComponentArgs cachedArgs) throws XmlPullParserException, IOException {
        Package packageR = owner;
        Resources resources = res;
        AttributeSet attributeSet = parser;
        String[] strArr = outError;
        CachedComponentArgs cachedComponentArgs = cachedArgs;
        TypedArray sa = resources.obtainAttributes(attributeSet, R.styleable.AndroidManifestService);
        if (cachedComponentArgs.mServiceArgs == null) {
            cachedComponentArgs.mServiceArgs = new ParseComponentArgs(packageR, strArr, 2, 0, 1, 15, 8, 12, this.mSeparateProcesses, 6, 7, 4);
            cachedComponentArgs.mServiceArgs.tag = "<service>";
        }
        cachedComponentArgs.mServiceArgs.sa = sa;
        cachedComponentArgs.mServiceArgs.flags = flags;
        Service s = new Service(cachedComponentArgs.mServiceArgs, new ServiceInfo());
        if (strArr[0] != null) {
            sa.recycle();
            return null;
        }
        ServiceInfo serviceInfo;
        boolean setExported = sa.hasValue(5);
        if (setExported) {
            s.info.exported = sa.getBoolean(5, false);
        }
        String str = sa.getNonConfigurationString(3, 0);
        if (str == null) {
            s.info.permission = packageR.applicationInfo.permission;
        } else {
            s.info.permission = str.length() > 0 ? str.toString().intern() : null;
        }
        s.info.splitName = sa.getNonConfigurationString(17, 0);
        s.info.flags = 0;
        boolean z = true;
        if (sa.getBoolean(9, false)) {
            serviceInfo = s.info;
            serviceInfo.flags |= 1;
        }
        if (sa.getBoolean(10, false)) {
            serviceInfo = s.info;
            serviceInfo.flags |= 2;
        }
        if (sa.getBoolean(14, false)) {
            serviceInfo = s.info;
            serviceInfo.flags |= 4;
        }
        if (sa.getBoolean(11, false)) {
            serviceInfo = s.info;
            serviceInfo.flags |= 1073741824;
        }
        serviceInfo = s.info;
        ServiceInfo serviceInfo2 = s.info;
        boolean z2 = sa.getBoolean(13, false);
        serviceInfo2.directBootAware = z2;
        serviceInfo.encryptionAware = z2;
        if (s.info.directBootAware) {
            ApplicationInfo applicationInfo = packageR.applicationInfo;
            applicationInfo.privateFlags |= 256;
        }
        boolean visibleToEphemeral = sa.getBoolean(16, false);
        if (visibleToEphemeral) {
            serviceInfo = s.info;
            serviceInfo.flags |= 1048576;
            packageR.visibleToInstantApps = true;
        }
        sa.recycle();
        if ((packageR.applicationInfo.privateFlags & 2) == 0 || s.info.processName != packageR.packageName) {
            boolean outerDepth;
            ServiceInfo serviceInfo3;
            int outerDepth2 = parser.getDepth();
            while (true) {
                boolean next = parser.next();
                boolean type = next;
                TypedArray typedArray;
                Resources resources2;
                AttributeSet attributeSet2;
                String[] strArr2;
                if (next == z) {
                    typedArray = sa;
                    resources2 = resources;
                    outerDepth = z;
                    next = type;
                    attributeSet2 = attributeSet;
                    strArr2 = strArr;
                    break;
                }
                next = type;
                int i;
                if (next && parser.getDepth() <= outerDepth2) {
                    i = outerDepth2;
                    typedArray = sa;
                    attributeSet2 = attributeSet;
                    resources2 = resources;
                    outerDepth = true;
                    strArr2 = strArr;
                    break;
                }
                XmlResourceParser xmlResourceParser;
                int i2;
                if (next) {
                    i = outerDepth2;
                    typedArray = sa;
                    xmlResourceParser = attributeSet;
                    resources2 = resources;
                    outerDepth = true;
                    strArr2 = strArr;
                    i2 = 1048576;
                } else if (next) {
                    i = outerDepth2;
                    typedArray = sa;
                    xmlResourceParser = attributeSet;
                    resources2 = resources;
                    outerDepth = true;
                    strArr2 = strArr;
                    i2 = 1048576;
                } else if (parser.getName().equals("intent-filter")) {
                    IntentInfo intent = new ServiceIntentInfo(s);
                    i = outerDepth2;
                    typedArray = sa;
                    xmlResourceParser = attributeSet;
                    if (!parseIntent(resources, attributeSet, true, false, intent, outError)) {
                        return null;
                    }
                    if (visibleToEphemeral) {
                        outerDepth = true;
                        intent.setVisibilityToInstantApp(1);
                        serviceInfo3 = s.info;
                        i2 = 1048576;
                        serviceInfo3.flags |= 1048576;
                    } else {
                        outerDepth = true;
                        i2 = 1048576;
                    }
                    s.order = Math.max(intent.getOrder(), s.order);
                    s.intents.add(intent);
                    strArr2 = outError;
                    resources2 = res;
                } else {
                    i = outerDepth2;
                    typedArray = sa;
                    xmlResourceParser = attributeSet;
                    outerDepth = true;
                    i2 = 1048576;
                    if (parser.getName().equals("meta-data")) {
                        strArr2 = outError;
                        resources2 = res;
                        Bundle parseMetaData = parseMetaData(resources2, xmlResourceParser, s.metaData, strArr2);
                        s.metaData = parseMetaData;
                        if (parseMetaData == null) {
                            return null;
                        }
                    } else {
                        strArr2 = outError;
                        resources2 = res;
                        String str2 = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown element under <service>: ");
                        stringBuilder.append(parser.getName());
                        stringBuilder.append(" at ");
                        stringBuilder.append(this.mArchiveSourcePath);
                        stringBuilder.append(" ");
                        stringBuilder.append(parser.getPositionDescription());
                        Slog.w(str2, stringBuilder.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
                packageR = owner;
                resources = resources2;
                int i3 = i2;
                strArr = strArr2;
                Object attributeSet3 = xmlResourceParser;
                sa = typedArray;
                cachedComponentArgs = cachedArgs;
                z = outerDepth;
                outerDepth2 = i;
            }
            if (!setExported) {
                serviceInfo3 = s.info;
                if (s.intents.size() <= 0) {
                    outerDepth = false;
                }
                serviceInfo3.exported = outerDepth;
            }
            return s;
        }
        strArr[0] = "Heavy-weight applications can not have services in main process";
        return null;
    }

    private boolean isImplicitlyExposedIntent(IntentInfo intent) {
        return intent.hasCategory(Intent.CATEGORY_BROWSABLE) || intent.hasAction(Intent.ACTION_SEND) || intent.hasAction(Intent.ACTION_SENDTO) || intent.hasAction(Intent.ACTION_SEND_MULTIPLE);
    }

    private boolean parseAllMetaData(Resources res, XmlResourceParser parser, String tag, Component<?> outInfo, String[] outError) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                return true;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals("meta-data")) {
                        Bundle parseMetaData = parseMetaData(res, parser, outInfo.metaData, outError);
                        outInfo.metaData = parseMetaData;
                        if (parseMetaData == null) {
                            return false;
                        }
                    } else {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown element under ");
                        stringBuilder.append(tag);
                        stringBuilder.append(": ");
                        stringBuilder.append(parser.getName());
                        stringBuilder.append(" at ");
                        stringBuilder.append(this.mArchiveSourcePath);
                        stringBuilder.append(" ");
                        stringBuilder.append(parser.getPositionDescription());
                        Slog.w(str, stringBuilder.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
        return true;
    }

    private Bundle parseMetaData(Resources res, XmlResourceParser parser, Bundle data, String[] outError) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestMetaData);
        if (data == null) {
            data = new Bundle();
        }
        boolean z = false;
        String name = sa.getNonConfigurationString(0, 0);
        String str = null;
        if (name == null) {
            outError[0] = "<meta-data> requires an android:name attribute";
            sa.recycle();
            return null;
        }
        name = name.intern();
        TypedValue v = sa.peekValue(2);
        if (v == null || v.resourceId == 0) {
            v = sa.peekValue(1);
            if (v == null) {
                outError[0] = "<meta-data> requires an android:value or android:resource attribute";
                data = null;
            } else if (v.type == 3) {
                CharSequence cs = v.coerceToString();
                if (cs != null) {
                    str = cs.toString();
                }
                data.putString(name, str);
            } else if (v.type == 18) {
                if (v.data != 0) {
                    z = true;
                }
                data.putBoolean(name, z);
            } else if (v.type >= 16 && v.type <= 31) {
                data.putInt(name, v.data);
            } else if (v.type == 4) {
                data.putFloat(name, v.getFloat());
            } else {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("<meta-data> only supports string, integer, float, color, boolean, and resource reference types: ");
                stringBuilder.append(parser.getName());
                stringBuilder.append(" at ");
                stringBuilder.append(this.mArchiveSourcePath);
                stringBuilder.append(" ");
                stringBuilder.append(parser.getPositionDescription());
                Slog.w(str2, stringBuilder.toString());
            }
        } else {
            data.putInt(name, v.resourceId);
        }
        sa.recycle();
        XmlUtils.skipCurrentTag(parser);
        return data;
    }

    private static VerifierInfo parseVerifier(AttributeSet attrs) {
        String packageName = null;
        String encodedPublicKey = null;
        int attrCount = attrs.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            int attrResId = attrs.getAttributeNameResource(i);
            if (attrResId == 16842755) {
                packageName = attrs.getAttributeValue(i);
            } else if (attrResId == 16843686) {
                encodedPublicKey = attrs.getAttributeValue(i);
            }
        }
        if (packageName == null || packageName.length() == 0) {
            Slog.i(TAG, "verifier package name was null; skipping");
            return null;
        }
        PublicKey publicKey = parsePublicKey(encodedPublicKey);
        if (publicKey != null) {
            return new VerifierInfo(packageName, publicKey);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unable to parse verifier public key for ");
        stringBuilder.append(packageName);
        Slog.i(str, stringBuilder.toString());
        return null;
    }

    public static final PublicKey parsePublicKey(String encodedPublicKey) {
        if (encodedPublicKey == null) {
            Slog.w(TAG, "Could not parse null public key");
            return null;
        }
        try {
            EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decode(encodedPublicKey, null));
            try {
                return KeyFactory.getInstance("RSA").generatePublic(keySpec);
            } catch (NoSuchAlgorithmException e) {
                Slog.wtf(TAG, "Could not parse public key: RSA KeyFactory not included in build");
                try {
                    return KeyFactory.getInstance("EC").generatePublic(keySpec);
                } catch (NoSuchAlgorithmException e2) {
                    Slog.wtf(TAG, "Could not parse public key: EC KeyFactory not included in build");
                    try {
                        return KeyFactory.getInstance("DSA").generatePublic(keySpec);
                    } catch (NoSuchAlgorithmException e3) {
                        Slog.wtf(TAG, "Could not parse public key: DSA KeyFactory not included in build");
                        return null;
                    } catch (InvalidKeySpecException e4) {
                        return null;
                    }
                } catch (InvalidKeySpecException e5) {
                    return KeyFactory.getInstance("DSA").generatePublic(keySpec);
                }
            } catch (InvalidKeySpecException e6) {
                return KeyFactory.getInstance("EC").generatePublic(keySpec);
            }
        } catch (IllegalArgumentException e7) {
            Slog.w(TAG, "Could not parse verifier public key; invalid Base64");
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:37:0x00cc, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:46:0x00f5, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean parseIntent(Resources res, XmlResourceParser parser, boolean allowGlobs, boolean allowAutoVerify, IntentInfo outInfo, String[] outError) throws XmlPullParserException, IOException {
        int roundIconVal;
        Resources resources = res;
        XmlResourceParser xmlResourceParser = parser;
        IntentInfo intentInfo = outInfo;
        TypedArray sa = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestIntentFilter);
        int priority = sa.getInt(2, 0);
        intentInfo.setPriority(priority);
        intentInfo.setOrder(sa.getInt(3, 0));
        TypedValue v = sa.peekValue(0);
        if (v != null) {
            int i = v.resourceId;
            intentInfo.labelRes = i;
            if (i == 0) {
                intentInfo.nonLocalizedLabel = v.coerceToString();
            }
        }
        if (Resources.getSystem().getBoolean(R.bool.config_useRoundIcon)) {
            roundIconVal = sa.getResourceId(7, 0);
        } else {
            roundIconVal = 0;
        }
        int i2 = 1;
        if (roundIconVal != 0) {
            intentInfo.icon = roundIconVal;
        } else {
            intentInfo.icon = sa.getResourceId(1, 0);
        }
        intentInfo.logo = sa.getResourceId(4, 0);
        intentInfo.banner = sa.getResourceId(5, 0);
        if (allowAutoVerify) {
            intentInfo.setAutoVerify(sa.getBoolean(6, false));
        }
        sa.recycle();
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            TypedArray typedArray;
            int i3;
            int i4;
            if (next == i2) {
                typedArray = sa;
                i3 = priority;
                i4 = outerDepth;
                next = type;
                break;
            }
            next = type;
            if (next == 3 && parser.getDepth() <= outerDepth) {
                typedArray = sa;
                i3 = priority;
                i4 = outerDepth;
                break;
            }
            Object obj;
            if (next == 3) {
                typedArray = sa;
                i3 = priority;
                i4 = outerDepth;
                obj = null;
                priority = 3;
            } else if (next == 4) {
                typedArray = sa;
                i3 = priority;
                i4 = outerDepth;
                obj = null;
            } else {
                int str;
                int i5;
                String nodeName = parser.getName();
                if (nodeName.equals("action")) {
                    typedArray = sa;
                    sa = xmlResourceParser.getAttributeValue(ANDROID_RESOURCES, MidiDeviceInfo.PROPERTY_NAME);
                    if (sa == null || sa == "") {
                        outError[0] = "No value supplied for <android:name>";
                    } else {
                        XmlUtils.skipCurrentTag(parser);
                        intentInfo.addAction(sa);
                    }
                } else {
                    typedArray = sa;
                    String value;
                    if (nodeName.equals("category")) {
                        value = xmlResourceParser.getAttributeValue(ANDROID_RESOURCES, MidiDeviceInfo.PROPERTY_NAME);
                        if (value == null || value == "") {
                            outError[0] = "No value supplied for <android:name>";
                        } else {
                            XmlUtils.skipCurrentTag(parser);
                            intentInfo.addCategory(value);
                        }
                    } else {
                        i3 = priority;
                        if (nodeName.equals(ActivityManagerInternal.ASSIST_KEY_DATA)) {
                            TypedArray sa2 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestData);
                            String str2 = sa2.getNonConfigurationString(0, 0);
                            if (str2 != null) {
                                try {
                                    intentInfo.addDataType(str2);
                                    String str3 = str2;
                                    str = 0;
                                } catch (MalformedMimeTypeException e) {
                                    MalformedMimeTypeException malformedMimeTypeException = e;
                                    outError[0] = e.toString();
                                    sa2.recycle();
                                    return false;
                                }
                            }
                            str = 0;
                            String str4 = sa2.getNonConfigurationString(1, str);
                            if (str4 != null) {
                                intentInfo.addDataScheme(str4);
                            }
                            str4 = sa2.getNonConfigurationString(7, str);
                            if (str4 != null) {
                                intentInfo.addDataSchemeSpecificPart(str4, str);
                            }
                            value = sa2.getNonConfigurationString(8, str);
                            if (value != null) {
                                intentInfo.addDataSchemeSpecificPart(value, 1);
                            }
                            value = sa2.getNonConfigurationString(9, str);
                            if (value == null) {
                                priority = 2;
                            } else if (allowGlobs) {
                                priority = 2;
                                intentInfo.addDataSchemeSpecificPart(value, 2);
                            } else {
                                outError[str] = "sspPattern not allowed here; ssp must be literal";
                                return str;
                            }
                            value = sa2.getNonConfigurationString(priority, str);
                            i4 = outerDepth;
                            outerDepth = sa2.getNonConfigurationString(3, str);
                            if (value != null) {
                                intentInfo.addDataAuthority(value, outerDepth);
                            }
                            value = sa2.getNonConfigurationString(4, str);
                            if (value != null) {
                                intentInfo.addDataPath(value, str);
                            }
                            value = sa2.getNonConfigurationString(5, str);
                            if (value != null) {
                                intentInfo.addDataPath(value, 1);
                            }
                            value = sa2.getNonConfigurationString(6, str);
                            if (value != null) {
                                if (allowGlobs) {
                                    intentInfo.addDataPath(value, 2);
                                } else {
                                    outError[str] = "pathPattern not allowed here; path must be literal";
                                    return str;
                                }
                            }
                            value = sa2.getNonConfigurationString(10, str);
                            if (value != null) {
                                if (allowGlobs) {
                                    intentInfo.addDataPath(value, 3);
                                } else {
                                    outError[str] = "pathAdvancedPattern not allowed here; path must be literal";
                                    return str;
                                }
                            }
                            sa2.recycle();
                            XmlUtils.skipCurrentTag(parser);
                            sa = sa2;
                            i5 = str;
                            priority = i3;
                            outerDepth = i4;
                            resources = res;
                            i2 = 1;
                        } else {
                            str = 0;
                            i4 = outerDepth;
                            if (nodeName.equals("state")) {
                                parseIntentFilterState(xmlResourceParser, ANDROID_RESOURCES, intentInfo);
                                XmlUtils.skipCurrentTag(parser);
                            } else {
                                value = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown element under <intent-filter>: ");
                                stringBuilder.append(parser.getName());
                                stringBuilder.append(" at ");
                                stringBuilder.append(this.mArchiveSourcePath);
                                stringBuilder.append(" ");
                                stringBuilder.append(parser.getPositionDescription());
                                Slog.w(value, stringBuilder.toString());
                                XmlUtils.skipCurrentTag(parser);
                            }
                            sa = typedArray;
                            i5 = str;
                            priority = i3;
                            outerDepth = i4;
                            resources = res;
                            i2 = 1;
                        }
                    }
                }
                i3 = priority;
                i4 = outerDepth;
                str = 0;
                sa = typedArray;
                i5 = str;
                priority = i3;
                outerDepth = i4;
                resources = res;
                i2 = 1;
            }
            Object obj2 = obj;
            sa = typedArray;
            priority = i3;
            outerDepth = i4;
            resources = res;
            i2 = 1;
        }
        intentInfo.hasDefault = intentInfo.hasCategory(Intent.CATEGORY_DEFAULT);
        return true;
    }

    /* JADX WARNING: Missing block: B:44:0x005b, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean copyNeeded(int flags, Package p, PackageUserState state, Bundle metaData, int userId) {
        if (userId != 0) {
            return true;
        }
        if (state.enabled != 0) {
            if (p.applicationInfo.enabled != (state.enabled == 1)) {
                return true;
            }
        }
        if (state.suspended != ((p.applicationInfo.flags & 1073741824) != 0) || !state.installed || state.hidden || state.stopped || state.instantApp != p.applicationInfo.isInstantApp()) {
            return true;
        }
        if ((flags & 128) != 0 && (metaData != null || p.mAppMetaData != null)) {
            return true;
        }
        if (((flags & 1024) == 0 || p.usesLibraryFiles == null) && p.staticSharedLibName == null) {
            return false;
        }
        return true;
    }

    public static ApplicationInfo generateApplicationInfo(Package p, int flags, PackageUserState state) {
        return generateApplicationInfo(p, flags, state, UserHandle.getCallingUserId());
    }

    private static void updateApplicationInfo(ApplicationInfo ai, int flags, PackageUserState state) {
        if (!sCompatibilityModeEnabled) {
            ai.disableCompatibilityMode();
        }
        if (state.installed) {
            ai.flags |= 8388608;
        } else {
            ai.flags &= -8388609;
        }
        if (state.suspended) {
            ai.flags |= 1073741824;
        } else {
            ai.flags &= -1073741825;
        }
        if (state.instantApp) {
            ai.privateFlags |= 128;
        } else {
            ai.privateFlags &= -129;
        }
        if (state.virtualPreload) {
            ai.privateFlags |= 65536;
        } else {
            ai.privateFlags &= -65537;
        }
        boolean z = true;
        if (state.hidden) {
            ai.privateFlags |= 1;
        } else {
            ai.privateFlags &= -2;
        }
        if (state.enabled == 1) {
            ai.enabled = true;
        } else if (state.enabled == 4) {
            if ((32768 & flags) == 0) {
                z = false;
            }
            ai.enabled = z;
        } else if (state.enabled == 2 || state.enabled == 3) {
            ai.enabled = false;
        }
        ai.enabledSetting = state.enabled;
        if (ai.category == -1) {
            ai.category = state.categoryHint;
        }
        if (ai.category == -1) {
            ai.category = FallbackCategoryProvider.getFallbackCategory(ai.packageName);
        }
        ai.seInfoUser = SELinuxUtil.assignSeinfoUser(state);
        ai.resourceDirs = state.overlayPaths;
    }

    public static ApplicationInfo generateApplicationInfo(Package p, int flags, PackageUserState state, int userId) {
        if (p == null || !checkUseInstalledOrHidden(flags, state, p.applicationInfo) || !p.isMatch(flags)) {
            return null;
        }
        if (copyNeeded(flags, p, state, null, userId) || ((32768 & flags) != 0 && state.enabled == 4)) {
            ApplicationInfo ai = new ApplicationInfo(p.applicationInfo);
            ai.initForUser(userId);
            if ((flags & 128) != 0) {
                ai.metaData = p.mAppMetaData;
            }
            if ((flags & 1024) != 0) {
                ai.sharedLibraryFiles = p.usesLibraryFiles;
            }
            if (state.stopped) {
                ai.flags |= 2097152;
            } else {
                ai.flags &= -2097153;
            }
            updateApplicationInfo(ai, flags, state);
            return ai;
        }
        updateApplicationInfo(p.applicationInfo, flags, state);
        return p.applicationInfo;
    }

    public static ApplicationInfo generateApplicationInfo(ApplicationInfo ai, int flags, PackageUserState state, int userId) {
        if (ai == null || !checkUseInstalledOrHidden(flags, state, ai)) {
            return null;
        }
        ai = new ApplicationInfo(ai);
        ai.initForUser(userId);
        if (state.stopped) {
            ai.flags |= 2097152;
        } else {
            ai.flags &= -2097153;
        }
        updateApplicationInfo(ai, flags, state);
        return ai;
    }

    public static final PermissionInfo generatePermissionInfo(Permission p, int flags) {
        if (p == null) {
            return null;
        }
        if ((flags & 128) == 0) {
            return p.info;
        }
        PermissionInfo pi = new PermissionInfo(p.info);
        pi.metaData = p.metaData;
        return pi;
    }

    public static final PermissionGroupInfo generatePermissionGroupInfo(PermissionGroup pg, int flags) {
        if (pg == null) {
            return null;
        }
        if ((flags & 128) == 0) {
            return pg.info;
        }
        PermissionGroupInfo pgi = new PermissionGroupInfo(pg.info);
        pgi.metaData = pg.metaData;
        return pgi;
    }

    public static final ActivityInfo generateActivityInfo(Activity a, int flags, PackageUserState state, int userId) {
        if (a == null || !checkUseInstalledOrHidden(flags, state, a.owner.applicationInfo)) {
            return null;
        }
        if (copyNeeded(flags, a.owner, state, a.metaData, userId)) {
            ActivityInfo ai = new ActivityInfo(a.info);
            ai.metaData = a.metaData;
            ai.applicationInfo = generateApplicationInfo(a.owner, flags, state, userId);
            return ai;
        }
        updateApplicationInfo(a.info.applicationInfo, flags, state);
        return a.info;
    }

    public static final ActivityInfo generateActivityInfo(ActivityInfo ai, int flags, PackageUserState state, int userId) {
        if (ai == null || !checkUseInstalledOrHidden(flags, state, ai.applicationInfo)) {
            return null;
        }
        ai = new ActivityInfo(ai);
        ai.applicationInfo = generateApplicationInfo(ai.applicationInfo, flags, state, userId);
        return ai;
    }

    public static final ServiceInfo generateServiceInfo(Service s, int flags, PackageUserState state, int userId) {
        if (s == null || !checkUseInstalledOrHidden(flags, state, s.owner.applicationInfo)) {
            return null;
        }
        if (copyNeeded(flags, s.owner, state, s.metaData, userId)) {
            ServiceInfo si = new ServiceInfo(s.info);
            si.metaData = s.metaData;
            si.applicationInfo = generateApplicationInfo(s.owner, flags, state, userId);
            return si;
        }
        updateApplicationInfo(s.info.applicationInfo, flags, state);
        return s.info;
    }

    public static final ProviderInfo generateProviderInfo(Provider p, int flags, PackageUserState state, int userId) {
        if (p == null || !checkUseInstalledOrHidden(flags, state, p.owner.applicationInfo)) {
            return null;
        }
        if (copyNeeded(flags, p.owner, state, p.metaData, userId) || ((flags & 2048) == 0 && p.info.uriPermissionPatterns != null)) {
            ProviderInfo pi = new ProviderInfo(p.info);
            pi.metaData = p.metaData;
            if ((flags & 2048) == 0) {
                pi.uriPermissionPatterns = null;
            }
            pi.applicationInfo = generateApplicationInfo(p.owner, flags, state, userId);
            return pi;
        }
        updateApplicationInfo(p.info.applicationInfo, flags, state);
        return p.info;
    }

    public static final InstrumentationInfo generateInstrumentationInfo(Instrumentation i, int flags) {
        if (i == null) {
            return null;
        }
        if ((flags & 128) == 0) {
            return i.info;
        }
        InstrumentationInfo ii = new InstrumentationInfo(i.info);
        ii.metaData = i.metaData;
        return ii;
    }

    public static void setCompatibilityModeEnabled(boolean compatibilityModeEnabled) {
        sCompatibilityModeEnabled = compatibilityModeEnabled;
    }

    private void parseIntentFilterState(XmlResourceParser parser, String android_resources, IntentInfo outInfo) {
        XmlResourceParser xmlResourceParser = parser;
        String str = android_resources;
        String name = xmlResourceParser.getAttributeValue(str, MidiDeviceInfo.PROPERTY_NAME);
        if (name == null) {
            Log.w(TAG, "No value supplied for <android:name>");
            return;
        }
        String value = xmlResourceParser.getAttributeValue(str, Slice.SUBTYPE_VALUE);
        if (value == null) {
            Log.w(TAG, "No value supplied for <android:value>");
            return;
        }
        String[] items = name.split("@");
        if (items.length != 2) {
            Log.w(TAG, "state name error");
            return;
        }
        int i = 0;
        String action = items[0];
        if (items[1].equals("ImplicitBroadcastExpand")) {
            String[] filters = value.split("\\|");
            int i2 = 0;
            while (i2 < filters.length) {
                String[] state = filters[i2].split("=");
                if (state.length != 2) {
                    Log.w(TAG, "value format error");
                    return;
                }
                outInfo.addActionFilter(action, state[i], state[1]);
                i2++;
                i = 0;
            }
            IntentInfo intentInfo = outInfo;
            return;
        }
        Log.w(TAG, "state flag error");
    }
}
