package org.bouncycastle.pqc.crypto.xmss;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.pqc.crypto.StateAwareMessageSigner;
import org.bouncycastle.pqc.crypto.xmss.XMSSSignature.Builder;
import org.bouncycastle.util.Arrays;

public class XMSSSigner implements StateAwareMessageSigner {
    private boolean hasGenerated;
    private boolean initSign;
    private KeyedHashFunctions khf;
    private XMSSPrivateKeyParameters nextKeyGenerator;
    private XMSSParameters params;
    private XMSSPrivateKeyParameters privateKey;
    private XMSSPublicKeyParameters publicKey;

    private WOTSPlusSignature wotsSign(byte[] bArr, OTSHashAddress oTSHashAddress) {
        if (bArr.length != this.params.getDigestSize()) {
            throw new IllegalArgumentException("size of messageDigest needs to be equal to size of digest");
        } else if (oTSHashAddress != null) {
            this.params.getWOTSPlus().importKeys(this.params.getWOTSPlus().getWOTSPlusSecretKey(this.privateKey.getSecretKeySeed(), oTSHashAddress), this.privateKey.getPublicSeed());
            return this.params.getWOTSPlus().sign(bArr, oTSHashAddress);
        } else {
            throw new NullPointerException("otsHashAddress == null");
        }
    }

    public byte[] generateSignature(byte[] bArr) {
        if (bArr == null) {
            throw new NullPointerException("message == null");
        } else if (!this.initSign) {
            throw new IllegalStateException("signer not initialized for signature generation");
        } else if (this.privateKey == null) {
            throw new IllegalStateException("signing key no longer usable");
        } else if (this.privateKey.getBDSState().getAuthenticationPath().isEmpty()) {
            throw new IllegalStateException("not initialized");
        } else {
            int index = this.privateKey.getIndex();
            long j = (long) index;
            if (XMSSUtil.isIndexValid(this.params.getHeight(), j)) {
                byte[] PRF = this.khf.PRF(this.privateKey.getSecretKeyPRF(), XMSSUtil.toBytesBigEndian(j, 32));
                XMSSSignature xMSSSignature = (XMSSSignature) new Builder(this.params).withIndex(index).withRandom(PRF).withWOTSPlusSignature(wotsSign(this.khf.HMsg(Arrays.concatenate(PRF, this.privateKey.getRoot(), XMSSUtil.toBytesBigEndian(j, this.params.getDigestSize())), bArr), (OTSHashAddress) new Builder().withOTSAddress(index).build())).withAuthPath(this.privateKey.getBDSState().getAuthenticationPath()).build();
                this.hasGenerated = true;
                if (this.nextKeyGenerator != null) {
                    this.privateKey = this.nextKeyGenerator.getNextKey();
                    this.nextKeyGenerator = this.privateKey;
                } else {
                    this.privateKey = null;
                }
                return xMSSSignature.toByteArray();
            }
            throw new IllegalStateException("index out of bounds");
        }
    }

    public AsymmetricKeyParameter getUpdatedPrivateKey() {
        AsymmetricKeyParameter asymmetricKeyParameter;
        if (this.hasGenerated) {
            asymmetricKeyParameter = this.privateKey;
            this.privateKey = null;
        } else {
            asymmetricKeyParameter = this.nextKeyGenerator.getNextKey();
        }
        this.nextKeyGenerator = null;
        return asymmetricKeyParameter;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0032 in {2, 4, 5} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public void init(boolean r2, org.bouncycastle.crypto.CipherParameters r3) {
        /*
        r1 = this;
        r0 = 0;
        if (r2 == 0) goto L_0x0025;
    L_0x0003:
        r2 = 1;
        r1.initSign = r2;
        r1.hasGenerated = r0;
        r3 = (org.bouncycastle.pqc.crypto.xmss.XMSSPrivateKeyParameters) r3;
        r1.privateKey = r3;
        r2 = r1.privateKey;
        r1.nextKeyGenerator = r2;
        r2 = r1.privateKey;
        r2 = r2.getParameters();
    L_0x0016:
        r1.params = r2;
        r2 = r1.params;
        r2 = r2.getWOTSPlus();
        r2 = r2.getKhf();
        r1.khf = r2;
        return;
    L_0x0025:
        r1.initSign = r0;
        r3 = (org.bouncycastle.pqc.crypto.xmss.XMSSPublicKeyParameters) r3;
        r1.publicKey = r3;
        r2 = r1.publicKey;
        r2 = r2.getParameters();
        goto L_0x0016;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.pqc.crypto.xmss.XMSSSigner.init(boolean, org.bouncycastle.crypto.CipherParameters):void");
    }

    public boolean verifySignature(byte[] bArr, byte[] bArr2) {
        XMSSReducedSignature build = new Builder(this.params).withSignature(bArr2).build();
        int index = build.getIndex();
        this.params.getWOTSPlus().importKeys(new byte[this.params.getDigestSize()], this.publicKey.getPublicSeed());
        long j = (long) index;
        byte[] HMsg = this.khf.HMsg(Arrays.concatenate(build.getRandom(), this.publicKey.getRoot(), XMSSUtil.toBytesBigEndian(j, this.params.getDigestSize())), bArr);
        int height = this.params.getHeight();
        int leafIndex = XMSSUtil.getLeafIndex(j, height);
        return Arrays.constantTimeAreEqual(XMSSVerifierUtil.getRootNodeFromSignature(this.params.getWOTSPlus(), height, HMsg, build, (OTSHashAddress) new Builder().withOTSAddress(index).build(), leafIndex).getValue(), this.publicKey.getRoot());
    }
}
