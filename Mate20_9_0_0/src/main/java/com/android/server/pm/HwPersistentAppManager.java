package com.android.server.pm;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser.Package;
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("packageName:");
            stringBuilder.append(this.packageName);
            stringBuilder.append(" original:");
            stringBuilder.append(this.original);
            stringBuilder.append(" persistent:");
            stringBuilder.append(this.persistent);
            stringBuilder.append(" updatable:");
            stringBuilder.append(this.updatable);
            return stringBuilder.toString();
        }
    }

    private static ArrayList<File> getPersistentConfigFileList(String filePath) {
        ArrayList<File> fileList = new ArrayList();
        if (TextUtils.isEmpty(filePath)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: file = [");
            stringBuilder.append(filePath);
            stringBuilder.append("]");
            Log.e(str, stringBuilder.toString());
            return fileList;
        }
        String[] policyDir = null;
        int i = 0;
        try {
            policyDir = HwCfgFilePolicy.getCfgPolicyDir(0);
        } catch (NoClassDefFoundError e) {
            Slog.w(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        }
        if (policyDir == null) {
            return fileList;
        }
        while (i < policyDir.length) {
            File file = new File(policyDir[i], filePath);
            if (file.exists()) {
                fileList.add(file);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getPersistentConfigFileList from  i=");
                stringBuilder2.append(i);
                stringBuilder2.append("| ");
                stringBuilder2.append(file.getAbsolutePath());
                Slog.d(str2, stringBuilder2.toString());
            }
            i++;
        }
        if (fileList.size() == 0) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("No persistent config file found for:");
            stringBuilder3.append(filePath);
            Log.w(str3, stringBuilder3.toString());
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
        HashMap<String, PersistentInfo> persistentConfigMap = new HashMap();
        int N = fileList.size();
        for (int i = 0; i < N; i++) {
            File file = (File) fileList.get(i);
            if (file != null && file.exists()) {
                persistentConfigMap.putAll(readPersistentConfigFile(file));
            }
        }
        return persistentConfigMap;
    }

    /* JADX WARNING: Missing block: B:30:?, code:
            r2.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static HashMap<String, PersistentInfo> readPersistentConfigFile(File file) {
        String str;
        StringBuilder stringBuilder;
        HashMap<String, PersistentInfo> result = new HashMap();
        if (file == null || !file.exists()) {
            return result;
        }
        FileInputStream stream = null;
        try {
            int type;
            String tag;
            stream = new FileInputStream(file);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, null);
            while (true) {
                int next = parser.next();
                type = next;
                if (next == 1 || type == 2) {
                    tag = parser.getName();
                }
            }
            tag = parser.getName();
            if ("persistent-config".equals(tag)) {
                parser.next();
                int outerDepth = parser.getDepth();
                while (true) {
                    int next2 = parser.next();
                    type = next2;
                    if (next2 == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                        try {
                            break;
                        } catch (IOException e) {
                        }
                    } else if (type != 3) {
                        if (type != 4) {
                            if ("item".equals(parser.getName())) {
                                PersistentInfo info = new PersistentInfo();
                                info.packageName = parser.getAttributeValue(null, "package");
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
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Settings do not start with policies tag: found ");
            stringBuilder2.append(tag);
            throw new XmlPullParserException(stringBuilder2.toString());
        } catch (FileNotFoundException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("file is not exist ");
            stringBuilder.append(e2.getMessage());
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (XmlPullParserException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e3.getMessage());
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (Exception e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e4.getMessage());
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (Throwable th) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e5) {
                }
            }
        }
    }

    private static boolean fixPkgPersistentFlag(Package pkg) {
        if (sPersistentConfigMap != null) {
            PersistentInfo persistentInfo = (PersistentInfo) sPersistentConfigMap.get(pkg.packageName);
            PersistentInfo info = persistentInfo;
            if (persistentInfo != null && ("true".equals(info.original) || "true".equals(info.persistent))) {
                return false;
            }
        }
        ApplicationInfo applicationInfo = pkg.applicationInfo;
        applicationInfo.flags &= -9;
        return true;
    }

    public static void readPersistentConfig() {
        if (!PERSISTENT_CONFIG_DISABLED) {
            sPersistentConfigMap = loadPersistentConfigInfo();
        }
    }

    public static void resolvePersistentFlagForPackage(int oldFlags, Package pkg) {
        if (!PERSISTENT_CONFIG_DISABLED && pkg != null && pkg.applicationInfo != null) {
            boolean oldNonPersistent = false;
            boolean newPersistent = (pkg.applicationInfo.flags & 8) != 0;
            if (newPersistent) {
                if ((oldFlags & 8) == 0) {
                    oldNonPersistent = true;
                }
                if (oldNonPersistent && newPersistent && fixPkgPersistentFlag(pkg)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(pkg.packageName);
                    stringBuilder.append(" does not allow to become a persistent app since old app is not a persistent app!");
                    Slog.i(str, stringBuilder.toString());
                }
            }
        }
    }

    public static boolean isPersistentUpdatable(Package pkg) {
        if (!(PERSISTENT_CONFIG_DISABLED || pkg == null || sPersistentConfigMap == null)) {
            PersistentInfo persistentInfo = (PersistentInfo) sPersistentConfigMap.get(pkg.packageName);
            PersistentInfo info = persistentInfo;
            if (persistentInfo == null || !"true".equals(info.updatable)) {
                return false;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(pkg.packageName);
            stringBuilder.append(" is marked as a updatable persistent app!");
            Slog.i(str, stringBuilder.toString());
            return true;
        }
        return false;
    }
}
