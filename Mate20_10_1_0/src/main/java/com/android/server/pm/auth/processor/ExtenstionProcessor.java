package com.android.server.pm.auth.processor;

import android.content.pm.PackageParser;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.util.HwAuthLogger;
import org.xmlpull.v1.XmlPullParser;

public class ExtenstionProcessor extends BaseProcessor {
    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean readCert(String line, HwCertification.CertificationData rawCert) {
        if (line == null || line.isEmpty()) {
            HwAuthLogger.e("HwCertificationManager", "ET_RC line is null");
            return false;
        } else if (rawCert == null) {
            HwAuthLogger.e("HwCertificationManager", "ET_RC is empty");
            return false;
        } else if (line.startsWith("Extension:")) {
            rawCert.mExtenstion = line.substring(HwCertification.KEY_EXTENSION.length() + 1);
            return true;
        } else {
            HwAuthLogger.e("HwCertificationManager", "ET_RC error");
            return false;
        }
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean parserCert(HwCertification rawCert) {
        if (rawCert == null) {
            HwAuthLogger.e("HwCertificationManager", "ET_PC error cert is null");
            return false;
        }
        HwCertification.CertificationData certData = rawCert.mCertificationData;
        String extenstion = certData.mExtenstion;
        if (extenstion == null || extenstion.isEmpty()) {
            HwAuthLogger.e("HwCertificationManager", "ET_PC error is null");
            return false;
        }
        rawCert.setExtenstion(certData.mExtenstion);
        return true;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean verifyCert(PackageParser.Package pkg, HwCertification cert) {
        if (pkg == null || cert == null) {
            HwAuthLogger.e("HwCertificationManager", "ET_VC error package or cert is null");
            return false;
        }
        String extenstion = cert.getExtenstion();
        if (extenstion != null && !extenstion.isEmpty()) {
            return true;
        }
        HwAuthLogger.e("HwCertificationManager", "ET_VC is empty");
        return false;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        if (HwCertification.KEY_EXTENSION.equals(tag)) {
            cert.mCertificationData.mExtenstion = parser.getAttributeValue(null, "value");
            return true;
        }
        HwAuthLogger.e("HwCertificationManager", "ET_PX error");
        return false;
    }
}
