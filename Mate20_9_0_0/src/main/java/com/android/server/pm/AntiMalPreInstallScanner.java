package com.android.server.pm;

import android.content.Context;
import android.content.pm.PackageParser.Package;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.server.security.securitydiagnose.AntiMalApkInfo;
import com.android.server.security.securitydiagnose.HwSecDiagnoseConstant;
import com.android.server.wifipro.WifiProCommonUtils;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;

public class AntiMalPreInstallScanner {
    private static final int CACHE_SIZE = 1024;
    private static final boolean CHINA_RELEASE_VERSION = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static final String[] COMPONENT_ARRY = new String[]{"system/etc/", "version/", "product/etc/", "cust/", "preload/etc/"};
    private static final boolean DEBUG = false;
    private static final String ENCRYPT_ARG = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";
    private static final boolean HW_DEBUG;
    private static final String KEY_ALGORITHM = "RSA";
    private static final String PATH_SLANT = "/";
    private static final String PKGLIST_SIGN_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArO6tIeIxD78HrazoYkAeKdXhKINVUE1fAwnXb6OiabTYf3qO22wcFoqyKsm2tlaWrDU8+hnxjkZOLIvpcJ0bEDAkICoIRNGBoJzJzN6PyIOyfLd4IOA/bS071jaA5JjLGpMkBKYuhzECnK/pmruKngl3ED/t8HRw44ku1rabcwJjKl8dF4D0ogoosrr8mrwfnQaJpkmTL1oScF/Mr4plkrUdw3Ab00HZoklMVznT+M5KV8DmEjo8PIYkdFlJCwEx4Cj6PXKHfBEGeivyPe2W1/EnYdaREu4GO9ZLBsIhRhS3b7UY5UFsjbYBK23M4zrpZlMVQer4zyqmzefs25BYAwIDAQAB";
    private static final String[] PREINSTALL_APK_DIR = new String[]{"/system/", "/oem/app/", "/version/", "/product/", "/cust/", "/preload/"};
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String SYSAPK_SIGN_PATH = "xml/sign";
    private static final String SYSAPK_WHITE_LIST_PATH = "xml/criticalpro.xml";
    private static final String TAG = "AntiMalPreInstallScanner";
    private static Context mContext;
    private static AntiMalPreInstallScanner mInstance;
    private static boolean mIsOtaBoot;
    private HashMap<String, ApkBasicInfo> mApkInfoList = new HashMap();
    private AntiMalDataManager mDataManager = new AntiMalDataManager(mIsOtaBoot);
    private long mDeviceFirstUseTime;
    private boolean mNeedScan = this.mDataManager.needScanIllegalApks();
    private HashMap<String, AntiMalApkInfo> mOldIllegalApks = new HashMap();
    private AntiMalPreInstallReport mReport = new AntiMalPreInstallReport(this.mDataManager);
    private HashMap<String, ArrayList<ApkBasicInfo>> mSysApkWhitelist = new HashMap();

    private static class ApkBasicInfo {
        public boolean mExist = false;
        public final String[] mHashCode;
        public final String mPackagename;
        public final String mPath;
        public final String mVersion;

        ApkBasicInfo(String packagename, String path, String[] hashCodeArry, String version) {
            this.mPackagename = packagename;
            this.mPath = path;
            this.mHashCode = hashCodeArry;
            this.mVersion = version;
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HW_DEBUG = z;
    }

    public static void init(Context contxt, boolean isOtaBoot) {
        mContext = contxt;
        mIsOtaBoot = isOtaBoot;
        if (HW_DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("init isOtaBoot = ");
            stringBuilder.append(isOtaBoot);
            Slog.d(str, stringBuilder.toString());
        }
    }

    public static AntiMalPreInstallScanner getInstance() {
        AntiMalPreInstallScanner antiMalPreInstallScanner;
        synchronized (AntiMalPreInstallScanner.class) {
            if (mInstance == null) {
                mInstance = new AntiMalPreInstallScanner();
            }
            antiMalPreInstallScanner = mInstance;
        }
        return antiMalPreInstallScanner;
    }

