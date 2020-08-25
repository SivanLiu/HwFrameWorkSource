package com.android.server.pm.auth.processor;

import android.content.pm.PackageParser;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.util.CryptionUtils;
import com.android.server.pm.auth.util.HwAuthLogger;
import com.android.server.pm.auth.util.Utils;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipFile;
import org.xmlpull.v1.XmlPullParser;

public class ApkHashProcessor extends BaseProcessor {
    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean readCert(String line, HwCertification.CertificationData rawCert) {
        if (line == null || line.isEmpty() || !line.startsWith(HwCertification.KEY_APK_HASH)) {
            return false;
        }
        String apkHash = line.substring(HwCertification.KEY_APK_HASH.length() + 1);
        if (apkHash == null || apkHash.isEmpty()) {
            HwAuthLogger.e("HwCertificationManager", "AH_RC is empty");
            return false;
        }
        rawCert.mApkHash = apkHash;
        return true;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean parserCert(HwCertification rawCert) {
        HwCertification.CertificationData certData = rawCert.mCertificationData;
        if (certData.mApkHash == null || certData.mApkHash.isEmpty()) {
            HwAuthLogger.e("HwCertificationManager", "AH_PC error");
            return false;
        }
        rawCert.setApkHash(certData.mApkHash);
        return true;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean verifyCert(PackageParser.Package pkg, HwCertification cert) {
        if (cert.isContainSpecialPermissions()) {
            return true;
        }
        String hashFromcert = cert.getApkHash();
        if (hashFromcert == null || hashFromcert.isEmpty()) {
            return false;
        }
        if (!cert.isReleased()) {
            if ("*".equals(hashFromcert)) {
                return true;
            }
            HwAuthLogger.e("HwCertificationManager", "AH_VC error not * in debug cert");
            return false;
        } else if ("*".equals(hashFromcert)) {
            return true;
        } else {
            try {
                if (hashFromcert.equals(generateAPKHash(cert))) {
                    return true;
                }
                HwAuthLogger.e("HwCertificationManager", "AH_VC error:not same");
                return false;
            } catch (Exception e) {
                HwAuthLogger.e("HwCertificationManager", "AH_VC failed");
                return false;
            }
        }
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        if (!HwCertification.KEY_APK_HASH.equals(tag)) {
            return false;
        }
        cert.mCertificationData.mApkHash = parser.getAttributeValue(null, "value");
        return true;
    }

    private String generateAPKHash(HwCertification rawCert) throws Exception {
        byte[] manifest;
        if (rawCert == null) {
            HwAuthLogger.e("HwCertificationManager", "AH_G cert is null");
            return "";
        }
        String apkHashFromFile = null;
        ZipFile zFile = rawCert.getZipFile();
        String sfFilePath = null;
        if (zFile != null) {
            try {
                sfFilePath = Utils.getSfFileName(zFile);
            } catch (Throwable th) {
                rawCert.resetZipFile();
                throw th;
            }
        }
        if (sfFilePath == null || !Utils.isUsingSignatureSchemaV2(zFile, zFile.getEntry(sfFilePath))) {
            HwAuthLogger.i("HwCertificationManager", " AH_G not V2");
            manifest = Utils.getManifestFileWithoutHwCER(rawCert.getApkFile());
        } else {
            HwAuthLogger.i("HwCertificationManager", "AH_G V2 sort manifest content");
            manifest = Utils.getManifestFileWithoutHwCER(zFile, zFile.getEntry("META-INF/MANIFEST.MF"));
        }
        if (manifest == null || manifest.length == 0) {
            rawCert.resetZipFile();
            return null;
        }
        try {
            apkHashFromFile = Utils.bytesToString(CryptionUtils.sha256(manifest));
        } catch (NoSuchAlgorithmException e) {
            HwAuthLogger.e("HwCertificationManager", "AH_G error");
        }
        rawCert.resetZipFile();
        return apkHashFromFile;
    }
}
