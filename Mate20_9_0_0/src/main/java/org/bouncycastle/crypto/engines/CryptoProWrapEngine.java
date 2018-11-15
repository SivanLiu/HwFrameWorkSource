package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.modes.GCFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.ParametersWithSBox;
import org.bouncycastle.util.Pack;

public class CryptoProWrapEngine extends GOST28147WrapEngine {
    private static boolean bitSet(byte b, int i) {
        return (b & (1 << i)) != 0;
    }

    private static byte[] cryptoProDiversify(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        for (int i = 0; i != 8; i++) {
            int i2 = 0;
            int i3 = i2;
            int i4 = i3;
            while (i2 != 8) {
                int littleEndianToInt = Pack.littleEndianToInt(bArr, i2 * 4);
                if (bitSet(bArr2[i], i2)) {
                    i3 += littleEndianToInt;
                } else {
                    i4 += littleEndianToInt;
                }
                i2++;
            }
            byte[] bArr4 = new byte[8];
            Pack.intToLittleEndian(i3, bArr4, 0);
            Pack.intToLittleEndian(i4, bArr4, 4);
            GCFBBlockCipher gCFBBlockCipher = new GCFBBlockCipher(new GOST28147Engine());
            gCFBBlockCipher.init(true, new ParametersWithIV(new ParametersWithSBox(new KeyParameter(bArr), bArr3), bArr4));
            gCFBBlockCipher.processBlock(bArr, 0, bArr, 0);
            gCFBBlockCipher.processBlock(bArr, 8, bArr, 8);
            gCFBBlockCipher.processBlock(bArr, 16, bArr, 16);
            gCFBBlockCipher.processBlock(bArr, 24, bArr, 24);
        }
        return bArr;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:13:0x0064 in {2, 5, 6, 9, 11, 12} preds:[]
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
    public void init(boolean r6, org.bouncycastle.crypto.CipherParameters r7) {
        /*
        r5 = this;
        r0 = r7 instanceof org.bouncycastle.crypto.params.ParametersWithRandom;
        if (r0 == 0) goto L_0x000a;
    L_0x0004:
        r7 = (org.bouncycastle.crypto.params.ParametersWithRandom) r7;
        r7 = r7.getParameters();
    L_0x000a:
        r7 = (org.bouncycastle.crypto.params.ParametersWithUKM) r7;
        r0 = 0;
        r1 = r7.getParameters();
        r1 = r1 instanceof org.bouncycastle.crypto.params.ParametersWithSBox;
        if (r1 == 0) goto L_0x002c;
    L_0x0015:
        r0 = r7.getParameters();
        r0 = (org.bouncycastle.crypto.params.ParametersWithSBox) r0;
        r0 = r0.getParameters();
        r0 = (org.bouncycastle.crypto.params.KeyParameter) r0;
        r1 = r7.getParameters();
        r1 = (org.bouncycastle.crypto.params.ParametersWithSBox) r1;
        r1 = r1.getSBox();
        goto L_0x0035;
    L_0x002c:
        r1 = r7.getParameters();
        r1 = (org.bouncycastle.crypto.params.KeyParameter) r1;
        r4 = r1;
        r1 = r0;
        r0 = r4;
    L_0x0035:
        r2 = new org.bouncycastle.crypto.params.KeyParameter;
        r0 = r0.getKey();
        r3 = r7.getUKM();
        r0 = cryptoProDiversify(r0, r3, r1);
        r2.<init>(r0);
        if (r1 == 0) goto L_0x005a;
    L_0x0048:
        r0 = new org.bouncycastle.crypto.params.ParametersWithUKM;
        r3 = new org.bouncycastle.crypto.params.ParametersWithSBox;
        r3.<init>(r2, r1);
        r7 = r7.getUKM();
        r0.<init>(r3, r7);
    L_0x0056:
        super.init(r6, r0);
        return;
    L_0x005a:
        r0 = new org.bouncycastle.crypto.params.ParametersWithUKM;
        r7 = r7.getUKM();
        r0.<init>(r2, r7);
        goto L_0x0056;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.engines.CryptoProWrapEngine.init(boolean, org.bouncycastle.crypto.CipherParameters):void");
    }
}
