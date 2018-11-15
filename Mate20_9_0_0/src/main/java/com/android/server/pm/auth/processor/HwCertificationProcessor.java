package com.android.server.pm.auth.processor;

import android.annotation.SuppressLint;
import android.content.pm.PackageParser.Package;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.HwCertification.CertificationData;
import com.android.server.pm.auth.util.HwAuthLogger;
import com.android.server.pm.auth.util.Utils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipFile;
import org.xmlpull.v1.XmlPullParser;

public class HwCertificationProcessor {
    private static final int MAX_LINE_LENGHT = 20000;
    public static final String TAG = "HwCertificationManager";
    protected static final HashMap<String, IProcessor> mProcessorMap = new HashMap();
    private List<String> mAvailableTagList = new ArrayList();
    private ZipFile mZipFile = null;

    static {
        mProcessorMap.put(HwCertification.KEY_VERSION, new VersionProcessor());
        mProcessorMap.put(HwCertification.KEY_DEVELIOPER, new DeveloperKeyProcessor());
        mProcessorMap.put("PackageName", new PackageNameProcessor());
        mProcessorMap.put(HwCertification.KEY_PERMISSIONS, new PermissionProcessor());
        mProcessorMap.put(HwCertification.KEY_DEVICE_IDS, new DeviceIdProcessor());
        mProcessorMap.put(HwCertification.KEY_VALID_PERIOD, new ValidPeriodProcessor());
        mProcessorMap.put(HwCertification.KEY_APK_HASH, new ApkHashProcessor());
        mProcessorMap.put(HwCertification.KEY_CERTIFICATE, new CertificateProcessor());
        mProcessorMap.put(HwCertification.KEY_EXTENSION, new ExtenstionProcessor());
        mProcessorMap.put(HwCertification.KEY_SIGNATURE, new SignatureProcessor());
    }

    private InputStream readCertFileFromApk(String apkPath, HwCertification cert) {
        File apkFile = new File(apkPath);
        if (apkFile.exists()) {
            InputStream input;
            cert.mCertificationData.mApkFile = apkFile;
            cert.setApkFile(apkFile);
            if (this.mZipFile != null) {
                input = Utils.readHwCertFromApk(this.mZipFile, this.mZipFile.getEntry(Utils.CERT_NAME));
            } else {
                input = Utils.readHwCertFromApk(apkPath);
            }
            return input;
        }
        HwAuthLogger.e("HwCertificationManager", "HC_RC read file error");
        return null;
    }

    public boolean createZipFile(String apkPath) {
        File apkFile = new File(apkPath);
        if (apkFile.exists()) {
            try {
                this.mZipFile = new ZipFile(apkFile);
                return true;
            } catch (IOException e) {
                this.mZipFile = null;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IOException in createZipFile, e is ");
                stringBuilder.append(e);
                HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                return false;
            }
        }
        HwAuthLogger.e("HwCertificationManager", "HC_CZ read file error");
        return false;
    }

