package android.graphics;

import android.content.res.AssetManager;
import android.content.res.FontResourcesParser.FamilyResourceEntry;
import android.content.res.FontResourcesParser.FontFamilyFilesResourceEntry;
import android.content.res.FontResourcesParser.FontFileResourceEntry;
import android.content.res.FontResourcesParser.ProviderResourceEntry;
import android.graphics.fonts.FontVariationAxis;
import android.media.tv.TvContract.PreviewPrograms;
import android.net.Uri;
import android.provider.FontRequest;
import android.provider.FontsContract;
import android.provider.FontsContract.FontInfo;
import android.text.FontConfig;
import android.text.FontConfig.Alias;
import android.text.FontConfig.Family;
import android.text.FontConfig.Font;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.LruCache;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import libcore.util.NativeAllocationRegistry;
import org.xmlpull.v1.XmlPullParserException;

public class Typeface {
    public static final int BOLD = 1;
    public static final int BOLD_ITALIC = 3;
    public static final Typeface DEFAULT;
    public static final Typeface DEFAULT_BOLD;
    private static final String DEFAULT_FAMILY = "sans-serif";
    private static final int[] EMPTY_AXES = new int[0];
    public static final int HW_STYLE_ITALIC = 0;
    public static final int HW_STYLE_NORMAL = 0;
    public static final int ITALIC = 2;
    public static final int MAX_WEIGHT = 1000;
    public static final Typeface MONOSPACE = create("monospace", 0);
    public static final int NORMAL = 0;
    public static final int RESOLVE_BY_FONT_TABLE = -1;
    public static final Typeface SANS_SERIF = create(DEFAULT_FAMILY, 0);
    public static final Typeface SERIF = create("serif", 0);
    private static final int STYLE_ITALIC = 1;
    public static final int STYLE_MASK = 3;
    private static final int STYLE_NORMAL = 0;
    private static String TAG = "Typeface";
    static FontFamily[] familiestemp;
    static Typeface sDefaultTypeface;
    static Typeface[] sDefaults;
    private static final Object sDynamicCacheLock = new Object();
    @GuardedBy("sDynamicCacheLock")
    private static final LruCache<String, Typeface> sDynamicTypefaceCache = new LruCache(16);
    private static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(Typeface.class.getClassLoader(), nativeGetReleaseFunc(), 64);
    private static final Object sStyledCacheLock = new Object();
    @GuardedBy("sStyledCacheLock")
    private static final LongSparseArray<SparseArray<Typeface>> sStyledTypefaceCache = new LongSparseArray(3);
    static final Map<String, FontFamily[]> sSystemFallbackMap;
    static final Map<String, Typeface> sSystemFontMap;
    private static final Object sWeightCacheLock = new Object();
    @GuardedBy("sWeightCacheLock")
    private static final LongSparseArray<SparseArray<Typeface>> sWeightTypefaceCache = new LongSparseArray(3);
    private int mStyle = 0;
    private int[] mSupportedAxes;
    private int mWeight = 0;
    public long native_instance;

    public static final class Builder {
        public static final int BOLD_WEIGHT = 700;
        public static final int NORMAL_WEIGHT = 400;
        private AssetManager mAssetManager;
        private FontVariationAxis[] mAxes;
        private String mFallbackFamilyName;
        private FileDescriptor mFd;
        private Map<Uri, ByteBuffer> mFontBuffers;
        private FontInfo[] mFonts;
        private int mItalic = -1;
        private String mPath;
        private int mTtcIndex;
        private int mWeight = -1;

        public Builder(File path) {
            this.mPath = path.getAbsolutePath();
        }

        public Builder(FileDescriptor fd) {
            this.mFd = fd;
        }

        public Builder(String path) {
            this.mPath = path;
        }

        public Builder(AssetManager assetManager, String path) {
            this.mAssetManager = (AssetManager) Preconditions.checkNotNull(assetManager);
            this.mPath = (String) Preconditions.checkStringNotEmpty(path);
        }

        public Builder(FontInfo[] fonts, Map<Uri, ByteBuffer> buffers) {
            this.mFonts = fonts;
            this.mFontBuffers = buffers;
        }

        public Builder setWeight(int weight) {
            this.mWeight = weight;
            return this;
        }

