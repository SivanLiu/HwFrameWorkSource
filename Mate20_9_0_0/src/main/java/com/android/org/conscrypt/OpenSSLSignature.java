package com.android.org.conscrypt;

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

public class OpenSSLSignature extends SignatureSpi {
    private EVP_MD_CTX ctx;
    private final EngineType engineType;
    private final long evpMdRef;
    private long evpPkeyCtx;
    private OpenSSLKey key;
    private boolean signing;
    private final byte[] singleByte;

    private enum EngineType {
        RSA,
        EC
    }

    static abstract class RSAPKCS1Padding extends OpenSSLSignature {
        RSAPKCS1Padding(long evpMdRef) {
            super(evpMdRef, EngineType.RSA);
        }

        protected final void configureEVP_PKEY_CTX(long ctx) throws InvalidAlgorithmParameterException {
            NativeCrypto.EVP_PKEY_CTX_set_rsa_padding(ctx, 1);
        }
    }

    static abstract class RSAPSSPadding extends OpenSSLSignature {
        private static final int TRAILER_FIELD_BC_ID = 1;
        private final String contentDigestAlgorithm;
        private String mgf1DigestAlgorithm;
        private long mgf1EvpMdRef;
        private int saltSizeBytes;

        RSAPSSPadding(long contentDigestEvpMdRef, String contentDigestAlgorithm, int saltSizeBytes) {
            super(contentDigestEvpMdRef, EngineType.RSA);
            this.contentDigestAlgorithm = contentDigestAlgorithm;
            this.mgf1DigestAlgorithm = contentDigestAlgorithm;
            this.mgf1EvpMdRef = contentDigestEvpMdRef;
            this.saltSizeBytes = saltSizeBytes;
        }

        protected final void configureEVP_PKEY_CTX(long ctx) throws InvalidAlgorithmParameterException {
            NativeCrypto.EVP_PKEY_CTX_set_rsa_padding(ctx, 6);
            NativeCrypto.EVP_PKEY_CTX_set_rsa_mgf1_md(ctx, this.mgf1EvpMdRef);
            NativeCrypto.EVP_PKEY_CTX_set_rsa_pss_saltlen(ctx, this.saltSizeBytes);
        }

