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

    /* JADX WARNING: Removed duplicated region for block: B:38:0x0095 A:{Catch:{ RuntimeException -> 0x00de, Exception -> 0x00d2 }} */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x0094 A:{Catch:{ RuntimeException -> 0x00de, Exception -> 0x00d2 }} */
    /* JADX WARNING: Missing block: B:53:?, code skipped:
            closeStream(r0, r1);
     */
    /* JADX WARNING: Missing block: B:58:0x00e9, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void readHwCertXml(ConcurrentHashMap<String, HwCertification> certs) {
        InputStreamReader fr = null;
        FileInputStream fis = null;
        File certXml = new File(new File(Environment.getDataDirectory(), "system"), FILE_NAME);
        if (certXml.exists()) {
            try {
                int type;
                String tag;
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                fis = new FileInputStream(certXml);
                fr = new InputStreamReader(fis, "UTF-8");
                parser.setInput(fr);
                while (true) {
                    int next = parser.next();
                    type = next;
                    if (next == 1 || type == 2) {
                        tag = parser.getName();
                    }
                }
                tag = parser.getName();
                if (TAG_HWCERT.equals(tag)) {
                    int outerDepth = parser.getDepth();
                    while (true) {
                        int next2 = parser.next();
                        type = next2;
                        if (next2 != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                            boolean shouldSkip;
                            if (type != 3) {
                                if (type != 4) {
                                    shouldSkip = false;
                                    if (shouldSkip) {
                                        tag = parser.getName();
                                        if (TAG_CERT.equals(tag)) {
                                            HwCertification cert = processCertTag(parser);
                                            if (!(cert == null || cert.getPackageName() == null)) {
                                                certs.put(cert.getPackageName(), cert);
                                            }
                                        } else {
                                            StringBuilder stringBuilder = new StringBuilder();
                                            stringBuilder.append("readHwCertXml:unknow tag:");
                                            stringBuilder.append(tag);
                                            HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                                            XmlUtils.skipCurrentTag(parser);
                                        }
                                    }
                                }
                            }
                            shouldSkip = true;
                            if (shouldSkip) {
                            }
                        }
                        break;
                    }
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("readHwCertXml:unexpected tag found: ");
                stringBuilder2.append(tag);
                HwAuthLogger.e("HwCertificationManager", stringBuilder2.toString());
                closeStream(fr, fis);
            } catch (RuntimeException e) {
                HwAuthLogger.e("HwCertificationManager", "readHwCertXml:RuntimeException", e);
            } catch (Exception e2) {
                try {
                    HwAuthLogger.e("HwCertificationManager", "readHwCertXml:IOException", e2);
                } catch (Throwable th) {
                    closeStream(fr, fis);
                }
            }
        } else {
            HwAuthLogger.e("HwCertificationManager", "readHwCertXml:hwcert.xml not exists");
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
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        File certXml = new File(new File(Environment.getDataDirectory(), "system"), FILE_NAME);
        if (certXml.exists()) {
            boolean deleted = false;
            try {
                deleted = certXml.delete();
            } catch (IOException e) {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ex) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("can not close fis in updateHwCert, ex is ");
                        stringBuilder.append(ex);
                        HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                    }
                }
                return false;
            } catch (SecurityException e2) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SecurityException when delete certXml in updateHwCert, e is ");
                stringBuilder2.append(e2);
                HwAuthLogger.e("HwCertificationManager", stringBuilder2.toString());
            } catch (Throwable th) {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ex2) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("can not close fis in updateHwCert, ex is ");
                        stringBuilder2.append(ex2);
                        HwAuthLogger.e("HwCertificationManager", stringBuilder2.toString());
                    }
                }
            }
            if (!deleted) {
                HwAuthLogger.e("HwCertificationManager", "canot delete certXml in updateHwCert.");
                return false;
            }
        }
        FileOutputStream fis = null;
        fis = new FileOutputStream(certXml, false);
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
        try {
            fis.close();
        } catch (IOException ex3) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("can not close fis in updateHwCert, ex is ");
            stringBuilder.append(ex3);
            HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
        }
        return true;
    }

    public static void readOneCertXml(XmlPullParser parser, HwCertification cert) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                if (type != 3) {
                    if (type != 4) {
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
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("processCertTag:error, tag is ");
                            stringBuilder.append(tag);
                            HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                            return;
                        }
                    }
                }
            }
        }
    }

    public static void writeToXml(XmlSerializer out, HwCertification cert) throws IllegalArgumentException, IllegalStateException, IOException {
        out.startTag(null, TAG_CERT);
        out.attribute(null, "name", cert.mCertificationData.mPackageName);
        if (!(cert.getVersion() == null || cert.getVersion().isEmpty())) {
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
        if (!(cert.getCertificate() == null || cert.getCertificate().isEmpty())) {
            out.startTag(null, HwCertification.KEY_CERTIFICATE);
            out.attribute(null, "value", cert.mCertificationData.mCertificate);
            out.endTag(null, HwCertification.KEY_CERTIFICATE);
        }
        if (!(cert.getExtenstion() == null || cert.getExtenstion().isEmpty())) {
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
