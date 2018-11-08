package android.util;

import android.content.pm.Signature;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public final class PackageUtils {
    private PackageUtils() {
    }

    public static String[] computeSignaturesSha256Digests(Signature[] signatures) {
        int signatureCount = signatures.length;
        String[] digests = new String[signatureCount];
        for (int i = 0; i < signatureCount; i++) {
            digests[i] = computeSha256Digest(signatures[i].toByteArray());
        }
        return digests;
    }

    public static String computeSignaturesSha256Digest(Signature[] signatures) {
        if (signatures.length == 1) {
            return computeSha256Digest(signatures[0].toByteArray());
        }
        return computeSignaturesSha256Digest(computeSignaturesSha256Digests(signatures));
    }

    public static String computeSignaturesSha256Digest(String[] sha256Digests) {
        int i = 0;
        if (sha256Digests.length == 1) {
            return sha256Digests[0];
        }
        Arrays.sort(sha256Digests);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int length = sha256Digests.length;
        while (i < length) {
            try {
                bytes.write(sha256Digests[i].getBytes());
            } catch (IOException e) {
            }
            i++;
        }
        return computeSha256Digest(bytes.toByteArray());
    }

    public static String computeSha256Digest(byte[] data) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
            messageDigest.update(data);
            return ByteStringUtils.toHexString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