    public boolean releaseZipFileResource() {
        StringBuilder stringBuilder;
        try {
            if (this.mZipFile != null) {
                this.mZipFile.close();
                this.mZipFile = null;
            }
            return true;
        } catch (IOException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("IOException in releaseZipFileResource, e is ");
            stringBuilder.append(e);
            HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
            return false;
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception in releaseZipFileResource, e is ");
            stringBuilder.append(e2);
            HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
            return false;
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Exception block dominator not found, method:com.android.server.pm.auth.processor.HwCertificationProcessor.readCert(java.lang.String, com.android.server.pm.auth.HwCertification):boolean, dom blocks: [B:9:0x0015, B:81:0x00f2]
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.searchTryCatchDominators(ProcessTryCatchRegions.java:89)
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.process(ProcessTryCatchRegions.java:45)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.postProcessRegions(RegionMakerVisitor.java:63)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:58)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public synchronized boolean readCert(java.lang.String r13, com.android.server.pm.auth.HwCertification r14) {
        /*
        r12 = this;
        monitor-enter(r12);
        r0 = r12.mAvailableTagList;	 Catch:{ all -> 0x0177 }
        r0.clear();	 Catch:{ all -> 0x0177 }
        r0 = 0;
        r1 = 0;
        r2 = 0;
        r3 = 0;
        r4 = r3;
        r5 = r12.readCertFileFromApk(r13, r14);	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r1 = r5;
        if (r1 != 0) goto L_0x0035;
        if (r1 == 0) goto L_0x0022;
    L_0x0015:
        r1.close();	 Catch:{ IOException -> 0x0019 }
        goto L_0x0022;
    L_0x0019:
        r5 = move-exception;
        r6 = "HwCertificationManager";	 Catch:{ all -> 0x0177 }
        r7 = "read cert error : close stream failed!";	 Catch:{ all -> 0x0177 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r6, r7);	 Catch:{ all -> 0x0177 }
        goto L_0x0023;
    L_0x0023:
        if (r2 == 0) goto L_0x0032;
    L_0x0025:
        r2.close();	 Catch:{ IOException -> 0x0029 }
        goto L_0x0032;
    L_0x0029:
        r5 = move-exception;
        r6 = "HwCertificationManager";	 Catch:{ all -> 0x0177 }
        r7 = "read cert error : close stream failed!";	 Catch:{ all -> 0x0177 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r6, r7);	 Catch:{ all -> 0x0177 }
        goto L_0x0033;
    L_0x0033:
        monitor-exit(r12);
        return r3;
    L_0x0035:
        r5 = new java.io.BufferedReader;	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r6 = new java.io.InputStreamReader;	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r7 = "UTF-8";	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r6.<init>(r1, r7);	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r5.<init>(r6);	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r2 = r5;	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
    L_0x0042:
        r5 = r2.readLine();	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r0 = r5;	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        if (r5 == 0) goto L_0x00c8;	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
    L_0x0049:
        r5 = r0;	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r6 = ":";	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r6 = r5.split(r6);	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r7 = r6[r3];	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r7 = r7.trim();	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r8 = com.android.server.pm.auth.HwCertification.isHwCertKeyContainsTag(r7);	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        if (r8 == 0) goto L_0x00c6;	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
    L_0x005c:
        r8 = mProcessorMap;	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r8 = r8.get(r7);	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r8 = (com.android.server.pm.auth.processor.IProcessor) r8;	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        if (r8 != 0) goto L_0x008f;	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
    L_0x0066:
        r9 = "HwCertificationManager";	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r10 = "HC_RC error process is null";	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r9, r10);	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        if (r1 == 0) goto L_0x007d;
    L_0x0070:
        r1.close();	 Catch:{ IOException -> 0x0074 }
        goto L_0x007d;
    L_0x0074:
        r9 = move-exception;
        r10 = "HwCertificationManager";	 Catch:{ all -> 0x0177 }
        r11 = "read cert error : close stream failed!";	 Catch:{ all -> 0x0177 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r10, r11);	 Catch:{ all -> 0x0177 }
        goto L_0x007e;
        r2.close();	 Catch:{ IOException -> 0x0083 }
        goto L_0x008c;
    L_0x0083:
        r9 = move-exception;
        r10 = "HwCertificationManager";	 Catch:{ all -> 0x0177 }
        r11 = "read cert error : close stream failed!";	 Catch:{ all -> 0x0177 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r10, r11);	 Catch:{ all -> 0x0177 }
        goto L_0x008d;
    L_0x008d:
        monitor-exit(r12);
        return r3;
    L_0x008f:
        r9 = r14.mCertificationData;	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r9 = r12.readCert(r8, r0, r9);	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r4 = r9;	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        if (r4 != 0) goto L_0x00c1;	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
    L_0x0098:
        r9 = "HwCertificationManager";	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r10 = "HC_RC error line mismatch";	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r9, r10);	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        if (r1 == 0) goto L_0x00af;
    L_0x00a2:
        r1.close();	 Catch:{ IOException -> 0x00a6 }
        goto L_0x00af;
    L_0x00a6:
        r9 = move-exception;
        r10 = "HwCertificationManager";	 Catch:{ all -> 0x0177 }
        r11 = "read cert error : close stream failed!";	 Catch:{ all -> 0x0177 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r10, r11);	 Catch:{ all -> 0x0177 }
        goto L_0x00b0;
        r2.close();	 Catch:{ IOException -> 0x00b5 }
        goto L_0x00be;
    L_0x00b5:
        r9 = move-exception;
        r10 = "HwCertificationManager";	 Catch:{ all -> 0x0177 }
        r11 = "read cert error : close stream failed!";	 Catch:{ all -> 0x0177 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r10, r11);	 Catch:{ all -> 0x0177 }
        goto L_0x00bf;
    L_0x00bf:
        monitor-exit(r12);
        return r3;
    L_0x00c1:
        r9 = r12.mAvailableTagList;	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
        r9.add(r7);	 Catch:{ RuntimeException -> 0x011c, Exception -> 0x00f1 }
    L_0x00c6:
        goto L_0x0042;
    L_0x00c8:
        if (r1 == 0) goto L_0x00d7;
    L_0x00ca:
        r1.close();	 Catch:{ IOException -> 0x00ce }
        goto L_0x00d7;
    L_0x00ce:
        r3 = move-exception;
        r5 = "HwCertificationManager";	 Catch:{ all -> 0x0177 }
        r6 = "read cert error : close stream failed!";	 Catch:{ all -> 0x0177 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r5, r6);	 Catch:{ all -> 0x0177 }
        goto L_0x00d8;
        r2.close();	 Catch:{ IOException -> 0x00dd }
        goto L_0x00e6;
    L_0x00dd:
        r3 = move-exception;
        r5 = "HwCertificationManager";	 Catch:{ all -> 0x0177 }
        r6 = "read cert error : close stream failed!";	 Catch:{ all -> 0x0177 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r5, r6);	 Catch:{ all -> 0x0177 }
        goto L_0x00e7;	 Catch:{ all -> 0x0177 }
    L_0x00e7:
        r3 = r12.mZipFile;	 Catch:{ all -> 0x0177 }
        r14.setZipFile(r3);	 Catch:{ all -> 0x0177 }
        r3 = 1;
        monitor-exit(r12);
        return r3;
    L_0x00ef:
        r3 = move-exception;
        goto L_0x0156;
    L_0x00f1:
        r5 = move-exception;
        r6 = "HwCertificationManager";	 Catch:{ all -> 0x00ef }
        r7 = "HC_RC error throw exception";	 Catch:{ all -> 0x00ef }
        com.android.server.pm.auth.util.HwAuthLogger.e(r6, r7);	 Catch:{ all -> 0x00ef }
        if (r1 == 0) goto L_0x0109;
    L_0x00fc:
        r1.close();	 Catch:{ IOException -> 0x0100 }
        goto L_0x0109;
    L_0x0100:
        r6 = move-exception;
        r7 = "HwCertificationManager";	 Catch:{ all -> 0x0177 }
        r8 = "read cert error : close stream failed!";	 Catch:{ all -> 0x0177 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r7, r8);	 Catch:{ all -> 0x0177 }
        goto L_0x010a;
    L_0x010a:
        if (r2 == 0) goto L_0x0119;
    L_0x010c:
        r2.close();	 Catch:{ IOException -> 0x0110 }
        goto L_0x0119;
    L_0x0110:
        r6 = move-exception;
        r7 = "HwCertificationManager";	 Catch:{ all -> 0x0177 }
        r8 = "read cert error : close stream failed!";	 Catch:{ all -> 0x0177 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r7, r8);	 Catch:{ all -> 0x0177 }
        goto L_0x011a;
    L_0x011a:
        monitor-exit(r12);
        return r3;
    L_0x011c:
        r5 = move-exception;
        r6 = "HwCertificationManager";	 Catch:{ all -> 0x00ef }
        r7 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00ef }
        r7.<init>();	 Catch:{ all -> 0x00ef }
        r8 = "HC_RC error runtimeException : ";	 Catch:{ all -> 0x00ef }
        r7.append(r8);	 Catch:{ all -> 0x00ef }
        r7.append(r5);	 Catch:{ all -> 0x00ef }
        r7 = r7.toString();	 Catch:{ all -> 0x00ef }
        com.android.server.pm.auth.util.HwAuthLogger.e(r6, r7);	 Catch:{ all -> 0x00ef }
        if (r1 == 0) goto L_0x0143;
    L_0x0136:
        r1.close();	 Catch:{ IOException -> 0x013a }
        goto L_0x0143;
    L_0x013a:
        r6 = move-exception;
        r7 = "HwCertificationManager";	 Catch:{ all -> 0x0177 }
        r8 = "read cert error : close stream failed!";	 Catch:{ all -> 0x0177 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r7, r8);	 Catch:{ all -> 0x0177 }
        goto L_0x0144;
    L_0x0144:
        if (r2 == 0) goto L_0x0153;
    L_0x0146:
        r2.close();	 Catch:{ IOException -> 0x014a }
        goto L_0x0153;
    L_0x014a:
        r6 = move-exception;
        r7 = "HwCertificationManager";	 Catch:{ all -> 0x0177 }
        r8 = "read cert error : close stream failed!";	 Catch:{ all -> 0x0177 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r7, r8);	 Catch:{ all -> 0x0177 }
        goto L_0x0154;
    L_0x0154:
        monitor-exit(r12);
        return r3;
        if (r1 == 0) goto L_0x0166;
    L_0x0159:
        r1.close();	 Catch:{ IOException -> 0x015d }
        goto L_0x0166;
    L_0x015d:
        r5 = move-exception;
        r6 = "HwCertificationManager";	 Catch:{ all -> 0x0177 }
        r7 = "read cert error : close stream failed!";	 Catch:{ all -> 0x0177 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r6, r7);	 Catch:{ all -> 0x0177 }
        goto L_0x0167;
    L_0x0167:
        if (r2 == 0) goto L_0x0176;
    L_0x0169:
        r2.close();	 Catch:{ IOException -> 0x016d }
        goto L_0x0176;
    L_0x016d:
        r5 = move-exception;
        r6 = "HwCertificationManager";	 Catch:{ all -> 0x0177 }
        r7 = "read cert error : close stream failed!";	 Catch:{ all -> 0x0177 }
        com.android.server.pm.auth.util.HwAuthLogger.e(r6, r7);	 Catch:{ all -> 0x0177 }
    L_0x0176:
        throw r3;	 Catch:{ all -> 0x0177 }
    L_0x0177:
        r13 = move-exception;
        monitor-exit(r12);
        throw r13;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.auth.processor.HwCertificationProcessor.readCert(java.lang.String, com.android.server.pm.auth.HwCertification):boolean");
    }

    private boolean readCert(IProcessor processor, String line, CertificationData rawCert) {
        if (line.length() < MAX_LINE_LENGHT) {
            return processor.readCert(line, rawCert);
        }
        HwAuthLogger.e("HwCertificationManager", "HC_RC cert is too long");
        return false;
    }

    @SuppressLint({"AvoidMethodInForLoop"})
    public boolean parserCert(HwCertification rawCert) {
        String parserTag = null;
        int i = 0;
        while (i < this.mAvailableTagList.size()) {
            parserTag = (String) this.mAvailableTagList.get(i);
            IProcessor processor = (IProcessor) mProcessorMap.get(parserTag);
            if (processor == null) {
                HwAuthLogger.e("HwCertificationManager", "HC_PC process is null");
                return false;
            } else if (processor.parserCert(rawCert)) {
                i++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("HC_PC tag =");
                stringBuilder.append(parserTag);
                HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                return false;
            }
        }
        return true;
    }

    @SuppressLint({"AvoidMethodInForLoop"})
    public boolean verifyCert(Package pkg, HwCertification cert) {
        String verifyTag = null;
        int i = 0;
        while (i < this.mAvailableTagList.size()) {
            verifyTag = (String) this.mAvailableTagList.get(i);
            IProcessor processor = (IProcessor) mProcessorMap.get(verifyTag);
            StringBuilder stringBuilder;
            if (processor == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("HC_VC error process is null tag =");
                stringBuilder.append(verifyTag);
                HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                return false;
            } else if (processor.verifyCert(pkg, cert)) {
                i++;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("HC_VC error error tag =");
                stringBuilder.append(verifyTag);
                HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                return false;
            }
        }
        HwAuthLogger.i("HwCertificationManager", "HC_VC ok");
        return true;
    }

    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        IProcessor processor = (IProcessor) mProcessorMap.get(tag);
        if (processor == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HC_PX error process is null tag =");
            stringBuilder.append(tag);
            HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
            return false;
        } else if (processor.parseXmlTag(tag, parser, cert)) {
            return true;
        } else {
            return false;
        }
    }

    public static IProcessor getProcessors(String tag) {
        return (IProcessor) mProcessorMap.get(tag);
    }
}
