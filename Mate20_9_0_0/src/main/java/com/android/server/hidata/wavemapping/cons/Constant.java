package com.android.server.hidata.wavemapping.cons;

import android.content.Context;
import com.android.server.hidata.wavemapping.util.FileUtils;
import com.android.server.hidata.wavemapping.util.LogUtil;
import java.util.HashMap;
import java.util.regex.Pattern;

public class Constant {
    public static final float APPQOE_POOR_INTERVAL = 5000.0f;
    public static final String BEHAVIOR_TABLE_NAME = "BEHAVIOR_MAINTAIN";
    public static final String CHR_HISTQOERPT = "CHR_HISTQOERPT";
    public static final String CHR_LOCATION_TABLE_NAME = "CHR_FREQUENT_LOCATION";
    public static final int DEFAULT_CLUSTER_NUM = -1;
    public static final String DISCRI_LOG_FILE_EXTENSION = ".discri.log.csv";
    public static final String ENTERPRISE_AP_TABLE_NAME = "ENTERPRISE_AP";
    public static final String FASTBACK2LTECHR_NAME = "CHR_FASTBACK2LTE";
    public static final String IDENTIFY_LOG_EXTENSION = ".identify.log.csv";
    public static final String IDENTIFY_RESULT_TABLE_NAME = "IDENTIFY_RESULT";
    public static final int ID_INVALID = 0;
    public static final String ID_NA = "NA";
    public static final String ID_UNKNOWN = "UNKNOWN";
    public static final String ID_ZERO = "0";
    public static final int ISMAINAP = 1;
    public static final int LIMIT_TRAINING_INTERVAL = 43200000;
    public static final String LOCATION_TABLE_NAME = "FREQUENT_LOCATION";
    public static final String LOG_FILE = "log.txt";
    public static final String LOG_FILE_EXTENSION = ".log.csv";
    public static final String MAINAP_IDENTIFY_LOG_EXTENSION = ".mpidentify.log.csv";
    public static final String MAINAP_LOG_FILE_EXTENSION = ".mplog.csv";
    public static final String MAINAP_MODEL_FILE_EXTENSION = ".mpmd.csv";
    public static final String MAINAP_RAW_FILE_EXTENSION = ".mprd.csv";
    public static final String MAINAP_STA_DATA_FILE_EXTENSION = ".mpdt.csv";
    public static final String MAINAP_TAG = "MAIN_AP";
    public static final String MAINAP_TESTDATA_FILE_EXTENSION = ".mptd.csv";
    public static final String MAINAP_TEST_DATA_FILE_EXTENSION = ".test.mprd.csv";
    public static final String MAINAP_TRAIN_DATA_FILE_EXTENSION = ".train.mprd.csv";
    public static final int MASK_GAME_SCENES = 200000;
    public static final long MAX_FILE_SIZE = 52428800;
    public static final long MILLISEC_ONE_MONTH = 2592000000L;
    public static final long MILLISEC_SEVEN_DAYS = 604800000;
    public static final int MILLISEC_TO_HOURS = 3600000;
    public static final int MILLISEC_TO_MINUTES = 60000;
    public static final int MOBILEAP_ENTERPRESIAP_EXPIRE_DAYS = -30;
    public static final int MOBILE_AP_SOURCE_TYPE_1 = 1;
    public static final int MOBILE_AP_SOURCE_TYPE_2 = 2;
    public static final String MOBILE_AP_TABLE_NAME = "MOBILE_AP";
    public static final String MODEL_FILE_EXTENSION = ".md.csv";
    public static final int MSG_BUILD_MODEL = 1;
    public static final int MSG_HAVE_MODEL = 4;
    public static final int MSG_REV_SYSBOOT = 1;
    public static final int MSG_REV_SYSSHUTDOWN = 2;
    public static final int MSG_TRAIN_MODEL = 3;
    public static final String NAME_FREQLACATION = "FREQLOC";
    public static final String NAME_FREQLOCATION_HOME = "HOME";
    public static final String NAME_FREQLOCATION_OFFICE = "OFFICE";
    public static final String NAME_FREQLOCATION_OTHER = "OTHER";
    private static final int OFFSET_APP_ID = 100;
    public static final Pattern PATTERN_STR2INT = Pattern.compile("[0-9]+");
    public static final String RAW_FILE_EXTENSION = ".rd.csv";
    public static final String RAW_FILE_GAME_EXTENSION = ".game.csv";
    public static final String RAW_FILE_STREAM_EXTENSION = ".stream.csv";
    public static final String RAW_FILE_WIFIPRO_EXTENSION = ".wifipro.csv";
    public static final String REGULAR_PLACESTATE_TABLE_NAME = "RGL_PLACESTATE";
    public static final int REPORT_CALLBACK_TYPE_NO_PREFER = 0;
    public static final int REPORT_CALLBACK_TYPE_POWER = 2;
    public static final int REPORT_CALLBACK_TYPE_USER = 1;
    public static final String RESULT_SEPERATE = "_";
    public static final String RESULT_UNKNOWN = "unkno";
    private static String ROOTPath = "/data/system/app_wavemapping/";
    public static final int SEND_STALL_MSG_DELAY = 15000;
    public static final String SPACEUSER_TABLE_NAME = "SPACEUSER_BASE";
    public static final String STA_DATA_FILE_EXTENSION = ".dt.csv";
    private static final String TAG;
    public static final String TEMP_STD_DATA_TABLE_NAME = "_TEMP_STD";
    public static final String TESTDATA_FILE_EXTENSION = ".td.csv";
    public static final String TEST_DATA_FILE_EXTENSION = ".test.rd.csv";
    public static final String TRAIN_DATA_FILE_EXTENSION = ".train.rd.csv";
    public static final String USERDB_APP_NAME_DURATION = "DURATION";
    public static final String USERDB_APP_NAME_GOOD = "GOODCOUNT";
    public static final String USERDB_APP_NAME_MOBILE = "MOBILE";
    public static final String USERDB_APP_NAME_NONE = "NONE";
    public static final String USERDB_APP_NAME_POOR = "POORCOUNT";
    public static final String USERDB_APP_NAME_PREFIX = "APP_";
    public static final String USERDB_APP_NAME_WIFI = "WIFI";
    public static final int USERDB_APP_QOE_GOOD = 2;
    public static final int USERDB_APP_QOE_NULL = -1;
    public static final int USERDB_APP_QOE_POOR = 1;
    public static final int USERDB_EXPIRED_DAYS = 30;
    private static String WMAPING_CONFIG = "/system/emui/base/emcom/noncell/wmp_beta.conf";
    private static String WMAPING_MAINAP_CONFIG = "/system/emui/base/emcom/noncell/wmp_mainap_beta.conf";
    public static final int WM_ACTION_ENTER = 0;
    public static final int WM_ACTION_LEAVE = 1;
    public static final int WM_LOCATION_HOME = 0;
    public static final int WM_LOCATION_OFFICE = 1;
    private static String dataPath = null;
    private static String dbPath = null;
    public static final String lineSeperate = System.getProperty("line.separator");
    private static String logPath = null;
    private static String modelPath = null;
    private static String rawDataPath = null;
    private static HashMap<Integer, Float> savedQoeAppList = new HashMap();
    private static String testResultPath = null;
    public static final String wordSeperate = ",";

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(Constant.class.getSimpleName());
        TAG = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(ROOTPath);
        stringBuilder.append("HwWaveMap.db");
        dbPath = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(ROOTPath);
        stringBuilder.append("data/");
        dataPath = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(ROOTPath);
        stringBuilder.append("rawdata/");
        rawDataPath = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(ROOTPath);
        stringBuilder.append("model/");
        modelPath = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(ROOTPath);
        stringBuilder.append("log/");
        logPath = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(ROOTPath);
        stringBuilder.append("test/");
        testResultPath = stringBuilder.toString();
    }

    public static boolean checkPath(Context context) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ROOTPath);
        stringBuilder.append("HwWaveMap.db");
        dbPath = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(ROOTPath);
        stringBuilder.append("data/");
        dataPath = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(ROOTPath);
        stringBuilder.append("rawdata/");
        rawDataPath = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(ROOTPath);
        stringBuilder.append("model/");
        modelPath = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(ROOTPath);
        stringBuilder.append("log/");
        logPath = stringBuilder.toString();
        LogUtil.i("checkPath begin");
        if (!FileUtils.mkdir(ROOTPath)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mkdirs ");
            stringBuilder.append(ROOTPath);
            stringBuilder.append(" error ");
            LogUtil.d(stringBuilder.toString());
            return false;
        } else if (!FileUtils.mkdir(dataPath)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mkdirs ");
            stringBuilder.append(dataPath);
            stringBuilder.append(" error ");
            LogUtil.d(stringBuilder.toString());
            return false;
        } else if (!FileUtils.mkdir(rawDataPath)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mkdirs ");
            stringBuilder.append(rawDataPath);
            stringBuilder.append(" error ");
            LogUtil.d(stringBuilder.toString());
            return false;
        } else if (!FileUtils.mkdir(modelPath)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mkdirs ");
            stringBuilder.append(modelPath);
            stringBuilder.append(" error ");
            LogUtil.d(stringBuilder.toString());
            return false;
        } else if (!FileUtils.mkdir(logPath)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mkdirs ");
            stringBuilder.append(logPath);
            stringBuilder.append(" error ");
            LogUtil.d(stringBuilder.toString());
            return false;
        } else if (FileUtils.mkdir(testResultPath)) {
            return true;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mkdirs ");
            stringBuilder.append(testResultPath);
            stringBuilder.append(" error ");
            LogUtil.d(stringBuilder.toString());
            return false;
        }
    }

    public static int transferGameId2FullId(int appId, int scenesId) {
        return ((appId * 100) + scenesId) - 200000;
    }

    public static HashMap<Integer, Float> getSavedQoeAppList() {
        return savedQoeAppList;
    }

    public static void setSavedQoeAppList(HashMap<Integer, Float> inList) {
        savedQoeAppList = (HashMap) inList.clone();
    }

    public static String getLogPath() {
        return logPath;
    }

    public static String getLogFilePath() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(logPath);
        stringBuilder.append(LOG_FILE);
        return stringBuilder.toString();
    }

    public static void setLogPath(String logPath) {
        logPath = logPath;
    }

    public static String getRawDataPath() {
        return rawDataPath;
    }

    public static void setRawDataPath(String rawDataPath) {
        rawDataPath = rawDataPath;
    }

    public static String getROOTPath() {
        return ROOTPath;
    }

    public static void setROOTPath(String ROOTPath) {
        ROOTPath = ROOTPath;
    }

    public static String getDbPath() {
        return dbPath;
    }

    public static void setDbPath(String dbPath) {
        dbPath = dbPath;
    }

    public static String getDataPath() {
        return dataPath;
    }

    public static void setDataPath(String dataPath) {
        dataPath = dataPath;
    }

    public static String getModelPath() {
        return modelPath;
    }

    public static void setModelPath(String modelPath) {
        modelPath = modelPath;
    }

    public static String getTestResultPath() {
        return testResultPath;
    }

    public static void setTestResultPath(String testResultPath) {
        testResultPath = testResultPath;
    }

    public static String getWmapingConfig() {
        return WMAPING_CONFIG;
    }

    public static String getWmapingMainapConfig() {
        return WMAPING_MAINAP_CONFIG;
    }
}
