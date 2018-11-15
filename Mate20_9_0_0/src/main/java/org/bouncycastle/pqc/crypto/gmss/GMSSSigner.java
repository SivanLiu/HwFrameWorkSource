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
            Object obj = new byte[this.mdLength];
            System.arraycopy(gMSSPrivateKeyParameters.getCurrentSeeds()[this.numLayer - 1], 0, obj, 0, this.mdLength);
            this.ots = new WinternitzOTSignature(this.gmssRandom.nextSeed(obj), this.digestProvider.get(), this.gmssPS.getWinternitzParameter()[this.numLayer - 1]);
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
                obj = gMSSPrivateKeyParameters.getSubtreeRootSig(i3);
                this.subtreeRootSig[i3] = new byte[obj.length];
                System.arraycopy(obj, 0, this.subtreeRootSig[i3], 0, obj.length);
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
        Object signature = this.ots.getSignature(bArr);
        Object concatenateArray = this.gmssUtil.concatenateArray(this.currentAuthPaths[this.numLayer - 1]);
        Object intToBytesLittleEndian = this.gmssUtil.intToBytesLittleEndian(this.index[this.numLayer - 1]);
        Object obj = new byte[((intToBytesLittleEndian.length + signature.length) + concatenateArray.length)];
        System.arraycopy(intToBytesLittleEndian, 0, obj, 0, intToBytesLittleEndian.length);
        System.arraycopy(signature, 0, obj, intToBytesLittleEndian.length, signature.length);
        System.arraycopy(concatenateArray, 0, obj, intToBytesLittleEndian.length + signature.length, concatenateArray.length);
        signature = new byte[0];
        for (int i = (this.numLayer - 1) - 1; i >= 0; i--) {
            intToBytesLittleEndian = this.gmssUtil.concatenateArray(this.currentAuthPaths[i]);
            Object intToBytesLittleEndian2 = this.gmssUtil.intToBytesLittleEndian(this.index[i]);
            Object obj2 = new byte[signature.length];
            System.arraycopy(signature, 0, obj2, 0, signature.length);
            signature = new byte[(((obj2.length + intToBytesLittleEndian2.length) + this.subtreeRootSig[i].length) + intToBytesLittleEndian.length)];
            System.arraycopy(obj2, 0, signature, 0, obj2.length);
            System.arraycopy(intToBytesLittleEndian2, 0, signature, obj2.length, intToBytesLittleEndian2.length);
            System.arraycopy(this.subtreeRootSig[i], 0, signature, obj2.length + intToBytesLittleEndian2.length, this.subtreeRootSig[i].length);
            System.arraycopy(intToBytesLittleEndian, 0, signature, (obj2.length + intToBytesLittleEndian2.length) + this.subtreeRootSig[i].length, intToBytesLittleEndian.length);
        }
        concatenateArray = new byte[(obj.length + signature.length)];
        System.arraycopy(obj, 0, concatenateArray, 0, obj.length);
        System.arraycopy(signature, 0, concatenateArray, obj.length, signature.length);
        return concatenateArray;
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
            Object obj = new byte[signatureLength];
            System.arraycopy(bArr2, i2, obj, 0, signatureLength);
            i2 += signatureLength;
            Object Verify = winternitzOTSVerify.Verify(bArr, obj);
            if (Verify == null) {
                System.err.println("OTS Public Key is null in GMSSSignature.verify");
                return false;
            }
            byte[][] bArr3 = (byte[][]) Array.newInstance(byte.class, new int[]{this.gmssPS.getHeightOfTrees()[i], this.mdLength});
            signatureLength = i2;
            for (Object obj2 : bArr3) {
                System.arraycopy(bArr2, signatureLength, obj2, 0, this.mdLength);
                signatureLength += this.mdLength;
            }
            byte[] bArr4 = new byte[this.mdLength];
            i2 = (1 << bArr3.length) + bytesToIntLittleEndian;
            Object obj3 = Verify;
            for (int i3 = 0; i3 < bArr3.length; i3++) {
                obj2 = new byte[(this.mdLength << 1)];
                if (i2 % 2 == 0) {
                    System.arraycopy(obj3, 0, obj2, 0, this.mdLength);
                    System.arraycopy(bArr3[i3], 0, obj2, this.mdLength, this.mdLength);
                } else {
                    System.arraycopy(bArr3[i3], 0, obj2, 0, this.mdLength);
                    System.arraycopy(obj3, 0, obj2, this.mdLength, obj3.length);
                    i2--;
                }
                i2 /= 2;
                this.messDigestTrees.update(obj2, 0, obj2.length);
                obj3 = new byte[this.messDigestTrees.getDigestSize()];
                this.messDigestTrees.doFinal(obj3, 0);
            }
            i--;
            i2 = signatureLength;
            Verify = obj3;
        }
        return Arrays.areEqual(this.pubKeyBytes, bArr);
    }
}
