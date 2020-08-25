package com.android.server.pm;

import android.annotation.SuppressLint;
import android.content.pm.PackageParser;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class HwShellAppsHandler {
    private static final String CONFIG_FILE = "/system/etc/dexopt/shell_identify_and_oat_clean.xml";
    private static final String INFO_TYPE_SHELL_BY_PACKAHE_NAME = "shellByPackageName";
    private static final String INFO_TYPE_SHELL_CLEAR_OAT = "shellClearOat";
    private static final String INFO_TYPE_SHELL_IDENTIFY = "shellIdentify";
    private static final boolean IS_DEBUG = false;
    private static final int PARSER_DEPTH_FOUR = 4;
    private static final int PARSER_DEPTH_ONE = 1;
    private static final int PARSER_DEPTH_THREE = 3;
    private static final int PARSER_DEPTH_TWO = 2;
    private static final int PARSER_DEPTH_ZERO = 0;
    private static final String TAG = "HwShellAppsHandler";
    private File mAppDataDir;
    private final Installer mInstaller;
    private List<ShellItem> mShellByPackageNames;
    private List<ShellItem> mShellClearOats;
    private List<ShellItem> mShellIdentifies;
    private File mUserAppDataDir;

    HwShellAppsHandler(Installer installer) {
        this.mShellIdentifies = null;
        this.mShellClearOats = null;
        this.mShellByPackageNames = null;
        this.mShellClearOats = new ArrayList();
        this.mShellIdentifies = new ArrayList();
        this.mShellByPackageNames = new ArrayList();
        parseShellConfig();
        File dataDir = Environment.getDataDirectory();
        this.mAppDataDir = new File(dataDir, "data");
        this.mUserAppDataDir = new File(dataDir, "user");
        this.mInstaller = installer;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0038, code lost:
        r4 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:?, code lost:
        r2.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x003d, code lost:
        r5 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x003e, code lost:
        r3.addSuppressed(r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0041, code lost:
        throw r4;
     */
    @SuppressLint({"PreferForInArrayList"})
    public String analyseShell(PackageParser.Package pkg) {
        if (pkg == null) {
            return null;
        }
        List<ShellItem> tmpShellIdentifies = deepCopyShellItem(this.mShellIdentifies);
        try {
            ZipFile file = new ZipFile(pkg.baseCodePath);
            Enumeration<? extends ZipEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (!entry.isDirectory()) {
                    filterOutIdentifyFiles(entry.getName(), tmpShellIdentifies);
                }
            }
            file.close();
        } catch (IOException e) {
            Log.e(TAG, "IOException! fail to process apk file: " + pkg.baseCodePath);
        } catch (Exception e2) {
            Log.e(TAG, "fail to process apk file: " + pkg.baseCodePath + ", " + e2.getClass().toString());
        }
        for (ShellItem shellIdentify : tmpShellIdentifies) {
            if (shellIdentify.getFiles().size() == 0) {
                return shellIdentify.getName();
            }
        }
        return analyseShellByPackageName(pkg);
    }

    @SuppressLint({"PreferForInArrayList"})
    private String analyseShellByPackageName(PackageParser.Package pkg) {
        if (this.mShellByPackageNames == null || pkg == null || pkg.packageName == null) {
            return null;
        }
        for (ShellItem shellIdentify : this.mShellByPackageNames) {
            Iterator<String> i = shellIdentify.getFiles().iterator();
            while (true) {
                if (i.hasNext()) {
                    String identifyPackageName = i.next();
                    if (identifyPackageName != null && identifyPackageName.equals(pkg.packageName)) {
                        return shellIdentify.getName();
                    }
                }
            }
        }
        return null;
    }

    public void processShellApp(PackageParser.Package pkg) {
    }

    @SuppressLint({"PreferForInArrayList"})
    private void filterOutIdentifyFiles(String fileName, List<ShellItem> tmpShellIdentifies) {
        for (ShellItem shellIdentify : tmpShellIdentifies) {
            Iterator<String> iterator = shellIdentify.getFiles().iterator();
            while (iterator.hasNext()) {
                String identifyFileName = iterator.next();
                if (!fileName.equals(identifyFileName)) {
                    if (!fileName.endsWith("/" + identifyFileName)) {
                    }
                }
                iterator.remove();
            }
        }
    }

    private static class ShellItem {
        private ArrayList<String> mFiles;
        private String mName;

        ShellItem(String name, ArrayList<String> files) {
            this.mName = name;
            this.mFiles = files;
        }

        public String getName() {
            return this.mName;
        }

        public ArrayList<String> getFiles() {
            return this.mFiles;
        }
    }

    @SuppressLint({"PreferForInArrayList"})
    public String dumpIdentifies() {
        StringBuffer sb = new StringBuffer();
        sb.append(System.lineSeparator());
        sb.append("INFO_TYPE_SHELL_IDENTIFY");
        for (ShellItem shellItem : this.mShellIdentifies) {
            sb.append("    ");
            sb.append(shellItem.getName());
            sb.append(System.lineSeparator());
            Iterator<String> it = shellItem.getFiles().iterator();
            while (it.hasNext()) {
                sb.append("        ");
                sb.append(it.next());
                sb.append(System.lineSeparator());
            }
        }
        sb.append(System.lineSeparator());
        sb.append("INFO_TYPE_SHELL_CLEAR_OAT");
        for (ShellItem shellItem2 : this.mShellClearOats) {
            sb.append("    ");
            sb.append(shellItem2.getName());
            sb.append(System.lineSeparator());
            Iterator<String> it2 = shellItem2.getFiles().iterator();
            while (it2.hasNext()) {
                sb.append("        ");
                sb.append(it2.next());
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    private void parseShellConfig() {
        File file = new File("/system/etc", "dexopt/shell_identify_and_oat_clean.xml");
        FileInputStream stream = null;
        try {
            File cfg = HwCfgFilePolicy.getCfgFile("dexopt/shell_identify_and_oat_clean.xml", 0);
            if (cfg != null) {
                file = cfg;
            }
            FileInputStream stream2 = new FileInputStream(file);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream2, null);
            parseXml(parser);
            try {
                stream2.close();
            } catch (IOException e) {
                Log.d(TAG, "parseShellConfig stream close FAIL!");
            }
        } catch (NoClassDefFoundError e2) {
            Log.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
            if (0 != 0) {
                stream.close();
            }
        } catch (FileNotFoundException e3) {
            Log.w(TAG, "parseShellConfig, file is not exist");
            if (0 != 0) {
                stream.close();
            }
        } catch (XmlPullParserException e4) {
            Log.w(TAG, "failed parsing " + file + ".XmlPullParserException");
            if (0 != 0) {
                stream.close();
            }
        } catch (IOException e5) {
            Log.w(TAG, "failed parsing " + file + ".IOException");
            if (0 != 0) {
                stream.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    stream.close();
                } catch (IOException e6) {
                    Log.d(TAG, "parseShellConfig stream close FAIL!");
                }
            }
            throw th;
        }
        Iterator<ShellItem> i = this.mShellIdentifies.iterator();
        while (i.hasNext()) {
            if (i.next().getFiles().size() == 0) {
                i.remove();
            }
        }
    }

    private void parseXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        ShellItem currentShell = null;
        List<ShellItem> shellItems = null;
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type == 2) {
                String tag = parser.getName();
                int depth = parser.getDepth();
                if (depth == 0) {
                    continue;
                } else if (depth != 1) {
                    if (depth == 2) {
                        shellItems = getShellItem(tag);
                    } else if (depth != 3) {
                        if (depth != 4) {
                            Log.e(TAG, "Parse Shell Identify Xml Reach a invalid Depth: " + String.valueOf(0));
                        } else if (currentShell == null) {
                            Log.e(TAG, "Parsing Shell Identify Xml, Find File Tag before a Shell Tag.");
                        } else if ("File".equals(tag) || "Packagename".equals(tag)) {
                            currentShell.getFiles().add(parser.nextText());
                        } else {
                            Log.e(TAG, "Find an unknown Tag: " + tag);
                        }
                    } else if (shellItems != null) {
                        currentShell = addShellItem(tag, shellItems);
                    } else {
                        Log.e(TAG, "Parsing Shell Identify Xml, Find an unknown Tag " + tag + " before a InfoType Tag.");
                        currentShell = null;
                    }
                } else if (!"shell".equals(tag)) {
                    Log.w(TAG, "invalid file: /system/etc/dexopt/shell_identify_and_oat_clean.xml");
                    return;
                }
            }
        }
    }

    private List<ShellItem> getShellItem(String tag) {
        if (INFO_TYPE_SHELL_IDENTIFY.equals(tag)) {
            return this.mShellIdentifies;
        }
        if (INFO_TYPE_SHELL_CLEAR_OAT.equals(tag)) {
            return this.mShellClearOats;
        }
        if (INFO_TYPE_SHELL_BY_PACKAHE_NAME.equals(tag)) {
            return this.mShellByPackageNames;
        }
        Log.e(TAG, "Parsing Shell Identify Xml, Find an unknown infoType: " + tag);
        return null;
    }

    @SuppressLint({"PreferForInArrayList"})
    private ShellItem addShellItem(String shellName, List<ShellItem> shellItems) {
        for (ShellItem shellItem : shellItems) {
            if (shellItem.getName().equals(shellName)) {
                return shellItem;
            }
        }
        ShellItem newShell = new ShellItem(shellName, new ArrayList());
        shellItems.add(newShell);
        return newShell;
    }

    @SuppressLint({"PreferForInArrayList"})
    private List<ShellItem> deepCopyShellItem(List<ShellItem> oriShellItems) {
        if (oriShellItems == null) {
            return new ArrayList(0);
        }
        List<ShellItem> shellItems = new ArrayList<>();
        for (ShellItem oriShellItem : oriShellItems) {
            ShellItem shellItem = new ShellItem(oriShellItem.getName(), new ArrayList());
            Iterator<String> it = oriShellItem.getFiles().iterator();
            while (it.hasNext()) {
                shellItem.getFiles().add(it.next());
            }
            shellItems.add(shellItem);
        }
        return shellItems;
    }
}
