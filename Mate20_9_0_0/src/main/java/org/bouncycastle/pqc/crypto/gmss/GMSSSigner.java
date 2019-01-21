package org.bouncycastle.pqc.crypto.gmss;

import java.lang.reflect.Array;
import java.security.SecureRandom;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.pqc.crypto.MessageSigner;
import org.bouncycastle.pqc.crypto.gmss.util.GMSSRandom;
import org.bouncycastle.pqc.crypto.gmss.util.GMSSUtil;
import org.bouncycastle.pqc.crypto.gmss.util.WinternitzOTSVerify;
import org.bouncycastle.pqc.crypto.gmss.util.WinternitzOTSignature;
import org.bouncycastle.util.Arrays;

public class GMSSSigner implements MessageSigner {
    private byte[][][] currentAuthPaths;
    private GMSSDigestProvider digestProvider;
    private GMSSParameters gmssPS;
    private GMSSRandom gmssRandom;
    private GMSSUtil gmssUtil = new GMSSUtil();
    private int[] index;
    GMSSKeyParameters key;
    private int mdLength;
    private Digest messDigestOTS;
    private Digest messDigestTrees;
    private int numLayer;
    private WinternitzOTSignature ots;
    private byte[] pubKeyBytes;
    private SecureRandom random;
    private byte[][] subtreeRootSig;

    public GMSSSigner(GMSSDigestProvider gMSSDigestProvider) {
        this.digestProvider = gMSSDigestProvider;
        this.messDigestTrees = gMSSDigestProvider.get();
        this.messDigestOTS = this.messDigestTrees;
        this.mdLength = this.messDigestTrees.getDigestSize();
        this.gmssRandom = new GMSSRandom(this.messDigestTrees);
    }

    private void initSign() {
        this.messDigestTrees.reset();
        GMSSPrivateKeyParameters gMSSPrivateKeyParameters = (GMSSPrivateKeyParameters) this.key;
        if (gMSSPrivateKeyParameters.isUsed()) {
            throw new IllegalStateException("Private key already used");
        } else if (gMSSPrivateKeyParameters.getIndex(0) < gMSSPrivateKeyParameters.getNumLeafs(0)) {
            this.gmssPS = gMSSPrivateKeyParameters.getParameters();
            this.numLayer = this.gmssPS.getNumOfLayers();
            byte[] bArr = new byte[this.mdLength];
            bArr = new byte[this.mdLength];
            System.arraycopy(gMSSPrivateKeyParameters.getCurrentSeeds()[this.numLayer - 1], 0, bArr, 0, this.mdLength);
            this.ots = new WinternitzOTSignature(this.gmssRandom.nextSeed(bArr), this.digestProvider.get(), this.gmssPS.getWinternitzParameter()[this.numLayer - 1]);
            byte[][][] currentAuthPaths = gMSSPrivateKeyParameters.getCurrentAuthPaths();
            this.currentAuthPaths = new byte[this.numLayer][][];
            for (int i = 0; i < this.numLayer; i++) {
                this.currentAuthPaths[i] = (byte[][]) Array.newInstance(byte.class, new int[]{currentAuthPaths[i].length, this.mdLength});
                for (int i2 = 0; i2 < currentAuthPaths[i].length; i2++) {
                    System.arraycopy(currentAuthPaths[i][i2], 0, this.currentAuthPaths[i][i2], 0, this.mdLength);
                }
            }
            this.index = new int[this.numLayer];
            System.arraycopy(gMSSPrivateKeyParameters.getIndex(), 0, this.index, 0, this.numLayer);
            this.subtreeRootSig = new byte[(this.numLayer - 1)][];
            for (int i3 = 0; i3 < this.numLayer - 1; i3++) {
                bArr = gMSSPrivateKeyParameters.getSubtreeRootSig(i3);
                this.subtreeRootSig[i3] = new byte[bArr.length];
                System.arraycopy(bArr, 0, this.subtreeRootSig[i3], 0, bArr.length);
            }
            gMSSPrivateKeyParameters.markUsed();
        } else {
            throw new IllegalStateException("No more signatures can be generated");
        }
    }

    private void initVerify() {
        this.messDigestTrees.reset();
        GMSSPublicKeyParameters gMSSPublicKeyParameters = (GMSSPublicKeyParameters) this.key;
        this.pubKeyBytes = gMSSPublicKeyParameters.getPublicKey();
        this.gmssPS = gMSSPublicKeyParameters.getParameters();
        this.numLayer = this.gmssPS.getNumOfLayers();
    }

