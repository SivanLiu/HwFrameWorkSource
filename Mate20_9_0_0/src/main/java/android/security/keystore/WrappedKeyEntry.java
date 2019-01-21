package android.security.keystore;

import java.security.KeyStore.Entry;
import java.security.spec.AlgorithmParameterSpec;

public class WrappedKeyEntry implements Entry {
    private final AlgorithmParameterSpec mAlgorithmParameterSpec;
    private final String mTransformation;
    private final byte[] mWrappedKeyBytes;
    private final String mWrappingKeyAlias;

    public WrappedKeyEntry(byte[] wrappedKeyBytes, String wrappingKeyAlias, String transformation, AlgorithmParameterSpec algorithmParameterSpec) {
        this.mWrappedKeyBytes = wrappedKeyBytes;
        this.mWrappingKeyAlias = wrappingKeyAlias;
        this.mTransformation = transformation;
        this.mAlgorithmParameterSpec = algorithmParameterSpec;
    }

    public byte[] getWrappedKeyBytes() {
        return this.mWrappedKeyBytes;
    }

    public String getWrappingKeyAlias() {
        return this.mWrappingKeyAlias;
    }

    public String getTransformation() {
        return this.mTransformation;
    }

    public AlgorithmParameterSpec getAlgorithmParameterSpec() {
        return this.mAlgorithmParameterSpec;
    }
}
