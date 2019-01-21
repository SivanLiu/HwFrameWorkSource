package com.android.org.conscrypt;

import com.android.org.conscrypt.ct.CTConstants;
import java.lang.reflect.InvocationTargetException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.BadPaddingException;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public abstract class OpenSSLCipher extends CipherSpi {
    private int blockSize;
    byte[] encodedKey;
    private boolean encrypting;
    byte[] iv;
    Mode mode = Mode.ECB;
    private Padding padding = Padding.PKCS5PADDING;

    /* renamed from: com.android.org.conscrypt.OpenSSLCipher$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$org$conscrypt$OpenSSLCipher$Mode = new int[Mode.values().length];

        static {
            $SwitchMap$org$conscrypt$OpenSSLCipher$Padding = new int[Padding.values().length];
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Padding[Padding.NOPADDING.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Padding[Padding.PKCS5PADDING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Mode[Mode.CBC.ordinal()] = 1;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Mode[Mode.CTR.ordinal()] = 2;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Mode[Mode.ECB.ordinal()] = 3;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    enum Mode {
        NONE,
        CBC,
        CTR,
        ECB,
        GCM,
        POLY1305
    }

    enum Padding {
        NOPADDING,
        PKCS5PADDING,
        PKCS7PADDING;

        public static Padding getNormalized(String value) {
            Padding p = valueOf(value);
            if (p == PKCS7PADDING) {
                return PKCS5PADDING;
            }
            return p;
        }
    }

    public static abstract class EVP_AEAD extends OpenSSLCipher {
        private static final int DEFAULT_TAG_SIZE_BITS = 128;
        private static int lastGlobalMessageSize = 32;
        private byte[] aad;
        byte[] buf;
        int bufCount;
        long evpAead;
        private boolean mustInitialize;
        private byte[] previousIv;
        private byte[] previousKey;
        int tagLengthInBytes;

        public static abstract class AES extends EVP_AEAD {
            private static final int AES_BLOCK_SIZE = 16;

            public static class GCM extends AES {

                public static class AES_128 extends GCM {
                    void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
                        if (keyLength != AES.AES_BLOCK_SIZE) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unsupported key size: ");
                            stringBuilder.append(keyLength);
                            stringBuilder.append(" bytes (must be 16)");
                            throw new InvalidKeyException(stringBuilder.toString());
                        }
                    }
                }

                public static class AES_256 extends GCM {
                    void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
                        if (keyLength != 32) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unsupported key size: ");
                            stringBuilder.append(keyLength);
                            stringBuilder.append(" bytes (must be 32)");
                            throw new InvalidKeyException(stringBuilder.toString());
                        }
                    }
                }

                public GCM() {
                    super(Mode.GCM);
                }

                void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
                    if (mode != Mode.GCM) {
                        throw new NoSuchAlgorithmException("Mode must be GCM");
                    }
                }

                protected AlgorithmParameters engineGetParameters() {
                    if (this.iv == null) {
                        return null;
                    }
                    AlgorithmParameterSpec spec = Platform.toGCMParameterSpec(this.tagLengthInBytes * 8, this.iv);
                    if (spec == null) {
                        return super.engineGetParameters();
                    }
                    try {
                        AlgorithmParameters params = AlgorithmParameters.getInstance("GCM");
                        params.init(spec);
                        return params;
                    } catch (NoSuchAlgorithmException e) {
                        throw ((Error) new AssertionError("GCM not supported").initCause(e));
                    } catch (InvalidParameterSpecException e2) {
                        return null;
                    }
                }

                protected AlgorithmParameterSpec getParameterSpec(AlgorithmParameters params) throws InvalidAlgorithmParameterException {
                    if (params == null) {
                        return null;
                    }
                    AlgorithmParameterSpec spec = Platform.fromGCMParameters(params);
                    if (spec != null) {
                        return spec;
                    }
                    return super.getParameterSpec(params);
                }

                long getEVP_AEAD(int keyLength) throws InvalidKeyException {
                    if (keyLength == AES.AES_BLOCK_SIZE) {
                        return NativeCrypto.EVP_aead_aes_128_gcm();
                    }
                    if (keyLength == 32) {
                        return NativeCrypto.EVP_aead_aes_256_gcm();
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected key length: ");
                    stringBuilder.append(keyLength);
                    throw new RuntimeException(stringBuilder.toString());
                }
            }

            AES(Mode mode) {
                super(mode);
            }

            void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
                if (keyLength != AES_BLOCK_SIZE && keyLength != 32) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported key size: ");
                    stringBuilder.append(keyLength);
                    stringBuilder.append(" bytes (must be 16 or 32)");
                    throw new InvalidKeyException(stringBuilder.toString());
                }
            }

            String getBaseCipherName() {
                return "AES";
            }

            int getCipherBlockSize() {
                return AES_BLOCK_SIZE;
            }
        }

        public static class ChaCha20 extends EVP_AEAD {
            public ChaCha20() {
                super(Mode.POLY1305);
            }

            void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
                if (keyLength != 32) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported key size: ");
                    stringBuilder.append(keyLength);
                    stringBuilder.append(" bytes (must be 32)");
                    throw new InvalidKeyException(stringBuilder.toString());
                }
            }

            String getBaseCipherName() {
                return "ChaCha20";
            }

            int getCipherBlockSize() {
                return 0;
            }

            void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
                if (mode != Mode.POLY1305) {
                    throw new NoSuchAlgorithmException("Mode must be Poly1305");
                }
            }

            long getEVP_AEAD(int keyLength) throws InvalidKeyException {
                if (keyLength == 32) {
                    return NativeCrypto.EVP_aead_chacha20_poly1305();
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected key length: ");
                stringBuilder.append(keyLength);
                throw new RuntimeException(stringBuilder.toString());
            }
        }

        abstract long getEVP_AEAD(int i) throws InvalidKeyException;

        public EVP_AEAD(Mode mode) {
            super(mode, Padding.NOPADDING);
        }

        private void checkInitialization() {
            if (this.mustInitialize) {
                throw new IllegalStateException("Cannot re-use same key and IV for multiple encryptions");
            }
        }

        private boolean arraysAreEqual(byte[] a, byte[] b) {
            boolean z = false;
            if (a.length != b.length) {
                return false;
            }
            int diff = 0;
            for (int i = 0; i < a.length; i++) {
                diff |= a[i] ^ b[i];
            }
            if (diff == 0) {
                z = true;
            }
            return z;
        }

        private void expand(int i) {
            if (this.bufCount + i > this.buf.length) {
                byte[] newbuf = new byte[((this.bufCount + i) * 2)];
                System.arraycopy(this.buf, 0, newbuf, 0, this.bufCount);
                this.buf = newbuf;
            }
        }

        private void reset() {
            this.aad = null;
            int lastBufSize = lastGlobalMessageSize;
            if (this.buf == null) {
                this.buf = new byte[lastBufSize];
            } else if (this.bufCount > 0 && this.bufCount != lastBufSize) {
                lastGlobalMessageSize = this.bufCount;
                if (this.buf.length != this.bufCount) {
                    this.buf = new byte[this.bufCount];
                }
            }
            this.bufCount = 0;
        }

        void engineInitInternal(byte[] encodedKey, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
            byte[] iv;
            int tagLenBits;
            if (params == null) {
                iv = null;
                tagLenBits = 128;
            } else {
                GCMParameters gcmParams = Platform.fromGCMParameterSpec(params);
                if (gcmParams != null) {
                    byte[] iv2 = gcmParams.getIV();
                    int tagLenBits2 = gcmParams.getTLen();
                    iv = iv2;
                    tagLenBits = tagLenBits2;
                } else if (params instanceof IvParameterSpec) {
                    tagLenBits = 128;
                    iv = ((IvParameterSpec) params).getIV();
                } else {
                    tagLenBits = 128;
                    iv = null;
                }
            }
            if (tagLenBits % 8 == 0) {
                this.tagLengthInBytes = tagLenBits / 8;
                boolean encrypting = isEncrypting();
                this.evpAead = getEVP_AEAD(encodedKey.length);
                int expectedIvLength = NativeCrypto.EVP_AEAD_nonce_length(this.evpAead);
                StringBuilder stringBuilder;
                if (iv != null || expectedIvLength == 0) {
                    if (expectedIvLength == 0 && iv != null) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("IV not used in ");
                        stringBuilder.append(this.mode);
                        stringBuilder.append(" mode");
                        throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                    } else if (!(iv == null || iv.length == expectedIvLength)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Expected IV length of ");
                        stringBuilder.append(expectedIvLength);
                        stringBuilder.append(" but was ");
                        stringBuilder.append(iv.length);
                        throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                    }
                } else if (encrypting) {
                    iv = new byte[expectedIvLength];
                    if (random != null) {
                        random.nextBytes(iv);
                    } else {
                        NativeCrypto.RAND_bytes(iv);
                    }
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("IV must be specified in ");
                    stringBuilder.append(this.mode);
                    stringBuilder.append(" mode");
                    throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                }
                if (isEncrypting() && iv != null) {
                    if (this.previousKey == null || this.previousIv == null || !arraysAreEqual(this.previousKey, encodedKey) || !arraysAreEqual(this.previousIv, iv)) {
                        this.previousKey = encodedKey;
                        this.previousIv = iv;
                    } else {
                        this.mustInitialize = true;
                        throw new InvalidAlgorithmParameterException("When using AEAD key and IV must not be re-used");
                    }
                }
                this.mustInitialize = false;
                this.iv = iv;
                reset();
                return;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Tag length must be a multiple of 8; was ");
            stringBuilder2.append(this.tagLengthInBytes);
            throw new InvalidAlgorithmParameterException(stringBuilder2.toString());
        }

        int updateInternal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int maximumLen) throws ShortBufferException {
            checkInitialization();
            if (this.buf != null) {
                ArrayUtils.checkOffsetAndCount(input.length, inputOffset, inputLen);
                if (inputLen > 0) {
                    expand(inputLen);
                    System.arraycopy(input, inputOffset, this.buf, this.bufCount, inputLen);
                    this.bufCount += inputLen;
                }
                return 0;
            }
            throw new IllegalStateException("Cipher not initialized");
        }

        private void throwAEADBadTagExceptionIfAvailable(String message, Throwable cause) throws BadPaddingException {
            try {
                BadPaddingException badTagException = null;
                try {
                    badTagException = (BadPaddingException) Class.forName("javax.crypto.AEADBadTagException").getConstructor(new Class[]{String.class}).newInstance(new Object[]{message});
                    badTagException.initCause(cause);
                } catch (IllegalAccessException | InstantiationException e) {
                } catch (InvocationTargetException e2) {
                    throw ((BadPaddingException) new BadPaddingException().initCause(e2.getTargetException()));
                }
                if (badTagException != null) {
                    throw badTagException;
                }
            } catch (Exception e3) {
            }
        }

        int doFinalInternal(byte[] output, int outputOffset, int maximumLen) throws IllegalBlockSizeException, BadPaddingException {
            checkInitialization();
            try {
                int bytesWritten;
                if (isEncrypting()) {
                    bytesWritten = NativeCrypto.EVP_AEAD_CTX_seal(this.evpAead, this.encodedKey, this.tagLengthInBytes, output, outputOffset, this.iv, this.buf, 0, this.bufCount, this.aad);
                } else {
                    bytesWritten = NativeCrypto.EVP_AEAD_CTX_open(this.evpAead, this.encodedKey, this.tagLengthInBytes, output, outputOffset, this.iv, this.buf, 0, this.bufCount, this.aad);
                }
                if (isEncrypting()) {
                    this.mustInitialize = true;
                }
                reset();
                return bytesWritten;
            } catch (BadPaddingException e) {
                throwAEADBadTagExceptionIfAvailable(e.getMessage(), e.getCause());
                throw e;
            }
        }

        void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
            if (padding != Padding.NOPADDING) {
                throw new NoSuchPaddingException("Must be NoPadding for AEAD ciphers");
            }
        }

        int getOutputSizeForUpdate(int inputLen) {
            return 0;
        }

        int getOutputSizeForFinal(int inputLen) {
            return (this.bufCount + inputLen) + (isEncrypting() ? NativeCrypto.EVP_AEAD_max_overhead(this.evpAead) : 0);
        }

        protected void engineUpdateAAD(byte[] input, int inputOffset, int inputLen) {
            checkInitialization();
            if (this.aad == null) {
                this.aad = Arrays.copyOfRange(input, inputOffset, inputOffset + inputLen);
                return;
            }
            byte[] newaad = new byte[(this.aad.length + inputLen)];
            System.arraycopy(this.aad, 0, newaad, 0, this.aad.length);
            System.arraycopy(input, inputOffset, newaad, this.aad.length, inputLen);
            this.aad = newaad;
        }
    }

    public static abstract class EVP_CIPHER extends OpenSSLCipher {
        boolean calledUpdate;
        private final EVP_CIPHER_CTX cipherCtx = new EVP_CIPHER_CTX(NativeCrypto.EVP_CIPHER_CTX_new());
        private int modeBlockSize;

        static abstract class AES_BASE extends EVP_CIPHER {
            private static final int AES_BLOCK_SIZE = 16;

            AES_BASE(Mode mode, Padding padding) {
                super(mode, padding);
            }

            void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
                switch (AnonymousClass1.$SwitchMap$org$conscrypt$OpenSSLCipher$Mode[mode.ordinal()]) {
                    case 1:
                    case 2:
                    case CTConstants.CERTIFICATE_LENGTH_BYTES /*3*/:
                        return;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported mode ");
                        stringBuilder.append(mode.toString());
                        throw new NoSuchAlgorithmException(stringBuilder.toString());
                }
            }

            void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
                switch (padding) {
                    case NOPADDING:
                    case PKCS5PADDING:
                        return;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported padding ");
                        stringBuilder.append(padding.toString());
                        throw new NoSuchPaddingException(stringBuilder.toString());
                }
            }

            String getBaseCipherName() {
                return "AES";
            }

            String getCipherName(int keyLength, Mode mode) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("aes-");
                stringBuilder.append(keyLength * 8);
                stringBuilder.append("-");
                stringBuilder.append(mode.toString().toLowerCase(Locale.US));
                return stringBuilder.toString();
            }

            int getCipherBlockSize() {
                return AES_BLOCK_SIZE;
            }
        }

        public static class ARC4 extends EVP_CIPHER {
            public ARC4() {
                super(Mode.ECB, Padding.NOPADDING);
            }

            String getBaseCipherName() {
                return "ARCFOUR";
            }

            String getCipherName(int keySize, Mode mode) {
                return "rc4";
            }

            void checkSupportedKeySize(int keySize) throws InvalidKeyException {
            }

            void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
                if (mode != Mode.NONE && mode != Mode.ECB) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported mode ");
                    stringBuilder.append(mode.toString());
                    throw new NoSuchAlgorithmException(stringBuilder.toString());
                }
            }

            void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
                if (padding != Padding.NOPADDING) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported padding ");
                    stringBuilder.append(padding.toString());
                    throw new NoSuchPaddingException(stringBuilder.toString());
                }
            }

            int getCipherBlockSize() {
                return 0;
            }

            boolean supportsVariableSizeKey() {
                return true;
            }
        }

        public static class DESEDE extends EVP_CIPHER {
            private static final int DES_BLOCK_SIZE = 8;

            public static class CBC extends DESEDE {

                public static class NoPadding extends CBC {
                    public NoPadding() {
                        super(Padding.NOPADDING);
                    }
                }

                public static class PKCS5Padding extends CBC {
                    public PKCS5Padding() {
                        super(Padding.PKCS5PADDING);
                    }
                }

                public CBC(Padding padding) {
                    super(Mode.CBC, padding);
                }
            }

            public DESEDE(Mode mode, Padding padding) {
                super(mode, padding);
            }

            String getBaseCipherName() {
                return "DESede";
            }

            String getCipherName(int keySize, Mode mode) {
                String baseCipherName;
                if (keySize == 16) {
                    baseCipherName = "des-ede";
                } else {
                    baseCipherName = "des-ede3";
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(baseCipherName);
                stringBuilder.append("-");
                stringBuilder.append(mode.toString().toLowerCase(Locale.US));
                return stringBuilder.toString();
            }

            void checkSupportedKeySize(int keySize) throws InvalidKeyException {
                if (keySize != 16 && keySize != 24) {
                    throw new InvalidKeyException("key size must be 128 or 192 bits");
                }
            }

            void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
                if (mode != Mode.CBC) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported mode ");
                    stringBuilder.append(mode.toString());
                    throw new NoSuchAlgorithmException(stringBuilder.toString());
                }
            }

            void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
                switch (padding) {
                    case NOPADDING:
                    case PKCS5PADDING:
                        return;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported padding ");
                        stringBuilder.append(padding.toString());
                        throw new NoSuchPaddingException(stringBuilder.toString());
                }
            }

            int getCipherBlockSize() {
                return 8;
            }
        }

        public static class AES extends AES_BASE {

            public static class CBC extends AES {

                public static class NoPadding extends CBC {
                    public NoPadding() {
                        super(Padding.NOPADDING);
                    }
                }

                public static class PKCS5Padding extends CBC {
                    public PKCS5Padding() {
                        super(Padding.PKCS5PADDING);
                    }
                }

                public CBC(Padding padding) {
                    super(Mode.CBC, padding);
                }
            }

            public static class CTR extends AES {
                public CTR() {
                    super(Mode.CTR, Padding.NOPADDING);
                }
            }

            public static class ECB extends AES {

                public static class NoPadding extends ECB {
                    public NoPadding() {
                        super(Padding.NOPADDING);
                    }
                }

                public static class PKCS5Padding extends ECB {
                    public PKCS5Padding() {
                        super(Padding.PKCS5PADDING);
                    }
                }

                public ECB(Padding padding) {
                    super(Mode.ECB, padding);
                }
            }

            AES(Mode mode, Padding padding) {
                super(mode, padding);
            }

            void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
                if (keyLength != 16 && keyLength != 24 && keyLength != 32) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported key size: ");
                    stringBuilder.append(keyLength);
                    stringBuilder.append(" bytes");
                    throw new InvalidKeyException(stringBuilder.toString());
                }
            }
        }

        public static class AES_128 extends AES_BASE {

            public static class CBC extends AES_128 {

                public static class NoPadding extends CBC {
                    public NoPadding() {
                        super(Padding.NOPADDING);
                    }
                }

                public static class PKCS5Padding extends CBC {
                    public PKCS5Padding() {
                        super(Padding.PKCS5PADDING);
                    }
                }

                public CBC(Padding padding) {
                    super(Mode.CBC, padding);
                }
            }

            public static class CTR extends AES_128 {
                public CTR() {
                    super(Mode.CTR, Padding.NOPADDING);
                }
            }

            public static class ECB extends AES_128 {

                public static class NoPadding extends ECB {
                    public NoPadding() {
                        super(Padding.NOPADDING);
                    }
                }

                public static class PKCS5Padding extends ECB {
                    public PKCS5Padding() {
                        super(Padding.PKCS5PADDING);
                    }
                }

                public ECB(Padding padding) {
                    super(Mode.ECB, padding);
                }
            }

            AES_128(Mode mode, Padding padding) {
                super(mode, padding);
            }

            void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
                if (keyLength != 16) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported key size: ");
                    stringBuilder.append(keyLength);
                    stringBuilder.append(" bytes");
                    throw new InvalidKeyException(stringBuilder.toString());
                }
            }
        }

        public static class AES_256 extends AES_BASE {

            public static class CBC extends AES_256 {

                public static class NoPadding extends CBC {
                    public NoPadding() {
                        super(Padding.NOPADDING);
                    }
                }

                public static class PKCS5Padding extends CBC {
                    public PKCS5Padding() {
                        super(Padding.PKCS5PADDING);
                    }
                }

                public CBC(Padding padding) {
                    super(Mode.CBC, padding);
                }
            }

            public static class CTR extends AES_256 {
                public CTR() {
                    super(Mode.CTR, Padding.NOPADDING);
                }
            }

            public static class ECB extends AES_256 {

                public static class NoPadding extends ECB {
                    public NoPadding() {
                        super(Padding.NOPADDING);
                    }
                }

                public static class PKCS5Padding extends ECB {
                    public PKCS5Padding() {
                        super(Padding.PKCS5PADDING);
                    }
                }

                public ECB(Padding padding) {
                    super(Mode.ECB, padding);
                }
            }

            AES_256(Mode mode, Padding padding) {
                super(mode, padding);
            }

            void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
                if (keyLength != 32) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported key size: ");
                    stringBuilder.append(keyLength);
                    stringBuilder.append(" bytes");
                    throw new InvalidKeyException(stringBuilder.toString());
                }
            }
        }

        abstract String getCipherName(int i, Mode mode);

        public EVP_CIPHER(Mode mode, Padding padding) {
            super(mode, padding);
        }

        void engineInitInternal(byte[] encodedKey, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
            byte[] iv;
            byte[] bArr = encodedKey;
            AlgorithmParameterSpec algorithmParameterSpec = params;
            SecureRandom secureRandom = random;
            if (algorithmParameterSpec instanceof IvParameterSpec) {
                iv = ((IvParameterSpec) algorithmParameterSpec).getIV();
            } else {
                iv = null;
            }
            long cipherType = NativeCrypto.EVP_get_cipherbyname(getCipherName(bArr.length, this.mode));
            StringBuilder stringBuilder;
            if (cipherType != 0) {
                boolean encrypting = isEncrypting();
                int expectedIvLength = NativeCrypto.EVP_CIPHER_iv_length(cipherType);
                if (iv != null || expectedIvLength == 0) {
                    if (expectedIvLength == 0 && iv != null) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("IV not used in ");
                        stringBuilder.append(this.mode);
                        stringBuilder.append(" mode");
                        throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                    } else if (!(iv == null || iv.length == expectedIvLength)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("expected IV length of ");
                        stringBuilder.append(expectedIvLength);
                        stringBuilder.append(" but was ");
                        stringBuilder.append(iv.length);
                        throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                    }
                } else if (encrypting) {
                    iv = new byte[expectedIvLength];
                    if (secureRandom != null) {
                        secureRandom.nextBytes(iv);
                    } else {
                        NativeCrypto.RAND_bytes(iv);
                    }
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("IV must be specified in ");
                    stringBuilder.append(this.mode);
                    stringBuilder.append(" mode");
                    throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                }
                byte[] iv2 = iv;
                this.iv = iv2;
                if (supportsVariableSizeKey()) {
                    NativeCrypto.EVP_CipherInit_ex(this.cipherCtx, cipherType, null, null, encrypting);
                    NativeCrypto.EVP_CIPHER_CTX_set_key_length(this.cipherCtx, bArr.length);
                    NativeCrypto.EVP_CipherInit_ex(this.cipherCtx, 0, bArr, iv2, isEncrypting());
                } else {
                    int i = expectedIvLength;
                    NativeCrypto.EVP_CipherInit_ex(this.cipherCtx, cipherType, bArr, iv2, encrypting);
                }
                NativeCrypto.EVP_CIPHER_CTX_set_padding(this.cipherCtx, getPadding() == Padding.PKCS5PADDING);
                this.modeBlockSize = NativeCrypto.EVP_CIPHER_CTX_block_size(this.cipherCtx);
                this.calledUpdate = false;
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot find name for key length = ");
            stringBuilder.append(bArr.length * 8);
            stringBuilder.append(" and mode = ");
            stringBuilder.append(this.mode);
            throw new InvalidAlgorithmParameterException(stringBuilder.toString());
        }

        int updateInternal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int maximumLen) throws ShortBufferException {
            int intialOutputOffset = outputOffset;
            int bytesLeft = output.length - outputOffset;
            if (bytesLeft >= maximumLen) {
                outputOffset += NativeCrypto.EVP_CipherUpdate(this.cipherCtx, output, outputOffset, input, inputOffset, inputLen);
                this.calledUpdate = true;
                return outputOffset - intialOutputOffset;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("output buffer too small during update: ");
            stringBuilder.append(bytesLeft);
            stringBuilder.append(" < ");
            stringBuilder.append(maximumLen);
            throw new ShortBufferException(stringBuilder.toString());
        }

        int doFinalInternal(byte[] output, int outputOffset, int maximumLen) throws IllegalBlockSizeException, BadPaddingException, ShortBufferException {
            int initialOutputOffset = outputOffset;
            if (!isEncrypting() && !this.calledUpdate) {
                return 0;
            }
            int writtenBytes;
            int bytesLeft = output.length - outputOffset;
            if (bytesLeft >= maximumLen) {
                writtenBytes = NativeCrypto.EVP_CipherFinal_ex(this.cipherCtx, output, outputOffset);
            } else {
                byte[] lastBlock = new byte[maximumLen];
                int writtenBytes2 = NativeCrypto.EVP_CipherFinal_ex(this.cipherCtx, lastBlock, 0);
                if (writtenBytes2 <= bytesLeft) {
                    if (writtenBytes2 > 0) {
                        System.arraycopy(lastBlock, 0, output, outputOffset, writtenBytes2);
                    }
                    writtenBytes = writtenBytes2;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("buffer is too short: ");
                    stringBuilder.append(writtenBytes2);
                    stringBuilder.append(" > ");
                    stringBuilder.append(bytesLeft);
                    throw new ShortBufferException(stringBuilder.toString());
                }
            }
            outputOffset += writtenBytes;
            reset();
            return outputOffset - initialOutputOffset;
        }

        int getOutputSizeForFinal(int inputLen) {
            if (this.modeBlockSize == 1) {
                return inputLen;
            }
            int buffered = NativeCrypto.get_EVP_CIPHER_CTX_buf_len(this.cipherCtx);
            if (getPadding() == Padding.NOPADDING) {
                return buffered + inputLen;
            }
            int i = 0;
            int totalLen = (inputLen + buffered) + (NativeCrypto.get_EVP_CIPHER_CTX_final_used(this.cipherCtx) ? this.modeBlockSize : 0);
            if (totalLen % this.modeBlockSize != 0 || isEncrypting()) {
                i = this.modeBlockSize;
            }
            totalLen += i;
            return totalLen - (totalLen % this.modeBlockSize);
        }

        int getOutputSizeForUpdate(int inputLen) {
            return getOutputSizeForFinal(inputLen);
        }

        private void reset() {
            NativeCrypto.EVP_CipherInit_ex(this.cipherCtx, 0, this.encodedKey, this.iv, isEncrypting());
            this.calledUpdate = false;
        }
    }

    abstract void checkSupportedKeySize(int i) throws InvalidKeyException;

    abstract void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException;

    abstract void checkSupportedPadding(Padding padding) throws NoSuchPaddingException;

    abstract int doFinalInternal(byte[] bArr, int i, int i2) throws IllegalBlockSizeException, BadPaddingException, ShortBufferException;

    abstract void engineInitInternal(byte[] bArr, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException;

    abstract String getBaseCipherName();

    abstract int getCipherBlockSize();

    abstract int getOutputSizeForFinal(int i);

    abstract int getOutputSizeForUpdate(int i);

    abstract int updateInternal(byte[] bArr, int i, int i2, byte[] bArr2, int i3, int i4) throws ShortBufferException;

    OpenSSLCipher() {
    }

    OpenSSLCipher(Mode mode, Padding padding) {
        this.mode = mode;
        this.padding = padding;
        this.blockSize = getCipherBlockSize();
    }

    boolean supportsVariableSizeKey() {
        return false;
    }

    boolean supportsVariableSizeIv() {
        return false;
    }

    protected void engineSetMode(String modeStr) throws NoSuchAlgorithmException {
        try {
            Mode mode = Mode.valueOf(modeStr.toUpperCase(Locale.US));
            checkSupportedMode(mode);
            this.mode = mode;
        } catch (IllegalArgumentException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No such mode: ");
            stringBuilder.append(modeStr);
            NoSuchAlgorithmException newE = new NoSuchAlgorithmException(stringBuilder.toString());
            newE.initCause(e);
            throw newE;
        }
    }

    protected void engineSetPadding(String paddingStr) throws NoSuchPaddingException {
        try {
            Padding padding = Padding.getNormalized(paddingStr.toUpperCase(Locale.US));
            checkSupportedPadding(padding);
            this.padding = padding;
        } catch (IllegalArgumentException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No such padding: ");
            stringBuilder.append(paddingStr);
            NoSuchPaddingException newE = new NoSuchPaddingException(stringBuilder.toString());
            newE.initCause(e);
            throw newE;
        }
    }

    Padding getPadding() {
        return this.padding;
    }

    protected int engineGetBlockSize() {
        return this.blockSize;
    }

    protected int engineGetOutputSize(int inputLen) {
        return Math.max(getOutputSizeForUpdate(inputLen), getOutputSizeForFinal(inputLen));
    }

    protected byte[] engineGetIV() {
        return this.iv;
    }

    protected AlgorithmParameters engineGetParameters() {
        if (this.iv == null || this.iv.length <= 0) {
            return null;
        }
        try {
            AlgorithmParameters params = AlgorithmParameters.getInstance(getBaseCipherName());
            params.init(new IvParameterSpec(this.iv));
            return params;
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (InvalidParameterSpecException e2) {
            return null;
        }
    }

    protected AlgorithmParameterSpec getParameterSpec(AlgorithmParameters params) throws InvalidAlgorithmParameterException {
        if (params == null) {
            return null;
        }
        try {
            return params.getParameterSpec(IvParameterSpec.class);
        } catch (InvalidParameterSpecException e) {
            throw new InvalidAlgorithmParameterException("Params must be convertible to IvParameterSpec", e);
        }
    }

    protected void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        checkAndSetEncodedKey(opmode, key);
        try {
            engineInitInternal(this.encodedKey, null, random);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        checkAndSetEncodedKey(opmode, key);
        engineInitInternal(this.encodedKey, params, random);
    }

    protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        engineInit(opmode, key, getParameterSpec(params), random);
    }

    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        byte[] output;
        int maximumLen = getOutputSizeForUpdate(inputLen);
        if (maximumLen > 0) {
            output = new byte[maximumLen];
        } else {
            output = EmptyArray.BYTE;
        }
        byte[] output2 = output;
        try {
            int bytesWritten = updateInternal(input, inputOffset, inputLen, output2, 0, maximumLen);
            if (output2.length == bytesWritten) {
                return output2;
            }
            if (bytesWritten == 0) {
                return EmptyArray.BYTE;
            }
            return Arrays.copyOfRange(output2, 0, bytesWritten);
        } catch (ShortBufferException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("calculated buffer size was wrong: ");
            stringBuilder.append(maximumLen);
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        return updateInternal(input, inputOffset, inputLen, output, outputOffset, getOutputSizeForUpdate(inputLen));
    }

    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) throws IllegalBlockSizeException, BadPaddingException {
        int bytesWritten;
        int maximumLen = getOutputSizeForFinal(inputLen);
        byte[] output = new byte[maximumLen];
        if (inputLen > 0) {
            try {
                bytesWritten = updateInternal(input, inputOffset, inputLen, output, 0, maximumLen);
            } catch (ShortBufferException e) {
                throw new RuntimeException("our calculated buffer was too small", e);
            }
        }
        bytesWritten = 0;
        try {
            bytesWritten += doFinalInternal(output, bytesWritten, maximumLen - bytesWritten);
            if (bytesWritten == output.length) {
                return output;
            }
            if (bytesWritten == 0) {
                return EmptyArray.BYTE;
            }
            return Arrays.copyOfRange(output, 0, bytesWritten);
        } catch (ShortBufferException e2) {
            throw new RuntimeException("our calculated buffer was too small", e2);
        }
    }

    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        if (output != null) {
            int bytesWritten;
            int maximumLen = getOutputSizeForFinal(inputLen);
            if (inputLen > 0) {
                bytesWritten = updateInternal(input, inputOffset, inputLen, output, outputOffset, maximumLen);
                outputOffset += bytesWritten;
                maximumLen -= bytesWritten;
            } else {
                bytesWritten = 0;
            }
            return doFinalInternal(output, outputOffset, maximumLen) + bytesWritten;
        }
        throw new NullPointerException("output == null");
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

    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        if (key instanceof SecretKey) {
            byte[] encodedKey = key.getEncoded();
            if (encodedKey != null) {
                checkSupportedKeySize(encodedKey.length);
                return encodedKey.length * 8;
            }
            throw new InvalidKeyException("key.getEncoded() == null");
        }
        throw new InvalidKeyException("Only SecretKey is supported");
    }

    private byte[] checkAndSetEncodedKey(int opmode, Key key) throws InvalidKeyException {
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
        if (key instanceof SecretKey) {
            byte[] encodedKey = key.getEncoded();
            if (encodedKey != null) {
                checkSupportedKeySize(encodedKey.length);
                this.encodedKey = encodedKey;
                return encodedKey;
            }
            throw new InvalidKeyException("key.getEncoded() == null");
        }
        throw new InvalidKeyException("Only SecretKey is supported");
    }

    boolean isEncrypting() {
        return this.encrypting;
    }
}
