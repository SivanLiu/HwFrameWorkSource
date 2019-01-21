package android.content.res;

import android.annotation.SuppressLint;
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
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
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
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwResourcesImpl extends AbsResourcesImpl {
    private static final String ANDROID_RES = "android";
    private static final String ANDROID_RES_EXT = "androidhwext";
    private static final String[] APP_WHILTLIST_ADD = new String[]{"com.android.chrome", "com.android.vending", "com.whatsapp", "com.facebook.orca"};
    private static final String CUSTOM_DIFF_THEME_DIR = SystemProperties.get("ro.config.diff_themes");
    private static final String DARK = "dark";
    private static final String DARK_OVERLAY_RES = "com.android.frameworkhwext.dark";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_DRAWABLE = false;
    private static final boolean DEBUG_ICON = false;
    private static final boolean DEBUG_VERBOSE_ICON = false;
    private static final String[] DEEPTHEME_WHILTLIST_ADD = new String[]{"/system/priv-app/Settings/Settings.apk", "/system/priv-app/Contacts/Contacts.apk"};
    private static final String DEFAULT_CONFIG_NAME = "xml/hw_launcher_load_icon.xml";
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
    private static final String EMUI_TAG;
    private static final String FILE_DESCRIPTION = "description.xml";
    private static final String FILE_MULTI_DPI_WHITELIST = "xml/multidpi_whitelist.xml";
    private static final String FRAMEWORK_RES = "framework-res";
    private static final String FRAMEWORK_RES_EXT = "framework-res-hwext";
    private static final String GOOGLE_KEYWORD = "com.google";
    private static final String HONOR = "honor";
    private static final String HONOR_OVERLAY_RES = "com.android.frameworkhwext.honor";
    private static final String ICONS_ZIPFILE = "icons";
    private static final String ICON_BACKGROUND_PREFIX = "icon_background";
    private static final String ICON_BORDER_FILE = "icon_border.png";
    private static final String ICON_BORDER_UNFLAT_FILE = "icon_border_unflat.png";
    private static final String ICON_MASK_ALL_FILE = "icon_mask_all.png";
    private static final String ICON_MASK_FILE = "icon_mask.png";
    private static final boolean IS_CUSTOM_ICON = SystemProperties.getBoolean("ro.config.custom_icon", false);
    private static final int LEN_OF_ANDROID = 7;
    private static final int LEN_OF_ANDROID_EXT = 12;
    private static final int LEN_OF_ANDROID_EXT_DARK = 31;
    private static final int LEN_OF_ANDROID_EXT_HONOR = 32;
    private static final int LEN_OF_ANDROID_EXT_NOVA = 31;
    private static final int MASK_ABS_VALID_RANGE = 10;
    private static final String NOTIFICATION_ICON_BORDER = "ic_stat_notify_icon_border.png";
    private static final String NOTIFICATION_ICON_EXIST = "ic_stat_notify_bg_0.png";
    private static final String NOTIFICATION_ICON_MASK = "ic_stat_notify_icon_mask.png";
    private static final String NOTIFICATION_ICON_PREFIX = "ic_stat_notify_bg_";
    private static final String NOVA = "nova";
    private static final String NOVA_OVERLAY_RES = "com.android.frameworkhwext.nova";
    static final String TAG = "HwResourcesImpl";
    private static final String TAG_CONFIG = "launcher_config";
    private static final String THEMEDISIGNER = "designer";
    static final String THEME_ANIMATE_FOLDER_ICON_NAME = "portal_ring_inner_holo.png.animate";
    static final String THEME_FOLDER_ICON_NAME = "portal_ring_inner_holo.png";
    private static final String TYPT_ACTIVITY_ICON = "activityIcon";
    private static final String TYPT_BACKGROUND = "background";
    private static final String TYPT_ICON = "icon";
    private static final String XML_ATTRIBUTE_PACKAGE_NAME = "name";
    private static final String XML_SUFFIX = ".xml";
    private static final boolean isIconSupportCut = SystemProperties.getBoolean("ro.config.hw_icon_supprot_cut", true);
    private static HashMap<String, Integer> mAssetCacheColorInfoList = new HashMap();
    private static HashMap<String, String> mCacheAssetsList = new HashMap();
    private static HashMap<String, Integer> mCacheColorInfoList = new HashMap();
    private static int mCurrentUserId = 0;
    private static HwCustHwResources mCustHwResources = null;
    private static HashSet<String> mNonThemedPackage = new HashSet();
    private static HashSet<String> mPreloadedAssetsPackage = new HashSet();
    private static HashSet<String> mPreloadedThemeColorList = new HashSet();
    private static String mThemeAbsoluteDir;
    private static ResourcesUtils resUtils = ((ResourcesUtils) EasyInvokeFactory.getInvokeUtils(ResourcesUtils.class));
    private static ArrayList<Bitmap> sBgList = new ArrayList();
    private static Bitmap sBmpBorder = null;
    private static Bitmap sBmpMask = null;
    private static final Object sCacheLock = new Object();
    private static final int sConfigAppBigIconSize = SystemProperties.getInt("ro.config.app_big_icon_size", -1);
    private static final int sConfigAppInnerIconSize = SystemProperties.getInt("ro.config.app_inner_icon_size", -1);
    private static boolean sDefaultConfigHasRead = false;
    private static int sDefaultSizeWithoutEdge = -1;
    private static HashSet<String> sDisThemeActivityIcon = null;
    private static HashSet<String> sDisThemeBackground = null;
    private static HashSet<String> sDisThemeIcon = null;
    private static boolean sIsHWThemes = true;
    private static int sMaskSizeWithoutEdge = -1;
    private static boolean sMultiDirHasRead = false;
    private static HashSet<String> sMultiDirPkgNames = new HashSet();
    private static Pattern sPattern = Pattern.compile("-v\\d+/");
    private static LongSparseArray<ConstantState> sPreloadedColorDrawablesEx = new LongSparseArray();
    private static LongSparseArray<ConstantState> sPreloadedDrawablesEx = new LongSparseArray();
    private static final Object sPreloadedHwThemeZipLock = new Object();
    private static final HashSet<String> sPreloadedHwThemeZips = new HashSet();
    private static boolean sSerbiaLocale = false;
    private static int sStandardBgSize = -1;
    private static int sStandardIconSize = -1;
    private static Object sThemeColorArrayLock = new Object();
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
    private List<Integer> mOutThemeColorIdArray = new ArrayList();
    protected String mPackageName = null;
    private ResourcesImpl mResourcesImpl;
    protected boolean mThemeChanged = false;
    private HashMap<Integer, ThemeColor> mThemeColorArray = new HashMap();
    private TypedValue mTmpValue = new TypedValue();
    private final Object mTmpValueLock = new Object();
    private ArrayList<String> recPackageList = new ArrayList();

    static class HwThemeFileFilter implements FileFilter {
        HwThemeFileFilter() {
        }

        public boolean accept(File pathname) {
            if (!pathname.isFile() || pathname.getName().toLowerCase(Locale.getDefault()).endsWith(HwResourcesImpl.XML_SUFFIX)) {
                return false;
            }
            return true;
        }
    }

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Environment.getDataDirectory());
        stringBuilder.append("/themes/0");
        mThemeAbsoluteDir = stringBuilder.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(File.separator);
        stringBuilder2.append("emui_");
        EMUI_TAG = stringBuilder2.toString();
    }

    private void getAllBgImage(String path, String zip) {
        if (sBmpBorder != null) {
            if (sBgList == null || sBgList.isEmpty()) {
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
        if (sBmpMask != null && !sBmpMask.isRecycled()) {
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
            synchronized (HwResourcesImpl.class) {
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
        if (!imgFile.endsWith(".png")) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(imgFile);
            stringBuilder.append(".png");
            imgFile = stringBuilder.toString();
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
            int len;
            initBorder(iconZipFileCache);
            initMask(iconZipFileCache);
            Bitmap bmpBg = null;
            if (sBgList == null || sBgList.isEmpty()) {
                getAllBgImage(getThemeDir(), ICONS_ZIPFILE);
            }
            if (sBgList != null) {
                len = sBgList.size();
                if (len > 0) {
                    bmpBg = (Bitmap) sBgList.get(new Random().nextInt(len));
                }
            }
            initMaskSizeWithoutEdge(true);
            len = bmpSrc.getWidth();
            int h = bmpSrc.getHeight();
            int iconSize = sDefaultSizeWithoutEdge;
            Rect r = IconBitmapUtils.getIconInfo(bmpSrc);
            if (r != null) {
                iconSize = computeDestIconSize(r, sMaskSizeWithoutEdge, len, h);
            }
            Bitmap composeIcon = IconBitmapUtils.composeIcon(IconBitmapUtils.drawSource(bmpSrc, sStandardBgSize, iconSize), sBmpMask, bmpBg, sBmpBorder, false);
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
            Bitmap overlap2Bitmap;
            if (w == 1 && h == 1) {
                overlap2Bitmap = IconBitmapUtils.overlap2Bitmap(bmpSrc, bmpBg);
                return overlap2Bitmap;
            }
            overlap2Bitmap = IconBitmapUtils.drawSource(bmpSrc, sStandardBgSize, iconSize);
            if (composeIcon) {
                Bitmap composeIcon2 = IconBitmapUtils.composeIcon(overlap2Bitmap, sBmpMask, bmpBg, sBmpBorder, false);
                return composeIcon2;
            }
            return overlap2Bitmap;
        }
    }

    protected Drawable handleAddIconBackground(Resources res, int id, Drawable dr) {
        if (id != 0) {
            String resourcesPackageName = this.mResourcesImpl.getResourcePackageName(id);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(id);
            stringBuilder.append("#");
            stringBuilder.append(resourcesPackageName);
            String idAndPackageName = stringBuilder.toString();
            if (IconCache.contains(idAndPackageName)) {
                boolean isAdaptiveIcon = dr instanceof AdaptiveIconDrawable;
                String packageName = this.mPackageName != null ? this.mPackageName : resourcesPackageName;
                Bitmap bmp = IconBitmapUtils.drawableToBitmap(dr);
                if (bmp == null) {
                    return dr;
                }
                boolean z = true;
                if (bmp.getWidth() > sDefaultSizeWithoutEdge * 3 || bmp.getHeight() > 3 * sDefaultSizeWithoutEdge) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("icon in pkg ");
                    stringBuilder2.append(packageName);
                    stringBuilder2.append(" is too large.sDefaultSizeWithoutEdge = ");
                    stringBuilder2.append(sDefaultSizeWithoutEdge);
                    stringBuilder2.append("bmp.getWidth() = ");
                    stringBuilder2.append(bmp.getWidth());
                    Log.w(str, stringBuilder2.toString());
                    bmp = IconBitmapUtils.zoomIfNeed(bmp, sDefaultSizeWithoutEdge, true);
                    dr = new BitmapDrawable(res, bmp);
                    ((BitmapDrawable) dr).setTargetDensity(this.mResourcesImpl.getDisplayMetrics());
                }
                ZipFileCache iconZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), ICONS_ZIPFILE);
                if (iconZipFileCache == null) {
                    return dr;
                }
                initMask(iconZipFileCache);
                Bitmap resultBitmap = null;
                if ((!isIconSupportCut && isCurrentHwTheme(getThemeDir(), FILE_DESCRIPTION)) || checkWhiteListApp(packageName)) {
                    resultBitmap = addIconBackgroud(bmp, idAndPackageName, false);
                } else if ((getHwCustHwResources() == null || getHwCustHwResources().isUseThemeIconAndBackground()) && (sDisThemeBackground == null || !sDisThemeBackground.contains(packageName))) {
                    if (isAdaptiveIcon) {
                        z = false;
                    }
                    resultBitmap = addIconBackgroud(bmp, idAndPackageName, z);
                }
                if (resultBitmap == null) {
                    resultBitmap = IconBitmapUtils.zoomIfNeed(bmp, sStandardBgSize, false);
                }
                if (resultBitmap != null) {
                    dr = new BitmapDrawable(res, resultBitmap);
                    ((BitmapDrawable) dr).setTargetDensity(this.mResourcesImpl.getDisplayMetrics());
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

    private boolean checkWhiteImgFile(String imgFile) {
        if (sDisThemeActivityIcon == null || !sDisThemeActivityIcon.contains(imgFile)) {
            return false;
        }
        return true;
    }

    private Drawable getThemeIcon(Resources resources, String iconKey, String packageName) {
        if (checkWhiteListApp(packageName)) {
            return null;
        }
        Drawable dr = null;
        Bitmap bmp = null;
        String imgFile = getIconFileName(packageName, IconCache.get(iconKey));
        if (checkWhiteImgFile(imgFile)) {
            return null;
        }
        synchronized (HwResourcesImpl.class) {
            if (IS_CUSTOM_ICON) {
                bmp = getIconsfromDiffTheme(this.mResourcesImpl, imgFile, packageName);
            }
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
                    dr = new BitmapDrawable(resources, srcBitmap);
                    ((BitmapDrawable) dr).setTargetDensity(this.mResourcesImpl.getDisplayMetrics());
                }
            }
        }
        return dr;
    }

    private ThemeColor loadThemeColor(String packageName, String zipName, String resName, String themeXml, int defaultColor) {
        if (!(isEmptyPreloadZip(zipName) || mNonThemedPackage.contains(packageName) || mPreloadedThemeColorList.contains(packageName))) {
            ZipFileCache packageZipFileCache = ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), zipName);
            if (packageZipFileCache != null) {
                parserColorInfoList(packageName, packageZipFileCache.getInputStreamEntry(themeXml), true);
                mPreloadedThemeColorList.add(packageName);
            } else {
                mNonThemedPackage.add(packageName);
            }
        }
        Integer value = null;
        if (!mCacheColorInfoList.isEmpty()) {
            value = (Integer) mCacheColorInfoList.get(resName);
        }
        String themeType = this.mDeepThemeType;
        if (!TextUtils.isEmpty(themeType) && value == null) {
            value = (Integer) mAssetCacheColorInfoList.get(resName);
            if (!(value != null || mPreloadedThemeColorList.contains(packageName) || mPreloadedAssetsPackage.contains(packageName))) {
                loadThemeColorFromAssets(packageName, themeXml, themeType);
                mPreloadedAssetsPackage.add(packageName);
                value = (Integer) mAssetCacheColorInfoList.get(resName);
            }
        }
        if (value != null) {
            return new ThemeColor(value.intValue(), true);
        }
        return new ThemeColor(defaultColor, false);
    }

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
                bufferedInput.close();
                is.close();
            } catch (IOException e3) {
                Log.e(TAG, "loadThemeColor : IOException");
                bufferedInput.close();
                is.close();
            } catch (RuntimeException e4) {
                Log.e(TAG, "loadThemeColor : RuntimeException", e4);
                bufferedInput.close();
                is.close();
            } catch (Throwable th) {
                try {
                    bufferedInput.close();
                    is.close();
                } catch (IOException e5) {
                    Log.e(TAG, "loadThemeColor : IOException when close stream");
                }
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0014, code skipped:
            r1 = false;
     */
    /* JADX WARNING: Missing block: B:8:0x0019, code skipped:
            if (r7.mResourcesImpl.mPreloading != false) goto L_0x011c;
     */
    /* JADX WARNING: Missing block: B:9:0x001b, code skipped:
            if (r9 == false) goto L_0x001f;
     */
    /* JADX WARNING: Missing block: B:10:0x001f, code skipped:
            r3 = sThemeColorArrayLock;
     */
    /* JADX WARNING: Missing block: B:11:0x0022, code skipped:
            monitor-enter(r3);
     */
    /* JADX WARNING: Missing block: B:14:0x0029, code skipped:
            if (r7.mThemeColorArray.isEmpty() != false) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:16:0x0037, code skipped:
            r10 = (android.content.res.AbsResourcesImpl.ThemeColor) r7.mThemeColorArray.get(java.lang.Integer.valueOf(r19));
     */
    /* JADX WARNING: Missing block: B:17:0x0039, code skipped:
            r10 = null;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:20:0x003b, code skipped:
            if (r10 == null) goto L_0x003e;
     */
    /* JADX WARNING: Missing block: B:21:0x003d, code skipped:
            return r10;
     */
    /* JADX WARNING: Missing block: B:22:0x003e, code skipped:
            r12 = getThemeResource(r19, null);
            r13 = r12.packageName;
            r14 = r12.resName;
     */
    /* JADX WARNING: Missing block: B:23:0x004f, code skipped:
            if (r13.equals(FRAMEWORK_RES) != false) goto L_0x005b;
     */
    /* JADX WARNING: Missing block: B:25:0x0057, code skipped:
            if (r13.equals(FRAMEWORK_RES_EXT) == false) goto L_0x005c;
     */
    /* JADX WARNING: Missing block: B:26:0x005b, code skipped:
            r1 = true;
     */
    /* JADX WARNING: Missing block: B:28:0x005d, code skipped:
            if (r1 == false) goto L_0x00d2;
     */
    /* JADX WARNING: Missing block: B:30:0x0063, code skipped:
            if (getPackageName() == null) goto L_0x00d2;
     */
    /* JADX WARNING: Missing block: B:32:0x006d, code skipped:
            if (getPackageName().isEmpty() != false) goto L_0x00d2;
     */
    /* JADX WARNING: Missing block: B:33:0x006f, code skipped:
            r0 = new java.lang.StringBuilder();
            r0.append(getPackageName());
            r0.append("/");
            r0.append(r13);
            r16 = r0.toString();
            r3 = getPackageName();
            r0 = new java.lang.StringBuilder();
            r0.append(getPackageName());
            r0.append("/");
            r0.append(r14);
            r4 = r0.toString();
            r0 = new java.lang.StringBuilder();
            r0.append(r13);
            r0.append("/theme.xml");
            r1 = loadThemeColor(r16, r3, r4, r0.toString(), r8.data);
     */
    /* JADX WARNING: Missing block: B:34:0x00bf, code skipped:
            if (r1.mIsThemed == false) goto L_0x00d2;
     */
    /* JADX WARNING: Missing block: B:35:0x00c1, code skipped:
            r2 = sThemeColorArrayLock;
     */
    /* JADX WARNING: Missing block: B:36:0x00c3, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:38:?, code skipped:
            r7.mThemeColorArray.put(java.lang.Integer.valueOf(r19), r1);
     */
    /* JADX WARNING: Missing block: B:39:0x00cd, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:40:0x00ce, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:44:0x00d2, code skipped:
            r1 = loadThemeColor(r13, r13, r14, "theme.xml", r8.data);
     */
    /* JADX WARNING: Missing block: B:45:0x00e0, code skipped:
            if (r1.mIsThemed == false) goto L_0x00f3;
     */
    /* JADX WARNING: Missing block: B:46:0x00e2, code skipped:
            r2 = sThemeColorArrayLock;
     */
    /* JADX WARNING: Missing block: B:47:0x00e4, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:49:?, code skipped:
            r7.mThemeColorArray.put(java.lang.Integer.valueOf(r19), r1);
     */
    /* JADX WARNING: Missing block: B:50:0x00ee, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:55:0x00f3, code skipped:
            r2 = sThemeColorArrayLock;
     */
    /* JADX WARNING: Missing block: B:56:0x00f5, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:59:0x0100, code skipped:
            if (r7.mOutThemeColorIdArray.contains(java.lang.Integer.valueOf(r19)) != false) goto L_0x010b;
     */
    /* JADX WARNING: Missing block: B:60:0x0102, code skipped:
            r7.mOutThemeColorIdArray.add(java.lang.Integer.valueOf(r19));
     */
    /* JADX WARNING: Missing block: B:61:0x010b, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:62:0x010c, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:66:0x0110, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:67:0x0111, code skipped:
            r11 = r19;
            r2 = r10;
     */
    /* JADX WARNING: Missing block: B:68:0x0115, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:69:0x0116, code skipped:
            r11 = r19;
     */
    /* JADX WARNING: Missing block: B:71:?, code skipped:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:72:0x0119, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:73:0x011a, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:75:0x011c, code skipped:
            r11 = r19;
     */
    /* JADX WARNING: Missing block: B:76:0x0125, code skipped:
            return new android.content.res.AbsResourcesImpl.ThemeColor(r8.data, false);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ThemeColor getThemeColor(TypedValue value, int id) throws NotFoundException {
        Throwable th;
        int i;
        TypedValue typedValue = value;
        synchronized (sThemeColorArrayLock) {
            try {
                boolean isOutThemeColorId = this.mOutThemeColorIdArray.contains(Integer.valueOf(id));
                try {
                } catch (Throwable th2) {
                    th = th2;
                    i = id;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    }
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                i = id;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:77:0x00ce  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x006b  */
    /* JADX WARNING: Missing block: B:81:0x00d6, code skipped:
            if (r4 == 0) goto L_0x01dd;
     */
    /* JADX WARNING: Missing block: B:82:0x00d8, code skipped:
            r10 = r1.mResourcesImpl.getResourcePackageName(r4);
            r0 = new java.lang.StringBuilder();
            r0.append(r4);
            r0.append("#");
            r0.append(r10);
            r11 = r0.toString();
            r12 = r1.mResourcesImpl.getResourceName(r4);
     */
    /* JADX WARNING: Missing block: B:83:0x00fc, code skipped:
            if (huawei.android.hwutil.IconCache.contains(r11) == false) goto L_0x0170;
     */
    /* JADX WARNING: Missing block: B:84:0x00fe, code skipped:
            readDefaultConfig();
            r13 = getThemeIcon(r2, r11, r10);
     */
    /* JADX WARNING: Missing block: B:85:0x0106, code skipped:
            if (r13 == null) goto L_0x01dd;
     */
    /* JADX WARNING: Missing block: B:86:0x0108, code skipped:
            if (r12 == null) goto L_0x014e;
     */
    /* JADX WARNING: Missing block: B:88:0x0110, code skipped:
            if (r12.contains("ic_statusbar_battery") == false) goto L_0x014e;
     */
    /* JADX WARNING: Missing block: B:89:0x0112, code skipped:
            r14 = new java.lang.StringBuilder();
            r14.append("HwResourcesImpl#loadDrawable iconKey = ");
            r14.append(r11);
            r14.append(" mPackageName = ");
            r14.append(r1.mPackageName);
            r14.append(" id = ");
            r14.append(java.lang.Integer.toHexString(r19));
            r14.append(" packageName = ");
            r14.append(r10);
            r14.append(" resName = ");
            r14.append(r12);
            android.util.Log.d("battery_debug", r14.toString());
     */
    /* JADX WARNING: Missing block: B:90:0x014e, code skipped:
            r14 = sCacheLock;
     */
    /* JADX WARNING: Missing block: B:91:0x0150, code skipped:
            monitor-enter(r14);
     */
    /* JADX WARNING: Missing block: B:94:0x0155, code skipped:
            if (r1.mResourcesImpl.mPreloading == false) goto L_0x0161;
     */
    /* JADX WARNING: Missing block: B:95:0x0157, code skipped:
            sPreloadedDrawablesEx.put(r6, r13.getConstantState());
     */
    /* JADX WARNING: Missing block: B:96:0x0161, code skipped:
            r1.mDrawableCacheEx.put(r6, r5, r13.getConstantState());
     */
    /* JADX WARNING: Missing block: B:97:0x016a, code skipped:
            monitor-exit(r14);
     */
    /* JADX WARNING: Missing block: B:98:0x016b, code skipped:
            return r13;
     */
    /* JADX WARNING: Missing block: B:102:0x0170, code skipped:
            r9 = getThemeDrawable(r3, r4, r2, r10);
     */
    /* JADX WARNING: Missing block: B:103:0x0174, code skipped:
            if (r9 == null) goto L_0x01ba;
     */
    /* JADX WARNING: Missing block: B:104:0x0176, code skipped:
            if (r12 == null) goto L_0x01ba;
     */
    /* JADX WARNING: Missing block: B:106:0x017e, code skipped:
            if (r12.contains("ic_statusbar_battery") == false) goto L_0x01ba;
     */
    /* JADX WARNING: Missing block: B:107:0x0180, code skipped:
            r13 = new java.lang.StringBuilder();
            r13.append("HwResourcesImpl#loadDrawable after getThemeDrawable  dr.getIntrinsicWidth() = ");
            r13.append(r9.getIntrinsicWidth());
            r13.append("dr.getIntrinsicHeight() = ");
            r13.append(r9.getIntrinsicHeight());
            r13.append(" id = 0x");
            r13.append(java.lang.Integer.toHexString(r19));
            r13.append(" resName = ");
            r13.append(r12);
            android.util.Log.d("battery_debug", r13.toString());
     */
    /* JADX WARNING: Missing block: B:108:0x01ba, code skipped:
            if (r9 == null) goto L_0x01dd;
     */
    /* JADX WARNING: Missing block: B:109:0x01bc, code skipped:
            r13 = sCacheLock;
     */
    /* JADX WARNING: Missing block: B:110:0x01be, code skipped:
            monitor-enter(r13);
     */
    /* JADX WARNING: Missing block: B:113:0x01c3, code skipped:
            if (r1.mResourcesImpl.mPreloading == false) goto L_0x01cf;
     */
    /* JADX WARNING: Missing block: B:114:0x01c5, code skipped:
            sPreloadedDrawablesEx.put(r6, r9.getConstantState());
     */
    /* JADX WARNING: Missing block: B:115:0x01cf, code skipped:
            r1.mDrawableCacheEx.put(r6, r5, r9.getConstantState());
     */
    /* JADX WARNING: Missing block: B:116:0x01d8, code skipped:
            monitor-exit(r13);
     */
    /* JADX WARNING: Missing block: B:117:0x01d9, code skipped:
            return r9;
     */
    /* JADX WARNING: Missing block: B:121:0x01dd, code skipped:
            return r9;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected Drawable loadDrawable(Resources wrapper, TypedValue value, int id, Theme theme, boolean useCache) throws NotFoundException {
        Resources resources = wrapper;
        TypedValue typedValue = value;
        int i = id;
        Theme theme2 = theme;
        long key = (((long) typedValue.assetCookie) << 32) | ((long) typedValue.data);
        Drawable dr = null;
        ConstantState constantState = null;
        if (typedValue.type < 28 || typedValue.type > 31) {
            synchronized (sCacheLock) {
                dr = this.mDrawableCacheEx.getInstance(key, resources, theme2);
            }
            if (dr != null) {
                return dr;
            }
            synchronized (sCacheLock) {
                ConstantState cs;
                if (!this.mResourcesImpl.mPreloading) {
                    if (!this.mThemeChanged) {
                        if (resUtils.getPreloadedDensity(this.mResourcesImpl) == this.mResourcesImpl.getConfiguration().densityDpi) {
                            constantState = (ConstantState) sPreloadedDrawablesEx.get(key);
                        }
                        cs = constantState;
                        if (cs == null) {
                            dr = cs.newDrawable(resources);
                            return dr;
                        }
                    }
                }
                cs = constantState;
                if (cs == null) {
                }
            }
        } else {
            if (i != 0) {
                synchronized (sCacheLock) {
                    dr = this.mColorDrawableCacheEx.getInstance(key, resources, theme2);
                }
                if (dr != null) {
                    return dr;
                }
                ConstantState cs2;
                Drawable dr2;
                synchronized (sCacheLock) {
                    if (!this.mResourcesImpl.mPreloading) {
                        if (!this.mThemeChanged) {
                            constantState = (ConstantState) sPreloadedColorDrawablesEx.get(key);
                        }
                    }
                    cs2 = constantState;
                }
                if (cs2 != null) {
                    dr2 = cs2.newDrawable(resources);
                } else {
                    ThemeColor colorValue = getThemeColor(typedValue, i);
                    if (colorValue != null && colorValue.mIsThemed) {
                        dr2 = new ColorDrawable(colorValue.mColor);
                    }
                    if (dr != null) {
                        constantState = dr.getConstantState();
                        if (constantState != null) {
                            synchronized (sCacheLock) {
                                if (this.mResourcesImpl.mPreloading) {
                                    sPreloadedColorDrawablesEx.put(key, constantState);
                                } else {
                                    this.mColorDrawableCacheEx.put(key, theme2, constantState);
                                }
                            }
                        }
                    }
                }
                dr = dr2;
                if (dr != null) {
                }
            }
            return dr;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x011a A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Drawable getThemeDrawable(TypedValue value, int id, Resources res, String packageName) throws NotFoundException {
        TypedValue typedValue = value;
        Resources resources = res;
        if (this.mResourcesImpl.mPreloading) {
            return null;
        }
        String packageName2 = getThemeResource(id, packageName).packageName;
        if (typedValue.string == null) {
            return null;
        }
        if (isEmptyPreloadZip(packageName2) && this.mCurrentDeepTheme == 0) {
            return null;
        }
        String file = typedValue.string.toString();
        if (file == null || file.isEmpty() || file.endsWith(XML_SUFFIX)) {
            return null;
        }
        String file2 = sPattern.matcher(file).replaceFirst("/");
        Drawable dr = null;
        if (packageName2.indexOf(FRAMEWORK_RES) >= 0 && file2.indexOf("_holo") >= 0) {
            return null;
        }
        int i;
        String themeType;
        String key;
        int index;
        int themeDensity;
        String dir;
        Drawable dr2;
        boolean isLand = file2.indexOf("-land") >= 0;
        boolean isFramework = packageName2.equals(FRAMEWORK_RES);
        boolean isHwFramework = packageName2.equals(FRAMEWORK_RES_EXT);
        String themeType2 = this.mDeepThemeType;
        if (!isFramework && !isHwFramework) {
            i = -1;
            themeType = themeType2;
        } else if (this.mPackageName == null || this.mPackageName.isEmpty()) {
            i = -1;
            themeType = themeType2;
        } else {
            ZipFileCache frameworkZipFileCache = isEmptyPreloadZip(packageName2) ? null : ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), this.mPackageName);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(packageName2);
            stringBuilder.append("/");
            stringBuilder.append(file2);
            key = stringBuilder.toString();
            if (frameworkZipFileCache != null) {
                dr = frameworkZipFileCache.getDrawableEntry(resources, typedValue, key, null);
                if (dr == null && !file2.contains(DRAWABLE_FHD)) {
                    frameworkZipFileCache.initResDirInfo();
                    index = isFramework ? isLand ? 3 : 2 : isLand ? 5 : 4;
                    themeDensity = frameworkZipFileCache.getDrawableDensity(index);
                    dir = frameworkZipFileCache.getDrawableDir(index);
                    if (!(themeDensity == -1 || dir == null)) {
                        Options opts = new Options();
                        opts.inDensity = themeDensity;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(dir);
                        stringBuilder2.append(File.separator);
                        stringBuilder2.append(file2.substring(file2.lastIndexOf(File.separator) + 1));
                        dr = frameworkZipFileCache.getDrawableEntry(resources, typedValue, stringBuilder2.toString(), opts);
                        if (dr != null) {
                            return dr;
                        }
                    }
                }
                dr = dr;
                if (dr != null) {
                }
            }
            dr2 = dr;
            if (!TextUtils.isEmpty(themeType2) && dr2 == null && frameworkZipFileCache == null) {
                i = -1;
                themeType = themeType2;
                dr = getFwkDrawableFromAsset(typedValue, resources, packageName2, file2, key, themeType2);
            } else {
                ZipFileCache zipFileCache = frameworkZipFileCache;
                themeType = themeType2;
                Drawable dr3 = dr2;
                i = -1;
                dr = dr3;
            }
            if (dr != null) {
                return dr;
            }
        }
        ZipFileCache packageZipFileCache = isEmptyPreloadZip(packageName2) ? null : ZipFileCache.getAndCheckCachedZipFile(getThemeDir(), packageName2);
        if (packageZipFileCache != null) {
            dr = packageZipFileCache.getDrawableEntry(resources, typedValue, file2, null);
        }
        if (!(packageZipFileCache == null || dr != null || file2.contains(DRAWABLE_FHD))) {
            packageZipFileCache.initResDirInfo();
            index = isLand ? 1 : 0;
            themeDensity = packageZipFileCache.getDrawableDensity(index);
            key = packageZipFileCache.getDrawableDir(index);
            if (!(themeDensity == i || key == null)) {
                Options opts2 = new Options();
                opts2.inDensity = themeDensity;
                if (file2.contains(DPI_2K)) {
                    dr = packageZipFileCache.getDrawableEntry(resources, typedValue, file2.replace(DPI_2K, DPI_FHD), opts2);
                } else {
                    String newFile = new StringBuilder();
                    newFile.append(key);
                    newFile.append(File.separator);
                    newFile.append(file2.substring(file2.lastIndexOf(File.separator) + 1));
                    dr = packageZipFileCache.getDrawableEntry(resources, typedValue, newFile.toString(), opts2);
                }
            }
        }
        dr2 = dr;
        dir = themeType;
        if (!(TextUtils.isEmpty(dir) || dr2 != null || packageZipFileCache != null || isFramework || isHwFramework)) {
            dr2 = getAppDrawableFromAsset(typedValue, resources, packageName2, file2, dir);
        }
        return dr2;
    }

    private boolean isEmptyPreloadZip(String packageName) {
        return (emptyOrContainsPreloadedZip(packageName) || emptyOrContainsPreloadedZip(this.mPackageName)) ? false : true;
    }

    public ThemeResource getThemeResource(int id, String packageName) {
        if (packageName == null) {
            packageName = this.mResourcesImpl.getResourcePackageName(id);
        }
        String resName = this.mResourcesImpl.getResourceName(id);
        StringBuilder stringBuilder;
        if (packageName.equals("android")) {
            packageName = FRAMEWORK_RES;
            stringBuilder = new StringBuilder();
            stringBuilder.append(FRAMEWORK_RES);
            stringBuilder.append(resName.substring(7));
            resName = stringBuilder.toString();
        } else if (packageName.equals(ANDROID_RES_EXT)) {
            packageName = FRAMEWORK_RES_EXT;
            stringBuilder = new StringBuilder();
            stringBuilder.append(FRAMEWORK_RES_EXT);
            stringBuilder.append(resName.substring(12));
            resName = stringBuilder.toString();
        } else if (packageName.equals(DARK_OVERLAY_RES)) {
            packageName = FRAMEWORK_RES_EXT;
            stringBuilder = new StringBuilder();
            stringBuilder.append(FRAMEWORK_RES_EXT);
            stringBuilder.append(resName.substring(31));
            resName = stringBuilder.toString();
        } else if (packageName.equals(HONOR_OVERLAY_RES)) {
            packageName = FRAMEWORK_RES_EXT;
            stringBuilder = new StringBuilder();
            stringBuilder.append(FRAMEWORK_RES_EXT);
            stringBuilder.append(resName.substring(32));
            resName = stringBuilder.toString();
        } else if (packageName.equals(NOVA_OVERLAY_RES)) {
            packageName = FRAMEWORK_RES_EXT;
            stringBuilder = new StringBuilder();
            stringBuilder.append(FRAMEWORK_RES_EXT);
            stringBuilder.append(resName.substring(31));
            resName = stringBuilder.toString();
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
    }

    public int getDimensionPixelSize(int id) throws NotFoundException {
        TypedValue value = obtainTempTypedValue();
        try {
            this.mResourcesImpl.getValue(id, value, true);
            if (value.type == 5) {
                int complexToDimensionPixelSize = TypedValue.complexToDimensionPixelSize(value.data, this.mResourcesImpl.getDisplayMetrics());
                return complexToDimensionPixelSize;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Resource ID #0x");
            stringBuilder.append(Integer.toHexString(id));
            stringBuilder.append(" type #0x");
            stringBuilder.append(Integer.toHexString(value.type));
            stringBuilder.append(" is not valid");
            throw new NotFoundException(stringBuilder.toString());
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
        TypedValue tmpValue = null;
        synchronized (this.mTmpValueLock) {
            if (this.mTmpValue != null) {
                tmpValue = this.mTmpValue;
                this.mTmpValue = null;
            }
        }
        if (tmpValue == null) {
            return new TypedValue();
        }
        return tmpValue;
    }

    private static void setStandardSize(int standardBgSize, int standardIconSize) {
        sStandardBgSize = standardBgSize;
        sDefaultSizeWithoutEdge = sStandardBgSize + 8;
        sStandardIconSize = standardIconSize;
    }

    protected void handleClearCache(int configChanges, boolean themeChanged) {
        if (themeChanged) {
            synchronized (sThemeColorArrayLock) {
                this.mThemeColorArray.clear();
                this.mOutThemeColorIdArray.clear();
            }
        }
        synchronized (HwResourcesImpl.class) {
            if (themeChanged) {
                try {
                    mCacheColorInfoList.clear();
                    mAssetCacheColorInfoList.clear();
                    mPreloadedThemeColorList.clear();
                    mNonThemedPackage.clear();
                    mPreloadedAssetsPackage.clear();
                    mCacheAssetsList.clear();
                    clearHwThemeZipsAndIconsCache();
                    sThemeDescripHasRead = false;
                    sIsHWThemes = true;
                } catch (Throwable th) {
                    while (true) {
                    }
                }
            }
            if (this.mDynamicDrawableCache != null) {
                this.mDynamicDrawableCache.onConfigurationChange(configChanges);
            }
        }
        synchronized (sCacheLock) {
            if (this.mColorDrawableCacheEx != null) {
                this.mColorDrawableCacheEx.onConfigurationChange(configChanges);
            }
            if (this.mDrawableCacheEx != null) {
                this.mDrawableCacheEx.onConfigurationChange(configChanges);
            }
        }
        if (themeChanged) {
            clearPreloadedHwThemeZipsCache();
        }
    }

    public void setPackageName(String name) {
        this.mPackageName = name;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    protected void setHwTheme(Configuration config) {
        if (config != null) {
            boolean z = true;
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Environment.getDataDirectory());
            stringBuilder.append("/themes/");
            stringBuilder.append(userId);
            String path = stringBuilder.toString();
            synchronized (HwResourcesImpl.class) {
                if (!mThemeAbsoluteDir.equals(path)) {
                    setHwThemeAbsoluteDir(path, userId);
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("updateConfiguration! the new theme absolute directory:");
                    stringBuilder2.append(mThemeAbsoluteDir);
                    Log.i(str, stringBuilder2.toString());
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("persist.deep.theme_");
            stringBuilder.append(userId);
            this.mDarkThemeType = SystemProperties.get(stringBuilder.toString());
            this.mDeepThemeType = getThemeType();
        }
        if (this.mResourcesImpl.getConfiguration().locale != null) {
            HwResources.setIsSRLocale("sr".equals(this.mResourcesImpl.getConfiguration().locale.getLanguage()));
        }
    }

    public Drawable getDrawableForDynamic(Resources res, String packageName, String iconName) throws NotFoundException {
        String imgFile = "";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dynamic_icons/");
        stringBuilder.append(packageName);
        stringBuilder.append("/");
        stringBuilder.append(iconName);
        stringBuilder.append(".png");
        imgFile = stringBuilder.toString();
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
            ((BitmapDrawable) dr).setTargetDensity(this.mResourcesImpl.getDisplayMetrics());
            this.mDynamicDrawableCache.put(key, null, dr.getConstantState(), true);
        }
        return dr;
    }

    private static synchronized void readDefaultConfig() {
        InputStream in;
        String str;
        String str2;
        synchronized (HwResourcesImpl.class) {
            if (sDefaultConfigHasRead) {
                return;
            }
            File inputFile = null;
            try {
                inputFile = HwCfgFilePolicy.getCfgFile(DEFAULT_CONFIG_NAME, 0);
            } catch (NoClassDefFoundError e) {
                Log.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
            } catch (Throwable th) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e2) {
                        Log.e(TAG, "readDefaultConfig : IOException when close stream");
                    }
                }
            }
            if (inputFile == null) {
                sDefaultConfigHasRead = true;
                return;
            }
            in = null;
            try {
                in = new FileInputStream(inputFile);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(in, null);
                XmlUtils.beginDocument(parser, TAG_CONFIG);
                sDisThemeIcon = new HashSet();
                sDisThemeBackground = new HashSet();
                sDisThemeActivityIcon = new HashSet();
                for (int type = parser.next(); type != 1; type = parser.next()) {
                    if (type == 2) {
                        String packageName = parser.getAttributeValue(0);
                        String DrawableType = null;
                        parser.next();
                        if (parser.getEventType() == 4) {
                            DrawableType = String.valueOf(parser.getText());
                        }
                        if (TYPT_ICON.equals(DrawableType)) {
                            sDisThemeIcon.add(packageName);
                        } else if (TYPT_BACKGROUND.equals(DrawableType)) {
                            sDisThemeBackground.add(packageName);
                        } else if (TYPT_ACTIVITY_ICON.equals(DrawableType)) {
                            sDisThemeActivityIcon.add(packageName);
                        }
                    }
                }
                try {
                    in.close();
                } catch (IOException e3) {
                    str = TAG;
                    str2 = "readDefaultConfig : IOException when close stream";
                    Log.e(str, str2);
                    sDefaultConfigHasRead = true;
                }
            } catch (FileNotFoundException e4) {
                Log.e(TAG, "readDefaultConfig : FileNotFoundException");
                sDisThemeIcon = null;
                sDisThemeBackground = null;
                sDisThemeActivityIcon = null;
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e5) {
                        str = TAG;
                        str2 = "readDefaultConfig : IOException when close stream";
                        Log.e(str, str2);
                        sDefaultConfigHasRead = true;
                    }
                }
            } catch (IOException | XmlPullParserException e6) {
                Log.e(TAG, "readDefaultConfig : XmlPullParserException | IOException");
                sDisThemeIcon = null;
                sDisThemeBackground = null;
                sDisThemeActivityIcon = null;
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e7) {
                        str = TAG;
                        str2 = "readDefaultConfig : IOException when close stream";
                        Log.e(str, str2);
                        sDefaultConfigHasRead = true;
                    }
                }
            }
            sDefaultConfigHasRead = true;
        }
    }

    private static synchronized void loadMultiDpiWhiteList() {
        synchronized (HwResourcesImpl.class) {
            sMultiDirHasRead = true;
            ArrayList<File> configFiles = new ArrayList();
            int i = 0;
            try {
                configFiles = HwCfgFilePolicy.getCfgFileList(FILE_MULTI_DPI_WHITELIST, 0);
            } catch (NoClassDefFoundError e) {
                Log.e(TAG, "HwCfgFilePolicy NoClassDefFoundError! ");
            }
            int size = configFiles.size();
            while (i < size) {
                setMultiDpiPkgNameFromXml((File) configFiles.get(i));
                i++;
            }
            setMultiDpiPkgNameFromXml(new File(DEFAULT_MULTI_DPI_WHITELIST));
        }
    }

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:17:0x0035, B:25:0x0045] */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            r3 = TAG;
            r4 = "loadMultiDpiWhiteList:- IOE while closing stream";
     */
    /* JADX WARNING: Missing block: B:53:0x0088, code skipped:
            if (r2 != null) goto L_0x008a;
     */
    /* JADX WARNING: Missing block: B:55:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:59:?, code skipped:
            android.util.Log.e(TAG, "loadMultiDpiWhiteList:- IOE while closing stream");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static synchronized void setMultiDpiPkgNameFromXml(File configFile) {
        String str;
        String str2;
        synchronized (HwResourcesImpl.class) {
            if (configFile == null) {
                return;
            }
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(configFile);
                XmlPullParser xmlParser = Xml.newPullParser();
                xmlParser.setInput(inputStream, null);
                for (int xmlEventType = xmlParser.next(); xmlEventType != 1; xmlEventType = xmlParser.next()) {
                    if (xmlEventType == 2) {
                        String packageName = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_PACKAGE_NAME);
                        if (packageName != null) {
                            sMultiDirPkgNames.add(packageName);
                        }
                    }
                }
                inputStream.close();
            } catch (FileNotFoundException e) {
                sMultiDirHasRead = false;
                Log.e(TAG, "loadMultiDpiWhiteList : FileNotFoundException");
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e2) {
                        str = TAG;
                        str2 = "loadMultiDpiWhiteList:- IOE while closing stream";
                        Log.e(str, str2);
                    }
                }
            } catch (XmlPullParserException e3) {
                sMultiDirHasRead = false;
                Log.e(TAG, "loadMultiDpiWhiteList : XmlPullParserException");
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e4) {
                        str = TAG;
                        str2 = "loadMultiDpiWhiteList:- IOE while closing stream";
                        Log.e(str, str2);
                    }
                }
            } catch (IOException e5) {
                sMultiDirHasRead = false;
                Log.e(TAG, "loadMultiDpiWhiteList : IOException");
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e6) {
                        str = TAG;
                        str2 = "loadMultiDpiWhiteList:- IOE while closing stream";
                        Log.e(str, str2);
                    }
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
        String imgFile = new StringBuilder();
        imgFile.append(ce.type == 1 ? ce.name : packageName);
        imgFile.append(".png");
        return imgFile.toString();
    }

    private String getIconFileName(String packageName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(packageName);
        stringBuilder.append(".png");
        return stringBuilder.toString();
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

    /* JADX WARNING: Missing block: B:40:0x00c4, code skipped:
            return r12;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Drawable getJoinBitmap(Drawable srcDraw, int backgroundId) {
        Drawable drawable = srcDraw;
        int i = backgroundId;
        synchronized (HwResourcesImpl.class) {
            if (drawable == null) {
                try {
                    Log.w(TAG, "notification icon is null!");
                    return null;
                } catch (Throwable th) {
                }
            } else {
                Bitmap temp;
                Bitmap bmpSrc = null;
                if (drawable instanceof BitmapDrawable) {
                    temp = ((BitmapDrawable) drawable).getBitmap();
                    int width = temp.getWidth();
                    int height = temp.getHeight();
                    int[] pixels = new int[(width * height)];
                    int[] pixels2 = pixels;
                    int height2 = height;
                    temp.getPixels(pixels, 0, width, 0, 0, width, height);
                    bmpSrc = Bitmap.createBitmap(pixels2, width, height2, Config.ARGB_8888);
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
                ResourcesImpl bmpBg = this.mResourcesImpl;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(NOTIFICATION_ICON_PREFIX);
                stringBuilder.append(i);
                stringBuilder.append(".png");
                temp = iconZipFileCache.getBitmapEntry(bmpBg, stringBuilder.toString());
                if (temp == null) {
                    temp = iconZipFileCache.getBitmapEntry(this.mResourcesImpl, NOTIFICATION_ICON_EXIST);
                }
                Bitmap bmpMask = iconZipFileCache.getBitmapEntry(this.mResourcesImpl, NOTIFICATION_ICON_MASK);
                Bitmap bmpBorder = iconZipFileCache.getBitmapEntry(this.mResourcesImpl, NOTIFICATION_ICON_BORDER);
                if (!(temp == null || bmpMask == null)) {
                    if (bmpBorder != null) {
                        int w = bmpSrc.getWidth();
                        int h = bmpSrc.getHeight();
                        Bitmap overlap;
                        if (w == 1 && h == 1) {
                            overlap = IconBitmapUtils.overlap2Bitmap(bmpSrc, temp);
                            Drawable bitmapDrawable = overlap == null ? null : new BitmapDrawable(overlap);
                        } else {
                            if (!(bmpMask.getWidth() == w || bmpMask.getHeight() == h)) {
                                bmpMask = IconBitmapUtils.drawSource(bmpMask, w, w);
                            }
                            if (!(bmpBorder.getWidth() == w || bmpBorder.getHeight() == h)) {
                                bmpBorder = IconBitmapUtils.drawSource(bmpBorder, w, w);
                            }
                            if (!(temp.getWidth() == w || temp.getHeight() == h)) {
                                temp = IconBitmapUtils.drawSource(temp, w, w);
                            }
                            overlap = IconBitmapUtils.composeIcon(bmpSrc, bmpMask, temp, bmpBorder, null);
                            if (overlap != null) {
                                BitmapDrawable bitmapDrawable2 = new BitmapDrawable(overlap);
                                return bitmapDrawable2;
                            }
                            Log.w(TAG, "getJoinBitmap is null!");
                            return null;
                        }
                    }
                }
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getJoinBitmap :");
                stringBuilder2.append(temp == null ? " bmpBg == null, " : " bmpBg != null, ");
                stringBuilder2.append(" id = ");
                stringBuilder2.append(i);
                stringBuilder2.append(bmpMask == null ? " bmpMask == null, " : "bmpMask != null, ");
                stringBuilder2.append(bmpBorder == null ? " bmpBorder == null, " : " bmpBorder != null");
                Log.w(str, stringBuilder2.toString());
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
        boolean z;
        synchronized (sPreloadedHwThemeZipLock) {
            if (!sPreloadedHwThemeZips.isEmpty()) {
                if (!sPreloadedHwThemeZips.contains(zipFile)) {
                    z = false;
                }
            }
            z = true;
        }
        return z;
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
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(CUSTOM_DIFF_THEME_DIR);
        stringBuilder.append("/");
        stringBuilder.append(ICONS_ZIPFILE);
        if (new File(stringBuilder.toString()).exists()) {
            return CUSTOM_DIFF_THEME_DIR;
        }
        return null;
    }

    public Bitmap getIconsfromDiffTheme(ResourcesImpl Impl, String imgFile, String packageName) {
        String iconFilePath = getDiffThemeIconPath();
        if (iconFilePath == null) {
            return null;
        }
        ZipFileCache diffIconZipFileCache = ZipFileCache.getAndCheckCachedZipFile(iconFilePath, ICONS_ZIPFILE);
        if (diffIconZipFileCache == null) {
            return null;
        }
        Bitmap bmp = diffIconZipFileCache.getBitmapEntry(Impl, imgFile);
        String str;
        StringBuilder stringBuilder;
        if (bmp != null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("icon : ");
            stringBuilder.append(imgFile);
            stringBuilder.append(" found in custom diff theme");
            Log.i(str, stringBuilder.toString());
            return bmp;
        } else if (imgFile.equals(getIconFileName(packageName))) {
            return bmp;
        } else {
            bmp = diffIconZipFileCache.getBitmapEntry(Impl, getIconFileName(packageName));
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("icon : ");
            stringBuilder.append(imgFile);
            stringBuilder.append(" package name found in custom diff theme");
            Log.i(str, stringBuilder.toString());
            return bmp;
        }
    }

    /* JADX WARNING: Missing block: B:13:0x004b, code skipped:
            android.content.res.HwResources.setIsSRLocale("sr".equals(java.util.Locale.getDefault().getLanguage()));
     */
    /* JADX WARNING: Missing block: B:14:0x005c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void preloadHwThemeZipsAndSomeIcons(int currentUserId) {
        String path = new StringBuilder();
        path.append(Environment.getDataDirectory());
        path.append("/themes/");
        path.append(currentUserId);
        setHwThemeAbsoluteDir(path.toString(), currentUserId);
        synchronized (sPreloadedHwThemeZipLock) {
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
        clearPreloadedHwThemeZipsCache();
    }

    private void clearHwThemeZipsAndIconsCache() {
        if (sBgList != null) {
            int listSize = sBgList.size();
            for (int i = 0; i < listSize; i++) {
                ((Bitmap) sBgList.get(i)).recycle();
            }
            sBgList.clear();
        }
        if (!(sBmpBorder == null || sBmpBorder.isRecycled())) {
            sBmpBorder.recycle();
            sBmpBorder = null;
        }
        if (!(sBmpMask == null || sBmpMask.isRecycled())) {
            sBmpMask.recycle();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sBmpMask  recycle  sBmpMask = ");
            stringBuilder.append(sBmpMask);
            Log.w(str, stringBuilder.toString());
            sBmpMask = null;
            sMaskSizeWithoutEdge = -1;
        }
        ZipFileCache.clear();
        sUseAvgColor = -1;
    }

    private void clearPreloadedHwThemeZipsCache() {
        synchronized (sPreloadedHwThemeZipLock) {
            sPreloadedHwThemeZips.clear();
        }
    }

    private void inflateColorInfoList(XmlPullParser parser, String packageName, boolean isZipFile) throws XmlPullParserException, IOException {
        int innerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (parser.getDepth() <= innerDepth && type == 3) {
                return;
            }
            if (type == 2) {
                String lableName = parser.getName();
                if (lableName.equals("color") || lableName.equals("drawable")) {
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
        String str;
        StringBuilder stringBuilder;
        if (value.startsWith("#")) {
            value = value.substring(1);
            if (value.length() > 8) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getColorValueFromStr ");
                stringBuilder.append(String.format("Color value '%s' is too long. Format is either#AARRGGBB, #RRGGBB, #RGB, or #ARGB", new Object[]{value}));
                Log.e(str, stringBuilder.toString());
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
                char charAt2 = value.charAt(0);
                color[1] = charAt2;
                color[0] = charAt2;
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
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("FF");
                stringBuilder2.append(value);
                value = stringBuilder2.toString();
            }
            return (int) Long.parseLong(value, 16);
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("getColorValueFromStr ");
        stringBuilder.append(String.format("Color value '%s' must start with #", new Object[]{value}));
        Log.e(str, stringBuilder.toString());
        return 0;
    }

    /* JADX WARNING: Missing block: B:9:0x000c, code skipped:
            r0 = null;
            r1 = javax.xml.parsers.DocumentBuilderFactory.newInstance();
     */
    /* JADX WARNING: Missing block: B:12:0x001e, code skipped:
            r0 = r1.newDocumentBuilder().parse(new java.io.File(r11, r12));
     */
    /* JADX WARNING: Missing block: B:14:0x0021, code skipped:
            r3 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("getLayoutXML Exception filename =");
            r4.append(r12);
            android.util.Log.e(r3, r4.toString());
     */
    /* JADX WARNING: Missing block: B:16:0x0039, code skipped:
            r3 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("getLayoutXML IOException filename = ");
            r4.append(r12);
            android.util.Log.e(r3, r4.toString());
     */
    /* JADX WARNING: Missing block: B:18:0x0051, code skipped:
            r3 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("getLayoutXML SAXException filename =");
            r4.append(r12);
            android.util.Log.e(r3, r4.toString());
     */
    /* JADX WARNING: Missing block: B:20:0x0069, code skipped:
            r3 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("getLayoutXML ParserConfigurationException filename =");
            r4.append(r12);
            android.util.Log.e(r3, r4.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isCurrentHwTheme(String dirpath, String filename) {
        synchronized (HwResourcesImpl.class) {
            if (sThemeDescripHasRead) {
                boolean z = sIsHWThemes;
                return z;
            }
        }
        Document document = document;
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
        this.mCurrentDeepTheme = this.mResourcesImpl.getAssets().getDeepType();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("persist.deep.theme_");
        stringBuilder.append(mCurrentUserId);
        this.mDarkThemeType = SystemProperties.get(stringBuilder.toString());
        if (isDeepDarkTheme() || HwThemeManager.isHonorProduct() || HwThemeManager.isNovaProduct()) {
            this.mDeepThemeType = getThemeType();
        }
    }

    private boolean checkWhiteDeepThemeAppList(String resDir) {
        int length = DEEPTHEME_WHILTLIST_ADD.length;
        int i = 0;
        while (i < length) {
            if (resDir != null && resDir.equals(DEEPTHEME_WHILTLIST_ADD[i])) {
                return true;
            }
            i++;
        }
        return false;
    }

    @SuppressLint({"AvoidInHardConnectInString"})
    private Drawable getAppDrawableFromAsset(TypedValue value, Resources res, String packageName, String file, String dir) {
        StringBuilder drawableSb = new StringBuilder();
        drawableSb.append(this.mPackageName);
        drawableSb.append(":");
        drawableSb.append(dir);
        drawableSb.append("/");
        drawableSb.append(file);
        String drawablePath = drawableSb.toString();
        if (DEFAULT_NO_DRAWABLE_FLAG.equals((String) mCacheAssetsList.get(drawablePath))) {
            return null;
        }
        Drawable dr = this.mResourcesImpl.getAssets();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dir);
        stringBuilder.append(File.separator);
        stringBuilder.append(file);
        dr = AssetsFileCache.getDrawableEntry(dr, res, value, stringBuilder.toString(), null);
        if (dr == null && !file.contains(DRAWABLE_FHD)) {
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
        sb.append(":");
        sb.append(dir);
        sb.append("/");
        sb.append(key);
        String fullPath = sb.toString();
        if (DEFAULT_NO_DRAWABLE_FLAG.equals((String) mCacheAssetsList.get(fullPath))) {
            return null;
        }
        Drawable dr = this.mResourcesImpl.getAssets();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dir);
        stringBuilder.append(File.separator);
        stringBuilder.append(key);
        dr = AssetsFileCache.getDrawableEntry(dr, res, value, stringBuilder.toString(), null);
        if (dr == null && !file.contains(DRAWABLE_FHD)) {
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
        if (isDeepDarkTheme() && (this.mCurrentDeepTheme & 1) == 1) {
            return DARK;
        }
        if (HwThemeManager.isHonorProduct() && (this.mCurrentDeepTheme & 16) == 16) {
            return HONOR;
        }
        if (HwThemeManager.isNovaProduct() && (this.mCurrentDeepTheme & 256) == 256) {
            return NOVA;
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
        if ((!packageName.equals(FRAMEWORK_RES) && !packageName.equals(FRAMEWORK_RES_EXT)) || themeXml.contains(packageName)) {
            String tempColorPath = new StringBuilder();
            tempColorPath.append(dir);
            tempColorPath.append(File.separator);
            tempColorPath.append(themeXml);
            parserColorInfoList(packageName, AssetsFileCache.getInputStreamEntry(this.mResourcesImpl.getAssets(), tempColorPath.toString()), false);
        }
    }

    protected Bundle getMultidpiInfo(String packageName) {
        return Resources.getPreMultidpiInfo(packageName);
    }

    public DisplayMetrics hwGetDisplayMetrics() {
        return this.mResourcesImpl.getDisplayMetrics();
    }
}