        protected final void engineSetParameter(AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
            if (params instanceof PSSParameterSpec) {
                PSSParameterSpec spec = (PSSParameterSpec) params;
                String specContentDigest = EvpMdRef.getJcaDigestAlgorithmStandardName(spec.getDigestAlgorithm());
                if (specContentDigest == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported content digest algorithm: ");
                    stringBuilder.append(spec.getDigestAlgorithm());
                    throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                } else if (this.contentDigestAlgorithm.equalsIgnoreCase(specContentDigest)) {
                    String specMgfAlgorithm = spec.getMGFAlgorithm();
                    if ("MGF1".equalsIgnoreCase(specMgfAlgorithm) || "1.2.840.113549.1.1.8".equals(specMgfAlgorithm)) {
                        AlgorithmParameterSpec mgfSpec = spec.getMGFParameters();
                        if (mgfSpec instanceof MGF1ParameterSpec) {
                            MGF1ParameterSpec specMgf1Spec = (MGF1ParameterSpec) spec.getMGFParameters();
                            String specMgf1Digest = EvpMdRef.getJcaDigestAlgorithmStandardName(specMgf1Spec.getDigestAlgorithm());
                            if (specMgf1Digest != null) {
                                try {
                                    long specMgf1EvpMdRef = EvpMdRef.getEVP_MDByJcaDigestAlgorithmStandardName(specMgf1Digest);
                                    int specSaltSizeBytes = spec.getSaltLength();
                                    if (specSaltSizeBytes >= 0) {
                                        int specTrailer = spec.getTrailerField();
                                        if (specTrailer == 1) {
                                            this.mgf1DigestAlgorithm = specMgf1Digest;
                                            this.mgf1EvpMdRef = specMgf1EvpMdRef;
                                            this.saltSizeBytes = specSaltSizeBytes;
                                            long ctx = getEVP_PKEY_CTX();
                                            if (ctx != 0) {
                                                configureEVP_PKEY_CTX(ctx);
                                                return;
                                            }
                                            return;
                                        }
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Unsupported trailer field: ");
                                        stringBuilder2.append(specTrailer);
                                        stringBuilder2.append(". Only ");
                                        stringBuilder2.append(1);
                                        stringBuilder2.append(" supported");
                                        throw new InvalidAlgorithmParameterException(stringBuilder2.toString());
                                    }
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Salt length must be non-negative: ");
                                    stringBuilder3.append(specSaltSizeBytes);
                                    throw new InvalidAlgorithmParameterException(stringBuilder3.toString());
                                } catch (NoSuchAlgorithmException e) {
                                    StringBuilder stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("Failed to obtain EVP_MD for ");
                                    stringBuilder4.append(specMgf1Digest);
                                    throw new ProviderException(stringBuilder4.toString(), e);
                                }
                            }
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("Unsupported MGF1 digest algorithm: ");
                            stringBuilder5.append(specMgf1Spec.getDigestAlgorithm());
                            throw new InvalidAlgorithmParameterException(stringBuilder5.toString());
                        }
                        StringBuilder stringBuilder6 = new StringBuilder();
                        stringBuilder6.append("Unsupported MGF parameters: ");
                        stringBuilder6.append(mgfSpec);
                        stringBuilder6.append(". Only ");
                        stringBuilder6.append(MGF1ParameterSpec.class.getName());
                        stringBuilder6.append(" supported");
                        throw new InvalidAlgorithmParameterException(stringBuilder6.toString());
                    }
                    StringBuilder stringBuilder7 = new StringBuilder();
                    stringBuilder7.append("Unsupported MGF algorithm: ");
                    stringBuilder7.append(specMgfAlgorithm);
                    stringBuilder7.append(". Only ");
                    stringBuilder7.append("MGF1");
                    stringBuilder7.append(" supported");
                    throw new InvalidAlgorithmParameterException(stringBuilder7.toString());
                } else {
                    throw new InvalidAlgorithmParameterException("Changing content digest algorithm not supported");
                }
            }
            StringBuilder stringBuilder8 = new StringBuilder();
            stringBuilder8.append("Unsupported parameter: ");
            stringBuilder8.append(params);
            stringBuilder8.append(". Only ");
            stringBuilder8.append(PSSParameterSpec.class.getName());
            stringBuilder8.append(" supported");
            throw new InvalidAlgorithmParameterException(stringBuilder8.toString());
        }

        protected final AlgorithmParameters engineGetParameters() {
            try {
                AlgorithmParameters result = AlgorithmParameters.getInstance("PSS");
                result.init(new PSSParameterSpec(this.contentDigestAlgorithm, "MGF1", new MGF1ParameterSpec(this.mgf1DigestAlgorithm), this.saltSizeBytes, 1));
                return result;
            } catch (NoSuchAlgorithmException e) {
                throw new ProviderException("Failed to create PSS AlgorithmParameters", e);
            } catch (InvalidParameterSpecException e2) {
                throw new ProviderException("Failed to create PSS AlgorithmParameters", e2);
            }
        }
    }

    public static final class SHA1ECDSA extends OpenSSLSignature {
        public SHA1ECDSA() {
            super(SHA1.EVP_MD, EngineType.EC);
        }
    }

    public static final class SHA224ECDSA extends OpenSSLSignature {
        public SHA224ECDSA() {
            super(SHA224.EVP_MD, EngineType.EC);
        }
    }

    public static final class SHA256ECDSA extends OpenSSLSignature {
        public SHA256ECDSA() {
            super(SHA256.EVP_MD, EngineType.EC);
        }
    }

