package com.android.org.conscrypt;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.BadPaddingException;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.PSource.PSpecified;
import javax.crypto.spec.SecretKeySpec;

abstract class OpenSSLCipherRSA extends CipherSpi {
    private byte[] buffer;
    private int bufferOffset;
    boolean encrypting;
    private boolean inputTooLarge;
    OpenSSLKey key;
    int padding = 1;
    boolean usingPrivateKey;

    public static abstract class DirectRSA extends OpenSSLCipherRSA {
        public DirectRSA(int padding) {
            super(padding);
        }

        int doCryptoOperation(byte[] tmpBuf, byte[] output) throws BadPaddingException, IllegalBlockSizeException {
            if (!this.encrypting) {
                try {
                    if (this.usingPrivateKey) {
                        return NativeCrypto.RSA_private_decrypt(tmpBuf.length, tmpBuf, output, this.key.getNativeRef(), this.padding);
                    }
                    return NativeCrypto.RSA_public_decrypt(tmpBuf.length, tmpBuf, output, this.key.getNativeRef(), this.padding);
                } catch (SignatureException e) {
                    IllegalBlockSizeException newE = new IllegalBlockSizeException();
                    newE.initCause(e);
                    throw newE;
                }
            } else if (this.usingPrivateKey) {
                return NativeCrypto.RSA_private_encrypt(tmpBuf.length, tmpBuf, output, this.key.getNativeRef(), this.padding);
            } else {
                return NativeCrypto.RSA_public_encrypt(tmpBuf.length, tmpBuf, output, this.key.getNativeRef(), this.padding);
            }
        }
    }

    static class OAEP extends OpenSSLCipherRSA {
        private byte[] label;
        private long mgf1Md;
        private long oaepMd;
        private int oaepMdSizeBytes;
        private EVP_PKEY_CTX pkeyCtx;

        public static final class SHA1 extends OAEP {
            public SHA1() {
                super(SHA1.EVP_MD, SHA1.SIZE_BYTES);
            }
        }

        public static final class SHA224 extends OAEP {
            public SHA224() {
                super(SHA224.EVP_MD, SHA224.SIZE_BYTES);
            }
        }

        public static final class SHA256 extends OAEP {
            public SHA256() {
                super(SHA256.EVP_MD, SHA256.SIZE_BYTES);
            }
        }

        public static final class SHA384 extends OAEP {
            public SHA384() {
                super(SHA384.EVP_MD, SHA384.SIZE_BYTES);
            }
        }

        public static final class SHA512 extends OAEP {
            public SHA512() {
                super(SHA512.EVP_MD, SHA512.SIZE_BYTES);
            }
        }

        public OAEP(long defaultMd, int defaultMdSizeBytes) {
            super(4);
            this.mgf1Md = defaultMd;
            this.oaepMd = defaultMd;
            this.oaepMdSizeBytes = defaultMdSizeBytes;
        }

        protected AlgorithmParameters engineGetParameters() {
            if (!isInitialized()) {
                return null;
            }
            try {
                PSource pSrc;
                AlgorithmParameters params = AlgorithmParameters.getInstance("OAEP");
                if (this.label == null) {
                    pSrc = PSpecified.DEFAULT;
                } else {
                    pSrc = new PSpecified(this.label);
                }
                params.init(new OAEPParameterSpec(EvpMdRef.getJcaDigestAlgorithmStandardNameFromEVP_MD(this.oaepMd), "MGF1", new MGF1ParameterSpec(EvpMdRef.getJcaDigestAlgorithmStandardNameFromEVP_MD(this.mgf1Md)), pSrc));
                return params;
            } catch (NoSuchAlgorithmException e) {
                throw ((Error) new AssertionError("OAEP not supported").initCause(e));
            } catch (InvalidParameterSpecException e2) {
                throw new RuntimeException("No providers of AlgorithmParameters.OAEP available");
            }
        }

        protected void engineSetPadding(String padding) throws NoSuchPaddingException {
            if (padding.toUpperCase(Locale.US).equals("OAEPPADDING")) {
                this.padding = 4;
                return;
            }
            throw new NoSuchPaddingException("Only OAEP padding is supported");
        }

        protected void engineInit(int opmode, Key key, AlgorithmParameterSpec spec, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
            if (spec == null || (spec instanceof OAEPParameterSpec)) {
                engineInitInternal(opmode, key, spec);
                return;
            }
            throw new InvalidAlgorithmParameterException("Only OAEPParameterSpec accepted in OAEP mode");
        }

        protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
            OAEPParameterSpec spec = null;
            if (params != null) {
                try {
                    spec = (OAEPParameterSpec) params.getParameterSpec(OAEPParameterSpec.class);
                } catch (InvalidParameterSpecException e) {
                    throw new InvalidAlgorithmParameterException("Only OAEP parameters are supported", e);
                }
            }
            engineInitInternal(opmode, key, spec);
        }

        void engineInitInternal(int opmode, Key key, AlgorithmParameterSpec spec) throws InvalidKeyException, InvalidAlgorithmParameterException {
            if (opmode == 1 || opmode == 3) {
                if (!(key instanceof PublicKey)) {
                    throw new InvalidKeyException("Only public keys may be used to encrypt");
                }
            } else if ((opmode == 2 || opmode == 4) && !(key instanceof PrivateKey)) {
                throw new InvalidKeyException("Only private keys may be used to decrypt");
            }
            super.engineInitInternal(opmode, key, spec);
        }

        void doCryptoInit(AlgorithmParameterSpec spec) throws InvalidAlgorithmParameterException, InvalidKeyException {
            long EVP_PKEY_encrypt_init;
            if (this.encrypting) {
                EVP_PKEY_encrypt_init = NativeCrypto.EVP_PKEY_encrypt_init(this.key.getNativeRef());
            } else {
                EVP_PKEY_encrypt_init = NativeCrypto.EVP_PKEY_decrypt_init(this.key.getNativeRef());
            }
            this.pkeyCtx = new EVP_PKEY_CTX(EVP_PKEY_encrypt_init);
            if (spec instanceof OAEPParameterSpec) {
                readOAEPParameters((OAEPParameterSpec) spec);
            }
            NativeCrypto.EVP_PKEY_CTX_set_rsa_padding(this.pkeyCtx.context, 4);
            NativeCrypto.EVP_PKEY_CTX_set_rsa_oaep_md(this.pkeyCtx.context, this.oaepMd);
            NativeCrypto.EVP_PKEY_CTX_set_rsa_mgf1_md(this.pkeyCtx.context, this.mgf1Md);
            if (this.label != null && this.label.length > 0) {
                NativeCrypto.EVP_PKEY_CTX_set_rsa_oaep_label(this.pkeyCtx.context, this.label);
            }
        }

        int paddedBlockSizeBytes() {
            return keySizeBytes() - ((this.oaepMdSizeBytes * 2) + 2);
        }

        private void readOAEPParameters(OAEPParameterSpec spec) throws InvalidAlgorithmParameterException {
            String mgfAlgUpper = spec.getMGFAlgorithm().toUpperCase(Locale.US);
            AlgorithmParameterSpec mgfSpec = spec.getMGFParameters();
            if (("MGF1".equals(mgfAlgUpper) || "1.2.840.113549.1.1.8".equals(mgfAlgUpper)) && (mgfSpec instanceof MGF1ParameterSpec)) {
                MGF1ParameterSpec mgf1spec = (MGF1ParameterSpec) mgfSpec;
                String oaepAlgUpper = spec.getDigestAlgorithm().toUpperCase(Locale.US);
                try {
                    this.oaepMd = EvpMdRef.getEVP_MDByJcaDigestAlgorithmStandardName(oaepAlgUpper);
                    this.oaepMdSizeBytes = EvpMdRef.getDigestSizeBytesByJcaDigestAlgorithmStandardName(oaepAlgUpper);
                    this.mgf1Md = EvpMdRef.getEVP_MDByJcaDigestAlgorithmStandardName(mgf1spec.getDigestAlgorithm());
                    PSource pSource = spec.getPSource();
                    if ("PSpecified".equals(pSource.getAlgorithm()) && (pSource instanceof PSpecified)) {
                        this.label = ((PSpecified) pSource).getValue();
                        return;
                    }
                    throw new InvalidAlgorithmParameterException("Only PSpecified accepted for PSource");
                } catch (NoSuchAlgorithmException e) {
                    throw new InvalidAlgorithmParameterException(e);
                }
            }
            throw new InvalidAlgorithmParameterException("Only MGF1 supported as mask generation function");
        }

