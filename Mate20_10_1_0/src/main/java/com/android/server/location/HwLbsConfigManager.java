package com.android.server.location;

import android.content.Context;
import android.provider.Settings;
import com.android.server.LocationManagerServiceUtil;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupDataMgr;
import java.util.ArrayList;
import java.util.Arrays;

public class HwLbsConfigManager {
    private static final String BOOLEAN_VALUE_FALSE = "false";
    private static final String BOOLEAN_VALUE_TRUE = "true";
    private static final int DEFAULT_SIZE = 16;
    private static final String IAWARE_LIST_ADD_PREFIX = "+";
    private static final String IAWARE_LIST_DEL_PREFIX = "-";
    private static final String TAG = "HwLbsConfigManager";
    private static ArrayList<String> localQttffDisableList = new ArrayList<>(Arrays.asList("com.huawei.msdp", "com.huawei.HwOPServer", "com.yuedong.sport", "com.gotokeep.keep", "com.codoon.gps", "cn.ledongli.ldl", "com.sec.android.app.shealth", "com.mandian.android.dongdong", "com.imohoo.shanpao", "co.runner.app", "me.chunyu.Pedometer", "gz.lifesense.weidong", "com.lptiyu.tanke", "com.garmin.android.apps.connectmobile", "me.chunyu.ChunyuDoctor", "com.zhiyun.feel", "com.yelong.jibuqi", "com.fittimellc.fittime", "com.hupu.joggers", "com.runtastic.android.pro2", "com.lolaage.tbulu.tools", "com.cashwalk.cashwalk", "com.clue.android", "com.endomondo.android", "com.fitbit.FitbitMobile", "com.fitnesskeeper.runkeeper.pro", "com.garmin.android.apps.connectmobile", "com.google.android.apps.fitness", "com.huawei.health", "com.myfitnesspal.android", "com.popularapp.periodcalendar", "com.strava", "com.stt.android", "com.xiaomi.hm.health", "de.komoot.android", "fr.cnamts.it.activity", "io.yuka.android", "jp.co.mti.android.lunalunalite", "st.android.imsspublico", "za.co.myvirginactive", "com.android.location.fused", AppStartupDataMgr.HWPUSH_PKGNAME, "com.huawei.hidisk", "com.dada.mobile.android"));
    private static volatile HwLbsConfigManager mInstance;
    private static ArrayList<String> mSupervisoryControlWhiteList = new ArrayList<>();
    private static ArrayList<String> sBackgroundThrottlePackageWhitelists = new ArrayList<>(Arrays.asList("com.huawei.hwid", "com.huawei.aml"));
    private static ArrayList<String> sSupervisoryBackgroundThrottlePackageWhiteLists = new ArrayList<>(16);
    private static ArrayList<String> supervisoryQuickTtffBlackLists = new ArrayList<>(16);
    private boolean isIawareEnable = true;
    private boolean isQuickTtffEnable = true;
    private Context mContext;

    static {
        mSupervisoryControlWhiteList.add("com.sankuai.meituan");
        mSupervisoryControlWhiteList.add("com.sankuai.meituan.dispatch.homebrew");
        mSupervisoryControlWhiteList.add("com.huawei.hidisk");
        mSupervisoryControlWhiteList.add("com.nianticlabs.pokemongo");
        mSupervisoryControlWhiteList.add("com.motoband");
        mSupervisoryControlWhiteList.add("com.tencent.gwgo");
        supervisoryQuickTtffBlackLists.addAll(localQttffDisableList);
        sSupervisoryBackgroundThrottlePackageWhiteLists.addAll(sBackgroundThrottlePackageWhitelists);
    }

    private HwLbsConfigManager(Context context) {
        this.mContext = context;
    }

    public static HwLbsConfigManager getInstance(Context context) {
        HwLbsConfigManager hwLbsConfigManager;
        synchronized (HwLbsConfigManager.class) {
            if (mInstance == null) {
                mInstance = new HwLbsConfigManager(context);
            }
            hwLbsConfigManager = mInstance;
        }
        return hwLbsConfigManager;
    }

