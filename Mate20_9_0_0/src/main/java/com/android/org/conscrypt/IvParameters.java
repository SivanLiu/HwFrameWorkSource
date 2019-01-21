package com.android.org.conscrypt;

import java.io.IOException;
import java.security.AlgorithmParametersSpi;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.spec.IvParameterSpec;

public class IvParameters extends AlgorithmParametersSpi {
    private byte[] iv;

    public static class AES extends IvParameters {
    }

    public static class ChaCha20 extends IvParameters {
    }

    public static class DESEDE extends IvParameters {
    }

    protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
        if (algorithmParameterSpec instanceof IvParameterSpec) {
            this.iv = (byte[]) ((IvParameterSpec) algorithmParameterSpec).getIV().clone();
            return;
        }
        throw new InvalidParameterSpecException("Only IvParameterSpec is supported");
    }

    protected void engineInit(byte[] bytes) throws IOException {
        long readRef = 0;
        try {
            readRef = NativeCrypto.asn1_read_init(bytes);
            byte[] newIv = NativeCrypto.asn1_read_octetstring(readRef);
            if (NativeCrypto.asn1_read_is_empty(readRef)) {
                this.iv = newIv;
                return;
            }
            throw new IOException("Error reading ASN.1 encoding");
        } finally {
            NativeCrypto.asn1_read_free(readRef);
        }
    }

    protected void engineInit(byte[] bytes, String format) throws IOException {
        if (format == null || format.equals("ASN.1")) {
            engineInit(bytes);
        } else if (format.equals("RAW")) {
            this.iv = (byte[]) bytes.clone();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported format: ");
            stringBuilder.append(format);
            throw new IOException(stringBuilder.toString());
        }
    }

    protected <T extends AlgorithmParameterSpec> T engineGetParameterSpec(Class<T> aClass) throws InvalidParameterSpecException {
        if (aClass == IvParameterSpec.class) {
            return new IvParameterSpec(this.iv);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Incompatible AlgorithmParametersSpec class: ");
        stringBuilder.append(aClass);
        throw new InvalidParameterSpecException(stringBuilder.toString());
    }

    protected byte[] engineGetEncoded() throws IOException {
        long cbbRef = 0;
        try {
            cbbRef = NativeCrypto.asn1_write_init();
            NativeCrypto.asn1_write_octetstring(cbbRef, this.iv);
            byte[] asn1_write_finish = NativeCrypto.asn1_write_finish(cbbRef);
            NativeCrypto.asn1_write_free(cbbRef);
            return asn1_write_finish;
        } catch (IOException e) {
            NativeCrypto.asn1_write_cleanup(cbbRef);
            throw e;
        } catch (Throwable th) {
            NativeCrypto.asn1_write_free(cbbRef);
        }
    }

    protected byte[] engineGetEncoded(String format) throws IOException {
        if (format == null || format.equals("ASN.1")) {
            return engineGetEncoded();
        }
        if (format.equals("RAW")) {
            return (byte[]) this.iv.clone();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported format: ");
        stringBuilder.append(format);
        throw new IOException(stringBuilder.toString());
    }

    protected String engineToString() {
        return "Conscrypt IV AlgorithmParameters";
    }
}
