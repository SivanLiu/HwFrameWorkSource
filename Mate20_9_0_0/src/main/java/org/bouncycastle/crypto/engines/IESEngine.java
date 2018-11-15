package org.bouncycastle.crypto.engines;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.bouncycastle.crypto.BasicAgreement;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DerivationFunction;
import org.bouncycastle.crypto.EphemeralKeyPair;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.KeyParser;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.generators.EphemeralKeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.IESParameters;
import org.bouncycastle.crypto.params.IESWithCipherParameters;
import org.bouncycastle.crypto.params.KDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.Pack;

public class IESEngine {
    private byte[] IV;
    byte[] V;
    BasicAgreement agree;
    BufferedBlockCipher cipher;
    boolean forEncryption;
    DerivationFunction kdf;
    private EphemeralKeyPairGenerator keyPairGenerator;
    private KeyParser keyParser;
    Mac mac;
    byte[] macBuf;
    IESParameters param;
    CipherParameters privParam;
    CipherParameters pubParam;

    public IESEngine(BasicAgreement basicAgreement, DerivationFunction derivationFunction, Mac mac) {
        this.agree = basicAgreement;
        this.kdf = derivationFunction;
        this.mac = mac;
        this.macBuf = new byte[mac.getMacSize()];
        this.cipher = null;
    }

    public IESEngine(BasicAgreement basicAgreement, DerivationFunction derivationFunction, Mac mac, BufferedBlockCipher bufferedBlockCipher) {
        this.agree = basicAgreement;
        this.kdf = derivationFunction;
        this.mac = mac;
        this.macBuf = new byte[mac.getMacSize()];
        this.cipher = bufferedBlockCipher;
    }