    public ArrayList<String> getListForFeature(String listName) {
        ArrayList<String> packges = new ArrayList<>();
        if (LbsConfigContent.CONFIG_IAWARE_CONTROLL_WHITELIST.equals(listName)) {
            packges = getIawareWhiteList();
        }
        if (LbsConfigContent.CONFIG_QTTFF_DISABLE_BLACKLIST.equals(listName)) {
            packges = getQuickTtffBlackList();
        }
        if (LbsConfigContent.CONFIG_BACKGROUNG_THROTTLE_WHITELIST.equals(listName)) {
            return getBackgroundThrottlePackageWhitelist();
        }
        return packges;
    }

    public boolean isEnableForParam(String paramName) {
        boolean isEnable = false;
        if (LbsConfigContent.CONFIG_IAWARE_ENABLE.equals(paramName)) {
            isEnable = isIawareEnabled();
        }
        if (LbsConfigContent.CONFIG_NLP_MEMORY_ENABLE.equals(paramName)) {
            isEnable = isNlpMemoryEnabled();
        }
        if (LbsConfigContent.CONFIG_ENABLE_GOOGLE_SUPL_SERVER.equals(paramName)) {
            isEnable = isGoogleSuplServerEnable();
        }
        if (LbsConfigContent.CONFIG_GMS_NLP_ENABLE.equals(paramName)) {
            isEnable = isGmsNlpEnabled();
        }
        if (LbsConfigContent.CONFIG_QTTFF_ENABLE.equals(paramName)) {
            return isQuickTtffEnabled();
        }
        return isEnable;
    }

    public boolean isParamAlreadySetup(String paramName) {
        if (LbsConfigContent.CONFIG_QTTFF_ENABLE.equals(paramName)) {
            return isQuickTtffSetup();
        }
        return false;
    }

    public String getStringForParam(String paramName) {
        String value = "";
        if (LbsConfigContent.CONFIG_SUPL_SERVER.equals(paramName)) {
            value = getSuplServer();
        }
        if (LbsConfigContent.CONFIG_SUPL_PORT.equals(paramName)) {
            value = getSuplPort();
        }
        if (LbsConfigContent.CONFIG_SUPL_HWSERVER.equals(paramName)) {
            value = Settings.Global.getString(this.mContext.getContentResolver(), LbsConfigContent.CONFIG_SUPL_HWSERVER);
        }
        if (LbsConfigContent.CONFIG_SUPL_HWPORT.equals(paramName)) {
            return Settings.Global.getString(this.mContext.getContentResolver(), LbsConfigContent.CONFIG_SUPL_HWPORT);
        }
        return value;
    }

    private ArrayList<String> getIawareWhiteList() {
        String whiteList = Settings.Global.getString(this.mContext.getContentResolver(), LbsConfigContent.CONFIG_IAWARE_CONTROLL_WHITELIST);
        if (whiteList == null) {
            return mSupervisoryControlWhiteList;
        }
        for (String packageName : Arrays.asList(whiteList.split(","))) {
            if (packageName.startsWith(IAWARE_LIST_ADD_PREFIX) && !mSupervisoryControlWhiteList.contains(packageName.substring(1))) {
                mSupervisoryControlWhiteList.add(packageName.substring(1));
            }
            if (packageName.startsWith("-") && mSupervisoryControlWhiteList.contains(packageName.substring(1))) {
                mSupervisoryControlWhiteList.remove(packageName.substring(1));
            }
        }
        return mSupervisoryControlWhiteList;
    }

    private boolean isIawareEnabled() {
        String value = Settings.Global.getString(this.mContext.getContentResolver(), LbsConfigContent.CONFIG_IAWARE_ENABLE);
        boolean isEnable = this.isIawareEnable;
        if (BOOLEAN_VALUE_TRUE.equals(value)) {
            return true;
        }
        if (BOOLEAN_VALUE_FALSE.equals(value)) {
            return false;
        }
        return isEnable;
    }

