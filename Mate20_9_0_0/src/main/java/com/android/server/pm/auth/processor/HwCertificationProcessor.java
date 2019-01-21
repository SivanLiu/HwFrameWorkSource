package com.android.server.pm.auth.processor;

import android.annotation.SuppressLint;
import android.content.pm.PackageParser.Package;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.HwCertification.CertificationData;
import com.android.server.pm.auth.util.HwAuthLogger;
import com.android.server.pm.auth.util.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:9:0x0015, B:82:0x00f2] */
    /* JADX WARNING: Missing block: B:13:?, code skipped:
            com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
     */
    /* JADX WARNING: Missing block: B:30:0x0066, code skipped:
            com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "HC_RC error process is null");
     */
    /* JADX WARNING: Missing block: B:31:0x006e, code skipped:
            if (r1 == null) goto L_0x007f;
     */
    /* JADX WARNING: Missing block: B:33:?, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:36:?, code skipped:
            com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
     */
    /* JADX WARNING: Missing block: B:47:0x0098, code skipped:
            com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "HC_RC error line mismatch");
     */
    /* JADX WARNING: Missing block: B:48:0x00a0, code skipped:
            if (r1 == null) goto L_0x00b1;
     */
    /* JADX WARNING: Missing block: B:50:?, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:53:?, code skipped:
            com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
     */
    /* JADX WARNING: Missing block: B:64:0x00c8, code skipped:
            if (r1 == null) goto L_0x00d9;
     */
    /* JADX WARNING: Missing block: B:66:?, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:69:?, code skipped:
            com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
     */
    /* JADX WARNING: Missing block: B:115:0x0157, code skipped:
            if (r1 != null) goto L_0x0159;
     */
    /* JADX WARNING: Missing block: B:117:?, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:120:?, code skipped:
            com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
     */
    /* JADX WARNING: Missing block: B:121:0x0167, code skipped:
            if (r2 != null) goto L_0x0169;
     */
    /* JADX WARNING: Missing block: B:123:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:126:?, code skipped:
            com.android.server.pm.auth.util.HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean readCert(String apkPath, HwCertification cert) {
        BufferedReader br;
        this.mAvailableTagList.clear();
        InputStream input = null;
        br = null;
        boolean readResult = false;
        try {
            input = readCertFileFromApk(apkPath, cert);
            if (input != null) {
                br = new BufferedReader(new InputStreamReader(input, "UTF-8"));
                while (true) {
                    String readLine = br.readLine();
                    String line = readLine;
                    if (readLine == null) {
                        break;
                    }
                    String keyTag = line.split(":")[0].trim();
                    if (HwCertification.isHwCertKeyContainsTag(keyTag)) {
                        IProcessor processor = (IProcessor) mProcessorMap.get(keyTag);
                        if (processor == null) {
                            break;
                        } else if (!readCert(processor, line, cert.mCertificationData)) {
                            break;
                        } else {
                            this.mAvailableTagList.add(keyTag);
                        }
                    }
                }
            } else {
                if (input != null) {
                    input.close();
                }
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                    }
                }
            }
        } catch (RuntimeException e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HC_RC error runtimeException : ");
            stringBuilder.append(e2);
            HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e3) {
                    HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e4) {
                    HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                }
            }
            return false;
        } catch (Exception e5) {
            HwAuthLogger.e("HwCertificationManager", "HC_RC error throw exception");
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e6) {
                    HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e7) {
                    HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                }
            }
            return false;
        }
        cert.setZipFile(this.mZipFile);
        return true;
        return false;
        try {
            br.close();
        } catch (IOException e8) {
            HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
        }
        return false;
        return false;
        try {
            br.close();
        } catch (IOException e9) {
            HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
        }
        cert.setZipFile(this.mZipFile);
        return true;
        return false;
        try {
            br.close();
        } catch (IOException e10) {
            HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
        }
        return false;
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