        public Builder setItalic(boolean italic) {
            this.mItalic = italic;
            return this;
        }

        public Builder setTtcIndex(int ttcIndex) {
            if (this.mFonts == null) {
                this.mTtcIndex = ttcIndex;
                return this;
            }
            throw new IllegalArgumentException("TTC index can not be specified for FontResult source.");
        }

        public Builder setFontVariationSettings(String variationSettings) {
            if (this.mFonts != null) {
                throw new IllegalArgumentException("Font variation settings can not be specified for FontResult source.");
            } else if (this.mAxes == null) {
                this.mAxes = FontVariationAxis.fromFontVariationSettings(variationSettings);
                return this;
            } else {
                throw new IllegalStateException("Font variation settings are already set.");
            }
        }

        public Builder setFontVariationSettings(FontVariationAxis[] axes) {
            if (this.mFonts != null) {
                throw new IllegalArgumentException("Font variation settings can not be specified for FontResult source.");
            } else if (this.mAxes == null) {
                this.mAxes = axes;
                return this;
            } else {
                throw new IllegalStateException("Font variation settings are already set.");
            }
        }

        public Builder setFallback(String familyName) {
            this.mFallbackFamilyName = familyName;
            return this;
        }

        private static String createAssetUid(AssetManager mgr, String path, int ttcIndex, FontVariationAxis[] axes, int weight, int italic, String fallback) {
            int i;
            SparseArray<String> pkgs = mgr.getAssignedPackageIdentifiers();
            StringBuilder builder = new StringBuilder();
            int size = pkgs.size();
            int i2 = 0;
            for (i = 0; i < size; i++) {
                builder.append((String) pkgs.valueAt(i));
                builder.append("-");
            }
            builder.append(path);
            builder.append("-");
            builder.append(Integer.toString(ttcIndex));
            builder.append("-");
            builder.append(Integer.toString(weight));
            builder.append("-");
            builder.append(Integer.toString(italic));
            builder.append("--");
            builder.append(fallback);
            builder.append("--");
            if (axes != null) {
                i = axes.length;
                while (i2 < i) {
                    FontVariationAxis axis = axes[i2];
                    builder.append(axis.getTag());
                    builder.append("-");
                    builder.append(Float.toString(axis.getStyleValue()));
                    i2++;
                }
            }
            return builder.toString();
        }

        private Typeface resolveFallbackTypeface() {
            if (this.mFallbackFamilyName == null) {
                return null;
            }
            Typeface base = (Typeface) Typeface.sSystemFontMap.get(this.mFallbackFamilyName);
            if (base == null) {
                base = Typeface.sDefaultTypeface;
            }
            if (this.mWeight == -1 && this.mItalic == -1) {
                return base;
            }
            int weight = this.mWeight == -1 ? base.mWeight : this.mWeight;
            boolean z = false;
            if (this.mItalic != -1 ? this.mItalic != 1 : (base.mStyle & 2) == 0) {
                z = true;
            }
            return Typeface.createWeightStyle(base, weight, z);
        }