    private AntiMalPreInstallScanner() {
        loadOldAntiMalList();
    }

    public void systemReady() {
        if (CHINA_RELEASE_VERSION) {
            if (this.mNeedScan) {
                checkDeletedIllegallyApks();
            }
            this.mReport.report(null);
        }
    }

    public List<String> getSysWhiteList() {
        if (this.mApkInfoList != null && !this.mApkInfoList.isEmpty()) {
            return new ArrayList(this.mApkInfoList.keySet());
        }
        if (!CHINA_RELEASE_VERSION) {
            return null;
        }
        long timeStart = System.currentTimeMillis();
        for (String componentPath : COMPONENT_ARRY) {
            if (HW_DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getSysWhitelist componentPath ");
                stringBuilder.append(componentPath);
                Log.d(str, stringBuilder.toString());
            }
            File apkListFile = new File(componentPath, SYSAPK_WHITE_LIST_PATH);
            if (apkListFile.exists()) {
                if (HW_DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getSysWhitelist the white list file = ");
                    stringBuilder2.append(apkListFile.getAbsolutePath());
                    Slog.d(str2, stringBuilder2.toString());
                }
                File signFile = new File(componentPath, SYSAPK_SIGN_PATH);
                if (signFile.exists()) {
                    if (!verify(fileToByte(apkListFile), PKGLIST_SIGN_PUBLIC_KEY, readFromFile(signFile))) {
                        Slog.w(TAG, "getSysWhitelist System package list verify failed");
                    } else if (!parsePackagelist(apkListFile)) {
                        Slog.w(TAG, "getSysWhitelist Sign verified, but parsing whitelist failed");
                    }
                } else if (HW_DEBUG) {
                    Slog.w(TAG, "Apk sign File does not exist");
                }
            } else if (HW_DEBUG) {
                Slog.w(TAG, "Apk List File does not exist");
            }
        }
        if (HW_DEBUG) {
            long end = System.currentTimeMillis();
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("getSysWhiteList TIME = ");
            stringBuilder3.append(end - timeStart);
            Slog.d(str3, stringBuilder3.toString());
        }
        return new ArrayList(this.mApkInfoList.keySet());
    }

    private void loadOldAntiMalList() {
        ArrayList<AntiMalApkInfo> oldList = this.mDataManager.getOldApkInfoList();
        if (oldList != null) {
            synchronized (this.mOldIllegalApks) {
                Iterator it = oldList.iterator();
                while (it.hasNext()) {
                    AntiMalApkInfo ai = (AntiMalApkInfo) it.next();
                    if (ai.mType == 1 || ai.mType == 2) {
                        this.mOldIllegalApks.put(ai.mPackageName, ai);
                    }
                }
            }
        }
    }

