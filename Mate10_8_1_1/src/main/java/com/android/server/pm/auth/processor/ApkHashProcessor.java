package com.android.server.pm.auth.processor;

import android.content.pm.PackageParser.Package;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.HwCertification.CertificationData;
import com.android.server.pm.auth.util.CryptionUtils;
import com.android.server.pm.auth.util.HwAuthLogger;
import com.android.server.pm.auth.util.Utils;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipFile;
import org.xmlpull.v1.XmlPullParser;

public class ApkHashProcessor extends BaseProcessor {
    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean readCert(String line, CertificationData rawCert) {
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

    public boolean parserCert(HwCertification rawCert) {
        CertificationData certData = rawCert.mCertificationData;
        if (certData.mApkHash == null || (certData.mApkHash.isEmpty() ^ 1) == 0) {
            HwAuthLogger.e("HwCertificationManager", "AH_PC error");
            return false;
        }
        rawCert.setApkHash(certData.mApkHash);
        return true;
    }

    public boolean verifyCert(Package pkg, HwCertification cert) {
        if (HwAuthLogger.getHWFLOW()) {
            HwAuthLogger.i("HwCertificationManager", "AH_VC start");
        }
        String hashFromcert = cert.getApkHash();
        if (hashFromcert == null || hashFromcert.isEmpty()) {
            return false;
        }
        if (cert.isReleased()) {
            if ("*".equals(hashFromcert)) {
                if (HwAuthLogger.getHWDEBUG()) {
                    HwAuthLogger.d("HwCertificationManager", "AH_VC is * in released cert");
                }
                return true;
            }
            try {
                if (hashFromcert.equals(generateAPKHash(cert))) {
                    if (HwAuthLogger.getHWFLOW()) {
                        HwAuthLogger.i("HwCertificationManager", "AH_VC line ok released cert");
                    }
                    return true;
                }
                HwAuthLogger.e("HwCertificationManager", "AH_VC error:not same");
                return false;
            } catch (Exception e) {
                HwAuthLogger.e("HwCertificationManager", "AH_VC failed");
                return false;
            }
        } else if ("*".equals(hashFromcert)) {
            if (HwAuthLogger.getHWDEBUG()) {
                HwAuthLogger.d("HwCertificationManager", "AH_VC debug cert,line ok");
            }
            return true;
        } else {
            HwAuthLogger.e("HwCertificationManager", "AH_VC error not * in debug cert");
            return false;
        }
    }

    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        if (!HwCertification.KEY_APK_HASH.equals(tag)) {
            return false;
        }
        cert.mCertificationData.mApkHash = parser.getAttributeValue(null, "value");
        return true;
    }

    private String generateAPKHash(HwCertification rawCert) throws Exception {
        if (rawCert == null) {
            HwAuthLogger.e("HwCertificationManager", "AH_G cert is null");
            return "";
        }
        byte[] manifest;
        String apkHashFromFile = null;
        ZipFile zFile = rawCert.getZipFile();
        String sfFilePath = null;
        if (zFile != null) {
            try {
                sfFilePath = Utils.getSfFileName(zFile);
            } catch (NoSuchAlgorithmException e) {
                HwAuthLogger.e("HwCertificationManager", "AH_G error");
            } catch (Throwable th) {
                rawCert.resetZipFile();
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
        apkHashFromFile = Utils.bytesToString(CryptionUtils.sha256(manifest));
        rawCert.resetZipFile();
        return apkHashFromFile;
    }
}
