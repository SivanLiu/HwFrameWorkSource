package com.android.server.pm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.HwInvisibleAppsFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.Signature;
import android.os.Environment;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.os.ServiceManager;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.devicepolicy.HwDevicePolicyManagerServiceUtil;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import libcore.io.Libcore;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PackagePublicityUtils {
    private static final String HW_PUBLICITY_ALL_DIRS = "/system/etc:/version/etc:cust/etc:product/etc:vendor/etc:preload/etc";
    private static final String HW_PUBLICITY_PERMISSION_PATH = "emui/china/xml/publicity_permission.xml";
    private static Comparator<PackagePublicityInfo> PUBLICITYINFO_COMPARATOR = new Comparator<PackagePublicityInfo>() {
        private final Collator sCollator = Collator.getInstance(Locale.CHINA);

        public int compare(PackagePublicityInfo object1, PackagePublicityInfo object2) {
            int compareResult = this.sCollator.compare(object1.getLabel(), object2.getLabel());
            if (compareResult != 0) {
                return compareResult;
            }
            return this.sCollator.compare(object1.getPackageName(), object2.getPackageName());
        }
    };
    private static final String TAG = "PackagePublicityUtils";
    private static List<String> mHwPlublicityAppList = null;

    @SuppressLint({"PreferForInArrayList"})
    private static List<PackagePublicityInfo> loadPackagePublicityInfo() {
        ArrayList<File> publicityFileList = new ArrayList();
        try {
            publicityFileList = getPublicityFileList("publicity_all.xml");
        } catch (NoClassDefFoundError er) {
            Slog.e(TAG, er.getMessage());
        }
        List<PackagePublicityInfo> publicityInfos = new ArrayList();
        Iterator it = publicityFileList.iterator();
        while (it.hasNext()) {
            File file = (File) it.next();
            if (file != null) {
                if (file.exists()) {
                    publicityInfos.addAll(getPackagePublicityInfoFromCust(file));
                }
            }
        }
        return publicityInfos;
    }

    private static ArrayList<File> getPublicityFileList(String fileName) {
        ArrayList<File> fileList = new ArrayList();
        if (TextUtils.isEmpty(fileName)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: file = [");
            stringBuilder.append(fileName);
            stringBuilder.append("]");
            Log.e(str, stringBuilder.toString());
            return fileList;
        }
        String[] dirs = HW_PUBLICITY_ALL_DIRS.split(":");
        for (String file : dirs) {
            File file2 = new File(file, fileName);
            if (file2.exists()) {
                fileList.add(file2);
            }
        }
        if (fileList.size() == 0) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("No publicity file found for:");
            stringBuilder2.append(fileName);
            Log.w(str2, stringBuilder2.toString());
        }
        return fileList;
    }

    public static List<PackagePublicityInfo> getPackagePublicityInfoFromCust(File file) {
        List<PackagePublicityInfo> result = new ArrayList();
        if (file == null || !file.exists()) {
            return result;
        }
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            XmlPullParser xml = Xml.newPullParser();
            xml.setInput(in, "utf-8");
            while (true) {
                int next = xml.next();
                int xmlEventType = next;
                if (next == 1) {
                    try {
                        break;
                    } catch (IOException e) {
                        Slog.e(TAG, "close FileInputStram error");
                    }
                } else if (xmlEventType == 2 && "packageInfo".equals(xml.getName())) {
                    PackagePublicityInfo packagePublicityInfo = new PackagePublicityInfo();
                    String mPackage = xml.getAttributeValue(null, "package");
                    packagePublicityInfo.setPackageName(mPackage);
                    String label = xml.getAttributeValue(null, HwDevicePolicyManagerServiceUtil.EXCHANGE_LABEL);
                    if (TextUtils.isEmpty(label)) {
                        packagePublicityInfo.setLabel(mPackage);
                    } else {
                        packagePublicityInfo.setLabel(label);
                    }
                    packagePublicityInfo.setFeature(xml.getAttributeValue(null, "feature"));
                    packagePublicityInfo.setAuthor(xml.getAttributeValue(null, "author"));
                    packagePublicityInfo.setIsLauncher(xml.getAttributeValue(null, "launcher"));
                    packagePublicityInfo.setIsUninstall(xml.getAttributeValue(null, "uninstall"));
                    packagePublicityInfo.setPackageFileName(xml.getAttributeValue(null, "packageFileName"));
                    packagePublicityInfo.setUsePermission(xml.getAttributeValue(null, "use-permission"));
                    packagePublicityInfo.setCategory(xml.getAttributeValue(null, "app-category"));
                    packagePublicityInfo.setSignature(xml.getAttributeValue(null, "app-signature"));
                    if (!"".equals(mPackage)) {
                        if (mPackage != null) {
                            result.add(packagePublicityInfo);
                        }
                    }
                }
            }
            in.close();
        } catch (FileNotFoundException e2) {
            Slog.w(TAG, "Error FileNotFound while trying to read from publicity_all.xml", e2);
            if (in != null) {
                in.close();
            }
        } catch (XmlPullParserException e3) {
            Slog.e(TAG, "Error XmlPullParser while trying to read from publicity_all.xml", e3);
            if (in != null) {
                in.close();
            }
        } catch (IOException e4) {
            Slog.e(TAG, "Error while trying to read from publicity_all.xml", e4);
            if (in != null) {
                in.close();
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e5) {
                    Slog.e(TAG, "close FileInputStram error");
                }
            }
        }
        return result;
    }

    public static void writeAllPakcagePublicityInfoIntoFile(Context context, ParceledListSlice<ApplicationInfo> slice) {
        IOException e;
        IOException e2;
        StringBuilder stringBuilder;
        String str;
        StringBuilder stringBuilder2;
        File file = getPublicityFile();
        if (file != null && !file.exists()) {
            List<PackagePublicityInfo> allPackagePublicityInfo = loadPackagePublicityInfo();
            if (allPackagePublicityInfo.size() != 0) {
                handlePublicityInfos(context, slice, allPackagePublicityInfo);
                FileOutputStream out = null;
                BufferedOutputStream str2 = null;
                try {
                    out = new FileOutputStream(file);
                    str2 = new BufferedOutputStream(out);
                    XmlSerializer serializer = new FastXmlSerializer();
                    serializer.setOutput(str2, "utf-8");
                    serializer.startDocument(null, Boolean.valueOf(true));
                    serializer.startTag(null, "packageList");
                    for (PackagePublicityInfo info : allPackagePublicityInfo) {
                        serializer.startTag(null, "packageInfo");
                        serializer.attribute(null, "package", info.getPackageName());
                        serializer.attribute(null, HwDevicePolicyManagerServiceUtil.EXCHANGE_LABEL, info.getLabel());
                        serializer.attribute(null, "feature", info.getFeature());
                        serializer.attribute(null, "author", info.getAuthor());
                        serializer.attribute(null, "launcher", info.getIsLauncher());
                        serializer.attribute(null, "uninstall", info.getIsUninstall());
                        serializer.attribute(null, "packageFileName", info.getPackageFileName());
                        serializer.attribute(null, "use-permission", info.getUsePermission());
                        serializer.attribute(null, "app-category", info.getCategory());
                        serializer.attribute(null, "app-signature", info.getSignature());
                        serializer.endTag(null, "packageInfo");
                    }
                    serializer.endTag(null, "packageList");
                    serializer.endDocument();
                    str2.flush();
                    FileUtils.sync(out);
                    try {
                        str2.close();
                        out.close();
                        FileUtils.setPermissions(file.toString(), 432, -1, -1);
                    } catch (IOException e3) {
                        e = e3;
                        e2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error close writing package manager settings");
                        stringBuilder.append(e.getMessage());
                        Slog.e(e2, stringBuilder.toString());
                    }
                } catch (FileNotFoundException e4) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("File not found when writing pakcagePublicity file: ");
                    stringBuilder2.append(e4.getMessage());
                    Slog.e(str, stringBuilder2.toString());
                    if (str2 != null) {
                        try {
                            str2.close();
                        } catch (IOException e5) {
                            e = e5;
                            e2 = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error close writing package manager settings");
                            stringBuilder.append(e.getMessage());
                            Slog.e(e2, stringBuilder.toString());
                        }
                    }
                    if (out != null) {
                        out.close();
                    }
                    FileUtils.setPermissions(file.toString(), 432, -1, -1);
                } catch (IllegalArgumentException e6) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("IllegalArgument when writing pakcagePublicity file: ");
                    stringBuilder2.append(e6.getMessage());
                    Slog.e(str, stringBuilder2.toString());
                    if (str2 != null) {
                        try {
                            str2.close();
                        } catch (IOException e7) {
                            e = e7;
                            e2 = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error close writing package manager settings");
                            stringBuilder.append(e.getMessage());
                            Slog.e(e2, stringBuilder.toString());
                        }
                    }
                    if (out != null) {
                        out.close();
                    }
                    FileUtils.setPermissions(file.toString(), 432, -1, -1);
                } catch (IOException e8) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("IOException when writing pakcagePublicity file: ");
                    stringBuilder2.append(e8.getMessage());
                    Slog.e(str, stringBuilder2.toString());
                    if (str2 != null) {
                        try {
                            str2.close();
                        } catch (IOException e9) {
                            e8 = e9;
                            e2 = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error close writing package manager settings");
                            stringBuilder.append(e8.getMessage());
                            Slog.e(e2, stringBuilder.toString());
                        }
                    }
                    if (out != null) {
                        out.close();
                    }
                    FileUtils.setPermissions(file.toString(), 432, -1, -1);
                } catch (Throwable th) {
                    if (str2 != null) {
                        try {
                            str2.close();
                        } catch (IOException e22) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error close writing package manager settings");
                            stringBuilder.append(e22.getMessage());
                            Slog.e(TAG, stringBuilder.toString());
                        }
                    }
                    if (out != null) {
                        out.close();
                    }
                    FileUtils.setPermissions(file.toString(), 432, -1, -1);
                }
            }
        }
    }

    public static File getPublicityFile() {
        File publicityFile = null;
        try {
            File systemDir = new File(Environment.getDataDirectory(), "system");
            if (systemDir.exists() || systemDir.mkdirs()) {
                publicityFile = new File(systemDir, "publicity_all.xml");
                if (publicityFile.exists()) {
                    return null;
                }
                Slog.i(TAG, "first boot. init publicity_all.xml");
                return publicityFile;
            }
            Slog.i(TAG, "PakcagePublicity file create error");
            return null;
        } catch (SecurityException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PakcagePublicity file SecurityException: ");
            stringBuilder.append(e.getMessage());
            Slog.i(str, stringBuilder.toString());
        }
    }

    public static void deletePublicityFile() {
        try {
            File publicityFile = new File(Environment.getDataDirectory(), "system/publicity_all.xml");
            if (publicityFile.exists() && publicityFile.delete()) {
                Slog.i(TAG, "Delete publicity file ...");
            }
        } catch (SecurityException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Delete publicity file error: ");
            stringBuilder.append(e.getMessage());
            Slog.i(str, stringBuilder.toString());
        }
    }

    private static void handlePublicityInfos(Context context, ParceledListSlice<ApplicationInfo> slice, List<PackagePublicityInfo> pkgPubInfo) {
        filterPublicityInfos(new HwInvisibleAppsFilter(context), slice, pkgPubInfo, context.getPackageManager());
        sortPackagePublicityInfos(pkgPubInfo);
    }

    private static void initPubPermissions(List<String> pubPermissionGroup, List<String> pubPermission) {
        File file = new File("system", HW_PUBLICITY_PERMISSION_PATH);
        if (file.exists()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                XmlPullParser xml = Xml.newPullParser();
                xml.setInput(in, "utf-8");
                while (true) {
                    int next = xml.next();
                    int xmlEventType = next;
                    if (next == 1) {
                        try {
                            break;
                        } catch (IOException e) {
                            Log.e(TAG, "close FileInputStram error");
                        }
                    } else if (xmlEventType == 2 && "permissionInfo".equals(xml.getName())) {
                        String group = xml.getAttributeValue(null, "group");
                        String name = xml.getAttributeValue(null, "name");
                        if (!(TextUtils.isEmpty(group) || TextUtils.isEmpty(name))) {
                            pubPermissionGroup.add(group);
                            pubPermission.add(name);
                        }
                    }
                }
                in.close();
            } catch (FileNotFoundException e2) {
                Log.w(TAG, "Error FileNotFound while trying to read from publicity_permission.xml");
                if (in != null) {
                    in.close();
                }
            } catch (XmlPullParserException e3) {
                Log.e(TAG, "Error XmlPullParser while trying to read from publicity_permission.xml", e3);
                if (in != null) {
                    in.close();
                }
            } catch (IOException e4) {
                Log.e(TAG, "Error while trying to read from publicity_permission.xml", e4);
                if (in != null) {
                    in.close();
                }
            } catch (Throwable th) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e5) {
                        Log.e(TAG, "close FileInputStram error");
                    }
                }
            }
        }
    }

    private static void filterPublicityInfos(HwInvisibleAppsFilter filter, ParceledListSlice<ApplicationInfo> slice, List<PackagePublicityInfo> pkgPubInfo, PackageManager pm) {
        List<String> pubPermissionGroup = new ArrayList();
        List<String> pubPermission = new ArrayList();
        initPubPermissions(pubPermissionGroup, pubPermission);
        List<String> sysWhiteList = AntiMalPreInstallScanner.getInstance().getSysWhiteList();
        if (slice == null) {
            Slog.w(TAG, "FiltePublicityInfo: getInstalledApplications is null");
            pkgPubInfo.clear();
            return;
        }
        HwInvisibleAppsFilter hwInvisibleAppsFilter;
        PackageManager packageManager;
        List<String> list;
        List<String> list2;
        if (sysWhiteList == null) {
            hwInvisibleAppsFilter = filter;
            packageManager = pm;
            list = pubPermissionGroup;
            list2 = pubPermission;
            pubPermissionGroup = pkgPubInfo;
        } else if (sysWhiteList.size() == 0) {
            hwInvisibleAppsFilter = filter;
            packageManager = pm;
            list = pubPermissionGroup;
            list2 = pubPermission;
            pubPermissionGroup = pkgPubInfo;
        } else {
            List<ApplicationInfo> allApps = slice.getList();
            if (allApps == null) {
                hwInvisibleAppsFilter = filter;
                packageManager = pm;
                list = pubPermissionGroup;
                list2 = pubPermission;
                pubPermissionGroup = pkgPubInfo;
            } else if (allApps.size() == 0) {
                hwInvisibleAppsFilter = filter;
                packageManager = pm;
                list = pubPermissionGroup;
                list2 = pubPermission;
                pubPermissionGroup = pkgPubInfo;
            } else {
                allApps = filter.filterHideApp(allApps);
                List<String> allInstallAppName = new ArrayList();
                for (ApplicationInfo app : allApps) {
                    allInstallAppName.add(app.packageName);
                }
                List<PackagePublicityInfo> insatlledPkgs = new ArrayList();
                List<String> existPkg = new ArrayList();
                PackageManagerService pms = (PackageManagerService) ServiceManager.getService("package");
                for (PackagePublicityInfo pubInfo : pkgPubInfo) {
                    String pubInfoPkg = pubInfo.getPackageName();
                    if (!existPkg.contains(pubInfoPkg)) {
                        if (allInstallAppName.contains(pubInfoPkg) && sysWhiteList.contains(pubInfoPkg)) {
                            PackageInfo pkgInfo = pms.getPackageInfo(pubInfoPkg, 4096, 0);
                            pubInfo.setUsePermission(getUsePermission(pkgInfo, pubPermissionGroup, pubPermission));
                            String label = pkgInfo.applicationInfo.loadUnsafeLabel(pm).toString();
                            if (TextUtils.isEmpty(label)) {
                                pubInfo.setLabel(pubInfoPkg);
                            } else {
                                pubInfo.setLabel(label);
                            }
                            list = pubPermissionGroup;
                            list2 = pubPermission;
                            pubPermissionGroup = pms.getPackageInfo(pubInfoPkg, 64, null);
                            if (pubPermissionGroup.signatures != null) {
                                pubInfo.setSignature(getSignatureString(pubPermissionGroup.signatures));
                                insatlledPkgs.add(pubInfo);
                                existPkg.add(pubInfoPkg);
                            }
                        } else {
                            packageManager = pm;
                            list = pubPermissionGroup;
                            list2 = pubPermission;
                        }
                        pubPermissionGroup = list;
                        pubPermission = list2;
                    }
                }
                packageManager = pm;
                list = pubPermissionGroup;
                list2 = pubPermission;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("insatlledPkgs size = ");
                stringBuilder.append(insatlledPkgs.size());
                Slog.w(str, stringBuilder.toString());
                pkgPubInfo.clear();
                pkgPubInfo.addAll(insatlledPkgs);
                return;
            }
            pkgPubInfo.clear();
            return;
        }
        Slog.w(TAG, "FiltePublicityInfo: getSysWhiteList is null or isEmpty");
        pkgPubInfo.clear();
    }

    private static String getUsePermission(PackageInfo pkgInfo, List<String> pubPermissionGroup, List<String> pubPermission) {
        if (pkgInfo == null) {
            return "";
        }
        String[] requestedPermissions = pkgInfo.requestedPermissions;
        if (requestedPermissions == null || requestedPermissions.length <= 0) {
            return "";
        }
        StringBuilder usePermissionBuilder = new StringBuilder();
        List<String> usePermissionGroup = new ArrayList();
        List<String> requestedPermissionList = Arrays.asList(requestedPermissions);
        for (String permission : pubPermission) {
            if (requestedPermissionList.contains(permission)) {
                String prermissionGroup = (String) pubPermissionGroup.get(pubPermission.indexOf(permission));
                if (!usePermissionGroup.contains(prermissionGroup)) {
                    usePermissionGroup.add(prermissionGroup);
                    usePermissionBuilder.append(prermissionGroup);
                    usePermissionBuilder.append(",");
                }
            }
        }
        String usePermission = usePermissionBuilder.toString();
        if (!TextUtils.isEmpty(usePermission) && usePermission.endsWith(",")) {
            usePermission = usePermission.substring(0, usePermission.length() - 1);
        }
        return usePermission;
    }

    private static void sortPackagePublicityInfos(List<PackagePublicityInfo> pkgPubInfo) {
        if (pkgPubInfo != null && pkgPubInfo.size() > 0 && PUBLICITYINFO_COMPARATOR != null) {
            Collections.sort(pkgPubInfo, PUBLICITYINFO_COMPARATOR);
            PUBLICITYINFO_COMPARATOR = null;
        }
    }

    public static List<String> getHwPublicityAppList(Context context) {
        if (mHwPlublicityAppList == null) {
            File publicityFile = new File(Environment.getDataDirectory(), "system/publicity_all.xml");
            if (publicityFile.exists()) {
                mHwPlublicityAppList = new ArrayList();
                List<PackagePublicityInfo> readResult = getPackagePublicityInfoFromCust(publicityFile);
                PackageManagerService pms = (PackageManagerService) ServiceManager.getService("package");
                PackageManager pm = context.getPackageManager();
                List<String> sysWhiteList = AntiMalPreInstallScanner.getInstance().getSysWhiteList();
                if (sysWhiteList == null || sysWhiteList.size() == 0) {
                    Slog.w(TAG, "FiltePublicityInfo: getSysWhiteList is null or isEmpty");
                    return mHwPlublicityAppList;
                }
                for (PackagePublicityInfo pp : readResult) {
                    String pkName = pp.getPackageName();
                    if (sysWhiteList.contains(pkName)) {
                        String label = "";
                        String info = "";
                        PackageInfo pkgInfo = pms.getPackageInfo(pkName, 64, 0);
                        if (pkgInfo == null) {
                            label = pp.getLabel();
                        } else if (pkgInfo.signatures != null) {
                            if (pkgInfo.applicationInfo != null) {
                                if (pp.getSignature().equals(getSignatureString(pkgInfo.signatures))) {
                                    label = pkgInfo.applicationInfo.loadUnsafeLabel(pm).toString();
                                    if (TextUtils.isEmpty(label)) {
                                        label = pkName;
                                    }
                                }
                            }
                        }
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(pkName);
                        stringBuilder.append("+++++");
                        stringBuilder.append(label);
                        mHwPlublicityAppList.add(stringBuilder.toString());
                    }
                }
            }
        }
        return mHwPlublicityAppList;
    }

    private static String getSignatureString(Signature[] signatures) {
        StringBuilder signatureStrBuilder = new StringBuilder();
        for (Signature toCharsString : signatures) {
            signatureStrBuilder.append(toCharsString.toCharsString());
            signatureStrBuilder.append(",");
        }
        String signatureStr = signatureStrBuilder.toString();
        if (TextUtils.isEmpty(signatureStr) || !signatureStr.endsWith(",")) {
            return signatureStr;
        }
        return signatureStr.substring(0, signatureStr.length() - 1);
    }

    public static ParcelFileDescriptor getHwPublicityAppParcelFileDescriptor() {
        try {
            File target = new File(Environment.getDataDirectory(), "system/publicity_all.xml");
            if (target.exists()) {
                return new ParcelFileDescriptor(Libcore.os.open(target.getAbsolutePath(), OsConstants.O_RDONLY, 0));
            }
            return null;
        } catch (ErrnoException e) {
            Slog.w(TAG, "getHwPlublicityAppParcelFileDescriptor file not found .");
            return null;
        }
    }
}