    public byte[] generateSignature(byte[] bArr) {
        byte[] bArr2 = new byte[this.mdLength];
        bArr = this.ots.getSignature(bArr);
        bArr2 = this.gmssUtil.concatenateArray(this.currentAuthPaths[this.numLayer - 1]);
        byte[] intToBytesLittleEndian = this.gmssUtil.intToBytesLittleEndian(this.index[this.numLayer - 1]);
        byte[] bArr3 = new byte[((intToBytesLittleEndian.length + bArr.length) + bArr2.length)];
        System.arraycopy(intToBytesLittleEndian, 0, bArr3, 0, intToBytesLittleEndian.length);
        System.arraycopy(bArr, 0, bArr3, intToBytesLittleEndian.length, bArr.length);
        System.arraycopy(bArr2, 0, bArr3, intToBytesLittleEndian.length + bArr.length, bArr2.length);
        Object obj = new byte[0];
        for (int i = (this.numLayer - 1) - 1; i >= 0; i--) {
            intToBytesLittleEndian = this.gmssUtil.concatenateArray(this.currentAuthPaths[i]);
            byte[] intToBytesLittleEndian2 = this.gmssUtil.intToBytesLittleEndian(this.index[i]);
            byte[] bArr4 = new byte[obj.length];
            System.arraycopy(obj, 0, bArr4, 0, obj.length);
            obj = new byte[(((bArr4.length + intToBytesLittleEndian2.length) + this.subtreeRootSig[i].length) + intToBytesLittleEndian.length)];
            System.arraycopy(bArr4, 0, obj, 0, bArr4.length);
            System.arraycopy(intToBytesLittleEndian2, 0, obj, bArr4.length, intToBytesLittleEndian2.length);
            System.arraycopy(this.subtreeRootSig[i], 0, obj, bArr4.length + intToBytesLittleEndian2.length, this.subtreeRootSig[i].length);
            System.arraycopy(intToBytesLittleEndian, 0, obj, (bArr4.length + intToBytesLittleEndian2.length) + this.subtreeRootSig[i].length, intToBytesLittleEndian.length);
        }
        bArr2 = new byte[(bArr3.length + obj.length)];
        System.arraycopy(bArr3, 0, bArr2, 0, bArr3.length);
        System.arraycopy(obj, 0, bArr2, bArr3.length, obj.length);
        return bArr2;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (z) {
            if (cipherParameters instanceof ParametersWithRandom) {
                ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
                this.random = parametersWithRandom.getRandom();
                this.key = (GMSSPrivateKeyParameters) parametersWithRandom.getParameters();
            } else {
                this.random = new SecureRandom();
                this.key = (GMSSPrivateKeyParameters) cipherParameters;
            }
            initSign();
            return;
        }
        this.key = (GMSSPublicKeyParameters) cipherParameters;
        initVerify();
    }

    public boolean verifySignature(byte[] bArr, byte[] bArr2) {
        this.messDigestOTS.reset();
        int i = this.numLayer - 1;
        int i2 = 0;
        while (i >= 0) {
            WinternitzOTSVerify winternitzOTSVerify = new WinternitzOTSVerify(this.digestProvider.get(), this.gmssPS.getWinternitzParameter()[i]);
            int signatureLength = winternitzOTSVerify.getSignatureLength();
            int bytesToIntLittleEndian = this.gmssUtil.bytesToIntLittleEndian(bArr2, i2);
            i2 += 4;
            byte[] bArr3 = new byte[signatureLength];
            System.arraycopy(bArr2, i2, bArr3, 0, signatureLength);
            i2 += signatureLength;
            Object Verify = winternitzOTSVerify.Verify(bArr, bArr3);
            if (Verify == null) {
                System.err.println("OTS Public Key is null in GMSSSignature.verify");
                return false;
            }
            byte[][] bArr4 = (byte[][]) Array.newInstance(byte.class, new int[]{this.gmssPS.getHeightOfTrees()[i], this.mdLength});
            signatureLength = i2;
            for (Object arraycopy : bArr4) {
                System.arraycopy(bArr2, signatureLength, arraycopy, 0, this.mdLength);
                signatureLength += this.mdLength;
            }
            byte[] bArr5 = new byte[this.mdLength];
            i2 = (1 << bArr4.length) + bytesToIntLittleEndian;
            Object obj = Verify;
            for (int i3 = 0; i3 < bArr4.length; i3++) {
                bArr3 = new byte[(this.mdLength << 1)];
                if (i2 % 2 == 0) {
                    System.arraycopy(obj, 0, bArr3, 0, this.mdLength);
                    System.arraycopy(bArr4[i3], 0, bArr3, this.mdLength, this.mdLength);
                } else {
                    System.arraycopy(bArr4[i3], 0, bArr3, 0, this.mdLength);
                    System.arraycopy(obj, 0, bArr3, this.mdLength, obj.length);
                    i2--;
                }
                i2 /= 2;
                this.messDigestTrees.update(bArr3, 0, bArr3.length);
                obj = new byte[this.messDigestTrees.getDigestSize()];
                this.messDigestTrees.doFinal(obj, 0);
            }
            i--;
            i2 = signatureLength;
            Verify = obj;
        }
        return Arrays.areEqual(this.pubKeyBytes, bArr);
    }
}
