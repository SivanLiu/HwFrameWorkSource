package android.content.res;

import android.annotation.SuppressLint;
import android.content.res.AbsResourcesImpl.ThemeColor;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.DisplayAdjustments;
import com.huawei.android.text.HwTextUtils;
import huawei.android.hwutil.AssetsFileCache;
import huawei.android.hwutil.ZipFileCache;
import java.io.File;
import java.util.Locale;

public class HwResources extends Resources {
    private static final boolean DEBUG_DRAWABLE = false;
    private static final String DEFAULT_RES_XX_DIR = "res/drawable-xxhdpi";
    private static final String DRAWABLE_FHD = "drawable-xxhdpi";
    private static final String FRAMEWORK_RES = "framework-res";
    private static final String FRAMEWORK_RES_EXT = "framework-res-hwext";
    static final String TAG = "HwResources";
    private static boolean sSerbiaLocale = false;
    protected String mPackageName = null;
    private boolean system = false;

    public ResourcesImpl getImpl() {
        return super.getImpl();
    }

    public void setPackageName(String name) {
        this.mPackageName = name;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public Bitmap getThemeBitmap(TypedValue value, int id, Rect padding) throws NotFoundException {
        TypedValue typedValue = value;
        Rect rect = padding;
        if (getImpl().mPreloading) {
            return null;
        }
        String packageName = getImpl().getHwResourcesImpl().getThemeResource(id, null).packageName;
        if (typedValue.string == null) {
            return null;
        }
        String file = typedValue.string.toString().replaceFirst("-v\\d+/", "/");
        if (file.isEmpty()) {
            return null;
        }
        Bitmap bmp = null;
        if (packageName.indexOf(FRAMEWORK_RES) >= 0 && file.indexOf("_holo") >= 0) {
            return null;
        }
        int i;
        String deepThemeType;
        String key;
        int index;
        int themeDensity;
        String dir;
        boolean isLand = file.indexOf("-land") >= 0;
        boolean isFramework = packageName.equals(FRAMEWORK_RES);
        boolean isHwFramework = packageName.equals(FRAMEWORK_RES_EXT);
        String deepThemeType2 = getImpl().getHwResourcesImpl().getDeepThemeType();
        if (!isFramework && !isHwFramework) {
            i = -1;
            deepThemeType = deepThemeType2;
        } else if (this.mPackageName == null || this.mPackageName.isEmpty()) {
            i = -1;
            deepThemeType = deepThemeType2;
        } else {
            ZipFileCache frameworkZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getImpl().getHwResourcesImpl().getThemeDir(), this.mPackageName);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(packageName);
            stringBuilder.append("/");
            stringBuilder.append(file);
            key = stringBuilder.toString();
            if (frameworkZipFileCache != null) {
                bmp = frameworkZipFileCache.getBitmapEntry(this, typedValue, key, rect);
                if (bmp == null && !file.contains(DRAWABLE_FHD)) {
                    frameworkZipFileCache.initResDirInfo();
                    index = isFramework ? isLand ? 3 : 2 : isLand ? 5 : 4;
                    themeDensity = frameworkZipFileCache.getDrawableDensity(index);
                    dir = frameworkZipFileCache.getDrawableDir(index);
                    if (!(themeDensity == -1 || dir == null)) {
                        String newFile = new StringBuilder();
                        newFile.append(dir);
                        newFile.append(File.separator);
                        newFile.append(file.substring(file.lastIndexOf(File.separator) + 1));
                        bmp = frameworkZipFileCache.getBitmapEntry(this, typedValue, newFile.toString(), rect);
                    }
                }
                if (bmp != null) {
                    return bmp;
                }
            }
            Bitmap bmp2 = bmp;
            if (!TextUtils.isEmpty(deepThemeType2) && bmp2 == null && frameworkZipFileCache == null) {
                i = -1;
                deepThemeType = deepThemeType2;
                bmp = getFwkBitmapFromAsset(typedValue, this, packageName, file, key, deepThemeType2, rect);
            } else {
                ZipFileCache zipFileCache = frameworkZipFileCache;
                deepThemeType = deepThemeType2;
                i = -1;
                bmp = bmp2;
            }
            if (bmp != null) {
                return bmp;
            }
        }
        ZipFileCache packageZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getImpl().getHwResourcesImpl().getThemeDir(), packageName);
        if (packageZipFileCache != null) {
            bmp = packageZipFileCache.getBitmapEntry(this, typedValue, file, rect);
        }
        if (!(bmp != null || packageZipFileCache == null || file.contains(DRAWABLE_FHD))) {
            packageZipFileCache.initResDirInfo();
            index = file.indexOf("-land") >= 0 ? 1 : 0;
            themeDensity = packageZipFileCache.getDrawableDensity(index);
            dir = packageZipFileCache.getDrawableDir(index);
            if (!(themeDensity == i || dir == null)) {
                key = new StringBuilder();
                key.append(dir);
                key.append(File.separator);
                key.append(file.substring(file.lastIndexOf(File.separator) + 1));
                bmp = packageZipFileCache.getBitmapEntry(this, typedValue, key.toString(), rect);
            }
        }
        Bitmap bmp3 = bmp;
        String deepThemeType3 = deepThemeType;
        if (!(TextUtils.isEmpty(deepThemeType3) || bmp3 != null || packageZipFileCache != null || isFramework || isHwFramework)) {
            bmp3 = getAppBitmapFromAsset(typedValue, this, file, deepThemeType3, rect);
        }
        return bmp3;
    }

    @SuppressLint({"AvoidInHardConnectInString"})
    private Bitmap getFwkBitmapFromAsset(TypedValue value, Resources res, String packageName, String file, String key, String dir, Rect padding) {
        Bitmap btm = getAssets();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dir);
        stringBuilder.append(File.separator);
        stringBuilder.append(key);
        btm = AssetsFileCache.getBitmapEntry(btm, res, value, stringBuilder.toString(), padding);
        if (btm != null || file.contains(DRAWABLE_FHD)) {
            return btm;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(dir);
        stringBuilder.append(File.separator);
        stringBuilder.append(packageName);
        stringBuilder.append(File.separator);
        stringBuilder.append(DEFAULT_RES_XX_DIR);
        stringBuilder.append(File.separator);
        stringBuilder.append(file.substring(file.lastIndexOf(File.separator) + 1));
        return AssetsFileCache.getBitmapEntry(getAssets(), res, value, stringBuilder.toString(), padding);
    }

    @SuppressLint({"AvoidInHardConnectInString"})
    private Bitmap getAppBitmapFromAsset(TypedValue value, Resources res, String file, String dir, Rect rect) {
        Bitmap btmp = getAssets();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dir);
        stringBuilder.append(File.separator);
        stringBuilder.append(file);
        btmp = AssetsFileCache.getBitmapEntry(btmp, res, value, stringBuilder.toString(), null);
        if (btmp != null || file.contains(DRAWABLE_FHD)) {
            return btmp;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(dir);
        stringBuilder.append(File.separator);
        stringBuilder.append(DEFAULT_RES_XX_DIR);
        stringBuilder.append(File.separator);
        stringBuilder.append(file.substring(file.lastIndexOf(File.separator) + 1));
        return AssetsFileCache.getBitmapEntry(getAssets(), res, value, stringBuilder.toString(), rect);
    }

    public Bitmap getThemeBitmap(TypedValue value, int id) throws NotFoundException {
        return getThemeBitmap(value, id, null);
    }

    protected ColorStateList loadColorStateList(TypedValue value, int id, Theme theme) throws NotFoundException {
        boolean isThemeColor = false;
        ColorStateList csl = null;
        if (id != 0) {
            ThemeColor colorValue = getThemeColor(value, id);
            if (colorValue != null) {
                isThemeColor = colorValue.mIsThemed;
                if (isThemeColor) {
                    csl = ColorStateList.valueOf(colorValue.mColor);
                }
            }
        }
        if (isThemeColor) {
            return csl;
        }
        return super.loadColorStateList(value, id, theme);
    }

    public int getColor(int id, Theme theme) throws NotFoundException {
        TypedValue value = hwObtainTempTypedValue();
        try {
            getValue(id, value, true);
            int colorVaue = 16;
            if (value.type >= 16) {
                colorVaue = 31;
                if (value.type <= 31) {
                    ThemeColor themecolor = getThemeColor(value, id);
                    if (themecolor != null) {
                        colorVaue = themecolor.mColor;
                        return colorVaue;
                    }
                    colorVaue = value.data;
                    hwReleaseTempTypedValue(value);
                    return colorVaue;
                }
            }
            int color = super.getColor(id, theme);
            hwReleaseTempTypedValue(value);
            return color;
        } finally {
            hwReleaseTempTypedValue(value);
        }
    }

    public ThemeColor getThemeColor(TypedValue value, int id) {
        ResourcesImpl impl = getImpl();
        if (impl == null) {
            return null;
        }
        impl.getHwResourcesImpl().setPackageName(getPackageName());
        return impl.getHwResourcesImpl().getThemeColor(value, id);
    }

    public HwResources(AssetManager assets, DisplayMetrics metrics, Configuration config) {
        super(assets, metrics, config);
        setIsSRLocale("sr".equals(Locale.getDefault().getLanguage()));
    }

    public HwResources(ClassLoader classLoader) {
        super(classLoader);
    }

    public HwResources(boolean system) {
        this.system = system;
        getImpl().getHwResourcesImpl().initResource();
    }

    public HwResources(AssetManager assets, DisplayMetrics metrics, Configuration config, DisplayAdjustments displayAdjustments, IBinder token) {
        super(assets, metrics, config, displayAdjustments);
        setIsSRLocale("sr".equals(Locale.getDefault().getLanguage()));
    }

    public HwResources() {
        getImpl().getHwResourcesImpl().initResource();
    }

    public Drawable getDrawableForDynamic(String packageName, String iconName) throws NotFoundException {
        return getImpl().getHwResourcesImpl().getDrawableForDynamic(this, packageName, iconName);
    }

    protected CharSequence serbianSyrillic2Latin(CharSequence res) {
        if (sSerbiaLocale) {
            return HwTextUtils.serbianSyrillic2Latin(res);
        }
        return res;
    }

    protected CharSequence[] serbianSyrillic2Latin(CharSequence[] res) {
        if (sSerbiaLocale) {
            for (int i = 0; i < res.length; i++) {
                res[i] = HwTextUtils.serbianSyrillic2Latin(res[i]);
            }
        }
        return res;
    }

    protected String serbianSyrillic2Latin(String res) {
        if (sSerbiaLocale) {
            return HwTextUtils.serbianSyrillic2Latin(res);
        }
        return res;
    }

    protected String[] serbianSyrillic2Latin(String[] res) {
        if (sSerbiaLocale) {
            for (int i = 0; i < res.length; i++) {
                res[i] = HwTextUtils.serbianSyrillic2Latin(res[i]);
            }
        }
        return res;
    }

    protected boolean isSRLocale() {
        return sSerbiaLocale;
    }

    protected static void setIsSRLocale(boolean isSerbia) {
        if (sSerbiaLocale != isSerbia) {
            sSerbiaLocale = isSerbia;
        }
    }

    public CharSequence getText(int id) throws NotFoundException {
        CharSequence res = super.getText(id);
        if (res != null) {
            return serbianSyrillic2Latin(res);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("String resource ID #0x");
        stringBuilder.append(Integer.toHexString(id));
        throw new NotFoundException(stringBuilder.toString());
    }

    public CharSequence getText(int id, CharSequence def) {
        return serbianSyrillic2Latin(super.getText(id, def));
    }

    public CharSequence[] getTextArray(int id) throws NotFoundException {
        CharSequence[] res = super.getTextArray(id);
        if (res != null) {
            return serbianSyrillic2Latin(res);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Text array resource ID #0x");
        stringBuilder.append(Integer.toHexString(id));
        throw new NotFoundException(stringBuilder.toString());
    }

    public String[] getStringArray(int id) throws NotFoundException {
        String[] res = super.getStringArray(id);
        if (res != null) {
            return serbianSyrillic2Latin(res);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("String array resource ID #0x");
        stringBuilder.append(Integer.toHexString(id));
        throw new NotFoundException(stringBuilder.toString());
    }
}
