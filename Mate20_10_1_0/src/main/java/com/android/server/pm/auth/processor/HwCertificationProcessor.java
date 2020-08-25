package com.android.server.pm.auth.processor;

import android.annotation.SuppressLint;
import android.content.pm.PackageParser;
import android.text.TextUtils;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.util.HwAuthLogger;
import com.android.server.pm.auth.util.Utils;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipFile;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;

public class HwCertificationProcessor {
    private static final int MAX_LINE_LENGHT = 20000;
    public static final String TAG = "HwCertificationManager";
    protected static final HashMap<String, IProcessor> mProcessorMap = new HashMap<>();
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
        mProcessorMap.put(HwCertification.KEY_SIGNATURE2, new Signature2Processor());
    }

    private InputStream readCertFileFromApk(String apkPath, HwCertification cert) {
        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            HwAuthLogger.e("HwCertificationManager", "HC_RC read file error");
            return null;
        }
        cert.mCertificationData.mApkFile = apkFile;
        cert.setApkFile(apkFile);
        ZipFile zipFile = this.mZipFile;
        if (zipFile == null) {
            return Utils.readHwCertFromApk(apkPath);
        }
        return Utils.readHwCertFromApk(this.mZipFile, zipFile.getEntry(Utils.CERT_NAME));
    }

    public boolean createZipFile(String apkPath) {
        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            HwAuthLogger.e("HwCertificationManager", "HC_CZ read file error");
            return false;
        }
        try {
            this.mZipFile = new ZipFile(apkFile);
            return true;
        } catch (IOException e) {
            this.mZipFile = null;
            HwAuthLogger.e("HwCertificationManager", "IOException in createZipFile, e is " + e);
            return false;
        }
    }

    public void releaseZipFileResource() {
        IoUtils.closeQuietly(this.mZipFile);
        this.mZipFile = null;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:101:0x015d, code lost:
        com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:104:?, code lost:
        r2.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:106:0x016e, code lost:
        com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:0x00ea, code lost:
        r4 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:69:?, code lost:
        com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "HC_RC error throw exception");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:0x00f5, code lost:
        if (r1 != null) goto L_0x00f7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:?, code lost:
        r1.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:74:0x00fc, code lost:
        com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:?, code lost:
        r2.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x010d, code lost:
        com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:0x0119, code lost:
        r5 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:86:?, code lost:
        r1.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:88:0x0138, code lost:
        com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:91:?, code lost:
        r2.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:93:0x0149, code lost:
        com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:99:?, code lost:
        r1.close();
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0169 A[SYNTHETIC, Splitter:B:103:0x0169] */
    /* JADX WARNING: Removed duplicated region for block: B:65:0x00ea A[ExcHandler: all (th java.lang.Throwable), PHI: r1 
      PHI: (r1v4 'input' java.io.InputStream) = (r1v0 'input' java.io.InputStream), (r1v0 'input' java.io.InputStream), (r1v5 'input' java.io.InputStream) binds: [B:4:0x000b, B:5:?, B:20:0x0037] A[DONT_GENERATE, DONT_INLINE], Splitter:B:4:0x000b] */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x00ec A[ExcHandler: Exception (e java.lang.Exception), PHI: r1 
      PHI: (r1v3 'input' java.io.InputStream) = (r1v0 'input' java.io.InputStream), (r1v0 'input' java.io.InputStream), (r1v5 'input' java.io.InputStream) binds: [B:4:0x000b, B:5:?, B:20:0x0037] A[DONT_GENERATE, DONT_INLINE], Splitter:B:4:0x000b] */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0108 A[SYNTHETIC, Splitter:B:76:0x0108] */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x0133 A[SYNTHETIC, Splitter:B:85:0x0133] */
    /* JADX WARNING: Removed duplicated region for block: B:90:0x0144 A[SYNTHETIC, Splitter:B:90:0x0144] */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0158 A[SYNTHETIC, Splitter:B:98:0x0158] */
    public synchronized boolean readCert(String apkPath, HwCertification cert) {
        InputStream input;
        this.mAvailableTagList.clear();
        input = null;
        BufferedReader br = null;
        try {
            input = readCertFileFromApk(apkPath, cert);
            if (input == null) {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                    }
                }
                if (0 != 0) {
                    try {
                        br.close();
                    } catch (IOException e2) {
                        HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                    }
                }
                return false;
            }
            BufferedReader br2 = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            while (true) {
                String line = br2.readLine();
                if (line != null) {
                    String keyTag = line.split(AwarenessInnerConstants.COLON_KEY)[0].trim();
                    if (HwCertification.isHwCertKeyContainsTag(keyTag)) {
                        IProcessor processor = mProcessorMap.get(keyTag);
                        if (processor == null) {
                            HwAuthLogger.e("HwCertificationManager", "HC_RC error process is null");
                            try {
                                input.close();
                            } catch (IOException e3) {
                                HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                            }
                            try {
                                br2.close();
                            } catch (IOException e4) {
                                HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                            }
                            return false;
                        } else if (!readCert(processor, line, cert.mCertificationData)) {
                            HwAuthLogger.e("HwCertificationManager", "HC_RC error line mismatch");
                            try {
                                input.close();
                            } catch (IOException e5) {
                                HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                            }
                            try {
                                br2.close();
                            } catch (IOException e6) {
                                HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                            }
                            return false;
                        } else {
                            this.mAvailableTagList.add(keyTag);
                        }
                    }
                } else {
                    try {
                        input.close();
                    } catch (IOException e7) {
                        HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                    }
                    try {
                        br2.close();
                    } catch (IOException e8) {
                        HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                    }
                    cert.setZipFile(this.mZipFile);
                    return true;
                }
            }
        } catch (RuntimeException e9) {
            e = e9;
            HwAuthLogger.e("HwCertificationManager", "HC_RC error runtimeException : " + e);
            if (input != null) {
            }
            if (0 != 0) {
            }
            return false;
        } catch (Exception e10) {
        } catch (Throwable th) {
            th = th;
        }
        if (0 != 0) {
        }
        return false;
        if (0 != 0) {
        }
        throw th;
        if (input != null) {
        }
        if (0 != 0) {
        }
        throw th;
        return false;
        throw th;
    }

    private boolean readCert(IProcessor processor, String line, HwCertification.CertificationData rawCert) {
        if (line.length() < MAX_LINE_LENGHT) {
            return processor.readCert(line, rawCert);
        }
        HwAuthLogger.e("HwCertificationManager", "HC_RC cert is too long");
        return false;
    }

    @SuppressLint({"AvoidMethodInForLoop"})
    public boolean parserCert(HwCertification rawCert) {
        int i = 0;
        while (i < this.mAvailableTagList.size()) {
            String parserTag = this.mAvailableTagList.get(i);
            IProcessor processor = mProcessorMap.get(parserTag);
            if (processor == null) {
                HwAuthLogger.e("HwCertificationManager", "HC_PC process is null");
                return false;
            } else if (!processor.parserCert(rawCert)) {
                HwAuthLogger.e("HwCertificationManager", "HC_PC tag =" + parserTag);
                return false;
            } else {
                i++;
            }
        }
        return true;
    }

    @SuppressLint({"AvoidMethodInForLoop"})
    public boolean verifyCert(PackageParser.Package pkg, HwCertification cert) {
        int i = 0;
        while (i < this.mAvailableTagList.size()) {
            String verifyTag = this.mAvailableTagList.get(i);
            IProcessor processor = mProcessorMap.get(verifyTag);
            if (processor == null) {
                HwAuthLogger.e("HwCertificationManager", "HC_VC error process is null tag =" + verifyTag);
                return false;
            } else if (!processor.verifyCert(pkg, cert)) {
                HwAuthLogger.e("HwCertificationManager", "HC_VC error error tag =" + verifyTag);
                return false;
            } else {
                i++;
            }
        }
        String version = cert.getVersion();
        if ("2".equals(version) || !TextUtils.isEmpty(cert.getSignature2())) {
            HwAuthLogger.i("HwCertificationManager", "HC_VC ok" + version);
            return true;
        }
        HwAuthLogger.e("HwCertificationManager", "This is old v1 cert, no signature2");
        return false;
    }

    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        IProcessor processor = mProcessorMap.get(tag);
        if (processor == null) {
            HwAuthLogger.e("HwCertificationManager", "HC_PX error process is null tag =" + tag);
            return false;
        } else if (processor.parseXmlTag(tag, parser, cert)) {
            return true;
        } else {
            return false;
        }
    }

    public static IProcessor getProcessors(String tag) {
        return mProcessorMap.get(tag);
    }
}
