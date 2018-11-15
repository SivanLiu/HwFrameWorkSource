package org.bouncycastle.crypto.encodings;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.util.Arrays;

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

    public PKCS1Encoding(AsymmetricBlockCipher asymmetricBlockCipher) {
        this.engine = asymmetricBlockCipher;
        this.useStrictLength = useStrict();
    }

    public PKCS1Encoding(AsymmetricBlockCipher asymmetricBlockCipher, int i) {
        this.engine = asymmetricBlockCipher;
        this.useStrictLength = useStrict();
        this.pLen = i;
    }

    public PKCS1Encoding(AsymmetricBlockCipher asymmetricBlockCipher, byte[] bArr) {
        this.engine = asymmetricBlockCipher;
        this.useStrictLength = useStrict();
        this.fallback = bArr;
        this.pLen = bArr.length;
    }

    private static int checkPkcs1Encoding(byte[] bArr, int i) {
        i++;
        int length = bArr.length - i;
        int i2 = 0 | (bArr[0] ^ 2);
        for (int i3 = 1; i3 < length; i3++) {
            byte b = bArr[i3];
            int i4 = b | (b >> 1);
            i4 |= i4 >> 2;
            i2 |= ((i4 | (i4 >> 4)) & 1) - 1;
        }
        int i5 = bArr[bArr.length - i] | i2;
        i5 |= i5 >> 1;
        i5 |= i5 >> 2;
        return ~(((i5 | (i5 >> 4)) & 1) - 1);
    }

    private byte[] decodeBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        if (this.pLen != -1) {
            return decodeBlockOrRandom(bArr, i, i2);
        }
        bArr = this.engine.processBlock(bArr, i, i2);
        int i3 = 1;
        i = this.useStrictLength & (bArr.length != this.engine.getOutputBlockSize() ? 1 : 0);
        if (bArr.length < getOutputBlockSize()) {
            bArr = this.blockBuffer;
        }
        byte b = bArr[0];
        int i4 = (this.forPrivateKey ? b == (byte) 2 : b == (byte) 1) ? 0 : 1;
        i2 = findStart(b, bArr) + 1;
        if (i2 >= 10) {
            i3 = 0;
        }
        if ((i4 | i3) != 0) {
            Arrays.fill(bArr, (byte) 0);
            throw new InvalidCipherTextException("block incorrect");
        } else if (i == 0) {
            Object obj = new byte[(bArr.length - i2)];
            System.arraycopy(bArr, i2, obj, 0, obj.length);
            return obj;
        } else {
            Arrays.fill(bArr, (byte) 0);
            throw new InvalidCipherTextException("block incorrect size");
        }
    }

    private byte[] decodeBlockOrRandom(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        if (this.forPrivateKey) {
            byte[] bArr2;
            bArr = this.engine.processBlock(bArr, i, i2);
            if (this.fallback == null) {
                bArr2 = new byte[this.pLen];
                this.random.nextBytes(bArr2);
            } else {
                bArr2 = this.fallback;
            }
            if ((this.useStrictLength & (bArr.length != this.engine.getOutputBlockSize() ? 1 : 0)) != 0) {
                bArr = this.blockBuffer;
            }
            i2 = checkPkcs1Encoding(bArr, this.pLen);
            byte[] bArr3 = new byte[this.pLen];
            for (int i3 = 0; i3 < this.pLen; i3++) {
                bArr3[i3] = (byte) ((bArr[(bArr.length - this.pLen) + i3] & (~i2)) | (bArr2[i3] & i2));
            }
            Arrays.fill(bArr, (byte) 0);
            return bArr3;
        }
        throw new InvalidCipherTextException("sorry, this method is only for decryption, not for signing");
    }

    private byte[] encodeBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        if (i2 <= getInputBlockSize()) {
            Object obj = new byte[this.engine.getInputBlockSize()];
            int i3;
            if (this.forPrivateKey) {
                obj[0] = 1;
                for (i3 = 1; i3 != (obj.length - i2) - 1; i3++) {
                    obj[i3] = (byte) -1;
                }
            } else {
                this.random.nextBytes(obj);
                obj[0] = (byte) 2;
                for (i3 = 1; i3 != (obj.length - i2) - 1; i3++) {
                    while (obj[i3] == (byte) 0) {
                        obj[i3] = (byte) this.random.nextInt();
                    }
                }
            }
            obj[(obj.length - i2) - 1] = null;
            System.arraycopy(bArr, i, obj, obj.length - i2, i2);
            return this.engine.processBlock(obj, 0, obj.length);
        }
        throw new IllegalArgumentException("input data too large");
    }

    private int findStart(byte b, byte[] bArr) throws InvalidCipherTextException {
        int i = -1;
        int i2 = 0;
        for (int i3 = 1; i3 != bArr.length; i3++) {
            byte b2 = bArr[i3];
            if (((b2 == (byte) 0 ? 1 : 0) & (i < 0 ? 1 : 0)) != 0) {
                i = i3;
            }
            i2 |= (b2 != (byte) -1 ? 1 : 0) & ((b == (byte) 1 ? 1 : 0) & (i < 0 ? 1 : 0));
        }
        return i2 != 0 ? -1 : i;
    }

    private boolean useStrict() {
        String str = (String) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return System.getProperty(PKCS1Encoding.STRICT_LENGTH_ENABLED_PROPERTY);
            }
        });
        String str2 = (String) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return System.getProperty(PKCS1Encoding.NOT_STRICT_LENGTH_ENABLED_PROPERTY);
            }
        });
        boolean z = true;
        if (str2 != null) {
            return str2.equals("true") ^ true;
        }
        if (str != null) {
            if (str.equals("true")) {
                return true;
            }
            z = false;
        }
        return z;
    }

    public int getInputBlockSize() {
        int inputBlockSize = this.engine.getInputBlockSize();
        return this.forEncryption ? inputBlockSize - 10 : inputBlockSize;
    }

    public int getOutputBlockSize() {
        int outputBlockSize = this.engine.getOutputBlockSize();
        return this.forEncryption ? outputBlockSize : outputBlockSize - 10;
    }

    public AsymmetricBlockCipher getUnderlyingCipher() {
        return this.engine;
    }

    /* JADX WARNING: Missing block: B:16:0x0052, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void init(boolean z, CipherParameters cipherParameters) {
        AsymmetricKeyParameter asymmetricKeyParameter;
        if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            this.random = parametersWithRandom.getRandom();
            asymmetricKeyParameter = (AsymmetricKeyParameter) parametersWithRandom.getParameters();
        } else {
            asymmetricKeyParameter = (AsymmetricKeyParameter) cipherParameters;
            if (!asymmetricKeyParameter.isPrivate() && z) {
                this.random = new SecureRandom();
            }
        }
        this.engine.init(z, cipherParameters);
        this.forPrivateKey = asymmetricKeyParameter.isPrivate();
        this.forEncryption = z;
        this.blockBuffer = new byte[this.engine.getOutputBlockSize()];
        if (this.pLen > 0 && this.fallback == null && this.random == null) {
            throw new IllegalArgumentException("encoder requires random");
        }
    }

    public byte[] processBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        return this.forEncryption ? encodeBlock(bArr, i, i2) : decodeBlock(bArr, i, i2);
    }
}