    public void loadSysWhitelist() {
        Exception e;
        String str;
        StringBuilder stringBuilder;
        if (!CHINA_RELEASE_VERSION) {
            return;
        }
        if (this.mNeedScan) {
            String str2;
            long timeStart = System.currentTimeMillis();
            for (String componentPath : COMPONENT_ARRY) {
                if (HW_DEBUG) {
                    str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("loadSysWhitelist componentPath ");
                    stringBuilder2.append(componentPath);
                    Log.d(str2, stringBuilder2.toString());
                }
                File apkListFile = new File(componentPath, SYSAPK_WHITE_LIST_PATH);
                AntiMalComponentInfo aci = new AntiMalComponentInfo(componentPath);
                StringBuilder stringBuilder3;
                if (apkListFile.exists()) {
                    if (HW_DEBUG) {
                        str2 = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("loadSysWhitelist the white list file = ");
                        stringBuilder3.append(apkListFile.getAbsolutePath());
                        Slog.d(str2, stringBuilder3.toString());
                    }
                    File signFile = new File(componentPath, SYSAPK_SIGN_PATH);
                    StringBuilder stringBuilder4;
                    if (signFile.exists()) {
                        try {
                            if (verify(fileToByte(apkListFile), PKGLIST_SIGN_PUBLIC_KEY, readFromFile(signFile))) {
                                try {
                                    aci.setVerifyStatus(0);
                                    if (!parsePackagelist(apkListFile)) {
                                        aci.setVerifyStatus(4);
                                        str2 = TAG;
                                        stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("loadSysWhitelist Sign verified, but parsing whitelist failed, componentPath = ");
                                        stringBuilder4.append(componentPath);
                                        Slog.e(str2, stringBuilder4.toString());
                                    }
                                } catch (Exception e2) {
                                    e = e2;
                                    str = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("loadSysWhitelist Exception failed: ");
                                    stringBuilder.append(e);
                                    Slog.e(str, stringBuilder.toString());
                                    aci.setVerifyStatus(3);
                                    this.mDataManager.addComponentInfo(aci);
                                }
                            } else {
                                String str3 = TAG;
                                StringBuilder stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("loadSysWhitelist System package list verify failed componentPath = ");
                                stringBuilder5.append(componentPath);
                                Slog.e(str3, stringBuilder5.toString());
                                aci.setVerifyStatus(3);
                                this.mDataManager.addComponentInfo(aci);
                            }
                        } catch (Exception e3) {
                            e = e3;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("loadSysWhitelist Exception failed: ");
                            stringBuilder.append(e);
                            Slog.e(str, stringBuilder.toString());
                            aci.setVerifyStatus(3);
                            this.mDataManager.addComponentInfo(aci);
                        }
                    } else {
                        str2 = TAG;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("loadSysWhitelist sign not exist! componentPath = ");
                        stringBuilder4.append(componentPath);
                        Slog.e(str2, stringBuilder4.toString());
                        aci.setVerifyStatus(2);
                        this.mDataManager.addComponentInfo(aci);
                    }
                } else {
                    str2 = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("loadSysWhitelist criticalpro not exist. componentPath = ");
                    stringBuilder3.append(componentPath);
                    Slog.e(str2, stringBuilder3.toString());
                    aci.setVerifyStatus(1);
                }
                this.mDataManager.addComponentInfo(aci);
            }
            if (HW_DEBUG) {
                long end = System.currentTimeMillis();
                str2 = TAG;
                StringBuilder stringBuilder6 = new StringBuilder();
                stringBuilder6.append("loadSysWhitelist TIME = ");
                stringBuilder6.append(end - timeStart);
                Slog.d(str2, stringBuilder6.toString());
            }
            return;
        }
        if (HW_DEBUG) {
            Slog.d(TAG, "loadSysWhitelist no need load!");
        }
    }

    private byte[] fileToByte(File file) {
        String str;
        StringBuilder stringBuilder;
        byte[] data = null;
        FileInputStream in = null;
        ByteArrayOutputStream out = null;
        if (file.exists()) {
            try {
                in = new FileInputStream(file);
                out = new ByteArrayOutputStream(2048);
                byte[] cache = new byte[1024];
                int nRead = 0;
                while (true) {
                    int read = in.read(cache);
                    nRead = read;
                    if (read == -1) {
                        break;
                    }
                    out.write(cache, 0, nRead);
                }
                out.flush();
                data = out.toByteArray();
            } catch (FileNotFoundException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("fileToByte FileNotFoundException ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            } catch (IOException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("fileToByte IOException! ");
                stringBuilder.append(e2);
                Slog.e(str, stringBuilder.toString());
            } catch (Throwable th) {
                IoUtils.closeQuietly(null);
                IoUtils.closeQuietly(null);
            }
            IoUtils.closeQuietly(in);
            IoUtils.closeQuietly(out);
        }
        return data;
    }

    private String readFromFile(File file) {
        String str;
        StringBuilder stringBuilder;
        StringBuffer readBuf = new StringBuffer();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            for (String data = br.readLine(); data != null; data = br.readLine()) {
                readBuf.append(data);
            }
        } catch (FileNotFoundException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("readFromFile FileNotFoundException :");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("readFromFile IOException :");
            stringBuilder.append(e2);
            Slog.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
        }
        IoUtils.closeQuietly(br);
        return readBuf.toString();
    }

