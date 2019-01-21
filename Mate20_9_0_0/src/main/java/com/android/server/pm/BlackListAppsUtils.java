package com.android.server.pm;

import android.annotation.SuppressLint;
import android.content.pm.PackageParser.Package;
import android.content.pm.Signature;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.ZipFile;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class BlackListAppsUtils {
    private static final String APP_BLACKLIST_FILE_NAME = "app_blacklist.xml";
    private static final String DISABLED_APP_FILE_NAME = "disabled_app.xml";
    private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
    private static final String TAG = "BlackListAppsUtils";

    public static void writeToXml(XmlSerializer out, BlackListApp app) throws IllegalArgumentException, IllegalStateException, IOException {
        if (out != null && app != null) {
            out.startTag(null, "app");
            out.startTag(null, "packageName");
            out.text(app.mPackageName);
            out.endTag(null, "packageName");
            Signature[] signs = app.mSignature;
            boolean versionIsValid = false;
            if (!(signs == null || signs.length == 0)) {
                out.startTag(null, "signature");
                StringBuilder signsString = new StringBuilder();
                for (Signature sigStr : signs) {
                    if (!TextUtils.isEmpty(signsString)) {
                        signsString.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                    }
                    signsString.append(sigStr.toCharsString());
                }
                out.text(signsString.toString());
                out.endTag(null, "signature");
            }
            if ((app.mMinVersionId | app.mMaxVersionId) != 0) {
                versionIsValid = true;
            }
            if (versionIsValid) {
                out.startTag(null, "versionIDs");
                out.startTag(null, "versionIDStart");
                out.text(String.valueOf(app.mMinVersionId));
                out.endTag(null, "versionIDStart");
                out.startTag(null, "versionIDEnd");
                out.text(String.valueOf(app.mMaxVersionId));
                out.endTag(null, "versionIDEnd");
                out.endTag(null, "versionIDs");
            }
            if (!TextUtils.isEmpty(app.mHashValue)) {
                out.startTag(null, "hashValue");
                out.text(app.mHashValue);
                out.endTag(null, "hashValue");
            }
            out.endTag(null, "app");
        }
    }

    @SuppressLint({"PreferForInArrayList"})
    public static boolean writeBlackListToXml(BlackListInfo disableApp) {
        if (disableApp == null) {
            return false;
        }
        FileOutputStream fis = null;
        try {
            fis = new FileOutputStream(Environment.buildPath(Environment.getDataDirectory(), new String[]{"system", DISABLED_APP_FILE_NAME}), false);
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fis, "utf-8");
            out.startDocument(null, Boolean.valueOf(true));
            out.startTag(null, "blacklist");
            XmlUtils.writeIntAttribute(out, "version", disableApp.mVersionCode);
            Iterator it = disableApp.mBlackList.iterator();
            while (it.hasNext()) {
                writeToXml(out, (BlackListApp) it.next());
            }
            out.endTag(null, "blacklist");
            out.endDocument();
            fis.flush();
            try {
                fis.close();
            } catch (IOException e) {
                Slog.e(TAG, "close OutputStream failed when write blacklist file");
            }
            return true;
        } catch (IOException e2) {
            Slog.e(TAG, "write disabled app file failed due to IOException");
            deleteDisableAppListFile();
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e3) {
                    Slog.e(TAG, "close OutputStream failed when write blacklist file");
                }
            }
            return false;
        } catch (Throwable th) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e4) {
                    Slog.e(TAG, "close OutputStream failed when write blacklist file");
                }
            }
        }
    }

    public static boolean deleteDisableAppListFile() {
        File file = Environment.buildPath(Environment.getDataDirectory(), new String[]{"system", DISABLED_APP_FILE_NAME});
        if (file == null || !file.exists()) {
            return false;
        }
        return file.delete();
    }

    public static void readDisableAppList(BlackListInfo info) {
        readBlackListFromXml(info, Environment.buildPath(Environment.getDataDirectory(), new String[]{"system", DISABLED_APP_FILE_NAME}));
    }

    public static void readBlackList(BlackListInfo info) {
        readBlackListFromXml(info, Environment.buildPath(Environment.getRootDirectory(), new String[]{"etc", "xml", APP_BLACKLIST_FILE_NAME}));
    }

    @SuppressLint({"PreferForInArrayList"})
    private static void readBlackListFromXml(BlackListInfo info, File file) {
        if (info != null && file != null && file.exists()) {
            FileInputStream fis = null;
            BlackListApp app = null;
            ArrayList<BlackListApp> blacklist = new ArrayList();
            int version = -1;
            boolean parserError = false;
            try {
                fis = new FileInputStream(file);
                XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
                xpp.setInput(fis, "UTF-8");
                for (int type = xpp.getEventType(); type != 1; type = xpp.next()) {
                    if (type != 0) {
                        switch (type) {
                            case 2:
                                String name = xpp.getName();
                                if (!TextUtils.equals("blacklist", name)) {
                                    if (!TextUtils.equals("app", name)) {
                                        if (!TextUtils.equals("packageName", name) || app == null) {
                                            if (!TextUtils.equals("signature", name) || app == null) {
                                                int i;
                                                if (!TextUtils.equals("versionIDStart", name) || app == null) {
                                                    if (!TextUtils.equals("versionIDEnd", name) || app == null) {
                                                        if (TextUtils.equals("hashValue", name) && app != null) {
                                                            app.mHashValue = xpp.nextText();
                                                            break;
                                                        }
                                                    }
                                                    i = 0;
                                                    try {
                                                        app.mMaxVersionId = Integer.parseInt(xpp.nextText());
                                                        break;
                                                    } catch (NumberFormatException e) {
                                                        parserError = true;
                                                        break;
                                                    }
                                                }
                                                i = 0;
                                                try {
                                                    app.mMinVersionId = Integer.parseInt(xpp.nextText());
                                                    break;
                                                } catch (NumberFormatException e2) {
                                                    parserError = true;
                                                    break;
                                                }
                                            }
                                            try {
                                                app.setSignature(xpp.nextText().split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER));
                                                break;
                                            } catch (Exception e3) {
                                                parserError = true;
                                                break;
                                            }
                                        }
                                        app.mPackageName = xpp.nextText();
                                        break;
                                    }
                                    app = new BlackListApp();
                                    break;
                                }
                                try {
                                    version = Integer.parseInt(xpp.getAttributeValue(0));
                                    break;
                                } catch (NumberFormatException e4) {
                                    parserError = true;
                                    break;
                                }
                                break;
                            case 3:
                                if (TextUtils.equals("app", xpp.getName())) {
                                    blacklist.add(app);
                                    app = null;
                                    break;
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    if (parserError) {
                        try {
                            fis.close();
                        } catch (IOException e5) {
                            Slog.e(TAG, "close InputStream failed when read blacklist file");
                        }
                        return;
                    }
                }
                try {
                    fis.close();
                } catch (IOException e6) {
                    Slog.e(TAG, "close InputStream failed when read blacklist file");
                }
            } catch (IOException e7) {
                Slog.e(TAG, "read blacklist file failed due to IOException");
                parserError = true;
                if (fis != null) {
                    fis.close();
                }
            } catch (XmlPullParserException e8) {
                Slog.e(TAG, "read blacklist file failed due to XmlPullParserException");
                if (fis != null) {
                    fis.close();
                }
            } catch (Throwable th) {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e9) {
                        Slog.e(TAG, "close InputStream failed when read blacklist file");
                    }
                }
            }
            if (checkAppInfo(blacklist) && version >= 0 && !parserError) {
                info.mBlackList.clear();
                Iterator it = blacklist.iterator();
                while (it.hasNext()) {
                    info.mBlackList.add((BlackListApp) it.next());
                }
                info.mVersionCode = version;
            }
        }
    }

    public static boolean comparePackage(Package info, BlackListApp app) {
        if (info == null || app == null || !TextUtils.equals(info.packageName, app.mPackageName)) {
            return false;
        }
        int versionStart = app.mMinVersionId;
        int versionEnd = app.mMaxVersionId;
        if (((versionStart | versionEnd) != 0) && (info.mVersionCode < versionStart || info.mVersionCode > versionEnd)) {
            return false;
        }
        if (app.mSignature != null && PackageManagerServiceUtils.compareSignatures(info.mSigningDetails.signatures, app.mSignature) != 0) {
            return false;
        }
        if (!TextUtils.isEmpty(app.mHashValue)) {
            if (!TextUtils.equals(app.mHashValue, getSHA256(new File(info.applicationInfo.sourceDir)))) {
                return false;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("find blacklist apk : ");
        stringBuilder.append(app.mPackageName);
        Slog.d(str, stringBuilder.toString());
        return true;
    }

    private static String getSHA256(File file) {
        byte[] manifest = getManifestFile(file);
        if (manifest.length == 0) {
            return null;
        }
        return sha256(manifest);
    }

    private static byte[] getManifestFile(File apkFile) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        ZipFile zipFile = null;
        boolean catchFlag = false;
        InputStream zipInputStream = null;
        try {
            zipFile = new ZipFile(apkFile);
            zipInputStream = zipFile.getInputStream(zipFile.getEntry("META-INF/MANIFEST.MF"));
            if (zipInputStream != null) {
                while (true) {
                    int read = zipInputStream.read(b);
                    int length = read;
                    if (read <= 0) {
                        break;
                    }
                    os.write(b, 0, length);
                }
                catchFlag = true;
            }
            if (zipInputStream != null) {
                try {
                    zipInputStream.close();
                } catch (IOException e) {
                }
            }
            try {
                zipFile.close();
            } catch (IOException e2) {
            }
            return catchFlag ? os.toByteArray() : new byte[0];
        } catch (IOException e3) {
            Slog.e(TAG, " get manifest file failed due to IOException");
            if (zipInputStream != null) {
                try {
                    zipInputStream.close();
                } catch (IOException e4) {
                }
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e5) {
                }
            }
            return null;
        } catch (Throwable th) {
            if (zipInputStream != null) {
                try {
                    zipInputStream.close();
                } catch (IOException e6) {
                }
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e7) {
                }
            }
        }
    }

    public static String sha256(byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data);
            return bytesToString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            Slog.e(TAG, "get sha256 failed");
            return null;
        }
    }

    private static String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        char[] hexChars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] chars = new char[(bytes.length * 2)];
        for (int j = 0; j < bytes.length; j++) {
            int byteValue = bytes[j] & 255;
            chars[j * 2] = hexChars[byteValue >>> 4];
            chars[(j * 2) + 1] = hexChars[byteValue & 15];
        }
        return new String(chars);
    }

    public static boolean isBlackListUpdate(BlackListInfo blacklist, BlackListInfo disableApp) {
        int blacklistVersion = blacklist.mVersionCode;
        int disableAppVersion = disableApp.mVersionCode;
        String str;
        StringBuilder stringBuilder;
        if (disableAppVersion == -1) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("blacklist update, version = ");
            stringBuilder.append(blacklistVersion);
            Slog.d(str, stringBuilder.toString());
            return true;
        } else if (blacklistVersion < disableAppVersion) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("blacklist downgrade: from ");
            stringBuilder.append(disableAppVersion);
            stringBuilder.append(" to ");
            stringBuilder.append(blacklistVersion);
            Slog.d(str, stringBuilder.toString());
            return true;
        } else if (blacklistVersion > disableAppVersion) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("blacklist upgrade: from ");
            stringBuilder.append(disableAppVersion);
            stringBuilder.append(" to ");
            stringBuilder.append(blacklistVersion);
            Slog.d(str, stringBuilder.toString());
            return true;
        } else {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("blacklist version = ");
            stringBuilder2.append(blacklistVersion);
            Slog.d(str2, stringBuilder2.toString());
            return false;
        }
    }

    @SuppressLint({"PreferForInArrayList"})
    private static boolean checkAppInfo(ArrayList<BlackListApp> blacklist) {
        if (blacklist == null) {
            return false;
        }
        Iterator it = blacklist.iterator();
        while (it.hasNext()) {
            BlackListApp app = (BlackListApp) it.next();
            if (TextUtils.isEmpty(app.mPackageName)) {
                return false;
            }
            int count = 0;
            if (app.mSignature != null && app.mSignature.length > 0) {
                count = 0 + 1;
            }
            if (app.mMaxVersionId >= 0 && app.mMinVersionId >= 0 && app.mMaxVersionId >= app.mMinVersionId && (app.mMinVersionId | app.mMaxVersionId) != 0) {
                count++;
            }
            if (!TextUtils.isEmpty(app.mHashValue)) {
                count++;
            }
            if (count < 1) {
                return false;
            }
        }
        return true;
    }

    @SuppressLint({"PreferForInArrayList"})
    public static boolean containsApp(ArrayList<BlackListApp> blacklist, BlackListApp app) {
        if (blacklist == null || app == null) {
            return false;
        }
        Iterator it = blacklist.iterator();
        while (it.hasNext()) {
            BlackListApp blacklistApp = (BlackListApp) it.next();
            if (TextUtils.equals(blacklistApp.mPackageName, app.mPackageName) && Arrays.equals(blacklistApp.mSignature, app.mSignature) && app.mMinVersionId == blacklistApp.mMinVersionId && app.mMaxVersionId == blacklistApp.mMaxVersionId && TextUtils.equals(app.mHashValue, blacklistApp.mHashValue)) {
                return true;
            }
        }
        return false;
    }
}
