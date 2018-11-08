package com.android.org.bouncycastle.crypto.encodings;

import com.android.org.bouncycastle.crypto.AsymmetricBlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;
import com.android.org.bouncycastle.util.Arrays;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;

public class PKCS1Encoding implements AsymmetricBlockCipher {
    private static final int HEADER_LENGTH = 10;
    public static final String NOT_STRICT_LENGTH_ENABLED_PROPERTY = "org.bouncycastle.pkcs1.not_strict";
    public static final String STRICT_LENGTH_ENABLED_PROPERTY = "org.bouncycastle.pkcs1.strict";
    private byte[] blockBuffer;
    private AsymmetricBlockCipher engine;
    private byte[] fallback = null;
    private boolean forEncryption;
    private boolean forPrivateKey;
    private int pLen = -1;
    private SecureRandom random;
    private boolean useStrictLength;

    private static int checkPkcs1Encoding(byte[] r1, int r2) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.org.bouncycastle.crypto.encodings.PKCS1Encoding.checkPkcs1Encoding(byte[], int):int
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.crypto.encodings.PKCS1Encoding.checkPkcs1Encoding(byte[], int):int");
    }

    private byte[] decodeBlockOrRandom(byte[] r1, int r2, int r3) throws com.android.org.bouncycastle.crypto.InvalidCipherTextException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.org.bouncycastle.crypto.encodings.PKCS1Encoding.decodeBlockOrRandom(byte[], int, int):byte[]
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.crypto.encodings.PKCS1Encoding.decodeBlockOrRandom(byte[], int, int):byte[]");
    }

    public PKCS1Encoding(AsymmetricBlockCipher cipher) {
        this.engine = cipher;
        this.useStrictLength = useStrict();
    }

    public PKCS1Encoding(AsymmetricBlockCipher cipher, int pLen) {
        this.engine = cipher;
        this.useStrictLength = useStrict();
        this.pLen = pLen;
    }

    public PKCS1Encoding(AsymmetricBlockCipher cipher, byte[] fallback) {
        this.engine = cipher;
        this.useStrictLength = useStrict();
        this.fallback = fallback;
        this.pLen = fallback.length;
    }

    private boolean useStrict() {
        String strict = (String) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return System.getProperty("com.android.org.bouncycastle.pkcs1.strict");
            }
        });
        String notStrict = (String) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return System.getProperty("com.android.org.bouncycastle.pkcs1.not_strict");
            }
        });
        if (notStrict != null) {
            return notStrict.equals("true") ^ 1;
        }
        return strict != null ? strict.equals("true") : true;
    }

    public AsymmetricBlockCipher getUnderlyingCipher() {
        return this.engine;
    }

    public void init(boolean forEncryption, CipherParameters param) {
        AsymmetricKeyParameter kParam;
        if (param instanceof ParametersWithRandom) {
            ParametersWithRandom rParam = (ParametersWithRandom) param;
            this.random = rParam.getRandom();
            kParam = (AsymmetricKeyParameter) rParam.getParameters();
        } else {
            kParam = (AsymmetricKeyParameter) param;
            if (!kParam.isPrivate() && forEncryption) {
                this.random = new SecureRandom();
            }
        }
        this.engine.init(forEncryption, param);
        this.forPrivateKey = kParam.isPrivate();
        this.forEncryption = forEncryption;
        this.blockBuffer = new byte[this.engine.getOutputBlockSize()];
        if (this.pLen > 0 && this.fallback == null && this.random == null) {
            throw new IllegalArgumentException("encoder requires random");
        }
    }

    public int getInputBlockSize() {
        int baseBlockSize = this.engine.getInputBlockSize();
        if (this.forEncryption) {
            return baseBlockSize - 10;
        }
        return baseBlockSize;
    }

    public int getOutputBlockSize() {
        int baseBlockSize = this.engine.getOutputBlockSize();
        if (this.forEncryption) {
            return baseBlockSize;
        }
        return baseBlockSize - 10;
    }

    public byte[] processBlock(byte[] in, int inOff, int inLen) throws InvalidCipherTextException {
        if (this.forEncryption) {
            return encodeBlock(in, inOff, inLen);
        }
        return decodeBlock(in, inOff, inLen);
    }

    private byte[] encodeBlock(byte[] in, int inOff, int inLen) throws InvalidCipherTextException {
        if (inLen > getInputBlockSize()) {
            throw new IllegalArgumentException("input data too large");
        }
        byte[] block = new byte[this.engine.getInputBlockSize()];
        int i;
        if (this.forPrivateKey) {
            block[0] = (byte) 1;
            for (i = 1; i != (block.length - inLen) - 1; i++) {
                block[i] = (byte) -1;
            }
        } else {
            this.random.nextBytes(block);
            block[0] = (byte) 2;
            for (i = 1; i != (block.length - inLen) - 1; i++) {
                while (block[i] == (byte) 0) {
                    block[i] = (byte) this.random.nextInt();
                }
            }
        }
        block[(block.length - inLen) - 1] = (byte) 0;
        System.arraycopy(in, inOff, block, block.length - inLen, inLen);
        return this.engine.processBlock(block, 0, block.length);
    }

    private byte[] decodeBlock(byte[] in, int inOff, int inLen) throws InvalidCipherTextException {
        if (this.pLen != -1) {
            return decodeBlockOrRandom(in, inOff, inLen);
        }
        byte[] data;
        byte[] block = this.engine.processBlock(in, inOff, inLen);
        boolean incorrectLength = this.useStrictLength & (block.length != this.engine.getOutputBlockSize() ? 1 : 0);
        if (block.length < getOutputBlockSize()) {
            data = this.blockBuffer;
        } else {
            data = block;
        }
        byte type = data[0];
        boolean badType = this.forPrivateKey ? type != (byte) 2 : type != (byte) 1;
        if (!(type == (byte) 1 && this.forPrivateKey) && (type != (byte) 2 || (this.forPrivateKey ^ 1) == 0)) {
            int start = findStart(type, data) + 1;
            if (((start < 10 ? 1 : 0) | badType) != 0) {
                Arrays.fill(data, (byte) 0);
                throw new InvalidCipherTextException("block incorrect");
            } else if (incorrectLength) {
                Arrays.fill(data, (byte) 0);
                throw new InvalidCipherTextException("block incorrect size");
            } else {
                byte[] result = new byte[(data.length - start)];
                System.arraycopy(data, start, result, 0, result.length);
                return result;
            }
        }
        throw new InvalidCipherTextException("invalid block type " + type);
    }

    private int findStart(byte type, byte[] block) throws InvalidCipherTextException {
        int start = -1;
        int padErr = 0;
        for (int i = 1; i != block.length; i++) {
            int i2;
            int i3;
            byte pad = block[i];
            if (pad == (byte) 0) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            if (((start < 0 ? 1 : 0) & i2) != 0) {
                start = i;
            }
            if (type == (byte) 1) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            if (start < 0) {
                i3 = 1;
            } else {
                i3 = 0;
            }
            i2 &= i3;
            if (pad != (byte) -1) {
                i3 = 1;
            } else {
                i3 = 0;
            }
            padErr |= i3 & i2;
        }
        if (padErr != 0) {
            return -1;
        }
        return start;
    }
}
