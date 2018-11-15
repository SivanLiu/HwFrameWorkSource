package com.android.server.security.securityprofile;

import android.util.Slog;
import huawei.android.security.securityprofile.ApkDigest;
import huawei.android.security.securityprofile.DigestMatcher;
import huawei.android.security.securityprofile.PolicyExtractor;
import huawei.android.security.securityprofile.PolicyExtractor.PolicyNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;

public class PolicyVerifier {
    private static final String TAG = "SecurityProfileService";

    public static boolean packageHasValidPolicy(String packageName, String apkPath) {
        String str;
        StringBuilder stringBuilder;
        if (apkPath == null) {
            return false;
        }
        try {
            byte[] jws = PolicyExtractor.getPolicy(apkPath);
            ApkDigest apkDigest = PolicyExtractor.getDigest(packageName, jws);
            if (verifyAndDecodePolicy(jws) == null) {
                Slog.e(TAG, "Policy verification failed");
                return false;
            } else if (DigestMatcher.packageMatchesDigest(apkPath, apkDigest)) {
                return true;
            } else {
                Slog.e(TAG, "Package digest did not match policy digest");
                return false;
            }
        } catch (PolicyNotFoundException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Not found Exception ");
            stringBuilder.append(e.getMessage());
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("packageHasValidPolicy IOException:");
            stringBuilder.append(e2.getMessage());
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (Exception e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("packageHasValidPolicy Exception:");
            stringBuilder.append(e3.getMessage());
            Slog.e(str, stringBuilder.toString());
            return false;
        }
    }

    public static JSONObject verifyAndDecodePolicy(byte[] policyBlock) {
        try {
            String[] parts = StringFactory.newStringFromBytes(policyBlock, StandardCharsets.UTF_8).split("\\.");
            if (parts.length != 3) {
                return null;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(parts[0]);
            stringBuilder.append(".");
            stringBuilder.append(parts[1]);
            byte[] signedData = stringBuilder.toString().getBytes();
            byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
            JSONObject header = new JSONObject(StringFactory.newStringFromBytes(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8));
            ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode((String) header.getJSONArray("x5c").get(0)));
            Date timestamp = new Date();
            timestamp.setTime(header.optLong("timestamp", System.currentTimeMillis()));
            Signature sig = Signature.getInstance("SHA256withRSA");
            Certificate cert = CertificateFactory.getInstance("X.509").generateCertificate(bis);
            if (!new CertificateVerifier().verifyCertificateChain(Arrays.asList(new Certificate[]{cert}), timestamp)) {
                return null;
            }
            String valueofou = getSubjectAttr(cert, "OU");
            if (valueofou.equals("Huawei APK Production")) {
                sig.initVerify(cert);
                sig.update(signedData);
                if (sig.verify(signature)) {
                    return new JSONObject(new String(Base64.getUrlDecoder().decode(parts[1].getBytes())));
                }
                return null;
            }
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("the OU field:");
            stringBuilder2.append(valueofou);
            stringBuilder2.append(" of the subject is not correct");
            Slog.e(str, stringBuilder2.toString());
            return null;
        } catch (NoSuchAlgorithmException e) {
            Slog.e(TAG, e.getMessage());
            return null;
        } catch (CertificateException e2) {
            Slog.e(TAG, e2.getMessage());
            return null;
        } catch (JSONException e3) {
            Slog.e(TAG, e3.getMessage());
            return null;
        } catch (SignatureException e4) {
            Slog.e(TAG, e4.getMessage());
            return null;
        } catch (InvalidKeyException e5) {
            Slog.e(TAG, e5.getMessage());
            return null;
        }
    }

    public static String getSubjectAttr(Certificate cert, String field) {
        for (String attr : ((X509Certificate) cert).getSubjectDN().getName().split(",")) {
            String[] nameandvalue = attr.split("=");
            if (nameandvalue[0].equals(field)) {
                return nameandvalue[1];
            }
        }
        return null;
    }
}