        public Typeface build() {
            Throwable th;
            FontFamily fontFamily;
            if (this.mFd != null) {
                FileInputStream fis;
                try {
                    fis = new FileInputStream(this.mFd);
                    FileChannel channel = fis.getChannel();
                    ByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
                    FontFamily fontFamily2 = new FontFamily();
                    Typeface resolveFallbackTypeface;
                    if (!fontFamily2.addFontFromBuffer(buffer, this.mTtcIndex, this.mAxes, this.mWeight, this.mItalic)) {
                        fontFamily2.abortCreation();
                        resolveFallbackTypeface = resolveFallbackTypeface();
                        fis.close();
                        return resolveFallbackTypeface;
                    } else if (fontFamily2.freeze()) {
                        Typeface access$400 = Typeface.createFromFamiliesWithDefault(new FontFamily[]{fontFamily2}, this.mFallbackFamilyName, this.mWeight, this.mItalic);
                        fis.close();
                        return access$400;
                    } else {
                        resolveFallbackTypeface = resolveFallbackTypeface();
                        fis.close();
                        return resolveFallbackTypeface;
                    }
                } catch (IOException e) {
                    return resolveFallbackTypeface();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            } else if (this.mAssetManager != null) {
                String key = createAssetUid(this.mAssetManager, this.mPath, this.mTtcIndex, this.mAxes, this.mWeight, this.mItalic, this.mFallbackFamilyName);
                synchronized (Typeface.sDynamicCacheLock) {
                    Typeface typeface = (Typeface) Typeface.sDynamicTypefaceCache.get(key);
                    if (typeface != null) {
                        return typeface;
                    }
                    FontFamily fontFamily3 = new FontFamily();
                    if (!fontFamily3.addFontFromAssetManager(this.mAssetManager, this.mPath, this.mTtcIndex, true, this.mTtcIndex, this.mWeight, this.mItalic, this.mAxes)) {
                        fontFamily3.abortCreation();
                        return resolveFallbackTypeface();
                    } else if (fontFamily3.freeze()) {
                        typeface = Typeface.createFromFamiliesWithDefault(new FontFamily[]{fontFamily3}, this.mFallbackFamilyName, this.mWeight, this.mItalic);
                        Typeface.sDynamicTypefaceCache.put(key, typeface);
                        return typeface;
                    } else {
                        return resolveFallbackTypeface();
                    }
                }
            } else if (this.mPath != null) {
                fontFamily = new FontFamily();
                if (!fontFamily.addFont(this.mPath, this.mTtcIndex, this.mAxes, this.mWeight, this.mItalic)) {
                    fontFamily.abortCreation();
                    return resolveFallbackTypeface();
                } else if (!fontFamily.freeze()) {
                    return resolveFallbackTypeface();
                } else {
                    return Typeface.createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, this.mFallbackFamilyName, this.mWeight, this.mItalic);
                }
            } else if (this.mFonts != null) {
                fontFamily = new FontFamily();
                boolean atLeastOneFont = false;
                for (FontInfo font : this.mFonts) {
                    ByteBuffer fontBuffer = (ByteBuffer) this.mFontBuffers.get(font.getUri());
                    if (fontBuffer != null) {
                        if (fontFamily.addFontFromBuffer(fontBuffer, font.getTtcIndex(), font.getAxes(), font.getWeight(), font.isItalic())) {
                            atLeastOneFont = true;
                        } else {
                            fontFamily.abortCreation();
                            return null;
                        }
                    }
                }
                if (atLeastOneFont) {
                    fontFamily.freeze();
                    return Typeface.createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, this.mFallbackFamilyName, this.mWeight, this.mItalic);
                }
                fontFamily.abortCreation();
                return null;
            } else {
                throw new IllegalArgumentException("No source was set.");
            }
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Style {
    }

    private static native long nativeCreateFromArray(long[] jArr, int i, int i2);

    private static native long nativeCreateFromTypeface(long j, int i);

    private static native long nativeCreateFromTypefaceWithExactStyle(long j, int i, boolean z);

    private static native long nativeCreateFromTypefaceWithVariation(long j, List<FontVariationAxis> list);

    private static native long nativeCreateWeightAlias(long j, int i);

    private static native long nativeGetReleaseFunc();

    private static native int nativeGetStyle(long j);

    private static native int[] nativeGetSupportedAxes(long j);

    private static native int nativeGetWeight(long j);

    private static native void nativeSetDefault(long j);

    static {
        ArrayMap<String, Typeface> systemFontMap = new ArrayMap();
        ArrayMap<String, FontFamily[]> systemFallbackMap = new ArrayMap();
        buildSystemFallback("/system/etc/fonts.xml", "/system/fonts/", systemFontMap, systemFallbackMap);
        sSystemFontMap = Collections.unmodifiableMap(systemFontMap);
        sSystemFallbackMap = Collections.unmodifiableMap(systemFallbackMap);
        setDefault((Typeface) sSystemFontMap.get(DEFAULT_FAMILY));
        String str = (String) null;
        DEFAULT = create(str, 0);
        DEFAULT_BOLD = create(str, 1);
        sDefaults = new Typeface[]{DEFAULT, DEFAULT_BOLD, create(str, 2), create(str, 3)};
    }

    private static void setDefault(Typeface t) {
        sDefaultTypeface = t;
        nativeSetDefault(t.native_instance);
    }

    public int getWeight() {
        return this.mWeight;
    }

    public int getStyle() {
        return this.mStyle;
    }

