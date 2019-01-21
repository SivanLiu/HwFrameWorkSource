package android.content.res;

import android.animation.Animator;
import android.animation.StateListAnimator;
import android.app.ActivityThread;
import android.app.slice.Slice;
import android.common.HwFrameworkFactory;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager.AssetInputStream;
import android.content.res.FontResourcesParser.FamilyResourceEntry;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.AssetInputStreamSource;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.graphics.drawable.DrawableContainer;
import android.hwtheme.HwThemeManager;
import android.icu.text.PluralRules;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.TypedValue;
import android.util.Xml;
import android.view.DisplayAdjustments;
import com.android.internal.util.GrowingArrayUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParserException;

public class ResourcesImpl extends AbsResourcesImpl {
    private static final int CONFIG_DENSITY = ActivityInfo.activityInfoConfigJavaToNative(4096);
    private static final int CONFIG_FONT_SCALE = ActivityInfo.activityInfoConfigJavaToNative(1073741824);
    private static final int CONFIG_ORIENTATION = ActivityInfo.activityInfoConfigJavaToNative(128);
    private static final int CONFIG_SCREEN_SIZE = 1024;
    private static final int CONFIG_SMALLEST_SCREEN_SIZE = ActivityInfo.activityInfoConfigJavaToNative(2048);
    private static final boolean DEBUG_CONFIG = false;
    private static final boolean DEBUG_LOAD = false;
    private static final int ID_OTHER = 16777220;
    private static final int SPEC_PUBLIC = 1073741824;
    static final String TAG = "Resources";
    static final String TAG_PRELOAD = "Resources.preload";
    public static final boolean TRACE_FOR_DETAILED_PRELOAD = SystemProperties.getBoolean("debug.trace_resource_preload", false);
    private static final boolean TRACE_FOR_MISS_PRELOAD = false;
    private static final boolean TRACE_FOR_PRELOAD = false;
    private static final int XML_BLOCK_CACHE_SIZE = 4;
    public static final boolean mIsLockResWhitelist = SystemProperties.getBoolean("ro.config.hw_lock_res_whitelist", false);
    private static int sPreloadTracingNumLoadedDrawables;
    private static boolean sPreloaded;
    private static final LongSparseArray<ConstantState> sPreloadedColorDrawables = new LongSparseArray();
    private static final LongSparseArray<ConstantState<ComplexColor>> sPreloadedComplexColors = new LongSparseArray();
    private static int sPreloadedDensity;
    private static final LongSparseArray<ConstantState>[] sPreloadedDrawables = new LongSparseArray[2];
    private static final Object sSync = new Object();
    private final Object mAccessLock = new Object();
    private final ConfigurationBoundResourceCache<Animator> mAnimatorCache = new ConfigurationBoundResourceCache();
    final AssetManager mAssets;
    private final int[] mCachedXmlBlockCookies = new int[4];
    private final String[] mCachedXmlBlockFiles = new String[4];
    private final XmlBlock[] mCachedXmlBlocks = new XmlBlock[4];
    private final DrawableCache mColorDrawableCache = new DrawableCache();
    private final ConfigurationBoundResourceCache<ComplexColor> mComplexColorCache = new ConfigurationBoundResourceCache();
    private final Configuration mConfiguration = new Configuration();
    private final DisplayAdjustments mDisplayAdjustments;
    private final DrawableCache mDrawableCache = new DrawableCache();
    private AbsResourcesImpl mHwResourcesImpl;
    private int mLastCachedXmlBlockIndex = -1;
    private boolean mLockDpi = false;
    private boolean mLockRes = true;
    private final ThreadLocal<LookupStack> mLookupStack = ThreadLocal.withInitial(-$$Lambda$ResourcesImpl$h3PTRX185BeQl8SVC2_w9arp5Og.INSTANCE);
    private final DisplayMetrics mMetrics = new DisplayMetrics();
    private PluralRules mPluralRule;
    private long mPreloadTracingPreloadStartTime;
    private long mPreloadTracingStartBitmapCount;
    private long mPreloadTracingStartBitmapSize;
    public boolean mPreloading;
    private final ConfigurationBoundResourceCache<StateListAnimator> mStateListAnimatorCache = new ConfigurationBoundResourceCache();
    private final Configuration mTmpConfig = new Configuration();

    private static class LookupStack {
        private int[] mIds;
        private int mSize;

        private LookupStack() {
            this.mIds = new int[4];
            this.mSize = 0;
        }

        public void push(int id) {
            this.mIds = GrowingArrayUtils.append(this.mIds, this.mSize, id);
            this.mSize++;
        }

        public boolean contains(int id) {
            for (int i = 0; i < this.mSize; i++) {
                if (this.mIds[i] == id) {
                    return true;
                }
            }
            return false;
        }

        public void pop() {
            this.mSize--;
        }
    }

    public class ThemeImpl {
        private final AssetManager mAssets;
        private final ThemeKey mKey = new ThemeKey();
        private final long mTheme;
        private int mThemeResId = 0;

        ThemeImpl() {
            this.mAssets = ResourcesImpl.this.mAssets;
            this.mTheme = this.mAssets.createTheme();
        }

        protected void finalize() throws Throwable {
            super.finalize();
            this.mAssets.releaseTheme(this.mTheme);
        }

        ThemeKey getKey() {
            return this.mKey;
        }

        long getNativeTheme() {
            return this.mTheme;
        }

        int getAppliedStyleResId() {
            return this.mThemeResId;
        }

        void applyStyle(int resId, boolean force) {
            synchronized (this.mKey) {
                this.mAssets.applyStyleToTheme(this.mTheme, resId, force);
                this.mThemeResId = resId;
                this.mKey.append(resId, force);
            }
        }

        void setTo(ThemeImpl other) {
            synchronized (this.mKey) {
                synchronized (other.mKey) {
                    AssetManager.nativeThemeCopy(this.mTheme, other.mTheme);
                    this.mThemeResId = other.mThemeResId;
                    this.mKey.setTo(other.getKey());
                }
            }
        }

