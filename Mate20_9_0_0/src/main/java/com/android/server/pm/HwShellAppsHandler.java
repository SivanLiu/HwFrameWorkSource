package com.android.server.pm;

import android.annotation.SuppressLint;
import android.content.pm.PackageParser.Package;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class HwShellAppsHandler {
    private static final String CONFIG_FILE = "/system/etc/dexopt/shell_identify_and_oat_clean.xml";
    private static final boolean DEBUG = false;
    private static final String INFO_TYPE_ShellByPackageName = "shellByPackageName";
    private static final String INFO_TYPE_ShellClearOat = "shellClearOat";
    private static final String INFO_TYPE_ShellIdentify = "shellIdentify";
    private static final String TAG = "HwShellAppsHandler";
    private File mAppDataDir;
    private final Installer mInstaller;
    private ArrayList<ShellItem> mShellByPackageName;
    private ArrayList<ShellItem> mShellClearOats;
    private ArrayList<ShellItem> mShellIdentifies;
    private File mUserAppDataDir;

    private static class ShellItem {
        private ArrayList<String> mFiles;
        private String mName;

        public ShellItem(String name, ArrayList<String> files) {
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

    public HwShellAppsHandler(Installer installer, UserManagerService userManager) {
        this.mShellIdentifies = null;
        this.mShellClearOats = null;
        this.mShellByPackageName = null;
        this.mShellClearOats = new ArrayList();
        this.mShellIdentifies = new ArrayList();
        this.mShellByPackageName = new ArrayList();
        parseShellConfig();
        File dataDir = Environment.getDataDirectory();
        this.mAppDataDir = new File(dataDir, "data");
        this.mUserAppDataDir = new File(dataDir, "user");
        this.mInstaller = installer;
    }

    @SuppressLint({"PreferForInArrayList"})
    public String AnalyseShell(Package pkg) {
        String str;
        StringBuilder stringBuilder;
        ArrayList<ShellItem> tmpShellIdentifies = DeepcopyShellItem(this.mShellIdentifies);
        try {
            ZipFile file = new ZipFile(pkg.baseCodePath);
            try {
                Enumeration<? extends ZipEntry> entries = file.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    if (!entry.isDirectory()) {
                        filterOutIdentifyFiles(entry.getName(), tmpShellIdentifies);
                    }
                }
            } catch (Exception e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("fail to process apk file: ");
                stringBuilder.append(pkg.baseCodePath);
                Log.e(str, stringBuilder.toString());
            }
            file.close();
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("fail to process apk file: ");
            stringBuilder.append(pkg.baseCodePath);
            Log.e(str, stringBuilder.toString());
        }
        Iterator it = tmpShellIdentifies.iterator();
        while (it.hasNext()) {
            ShellItem shellIdentify = (ShellItem) it.next();
            if (shellIdentify.getFiles().size() == 0) {
                return shellIdentify.getName();
            }
        }
        return AnalyseShellByPackageName(pkg);
    }

    @SuppressLint({"PreferForInArrayList"})
    private String AnalyseShellByPackageName(Package pkg) {
        if (this.mShellByPackageName == null || pkg == null || pkg.packageName == null) {
            return null;
        }
        Iterator it = this.mShellByPackageName.iterator();
        while (it.hasNext()) {
            ShellItem shellIdentify = (ShellItem) it.next();
            Iterator<String> i = shellIdentify.getFiles().iterator();
            while (i.hasNext()) {
                String identifyPackageName = (String) i.next();
                if (identifyPackageName != null && identifyPackageName.equals(pkg.packageName)) {
                    return shellIdentify.getName();
                }
            }
        }
        return null;
    }

    public void ProcessShellApp(Package pkg) {
    }

    @SuppressLint({"PreferForInArrayList"})
    private void filterOutIdentifyFiles(String fileName, ArrayList<ShellItem> tmpShellIdentifies) {
        Iterator it = tmpShellIdentifies.iterator();
        while (it.hasNext()) {
            Iterator<String> i = ((ShellItem) it.next()).getFiles().iterator();
            while (i.hasNext()) {
                String identifyFileName = (String) i.next();
                if (!fileName.equals(identifyFileName)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("/");
                    stringBuilder.append(identifyFileName);
                    if (!fileName.endsWith(stringBuilder.toString())) {
                    }
                }
                i.remove();
            }
        }
    }

    private File getDataPathForPackage(String packageName, int userId) {
        if (userId == 0) {
            return new File(this.mAppDataDir, packageName);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mUserAppDataDir.getAbsolutePath());
        stringBuilder.append(File.separator);
        stringBuilder.append(userId);
        stringBuilder.append(File.separator);
        stringBuilder.append(packageName);
        return new File(stringBuilder.toString());
    }

    @SuppressLint({"PreferForInArrayList"})
    public String DumpIdentifies() {
        ShellItem shellItem;
        Iterator it;
        String fileName;
        StringBuffer sb = new StringBuffer();
        sb.append("\nINFO_TYPE_ShellIdentify");
        Iterator it2 = this.mShellIdentifies.iterator();
        while (it2.hasNext()) {
            shellItem = (ShellItem) it2.next();
            sb.append("    ");
            sb.append(shellItem.getName());
            sb.append("\n");
            it = shellItem.getFiles().iterator();
            while (it.hasNext()) {
                fileName = (String) it.next();
                sb.append("        ");
                sb.append(fileName);
                sb.append("\n");
            }
        }
        sb.append("\nINFO_TYPE_ClearOat");
        it2 = this.mShellClearOats.iterator();
        while (it2.hasNext()) {
            shellItem = (ShellItem) it2.next();
            sb.append("    ");
            sb.append(shellItem.getName());
            sb.append("\n");
            it = shellItem.getFiles().iterator();
            while (it.hasNext()) {
                fileName = (String) it.next();
                sb.append("        ");
                sb.append(fileName);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private void parseShellConfig() {
        String str;
        StringBuilder stringBuilder;
        File file = new File("/system/etc", "dexopt/shell_identify_and_oat_clean.xml");
        FileInputStream stream = null;
        try {
            File cfg = HwCfgFilePolicy.getCfgFile("dexopt/shell_identify_and_oat_clean.xml", 0);
            if (cfg != null) {
                file = cfg;
            }
            stream = new FileInputStream(file);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, null);
            ParseXml(parser);
            try {
                stream.close();
            } catch (IOException e) {
                Log.d(TAG, "parseShellConfig stream close FAIL!");
            }
        } catch (NoClassDefFoundError e2) {
            Log.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
            if (stream != null) {
                stream.close();
            }
        } catch (FileNotFoundException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("file is not exist ");
            stringBuilder.append(e3);
            Log.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (XmlPullParserException e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e4);
            Log.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e5) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e5);
            Log.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (Throwable th) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e6) {
                    Log.d(TAG, "parseShellConfig stream close FAIL!");
                }
            }
        }
        Iterator<ShellItem> i = this.mShellIdentifies.iterator();
        while (i.hasNext()) {
            if (((ShellItem) i.next()).getFiles().size() == 0) {
                i.remove();
            }
        }
    }

    private void ParseXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        String tag = "";
        String fileName = "";
        ShellItem currentShell = null;
        ArrayList<ShellItem> currentArray = null;
        while (true) {
            int next = parser.next();
            int type = next;
            if (next != 1) {
                if (2 == type) {
                    tag = parser.getName();
                    String str;
                    StringBuilder stringBuilder;
                    switch (parser.getDepth()) {
                        case 0:
                            break;
                        case 1:
                            if ("shell".equals(tag)) {
                                break;
                            }
                            Log.w(TAG, "invalid file: /system/etc/dexopt/shell_identify_and_oat_clean.xml");
                            return;
                        case 2:
                            if (!INFO_TYPE_ShellIdentify.equals(tag)) {
                                if (!INFO_TYPE_ShellClearOat.equals(tag)) {
                                    if (!INFO_TYPE_ShellByPackageName.equals(tag)) {
                                        currentArray = null;
                                        str = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Parsing Shell Identify Xml, Find an unknown infoType: ");
                                        stringBuilder.append(tag);
                                        Log.e(str, stringBuilder.toString());
                                        break;
                                    }
                                    currentArray = this.mShellByPackageName;
                                    break;
                                }
                                currentArray = this.mShellClearOats;
                                break;
                            }
                            currentArray = this.mShellIdentifies;
                            break;
                        case 3:
                            if (currentArray == null) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Parsing Shell Identify Xml, Find an unknown Tag ");
                                stringBuilder.append(tag);
                                stringBuilder.append(" before a InfoType Tag.");
                                Log.e(str, stringBuilder.toString());
                                currentShell = null;
                                break;
                            }
                            currentShell = AddShellItem(tag, currentArray);
                            break;
                        case 4:
                            if (currentShell != null) {
                                if (!"File".equals(tag) && !"Packagename".equals(tag)) {
                                    str = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Find an unknown Tag: ");
                                    stringBuilder.append(tag);
                                    Log.e(str, stringBuilder.toString());
                                    break;
                                }
                                currentShell.getFiles().add(parser.nextText());
                                break;
                            }
                            Log.e(TAG, "Parsing Shell Identify Xml, Find File Tag before a Shell Tag.");
                            break;
                        default:
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Parse Shell Identify Xml Reach a invalid Depth: ");
                            stringBuilder.append(String.valueOf(0));
                            Log.e(str, stringBuilder.toString());
                            break;
                    }
                }
            } else {
                return;
            }
        }
    }

    @SuppressLint({"PreferForInArrayList"})
    private ShellItem AddShellItem(String shellName, ArrayList<ShellItem> shellItems) {
        Iterator it = shellItems.iterator();
        while (it.hasNext()) {
            ShellItem shellItem = (ShellItem) it.next();
            if (shellItem.getName().equals(shellName)) {
                return shellItem;
            }
        }
        ShellItem newShell = new ShellItem(shellName, new ArrayList());
        shellItems.add(newShell);
        return newShell;
    }

    @SuppressLint({"PreferForInArrayList"})
    private ArrayList<ShellItem> DeepcopyShellItem(ArrayList<ShellItem> oriShellItems) {
        if (oriShellItems == null) {
            return null;
        }
        ArrayList<ShellItem> shellItems = new ArrayList();
        Iterator it = oriShellItems.iterator();
        while (it.hasNext()) {
            ShellItem oriShellItem = (ShellItem) it.next();
            ShellItem shellItem = new ShellItem(oriShellItem.getName(), new ArrayList());
            Iterator it2 = oriShellItem.getFiles().iterator();
            while (it2.hasNext()) {
                shellItem.getFiles().add((String) it2.next());
            }
            shellItems.add(shellItem);
        }
        return shellItems;
    }
}
