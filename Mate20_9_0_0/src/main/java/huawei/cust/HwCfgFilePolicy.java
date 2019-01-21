package huawei.cust;

import android.common.HwFrameworkFactory;
import android.os.SystemProperties;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public class HwCfgFilePolicy {
    public static final int BASE = 3;
    private static String[] CFG_DIRS = null;
    public static final int CLOUD_APN = 7;
    public static final int CLOUD_DPLMN = 6;
    public static final int CLOUD_MCC = 5;
    public static final int CUST = 4;
    public static final int CUST_TYPE_CONFIG = 0;
    public static final int CUST_TYPE_MEDIA = 1;
    public static final int DEFAULT_SLOT = -2;
    public static final int EMUI = 1;
    public static final int GLOBAL = 0;
    public static final String HW_ACTION_CARRIER_CONFIG_CHANGED = "com.huawei.action.CARRIER_CONFIG_CHANGED";
    public static final String HW_CARRIER_CONFIG_CHANGE_STATE = "state";
    public static final String HW_CARRIER_CONFIG_OPKEY = "opkey";
    public static final String HW_CARRIER_CONFIG_SLOT = "slot";
    public static final int HW_CONFIG_STATE_PARA_UPDATE = 3;
    public static final int HW_CONFIG_STATE_SIM_ABSENT = 2;
    public static final int HW_CONFIG_STATE_SIM_LOADED = 1;
    private static String[] MEDIA_DIRS = ((String[]) CFG_DIRS.clone());
    public static final int PC = 2;
    private static String TAG = "CfgFilePolicy";
    private static final int TXTSECTION = 2;
    private static final String[] VERSION_MARK = new String[]{"global_cfg_version", "emui_cfg_version", "pc_cfg_version", " ", "carrier_cfg_version"};
    private static IHwCarrierConfigPolicy hwCarrierConfigPolicy = HwFrameworkFactory.getHwCarrierConfigPolicy();
    private static HashMap<String, String> mCfgVersions = new HashMap();

    static {
        String policy = System.getenv("CUST_POLICY_DIRS");
        if (policy == null || policy.length() == 0) {
            Log.e(TAG, "****ERROR: env CUST_POLICY_DIRS not set, use default");
            policy = "/system/emui:/system/global:/system/etc:/oem:/data/cust:/cust_spec";
        }
        CFG_DIRS = policy.split(":");
        String CFG_SUFFIX = "/etc";
        int i = 0;
        while (i < MEDIA_DIRS.length) {
            if (MEDIA_DIRS[i].endsWith(CFG_SUFFIX) && !MEDIA_DIRS[i].equals(CFG_SUFFIX)) {
                MEDIA_DIRS[i] = MEDIA_DIRS[i].replace(CFG_SUFFIX, "");
            }
            i++;
        }
    }

    public static ArrayList<File> getCfgFileList(String fileName, int type) throws NoClassDefFoundError {
        return getCfgFileListCommon(fileName, type, -2);
    }

    public static ArrayList<File> getCfgFileList(String fileName, int type, int slotId) throws NoClassDefFoundError {
        return getCfgFileListCommon(fileName, type, slotId);
    }

    private static ArrayList<File> getCfgFileListCommon(String fileName, int type, int slotId) throws NoClassDefFoundError {
        ArrayList<File> res = new ArrayList();
        if (fileName == null || fileName.length() == 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: file = [");
            stringBuilder.append(fileName);
            stringBuilder.append("]");
            Log.e(str, stringBuilder.toString());
            return res;
        }
        String[] dirs = getCfgPolicyDir(type, slotId);
        for (String file : dirs) {
            File file2 = new File(file, fileName);
            if (file2.exists()) {
                res.add(file2);
            }
        }
        return res;
    }

    public static File getCfgFile(String fileName, int type) throws NoClassDefFoundError {
        return getCfgFileCommon(fileName, type, -2);
    }

    public static File getCfgFile(String fileName, int type, int slotId) throws NoClassDefFoundError {
        return getCfgFileCommon(fileName, type, slotId);
    }

    private static File getCfgFileCommon(String fileName, int type, int slotId) throws NoClassDefFoundError {
        String[] dirs = getCfgPolicyDir(type, slotId);
        for (int i = dirs.length - 1; i >= 0; i--) {
            File file = new File(dirs[i], fileName);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    public static String[] getCfgPolicyDir(int type) throws NoClassDefFoundError {
        return getCfgPolicyDirCommon(type, -2);
    }

    public static String[] getCfgPolicyDir(int type, int slotId) throws NoClassDefFoundError {
        return getCfgPolicyDirCommon(type, slotId);
    }

    private static String[] getCfgPolicyDirCommon(int type, int slotId) throws NoClassDefFoundError {
        String[] dirs;
        if (type == 1) {
            dirs = (String[]) MEDIA_DIRS.clone();
        } else {
            dirs = (String[]) CFG_DIRS.clone();
        }
        if (slotId != -2) {
            return parseCarrierPath(dirs, getOpKey(slotId));
        }
        try {
            return parseCarrierPath(dirs, getOpKey());
        } catch (Exception e) {
            Log.e(TAG, "parseCarrierPath fail.");
            return dirs;
        }
    }

    public static String getCfgVersion(int cfgType) throws NoClassDefFoundError {
        String str = null;
        String[] mccInfo;
        switch (cfgType) {
            case 0:
            case 1:
            case 2:
            case 4:
                if (!mCfgVersions.containsKey(VERSION_MARK[cfgType])) {
                    initFileVersions(getCfgFileList("version.txt", 0));
                }
                return (String) mCfgVersions.get(VERSION_MARK[cfgType]);
            case 3:
                return SystemProperties.get("ro.product.BaseVersion", null);
            case 5:
                mccInfo = getDownloadCfgFile("/cloud/mcc", "cloud/mcc/version.txt");
                if (mccInfo != null) {
                    str = mccInfo[1];
                }
                return str;
            case 6:
                mccInfo = getDownloadCfgFile("/cloud/dplmn", "cloud/dplmn/version.txt");
                if (mccInfo != null) {
                    str = mccInfo[1];
                }
                return str;
            case 7:
                mccInfo = getDownloadCfgFile("/cloud/apn", "cloud/apn/version.txt");
                if (mccInfo != null) {
                    str = mccInfo[1];
                }
                return str;
            default:
                return null;
        }
    }

    private static void initFileVersions(ArrayList<File> cfgFileList) {
        Iterator it = cfgFileList.iterator();
        while (it.hasNext()) {
            String[] versions = getVersionsFromFile((File) it.next());
            if (versions != null) {
                String oldversion = (String) mCfgVersions.get(versions[0]);
                if (oldversion == null || oldversion.compareTo(versions[1]) < 0) {
                    mCfgVersions.put(versions[0], versions[1]);
                }
            }
        }
    }

    private static String[] getVersionsFromFile(File file) {
        Scanner sc = null;
        try {
            sc = new Scanner(file, "UTF-8");
            while (sc.hasNextLine()) {
                String[] versions = sc.nextLine().split("=");
                if (2 == versions.length) {
                    sc.close();
                    return versions;
                }
            }
            sc.close();
            Log.e(TAG, "version file format is wrong.");
            return null;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "version file is not found.");
            if (sc != null) {
                sc.close();
            }
            return null;
        } catch (NullPointerException e2) {
            Log.e(TAG, "version file format is wrong.");
            if (sc != null) {
                sc.close();
            }
            return null;
        } catch (Throwable th) {
            if (sc != null) {
                sc.close();
            }
            throw th;
        }
    }

    private static String[] getFileInfo(String baseDir, String verDir, String filePath) {
        if (!new File(baseDir, filePath).exists()) {
            return null;
        }
        String[] info = new String[]{new File(baseDir, filePath).getPath(), ""};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(verDir);
        stringBuilder.append("/version.txt");
        String[] vers = getVersionsFromFile(new File(baseDir, stringBuilder.toString()));
        if (vers != null) {
            info[1] = vers[1];
        }
        return info;
    }

    public static String[] getDownloadCfgFile(String verDir, String filePath) throws NoClassDefFoundError {
        String[] cotaInfo = getFileInfo("/data/cota/para/", verDir, filePath);
        String[] independentCotaInfo = getFileInfo("/data/vendor/cota/para/", verDir, filePath);
        if (isNewerVersionInfo(independentCotaInfo, cotaInfo)) {
            cotaInfo = independentCotaInfo;
        }
        int i = 0;
        String[] cfgPolicyDir = getCfgPolicyDir(0);
        int length = cfgPolicyDir.length;
        while (i < length) {
            String[] info = getFileInfo(cfgPolicyDir[i], verDir, filePath);
            if (isNewerVersionInfo(info, cotaInfo)) {
                cotaInfo = info;
            }
            i++;
        }
        return cotaInfo;
    }

    private static boolean isNewerVersionInfo(String[] info, String[] cotaInfo) {
        if (info == null || (cotaInfo != null && info[1].compareTo(cotaInfo[1]) <= 0)) {
            return false;
        }
        return true;
    }

    public static String getOpKey() {
        if (hwCarrierConfigPolicy != null) {
            return hwCarrierConfigPolicy.getOpKey();
        }
        Log.e(TAG, "Error: hwCarrierConfigPolicy is null");
        return null;
    }

    public static String getOpKey(int slotId) {
        if (hwCarrierConfigPolicy != null) {
            return hwCarrierConfigPolicy.getOpKey(slotId);
        }
        Log.e(TAG, "Error: hwCarrierConfigPolicy is null");
        return null;
    }

    public static <T> T getValue(String key, Class<T> clazz) {
        if (hwCarrierConfigPolicy != null) {
            return hwCarrierConfigPolicy.getValue(key, clazz);
        }
        Log.e(TAG, "Error: hwCarrierConfigPolicy is null");
        return null;
    }

    public static <T> T getValue(String key, int slotId, Class<T> clazz) {
        if (hwCarrierConfigPolicy != null) {
            return hwCarrierConfigPolicy.getValue(key, slotId, clazz);
        }
        Log.e(TAG, "Error: hwCarrierConfigPolicy is null");
        return null;
    }

    public static Map getFileConfig(String fileName) {
        if (hwCarrierConfigPolicy != null) {
            return hwCarrierConfigPolicy.getFileConfig(fileName);
        }
        Log.e(TAG, "Error: hwCarrierConfigPolicy is null");
        return null;
    }

    public static Map getFileConfig(String fileName, int slotId) {
        if (hwCarrierConfigPolicy != null) {
            return hwCarrierConfigPolicy.getFileConfig(fileName, slotId);
        }
        Log.e(TAG, "Error: hwCarrierConfigPolicy is null");
        return null;
    }

    private static String[] parseCarrierPath(String[] dirs, String opKey) {
        if (opKey == null) {
            return (String[]) dirs.clone();
        }
        ArrayList<String> paths = new ArrayList();
        for (int i = 0; i < dirs.length; i++) {
            paths.add(dirs[i]);
            if (new File(dirs[i], "carrier").exists()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(dirs[i]);
                stringBuilder.append("/carrier/");
                stringBuilder.append(opKey);
                paths.add(stringBuilder.toString());
            }
        }
        return (String[]) paths.toArray(new String[0]);
    }
}
