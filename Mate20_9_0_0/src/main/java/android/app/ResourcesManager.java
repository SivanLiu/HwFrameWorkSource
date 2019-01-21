package android.app;

import android.content.pm.PackageParser;
import android.content.res.ApkAssets;
import android.content.res.AssetManager;
import android.content.res.AssetManager.Builder;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.ResourcesImpl;
import android.content.res.ResourcesKey;
import android.graphics.Rect;
import android.hardware.display.DisplayManagerGlobal;
import android.hwtheme.HwThemeManager;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayAdjustments;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Predicate;

public class ResourcesManager {
    private static final boolean DEBUG = false;
    private static final boolean ENABLE_LRU_CACHE = SystemProperties.getBoolean("persist.sys.enable_apk_assets_lru_cache", true);
    private static final String FWK_ANDROID_TAG = "frameworkoverlaydark";
    private static final String FWK_EXT_TAG = "frameworkhwext";
    static final String TAG = "ResourcesManager";
    private static final Predicate<WeakReference<Resources>> sEmptyReferencePredicate = -$$Lambda$ResourcesManager$QJ7UiVk_XS90KuXAsIjIEym1DnM.INSTANCE;
    private static ArrayMap<String, Integer> sHwThemeType = new ArrayMap();
    private static ResourcesManager sResourcesManager;
    private static final Object sThemeTypeLock = new Object();
    private final WeakHashMap<IBinder, ActivityResources> mActivityResourceReferences = new WeakHashMap();
    private final ArrayMap<Pair<Integer, DisplayAdjustments>, WeakReference<Display>> mAdjustedDisplays = new ArrayMap();
    private final ArrayMap<ApkKey, WeakReference<ApkAssets>> mCachedApkAssets = new ArrayMap();
    private final LruCache<ApkKey, ApkAssets> mLoadedApkAssets = new LruCache(3);
    private CompatibilityInfo mResCompatibilityInfo;
    private final Configuration mResConfiguration = new Configuration();
    private final ArrayMap<ResourcesKey, WeakReference<ResourcesImpl>> mResourceImpls = new ArrayMap();
    private final ArrayList<WeakReference<Resources>> mResourceReferences = new ArrayList();

    private static class ActivityResources {
        public final ArrayList<WeakReference<Resources>> activityResources;
        public final Configuration overrideConfig;

        private ActivityResources() {
            this.overrideConfig = new Configuration();
            this.activityResources = new ArrayList();
        }
    }

    private static class ApkKey {
        public final boolean overlay;
        public final String path;
        public final boolean sharedLib;

        ApkKey(String path, boolean sharedLib, boolean overlay) {
            this.path = path;
            this.sharedLib = sharedLib;
            this.overlay = overlay;
        }

