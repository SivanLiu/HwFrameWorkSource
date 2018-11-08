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
                HwAuthLogger.e("HwCertificationManager", "IOException in createZipFile, e is " + e);
                return false;
            }
        }
        HwAuthLogger.e("HwCertificationManager", "HC_CZ read file error");
        return false;
    }

    public boolean releaseZipFileResource() {
        try {
            if (this.mZipFile != null) {
                this.mZipFile.close();
                this.mZipFile = null;
            }
            return true;
        } catch (IOException e) {
            HwAuthLogger.e("HwCertificationManager", "IOException in releaseZipFileResource, e is " + e);
            return false;
        } catch (Exception e2) {
            HwAuthLogger.e("HwCertificationManager", "Exception in releaseZipFileResource, e is " + e2);
            return false;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean readCert(String apkPath, HwCertification cert) {
        RuntimeException e;
        Throwable th;
        BufferedReader br;
        this.mAvailableTagList.clear();
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        try {
            inputStream = readCertFileFromApk(apkPath, cert);
            if (inputStream == null) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e2) {
                        HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                    }
                }
                return false;
            }
            br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            while (true) {
                try {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    String str = line;
                    String keyTag = line.split(":")[0].trim();
                    if (HwCertification.isHwCertKeyContainsTag(keyTag)) {
                        IProcessor processor = (IProcessor) mProcessorMap.get(keyTag);
                        if (processor == null) {
                            break;
                        }
                        if (!readCert(processor, line, cert.mCertificationData)) {
                            break;
                        }
                        this.mAvailableTagList.add(keyTag);
                    }
                } catch (RuntimeException e3) {
                    e = e3;
                    bufferedReader = br;
                } catch (Exception e4) {
                    bufferedReader = br;
                } catch (Throwable th2) {
                    th = th2;
                    bufferedReader = br;
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e5) {
                    HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e6) {
                    HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                }
            }
            cert.setZipFile(this.mZipFile);
            return true;
        } catch (RuntimeException e7) {
            e = e7;
            try {
                HwAuthLogger.e("HwCertificationManager", "HC_RC error runtimeException : " + e);
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e8) {
                        HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                    }
                }
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e9) {
                        HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                    }
                }
                return false;
            } catch (Throwable th3) {
                th = th3;
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e10) {
                        HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                    }
                }
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e11) {
                        HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                    }
                }
                throw th;
            }
        } catch (Exception e12) {
            HwAuthLogger.e("HwCertificationManager", "HC_RC error throw exception");
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e13) {
                    HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                }
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e14) {
                    HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
                }
            }
            return false;
        }
        if (br != null) {
            try {
                br.close();
            } catch (IOException e15) {
                HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
            }
        }
        return false;
        return false;
        if (br != null) {
            try {
                br.close();
            } catch (IOException e16) {
                HwAuthLogger.e("HwCertificationManager", "read cert error : close stream failed!");
            }
        }
        return false;
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
        int i = 0;
        while (i < this.mAvailableTagList.size()) {
            String parserTag = (String) this.mAvailableTagList.get(i);
            IProcessor processor = (IProcessor) mProcessorMap.get(parserTag);
            if (processor == null) {
                HwAuthLogger.e("HwCertificationManager", "HC_PC process is null");
                return false;
            } else if (processor.parserCert(rawCert)) {
                i++;
            } else {
                HwAuthLogger.e("HwCertificationManager", "HC_PC tag =" + parserTag);
                return false;
            }
        }
        return true;
    }

    @SuppressLint({"AvoidMethodInForLoop"})
    public boolean verifyCert(Package pkg, HwCertification cert) {
        int i = 0;
        while (i < this.mAvailableTagList.size()) {
            String verifyTag = (String) this.mAvailableTagList.get(i);
            IProcessor processor = (IProcessor) mProcessorMap.get(verifyTag);
            if (processor == null) {
                HwAuthLogger.e("HwCertificationManager", "HC_VC error process is null tag =" + verifyTag);
                return false;
            } else if (processor.verifyCert(pkg, cert)) {
                i++;
            } else {
                HwAuthLogger.e("HwCertificationManager", "HC_VC error error tag =" + verifyTag);
                return false;
            }
        }
        HwAuthLogger.i("HwCertificationManager", "HC_VC ok");
        return true;
    }

    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        IProcessor processor = (IProcessor) mProcessorMap.get(tag);
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
        return (IProcessor) mProcessorMap.get(tag);
    }
}
