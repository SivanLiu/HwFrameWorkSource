package com.android.server.security.ukey;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;

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

    private boolean parsePackagelist() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:69:?
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.modifyBlocksTree(BlockProcessor.java:248)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.rerun(BlockProcessor.java:44)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:58)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r23 = this;
        r16 = 1;
        r13 = 0;
        r15 = android.util.Xml.newPullParser();	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r23;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r0.mContext;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = r0;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = r20.getAssets();	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = "ukeyapp.xml";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r13 = r20.open(r21);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = "UTF-8";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r15.setInput(r13, r0);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r19 = 0;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x0022:
        r19 = r15.next();	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = 2;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r19;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        if (r0 == r1) goto L_0x0036;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x002e:
        r20 = 1;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r19;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        if (r0 != r1) goto L_0x0022;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x0036:
        r20 = 2;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r19;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        if (r0 == r1) goto L_0x004d;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x003e:
        r20 = "UKeyApplicationScanner";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = "No start tag found in Package white list.";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        android.util.Slog.i(r20, r21);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = 0;
        libcore.io.IoUtils.closeQuietly(r13);
        return r20;
    L_0x004d:
        r14 = r15.getDepth();	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x0051:
        r19 = r15.next();	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = 1;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r19;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        if (r0 == r1) goto L_0x0173;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x005d:
        r20 = 3;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r19;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        if (r0 != r1) goto L_0x006d;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x0065:
        r20 = r15.getDepth();	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        if (r0 <= r14) goto L_0x0173;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x006d:
        r20 = 3;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r19;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        if (r0 == r1) goto L_0x0051;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x0075:
        r20 = 4;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r19;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        if (r0 == r1) goto L_0x0051;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x007d:
        r18 = r15.getName();	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = "package";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r18;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = r0.equals(r1);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        if (r20 == 0) goto L_0x0051;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x008e:
        r20 = "packageName";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = 0;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r21;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r3 = r15.getAttributeValue(r0, r1);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = "apkNameCn";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = 0;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r21;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r4 = r15.getAttributeValue(r0, r1);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = "apkNameEn";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = 0;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r21;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r5 = r15.getAttributeValue(r0, r1);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = "ukeyId";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = 0;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r21;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r6 = r15.getAttributeValue(r0, r1);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = "sign";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = 0;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r21;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r17 = r15.getAttributeValue(r0, r1);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = "certMgrName";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = 0;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r21;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r8 = r15.getAttributeValue(r0, r1);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = "certMgrSign";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = 0;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r21;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r10 = r15.getAttributeValue(r0, r1);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = "UKeyApplicationScanner";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = new java.lang.StringBuilder;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21.<init>();	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r22 = "PARSER from xml packageName : ";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = r21.append(r22);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r21;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = r0.append(r3);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = r21.toString();	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        android.util.Slog.i(r20, r21);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = android.text.TextUtils.isEmpty(r3);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        if (r20 != 0) goto L_0x0051;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x010b:
        r20 = android.text.TextUtils.isEmpty(r17);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        if (r20 != 0) goto L_0x0051;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x0111:
        r20 = ",";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r17;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r1 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r7 = r0.split(r1);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        if (r10 != 0) goto L_0x0169;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x011e:
        r20 = 0;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r9 = new java.lang.String[r0];	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x0124:
        r2 = new com.android.server.security.ukey.UKeyApplicationScanner$UKeyApkInfo;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r2.<init>(r3, r4, r5, r6, r7, r8, r9);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = mUKeyApkList;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0.put(r3, r2);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = mPackageNameMap;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0.put(r3, r3);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r20 = android.text.TextUtils.isEmpty(r8);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        if (r20 != 0) goto L_0x0051;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
    L_0x013d:
        r20 = mPackageNameMap;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0.put(r8, r3);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        goto L_0x0051;
    L_0x0146:
        r11 = move-exception;
        r20 = "UKeyApplicationScanner";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = new java.lang.StringBuilder;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21.<init>();	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r22 = "parsePackagelist Error reading Pkg white list: ";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = r21.append(r22);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r21;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = r0.append(r11);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = r21.toString();	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        android.util.Slog.e(r20, r21);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r16 = 0;
        libcore.io.IoUtils.closeQuietly(r13);
    L_0x0168:
        return r16;
    L_0x0169:
        r20 = ",";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r20;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r9 = r10.split(r0);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        goto L_0x0124;
    L_0x0173:
        libcore.io.IoUtils.closeQuietly(r13);
        goto L_0x0168;
    L_0x0177:
        r12 = move-exception;
        r20 = "UKeyApplicationScanner";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = new java.lang.StringBuilder;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21.<init>();	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r22 = "parsePackagelist Error reading Pkg white list: ";	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = r21.append(r22);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r0 = r21;	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = r0.append(r12);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r21 = r21.toString();	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        android.util.Slog.e(r20, r21);	 Catch:{ FileNotFoundException -> 0x0146, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, XmlPullParserException -> 0x0177, all -> 0x019a }
        r16 = 0;
        libcore.io.IoUtils.closeQuietly(r13);
        goto L_0x0168;
    L_0x019a:
        r20 = move-exception;
        libcore.io.IoUtils.closeQuietly(r13);
        throw r20;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.security.ukey.UKeyApplicationScanner.parsePackagelist():boolean");
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
        boolean z = true;
        if (s1.length != s2.length) {
            return false;
        }
        if (s1.length == 1) {
            if (!s1[0].equals(s2[0])) {
                z = false;
            }
            return z;
        }
        ArraySet<String> set1 = new ArraySet();
        for (String sig : s1) {
            Slog.e(TAG, "sig 1 = " + sig);
            set1.add(sig);
        }
        ArraySet<String> set2 = new ArraySet();
        for (String sig2 : s2) {
            Slog.e(TAG, "sig 2 = " + sig2);
            set2.add(sig2);
        }
        return set1.equals(set2);
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
        int i = 1;
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
            boolean result = compareHashcode(apkSign.mCertPkgHashCode, this.mContext.getPackageManager().getPackageInfo(apkSign.mCertMgrType, 64).signatures);
            Slog.i(TAG, "The compare Hashcode result of cert manager apk " + apkSign.mCertMgrType + "result is " + result);
            if (!result) {
                i = 0;
            }
            return i;
        } catch (NameNotFoundException e) {
            Slog.i(TAG, "ukey cert manager apk" + apkSign.mCertMgrType + " does not install!!!");
            return 0;
        }
    }
}
