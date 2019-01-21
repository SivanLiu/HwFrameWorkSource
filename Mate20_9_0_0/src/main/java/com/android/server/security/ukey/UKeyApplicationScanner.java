package com.android.server.security.ukey;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import android.util.Xml;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class UKeyApplicationScanner {
    private static final String APK_NAME_CN = "apkNameCn";
    private static final String APK_NAME_EN = "apkNameEn";
    private static final String CERT_MGR_NAME = "certMgrName";
    private static final int CERT_MGR_RESULT_DISABLE = 0;
    private static final int CERT_MGR_RESULT_ENABLE = 1;
    private static final int CERT_MGR_RESULT_HIDE = 2;
    private static final String CERT_MGR_SIGN = "certMgrSign";
    private static final String CERT_MGR_TYPE_DISABLE = "disable";
    private static final String CERT_MGR_TYPE_HIDE = "hide";
    private static final String KEY_ALGORITHM = "RSA";
    private static final String PACKAGE_NAME = "packageName";
    private static final String SIGN = "sign";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String TAG = "UKeyApplicationScanner";
    private static final String UKEY_APK_WHITE_LIST_NAME = "ukeyapp.xml";
    private static final String UKEY_ID = "ukeyId";
    private static HashMap<String, String> mPackageNameMap = new HashMap();
    private static HashMap<String, UKeyApkInfo> mUKeyApkList = new HashMap();
    private Context mContext;

    public static class UKeyApkInfo {
        public final String mApkNameCn;
        public final String mApkNameEn;
        public final String mCertMgrType;
        public final String[] mCertPkgHashCode;
        public final String[] mHashCode;
        public final String mPackageName;
        public final String mUKeyId;

        UKeyApkInfo(String packageName, String apkNameCn, String apkNameEn, String ukeyId, String[] hashCodeArray, String certMgrType, String[] certPkgSign) {
            this.mPackageName = packageName;
            this.mApkNameCn = apkNameCn;
            this.mApkNameEn = apkNameEn;
            this.mUKeyId = ukeyId;
            this.mHashCode = hashCodeArray;
            this.mCertMgrType = certMgrType;
            this.mCertPkgHashCode = certPkgSign;
        }
    }

    public UKeyApplicationScanner(Context context) {
        this.mContext = context;
    }

    public void loadUKeyApkWhitelist() {
        mUKeyApkList.clear();
        if (parsePackagelist()) {
            Slog.i(TAG, "parsing whitelist succeeded!!!");
        } else {
            Slog.i(TAG, "parsing whitelist failed!!!");
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0047 A:{SYNTHETIC, Splitter:B:22:0x0047} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x002c A:{SYNTHETIC, Splitter:B:12:0x002c} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean parsePackagelist() {
        FileNotFoundException e;
        String str;
        StringBuilder stringBuilder;
        Exception e2;
        Throwable th;
        boolean retVal = true;
        String str2 = null;
        InputStream fin = null;
        boolean z;
        try {
            XmlPullParser parser = Xml.newPullParser();
            try {
                fin = this.mContext.getAssets().open(UKEY_APK_WHITE_LIST_NAME);
                parser.setInput(fin, "UTF-8");
                boolean z2 = false;
                int type = 0;
                while (true) {
                    int next = parser.next();
                    type = next;
                    int i = 1;
                    if (next == 2 || type == 1) {
                        if (type == 2) {
                            try {
                                Slog.i(TAG, "No start tag found in Package white list.");
                                IoUtils.closeQuietly(fin);
                                return false;
                            } catch (FileNotFoundException e3) {
                                e = e3;
                                z = retVal;
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("parsePackagelist Error reading Pkg white list: ");
                                stringBuilder.append(e);
                                Slog.e(str, stringBuilder.toString());
                                retVal = false;
                                IoUtils.closeQuietly(fin);
                                return retVal;
                            } catch (IOException | IndexOutOfBoundsException | XmlPullParserException e4) {
                                e2 = e4;
                                z = retVal;
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("parsePackagelist Error reading Pkg white list: ");
                                stringBuilder.append(e2);
                                Slog.e(str, stringBuilder.toString());
                                retVal = false;
                                IoUtils.closeQuietly(fin);
                                return retVal;
                            } catch (Throwable th2) {
                                th = th2;
                                z = retVal;
                                IoUtils.closeQuietly(fin);
                                throw th;
                            }
                        }
                        next = parser.getDepth();
                        while (true) {
                            int next2 = parser.next();
                            type = next2;
                            if (next2 == i) {
                                z = retVal;
                                break;
                            }
                            boolean z3;
                            if (type == 3) {
                                if (parser.getDepth() <= next) {
                                    z = retVal;
                                    break;
                                }
                            }
                            if (type == 3) {
                                z = retVal;
                                z3 = z2;
                            } else if (type == 4) {
                                z = retVal;
                                z3 = z2;
                            } else if (parser.getName().equals("package")) {
                                String packageName = parser.getAttributeValue(str2, "packageName");
                                String apkNameCn = parser.getAttributeValue(str2, APK_NAME_CN);
                                String apkNameEn = parser.getAttributeValue(str2, APK_NAME_EN);
                                String ukeyId = parser.getAttributeValue(str2, UKEY_ID);
                                String sign = parser.getAttributeValue(str2, SIGN);
                                String certName = parser.getAttributeValue(str2, CERT_MGR_NAME);
                                String certMgrApkSign = parser.getAttributeValue(str2, CERT_MGR_SIGN);
                                str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                z = retVal;
                                try {
                                    stringBuilder2.append("PARSER from xml packageName : ");
                                    stringBuilder2.append(packageName);
                                    Slog.i(str2, stringBuilder2.toString());
                                    if (!TextUtils.isEmpty(packageName)) {
                                        if (!TextUtils.isEmpty(sign)) {
                                            String[] signArry = sign.split(",");
                                            if (certMgrApkSign == null) {
                                                z3 = false;
                                                retVal = new String[0];
                                            } else {
                                                z3 = false;
                                                retVal = certMgrApkSign.split(",");
                                            }
                                            String certName2 = certName;
                                            mUKeyApkList.put(packageName, new UKeyApkInfo(packageName, apkNameCn, apkNameEn, ukeyId, signArry, certName2, retVal));
                                            mPackageNameMap.put(packageName, packageName);
                                            if (!TextUtils.isEmpty(certName2)) {
                                                mPackageNameMap.put(certName2, packageName);
                                            }
                                        }
                                    }
                                    z3 = false;
                                } catch (FileNotFoundException e5) {
                                    e = e5;
                                    str = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("parsePackagelist Error reading Pkg white list: ");
                                    stringBuilder.append(e);
                                    Slog.e(str, stringBuilder.toString());
                                    retVal = false;
                                    IoUtils.closeQuietly(fin);
                                    return retVal;
                                } catch (IOException | IndexOutOfBoundsException | XmlPullParserException e6) {
                                    e2 = e6;
                                    str = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("parsePackagelist Error reading Pkg white list: ");
                                    stringBuilder.append(e2);
                                    Slog.e(str, stringBuilder.toString());
                                    retVal = false;
                                    IoUtils.closeQuietly(fin);
                                    return retVal;
                                }
                            } else {
                                z = retVal;
                                z3 = z2;
                            }
                            z2 = z3;
                            retVal = z;
                            str2 = null;
                            i = 1;
                        }
                        IoUtils.closeQuietly(fin);
                        retVal = z;
                        return retVal;
                    }
                }
                if (type == 2) {
                }
            } catch (FileNotFoundException e7) {
                e = e7;
                z = retVal;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("parsePackagelist Error reading Pkg white list: ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
                retVal = false;
                IoUtils.closeQuietly(fin);
                return retVal;
            } catch (IOException | IndexOutOfBoundsException | XmlPullParserException e8) {
                e2 = e8;
                z = retVal;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("parsePackagelist Error reading Pkg white list: ");
                stringBuilder.append(e2);
                Slog.e(str, stringBuilder.toString());
                retVal = false;
                IoUtils.closeQuietly(fin);
                return retVal;
            } catch (Throwable th3) {
                th = th3;
                z = retVal;
                IoUtils.closeQuietly(fin);
                throw th;
            }
        } catch (FileNotFoundException e9) {
            e = e9;
            z = retVal;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("parsePackagelist Error reading Pkg white list: ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            retVal = false;
            IoUtils.closeQuietly(fin);
            return retVal;
        } catch (IOException | IndexOutOfBoundsException | XmlPullParserException e10) {
            e2 = e10;
            z = retVal;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("parsePackagelist Error reading Pkg white list: ");
            stringBuilder.append(e2);
            Slog.e(str, stringBuilder.toString());
            retVal = false;
            IoUtils.closeQuietly(fin);
            return retVal;
        } catch (Throwable th4) {
            th = th4;
            IoUtils.closeQuietly(fin);
            throw th;
        }
    }

    public boolean isWhiteListedUKeyApp(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        String realPkgName = getRealUKeyPkgName(packageName);
        UKeyApkInfo apkSign = (UKeyApkInfo) mUKeyApkList.get(realPkgName);
        if (apkSign == null || apkSign.mPackageName == null) {
            return false;
        }
        try {
            return compareHashcode(apkSign.mHashCode, this.mContext.getPackageManager().getPackageInfo(realPkgName, 64).signatures);
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    public Bundle getUKeyApkInfoData(String packageName) {
        UKeyApkInfo uKeyApkInfo = (UKeyApkInfo) mUKeyApkList.get(packageName);
        if (uKeyApkInfo == null || uKeyApkInfo.mPackageName == null) {
            return null;
        }
        Bundle bundle = new Bundle();
        bundle.putString("packageName", uKeyApkInfo.mPackageName);
        bundle.putString(APK_NAME_CN, uKeyApkInfo.mApkNameCn);
        bundle.putString(APK_NAME_EN, uKeyApkInfo.mApkNameEn);
        if (compareCertMgrApkResult(uKeyApkInfo.mPackageName) == 0) {
            bundle.putString(CERT_MGR_NAME, CERT_MGR_TYPE_DISABLE);
        } else if (TextUtils.isEmpty(uKeyApkInfo.mCertMgrType)) {
            bundle.putString(CERT_MGR_NAME, uKeyApkInfo.mPackageName);
        } else {
            bundle.putString(CERT_MGR_NAME, uKeyApkInfo.mCertMgrType);
        }
        return bundle;
    }

    public UKeyApkInfo getUKeyApkInfo(String packageName) {
        if (mUKeyApkList == null) {
            return null;
        }
        return (UKeyApkInfo) mUKeyApkList.get(packageName);
    }

    private boolean compareHashcode(String[] hashcode, Signature[] signatures) {
        if (signatures == null || signatures.length == 0) {
            return false;
        }
        String[] apkSignHashAry = new String[signatures.length];
        int i = 0;
        while (i < signatures.length) {
            try {
                apkSignHashAry[i] = sha256(((X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(signatures[i].toByteArray()))).getSignature());
                i++;
            } catch (CertificateException e) {
                Slog.e(TAG, "compareHashcode Error !!!");
                return false;
            }
        }
        return compareSignatures(hashcode, apkSignHashAry);
    }

    private boolean compareSignatures(String[] s1, String[] s2) {
        if (s1.length != s2.length) {
            return false;
        }
        if (s1.length == 1) {
            return s1[0].equals(s2[0]);
        }
        String str;
        ArraySet<String> set1 = new ArraySet();
        for (String sig : s1) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sig 1 = ");
            stringBuilder.append(sig);
            Slog.e(str, stringBuilder.toString());
            set1.add(sig);
        }
        ArraySet<String> set2 = new ArraySet();
        for (String str2 : s2) {
            String str3 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("sig 2 = ");
            stringBuilder2.append(str2);
            Slog.e(str3, stringBuilder2.toString());
            set2.add(str2);
        }
        if (set1.equals(set2)) {
            return true;
        }
        return false;
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
            return null;
        }
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

    public String getRealUKeyPkgName(String packageName) {
        return (String) mPackageNameMap.get(packageName);
    }

    private int compareCertMgrApkResult(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return 0;
        }
        UKeyApkInfo apkSign = (UKeyApkInfo) mUKeyApkList.get(packageName);
        if (apkSign == null) {
            return 0;
        }
        if (TextUtils.isEmpty(apkSign.mCertMgrType)) {
            return 1;
        }
        if (CERT_MGR_TYPE_DISABLE.equals(apkSign.mCertMgrType)) {
            return 0;
        }
        if (CERT_MGR_TYPE_HIDE.equals(apkSign.mCertMgrType)) {
            return 2;
        }
        try {
            PackageInfo packageInfo = compareHashcode(apkSign.mCertPkgHashCode, this.mContext.getPackageManager().getPackageInfo(apkSign.mCertMgrType, 64).signatures);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("The compare Hashcode result of cert manager apk ");
            stringBuilder.append(apkSign.mCertMgrType);
            stringBuilder.append("result is ");
            stringBuilder.append(packageInfo);
            Slog.i(str, stringBuilder.toString());
            return packageInfo;
        } catch (NameNotFoundException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("ukey cert manager apk");
            stringBuilder2.append(apkSign.mCertMgrType);
            stringBuilder2.append(" does not install!!!");
            Slog.i(str2, stringBuilder2.toString());
            return 0;
        }
    }
}