    private boolean verify(byte[] data, String publicKey, String sign) {
        String str;
        StringBuilder stringBuilder;
        if (data == null || data.length == 0 || sign == null) {
            Slog.e(TAG, "verify Input invalid!");
            return false;
        }
        try {
            PublicKey publicK = KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey.getBytes("UTF-8"))));
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicK);
            signature.update(data);
            return signature.verify(Base64.getDecoder().decode(sign.getBytes("UTF-8")));
        } catch (IllegalArgumentException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("verify IllegalArgumentException : ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (UnsupportedEncodingException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("verify UnsupportedEncodingException : ");
            stringBuilder.append(e2);
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (NoSuchAlgorithmException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("verify NoSuchAlgorithmException : ");
            stringBuilder.append(e3);
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (InvalidKeySpecException e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("verify InvalidKeySpecException : ");
            stringBuilder.append(e4);
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (InvalidKeyException e5) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("verify InvalidKeyException : ");
            stringBuilder.append(e5);
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (SignatureException e6) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("verify SignatureException : ");
            stringBuilder.append(e6);
            Slog.e(str, stringBuilder.toString());
            return false;
        }
    }

    private boolean isPreinstallApkDir(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String apkPath = path;
        if (!path.startsWith(PATH_SLANT)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PATH_SLANT);
            stringBuilder.append(path);
            apkPath = stringBuilder.toString();
        }
        for (String str : PREINSTALL_APK_DIR) {
            if (apkPath.contains(str)) {
                return true;
            }
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:14:0x0032 A:{SYNTHETIC, Splitter:B:14:0x0032} */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x0026 A:{Catch:{ Exception -> 0x00c8 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean parsePackagelist(File whitelist) {
        Exception e;
        String str;
        StringBuilder stringBuilder;
        Throwable th;
        File file;
        boolean retVal = true;
        String str2 = null;
        FileInputStream fin = null;
        try {
            XmlPullParser parser = Xml.newPullParser();
            try {
                fin = new FileInputStream(whitelist);
                parser.setInput(fin, "UTF-8");
                int type = 0;
                while (true) {
                    int next = parser.next();
                    type = next;
                    int i = 1;
                    if (next == 2 || type == 1) {
                        if (type == 2) {
                            Slog.e(TAG, "No start tag found in Package white list.");
                            IoUtils.closeQuietly(fin);
                            return false;
                        }
                        int outerDepth = parser.getDepth();
                        while (true) {
                            next = parser.next();
                            type = next;
                            if (next == i || (type == 3 && parser.getDepth() <= outerDepth)) {
                                break;
                            }
                            if (type != 3) {
                                if (type != 4) {
                                    if (parser.getName().equals("package")) {
                                        String packageName = parser.getAttributeValue(str2, "name");
                                        String path = parser.getAttributeValue(str2, HwSecDiagnoseConstant.ANTIMAL_APK_PATH);
                                        String sign = parser.getAttributeValue(str2, "ss");
                                        String version = parser.getAttributeValue(str2, "version");
                                        if (!(TextUtils.isEmpty(packageName) || TextUtils.isEmpty(path))) {
                                            if (!TextUtils.isEmpty(sign)) {
                                                if (isPreinstallApkDir(path)) {
                                                    ApkBasicInfo pbi = new ApkBasicInfo(packageName, path, sign.split(","), version);
                                                    this.mApkInfoList.put(packageName, pbi);
                                                    ArrayList<ApkBasicInfo> plist;
                                                    if (this.mSysApkWhitelist.get(packageName) == null) {
                                                        plist = new ArrayList();
                                                        plist.add(pbi);
                                                        this.mSysApkWhitelist.put(packageName, plist);
                                                    } else {
                                                        plist = (ArrayList) this.mSysApkWhitelist.get(packageName);
                                                        plist.add(pbi);
                                                        this.mSysApkWhitelist.put(packageName, plist);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            str2 = null;
                            i = 1;
                        }
                        IoUtils.closeQuietly(fin);
                        return retVal;
                    }
                }
                if (type == 2) {
                }
            } catch (Exception e2) {
                e = e2;
                try {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("parsePackagelist Error reading Pkg white list: ");
                    stringBuilder.append(e);
                    Slog.e(str, stringBuilder.toString());
                    retVal = false;
                    IoUtils.closeQuietly(fin);
                    return retVal;
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(fin);
                    throw th;
                }
            }
        } catch (Exception e3) {
            e = e3;
            file = whitelist;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("parsePackagelist Error reading Pkg white list: ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            retVal = false;
            IoUtils.closeQuietly(fin);
            return retVal;
        } catch (Throwable th3) {
            th = th3;
            file = whitelist;
            IoUtils.closeQuietly(fin);
            throw th;
        }
    }

    private int stringToInt(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(str);
    }

    private void checkDeletedIllegallyApks() {
        synchronized (this.mApkInfoList) {
            for (ApkBasicInfo abi : this.mApkInfoList.values()) {
                if (!abi.mExist) {
                    if (checkApkExist(abi.mPath)) {
                        abi.mExist = true;
                    } else {
                        AntiMalApkInfo deletedApkInfo = new AntiMalApkInfo(abi.mPackagename, formatPath(abi.mPath), null, 3, null, null, stringToInt(abi.mVersion));
                        if (HW_DEBUG) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("checkDeletedIllegallyApks AntiMalApkInfo : ");
                            stringBuilder.append(deletedApkInfo);
                            Log.d(str, stringBuilder.toString());
                        }
                        setComponentAntiMalStatus(abi.mPath, 4);
                        this.mDataManager.addAntiMalApkInfo(deletedApkInfo);
                    }
                }
            }
        }
    }

    private String formatPath(String path) {
        if (path == null || !path.startsWith(PATH_SLANT)) {
            return path;
        }
        return path.substring(1, path.length());
    }

    private boolean checkApkExist(String path) {
        int i = 0;
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return false;
            }
            int length = files.length;
            while (i < length) {
                File f = files[i];
                if (f != null && isApkPath(f.getAbsolutePath())) {
                    return true;
                }
                i++;
            }
        }
        return isApkPath(path);
    }

    private boolean isApkPath(String path) {
        return path != null && path.endsWith(".apk");
    }

    private void markApkExist(Package pkg) {
        if (pkg != null) {
            synchronized (this.mApkInfoList) {
                ApkBasicInfo abi = (ApkBasicInfo) this.mApkInfoList.get(pkg.packageName);
                if (abi != null) {
                    abi.mExist = true;
                }
            }
        }
    }

    private String sha256(byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data);
            return bytesToString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            if (HW_DEBUG) {
                Slog.e(TAG, "get sha256 failed");
            }
            return null;
        }
    }

    private boolean compareHashcode(String[] hashcode, Package pkg) {
        if (pkg == null || !isApkPath(pkg.baseCodePath) || hashcode == null || hashcode.length == 0) {
            if (HW_DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("compareHashcode not hashcode : ");
                stringBuilder.append(pkg != null ? pkg.baseCodePath : "null");
                Slog.d(str, stringBuilder.toString());
            }
            return false;
        }
        android.content.pm.Signature[] apkSign = pkg.mSigningDetails.signatures;
        if (apkSign == null || apkSign.length == 0) {
            if (HW_DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("compareHashcode not apk : ");
                stringBuilder2.append(pkg.baseCodePath);
                Slog.d(str2, stringBuilder2.toString());
            }
            return false;
        }
        String[] apkSignHashAry = new String[apkSign.length];
        int i = 0;
        while (i < apkSign.length) {
            try {
                apkSignHashAry[i] = sha256(((X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(apkSign[i].toByteArray()))).getSignature());
                i++;
            } catch (CertificateException e) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("compareHashcode E: ");
                stringBuilder3.append(e);
                Slog.e(str3, stringBuilder3.toString());
                return false;
            }
        }
        i = 0;
        while (i < hashcode.length && i < apkSignHashAry.length) {
            if (hashcode[i].equals(apkSignHashAry[i])) {
                i++;
            } else {
                if (HW_DEBUG) {
                    String str4 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("compareHashcode hashcode not equal pkg = ");
                    stringBuilder4.append(pkg.baseCodePath);
                    stringBuilder4.append("white apk hash = ");
                    stringBuilder4.append(hashcode[i]);
                    stringBuilder4.append(" apk hashcod = ");
                    stringBuilder4.append(apkSignHashAry[i]);
                    Slog.d(str4, stringBuilder4.toString());
                }
                return false;
            }
        }
        return true;
    }

    private String bytesToString(byte[] bytes) {
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
        return new String(chars).toUpperCase(Locale.US);
    }

    private boolean componentValid(String apkPath) {
        AntiMalComponentInfo aci = this.mDataManager.getComponentByApkPath(apkPath);
        return aci != null ? aci.isVerifyStatusValid() : false;
    }

    private void setComponentAntiMalStatus(String path, int bitMask) {
        AntiMalComponentInfo aci = this.mDataManager.getComponentByApkPath(path);
        if (aci != null) {
            aci.setAntiMalStatus(bitMask);
        }
    }

    public int checkIllegalSysApk(Package pkg, int flags) throws PackageManagerException {
        int modifiedApkInfo = 0;
        if (!CHINA_RELEASE_VERSION) {
            return 0;
        }
        if (pkg == null) {
            Slog.e(TAG, "Invalid input args pkg(null) in checkIllegalSysApk.");
            return 0;
        } else if (this.mNeedScan) {
            markApkExist(pkg);
            if (!componentValid(pkg.baseCodePath)) {
                if (HW_DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("checkIllegalSysApk COMPONENT INVALID! path = ");
                    stringBuilder.append(pkg.baseCodePath);
                    Log.d(str, stringBuilder.toString());
                }
                return 0;
            } else if (!isPreinstallApkDir(pkg.baseCodePath)) {
                return 0;
            } else {
                ArrayList<ApkBasicInfo> pbi = (ArrayList) this.mSysApkWhitelist.get(pkg.packageName);
                AntiMalApkInfo illegalApkInfo;
                if (pbi == null) {
                    illegalApkInfo = new AntiMalApkInfo(pkg, 1);
                    this.mDataManager.addAntiMalApkInfo(illegalApkInfo);
                    if (HW_DEBUG) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("checkIllegalSysApk Add illegally AntiMalApkInfo : ");
                        stringBuilder2.append(illegalApkInfo);
                        Slog.d(str2, stringBuilder2.toString());
                    }
                    setComponentAntiMalStatus(pkg.baseCodePath, 1);
                    return 1;
                }
                Iterator<ApkBasicInfo> it = pbi.iterator();
                while (it.hasNext()) {
                    ApkBasicInfo apkInfo = (ApkBasicInfo) it.next();
                    if (apkInfo.mPackagename != null && apkInfo.mPath != null && pkg.baseCodePath.contains(apkInfo.mPath) && compareHashcode(apkInfo.mHashCode, pkg)) {
                        return 0;
                    }
                }
                illegalApkInfo = new AntiMalApkInfo(pkg, 2);
                if (HW_DEBUG) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("checkIllegalSysApk Add modify AntiMalApkInfo : ");
                    stringBuilder3.append(illegalApkInfo);
                    Slog.d(str3, stringBuilder3.toString());
                }
                this.mDataManager.addAntiMalApkInfo(illegalApkInfo);
                return 2;
            }
        } else {
            AntiMalApkInfo ai = (AntiMalApkInfo) this.mOldIllegalApks.get(pkg.packageName);
            if (HW_DEBUG && ai != null) {
                String str4 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("checkIllegalSysApk no need check legally AI = ");
                stringBuilder4.append(ai);
                Slog.d(str4, stringBuilder4.toString());
            }
            if (ai != null) {
                modifiedApkInfo = ai.mType;
            }
            return modifiedApkInfo;
        }
    }
}