        int doCryptoOperation(byte[] tmpBuf, byte[] output) throws BadPaddingException, IllegalBlockSizeException {
            if (this.encrypting) {
                return NativeCrypto.EVP_PKEY_encrypt(this.pkeyCtx, output, 0, tmpBuf, 0, tmpBuf.length);
            }
            return NativeCrypto.EVP_PKEY_decrypt(this.pkeyCtx, output, 0, tmpBuf, 0, tmpBuf.length);
        }
    }

    public static final class PKCS1 extends DirectRSA {
        public PKCS1() {
            super(1);
        }
    }

    public static final class Raw extends DirectRSA {
        public Raw() {
            super(3);
        }
    }

    abstract int doCryptoOperation(byte[] bArr, byte[] bArr2) throws BadPaddingException, IllegalBlockSizeException;

    OpenSSLCipherRSA(int padding) {
        this.padding = padding;
    }

    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        String modeUpper = mode.toUpperCase(Locale.ROOT);
        if (!"NONE".equals(modeUpper) && !"ECB".equals(modeUpper)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mode not supported: ");
            stringBuilder.append(mode);
            throw new NoSuchAlgorithmException(stringBuilder.toString());
        }
    }

    protected void engineSetPadding(String padding) throws NoSuchPaddingException {
        String paddingUpper = padding.toUpperCase(Locale.ROOT);
        if ("PKCS1PADDING".equals(paddingUpper)) {
            this.padding = 1;
        } else if ("NOPADDING".equals(paddingUpper)) {
            this.padding = 3;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("padding not supported: ");
            stringBuilder.append(padding);
            throw new NoSuchPaddingException(stringBuilder.toString());
        }
    }

    protected int engineGetBlockSize() {
        if (this.encrypting) {
            return paddedBlockSizeBytes();
        }
        return keySizeBytes();
    }

    protected int engineGetOutputSize(int inputLen) {
        if (this.encrypting) {
            return keySizeBytes();
        }
        return paddedBlockSizeBytes();
    }

    int paddedBlockSizeBytes() {
        int paddedBlockSizeBytes = keySizeBytes();
        if (this.padding == 1) {
            return (paddedBlockSizeBytes - 1) - 10;
        }
        return paddedBlockSizeBytes;
    }

    int keySizeBytes() {
        if (isInitialized()) {
            return NativeCrypto.RSA_size(this.key.getNativeRef());
        }
        throw new IllegalStateException("cipher is not initialized");
    }

    boolean isInitialized() {
        return this.key != null;
    }

    protected byte[] engineGetIV() {
        return null;
    }

    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    void doCryptoInit(AlgorithmParameterSpec spec) throws InvalidAlgorithmParameterException, InvalidKeyException {
    }

    void engineInitInternal(int opmode, Key key, AlgorithmParameterSpec spec) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (opmode == 1 || opmode == 3) {
            this.encrypting = true;
        } else if (opmode == 2 || opmode == 4) {
            this.encrypting = false;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported opmode ");
            stringBuilder.append(opmode);
            throw new InvalidParameterException(stringBuilder.toString());
        }
        if (key instanceof OpenSSLRSAPrivateKey) {
            OpenSSLRSAPrivateKey rsaPrivateKey = (OpenSSLRSAPrivateKey) key;
            this.usingPrivateKey = true;
            this.key = rsaPrivateKey.getOpenSSLKey();
        } else if (key instanceof RSAPrivateCrtKey) {
            RSAPrivateCrtKey rsaPrivateKey2 = (RSAPrivateCrtKey) key;
            this.usingPrivateKey = true;
            this.key = OpenSSLRSAPrivateCrtKey.getInstance(rsaPrivateKey2);
        } else if (key instanceof RSAPrivateKey) {
            RSAPrivateKey rsaPrivateKey3 = (RSAPrivateKey) key;
            this.usingPrivateKey = true;
            this.key = OpenSSLRSAPrivateKey.getInstance(rsaPrivateKey3);
        } else if (key instanceof OpenSSLRSAPublicKey) {
            OpenSSLRSAPublicKey rsaPublicKey = (OpenSSLRSAPublicKey) key;
            this.usingPrivateKey = false;
            this.key = rsaPublicKey.getOpenSSLKey();
        } else if (key instanceof RSAPublicKey) {
            RSAPublicKey rsaPublicKey2 = (RSAPublicKey) key;
            this.usingPrivateKey = false;
            this.key = OpenSSLRSAPublicKey.getInstance(rsaPublicKey2);
        } else if (key == null) {
            throw new InvalidKeyException("RSA private or public key is null");
        } else {
            throw new InvalidKeyException("Need RSA private or public key");
        }
        this.buffer = new byte[NativeCrypto.RSA_size(this.key.getNativeRef())];
        this.bufferOffset = 0;
        this.inputTooLarge = false;
        doCryptoInit(spec);
    }

    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        if (key instanceof OpenSSLRSAPrivateKey) {
            return ((OpenSSLRSAPrivateKey) key).getModulus().bitLength();
        }
        if (key instanceof RSAPrivateCrtKey) {
            return ((RSAPrivateCrtKey) key).getModulus().bitLength();
        }
        if (key instanceof RSAPrivateKey) {
            return ((RSAPrivateKey) key).getModulus().bitLength();
        }
        if (key instanceof OpenSSLRSAPublicKey) {
            return ((OpenSSLRSAPublicKey) key).getModulus().bitLength();
        }
        if (key instanceof RSAPublicKey) {
            return ((RSAPublicKey) key).getModulus().bitLength();
        }
        if (key == null) {
            throw new InvalidKeyException("RSA private or public key is null");
        }
        throw new InvalidKeyException("Need RSA private or public key");
    }

    protected void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        try {
            engineInitInternal(opmode, key, null);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException("Algorithm parameters rejected when none supplied", e);
        }
    }

    protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params == null) {
            engineInitInternal(opmode, key, params);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unknown param type: ");
        stringBuilder.append(params.getClass().getName());
        throw new InvalidAlgorithmParameterException(stringBuilder.toString());
    }

    protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params == null) {
            engineInitInternal(opmode, key, null);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unknown param type: ");
        stringBuilder.append(params.getClass().getName());
        throw new InvalidAlgorithmParameterException(stringBuilder.toString());
    }

    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        if (this.bufferOffset + inputLen > this.buffer.length) {
            this.inputTooLarge = true;
            return EmptyArray.BYTE;
        }
        System.arraycopy(input, inputOffset, this.buffer, this.bufferOffset, inputLen);
        this.bufferOffset += inputLen;
        return EmptyArray.BYTE;
    }

    protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        engineUpdate(input, inputOffset, inputLen);
        return 0;
    }

    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) throws IllegalBlockSizeException, BadPaddingException {
        if (input != null) {
            engineUpdate(input, inputOffset, inputLen);
        }
        if (this.inputTooLarge) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("input must be under ");
            stringBuilder.append(this.buffer.length);
            stringBuilder.append(" bytes");
            throw new IllegalBlockSizeException(stringBuilder.toString());
        }
        byte[] tmpBuf;
        if (this.bufferOffset == this.buffer.length) {
            tmpBuf = this.buffer;
        } else if (this.padding == 3) {
            tmpBuf = new byte[this.buffer.length];
            System.arraycopy(this.buffer, 0, tmpBuf, this.buffer.length - this.bufferOffset, this.bufferOffset);
        } else {
            tmpBuf = Arrays.copyOf(this.buffer, this.bufferOffset);
        }
        byte[] output = new byte[this.buffer.length];
        int resultSize = doCryptoOperation(tmpBuf, output);
        if (!(this.encrypting || resultSize == output.length)) {
            output = Arrays.copyOf(output, resultSize);
        }
        this.bufferOffset = 0;
        return output;
    }

    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        byte[] b = engineDoFinal(input, inputOffset, inputLen);
        int lastOffset = b.length + outputOffset;
        if (lastOffset <= output.length) {
            System.arraycopy(b, 0, output, outputOffset, b.length);
            return b.length;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("output buffer is too small ");
        stringBuilder.append(output.length);
        stringBuilder.append(" < ");
        stringBuilder.append(lastOffset);
        throw new ShortBufferException(stringBuilder.toString());
    }

    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        try {
            byte[] encoded = key.getEncoded();
            return engineDoFinal(encoded, 0, encoded.length);
        } catch (BadPaddingException e) {
            IllegalBlockSizeException newE = new IllegalBlockSizeException();
            newE.initCause(e);
            throw newE;
        }
    }

    protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType) throws InvalidKeyException, NoSuchAlgorithmException {
        try {
            byte[] encoded = engineDoFinal(wrappedKey, null, wrappedKey.length);
            if (wrappedKeyType == 1) {
                return KeyFactory.getInstance(wrappedKeyAlgorithm).generatePublic(new X509EncodedKeySpec(encoded));
            }
            if (wrappedKeyType == 2) {
                return KeyFactory.getInstance(wrappedKeyAlgorithm).generatePrivate(new PKCS8EncodedKeySpec(encoded));
            }
            if (wrappedKeyType == 3) {
                return new SecretKeySpec(encoded, wrappedKeyAlgorithm);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("wrappedKeyType == ");
            stringBuilder.append(wrappedKeyType);
            throw new UnsupportedOperationException(stringBuilder.toString());
        } catch (IllegalBlockSizeException e) {
            throw new InvalidKeyException(e);
        } catch (BadPaddingException e2) {
            throw new InvalidKeyException(e2);
        } catch (InvalidKeySpecException e3) {
            throw new InvalidKeyException(e3);
        }
    }
}
