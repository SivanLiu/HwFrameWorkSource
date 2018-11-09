package com.android.org.conscrypt;

import java.io.IOException;
import java.security.AlgorithmParametersSpi;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

public final class GCMParameters extends AlgorithmParametersSpi {
    private static final int DEFAULT_TLEN = 96;
    private byte[] iv;
    private int tLen;

    GCMParameters(int tLen, byte[] iv) {
        this.tLen = tLen;
        this.iv = iv;
    }

    int getTLen() {
        return this.tLen;
    }

    byte[] getIV() {
        return this.iv;
    }

    protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
        GCMParameters params = Platform.fromGCMParameterSpec(algorithmParameterSpec);
        if (params == null) {
            throw new InvalidParameterSpecException("Only GCMParameterSpec is supported");
        }
        this.tLen = params.tLen;
        this.iv = params.iv;
    }

    protected void engineInit(byte[] bytes) throws IOException {
        long readRef = 0;
        long seqRef = 0;
        try {
            readRef = NativeCrypto.asn1_read_init(bytes);
            seqRef = NativeCrypto.asn1_read_sequence(readRef);
            byte[] newIv = NativeCrypto.asn1_read_octetstring(seqRef);
            int newTlen = DEFAULT_TLEN;
            if (!NativeCrypto.asn1_read_is_empty(seqRef)) {
                newTlen = ((int) NativeCrypto.asn1_read_uint64(seqRef)) * 8;
            }
            if (NativeCrypto.asn1_read_is_empty(seqRef) && (NativeCrypto.asn1_read_is_empty(readRef) ^ 1) == 0) {
                this.iv = newIv;
                this.tLen = newTlen;
                return;
            }
            throw new IOException("Error reading ASN.1 encoding");
        } finally {
            NativeCrypto.asn1_read_free(seqRef);
            NativeCrypto.asn1_read_free(readRef);
        }
    }

    protected void engineInit(byte[] bytes, String format) throws IOException {
        if (format == null || format.equals("ASN.1")) {
            engineInit(bytes);
            return;
        }
        throw new IOException("Unsupported format: " + format);
    }

    protected <T extends AlgorithmParameterSpec> T engineGetParameterSpec(Class<T> aClass) throws InvalidParameterSpecException {
        if (aClass != null && aClass.getName().equals("javax.crypto.spec.GCMParameterSpec")) {
            return (AlgorithmParameterSpec) aClass.cast(Platform.toGCMParameterSpec(this.tLen, this.iv));
        }
        throw new InvalidParameterSpecException("Unsupported class: " + aClass);
    }

    protected byte[] engineGetEncoded() throws IOException {
        try {
            long cbbRef = NativeCrypto.asn1_write_init();
            long seqRef = NativeCrypto.asn1_write_sequence(cbbRef);
            NativeCrypto.asn1_write_octetstring(seqRef, this.iv);
            if (this.tLen != DEFAULT_TLEN) {
                NativeCrypto.asn1_write_uint64(seqRef, (long) (this.tLen / 8));
            }
            byte[] asn1_write_finish = NativeCrypto.asn1_write_finish(cbbRef);
            NativeCrypto.asn1_write_free(seqRef);
            NativeCrypto.asn1_write_free(cbbRef);
            return asn1_write_finish;
        } catch (IOException e) {
            NativeCrypto.asn1_write_cleanup(0);
            throw e;
        } catch (Throwable th) {
            NativeCrypto.asn1_write_free(0);
            NativeCrypto.asn1_write_free(0);
        }
    }

    protected byte[] engineGetEncoded(String format) throws IOException {
        if (format == null || format.equals("ASN.1")) {
            return engineGetEncoded();
        }
        throw new IOException("Unsupported format: " + format);
    }

    protected String engineToString() {
        return "Conscrypt GCM AlgorithmParameters";
    }
}
