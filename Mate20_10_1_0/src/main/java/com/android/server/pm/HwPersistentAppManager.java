package com.android.server.pm;

import android.content.pm.PackageParser;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class HwPersistentAppManager {
    private static final boolean PERSISTENT_CONFIG_DISABLED = SystemProperties.get("ro.hw_persistent.disable", "0").equals("1");
    private static final String PERSISTENT_CONFIG_FILE_PATH = "xml/hw_persistent_config.xml";
    private static final String TAG = "HwPersistentAppManager";
    private static HashMap<String, PersistentInfo> sPersistentConfigMap = null;

    private static final class PersistentInfo {
        public String original;
        public String packageName;
        public String persistent;
        public String updatable;

        private PersistentInfo() {
            this.packageName = "";
            this.original = "";
            this.persistent = "";
            this.updatable = "";
        }

        public String toString() {
            return "packageName:" + this.packageName + " original:" + this.original + " persistent:" + this.persistent + " updatable:" + this.updatable;
        }
    }

    private static ArrayList<File> getPersistentConfigFileList(String filePath) {
        ArrayList<File> fileList = new ArrayList<>();
        if (TextUtils.isEmpty(filePath)) {
            Log.e(TAG, "Error: file = [" + filePath + "]");
            return fileList;
        }
        String[] policyDir = null;
        try {
            policyDir = HwCfgFilePolicy.getCfgPolicyDir(0);
        } catch (NoClassDefFoundError e) {
            Slog.w(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        }
        if (policyDir == null) {
            return fileList;
        }
        for (int i = 0; i < policyDir.length; i++) {
            File file = new File(policyDir[i], filePath);
            if (file.exists()) {
                fileList.add(file);
                Slog.d(TAG, "getPersistentConfigFileList from  i=" + i + "| " + file.getAbsolutePath());
            }
        }
        if (fileList.size() == 0) {
            Log.w(TAG, "No persistent config file found for:" + filePath);
        }
        return fileList;
    }

    private static HashMap<String, PersistentInfo> loadPersistentConfigInfo() {
        ArrayList<File> fileList = null;
        try {
            fileList = getPersistentConfigFileList(PERSISTENT_CONFIG_FILE_PATH);
        } catch (NoClassDefFoundError er) {
            Slog.e(TAG, er.getMessage());
        }
        if (fileList == null || fileList.size() == 0) {
            return null;
        }
        HashMap<String, PersistentInfo> persistentConfigMap = new HashMap<>();
        int N = fileList.size();
        for (int i = 0; i < N; i++) {
            File file = fileList.get(i);
            if (file != null && file.exists()) {
                persistentConfigMap.putAll(readPersistentConfigFile(file));
            }
        }
        return persistentConfigMap;
    }

    private static HashMap<String, PersistentInfo> readPersistentConfigFile(File file) {
        HashMap<String, PersistentInfo> result = new HashMap<>();
        if (file == null || !file.exists()) {
            return result;
        }
        FileInputStream stream = null;
        try {
            FileInputStream stream2 = new FileInputStream(file);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream2, null);
            while (true) {
                int type = parser.next();
                if (type == 1 || type == 2) {
                    String tag = parser.getName();
                }
            }
            String tag2 = parser.getName();
            if ("persistent-config".equals(tag2)) {
                parser.next();
                int outerDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                        try {
                            stream2.close();
                            break;
                        } catch (IOException e) {
                        }
                    } else if (type2 != 3) {
                        if (type2 != 4) {
                            if ("item".equals(parser.getName())) {
                                PersistentInfo info = new PersistentInfo();
                                info.packageName = parser.getAttributeValue(null, "package");
                                if (!TextUtils.isEmpty(info.packageName)) {
                                    info.packageName = info.packageName.intern();
                                }
                                info.original = parser.getAttributeValue(null, "original");
                                info.persistent = parser.getAttributeValue(null, "persistent");
                                info.updatable = parser.getAttributeValue(null, "updatable");
                                result.put(info.packageName, info);
                                Slog.d(TAG, info.toString());
                            }
                        }
                    }
                }
                return result;
            }
            throw new XmlPullParserException("Settings do not start with policies tag: found " + tag2);
        } catch (FileNotFoundException e2) {
            Slog.w(TAG, "file is not exist " + e2.getMessage());
            if (0 != 0) {
                stream.close();
            }
        } catch (XmlPullParserException e3) {
            Slog.w(TAG, "failed parsing " + file + " " + e3.getMessage());
            if (0 != 0) {
                stream.close();
            }
        } catch (Exception e4) {
            Slog.w(TAG, "readPersistentConfigFile, failed parsing " + file + " catch Exception");
            if (0 != 0) {
                stream.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    stream.close();
                } catch (IOException e5) {
                }
            }
            throw th;
        }
    }

    private static boolean fixPkgPersistentFlag(PackageParser.Package pkg) {
        PersistentInfo info;
        HashMap<String, PersistentInfo> hashMap = sPersistentConfigMap;
        if (hashMap != null && (info = hashMap.get(pkg.packageName)) != null && ("true".equals(info.original) || "true".equals(info.persistent))) {
            return false;
        }
        pkg.applicationInfo.flags &= -9;
        return true;
    }

    public static void readPersistentConfig() {
        if (!PERSISTENT_CONFIG_DISABLED) {
            sPersistentConfigMap = loadPersistentConfigInfo();
        }
    }

    public static void resolvePersistentFlagForPackage(int oldFlags, PackageParser.Package pkg) {
        if (!PERSISTENT_CONFIG_DISABLED && oldFlags != Integer.MIN_VALUE && pkg != null && pkg.applicationInfo != null) {
            boolean oldNonPersistent = true;
            boolean newPersistent = (pkg.applicationInfo.flags & 8) != 0;
            if (newPersistent) {
                if ((oldFlags & 8) != 0) {
                    oldNonPersistent = false;
                }
                if (oldNonPersistent && newPersistent && fixPkgPersistentFlag(pkg)) {
                    Slog.i(TAG, pkg.packageName + " does not allow to become a persistent app since old app is not a persistent app!");
                }
            }
        }
    }

    public static boolean isPersistentUpdatable(PackageParser.Package pkg) {
        HashMap<String, PersistentInfo> hashMap;
        PersistentInfo info;
        if (PERSISTENT_CONFIG_DISABLED || pkg == null || (hashMap = sPersistentConfigMap) == null || (info = hashMap.get(pkg.packageName)) == null || !"true".equals(info.updatable)) {
            return false;
        }
        Slog.i(TAG, pkg.packageName + " is marked as a updatable persistent app!");
        return true;
    }
}