    private ArrayList<String> getQuickTtffBlackList() {
        String whiteList = Settings.Global.getString(this.mContext.getContentResolver(), LbsConfigContent.CONFIG_QTTFF_DISABLE_BLACKLIST);
        if (whiteList == null) {
            return supervisoryQuickTtffBlackLists;
        }
        for (String packageName : Arrays.asList(whiteList.split(","))) {
            if (packageName.startsWith(IAWARE_LIST_ADD_PREFIX) && !supervisoryQuickTtffBlackLists.contains(packageName.substring(1))) {
                supervisoryQuickTtffBlackLists.add(packageName.substring(1));
            }
            if (packageName.startsWith("-") && supervisoryQuickTtffBlackLists.contains(packageName.substring(1))) {
                supervisoryQuickTtffBlackLists.remove(packageName.substring(1));
            }
        }
        return supervisoryQuickTtffBlackLists;
    }

    private boolean isQuickTtffEnabled() {
        String value = Settings.Global.getString(this.mContext.getContentResolver(), LbsConfigContent.CONFIG_QTTFF_ENABLE);
        boolean z = this.isQuickTtffEnable;
        if (BOOLEAN_VALUE_TRUE.equals(value)) {
            return true;
        }
        if (BOOLEAN_VALUE_FALSE.equals(value)) {
            return false;
        }
        return this.isQuickTtffEnable;
    }

    private boolean isQuickTtffSetup() {
        String value = Settings.Global.getString(this.mContext.getContentResolver(), LbsConfigContent.CONFIG_QTTFF_ENABLE);
        if (BOOLEAN_VALUE_TRUE.equals(value) || BOOLEAN_VALUE_FALSE.equals(value)) {
            return true;
        }
        return false;
    }

    private ArrayList<String> getBackgroundThrottlePackageWhitelist() {
        String whiteList = Settings.Global.getString(this.mContext.getContentResolver(), LbsConfigContent.CONFIG_BACKGROUNG_THROTTLE_WHITELIST);
        if (whiteList == null) {
            return sSupervisoryBackgroundThrottlePackageWhiteLists;
        }
        for (String packageName : Arrays.asList(whiteList.split(","))) {
            if (packageName.startsWith(IAWARE_LIST_ADD_PREFIX) && !sSupervisoryBackgroundThrottlePackageWhiteLists.contains(packageName.substring(1))) {
                sSupervisoryBackgroundThrottlePackageWhiteLists.add(packageName.substring(1));
            }
            if (packageName.startsWith("-") && sSupervisoryBackgroundThrottlePackageWhiteLists.contains(packageName.substring(1))) {
                sSupervisoryBackgroundThrottlePackageWhiteLists.remove(packageName.substring(1));
            }
        }
        return sSupervisoryBackgroundThrottlePackageWhiteLists;
    }

    private boolean isNlpMemoryEnabled() {
        if (BOOLEAN_VALUE_FALSE.equals(Settings.Global.getString(this.mContext.getContentResolver(), LbsConfigContent.CONFIG_NLP_MEMORY_ENABLE))) {
            return false;
        }
        return true;
    }

    private boolean isGoogleSuplServerEnable() {
        if (!LocationManagerServiceUtil.isGmsVersion() && !BOOLEAN_VALUE_TRUE.equals(Settings.Global.getString(this.mContext.getContentResolver(), LbsConfigContent.CONFIG_ENABLE_GOOGLE_SUPL_SERVER))) {
            return false;
        }
        return true;
    }

    private String getSuplServer() {
        if (LocationManagerServiceUtil.isChineseVersion()) {
            return Settings.Global.getString(this.mContext.getContentResolver(), "supl_server_cn");
        }
        return Settings.Global.getString(this.mContext.getContentResolver(), "supl_server_global");
    }

    private String getSuplPort() {
        if (LocationManagerServiceUtil.isChineseVersion()) {
            return Settings.Global.getString(this.mContext.getContentResolver(), "supl_port_cn");
        }
        return Settings.Global.getString(this.mContext.getContentResolver(), "supl_port_global");
    }

    private boolean isGmsNlpEnabled() {
        if (BOOLEAN_VALUE_TRUE.equals(Settings.Global.getString(this.mContext.getContentResolver(), LbsConfigContent.CONFIG_GMS_NLP_ENABLE))) {
            return true;
        }
        return false;
    }
}
