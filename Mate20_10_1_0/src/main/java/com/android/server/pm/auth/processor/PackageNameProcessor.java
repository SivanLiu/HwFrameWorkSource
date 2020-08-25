package com.android.server.pm.auth.processor;

import android.content.pm.PackageParser;
import android.text.TextUtils;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.util.HwAuthLogger;
import org.xmlpull.v1.XmlPullParser;

public class PackageNameProcessor extends BaseProcessor {
    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean readCert(String line, HwCertification.CertificationData rawCert) {
        if (line == null || line.isEmpty() || !line.startsWith("PackageName")) {
            return false;
        }
        String key = line.substring("PackageName".length() + 1);
        if (key == null || key.isEmpty()) {
            HwAuthLogger.e("HwCertificationManager", "PN_RC is empty");
            return false;
        }
        rawCert.mPackageName = key;
        return true;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean parserCert(HwCertification rawCert) {
        HwCertification.CertificationData certData = rawCert.mCertificationData;
        if (certData.mPackageName == null || certData.mPackageName.isEmpty()) {
            HwAuthLogger.e("HwCertificationManager", "PN_PC error");
            return false;
        }
        rawCert.setPackageName(certData.mPackageName);
        return true;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean verifyCert(PackageParser.Package pkg, HwCertification cert) {
        String pkgName = cert.getPackageName();
        if (pkgName == null || pkgName.isEmpty()) {
            return false;
        }
        String realPkgName = pkg.packageName;
        if (pkgName.equals(realPkgName)) {
            return true;
        }
        HwAuthLogger.w("HwCertificationManager", "PN_PC error rn is :" + realPkgName + "pkgName in HUAWEI.CER is" + pkgName);
        return false;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        if (!"PackageName".equals(tag)) {
            return false;
        }
        String packageName = parser.getAttributeValue(null, "value");
        if (!TextUtils.isEmpty(packageName)) {
            packageName = packageName.intern();
        }
        cert.mCertificationData.mPackageName = packageName;
        return true;
    }
}