        public int hashCode() {
            return (31 * ((31 * ((31 * 1) + this.path.hashCode())) + Boolean.hashCode(this.sharedLib))) + Boolean.hashCode(this.overlay);
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof ApkKey)) {
                return false;
            }
            ApkKey other = (ApkKey) obj;
            if (this.path.equals(other.path) && this.sharedLib == other.sharedLib && this.overlay == other.overlay) {
                z = true;
            }
            return z;
        }
    }

    public void setHwThemeType(String resDir, int type) {
        if (resDir != null) {
            synchronized (sThemeTypeLock) {
                sHwThemeType.put(resDir, Integer.valueOf(type));
            }
        }
    }

    static /* synthetic */ boolean lambda$static$0(WeakReference weakRef) {
        return weakRef == null || weakRef.get() == null;
    }

    public static ResourcesManager getInstance() {
        ResourcesManager resourcesManager;
        synchronized (ResourcesManager.class) {
            if (sResourcesManager == null) {
                sResourcesManager = new ResourcesManager();
            }
            resourcesManager = sResourcesManager;
        }
        return resourcesManager;
    }

    public void invalidatePath(String path) {
        synchronized (this) {
            int count = 0;
            int i = 0;
            while (i < this.mResourceImpls.size()) {
                ResourcesKey key = (ResourcesKey) this.mResourceImpls.keyAt(i);
                if (key.isPathReferenced(path)) {
                    cleanupResourceImpl(key);
                    count++;
                } else {
                    i++;
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalidated ");
            stringBuilder.append(count);
            stringBuilder.append(" asset managers that referenced ");
            stringBuilder.append(path);
            Log.i(str, stringBuilder.toString());
        }
    }

    public Configuration getConfiguration() {
        Configuration configuration;
        synchronized (this) {
            configuration = this.mResConfiguration;
        }
        return configuration;
    }

    DisplayMetrics getDisplayMetrics() {
        return getDisplayMetrics(0, DisplayAdjustments.DEFAULT_DISPLAY_ADJUSTMENTS);
    }

    @VisibleForTesting
    protected DisplayMetrics getDisplayMetrics(int displayId, DisplayAdjustments da) {
        DisplayMetrics dm = new DisplayMetrics();
        Display display = getAdjustedDisplay(displayId, da);
        if (display != null) {
            display.getMetrics(dm);
        } else {
            dm.setToDefaults();
        }
        return dm;
    }

    private static void applyNonDefaultDisplayMetricsToConfiguration(DisplayMetrics dm, Configuration config) {
        config.touchscreen = 1;
        config.densityDpi = dm.densityDpi;
        config.screenWidthDp = (int) (((float) dm.widthPixels) / dm.density);
        config.screenHeightDp = (int) (((float) dm.heightPixels) / dm.density);
        int sl = Configuration.resetScreenLayout(config.screenLayout);
        if (dm.widthPixels > dm.heightPixels) {
            config.orientation = 2;
            config.screenLayout = Configuration.reduceScreenLayout(sl, config.screenWidthDp, config.screenHeightDp);
        } else {
            config.orientation = 1;
            config.screenLayout = Configuration.reduceScreenLayout(sl, config.screenHeightDp, config.screenWidthDp);
        }
        config.smallestScreenWidthDp = config.screenWidthDp;
        config.compatScreenWidthDp = config.screenWidthDp;
        config.compatScreenHeightDp = config.screenHeightDp;
        config.compatSmallestScreenWidthDp = config.smallestScreenWidthDp;
    }

    public boolean applyCompatConfigurationLocked(int displayDensity, Configuration compatConfiguration) {
        if (this.mResCompatibilityInfo == null || this.mResCompatibilityInfo.supportsScreen()) {
            return false;
        }
        this.mResCompatibilityInfo.applyToConfiguration(displayDensity, compatConfiguration);
        return true;
    }

    /* JADX WARNING: Missing block: B:21:0x0049, code skipped:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Display getAdjustedDisplay(int displayId, DisplayAdjustments displayAdjustments) {
        Pair<Integer, DisplayAdjustments> key = Pair.create(Integer.valueOf(displayId), displayAdjustments != null ? new DisplayAdjustments(displayAdjustments) : new DisplayAdjustments());
        synchronized (this) {
            WeakReference<Display> wd = (WeakReference) this.mAdjustedDisplays.get(key);
            if (wd != null) {
                Display display = (Display) wd.get();
                if (display != null) {
                    return display;
                }
            }
            DisplayManagerGlobal dm = DisplayManagerGlobal.getInstance();
            if (dm == null) {
                return null;
            }
            Display display2 = dm.getCompatibleDisplay(displayId, (DisplayAdjustments) key.second);
            if (display2 != null) {
                this.mAdjustedDisplays.put(key, new WeakReference(display2));
            }
        }
    }

    public Display getAdjustedDisplay(int displayId, Resources resources) {
        synchronized (this) {
            DisplayManagerGlobal dm = DisplayManagerGlobal.getInstance();
            if (dm == null) {
                return null;
            }
            Display compatibleDisplay = dm.getCompatibleDisplay(displayId, resources);
            return compatibleDisplay;
        }
    }

    private void cleanupResourceImpl(ResourcesKey removedKey) {
        ResourcesImpl res = (ResourcesImpl) ((WeakReference) this.mResourceImpls.remove(removedKey)).get();
        if (res != null) {
            res.flushLayoutCache();
        }
    }

    private static String overlayPathToIdmapPath(String path) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("/data/resource-cache/");
        stringBuilder.append(path.substring(1).replace('/', '@'));
        stringBuilder.append("@idmap");
        return stringBuilder.toString();
    }

    private ApkAssets loadApkAssets(String path, boolean sharedLib, boolean overlay) throws IOException {
        ApkAssets apkAssets;
        ApkKey newKey = new ApkKey(path, sharedLib, overlay);
        boolean isSystemServer = false;
        if (ENABLE_LRU_CACHE) {
            isSystemServer = ActivityThread.isSystem();
        }
        if (isSystemServer) {
            apkAssets = (ApkAssets) this.mLoadedApkAssets.get(newKey);
            if (apkAssets != null) {
                return apkAssets;
            }
        }
        WeakReference<ApkAssets> apkAssetsRef = (WeakReference) this.mCachedApkAssets.get(newKey);
        if (apkAssetsRef != null) {
            apkAssets = (ApkAssets) apkAssetsRef.get();
            if (apkAssets != null) {
                if (isSystemServer) {
                    this.mLoadedApkAssets.put(newKey, apkAssets);
                }
                return apkAssets;
            }
            this.mCachedApkAssets.remove(newKey);
        }
        if (overlay) {
            apkAssets = ApkAssets.loadOverlayFromPath(overlayPathToIdmapPath(path), false);
        } else {
            apkAssets = ApkAssets.loadFromPath(path, false, sharedLib);
        }
        if (isSystemServer) {
            this.mLoadedApkAssets.put(newKey, apkAssets);
        }
        this.mCachedApkAssets.put(newKey, new WeakReference(apkAssets));
        return apkAssets;
    }

    @VisibleForTesting
    protected AssetManager createAssetManager(ResourcesKey key) {
        int i;
        String str;
        StringBuilder stringBuilder;
        Builder builder = new Builder();
        if (key.mResDir != null) {
            try {
                builder.addApkAssets(loadApkAssets(key.mResDir, false, false));
            } catch (IOException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("failed to add asset path ");
                stringBuilder2.append(key.mResDir);
                Log.e(str2, stringBuilder2.toString());
                return null;
            }
        }
        Integer type = null;
        if (key.mResDir != null) {
            synchronized (sThemeTypeLock) {
                type = (Integer) sHwThemeType.get(key.mResDir);
            }
        }
        if (key.mSplitResDirs != null) {
            String[] strArr = key.mSplitResDirs;
            int length = strArr.length;
            i = 0;
            while (i < length) {
                String splitResDir = strArr[i];
                try {
                    builder.addApkAssets(loadApkAssets(splitResDir, false, false));
                    i++;
                } catch (IOException e2) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("failed to add split asset path ");
                    stringBuilder3.append(splitResDir);
                    Log.e(str3, stringBuilder3.toString());
                    return null;
                }
            }
        }
        boolean isNeedSuppprotDeepType = (key.mResDir == null || (HwPCUtils.enabled() && ActivityThread.currentActivityThread() != null && HwPCUtils.isValidExtDisplayId(ActivityThread.currentActivityThread().getDisplayId()))) ? false : true;
        if (isNeedSuppprotDeepType) {
            builder.getAssets().setDeepType(type.intValue());
        }
        if (key.mOverlayDirs != null) {
            for (String idmapPath : key.mOverlayDirs) {
                if (idmapPath != null) {
                    try {
                        if (idmapPath.contains("frameworkhwext") || idmapPath.contains(FWK_ANDROID_TAG)) {
                            if (isNeedSuppprotDeepType && type != null) {
                                if (type.intValue() != 0) {
                                    if (idmapPath.contains(HwThemeManager.DARK_TAG) && (type.intValue() & 1) == 1) {
                                        builder.addApkAssets(loadApkAssets(idmapPath, false, true));
                                    }
                                    if (idmapPath.contains(HwThemeManager.HONOR_TAG) && HwThemeManager.isHonorProduct() && (type.intValue() & 16) == 16) {
                                        builder.addApkAssets(loadApkAssets(idmapPath, false, true));
                                    }
                                    if (idmapPath.contains(HwThemeManager.NOVA_TAG) && HwThemeManager.isNovaProduct() && (type.intValue() & 256) == 256) {
                                        builder.addApkAssets(loadApkAssets(idmapPath, false, true));
                                    }
                                }
                            }
                        }
                    } catch (IOException e3) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("failed to add overlay path ");
                        stringBuilder.append(idmapPath);
                        Log.w(str, stringBuilder.toString());
                    }
                }
                builder.addApkAssets(loadApkAssets(idmapPath, false, true));
            }
        }
        if (key.mLibDirs != null) {
            for (String idmapPath2 : key.mLibDirs) {
                if (idmapPath2.endsWith(PackageParser.APK_FILE_EXTENSION)) {
                    try {
                        builder.addApkAssets(loadApkAssets(idmapPath2, true, false));
                    } catch (IOException e4) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Asset path '");
                        stringBuilder.append(idmapPath2);
                        stringBuilder.append("' does not exist or contains no resources.");
                        Log.w(str, stringBuilder.toString());
                    }
                }
            }
        }
        return builder.build();
    }

    private static <T> int countLiveReferences(Collection<WeakReference<T>> collection) {
        int count = 0;
        for (WeakReference<T> ref : collection) {
            if ((ref != null ? ref.get() : null) != null) {
                count++;
            }
        }
        return count;
    }

    public void dump(String prefix, PrintWriter printWriter) {
        synchronized (this) {
            int i;
            IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
            for (i = 0; i < prefix.length() / 2; i++) {
                pw.increaseIndent();
            }
            pw.println("ResourcesManager:");
            pw.increaseIndent();
            if (ENABLE_LRU_CACHE && ActivityThread.isSystem()) {
                pw.print("cached apks: total=");
                pw.print(this.mLoadedApkAssets.size());
                pw.print(" created=");
                pw.print(this.mLoadedApkAssets.createCount());
                pw.print(" evicted=");
                pw.print(this.mLoadedApkAssets.evictionCount());
                pw.print(" hit=");
                pw.print(this.mLoadedApkAssets.hitCount());
                pw.print(" miss=");
                pw.print(this.mLoadedApkAssets.missCount());
                pw.print(" max=");
                pw.print(this.mLoadedApkAssets.maxSize());
                pw.println();
            }
            pw.print("total apks: ");
            pw.println(countLiveReferences(this.mCachedApkAssets.values()));
            pw.print("resources: ");
            i = countLiveReferences(this.mResourceReferences);
            for (ActivityResources activityResources : this.mActivityResourceReferences.values()) {
                i += countLiveReferences(activityResources.activityResources);
            }
            pw.println(i);
            pw.print("resource impls: ");
            pw.println(countLiveReferences(this.mResourceImpls.values()));
            if (ENABLE_LRU_CACHE) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("callerPid=");
                stringBuilder.append(Process.myPid());
                stringBuilder.append(", isSystemServer=");
                stringBuilder.append(ActivityThread.isSystem());
                pw.print(stringBuilder.toString());
            }
        }
    }

    private Configuration generateConfig(ResourcesKey key, DisplayMetrics dm) {
        boolean isDefaultDisplay = key.mDisplayId == 0;
        boolean hasOverrideConfig = key.hasOverrideConfiguration();
        if (isDefaultDisplay && !hasOverrideConfig) {
            return getConfiguration();
        }
        Configuration config = new Configuration(getConfiguration());
        if (!isDefaultDisplay) {
            applyNonDefaultDisplayMetricsToConfiguration(dm, config);
        }
        if (!hasOverrideConfig) {
            return config;
        }
        config.updateFrom(key.mOverrideConfiguration);
        return config;
    }

    private ResourcesImpl createResourcesImpl(ResourcesKey key) {
        DisplayAdjustments daj = new DisplayAdjustments(key.mOverrideConfiguration);
        daj.setCompatibilityInfo(key.mCompatInfo);
        AssetManager assets = createAssetManager(key);
        if (assets == null) {
            return null;
        }
        DisplayMetrics dm = getDisplayMetrics(key.mDisplayId, daj);
        return new ResourcesImpl(assets, dm, generateConfig(key, dm), daj);
    }

    private ResourcesImpl findResourcesImplForKeyLocked(ResourcesKey key) {
        WeakReference<ResourcesImpl> weakImplRef = (WeakReference) this.mResourceImpls.get(key);
        ResourcesImpl impl = weakImplRef != null ? (ResourcesImpl) weakImplRef.get() : null;
        if (impl == null || !impl.getAssets().isUpToDate()) {
            return null;
        }
        return impl;
    }

    private ResourcesImpl findOrCreateResourcesImplForKeyLocked(ResourcesKey key) {
        ResourcesImpl impl = findResourcesImplForKeyLocked(key);
        if (impl == null) {
            impl = createResourcesImpl(key);
            if (impl != null) {
                this.mResourceImpls.put(key, new WeakReference(impl));
            }
        }
        return impl;
    }

    private ResourcesKey findKeyForResourceImplLocked(ResourcesImpl resourceImpl) {
        int refCount = this.mResourceImpls.size();
        int i = 0;
        while (true) {
            ResourcesImpl impl = null;
            if (i >= refCount) {
                return null;
            }
            WeakReference<ResourcesImpl> weakImplRef = (WeakReference) this.mResourceImpls.valueAt(i);
            if (weakImplRef != null) {
                impl = (ResourcesImpl) weakImplRef.get();
            }
            if (impl != null && resourceImpl == impl) {
                return (ResourcesKey) this.mResourceImpls.keyAt(i);
            }
            i++;
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0018, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:24:0x0033, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean isSameResourcesOverrideConfig(IBinder activityToken, Configuration overrideConfig) {
        synchronized (this) {
            ActivityResources activityResources;
            if (activityToken != null) {
                try {
                    activityResources = (ActivityResources) this.mActivityResourceReferences.get(activityToken);
                } catch (Throwable th) {
                }
            } else {
                activityResources = null;
            }
            boolean z = false;
            if (activityResources != null) {
                if (!Objects.equals(activityResources.overrideConfig, overrideConfig)) {
                    if (overrideConfig == null || activityResources.overrideConfig == null || overrideConfig.diffPublicOnly(activityResources.overrideConfig) != 0) {
                    }
                }
                z = true;
            } else if (overrideConfig == null) {
                z = true;
            }
        }
    }

    private ActivityResources getOrCreateActivityResourcesStructLocked(IBinder activityToken) {
        ActivityResources activityResources = (ActivityResources) this.mActivityResourceReferences.get(activityToken);
        if (activityResources != null) {
            return activityResources;
        }
        activityResources = new ActivityResources();
        this.mActivityResourceReferences.put(activityToken, activityResources);
        return activityResources;
    }

    private Resources getOrCreateResourcesForActivityLocked(IBinder activityToken, ClassLoader classLoader, ResourcesImpl impl, CompatibilityInfo compatInfo) {
        ActivityResources activityResources = getOrCreateActivityResourcesStructLocked(activityToken);
        int refCount = activityResources.activityResources.size();
        for (int i = 0; i < refCount; i++) {
            Resources resources = (Resources) ((WeakReference) activityResources.activityResources.get(i)).get();
            if (resources != null && Objects.equals(resources.getClassLoader(), classLoader) && resources.getImpl() == impl) {
                return resources;
            }
        }
        Resources resources2 = HwThemeManager.getResources(classLoader);
        if (resources2 != null) {
            resources2.setImpl(impl);
        }
        activityResources.activityResources.add(new WeakReference(resources2));
        return resources2;
    }

    private Resources getOrCreateResourcesLocked(ClassLoader classLoader, ResourcesImpl impl, CompatibilityInfo compatInfo) {
        int refCount = this.mResourceReferences.size();
        for (int i = 0; i < refCount; i++) {
            Resources resources = (Resources) ((WeakReference) this.mResourceReferences.get(i)).get();
            if (resources != null && Objects.equals(resources.getClassLoader(), classLoader) && resources.getImpl() == impl) {
                return resources;
            }
        }
        Resources resources2 = HwThemeManager.getResources(classLoader);
        if (resources2 != null) {
            resources2.setImpl(impl);
        }
        this.mResourceReferences.add(new WeakReference(resources2));
        return resources2;
    }

    public Resources createBaseActivityResources(IBinder activityToken, String resDir, String[] splitResDirs, String[] overlayDirs, String[] libDirs, int displayId, Configuration overrideConfig, CompatibilityInfo compatInfo, ClassLoader classLoader) {
        int i;
        Throwable th;
        IBinder iBinder = activityToken;
        Configuration configuration = overrideConfig;
        ClassLoader classLoader2;
        try {
            Trace.traceBegin(8192, "ResourcesManager#createBaseActivityResources");
            ResourcesKey key = new ResourcesKey(resDir, splitResDirs, overlayDirs, libDirs, displayId, configuration != null ? new Configuration(configuration) : null, compatInfo);
            classLoader2 = classLoader != null ? classLoader : ClassLoader.getSystemClassLoader();
            try {
                synchronized (this) {
                    try {
                        getOrCreateActivityResourcesStructLocked(iBinder);
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                updateResourcesForActivity(iBinder, configuration, displayId, false);
                Resources orCreateResources = getOrCreateResources(iBinder, key, classLoader2);
                Trace.traceEnd(8192);
                return orCreateResources;
            } catch (Throwable th3) {
                th = th3;
                i = displayId;
                Trace.traceEnd(8192);
                throw th;
            }
        } catch (Throwable th4) {
            th = th4;
            i = displayId;
            classLoader2 = classLoader;
            Trace.traceEnd(8192);
            throw th;
        }
    }

    /* JADX WARNING: Missing block: B:30:0x0079, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Resources getOrCreateResources(IBinder activityToken, ResourcesKey key, ClassLoader classLoader) {
        synchronized (this) {
            ResourcesImpl resourcesImpl;
            Resources orCreateResourcesLocked;
            if (activityToken != null) {
                try {
                    ActivityResources activityResources = getOrCreateActivityResourcesStructLocked(activityToken);
                    ArrayUtils.unstableRemoveIf(activityResources.activityResources, sEmptyReferencePredicate);
                    if (key.hasOverrideConfiguration() && !activityResources.overrideConfig.equals(Configuration.EMPTY)) {
                        Configuration temp = new Configuration(activityResources.overrideConfig);
                        temp.updateFrom(key.mOverrideConfiguration);
                        key.mOverrideConfiguration.setTo(temp);
                    }
                    ResourcesImpl resourcesImpl2 = findResourcesImplForKeyLocked(key);
                    if (resourcesImpl2 != null) {
                        Resources orCreateResourcesForActivityLocked = getOrCreateResourcesForActivityLocked(activityToken, classLoader, resourcesImpl2, key.mCompatInfo);
                        return orCreateResourcesForActivityLocked;
                    }
                } finally {
                }
            } else {
                ArrayUtils.unstableRemoveIf(this.mResourceReferences, sEmptyReferencePredicate);
                resourcesImpl = findResourcesImplForKeyLocked(key);
                if (resourcesImpl != null) {
                    orCreateResourcesLocked = getOrCreateResourcesLocked(classLoader, resourcesImpl, key.mCompatInfo);
                    return orCreateResourcesLocked;
                }
            }
            resourcesImpl = createResourcesImpl(key);
            if (resourcesImpl == null) {
                return null;
            }
            this.mResourceImpls.put(key, new WeakReference(resourcesImpl));
            if (activityToken != null) {
                orCreateResourcesLocked = getOrCreateResourcesForActivityLocked(activityToken, classLoader, resourcesImpl, key.mCompatInfo);
            } else {
                orCreateResourcesLocked = getOrCreateResourcesLocked(classLoader, resourcesImpl, key.mCompatInfo);
            }
        }
    }

    public Resources getResources(IBinder activityToken, String resDir, String[] splitResDirs, String[] overlayDirs, String[] libDirs, int displayId, Configuration overrideConfig, CompatibilityInfo compatInfo, ClassLoader classLoader) {
        Throwable th;
        Configuration configuration = overrideConfig;
        try {
            Trace.traceBegin(8192, "ResourcesManager#getResources");
            try {
                Resources orCreateResources = getOrCreateResources(activityToken, new ResourcesKey(resDir, splitResDirs, overlayDirs, libDirs, displayId, configuration != null ? new Configuration(configuration) : null, compatInfo), classLoader != null ? classLoader : ClassLoader.getSystemClassLoader());
                Trace.traceEnd(8192);
                return orCreateResources;
            } catch (Throwable th2) {
                th = th2;
                Trace.traceEnd(8192);
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            IBinder iBinder = activityToken;
            ClassLoader classLoader2 = classLoader;
            Trace.traceEnd(8192);
            throw th;
        }
    }

    /* JADX WARNING: Missing block: B:51:0x010e, code skipped:
            android.os.Trace.traceEnd(8192);
     */
    /* JADX WARNING: Missing block: B:52:0x0114, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateResourcesForActivity(IBinder activityToken, Configuration overrideConfig, int displayId, boolean movedToDifferentDisplay) {
        Configuration configuration = overrideConfig;
        try {
            Trace.traceBegin(8192, "ResourcesManager#updateResourcesForActivity");
            synchronized (this) {
                ActivityResources activityResources = getOrCreateActivityResourcesStructLocked(activityToken);
                if (!Objects.equals(activityResources.overrideConfig, configuration) || movedToDifferentDisplay) {
                    Configuration oldConfig = new Configuration(activityResources.overrideConfig);
                    if (configuration != null) {
                        activityResources.overrideConfig.setTo(configuration);
                    } else {
                        activityResources.overrideConfig.unset();
                    }
                    boolean activityHasOverrideConfig = activityResources.overrideConfig.equals(Configuration.EMPTY) ^ 1;
                    int refCount = activityResources.activityResources.size();
                    int i = 0;
                    while (i < refCount) {
                        ActivityResources activityResources2;
                        Resources resources = (Resources) ((WeakReference) activityResources.activityResources.get(i)).get();
                        if (resources != null) {
                            ResourcesKey oldKey = findKeyForResourceImplLocked(resources.getImpl());
                            if (oldKey == null) {
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("can't find ResourcesKey for resources impl=");
                                stringBuilder.append(resources.getImpl());
                                Slog.e(str, stringBuilder.toString());
                            } else {
                                Configuration rebasedOverrideConfig = new Configuration();
                                if (configuration != null) {
                                    rebasedOverrideConfig.setTo(configuration);
                                }
                                if (activityHasOverrideConfig && oldKey.hasOverrideConfiguration()) {
                                    rebasedOverrideConfig.updateFrom(Configuration.generateDelta(oldConfig, oldKey.mOverrideConfiguration));
                                }
                                activityResources2 = activityResources;
                                ResourcesKey activityResources3 = new ResourcesKey(oldKey.mResDir, oldKey.mSplitResDirs, oldKey.mOverlayDirs, oldKey.mLibDirs, displayId, rebasedOverrideConfig, oldKey.mCompatInfo);
                                ResourcesImpl resourcesImpl = findResourcesImplForKeyLocked(activityResources3);
                                if (resourcesImpl == null) {
                                    resourcesImpl = createResourcesImpl(activityResources3);
                                    if (resourcesImpl != null) {
                                        this.mResourceImpls.put(activityResources3, new WeakReference(resourcesImpl));
                                    }
                                }
                                if (!(resourcesImpl == null || resourcesImpl == resources.getImpl())) {
                                    resources.setImpl(resourcesImpl);
                                }
                                i++;
                                activityResources = activityResources2;
                            }
                        }
                        activityResources2 = activityResources;
                        i++;
                        activityResources = activityResources2;
                    }
                    if (HwPCUtils.enabled() && HwPCUtils.isValidExtDisplayId(displayId) && configuration != null && !configuration.equals(Configuration.EMPTY)) {
                        ActivityThread.currentActivityThread().updateOverrideConfig(configuration);
                        ActivityThread.currentActivityThread().applyConfigurationToResources(configuration);
                    }
                } else {
                    Trace.traceEnd(8192);
                }
            }
        } catch (Throwable th) {
            Trace.traceEnd(8192);
        }
    }

    public final boolean applyConfigurationToResourcesLocked(Configuration config, CompatibilityInfo compat) {
        Configuration configuration = config;
        CompatibilityInfo compatibilityInfo = compat;
        try {
            Trace.traceBegin(8192, "ResourcesManager#applyConfigurationToResourcesLocked");
            if (this.mResConfiguration.isOtherSeqNewer(configuration) || compatibilityInfo != null) {
                Configuration newConfiguration;
                DisplayMetrics defaultDisplayMetrics;
                int changes = this.mResConfiguration.updateFrom(configuration);
                this.mAdjustedDisplays.clear();
                Rect rect = null;
                if (HwPCUtils.enabled() && ActivityThread.currentActivityThread() != null && HwPCUtils.isValidExtDisplayId(ActivityThread.currentActivityThread().mDisplayId)) {
                    DisplayAdjustments adj = new DisplayAdjustments(configuration);
                    if (HwPCUtils.enabledInPad() && (ActivityThread.currentActivityThread().getOverrideConfig() == null || ActivityThread.currentActivityThread().getOverrideConfig().equals(Configuration.EMPTY))) {
                        newConfiguration = new Configuration(configuration);
                        newConfiguration.windowConfiguration.setAppBounds(null);
                        adj = new DisplayAdjustments(newConfiguration);
                    }
                    defaultDisplayMetrics = getDisplayMetrics(ActivityThread.currentActivityThread().mDisplayId, adj);
                } else {
                    defaultDisplayMetrics = getDisplayMetrics();
                }
                if (compatibilityInfo != null && (this.mResCompatibilityInfo == null || !this.mResCompatibilityInfo.equals(compatibilityInfo))) {
                    this.mResCompatibilityInfo = compatibilityInfo;
                    changes |= 3328;
                }
                Resources.updateSystemConfiguration(configuration, defaultDisplayMetrics, compatibilityInfo);
                ApplicationPackageManager.configurationChanged();
                newConfiguration = null;
                boolean z = true;
                int i = this.mResourceImpls.size() - 1;
                while (i >= 0) {
                    ResourcesKey key = (ResourcesKey) this.mResourceImpls.keyAt(i);
                    WeakReference<ResourcesImpl> weakImplRef = (WeakReference) this.mResourceImpls.valueAt(i);
                    ResourcesImpl r = weakImplRef != null ? (ResourcesImpl) weakImplRef.get() : rect;
                    if (r == null) {
                        this.mResourceImpls.removeAt(i);
                    } else if (HwPCUtils.enabled() && ActivityThread.currentActivityThread() != null && HwPCUtils.isValidExtDisplayId(ActivityThread.currentActivityThread().mDisplayId)) {
                        r.updateConfiguration(configuration, defaultDisplayMetrics, compatibilityInfo);
                    } else {
                        if (ActivityThread.DEBUG_CONFIGURATION) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Changing resources ");
                            stringBuilder.append(r);
                            stringBuilder.append(" config to: ");
                            stringBuilder.append(configuration);
                            Slog.v(str, stringBuilder.toString());
                        }
                        int displayId = key.mDisplayId;
                        boolean isDefaultDisplay = displayId == 0 ? z : false;
                        DisplayMetrics dm = defaultDisplayMetrics;
                        boolean hasOverrideConfiguration = key.hasOverrideConfiguration();
                        if (isDefaultDisplay) {
                            if (!hasOverrideConfiguration) {
                                r.updateConfiguration(configuration, dm, compatibilityInfo);
                            }
                        }
                        if (newConfiguration == null) {
                            newConfiguration = new Configuration();
                        }
                        newConfiguration.setTo(configuration);
                        DisplayAdjustments daj = r.getDisplayAdjustments();
                        if (compatibilityInfo != null) {
                            daj = new DisplayAdjustments(daj);
                            daj.setCompatibilityInfo(compatibilityInfo);
                        }
                        DisplayMetrics dm2 = getDisplayMetrics(displayId, daj);
                        if (!isDefaultDisplay) {
                            applyNonDefaultDisplayMetricsToConfiguration(dm2, newConfiguration);
                        }
                        if (hasOverrideConfiguration) {
                            newConfiguration.updateFrom(key.mOverrideConfiguration);
                        }
                        r.updateConfiguration(newConfiguration, dm2, compatibilityInfo);
                    }
                    i--;
                    rect = null;
                    z = true;
                }
                boolean z2 = changes != 0;
                Trace.traceEnd(8192);
                return z2;
            }
            if (ActivityThread.DEBUG_CONFIGURATION) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Skipping new config: curSeq=");
                stringBuilder2.append(this.mResConfiguration.seq);
                stringBuilder2.append(", newSeq=");
                stringBuilder2.append(configuration.seq);
                Slog.v(str2, stringBuilder2.toString());
            }
            Trace.traceEnd(8192);
            return false;
        } catch (Throwable th) {
            Trace.traceEnd(8192);
        }
    }

    public void appendLibAssetForMainAssetPath(String assetPath, String libAsset) {
        Throwable th;
        Object obj = libAsset;
        synchronized (this) {
            String str;
            try {
                int implCount;
                ArrayMap<ResourcesImpl, ResourcesKey> updatedResourceKeys = new ArrayMap();
                int implCount2 = this.mResourceImpls.size();
                int i = 0;
                int i2 = 0;
                while (i2 < implCount2) {
                    String obj2;
                    ResourcesKey key = (ResourcesKey) this.mResourceImpls.keyAt(i2);
                    WeakReference<ResourcesImpl> weakImplRef = (WeakReference) this.mResourceImpls.valueAt(i2);
                    ResourcesImpl impl = weakImplRef != null ? (ResourcesImpl) weakImplRef.get() : null;
                    if (impl == null) {
                        str = assetPath;
                    } else if (Objects.equals(key.mResDir, assetPath) && !ArrayUtils.contains(key.mLibDirs, obj2)) {
                        int newLibAssetCount = 1 + (key.mLibDirs != null ? key.mLibDirs.length : i);
                        String[] newLibAssets = new String[newLibAssetCount];
                        if (key.mLibDirs != null) {
                            System.arraycopy(key.mLibDirs, i, newLibAssets, i, key.mLibDirs.length);
                        }
                        newLibAssets[newLibAssetCount - 1] = obj2;
                        String str2 = key.mResDir;
                        String[] strArr = key.mSplitResDirs;
                        String[] strArr2 = key.mOverlayDirs;
                        i = key.mDisplayId;
                        Configuration configuration = key.mOverrideConfiguration;
                        implCount = implCount2;
                        CompatibilityInfo compatibilityInfo = key.mCompatInfo;
                        key = r12;
                        ResourcesKey resourcesKey = new ResourcesKey(str2, strArr, strArr2, newLibAssets, i, configuration, compatibilityInfo);
                        updatedResourceKeys.put(impl, key);
                        i2++;
                        implCount2 = implCount;
                        obj2 = libAsset;
                        i = 0;
                    }
                    implCount = implCount2;
                    i2++;
                    implCount2 = implCount;
                    obj2 = libAsset;
                    i = 0;
                }
                str = assetPath;
                implCount = implCount2;
                redirectResourcesToNewImplLocked(updatedResourceKeys);
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    final void applyNewResourceDirsLocked(String baseCodePath, String[] newResourceDirs) {
        Throwable th;
        String str;
        try {
            Trace.traceBegin(8192, "ResourcesManager#applyNewResourceDirsLocked");
            ArrayMap<ResourcesImpl, ResourcesKey> updatedResourceKeys = new ArrayMap();
            int implCount = this.mResourceImpls.size();
            for (int i = 0; i < implCount; i++) {
                ResourcesKey key = (ResourcesKey) this.mResourceImpls.keyAt(i);
                WeakReference<ResourcesImpl> weakImplRef = (WeakReference) this.mResourceImpls.valueAt(i);
                ResourcesImpl impl = weakImplRef != null ? (ResourcesImpl) weakImplRef.get() : null;
                if (impl != null) {
                    if (key.mResDir != null) {
                        try {
                            if (key.mResDir.equals(baseCodePath)) {
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            Trace.traceEnd(8192);
                            throw th;
                        }
                    }
                    str = baseCodePath;
                    updatedResourceKeys.put(impl, new ResourcesKey(key.mResDir, key.mSplitResDirs, newResourceDirs, key.mLibDirs, key.mDisplayId, key.mOverrideConfiguration, key.mCompatInfo));
                } else {
                    str = baseCodePath;
                }
            }
            str = baseCodePath;
            redirectResourcesToNewImplLocked(updatedResourceKeys);
            Trace.traceEnd(8192);
        } catch (Throwable th3) {
            th = th3;
            str = baseCodePath;
            Trace.traceEnd(8192);
            throw th;
        }
    }

    private void redirectResourcesToNewImplLocked(ArrayMap<ResourcesImpl, ResourcesKey> updatedResourceKeys) {
        if (!updatedResourceKeys.isEmpty()) {
            int resourcesCount = this.mResourceReferences.size();
            int i = 0;
            while (true) {
                Resources r = null;
                if (i < resourcesCount) {
                    WeakReference<Resources> ref = (WeakReference) this.mResourceReferences.get(i);
                    if (ref != null) {
                        r = (Resources) ref.get();
                    }
                    if (r != null) {
                        ResourcesKey key = (ResourcesKey) updatedResourceKeys.get(r.getImpl());
                        if (key != null) {
                            ResourcesImpl impl = findOrCreateResourcesImplForKeyLocked(key);
                            if (impl != null) {
                                r.setImpl(impl);
                            } else {
                                throw new NotFoundException("failed to redirect ResourcesImpl");
                            }
                        }
                        continue;
                    }
                    i++;
                } else {
                    for (ActivityResources activityResources : this.mActivityResourceReferences.values()) {
                        int resCount = activityResources.activityResources.size();
                        for (int i2 = 0; i2 < resCount; i2++) {
                            WeakReference<Resources> ref2 = (WeakReference) activityResources.activityResources.get(i2);
                            Resources r2 = ref2 != null ? (Resources) ref2.get() : null;
                            if (r2 != null) {
                                ResourcesKey key2 = (ResourcesKey) updatedResourceKeys.get(r2.getImpl());
                                if (key2 != null) {
                                    ResourcesImpl impl2 = findOrCreateResourcesImplForKeyLocked(key2);
                                    if (impl2 != null) {
                                        r2.setImpl(impl2);
                                    } else {
                                        throw new NotFoundException("failed to redirect ResourcesImpl");
                                    }
                                }
                                continue;
                            }
                        }
                    }
                    return;
                }
            }
        }
    }
}