        TypedArray obtainStyledAttributes(Theme wrapper, AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes) {
            Throwable th;
            synchronized (this.mKey) {
                int[] iArr = attrs;
                try {
                    int len = iArr.length;
                    TypedArray array = TypedArray.obtain(wrapper.getResources(), len);
                    Parser parser = (Parser) set;
                    Parser parser2 = parser;
                    this.mAssets.applyStyle(this.mTheme, defStyleAttr, defStyleRes, parser, iArr, array.mDataAddress, array.mIndicesAddress);
                    array.mTheme = wrapper;
                    array.mXml = parser2;
                    return array;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }

        TypedArray resolveAttributes(Theme wrapper, int[] values, int[] attrs) {
            TypedArray array;
            synchronized (this.mKey) {
                int len = attrs.length;
                if (values == null || len != values.length) {
                    throw new IllegalArgumentException("Base attribute values must the same length as attrs");
                }
                array = TypedArray.obtain(wrapper.getResources(), len);
                this.mAssets.resolveAttrs(this.mTheme, 0, 0, values, attrs, array.mData, array.mIndices);
                array.mTheme = wrapper;
                array.mXml = null;
            }
            return array;
        }

        boolean resolveAttribute(int resid, TypedValue outValue, boolean resolveRefs) {
            boolean themeValue;
            synchronized (this.mKey) {
                themeValue = this.mAssets.getThemeValue(this.mTheme, resid, outValue, resolveRefs);
            }
            return themeValue;
        }

        int[] getAllAttributes() {
            return this.mAssets.getStyleAttributes(getAppliedStyleResId());
        }

        int getChangingConfigurations() {
            int activityInfoConfigNativeToJava;
            synchronized (this.mKey) {
                activityInfoConfigNativeToJava = ActivityInfo.activityInfoConfigNativeToJava(AssetManager.nativeThemeGetChangingConfigurations(this.mTheme));
            }
            return activityInfoConfigNativeToJava;
        }

        public void dump(int priority, String tag, String prefix) {
            synchronized (this.mKey) {
                this.mAssets.dumpTheme(this.mTheme, priority, tag, prefix);
            }
        }

        String[] getTheme() {
            String[] themes;
            synchronized (this.mKey) {
                int N = this.mKey.mCount;
                themes = new String[(N * 2)];
                int i = 0;
                int j = N - 1;
                while (i < themes.length) {
                    int resId = this.mKey.mResId[j];
                    boolean forced = this.mKey.mForce[j];
                    try {
                        themes[i] = ResourcesImpl.this.getResourceName(resId);
                    } catch (NotFoundException e) {
                        themes[i] = Integer.toHexString(i);
                    }
                    themes[i + 1] = forced ? "forced" : "not forced";
                    i += 2;
                    j--;
                }
            }
            return themes;
        }

        void rebase() {
            synchronized (this.mKey) {
                AssetManager.nativeThemeClear(this.mTheme);
                for (int i = 0; i < this.mKey.mCount; i++) {
                    this.mAssets.applyStyleToTheme(this.mTheme, this.mKey.mResId[i], this.mKey.mForce[i]);
                }
            }
        }
    }

    static {
        sPreloadedDrawables[0] = new LongSparseArray();
        sPreloadedDrawables[1] = new LongSparseArray();
    }

    public ResourcesImpl(AssetManager assets, DisplayMetrics metrics, Configuration config, DisplayAdjustments displayAdjustments) {
        this.mAssets = assets;
        this.mHwResourcesImpl = HwThemeManager.getHwResourcesImpl();
        this.mHwResourcesImpl.setResourcesImpl(this);
        this.mHwResourcesImpl.setHwTheme(config);
        if (!(HwPCUtils.enabled() && ActivityThread.currentActivityThread() != null && HwPCUtils.isValidExtDisplayId(ActivityThread.currentActivityThread().getDisplayId()))) {
            this.mHwResourcesImpl.initDeepTheme();
        }
        this.mMetrics.setToDefaults();
        this.mDisplayAdjustments = displayAdjustments;
        this.mConfiguration.setToDefaults();
        updateConfiguration(config, metrics, displayAdjustments.getCompatibilityInfo());
    }

    public AbsResourcesImpl getHwResourcesImpl() {
        return this.mHwResourcesImpl;
    }

    public DisplayAdjustments getDisplayAdjustments() {
        return this.mDisplayAdjustments;
    }

    public AssetManager getAssets() {
        return this.mAssets;
    }

    DisplayMetrics getDisplayMetrics() {
        return this.mMetrics;
    }

    Configuration getConfiguration() {
        return this.mConfiguration;
    }

    Configuration[] getSizeConfigurations() {
        return this.mAssets.getSizeConfigurations();
    }

    CompatibilityInfo getCompatibilityInfo() {
        return this.mDisplayAdjustments.getCompatibilityInfo();
    }

    private PluralRules getPluralRule() {
        PluralRules pluralRules;
        synchronized (sSync) {
            if (this.mPluralRule == null) {
                this.mPluralRule = PluralRules.forLocale(this.mConfiguration.getLocales().get(0));
            }
            pluralRules = this.mPluralRule;
        }
        return pluralRules;
    }

    void getValue(int id, TypedValue outValue, boolean resolveRefs) throws NotFoundException {
        if (!this.mAssets.getResourceValue(id, 0, outValue, resolveRefs)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Resource ID #0x");
            stringBuilder.append(Integer.toHexString(id));
            throw new NotFoundException(stringBuilder.toString());
        }
    }

    void getValueForDensity(int id, int density, TypedValue outValue, boolean resolveRefs) throws NotFoundException {
        if (!this.mAssets.getResourceValue(id, density, outValue, resolveRefs)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Resource ID #0x");
            stringBuilder.append(Integer.toHexString(id));
            throw new NotFoundException(stringBuilder.toString());
        }
    }

    void getValue(String name, TypedValue outValue, boolean resolveRefs) throws NotFoundException {
        int id = getIdentifier(name, "string", null);
        if (id != 0) {
            getValue(id, outValue, resolveRefs);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("String resource name ");
        stringBuilder.append(name);
        throw new NotFoundException(stringBuilder.toString());
    }

    int getIdentifier(String name, String defType, String defPackage) {
        if (name != null) {
            try {
                return Integer.parseInt(name);
            } catch (Exception e) {
                return this.mAssets.getResourceIdentifier(name, defType, defPackage);
            }
        }
        throw new NullPointerException("name is null");
    }

    String getResourceName(int resid) throws NotFoundException {
        String str = this.mAssets.getResourceName(resid);
        if (str != null) {
            return str;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unable to find resource ID #0x");
        stringBuilder.append(Integer.toHexString(resid));
        throw new NotFoundException(stringBuilder.toString());
    }

    String getResourcePackageName(int resid) throws NotFoundException {
        String str = this.mAssets.getResourcePackageName(resid);
        if (str != null) {
            return str;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unable to find resource ID #0x");
        stringBuilder.append(Integer.toHexString(resid));
        throw new NotFoundException(stringBuilder.toString());
    }

    String getResourceTypeName(int resid) throws NotFoundException {
        String str = this.mAssets.getResourceTypeName(resid);
        if (str != null) {
            return str;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unable to find resource ID #0x");
        stringBuilder.append(Integer.toHexString(resid));
        throw new NotFoundException(stringBuilder.toString());
    }

    String getResourceEntryName(int resid) throws NotFoundException {
        String str = this.mAssets.getResourceEntryName(resid);
        if (str != null) {
            return str;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unable to find resource ID #0x");
        stringBuilder.append(Integer.toHexString(resid));
        throw new NotFoundException(stringBuilder.toString());
    }

    CharSequence getQuantityText(int id, int quantity) throws NotFoundException {
        PluralRules rule = getPluralRule();
        CharSequence res = this.mAssets.getResourceBagText(id, attrForQuantityCode(rule.select((double) quantity)));
        if (res != null) {
            return res;
        }
        res = this.mAssets.getResourceBagText(id, ID_OTHER);
        if (res != null) {
            return res;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Plural resource ID #0x");
        stringBuilder.append(Integer.toHexString(id));
        stringBuilder.append(" quantity=");
        stringBuilder.append(quantity);
        stringBuilder.append(" item=");
        stringBuilder.append(rule.select((double) quantity));
        throw new NotFoundException(stringBuilder.toString());
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int attrForQuantityCode(String quantityCode) {
        Object obj;
        switch (quantityCode.hashCode()) {
            case 101272:
                if (quantityCode.equals("few")) {
                    obj = 3;
                    break;
                }
            case 110182:
                if (quantityCode.equals("one")) {
                    obj = 1;
                    break;
                }
            case 115276:
                if (quantityCode.equals("two")) {
                    obj = 2;
                    break;
                }
            case 3343967:
                if (quantityCode.equals("many")) {
                    obj = 4;
                    break;
                }
            case 3735208:
                if (quantityCode.equals("zero")) {
                    obj = null;
                    break;
                }
            default:
                obj = -1;
                break;
        }
        switch (obj) {
            case null:
                return 16777221;
            case 1:
                return 16777222;
            case 2:
                return 16777223;
            case 3:
                return 16777224;
            case 4:
                return 16777225;
            default:
                return ID_OTHER;
        }
    }

    AssetFileDescriptor openRawResourceFd(int id, TypedValue tempValue) throws NotFoundException {
        getValue(id, tempValue, true);
        try {
            return this.mAssets.openNonAssetFd(tempValue.assetCookie, tempValue.string.toString());
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("File ");
            stringBuilder.append(tempValue.string.toString());
            stringBuilder.append(" from drawable resource ID #0x");
            stringBuilder.append(Integer.toHexString(id));
            throw new NotFoundException(stringBuilder.toString(), e);
        }
    }

    InputStream openRawResource(int id, TypedValue value) throws NotFoundException {
        getValue(id, value, true);
        try {
            return this.mAssets.openNonAsset(value.assetCookie, value.string.toString(), 2);
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("File ");
            stringBuilder.append(value.string == null ? "(null)" : value.string.toString());
            stringBuilder.append(" from drawable resource ID #0x");
            stringBuilder.append(Integer.toHexString(id));
            NotFoundException rnf = new NotFoundException(stringBuilder.toString());
            rnf.initCause(e);
            throw rnf;
        }
    }

    ConfigurationBoundResourceCache<Animator> getAnimatorCache() {
        return this.mAnimatorCache;
    }

    ConfigurationBoundResourceCache<StateListAnimator> getStateListAnimatorCache() {
        return this.mStateListAnimatorCache;
    }

    /* JADX WARNING: Missing block: B:75:?, code skipped:
            r4 = sSync;
     */
    /* JADX WARNING: Missing block: B:76:0x0171, code skipped:
            monitor-enter(r4);
     */
    /* JADX WARNING: Missing block: B:79:0x0174, code skipped:
            if (r1.mPluralRule == null) goto L_0x0186;
     */
    /* JADX WARNING: Missing block: B:80:0x0176, code skipped:
            r1.mPluralRule = android.icu.text.PluralRules.forLocale(r1.mConfiguration.getLocales().get(0));
     */
    /* JADX WARNING: Missing block: B:81:0x0186, code skipped:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:82:0x0187, code skipped:
            android.os.Trace.traceEnd(8192);
     */
    /* JADX WARNING: Missing block: B:83:0x018d, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:93:0x0193, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateConfiguration(Configuration config, DisplayMetrics metrics, CompatibilityInfo compat) {
        Throwable th;
        Configuration configuration;
        DisplayMetrics displayMetrics = metrics;
        CompatibilityInfo compatibilityInfo = compat;
        Trace.traceBegin(8192, "ResourcesImpl#updateConfiguration");
        try {
            synchronized (this.mAccessLock) {
                if (compatibilityInfo != null) {
                    try {
                        this.mDisplayAdjustments.setCompatibilityInfo(compatibilityInfo);
                    } catch (Throwable th2) {
                        th = th2;
                        configuration = config;
                        throw th;
                    }
                }
                if (displayMetrics != null) {
                    this.mMetrics.setTo(displayMetrics);
                }
                this.mDisplayAdjustments.getCompatibilityInfo().applyToDisplayMetrics(this.mMetrics);
                if (HwPCUtils.enabledInPad() || ActivityThread.currentActivityThread() == null || !HwPCUtils.isValidExtDisplayId(ActivityThread.currentActivityThread().getDisplayId())) {
                    String pkgName = ActivityThread.currentPackageName();
                    Bundle multidpiInfo = null;
                    if (!(pkgName == null || pkgName.length() == 0)) {
                        multidpiInfo = this.mHwResourcesImpl.getMultidpiInfo(pkgName);
                    }
                    if (multidpiInfo != null) {
                        this.mLockDpi = multidpiInfo.getBoolean("LockDpi", false);
                        this.mLockRes = multidpiInfo.getBoolean("LockRes", true);
                        if (this.mLockDpi) {
                            this.mMetrics.setToNonCompat();
                        }
                    }
                }
                int configChanges = calcConfigChanges(config);
                LocaleList locales = this.mConfiguration.getLocales();
                if (locales.isEmpty()) {
                    locales = LocaleList.getDefault();
                    this.mConfiguration.setLocales(locales);
                }
                boolean changeRes = false;
                if ((configChanges & 4) != 0) {
                    if (locales.size() > 1) {
                        String[] availableLocales = this.mAssets.getNonSystemLocales();
                        if (LocaleList.isPseudoLocalesOnly(availableLocales) || availableLocales.length == 1) {
                            availableLocales = this.mAssets.getLocales();
                            if (LocaleList.isPseudoLocalesOnly(availableLocales)) {
                                availableLocales = null;
                            }
                        }
                        if (availableLocales != null) {
                            boolean hasDbid = false;
                            for (String oneLocale : availableLocales) {
                                if (oneLocale.equals("zz-ZX")) {
                                    hasDbid = true;
                                    break;
                                }
                            }
                            if (hasDbid) {
                                String[] internal = availableLocales;
                                String[] shared = AssetManager.getSharedResList();
                                if (shared != null) {
                                    String[] result = (String[]) Arrays.copyOf(internal, internal.length + shared.length);
                                    System.arraycopy(shared, 0, result, internal.length, shared.length);
                                    availableLocales = result;
                                    if (shared.length > 0) {
                                        changeRes = true;
                                    }
                                }
                            }
                            Locale bestLocale = locales.getFirstMatchWithEnglishSupported(availableLocales);
                            if (!(bestLocale == null || bestLocale == locales.get(0))) {
                                this.mConfiguration.setLocales(new LocaleList(bestLocale, locales));
                            }
                        }
                    } else {
                        changeRes = true;
                    }
                }
                if (!(this.mConfiguration.densityDpi == 0 || this.mLockDpi)) {
                    this.mMetrics.densityDpi = this.mConfiguration.densityDpi;
                    this.mMetrics.density = ((float) this.mConfiguration.densityDpi) * 0.00625f;
                }
                this.mMetrics.scaledDensity = this.mMetrics.density * (this.mConfiguration.fontScale != 0.0f ? this.mConfiguration.fontScale : 1.0f);
                updateDpiConfiguration();
                updateAssetConfiguration(ActivityThread.currentPackageName(), true, changeRes);
                this.mDrawableCache.onConfigurationChange(configChanges);
                this.mColorDrawableCache.onConfigurationChange(configChanges);
                this.mComplexColorCache.onConfigurationChange(configChanges);
                this.mAnimatorCache.onConfigurationChange(configChanges);
                this.mStateListAnimatorCache.onConfigurationChange(configChanges);
                flushLayoutCache();
                try {
                    this.mHwResourcesImpl.updateConfiguration(config, configChanges);
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
        } catch (Throwable th4) {
            th = th4;
            configuration = config;
            Trace.traceEnd(8192);
            throw th;
        }
    }

    public void updateAssetConfiguration(String packageName) {
        updateAssetConfiguration(packageName, false, false);
    }

    /* JADX WARNING: Missing block: B:16:0x0057, code skipped:
            if (getHwResourcesImpl().isInMultiDpiWhiteList(r39) == false) goto L_0x005c;
     */
    /* JADX WARNING: Missing block: B:21:0x0062, code skipped:
            if (r0.mLockRes != false) goto L_0x0064;
     */
    /* JADX WARNING: Missing block: B:22:0x0064, code skipped:
            r7 = android.os.SystemProperties.getInt("ro.sf.real_lcd_density", android.os.SystemProperties.getInt("ro.sf.lcd_density", 0));
            r9 = android.os.SystemProperties.getInt("persist.sys.dpi", r7);
     */
    /* JADX WARNING: Missing block: B:23:0x0079, code skipped:
            if (r9 > 0) goto L_0x007c;
     */
    /* JADX WARNING: Missing block: B:24:0x007b, code skipped:
            r9 = r7;
     */
    /* JADX WARNING: Missing block: B:25:0x007c, code skipped:
            if (r9 == r7) goto L_0x00a0;
     */
    /* JADX WARNING: Missing block: B:26:0x007e, code skipped:
            if (r9 == 0) goto L_0x00a0;
     */
    /* JADX WARNING: Missing block: B:27:0x0080, code skipped:
            if (r7 == 0) goto L_0x00a0;
     */
    /* JADX WARNING: Missing block: B:28:0x0082, code skipped:
            r35 = (((r4 * r9) + r9) - 1) / r7;
            r36 = (((r5 * r9) + r9) - 1) / r7;
            r34 = (((r2 * r9) + r9) - 1) / r7;
            r2 = (((r6 * r7) + r9) - 1) / r9;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateAssetConfiguration(String packageName, boolean resFlag, boolean changeRes) {
        int width;
        int i;
        if (this.mMetrics.widthPixels >= this.mMetrics.heightPixels) {
            width = this.mMetrics.widthPixels;
            i = this.mMetrics.heightPixels;
        } else {
            width = this.mMetrics.heightPixels;
            i = this.mMetrics.widthPixels;
        }
        int height = i;
        if (this.mConfiguration.keyboardHidden == 1 && this.mConfiguration.hardKeyboardHidden == 2) {
            i = 3;
        } else {
            i = this.mConfiguration.keyboardHidden;
        }
        int keyboardHidden = i;
        i = this.mConfiguration.smallestScreenWidthDp;
        int screenWidthDp = this.mConfiguration.screenWidthDp;
        int screenHeightDp = this.mConfiguration.screenHeightDp;
        int densityDpi = this.mConfiguration.densityDpi;
        String str;
        if (this.mLockDpi) {
            str = packageName;
        } else {
            if (!mIsLockResWhitelist) {
                str = packageName;
            }
            if (!mIsLockResWhitelist) {
            }
        }
        int smallestScreenWidthDp = i;
        int screenWidthDp2 = screenWidthDp;
        int screenHeightDp2 = screenHeightDp;
        i = densityDpi;
        int i2;
        int i3;
        int i4;
        int i5;
        if (resFlag) {
            AssetManager assetManager = this.mAssets;
            screenWidthDp = this.mConfiguration.mcc;
            screenHeightDp = this.mConfiguration.mnc;
            String adjustLanguageTag = adjustLanguageTag(this.mConfiguration.getLocales().get(0).toLanguageTag());
            i2 = this.mConfiguration.orientation;
            i3 = this.mConfiguration.touchscreen;
            i4 = this.mConfiguration.keyboard;
            int i6 = this.mConfiguration.navigation;
            int i7 = this.mConfiguration.screenLayout;
            int i8 = this.mConfiguration.uiMode;
            int i9 = this.mConfiguration.colorMode;
            i5 = i;
            int i10 = i7;
            i7 = width;
            int i11 = smallestScreenWidthDp;
            int i12 = screenWidthDp2;
            int i13 = screenHeightDp2;
            assetManager.setConfiguration(screenWidthDp, screenHeightDp, adjustLanguageTag, i2, i3, i5, i4, keyboardHidden, i6, i7, height, i11, i12, i13, i10, i8, i9, VERSION.RESOURCES_SDK_INT, changeRes);
            return;
        }
        AssetManager assetManager2 = this.mAssets;
        int i14 = this.mConfiguration.mcc;
        screenWidthDp = this.mConfiguration.mnc;
        String adjustLanguageTag2 = adjustLanguageTag(this.mConfiguration.getLocales().get(0).toLanguageTag());
        screenHeightDp = this.mConfiguration.orientation;
        densityDpi = this.mConfiguration.touchscreen;
        i2 = this.mConfiguration.keyboard;
        i3 = this.mConfiguration.navigation;
        i5 = this.mConfiguration.screenLayout;
        i4 = this.mConfiguration.uiMode;
        assetManager2.setConfiguration(i14, screenWidthDp, adjustLanguageTag2, screenHeightDp, densityDpi, i, i2, keyboardHidden, i3, width, height, smallestScreenWidthDp, screenWidthDp2, screenHeightDp2, i5, i4, this.mConfiguration.colorMode, VERSION.RESOURCES_SDK_INT);
    }

    private void updateDpiConfiguration() {
        if (this.mLockDpi) {
            int smallestScreenWidthDp = this.mConfiguration.smallestScreenWidthDp;
            int screenWidthDp = this.mConfiguration.screenWidthDp;
            int screenHeightDp = this.mConfiguration.screenHeightDp;
            int densityDpi = this.mConfiguration.densityDpi;
            int screenLayout = this.mConfiguration.screenLayout;
            int srcDpi = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0));
            int dpi = SystemProperties.getInt("persist.sys.dpi", srcDpi);
            int rogDpi = SystemProperties.getInt("persist.sys.realdpi", srcDpi);
            if (dpi <= 0) {
                dpi = srcDpi;
            }
            if (dpi != srcDpi && dpi != 0 && srcDpi != 0 && densityDpi != srcDpi && densityDpi != ((int) (((((float) (srcDpi * rogDpi)) * 1.0f) / ((float) dpi)) * 1.0f))) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("original config :");
                stringBuilder.append(this.mConfiguration);
                Slog.i(str, stringBuilder.toString());
                int densityDpi2 = (((densityDpi * srcDpi) + dpi) - 1) / dpi;
                smallestScreenWidthDp = (((screenWidthDp * dpi) + dpi) - 1) / srcDpi;
                screenWidthDp = (((screenHeightDp * dpi) + dpi) - 1) / srcDpi;
                this.mConfiguration.smallestScreenWidthDp = (((smallestScreenWidthDp * dpi) + dpi) - 1) / srcDpi;
                this.mConfiguration.screenWidthDp = smallestScreenWidthDp;
                this.mConfiguration.screenHeightDp = screenWidthDp;
                this.mConfiguration.densityDpi = densityDpi2;
                if ("com.android.browser".equals(ActivityThread.currentPackageName())) {
                    int longSizeDp = screenWidthDp > smallestScreenWidthDp ? screenWidthDp : smallestScreenWidthDp;
                    int shortSizeDp = screenWidthDp > smallestScreenWidthDp ? smallestScreenWidthDp : screenWidthDp;
                    int screenLayoutSize = screenLayout & 15;
                    if (longSizeDp < 470) {
                        screenLayoutSize = 1;
                    } else if (longSizeDp >= 960 && shortSizeDp >= 720) {
                        screenLayoutSize = 4;
                    } else if (longSizeDp < 640 || shortSizeDp < 480) {
                        screenLayoutSize = 2;
                    } else {
                        screenLayoutSize = 3;
                    }
                    this.mConfiguration.screenLayout = (screenLayout & -16) | screenLayoutSize;
                }
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("set to no compat config  to:");
                stringBuilder2.append(this.mConfiguration);
                Slog.i(str2, stringBuilder2.toString());
            }
        }
    }

    public int calcConfigChanges(Configuration config) {
        if (config == null) {
            return -1;
        }
        this.mTmpConfig.setTo(config);
        int density = config.densityDpi;
        if (density == 0) {
            density = this.mMetrics.noncompatDensityDpi;
        }
        this.mDisplayAdjustments.getCompatibilityInfo().applyToConfiguration(density, this.mTmpConfig);
        if (this.mTmpConfig.getLocales().isEmpty()) {
            this.mTmpConfig.setLocales(LocaleList.getDefault());
        }
        return this.mConfiguration.updateFrom(this.mTmpConfig);
    }

    private static String adjustLanguageTag(String languageTag) {
        String language;
        String remainder;
        int separator = languageTag.indexOf(45);
        if (separator == -1) {
            language = languageTag;
            remainder = "";
        } else {
            language = languageTag.substring(0, separator);
            remainder = languageTag.substring(separator);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Locale.adjustLanguageCode(language));
        stringBuilder.append(remainder);
        return stringBuilder.toString();
    }

    public void flushLayoutCache() {
        synchronized (this.mCachedXmlBlocks) {
            int i = 0;
            Arrays.fill(this.mCachedXmlBlockCookies, 0);
            Arrays.fill(this.mCachedXmlBlockFiles, null);
            XmlBlock[] cachedXmlBlocks = this.mCachedXmlBlocks;
            while (i < 4) {
                XmlBlock oldBlock = cachedXmlBlocks[i];
                if (oldBlock != null) {
                    oldBlock.close();
                }
                i++;
            }
            Arrays.fill(cachedXmlBlocks, null);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:74:0x011a A:{Catch:{ Exception -> 0x0116 }} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x0111  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x011f A:{Catch:{ Exception -> 0x0116 }} */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x0144 A:{Catch:{ Exception -> 0x0116 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected Drawable loadDrawable(Resources wrapper, TypedValue value, int id, int density, Theme theme) throws NotFoundException {
        Exception e;
        int i;
        Exception e2;
        String name;
        NotFoundException notFoundException;
        StringBuilder stringBuilder;
        ResourcesImpl resourcesImpl = this;
        Resources resources = wrapper;
        TypedValue typedValue = value;
        int i2 = id;
        int i3 = density;
        Theme theme2 = theme;
        boolean canApplyTheme = true;
        boolean z = i3 == 0 || typedValue.density == resourcesImpl.mMetrics.densityDpi;
        boolean useCache = z;
        if (i3 > 0 && typedValue.density > 0 && typedValue.density != 65535) {
            if (typedValue.density == i3) {
                typedValue.density = resourcesImpl.mMetrics.densityDpi;
            } else {
                typedValue.density = (typedValue.density * resourcesImpl.mMetrics.densityDpi) / i3;
            }
        }
        try {
            DrawableCache caches;
            long key;
            Drawable cachedDrawable;
            long key2;
            ConstantState cs;
            long key3;
            Drawable dr;
            boolean z2;
            boolean needsNewDrawableAfterCache;
            Drawable dr2;
            ConstantState constantState;
            DrawableCache drawableCache;
            long j;
            if (typedValue.type < 28 || typedValue.type > 31) {
                z = false;
                caches = resourcesImpl.mDrawableCache;
                key = (((long) typedValue.assetCookie) << 32) | ((long) typedValue.data);
            } else {
                z = true;
                caches = resourcesImpl.mColorDrawableCache;
                key = (long) typedValue.data;
            }
            boolean isColorDrawable = z;
            DrawableCache caches2 = caches;
            long key4 = key;
            if (!resourcesImpl.mPreloading && useCache) {
                cachedDrawable = caches2.getInstance(key4, resources, theme2);
                if (cachedDrawable != null) {
                    cachedDrawable.setChangingConfigurations(typedValue.changingConfigurations);
                    if (HwFrameworkFactory.getHwActivityThread() != null) {
                        HwFrameworkFactory.getHwActivityThread().hitDrawableCache(i2);
                    }
                    return cachedDrawable;
                }
            }
            if (resourcesImpl.mHwResourcesImpl != null) {
                key2 = key4;
                try {
                    cachedDrawable = resourcesImpl.mHwResourcesImpl.loadDrawable(resources, typedValue, i2, theme2, useCache);
                    if (cachedDrawable != null) {
                        return cachedDrawable;
                    }
                } catch (Exception e3) {
                    e = e3;
                    resourcesImpl = this;
                    i = 0;
                    e2 = e;
                    try {
                        name = resourcesImpl.getResourceName(i2);
                    } catch (NotFoundException e4) {
                        notFoundException = e4;
                        name = "(missing name)";
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Drawable ");
                    stringBuilder.append(name);
                    stringBuilder.append(" with resource ID #0x");
                    stringBuilder.append(Integer.toHexString(id));
                    notFoundException = new NotFoundException(stringBuilder.toString(), e2);
                    notFoundException.setStackTrace(new StackTraceElement[i]);
                    throw notFoundException;
                }
            }
            key2 = key4;
            if (isColorDrawable) {
                cs = (ConstantState) sPreloadedColorDrawables.get(key2);
                key3 = key2;
                resourcesImpl = this;
            } else {
                key3 = key2;
                resourcesImpl = this;
                cs = (ConstantState) sPreloadedDrawables[resourcesImpl.mConfiguration.getLayoutDirection()].get(key3);
            }
            ConstantState cs2 = cs;
            if (resources != null) {
                resources.setResourceScaleOpt(true);
            }
            z = false;
            if (cs2 != null) {
                if (TRACE_FOR_DETAILED_PRELOAD && (i2 >>> 24) == 1 && Process.myUid() != 0) {
                    String name2 = resourcesImpl.getResourceName(i2);
                    if (name2 != null) {
                        String str = TAG_PRELOAD;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Hit preloaded FW drawable #");
                        stringBuilder2.append(Integer.toHexString(id));
                        stringBuilder2.append(" ");
                        stringBuilder2.append(name2);
                        Log.d(str, stringBuilder2.toString());
                    }
                }
                dr = cs2.newDrawable(resources);
            } else if (isColorDrawable) {
                dr = new ColorDrawable(typedValue.data);
            } else {
                dr = loadDrawableForCookie(wrapper, value, id, density);
                if (resources == null) {
                    z2 = false;
                    try {
                        resources.setResourceScaleOpt(false);
                    } catch (Exception e5) {
                        e = e5;
                        i = 0;
                        e2 = e;
                        name = resourcesImpl.getResourceName(i2);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Drawable ");
                        stringBuilder.append(name);
                        stringBuilder.append(" with resource ID #0x");
                        stringBuilder.append(Integer.toHexString(id));
                        notFoundException = new NotFoundException(stringBuilder.toString(), e2);
                        notFoundException.setStackTrace(new StackTraceElement[i]);
                        throw notFoundException;
                    }
                }
                z2 = false;
                if (dr instanceof DrawableContainer) {
                    z = true;
                }
                needsNewDrawableAfterCache = z;
                if (dr != null || !dr.canApplyTheme()) {
                    canApplyTheme = z2;
                }
                if (canApplyTheme && theme2 != null) {
                    dr = dr.mutate();
                    dr.applyTheme(theme2);
                    dr.clearMutated();
                }
                dr2 = resourcesImpl.mHwResourcesImpl.handleAddIconBackground(resources, i2, dr);
                if (dr2 != null) {
                    dr2.setChangingConfigurations(typedValue.changingConfigurations);
                    if (useCache) {
                        TypedValue typedValue2 = typedValue;
                        Drawable dr3 = dr2;
                        i = z2;
                        try {
                            resourcesImpl.cacheDrawable(typedValue2, isColorDrawable, caches2, theme2, canApplyTheme, key3, dr3);
                            if (needsNewDrawableAfterCache) {
                                cachedDrawable = dr3;
                                ConstantState state = cachedDrawable.getConstantState();
                                if (state != null) {
                                    cachedDrawable = state.newDrawable(resources);
                                }
                            } else {
                                cachedDrawable = dr3;
                            }
                            return cachedDrawable;
                        } catch (Exception e6) {
                            e = e6;
                            e2 = e;
                            name = resourcesImpl.getResourceName(i2);
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Drawable ");
                            stringBuilder.append(name);
                            stringBuilder.append(" with resource ID #0x");
                            stringBuilder.append(Integer.toHexString(id));
                            notFoundException = new NotFoundException(stringBuilder.toString(), e2);
                            notFoundException.setStackTrace(new StackTraceElement[i]);
                            throw notFoundException;
                        }
                    }
                }
                cachedDrawable = dr2;
                constantState = cs2;
                drawableCache = caches2;
                j = key3;
                return cachedDrawable;
            }
            if (resources == null) {
            }
            if (dr instanceof DrawableContainer) {
            }
            needsNewDrawableAfterCache = z;
            if (dr != null) {
            }
            canApplyTheme = z2;
            dr = dr.mutate();
            dr.applyTheme(theme2);
            dr.clearMutated();
            dr2 = resourcesImpl.mHwResourcesImpl.handleAddIconBackground(resources, i2, dr);
            if (dr2 != null) {
            }
            cachedDrawable = dr2;
            constantState = cs2;
            drawableCache = caches2;
            j = key3;
            return cachedDrawable;
        } catch (Exception e7) {
            e = e7;
            i = 0;
            e2 = e;
            name = resourcesImpl.getResourceName(i2);
            stringBuilder = new StringBuilder();
            stringBuilder.append("Drawable ");
            stringBuilder.append(name);
            stringBuilder.append(" with resource ID #0x");
            stringBuilder.append(Integer.toHexString(id));
            notFoundException = new NotFoundException(stringBuilder.toString(), e2);
            notFoundException.setStackTrace(new StackTraceElement[i]);
            throw notFoundException;
        }
    }

    private void cacheDrawable(TypedValue value, boolean isColorDrawable, DrawableCache caches, Theme theme, boolean usesTheme, long key, Drawable dr) {
        TypedValue typedValue = value;
        long j = key;
        ConstantState cs = dr.getConstantState();
        if (cs != null) {
            if (this.mPreloading) {
                int changingConfigs = cs.getChangingConfigurations();
                if (isColorDrawable) {
                    if (verifyPreloadConfig(changingConfigs, 0, typedValue.resourceId, "drawable")) {
                        sPreloadedColorDrawables.put(j, cs);
                    }
                } else if (verifyPreloadConfig(changingConfigs, 8192, typedValue.resourceId, "drawable")) {
                    if ((changingConfigs & 8192) == 0) {
                        sPreloadedDrawables[0].put(j, cs);
                        sPreloadedDrawables[1].put(j, cs);
                    } else {
                        sPreloadedDrawables[this.mConfiguration.getLayoutDirection()].put(j, cs);
                    }
                }
            } else {
                synchronized (this.mAccessLock) {
                    caches.put(j, theme, cs, usesTheme);
                }
            }
        }
    }

    private boolean verifyPreloadConfig(int changingConfigurations, int allowVarying, int resourceId, String name) {
        if ((((~(((((CONFIG_FONT_SCALE | CONFIG_DENSITY) | 1073741824) | CONFIG_SMALLEST_SCREEN_SIZE) | 1024) | CONFIG_ORIENTATION)) & changingConfigurations) & (~allowVarying)) == 0) {
            return true;
        }
        String resName;
        try {
            resName = getResourceName(resourceId);
        } catch (NotFoundException e) {
            resName = "?";
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Preloaded ");
        stringBuilder.append(name);
        stringBuilder.append(" resource #0x");
        stringBuilder.append(Integer.toHexString(resourceId));
        stringBuilder.append(" (");
        stringBuilder.append(resName);
        stringBuilder.append(") that varies with configuration!!");
        Log.w(str, stringBuilder.toString());
        return false;
    }

    private Drawable decodeImageDrawable(AssetInputStream ais, Resources wrapper, TypedValue value) {
        try {
            return ImageDecoder.decodeDrawable(new AssetInputStreamSource(ais, wrapper, value), -$$Lambda$ResourcesImpl$99dm2ENnzo9b0SIUjUj2Kl3pi90.INSTANCE);
        } catch (IOException e) {
            return null;
        }
    }

    private Drawable loadDrawableForCookie(Resources wrapper, TypedValue value, int id, int density) {
        Throwable th;
        LookupStack stack;
        StringBuilder stringBuilder;
        NotFoundException rnf;
        Resources resources = wrapper;
        TypedValue typedValue = value;
        int i = id;
        if (typedValue.string != null) {
            Drawable drCache;
            if (HwFrameworkFactory.getHwActivityThread() != null) {
                drCache = HwFrameworkFactory.getHwActivityThread().getCacheDrawableFromAware(i, resources, typedValue.assetCookie, this.mAssets);
                if (drCache != null) {
                    drCache.setChangingConfigurations(typedValue.changingConfigurations);
                    return drCache;
                }
            }
            String file = typedValue.string.toString();
            long startTime = System.nanoTime();
            int startBitmapCount = 0;
            long startBitmapSize = 0;
            int startDrawableCount = 0;
            if (TRACE_FOR_DETAILED_PRELOAD) {
                startBitmapCount = Bitmap.sPreloadTracingNumInstantiatedBitmaps;
                startBitmapSize = Bitmap.sPreloadTracingTotalBitmapsSize;
                startDrawableCount = sPreloadTracingNumLoadedDrawables;
            }
            int startBitmapCount2 = startBitmapCount;
            long startBitmapSize2 = startBitmapSize;
            int startDrawableCount2 = startDrawableCount;
            Trace.traceBegin(8192, file);
            LookupStack stack2 = (LookupStack) this.mLookupStack.get();
            long j;
            try {
                if (stack2.contains(i)) {
                    j = startTime;
                    throw new Exception("Recursive reference in drawable");
                }
                stack2.push(i);
                try {
                    Drawable dr;
                    int i2;
                    if (file.endsWith(".xml")) {
                        try {
                            XmlResourceParser rp = loadXmlResourceParser(file, i, typedValue.assetCookie, "drawable");
                            try {
                                Drawable dr2 = Drawable.createFromXmlForDensity(resources, rp, density, null);
                                rp.close();
                                dr = dr2;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            i2 = density;
                            stack = stack2;
                            try {
                                stack.pop();
                                throw th;
                            } catch (Exception | StackOverflowError e) {
                                th = e;
                                Trace.traceEnd(8192);
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("File ");
                                stringBuilder.append(file);
                                stringBuilder.append(" from drawable resource ID #0x");
                                stringBuilder.append(Integer.toHexString(id));
                                rnf = new NotFoundException(stringBuilder.toString());
                                rnf.initCause(th);
                                throw rnf;
                            }
                        }
                    }
                    i2 = density;
                    InputStream is = this.mAssets.openNonAsset(typedValue.assetCookie, file, 2);
                    dr = HwPCUtils.isValidExtDisplayId(HwPCUtils.getPCDisplayID()) ? Drawable.createFromResourceStream(resources, typedValue, is, file, new Options()) : decodeImageDrawable((AssetInputStream) is, resources, typedValue);
                    drCache = dr;
                    stack2.pop();
                    Trace.traceEnd(8192);
                    long time = System.nanoTime() - startTime;
                    if (HwFrameworkFactory.getHwActivityThread() != null) {
                        stack = stack2;
                        HwFrameworkFactory.getHwActivityThread().postCacheDrawableToAware(i, resources, time, typedValue.assetCookie, this.mAssets);
                    }
                    if (TRACE_FOR_DETAILED_PRELOAD) {
                        boolean isRoot = true;
                        if ((i >>> 24) == 1) {
                            String name = getResourceName(i);
                            if (name != null) {
                                String str;
                                startDrawableCount = Bitmap.sPreloadTracingNumInstantiatedBitmaps - startBitmapCount2;
                                long loadedBitmapSize = Bitmap.sPreloadTracingTotalBitmapsSize - startBitmapSize2;
                                i2 = sPreloadTracingNumLoadedDrawables - startDrawableCount2;
                                sPreloadTracingNumLoadedDrawables++;
                                if (Process.myUid() != 0) {
                                    isRoot = false;
                                }
                                String str2 = TAG_PRELOAD;
                                j = startTime;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                if (isRoot) {
                                    str = "Preloaded FW drawable #";
                                } else {
                                    str = "Loaded non-preloaded FW drawable #";
                                }
                                stringBuilder2.append(str);
                                stringBuilder2.append(Integer.toHexString(id));
                                stringBuilder2.append(" ");
                                stringBuilder2.append(name);
                                stringBuilder2.append(" ");
                                stringBuilder2.append(file);
                                stringBuilder2.append(" ");
                                stringBuilder2.append(drCache.getClass().getCanonicalName());
                                stringBuilder2.append(" #nested_drawables= ");
                                stringBuilder2.append(i2);
                                stringBuilder2.append(" #bitmaps= ");
                                stringBuilder2.append(startDrawableCount);
                                stringBuilder2.append(" total_bitmap_size= ");
                                stringBuilder2.append(loadedBitmapSize);
                                stringBuilder2.append(" in[us] ");
                                stringBuilder2.append(time / 1000);
                                Log.d(str2, stringBuilder2.toString());
                                return drCache;
                            }
                        }
                    }
                    return drCache;
                } catch (Throwable th4) {
                    th = th4;
                    stack = stack2;
                    j = startTime;
                    stack.pop();
                    throw th;
                }
            } catch (Exception | StackOverflowError e2) {
                th = e2;
                stack = stack2;
                j = startTime;
                Trace.traceEnd(8192);
                stringBuilder = new StringBuilder();
                stringBuilder.append("File ");
                stringBuilder.append(file);
                stringBuilder.append(" from drawable resource ID #0x");
                stringBuilder.append(Integer.toHexString(id));
                rnf = new NotFoundException(stringBuilder.toString());
                rnf.initCause(th);
                throw rnf;
            }
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Resource \"");
        stringBuilder3.append(getResourceName(i));
        stringBuilder3.append("\" (");
        stringBuilder3.append(Integer.toHexString(id));
        stringBuilder3.append(") is not a Drawable (color or path): ");
        stringBuilder3.append(typedValue);
        throw new NotFoundException(stringBuilder3.toString());
    }

    public Typeface loadFont(Resources wrapper, TypedValue value, int id) {
        String str;
        StringBuilder stringBuilder;
        if (value.string != null) {
            String file = value.string.toString();
            if (!file.startsWith("res/")) {
                return null;
            }
            Typeface cached = Typeface.findFromCache(this.mAssets, file);
            if (cached != null) {
                return cached;
            }
            Trace.traceBegin(8192, file);
            try {
                if (file.endsWith("xml")) {
                    FamilyResourceEntry familyEntry = FontResourcesParser.parse(loadXmlResourceParser(file, id, value.assetCookie, "font"), wrapper);
                    if (familyEntry == null) {
                        Trace.traceEnd(8192);
                        return null;
                    }
                    Typeface createFromResources = Typeface.createFromResources(familyEntry, this.mAssets, file);
                    Trace.traceEnd(8192);
                    return createFromResources;
                }
                Typeface createFromResources2 = Typeface.createFromResources(this.mAssets, file, value.assetCookie);
                Trace.traceEnd(8192);
                return createFromResources2;
            } catch (XmlPullParserException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to parse xml resource ");
                stringBuilder.append(file);
                Log.e(str, stringBuilder.toString(), e);
            } catch (IOException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to read xml resource ");
                stringBuilder.append(file);
                Log.e(str, stringBuilder.toString(), e2);
            } catch (Throwable th) {
                Trace.traceEnd(8192);
            }
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Resource \"");
            stringBuilder2.append(getResourceName(id));
            stringBuilder2.append("\" (");
            stringBuilder2.append(Integer.toHexString(id));
            stringBuilder2.append(") is not a Font: ");
            stringBuilder2.append(value);
            throw new NotFoundException(stringBuilder2.toString());
        }
        Trace.traceEnd(8192);
        return null;
    }

    private ComplexColor loadComplexColorFromName(Resources wrapper, Theme theme, TypedValue value, int id) {
        long key = (((long) value.assetCookie) << 32) | ((long) value.data);
        ConfigurationBoundResourceCache<ComplexColor> cache = this.mComplexColorCache;
        ComplexColor complexColor = (ComplexColor) cache.getInstance(key, wrapper, theme);
        if (complexColor != null) {
            return complexColor;
        }
        ConstantState<ComplexColor> factory = (ConstantState) sPreloadedComplexColors.get(key);
        if (factory != null) {
            complexColor = (ComplexColor) factory.newInstance(wrapper, theme);
        }
        if (complexColor == null) {
            complexColor = loadComplexColorForCookie(wrapper, value, id, theme);
        }
        if (complexColor != null) {
            complexColor.setBaseChangingConfigurations(value.changingConfigurations);
            if (!this.mPreloading) {
                cache.put(key, theme, complexColor.getConstantState());
            } else if (verifyPreloadConfig(complexColor.getChangingConfigurations(), 0, value.resourceId, Slice.SUBTYPE_COLOR)) {
                sPreloadedComplexColors.put(key, complexColor.getConstantState());
            }
        }
        return complexColor;
    }

    ComplexColor loadComplexColor(Resources wrapper, TypedValue value, int id, Theme theme) {
        long key = (((long) value.assetCookie) << 32) | ((long) value.data);
        if (value.type >= 28 && value.type <= 31) {
            return getColorStateListFromInt(value, key);
        }
        String file = value.string.toString();
        if (file.endsWith(".xml")) {
            try {
                return loadComplexColorFromName(wrapper, theme, value, id);
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("File ");
                stringBuilder.append(file);
                stringBuilder.append(" from complex color resource ID #0x");
                stringBuilder.append(Integer.toHexString(id));
                NotFoundException rnf = new NotFoundException(stringBuilder.toString());
                rnf.initCause(e);
                throw rnf;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("File ");
        stringBuilder2.append(file);
        stringBuilder2.append(" from drawable resource ID #0x");
        stringBuilder2.append(Integer.toHexString(id));
        stringBuilder2.append(": .xml extension required");
        throw new NotFoundException(stringBuilder2.toString());
    }

    protected ColorStateList loadColorStateList(Resources wrapper, TypedValue value, int id, Theme theme) throws NotFoundException {
        long key = (((long) value.assetCookie) << 32) | ((long) value.data);
        if (value.type >= 28 && value.type <= 31) {
            return getColorStateListFromInt(value, key);
        }
        ComplexColor complexColor = loadComplexColorFromName(wrapper, theme, value, id);
        if (complexColor != null && (complexColor instanceof ColorStateList)) {
            return (ColorStateList) complexColor;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can't find ColorStateList from drawable resource ID #0x");
        stringBuilder.append(Integer.toHexString(id));
        throw new NotFoundException(stringBuilder.toString());
    }

    private ColorStateList getColorStateListFromInt(TypedValue value, long key) {
        ConstantState<ComplexColor> factory = (ConstantState) sPreloadedComplexColors.get(key);
        if (factory != null) {
            return (ColorStateList) factory.newInstance();
        }
        ColorStateList csl = ColorStateList.valueOf(value.data);
        if (this.mPreloading && verifyPreloadConfig(value.changingConfigurations, 0, value.resourceId, Slice.SUBTYPE_COLOR)) {
            sPreloadedComplexColors.put(key, csl.getConstantState());
        }
        return csl;
    }

    /* JADX WARNING: Removed duplicated region for block: B:21:0x005a A:{SYNTHETIC, Splitter:B:21:0x005a} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0032 A:{Catch:{ Exception -> 0x0062 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ComplexColor loadComplexColorForCookie(Resources wrapper, TypedValue value, int id, Theme theme) {
        StringBuilder stringBuilder;
        if (value.string != null) {
            String file = value.string.toString();
            ComplexColor complexColor = null;
            Trace.traceBegin(8192, file);
            if (file.endsWith(".xml")) {
                try {
                    int type;
                    XmlResourceParser parser = loadXmlResourceParser(file, id, value.assetCookie, "ComplexColor");
                    AttributeSet attrs = Xml.asAttributeSet(parser);
                    while (true) {
                        int next = parser.next();
                        type = next;
                        if (next == 2 || type == 1) {
                            if (type != 2) {
                                String name = parser.getName();
                                if (name.equals("gradient")) {
                                    complexColor = GradientColor.createFromXmlInner(wrapper, parser, attrs, theme);
                                } else if (name.equals("selector")) {
                                    complexColor = ColorStateList.createFromXmlInner(wrapper, parser, attrs, theme);
                                }
                                parser.close();
                                Trace.traceEnd(8192);
                                return complexColor;
                            }
                            throw new XmlPullParserException("No start tag found");
                        }
                    }
                    if (type != 2) {
                    }
                } catch (Exception e) {
                    Trace.traceEnd(8192);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("File ");
                    stringBuilder.append(file);
                    stringBuilder.append(" from ComplexColor resource ID #0x");
                    stringBuilder.append(Integer.toHexString(id));
                    NotFoundException rnf = new NotFoundException(stringBuilder.toString());
                    rnf.initCause(e);
                    throw rnf;
                }
            }
            Trace.traceEnd(8192);
            stringBuilder = new StringBuilder();
            stringBuilder.append("File ");
            stringBuilder.append(file);
            stringBuilder.append(" from drawable resource ID #0x");
            stringBuilder.append(Integer.toHexString(id));
            stringBuilder.append(": .xml extension required");
            throw new NotFoundException(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Can't convert to ComplexColor: type=0x");
        stringBuilder2.append(value.type);
        throw new UnsupportedOperationException(stringBuilder2.toString());
    }

    XmlResourceParser loadXmlResourceParser(String file, int id, int assetCookie, String type) throws NotFoundException {
        if (id != 0) {
            try {
                synchronized (this.mCachedXmlBlocks) {
                    int[] cachedXmlBlockCookies = this.mCachedXmlBlockCookies;
                    String[] cachedXmlBlockFiles = this.mCachedXmlBlockFiles;
                    XmlBlock[] cachedXmlBlocks = this.mCachedXmlBlocks;
                    int num = cachedXmlBlockFiles.length;
                    int i = 0;
                    while (i < num) {
                        if (cachedXmlBlockCookies[i] == assetCookie && cachedXmlBlockFiles[i] != null && cachedXmlBlockFiles[i].equals(file)) {
                            XmlResourceParser newParser = cachedXmlBlocks[i].newParser();
                            return newParser;
                        }
                        i++;
                    }
                    XmlBlock block = this.mAssets.openXmlBlockAsset(assetCookie, file);
                    if (block != null) {
                        int pos = (this.mLastCachedXmlBlockIndex + 1) % num;
                        this.mLastCachedXmlBlockIndex = pos;
                        XmlBlock oldBlock = cachedXmlBlocks[pos];
                        if (oldBlock != null) {
                            oldBlock.close();
                        }
                        cachedXmlBlockCookies[pos] = assetCookie;
                        cachedXmlBlockFiles[pos] = file;
                        cachedXmlBlocks[pos] = block;
                        XmlResourceParser newParser2 = block.newParser();
                        return newParser2;
                    }
                }
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("File ");
                stringBuilder.append(file);
                stringBuilder.append(" from xml type ");
                stringBuilder.append(type);
                stringBuilder.append(" resource ID #0x");
                stringBuilder.append(Integer.toHexString(id));
                NotFoundException rnf = new NotFoundException(stringBuilder.toString());
                rnf.initCause(e);
                throw rnf;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("File ");
        stringBuilder2.append(file);
        stringBuilder2.append(" from xml type ");
        stringBuilder2.append(type);
        stringBuilder2.append(" resource ID #0x");
        stringBuilder2.append(Integer.toHexString(id));
        throw new NotFoundException(stringBuilder2.toString());
    }

    public final void startPreloading() {
        synchronized (sSync) {
            if (sPreloaded) {
                throw new IllegalStateException("Resources already preloaded");
            }
            sPreloaded = true;
            this.mPreloading = true;
            sPreloadedDensity = DisplayMetrics.DENSITY_DEVICE;
            this.mConfiguration.densityDpi = sPreloadedDensity;
            updateConfiguration(null, null, null);
            if (TRACE_FOR_DETAILED_PRELOAD) {
                this.mPreloadTracingPreloadStartTime = SystemClock.uptimeMillis();
                this.mPreloadTracingStartBitmapSize = Bitmap.sPreloadTracingTotalBitmapsSize;
                this.mPreloadTracingStartBitmapCount = (long) Bitmap.sPreloadTracingNumInstantiatedBitmaps;
                Log.d(TAG_PRELOAD, "Preload starting");
            }
        }
    }

    void finishPreloading() {
        if (this.mPreloading) {
            if (TRACE_FOR_DETAILED_PRELOAD) {
                long time = SystemClock.uptimeMillis() - this.mPreloadTracingPreloadStartTime;
                long size = Bitmap.sPreloadTracingTotalBitmapsSize - this.mPreloadTracingStartBitmapSize;
                long count = ((long) Bitmap.sPreloadTracingNumInstantiatedBitmaps) - this.mPreloadTracingStartBitmapCount;
                String str = TAG_PRELOAD;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Preload finished, ");
                stringBuilder.append(count);
                stringBuilder.append(" bitmaps of ");
                stringBuilder.append(size);
                stringBuilder.append(" bytes in ");
                stringBuilder.append(time);
                stringBuilder.append(" ms");
                Log.d(str, stringBuilder.toString());
            }
            this.mPreloading = false;
            flushLayoutCache();
        }
    }

    LongSparseArray<ConstantState> getPreloadedDrawables() {
        return sPreloadedDrawables[0];
    }

    ThemeImpl newThemeImpl() {
        return new ThemeImpl();
    }

    ThemeImpl newThemeImpl(ThemeKey key) {
        ThemeImpl impl = new ThemeImpl();
        impl.mKey.setTo(key);
        impl.rebase();
        return impl;
    }
}
