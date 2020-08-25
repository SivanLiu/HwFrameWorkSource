package com.android.server.pm.auth;

import android.os.Environment;
import android.text.TextUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.auth.processor.HwCertificationProcessor;
import com.android.server.pm.auth.processor.IProcessor;
import com.android.server.pm.auth.util.HwAuthLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class HwCertXmlHandler {
    private static final String BLACKLIST_FILE_NAME = "hwCertBlacklist.xml";
    private static final String FILE_NAME = "hwcert.xml";
    private static final String SYSTEM_DIR = "system";
    public static final String TAG = "HwCertificationManager";
    private static final String TAG_BLACKLIST = "blacklist";
    public static final String TAG_CERT = "cert";
    public static final String TAG_HWCERT = "hwcerts";
    private static final String TAG_SIGNATURE = "signature";
    public static final String TAG_VALUE = "value";

    public void readHwCertXml(ConcurrentHashMap<String, HwCertification> certs) {
        boolean z;
        File certXml = new File(new File(Environment.getDataDirectory(), SYSTEM_DIR), FILE_NAME);
        if (!certXml.exists()) {
            HwAuthLogger.e("HwCertificationManager", "readHwCertXml:hwcert.xml not exists");
            return;
        }
        try {
            FileInputStream fis = new FileInputStream(certXml);
            try {
                InputStreamReader fr = new InputStreamReader(fis, "UTF-8");
                try {
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParserFactory.newInstance();
                    XmlPullParser parser = factory.newPullParser();
                    parser.setInput(fr);
                    int type = parser.next();
                    while (true) {
                        z = true;
                        if (type == 1 || type == 2) {
                            String tag = parser.getName();
                        } else {
                            type = parser.next();
                        }
                    }
                    String tag2 = parser.getName();
                    if (!TAG_HWCERT.equals(tag2)) {
                        HwAuthLogger.e("HwCertificationManager", "readHwCertXml:unexpected tag found: " + tag2);
                        $closeResource(null, fr);
                        $closeResource(null, fis);
                        return;
                    }
                    int outerDepth = parser.getDepth();
                    while (true) {
                        int type2 = parser.next();
                        if (type2 != z) {
                            if (type2 == 3 && parser.getDepth() <= outerDepth) {
                                break;
                            }
                            if (!((type2 == 3 || type2 == 4) ? z : false)) {
                                String tag3 = parser.getName();
                                if (TAG_CERT.equals(tag3)) {
                                    try {
                                        HwCertification cert = processCertTag(parser);
                                        if (cert != null && cert.getPackageName() != null) {
                                            certs.put(cert.getPackageName(), cert);
                                        }
                                    } catch (Throwable th) {
                                        th = th;
                                        throw th;
                                    }
                                } else {
                                    HwAuthLogger.e("HwCertificationManager", "readHwCertXml:unknow tag:" + tag3);
                                    XmlUtils.skipCurrentTag(parser);
                                }
                                z = true;
                            }
                        } else {
                            break;
                        }
                    }
                    try {
                        $closeResource(null, fr);
                    } catch (Throwable th2) {
                        th = th2;
                        try {
                            throw th;
                        } catch (Throwable th3) {
                            $closeResource(th, fis);
                            throw th3;
                        }
                    }
                    try {
                        $closeResource(null, fis);
                    } catch (XmlPullParserException e) {
                    } catch (FileNotFoundException e2) {
                        HwAuthLogger.e("HwCertificationManager", "readHwCertXml:FileNotFoundException");
                    } catch (RuntimeException e3) {
                        HwAuthLogger.e("HwCertificationManager", "readHwCertXml:RuntimeException");
                    } catch (Exception e4) {
                        HwAuthLogger.e("HwCertificationManager", "readHwCertXml:IOException");
                    }
                } catch (Throwable th4) {
                    th = th4;
                    throw th;
                }
            } catch (Throwable th5) {
                th = th5;
                throw th;
            }
        } catch (XmlPullParserException e5) {
            HwAuthLogger.e("HwCertificationManager", "readHwCertXml:XmlPullParserException.");
        } catch (FileNotFoundException e6) {
            HwAuthLogger.e("HwCertificationManager", "readHwCertXml:FileNotFoundException");
        } catch (RuntimeException e7) {
            HwAuthLogger.e("HwCertificationManager", "readHwCertXml:RuntimeException");
        } catch (Exception e8) {
            HwAuthLogger.e("HwCertificationManager", "readHwCertXml:IOException");
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
            } catch (Throwable th) {
                x0.addSuppressed(th);
            }
        } else {
            x1.close();
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:21:0x007f, code lost:
        r1 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0080, code lost:
        $closeResource(r0, r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0083, code lost:
        throw r1;
     */
    public boolean updateMdmCertBlacklist(List<String> blacklist) {
        if (blacklist == null) {
            return false;
        }
        File blackListXml = new File(new File(Environment.getDataDirectory(), SYSTEM_DIR), BLACKLIST_FILE_NAME);
        if (blackListXml.exists() && !blackListXml.delete()) {
            return false;
        }
        try {
            FileOutputStream fis = new FileOutputStream(blackListXml, false);
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fis, StandardCharsets.UTF_8.name());
            out.startDocument(null, true);
            out.startTag(null, TAG_BLACKLIST);
            for (String signature : blacklist) {
                out.startTag(null, TAG_SIGNATURE);
                out.attribute(null, "value", signature);
                out.endTag(null, TAG_SIGNATURE);
            }
            out.endTag(null, TAG_BLACKLIST);
            out.endDocument();
            fis.flush();
            HwCertificationManager.getIntance().updateMdmCertBlacklist();
            $closeResource(null, fis);
            return true;
        } catch (IOException e) {
            HwAuthLogger.e("HwCertificationManager", "updateMdmCertBlacklist failed, IOException.");
            return false;
        } catch (IllegalArgumentException e2) {
            HwAuthLogger.e("HwCertificationManager", "updateMdmCertBlacklist failed, IllegalArgumentException.");
            return false;
        } catch (IllegalStateException e3) {
            HwAuthLogger.e("HwCertificationManager", "updateMdmCertBlacklist failed, IllegalStateException.");
            return false;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0099, code lost:
        r6 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x009a, code lost:
        $closeResource(r5, r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x009d, code lost:
        throw r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x00a0, code lost:
        r5 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x00a1, code lost:
        $closeResource(r4, r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x00a4, code lost:
        throw r5;
     */
    public void readMdmCertBlacklist(List<String> signatureBlackList) {
        if (signatureBlackList != null) {
            File hwBlackList = new File(new File(Environment.getDataDirectory(), SYSTEM_DIR), BLACKLIST_FILE_NAME);
            if (!hwBlackList.exists()) {
                HwAuthLogger.e("HwCertificationManager", "BlackList file not found.");
                return;
            }
            try {
                FileInputStream fis = new FileInputStream(hwBlackList);
                InputStreamReader fr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                parser.setInput(fr);
                int eventType = parser.getEventType();
                while (eventType != 1) {
                    if (eventType == 2) {
                        if (TAG_SIGNATURE.equals(parser.getName())) {
                            String attributeValue = parser.getAttributeValue(null, "value");
                            if (TextUtils.isEmpty(attributeValue)) {
                                eventType = parser.next();
                            } else {
                                signatureBlackList.add(attributeValue);
                                eventType = parser.next();
                            }
                        }
                    }
                    eventType = parser.next();
                }
                HwAuthLogger.i("HwCertificationManager", "the size of signatureBlackList is : " + signatureBlackList.size());
                $closeResource(null, fr);
                $closeResource(null, fis);
            } catch (XmlPullParserException e) {
                HwAuthLogger.e("HwCertificationManager", "readMdmCertBlacklist failed, XmlPullParserException.");
            } catch (FileNotFoundException e2) {
                HwAuthLogger.e("HwCertificationManager", "readMdmCertBlacklist failed, FileNotFoundException.");
            } catch (UnsupportedEncodingException e3) {
                HwAuthLogger.e("HwCertificationManager", "readMdmCertBlacklist failed, UnsupportedEncodingException.");
            } catch (IOException e4) {
                HwAuthLogger.e("HwCertificationManager", "readMdmCertBlacklist failed, IOException.");
            }
        }
    }

    private HwCertification processCertTag(XmlPullParser parser) {
        try {
            HwCertification cert = new HwCertification();
            readOneCertXml(parser, cert);
            return cert;
        } catch (XmlPullParserException e) {
            HwAuthLogger.e("HwCertificationManager", "processCertTag:XmlPullParserException.");
            return null;
        } catch (Exception e2) {
            HwAuthLogger.e("HwCertificationManager", "processCertTag:Exception");
            return null;
        }
    }

    /* JADX INFO: Multiple debug info for r4v3 java.io.FileOutputStream: [D('isDeleted' boolean), D('fis' java.io.FileOutputStream)] */
    public boolean updateHwCert(List<HwCertification> certs) {
        File certXml = new File(new File(Environment.getDataDirectory(), SYSTEM_DIR), FILE_NAME);
        if (certXml.exists()) {
            boolean isDeleted = false;
            try {
                isDeleted = certXml.delete();
            } catch (SecurityException e) {
                HwAuthLogger.e("HwCertificationManager", "SecurityException when delete certXml in updateHwCert");
            }
            if (!isDeleted) {
                HwAuthLogger.e("HwCertificationManager", "canot delete certXml in updateHwCert.");
                return false;
            }
        }
        FileOutputStream fis = null;
        try {
            FileOutputStream fis2 = new FileOutputStream(certXml, false);
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fis2, "utf-8");
            out.startDocument(null, true);
            out.startTag(null, TAG_HWCERT);
            for (HwCertification cert : certs) {
                writeToXml(out, cert);
            }
            out.endTag(null, TAG_HWCERT);
            out.endDocument();
            fis2.flush();
            try {
                fis2.close();
            } catch (IOException ex) {
                HwAuthLogger.e("HwCertificationManager", "can not close fis in updateHwCert, ex is " + ex);
            }
            return true;
        } catch (IOException e2) {
            if (0 != 0) {
                try {
                    fis.close();
                } catch (IOException ex2) {
                    HwAuthLogger.e("HwCertificationManager", "can not close fis in updateHwCert, ex is " + ex2);
                }
            }
            return false;
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    fis.close();
                } catch (IOException ex3) {
                    HwAuthLogger.e("HwCertificationManager", "can not close fis in updateHwCert, ex is " + ex3);
                }
            }
            throw th;
        }
    }

    public static void readOneCertXml(XmlPullParser parser, HwCertification cert) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (!(type == 3 || type == 4)) {
                String tag = parser.getName();
                IProcessor processor = HwCertificationProcessor.getProcessors(tag);
                if (processor == null) {
                    HwAuthLogger.e("HwCertificationManager", "readOneCertXml:processor is null");
                    return;
                }
                processor.parseXmlTag(tag, parser, cert);
                if (!processor.parserCert(cert)) {
                    HwAuthLogger.e("HwCertificationManager", "processCertTag:error, tag is " + tag);
                    return;
                }
                XmlUtils.skipCurrentTag(parser);
            }
        }
    }

    public static void writeToXml(XmlSerializer out, HwCertification cert) throws IllegalArgumentException, IllegalStateException, IOException {
        out.startTag(null, TAG_CERT);
        out.attribute(null, "name", cert.mCertificationData.mPackageName);
        if (cert.getVersion() != null && !cert.getVersion().isEmpty()) {
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
        if (cert.getCertificate() != null && !cert.getCertificate().isEmpty()) {
            out.startTag(null, HwCertification.KEY_CERTIFICATE);
            out.attribute(null, "value", cert.mCertificationData.mCertificate);
            out.endTag(null, HwCertification.KEY_CERTIFICATE);
        }
        if (cert.getExtenstion() != null && !cert.getExtenstion().isEmpty()) {
            out.startTag(null, HwCertification.KEY_EXTENSION);
            out.attribute(null, "value", cert.mCertificationData.mExtenstion);
            out.endTag(null, HwCertification.KEY_EXTENSION);
        }
        out.startTag(null, HwCertification.KEY_SIGNATURE);
        out.attribute(null, "value", cert.mCertificationData.mSignature);
        out.endTag(null, HwCertification.KEY_SIGNATURE);
        out.startTag(null, HwCertification.KEY_SIGNATURE2);
        out.attribute(null, "value", cert.mCertificationData.mSignature2);
        out.endTag(null, HwCertification.KEY_SIGNATURE2);
        out.endTag(null, TAG_CERT);
    }
}
