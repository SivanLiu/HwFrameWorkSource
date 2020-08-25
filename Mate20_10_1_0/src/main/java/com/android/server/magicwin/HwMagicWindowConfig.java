package com.android.server.magicwin;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.HwMwUtils;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import com.android.server.magicwin.HwMagicWindowConfig;
import com.android.server.rms.iaware.cpu.CPUCustBaseConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HwMagicWindowConfig {
    private static final String ACTIVITY_DEFAULT_FULLSCREEN = "defaultFullScreen";
    private static final String ACTIVITY_DESTROY_WHEN_REPLACE_ON_RIGHT = "destroyWhenReplacedOnRight";
    private static final String ACTIVITY_FROM = "from";
    private static final String ACTIVITY_IS_SUPPORT_TASK_SPLIT_SCREEN = "isSupportTaskSplitScreen";
    private static final String ACTIVITY_NAME = "name";
    private static final String ACTIVITY_PAIRS = "activityPairs";
    private static final String ACTIVITY_PARAM = "Activities";
    private static final String ACTIVITY_TO = "to";
    private static final String BODY_DUAL_ACTS = "defaultDualActivities";
    private static final String BODY_FULLSCREEN_VIDEO = "supportVideoFullscreen";
    private static final String CLIENT_NAME = "client_name";
    private static final String CLIENT_REQUEST = "client_request";
    /* access modifiers changed from: private */
    public static final Rect DEFAULT_LEFT_BOUNDS = new Rect(20, 5, 1275, 1595);
    private static final Rect DEFAULT_MIDDLE_BOUNDS = new Rect(657, 5, 1912, 1595);
    /* access modifiers changed from: private */
    public static final Rect DEFAULT_RIGHT_BOUNDS = new Rect(1285, 5, 2540, 1595);
    private static final Rect DEFAULT_SINGLE_BOUNDS = new Rect(657, 5, 1912, 1595);
    private static final int DRAG_MID_MODE = 0;
    private static final String IS_RELAUNCH = "isRelaunchwhenResize";
    private static final String IS_SUPPORT_APP_TASK_SPLIT_SCREEN = "supportAppTaskSplitScreen";
    private static final String MAIN_PAGE = "mainPage";
    private static final String MAIN_PAGE_SET = "mainPages";
    private static final int MIDDLE_GAP_WIDTH_DP = 2;
    private static final int NUM_BOUNDS = 2;
    private static final int PARSER_STRING_TO_INT_ERROR = -1;
    private static final String RELATED_PAGE = "relatedPage";
    private static final int SPLIT_MIDDLE_GAP_WIDTH_DP = 8;
    private static final int SPLIT_MIDDLE_GAP_WIDTH_DP_PAD = 6;
    private static final int STATUS_LOGIN = 1;
    private static final int STATUS_LOGOFF = 2;
    private static final String TAG = "HwMagicWindowConfig";
    private static final String TRANS_ACTIVITIES = "transActivities";
    private static final String UX_DEVICE_PARAM = "device";
    private static final String UX_FOLD_DEVICE = "FOLD";
    private static final String UX_IS_DRAGABLE = "isDraggable";
    private static final String UX_IS_SCALED = "supportRotationUxCompat";
    private static final String UX_IS_SHOW_STATUSBAR = "showStatusBar";
    private static final String UX_PAD_DEVICE = "PAD";
    private static final String UX_PARAM = "UX";
    private static final String UX_RATIO_PARAM = "ratio";
    private static final String UX_RATIO_PARAM_SPLIT = "\\|";
    private static final String UX_SPLIT_BAR_BG_COLOR = "splitBarBgColor";
    private static final String UX_SPLIT_LINE_BG_COLOR = "splitLineBgColor";
    private static final String UX_USE_SYSTEM_ACTIVITY_ANIMATION = "useSystemActivityAnimation";
    private static final String UX_WINDOWS_RATIO = "windowsRatio";
    private static final String WECHAT_PACKAGE_NAME = "com.tencent.mm";
    private static final String WINDOW_SWITCH_TYPE = "mode";
    /* access modifiers changed from: private */
    public static Context mContext = null;
    private final int INDEX_B;
    private final int INDEX_CNT;
    private final int INDEX_L;
    private final int INDEX_R;
    private final int INDEX_T;
    private Map<String, List<Rect>> mAppDragBoundsConfigs = new HashMap();
    private HwMagicWindowConfigLoader mCfgLoader = null;
    private Map<Integer, List<Rect>> mDragBounds = null;
    private Map<String, HomeConfig> mHomeConfigs = new HashMap();
    private Map<String, HostRecognizeConfig> mHostRecognizeConfigs = new HashMap();
    /* access modifiers changed from: private */
    public boolean mIsCurrentRtl;
    /* access modifiers changed from: private */
    public Set<Rect> mMasterBoounds;
    private Set<Rect> mMidBoounds;
    private Map<String, OpenCapAppConfig> mOpenCapAppConfigs = new ConcurrentHashMap();
    private Map<String, PackageConfig> mPackageConfigs = new HashMap();
    private Map<String, LoginStatusConfig> mPkgLoginConfigs = new HashMap();
    private float mRatio = 1.0f;
    private Map<String, SettingConfig> mSettingConfigs = new ConcurrentHashMap();
    /* access modifiers changed from: private */
    public Set<Rect> mSlaveBoounds;
    /* access modifiers changed from: private */
    public SystemConfig mSystemConfig;

    public HwMagicWindowConfig(Context cxt) {
        boolean z = false;
        this.INDEX_L = 0;
        this.INDEX_T = 1;
        this.INDEX_R = 2;
        this.INDEX_B = 3;
        this.INDEX_CNT = 4;
        this.mMidBoounds = new HashSet();
        this.mMasterBoounds = new HashSet();
        this.mSlaveBoounds = new HashSet();
        mContext = cxt;
        this.mIsCurrentRtl = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1 ? true : z;
        this.mCfgLoader = new HwMagicWindowConfigLoader(mContext, ActivityManager.getCurrentUser());
        this.mCfgLoader.loadSystem(this);
        this.mCfgLoader.loadPackage(this);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:21:0x00b0, code lost:
        if (r8.getCount() == 0) goto L_0x00b2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x00b2, code lost:
        delAppAdapterInfo(r14);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x00b5, code lost:
        r8.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x00d8, code lost:
        if (r8.getCount() == 0) goto L_0x00b2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x00e9, code lost:
        if (r8.getCount() == 0) goto L_0x00b2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:?, code lost:
        return;
     */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x00ac  */
    /* JADX WARNING: Removed duplicated region for block: B:45:? A[ORIG_RETURN, RETURN, SYNTHETIC] */
    public void loadAppAdapterInfo(String packageName) {
        Slog.i(TAG, "loadAppAdapterInfo");
        Uri uri = Uri.parse("content://com.huawei.easygo.easygoprovider/v_function");
        Cursor cursor = null;
        try {
            Context context = mContext.createPackageContextAsUser("com.huawei.systemserver", 0, UserHandle.of(ActivityManager.getCurrentUser()));
            if (context != null) {
                if ("*".equals(packageName)) {
                    cursor = context.getContentResolver().query(uri, new String[]{CLIENT_REQUEST, CLIENT_NAME}, "server_data_schema=\"package:magicwin\"", null, null);
                } else {
                    cursor = context.getContentResolver().query(uri, new String[]{CLIENT_REQUEST, CLIENT_NAME}, "server_data_schema=\"package:magicwin\" and client_name=?", new String[]{packageName}, null);
                }
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String clientPackageName = cursor.getString(cursor.getColumnIndex(CLIENT_NAME));
                        String clientRequest = cursor.getString(cursor.getColumnIndex(CLIENT_REQUEST));
                        Slog.i(TAG, "loadAppAdapter PackageName=" + clientPackageName + "Request=" + clientRequest);
                        parseRequest(clientPackageName, clientRequest);
                    } while (cursor.moveToNext());
                    if (cursor != null) {
                    }
                } else if (cursor != null) {
                }
            } else if (0 != 0) {
                if (cursor.getCount() == 0) {
                    delAppAdapterInfo(packageName);
                }
                cursor.close();
            }
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "register EasyGo no package context");
            if (0 == 0) {
            }
        } catch (IllegalStateException e2) {
            Slog.e(TAG, "loadAppAdapter failed " + e2);
            if (0 == 0) {
            }
        } catch (Throwable th) {
            if (0 != 0) {
                if (cursor.getCount() == 0) {
                    delAppAdapterInfo(packageName);
                }
                cursor.close();
            }
            throw th;
        }
    }

    private void parseRequest(String pkg, String clientRequest) {
        if (pkg != null && clientRequest != null) {
            if (!this.mPackageConfigs.containsKey(pkg) || this.mPackageConfigs.get(pkg).getWindowMode() != -1) {
                OpenCapAppConfig openAppConfig = new OpenCapAppConfig(pkg, clientRequest);
                addOpenCapAppConfig(pkg, openAppConfig);
                openAppConfig.parseAppConfig();
                if (!this.mSettingConfigs.containsKey(pkg)) {
                    String defaultSettingInWhiteList = "true";
                    if (this.mPackageConfigs.containsKey(pkg)) {
                        if (!this.mPackageConfigs.get(pkg).isDefaultSetting()) {
                            defaultSettingInWhiteList = "false";
                        }
                        createSetting(pkg, defaultSettingInWhiteList, "false", String.valueOf(0));
                        return;
                    }
                    if (HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
                        defaultSettingInWhiteList = "false";
                    }
                    createSetting(pkg, defaultSettingInWhiteList, "false", String.valueOf(0));
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void delAppAdapterInfo(String packageName) {
        Slog.i(TAG, "delete packageName=" + packageName + " AppAdapterInfo");
        if (!isEmpty(packageName)) {
            if (this.mOpenCapAppConfigs.containsKey(packageName) && (!this.mPackageConfigs.containsKey(packageName) || this.mPackageConfigs.get(packageName).getWindowMode() == -2)) {
                this.mSettingConfigs.remove(packageName);
            }
            this.mOpenCapAppConfigs.remove(packageName);
        }
    }

    public Set<String> onCloudUpdate() {
        Map<String, PackageConfig> tmpPackageConfigs = (Map) ((HashMap) this.mPackageConfigs).clone();
        this.mPackageConfigs.clear();
        this.mHomeConfigs.clear();
        this.mCfgLoader.loadPackage(this);
        syncSettingsWithWhiteList();
        tmpPackageConfigs.putAll(this.mPackageConfigs);
        return tmpPackageConfigs.keySet();
    }

    public void onUserSwitch() {
        this.mCfgLoader.initSettingsDirForUser(ActivityManager.getCurrentUser());
        this.mSettingConfigs.clear();
        this.mAppDragBoundsConfigs.clear();
        loadUserSettingsData();
        syncSettingsWithWhiteList();
    }

    public void onAppSwitchChanged(String pkg, boolean isMagicWinEnabled) {
        Slog.d(TAG, "onAppSwitchChanged, pkg = " + pkg + ", hwMagicWinEnabled = " + isMagicWinEnabled);
        this.mSettingConfigs.computeIfPresent(pkg, new BiFunction(isMagicWinEnabled) {
            /* class com.android.server.magicwin.$$Lambda$HwMagicWindowConfig$YVN7Wq1oDpvdYLdVAhS_Odip2Qw */
            private final /* synthetic */ boolean f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return ((SettingConfig) obj2).setMagicWinEnabled(this.f$0);
            }
        });
        this.mCfgLoader.writeSetting(this);
    }

    public void onAppDialogShown(String pkg, boolean isDialogShown) {
        Slog.d(TAG, "onAppSwitchChanged, pkg = " + pkg + ", hwDialogShown = " + isDialogShown);
        this.mSettingConfigs.computeIfPresent(pkg, new BiFunction(isDialogShown) {
            /* class com.android.server.magicwin.$$Lambda$HwMagicWindowConfig$4U6qo86ZhtCwpDLqSmb_muU4hA */
            private final /* synthetic */ boolean f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return ((SettingConfig) obj2).setHwDialogShown(this.f$0);
            }
        });
        this.mCfgLoader.writeSetting(this);
    }

    private void loadUserSettingsData() {
        this.mCfgLoader.readSetting(this);
    }

    private void syncSettingsWithWhiteList() {
        if (getIsSupportOpenCap()) {
            syncOpenCapAppConfig();
        } else {
            syncWhiteListAppConfig();
        }
    }

    public void writeSetting() {
        this.mCfgLoader.writeSetting(this);
    }

    public boolean getIsSupportOpenCap() {
        SystemConfig systemConfig = this.mSystemConfig;
        if (systemConfig != null) {
            return systemConfig.isSystemSupport(3);
        }
        return false;
    }

    public int getAppDragMode(String pkgName) {
        SettingConfig setCfg;
        if (!isEmpty(pkgName) && (setCfg = this.mSettingConfigs.get(pkgName)) != null) {
            return setCfg.getDragMode();
        }
        return 0;
    }

    private void syncWhiteListAppConfig() {
        this.mSettingConfigs.keySet().retainAll(this.mPackageConfigs.keySet());
        this.mPackageConfigs.forEach(new BiConsumer() {
            /* class com.android.server.magicwin.$$Lambda$HwMagicWindowConfig$rSmAMew5cqlKt6Fq70F50hxmpoo */

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                HwMagicWindowConfig.this.lambda$syncWhiteListAppConfig$2$HwMagicWindowConfig((String) obj, (PackageConfig) obj2);
            }
        });
    }

    public /* synthetic */ void lambda$syncWhiteListAppConfig$2$HwMagicWindowConfig(String key, PackageConfig pkgCfg) {
        this.mSettingConfigs.putIfAbsent(key, new SettingConfig(key, pkgCfg.isDefaultSetting(), false, 0));
    }

    private void syncOpenCapAppConfig() {
        Set<String> results = new HashSet<>();
        Set<String> openCapSet = this.mOpenCapAppConfigs.keySet();
        Set<String> pkgSet = this.mPackageConfigs.keySet();
        results.addAll(openCapSet);
        results.addAll(pkgSet);
        this.mSettingConfigs.keySet().retainAll(results);
        this.mPackageConfigs.forEach(new BiConsumer() {
            /* class com.android.server.magicwin.$$Lambda$HwMagicWindowConfig$35MROxuwmMs4nyUGQ_MVjf4ajRA */

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                HwMagicWindowConfig.this.lambda$syncOpenCapAppConfig$3$HwMagicWindowConfig((String) obj, (PackageConfig) obj2);
            }
        });
        this.mOpenCapAppConfigs.forEach(new BiConsumer() {
            /* class com.android.server.magicwin.$$Lambda$HwMagicWindowConfig$mRiRhiVF4bAletLAUt78wyQMBDk */

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                HwMagicWindowConfig.this.lambda$syncOpenCapAppConfig$4$HwMagicWindowConfig((String) obj, (OpenCapAppConfig) obj2);
            }
        });
        this.mSettingConfigs.keySet().removeAll(((Map) this.mPackageConfigs.entrySet().stream().filter(new Predicate(openCapSet) {
            /* class com.android.server.magicwin.$$Lambda$HwMagicWindowConfig$kgrHhnh_cC82K93IZg5Q5wbpdc */
            private final /* synthetic */ Set f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return HwMagicWindowConfig.lambda$syncOpenCapAppConfig$5(this.f$0, (Map.Entry) obj);
            }
        }).collect(Collectors.toMap($$Lambda$CSz_ibwXhtkKNl72Q8tR5oBgkWk.INSTANCE, $$Lambda$SkNQK6qWCAnTxcFyS9lnFlzNXDA.INSTANCE))).keySet());
    }

    public /* synthetic */ void lambda$syncOpenCapAppConfig$3$HwMagicWindowConfig(String key, PackageConfig pkgCfg) {
        this.mSettingConfigs.putIfAbsent(key, new SettingConfig(key, pkgCfg.isDefaultSetting(), false, 0));
    }

    public /* synthetic */ void lambda$syncOpenCapAppConfig$4$HwMagicWindowConfig(String key, OpenCapAppConfig openCfg) {
        this.mSettingConfigs.putIfAbsent(key, new SettingConfig(key, !HwMwUtils.IS_FOLD_SCREEN_DEVICE, false, 0));
    }

    static /* synthetic */ boolean lambda$syncOpenCapAppConfig$5(Set openCapSet, Map.Entry map) {
        return ((PackageConfig) map.getValue()).getWindowMode() == -1 || (((PackageConfig) map.getValue()).getWindowMode() == -2 && !openCapSet.contains(map.getKey()));
    }

    public boolean createPackage(PackageConfig packageConfig) {
        if (packageConfig == null) {
            return false;
        }
        this.mPackageConfigs.put(packageConfig.mPackageName, packageConfig);
        return true;
    }

    public boolean createHome(String pkg, String[] homes) {
        if (isEmpty(pkg) || isEmpty(homes)) {
            return false;
        }
        this.mHomeConfigs.put(pkg, new HomeConfig(pkg, homes));
        return true;
    }

    private boolean addOpenCapAppConfig(String pkg, OpenCapAppConfig openAppConfig) {
        if (isEmpty(pkg) || openAppConfig == null) {
            return false;
        }
        this.mOpenCapAppConfigs.remove(pkg);
        this.mOpenCapAppConfigs.put(pkg, openAppConfig);
        return true;
    }

    public boolean createHost(String pkg, String home) {
        if (isEmpty(pkg) || isEmpty(home)) {
            return false;
        }
        if (this.mHostRecognizeConfigs.containsKey(pkg)) {
            this.mHostRecognizeConfigs.remove(pkg);
        }
        this.mHostRecognizeConfigs.put(pkg, new HostRecognizeConfig(pkg, home));
        return true;
    }

    public boolean isNeedDect(String pkg) {
        if (getWindowMode(pkg) > 0 && !this.mHomeConfigs.containsKey(pkg)) {
            return true;
        }
        return false;
    }

    public boolean createSystem(SystemConfig systemConfig) {
        if (systemConfig == null) {
            return false;
        }
        this.mSystemConfig = systemConfig;
        updateSystemBoundSize();
        return true;
    }

    public boolean createSetting(String pkg, String hwMagicWinEnabled, String hwDialogShown, String hwDragMode) {
        Slog.d(TAG, "createSetting, pkg = " + pkg + ", hwMagicWinEnabled = " + hwMagicWinEnabled + ", hwDialogShown = " + hwDialogShown + ", hwDragMode = " + hwDragMode);
        if (isEmpty(pkg) || isEmpty(hwMagicWinEnabled) || isEmpty(hwDialogShown)) {
            return false;
        }
        this.mSettingConfigs.put(pkg, new SettingConfig(pkg, hwMagicWinEnabled, hwDialogShown, hwDragMode));
        return true;
    }

    public void removeSettingConfig(String pkgName) {
        if (this.mSettingConfigs.containsKey(pkgName)) {
            this.mSettingConfigs.remove(pkgName);
        }
    }

    /* access modifiers changed from: private */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private static boolean isEmpty(String[] strs) {
        return strs == null || strs.length <= 0;
    }

    /* access modifiers changed from: private */
    public static boolean isEmpty(List<String> list) {
        return list == null || list.isEmpty();
    }

    public int getWindowMode(String pkg) {
        if (isEmpty(pkg)) {
            return -1;
        }
        PackageConfig target = this.mPackageConfigs.get(pkg);
        OpenCapAppConfig openCapAppConfig = this.mOpenCapAppConfigs.get(pkg);
        if (target != null) {
            int localMode = target.getWindowMode();
            if (localMode == 0 || localMode == 1 || localMode == 2 || localMode == 3) {
                return localMode;
            }
            if (localMode != -2 || openCapAppConfig == null) {
                return -1;
            }
            return openCapAppConfig.getWindowMode();
        } else if (openCapAppConfig == null) {
            return -1;
        } else {
            return openCapAppConfig.getWindowMode();
        }
    }

    public boolean needRelaunch(String pkg) {
        PackageConfig target;
        if (!isEmpty(pkg) && (target = this.mPackageConfigs.get(pkg)) != null && target.needRelaunch()) {
            return true;
        }
        return false;
    }

    public boolean isSpecPairActivities(String pkg, String focus, String target) {
        OpenCapAppConfig openCapAppConfig = getOpenCapAppConfig(pkg);
        return openCapAppConfig != null && openCapAppConfig.isSpecPairActivities(focus, target);
    }

    public String getRelateActivity(String pkg) {
        OpenCapAppConfig openCapAppConfig = getOpenCapAppConfig(pkg);
        return openCapAppConfig == null ? "" : openCapAppConfig.getRelateActivity();
    }

    public List<String> getMainActivity(String pkg) {
        OpenCapAppConfig openCapAppConfig = getOpenCapAppConfig(pkg);
        return openCapAppConfig == null ? new ArrayList() : openCapAppConfig.getMainActivity();
    }

    public boolean isSpecTransActivity(String pkg, String curActivity) {
        OpenCapAppConfig target = getOpenCapAppConfig(pkg, curActivity);
        return target != null && target.isSpecTransActivity(curActivity);
    }

    public boolean isDefaultFullscreenActivity(String pkg, String curActivity) {
        OpenCapAppConfig target = getOpenCapAppConfig(pkg, curActivity);
        return target != null && target.isDefaultFullscreenActivity(curActivity);
    }

    public boolean isNeedStartByNewTaskActivity(String pkg, String curActivity) {
        OpenCapAppConfig target = getOpenCapAppConfig(pkg, curActivity);
        return target != null && target.isNeedStartByNewTaskActivity(curActivity);
    }

    public boolean isNeedDestroyWhenReplaceOnRight(String pkg, String curActivity) {
        OpenCapAppConfig target = getOpenCapAppConfig(pkg, curActivity);
        return target != null && target.isNeedDestroyWhenReplaceOnRight(curActivity);
    }

    public boolean isReLaunchWhenResize(String pkg) {
        OpenCapAppConfig target = getOpenCapAppConfig(pkg);
        return target != null && target.isReLaunchWhenResize();
    }

    public boolean isSupportAppTaskSplitScreen(String pkg) {
        OpenCapAppConfig target = getOpenCapAppConfig(pkg);
        return target != null && target.isSupportAppTaskSplitScreen() && getHwMagicWinEnabled(pkg);
    }

    public boolean isShowStatusBar(String pkgName) {
        OpenCapAppConfig target = getOpenCapAppConfig(pkgName);
        return target != null && target.isShowStatusBar();
    }

    public boolean isUsingSystemActivityAnimation(String pkgName) {
        OpenCapAppConfig target = getOpenCapAppConfig(pkgName);
        return target == null || target.isUsingSystemActivityAnimation();
    }

    public int getSplitLineBgColor(String pkgName) {
        OpenCapAppConfig openCapAppConfig = getOpenCapAppConfig(pkgName);
        if (openCapAppConfig == null) {
            return -1;
        }
        return openCapAppConfig.getSplitLineBgColor();
    }

    private OpenCapAppConfig getOpenCapAppConfig(String pkgName) {
        if (isEmpty(pkgName)) {
            return null;
        }
        return this.mOpenCapAppConfigs.get(pkgName);
    }

    private OpenCapAppConfig getOpenCapAppConfig(String pkgName, String curActivity) {
        if (isEmpty(pkgName) || isEmpty(curActivity)) {
            return null;
        }
        return this.mOpenCapAppConfigs.get(pkgName);
    }

    public int getSplitBarBgColor(String pkgName) {
        OpenCapAppConfig openCapAppConfig = getOpenCapAppConfig(pkgName);
        if (openCapAppConfig == null) {
            return -1;
        }
        return openCapAppConfig.getSplitBarBgColor();
    }

    public boolean isPkgSupport(String pkg, int type) {
        PackageConfig target = this.mPackageConfigs.get(pkg);
        return target != null && target.isPkgSupport(type);
    }

    public boolean isVideoFullscreen(String pkg) {
        OpenCapAppConfig easyGoCfg = getOpenCapAppConfig(pkg);
        if (easyGoCfg != null && easyGoCfg.isVideoFullscreenExist()) {
            return easyGoCfg.isVideoFullscreen();
        }
        PackageConfig localCfg = this.mPackageConfigs.get(pkg);
        if (localCfg != null) {
            return localCfg.isVideoFullscreen();
        }
        return true;
    }

    public String[] getHomes(String pkg) {
        if (isEmpty(pkg)) {
            return null;
        }
        HomeConfig homeTarget = this.mHomeConfigs.get(pkg);
        if (homeTarget != null) {
            return homeTarget.getHomes();
        }
        HostRecognizeConfig hostTarget = this.mHostRecognizeConfigs.get(pkg);
        if (hostTarget == null) {
            return null;
        }
        return new String[]{hostTarget.getHome()};
    }

    public int getBoundPosition(Rect bounds, int defaultPos) {
        if (this.mMasterBoounds.contains(bounds)) {
            return 1;
        }
        if (this.mSlaveBoounds.contains(bounds)) {
            return 2;
        }
        if (this.mMidBoounds.contains(bounds)) {
            return 3;
        }
        return defaultPos;
    }

    public String getJudgeHost(String pkg) {
        HostRecognizeConfig hostTarget;
        if (!isEmpty(pkg) && (hostTarget = this.mHostRecognizeConfigs.get(pkg)) != null) {
            return hostTarget.getHome();
        }
        return null;
    }

    public void setLoginStatus(String pkg, int status) {
        Slog.i(TAG, "setLoginStatus : status " + status + " pkg " + pkg);
        if (status == 1 || status == 2) {
            LoginStatusConfig config = this.mPkgLoginConfigs.get(pkg);
            if (config != null) {
                config.setStatus(status);
                return;
            }
            LoginStatusConfig newconfig = new LoginStatusConfig();
            newconfig.setStatus(status);
            this.mPkgLoginConfigs.put(pkg, newconfig);
        }
    }

    public void removeReportLoginStatus(String pkg) {
        if (!isEmpty(pkg) && this.mPkgLoginConfigs.containsKey(pkg)) {
            this.mPkgLoginConfigs.remove(pkg);
            Slog.i(TAG, "remove status pkg " + pkg);
        }
    }

    public boolean isInLoginStatus(String pkg) {
        LoginStatusConfig config;
        if (isEmpty(pkg) || !this.mPkgLoginConfigs.containsKey(pkg) || (config = this.mPkgLoginConfigs.get(pkg)) == null || config.getStatus() != 1) {
            return false;
        }
        return true;
    }

    public Rect getBounds(int position, String pkgName) {
        return getBounds(position, isScaled(pkgName), isDragable(pkgName), pkgName);
    }

    private Rect[] getOrigDragRect(String pkgName) {
        if (!isDragable(pkgName)) {
            return new Rect[]{DEFAULT_LEFT_BOUNDS, DEFAULT_RIGHT_BOUNDS};
        } else if (this.mAppDragBoundsConfigs.containsKey(pkgName)) {
            List<Rect> listRects = this.mAppDragBoundsConfigs.get(pkgName);
            return new Rect[]{listRects.get(0), listRects.get(1)};
        } else {
            return new Rect[]{parseStrToRect(this.mSystemConfig.mLeftDragleBounds, false, DEFAULT_SINGLE_BOUNDS), parseStrToRect(this.mSystemConfig.mRightDragleBounds, false, DEFAULT_SINGLE_BOUNDS)};
        }
    }

    public void updateAppBoundsFromMode(Map<Integer, List<Rect>> bounds) {
        this.mDragBounds = bounds;
        updateBoundsSet();
        this.mSettingConfigs.forEach(new BiConsumer(bounds) {
            /* class com.android.server.magicwin.$$Lambda$HwMagicWindowConfig$NdXkV3elytJZcQWdnHC703Qetk */
            private final /* synthetic */ Map f$1;

            {
                this.f$1 = r2;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                HwMagicWindowConfig.this.lambda$updateAppBoundsFromMode$6$HwMagicWindowConfig(this.f$1, (String) obj, (SettingConfig) obj2);
            }
        });
    }

    public /* synthetic */ void lambda$updateAppBoundsFromMode$6$HwMagicWindowConfig(Map bounds, String pkg, SettingConfig cfg) {
        this.mAppDragBoundsConfigs.put(pkg, (List) bounds.getOrDefault(Integer.valueOf(cfg.getDragMode()), (List) bounds.get(0)));
    }

    public void updateDragModeForLocaleChange() {
        this.mAppDragBoundsConfigs.clear();
        this.mSettingConfigs.forEach($$Lambda$HwMagicWindowConfig$fWLP35WXHnWgAWz3QlRkEB4p5GA.INSTANCE);
    }

    static /* synthetic */ void lambda$updateDragModeForLocaleChange$7(String pkg, SettingConfig cfg) {
        if (cfg.getDragMode() == 1) {
            cfg.setDragMode(2);
        } else if (cfg.getDragMode() == 2) {
            cfg.setDragMode(1);
        }
    }

    public void updateAppDragBounds(String pkgName, Rect leftBounds, Rect rightBounds, int dragMode) {
        if (!isEmpty(pkgName) && leftBounds != null && rightBounds != null) {
            List<Rect> listRect = new ArrayList<>();
            listRect.add(leftBounds);
            listRect.add(rightBounds);
            this.mAppDragBoundsConfigs.put(pkgName, listRect);
            this.mSettingConfigs.computeIfPresent(pkgName, new BiFunction(dragMode) {
                /* class com.android.server.magicwin.$$Lambda$HwMagicWindowConfig$oWcxszZSrSLjQ9viDd85Stj05M */
                private final /* synthetic */ int f$0;

                {
                    this.f$0 = r1;
                }

                @Override // java.util.function.BiFunction
                public final Object apply(Object obj, Object obj2) {
                    return ((SettingConfig) obj2).setDragMode(this.f$0);
                }
            });
        }
    }

    public Rect[] adjustBoundsForResize(Rect leftBound, Rect rightBound) {
        if (leftBound == null || rightBound == null) {
            return null;
        }
        Rect newLeftBounds = new Rect();
        int i = leftBound.left;
        SystemConfig systemConfig = this.mSystemConfig;
        newLeftBounds.left = i + systemConfig.dp2px(systemConfig.mLeftPadding);
        int i2 = leftBound.top;
        SystemConfig systemConfig2 = this.mSystemConfig;
        newLeftBounds.top = i2 + systemConfig2.dp2px(systemConfig2.mTopPadding);
        newLeftBounds.right = leftBound.right;
        int i3 = leftBound.bottom;
        SystemConfig systemConfig3 = this.mSystemConfig;
        newLeftBounds.bottom = i3 - systemConfig3.dp2px(systemConfig3.mBottomPadding);
        Rect newRightBounds = new Rect();
        newRightBounds.left = rightBound.left;
        int i4 = rightBound.top;
        SystemConfig systemConfig4 = this.mSystemConfig;
        newRightBounds.top = i4 + systemConfig4.dp2px(systemConfig4.mTopPadding);
        int i5 = rightBound.right;
        SystemConfig systemConfig5 = this.mSystemConfig;
        newRightBounds.right = i5 - systemConfig5.dp2px(systemConfig5.mRightPadding);
        int i6 = rightBound.bottom;
        SystemConfig systemConfig6 = this.mSystemConfig;
        newRightBounds.bottom = i6 - systemConfig6.dp2px(systemConfig6.mBottomPadding);
        return new Rect[]{newLeftBounds, newRightBounds};
    }

    public Rect getBounds(int position, boolean isScaled) {
        return getBounds(position, isScaled, false, "");
    }

    private Rect getBounds(int position, boolean isScaled, boolean isDragable, String pkgName) {
        SystemConfig systemConfig = this.mSystemConfig;
        if (systemConfig == null) {
            return DEFAULT_SINGLE_BOUNDS;
        }
        int posIndex = 0;
        if (position == 1 || position == 2) {
            if (!isDragable) {
                return getRationBounds(position, isScaled, pkgName);
            }
            if ((position != 1 || this.mIsCurrentRtl) && (position != 2 || !this.mIsCurrentRtl)) {
                posIndex = 1;
            }
            return getOrigDragRect(pkgName)[posIndex];
        } else if (position == 3) {
            return getMidConfigBounds(position, isScaled, isDragable, pkgName);
        } else {
            if (position != 5) {
                return DEFAULT_SINGLE_BOUNDS;
            }
            return parseStrToRect(systemConfig.getBounds(5), false, DEFAULT_SINGLE_BOUNDS);
        }
    }

    private Rect getMidConfigBounds(int type, boolean scaled, boolean isDragable, String pkgName) {
        if (isSupportAppTaskSplitScreen(pkgName)) {
            Rect midRect = parseStrToRect(this.mSystemConfig.getBounds(5), false, new Rect(0, 0, 0, 0));
            this.mMidBoounds.add(midRect);
            return midRect;
        }
        SystemConfig systemConfig = this.mSystemConfig;
        return parseStrToRect(isDragable ? systemConfig.mMiddleDragleBounds : systemConfig.mMiddleBounds, scaled, DEFAULT_MIDDLE_BOUNDS);
    }

    private Rect getRationBounds(int position, boolean scaled, String pkgName) {
        if (position != 1 && position != 2) {
            return null;
        }
        Rect rect = parseStrToRect(this.mSystemConfig.getBounds(position), scaled, position == 1 ? DEFAULT_LEFT_BOUNDS : DEFAULT_RIGHT_BOUNDS);
        OpenCapAppConfig pkgConfig = getOpenCapAppConfig(pkgName);
        if (pkgConfig == null) {
            return rect;
        }
        Rect rationRect = pkgConfig.getPositionRationBound(position);
        return rationRect == null ? rect : rationRect;
    }

    public void updateSystemBoundSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) mContext.getSystemService("window")).getDefaultDisplay().getRealMetrics(metrics);
        SystemConfig systemConfig = this.mSystemConfig;
        if (systemConfig != null) {
            systemConfig.updateSystemBoundSize(metrics, HwMwUtils.IS_FOLD_SCREEN_DEVICE, this.mIsCurrentRtl);
            this.mOpenCapAppConfigs.forEach($$Lambda$HwMagicWindowConfig$OQpTryGxyh6wXWjfsCkNzQp2Zdk.INSTANCE);
        }
        updateScaleRatio();
        updateBoundsSet();
    }

    private void updateScaleRatio() {
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) mContext.getSystemService("window")).getDefaultDisplay().getRealMetrics(dm);
        SystemConfig systemConfig = this.mSystemConfig;
        if (systemConfig != null) {
            String[] strGroups = systemConfig.getBounds(3).split(",", 0);
            if (strGroups.length == 4) {
                this.mRatio = ((float) ((int) (Float.valueOf(strGroups[2]).floatValue() - Float.valueOf(strGroups[0]).floatValue()))) / ((float) Math.min(dm.widthPixels, dm.heightPixels));
            } else {
                this.mRatio = 1.0f;
            }
        } else {
            this.mRatio = 1.0f;
        }
    }

    private void updateBoundsSet() {
        if (this.mSystemConfig == null) {
            Slog.w(TAG, "SystemConfig is null, can not update ration bound");
            return;
        }
        this.mMasterBoounds.clear();
        this.mSlaveBoounds.clear();
        this.mMidBoounds.clear();
        this.mMasterBoounds.add(parseStrToRect(this.mSystemConfig.mMasterBounds, false, DEFAULT_LEFT_BOUNDS));
        this.mSlaveBoounds.add(parseStrToRect(this.mSystemConfig.mSlaveBounds, false, DEFAULT_RIGHT_BOUNDS));
        this.mMidBoounds.add(parseStrToRect(this.mSystemConfig.mMiddleBounds, false, DEFAULT_MIDDLE_BOUNDS));
        this.mOpenCapAppConfigs.forEach($$Lambda$HwMagicWindowConfig$oqBF5BZVP3Bn1NvEzzH1YKNboSk.INSTANCE);
        if (HwMwUtils.IS_TABLET) {
            this.mMidBoounds.add(parseStrToRect(this.mSystemConfig.mMiddleDragleBounds, false, DEFAULT_MIDDLE_BOUNDS));
            this.mMasterBoounds.add(parseStrToRect(this.mSystemConfig.mMasterBounds, true, DEFAULT_LEFT_BOUNDS));
            this.mSlaveBoounds.add(parseStrToRect(this.mSystemConfig.mSlaveBounds, true, DEFAULT_RIGHT_BOUNDS));
            this.mMidBoounds.add(parseStrToRect(this.mSystemConfig.mMiddleBounds, true, DEFAULT_MIDDLE_BOUNDS));
            Map<Integer, List<Rect>> map = this.mDragBounds;
            if (map != null) {
                for (Map.Entry<Integer, List<Rect>> map2 : map.entrySet()) {
                    List<Rect> bounds = map2.getValue();
                    this.mMasterBoounds.add(bounds.get(isRtl() ? 1 : 0));
                    this.mSlaveBoounds.add(bounds.get(!isRtl()));
                }
            }
        }
    }

    private Rect getLargerBounds(Rect bound) {
        if (bound == null || bound.isEmpty()) {
            return bound;
        }
        return new Rect(bound.left, bound.top, ((int) (((float) bound.width()) / this.mRatio)) + bound.left, ((int) (((float) bound.height()) / this.mRatio)) + bound.top);
    }

    public boolean isSystemSupport(int type) {
        SystemConfig systemConfig = this.mSystemConfig;
        if (systemConfig == null) {
            return false;
        }
        if (type == 0) {
            return systemConfig.isSystemSupport(0);
        }
        if (type == 1) {
            return systemConfig.isSystemSupport(1);
        }
        if (type == 2) {
            return systemConfig.isSystemSupport(2);
        }
        if (type == 3) {
            return systemConfig.isSystemSupport(3);
        }
        if (type != 4) {
            return false;
        }
        return systemConfig.isSystemSupport(4);
    }

    public float getCornerRadius() {
        return this.mSystemConfig.getCornerRadius();
    }

    public int getHostViewThreshold() {
        return this.mSystemConfig.getHostViewThreshold();
    }

    public float getRatio(String pkgName) {
        if (isScaled(pkgName)) {
            return this.mRatio;
        }
        return 1.0f;
    }

    /* access modifiers changed from: private */
    public Rect parseStrToRect(String str, boolean scaled, Rect defaultRect) {
        try {
            String[] strGroup = str.split(",");
            int leftValue = Integer.valueOf(strGroup[0]).intValue();
            int topValue = Integer.valueOf(strGroup[1]).intValue();
            int rightValue = Integer.valueOf(strGroup[2]).intValue();
            int bottomValue = Integer.valueOf(strGroup[3]).intValue();
            if (scaled) {
                rightValue = ((int) ((((float) (rightValue - leftValue)) / this.mRatio) + 0.5f)) + leftValue;
                bottomValue = ((int) ((((float) (bottomValue - topValue)) / this.mRatio) + 0.5f)) + topValue;
            }
            return new Rect(leftValue, topValue, rightValue, bottomValue);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "parse magic window bound string to Rect error");
            return defaultRect;
        }
    }

    public boolean getHwMagicWinEnabled(String pkg) {
        SettingConfig target;
        if (!isEmpty(pkg) && (target = this.mSettingConfigs.get(pkg)) != null) {
            return target.getHwMagicWinEnabled();
        }
        return false;
    }

    public boolean getDialogShownForApp(String pkg) {
        SettingConfig target;
        if (!isEmpty(pkg) && (target = this.mSettingConfigs.get(pkg)) != null) {
            return target.getHwDialogShown();
        }
        return false;
    }

    public Map<String, Boolean> getHwMagicWinEnabledApps() {
        Map<String, Boolean> mHwMagicWinEnabledApps = new HashMap<>();
        this.mSettingConfigs.forEach(new BiConsumer(mHwMagicWinEnabledApps) {
            /* class com.android.server.magicwin.$$Lambda$HwMagicWindowConfig$LCd3SYpmfiFLTHW6jx23gXChZs */
            private final /* synthetic */ Map f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                HwMagicWindowConfig.lambda$getHwMagicWinEnabledApps$11(this.f$0, (String) obj, (SettingConfig) obj2);
            }
        });
        OpenCapAppConfig target = getOpenCapAppConfig(WECHAT_PACKAGE_NAME);
        if (target != null && target.isSupportAppTaskSplitScreen()) {
            mHwMagicWinEnabledApps.remove(WECHAT_PACKAGE_NAME);
        }
        return mHwMagicWinEnabledApps;
    }

    static /* synthetic */ void lambda$getHwMagicWinEnabledApps$11(Map mHwMagicWinEnabledApps, String k, SettingConfig v) {
        Boolean bool = (Boolean) mHwMagicWinEnabledApps.put(k, Boolean.valueOf(v.getHwMagicWinEnabled()));
    }

    public Map<String, SettingConfig> getHwMagicWinSettingConfigs() {
        return this.mSettingConfigs;
    }

    public boolean isNotchModeEnabled(String pkg) {
        PackageConfig appConfig;
        if (pkg == null || pkg.isEmpty() || (appConfig = this.mPackageConfigs.get(pkg)) == null || !appConfig.isNotchModeEnabled()) {
            return false;
        }
        return true;
    }

    public boolean isScaled(String pkgName) {
        if (HwMwUtils.IS_FOLD_SCREEN_DEVICE || isSupportAppTaskSplitScreen(pkgName)) {
            return false;
        }
        OpenCapAppConfig easyGoCfg = getOpenCapAppConfig(pkgName);
        if (easyGoCfg == null || !easyGoCfg.isScaleExist()) {
            PackageConfig pkgConfig = this.mPackageConfigs.get(pkgName);
            if (pkgConfig == null || !pkgConfig.isScaleEnabled() || isDragable(pkgName)) {
                return false;
            }
            return true;
        } else if (!easyGoCfg.isScaleEnabled() || isDragable(pkgName)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isDragable(String pkgName) {
        if (HwMwUtils.IS_FOLD_SCREEN_DEVICE || isEmpty(pkgName) || isSupportAppTaskSplitScreen(pkgName)) {
            return false;
        }
        OpenCapAppConfig easyGoCfg = this.mOpenCapAppConfigs.get(pkgName);
        if (easyGoCfg != null && easyGoCfg.isDragableExist()) {
            return easyGoCfg.isDragable();
        }
        PackageConfig localCfg = this.mPackageConfigs.get(pkgName);
        if (localCfg != null) {
            return localCfg.isDragable();
        }
        return false;
    }

    public int parseIntParama(String paramsStr, String key) {
        try {
            return new JSONObject(paramsStr).getInt(key);
        } catch (JSONException e) {
            Slog.e(TAG, "parseIntParamas fail ");
            return -1;
        }
    }

    private static class BaseAppConfig {
        boolean mIsDragable;
        boolean mIsScaleEnabled;
        List<String> mMainActivities;
        boolean mSupportVideoFScreen;

        private BaseAppConfig() {
            this.mSupportVideoFScreen = true;
            this.mIsDragable = false;
            this.mIsScaleEnabled = false;
            this.mMainActivities = new ArrayList();
        }

        public boolean isVideoFullscreen() {
            return this.mSupportVideoFScreen;
        }

        public boolean isDragable() {
            return this.mIsDragable;
        }

        public boolean isScaleEnabled() {
            return this.mIsScaleEnabled;
        }

        public void split(String strSeq) {
            if (!TextUtils.isEmpty(strSeq)) {
                String[] strArray = strSeq.split(",");
                for (int i = 0; i < strArray.length; i++) {
                    if (!"".equals(strArray[i])) {
                        this.mMainActivities.add(strArray[i]);
                    }
                }
            }
        }
    }

    public static class PackageConfig extends BaseAppConfig {
        private boolean mIsDefaultSetting;
        private boolean mIsNotchAdapted;
        private int mMagicWindowMode;
        private boolean mNeedRelaunch;
        /* access modifiers changed from: private */
        public String mPackageName;
        private boolean mSupportCameraPreview;
        private boolean mSupportLeftResume;

        @Override // com.android.server.magicwin.HwMagicWindowConfig.BaseAppConfig
        public /* bridge */ /* synthetic */ boolean isDragable() {
            return super.isDragable();
        }

        @Override // com.android.server.magicwin.HwMagicWindowConfig.BaseAppConfig
        public /* bridge */ /* synthetic */ boolean isScaleEnabled() {
            return super.isScaleEnabled();
        }

        @Override // com.android.server.magicwin.HwMagicWindowConfig.BaseAppConfig
        public /* bridge */ /* synthetic */ boolean isVideoFullscreen() {
            return super.isVideoFullscreen();
        }

        @Override // com.android.server.magicwin.HwMagicWindowConfig.BaseAppConfig
        public /* bridge */ /* synthetic */ void split(String str) {
            super.split(str);
        }

        public PackageConfig() {
            super();
        }

        public PackageConfig(String pkg, String mode, String fullScreenVideo, String leftResume, String cameraPreview, String isScaleEnabled, String needRelaunch, String defaultSetting, String isDragable, String isNotchAdapted) {
            super();
            this.mPackageName = pkg;
            this.mMagicWindowMode = strWindowModeToInt(mode);
            this.mSupportVideoFScreen = HwMagicWindowConfig.strToBoolean(fullScreenVideo);
            this.mSupportLeftResume = HwMagicWindowConfig.strToBoolean(leftResume);
            this.mSupportCameraPreview = HwMagicWindowConfig.strToBoolean(cameraPreview);
            this.mIsDragable = HwMagicWindowConfig.strToBoolean(isDragable);
            this.mNeedRelaunch = HwMagicWindowConfig.strToBoolean(needRelaunch);
            boolean z = true;
            this.mIsDefaultSetting = !"false".equals(defaultSetting);
            this.mIsScaleEnabled = (this.mIsDragable || !HwMagicWindowConfig.strToBoolean(isScaleEnabled)) ? false : z;
            this.mIsNotchAdapted = HwMagicWindowConfig.strToBoolean(isNotchAdapted);
        }

        public boolean needRelaunch() {
            return this.mNeedRelaunch;
        }

        public int getWindowMode() {
            return this.mMagicWindowMode;
        }

        public boolean isPkgSupport(int type) {
            if (type == 0) {
                return this.mSupportVideoFScreen;
            }
            if (type == 1) {
                return this.mSupportLeftResume;
            }
            if (type != 2) {
                return false;
            }
            return this.mSupportCameraPreview;
        }

        public boolean isNotchModeEnabled() {
            return this.mIsNotchAdapted;
        }

        public boolean isDefaultSetting() {
            return this.mIsDefaultSetting;
        }

        private int strWindowModeToInt(String mode) {
            if (HwMagicWindowConfig.isEmpty(mode)) {
                return -2;
            }
            try {
                return Integer.valueOf(mode).intValue();
            } catch (NumberFormatException e) {
                Slog.e(HwMagicWindowConfig.TAG, "parse string WindowMode to int error.mode=" + mode);
                return 1;
            }
        }
    }

    public static class HomeConfig {
        private String[] mHomeActivities;
        private String mPackageName;

        public HomeConfig() {
        }

        public HomeConfig(String mPackageName2, String[] mHomeActivities2) {
            this.mPackageName = mPackageName2;
            this.mHomeActivities = (String[]) mHomeActivities2.clone();
        }

        public String[] getHomes() {
            return this.mHomeActivities;
        }
    }

    public class OpenCapAppConfig extends BaseAppConfig {
        private static final int DEFALUT_VAULE = -1;
        private boolean isRelaunch;
        private String mClientRequest;
        private List<String> mDefaultFullScreenActivities = new ArrayList();
        private List<String> mDestroyWhenReplacedOnRightActivities = new ArrayList();
        private boolean mIsDragableExist = false;
        private boolean mIsScaleExist = false;
        private boolean mIsShowStatusBar = false;
        private boolean mIsSupportAppTaskSplitScreen = false;
        private boolean mIsUsingSystemActivityAnimation = true;
        private boolean mIsVideoFullscreenExist = false;
        private int mMode = -1;
        private String mPackageName;
        private int mRationL = -1;
        private Rect mRationMasterBound;
        private int mRationR = -1;
        private Rect mRationSlaveBound;
        private String mRelateActivity = "";
        private Map<String, Set<String>> mSpecPairActivityInfo = new HashMap();
        private int mSplitBarBgColor = -1;
        private int mSplitLineBgColor = -1;
        private List<String> mSupportTaskSplitScreenActivities = new ArrayList();
        private List<String> mTransActivities = new ArrayList();

        @Override // com.android.server.magicwin.HwMagicWindowConfig.BaseAppConfig
        public /* bridge */ /* synthetic */ boolean isDragable() {
            return super.isDragable();
        }

        @Override // com.android.server.magicwin.HwMagicWindowConfig.BaseAppConfig
        public /* bridge */ /* synthetic */ boolean isScaleEnabled() {
            return super.isScaleEnabled();
        }

        @Override // com.android.server.magicwin.HwMagicWindowConfig.BaseAppConfig
        public /* bridge */ /* synthetic */ boolean isVideoFullscreen() {
            return super.isVideoFullscreen();
        }

        @Override // com.android.server.magicwin.HwMagicWindowConfig.BaseAppConfig
        public /* bridge */ /* synthetic */ void split(String str) {
            super.split(str);
        }

        public boolean isVideoFullscreenExist() {
            return this.mIsVideoFullscreenExist;
        }

        public boolean isDragableExist() {
            return this.mIsDragableExist;
        }

        public boolean isScaleExist() {
            return this.mIsScaleExist;
        }

        private void setWindowsRationAndBound(String ratioParam, String devType) {
            if ((HwMwUtils.IS_FOLD_SCREEN_DEVICE ? HwMagicWindowConfig.UX_FOLD_DEVICE : HwMagicWindowConfig.UX_PAD_DEVICE).equals(devType)) {
                String[] foldRatio = ratioParam.split("\\|");
                if (foldRatio.length == 2) {
                    this.mRationL = HwMagicWindowConfig.strToInt(foldRatio[0], -1);
                    this.mRationR = HwMagicWindowConfig.strToInt(foldRatio[1], -1);
                    Slog.i(HwMagicWindowConfig.TAG, "setRationAndBound LR " + this.mRationL + " RR " + this.mRationR);
                    updateRationAndBound();
                    saveRationBound();
                }
            }
        }

        public void updateRationAndBound() {
            if (HwMagicWindowConfig.this.mSystemConfig == null || this.mRationL <= 0 || this.mRationR <= 0) {
                this.mRationSlaveBound = null;
                this.mRationMasterBound = null;
                return;
            }
            HwMagicWindowConfig hwMagicWindowConfig = HwMagicWindowConfig.this;
            this.mRationMasterBound = hwMagicWindowConfig.parseStrToRect(hwMagicWindowConfig.mSystemConfig.getBounds(1), false, HwMagicWindowConfig.DEFAULT_LEFT_BOUNDS);
            HwMagicWindowConfig hwMagicWindowConfig2 = HwMagicWindowConfig.this;
            this.mRationSlaveBound = hwMagicWindowConfig2.parseStrToRect(hwMagicWindowConfig2.mSystemConfig.getBounds(2), false, HwMagicWindowConfig.DEFAULT_RIGHT_BOUNDS);
            if (!HwMagicWindowConfig.this.mIsCurrentRtl) {
                int gapRtoL = this.mRationSlaveBound.left - this.mRationMasterBound.right;
                Rect rect = this.mRationMasterBound;
                int i = rect.left;
                int i2 = this.mRationL;
                rect.right = i + ((((this.mRationSlaveBound.right - this.mRationMasterBound.left) - gapRtoL) * i2) / (this.mRationR + i2));
                this.mRationSlaveBound.left = this.mRationMasterBound.right + gapRtoL;
            } else {
                int gapRtoL2 = this.mRationMasterBound.left - this.mRationSlaveBound.right;
                Rect rect2 = this.mRationSlaveBound;
                int i3 = rect2.left;
                int i4 = this.mRationR;
                rect2.right = i3 + ((((this.mRationMasterBound.right - this.mRationSlaveBound.left) - gapRtoL2) * i4) / (i4 + this.mRationL));
                this.mRationMasterBound.left = this.mRationSlaveBound.right + gapRtoL2;
            }
            Slog.i(HwMagicWindowConfig.TAG, "getBounds add slaveBound " + this.mRationSlaveBound + " masterBound " + this.mRationMasterBound);
        }

        public void saveRationBound() {
            if (HwMagicWindowConfig.this.mSystemConfig == null) {
                Slog.w(HwMagicWindowConfig.TAG, "SystemConfig is null, can not save ration bound");
                return;
            }
            Rect adjustRationRightBound = new Rect(this.mRationSlaveBound);
            int adjust = HwMagicWindowConfig.this.mSystemConfig.getSplitAdjustValue();
            if (!HwMagicWindowConfig.this.mIsCurrentRtl) {
                adjustRationRightBound.left += adjust;
            } else {
                adjustRationRightBound.right -= adjust;
            }
            HwMagicWindowConfig.this.mMasterBoounds.add(this.mRationMasterBound);
            HwMagicWindowConfig.this.mSlaveBoounds.add(this.mRationSlaveBound);
            HwMagicWindowConfig.this.mSlaveBoounds.add(adjustRationRightBound);
        }

        public Rect getPositionRationBound(int position) {
            if (position == 2) {
                return this.mRationSlaveBound;
            }
            if (position == 1) {
                return this.mRationMasterBound;
            }
            return null;
        }

        public OpenCapAppConfig(String packageName, String clientRequest) {
            super();
            this.mPackageName = packageName;
            this.mClientRequest = clientRequest;
        }

        /* access modifiers changed from: private */
        public void parseAppConfig() {
            try {
                JSONObject jsonObject = new JSONObject(this.mClientRequest);
                this.mMode = Integer.parseInt(jsonObject.getString(HwMagicWindowConfig.WINDOW_SWITCH_TYPE));
                this.mSupportVideoFScreen = getProperty(jsonObject, HwMagicWindowConfig.BODY_FULLSCREEN_VIDEO, this.mSupportVideoFScreen);
                this.mIsVideoFullscreenExist = jsonObject.has(HwMagicWindowConfig.BODY_FULLSCREEN_VIDEO);
                if (!jsonObject.isNull(HwMagicWindowConfig.BODY_DUAL_ACTS)) {
                    JSONObject dualPageJsonObj = jsonObject.getJSONObject(HwMagicWindowConfig.BODY_DUAL_ACTS);
                    split(dualPageJsonObj.optString(HwMagicWindowConfig.MAIN_PAGE_SET));
                    this.mRelateActivity = dualPageJsonObj.optString(HwMagicWindowConfig.RELATED_PAGE);
                    if (this.mMainActivities.isEmpty() || this.mMainActivities.contains(this.mRelateActivity)) {
                        this.mRelateActivity = "";
                        this.mMainActivities.clear();
                    }
                }
                JSONArray entities = jsonObject.optJSONArray(HwMagicWindowConfig.ACTIVITY_PAIRS);
                boolean z = true;
                if (this.mMode == 1) {
                    z = false;
                }
                parsePairsActivties(entities, z);
                parseTransActivties(jsonObject.optJSONArray(HwMagicWindowConfig.TRANS_ACTIVITIES));
                this.isRelaunch = Boolean.valueOf(jsonObject.optString(HwMagicWindowConfig.IS_RELAUNCH)).booleanValue();
                if (jsonObject.has(HwMagicWindowConfig.IS_SUPPORT_APP_TASK_SPLIT_SCREEN)) {
                    this.mIsSupportAppTaskSplitScreen = Boolean.valueOf(jsonObject.optString(HwMagicWindowConfig.IS_SUPPORT_APP_TASK_SPLIT_SCREEN)).booleanValue();
                }
                if (jsonObject.has(HwMagicWindowConfig.ACTIVITY_PARAM)) {
                    parseActivtiesParams(jsonObject.optJSONArray(HwMagicWindowConfig.ACTIVITY_PARAM));
                }
                if (jsonObject.has(HwMagicWindowConfig.UX_PARAM)) {
                    parseUxParams(jsonObject.getJSONObject(HwMagicWindowConfig.UX_PARAM));
                }
            } catch (NumberFormatException | JSONException e) {
                Slog.e(HwMagicWindowConfig.TAG, "parseRequest fail " + e);
                clearAppConfig();
            } catch (Exception e2) {
                Slog.e(HwMagicWindowConfig.TAG, "parseRequest fail unknow Exception");
                clearAppConfig();
            }
        }

        private void clearAppConfig() {
            HwMagicWindowConfig.this.delAppAdapterInfo(this.mPackageName);
        }

        private void parseUxParams(JSONObject uxJsonObj) throws JSONException {
            if (uxJsonObj == null) {
                Slog.d(HwMagicWindowConfig.TAG, "no Ux Params");
                return;
            }
            this.mIsDragable = getProperty(uxJsonObj, HwMagicWindowConfig.UX_IS_DRAGABLE, this.mIsDragable);
            this.mIsDragableExist = uxJsonObj.has(HwMagicWindowConfig.UX_IS_DRAGABLE);
            this.mIsScaleEnabled = getProperty(uxJsonObj, HwMagicWindowConfig.UX_IS_SCALED, this.mIsScaleEnabled);
            this.mIsScaleExist = uxJsonObj.has(HwMagicWindowConfig.UX_IS_SCALED);
            if (uxJsonObj.has(HwMagicWindowConfig.UX_IS_SHOW_STATUSBAR)) {
                this.mIsShowStatusBar = Boolean.valueOf(uxJsonObj.optString(HwMagicWindowConfig.UX_IS_SHOW_STATUSBAR)).booleanValue();
            }
            if (uxJsonObj.has(HwMagicWindowConfig.UX_SPLIT_LINE_BG_COLOR)) {
                this.mSplitLineBgColor = Color.parseColor(uxJsonObj.optString(HwMagicWindowConfig.UX_SPLIT_LINE_BG_COLOR).replace("0x", CPUCustBaseConfig.CPUCONFIG_INVALID_STR));
            }
            if (uxJsonObj.has(HwMagicWindowConfig.UX_SPLIT_BAR_BG_COLOR)) {
                this.mSplitBarBgColor = Color.parseColor(uxJsonObj.optString(HwMagicWindowConfig.UX_SPLIT_BAR_BG_COLOR).replace("0x", CPUCustBaseConfig.CPUCONFIG_INVALID_STR));
            }
            if (uxJsonObj.has(HwMagicWindowConfig.UX_WINDOWS_RATIO)) {
                JSONArray entities = uxJsonObj.optJSONArray(HwMagicWindowConfig.UX_WINDOWS_RATIO);
                for (int i = 0; i < entities.length(); i++) {
                    JSONObject eachEntity = entities.optJSONObject(i);
                    if (eachEntity != null) {
                        String deviceType = eachEntity.optString(HwMagicWindowConfig.UX_DEVICE_PARAM);
                        String ratio = eachEntity.optString(HwMagicWindowConfig.UX_RATIO_PARAM);
                        if (!HwMagicWindowConfig.isEmpty(deviceType) && !HwMagicWindowConfig.isEmpty(ratio)) {
                            setWindowsRationAndBound(ratio, deviceType);
                        }
                    }
                }
            }
            if (uxJsonObj.has(HwMagicWindowConfig.UX_USE_SYSTEM_ACTIVITY_ANIMATION)) {
                this.mIsUsingSystemActivityAnimation = Boolean.valueOf(uxJsonObj.optString(HwMagicWindowConfig.UX_USE_SYSTEM_ACTIVITY_ANIMATION)).booleanValue();
            }
        }

        private boolean getProperty(JSONObject jsonObj, String propertyName, boolean defaultValue) {
            if (jsonObj == null || HwMagicWindowConfig.isEmpty(propertyName)) {
                return defaultValue;
            }
            String value = jsonObj.optString(propertyName);
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
            if ("false".equalsIgnoreCase(value)) {
                return false;
            }
            return defaultValue;
        }

        private void parseActivtiesParams(JSONArray entities) throws JSONException {
            if (entities == null || entities.length() <= 0) {
                Slog.d(HwMagicWindowConfig.TAG, "no activity Params");
                return;
            }
            for (int i = 0; i < entities.length(); i++) {
                JSONObject eachEntity = entities.optJSONObject(i);
                if (eachEntity != null) {
                    String activityName = eachEntity.optString("name");
                    if (eachEntity.has(HwMagicWindowConfig.ACTIVITY_DEFAULT_FULLSCREEN) && Boolean.valueOf(eachEntity.optString(HwMagicWindowConfig.ACTIVITY_DEFAULT_FULLSCREEN)).booleanValue()) {
                        this.mDefaultFullScreenActivities.add(activityName);
                    }
                    if (eachEntity.has(HwMagicWindowConfig.ACTIVITY_IS_SUPPORT_TASK_SPLIT_SCREEN) && Boolean.valueOf(eachEntity.optString(HwMagicWindowConfig.ACTIVITY_IS_SUPPORT_TASK_SPLIT_SCREEN)).booleanValue()) {
                        this.mSupportTaskSplitScreenActivities.add(activityName);
                    }
                    if (eachEntity.has(HwMagicWindowConfig.ACTIVITY_DESTROY_WHEN_REPLACE_ON_RIGHT) && Boolean.valueOf(eachEntity.optString(HwMagicWindowConfig.ACTIVITY_DESTROY_WHEN_REPLACE_ON_RIGHT)).booleanValue()) {
                        this.mDestroyWhenReplacedOnRightActivities.add(activityName);
                    }
                }
            }
        }

        private void parsePairsActivties(JSONArray entities, boolean isSkipFromTo) throws JSONException {
            if (entities != null && entities.length() > 0) {
                for (int i = 0; i < entities.length(); i++) {
                    JSONObject eachEntity = entities.optJSONObject(i);
                    if (eachEntity != null) {
                        if (!isSkipFromTo) {
                            addActivityPolicyMap(eachEntity.optString("from"), eachEntity.optString("to"));
                        }
                        String mainActivity = eachEntity.optString(HwMagicWindowConfig.MAIN_PAGE);
                        String relateActivity = eachEntity.optString(HwMagicWindowConfig.RELATED_PAGE);
                        if (!HwMagicWindowConfig.isEmpty(relateActivity) && !HwMagicWindowConfig.isEmpty(mainActivity) && HwMagicWindowConfig.isEmpty(this.mRelateActivity) && HwMagicWindowConfig.isEmpty(this.mMainActivities) && !mainActivity.equals(relateActivity)) {
                            this.mRelateActivity = relateActivity;
                            if (this.mMainActivities == null) {
                                this.mMainActivities = new ArrayList();
                            }
                            this.mMainActivities.add(mainActivity);
                        }
                    }
                }
            }
        }

        private void parseTransActivties(JSONArray transEntities) throws JSONException {
            if (transEntities != null && transEntities.length() > 0) {
                for (int i = 0; i < transEntities.length(); i++) {
                    String activityName = transEntities.getString(i);
                    if (!HwMagicWindowConfig.isEmpty(activityName)) {
                        this.mTransActivities.add(activityName);
                    }
                }
            }
        }

        private void addActivityPolicyMap(String activityFrom, String activityTo) {
            if (!HwMagicWindowConfig.isEmpty(activityFrom) && !HwMagicWindowConfig.isEmpty(activityTo)) {
                Set<String> singleActPolicySet = this.mSpecPairActivityInfo.get(activityFrom);
                if (singleActPolicySet == null) {
                    singleActPolicySet = new HashSet();
                    this.mSpecPairActivityInfo.put(activityFrom, singleActPolicySet);
                }
                singleActPolicySet.add(activityTo);
            }
        }

        /* access modifiers changed from: package-private */
        public boolean isSpecPairActivities(String focus, String target) {
            Map<String, Set<String>> map;
            Set<String> set;
            if (HwMagicWindowConfig.isEmpty(focus) || HwMagicWindowConfig.isEmpty(target) || (map = this.mSpecPairActivityInfo) == null || !map.containsKey(focus) || (set = this.mSpecPairActivityInfo.get(focus)) == null || set.isEmpty() || (!set.contains("*") && !set.contains(target))) {
                return false;
            }
            return true;
        }

        /* access modifiers changed from: package-private */
        public boolean isSpecTransActivity(String curActivity) {
            return this.mTransActivities.contains(curActivity);
        }

        /* access modifiers changed from: package-private */
        public boolean isDefaultFullscreenActivity(String curActivity) {
            return this.mDefaultFullScreenActivities.contains(curActivity);
        }

        /* access modifiers changed from: package-private */
        public boolean isNeedStartByNewTaskActivity(String curActivity) {
            return this.mSupportTaskSplitScreenActivities.contains(curActivity);
        }

        /* access modifiers changed from: package-private */
        public boolean isNeedDestroyWhenReplaceOnRight(String curActivity) {
            return this.mDestroyWhenReplacedOnRightActivities.contains(curActivity);
        }

        /* access modifiers changed from: package-private */
        public boolean isReLaunchWhenResize() {
            return this.isRelaunch;
        }

        /* access modifiers changed from: package-private */
        public boolean isShowStatusBar() {
            return this.mIsShowStatusBar;
        }

        /* access modifiers changed from: package-private */
        public boolean isUsingSystemActivityAnimation() {
            return this.mIsUsingSystemActivityAnimation;
        }

        /* access modifiers changed from: package-private */
        public boolean isSupportAppTaskSplitScreen() {
            return this.mIsSupportAppTaskSplitScreen;
        }

        /* access modifiers changed from: package-private */
        public int getSplitLineBgColor() {
            return this.mSplitLineBgColor;
        }

        /* access modifiers changed from: package-private */
        public int getSplitBarBgColor() {
            return this.mSplitBarBgColor;
        }

        public List<String> getMainActivity() {
            return this.mMainActivities;
        }

        public String getRelateActivity() {
            return this.mRelateActivity;
        }

        public int getWindowMode() {
            int i = this.mMode;
            if (i == 0) {
                return 2;
            }
            if (i == 1) {
                return 3;
            }
            return -1;
        }
    }

    public static class SystemConfig {
        private boolean mAnimation;
        private boolean mBackToMiddle;
        private boolean mBackground;
        /* access modifiers changed from: private */
        public float mBottomPadding;
        private float mCornerRadius;
        private String mFullBounds;
        private int mHostViewThreshold;
        /* access modifiers changed from: private */
        public String mLeftDragleBounds;
        /* access modifiers changed from: private */
        public float mLeftPadding;
        /* access modifiers changed from: private */
        public String mMasterBounds;
        private float mMidDragPadding;
        private float mMidPadding;
        /* access modifiers changed from: private */
        public String mMiddleBounds;
        /* access modifiers changed from: private */
        public String mMiddleDragleBounds;
        /* access modifiers changed from: private */
        public boolean mOpenCapability;
        /* access modifiers changed from: private */
        public String mRightDragleBounds;
        /* access modifiers changed from: private */
        public float mRightPadding;
        private boolean mRoundAngle;
        /* access modifiers changed from: private */
        public String mSlaveBounds;
        private int mSplitAdjustValue;
        /* access modifiers changed from: private */
        public float mTopPadding;

        public SystemConfig() {
            this.mSplitAdjustValue = 0;
        }

        public SystemConfig(String leftPadding, String topPadding, String rightPadding, String bottomPadding, String midPadding, String midDragPadding, String roundAngle, String dynamicEffect, String background, String backToMiddle, String cornerRadius) {
            this.mLeftPadding = HwMagicWindowConfig.strToFloat(leftPadding, 0.0f);
            this.mTopPadding = HwMagicWindowConfig.strToFloat(topPadding, 0.0f);
            this.mRightPadding = HwMagicWindowConfig.strToFloat(rightPadding, 0.0f);
            this.mBottomPadding = HwMagicWindowConfig.strToFloat(bottomPadding, 0.0f);
            this.mMidPadding = HwMagicWindowConfig.strToFloat(midPadding, 0.0f);
            this.mMidDragPadding = HwMagicWindowConfig.strToFloat(midDragPadding, 0.0f);
            this.mRoundAngle = HwMagicWindowConfig.strToBoolean(roundAngle);
            this.mAnimation = HwMagicWindowConfig.strToBoolean(dynamicEffect);
            this.mBackground = HwMagicWindowConfig.strToBoolean(background);
            this.mBackToMiddle = HwMagicWindowConfig.strToBoolean(backToMiddle);
            this.mCornerRadius = HwMagicWindowConfig.strToFloat(cornerRadius, 0.0f);
        }

        public void setHostViewThreshold(String viewThreshold) {
            this.mHostViewThreshold = HwMagicWindowConfig.strToInt(viewThreshold, 0);
        }

        public int getHostViewThreshold() {
            int i = this.mHostViewThreshold;
            if (i > 0) {
                return i;
            }
            return 70;
        }

        public String getBounds(int type) {
            if (type == 1) {
                return this.mMasterBounds;
            }
            if (type == 2) {
                return this.mSlaveBounds;
            }
            if (type == 3) {
                return this.mMiddleBounds;
            }
            if (type != 5) {
                return null;
            }
            return this.mFullBounds;
        }

        public int getSplitAdjustValue() {
            return this.mSplitAdjustValue;
        }

        /* access modifiers changed from: private */
        public void updateSystemBoundSize(DisplayMetrics metrics, boolean isFoldScreen, boolean isRtl) {
            if (isFoldScreen) {
                initWindowBoundsForFold(metrics, isRtl);
            } else {
                initWindowBounds(metrics, isRtl);
            }
        }

        private String getSequenceBounds(int startx, int starty, int endx, int endy) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(String.valueOf(startx));
            buffer.append(",");
            buffer.append(String.valueOf(starty));
            buffer.append(",");
            buffer.append(String.valueOf(endx));
            buffer.append(",");
            buffer.append(String.valueOf(endy));
            return buffer.toString();
        }

        /* access modifiers changed from: private */
        public int dp2px(float dp) {
            return (int) Math.ceil((double) (HwMagicWindowConfig.mContext.getResources().getDisplayMetrics().density * dp));
        }

        private void initWindowBoundsForFold(DisplayMetrics metrics, boolean isRtl) {
            try {
                IWindowManager windowManager = WindowManagerGlobal.getWindowManagerService();
                Point initialSize = new Point();
                windowManager.getInitialDisplaySize(0, initialSize);
                int height = metrics.heightPixels;
                int width = (int) (((float) height) / (((float) initialSize.x) / ((float) initialSize.y)));
                Slog.d(HwMagicWindowConfig.TAG, "initWindowBoundsForFold width = " + width + " height = " + height);
                this.mSplitAdjustValue = (int) (metrics.density * 6.0f);
                this.mMasterBounds = getSequenceBounds(0, 0, (width - dp2px(this.mMidPadding)) / 2, height);
                this.mSlaveBounds = getSequenceBounds((dp2px(this.mMidPadding) + width) / 2, 0, width, height);
                if (isRtl) {
                    String tempBounds = new String(this.mMasterBounds);
                    this.mMasterBounds = this.mSlaveBounds;
                    this.mSlaveBounds = tempBounds;
                }
                this.mMiddleBounds = getSequenceBounds(0, 0, width, height);
                this.mFullBounds = this.mMiddleBounds;
            } catch (RemoteException e) {
                Slog.e(HwMagicWindowConfig.TAG, "RemoteException while calculate device size");
            }
        }

        public void initWindowBounds(DisplayMetrics metrics, boolean isRtl) {
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            if (HwMwUtils.IS_TABLET && height > width) {
                width = metrics.heightPixels;
                height = metrics.widthPixels;
            }
            int leftToMiddleX = (width - dp2px(this.mMidPadding)) / 2;
            int boundWidth = leftToMiddleX - dp2px(this.mLeftPadding);
            this.mMasterBounds = getSequenceBounds(dp2px(this.mLeftPadding), dp2px(this.mTopPadding), leftToMiddleX, height - dp2px(this.mBottomPadding));
            this.mSlaveBounds = getSequenceBounds((dp2px(this.mMidPadding) + width) / 2, dp2px(this.mTopPadding), width - dp2px(this.mRightPadding), height - dp2px(this.mBottomPadding));
            if (isRtl) {
                String tempBounds = new String(this.mMasterBounds);
                this.mMasterBounds = this.mSlaveBounds;
                this.mSlaveBounds = tempBounds;
            }
            this.mMiddleBounds = getSequenceBounds((width - boundWidth) / 2, dp2px(this.mTopPadding), (width + boundWidth) / 2, height - dp2px(this.mBottomPadding));
            int leftToMiddleDragableX = (width - dp2px(this.mMidDragPadding)) / 2;
            int boundDragableWidth = leftToMiddleDragableX - dp2px(this.mLeftPadding);
            this.mLeftDragleBounds = getSequenceBounds(dp2px(this.mLeftPadding), dp2px(this.mTopPadding), leftToMiddleDragableX, height - dp2px(this.mBottomPadding));
            this.mRightDragleBounds = getSequenceBounds((dp2px(this.mMidDragPadding) + width) / 2, dp2px(this.mTopPadding), width - dp2px(this.mRightPadding), height - dp2px(this.mBottomPadding));
            this.mMiddleDragleBounds = getSequenceBounds((width - boundDragableWidth) / 2, dp2px(this.mTopPadding), (width + boundDragableWidth) / 2, height - dp2px(this.mBottomPadding));
            this.mSplitAdjustValue = (int) (metrics.density * 4.0f);
            this.mFullBounds = getSequenceBounds(0, 0, width, height);
        }

        public boolean isSystemSupport(int type) {
            if (type == 0) {
                return this.mRoundAngle;
            }
            if (type == 1) {
                return this.mAnimation;
            }
            if (type == 2) {
                return this.mBackground;
            }
            if (type == 3) {
                return this.mOpenCapability;
            }
            if (type != 4) {
                return false;
            }
            return this.mBackToMiddle;
        }

        public float getCornerRadius() {
            return (float) dp2px(this.mCornerRadius);
        }
    }

    public void setOpenCapability(String openCapability) {
        if (this.mSystemConfig == null) {
            Slog.w(TAG, "SystemConfig is null,not can set capability");
        } else if (isEmpty(openCapability)) {
            boolean unused = this.mSystemConfig.mOpenCapability = false;
        } else {
            boolean unused2 = this.mSystemConfig.mOpenCapability = strToBoolean(openCapability);
        }
    }

    public Map<String, OpenCapAppConfig> getOpenCapAppConfigs() {
        return this.mOpenCapAppConfigs;
    }

    public static class SettingConfig {
        private boolean hwDialogShown;
        private int hwDragMode = 0;
        private boolean hwMagicWinEnabled;
        private String name;

        public SettingConfig(String name2, String enabled, String dialogShown, String mode) {
            this.name = name2;
            this.hwMagicWinEnabled = HwMagicWindowConfig.strToBoolean(enabled);
            this.hwDialogShown = HwMagicWindowConfig.strToBoolean(dialogShown);
            this.hwDragMode = HwMagicWindowConfig.strToInt(mode, 0);
        }

        public SettingConfig(String name2, boolean enabled, boolean dialogShown, int mode) {
            this.name = name2;
            this.hwMagicWinEnabled = enabled;
            this.hwDialogShown = dialogShown;
            this.hwDragMode = mode;
        }

        public String getName() {
            return this.name;
        }

        public boolean getHwMagicWinEnabled() {
            return this.hwMagicWinEnabled;
        }

        public void setMagicWinEnabled(boolean isEnabled) {
            this.hwMagicWinEnabled = isEnabled;
        }

        public boolean getHwDialogShown() {
            return this.hwDialogShown;
        }

        public void setHwDialogShown(boolean isDialogShown) {
            this.hwDialogShown = isDialogShown;
        }

        public int getDragMode() {
            return this.hwDragMode;
        }

        public void setDragMode(int mode) {
            this.hwDragMode = mode;
        }
    }

    public static class LoginStatusConfig {
        private int mStatus;

        public int getStatus() {
            return this.mStatus;
        }

        public void setStatus(int status) {
            this.mStatus = status;
        }
    }

    public static class HostRecognizeConfig {
        private String mHomeActivity;
        private String mPackageName;

        public HostRecognizeConfig(String pkg, String home) {
            this.mPackageName = pkg;
            this.mHomeActivity = home;
        }

        public String getHome() {
            return this.mHomeActivity;
        }
    }

    public void adjustSplitBound(int position, Rect bound) {
        if (position == 2) {
            SystemConfig systemConfig = this.mSystemConfig;
            int adjust = systemConfig == null ? 0 : systemConfig.getSplitAdjustValue();
            if (!this.mIsCurrentRtl) {
                bound.left += adjust;
            } else {
                bound.right -= adjust;
            }
        }
    }

    /* access modifiers changed from: private */
    public static int strToInt(String value, int defaultValue) {
        if (isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "parse string to float error");
            return defaultValue;
        }
    }

    /* access modifiers changed from: private */
    public static float strToFloat(String mode, float defaultValue) {
        if (mode == null || mode.isEmpty()) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(mode);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "parse string to float error.mode=" + mode + "  defaultValue=" + defaultValue);
            return defaultValue;
        }
    }

    /* access modifiers changed from: private */
    public static boolean strToBoolean(String str) {
        return "true".equals(str);
    }

    public void setIsRtl(boolean isRtl) {
        this.mIsCurrentRtl = isRtl;
    }

    public boolean isRtl() {
        return this.mIsCurrentRtl;
    }
}