    public static final class SHA384ECDSA extends OpenSSLSignature {
        public SHA384ECDSA() {
            super(SHA384.EVP_MD, EngineType.EC);
        }
    }

    public static final class SHA512ECDSA extends OpenSSLSignature {
        public SHA512ECDSA() {
            super(SHA512.EVP_MD, EngineType.EC);
        }
    }

    public static final class MD5RSA extends RSAPKCS1Padding {
        public MD5RSA() {
            super(MD5.EVP_MD);
        }
    }

    public static final class SHA1RSA extends RSAPKCS1Padding {
        public SHA1RSA() {
            super(SHA1.EVP_MD);
        }
    }

    public static final class SHA1RSAPSS extends RSAPSSPadding {
        public SHA1RSAPSS() {
            super(SHA1.EVP_MD, "SHA-1", SHA1.SIZE_BYTES);
        }
    }

    public static final class SHA224RSA extends RSAPKCS1Padding {
        public SHA224RSA() {
            super(SHA224.EVP_MD);
        }
    }

    public static final class SHA224RSAPSS extends RSAPSSPadding {
        public SHA224RSAPSS() {
            super(SHA224.EVP_MD, "SHA-224", SHA224.SIZE_BYTES);
        }
    }

    public static final class SHA256RSA extends RSAPKCS1Padding {
        public SHA256RSA() {
            super(SHA256.EVP_MD);
        }
    }

    public static final class SHA256RSAPSS extends RSAPSSPadding {
        public SHA256RSAPSS() {
            super(SHA256.EVP_MD, "SHA-256", SHA256.SIZE_BYTES);
        }
    }

    public static final class SHA384RSA extends RSAPKCS1Padding {
        public SHA384RSA() {
            super(SHA384.EVP_MD);
        }
    }

    public static final class SHA384RSAPSS extends RSAPSSPadding {
        public SHA384RSAPSS() {
            super(SHA384.EVP_MD, "SHA-384", SHA384.SIZE_BYTES);
        }
    }

    public static final class SHA512RSA extends RSAPKCS1Padding {
        public SHA512RSA() {
            super(SHA512.EVP_MD);
        }
    }

    public static final class SHA512RSAPSS extends RSAPSSPadding {
        public SHA512RSAPSS() {
            super(SHA512.EVP_MD, "SHA-512", SHA512.SIZE_BYTES);
        }
    }

    private OpenSSLSignature(long evpMdRef, EngineType engineType) {
        this.singleByte = new byte[1];
        this.engineType = engineType;
        this.evpMdRef = evpMdRef;
    }

    private void resetContext() throws InvalidAlgorithmParameterException {
        EVP_MD_CTX ctxLocal = new EVP_MD_CTX(NativeCrypto.EVP_MD_CTX_create());
        if (this.signing) {
            this.evpPkeyCtx = NativeCrypto.EVP_DigestSignInit(ctxLocal, this.evpMdRef, this.key.getNativeRef());
        } else {
            this.evpPkeyCtx = NativeCrypto.EVP_DigestVerifyInit(ctxLocal, this.evpMdRef, this.key.getNativeRef());
        }
        configureEVP_PKEY_CTX(this.evpPkeyCtx);
        this.ctx = ctxLocal;
    }

    protected void configureEVP_PKEY_CTX(long ctx) throws InvalidAlgorithmParameterException {
    }

    protected void engineUpdate(byte input) {
        this.singleByte[0] = input;
        engineUpdate(this.singleByte, 0, 1);
    }

    protected void engineUpdate(byte[] input, int offset, int len) {
        EVP_MD_CTX ctxLocal = this.ctx;
        if (this.signing) {
            NativeCrypto.EVP_DigestSignUpdate(ctxLocal, input, offset, len);
        } else {
            NativeCrypto.EVP_DigestVerifyUpdate(ctxLocal, input, offset, len);
        }
    }

