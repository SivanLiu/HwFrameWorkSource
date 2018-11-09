package com.android.org.conscrypt;

import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public final class InternalUtil {
    public static PublicKey logKeyToPublicKey(byte[] logKey) throws NoSuchAlgorithmException {
        return new OpenSSLKey(NativeCrypto.EVP_parse_public_key(logKey)).getPublicKey();
    }

    public static PublicKey readPublicKeyPem(InputStream pem) throws InvalidKeyException, NoSuchAlgorithmException {
        return OpenSSLKey.fromPublicKeyPemInputStream(pem).getPublicKey();
    }

    public static byte[] getOcspSingleExtension(byte[] ocspResponse, String oid, long x509Ref, long issuerX509Ref) {
        return NativeCrypto.get_ocsp_single_extension(ocspResponse, oid, x509Ref, issuerX509Ref);
    }

    private InternalUtil() {
    }
}
