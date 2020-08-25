package com.android.server.pm.auth.processor;

import android.content.pm.PackageParser;
import android.text.TextUtils;
import com.android.server.pm.auth.DevicePublicKeyLoader;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.HwCertificationManager;
import com.android.server.pm.auth.util.CryptionUtils;
import com.android.server.pm.auth.util.HwAuthLogger;
import com.android.server.pm.auth.util.Utils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import org.xmlpull.v1.XmlPullParser;

public class Signature2Processor extends BaseProcessor {
    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean readCert(String line, HwCertification.CertificationData rawCert) {
        if (TextUtils.isEmpty(line) || rawCert == null) {
            HwAuthLogger.e("HwCertificationManager", "read cert null");
            return false;
        } else if (!line.startsWith(HwCertification.KEY_SIGNATURE2)) {
            return false;
        } else {
            String key = line.substring(HwCertification.KEY_SIGNATURE2.length() + 1);
            if (TextUtils.isEmpty(key)) {
                HwAuthLogger.e("HwCertificationManager", "SN2_RC is empty");
                return false;
            }
            rawCert.mSignature2 = key;
            return true;
        }
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean parserCert(HwCertification rawCert) {
        if (rawCert == null) {
            HwAuthLogger.e("HwCertificationManager", "parse cert null");
            return false;
        }
        HwCertification.CertificationData certData = rawCert.mCertificationData;
        if (TextUtils.isEmpty(certData.mSignature2)) {
            HwAuthLogger.e("HwCertificationManager", "parser signature2 is null, " + rawCert.getPackageName());
            return false;
        }
        rawCert.setSignature2(certData.mSignature2);
        return true;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean verifyCert(PackageParser.Package pkg, HwCertification cert) {
        if (cert == null || pkg == null) {
            return false;
        }
        String signature2 = cert.mCertificationData.mSignature2;
        if (TextUtils.isEmpty(signature2)) {
            HwAuthLogger.e("HwCertificationManager", "This is old cert, no signature2");
            return false;
        }
        HwCertificationManager certificationManager = HwCertificationManager.getIntance();
        if (certificationManager == null) {
            return false;
        }
        if (certificationManager.checkMdmCertBlacklist(signature2)) {
            HwAuthLogger.i("HwCertificationManager", "signature2 contains in blacklist, need to return false.");
            return false;
        }
        try {
            byte[] digestFromFileText = CryptionUtils.sha256(SignatureProcessor.generatePartlyContent(cert.isReleased(), cert).getBytes("UTF-8"));
            PublicKey pubKey = DevicePublicKeyLoader.getPublicKeyForBase64(DevicePublicKeyLoader.EMUI10_PK);
            if (pubKey == null) {
                HwAuthLogger.e("HwCertificationManager", "SN2_VC pubKey is null");
                return false;
            }
            try {
                boolean isVerifySuccess = CryptionUtils.verify(digestFromFileText, pubKey, Utils.stringToBytes(signature2));
                StringBuilder sb = new StringBuilder();
                sb.append("SN2_VC result:");
                sb.append(isVerifySuccess ? "OK" : "not the same");
                HwAuthLogger.i("HwCertificationManager", sb.toString());
                return isVerifySuccess;
            } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
                HwAuthLogger.e("HwCertificationManager", "signature2 verify Exception");
                return false;
            }
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e2) {
            HwAuthLogger.e("HwCertificationManager", "sha256 has Exception");
            return false;
        }
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        if (cert == null || parser == null || !HwCertification.KEY_SIGNATURE2.equals(tag)) {
            return false;
        }
        cert.mCertificationData.mSignature2 = parser.getAttributeValue(null, "value");
        return true;
    }
}
