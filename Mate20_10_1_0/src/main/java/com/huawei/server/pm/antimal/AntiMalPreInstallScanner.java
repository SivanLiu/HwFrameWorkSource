package com.huawei.server.pm.antimal;

import android.content.Context;
import android.content.pm.PackageParser;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.server.pm.PackageManagerException;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.server.security.securitydiagnose.AntiMalApkInfo;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
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
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AntiMalPreInstallScanner {
    private static final int CACHE_SIZE = 1024;
    private static final String[] COMPONENT_ARRAY;
    private static final String ENCRYPT_ARG = (KEY_ALGORITHM + PATH_SLANT + "ECB" + PATH_SLANT + "OAEPWithSHA-1AndMGF1Padding");
    private static final int EXCEPTION_NUM = -1;
    private static final int HASH_MAP_SIZE = 16;
    private static final boolean IS_CHINA_RELEASE_VERSION = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static final boolean IS_HW_DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final String KEY_ALGORITHM = "RSA";
    private static final int LIST_SIZE = 10;
    private static final int MAX_STR_LEN = 350;
    private static final String PATH_APP = "app";
    private static final String PATH_CUST = "cust";
    private static final String PATH_CUST_ECOTA = "cust/ecota";
    private static final String PATH_ETC = "etc";
    private static final String PATH_HW_PRODUCT = "hw_product";
    private static final String PATH_OEM = "oem";
    private static final String PATH_PREAS = "preas";
    private static final String PATH_PRELOAD = "preload";
    private static final String PATH_PRODUCT = "product";
    private static final String PATH_SLANT = File.separator;
    private static final String PATH_SYSTEM = "system";
    private static final String PATH_VERSION = "version";
    private static final String PKGLIST_SIGN_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArO6tIeIxD78HrazoYkAeKdXhKINVUE1fAwnXb6OiabTYf3qO22wcFoqyKsm2tlaWrDU8+hnxjkZOLIvpcJ0bEDAkICoIRNGBoJzJzN6PyIOyfLd4IOA/bS071jaA5JjLGpMkBKYuhzECnK/pmruKngl3ED/t8HRw44ku1rabcwJjKl8dF4D0ogoosrr8mrwfnQaJpkmTL1oScF/Mr4plkrUdw3Ab00HZoklMVznT+M5KV8DmEjo8PIYkdFlJCwEx4Cj6PXKHfBEGeivyPe2W1/EnYdaREu4GO9ZLBsIhRhS3b7UY5UFsjbYBK23M4zrpZlMVQer4zyqmzefs25BYAwIDAQAB";
    private static final String[] PREINSTALL_APK_DIRS = {PATH_SLANT + PATH_SYSTEM + PATH_SLANT, PATH_SLANT + PATH_OEM + PATH_SLANT + PATH_APP + PATH_SLANT, PATH_SLANT + PATH_VERSION + PATH_SLANT, PATH_SLANT + PATH_HW_PRODUCT + PATH_SLANT, PATH_SLANT + PATH_PRODUCT + PATH_SLANT, PATH_SLANT + PATH_CUST_ECOTA + PATH_SLANT, PATH_SLANT + PATH_CUST + PATH_SLANT, PATH_SLANT + PATH_PRELOAD + PATH_SLANT, PATH_SLANT + PATH_PREAS + PATH_SLANT};
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String SYSAPK_SIGN_PATH = ("xml" + PATH_SLANT + "sign");
    private static final String SYSAPK_WHITE_LIST_PATH = ("xml" + PATH_SLANT + "criticalpro.xml");
    private static final String TAG = "HW-AntiMalPreInstallScanner";
    private static final String UNICODE_UTF_8 = "UTF-8";
    private static Context sContext;
    private static AntiMalPreInstallScanner sInstance;
    private static boolean sIsOtaBoot;
    private Map<String, ApkBasicInfo> mApkInfoList = new HashMap(16);
    private final AntiMalDataManager mDataManager = new AntiMalDataManager(sIsOtaBoot);
    private long mDeviceFirstUseTime;
    private boolean mIsNeedScan = this.mDataManager.isNeedScanIllegalApks();
    private Map<String, AntiMalApkInfo> mOldIllegalApks = new HashMap(16);
    private final AntiMalPreInstallReport mReport = new AntiMalPreInstallReport(this.mDataManager);
    private Map<String, List<ApkBasicInfo>> mSysApkWhitelist = new HashMap(16);

    static {
        StringBuilder sb = new StringBuilder();
        sb.append(PATH_SYSTEM);
        sb.append(PATH_SLANT);
        sb.append(PATH_ETC);
        sb.append(PATH_SLANT);
        StringBuilder sb2 = new StringBuilder();
        sb2.append(PATH_VERSION);
        sb2.append(PATH_SLANT);
        StringBuilder sb3 = new StringBuilder();
        sb3.append(PATH_HW_PRODUCT);
        sb3.append(PATH_SLANT);
        sb3.append(PATH_ETC);
        sb3.append(PATH_SLANT);
        StringBuilder sb4 = new StringBuilder();
        sb4.append(PATH_PRODUCT);
        sb4.append(PATH_SLANT);
        sb4.append(PATH_ETC);
        sb4.append(PATH_SLANT);
        StringBuilder sb5 = new StringBuilder();
        sb5.append(PATH_CUST_ECOTA);
        sb5.append(PATH_SLANT);
        StringBuilder sb6 = new StringBuilder();
        sb6.append(PATH_CUST);
        sb6.append(PATH_SLANT);
        StringBuilder sb7 = new StringBuilder();
        sb7.append(PATH_PRELOAD);
        sb7.append(PATH_SLANT);
        sb7.append(PATH_ETC);
        sb7.append(PATH_SLANT);
        StringBuilder sb8 = new StringBuilder();
        sb8.append(PATH_PREAS);
        sb8.append(PATH_SLANT);
        COMPONENT_ARRAY = new String[]{sb.toString(), sb2.toString(), sb3.toString(), sb4.toString(), sb5.toString(), sb6.toString(), sb7.toString(), sb8.toString()};
    }

    private AntiMalPreInstallScanner() {
        loadOldAntiMalList();
    }

    public static void init(Context context, boolean isOtaBoot) {
        sContext = context;
        sIsOtaBoot = isOtaBoot;
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "init isOtaBoot = " + isOtaBoot);
        }
    }

    public static synchronized AntiMalPreInstallScanner getInstance() {
        AntiMalPreInstallScanner antiMalPreInstallScanner;
        synchronized (AntiMalPreInstallScanner.class) {
            if (sInstance == null) {
                sInstance = new AntiMalPreInstallScanner();
            }
            antiMalPreInstallScanner = sInstance;
        }
        return antiMalPreInstallScanner;
    }

    public void systemReady() {
        if (IS_CHINA_RELEASE_VERSION) {
            if (this.mIsNeedScan) {
                checkDeletedIllegallyApks();
            }
            this.mReport.report(null);
        }
    }

    public List<String> getSysWhiteList() {
        Map<String, ApkBasicInfo> map = this.mApkInfoList;
        if (map != null && !map.isEmpty()) {
            return new ArrayList(this.mApkInfoList.keySet());
        }
        if (!IS_CHINA_RELEASE_VERSION) {
            return new ArrayList(0);
        }
        if (IS_HW_DEBUG) {
            Log.d(TAG, "getSysWhitelist start.");
        }
        long timeStart = System.currentTimeMillis();
        String[] strArr = COMPONENT_ARRAY;
        for (String componentPath : strArr) {
            File apkListFile = new File(componentPath, SYSAPK_WHITE_LIST_PATH);
            if (apkListFile.exists()) {
                File signFile = new File(componentPath, SYSAPK_SIGN_PATH);
                if (!signFile.exists()) {
                    if (IS_HW_DEBUG) {
                        Slog.w(TAG, "SYSAPK_SIGN_PATH does not exist, componentPath: " + componentPath);
                    }
                } else if (!verify(fileToByte(apkListFile), PKGLIST_SIGN_PUBLIC_KEY, readFromFile(signFile))) {
                    Slog.w(TAG, "getSysWhitelist System package list verify failed, componentPath: " + componentPath);
                } else if (!parsePackagelist(apkListFile)) {
                    Slog.w(TAG, "getSysWhitelist parsing whitelist failed, componentPath: " + componentPath);
                }
            } else if (IS_HW_DEBUG) {
                Slog.d(TAG, "SYSAPK_WHITE_LIST_PATH does not exist, componentPath: " + componentPath);
            }
        }
        if (IS_HW_DEBUG) {
            long timeEnd = System.currentTimeMillis();
            Slog.d(TAG, "execution time = " + (timeEnd - timeStart));
        }
        Map<String, ApkBasicInfo> map2 = this.mApkInfoList;
        if (map2 != null) {
            return new ArrayList(map2.keySet());
        }
        Slog.e(TAG, "getSysWhitelist apk info list failed to get");
        return new ArrayList(0);
    }

    public void loadSysWhitelist() {
        if (IS_CHINA_RELEASE_VERSION) {
            if (this.mIsNeedScan) {
                if (IS_HW_DEBUG) {
                    Log.d(TAG, "loadSysWhitelist start.");
                }
                long timeStart = System.currentTimeMillis();
                addSysWhitelistInfo();
                if (IS_HW_DEBUG) {
                    long end = System.currentTimeMillis();
                    Slog.d(TAG, "loadSysWhitelist TIME = " + (end - timeStart));
                }
            } else if (IS_HW_DEBUG) {
                Slog.d(TAG, "loadSysWhitelist no need load!");
            }
        }
    }

    public int checkIllegalSysApk(PackageParser.Package pkg, int flags) throws PackageManagerException {
        if (!IS_CHINA_RELEASE_VERSION) {
            return 0;
        }
        if (pkg == null) {
            Slog.e(TAG, "Invalid input args pkg(null) in checkIllegalSysApk.");
            return 0;
        } else if (!this.mIsNeedScan) {
            AntiMalApkInfo ai = this.mOldIllegalApks.get(pkg.packageName);
            if (IS_HW_DEBUG && ai != null) {
                Slog.d(TAG, "checkIllegalSysApk no need check legally AI = " + ai);
            }
            if (ai != null) {
                return ai.getType();
            }
            return 0;
        } else {
            markApkExist(pkg);
            if (componentValid(pkg.baseCodePath)) {
                return preinstallApkDirHandle(pkg);
            }
            if (IS_HW_DEBUG) {
                Log.d(TAG, "checkIllegalSysApk COMPONENT INVALID! path = " + pkg.baseCodePath);
            }
            return 0;
        }
    }

    private void loadOldAntiMalList() {
        List<AntiMalApkInfo> oldList = this.mDataManager.getOldApkInfoList();
        if (oldList != null) {
            synchronized (this.mOldIllegalApks) {
                for (AntiMalApkInfo ai : oldList) {
                    if (ai.getType() == 1 || ai.getType() == 2) {
                        this.mOldIllegalApks.put(ai.getPackageName(), ai);
                    }
                }
            }
        }
    }

    private void addSysWhitelistInfo() {
        String[] strArr = COMPONENT_ARRAY;
        for (String componentPath : strArr) {
            File apkListFile = new File(componentPath, SYSAPK_WHITE_LIST_PATH);
            AntiMalComponentInfo aci = new AntiMalComponentInfo(componentPath);
            if (!apkListFile.exists()) {
                Slog.e(TAG, "loadSysWhitelist criticalpro not exist. componentPath = " + componentPath);
                aci.setVerifyStatus(1);
                this.mDataManager.addComponentInfo(aci);
            } else {
                File signFile = new File(componentPath, SYSAPK_SIGN_PATH);
                if (!signFile.exists()) {
                    Slog.e(TAG, "loadSysWhitelist sign not exist! componentPath = " + componentPath);
                    aci.setVerifyStatus(2);
                    this.mDataManager.addComponentInfo(aci);
                } else {
                    try {
                        if (!verify(fileToByte(apkListFile), PKGLIST_SIGN_PUBLIC_KEY, readFromFile(signFile))) {
                            Slog.e(TAG, "loadSysWhitelist System package list verify failed componentPath = " + componentPath);
                            aci.setVerifyStatus(3);
                            this.mDataManager.addComponentInfo(aci);
                        } else {
                            aci.setVerifyStatus(0);
                            if (!parsePackagelist(apkListFile)) {
                                aci.setVerifyStatus(4);
                                Slog.e(TAG, "loadSysWhitelist parsing whitelist failed, componentPath = " + componentPath);
                            }
                            this.mDataManager.addComponentInfo(aci);
                        }
                    } catch (RuntimeException e) {
                        Slog.e(TAG, "loadSysWhitelist Exception failed: " + e.getMessage());
                        aci.setVerifyStatus(3);
                        this.mDataManager.addComponentInfo(aci);
                    }
                }
            }
        }
    }

    private byte[] fileToByte(File file) {
        byte[] dataOfBytes = null;
        FileInputStream input = null;
        ByteArrayOutputStream output = null;
        if (file != null && file.exists()) {
            try {
                input = new FileInputStream(file);
                output = new ByteArrayOutputStream(2048);
                byte[] caches = new byte[1024];
                while (true) {
                    int readRet = input.read(caches);
                    if (readRet == -1) {
                        break;
                    }
                    output.write(caches, 0, readRet);
                }
                output.flush();
                dataOfBytes = output.toByteArray();
            } catch (FileNotFoundException e) {
                Slog.e(TAG, "fileToByte FileNotFoundException.");
            } catch (IOException e2) {
                Slog.e(TAG, "fileToByte IOException!");
            } catch (Throwable th) {
                IoUtils.closeQuietly((AutoCloseable) null);
                IoUtils.closeQuietly((AutoCloseable) null);
                throw th;
            }
            IoUtils.closeQuietly(input);
            IoUtils.closeQuietly(output);
        }
        return dataOfBytes;
    }

    private String readFromFile(File file) {
        StringBuffer readBuf = null;
        InputStreamReader input = null;
        BufferedReader br = null;
        try {
            readBuf = new StringBuffer(350);
            input = new InputStreamReader(new FileInputStream(file), UNICODE_UTF_8);
            br = new BufferedReader(input);
            while (true) {
                int intChar = br.read();
                if (intChar == -1) {
                    break;
                } else if (readBuf.length() >= 350) {
                    break;
                } else {
                    readBuf.append((char) intChar);
                }
            }
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "readFromFile FileNotFoundException.");
        } catch (IOException e2) {
            Slog.e(TAG, "readFromFile IOException.");
        } finally {
            IoUtils.closeQuietly(input);
            IoUtils.closeQuietly(br);
        }
        return readBuf.toString();
    }

    private boolean verify(byte[] data, String publicKey, String sign) {
        if (!((data == null || data.length == 0 || sign == null || publicKey == null) ? false : true)) {
            Slog.e(TAG, "verify Input invalid!");
            return false;
        }
        try {
            PublicKey publicK = KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey.getBytes(UNICODE_UTF_8))));
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicK);
            signature.update(data);
            return signature.verify(Base64.getDecoder().decode(sign.getBytes(UNICODE_UTF_8)));
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "verify IllegalArgumentException.");
            return false;
        } catch (UnsupportedEncodingException e2) {
            Slog.e(TAG, "verify UnsupportedEncodingException.");
            return false;
        } catch (NoSuchAlgorithmException e3) {
            Slog.e(TAG, "verify NoSuchAlgorithmException.");
            return false;
        } catch (InvalidKeySpecException e4) {
            Slog.e(TAG, "verify InvalidKeySpecException.");
            return false;
        } catch (InvalidKeyException e5) {
            Slog.e(TAG, "verify InvalidKeyException.");
            return false;
        } catch (SignatureException e6) {
            Slog.e(TAG, "verify SignatureException. ");
            return false;
        }
    }

    private boolean isPreinstallApkDir(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String apkPath = path;
        if (!path.startsWith(PATH_SLANT)) {
            apkPath = PATH_SLANT + path;
        }
        for (String str : PREINSTALL_APK_DIRS) {
            if (apkPath.contains(str)) {
                return true;
            }
        }
        return false;
    }

    private void addApkListToSysApkWhitelist(String packageName, ApkBasicInfo pbi) {
        if (this.mSysApkWhitelist.get(packageName) == null) {
            List<ApkBasicInfo> apkList = new ArrayList<>(10);
            apkList.add(pbi);
            this.mSysApkWhitelist.put(packageName, apkList);
            return;
        }
        List<ApkBasicInfo> apkList2 = this.mSysApkWhitelist.get(packageName);
        apkList2.add(pbi);
        this.mSysApkWhitelist.put(packageName, apkList2);
    }

    private boolean parsePackagelistHandle(XmlPullParser parser) {
        int outerDepth = parser.getDepth();
        while (true) {
            try {
                int type = parser.next();
                boolean isNeedParse = true;
                if (type == 1) {
                    return true;
                }
                if (type == 3 && parser.getDepth() <= outerDepth) {
                    return true;
                }
                String tagName = parser.getName();
                if (type == 3 || type == 4 || !"package".equals(tagName)) {
                    isNeedParse = false;
                }
                if (isNeedParse) {
                    String packageName = parser.getAttributeValue(null, "name");
                    String path = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_APK_PATH);
                    String sign = parser.getAttributeValue(null, "ss");
                    String version = parser.getAttributeValue(null, PATH_VERSION);
                    if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(path)) {
                        if (!TextUtils.isEmpty(sign)) {
                            String packageName2 = packageName.intern();
                            if (isPreinstallApkDir(path)) {
                                ApkBasicInfo pbi = new ApkBasicInfo(packageName2, path, sign.split(","), version);
                                if (this.mApkInfoList == null) {
                                    Slog.e(TAG, "mApkInfoList is null!");
                                    return false;
                                }
                                this.mApkInfoList.put(packageName2, pbi);
                                addApkListToSysApkWhitelist(packageName2, pbi);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Slog.e(TAG, "failed to parse the whitelist. IOException!");
                return false;
            } catch (XmlPullParserException e2) {
                Slog.e(TAG, "parsePackagelist Error reading Pkg white list. XmlPullParserException!");
                return false;
            }
        }
    }

    private boolean parsePackagelist(File whiteList) {
        boolean isTrueVal;
        int type;
        FileInputStream fin = null;
        try {
            XmlPullParser parser = Xml.newPullParser();
            fin = new FileInputStream(whiteList);
            parser.setInput(fin, UNICODE_UTF_8);
            do {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } while (type != 1);
            if (type != 2) {
                Slog.e(TAG, "No start tag found in Package white list.");
                IoUtils.closeQuietly(fin);
                IoUtils.closeQuietly(fin);
                return false;
            }
            isTrueVal = parsePackagelistHandle(parser);
            IoUtils.closeQuietly(fin);
            return isTrueVal;
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "file whitelist cannot be found.");
            isTrueVal = false;
        } catch (IOException e2) {
            Slog.e(TAG, "failed to parse the whitelist. IOException!");
            isTrueVal = false;
        } catch (XmlPullParserException e3) {
            Slog.e(TAG, "parsePackagelist Error reading Pkg white list. XmlPullParserException!");
            isTrueVal = false;
        } catch (Throwable th) {
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
    }

    private int stringToInt(String str) {
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Catched NumberFormatException, str = " + str);
            return -1;
        }
    }

    private void checkDeletedIllegallyApks() {
        synchronized (this.mApkInfoList) {
            for (ApkBasicInfo abi : this.mApkInfoList.values()) {
                if (!abi.isExist()) {
                    if (checkApkExist(abi.getPath())) {
                        abi.setIsExist(true);
                    } else {
                        AntiMalApkInfo deletedApkInfo = new AntiMalApkInfo.Builder().setPackageName(abi.getPackageName()).setPath(formatPath(abi.getPath())).setApkName(null).setType(3).setLastModifyTime(null).setFrom(null).setVersion(stringToInt(abi.getVersion())).build();
                        setComponentAntiMalStatus(abi.getPath(), 4);
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
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return false;
            }
            for (File f : files) {
                if (f != null) {
                    try {
                        if (isApkPath(f.getCanonicalPath())) {
                            return true;
                        }
                    } catch (IOException e) {
                        if (IS_HW_DEBUG) {
                            Slog.e(TAG, "file path failed. IOException!");
                        }
                    }
                }
            }
        }
        return isApkPath(path);
    }

    private boolean isApkPath(String path) {
        return path != null && path.endsWith(".apk");
    }

    private void markApkExist(PackageParser.Package pkg) {
        if (pkg != null) {
            synchronized (this.mApkInfoList) {
                ApkBasicInfo abi = this.mApkInfoList.get(pkg.packageName);
                if (abi != null) {
                    abi.setIsExist(true);
                }
            }
        }
    }

    private String sha256(byte[] data) {
        if (data == null) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data);
            return bytesToString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            if (IS_HW_DEBUG) {
                Slog.e(TAG, "get sha256 failed");
            }
            return "";
        }
    }

    private String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] chars = new char[(bytes.length * 2)];
        for (int index = 0; index < bytes.length; index++) {
            int byteValue = bytes[index] & 255;
            chars[index * 2] = hexChars[byteValue >>> 4];
            chars[(index * 2) + 1] = hexChars[byteValue & 15];
        }
        return new String(chars).toUpperCase(Locale.ENGLISH);
    }

    private boolean compareHashcode(String[] hashcodes, PackageParser.Package pkg) {
        if (!((pkg == null || !isApkPath(pkg.baseCodePath) || hashcodes == null || hashcodes.length == 0) ? false : true)) {
            if (IS_HW_DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("compareHashcode not hashcode : ");
                sb.append(pkg != null ? pkg.baseCodePath : "null");
                Slog.d(TAG, sb.toString());
            }
            return false;
        }
        android.content.pm.Signature[] apkSigns = pkg.mSigningDetails.signatures;
        if (apkSigns == null || apkSigns.length == 0) {
            if (IS_HW_DEBUG) {
                Slog.d(TAG, "compareHashcode not apk : " + pkg.baseCodePath);
            }
            return false;
        }
        String[] apkSignHashes = new String[apkSigns.length];
        int index = 0;
        while (index < apkSigns.length) {
            try {
                Certificate generateCertificate = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(apkSigns[index].toByteArray()));
                if (generateCertificate instanceof X509Certificate) {
                    apkSignHashes[index] = sha256(((X509Certificate) generateCertificate).getSignature());
                }
                index++;
            } catch (CertificateException e) {
                Slog.e(TAG, "occur CertificateException when compareHashcode");
                return false;
            }
        }
        int index2 = 0;
        while (index2 < hashcodes.length && index2 < apkSignHashes.length) {
            if (hashcodes[index2] == null || !hashcodes[index2].equals(apkSignHashes[index2])) {
                if (IS_HW_DEBUG) {
                    Slog.d(TAG, "compareHashcode hashcode not equal pkg = " + pkg.baseCodePath + "white apk hash = " + hashcodes[index2] + " apk hashcode = " + apkSignHashes[index2]);
                }
                return false;
            }
            index2++;
        }
        return true;
    }

    private boolean componentValid(String apkPath) {
        AntiMalComponentInfo aci = this.mDataManager.getComponentByApkPath(apkPath);
        if (aci != null) {
            return aci.isVerifyStatusValid();
        }
        return false;
    }

    private void setComponentAntiMalStatus(String path, int bitMask) {
        AntiMalComponentInfo aci = this.mDataManager.getComponentByApkPath(path);
        if (aci != null) {
            aci.setAntiMalStatus(bitMask);
        }
    }

    private int preinstallApkDirHandle(PackageParser.Package pkg) {
        if (!isPreinstallApkDir(pkg.baseCodePath)) {
            return 0;
        }
        List<ApkBasicInfo> apkBasicInfoList = this.mSysApkWhitelist.get(pkg.staticSharedLibName != null ? pkg.manifestPackageName : pkg.packageName);
        if (apkBasicInfoList == null) {
            AntiMalApkInfo illegalApkInfo = new AntiMalApkInfo(pkg, 1);
            this.mDataManager.addAntiMalApkInfo(illegalApkInfo);
            if (IS_HW_DEBUG) {
                Slog.d(TAG, "checkIllegalSysApk Add illegally AntiMalApkInfo : " + illegalApkInfo);
            }
            setComponentAntiMalStatus(pkg.baseCodePath, 1);
            return 1;
        }
        for (ApkBasicInfo apkInfo : apkBasicInfoList) {
            if (apkInfo.getPackageName() != null && apkInfo.getPath() != null && pkg.baseCodePath.contains(apkInfo.getPath()) && compareHashcode(apkInfo.getHashCodes(), pkg)) {
                return 0;
            }
        }
        AntiMalApkInfo modifiedApkInfo = new AntiMalApkInfo(pkg, 2);
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "checkIllegalSysApk Add modify AntiMalApkInfo : " + modifiedApkInfo);
        }
        this.mDataManager.addAntiMalApkInfo(modifiedApkInfo);
        return 2;
    }

    private static class ApkBasicInfo {
        private final String[] mHashCodes;
        private boolean mIsExist = false;
        private final String mPackageName;
        private final String mPath;
        private final String mVersion;

        ApkBasicInfo(String packageName, String path, String[] hashCodeArry, String version) {
            this.mPackageName = packageName;
            this.mPath = path;
            this.mHashCodes = hashCodeArry;
            this.mVersion = version;
        }

        /* access modifiers changed from: package-private */
        public void setIsExist(boolean isExist) {
            this.mIsExist = isExist;
        }

        public boolean isExist() {
            return this.mIsExist;
        }

        public String[] getHashCodes() {
            return this.mHashCodes;
        }

        public String getVersion() {
            return this.mVersion;
        }

        public String getPath() {
            return this.mPath;
        }

        public String getPackageName() {
            return this.mPackageName;
        }
    }
}