    protected void engineUpdate(ByteBuffer input) {
        if (!input.hasRemaining()) {
            return;
        }
        if (input.isDirect()) {
            long baseAddress = NativeCrypto.getDirectBufferAddress(input);
            if (baseAddress == 0) {
                super.engineUpdate(input);
                return;
            }
            int position = input.position();
            if (position >= 0) {
                long ptr = ((long) position) + baseAddress;
                int len = input.remaining();
                if (len >= 0) {
                    EVP_MD_CTX ctxLocal = this.ctx;
                    if (this.signing) {
                        NativeCrypto.EVP_DigestSignUpdateDirect(ctxLocal, ptr, len);
                    } else {
                        NativeCrypto.EVP_DigestVerifyUpdateDirect(ctxLocal, ptr, len);
                    }
                    input.position(position + len);
                    return;
                }
                throw new RuntimeException("Negative remaining amount");
            }
            throw new RuntimeException("Negative position");
        }
        super.engineUpdate(input);
    }

    @Deprecated
    protected Object engineGetParameter(String param) throws InvalidParameterException {
        return null;
    }

    private void checkEngineType(OpenSSLKey pkey) throws InvalidKeyException {
        int pkeyType = NativeCrypto.EVP_PKEY_type(pkey.getNativeRef());
        StringBuilder stringBuilder;
        switch (this.engineType) {
            case RSA:
                if (pkeyType != 6) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Signature initialized as ");
                    stringBuilder.append(this.engineType);
                    stringBuilder.append(" (not RSA)");
                    throw new InvalidKeyException(stringBuilder.toString());
                }
                return;
            case EC:
                if (pkeyType != 408) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Signature initialized as ");
                    stringBuilder.append(this.engineType);
                    stringBuilder.append(" (not EC)");
                    throw new InvalidKeyException(stringBuilder.toString());
                }
                return;
            default:
                stringBuilder = new StringBuilder();
                stringBuilder.append("Key must be of type ");
                stringBuilder.append(this.engineType);
                throw new InvalidKeyException(stringBuilder.toString());
        }
    }

    private void initInternal(OpenSSLKey newKey, boolean signing) throws InvalidKeyException {
        checkEngineType(newKey);
        this.key = newKey;
        this.signing = signing;
        try {
            resetContext();
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e);
        }
    }

    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        initInternal(OpenSSLKey.fromPrivateKey(privateKey), true);
    }

    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        initInternal(OpenSSLKey.fromPublicKey(publicKey), false);
    }

    @Deprecated
    protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
    }

    protected byte[] engineSign() throws SignatureException {
        try {
            byte[] EVP_DigestSignFinal = NativeCrypto.EVP_DigestSignFinal(this.ctx);
            try {
                resetContext();
                return EVP_DigestSignFinal;
            } catch (InvalidAlgorithmParameterException e) {
                throw new AssertionError("Reset of context failed after it was successful once");
            }
        } catch (Exception ex) {
            throw new SignatureException(ex);
        } catch (Throwable th) {
            try {
                resetContext();
            } catch (InvalidAlgorithmParameterException e2) {
                throw new AssertionError("Reset of context failed after it was successful once");
            }
        }
    }

    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        try {
            boolean EVP_DigestVerifyFinal = NativeCrypto.EVP_DigestVerifyFinal(this.ctx, sigBytes, 0, sigBytes.length);
            try {
                resetContext();
                return EVP_DigestVerifyFinal;
            } catch (InvalidAlgorithmParameterException e) {
                throw new AssertionError("Reset of context failed after it was successful once");
            }
        } catch (Exception ex) {
            throw new SignatureException(ex);
        } catch (Throwable th) {
            try {
                resetContext();
            } catch (InvalidAlgorithmParameterException e2) {
                throw new AssertionError("Reset of context failed after it was successful once");
            }
        }
    }

    protected final long getEVP_PKEY_CTX() {
        return this.evpPkeyCtx;
    }
}