    private byte[] decryptBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        if (i2 >= this.V.length + this.mac.getMacSize()) {
            byte[] bArr2;
            byte[] bArr3;
            int i3;
            Object obj;
            Object obj2;
            if (this.cipher == null) {
                obj = new byte[((i2 - this.V.length) - this.mac.getMacSize())];
                bArr2 = new byte[(this.param.getMacKeySize() / 8)];
                obj2 = new byte[(obj.length + bArr2.length)];
                this.kdf.generateBytes(obj2, 0, obj2.length);
                if (this.V.length != 0) {
                    System.arraycopy(obj2, 0, bArr2, 0, bArr2.length);
                    System.arraycopy(obj2, bArr2.length, obj, 0, obj.length);
                } else {
                    System.arraycopy(obj2, 0, obj, 0, obj.length);
                    System.arraycopy(obj2, obj.length, bArr2, 0, bArr2.length);
                }
                byte[] bArr4 = new byte[obj.length];
                for (int i4 = 0; i4 != obj.length; i4++) {
                    bArr4[i4] = (byte) (bArr[(this.V.length + i) + i4] ^ obj[i4]);
                }
                bArr3 = bArr4;
                i3 = 0;
            } else {
                obj = new byte[(((IESWithCipherParameters) this.param).getCipherKeySize() / 8)];
                bArr2 = new byte[(this.param.getMacKeySize() / 8)];
                obj2 = new byte[(obj.length + bArr2.length)];
                this.kdf.generateBytes(obj2, 0, obj2.length);
                System.arraycopy(obj2, 0, obj, 0, obj.length);
                System.arraycopy(obj2, obj.length, bArr2, 0, bArr2.length);
                CipherParameters keyParameter = new KeyParameter(obj);
                this.cipher.init(false, this.IV != null ? new ParametersWithIV(keyParameter, this.IV) : keyParameter);
                bArr3 = new byte[this.cipher.getOutputSize((i2 - this.V.length) - this.mac.getMacSize())];
                i3 = this.cipher.processBytes(bArr, i + this.V.length, (i2 - this.V.length) - this.mac.getMacSize(), bArr3, 0);
            }
            byte[] encodingV = this.param.getEncodingV();
            byte[] bArr5 = null;
            if (this.V.length != 0) {
                bArr5 = getLengthTag(encodingV);
            }
            int i5 = i + i2;
            byte[] copyOfRange = Arrays.copyOfRange(bArr, i5 - this.mac.getMacSize(), i5);
            byte[] bArr6 = new byte[copyOfRange.length];
            this.mac.init(new KeyParameter(bArr2));
            this.mac.update(bArr, i + this.V.length, (i2 - this.V.length) - bArr6.length);
            if (encodingV != null) {
                this.mac.update(encodingV, 0, encodingV.length);
            }
            if (this.V.length != 0) {
                this.mac.update(bArr5, 0, bArr5.length);
            }
            this.mac.doFinal(bArr6, 0);
            if (Arrays.constantTimeAreEqual(copyOfRange, bArr6)) {
                return this.cipher == null ? bArr3 : Arrays.copyOfRange(bArr3, 0, i3 + this.cipher.doFinal(bArr3, i3));
            } else {
                throw new InvalidCipherTextException("invalid MAC");
            }
        }
        throw new InvalidCipherTextException("Length of input must be greater than the MAC and V combined");
    }

    private byte[] encryptBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        Object obj;
        byte[] bArr2;
        Object obj2;
        if (this.cipher == null) {
            obj = new byte[i2];
            bArr2 = new byte[(this.param.getMacKeySize() / 8)];
            obj2 = new byte[(obj.length + bArr2.length)];
            this.kdf.generateBytes(obj2, 0, obj2.length);
            if (this.V.length != 0) {
                System.arraycopy(obj2, 0, bArr2, 0, bArr2.length);
                System.arraycopy(obj2, bArr2.length, obj, 0, obj.length);
            } else {
                System.arraycopy(obj2, 0, obj, 0, obj.length);
                System.arraycopy(obj2, i2, bArr2, 0, bArr2.length);
            }
            obj2 = new byte[i2];
            for (int i3 = 0; i3 != i2; i3++) {
                obj2[i3] = (byte) (bArr[i + i3] ^ obj[i3]);
            }
            obj = obj2;
        } else {
            BufferedBlockCipher bufferedBlockCipher;
            CipherParameters parametersWithIV;
            obj = new byte[(((IESWithCipherParameters) this.param).getCipherKeySize() / 8)];
            bArr2 = new byte[(this.param.getMacKeySize() / 8)];
            obj2 = new byte[(obj.length + bArr2.length)];
            this.kdf.generateBytes(obj2, 0, obj2.length);
            System.arraycopy(obj2, 0, obj, 0, obj.length);
            System.arraycopy(obj2, obj.length, bArr2, 0, bArr2.length);
            if (this.IV != null) {
                bufferedBlockCipher = this.cipher;
                parametersWithIV = new ParametersWithIV(new KeyParameter(obj), this.IV);
            } else {
                bufferedBlockCipher = this.cipher;
                parametersWithIV = new KeyParameter(obj);
            }
            bufferedBlockCipher.init(true, parametersWithIV);
            obj = new byte[this.cipher.getOutputSize(i2)];
            int processBytes = this.cipher.processBytes(bArr, i, i2, obj, 0);
            i2 = processBytes + this.cipher.doFinal(obj, processBytes);
        }
        bArr = this.param.getEncodingV();
        byte[] bArr3 = null;
        if (this.V.length != 0) {
            bArr3 = getLengthTag(bArr);
        }
        obj2 = new byte[this.mac.getMacSize()];
        this.mac.init(new KeyParameter(bArr2));
        this.mac.update(obj, 0, obj.length);
        if (bArr != null) {
            this.mac.update(bArr, 0, bArr.length);
        }
        if (this.V.length != 0) {
            this.mac.update(bArr3, 0, bArr3.length);
        }
        this.mac.doFinal(obj2, 0);
        Object obj3 = new byte[((this.V.length + i2) + obj2.length)];
        System.arraycopy(this.V, 0, obj3, 0, this.V.length);
        System.arraycopy(obj, 0, obj3, this.V.length, i2);
        System.arraycopy(obj2, 0, obj3, this.V.length + i2, obj2.length);
        return obj3;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0019 in {2, 4, 5} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private void extractParams(org.bouncycastle.crypto.CipherParameters r2) {
        /*
        r1 = this;
        r0 = r2 instanceof org.bouncycastle.crypto.params.ParametersWithIV;
        if (r0 == 0) goto L_0x0015;
    L_0x0004:
        r2 = (org.bouncycastle.crypto.params.ParametersWithIV) r2;
        r0 = r2.getIV();
        r1.IV = r0;
        r2 = r2.getParameters();
    L_0x0010:
        r2 = (org.bouncycastle.crypto.params.IESParameters) r2;
        r1.param = r2;
        return;
    L_0x0015:
        r0 = 0;
        r1.IV = r0;
        goto L_0x0010;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.engines.IESEngine.extractParams(org.bouncycastle.crypto.CipherParameters):void");
    }

    public BufferedBlockCipher getCipher() {
        return this.cipher;
    }

    protected byte[] getLengthTag(byte[] bArr) {
        byte[] bArr2 = new byte[8];
        if (bArr != null) {
            Pack.longToBigEndian(((long) bArr.length) * 8, bArr2, 0);
        }
        return bArr2;
    }

    public Mac getMac() {
        return this.mac;
    }

    public void init(AsymmetricKeyParameter asymmetricKeyParameter, CipherParameters cipherParameters, KeyParser keyParser) {
        this.forEncryption = false;
        this.privParam = asymmetricKeyParameter;
        this.keyParser = keyParser;
        extractParams(cipherParameters);
    }

    public void init(AsymmetricKeyParameter asymmetricKeyParameter, CipherParameters cipherParameters, EphemeralKeyPairGenerator ephemeralKeyPairGenerator) {
        this.forEncryption = true;
        this.pubParam = asymmetricKeyParameter;
        this.keyPairGenerator = ephemeralKeyPairGenerator;
        extractParams(cipherParameters);
    }

    public void init(boolean z, CipherParameters cipherParameters, CipherParameters cipherParameters2, CipherParameters cipherParameters3) {
        this.forEncryption = z;
        this.privParam = cipherParameters;
        this.pubParam = cipherParameters2;
        this.V = new byte[0];
        extractParams(cipherParameters3);
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x0093  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x00b6 A:{Catch:{ all -> 0x00be }} */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x00b1 A:{Catch:{ all -> 0x00be }} */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0093  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x00b1 A:{Catch:{ all -> 0x00be }} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x00b6 A:{Catch:{ all -> 0x00be }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public byte[] processBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        byte[] encodedPublicKey;
        StringBuilder stringBuilder;
        if (this.forEncryption) {
            if (this.keyPairGenerator != null) {
                EphemeralKeyPair generate = this.keyPairGenerator.generate();
                this.privParam = generate.getKeyPair().getPrivate();
                encodedPublicKey = generate.getEncodedPublicKey();
            }
            this.agree.init(this.privParam);
            encodedPublicKey = BigIntegers.asUnsignedByteArray(this.agree.getFieldSize(), this.agree.calculateAgreement(this.pubParam));
            if (this.V.length != 0) {
                byte[] concatenate = Arrays.concatenate(this.V, encodedPublicKey);
                Arrays.fill(encodedPublicKey, (byte) 0);
                encodedPublicKey = concatenate;
            }
            this.kdf.init(new KDFParameters(encodedPublicKey, this.param.getDerivationV()));
            bArr = this.forEncryption ? encryptBlock(bArr, i, i2) : decryptBlock(bArr, i, i2);
            Arrays.fill(encodedPublicKey, (byte) 0);
            return bArr;
        }
        if (this.keyParser != null) {
            InputStream byteArrayInputStream = new ByteArrayInputStream(bArr, i, i2);
            try {
                this.pubParam = this.keyParser.readKey(byteArrayInputStream);
                encodedPublicKey = Arrays.copyOfRange(bArr, i, (i2 - byteArrayInputStream.available()) + i);
            } catch (Throwable e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("unable to recover ephemeral public key: ");
                stringBuilder.append(e.getMessage());
                throw new InvalidCipherTextException(stringBuilder.toString(), e);
            } catch (Throwable e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("unable to recover ephemeral public key: ");
                stringBuilder.append(e2.getMessage());
                throw new InvalidCipherTextException(stringBuilder.toString(), e2);
            }
        }
        this.agree.init(this.privParam);
        encodedPublicKey = BigIntegers.asUnsignedByteArray(this.agree.getFieldSize(), this.agree.calculateAgreement(this.pubParam));
        if (this.V.length != 0) {
        }
        this.kdf.init(new KDFParameters(encodedPublicKey, this.param.getDerivationV()));
        if (this.forEncryption) {
        }
        Arrays.fill(encodedPublicKey, (byte) 0);
        return bArr;
        this.V = encodedPublicKey;
        this.agree.init(this.privParam);
        encodedPublicKey = BigIntegers.asUnsignedByteArray(this.agree.getFieldSize(), this.agree.calculateAgreement(this.pubParam));
        if (this.V.length != 0) {
        }
        try {
            this.kdf.init(new KDFParameters(encodedPublicKey, this.param.getDerivationV()));
            if (this.forEncryption) {
            }
            Arrays.fill(encodedPublicKey, (byte) 0);
            return bArr;
        } catch (Throwable th) {
            Arrays.fill(encodedPublicKey, (byte) 0);
        }
    }
}
