package com.android.server.pm.auth.processor;

import android.content.pm.PackageParser;
import com.android.server.pm.auth.HwCertification;
import org.xmlpull.v1.XmlPullParser;

public abstract class BaseProcessor implements IProcessor {
    @Override // com.android.server.pm.auth.processor.IProcessor
    public boolean readCert(String line, HwCertification.CertificationData rawCert) {
        return false;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor
    public boolean parserCert(HwCertification rawCert) {
        return false;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor
    public boolean verifyCert(PackageParser.Package pkg, HwCertification cert) {
        return false;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor
    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        return false;
    }
}