    public final boolean isBold() {
        return (this.mStyle & 1) != 0;
    }

    public final boolean isItalic() {
        return (this.mStyle & 2) != 0;
    }

    public static Typeface createFromResources(AssetManager mgr, String path, int cookie) {
        synchronized (sDynamicCacheLock) {
            String key = Builder.createAssetUid(mgr, path, 0, null, -1, -1, DEFAULT_FAMILY);
            Typeface typeface = (Typeface) sDynamicTypefaceCache.get(key);
            if (typeface != null) {
                return typeface;
            }
            FontFamily fontFamily = new FontFamily();
            if (!fontFamily.addFontFromAssetManager(mgr, path, cookie, false, 0, -1, -1, null)) {
                return null;
            } else if (fontFamily.freeze()) {
                typeface = createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, DEFAULT_FAMILY, -1, -1);
                sDynamicTypefaceCache.put(key, typeface);
                return typeface;
            } else {
                return null;
            }
        }
    }

    public static Typeface createFromResources(FamilyResourceEntry entry, AssetManager mgr, String path) {
        FamilyResourceEntry familyResourceEntry = entry;
        Typeface typeface;
        if (familyResourceEntry instanceof ProviderResourceEntry) {
            ProviderResourceEntry providerEntry = (ProviderResourceEntry) familyResourceEntry;
            List<List<String>> givenCerts = providerEntry.getCerts();
            List<List<byte[]>> certs = new ArrayList();
            if (givenCerts != null) {
                for (int i = 0; i < givenCerts.size(); i++) {
                    List<String> certSet = (List) givenCerts.get(i);
                    List<byte[]> byteArraySet = new ArrayList();
                    for (int j = 0; j < certSet.size(); j++) {
                        byteArraySet.add(Base64.decode((String) certSet.get(j), 0));
                    }
                    certs.add(byteArraySet);
                }
            }
            typeface = FontsContract.getFontSync(new FontRequest(providerEntry.getAuthority(), providerEntry.getPackage(), providerEntry.getQuery(), certs));
            return typeface == null ? DEFAULT : typeface;
        }
        Typeface typeface2 = findFromCache(mgr, path);
        if (typeface2 != null) {
            return typeface2;
        }
        FontFamilyFilesResourceEntry filesEntry = (FontFamilyFilesResourceEntry) familyResourceEntry;
        FontFamily fontFamily = new FontFamily();
        FontFileResourceEntry[] entries = filesEntry.getEntries();
        int length = entries.length;
        int i2 = 0;
        while (i2 < length) {
            FontFileResourceEntry fontFile = entries[i2];
            String fileName = fontFile.getFileName();
            int ttcIndex = fontFile.getTtcIndex();
            int weight = fontFile.getWeight();
            int italic = fontFile.getItalic();
            FontVariationAxis[] fromFontVariationSettings = FontVariationAxis.fromFontVariationSettings(fontFile.getVariationSettings());
            int i3 = italic;
            italic = i2;
            if (!fontFamily.addFontFromAssetManager(mgr, fileName, 0, false, ttcIndex, weight, i3, fromFontVariationSettings)) {
                return null;
            }
            i2 = italic + 1;
        }
        if (!fontFamily.freeze()) {
            return null;
        }
        typeface = createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, DEFAULT_FAMILY, -1, -1);
        synchronized (sDynamicCacheLock) {
            sDynamicTypefaceCache.put(Builder.createAssetUid(mgr, path, 0, null, -1, -1, DEFAULT_FAMILY), typeface);
        }
        return typeface;
    }

    public static Typeface findFromCache(AssetManager mgr, String path) {
        synchronized (sDynamicCacheLock) {
            Typeface typeface = (Typeface) sDynamicTypefaceCache.get(Builder.createAssetUid(mgr, path, 0, null, -1, -1, DEFAULT_FAMILY));
            if (typeface != null) {
                return typeface;
            }
            return null;
        }
    }

    public static Typeface create(String familyName, int style) {
        return create((Typeface) sSystemFontMap.get(familyName), style);
    }

    public static Typeface create(Typeface family, int style) {
        if ((style & -4) != 0) {
            style = 0;
        }
        if (family == null) {
            family = sDefaultTypeface;
        }
        if (family.mStyle == style) {
            return family;
        }
        long ni = family.native_instance;
        synchronized (sStyledCacheLock) {
            Typeface typeface;
            SparseArray<Typeface> styles = (SparseArray) sStyledTypefaceCache.get(ni);
            if (styles == null) {
                styles = new SparseArray(4);
                sStyledTypefaceCache.put(ni, styles);
            } else {
                typeface = (Typeface) styles.get(style);
                if (typeface != null) {
                    return typeface;
                }
            }
            typeface = new Typeface(nativeCreateFromTypeface(ni, style));
            styles.put(style, typeface);
            return typeface;
        }
    }

    public static Typeface create(Typeface family, int weight, boolean italic) {
        Preconditions.checkArgumentInRange(weight, 0, 1000, PreviewPrograms.COLUMN_WEIGHT);
        if (family == null) {
            family = sDefaultTypeface;
        }
        return createWeightStyle(family, weight, italic);
    }

    private static Typeface createWeightStyle(Typeface base, int weight, boolean italic) {
        int key = (weight << 1) | italic;
        synchronized (sWeightCacheLock) {
            Typeface typeface;
            SparseArray<Typeface> innerCache = (SparseArray) sWeightTypefaceCache.get(base.native_instance);
            if (innerCache == null) {
                innerCache = new SparseArray(4);
                sWeightTypefaceCache.put(base.native_instance, innerCache);
            } else {
                typeface = (Typeface) innerCache.get(key);
                if (typeface != null) {
                    return typeface;
                }
            }
            typeface = new Typeface(nativeCreateFromTypefaceWithExactStyle(base.native_instance, weight, italic));
            innerCache.put(key, typeface);
            return typeface;
        }
    }

    public static Typeface createFromTypefaceWithVariation(Typeface family, List<FontVariationAxis> axes) {
        return new Typeface(nativeCreateFromTypefaceWithVariation(family == null ? 0 : family.native_instance, axes));
    }

    public static Typeface defaultFromStyle(int style) {
        return sDefaults[style];
    }

    public static Typeface createFromAsset(AssetManager mgr, String path) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(mgr);
        Typeface typeface = new Builder(mgr, path).build();
        if (typeface != null) {
            return typeface;
        }
        try {
            InputStream inputStream = mgr.open(path);
            if (inputStream != null) {
                inputStream.close();
            }
            return DEFAULT;
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Font asset not found ");
            stringBuilder.append(path);
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    private static String createProviderUid(String authority, String query) {
        StringBuilder builder = new StringBuilder();
        builder.append("provider:");
        builder.append(authority);
        builder.append("-");
        builder.append(query);
        return builder.toString();
    }

    public static Typeface createFromFile(File file) {
        Typeface typeface = new Builder(file).build();
        if (typeface != null) {
            return typeface;
        }
        if (file.exists()) {
            return DEFAULT;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Font asset not found ");
        stringBuilder.append(file.getAbsolutePath());
        throw new RuntimeException(stringBuilder.toString());
    }

    public static Typeface createFromFile(String path) {
        Preconditions.checkNotNull(path);
        return createFromFile(new File(path));
    }

    private static Typeface createFromFamilies(FontFamily[] families) {
        long[] ptrArray = new long[families.length];
        for (int i = 0; i < families.length; i++) {
            ptrArray[i] = families[i].mNativePtr;
        }
        return new Typeface(nativeCreateFromArray(ptrArray, -1, -1));
    }

    private static Typeface createFromFamiliesWithDefault(FontFamily[] families, int weight, int italic) {
        return createFromFamiliesWithDefault(families, DEFAULT_FAMILY, weight, italic);
    }

    private static Typeface createFromFamiliesWithDefault(FontFamily[] families, String fallbackName, int weight, int italic) {
        FontFamily[] tmpfallback;
        FontFamily[] fallback = (FontFamily[]) sSystemFallbackMap.get(fallbackName);
        if (fallback == null) {
            fallback = (FontFamily[]) sSystemFallbackMap.get(DEFAULT_FAMILY);
        }
        boolean hashook = false;
        int i = 0;
        for (FontFamily fontFamily : fallback) {
            if (fontFamily.mIsHook) {
                hashook = true;
                break;
            }
        }
        if (hashook) {
            tmpfallback = new FontFamily[(fallback.length - 1)];
        } else {
            tmpfallback = new FontFamily[fallback.length];
        }
        int index = 0;
        for (int j = 0; j < fallback.length; j++) {
            if (!fallback[j].mIsHook) {
                tmpfallback[index] = fallback[j];
                index++;
            }
        }
        long[] ptrArray = new long[(families.length + tmpfallback.length)];
        for (int i2 = 0; i2 < families.length; i2++) {
            ptrArray[i2] = families[i2].mNativePtr;
        }
        while (i < tmpfallback.length) {
            ptrArray[families.length + i] = tmpfallback[i].mNativePtr;
            i++;
        }
        return new Typeface(nativeCreateFromArray(ptrArray, weight, italic));
    }

    private Typeface(long ni) {
        if (ni != 0) {
            this.native_instance = ni;
            sRegistry.registerNativeAllocation(this, this.native_instance);
            this.mStyle = nativeGetStyle(ni);
            this.mWeight = nativeGetWeight(ni);
            return;
        }
        throw new RuntimeException("native typeface cannot be made");
    }

    private static ByteBuffer mmap(String fullPath) {
        Throwable th;
        Throwable th2;
        try {
            FileInputStream file = new FileInputStream(fullPath);
            try {
                FileChannel fileChannel = file.getChannel();
                FileChannel fileChannel2 = fileChannel;
                MappedByteBuffer map = fileChannel2.map(MapMode.READ_ONLY, 0, fileChannel.size());
                file.close();
                return map;
            } catch (Throwable th22) {
                Throwable th3 = th22;
                th22 = th;
                th = th3;
            }
            throw th;
            if (th22 != null) {
                try {
                    file.close();
                } catch (Throwable th4) {
                    th22.addSuppressed(th4);
                }
            } else {
                file.close();
            }
            throw th;
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error mapping font file ");
            stringBuilder.append(fullPath);
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }

    private static FontFamily createFontFamily(String familyName, List<Font> fonts, String[] languageTags, int variant, Map<String, ByteBuffer> cache, String fontDir) {
        String fullPath;
        StringBuilder stringBuilder;
        Map<String, ByteBuffer> map = cache;
        FontFamily family = new FontFamily(languageTags, variant);
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= fonts.size()) {
                break;
            }
            Font font = (Font) fonts.get(i2);
            fullPath = new StringBuilder();
            fullPath.append(fontDir);
            fullPath.append(font.getFontName());
            fullPath = fullPath.toString();
            if (fullPath.contains(HwTypefaceUtil.HW_DEFAULT_FONT_NAME) && HwTypefaceUtil.HW_CUSTOM_FONT_NAME != null) {
                String tmppath = new StringBuilder();
                tmppath.append(HwTypefaceUtil.HW_CUSTOM_FONT_PATH);
                tmppath.append(HwTypefaceUtil.HW_CUSTOM_FONT_NAME);
                tmppath = tmppath.toString();
                if (new File(tmppath).exists()) {
                    fullPath = tmppath;
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("custom font, name = ");
                    stringBuilder2.append(fullPath);
                    Log.i(str, stringBuilder2.toString());
                }
            }
            String fullPath2 = fullPath;
            if (fullPath2.contains(HwTypefaceUtil.HWFONT_NAME)) {
                family.mIsHook = true;
            }
            ByteBuffer buffer = (ByteBuffer) map.get(fullPath2);
            if (buffer == null) {
                if (!map.containsKey(fullPath2)) {
                    buffer = mmap(fullPath2);
                    map.put(fullPath2, buffer);
                    if (buffer == null) {
                    }
                }
                i = i2 + 1;
            }
            ByteBuffer buffer2 = buffer;
            if (!family.addFontFromBuffer(buffer2, font.getTtcIndex(), font.getAxes(), font.getWeight(), font.isItalic())) {
                fullPath = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error creating font ");
                stringBuilder.append(fullPath2);
                stringBuilder.append("#");
                stringBuilder.append(font.getTtcIndex());
                Log.e(fullPath, stringBuilder.toString());
            }
            i = i2 + 1;
        }
        List<Font> list = fonts;
        String str2 = fontDir;
        if (family.freeze()) {
            String str3 = familyName;
            return family;
        }
        fullPath = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unable to load Family: ");
        stringBuilder.append(familyName);
        stringBuilder.append(" : ");
        stringBuilder.append(Arrays.toString(languageTags));
        Log.e(fullPath, stringBuilder.toString());
        return null;
    }

    private static void pushFamilyToFallback(Family xmlFamily, ArrayMap<String, ArrayList<FontFamily>> fallbackMap, Map<String, ByteBuffer> cache, String fontDir) {
        ArrayMap<String, ArrayList<FontFamily>> arrayMap = fallbackMap;
        String[] languageTags = xmlFamily.getLanguages();
        int variant = xmlFamily.getVariant();
        ArrayList<Font> defaultFonts = new ArrayList();
        ArrayMap<String, ArrayList<Font>> specificFallbackFonts = new ArrayMap();
        int i = 0;
        for (Font font : xmlFamily.getFonts()) {
            String fallbackName = font.getFallbackFor();
            if (fallbackName == null) {
                defaultFonts.add(font);
            } else {
                ArrayList<Font> fallback = (ArrayList) specificFallbackFonts.get(fallbackName);
                if (fallback == null) {
                    fallback = new ArrayList();
                    specificFallbackFonts.put(fallbackName, fallback);
                }
                fallback.add(font);
            }
        }
        FontFamily defaultFamily = defaultFonts.isEmpty() ? null : createFontFamily(xmlFamily.getName(), defaultFonts, languageTags, variant, cache, fontDir);
        while (i < arrayMap.size()) {
            ArrayList<Font> fallback2 = (ArrayList) specificFallbackFonts.get(arrayMap.keyAt(i));
            if (fallback2 != null) {
                FontFamily family = createFontFamily(xmlFamily.getName(), fallback2, languageTags, variant, cache, fontDir);
                if (family != null) {
                    ((ArrayList) arrayMap.valueAt(i)).add(family);
                } else if (defaultFamily != null) {
                    ((ArrayList) arrayMap.valueAt(i)).add(defaultFamily);
                }
            } else if (defaultFamily != null) {
                ((ArrayList) arrayMap.valueAt(i)).add(defaultFamily);
            }
            i++;
        }
    }

    @VisibleForTesting
    public static void buildSystemFallback(String xmlPath, String fontDir, ArrayMap<String, Typeface> fontMap, ArrayMap<String, FontFamily[]> fallbackMap) {
        RuntimeException e;
        FileNotFoundException e2;
        String str;
        StringBuilder stringBuilder;
        IOException e3;
        XmlPullParserException e4;
        String str2 = xmlPath;
        ArrayMap<String, Typeface> arrayMap = fontMap;
        ArrayMap<String, FontFamily[]> arrayMap2;
        try {
            int i;
            String str3;
            int j;
            FontConfig fontConfig = FontListParser.parse(new FileInputStream(str2));
            HashMap<String, ByteBuffer> bufferCache = new HashMap();
            Family[] xmlFamilies = fontConfig.getFamilies();
            ArrayMap<String, ArrayList<FontFamily>> fallbackListMap = new ArrayMap();
            for (Family xmlFamily : xmlFamilies) {
                String familyName = xmlFamily.getName();
                if (familyName != null) {
                    String familyName2 = familyName;
                    FontFamily family = createFontFamily(xmlFamily.getName(), Arrays.asList(xmlFamily.getFonts()), xmlFamily.getLanguages(), xmlFamily.getVariant(), bufferCache, fontDir);
                    if (family != null) {
                        ArrayList<FontFamily> fallback = new ArrayList();
                        fallback.add(family);
                        fallbackListMap.put(familyName2, fallback);
                    }
                }
            }
            for (i = 0; i < xmlFamilies.length; i++) {
                Family xmlFamily2 = xmlFamilies[i];
                if (i != 0) {
                    if (xmlFamily2.getName() != null) {
                        str3 = fontDir;
                    }
                }
                pushFamilyToFallback(xmlFamily2, fallbackListMap, bufferCache, fontDir);
            }
            str3 = fontDir;
            i = 0;
            while (i < fallbackListMap.size()) {
                String fallbackName = (String) fallbackListMap.keyAt(i);
                List<FontFamily> familyList = (List) fallbackListMap.valueAt(i);
                FontFamily[] families = (FontFamily[]) familyList.toArray(new FontFamily[familyList.size()]);
                familiestemp = (FontFamily[]) familyList.toArray(new FontFamily[familyList.size()]);
                try {
                    fallbackMap.put(fallbackName, families);
                    long[] ptrArray = new long[families.length];
                    j = 0;
                    while (j < families.length) {
                        List<FontFamily> familyList2 = familyList;
                        ptrArray[j] = families[j].mNativePtr;
                        j++;
                        familyList = familyList2;
                        str3 = fontDir;
                    }
                    arrayMap.put(fallbackName, new Typeface(nativeCreateFromArray(ptrArray, -1, -1)));
                    i++;
                    str3 = fontDir;
                } catch (RuntimeException e5) {
                    e = e5;
                    Log.w(TAG, "Didn't create default family (most likely, non-Minikin build)", e);
                } catch (FileNotFoundException e6) {
                    e2 = e6;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error opening ");
                    stringBuilder.append(str2);
                    Log.e(str, stringBuilder.toString(), e2);
                } catch (IOException e7) {
                    e3 = e7;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error reading ");
                    stringBuilder.append(str2);
                    Log.e(str, stringBuilder.toString(), e3);
                } catch (XmlPullParserException e8) {
                    e4 = e8;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("XML parse exception for ");
                    stringBuilder.append(str2);
                    Log.e(str, stringBuilder.toString(), e4);
                }
            }
            arrayMap2 = fallbackMap;
            Alias[] aliases = fontConfig.getAliases();
            int length = aliases.length;
            int i2 = 0;
            while (i2 < length) {
                FontConfig fontConfig2;
                HashMap<String, ByteBuffer> bufferCache2;
                Alias alias = aliases[i2];
                Typeface base = (Typeface) arrayMap.get(alias.getToName());
                Typeface newFace = base;
                j = alias.getWeight();
                if (j != 400) {
                    fontConfig2 = fontConfig;
                    bufferCache2 = bufferCache;
                    newFace = new Typeface(nativeCreateWeightAlias(base.native_instance, j));
                } else {
                    fontConfig2 = fontConfig;
                    bufferCache2 = bufferCache;
                }
                arrayMap.put(alias.getName(), newFace);
                i2++;
                fontConfig = fontConfig2;
                bufferCache = bufferCache2;
            }
        } catch (RuntimeException e9) {
            e = e9;
            arrayMap2 = fallbackMap;
            Log.w(TAG, "Didn't create default family (most likely, non-Minikin build)", e);
        } catch (FileNotFoundException e10) {
            e2 = e10;
            arrayMap2 = fallbackMap;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error opening ");
            stringBuilder.append(str2);
            Log.e(str, stringBuilder.toString(), e2);
        } catch (IOException e11) {
            e3 = e11;
            arrayMap2 = fallbackMap;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error reading ");
            stringBuilder.append(str2);
            Log.e(str, stringBuilder.toString(), e3);
        } catch (XmlPullParserException e12) {
            e4 = e12;
            arrayMap2 = fallbackMap;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("XML parse exception for ");
            stringBuilder.append(str2);
            Log.e(str, stringBuilder.toString(), e4);
        }
    }

    public static synchronized void loadSystemFonts() {
        synchronized (Typeface.class) {
            for (int i = 0; i < familiestemp.length; i++) {
                if (familiestemp[i].mIsHook) {
                    HwTypefaceUtil.updateFont(familiestemp[i]);
                }
            }
        }
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Typeface typeface = (Typeface) o;
        if (!(this.mStyle == typeface.mStyle && this.native_instance == typeface.native_instance)) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (31 * ((31 * 17) + ((int) (this.native_instance ^ (this.native_instance >>> 32))))) + this.mStyle;
    }

    public boolean isSupportedAxes(int axis) {
        if (this.mSupportedAxes == null) {
            synchronized (this) {
                if (this.mSupportedAxes == null) {
                    this.mSupportedAxes = nativeGetSupportedAxes(this.native_instance);
                    if (this.mSupportedAxes == null) {
                        this.mSupportedAxes = EMPTY_AXES;
                    }
                }
            }
        }
        return Arrays.binarySearch(this.mSupportedAxes, axis) >= 0;
    }
}
