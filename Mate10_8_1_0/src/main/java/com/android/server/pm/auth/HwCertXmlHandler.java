package com.android.server.pm.auth;

import android.os.Environment;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.auth.processor.HwCertificationProcessor;
import com.android.server.pm.auth.processor.IProcessor;
import com.android.server.pm.auth.util.HwAuthLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class HwCertXmlHandler {
    private static final String FILE_NAME = "hwcert.xml";
    public static final String TAG = "HwCertificationManager";
    public static final String TAG_CERT = "cert";
    public static final String TAG_HWCERT = "hwcerts";
    public static final String TAG_VALUE = "value";

    public synchronized void readHwCertXml(ConcurrentHashMap<String, HwCertification> certs) {
        Throwable th;
        RuntimeException e;
        Exception e2;
        InputStreamReader inputStreamReader = null;
        FileInputStream fileInputStream = null;
        try {
            File certXml = new File(new File(Environment.getDataDirectory(), "system"), FILE_NAME);
            if (certXml.exists()) {
                try {
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParserFactory.newInstance();
                    XmlPullParser parser = factory.newPullParser();
                    FileInputStream fis = new FileInputStream(certXml);
                    try {
                        InputStreamReader fr = new InputStreamReader(fis, "UTF-8");
                        try {
                            int type;
                            parser.setInput(fr);
                            do {
                                type = parser.next();
                                if (type == 1) {
                                    break;
                                }
                            } while (type != 2);
                            String tag = parser.getName();
                            if (TAG_HWCERT.equals(tag)) {
                                int outerDepth = parser.getDepth();
                                while (true) {
                                    type = parser.next();
                                    if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                                        break;
                                    }
                                    boolean shouldSkip = type == 3 || type == 4;
                                    if (!shouldSkip) {
                                        tag = parser.getName();
                                        if (TAG_CERT.equals(tag)) {
                                            HwCertification cert = processCertTag(parser);
                                            if (!(cert == null || cert.getPackageName() == null)) {
                                                certs.put(cert.getPackageName(), cert);
                                            }
                                        } else {
                                            HwAuthLogger.e("HwCertificationManager", "readHwCertXml:unknow tag:" + tag);
                                            XmlUtils.skipCurrentTag(parser);
                                        }
                                    }
                                }
                                closeStream(fr, fis);
                                fileInputStream = fis;
                            } else {
                                HwAuthLogger.e("HwCertificationManager", "readHwCertXml:unexpected tag found: " + tag);
                                try {
                                    closeStream(fr, fis);
                                    return;
                                } catch (Throwable th2) {
                                    th = th2;
                                    fileInputStream = fis;
                                    throw th;
                                }
                            }
                        } catch (RuntimeException e3) {
                            e = e3;
                            fileInputStream = fis;
                            inputStreamReader = fr;
                        } catch (Exception e4) {
                            e2 = e4;
                            fileInputStream = fis;
                            inputStreamReader = fr;
                        } catch (Throwable th3) {
                            th = th3;
                            fileInputStream = fis;
                            inputStreamReader = fr;
                        }
                    } catch (RuntimeException e5) {
                        e = e5;
                        fileInputStream = fis;
                        try {
                            HwAuthLogger.e("HwCertificationManager", "readHwCertXml:RuntimeException", e);
                            closeStream(inputStreamReader, fileInputStream);
                            return;
                        } catch (Throwable th4) {
                            th = th4;
                            closeStream(inputStreamReader, fileInputStream);
                            throw th;
                        }
                    } catch (Exception e6) {
                        e2 = e6;
                        fileInputStream = fis;
                        HwAuthLogger.e("HwCertificationManager", "readHwCertXml:IOException", e2);
                        closeStream(inputStreamReader, fileInputStream);
                        return;
                    } catch (Throwable th5) {
                        th = th5;
                        fileInputStream = fis;
                        closeStream(inputStreamReader, fileInputStream);
                        throw th;
                    }
                } catch (RuntimeException e7) {
                    e = e7;
                    HwAuthLogger.e("HwCertificationManager", "readHwCertXml:RuntimeException", e);
                    closeStream(inputStreamReader, fileInputStream);
                    return;
                } catch (Exception e8) {
                    e2 = e8;
                    HwAuthLogger.e("HwCertificationManager", "readHwCertXml:IOException", e2);
                    closeStream(inputStreamReader, fileInputStream);
                    return;
                }
            }
            HwAuthLogger.e("HwCertificationManager", "readHwCertXml:hwcert.xml not exists");
        } catch (Throwable th6) {
            th = th6;
            throw th;
        }
    }

    private void closeStream(InputStreamReader fr, FileInputStream fis) {
        if (fr != null) {
            try {
                fr.close();
            } catch (IOException e) {
                HwAuthLogger.e("HwCertificationManager", "readHwCertXml:failed to close FileReader", e);
            }
        }
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e2) {
                HwAuthLogger.e("HwCertificationManager", "readHwCertXml:failed to close FileInputStream", e2);
            }
        }
    }

    private HwCertification processCertTag(XmlPullParser parser) {
        try {
            HwCertification cert = new HwCertification();
            readOneCertXml(parser, cert);
            return cert;
        } catch (Exception ex) {
            HwAuthLogger.e("HwCertificationManager", "processCertTag:IOException", ex);
            return null;
        }
    }

    public synchronized boolean updateHwCert(List<HwCertification> certs) {
        Throwable th;
        File certXml = new File(new File(Environment.getDataDirectory(), "system"), FILE_NAME);
        if (certXml.exists()) {
            boolean deleted = false;
            try {
                deleted = certXml.delete();
            } catch (SecurityException e) {
                HwAuthLogger.e("HwCertificationManager", "SecurityException when delete certXml in updateHwCert, e is " + e);
            }
            if (!deleted) {
                HwAuthLogger.e("HwCertificationManager", "canot delete certXml in updateHwCert.");
                return false;
            }
        }
        FileOutputStream fileOutputStream = null;
        try {
            FileOutputStream fis = new FileOutputStream(certXml, false);
            try {
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(fis, "utf-8");
                out.startDocument(null, Boolean.valueOf(true));
                out.startTag(null, TAG_HWCERT);
                for (HwCertification cert : certs) {
                    writeToXml(out, cert);
                }
                out.endTag(null, TAG_HWCERT);
                out.endDocument();
                fis.flush();
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ex) {
                        HwAuthLogger.e("HwCertificationManager", "can not close fis in updateHwCert, ex is " + ex);
                    }
                }
                return true;
            } catch (IOException e2) {
                fileOutputStream = fis;
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException ex2) {
                        HwAuthLogger.e("HwCertificationManager", "can not close fis in updateHwCert, ex is " + ex2);
                    }
                }
                return false;
            } catch (Throwable th2) {
                th = th2;
                fileOutputStream = fis;
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException ex22) {
                        HwAuthLogger.e("HwCertificationManager", "can not close fis in updateHwCert, ex is " + ex22);
                    }
                }
                throw th;
            }
        } catch (IOException e3) {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            return false;
        } catch (Throwable th3) {
            th = th3;
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            throw th;
        }
    }

    public static void readOneCertXml(XmlPullParser parser, HwCertification cert) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                if (!(type == 3 || type == 4)) {
                    String tag = parser.getName();
                    IProcessor processor = HwCertificationProcessor.getProcessors(tag);
                    if (processor == null) {
                        HwAuthLogger.e("HwCertificationManager", "readOneCertXml:processor is null");
                        return;
                    }
                    processor.parseXmlTag(tag, parser, cert);
                    if (processor.parserCert(cert)) {
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        HwAuthLogger.e("HwCertificationManager", "processCertTag:error, tag is " + tag);
                        return;
                    }
                }
            }
        }
    }

    public static void writeToXml(XmlSerializer out, HwCertification cert) throws IllegalArgumentException, IllegalStateException, IOException {
        out.startTag(null, TAG_CERT);
        out.attribute(null, "name", cert.mCertificationData.mPackageName);
        if (!(cert.getVersion() == null || (cert.getVersion().isEmpty() ^ 1) == 0)) {
            out.startTag(null, HwCertification.KEY_VERSION);
            out.attribute(null, "value", cert.mCertificationData.mVersion);
            out.endTag(null, HwCertification.KEY_VERSION);
        }
        out.startTag(null, HwCertification.KEY_DEVELIOPER);
        out.attribute(null, "value", cert.mCertificationData.mDelveoperKey);
        out.endTag(null, HwCertification.KEY_DEVELIOPER);
        out.startTag(null, "PackageName");
        out.attribute(null, "value", cert.mCertificationData.mPackageName);
        out.endTag(null, "PackageName");
        out.startTag(null, HwCertification.KEY_PERMISSIONS);
        out.attribute(null, "value", cert.mCertificationData.mPermissionsString);
        out.endTag(null, HwCertification.KEY_PERMISSIONS);
        out.startTag(null, HwCertification.KEY_DEVICE_IDS);
        out.attribute(null, "value", cert.mCertificationData.mDeviceIdsString);
        out.endTag(null, HwCertification.KEY_DEVICE_IDS);
        out.startTag(null, HwCertification.KEY_VALID_PERIOD);
        out.attribute(null, "value", cert.mCertificationData.mPeriodString);
        out.endTag(null, HwCertification.KEY_VALID_PERIOD);
        out.startTag(null, HwCertification.KEY_APK_HASH);
        out.attribute(null, "value", cert.mCertificationData.mApkHash);
        out.endTag(null, HwCertification.KEY_APK_HASH);
        if (!(cert.getCertificate() == null || (cert.getCertificate().isEmpty() ^ 1) == 0)) {
            out.startTag(null, HwCertification.KEY_CERTIFICATE);
            out.attribute(null, "value", cert.mCertificationData.mCertificate);
            out.endTag(null, HwCertification.KEY_CERTIFICATE);
        }
        if (!(cert.getExtenstion() == null || (cert.getExtenstion().isEmpty() ^ 1) == 0)) {
            out.startTag(null, HwCertification.KEY_EXTENSION);
            out.attribute(null, "value", cert.mCertificationData.mExtenstion);
            out.endTag(null, HwCertification.KEY_EXTENSION);
        }
        out.startTag(null, HwCertification.KEY_SIGNATURE);
        out.attribute(null, "value", cert.mCertificationData.mSignature);
        out.endTag(null, HwCertification.KEY_SIGNATURE);
        out.endTag(null, TAG_CERT);
    }
}
