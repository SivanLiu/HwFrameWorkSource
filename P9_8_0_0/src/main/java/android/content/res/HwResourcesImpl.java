package android.content.res;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.res.AbsResourcesImpl.ThemeColor;
import android.content.res.AbsResourcesImpl.ThemeResource;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.hwtheme.HwThemeManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.TypedValue;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import com.huawei.utils.reflect.EasyInvokeFactory;
import huawei.android.hwutil.AssetsFileCache;
import huawei.android.hwutil.IconBitmapUtils;
import huawei.android.hwutil.IconCache;
import huawei.android.hwutil.IconCache.CacheEntry;
import huawei.android.hwutil.ZipFileCache;
import huawei.cust.HwCfgFilePolicy;
import huawei.cust.HwCustUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwResourcesImpl extends AbsResourcesImpl {
    private static final String ANDROID_RES = "android";
    private static final String ANDROID_RES_EXT = "androidhwext";
    private static final String[] APP_WHILTLIST_ADD = new String[]{"com.android.chrome", "com.android.vending", "com.whatsapp", "com.facebook.orca"};
    private static final String CHANGEPACKAGE_CONFIG_NAME = "xml/hw_change_package.xml";
    private static final String CUSTOM_DIFF_THEME_DIR = SystemProperties.get("ro.config.diff_themes");
    private static final String DARK = "dark";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_DRAWABLE = false;
    private static final boolean DEBUG_ICON = false;
    private static final boolean DEBUG_VERBOSE_ICON = false;
    private static final String DEFAULT_CONFIG_NAME = "xml/hw_launcher_load_icon.xml";
    private static final String DEFAULT_CONFIG_PATH = "/data/cust/xml/hw_launcher_load_icon.xml";
    private static final int DEFAULT_EDGE_SIZE = 8;
    private static final String DEFAULT_MULTI_DPI_WHITELIST = "/system/etc/xml/multidpi_whitelist.xml";
    private static final String DEFAULT_NO_DRAWABLE_FLAG = "null";
    private static final String DEFAULT_RES_DIR = "res/drawable-xxhdpi";
    private static final int DELTA_X_OF_BACKGROUND = 0;
    private static final int DELTA_Y_OF_BACKGROUND = 0;
    private static final String DIFF_THEME_ICON = "/themes/diff/icons";
    private static final String DPI_2K = "xxxhdpi";
    private static final String DPI_FHD = "xxhdpi";
    private static final String DRAWABLE_FHD = "drawable-xxhdpi";
    private static final String DYNAMIC_ICONS = "dynamic_icons";
    private static final String EMUI = "EMUI";
    private static final String EMUI_TAG = (File.separator + "emui_");
    private static final String FILE_DESCRIPTION = "description.xml";
    private static final String FILE_MULTI_DPI_WHITELIST = "xml/multidpi_whitelist.xml";
    private static final String FRAMEWORK_RES = "framework-res";
    private static final String FRAMEWORK_RES_EXT = "framework-res-hwext";
    private static final String GOOGLE_KEYWORD = "com.google";
    private static final String HONOR = "honor";
    private static final String ICONS_ZIPFILE = "icons";
    private static final String ICON_BACKGROUND_PREFIX = "icon_background";
    private static final String ICON_BORDER_FILE = "icon_border.png";
    private static final String ICON_BORDER_UNFLAT_FILE = "icon_border_unflat.png";
    private static final String ICON_MASK_ALL_FILE = "icon_mask_all.png";
    private static final String ICON_MASK_FILE = "icon_mask.png";
    private static final int LEN_OF_ANDROID = 7;
    private static final int LEN_OF_ANDROID_EXT = 12;
    private static final int MASK_ABS_VALID_RANGE = 10;
    private static final String NOTIFICATION_ICON_BORDER = "ic_stat_notify_icon_border.png";
    private static final String NOTIFICATION_ICON_EXIST = "ic_stat_notify_bg_0.png";
    private static final String NOTIFICATION_ICON_MASK = "ic_stat_notify_icon_mask.png";
    private static final String NOTIFICATION_ICON_PREFIX = "ic_stat_notify_bg_";
    private static final int NO_SUPPORT_DEEP_THEME = 0;
    private static final int SUPPORT_DARK_DEEP_THEME = 1;
    private static final int SUPPORT_DARK_HONOR_DEEP_THEME = 17;
    private static final int SUPPORT_HONOR_DEEP_THEME = 16;
    private static boolean SUPPORT_LOCK_DPI = SystemProperties.getBoolean("ro.config.auto_display_mode", true);
    static final String TAG = "HwResourcesImpl";
    private static final String TAG_CONFIG = "launcher_config";
    private static final String THEMEDISIGNER = "designer";
    static final String THEME_ANIMATE_FOLDER_ICON_NAME = "portal_ring_inner_holo.png.animate";
    static final String THEME_FOLDER_ICON_NAME = "portal_ring_inner_holo.png";
    private static final String TYPT_BACKGROUND = "background";
    private static final String TYPT_ICON = "icon";
    private static final String XML_ATTRIBUTE_PACKAGE_NAME = "name";
    private static boolean bCfgFileNotExist = false;
    private static final boolean isIconSupportCut = SystemProperties.getBoolean("ro.config.hw_icon_supprot_cut", true);
    private static HashMap<String, Integer> mAssetCacheColorInfoList = new HashMap();
    private static HashMap<String, String> mCacheAssetsList = new HashMap();
    private static HashMap<String, Integer> mCacheColorInfoList = new HashMap();
    private static int mCurrentUserId = 0;
    private static HwCustHwResources mCustHwResources = null;
    private static HashSet<String> mNonThemedPackage = new HashSet();
    private static HashSet<String> mPreloadedAssetsPackage = new HashSet();
    private static HashSet<String> mPreloadedThemeColorList = new HashSet();
    private static String mThemeAbsoluteDir = (Environment.getDataDirectory() + "/themes/0");
    private static HashMap<String, String> packageNameMap = new HashMap();
    private static ResourcesUtils resUtils = ((ResourcesUtils) EasyInvokeFactory.getInvokeUtils(ResourcesUtils.class));
    private static ArrayList<Bitmap> sBgList = new ArrayList();
    private static Bitmap sBmpBorder = null;
    private static Bitmap sBmpMask = null;
    private static final int sConfigAppBigIconSize = SystemProperties.getInt("ro.config.app_big_icon_size", -1);
    private static final int sConfigAppInnerIconSize = SystemProperties.getInt("ro.config.app_inner_icon_size", -1);
    private static boolean sDefaultConfigHasRead = false;
    private static int sDefaultSizeWithoutEdge = -1;
    private static HashSet<String> sDisThemeBackground = null;
    private static HashSet<String> sDisThemeIcon = null;
    private static boolean sIsHWThemes = true;
    private static int sMaskSizeWithoutEdge = -1;
    private static boolean sMultiDirHasRead = false;
    private static HashSet<String> sMultiDirPkgNames = new HashSet();
    private static Map<String, Bundle> sMultidpiInfos = new HashMap();
    private static LongSparseArray<ConstantState> sPreloadedColorDrawablesEx = new LongSparseArray();
    private static LongSparseArray<ConstantState> sPreloadedDrawablesEx = new LongSparseArray();
    private static final HashSet<String> sPreloadedHwThemeZips = new HashSet();
    private static boolean sSerbiaLocale = false;
    private static int sStandardBgSize = -1;
    private static int sStandardIconSize = -1;
    private static boolean sThemeDescripHasRead = false;
    private static int sUseAvgColor = -1;
    private final DrawableCache mColorDrawableCacheEx = new DrawableCache();
    protected int mConfigHwt = 0;
    private boolean mContainPackage = false;
    private int mCurrentDeepTheme = 0;
    private String mDarkThemeType = "";
    private String mDeepThemeType = "";
    private final DrawableCache mDrawableCacheEx = new DrawableCache();
    private final DrawableCache mDynamicDrawableCache = new DrawableCache();
    protected String mPackageName = null;
    private ResourcesImpl mResourcesImpl;
    protected boolean mThemeChanged = false;
    private TypedValue mTmpValue = new TypedValue();
    private final Object mTmpValueLock = new Object();
    private ArrayList<String> recPackageList = new ArrayList();

    static class HwThemeFileFilter implements FileFilter {
        HwThemeFileFilter() {
        }

        public boolean accept(File pathname) {
            if (!pathname.isFile() || (pathname.getName().toLowerCase(Locale.getDefault()).endsWith(".xml") ^ 1) == 0) {
                return false;
            }
            return true;
        }
    }

    private void getAllBgImage(String path, String zip) {
        if (sBmpBorder != null) {
            if (sBgList == null || (sBgList.isEmpty() ^ 1) == 0) {
                ZipFileCache iconZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), ICONS_ZIPFILE);
                if (iconZipFileCache != null) {
                    sBgList = iconZipFileCache.getBitmapList(this.mResourcesImpl, ICON_BACKGROUND_PREFIX);
                    if (sBgList != null) {
                        int bgListSize = sBgList.size();
                        for (int i = 0; i < bgListSize; i++) {
                            Bitmap bmp = (Bitmap) sBgList.get(i);
                            if (bmp != null) {
                                sBgList.set(i, IconBitmapUtils.zoomIfNeed(bmp, sStandardBgSize, true));
                            }
                        }
                    }
                }
            }
        }
    }

    private Bitmap getRandomBgImage(String idAndPackageName) {
        if (sBgList == null || sBgList.isEmpty()) {
            getAllBgImage(getThemeDir(), ICONS_ZIPFILE);
        }
        int len = 0;
        if (sBgList != null) {
            len = sBgList.size();
        }
        if (len <= 0) {
            return null;
        }
        return (Bitmap) sBgList.get(getCode(idAndPackageName) % len);
    }

    public int getCode(String idAndPackageName) {
        int code = 0;
        for (int i = 0; i < idAndPackageName.length(); i++) {
            code += idAndPackageName.charAt(i);
        }
        return code;
    }

    private void initMaskSizeWithoutEdge(boolean useDefault) {
        if (-1 != sMaskSizeWithoutEdge) {
            return;
        }
        if (sBmpMask != null) {
            Rect info = IconBitmapUtils.getIconInfo(sBmpMask);
            if (info != null) {
                sMaskSizeWithoutEdge = info.width() + 1;
            } else {
                sMaskSizeWithoutEdge = sStandardBgSize - 8;
            }
        } else if (useDefault) {
            sMaskSizeWithoutEdge = sStandardBgSize - 8;
        }
    }

    private void initMask(ZipFileCache iconZipFileCache) {
        if (sBmpMask == null && iconZipFileCache != null) {
            sBmpMask = iconZipFileCache.getBitmapEntry(this.mResourcesImpl, ICON_MASK_FILE);
            if (sBmpMask != null) {
                sBmpMask = IconBitmapUtils.zoomIfNeed(sBmpMask, sStandardBgSize, true);
            } else {
                sBmpMask = iconZipFileCache.getBitmapEntry(this.mResourcesImpl, ICON_MASK_ALL_FILE);
                if (sBmpMask != null) {
                    sBmpMask = IconBitmapUtils.zoomIfNeed(sBmpMask, sStandardBgSize, true);
                }
            }
        }
    }

    private void initBorder(ZipFileCache iconZipFileCache) {
        if (sBmpBorder == null && iconZipFileCache != null) {
            sBmpBorder = iconZipFileCache.getBitmapEntry(this.mResourcesImpl, ICON_BORDER_FILE);
            if (sBmpBorder != null) {
                sBmpBorder = IconBitmapUtils.zoomIfNeed(sBmpBorder, sStandardBgSize, true);
            }
        }
    }

    public static synchronized HwCustHwResources getHwCustHwResources() {
        HwCustHwResources hwCustHwResources;
        synchronized (HwResourcesImpl.class) {
            if (mCustHwResources == null) {
                mCustHwResources = (HwCustHwResources) HwCustUtils.createObj(HwCustHwResources.class, new Object[0]);
            }
            hwCustHwResources = mCustHwResources;
        }
        return hwCustHwResources;
    }

    public Bitmap getThemeIconByName(String name) {
        String imgFile = name;
        if (!name.endsWith(".png")) {
            imgFile = name + ".png";
        }
        if (THEME_ANIMATE_FOLDER_ICON_NAME.equalsIgnoreCase(imgFile)) {
            imgFile = THEME_FOLDER_ICON_NAME;
        }
        Bitmap bmp = null;
        synchronized (HwResourcesImpl.class) {
            ZipFileCache iconZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), ICONS_ZIPFILE);
            if (iconZipFileCache != null) {
                bmp = iconZipFileCache.getBitmapEntry(this.mResourcesImpl, imgFile);
            }
            if (bmp != null) {
                Bitmap srcBitmap = IconBitmapUtils.zoomIfNeed(bmp, sStandardBgSize, true);
                return srcBitmap;
            }
            return bmp;
        }
    }

    public Bitmap addShortcutBackgroud(Bitmap bmpSrc) {
        synchronized (HwResources.class) {
            ZipFileCache iconZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), ICONS_ZIPFILE);
            if (iconZipFileCache == null) {
                return null;
            }
            initBorder(iconZipFileCache);
            initMask(iconZipFileCache);
            Bitmap bitmap = null;
            if (sBgList == null || sBgList.isEmpty()) {
                getAllBgImage(getThemeDir(), ICONS_ZIPFILE);
            }
            if (sBgList != null) {
                int len = sBgList.size();
                if (len > 0) {
                    bitmap = (Bitmap) sBgList.get(new Random().nextInt(len));
                }
            }
            initMaskSizeWithoutEdge(true);
            int w = bmpSrc.getWidth();
            int h = bmpSrc.getHeight();
            int iconSize = sDefaultSizeWithoutEdge;
            Rect r = IconBitmapUtils.getIconInfo(bmpSrc);
            if (r != null) {
                iconSize = computeDestIconSize(r, sMaskSizeWithoutEdge, w, h);
            }
            Bitmap composeIcon = IconBitmapUtils.composeIcon(IconBitmapUtils.drawSource(bmpSrc, sStandardBgSize, iconSize), sBmpMask, bitmap, sBmpBorder, false);
            return composeIcon;
        }
    }

    private Bitmap addIconBackgroud(Bitmap bmpSrc, String idAndPackageName, boolean composeIcon) {
        synchronized (HwResourcesImpl.class) {
            ZipFileCache iconZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), ICONS_ZIPFILE);
            if (iconZipFileCache == null) {
                return null;
            }
            initBorder(iconZipFileCache);
            initMask(iconZipFileCache);
            Bitmap bmpBg = getRandomBgImage(idAndPackageName);
            initMaskSizeWithoutEdge(true);
            int w = bmpSrc.getWidth();
            int h = bmpSrc.getHeight();
            int iconSize = sDefaultSizeWithoutEdge;
            Rect r = IconBitmapUtils.getIconInfo(bmpSrc);
            if (r != null) {
                iconSize = computeDestIconSize(r, sMaskSizeWithoutEdge, w, h);
            }
            if (w == 1 && h == 1) {
                Bitmap overlap2Bitmap = IconBitmapUtils.overlap2Bitmap(bmpSrc, bmpBg);
                return overlap2Bitmap;
            }
            Bitmap bmpSrcStd = IconBitmapUtils.drawSource(bmpSrc, sStandardBgSize, iconSize);
            if (composeIcon) {
                overlap2Bitmap = IconBitmapUtils.composeIcon(bmpSrcStd, sBmpMask, bmpBg, sBmpBorder, false);
                return overlap2Bitmap;
            }
            return bmpSrcStd;
        }
    }

    protected Drawable handleAddIconBackground(Resources res, int id, Drawable dr) {
        if (id != 0) {
            String resourcesPackageName = this.mResourcesImpl.getResourcePackageName(id);
            String idAndPackageName = id + "#" + resourcesPackageName;
            if (IconCache.contains(idAndPackageName)) {
                String resName = this.mResourcesImpl.getResourceName(id);
                if (resName != null && resName.contains("ic_statusbar_battery")) {
                    Log.d("battery_debug", "HwResourcesImpl#handleAddIconBackground idAndPackageName = " + idAndPackageName + " mPackageName = " + this.mPackageName + " id = 0x" + Integer.toHexString(id) + " resName = " + resName);
                }
                boolean isAdaptiveIcon = dr instanceof AdaptiveIconDrawable;
                String packageName = this.mPackageName != null ? this.mPackageName : resourcesPackageName;
                Bitmap bmp = IconBitmapUtils.drawableToBitmap(dr);
                if (bmp == null) {
                    return dr;
                }
                if (bmp.getWidth() > sDefaultSizeWithoutEdge * 3 || bmp.getHeight() > sDefaultSizeWithoutEdge * 3) {
                    Log.w(TAG, "icon in pkg " + packageName + " is too large." + "sDefaultSizeWithoutEdge = " + sDefaultSizeWithoutEdge + "bmp.getWidth() = " + bmp.getWidth());
                    bmp = IconBitmapUtils.zoomIfNeed(bmp, sDefaultSizeWithoutEdge, true);
                    dr = new BitmapDrawable(res, bmp);
                    if (dr != null) {
                        ((BitmapDrawable) dr).setTargetDensity(this.mResourcesImpl.getDisplayMetrics());
                    }
                }
                ZipFileCache iconZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), ICONS_ZIPFILE);
                if (iconZipFileCache == null) {
                    return dr;
                }
                initMask(iconZipFileCache);
                Bitmap resultBitmap = null;
                if (sBmpMask != null) {
                    if ((!isIconSupportCut && isCurrentHwTheme(getThemeDir(), FILE_DESCRIPTION)) || checkWhiteListApp(packageName)) {
                        resultBitmap = isAdaptiveIcon ? addIconBackgroud(bmp, idAndPackageName, true) : addIconBackgroud(bmp, idAndPackageName, false);
                    } else if ((getHwCustHwResources() == null || getHwCustHwResources().isUseThemeIconAndBackground()) && (sDisThemeBackground == null || (sDisThemeBackground.contains(packageName) ^ 1) != 0)) {
                        resultBitmap = addIconBackgroud(bmp, idAndPackageName, true);
                    }
                }
                if (resultBitmap == null) {
                    resultBitmap = IconBitmapUtils.zoomIfNeed(bmp, sStandardBgSize, false);
                }
                if (resultBitmap != null) {
                    dr = new BitmapDrawable(res, resultBitmap);
                    if (dr != null) {
                        ((BitmapDrawable) dr).setTargetDensity(this.mResourcesImpl.getDisplayMetrics());
                    }
                    return dr;
                }
            }
        }
        return dr;
    }

    private boolean checkWhiteListApp(String packageName) {
        if (sDisThemeIcon != null && sDisThemeIcon.contains(packageName) && isCurrentHwTheme(getThemeDir(), FILE_DESCRIPTION)) {
            return true;
        }
        if (packageName != null && packageName.contains(GOOGLE_KEYWORD) && isCurrentHwTheme(getThemeDir(), FILE_DESCRIPTION)) {
            return true;
        }
        int i = 0;
        while (i < APP_WHILTLIST_ADD.length) {
            if (packageName != null && packageName.equals(APP_WHILTLIST_ADD[i]) && isCurrentHwTheme(getThemeDir(), FILE_DESCRIPTION)) {
                return true;
            }
            i++;
        }
        return false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Drawable getThemeIcon(Resources resources, String iconKey, String packageName) {
        Throwable th;
        if (checkWhiteListApp(packageName)) {
            return null;
        }
        Drawable drawable = null;
        CacheEntry ce = IconCache.get(iconKey);
        if (packageNameMap.get(packageName) != null) {
            packageName = (String) packageNameMap.get(packageName);
        }
        String imgFile = getIconFileName(packageName, ce);
        synchronized (HwResourcesImpl.class) {
            try {
                Bitmap bmp = getIconsfromDiffTheme(this.mResourcesImpl, imgFile);
                ZipFileCache iconZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), ICONS_ZIPFILE);
                if (bmp == null && iconZipFileCache != null) {
                    bmp = iconZipFileCache.getBitmapEntry(this.mResourcesImpl, imgFile);
                    if (!imgFile.equals(getIconFileName(packageName)) && bmp == null) {
                        bmp = iconZipFileCache.getBitmapEntry(this.mResourcesImpl, getIconFileName(packageName));
                    }
                }
                if (bmp != null) {
                    Bitmap srcBitmap = IconBitmapUtils.zoomIfNeed(bmp, sStandardBgSize, true);
                    if (srcBitmap != null) {
                        Drawable dr = new BitmapDrawable(resources, srcBitmap);
                        if (dr != null) {
                            try {
                                ((BitmapDrawable) dr).setTargetDensity(this.mResourcesImpl.getDisplayMetrics());
                                drawable = dr;
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        }
                        drawable = dr;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    private ThemeColor loadThemeColor(String packageName, String zipName, String resName, String themeXml, int defaultColor) {
        if (!(mNonThemedPackage.contains(packageName) || (mPreloadedThemeColorList.contains(packageName) ^ 1) == 0)) {
            ZipFileCache packageZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), zipName);
            if (packageZipFileCache != null) {
                parserColorInfoList(packageName, packageZipFileCache.getInputStreamEntry(themeXml), true);
                mPreloadedThemeColorList.add(packageName);
            } else {
                mNonThemedPackage.add(packageName);
            }
        }
        Integer num = (Integer) mCacheColorInfoList.get(resName);
        String themeType = this.mDeepThemeType;
        if (!TextUtils.isEmpty(themeType) && num == null) {
            num = (Integer) mAssetCacheColorInfoList.get(resName);
        }
        if (!(TextUtils.isEmpty(themeType) || value != null || (mPreloadedThemeColorList.contains(packageName) ^ 1) == 0 || (mPreloadedAssetsPackage.contains(packageName) ^ 1) == 0)) {
            loadThemeColorFromAssets(packageName, themeXml, themeType);
            mPreloadedAssetsPackage.add(packageName);
            num = (Integer) mAssetCacheColorInfoList.get(resName);
        }
        if (num != null) {
            return new ThemeColor(num.intValue(), true);
        }
        return new ThemeColor(defaultColor, false);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parserColorInfoList(String packageName, InputStream is, boolean isZipFile) {
        if (is != null) {
            BufferedInputStream bufferedInput = new BufferedInputStream(is);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(bufferedInput, null);
                inflateColorInfoList(parser, packageName, isZipFile);
                try {
                    bufferedInput.close();
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "loadThemeColor : IOException when close stream");
                }
            } catch (XmlPullParserException e2) {
                Log.e(TAG, "loadThemeColor : XmlPullParserException");
            } catch (IOException e3) {
                Log.e(TAG, "loadThemeColor : IOException");
                try {
                    bufferedInput.close();
                    is.close();
                } catch (IOException e4) {
                    Log.e(TAG, "loadThemeColor : IOException when close stream");
                }
            } catch (RuntimeException e5) {
                Log.e(TAG, "loadThemeColor : RuntimeException", e5);
                try {
                    bufferedInput.close();
                    is.close();
                } catch (IOException e6) {
                    Log.e(TAG, "loadThemeColor : IOException when close stream");
                }
            } catch (Throwable th) {
                try {
                    bufferedInput.close();
                    is.close();
                } catch (IOException e7) {
                    Log.e(TAG, "loadThemeColor : IOException when close stream");
                }
            }
        }
    }

    public ThemeColor getThemeColor(TypedValue value, int id) throws NotFoundException {
        if (this.mResourcesImpl.mPreloading) {
            return new ThemeColor(value.data, false);
        }
        ThemeResource themeRes = getThemeResource(id, null);
        String packageName = themeRes.packageName;
        String resName = themeRes.resName;
        boolean isFramework = packageName.equals(FRAMEWORK_RES);
        boolean isHwFramework = packageName.equals(FRAMEWORK_RES_EXT);
        if (!((!isFramework && !isHwFramework) || getPackageName() == null || (getPackageName().isEmpty() ^ 1) == 0)) {
            String fwDir = isFramework ? FRAMEWORK_RES : FRAMEWORK_RES_EXT;
            ThemeColor tc = loadThemeColor(getPackageName() + "/" + fwDir, getPackageName(), getPackageName() + "/" + resName, fwDir + "/theme.xml", value.data);
            if (tc.mIsThemed) {
                return tc;
            }
        }
        return loadThemeColor(packageName, packageName, resName, "theme.xml", value.data);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected Drawable loadDrawable(Resources wrapper, TypedValue value, int id, Theme theme, boolean useCache) throws NotFoundException {
        long key = (((long) value.assetCookie) << 32) | ((long) value.data);
        Drawable dr = null;
        ConstantState constantState;
        if (value.type < 28 || value.type > 31) {
            synchronized (HwResourcesImpl.class) {
                dr = this.mDrawableCacheEx.getInstance(key, wrapper, theme);
            }
            if (dr != null) {
                return dr;
            }
            synchronized (HwResourcesImpl.class) {
                constantState = (this.mResourcesImpl.mPreloading || this.mThemeChanged) ? null : resUtils.getPreloadedDensity(this.mResourcesImpl) == this.mResourcesImpl.getConfiguration().densityDpi ? (ConstantState) sPreloadedDrawablesEx.get(key) : null;
                if (constantState != null) {
                    dr = constantState.newDrawable(wrapper);
                    return dr;
                }
            }
        }
        if (id != 0) {
            synchronized (HwResourcesImpl.class) {
                dr = this.mColorDrawableCacheEx.getInstance(key, wrapper, theme);
            }
            if (dr != null) {
                return dr;
            }
            synchronized (HwResourcesImpl.class) {
                constantState = (this.mResourcesImpl.mPreloading || this.mThemeChanged) ? null : (ConstantState) sPreloadedColorDrawablesEx.get(key);
            }
            if (constantState != null) {
                dr = constantState.newDrawable(wrapper);
            } else {
                ThemeColor colorValue = getThemeColor(value, id);
                if (colorValue != null && colorValue.mIsThemed) {
                    dr = new ColorDrawable(colorValue.mColor);
                }
            }
            if (dr != null) {
                constantState = dr.getConstantState();
                if (constantState != null) {
                    synchronized (HwResourcesImpl.class) {
                        if (this.mResourcesImpl.mPreloading) {
                            sPreloadedColorDrawablesEx.put(key, constantState);
                        } else {
                            this.mColorDrawableCacheEx.put(key, theme, constantState);
                        }
                    }
                }
            }
        }
        return dr;
    }

    public Drawable getThemeDrawable(TypedValue value, int id, Resources res, String packageName) throws NotFoundException {
        if (this.mResourcesImpl.mPreloading) {
            return null;
        }
        packageName = getThemeResource(id, packageName).packageName;
        if (value.string == null) {
            return null;
        }
        if (!emptyOrContainsPreloadedZip(packageName) && (emptyOrContainsPreloadedZip(this.mPackageName) ^ 1) != 0 && this.mCurrentDeepTheme == 0) {
            return null;
        }
        String file = value.string.toString();
        if (file == null || file.isEmpty()) {
            return null;
        }
        file = file.replaceFirst("-v\\d+/", "/");
        if (file == null || file.isEmpty()) {
            return null;
        }
        Drawable drawable = null;
        if (packageName.indexOf(FRAMEWORK_RES) >= 0 && file.indexOf("_holo") >= 0) {
            return null;
        }
        int themeDensity;
        String dir;
        boolean isLand = file.indexOf("-land") >= 0;
        boolean isFramework = packageName.equals(FRAMEWORK_RES);
        boolean isHwFramework = packageName.equals(FRAMEWORK_RES_EXT);
        String themeType = this.mDeepThemeType;
        if (!((!isFramework && !isHwFramework) || this.mPackageName == null || (this.mPackageName.isEmpty() ^ 1) == 0)) {
            ZipFileCache frameworkZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), this.mPackageName);
            String key = packageName + "/" + file;
            if (frameworkZipFileCache != null) {
                drawable = frameworkZipFileCache.getDrawableEntry(res, value, key, null);
                if (drawable == null && (file.contains(DRAWABLE_FHD) ^ 1) != 0) {
                    frameworkZipFileCache.initResDirInfo();
                    int index = isFramework ? isLand ? 3 : 2 : isLand ? 5 : 4;
                    themeDensity = frameworkZipFileCache.getDrawableDensity(index);
                    dir = frameworkZipFileCache.getDrawableDir(index);
                    if (!(themeDensity == -1 || dir == null)) {
                        Options opts = new Options();
                        opts.inDensity = themeDensity;
                        drawable = frameworkZipFileCache.getDrawableEntry(res, value, dir + File.separator + file.substring(file.lastIndexOf(File.separator) + 1), opts);
                    }
                }
                if (drawable != null) {
                    return drawable;
                }
            }
            if (!TextUtils.isEmpty(themeType) && r19 == null && frameworkZipFileCache == null) {
                drawable = getFwkDrawableFromAsset(value, res, packageName, file, key, themeType);
            }
            if (drawable != null) {
                return drawable;
            }
        }
        ZipFileCache packageZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), packageName);
        if (packageZipFileCache != null) {
            drawable = packageZipFileCache.getDrawableEntry(res, value, file, null);
        }
        if (!(packageZipFileCache == null || r19 != null || (file.contains(DRAWABLE_FHD) ^ 1) == 0)) {
            packageZipFileCache.initResDirInfo();
            index = isLand ? 1 : 0;
            themeDensity = packageZipFileCache.getDrawableDensity(index);
            dir = packageZipFileCache.getDrawableDir(index);
            if (!(themeDensity == -1 || dir == null)) {
                opts = new Options();
                opts.inDensity = themeDensity;
                if (file.contains(DPI_2K)) {
                    drawable = packageZipFileCache.getDrawableEntry(res, value, file.replace(DPI_2K, DPI_FHD), opts);
                } else {
                    drawable = packageZipFileCache.getDrawableEntry(res, value, dir + File.separator + file.substring(file.lastIndexOf(File.separator) + 1), opts);
                }
            }
        }
        if (!TextUtils.isEmpty(themeType) && r19 == null && packageZipFileCache == null) {
            if (isFramework) {
                isHwFramework = true;
            }
            if ((isHwFramework ^ 1) != 0) {
                drawable = getAppDrawableFromAsset(value, res, packageName, file, themeType);
            }
        }
        return drawable;
    }

    public ThemeResource getThemeResource(int id, String packageName) {
        if (packageName == null) {
            packageName = this.mResourcesImpl.getResourcePackageName(id);
        }
        String resName = this.mResourcesImpl.getResourceName(id);
        if (packageName.equals(ANDROID_RES)) {
            packageName = FRAMEWORK_RES;
            resName = FRAMEWORK_RES + resName.substring(7);
        } else if (packageName.equals(ANDROID_RES_EXT)) {
            packageName = FRAMEWORK_RES_EXT;
            resName = FRAMEWORK_RES_EXT + resName.substring(12);
        }
        return new ThemeResource(packageName, resName);
    }

    public void setResourcesImpl(ResourcesImpl resourcesImpl) {
        this.mResourcesImpl = resourcesImpl;
    }

    public void initResource() {
        if (-1 == sStandardBgSize) {
            setStandardSize(sConfigAppBigIconSize == -1 ? getDimensionPixelSize(34472064) : sConfigAppBigIconSize, sConfigAppInnerIconSize == -1 ? getDimensionPixelSize(34472063) : sConfigAppInnerIconSize);
        }
        HwResources.setIsSRLocale("sr".equals(Locale.getDefault().getLanguage()));
        checkChangedNameFile();
    }

    public int getDimensionPixelSize(int id) throws NotFoundException {
        TypedValue value = obtainTempTypedValue();
        try {
            this.mResourcesImpl.getValue(id, value, true);
            if (value.type == 5) {
                int complexToDimensionPixelSize = TypedValue.complexToDimensionPixelSize(value.data, this.mResourcesImpl.getDisplayMetrics());
                return complexToDimensionPixelSize;
            }
            throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id) + " type #0x" + Integer.toHexString(value.type) + " is not valid");
        } finally {
            releaseTempTypedValue(value);
        }
    }

    protected void releaseTempTypedValue(TypedValue value) {
        synchronized (this.mTmpValueLock) {
            if (this.mTmpValue == null) {
                this.mTmpValue = value;
            }
        }
    }

    protected TypedValue obtainTempTypedValue() {
        TypedValue typedValue = null;
        synchronized (this.mTmpValueLock) {
            if (this.mTmpValue != null) {
                typedValue = this.mTmpValue;
                this.mTmpValue = null;
            }
        }
        if (typedValue == null) {
            return new TypedValue();
        }
        return typedValue;
    }

    private static void setStandardSize(int standardBgSize, int standardIconSize) {
        sStandardBgSize = standardBgSize;
        sDefaultSizeWithoutEdge = sStandardBgSize + 8;
        sStandardIconSize = standardIconSize;
    }

    protected void handleClearCache(int configChanges, boolean themeChanged) {
        synchronized (HwResourcesImpl.class) {
            if (themeChanged) {
                mCacheColorInfoList.clear();
                mAssetCacheColorInfoList.clear();
                mPreloadedThemeColorList.clear();
                mNonThemedPackage.clear();
                mPreloadedAssetsPackage.clear();
                mCacheAssetsList.clear();
                clearHwThemeZipsAndIconsCache();
                sThemeDescripHasRead = false;
                sIsHWThemes = true;
            }
            if (this.mDrawableCacheEx != null) {
                this.mDrawableCacheEx.onConfigurationChange(configChanges);
            }
            if (this.mDynamicDrawableCache != null) {
                this.mDynamicDrawableCache.onConfigurationChange(configChanges);
            }
            if (this.mColorDrawableCacheEx != null) {
                this.mColorDrawableCacheEx.onConfigurationChange(configChanges);
            }
        }
    }

    public void setPackageName(String name) {
        this.mPackageName = name;
        ResourcesImpl resourcesImpl = this.mResourcesImpl;
        if (ResourcesImpl.mIsLockResWhitelist) {
            String currentPackageName = ActivityThread.currentPackageName();
            if (!("com.nttdocomo.android.dhome".equals(currentPackageName) || this.mPackageName == null || (this.mPackageName.equals(currentPackageName) ^ 1) == 0)) {
                this.mResourcesImpl.updateAssetConfiguration(this.mPackageName, false, false);
            }
        }
    }

    public void setResourcesPackageName(String name) {
        this.mPackageName = name;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    protected void setHwTheme(Configuration config) {
        boolean z = true;
        if (config != null) {
            this.mConfigHwt = config.extraConfig.getConfigItem(1);
            if (this.mResourcesImpl.getConfiguration().extraConfig.getConfigItem(1) == 0) {
                z = false;
            }
            this.mThemeChanged = z;
        }
    }

    public void updateConfiguration(Configuration config, int configChanges) {
        IHwConfiguration configEx = this.mResourcesImpl.getConfiguration().extraConfig;
        if (this.mConfigHwt != configEx.getConfigItem(1)) {
            this.mConfigHwt = configEx.getConfigItem(1);
            synchronized (HwResourcesImpl.class) {
                this.mThemeChanged = true;
            }
            handleClearCache(configChanges, this.mThemeChanged);
            int userId = configEx.getConfigItem(3);
            String path = Environment.getDataDirectory() + "/themes/" + userId;
            synchronized (HwResourcesImpl.class) {
                if (!mThemeAbsoluteDir.equals(path)) {
                    setHwThemeAbsoluteDir(path, userId);
                    Log.i(TAG, "updateConfiguration! the new theme absolute directory:" + mThemeAbsoluteDir);
                }
            }
            this.mDarkThemeType = SystemProperties.get("persist.deep.theme_" + userId);
            this.mDeepThemeType = getThemeType();
        }
        if (this.mResourcesImpl.getConfiguration().locale != null) {
            HwResources.setIsSRLocale("sr".equals(this.mResourcesImpl.getConfiguration().locale.getLanguage()));
        }
    }

    public Drawable getDrawableForDynamic(Resources res, String packageName, String iconName) throws NotFoundException {
        String imgFile = "";
        imgFile = "dynamic_icons/" + packageName + "/" + iconName + ".png";
        long key = (((long) imgFile.length()) << 32) | ((long) getCode(imgFile));
        Drawable dr = this.mDynamicDrawableCache.getInstance(key, res, null);
        if (dr != null) {
            return dr;
        }
        ZipFileCache iconZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), ICONS_ZIPFILE);
        if (iconZipFileCache == null) {
            return null;
        }
        Bitmap bmp = iconZipFileCache.getBitmapEntry(this.mResourcesImpl, imgFile);
        if (bmp != null) {
            dr = new BitmapDrawable(res, bmp);
            if (dr != null) {
                ((BitmapDrawable) dr).setTargetDensity(this.mResourcesImpl.getDisplayMetrics());
                this.mDynamicDrawableCache.put(key, null, dr.getConstantState(), true);
            }
        }
        return dr;
    }

    private static synchronized void readDefaultConfig() {
        Throwable th;
        synchronized (HwResourcesImpl.class) {
            if (sDefaultConfigHasRead) {
                return;
            }
            File inputFile;
            InputStream inputStream = null;
            try {
                inputFile = HwCfgFilePolicy.getCfgFile(DEFAULT_CONFIG_NAME, 0);
                if (inputFile == null) {
                    inputFile = new File(DEFAULT_CONFIG_PATH);
                }
            } catch (NoClassDefFoundError e) {
                Log.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
                inputFile = new File(DEFAULT_CONFIG_PATH);
            } catch (Throwable th2) {
                inputFile = new File(DEFAULT_CONFIG_PATH);
            }
            try {
                InputStream in = new FileInputStream(inputFile);
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(in, null);
                    XmlUtils.beginDocument(parser, TAG_CONFIG);
                    sDisThemeIcon = new HashSet();
                    sDisThemeBackground = new HashSet();
                    for (int type = parser.next(); type != 1; type = parser.next()) {
                        if (type == 2) {
                            String packageName = parser.getAttributeValue(0);
                            Object DrawableType = null;
                            parser.next();
                            if (parser.getEventType() == 4) {
                                DrawableType = String.valueOf(parser.getText());
                            }
                            if (TYPT_ICON.equals(DrawableType)) {
                                sDisThemeIcon.add(packageName);
                            } else if (TYPT_BACKGROUND.equals(DrawableType)) {
                                sDisThemeBackground.add(packageName);
                            }
                        }
                    }
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e2) {
                            Log.e(TAG, "readDefaultConfig : IOException when close stream");
                        }
                    }
                } catch (FileNotFoundException e3) {
                    inputStream = in;
                } catch (XmlPullParserException e4) {
                    inputStream = in;
                } catch (Throwable th3) {
                    th = th3;
                    inputStream = in;
                }
            } catch (FileNotFoundException e5) {
                try {
                    Log.e(TAG, "readDefaultConfig : FileNotFoundException");
                    sDisThemeIcon = null;
                    sDisThemeBackground = null;
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e6) {
                            Log.e(TAG, "readDefaultConfig : IOException when close stream");
                        }
                    }
                    sDefaultConfigHasRead = true;
                } catch (Throwable th4) {
                    th = th4;
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e7) {
                            Log.e(TAG, "readDefaultConfig : IOException when close stream");
                        }
                    }
                    throw th;
                }
            } catch (XmlPullParserException e8) {
                Log.e(TAG, "readDefaultConfig : XmlPullParserException | IOException");
                sDisThemeIcon = null;
                sDisThemeBackground = null;
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e9) {
                        Log.e(TAG, "readDefaultConfig : IOException when close stream");
                    }
                }
                sDefaultConfigHasRead = true;
            }
            sDefaultConfigHasRead = true;
        }
    }

    private static synchronized void loadMultiDpiWhiteList() {
        synchronized (HwResourcesImpl.class) {
            sMultiDirHasRead = true;
            ArrayList<File> configFiles = new ArrayList();
            try {
                configFiles = HwCfgFilePolicy.getCfgFileList(FILE_MULTI_DPI_WHITELIST, 0);
            } catch (NoClassDefFoundError e) {
                Log.e(TAG, "HwCfgFilePolicy NoClassDefFoundError! ");
            }
            int size = configFiles.size();
            for (int i = 0; i < size; i++) {
                setMultiDpiPkgNameFromXml((File) configFiles.get(i));
            }
            setMultiDpiPkgNameFromXml(new File(DEFAULT_MULTI_DPI_WHITELIST));
        }
    }

    private static synchronized void setMultiDpiPkgNameFromXml(File configFile) {
        Throwable th;
        synchronized (HwResourcesImpl.class) {
            if (configFile == null) {
                return;
            }
            InputStream inputStream = null;
            try {
                InputStream inputStream2 = new FileInputStream(configFile);
                try {
                    XmlPullParser xmlParser = Xml.newPullParser();
                    xmlParser.setInput(inputStream2, null);
                    for (int xmlEventType = xmlParser.next(); xmlEventType != 1; xmlEventType = xmlParser.next()) {
                        if (xmlEventType == 2) {
                            String packageName = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_PACKAGE_NAME);
                            if (packageName != null) {
                                sMultiDirPkgNames.add(packageName);
                            }
                        }
                    }
                    if (inputStream2 != null) {
                        try {
                            inputStream2.close();
                        } catch (IOException e) {
                            Log.e(TAG, "loadMultiDpiWhiteList:- IOE while closing stream");
                        } catch (Throwable th2) {
                            th = th2;
                            inputStream = inputStream2;
                            throw th;
                        }
                    }
                    inputStream = inputStream2;
                } catch (FileNotFoundException e2) {
                    inputStream = inputStream2;
                    sMultiDirHasRead = false;
                    Log.e(TAG, "loadMultiDpiWhiteList : FileNotFoundException");
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e3) {
                            Log.e(TAG, "loadMultiDpiWhiteList:- IOE while closing stream");
                        }
                    }
                } catch (XmlPullParserException e4) {
                    inputStream = inputStream2;
                    sMultiDirHasRead = false;
                    Log.e(TAG, "loadMultiDpiWhiteList : XmlPullParserException");
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e5) {
                            Log.e(TAG, "loadMultiDpiWhiteList:- IOE while closing stream");
                        }
                    }
                } catch (IOException e6) {
                    inputStream = inputStream2;
                    try {
                        sMultiDirHasRead = false;
                        Log.e(TAG, "loadMultiDpiWhiteList : IOException");
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e7) {
                                Log.e(TAG, "loadMultiDpiWhiteList:- IOE while closing stream");
                            } catch (Throwable th3) {
                                th = th3;
                                throw th;
                            }
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e8) {
                                Log.e(TAG, "loadMultiDpiWhiteList:- IOE while closing stream");
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th5) {
                    th = th5;
                    inputStream = inputStream2;
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    throw th;
                }
            } catch (FileNotFoundException e9) {
                sMultiDirHasRead = false;
                Log.e(TAG, "loadMultiDpiWhiteList : FileNotFoundException");
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (XmlPullParserException e10) {
                sMultiDirHasRead = false;
                Log.e(TAG, "loadMultiDpiWhiteList : XmlPullParserException");
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e11) {
                sMultiDirHasRead = false;
                Log.e(TAG, "loadMultiDpiWhiteList : IOException");
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
    }

    public boolean isInMultiDpiWhiteList(String packageName) {
        if (!sMultiDirHasRead) {
            loadMultiDpiWhiteList();
        }
        Boolean result = Boolean.valueOf(true);
        if (sMultiDirPkgNames.size() == 0) {
            Log.i(TAG, "open lock res on whitelist, but not provide the whitelist, so lock resource");
        } else if (packageName == null) {
            Log.i(TAG, "can't get the package name, so lock resource");
        } else if (!sMultiDirPkgNames.contains(packageName)) {
            result = Boolean.valueOf(false);
        }
        return result.booleanValue();
    }

    private String getIconFileName(String packageName, CacheEntry ce) {
        StringBuilder stringBuilder = new StringBuilder();
        if (ce.type == 1) {
            packageName = ce.name;
        }
        return stringBuilder.append(packageName).append(".png").toString();
    }

    private String getIconFileName(String packageName) {
        return packageName + ".png";
    }

    private int computeDestIconSize(Rect validRect, int maskSize, int w, int h) {
        double validSize;
        if (validRect.height() > validRect.width()) {
            validSize = (((double) maskSize) / (((double) validRect.height()) + 1.0d)) * ((double) h);
        } else {
            validSize = (((double) maskSize) / (((double) validRect.width()) + 1.0d)) * ((double) w);
        }
        int iconSize = (int) validSize;
        if (1 == iconSize % 2) {
            return (int) (0.5d + validSize);
        }
        return iconSize;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void checkChangedNameFile() {
        synchronized (HwResourcesImpl.class) {
            if (packageNameMap.size() > 0) {
            } else if (bCfgFileNotExist) {
            } else {
                File file = HwCfgFilePolicy.getCfgFile(CHANGEPACKAGE_CONFIG_NAME, 0);
                if (file == null) {
                    bCfgFileNotExist = true;
                    return;
                }
                StringBuilder sb = new StringBuilder();
                try {
                    FileInputStream fis = new FileInputStream(file);
                    while (true) {
                        try {
                            int ch = fis.read();
                            if (ch != -1) {
                                sb.append((char) ch);
                            } else {
                                try {
                                    break;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        } catch (Throwable th) {
                            try {
                                fis.close();
                            } catch (IOException e22) {
                                e22.printStackTrace();
                            }
                        }
                    }
                    fis.close();
                } catch (FileNotFoundException e3) {
                    e3.printStackTrace();
                }
                if (sb.length() == 0) {
                    return;
                }
                for (String pkg : sb.toString().trim().split(";")) {
                    String[] newPkgs = pkg.split(",");
                    if (newPkgs.length == 2) {
                        packageNameMap.put(newPkgs[1], newPkgs[0]);
                    }
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Drawable getJoinBitmap(Drawable srcDraw, int backgroundId) {
        synchronized (HwResourcesImpl.class) {
            if (srcDraw == null) {
                Log.w(TAG, "notification icon is null!");
                return null;
            }
            Bitmap bmpSrc = null;
            if (srcDraw instanceof BitmapDrawable) {
                Bitmap temp = ((BitmapDrawable) srcDraw).getBitmap();
                int width = temp.getWidth();
                int height = temp.getHeight();
                int[] pixels = new int[(width * height)];
                temp.getPixels(pixels, 0, width, 0, 0, width, height);
                bmpSrc = Bitmap.createBitmap(pixels, width, height, Config.ARGB_8888);
            }
            if (bmpSrc == null) {
                Log.w(TAG, "getJoinBitmap : bmpSrc is null!");
                return null;
            }
            ZipFileCache iconZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), ICONS_ZIPFILE);
            if (iconZipFileCache == null) {
                Log.w(TAG, "getJoinBitmap : iconZipFileCache == null");
                return null;
            }
            Bitmap bmpBg = iconZipFileCache.getBitmapEntry(this.mResourcesImpl, NOTIFICATION_ICON_PREFIX + backgroundId + ".png");
            if (bmpBg == null) {
                bmpBg = iconZipFileCache.getBitmapEntry(this.mResourcesImpl, NOTIFICATION_ICON_EXIST);
            }
            Bitmap bmpMask = iconZipFileCache.getBitmapEntry(this.mResourcesImpl, NOTIFICATION_ICON_MASK);
            Bitmap bmpBorder = iconZipFileCache.getBitmapEntry(this.mResourcesImpl, NOTIFICATION_ICON_BORDER);
            if (bmpBg == null || bmpMask == null || bmpBorder == null) {
                Log.w(TAG, "getJoinBitmap :" + (bmpBg == null ? " bmpBg == null, " : " bmpBg != null, ") + " id = " + backgroundId + (bmpMask == null ? " bmpMask == null, " : "bmpMask != null, ") + (bmpBorder == null ? " bmpBorder == null, " : " bmpBorder != null"));
                return null;
            }
            int w = bmpSrc.getWidth();
            int h = bmpSrc.getHeight();
            Drawable drawable;
            if (w == 1 && h == 1) {
                Bitmap overlap = IconBitmapUtils.overlap2Bitmap(bmpSrc, bmpBg);
                if (overlap == null) {
                    drawable = null;
                } else {
                    drawable = new BitmapDrawable(overlap);
                }
            } else {
                if (!(bmpMask.getWidth() == w || bmpMask.getHeight() == h)) {
                    bmpMask = IconBitmapUtils.drawSource(bmpMask, w, w);
                }
                if (!(bmpBorder.getWidth() == w || bmpBorder.getHeight() == h)) {
                    bmpBorder = IconBitmapUtils.drawSource(bmpBorder, w, w);
                }
                if (!(bmpBg.getWidth() == w || bmpBg.getHeight() == h)) {
                    bmpBg = IconBitmapUtils.drawSource(bmpBg, w, w);
                }
                Bitmap result = IconBitmapUtils.composeIcon(bmpSrc, bmpMask, bmpBg, bmpBorder, false);
                if (result != null) {
                    drawable = new BitmapDrawable(result);
                    return drawable;
                }
                Log.w(TAG, "getJoinBitmap is null!");
                return null;
            }
        }
    }

    public String getThemeDir() {
        String str;
        synchronized (HwResourcesImpl.class) {
            str = mThemeAbsoluteDir;
        }
        return str;
    }

    public static boolean emptyOrContainsPreloadedZip(String zipFile) {
        boolean contains;
        synchronized (HwResourcesImpl.class) {
            contains = !sPreloadedHwThemeZips.isEmpty() ? sPreloadedHwThemeZips.contains(zipFile) : true;
        }
        return contains;
    }

    public String getDiffThemeIconPath() {
        if (TextUtils.isEmpty(CUSTOM_DIFF_THEME_DIR)) {
            File iconZip;
            try {
                iconZip = HwCfgFilePolicy.getCfgFile(DIFF_THEME_ICON, 0);
            } catch (NoClassDefFoundError e) {
                iconZip = null;
                Log.e(TAG, "HwCfgFilePolicy NoClassDefFoundError");
            }
            if (iconZip != null) {
                return iconZip.getParent();
            }
            return null;
        } else if (new File(CUSTOM_DIFF_THEME_DIR + "/" + ICONS_ZIPFILE).exists()) {
            return CUSTOM_DIFF_THEME_DIR;
        } else {
            return null;
        }
    }

    public Bitmap getIconsfromDiffTheme(ResourcesImpl Impl, String imgFile) {
        Bitmap bmp = null;
        String iconFilePath = getDiffThemeIconPath();
        if (iconFilePath != null) {
            ZipFileCache diffIconZipFileCache = ZipFileCache.getAndCheckCachedZipFile(iconFilePath, ICONS_ZIPFILE);
            if (diffIconZipFileCache != null) {
                bmp = diffIconZipFileCache.getBitmapEntry(Impl, imgFile);
                if (bmp != null) {
                    Log.i(TAG, "icon : " + imgFile + " found in custom diff theme");
                }
            }
        }
        return bmp;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void preloadHwThemeZipsAndSomeIcons(int currentUserId) {
        setHwThemeAbsoluteDir(Environment.getDataDirectory() + "/themes/" + currentUserId, currentUserId);
        synchronized (HwResourcesImpl.class) {
            File themePath = new File(mThemeAbsoluteDir);
            if (themePath.exists()) {
                File[] files = themePath.listFiles(new HwThemeFileFilter());
                if (files == null) {
                    return;
                }
                for (File f : files) {
                    sPreloadedHwThemeZips.add(f.getName());
                }
            }
        }
    }

    private void setHwThemeAbsoluteDir(String path, int userId) {
        synchronized (HwResourcesImpl.class) {
            mThemeAbsoluteDir = path;
            mCurrentUserId = userId;
        }
    }

    public void clearHwThemeZipsAndSomeIcons() {
        synchronized (HwResourcesImpl.class) {
            clearHwThemeZipsAndIconsCache();
        }
    }

    private void clearHwThemeZipsAndIconsCache() {
        if (sBgList != null) {
            int listSize = sBgList.size();
            for (int i = 0; i < listSize; i++) {
                ((Bitmap) sBgList.get(i)).recycle();
            }
            sBgList.clear();
        }
        if (!(sBmpBorder == null || (sBmpBorder.isRecycled() ^ 1) == 0)) {
            sBmpBorder.recycle();
            sBmpBorder = null;
        }
        if (!(sBmpMask == null || (sBmpMask.isRecycled() ^ 1) == 0)) {
            sBmpMask.recycle();
            sBmpMask = null;
            sMaskSizeWithoutEdge = -1;
        }
        ZipFileCache.clear();
        sUseAvgColor = -1;
        sPreloadedHwThemeZips.clear();
    }

    private void inflateColorInfoList(XmlPullParser parser, String packageName, boolean isZipFile) throws XmlPullParserException, IOException {
        int innerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (parser.getDepth() <= innerDepth && type == 3) {
                return;
            }
            if (type == 2) {
                String lableName = parser.getName();
                if (lableName.equals("color") || (lableName.equals("drawable") ^ 1) == 0) {
                    String name = parser.getAttributeName(0);
                    String colorName = parser.getAttributeValue(0);
                    String text = null;
                    if (parser.next() == 4) {
                        text = parser.getText();
                    }
                    if (XML_ATTRIBUTE_PACKAGE_NAME.equalsIgnoreCase(name)) {
                        int colorVaue = getColorValueFromStr(text);
                        StringBuilder sb = new StringBuilder();
                        sb.append(packageName);
                        sb.append(":");
                        sb.append(lableName);
                        sb.append("/");
                        sb.append(colorName);
                        String fullColorName = sb.toString();
                        if (isZipFile) {
                            mCacheColorInfoList.put(fullColorName, Integer.valueOf(colorVaue));
                        } else {
                            mAssetCacheColorInfoList.put(fullColorName, Integer.valueOf(colorVaue));
                        }
                    }
                }
            }
        }
    }

    private int getColorValueFromStr(String value) throws IOException {
        if (value == null) {
            return 0;
        }
        if (value.startsWith("#")) {
            value = value.substring(1);
            if (value.length() > 8) {
                Log.e(TAG, "getColorValueFromStr " + String.format("Color value '%s' is too long. Format is either#AARRGGBB, #RRGGBB, #RGB, or #ARGB", new Object[]{value}));
                return 0;
            }
            char[] color;
            char charAt;
            if (value.length() == 3) {
                color = new char[8];
                charAt = value.charAt(0);
                color[3] = charAt;
                color[2] = charAt;
                charAt = value.charAt(1);
                color[5] = charAt;
                color[4] = charAt;
                charAt = value.charAt(2);
                color[7] = charAt;
                color[6] = charAt;
                value = new String(color);
            } else if (value.length() == 4) {
                color = new char[8];
                charAt = value.charAt(0);
                color[1] = charAt;
                color[0] = charAt;
                charAt = value.charAt(1);
                color[3] = charAt;
                color[2] = charAt;
                charAt = value.charAt(2);
                color[5] = charAt;
                color[4] = charAt;
                charAt = value.charAt(3);
                color[7] = charAt;
                color[6] = charAt;
                value = new String(color);
            } else if (value.length() == 6) {
                value = "FF" + value;
            }
            return (int) Long.parseLong(value, 16);
        }
        Log.e(TAG, "getColorValueFromStr " + String.format("Color value '%s' must start with #", new Object[]{value}));
        return 0;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isCurrentHwTheme(String dirpath, String filename) {
        synchronized (HwResourcesImpl.class) {
            if (sThemeDescripHasRead) {
                boolean z = sIsHWThemes;
                return z;
            }
        }
        if (document == null) {
            return sIsHWThemes;
        }
        Element rootElement = document.getDocumentElement();
        if (rootElement == null) {
            return sIsHWThemes;
        }
        NodeList itemNodes = rootElement.getChildNodes();
        int itemNodeLength = itemNodes.getLength();
        for (int i = 0; i < itemNodeLength; i++) {
            Node itemNode = itemNodes.item(i);
            if (itemNode.getNodeType() == (short) 1 && THEMEDISIGNER.equals(itemNode.getNodeName())) {
                sIsHWThemes = EMUI.equals(itemNode.getTextContent());
                synchronized (HwResourcesImpl.class) {
                    sThemeDescripHasRead = true;
                }
                return sIsHWThemes;
            }
        }
        synchronized (HwResourcesImpl.class) {
            sThemeDescripHasRead = true;
        }
        return sIsHWThemes;
    }

    public void initDeepTheme() {
        if (UserHandle.getAppId(Process.myUid()) < 1000) {
            Log.w(TAG, "zygote not need to initDeepTheme");
            return;
        }
        boolean z;
        this.mDarkThemeType = SystemProperties.get("persist.deep.theme_" + mCurrentUserId);
        if (isDeepDarkTheme()) {
            z = true;
        } else {
            z = HwThemeManager.isHonorProduct();
        }
        if (!z) {
            return;
        }
        if (this.mResourcesImpl == null) {
            Log.w(TAG, "setHwAppDarkSign fail mResourcesImpl null ");
            return;
        }
        AssetManager asset = this.mResourcesImpl.getAssets();
        if (asset == null) {
            Log.w(TAG, "setHwAppDarkSign fail asset null ");
            return;
        }
        String[] pathArray = null;
        String[] honorPathArray = null;
        try {
            if (HwThemeManager.isHonorProduct()) {
                honorPathArray = asset.list(HONOR);
            } else {
                pathArray = asset.list(DARK);
            }
        } catch (IOException e) {
        }
        int honorLength = honorPathArray != null ? honorPathArray.length : 0;
        boolean isSupportDarkTheme = (pathArray != null ? pathArray.length : 0) > 0;
        boolean isSupportHonorTheme = honorLength > 0;
        if (isSupportDarkTheme) {
            this.mCurrentDeepTheme = 1;
        }
        if (isSupportHonorTheme) {
            this.mCurrentDeepTheme = 16;
        }
        this.mDeepThemeType = getThemeType();
    }

    @SuppressLint({"AvoidInHardConnectInString"})
    private Drawable getAppDrawableFromAsset(TypedValue value, Resources res, String packageName, String file, String dir) {
        StringBuilder drawableSb = new StringBuilder();
        drawableSb.append(this.mPackageName);
        drawableSb.append(":").append(dir).append("/").append(file);
        String drawablePath = drawableSb.toString();
        if (DEFAULT_NO_DRAWABLE_FLAG.equals((String) mCacheAssetsList.get(drawablePath))) {
            return null;
        }
        Drawable dr = AssetsFileCache.getDrawableEntry(this.mResourcesImpl.getAssets(), res, value, dir + File.separator + file, null);
        if (dr == null && (file.contains(DRAWABLE_FHD) ^ 1) != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(dir);
            sb.append(File.separator);
            sb.append(getDefaultResDir());
            sb.append(File.separator);
            sb.append(file.substring(file.lastIndexOf(File.separator) + 1));
            dr = AssetsFileCache.getDrawableEntry(this.mResourcesImpl.getAssets(), res, value, sb.toString(), getDefaultOptions());
        }
        if (dr == null) {
            mCacheAssetsList.put(drawablePath, DEFAULT_NO_DRAWABLE_FLAG);
        }
        return dr;
    }

    @SuppressLint({"AvoidInHardConnectInString"})
    private Drawable getFwkDrawableFromAsset(TypedValue value, Resources res, String packageName, String file, String key, String dir) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.mPackageName);
        sb.append(":").append(dir).append("/").append(key);
        String fullPath = sb.toString();
        if (DEFAULT_NO_DRAWABLE_FLAG.equals((String) mCacheAssetsList.get(fullPath))) {
            return null;
        }
        Drawable dr = AssetsFileCache.getDrawableEntry(this.mResourcesImpl.getAssets(), res, value, dir + File.separator + key, null);
        if (dr == null && (file.contains(DRAWABLE_FHD) ^ 1) != 0) {
            StringBuilder fullName = new StringBuilder();
            fullName.append(dir);
            fullName.append(File.separator);
            fullName.append(packageName);
            fullName.append(File.separator);
            fullName.append(getDefaultResDir());
            fullName.append(File.separator);
            fullName.append(file.substring(file.lastIndexOf(File.separator) + 1));
            dr = AssetsFileCache.getDrawableEntry(this.mResourcesImpl.getAssets(), res, value, fullName.toString(), getDefaultOptions());
        }
        if (dr == null) {
            mCacheAssetsList.put(fullPath, DEFAULT_NO_DRAWABLE_FLAG);
        }
        return dr;
    }

    private Options getDefaultOptions() {
        Options opts = new Options();
        opts.inDensity = 480;
        return opts;
    }

    private String getDefaultResDir() {
        return DEFAULT_RES_DIR;
    }

    private String getThemeType() {
        if (isDeepDarkTheme() && (this.mCurrentDeepTheme == 1 || this.mCurrentDeepTheme == 17)) {
            return DARK;
        }
        if (HwThemeManager.isHonorProduct() && (this.mCurrentDeepTheme == 16 || this.mCurrentDeepTheme == 17)) {
            return HONOR;
        }
        return null;
    }

    protected String getDeepThemeType() {
        return this.mDeepThemeType;
    }

    private boolean isDeepDarkTheme() {
        return DARK.equals(this.mDarkThemeType);
    }

    private void loadThemeColorFromAssets(String packageName, String themeXml, String dir) {
        if ((!packageName.equals(FRAMEWORK_RES) && !packageName.equals(FRAMEWORK_RES_EXT)) || (themeXml.contains(packageName) ^ 1) == 0) {
            parserColorInfoList(packageName, AssetsFileCache.getInputStreamEntry(this.mResourcesImpl.getAssets(), dir + File.separator + themeXml), false);
        }
    }

    protected Bundle getMultidpiInfo(String packageName) {
        if (!SUPPORT_LOCK_DPI || packageName == null) {
            return null;
        }
        Bundle dpiInfo = (Bundle) sMultidpiInfos.get(packageName);
        if (dpiInfo != null) {
            return dpiInfo;
        }
        dpiInfo = new Bundle();
        ApplicationInfo tmpInfo = null;
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            if (pm != null) {
                tmpInfo = pm.getApplicationInfo(packageName, 128, UserHandle.getUserId(Process.myUid()));
            }
        } catch (RemoteException e) {
            Log.w(TAG, "getApplicationInfo for " + packageName + ", Exception:" + e);
        }
        if (tmpInfo != null) {
            boolean needLockRes = (tmpInfo.flags & 1) != 0;
            Bundle metaData = tmpInfo.metaData;
            if (metaData != null) {
                if ("default".equalsIgnoreCase(metaData.getString("support_display_mode"))) {
                    dpiInfo.putBoolean("LockDpi", true);
                }
                String msg = metaData.getString("support_lock_res");
                if ("lock".equalsIgnoreCase(msg)) {
                    dpiInfo.putBoolean("LockRes", true);
                } else if ("no_lock".equalsIgnoreCase(msg)) {
                    dpiInfo.putBoolean("LockRes", false);
                } else {
                    dpiInfo.putBoolean("LockRes", needLockRes);
                }
            } else {
                dpiInfo.putBoolean("LockRes", needLockRes);
            }
        }
        sMultidpiInfos.put(packageName, dpiInfo);
        return dpiInfo;
    }
}
