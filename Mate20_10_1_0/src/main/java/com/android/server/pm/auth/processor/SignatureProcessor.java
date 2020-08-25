package com.android.server.pm.auth.processor;

import android.content.pm.PackageParser;
import com.android.server.pm.auth.DevicePublicKeyLoader;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.HwCertificationManager;
import com.android.server.pm.auth.util.CryptionUtils;
import com.android.server.pm.auth.util.HwAuthLogger;
import com.android.server.pm.auth.util.Utils;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import java.security.PublicKey;
import org.xmlpull.v1.XmlPullParser;

public class SignatureProcessor extends BaseProcessor {
    private static final String SEPARATOR = "\r\n";

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean readCert(String line, HwCertification.CertificationData rawCert) {
        if (line == null || line.isEmpty() || !line.startsWith(HwCertification.KEY_SIGNATURE)) {
            return false;
        }
        String key = line.substring(HwCertification.KEY_SIGNATURE.length() + 1);
        if (key == null || key.isEmpty()) {
            HwAuthLogger.e("HwCertificationManager", "SN_RC is empty");
            return false;
        }
        rawCert.mSignature = key;
        return true;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean parserCert(HwCertification rawCert) {
        HwCertification.CertificationData certData = rawCert.mCertificationData;
        if (certData.mSignature == null || certData.mSignature.isEmpty()) {
            HwAuthLogger.e("HwCertificationManager", "SN_PC error");
            return false;
        }
        rawCert.setSignature(certData.mSignature);
        return true;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean verifyCert(PackageParser.Package pkg, HwCertification cert) {
        String orginal = cert.mCertificationData.mSignature;
        if (orginal == null || orginal.isEmpty()) {
            return false;
        }
        String contentFromText = generatePartlyContent(cert.isReleased(), cert);
        HwCertificationManager certificationManager = HwCertificationManager.getIntance();
        if (certificationManager == null) {
            return false;
        }
        if (certificationManager.checkMdmCertBlacklist(orginal)) {
            HwAuthLogger.i("HwCertificationManager", "signature contains in blacklist, need to return false.");
            return false;
        }
        try {
            byte[] digestFromFileText = CryptionUtils.sha256(contentFromText.getBytes("UTF-8"));
            PublicKey pubKey = DevicePublicKeyLoader.getPublicKey(HwCertificationManager.getIntance().getContext());
            if (pubKey == null) {
                return false;
            }
            boolean result = CryptionUtils.verify(digestFromFileText, pubKey, Utils.stringToBytes(orginal));
            if (HwAuthLogger.getHWFLOW()) {
                StringBuilder sb = new StringBuilder();
                sb.append("SN_VC result:");
                sb.append(result ? "OK" : "not the same");
                HwAuthLogger.i("HwCertificationManager", sb.toString());
            }
            return result;
        } catch (RuntimeException e) {
            HwAuthLogger.e("HwCertificationManager", "SN_VC RuntimeException when encounting");
            return false;
        } catch (Exception e2) {
            HwAuthLogger.e("HwCertificationManager", "SN_VC exception when encounting");
            return false;
        }
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        if (!HwCertification.KEY_SIGNATURE.equals(tag)) {
            return false;
        }
        cert.mCertificationData.mSignature = parser.getAttributeValue(null, "value");
        return true;
    }

    public static String generatePartlyContent(boolean isRelease, HwCertification rawCert) {
        String str = "";
        if (rawCert == null) {
            return str;
        }
        StringBuffer sb = new StringBuffer();
        String tDelveoperKey = rawCert.getDelveoperKey();
        String tPackageName = rawCert.getPackageName();
        String tPermissionsString = rawCert.mCertificationData.mPermissionsString;
        String tDeviceIdsString = rawCert.mCertificationData.mDeviceIdsString;
        String tPeriod = rawCert.mCertificationData.mPeriodString;
        String tApkHash = rawCert.getApkHash();
        String tCertificate = rawCert.getCertificate();
        String tVersion = rawCert.getVersion();
        String tExtenstion = rawCert.getExtenstion();
        if (tVersion != null && !tVersion.isEmpty()) {
            sb.append(HwCertification.KEY_VERSION);
            sb.append(AwarenessInnerConstants.COLON_KEY);
            sb.append(tVersion);
            sb.append(SEPARATOR);
        }
        sb.append(HwCertification.KEY_DEVELIOPER);
        sb.append(AwarenessInnerConstants.COLON_KEY);
        sb.append(tDelveoperKey == null ? str : tDelveoperKey);
        sb.append(SEPARATOR);
        sb.append("PackageName");
        sb.append(AwarenessInnerConstants.COLON_KEY);
        sb.append(tPackageName == null ? str : tPackageName);
        sb.append(SEPARATOR);
        sb.append(HwCertification.KEY_PERMISSIONS);
        sb.append(AwarenessInnerConstants.COLON_KEY);
        sb.append(tPermissionsString == null ? str : tPermissionsString);
        sb.append(SEPARATOR);
        if (!isRelease) {
            sb.append(HwCertification.KEY_DEVICE_IDS);
            sb.append(AwarenessInnerConstants.COLON_KEY);
            sb.append(tDeviceIdsString == null ? str : tDeviceIdsString);
            sb.append(SEPARATOR);
        } else {
            sb.append(HwCertification.KEY_DEVICE_IDS);
            sb.append(":*");
            sb.append(SEPARATOR);
        }
        sb.append(HwCertification.KEY_VALID_PERIOD);
        sb.append(AwarenessInnerConstants.COLON_KEY);
        sb.append(tPeriod);
        sb.append(SEPARATOR);
        if (isRelease) {
            sb.append(HwCertification.KEY_APK_HASH);
            sb.append(AwarenessInnerConstants.COLON_KEY);
            if (tApkHash != null) {
                str = tApkHash;
            }
            sb.append(str);
            sb.append(SEPARATOR);
        } else {
            sb.append(HwCertification.KEY_APK_HASH);
            sb.append(":*");
            sb.append(SEPARATOR);
        }
        if (tCertificate != null && !tCertificate.isEmpty()) {
            sb.append(HwCertification.KEY_CERTIFICATE);
            sb.append(AwarenessInnerConstants.COLON_KEY);
            sb.append(tCertificate);
            sb.append(SEPARATOR);
        }
        if (tExtenstion != null && !tExtenstion.isEmpty()) {
            sb.append(HwCertification.KEY_EXTENSION);
            sb.append(AwarenessInnerConstants.COLON_KEY);
            sb.append(tExtenstion);
            sb.append(SEPARATOR);
        }
        return sb.toString();
    }
}
